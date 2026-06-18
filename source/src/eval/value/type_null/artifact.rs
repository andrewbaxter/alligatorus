use {
    crate::{
        builtin::ArtifactDeserializer,
        eval::value::value_null::r#const::ValueNullConst,
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

pub(crate) type ArtifactDataNull = ();

pub(crate) struct DeserializerConst;

impl ArtifactDeserializer for DeserializerConst {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::r#const::TypeNullConst), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueNullConst) as Rc<dyn Value>,
        })
    }
}
pub(crate) struct DeserializerDynamic;

impl ArtifactDeserializer for DeserializerDynamic {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeNullDynamic), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueNullConst),
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactDeserializer>>) {
    m.insert(super::r#const::TypeNullConst::artifact_id(), Rc::new(DeserializerConst));
    m.insert(super::dynamic::TypeNullDynamic::artifact_id(), Rc::new(DeserializerDynamic));
}
