pub mod dynamic;
pub mod r#const;

use std::rc::Rc;

pub(crate) struct SourceTaggedUnionConstructorRef {
    pub data: Rc<super::type_source_tagged_union::SourceTaggedUnionData>,
    pub variant_index: usize,
}
