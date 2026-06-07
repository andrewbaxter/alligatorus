pub mod scope_entry;

pub(crate) use scope_entry::ScopeEntryPendingImport;
pub(crate) use super::value_pending_import::PendingImportState;
use {
    crate::{
        eval::value::type_any,
        langhelp::{
            CodeValue,
            EResult,
            ExprContext,
            QExprId,
            ScopeEntry,
            Type,
            Value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

struct PendingImportBind {
    state: Rc<PendingImportState>,
}

impl crate::builtin::AspectBind for PendingImportBind {
    fn bind(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(crate::langhelp::Code, Box<dyn ScopeEntry>)> {
        EResult::ok((crate::langhelp::Code {
            loc: loc,
            code: crate::langhelp::Code_::None,
        }, Box::new(ScopeEntryPendingImport {
            state: self.state.clone(),
            code: cv.code,
        }) as Box<dyn ScopeEntry>))
    }
}

impl crate::langhelp::Aspect for PendingImportBind { }

pub(crate) struct TypePendingImport {
    pub(crate) state: Rc<PendingImportState>,
    pub(crate) resolved: Rc<dyn Value>,
}

impl TypePendingImport {
    fn resolved_type(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        self.resolved.type_(xx, loc)
    }

    fn substitute_cv(&self, cv: CodeValue) -> CodeValue {
        CodeValue {
            code: cv.code,
            value: self.resolved.clone(),
        }
    }
}

impl Type for TypePendingImport {
    fn desc(&self) -> String {
        "pending import".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        self.resolved.clone()
    }

    fn aspect(&self, xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn Any>> {
        if *key == crate::builtin::aspect_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(PendingImportBind { state: self.state.clone() }) as Box<dyn crate::builtin::AspectBind>,
                ),
            );
        }

        // For all other aspects, resolve and delegate
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        self.resolved_type(xx, loc).aspect(xx, key)
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> crate::langhelp::EvalRes {
        self.resolved_type(xx, loc).cast_to(xx, loc, self.substitute_cv(cv), type_)
    }

    fn can_cast_to(&self, xx: &ExprContext, loc: QExprId, type_: &dyn Type) -> bool {
        self.resolved_type(xx, loc).can_cast_to(xx, loc, type_)
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> crate::langhelp::EvalRes {
        self.resolved_type(xx, loc).cast_from(xx, loc, cv)
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        self.resolved_type(xx, loc).can_cast_from(xx, loc, source_type)
    }
}

impl Value for TypePendingImport {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(type_any::r#const::TypeAnyConst)
    }
}
