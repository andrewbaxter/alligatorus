use {
    crate::{
        artifact::{
            ArtifactBody,
            ArtifactIdSource,
            Artifacts,
        },
        langhelp::{
            CompileId,
            GlobalContext,
            ImportStateId,
            SourceId,
            Sources,
        },
        syntax::{
            ExprId,
            SerialExprIfConditionBranch,
            SerialExprMatchTagBranch,
            SourceExpr,
            SourceExprAs,
            SourceExprBind,
            SourceExprBindingUse,
            SourceExprCall,
            SourceExprField,
            SourceExprForeach,
            SourceExprGraft,
            SourceExprIf,
            SourceExprImport,
            SourceExprLower,
            SourceExprMatch,
            SourceExprOperatorAdd,
            SourceExprOperatorEq,
            SourceExprOperatorLt,
            SourceExprSeq,
            SourceExprSet,
            SourceExprStage,
            SourceExprTry,
            SourceExprTryCatch,
            SourceExpr_,
            SourceLiteral,
            SourceModule,
            SourceSpecAbsolute,
        },
    },
    std::{
        cell::Cell,
        collections::{
            BTreeMap,
            HashMap,
        },
        sync::{
            Arc,
            Mutex,
        },
    },
};

#[derive(Default)]
pub(super) struct ModuleBuilder {
    next_phrase_id: Cell<usize>,
}

impl ModuleBuilder {
    pub fn as_(&self, base: SourceExpr, target: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::As(SourceExprAs {
                base: Box::new(base),
                target: Box::new(target),
            }),
        };
    }

    pub fn bind(&self, key: &str, expr: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Bind(SourceExprBind {
                base: key.to_string(),
                value: Box::new(expr),
            }),
        };
    }

    #[allow(dead_code)]
    pub fn bool(&self, v: bool) -> SourceExpr {
        if v {
            return self.operator_lt(self.num(0.), self.num(1.));
        } else {
            return self.operator_lt(self.num(1.), self.num(0.));
        }
    }

    #[allow(dead_code)]
    pub fn builtin(&self, path: &[&str]) -> SourceExpr {
        let mut expr = self.scope_use("builtins");
        for segment in path {
            expr = self.field(expr, self.str(segment));
        }
        return expr;
    }

    pub fn call(&self, func: SourceExpr, args: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Call(SourceExprCall {
                func: Box::new(func),
                arg: Box::new(args),
            }),
        };
    }

    #[allow(dead_code)]
    pub fn define(&self) -> SourceExpr {
        return self.builtin(&["define"]);
    }

    #[allow(dead_code)]
    pub fn eval_aspect_field(&self) -> SourceExpr {
        return self.builtin(&["eval", "aspect_field"]);
    }

    #[allow(dead_code)]
    pub fn eval_aspect_field_id(&self) -> SourceExpr {
        return self.builtin(&["eval", "aspect_field_id"]);
    }

    #[allow(dead_code)]
    pub fn eval_aspect_function(&self) -> SourceExpr {
        return self.builtin(&["eval", "aspect_function"]);
    }

    #[allow(dead_code)]
    pub fn eval_aspect_function_id(&self) -> SourceExpr {
        return self.builtin(&["eval", "aspect_function_id"]);
    }

    #[allow(dead_code)]
    pub fn eval_aspect_lookup(&self) -> SourceExpr {
        return self.builtin(&["eval", "aspect_lookup"]);
    }

    #[allow(dead_code)]
    pub fn eval_aspect_string_dynamic(&self) -> SourceExpr {
        return self.builtin(&["eval", "aspect_string_dynamic"]);
    }

    #[allow(dead_code)]
    pub fn eval_binding(&self) -> SourceExpr {
        return self.builtin(&["eval", "binding"]);
    }

    #[allow(dead_code)]
    pub fn eval_cast_value(&self) -> SourceExpr {
        return self.builtin(&["eval", "cast_value"]);
    }

    #[allow(dead_code)]
    pub fn eval_code_dyn(&self) -> SourceExpr {
        return self.builtin(&["eval", "code_dyn"]);
    }

    #[allow(dead_code)]
    pub fn eval_codevalue(&self) -> SourceExpr {
        return self.builtin(&["eval", "codevalue"]);
    }

    #[allow(dead_code)]
    pub fn eval_const_eval(&self) -> SourceExpr {
        return self.builtin(&["eval", "const_eval"]);
    }

    #[allow(dead_code)]
    pub fn eval_constkey_to_str(&self) -> SourceExpr {
        return self.builtin(&["eval", "constkey_to_str"]);
    }

    #[allow(dead_code)]
    pub fn eval_gen_binding_name(&self) -> SourceExpr {
        return self.builtin(&["eval", "gen_binding_name"]);
    }

    #[allow(dead_code)]
    pub fn eval_new_expr_context(&self) -> SourceExpr {
        return self.builtin(&["eval", "new_expr_context"]);
    }

    #[allow(dead_code)]
    pub fn eval_str_to_json(&self) -> SourceExpr {
        return self.builtin(&["eval", "str_to_json"]);
    }

    #[allow(dead_code)]
    pub fn eval_target(&self) -> SourceExpr {
        return self.builtin(&["eval", "target"]);
    }

    pub fn field(&self, base: SourceExpr, key: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Field(SourceExprField {
                base: Box::new(base),
                key: Box::new(key),
            }),
        };
    }

    pub fn foreach(&self, base: SourceExpr, element_key: SourceExpr, body: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Foreach(SourceExprForeach {
                base: Box::new(base),
                element_key: Box::new(element_key),
                body: Box::new(body),
            }),
        }
    }

    pub fn graft(&self, expr: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Graft(SourceExprGraft { expr: Box::new(expr) }),
        };
    }

    pub fn id(&self) -> ExprId {
        let id = self.next_phrase_id.get();
        self.next_phrase_id.set(id + 1);
        return ExprId(id);
    }

    pub fn if_(&self, cases: Vec<(SourceExpr, SourceExpr)>, else_: Option<SourceExpr>) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::If(SourceExprIf {
                conditions: cases.into_iter().map(|x| SerialExprIfConditionBranch {
                    condition: x.0,
                    body: x.1,
                }).collect(),
                fallback: else_.map(Box::new),
            }),
        };
    }

    pub fn import(&self, spec: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Import(SourceExprImport { spec: Box::new(spec) }),
        };
    }

    #[allow(dead_code)]
    pub fn log(&self) -> SourceExpr {
        return self.builtin(&["log"]);
    }

    pub fn lower(&self, expr: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Lower(SourceExprLower { expr: Box::new(expr) }),
        };
    }

    pub fn match_(
        &self,
        base: SourceExpr,
        element_key: SourceExpr,
        cases: Vec<(SourceExpr, SourceExpr)>,
        fallback: Option<SourceExpr>,
    ) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Match(SourceExprMatch {
                base: Box::new(base),
                element_key: Box::new(element_key),
                cases: cases.into_iter().map(|x| SerialExprMatchTagBranch {
                    condition: x.0,
                    body: x.1,
                }).collect(),
                fallback: fallback.map(Box::new),
            }),
        }
    }

    pub fn null(&self) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Literal(SourceLiteral::Null),
        }
    }

    pub fn num(&self, v: f64) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Literal(SourceLiteral::Number(v.try_into().unwrap())),
        }
    }

    pub fn operator_add(&self, base: SourceExpr, reference: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::OperatorAdd(SourceExprOperatorAdd {
                base: Box::new(base),
                reference: Box::new(reference),
            }),
        };
    }

    #[allow(dead_code)]
    pub fn operator_eq(&self, base: SourceExpr, reference: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::OperatorEq(SourceExprOperatorEq {
                base: Box::new(base),
                reference: Box::new(reference),
            }),
        };
    }

    pub fn operator_lt(&self, base: SourceExpr, reference: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::OperatorLt(SourceExprOperatorLt {
                base: Box::new(base),
                reference: Box::new(reference),
            }),
        };
    }

    pub fn record(&self, entries: Vec<(&str, SourceExpr)>) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Record(entries.into_iter().map(|(k, v)| (self.str(k), v)).collect()),
        };
    }

    pub fn scope_use(&self, key: &str) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::ScopeUse(SourceExprBindingUse { key: Box::new(SourceExpr {
                id: self.id(),
                r#type: SourceExpr_::Literal(SourceLiteral::String(key.to_string())),
            }) }),
        };
    }

    pub fn seq(&self, exprs: Vec<SourceExpr>) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Seq(SourceExprSeq { exprs: exprs }),
        };
    }

    pub fn set(&self, base: SourceExpr, expr: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Set(SourceExprSet {
                base: Box::new(base),
                value: Box::new(expr),
            }),
        };
    }

    pub fn stage(&self, expr: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Stage(SourceExprStage { expr: Box::new(expr) }),
        };
    }

    pub fn str(&self, s: &str) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Literal(SourceLiteral::String(s.to_string())),
        }
    }

    #[allow(dead_code)]
    pub fn throw(&self, value: SourceExpr) -> SourceExpr {
        return self.call(self.builtin(&["throw"]), value);
    }

    #[allow(dead_code)]
    pub fn try_(
        &self,
        body: SourceExpr,
        catch: Option<(&str, SourceExpr, SourceExpr)>,
        finally: Option<SourceExpr>,
    ) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Try(SourceExprTry {
                body: Box::new(body),
                catch: catch.map(|(name, exc_type, body)| SourceExprTryCatch {
                    binding_name: Box::new(self.str(name)),
                    exception_type: Box::new(exc_type),
                    body: Box::new(body),
                }),
                finally: finally.map(Box::new),
            }),
        };
    }

    pub fn tuple(&self, values: Vec<SourceExpr>) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Tuple(values),
        }
    }

    #[allow(dead_code)]
    pub fn types_any(&self) -> SourceExpr {
        return self.builtin(&["types", "any"]);
    }

    #[allow(dead_code)]
    pub fn types_aspect(&self) -> SourceExpr {
        return self.builtin(&["types", "aspect"]);
    }

    #[allow(dead_code)]
    pub fn types_bool(&self) -> SourceExpr {
        return self.builtin(&["types", "bool"]);
    }

    #[allow(dead_code)]
    pub fn types_codedyn(&self) -> SourceExpr {
        return self.builtin(&["types", "codedyn"]);
    }

    #[allow(dead_code)]
    pub fn types_constkey(&self) -> SourceExpr {
        return self.builtin(&["types", "constkey"]);
    }

    #[allow(dead_code)]
    pub fn types_cve(&self) -> SourceExpr {
        return self.builtin(&["types", "cve"]);
    }

    #[allow(dead_code)]
    pub fn types_expr(&self) -> SourceExpr {
        return self.builtin(&["types", "expr"]);
    }

    #[allow(dead_code)]
    pub fn types_list(&self) -> SourceExpr {
        return self.builtin(&["types", "list"]);
    }

    #[allow(dead_code)]
    pub fn types_map(&self) -> SourceExpr {
        return self.builtin(&["types", "map"]);
    }

    #[allow(dead_code)]
    pub fn types_null(&self) -> SourceExpr {
        return self.builtin(&["types", "null"]);
    }

    #[allow(dead_code)]
    pub fn types_number(&self) -> SourceExpr {
        return self.builtin(&["types", "number"]);
    }

    #[allow(dead_code)]
    pub fn types_option(&self) -> SourceExpr {
        return self.builtin(&["types", "option"]);
    }

    #[allow(dead_code)]
    pub fn types_string(&self) -> SourceExpr {
        return self.builtin(&["types", "string"]);
    }

    #[allow(dead_code)]
    pub fn types_type(&self) -> SourceExpr {
        return self.builtin(&["types", "type"]);
    }
}

pub(super) fn new_gx(modules: BTreeMap<&str, SourceModule>) -> Arc<GlobalContext> {
    new_gx_with_artifacts(modules).0
}

pub(super) fn new_gx_with_artifacts(
    modules: BTreeMap<&str, SourceModule>,
) -> (Arc<GlobalContext>, Arc<TestArtifacts>) {
    let mut sources = HashMap::new();
    for (i, (name, module)) in modules.into_iter().enumerate() {
        sources.insert(name.to_string(), (SourceId(i), module));
    }
    let artifacts = Arc::new(TestArtifacts::new());
    let gx = GlobalContext::new(Box::new(TestSources { modules: sources }), Box::new(artifacts.clone()));
    (gx, artifacts)
}

pub(super) fn redact_builtins(sources: &[String]) -> Vec<String> {
    let re = regex::Regex::new(r"\bb\d+\b").unwrap();
    sources.iter().map(|s| {
        re
            .replace_all(s, "BUILTIN")
            .into_owned()
            .split('\n')
            .filter(|l| *l != "{/*main*/" && *l != "}/*main*/")
            .collect::<Vec<_>>()
            .join("\n")
    }).collect()
}

pub(super) fn sanitize_builtins(sources: &[String]) -> Vec<String> {
    redact_builtins(sources)
}

pub(super) struct TestArtifacts {
    pub data: Mutex<HashMap<ArtifactIdSource, ArtifactBody>>,
    next_id: Mutex<usize>,
}

impl TestArtifacts {
    fn new() -> Self {
        TestArtifacts {
            next_id: Mutex::new(0),
            data: Mutex::new(HashMap::new()),
        }
    }
}

impl Artifacts for Arc<TestArtifacts> {
    fn get(&self, id: ArtifactIdSource) -> Result<ArtifactBody, String> {
        self.data.lock().unwrap().get(&id).cloned().ok_or_else(|| format!("Artifact not found: {:?}", id))
    }

    fn reserve_id(&self) -> ArtifactIdSource {
        let mut next = self.next_id.lock().unwrap();
        let id = *next;
        *next += 1;
        ArtifactIdSource(CompileId(ImportStateId(0), SourceId(0)), id)
    }

    fn set(&self, id: ArtifactIdSource, data: ArtifactBody) {
        self.data.lock().unwrap().insert(id, data);
    }
}

struct TestSources {
    modules: HashMap<String, (SourceId, SourceModule)>,
}

impl Sources for TestSources {
    fn get(&self, spec: SourceSpecAbsolute) -> Result<(SourceId, SourceModule), String> {
        match spec {
            SourceSpecAbsolute::Virtual(name) => {
                self.modules.get(&name).cloned().ok_or_else(|| format!("Module not found: {}", name))
            },
        }
    }
}
