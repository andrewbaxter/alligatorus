use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectJsStackValue,
            AspectLiftJs,
            AspectNull,
            AspectSerial,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_null_aspect_id,
            aspect_null_get,
            aspect_serial_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeType,
        },
        langhelp::{
            Artifacts,
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            SerialValue as LangSerialValue,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
        },
        null::ValueNull,
    },
};

pub(crate) struct TypeMortarNull;

impl Value for TypeMortarNull {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeMortarNull {
    fn desc(&self) -> String {
        return format!("mortar null");
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_serial_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectSerial>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectLiftJs>));
        }
        if *key == aspect_null_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectNull>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNull) as Box<dyn AspectBindJs>));
        }
        return None;
    }

    fn cast_to(&self, ex: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            erb.must_er(aspect_null_get(ex, loc, type_))?;
            return Ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(ValueNull),
            });
        });
    }

    fn cast_from(&self, ex: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(ex, loc);
            erb.must_er(aspect_null_get(ex, loc, type_.as_ref()))?;
            return Ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(ValueNull),
            });
        });
    }
}

impl Aspect for TypeMortarNull { }

impl AspectJsStackValue for TypeMortarNull {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarNull),
            }),
        }
    }
}

impl AspectSerial for TypeMortarNull {
    fn to_serial(&self, _artifacts: &mut Artifacts, _value: Box<dyn Value>) -> EResult<LangSerialValue> {
        EResult::ok(LangSerialValue::Null)
    }
}

impl AspectLiftJs for TypeMortarNull {
    fn lift(&self, _scope: &v8::PinScope, _value: v8::Local<v8::Value>) -> Box<dyn Value> {
        Box::new(ValueNull)
    }
}

impl AspectNull for TypeMortarNull { }

impl AspectConsumeJs for TypeMortarNull {
    fn to_js(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        match cv.code.code {
            Code_::Dyn(_) => cv.code.cast::<JsCode>().map(|x| x.unwrap().clone()),
            Code_::None => EResult::ok(JsCode {
                loc,
                type_: JsCodeType::Value(serde_json::Value::Null),
            }),
        }
    }
}

impl AspectBindJs for TypeMortarNull {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}
