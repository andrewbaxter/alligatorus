use {
    crate::{
        artifact::ExporterAny,
        langhelp::{
            EResult,
            ExprContext,
            QExprId,
            Type,
            Value_,
        },
    },
    std::rc::Rc,
};

#[derive(Clone)]
pub(crate) struct ValueEmptyConst {
    pub type_: Rc<dyn Type>,
}

impl ExporterAny for ValueEmptyConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        EResult::ok(Rc::new(self.clone()))
    }
}

impl Value_ for ValueEmptyConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(self.clone())
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(self.type_.clone())
    }
}
