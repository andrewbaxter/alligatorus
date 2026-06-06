use {
    crate::{
        builtin::{
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectLiftDynamic,
            AspectRecord,
            aspect_bind_dynamic_aspect_id,
            aspect_bind_dynamic_get_maybe,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get_maybe,
            aspect_field_aspect_id,
            aspect_field_get_maybe,
            aspect_lift_dynamic_aspect_id,
            aspect_record_aspect_id,
            aspect_record_get_maybe,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::Erb,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            External,
            QExprId,
            ScopeEntry,
            ScopeEntryId,
            Type,
            Unique,
            Value,
            downcast_value,
        },
    },
    std::{
        any::Any,
        collections::BTreeMap,
        mem,
        rc::Rc,
    },
    super::value_tuple::wrap_with_carry,
};

/// Represents a single field entry in a Record.
pub(crate) enum RecordField {
    /// Field available only via the mortar source (a JS object). Type is known but
    /// value is deferred to runtime.
    Mortar(Rc<dyn Type>),
    /// Field has been fully evaluated and is available immediately.
    EvalRes(EvalRes),
}

pub(crate) struct Record {
    /// Optional mortar source (like ValueDynamic) — a JS object that fields can be
    /// accessed from at runtime.
    pub source: Option<super::value_dynamic::ValueDynamicSource>,
    /// Mapping from keys to field entries.
    pub fields: BTreeMap<ConstKey, RecordField>,
}

impl Record {
    /// Clone this type-Record. Panics if not a type.
    fn clone_as_type(&self) -> Record {
        Record {
            source: None,
            fields: self.fields.iter().map(|(k, f)| {
                (k.clone(), match f {
                    RecordField::Mortar(t) => RecordField::Mortar(t.clone()),
                    RecordField::EvalRes(_) => panic!("clone_as_type on non-type record"),
                })
            }).collect(),
        }
    }
}

fn record_field_type(xx: &ExprContext, loc: QExprId, f: &RecordField) -> Rc<dyn Type> {
    match f {
        RecordField::Mortar(t) => t.clone(),
        RecordField::EvalRes(er) => match &er.v {
            Some(cv) => cv.value.type_(xx, loc),
            None => Rc::new(super::value_tuple::Tuple {
                source: None,
                elements: vec![],
            }) as Rc<dyn Type>,
        },
    }
}

impl Value for Record {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let fields = self.fields.iter().map(|(k, f)| {
            let t = record_field_type(xx, loc, f);
            (k.clone(), RecordField::Mortar(t))
        }).collect();
        Rc::new(Record {
            source: None,
            fields,
        })
    }
}

impl Type for Record {
    fn desc(&self) -> String {
        format!("record({})", self.fields.iter().map(|(k, f)| {
            let t_desc = match f {
                RecordField::Mortar(t) => t.desc(),
                RecordField::EvalRes(_) => "?".to_string(),
            };
            format!("{:?}: {}", k, t_desc)
        }).collect::<Vec<_>>().join(", "))
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(self.clone_as_type())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_record_aspect_id() {
            return Some(Box::new(Box::new(RecordAspectImpl) as Box<dyn AspectRecord>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            let entry_lifts: Vec<(String, Box<dyn AspectLiftDynamic>)> = self.fields.iter().map(|(k, f)| {
                let key_str = match k {
                    ConstKey::String(s) => s.clone(),
                    ConstKey::Usize(n) => n.to_string(),
                };
                let t = match f {
                    RecordField::Mortar(t) => t.clone(),
                    RecordField::EvalRes(_) => panic!("lift_js on non-type record"),
                };
                let a =
                    t
                        .aspect(_xx, &aspect_lift_dynamic_aspect_id())
                        .expect(&format!("Record entry '{:?}' type ({}) missing AspectLiftDynamic", k, t.desc()));
                (key_str, *a.downcast::<Box<dyn AspectLiftDynamic>>().unwrap())
            }).collect();
            return Some(Box::new(Box::new(RecordLiftDynamic { entry_lifts }) as Box<dyn AspectLiftDynamic>));
        }
        return None;
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(_xx, &super::value_any::r#const::type_type_identity()).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        let result_type = cv.value.type_(xx, loc);
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value).expect("Record cast_to: expected Record value"))
                .ok()
                .expect("Record cast_to: Rc not unique");
        let needs_runtime = record.fields.values().any(|f| match f {
            RecordField::Mortar(_) => true,
            RecordField::EvalRes(er) => er.v.as_ref().map_or(false, |cv| !matches!(cv.code.code, Code_::None)),
        });
        if needs_runtime {
            // Has mortar or JS-coded fields: build a JS object with all fields having proper
            // JS code, then wrap with __record_to_type() to convert to Rc`<dyn Type>` at
            // runtime.
            let mut converted_fields = std::collections::BTreeMap::new();
            let mut converted_type = self.clone_as_type();
            for (key, field) in record.fields {
                let field_with_code = match field {
                    RecordField::Mortar(t) => RecordField::Mortar(t),
                    RecordField::EvalRes(er) => {
                        if let Some(entry_cv) = er.v {
                            if matches!(entry_cv.code.code, Code_::None) {
                                // Compile-time value without JS code: store its type as Rc`<dyn Type>` external
                                // and give it a code reference. Use TypeType as the consumer type so to_js passes
                                // through the BindingUse code.
                                let field_type = record_field_type(xx, loc, &RecordField::EvalRes(EvalRes::ok(CodeValue {
                                    code: entry_cv.code.clone(),
                                    value: entry_cv.value,
                                })));
                                let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                                xx.vx.externals.borrow_mut().push((id, External::Type(field_type)));
                                converted_type
                                    .fields
                                    .insert(
                                        key.clone(),
                                        RecordField::Mortar(
                                            Rc::new(super::value_any::r#const::TypeAnyConst) as Rc<dyn Type>,
                                        ),
                                    );
                                RecordField::EvalRes(EvalRes::ok(CodeValue {
                                    code: Code {
                                        loc,
                                        code: Code_::Dyn(Rc::new(CodeDynamic {
                                            loc,
                                            type_: CodeDynamicType::BindingUse(
                                                crate::code_dynamic::CodeDynamicBindingUse { id },
                                            ),
                                        })),
                                    },
                                    value: Rc::new(super::value_any::r#const::TypeAnyConst),
                                }))
                            } else {
                                RecordField::EvalRes(EvalRes::ok(entry_cv))
                            }
                        } else {
                            RecordField::EvalRes(er)
                        }
                    },
                };
                converted_fields.insert(key, field_with_code);
            }
            let converted_record = Record {
                source: record.source,
                fields: converted_fields,
            };
            let converted_cv = CodeValue {
                code: cv.code,
                value: Rc::new(converted_record),
            };
            match converted_type.to_js(xx, loc, converted_cv).into_res() {
                Ok(record_js) => {
                    let call_code = CodeDynamic {
                        loc,
                        type_: crate::code_dynamic::CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                            func: Box::new(CodeDynamic {
                                loc,
                                type_: CodeDynamicType::BindingUse(
                                    crate::code_dynamic::CodeDynamicBindingUse {
                                        id: crate::eval::RECORD_TO_TYPE_FN_SCOPE_ID,
                                    },
                                ),
                            }),
                            args: vec![record_js],
                        }),
                    };
                    EvalRes::ok(CodeValue {
                        code: Code {
                            loc,
                            code: Code_::Dyn(Rc::new(call_code)),
                        },
                        value: result_type.as_value(),
                    })
                },
                Err(errors) => EvalRes {
                    v: None,
                    errors: Some(errors),
                },
            }
        } else {
            // No mortar data: all compile-time, store type as external.
            let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
            xx.vx.externals.borrow_mut().push((id, External::Type(result_type.clone())));
            EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(crate::code_dynamic::CodeDynamicBindingUse { id }),
                    })),
                },
                value: result_type.as_value(),
            })
        }
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        if aspect_record_get_maybe(xx, source_type).is_none() {
            return false;
        }
        let Some(source_field) = aspect_field_get_maybe(xx, source_type) else {
            return false;
        };
        self.fields.keys().all(|k| source_field.field_opt(xx, &k.to_str()).is_some())
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let code = cv.code;
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value).expect("Record type must have Record value"))
                .ok()
                .expect("Record cast_from: Rc not unique");
        return er_seq_cv!(loc, erb, {
            let mut converted_fields = BTreeMap::new();
            for (key, field) in record.fields {
                let dest_entry_type = match self.fields.get(&key) {
                    Some(RecordField::Mortar(t)) => t.clone(),
                    _ => panic!("cast_from: dest missing key {:?}", key.to_str()),
                };
                let converted_field = match field {
                    RecordField::EvalRes(entry_res) => {
                        if let Some(entry_cv) = entry_res.v {
                            let conv =
                                erb.must_er(
                                    crate::eval::typecheck_convert(xx, loc, dest_entry_type.as_ref(), entry_cv),
                                )?;
                            RecordField::EvalRes(EvalRes::ok(conv))
                        } else {
                            RecordField::EvalRes(EvalRes {
                                v: None,
                                errors: entry_res.errors
                            })
                        }
                    },
                    RecordField::Mortar(_) => RecordField::Mortar(dest_entry_type),
                };
                converted_fields.insert(key, converted_field);
            }
            return Ok(CodeValue {
                code: code,
                value: Rc::new(Record {
                    source: record.source,
                    fields: converted_fields
                }),
            });
        });
    }
}
struct RecordAspectImpl;

impl Aspect for RecordAspectImpl { }

impl AspectRecord for RecordAspectImpl { }

impl Aspect for Record { }

impl AspectField for Record {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return crate::eval::util::er_seq_cv_inline(loc, |erb| {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let const_key = ConstKey::String(key_name.clone());
            let entry_type = record_field_type(xx, loc, self.fields.get(&const_key).ok_or_else(|| {
                erb.errors.push(crate::langhelp::ExprErr {
                    loc,
                    errors: vec![
                        crate::langhelp::ExprSubErr::Opaque(format!("Field '{}' not found on record", key_name),)
                    ],
                });
            })?);

            // If self_ is a Record, extract the field directly
            let CodeValue { code: self_code_raw, value: self_value } = self_;
            let is_none = matches!(self_code_raw.code, Code_::None);
            if let Some(mut record) = downcast_value::<Record>(self_value).and_then(|r| Rc::try_unwrap(r).ok()) {
                match record.fields.remove(&const_key) {
                    Some(RecordField::EvalRes(entry)) => {
                        if let Some(cv) = entry.v {
                            return Ok(cv);
                        }
                        return erb.err_new(format!("Field '{}' evaluated to error", key_name));
                    },
                    Some(RecordField::Mortar(mortar_type)) => {
                        // Field is only available via mortar source — generate JS access
                        let self_code = erb.must_er(self_code_raw.cast::<CodeDynamic>())?.unwrap().clone();
                        let field_code = Code {
                            loc,
                            code: Code_::Dyn(Rc::new(CodeDynamic {
                                loc,
                                type_: CodeDynamicType::FieldUse(crate::code_dynamic::CodeDynamicFieldUse {
                                    base: Box::new(self_code),
                                    field: key_name.clone(),
                                }),
                            })),
                        };
                        let field_source =
                            super::value_dynamic::ValueDynamicSource::Field(
                                super::value_dynamic::ValueDynamicDataAccess { key: key_name },
                            );
                        if aspect_record_get_maybe(xx, mortar_type.as_ref()).is_some() {
                            let record_type =
                                (mortar_type.as_ref() as &dyn Any)
                                    .downcast_ref::<Record>()
                                    .expect("record aspect present but type is not Record");
                            let fields = record_type.fields.iter().map(|(k, f)| {
                                let t = match f {
                                    RecordField::Mortar(t) => t.clone(),
                                    RecordField::EvalRes(_) => {
                                        panic!("field mortar type Record has non-Mortar field")
                                    },
                                };
                                (k.clone(), RecordField::Mortar(t))
                            }).collect();
                            return Ok(CodeValue {
                                code: field_code,
                                value: Rc::new(Record {
                                    source: Some(field_source),
                                    fields,
                                }),
                            });
                        }
                        return Ok(CodeValue {
                            code: field_code,
                            value: Rc::new(super::value_dynamic::ValueDynamic {
                                source: field_source,
                                type_: mortar_type,
                            }),
                        });
                    },
                    None => {
                        return erb.err_new(format!("Field '{}' not found on record value", key_name));
                    },
                }
            }

            // If self_ is a static value (no JS code), return a static value for the field
            if is_none {
                return Ok(CodeValue {
                    code: Code {
                        loc,
                        code: Code_::None,
                    },
                    value: Rc::new(super::value_tuple::ValueStatic { type_: entry_type }),
                });
            }
            let self_code = erb.must_er(self_code_raw.cast::<CodeDynamic>())?.unwrap().clone();
            let field_code = Code {
                loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::FieldUse(crate::code_dynamic::CodeDynamicFieldUse {
                        base: Box::new(self_code),
                        field: key_name.clone(),
                    }),
                })),
            };
            let field_source =
                super::value_dynamic::ValueDynamicSource::Field(
                    super::value_dynamic::ValueDynamicDataAccess { key: key_name },
                );
            if aspect_record_get_maybe(xx, entry_type.as_ref()).is_some() {
                let record_type =
                    (entry_type.as_ref() as &dyn Any)
                        .downcast_ref::<Record>()
                        .expect("record aspect present but type is not Record");
                let fields = record_type.fields.iter().map(|(k, f)| {
                    let t = match f {
                        RecordField::Mortar(t) => t.clone(),
                        RecordField::EvalRes(_) => {
                            panic!("field mortar type Record has non-Mortar field")
                        },
                    };
                    (k.clone(), RecordField::Mortar(t))
                }).collect();
                return Ok(CodeValue {
                    code: field_code,
                    value: Rc::new(Record {
                        source: Some(field_source),
                        fields,
                    }),
                });
            }
            return Ok(CodeValue {
                code: field_code,
                value: Rc::new(super::value_dynamic::ValueDynamic {
                    source: field_source,
                    type_: entry_type,
                }),
            });
        });
    }

    fn field_opt(&self, xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        let const_key = ConstKey::from_str(key).ok()?;
        self.fields.get(&const_key).map(|f| record_field_type(xx, loc, f))
    }
}

impl AspectConsumeDynamic for Record {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        let carry = cv.code;

        // If the value is a ValueDynamic (e.g. from cast_from), the JS code already
        // represents the record object — just pass the code through
        let record: Record = match downcast_value::<Record>(cv.value).and_then(|r| Rc::try_unwrap(r).ok()) {
            Some(r) => r,
            None => {
                return carry.cast::<CodeDynamic>().map(|x| x.unwrap().clone());
            },
        };

        // If the record has a mortar source (it's backed by a JS object), pass through
        // the code — the fields are accessed at runtime.
        if record.source.is_some() {
            return carry.cast::<CodeDynamic>().map(|x| x.unwrap().clone());
        }
        let mut errors = vec![];
        let mut js_fields = Vec::new();
        for (key, field) in record.fields.into_iter() {
            let key_str = match &key {
                ConstKey::String(s) => s.clone(),
                ConstKey::Usize(n) => n.to_string(),
            };
            match field {
                RecordField::Mortar(_) => {
                    // Mortar fields are only available via the source object — they shouldn't appear
                    // in a record being directly consumed to JS (literal records have all EvalRes
                    // fields)
                    errors.push(crate::langhelp::ExprErr {
                        loc,
                        errors: vec![
                            crate::langhelp::ExprSubErr::Opaque(
                                format!("Record entry '{}' is mortar-only, cannot consume directly to JS", key_str),
                            )
                        ],
                    });
                },
                RecordField::EvalRes(elem_res) => {
                    if let Some(errs) = elem_res.errors {
                        errors.extend(errs);
                    }
                    if let Some(elem_cv) = elem_res.v {
                        let elem_value_type = elem_cv.value.type_(xx, loc);
                        if let Some(to_js) = aspect_consume_dynamic_get_maybe(xx, elem_value_type.as_ref()) {
                            match to_js.to_js(xx, loc, elem_cv).into_res() {
                                Ok(js) => js_fields.push((key_str, js)),
                                Err(e) => errors.extend(e),
                            }
                        } else if let Some(bind) = aspect_bind_dynamic_get_maybe(xx, elem_value_type.as_ref()) {
                            match bind.bind_js(xx, loc, elem_cv).into_res() {
                                Ok((Some(js), _)) => js_fields.push((key_str, js)),
                                Ok((None, _)) => { },
                                Err(e) => errors.extend(e),
                            }
                        } else {
                            errors.push(crate::langhelp::ExprErr {
                                loc,
                                errors: vec![
                                    crate::langhelp::ExprSubErr::Opaque(
                                        format!(
                                            "Record entry '{}' type ({}) cannot be converted to JS",
                                            key_str,
                                            elem_value_type.desc()
                                        ),
                                    )
                                ],
                            });
                        }
                    }
                },
            }
        }
        if !errors.is_empty() {
            return EResult {
                v: None,
                errors: Some(errors),
            };
        }
        EResult::ok(wrap_with_carry(loc, carry, CodeDynamic {
            loc,
            type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: js_fields }),
        }))
    }
}

impl AspectBindDynamic for Record {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn super::value_scope_entry::dynamic::DynamicScopeEntry>)> {
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value).unwrap())
                .ok()
                .expect("Record bind_js: Rc not unique");

        // If the record has no source and no JS code (pure compile-time value, e.g. from
        // a lifted record), bind as a value without producing JS.
        let is_sourceless =
            matches!(cv.code.code, Code_::None) && record.source.is_none() &&
                record.fields.values().all(|f| match f {
                    RecordField::EvalRes(er) => {
                        er.v.as_ref().map_or(true, |cv| matches!(cv.code.code, Code_::None))
                    },
                    RecordField::Mortar(_) => false,
                });
        if is_sourceless {
            let mut scope_fields = BTreeMap::new();
            for (key, field) in record.fields {
                match field {
                    RecordField::EvalRes(er) => {
                        if let Some(errs) = &er.errors {
                            scope_fields.insert(key, RecordScopeField::Error(errs.clone()));
                        } else if let Some(field_cv) = er.v {
                            scope_fields.insert(
                                key,
                                RecordScopeField::Scope(
                                    Box::new(
                                        super::value_tuple::ScopeEntryValue { value: Rc::from(field_cv.value) },
                                    ),
                                ),
                            );
                        }
                    },
                    RecordField::Mortar(_) => unreachable!(),
                }
            }
            return EResult::ok((None, Box::new(ScopeEntryRecordMortar { fields: scope_fields })));
        }
        let scope_fields: BTreeMap<ConstKey, RecordScopeField> = self.fields.iter().map(|(k, f)| {
            let t = match f {
                RecordField::Mortar(t) => t.clone(),
                RecordField::EvalRes(_) => panic!("bind_js on non-type record"),
            };
            (k.clone(), RecordScopeField::Mortar(t))
        }).collect();
        let cv = CodeValue {
            code: cv.code,
            value: Rc::new(record),
        };
        self.to_js(xx, loc, cv).map(|c| {
            (
                Some(c),
                Box::new(ScopeEntryRecordMortar { fields: scope_fields }) as
                    Box<dyn super::value_scope_entry::dynamic::DynamicScopeEntry>,
            )
        })
    }
}

struct RecordLiftDynamic {
    entry_lifts: Vec<(String, Box<dyn AspectLiftDynamic>)>,
}

impl Aspect for RecordLiftDynamic { }

impl AspectLiftDynamic for RecordLiftDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let obj = v8::Local::<v8::Object>::try_from(value).expect("Expected JS object for record lift");
        let mut fields = BTreeMap::new();
        for (key, entry_lift) in &self.entry_lifts {
            let v8_key = v8::String::new(scope, key).unwrap();
            let v8_val = obj.get(scope, v8_key.into()).expect("Record field missing in JS object");
            let lifted = entry_lift.lift(xx, scope, v8_val)?;
            let const_key = ConstKey::String(key.clone());
            fields.insert(const_key, RecordField::EvalRes(EvalRes::ok(CodeValue {
                code: Code {
                    loc: (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)),
                    code: Code_::None,
                },
                value: lifted,
            })));
        }
        Ok(Rc::new(Record {
            source: None,
            fields,
        }))
    }
}
pub mod scope_entry;

pub(crate) use scope_entry::{
    RecordScopeField,
    ScopeEntryRecordMortar,
};
