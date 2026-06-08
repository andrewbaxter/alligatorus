use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        Type,
        Value,
    },
    std::rc::Rc,
};

/// A generic value to be used by purely logical types (like const functions).
pub(crate) struct ValueStatic {
    pub type_: Rc<dyn Type>,
}

impl Value for ValueStatic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        self.type_.clone()
    }
}
