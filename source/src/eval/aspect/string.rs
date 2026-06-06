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

aspect!(AspectString {
    fn get(&self, value: & dyn Value) -> String;
});
