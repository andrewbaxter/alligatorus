//! Builtin deserializer for `TypeAnyConst` — the singleton "any type" /
//! "type" type used as the type-of-types in many contexts.

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

pub(crate) struct DeserializerAnyConst;

impl ArtifactBuiltinDeserializer for DeserializerAnyConst {
    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(super::r#const::TypeAnyConst) as Rc<dyn Value>,
        })
    }

    fn type_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::r#const::TypeAnyConst), Code { loc, code: Code_::None }))
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(super::r#const::TypeAnyConst::artifact_id(), Rc::new(DeserializerAnyConst));
}
