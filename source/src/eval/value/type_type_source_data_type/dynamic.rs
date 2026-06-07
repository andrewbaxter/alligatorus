use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectType,
            aspect_artifact_aspect_id,
            aspect_type_aspect_id,
        },
        eval::value::type_source_data::{
            TypeSourceDataTypeConst,
            artifact::ArtifactDataDefSetDataType,
            r#const::deserialize_definition_set,
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

pub(crate) struct TypeTypeSourceDataTypeDynamic;

generate_builtin_artifact!(TypeTypeSourceDataTypeDynamic, serde_json::Value);

impl Value for TypeTypeSourceDataTypeDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeTypeSourceDataTypeDynamic {
    fn desc(&self) -> String {
        "definition_set_data_type_type".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceDataTypeDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == crate::eval::value::type_any::r#const::type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeDynamic) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeDynamic) as Box<dyn AspectType>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_data_type_type".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_type".to_string())
    }
}

impl Aspect for TypeTypeSourceDataTypeDynamic { }

impl AspectArtifact for TypeTypeSourceDataTypeDynamic {
    fn to_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let dt =
            (self_.as_ref() as &dyn Any)
                .downcast_ref::<TypeSourceDataTypeConst>()
                .expect("TypeTypeSourceDataTypeDynamic::to_serial expects TypeSourceDataTypeConst");
        let artifact_id = dt.serialize_data_type(xx, gx, loc);
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
        let serial: ArtifactDataDefSetDataType =
            serde_json::from_value(data.clone()).map_err(|e| format!("Failed to parse defset data type: {}", e))?;
        let def_set_artifact = gx.artifacts.get(serial.def_set)?;
        let def_set = deserialize_definition_set(xx, gx, loc, &def_set_artifact)?;
        let data_types = def_set.data_types.borrow();
        let dt_data = data_types[serial.type_index].clone();
        drop(data_types);
        Ok(Rc::new(TypeSourceDataTypeConst { data: dt_data }))
    }
}

impl AspectType for TypeTypeSourceDataTypeDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let dt = (value as &dyn Any).downcast_ref::<TypeSourceDataTypeConst>().unwrap();
        Rc::new(TypeSourceDataTypeConst { data: dt.data.clone() })
    }
}
