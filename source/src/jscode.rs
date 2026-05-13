use {
    crate::langhelp::{
        CodeDyn,
        QExprId,
        ScopeEntryId,
    },
    std::{
        any::Any,
        rc::Rc,
    },
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
pub(crate) struct JsCodeField {
    pub(crate) base: Box<JsCode>,
    pub(crate) field: String,
}

#[derive(Clone)]
pub(crate) struct JsCodeCall {
    pub(crate) func: Box<JsCode>,
    pub(crate) args: Vec<JsCode>,
}

#[derive(Clone)]
pub(crate) struct JsCodeExternal {
    /// Opaque value to pass through JS as v8::External.
    pub(crate) value: Rc<dyn Any>,
}

#[derive(Clone)]
pub(crate) enum JsCodeType {
    Seq(JsCodeSeq),
    Value(serde_json::Value),
    FieldSet(JsCodeFieldSet),
    Bind(JsCodeBind),
    BindingSet(JsCodeBindingSet),
    BindingUse(JsCodeBindingUse),
    Field(JsCodeField),
    Call(JsCodeCall),
    /// A Rust value embedded as a v8::External. The id indexes into a registry
    /// set up before JS execution.
    External(JsCodeExternal),
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
