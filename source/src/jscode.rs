use crate::langhelp::{
    CodeDyn,
    QExprId,
};

#[derive(PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash)]
pub struct ScopeEntryId(pub usize);

#[derive(Clone)]
pub struct JsCodeSeq {
    pub sub: Vec<JsCode>,
}

#[derive(Clone)]
pub struct JsCodeSet {
    pub id: ScopeEntryId,
    pub value: Box<JsCode>,
}

#[derive(Clone)]
pub struct JsCodeGet {
    pub id: ScopeEntryId,
}

#[derive(Clone)]
pub struct JsCodeCall {
    pub func: Box<JsCode>,
    pub args: Vec<JsCode>,
}

#[derive(Clone)]
pub enum JsCodeType {
    Seq(JsCodeSeq),
    Value(serde_json::Value),
    Set(JsCodeSet),
    Get(JsCodeGet),
    Call(JsCodeCall),
}

#[derive(Clone)]
pub struct JsCode {
    pub loc: QExprId,
    pub type_: JsCodeType,
}

impl CodeDyn for JsCode {
    fn loc(&self) -> QExprId {
        return self.loc;
    }
}
