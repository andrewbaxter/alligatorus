use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectLiftJs,
            AspectTuple,
            aspect_bind_js_aspect_id,
            aspect_bind_js_get_maybe,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_consume_js_get_maybe,
            aspect_field_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_string_get,
            aspect_tuple_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeBlock,
            JsCodeBlockEntry,
            JsCodeNewArray,
            JsCodeNewObject,
            JsCodeType,
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
            ScopeEntry,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        collections::BTreeMap,
        rc::Rc,
    },
    super::mortar_js_value::bind_js_some,
};

pub(crate) struct ValueTuple {
    pub elements: Vec<EvalRes>,
}

impl Value for ValueTuple {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let types: Vec<Rc<dyn Type>> = self.elements.iter().map(|e| {
            match &e.v {
                Some(cv) => cv.value.type_(xx, loc),
                None => Rc::new(TypeTuple { types: vec![] }) as Rc<dyn Type>,
            }
        }).collect();
        Rc::new(TypeTuple { types })
    }
}

pub(crate) struct TypeTuple {
    pub types: Vec<Rc<dyn Type>>,
}

impl Value for TypeTuple {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeTuple {
    fn desc(&self) -> String {
        format!("tuple({})", self.types.iter().map(|t| t.desc()).collect::<Vec<_>>().join(", "))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_tuple_aspect_id() {
            return Some(Box::new(Box::new(TypeTuple { types: self.types.clone() }) as Box<dyn AspectTuple>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeTuple { types: self.types.clone() }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeTuple { types: self.types.clone() }) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_lift_js_aspect_id() {
            let element_lifts: Vec<Box<dyn AspectLiftJs>> = self.types.iter().map(|t| {
                let a =
                    t
                        .aspect(_xx, &aspect_lift_js_aspect_id())
                        .expect(&format!("Tuple element type ({}) missing AspectLiftJs", t.desc()));
                *a.downcast::<Box<dyn AspectLiftJs>>().unwrap()
            }).collect();
            return Some(Box::new(Box::new(TupleLiftJs { element_lifts }) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert tuple to other type"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to tuple type"))
    }
}

impl Aspect for TypeTuple { }

impl AspectTuple for TypeTuple {
    fn element_types(&self) -> Vec<Rc<dyn Type>> {
        self.types.clone()
    }

    fn take_elements(&self, value: Box<dyn Value>) -> Vec<EvalRes> {
        let tuple: Box<ValueTuple> = (value as Box<dyn Any>).downcast().unwrap();
        tuple.elements
    }
}

struct TupleLiftJs {
    element_lifts: Vec<Box<dyn AspectLiftJs>>,
}

impl Aspect for TupleLiftJs { }

impl AspectLiftJs for TupleLiftJs {
    fn lift(&self, scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let array = v8::Local::<v8::Array>::try_from(value).expect("Expected JS array for tuple lift");
        let mut elements = Vec::new();
        for (i, elem_lift) in self.element_lifts.iter().enumerate() {
            let elem_v8 = array.get_index(scope, i as u32).expect("Array index out of bounds in tuple lift");
            let lifted = elem_lift.lift(scope, elem_v8)?;
            elements.push(EvalRes::ok(CodeValue {
                code: Code {
                    loc: (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)),
                    code: Code_::None,
                },
                value: lifted,
            }));
        }
        Ok(Box::new(ValueTuple { elements }))
    }
}

/// When a compound value (Tuple/Record) is the last expression in a block, the
/// block's code contains binding declarations but the compound value has
/// Code_::None. When consumed to JS, the generated JS references those bindings.
/// This helper wraps the consumed JS with the carry code so the bindings are in
/// scope.
fn wrap_with_carry(loc: QExprId, carry: Code, js: JsCode) -> JsCode {
    if let Some(Some(carry_js)) = carry.cast::<JsCode>().v {
        let mut entries = match &carry_js.type_ {
            JsCodeType::Block(block) => block.entries.clone(),
            _ => vec![JsCodeBlockEntry::Code(carry_js.clone())],
        };
        entries.push(JsCodeBlockEntry::Code(js));
        return JsCode {
            loc,
            type_: JsCodeType::Block(JsCodeBlock { entries }),
        };
    }
    js
}

impl AspectConsumeJs for TypeTuple {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        let carry = cv.code;
        let tuple: Box<ValueTuple> = (cv.value as Box<dyn Any>).downcast().unwrap();
        let mut errors = vec![];
        let mut js_elements = Vec::new();
        for (elem_res, elem_type) in tuple.elements.into_iter().zip(self.types.iter()) {
            if let Some(errs) = elem_res.errors {
                errors.extend(errs);
            }
            if let Some(elem_cv) = elem_res.v {
                match aspect_consume_js_get(xx, loc, elem_type.as_ref()).into_res() {
                    Ok(to_js) => match to_js.to_js(xx, loc, elem_cv).into_res() {
                        Ok(js) => js_elements.push(js),
                        Err(e) => errors.extend(e),
                    },
                    Err(e) => errors.extend(e),
                }
            }
        }
        if !errors.is_empty() {
            return EResult {
                v: None,
                errors: Some(errors),
            };
        }
        EResult::ok(wrap_with_carry(loc, carry, JsCode {
            loc,
            type_: JsCodeType::NewArray(JsCodeNewArray { elements: js_elements }),
        }))
    }
}

impl AspectBindJs for TypeTuple {
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

/// Represents a single field entry in a RecordValue.
pub(crate) enum RecordField {
    /// Field available only via the mortar source (a JS object). Type is known but
    /// value is deferred to runtime.
    Mortar(Rc<dyn Type>),
    /// Field has been fully evaluated and is available immediately.
    EvalRes(EvalRes),
}

pub(crate) struct RecordValue {
    /// Optional mortar source (like MortarJsValue) — a JS object that fields can be
    /// accessed from at runtime.
    pub source: Option<super::mortar_js_value::MortarJsValueSource>,
    /// Mapping from keys to field entries.
    pub fields: BTreeMap<ConstKey, RecordField>,
}

impl Value for RecordValue {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let entry_types: BTreeMap<ConstKey, Rc<dyn Type>> = self.fields.iter().map(|(k, f)| {
            let t = match f {
                RecordField::Mortar(t) => t.clone(),
                RecordField::EvalRes(er) => match &er.v {
                    Some(cv) => cv.value.type_(xx, loc),
                    None => Rc::new(TypeTuple { types: vec![] }) as Rc<dyn Type>,
                },
            };
            (k.clone(), t)
        }).collect();
        Rc::new(RecordType { entry_types })
    }
}

pub(crate) struct RecordType {
    pub entry_types: BTreeMap<ConstKey, Rc<dyn Type>>,
}

impl Value for RecordType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for RecordType {
    fn desc(&self) -> String {
        format!(
            "record({})",
            self.entry_types.iter().map(|(k, t)| format!("{:?}: {}", k, t.desc())).collect::<Vec<_>>().join(", ")
        )
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(
                Box::new(Box::new(RecordType { entry_types: self.entry_types.clone() }) as Box<dyn AspectField>),
            );
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(
                Box::new(
                    Box::new(RecordType { entry_types: self.entry_types.clone() }) as Box<dyn AspectConsumeJs>,
                ),
            );
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(
                Box::new(Box::new(RecordType { entry_types: self.entry_types.clone() }) as Box<dyn AspectBindJs>),
            );
        }
        if *key == aspect_lift_js_aspect_id() {
            let entry_lifts: Vec<(String, Box<dyn AspectLiftJs>)> = self.entry_types.iter().map(|(k, t)| {
                let key_str = match k {
                    ConstKey::String(s) => s.clone(),
                };
                let a =
                    t
                        .aspect(_xx, &aspect_lift_js_aspect_id())
                        .expect(&format!("Record entry '{:?}' type ({}) missing AspectLiftJs", k, t.desc()));
                (key_str, *a.downcast::<Box<dyn AspectLiftJs>>().unwrap())
            }).collect();
            return Some(Box::new(Box::new(RecordLiftJs { entry_lifts }) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert record to other type"))
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        let source_any: &dyn std::any::Any = source_type.as_ref();

        // Check compatibility: source must be a record type with matching keys
        let source_keys_match = if let Some(source_record) = source_any.downcast_ref::<RecordType>() {
            self.entry_types.keys().all(|k| source_record.entry_types.contains_key(k))
        } else {
            false
        };
        if source_keys_match {
            // Only allow pass-through for non-RecordValue values (MortarJsValue, etc.)
            // RecordValue values should go through typecheck_convert_record for proper entry
            // conversion
            let value_any: &dyn std::any::Any = cv.value.as_ref();
            if value_any.downcast_ref::<RecordValue>().is_none() {
                return EvalRes::ok(CodeValue {
                    code: cv.code.clone(),
                    value: Box::new(super::mortar_js_value::MortarJsValue {
                        source: super::mortar_js_value::MortarJsValueSource::Expr,
                        type_: Rc::new(RecordType { entry_types: self.entry_types.clone() }),
                    }),
                });
            }
        }
        EvalRes::err(loc, format!("Cannot convert to record type"))
    }
}

impl Aspect for RecordType { }

impl AspectField for RecordType {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return super::util::er_seq_cv_inline(loc, |erb| {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let const_key = ConstKey::String(key_name.clone());
            let entry_type = self.entry_types.get(&const_key).cloned().ok_or_else(|| {
                erb.errors.push(crate::langhelp::ExprErr {
                    loc,
                    errors: vec![
                        crate::langhelp::ExprSubErr::Opaque(format!("Field '{}' not found on record", key_name))
                    ],
                });
            })?;

            // If self_ is a RecordValue, extract the field directly
            let CodeValue { code: self_code_raw, value: self_value } = self_;
            let is_none = matches!(self_code_raw.code, Code_::None);
            let any_value: Box<dyn Any> = self_value;
            if let Ok(mut record) = any_value.downcast::<RecordValue>() {
                match record.fields.remove(&const_key) {
                    Some(RecordField::EvalRes(entry)) => {
                        if let Some(cv) = entry.v {
                            return Ok(cv);
                        }
                        return erb.err_new(format!("Field '{}' evaluated to error", key_name));
                    },
                    Some(RecordField::Mortar(mortar_type)) => {
                        // Field is only available via mortar source — generate JS access
                        let source = record.source.take();
                        let self_code = erb.must_er(self_code_raw.cast::<JsCode>())?.unwrap().clone();
                        return Ok(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::Dyn(Rc::new(JsCode {
                                    loc,
                                    type_: JsCodeType::FieldUse(crate::jscode::JsCodeFieldUse {
                                        base: Box::new(self_code),
                                        field: key_name.clone(),
                                    }),
                                })),
                            },
                            value: Box::new(super::mortar_js_value::MortarJsValue {
                                source: super::mortar_js_value::MortarJsValueSource::Field(
                                    super::mortar_js_value::MortarJsValueDataAccess {
                                        loc,
                                        base: Box::new(super::mortar_js_value::MortarJsValue {
                                            source: source.unwrap_or(
                                                super::mortar_js_value::MortarJsValueSource::Expr,
                                            ),
                                            type_: Rc::new(RecordType { entry_types: self.entry_types.clone() }),
                                        }),
                                        key: key_name,
                                    },
                                ),
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
                    value: Box::new(StaticValue { type_: entry_type }),
                });
            }
            let self_code = erb.must_er(self_code_raw.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::FieldUse(crate::jscode::JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: key_name.clone(),
                        }),
                    })),
                },
                value: Box::new(super::mortar_js_value::MortarJsValue {
                    source: super::mortar_js_value::MortarJsValueSource::Field(
                        super::mortar_js_value::MortarJsValueDataAccess {
                            loc,
                            base: Box::new(super::mortar_js_value::MortarJsValue {
                                source: super::mortar_js_value::MortarJsValueSource::Expr,
                                type_: Rc::new(RecordType { entry_types: self.entry_types.clone() }),
                            }),
                            key: key_name,
                        },
                    ),
                    type_: entry_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        self.entry_types.get(&ConstKey::String(key.to_string())).cloned()
    }
}

impl AspectConsumeJs for RecordType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        let carry = cv.code;

        // If the value is a MortarJsValue (e.g. from cast_from), the JS code already
        // represents the record object — just pass the code through
        let record: Box<RecordValue> = match (cv.value as Box<dyn Any>).downcast::<RecordValue>() {
            Ok(r) => r,
            Err(_) => {
                return carry.cast::<JsCode>().map(|x| x.unwrap().clone());
            },
        };
        let mut errors = vec![];
        let mut js_fields = Vec::new();
        for (key, field) in record.fields.into_iter() {
            let key_str = match &key {
                ConstKey::String(s) => s.clone(),
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
                        if let Some(to_js) = aspect_consume_js_get_maybe(xx, elem_value_type.as_ref()) {
                            match to_js.to_js(xx, loc, elem_cv).into_res() {
                                Ok(js) => js_fields.push((key_str, js)),
                                Err(e) => errors.extend(e),
                            }
                        } else if let Some(bind) = aspect_bind_js_get_maybe(xx, elem_value_type.as_ref()) {
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
        EResult::ok(wrap_with_carry(loc, carry, JsCode {
            loc,
            type_: JsCodeType::NewObject(JsCodeNewObject { fields: js_fields }),
        }))
    }
}

impl AspectBindJs for RecordType {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        // If the record has no source and no JS code (pure compile-time value, e.g. from
        // a lifted record), bind as a value without producing JS.
        let is_no_code = matches!(cv.code.code, Code_::None);
        let any_ref: &dyn Any = cv.value.as_ref();
        let is_sourceless_record = if let Some(record) = any_ref.downcast_ref::<RecordValue>() {
            record.source.is_none() && record.fields.values().all(|f| match f {
                RecordField::EvalRes(er) => {
                    er.v.as_ref().map_or(true, |cv| matches!(cv.code.code, Code_::None))
                },
                RecordField::Mortar(_) => false,
            })
        } else {
            false
        };
        if is_no_code && is_sourceless_record {
            let record: Box<RecordValue> = (cv.value as Box<dyn Any>).downcast().unwrap();
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
                                    Box::new(ValueScopeEntry { value: Rc::from(field_cv.value) }),
                                ),
                            );
                        }
                    },
                    RecordField::Mortar(_) => unreachable!(),
                }
            }
            return EResult::ok((None, Box::new(RecordScopeEntryAsMortar { fields: scope_fields })));
        }
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

/// A value whose data is entirely carried by its type.
pub(crate) struct StaticValue {
    pub type_: Rc<dyn Type>,
}

impl Value for StaticValue {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        self.type_.clone()
    }
}

/// A scope entry for non-JS values. Returns Code_::None and reconstructs the value
/// from its type.
pub(crate) struct StaticScopeEntry {
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for StaticScopeEntry {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Box::new(StaticValue { type_: self.type_.clone() }),
        }
    }
}

/// A scope entry that preserves the actual value (for compile-time values without
/// JS code).
pub(crate) struct ValueScopeEntry {
    pub value: Rc<dyn Value>,
}

impl ScopeEntry for ValueScopeEntry {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Box::new(SharedValue { inner: self.value.clone() }),
        }
    }
}

/// Value wrapper for an Rc`<dyn Value>`, allowing shared ownership of a value.
pub(crate) struct SharedValue {
    pub inner: Rc<dyn Value>,
}

impl Value for SharedValue {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        self.inner.type_(xx, loc)
    }
}

struct RecordLiftJs {
    entry_lifts: Vec<(String, Box<dyn AspectLiftJs>)>,
}

impl Aspect for RecordLiftJs { }

impl AspectLiftJs for RecordLiftJs {
    fn lift(&self, scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let obj = v8::Local::<v8::Object>::try_from(value).expect("Expected JS object for record lift");
        let mut fields = BTreeMap::new();
        for (key, entry_lift) in &self.entry_lifts {
            let v8_key = v8::String::new(scope, key).unwrap();
            let v8_val = obj.get(scope, v8_key.into()).expect("Record field missing in JS object");
            let lifted = entry_lift.lift(scope, v8_val)?;
            let const_key = ConstKey::String(key.clone());
            fields.insert(const_key, RecordField::EvalRes(EvalRes::ok(CodeValue {
                code: Code {
                    loc: (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)),
                    code: Code_::None,
                },
                value: lifted,
            })));
        }
        Ok(Box::new(RecordValue {
            source: None,
            fields,
        }))
    }
}

/// Scope entry for a bound record. Maps each key to one of: a MortarScopeEntry
/// (for fields that were mortar-only), a ScopeEntry (for fields that were fully
/// evaluated), or an error.
pub(crate) enum RecordScopeField {
    Mortar(Box<dyn super::mortar_scope_entry::MortarScopeEntry>),
    Scope(Box<dyn ScopeEntry>),
    Error(Vec<crate::langhelp::ExprErr>),
}

pub(crate) struct RecordScopeEntry {
    pub fields: BTreeMap<ConstKey, RecordScopeField>,
}

/// MortarScopeEntry adapter for sourceless records (used by bind_js when record
/// has no JS code). On use_, constructs a RecordValue from the stored scope
/// entries.
struct RecordScopeEntryAsMortar {
    fields: BTreeMap<ConstKey, RecordScopeField>,
}

impl super::mortar_scope_entry::MortarScopeEntry for RecordScopeEntryAsMortar {
    fn use_(&self, _value: super::mortar_js_value::MortarJsValue) -> EvalRes {
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        let mut result_fields = BTreeMap::new();
        for (key, field) in &self.fields {
            match field {
                RecordScopeField::Scope(se) => {
                    let cv = se.use_(loc, Code {
                        loc,
                        code: Code_::None,
                    });
                    result_fields.insert(key.clone(), RecordField::EvalRes(EvalRes::ok(cv)));
                },
                RecordScopeField::Mortar(mse) => {
                    let value = super::mortar_js_value::MortarJsValue {
                        source: super::mortar_js_value::MortarJsValueSource::Expr,
                        type_: Rc::new(RecordType { entry_types: BTreeMap::new() }),
                    };
                    let er = mse.use_(value);
                    result_fields.insert(key.clone(), RecordField::EvalRes(er));
                },
                RecordScopeField::Error(errs) => {
                    result_fields.insert(key.clone(), RecordField::EvalRes(EvalRes {
                        v: None,
                        errors: Some(errs.clone()),
                    }));
                },
            }
        }
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Box::new(RecordValue {
                source: None,
                fields: result_fields,
            }),
        })
    }
}

impl ScopeEntry for RecordScopeEntry {
    fn use_(&self, loc: QExprId, code: Code) -> CodeValue {
        let mut result_fields = BTreeMap::new();
        for (key, field) in &self.fields {
            match field {
                RecordScopeField::Scope(se) => {
                    let cv = se.use_(loc, code.clone());
                    result_fields.insert(key.clone(), RecordField::EvalRes(EvalRes::ok(cv)));
                },
                RecordScopeField::Mortar(mse) => {
                    let value = super::mortar_js_value::MortarJsValue {
                        source: super::mortar_js_value::MortarJsValueSource::Expr,
                        type_: Rc::new(RecordType { entry_types: BTreeMap::new() }),
                    };
                    let er = mse.use_(value);
                    result_fields.insert(key.clone(), RecordField::EvalRes(er));
                },
                RecordScopeField::Error(errs) => {
                    result_fields.insert(key.clone(), RecordField::EvalRes(EvalRes {
                        v: None,
                        errors: Some(errs.clone()),
                    }));
                },
            }
        }
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Box::new(RecordValue {
                source: None,
                fields: result_fields,
            }),
        }
    }
}
