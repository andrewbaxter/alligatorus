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
    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        match data.as_f64() {
            Some(n) => EvalRes::ok(CodeValue {
                code: Code { loc, code: Code_::None },
                value: Rc::new(crate::eval::value::value_number::r#const::ValueNumberConst(n))
                    as Rc<dyn Value>,
            }),
            None => EvalRes::err(loc, "Expected number data for number type".to_string()),
        }
    }

    fn type_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::r#const::TypeNumberConst), Code { loc, code: Code_::None }))
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(super::r#const::TypeNumberConst::artifact_id(), Rc::new(DeserializerConst));
}
