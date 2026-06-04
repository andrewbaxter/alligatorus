pub mod jscode;
pub mod langhelp;
pub mod syntax;
pub mod builtin;
pub mod eval;

use {
    aargvark::{
        Aargvark,
        traits_impls::AargvarkJson,
        vark,
    },
    crate::{
        eval::{
            compile_importid,
            expr::{
                Expr,
                ExprBind,
                ExprBlock,
                ExprCall,
                ExprConditionBranch,
                ExprField,
                ExprForeach,
                ExprIf,
                ExprLower,
                ExprMatch,
                ExprMatchBranch,
                ExprOperatorAdd,
                ExprOperatorLt,
                ExprRecord,
                ExprScopeUse,
                ExprSet,
                ExprStage,
                ExprTuple,
                Expr_,
                Module,
            },
            null::ValueNull,
            number::ValueNumber,
            setup_v8,
            string::ValueString,
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
        collections::HashMap,
        process::exit,
        rc::Rc,
        sync::Arc,
    },
};

fn new_gx() -> Arc<GlobalContext> {
    let gx = Arc::new(GlobalContext {
        artifacts: Default::default(),
        generated_mortar_scripts: Default::default(),
        compile_results: Default::default(),
    });
    return gx;
}

async fn compile(gx: Arc<GlobalContext>, source: SourceSpecAbsolute) -> Result<Vec<LogEntry>, Vec<ExprErr>> {
    eprintln!("code {:#?}", source);

    fn source_module_to_module(source_id: SourceId, m: &SourceModule) -> Module {
        fn source_expr_to_expr(source_id: SourceId, m: &SourceExpr) -> Expr {
            return Expr {
                loc: (source_id, m.id),
                r#type: match &m.r#type {
                    SourceExpr_::Literal(lit) => Expr_::Literal(match lit {
                        SourceLiteral::Null => Rc::new(|| Rc::new(ValueNull) as Rc<dyn crate::langhelp::Value>),
                        SourceLiteral::String(l) => {
                            let l = l.clone();
                            Rc::new(move || Rc::new(ValueString(l.clone())) as Rc<dyn crate::langhelp::Value>)
                        },
                        SourceLiteral::Number(n) => {
                            let n = n.into_inner();
                            Rc::new(move || Rc::new(ValueNumber(n)) as Rc<dyn crate::langhelp::Value>)
                        },
                    }),
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
                            r#type: Expr_::Literal({
                                let base = expr.base.clone();
                                Rc::new(
                                    move || Rc::new(ValueString(base.clone())) as Rc<dyn crate::langhelp::Value>,
                                )
                            }),
                        }),
                        value: Rc::new(source_expr_to_expr(source_id, &expr.value)),
                    }),
                    SourceExpr_::Stage(expr) => Expr_::Stage(
                        ExprStage { expr: Rc::new(source_expr_to_expr(source_id, &expr.expr)) },
                    ),
                    SourceExpr_::Lower(expr) => Expr_::Lower(
                        ExprLower { expr: Rc::new(source_expr_to_expr(source_id, &expr.expr)) },
                    ),
                    SourceExpr_::Tuple(exprs) => Expr_::Tuple(
                        ExprTuple { elements: exprs.iter().map(|e| source_expr_to_expr(source_id, e)).collect() },
                    ),
                    SourceExpr_::Record(entries) => Expr_::Record(
                        ExprRecord {
                            entries: entries
                                .iter()
                                .map(
                                    |(k, e)| (
                                        source_expr_to_expr(source_id, k),
                                        source_expr_to_expr(source_id, e),
                                    ),
                                )
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
                },
            };
        }

        match m {
            SourceModule::V1(m) => Module::V1(source_expr_to_expr(source_id, m)),
        }
    }

    let module = source_module_to_module(source_id, &source);
    let mut source_names = HashMap::new();
    source_names.insert(source_id, "(virtual)".to_string());
    let res = compile_importid(&gx, ImportStateId(0), module, vec![]).await;

    fn collect_errors(r: &CompileRes) -> Vec<ExprErr> {
        match &r.result {
            CompileResResult::Err(errs) => errs.clone(),
            CompileResResult::Ok(_) => vec![],
        }
    }

    let mut all_errors = vec![];
    for (_, r) in gx.compile_results.lock().unwrap().iter() {
        let Ok(r) = r.clone().await else {
            continue;
        };
        all_errors.extend(collect_errors(&r));
    }
    all_errors.extend(collect_errors(&res));
    if !all_errors.is_empty() {
        return Err(all_errors);
    }
    return Ok(res.logs);
}

#[derive(Aargvark)]
struct Args {
    // TODO abs spec instead of source itself
    root: AargvarkJson<SourceModule>,
}

#[tokio::main(flavor = "multi_thread")]
async fn main() {
    let args = vark::<Args>();
    setup_v8();
    let gx = new_gx();
    match compile(gx, SourceId(0), args.root.value).await {
        Ok(_logs) => { },
        Err(e) => {
            eprintln!("{}", serde_json::to_string_pretty(&e).unwrap());
            exit(1);
        },
    };
}
#[cfg(test)]
mod tests;
