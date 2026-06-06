use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        Type,
        Value,
    },
    std::rc::Rc,
};

pub(crate) struct ValueNumberConst(pub f64);

impl Value for ValueNumberConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(super::super::type_number::r#const::TypeNumberConst);
    }
}
