use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactBodyExporter,
            ArtifactExporter,
            ArtifactIdSource,
            ExporterAny,
        },
        eval::value::{
            type_list::dynamic::TypeListDynamic,
            type_null::r#const::TypeNullConst,
        },
        langhelp::{
            EResult,
            ExprContext,
            ExprErr,
            ExprSubErr,
            GlobalContext,
            QExprId,
            Type,
            Value,
        },
        r#gen::{
            AspectArtifact,
            aspect_artifact_aspect_id,
        },
    },
    serde::{
        Deserialize,
        Serialize,
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) trait TypeRootExporter {
    fn self_from_artifact_data(xx: &ExprContext, loc: QExprId, data: serde_json::Value) -> EResult<Rc<dyn Any>>;
    fn self_to_artifact_data(xx: &ExprContext, loc: QExprId, self_: Rc<dyn Any>) -> EResult<serde_json::Value>;
}

#[derive(Clone, Copy, Serialize, Deserialize)]
pub enum TypeRoot {
    ListDynamic,
    NullConst,
    Root,
}

impl TypeRoot {
    fn self_to_serial_exporter(self) -> ArtifactBodyExporter {
        match self {
            TypeRoot::Root => ArtifactBodyExporter::Root,
            _ => ArtifactBodyExporter::Body(Box::new(ArtifactBody {
                data: serde_json::to_value(self).unwrap(),
                exporter: TypeRoot::Root.self_to_serial_exporter(),
            })),
        }
    }
}

impl ArtifactExporter for TypeRoot {
    fn from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        id: Option<ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn std::any::Any>> {
        let res: EResult<Rc<dyn Any>> = match self {
            TypeRoot::ListDynamic => TypeListDynamic::self_from_artifact_data(&xx, loc, data),
            TypeRoot::NullConst => TypeNullConst::self_from_artifact_data(&xx, loc, data),
            TypeRoot::Root => EResult::from(
                loc,
                serde_json::from_value::<TypeRoot>(data).map(|r| Rc::new(r).into()),
            ),
        };
        let mut errors = None;
        if let Some(errors1) = res.errors {
            errors.get_or_default().extend(errors1);
        }
        let Some(data) = res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors: None,
            v: Some(data),
        };
    }

    fn to_serial(self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn std::any::Any>) -> EResult<Artifact> {
        let mut errors = None;
        let mut data = match self {
            TypeRoot::ListDynamic => TypeListDynamic::self_to_artifact_data(xx, loc, self_),
            TypeRoot::NullConst => TypeNullConst::self_to_artifact_data(xx, loc, self_),
            TypeRoot::Root => EResult::ok(serde_json::to_value(self).unwrap()),
        };
        data.push_errors(&mut errors);
        let Some(data) = data.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult::ok(Artifact::Body(ArtifactBody {
            data: data,
            exporter: self.self_to_serial_exporter(),
        }));
    }
}

impl AspectArtifact for TypeRoot {
    fn exporter(&self, xx: &ExprContext, loc: QExprId) -> Rc<dyn ArtifactExporter> {
        return Rc::new(TypeRoot::Root);
    }
}

impl ExporterAny for TypeRoot {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeRoot {
    fn as_value(&self) -> Rc<dyn Value> {
        return Rc::new(*self);
    }

    fn aspect(&self, xx: &ExprContext, key: &crate::langhelp::Unique) -> Option<Box<dyn std::any::Any>> {
        if key == aspect_artifact_aspect_id() {
            return Some(Box::new(*self));
        }
        return None;
    }

    fn desc(&self) -> String {
        return format!("{} type", match self {
            TypeRoot::Root => "Root type",
            TypeRoot::ListDynamic => "List type (dynamic)",
            TypeRoot::NullConst => "Null type (const)",
        });
    }
}

impl Value for TypeRoot {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(*self);
    }

    fn type_(&self, xx: &ExprContext, loc: QExprId) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeRoot::Root));
    }
}
