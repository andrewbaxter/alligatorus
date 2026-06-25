use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_bool::r#const::TypeBoolConst,
            type_root::r#const::TypeRoot,
        },
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

pub(crate) struct ValueTypeBoolConst;

impl ExporterAny for ValueTypeBoolConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeBoolConst))
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        EResult::ok(Rc::new(ValueTypeBoolConst))
    }
}

impl Value_ for ValueTypeBoolConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(ValueTypeBoolConst)
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::BoolConst))
    }
}
