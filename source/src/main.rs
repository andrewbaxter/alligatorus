pub mod artifact;
pub mod eval;
pub mod invariant;
pub mod langhelp;
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
                ExprCall,
                ExprConditionBranch,
                ExprField,
                ExprForeach,
                ExprGraft,
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
                ExprSeq,
                ExprSet,
                ExprStage,
                ExprTuple,
                Expr_,
                Module,
                make_lit_value,
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
            ExprSubErr,
            GlobalContext,
            ImportStateId,
            LogEntry,
            SourceId,
            Value,
        },
        syntax::{
            ExprId,
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

struct CliArtifacts(
    std::sync::Mutex<std::collections::HashMap<crate::artifact::ArtifactIdSource, crate::artifact::ArtifactBody>>,
    std::sync::Mutex<usize>,
);

impl crate::artifact::Artifacts for CliArtifacts {
    fn get(&self, id: crate::artifact::ArtifactIdSource) -> Result<crate::artifact::ArtifactBody, String> {
        self.0.lock().unwrap().get(&id).cloned().ok_or_else(|| format!("Unknown artifact: {:?}", id))
    }

    fn reserve_id(&self) -> crate::artifact::ArtifactIdSource {
        let mut next = self.1.lock().unwrap();
        let id = *next;
        *next += 1;
        crate::artifact::ArtifactIdSource(
            crate::langhelp::CompileId(crate::langhelp::ImportStateId(0), crate::langhelp::SourceId(0)),
            id,
        )
    }

    fn set(&self, id: crate::artifact::ArtifactIdSource, data: crate::artifact::ArtifactBody) {
        self.0.lock().unwrap().insert(id, data);
    }
}

struct CliSources(std::collections::HashMap<String, (crate::langhelp::SourceId, crate::syntax::SourceModule)>);

impl crate::langhelp::Sources for CliSources {
    fn get(
        &self,
        spec: SourceSpecAbsolute,
    ) -> Result<(crate::langhelp::SourceId, crate::syntax::SourceModule), String> {
        let SourceSpecAbsolute::Virtual(name) = spec;
        self.0.get(&name).cloned().ok_or_else(|| format!("Unknown source: {}", name))
    }
}

pub(crate) fn compile(gx: Arc<GlobalContext>, source: SourceSpecAbsolute) -> Result<CompileOutput, Vec<ExprErr>> {
    let (source_id, source_module) = gx.sources.get(source).map_err(|e| vec![ExprErr {
        loc: (SourceId(0), ExprId(0)),
        errors: vec![ExprSubErr::Opaque(e)],
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
        let guard = entry.read().unwrap();
        if let Some(r) = guard.as_ref() {
            all_errors.extend(collect_errors(r));
            all_scripts.extend(r.generated_dynamic_scripts.iter().cloned());
        }
    }
    all_errors.extend(collect_errors(&res));
    all_scripts.extend(res.generated_dynamic_scripts.iter().cloned());
    if !all_errors.is_empty() {
        return Err(all_errors);
    }
    return Ok(CompileOutput {
        logs: res.logs,
        generated_dynamic_scripts: all_scripts,
    });
}

#[derive(Debug)]
pub(crate) struct CompileOutput {
    pub(crate) generated_dynamic_scripts: Vec<String>,
    pub(crate) logs: Vec<LogEntry>,
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let Some(path) = args.get(1).cloned() else {
        eprintln!("usage: source <name>");
        std::process::exit(1);
    };
    let module: crate::syntax::SourceModule =
        serde_json::from_reader(std::io::stdin()).expect("failed to parse stdin as SourceModule json");
    let mut sources = std::collections::HashMap::new();
    sources.insert(path.clone(), (crate::langhelp::SourceId(0), module));
    let gx =
        GlobalContext::new(
            Box::new(CliSources(sources)),
            Box::new(CliArtifacts(Default::default(), std::sync::Mutex::new(0))),
        );
    match compile(gx, SourceSpecAbsolute::Virtual(path)) {
        Ok(out) => {
            for log in out.logs {
                println!("{}", log.message);
            }
            for script in out.generated_dynamic_scripts {
                println!("--- script ---");
                println!("{}", script);
            }
        },
        Err(errs) => {
            for e in errs {
                eprintln!("error at {:?}: {:?}", e.loc, e.errors);
            }
            std::process::exit(1);
        },
    }
}

pub(crate) fn source_module_to_module(source_id: SourceId, m: &SourceModule) -> Module {
    fn source_expr_to_expr(source_id: SourceId, m: &SourceExpr) -> Expr {
        return Expr {
            loc: (source_id, m.id),
            r#type: match &m.r#type {
                SourceExpr_::Literal(lit) => Expr_::Literal(make_lit_value(match lit {
                    SourceLiteral::Null => Rc::new(ValueNullConst) as Rc<dyn Value>,
                    SourceLiteral::String(l) => Rc::new(ValueStringConst(l.clone())) as Rc<dyn Value>,
                    SourceLiteral::Number(n) => Rc::new(ValueNumberConst(n.into_inner())) as Rc<dyn Value>,
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
                SourceExpr_::Seq(expr) => Expr_::Seq(
                    ExprSeq { exprs: expr.exprs.iter().map(|e| source_expr_to_expr(source_id, e)).collect() },
                ),
                SourceExpr_::Bind(expr) => Expr_::Bind(ExprBind {
                    base: Rc::new(Expr {
                        loc: (source_id, m.id),
                        r#type: Expr_::Literal(
                            make_lit_value(Rc::new(ValueStringConst(expr.base.clone())) as Rc<dyn Value>),
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
                    ExprGraft::Pending(Rc::new(source_expr_to_expr(source_id, &expr.expr))),
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
                SourceExpr_::Try(expr) => Expr_::Try(crate::eval::expr::ExprTry {
                    body: Rc::new(source_expr_to_expr(source_id, &expr.body)),
                    catch: expr.catch.as_ref().map(|c| crate::eval::expr::ExprTryCatch {
                        binding_name: Rc::new(source_expr_to_expr(source_id, &c.binding_name)),
                        exception_type: Rc::new(source_expr_to_expr(source_id, &c.exception_type)),
                        body: Rc::new(source_expr_to_expr(source_id, &c.body)),
                    }),
                    finally: expr.finally.as_ref().map(|f| Rc::new(source_expr_to_expr(source_id, f))),
                }),
            },
        };
    }

    match m {
        SourceModule::V1(m) => Module::V1(source_expr_to_expr(source_id, m)),
    }
}
