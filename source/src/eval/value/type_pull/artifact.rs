use {
    crate::artifact::Artifact,
    serde::{
        Deserialize,
        Serialize,
    },
};

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataPull {
    pub data_type: Artifact,
    pub inner: Artifact,
}
