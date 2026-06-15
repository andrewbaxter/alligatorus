use crate::r#gen::ScopeEntry;
use {
    crate::{
        r#gen::{AspectBind, AspectBindDynamic, AspectConsumeDynamic, AspectField, AspectInitialBind, AspectNumberDynamic, AspectOperatorAdd, AspectOperatorEq, AspectOperatorLt, AspectSet, AspectStackValueDynamic, aspect_bind_aspect_id, aspect_bind_dynamic_aspect_id, aspect_consume_dynamic_aspect_id, aspect_field_aspect_id, aspect_initial_bind_aspect_id, aspect_number_const_get_maybe, aspect_number_dynamic_aspect_id, aspect_number_dynamic_get_maybe, aspect_operator_add_aspect_id, aspect_operator_eq_aspect_id, aspect_operator_lt_aspect_id, aspect_set_aspect_id, aspect_stack_value_dynamic_aspect_id, aspect_type_aspect_id},
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBlock,
            CodeDynamicBlockEntry,
            CodeDynamicFieldUse,
            CodeDynamicOperatorAdd,
            CodeDynamicOperatorEq,
            CodeDynamicOperatorLt,
            CodeDynamicType,
        },
        eval::{
            Erb,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicDataAccess,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
                mortar_consume_to_dynamic,
                mortar_dynamic_stack_to_value,
                mortar_set,
            },
            value::{
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_string::dynamic::TypeStringDynamic,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
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
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

fn js_field_name(key: &str) -> &str {
    match key {
        "to_string" => "toString",
        other => other,
    }
}
pub(crate) struct TypeNumberDynamic;

impl Aspect for TypeNumberDynamic { }

impl AspectBind for TypeNumberDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeNumberDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeNumberDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectField for TypeNumberDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let const_key = ConstKey::String(key_name.clone());
            let field_type = match self.field_type_opt(xx, &const_key) {
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

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(key) = key else {
            return None;
        };
        match key.as_str() {
            "to_string" => Some(Rc::new(TypeFunctionDynamic {
                arg: Rc::new(TypeNullDynamic),
                ret: Rc::new(TypeStringDynamic),
            })),
            _ => None,
        }
    }
}

impl AspectInitialBind for TypeNumberDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeNumberDynamic),
        })
    }
}

impl AspectNumberDynamic for TypeNumberDynamic { }

impl AspectOperatorAdd for TypeNumberDynamic {
    fn add(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeNumberDynamic, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeNumberDynamic.cast_from(xx, loc, reference))?;
            let ref_code = erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeNumberDynamic, xx, loc, ref_cv))?;
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

impl AspectOperatorLt for TypeNumberDynamic {
    fn lt(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeNumberDynamic, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeNumberDynamic.cast_from(xx, loc, reference))?;
            let ref_code = erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeNumberDynamic, xx, loc, ref_cv))?;
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

impl AspectOperatorEq for TypeNumberDynamic {
    fn eq(&self, xx: &ExprContext, loc: QExprId, base: CodeValue, reference: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_code = erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeNumberDynamic, xx, loc, base))?;
            let ref_cv = erb.must_er(TypeNumberDynamic.cast_from(xx, loc, reference))?;
            let ref_code = erb.must_er(AspectConsumeDynamic::to_dynamic(&TypeNumberDynamic, xx, loc, ref_cv))?;
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::OperatorEq(CodeDynamicOperatorEq {
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

impl AspectSet for TypeNumberDynamic {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        mortar_set(xx, loc, self_, value)
    }
}

impl AspectStackValueDynamic for TypeNumberDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeNumberDynamic))
    }
}

impl crate::r#gen::AspectType for TypeNumberDynamic {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeNumberDynamic)
    }
}

impl Type for TypeNumberDynamic {
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
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectBind>));
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
        if *key == aspect_operator_eq_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectOperatorEq>));
        }
        if *key == aspect_operator_add_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectOperatorAdd>));
        }
        if *key == aspect_set_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectSet>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn crate::r#gen::AspectType>));
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_number_const_get_maybe(xx, source_type).is_some() ||
            aspect_number_dynamic_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_number_dynamic_get_maybe(xx, type_).is_some() ||
            type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some()
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

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        // No cast to `any` — numbers aren't types. Misuse.
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeNumberDynamic),
            }),
        })
    }

    fn desc(&self) -> String {
        return format!("mortar number");
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> crate::langhelp::Artifact {
        Self::build_type_artifact()
    }
}

impl Value for TypeNumberDynamic {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
