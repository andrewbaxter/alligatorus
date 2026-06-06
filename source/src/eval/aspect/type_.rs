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
    std::rc::Rc,
};

aspect!(AspectType {
    fn type_(&self, value: & dyn Value) -> Rc < dyn Type >;
});
