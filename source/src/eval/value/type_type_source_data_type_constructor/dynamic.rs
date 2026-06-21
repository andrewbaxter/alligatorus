use {
    crate::artifact::ExporterAny,
    crate::artifact::Artifact,
    crate::{
        eval::{            value::{
                type_any::r#const::TypeAnyConst,
                type_source_data_type_constructor::{
                    TypeSourceDataTypeConstructorConst,
                    dynamic::deserialize_defset_ctor_as_type,
                },
            },
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
            AspectArtifact,
            AspectType,
            aspect_artifact_aspect_id,
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

impl AspectArtifact for TypeTypeSourceDataTypeConstructorDynamic {
    fn from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let (type_, type_codes) = erb.must_res(deserialize_defset_ctor_as_type(xx, loc, data))?;
            let code = erb.must_er(xx.target.seq(loc, type_codes))?;
            return Ok(CodeValue {
                code: code,
                value: type_.as_value(),
            });
        })
    }

    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn Value>) -> EResult<Artifact> {
        let ctor = must_downcast_ref::<TypeSourceDataTypeConstructorConst>(self_.as_ref());
        let artifact_id = ctor.serialize_ctor(xx, loc);
        EResult::ok(Artifact {
            deserializer: artifact_id,
            data: serde_json::Value::Null,
        })
    }
}

impl AspectType for TypeTypeSourceDataTypeConstructorDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let ctor = must_downcast_ref::<TypeSourceDataTypeConstructorConst>(value);
        Rc::new(TypeSourceDataTypeConstructorConst { data: ctor.data.clone() })
    }
}

impl Type for TypeTypeSourceDataTypeConstructorDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceDataTypeConstructorDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeConstructorDynamic) as Box<dyn AspectArtifact>));
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

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Artifact {
        Self::build_type_artifact()
    }
}

impl ExporterAny for TypeTypeSourceDataTypeConstructorDynamic {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for TypeTypeSourceDataTypeConstructorDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeAnyConst))
    }
}
