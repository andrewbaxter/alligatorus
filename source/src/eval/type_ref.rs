use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectJsStackValue,
            AspectLiftJs,
            AspectType,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_type_aspect_id,
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

pub(crate) struct ValueType {
    pub type_: Rc<dyn Type>,
}

impl Value for ValueType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeType)
    }
}
pub(crate) struct TypeType;

pub(crate) fn type_type_identity() -> Unique {
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
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeAspectType) as Box<dyn AspectType>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeType) as Box<dyn AspectBindJs>));
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
            value: Box::new(ValueType { type_ }),
        })
    }
}

impl Aspect for TypeType { }
struct TypeTypeAspectType;

impl Aspect for TypeTypeAspectType { }

impl AspectType for TypeTypeAspectType {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let any: &dyn Any = value;
        any.downcast_ref::<ValueType>().expect("TypeTypeAspectType: value must be ValueType").type_.clone()
    }
}

impl AspectConsumeJs for TypeType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<JsCode> {
        // If code already has JS (e.g. from Record/Tuple cast_to mortar case or
        // cast_from), pass through.
        if let Some(Some(js)) = cv.code.cast::<JsCode>().v {
            return EResult::ok(js.clone());
        }

        // Compile-time: create external from ValueType
        let value_type =
            (cv.value.as_ref() as &dyn Any)
                .downcast_ref::<ValueType>()
                .expect("TypeType to_js: value must be ValueType");
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(value_type.type_.clone())));
        EResult::ok(JsCode {
            loc,
            type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
        })
    }
}

impl AspectBindJs for TypeType {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        self.to_js(xx, loc, cv).map(|js| {
            super::mortar_js_value::bind_js_some(loc, js, Rc::new(TypeMortarType))
        })
    }
}

// TypeMortarType — the mortar (v8) representation of a type value. In v8, types
// are always Rc`<dyn Type>` externals.
pub(crate) struct TypeMortarType;

impl Value for TypeMortarType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeType)
    }
}

impl Type for TypeMortarType {
    fn desc(&self) -> String {
        "mortar type".to_string()
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarType) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarType) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarType) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarType) as Box<dyn AspectLiftJs>));
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
        let vtype = cv.value.type_(xx, loc);

        // If source is a type value (AspectType), extract the inner type. Otherwise, use
        // the source's type directly.
        let type_ = match crate::builtin::aspect_type_get_maybe(xx, vtype.as_ref()) {
            Some(at) => at.type_(cv.value.as_ref()),
            None => vtype,
        };
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
            value: Box::new(super::mortar_js_value::MortarJsValue {
                source: super::mortar_js_value::MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarType),
            }),
        })
    }
}

impl Aspect for TypeMortarType { }

impl AspectConsumeJs for TypeMortarType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        super::mortar_js_value::mortar_consume_js_to_js(xx, loc, cv)
    }
}

impl AspectBindJs for TypeMortarType {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        super::mortar_js_value::mortar_bind_js(xx, loc, cv)
    }
}

impl AspectJsStackValue for TypeMortarType {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        super::mortar_js_value::mortar_js_stack_to_value(loc, value, Rc::new(TypeMortarType))
    }
}

impl AspectLiftJs for TypeMortarType {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let type_ = super::rc_type_from_v8(&*value);
        Ok(Box::new(ValueType { type_ }))
    }
}
