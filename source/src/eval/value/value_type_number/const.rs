use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_number::r#const::TypeNumberConst,
            type_root::r#const::TypeRoot,
        },
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

pub(crate) struct ValueTypeNumberConst;

impl ExporterAny for ValueTypeNumberConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeNumberConst))
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(ValueTypeNumberConst))
    }
}

impl Value for ValueTypeNumberConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(ValueTypeNumberConst)
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::NumberConst))
    }
}
