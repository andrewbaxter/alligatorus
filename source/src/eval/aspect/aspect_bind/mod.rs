pub mod dynamic;

use {
    crate::langhelp::{
        Aspect,
        Code,
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
    fn bind(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult <(Code, Box < dyn ScopeEntry >) >;
});
