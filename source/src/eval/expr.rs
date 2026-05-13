use {
    crate::{
        builtin::{
            AspectConsumeJs,
            aspect_consume_js_aspect_id,
        },
        jscode::{
            JsCode,
            JsCodeExternal,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
};

#[derive(Clone)]
pub struct ExprSequence {
    pub exprs: Vec<Expr>,
}

#[derive(Clone)]
pub struct ExprBind {
    pub base: Rc<Expr>,
    pub value: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprSet {
    pub base: Rc<Expr>,
    pub value: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprField {
    pub base: Rc<Expr>,
    pub key: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprSeq {
    pub seq: Vec<Expr>,
}

#[derive(Clone)]
pub struct ExprScopeUse {
    pub key: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprCall {
    pub func: Rc<Expr>,
    pub arg: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprStage {
    pub expr: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprLower {
    pub expr: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprTuple {
    pub elements: Vec<Expr>,
}

pub type LitValue = Rc<dyn Fn() -> Box<dyn Value>>;

#[derive(Clone)]
pub enum Expr_ {
    Literal(LitValue),
    Bind(ExprBind),
    Set(ExprSet),
    Field(ExprField),
    Seq(ExprSeq),
    ScopeUse(ExprScopeUse),
    Call(ExprCall),
    Stage(ExprStage),
    Lower(ExprLower),
    Tuple(ExprTuple),
}

#[derive(Clone)]
pub struct Expr {
    pub loc: QExprId,
    pub r#type: Expr_,
}

pub enum Module {
    V1(Expr),
}

impl Value for Expr {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(ExprType)
    }
}
pub(crate) struct ExprType;

impl Value for ExprType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(ExprType)
    }
}

/// Stored in JsCodeExternal during eval_half. Value is taken out during eval() setup.
pub(crate) struct StagedExprCell {
    pub value: RefCell<Option<Box<dyn Value>>>,
}

/// Thin-pointer wrapper for v8::External (stored behind Box raw pointer).
pub struct StagedExpr {
    pub value: Box<dyn Value>,
}

impl Type for ExprType {
    fn desc(&self) -> String {
        return format!("expr");
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(ExprType) as Box<dyn AspectConsumeJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        return EvalRes::err(loc, format!("Cannot convert expr to other type"));
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        return EvalRes::err(loc, format!("Cannot convert to expr type"));
    }
}

impl Aspect for ExprType { }

impl AspectConsumeJs for ExprType {
    fn to_js(&self, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        let cell = Rc::new(StagedExprCell {
            value: RefCell::new(Some(cv.value)),
        });
        EResult::ok(JsCode {
            loc,
            type_: JsCodeType::External(JsCodeExternal { value: cell }),
        })
    }
}
