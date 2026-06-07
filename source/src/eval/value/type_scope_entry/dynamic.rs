use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::value::type_dynamic::{
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
