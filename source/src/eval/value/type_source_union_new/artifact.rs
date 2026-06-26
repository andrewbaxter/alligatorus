use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataSourceUnionNew {
    pub union: Artifact,
    pub variant_index: usize,
}
