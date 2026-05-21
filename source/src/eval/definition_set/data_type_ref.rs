use {
    super::{
        DefinitionSetDataType,
        DefinitionSetDataTypeRef,
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

pub(crate) struct MortarTypeDefinitionSetDataTypeRef;

impl Value for MortarTypeDefinitionSetDataTypeRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(MortarTypeDefinitionSetDataTypeRef)
    }
}

impl Type for MortarTypeDefinitionSetDataTypeRef {
    fn desc(&self) -> String {
        format!("definition_set_data_type_ref")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeRef) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeRef) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeRef) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_ref"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_ref"))
    }
}

impl Aspect for MortarTypeDefinitionSetDataTypeRef { }

impl AspectConsumeJs for MortarTypeDefinitionSetDataTypeRef {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetDataTypeRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetDataTypeRef),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetDataTypeRef {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Box<dyn Value> {
        let type_ref: Rc<DefinitionSetDataTypeRef> = rc_from_v8(&value);
        type_ref.set.finish().expect("Definition set finish failed");
        Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetDataType {
                set: type_ref.set.clone(),
                data_type_index: type_ref.data_type_index,
            }),
        })
    }
}
