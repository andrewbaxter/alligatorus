use {
    crate::{
        artifact::ExporterAny,
        code_dynamic::CodeDynamic,
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_consume_to_dynamic,
                dynamic_stack_to_value,
            },
            value::{
                type_any::r#const::ValueAnyConst,
                type_binding::dynamic::BindingDynamicBinding,
                type_code_dyn_cast_fn::r#const::TypeCodeDynCastFn,
                type_root::r#const::TypeRoot,
                value_dynamic::{
                    ValueDynamic,
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
            AspectStackValueDynamic,
            Binding,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            builtin_code_dyn_iface_identity,
            type_code_dyn_handle_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeCodeDynHandleDynamic;

impl Aspect for TypeCodeDynHandleDynamic { }

impl AspectBind for TypeCodeDynHandleDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeDynHandleDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeDynHandleDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectField for TypeCodeDynHandleDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = match key.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_deref().unwrap());
        match key_name.as_str() {
            "cast" => {
                let self_code = match self_.code.cast::<CodeDynamic>().into_res() {
                    Ok(Some(c)) => c.clone(),
                    _ => return EvalRes::err(loc, "code_dyn.cast: receiver has no dynamic code".to_string()),
                };
                EvalRes::ok(CodeValue {
                    code: Code {
                        loc,
                        code: Code_::Dyn(Rc::new(self_code)),
                    },
                    value: Some(Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeDynCastFn) })),
                })
            },
            _ => EvalRes::err(loc, format!("Field '{}' not found on code_dyn handle", key_name)),
        }
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(k) = key else {
            return None;
        };
        if k == "cast" {
            Some(Rc::new(TypeCodeDynCastFn))
        } else {
            None
        }
    }

    fn field_keys(&self, _xx: &crate::langhelp::ExprContext) -> Vec<crate::langhelp::ConstKey> {
        Vec::new()
    }
}

impl AspectInitialBind for TypeCodeDynHandleDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeDynHandleDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeDynHandleDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeCodeDynHandleDynamic))
    }
}

impl ExporterAny for TypeCodeDynHandleDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeCodeDynHandleDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeCodeDynHandleDynamic));
    }
}

impl Type for TypeCodeDynHandleDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeDynHandleDynamic) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_code_dyn_handle_identity() {
            return Some(Box::new(true));
        }
        if *key == builtin_code_dyn_iface_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynHandleDynamic) as Box<dyn AspectField>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        None
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.clone_rc().is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to code_dyn handle".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        let dest_rc = match type_.clone_rc() {
            Some(rc) => rc,
            None => return EvalRes::err(
                loc,
                format!("code_dyn handle cast: dest type ({}) doesn't support clone_rc", type_.desc()),
            ),
        };
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            })),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeDynHandleDynamic))
    }

    fn desc(&self) -> String {
        "code_dyn".to_string()
    }
}

impl Value for TypeCodeDynHandleDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeCodeDynHandleDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeDynHandleDynamic))
    }
}
