pub mod r#const;
pub mod dynamic;

use std::rc::Rc;

pub(crate) struct SourceTaggedUnionConstructorRef {
    pub data: Rc<super::type_source_tagged_union::SourceTaggedUnionData>,
    pub variant_index: usize,
}
