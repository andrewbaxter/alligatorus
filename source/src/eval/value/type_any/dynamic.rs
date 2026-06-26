use {
    crate::{
        artifact::{
            Artifact,
            ExporterAny,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicFieldUse,
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
            rc_type_from_v8,
            value::{
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
            External,
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
            AspectLiftDynamic,
            AspectStackValueDynamic,
            AspectType,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            aspect_type_aspect_id,
            aspect_type_get,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeAnyDynamic;

impl Aspect for TypeAnyDynamic { }

impl AspectBind for TypeAnyDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
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
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectField for TypeAnyDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let self_code = match self_.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "any.field: receiver has no runtime code".to_string()),
        };
        let key_type = match key.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        let access = CodeDynamic {
            loc,
            type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                base: Box::new(self_code),
                field: ConstKey::String(key_name).to_str(),
            }),
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(access)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeAnyDynamic),
            }),
        })
    }

    fn field_type_opt(&self, _xx: &ExprContext, _key: &ConstKey) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeAnyDynamic))
    }
}

impl AspectFunction for TypeAnyDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, argument: CodeValue) -> EvalRes {
        let func_code = match self_.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "any.call: function has no runtime code".to_string()),
        };
        let arg_type = match argument.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let consume = match aspect_consume_dynamic_get(xx, loc, arg_type.as_ref()).into_res() {
            Ok(c) => c,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let arg_code = match consume.to_dynamic(xx, loc, argument).into_res() {
            Ok(c) => c,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(func_code),
                args: vec![arg_code],
            }),
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(call)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeAnyDynamic),
            }),
        })
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
        let type_ = rc_type_from_v8(&*value);
        Ok(type_.as_value())
    }
}

impl AspectStackValueDynamic for TypeAnyDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeAnyDynamic))
    }
}

impl AspectType for TypeAnyDynamic {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(TypeAnyDynamic)
    }
}

impl crate::eval::value::type_root::r#const::ExporterRootDelegate for TypeAnyDynamic {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        EResult::ok(Rc::new(TypeAnyDynamic) as Rc<dyn ExporterAny>)
    }

    fn self_export(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> crate::artifact::ArtifactBody>,
    ) -> EResult<crate::artifact::Artifact> {
        EResult::ok(crate::artifact::Artifact::Body(make_body(serde_json::Value::Null)))
    }
}

impl crate::r#gen::AspectArtifact for TypeAnyDynamic {
    fn exporter(&self) -> Rc<dyn crate::artifact::ArtifactExporter> {
        Rc::new(crate::artifact::ExporterRoot::AnyDynamic)
    }
}

impl ExporterAny for TypeAnyDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeAnyDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeAnyDynamic));
    }
}

impl Type for TypeAnyDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeAnyDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
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
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectField>));
        }
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectFunction>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn AspectType>));
        }
        if *key == crate::r#gen::aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeAnyDynamic) as Box<dyn crate::r#gen::AspectArtifact>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        if matches!(cv.code.code, Code_::Dyn(_)) {
            return EvalRes::ok(cv);
        }
        let meta_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let aspect_type = match aspect_type_get(xx, loc, meta_type.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let type_ = aspect_type.type_(cv.value.as_ref());
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, External::Type(type_)));
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id }),
                })),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeAnyDynamic),
            }),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeAnyDynamic))
    }

    fn desc(&self) -> String {
        "dynamic type".to_string()
    }
}

impl Value for TypeAnyDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeAnyDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeAnyDynamic))
    }
}
