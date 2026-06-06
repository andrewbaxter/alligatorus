use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        Type,
        Value,
    },
    std::rc::Rc,
};

/// Placeholder value for stage results. Its type exposes AspectLiftDynamic (to
/// extract the Expr from the v8::External returned by __stage) but NOT
/// AspectConsumeDynamic (so eval() preserves the Call CodeDynamic).
pub(crate) struct ValueStagedExprPlaceholder;

impl Value for ValueStagedExprPlaceholder {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_expr::TypeStagedExprPlaceholder)
    }
}
