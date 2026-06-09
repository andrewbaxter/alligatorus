use crate::langhelp::{
    Artifact,
    ArtifactId,
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSet {
    pub data_types: Vec<ArtifactId>,
    pub def_set_id: crate::langhelp::DefSetId,
    pub import_id: crate::langhelp::ImportStateId,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetDataType {
    pub data_field: String,
    pub def_set: ArtifactId,
    pub identity: crate::langhelp::Unique,
    pub methods: Vec<ArtifactDataDefSetMethod>,
    pub name: Option<String>,
    pub underlying_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetMethod {
    pub compiled_js: String,
    pub field_name: String,
    pub name: String,
    pub ret_type: Artifact,
}
