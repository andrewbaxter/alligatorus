use crate::artifact::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactMapType {
    pub key_type: Artifact,
    pub value_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactMapNewType {
    pub key_type: Artifact,
    pub value_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactMapGetFnType {
    pub key_type: Artifact,
    pub value_type: Artifact,
}

pub(crate) struct TypeMapArtifact;
