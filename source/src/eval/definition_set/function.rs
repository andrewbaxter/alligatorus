use {
    super::{
        DefinitionSet,
        DefinitionSetFunction,
    },
    crate::{
        builtin::{
            AspectBind,
            AspectBindJs,
            AspectConsumeJs,
            AspectFunction,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_bind_aspect_id,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_function_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_lift_js_aspect_id,
            aspect_null_get_maybe,
            aspect_tuple_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeCall,
            JsCodeType,
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
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
    },
    crate::eval::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataDefinitionSet,
            MortarJsValueSource,
            bind_js_some,
        },
        mortar_null::TypeMortarNull,
        from_v8_ref,
        typecheck_convert,
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

// --- MortarTypeDefinitionSetFunction: eval_half type of a function ref ---
pub(crate) struct MortarTypeDefinitionSetFunction;

impl Value for MortarTypeDefinitionSetFunction {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for MortarTypeDefinitionSetFunction {
    fn desc(&self) -> String {
        format!("definition_set_function")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetFunction) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetFunction) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetFunction) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetFunction) as Box<dyn AspectLiftJs>));
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

impl Aspect for MortarTypeDefinitionSetFunction { }

impl AspectConsumeJs for MortarTypeDefinitionSetFunction {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeDefinitionSetFunction {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn crate::eval::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetFunction {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetFunction),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetFunction {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let func_ref: Rc<DefinitionSetFunction> = from_v8_ref::<Rc<DefinitionSetFunction>>(&value).clone();
        Ok(Box::new(MortarJsValue {
            source: MortarJsValueSource::DefinitionSet(MortarJsValueDataDefinitionSet {
                set: func_ref.set.clone(),
                func_index: func_ref.func_index,
            }),
            type_: Rc::new(DefinitionSetFunction {
                set: func_ref.set.clone(),
                func_index: func_ref.func_index,
            }),
        }))
    }
}

// --- DefinitionSetFunction as Type: used after lift ---
impl Value for DefinitionSetFunction {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for DefinitionSetFunction {
    fn desc(&self) -> String {
        let functions = self.set.functions.borrow();
        let func_data = &functions[self.func_index];
        match func_data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_function"),
        }
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetFunction {
                set: self.set.clone(),
                func_index: self.func_index,
            }) as Box<dyn AspectFunction>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetFunction {
                set: self.set.clone(),
                func_index: self.func_index,
            }) as Box<dyn AspectBind>));
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

impl Aspect for DefinitionSetFunction { }

impl AspectBind for DefinitionSetFunction {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<Box<dyn ScopeEntry>> {
        EResult::ok(
            Box::new(crate::eval::tuple::ValueScopeEntry { value: Rc::from(cv.value) }) as Box<dyn ScopeEntry>,
        )
    }
}

impl AspectFunction for DefinitionSetFunction {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Register with eval context (dedup)
        {
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already =
                sets
                    .iter()
                    .any(|s| std::ptr::eq(&**s as *const DefinitionSet, &*self.set as *const DefinitionSet));
            if !already {
                sets.push(self.set.clone());
            }
        }

        // Get arg/ret types directly
        let (scope_id, arg_type, ret_type) = {
            let functions = self.set.functions.borrow();
            let fd = &functions[self.func_index];
            let sid = fd.scope_entry_id.get().expect("Definition set function not registered");
            let arg_type = fd.arg_type.borrow().clone().expect("Definition set function missing arg type");
            let ret_type = fd.ret_type.borrow().clone().expect("Definition set function missing return type");
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
                    let to_js = erb.must_er(aspect_consume_js_get(xx, loc, arg_type.as_ref()))?;
                    let js = erb.must_er(to_js.to_js(xx, loc, converted))?;
                    js_args.push(js);
                }
            } else {
                let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), argument))?;
                let to_js = erb.must_er(aspect_consume_js_get(xx, loc, arg_type.as_ref()))?;
                let js = erb.must_er(to_js.to_js(xx, loc, converted))?;
                js_args.push(js);
            }
            let ret_type_rc: Rc<dyn Type> = ret_type;
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, ret_type_rc.as_ref()))?;
            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::Call(JsCodeCall {
                    func: Box::new(JsCode {
                        loc,
                        type_: JsCodeType::BindingUse(JsCodeBindingUse { id: scope_id }),
                    }),
                    args: js_args,
                }),
            }));
        });
    }
}
