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
pub(crate) struct TypeListConstructorConst;

impl Aspect for TypeListConstructorConst { }

impl AspectFunction for TypeListConstructorConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let element_type = argument.value.type_(xx, loc);
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(super::super::type_list::dynamic::TypeListDynamic { element_type }),
        })
    }
}

impl AspectInitialBind for TypeListConstructorConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeListConstructorConst),
        })
    }
}

impl Type for TypeListConstructorConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeListConstructorConst)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeListConstructorConst) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeListConstructorConst) as Box<dyn AspectInitialBind>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to list constructor".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert list constructor".to_string())
    }

    fn desc(&self) -> String {
        "list constructor".to_string()
    }
}

impl Value for TypeListConstructorConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeListConstructorConst)
    }
}
