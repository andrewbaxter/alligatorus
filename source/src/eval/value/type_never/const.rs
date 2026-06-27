use {
    crate::{
        artifact::ExporterAny,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Value,
        },
    },
    std::rc::Rc,
};

pub(crate) struct TypeNever;

impl Aspect for TypeNever { }

impl ExporterAny for TypeNever {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeNever));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeNever));
    }
}

impl Type for TypeNever {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNever)
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        true
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        return EvalRes::ok(CodeValue {
            code: Code {
                code: Code_::None,
                loc,
            },
            value: None,
        });
    }

    fn desc(&self) -> String {
        return format!("Never");
    }
}

impl Value for TypeNever {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeNever);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeNever));
    }
}
