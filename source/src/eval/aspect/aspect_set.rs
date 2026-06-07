use {
    crate::langhelp::{
        Aspect,
        CodeValue,
        EResult,
        EvalRes,
        ExprContext,
        QExprId,
        Type,
        Unique,
    },
    paste::paste,
};

aspect!(AspectSet {
    fn set(&self, xx: & ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes;
});
