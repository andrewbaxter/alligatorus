use {
    crate::{
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeType,
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
            Value,
        },
    },
    std::rc::Rc,
    super::mortar_js_value::{
        MortarJsValue,
        MortarJsValueDataScope,
        MortarJsValueSource,
    },
};

pub(crate) trait MortarScopeEntry {
    fn use_(&self, value: MortarJsValue) -> EvalRes;
}

/// Default MortarScopeEntry for JS-bound values. Generates BindingUse code from
/// the MortarJsValue's Binding source.
pub(crate) struct MortarScopeEntryDirect {
    pub type_: Rc<dyn Type>,
}

impl MortarScopeEntry for MortarScopeEntryDirect {
    fn use_(&self, value: MortarJsValue) -> EvalRes {
        let (loc, key) = match &value.source {
            MortarJsValueSource::Binding(b) => (b.loc, b.key),
            _ => unreachable!("MortarScopeEntryDirect::use_ called with non-binding source"),
        };
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: key }),
                })),
            },
            value: Rc::new(MortarJsValue {
                source: value.source,
                type_: self.type_.clone(),
            }),
        })
    }
}

/// MortarScopeEntry for compile-time values without JS representation.
pub(crate) struct MortarScopeEntryValue {
    pub loc: QExprId,
    pub value: Rc<dyn Value>,
}

impl MortarScopeEntry for MortarScopeEntryValue {
    fn use_(&self, _value: MortarJsValue) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: Code {
                loc: self.loc,
                code: Code_::None,
            },
            value: self.value.clone(),
        })
    }
}

/// Adapter: wraps a MortarScopeEntry trait object to implement ScopeEntry.
pub(crate) struct MortarScopeEntryAsScope {
    pub js_key: Option<ScopeEntryId>,
    pub type_: Rc<dyn Type>,
    pub inner: Box<dyn MortarScopeEntry>,
}

impl ScopeEntry for MortarScopeEntryAsScope {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        let source = match self.js_key {
            Some(key) => MortarJsValueSource::Binding(MortarJsValueDataScope {
                loc,
                key,
            }),
            None => MortarJsValueSource::Expr,
        };
        let value = MortarJsValue {
            source,
            type_: self.type_.clone(),
        };
        self.inner.use_(value).v.expect("MortarScopeEntry::use_ should not fail")
    }
}

/// Direct scope entry for mortar bindings (used by match, foreach, builtins).
#[derive(Clone)]
pub struct MortarScopeEntryBinding {
    pub js_key: ScopeEntryId,
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for MortarScopeEntryBinding {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        return CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(JsCode {
                    loc: loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: self.js_key }),
                })),
            },
            value: Rc::new(MortarJsValue {
                source: MortarJsValueSource::Binding(MortarJsValueDataScope {
                    loc: loc,
                    key: self.js_key,
                }),
                type_: self.type_.clone(),
            }),
        };
    }
}
