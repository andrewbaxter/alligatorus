use {
    crate::langhelp::{
        Aspect,
        EResult,
        ExprContext,
        QExprId,
        ScopeEntry,
        ScopeEntryId,
        Type,
        Unique,
    },
    paste::paste,
};

aspect!(AspectInitialBind {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box < dyn ScopeEntry >;
});
