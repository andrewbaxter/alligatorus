use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        Type,
        Value,
    },
    std::rc::Rc,
};

pub(crate) struct ValueEmptyConst {
    pub type_: Rc<dyn Type>,
}

impl Value for ValueEmptyConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        self.type_.clone()
    }
}
