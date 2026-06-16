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

pub(crate) struct DeserializerRef;

impl ArtifactBuiltinDeserializer for DeserializerRef {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeSourceTaggedUnionRefDynamic), Code {
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
            value: Rc::new(super::dynamic::TypeSourceTaggedUnionRefDynamic) as Rc<dyn Value>,
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(super::dynamic::TypeSourceTaggedUnionRefDynamic::artifact_id(), Rc::new(DeserializerRef));
}
