use {
    crate::langhelp::{
        Code,
        CodeValue,
        QExprId,
        ScopeEntry,
    },
    std::rc::Rc,
    super::PendingImportState,
};

pub(crate) struct ScopeEntryPendingImport {
    pub state: Rc<PendingImportState>,
}

impl ScopeEntry for ScopeEntryPendingImport {
    fn use_(&self, _loc: QExprId, _code: Code) -> CodeValue {
        self.state.resolve_standalone()
    }
}
