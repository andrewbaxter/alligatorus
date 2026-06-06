use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
            AspectJsString,
            AspectLiftJs,
            AspectSet,
            aspect_artifact_aspect_id,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_string_aspect_id,
            aspect_js_string_get_maybe,
            aspect_lift_js_aspect_id,
            aspect_set_aspect_id,
            aspect_string_get,
            aspect_string_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeBlock,
            JsCodeBlockEntry,
            JsCodeFieldUse,
            JsCodeType,
        },
        langhelp::{
            Artifact,
            ArtifactData,
            ArtifactId,
            ArtifactIdBuiltin,
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            GlobalContext,
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
        mortar_function::MortarTypeFunction,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataAccess,
            MortarJsValueSource,
            mortar_bind_js,
            mortar_consume_js_to_js,
            mortar_js_stack_to_value,
            mortar_set,
        },
        mortar_null::TypeMortarNull,
    },
};

pub(crate) struct TypeMortarString;

impl Value for TypeMortarString {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeAny)
    }
}

impl Type for TypeMortarString {
    fn desc(&self) -> String {
        return format!("mortar string");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeMortarString)
    }

    fn aspect(&self, _ex: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_js_string_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectJsString>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectField>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_set_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectSet>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(TypeMortarString) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        aspect_js_string_get_maybe(xx, type_).is_some()
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(TypeMortarString),
            }),
        })
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_string_get_maybe(xx, source_type).is_some() || aspect_js_string_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let type_ = cv.value.type_(xx, loc);
            if let Some(aspect_string) = aspect_string_get_maybe(xx, type_.as_ref()) {
                let CodeValue { code, value } = cv;
                let mut seq = vec![];
                if let Some(c) = erb.must_er(code.cast::<JsCode>())? {
                    seq.push(c.clone());
                };
                seq.push(JsCode {
                    loc: loc,
                    type_: JsCodeType::Literal(serde_json::Value::String(aspect_string.get(value.as_ref())))
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
                        type_: Rc::new(TypeMortarString),
                    }),
                });
            } else {
                return Ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(TypeMortarString),
                    })
                });
            }
        });
    }
}

impl Aspect for TypeMortarString { }

impl AspectJsString for TypeMortarString { }

impl AspectJsStackValue for TypeMortarString {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        mortar_js_stack_to_value(loc, value, Rc::new(TypeMortarString))
    }
}

impl AspectConsumeJs for TypeMortarString {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        mortar_consume_js_to_js(xx, loc, cv)
    }
}

impl AspectBindJs for TypeMortarString {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        mortar_bind_js(xx, loc, cv)
    }
}

impl AspectField for TypeMortarString {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on mortar string", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc: loc,
                        type_: JsCodeType::FieldUse(JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: key_name.clone(),
                        }),
                    })),
                },
                value: Rc::new(MortarJsValue {
                    source: MortarJsValueSource::Field(MortarJsValueDataAccess { key: key_name, }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "toLowerCase" => Some(Rc::new(MortarTypeFunction {
                arg: Rc::new(TypeMortarNull),
                ret: Rc::new(TypeMortarString),
            })),
            "toUpperCase" => Some(Rc::new(MortarTypeFunction {
                arg: Rc::new(TypeMortarNull),
                ret: Rc::new(TypeMortarString),
            })),
            _ => None,
        }
    }
}

impl AspectLiftJs for TypeMortarString {
    fn lift(&self, scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Rc<dyn Value>, String> {
        let s = value.to_string(scope).unwrap().to_rust_string_lossy(scope);
        Ok(Rc::new(super::string::ValueString(s)))
    }
}

impl AspectArtifact for TypeMortarString {
    fn to_serial(&self, _xx: &ExprContext, _gx: &GlobalContext, _value: Rc<dyn Value>) -> EResult<Artifact> {
        EResult::ok(Artifact {
            type_: ArtifactId::Builtin(ArtifactIdBuiltin::from_unique(&super::string::string_artifact_identity())),
            data: ArtifactData::Null,
        })
    }

    fn from_serial(
        &self,
        _xx: &ExprContext,
        _gx: &GlobalContext,
        _data: &ArtifactData,
    ) -> Result<Rc<dyn Value>, String> {
        Ok(Rc::new(TypeMortarString) as Rc<dyn Value>)
    }
}

impl AspectSet for TypeMortarString {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        mortar_set(xx, loc, self_, value)
    }
}
