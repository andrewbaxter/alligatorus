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
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_null::dynamic::TypeNullDynamic,
                type_root::r#const::TypeRoot,
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
            AspectFunction,
            AspectInitialBind,
            AspectStackValueDynamic,
            AspectTaggedUnion,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            aspect_tagged_union_aspect_id,
            builtin_code_dyn_iface_identity,
            type_code_dyn_handle_identity,
            type_code_enum_identity,
            type_code_struct_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeCodeDynCastFn;

impl Aspect for TypeCodeDynCastFn { }

impl AspectFunction for TypeCodeDynCastFn {
    fn call(&self, _xx: &ExprContext, _loc: QExprId, function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_any: &dyn Any = argument.value.as_ref();
        let target_type: Rc<dyn Type> = if let Some(v) = arg_any.downcast_ref::<ValueAnyConst>() {
            v.inner.clone()
        } else if let Some(dt) =
            arg_any.downcast_ref::<crate::eval::value::type_source_data_type::r#const::TypeSourceDataTypeConst>() {
            Rc::new(
                crate::eval::value::type_source_data_type::r#const::TypeSourceDataTypeConst { data: dt.data.clone() },
            )
        } else {
            Rc::new(TypeCodeDynHandleDynamic) as Rc<dyn Type>
        };
        EvalRes::ok(CodeValue {
            code: function.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: target_type,
            }),
        })
    }
}

impl ExporterAny for TypeCodeDynCastFn {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeCodeDynCastFn {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeDynCastFn) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeDynCastFn) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to code_dyn cast fn".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from code_dyn cast fn".to_string())
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeDynCastFn))
    }

    fn desc(&self) -> String {
        "code_dyn.cast".to_string()
    }
}

impl Value for TypeCodeDynCastFn {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeDynCastFn))
    }
}
#[derive(Clone)]
pub(crate) struct TypeCodeDynHandleDynamic;

impl Aspect for TypeCodeDynHandleDynamic { }

impl AspectBind for TypeCodeDynHandleDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
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
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
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
                    value: Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeDynCastFn) }),
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
}

impl AspectInitialBind for TypeCodeDynHandleDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
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
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
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
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            }),
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
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeDynHandleDynamic))
    }
}
#[derive(Clone)]
pub(crate) struct TypeCodeEnumDynamic;

impl Aspect for TypeCodeEnumDynamic { }

impl AspectBind for TypeCodeEnumDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeEnumDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeEnumDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeCodeEnumDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeEnumDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeEnumDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeCodeEnumDynamic))
    }
}

impl AspectTaggedUnion for TypeCodeEnumDynamic {
    fn variants(&self) -> Vec<(ConstKey, String, Rc<dyn Type>)> {
        vec![
            (ConstKey::String("none".to_string()), "none".to_string(), Rc::new(TypeNullDynamic) as Rc<dyn Type>),
            (
                ConstKey::String("dyn".to_string()),
                "dyn".to_string(),
                Rc::new(TypeCodeDynHandleDynamic) as Rc<dyn Type>,
            ),
        ]
    }
}

impl ExporterAny for TypeCodeEnumDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeCodeEnumDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeEnumDynamic) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_code_enum_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_tagged_union_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeEnumDynamic) as Box<dyn AspectTaggedUnion>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to Code_ enum".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from Code_ enum".to_string())
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeEnumDynamic))
    }

    fn desc(&self) -> String {
        "Code_".to_string()
    }
}

impl Value for TypeCodeEnumDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeEnumDynamic))
    }
}
#[derive(Clone)]
pub(crate) struct TypeCodeStructDynamic;

impl Aspect for TypeCodeStructDynamic { }

impl AspectBind for TypeCodeStructDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeStructDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeStructDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectField for TypeCodeStructDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        match key_name.as_str() {
            "code" => {
                EvalRes::ok(CodeValue {
                    code: self_.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeCodeEnumDynamic),
                    }),
                })
            },
            _ => EvalRes::err(loc, format!("Field '{}' not found on Code struct", key_name)),
        }
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(k) = key else {
            return None;
        };
        if k == "code" {
            Some(Rc::new(TypeCodeEnumDynamic))
        } else {
            None
        }
    }
}

impl AspectInitialBind for TypeCodeStructDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeStructDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeStructDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeCodeStructDynamic))
    }
}

impl ExporterAny for TypeCodeStructDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeCodeStructDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeCodeStructDynamic) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_code_struct_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeStructDynamic) as Box<dyn AspectField>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to Code struct".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from Code struct".to_string())
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeCodeStructDynamic))
    }

    fn desc(&self) -> String {
        "Code".to_string()
    }
}

impl Value for TypeCodeStructDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeStructDynamic))
    }
}
