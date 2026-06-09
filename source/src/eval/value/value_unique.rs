//! `Unique` as a value, plus the `TypeUnique` that types it. Used to expose
//! aspect ids (like `aspect_string_dynamic`) as scope-usable values that can
//! be passed to `.aspect(...)` lookups.

use {
    crate::{
        builtin::{
            AspectBind,
            aspect_bind_aspect_id,
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
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
        rc::Rc,
    },
};

pub(crate) struct TypeUnique;

impl Value for Unique {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeUnique)
    }
}

impl Aspect for TypeUnique { }

impl Value for TypeUnique {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_any::r#const::TypeAnyConst)
    }
}

impl AspectBind for TypeUnique {
    fn bind(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((
            cv.code,
            Box::new(super::tuple::scope_entry::ScopeEntryValue { value: cv.value }) as Box<dyn ScopeEntry>,
        ))
    }
}

impl Type for TypeUnique {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeUnique)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeUnique) as Box<dyn AspectBind>));
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to unique".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert from unique".to_string())
    }

    fn desc(&self) -> String {
        "unique".to_string()
    }
}
