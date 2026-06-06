use {
    crate::langhelp::{
        Code,
        CodeValue,
        QExprId,
        ScopeEntry,
        Value,
    },
    std::rc::Rc,
    super::PendingImportState,
};

pub(crate) struct ScopeEntryPendingImport {
    pub state: Rc<PendingImportState>,
    pub code: Code,
}

impl ScopeEntry for ScopeEntryPendingImport {
    fn use_(&self, _loc: QExprId, _code: Code) -> CodeValue {
        let value = self.state.resolve_standalone();
        CodeValue {
            code: self.code.clone(),
            value,
        }
    }
}
