use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            import_artifact,
            export_artifact,
        },
        eval::value::{
            type_root::r#const::ExporterRootDelegate,
            type_source_data_type::{
                SourceDataTypeData,
                r#const::TypeSourceDataTypeConst,
            },
        },
        langhelp::{
            EResult,
            ExprContext,
            ExprErr,
            QExprId,
            Type,
            Value,
        },
    },
    dumpster::unsync::Gc,
    serde::{
        Deserialize,
        Serialize,
    },
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
};

#[derive(Serialize, Deserialize)]
pub(crate) struct ArtifactValueSourceDataType {
    pub type_artifact: Artifact,
    pub underlying: Artifact,
}

#[derive(Clone)]
pub(crate) struct ValueSourceDataTypeConst {
    pub data: Gc<SourceDataTypeData>,
    pub isolate_bundle: RefCell<Option<v8::Global<v8::Object>>>,
    pub underlying: Rc<dyn Value>,
}

impl ExporterAny for ValueSourceDataTypeConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(self.clone()))
    }
}

impl ExporterRootDelegate for ValueSourceDataTypeConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        _id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        let body: ArtifactValueSourceDataType = match serde_json::from_value(data) {
            Ok(v) => v,
            Err(e) => return EResult::err(loc, e.to_string()),
        };
        let mut errors: Option<Vec<ExprErr>> = None;
        let type_any = import_artifact(xx, loc, body.type_artifact);
        if let Some(errors1) = type_any.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(type_any) = type_any.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let rc_any: Rc<dyn Any> = type_any;
        let Ok(wrapped) = rc_any.downcast::<ExporterDataAny<Gc<SourceDataTypeData>>>() else {
            return EResult::err(
                loc,
                "ValueSourceDataTypeConst: type artifact did not deserialize to a data type".to_string(),
            );
        };
        let dt_data = wrapped.0.clone();
        let underlying_any = import_artifact(xx, loc, body.underlying);
        if let Some(errors1) = underlying_any.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(underlying_any) = underlying_any.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let underlying_res = underlying_any.as_value();
        if let Some(errors1) = underlying_res.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(underlying) = underlying_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let has_type_iface = dt_data.interface_impls.borrow().iter().any(|impl_data| {
            impl_data.interface.builtin_kind ==
                Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
        });
        let isolate_bundle = if has_type_iface {
            match crate::eval::value::type_pull::r#const::pull_mini_eval(xx, loc, &dt_data, underlying.clone()) {
                Ok(b) => RefCell::new(Some(b)),
                Err(e) => return EResult::err(loc, format!("ValueSourceDataTypeConst pull_mini_eval: {}", e)),
            }
        } else {
            RefCell::new(None)
        };
        let out: Rc<dyn ExporterAny> = Rc::new(ValueSourceDataTypeConst {
            data: dt_data,
            isolate_bundle,
            underlying,
        });
        return EResult {
            errors,
            v: Some(out),
        };
    }

    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = match self_.downcast::<ValueSourceDataTypeConst>() {
            Ok(v) => v,
            Err(_) => return EResult::err(
                loc,
                "ValueSourceDataTypeConst::self_export: type mismatch".to_string(),
            ),
        };
        let mut errors: Option<Vec<ExprErr>> = None;
        let type_value = TypeSourceDataTypeConst { data: self_.data.clone() };
        let type_art = export_artifact(xx, loc, &type_value as &dyn Value);
        if let Some(errors1) = type_art.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(type_art) = type_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let underlying_art = export_artifact(xx, loc, self_.underlying.as_ref());
        if let Some(errors1) = underlying_art.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(underlying_art) = underlying_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let body = ArtifactValueSourceDataType {
            type_artifact: type_art,
            underlying: underlying_art,
        };
        return EResult {
            errors,
            v: Some(Artifact::Body(make_body(serde_json::to_value(&body).unwrap()))),
        };
    }
}

impl Value for ValueSourceDataTypeConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(self.clone())
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }))
    }
}
