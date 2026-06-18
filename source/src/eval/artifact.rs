use {
    crate::{
        eval::util::er_seq_cv_inline,
        invariant::must_downcast_ref,
        langhelp::{
            Code,
            Code_,
            CompileId,
            CompileImportContext,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            ExprSubErr,
            QExprId,
            Type,
        },
    },
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

pub(crate) trait ArtifactDeserializer {
    fn from_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        id: ArtifactIdSource,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>>;
}

pub(crate) trait Artifacts: Send + Sync {
    fn get(&self, id: ArtifactIdSource) -> Result<Artifact, String>;
    fn reserve_id(&self) -> ArtifactIdSource;
    fn set(&self, id: ArtifactIdSource, data: Artifact);
}

#[derive(Serialize, Deserialize, Clone)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct Artifact {
    pub(crate) data: serde_json::Value,
    pub(crate) deserializer: ArtifactId,
}

struct ArtifactDeserializerFromAny {
    v: Rc<dyn ArtifactDeserializer>,
}

pub(crate) type ArtifactFinalizer = Box<dyn FnOnce(&ExprContext, QExprId) -> ()>;

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) enum ArtifactId {
    Builtin(ArtifactIdBuiltin),
    Source(ArtifactIdSource),
}

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(transparent)]
pub(crate) struct ArtifactIdBuiltin(pub usize);

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactIdSource(pub CompileId, pub usize);

pub(crate) fn deserialize_artifact(xx: &ExprContext, loc: QExprId, id: &ArtifactId) -> EResult<Rc<dyn Any>> {
    match id {
        ArtifactId::Builtin(id) => {
            let Some(out) = xx.vx.ix.builtin_artifacts.get(id) else {
                return EResult::err(loc, format!("Unknown builtin artifact ID: {:?}", id));
            };
            return EResult {
                errors: None,
                v: Some(out.clone()),
            };
        },
        ArtifactId::Source(id) => {
            if let Some(out) = xx.vx.ix.deserialized_artifacts.borrow().get(id) {
                return EResult {
                    errors: None,
                    v: Some(out.clone()),
                };
            }
            let artifact = match xx.vx.ix.gx.artifacts.get(*id) {
                Ok(v) => v,
                Err(e) => return EResult::err(loc, e),
            };
            let deserializer = deserialize_artifact(xx, loc, &artifact.deserializer);
            let mut errors = deserializer.errors;
            let Some(deserializer) = deserializer.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            let deserializer = match deserializer.downcast::<ArtifactDeserializerFromAny>() {
                Ok(v) => v,
                Err(e) => {
                    errors.get_or_insert_default().push(ExprErr {
                        errors: vec![
                            ExprSubErr::Opaque("Artifact deserializer reference is not a deserializer".to_string()),
                        ],
                        loc: loc,
                    });
                    return EResult {
                        errors,
                        v: None,
                    };
                },
            };
            let out = deserializer.v.from_serial(xx, loc, *id, artifact.data);
            if let Some(errors1) = out.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            return EResult {
                errors,
                v: out.v,
            };
        },
    }
}

pub(crate) fn deserialize_type_artifact(xx: &ExprContext, loc: QExprId, id: ArtifactId) -> EResult<Rc<dyn Type>> {
    let res = deserialize_artifact(xx, loc, &id);
    let mut errors = res.errors;
    let v = match res.v {
        Some(v) => {
            let Ok(v) = v.downcast::<TypeFromAny>() else {
                errors.get_or_insert_default().push(ExprErr {
                    errors: vec![ExprSubErr::Opaque("Artifact didn't deserialize to type".to_string())],
                    loc: loc,
                });
                return EResult {
                    errors,
                    v: None,
                };
            };
            Some(v.v.clone())
        },
        None => None,
    };
    return EResult {
        errors: res.errors,
        v: v,
    };
}

pub(crate) fn impl_deserialize_independent_artifact<
    T: DeserializeOwned,
>(
    xx: &ExprContext,
    loc: QExprId,
    id: &ArtifactIdSource,
    data: serde_json::Value,
    create: impl FnOnce(T) -> Result<(Rc<dyn Any>, Vec<ArtifactId>, ArtifactFinalizer), String>,
) -> EResult<Rc<dyn Any>> {
    let data = serde_json::from_value(data).map_err(|e| e.to_string())?;
    let (out, dependencies, finalizer) = create(data)?;
    xx.vx.ix.deserialized_artifacts.borrow_mut().insert(id.clone(), Rc::new(out.clone()));
    let mut errors = None;
    for dep in dependencies {
        let res = deserialize_artifact(xx, loc, &dep);
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

pub(crate) fn impl_produce_independent_artifact(
    artifacts: &dyn Artifacts,
    cell: &std::cell::RefCell<Option<ArtifactIdSource>>,
    produce: impl FnOnce() -> Artifact,
) -> ArtifactIdSource {
    if let Some(existing_id) = cell.borrow().clone() {
        return existing_id;
    }
    let id = artifacts.reserve_id();
    *cell.borrow_mut() = Some(id.clone());
    let artifact = produce();
    artifacts.set(id.clone(), artifact);
    id
}

struct TypeFromAny {
    v: Rc<dyn Type>,
}
