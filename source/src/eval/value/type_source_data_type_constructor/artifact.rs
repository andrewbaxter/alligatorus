use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataSourceDataTypeConstructor {
    pub data_type: Artifact,
}
