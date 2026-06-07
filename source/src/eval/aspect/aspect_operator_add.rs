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

aspect!(AspectOperatorAdd {
    fn add(&self, xx: & ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes;
});
