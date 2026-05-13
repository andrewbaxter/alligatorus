use {
    crate::syntax::{
        ExprId,
        SourceSpecAbsolute,
    },
    futures::future::{
        BoxFuture,
        Shared,
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
    v8::Local,
};

#[derive(Default)]
pub(crate) struct Counter(Cell<usize>);

impl Counter {
    pub(crate) fn next(&self) -> usize {
        let out = self.0.get();
        self.0.set(out + 1);
        return out;
    }
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ImportStateId(pub usize);

/// World id for this import spec
#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct SourceId(pub usize);

pub(crate) type QExprId = (SourceId, ExprId);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct CompileId(pub ImportStateId, pub SourceId);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum ExprSubErr {
    Opaque(String),
}

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash)]
pub(crate) struct ScopeEntryId(pub usize);

#[derive(Serialize, Deserialize, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum SerialValueSource {
    Builtin,
    Compile(CompileId),
}

pub(crate) struct ValueTypeAny {}

/// A value guaranteed to be unique in the world
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub(crate) struct Unique(pub SerialValueSource, pub usize);

impl Value for Unique {
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        todo!()
    }
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum SerialValue {
    Null,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum CompileRes {
    Ok(SerialValue),
    Err(Vec<ExprErr>),
}

pub(crate) struct GlobalContext {
    pub(crate) artifacts: RwLock<Artifacts>,
    pub(crate) cache_root: Vec<String>,
    // Remote packages/sources, resolved when downloaded (path is deterministic so not
    // returned, use a method to get the path)
    pub(crate) compile_results: Mutex<HashMap<CompileId, Shared<BoxFuture<'static, Result<CompileRes, ExprSubErr>>>>>,
    pub(crate) const_aspect_unique: Unique,
    pub(crate) const_aspect_func: Unique,
    pub(crate) const_aspect_artifact: Unique,
    pub(crate) const_aspect_mortar_func: Unique,
    pub(crate) const_aspect_mortar_from_js: Unique,
    pub(crate) const_aspect_mortar_str: Unique,
    pub(crate) const_aspect_mortar_null: Unique,
}

/// One per module (per import env).
pub(crate) struct CompileImportContext {
    pub(crate) gx: Arc<GlobalContext>,
    pub(crate) compile_path: Vec<(CompileId, ExprId)>,
    pub(crate) next_unique_id: Counter,
}

/// Multiple evaluations per module.
pub(crate) struct EvalContext {
    pub(crate) ix: Rc<CompileImportContext>,
    pub(crate) next_scope_entry_id: Counter,
    pub(crate) definition_sets: std::cell::RefCell<Vec<dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>>>,
}

/// Per expression
pub(crate) struct ExprContext {
    pub(crate) vx: Rc<EvalContext>,
    pub(crate) import_state: Rc<Cell<ImportStateId>>,
    pub(crate) scope: Rc<Scope>,
}

pub(crate) trait CodeDyn: Any {
    fn loc(&self) -> QExprId;
}

#[derive(Clone)]
pub enum Code_ {
    None,
    Dyn(Rc<dyn CodeDyn>),
}

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

#[derive(Serialize, Deserialize, Clone, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ExprErr {
    pub(crate) loc: QExprId,
    pub(crate) errors: Vec<ExprSubErr>,
}

/// In the first pass (meta evaluation) this bundles a Value (static meta about a
/// runtime value) with the Code that produces the value.
///
/// The code results in a value described by Value (i.e., in a stack target the top
/// stack element would be the value), but may contain other preceding side
/// effects, so even if the value isn't used all code must be kept.
pub(crate) struct CodeValue {
    pub(crate) code: Code,
    pub(crate) value: Box<dyn Value>,
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
    pub(crate) errors: Vec<ExprErr>,
}

impl<T> EResult<T> {
    pub(crate) fn ok(v: T) -> Self {
        return Self {
            v: Some(v),
            errors: vec![],
        };
    }

    pub(crate) fn err(loc: QExprId, e: String) -> Self {
        return Self {
            v: None,
            errors: vec![ExprErr {
                loc: loc,
                errors: vec![ExprSubErr::Opaque(e)]
            }],
        };
    }

    pub(crate) fn into_res(self) -> Result<T, Vec<ExprErr>> {
        if !self.errors.is_empty() {
            return Err(self.errors);
        } else {
            return Ok(self.v.unwrap());
        }
    }

    pub(crate) fn is_ok(&self) -> bool {
        return self.v.is_some();
    }

    pub(crate) fn map<U>(self, f: impl FnOnce(T) -> U) -> EResult<U> {
        return EResult {
            v: self.v.map(f),
            errors: self.errors,
        };
    }
}

/// If None, it means that the evaluation failed in a way that affects downstream
/// meta evaluation, so no Value could be provided.
pub type EvalRes = EResult<CodeValue>;

pub(crate) trait Value: Any {
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type>;
}

pub(crate) trait Aspect: Any { }

pub(crate) trait Type: Value {
    fn desc(&self) -> String;
    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>>;
    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes;
    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes;
}

#[derive(Default)]
pub(crate) struct Artifacts {}

pub(crate) trait ScopeEntry {
    fn use_(&self, loc: QExprId, code: Code) -> CodeValue;
}

#[derive(PartialEq, Eq, PartialOrd, Ord, Hash, Clone, Debug)]
pub(crate) enum ConstKey {
    String(String),
}

pub(crate) struct Scope {
    pub(crate) values: RefCell<HashMap<ConstKey, Box<dyn ScopeEntry>>>,
    pub(crate) parent: Option<Rc<Scope>>,
}
