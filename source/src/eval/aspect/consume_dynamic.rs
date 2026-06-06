use {
    crate::{
        code_dynamic::CodeDynamic,
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            ExprContext,
            QExprId,
            Type,
            Unique,
        },
    },
    paste::paste,
};

aspect!(AspectConsumeDynamic {
    fn to_js(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult < CodeDynamic >;
});
