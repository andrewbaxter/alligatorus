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
        null::ValueNull,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
            AspectJsString,
            AspectSet,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_field_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_string_aspect_id,
            aspect_js_string_get,
            aspect_js_string_get_maybe,
            aspect_string_get,
            aspect_string_get_maybe,
        },
        eval::expr,
        jscode::{
            JsCode,
            JsCodeBindingSet,
            JsCodeFieldUse,
            JsCodeFieldSet,
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

pub(crate) struct TypeMortarString;

impl Value for TypeMortarString {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeMortarString {
    fn desc(&self) -> String {
        return format!("mortar string");
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_js_string_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectJsString>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectField>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectJsStackValue>));
        }
        return None;
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            erb.must_er(aspect_js_string_get(xx, loc, type_))?;
            return Ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(TypeMortarString),
                })
            });
        });
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            return Ok(erb.must_er(er_one!(loc, "searching for string aspects", {
                aspect_string = aspect_string_get_maybe(xx, type_.as_ref()) => er_seq_cv !(loc, erb, {
                    let CodeValue { code, value } = cv;
                    let mut seq = vec![];
                    if let Some(c) = erb.must_er(code.cast::<JsCode>())? {
                        seq.push(c.clone());
                    };
                    seq.push(JsCode {
                        loc: loc,
                        type_: JsCodeType::Literal(serde_json::Value::String(aspect_string.get(value.as_ref())))
                    });
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::Block(JsCodeBlock { entries: seq.into_iter().map(JsCodeBlockEntry::Code).collect() })
                            }))
                        },
                        value: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarString),
                        }),
                    });
                }),
                _x = aspect_js_string_get_maybe(xx, type_.as_ref()) => er_seq_cv !(loc, erb, {
                    let CodeValue { code, value: _value } = cv;
                    return Ok(CodeValue {
                        code: code.clone(),
                        value: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarString),
                        })
                    });
                }),
            }))?);
        });
    }
}

impl Aspect for TypeMortarString { }

impl AspectJsString for TypeMortarString { }

impl AspectJsStackValue for TypeMortarString {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarString),
            }),
        }
    }
}

impl AspectConsumeJs for TypeMortarString {
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

impl AspectBindJs for TypeMortarString {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectField for TypeMortarString {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on mortar string", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc: loc,
                        type_: JsCodeType::FieldUse(JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: key_name.clone(),
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Field(MortarJsValueDataAccess {
                        loc: loc,
                        base: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(TypeMortarString),
                        }),
                        key: key_name,
                    }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "toLowerCase" => Some(Rc::new(MortarTypeFunction {
                arg: Rc::new(TypeMortarNull),
                ret: Rc::new(TypeMortarString),
            })),
            "toUpperCase" => Some(Rc::new(MortarTypeFunction {
                arg: Rc::new(TypeMortarNull),
                ret: Rc::new(TypeMortarString),
            })),
            _ => None,
        }
    }
}

impl AspectSet for TypeMortarString {
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
                    return Ok(CodeValue {
                        code: Code {
                            loc: loc,
                            code: Code_::Dyn(Rc::new(JsCode {
                                loc: loc,
                                type_: JsCodeType::FieldSet(JsCodeFieldSet {
                                    base: Box::new(self_code.clone()),
                                    key: b.key.clone(),
                                    value: Box::new(value_code)
                                })
                            }))
                        },
                        value: Box::new(ValueNull),
                    })
                },
                MortarJsValueSource::Expr => {
                    return erb.err_new(format!("Cannot modify a temporary value"));
                },
                MortarJsValueSource::DefinitionSet(_) => {
                    return erb.err_new(format!("Cannot modify a definition set function"));
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
                                type_: JsCodeType::Block(JsCodeBlock { entries: seq.into_iter().map(JsCodeBlockEntry::Code).collect() })
                            }))
                        },
                        value: Box::new(ValueNull),
                    })
                },
            }
        });
    }
}
