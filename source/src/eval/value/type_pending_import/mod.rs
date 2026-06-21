pub mod scope_entry;

use {
    crate::artifact::ExporterAny,
    crate::{
        eval::value::type_any,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            SourceId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            ScopeEntry,
            aspect_bind_aspect_id,
        },
        syntax::ExprId,
    },
    scope_entry::ScopeEntryPendingImport,
    std::{
        any::Any,
        rc::Rc,
    },
    super::value_pending_import::PendingImportState,
};

struct PendingImportBind {
    state: Rc<PendingImportState>,
}

impl Aspect for PendingImportBind { }

impl AspectBind for PendingImportBind {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((Code {
            loc: loc,
            code: Code_::None,
        }, Box::new(ScopeEntryPendingImport { state: self.state.clone() }) as Box<dyn ScopeEntry>))
    }
}

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

    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(PendingImportBind { state: self.state.clone() }) as Box<dyn AspectBind>),
            );
        }
        let loc = (SourceId(0), ExprId(0));
        self.resolved_type(xx, loc).aspect(xx, key)
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        self.resolved_type(xx, loc).can_cast_from(xx, loc, source_type)
    }

    fn can_cast_to(&self, xx: &ExprContext, loc: QExprId, type_: &dyn Type) -> bool {
        let resolved = self.resolved_type(xx, loc);
        resolved.can_cast_to(xx, loc, type_) || type_.can_cast_from(xx, loc, resolved.as_ref())
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        self.resolved_type(xx, loc).cast_from(xx, loc, cv)
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
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

impl ExporterAny for TypePendingImport {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for TypePendingImport {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(type_any::r#const::TypeAnyConst))
    }
}
