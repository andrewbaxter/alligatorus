use crate::langhelp::{
    Artifact,
    DefSetId,
    ImportStateId,
    SourceId,
    Unique,
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetCtorData {
    pub data_field: String,
    pub def_set_id: DefSetId,
    pub identity: Unique,
    pub import_id: ImportStateId,
    pub methods: Vec<ArtifactDataDefSetCtorMethod>,
    pub name: Option<String>,
    pub source_id: SourceId,
    pub underlying_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetCtorMethod {
    pub compiled_js: String,
    pub field_name: String,
    pub name: String,
    pub ret_type: Artifact,
}
