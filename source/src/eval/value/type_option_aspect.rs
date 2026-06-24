use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_bool::dynamic::TypeBoolDynamic,
            type_function::dynamic::TypeFunctionDynamic,
            type_null::dynamic::TypeNullDynamic,
            value_bool::r#const::ValueBoolConst,
        },
        invariant::must_downcast_ref,
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
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectField,
            AspectFunction,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_string_const_get,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

struct OptionIsSome {
    receiver: Rc<dyn Value>,
}

impl ExporterAny for OptionIsSome {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for OptionIsSome {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeOptionIsSomeFn))
    }
}
pub(crate) struct TypeOption;

impl Aspect for TypeOption { }

impl AspectField for TypeOption {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = match key.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        match key_name.as_str() {
            "is_some" => EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(OptionIsSome { receiver: self_.value }),
            }),
            _ => EvalRes::err(loc, format!("Field '{}' not found on option", key_name)),
        }
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(k) = key else {
            return None;
        };
        if k == "is_some" {
            return Some(Rc::new(TypeFunctionDynamic {
                arg: Rc::new(TypeNullDynamic),
                ret: Rc::new(TypeBoolDynamic),
            }));
        }
        None
    }
}

impl ExporterAny for TypeOption {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeOption {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeOption)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypeOption) as Box<dyn AspectField>));
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to option".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from option".to_string())
    }

    fn desc(&self) -> String {
        "option".to_string()
    }
}

impl Value for TypeOption {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(super::type_any::r#const::TypeAnyConst))
    }
}
struct TypeOptionIsSomeFn;

impl Aspect for TypeOptionIsSomeFn { }

impl AspectFunction for TypeOptionIsSomeFn {
    fn call(&self, _xx: &ExprContext, loc: QExprId, self_: CodeValue, _argument: CodeValue) -> EvalRes {
        let is_some_fn = must_downcast_ref::<OptionIsSome>(self_.value.as_ref());
        let opt = must_downcast_ref::<ValueOption>(is_some_fn.receiver.as_ref());
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueBoolConst(opt.is_some)),
        })
    }
}

impl ExporterAny for TypeOptionIsSomeFn {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeOptionIsSomeFn {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeOptionIsSomeFn)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeOptionIsSomeFn) as Box<dyn AspectFunction>));
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to option.is_some fn".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from option.is_some fn".to_string())
    }

    fn desc(&self) -> String {
        "option.is_some".to_string()
    }
}

impl Value for TypeOptionIsSomeFn {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(super::type_any::r#const::TypeAnyConst))
    }
}

pub(crate) struct ValueOption {
    pub is_some: bool,
}

impl ExporterAny for ValueOption {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for ValueOption {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeOption))
    }
}
