use serde::{
    Deserialize,
    Serialize,
};

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct ExprId(pub usize);

//. #[derive(Debug,Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub enum ModuleHash {
//.    Sha256(String),
//. }
//. 
//. #[derive(Debug,Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub enum RemotePathHttpType {
//.    File,
//.    Zip,
//. }
//. 
//. #[derive(Debug,Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub struct RemotePathHttp {
//.    pub url: String,
//.    pub r#type: RemotePathHttpType,
//. }
//. 
//. #[derive(Debug,Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub enum RemotePath {
//.    Http(RemotePathHttp),
//. }
//. 
//. #[derive(Debug,Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub struct ImportSpecRemote {
//.    pub hash: ModuleHash,
//.    pub r#path: RemotePath,
//.    pub subpath: Vec<String>,
//. }
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceSpecAbsolute {
    Virtual(Box<SerialModule>),
    //. Local(Vec<String>),
    //. Remote(ImportSpecRemote),
}

/// A value specified in source (literal), rather than as the result of a
/// calculation.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialLiteral {
    Null,
    String(String),
    //. Number(NotNan<f64>),
    //. Bool(bool),
    //. Array(Vec<SerialValue>),
    //. Set(BTreeSet<SerialValue>),
    //. Map(BTreeMap<SerialValue, SerialValue>),
}

/// Bind a value in the closest scope.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprBind {
    pub base: String,
    pub value: Box<SerialExpr>,
}

/// Run a number of expressions in sequence. This resolves to the value of the last
/// sequence element - all preceding values are discarded. If the sequence is
/// empty, evaluates to null.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprSequence {
    pub exprs: Vec<SerialExpr>,
}

/// Refer to a binding in scope.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprBindingUse {
    pub key: Box<SerialExpr>,
}

/// Modify the value of a value field or an existing binding.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprSet {
    pub base: Box<SerialExpr>,
    pub value: Box<SerialExpr>,
}

/// Refer to a field of a value.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprField {
    pub base: Box<SerialExpr>,
    pub key: Box<SerialExpr>,
}

/// Call a function with the specified argument.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprCall {
    pub func: Box<SerialExpr>,
    pub arg: Box<SerialExpr>,
}

/// Turn the expression tree into a value representing the expression tree, rather
/// than evaluating the expression tree directly. Used for things like providing
/// function bodies to function builders.
///
/// Values can be substituted in using "lower".
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprStage {
    pub expr: Box<SerialExpr>,
}

/// Places a value in a staged source tree (or the root source if outside any
/// source tree).
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprLower {
    pub expr: Box<SerialExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialExpr_ {
    Literal(SerialLiteral),
    Sequence(SerialExprSequence),
    ScopeUse(SerialExprBindingUse),
    Bind(SerialExprBind),
    Set(SerialExprSet),
    Field(SerialExprField),
    Call(SerialExprCall),
    Stage(SerialExprStage),
    Lower(SerialExprLower),
    Tuple(Vec<SerialExpr>),
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExpr {
    pub id: ExprId,
    pub r#type: SerialExpr_,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialModule {
    V1(SerialExpr),
}
