use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        ScopeEntryId,
        Type,
        Value,
    },
    std::rc::Rc,
};

pub(crate) struct ValueDynamic {
    pub source: ValueDynamicSource,
    pub type_: Rc<dyn Type>,
}

impl Value for ValueDynamic {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return self.type_.clone();
    }
}

pub(crate) struct ValueDynamicDataAccess {
    pub key: String,
}

pub(crate) struct ValueDynamicDataScope {
    pub loc: QExprId,
    pub key: ScopeEntryId,
}

pub(crate) enum ValueDynamicSource {
    Binding(ValueDynamicDataScope),
    DefinitionSet,
    // Result of effect
    Expr,
    Field(ValueDynamicDataAccess),
}
