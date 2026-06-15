use {
    crate::{
        r#gen::{AspectArtifact, AspectType, aspect_artifact_aspect_id, aspect_type_aspect_id},
        eval::{
            Erb,
            definition_set::DefinitionSet,
            value::type_source_data_type::{
                SourceDataMethodData,
                SourceDataTypeData,
                TypeSourceDataTypeConst,
                artifact::{
                    ArtifactDataDefSet,
                    ArtifactDataDefSetDataType,
                },
            },
        },
        invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
        langhelp::{
            Artifact,
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
            deserialize_independent_artifact,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        mem,
        rc::Rc,
    },
};

pub(crate) struct TypeTypeSourceDataTypeDynamic;

impl Aspect for TypeTypeSourceDataTypeDynamic { }

impl AspectArtifact for TypeTypeSourceDataTypeDynamic {
    fn from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let serial: ArtifactDataDefSetDataType =
                erb.must_res(
                    serde_json::from_value(
                        data.clone(),
                    ).map_err(|e| format!("Failed to parse defset data type: {}", e)),
                )?;
            let def_set =
                erb.must_res(crate::eval::definition_set::load_def_set(xx, &serial.def_set))?;
            let data_types = def_set.data_types.borrow();
            let dt_data =
                data_types
                    .iter()
                    .find(|dt| dt.identity == serial.identity)
                    .expect("Data type not found in definition set")
                    .clone();
            drop(data_types);
            return Ok(CodeValue {
                code: Code { loc, code: Code_::None },
                value: Rc::new(TypeSourceDataTypeConst { data: dt_data }),
            });
        })
    }

    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn Value>) -> EResult<Artifact> {
        let dt = must_downcast_ref::<TypeSourceDataTypeConst>(self_.as_ref());
        let artifact_id = dt.serialize_data_type(xx, loc);
        EResult::ok(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }
}

impl AspectType for TypeTypeSourceDataTypeDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let dt = must_downcast_ref::<TypeSourceDataTypeConst>(value);
        Rc::new(TypeSourceDataTypeConst { data: dt.data.clone() })
    }
}

impl Type for TypeTypeSourceDataTypeDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceDataTypeDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == crate::r#gen::type_type_identity() {
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

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_type".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_data_type_type".to_string())
    }

    fn desc(&self) -> String {
        "definition_set_data_type_type".to_string()
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Artifact {
        Self::build_type_artifact()
    }
}

impl Value for TypeTypeSourceDataTypeDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
