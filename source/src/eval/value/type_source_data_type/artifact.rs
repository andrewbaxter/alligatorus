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

/// Builtin id for `TypeSourceDataTypeDynamic { data: None }` — the typeless
/// mortar marker. Lifting fills in a data-bearing instance, so the typeless
/// form only appears as a static marker before lift; it serializes to this id
/// with null data and deserializes back to itself.
pub(crate) fn type_source_data_type_ref_artifact_id() -> ArtifactIdBuiltin {
    ArtifactIdBuiltin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

pub(crate) struct DeserializerRef;

impl ArtifactBuiltinDeserializer for DeserializerRef {
    fn value_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::ok(crate::langhelp::CodeValue {
            code: Code { loc, code: Code_::None },
            value: Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: None })
                as Rc<dyn crate::langhelp::Value>,
        })
    }

    fn type_from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EResult<(Rc<dyn Type>, Code)> {
        EResult::ok((
            Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: None }) as Rc<dyn Type>,
            Code { loc, code: Code_::None },
        ))
    }
}

/// Builtin id for serializing a *specific* data type (`TypeSourceDataTypeConst`
/// or `TypeSourceDataTypeDynamic { data: Some(...) }`). The data carries the
/// underlying type, identity, methods, and interface impls; deserialization
/// rebuilds the data-bearing `TypeSourceDataTypeConst` from that data.
pub(crate) fn type_source_data_type_artifact_id() -> ArtifactIdBuiltin {
    ArtifactIdBuiltin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

pub(crate) struct DeserializerDataType;

impl ArtifactBuiltinDeserializer for DeserializerDataType {
    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        super::super::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic.from_serial(xx, loc, data)
    }

    fn type_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EResult<(Rc<dyn Type>, Code)> {
        let cv =
            match super::super::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic
                .from_serial(xx, loc, data)
                .into_res() {
                Ok(v) => v,
                Err(e) => return EResult { v: None, errors: Some(e) },
            };
        let type_value_type = cv.value.type_(xx, loc);
        match crate::builtin::aspect_type_get(xx, loc, type_value_type.as_ref()).into_res() {
            Ok(aspect_type) => EResult::ok((aspect_type.type_(cv.value.as_ref()), cv.code)),
            Err(e) => EResult { v: None, errors: Some(e) },
        }
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactBuiltinDeserializer>>) {
    m.insert(type_source_data_type_ref_artifact_id(), Rc::new(DeserializerRef));
    m.insert(type_source_data_type_artifact_id(), Rc::new(DeserializerDataType));
}
