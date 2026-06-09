use {
    crate::{
        eval::TargetDynamic,
        langhelp::{
            Code,
            CodeValue,
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
    pub(crate) fn resolve(&self, xx: &ExprContext) -> CodeValue {
        let guard = self.entry.result.read().unwrap();
        let compile_res = guard.as_ref().unwrap();
        let compile_res = compile_res.as_ref().expect("Import compilation error");
        let artifact = match &compile_res.result {
            CompileResResult::Ok(a) => a,
            CompileResResult::Err(e) => panic!("Import compilation failed: {:?}", e),
        };
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        let cv = crate::eval::util::deserialize_artifact(xx, loc, artifact)
            .into_res()
            .expect("Artifact deserialization failed");
        let mut all_finalizer_codes: Vec<Code> = Vec::new();
        loop {
            let finalizers =
                std::mem::take(&mut *xx.vx.ix.pending_finalizers.borrow_mut());
            if finalizers.is_empty() {
                break;
            }
            for f in finalizers {
                let codes = f(xx, loc).expect("Finalizer failed");
                all_finalizer_codes.extend(
                    codes
                        .into_iter()
                        .filter(|c| !matches!(c.code, crate::langhelp::Code_::None)),
                );
            }
        }
        let mut all_codes = all_finalizer_codes;
        let cv_code_is_none = matches!(cv.code.code, crate::langhelp::Code_::None);
        if !cv_code_is_none {
            all_codes.push(cv.code);
        }
        if all_codes.is_empty() {
            CodeValue {
                code: Code {
                    loc: loc,
                    code: crate::langhelp::Code_::None,
                },
                value: cv.value,
            }
        } else if all_codes.len() == 1 {
            CodeValue {
                code: all_codes.pop().unwrap(),
                value: cv.value,
            }
        } else {
            let combined_code = xx
                .target
                .block(loc, all_codes)
                .into_res()
                .expect("Failed to combine finalizer codes");
            CodeValue {
                code: combined_code,
                value: cv.value,
            }
        }
    }

    pub(crate) fn resolve_standalone(&self) -> CodeValue {
        let cix = Rc::new(CompileImportContext {
            gx: self.gx.clone(),
            compile_path: vec![],
            builtin_types: crate::langhelp::make_builtin_types(),
            deserialized_artifacts: std::cell::RefCell::new(std::collections::HashMap::new()),
            pending_finalizers: std::cell::RefCell::new(Vec::new()),
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
    pub resolved: RefCell<Option<(Code, Rc<dyn Value>)>>,
    pub state: Rc<PendingImportState>,
}

impl ValuePendingImport {
    pub(crate) fn resolve(&self, xx: &ExprContext) -> CodeValue {
        let mut cached = self.resolved.borrow_mut();
        if let Some((code, value)) = cached.as_ref() {
            return CodeValue {
                code: code.clone(),
                value: value.clone(),
            };
        }
        let cv = self.state.resolve(xx);
        *cached = Some((cv.code.clone(), cv.value.clone()));
        cv
    }
}

impl Value for ValuePendingImport {
    fn type_(&self, xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        let cv = self.resolve(xx);
        Rc::new(super::type_pending_import::TypePendingImport {
            state: self.state.clone(),
            resolved: cv.value,
        })
    }

}
