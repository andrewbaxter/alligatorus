use crate::langhelp::{
    CodeDyn,
    QExprId,
    ScopeEntryId,
};

pub(crate) const EXPR_PREFIX: &str = "e";
pub(crate) const BINDING_PREFIX: &str = "a";
pub(crate) const TAG_PREFIX: &str = "t";

#[derive(Clone)]
pub(crate) enum JsCodeBlockEntry {
    Bind(JsCodeBind),
    Code(JsCode),
}

#[derive(Clone)]
pub(crate) struct JsCodeBlock {
    pub(crate) entries: Vec<JsCodeBlockEntry>,
}

#[derive(Clone)]
pub(crate) struct JsCodeFieldSet {
    pub(crate) base: Box<JsCode>,
    pub(crate) key: String,
    pub(crate) value: Box<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeBind {
    pub(crate) base: ScopeEntryId,
    pub(crate) value: Box<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeBindingSet {
    pub(crate) base: ScopeEntryId,
    pub(crate) value: Box<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeBindingUse {
    pub(crate) id: ScopeEntryId,
}

#[derive(Clone)]
pub(crate) struct JsCodeFieldUse {
    pub(crate) base: Box<JsCode>,
    pub(crate) field: String,
}

#[derive(Clone)]
pub(crate) struct JsCodeCall {
    pub(crate) func: Box<JsCode>,
    pub(crate) args: Vec<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeNewObject {
    pub(crate) fields: Vec<(String, JsCode)>,
}

#[derive(Clone)]
pub(crate) struct JsCodeNewArray {
    pub(crate) elements: Vec<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeIf {
    pub(crate) conditions: Vec<(JsCode, JsCode)>,
    pub(crate) else_: Option<Box<JsCode>>,
}

#[derive(Clone)]
pub(crate) struct JsCodeOperatorLt {
    pub(crate) base: Box<JsCode>,
    pub(crate) reference: Box<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeOperatorAdd {
    pub(crate) base: Box<JsCode>,
    pub(crate) reference: Box<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeForeach {
    pub(crate) base: Box<JsCode>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) body: Box<JsCode>,
}

#[derive(Clone)]
pub(crate) enum JsCodeType {
    Literal(serde_json::Value),
    NewObject(JsCodeNewObject),
    NewArray(JsCodeNewArray),
    Block(JsCodeBlock),
    BindingUse(JsCodeBindingUse),
    BindingSet(JsCodeBindingSet),
    FieldUse(JsCodeFieldUse),
    FieldSet(JsCodeFieldSet),
    Call(JsCodeCall),
    If(JsCodeIf),
    OperatorLt(JsCodeOperatorLt),
    OperatorAdd(JsCodeOperatorAdd),
    Foreach(JsCodeForeach),
    Match(JsCodeMatch),
}

#[derive(Clone)]
pub(crate) struct JsCodeMatchBranch {
    pub(crate) tag: String,
    pub(crate) body: JsCode,
}

#[derive(Clone)]
pub(crate) struct JsCodeMatch {
    pub(crate) base: Box<JsCode>,
    pub(crate) element_key: ScopeEntryId,
    pub(crate) cases: Vec<JsCodeMatchBranch>,
    pub(crate) fallback: Option<Box<JsCode>>,
}

#[derive(Clone)]
pub(crate) struct JsCode {
    pub(crate) loc: QExprId,
    pub(crate) type_: JsCodeType,
}

impl CodeDyn for JsCode {
    fn loc(&self) -> QExprId {
        return self.loc;
    }
}
