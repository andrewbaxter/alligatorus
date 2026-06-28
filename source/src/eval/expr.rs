use {
    crate::langhelp::{
        CodeValue,
        QExprId,
        Value,
    },
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct Expr {
    pub loc: QExprId,
    pub r#type: Expr_,
}

#[derive(Clone)]
pub(crate) enum Expr_ {
    As(ExprAs),
    Bind(ExprBind),
    Call(ExprCall),
    Field(ExprField),
    Foreach(ExprForeach),
    Graft(ExprGraft),
    If(ExprIf),
    Import(ExprImport),
    Literal(LitValue),
    Lower(ExprLower),
    Match(ExprMatch),
    OperatorAdd(ExprOperatorAdd),
    OperatorEq(ExprOperatorEq),
    OperatorLt(ExprOperatorLt),
    Record(ExprRecord),
    ScopeUse(ExprScopeUse),
    Seq(ExprSeq),
    Set(ExprSet),
    Stage(ExprStage),
    Try(ExprTry),
    Tuple(ExprTuple),
}

#[derive(Clone)]
pub(crate) struct ExprAs {
    pub base: Rc<Expr>,
    pub target: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprBind {
    pub base: Rc<Expr>,
    pub value: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprCall {
    pub arg: Rc<Expr>,
    pub func: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprConditionBranch {
    pub body: Expr,
    pub condition: Expr,
}

#[derive(Clone)]
pub(crate) struct ExprField {
    pub base: Rc<Expr>,
    pub key: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprForeach {
    pub base: Rc<Expr>,
    pub body: Rc<Expr>,
    pub element_key: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) enum ExprGraft {
    Evaluated(CodeValue),
    Pending(Rc<Expr>),
}

#[derive(Clone)]
pub(crate) struct ExprIf {
    pub conditions: Vec<ExprConditionBranch>,
    pub else_: Option<Rc<Expr>>,
}

#[derive(Clone)]
pub(crate) struct ExprImport {
    pub spec: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprLower {
    pub expr: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprMatch {
    pub base: Rc<Expr>,
    pub cases: Vec<ExprMatchBranch>,
    pub element_key: Rc<Expr>,
    pub fallback: Option<Rc<Expr>>,
}

#[derive(Clone)]
pub(crate) struct ExprMatchBranch {
    pub body: Expr,
    pub condition: Expr,
}

#[derive(Clone)]
pub(crate) struct ExprOperatorAdd {
    pub base: Rc<Expr>,
    pub reference: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprOperatorEq {
    pub base: Rc<Expr>,
    pub reference: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprOperatorLt {
    pub base: Rc<Expr>,
    pub reference: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprRecord {
    pub entries: Vec<(Expr, Expr)>,
}

#[derive(Clone)]
pub(crate) struct ExprScopeUse {
    pub key: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprSeq {
    pub exprs: Vec<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprSet {
    pub base: Rc<Expr>,
    pub value: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprStage {
    pub expr: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprTry {
    pub body: Rc<Expr>,
    pub catch: Option<ExprTryCatch>,
    pub finally: Option<Rc<Expr>>,
}

#[derive(Clone)]
pub(crate) struct ExprTryCatch {
    pub binding_name: Rc<Expr>,
    pub body: Rc<Expr>,
    pub exception_type: Rc<Expr>,
}

#[derive(Clone)]
pub(crate) struct ExprTuple {
    pub elements: Vec<Expr>,
}

#[derive(Clone)]
pub(crate) enum LitValue {
    Once(Rc<RefCell<Option<Rc<dyn Value>>>>),
    Shared(Rc<dyn Value>),
}

impl LitValue {
    pub(crate) fn consume(&self) -> Rc<dyn Value> {
        match self {
            LitValue::Shared(v) => v.clone(),
            LitValue::Once(cell) => cell.borrow_mut().take().expect("Once Literal already consumed"),
        }
    }

    pub(crate) fn peek(&self) -> Option<Rc<dyn Value>> {
        match self {
            LitValue::Shared(v) => Some(v.clone()),
            LitValue::Once(cell) => cell.borrow().clone(),
        }
    }
}

pub(crate) fn make_lit_value(v: Rc<dyn Value>) -> LitValue {
    LitValue::Shared(v)
}

pub(crate) fn make_lit_value_once(v: Rc<dyn Value>) -> LitValue {
    LitValue::Once(Rc::new(RefCell::new(Some(v))))
}

pub(crate) enum Module {
    V1(Expr),
}

pub(crate) struct StagedExprCell {
    pub value: RefCell<Option<Rc<dyn Value>>>,
}
