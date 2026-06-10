//! v8-side wrappers around `Rc<dyn Type>` and `Option<...>`. These give the
//! user's compiled-to-JS impl body a JS object to call `.aspect(unique)` on
//! (returns `OptionV8`), and `.is_some()` on `OptionV8`. Built via
//! `dynamic_wrap!` so the structs are both Rust state holders and have v8
//! method dispatch.
//!
//! The `TypeTypeRefV8Dynamic` and `TypeOptionV8Dynamic` types — produced by
//! the macro alongside the structs — are what's referenced from
//! `build_cast_arg_record_type` and `TypeTypeRef`'s compile-path field
//! return, so the compiled method bodies type-check and emit JS that
//! addresses the dynamic_wrap-generated `_0` method names.
//!
//! The `OptionV8` and `TypeRefV8` structs themselves are constructed only at
//! runtime, when the v8 dispatch path (currently scaffolded but not yet
//! wired up at `as`-time) builds JS objects for `arg.source_type` and the
//! result of `.aspect`.

#[allow(unused_imports)]
use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::{
            Erb,
            TargetBindSub,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            dynamic_wrap::{
                DynamicWrap,
                dynamic_wrap_method_name,
            },
            value::{
                tuple::Tuple,
                type_any::r#const::TypeAnyConst,
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Scope,
            ScopeEntry,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    paste::paste,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        collections::HashMap,
        mem,
        rc::Rc,
    },
};

dynamic_wrap!(struct OptionV8(self_, scope) {
    data {
        is_some_inner: bool
    }
    fn is_some_() -> bool {
        self_.is_some_inner
    }
});

dynamic_wrap!(struct TypeRefV8(self_, scope) {
    data {
        type_ref: Rc<dyn Type>
    }
    fn aspect(unique: Unique) -> OptionV8 {
        let ex_slot = scope
            .get_slot::<crate::eval::V8EvalContextSlot>()
            .expect("TypeRefV8.aspect: V8EvalContextSlot not set");
        let import_state_id = scope.get_slot::<crate::eval::V8DefSetCounter>().unwrap().import_id;
        let xx = Rc::new(ExprContext {
            vx: ex_slot.ex.clone(),
            import_state: Rc::new(Cell::new(import_state_id)),
            scope: Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: None,
            }),
            target: Rc::new(crate::eval::TargetDynamic),
        });
        let is_some = self_.type_ref.aspect(&xx, &unique).is_some();
        OptionV8 { is_some_inner: is_some }
    }
});
