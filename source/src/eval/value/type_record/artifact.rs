use {
    crate::{
        artifact::Artifact,
        langhelp::ConstKey,
    },
    std::collections::BTreeMap,
};

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactRecordType {
    pub fields: BTreeMap<ConstKey, Artifact>,
}
