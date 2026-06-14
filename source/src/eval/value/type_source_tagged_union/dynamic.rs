use crate::r#gen::ScopeEntry;
use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            AspectTaggedUnion,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_tagged_union_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            from_v8_ref,
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
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
        rc::Rc,
    },
    super::{
        SourceTaggedUnionData,
        SourceTaggedUnionRef,
        r#const::TypeSourceTaggedUnionConst,
    },
};
use crate::r#gen::tagged_union_ref_type_identity;


pub(crate) struct TypeSourceTaggedUnionDynamic {
    pub data: Rc<SourceTaggedUnionData>,
}

impl Aspect for TypeSourceTaggedUnionDynamic { }

/// Parameterless mortar marker for `Rc<SourceTaggedUnionRef>` v8 marshaling.
/// See `TypeSourceDataTypeRefDynamic` — same pattern.
pub(crate) struct TypeSourceTaggedUnionRefDynamic;

impl Aspect for TypeSourceTaggedUnionRefDynamic { }

impl AspectConsumeDynamic for TypeSourceTaggedUnionRefDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for TypeSourceTaggedUnionRefDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectBind for TypeSourceTaggedUnionRefDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectInitialBind for TypeSourceTaggedUnionRefDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeSourceTaggedUnionRefDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeSourceTaggedUnionRefDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let tu_ref: &Rc<super::SourceTaggedUnionRef> = from_v8_ref(&value);
        let tu_type: Rc<dyn Type> = Rc::new(TypeSourceTaggedUnionConst { data: tu_ref.data.clone() });
        Ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner: tu_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceTaggedUnionRefDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceTaggedUnionRefDynamic),
            }),
        }
    }
}

impl Type for TypeSourceTaggedUnionRefDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceTaggedUnionRefDynamic)
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceTaggedUnionRefDynamic))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == tagged_union_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionRefDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionRefDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionRefDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionRefDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionRefDynamic) as Box<dyn AspectLiftDynamic>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionRefDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some()
            || source_type.aspect(xx, &aspect_tagged_union_aspect_id()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceTaggedUnionRefDynamic),
                }),
            });
        }
        if source_type.aspect(xx, &aspect_tagged_union_aspect_id()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceTaggedUnionRefDynamic),
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert {} to tagged_union_ref", source_type.desc()))
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &crate::r#gen::type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }
        // Emit a call to the `__tagged_union_ref_to_type` rust-in-v8 function.
        let source_code = match cv.code.cast::<crate::code_dynamic::CodeDynamic>() {
            crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(
                loc,
                format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc()),
            ),
        };
        let call = crate::code_dynamic::CodeDynamic {
            loc,
            type_: crate::code_dynamic::CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                func: Box::new(crate::code_dynamic::CodeDynamic {
                    loc,
                    type_: crate::code_dynamic::CodeDynamicType::BindingUse(crate::code_dynamic::CodeDynamicBindingUse {
                        id: crate::eval::TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID,
                    }),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceTaggedUnionRefDynamic) as Rc<dyn Type>);
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(call)) },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            }),
        })
    }

    fn desc(&self) -> String {
        "tagged_union_ref".to_string()
    }

    fn type_to_serial(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
    ) -> Option<crate::langhelp::Artifact> {
        Some(Self::build_type_artifact())
    }
}


impl Value for TypeSourceTaggedUnionRefDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl AspectBind for TypeSourceTaggedUnionDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceTaggedUnionDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceTaggedUnionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceTaggedUnionDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }),
        })
    }
}

impl AspectLiftDynamic for TypeSourceTaggedUnionDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let mortar = must_downcast_ref::<TypeSourceTaggedUnionDynamic>(type_.as_ref());
        let tu_type: Rc<dyn Type> = Rc::new(TypeSourceTaggedUnionConst { data: mortar.data.clone() });
        Ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner: tu_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceTaggedUnionDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }),
            }),
        }
    }
}

impl Type for TypeSourceTaggedUnionDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == tagged_union_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }) as
                        Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }) as Box<dyn AspectLiftDynamic>,
                ),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }
        {
            let data = &self.data;
            if *key == data.identity {
                return Some(Box::new(true));
            }
            if *key == aspect_tagged_union_aspect_id() {
                return Some(
                    Box::new(
                        Box::new(TypeSourceTaggedUnionConst { data: data.clone() }) as Box<dyn AspectTaggedUnion>,
                    ),
                );
            }
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        if source_type.aspect(xx, &self.data.identity).is_some() {
            return true;
        }
        if source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some() {
            return true;
        }
        source_type.aspect(xx, &aspect_tagged_union_aspect_id()).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &self.data.identity).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &self.data.identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }),
                }),
            });
        }
        if source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }),
                }),
            });
        }
        if source_type.aspect(xx, &aspect_tagged_union_aspect_id()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: source_type,
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert {} to tagged_union_ref", source_type.desc()))
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }),
            }),
        })
    }

    fn desc(&self) -> String {
        format!("tagged_union_dyn")
    }

    fn type_to_serial(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
    ) -> Option<crate::langhelp::Artifact> {
        // Round-trip via the ref-form deserializer for now. This loses the
        // specific tagged-union's identity / variants — a proper serialization
        // would mirror the per-data-type artifact for the source data type.
        Some(TypeSourceTaggedUnionRefDynamic::build_type_artifact())
    }
}

impl Value for TypeSourceTaggedUnionDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceTaggedUnionRef> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        crate::eval::from_v8_ref::<Rc<SourceTaggedUnionRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, self).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeSourceTaggedUnionRefDynamic)
    }
}
