use {
    crate::langhelp::{
        Aspect,
        CodeValue,
        EResult,
        EvalRes,
        ExprContext,
        QExprId,
        ScopeEntryId,
        Type,
        Unique,
    },
    paste::paste,
    std::rc::Rc,
};

aspect!(AspectIterable {
    fn element_type(&self) -> Rc < dyn Type >;
    fn foreach(
        &self,
        xx: & ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ScopeEntryId,
        body: CodeValue,
    ) -> EvalRes;
});
