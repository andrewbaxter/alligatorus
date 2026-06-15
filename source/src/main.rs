#[macro_use]
pub mod builtin;
pub mod code_dynamic;
pub mod eval;
pub mod invariant;
pub mod langhelp;

// `gen` is a reserved keyword in Rust 2024 (gen blocks/closures), so the module's
// identifier is raw-form `r#gen`. Paths in callers use `crate::r#gen::xyz` (or
// `use crate::r#gen as gen;` for local convenience).
#[path = "gen.rs"]
pub mod r#gen;
pub mod syntax;
#[cfg(test)]
mod tests;

use {
    crate::{
        eval::{
            compile_importid,
            expr::{
                Expr,
                ExprAs,
                ExprBind,
                ExprBlock,
                ExprCall,
                ExprConditionBranch,
                ExprField,
                ExprForeach,
                ExprIf,
                ExprImport,
                ExprLower,
                ExprMatch,
                ExprMatchBranch,
                ExprOperatorAdd,
                ExprOperatorEq,
                ExprOperatorLt,
                ExprRecord,
                ExprScopeUse,
                ExprSet,
                ExprStage,
                ExprTuple,
                Expr_,
                Module,
            },
            value::{
                value_null::r#const::ValueNullConst,
                value_number::r#const::ValueNumberConst,
                value_string::r#const::ValueStringConst,
            },
        },
        langhelp::{
            CompileRes,
            CompileResResult,
            ExprErr,
            GlobalContext,
            ImportStateId,
            LogEntry,
            SourceId,
        },
        syntax::{
            SourceExpr,
            SourceExpr_,
            SourceLiteral,
            SourceModule,
            SourceSpecAbsolute,
        },
    },
    std::{
        rc::Rc,
        sync::Arc,
    },
};

pub(crate) fn compile(gx: Arc<GlobalContext>, source: SourceSpecAbsolute) -> Result<CompileOutput, Vec<ExprErr>> {
    let (source_id, source_module) = gx.sources.get(source).map_err(|e| vec![ExprErr {
        loc: (SourceId(0), crate::syntax::ExprId(0)),
        errors: vec![langhelp::ExprSubErr::Opaque(e)],
    }])?;
    let module = source_module_to_module(source_id, &source_module);
    let res = compile_importid(&gx, ImportStateId(0), module, vec![]);

    fn collect_errors(r: &CompileRes) -> Vec<ExprErr> {
        match &r.result {
            CompileResResult::Err(errs) => errs.clone(),
            CompileResResult::Ok(_) => vec![],
        }
    }

    let mut all_errors = vec![];
    let mut all_scripts = vec![];
    for (_, entry) in gx.compile_results.lock().unwrap().iter() {
        let guard = entry.result.read().unwrap();
        if let Some(Ok(r)) = guard.as_ref() {
            all_errors.extend(collect_errors(r));
            all_scripts.extend(r.generated_mortar_scripts.iter().cloned());
        }
    }
    all_errors.extend(collect_errors(&res));
    all_scripts.extend(res.generated_mortar_scripts.iter().cloned());
    if !all_errors.is_empty() {
        return Err(all_errors);
    }
    return Ok(CompileOutput {
        logs: res.logs,
        generated_mortar_scripts: all_scripts,
    });
}

#[derive(Debug)]
pub(crate) struct CompileOutput {
    pub(crate) generated_mortar_scripts: Vec<String>,
    pub(crate) logs: Vec<LogEntry>,
}

fn main() { }

pub(crate) fn source_module_to_module(source_id: SourceId, m: &SourceModule) -> Module {
    fn source_expr_to_expr(source_id: SourceId, m: &SourceExpr) -> Expr {
        return Expr {
            loc: (source_id, m.id),
            r#type: match &m.r#type {
                SourceExpr_::Literal(lit) => Expr_::Literal(crate::eval::expr::make_lit_value(match lit {
                    SourceLiteral::Null => Rc::new(ValueNullConst) as Rc<dyn crate::langhelp::Value>,
                    SourceLiteral::String(l) => Rc::new(ValueStringConst(l.clone())) as
                        Rc<dyn crate::langhelp::Value>,
                    SourceLiteral::Number(n) => {
                        Rc::new(ValueNumberConst(n.into_inner())) as Rc<dyn crate::langhelp::Value>
                    },
                })),
                SourceExpr_::ScopeUse(expr) => Expr_::ScopeUse(
                    ExprScopeUse { key: Rc::new(source_expr_to_expr(source_id, &expr.key)) },
                ),
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
                        r#type: Expr_::Literal(
                            crate::eval::expr::make_lit_value(
                                Rc::new(ValueStringConst(expr.base.clone())) as Rc<dyn crate::langhelp::Value>,
                            ),
                        ),
                    }),
                    value: Rc::new(source_expr_to_expr(source_id, &expr.value)),
                }),
                SourceExpr_::Stage(expr) => Expr_::Stage(
                    ExprStage { expr: Rc::new(source_expr_to_expr(source_id, &expr.expr)) },
                ),
                SourceExpr_::Lower(expr) => Expr_::Lower(
                    ExprLower { expr: Rc::new(source_expr_to_expr(source_id, &expr.expr)) },
                ),
                SourceExpr_::Graft(expr) => Expr_::Graft(
                    crate::eval::expr::ExprGraft { expr: Rc::new(source_expr_to_expr(source_id, &expr.expr)) },
                ),
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
                SourceExpr_::OperatorEq(expr) => Expr_::OperatorEq(ExprOperatorEq {
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
                SourceExpr_::As(expr) => Expr_::As(ExprAs {
                    base: Rc::new(source_expr_to_expr(source_id, &expr.base)),
                    target: Rc::new(source_expr_to_expr(source_id, &expr.target)),
                }),
                SourceExpr_::Import(expr) => Expr_::Import(
                    ExprImport { spec: Rc::new(source_expr_to_expr(source_id, &expr.spec)) },
                ),
            },
        };
    }

    match m {
        SourceModule::V1(m) => Module::V1(source_expr_to_expr(source_id, m)),
    }
}
