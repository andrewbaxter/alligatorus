use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamicSub,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_sub_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            aspect::ScopeEntryDynamicSub,
            from_v8_ref,
            value::type_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic,
                bind_dynamic_sub_default,
            },
        },
        langhelp::{
            Aspect,
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

impl Value for TypeSourceTaggedUnionConstructorDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceTaggedUnionConstructorDynamic {
    fn desc(&self) -> String {
        format!("tagged_union_constructor_ref")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceTaggedUnionConstructorDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectConsumeDynamic>),
            );
        }
        if *key == aspect_bind_dynamic_sub_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceTaggedUnionConstructorDynamic) as Box<dyn AspectBindDynamicSub>));
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
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union_constructor_ref"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union_constructor_ref"))
    }
}

impl Aspect for TypeSourceTaggedUnionConstructorDynamic { }

impl AspectConsumeDynamic for TypeSourceTaggedUnionConstructorDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamicSub for TypeSourceTaggedUnionConstructorDynamic {
    fn bind_dynamic_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(CodeDynamic, ScopeEntryDynamicSub)> {
        bind_dynamic_sub_default(xx, loc, cv)
    }
}

impl AspectBind for TypeSourceTaggedUnionConstructorDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_dynamic(xx, loc, cv)
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

impl AspectLiftDynamic for TypeSourceTaggedUnionConstructorDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let ctor =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<TypeSourceTaggedUnionConstructorConst>()
                .expect(
                    "TypeSourceTaggedUnionConstructorDynamic::lift: expected TypeSourceTaggedUnionConstructorConst",
                );
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(TypeSourceTaggedUnionConstructorConst {
                data: ctor.data.clone(),
                variant_index: ctor.variant_index,
            }),
        }))
    }
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceTaggedUnionConstructorRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(TypeSourceTaggedUnionConstructorDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn Type> = crate::eval::from_v8_ref(&*value);
        let ctor =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<TypeSourceTaggedUnionConstructorConst>()
                .expect("SourceTaggedUnionConstructorRef::from_v8: expected TypeSourceTaggedUnionConstructorConst");
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
}
