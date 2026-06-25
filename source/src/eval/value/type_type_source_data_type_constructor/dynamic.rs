use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        eval::value::{
            type_root::r#const::TypeRoot,
            type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
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
            Value_,
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

pub(crate) struct TypeTypeSourceDataTypeConstructorDynamic;

impl Aspect for TypeTypeSourceDataTypeConstructorDynamic { }

impl AspectType for TypeTypeSourceDataTypeConstructorDynamic {
    fn type_(&self, value: &dyn Value_) -> Rc<dyn Type> {
        let ctor = must_downcast_ref::<TypeSourceDataTypeConstructorConst>(value);
        Rc::new(TypeSourceDataTypeConstructorConst { data: ctor.data.clone() })
    }
}

impl ExporterAny for TypeTypeSourceDataTypeConstructorDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeTypeSourceDataTypeConstructorDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(TypeTypeSourceDataTypeConstructorDynamic));
    }
}

impl Type for TypeTypeSourceDataTypeConstructorDynamic {
    fn as_value(&self) -> Rc<dyn Value_> {
        Rc::new(TypeTypeSourceDataTypeConstructorDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeConstructorDynamic) as Box<dyn AspectType>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_constructor_type".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_data_type_constructor_type".to_string())
    }

    fn desc(&self) -> String {
        "definition_set_data_type_constructor_type".to_string()
    }
}

impl Value_ for TypeTypeSourceDataTypeConstructorDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeTypeSourceDataTypeConstructorDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::TypeSourceDataTypeConstructorDynamic))
    }
}
