use {
    crate::{
        builtin::{
            AspectConsumeJs,
            AspectType,
            aspect_consume_js_aspect_id,
            aspect_record_get_maybe,
            aspect_tuple_get_maybe,
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
};

pub(crate) struct TypeRefValue {
    pub type_: Rc<dyn Type>,
    pub field_codes: Vec<(crate::langhelp::ConstKey, crate::langhelp::Code)>,
}

impl Value for TypeRefValue {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeType)
    }
}
pub(crate) struct TypeType;

pub(crate) fn type_type_identity() -> Unique {
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
        let mut field_codes = Vec::new();
        if aspect_record_get_maybe(xx, type_.as_ref()).is_some() {
            let record =
                (cv.value.as_ref() as &dyn Any)
                    .downcast_ref::<crate::eval::record::Record>()
                    .expect("TypeType cast_from: record aspect present but value is not Record");
            for (key, field) in &record.fields {
                if let crate::eval::record::RecordField::EvalRes(entry_res) = field {
                    if let Some(entry_cv) = &entry_res.v {
                        field_codes.push((key.clone(), entry_cv.code.clone()));
                    }
                }
            }
        } else if aspect_tuple_get_maybe(xx, type_.as_ref()).is_some() {
            let tuple =
                (cv.value.as_ref() as &dyn Any)
                    .downcast_ref::<crate::eval::tuple::Tuple>()
                    .expect("TypeType cast_from: tuple aspect present but value is not Tuple");
            for (i, elem) in tuple.elements.iter().enumerate() {
                if let crate::eval::tuple::TupleElement::EvalRes(entry_res) = elem {
                    if let Some(entry_cv) = &entry_res.v {
                        field_codes.push((crate::langhelp::ConstKey::Usize(i), entry_cv.code.clone()));
                    }
                }
            }
        }
        crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
            code: cv.code,
            value: Box::new(TypeRefValue {
                type_,
                field_codes,
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
        any.downcast_ref::<TypeRefValue>()
            .expect("TypeTypeAspectType: value must be TypeRefValue")
            .type_
            .clone()
    }
}

impl AspectConsumeJs for TypeType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: crate::langhelp::CodeValue) -> EResult<JsCode> {
        let type_ref_value =
            (cv.value.as_ref() as &dyn Any)
                .downcast_ref::<TypeRefValue>()
                .expect("TypeType to_js: value must be TypeRefValue");
        let field_code_map: std::collections::BTreeMap<crate::langhelp::ConstKey, crate::langhelp::Code> =
            type_ref_value.field_codes.iter().cloned().collect();
        type_ref_to_js(xx, loc, &type_ref_value.type_, &field_code_map)
    }
}

fn type_ref_to_js(
    xx: &ExprContext,
    loc: QExprId,
    type_: &Rc<dyn Type>,
    field_codes: &std::collections::BTreeMap<crate::langhelp::ConstKey, crate::langhelp::Code>,
) -> EResult<JsCode> {
    // Tuple types → build JS array from field codes, then wrap with __tuple_to_type()
    // so the v8 result is always an Rc`<dyn Type>` external.
    if aspect_tuple_get_maybe(xx, type_.as_ref()).is_some() {
        let tuple =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<super::tuple::Tuple>()
                .expect("type_ref_to_js: tuple aspect present but type is not Tuple");
        let mut elements = Vec::new();
        for (i, elem) in tuple.elements.iter().enumerate() {
            let elem_type = match elem {
                super::tuple::TupleElement::Mortar(t) => t,
                super::tuple::TupleElement::EvalRes(_) => {
                    return EResult::err(loc, "Tuple used as type contains non-type element".to_string());
                },
            };
            let key = crate::langhelp::ConstKey::Usize(i);
            match type_ref_to_js_leaf(xx, loc, elem_type, field_codes.get(&key)).into_res() {
                Ok(js) => elements.push(js),
                Err(e) => return EResult {
                    v: None,
                    errors: Some(e),
                },
            }
        }
        let array_js = JsCode {
            loc,
            type_: JsCodeType::NewArray(crate::jscode::JsCodeNewArray { elements }),
        };
        return EResult::ok(JsCode {
            loc,
            type_: JsCodeType::Call(crate::jscode::JsCodeCall {
                func: Box::new(JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: super::TUPLE_TO_TYPE_FN_SCOPE_ID }),
                }),
                args: vec![array_js],
            }),
        });
    }

    // Record types → build JS object from field codes, then wrap with
    // __record_to_type() so the v8 result is always an Rc`<dyn Type>` external.
    if aspect_record_get_maybe(xx, type_.as_ref()).is_some() {
        let record =
            (type_.as_ref() as &dyn Any)
                .downcast_ref::<super::record::Record>()
                .expect("type_ref_to_js: record aspect present but type is not Record");
        let mut fields = Vec::new();
        for (key, field) in &record.fields {
            let field_type = match field {
                super::record::RecordField::Mortar(t) => t,
                super::record::RecordField::EvalRes(_) => {
                    return EResult::err(loc, "Record used as type contains non-type field".to_string());
                },
            };
            let key_str = match key {
                crate::langhelp::ConstKey::String(s) => s.clone(),
                crate::langhelp::ConstKey::Usize(n) => n.to_string(),
            };
            match type_ref_to_js_leaf(xx, loc, field_type, field_codes.get(key)).into_res() {
                Ok(js) => fields.push((key_str, js)),
                Err(e) => return EResult {
                    v: None,
                    errors: Some(e),
                },
            }
        }
        let obj_js = JsCode {
            loc,
            type_: JsCodeType::NewObject(crate::jscode::JsCodeNewObject { fields }),
        };
        return EResult::ok(JsCode {
            loc,
            type_: JsCodeType::Call(crate::jscode::JsCodeCall {
                func: Box::new(JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: super::RECORD_TO_TYPE_FN_SCOPE_ID }),
                }),
                args: vec![obj_js],
            }),
        });
    }

    // Leaf type at root level
    type_ref_to_js_leaf(xx, loc, type_, None)
}

fn type_ref_to_js_leaf(
    xx: &ExprContext,
    loc: QExprId,
    type_: &Rc<dyn Type>,
    field_code: Option<&crate::langhelp::Code>,
) -> EResult<JsCode> {
    // If the type is a record/tuple, recurse
    if aspect_record_get_maybe(xx, type_.as_ref()).is_some() ||
        aspect_tuple_get_maybe(xx, type_.as_ref()).is_some() {
        let empty = std::collections::BTreeMap::new();
        return type_ref_to_js(xx, loc, type_, &empty);
    }

    // Leaf type: use original code if available
    if let Some(code) = field_code {
        if let Some(js) = code.cast::<JsCode>().v.flatten() {
            return EResult::ok(js.clone());
        }
    }

    // Otherwise store as Rc`<dyn Type>` external
    let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
    xx.vx.externals.borrow_mut().push((id, crate::langhelp::External::Type(type_.clone())));
    EResult::ok(JsCode {
        loc,
        type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
    })
}
