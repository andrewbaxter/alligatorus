use {
    crate::{
        eval::{
            TargetDynamic,
            artifact::deserialize_artifact,
            value::{
                type_source_data_type::artifact::ArtifactDataDefSetDataType,
                type_source_interface::BuiltinInterfaceKind,
            },
        },
        eval::artifact::ArtifactId,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            CompileImportContext,
            CompileResResult,
            CompileResultEntry,
            Counter,
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

pub(crate) struct PendingImportState {
    pub entry: Arc<CompileResultEntry>,
    pub gx: Arc<GlobalContext>,
}

impl PendingImportState {
    pub(crate) fn imported_needs_v8_bundle(&self) -> bool {
        let guard = self.entry.result.read().unwrap();
        let compile_res = match guard.as_ref().and_then(|r| r.as_ref().ok()) {
            Some(c) => c,
            None => return false,
        };
        let artifact = match &compile_res.result {
            CompileResResult::Ok(a) => a,
            _ => return false,
        };
        let dt_artifact_id = match &artifact.deserializer {
            ArtifactId::Source(_) => artifact.deserializer.clone(),
            _ => return false,
        };
        let dt_artifact = match self.gx.artifacts.get(dt_artifact_id) {
            Ok(a) => a,
            Err(_) => return false,
        };
        let dt_serial: Result<ArtifactDataDefSetDataType, _> = serde_json::from_value(dt_artifact.data);
        match dt_serial {
            Ok(dt) => dt.members.iter().any(|m_id| {
                let m_art = match self.gx.artifacts.get(m_id.clone()) {
                    Ok(a) => a,
                    Err(_) => return false,
                };
                let is_iface_impl =
                    matches!(
                        &m_art.deserializer,
                        crate:: eval:: artifact:: ArtifactId:: Builtin(
                            b
                        ) if * b == crate:: r#gen:: defset_iface_impl_artifact_id()
                    );
                if !is_iface_impl {
                    return false;
                }
                let impl_serial: crate::eval::value::type_source_data_type::artifact::ArtifactDataDefSetInterfaceImpl =
                    match serde_json::from_value(m_art.data) {
                        Ok(s) => s,
                        Err(_) => return false,
                    };
                impl_serial.builtin_kind == Some(BuiltinInterfaceKind::Type)
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
        let loc = (SourceId(0), ExprId(0));
        let cv = deserialize_artifact(xx, loc, artifact).into_res().expect("Artifact deserialization failed");
        let mut all_finalizer_codes: Vec<Code> = Vec::new();
        loop {
            let finalizers = std::mem::take(&mut *xx.vx.ix.pending_finalizers.borrow_mut());
            if finalizers.is_empty() {
                break;
            }
            for f in finalizers {
                let codes = f(xx, loc).expect("Finalizer failed");
                all_finalizer_codes.extend(codes.into_iter().filter(|c| !matches!(c.code, Code_::None)));
            }
        }
        let mut all_codes = all_finalizer_codes;
        let cv_code_is_none = matches!(cv.code.code, Code_::None);
        if !cv_code_is_none {
            all_codes.push(cv.code);
        }
        if all_codes.is_empty() {
            CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::None,
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
                xx.target.seq(loc, all_codes).into_res().expect("Failed to combine finalizer codes");
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
            builtin_artifacts: make_builtin_deserializers(),
            deserialized_artifacts: std::cell::RefCell::new(std::collections::HashMap::new()),
            pending_finalizers: std::cell::RefCell::new(Vec::new()),
            isolate: Rc::new(std::cell::RefCell::new(v8::Isolate::new(Default::default()))),
            import_state: ImportStateId(0),
            installed_def_sets: RefCell::new(std::collections::HashSet::new()),
            def_set_counter: Counter::default(),
            v8_context: RefCell::new(None),
            builtins_registered: Cell::new(false),
            source_id: SourceId(0),
            builtin_scope_entries: RefCell::new(Vec::new()),
        });
        let ex = Rc::new(EvalContext {
            ix: cix,
            next_local_id: Default::default(),
            externals: RefCell::new(Vec::new()),
            v8_scope: std::cell::Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
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
