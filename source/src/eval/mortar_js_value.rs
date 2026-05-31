use {
    crate::{
        builtin::{
            AspectConsumeJs,
            aspect_consume_js_get,
        },
        jscode::{
            JsCode,
            JsCodeBindingSet,
            JsCodeBlock,
            JsCodeBlockEntry,
            JsCodeFieldSet,
            JsCodeType,
        },
        langhelp::{
            Code,
            Code_,
            CodeValue,
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
    dumpster::unsync::Gc,
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::Erb,
};

pub struct MortarJsValueDataScope {
    pub loc: QExprId,
    pub key: ScopeEntryId,
}

pub struct MortarJsValueDataAccess {
    /// Location of where access occurred
    pub loc: QExprId,
    pub base: Box<MortarJsValue>,
    pub key: String,
}

pub struct MortarJsValueDataDefinitionSet {
    pub set: Gc<super::definition_set::DefinitionSet>,
    pub func_index: usize,
}

pub enum MortarJsValueSource {
    Binding(MortarJsValueDataScope),
    DefinitionSet(MortarJsValueDataDefinitionSet),
    // Result of effect
    Expr,
    Field(MortarJsValueDataAccess),
}

pub struct MortarJsValue {
    pub source: MortarJsValueSource,
    pub type_: Rc<dyn Type>,
}

impl Value for MortarJsValue {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return self.type_.clone();
    }
}

pub(crate) fn mortar_consume_js_to_js(_xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
    let r = cv.code.cast::<JsCode>();
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
) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
    let vtype = cv.value.type_(xx, loc);
    mortar_consume_js_to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
}

/// Helper to construct the return value for bind_js when JS code is produced.
pub(crate) fn bind_js_some(
    _loc: QExprId,
    js: JsCode,
    bound_type: Rc<dyn Type>,
) -> (Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>) {
    (Some(js), Box::new(super::mortar_scope_entry::MortarScopeEntryDirect { type_: bound_type }))
}

pub(crate) fn mortar_js_stack_to_value(loc: QExprId, value: JsCode, type_: Rc<dyn Type>) -> CodeValue {
    CodeValue {
        code: Code {
            loc,
            code: Code_::Dyn(Rc::new(value)),
        },
        value: Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_,
        }),
    }
}

pub(crate) fn mortar_set(xx: &ExprContext, loc: QExprId, self_: CodeValue, value: CodeValue) -> EvalRes {
    return er_seq_cv!(loc, erb, {
        let value_type = value.value.type_(xx, loc);
        er_all!(erb, {
            value_to_js = aspect_consume_js_get(xx, loc, value_type.as_ref()),
            self_code = self_.code.cast::<JsCode>(),
        });
        let value_code = erb.must_er(value_to_js.to_js(xx, loc, value))?;
        let v = (self_.value.as_ref() as &dyn Any).downcast_ref::<MortarJsValue>().unwrap();
        match &v.source {
            MortarJsValueSource::Field(b) => {
                let Some(self_code) = self_code else {
                    return erb.err_new(format!("TODO assertion set left hand doesn't evaluate to js code (empty)"));
                };

                // Extract the base from FieldUse so FieldSet targets the parent object
                let base_code = match &self_code.type_ {
                    JsCodeType::FieldUse(fu) => *fu.base.clone(),
                    _ => self_code.clone(),
                };
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(JsCode {
                            loc: loc,
                            type_: JsCodeType::FieldSet(JsCodeFieldSet {
                                base: Box::new(base_code),
                                key: b.key.clone(),
                                value: Box::new(value_code)
                            })
                        }))
                    },
                    value: Box::new(super::null::ValueNull),
                })
            },
            MortarJsValueSource::Binding(b) => {
                let mut seq = vec![];
                if let Some(self_code) = self_code {
                    seq.push(self_code.clone());
                }
                seq.push(JsCode {
                    loc: loc,
                    type_: JsCodeType::BindingSet(JsCodeBindingSet {
                        base: b.key,
                        value: Box::new(value_code),
                    })
                });
                return Ok(CodeValue {
                    code: Code {
                        loc: loc,
                        code: Code_::Dyn(Rc::new(JsCode {
                            loc: loc,
                            type_: JsCodeType::Block(
                                JsCodeBlock { entries: seq.into_iter().map(JsCodeBlockEntry::Code).collect() }
                            )
                        }))
                    },
                    value: Box::new(super::null::ValueNull),
                })
            },
            MortarJsValueSource::Expr => {
                return erb.err_new(format!("Cannot modify a temporary value"));
            },
            MortarJsValueSource::DefinitionSet(_) => {
                return erb.err_new(format!("Cannot modify a definition set function"));
            },
        }
    });
}
