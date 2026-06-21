use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactListType {
    pub element_type: Artifact,
}
