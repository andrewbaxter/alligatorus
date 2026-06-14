use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
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
        SourceTaggedUnionConstructorRef,
        r#const::TypeSourceTaggedUnionConstructorConst,
    },
};

pub(crate) struct TypeSourceTaggedUnionConstructorDynamic;

impl Aspect for TypeSourceTaggedUnionConstructorDynamic { }

impl AspectBind for TypeSourceTaggedUnionConstructorDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceTaggedUnionConstructorDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceTaggedUnionConstructorDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceTaggedUnionConstructorDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceTaggedUnionConstructorDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeSourceTaggedUnionConstructorDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let ctor = must_downcast_ref::<TypeSourceTaggedUnionConstructorConst>(type_.as_ref());
        let ctor_type: Rc<dyn Type> = Rc::new(TypeSourceTaggedUnionConstructorConst {
            data: ctor.data.clone(),
            variant_index: ctor.variant_index,
        });
        Ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner: ctor_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceTaggedUnionConstructorDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceTaggedUnionConstructorDynamic),
            }),
        }
    }
}

impl Type for TypeSourceTaggedUnionConstructorDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceTaggedUnionConstructorDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectConsumeDynamic>),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectBindDynamic>),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectStackValueDynamic>),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectLiftDynamic>),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectInitialBind>),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union_constructor_ref"))
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &crate::eval::value::type_any::r#const::type_type_identity()).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &crate::eval::value::type_any::r#const::type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }
        let source_code = match cv.code.cast::<crate::code_dynamic::CodeDynamic>() {
            crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(loc, format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc())),
        };
        let call = crate::code_dynamic::CodeDynamic {
            loc,
            type_: crate::code_dynamic::CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                func: Box::new(crate::code_dynamic::CodeDynamic {
                    loc,
                    type_: crate::code_dynamic::CodeDynamicType::BindingUse(crate::code_dynamic::CodeDynamicBindingUse {
                        id: crate::eval::TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID,
                    }),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceTaggedUnionConstructorDynamic) as Rc<dyn Type>);
        EvalRes::ok(CodeValue {
            code: crate::langhelp::Code { loc, code: crate::langhelp::Code_::Dyn(Rc::new(call)) },
            value: Rc::new(crate::eval::dynamic_util::ValueDynamic {
                source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                type_: dest_rc,
            }),
        })
    }

    fn desc(&self) -> String {
        format!("tagged_union_constructor_ref")
    }
}

impl Value for TypeSourceTaggedUnionConstructorDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceTaggedUnionConstructorRef> {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn Type> = crate::eval::from_v8_ref(&*value);
        let ctor = must_downcast_ref::<TypeSourceTaggedUnionConstructorConst>(type_.as_ref());
        Rc::new(SourceTaggedUnionConstructorRef {
            data: ctor.data.clone(),
            variant_index: ctor.variant_index,
        })
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn Type> = Rc::new(TypeSourceTaggedUnionConstructorConst {
            data: self.data.clone(),
            variant_index: self.variant_index,
        });
        crate::eval::to_v8(scope, type_).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeSourceTaggedUnionConstructorDynamic)
    }
}
