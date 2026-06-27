use {
    crate::{
        eval::value::{
            type_bool::{
                dynamic::TypeBoolDynamic,
                r#const::TypeBoolConst,
            },
            type_expr::TypeExpr,
            type_list::dynamic::TypeListDynamic,
            type_map::dynamic::TypeMapDynamic,
            type_null::{
                dynamic::TypeNullDynamic,
                r#const::TypeNullConst,
            },
            type_number::{
                dynamic::TypeNumberDynamic,
                r#const::TypeNumberConst,
            },
            type_option::dynamic::TypeOptionDynamic,
            type_record::Record,
            type_root::r#const::{
                ExporterRootDelegate,
                TypeRoot,
            },
            type_source_exports::r#const::TypeSourceExportsLiftedConst,
            type_source_function::r#const::TypeSourceFunctionConst,
            type_source_interface::r#const::TypeSourceInterfaceConst,
            type_source_object::r#const::TypeSourceObjectConst,
            type_source_object_new::TypeSourceObjectNewConst,
            type_source_union::r#const::TypeSourceUnionConst,
            type_source_union_new::r#const::TypeSourceUnionNewConst,
            type_string::{
                dynamic::TypeStringDynamic,
                r#const::TypeStringConst,
            },
            type_tuple::Tuple,
            type_unique::r#const::TypeUnique,
            value_bool::r#const::ValueBoolConst,
            value_number::r#const::ValueNumberConst,
            value_source_object::r#const::ValueSourceObjectConst,
            value_string::r#const::ValueStringConst,
        },
        langhelp::{
            CompileId,
            EResult,
            ExprContext,
            ExprErr,
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
    fn export(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn ExporterAny>) -> EResult<Artifact>;
    fn import(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        id: Option<ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>>;
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

pub(crate) type ArtifactFinalizer = Box<dyn FnOnce(&ExprContext, QExprId) -> ()>;

#[derive(Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord, Clone, Copy, Hash, Debug)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub(crate) struct ArtifactIdSource(pub CompileId, pub usize);

pub(crate) fn export_artifact(xx: &ExprContext, loc: QExprId, v: &dyn Value) -> EResult<Artifact> {
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
    let mut art = exporter.export(xx, loc, v.as_exporter_any());
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

fn exporter_any_to_artifact_exporter(
    xx: &ExprContext,
    loc: QExprId,
    exporter_any: Rc<dyn ExporterAny>,
) -> EResult<Rc<dyn ArtifactExporter>> {
    if let Ok(er) = Rc::clone(&exporter_any).downcast::<ExporterRoot>() {
        return EResult::ok(er as Rc<dyn ArtifactExporter>);
    }
    let type_res = exporter_any.as_type();
    let mut errors = type_res.errors;
    let Some(type_) = type_res.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    let asp_res = aspect_artifact_get(xx, loc, type_.as_ref());
    if let Some(errors1) = asp_res.errors {
        errors.get_or_insert_default().extend(errors1);
    }
    let Some(asp) = asp_res.v else {
        return EResult {
            errors,
            v: None,
        };
    };
    return EResult {
        errors,
        v: Some(asp.exporter()),
    };
}

pub(crate) struct ExporterDataAny<T: 'static>(pub T);

#[derive(Clone, Copy, Serialize, Deserialize)]
#[serde(rename_all = "snake_case", deny_unknown_fields)]
pub enum ExporterRoot {
    AnyDynamic,
    BoolConst,
    BoolDynamic,
    BoolValueConst,
    CodeValueExternDynamic,
    DefinitionSet,
    Expr,
    ListDynamic,
    MapDynamic,
    NullConst,
    NullDynamic,
    NumberConst,
    NumberDynamic,
    NumberValueConst,
    OptionDynamic,
    OptionVariantConstructorDynamic,
    Pull,
    Record,
    Root,
    SourceExportsLiftedConst,
    SourceFunctionConst,
    SourceInterfaceConst,
    SourceObjectConst,
    SourceObjectNewConst,
    SourceObjectValueConst,
    SourceUnionConst,
    SourceUnionNewConst,
    StringConst,
    StringDynamic,
    StringValueConst,
    Tuple,
    Unique,
}

impl ArtifactExporter for ExporterRoot {
    fn export(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn ExporterAny>) -> EResult<Artifact> {
        let me = *self;
        let make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody> = Box::new(move |data| ArtifactBody {
            data,
            exporter: match me {
                ExporterRoot::Root => ArtifactBodyExporter::Root,
                _ => ArtifactBodyExporter::Body(Box::new(ArtifactBody {
                    data: serde_json::to_value(me).unwrap(),
                    exporter: ArtifactBodyExporter::Root,
                })),
            },
        });
        let mut errors: Option<Vec<ExprErr>> = None;
        let res = match self {
            ExporterRoot::AnyDynamic => crate::eval::value::type_any::dynamic::TypeAnyDynamic::self_export(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::BoolConst => TypeBoolConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::BoolDynamic => TypeBoolDynamic::self_export(xx, loc, self_, make_body),
            ExporterRoot::BoolValueConst => ValueBoolConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::CodeValueExternDynamic => crate
            ::eval
            ::code_value_extern
            ::TypeCodeValueExternDynamic
            ::self_export(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::DefinitionSet => crate::eval::definition_set::definition_set_export(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::Expr => TypeExpr::self_export(xx, loc, self_, make_body),
            ExporterRoot::ListDynamic => TypeListDynamic::self_export(xx, loc, self_, make_body),
            ExporterRoot::MapDynamic => TypeMapDynamic::self_export(xx, loc, self_, make_body),
            ExporterRoot::NullConst => TypeNullConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::NullDynamic => TypeNullDynamic::self_export(xx, loc, self_, make_body),
            ExporterRoot::NumberConst => TypeNumberConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::NumberDynamic => TypeNumberDynamic::self_export(xx, loc, self_, make_body),
            ExporterRoot::NumberValueConst => ValueNumberConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::OptionDynamic => TypeOptionDynamic::self_export(xx, loc, self_, make_body),
            ExporterRoot::OptionVariantConstructorDynamic => crate
            ::eval
            ::value
            ::type_option
            ::dynamic
            ::TypeOptionVariantConstructorDynamic
            ::self_export(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::Pull => crate::eval::value::type_pull::r#const::TypePull::self_export(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::SourceObjectConst => TypeSourceObjectConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::SourceObjectValueConst => ValueSourceObjectConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::SourceObjectNewConst => TypeSourceObjectNewConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::SourceUnionConst => TypeSourceUnionConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::SourceUnionNewConst => TypeSourceUnionNewConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::SourceExportsLiftedConst => TypeSourceExportsLiftedConst::self_export(
                xx,
                loc,
                self_,
                make_body,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::StringDynamic => TypeStringDynamic::self_export(xx, loc, self_, make_body),
            ExporterRoot::StringValueConst => ValueStringConst::self_export(xx, loc, self_, make_body),
            ExporterRoot::Record => Record::self_export(xx, loc, self_, make_body),
            ExporterRoot::Tuple => Tuple::self_export(xx, loc, self_, make_body),
            ExporterRoot::Unique => TypeUnique::self_export(xx, loc, self_, make_body),
            ExporterRoot::Root => EResult::ok(Artifact::Body(make_body(serde_json::to_value(me).unwrap()))),
        };
        if let Some(e) = res.errors {
            errors.get_or_insert_default().extend(e);
        }
        return EResult {
            errors,
            v: res.v,
        };
    }

    fn import(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        id: Option<ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn ExporterAny>> {
        let id_ref = id.as_ref();
        let res: EResult<Rc<dyn ExporterAny>> = match self {
            ExporterRoot::AnyDynamic => crate::eval::value::type_any::dynamic::TypeAnyDynamic::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::BoolConst => TypeBoolConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::BoolDynamic => TypeBoolDynamic::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::BoolValueConst => ValueBoolConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::CodeValueExternDynamic => crate
            ::eval
            ::code_value_extern
            ::TypeCodeValueExternDynamic
            ::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::DefinitionSet => crate::eval::definition_set::definition_set_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::Expr => TypeExpr::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::ListDynamic => TypeListDynamic::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::MapDynamic => TypeMapDynamic::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::NullConst => TypeNullConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::NullDynamic => TypeNullDynamic::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::NumberConst => TypeNumberConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::NumberDynamic => TypeNumberDynamic::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::NumberValueConst => ValueNumberConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::OptionDynamic => TypeOptionDynamic::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::OptionVariantConstructorDynamic => crate
            ::eval
            ::value
            ::type_option
            ::dynamic
            ::TypeOptionVariantConstructorDynamic
            ::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::Pull => crate::eval::value::type_pull::r#const::TypePull::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceObjectConst => TypeSourceObjectConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::SourceObjectValueConst => ValueSourceObjectConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceObjectNewConst => TypeSourceObjectNewConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceExportsLiftedConst => TypeSourceExportsLiftedConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceFunctionConst => TypeSourceFunctionConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceInterfaceConst => TypeSourceInterfaceConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::SourceUnionConst => TypeSourceUnionConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::SourceUnionNewConst => TypeSourceUnionNewConst::self_from_artifact_data(
                &xx,
                loc,
                id_ref,
                data,
            ),
            ExporterRoot::StringConst => TypeStringConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::StringDynamic => TypeStringDynamic::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::StringValueConst => ValueStringConst::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Record => Record::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Tuple => Tuple::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Unique => TypeUnique::self_from_artifact_data(&xx, loc, id_ref, data),
            ExporterRoot::Root => EResult::from(
                loc,
                serde_json::from_value::<ExporterRoot>(data).map(|r| Rc::new(r) as Rc<dyn ExporterAny>),
            ),
        };
        let mut errors: Option<Vec<ExprErr>> = None;
        if let Some(errors1) = res.errors {
            errors.get_or_insert_default().extend(errors1);
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
}

impl ExporterAny for ExporterRoot {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        unimplemented!()
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

pub(crate) fn impl_export_independent_artifact(
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

pub(crate) fn impl_import_independent_artifact<
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
        let res = import_artifact(xx, loc, dep);
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

pub(crate) fn import_artifact(xx: &ExprContext, loc: QExprId, artifact: Artifact) -> EResult<Rc<dyn ExporterAny>> {
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

    fn import_artifact_body(
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
                        let any = import_artifact_body(xx, loc, None, *body);
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
                        let any = import_artifact_body(xx, loc, Some(id), body);
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
            let exporter_res = exporter_any_to_artifact_exporter(xx, loc, exporter_any);
            if let Some(errors1) = exporter_res.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            let Some(exporter) = exporter_res.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            break 'exporter exporter;
        };
        return exporter.import(xx, loc, id, body.data);
    }

    return import_artifact_body(xx, loc, id, body);
}

pub(crate) fn import_type_artifact(xx: &ExprContext, loc: QExprId, artifact: Artifact) -> EResult<Rc<dyn Type>> {
    let res = import_artifact(xx, loc, artifact);
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

pub(crate) fn independent_finalize_import_artifact(
    xx: &ExprContext,
    loc: QExprId,
    artifact: Artifact,
) -> EResult<Rc<dyn ExporterAny>> {
    fn lookup_ref(xx: &ExprContext, loc: QExprId, id: ArtifactIdSource) -> EResult<Rc<dyn ExporterAny>> {
        let cache = xx.vx.ix.deserialized_artifacts.borrow();
        match cache.get(&id) {
            Some(v) => EResult::ok(v.clone()),
            None => {
                let cached_ids: Vec<_> = cache.keys().cloned().collect();
                EResult::err(
                    loc,
                    format!(
                        "independent_finalize_import_artifact: artifact {:?} not in cache. Cached={:?}",
                        id,
                        cached_ids,
                    ),
                )
            },
        }
    }

    fn walk_body(xx: &ExprContext, loc: QExprId, body: ArtifactBody) -> EResult<Rc<dyn ExporterAny>> {
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
                let res = TypeRoot::Root.exporter().import(xx, loc, None, body.data);
                if let Some(errors1) = res.errors {
                    errors.get_or_insert_default().extend(errors1);
                }
                return EResult {
                    errors,
                    v: res.v,
                };
            },
        };
        let exporter_res = exporter_any_to_artifact_exporter(xx, loc, exporter_any);
        if let Some(errors1) = exporter_res.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(exporter) = exporter_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let res = exporter.import(xx, loc, None, body.data);
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

pub(crate) fn independent_finalize_import_type_artifact(
    xx: &ExprContext,
    loc: QExprId,
    artifact: Artifact,
) -> EResult<Rc<dyn Type>> {
    let res = independent_finalize_import_artifact(xx, loc, artifact);
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

impl dyn ExporterAny {
    pub(crate) fn downcast<T: ExporterAny>(self: Rc<Self>) -> Result<Rc<T>, Rc<dyn Any>> {
        let any: Rc<dyn Any> = self;
        any.downcast::<T>()
    }
}
