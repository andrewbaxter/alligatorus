use {
    crate::{
        code_dynamic::CodeDynamic,
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

aspect!(AspectBindDynamic {
    fn bind_js(
        &self,
        xx: & ExprContext,
        loc: QExprId,
        cv: CodeValue
    ) -> EResult <(Option < CodeDynamic >, Box < dyn DynamicScopeEntry >) >;
});
