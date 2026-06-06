use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::value::value_dynamic::{
            ValueDynamic,
            ValueDynamicDataScope,
            ValueDynamicSource,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            EvalRes,
            QExprId,
            ScopeEntry,
            ScopeEntryId,
            Type,
        },
    },
    std::rc::Rc,
};

pub(crate) trait DynamicScopeEntry {
    fn use_(&self, value: ValueDynamic) -> EvalRes;
}

/// Default DynamicScopeEntry for JS-bound values. Generates BindingUse code from
/// the ValueDynamic's Binding source.
pub(crate) struct ScopeEntryDynamicDirect {
    pub type_: Rc<dyn Type>,
}

impl DynamicScopeEntry for ScopeEntryDynamicDirect {
    fn use_(&self, value: ValueDynamic) -> EvalRes {
        let (loc, key) = match &value.source {
            ValueDynamicSource::Binding(b) => (b.loc, b.key),
            _ => unreachable!("ScopeEntryDynamicDirect::use_ called with non-binding source"),
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: key }),
                })),
            },
            value: Rc::new(ValueDynamic {
                source: value.source,
                type_: self.type_.clone(),
            }),
        })
    }
}

/// Adapter: wraps a DynamicScopeEntry trait object to implement ScopeEntry.
pub(crate) struct ScopeEntryDynamicAsScope {
    pub js_key: Option<ScopeEntryId>,
    pub type_: Rc<dyn Type>,
    pub inner: Box<dyn DynamicScopeEntry>,
}

impl ScopeEntry for ScopeEntryDynamicAsScope {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        let source = match self.js_key {
            Some(key) => ValueDynamicSource::Binding(ValueDynamicDataScope {
                loc,
                key,
            }),
            None => ValueDynamicSource::Expr,
        };
        let value = ValueDynamic {
            source,
            type_: self.type_.clone(),
        };
        self.inner.use_(value).v.expect("DynamicScopeEntry::use_ should not fail")
    }
}

/// Direct scope entry for mortar bindings (used by match, foreach, builtins).
#[derive(Clone)]
pub(crate) struct ScopeEntryDynamicBinding {
    pub js_key: ScopeEntryId,
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for ScopeEntryDynamicBinding {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        return CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: self.js_key }),
                })),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Binding(ValueDynamicDataScope {
                    loc: loc,
                    key: self.js_key,
                }),
                type_: self.type_.clone(),
            }),
        };
    }
}
