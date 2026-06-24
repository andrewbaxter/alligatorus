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
pub(crate) struct ValueBoolConst(pub bool);

impl ExporterAny for ValueBoolConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(*self))
    }
}

impl Value for ValueBoolConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(*self)
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(super::super::type_bool::r#const::TypeBoolConst));
    }
}
