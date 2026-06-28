use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
        },
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
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_consume_to_dynamic,
                dynamic_set,
                dynamic_stack_to_value,
            },
            value::{
                type_binding::dynamic::BindingDynamicBinding,
                type_bool::dynamic::TypeBoolDynamic,
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_string::dynamic::TypeStringDynamic,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicDataAccess,
                    ValueDynamicSource,
                },
            },
        },
        langhelp::{
            Aspect,
            BindingId,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectNumberDynamic,
            AspectOperatorAdd,
            AspectOperatorEq,
            AspectOperatorLt,
            AspectSet,
            AspectStackValueDynamic,
            AspectType,
            Binding,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_number_const_get_maybe,
            aspect_number_dynamic_aspect_id,
            aspect_number_dynamic_get_maybe,
            aspect_operator_add_aspect_id,
            aspect_operator_eq_aspect_id,
            aspect_operator_lt_aspect_id,
            aspect_set_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            aspect_type_aspect_id,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeNumberDynamic;

impl Aspect for TypeNumberDynamic { }

impl AspectBind for TypeNumberDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeNumberDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeNumberDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectField for TypeNumberDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = erb.must_er(key.value.as_ref().unwrap().type_(xx, loc))?;
            let key_name =
                erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?.get(key.value.as_deref().unwrap());
            let field_type = match self.field_type_opt(xx, &ConstKey::String(key_name.clone())) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on dynamic number", key_name)),
            };
            let js_name = match key_name.as_str() {
                "to_string" => "toString".to_string(),
                _ => key_name.clone(),
            };
            return Ok(CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc: loc,
                        type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                            base: Box::new(erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone()),
                            field: js_name.clone(),
                        }),
                    })),
                },
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Field(ValueDynamicDataAccess { key: js_name, }),
                    type_: field_type,
                })),
            });
        });
    }

    fn field_keys(&self, _xx: &crate::langhelp::ExprContext) -> Vec<crate::langhelp::ConstKey> {
        Vec::new()
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
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
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
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeNumberDynamic),
                })),
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
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeBoolDynamic),
                })),
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
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeBoolDynamic),
                })),
            });
        });
    }
}

impl AspectSet for TypeNumberDynamic {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        dynamic_set(xx, loc, self_, value)
    }
}

impl AspectStackValueDynamic for TypeNumberDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeNumberDynamic))
    }
}

impl AspectType for TypeNumberDynamic {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeNumberDynamic)
    }
}

impl ExporterAny for TypeNumberDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeNumberDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeNumberDynamic));
    }
}

impl ExporterRootDelegate for TypeNumberDynamic {
    fn self_export(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        EResult::ok(Artifact::Body(make_body(serde_json::Value::Null)))
    }

    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        EResult::ok(Rc::new(TypeNumberDynamic) as Rc<dyn ExporterAny>)
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
            return Some(Box::new(Box::new(TypeNumberDynamic) as Box<dyn AspectType>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_number_const_get_maybe(xx, source_type).is_some() ||
            aspect_number_dynamic_get_maybe(xx, source_type).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_number_dynamic_get_maybe(xx, type_).is_some() || type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = erb.must_er(cv.value.as_ref().unwrap().type_(xx, loc))?;
            if let Some(aspect_number) = aspect_number_const_get_maybe(xx, type_.as_ref()) {
                let CodeValue { code, value } = cv;
                let value = value.unwrap();
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
                    value: Some(Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeNumberDynamic),
                    })),
                });
            } else {
                return Ok(CodeValue {
                    code: cv.code,
                    value: Some(Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeNumberDynamic),
                    }))
                });
            }
        });
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeNumberDynamic),
            })),
        })
    }

    fn desc(&self) -> String {
        return format!("dynamic number");
    }
}

impl Value for TypeNumberDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeNumberDynamic);
    }

    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::NumberDynamic))
    }
}
