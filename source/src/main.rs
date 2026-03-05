use {
    crate::syntax::{
        Expr,
        ExprId,
        ExprType,
        ImportSpec,
        ImportSpecAbsolute,
        ImportSpecRemote,
        Module,
        PhraseCall,
        PhraseGet,
        PhraseSequence,
        ScopeId,
        Value,
    },
    flowcontrol::{
        shed,
        ta_return,
    },
    futures::{
        FutureExt,
        future::{
            BoxFuture,
            Remote,
            Shared,
        },
    },
    http::{
        Method,
        Uri,
    },
    serde::{
        Deserialize,
        Serialize,
        de::DeserializeOwned,
    },
    std::{
        cell::Cell,
        collections::{
            HashMap,
            HashSet,
            hash_map,
        },
        env::current_dir,
        path::PathBuf,
        pin::Pin,
        process::exit,
        rc::Rc,
        sync::{
            Arc,
            Mutex,
        },
    },
    tokio::{
        fs::{
            File,
            create_dir_all,
            read,
        },
        spawn,
    },
};

pub mod syntax;

fn path_to_fs_path(path: &Vec<String>) -> PathBuf {
    let mut at = PathBuf::new();
    for seg in path {
        at = at.join(seg);
    }
    return at;
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct ImportStateId(usize);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub struct CompileId(ImportStateId, ImportSpecAbsolute);

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Debug)]
pub enum CompileErr {
    Opaque(String),
}

fn cerr<T>(e: String) -> Result<T, CompileErr> {
    return Err(CompileErr::Opaque(e));
}

#[derive(Clone)]
pub struct ModuleResult {
    pub value: Value,
    pub errors: HashMap<ExprId, HashSet<CompileErr>>,
}

pub struct GlobalContext {
    pub cache_root: Vec<String>,
    // Remote packages/sources, resolved when downloaded (path is deterministic so not
    // returned, use a method to get the path)
    pub remote_source: Mutex<HashMap<ImportSpecRemote, Shared<BoxFuture<'static, Result<(), CompileErr>>>>>,
    pub module_results: Mutex<HashMap<CompileId, Shared<BoxFuture<'static, Result<ModuleResult, CompileErr>>>>>,
}

pub struct CompileModuleContext {
    pub import_spec: ImportSpecAbsolute,
    pub compile_path: Vec<(CompileId, ExprId)>,
    pub errors: HashMap<ExprId, HashSet<CompileErr>>,
}

pub struct Scope {
    values: HashMap<ScopeId, (BindingId, Value)>,
    parent: Option<Rc<Scope>>,
}

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash)]
struct BindingId(usize);

pub struct EvalContext {
    pub next_binding_id: BindingId,
}

pub struct PhraseContext {
    pub import_state: Rc<Cell<ImportStateId>>,
    pub scope: Rc<Scope>,
}

struct JsCodeSeq {
    pub sub: Vec<JsCode>,
}

struct JsCodeSet {
    pub id: BindingId,
    pub value: Box<JsCode>,
}

struct JsCodeGet {
    pub id: BindingId,
}

struct JsCodeCall {
    pub func: Box<JsCode>,
    pub args: Vec<JsCode>,
}

enum JsCode {
    Seq(JsCodeSeq),
    Value(serde_json::Value),
    Set(JsCodeSet),
    Get(JsCodeGet),
    Call(JsCodeCall),
}

pub struct EvalResult<EF> {
    pub effect: Option<EF>,
    pub value: Value,
}

trait Target {
    type Effect;
}
struct MortarTarget;

impl Target for MortarTarget {
    type Effect = JsCode;
}

async fn eval_phrase<
    T: Target,
>(
    gx: &Arc<GlobalContext>,
    cmx: &mut CompileModuleContext,
    ex: &mut EvalContext,
    px: PhraseContext,
    phrase: &Expr,
    target: &T,
) -> EvalResult<T::Effect> {
    match async {
        ta_return!(EvalResult < T:: Effect >, CompileErr);
        match &phrase.r#type {
            ExprType::Import(p) => {
                let abs_import_spec = match p {
                    ImportSpec::Relative(r) => {
                        fn join_path(path: &mut Vec<String>, rel: &String) -> Result<(), CompileErr> {
                            if path.pop().is_none() {
                                return cerr(
                                    format!(
                                        "Current package is a single file, cannot do a relative import (no other files in package)"
                                    ),
                                );
                            }
                            for seg in rel.split("/") {
                                match seg {
                                    ".." => {
                                        if path.pop().is_none() {
                                            return cerr(
                                                format!(
                                                    "Relative import path has too many .., goes out of containing package root",
                                                ),
                                            );
                                        }
                                    },
                                    "." => { },
                                    seg => {
                                        path.push(seg.to_string());
                                    },
                                }
                            }
                            return Ok(());
                        }

                        match &cmx.import_spec {
                            ImportSpecAbsolute::Virtual(m) => {
                                return cerr(
                                    "Virtual modules have no filesystem from which relative imports can be made".to_string(),
                                );
                            },
                            ImportSpecAbsolute::Local(s) => {
                                let mut path = s.clone();
                                join_path(&mut path, r)?;
                                ImportSpecAbsolute::Local(path)
                            },
                        //.                            ImportSpecAbsolute::Remote(s) => {
                        //.                                let mut subpath = s.subpath.clone();
                        //.                                join_path(&mut subpath, r)?;
                        //.                                ImportSpecAbsolute::Remote(ImportSpecRemote {
                        //.                                    hash: s.hash.clone(),
                        //.                                    path: s.path.clone(),
                        //.                                    subpath: subpath,
                        //.                                })
                        //.                            },
                        }
                    },
                    ImportSpec::Absolute(s) => s.clone(),
                };
                let compiled_id = CompileId(px.import_state.get(), abs_import_spec);
                for (i, p) in cmx.compile_path.iter().enumerate().rev() {
                    if p.0 == compiled_id {
                        return cerr(
                            format!("Import cycle: {}", serde_json::to_string(&cmx.compile_path[i..]).unwrap()),
                        );
                    }
                }
                let compiled = gx.module_results.lock().unwrap();
                let res;
                match compiled.entry(compiled_id) {
                    hash_map::Entry::Occupied(e) => {
                        res = e.get().clone().await?.value.clone();
                    },
                    hash_map::Entry::Vacant(e) => {
                        let f = {
                            let gx = gx.clone();
                            async move {
                                ta_return!(ModuleResult, CompileErr);
                                return Ok(compile_module(&gx, px.import_state.get(), abs_import_spec, {
                                    let mut compile_path = cmx.compile_path.clone();
                                    compile_path.push((compiled_id, phrase.id));
                                    compile_path
                                }).await?);
                            }
                        }.boxed().shared();
                        spawn(f.clone());
                        e.insert(f.clone());
                        res = f.await?.value.clone();
                    },
                };
                return Ok(EvalResult {
                    effect: None,
                    value: res,
                });
            },
            ExprType::Literal(p) => {
                return Ok(EvalResult {
                    effect: None,
                    value: p.clone(),
                });
            },
            ExprType::Set(p) => {
                let value = eval_phrase(gx, cmx, ex, PhraseContext {
                    import_state: px.import_state.clone(),
                    scope: px.scope.clone(),
                }, &p.value, target).await;
                let binding_id = ex.binding_ids;
                px.scope.set(p.id, binding_id);
                return Ok(EvalResult {
                    effect: JsCode::Set(JsCodeSet {
                        id: binding_id,
                        value: value.effect,
                    }),
                    value: value.value,
                });
            },
            ExprType::Get(p) => {
                let (binding_id, value) = shed!{
                    'found _;
                    let mut at = &px.scope;
                    loop {
                        if let Some(found) = at.values.get(&p.id) {
                            break 'found found;
                        }
                        if let Some(parent) = &at.parent {
                            at = parent;
                        }
                        break;
                    }
                    return cerr(format!("No bound value with id [{}]", p.id.0));
                };
                let eff = match target.eval_get(binding_id) {
                    Ok(eff) => eff,
                    Err(e) => {
                        return Err(e);
                    },
                };
                return Ok(EvalResult {
                    effect: eff,
                    value: value.clone(),
                });
            },
            ExprType::Call(p) => {
                let func = eval_phrase(gx, cmx, ex, PhraseContext {
                    import_state: px.import_state.clone(),
                    scope: px.scope.clone(),
                }, &p.func, target).await;
                let value = eval_phrase(gx, cmx, ex, PhraseContext {
                    import_state: px.import_state,
                    scope: px.scope.clone(),
                }, &p.args, target).await;
                let Value::Func(func) = func else {
                    return Err("Call phrase function operand is not a function");
                };
                return func.call(target, value);
            },
        }
    }.await {
        Ok(r) => return Ok(r),
        Err(e) => {
            cmx.errors.insert(phrase.id, e);
            return Ok(EvalResult {
                effect: None,
                value: Value::Err,
            });
        },
    }
}

async fn compile_module(
    gx: &Arc<GlobalContext>,
    import_state: ImportStateId,
    import_spec: ImportSpecAbsolute,
    compile_path: Vec<(CompileId, ExprId)>,
) -> Result<ModuleResult, CompileErr> {
    let cmx = CompileModuleContext {
        import_spec: import_spec,
        compile_path: compile_path,
        errors: Default::default(),
    };
    let module = match &cmx.import_spec {
            ImportSpecAbsolute::Local(s) => {
                serde_json::from_slice::<Module>(
                    &read(path_to_fs_path(&s))
                        .await
                        .map_err(|e| CompileErr::Opaque(format!("Error reading module file at {:?}: {}", s, e)))?,
                ).map_err(|e| CompileErr::Opaque(format!("Error parsing module file at {:?}: {}", s, e)))?
            },
        //.        ImportSpecAbsolute::Remote(s) => {
        //.            let remote = gx.remote_source.lock().unwrap();
        //.            let mut cache_path = gx.cache_root.clone();
        //.            cache_path.extend(
        //.                [
        //.                    format!("remote_source"),
        //.                    urlencoding::encode(&serde_json_canonicalizer::to_string(s).unwrap()).to_string(),
        //.                ].into_iter(),
        //.            );
        //.            match remote.entry(s.clone()) {
        //.                hash_map::Entry::Occupied(e) => {
        //.                    e.get().clone().await?;
        //.                    read_json::<Module>(&cache_path).await?
        //.                },
        //.                hash_map::Entry::Vacant(e) => {
        //.                    let fs_cache_path = path_to_fs_path(&cache_path);
        //.                    let f = {
        //.                        let fs_cache_path = fs_cache_path.clone();
        //.                        async move {
        //.                            ta_return!((), Errx);
        //.                            if fs_cache_path.exists() {
        //.                                return Ok(());
        //.                            }
        //.                            match s.path {
        //.                                syntax::RemotePath::Http(p) => {
        //.                                    let url = Uri::try_from(&p.url)?;
        //.                                    let l = htreq::Limits::default();
        //.                                    let mut conn = htreq::connect(l, &url).await?;
        //.                                    let log = loga::Log::new_root(loga::ERR);
        //.                                    let (code, headers, tail) =
        //.                                        htreq::send(
        //.                                            &log,
        //.                                            l,
        //.                                            &mut conn,
        //.                                            http::Request::builder()
        //.                                                .method(Method::GET)
        //.                                                .uri(&url)
        //.                                                .body(body_empty())
        //.                                                .unwrap(),
        //.                                        ).await?;
        //.                                    if let Some(parent) = fs_cache_path.parent() {
        //.                                        create_dir_all(parent).await?;
        //.                                    }
        //.                                    let mut out = File::create_new(fs_cache_path).await?;
        //.                                    htreq::receive_stream(tail, &mut out).await?;
        //.                                },
        //.                            }
        //.                            return Ok(());
        //.                        }
        //.                    }.boxed().shared();
        //.                    spawn(f.clone());
        //.                    e.insert(f.clone());
        //.                    f.await?;
        //.                    let f = File::open(&fs_cache_path).await?;
        //.                    let fa = zip::ZipArchive::new(&f)?;
        //.                    serde_json::from_slice::<Module>(&fa.by_name(s.subpath.join("/"))?.read())?
        //.                },
        //.            };
        //.        },
        };

    // Prep scope
    let mut s1_scope = HashMap::new();
    let s2_scope = &mut {
        let isolate = &mut v8::Isolate::new(Default::default());
        let handlescope = std::pin::pin!(v8::HandleScope::new(isolate));
        let handlescope = &mut handlescope.init();
        let context = v8::Context::new(handlescope, Default::default());
        v8::ContextScope::new(handlescope, context)
    };
    let mut next_binding_id = 0;
    for (
        id,
        s1,
        s2,
    ) in [
        (
            Value::Func(x),
            v8::Function::builder(
                |
                    scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_>>,
                    args: v8::FunctionCallbackArguments<'_>,
                    ret: v8::ReturnValue<'_>,
                | {
                    println!("{}", args.get(0).to_string(scope).unwrap().to_rust_string_lossy(scope));
                },
            ).build(s2_scope).unwrap(),
        ),
    ] {
        s1_scope.insert(id, s1);
        let binding_id = next_binding_id;
        next_binding_id += 1;
        s2_scope
            .get_current_context()
            .global(s2_scope)
            .set(s2_scope, v8::String::new(s2_scope, &format!("v{}", binding_id)).unwrap().cast(), s2);
    }
    let mut ex = EvalContext { next_binding_id: BindingId(next_binding_id) };
    let stage1 = match module {
        Module::V1(root_expr) => {
            eval_phrase(
                //. .
                gx,
                &mut cmx,
                &mut ex,
                PhraseContext {
                    import_state: import_state,
                    scope: Rc::new(Scope {
                        values: s1_scope,
                        parent: (),
                    }),
                },
                // Inject synthetic seq to wrap root to create a RW scope
                &Expr {
                    id: root_expr.root.id,
                    r#type: ExprType::Sequence(PhraseSequence { exprs: vec![root_expr] }),
                },
                &MortarTarget,
            ).await
        },
    };
    let value = if let Some(code) = stage1.effect {
        fn render_code(out: &mut Vec<String>, code: &JsCode) {
            match code {
                JsCode::Seq(c) => {
                    for x in &c.sub {
                        render_code(out, x);
                    }
                },
                JsCode::Value(c) => {
                    out.push(serde_json::to_string(&c).unwrap());
                },
                JsCode::Set(c) => {
                    out.push(format!("v{} = ", c.id.0));
                    render_code(out, &c.value);
                },
                JsCode::Get(c) => {
                    out.push(format!("v{}", c.id.0));
                },
                JsCode::Call(c) => {
                    out.push(format!("("));
                    out.push(format!("("));
                    render_code(out, &c.func);
                    out.push(format!(")"));
                    out.push(format!("("));
                    for arg in &c.args {
                        render_code(out, arg);
                        out.push(format!(","));
                    }
                    out.push(format!(")"));
                    out.push(format!(")"));
                },
            }
        }

        let mut code1 = vec![];
        render_code(&mut code1, &code);
        let code = v8::String::new(s2_scope, &code1.join("\n")).unwrap();
        let script = v8::Script::compile(s2_scope, code, None).unwrap();
        let stage2 = script.run(s2_scope).unwrap();
        stage1.value.read_stage2(stage2)?
    } else {
        stage1.value
    };
    return Ok(ModuleResult {
        value: value,
        errors: cmx.errors,
    });
}

#[tokio::main(flavor = "multi_thread")]
async fn main() {
    let platform = v8::new_default_platform(0, false).make_shared();
    v8::V8::initialize_platform(platform);
    v8::V8::initialize();
    let gx = Arc::new(GlobalContext {
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
        remote_source: Default::default(),
        module_results: Default::default(),
    });

    struct ModuleBuilder {
        next_phrase_id: Cell<usize>,
    }

    impl ModuleBuilder {
        fn id(&self) -> ExprId {
            let id = self.next_phrase_id.get();
            self.next_phrase_id.set(id + 1);
            return ExprId(id);
        }

        fn lit_str(&self, s: &str) -> Expr {
            return Expr {
                id: self.id(),
                r#type: ExprType::Literal(Value::String(s.to_string())),
            }
        }

        fn get(&self, id: &str) -> Expr {
            return Expr {
                id: self.id(),
                r#type: ExprType::Get(PhraseGet { id: ScopeId(id.to_string()) }),
            };
        }

        fn call(&self, func: Expr, args: Expr) -> Expr {
            return Expr {
                id: self.id(),
                r#type: ExprType::Call(PhraseCall {
                    func: Box::new(func),
                    args: Box::new(args),
                }),
            };
        }
    }

    let mb = ModuleBuilder { next_phrase_id: Cell::new(0) };
    let root_spec =
        ImportSpecAbsolute::Virtual(
            Box::new(Module::V1(mb.call(mb.get("print"), mb.lit_str("Hello world (mortar)")))),
        );
    let res = compile_module(&gx, ImportStateId(0), root_spec.clone(), vec![]).await;

    fn print_errors(i: &ImportSpecAbsolute, r: &ModuleResult) -> bool {
        for (loc, errs) in &r.errors {
            for err in errs {
                eprintln!("Error at {}:{}: {:?}", match i {
                    ImportSpecAbsolute::Virtual(_) => "(virtual)".to_string(),
                    ImportSpecAbsolute::Local(path) => format!("{:?}", path),
                }, loc.0, match err {
                    CompileErr::Opaque(v) => v,
                });
            }
        }
        return !r.errors.is_empty();
    }

    for (i, r) in gx.module_results.lock().unwrap().iter() {
        let Ok(r) = r.clone().await else {
            // Root module errors are handled by importer
            continue;
        };
        print_errors(&i.1, &r);
    }
    let failed;
    match res {
        Ok(r) => {
            if print_errors(&root_spec, &r) {
                failed = true;
            } else {
                println!("{}", serde_json::to_string(&r.value).unwrap());
                failed = false;
            }
        },
        Err(e) => {
            eprintln!("Root error: {:?}", e);
            failed = true;
        },
    };
    if failed {
        exit(1);
    }
}
