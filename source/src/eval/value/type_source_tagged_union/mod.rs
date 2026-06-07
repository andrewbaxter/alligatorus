pub mod dynamic;
pub mod r#const;

use {
    crate::langhelp::{
        Counter,
        Unique,
    },
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

pub(crate) struct SourceTaggedUnionData {
    pub variants: RefCell<Vec<SourceTaggedUnionVariant>>,
    pub identity: Unique,
    pub next_tag_id: Counter,
}

pub(crate) struct SourceTaggedUnionRef {
    pub data: Rc<SourceTaggedUnionData>,
}

pub(crate) struct SourceTaggedUnionVariant {
    pub tag: crate::langhelp::ConstKey,
    pub js_tag: String,
    pub type_: Rc<dyn crate::langhelp::Type>,
}
