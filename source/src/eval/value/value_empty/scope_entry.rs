use {
    crate::{
        eval::value::value_empty::r#const::ValueEmptyConst,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            QExprId,
            ScopeEntry,
            Type,
        },
    },
    std::rc::Rc,
};

/// A scope entry for non-JS values. Returns Code_::None and reconstructs the value
/// from its type.
pub(crate) struct ScopeEntryEmpty {
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for ScopeEntryEmpty {
    fn use_(&self, _xx: &crate::langhelp::ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueEmptyConst { type_: self.type_.clone() }),
        }
    }
}
