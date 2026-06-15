use crate::r#gen::ScopeEntry;
use {
    crate::{
        r#gen::{AspectBind, AspectBindDynamic, AspectConsumeDynamic, AspectField, AspectFunction, AspectInitialBind, AspectIterable, AspectMapDynamic, AspectSet, AspectStackValueDynamic, aspect_bind_aspect_id, aspect_bind_dynamic_aspect_id, aspect_consume_dynamic_aspect_id, aspect_consume_dynamic_get, aspect_consume_dynamic_get_maybe, aspect_field_aspect_id, aspect_function_aspect_id, aspect_initial_bind_aspect_id, aspect_iterable_aspect_id, aspect_map_dynamic_aspect_id, aspect_map_dynamic_get_maybe, aspect_record_get_maybe, aspect_set_aspect_id, aspect_stack_value_dynamic_aspect_id, aspect_stack_value_dynamic_get, aspect_string_const_get, aspect_tuple_get_maybe},
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicForeachMap,
            CodeDynamicNewMap,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            Erb,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicDataAccess,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
                mortar_set,
            },
            value::{
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_empty::r#const::ValueEmptyConst,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
            downcast_value,
        },
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

// --- map type (result of map(string, number), and also type of map instances) ---
#[derive(Clone)]
pub(crate) struct TypeMapDynamic {
    pub(crate) key_type: Rc<dyn Type>,
    pub(crate) value_type: Rc<dyn Type>,
}

impl Aspect for TypeMapDynamic { }

impl AspectBind for TypeMapDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeMapDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeMapDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectField for TypeMapDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        if key_name == "new" {
            let new_type: Rc<dyn Type> = Rc::new(TypeMapNewDynamic {
                key_type: self.key_type.clone(),
                value_type: self.value_type.clone(),
            });
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
                    inner: new_type,
                }),
            });
        }

        // Instance-level fields
        return er_seq_cv!(loc, erb, {
            let const_key = ConstKey::String(key_name.clone());
            let field_type = match self.field_type_opt(xx, &const_key) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on map", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
            if key_name == "get" {
                // `.get` returns a `TypeMapGetFnDynamic` carrying the map's
                // runtime code; calling it emits `<self>[<key>]` (the map's JS
                // form is a plain object — there's no real `.get` method to
                // invoke). Wrap in `ValueEmptyConst` so the CV's value-type
                // resolves to `TypeMapGetFnDynamic` (mirrors how `.new`
                // returns `ValueEmptyConst { type_: TypeMapNewDynamic }`).
                return Ok(CodeValue {
                    code: Code { loc, code: Code_::None },
                    value: Rc::new(ValueEmptyConst { type_: Rc::new(TypeMapGetFnDynamic {
                        self_code: Rc::new(self_code),
                        key_type: self.key_type.clone(),
                        value_type: self.value_type.clone(),
                    }) }),
                });
            }
            let access_code = CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                base: Box::new(self_code),
                field: key_name.clone(),
            });
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic { loc, type_: access_code })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Field(ValueDynamicDataAccess { key: key_name, }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(key) = key else {
            return None;
        };
        match key.as_str() {
            "new" => Some(Rc::new(TypeMapNewDynamic {
                key_type: self.key_type.clone(),
                value_type: self.value_type.clone(),
            })),
            "get" => Some(Rc::new(crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
                arg: self.key_type.clone(),
                ret: self.value_type.clone(),
            })),
            _ => Some(self.value_type.clone()),
        }
    }
}

impl AspectInitialBind for TypeMapDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(self.clone()),
        })
    }
}


impl AspectIterable for TypeMapDynamic {
    fn element_type(&self) -> Rc<dyn Type> {
        // Map iteration yields records with "key" and "value" fields
        Rc::new(crate::eval::value::record::Record {
            base: None,
            fields: [
                (
                    crate::langhelp::ConstKey::String("key".to_string()),
                    crate::eval::value::record::RecordField::Mortar(self.key_type.clone()),
                ),
                (
                    crate::langhelp::ConstKey::String("value".to_string()),
                    crate::eval::value::record::RecordField::Mortar(self.value_type.clone()),
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
            let base_consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, &*Rc::new(self.clone())))?;
            let base_js = erb.must_er(base_consume.to_dynamic(xx, loc, base))?;
            let body_js = match body.code.cast::<CodeDynamic>().into_res() {
                Ok(Some(js)) => js.clone(),
                Ok(None) => CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Literal(serde_json::Value::Null)
                },
                Err(e) => {
                    erb.errors.extend(e);
                    return Err(());
                },
            };
            let foreach_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::ForeachMap(CodeDynamicForeachMap {
                    base: Box::new(base_js),
                    element_key,
                    body: Box::new(body_js),
                }),
            };
            let null_type = Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic);
            let stack_value = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, null_type.as_ref()))?;
            return Ok(stack_value.to_value(loc, foreach_js));
        });
    }
}

impl AspectMapDynamic for TypeMapDynamic {
    fn key_type(&self) -> Rc<dyn Type> {
        self.key_type.clone()
    }

    fn value_type(&self) -> Rc<dyn Type> {
        self.value_type.clone()
    }
}

impl AspectSet for TypeMapDynamic {
    fn set(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
        mortar_set(xx, loc, self_, value)
    }
}

impl AspectStackValueDynamic for TypeMapDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(self.clone()),
            }),
        }
    }
}

impl crate::eval::dynamic_wrap::DynamicWrap for TypeMapDynamic {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let rc: Rc<TypeMapDynamic> = crate::eval::from_v8_ref::<Rc<TypeMapDynamic>>(&*value).clone();
        (*rc).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, Rc::new(self)).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeMapDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeMapDynamic {
                key_type: self.key_type.clone(),
                value_type: self.value_type.clone(),
            }),
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_set_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectSet>));
        }
        if *key == aspect_iterable_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectIterable>));
        }
        if *key == aspect_map_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectMapDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectInitialBind>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        if let Some(source_map) = aspect_map_dynamic_get_maybe(xx, source_type) {
            // Map → map: require identical key types. Differing key types
            // would need runtime re-encoding (e.g. `string` keys are stored
            // raw while `constkey` keys are canonical-JSON), and we don't
            // synthesize that. Identical-type check via `TypeId` —
            // `types_convertible` would admit one-way-cast pairs (string →
            // constkey works) which can't reuse the JS object verbatim.
            let key_match = std::any::Any::type_id(source_map.key_type().as_ref() as &dyn std::any::Any) ==
                std::any::Any::type_id(self.key_type.as_ref() as &dyn std::any::Any);
            if !key_match {
                return false;
            }
            return types_convertible(xx, loc, source_map.value_type().as_ref(), self.value_type.as_ref());
        }
        if aspect_record_get_maybe(xx, source_type).is_some() &&
            aspect_consume_dynamic_get_maybe(xx, source_type).is_some() {
            // Record → map: cast_from performs the real per-entry
            // `typecheck_convert` to the map's value type (which exercises
            // each value type's `cast_to`/`cast_from` chain — e.g.
            // `TypeSourceDataType::cast_to` wrapping an instance as a
            // `Rc<dyn Type>` when the map's value type is a type-iface
            // ref). No pre-check here — let the per-entry conversion report
            // the precise mismatch if one occurs.
            return true;
        }
        false
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if let Some(source_map) = aspect_map_dynamic_get_maybe(xx, source_type.as_ref()) {
            let key_match = std::any::Any::type_id(source_map.key_type().as_ref() as &dyn std::any::Any) ==
                std::any::Any::type_id(self.key_type.as_ref() as &dyn std::any::Any);
            if !key_match {
                return EvalRes::err(
                    loc,
                    format!(
                        "Incompatible map key types: {} cannot convert to {}",
                        source_map.key_type().desc(),
                        self.key_type.desc()
                    ),
                );
            }
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
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        }

        // Record source: decompose, typecheck_convert each value to the map's value type,
        // then convert each to JS individually.
        let record: crate::eval::value::record::Record =
            Rc::try_unwrap(downcast_value::<crate::eval::value::record::Record>(cv.value))
                .ok()
                .expect("Record Rc not unique in map cast");
        return er_seq_cv!(loc, erb, {
            let value_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, self.value_type.as_ref()))?;
            let key_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, self.key_type.as_ref()))?;
            let mut js_fields = Vec::new();
            for (key, field) in record.fields.into_iter() {
                // Build a CV from the const key and cast it through the map's
                // key type. `typecheck_convert` performs the actual encoding —
                // its result already carries the correctly-encoded runtime
                // form for `key_type`, so we use it verbatim. Record keys are
                // always const, so this is a per-entry compile-time
                // reconstruction (no record-dynamic-form passthrough).
                let display = key.to_str();
                let key_cv = match &key {
                    crate::langhelp::ConstKey::String(s) => CodeValue {
                        code: Code { loc, code: Code_::None },
                        value: Rc::new(
                            crate::eval::value::value_string::r#const::ValueStringConst(s.clone()),
                        ),
                    },
                    crate::langhelp::ConstKey::Usize(n) => CodeValue {
                        code: Code { loc, code: Code_::None },
                        value: Rc::new(
                            crate::eval::value::value_number::r#const::ValueNumberConst(*n as f64),
                        ),
                    },
                };
                let key_cv =
                    erb.must_er(crate::eval::typecheck_convert(xx, loc, self.key_type.as_ref(), key_cv))?;
                let key_code = erb.must_er(key_to_dynamic.to_dynamic(xx, loc, key_cv))?;
                let entry_cv = match field {
                    crate::eval::value::record::RecordField::EvalRes(entry_res) => {
                        if let Some(errs) = entry_res.errors {
                            erb.errors.extend(errs);
                        }
                        match entry_res.v {
                            Some(v) => v,
                            None => {
                                return erb.err_new(format!("Record entry '{}' failed to evaluate", display),);
                            },
                        }
                    },
                    crate::eval::value::record::RecordField::Mortar(_) => {
                        return erb.err_new(
                            format!("Record entry '{}' is mortar-only, cannot consume directly to JS", display),
                        );
                    },
                };
                let converted =
                    erb.must_er(crate::eval::typecheck_convert(xx, loc, self.value_type.as_ref(), entry_cv),)?;
                let js_val = erb.must_er(value_to_dynamic.to_dynamic(xx, loc, converted))?;
                js_fields.push((key_code, js_val));
            }
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::NewMap(CodeDynamicNewMap { fields: js_fields }),
                    })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        });
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        // No cast to `any` — maps aren't types. Misuse.
        false
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map type".to_string())
    }

    fn desc(&self) -> String {
        format!("map({}, {})", self.key_type.desc(), self.value_type.desc())
    }

    fn type_to_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
    ) -> Option<crate::langhelp::Artifact> {
        let key_artifact =
            crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                xx,
                loc,
                self.key_type.as_ref(),
            );
        let value_artifact =
            crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                xx,
                loc,
                self.value_type.as_ref(),
            );
        let payload = crate::eval::value::type_map::artifact::ArtifactMapType {
            key_type: key_artifact,
            value_type: value_artifact,
        };
        Some(crate::eval::value::type_map::artifact::TypeMapArtifact::build_artifact(
            serde_json::to_value(&payload).expect("Failed to serialize map type"),
        ))
    }
}

impl Value for TypeMapDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

// --- map new function ---
#[derive(Clone)]
struct TypeMapNewDynamic {
    key_type: Rc<dyn Type>,
    value_type: Rc<dyn Type>,
}

impl Aspect for TypeMapNewDynamic { }

impl AspectFunction for TypeMapNewDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let instance_type = Rc::new(TypeMapDynamic {
            key_type: self.key_type.clone(),
            value_type: self.value_type.clone(),
        });
        let arg_type = argument.value.type_(xx, loc);

        // Empty tuple → empty map
        if let Some(t) = aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            if t.element_types().is_empty() {
                return er_seq_cv!(loc, erb, {
                    let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, instance_type.as_ref()))?;
                    return Ok(ret_stack.to_value(loc, CodeDynamic {
                        loc,
                        type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: vec![] }),
                    }));
                });
            }
        }

        // Non-empty argument: convert to map type via typecheck_convert (handles records
        // via cast_from)
        crate::eval::typecheck_convert(xx, loc, instance_type.as_ref(), argument)
    }
}

impl Type for TypeMapNewDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeMapNewDynamic {
            key_type: self.key_type.clone(),
            value_type: self.value_type.clone(),
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to map new".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map new".to_string())
    }

    fn desc(&self) -> String {
        format!("map({}, {}).new", self.key_type.desc(), self.value_type.desc())
    }
}

impl Value for TypeMapNewDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

// --- map .get function ---
// `.get` on a map returns a value of this type. Calling it emits the JS
// subscript `<self>[<key>]` rather than a real `.get` method call — the map's
// runtime form is a plain object, so it has no actual `get` method.
#[derive(Clone)]
struct TypeMapGetFnDynamic {
    self_code: Rc<CodeDynamic>,
    key_type: Rc<dyn Type>,
    value_type: Rc<dyn Type>,
}

impl Aspect for TypeMapGetFnDynamic { }

impl AspectFunction for TypeMapGetFnDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let argument = erb.must_er(crate::eval::typecheck_convert(xx, loc, self.key_type.as_ref(), argument))?;
            let key_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, self.key_type.as_ref()))?;
            // `typecheck_convert` + `to_dynamic` give the encoded runtime form
            // for `key_type` — the same shape `new_map`'s cast_from emits when
            // it stores keys. Use it directly as the subscript.
            let key_code = erb.must_er(key_to_dynamic.to_dynamic(xx, loc, argument))?;
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, self.value_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::SubscriptUse(crate::code_dynamic::CodeDynamicSubscriptUse {
                    base: Box::new((*self.self_code).clone()),
                    key: Box::new(key_code),
                }),
            }));
        });
    }
}

impl Value for TypeMapGetFnDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeMapGetFnDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(self.clone())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to map get fn".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map get fn".to_string())
    }

    fn desc(&self) -> String {
        format!("map get fn")
    }
}

/// Check if `source_type` can be converted to `dest_type` by either direction of
/// cast.
fn types_convertible(xx: &ExprContext, loc: QExprId, source_type: &dyn Type, dest_type: &dyn Type) -> bool {
    source_type.can_cast_to(xx, loc, dest_type) || dest_type.can_cast_from(xx, loc, source_type)
}
