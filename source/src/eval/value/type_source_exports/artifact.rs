use {
    crate::{
        artifact::{
            Artifact,
            ArtifactExporter,
            ArtifactId,
            ArtifactIdBuiltin,
            ArtifactIdSource,
            ExporterAny,
            TypeFromAny,
        },
        eval::definition_set::load_def_set,
        langhelp::{
            Aspect,
            EResult,
            ExprContext,
            QExprId,
            Type,
        },
        r#gen::AspectArtifact,
    },
    std::{
        any::Any,
        collections::HashMap,
        rc::Rc,
    },
};

pub(crate) fn register(m: &mut HashMap<ArtifactIdBuiltin, Rc<dyn ArtifactExporter>>) {
    TypeSourceExportsLiftedConstArtifact::register(m);
}
pub(crate) struct TypeSourceExportsLiftedConstArtifact;

impl ArtifactExporter for TypeSourceExportsLiftedConstArtifact {
    fn from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        _id: Option<ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        let def_set_id: ArtifactId = match serde_json::from_value(data) {
            Ok(id) => id,
            Err(e) => return EResult::err(loc, format!("Failed to parse exports def_set_id: {}", e)),
        };
        let def_set = match load_def_set(xx, &def_set_id) {
            Ok(d) => d,
            Err(e) => return EResult::err(loc, e),
        };
        xx.vx.definition_sets.borrow_mut().entry(def_set.def_set_id).or_insert_with(|| def_set.clone());
        let exports_type: Rc<dyn Type> = Rc::new(super::r#const::TypeSourceExportsLiftedConst { set: def_set });
        EResult::ok(Rc::new(TypeFromAny(exports_type)) as Rc<dyn Any>)
    }

    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn ExporterAny>) -> EResult<Artifact> {
        let rc_any = self_.as_any_rc();
        let exports =
            rc_any
                .downcast::<super::r#const::TypeSourceExportsLiftedConst>()
                .expect("TypeSourceExportsLiftedConstArtifact::to_serial: expected TypeSourceExportsLiftedConst");
        let def_set_artifact_id = crate::eval::definition_set::serialize_definition_set(xx, loc, &exports.set);
        EResult::ok(Artifact::Body(Self::build_artifact(ArtifactId::Source(def_set_artifact_id))))
    }
}

impl Aspect for TypeSourceExportsLiftedConstArtifact { }

impl AspectArtifact for TypeSourceExportsLiftedConstArtifact {
    fn exporter(&self) -> Rc<dyn ArtifactExporter> {
        Rc::new(TypeSourceExportsLiftedConstArtifact)
    }
}
