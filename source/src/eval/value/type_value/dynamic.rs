//! `TypeValueDynamic` — singleton Type whose only role is to mark "value
//! interface" as a cast destination. Source-data-type instances impl'ing the
//! builtin `value` interface cast to this; the cast (in
//! `TypeSourceDataTypeDynamic::cast_to`) emits a `__data_instance_to_value` call
//! that wraps the runtime v8 instance into a `ValueUserDefined` external.
use crate::r#gen::ScopeEntry;
use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                mortar_consume_to_dynamic,
                mortar_dynamic_stack_to_value,
            },
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
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
            AspectInitialBind,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
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
    ) -> EResult<(crate::langhelp::Code, Box<dyn ScopeEntry>)> {
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
        mortar_consume_to_dynamic(xx, loc, cv)
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
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeValueDynamic))
    }
}

impl crate::r#gen::AspectField for TypeValueDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> crate::langhelp::EvalRes {
        let self_code = match self_.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return crate::langhelp::EvalRes::err(loc, "value.field: receiver has no runtime code".to_string()),
        };
        let key_type = key.value.type_(xx, loc);
        let key_string = match crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return crate::langhelp::EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        if key_name == "type" {
            // Emit `__value_get_type(<value_code>)`. At v8 time the callback invokes the
            // user's compiled `value.type` impl on the instance, unwraps the resulting
            // `Rc<dyn Type>` external, and rebuilds it as a `TypeRefV8` JS object —
            // supporting `.aspect(unique)` from source as if it were a Rust `Rc<dyn Type>`.
            let call = crate::code_dynamic::CodeDynamic {
                loc,
                type_: crate::code_dynamic::CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                    func: Box::new(crate::code_dynamic::CodeDynamic {
                        loc,
                        type_: crate::code_dynamic::CodeDynamicType::BindingUse(
                            crate::code_dynamic::CodeDynamicBindingUse { id: crate::eval::VALUE_GET_TYPE_FN_SCOPE_ID },
                        ),
                    }),
                    args: vec![self_code],
                }),
            };
            return crate::langhelp::EvalRes::ok(CodeValue {
                code: crate::langhelp::Code {
                    loc,
                    code: crate::langhelp::Code_::Dyn(Rc::new(call)),
                },
                value: Rc::new(crate::eval::dynamic_util::ValueDynamic {
                    source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                    type_: Rc::new(crate::r#gen::TypeTypeRefV8Dynamic),
                }),
            });
        }
        crate::langhelp::EvalRes::err(loc, format!("value has no field '{}' (only 'type')", key_name))
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &crate::langhelp::ConstKey) -> Option<Rc<dyn Type>> {
        let crate::langhelp::ConstKey::String(s) = key else {
            return None;
        };
        if s == "type" {
            return Some(Rc::new(crate::r#gen::TypeTypeRefV8Dynamic));
        }
        None
    }
}

impl Type for TypeValueDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeValueDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == crate::eval::builtin_value_iface_identity() {
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
        if *key == crate::r#gen::aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeValueDynamic) as Box<dyn crate::r#gen::AspectField>));
        }
        None
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        // A `TypeValueDynamic` is "the v8 representation of some value" — typically a
        // user data-type instance object the source-language type system has lost track
        // of. Allow re-typing it to any clone_rc'able dest: the v8 form is the same JS
        // object, and the user is asserting "this value really is `T`". (Same shape as
        // `CodeValueExtern.cast_to` but with no `__cve_extract` — the value form is
        // already the runtime JS object.)
        type_.clone_rc().is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> crate::langhelp::EvalRes {
        let dest_type_rc = match type_.clone_rc() {
            Some(rc) => rc,
            None => {
                return crate::langhelp::EvalRes::err(
                    loc,
                    format!("value.cast_to: dest type ({}) doesn't support clone_rc", type_.desc(),),
                );
            },
        };
        let self_code = match cv.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => {
                return crate::langhelp::EvalRes::err(loc, "value.cast_to: receiver has no runtime code".to_string());
            },
        };
        crate::langhelp::EvalRes::ok(CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(self_code)),
            },
            value: Rc::new(crate::eval::dynamic_util::ValueDynamic {
                source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
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
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
