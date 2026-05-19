use {
    super::DefinitionSetDataType,
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
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
        Rc::new(DefinitionSetDataType {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        })
    }
}

impl Type for DefinitionSetDataType {
    fn desc(&self) -> String {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        format!("data_type(identity={})", dt.identity)
    }

    fn identity(&self) -> Option<usize> {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        Some(dt.identity)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
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
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
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

impl Aspect for DefinitionSetDataType { }

impl AspectConsumeJs for DefinitionSetDataType {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectJsStackValue for DefinitionSetDataType {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
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

            // Look up method by name - extract data before calling eval
            let (field_name, ret_type_expr_clone) = {
                let data_types = self.set.data_types.borrow();
                let dt = &data_types[self.data_type_index];
                let methods = dt.methods.borrow();
                let method = methods.iter().find(|m| m.name == key_name);
                match method {
                    Some(m) => {
                        let ret_clone = m.ret_type_expr.borrow().as_ref().map(|v| {
                            (v.as_ref() as &dyn Any)
                                .downcast_ref::<crate::eval::expr::Expr>()
                                .expect("ret type must be an Expr")
                                .clone()
                        });
                        (m.field_name.clone(), ret_clone)
                    },
                    None => return erb.err_new(
                        format!("Field '{}' not found on data type", key_name)
                    ),
                }
            };

            // Resolve return type
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
                arg: Rc::new(TypeMortarNull),
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
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        let methods = dt.methods.borrow();
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
