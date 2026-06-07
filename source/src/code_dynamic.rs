use crate::langhelp::{
    CodeDyn,
    QExprId,
    ScopeEntryId,
};

pub(crate) const BINDING_PREFIX: &str = "a";
pub(crate) const BUILTIN_PREFIX: &str = "b";
pub(crate) const DEF_PREFIX: &str = "d";
pub(crate) const EXPR_PREFIX: &str = "e";
pub(crate) const TAG_PREFIX: &str = "t";
pub(crate) const TAG_VAR: &str = "__tag";

#[derive(Clone)]
pub(crate) struct CodeDynamic {
    pub(crate) loc: QExprId,
    pub(crate) type_: CodeDynamicType,
}

impl CodeDyn for CodeDynamic { }

#[derive(Clone)]
pub(crate) struct CodeDynamicBind {
    pub(crate) base: ScopeEntryId,
    pub(crate) value: Box<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicBindingSet {
    pub(crate) base: ScopeEntryId,
    pub(crate) value: Box<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicBindingUse {
    pub(crate) id: ScopeEntryId,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicBlock {
    pub(crate) entries: Vec<CodeDynamicBlockEntry>,
}

#[derive(Clone)]
pub(crate) enum CodeDynamicBlockEntry {
    Bind(CodeDynamicBind),
    Code(CodeDynamic),
}

#[derive(Clone)]
pub(crate) struct CodeDynamicCall {
    pub(crate) func: Box<CodeDynamic>,
    pub(crate) args: Vec<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicFieldSet {
    pub(crate) base: Box<CodeDynamic>,
    pub(crate) key: String,
    pub(crate) value: Box<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicFieldUse {
    pub(crate) base: Box<CodeDynamic>,
    pub(crate) field: String,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicForeach {
    pub(crate) base: Box<CodeDynamic>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) body: Box<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicForeachMap {
    pub(crate) base: Box<CodeDynamic>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) body: Box<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicIf {
    pub(crate) conditions: Vec<(CodeDynamic, CodeDynamic)>,
    pub(crate) else_: Option<Box<CodeDynamic>>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicMatch {
    pub(crate) base: Box<CodeDynamic>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) cases: Vec<CodeDynamicMatchBranch>,
    pub(crate) fallback: Option<Box<CodeDynamic>>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicMatchBranch {
    pub(crate) dynamic_tag: String,
    pub(crate) body: CodeDynamic,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicNewArray {
    pub(crate) elements: Vec<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicNewMap {
    pub(crate) fields: Vec<(CodeDynamic, CodeDynamic)>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicNewObject {
    pub(crate) fields: Vec<(String, CodeDynamic)>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicOperatorAdd {
    pub(crate) base: Box<CodeDynamic>,
    pub(crate) reference: Box<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) struct CodeDynamicOperatorLt {
    pub(crate) base: Box<CodeDynamic>,
    pub(crate) reference: Box<CodeDynamic>,
}

#[derive(Clone)]
pub(crate) enum CodeDynamicType {
    Literal(serde_json::Value),
    NewObject(CodeDynamicNewObject),
    NewMap(CodeDynamicNewMap),
    NewArray(CodeDynamicNewArray),
    Block(CodeDynamicBlock),
    BindingUse(CodeDynamicBindingUse),
    BindingSet(CodeDynamicBindingSet),
    FieldUse(CodeDynamicFieldUse),
    FieldSet(CodeDynamicFieldSet),
    Call(CodeDynamicCall),
    If(CodeDynamicIf),
    OperatorLt(CodeDynamicOperatorLt),
    OperatorAdd(CodeDynamicOperatorAdd),
    Foreach(CodeDynamicForeach),
    ForeachMap(CodeDynamicForeachMap),
    Match(CodeDynamicMatch),
}
