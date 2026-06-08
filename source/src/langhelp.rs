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

pub trait Artifacts: Send + Sync {
    fn reserve_id(&self) -> ArtifactId;
    fn set(&self, id: ArtifactId, data: Artifact);
    fn get(&self, id: ArtifactId) -> Result<Artifact, String>;
}

pub(crate) trait Aspect: Any { }

pub(crate) trait CodeDyn: Any { }

pub(crate) trait ScopeEntry {
    fn use_(&self, loc: QExprId, code: Code) -> CodeValue;
}

pub trait Sources: Send + Sync {
    fn get(&self, spec: SourceSpecAbsolute) -> Result<(SourceId, SourceModule), String>;
}

pub(crate) trait Type: Value {
    fn desc(&self) -> String;
    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>>;
    fn as_value(&self) -> Rc<dyn Value>;

    fn can_cast_to(&self, _xx: &ExprContext, _loc: QExprId, _type_: &dyn Type) -> bool {
        false
    }
    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes;

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }
    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes;

    /// Serialize this type itself as an artifact (for type references).
    fn type_to_serial(&self, _xx: &ExprContext, _gx: &GlobalContext, _loc: QExprId) -> Option<Artifact> {
        None
    }
}

pub(crate) trait Value: Any {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type>;
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct Artifact {
    pub(crate) type_: ArtifactId,
    pub(crate) data: serde_json::Value,
}

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
    pub(crate) loc: QExprId,
    pub(crate) code: Code_,
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
    None,
    Dyn(Rc<dyn CodeDyn>),
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
    pub(crate) gx: Arc<GlobalContext>,
    pub(crate) compile_path: Vec<(CompileId, ExprId)>,
    pub(crate) builtin_types: HashMap<ArtifactIdBuiltin, Rc<dyn Type>>,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct CompileRes {
    pub(crate) logs: Vec<LogEntry>,
    pub(crate) result: CompileResResult,
    pub(crate) generated_mortar_scripts: Vec<String>,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum CompileResResult {
    Ok(Artifact),
    Err(Vec<ExprErr>),
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
    /// Serialize to a canonical JSON string (for persistence/wire format).
    pub(crate) fn to_str(&self) -> String {
        let json = match self {
            ConstKey::String(s) => serde_json::Value::String(s.clone()),
            ConstKey::Usize(n) => serde_json::Value::Number((*n).into()),
        };
        serde_json_canonicalizer::to_string(&json).unwrap()
    }

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

/// Downcast an `Rc<dyn Value>` to `Rc<T>`.
///
/// This is safe because we verify the concrete type via `Any` before performing
/// the pointer cast. The type guarantee comes from the type checker (cast_to /
/// cast_from).
pub(crate) fn downcast_value<T: Value>(v: Rc<dyn Value>) -> Option<Rc<T>> {
    if (v.as_ref() as &dyn Any).is::<T>() {
        let raw = Rc::into_raw(v) as *const T;
        Some(unsafe {
            Rc::from_raw(raw)
        })
    } else {
        None
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
    pub(crate) v: Option<T>,
    /// Various errors, either causing the current evaluation to fail or from side
    /// effects or prior statements. When considering alternative evaluation paths,
    /// errors may be discarded, but the errors from one branch will always be kept.
    /// Errors are accumulated into parent EvalRes's and propagated upward. When root
    /// evaluation has at least one error, compilation fails.
    ///
    /// If `Some`, the result is in an error state (even if the list is empty).
    pub(crate) errors: Option<Vec<ExprErr>>,
}

/// Multiple evaluations per module.
pub(crate) struct EvalContext {
    pub(crate) ix: Rc<CompileImportContext>,
    pub(crate) next_local_id: Counter,
    pub(crate) definition_sets: std
    ::cell
    ::RefCell<Vec<dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>>>,
    pub(crate) externals: RefCell<Vec<(ScopeEntryId, External)>>,
}

/// If None, it means that the evaluation failed in a way that affects downstream
/// meta evaluation, so no Value could be provided.
pub(crate) type EvalRes = EResult<CodeValue>;

/// Per expression
pub(crate) struct ExprContext {
    pub(crate) vx: Rc<EvalContext>,
    pub(crate) import_state: Rc<Cell<ImportStateId>>,
    pub(crate) scope: Rc<Scope>,
    pub(crate) target: Rc<dyn crate::eval::Target>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ExprErr {
    pub(crate) loc: QExprId,
    pub(crate) errors: Vec<ExprSubErr>,
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
    pub(crate) sources: Box<dyn Sources>,
    pub(crate) artifacts: Box<dyn Artifacts>,
    pub(crate) generated_mortar_scripts: Mutex<Vec<String>>,
    pub(crate) compile_results: Mutex<HashMap<CompileId, Arc<CompileResultEntry>>>,
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

    pub(crate) fn deserialize_artifact(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        artifact: &Artifact,
    ) -> Result<Rc<dyn Value>, String> {
        match &artifact.type_ {
            ArtifactId::Builtin(id) => {
                let type_ =
                    xx.vx.ix.builtin_types.get(id).ok_or_else(|| format!("Unknown builtin type ID: {:?}", id))?;
                let aspect = crate::builtin::aspect_artifact_get_maybe(xx, type_.as_ref()).ok_or_else(|| {
                    format!("Type ({}) missing AspectArtifact", type_.desc())
                })?;
                aspect.from_serial(xx, self, loc, &artifact.data)
            },
            ArtifactId::Source(compile_id) => {
                let type_artifact = self.artifacts.get(ArtifactId::Source(*compile_id))?;
                let ArtifactId::Builtin(tid) = &type_artifact.type_ else {
                    return Err("Type artifact must have builtin type_".to_string());
                };
                let type_type =
                    xx
                        .vx
                        .ix
                        .builtin_types
                        .get(tid)
                        .ok_or_else(|| format!("Unknown type artifact builtin ID: {:?}", tid))?;
                let aspect_type =
                    crate::builtin::aspect_type_get_maybe(
                        xx,
                        type_type.as_ref(),
                    ).ok_or_else(|| format!("Type type ({}) missing AspectType", type_type.desc()))?;
                let aspect_artifact =
                    crate::builtin::aspect_artifact_get_maybe(
                        xx,
                        type_type.as_ref(),
                    ).ok_or_else(|| format!("Type type ({}) missing AspectArtifact", type_type.desc()))?;
                let type_value = aspect_artifact.from_serial(xx, self, loc, &type_artifact.data)?;
                let actual_type = aspect_type.type_(type_value.as_ref());
                let aspect = crate::builtin::aspect_artifact_get_maybe(xx, actual_type.as_ref()).ok_or_else(|| {
                    format!("Reconstructed type ({}) missing AspectArtifact", actual_type.desc())
                })?;
                aspect.from_serial(xx, self, loc, &artifact.data)
            },
        }
    }

    /// Deserialize a type reference artifact into an Rc`<dyn Type>`. For builtins,
    /// looks up the type directly in builtin_types. For compiled types, reconstructs
    /// from the artifact store.
    pub(crate) fn deserialize_type_ref(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        artifact: &Artifact,
    ) -> Result<Rc<dyn Type>, String> {
        match &artifact.type_ {
            ArtifactId::Builtin(id) => {
                let type_ =
                    xx.vx.ix.builtin_types.get(id).ok_or_else(|| format!("Unknown builtin type ID: {:?}", id))?;
                Ok(type_.clone())
            },
            ArtifactId::Source(compile_id) => {
                let type_artifact = self.artifacts.get(ArtifactId::Source(*compile_id))?;
                let ArtifactId::Builtin(tid) = &type_artifact.type_ else {
                    return Err("Type artifact must have builtin type_".to_string());
                };
                let type_type =
                    xx
                        .vx
                        .ix
                        .builtin_types
                        .get(tid)
                        .ok_or_else(|| format!("Unknown type artifact builtin ID: {:?}", tid))?;
                let aspect_type =
                    crate::builtin::aspect_type_get_maybe(
                        xx,
                        type_type.as_ref(),
                    ).ok_or_else(|| format!("Type type ({}) missing AspectType", type_type.desc()))?;
                let aspect_artifact =
                    crate::builtin::aspect_artifact_get_maybe(
                        xx,
                        type_type.as_ref(),
                    ).ok_or_else(|| format!("Type type ({}) missing AspectArtifact", type_type.desc()))?;
                let type_value = aspect_artifact.from_serial(xx, self, loc, &type_artifact.data)?;
                Ok(aspect_type.type_(type_value.as_ref()))
            },
        }
    }
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ImportStateId(pub usize);

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct LogEntry {
    pub(crate) timestamp_ms: u64,
    pub(crate) message: String,
}

pub(crate) fn make_builtin_types() -> HashMap<ArtifactIdBuiltin, Rc<dyn Type>> {
    use crate::eval::{
        value::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
        value::type_type_source_data_type_constructor::dynamic::TypeTypeSourceDataTypeConstructorDynamic,
        value::type_null::dynamic::TypeNullDynamic,
        value::type_string::dynamic::TypeStringDynamic,
        value::type_null::r#const::TypeNullConst,
        value::type_string::r#const::TypeStringConst,
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

pub(crate) type QExprId = (SourceId, ExprId);

pub(crate) struct Scope {
    pub(crate) values: RefCell<HashMap<ConstKey, Box<dyn ScopeEntry>>>,
    pub(crate) parent: Option<Rc<Scope>>,
}

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
pub(crate) enum ScopeEntryId {
    /// Local bindings within an evaluation
    Local(usize),
    /// Builtin/static functions - deterministic indices
    Builtin(usize),
    /// Definition set entry - deterministic
    Def(ImportStateId, DefSetId, usize),
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
    pub(crate) fn ok(v: T) -> Self {
        return Self {
            v: Some(v),
            errors: None,
        };
    }

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
}
