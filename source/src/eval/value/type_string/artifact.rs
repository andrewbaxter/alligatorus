use {
    crate::{
        builtin::ArtifactBuiltinDeserializer,
        langhelp::{
            ArtifactIdBuiltin,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Value,
        },
    },
    std::{
        collections::HashMap,
        rc::Rc,
    },
};

pub(crate) type ArtifactDataString = String;

/// Deserializer for `TypeStringConst` artifacts — const string values.
pub(crate) struct DeserializerConst;

impl ArtifactBuiltinDeserializer for DeserializerConst {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::r#const::TypeStringConst), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        match data {
            serde_json::Value::String(s) => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(crate::eval::value::value_string::r#const::ValueStringConst(s.clone())) as
                    Rc<dyn Value>,
            }),
            _ => EvalRes::err(loc, "Expected string data for string type".to_string()),
        }
    }
}

/// Deserializer for `TypeStringDynamic` artifacts — dynamic string values (lifted
/// form).
pub(crate) struct DeserializerDynamic;

impl ArtifactBuiltinDeserializer for DeserializerDynamic {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeStringDynamic), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        let Some(s) = data.as_str() else {
            return EvalRes::err(loc, format!("Expected string data for dynamic string, got {:?}", data));
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(crate::eval::value::value_string::r#const::ValueStringConst(s.to_string())),
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(super::r#const::TypeStringConst::artifact_id(), Rc::new(DeserializerConst));
    m.insert(super::dynamic::TypeStringDynamic::artifact_id(), Rc::new(DeserializerDynamic));
}
