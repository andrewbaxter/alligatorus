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
            QExprId,
            ScopeEntry,
            ScopeEntryId,
            Type,
        },
    },
    std::rc::Rc,
    super::mortar_js_value::{
        MortarJsValue,
        MortarJsValueDataScope,
        MortarJsValueSource,
    },
};

pub struct MortarScopeEntry {
    pub js_key: ScopeEntryId,
    pub type_: Rc<dyn Type>,
}

impl ScopeEntry for MortarScopeEntry {
    fn use_(&self, loc: QExprId, _code: Code) -> CodeValue {
        return CodeValue {
            code: Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(JsCode {
                    loc: loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: self.js_key }),
                })),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Binding(MortarJsValueDataScope {
                    loc: loc,
                    key: self.js_key,
                }),
                type_: self.type_.clone(),
            }),
        };
    }
}
