use {
    crate::{
        artifact::deserialize_artifact,
        langhelp::{
            CompileResResult,
            CompileResultEntry,
            EResult,
            ExprContext,
            QExprId,
            Value_,
        },
    },
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct ValuePendingImport {
    pub entry: CompileResultEntry,
    pub resolved: RefCell<Option<EResult<Rc<dyn Value_>>>>,
}

impl ValuePendingImport {
    pub(crate) fn resolve(&self, xx: &ExprContext, loc: QExprId) -> EResult<Rc<dyn Value_>> {
        let mut cached = self.resolved.borrow_mut();
        if let Some(v) = cached.as_ref() {
            return v.clone();
        }
        let guard = self.entry.read().unwrap();
        let compile_res = guard.as_ref().unwrap();
        let artifact = match &compile_res.result {
            CompileResResult::Ok(a) => a.clone(),
            CompileResResult::Err(e) => return EResult {
                errors: Some(e.clone()),
                v: None,
            },
        };
        let mut res = deserialize_artifact(xx, loc, artifact);
        let mut errors = None;
        res.push_errors(&mut errors);
        let Some(value) = res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let mut value = value.as_value();
        value.push_errors(&mut errors);
        let Some(value) = value.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let out = EResult {
            errors: errors,
            v: Some(value),
        };
        *cached = Some(out.clone());
        out
    }
}
