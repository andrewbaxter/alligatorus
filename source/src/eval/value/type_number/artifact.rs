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

pub(crate) struct DeserializerConst;

impl ArtifactBuiltinDeserializer for DeserializerConst {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::r#const::TypeNumberConst), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        match data.as_f64() {
            Some(n) => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(crate::eval::value::value_number::r#const::ValueNumberConst(n)) as Rc<dyn Value>,
            }),
            None => EvalRes::err(loc, "Expected number data for number type".to_string()),
        }
    }
}
pub(crate) struct DeserializerDynamic;

impl ArtifactBuiltinDeserializer for DeserializerDynamic {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeNumberDynamic), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        let Some(n) = data.as_f64() else {
            return EvalRes::err(loc, format!("Expected number data for mortar number, got {:?}", data));
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(crate::eval::value::value_number::r#const::ValueNumberConst(n)),
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(super::r#const::TypeNumberConst::artifact_id(), Rc::new(DeserializerConst));
    m.insert(super::dynamic::TypeNumberDynamic::artifact_id(), Rc::new(DeserializerDynamic));
}
