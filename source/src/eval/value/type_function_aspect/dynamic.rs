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
        // Argument is the 2-tuple `(base, key)`. Use `AspectTuple` to
        // destructure — tuples don't expose `_0`/`_1` via `AspectField`.
        let arg_type = argument.value.type_(xx, loc);
        let arg_tuple = match crate::builtin::aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            Some(t) => t,
            None => return EvalRes::err(
                loc,
                format!(
                    "aspect(): argument type ({}) is not a tuple — expected `(base, key)`",
                    arg_type.desc(),
                ),
            ),
        };
        let CodeValue { code: _argument_code, value: argument_value } = argument;
        let elements = arg_tuple.take_elements(argument_value);
        if elements.len() != 2 {
            return EvalRes::err(
                loc,
                format!("aspect(): expected 2-tuple `(base, key)`, got {} elements", elements.len()),
            );
        }
        let mut iter = elements.into_iter();
        let base = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let key = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };

        // Pull the iface ref out of `key`. Type-of-types values are now
        // carried as `ValueAnyConst { inner: Rc<dyn Type> }` typed as
        // `TypeAnyConst`, so `AspectAspect` lives on the inner type rather
        // than on `key.value.type_()` directly. Extract the inner via
        // `AspectType`, then look up `AspectAspect` on it. (The `AspectAspect`
        // methods on `TypeSourceInterfaceConst` ignore the value arg — they
        // read from the type's own data — so passing the inner type's own
        // `as_value()` is fine.)
        let key_meta = key.value.type_(xx, loc);
        let key_type = match crate::builtin::aspect_type_get_maybe(xx, key_meta.as_ref()) {
            Some(at) => at.type_(key.value.as_ref()),
            None => key_meta.clone(),
        };
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
        let key_as_value = key_type.as_value();
        let unique = aspect_aspect.get_unique(key_as_value.as_ref());
        let iface_data = aspect_aspect.get_iface_data(key_as_value.as_ref());
        let iface_ref = Rc::new(
            crate::eval::value::type_source_interface::SourceInterfaceRef { data: iface_data },
        );

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
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeFunctionAspectDynamic),
        })
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
        "fn-aspect".to_string()
    }
}

impl Value for TypeFunctionAspectDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
