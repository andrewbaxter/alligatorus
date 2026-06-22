use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactListType {
    pub element_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactListNewType {
    pub element_type: Artifact,
}
