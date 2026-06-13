//! `AspectUserDefined` — Rust-side wrapper around a v8 instance of a source
//! data type that implements one of the aspect interfaces (`aspect_field`,
//! `aspect_function`). Implements the corresponding Rust aspect traits by
//! dispatching to the underlying v8 instance via the interface's `_im_*`
//! method names.
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
        code_dynamic::{
            CodeDynamic,
            CodeDynamicCall,
            CodeDynamicFieldUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
            },
            isolate_bundle::IsolateBundle,
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
            Type,
            Value,
        },
    },
    std::rc::Rc,
};

/// A v8-instance-backed aspect impl. Holds the binding name(s) of the
/// underlying interface methods (`_im_afd0` for `aspect_field.field`,
/// `_im_af0` for `aspect_function.call`, etc.) implicitly via convention.
#[derive(Clone)]
pub(crate) struct AspectUserDefined {
    pub bundle: Rc<IsolateBundle>,
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

impl Aspect for AspectUserDefined { }

impl Value for AspectUserDefined {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic)
    }
}

/// Convert a `CodeValue` to a `CodeDynamic` by routing through the value
/// type's `AspectConsumeDynamic`. Used so const-folded args (where the
/// CV's `code` is `None`) still produce a runtime expression.
fn cv_to_dynamic_code(xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> Result<CodeDynamic, String> {
    if let Some(c) = cv.code.cast::<CodeDynamic>().into_res().ok().flatten() {
        return Ok(c.clone());
    }
    let vtype = cv.value.type_(xx, loc);
    let consume = crate::builtin::aspect_consume_dynamic_get(xx, loc, vtype.as_ref())
        .into_res()
        .map_err(|e| format!("AspectUserDefined: missing consume on {}: {:?}", vtype.desc(), e))?;
    consume
        .to_dynamic(xx, loc, CodeValue { code: cv.code.clone(), value: cv.value.clone() })
        .into_res()
        .map_err(|e| format!("AspectUserDefined: consume failed on {}: {:?}", vtype.desc(), e))
}

/// Build a `CodeDynamic` that, at runtime, looks up `field_name` on the
/// captured `self.bundle.instance` and calls it with `arg_record`.
/// Returns a `CodeValue` typed as `TypeAnyDynamic` (the static return type of
/// every interface method is `any`).
fn dispatch_method_call(
    loc: QExprId,
    bundle: &Rc<IsolateBundle>,
    xx: &ExprContext,
    field_name: &str,
    arg_record_fields: Vec<(String, CodeDynamic)>,
) -> EvalRes {
    // Capture the v8 instance as an `External::IsolateBundle` and emit a
    // `BindingUse` that resolves to it at runtime.
    let id = crate::langhelp::ScopeEntryId::Local(xx.vx.next_local_id.next());
    xx.vx.externals.borrow_mut().push((
        id,
        crate::langhelp::External::IsolateBundle(bundle.clone()),
    ));
    let instance_code = CodeDynamic {
        loc,
        type_: CodeDynamicType::BindingUse(crate::code_dynamic::CodeDynamicBindingUse { id }),
    };
    let arg_code = CodeDynamic {
        loc,
        type_: CodeDynamicType::NewObject(CodeDynamicNewObject {
            fields: arg_record_fields
                .into_iter()
                .map(|(k, v)| (ConstKey::String(k).to_str(), v))
                .collect(),
        }),
    };
    let method_access = CodeDynamic {
        loc,
        type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
            base: Box::new(instance_code),
            field: ConstKey::String(field_name.to_string()).to_str(),
        }),
    };
    let call = CodeDynamic {
        loc,
        type_: CodeDynamicType::Call(CodeDynamicCall {
            func: Box::new(method_access),
            args: vec![arg_code],
        }),
    };
    EvalRes::ok(CodeValue {
        code: Code { loc, code: Code_::Dyn(Rc::new(call)) },
        value: Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
        }),
    })
}

impl AspectField for AspectUserDefined {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let self_code = match cv_to_dynamic_code(xx, loc, &self_) {
            Ok(c) => c,
            Err(e) => return EvalRes::err(loc, e),
        };
        let key_code = match cv_to_dynamic_code(xx, loc, &key) {
            Ok(c) => c,
            Err(e) => return EvalRes::err(loc, e),
        };
        dispatch_method_call(
            loc,
            &self.bundle,
            xx,
            "_im_afd0",
            vec![("self_".to_string(), self_code), ("key".to_string(), key_code)],
        )
    }

    fn field_type_opt(&self, _xx: &ExprContext, _key: &ConstKey) -> Option<Rc<dyn Type>> {
        // Source-side field type lookups aren't currently routed through v8;
        // return None to fall back to whatever the caller does in the absence
        // of a static field type (treat as `any`).
        None
    }
}

impl AspectFunction for AspectUserDefined {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        let self_code = match cv_to_dynamic_code(xx, loc, &self_) {
            Ok(c) => c,
            Err(e) => return EvalRes::err(loc, e),
        };
        let arg_code = match cv_to_dynamic_code(xx, loc, &argument) {
            Ok(c) => c,
            Err(e) => return EvalRes::err(loc, e),
        };
        dispatch_method_call(
            loc,
            &self.bundle,
            xx,
            "_im_af0",
            vec![("self_".to_string(), self_code), ("argument".to_string(), arg_code)],
        )
    }
}
