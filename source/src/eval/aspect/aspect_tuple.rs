use {
    crate::langhelp::{
        Aspect,
        EResult,
        EvalRes,
        ExprContext,
        QExprId,
        Type,
        Unique,
        Value,
    },
    paste::paste,
    std::rc::Rc,
};

aspect!(AspectTuple {
    fn element_types(&self) -> Vec < Rc < dyn Type >>;
    fn take_elements(&self, value: Rc < dyn Value >) -> Vec < EvalRes >;
});
