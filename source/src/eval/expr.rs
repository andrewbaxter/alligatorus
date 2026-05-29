use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectLiftJs,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_lift_js_aspect_id,
        },
        eval::rc_from_v8,
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeType,
        },
        langhelp::{
            Aspect,
            CodeValue,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
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
pub struct ExprBlock {
    pub exprs: Vec<Expr>,
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

#[derive(Clone)]
pub struct ExprRecord {
    pub entries: Vec<(Expr, Expr)>,
}

#[derive(Clone)]
pub struct ExprConditionBranch {
    pub condition: Expr,
    pub body: Expr,
}

#[derive(Clone)]
pub struct ExprIf {
    pub conditions: Vec<ExprConditionBranch>,
    pub else_: Option<Rc<Expr>>,
}

#[derive(Clone)]
pub struct ExprOperatorLt {
    pub base: Rc<Expr>,
    pub reference: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprOperatorAdd {
    pub base: Rc<Expr>,
    pub reference: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprForeach {
    pub base: Rc<Expr>,
    pub element_key: Rc<Expr>,
    pub body: Rc<Expr>,
}

#[derive(Clone)]
pub struct ExprMatchBranch {
    pub condition: Expr,
    pub body: Expr,
}

#[derive(Clone)]
pub struct ExprMatch {
    pub base: Rc<Expr>,
    pub element_key: Rc<Expr>,
    pub cases: Vec<ExprMatchBranch>,
    pub fallback: Option<Rc<Expr>>,
}

pub type LitValue = Rc<dyn Fn() -> Box<dyn Value>>;

#[derive(Clone)]
pub enum Expr_ {
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

fn expr_type_identity() -> Unique {
    Unique::Builtin { file: file!(), line: line!() as usize }
}

impl Value for ExprType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

/// Stored in JsCodeExternal during eval_half. Value is taken out via
/// `rc_from_v8` + `borrow_mut().take()` — a safe take-once pattern that
/// panics on double-consume instead of double-freeing.
pub(crate) struct StagedExprCell {
    pub value: RefCell<Option<Box<dyn Value>>>,
}

impl Type for ExprType {
    fn desc(&self) -> String {
        return format!("expr");
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == expr_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(ExprType) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(ExprType) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(ExprType) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        return EvalRes::err(loc, format!("Cannot convert expr to other type"));
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &expr_type_identity()).is_some() {
            let expr = (cv.value.as_ref() as &dyn Any)
                .downcast_ref::<Expr>()
                .expect("ExprType value must be an Expr")
                .clone();
            return EvalRes::ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(expr),
            });
        }
        // Wrap any non-Expr value as a literal Expr that reproduces the value
        let type_rc = cv.value.type_(xx, loc);
        let expr = Expr {
            loc,
            r#type: Expr_::Literal(Rc::new(move || {
                Box::new(crate::eval::tuple::StaticValue { type_: type_rc.clone() })
            })),
        };
        return EvalRes::ok(CodeValue {
            code: cv.code.clone(),
            value: Box::new(expr),
        });
    }
}

impl Aspect for ExprType { }

impl AspectConsumeJs for ExprType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        let cell = Rc::new(StagedExprCell {
            value: RefCell::new(Some(cv.value)),
        });
        let id = ScopeEntryId(xx.vx.next_scope_entry_id.next());
        xx.vx.externals.borrow_mut().push((id, cell));
        EResult::ok(JsCode {
            loc,
            type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
        })
    }
}

impl AspectBindJs for ExprType {
    fn bind_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(JsCode, Rc<dyn Type>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| (c, vtype))
    }
}

impl AspectLiftJs for ExprType {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = rc_from_v8(&value);
        Ok(cell.value.borrow_mut().take().expect("Expr value already consumed during lift"))
    }
}

/// Placeholder value for stage results. Its type exposes AspectLiftJs
/// (to extract the Expr from the v8::External returned by __stage) but
/// NOT AspectConsumeJs (so eval() preserves the StageCall JsCode).
pub(crate) struct StagedExprPlaceholder;

struct StagedExprPlaceholderType;

impl Value for StagedExprPlaceholder {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(StagedExprPlaceholderType)
    }
}

impl Value for StagedExprPlaceholderType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }
}

impl Type for StagedExprPlaceholderType {
    fn desc(&self) -> String {
        "stage result".to_string()
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(StagedExprPlaceholderType) as Box<dyn AspectLiftJs>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert stage result".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to stage result".to_string())
    }
}

impl Aspect for StagedExprPlaceholderType {}

impl AspectLiftJs for StagedExprPlaceholderType {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = rc_from_v8(&value);
        Ok(cell.value.borrow_mut().take().expect("Stage result already consumed during lift"))
    }
}
