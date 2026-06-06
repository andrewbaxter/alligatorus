use {
    crate::langhelp::{
        Aspect,
        EResult,
        ExprContext,
        QExprId,
        Type,
        Unique,
    },
    paste::paste,
    std::rc::Rc,
};

aspect!(AspectMortarMap {
    fn key_type(&self) -> Rc < dyn Type >;
    fn value_type(&self) -> Rc < dyn Type >;
});
