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

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
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

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprBind {
    pub base: String,
    pub value: Box<SerialExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprSequence {
    pub exprs: Vec<SerialExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprScopeUse {
    pub key: Box<SerialExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprSet {
    pub base: Box<SerialExpr>,
    pub value: Box<SerialExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprField {
    pub base: Box<SerialExpr>,
    pub key: Box<SerialExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprCall {
    pub func: Box<SerialExpr>,
    pub args: Box<SerialExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialExprType {
    Literal(SerialValue),
    Sequence(SerialExprSequence),
    ScopeUse(SerialExprScopeUse),
    Bind(SerialExprBind),
    Set(SerialExprSet),
    Field(SerialExprField),
    Call(SerialExprCall),
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExpr {
    pub id: ExprId,
    pub r#type: SerialExprType,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SerialModule {
    V1(SerialExpr),
}
