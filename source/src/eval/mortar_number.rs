use {
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
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_js_number_aspect_id,
            aspect_js_number_get_maybe,
            aspect_js_stack_value_aspect_id,
            aspect_number_get_maybe,
            aspect_operator_add_aspect_id,
            aspect_operator_lt_aspect_id,
            aspect_set_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeBlock,
            JsCodeBlockEntry,
            JsCodeFieldUse,
            JsCodeOperatorAdd,
            JsCodeOperatorLt,
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
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::{
        Erb,
        mortar_function::MortarTypeFunction,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataAccess,
            MortarJsValueSource,
            mortar_bind_js,
            mortar_consume_js_to_js,
            mortar_js_stack_to_value,
            mortar_set,
        },
        mortar_null::TypeMortarNull,
        mortar_string::TypeMortarString,
    },
};

pub(crate) struct TypeMortarNumber;

impl Value for TypeMortarNumber {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeAny)
    }
}

impl Type for TypeMortarNumber {
    fn desc(&self) -> String {
        return format!("mortar number");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeMortarNumber)
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

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_js_number_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarNumber),
            }),
        })
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_number_get_maybe(xx, source_type).is_some() || aspect_js_number_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            if let Some(aspect_number) = aspect_number_get_maybe(xx, type_.as_ref()) {
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
                    value: Rc::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(TypeMortarNumber),
                    }),
                });
            } else {
                return Ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(TypeMortarNumber),
                    })
                });
            }
        });
    }
}

impl Aspect for TypeMortarNumber { }

impl AspectJsNumber for TypeMortarNumber { }

impl AspectJsStackValue for TypeMortarNumber {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        mortar_js_stack_to_value(loc, value, Rc::new(TypeMortarNumber))
    }
}

impl AspectConsumeJs for TypeMortarNumber {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        mortar_consume_js_to_js(xx, loc, cv)
    }
}

impl AspectBindJs for TypeMortarNumber {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        mortar_bind_js(xx, loc, cv)
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
                value: Rc::new(MortarJsValue {
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
        mortar_set(xx, loc, self_, value)
    }
}

impl AspectOperatorLt for TypeMortarNumber {
    fn lt(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeJs::to_js(&TypeMortarNumber, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeMortarNumber.cast_from(xx, loc, reference))?;
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
                value: Rc::new(MortarJsValue {
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
            let ref_cv = erb.must_er(TypeMortarNumber.cast_from(xx, loc, reference))?;
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
                value: Rc::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(TypeMortarNumber),
                }),
            });
        });
    }
}
