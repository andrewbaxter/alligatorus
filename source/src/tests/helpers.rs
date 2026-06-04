pub(super) fn sanitize_builtins(sources: &[String]) -> Vec<String> {
    let re = regex::Regex::new(r"\bb\d+\b").unwrap();
    sources.iter().map(|s| re.replace_all(s, "BUILTIN").into_owned()).collect()
}

use {
    crate::syntax::{
        ExprId,
        SerialExprIfConditionBranch,
        SerialExprMatchTagBranch,
        SourceExpr,
        SourceExprBind,
        SourceExprBindingUse,
        SourceExprBlock,
        SourceExprCall,
        SourceExprField,
        SourceExprForeach,
        SourceExprIf,
        SourceExprLower,
        SourceExprMatch,
        SourceExprOperatorAdd,
        SourceExprOperatorLt,
        SourceExprSet,
        SourceExprStage,
        SourceExpr_,
        SourceLiteral,
    },
    std::cell::Cell,
};

#[derive(Default)]
pub(super) struct ModuleBuilder {
    next_phrase_id: Cell<usize>,
}

impl ModuleBuilder {
    pub fn id(&self) -> ExprId {
        let id = self.next_phrase_id.get();
        self.next_phrase_id.set(id + 1);
        return ExprId(id);
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

    pub fn scope_use(&self, key: &str) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::ScopeUse(SourceExprBindingUse { key: Box::new(SourceExpr {
                id: self.id(),
                r#type: SourceExpr_::Literal(SourceLiteral::String(key.to_string())),
            }) }),
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

    pub fn call(&self, func: SourceExpr, args: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Call(SourceExprCall {
                func: Box::new(func),
                arg: Box::new(args),
            }),
        };
    }

    pub fn block(&self, exprs: Vec<SourceExpr>) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Block(SourceExprBlock { exprs: exprs }),
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

    pub fn operator_lt(&self, base: SourceExpr, reference: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::OperatorLt(SourceExprOperatorLt {
                base: Box::new(base),
                reference: Box::new(reference),
            }),
        };
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

    pub fn lower(&self, expr: SourceExpr) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Lower(SourceExprLower { expr: Box::new(expr) }),
        };
    }

    pub fn record(&self, entries: Vec<(&str, SourceExpr)>) -> SourceExpr {
        return SourceExpr {
            id: self.id(),
            r#type: SourceExpr_::Record(entries.into_iter().map(|(k, v)| (self.str(k), v)).collect()),
        };
    }
}
