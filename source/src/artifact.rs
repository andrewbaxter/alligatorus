use {
    crate::{
        eval::value::type_root::r#const::TypeRoot,
        langhelp::{
            CompileId,
            EResult,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
            Type,
            Value,
        },
        r#gen::{
            AspectArtifact,
            aspect_artifact_get,
        },
    },
    flowcontrol::shed,
    serde::{
        Deserialize,
        Serialize,
        de::DeserializeOwned,
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) trait ArtifactExporter {
    fn from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        // None for inline artifacts (e.g. sometimes trivial exporters)
        id: Option<ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>>;
    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn ExporterAny>) -> EResult<Artifact>;
}

pub(crate) trait Artifacts: Send + Sync {
    fn get(&self, id: ArtifactIdSource) -> Result<ArtifactBody, String>;
    fn reserve_id(&self) -> ArtifactIdSource;
    fn set(&self, id: ArtifactIdSource, data: ArtifactBody);
}

pub(crate) trait ExporterAny: Any {
    fn as_type(&self) -> EResult<Rc<dyn Type>>;
    fn as_value(&self) -> EResult<Rc<dyn Value>>;
}

impl dyn ExporterAny {
    pub(crate) fn downcast<T: ExporterAny>(self: Rc<Self>) -> Result<Rc<T>, Rc<dyn Any>> {
        let any: Rc<dyn Any> = self;
        any.downcast::<T>()
    }
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum Artifact {
    Body(ArtifactBody),
    Ref(ArtifactIdSource),
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactBody {
    pub(crate) data: serde_json::Value,
    pub(crate) exporter: ArtifactBodyExporter,
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum ArtifactBodyExporter {
    Body(Box<ArtifactBody>),
    Root,
    SourceRef(ArtifactIdSource),
}

struct ArtifactDeserializerFromAny {
    v: Rc<dyn ArtifactExporter>,
}

impl ExporterAny for ArtifactDeserializerFromAny {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

pub(crate) type ArtifactFinalizer = Box<dyn FnOnce(&ExprContext, QExprId) -> ()>;

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactIdSource(pub CompileId, pub usize);

pub(crate) fn deserialize_artifact(
    xx: &ExprContext,
    loc: QExprId,
    artifact: Artifact,
) -> EResult<Rc<dyn ExporterAny>> {
    let id;
    let body = match artifact {
        Artifact::Body(a) => {
            id = None;
            a
        },
        Artifact::Ref(id1) => {
            id = Some(id1);
            if let Some(out) = xx.vx.ix.deserialized_artifacts.borrow().get(&id1) {
                return EResult {
                    errors: None,
                    v: Some(out.clone()),
                };
            }
            match xx.vx.ix.gx.artifacts.get(id1) {
                Ok(v) => v,
                Err(e) => return EResult::err(loc, e),
            }
        },
    };

    fn deserialize_artifact_body(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<ArtifactIdSource>,
        body: ArtifactBody,
    ) -> EResult<Rc<dyn ExporterAny>> {
        let mut errors: Option<Vec<ExprErr>> = None;
        let exporter = shed!{
            'exporter _;
            let exporter_any = shed!{
                'any _;
                match body.exporter {
                    ArtifactBodyExporter::Body(body) => {
                        let any = deserialize_artifact_body(xx, loc, None, *body);
                        if let Some(errors1) = any.errors {
                            errors.get_or_insert_default().extend(errors1);
                        }
                        let Some(any) = any.v else {
                            return EResult {
                                errors,
                                v: None
                            };
                        };
                        break 'any any;
                    },
                    ArtifactBodyExporter::SourceRef(id) => {
                        if let Some(out) = xx.vx.ix.deserialized_artifacts.borrow().get(&id) {
                            break 'any out.clone();
                        }
                        let body = match xx.vx.ix.gx.artifacts.get(id) {
                            Ok(v) => v,
                            Err(e) => return EResult::err(loc, e),
                        };
                        let any = deserialize_artifact_body(xx, loc, Some(id), body);
                        if let Some(errors1) = any.errors {
                            errors.get_or_insert_default().extend(errors1);
                        }
                        let Some(any) = any.v else {
                            return EResult {
                                errors,
                                v: None
                            };
                        };
                        break 'any any;
                    },
                    ArtifactBodyExporter::Root => {
                        break 'exporter TypeRoot::Root.exporter();
                    },
                }
            };
            let exporter = match exporter_any.downcast::<ArtifactDeserializerFromAny>() {
                Ok(v) => v,
                Err(e) => {
                    errors.get_or_insert_default().push(ExprErr {
                        errors: vec![
                            ExprSubErr::Opaque("Artifact deserializer reference is not a deserializer".to_string(),),
                        ],
                        loc: loc,
                    });
                    return EResult {
                        errors: errors,
                        v: None,
                    };
                },
            };
            return EResult {
                errors,
                v: Some(exporter)
            }
        };
        return exporter.from_serial(xx, loc, id, body.data);
    }

    return deserialize_artifact_body(xx, loc, id, body);
}

pub(crate) fn deserialize_type_artifact(
    xx: &ExprContext,
    loc: QExprId,
    artifact: Artifact,
) -> EResult<Rc<dyn Type>> {
    let res = deserialize_artifact(xx, loc, artifact);
    let mut errors = res.errors;
    let Some(v) = res.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    let v = v.as_type();
    if let Some(errors1) = v.errors {
        errors.get_or_insert_default().extend(errors1);
    }
    let Some(v) = v.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    return EResult {
        errors,
        v: Some(v),
    };
}

pub(crate) fn independent_finalize_deserialize_type_artifact(
    xx: &ExprContext,
    loc: QExprId,
    artifact: Artifact,
) -> EResult<Rc<dyn Type>> {
    let res = independent_finalize_deserialize_artifact(xx, loc, artifact);
    let mut errors = res.errors;
    let Some(v) = res.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    let v = v.as_type();
    if let Some(errors1) = v.errors {
        errors.get_or_insert_default().extend(errors1);
    }
    let Some(v) = v.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    return EResult {
        errors,
        v: Some(v),
    };
}

pub(crate) fn independent_finalize_deserialize_artifact(
    xx: &ExprContext,
    loc: QExprId,
    artifact: Artifact,
) -> EResult<Rc<dyn ExporterAny>> {
    fn lookup_ref(
        xx: &ExprContext,
        loc: QExprId,
        id: ArtifactIdSource,
    ) -> EResult<Rc<dyn ExporterAny>> {
        match xx.vx.ix.deserialized_artifacts.borrow().get(&id) {
            Some(v) => EResult::ok(v.clone()),
            None => EResult::err(
                loc,
                format!(
                    "independent_finalize_deserialize_artifact: artifact {:?} not in cache; deserializer must declare it as a dependency",
                    id,
                ),
            ),
        }
    }

    fn walk_body(
        xx: &ExprContext,
        loc: QExprId,
        body: ArtifactBody,
    ) -> EResult<Rc<dyn ExporterAny>> {
        let mut errors: Option<Vec<ExprErr>> = None;
        let exporter_any = match body.exporter {
            ArtifactBodyExporter::Body(inner) => {
                let res = walk_body(xx, loc, *inner);
                if let Some(errors1) = res.errors {
                    errors.get_or_insert_default().extend(errors1);
                }
                let Some(any) = res.v else {
                    return EResult {
                        errors,
                        v: None,
                    };
                };
                any
            },
            ArtifactBodyExporter::SourceRef(id) => {
                let res = lookup_ref(xx, loc, id);
                if let Some(errors1) = res.errors {
                    errors.get_or_insert_default().extend(errors1);
                }
                let Some(any) = res.v else {
                    return EResult {
                        errors,
                        v: None,
                    };
                };
                any
            },
            ArtifactBodyExporter::Root => {
                let res = TypeRoot::Root.exporter().from_serial(xx, loc, None, body.data);
                if let Some(errors1) = res.errors {
                    errors.get_or_insert_default().extend(errors1);
                }
                return EResult {
                    errors,
                    v: res.v,
                };
            },
        };
        let exporter = match exporter_any.downcast::<ArtifactDeserializerFromAny>() {
            Ok(v) => v,
            Err(_) => {
                errors.get_or_insert_default().push(ExprErr {
                    errors: vec![
                        ExprSubErr::Opaque(
                            "independent_finalize_deserialize_artifact: exporter reference is not a deserializer".to_string(),
                        ),
                    ],
                    loc,
                });
                return EResult {
                    errors,
                    v: None,
                };
            },
        };
        let res = exporter.v.from_serial(xx, loc, None, body.data);
        if let Some(errors1) = res.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        return EResult {
            errors,
            v: res.v,
        };
    }

    match artifact {
        Artifact::Body(body) => walk_body(xx, loc, body),
        Artifact::Ref(id) => lookup_ref(xx, loc, id),
    }
}

pub(crate) fn impl_deserialize_independent_artifact<
    T: DeserializeOwned,
>(
    xx: &ExprContext,
    loc: QExprId,
    id: &ArtifactIdSource,
    data: serde_json::Value,
    create: impl FnOnce(T) -> Result<(Rc<dyn ExporterAny>, Vec<Artifact>, ArtifactFinalizer), String>,
) -> EResult<Rc<dyn ExporterAny>> {
    let data = match serde_json::from_value(data) {
        Ok(v) => v,
        Err(e) => return EResult::err(loc, e.to_string()),
    };
    let (out, dependencies, finalizer) = match create(data) {
        Ok(v) => v,
        Err(e) => return EResult::err(loc, e),
    };
    xx.vx.ix.deserialized_artifacts.borrow_mut().insert(id.clone(), out.clone());
    let mut errors: Option<Vec<ExprErr>> = None;
    for dep in dependencies {
        let res = deserialize_artifact(xx, loc, dep);
        if let Some(errors1) = res.errors {
            errors.get_or_insert_default().extend(errors1);
        }
    }
    finalizer(xx, loc);
    return EResult {
        errors,
        v: Some(out),
    };
}

pub(crate) fn impl_serialize_independent_artifact(
    artifacts: &dyn Artifacts,
    cell: &std::cell::RefCell<Option<ArtifactIdSource>>,
    produce: impl FnOnce() -> ArtifactBody,
) -> Artifact {
    if let Some(existing_id) = cell.borrow().clone() {
        return Artifact::Ref(existing_id);
    }
    let id = artifacts.reserve_id();
    *cell.borrow_mut() = Some(id.clone());
    let artifact = produce();
    artifacts.set(id.clone(), artifact);
    return Artifact::Ref(id);
}

pub(crate) fn serialize_artifact(xx: &ExprContext, loc: QExprId, v: &dyn Value) -> EResult<Artifact> {
    let mut t = v.type_(xx, loc);
    let mut errors = None;
    t.push_errors(&mut errors);
    let Some(t) = t.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    let asp_art = aspect_artifact_get(xx, loc, t.as_ref());
    if let Some(errors1) = asp_art.errors {
        errors.get_or_insert_default().extend(errors1);
    }
    let Some(asp_art) = asp_art.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    let exporter: Rc<dyn ArtifactExporter> = asp_art.exporter();
    let mut art = exporter.to_serial(xx, loc, v.as_exporter_any());
    art.push_errors(&mut errors);
    let Some(art) = art.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    return EResult {
        errors,
        v: Some(art),
    };
}

pub(crate) struct TypeFromAny(pub Rc<dyn Type>);

impl ExporterAny for TypeFromAny {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(self.0.clone());
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

pub(crate) struct ValueFromAny(pub Rc<dyn Value>);

impl ExporterAny for ValueFromAny {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(self.0.clone());
    }
}

pub(crate) struct ExporterDataAny<T: 'static>(pub T);

impl<T: 'static> ExporterAny for ExporterDataAny<T> {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}
