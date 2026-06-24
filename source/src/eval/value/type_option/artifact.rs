use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactOptionType {
    pub element_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactOptionVariantConstructor {
    pub element_type: Artifact,
    pub is_some: bool,
}
pub(crate) struct TypeOptionArtifact;
