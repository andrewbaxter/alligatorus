use {
    crate::{
        builtin::{
            AspectFunction,
            AspectInitialBind,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
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
pub(crate) struct TypeOptionConstructorConst;

impl Aspect for TypeOptionConstructorConst { }

impl AspectFunction for TypeOptionConstructorConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_meta = argument.value.type_(xx, loc);
        let aspect_type = match crate::builtin::aspect_type_get(xx, loc, arg_meta.as_ref()).into_res() {
            Ok(at) => at,
            Err(e) => return EvalRes { v: None, errors: Some(e) },
        };
        let element_type = aspect_type.type_(argument.value.as_ref());
        let opt_type: Rc<dyn Type> =
            Rc::new(super::super::type_option::dynamic::TypeOptionDynamic { element_type });
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(super::super::type_any::r#const::ValueAnyConst { inner: opt_type }),
        })
    }
}

impl AspectInitialBind for TypeOptionConstructorConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeOptionConstructorConst),
        })
    }
}

impl Type for TypeOptionConstructorConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeOptionConstructorConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeOptionConstructorConst) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeOptionConstructorConst) as Box<dyn AspectInitialBind>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to option constructor".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert option constructor".to_string())
    }

    fn desc(&self) -> String {
        "option constructor".to_string()
    }
}

impl Value for TypeOptionConstructorConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
