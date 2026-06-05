use {
    ordered_float::NotNan,
    serde::{
        Deserialize,
        Serialize,
    },
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
    Virtual(String),
    //. Local(Vec<String>),
    //. Remote(ImportSpecRemote),
}

/// A value specified in source (literal), rather than as the result of a
/// calculation.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceLiteral {
    Null,
    String(String),
    Number(NotNan<f64>),
    //. Bool(bool),
    //. Array(Vec<SerialValue>),
    //. Set(BTreeSet<SerialValue>),
    //. Map(BTreeMap<SerialValue, SerialValue>),
}

/// Bind a value in the closest scope.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprBind {
    pub base: String,
    pub value: Box<SourceExpr>,
}

/// Run a number of expressions in sequence. This resolves to the value of the last
/// sequence element - all preceding values are discarded. If the sequence is
/// empty, evaluates to null.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprBlock {
    pub exprs: Vec<SourceExpr>,
}

/// Refer to a binding in scope.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprBindingUse {
    pub key: Box<SourceExpr>,
}

/// Modify the value of a value field or an existing binding.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprSet {
    pub base: Box<SourceExpr>,
    pub value: Box<SourceExpr>,
}

/// Refer to a field of a value.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprField {
    pub base: Box<SourceExpr>,
    pub key: Box<SourceExpr>,
}

/// Call a function with the specified argument.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprCall {
    pub func: Box<SourceExpr>,
    pub arg: Box<SourceExpr>,
}

/// Turn the expression tree into a value representing the expression tree, rather
/// than evaluating the expression tree directly. Used for things like providing
/// function bodies to function builders.
///
/// Values can be substituted in using "lower".
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprStage {
    pub expr: Box<SourceExpr>,
}

/// Places a value in a staged source tree. When at the root source level,
/// initiates a new higher level evaluation before continuing to evaluate the root
/// source.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprLower {
    pub expr: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprIfConditionBranch {
    pub condition: SourceExpr,
    pub body: SourceExpr,
}

/// An `if` statement, with multiple condition clauses and optionally one
/// default/else clause.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprIf {
    pub conditions: Vec<SerialExprIfConditionBranch>,
    pub fallback: Option<Box<SourceExpr>>,
}

/// Evaluated in order "base", "reference". Result matches the type of the "base"
/// argument.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprOperatorLt {
    pub base: Box<SourceExpr>,
    pub reference: Box<SourceExpr>,
}

/// Evaluated in order "base", "reference". Result matches the type of the "base"
/// argument.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprOperatorAdd {
    pub base: Box<SourceExpr>,
    pub reference: Box<SourceExpr>,
}

/// Generates code to iterate over `base` via the "iterable" aspect. Creates a new
/// scope before evaluating body - the current iteration element is available in
/// that scope using the "element_key" key.
#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprForeach {
    pub base: Box<SourceExpr>,
    pub element_key: Box<SourceExpr>,
    pub body: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprMatchTagBranch {
    pub condition: SourceExpr,
    pub body: SourceExpr,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprMatch {
    pub base: Box<SourceExpr>,
    pub element_key: Box<SourceExpr>,
    pub cases: Vec<SerialExprMatchTagBranch>,
    pub fallback: Option<Box<SourceExpr>>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprImport {
    pub spec: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceExpr_ {
    Literal(SourceLiteral),
    Block(SourceExprBlock),
    ScopeUse(SourceExprBindingUse),
    Bind(SourceExprBind),
    Set(SourceExprSet),
    Field(SourceExprField),
    Call(SourceExprCall),
    Stage(SourceExprStage),
    Lower(SourceExprLower),
    Tuple(Vec<SourceExpr>),
    Record(Vec<(SourceExpr, SourceExpr)>),
    If(SourceExprIf),
    OperatorLt(SourceExprOperatorLt),
    OperatorAdd(SourceExprOperatorAdd),
    Foreach(SourceExprForeach),
    Match(SourceExprMatch),
    Import(SourceExprImport),
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExpr {
    pub id: ExprId,
    pub r#type: SourceExpr_,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceModule {
    V1(SourceExpr),
}
