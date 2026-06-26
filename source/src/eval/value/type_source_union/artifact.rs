use crate::{
    artifact::Artifact,
    langhelp::{
        ConstKey,
        Unique,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataSourceUnion {
    pub identity: Unique,
    pub variants: Vec<ArtifactDataSourceUnionVariant>,
}

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataSourceUnionVariant {
    pub js_tag: String,
    pub tag: ConstKey,
    pub type_: Artifact,
}
