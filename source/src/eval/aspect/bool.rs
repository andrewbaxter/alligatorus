use {
    crate::langhelp::{
        Aspect,
        EResult,
        ExprContext,
        QExprId,
        Type,
        Unique,
        Value,
    },
    paste::paste,
};

aspect!(AspectBool {
    fn get(&self, value: & dyn Value) -> bool;
});
