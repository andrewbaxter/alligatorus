use {
    crate::artifact::ExporterAny,
    crate::langhelp::{
            EResult,
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

impl ExporterAny for ValueDynamic {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for ValueDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(self.type_.clone());
    }
}

#[derive(Clone)]
pub(crate) struct ValueDynamicDataAccess {
    pub key: String,
}

#[derive(Clone, Copy)]
pub(crate) struct ValueDynamicDataScope {
    pub key: ScopeEntryId,
}

#[derive(Clone)]
pub(crate) enum ValueDynamicSource {
    Binding(ValueDynamicDataScope),
    Expr,
    Field(ValueDynamicDataAccess),
}
