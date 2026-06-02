use {
    super::{
        DefinitionSet,
        DefinitionSetDataType,
        DefinitionSetDataTypeConstructor,
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
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeNewObject,
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
            MortarJsValueSource,
            bind_js_some,
        },
        from_v8_ref,
        typecheck_convert,
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

// --- MortarTypeDefinitionSetDataTypeConstructor: eval_half type during JS
// execution ---
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

    fn as_value(&self) -> Box<dyn Value> {
        Box::new(MortarTypeDefinitionSetDataTypeConstructor)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectConsumeJs>),
            );
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectJsStackValue>),
            );
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
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

impl AspectJsStackValue for MortarTypeDefinitionSetDataTypeConstructor {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetDataTypeConstructor),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let ctor_ref: Rc<DefinitionSetDataTypeConstructor> = from_v8_ref::<Rc<DefinitionSetDataTypeConstructor>>(&value).clone();
        ctor_ref.set.finish()?;
        Ok(Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetDataTypeConstructor {
                set: ctor_ref.set.clone(),
                data_type_index: ctor_ref.data_type_index,
            }),
        }))
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

    fn as_value(&self) -> Box<dyn Value> {
        Box::new(DefinitionSetDataTypeConstructor {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataTypeConstructor {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectFunction>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataTypeConstructor {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectBind>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Aspect for DefinitionSetDataTypeConstructor { }

impl AspectBind for DefinitionSetDataTypeConstructor {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<Box<dyn ScopeEntry>> {
        EResult::ok(
            Box::new(crate::eval::tuple::ValueScopeEntry { value: Rc::from(cv.value) }) as Box<dyn ScopeEntry>,
        )
    }
}

impl AspectFunction for DefinitionSetDataTypeConstructor {
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
        let underlying_type = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            dt.underlying_type.borrow().as_ref().expect("Data type missing underlying type").clone()
        };
        return er_seq_cv!(loc, erb, {
            // Convert argument to underlying type
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_js = erb.must_er(aspect_consume_js_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_js.to_js(xx, loc, converted))?;

            // Build wrapper object fields
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let mut object_fields = vec![(dt.data_field.clone(), underlying_js),];
            for method in dt.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
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
                    let method_id =
                        method_impl.scope_entry_id.get().expect("Interface method impl scope entry ID not allocated");
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
                type_: JsCodeType::NewObject(JsCodeNewObject { fields: object_fields }),
            }));
        });
    }
}
