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

aspect!(AspectFunction {
    fn call(&self, xx: & ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes;
});
