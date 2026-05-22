use {
    super::{
        DefinitionSet,
        DefinitionSetFunction,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectFunction,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_function_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_lift_js_aspect_id,
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
        },
        mortar_null::TypeMortarNull,
        rc_from_v8,
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

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
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
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetFunction {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetFunction),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetFunction {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Box<dyn Value> {
        let func_ref: Rc<DefinitionSetFunction> = rc_from_v8(&value);
        func_ref.set.finish().expect("Definition set finish failed");
        Box::new(MortarJsValue {
            source: MortarJsValueSource::DefinitionSet(MortarJsValueDataDefinitionSet {
                set: func_ref.set.clone(),
                func_index: func_ref.func_index,
            }),
            type_: Rc::new(DefinitionSetFunction {
                set: func_ref.set.clone(),
                func_index: func_ref.func_index,
            }),
        })
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
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }
}

impl Aspect for DefinitionSetFunction { }


impl AspectFunction for DefinitionSetFunction {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Ensure scope entry IDs are allocated
        {
            let functions = self.set.functions.borrow();
            for func_data in functions.iter() {
                if func_data.scope_entry_id.get().is_none() {
                    let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                    func_data.scope_entry_id.set(Some(id));
                }
            }
        }

        // Register with eval context (dedup)
        {
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already = sets.iter().any(|s| std::ptr::eq(
                &**s as *const DefinitionSet,
                &*self.set as *const DefinitionSet,
            ));
            if !already {
                sets.push(self.set.clone());
            }
        }

        // Clone arg/ret type exprs (to release borrow before eval)
        let (scope_id, arg_type_expr, ret_type_expr) = {
            let functions = self.set.functions.borrow();
            let fd = &functions[self.func_index];
            let sid = fd.scope_entry_id.get()
                .expect("Definition set function not registered");
            let arg_e = fd.arg_type_expr.borrow().as_ref().map(|v| {
                (v.as_ref() as &dyn Any)
                    .downcast_ref::<crate::eval::expr::Expr>()
                    .expect("arg type must be an Expr")
                    .clone()
            });
            let ret_e = fd.ret_type_expr.borrow().as_ref().map(|v| {
                (v.as_ref() as &dyn Any)
                    .downcast_ref::<crate::eval::expr::Expr>()
                    .expect("ret type must be an Expr")
                    .clone()
            });
            (sid, arg_e, ret_e)
        };

        // Resolve arg/ret types by evaluating the type exprs
        return er_seq_cv!(loc, erb, {
            let arg_type: Option<Rc<dyn Type>> = match &arg_type_expr {
                Some(expr) => {
                    let r = crate::eval::eval_half_mortar(xx, expr);
                    let cv = erb.must_er(r)?;
                    Some(cv.value.type_(xx, loc))
                },
                None => None,
            };
            let ret_type: Option<Rc<dyn Type>> = match &ret_type_expr {
                Some(expr) => {
                    let r = crate::eval::eval_half_mortar(xx, expr);
                    let cv = erb.must_er(r)?;
                    Some(cv.value.type_(xx, loc))
                },
                None => None,
            };

            // Handle argument
            let mut js_args = vec![];
            if let Some(arg_type) = &arg_type {
                let arg_value_type = argument.value.type_(xx, loc);
                if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_value_type.as_ref()) {
                    let elements = arg_tuple.take_elements(argument.value);
                    if elements.len() != 1 {
                        return erb.err_new(
                            format!("Function expects 1 argument, got {}", elements.len())
                        );
                    }
                    let elem = elements.into_iter().next().unwrap();
                    let cv = match elem.v {
                        Some(cv) => cv,
                        None => {
                            erb.errors.extend(elem.errors);
                            return erb.err_new(format!("Argument failed to evaluate"));
                        },
                    };
                    erb.errors.extend(elem.errors);
                    let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), cv))?;
                    let to_js = erb.must_er(aspect_consume_js_get(xx, loc, arg_type.as_ref()))?;
                    let js = erb.must_er(to_js.to_js(xx, loc, converted))?;
                    js_args.push(js);
                } else {
                    let converted = erb.must_er(typecheck_convert(xx, loc, arg_type.as_ref(), argument))?;
                    let to_js = erb.must_er(aspect_consume_js_get(xx, loc, arg_type.as_ref()))?;
                    let js = erb.must_er(to_js.to_js(xx, loc, converted))?;
                    js_args.push(js);
                }
            }

            let ret_type_rc: Rc<dyn Type> = ret_type.unwrap_or_else(|| Rc::new(TypeMortarNull));
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
