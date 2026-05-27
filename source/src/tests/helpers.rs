use {
    crate::syntax::{
        ExprId,
        SerialExpr,
        SerialExpr_,
        SerialExprBind,
        SerialExprBindingUse,
        SerialExprBlock,
        SerialExprCall,
        SerialExprField,
        SerialExprForeach,
        SerialExprIf,
        SerialExprIfConditionBranch,
        SerialExprLower,
        SerialExprMatch,
        SerialExprMatchTagBranch,
        SerialExprOperatorAdd,
        SerialExprOperatorLt,
        SerialExprSet,
        SerialExprStage,
        SerialLiteral,
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

    pub fn null(&self) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Literal(SerialLiteral::Null),
        }
    }

    pub fn num(&self, v: f64) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Literal(SerialLiteral::Number(v.try_into().unwrap())),
        }
    }

    pub fn str(&self, s: &str) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Literal(SerialLiteral::String(s.to_string())),
        }
    }

    pub fn tuple(&self, values: Vec<SerialExpr>) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Tuple(values),
        }
    }

    pub fn foreach(&self, base: SerialExpr, element_key: SerialExpr, body: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Foreach(SerialExprForeach {
                base: Box::new(base),
                element_key: Box::new(element_key),
                body: Box::new(body),
            }),
        }
    }

    pub fn match_(
        &self,
        base: SerialExpr,
        element_key: SerialExpr,
        cases: Vec<(SerialExpr, SerialExpr)>,
        fallback: Option<SerialExpr>,
    ) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Match(SerialExprMatch {
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

    pub fn scope_use(&self, key: &str) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::ScopeUse(SerialExprBindingUse { key: Box::new(SerialExpr {
                id: self.id(),
                r#type: SerialExpr_::Literal(SerialLiteral::String(key.to_string())),
            }) }),
        };
    }

    pub fn field(&self, base: SerialExpr, key: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Field(SerialExprField {
                base: Box::new(base),
                key: Box::new(key),
            }),
        };
    }

    pub fn call(&self, func: SerialExpr, args: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Call(SerialExprCall {
                func: Box::new(func),
                arg: Box::new(args),
            }),
        };
    }

    pub fn block(&self, exprs: Vec<SerialExpr>) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Block(SerialExprBlock { exprs: exprs }),
        };
    }

    pub fn bind(&self, key: &str, expr: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Bind(SerialExprBind {
                base: key.to_string(),
                value: Box::new(expr),
            }),
        };
    }

    pub fn if_(&self, cases: Vec<(SerialExpr, SerialExpr)>, else_: Option<SerialExpr>) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::If(SerialExprIf {
                conditions: cases.into_iter().map(|x| SerialExprIfConditionBranch {
                    condition: x.0,
                    body: x.1,
                }).collect(),
                fallback: else_.map(Box::new),
            }),
        };
    }

    pub fn operator_lt(&self, base: SerialExpr, reference: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::OperatorLt(SerialExprOperatorLt {
                base: Box::new(base),
                reference: Box::new(reference),
            }),
        };
    }

    pub fn operator_add(&self, base: SerialExpr, reference: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::OperatorAdd(SerialExprOperatorAdd {
                base: Box::new(base),
                reference: Box::new(reference),
            }),
        };
    }

    pub fn set(&self, base: SerialExpr, expr: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Set(SerialExprSet {
                base: Box::new(base),
                value: Box::new(expr),
            }),
        };
    }

    pub fn stage(&self, expr: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Stage(SerialExprStage { expr: Box::new(expr) }),
        };
    }

    pub fn lower(&self, expr: SerialExpr) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Lower(SerialExprLower { expr: Box::new(expr) }),
        };
    }

    pub fn record(&self, entries: Vec<(&str, SerialExpr)>) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Record(entries.into_iter().map(|(k, v)| (self.str(k), v)).collect()),
        };
    }
}
