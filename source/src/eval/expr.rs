use {
    crate::langhelp::{
        QExprId,
        Value,
    },
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct ExprBind {
    pub base: Rc<Expr>,
    pub value: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprSet {
    pub base: Rc<Expr>,
    pub value: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprField {
    pub base: Rc<Expr>,
    pub key: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprBlock {
    pub exprs: Vec<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprScopeUse {
    pub key: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprCall {
    pub func: Rc<Expr>,
    pub arg: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprStage {
    pub expr: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprLower {
    pub expr: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprTuple {
    pub elements: Vec<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprRecord {
    pub entries: Vec<(Expr, Expr)>,
}

#[derive(Clone)]
pub(crate) struct ExprConditionBranch {
    pub condition: Expr,
    pub body: Expr,
}

#[derive(Clone)]
pub(crate) struct ExprIf {
    pub conditions: Vec<ExprConditionBranch>,
    pub else_: Option<Rc<Expr>>,
}

#[derive(Clone)]
pub(crate) struct ExprOperatorLt {
    pub base: Rc<Expr>,
    pub reference: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprOperatorAdd {
    pub base: Rc<Expr>,
    pub reference: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprForeach {
    pub base: Rc<Expr>,
    pub element_key: Rc<Expr>,
    pub body: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprMatchBranch {
    pub condition: Expr,
    pub body: Expr,
}

#[derive(Clone)]
pub(crate) struct ExprMatch {
    pub base: Rc<Expr>,
    pub element_key: Rc<Expr>,
    pub cases: Vec<ExprMatchBranch>,
    pub fallback: Option<Rc<Expr>>,
}

#[derive(Clone)]
pub(crate) struct ExprAs {
    pub base: Rc<Expr>,
    pub target: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprImport {
    pub spec: Rc<Expr>,
}

pub(crate) type LitValue = Rc<dyn Fn() -> Rc<dyn Value>>;

#[derive(Clone)]
pub(crate) enum Expr_ {
    Literal(LitValue),
    Bind(ExprBind),
    Set(ExprSet),
    Field(ExprField),
    Block(ExprBlock),
    ScopeUse(ExprScopeUse),
    Call(ExprCall),
    Stage(ExprStage),
    Lower(ExprLower),
    Tuple(ExprTuple),
    Record(ExprRecord),
    If(ExprIf),
    OperatorLt(ExprOperatorLt),
    OperatorAdd(ExprOperatorAdd),
    Foreach(ExprForeach),
    Match(ExprMatch),
    As(ExprAs),
    Import(ExprImport),
}

#[derive(Clone)]
pub(crate) struct Expr {
    pub loc: QExprId,
    pub r#type: Expr_,
}

pub(crate) enum Module {
    V1(Expr),
}

/// Stored in CodeDynamicExternal during eval_half. Value is taken out via
/// `from_v8_ref`
///
/// * `borrow_mut().take()` — a safe take-once pattern that panics on double-consume
///   instead of double-freeing.
pub(crate) struct StagedExprCell {
    pub value: RefCell<Option<Rc<dyn Value>>>,
}
