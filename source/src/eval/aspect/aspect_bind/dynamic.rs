use {
    crate::{
        eval::TargetBindSub,
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

aspect!(AspectBindDynamic {
    fn bind_dynamic(&self, xx: & ExprContext, loc: QExprId, cv: CodeValue) -> EResult < Option < TargetBindSub > >;
});
