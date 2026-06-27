use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactListType {
    pub element_type: Artifact,
}
