use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        Type,
        Value,
    },
    std::rc::Rc,
};

pub(crate) struct ValueBoolConst(pub bool);

impl Value for ValueBoolConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return Rc::new(super::super::type_bool::r#const::TypeBoolConst);
    }
}
