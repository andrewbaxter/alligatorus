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

        fn null(&self) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Literal(SerialLiteral::Null),
            }
        }

        fn str(&self, s: &str) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Literal(SerialLiteral::String(s.to_string())),
            }
        }

        fn tuple(&self, values: Vec<SerialExpr>) -> SerialExpr {
            return SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Tuple(values),
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
    async fn test_00100_call_lit() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        let logs =
            compile(
                gx,
                SourceId(0),
                SerialModule::V1(mb.call(mb.scope_use("log"), mb.str("Hello world (mortar)"))),
            )
                .await
                .unwrap();
        assert_eq!(logs.len(), 1);
        assert_eq!(logs[0].message, "Hello world (mortar)");
    }

    #[tokio::test]
    async fn test_00101_type_error() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        compile(gx, SourceId(0), SerialModule::V1(mb.call(mb.scope_use("log"), mb.null())))
            .await
            .unwrap_err();
    }

    #[tokio::test]
    async fn test_00200_bind_use() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        let logs =
            compile(
                gx,
                SourceId(0),
                SerialModule::V1(
                    mb.seq(
                        vec![
                            mb.bind("hello", mb.str("Hello world (mortar)")),
                            mb.call(mb.scope_use("log"), mb.scope_use("hello"))
                        ],
                    ),
                ),
            )
                .await
                .unwrap();
        assert_eq!(logs.len(), 1);
        assert_eq!(logs[0].message, "Hello world (mortar)");
    }

    #[tokio::test]
    async fn test_00300_stage_lower() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        let logs =
            compile(
                gx,
                SourceId(0),
                SerialModule::V1(
                    mb.call(mb.scope_use("log"), mb.lower(mb.stage(mb.str("Hello world (mortar)")))),
                ),
            )
                .await
                .unwrap();
        assert_eq!(logs.len(), 1);
        assert_eq!(logs[0].message, "Hello world (mortar)");
    }

    #[tokio::test]
    async fn test_00400_lower_nostage() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        let logs =
            compile(
                gx,
                SourceId(0),
                SerialModule::V1(mb.call(mb.scope_use("log"), mb.lower(mb.str("Hello world (mortar)")))),
            )
                .await
                .unwrap();
        assert_eq!(logs.len(), 1);
        assert_eq!(logs[0].message, "Hello world (mortar)");
    }

    #[tokio::test]
    async fn test_00500_method_call() {
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.call(
                    mb.scope_use("log"),
                    mb.call(mb.field(mb.str("HELLO WORLD"), mb.str("toLowerCase")), mb.null()),
                ),
            ),
        )
            .await
            .unwrap();
    }

    #[tokio::test]
    async fn test_00600define_func() {
        //! Define a function that prints "hello", then call it.
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        let logs = compile(gx, SourceId(0), SerialModule::V1(mb.seq(vec![
            // The function must be defined at a higher evaluation level, because (in the
            // future) the exact function signature isn't known until after `define` is called
            // in javascript, so type checking can't occur during half evaluation.
            //
            // So the code to generate the function is run, then after running the exact type
            // is "lifted" from the `lowered` code's result value.
            mb.bind("hello", mb.lower(mb.seq(vec![
                // Returns a `DefinitionSet`. The ownership of this will need to be transfered to
                // javascript. It keeps a counter for ids, and a mapping of all definitions to
                // their implementations (Option). Definition references are bi-directional so use
                // dumpster's GC ref.
                //
                // `define` is a rust function placed in javascript, and called via javascript. It
                // creates a new object with `define` and `new_func` methods. The
                // `Gc<DefinitionSet>` is shared between them.
                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                // `new_func` creates a `DefinitionSetFunction` in the definition set and returns
                // an object with `func` (the function ref) and `builder` (a FunctionBuilder with
                // an `impl` method).
                mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.tuple(vec![]))),
                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                // The result's `impl` method takes a staged expr and stores it as the
                // implementation for the function. Error if already has a definition.
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                    mb.tuple(vec![mb.stage(mb.call(mb.scope_use("log"), mb.str("hello")))])
                ),
                // Finally, return the function.
                //
                // `MortarTypeDefinitionSetFunction`'s aspect lift method will extract the
                // `DefinitionSetFunction` from the script return value.
                //
                // First it will run a "finish" procedure on the definition set that confirms that
                // all functions have implementations, or else return an error.
                //
                // Then it returns a value where the value type is the `DefinitionSetFunction`
                // itself (which implements `Type` and aspects similar to `MortarTypeFunction`),
                // and the value data is a new MortarValue data variant, `DefinitionSet`, with the
                // definition set id and definition id.
                mb.scope_use("f")
            ]))),
            // `DefinitionSetFunction`'s type and aspect methods should ensure that its
            // definition set is registered with the evaluation context, so that when half
            // evaluation is done the evaluation context has a list of all definition sets
            // used in the javascript to be executed. All definitions in all the definition
            // sets should have their implementations rendered into functions and bound to
            // javascript variables derived from definition set and definition id (reserve
            // scope ids, and keep the scope id in a lookup).
            //
            // Then, when calling a `DefinitionSetFunction` value, lookup the scope entry
            // using the definition set/definition id.
            mb.call(mb.scope_use("hello"), mb.tuple(vec![])),
        ]))).await.unwrap();
        assert_eq!(logs.len(), 1);
        assert_eq!(logs[0].message, "hello");
    }

    #[tokio::test]
    async fn test_00700define_func_arg_ret() {
        //! Define a function with string arg and string ret that uppercases the arg,
        //! call it with a string, and log the result.
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        let logs = compile(gx, SourceId(0), SerialModule::V1(mb.seq(vec![
            mb.bind("my_func", mb.lower(mb.seq(vec![
                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.tuple(vec![]))),
                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                // Set arg type to String
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("arg")),
                    mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
                ),
                // Set ret type to String
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("ret")),
                    mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
                ),
                // Impl: call toUpperCase on arg and return
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                    mb.tuple(vec![mb.stage(
                        mb.call(
                            mb.field(mb.scope_use("arg"), mb.str("toUpperCase")),
                            mb.null(),
                        ),
                    )]),
                ),
                mb.scope_use("f"),
            ]))),
            mb.call(
                mb.scope_use("log"),
                mb.call(mb.scope_use("my_func"), mb.tuple(vec![mb.str("hello world")])),
            ),
        ]))).await.unwrap();
        assert_eq!(logs.len(), 1);
        assert_eq!(logs[0].message, "HELLO WORLD");
    }

    #[tokio::test]
    async fn test_00800define_data() {
        //! Define an "uppercase string" data type with string as the underlying type,
        //! a "to_string" method that uppercases self, construct one, call to_string, and log.
        setup_v8();
        let gx = new_gx();
        let mb = ModuleBuilder::default();
        let logs = compile(gx, SourceId(0), SerialModule::V1(mb.seq(vec![
            // Define the data type inside a lower block, return the constructor
            mb.bind("make_upper", mb.lower(mb.seq(vec![
                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.tuple(vec![]))),
                // Set underlying type to String
                mb.call(
                    mb.field(mb.scope_use("ndr"), mb.str("set_underlying")),
                    mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
                ),
                // Define to_string method
                mb.bind("mnfr", mb.call(
                    mb.field(mb.scope_use("ndr"), mb.str("new_func")),
                    mb.str("to_string"),
                )),
                mb.call(
                    mb.field(mb.scope_use("mnfr"), mb.str("ret")),
                    mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
                ),
                mb.call(
                    mb.field(mb.scope_use("mnfr"), mb.str("impl")),
                    mb.tuple(vec![mb.stage(
                        mb.call(
                            mb.field(mb.scope_use("self"), mb.str("toUpperCase")),
                            mb.null(),
                        ),
                    )]),
                ),
                // Return the constructor
                mb.field(mb.scope_use("ndr"), mb.str("new")),
            ]))),
            // Create an instance and call to_string
            mb.bind("val", mb.call(
                mb.scope_use("make_upper"),
                mb.tuple(vec![mb.str("hello")]),
            )),
            mb.call(
                mb.scope_use("log"),
                mb.call(
                    mb.field(mb.scope_use("val"), mb.str("to_string")),
                    mb.null(),
                ),
            ),
        ]))).await.unwrap();
        assert_eq!(logs.len(), 1);
        assert_eq!(logs[0].message, "HELLO");
    }
}
