use {
    crate::syntax::{
        ExprId,
        ScopeKey,
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
        cell::Cell,
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
pub struct Counter(Cell<usize>);

impl Counter {
    pub fn next(&self) -> usize {
        let out = self.0.get();
        self.0.set(out + 1);
        return out;
    }
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct ImportStateId(pub usize);

/// World id for this import spec
#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceId(pub usize);

pub type QExprId = (SourceId, ExprId);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct CompileId(pub ImportStateId, pub SourceId);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum ExprErr {
    Opaque(String),
}

#[derive(Serialize, Deserialize, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialValueSource {
    Builtin,
    Compile(CompileId),
}

pub struct ValueTypeAny {}

/// A value guaranteed to be unique in the world
#[derive(Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct Unique(pub SerialValueSource, pub usize);

impl Value for Unique {
    fn type_(&self, ex: &ExprContext, loc: QExprId) -> Rc<dyn Type> {
        todo!()
    }
}

//. #[derive(Serialize, Deserialize, Clone)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub struct SerialValueId(SerialValueSource, usize);
//. 
//. #[derive(Serialize, Deserialize, Clone)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub struct SerialValueComplex {
//.     pub type_: SerialValueId,
//.     pub value: serde_json::Value,
//. }
#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialValue {
    Null,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum CompileRes {
    Ok(SerialValue),
    Err(Vec<EvalErr>),
}

pub struct FullCompileContext {
    pub artifacts: RwLock<Artifacts>,
    pub cache_root: Vec<String>,
    // Remote packages/sources, resolved when downloaded (path is deterministic so not
    // returned, use a method to get the path)
    //. pub remote_source: Mutex<HashMap<ImportSpecRemote, Shared<BoxFuture<'static, Result<(), ExprErr>>>>>,
    pub compile_results: Mutex<HashMap<CompileId, Shared<BoxFuture<'static, Result<CompileRes, ExprErr>>>>>,
    pub const_aspect_unique: Unique,
    pub const_aspect_func: Unique,
    pub const_aspect_artifact: Unique,
    pub const_aspect_mortar_func: Unique,
    pub const_aspect_mortar_from_js: Unique,
    pub const_aspect_mortar_str: Unique,
    pub const_aspect_mortar_null: Unique,
}

/// One per module (per import env).
pub struct CompileContext {
    pub cx: Arc<FullCompileContext>,
    pub compile_path: Vec<(CompileId, ExprId)>,
    pub next_unique_id: Counter,
}

/// Multiple evaluations per module.
pub struct EvalContext {
    pub ix: Rc<CompileContext>,
    pub next_scope_entry_id: Counter,
}

/// Per expression
pub struct ExprContext {
    pub vx: Rc<EvalContext>,
    pub import_state: Rc<Cell<ImportStateId>>,
    pub scope: Rc<Scope>,
}

pub trait CodeDyn: Any {
    fn loc(&self) -> QExprId;
}

#[derive(Clone)]
pub enum Code {
    None,
    Dyn(Rc<dyn CodeDyn>),
}

impl Code {
    pub fn cast<T: CodeDyn>(&self) -> Result<Option<&T>, String> {
        match self {
            Code::None => {
                return Ok(None);
            },
            Code::Dyn(c) => match <dyn Any>::downcast_ref::<T>(c.as_ref()) {
                Some(c) => {
                    return Ok(Some(c));
                },
                None => {
                    return Err(format!("TODO code generated at {:?} is not the expected type", c.loc()));
                },
            },
        }
    }
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct EvalErr {
    pub expr: QExprId,
    pub errors: Vec<ExprErr>,
}

pub struct CodeValue {
    pub code: Code,
    pub value: Box<dyn Value>,
}

pub struct EvalRes {
    pub cv: Option<CodeValue>,
    pub errors: Vec<EvalErr>,
}

impl EvalRes {
    pub fn ok(&self) -> bool {
        return self.cv.is_some();
    }
}

pub trait Value: Any {
    /// Mutually exclusive with consume.
    //.     fn drop(&self, xx: &ExprContext, loc: QExprId) -> Code {
    //.         return Code::None;
    //.     }
    /// Called after consume, end of lifecycle path.
    //. fn cleanup(&self, ex: &ExprContext, loc: QExprId) -> Code;
    /// * Take a deferred value and make it not deferred (like consume, but returns stack
    ///   value too).
    ///
    /// * Done before finishing branches because unfork must be done on stack values.
    //. fn realize(&self, ex: &ExprContext, loc: QExprId) -> EvaluateResult;
    fn type_(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn Type>;
}

pub trait Aspect: Any { }

pub trait Type: Value {
    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>>;
    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue, type_: &dyn Type) -> EvalRes;
    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes;
}

#[derive(Default)]
pub struct Artifacts {}

//. 
//. impl Artifacts {
//.     fn reserve(&self) -> SerialValue { }
//. }
pub trait ScopeEntry {
    fn use_(&self) -> CodeValue;
}

pub struct Scope {
    pub values: HashMap<ScopeKey, Box<dyn ScopeEntry>>,
    pub parent: Option<Rc<Scope>>,
}
