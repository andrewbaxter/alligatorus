use {
    crate::{
        artifact::{
            Artifact,
            ArtifactExporter,
            ArtifactIdBuiltin,
            deserialize_type_artifact,
        },
        eval::value::type_option::dynamic::TypeOptionDynamic,
        langhelp::{
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
pub(crate) struct ArtifactOptionType {
    pub element_type: Artifact,
}
pub(crate) struct Deserializer;

impl Deserializer {
    fn decode(xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> Result<Rc<dyn Type>, Vec<ExprErr>> {
        let serial: ArtifactOptionType = match serde_json::from_value(data.clone()) {
            Ok(s) => s,
            Err(e) => {
                return Err(vec![ExprErr {
                    loc,
                    errors: vec![ExprSubErr::Opaque(format!("Failed to parse option type artifact: {}", e))],
                }]);
            },
        };
        let (element_type, _ec) = deserialize_type_artifact(xx, loc, &serial.element_type).into_res()?;
        Ok(Rc::new(TypeOptionDynamic { element_type }))
    }
}

impl ArtifactExporter for Deserializer {
    fn type_from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        match Self::decode(xx, loc, data) {
            Ok(opt_type) => EResult::ok((opt_type, Code {
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
            Ok(opt_type) => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: opt_type.as_value(),
            }),
            Err(errs) => EvalRes {
                v: None,
                errors: Some(errs),
            },
        }
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactExporter>>) {
    m.insert(TypeOptionArtifact::artifact_id(), Rc::new(Deserializer));
}
pub(crate) struct TypeOptionArtifact;
