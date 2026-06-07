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
    std::rc::Rc,
};

aspect!(AspectBindDynamicSub {
    fn bind_dynamic_sub(
        &self,
        xx: & ExprContext,
        loc: QExprId,
        cv: CodeValue
    ) -> EResult <(CodeDynamic, ScopeEntryDynamicSub) >;
});

pub(crate) struct ScopeEntryDynamicSub {
    pub type_: Rc<dyn Type>,
}
