use {
    crate::{
        eval::{
            rc_type_from_v8,
            value::type_any::dynamic::TypeAnyDynamic,
        },
        langhelp::{
            ConstKey,
            ExprContext,
            QExprId,
            Type,
            Value,
        },
        r#gen::IM_VALUE_TYPE_FIELD,
    },
    std::rc::Rc,
};

pub(crate) struct ValuePull {
    pub bundle: v8::Global<v8::Object>,
}

impl Value for ValuePull {
    fn type_(&self, xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        let scope_ptr = match xx.vx.v8_scope.get() {
            Some(p) => p,
            None => return Rc::new(TypeAnyDynamic),
        };
        let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
            &mut *(scope_ptr.as_ptr() as *mut _)
        };
        let instance = v8::Local::new(scope, &self.bundle);
        let method_key = match v8::String::new(scope, &ConstKey::String(IM_VALUE_TYPE_FIELD.to_string()).to_str()) {
            Some(k) => k,
            None => return Rc::new(TypeAnyDynamic),
        };
        let method_val = match instance.get(scope, method_key.into()) {
            Some(v) => v,
            None => return Rc::new(TypeAnyDynamic),
        };
        let method_fn = match v8::Local::<v8::Function>::try_from(method_val) {
            Ok(f) => f,
            Err(_) => return Rc::new(TypeAnyDynamic),
        };
        let recv: v8::Local<v8::Value> = instance.into();
        let null_arg: v8::Local<v8::Value> = v8::null(scope).into();
        let result = match method_fn.call(scope, recv, &[null_arg]) {
            Some(r) => r,
            None => return Rc::new(TypeAnyDynamic),
        };
        rc_type_from_v8(&*result)
    }
}
