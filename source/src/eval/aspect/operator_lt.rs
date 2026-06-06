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

aspect!(AspectOperatorLt {
    fn lt(&self, xx: & ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes;
});
