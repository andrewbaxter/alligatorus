use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        Type,
        Value,
    },
    std::rc::Rc,
};

pub(crate) struct ValueStringConst(pub String);

impl Value for ValueStringConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(super::super::type_string::r#const::TypeStringConst);
    }
}
