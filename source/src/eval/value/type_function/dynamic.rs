use {
    crate::{
        artifact::ExporterAny,
        eval::{
            dynamic::{
                code::{
                    CodeDynamic,
                    CodeDynamicCall,
                    CodeDynamicType,
                },
                target::TargetBindSub,
                util::{
                    bind_dynamic_default,
                    bind_via_target,
                },
            },
            typecheck_convert,
            value::{
                type_binding::dynamic::BindingDynamicBinding,
                type_root::r#const::TypeRoot,
                type_tuple::Tuple,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
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
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectFunction,
            AspectInitialBind,
            Binding,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_function_get,
            aspect_function_get_maybe,
            aspect_initial_bind_aspect_id,
            aspect_null_get_maybe,
            aspect_stack_value_dynamic_get,
            aspect_tuple_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) fn dynamic_call(
    xx: &ExprContext,
    loc: QExprId,
    func_code: CodeDynamic,
    arg_type: Rc<dyn Type>,
    ret_type: Rc<dyn Type>,
    argument: CodeValue,
) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let arg_value_type = erb.must_er(argument.value.as_ref().unwrap().type_(xx, loc))?;
        let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, ret_type.as_ref()))?;
        if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_value_type.as_ref()) {
            let param_types = if let Some(param_tuple) = aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
                param_tuple.element_types()
            } else {
                vec![arg_type.clone()]
            };
            let elements = arg_tuple.take_elements(argument.value.unwrap());
            let (elements, param_types) =
                if elements.is_empty() && param_types.len() == 1 &&
                    aspect_null_get_maybe(xx, param_types[0].as_ref()).is_some() {
                    (vec![], vec![])
                } else {
                    (elements, param_types)
                };
            if elements.len() != param_types.len() {
                if elements.is_empty() && param_types.len() == 1 &&
                    aspect_tuple_get_maybe(xx, arg_type.as_ref()).is_none() {
                    let unit_cv = CodeValue {
                        code: Code {
                            loc,
                            code: Code_::None
                        },
                        value: Some(Rc::new(Tuple { elements: vec![], })),
                    };
                    let arg_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, arg_type.as_ref()))?;
                    let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), unit_cv))?;
                    let arg = erb.must_er(arg_to_dynamic.to_dynamic(xx, loc, converted))?;
                    return Ok(ret_stack.to_value(loc, CodeDynamic {
                        loc,
                        type_: CodeDynamicType::Call(CodeDynamicCall {
                            func: Box::new(func_code),
                            args: vec![arg],
                        }),
                    }));
                }
                return erb.err_new(
                    format!("Function expects {} arguments, got {}", param_types.len(), elements.len())
                );
            }
            let mut dynamic_args = vec![];
            for (elem, param_type) in elements.into_iter().zip(param_types.iter()) {
                let cv = match elem.v {
                    Some(cv) => cv,
                    None => {
                        if let Some(errs) = elem.errors {
                            erb.errors.extend(errs);
                        }
                        return erb.err_new(format!("Tuple element failed to evaluate"));
                    },
                };
                if let Some(errs) = elem.errors {
                    erb.errors.extend(errs);
                }
                let converted = erb.must_er(typecheck_convert(xx, loc, param_type.as_ref(), cv))?;
                let elem_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, param_type.as_ref()))?;
                let dynamic_code = erb.must_er(elem_to_dynamic.to_dynamic(xx, loc, converted))?;
                dynamic_args.push(dynamic_code);
            }
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(func_code),
                    args: dynamic_args,
                }),
            }));
        }
        let arg_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, arg_type.as_ref()))?;
        let argument = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), argument))?;
        let arg = erb.must_er(arg_to_dynamic.to_dynamic(xx, loc, argument))?;
        return Ok(ret_stack.to_value(loc, CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(func_code),
                args: vec![arg],
            }),
        }));
    });
}

#[derive(Clone)]
pub(crate) struct TypeFunctionDynamic {
    pub arg: Rc<dyn Type>,
    pub ret: Rc<dyn Type>,
}

impl Aspect for TypeFunctionDynamic { }

impl AspectBind for TypeFunctionDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeFunctionDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeFunctionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        return cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone());
    }
}

impl AspectFunction for TypeFunctionDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let func_type = erb.must_er(function.value.as_ref().unwrap().type_(xx, loc))?;
            let func_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, func_type.as_ref()))?;
            let func_code = erb.must_er(func_to_dynamic.to_dynamic(xx, loc, function))?;
            let result = erb.must_er(dynamic_call(xx, loc, func_code, self.arg.clone(), self.ret.clone(), argument))?;
            return Ok(result);
        });
    }

    fn exception_type(&self) -> Option<std::rc::Rc<dyn crate::langhelp::Type>> {
        None
    }
}

impl AspectInitialBind for TypeFunctionDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(self.clone()),
        })
    }
}

impl ExporterAny for TypeFunctionDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl Type for TypeFunctionDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeFunctionDynamic {
            arg: self.arg.clone(),
            ret: self.ret.clone(),
        })
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_function_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, ex: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let source_type = erb.must_er(cv.value.as_ref().unwrap().type_(ex, loc))?;
            erb.must_er(aspect_function_get(ex, loc, source_type.as_ref()))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(self.clone()),
                })),
            });
        });
    }

    fn cast_to(&self, _ex: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert dynamic function"))
    }

    fn desc(&self) -> String {
        return format!("dynamic func");
    }
}

impl Value for TypeFunctionDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::FunctionDynamic))
    }
}
