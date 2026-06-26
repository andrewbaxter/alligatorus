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
            ExprContext,
            QExprId,
            BindingId,
            Type,
        },
        r#gen::Binding,
    },
    std::rc::Rc,
};

#[derive(Clone)]
pub(crate) struct BindingDynamicBinding {
    pub dynamic_key: BindingId,
    pub type_: Rc<dyn Type>,
}

impl Binding for BindingDynamicBinding {
    fn use_(&self, _xx: &ExprContext, loc: QExprId, _code: Code) -> CodeValue {
        return CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: self.dynamic_key }),
                })),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Binding(ValueDynamicDataScope { key: self.dynamic_key }),
                type_: self.type_.clone(),
            }),
        };
    }
}
