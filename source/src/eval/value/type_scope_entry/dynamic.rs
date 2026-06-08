use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::value::dynamic_util::{
            ValueDynamic,
            ValueDynamicDataScope,
            ValueDynamicSource,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            QExprId,
            ScopeEntry,
            ScopeEntryId,
            Type,
        },
    },
    std::rc::Rc,
};

#[derive(Clone)]
pub(crate) struct ScopeEntryDynamicBinding {
    pub dynamic_key: ScopeEntryId,
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for ScopeEntryDynamicBinding {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
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
