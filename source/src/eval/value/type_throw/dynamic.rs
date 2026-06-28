use {
    crate::{
        artifact::ExporterAny,
        code_dynamic::{
            CodeDynamic,
            CodeDynamicThrow,
            CodeDynamicType,
        },
        eval::{
            typecheck_convert,
            value::{
                type_binding::dynamic::BindingDynamicBinding,
                type_root::r#const::TypeRoot,
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

pub(crate) struct TypeThrowDynamic;

impl Aspect for TypeThrowDynamic { }

impl AspectFunction for TypeThrowDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let exc_type = match xx.current_exception_type.as_ref() {
                Some(t) => t.clone(),
                None => return erb.err_new(
                    "throw not allowed: enclosing function did not declare an exception type".to_string(),
                ),
            };
            let converted = erb.must_er(typecheck_convert(xx, loc, exc_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, exc_type.as_ref()))?;
            let value_code = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let throw_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::Throw(CodeDynamicThrow { value: Box::new(value_code) }),
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

impl AspectInitialBind for TypeThrowDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeThrowDynamic),
        })
    }
}

impl ExporterAny for TypeThrowDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeThrowDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeThrowDynamic));
    }
}

impl Type for TypeThrowDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeThrowDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeThrowDynamic) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeThrowDynamic) as Box<dyn AspectInitialBind>));
        }
        None
    }

    fn desc(&self) -> String {
        "throw".to_string()
    }
}

impl Value for TypeThrowDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeThrowDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::ThrowDynamic))
    }
}
