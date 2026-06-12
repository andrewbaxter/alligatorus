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
    /// True if the imported value is an instance of a data type that
    /// implements the builtin `type` interface — those imports must be
    /// resolved against the importing compile's `ExprContext` (we need its
    /// v8 scope + `builtin_scope_entries` to re-compile the interface impls
    /// and install the bundle).
    pub(crate) fn imported_needs_v8_bundle(&self) -> bool {
        let guard = self.entry.result.read().unwrap();
        let compile_res = match guard.as_ref().and_then(|r| r.as_ref().ok()) {
            Some(c) => c,
            None => return false,
        };
        let artifact = match &compile_res.result {
            crate::langhelp::CompileResResult::Ok(a) => a,
            _ => return false,
        };
        let dt_artifact_id = match &artifact.type_ {
            crate::langhelp::ArtifactId::Source(_) => artifact.type_.clone(),
            _ => return false,
        };
        let dt_artifact = match self.gx.artifacts.get(dt_artifact_id) {
            Ok(a) => a,
            Err(_) => return false,
        };
        let dt_serial: Result<
            crate::eval::value::type_source_data_type::artifact::ArtifactDataDefSetDataType,
            _,
        > = serde_json::from_value(dt_artifact.data);
        match dt_serial {
            Ok(dt) => dt.interface_impls.iter().any(|i| {
                i.builtin_kind == crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type
            }),
            Err(_) => false,
        }
    }

    pub(crate) fn resolve(&self, xx: &ExprContext) -> CodeValue {
        let guard = self.entry.result.read().unwrap();
        let compile_res = guard.as_ref().unwrap();
        let compile_res = compile_res.as_ref().expect("Import compilation error");
        let artifact = match &compile_res.result {
            CompileResResult::Ok(a) => a,
            CompileResResult::Err(e) => panic!("Import compilation failed: {:?}", e),
        };
        let loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        let cv =
            crate::eval::util::deserialize_artifact(xx, loc, artifact)
                .into_res()
                .expect("Artifact deserialization failed");
        let mut all_finalizer_codes: Vec<Code> = Vec::new();
        loop {
            let finalizers = std::mem::take(&mut *xx.vx.ix.pending_finalizers.borrow_mut());
            if finalizers.is_empty() {
                break;
            }
            for f in finalizers {
                let codes = f(xx, loc).expect("Finalizer failed");
                all_finalizer_codes.extend(
                    codes.into_iter().filter(|c| !matches!(c.code, crate::langhelp::Code_::None)),
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
            let combined_code =
                xx.target.block(loc, all_codes).into_res().expect("Failed to combine finalizer codes");
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
            builtin_deserializers: crate::langhelp::make_builtin_deserializers(),
            deserialized_artifacts: std::cell::RefCell::new(std::collections::HashMap::new()),
            pending_finalizers: std::cell::RefCell::new(Vec::new()),
            isolate: Rc::new(std::cell::RefCell::new(v8::Isolate::new(Default::default()))),
            import_state: ImportStateId(0),
            definition_sets: RefCell::new(Vec::new()),
            def_set_counter: crate::langhelp::Counter::default(),
            v8_context: RefCell::new(None),
            builtins_registered: Cell::new(false),
            source_id: crate::langhelp::SourceId(0),
            builtin_scope_entries: RefCell::new(Vec::new()),
        });
        let ex = Rc::new(EvalContext {
            ix: cix,
            next_local_id: Default::default(),
            externals: RefCell::new(Vec::new()),
                v8_scope: std::cell::Cell::new(None),
                v8_dispatcher: std::cell::Cell::new(None),
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
