use {
    crate::langhelp::{
        Code,
        CodeValue,
        Code_,
        QExprId,
        ScopeEntry,
        Type,
        Value,
    },
    std::rc::Rc,
    super::ValueStatic,
};

/// A scope entry for non-JS values. Returns Code_::None and reconstructs the value
/// from its type.
pub(crate) struct ScopeEntryStatic {
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for ScopeEntryStatic {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueStatic { type_: self.type_.clone() }),
        }
    }
}

/// A scope entry that preserves the actual value (for compile-time values without
/// JS code).
pub(crate) struct ScopeEntryValue {
    pub value: Rc<dyn Value>,
}

impl ScopeEntry for ScopeEntryValue {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: self.value.clone(),
        }
    }
}
