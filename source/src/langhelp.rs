use {
    crate::syntax::{
        ExprId,
        SourceModule,
        SourceSpecAbsolute,
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
        collections::HashMap,
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

pub(crate) trait Aspect: Any { }

pub(crate) trait CodeDyn: Any { }

pub(crate) trait ScopeEntry {
    fn use_(&self, loc: QExprId, code: Code) -> CodeValue;
}

pub(crate) trait Sources: Send + Sync {
    fn get(&self, spec: SourceSpecAbsolute) -> Result<(SourceId, SourceModule), String>;
}

pub(crate) trait Type: Value {
    fn as_value(&self) -> Rc<dyn Value>;

    /// Default `Type` doesn't know how to clone itself; concrete impls return
    /// `Rc::new(Self)` (or clone any inner data). Only needed when a caller
    /// holds `&dyn Type` and must produce an owned `Rc<dyn Type>`. Returning
    /// `None` here keeps the default cheap and signals "type does not support
    /// being held as `Rc<dyn Type>` outside its original construction site."
    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        None
    }

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
    fn desc(&self) -> String;

    /// Serialize this type itself as an artifact (for type references).
    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Option<Artifact> {
        None
    }
}

pub(crate) trait Value: Any {
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

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactIdBuiltin {
    pub file: String,
    pub line: usize,
}

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

/// In the first pass (meta evaluation) this bundles a Value (static meta about a
/// runtime value) with the Code that produces the value.
///
/// The code results in a value described by Value (i.e., in a stack target the top
/// stack element would be the value), but may contain other preceding side
/// effects, so even if the value isn't used all code must be kept.
pub(crate) struct CodeValue {
    pub(crate) code: Code,
    pub(crate) value: Rc<dyn Value>,
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Copy, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct CompileId(pub ImportStateId, pub SourceId);

/// One per module (per import env).
pub(crate) struct CompileImportContext {
    pub(crate) builtin_types: HashMap<ArtifactIdBuiltin, Rc<dyn Type>>,
    pub(crate) compile_path: Vec<(CompileId, ExprId)>,
    pub(crate) deserialized_artifacts: RefCell<HashMap<ArtifactId, Box<dyn std::any::Any>>>,
    pub(crate) gx: Arc<GlobalContext>,
    pub(crate) pending_finalizers: RefCell<Vec<ArtifactFinalizer>>,
    /// Shared v8 isolate for all compile stages — one isolate per
    /// `compile_importid`. Persistent handles (`v8::Global<...>`) captured
    /// during one stage's lift remain valid in later stages, which is
    /// essential for `IsolateBundle` to keep a JS instance handle alive
    /// across stages.
    pub(crate) isolate: Rc<RefCell<v8::OwnedIsolate>>,
    /// Import id for this compile. Stable across all stages.
    pub(crate) import_state: ImportStateId,
    /// Definition sets created via `define()`. Persists across all stages so
    /// sets from earlier stages remain registered (their v8 closures live on
    /// the shared `v8_context` global) and visible to later stages.
    pub(crate) definition_sets: RefCell<Vec<dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>>>,
    /// Monotonic per-compile counter for allocating fresh `DefSetId`s. Never
    /// resets between stages so `(import_state, def_set_id)` is globally
    /// unique within this compile.
    pub(crate) def_set_counter: Counter,
    /// Persistent v8 context shared by all stages. The global object accumulates
    /// builtin closures (registered once) and def-set member closures (registered
    /// in the stage that defines them) so later stages see them without
    /// re-registration.
    pub(crate) v8_context: RefCell<Option<v8::Global<v8::Context>>>,
    /// True once builtins have been registered on `v8_context`'s global.
    pub(crate) builtins_registered: Cell<bool>,
    /// Source id of the module being compiled at this level. Used to derive
    /// the `CompileId` for cross-compile lookups.
    pub(crate) source_id: crate::langhelp::SourceId,
    /// Builtin scope entries `(name, scope_entry_id, type)` populated at
    /// the start of each stage's `eval()`. Read by import-side `from_serial`
    /// when re-compiling an imported data type's interface impls — it needs
    /// the same builtin scope (`define`, `eval_const`, type refs, etc.) the
    /// originating compile saw, so the user's `impl_expr` resolves the same
    /// scope references.
    pub(crate) builtin_scope_entries: RefCell<Vec<(String, ScopeEntryId, std::rc::Rc<dyn Type>)>>,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct CompileRes {
    pub(crate) generated_mortar_scripts: Vec<String>,
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
    /// Deserialize from a canonical JSON string.
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

    /// Serialize to a canonical JSON string (for persistence/wire format).
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
pub(crate) struct DefSetId(pub usize);

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
            return Ok(crate::invariant::must_downcast_ref::<T>(cached.as_ref()).clone());
        }
    }
    let (shell, finalizer) = create()?;
    ix.deserialized_artifacts.borrow_mut().insert(id.clone(), Box::new(shell.clone()));
    ix.pending_finalizers.borrow_mut().push(finalizer);
    Ok(shell)
}

/// Downcast an `Rc<dyn Value>` to `Rc<T>`.
///
/// This is safe because we verify the concrete type via `Any` before performing
/// the pointer cast. The type guarantee comes from the type checker (cast_to /
/// cast_from).
pub(crate) fn downcast_value<T: Value>(v: Rc<dyn Value>) -> Rc<T> {
    assert!((v.as_ref() as &dyn Any).is::<T>(), "downcast_value: expected {}", std::any::type_name::<T>());
    let raw = Rc::into_raw(v) as *const T;
    unsafe {
        Rc::from_raw(raw)
    }
}

/// State table:
///
/// * v empty, errors non-empty: error
///
/// * v non-empty, errors empty
///
/// * v non-empty, errors non-empty
///
/// If `v` is empty this is an error result, and `errors` must be non-empty.
pub(crate) struct EResult<T> {
    /// Various errors, either causing the current evaluation to fail or from side
    /// effects or prior statements. When considering alternative evaluation paths,
    /// errors may be discarded, but the errors from one branch will always be kept.
    /// Errors are accumulated into parent EvalRes's and propagated upward. When root
    /// evaluation has at least one error, compilation fails.
    ///
    /// If `Some`, the result is in an error state (even if the list is empty).
    pub(crate) errors: Option<Vec<ExprErr>>,
    pub(crate) v: Option<T>,
}

/// Multiple evaluations per module.
pub(crate) struct EvalContext {
    pub(crate) externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub(crate) ix: Rc<CompileImportContext>,
    pub(crate) next_local_id: Counter,
    /// Raw pointer to the active v8 scope. `eval()` installs it at the top
    /// of the function (after creating `dynamic_scope`) and clears it just
    /// before returning. Stored as a lifetime-erased `NonNull` so it can
    /// sit in `EvalContext` (which the borrow checker treats as `'static`).
    /// Re-borrowed in `V8CastDispatcher::dispatch_cast_from` and other v8
    /// dispatch points; soundness depends on no two re-borrows being live
    /// at once and on the underlying scope outliving every read.
    pub(crate) v8_scope: std::cell::Cell<
        Option<std::ptr::NonNull<v8::PinnedRef<'static, v8::HandleScope<'static, v8::Context>>>>,
    >,
    /// Trait-object pointer to a dispatcher for v8-side type interface
    /// methods (currently `cast_from`). `eval()` constructs a unit impl
    /// and installs it here for the duration of `eval()`. The dispatcher
    /// itself stateless — it reads the active scope from `v8_scope` on
    /// each call, so its lifetime is decoupled from the v8 scope's borrow.
    pub(crate) v8_dispatcher: std::cell::Cell<Option<std::ptr::NonNull<dyn crate::eval::V8CastDispatcher>>>,
}

/// If None, it means that the evaluation failed in a way that affects downstream
/// meta evaluation, so no Value could be provided.
pub(crate) type EvalRes = EResult<CodeValue>;

/// Per expression
pub(crate) struct ExprContext {
    pub(crate) import_state: Rc<Cell<ImportStateId>>,
    pub(crate) scope: Rc<Scope>,
    pub(crate) target: Rc<dyn crate::eval::Target>,
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

pub(crate) enum External {
    StagedExprCell(Rc<crate::eval::expr::StagedExprCell>),
    StageLiftCell(Rc<crate::eval::StageLiftCell>),
    Type(Rc<dyn Type>),
}

pub(crate) struct GlobalContext {
    pub(crate) artifacts: Box<dyn Artifacts>,
    pub(crate) compile_results: Mutex<HashMap<CompileId, Arc<CompileResultEntry>>>,
    pub(crate) generated_mortar_scripts: Mutex<Vec<String>>,
    pub(crate) sources: Box<dyn Sources>,
}

impl GlobalContext {
    pub(crate) fn new(sources: Box<dyn Sources>, artifacts: Box<dyn Artifacts>) -> Arc<GlobalContext> {
        Arc::new(GlobalContext {
            sources,
            artifacts,
            generated_mortar_scripts: Default::default(),
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

pub(crate) fn make_builtin_types() -> HashMap<ArtifactIdBuiltin, Rc<dyn Type>> {
    use crate::eval::{
        value::type_null::dynamic::TypeNullDynamic,
        value::type_string::dynamic::TypeStringDynamic,
        value::type_null::r#const::TypeNullConst,
        value::type_string::r#const::TypeStringConst,
        value::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
        value::type_type_source_data_type_constructor::dynamic::TypeTypeSourceDataTypeConstructorDynamic,
    };

    let mut m: HashMap<ArtifactIdBuiltin, Rc<dyn Type>> = HashMap::new();
    TypeNullConst::register(&mut m);
    TypeStringConst::register(&mut m);
    TypeNullDynamic::register(&mut m);
    TypeStringDynamic::register(&mut m);
    TypeTypeSourceDataTypeDynamic::register(&mut m);
    TypeTypeSourceDataTypeConstructorDynamic::register(&mut m);
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
    /// Builtin/static functions - deterministic indices
    Builtin(usize),
    /// Definition set entry - deterministic
    Def(ImportStateId, DefSetId, usize),
    /// Local bindings within an evaluation
    Local(usize),
}

/// World id for this import spec
#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct SourceId(pub usize);

/// A value guaranteed to be unique in the world
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub(crate) enum Unique {
    Builtin {
        file: String,
        line: usize,
    },
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
