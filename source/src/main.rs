pub mod jscode;
pub mod langhelp;
pub mod syntax;
pub mod builtin;
pub mod eval;

use {
    crate::{
        eval::{
            compile_importid,
            expr::{
                Expr,
                Expr_,
                ExprBind,
                ExprCall,
                ExprField,
                ExprLower,
                ExprRecord,
                ExprSeq,
                ExprScopeUse,
                ExprSet,
                ExprStage,
                ExprTuple,
                Module,
            },
            null::ValueNull,
            setup_v8,
            string::ValueString,
        },
        langhelp::{
            CompileRes,
            CompileResResult,
            ExprErr,
            ExprSubErr,
            GlobalContext,
            ImportStateId,
            LogEntry,
            SerialValueSource,
            SourceId,
            Unique,
        },
        syntax::{
            SerialExpr,
            SerialExpr_,
            SerialLiteral,
            SerialModule,
        },
    },
    aargvark::{
        Aargvark,
        traits_impls::AargvarkJson,
        vark,
    },
    std::{
        collections::HashMap,
        env::current_dir,
        process::exit,
        rc::Rc,
        sync::Arc,
    },
};

fn new_gx() -> Arc<GlobalContext> {
    let mut builtin_unique = {
        let mut next = 0;
        move || {
            let v = next;
            next += 1;
            return Unique(SerialValueSource::Builtin, v);
        }
    };
    let gx = Arc::new(GlobalContext {
        artifacts: Default::default(),
        cache_root: {
            let fs_cwd = current_dir().unwrap();
            let fs_cwd = match fs_cwd.to_str() {
                Some(c) => c,
                None => {
                    panic!("Non-string current path [{:?}], required for cache path", fs_cwd);
                },
            };
            let mut out = vec![];
            for c in fs_cwd.split("/") {
                out.push(c.to_string());
            }
            out
        },
        compile_results: Default::default(),
        const_aspect_mortar_func: builtin_unique(),
        const_aspect_unique: builtin_unique(),
        const_aspect_func: builtin_unique(),
        const_aspect_artifact: builtin_unique(),
        const_aspect_mortar_from_js: builtin_unique(),
        const_aspect_mortar_null: builtin_unique(),
        const_aspect_mortar_str: builtin_unique(),
    });
    return gx;
}

async fn compile(
    gx: Arc<GlobalContext>,
    source_id: SourceId,
    source: SerialModule,
) -> Result<Vec<LogEntry>, Vec<ExprErr>> {
    eprintln!("code {:#?}", source);

    fn serial_module_to_module(source_id: SourceId, m: &SerialModule) -> Module {
        fn serial_expr_to_expr(source_id: SourceId, m: &SerialExpr) -> Expr {
            return Expr {
                loc: (source_id, m.id),
                r#type: match &m.r#type {
                    SerialExpr_::Literal(lit) => Expr_::Literal(match lit {
                        SerialLiteral::Null => Rc::new(|| Box::new(ValueNull)),
                        SerialLiteral::String(l) => {
                            let l = l.clone();
                            Rc::new(move || Box::new(ValueString(l.clone())))
                        },
                    }),
                    SerialExpr_::ScopeUse(expr) => Expr_::ScopeUse(
                        ExprScopeUse { key: Rc::new(serial_expr_to_expr(source_id, &expr.key)) },
                    ),
                    SerialExpr_::Call(expr) => Expr_::Call(ExprCall {
                        func: Rc::new(serial_expr_to_expr(source_id, &expr.func)),
                        arg: Rc::new(serial_expr_to_expr(source_id, &expr.arg)),
                    }),
                    SerialExpr_::Set(expr) => Expr_::Set(ExprSet {
                        base: Rc::new(serial_expr_to_expr(source_id, &expr.base)),
                        value: Rc::new(serial_expr_to_expr(source_id, &expr.value)),
                    }),
                    SerialExpr_::Field(expr) => Expr_::Field(ExprField {
                        base: Rc::new(serial_expr_to_expr(source_id, &expr.base)),
                        key: Rc::new(serial_expr_to_expr(source_id, &expr.key)),
                    }),
                    SerialExpr_::Sequence(expr) => Expr_::Seq(
                        ExprSeq { seq: expr.exprs.iter().map(|e| serial_expr_to_expr(source_id, e)).collect() },
                    ),
                    SerialExpr_::Bind(expr) => Expr_::Bind(ExprBind {
                        base: Rc::new(Expr {
                            loc: (source_id, m.id),
                            r#type: Expr_::Literal({
                                let base = expr.base.clone();
                                Rc::new(move || Box::new(ValueString(base.clone())))
                            }),
                        }),
                        value: Rc::new(serial_expr_to_expr(source_id, &expr.value)),
                    }),
                    SerialExpr_::Stage(expr) => Expr_::Stage(
                        ExprStage { expr: Rc::new(serial_expr_to_expr(source_id, &expr.expr)) },
                    ),
                    SerialExpr_::Lower(expr) => Expr_::Lower(
                        ExprLower { expr: Rc::new(serial_expr_to_expr(source_id, &expr.expr)) },
                    ),
                    SerialExpr_::Tuple(exprs) => Expr_::Tuple(
                        ExprTuple { elements: exprs.iter().map(|e| serial_expr_to_expr(source_id, e)).collect() },
                    ),
                    SerialExpr_::Record(entries) => Expr_::Record(
                        ExprRecord { entries: entries.iter().map(|(k, e)| (serial_expr_to_expr(source_id, k), serial_expr_to_expr(source_id, e))).collect() },
                    ),
                },
            };
        }

        match m {
            SerialModule::V1(m) => Module::V1(serial_expr_to_expr(source_id, m)),
        }
    }

    let module = serial_module_to_module(source_id, &source);
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
    root: AargvarkJson<SerialModule>,
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
