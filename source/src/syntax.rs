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

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprIfConditionBranch {
    pub body: SourceExpr,
    pub condition: SourceExpr,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SerialExprMatchTagBranch {
    pub body: SourceExpr,
    pub condition: SourceExpr,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExpr {
    pub id: ExprId,
    pub r#type: SourceExpr_,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceExpr_ {
    As(SourceExprAs),
    Bind(SourceExprBind),
    Call(SourceExprCall),
    Field(SourceExprField),
    Foreach(SourceExprForeach),
    Graft(SourceExprGraft),
    If(SourceExprIf),
    Import(SourceExprImport),
    Literal(SourceLiteral),
    Lower(SourceExprLower),
    Match(SourceExprMatch),
    OperatorAdd(SourceExprOperatorAdd),
    OperatorEq(SourceExprOperatorEq),
    OperatorLt(SourceExprOperatorLt),
    Record(Vec<(SourceExpr, SourceExpr)>),
    ScopeUse(SourceExprBindingUse),
    Seq(SourceExprSeq),
    Set(SourceExprSet),
    Stage(SourceExprStage),
    Try(SourceExprTry),
    Tuple(Vec<SourceExpr>),
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprAs {
    pub base: Box<SourceExpr>,
    pub target: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprBind {
    pub base: String,
    pub value: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprBindingUse {
    pub key: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprCall {
    pub arg: Box<SourceExpr>,
    pub func: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprField {
    pub base: Box<SourceExpr>,
    pub key: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprForeach {
    pub base: Box<SourceExpr>,
    pub body: Box<SourceExpr>,
    pub element_key: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprGraft {
    pub expr: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprIf {
    pub conditions: Vec<SerialExprIfConditionBranch>,
    pub fallback: Option<Box<SourceExpr>>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprImport {
    pub spec: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprLower {
    pub expr: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprMatch {
    pub base: Box<SourceExpr>,
    pub cases: Vec<SerialExprMatchTagBranch>,
    pub element_key: Box<SourceExpr>,
    pub fallback: Option<Box<SourceExpr>>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprOperatorAdd {
    pub base: Box<SourceExpr>,
    pub reference: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprOperatorEq {
    pub base: Box<SourceExpr>,
    pub reference: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprOperatorLt {
    pub base: Box<SourceExpr>,
    pub reference: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprSeq {
    pub exprs: Vec<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprSet {
    pub base: Box<SourceExpr>,
    pub value: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprStage {
    pub expr: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprTry {
    pub body: Box<SourceExpr>,
    pub catch: Option<SourceExprTryCatch>,
    pub finally: Option<Box<SourceExpr>>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct SourceExprTryCatch {
    pub binding_name: Box<SourceExpr>,
    pub body: Box<SourceExpr>,
    pub exception_type: Box<SourceExpr>,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceLiteral {
    Null,
    Number(NotNan<f64>),
    String(String),
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Hash, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceModule {
    V1(SourceExpr),
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum SourceSpecAbsolute {
    Virtual(String),
}
