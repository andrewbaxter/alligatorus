use {
    crate::{
        eval::value::value_empty::r#const::ValueEmptyConst,
        langhelp::{
            Code,
            CodeValue,
            Code_,
            ExprContext,
            QExprId,
            Type,
        },
        r#gen::ScopeEntry,
    },
    std::rc::Rc,
};

pub(crate) struct ScopeEntryEmpty {
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for ScopeEntryEmpty {
    fn use_(&self, _xx: &ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(ValueEmptyConst { type_: self.type_.clone() }),
        }
    }
}
