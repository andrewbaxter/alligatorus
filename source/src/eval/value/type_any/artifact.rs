use {
    crate::{
        eval::artifact::{
            ArtifactExporter,
            ArtifactIdBuiltin,
        },
        langhelp::{
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

impl ArtifactExporter for DeserializerAnyConst {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::r#const::TypeAnyConst), Code {
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
            value: Rc::new(super::r#const::TypeAnyConst) as Rc<dyn Value>,
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactExporter>>) {
    m.insert(super::r#const::TypeAnyConst::artifact_id(), Rc::new(DeserializerAnyConst));
}
