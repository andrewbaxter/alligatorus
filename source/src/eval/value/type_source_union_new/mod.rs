pub mod artifact;
pub mod dynamic;
pub mod r#const;

use std::rc::Rc;

pub(crate) struct SourceUnionNewRef {
    pub data: Rc<super::type_source_union::SourceUnionData>,
    pub variant_index: usize,
}
