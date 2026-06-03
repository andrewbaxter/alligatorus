use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectJsBool,
            AspectJsStackValue,
            aspect_bind_js_aspect_id,
            aspect_bool_get_maybe,
            aspect_consume_js_aspect_id,
            aspect_js_bool_aspect_id,
            aspect_js_bool_get_maybe,
            aspect_js_stack_value_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeBlock,
            JsCodeBlockEntry,
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
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
            mortar_bind_js,
            mortar_consume_js_to_js,
            mortar_js_stack_to_value,
        },
    },
};

pub(crate) struct TypeMortarBool;

impl Value for TypeMortarBool {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeAny)
    }
}

impl Type for TypeMortarBool {
    fn desc(&self) -> String {
        return format!("mortar bool");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeMortarBool)
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_js_bool_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarBool) as Box<dyn AspectJsBool>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarBool) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarBool) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarBool) as Box<dyn AspectJsStackValue>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_js_bool_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarBool),
            }),
        })
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_bool_get_maybe(xx, source_type).is_some() || aspect_js_bool_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            if let Some(aspect_bool) = aspect_bool_get_maybe(xx, type_.as_ref()) {
                let CodeValue { code, value } = cv;
                let mut seq = vec![];
                if let Some(c) = erb.must_er(code.cast::<JsCode>())? {
                    seq.push(c.clone());
                };
                let bool_val = aspect_bool.get(value.as_ref());
                seq.push(JsCode {
                    loc: loc,
                    type_: JsCodeType::Literal(serde_json::Value::Bool(bool_val))
                });
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(JsCode {
                            loc: loc,
                            type_: JsCodeType::Block(
                                JsCodeBlock { entries: seq.into_iter().map(JsCodeBlockEntry::Code).collect() }
                            )
                        }))
                    },
                    value: Rc::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(TypeMortarBool),
                    }),
                });
            } else {
                return Ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(TypeMortarBool),
                    })
                });
            }
        });
    }
}

impl Aspect for TypeMortarBool { }

impl AspectJsBool for TypeMortarBool { }

impl AspectJsStackValue for TypeMortarBool {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        mortar_js_stack_to_value(loc, value, Rc::new(TypeMortarBool))
    }
}

impl AspectConsumeJs for TypeMortarBool {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        mortar_consume_js_to_js(xx, loc, cv)
    }
}

impl AspectBindJs for TypeMortarBool {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        mortar_bind_js(xx, loc, cv)
    }
}
