use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_consume_to_dynamic,
                dynamic_stack_to_value,
            },
            value::{
                type_any::r#const::TypeAnyConst,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
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
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectStackValueDynamic,
            ScopeEntry,
            TypeTypeRefPushDynamic,
            VALUE_GET_TYPE_FN_SCOPE_ID,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            builtin_value_iface_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeValueDynamic;

impl Aspect for TypeValueDynamic { }

impl AspectBind for TypeValueDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeValueDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeValueDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeValueDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeValueDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeValueDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeValueDynamic))
    }
}

impl AspectField for TypeValueDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let self_code = match self_.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "value.field: receiver has no runtime code".to_string()),
        };
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        if key_name == "type" {
            let call = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(
                            CodeDynamicBindingUse { id: VALUE_GET_TYPE_FN_SCOPE_ID },
                        ),
                    }),
                    args: vec![self_code],
                }),
            };
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(call)),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeTypeRefPushDynamic),
                }),
            });
        }
        EvalRes::err(loc, format!("value has no field '{}' (only 'type')", key_name))
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(s) = key else {
            return None;
        };
        if s == "type" {
            return Some(Rc::new(TypeTypeRefPushDynamic));
        }
        None
    }
}

impl Type for TypeValueDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeValueDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == builtin_value_iface_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeValueDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeValueDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeValueDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeValueDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeValueDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeValueDynamic) as Box<dyn AspectField>));
        }
        None
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.clone_rc().is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        let dest_type_rc = match type_.clone_rc() {
            Some(rc) => rc,
            None => {
                return EvalRes::err(
                    loc,
                    format!("value.cast_to: dest type ({}) doesn't support clone_rc", type_.desc(),),
                );
            },
        };
        let self_code = match cv.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => {
                return EvalRes::err(loc, "value.cast_to: receiver has no runtime code".to_string());
            },
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(self_code)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_type_rc,
            }),
        })
    }

    fn desc(&self) -> String {
        "value".to_string()
    }
}

impl Value for TypeValueDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
