use {
    crate::{
        builtin::aspect_consume_js_get,
        dynamic_code::{
            DynamicCode,
            DynamicCodeBindingSet,
            DynamicCodeBlock,
            DynamicCodeBlockEntry,
            DynamicCodeFieldSet,
            DynamicCodeType,
        },
        eval::Erb,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
            ScopeEntryId,
            Type,
            Value,
        },
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

pub(crate) struct ValueDynamicDataScope {
    pub loc: QExprId,
    pub key: ScopeEntryId,
}

pub(crate) struct ValueDynamicDataAccess {
    pub key: String,
}

pub(crate) enum ValueDynamicSource {
    Binding(ValueDynamicDataScope),
    DefinitionSet,
    // Result of effect
    Expr,
    Field(ValueDynamicDataAccess),
}

pub(crate) struct ValueDynamic {
    pub source: ValueDynamicSource,
    pub type_: Rc<dyn Type>,
}

impl Value for ValueDynamic {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return self.type_.clone();
    }
}

pub(crate) fn mortar_consume_js_to_js(_xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<DynamicCode> {
    let r = cv.code.cast::<DynamicCode>();
    if let Some(c) = r.v {
        if let Some(c) = c {
            return EResult {
                v: Some(c.clone()),
                errors: r.errors,
            };
        } else {
            let mut errors = r.errors.unwrap_or_default();
            errors.push(ExprErr {
                loc: loc,
                errors: vec![ExprSubErr::Opaque(format!("Expression resulted in empty code"))],
            });
            return EResult {
                v: None,
                errors: Some(errors),
            }
        }
    } else {
        return EResult {
            v: None,
            errors: r.errors,
        };
    }
}

pub(crate) fn mortar_bind_js(
    xx: &ExprContext,
    loc: QExprId,
    cv: CodeValue,
) -> EResult<(Option<DynamicCode>, Box<dyn crate::eval::value::value_scope_entry::dynamic::DynamicScopeEntry>)> {
    let vtype = cv.value.type_(xx, loc);
    mortar_consume_js_to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
}

/// Helper to construct the return value for bind_js when JS code is produced.
pub(crate) fn bind_js_some(
    _loc: QExprId,
    js: DynamicCode,
    bound_type: Rc<dyn Type>,
) -> (Option<DynamicCode>, Box<dyn crate::eval::value::value_scope_entry::dynamic::DynamicScopeEntry>) {
    (
        Some(js),
        Box::new(crate::eval::value::value_scope_entry::dynamic::DynamicScopeEntryDirect { type_: bound_type }),
    )
}

pub(crate) fn mortar_js_stack_to_value(loc: QExprId, value: DynamicCode, type_: Rc<dyn Type>) -> CodeValue {
    CodeValue {
        code: Code {
            loc,
            code: Code_::Dyn(Rc::new(value)),
        },
        value: Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_,
        }),
    }
}

pub(crate) fn mortar_set(xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let target_type = {
            let v = (self_.value.as_ref() as &dyn Any).downcast_ref::<ValueDynamic>().unwrap();
            v.type_.clone()
        };
        let value = erb.must_er(crate::eval::typecheck_convert(xx, loc, target_type.as_ref(), value))?;
        let value_type = value.value.type_(xx, loc);
        er_all!(erb, {
            value_to_js = aspect_consume_js_get(xx, loc, value_type.as_ref()),
            self_code = self_.code.cast::<DynamicCode>(),
        });
        let value_code = erb.must_er(value_to_js.to_js(xx, loc, value))?;
        let v = (self_.value.as_ref() as &dyn Any).downcast_ref::<ValueDynamic>().unwrap();
        match &v.source {
            ValueDynamicSource::Field(b) => {
                let Some(self_code) = self_code else {
                    return erb.err_new(format!("TODO assertion set left hand doesn't evaluate to js code (empty)"));
                };

                // Extract the base from FieldUse so FieldSet targets the parent object
                let base_code = match &self_code.type_ {
                    DynamicCodeType::FieldUse(fu) => *fu.base.clone(),
                    _ => self_code.clone(),
                };
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(DynamicCode {
                            loc: loc,
                            type_: DynamicCodeType::FieldSet(DynamicCodeFieldSet {
                                base: Box::new(base_code),
                                key: b.key.clone(),
                                value: Box::new(value_code)
                            })
                        }))
                    },
                    value: Rc::new(crate::eval::value::value_null::r#const::ValueConstNull),
                })
            },
            ValueDynamicSource::Binding(b) => {
                let mut seq = vec![];
                if let Some(self_code) = self_code {
                    seq.push(self_code.clone());
                }
                seq.push(DynamicCode {
                    loc: loc,
                    type_: DynamicCodeType::BindingSet(DynamicCodeBindingSet {
                        base: b.key,
                        value: Box::new(value_code),
                    })
                });
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(DynamicCode {
                            loc: loc,
                            type_: DynamicCodeType::Block(
                                DynamicCodeBlock { entries: seq.into_iter().map(DynamicCodeBlockEntry::Code).collect() }
                            )
                        }))
                    },
                    value: Rc::new(crate::eval::value::value_null::r#const::ValueConstNull),
                })
            },
            ValueDynamicSource::Expr => {
                return erb.err_new(format!("Cannot modify a temporary value"));
            },
            ValueDynamicSource::DefinitionSet => {
                return erb.err_new(format!("Cannot modify a definition set function"));
            },
        }
    });
}
