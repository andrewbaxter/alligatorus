use crate::langhelp::{
    Artifact,
    ArtifactId,
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSet {
    pub data_types: Vec<ArtifactDataDefSetDataTypeEntry>,
    pub def_set_id: crate::langhelp::DefSetId,
    pub import_id: crate::langhelp::ImportStateId,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetDataType {
    pub def_set: ArtifactId,
    pub type_index: usize,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetDataTypeEntry {
    pub data_field: String,
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
