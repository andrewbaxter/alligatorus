use {
    crate::{
        builtin::ArtifactDeserializer,
        eval::{
            util::deserialize_type_artifact,
            value::type_map::dynamic::TypeMapDynamic,
        },
        langhelp::{
            Artifact,
            ArtifactIdBuiltin,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
            Type,
        },
    },
    std::{
        collections::HashMap,
        rc::Rc,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactMapType {
    pub key_type: Artifact,
    pub value_type: Artifact,
}
pub(crate) struct Deserializer;

impl Deserializer {
    fn decode(xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> Result<Rc<dyn Type>, Vec<ExprErr>> {
        let serial: ArtifactMapType = match serde_json::from_value(data.clone()) {
            Ok(s) => s,
            Err(e) => {
                return Err(vec![ExprErr {
                    loc,
                    errors: vec![ExprSubErr::Opaque(format!("Failed to parse map type artifact: {}", e))],
                }]);
            },
        };
        let (key_type, _kc) = deserialize_type_artifact(xx, loc, &serial.key_type).into_res()?;
        let (value_type, _vc) = deserialize_type_artifact(xx, loc, &serial.value_type).into_res()?;
        Ok(Rc::new(TypeMapDynamic {
            key_type,
            value_type,
        }))
    }
}

impl ArtifactDeserializer for Deserializer {
    fn type_from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        match Self::decode(xx, loc, data) {
            Ok(map_type) => EResult::ok((map_type, Code {
                loc,
                code: Code_::None,
            })),
            Err(errs) => EResult {
                v: None,
                errors: Some(errs),
            },
        }
    }

    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        match Self::decode(xx, loc, data) {
            Ok(map_type) => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: map_type.as_value(),
            }),
            Err(errs) => EvalRes {
                v: None,
                errors: Some(errs),
            },
        }
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactDeserializer>>) {
    m.insert(TypeMapArtifact::artifact_id(), Rc::new(Deserializer));
}
pub(crate) struct TypeMapArtifact;
