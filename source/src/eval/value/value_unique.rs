use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        Type,
        Unique,
        Value,
    },
    std::rc::Rc,
};

impl Value for Unique {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        todo!()
    }
}
