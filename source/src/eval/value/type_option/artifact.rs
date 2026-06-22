use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactOptionType {
    pub element_type: Artifact,
}

pub(crate) struct TypeOptionArtifact;
