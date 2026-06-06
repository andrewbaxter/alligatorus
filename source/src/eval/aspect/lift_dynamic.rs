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

aspect!(AspectLiftDynamic {
    fn lift(
        &self,
        xx: & ExprContext,
        scope: & v8:: PinScope,
        self_: v8:: Local < v8:: Value >,
    ) -> Result < Rc < dyn Value >,
    String >;
});
