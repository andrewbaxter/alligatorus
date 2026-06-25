use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
        },
        eval::value::type_root::r#const::ExporterRootDelegate,
        langhelp::{
            EResult,
            ExprContext,
            QExprId,
            Type,
            Value,
        },
    },
    std::rc::Rc,
};

#[derive(Clone)]
pub(crate) struct ValueStringConst(pub String);

impl ExporterAny for ValueStringConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(self.clone()))
    }
}

impl ExporterRootDelegate for ValueStringConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        return EResult::from(
            loc,
            serde_json::from_value::<String>(data).map(|s| Rc::new(ValueStringConst(s)) as Rc<dyn ExporterAny>),
        );
    }

    fn self_to_serial(
        _xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let v = match self_.downcast::<ValueStringConst>() {
            Ok(v) => v,
            Err(_) => return EResult::err(
                loc,
                "ValueStringConst::self_to_serial: expected ValueStringConst".to_string(),
            ),
        };
        return EResult::ok(Artifact::Body(make_body(serde_json::Value::String(v.0.clone()))));
    }
}

impl Value for ValueStringConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(self.clone())
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(super::super::type_string::r#const::TypeStringConst));
    }
}
