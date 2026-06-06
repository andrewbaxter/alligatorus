use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectFunction,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            AspectTaggedUnion,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_stack_value_dynamic_get,
            aspect_tagged_union_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            Erb,
            from_v8_ref,
            typecheck_convert,
            value::type_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_some,
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
        mem,
        rc::Rc,
    },
    super::{
        SourceTaggedUnionConstructorRef,
        SourceTaggedUnionData,
        SourceTaggedUnionRef,
    },
};

fn tagged_union_ref_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

// --- Mortar type for SourceTaggedUnionRef (during eval_half / v8 phase) ---
pub(crate) struct MortarTypeSourceTaggedUnionRef {
    pub data: Option<Rc<SourceTaggedUnionData>>,
}

impl Value for MortarTypeSourceTaggedUnionRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for MortarTypeSourceTaggedUnionRef {
    fn desc(&self) -> String {
        format!("tagged_union_ref")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == tagged_union_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }) as
                        Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }) as
                        Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }) as
                        Box<dyn AspectLiftDynamic>,
                ),
            );
        }
        if let Some(data) = &self.data {
            if *key == data.identity {
                return Some(Box::new(true));
            }
            if *key == aspect_tagged_union_aspect_id() {
                return Some(
                    Box::new(Box::new(TaggedUnionType { data: data.clone() }) as Box<dyn AspectTaggedUnion>),
                );
            }
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        if let Some(data) = &self.data {
            type_.aspect(xx, &data.identity).is_some()
        } else {
            type_.aspect(xx, &tagged_union_ref_type_identity()).is_some()
        }
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }),
            }),
        })
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

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if let Some(data) = &self.data {
            if source_type.aspect(xx, &data.identity).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }),
                    }),
                });
            }
        } else {
            if source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }),
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
}

impl Aspect for MortarTypeSourceTaggedUnionRef { }

impl AspectConsumeDynamic for MortarTypeSourceTaggedUnionRef {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for MortarTypeSourceTaggedUnionRef {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
    }
}

impl AspectStackValueDynamic for MortarTypeSourceTaggedUnionRef {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(MortarTypeSourceTaggedUnionRef { data: self.data.clone() }),
            }),
        }
    }
}

impl AspectLiftDynamic for MortarTypeSourceTaggedUnionRef {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let mortar =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<MortarTypeSourceTaggedUnionRef>()
                .expect("MortarTypeSourceTaggedUnionRef::lift: expected MortarTypeSourceTaggedUnionRef");
        let data = mortar.data.clone().expect("MortarTypeSourceTaggedUnionRef::lift: data must be Some");
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(TaggedUnionType { data }),
        }))
    }
}

// --- Mortar type for SourceTaggedUnionConstructorRef (during eval_half / v8
// phase) ---
pub(crate) struct MortarTypeSourceTaggedUnionConstructorRef;

impl Value for MortarTypeSourceTaggedUnionConstructorRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for MortarTypeSourceTaggedUnionConstructorRef {
    fn desc(&self) -> String {
        format!("tagged_union_constructor_ref")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(MortarTypeSourceTaggedUnionConstructorRef)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeSourceTaggedUnionConstructorRef) as Box<dyn AspectConsumeDynamic>),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeSourceTaggedUnionConstructorRef) as Box<dyn AspectBindDynamic>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeSourceTaggedUnionConstructorRef) as Box<dyn AspectStackValueDynamic>),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeSourceTaggedUnionConstructorRef) as Box<dyn AspectLiftDynamic>),
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

impl Aspect for MortarTypeSourceTaggedUnionConstructorRef { }

impl AspectConsumeDynamic for MortarTypeSourceTaggedUnionConstructorRef {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for MortarTypeSourceTaggedUnionConstructorRef {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
    }
}

impl AspectStackValueDynamic for MortarTypeSourceTaggedUnionConstructorRef {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(MortarTypeSourceTaggedUnionConstructorRef),
            }),
        }
    }
}

impl AspectLiftDynamic for MortarTypeSourceTaggedUnionConstructorRef {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let ctor =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<TaggedUnionConstructor>()
                .expect("MortarTypeSourceTaggedUnionConstructorRef::lift: expected TaggedUnionConstructor");
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(TaggedUnionConstructor {
                data: ctor.data.clone(),
                variant_index: ctor.variant_index,
            }),
        }))
    }
}

// --- DynamicWrap impls ---
fn tagged_union_ref_from_v8_type(type_: &Rc<dyn Type>) -> Rc<SourceTaggedUnionRef> {
    let mortar =
        (type_.as_ref() as &dyn Any)
            .downcast_ref::<MortarTypeSourceTaggedUnionRef>()
            .expect("SourceTaggedUnionRef: expected MortarTypeSourceTaggedUnionRef");
    Rc::new(SourceTaggedUnionRef { data: mortar.data.clone().expect("SourceTaggedUnionRef: data must be Some") })
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceTaggedUnionRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeSourceTaggedUnionRef { data: None })
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        tagged_union_ref_from_v8_type(crate::eval::from_v8_ref::<Rc<dyn Type>>(&*value))
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn Type> = Rc::new(MortarTypeSourceTaggedUnionRef { data: Some(self.data.clone()) });
        crate::eval::to_v8(scope, type_).into()
    }
}

impl crate::eval::dynamic_wrap::DynamicWrap for Rc<SourceTaggedUnionConstructorRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeSourceTaggedUnionConstructorRef)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn Type> = crate::eval::from_v8_ref(&*value);
        let ctor =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<TaggedUnionConstructor>()
                .expect("SourceTaggedUnionConstructorRef::from_v8: expected TaggedUnionConstructor");
        Rc::new(SourceTaggedUnionConstructorRef {
            data: ctor.data.clone(),
            variant_index: ctor.variant_index,
        })
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn Type> = Rc::new(TaggedUnionConstructor {
            data: self.data.clone(),
            variant_index: self.variant_index,
        });
        crate::eval::to_v8(scope, type_).into()
    }
}

// --- Post-lift: TaggedUnionType (type of tagged union values) ---
pub(crate) struct TaggedUnionType {
    pub data: Rc<SourceTaggedUnionData>,
}

impl Value for TaggedUnionType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TaggedUnionType {
    fn desc(&self) -> String {
        format!("tagged_union")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TaggedUnionType { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == aspect_tagged_union_aspect_id() {
            return Some(
                Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectTaggedUnion>),
            );
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectBindDynamic>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectStackValueDynamic>),
            );
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union"))
    }
}

impl Aspect for TaggedUnionType { }

impl AspectTaggedUnion for TaggedUnionType {
    fn variants(&self) -> Vec<(crate::langhelp::ConstKey, String, Rc<dyn Type>)> {
        self.data.variants.borrow().iter().map(|v| (v.tag.clone(), v.js_tag.clone(), v.type_.clone())).collect()
    }
}

impl AspectConsumeDynamic for TaggedUnionType {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for TaggedUnionType {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
    }
}

impl AspectStackValueDynamic for TaggedUnionType {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TaggedUnionType { data: self.data.clone() }),
            }),
        }
    }
}

// ## --- Post-lift: TaggedUnionConstructor (callable to create tagged union values)
pub(crate) struct TaggedUnionConstructor {
    pub data: Rc<SourceTaggedUnionData>,
    pub variant_index: usize,
}

impl Value for TaggedUnionConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TaggedUnionConstructor {
    fn desc(&self) -> String {
        let variants = self.data.variants.borrow();
        let tag = &variants[self.variant_index].tag;
        format!("tagged_union_constructor({:?})", tag)
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TaggedUnionConstructor {
            data: self.data.clone(),
            variant_index: self.variant_index,
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TaggedUnionConstructor {
                data: self.data.clone(),
                variant_index: self.variant_index,
            }) as Box<dyn AspectFunction>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union_constructor"))
    }
}

impl Aspect for TaggedUnionConstructor { }

impl AspectFunction for TaggedUnionConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let variants = self.data.variants.borrow();
        let variant = &variants[self.variant_index];
        let js_tag = variant.js_tag.clone();
        let underlying_type = variant.type_.clone();
        drop(variants);
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let result_type = Rc::new(TaggedUnionType { data: self.data.clone() });
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: vec![(js_tag, underlying_js)], }),
            }));
        });
    }
}
