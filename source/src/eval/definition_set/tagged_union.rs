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
        },
        rc_from_v8,
        typecheck_convert,
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

// --- Mortar type for TaggedUnionRef (during eval_half / v8 phase) ---

pub(crate) struct MortarTypeTaggedUnionRef;

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
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionRef) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionRef) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionRef) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeTaggedUnionRef) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union_ref"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union_ref"))
    }
}

impl Aspect for MortarTypeTaggedUnionRef { }

impl AspectConsumeJs for MortarTypeTaggedUnionRef {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeTaggedUnionRef {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectJsStackValue for MortarTypeTaggedUnionRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeTaggedUnionRef),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeTaggedUnionRef {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Box<dyn Value> {
        let ref_: Rc<TaggedUnionRef> = rc_from_v8(&value);
        Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(TaggedUnionType { data: ref_.data.clone() }),
        })
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

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union_constructor_ref"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
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
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectJsStackValue for MortarTypeTaggedUnionConstructorRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeTaggedUnionConstructorRef),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeTaggedUnionConstructorRef {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Box<dyn Value> {
        let ref_: Rc<TaggedUnionConstructorRef> = rc_from_v8(&value);
        Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(TaggedUnionConstructor {
                data: ref_.data.clone(),
                variant_index: ref_.variant_index,
            }),
        })
    }
}

// --- JsWrap impls ---

impl crate::eval::jswrap::JsWrap for Rc<TaggedUnionRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeTaggedUnionRef)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
    }
}

impl crate::eval::jswrap::JsWrap for Rc<TaggedUnionConstructorRef> {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(MortarTypeTaggedUnionConstructorRef)
    }
    fn from_v8<'s>(
        _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        value: v8::Local<'s, v8::Value>,
    ) -> Self {
        crate::eval::rc_from_v8(&*value)
    }
    fn into_v8<'s>(
        self,
        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
    ) -> v8::Local<'s, v8::Value> {
        crate::eval::rc_to_v8(scope, self).into()
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
            return Some(Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectTaggedUnion>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TaggedUnionType { data: self.data.clone() }) as Box<dyn AspectJsStackValue>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union"))
    }
}

impl Aspect for TaggedUnionType { }

impl AspectTaggedUnion for TaggedUnionType {
    fn variants(&self) -> Vec<(crate::langhelp::ConstKey, Rc<dyn Type>)> {
        self.data.variants.borrow().iter().map(|v| (v.tag.clone(), v.type_.clone())).collect()
    }
}

impl AspectConsumeJs for TaggedUnionType {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for TaggedUnionType {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectJsStackValue for TaggedUnionType {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code { loc, code: Code_::Dyn(Rc::new(value)) },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TaggedUnionType { data: self.data.clone() }),
            }),
        }
    }
}

// --- Post-lift: TaggedUnionConstructor (callable to create tagged union values) ---

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

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tagged_union_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tagged_union_constructor"))
    }
}

impl Aspect for TaggedUnionConstructor { }

impl AspectFunction for TaggedUnionConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let variants = self.data.variants.borrow();
        let variant = &variants[self.variant_index];
        let tag = variant.tag.clone();
        let underlying_type = variant.type_.clone();
        drop(variants);

        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;

            let to_js = erb.must_er(aspect_consume_js_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_js.to_js(xx, loc, converted))?;

            let tag_str = match &tag {
                crate::langhelp::ConstKey::String(s) => s.clone(),
            };

            let result_type = Rc::new(TaggedUnionType { data: self.data.clone() });
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::NewObject(JsCodeNewObject {
                    fields: vec![(tag_str, underlying_js)],
                }),
            }));
        });
    }
}
