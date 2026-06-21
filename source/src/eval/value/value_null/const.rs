use {
    crate::artifact::ExporterAny,
    crate::langhelp::{
            EResult,
        ExprContext,
        QExprId,
        Type,
        Value,
    },
    std::rc::Rc,
};

pub(crate) struct ValueNullConst;

impl ExporterAny for ValueNullConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for ValueNullConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(super::super::type_null::r#const::TypeNullConst));
    }
}
