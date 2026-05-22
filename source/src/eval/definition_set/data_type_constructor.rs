use {
    super::{
        DefinitionSet,
        DefinitionSetDataType,
        DefinitionSetDataTypeConstructor,
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
            JsCodeObject,
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
            MortarJsValueSource,
        },
        rc_from_v8,
        typecheck_convert,
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

// --- MortarTypeDefinitionSetDataTypeConstructor: eval_half type during JS execution ---

pub(crate) struct MortarTypeDefinitionSetDataTypeConstructor;

impl Value for MortarTypeDefinitionSetDataTypeConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for MortarTypeDefinitionSetDataTypeConstructor {
    fn desc(&self) -> String {
        format!("definition_set_data_type_constructor")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Aspect for MortarTypeDefinitionSetDataTypeConstructor { }

impl AspectConsumeJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectJsStackValue for MortarTypeDefinitionSetDataTypeConstructor {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetDataTypeConstructor),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Box<dyn Value> {
        let ctor_ref: Rc<DefinitionSetDataTypeConstructor> = rc_from_v8(&value);
        ctor_ref.set.finish().expect("Definition set finish failed");
        Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetDataTypeConstructor {
                set: ctor_ref.set.clone(),
                data_type_index: ctor_ref.data_type_index,
            }),
        })
    }
}

// --- DefinitionSetDataTypeConstructor as Type: used after lift ---

impl Value for DefinitionSetDataTypeConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for DefinitionSetDataTypeConstructor {
    fn desc(&self) -> String {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        match dt.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_data_type_constructor"),
        }
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataTypeConstructor {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectFunction>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Aspect for DefinitionSetDataTypeConstructor { }


impl AspectFunction for DefinitionSetDataTypeConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Ensure scope entry IDs are allocated for all functions and methods
        {
            let functions = self.set.functions.borrow();
            for func_data in functions.iter() {
                if func_data.scope_entry_id.get().is_none() {
                    let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                    func_data.scope_entry_id.set(Some(id));
                }
            }
        }
        {
            let data_types = self.set.data_types.borrow();
            for dt in data_types.iter() {
                for method in dt.methods.borrow().iter() {
                    if method.scope_entry_id.get().is_none() {
                        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                        method.scope_entry_id.set(Some(id));
                    }
                    if method.self_scope_entry_id.get().is_none() {
                        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                        method.self_scope_entry_id.set(Some(id));
                    }
                }
                for impl_data in dt.interface_impls.borrow().iter() {
                    for method_impl in &impl_data.method_impls {
                        if method_impl.scope_entry_id.get().is_none() {
                            let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                            method_impl.scope_entry_id.set(Some(id));
                        }
                        if method_impl.self_scope_entry_id.get().is_none() {
                            let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
                            method_impl.self_scope_entry_id.set(Some(id));
                        }
                    }
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

        let underlying_type = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            dt.underlying_type.borrow().as_ref()
                .expect("Data type missing underlying type")
                .clone()
        };

        return er_seq_cv!(loc, erb, {

            // Convert argument to underlying type
            let arg_value_type = argument.value.type_(xx, loc);
            let converted = if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_value_type.as_ref()) {
                let elements = arg_tuple.take_elements(argument.value);
                if elements.len() != 1 {
                    return erb.err_new(
                        format!("Constructor expects 1 argument, got {}", elements.len())
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
                erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), cv))?
            } else {
                erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?
            };
            let to_js = erb.must_er(aspect_consume_js_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_js.to_js(xx, loc, converted))?;

            // Build wrapper object fields
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let mut object_fields = vec![
                (dt.data_field.clone(), underlying_js),
            ];
            for method in dt.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get()
                    .expect("Method scope entry ID not allocated");
                object_fields.push((method.field_name.clone(), JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: method_id }),
                }));
            }
            for impl_data in dt.interface_impls.borrow().iter() {
                let interfaces = self.set.interfaces.borrow();
                let iface = &interfaces[impl_data.interface_index];
                let iface_methods = iface.methods.borrow();
                for method_impl in &impl_data.method_impls {
                    let iface_method = &iface_methods[method_impl.interface_method_index];
                    let method_id = method_impl.scope_entry_id.get()
                        .expect("Interface method impl scope entry ID not allocated");
                    object_fields.push((iface_method.field_name.clone(), JsCode {
                        loc,
                        type_: JsCodeType::BindingUse(JsCodeBindingUse { id: method_id }),
                    }));
                }
            }

            let result_type = Rc::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            });
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, result_type.as_ref()))?;

            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::Object(JsCodeObject { fields: object_fields }),
            }));
        });
    }
}
