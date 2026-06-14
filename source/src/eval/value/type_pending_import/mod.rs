pub mod scope_entry;

use crate::r#gen::ScopeEntry;
use {
    crate::{
        eval::value::type_any,
        langhelp::{
            CodeValue,
            EResult,
            ExprContext,
            QExprId,
            Type,
            Value,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};
pub(crate) use {
    scope_entry::ScopeEntryPendingImport,
    super::value_pending_import::PendingImportState,
};

struct PendingImportBind {
    state: Rc<PendingImportState>,
}

impl crate::r#gen::AspectBind for PendingImportBind {
    fn bind(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _cv: CodeValue,
    ) -> EResult<(crate::langhelp::Code, Box<dyn ScopeEntry>)> {
        EResult::ok((crate::langhelp::Code {
            loc: loc,
            code: crate::langhelp::Code_::None,
        }, Box::new(ScopeEntryPendingImport { state: self.state.clone() }) as Box<dyn ScopeEntry>))
    }
}

impl crate::langhelp::Aspect for PendingImportBind { }

pub(crate) struct TypePendingImport {
    pub(crate) resolved: Rc<dyn Value>,
    pub(crate) state: Rc<PendingImportState>,
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
    fn as_value(&self) -> Rc<dyn Value> {
        self.resolved.clone()
    }

    fn aspect(&self, xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn Any>> {
        if *key == crate::r#gen::aspect_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(PendingImportBind { state: self.state.clone() }) as Box<dyn crate::r#gen::AspectBind>,
                ),
            );
        }

        // For all other aspects, resolve and delegate
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        self.resolved_type(xx, loc).aspect(xx, key)
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        self.resolved_type(xx, loc).can_cast_from(xx, loc, source_type)
    }

    fn can_cast_to(&self, xx: &ExprContext, loc: QExprId, type_: &dyn Type) -> bool {
        let resolved = self.resolved_type(xx, loc);
        resolved.can_cast_to(xx, loc, type_) || type_.can_cast_from(xx, loc, resolved.as_ref())
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> crate::langhelp::EvalRes {
        self.resolved_type(xx, loc).cast_from(xx, loc, cv)
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> crate::langhelp::EvalRes {
        let resolved = self.resolved_type(xx, loc);
        let cv = self.substitute_cv(cv);
        if resolved.can_cast_to(xx, loc, type_) {
            resolved.cast_to(xx, loc, cv, type_)
        } else {
            type_.cast_from(xx, loc, cv)
        }
    }

    fn desc(&self) -> String {
        "pending import".to_string()
    }
}

impl Value for TypePendingImport {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(type_any::r#const::TypeAnyConst)
    }
}
