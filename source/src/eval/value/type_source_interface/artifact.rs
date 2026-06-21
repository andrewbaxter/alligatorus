use {
    crate::artifact::ArtifactExporter,
    crate::artifact::ArtifactIdBuiltin,
    crate::{
        eval::{            value::type_source_interface::BuiltinInterfaceKind,
        },
        langhelp::{
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            Type,
        },
        r#gen::gen_build_builtin_interfaces,
    },
    std::{
        collections::HashMap,
        rc::Rc,
    },
};

pub(crate) struct Deserializer;

impl Deserializer {
    fn lookup(xx: &ExprContext, kind: BuiltinInterfaceKind) -> Rc<dyn Type> {
        let ifaces = gen_build_builtin_interfaces(xx.import_state.get());
        let data = match kind {
            BuiltinInterfaceKind::Value => ifaces.value,
            BuiltinInterfaceKind::Type => ifaces.type_,
            BuiltinInterfaceKind::AspectFunction => ifaces.aspect_function,
            BuiltinInterfaceKind::AspectField => ifaces.aspect_field,
            BuiltinInterfaceKind::ScopeEntry => ifaces.scope_entry,
            BuiltinInterfaceKind::Target => ifaces.target,
            BuiltinInterfaceKind::CodeDyn => ifaces.code_dyn,
        };
        Rc::new(super::r#const::TypeSourceInterfaceConst { data })
    }
}

impl ArtifactExporter for Deserializer {
    fn type_from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> EResult<(Rc<dyn Type>, Code)> {
        let kind: BuiltinInterfaceKind = match serde_json::from_value(data.clone()) {
            Ok(k) => k,
            Err(e) => return EResult::err(loc, format!("Failed to parse interface kind: {}", e)),
        };
        EResult::ok((Self::lookup(xx, kind), Code {
            loc,
            code: Code_::None,
        }))
    }

    fn value_from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        let kind: BuiltinInterfaceKind = match serde_json::from_value(data.clone()) {
            Ok(k) => k,
            Err(e) => return EvalRes::err(loc, format!("Failed to parse interface kind: {}", e)),
        };
        let type_ = Self::lookup(xx, kind);
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: type_.as_value(),
        })
    }
}

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactExporter>>) {
    m.insert(TypeSourceInterfaceConstArtifact::artifact_id(), Rc::new(Deserializer));
}
pub(crate) struct TypeSourceInterfaceConstArtifact;
