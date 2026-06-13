use {
    crate::langhelp::{
        Code,
        CodeValue,
        Code_,
        QExprId,
        ScopeEntry,
        Value,
    },
    std::rc::Rc,
};

/// A scope entry that preserves the actual value (for compile-time values without
/// JS code).
pub(crate) struct ScopeEntryValue {
    pub value: Rc<dyn Value>,
}

impl ScopeEntry for ScopeEntryValue {
    fn use_(&self, _xx: &crate::langhelp::ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: self.value.clone(),
        }
    }
}
