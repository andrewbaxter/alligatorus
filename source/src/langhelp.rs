use {
    crate::{
        builtin::ArtifactBuiltinDeserializer,
        eval::{
            StageLiftCell,
            code_value_extern,
            definition_set::DefinitionSet,
            expr::StagedExprCell,
            value::{
                record,
                tuple,
                type_any,
                type_expr,
                type_list,
                type_map,
                type_null,
                type_number,
                type_option,
                type_source_data_type,
                type_source_function,
                type_source_interface,
                type_source_tagged_union,
                type_string,
                type_type_source_data_type,
                type_type_source_data_type_constructor,
            },
        },
        invariant::must_downcast_ref,
        r#gen::{
            ScopeEntry,
            Target,
        },
        syntax::{
            ExprId,
            SourceModule,
            SourceSpecAbsolute,
        },
    },
    serde::{
        Deserialize,
        Serialize,
    },
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        collections::{
            HashMap,
            HashSet,
        },
        rc::Rc,
        sync::{
            Arc,
            Mutex,
            RwLock,
        },
    },
};

pub(crate) trait Artifacts: Send + Sync {
    fn get(&self, id: ArtifactId) -> Result<Artifact, String>;
    fn reserve_id(&self) -> ArtifactId;
    fn set(&self, id: ArtifactId, data: Artifact);
}

pub(crate) trait Aspect: Any {
    fn v8_instance(&self) -> Option<v8::Global<v8::Object>> {
        None
    }
}

pub(crate) trait CodeDyn: Any { }

pub(crate) trait Sources: Send + Sync {
    fn get(&self, spec: SourceSpecAbsolute) -> Result<(SourceId, SourceModule), String>;
}

pub(crate) trait Type: Value {
    fn as_value(&self) -> Rc<dyn Value>;

    fn aspect(&self, _xx: &ExprContext, _key: &Unique) -> Option<Box<dyn Any>> {
        return None;
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        false
    }

    fn cast_from(&self, _xx: &ExprContext, _loc: QExprId, _cv: CodeValue) -> EvalRes {
        unimplemented!();
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        unimplemented!();
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        None
    }
    fn desc(&self) -> String;

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Artifact {
        panic!("type_to_serial not implemented for {}", self.desc())
    }
}

pub(crate) trait Value: Any {
    fn lift(&self, _xx: &ExprContext) -> Result<(), Vec<ExprErr>> {
        Ok(())
    }
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type>;
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct Artifact {
    pub(crate) data: serde_json::Value,
    pub(crate) type_: ArtifactId,
}

pub(crate) type ArtifactFinalizer = Box<dyn FnOnce(&ExprContext, QExprId) -> Result<Vec<Code>, String>>;

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum ArtifactId {
    Builtin(ArtifactIdBuiltin),
    Source(ArtifactIdSource),
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(transparent)]
pub(crate) struct ArtifactIdBuiltin(pub usize);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactIdSource(pub CompileId, pub usize);

#[derive(Clone)]
pub(crate) struct Code {
    pub(crate) code: Code_,
    pub(crate) loc: QExprId,
}

impl Code {
    pub(crate) fn cast<T: CodeDyn>(&self) -> EResult<Option<&T>> {
        match &self.code {
            Code_::None => {
                return EResult::ok(None);
            },
            Code_::Dyn(c) => match <dyn Any>::downcast_ref::<T>(c.as_ref()) {
                Some(c) => {
                    return EResult::ok(Some(c));
                },
                None => {
                    return EResult::err(self.loc, format!("TODO code is not the expected type"));
                },
            },
        }
    }
}

#[derive(Clone)]
pub(crate) enum Code_ {
    Dyn(Rc<dyn CodeDyn>),
    None,
}

#[derive(Clone)]
pub(crate) struct CodeValue {
    pub(crate) code: Code,
    pub(crate) value: Rc<dyn Value>,
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Copy, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct CompileId(pub ImportStateId, pub SourceId);

pub(crate) struct CompileImportContext {
    pub(crate) builtin_deserializers: HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>,
    pub(crate) builtin_scope_entries: RefCell<Vec<(String, ScopeEntryId, std::rc::Rc<dyn Type>)>>,
    pub(crate) builtins_registered: Cell<bool>,
    pub(crate) compile_path: Vec<(CompileId, ExprId)>,
    pub(crate) def_set_counter: Counter,
    pub(crate) deserialized_artifacts: RefCell<HashMap<ArtifactId, Box<dyn std::any::Any>>>,
    pub(crate) gx: Arc<GlobalContext>,
    pub(crate) import_state: ImportStateId,
    pub(crate) installed_def_sets: RefCell<HashSet<DefSetId>>,
    pub(crate) isolate: Rc<RefCell<v8::OwnedIsolate>>,
    pub(crate) pending_finalizers: RefCell<Vec<ArtifactFinalizer>>,
    pub(crate) source_id: SourceId,
    pub(crate) v8_context: RefCell<Option<v8::Global<v8::Context>>>,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct CompileRes {
    pub(crate) generated_dynamic_scripts: Vec<String>,
    pub(crate) logs: Vec<LogEntry>,
    pub(crate) result: CompileResResult,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum CompileResResult {
    Err(Vec<ExprErr>),
    Ok(Artifact),
}

pub(crate) struct CompileResultEntry {
    pub(crate) result: RwLock<Option<Result<CompileRes, ExprSubErr>>>,
}

#[derive(PartialEq, Eq, PartialOrd, Ord, Hash, Clone, Debug)]
pub(crate) enum ConstKey {
    String(String),
    Usize(usize),
}

impl ConstKey {
    pub(crate) fn from_str(s: &str) -> Result<ConstKey, String> {
        let json: serde_json::Value =
            serde_json::from_str(s).map_err(|e| format!("Invalid ConstKey string: {}", e))?;
        match json {
            serde_json::Value::String(s) => Ok(ConstKey::String(s)),
            serde_json::Value::Number(n) => {
                if let Some(u) = n.as_u64() {
                    Ok(ConstKey::Usize(u as usize))
                } else {
                    Err(format!("Unsupported ConstKey number: {}", n))
                }
            },
            _ => Err(format!("Unsupported ConstKey JSON value: {}", s)),
        }
    }

    pub(crate) fn to_str(&self) -> String {
        let json = match self {
            ConstKey::String(s) => serde_json::Value::String(s.clone()),
            ConstKey::Usize(n) => serde_json::Value::Number((*n).into()),
        };
        serde_json_canonicalizer::to_string(&json).unwrap()
    }
}

impl<'de> Deserialize<'de> for ConstKey {
    fn deserialize<D: serde::Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        let s = String::deserialize(deserializer)?;
        ConstKey::from_str(&s).map_err(serde::de::Error::custom)
    }
}

impl Serialize for ConstKey {
    fn serialize<S: serde::Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        serializer.serialize_str(&self.to_str())
    }
}

#[derive(Default)]
pub(crate) struct Counter(Cell<usize>);

impl Counter {
    pub(crate) fn next(&self) -> usize {
        let out = self.0.get();
        self.0.set(out + 1);
        return out;
    }
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct DefSetId(pub CompileId, pub usize);

pub(crate) fn deserialize_independent_artifact<
    T: Clone + 'static,
>(
    ix: &CompileImportContext,
    id: &ArtifactId,
    create: impl FnOnce() -> Result<(T, ArtifactFinalizer), String>,
) -> Result<T, String> {
    {
        let cache = ix.deserialized_artifacts.borrow();
        if let Some(cached) = cache.get(id) {
            return Ok(must_downcast_ref::<T>(cached.as_ref()).clone());
        }
    }
    let (shell, finalizer) = create()?;
    ix.deserialized_artifacts.borrow_mut().insert(id.clone(), Box::new(shell.clone()));
    ix.pending_finalizers.borrow_mut().push(finalizer);
    Ok(shell)
}

pub(crate) fn downcast_value<T: Value>(v: Rc<dyn Value>) -> Rc<T> {
    assert!((v.as_ref() as &dyn Any).is::<T>(), "downcast_value: expected {}", std::any::type_name::<T>());
    let raw = Rc::into_raw(v) as *const T;
    unsafe {
        Rc::from_raw(raw)
    }
}

pub(crate) struct EResult<T> {
    pub(crate) errors: Option<Vec<ExprErr>>,
    pub(crate) v: Option<T>,
}

pub(crate) struct EvalContext {
    pub(crate) definition_sets: RefCell<HashMap<DefSetId, dumpster::unsync::Gc<DefinitionSet>>>,
    pub(crate) externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub(crate) ix: Rc<CompileImportContext>,
    pub(crate) next_local_id: Counter,
    pub(crate) v8_scope: std
    ::cell
    ::Cell<Option<std::ptr::NonNull<v8::PinnedRef<'static, v8::HandleScope<'static, v8::Context>>>>>,
}

pub(crate) type EvalRes = EResult<CodeValue>;

pub(crate) struct ExprContext {
    pub(crate) import_state: Rc<Cell<ImportStateId>>,
    pub(crate) scope: Rc<Scope>,
    pub(crate) target: Rc<dyn Target>,
    pub(crate) vx: Rc<EvalContext>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ExprErr {
    pub(crate) errors: Vec<ExprSubErr>,
    pub(crate) loc: QExprId,
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum ExprSubErr {
    Opaque(String),
}

#[derive(Clone)]
pub(crate) enum External {
    CodeValueExternCell(Rc<std::cell::RefCell<Option<CodeValue>>>),
    StagedExprCell(Rc<StagedExprCell>),
    StageLiftCell(Rc<StageLiftCell>),
    Type(Rc<dyn Type>),
    Unique(Unique),
}

pub(crate) struct GlobalContext {
    pub(crate) artifacts: Box<dyn Artifacts>,
    pub(crate) compile_results: Mutex<HashMap<CompileId, Arc<CompileResultEntry>>>,
    pub(crate) generated_dynamic_scripts: Mutex<Vec<String>>,
    pub(crate) sources: Box<dyn Sources>,
}

impl GlobalContext {
    pub(crate) fn new(sources: Box<dyn Sources>, artifacts: Box<dyn Artifacts>) -> Arc<GlobalContext> {
        Arc::new(GlobalContext {
            sources,
            artifacts,
            generated_dynamic_scripts: Default::default(),
            compile_results: Default::default(),
        })
    }
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ImportStateId(pub usize);

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct LogEntry {
    pub(crate) message: String,
    pub(crate) timestamp_ms: u64,
}

pub(crate) fn make_builtin_deserializers() -> HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>> {
    let mut m: HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>> = HashMap::new();
    type_null::artifact::register(&mut m);
    type_string::artifact::register(&mut m);
    type_number::artifact::register(&mut m);
    type_type_source_data_type::artifact::register(&mut m);
    type_type_source_data_type_constructor::artifact::register(&mut m);
    type_source_function::artifact::register(&mut m);
    record::artifact::register(&mut m);
    tuple::artifact::register(&mut m);
    type_list::artifact::register(&mut m);
    type_map::artifact::register(&mut m);
    type_option::artifact::register(&mut m);
    type_source_data_type::artifact::register(&mut m);
    type_source_tagged_union::artifact::register(&mut m);
    type_any::artifact::register(&mut m);
    type_source_interface::artifact::register(&mut m);
    type_expr::register_expr_artifact(&mut m);
    code_value_extern::register_artifact_deserializer(&mut m);
    m
}

pub(crate) fn produce_independent_artifact(
    artifacts: &dyn Artifacts,
    cell: &std::cell::RefCell<Option<ArtifactId>>,
    produce: impl FnOnce() -> Artifact,
) -> ArtifactId {
    if let Some(existing_id) = cell.borrow().clone() {
        return existing_id;
    }
    let id = artifacts.reserve_id();
    *cell.borrow_mut() = Some(id.clone());
    let artifact = produce();
    artifacts.set(id.clone(), artifact);
    id
}

pub(crate) type QExprId = (SourceId, ExprId);

pub(crate) struct Scope {
    pub(crate) parent: Option<Rc<Scope>>,
    pub(crate) values: RefCell<HashMap<ConstKey, Box<dyn ScopeEntry>>>,
}

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
pub(crate) enum ScopeEntryId {
    Builtin(usize),
    Def(DefSetId, usize),
    Local(usize),
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct SourceId(pub usize);

#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub(crate) enum Unique {
    Builtin(usize),
    Source(CompileId, usize),
}

impl<T> EResult<T> {
    pub(crate) fn err(loc: QExprId, e: String) -> Self {
        return Self {
            v: None,
            errors: Some(vec![ExprErr {
                loc: loc,
                errors: vec![ExprSubErr::Opaque(e)],
            }]),
        };
    }

    pub(crate) fn into_res(self) -> Result<T, Vec<ExprErr>> {
        if self.errors.is_some() {
            return Err(self.errors.unwrap_or_default());
        } else {
            return Ok(self.v.unwrap());
        }
    }

    pub(crate) fn map<U>(self, f: impl FnOnce(T) -> U) -> EResult<U> {
        return EResult {
            v: self.v.map(f),
            errors: self.errors,
        };
    }

    pub(crate) fn ok(v: T) -> Self {
        return Self {
            v: Some(v),
            errors: None,
        };
    }
}
