use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectLiftJs,
            AspectTuple,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
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

/// When a compound value (Tuple/Record) is the last expression in a block, the block's code
/// contains binding declarations but the compound value has Code_::None. When consumed to JS,
/// the generated JS references those bindings. This helper wraps the consumed JS with the
/// carry code so the bindings are in scope.
fn wrap_with_carry(loc: QExprId, carry: Code, js: JsCode) -> JsCode {
    if let Some(Some(carry_js)) = carry.cast::<JsCode>().v {
        let mut entries = match &carry_js.type_ {
            JsCodeType::Block(block) => block.entries.clone(),
            _ => vec![JsCodeBlockEntry::Code(carry_js.clone())],
        };
        entries.push(JsCodeBlockEntry::Code(js));
        return JsCode { loc, type_: JsCodeType::Block(JsCodeBlock { entries }) };
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
            errors.extend(elem_res.errors);
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
                errors,
            };
        }
        EResult::ok(wrap_with_carry(loc, carry, JsCode {
            loc,
            type_: JsCodeType::NewArray(JsCodeNewArray { elements: js_elements }),
        }))
    }
}

impl AspectBindJs for TypeTuple {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

pub(crate) struct ValueRecord {
    pub entries: BTreeMap<ConstKey, EvalRes>,
}

impl Value for ValueRecord {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let entry_types: BTreeMap<ConstKey, Rc<dyn Type>> = self.entries.iter().map(|(k, e)| {
            let t = match &e.v {
                Some(cv) => cv.value.type_(xx, loc),
                None => Rc::new(TypeTuple { types: vec![] }) as Rc<dyn Type>,
            };
            (k.clone(), t)
        }).collect();
        Rc::new(TypeValueRecord { entry_types })
    }
}

pub(crate) struct TypeValueRecord {
    pub entry_types: BTreeMap<ConstKey, Rc<dyn Type>>,
}

impl Value for TypeValueRecord {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeValueRecord {
    fn desc(&self) -> String {
        format!("record({})", self.entry_types.iter().map(|(k, t)| format!("{:?}: {}", k, t.desc())).collect::<Vec<_>>().join(", "))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeValueRecord { entry_types: self.entry_types.clone() }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeValueRecord { entry_types: self.entry_types.clone() }) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_lift_js_aspect_id() {
            let entry_lifts: Vec<(String, Box<dyn AspectLiftJs>)> = self.entry_types.iter().map(|(k, t)| {
                let key_str = match k {
                    ConstKey::String(s) => s.clone(),
                };
                let a = t
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

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to record type"))
    }
}

impl Aspect for TypeValueRecord { }

impl AspectConsumeJs for TypeValueRecord {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        let carry = cv.code;
        let record: Box<ValueRecord> = (cv.value as Box<dyn Any>).downcast().unwrap();
        let mut errors = vec![];
        let mut js_fields = Vec::new();
        for (key, elem_res) in record.entries.into_iter() {
            let elem_type = self.entry_types.get(&key).expect("Record entry type missing");
            let key_str = match &key {
                ConstKey::String(s) => s.clone(),
            };
            errors.extend(elem_res.errors);
            if let Some(elem_cv) = elem_res.v {
                match aspect_consume_js_get(xx, loc, elem_type.as_ref()).into_res() {
                    Ok(to_js) => match to_js.to_js(xx, loc, elem_cv).into_res() {
                        Ok(js) => js_fields.push((key_str, js)),
                        Err(e) => errors.extend(e),
                    },
                    Err(e) => errors.extend(e),
                }
            }
        }
        if !errors.is_empty() {
            return EResult { v: None, errors };
        }
        EResult::ok(wrap_with_carry(loc, carry, JsCode {
            loc,
            type_: JsCodeType::NewObject(JsCodeNewObject { fields: js_fields }),
        }))
    }
}

impl AspectBindJs for TypeValueRecord {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
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

// --- Record types (for JS object records) ---

pub(crate) struct TypeRecord {
    pub entries: Vec<(String, Rc<dyn Type>)>,
}

impl Value for TypeRecord {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for TypeRecord {
    fn desc(&self) -> String {
        format!("record({})", self.entries.iter().map(|(k, t)| format!("{}: {}", k, t.desc())).collect::<Vec<_>>().join(", "))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeRecord { entries: self.entries.clone() }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeRecord { entries: self.entries.clone() }) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_lift_js_aspect_id() {
            let entry_lifts: Vec<(String, Box<dyn AspectLiftJs>)> = self.entries.iter().map(|(k, t)| {
                let a = t
                    .aspect(_xx, &aspect_lift_js_aspect_id())
                    .expect(&format!("Record entry '{}' type ({}) missing AspectLiftJs", k, t.desc()));
                (k.clone(), *a.downcast::<Box<dyn AspectLiftJs>>().unwrap())
            }).collect();
            return Some(Box::new(Box::new(RecordLiftJs { entry_lifts }) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert record to other type"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to record type"))
    }
}

impl Aspect for TypeRecord { }

impl AspectConsumeJs for TypeRecord {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for TypeRecord {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

struct RecordLiftJs {
    entry_lifts: Vec<(String, Box<dyn AspectLiftJs>)>,
}

impl Aspect for RecordLiftJs { }

impl AspectLiftJs for RecordLiftJs {
    fn lift(&self, scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let obj = v8::Local::<v8::Object>::try_from(value).expect("Expected JS object for record lift");
        let mut entries = Vec::new();
        for (key, entry_lift) in &self.entry_lifts {
            let v8_key = v8::String::new(scope, key).unwrap();
            let v8_val = obj.get(scope, v8_key.into()).expect("Record field missing in JS object");
            let lifted = entry_lift.lift(scope, v8_val)?;
            entries.push((key.clone(), lifted));
        }
        Ok(Box::new(LiftedRecord { entries }))
    }
}

pub(crate) struct LiftedRecord {
    pub entries: Vec<(String, Box<dyn Value>)>,
}

impl Value for LiftedRecord {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        let type_entries: Vec<(String, Rc<dyn Type>)> = self.entries.iter()
            .map(|(k, v)| (k.clone(), v.type_(xx, loc)))
            .collect();
        Rc::new(LiftedRecordType { entries: type_entries })
    }
}

pub(crate) struct LiftedRecordType {
    pub entries: Vec<(String, Rc<dyn Type>)>,
}

impl Value for LiftedRecordType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for LiftedRecordType {
    fn desc(&self) -> String {
        format!("lifted_record({})", self.entries.iter().map(|(k, t)| format!("{}: {}", k, t.desc())).collect::<Vec<_>>().join(", "))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(LiftedRecordType { entries: self.entries.clone() }) as Box<dyn AspectField>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert lifted record"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to lifted record"))
    }
}

impl Aspect for LiftedRecordType { }

impl AspectField for LiftedRecordType {
    fn field(&self, xx: &ExprContext, loc: QExprId, _self_: CodeValue, key: CodeValue) -> EvalRes {
        return super::util::er_seq_cv_inline(loc, |erb| {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            if let Some((_, entry_type)) = self.entries.iter().find(|(k, _)| k == &key_name) {
                return Ok(CodeValue {
                    code: Code { loc, code: Code_::None },
                    value: Box::new(StaticValue { type_: entry_type.clone() }),
                });
            }
            return erb.err_new(format!("Field '{}' not found on record", key_name));
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        self.entries.iter().find(|(k, _)| k == key).map(|(_, t)| t.clone())
    }
}
