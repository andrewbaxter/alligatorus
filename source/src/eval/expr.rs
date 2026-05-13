use {
    crate::langhelp::{
        Aspect,
        CodeValue,
        EvalRes,
        ExprContext,
        QExprId,
        Type,
        Unique,
        Value,
    },
    std::{
        any::Any,
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

impl Type for ExprType {
    fn desc(&self) -> String {
        return format!("expr");
    }

    fn aspect(&self, _xx: &ExprContext, _key: &Unique) -> Option<Box<dyn Any>> {
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, _loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        todo!();
        //.        let expr = (cv.value.as_ref() as &dyn Any).downcast_ref::<Expr>().unwrap();
        //.        let result = eval_expr(xx, expr, &MortarTarget);
        //.        match result.v {
        //.            Some(result_cv) => {
        //.                let mut converted = typecheck_convert(xx, loc, type_, result_cv);
        //.                converted.errors.extend(result.errors);
        //.                converted
        //.            },
        //.            None => EvalRes {
        //.                v: None,
        //.                errors: result.errors,
        //.            },
        //.        }
    }

    fn cast_from(&self, _xx: &ExprContext, _loc: QExprId, _cv: &CodeValue) -> EvalRes {
        todo!();
        //. return EvalRes::err(loc, format!("Cannot convert to expr type"));
    }
}

impl Aspect for ExprType { }
