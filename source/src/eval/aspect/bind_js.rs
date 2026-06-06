use {
    crate::{
        dynamic_code::DynamicCode,
        eval::value::value_scope_entry::dynamic::DynamicScopeEntry,
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

aspect!(AspectBindJs {
    fn bind_js(
        &self,
        xx: & ExprContext,
        loc: QExprId,
        cv: CodeValue
    ) -> EResult <(Option < DynamicCode >, Box < dyn DynamicScopeEntry >) >;
});
