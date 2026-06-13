//! `TypeFunctionAspectDynamic` — the source-level `aspect(base, key)` builtin.
//! `base` is a `Rc<dyn Type>` handle (a `TypeRefV8`-typed value, typically
//! `value.type`). `key` is an interface-bearing value (`Rc<SourceInterfaceRef>`
//! — the result of `d.new_interface()`, or a globally-bound aspect interface
//! like `aspect_field`).
//!
//! At call time we pull the aspect identity (`Unique`) out of `key` via
//! `AspectAspect::get_unique` on its source type — no RTTI on `dyn Value`,
//! the invariant lives in the trait. We then emit code calling `.aspect(unique)`
//! on the `base` handle at v8 time, and type the result CV as
//! `option(TypeUserDefinedAspect { iface_ref })` so source-level
//! `.method(arg)` on the unwrapped handle dispatches via the iface's compiled
//! `field_name` against the bundled instance.

use {
    crate::{
        builtin::{
            AspectFunction,
            AspectInitialBind,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
            CodeDynamicFieldUse,
        },
        eval::{
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
            },
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
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
            ScopeEntry,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeFunctionAspectDynamic;

impl Aspect for TypeFunctionAspectDynamic { }

impl AspectFunction for TypeFunctionAspectDynamic {
    fn call(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        _function: CodeValue,
        argument: CodeValue,
    ) -> EvalRes {
        // The argument is a 2-tuple `(base, key)`. We index by tuple element
        // — keys `_0` and `_1` — using `AspectField`.
        let arg_type = argument.value.type_(xx, loc);
        let arg_field = match crate::builtin::aspect_field_get_maybe(xx, arg_type.as_ref()) {
            Some(f) => f,
            None => return EvalRes::err(
                loc,
                format!(
                    "aspect(): argument type ({}) is not a record/tuple — expected `(base, key)`",
                    arg_type.desc(),
                ),
            ),
        };
        let arg_for_base = clone_cv(&argument);
        let key_cv_base = CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(crate::eval::value::value_string::r#const::ValueStringConst(
                "_0".to_string(),
            )),
        };
        let base = match arg_field.field(xx, loc, arg_for_base, key_cv_base).into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let arg_for_key = clone_cv(&argument);
        let key_cv_key = CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(crate::eval::value::value_string::r#const::ValueStringConst(
                "_1".to_string(),
            )),
        };
        let key = match arg_field.field(xx, loc, arg_for_key, key_cv_key).into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };

        // Pull the iface ref out of `key` for compile-time typing of the
        // result. Done by invariant downcast — `aspect()` is only meaningful
        // when key's source type provides `AspectAspect`, and the only
        // current provider is `TypeSourceInterfaceDynamic` whose values are
        // `Rc<SourceInterfaceRef>`.
        let key_type = key.value.type_(xx, loc);
        let aspect_aspect = match crate::builtin::aspect_aspect_get_maybe(xx, key_type.as_ref()) {
            Some(a) => a,
            None => return EvalRes::err(
                loc,
                format!(
                    "aspect(): key argument's type ({}) is not an aspect interface",
                    key_type.desc(),
                ),
            ),
        };
        let unique = aspect_aspect.get_unique(key.value.as_ref());
        let iface_ref = crate::invariant::must_downcast_ref::<
            Rc<crate::eval::value::type_source_interface::SourceInterfaceRef>,
        >(key.value.as_ref() as &dyn std::any::Any)
            .clone();

        // Generate `base_code.aspect(unique_external)` — the `aspect` method
        // on the `TypeRefV8` JS wrapper. `dynamic_wrap!` mangles the method
        // name as `_0` per its declaration order in `type_ref_v8.rs`.
        let base_code = match base.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(
                loc,
                "aspect(): base has no runtime code".to_string(),
            ),
        };
        let unique_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((
            unique_id,
            crate::langhelp::External::Unique(unique),
        ));
        let unique_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: unique_id }),
        };
        // Emit `__aspect_lookup(base, unique)` — the v8 callback that runs
        // `type_ref.aspect(unique)` and shapes the result as the source-level
        // option tagged union (`{some: instance}` / `{none: null}`).
        let lookup_fn = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse {
                id: crate::eval::ASPECT_LOOKUP_FN_SCOPE_ID,
            }),
        };
        let call_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(lookup_fn),
                args: vec![base_code, unique_code],
            }),
        };

        // Result CV: value-type is `option(TypeUserDefinedAspect{iface_ref})`,
        // with bundle filled at runtime when the OptionV8 unwraps to the JS
        // instance. The compile-time `bundle` slot on `TypeUserDefinedAspect`
        // is only consulted via `Aspect::v8_instance` at `TypeRefV8.aspect`
        // time, so we put a placeholder here — source-level field access on
        // the unwrapped handle uses the CV's runtime code, not the type's
        // bundle.
        //
        // TODO: when `OptionV8.get()` lands, ensure the bundle propagated by
        // `Aspect::v8_instance` is the same one the source-level CV code
        // references.
        // The compile-time `bundle` slot on `TypeUserDefinedAspect` is only
        // consulted via `Aspect::v8_instance` at `TypeRefV8.aspect` time, not
        // for AspectField field access on this CV. Stash a "no bundle"
        // sentinel here — see `TypeUserDefinedAspect.bundle` doc.
        let handle_type: Rc<dyn Type> = Rc::new(
            crate::eval::value::value_user_defined::aspect::TypeUserDefinedAspect {
                iface_ref,
                bundle: None,
            },
        );
        let option_type: Rc<dyn Type> = Rc::new(
            crate::eval::value::type_option::dynamic::TypeOptionDynamic {
                element_type: handle_type,
            },
        );
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(call_code)) },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: option_type,
            }),
        })
    }
}

fn clone_cv(cv: &CodeValue) -> CodeValue {
    CodeValue { code: cv.code.clone(), value: cv.value.clone() }
}

impl AspectInitialBind for TypeFunctionAspectDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeFunctionAspectDynamic),
        })
    }
}

impl Type for TypeFunctionAspectDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeFunctionAspectDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(
                Box::new(TypeFunctionAspectDynamic) as Box<dyn AspectFunction>,
            ));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(
                Box::new(TypeFunctionAspectDynamic) as Box<dyn AspectInitialBind>,
            ));
        }
        None
    }

    fn desc(&self) -> String {
        "aspect".to_string()
    }
}

impl Value for TypeFunctionAspectDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeFunctionAspectDynamic)
    }
}
