use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::{
            Erb,
            TargetBindSub,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            dynamic_wrap::{
                DynamicWrap,
                dynamic_wrap_method_name,
            },
            value::{
                tuple::Tuple,
                type_any::r#const::TypeAnyConst,
                type_function::dynamic::TypeFunctionDynamic,
                type_source_data_type::{
                    SourceDataTypeData,
                    SourceDataTypeRef,
                },
                type_source_function::{
                    SourceFunctionData,
                    TypeSourceFunctionConst,
                },
                type_source_interface::{
                    SourceInterfaceData,
                    SourceInterfaceRef,
                },
                type_source_new_data_result::dynamic::SourceNewDataResult,
                type_source_new_func_result::dynamic::SourceNewFuncResult,
                type_source_new_interface_result::dynamic::SourceNewInterfaceResult,
                type_source_tagged_union::SourceTaggedUnionData,
                type_source_tagged_union_builder::dynamic::SourceTaggedUnionBuilder,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntry,
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
        mem,
        rc::Rc,
    },
};

dynamic_wrap!(wrap Gc < DefinitionSet > as DefinitionSet(self_, scope) {
    fn new_func(name: String) -> SourceNewFuncResult {
        let ds = (*self_).clone();
        let entry_id = ds.next_entry_id.next();
        let func_data = Gc::new(SourceFunctionData {
            set: ds.clone(),
            name: Some(name),
            description: RefCell::new(None),
            impl_expr: RefCell::new(None),
            arg_type: RefCell::new(None),
            ret_type: RefCell::new(None),
            scope_entry_id: Cell::new(
                Some(crate::langhelp::ScopeEntryId::Def(ds.import_id, ds.def_set_id, entry_id,))
            ),
            artifact_id: RefCell::new(None),
            compiled_js: RefCell::new(None),
            captured_externals: RefCell::new(Vec::new()),
        });
        ds.functions.borrow_mut().push(func_data.clone());
        let func_ref = Rc::new(TypeSourceFunctionConst { data: func_data, });
        SourceNewFuncResult { func_ref }
    }

    fn new_data(name: String) -> SourceNewDataResult {
        let ds = (*self_).clone();
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
        let data_field = format!("_{}", ds.next_field_id.next());
        let dt_data = Gc::new(SourceDataTypeData {
            set: ds.clone(),
            name: Some(name),
            description: RefCell::new(None),
            underlying_type: crate::invariant::DeserialLazy::new(),
            methods: RefCell::new(Vec::new()),
            interface_impls: RefCell::new(Vec::new()),
            identity: identity,
            data_field: data_field,
            artifact_id: RefCell::new(None),
        });
        ds.data_types.borrow_mut().push(dt_data.clone());
        let data_type_ref = Rc::new(SourceDataTypeRef { data: dt_data, });
        SourceNewDataResult { data_type_ref }
    }

    fn build() -> Gc<DefinitionSet> {
        (*self_).clone()
    }

    fn tagged_union() -> SourceTaggedUnionBuilder {
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
        let data = Rc::new(SourceTaggedUnionData {
            variants: RefCell::new(Vec::new()),
            identity: identity,
            next_tag_id: crate::langhelp::Counter::default(),
        });
        SourceTaggedUnionBuilder { data }
    }

    fn new_interface() -> SourceNewInterfaceResult {
        let ds = (*self_).clone();
        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
        let iface_data = Gc::new(SourceInterfaceData {
            builtin_kind: None,
            set: ds.clone(),
            description: RefCell::new(None),
            methods: RefCell::new(Vec::new()),
            identity: identity,
        });
        ds.interfaces.borrow_mut().push(iface_data.clone());
        let interface_ref = Rc::new(SourceInterfaceRef { data: iface_data, });
        SourceNewInterfaceResult { interface_ref }
    }
});

fn definition_set_extra_aspect(_xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
    if *key == crate::builtin::aspect_lift_dynamic_aspect_id() {
        return Some(
            Box::new(Box::new(TypeDefinitionSetLiftDynamic) as Box<dyn crate::builtin::AspectLiftDynamic>),
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
    /// Source id of the compile that created this definition set. Imports use
    /// this together with `import_id` to look up the per-compile IMPORT_STAGE_DATA
    /// when rebuilding instance objects in the importer's isolate.
    pub source_id: crate::langhelp::SourceId,
}

impl DefinitionSet {
    pub fn finish(&self) -> Result<(), String> {
        if self.finished.get() {
            return Ok(());
        }
        let functions = self.functions.borrow();
        for func_data in functions.iter() {
            let name = func_data.name.as_deref().unwrap_or("(unnamed)");
            if func_data.impl_expr.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing an implementation", name));
            }
            if func_data.arg_type.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing an arg type", name));
            }
            if func_data.ret_type.borrow().is_none() {
                return Err(format!("Definition set function '{}' is missing a return type", name));
            }
        }
        let data_types = self.data_types.borrow();
        for dt_data in data_types.iter() {
            let name = dt_data.name.as_deref().unwrap_or("(unnamed)");
            for method in dt_data.methods.borrow().iter() {
                if method.impl_expr.borrow().is_none() {
                    return Err(format!("Data type '{}' method '{}' is missing implementation", name, method.name));
                }
                if method.ret_type.borrow().is_none() {
                    return Err(format!("Data type '{}' method '{}' is missing a return type", name, method.name));
                }
            }
        }
        let interfaces = self.interfaces.borrow();
        for (i, iface) in interfaces.iter().enumerate() {
            let iface_name = iface.description.borrow().clone().unwrap_or_else(|| format!("(interface {})", i));
            for method in iface.methods.borrow().iter() {
                if method.arg_type.borrow().is_none() {
                    return Err(
                        format!("Interface '{}' method '{}' is missing an arg type", iface_name, method.name),
                    );
                }
                if method.ret_type.borrow().is_none() {
                    return Err(
                        format!("Interface '{}' method '{}' is missing a return type", iface_name, method.name),
                    );
                }
            }
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
    builtin_artifact_id!()
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
    externals
        .iter()
        .map(|(id, ext)| {
            let local_id = match id {
                crate::langhelp::ScopeEntryId::Local(n) => *n,
                other => panic!("captured external has non-Local scope entry id: {:?}", other),
            };
            match ext {
                crate::langhelp::External::StagedExprCell(cell) => {
                    let value =
                        cell.value.borrow().clone().expect("captured StagedExprCell already consumed");
                    let expr_ref = crate::invariant::must_downcast_ref::<crate::eval::expr::Expr>(value.as_ref());
                    let expr = crate::eval::expr_artifact::expr_to_artifact(xx, expr_ref)
                        .expect("Failed to serialize captured StagedExprCell value");
                    ArtifactExternal::StagedExpr { local_id, expr }
                },
                crate::langhelp::External::StageLiftCell(_cell) => {
                    // The lift's source type isn't directly recoverable from
                    // the `Box<dyn AspectLiftDynamic>`, so for now we punt;
                    // round-trip support for `StageLiftCell` needs the source
                    // type carried alongside the lift on the cell.
                    panic!(
                        "captured StageLiftCell at Local({}) — round-trip not yet implemented",
                        local_id,
                    )
                },
                crate::langhelp::External::Type(type_) => ArtifactExternal::Type {
                    local_id,
                    type_artifact:
                        crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                            xx,
                            loc,
                            type_.as_ref(),
                        ),
                },
            }
        })
        .collect()
}

/// Reconstruct a body's captured `External`s from `ArtifactExternal`s. Used
/// after cross-module import so the body's `compiled_js` finds its closure
/// bindings populated at v8 setup. The reconstructed `Local` ids match the
/// body's original local-id numbering.
pub(crate) fn deserialize_captured_externals(
    xx: &ExprContext,
    loc: QExprId,
    serial: &[crate::eval::value::type_source_data_type::artifact::ArtifactExternal],
) -> Vec<(crate::langhelp::ScopeEntryId, crate::langhelp::External)> {
    use crate::eval::value::type_source_data_type::artifact::ArtifactExternal;
    serial
        .iter()
        .map(|ext| {
            match ext {
                ArtifactExternal::StagedExpr { local_id, expr } => {
                    let expr = crate::eval::expr_artifact::expr_from_artifact(xx, expr)
                        .expect("Failed to deserialize captured StagedExprCell expr");
                    let cell = Rc::new(crate::eval::expr::StagedExprCell {
                        value: RefCell::new(Some(Rc::new(expr) as Rc<dyn crate::langhelp::Value>)),
                    });
                    (
                        crate::langhelp::ScopeEntryId::Local(*local_id),
                        crate::langhelp::External::StagedExprCell(cell),
                    )
                },
                ArtifactExternal::StageLift { local_id, .. } => {
                    panic!(
                        "captured StageLiftCell at Local({}) — round-trip not yet implemented",
                        local_id,
                    )
                },
                ArtifactExternal::Type { local_id, type_artifact } => {
                    let (type_, _code) =
                        crate::eval::util::deserialize_type_artifact(xx, loc, type_artifact)
                            .into_res()
                            .expect("Failed to deserialize captured External::Type");
                    (
                        crate::langhelp::ScopeEntryId::Local(*local_id),
                        crate::langhelp::External::Type(type_),
                    )
                },
            }
        })
        .collect()
}

/// Single point of entry for deserializing a `DefinitionSet` and all its
/// data types and functions. Implements the two-phase pattern:
/// 1. Build empty `Gc<DefinitionSet>` shell, build all data-type and function
///    shells, register each in `deserialized_artifacts`, attach to
///    `set.data_types` / `set.functions`. No cross-artifact reads.
/// 2. Register a single finalizer that resolves cross-artifact references
///    (data type's underlying_type / method ret types / interface_impls;
///    function arg_type / ret_type) by reading other shells out of the cache.
pub(crate) fn load_def_set(
    xx: &ExprContext,
    def_set_artifact_id: &crate::langhelp::ArtifactId,
) -> Result<Gc<DefinitionSet>, String> {
    use {
        crate::eval::value::{
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
        crate::invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
    };
    crate::langhelp::deserialize_independent_artifact(&xx.vx.ix, def_set_artifact_id, || {
        let def_set_artifact = xx.vx.ix.gx.artifacts.get(def_set_artifact_id.clone())?;
        let def_set_serial: ArtifactDataDefSet =
            serde_json::from_value(def_set_artifact.data)
                .map_err(|e| format!("Failed to parse definition set: {}", e))?;
        let import_id = def_set_serial.import_id;
        let def_set_id = def_set_serial.def_set_id;
        let set =
            Gc::new(DefinitionSet::new(import_id, def_set_id, def_set_serial.source_id));
        let mut dt_serials: Vec<(crate::langhelp::ArtifactId, ArtifactDataDefSetDataType)> = Vec::new();
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
                artifact_id: RefCell::new(Some(dt_artifact_id.clone())),
            });
            xx
                .vx
                .ix
                .deserialized_artifacts
                .borrow_mut()
                .insert(dt_artifact_id.clone(), Box::new(dt_data.clone()));
            dt_gcs.push(dt_data);
            dt_serials.push((dt_artifact_id.clone(), dt_serial));
        }
        *set.data_types.borrow_mut() = dt_gcs;
        let mut func_serials: Vec<(crate::langhelp::ArtifactId, ArtifactDataDefSetFunction)> = Vec::new();
        let mut func_gcs: Vec<Gc<SourceFunctionData>> = Vec::new();
        for func_artifact_id in &def_set_serial.functions {
            let func_artifact = xx.vx.ix.gx.artifacts.get(func_artifact_id.clone())?;
            let func_serial: ArtifactDataDefSetFunction =
                serde_json::from_value(func_artifact.data)
                    .map_err(|e| format!("Failed to parse function: {}", e))?;
            let func_data = Gc::new(SourceFunctionData {
                set: set.clone(),
                name: func_serial.name.clone(),
                description: RefCell::new(None),
                impl_expr: RefCell::new(None),
                arg_type: RefCell::new(None),
                ret_type: RefCell::new(None),
                scope_entry_id: Cell::new(Some(crate::langhelp::ScopeEntryId::Def(
                    import_id,
                    def_set_id,
                    func_serial.entry_id,
                ))),
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
        let finalizer: crate::langhelp::ArtifactFinalizer = Box::new(move |xx: &ExprContext, loc: QExprId| {
            let mut codes: Vec<Code> = Vec::new();
            for (dt_artifact_id, dt_serial) in &dt_serials {
                let dt_data = {
                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                    must_downcast_ref::<Gc<SourceDataTypeData>>(cache.get(dt_artifact_id).unwrap().as_ref()).clone()
                };
                let (underlying_type, underlying_code) =
                    crate::eval::util::deserialize_type_artifact(xx, loc, &dt_serial.underlying_type)
                        .into_res()
                        .map_err(|e| format!("Failed to deserialize underlying type: {:?}", e))?;
                codes.push(underlying_code);
                dt_data.underlying_type.set(underlying_type);
                let methods: Vec<SourceDataMethodData> = dt_serial.methods.iter().enumerate().map(|(i, m)| {
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
                    let captured_externals =
                        deserialize_captured_externals(xx, loc, &m.captured_externals);
                    SourceDataMethodData {
                        name: m.name.clone(),
                        arg_type: RefCell::new(Some(arg_type)),
                        impl_expr: RefCell::new(None),
                        ret_type: RefCell::new(Some(ret_type)),
                        scope_entry_id: Cell::new(Some(crate::langhelp::ScopeEntryId::Def(
                            import_id,
                            def_set_id,
                            i,
                        ))),
                        self_scope_entry_id: Cell::new(None),
                        field_name: m.field_name.clone(),
                        compiled_js: RefCell::new(Some(m.compiled_js.clone())),
                        captured_externals: RefCell::new(captured_externals),
                    }
                }).collect();
                *dt_data.methods.borrow_mut() = methods;
                let builtin_ifaces =
                    crate::eval::value::type_source_interface::build_builtin_interfaces(import_id);
                let interface_impls: Vec<SourceDataTypeInterfaceImplData> =
                    dt_serial.interface_impls.iter().map(|impl_serial| {
                        let iface = match impl_serial.builtin_kind {
                            BuiltinInterfaceKind::Value => builtin_ifaces.value.clone(),
                            BuiltinInterfaceKind::Type => builtin_ifaces.type_.clone(),
                            BuiltinInterfaceKind::AspectFunction => builtin_ifaces.aspect_function.clone(),
                            BuiltinInterfaceKind::AspectField => builtin_ifaces.aspect_field.clone(),
                            BuiltinInterfaceKind::ScopeEntry => builtin_ifaces.scope_entry.clone(),
                            BuiltinInterfaceKind::Target => builtin_ifaces.target.clone(),
                            BuiltinInterfaceKind::CodeDyn => builtin_ifaces.code_dyn.clone(),
                        };
                        let method_impls = impl_serial.method_impls.iter().map(|m| {
                            let impl_expr =
                                crate::eval::expr_artifact::expr_from_artifact(xx, &m.impl_expr)
                                    .expect("Failed to deserialize interface method impl_expr");
                            let captured_externals =
                                deserialize_captured_externals(xx, loc, &m.captured_externals);
                            SourceDataTypeInterfaceMethodImplData {
                                interface_method_index: m.interface_method_index,
                                impl_expr: RefCell::new(
                                    Some(Rc::new(impl_expr) as Rc<dyn crate::langhelp::Value>),
                                ),
                                impl_expr_artifact: RefCell::new(Some(m.impl_expr.clone())),
                                compiled_js: RefCell::new(Some(m.compiled_js.clone())),
                                captured_externals: RefCell::new(captured_externals),
                                scope_entry_id: Cell::new(Some(crate::langhelp::ScopeEntryId::Def(
                                    import_id,
                                    def_set_id,
                                    m.entry_id,
                                ))),
                                self_scope_entry_id: Cell::new(None),
                            }
                        }).collect();
                        SourceDataTypeInterfaceImplData { interface: iface, method_impls }
                    }).collect();
                *dt_data.interface_impls.borrow_mut() = interface_impls;
            }
            for (func_artifact_id, func_serial) in &func_serials {
                let func_data = {
                    let cache = xx.vx.ix.deserialized_artifacts.borrow();
                    must_downcast_ref::<Gc<SourceFunctionData>>(cache.get(func_artifact_id).unwrap().as_ref()).clone()
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
                    // `arg_type == None` is the conventional "method takes null /
                    // no argument" form (see `AspectField::field` use-site at
                    // type_source_data_type/dynamic.rs which defaults to
                    // `TypeNullDynamic`). Serialize that as the null mortar type.
                    let arg_type: Rc<dyn Type> = m.arg_type.borrow().clone().unwrap_or_else(|| {
                        Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic)
                    });
                    let captured_externals =
                        serialize_captured_externals(xx, loc, &m.captured_externals.borrow());
                    ArtifactDataDefSetMethod {
                        name: m.name.clone(),
                        field_name: m.field_name.clone(),
                        arg_type: serialize_type_ref(xx, loc, arg_type.as_ref()),
                        ret_type: serialize_type_ref(xx, loc, ret_type.as_ref()),
                        compiled_js: compiled_js.clone(),
                        captured_externals,
                    }
                }).collect();
                let interface_impls: Vec<ArtifactDataDefSetInterfaceImpl> =
                    dt.interface_impls.borrow().iter().filter_map(|impl_data| {
                        let builtin_kind = impl_data.interface.builtin_kind?;
                        let method_impls: Vec<ArtifactDataDefSetInterfaceMethodImpl> =
                            impl_data.method_impls.iter().map(|m| {
                                let compiled_js = m.compiled_js.borrow();
                                let compiled_js =
                                    compiled_js.as_ref().expect("Interface method impl missing compiled JS");
                                let entry_id = match m
                                    .scope_entry_id
                                    .get()
                                    .expect("Interface method impl missing scope_entry_id") {
                                    crate::langhelp::ScopeEntryId::Def(_, _, id) => id,
                                    other => panic!(
                                        "Interface method impl has non-Def scope_entry_id: {:?}",
                                        other,
                                    ),
                                };
                                // Falls back to a placeholder when
                                // pre-serialization couldn't capture the body
                                // (e.g. a Literal whose value type lacks
                                // AspectArtifact). The placeholder won't
                                // const_eval correctly cross-isolate, but
                                // same-isolate runtime uses `compiled_js`.
                                let impl_expr = m.impl_expr_artifact.borrow().clone().unwrap_or_else(|| {
                                    crate::eval::expr_artifact::ArtifactExpr {
                                        loc: crate::eval::expr_artifact::ArtifactQExprId(0, 0),
                                        r#type: Box::new(
                                            crate::eval::expr_artifact::ArtifactExpr_::Literal {
                                                value:
                                                    crate::eval::value::type_null::r#const::TypeNullConst::build_artifact(
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
                        Some(ArtifactDataDefSetInterfaceImpl {
                            builtin_kind,
                            method_impls,
                        })
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
                let captured_externals =
                    serialize_captured_externals(xx, loc, &f.captured_externals.borrow());
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

impl crate::builtin::AspectLiftDynamic for TypeDefinitionSetLiftDynamic {
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
        let ds: Gc<DefinitionSet> = crate::eval::from_v8_ref::<Rc<Gc<DefinitionSet>>>(&*ext).as_ref().clone();
        ds.finish()?;
        Ok(Rc::new(crate::eval::value::type_source_exports::r#const::TypeSourceExportsLiftedConst {
            set: ds,
        }))
    }
}
