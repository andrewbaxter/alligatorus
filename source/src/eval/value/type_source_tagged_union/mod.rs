pub mod artifact;
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
    pub identity: Unique,
    pub next_tag_id: Counter,
    pub variants: RefCell<Vec<SourceTaggedUnionVariant>>,
}

pub(crate) struct SourceTaggedUnionRef {
    pub data: Rc<SourceTaggedUnionData>,
}

pub(crate) struct SourceTaggedUnionVariant {
    pub js_tag: String,
    pub tag: crate::langhelp::ConstKey,
    pub type_: Rc<dyn crate::langhelp::Type>,
}
