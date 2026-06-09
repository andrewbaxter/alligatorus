use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectType,
            aspect_artifact_aspect_id,
            aspect_type_aspect_id,
        },
        eval::{
            Erb,
            value::type_source_data_type_constructor::{
                TypeSourceDataTypeConstructorConst,
                dynamic::deserialize_defset_ctor_as_type,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Artifact,
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            GlobalContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

generate_builtin_artifact!(TypeTypeSourceDataTypeConstructorDynamic, serde_json::Value);
pub(crate) struct TypeTypeSourceDataTypeConstructorDynamic;

impl Aspect for TypeTypeSourceDataTypeConstructorDynamic { }

impl AspectArtifact for TypeTypeSourceDataTypeConstructorDynamic {
    fn from_serial(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let type_ = erb.must_res(deserialize_defset_ctor_as_type(xx, gx, loc, data))?;
            return Ok(CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::None,
                },
                value: type_.as_value(),
            });
        })
    }

    fn to_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let ctor = must_downcast_ref::<TypeSourceDataTypeConstructorConst>(self_.as_ref());
        let artifact_id = ctor.serialize_ctor(xx, gx, loc);
        EResult::ok(Artifact {
            type_: artifact_id,
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
        if *key == crate::eval::value::type_any::r#const::type_type_identity() {
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

    fn type_to_serial(&self, _xx: &ExprContext, _gx: &GlobalContext, _loc: QExprId) -> Option<Artifact> {
        Some(Self::build_type_artifact())
    }
}

impl Value for TypeTypeSourceDataTypeConstructorDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
