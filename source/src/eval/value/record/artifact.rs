use {
    crate::{
        builtin::ArtifactBuiltinDeserializer,
        eval::{
            util::deserialize_type_artifact,
            value::record::{
                Record,
                RecordField,
            },
        },
        langhelp::{
            Artifact,
            ArtifactIdBuiltin,
            Code,
            CodeValue,
            Code_,
            ConstKey,
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
        collections::{
            BTreeMap,
            HashMap,
        },
        rc::Rc,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactRecordType {
    pub fields: BTreeMap<ConstKey, Artifact>,
}
pub(crate) struct Deserializer;

impl Deserializer {
    fn decode(xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> Result<Rc<dyn Type>, Vec<ExprErr>> {
        let serial: ArtifactRecordType = match serde_json::from_value(data.clone()) {
            Ok(s) => s,
            Err(e) => {
                return Err(vec![ExprErr {
                    loc,
                    errors: vec![ExprSubErr::Opaque(format!("Failed to parse record type artifact: {}", e,))],
                }]);
            },
        };
        let mut fields: BTreeMap<ConstKey, RecordField> = BTreeMap::new();
        for (key, field_artifact) in serial.fields {
            let (field_type, _code) = deserialize_type_artifact(xx, loc, &field_artifact).into_res()?;
            fields.insert(key, RecordField::Dynamic(field_type));
        }
        Ok(Rc::new(Record {
            base: None,
            fields,
        }))
    }
}

impl ArtifactBuiltinDeserializer for Deserializer {
    fn type_from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        match Self::decode(xx, loc, data) {
            Ok(record_type) => EResult::ok((record_type, Code {
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
            Ok(record_type) => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: record_type.as_value(),
            }),
            Err(errs) => EvalRes {
                v: None,
                errors: Some(errs),
            },
        }
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(TypeRecordArtifact::artifact_id(), Rc::new(Deserializer));
}
pub(crate) struct TypeRecordArtifact;
