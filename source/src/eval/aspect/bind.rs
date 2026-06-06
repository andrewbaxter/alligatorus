use {
    crate::langhelp::{
        Aspect,
        CodeValue,
        EResult,
        ExprContext,
        QExprId,
        ScopeEntry,
        Type,
        Unique,
    },
    paste::paste,
};

aspect!(AspectBind {
    fn bind(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult < Box < dyn ScopeEntry > >;
});
