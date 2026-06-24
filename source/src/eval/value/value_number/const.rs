use {
    crate::{
        artifact::ExporterAny,
        langhelp::{
            EResult,
            ExprContext,
            QExprId,
            Type,
            Value,
        },
    },
    std::rc::Rc,
};

#[derive(Clone, Copy)]
pub(crate) struct ValueNumberConst(pub f64);

impl ExporterAny for ValueNumberConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(*self));
    }
}

impl Value for ValueNumberConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(*self);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(super::super::type_number::r#const::TypeNumberConst));
    }
}
