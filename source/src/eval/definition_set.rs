use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
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
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
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
            ScopeEntryId,
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
                    ArtifactDataDefSetMethod {
                        name: m.name.clone(),
                        field_name: m.field_name.clone(),
                        ret_type: serialize_type_ref(xx, loc, ret_type.as_ref()),
                        compiled_js: compiled_js.clone(),
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
                                let impl_expr_value =
                                    m
                                        .impl_expr
                                        .borrow()
                                        .clone()
                                        .expect("Interface method impl missing impl_expr");
                                let impl_expr_ast =
                                    crate::invariant::must_downcast_ref::<crate::eval::expr::Expr>(
                                        impl_expr_value.as_ref(),
                                    );
                                let impl_expr =
                                    crate::eval::expr_artifact::expr_to_artifact(xx, impl_expr_ast)
                                        .expect("Failed to serialize interface method impl_expr");
                                ArtifactDataDefSetInterfaceMethodImpl {
                                    interface_method_index: m.interface_method_index,
                                    entry_id,
                                    compiled_js: compiled_js.clone(),
                                    impl_expr,
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
        let serial = ArtifactDataDefSet {
            data_types: data_type_ids,
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
