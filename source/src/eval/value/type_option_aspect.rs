//! A simple const-only `Option`. Returned by `.aspect(unique)` lookups on
//! Type values: `Some(empty)` if the type has the aspect, `None` otherwise.
//! Supports `.is_some() -> bool` via an `AspectField` that returns a callable.

use {
    crate::{
        builtin::{
            AspectField,
            AspectFunction,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_string_const_get,
        },
        eval::value::{
            type_bool::dynamic::TypeBoolDynamic,
            type_function::dynamic::TypeFunctionDynamic,
            type_null::dynamic::TypeNullDynamic,
            value_bool::r#const::ValueBoolConst,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
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

/// A const-only Option value. `is_some` is true when wrapping a present value.
pub(crate) struct ValueOption {
    pub is_some: bool,
}

impl Value for ValueOption {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeOption)
    }
}

pub(crate) struct TypeOption;

impl Aspect for TypeOption { }

impl Value for TypeOption {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_any::r#const::TypeAnyConst)
    }
}

impl AspectField for TypeOption {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let key_name = key_string.get(key.value.as_ref());
        match key_name.as_str() {
            "is_some" => EvalRes::ok(CodeValue {
                code: Code { loc, code: Code_::None },
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

/// The closure-like value produced by `option.is_some` (a 0-arg function).
/// `receiver` is the option value the field was accessed on; we resolve
/// `is_some` against it lazily so source code that captures the field as a
/// value still works.
struct OptionIsSome {
    receiver: Rc<dyn Value>,
}

impl Value for OptionIsSome {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeOptionIsSomeFn)
    }
}

struct TypeOptionIsSomeFn;

impl Aspect for TypeOptionIsSomeFn { }

impl Value for TypeOptionIsSomeFn {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_any::r#const::TypeAnyConst)
    }
}

impl AspectFunction for TypeOptionIsSomeFn {
    fn call(&self, _xx: &ExprContext, loc: QExprId, self_: CodeValue, _argument: CodeValue) -> EvalRes {
        let is_some_fn = crate::invariant::must_downcast_ref::<OptionIsSome>(self_.value.as_ref());
        let opt = crate::invariant::must_downcast_ref::<ValueOption>(is_some_fn.receiver.as_ref());
        EvalRes::ok(CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(ValueBoolConst(opt.is_some)),
        })
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

