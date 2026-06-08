use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectFunction,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_function_get,
            aspect_function_get_maybe,
            aspect_stack_value_dynamic_get,
            aspect_tuple_get_maybe,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            Erb,
            typecheck_convert,
            value::dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntry,
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
};

#[derive(Clone)]
pub(crate) struct TypeFunctionDynamic {
    pub arg: Rc<dyn Type>,
    pub ret: Rc<dyn Type>,
}

impl Aspect for TypeFunctionDynamic { }

impl AspectBind for TypeFunctionDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        crate::eval::value::dynamic_util::bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeFunctionDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        crate::eval::value::dynamic_util::bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeFunctionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        return cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone());
    }
}

impl AspectFunction for TypeFunctionDynamic {
    fn call(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        function: CodeValue,
        argument: CodeValue,
    ) -> crate::langhelp::EvalRes {
        return er_seq_cv!(loc, erb, {
            let func_type = function.value.type_(xx, loc);
            let arg_type = argument.value.type_(xx, loc);

            // Check if argument is a tuple
            if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
                let func_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, func_type.as_ref()))?;
                let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, self.ret.as_ref()))?;
                let func = erb.must_er(func_to_dynamic.to_dynamic(xx, loc, function))?;

                // Get parameter types from function arg type
                let param_types = if let Some(param_tuple) = aspect_tuple_get_maybe(xx, self.arg.as_ref()) {
                    param_tuple.element_types()
                } else {
                    vec![self.arg.clone()]
                };
                let elements = arg_tuple.take_elements(argument.value);

                // Empty tuple calling a null-arg function → zero JS args
                let (elements, param_types) =
                    if elements.is_empty() && param_types.len() == 1 &&
                        crate::builtin::aspect_null_get_maybe(xx, param_types[0].as_ref()).is_some() {
                        (vec![], vec![])
                    } else {
                        (elements, param_types)
                    };
                if elements.len() != param_types.len() {
                    return erb.err_new(
                        format!("Function expects {} arguments, got {}", param_types.len(), elements.len())
                    );
                }
                let mut js_args = vec![];
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
                    let js = erb.must_er(elem_to_dynamic.to_dynamic(xx, loc, converted))?;
                    js_args.push(js);
                }
                return Ok(ret_stack.to_value(loc, CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::Call(CodeDynamicCall {
                        func: Box::new(func),
                        args: js_args,
                    })
                }));
            }

            // Non-tuple argument path (original behavior)
            er_all!(erb, {
                func_to_dynamic = aspect_consume_dynamic_get(xx, loc, func_type.as_ref()),
                arg_to_dynamic = aspect_consume_dynamic_get(xx, loc, self.arg.as_ref()),
                ret_stack = aspect_stack_value_dynamic_get(xx, loc, self.ret.as_ref()),
                argument = typecheck_convert(xx, loc, self.arg.as_ref(), argument),
            });
            er_all!(erb, {
                func = func_to_dynamic.to_dynamic(xx, loc, function),
                arg = arg_to_dynamic.to_dynamic(xx, loc, argument),
            });
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(func),
                    args: vec![arg],
                })
            }));
        });
    }
}

impl Type for TypeFunctionDynamic {
    fn desc(&self) -> String {
        return format!("mortar func");
    }

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
        return None;
    }

    fn cast_to(&self, _ex: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert mortar function"))
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_function_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, ex: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let source_type = cv.value.type_(ex, loc);
            erb.must_er(aspect_function_get(ex, loc, source_type.as_ref()))?;
            return Ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        });
    }
}

impl Value for TypeFunctionDynamic {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
