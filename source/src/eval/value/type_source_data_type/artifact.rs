use crate::langhelp::{
    Artifact,
    ArtifactId,
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSet {
    pub data_types: Vec<ArtifactId>,
    pub def_set_id: crate::langhelp::DefSetId,
    pub import_id: crate::langhelp::ImportStateId,
    pub source_id: crate::langhelp::SourceId,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetDataType {
    pub data_field: String,
    pub def_set: ArtifactId,
    pub identity: crate::langhelp::Unique,
    pub interface_impls: Vec<ArtifactDataDefSetInterfaceImpl>,
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

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetInterfaceImpl {
    pub builtin_kind: crate::eval::value::type_source_interface::BuiltinInterfaceKind,
    pub method_impls: Vec<ArtifactDataDefSetInterfaceMethodImpl>,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetInterfaceMethodImpl {
    pub compiled_js: String,
    pub entry_id: usize,
    /// The user's impl Expr, serialized. Used to reconstruct the staged
    /// `Expr` values referenced by the compiled method body (which calls
    /// `eval_const` on them at dispatch time) when the data type is imported
    /// into another isolate.
    pub impl_expr: crate::eval::expr_artifact::ArtifactExpr,
    pub interface_method_index: usize,
}
