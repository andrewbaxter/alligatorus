use {
    crate::{
        eval::{
            MortarTarget,
            value::value_any,
        },
        langhelp::{
            Code,
            CodeValue,
            CompileImportContext,
            CompileResResult,
            CompileResultEntry,
            EResult,
            EvalContext,
            ExprContext,
            GlobalContext,
            ImportStateId,
            QExprId,
            Scope,
            ScopeEntry,
            Type,
            Value,
        },
    },
    std::{
        any::Any,
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
            target: Rc::new(MortarTarget),
        };
        self.resolve(&xx)
    }
}

pub(crate) struct ValuePendingImport {
    pub state: Rc<PendingImportState>,
    pub resolved: RefCell<Option<Rc<dyn Value>>>,
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
        Rc::new(TypePendingImport {
            state: self.state.clone(),
            resolved: self.resolve(xx),
        })
    }
}

pub(crate) struct TypePendingImport {
    state: Rc<PendingImportState>,
    resolved: Rc<dyn Value>,
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

impl Value for TypePendingImport {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(value_any::r#const::TypeAnyConst)
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
        if *key == crate::builtin::aspect_bind_const_aspect_id() {
            return Some(
                Box::new(
                    Box::new(PendingImportBind { state: self.state.clone() }) as
                        Box<dyn crate::builtin::AspectBindConst>,
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

struct PendingImportBind {
    state: Rc<PendingImportState>,
}

impl crate::langhelp::Aspect for PendingImportBind { }

impl crate::builtin::AspectBindConst for PendingImportBind {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<Box<dyn ScopeEntry>> {
        EResult::ok(Box::new(ScopeEntryPendingImport {
            state: self.state.clone(),
            code: cv.code,
        }) as Box<dyn ScopeEntry>)
    }
}
pub mod scope_entry;

pub(crate) use scope_entry::ScopeEntryPendingImport;
