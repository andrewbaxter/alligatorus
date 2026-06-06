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

aspect!(AspectStackValueDynamic {
    fn to_value(&self, loc: QExprId, self_: CodeDynamic) -> CodeValue;
});
