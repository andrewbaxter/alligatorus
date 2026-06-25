use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            deserialize_type_artifact,
            serialize_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicForeachMap,
            CodeDynamicNewMap,
            CodeDynamicNewObject,
            CodeDynamicSubscriptUse,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
                dynamic_set,
            },
            from_v8_ref,
            push::Push,
            to_v8,
            typecheck_convert,
            value::{
                record::{
                    Record,
                    RecordField,
                },
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_map::artifact::{
                    ArtifactMapType,
                    TypeMapArtifact,
                },
                type_null::dynamic::TypeNullDynamic,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicDataAccess,
                    ValueDynamicSource,
                },
                value_empty::r#const::ValueEmptyConst,
                value_number::r#const::ValueNumberConst,
                value_string::r#const::ValueStringConst,
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
            Value_,
            downcast_value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectFunction,
            AspectInitialBind,
            AspectIterable,
            AspectMapDynamic,
            AspectSet,
            AspectStackValueDynamic,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_consume_dynamic_get_maybe,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_iterable_aspect_id,
            aspect_map_dynamic_aspect_id,
            aspect_map_dynamic_get_maybe,
            aspect_record_get_maybe,
            aspect_set_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_stack_value_dynamic_get,
            aspect_string_const_get,
            aspect_tuple_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

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
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
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
        let key_type = match key.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
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
                value: Rc::new(ValueAnyConst { inner: new_type }).into(),
            });
        }
        return er_seq_cv!(loc, erb, {
            let const_key = ConstKey::String(key_name.clone());
            let field_type = match self.field_type_opt(xx, &const_key) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on map", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
            if key_name == "get" {
                return Ok(CodeValue {
                    code: Code {
                        loc,
                        code: Code_::Dyn(Rc::new(self_code)),
                    },
                    value: Rc::new(ValueEmptyConst { type_: Rc::new(TypeMapGetFnDynamic {
                        key_type: self.key_type.clone(),
                        value_type: self.value_type.clone(),
                    }) }).into(),
                });
            }
            let access_code = CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                base: Box::new(self_code),
                field: key_name.clone(),
            });
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: access_code
                    })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Field(ValueDynamicDataAccess { key: key_name, }),
                    type_: field_type,
                }).into(),
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
            "get" => Some(Rc::new(TypeFunctionDynamic {
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
        Rc::new(Record {
            base: None,
            fields: [
                (ConstKey::String("key".to_string()), RecordField::Dynamic(self.key_type.clone())),
                (ConstKey::String("value".to_string()), RecordField::Dynamic(self.value_type.clone())),
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
            let null_type = Rc::new(TypeNullDynamic);
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
        dynamic_set(xx, loc, self_, value)
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
            }).into(),
        }
    }
}

impl ExporterAny for TypeMapDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeMapDynamic {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        let body: ArtifactMapType = match serde_json::from_value(data) {
            Ok(v) => v,
            Err(e) => return EResult::err(loc, e.to_string()),
        };
        let key_res = deserialize_type_artifact(xx, loc, body.key_type);
        let mut errors = key_res.errors;
        let Some(key_type) = key_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let value_res = deserialize_type_artifact(xx, loc, body.value_type);
        if let Some(errors1) = value_res.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(value_type) = value_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(Rc::new(TypeMapDynamic {
                key_type,
                value_type,
            }) as Rc<dyn ExporterAny>),
        };
    }

    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = match self_.downcast::<TypeMapDynamic>() {
            Ok(v) => v,
            Err(_) => return EResult::err(loc, "TypeMapDynamic::self_to_serial: type mismatch".to_string()),
        };
        let key_val: &dyn Value_ = self_.key_type.as_ref();
        let key_art = serialize_artifact(xx, loc, key_val);
        let mut errors = key_art.errors;
        let Some(key_artifact) = key_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let value_val: &dyn Value_ = self_.value_type.as_ref();
        let value_art = serialize_artifact(xx, loc, value_val);
        if let Some(errors1) = value_art.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(value_artifact) = value_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let body = ArtifactMapType {
            key_type: key_artifact,
            value_type: value_artifact,
        };
        return EResult {
            errors,
            v: Some(Artifact::Body(make_body(serde_json::to_value(&body).unwrap()))),
        };
    }
}

impl Push for TypeMapDynamic {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let rc: Rc<TypeMapDynamic> = from_v8_ref::<Rc<TypeMapDynamic>>(&*value).clone();
        (*rc).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, Rc::new(self)).into()
    }
}

impl Type for TypeMapDynamic {
    fn as_value(&self) -> Rc<dyn Value_> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeMapDynamic {
            key_type: self.key_type.clone(),
            value_type: self.value_type.clone(),
        }) })
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
            let key_match =
                std::any::Any::type_id(source_map.key_type().as_ref() as &dyn std::any::Any) ==
                    std::any::Any::type_id(self.key_type.as_ref() as &dyn std::any::Any);
            if !key_match {
                return false;
            }
            return types_convertible(xx, loc, source_map.value_type().as_ref(), self.value_type.as_ref());
        }
        if aspect_record_get_maybe(xx, source_type).is_some() &&
            aspect_consume_dynamic_get_maybe(xx, source_type).is_some() {
            return true;
        }
        false
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if let Some(source_map) = aspect_map_dynamic_get_maybe(xx, source_type.as_ref()) {
            let key_match =
                std::any::Any::type_id(source_map.key_type().as_ref() as &dyn std::any::Any) ==
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
                }).into(),
            });
        }
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value.into_direct()))
                .ok()
                .expect("Record Rc not unique in map cast");
        return er_seq_cv!(loc, erb, {
            let value_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, self.value_type.as_ref()))?;
            let key_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, self.key_type.as_ref()))?;
            let mut js_fields = Vec::new();
            for (key, field) in record.fields.into_iter() {
                let display = key.to_str();
                let key_cv = match &key {
                    ConstKey::String(s) => CodeValue {
                        code: Code {
                            loc,
                            code: Code_::None
                        },
                        value: Rc::new(ValueStringConst(s.clone()),).into(),
                    },
                    ConstKey::Usize(n) => CodeValue {
                        code: Code {
                            loc,
                            code: Code_::None
                        },
                        value: Rc::new(ValueNumberConst(*n as f64),).into(),
                    },
                };
                let key_cv = erb.must_er(typecheck_convert(xx, loc, self.key_type.as_ref(), key_cv))?;
                let key_code = erb.must_er(key_to_dynamic.to_dynamic(xx, loc, key_cv))?;
                let entry_cv = match field {
                    RecordField::EvalRes(entry_res) => {
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
                    RecordField::Dynamic(_) => {
                        return erb.err_new(
                            format!("Record entry '{}' is dynamic-only, cannot consume directly to JS", display),
                        );
                    },
                };
                let converted = erb.must_er(typecheck_convert(xx, loc, self.value_type.as_ref(), entry_cv),)?;
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
                }).into(),
            });
        });
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map type".to_string())
    }

    fn desc(&self) -> String {
        format!("map({}, {})", self.key_type.desc(), self.value_type.desc())
    }
}

impl Value_ for TypeMapDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::MapDynamic))
    }
}

#[derive(Clone)]
pub(crate) struct TypeMapGetFnDynamic {
    key_type: Rc<dyn Type>,
    value_type: Rc<dyn Type>,
}

impl Aspect for TypeMapGetFnDynamic { }

impl AspectFunction for TypeMapGetFnDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let self_code = erb.must_er(function.code.cast::<CodeDynamic>())?.unwrap().clone();
            let argument = erb.must_er(typecheck_convert(xx, loc, self.key_type.as_ref(), argument))?;
            let key_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, self.key_type.as_ref()))?;
            let key_code = erb.must_er(key_to_dynamic.to_dynamic(xx, loc, argument))?;
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, self.value_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::SubscriptUse(CodeDynamicSubscriptUse {
                    base: Box::new(self_code),
                    key: Box::new(key_code),
                }),
            }));
        });
    }
}

impl ExporterAny for TypeMapGetFnDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl Type for TypeMapGetFnDynamic {
    fn as_value(&self) -> Rc<dyn Value_> {
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

impl Value_ for TypeMapGetFnDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::MapGetFnDynamic))
    }
}

#[derive(Clone)]
pub(crate) struct TypeMapNewDynamic {
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
        let arg_type = match argument.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
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
        typecheck_convert(xx, loc, instance_type.as_ref(), argument)
    }
}

impl ExporterAny for TypeMapNewDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl Type for TypeMapNewDynamic {
    fn as_value(&self) -> Rc<dyn Value_> {
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

impl Value_ for TypeMapNewDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::MapNewDynamic))
    }
}

fn types_convertible(xx: &ExprContext, loc: QExprId, source_type: &dyn Type, dest_type: &dyn Type) -> bool {
    source_type.can_cast_to(xx, loc, dest_type) || dest_type.can_cast_from(xx, loc, source_type)
}
