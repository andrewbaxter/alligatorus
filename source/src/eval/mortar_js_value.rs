use {
    crate::langhelp::{
        ExprContext,
        QExprId,
        ScopeEntryId,
        Type,
        Value,
    },
    dumpster::unsync::Gc,
    std::rc::Rc,
};

pub struct MortarJsValueDataScope {
    pub loc: QExprId,
    pub key: ScopeEntryId,
}

pub struct MortarJsValueDataAccess {
    /// Location of where access occurred
    pub loc: QExprId,
    pub base: Box<MortarJsValue>,
    pub key: String,
}

pub struct MortarJsValueDataDefinitionSet {
    pub set: Gc<super::definition_set::DefinitionSet>,
    pub func_index: usize,
}

pub enum MortarJsValueSource {
    Binding(MortarJsValueDataScope),
    DefinitionSet(MortarJsValueDataDefinitionSet),
    // Result of effect
    Expr,
    Field(MortarJsValueDataAccess),
}

pub struct MortarJsValue {
    pub source: MortarJsValueSource,
    pub type_: Rc<dyn Type>,
}

impl Value for MortarJsValue {
    fn type_(&self, _ex: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        return self.type_.clone();
    }
}
