use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataSourceObjectNew {
    pub object: Artifact,
}
