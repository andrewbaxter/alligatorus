use {
    super::{
        TaggedUnionConstructorRef,
        TaggedUnionData,
        TaggedUnionRef,
    },
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectFunction,
            AspectJsStackValue,
            AspectLiftJs,
            AspectTaggedUnion,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_function_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_lift_js_aspect_id,
            aspect_tagged_union_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeNewObject,
            JsCodeType,
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
    crate::eval::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
            bind_js_some,
        },
        from_v8_ref,
        typecheck_convert,
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

fn tagged_union_ref_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

// --- Mortar type for TaggedUnionRef (during eval_half / v8 phase) ---
pub(crate) struct MortarTypeTaggedUnionRef {
    pub data: Option<Rc<TaggedUnionData>>,
}

impl Value for MortarTypeTaggedUnionRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for MortarTypeTaggedUnionRef {
    fn desc(&self) -> String {
        format!("tagged_union_ref")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == tagged_union_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(
                Box::new(
                    Box::new(MortarTypeTaggedUnionRef { data: self.data.clone() }) as Box<dyn AspectConsumeJs>,
                ),
            );
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeTaggedUnionRef { data: self.data.clone() }) as Box<dyn AspectBindJs>),
            );
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(
                Box::new(
                    Box::new(MortarTypeTaggedUnionRef { data: self.data.clone() }) as Box<dyn AspectJsStackValue>,
                ),
            );
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeTaggedUnionRef { data: self.data.clone() }) as Box<dyn AspectLiftJs>),
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
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeTaggedUnionRef { data: self.data.clone() }),
            }),
        })
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> bool {
        let source_type = cv.value.type_(xx, loc);
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
                    value: Box::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(MortarTypeTaggedUnionRef { data: self.data.clone() }),
                    }),
                });
            }
        } else {
            if source_type.aspect(xx, &tagged_union_ref_type_identity()).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Box::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(MortarTypeTaggedUnionRef { data: self.data.clone() }),
                    }),
                });
            }
        }
        if source_type.aspect(xx, &aspect_tagged_union_aspect_id()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: source_type,
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert {} to tagged_union_ref", source_type.desc()))
    }
}

impl Aspect for MortarTypeTaggedUnionRef { }

impl AspectConsumeJs for MortarTypeTaggedUnionRef {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeTaggedUnionRef {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn crate::eval::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectJsStackValue for MortarTypeTaggedUnionRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeTaggedUnionRef { data: self.data.clone() }),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeTaggedUnionRef {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let mortar =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<MortarTypeTaggedUnionRef>()
                .expect("MortarTypeTaggedUnionRef::lift: expected MortarTypeTaggedUnionRef");
        let data = mortar.data.clone().expect("MortarTypeTaggedUnionRef::lift: data must be Some");
        Ok(Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(TaggedUnionType { data }),
        }))
    }
}

// --- Mortar type for TaggedUnionConstructorRef (during eval_half / v8 phase) ---
pub(crate) struct MortarTypeTaggedUnionConstructorRef;

impl Value for MortarTypeTaggedUnionConstructorRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for MortarTypeTaggedUnionConstructorRef {
    fn desc(&self) -> String {
        format!("tagged_union_constructor_ref")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionConstructorRef) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionConstructorRef) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionConstructorRef) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionConstructorRef) as Box<dyn AspectLiftJs>));
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

impl Aspect for MortarTypeTaggedUnionConstructorRef { }

impl AspectConsumeJs for MortarTypeTaggedUnionConstructorRef {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeTaggedUnionConstructorRef {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn crate::eval::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectJsStackValue for MortarTypeTaggedUnionConstructorRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeTaggedUnionConstructorRef),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeTaggedUnionConstructorRef {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let type_: &Rc<dyn Type> = from_v8_ref(&value);
        let ctor =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<TaggedUnionConstructor>()
                .expect("MortarTypeTaggedUnionConstructorRef::lift: expected TaggedUnionConstructor");
        Ok(Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(TaggedUnionConstructor {
                data: ctor.data.clone(),
                variant_index: ctor.variant_index,
            }),
        }))
    }
}

// --- JsWrap impls ---
fn tagged_union_ref_from_v8_type(type_: &Rc<dyn Type>) -> Rc<TaggedUnionRef> {
    let mortar =
        (type_.as_ref() as &dyn Any)
            .downcast_ref::<MortarTypeTaggedUnionRef>()
            .expect("TaggedUnionRef: expected MortarTypeTaggedUnionRef");
    Rc::new(TaggedUnionRef { data: mortar.data.clone().expect("TaggedUnionRef: data must be Some") })
}

impl crate::eval::jswrap::JsWrap for Rc<TaggedUnionRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeTaggedUnionRef { data: None })
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        tagged_union_ref_from_v8_type(crate::eval::from_v8_ref::<Rc<dyn Type>>(&*value))
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        let type_: Rc<dyn Type> = Rc::new(MortarTypeTaggedUnionRef { data: Some(self.data.clone()) });
        crate::eval::to_v8(scope, type_).into()
    }
}

impl crate::eval::jswrap::JsWrap for Rc<TaggedUnionConstructorRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeTaggedUnionConstructorRef)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let type_: &Rc<dyn Type> = crate::eval::from_v8_ref(&*value);
        let ctor =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<TaggedUnionConstructor>()
                .expect("TaggedUnionConstructorRef::from_v8: expected TaggedUnionConstructor");
        Rc::new(TaggedUnionConstructorRef {
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
    pub data: Rc<TaggedUnionData>,
}

impl Value for TaggedUnionType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for TaggedUnionType {
    fn desc(&self) -> String {
        format!("tagged_union")
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
        if *key == aspect_consume_js_aspect_id() {
            return Some(
                Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectConsumeJs>),
            );
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(
                Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectBindJs>),
            );
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(
                Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectJsStackValue>),
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

impl AspectConsumeJs for TaggedUnionType {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for TaggedUnionType {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn crate::eval::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectJsStackValue for TaggedUnionType {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TaggedUnionType { data: self.data.clone() }),
            }),
        }
    }
}

// ## --- Post-lift: TaggedUnionConstructor (callable to create tagged union values)
pub(crate) struct TaggedUnionConstructor {
    pub data: Rc<TaggedUnionData>,
    pub variant_index: usize,
}

impl Value for TaggedUnionConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for TaggedUnionConstructor {
    fn desc(&self) -> String {
        let variants = self.data.variants.borrow();
        let tag = &variants[self.variant_index].tag;
        format!("tagged_union_constructor({:?})", tag)
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
            let to_js = erb.must_er(aspect_consume_js_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_js.to_js(xx, loc, converted))?;
            let result_type = Rc::new(TaggedUnionType { data: self.data.clone() });
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::NewObject(JsCodeNewObject { fields: vec![(js_tag, underlying_js)], }),
            }));
        });
    }
}
