use {
    crate::{
        artifact::ExporterAny,
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
            from_dynamic_ref,
            push::Push,
            to_dynamic_ext,
            value::{
                type_code_struct::dynamic::TypeCodeStructDynamic,
                type_root::r#const::TypeRoot,
                type_binding::dynamic::BindingDynamicBinding,
                type_value::dynamic::TypeValueDynamic,
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
            CVE_EXTRACT_FN_SCOPE_ID,
            CVE_GET_CODE_ENUM_FN_SCOPE_ID,
            CVE_GET_VALUE_FN_SCOPE_ID,
            Binding,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            code_value_extern_type_identity,
            expr_context_ref_type_identity,
        },
    },
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
};

pub(crate) struct CodeValueExtern(pub Rc<RefCell<Option<CodeValue>>>);

impl CodeValueExtern {
    pub fn into_inner(self) -> CodeValue {
        self.0.borrow_mut().take().expect("CodeValueExtern: already taken")
    }

}

impl Push for CodeValueExtern {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeCodeValueExternDynamic)
    }

    fn from_dynamic<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        CodeValueExtern(from_dynamic_ref::<Rc<RefCell<Option<CodeValue>>>>(&*value).clone())
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_dynamic_ext(scope, self.0).into()
    }
}

pub(crate) struct ExprContextPush(pub Rc<ExprContext>);

impl Push for ExprContextPush {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeExprContextPushDynamic)
    }

    fn from_dynamic<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        ExprContextPush(from_dynamic_ref::<Rc<ExprContext>>(&*value).clone())
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_dynamic_ext(scope, self.0).into()
    }
}
pub(crate) struct TypeCodeValueExternDynamic;

impl Aspect for TypeCodeValueExternDynamic { }

impl AspectBind for TypeCodeValueExternDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeCodeValueExternDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeCodeValueExternDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectField for TypeCodeValueExternDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
            let key_type = erb.must_er(key.value.type_(xx, loc))?;
            let aspect_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_str = aspect_string.get(key.value.as_ref());
            if key_str == "code" {
                return Ok(CodeValue {
                    code: Code {
                        loc,
                        code: Code_::Dyn(Rc::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::Call(CodeDynamicCall {
                                func: Box::new(CodeDynamic {
                                    loc,
                                    type_: CodeDynamicType::BindingUse(
                                        CodeDynamicBindingUse { id: CVE_GET_CODE_ENUM_FN_SCOPE_ID, }
                                    ),
                                }),
                                args: vec![self_code],
                            }),
                        }))
                    },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeCodeStructDynamic),
                    }),
                });
            }
            if key_str == "value" {
                return Ok(CodeValue {
                    code: Code {
                        loc,
                        code: Code_::Dyn(Rc::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::Call(CodeDynamicCall {
                                func: Box::new(CodeDynamic {
                                    loc,
                                    type_: CodeDynamicType::BindingUse(
                                        CodeDynamicBindingUse { id: CVE_GET_VALUE_FN_SCOPE_ID, }
                                    ),
                                }),
                                args: vec![self_code],
                            }),
                        }))
                    },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeValueDynamic),
                    }),
                });
            }
            return erb.err_new(format!("CodeValueExtern has no field '{}' (only 'code' and 'value')", key_str));
        });
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(s) = key else {
            return None;
        };
        match s.as_str() {
            "code" => Some(Rc::new(TypeCodeStructDynamic)),
            "value" => Some(Rc::new(TypeValueDynamic)),
            _ => None,
        }
    }
}

impl AspectInitialBind for TypeCodeValueExternDynamic {
    fn initial_bind(&self, dynamic_key: crate::langhelp::BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeCodeValueExternDynamic),
        })
    }
}

impl AspectStackValueDynamic for TypeCodeValueExternDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeCodeValueExternDynamic))
    }
}

impl crate::eval::value::type_root::r#const::ExporterRootDelegate for TypeCodeValueExternDynamic {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _id: Option<&crate::artifact::ArtifactIdSource>,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        return EResult::ok(Rc::new(TypeCodeValueExternDynamic));
    }

    fn self_export(
        _xx: &ExprContext,
        _loc: QExprId,
        _self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> crate::artifact::ArtifactBody>,
    ) -> EResult<crate::artifact::Artifact> {
        return EResult::ok(crate::artifact::Artifact::Body(make_body(serde_json::Value::Null)));
    }
}

impl ExporterAny for TypeCodeValueExternDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeCodeValueExternDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeCodeValueExternDynamic));
    }
}

impl Type for TypeCodeValueExternDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeCodeValueExternDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == code_value_extern_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectField>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeCodeValueExternDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &code_value_extern_type_identity()).is_some()
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.clone_rc().is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &code_value_extern_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeCodeValueExternDynamic),
                }),
            });
        }
        EvalRes::err(loc, "Cannot convert to CodeValueExtern".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        let dest_type_rc = match type_.clone_rc() {
            Some(rc) => rc,
            None => return EvalRes::err(
                loc,
                format!("CodeValueExtern.cast_to: dest type ({}) doesn't support clone_rc", type_.desc()),
            ),
        };
        let self_code = match cv.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "CodeValueExtern.cast_to: receiver has no dynamic code".to_string()),
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Call(CodeDynamicCall {
                        func: Box::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: CVE_EXTRACT_FN_SCOPE_ID }),
                        }),
                        args: vec![self_code],
                    }),
                })),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_type_rc,
            }),
        })
    }

    fn desc(&self) -> String {
        "CodeValueExtern".to_string()
    }
}

impl Value for TypeCodeValueExternDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeCodeValueExternDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::CodeValueExternDynamic))
    }
}
pub(crate) struct TypeExprContextPushDynamic;

impl Aspect for TypeExprContextPushDynamic { }

impl AspectBind for TypeExprContextPushDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeExprContextPushDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeExprContextPushDynamic {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        dynamic_consume_to_dynamic(xx, loc, cv)
    }
}

impl AspectStackValueDynamic for TypeExprContextPushDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        dynamic_stack_to_value(loc, value, Rc::new(TypeExprContextPushDynamic))
    }
}

impl ExporterAny for TypeExprContextPushDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeExprContextPushDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeExprContextPushDynamic));
    }
}

impl Type for TypeExprContextPushDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeExprContextPushDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == expr_context_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextPushDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextPushDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextPushDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeExprContextPushDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &expr_context_ref_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &expr_context_ref_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeExprContextPushDynamic),
                }),
            });
        }
        EvalRes::err(loc, "Cannot convert to ExprContextPush".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from ExprContextPush".to_string())
    }

    fn desc(&self) -> String {
        "ExprContextPush".to_string()
    }
}

impl Value for TypeExprContextPushDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeExprContextPushDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::ExprContextPushDynamic))
    }
}

impl Push for CodeValue {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeCodeValueExternDynamic)
    }

    fn from_dynamic<'s>(scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        <CodeValueExtern as Push>::from_dynamic(scope, value).into_inner()
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        CodeValueExtern(Rc::new(RefCell::new(Some(self)))).into_dynamic(scope)
    }
}
