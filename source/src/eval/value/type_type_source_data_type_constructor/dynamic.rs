use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectType,
            aspect_artifact_aspect_id,
            aspect_type_aspect_id,
        },
        eval::value::type_source_data_type_constructor::{
            TypeSourceDataTypeConstructorConst,
            dynamic::deserialize_defset_ctor_as_type,
        },
        langhelp::{
            Artifact,
            Aspect,
            CodeValue,
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
        rc::Rc,
    },
};

pub(crate) struct TypeTypeSourceDataTypeConstructorDynamic;

generate_builtin_artifact!(TypeTypeSourceDataTypeConstructorDynamic, serde_json::Value);

impl Value for TypeTypeSourceDataTypeConstructorDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeTypeSourceDataTypeConstructorDynamic {
    fn desc(&self) -> String {
        "definition_set_data_type_constructor_type".to_string()
    }

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

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_data_type_constructor_type".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_constructor_type".to_string())
    }
}

impl Aspect for TypeTypeSourceDataTypeConstructorDynamic { }

impl AspectArtifact for TypeTypeSourceDataTypeConstructorDynamic {
    fn to_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let ctor =
            (self_.as_ref() as &dyn Any)
                .downcast_ref::<TypeSourceDataTypeConstructorConst>()
                .expect(
                    "TypeTypeSourceDataTypeConstructorDynamic::to_serial expects TypeSourceDataTypeConstructorConst",
                );
        let artifact_id = ctor.serialize_ctor(xx, gx, loc);
        EResult::ok(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }

    fn from_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        let type_ = deserialize_defset_ctor_as_type(xx, gx, loc, data)?;
        Ok(type_.as_value())
    }
}

impl AspectType for TypeTypeSourceDataTypeConstructorDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let ctor = (value as &dyn Any).downcast_ref::<TypeSourceDataTypeConstructorConst>().unwrap();
        Rc::new(TypeSourceDataTypeConstructorConst { data: ctor.data.clone() })
    }
}
