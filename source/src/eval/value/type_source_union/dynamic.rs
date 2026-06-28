use {
    crate::{
        artifact::ExporterAny,
        eval::{
            dynamic::{
                code::{
                    CodeDynamic,
                    CodeDynamicBindingUse,
                    CodeDynamicCall,
                    CodeDynamicType,
                },
                target::TargetBindSub,
                util::{
                    bind_dynamic_default,
                    bind_via_target,
                },
            },
            from_dynamic_ref,
            push::Push,
            to_dynamic_ext,
            value::{
                type_any::r#const::ValueAnyConst,
                type_binding::dynamic::BindingDynamicBinding,
                type_root::r#const::TypeRoot,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            BindingId,
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
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLift,
            AspectStackValueDynamic,
            AspectUnion,
            Binding,
            TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_union_aspect_id,
            type_type_identity,
            union_ref_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::{
        SourceUnionData,
        SourceUnionRef,
        r#const::TypeSourceUnionConst,
    },
};

#[allow(dead_code)]
#[derive(Clone)]
pub(crate) struct TypeSourceUnionDynamic {
    pub data: Rc<SourceUnionData>,
}

impl Aspect for TypeSourceUnionDynamic { }

impl AspectBind for TypeSourceUnionDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceUnionDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceUnionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceUnionDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceUnionDynamic { data: self.data.clone() }),
        })
    }
}

impl AspectLift for TypeSourceUnionDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_dynamic_ref(&value);
        let type_val = must_downcast_ref::<TypeSourceUnionDynamic>(type_.as_ref());
        let tu_type: Rc<dyn Type> = Rc::new(TypeSourceUnionConst { data: type_val.data.clone() });
        Ok(Rc::new(ValueAnyConst { inner: tu_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceUnionDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceUnionDynamic { data: self.data.clone() }),
            })),
        }
    }
}

impl ExporterAny for TypeSourceUnionDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl Type for TypeSourceUnionDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceUnionDynamic { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == union_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionDynamic { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionDynamic { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceUnionDynamic { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionDynamic { data: self.data.clone() }) as Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_lift_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceUnionDynamic { data: self.data.clone() }) as Box<dyn AspectLift>),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceUnionDynamic { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }
        {
            let data = &self.data;
            if *key == data.identity {
                return Some(Box::new(true));
            }
            if *key == aspect_union_aspect_id() {
                return Some(
                    Box::new(Box::new(TypeSourceUnionConst { data: data.clone() }) as Box<dyn AspectUnion>),
                );
            }
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
        if source_type.aspect(xx, &self.data.identity).is_some() {
            return true;
        }
        if source_type.aspect(xx, &union_ref_type_identity()).is_some() {
            return true;
        }
        source_type.aspect(xx, &aspect_union_aspect_id()).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &self.data.identity).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &self.data.identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceUnionDynamic { data: self.data.clone() }),
                })),
            });
        }
        if source_type.aspect(xx, &union_ref_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceUnionDynamic { data: self.data.clone() }),
                })),
            });
        }
        if source_type.aspect(xx, &aspect_union_aspect_id()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: source_type,
                })),
            });
        }
        EvalRes::err(loc, format!("Cannot convert {} to union_ref", source_type.desc()))
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceUnionDynamic { data: self.data.clone() }),
            })),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceUnionDynamic { data: self.data.clone() }))
    }

    fn desc(&self) -> String {
        format!("union_dyn")
    }
}

impl Value for TypeSourceUnionDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceUnionDynamic))
    }
}
pub(crate) struct TypeSourceUnionRefDynamic;

impl Aspect for TypeSourceUnionRefDynamic { }

impl AspectBind for TypeSourceUnionRefDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceUnionRefDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceUnionRefDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceUnionRefDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeSourceUnionRefDynamic),
        })
    }
}

impl AspectLift for TypeSourceUnionRefDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let tu_ref: &Rc<super::SourceUnionRef> = from_dynamic_ref(&value);
        let tu_type: Rc<dyn Type> = Rc::new(TypeSourceUnionConst { data: tu_ref.data.clone() });
        Ok(Rc::new(ValueAnyConst { inner: tu_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceUnionRefDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceUnionRefDynamic),
            })),
        }
    }
}

impl ExporterAny for TypeSourceUnionRefDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeSourceUnionRefDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeSourceUnionRefDynamic));
    }
}

impl Type for TypeSourceUnionRefDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceUnionRefDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == union_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionRefDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionRefDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionRefDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionRefDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_lift_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionRefDynamic) as Box<dyn AspectLift>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceUnionRefDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &union_ref_type_identity()).is_some() ||
            source_type.aspect(xx, &aspect_union_aspect_id()).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &union_ref_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceUnionRefDynamic),
                })),
            });
        }
        if source_type.aspect(xx, &aspect_union_aspect_id()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceUnionRefDynamic),
                })),
            });
        }
        EvalRes::err(loc, format!("Cannot convert {} to union_ref", source_type.desc()))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }
        let source_code = match cv.code.cast::<CodeDynamic>() {
            EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(
                loc,
                format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc()),
            ),
        };
        let call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(
                        CodeDynamicBindingUse { id: TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID },
                    ),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceUnionRefDynamic) as Rc<dyn Type>);
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(call)),
            },
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            })),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceUnionRefDynamic))
    }

    fn desc(&self) -> String {
        "union_ref".to_string()
    }
}

impl Value for TypeSourceUnionRefDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeSourceUnionRefDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceUnionRefDynamic))
    }
}

impl Push for Rc<SourceUnionRef> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeSourceUnionRefDynamic)
    }

    fn from_dynamic<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_dynamic_ref::<Rc<SourceUnionRef>>(&*value).clone()
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_dynamic_ext(scope, self).into()
    }
}
