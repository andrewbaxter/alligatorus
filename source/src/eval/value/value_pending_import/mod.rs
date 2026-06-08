use {
    crate::{
        eval::TargetDynamic,
        langhelp::{
            CompileImportContext,
            CompileResResult,
            CompileResultEntry,
            EvalContext,
            ExprContext,
            GlobalContext,
            ImportStateId,
            QExprId,
            Scope,
            Type,
            Value,
        },
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

pub(crate) struct PendingImportState {
    pub entry: Arc<CompileResultEntry>,
    pub gx: Arc<GlobalContext>,
}

impl PendingImportState {
    pub(crate) fn resolve(&self, xx: &ExprContext) -> Rc<dyn Value> {
        let guard = self.entry.result.read().unwrap();
        let compile_res = guard.as_ref().unwrap();
        let compile_res = compile_res.as_ref().expect("Import compilation error");
        let artifact = match &compile_res.result {
            CompileResResult::Ok(a) => a,
            CompileResResult::Err(e) => panic!("Import compilation failed: {:?}", e),
        };
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        self.gx.deserialize_artifact(xx, loc, artifact).expect("Artifact deserialization failed")
    }

    pub(crate) fn resolve_standalone(&self) -> Rc<dyn Value> {
        let cix = Rc::new(CompileImportContext {
            gx: self.gx.clone(),
            compile_path: vec![],
            builtin_types: crate::langhelp::make_builtin_types(),
        });
        let ex = Rc::new(EvalContext {
            ix: cix,
            next_local_id: Default::default(),
            definition_sets: RefCell::new(Vec::new()),
            externals: RefCell::new(Vec::new()),
        });
        let xx = ExprContext {
            vx: ex,
            import_state: Rc::new(Cell::new(ImportStateId(0))),
            scope: Rc::new(Scope {
                values: RefCell::new(HashMap::new()),
                parent: None,
            }),
            target: Rc::new(TargetDynamic),
        };
        self.resolve(&xx)
    }
}

pub(crate) struct ValuePendingImport {
    pub resolved: RefCell<Option<Rc<dyn Value>>>,
    pub state: Rc<PendingImportState>,
}

impl ValuePendingImport {
    pub(crate) fn resolve(&self, xx: &ExprContext) -> Rc<dyn Value> {
        let mut cached = self.resolved.borrow_mut();
        if let Some(v) = cached.as_ref() {
            return v.clone();
        }
        let v = self.state.resolve(xx);
        *cached = Some(v.clone());
        v
    }
}

impl Value for ValuePendingImport {
    fn type_(&self, xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_pending_import::TypePendingImport {
            state: self.state.clone(),
            resolved: self.resolve(xx),
        })
    }
}
