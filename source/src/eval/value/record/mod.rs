pub mod artifact;
pub mod scope_entry;

use crate::r#gen::ScopeEntry;
use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectRecord,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_bind_get,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get_maybe,
            aspect_field_aspect_id,
            aspect_field_get_maybe,
            aspect_initial_bind_aspect_id,
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
        eval::{
            Erb,
            TargetBindSub,
            dynamic_util::wrap_with_carry,
            value::{
                tuple::Tuple,
                type_any::r#const::TypeAnyConst,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicDataAccess,
                    ValueDynamicSource,
                },
            },
        },
        invariant::{
            must_downcast_box,
            must_downcast_ref,
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
            External,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
            downcast_value,
        },
    },
    scope_entry::{
        RecordScopeField,
        ScopeEntryRecordDynamic,
        ScopeEntryRecordMortar,
    },
    std::{
        any::Any,
        collections::BTreeMap,
        mem,
        rc::Rc,
    },
};

pub(crate) trait RecordBase {
    fn bind_sub(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        carry: Code,
        record_type: Rc<dyn Type>,
    ) -> EResult<TargetBindSub>;
    fn clone_base(&self) -> Box<dyn RecordBase>;
    fn field_access(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_code_raw: Code,
        key: String,
        entry_type: Rc<dyn Type>,
    ) -> CodeValue;
    fn to_dynamic(&self, carry: Code) -> EResult<CodeDynamic>;
}

enum BindSubResult {
    Const(CodeValue),
    Target(TargetBindSub),
}

pub(crate) struct Record {
    pub base: Option<Box<dyn RecordBase>>,
    pub fields: BTreeMap<ConstKey, RecordField>,
}

impl Record {
    fn bind_sub(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(BTreeMap<ConstKey, RecordScopeField>, Option<TargetBindSub>)> {
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value)).ok().expect("Record bind_sub: Rc not unique");
        let carry = cv.code;
        if let Some(base) = &record.base {
            let record_type = Rc::new(self.clone_as_type()) as Rc<dyn Type>;
            let scope_fields: BTreeMap<ConstKey, RecordScopeField> = self.fields.iter().map(|(k, f)| {
                let t = match f {
                    RecordField::Mortar(t) => t.clone(),
                    RecordField::EvalRes(_) => panic!("bind on non-type record"),
                };
                (k.clone(), RecordScopeField::Mortar(t))
            }).collect();
            return base.bind_sub(xx, loc, carry, record_type).map(|sub| (scope_fields, Some(sub)));
        }
        let mut errors = vec![];
        let mut bind_results: Vec<(ConstKey, BindSubResult)> = Vec::new();
        let mut has_target = false;
        for (key, field) in record.fields.into_iter() {
            match field {
                RecordField::Mortar(_) => {
                    errors.push(crate::langhelp::ExprErr {
                        loc: loc,
                        errors: vec![
                            crate::langhelp::ExprSubErr::Opaque(
                                format!("Record entry '{:?}' is mortar-only, cannot bind directly", key),
                            ),
                        ],
                    });
                },
                RecordField::EvalRes(elem_res) => {
                    if let Some(errs) = elem_res.errors {
                        errors.extend(errs);
                    }
                    if let Some(elem_cv) = elem_res.v {
                        let saved_value = elem_cv.value.clone();
                        match xx.target.maybe_bind_sub(xx, loc, elem_cv).into_res() {
                            Ok(Some(sub)) => {
                                has_target = true;
                                bind_results.push((key, BindSubResult::Target(sub)));
                            },
                            Ok(None) => {
                                bind_results.push((key, BindSubResult::Const(CodeValue {
                                    code: Code {
                                        loc: loc,
                                        code: Code_::None,
                                    },
                                    value: saved_value,
                                })));
                            },
                            Err(e) => errors.extend(e),
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
        if !has_target {
            let mut scope_fields = BTreeMap::new();
            for (key, result) in bind_results {
                match result {
                    BindSubResult::Const(cv) => {
                        let field_type = cv.value.type_(xx, loc);
                        let bind_res = match aspect_bind_get(xx, loc, field_type.as_ref()).into_res() {
                            Ok(fb) => fb.bind(xx, loc, cv),
                            Err(errs) => EResult {
                                v: None,
                                errors: Some(errs),
                            },
                        };
                        match bind_res.into_res() {
                            Ok((_, entry)) => {
                                scope_fields.insert(key, RecordScopeField::Scope(entry));
                            },
                            Err(errs) => {
                                scope_fields.insert(key, RecordScopeField::Error(errs));
                            },
                        }
                    },
                    BindSubResult::Target(_) => unreachable!(),
                }
            }
            return EResult::ok((scope_fields, None));
        }
        let mut target_fields = Vec::new();
        let mut scope_fields = BTreeMap::new();
        for (key, result) in bind_results {
            match result {
                BindSubResult::Target(sub) => {
                    scope_fields.insert(key.clone(), RecordScopeField::Mortar(sub.type_.clone()));
                    target_fields.push((key.clone(), sub));
                },
                BindSubResult::Const(cv) => {
                    let field_type = cv.value.type_(xx, loc);
                    let bind_res = match aspect_bind_get(xx, loc, field_type.as_ref()).into_res() {
                        Ok(fb) => fb.bind(xx, loc, cv),
                        Err(errs) => EResult {
                            v: None,
                            errors: Some(errs),
                        },
                    };
                    match bind_res.into_res() {
                        Ok((_, entry)) => {
                            scope_fields.insert(key, RecordScopeField::Scope(entry));
                        },
                        Err(errs) => {
                            scope_fields.insert(key, RecordScopeField::Error(errs));
                        },
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
        let combined = xx.target.bind_sub_record(loc, carry, target_fields);
        EResult::ok((scope_fields, Some(combined)))
    }

    fn clone_as_type(&self) -> Record {
        Record {
            base: self.base.as_ref().map(|b| b.clone_base()),
            fields: self.fields.iter().map(|(k, f)| {
                (k.clone(), match f {
                    RecordField::Mortar(t) => RecordField::Mortar(t.clone()),
                    RecordField::EvalRes(_) => panic!("clone_as_type on non-type record"),
                })
            }).collect(),
        }
    }
}

impl Aspect for Record { }

impl AspectBind for Record {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        match self.bind_sub(xx, loc, cv).into_res() {
            Err(errs) => EResult {
                v: None,
                errors: Some(errs),
            },
            Ok((scope_fields, None)) => {
                EResult::ok((Code {
                    loc: loc,
                    code: Code_::None,
                }, Box::new(ScopeEntryRecordMortar {
                    fields: scope_fields,
                    js_key: None,
                }) as Box<dyn ScopeEntry>))
            },
            Ok((scope_fields, Some(sub))) => {
                let (code, js_key) = xx.target.bind(xx, loc, sub);
                EResult::ok((code, Box::new(ScopeEntryRecordMortar {
                    fields: scope_fields,
                    js_key: Some(js_key),
                }) as Box<dyn ScopeEntry>))
            },
        }
    }
}

impl AspectBindDynamic for Record {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        self.bind_sub(xx, loc, cv).map(|(_scope_fields, sub)| sub)
    }
}

impl AspectConsumeDynamic for Record {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        let carry = cv.code;
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value)).ok().expect("Record Rc not unique in to_dynamic");
        if let Some(base) = &record.base {
            return base.to_dynamic(carry);
        }
        let mut errors = vec![];
        let mut js_fields = Vec::new();
        for (key, field) in record.fields.into_iter() {
            match field {
                RecordField::Mortar(_) => {
                    errors.push(crate::langhelp::ExprErr {
                        loc,
                        errors: vec![
                            crate::langhelp::ExprSubErr::Opaque(
                                format!("Record entry '{:?}' is mortar-only, cannot consume directly to JS", key),
                            ),
                        ],
                    });
                },
                RecordField::EvalRes(elem_res) => {
                    if let Some(errs) = elem_res.errors {
                        errors.extend(errs);
                    }
                    if let Some(elem_cv) = elem_res.v {
                        let elem_value_type = elem_cv.value.type_(xx, loc);
                        if let Some(to_dynamic) = aspect_consume_dynamic_get_maybe(xx, elem_value_type.as_ref()) {
                            match to_dynamic.to_dynamic(xx, loc, elem_cv).into_res() {
                                Ok(js) => js_fields.push((key.to_str(), js)),
                                Err(e) => errors.extend(e),
                            }
                        } else {
                            errors.push(crate::langhelp::ExprErr {
                                loc,
                                errors: vec![
                                    crate::langhelp::ExprSubErr::Opaque(
                                        format!(
                                            "Record entry '{:?}' type ({}) is missing aspect AspectConsumeDynamic",
                                            key,
                                            elem_value_type.desc()
                                        ),
                                    ),
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
                        crate::langhelp::ExprSubErr::Opaque(format!("Field '{}' not found on record", key_name)),
                    ],
                });
            })?);
            let CodeValue { code: self_code_raw, value: self_value } = self_;
            if let Some(base) = &self.base {
                return Ok(base.field_access(xx, loc, self_code_raw, const_key.to_str(), entry_type));
            }
            let mut record =
                Rc::try_unwrap(downcast_value::<Record>(self_value))
                    .ok()
                    .expect("Record Rc not unique in field access");
            match record.fields.remove(&const_key) {
                Some(RecordField::EvalRes(entry)) => {
                    if let Some(cv) = entry.v {
                        return Ok(cv);
                    }
                    return erb.err_new(format!("Field '{}' evaluated to error", key_name));
                },
                Some(RecordField::Mortar(_)) => {
                    panic!("baseless record has Mortar field");
                },
                None => {
                    return erb.err_new(format!("Field '{}' not found on record value", key_name));
                },
            }
        });
    }

    fn field_type_opt(&self, xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        self.fields.get(key).map(|f| record_field_type(xx, loc, f))
    }
}

impl AspectInitialBind for Record {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryRecordDynamic {
            dynamic_key: dynamic_key,
            fields: self.fields.iter().map(|(k, f)| {
                (k.clone(), match f {
                    RecordField::Mortar(t) => t.clone(),
                    RecordField::EvalRes(_) => panic!("initial_bind on non-type record"),
                })
            }).collect(),
        })
    }
}

impl Type for Record {
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
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectBind>));
        }
        if *key == aspect_record_aspect_id() {
            return Some(Box::new(Box::new(RecordAspectImpl) as Box<dyn AspectRecord>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            let entry_lifts: Vec<(ConstKey, Box<dyn AspectLiftDynamic>)> = self.fields.iter().map(|(k, f)| {
                let t = match f {
                    RecordField::Mortar(t) => t.clone(),
                    RecordField::EvalRes(_) => panic!("lift_js on non-type record"),
                };
                let a =
                    t
                        .aspect(_xx, &aspect_lift_dynamic_aspect_id())
                        .expect(&format!("Record entry '{:?}' type ({}) missing AspectLiftDynamic", k, t.desc()));
                (k.clone(), must_downcast_box::<Box<dyn AspectLiftDynamic>>(a))
            }).collect();
            return Some(Box::new(Box::new(RecordLiftDynamic { entry_lifts }) as Box<dyn AspectLiftDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone_as_type()) as Box<dyn AspectInitialBind>));
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        if aspect_record_get_maybe(xx, source_type).is_none() {
            return false;
        }
        let Some(source_field) = aspect_field_get_maybe(xx, source_type) else {
            return false;
        };
        self.fields.keys().all(|k| source_field.field_type_opt(xx, k).is_some())
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(_xx, &crate::r#gen::type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let code = cv.code;
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value)).ok().expect("Record cast_from: Rc not unique");
        return er_seq_cv!(loc, erb, {
            let mut converted_fields = BTreeMap::new();
            for (key, field) in record.fields {
                let dest_entry_type = match self.fields.get(&key) {
                    Some(RecordField::Mortar(t)) => t.clone(),
                    _ => panic!("cast_from: dest missing key {:?}", key),
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
                    base: record.base,
                    fields: converted_fields
                }),
            });
        });
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        let result_type = cv.value.type_(xx, loc);
        let record: Record =
            Rc::try_unwrap(downcast_value::<Record>(cv.value)).ok().expect("Record cast_to: Rc not unique");
        return er_seq_cv!(loc, erb, {
            let any_type: Rc<dyn Type> =
                Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic);
            let mut converted_fields = std::collections::BTreeMap::new();
            let mut converted_type = self.clone_as_type();
            for (key, field) in record.fields {
                let converted_cv = match field {
                    RecordField::Mortar(t) => {
                        // Compile-time type — store as External::Type, produce a BindingUse code
                        // that resolves to an `Rc<dyn Type>` external at runtime.
                        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                        xx.vx.externals.borrow_mut().push((id, External::Type(t)));
                        CodeValue {
                            code: Code {
                                loc,
                                code: Code_::Dyn(Rc::new(CodeDynamic {
                                    loc,
                                    type_: CodeDynamicType::BindingUse(
                                        crate::code_dynamic::CodeDynamicBindingUse { id },
                                    ),
                                })),
                            },
                            value: any_type.clone().as_value(),
                        }
                    },
                    RecordField::EvalRes(er) => {
                        let entry_cv = erb.must_er(er)?;
                        // Recursively cast the element value to a type. The conversion
                        // generates code that produces an `Rc<dyn Type>` external at runtime.
                        erb.must_er(crate::eval::typecheck_convert(xx, loc, any_type.as_ref(), entry_cv))?
                    },
                };
                converted_type.fields.insert(key.clone(), RecordField::Mortar(any_type.clone()));
                converted_fields.insert(key, RecordField::EvalRes(EvalRes::ok(converted_cv)));
            }
            let converted_record = Record {
                base: record.base,
                fields: converted_fields,
            };
            let converted_cv = CodeValue {
                code: cv.code,
                value: Rc::new(converted_record),
            };
            let record_js = erb.must_er(converted_type.to_dynamic(xx, loc, converted_cv))?;
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
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(call_code)),
                },
                value: any_type.as_value(),
            });
        });
    }

    fn desc(&self) -> String {
        format!("record({})", self.fields.iter().map(|(k, f)| {
            let t_desc = match f {
                RecordField::Mortar(t) => t.desc(),
                RecordField::EvalRes(_) => "?".to_string(),
            };
            format!("{:?}: {}", k, t_desc)
        }).collect::<Vec<_>>().join(", "))
    }

    fn type_to_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
    ) -> Option<crate::langhelp::Artifact> {
        let mut fields_serial: std::collections::BTreeMap<ConstKey, crate::langhelp::Artifact> =
            std::collections::BTreeMap::new();
        for (key, field) in &self.fields {
            let field_type = match field {
                RecordField::Mortar(t) => t.clone(),
                RecordField::EvalRes(er) => match &er.v {
                    Some(cv) => cv.value.type_(xx, loc),
                    None => return None,
                },
            };
            let field_artifact =
                crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                    xx,
                    loc,
                    field_type.as_ref(),
                );
            fields_serial.insert(key.clone(), field_artifact);
        }
        let payload = artifact::ArtifactRecordType { fields: fields_serial };
        Some(artifact::TypeRecordArtifact::build_artifact(
            serde_json::to_value(&payload).expect("Failed to serialize record type"),
        ))
    }
}

impl Value for Record {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let fields = self.fields.iter().map(|(k, f)| {
            let t = record_field_type(xx, loc, f);
            (k.clone(), RecordField::Mortar(t))
        }).collect();
        Rc::new(Record {
            base: self.base.as_ref().map(|b| b.clone_base()),
            fields: fields,
        })
    }
}

fn record_field_type(xx: &ExprContext, loc: QExprId, f: &RecordField) -> Rc<dyn Type> {
    match f {
        RecordField::Mortar(t) => t.clone(),
        RecordField::EvalRes(er) => match &er.v {
            Some(cv) => cv.value.type_(xx, loc),
            None => Rc::new(Tuple {
                base: None,
                elements: vec![],
            }) as Rc<dyn Type>,
        },
    }
}
struct RecordAspectImpl;

impl Aspect for RecordAspectImpl { }

impl AspectRecord for RecordAspectImpl { }
pub(crate) struct RecordBaseDynamic;

impl RecordBase for RecordBaseDynamic {
    fn bind_sub(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        carry: Code,
        record_type: Rc<dyn Type>,
    ) -> EResult<TargetBindSub> {
        xx.target.code_to_bind_sub(loc, carry, record_type)
    }

    fn clone_base(&self) -> Box<dyn RecordBase> {
        Box::new(RecordBaseDynamic)
    }

    fn field_access(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_code_raw: Code,
        key: String,
        entry_type: Rc<dyn Type>,
    ) -> CodeValue {
        let self_code = self_code_raw.cast::<CodeDynamic>().into_res().unwrap().unwrap().clone();
        let field_code = Code {
            loc: loc,
            code: Code_::Dyn(Rc::new(CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::FieldUse(crate::code_dynamic::CodeDynamicFieldUse {
                    base: Box::new(self_code),
                    field: key.clone(),
                }),
            })),
        };
        let field_source = ValueDynamicSource::Field(ValueDynamicDataAccess { key: key });
        if aspect_record_get_maybe(xx, entry_type.as_ref()).is_some() {
            let record_type = must_downcast_ref::<Record>(entry_type.as_ref());
            let fields = record_type.fields.iter().map(|(k, f)| {
                let t = match f {
                    RecordField::Mortar(t) => t.clone(),
                    RecordField::EvalRes(_) => {
                        panic!("field mortar type Record has non-Mortar field")
                    },
                };
                (k.clone(), RecordField::Mortar(t))
            }).collect();
            return CodeValue {
                code: field_code,
                value: Rc::new(Record {
                    base: Some(Box::new(RecordBaseDynamic)),
                    fields: fields,
                }),
            };
        }
        CodeValue {
            code: field_code,
            value: Rc::new(ValueDynamic {
                source: field_source,
                type_: entry_type,
            }),
        }
    }

    fn to_dynamic(&self, carry: Code) -> EResult<CodeDynamic> {
        carry.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

/// Represents a single field entry in a Record.
pub(crate) enum RecordField {
    /// Field has been fully evaluated and is available immediately.
    EvalRes(EvalRes),
    /// Field available only via the mortar source (a JS object). Type is known but
    /// value is deferred to runtime.
    Mortar(Rc<dyn Type>),
}

struct RecordLiftDynamic {
    entry_lifts: Vec<(ConstKey, Box<dyn AspectLiftDynamic>)>,
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
            let v8_key = v8::String::new(scope, &key.to_str()).unwrap();
            let v8_val = obj.get(scope, v8_key.into()).expect("Record field missing in JS object");
            let lifted = entry_lift.lift(xx, scope, v8_val)?;
            fields.insert(key.clone(), RecordField::EvalRes(EvalRes::ok(CodeValue {
                code: Code {
                    loc: (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)),
                    code: Code_::None,
                },
                value: lifted,
            })));
        }
        Ok(Rc::new(Record {
            base: None,
            fields: fields,
        }))
    }
}
