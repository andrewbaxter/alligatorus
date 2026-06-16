use {
    crate::{
        langhelp::{
            Code,
            CodeValue,
            Code_,
            ExprContext,
            QExprId,
            Value,
        },
        r#gen::ScopeEntry,
    },
    std::rc::Rc,
};

pub(crate) struct ScopeEntryValue {
    pub value: Rc<dyn Value>,
}

impl ScopeEntry for ScopeEntryValue {
    fn use_(&self, _xx: &ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: self.value.clone(),
        }
    }
}
