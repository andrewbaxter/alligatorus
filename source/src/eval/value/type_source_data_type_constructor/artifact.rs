use crate::{
    artifact::Artifact,
    langhelp::{
        DefSetId,
        Unique,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetCtorData {
    pub data_field: String,
    pub def_set_id: DefSetId,
    pub identity: Unique,
    pub methods: Vec<ArtifactDataDefSetCtorMethod>,
    pub name: Option<String>,
    pub underlying_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetCtorMethod {
    pub compiled_js: String,
    pub field_name: String,
    pub name: String,
    pub ret_type: Artifact,
}
