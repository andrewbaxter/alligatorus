use {
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectJsStackValue,
            AspectLiftJs,
            AspectNull,
            AspectSerial,
            aspect_consume_js_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_null_aspect_id,
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
        rc::Rc,
    },
    super::{
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
        return Rc::new(TypeMortarNull);
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
        return None;
    }

    fn cast_to(&self, _ex: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert mortar null to other type"))
    }

    fn cast_from(&self, _ex: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to mortar null"))
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
    fn lift(&self, _value: &v8::Value) -> Box<dyn Value> {
        Box::new(ValueNull)
    }
}

impl AspectNull for TypeMortarNull { }

impl AspectConsumeJs for TypeMortarNull {
    fn to_js(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EResult<JsCode> {
        EResult::ok(JsCode {
            loc: loc,
            type_: JsCodeType::Value(serde_json::Value::Null),
        })
    }
}
