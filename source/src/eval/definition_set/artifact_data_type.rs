use crate::langhelp::{
    Artifact,
    ArtifactId,
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetDataType {
    pub def_set: ArtifactId,
    pub type_index: usize,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSet {
    pub data_types: Vec<ArtifactDataDefSetDataTypeEntry>,
    pub import_id: crate::langhelp::ImportStateId,
    pub def_set_id: crate::langhelp::DefSetId,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetDataTypeEntry {
    pub name: Option<String>,
    pub underlying_type: Artifact,
    pub identity: super::data_type_constructor::ArtifactDataUnique,
    pub data_field: String,
    pub methods: Vec<ArtifactDataDefSetMethod>,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetMethod {
    pub name: String,
    pub field_name: String,
    pub ret_type: Artifact,
    pub compiled_js: String,
}
