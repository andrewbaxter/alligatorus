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

aspect!(AspectJsStackValue {
    fn to_value(&self, loc: QExprId, self_: DynamicCode) -> CodeValue;
});
