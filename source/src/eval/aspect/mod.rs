use {
    crate::{
        eval::{
            from_v8_ref,
            to_v8,
            value::type_aspect::dynamic::TypeAspectDynamic,
        },
        langhelp::{
            Aspect,
            CodeValue,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Value,
        },
        r#gen::{
            AspectField,
            AspectFunction,
            IM_ASPECT_FIELD_FIELD_FIELD,
            IM_ASPECT_FUNCTION_CALL_FIELD,
        },
    },
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct AspectPull {
    pub bundle: v8::Global<v8::Object>,
}

impl Aspect for AspectPull { }

impl AspectField for AspectPull {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        dispatch_aspect_method(
            &self.bundle,
            xx,
            loc,
            IM_ASPECT_FIELD_FIELD_FIELD,
            vec![("inner".to_string(), self_), ("key".to_string(), key)],
        )
    }

    fn field_type_opt(&self, _xx: &ExprContext, _key: &ConstKey) -> Option<Rc<dyn Type>> {
        None
    }
}

impl AspectFunction for AspectPull {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        dispatch_aspect_method(
            &self.bundle,
            xx,
            loc,
            IM_ASPECT_FUNCTION_CALL_FIELD,
            vec![("inner".to_string(), self_), ("argument".to_string(), argument)],
        )
    }
}

impl Value for AspectPull {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAspectDynamic)
    }
}

fn clone_cv(cv: &CodeValue) -> CodeValue {
    CodeValue {
        code: cv.code.clone(),
        value: cv.value.clone(),
    }
}

fn dispatch_aspect_method(
    bundle: &v8::Global<v8::Object>,
    xx: &ExprContext,
    loc: QExprId,
    method_field: &str,
    cv_fields: Vec<(String, CodeValue)>,
) -> EvalRes {
    let scope_ptr = match xx.vx.v8_scope.get() {
        Some(p) => p,
        None => return EvalRes::err(loc, "AspectPull dispatch: no active v8 scope".to_string()),
    };

    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };

    let arg_obj = v8::Object::new(scope);
    let xx_push = to_v8(scope, Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: xx.scope.clone(),
        target: xx.target.clone(),
    }));
    let xx_key = match v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "AspectPull dispatch: xx key alloc failed".to_string()),
    };
    arg_obj.set(scope, xx_key.into(), xx_push.into());
    for (name, cv) in &cv_fields {
        let k = match v8::String::new(scope, &ConstKey::String(name.clone()).to_str()) {
            Some(s) => s,
            None => return EvalRes::err(loc, format!("AspectPull dispatch: '{}' key alloc failed", name)),
        };
        let cve_cell: Rc<RefCell<Option<CodeValue>>> = Rc::new(RefCell::new(Some(clone_cv(cv))));
        let cve_ext = to_v8(scope, cve_cell);
        arg_obj.set(scope, k.into(), cve_ext.into());
    }
    let instance = v8::Local::new(scope, bundle);
    let method_key = match v8::String::new(scope, &ConstKey::String(method_field.to_string()).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "AspectPull dispatch: method key alloc failed".to_string()),
    };
    let method_val = match instance.get(scope, method_key.into()) {
        Some(v) => v,
        None => return EvalRes::err(loc, format!("instance missing field '{}'", method_field)),
    };
    let method_fn = match v8::Local::<v8::Function>::try_from(method_val) {
        Ok(f) => f,
        Err(_) => return EvalRes::err(loc, format!("instance field '{}' is not a function", method_field)),
    };
    let recv: v8::Local<v8::Value> = instance.into();
    let arg_v: v8::Local<v8::Value> = arg_obj.into();
    let result = {
        let tc = std::pin::pin!(v8::TryCatch::new(scope));
        let mut tc = tc.init();
        match method_fn.call(&mut tc, recv, &[arg_v]) {
            Some(r) => r,
            None => {
                let msg =
                    tc
                        .exception()
                        .map(|e| e.to_rust_string_lossy(&mut tc))
                        .unwrap_or_else(|| "<no exception>".to_string());
                return EvalRes::err(loc, format!("v8 method '{}' threw: {}", method_field, msg));
            },
        }
    };
    let cell: Rc<RefCell<Option<CodeValue>>> =
        from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*result).clone();
    let out_cv = match cell.borrow_mut().take() {
        Some(cv) => cv,
        None => return EvalRes::err(loc, format!("v8 method '{}' returned consumed CodeValueExtern", method_field)),
    };
    EvalRes::ok(out_cv)
}
