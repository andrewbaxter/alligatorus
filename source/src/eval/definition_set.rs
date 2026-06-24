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
            deserialize_artifact,
            deserialize_type_artifact,
            impl_deserialize_independent_artifact,
            impl_serialize_independent_artifact,
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
                type_source_exports::r#const::TypeSourceExportsLiftedConst,
                type_source_function::SourceFunctionData,
                type_source_interface::{
                    BuiltinInterfaceKind,
                    SourceInterfaceData,
                    SourceInterfaceMethodData,
                },
            },
        },
        invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
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
                let source_type =
                    deserialize_type_artifact(xx, loc, source_type.clone())
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
                    deserialize_type_artifact(xx, loc, type_artifact.clone())
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
                source_type: todo!("serialize_type_ref pending design"),
            },
            External::Type(type_) => ArtifactExternal::Type {
                local_id,
                type_artifact: todo!("serialize_type_ref pending design"),
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

// ====================================================================
// Inline serialization of definition-set member data into nested types
// ====================================================================

fn serialize_method(
    xx: &ExprContext,
    loc: QExprId,
    m: &Rc<SourceDataMethodData>,
) -> ArtifactDataDefSetMethod {
    let compiled_js = m.compiled_js.borrow().clone().unwrap_or_default();
    let entry_id = match m.scope_entry_id.get() {
        Some(ScopeEntryId::Def(_, eid)) => eid,
        _ => panic!("Method scope_entry_id missing or not Def"),
    };
    let captured_externals = serialize_captured_externals(xx, loc, &m.captured_externals.borrow());
    ArtifactDataDefSetMethod {
        name: m.name.clone(),
        entry_id,
        field_name: m.field_name.clone(),
        arg_type: todo!("serialize_type_ref pending design"),
        ret_type: todo!("serialize_type_ref pending design"),
        compiled_js,
        captured_externals,
    }
}

fn serialize_iface_method_impl(
    xx: &ExprContext,
    loc: QExprId,
    m: &SourceDataTypeInterfaceMethodImplData,
) -> ArtifactDataDefSetInterfaceMethodImpl {
    let compiled_js = m.compiled_js.borrow().clone().unwrap_or_default();
    let entry_id = match m.scope_entry_id.get().expect("Interface method impl missing scope_entry_id") {
        ScopeEntryId::Def(_, id) => id,
        other => panic!("Interface method impl has non-Def scope_entry_id: {:?}", other),
    };
    if m.impl_expr_artifact.borrow().is_none() {
        let impl_expr_value = m.impl_expr.borrow().clone();
        if let Some(impl_expr_value) = impl_expr_value {
            let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr_value.as_ref());
            if let Ok(serialized) = expr_to_artifact(xx, impl_expr_ast) {
                *m.impl_expr_artifact.borrow_mut() = Some(serialized);
            }
        }
    }
    let impl_expr = m.impl_expr_artifact.borrow().clone().unwrap_or_else(|| {
        use crate::eval::expr_artifact::{
            ArtifactExpr,
            ArtifactExpr_,
            ArtifactQExprId,
        };

        ArtifactExpr {
            loc: ArtifactQExprId(0, 0),
            r#type: Box::new(
                ArtifactExpr_::Literal {
                    value: todo!("serialize_type_ref pending design"),
                },
            ),
        }
    });
    let captured_externals = serialize_captured_externals(xx, loc, &m.captured_externals.borrow());
    ArtifactDataDefSetInterfaceMethodImpl {
        interface_method_index: m.interface_method_index,
        entry_id,
        compiled_js,
        impl_expr,
        captured_externals,
    }
}

fn serialize_interface(
    _xx: &ExprContext,
    _loc: QExprId,
    iface: &Gc<SourceInterfaceData>,
) -> ArtifactDataDefSetInterface {
    let methods: Vec<ArtifactDataDefSetInterfaceMethod> = iface.methods.borrow().iter().map(|m| {
        ArtifactDataDefSetInterfaceMethod {
            name: m.name.clone(),
            field_name: m.field_name.clone(),
            arg_type: todo!("serialize_type_ref pending design"),
            ret_type: todo!("serialize_type_ref pending design"),
        }
    }).collect();
    ArtifactDataDefSetInterface {
        identity: iface.identity.clone(),
        description: iface.description.borrow().clone(),
        is_aspect: iface.is_aspect.get(),
        methods,
    }
}

fn serialize_iface_impl(
    xx: &ExprContext,
    loc: QExprId,
    impl_data: &SourceDataTypeInterfaceImplData,
) -> ArtifactDataDefSetInterfaceImpl {
    let builtin_kind = impl_data.interface.builtin_kind;
    let method_impls: Vec<ArtifactDataDefSetInterfaceMethodImpl> =
        impl_data.method_impls.iter().map(|m| serialize_iface_method_impl(xx, loc, m)).collect();
    let user_iface = match builtin_kind {
        Some(_) => None,
        // TODO(porting): user-defined interfaces need their own artifact id strategy
        // — currently inlined; consider Artifact::Ref to dedupe across data types.
        None => Some(Artifact::Body(ArtifactBody {
            data: serde_json::to_value(&serialize_interface(xx, loc, &impl_data.interface)).unwrap(),
            exporter: ArtifactBodyExporter::Root,
        })),
    };
    ArtifactDataDefSetInterfaceImpl {
        builtin_kind,
        user_iface,
        method_impls,
    }
}

fn serialize_data_type(
    xx: &ExprContext,
    loc: QExprId,
    dt: &Gc<SourceDataTypeData>,
) -> ArtifactDataDefSetDataType {
    let methods: Vec<ArtifactDataDefSetMethod> =
        dt.methods.borrow().iter().map(|m| serialize_method(xx, loc, m)).collect();
    let interface_impls: Vec<ArtifactDataDefSetInterfaceImpl> =
        dt.interface_impls.borrow().iter().map(|i| serialize_iface_impl(xx, loc, i)).collect();
    ArtifactDataDefSetDataType {
        name: dt.name.clone(),
        data_field: dt.data_field.clone(),
        underlying_type: todo!("serialize_type_ref pending design"),
        identity: dt.identity.clone(),
        methods,
        interface_impls,
    }
}

fn serialize_function(
    xx: &ExprContext,
    loc: QExprId,
    f: &Gc<SourceFunctionData>,
) -> ArtifactDataDefSetFunction {
    let compiled_js = f.compiled_js.borrow().clone().unwrap_or_default();
    let entry_id = match f.scope_entry_id.get().expect("Function missing scope_entry_id") {
        ScopeEntryId::Def(_, id) => id,
        other => panic!("Function has non-Def scope_entry_id: {:?}", other),
    };
    let captured_externals = serialize_captured_externals(xx, loc, &f.captured_externals.borrow());
    ArtifactDataDefSetFunction {
        name: f.name.clone(),
        entry_id,
        arg_type: todo!("serialize_type_ref pending design"),
        ret_type: todo!("serialize_type_ref pending design"),
        compiled_js,
        captured_externals,
    }
}

pub(crate) fn serialize_definition_set(xx: &ExprContext, loc: QExprId, set: &Gc<DefinitionSet>) -> Artifact {
    impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &set.artifact_id, || {
        let data_types: Vec<ArtifactDataDefSetDataType> =
            set.data_types.borrow().iter().map(|dt| serialize_data_type(xx, loc, dt)).collect();
        let functions: Vec<ArtifactDataDefSetFunction> =
            set.functions.borrow().iter().map(|f| serialize_function(xx, loc, f)).collect();
        let interfaces: Vec<ArtifactDataDefSetInterface> =
            set.interfaces.borrow().iter().map(|i| serialize_interface(xx, loc, i)).collect();
        let dependencies: Vec<Artifact> = {
            let deps = set.dependencies.borrow();
            deps.values().map(|s| serialize_definition_set(xx, loc, s)).collect()
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
                    crate::eval::value::type_root::r#const::ExporterRoot::DefinitionSet,
                ).unwrap(),
                exporter: ArtifactBodyExporter::Root,
            })),
        }
    })
}

pub(crate) fn load_def_set(xx: &ExprContext, def_set_artifact: Artifact) -> Result<Gc<DefinitionSet>, String> {
    let any = deserialize_artifact(xx, (crate::langhelp::SourceId(0), crate::syntax::ExprId(0)), def_set_artifact);
    if let Some(errors) = any.errors {
        return Err(format!("Failed to deserialize def set: {:?}", errors));
    }
    let any = any.v.ok_or_else(|| "Definition set deserialization yielded no value".to_string())?;
    let rc: Rc<dyn Any> = any;
    let wrapped =
        rc.downcast::<ExporterDataAny<Gc<DefinitionSet>>>().map_err(|_| "Expected DefinitionSet artifact".to_string())?;
    Ok(wrapped.0.clone())
}

pub(crate) fn definition_set_from_artifact_data(
    xx: &ExprContext,
    loc: QExprId,
    id: Option<&ArtifactIdSource>,
    data: serde_json::Value,
) -> crate::langhelp::EResult<Rc<dyn Any>> {
    let id = match id {
        Some(id) => id,
        None => return crate::langhelp::EResult::err(
            loc,
            "DefinitionSet requires an artifact id to deserialize".to_string(),
        ),
    };
    let id_owned = id.clone();
    let res = impl_deserialize_independent_artifact::<ArtifactDataDefSet>(xx, loc, id, data, move |body| {
        let set = Gc::new(DefinitionSet::new(body.def_set_id));
        *set.artifact_id.borrow_mut() = Some(id_owned.clone());
        let def_set_id = body.def_set_id;

        // Build data types skeleton
        let mut dt_gcs: Vec<Gc<SourceDataTypeData>> = Vec::new();
        for dt_serial in &body.data_types {
            let dt_data = Gc::new(SourceDataTypeData {
                artifact_id: RefCell::new(None),
                set: set.clone(),
                name: dt_serial.name.clone(),
                identity: dt_serial.identity.clone(),
                data_field: dt_serial.data_field.clone(),
                description: RefCell::new(None),
                underlying_type: DeserialLazy::new(),
                methods: RefCell::new(Vec::new()),
                interface_impls: RefCell::new(Vec::new()),
            });
            dt_gcs.push(dt_data);
        }

        // Build functions skeleton
        let mut func_gcs: Vec<Gc<SourceFunctionData>> = Vec::new();
        for func_serial in &body.functions {
            let func_data = Gc::new(SourceFunctionData {
                artifact_id: RefCell::new(None),
                set: set.clone(),
                name: func_serial.name.clone(),
                description: RefCell::new(None),
                impl_expr: RefCell::new(None),
                arg_type: RefCell::new(None),
                ret_type: RefCell::new(None),
                scope_entry_id: Cell::new(Some(ScopeEntryId::Def(def_set_id, func_serial.entry_id))),
                compiled_js: RefCell::new(Some(func_serial.compiled_js.clone())),
                captured_externals: RefCell::new(Vec::new()),
            });
            func_gcs.push(func_data);
        }

        *set.data_types.borrow_mut() = dt_gcs;
        *set.functions.borrow_mut() = func_gcs;

        let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(set.clone()));
        let set_for_finalizer = set.clone();
        let body_for_finalizer = body;
        let finalizer: ArtifactFinalizer = Box::new(move |xx: &ExprContext, loc: QExprId| {
            let set = set_for_finalizer.clone();
            let body = body_for_finalizer;
            let builtin_ifaces = gen_build_builtin_interfaces(def_set_id.0.0);

            // Hook up data types
            let dt_gcs = set.data_types.borrow().clone();
            for (dt_data, dt_serial) in dt_gcs.iter().zip(body.data_types.iter()) {
                let underlying_type =
                    deserialize_type_artifact(xx, loc, dt_serial.underlying_type.clone())
                        .into_res()
                        .unwrap_or_else(|e| panic!("Failed to deserialize underlying type: {:?}", e));
                dt_data.underlying_type.set(underlying_type);

                let methods: Vec<Rc<SourceDataMethodData>> = dt_serial.methods.iter().map(|m| {
                    let arg_type =
                        deserialize_type_artifact(xx, loc, m.arg_type.clone())
                            .into_res()
                            .unwrap_or_else(|e| panic!("Failed to deserialize method arg type: {:?}", e));
                    let ret_type =
                        deserialize_type_artifact(xx, loc, m.ret_type.clone())
                            .into_res()
                            .unwrap_or_else(|e| panic!("Failed to deserialize method ret type: {:?}", e));
                    let captured_externals = deserialize_captured_externals(xx, loc, &m.captured_externals);
                    Rc::new(SourceDataMethodData {
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

                let interface_impls: Vec<SourceDataTypeInterfaceImplData> =
                    dt_serial.interface_impls.iter().map(|impl_serial| {
                        let iface = match impl_serial.builtin_kind {
                            Some(BuiltinInterfaceKind::Value) => builtin_ifaces.value.clone(),
                            Some(BuiltinInterfaceKind::Type) => builtin_ifaces.type_.clone(),
                            Some(BuiltinInterfaceKind::AspectFunction) => builtin_ifaces.aspect_function.clone(),
                            Some(BuiltinInterfaceKind::AspectField) => builtin_ifaces.aspect_field.clone(),
                            Some(BuiltinInterfaceKind::ScopeEntry) => builtin_ifaces.scope_entry.clone(),
                            Some(BuiltinInterfaceKind::Target) => builtin_ifaces.target.clone(),
                            Some(BuiltinInterfaceKind::CodeDyn) => builtin_ifaces.code_dyn.clone(),
                            None => {
                                let user_iface_artifact =
                                    impl_serial
                                        .user_iface
                                        .as_ref()
                                        .expect("user-defined interface impl missing user_iface artifact");
                                // Inline user iface deserialization
                                let user_iface: ArtifactDataDefSetInterface = match user_iface_artifact {
                                    Artifact::Body(b) => serde_json::from_value(b.data.clone())
                                        .expect("Failed to parse user iface artifact"),
                                    Artifact::Ref(_) => {
                                        // TODO(porting): support Ref'd user interfaces
                                        panic!("Ref'd user_iface not yet supported")
                                    },
                                };
                                let methods = user_iface.methods.iter().map(|m| {
                                    let arg_type =
                                        deserialize_type_artifact(xx, loc, m.arg_type.clone())
                                            .into_res()
                                            .expect("Failed to deserialize iface method arg_type");
                                    let ret_type =
                                        deserialize_type_artifact(xx, loc, m.ret_type.clone())
                                            .into_res()
                                            .expect("Failed to deserialize iface method ret_type");
                                    Rc::new(SourceInterfaceMethodData {
                                        name: m.name.clone(),
                                        field_name: m.field_name.clone(),
                                        arg_type: RefCell::new(Some(arg_type)),
                                        ret_type: RefCell::new(Some(ret_type)),
                                    })
                                }).collect();
                                Gc::new(SourceInterfaceData {
                                    artifact_id: RefCell::new(None),
                                    builtin_kind: None,
                                    set: set.clone(),
                                    description: RefCell::new(user_iface.description.clone()),
                                    is_aspect: Cell::new(user_iface.is_aspect),
                                    methods: RefCell::new(methods),
                                    identity: user_iface.identity.clone(),
                                })
                            },
                        };
                        let method_impls = impl_serial.method_impls.iter().map(|m| {
                            let impl_expr =
                                expr_from_artifact(
                                    xx,
                                    &m.impl_expr,
                                ).expect("Failed to deserialize interface method impl_expr");
                            let captured_externals = deserialize_captured_externals(xx, loc, &m.captured_externals);
                            SourceDataTypeInterfaceMethodImplData {
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
                            interface: iface,
                            method_impls,
                        }
                    }).collect();
                *dt_data.interface_impls.borrow_mut() = interface_impls;
            }

            // Hook up function types
            let func_gcs = set.functions.borrow().clone();
            for (func_data, func_serial) in func_gcs.iter().zip(body.functions.iter()) {
                let arg_type =
                    deserialize_type_artifact(xx, loc, func_serial.arg_type.clone())
                        .into_res()
                        .unwrap_or_else(|e| panic!("Failed to deserialize function arg type: {:?}", e));
                let ret_type =
                    deserialize_type_artifact(xx, loc, func_serial.ret_type.clone())
                        .into_res()
                        .unwrap_or_else(|e| panic!("Failed to deserialize function ret type: {:?}", e));
                *func_data.arg_type.borrow_mut() = Some(arg_type);
                *func_data.ret_type.borrow_mut() = Some(ret_type);
                let captured = deserialize_captured_externals(xx, loc, &func_serial.captured_externals);
                *func_data.captured_externals.borrow_mut() = captured;
            }

            // Dependencies
            for dep in body.dependencies {
                match load_def_set(xx, dep) {
                    Ok(dep_set) => {
                        set.dependencies.borrow_mut().entry(dep_set.def_set_id).or_insert_with(|| dep_set.clone());
                    },
                    Err(e) => panic!("dep load: {}", e),
                }
            }
            set.finished.set(true);
            let _ = crate::eval::import_install::install_def_set_to_v8(xx, loc, &set);
        });
        Ok((out, Vec::new(), finalizer))
    });
    let errors = res.errors;
    let Some(any) = res.v else {
        return crate::langhelp::EResult {
            errors,
            v: None,
        };
    };
    let rc: Rc<dyn Any> = any;
    crate::langhelp::EResult {
        errors,
        v: Some(rc),
    }
}

pub(crate) fn definition_set_to_serial(
    xx: &ExprContext,
    loc: QExprId,
    self_: Rc<dyn Any>,
    _make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
) -> crate::langhelp::EResult<Artifact> {
    let wrapped =
        self_
            .downcast::<ExporterDataAny<Gc<DefinitionSet>>>()
            .expect("DefinitionSet self_to_serial: expected DefinitionSet");
    crate::langhelp::EResult::ok(serialize_definition_set(xx, loc, &wrapped.0))
}

pub(crate) fn next_interface_method_id(set: &DefinitionSet) -> usize {
    set.next_interface_method_id.next()
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
