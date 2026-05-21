use {
    super::{
        DefinitionSetInterface,
        DefinitionSetInterfaceRef,
    },
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_consume_js_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
        },
        jscode::JsCode,
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
            Unique,
            Value,
        },
    },
    crate::eval::{
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
        },
        rc_from_v8,
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct MortarTypeDefinitionSetInterfaceRef;

impl Value for MortarTypeDefinitionSetInterfaceRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetInterfaceRef)
    }
}

impl Type for MortarTypeDefinitionSetInterfaceRef {
    fn desc(&self) -> String {
        format!("definition_set_interface_ref")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetInterfaceRef) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetInterfaceRef) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetInterfaceRef) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert interface_ref"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to interface_ref"))
    }
}

impl Aspect for MortarTypeDefinitionSetInterfaceRef { }

impl AspectConsumeJs for MortarTypeDefinitionSetInterfaceRef {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetInterfaceRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetInterfaceRef),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetInterfaceRef {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Box<dyn Value> {
        let iface_ref: Rc<DefinitionSetInterfaceRef> = rc_from_v8(&value);
        iface_ref.set.finish().expect("Definition set finish failed");
        Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetInterface {
                set: iface_ref.set.clone(),
                interface_index: iface_ref.interface_index,
            }),
        })
    }
}
