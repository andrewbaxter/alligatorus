use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
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
            AspectLiftDynamic,
            AspectStackValueDynamic,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

// TypeMortarType — the mortar (v8) representation of a type value. In v8, types
// are always Rc`<dyn Type>` externals.
pub(crate) struct TypeAnyDynamic;

impl Aspect for TypeAnyDynamic { }

impl AspectBind for TypeAnyDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeAnyDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeAnyDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        mortar_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeAnyDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeAnyDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeAnyDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_ = crate::eval::rc_type_from_v8(&*value);
        Ok(type_.as_value())
    }
}

impl AspectStackValueDynamic for TypeAnyDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        mortar_dynamic_stack_to_value(loc, value, Rc::new(TypeAnyDynamic))
    }
}

impl crate::r#gen::AspectField for TypeAnyDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> crate::langhelp::EvalRes {
        let self_code = match self_.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return crate::langhelp::EvalRes::err(loc, "any.field: receiver has no runtime code".to_string()),
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
        let access = CodeDynamic {
            loc,
            type_: CodeDynamicType::FieldUse(crate::code_dynamic::CodeDynamicFieldUse {
                base: Box::new(self_code),
                field: crate::langhelp::ConstKey::String(key_name).to_str(),
            }),
        };
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(access)),
            },
            value: Rc::new(crate::eval::value::value_dynamic::ValueDynamic {
                source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                type_: Rc::new(TypeAnyDynamic),
            }),
        })
    }

    fn field_type_opt(
        &self,
        _xx: &ExprContext,
        _key: &crate::langhelp::ConstKey,
    ) -> Option<Rc<dyn crate::langhelp::Type>> {
        Some(Rc::new(TypeAnyDynamic))
    }
}

impl crate::r#gen::AspectFunction for TypeAnyDynamic {
    fn call(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: CodeValue,
        argument: CodeValue,
    ) -> crate::langhelp::EvalRes {
        let func_code = match self_.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return crate::langhelp::EvalRes::err(loc, "any.call: function has no runtime code".to_string()),
        };
        let arg_type = argument.value.type_(xx, loc);
        let consume = match crate::r#gen::aspect_consume_dynamic_get(xx, loc, arg_type.as_ref()).into_res() {
            Ok(c) => c,
            Err(e) => return crate::langhelp::EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let arg_code = match consume.to_dynamic(xx, loc, argument).into_res() {
            Ok(c) => c,
            Err(e) => return crate::langhelp::EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                func: Box::new(func_code),
                args: vec![arg_code],
            }),
        };
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(call)),
            },
            value: Rc::new(crate::eval::value::value_dynamic::ValueDynamic {
                source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                type_: Rc::new(TypeAnyDynamic),
            }),
        })
    }
}

impl crate::r#gen::AspectLowerDynamic for TypeAnyDynamic {
    fn lower(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_ = crate::eval::rc_type_from_v8(&*value);
        Ok(type_.as_value())
    }
}

impl crate::r#gen::AspectType for TypeAnyDynamic {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeAnyDynamic)
    }
}

impl Type for TypeAnyDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeAnyDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == crate::r#gen::type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectLiftDynamic>));
        }
        if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn crate::r#gen::AspectLowerDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn crate::r#gen::AspectField>));
        }
        if *key == crate::r#gen::aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn crate::r#gen::AspectFunction>));
        }
        if *key == crate::r#gen::aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn crate::r#gen::AspectType>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        // Identity only — other source types reach `TypeAnyDynamic` via their own
        // `cast_to(TypeAnyDynamic)` (see `wrap_as_any` helper).
        source_type.aspect(xx, &crate::r#gen::type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        // Identity case with runtime code: the source's runtime form is already an
        // `Rc<dyn Type>` external, so pass `cv` through unchanged.
        if matches!(cv.code.code, crate::langhelp::Code_::Dyn(_)) {
            return crate::langhelp::EvalRes::ok(cv);
        }

        // No code (compile-time-only value): extract the actual `Rc<dyn Type>` via
        // `AspectType` on the value's meta-type, push as `External::Type`, and emit a
        // `BindingUse`.
        let meta_type = cv.value.type_(xx, loc);
        let aspect_type = match crate::r#gen::aspect_type_get(xx, loc, meta_type.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return crate::langhelp::EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let type_ = aspect_type.type_(cv.value.as_ref());
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_)));
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: crate::langhelp::Code {
                loc,
                code: crate::langhelp::Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                })),
            },
            value: Rc::new(crate::eval::value::value_dynamic::ValueDynamic {
                source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                type_: Rc::new(TypeAnyDynamic),
            }),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeAnyDynamic))
    }

    fn desc(&self) -> String {
        "mortar type".to_string()
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> crate::langhelp::Artifact {
        super::r#const::TypeAnyConst::build_type_artifact()
    }
}

impl Value for TypeAnyDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::r#const::TypeAnyConst)
    }
}
