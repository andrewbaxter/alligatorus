use {
    crate::{
        eval::value::{
            type_source_data_type::SourceDataTypeData,
            type_source_function::SourceFunctionData,
            type_source_interface::SourceInterfaceData,
        },
        langhelp::{
            Aspect,
            Code,
            ExprContext,
            QExprId,
            Type,
            Unique,
            Value,
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
    if *key == crate::r#gen::aspect_lift_dynamic_aspect_id() {
        return Some(Box::new(Box::new(TypeDefinitionSetLiftDynamic) as Box<dyn crate::r#gen::AspectLiftDynamic>));
    }
    if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
        return Some(
            Box::new(Box::new(TypeDefinitionSetLiftDynamic) as Box<dyn crate::r#gen::AspectLowerDynamic>),
        );
    }
    None
}

pub(crate) struct DefinitionSet {
    pub artifact_id: RefCell<Option<crate::langhelp::ArtifactId>>,
    pub data_types: RefCell<Vec<Gc<SourceDataTypeData>>>,
    pub def_set_id: crate::langhelp::DefSetId,
    pub finished: Cell<bool>,
    pub functions: RefCell<Vec<Gc<SourceFunctionData>>>,
    pub import_id: crate::langhelp::ImportStateId,
    pub interfaces: RefCell<Vec<Gc<SourceInterfaceData>>>,
    pub next_entry_id: crate::langhelp::Counter,
    pub next_field_id: crate::langhelp::Counter,
    pub next_interface_method_id: crate::langhelp::Counter,
    /// Source id of the compile that created this definition set. Imports use this
    /// together with `import_id` to look up the per-compile IMPORT_STAGE_DATA when
    /// rebuilding instance objects in the importer's isolate.
    pub source_id: crate::langhelp::SourceId,
}

impl DefinitionSet {
    pub fn finish(&self) -> Result<(), String> {
        if self.finished.get() {
            return Ok(());
        }
        for func_data in self.functions.borrow().iter() {
            func_data.finish()?;
        }
        for dt_data in self.data_types.borrow().iter() {
            dt_data.finish()?;
        }
        for iface in self.interfaces.borrow().iter() {
            iface.finish()?;
        }
        self.finished.set(true);
        Ok(())
    }

    pub fn new(
        import_id: crate::langhelp::ImportStateId,
        def_set_id: crate::langhelp::DefSetId,
        source_id: crate::langhelp::SourceId,
    ) -> Self {
        DefinitionSet {
            artifact_id: RefCell::new(None),
            import_id: import_id,
            def_set_id: def_set_id,
            functions: RefCell::new(Vec::new()),
            data_types: RefCell::new(Vec::new()),
            interfaces: RefCell::new(Vec::new()),
            finished: Cell::new(false),
            next_field_id: crate::langhelp::Counter::default(),
            next_entry_id: crate::langhelp::Counter::default(),
            next_interface_method_id: crate::langhelp::Counter::default(),
            source_id,
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

pub(crate) fn defset_artifact_identity() -> crate::langhelp::ArtifactIdBuiltin {
    crate::r#gen::defset_artifact_id()
}

/// Reconstruct a body's captured `External`s from `ArtifactExternal`s. Used after
/// cross-module import so the body's `compiled_js` finds its closure bindings
/// populated at v8 setup. The reconstructed `Local` ids match the body's original
/// local-id numbering.
pub(crate) fn deserialize_captured_externals(
    xx: &ExprContext,
    loc: QExprId,
    serial: &[crate::eval::value::type_source_data_type::artifact::ArtifactExternal],
) -> Vec<(crate::langhelp::ScopeEntryId, crate::langhelp::External)> {
    use crate::eval::value::type_source_data_type::artifact::ArtifactExternal;

    serial.iter().map(|ext| {
        match ext {
            ArtifactExternal::StagedExpr { local_id, expr } => {
                let expr =
                    crate::eval::expr_artifact::expr_from_artifact(
                        xx,
                        expr,
                    ).expect("Failed to deserialize captured StagedExprCell expr");
                let cell =
                    Rc::new(
                        crate::eval::expr::StagedExprCell {
                            value: RefCell::new(Some(Rc::new(expr) as Rc<dyn crate::langhelp::Value>)),
                        },
                    );
                (crate::langhelp::ScopeEntryId::Local(*local_id), crate::langhelp::External::StagedExprCell(cell))
            },
            ArtifactExternal::StageLift { local_id, source_type } => {
                let (source_type, _code) =
                    crate::eval::util::deserialize_type_artifact(xx, loc, source_type)
                        .into_res()
                        .expect("Failed to deserialize captured StageLiftCell source type");
                let cell = Rc::new(crate::eval::StageLiftCell {
                    source_type,
                    loc,
                    vx: xx.vx.clone(),
                    import_state: xx.import_state.clone(),
                    scope: xx.scope.clone(),
                    target: xx.target.clone(),
                });
                (crate::langhelp::ScopeEntryId::Local(*local_id), crate::langhelp::External::StageLiftCell(cell))
            },
            ArtifactExternal::Type { local_id, type_artifact } => {
                let (type_, _code) =
                    crate::eval::util::deserialize_type_artifact(xx, loc, type_artifact)
                        .into_res()
                        .expect("Failed to deserialize captured External::Type");
                (crate::langhelp::ScopeEntryId::Local(*local_id), crate::langhelp::External::Type(type_))
            },
            ArtifactExternal::Unique { local_id, unique } => (
                crate::langhelp::ScopeEntryId::Local(*local_id),
                crate::langhelp::External::Unique(*unique),
            ),
        }
    }).collect()
}

/// Single point of entry for deserializing a `DefinitionSet` and all its data
/// types and functions. Implements the two-phase pattern:
///
/// 1. Build empty `Gc<DefinitionSet>` shell, build all data-type and function shells,
///    register each in `deserialized_artifacts`, attach to `set.data_types` /
///    `set.functions`. No cross-artifact reads.
///
/// 2. Register a single finalizer that resolves cross-artifact references (data type's
///    underlying_type / method ret types / interface_impls; function arg_type /
///    ret_type) by reading other shells out of the cache.
pub(crate) fn load_def_set(
    xx: &ExprContext,
    def_set_artifact_id: &crate::langhelp::ArtifactId,
) -> Result<Gc<DefinitionSet>, String> {
    use crate::{
        eval::value::{
            type_source_data_type::{
                SourceDataMethodData,
                SourceDataTypeInterfaceImplData,
                SourceDataTypeInterfaceMethodImplData,
                artifact::{
                    ArtifactDataDefSet,
                    ArtifactDataDefSetDataType,
                    ArtifactDataDefSetFunction,
                },
            },
            type_source_interface::BuiltinInterfaceKind,
        },
        invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
    };

    crate::langhelp::deserialize_independent_artifact(&xx.vx.ix, def_set_artifact_id, || {
        let def_set_artifact = xx.vx.ix.gx.artifacts.get(def_set_artifact_id.clone())?;
        let def_set_serial: ArtifactDataDefSet =
            serde_json::from_value(
                def_set_artifact.data,
            ).map_err(|e| format!("Failed to parse definition set: {}", e))?;
        let import_id = def_set_serial.import_id;
        let def_set_id = def_set_serial.def_set_id;
        let set = Gc::new(DefinitionSet::new(import_id, def_set_id, def_set_serial.source_id));
        let mut dt_serials: Vec<(crate::langhelp::ArtifactId, ArtifactDataDefSetDataType)> = Vec::new();
        let mut dt_gcs: Vec<Gc<SourceDataTypeData>> = Vec::new();
        for dt_artifact_id in &def_set_serial.data_types {
            let dt_artifact = xx.vx.ix.gx.artifacts.get(dt_artifact_id.clone())?;
            let dt_serial: ArtifactDataDefSetDataType =
                serde_json::from_value(dt_artifact.data).map_err(|e| format!("Failed to parse data type: {}", e))?;
            let dt_data = Gc::new(SourceDataTypeData {
                set: set.clone(),
                name: dt_serial.name.clone(),
                identity: dt_serial.identity.clone(),
                data_field: dt_serial.data_field.clone(),
                description: RefCell::new(None),
                underlying_type: DeserialLazy::new(),
                methods: RefCell::new(Vec::new()),
                interface_impls: RefCell::new(Vec::new()),
                artifact_id: RefCell::new(Some(dt_artifact_id.clone())),
            });
            xx.vx.ix.deserialized_artifacts.borrow_mut().insert(dt_artifact_id.clone(), Box::new(dt_data.clone()));
            dt_gcs.push(dt_data);
            dt_serials.push((dt_artifact_id.clone(), dt_serial));
        }
        *set.data_types.borrow_mut() = dt_gcs;
        let mut func_serials: Vec<(crate::langhelp::ArtifactId, ArtifactDataDefSetFunction)> = Vec::new();
        let mut func_gcs: Vec<Gc<SourceFunctionData>> = Vec::new();
        for func_artifact_id in &def_set_serial.functions {
            let func_artifact = xx.vx.ix.gx.artifacts.get(func_artifact_id.clone())?;
            let func_serial: ArtifactDataDefSetFunction =
                serde_json::from_value(
                    func_artifact.data,
                ).map_err(|e| format!("Failed to parse function: {}", e))?;
            let func_data = Gc::new(SourceFunctionData {
                set: set.clone(),
                name: func_serial.name.clone(),
                description: RefCell::new(None),
                impl_expr: RefCell::new(None),
                arg_type: RefCell::new(None),
                ret_type: RefCell::new(None),
                scope_entry_id: Cell::new(
                    Some(crate::langhelp::ScopeEntryId::Def(import_id, def_set_id, func_serial.entry_id)),
                ),
                artifact_id: RefCell::new(Some(func_artifact_id.clone())),
                compiled_js: RefCell::new(Some(func_serial.compiled_js.clone())),
                captured_externals: RefCell::new(Vec::new()),
            });
            xx
                .vx
                .ix
                .deserialized_artifacts
                .borrow_mut()
                .insert(func_artifact_id.clone(), Box::new(func_data.clone()));
            func_gcs.push(func_data);
            func_serials.push((func_artifact_id.clone(), func_serial));
        }
        *set.functions.borrow_mut() = func_gcs;
        let set_for_finalizer = set.clone();
        let user_iface_impls:
            Vec<
                (
                    crate::langhelp::ArtifactId,
                    Vec<crate::eval::value::type_source_data_type::artifact::ArtifactDataDefSetInterfaceImpl>,
                ),
            > =
            dt_serials.iter().map(|(id, s)| (id.clone(), s.interface_impls.clone())).collect();
        let finalizer: crate::langhelp::ArtifactFinalizer = Box::new(move |xx: &ExprContext, loc: QExprId| {
            let set = set_for_finalizer.clone();

            // Materialise iface method arg/ret types on each user-defined iface that an impl
            // on this def-set's data types holds. Done here (not in the build closure) so
            // type artifacts referring back to data types in this same def-set find this
            // def-set's shell already cached.
            for (dt_artifact_id, dt_impl_serials) in &user_iface_impls {
                let dt_data = {
                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                    must_downcast_ref::<Gc<SourceDataTypeData>>(
                        cache.get(dt_artifact_id).unwrap().as_ref(),
                    ).clone()
                };
                let impls = dt_data.interface_impls.borrow();
                for (impl_data, impl_serial) in impls.iter().zip(dt_impl_serials.iter()) {
                    let Some(user_iface) = impl_serial.user_iface.as_ref() else {
                        continue;
                    };
                    if !impl_data.interface.methods.borrow().is_empty() {
                        continue;
                    }
                    let methods = user_iface.methods.iter().map(|m| {
                        let (arg_type, _) =
                            crate::eval::util::deserialize_type_artifact(xx, loc, &m.arg_type)
                                .into_res()
                                .expect("Failed to deserialize iface method arg_type");
                        let (ret_type, _) =
                            crate::eval::util::deserialize_type_artifact(xx, loc, &m.ret_type)
                                .into_res()
                                .expect("Failed to deserialize iface method ret_type");
                        Rc::new(crate::eval::value::type_source_interface::SourceInterfaceMethodData {
                            name: m.name.clone(),
                            field_name: m.field_name.clone(),
                            arg_type: RefCell::new(Some(arg_type)),
                            ret_type: RefCell::new(Some(ret_type)),
                        })
                    }).collect();
                    *impl_data.interface.methods.borrow_mut() = methods;
                }
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
                    crate::eval::util::deserialize_type_artifact(xx, loc, &dt_serial.underlying_type)
                        .into_res()
                        .map_err(|e| format!("Failed to deserialize underlying type: {:?}", e))?;
                codes.push(underlying_code);
                dt_data.underlying_type.set(underlying_type);
                let methods: Vec<Rc<SourceDataMethodData>> = dt_serial.methods.iter().map(|m| {
                    let (arg_type, arg_code) =
                        crate::eval::util::deserialize_type_artifact(xx, loc, &m.arg_type)
                            .into_res()
                            .unwrap_or_else(|e| panic!("Failed to deserialize method arg type: {:?}", e));
                    codes.push(arg_code);
                    let (ret_type, ret_code) =
                        crate::eval::util::deserialize_type_artifact(xx, loc, &m.ret_type)
                            .into_res()
                            .unwrap_or_else(|e| panic!("Failed to deserialize method ret type: {:?}", e));
                    codes.push(ret_code);
                    let captured_externals = deserialize_captured_externals(xx, loc, &m.captured_externals);
                    Rc::new(SourceDataMethodData {
                        name: m.name.clone(),
                        arg_type: RefCell::new(Some(arg_type)),
                        impl_expr: RefCell::new(None),
                        ret_type: RefCell::new(Some(ret_type)),
                        scope_entry_id: Cell::new(
                            Some(crate::langhelp::ScopeEntryId::Def(import_id, def_set_id, m.entry_id)),
                        ),
                        inner_scope_entry_id: Cell::new(None),
                        field_name: m.field_name.clone(),
                        compiled_js: RefCell::new(Some(m.compiled_js.clone())),
                        captured_externals: RefCell::new(captured_externals),
                    })
                }).collect();
                *dt_data.methods.borrow_mut() = methods;
                let builtin_ifaces = crate::r#gen::gen_build_builtin_interfaces(import_id);
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
                                // User-defined iface — the signature was serialized inline on the impl. Build a
                                // shell here with empty methods; the finalizer materialises method arg/ret types
                                // (running it now would recurse for any iface method type that's a data type in
                                // this same, not-yet-cached def-set).
                                let user_iface =
                                    impl_serial
                                        .user_iface
                                        .as_ref()
                                        .expect(
                                            "user-defined interface impl artifact missing inline iface signature",
                                        );
                                dumpster::unsync::Gc::new(
                                    crate::eval::value::type_source_interface::SourceInterfaceData {
                                        builtin_kind: None,
                                        set: set.clone(),
                                        description: RefCell::new(user_iface.description.clone()),
                                        is_aspect: std::cell::Cell::new(user_iface.is_aspect),
                                        methods: RefCell::new(vec![]),
                                        identity: user_iface.identity.clone(),
                                    },
                                )
                            },
                        };
                        let method_impls = impl_serial.method_impls.iter().map(|m| {
                            let impl_expr =
                                crate::eval::expr_artifact::expr_from_artifact(
                                    xx,
                                    &m.impl_expr,
                                ).expect("Failed to deserialize interface method impl_expr");
                            let captured_externals = deserialize_captured_externals(xx, loc, &m.captured_externals);
                            SourceDataTypeInterfaceMethodImplData {
                                interface_method_index: m.interface_method_index,
                                impl_expr: RefCell::new(
                                    Some(Rc::new(impl_expr) as Rc<dyn crate::langhelp::Value>),
                                ),
                                impl_expr_artifact: RefCell::new(Some(m.impl_expr.clone())),
                                compiled_js: RefCell::new(Some(m.compiled_js.clone())),
                                captured_externals: RefCell::new(captured_externals),
                                scope_entry_id: Cell::new(
                                    Some(crate::langhelp::ScopeEntryId::Def(import_id, def_set_id, m.entry_id)),
                                ),
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
            for (func_artifact_id, func_serial) in &func_serials {
                let func_data = {
                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                    must_downcast_ref::<Gc<SourceFunctionData>>(
                        cache.get(func_artifact_id).unwrap().as_ref(),
                    ).clone()
                };
                let (arg_type, arg_code) =
                    crate::eval::util::deserialize_type_artifact(xx, loc, &func_serial.arg_type)
                        .into_res()
                        .map_err(|e| format!("Failed to deserialize function arg type: {:?}", e))?;
                codes.push(arg_code);
                let (ret_type, ret_code) =
                    crate::eval::util::deserialize_type_artifact(xx, loc, &func_serial.ret_type)
                        .into_res()
                        .map_err(|e| format!("Failed to deserialize function ret type: {:?}", e))?;
                codes.push(ret_code);
                *func_data.arg_type.borrow_mut() = Some(arg_type);
                *func_data.ret_type.borrow_mut() = Some(ret_type);
                let captured = deserialize_captured_externals(xx, loc, &func_serial.captured_externals);
                *func_data.captured_externals.borrow_mut() = captured;
            }
            Ok(codes)
        });
        Ok((set, finalizer))
    })
}

pub(crate) fn next_interface_method_id(set: &DefinitionSet) -> usize {
    set.next_interface_method_id.next()
}

/// Serialize a body's captured `External`s into `ArtifactExternal`s. Each
/// external's `ScopeEntryId` must be `Local(n)` — body-local IDs.
pub(crate) fn serialize_captured_externals(
    xx: &ExprContext,
    loc: QExprId,
    externals: &[(crate::langhelp::ScopeEntryId, crate::langhelp::External)],
) -> Vec<crate::eval::value::type_source_data_type::artifact::ArtifactExternal> {
    use crate::eval::value::type_source_data_type::artifact::ArtifactExternal;

    externals.iter().map(|(id, ext)| {
        let local_id = match id {
            crate::langhelp::ScopeEntryId::Local(n) => *n,
            other => panic!("captured external has non-Local scope entry id: {:?}", other),
        };
        match ext {
            crate::langhelp::External::StagedExprCell(cell) => {
                let value = cell.value.borrow().clone().expect("captured StagedExprCell already consumed");
                let expr_ref = crate::invariant::must_downcast_ref::<crate::eval::expr::Expr>(value.as_ref());
                let expr =
                    crate::eval::expr_artifact::expr_to_artifact(
                        xx,
                        expr_ref,
                    ).expect("Failed to serialize captured StagedExprCell value");
                ArtifactExternal::StagedExpr {
                    local_id,
                    expr,
                }
            },
            crate::langhelp::External::StageLiftCell(cell) => ArtifactExternal::StageLift {
                local_id,
                source_type: crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                    xx,
                    loc,
                    cell.source_type.as_ref(),
                ),
            },
            crate::langhelp::External::Type(type_) => ArtifactExternal::Type {
                local_id,
                type_artifact: crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                    xx,
                    loc,
                    type_.as_ref(),
                ),
            },
            crate::langhelp::External::Unique(u) => ArtifactExternal::Unique {
                local_id,
                unique: *u,
            },
            crate::langhelp::External::JsInstance(_) => {
                panic!("External::JsInstance is runtime-only and must not be serialized")
            },
        }
    }).collect()
}

pub(crate) fn serialize_definition_set(
    xx: &ExprContext,
    loc: QExprId,
    set: &Gc<DefinitionSet>,
) -> crate::langhelp::ArtifactId {
    use crate::eval::value::{
        type_source_data_type::artifact::{
            ArtifactDataDefSet,
            ArtifactDataDefSetDataType,
            ArtifactDataDefSetInterfaceImpl,
            ArtifactDataDefSetInterfaceMethodImpl,
            ArtifactDataDefSetMethod,
        },
        type_source_data_type_constructor::dynamic::serialize_type_ref,
        type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
    };

    crate::langhelp::produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &set.artifact_id, || {
        let def_set_artifact_id = set.artifact_id.borrow().clone().unwrap();
        let data_types = set.data_types.borrow();
        let data_type_ids: Vec<crate::langhelp::ArtifactId> = data_types.iter().map(|dt| {
            crate::langhelp::produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &dt.artifact_id, || {
                let underlying_type = dt.underlying_type.get();
                let methods: Vec<ArtifactDataDefSetMethod> = dt.methods.borrow().iter().map(|m| {
                    let compiled_js = m.compiled_js.borrow();
                    let compiled_js = compiled_js.as_ref().expect("Method missing compiled JS");
                    let ret_type = m.ret_type.borrow().clone().expect("Method missing ret type");

                    // `arg_type == None` is the conventional "method takes null / no argument" form
                    // (see `AspectField::field` use-site at type_source_data_type/dynamic.rs which
                    // defaults to `TypeNullDynamic`). Serialize that as the null dynamic type.
                    let arg_type: Rc<dyn Type> = m.arg_type.borrow().clone().unwrap_or_else(|| {
                        Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic)
                    });
                    let captured_externals = serialize_captured_externals(xx, loc, &m.captured_externals.borrow());
                    let entry_id = match m.scope_entry_id.get() {
                        Some(crate::langhelp::ScopeEntryId::Def(_, _, eid)) => eid,
                        _ => panic!("Method scope_entry_id missing or not Def"),
                    };
                    ArtifactDataDefSetMethod {
                        name: m.name.clone(),
                        entry_id,
                        field_name: m.field_name.clone(),
                        arg_type: serialize_type_ref(xx, loc, arg_type.as_ref()),
                        ret_type: serialize_type_ref(xx, loc, ret_type.as_ref()),
                        compiled_js: compiled_js.clone(),
                        captured_externals,
                    }
                }).collect();
                let interface_impls: Vec<ArtifactDataDefSetInterfaceImpl> =
                    dt.interface_impls.borrow().iter().map(|impl_data| {
                        let builtin_kind = impl_data.interface.builtin_kind;
                        let method_impls: Vec<ArtifactDataDefSetInterfaceMethodImpl> =
                            impl_data.method_impls.iter().map(|m| {
                                let compiled_js = m.compiled_js.borrow();
                                let compiled_js =
                                    compiled_js.as_ref().expect("Interface method impl missing compiled JS");
                                let entry_id =
                                    match m
                                        .scope_entry_id
                                        .get()
                                        .expect("Interface method impl missing scope_entry_id") {
                                        crate::langhelp::ScopeEntryId::Def(_, _, id) => id,
                                        other => panic!(
                                            "Interface method impl has non-Def scope_entry_id: {:?}",
                                            other,
                                        ),
                                    };

                                // Lazily serialize `impl_expr` here — every method/impl's `compiled_js` is set by
                                // the time we reach this point (we're inside `to_serial`, which only runs once
                                // all of eval()'s compilation is done). Doing it here avoids the
                                // order-of-operations problem of serializing impl bodies during the compilation
                                // loop: an impl body's AST may contain `lower(some_type)` literals whose
                                // `type_to_serial` recursively walks back into `serialize_definition_set`, and
                                // that recursion needs every sibling impl's `compiled_js` to already exist.
                                //
                                // Fallback placeholder for the rare case the body genuinely can't be artifactised
                                // (Literal whose value type lacks AspectArtifact, etc.). Same-isolate dispatch
                                // uses `compiled_js`, so the placeholder only matters for cross-module import —
                                // and in that case the placeholder gives a deterministic, debuggable error.
                                if m.impl_expr_artifact.borrow().is_none() {
                                    let impl_expr_value = m.impl_expr.borrow().clone();
                                    if let Some(impl_expr_value) = impl_expr_value {
                                        let impl_expr_ast =
                                            crate::invariant::must_downcast_ref::<crate::eval::expr::Expr>(
                                                impl_expr_value.as_ref(),
                                            );
                                        if let Ok(serialized) =
                                            crate::eval::expr_artifact::expr_to_artifact(xx, impl_expr_ast) {
                                            *m.impl_expr_artifact.borrow_mut() = Some(serialized);
                                        }
                                    }
                                }
                                let impl_expr = m.impl_expr_artifact.borrow().clone().unwrap_or_else(|| {
                                    crate::eval::expr_artifact::ArtifactExpr {
                                        loc: crate::eval::expr_artifact::ArtifactQExprId(0, 0),
                                        r#type: Box::new(
                                            crate::eval::expr_artifact::ArtifactExpr_::Literal {
                                                value: crate
                                                ::eval
                                                ::value
                                                ::type_null
                                                ::r#const
                                                ::TypeNullConst
                                                ::build_artifact(
                                                    (),
                                                ),
                                            },
                                        ),
                                    }
                                });
                                let captured_externals =
                                    serialize_captured_externals(xx, loc, &m.captured_externals.borrow());
                                ArtifactDataDefSetInterfaceMethodImpl {
                                    interface_method_index: m.interface_method_index,
                                    entry_id,
                                    compiled_js: compiled_js.clone(),
                                    impl_expr,
                                    captured_externals,
                                }
                            }).collect();
                        let user_iface = match builtin_kind {
                            Some(_) => None,
                            None => {
                                let iface = &impl_data.interface;
                                let iface_methods = iface.methods.borrow().iter().map(|m| {
                                    let arg_type =
                                        m.arg_type.borrow().clone().expect("iface method missing arg_type");
                                    let ret_type =
                                        m.ret_type.borrow().clone().expect("iface method missing ret_type");
                                    crate
                                    ::eval
                                    ::value
                                    ::type_source_data_type
                                    ::artifact
                                    ::ArtifactDataDefSetInterfaceMethod {
                                        name: m.name.clone(),
                                        field_name: m.field_name.clone(),
                                        arg_type: serialize_type_ref(xx, loc, arg_type.as_ref()),
                                        ret_type: serialize_type_ref(xx, loc, ret_type.as_ref()),
                                    }
                                }).collect();
                                Some(crate::eval::value::type_source_data_type::artifact::ArtifactDataDefSetInterface {
                                    identity: iface.identity.clone(),
                                    description: iface.description.borrow().clone(),
                                    is_aspect: iface.is_aspect.get(),
                                    methods: iface_methods,
                                })
                            },
                        };
                        ArtifactDataDefSetInterfaceImpl {
                            builtin_kind,
                            user_iface,
                            method_impls,
                        }
                    }).collect();
                let serial = ArtifactDataDefSetDataType {
                    name: dt.name.clone(),
                    data_field: dt.data_field.clone(),
                    underlying_type: serialize_type_ref(xx, loc, underlying_type.as_ref()),
                    identity: dt.identity.clone(),
                    methods: methods,
                    interface_impls,
                    def_set: def_set_artifact_id.clone(),
                };
                TypeTypeSourceDataTypeDynamic::build_artifact(
                    serde_json::to_value(&serial).expect("Failed to serialize data type"),
                )
            })
        }).collect();
        let functions = set.functions.borrow();
        let function_ids: Vec<crate::langhelp::ArtifactId> = functions.iter().map(|f| {
            crate::langhelp::produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &f.artifact_id, || {
                let arg_type = f.arg_type.borrow().clone().expect("Function missing arg type");
                let ret_type = f.ret_type.borrow().clone().expect("Function missing ret type");
                let compiled_js = f.compiled_js.borrow().clone().expect("Function missing compiled JS");
                let entry_id = match f.scope_entry_id.get().expect("Function missing scope_entry_id") {
                    crate::langhelp::ScopeEntryId::Def(_, _, id) => id,
                    other => panic!("Function has non-Def scope_entry_id: {:?}", other),
                };
                let captured_externals = serialize_captured_externals(xx, loc, &f.captured_externals.borrow());
                let serial = crate::eval::value::type_source_data_type::artifact::ArtifactDataDefSetFunction {
                    name: f.name.clone(),
                    entry_id,
                    arg_type: serialize_type_ref(xx, loc, arg_type.as_ref()),
                    ret_type: serialize_type_ref(xx, loc, ret_type.as_ref()),
                    compiled_js,
                    def_set: def_set_artifact_id.clone(),
                    captured_externals,
                };
                crate::eval::value::type_source_function::dynamic::TypeTypeSourceFunctionDynamic::build_artifact(
                    serde_json::to_value(&serial).expect("Failed to serialize function"),
                )
            })
        }).collect();
        drop(functions);
        let serial = ArtifactDataDefSet {
            data_types: data_type_ids,
            functions: function_ids,
            import_id: set.import_id,
            def_set_id: set.def_set_id,
            source_id: set.source_id,
        };
        crate::langhelp::Artifact {
            type_: crate::langhelp::ArtifactId::Builtin(defset_artifact_identity()),
            data: serde_json::to_value(&serial).expect("Failed to serialize definition set"),
        }
    })
}
struct TypeDefinitionSetLiftDynamic;

impl Aspect for TypeDefinitionSetLiftDynamic { }

impl crate::r#gen::AspectLiftDynamic for TypeDefinitionSetLiftDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let obj =
            v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for definition set lift")?;
        let dk = v8::String::new(scope, "_d").unwrap();
        let ext = obj.get(scope, dk.into()).ok_or("Missing _d on definition set object")?;
        let ds: Gc<DefinitionSet> =
            crate::eval::from_v8_ref::<Rc<crate::r#gen::DefinitionSetBuilder>>(&*ext).inner.clone();
        ds.finish()?;
        let exports_type: Rc<dyn crate::langhelp::Type> =
            Rc::new(crate::eval::value::type_source_exports::r#const::TypeSourceExportsLiftedConst { set: ds });
        Ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner: exports_type }))
    }
}

impl crate::r#gen::AspectLowerDynamic for TypeDefinitionSetLiftDynamic {
    fn lower(
        &self,
        _xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        // Same as `lift` but does NOT finalize the def-set.
        let obj = v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for definition set lower")?;
        let dk = v8::String::new(scope, "_d").unwrap();
        let ext = obj.get(scope, dk.into()).ok_or("Missing _d on definition set object")?;
        let ds: Gc<DefinitionSet> =
            crate::eval::from_v8_ref::<Rc<crate::r#gen::DefinitionSetBuilder>>(&*ext).inner.clone();
        let exports_type: Rc<dyn crate::langhelp::Type> =
            Rc::new(crate::eval::value::type_source_exports::r#const::TypeSourceExportsLiftedConst { set: ds });
        Ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner: exports_type }))
    }
}
