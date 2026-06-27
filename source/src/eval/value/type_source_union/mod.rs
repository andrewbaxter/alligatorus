pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        artifact::ArtifactIdSource,
        langhelp::{
            ConstKey,
            Counter,
            Type,
            Unique,
        },
    },
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

pub(crate) struct SourceUnionData {
    pub identity: Unique,
    pub new_artifact_id: RefCell<Option<crate::artifact::ArtifactIdSource>>,
    pub next_tag_id: Counter,
    pub variants: RefCell<Vec<SourceUnionVariant>>,
}

pub(crate) struct SourceUnionRef {
    pub data: Rc<SourceUnionData>,
}

pub(crate) struct SourceUnionVariant {
    pub js_tag: String,
    pub new_artifact_id: RefCell<Option<ArtifactIdSource>>,
    pub tag: ConstKey,
    pub type_: Rc<dyn Type>,
}
