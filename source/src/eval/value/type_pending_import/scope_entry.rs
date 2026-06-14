use crate::r#gen::ScopeEntry;
use {
    crate::langhelp::{
        Code,
        CodeValue,
        QExprId,
    },
    std::rc::Rc,
    super::PendingImportState,
};

pub(crate) struct ScopeEntryPendingImport {
    pub state: Rc<PendingImportState>,
}

impl ScopeEntry for ScopeEntryPendingImport {
    fn use_(&self, _xx: &crate::langhelp::ExprContext, _loc: QExprId, _code: Code) -> CodeValue {
        self.state.resolve_standalone()
    }
}
