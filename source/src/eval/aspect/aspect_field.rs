use {
    crate::langhelp::{
        Aspect,
        CodeValue,
        ConstKey,
        EResult,
        EvalRes,
        ExprContext,
        QExprId,
        Type,
        Unique,
    },
    paste::paste,
    std::rc::Rc,
};

aspect!(AspectField {
    fn field(&self, xx: & ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes;
    fn field_type_opt(&self, xx: & ExprContext, key: & ConstKey) -> Option < Rc < dyn Type >>;
});
