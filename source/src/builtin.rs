use {
    crate::langhelp::{
        Code,
        EResult,
        EvalRes,
        ExprContext,
        QExprId,
        Type,
    },
    std::rc::Rc,
};

pub(crate) trait ArtifactBuiltinDeserializer {
    fn type_from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)>;
    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes;
}
