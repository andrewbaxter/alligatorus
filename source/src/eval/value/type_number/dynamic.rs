use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectNumberDynamic,
            AspectOperatorAdd,
            AspectOperatorLt,
            AspectSet,
            AspectStackValueDynamic,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_number_const_get_maybe,
            aspect_number_dynamic_aspect_id,
            aspect_number_dynamic_get_maybe,
            aspect_operator_add_aspect_id,
            aspect_operator_lt_aspect_id,
            aspect_set_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicFieldUse,
            CodeDynamicOperatorAdd,
            CodeDynamicOperatorLt,
            CodeDynamicType,
        },
        eval::{
            Erb,
            value::{
                type_dynamic::{
                    ValueDynamic,
                    ValueDynamicDataAccess,
                    ValueDynamicSource,
                    mortar_bind_js,
                    mortar_consume_js_to_js,
                    mortar_js_stack_to_value,
                    mortar_set,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_string::dynamic::TypeStringDynamic,
            },
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
};

pub(crate) struct TypeNumberDynamic;

impl Value for TypeNumberDynamic {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeNumberDynamic {
    fn desc(&self) -> String {
        return format!("mortar number");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeNumberDynamic)
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_number_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectNumberDynamic>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectField>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_operator_lt_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectOperatorLt>));
        }
        if *key == aspect_operator_add_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectOperatorAdd>));
        }
        if *key == aspect_set_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectSet>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_number_dynamic_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeNumberDynamic),
            }),
        })
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_number_const_get_maybe(xx, source_type).is_some() ||
            aspect_number_dynamic_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            if let Some(aspect_number) = aspect_number_const_get_maybe(xx, type_.as_ref()) {
                let CodeValue { code, value } = cv;
                let mut seq = vec![];
                if let Some(c) = erb.must_er(code.cast::<CodeDynamic>())? {
                    seq.push(c.clone());
                };
                let num_val = aspect_number.get(value.as_ref());
                seq.push(CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::Literal(serde_json::Value::from(num_val))
                });
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(CodeDynamic {
                            loc: loc,
                            type_: CodeDynamicType::Block(
                                CodeDynamicBlock { entries: seq.into_iter().map(CodeDynamicBlockEntry::Code).collect() }
                            )
                        }))
                    },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeNumberDynamic),
                    }),
                });
            } else {
                return Ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeNumberDynamic),
                    })
                });
            }
        });
    }
}

impl Aspect for TypeNumberDynamic { }

impl AspectNumberDynamic for TypeNumberDynamic { }

impl AspectStackValueDynamic for TypeNumberDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_js_stack_to_value(loc, value, Rc::new(TypeNumberDynamic))
    }
}

impl AspectConsumeDynamic for TypeNumberDynamic {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_js_to_js(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeNumberDynamic {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        mortar_bind_js(xx, loc, cv)
    }
}

fn js_field_name(key: &str) -> &str {
    match key {
        "to_string" => "toString",
        other => other,
    }
}

impl AspectField for TypeNumberDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(crate::builtin::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on mortar number", key_name)),
            };
            let js_name = js_field_name(&key_name).to_string();
            let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc: loc,
                        type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                            base: Box::new(self_code),
                            field: js_name.clone(),
                        }),
                    })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Field(ValueDynamicDataAccess { key: js_name, }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "to_string" => Some(Rc::new(TypeFunctionDynamic {
                arg: Rc::new(TypeNullDynamic),
                ret: Rc::new(TypeStringDynamic),
            })),
            _ => None,
        }
    }
}

impl AspectSet for TypeNumberDynamic {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        mortar_set(xx, loc, self_, value)
    }
}

impl AspectOperatorLt for TypeNumberDynamic {
    fn lt(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeDynamic::to_js(&TypeNumberDynamic, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeNumberDynamic.cast_from(xx, loc, reference))?;
            let ref_code = erb.must_er(AspectConsumeDynamic::to_js(&TypeNumberDynamic, xx, loc, ref_cv))?;
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::OperatorLt(CodeDynamicOperatorLt {
                            base: Box::new(base_code),
                            reference: Box::new(ref_code),
                        }),
                    })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(crate::eval::value::type_bool::dynamic::TypeBoolDynamic),
                }),
            });
        });
    }
}

impl AspectOperatorAdd for TypeNumberDynamic {
    fn add(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeDynamic::to_js(&TypeNumberDynamic, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeNumberDynamic.cast_from(xx, loc, reference))?;
            let ref_code = erb.must_er(AspectConsumeDynamic::to_js(&TypeNumberDynamic, xx, loc, ref_cv))?;
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::OperatorAdd(CodeDynamicOperatorAdd {
                            base: Box::new(base_code),
                            reference: Box::new(ref_code),
                        }),
                    })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeNumberDynamic),
                }),
            });
        });
    }
}
