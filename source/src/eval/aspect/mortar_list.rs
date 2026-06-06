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

aspect!(AspectMortarList {
    fn element_type(&self) -> Rc < dyn Type >;
});
