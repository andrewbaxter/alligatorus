use crate::{
    artifact::Artifact,
    langhelp::{
        ConstKey,
        Unique,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataSourceTaggedUnion {
    pub identity: Unique,
    pub variants: Vec<ArtifactDataSourceTaggedUnionVariant>,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataSourceTaggedUnionVariant {
    pub js_tag: String,
    pub tag: ConstKey,
    pub type_: Artifact,
}
