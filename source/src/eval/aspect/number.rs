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

aspect!(AspectNumber {
    fn get(&self, value: & dyn Value) -> f64;
});
