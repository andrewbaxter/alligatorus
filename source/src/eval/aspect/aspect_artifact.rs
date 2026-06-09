use {
    crate::langhelp::{
        Artifact,
        Aspect,
        EResult,
        EvalRes,
        ExprContext,
        QExprId,
        Type,
        Unique,
        Value,
    },
    paste::paste,
    std::rc::Rc,
};

aspect!(AspectArtifact {
    fn to_serial(
        &self,
        xx: & ExprContext,
        loc: QExprId,
        self_: Rc < dyn Value >,
    ) -> EResult < Artifact >;
    fn from_serial(
        &self,
        xx: & ExprContext,
        loc: QExprId,
        data: & serde_json:: Value,
    ) -> EvalRes;
});
