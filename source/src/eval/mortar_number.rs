use {
    super::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataAccess,
            MortarJsValueSource,
        },
        mortar_function::MortarTypeFunction,
        mortar_null::TypeMortarNull,
        mortar_string::TypeMortarString,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectJsNumber,
            AspectJsStackValue,
            AspectOperatorAdd,
            AspectOperatorLt,
            AspectSet,
            aspect_bind_js_aspect_id,
            aspect_set_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_field_aspect_id,
            aspect_js_number_aspect_id,
            aspect_js_number_get_maybe,
            aspect_js_stack_value_aspect_id,
            aspect_number_get,
            aspect_number_get_maybe,
            aspect_operator_add_aspect_id,
            aspect_operator_lt_aspect_id,
        },
        eval::expr,
        jscode::{
            JsCode,
            JsCodeBindingSet,
            JsCodeFieldUse,
            JsCodeFieldSet,
            JsCodeOperatorAdd,
            JsCodeOperatorLt,
            JsCodeBlock,
            JsCodeBlockEntry,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            Code,
            Code_,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
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

pub(crate) struct TypeMortarNumber;

impl Value for TypeMortarNumber {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeMortarNumber {
    fn desc(&self) -> String {
        return format!("mortar number");
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_js_number_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectJsNumber>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectField>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_operator_lt_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectOperatorLt>));
        }
        if *key == aspect_operator_add_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectOperatorAdd>));
        }
        if *key == aspect_set_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarNumber) as Box<dyn AspectSet>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            if aspect_js_number_get_maybe(xx, type_).is_none() {
                return erb.err_new(format!("Type ({}) missing AspectJsNumber", type_.desc()));
            }
            return Ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(TypeMortarNumber),
                })
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            return Ok(erb.must_er(er_one!(loc, "searching for number aspects", {
                aspect_number = aspect_number_get_maybe(xx, type_.as_ref()) => er_seq_cv !(loc, erb, {
                    let CodeValue { code, value } = cv;
                    let mut seq = vec![];
                    if let Some(c) = erb.must_er(code.cast::<JsCode>())? {
                        seq.push(c.clone());
                    };
                    let num_val = aspect_number.get(value.as_ref());
                    seq.push(JsCode {
                        loc: loc,
                        type_: JsCodeType::Literal(serde_json::Value::from(num_val))
                    });
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::Block(
                                    JsCodeBlock { entries: seq.into_iter().map(JsCodeBlockEntry::Code).collect() }
                                )
                            }))
                        },
                        value: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarNumber),
                        }),
                    });
                }),
                _x = aspect_js_number_get_maybe(xx, type_.as_ref()) => er_seq_cv !(loc, erb, {
                    let CodeValue { code, value: _value } = cv;
                    return Ok(CodeValue {
                        code: code.clone(),
                        value: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarNumber),
                        })
                    });
                }),
            }))?);
        });
    }
}

impl Aspect for TypeMortarNumber { }

impl AspectJsNumber for TypeMortarNumber { }

impl AspectJsStackValue for TypeMortarNumber {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarNumber),
            }),
        }
    }
}

impl AspectConsumeJs for TypeMortarNumber {
    fn to_js(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        let r = cv.code.cast::<JsCode>();
        if let Some(c) = r.v {
            if let Some(c) = c {
                return EResult {
                    v: Some(c.clone()),
                    errors: r.errors,
                };
            } else {
                let mut errors = r.errors;
                errors.push(ExprErr {
                    loc: loc,
                    errors: vec![ExprSubErr::Opaque(format!("Expression resulted in empty code"))],
                });
                return EResult {
                    v: None,
                    errors: errors,
                }
            }
        } else {
            return EResult {
                v: None,
                errors: r.errors,
            };
        }
    }
}

impl AspectBindJs for TypeMortarNumber {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

fn js_field_name(key: &str) -> &str {
    match key {
        "to_string" => "toString",
        other => other,
    }
}

impl AspectField for TypeMortarNumber {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(crate::builtin::aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on mortar number", key_name)),
            };
            let js_name = js_field_name(&key_name).to_string();
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc: loc,
                        type_: JsCodeType::FieldUse(JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: js_name.clone(),
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Field(MortarJsValueDataAccess {
                        loc: loc,
                        base: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarNumber),
                        }),
                        key: js_name,
                    }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "to_string" => Some(Rc::new(MortarTypeFunction {
                arg: Rc::new(TypeMortarNull),
                ret: Rc::new(TypeMortarString),
            })),
            _ => None,
        }
    }
}

impl AspectSet for TypeMortarNumber {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let value_type = value.value.type_(xx, loc);
            er_all!(erb, {
                value_to_js = aspect_consume_js_get(xx, loc, value_type.as_ref()),
                self_code = self_.code.cast::<JsCode>(),
            });
            let value_code = erb.must_er(value_to_js.to_js(xx, loc, value))?;
            let v = (self_.value.as_ref() as &dyn std::any::Any).downcast_ref::<MortarJsValue>().unwrap();
            match &v.source {
                MortarJsValueSource::Field(b) => {
                    let Some(self_code) = self_code else {
                        return erb.err_new(
                            format!("TODO assertion set left hand doesn't evaluate to js code (empty)")
                        );
                    };

                    // Extract the base from FieldUse so FieldSet targets the parent object
                    let base_code = match &self_code.type_ {
                        JsCodeType::FieldUse(fu) => *fu.base.clone(),
                        _ => self_code.clone(),
                    };
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::FieldSet(JsCodeFieldSet {
                                    base: Box::new(base_code),
                                    key: b.key.clone(),
                                    value: Box::new(value_code)
                                })
                            }))
                        },
                        value: Box::new(super::null::ValueNull),
                    })
                },
                MortarJsValueSource::Binding(b) => {
                    let mut seq = vec![];
                    if let Some(self_code) = self_code {
                        seq.push(self_code.clone());
                    }
                    seq.push(JsCode {
                        loc: loc,
                        type_: JsCodeType::BindingSet(JsCodeBindingSet {
                            base: b.key,
                            value: Box::new(value_code),
                        })
                    });
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::Block(
                                    JsCodeBlock { entries: seq.into_iter().map(JsCodeBlockEntry::Code).collect() }
                                )
                            }))
                        },
                        value: Box::new(super::null::ValueNull),
                    })
                },
                _ => {
                    return erb.err_new(format!("Cannot set on this number value"));
                },
            }
        });
    }
}

impl AspectOperatorLt for TypeMortarNumber {
    fn lt(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeJs::to_js(&TypeMortarNumber, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeMortarNumber.cast_from(xx, loc, &reference))?;
            let ref_code = erb.must_er(AspectConsumeJs::to_js(&TypeMortarNumber, xx, loc, ref_cv))?;
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::OperatorLt(JsCodeOperatorLt {
                            base: Box::new(base_code),
                            reference: Box::new(ref_code),
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(super::mortar_bool::TypeMortarBool),
                }),
            });
        });
    }
}

impl AspectOperatorAdd for TypeMortarNumber {
    fn add(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeJs::to_js(&TypeMortarNumber, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeMortarNumber.cast_from(xx, loc, &reference))?;
            let ref_code = erb.must_er(AspectConsumeJs::to_js(&TypeMortarNumber, xx, loc, ref_cv))?;
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::OperatorAdd(JsCodeOperatorAdd {
                            base: Box::new(base_code),
                            reference: Box::new(ref_code),
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(TypeMortarNumber),
                }),
            });
        });
    }
}
