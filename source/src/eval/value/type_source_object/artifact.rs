use crate::{
    artifact::Artifact,
    eval::{
        expr_artifact::ArtifactExpr,
        value::type_source_interface::BuiltinInterfaceKind,
    },
    langhelp::{
        DefSetId,
        Unique,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSet {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub objects: Vec<Artifact>,
    pub def_set_id: DefSetId,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub dependencies: Vec<Artifact>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub functions: Vec<Artifact>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub interfaces: Vec<Artifact>,
}

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSetObject {
    pub data_field: String,
    pub def_set: Artifact,
    pub def_set_id: DefSetId,
    pub identity: Unique,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub interface_impls: Vec<ArtifactDataDefSetInterfaceImpl>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub methods: Vec<ArtifactDataDefSetMethod>,
    pub name: Option<String>,
    pub underlying_type: Artifact,
}

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSetFunction {
    pub arg_type: Artifact,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub captured_externals: Vec<ArtifactExternal>,
    pub compiled_source: String,
    pub def_set: Artifact,
    pub def_set_id: DefSetId,
    pub entry_id: usize,
    pub name: Option<String>,
    pub ret_type: Artifact,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSetInterface {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub builtin_kind: Option<BuiltinInterfaceKind>,
    pub def_set: Artifact,
    pub def_set_id: DefSetId,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    pub identity: Unique,
    #[serde(default, skip_serializing_if = "std::ops::Not::not")]
    pub is_aspect: bool,
    pub methods: Vec<ArtifactDataDefSetInterfaceMethod>,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSetInterfaceImpl {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub builtin_kind: Option<BuiltinInterfaceKind>,
    pub method_impls: Vec<ArtifactDataDefSetInterfaceMethodImpl>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub user_iface: Option<Artifact>,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSetInterfaceMethod {
    pub arg_type: Artifact,
    pub field_name: String,
    pub name: String,
    pub ret_type: Artifact,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSetInterfaceMethodImpl {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub captured_externals: Vec<ArtifactExternal>,
    pub compiled_source: String,
    pub entry_id: usize,
    pub interface_method_index: usize,
}

#[derive(serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactDataDefSetMethod {
    pub arg_type: Artifact,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub captured_externals: Vec<ArtifactExternal>,
    pub compiled_source: String,
    pub entry_id: usize,
    pub field_name: String,
    pub name: String,
    pub ret_type: Artifact,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum ArtifactExternal {
    StagedExpr {
        local_id: usize,
        expr: ArtifactExpr,
    },
    StageLift {
        local_id: usize,
        source_type: Artifact,
    },
    Type {
        local_id: usize,
        type_artifact: Artifact,
    },
    Unique {
        local_id: usize,
        unique: Unique,
    },
}
