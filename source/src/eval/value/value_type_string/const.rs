use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_root::r#const::TypeRoot,
            type_string::r#const::TypeStringConst,
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

pub(crate) struct ValueTypeStringConst;

impl ExporterAny for ValueTypeStringConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeStringConst))
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        EResult::ok(Rc::new(ValueTypeStringConst))
    }
}

impl Value_ for ValueTypeStringConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(ValueTypeStringConst)
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::StringConst))
    }
}
