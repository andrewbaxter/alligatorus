use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactMapGetFnType {
    pub key_type: Artifact,
    pub value_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactMapNewType {
    pub key_type: Artifact,
    pub value_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactMapType {
    pub key_type: Artifact,
    pub value_type: Artifact,
}
pub(crate) struct TypeMapArtifact;
