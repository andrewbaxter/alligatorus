use serde::{
    Deserialize,
    Serialize,
};

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct ExprId(pub usize);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct ScopeKey(pub String);

//. #[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub enum ModuleHash {
//.    Sha256(String),
//. }
//. 
//. #[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub enum RemotePathHttpType {
//.    File,
//.    Zip,
//. }
//. 
//. #[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub struct RemotePathHttp {
//.    pub url: String,
//.    pub r#type: RemotePathHttpType,
//. }
//. 
//. #[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub enum RemotePath {
//.    Http(RemotePathHttp),
//. }
//. 
//. #[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
//. #[serde(rename_all = "snake_case", deny_unknown_fields)]
//. pub struct ImportSpecRemote {
//.    pub hash: ModuleHash,
//.    pub r#path: RemotePath,
//.    pub subpath: Vec<String>,
//. }
#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceSpecAbsolute {
    Virtual(Box<SerialModule>),
    //. Local(Vec<String>),
    //. Remote(ImportSpecRemote),
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialValue {
    Null,
    String(String),
    //. Number(NotNan<f64>),
    //. Bool(bool),
    //. Array(Vec<SerialValue>),
    //. Set(BTreeSet<SerialValue>),
    //. Map(BTreeMap<SerialValue, SerialValue>),
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprSequence {
    pub exprs: Vec<SerialExpr>,
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprScopeUse {
    pub key: ScopeKey,
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprCall {
    pub func: Box<SerialExpr>,
    pub args: Box<SerialExpr>,
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialExprType {
    Literal(SerialValue),
    //. Sequence(SerialExprSequence),
    //. ScopeCreate(SerialExprScopeCreate),
    ScopeUse(SerialExprScopeUse),
    //. Set(SerialExprSet),
    //. Get(SerialExprGet),
    Call(SerialExprCall),
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExpr {
    pub id: ExprId,
    pub r#type: SerialExprType,
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialModule {
    V1(SerialExpr),
}
