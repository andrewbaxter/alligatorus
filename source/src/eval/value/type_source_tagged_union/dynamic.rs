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
            ScopeEntry,
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

fn tagged_union_ref_from_v8_type(type_: &Rc<dyn Type>) -> Rc<SourceTaggedUnionRef> {
    let mortar = must_downcast_ref::<TypeSourceTaggedUnionDynamic>(type_.as_ref());
    Rc::new(SourceTaggedUnionRef { data: mortar.data.clone().expect("SourceTaggedUnionRef: data must be Some") })
}

fn tagged_union_ref_type_identity() -> Unique {
    Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

pub(crate) struct TypeSourceTaggedUnionDynamic {
    pub data: Option<Rc<SourceTaggedUnionData>>,
}

impl Aspect for TypeSourceTaggedUnionDynamic { }

impl AspectBind for TypeSourceTaggedUnionDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
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
        let data = mortar.data.clone().expect("TypeSourceTaggedUnionDynamic::lift: data must be Some");
        Ok(Rc::new(TypeSourceTaggedUnionConst { data }))
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
        if let Some(data) = &self.data {
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
        if let Some(data) = &self.data {
            if source_type.aspect(xx, &data.identity).is_some() {
                return true;
            }
        } else {
            if source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some() {
                return true;
            }
        }
        source_type.aspect(xx, &aspect_tagged_union_aspect_id()).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        if let Some(data) = &self.data {
            type_.aspect(xx, &data.identity).is_some()
        } else {
            type_.aspect(xx, &tagged_union_ref_type_identity()).is_some()
        }
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if let Some(data) = &self.data {
            if source_type.aspect(xx, &data.identity).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }),
                    }),
                });
            }
        } else {
            if source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceTaggedUnionDynamic { data: self.data.clone() }),
                    }),
                });
            }
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
        format!("tagged_union_ref")
    }
}

impl Value for TypeSourceTaggedUnionDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceTaggedUnionRef> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        tagged_union_ref_from_v8_type(crate::eval::from_v8_ref::<Rc<dyn Type>>(&*value))
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn Type> = Rc::new(TypeSourceTaggedUnionDynamic { data: Some(self.data.clone()) });
        crate::eval::to_v8(scope, type_).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeSourceTaggedUnionDynamic { data: None })
    }
}
