use {
    crate::langhelp::{
        Aspect,
        ConstKey,
        EResult,
        ExprContext,
        QExprId,
        Type,
        Unique,
    },
    paste::paste,
    std::rc::Rc,
};

aspect!(AspectTaggedUnion {
    fn variants(&self) -> Vec <(ConstKey, String, Rc < dyn Type >) >;
});
