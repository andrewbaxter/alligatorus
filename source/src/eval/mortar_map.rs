use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectFunction,
            AspectIterable,
            AspectJsStackValue,
            AspectMortarMap,
            AspectSet,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_consume_js_get_maybe,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_iterable_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_mortar_map_aspect_id,
            aspect_mortar_map_get_maybe,
            aspect_record_get_maybe,
            aspect_set_aspect_id,
            aspect_string_get,
            aspect_tuple_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeFieldUse,
            JsCodeForeachMap,
            JsCodeNewMap,
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
            ScopeEntryId,
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
            MortarJsValueDataAccess,
            MortarJsValueSource,
            bind_js_some,
        },
        tuple::StaticValue,
    },
};

/// Check if `source_type` can be converted to `dest_type` by either direction of
/// cast.
fn types_convertible(xx: &ExprContext, loc: QExprId, source_type: &dyn Type, dest_type: &dyn Type) -> bool {
    source_type.can_cast_to(xx, loc, dest_type) || dest_type.can_cast_from(xx, loc, source_type)
}

// --- map constructor (the "map" function in scope) ---
#[derive(Clone)]
pub(crate) struct TypeMapConstructor;

impl Value for TypeMapConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeMapConstructor)
    }
}

impl Type for TypeMapConstructor {
    fn desc(&self) -> String {
        "map constructor".to_string()
    }

    fn as_value(&self) -> Box<dyn Value> {
        Box::new(TypeMapConstructor)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeMapConstructor) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map constructor".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to map constructor".to_string())
    }
}

impl Aspect for TypeMapConstructor { }

impl AspectFunction for TypeMapConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_type = argument.value.type_(xx, loc);
        let arg_tuple = match aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            Some(t) => t,
            None => return EvalRes::err(loc, "map() requires a tuple of (key_type, value_type)".to_string()),
        };
        let element_types = arg_tuple.element_types();
        if element_types.len() != 2 {
            return EvalRes::err(
                loc,
                format!("map() requires exactly 2 type arguments (key, value), got {}", element_types.len()),
            );
        }
        let key_type = element_types[0].clone();
        let value_type = element_types[1].clone();
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Box::new(TypeMortarMap {
                key_type,
                value_type,
            }),
        })
    }
}

impl super::jswrap::JsWrap for TypeMortarMap {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let rc: Rc<TypeMortarMap> = super::from_v8_ref::<Rc<TypeMortarMap>>(&*value).clone();
        (*rc).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::to_v8(scope, Rc::new(self)).into()
    }
}

// --- map type (result of map(string, number), and also type of map instances) ---
#[derive(Clone)]
pub(crate) struct TypeMortarMap {
    pub(crate) key_type: Rc<dyn Type>,
    pub(crate) value_type: Rc<dyn Type>,
}

impl Value for TypeMortarMap {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}

impl Type for TypeMortarMap {
    fn desc(&self) -> String {
        format!("map({}, {})", self.key_type.desc(), self.value_type.desc())
    }

    fn as_value(&self) -> Box<dyn Value> {
        Box::new(TypeMortarMap { key_type: self.key_type.clone(), value_type: self.value_type.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_set_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectSet>));
        }
        if *key == aspect_iterable_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectIterable>));
        }
        if *key == aspect_mortar_map_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectMortarMap>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map type".to_string())
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        if let Some(source_map) = aspect_mortar_map_get_maybe(xx, source_type) {
            // Map → map: check element types are compatible
            return types_convertible(xx, loc, source_map.value_type().as_ref(), self.value_type.as_ref());
        }
        if aspect_record_get_maybe(xx, source_type).is_some() &&
            aspect_consume_js_get_maybe(xx, source_type).is_some() {
            // Record → map: check all field value types are compatible with the map's value
            // type
            if let Some(record) = (source_type as &dyn Any).downcast_ref::<super::record::Record>() {
                return record.fields.values().all(|f| {
                    let field_type = match f {
                        super::record::RecordField::Mortar(t) => t.clone(),
                        super::record::RecordField::EvalRes(er) => match &er.v {
                            Some(cv) => cv.value.type_(xx, loc),
                            None => return true,
                        },
                    };
                    types_convertible(xx, loc, field_type.as_ref(), self.value_type.as_ref())
                });
            }
            return true;
        }
        false
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if let Some(source_map) = aspect_mortar_map_get_maybe(xx, source_type.as_ref()) {
            if !types_convertible(xx, loc, source_map.value_type().as_ref(), self.value_type.as_ref()) {
                return EvalRes::err(
                    loc,
                    format!(
                        "Incompatible map value types: {} cannot convert to {}",
                        source_map.value_type().desc(),
                        self.value_type.desc()
                    ),
                );
            }
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        }

        // Record source: decompose, typecheck_convert each value to the map's value type,
        // then convert each to JS individually.
        let record: Box<super::record::Record> = match (cv.value as Box<dyn Any>).downcast() {
            Ok(r) => r,
            Err(_) => return EvalRes::err(loc, "Cannot convert non-record to map type".to_string()),
        };
        return er_seq_cv!(loc, erb, {
            let value_to_js = erb.must_er(aspect_consume_js_get(xx, loc, self.value_type.as_ref()))?;
            let mut js_fields = Vec::new();
            for (key, field) in record.fields.into_iter() {
                let canonical = key.to_str();
                let entry_cv = match field {
                    super::record::RecordField::EvalRes(entry_res) => {
                        if let Some(errs) = entry_res.errors {
                            erb.errors.extend(errs);
                        }
                        match entry_res.v {
                            Some(v) => v,
                            None => {
                                return erb.err_new(format!("Record entry '{}' failed to evaluate", canonical),);
                            },
                        }
                    },
                    super::record::RecordField::Mortar(_) => {
                        return erb.err_new(
                            format!("Record entry '{}' is mortar-only, cannot consume directly to JS", canonical),
                        );
                    },
                };
                let converted = erb.must_er(super::typecheck_convert(xx, loc, self.value_type.as_ref(), entry_cv),)?;
                let js_val = erb.must_er(value_to_js.to_js(xx, loc, converted))?;
                js_fields.push((JsCode {
                    loc,
                    type_: JsCodeType::Literal(serde_json::Value::String(canonical)),
                }, js_val));
            }
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::NewMap(JsCodeNewMap { fields: js_fields }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        });
    }
}

impl Aspect for TypeMortarMap { }

impl AspectMortarMap for TypeMortarMap {
    fn key_type(&self) -> Rc<dyn Type> {
        self.key_type.clone()
    }

    fn value_type(&self) -> Rc<dyn Type> {
        self.value_type.clone()
    }
}

impl AspectField for TypeMortarMap {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        if key_name == "new" {
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Box::new(StaticValue { type_: Rc::new(TypeMapNew {
                    key_type: self.key_type.clone(),
                    value_type: self.value_type.clone(),
                }) }),
            });
        }

        // Instance-level fields
        return er_seq_cv!(loc, erb, {
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on map", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::FieldUse(JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: key_name.clone(),
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Field(MortarJsValueDataAccess {
                        loc,
                        base: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(self.clone()),
                        }),
                        key: key_name,
                    }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "new" => Some(Rc::new(TypeMapNew {
                key_type: self.key_type.clone(),
                value_type: self.value_type.clone(),
            })),
            _ => Some(self.value_type.clone()),
        }
    }
}

impl AspectJsStackValue for TypeMortarMap {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(self.clone()),
            }),
        }
    }
}

impl AspectConsumeJs for TypeMortarMap {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for TypeMortarMap {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectSet for TypeMortarMap {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        super::mortar_js_value::mortar_set(xx, loc, self_, value)
    }
}

impl AspectIterable for TypeMortarMap {
    fn element_type(&self) -> Rc<dyn Type> {
        // Map iteration yields records with "key" and "value" fields
        Rc::new(super::record::Record {
            source: None,
            fields: [
                (
                    crate::langhelp::ConstKey::String("key".to_string()),
                    super::record::RecordField::Mortar(self.key_type.clone()),
                ),
                (
                    crate::langhelp::ConstKey::String("value".to_string()),
                    super::record::RecordField::Mortar(self.value_type.clone()),
                ),
            ]
                .into_iter()
                .collect(),
        })
    }

    fn foreach(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ScopeEntryId,
        body: CodeValue,
    ) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_consume = erb.must_er(aspect_consume_js_get(xx, loc, &*Rc::new(self.clone())))?;
            let base_js = erb.must_er(base_consume.to_js(xx, loc, base))?;
            let body_js = match body.code.cast::<JsCode>().into_res() {
                Ok(Some(js)) => js.clone(),
                Ok(None) => JsCode {
                    loc,
                    type_: JsCodeType::Literal(serde_json::Value::Null)
                },
                Err(e) => {
                    erb.errors.extend(e);
                    return Err(());
                },
            };
            let foreach_js = JsCode {
                loc,
                type_: JsCodeType::ForeachMap(JsCodeForeachMap {
                    base: Box::new(base_js),
                    element_key,
                    body: Box::new(body_js),
                }),
            };
            let null_type = Rc::new(super::mortar_null::TypeMortarNull);
            let stack_value = erb.must_er(aspect_js_stack_value_get(xx, loc, null_type.as_ref()))?;
            return Ok(stack_value.to_value(loc, foreach_js));
        });
    }
}

// --- map new function ---
#[derive(Clone)]
struct TypeMapNew {
    key_type: Rc<dyn Type>,
    value_type: Rc<dyn Type>,
}

impl Value for TypeMapNew {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}

impl Type for TypeMapNew {
    fn desc(&self) -> String {
        format!("map({}, {}).new", self.key_type.desc(), self.value_type.desc())
    }

    fn as_value(&self) -> Box<dyn Value> {
        Box::new(TypeMapNew { key_type: self.key_type.clone(), value_type: self.value_type.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map new".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to map new".to_string())
    }
}

impl Aspect for TypeMapNew { }

impl AspectFunction for TypeMapNew {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let instance_type = Rc::new(TypeMortarMap {
            key_type: self.key_type.clone(),
            value_type: self.value_type.clone(),
        });
        let arg_type = argument.value.type_(xx, loc);

        // Empty tuple → empty map
        if let Some(t) = aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            if t.element_types().is_empty() {
                return er_seq_cv!(loc, erb, {
                    let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, instance_type.as_ref()))?;
                    return Ok(ret_stack.to_value(loc, JsCode {
                        loc,
                        type_: JsCodeType::NewObject(JsCodeNewObject { fields: vec![] }),
                    }));
                });
            }
        }

        // Non-empty argument: convert to map type via typecheck_convert (handles records
        // via cast_from)
        super::typecheck_convert(xx, loc, instance_type.as_ref(), argument)
    }
}
