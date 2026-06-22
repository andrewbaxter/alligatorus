use {
    crate::{
        artifact::{
            Artifact,
            ArtifactExporter,
            ArtifactId,
            ArtifactIdBuiltin,
        },
        eval::{
            expr_artifact::ArtifactExpr,
            value::type_source_interface::BuiltinInterfaceKind,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            DefSetId,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        collections::HashMap,
        rc::Rc,
    },
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSet {
    pub def_set_id: DefSetId,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub dependencies: Vec<ArtifactId>,
    pub members: Vec<ArtifactId>,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetDataType {
    pub data_field: String,
    pub def_set: ArtifactId,
    pub identity: Unique,
    pub members: Vec<ArtifactId>,
    pub name: Option<String>,
    pub underlying_type: Artifact,
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

#[derive(Clone, serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetInterface {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    pub identity: Unique,
    #[serde(default, skip_serializing_if = "std::ops::Not::not")]
    pub is_aspect: bool,
    pub methods: Vec<ArtifactDataDefSetInterfaceMethod>,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetInterfaceImpl {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub builtin_kind: Option<BuiltinInterfaceKind>,
    pub method_impls: Vec<ArtifactId>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub user_iface: Option<ArtifactId>,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetInterfaceMethod {
    pub arg_type: Artifact,
    pub field_name: String,
    pub name: String,
    pub ret_type: Artifact,
}

#[derive(Clone, serde::Serialize, serde::Deserialize)]
pub(crate) struct ArtifactDataDefSetInterfaceMethodImpl {
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub captured_externals: Vec<ArtifactExternal>,
    pub compiled_js: String,
    pub entry_id: usize,
    pub impl_expr: ArtifactExpr,
    pub interface_method_index: usize,
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

#[derive(Clone, serde::Serialize, serde::Deserialize)]
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
pub(crate) struct DeserializerRef;

impl ArtifactExporter for DeserializerRef {
    fn type_from_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((Rc::new(super::dynamic::TypeSourceDataTypeRefDynamic), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Rc::new(super::dynamic::TypeSourceDataTypeRefDynamic) as Rc<dyn Value>,
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactExporter>>) {
    m.insert(super::dynamic::TypeSourceDataTypeRefDynamic::artifact_id(), Rc::new(DeserializerRef));
}
