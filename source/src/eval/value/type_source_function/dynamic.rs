use {
    crate::{
        builtin::{
            AspectBindConst,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectFunction,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_const_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_null_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
            aspect_stack_value_dynamic_get,
            aspect_tuple_get_maybe,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            Erb,
            definition_set::DefinitionSet,
            from_v8_ref,
            typecheck_convert,
            value::type_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_some,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
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
    super::TypeSourceFunctionConst,
};

// --- TypeSourceFunctionDynamic: eval_half type of a function ref ---
pub(crate) struct TypeSourceFunctionDynamic;

impl Value for TypeSourceFunctionDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceFunctionDynamic {
    fn desc(&self) -> String {
        format!("definition_set_function")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceFunctionDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectLiftDynamic>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }
}

impl Aspect for TypeSourceFunctionDynamic { }

impl AspectConsumeDynamic for TypeSourceFunctionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for TypeSourceFunctionDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
    }
}

impl AspectStackValueDynamic for TypeSourceFunctionDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceFunctionDynamic),
            }),
        }
    }
}

impl AspectLiftDynamic for TypeSourceFunctionDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let func_ref: Rc<TypeSourceFunctionConst> = from_v8_ref::<Rc<TypeSourceFunctionConst>>(&value).clone();
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::DefinitionSet,
            type_: Rc::new(TypeSourceFunctionConst { data: func_ref.data.clone() }),
        }))
    }
}

// --- TypeSourceFunctionConst as Type: used after lift ---
impl Value for TypeSourceFunctionConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceFunctionConst {
    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_function"),
        }
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceFunctionConst { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectFunction>),
            );
        }
        if *key == aspect_bind_const_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectBindConst>),
            );
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }
}

impl Aspect for TypeSourceFunctionConst { }

impl AspectBindConst for TypeSourceFunctionConst {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<Box<dyn ScopeEntry>> {
        EResult::ok(
            Box::new(crate::eval::value::type_tuple::ScopeEntryValue { value: Rc::from(cv.value) }) as
                Box<dyn ScopeEntry>,
        )
    }
}

impl AspectFunction for TypeSourceFunctionConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Register with eval context (dedup)
        {
            let set = &self.data.set;
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already =
                sets
                    .iter()
                    .any(
                        |s| std::ptr::eq(
                            &**s as *const crate::eval::definition_set::DefinitionSet,
                            &**set as *const crate::eval::definition_set::DefinitionSet,
                        ),
                    );
            if !already {
                sets.push(set.clone());
            }
        }

        // Get arg/ret types directly
        let (scope_id, arg_type, ret_type) = {
            let sid = self.data.scope_entry_id.get().expect("Definition set function not registered");
            let arg_type = self.data.arg_type.borrow().clone().expect("Definition set function missing arg type");
            let ret_type = self.data.ret_type.borrow().clone().expect("Definition set function missing return type");
            (sid, arg_type, ret_type)
        };
        return er_seq_cv!(loc, erb, {
            // Handle argument
            let mut js_args = vec![];
            let arg_value_type = argument.value.type_(xx, loc);
            if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_value_type.as_ref()) {
                let elements = arg_tuple.take_elements(argument.value);

                // Empty tuple calling a null-arg function → zero JS args
                let is_null_arg = aspect_null_get_maybe(xx, arg_type.as_ref()).is_some();
                if elements.is_empty() && is_null_arg {
                    // No args needed, no args provided
                } else if elements.len() != 1 {
                    return erb.err_new(format!("Function expects 1 argument, got {}", elements.len()));
                } else {
                    let elem = elements.into_iter().next().unwrap();
                    let cv = match elem.v {
                        Some(cv) => cv,
                        None => {
                            if let Some(errs) = elem.errors {
                                erb.errors.extend(errs);
                            }
                            return erb.err_new(format!("Argument failed to evaluate"));
                        },
                    };
                    if let Some(errs) = elem.errors {
                        erb.errors.extend(errs);
                    }
                    let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), cv))?;
                    let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, arg_type.as_ref()))?;
                    let js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
                    js_args.push(js);
                }
            } else {
                let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), argument))?;
                let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, arg_type.as_ref()))?;
                let js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
                js_args.push(js);
            }
            let ret_type_rc: Rc<dyn Type> = ret_type;
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, ret_type_rc.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: scope_id }),
                    }),
                    args: js_args,
                }),
            }));
        });
    }
}
