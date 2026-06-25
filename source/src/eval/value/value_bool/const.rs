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
pub(crate) struct ValueBoolConst(pub bool);

impl ExporterAny for ValueBoolConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value_>> {
        EResult::ok(Rc::new(*self))
    }
}

impl ExporterRootDelegate for ValueBoolConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        return EResult::from(
            loc,
            serde_json::from_value::<bool>(data).map(|b| Rc::new(ValueBoolConst(b)) as Rc<dyn ExporterAny>),
        );
    }

    fn self_to_serial(
        _xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let v = match self_.downcast::<ValueBoolConst>() {
            Ok(v) => v,
            Err(_) => return EResult::err(
                loc,
                "ValueBoolConst::self_to_serial: expected ValueBoolConst".to_string(),
            ),
        };
        return EResult::ok(Artifact::Body(make_body(serde_json::Value::Bool(v.0))));
    }
}

impl Value_ for ValueBoolConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(*self)
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(super::super::type_bool::r#const::TypeBoolConst));
    }
}
