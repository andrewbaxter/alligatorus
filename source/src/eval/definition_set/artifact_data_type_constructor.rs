use crate::langhelp::Artifact;

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetCtorData {
    pub name: Option<String>,
    pub data_field: String,
    pub underlying_type: Artifact,
    pub identity: super::data_type_constructor::ArtifactDataUnique,
    pub methods: Vec<ArtifactDataDefSetCtorMethod>,
    pub import_id: crate::langhelp::ImportStateId,
    pub def_set_id: crate::langhelp::DefSetId,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetCtorMethod {
    pub name: String,
    pub field_name: String,
    pub ret_type: Artifact,
    pub compiled_js: String,
}
