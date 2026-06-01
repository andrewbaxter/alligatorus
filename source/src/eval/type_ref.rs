use {
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectType,
            aspect_consume_js_aspect_id,
            aspect_type_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            EResult,
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
        rc::Rc,
    },
    super::expr::StagedExprCell,
};

pub(crate) struct TypeRefValue {
    pub type_: Rc<dyn Type>,
    /// For record types, maps entry names to their v8 binding IDs so that
    /// TypeRef::from_v8 can look up the actual v8 values
    pub entry_bindings: std::collections::BTreeMap<String, ScopeEntryId>,
}

impl Value for TypeRefValue {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeType)
    }
}
pub(crate) struct TypeType;

fn type_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for TypeType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeType)
    }
}

impl Type for TypeType {
    fn desc(&self) -> String {
        "type".to_string()
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeType) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeAspectType) as Box<dyn AspectType>));
        }
        None
    }

    fn cast_to(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _cv: crate::langhelp::CodeValue,
        _type_: &dyn Type,
    ) -> crate::langhelp::EvalRes {
        crate::langhelp::EvalRes::err(loc, "Cannot convert type".to_string())
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _cv: &crate::langhelp::CodeValue) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> crate::langhelp::EvalRes {
        let type_ = cv.value.type_(xx, loc);

        // For record values, extract entry binding IDs so TypeRef::from_v8 can look up
        // actual v8 values (needed for tagged union data)
        let mut entry_bindings = std::collections::BTreeMap::new();
        if let Some(record) = (cv.value.as_ref() as &dyn Any).downcast_ref::<crate::eval::record::Record>() {
            for (key, field) in &record.fields {
                if let crate::eval::record::RecordField::EvalRes(entry_res) = field {
                    if let Some(entry_cv) = &entry_res.v {
                        if let Some(js) = entry_cv.code.cast::<JsCode>().v.flatten() {
                            if let JsCodeType::BindingUse(b) = &js.type_ {
                                if let crate::langhelp::ConstKey::String(s) = key {
                                    entry_bindings.insert(s.clone(), b.id);
                                }
                            }
                        }
                    }
                }
            }
        }
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: cv.code,
            value: Box::new(TypeRefValue {
                type_,
                entry_bindings,
            }),
        })
    }
}

impl Aspect for TypeType { }
struct TypeTypeAspectType;

impl Aspect for TypeTypeAspectType { }

impl AspectType for TypeTypeAspectType {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let any: &dyn Any = value;
        if let Some(trv) = any.downcast_ref::<TypeRefValue>() {
            return trv.type_.clone();
        }
        if let Some(tt) = any.downcast_ref::<super::tuple::Tuple>() {
            return Rc::new(tt.clone_as_type());
        }
        if let Some(rv) = any.downcast_ref::<super::record::Record>() {
            return rv.as_type();
        }
        panic!("TypeTypeAspectType: unknown type value");
    }
}

impl AspectConsumeJs for TypeType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<JsCode> {
        let type_ref_value =
            (cv.value.as_ref() as &dyn Any)
                .downcast_ref::<TypeRefValue>()
                .expect("TypeType value must be TypeRefValue");
        let cell = Rc::new(StagedExprCell { value: std::cell::RefCell::new(Some(Box::new(TypeRefValue {
            type_: type_ref_value.type_.clone(),
            entry_bindings: type_ref_value.entry_bindings.clone(),
        }) as Box<dyn Value>)) });
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, cell));
        let type_ref_code = JsCode {
            loc,
            type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
        };

        // If we have entry bindings (from a record type), bundle them alongside so that
        // TypeRef::from_v8 can look up the actual v8 values
        if !type_ref_value.entry_bindings.is_empty() {
            let fields: Vec<(String, JsCode)> = type_ref_value.entry_bindings.iter().map(|(name, binding_id)| {
                (name.clone(), JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: *binding_id }),
                })
            }).collect();
            return EResult::ok(JsCode {
                loc,
                type_: JsCodeType::NewArray(crate::jscode::JsCodeNewArray { elements: vec![type_ref_code, JsCode {
                    loc,
                    type_: JsCodeType::NewObject(crate::jscode::JsCodeNewObject { fields }),
                },] }),
            });
        }
        EResult::ok(type_ref_code)
    }
}
