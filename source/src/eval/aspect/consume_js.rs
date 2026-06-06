use {
    crate::{
        dynamic_code::DynamicCode,
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

aspect!(AspectConsumeJs {
    fn to_js(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult < DynamicCode >;
});
