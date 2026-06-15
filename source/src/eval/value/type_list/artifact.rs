//! List-type artifact: serialized element type artifact.

use {
    crate::{
        builtin::ArtifactBuiltinDeserializer,
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
pub(crate) struct ArtifactListType {
    pub element_type: Artifact,
}

pub(crate) struct TypeListArtifact;

pub(crate) struct Deserializer;

impl Deserializer {
    fn decode(xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> Result<Rc<dyn Type>, Vec<ExprErr>> {
        let serial: ArtifactListType = match serde_json::from_value(data.clone()) {
            Ok(s) => s,
            Err(e) => {
                return Err(vec![ExprErr {
                    loc,
                    errors: vec![ExprSubErr::Opaque(format!("Failed to parse list type artifact: {}", e))],
                }]);
            },
        };
        let (element_type, _c) =
            crate::eval::util::deserialize_type_artifact(xx, loc, &serial.element_type).into_res()?;
        Ok(Rc::new(crate::eval::value::type_list::dynamic::TypeListDynamic { element_type }))
    }
}

impl ArtifactBuiltinDeserializer for Deserializer {
    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        match Self::decode(xx, loc, data) {
            Ok(list_type) => EvalRes::ok(CodeValue {
                code: Code { loc, code: Code_::None },
                value: list_type.as_value(),
            }),
            Err(errs) => EvalRes { v: None, errors: Some(errs) },
        }
    }

    fn type_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EResult<(Rc<dyn Type>, Code)> {
        match Self::decode(xx, loc, data) {
            Ok(list_type) => EResult::ok((list_type, Code { loc, code: Code_::None })),
            Err(errs) => EResult { v: None, errors: Some(errs) },
        }
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(TypeListArtifact::artifact_id(), Rc::new(Deserializer));
}
