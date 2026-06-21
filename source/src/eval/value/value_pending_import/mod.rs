use {
    crate::{
        artifact::{
            ValueFromAny,
            deserialize_artifact,
        },
        eval::{
            TargetDynamic,
            value::{
                type_source_data_type::artifact::ArtifactDataDefSetDataType,
                type_source_interface::BuiltinInterfaceKind,
            },
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            CompileImportContext,
            CompileResResult,
            CompileResultEntry,
            Counter,
            EResult,
            EvalContext,
            ExprContext,
            GlobalContext,
            ImportStateId,
            QExprId,
            Scope,
            SourceId,
            Type,
            Value,
            make_builtin_deserializers,
        },
        syntax::ExprId,
    },
    std::{
        cell::{
            Cell,
            RefCell,
        },
        collections::HashMap,
        rc::Rc,
        sync::Arc,
    },
};

pub(crate) struct ValuePendingImport {
    pub entry: CompileResultEntry,
    pub gx: Arc<GlobalContext>,
    pub resolved: RefCell<Option<EResult<Rc<dyn Value>>>>,
}

impl ValuePendingImport {
    pub(crate) fn resolve(&self, xx: &ExprContext, loc: QExprId) -> EResult<Rc<dyn Value>> {
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

impl Value for ValuePendingImport {
    fn as_exporter_any(&self) -> Rc<dyn crate::artifact::ExporterAny> {
        unreachable!();
    }

    fn type_(&self, xx: &ExprContext, loc: QExprId) -> EResult<Rc<dyn Type>> {
        let mut v = self.resolve(xx, loc);
        let mut errors = None;
        v.push_errors(&mut errors);
        let Some(v) = v.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let mut t = v.type_(xx, loc);
        let Some(t) = t.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        t.push_errors(&mut errors);
        return EResult {
            errors,
            v: Some(t),
        };
    }
}
