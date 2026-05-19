use {
    crate::syntax::{
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

    pub fn seq(&self, exprs: Vec<SerialExpr>) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Sequence(SerialExprSequence { exprs: exprs }),
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

    pub fn record(&self, entries: Vec<(String, SerialExpr)>) -> SerialExpr {
        return SerialExpr {
            id: self.id(),
            r#type: SerialExpr_::Record(entries),
        };
    }
}
