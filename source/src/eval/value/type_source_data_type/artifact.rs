use {
    crate::{
        builtin::{
            ArtifactBuiltinDeserializer,
            AspectArtifact,
        },
        langhelp::{
            Artifact,
            ArtifactId,
            ArtifactIdBuiltin,
            Code,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
        },
    },
    std::{
        collections::HashMap,
        rc::Rc,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSet {
    pub data_types: Vec<ArtifactId>,
    pub def_set_id: crate::langhelp::DefSetId,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub functions: Vec<ArtifactId>,
    pub import_id: crate::langhelp::ImportStateId,
    pub source_id: crate::langhelp::SourceId,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetFunction {
    pub arg_type: Artifact,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub captured_externals: Vec<ArtifactExternal>,
    pub compiled_js: String,
    pub def_set: ArtifactId,
    pub entry_id: usize,
    pub name: Option<String>,
    pub ret_type: Artifact,
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

/// Serialized form of a body's `External`. The body's `compiled_js` references
/// each by its body-local id (`a##`); at import the cell is reconstructed in
/// the importing env and installed under `<func_name>_<local_key>` so the
/// body's wrapping block can `let a## = globalThis["..."];` them.
#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) enum ArtifactExternal {
    /// `External::StagedExprCell` wrapping an `Expr`. The expression's AST is
    /// serialized as `ArtifactExpr`.
    StagedExpr {
        local_id: usize,
        expr: crate::eval::expr_artifact::ArtifactExpr,
    },
    /// `External::StageLiftCell`. The source type (whose `AspectLiftDynamic`
    /// is wrapped) is serialized; the runtime scope/target/vx come from the
    /// importing env at re-install time.
    StageLift {
        local_id: usize,
        source_type: Artifact,
    },
    /// `External::Type`. The type ref is serialized via `serialize_type_ref`.
    Type {
        local_id: usize,
        type_artifact: Artifact,
    },
    /// `External::Unique`. The captured `Unique` is JSON-roundtripped (it's
    /// already `serde::Serialize` + `Deserialize`).
    Unique {
        local_id: usize,
        unique: crate::langhelp::Unique,
    },
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetMethod {
    pub arg_type: Artifact,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub captured_externals: Vec<ArtifactExternal>,
    pub compiled_js: String,
    pub entry_id: usize,
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
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub captured_externals: Vec<ArtifactExternal>,
    pub compiled_js: String,
    pub entry_id: usize,
    /// The user's impl Expr, serialized. Used to reconstruct the staged
    /// `Expr` values referenced by the compiled method body (which calls
    /// `eval_const` on them at dispatch time) when the data type is imported
    /// into another isolate.
    pub impl_expr: crate::eval::expr_artifact::ArtifactExpr,
    pub interface_method_index: usize,
}

pub(crate) struct DeserializerRef;

impl ArtifactBuiltinDeserializer for DeserializerRef {
    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::ok(crate::langhelp::CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(super::dynamic::TypeSourceDataTypeRefDynamic)
                as Rc<dyn crate::langhelp::Value>,
        })
    }

    fn type_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeSourceDataTypeRefDynamic), Code { loc, code: Code_::None }))
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    // Source-defined data types serialize as `Artifact { type_: Source(N),
    // data: null }` where `N` points to the data-type's own artifact (which is
    // tagged `Builtin(TypeTypeSourceDataTypeDynamic::artifact_id())`). The
    // Source branch of `deserialize_(type_)artifact` handles that path.
    m.insert(super::dynamic::TypeSourceDataTypeRefDynamic::artifact_id(), Rc::new(DeserializerRef));
}
