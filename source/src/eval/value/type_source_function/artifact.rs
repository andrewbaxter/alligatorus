use {
    crate::{
        eval::artifact::{
            ArtifactDeserializer,
            ArtifactIdBuiltin,
        },
        langhelp::{
            Code,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
        },
        r#gen::AspectArtifact,
    },
    std::{
        collections::HashMap,
        rc::Rc,
    },
};

pub(crate) struct Deserializer;

impl ArtifactDeserializer for Deserializer {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeTypeSourceFunctionDynamic), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        super::dynamic::TypeTypeSourceFunctionDynamic.from_serial(xx, loc, data)
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactDeserializer>>) {
    m.insert(super::dynamic::TypeTypeSourceFunctionDynamic::artifact_id(), Rc::new(Deserializer));
}
