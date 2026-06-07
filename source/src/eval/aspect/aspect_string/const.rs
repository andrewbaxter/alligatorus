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

aspect!(AspectStringConst {
    fn get(&self, value: & dyn Value) -> String;
});
