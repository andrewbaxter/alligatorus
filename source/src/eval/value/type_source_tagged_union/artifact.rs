use crate::{
    artifact::Artifact,
    langhelp::{
        ConstKey,
        Unique,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataSourceTaggedUnionVariant {
    pub tag: ConstKey,
    pub js_tag: String,
    pub type_: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataSourceTaggedUnion {
    pub identity: Unique,
    pub variants: Vec<ArtifactDataSourceTaggedUnionVariant>,
}
