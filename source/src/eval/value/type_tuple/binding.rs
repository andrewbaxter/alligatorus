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
        r#gen::Binding,
    },
    std::rc::Rc,
};

pub(crate) struct BindingValue {
    pub value: Rc<dyn Value>,
}

impl Binding for BindingValue {
    fn use_(&self, _xx: &ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Some(self.value.clone()),
        }
    }
}
