//! `TypeAspectDynamic` — singleton Type marking "aspect" as a cast destination.
//! Source-data-type instances implementing some aspect interface (e.g.
//! `aspect_field`, `aspect_function`) cast to this; the cast (in
//! `TypeSourceDataTypeDynamic::cast_to`) emits a `__data_instance_to_aspect`
//! call that wraps the runtime v8 instance into an `AspectUserDefined`
//! external.

use crate::r#gen::ScopeEntry;
use crate::r#gen::type_aspect_identity;
use {
    crate::{
        r#gen::{AspectBind, AspectBindDynamic, AspectConsumeDynamic, AspectInitialBind, AspectLiftDynamic, AspectStackValueDynamic, aspect_bind_aspect_id, aspect_bind_dynamic_aspect_id, aspect_consume_dynamic_aspect_id, aspect_initial_bind_aspect_id, aspect_lift_dynamic_aspect_id, aspect_stack_value_dynamic_aspect_id},
        code_dynamic::CodeDynamic,
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                mortar_consume_to_dynamic,
                mortar_dynamic_stack_to_value,
            },
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            ExprContext,
            QExprId,
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


pub(crate) struct TypeAspectDynamic;

impl Aspect for TypeAspectDynamic { }

impl AspectBind for TypeAspectDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(crate::langhelp::Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeAspectDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeAspectDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectStackValueDynamic for TypeAspectDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeAspectDynamic))
    }
}

impl AspectInitialBind for TypeAspectDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeAspectDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeAspectDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        // The v8 representation is a v8::External wrapping an
        // `Rc<crate::eval::value::value_user_defined::aspect::AspectUserDefined>`.
        let aspect: Rc<crate::eval::value::value_user_defined::aspect::AspectUserDefined> =
            crate::eval::from_v8_ref::<
                Rc<crate::eval::value::value_user_defined::aspect::AspectUserDefined>,
            >(&*value).clone();
        Ok(aspect)
    }
}

impl crate::r#gen::AspectLowerDynamic for TypeAspectDynamic {
    fn lower(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let aspect: Rc<crate::eval::value::value_user_defined::aspect::AspectUserDefined> =
            crate::eval::from_v8_ref::<
                Rc<crate::eval::value::value_user_defined::aspect::AspectUserDefined>,
            >(&*value).clone();
        Ok(aspect)
    }
}

impl Type for TypeAspectDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeAspectDynamic)
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeAspectDynamic))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_aspect_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectDynamic) as Box<dyn AspectLiftDynamic>));
        }
        if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectDynamic) as Box<dyn crate::r#gen::AspectLowerDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAspectDynamic) as Box<dyn AspectInitialBind>));
        }
        None
    }

    fn desc(&self) -> String {
        "aspect".to_string()
    }
}

impl Value for TypeAspectDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
