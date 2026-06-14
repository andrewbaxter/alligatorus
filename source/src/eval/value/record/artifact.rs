//! Record-type artifact: a per-field map of serialized field-type artifacts.
//! Both `TypeRecordArtifact` (the artifact-id holder) and the parameterless
//! `Deserializer` registered for that id live here.

use {
    crate::{
        builtin::ArtifactBuiltinDeserializer,
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

pub(crate) struct TypeRecordArtifact;

pub(crate) struct Deserializer;

impl Deserializer {
    fn decode(xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> Result<Rc<dyn Type>, Vec<crate::langhelp::ExprErr>> {
        let serial: ArtifactRecordType = match serde_json::from_value(data.clone()) {
            Ok(s) => s,
            Err(e) => {
                return Err(vec![crate::langhelp::ExprErr {
                    loc,
                    errors: vec![crate::langhelp::ExprSubErr::Opaque(format!(
                        "Failed to parse record type artifact: {}",
                        e,
                    ))],
                }]);
            },
        };
        let mut fields: BTreeMap<ConstKey, crate::eval::value::record::RecordField> = BTreeMap::new();
        for (key, field_artifact) in serial.fields {
            let (field_type, _code) =
                crate::eval::util::deserialize_type_artifact(xx, loc, &field_artifact).into_res()?;
            fields.insert(key, crate::eval::value::record::RecordField::Mortar(field_type));
        }
        Ok(Rc::new(crate::eval::value::record::Record { base: None, fields }))
    }
}

impl ArtifactBuiltinDeserializer for Deserializer {
    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        match Self::decode(xx, loc, data) {
            Ok(record_type) => EvalRes::ok(CodeValue {
                code: Code { loc, code: Code_::None },
                value: record_type.as_value(),
            }),
            Err(errs) => EvalRes { v: None, errors: Some(errs) },
        }
    }

    fn type_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EResult<(Rc<dyn Type>, Code)> {
        match Self::decode(xx, loc, data) {
            Ok(record_type) => EResult::ok((record_type, Code { loc, code: Code_::None })),
            Err(errs) => EResult { v: None, errors: Some(errs) },
        }
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(TypeRecordArtifact::artifact_id(), Rc::new(Deserializer));
}
