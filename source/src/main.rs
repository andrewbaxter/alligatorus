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
                ExprSeq,
                ExprScopeUse,
                ExprSet,
                ExprStage,
                Module,
            },
            null::ValueNull,
            setup_v8,
            string::ValueString,
        },
        langhelp::{
            CompileRes,
            ExprErr,
            ExprSubErr,
            GlobalContext,
            ImportStateId,
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

async fn compile(gx: Arc<GlobalContext>, source_id: SourceId, source: SerialModule) -> Result<(), Vec<ExprErr>> {
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

    fn assemble_errors(out: &mut HashMap<String, HashMap<String, Vec<String>>>, r: &CompileRes) {
        let CompileRes::Err(errors) = r else {
            return;
        };
        for eval_err in errors {
            let out =
                out
                    .entry(eval_err.loc.0.0.to_string())
                    .or_default()
                    .entry(eval_err.loc.1.0.to_string())
                    .or_default();
            for err in &eval_err.errors {
                out.push(match err {
                    ExprSubErr::Opaque(err) => err.clone(),
                });
            }
        }
    }

    let mut errors = HashMap::new();
    for (_, r) in gx.compile_results.lock().unwrap().iter() {
        let Ok(r) = r.clone().await else {
            // Root module errors are handled by importer
            continue;
        };
        assemble_errors(&mut errors, &r);
    }
    assemble_errors(&mut errors, &res);
    return Ok(());
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
        Ok(()) => { },
        Err(e) => {
            eprintln!("{}", serde_json::to_string_pretty(&e).unwrap());
            exit(1);
        },
    };
}

#[cfg(test)]
mod test {
    use {
        crate::{
            compile,
            new_gx,
            eval::setup_v8,
            langhelp::SourceId,
            syntax::{
                ExprId,
                SerialExpr,
                SerialExprBind,
                SerialExprBindingUse,
                SerialExprCall,
                SerialExprField,
                SerialExprLower,
                SerialExprSequence,
                SerialExprStage,
                SerialExpr_,
                SerialLiteral,
                SerialModule,
            },
        },
        std::cell::Cell,
    };

    #[derive(Default)]
    struct ModuleBuilder {
        next_phrase_id: Cell<usize>,
    }

    impl ModuleBuilder {
        fn id(&self) -> ExprId {
            let id = self.next_phrase_id.get();
            self.next_phrase_id.set(id + 1);
            return ExprId(id);
        }

        fn lit_null(&self) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Literal(SerialLiteral::Null),
            }
        }

        fn lit_str(&self, s: &str) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Literal(SerialLiteral::String(s.to_string())),
            }
        }

        fn scope_use(&self, key: &str) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::ScopeUse(SerialExprBindingUse { key: Box::new(SerialExpr {
                    id: self.id(),
                    r#type: SerialExpr_::Literal(SerialLiteral::String(key.to_string())),
                }) }),
            };
        }

        fn field(&self, base: SerialExpr, key: SerialExpr) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Field(SerialExprField {
                    base: Box::new(base),
                    key: Box::new(key),
                }),
            };
        }

        fn call(&self, func: SerialExpr, args: SerialExpr) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Call(SerialExprCall {
                    func: Box::new(func),
                    arg: Box::new(args),
                }),
            };
        }

        fn seq(&self, exprs: Vec<SerialExpr>) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Sequence(SerialExprSequence { exprs: exprs }),
            };
        }

        fn bind(&self, key: &str, expr: SerialExpr) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Bind(SerialExprBind {
                    base: key.to_string(),
                    value: Box::new(expr),
                }),
            };
        }

        fn stage(&self, expr: SerialExpr) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Stage(SerialExprStage { expr: Box::new(expr) }),
            };
        }

        fn lower(&self, expr: SerialExpr) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Lower(SerialExprLower { expr: Box::new(expr) }),
            };
        }
    }

    #[tokio::test]
    async fn test_hello1() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(mb.call(mb.scope_use("print"), mb.lit_str("Hello world (mortar)"))),
        )
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn test_hello2() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.seq(
                    vec![
                        mb.bind("hello", mb.lit_str("Hello world (mortar)")),
                        mb.call(mb.scope_use("print"), mb.scope_use("hello"))
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn test_hello3() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.call(mb.scope_use("print"), mb.lower(mb.stage(mb.lit_str("Hello world (mortar)")))),
            ),
        )
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn test_hello4() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(mb.call(mb.scope_use("print"), mb.lower(mb.lit_str("Hello world (mortar)")))),
        )
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn test_hello5() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.call(
                    mb.scope_use("print"),
                    mb.call(mb.field(mb.lit_str("HELLO WORLD"), mb.lit_str("toLowerCase")), mb.lit_null()),
                ),
            ),
        )
            .await
            .unwrap();
    }
}
