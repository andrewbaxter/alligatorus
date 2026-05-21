use {
    super::DefinitionSetInterface,
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
            JsCodeField,
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
        DefaultBindJs,
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

impl Value for DefinitionSetInterface {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(DefinitionSetInterface {
            set: self.set.clone(),
            interface_index: self.interface_index,
        })
    }
}

impl Type for DefinitionSetInterface {
    fn desc(&self) -> String {
        let interfaces = self.set.interfaces.borrow();
        let iface = &interfaces[self.interface_index];
        format!("interface(identity={})", iface.identity)
    }

    fn identity(&self) -> Option<usize> {
        let interfaces = self.set.interfaces.borrow();
        let iface = &interfaces[self.interface_index];
        Some(iface.identity)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetInterface {
                set: self.set.clone(),
                interface_index: self.interface_index,
            }) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetInterface {
                set: self.set.clone(),
                interface_index: self.interface_index,
            }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(DefaultBindJs) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetInterface {
                set: self.set.clone(),
                interface_index: self.interface_index,
            }) as Box<dyn AspectJsStackValue>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }
}

impl Aspect for DefinitionSetInterface { }

impl AspectConsumeJs for DefinitionSetInterface {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for DefinitionSetInterface {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(DefinitionSetInterface {
                    set: self.set.clone(),
                    interface_index: self.interface_index,
                }),
            }),
        }
    }
}

impl AspectField for DefinitionSetInterface {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());

            let (field_name, arg_type_expr_clone, ret_type_expr_clone) = {
                let interfaces = self.set.interfaces.borrow();
                let iface = &interfaces[self.interface_index];
                let methods = iface.methods.borrow();
                let method = methods.iter().find(|m| m.name == key_name);
                match method {
                    Some(m) => {
                        let arg_clone = m.arg_type_expr.borrow().as_ref().map(|v| {
                            (v.as_ref() as &dyn Any)
                                .downcast_ref::<crate::eval::expr::Expr>()
                                .expect("arg type must be an Expr")
                                .clone()
                        });
                        let ret_clone = m.ret_type_expr.borrow().as_ref().map(|v| {
                            (v.as_ref() as &dyn Any)
                                .downcast_ref::<crate::eval::expr::Expr>()
                                .expect("ret type must be an Expr")
                                .clone()
                        });
                        (m.field_name.clone(), arg_clone, ret_clone)
                    },
                    None => return erb.err_new(
                        format!("Field '{}' not found on interface", key_name)
                    ),
                }
            };

            let arg_type: Rc<dyn Type> = match arg_type_expr_clone {
                Some(expr) => {
                    let r = crate::eval::eval_half_mortar(xx, &expr);
                    let cv = erb.must_er(r)?;
                    cv.value.type_(xx, loc)
                },
                None => Rc::new(TypeMortarNull),
            };

            let ret_type: Rc<dyn Type> = match ret_type_expr_clone {
                Some(expr) => {
                    let r = crate::eval::eval_half_mortar(xx, &expr);
                    let cv = erb.must_er(r)?;
                    cv.value.type_(xx, loc)
                },
                None => Rc::new(TypeMortarNull),
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
                        type_: JsCodeType::Field(JsCodeField {
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
        let interfaces = self.set.interfaces.borrow();
        let iface = &interfaces[self.interface_index];
        let methods = iface.methods.borrow();
        for method in methods.iter() {
            if method.name == key {
                return Some(Rc::new(MortarTypeFunction {
                    arg: Rc::new(TypeMortarNull),
                    ret: Rc::new(TypeMortarNull),
                }));
            }
        }
        None
    }
}
