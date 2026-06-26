use {
    crate::{
        artifact::ExporterAny,
        code_dynamic::CodeDynamic,
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_consume_to_dynamic,
                dynamic_stack_to_value,
            },
            value::{
                type_any::r#const::ValueAnyConst,
                type_code_dyn_handle::dynamic::TypeCodeDynHandleDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_root::r#const::TypeRoot,
                type_binding::dynamic::BindingDynamicBinding,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            BindingId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectStackValueDynamic,
            AspectUnion,
            Binding,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_union_aspect_id,
            type_code_enum_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeCodeEnumDynamic;

impl Aspect for TypeCodeEnumDynamic { }

impl AspectBind for TypeCodeEnumDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeEnumDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeEnumDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeCodeEnumDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeEnumDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeEnumDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeCodeEnumDynamic))
    }
}

impl AspectUnion for TypeCodeEnumDynamic {
    fn variants(&self) -> Vec<(ConstKey, String, Rc<dyn Type>)> {
        vec![
            (ConstKey::String("none".to_string()), "none".to_string(), Rc::new(TypeNullDynamic) as Rc<dyn Type>),
            (
                ConstKey::String("dyn".to_string()),
                "dyn".to_string(),
                Rc::new(TypeCodeDynHandleDynamic) as Rc<dyn Type>,
            ),
        ]
    }
}

impl ExporterAny for TypeCodeEnumDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeCodeEnumDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeCodeEnumDynamic));
    }
}

impl Type for TypeCodeEnumDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeEnumDynamic) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_code_enum_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_union_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectUnion>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to Code_ enum".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from Code_ enum".to_string())
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeEnumDynamic))
    }

    fn desc(&self) -> String {
        "Code_".to_string()
    }
}

impl Value for TypeCodeEnumDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeCodeEnumDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeEnumDynamic))
    }
}
