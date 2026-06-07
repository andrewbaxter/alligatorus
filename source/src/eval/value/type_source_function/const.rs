use {
    crate::{
        builtin::{
            AspectBind,
            AspectConsumeDynamic,
            AspectFunction,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_null_get_maybe,
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
            typecheck_convert,
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
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectBind>),
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

impl AspectBind for TypeSourceFunctionConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok(
            (
                Code {
                    loc: loc,
                    code: Code_::None,
                },
                Box::new(crate::eval::value::type_tuple::ScopeEntryValue { value: Rc::from(cv.value) }) as
                    Box<dyn ScopeEntry>,
            ),
        )
    }
}

impl AspectFunction for TypeSourceFunctionConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
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
        let (scope_id, arg_type, ret_type) = {
            let sid = self.data.scope_entry_id.get().expect("Definition set function not registered");
            let arg_type = self.data.arg_type.borrow().clone().expect("Definition set function missing arg type");
            let ret_type = self.data.ret_type.borrow().clone().expect("Definition set function missing return type");
            (sid, arg_type, ret_type)
        };
        return er_seq_cv!(loc, erb, {
            let mut js_args = vec![];
            let arg_value_type = argument.value.type_(xx, loc);
            if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_value_type.as_ref()) {
                let elements = arg_tuple.take_elements(argument.value);
                let is_null_arg = aspect_null_get_maybe(xx, arg_type.as_ref()).is_some();
                if elements.is_empty() && is_null_arg { } else if elements.len() != 1 {
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
