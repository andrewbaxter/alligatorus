use {
    crate::{
        builtin::{
            AspectConsumeJs,
            aspect_consume_js_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            EResult,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::expr::StagedExprCell,
};

pub(crate) struct TypeRefCell {
    pub type_: Rc<dyn Type>,
}

pub(crate) struct TypeRefValue(pub Rc<dyn Type>);

impl Value for TypeRefValue {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeType)
    }
}
pub(crate) struct TypeType;

fn type_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for TypeType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeType)
    }
}

impl Type for TypeType {
    fn desc(&self) -> String {
        "type".to_string()
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeType) as Box<dyn AspectConsumeJs>));
        }
        None
    }

    fn cast_to(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _cv: &crate::langhelp::CodeValue,
        _type_: &dyn Type,
    ) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::err(loc, "Cannot convert type".to_string())
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        let type_ = cv.value.type_(xx, loc);
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::None,
            },
            value: Box::new(TypeRefValue(type_)),
        })
    }
}

impl Aspect for TypeType { }

impl AspectConsumeJs for TypeType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<JsCode> {
        let type_ref_value =
            (cv.value.as_ref() as &dyn Any)
                .downcast_ref::<TypeRefValue>()
                .expect("TypeType value must be TypeRefValue");
        let cell =
            Rc::new(
                StagedExprCell {
                    value: std::cell::RefCell::new(
                        Some(Box::new(TypeRefValue(type_ref_value.0.clone())) as Box<dyn Value>),
                    ),
                },
            );
        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
        xx.vx.externals.borrow_mut().push((id, cell));
        EResult::ok(JsCode {
            loc,
            type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
        })
    }
}
