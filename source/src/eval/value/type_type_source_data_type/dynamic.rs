use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectType,
            aspect_artifact_aspect_id,
            aspect_type_aspect_id,
        },
        eval::{
            Erb,
            definition_set::DefinitionSet,
            value::type_source_data_type::{
                SourceDataMethodData,
                SourceDataTypeData,
                TypeSourceDataTypeConst,
                artifact::{
                    ArtifactDataDefSet,
                    ArtifactDataDefSetDataType,
                },
            },
        },
        invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
        langhelp::{
            Artifact,
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
            deserialize_independent_artifact,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        mem,
        rc::Rc,
    },
};

generate_builtin_artifact!(TypeTypeSourceDataTypeDynamic, serde_json::Value);
pub(crate) struct TypeTypeSourceDataTypeDynamic;

impl Aspect for TypeTypeSourceDataTypeDynamic { }

impl AspectArtifact for TypeTypeSourceDataTypeDynamic {
    fn from_serial(&self, xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        er_seq_cv!(loc, erb, {
            let serial: ArtifactDataDefSetDataType =
                erb.must_res(
                    serde_json::from_value(
                        data.clone()
                    ).map_err(|e| format!("Failed to parse defset data type: {}", e)),
                )?;
            let def_set = erb.must_res(deserialize_independent_artifact(
                &xx.vx.ix,
                &serial.def_set,
                || {
                    let def_set_artifact = xx.vx.ix.gx.artifacts.get(serial.def_set.clone())?;
                    let def_set_serial: ArtifactDataDefSet =
                        serde_json::from_value(def_set_artifact.data)
                            .map_err(|e| format!("Failed to parse definition set: {}", e))?;
                    let set = Gc::new(
                        DefinitionSet::new(def_set_serial.import_id, def_set_serial.def_set_id),
                    );
                    let mut dt_serials: Vec<(
                        crate::langhelp::ArtifactId,
                        ArtifactDataDefSetDataType,
                    )> = Vec::new();
                    let mut dt_gcs: Vec<Gc<SourceDataTypeData>> = Vec::new();
                    for dt_artifact_id in &def_set_serial.data_types {
                        let dt_artifact = xx.vx.ix.gx.artifacts.get(dt_artifact_id.clone())?;
                        let dt_serial: ArtifactDataDefSetDataType =
                            serde_json::from_value(dt_artifact.data)
                                .map_err(|e| format!("Failed to parse data type: {}", e))?;
                        let dt_data = Gc::new(SourceDataTypeData {
                            set: set.clone(),
                            name: dt_serial.name.clone(),
                            identity: dt_serial.identity.clone(),
                            data_field: dt_serial.data_field.clone(),
                            description: RefCell::new(None),
                            underlying_type: DeserialLazy::new(),
                            methods: RefCell::new(Vec::new()),
                            interface_impls: RefCell::new(Vec::new()),
                            artifact_id: RefCell::new(None),
                        });
                        xx.vx.ix.deserialized_artifacts.borrow_mut().insert(
                            dt_artifact_id.clone(),
                            Box::new(dt_data.clone()),
                        );
                        dt_gcs.push(dt_data);
                        dt_serials.push((dt_artifact_id.clone(), dt_serial));
                    }
                    *set.data_types.borrow_mut() = dt_gcs;
                    let finalizer: crate::langhelp::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            let mut codes: Vec<Code> = Vec::new();
                            for (dt_artifact_id, dt_serial) in &dt_serials {
                                let dt_data = {
                                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                                    must_downcast_ref::<Gc<SourceDataTypeData>>(
                                        cache.get(dt_artifact_id).unwrap().as_ref(),
                                    )
                                    .clone()
                                };
                                let (underlying_type, underlying_code) =
                                    crate::eval::util::deserialize_type_artifact(
                                        xx,
                                        loc,
                                        &dt_serial.underlying_type.type_,
                                    )
                                    .into_res()
                                    .map_err(|e| {
                                        format!("Failed to deserialize underlying type: {:?}", e)
                                    })?;
                                codes.push(underlying_code);
                                dt_data.underlying_type.set(underlying_type);
                                let methods: Vec<SourceDataMethodData> =
                                    dt_serial
                                        .methods
                                        .iter()
                                        .enumerate()
                                        .map(|(i, m)| {
                                            let (ret_type, ret_code) =
                                                crate::eval::util::deserialize_type_artifact(
                                                    xx,
                                                    loc,
                                                    &m.ret_type.type_,
                                                )
                                                .into_res()
                                                .unwrap_or_else(|e| {
                                                    panic!(
                                                        "Failed to deserialize method ret type: {:?}",
                                                        e
                                                    )
                                                });
                                            codes.push(ret_code);
                                            SourceDataMethodData {
                                                name: m.name.clone(),
                                                impl_expr: RefCell::new(None),
                                                ret_type: RefCell::new(Some(ret_type)),
                                                scope_entry_id: Cell::new(
                                                    Some(ScopeEntryId::Def(
                                                        def_set_serial.import_id,
                                                        def_set_serial.def_set_id,
                                                        i,
                                                    )),
                                                ),
                                                self_scope_entry_id: Cell::new(None),
                                                field_name: m.field_name.clone(),
                                                compiled_js: RefCell::new(
                                                    Some(m.compiled_js.clone()),
                                                ),
                                            }
                                        })
                                        .collect();
                                *dt_data.methods.borrow_mut() = methods;
                            }
                            Ok(codes)
                        });
                    Ok((set, finalizer))
                },
            ))?;
            let data_types = def_set.data_types.borrow();
            let dt_data =
                data_types
                    .iter()
                    .find(|dt| dt.identity == serial.identity)
                    .expect("Data type not found in definition set")
                    .clone();
            drop(data_types);
            return Ok(CodeValue {
                code: Code {
                    loc: loc,
                    code: Code_::None,
                },
                value: Rc::new(TypeSourceDataTypeConst { data: dt_data }),
            });
        })
    }

    fn to_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let dt = must_downcast_ref::<TypeSourceDataTypeConst>(self_.as_ref());
        let artifact_id = dt.serialize_data_type(xx, loc);
        EResult::ok(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }
}

impl AspectType for TypeTypeSourceDataTypeDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let dt = must_downcast_ref::<TypeSourceDataTypeConst>(value);
        Rc::new(TypeSourceDataTypeConst { data: dt.data.clone() })
    }
}

impl Type for TypeTypeSourceDataTypeDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceDataTypeDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == crate::eval::value::type_any::r#const::type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeDynamic) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeDynamic) as Box<dyn AspectType>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_type".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_data_type_type".to_string())
    }

    fn desc(&self) -> String {
        "definition_set_data_type_type".to_string()
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Option<Artifact> {
        Some(Self::build_type_artifact())
    }
}

impl Value for TypeTypeSourceDataTypeDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
