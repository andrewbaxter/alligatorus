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

pub(crate) struct ValueNullConst;

impl ExporterAny for ValueNullConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        EResult::ok(Rc::new(ValueNullConst))
    }
}

impl Value_ for ValueNullConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(ValueNullConst)
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(super::super::type_null::r#const::TypeNullConst));
    }
}
