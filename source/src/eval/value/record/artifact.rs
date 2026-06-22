use {
    crate::{
        artifact::Artifact,
        langhelp::ConstKey,
    },
    std::collections::BTreeMap,
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactRecordType {
    pub fields: BTreeMap<ConstKey, Artifact>,
}

pub(crate) struct TypeRecordArtifact;
