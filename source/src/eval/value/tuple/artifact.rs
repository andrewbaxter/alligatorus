use {
    crate::artifact::Artifact,
    crate::artifact::ArtifactExporter,
    crate::artifact::ArtifactIdBuiltin,
    crate::artifact::deserialize_type_artifact,
    crate::{
        eval::{            value::tuple::{
                Tuple,
                TupleElement,
            },
        },
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
pub(crate) struct ArtifactTupleType {
    pub elements: Vec<Artifact>,
}
pub(crate) struct Deserializer;

impl Deserializer {
    fn decode(xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> Result<Rc<dyn Type>, Vec<ExprErr>> {
        let serial: ArtifactTupleType = match serde_json::from_value(data.clone()) {
            Ok(s) => s,
            Err(e) => {
                return Err(vec![ExprErr {
                    loc,
                    errors: vec![ExprSubErr::Opaque(format!("Failed to parse tuple type artifact: {}", e))],
                }]);
            },
        };
        let mut elements: Vec<TupleElement> = Vec::new();
        for elem_artifact in serial.elements {
            let (elem_type, _code) = deserialize_type_artifact(xx, loc, &elem_artifact).into_res()?;
            elements.push(TupleElement::Dynamic(elem_type));
        }
        Ok(Rc::new(Tuple { elements: elements }))
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
            Ok(tuple_type) => EResult::ok((tuple_type, Code {
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
            Ok(tuple_type) => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: tuple_type.as_value(),
            }),
            Err(errs) => EvalRes {
                v: None,
                errors: Some(errs),
            },
        }
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactExporter>>) {
    m.insert(TypeTupleArtifact::artifact_id(), Rc::new(Deserializer));
}
pub(crate) struct TypeTupleArtifact;
