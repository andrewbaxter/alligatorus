use crate::langhelp::{
    CodeDyn,
    QExprId,
    ScopeEntryId,
};

pub(crate) const EXPR_PREFIX: &str = "e";
pub(crate) const BINDING_PREFIX: &str = "a";
pub(crate) const BUILTIN_PREFIX: &str = "b";
pub(crate) const DEF_PREFIX: &str = "d";
pub(crate) const TAG_PREFIX: &str = "t";
pub(crate) const TAG_VAR: &str = "__tag";

#[derive(Clone)]
pub(crate) enum DynamicCodeBlockEntry {
    Bind(DynamicCodeBind),
    Code(DynamicCode),
}

#[derive(Clone)]
pub(crate) struct DynamicCodeBlock {
    pub(crate) entries: Vec<DynamicCodeBlockEntry>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeFieldSet {
    pub(crate) base: Box<DynamicCode>,
    pub(crate) key: String,
    pub(crate) value: Box<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeBind {
    pub(crate) base: ScopeEntryId,
    pub(crate) value: Box<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeBindingSet {
    pub(crate) base: ScopeEntryId,
    pub(crate) value: Box<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeBindingUse {
    pub(crate) id: ScopeEntryId,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeFieldUse {
    pub(crate) base: Box<DynamicCode>,
    pub(crate) field: String,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeCall {
    pub(crate) func: Box<DynamicCode>,
    pub(crate) args: Vec<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeNewObject {
    pub(crate) fields: Vec<(String, DynamicCode)>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeNewMap {
    pub(crate) fields: Vec<(DynamicCode, DynamicCode)>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeNewArray {
    pub(crate) elements: Vec<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeIf {
    pub(crate) conditions: Vec<(DynamicCode, DynamicCode)>,
    pub(crate) else_: Option<Box<DynamicCode>>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeOperatorLt {
    pub(crate) base: Box<DynamicCode>,
    pub(crate) reference: Box<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeOperatorAdd {
    pub(crate) base: Box<DynamicCode>,
    pub(crate) reference: Box<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeForeach {
    pub(crate) base: Box<DynamicCode>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) body: Box<DynamicCode>,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeForeachMap {
    pub(crate) base: Box<DynamicCode>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) body: Box<DynamicCode>,
}

#[derive(Clone)]
pub(crate) enum DynamicCodeType {
    Literal(serde_json::Value),
    NewObject(DynamicCodeNewObject),
    NewMap(DynamicCodeNewMap),
    NewArray(DynamicCodeNewArray),
    Block(DynamicCodeBlock),
    BindingUse(DynamicCodeBindingUse),
    BindingSet(DynamicCodeBindingSet),
    FieldUse(DynamicCodeFieldUse),
    FieldSet(DynamicCodeFieldSet),
    Call(DynamicCodeCall),
    If(DynamicCodeIf),
    OperatorLt(DynamicCodeOperatorLt),
    OperatorAdd(DynamicCodeOperatorAdd),
    Foreach(DynamicCodeForeach),
    ForeachMap(DynamicCodeForeachMap),
    Match(DynamicCodeMatch),
}

#[derive(Clone)]
pub(crate) struct DynamicCodeMatchBranch {
    pub(crate) js_tag: String,
    pub(crate) body: DynamicCode,
}

#[derive(Clone)]
pub(crate) struct DynamicCodeMatch {
    pub(crate) base: Box<DynamicCode>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) cases: Vec<DynamicCodeMatchBranch>,
    pub(crate) fallback: Option<Box<DynamicCode>>,
}

#[derive(Clone)]
pub(crate) struct DynamicCode {
    pub(crate) loc: QExprId,
    pub(crate) type_: DynamicCodeType,
}

impl CodeDyn for DynamicCode { }
