use {
    crate::{
        artifact::ExporterAny,
        eval::{
            PANIC_MARKER_KEY,
            dynamic::code::{
                CodeDynamic,
                CodeDynamicNewObject,
                CodeDynamicThrow,
                CodeDynamicType,
            },
            typecheck_convert,
            value::{
                type_binding::dynamic::BindingDynamicBinding,
                type_root::r#const::TypeRoot,
                type_string::dynamic::TypeStringDynamic,
            },
        },
        langhelp::{
            Aspect,
            BindingId,
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
        r#gen::{
            AspectFunction,
            AspectInitialBind,
            Binding,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypePanicDynamic;

impl Aspect for TypePanicDynamic { }

impl AspectFunction for TypePanicDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let string_type: Rc<dyn Type> = Rc::new(TypeStringDynamic);
            let converted = erb.must_er(typecheck_convert(xx, loc, string_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, string_type.as_ref()))?;
            let msg_code = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let marker_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::Literal(serde_json::Value::Bool(true)),
            };
            let panic_obj = CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(
                    CodeDynamicNewObject {
                        fields: vec![
                            (PANIC_MARKER_KEY.to_string(), marker_code),
                            ("message".to_string(), msg_code),
                        ],
                    }
                ),
            };
            let throw_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::Throw(CodeDynamicThrow { value: Box::new(panic_obj) }),
            };
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(throw_code)),
                },
                value: None,
            });
        });
    }

    fn exception_type(&self) -> Option<Rc<dyn Type>> {
        None
    }
}

impl AspectInitialBind for TypePanicDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypePanicDynamic),
        })
    }
}

impl ExporterAny for TypePanicDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypePanicDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypePanicDynamic));
    }
}

impl Type for TypePanicDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypePanicDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypePanicDynamic) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypePanicDynamic) as Box<dyn AspectInitialBind>));
        }
        None
    }

    fn desc(&self) -> String {
        "panic".to_string()
    }
}

impl Value for TypePanicDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypePanicDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::PanicDynamic))
    }
}
