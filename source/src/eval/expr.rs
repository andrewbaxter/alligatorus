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
        eval::{
            from_v8_ref,
            mortar_js_value::bind_js_some,
        },
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
            External,
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

impl Value for Expr {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(ExprType)
    }
}
pub(crate) struct ExprType;

fn expr_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for ExprType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeAny)
    }
}

/// Stored in JsCodeExternal during eval_half. Value is taken out via `from_v8_ref`
///
/// * `borrow_mut().take()` — a safe take-once pattern that panics on double-consume
///   instead of double-freeing.
pub(crate) struct StagedExprCell {
    pub value: RefCell<Option<Rc<dyn Value>>>,
}

impl Type for ExprType {
    fn desc(&self) -> String {
        return format!("expr");
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ExprType)
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

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        return EvalRes::err(loc, format!("Cannot convert expr to other type"));
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        true
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &expr_type_identity()).is_some() {
            let expr =
                (cv.value.as_ref() as &dyn Any)
                    .downcast_ref::<Expr>()
                    .expect("ExprType value must be an Expr")
                    .clone();
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(expr),
            });
        }

        // Wrap any non-Expr value as a literal Expr that reproduces the value
        let type_rc = cv.value.type_(xx, loc);
        let expr = Expr {
            loc,
            r#type: Expr_::Literal(Rc::new(move || {
                Rc::new(crate::eval::tuple::StaticValue { type_: type_rc.clone() }) as Rc<dyn Value>
            })),
        };
        return EvalRes::ok(CodeValue {
            code: cv.code,
            value: Rc::new(expr),
        });
    }
}

impl Aspect for ExprType { }

impl AspectConsumeJs for ExprType {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        // If there's already JS code (e.g. from a __stage call), use it directly
        if let Some(js) = cv.code.cast::<JsCode>().v.flatten() {
            return EResult::ok(js.clone());
        }
        let cell = Rc::new(StagedExprCell { value: RefCell::new(Some(cv.value)) });
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, External::StagedExprCell(cell)));
        EResult::ok(JsCode {
            loc,
            type_: JsCodeType::BindingUse(JsCodeBindingUse { id }),
        })
    }
}

impl AspectBindJs for ExprType {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectLiftJs for ExprType {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Rc<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Expr value already consumed during lift"))
    }
}

/// Placeholder value for stage results. Its type exposes AspectLiftJs (to extract
/// the Expr from the v8::External returned by __stage) but NOT AspectConsumeJs (so
/// eval() preserves the Call JsCode).
pub(crate) struct StagedExprPlaceholder;
struct StagedExprPlaceholderType;

impl Value for StagedExprPlaceholder {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(StagedExprPlaceholderType)
    }
}

impl Value for StagedExprPlaceholderType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeAny)
    }
}

impl Type for StagedExprPlaceholderType {
    fn desc(&self) -> String {
        "stage result".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(StagedExprPlaceholderType)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(StagedExprPlaceholderType) as Box<dyn AspectLiftJs>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert stage result".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to stage result".to_string())
    }
}

impl Aspect for StagedExprPlaceholderType { }

impl AspectLiftJs for StagedExprPlaceholderType {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Rc<dyn Value>, String> {
        let cell: Rc<StagedExprCell> = from_v8_ref::<Rc<StagedExprCell>>(&value).clone();
        Ok(cell.value.borrow_mut().take().expect("Stage result already consumed during lift"))
    }
}

pub(crate) fn source_expr_to_expr(source_id: crate::langhelp::SourceId, m: &crate::syntax::SourceExpr) -> Expr {
    use crate::syntax::*;

    Expr {
        loc: (source_id, m.id),
        r#type: match &m.r#type {
            SourceExpr_::Literal(lit) => Expr_::Literal(match lit {
                SourceLiteral::Null => {
                    Rc::new(|| Rc::new(super::null::ValueNull) as Rc<dyn crate::langhelp::Value>)
                },
                SourceLiteral::String(l) => {
                    let l = l.clone();
                    Rc::new(move || {
                        Rc::new(super::string::ValueString(l.clone())) as Rc<dyn crate::langhelp::Value>
                    })
                },
                SourceLiteral::Number(n) => {
                    let n = n.into_inner();
                    Rc::new(move || {
                        Rc::new(super::number::ValueNumber(n)) as Rc<dyn crate::langhelp::Value>
                    })
                },
            }),
            SourceExpr_::ScopeUse(expr) => {
                Expr_::ScopeUse(ExprScopeUse { key: Rc::new(source_expr_to_expr(source_id, &expr.key)) })
            },
            SourceExpr_::Call(expr) => Expr_::Call(ExprCall {
                func: Rc::new(source_expr_to_expr(source_id, &expr.func)),
                arg: Rc::new(source_expr_to_expr(source_id, &expr.arg)),
            }),
            SourceExpr_::Set(expr) => Expr_::Set(ExprSet {
                base: Rc::new(source_expr_to_expr(source_id, &expr.base)),
                value: Rc::new(source_expr_to_expr(source_id, &expr.value)),
            }),
            SourceExpr_::Field(expr) => Expr_::Field(ExprField {
                base: Rc::new(source_expr_to_expr(source_id, &expr.base)),
                key: Rc::new(source_expr_to_expr(source_id, &expr.key)),
            }),
            SourceExpr_::Block(expr) => Expr_::Block(
                ExprBlock { exprs: expr.exprs.iter().map(|e| source_expr_to_expr(source_id, e)).collect() },
            ),
            SourceExpr_::Bind(expr) => Expr_::Bind(ExprBind {
                base: Rc::new(Expr {
                    loc: (source_id, m.id),
                    r#type: Expr_::Literal({
                        let base = expr.base.clone();
                        Rc::new(move || {
                            Rc::new(super::string::ValueString(base.clone())) as Rc<dyn crate::langhelp::Value>
                        })
                    }),
                }),
                value: Rc::new(source_expr_to_expr(source_id, &expr.value)),
            }),
            SourceExpr_::Stage(expr) => {
                Expr_::Stage(ExprStage { expr: Rc::new(source_expr_to_expr(source_id, &expr.expr)) })
            },
            SourceExpr_::Lower(expr) => {
                Expr_::Lower(ExprLower { expr: Rc::new(source_expr_to_expr(source_id, &expr.expr)) })
            },
            SourceExpr_::Tuple(exprs) => Expr_::Tuple(
                ExprTuple { elements: exprs.iter().map(|e| source_expr_to_expr(source_id, e)).collect() },
            ),
            SourceExpr_::Record(entries) => Expr_::Record(
                ExprRecord {
                    entries: entries
                        .iter()
                        .map(|(k, e)| (source_expr_to_expr(source_id, k), source_expr_to_expr(source_id, e)))
                        .collect(),
                },
            ),
            SourceExpr_::If(expr) => Expr_::If(ExprIf {
                conditions: expr.conditions.iter().map(|b| ExprConditionBranch {
                    condition: source_expr_to_expr(source_id, &b.condition),
                    body: source_expr_to_expr(source_id, &b.body),
                }).collect(),
                else_: expr.fallback.as_ref().map(|e| Rc::new(source_expr_to_expr(source_id, e))),
            }),
            SourceExpr_::OperatorLt(expr) => Expr_::OperatorLt(ExprOperatorLt {
                base: Rc::new(source_expr_to_expr(source_id, &expr.base)),
                reference: Rc::new(source_expr_to_expr(source_id, &expr.reference)),
            }),
            SourceExpr_::OperatorAdd(expr) => Expr_::OperatorAdd(ExprOperatorAdd {
                base: Rc::new(source_expr_to_expr(source_id, &expr.base)),
                reference: Rc::new(source_expr_to_expr(source_id, &expr.reference)),
            }),
            SourceExpr_::Foreach(expr) => Expr_::Foreach(ExprForeach {
                base: Rc::new(source_expr_to_expr(source_id, &expr.base)),
                element_key: Rc::new(source_expr_to_expr(source_id, &expr.element_key)),
                body: Rc::new(source_expr_to_expr(source_id, &expr.body)),
            }),
            SourceExpr_::Match(expr) => Expr_::Match(ExprMatch {
                base: Rc::new(source_expr_to_expr(source_id, &expr.base)),
                element_key: Rc::new(source_expr_to_expr(source_id, &expr.element_key)),
                cases: expr.cases.iter().map(|c| ExprMatchBranch {
                    condition: source_expr_to_expr(source_id, &c.condition),
                    body: source_expr_to_expr(source_id, &c.body),
                }).collect(),
                fallback: expr.fallback.as_ref().map(|e| Rc::new(source_expr_to_expr(source_id, e))),
            }),
            SourceExpr_::Import(expr) => {
                Expr_::Import(ExprImport { spec: Rc::new(source_expr_to_expr(source_id, &expr.spec)) })
            },
        },
    }
}
