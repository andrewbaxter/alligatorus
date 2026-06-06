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

aspect!(AspectNumberConst {
    fn get(&self, value: & dyn Value) -> f64;
});
