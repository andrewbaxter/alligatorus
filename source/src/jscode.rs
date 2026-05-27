use crate::langhelp::{
    CodeDyn,
    QExprId,
    ScopeEntryId,
};

#[derive(Clone)]
pub(crate) struct JsCodeSeq {
    pub(crate) sub: Vec<JsCode>,
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
pub(crate) enum JsCodeType {
    Literal(serde_json::Value),
    NewObject(JsCodeNewObject),
    NewArray(JsCodeNewArray),
    Seq(JsCodeSeq),
    Bind(JsCodeBind),
    BindingUse(JsCodeBindingUse),
    BindingSet(JsCodeBindingSet),
    FieldUse(JsCodeFieldUse),
    FieldSet(JsCodeFieldSet),
    Call(JsCodeCall),
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
