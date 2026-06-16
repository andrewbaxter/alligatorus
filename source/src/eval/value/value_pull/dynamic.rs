//! `ValuePull` — parallel to `TypePull` but on the value side. Holds
//! an `IsolateBundle` to a v8 instance of a source data type that implements the
//! `value` builtin interface. `type_()` invokes the user's compiled `value.type`
//! impl (`_im_v0`) on the held instance at v8 to recover the value's
//! source-language type (an `Rc<dyn Type>` external), and returns it directly.
use {
    crate::{
        eval::isolate_bundle::IsolateBundle,
        langhelp::{
            ExprContext,
            QExprId,
            Type,
            Value,
        },
    },
    std::rc::Rc,
};

/// A v8-instance-backed `Value` whose Rust-side type query routes through the
/// user's `value.type` impl in v8.
pub(crate) struct ValuePull {
    pub bundle: Rc<IsolateBundle>,
}

impl Value for ValuePull {
    fn type_(&self, xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        // Invoke `_im_v0` (the compiled `value.type` impl) on `self.bundle.instance` and
        // extract the resulting `Rc<dyn Type>` from the returned v8::External.
        let scope_ptr = match xx.vx.v8_scope.get() {
            Some(p) => p,
            None => return Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
        };

        // SAFETY: pointer set by `eval()` for its full duration; same access pattern as
        // `dispatch_cast_from` in mod.rs.
        let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
            &mut *(scope_ptr.as_ptr() as *mut _)
        };
        let instance = v8::Local::new(scope, &self.bundle.instance);
        let method_key =
            match v8::String::new(scope, &crate::langhelp::ConstKey::String("_im_v0".to_string()).to_str()) {
                Some(k) => k,
                None => return Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
            };
        let method_val = match instance.get(scope, method_key.into()) {
            Some(v) => v,
            None => return Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
        };
        let method_fn = match v8::Local::<v8::Function>::try_from(method_val) {
            Ok(f) => f,
            Err(_) => return Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
        };
        let recv: v8::Local<v8::Value> = instance.into();
        let null_arg: v8::Local<v8::Value> = v8::null(scope).into();
        let result = match method_fn.call(scope, recv, &[null_arg]) {
            Some(r) => r,
            None => return Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
        };
        crate::eval::rc_type_from_v8(&*result)
    }
}
