//! `AspectPull` — Rust-side wrapper around a v8 instance of a source data
//! type that implements one of the aspect interfaces (`aspect_field`,
//! `aspect_function`). Implements the corresponding Rust aspect traits by
//! synchronously invoking the v8 instance's interface method at const-eval time
//! (parallel to `StatelessV8CastDispatcher::dispatch_cast_from`), then unwrapping
//! the returned `CodeValueExtern` to obtain the inner `CodeValue`.
//!
//! Parallel to `TypePull` and `ValuePull`. Source-language code constructs values
//! of source type `aspect` by `option(aspect).some(impl_instance)`; the `.some`
//! cast (via `TypeAspectDynamic::cast_from` / `TypeSourceDataTypeDynamic::cast_to`)
//! emits a `__data_instance_to_aspect` v8 call that produces a `v8::External`
//! wrapping `AspectPull`.
use crate::r#gen::ScopeEntry;
use {
    crate::{
        eval::to_v8,
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
        r#gen::{
            AspectField,
            AspectFunction,
        },
    },
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

/// A v8-instance-backed aspect impl. Holds the binding name(s) of the underlying
/// interface methods (`_im_afd0` for `aspect_field.field`, `_im_af0` for
/// `aspect_function.call`, etc.) implicitly via convention.
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
            "_im_afd0",
            vec![("inner".to_string(), self_), ("key".to_string(), key)],
        )
    }

    fn field_type_opt(&self, _xx: &ExprContext, _key: &ConstKey) -> Option<Rc<dyn Type>> {
        // `field_type_opt` is a Rust-only fast-path; routing through v8 for every call
        // would be wasteful. Return None — the caller falls back to treating the field
        // type as unknown (effectively `any`).
        None
    }
}

impl AspectFunction for AspectPull {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        dispatch_aspect_method(
            &self.bundle,
            xx,
            loc,
            "_im_af0",
            vec![("inner".to_string(), self_), ("argument".to_string(), argument)],
        )
    }
}

impl Value for AspectPull {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic)
    }
}

fn clone_cv(cv: &CodeValue) -> CodeValue {
    CodeValue {
        code: cv.code.clone(),
        value: cv.value.clone(),
    }
}

/// Synchronous v8 dispatch shared by `field` and `call`. Mirrors
/// `StatelessV8CastDispatcher::dispatch_cast_from`: builds a child `ExprContext`
/// whose `arg` scope entry yields the named CV record, sets up the v8 arg object
/// (`xx` external + the named CV fields as `CodeValueExtern` externals), invokes
/// `instance[method_field](arg_obj)`, and unwraps the returned `CodeValueExtern`
/// to a `CodeValue`.
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

    // SAFETY: pointer set by `eval()` for its full duration; same access pattern as
    // `StatelessV8CastDispatcher::dispatch_cast_from`.
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };

    // Build the v8-side arg object: `xx` external + each named CV field as a
    // `CodeValueExtern` external. The compiled impl body emits JS that does
    // `__cve_field(arg.<name>, …)` and friends, so the field values must be real CVE
    // externals at v8 — not placeholders.
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
        let cve_ext = crate::eval::to_v8(scope, cve_cell);
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
        crate::eval::from_v8_ref::<Rc<RefCell<Option<CodeValue>>>>(&*result).clone();
    let out_cv = match cell.borrow_mut().take() {
        Some(cv) => cv,
        None => return EvalRes::err(loc, format!("v8 method '{}' returned consumed CodeValueExtern", method_field)),
    };
    EvalRes::ok(out_cv)
}

/// `TypePullAspect` — the source-language type of an aspect handle for a
/// user-defined `is_aspect` interface. Carries `iface_ref` so `AspectField` can
/// resolve a source-level method name (`to_code`) to the iface's
/// `SourceInterfaceMethodData.field_name` (`_im0`/`_im1`/…) and emit
/// `instance[field_name](arg)` at v8 time, with the result CV typed by the
/// method's declared `ret_type`. Parallel to how `TypeSourceDataTypeDynamic` looks
/// up data-type methods.
///
/// `bundle` is the v8 instance that backs the impl — the same one
/// `TypePull` held. `Aspect::v8_instance` surfaces it so
/// `TypeRefPush::aspect` (in v8) can put the JS instance into an `OptionPush` payload
/// without needing to RTTI the `Rc<dyn Type>` it came from.
pub(crate) struct TypePullAspect {
    /// Some when the handle was looked up via `TypePull::aspect` (bundle came
    /// from the type). None when it was minted at compile time by `aspect(base, key)`
    /// — the bundle then rides in the CV's runtime code via the `base` expression, and
    /// `Aspect::v8_instance` doesn't have one statically.
    pub bundle: Option<v8::Global<v8::Object>>,
    pub iface_ref: Rc<crate::eval::value::type_source_interface::SourceInterfaceRef>,
}

impl Aspect for TypePullAspect {
    fn v8_instance(&self) -> Option<v8::Global<v8::Object>> {
        self.bundle.clone()
    }
}

impl crate::r#gen::AspectField for TypePullAspect {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let self_code = match self_.code.cast::<crate::code_dynamic::CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "TypePullAspect.field: receiver has no runtime code".to_string()),
        };
        let key_type = key.value.type_(xx, loc);
        let key_string = match crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        let methods = self.iface_ref.data.methods.borrow();
        let method = match methods.iter().find(|m| m.name == key_name) {
            Some(m) => m,
            None => return EvalRes::err(
                loc,
                format!(
                    "aspect handle has no method '{}' on interface '{}'",
                    key_name,
                    self.iface_ref.data.description.borrow().as_deref().unwrap_or("?").to_string(),
                ),
            ),
        };
        let arg_type = match method.arg_type.borrow().clone() {
            Some(t) => t,
            None => return EvalRes::err(loc, "interface method missing arg type".to_string()),
        };
        let ret_type = match method.ret_type.borrow().clone() {
            Some(t) => t,
            None => return EvalRes::err(loc, "interface method missing ret type".to_string()),
        };
        let field_code = crate::code_dynamic::CodeDynamic {
            loc,
            type_: crate::code_dynamic::CodeDynamicType::FieldUse(crate::code_dynamic::CodeDynamicFieldUse {
                base: Box::new(self_code),
                field: ConstKey::String(method.field_name.clone()).to_str(),
            }),
        };
        let func_type = Rc::new(crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
            arg: arg_type,
            ret: ret_type,
        });
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(field_code)),
            },
            value: Rc::new(crate::eval::dynamic_util::ValueDynamic {
                source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                type_: func_type,
            }),
        })
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(s) = key else {
            return None;
        };
        let methods = self.iface_ref.data.methods.borrow();
        let method = methods.iter().find(|m| m.name == *s)?;
        let arg_type = method.arg_type.borrow().clone()?;
        let ret_type = method.ret_type.borrow().clone()?;
        Some(Rc::new(crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
            arg: arg_type,
            ret: ret_type,
        }))
    }
}

impl crate::r#gen::AspectInitialBind for TypePullAspect {
    fn initial_bind(&self, dynamic_key: crate::langhelp::ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(crate::eval::value::type_scope_entry::dynamic::ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypePullAspect {
                iface_ref: self.iface_ref.clone(),
                bundle: self.bundle.clone(),
            }),
        })
    }
}

impl Type for TypePullAspect {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypePullAspect {
            iface_ref: self.iface_ref.clone(),
            bundle: self.bundle.clone(),
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if *key == crate::r#gen::aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypePullAspect {
                iface_ref: self.iface_ref.clone(),
                bundle: self.bundle.clone(),
            }) as Box<dyn crate::r#gen::AspectField>));
        }
        if *key == crate::r#gen::aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypePullAspect {
                iface_ref: self.iface_ref.clone(),
                bundle: self.bundle.clone(),
            }) as Box<dyn crate::r#gen::AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_function_aspect_id() {
            // No-op marker: callers that only check `.is_some()` see this as an
            // aspect-bearing type without requiring AspectFunction work.
            return None;
        }
        None
    }

    fn desc(&self) -> String {
        format!("aspect<{}>", self.iface_ref.data.description.borrow().as_deref().unwrap_or("?"))
    }
}

impl Value for TypePullAspect {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
