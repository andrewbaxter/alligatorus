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
            Value_,
        },
    },
    std::rc::Rc,
};

#[derive(Clone, Copy)]
pub(crate) struct ValueNumberConst(pub f64);

impl ExporterAny for ValueNumberConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        return EResult::ok(Rc::new(*self));
    }
}

impl ExporterRootDelegate for ValueNumberConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        return EResult::from(
            loc,
            serde_json::from_value::<f64>(data).map(|n| Rc::new(ValueNumberConst(n)) as Rc<dyn ExporterAny>),
        );
    }

    fn self_to_serial(
        _xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let v = match self_.downcast::<ValueNumberConst>() {
            Ok(v) => v,
            Err(_) => return EResult::err(
                loc,
                "ValueNumberConst::self_to_serial: expected ValueNumberConst".to_string(),
            ),
        };
        return EResult::ok(Artifact::Body(make_body(serde_json::to_value(v.0).unwrap())));
    }
}

impl Value_ for ValueNumberConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(*self);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(super::super::type_number::r#const::TypeNumberConst));
    }
}
