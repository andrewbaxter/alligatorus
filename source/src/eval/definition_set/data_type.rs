use {
    super::{
        DefinitionSetDataType,
        DefinitionSetInterface,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_string_get,
        },
        jscode::{
            JsCode,
            JsCodeFieldUse,
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
            Type,
            Unique,
            Value,
        },
    },
    crate::eval::{
        Erb,
        mortar_function::MortarTypeFunction,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
        },
        mortar_null::TypeMortarNull,
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

impl Value for DefinitionSetDataType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for DefinitionSetDataType {
    fn desc(&self) -> String {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        match dt.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("data_type(index={})", self.data_type_index),
        }
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        // Check own data type identity aspect
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            if *key == dt.identity {
                return Some(Box::new(true));
            }
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectJsStackValue>));
        }

        // Check interface identity aspects
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let interface_impls = dt.interface_impls.borrow();
            let interfaces = self.set.interfaces.borrow();
            for impl_data in interface_impls.iter() {
                let iface = &interfaces[impl_data.interface_index];
                if *key == iface.identity {
                    return Some(Box::new(true));
                }
            }
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        // Extract own identity + interface identities (drop borrows before calling
        // aspect())
        let (own_identity, iface_data) = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let own = dt.identity.clone();
            let interface_impls = dt.interface_impls.borrow();
            let interfaces = self.set.interfaces.borrow();
            let ifaces: Vec<(usize, crate::langhelp::Unique)> = interface_impls.iter().map(|impl_data| {
                let iface = &interfaces[impl_data.interface_index];
                (impl_data.interface_index, iface.identity.clone())
            }).collect();
            (own, ifaces)
        };

        // Same data type check
        if type_.aspect(xx, &own_identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(DefinitionSetDataType {
                        set: self.set.clone(),
                        data_type_index: self.data_type_index,
                    }),
                }),
            });
        }

        // Interface casting
        for (interface_index, identity) in iface_data {
            if type_.aspect(xx, &identity).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code.clone(),
                    value: Box::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(DefinitionSetInterface {
                            set: self.set.clone(),
                            interface_index,
                        }),
                    }),
                });
            }
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }
}

impl Aspect for DefinitionSetDataType { }

impl AspectConsumeJs for DefinitionSetDataType {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for DefinitionSetDataType {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectJsStackValue for DefinitionSetDataType {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(DefinitionSetDataType {
                    set: self.set.clone(),
                    data_type_index: self.data_type_index,
                }),
            }),
        }
    }
}

impl AspectField for DefinitionSetDataType {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());

            // Look up method by name - get types directly
            type MethodLookup = (String, Rc<dyn Type>, Rc<dyn Type>);
            let lookup_result: Option<MethodLookup> = 'lookup: {
                let data_types = self.set.data_types.borrow();
                let dt = &data_types[self.data_type_index];

                // Check direct methods first
                {
                    let methods = dt.methods.borrow();
                    if let Some(m) = methods.iter().find(|m| m.name == key_name) {
                        let ret_t = m.ret_type.borrow().clone().expect("Data type method missing return type");
                        break 'lookup Some((m.field_name.clone(), Rc::new(TypeMortarNull), ret_t));
                    }
                }

                // Check interface methods
                {
                    let interface_impls = dt.interface_impls.borrow();
                    for impl_data in interface_impls.iter() {
                        let interfaces = self.set.interfaces.borrow();
                        let iface = &interfaces[impl_data.interface_index];
                        let iface_methods = iface.methods.borrow();
                        if let Some(method) = iface_methods.iter().find(|m| m.name == key_name) {
                            let arg_t = method.arg_type.borrow().clone().expect("Interface method missing arg type");
                            let ret_t =
                                method.ret_type.borrow().clone().expect("Interface method missing return type");
                            break 'lookup Some((method.field_name.clone(), arg_t, ret_t));
                        }
                    }
                }
                None
            };
            let (field_name, arg_type, ret_type) = match lookup_result {
                Some(r) => r,
                None => return erb.err_new(format!("Field '{}' not found on data type", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            let field_type = Rc::new(MortarTypeFunction {
                arg: arg_type,
                ret: ret_type,
            });
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::FieldUse(JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: field_name,
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];

        // Check direct methods
        let methods = dt.methods.borrow();
        for method in methods.iter() {
            if method.name == key {
                return Some(Rc::new(MortarTypeFunction {
                    arg: Rc::new(TypeMortarNull),
                    ret: Rc::new(TypeMortarNull),
                }));
            }
        }
        drop(methods);

        // Check interface methods
        let interface_impls = dt.interface_impls.borrow();
        for impl_data in interface_impls.iter() {
            let interfaces = self.set.interfaces.borrow();
            let iface = &interfaces[impl_data.interface_index];
            let iface_methods = iface.methods.borrow();
            for method in iface_methods.iter() {
                if method.name == key {
                    return Some(Rc::new(MortarTypeFunction {
                        arg: Rc::new(TypeMortarNull),
                        ret: Rc::new(TypeMortarNull),
                    }));
                }
            }
        }
        None
    }
}
