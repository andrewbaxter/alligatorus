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

    pub fn call(&self, func: SourceExpr, args: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Call(SourceExprCall {
                func: Box::new(func),
                arg: Box::new(args),
            }),
        };
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

    pub fn tuple(&self, values: Vec<SourceExpr>) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Tuple(values),
        }
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
    sources.iter().map(|s| strip_main_body_wrap(re.replace_all(s, "BUILTIN").into_owned())).collect()
}

pub(super) fn sanitize_builtins(sources: &[String]) -> Vec<String> {
    redact_builtins(sources)
}

fn strip_main_body_wrap(s: String) -> String {
    let lines: Vec<&str> = s.split('\n').filter(|l| *l != "{/*main*/" && *l != "}/*main*/").collect();
    lines.join("\n")
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
