//! `AspectUserDefined` — Rust-side wrapper around a v8 instance of a source
//! data type that implements one of the aspect interfaces (`aspect_field`,
//! `aspect_function`). Implements the corresponding Rust aspect traits by
//! synchronously invoking the v8 instance's interface method at const-eval
//! time (parallel to `StatelessV8CastDispatcher::dispatch_cast_from`), then
//! unwrapping the returned `CodeValueExtern` to obtain the inner `CodeValue`.
//!
//! Parallel to `TypeUserDefined` and `ValueUserDefined`. The instance is
//! held as a v8::Global on `IsolateBundle`. Source-language code constructs
//! values of source type `aspect` by `option(aspect).some(impl_instance)`;
//! the `.some` cast (via `TypeAspectDynamic::cast_from` /
//! `TypeSourceDataTypeDynamic::cast_to`) emits a `__data_instance_to_aspect`
//! v8 call that produces a `v8::External` wrapping `Rc<AspectUserDefined>`.

use {
    crate::{
        builtin::{
            AspectField,
            AspectFunction,
        },
        eval::{
            isolate_bundle::IsolateBundle,
            to_v8,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
            Scope,
            ScopeEntry,
            Type,
            Value,
        },
    },
    std::{
        cell::RefCell,
        collections::HashMap,
        rc::Rc,
    },
};

/// A v8-instance-backed aspect impl. Holds the binding name(s) of the
/// underlying interface methods (`_im_afd0` for `aspect_field.field`,
/// `_im_af0` for `aspect_function.call`, etc.) implicitly via convention.
#[derive(Clone)]
pub(crate) struct AspectUserDefined {
    pub bundle: Rc<IsolateBundle>,
}

impl Aspect for AspectUserDefined { }

impl Value for AspectUserDefined {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic)
    }
}

/// Owning handle returned from `TypeUserDefined::aspect` — wraps an
/// `Rc<AspectUserDefined>` and impls `AspectField` / `AspectFunction` by
/// forwarding to the inner. Lets the aspect lookup return a `Box<dyn ...>`
/// holding the `Rc` without cloning the underlying instance.
pub(crate) struct AspectUserDefinedHandle {
    pub inner: Rc<AspectUserDefined>,
}

impl Aspect for AspectUserDefinedHandle { }

impl Value for AspectUserDefinedHandle {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic)
    }
}

impl AspectField for AspectUserDefinedHandle {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        self.inner.field(xx, loc, self_, key)
    }

    fn field_type_opt(&self, xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        self.inner.field_type_opt(xx, key)
    }
}

impl AspectFunction for AspectUserDefinedHandle {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        self.inner.call(xx, loc, self_, argument)
    }
}

/// Synchronous v8 dispatch shared by `field` and `call`. Mirrors
/// `StatelessV8CastDispatcher::dispatch_cast_from`: builds a child
/// `ExprContext` whose `arg` scope entry yields the named CV record, sets up
/// the v8 arg object (`xx` external + the named CV fields as `CodeValueExtern`
/// externals), invokes `instance[method_field](arg_obj)`, and unwraps the
/// returned `CodeValueExtern` to a `CodeValue`.
fn dispatch_aspect_method(
    bundle: &IsolateBundle,
    xx: &ExprContext,
    loc: QExprId,
    method_field: &str,
    cv_fields: Vec<(String, CodeValue)>,
) -> EvalRes {
    let scope_ptr = match xx.vx.v8_scope.get() {
        Some(p) => p,
        None => return EvalRes::err(loc, "AspectUserDefined dispatch: no active v8 scope".to_string()),
    };
    // SAFETY: pointer set by `eval()` for its full duration; same access
    // pattern as `StatelessV8CastDispatcher::dispatch_cast_from`.
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> =
        unsafe { &mut *(scope_ptr.as_ptr() as *mut _) };

    // Child ExprContext: bind `arg` so `eval_const(arg.xx, stage(...))` in the
    // impl can resolve `arg.self_`, `arg.key` / `arg.argument` as the
    // corresponding CodeValues at Rust const_eval time.
    let arg_factory = AspectArgRecordFactory {
        loc,
        cv_fields: cv_fields.iter().map(|(k, v)| (k.clone(), clone_cv(v))).collect(),
    };
    let child_scope = Rc::new(Scope {
        values: RefCell::new(HashMap::new()),
        parent: Some(xx.scope.clone()),
    });
    child_scope.values.borrow_mut().insert(
        ConstKey::String("arg".to_string()),
        Box::new(arg_factory) as Box<dyn ScopeEntry>,
    );
    let child_xx = Rc::new(ExprContext {
        vx: xx.vx.clone(),
        import_state: xx.import_state.clone(),
        scope: child_scope,
        target: xx.target.clone(),
    });

    // Build the v8-side arg object: `xx` external + a `CodeValueExtern` per
    // named CV field (the user reads them at Rust const_eval time via
    // `arg.<name>`, not at v8 time, so the JS field values stand in as
    // opaque placeholders to keep the JS shape consistent).
    let arg_obj = v8::Object::new(scope);
    let xx_external = to_v8(scope, child_xx.clone());
    let xx_key = match v8::String::new(scope, &ConstKey::String("xx".to_string()).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "AspectUserDefined dispatch: xx key alloc failed".to_string()),
    };
    arg_obj.set(scope, xx_key.into(), xx_external.into());
    let null_for_placeholder: v8::Local<v8::Value> = v8::null(scope).into();
    for (name, _) in &cv_fields {
        let k = match v8::String::new(scope, &ConstKey::String(name.clone()).to_str()) {
            Some(s) => s,
            None => return EvalRes::err(loc, format!("AspectUserDefined dispatch: '{}' key alloc failed", name)),
        };
        arg_obj.set(scope, k.into(), null_for_placeholder);
    }

    let instance = v8::Local::new(scope, &bundle.instance);
    let method_key = match v8::String::new(scope, &ConstKey::String(method_field.to_string()).to_str()) {
        Some(k) => k,
        None => return EvalRes::err(loc, "AspectUserDefined dispatch: method key alloc failed".to_string()),
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
    let result = match method_fn.call(scope, recv, &[arg_v]) {
        Some(r) => r,
        None => return EvalRes::err(loc, format!("v8 method '{}' returned no value", method_field)),
    };
    let cell: Rc<RefCell<Option<CodeValue>>> =
        crate::eval::from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*result).clone();
    let out_cv = match cell.borrow_mut().take() {
        Some(cv) => cv,
        None => return EvalRes::err(loc, format!("v8 method '{}' returned consumed CodeValueExtern", method_field)),
    };
    EvalRes::ok(out_cv)
}

fn clone_cv(cv: &CodeValue) -> CodeValue {
    CodeValue {
        code: cv.code.clone(),
        value: cv.value.clone(),
    }
}

/// Scope entry that yields a fresh record on each `use_` with the named CV
/// fields exposed for `arg.<name>` access. Parallel to `ArgRecordFactory`
/// from `user_defined_type_dispatch`, but with a configurable field set.
struct AspectArgRecordFactory {
    loc: QExprId,
    cv_fields: Vec<(String, CodeValue)>,
}

impl ScopeEntry for AspectArgRecordFactory {
    fn use_(&self, _xx: &ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        let mut fields = std::collections::BTreeMap::new();
        for (name, cv) in &self.cv_fields {
            fields.insert(
                ConstKey::String(name.clone()),
                crate::eval::value::record::RecordField::EvalRes(EvalRes::ok(clone_cv(cv))),
            );
        }
        fields.insert(
            ConstKey::String("xx".to_string()),
            crate::eval::value::record::RecordField::EvalRes(EvalRes::ok(CodeValue {
                code: Code { loc: self.loc, code: Code_::None },
                value: Rc::new(crate::eval::value::value_null::r#const::ValueNullConst),
            })),
        );
        CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(crate::eval::value::record::Record { base: None, fields }),
        }
    }
}

impl AspectField for AspectUserDefined {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        dispatch_aspect_method(
            &self.bundle,
            xx,
            loc,
            "_im_afd0",
            vec![("self_".to_string(), self_), ("key".to_string(), key)],
        )
    }

    fn field_type_opt(&self, _xx: &ExprContext, _key: &ConstKey) -> Option<Rc<dyn Type>> {
        // `field_type_opt` is a Rust-only fast-path; routing through v8 for
        // every call would be wasteful. Return None — the caller falls back
        // to treating the field type as unknown (effectively `any`).
        None
    }
}

impl AspectFunction for AspectUserDefined {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        dispatch_aspect_method(
            &self.bundle,
            xx,
            loc,
            "_im_af0",
            vec![("self_".to_string(), self_), ("argument".to_string(), argument)],
        )
    }
}
