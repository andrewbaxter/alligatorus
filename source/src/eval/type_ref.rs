use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            CodeValue,
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
};

pub(crate) struct TypeAny;

pub(crate) fn type_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for TypeAny {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAny)
    }
}

impl Type for TypeAny {
    fn desc(&self) -> String {
        "type".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeAny)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeAny) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeAny) as Box<dyn AspectBindJs>));
        }
        None
    }

    fn cast_to(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _cv: crate::langhelp::CodeValue,
        _type_: &dyn Type,
    ) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::err(loc, "Cannot convert type".to_string())
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn crate::langhelp::Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        let type_ = cv.value.type_(xx, loc);
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_.clone())));
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
                })),
            },
            value: type_.as_value(),
        })
    }
}

impl Aspect for TypeAny { }

impl AspectConsumeJs for TypeAny {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<JsCode> {
        // If code already has JS (e.g. from Record/Tuple cast_to mortar case or
        // cast_from), pass through.
        if let Some(Some(js)) = cv.code.cast::<JsCode>().v {
            return EResult::ok(js.clone());
        }

        // Compile-time: create external from the value's type
        let type_ = cv.value.type_(xx, loc);
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_)));
        EResult::ok(JsCode {
            loc,
            type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
        })
    }
}

impl AspectBindJs for TypeAny {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        self.to_js(xx, loc, cv).map(|js| {
            super::mortar_js_value::bind_js_some(loc, js, Rc::new(TypeMortarAny))
        })
    }
}

// TypeMortarType — the mortar (v8) representation of a type value. In v8, types
// are always Rc`<dyn Type>` externals.
pub(crate) struct TypeMortarAny;

impl Value for TypeMortarAny {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAny)
    }
}

impl Type for TypeMortarAny {
    fn desc(&self) -> String {
        "mortar type".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeMortarAny)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarAny) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarAny) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarAny) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarAny) as Box<dyn AspectLiftJs>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::err(loc, "Cannot convert mortar type".to_string())
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        let type_ = cv.value.type_(xx, loc);
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_)));
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
                })),
            },
            value: Rc::new(super::mortar_js_value::MortarJsValue {
                source: super::mortar_js_value::MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarAny),
            }),
        })
    }
}

impl Aspect for TypeMortarAny { }

impl AspectConsumeJs for TypeMortarAny {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        super::mortar_js_value::mortar_consume_js_to_js(xx, loc, cv)
    }
}

impl AspectBindJs for TypeMortarAny {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        super::mortar_js_value::mortar_bind_js(xx, loc, cv)
    }
}

impl AspectJsStackValue for TypeMortarAny {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        super::mortar_js_value::mortar_js_stack_to_value(loc, value, Rc::new(TypeMortarAny))
    }
}

impl AspectLiftJs for TypeMortarAny {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Rc<dyn Value>, String> {
        let type_ = super::rc_type_from_v8(&*value);
        Ok(type_.as_value())
    }
}
