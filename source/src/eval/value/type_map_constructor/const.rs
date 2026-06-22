use {
    crate::{
        artifact::ExporterAny,
        eval::value::{
            type_any::r#const::{
                TypeAnyConst,
                ValueAnyConst,
            },
            type_root::r#const::TypeRoot,
            type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectFunction,
            AspectInitialBind,
            ScopeEntry,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_tuple_get_maybe,
            aspect_type_get,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeMapConstructorConst;

impl Aspect for TypeMapConstructorConst { }

impl AspectFunction for TypeMapConstructorConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_type = argument.value.type_(xx, loc);
        let arg_tuple = match aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            Some(t) => t,
            None => return EvalRes::err(loc, "map() requires a tuple of (key_type, value_type)".to_string()),
        };
        let elements = arg_tuple.take_elements(argument.value);
        if elements.len() != 2 {
            return EvalRes::err(
                loc,
                format!("map() requires exactly 2 type arguments (key, value), got {}", elements.len()),
            );
        }
        let mut iter = elements.into_iter();
        let key_cv = match iter.next().unwrap().into_res() {
            Ok(c) => c,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let value_cv = match iter.next().unwrap().into_res() {
            Ok(c) => c,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_meta = key_cv.value.type_(xx, loc);
        let key_at = match aspect_type_get(xx, loc, key_meta.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_type = key_at.type_(key_cv.value.as_ref());
        let value_meta = value_cv.value.type_(xx, loc);
        let value_at = match aspect_type_get(xx, loc, value_meta.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let value_type = value_at.type_(value_cv.value.as_ref());
        let map_type: Rc<dyn Type> = Rc::new(super::super::type_map::dynamic::TypeMapDynamic {
            key_type,
            value_type,
        });
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueAnyConst { inner: map_type }),
        })
    }
}

impl AspectInitialBind for TypeMapConstructorConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeMapConstructorConst),
        })
    }
}

impl ExporterAny for TypeMapConstructorConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeMapConstructorConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeMapConstructorConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeMapConstructorConst) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeMapConstructorConst) as Box<dyn AspectInitialBind>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to map constructor".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map constructor".to_string())
    }

    fn desc(&self) -> String {
        "map constructor".to_string()
    }
}

impl Value for TypeMapConstructorConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::MapConstructorConst))
    }
}
