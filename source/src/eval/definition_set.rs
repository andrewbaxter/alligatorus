use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactBodyExporter,
            ArtifactFinalizer,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            import_artifact,
            import_type_artifact,
            impl_import_independent_artifact,
            impl_export_independent_artifact,
        },
        eval::{
            StageLiftCell,
            expr::{
                Expr,
                StagedExprCell,
            },
            expr_artifact::{
                expr_from_artifact,
                expr_to_artifact,
            },
            from_v8_ref,
            value::{
                type_any::r#const::ValueAnyConst,
                type_source_data_type::{
                    SourceDataTypeData,
                    artifact::{
                        ArtifactDataDefSet,
                        ArtifactExternal,
                    },
                },
                type_source_exports::r#const::TypeSourceExportsLiftedConst,
                type_source_function::SourceFunctionData,
                type_source_interface::SourceInterfaceData,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Counter,
            DefSetId,
            ExprContext,
            External,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectLiftDynamic,
            DefinitionSetBuilder,
            aspect_lift_dynamic_aspect_id,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::{
            Cell,
            RefCell,
        },
        rc::Rc,
    },
};

pub(crate) fn definition_set_extra_aspect(_xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
    if *key == aspect_lift_dynamic_aspect_id() {
        return Some(Box::new(Box::new(TypeDefinitionSetLiftDynamic) as Box<dyn AspectLiftDynamic>));
    }
    None
}

pub(crate) fn definition_set_from_artifact_data(
    xx: &ExprContext,
    loc: QExprId,
    id: Option<&ArtifactIdSource>,
    data: serde_json::Value,
) -> crate::langhelp::EResult<Rc<dyn crate::artifact::ExporterAny>> {
    let id = match id {
        Some(id) => id,
        None => return crate::langhelp::EResult::err(
            loc,
            "DefinitionSet requires an artifact id to deserialize".to_string(),
        ),
    };
    let id_owned = id.clone();
    let res = impl_import_independent_artifact::<ArtifactDataDefSet>(xx, loc, id, data, move |body| {
        let set = Gc::new(DefinitionSet::new(body.def_set_id));
        *set.artifact_id.borrow_mut() = Some(id_owned.clone());
        let mut deps: Vec<Artifact> = Vec::new();
        deps.extend(body.data_types.iter().cloned());
        deps.extend(body.functions.iter().cloned());
        deps.extend(body.interfaces.iter().cloned());
        deps.extend(body.dependencies.iter().cloned());
        let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(set.clone()));
        let set_for_finalizer = set.clone();
        let body_for_finalizer = body;
        let finalizer: ArtifactFinalizer = Box::new(move |xx: &ExprContext, loc: QExprId| {
            let set = set_for_finalizer.clone();
            let body = body_for_finalizer;
            let mut dt_gcs: Vec<Gc<SourceDataTypeData>> = Vec::new();
            for dt_art in body.data_types.iter() {
                let any =
                    crate::artifact::independent_finalize_import_artifact(xx, loc, dt_art.clone())
                        .v
                        .expect("def-set data_type member missing from cache");
                let rc_any: Rc<dyn Any> = any;
                let wrapped =
                    rc_any
                        .downcast::<ExporterDataAny<Gc<SourceDataTypeData>>>()
                        .ok()
                        .expect("def-set data_type artifact had wrong type");
                dt_gcs.push(wrapped.0.clone());
            }
            *set.data_types.borrow_mut() = dt_gcs;
            let mut func_gcs: Vec<Gc<SourceFunctionData>> = Vec::new();
            for f_art in body.functions.iter() {
                let any =
                    crate::artifact::independent_finalize_import_artifact(xx, loc, f_art.clone())
                        .v
                        .expect("def-set function member missing from cache");
                let rc_any: Rc<dyn Any> = any;
                let wrapped =
                    rc_any
                        .downcast::<ExporterDataAny<Gc<SourceFunctionData>>>()
                        .ok()
                        .expect("def-set function artifact had wrong type");
                func_gcs.push(wrapped.0.clone());
            }
            *set.functions.borrow_mut() = func_gcs;
            let mut iface_gcs: Vec<Gc<SourceInterfaceData>> = Vec::new();
            for i_art in body.interfaces.iter() {
                let any =
                    crate::artifact::independent_finalize_import_artifact(xx, loc, i_art.clone())
                        .v
                        .expect("def-set interface member missing from cache");
                let rc_any: Rc<dyn Any> = any;
                let wrapped =
                    rc_any
                        .downcast::<ExporterDataAny<Gc<SourceInterfaceData>>>()
                        .ok()
                        .expect("def-set interface artifact had wrong type");
                iface_gcs.push(wrapped.0.clone());
            }
            *set.interfaces.borrow_mut() = iface_gcs;
            for dep in body.dependencies {
                let any =
                    crate::artifact::independent_finalize_import_artifact(xx, loc, dep)
                        .v
                        .expect("def-set dependency missing from cache");
                let rc_any: Rc<dyn Any> = any;
                let dep_set =
                    rc_any
                        .downcast::<ExporterDataAny<Gc<DefinitionSet>>>()
                        .ok()
                        .expect("def-set dependency artifact had wrong type")
                        .0
                        .clone();
                set.dependencies.borrow_mut().entry(dep_set.def_set_id).or_insert_with(|| dep_set.clone());
            }
            set.finished.set(true);
            let _ = (xx, loc);
        });
        Ok((out, deps, finalizer))
    });
    let errors = res.errors;
    let Some(any) = res.v else {
        return crate::langhelp::EResult {
            errors,
            v: None,
        };
    };
    let rc: Rc<dyn crate::artifact::ExporterAny> = any;
    crate::langhelp::EResult {
        errors,
        v: Some(rc),
    }
}

pub(crate) fn definition_set_export(
    xx: &ExprContext,
    loc: QExprId,
    self_: Rc<dyn crate::artifact::ExporterAny>,
    _make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
) -> crate::langhelp::EResult<Artifact> {
    let wrapped =
        self_
            .downcast::<ExporterDataAny<Gc<DefinitionSet>>>()
            .expect("DefinitionSet self_export: expected DefinitionSet");
    crate::langhelp::EResult::ok(export_definition_set(xx, loc, &wrapped.0))
}

pub(crate) struct DefinitionSet {
    pub artifact_id: RefCell<Option<ArtifactIdSource>>,
    pub data_types: RefCell<Vec<Gc<SourceDataTypeData>>>,
    pub def_set_id: DefSetId,
    pub dependencies: RefCell<std::collections::HashMap<DefSetId, Gc<DefinitionSet>>>,
    pub finished: Cell<bool>,
    pub functions: RefCell<Vec<Gc<SourceFunctionData>>>,
    pub interfaces: RefCell<Vec<Gc<SourceInterfaceData>>>,
    pub next_entry_id: Counter,
    pub next_field_id: Counter,
    pub next_interface_method_id: Counter,
}

impl DefinitionSet {
    pub fn check_mutable(&self, loc: crate::langhelp::QExprId) -> crate::langhelp::EResult<()> {
        if self.finished.get() {
            return crate::langhelp::EResult::err(
                loc,
                format!("definition set {:?} has been finalized; mutation not allowed", self.def_set_id,),
            );
        }
        crate::langhelp::EResult::ok(())
    }
}

impl DefinitionSet {
    pub fn finish(&self, xx: &ExprContext) -> Result<(), Vec<crate::langhelp::ExprErr>> {
        if self.finished.get() {
            return Ok(());
        }
        let inner_ex = Rc::new(crate::langhelp::EvalContext {
            ix: xx.vx.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
            v8_scope: Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
        });
        let inner_xx = Rc::new(ExprContext {
            vx: inner_ex.clone(),
            import_state: xx.import_state.clone(),
            scope: xx.scope.clone(),
            target: xx.target.clone(),
        });
        let root_loc = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        for func_data in self.functions.borrow().iter() {
            func_data.finish(&inner_xx, root_loc)?;
        }
        for dt_data in self.data_types.borrow().iter() {
            crate::eval::value::type_source_data_type::SourceDataTypeData::finish(dt_data, &inner_xx, root_loc)?;
        }
        for iface in self.interfaces.borrow().iter() {
            iface.finish().map_err(|e| vec![crate::langhelp::ExprErr {
                loc: root_loc,
                errors: vec![crate::langhelp::ExprSubErr::Opaque(e)],
            }])?;
        }
        let dep_list: Vec<Gc<DefinitionSet>> = {
            let registered = inner_ex.definition_sets.borrow();
            let mut deps = self.dependencies.borrow_mut();
            for (id, ds) in registered.iter() {
                if *id == self.def_set_id {
                    continue;
                }
                deps.entry(*id).or_insert_with(|| ds.clone());
            }
            deps.values().cloned().collect()
        };
        self.finished.set(true);
        for dep in dep_list {
            dep.finish(xx)?;
        }
        Ok(())
    }

    pub fn new(def_set_id: DefSetId) -> Self {
        DefinitionSet {
            artifact_id: RefCell::new(None),
            def_set_id: def_set_id,
            functions: RefCell::new(Vec::new()),
            data_types: RefCell::new(Vec::new()),
            interfaces: RefCell::new(Vec::new()),
            finished: Cell::new(false),
            next_field_id: Counter::default(),
            next_entry_id: Counter::default(),
            next_interface_method_id: Counter::default(),
            dependencies: RefCell::new(std::collections::HashMap::new()),
        }
    }
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for DefinitionSet {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        let functions = self.functions.try_borrow().map_err(|_| ())?;
        for f in functions.iter() {
            visitor.visit_unsync(f);
        }
        drop(functions);
        let data_types = self.data_types.try_borrow().map_err(|_| ())?;
        for dt in data_types.iter() {
            visitor.visit_unsync(dt);
        }
        drop(data_types);
        let interfaces = self.interfaces.try_borrow().map_err(|_| ())?;
        for i in interfaces.iter() {
            visitor.visit_unsync(i);
        }
        Ok(())
    }
}

pub(crate) fn deexport_captured_externals(
    xx: &ExprContext,
    loc: QExprId,
    serial: &[ArtifactExternal],
) -> Vec<(ScopeEntryId, External)> {
    serial.iter().map(|ext| {
        match ext {
            ArtifactExternal::StagedExpr { local_id, expr } => {
                let expr =
                    expr_from_artifact(xx, expr).expect("Failed to deserialize captured StagedExprCell expr");
                let cell = Rc::new(StagedExprCell { value: RefCell::new(Some(Rc::new(expr) as Rc<dyn Value>)) });
                (ScopeEntryId::Local(*local_id), External::StagedExprCell(cell))
            },
            ArtifactExternal::StageLift { local_id, source_type } => {
                let source_type =
                    import_type_artifact(xx, loc, source_type.clone())
                        .into_res()
                        .expect("Failed to deserialize captured StageLiftCell source type");
                let cell = Rc::new(StageLiftCell {
                    source_type,
                    loc,
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: xx.scope.clone(),
                    target: xx.target.clone(),
                });
                (ScopeEntryId::Local(*local_id), External::StageLiftCell(cell))
            },
            ArtifactExternal::Type { local_id, type_artifact } => {
                let type_ =
                    import_type_artifact(xx, loc, type_artifact.clone())
                        .into_res()
                        .expect("Failed to deserialize captured External::Type");
                (ScopeEntryId::Local(*local_id), External::Type(type_))
            },
            ArtifactExternal::Unique { local_id, unique } => (
                ScopeEntryId::Local(*local_id),
                External::Unique(*unique),
            ),
        }
    }).collect()
}

pub(crate) fn load_def_set(xx: &ExprContext, def_set_artifact: Artifact) -> Result<Gc<DefinitionSet>, String> {
    let any =
        import_artifact(xx, (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)), def_set_artifact);
    if let Some(errors) = any.errors {
        return Err(format!("Failed to deserialize def set: {:?}", errors));
    }
    let any = any.v.ok_or_else(|| "Definition set deserialization yielded no value".to_string())?;
    let rc: Rc<dyn Any> = any;
    let wrapped =
        rc
            .downcast::<ExporterDataAny<Gc<DefinitionSet>>>()
            .map_err(|_| "Expected DefinitionSet artifact".to_string())?;
    Ok(wrapped.0.clone())
}

pub(crate) fn next_interface_method_id(set: &DefinitionSet) -> usize {
    set.next_interface_method_id.next()
}

pub(crate) fn export_captured_externals(
    xx: &ExprContext,
    loc: QExprId,
    externals: &[(ScopeEntryId, External)],
) -> Vec<ArtifactExternal> {
    externals.iter().map(|(id, ext)| {
        let local_id = match id {
            ScopeEntryId::Local(n) => *n,
            other => panic!("captured external has non-Local scope entry id: {:?}", other),
        };
        match ext {
            External::StagedExprCell(cell) => {
                let value = cell.value.borrow().clone().expect("captured StagedExprCell already consumed");
                let expr_ref = must_downcast_ref::<Expr>(value.as_ref());
                let expr =
                    expr_to_artifact(xx, expr_ref).expect("Failed to serialize captured StagedExprCell value");
                ArtifactExternal::StagedExpr {
                    local_id,
                    expr,
                }
            },
            External::StageLiftCell(cell) => {
                let value = cell.source_type.as_value();
                let source_type =
                    crate::artifact::export_artifact(xx, loc, value.as_ref())
                        .into_res()
                        .expect("Failed to serialize captured StageLiftCell source type");
                ArtifactExternal::StageLift {
                    local_id,
                    source_type,
                }
            },
            External::Type(type_) => {
                let value = type_.as_value();
                let type_artifact =
                    crate::artifact::export_artifact(xx, loc, value.as_ref())
                        .into_res()
                        .expect("Failed to serialize captured External::Type");
                ArtifactExternal::Type {
                    local_id,
                    type_artifact,
                }
            },
            External::Unique(u) => ArtifactExternal::Unique {
                local_id,
                unique: *u,
            },
            External::CodeValueExternCell(_) => {
                panic!("External::CodeValueExternCell is runtime-only and must not be serialized")
            },
        }
    }).collect()
}

// # ==================================================================== Definition set serialization delegates to member self_export impls
pub(crate) fn export_definition_set(xx: &ExprContext, loc: QExprId, set: &Gc<DefinitionSet>) -> Artifact {
    impl_export_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &set.artifact_id, || {
        use crate::artifact::export_artifact;

        let data_types: Vec<Artifact> = set.data_types.borrow().iter().filter_map(|dt| {
            let dt_const =
                crate::eval::value::type_source_data_type::r#const::TypeSourceDataTypeConst { data: dt.clone() };
            export_artifact(xx, loc, &dt_const as &dyn Value).v
        }).collect();
        let functions: Vec<Artifact> = set.functions.borrow().iter().filter_map(|f| {
            let f_const =
                crate::eval::value::type_source_function::r#const::TypeSourceFunctionConst { data: f.clone() };
            export_artifact(xx, loc, &f_const as &dyn Value).v
        }).collect();
        let interfaces: Vec<Artifact> = set.interfaces.borrow().iter().filter_map(|i| {
            let i_const =
                crate::eval::value::type_source_interface::r#const::TypeSourceInterfaceConst { data: i.clone() };
            export_artifact(xx, loc, &i_const as &dyn Value).v
        }).collect();
        let dependencies: Vec<Artifact> = {
            let deps = set.dependencies.borrow();
            deps.values().map(|s| export_definition_set(xx, loc, s)).collect()
        };
        let body = ArtifactDataDefSet {
            def_set_id: set.def_set_id,
            data_types,
            functions,
            interfaces,
            dependencies,
        };
        ArtifactBody {
            data: serde_json::to_value(&body).expect("Failed to serialize definition set"),
            exporter: ArtifactBodyExporter::Body(Box::new(ArtifactBody {
                data: serde_json::to_value(
                    crate::artifact::ExporterRoot::DefinitionSet,
                ).unwrap(),
                exporter: ArtifactBodyExporter::Root,
            })),
        }
    })
}
struct TypeDefinitionSetLiftDynamic;

impl Aspect for TypeDefinitionSetLiftDynamic { }

impl AspectLiftDynamic for TypeDefinitionSetLiftDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let obj =
            v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for definition set lift")?;
        let dk = v8::String::new(scope, "_d").unwrap();
        let ext = obj.get(scope, dk.into()).ok_or("Missing _d on definition set object")?;
        let ds: Gc<DefinitionSet> = from_v8_ref::<Rc<DefinitionSetBuilder>>(&*ext).inner.clone();
        xx.vx.definition_sets.borrow_mut().entry(ds.def_set_id).or_insert_with(|| ds.clone());
        let exports_type: Rc<dyn Type> = Rc::new(TypeSourceExportsLiftedConst { set: ds });
        Ok(Rc::new(ValueAnyConst { inner: exports_type }))
    }
}

impl crate::artifact::ExporterAny for crate::artifact::ExporterDataAny<Gc<DefinitionSet>> {
    fn as_type(&self) -> crate::langhelp::EResult<std::rc::Rc<dyn Type>> {
        unimplemented!("ExporterDataAny<Gc<DefinitionSet>>::as_type not meaningful")
    }

    fn as_value(&self) -> crate::langhelp::EResult<std::rc::Rc<dyn Value>> {
        unimplemented!("ExporterDataAny<Gc<DefinitionSet>>::as_value not meaningful")
    }
}
