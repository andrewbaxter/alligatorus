use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_root::r#const::TypeRoot,
            type_source_object::r#const::TypeSourceObjectConst,
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
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

pub(crate) struct TypeTypeSourceObjectDynamic;

impl Aspect for TypeTypeSourceObjectDynamic { }

impl AspectType for TypeTypeSourceObjectDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let dt = must_downcast_ref::<TypeSourceObjectConst>(value);
        Rc::new(TypeSourceObjectConst { data: dt.data.clone() })
    }
}

impl ExporterAny for TypeTypeSourceObjectDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeTypeSourceObjectDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeTypeSourceObjectDynamic));
    }
}

impl Type for TypeTypeSourceObjectDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceObjectDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceObjectDynamic) as Box<dyn AspectType>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_object_type".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_object_type".to_string())
    }

    fn desc(&self) -> String {
        "definition_set_object_type".to_string()
    }
}

impl Value for TypeTypeSourceObjectDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeTypeSourceObjectDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::TypeSourceObjectDynamic))
    }
}
