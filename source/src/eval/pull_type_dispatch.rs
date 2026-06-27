use {
    crate::{
        eval::{
            from_dynamic_ref,
            push::Push,
            to_dynamic_ext,
            value::{
                type_source_object::SourceObjectData,
                type_source_interface::BuiltinInterfaceKind,
            },
        },
        langhelp::{
            Aspect,
            CodeValue,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
        },
        r#gen::{
            TypeRefPush,
        },
    },
    dumpster::unsync::Gc,
    std::{
        cell::RefCell,
        rc::Rc,
    },
};


pub(crate) fn pull_aspect_v8(
    data: &Gc<SourceObjectData>,
    bundle: &v8::Global<v8::Object>,
    xx: &ExprContext,
    _loc: QExprId,
    key: &Unique,
) -> Option<Rc<dyn Aspect>> {
    let field_name = type_method_field_name(data, "aspect")?;
    let scope_ptr = xx.vx.dynamic_scope.get()?;
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let arg_obj = v8::Object::new(scope);
    let key_push = to_dynamic_ext(scope, key.clone());
    let key_str = v8::String::new(scope, &ConstKey::String("key".to_string()).to_str())?;
    arg_obj.set(scope, key_str.into(), key_push.into());
    let instance = v8::Local::new(scope, bundle);
    let method_key = v8::String::new(scope, &ConstKey::String(field_name).to_str())?;
    let method_val = instance.get(scope, method_key.into())?;
    let method_fn = v8::Local::<v8::Function>::try_from(method_val).ok()?;
    let recv: v8::Local<v8::Value> = instance.into();
    let arg_v: v8::Local<v8::Value> = arg_obj.into();
    let result = method_fn.call(scope, recv, &[arg_v])?;
    let result_obj = v8::Local::<v8::Object>::try_from(result).ok()?;
    let some_key = v8::String::new(scope, &ConstKey::String("some".to_string()).to_str())?;
    let some_val = result_obj.get(scope, some_key.into())?;
    if some_val.is_undefined() {
        return None;
    }
    Some(from_dynamic_ref::<Rc<dyn Aspect>>(&*some_val).clone())
}

pub(crate) fn pull_can_cast_from_v8(
    data: &Gc<SourceObjectData>,
    bundle: &v8::Global<v8::Object>,
    xx: &ExprContext,
    _loc: QExprId,
    source_type: &dyn Type,
) -> bool {
    let Some(field_name) = type_method_field_name(data, "can_cast_from") else {
        return false;
    };
    let Some(source_type_rc) = source_type.clone_rc() else {
        return false;
    };
    let Some(scope_ptr) = xx.vx.dynamic_scope.get() else {
        return false;
    };
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let arg_obj = v8::Object::new(scope);
    let xx_push = to_dynamic_ext(scope, Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: xx.scope.clone(),
        target: xx.target.clone(),
    }));
    let Some(xx_key) = v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str()) else {
        return false;
    };
    arg_obj.set(scope, xx_key.into(), xx_push.into());
    let Some(st_key) = v8::String::new(scope, &ConstKey::String("source_type".to_string()).to_str()) else {
        return false;
    };
    let source_type_v8 = Push::into_dynamic(TypeRefPush { type_ref: source_type_rc }, scope);
    arg_obj.set(scope, st_key.into(), source_type_v8);
    let Some(cv_key) = v8::String::new(scope, &ConstKey::String("cv".to_string()).to_str()) else {
        return false;
    };
    let null_for_cv: v8::Local<v8::Value> = v8::null(scope).into();
    arg_obj.set(scope, cv_key.into(), null_for_cv);
    let instance = v8::Local::new(scope, bundle);
    let Some(method_key) = v8::String::new(scope, &ConstKey::String(field_name).to_str()) else {
        return false;
    };
    let Some(method_val) = instance.get(scope, method_key.into()) else {
        return false;
    };
    let Ok(method_fn) = v8::Local::<v8::Function>::try_from(method_val) else {
        return false;
    };
    let recv: v8::Local<v8::Value> = instance.into();
    let arg_v: v8::Local<v8::Value> = arg_obj.into();
    let Some(result) = method_fn.call(scope, recv, &[arg_v]) else {
        return false;
    };
    result.boolean_value(scope)
}

pub(crate) fn pull_cast_from_v8(
    data: &Gc<SourceObjectData>,
    bundle: &v8::Global<v8::Object>,
    xx: &ExprContext,
    loc: QExprId,
    cv: CodeValue,
) -> EvalRes {
    let field_name = match type_method_field_name(data, "cast_from") {
        Some(n) => n,
        None => return EvalRes::err(loc, "cast_from field name not found".to_string()),
    };
    let scope_ptr = match xx.vx.dynamic_scope.get() {
        Some(p) => p,
        None => {
            return EvalRes::err(loc, "dynamic dispatch attempted with no active dynamic scope — cast_from must be called from \
                 within const_eval".to_string());
        },
    };
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let arg_obj = v8::Object::new(scope);
    let xx_push = to_dynamic_ext(scope, Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: xx.scope.clone(),
        target: xx.target.clone(),
    }));
    let xx_key = match v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "xx key alloc".to_string()),
    };
    arg_obj.set(scope, xx_key.into(), xx_push.into());
    let cv_key = match v8::String::new(scope, &ConstKey::String("cv".to_string()).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "cv key alloc".to_string()),
    };
    let source_type = match cv.value.type_(xx, loc).into_res() {
        Ok(v) => v,
        Err(e) => return EvalRes {
            v: None,
            errors: Some(e),
        },
    };
    let cve_cell: Rc<RefCell<Option<CodeValue>>> = Rc::new(RefCell::new(Some(cv)));
    let cve_ext = to_dynamic_ext(scope, cve_cell);
    arg_obj.set(scope, cv_key.into(), cve_ext.into());
    let st_key = match v8::String::new(scope, &ConstKey::String("source_type".to_string()).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "source_type key alloc".to_string()),
    };
    let source_type_v8 = Push::into_dynamic(TypeRefPush { type_ref: source_type }, scope);
    arg_obj.set(scope, st_key.into(), source_type_v8);
    let instance = v8::Local::new(scope, bundle);
    let method_key = match v8::String::new(scope, &ConstKey::String(field_name).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "method_field alloc".to_string()),
    };
    let method_val = match instance.get(scope, method_key.into()) {
        Some(v) => v,
        None => return EvalRes::err(loc, "instance missing cast_from field".to_string()),
    };
    let method_fn = match v8::Local::<v8::Function>::try_from(method_val) {
        Ok(f) => f,
        Err(_) => return EvalRes::err(loc, "instance cast_from field is not a function".to_string()),
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
                return EvalRes::err(loc, format!("cast_from dynamic call threw: {}", msg));
            },
        }
    };
    match from_dynamic_ref::<Rc<RefCell<Option<CodeValue>>>>(&*result).clone().borrow_mut().take() {
        Some(c) => EvalRes::ok(c),
        None => EvalRes::err(loc, "cast_from dynamic returned consumed CodeValueExtern".to_string()),
    }
}

pub(crate) fn pull_desc(data: &Gc<SourceObjectData>) -> String {
    data.name.clone().unwrap_or_else(|| "user-defined type".to_string())
}

fn type_method_field_name(data: &Gc<SourceObjectData>, method_name: &str) -> Option<String> {
    let impls = data.interface_impls.borrow();
    for impl_data in impls.iter() {
        if impl_data.interface.builtin_kind != Some(BuiltinInterfaceKind::Type) {
            continue;
        }
        let methods = impl_data.interface.methods.borrow();
        for method_impl in &impl_data.method_impls {
            if methods[method_impl.interface_method_index].name == method_name {
                return Some(methods[method_impl.interface_method_index].field_name.clone());
            }
        }
    }
    None
}

