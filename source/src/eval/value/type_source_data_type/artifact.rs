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
