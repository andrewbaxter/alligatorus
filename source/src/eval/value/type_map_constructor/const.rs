use {
    crate::{
        builtin::{
            AspectFunction,
            AspectInitialBind,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_tuple_get_maybe,
        },
        eval::value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntry,
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
        let element_types = arg_tuple.element_types();
        if element_types.len() != 2 {
            return EvalRes::err(
                loc,
                format!("map() requires exactly 2 type arguments (key, value), got {}", element_types.len()),
            );
        }
        let key_type = element_types[0].clone();
        let value_type = element_types[1].clone();
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(super::super::type_map::dynamic::TypeMapDynamic {
                key_type,
                value_type,
            }),
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
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeMapConstructorConst)
    }
}
