use {
    crate::{
        eval::{
            StageLiftCell,
            artifact::{
                Artifact,
                ArtifactFinalizer,
                ArtifactId,
                ArtifactIdBuiltin,
                deserialize_type_artifact,
                impl_deserialize_independent_artifact as deserialize_independent_artifact,
                impl_produce_independent_artifact as produce_independent_artifact,
            },
            expr::{
                Expr,
                StagedExprCell,
            },
            expr_artifact::{
                ArtifactExpr,
                ArtifactExpr_,
                ArtifactQExprId,
                expr_from_artifact,
                expr_to_artifact,
            },
            from_v8_ref,
            value::{
                type_any::r#const::ValueAnyConst,
                type_null::dynamic::TypeNullDynamic,
                type_source_data_type::{
                    SourceDataMethodData,
                    SourceDataTypeData,
                    SourceDataTypeInterfaceImplData,
                    SourceDataTypeInterfaceMethodImplData,
                    artifact::{
                        ArtifactDataDefSet,
                        ArtifactDataDefSetDataType,
                        ArtifactDataDefSetFunction,
                        ArtifactDataDefSetInterface,
                        ArtifactDataDefSetInterfaceImpl,
                        ArtifactDataDefSetInterfaceMethod,
                        ArtifactDataDefSetInterfaceMethodImpl,
                        ArtifactDataDefSetMethod,
                        ArtifactExternal,
                    },
                },
                type_source_data_type_constructor::dynamic::serialize_type_ref,
                type_source_exports::r#const::TypeSourceExportsLiftedConst,
                type_source_function::{
                    SourceFunctionData,
                    dynamic::TypeTypeSourceFunctionDynamic,
                },
                type_source_interface::{
                    BuiltinInterfaceKind,
                    SourceInterfaceData,
                    SourceInterfaceMethodData,
                },
                type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
            },
        },
        invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
        langhelp::{
            Aspect,
            Code,
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
            defset_artifact_id,
            gen_build_builtin_interfaces,
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

pub(crate) struct DefinitionSet {
    pub artifact_id: RefCell<Option<ArtifactId>>,
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

pub(crate) fn defset_artifact_identity() -> ArtifactIdBuiltin {
    defset_artifact_id()
}

pub(crate) fn deserialize_captured_externals(
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
                let (source_type, _code) =
                    deserialize_type_artifact(xx, loc, source_type)
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
                let (type_, _code) =
                    deserialize_type_artifact(xx, loc, type_artifact)
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

pub(crate) fn load_def_set(xx: &ExprContext, def_set_artifact_id: &ArtifactId) -> Result<Gc<DefinitionSet>, String> {
    deserialize_independent_artifact(&xx.vx.ix, def_set_artifact_id, || {
        let def_set_artifact = xx.vx.ix.gx.artifacts.get(def_set_artifact_id.clone())?;
        let def_set_serial: ArtifactDataDefSet =
            serde_json::from_value(
                def_set_artifact.data,
            ).map_err(|e| format!("Failed to parse definition set: {}", e))?;
        let def_set_id = def_set_serial.def_set_id;
        let set = Gc::new(DefinitionSet::new(def_set_id));
        let mut dt_serials: Vec<(ArtifactId, ArtifactDataDefSetDataType)> = Vec::new();
        let mut dt_gcs: Vec<Gc<SourceDataTypeData>> = Vec::new();
        let mut func_serials: Vec<(ArtifactId, ArtifactDataDefSetFunction)> = Vec::new();
        let mut func_gcs: Vec<Gc<SourceFunctionData>> = Vec::new();

        use crate::r#gen::{
            defset_method_artifact_id,
            defset_iface_impl_artifact_id,
            defset_iface_method_impl_artifact_id,
            defset_iface_artifact_id,
        };

        for member_artifact_id in &def_set_serial.members {
            let member_artifact = xx.vx.ix.gx.artifacts.get(member_artifact_id.clone())?;
            let type_id = match &member_artifact.deserializer {
                ArtifactId::Builtin(b) => b.clone(),
                _ => return Err(format!("def-set member artifact has non-builtin type_")),
            };
            if type_id == crate::r#gen::type_type_source_data_type_dynamic_artifact_id() {
                let dt_serial: ArtifactDataDefSetDataType =
                    serde_json::from_value(
                        member_artifact.data,
                    ).map_err(|e| format!("Failed to parse data type: {}", e))?;
                let dt_data = Gc::new(SourceDataTypeData {
                    set: set.clone(),
                    name: dt_serial.name.clone(),
                    identity: dt_serial.identity.clone(),
                    data_field: dt_serial.data_field.clone(),
                    description: RefCell::new(None),
                    underlying_type: DeserialLazy::new(),
                    methods: RefCell::new(Vec::new()),
                    interface_impls: RefCell::new(Vec::new()),
                    artifact_id: RefCell::new(Some(member_artifact_id.clone())),
                });
                xx
                    .vx
                    .ix
                    .deserialized_artifacts
                    .borrow_mut()
                    .insert(member_artifact_id.clone(), Box::new(dt_data.clone()));
                dt_gcs.push(dt_data);
                dt_serials.push((member_artifact_id.clone(), dt_serial));
            } else if type_id == crate::r#gen::type_type_source_function_dynamic_artifact_id() {
                let func_serial: ArtifactDataDefSetFunction =
                    serde_json::from_value(
                        member_artifact.data,
                    ).map_err(|e| format!("Failed to parse function: {}", e))?;
                let func_data = Gc::new(SourceFunctionData {
                    set: set.clone(),
                    name: func_serial.name.clone(),
                    description: RefCell::new(None),
                    impl_expr: RefCell::new(None),
                    arg_type: RefCell::new(None),
                    ret_type: RefCell::new(None),
                    scope_entry_id: Cell::new(Some(ScopeEntryId::Def(def_set_id, func_serial.entry_id))),
                    artifact_id: RefCell::new(Some(member_artifact_id.clone())),
                    compiled_js: RefCell::new(Some(func_serial.compiled_js.clone())),
                    captured_externals: RefCell::new(Vec::new()),
                });
                xx
                    .vx
                    .ix
                    .deserialized_artifacts
                    .borrow_mut()
                    .insert(member_artifact_id.clone(), Box::new(func_data.clone()));
                func_gcs.push(func_data);
                func_serials.push((member_artifact_id.clone(), func_serial));
            } else {
                return Err(format!("Unknown def-set member artifact type_id: {:?}", type_id));
            }
        }
        *set.data_types.borrow_mut() = dt_gcs;
        *set.functions.borrow_mut() = func_gcs;
        let set_for_finalizer = set.clone();
        let finalizer: ArtifactFinalizer = Box::new(move |xx: &ExprContext, loc: QExprId| {
            let set = set_for_finalizer.clone();
            for (dt_artifact_id, dt_serial) in &dt_serials {
                let dt_data = {
                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                    must_downcast_ref::<Gc<SourceDataTypeData>>(
                        cache.get(dt_artifact_id).unwrap().as_ref(),
                    ).clone()
                };
                let builtin_ifaces = gen_build_builtin_interfaces(def_set_id.0.0);
                let dt_iface_impl_ids: Vec<ArtifactId> = dt_serial.members.iter().filter(|m_id| {
                    let m_art = xx.vx.ix.gx.artifacts.get((*m_id).clone()).ok();
                    matches!(
                        m_art.as_ref().map(|a| &a.deserializer),
                        Some(ArtifactId::Builtin(b)) if * b == defset_iface_impl_artifact_id()
                    )
                }).cloned().collect();
                let interface_impls: Vec<SourceDataTypeInterfaceImplData> =
                    dt_iface_impl_ids.iter().map(|impl_artifact_id| {
                        let impl_artifact =
                            xx
                                .vx
                                .ix
                                .gx
                                .artifacts
                                .get(impl_artifact_id.clone())
                                .expect("Failed to fetch iface impl artifact");
                        let impl_serial: ArtifactDataDefSetInterfaceImpl =
                            serde_json::from_value(
                                impl_artifact.data,
                            ).expect("Failed to parse iface impl artifact");
                        let iface = match impl_serial.builtin_kind {
                            Some(BuiltinInterfaceKind::Value) => builtin_ifaces.value.clone(),
                            Some(BuiltinInterfaceKind::Type) => builtin_ifaces.type_.clone(),
                            Some(BuiltinInterfaceKind::AspectFunction) => builtin_ifaces.aspect_function.clone(),
                            Some(BuiltinInterfaceKind::AspectField) => builtin_ifaces.aspect_field.clone(),
                            Some(BuiltinInterfaceKind::ScopeEntry) => builtin_ifaces.scope_entry.clone(),
                            Some(BuiltinInterfaceKind::Target) => builtin_ifaces.target.clone(),
                            Some(BuiltinInterfaceKind::CodeDyn) => builtin_ifaces.code_dyn.clone(),
                            None => {
                                let user_iface_id =
                                    impl_serial
                                        .user_iface
                                        .as_ref()
                                        .expect("user-defined interface impl missing user_iface artifact ref");
                                let user_iface_artifact =
                                    xx
                                        .vx
                                        .ix
                                        .gx
                                        .artifacts
                                        .get(user_iface_id.clone())
                                        .expect("Failed to fetch user iface artifact");
                                let user_iface: ArtifactDataDefSetInterface =
                                    serde_json::from_value(
                                        user_iface_artifact.data,
                                    ).expect("Failed to parse user iface artifact");
                                dumpster::unsync::Gc::new(SourceInterfaceData {
                                    artifact_id: RefCell::new(Some(user_iface_id.clone())),
                                    builtin_kind: None,
                                    set: set.clone(),
                                    description: RefCell::new(user_iface.description.clone()),
                                    is_aspect: std::cell::Cell::new(user_iface.is_aspect),
                                    methods: RefCell::new(vec![]),
                                    identity: user_iface.identity.clone(),
                                })
                            },
                        };
                        let method_impls = impl_serial.method_impls.iter().map(|mi_id| {
                            let mi_artifact =
                                xx
                                    .vx
                                    .ix
                                    .gx
                                    .artifacts
                                    .get(mi_id.clone())
                                    .expect("Failed to fetch iface method impl artifact");
                            let m: ArtifactDataDefSetInterfaceMethodImpl =
                                serde_json::from_value(
                                    mi_artifact.data,
                                ).expect("Failed to parse iface method impl artifact");
                            let impl_expr =
                                expr_from_artifact(
                                    xx,
                                    &m.impl_expr,
                                ).expect("Failed to deserialize interface method impl_expr");
                            let captured_externals = deserialize_captured_externals(xx, loc, &m.captured_externals);
                            SourceDataTypeInterfaceMethodImplData {
                                artifact_id: RefCell::new(Some(mi_id.clone())),
                                interface_method_index: m.interface_method_index,
                                impl_expr: RefCell::new(Some(Rc::new(impl_expr) as Rc<dyn Value>)),
                                impl_expr_artifact: RefCell::new(Some(m.impl_expr.clone())),
                                compiled_js: RefCell::new(Some(m.compiled_js.clone())),
                                captured_externals: RefCell::new(captured_externals),
                                scope_entry_id: Cell::new(Some(ScopeEntryId::Def(def_set_id, m.entry_id))),
                                inner_scope_entry_id: Cell::new(None),
                            }
                        }).collect();
                        SourceDataTypeInterfaceImplData {
                            artifact_id: RefCell::new(Some(impl_artifact_id.clone())),
                            interface: iface,
                            method_impls,
                        }
                    }).collect();
                *dt_data.interface_impls.borrow_mut() = interface_impls;
                let impls = dt_data.interface_impls.borrow();
                for (impl_data, impl_artifact_id) in impls.iter().zip(dt_iface_impl_ids.iter()) {
                    let impl_artifact =
                        xx
                            .vx
                            .ix
                            .gx
                            .artifacts
                            .get(impl_artifact_id.clone())
                            .expect("Failed to fetch iface impl artifact for user iface check");
                    let impl_serial: ArtifactDataDefSetInterfaceImpl =
                        serde_json::from_value(impl_artifact.data).expect("Failed to parse iface impl artifact");
                    let Some(user_iface_id) = impl_serial.user_iface.as_ref() else {
                        continue
                    };
                    if !impl_data.interface.methods.borrow().is_empty() {
                        continue
                    }
                    let user_iface_artifact =
                        xx
                            .vx
                            .ix
                            .gx
                            .artifacts
                            .get(user_iface_id.clone())
                            .expect("Failed to fetch user iface artifact");
                    let user_iface: ArtifactDataDefSetInterface =
                        serde_json::from_value(
                            user_iface_artifact.data,
                        ).expect("Failed to parse user iface artifact");
                    let methods = user_iface.methods.iter().map(|m| {
                        let (arg_type, _) =
                            deserialize_type_artifact(xx, loc, &m.arg_type)
                                .into_res()
                                .expect("Failed to deserialize iface method arg_type");
                        let (ret_type, _) =
                            deserialize_type_artifact(xx, loc, &m.ret_type)
                                .into_res()
                                .expect("Failed to deserialize iface method ret_type");
                        Rc::new(SourceInterfaceMethodData {
                            name: m.name.clone(),
                            field_name: m.field_name.clone(),
                            arg_type: RefCell::new(Some(arg_type)),
                            ret_type: RefCell::new(Some(ret_type)),
                        })
                    }).collect();
                    *impl_data.interface.methods.borrow_mut() = methods;
                }
                drop(impls);
            }
            let mut codes: Vec<Code> = Vec::new();
            for (dt_artifact_id, dt_serial) in &dt_serials {
                let dt_data = {
                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                    must_downcast_ref::<Gc<SourceDataTypeData>>(
                        cache.get(dt_artifact_id).unwrap().as_ref(),
                    ).clone()
                };
                let (underlying_type, underlying_code) =
                    deserialize_type_artifact(xx, loc, &dt_serial.underlying_type)
                        .into_res()
                        .map_err(|e| format!("Failed to deserialize underlying type: {:?}", e))?;
                codes.push(underlying_code);
                dt_data.underlying_type.set(underlying_type);
                let dt_method_ids: Vec<ArtifactId> = dt_serial.members.iter().filter(|m_id| {
                    let m_art = xx.vx.ix.gx.artifacts.get((*m_id).clone()).ok();
                    matches!(
                        m_art.as_ref().map(|a| &a.deserializer),
                        Some(ArtifactId::Builtin(b)) if * b == defset_method_artifact_id()
                    )
                }).cloned().collect();
                let methods: Vec<Rc<SourceDataMethodData>> = dt_method_ids.iter().map(|method_artifact_id| {
                    let method_artifact =
                        xx
                            .vx
                            .ix
                            .gx
                            .artifacts
                            .get(method_artifact_id.clone())
                            .expect("Failed to fetch method artifact");
                    let m: ArtifactDataDefSetMethod =
                        serde_json::from_value(method_artifact.data).expect("Failed to parse method artifact");
                    let (arg_type, arg_code) =
                        deserialize_type_artifact(xx, loc, &m.arg_type)
                            .into_res()
                            .unwrap_or_else(|e| panic!("Failed to deserialize method arg type: {:?}", e));
                    codes.push(arg_code);
                    let (ret_type, ret_code) =
                        deserialize_type_artifact(xx, loc, &m.ret_type)
                            .into_res()
                            .unwrap_or_else(|e| panic!("Failed to deserialize method ret type: {:?}", e));
                    codes.push(ret_code);
                    let captured_externals = deserialize_captured_externals(xx, loc, &m.captured_externals);
                    Rc::new(SourceDataMethodData {
                        artifact_id: RefCell::new(Some(method_artifact_id.clone())),
                        name: m.name.clone(),
                        arg_type: RefCell::new(Some(arg_type)),
                        impl_expr: RefCell::new(None),
                        ret_type: RefCell::new(Some(ret_type)),
                        scope_entry_id: Cell::new(Some(ScopeEntryId::Def(def_set_id, m.entry_id))),
                        inner_scope_entry_id: Cell::new(None),
                        field_name: m.field_name.clone(),
                        compiled_js: RefCell::new(Some(m.compiled_js.clone())),
                        captured_externals: RefCell::new(captured_externals),
                    })
                }).collect();
                *dt_data.methods.borrow_mut() = methods;
            }
            for (func_artifact_id, func_serial) in &func_serials {
                let func_data = {
                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                    must_downcast_ref::<Gc<SourceFunctionData>>(
                        cache.get(func_artifact_id).unwrap().as_ref(),
                    ).clone()
                };
                let (arg_type, arg_code) =
                    deserialize_type_artifact(xx, loc, &func_serial.arg_type)
                        .into_res()
                        .map_err(|e| format!("Failed to deserialize function arg type: {:?}", e))?;
                codes.push(arg_code);
                let (ret_type, ret_code) =
                    deserialize_type_artifact(xx, loc, &func_serial.ret_type)
                        .into_res()
                        .map_err(|e| format!("Failed to deserialize function ret type: {:?}", e))?;
                codes.push(ret_code);
                *func_data.arg_type.borrow_mut() = Some(arg_type);
                *func_data.ret_type.borrow_mut() = Some(ret_type);
                let captured = deserialize_captured_externals(xx, loc, &func_serial.captured_externals);
                *func_data.captured_externals.borrow_mut() = captured;
            }
            for dep_id in &def_set_serial.dependencies {
                let dep_set = load_def_set(xx, dep_id).map_err(|e| format!("dep load: {}", e))?;
                set.dependencies.borrow_mut().entry(dep_set.def_set_id).or_insert_with(|| dep_set.clone());
            }
            set.finished.set(true);
            crate::eval::import_install::install_def_set_to_v8(xx, loc, &set)?;
            Ok(codes)
        });
        Ok((set, finalizer))
    })
}

pub(crate) fn next_interface_method_id(set: &DefinitionSet) -> usize {
    set.next_interface_method_id.next()
}

pub(crate) fn serialize_captured_externals(
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
            External::StageLiftCell(cell) => ArtifactExternal::StageLift {
                local_id,
                source_type: serialize_type_ref(xx, loc, cell.source_type.as_ref()),
            },
            External::Type(type_) => ArtifactExternal::Type {
                local_id,
                type_artifact: serialize_type_ref(xx, loc, type_.as_ref()),
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

pub(crate) fn serialize_definition_set(xx: &ExprContext, loc: QExprId, set: &Gc<DefinitionSet>) -> ArtifactId {
    produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &set.artifact_id, || {
        let def_set_artifact_id = set.artifact_id.borrow().clone().unwrap();
        let mut members: Vec<ArtifactId> = Vec::new();
        for dt in set.data_types.borrow().iter() {
            members.push(dt.serialize(xx, loc, def_set_artifact_id.clone()));
        }
        for f in set.functions.borrow().iter() {
            members.push(f.serialize(xx, loc, def_set_artifact_id.clone()));
        }
        let dependencies: Vec<ArtifactId> = {
            let deps = set.dependencies.borrow();
            deps.values().map(|s| serialize_definition_set(xx, loc, s)).collect()
        };
        let serial = ArtifactDataDefSet {
            members,
            def_set_id: set.def_set_id,
            dependencies,
        };
        Artifact {
            deserializer: ArtifactId::Builtin(defset_artifact_identity()),
            data: serde_json::to_value(&serial).expect("Failed to serialize definition set"),
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
