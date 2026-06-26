use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        eval::value::{
            type_root::r#const::TypeRoot,
            type_source_object_new::TypeSourceObjectNewConst,
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectType,
            aspect_type_aspect_id,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeTypeSourceObjectNewDynamic;

impl Aspect for TypeTypeSourceObjectNewDynamic { }

impl AspectType for TypeTypeSourceObjectNewDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let ctor = must_downcast_ref::<TypeSourceObjectNewConst>(value);
        Rc::new(TypeSourceObjectNewConst { data: ctor.data.clone() })
    }
}

impl ExporterAny for TypeTypeSourceObjectNewDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeTypeSourceObjectNewDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeTypeSourceObjectNewDynamic));
    }
}

impl Type for TypeTypeSourceObjectNewDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceObjectNewDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceObjectNewDynamic) as Box<dyn AspectType>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_object_new_type".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_object_new_type".to_string())
    }

    fn desc(&self) -> String {
        "definition_set_object_new_type".to_string()
    }
}

impl Value for TypeTypeSourceObjectNewDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeTypeSourceObjectNewDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::TypeSourceObjectNewDynamic))
    }
}
