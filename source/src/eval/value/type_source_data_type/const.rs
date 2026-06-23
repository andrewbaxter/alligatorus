use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactId,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            deserialize_artifact,
            impl_deserialize_independent_artifact,
            impl_serialize_independent_artifact,
            serialize_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            definition_set::serialize_definition_set,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            import_install::install_imported_type_iface,
            value::{
                type_pull::TypePull,
                type_root::r#const::ExporterRootDelegate,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_interface::{
                    BuiltinInterfaceKind,
                    SourceInterfaceData,
                    r#const::TypeSourceInterfaceConst,
                },
                type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                value_null::r#const::ValueNullConst,
                value_source_data_type::r#const::ValueSourceDataTypeConst,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectArtifact,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            AspectType,
            ScopeEntry,
            aspect_artifact_aspect_id,
            aspect_artifact_get_maybe,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_lift_dynamic_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get_maybe,
            aspect_type_aspect_id,
            type_type_identity,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
};

fn read_artifact_has_type_iface(xx: &ExprContext, dt_data: &dumpster::unsync::Gc<super::SourceDataTypeData>) -> bool {
    let id = match dt_data.artifact_id.borrow().clone() {
        Some(id) => id,
        None => {
            return dt_data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
            });
        },
    };
    let art = match xx.vx.ix.gx.artifacts.get(id) {
        Ok(a) => a,
        Err(_) => return false,
    };
    let dt_serial: super::artifact::ArtifactDataDefSetDataType = match serde_json::from_value(art.data) {
        Ok(s) => s,
        Err(_) => return false,
    };
    dt_serial.members.iter().any(|m_id| {
        let m_art = match xx.vx.ix.gx.artifacts.get(m_id.clone()) {
            Ok(a) => a,
            Err(_) => return false,
        };
        let is_iface_impl =
            matches!(
                &m_art.exporter,
                crate:: eval:: artifact:: ArtifactId:: Builtin(
                    b
                ) if * b == crate:: r#gen:: defset_iface_impl_artifact_id()
            );
        if !is_iface_impl {
            return false;
        }
        let impl_serial: super::artifact::ArtifactDataDefSetInterfaceImpl = match serde_json::from_value(m_art.data) {
            Ok(s) => s,
            Err(_) => return false,
        };
        impl_serial.builtin_kind == Some(BuiltinInterfaceKind::Type)
    })
}

pub(crate) struct TypeSourceDataTypeConst {
    pub data: dumpster::unsync::Gc<super::SourceDataTypeData>,
}

impl TypeSourceDataTypeConst {
    pub(crate) fn serialize_data_type(&self, xx: &ExprContext, loc: QExprId) -> ArtifactId {
        serialize_definition_set(xx, loc, &self.data.set);
        self
            .data
            .artifact_id
            .borrow()
            .clone()
            .expect("Data type artifact ID should be set by serialize_definition_set")
    }
}

impl Aspect for TypeSourceDataTypeConst { }

impl AspectArtifact for TypeSourceDataTypeConst {
}

impl AspectBind for TypeSourceDataTypeConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeConst {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceDataTypeConst {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceDataTypeConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }),
        })
    }
}

impl AspectLiftDynamic for TypeSourceDataTypeConst {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let underlying_type = self.data.underlying_type.get().clone();
        let underlying_lift =
            aspect_lift_dynamic_get_maybe(
                xx,
                underlying_type.as_ref(),
            ).ok_or_else(|| format!("Underlying type ({}) missing AspectLiftDynamic", underlying_type.desc()))?;
        let obj = v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lift")?;
        let data_field = ConstKey::String(self.data.data_field.clone()).to_str();
        let key = v8::String::new(scope, &data_field).unwrap();
        let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
        let underlying_value = underlying_lift.lift(xx, scope, underlying_v8)?;
        let has_type_iface = self.data.interface_impls.borrow().iter().any(|impl_data| {
            impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
        });
        let isolate_bundle = if has_type_iface {
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, obj);
            std::cell::RefCell::new(Some(instance_global))
        } else {
            std::cell::RefCell::new(None)
        };
        Ok(Rc::new(ValueSourceDataTypeConst {
            underlying: underlying_value,
            data: self.data.clone(),
            isolate_bundle,
        }))
    }
}

impl AspectStackValueDynamic for TypeSourceDataTypeConst {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() }),
            }),
        }
    }
}

impl ExporterAny for TypeSourceDataTypeConst {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Type for TypeSourceDataTypeConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeConst { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectLiftDynamic>,
                ),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectArtifact>),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }
        if *key == aspect_field_aspect_id() {
            return Some(
                Box::new(
                    Box::new(super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() }) as
                        Box<dyn AspectField>,
                ),
            );
        }
        {
            let interface_impls = self.data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if *key == impl_data.interface.identity {
                    return Some(Box::new(true));
                }
            }
            if *key == aspect_type_aspect_id() {
                let has_type_impl = interface_impls.iter().any(|impl_data| {
                    impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
                });
                if has_type_impl {
                    return Some(
                        Box::new(
                            Box::new(TypeSourceDataTypeConstAspectType { data: self.data.clone() }) as
                                Box<dyn AspectType>,
                        ),
                    );
                }
            }
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        let (own_identity, iface_identities) = {
            let own = self.data.identity.clone();
            let interface_impls = self.data.interface_impls.borrow();
            let ifaces: Vec<Unique> = interface_impls.iter().map(|impl_data| {
                impl_data.interface.identity.clone()
            }).collect();
            (own, ifaces)
        };
        if type_.aspect(xx, &own_identity).is_some() {
            return true;
        }
        for identity in iface_identities {
            if type_.aspect(xx, &identity).is_some() {
                return true;
            }
        }
        if type_.aspect(xx, &type_type_identity()).is_some() {
            return true;
        }
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        let (own_identity, iface_data) = {
            let own = self.data.identity.clone();
            let interface_impls = self.data.interface_impls.borrow();
            let ifaces: Vec<(Gc<SourceInterfaceData>, Unique)> = interface_impls.iter().map(|impl_data| {
                (impl_data.interface.clone(), impl_data.interface.identity.clone())
            }).collect();
            (own, ifaces)
        };
        if type_.aspect(xx, &own_identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }),
                }),
            });
        }
        for (iface_data, identity) in iface_data {
            if type_.aspect(xx, &identity).is_some() {
                if iface_data.builtin_kind.is_some() {
                    let dyn_cv = CodeValue {
                        code: cv.code,
                        value: Rc::new(ValueDynamic {
                            source: ValueDynamicSource::Expr,
                            type_: Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() }),
                        }),
                    };
                    let dyn_type = super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() };
                    return dyn_type.cast_to(xx, loc, dyn_cv, type_);
                }
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceInterfaceConst { data: iface_data }),
                    }),
                });
            }
        }
        if type_.aspect(xx, &type_type_identity()).is_some() {
            let dyn_cv = CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() }),
                }),
            };
            let dyn_type = super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() };
            return dyn_type.cast_to(xx, loc, dyn_cv, type_);
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("data_type({})", self.data.name.as_deref().unwrap_or("?")),
        }
    }

}

impl Value for TypeSourceDataTypeConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeTypeSourceDataTypeDynamic))
    }
}

#[derive(serde::Serialize, serde::Deserialize)]
struct ArtifactNewSourceDataMethod {
    name: String,
    field_name: String,
    arg_type: Artifact,
    ret_type: Artifact,
    compiled_js: String,
    scope_entry_id: usize,
}

#[derive(serde::Serialize, serde::Deserialize)]
struct ArtifactNewSourceInterfaceMethodImpl {
    interface_method_index: usize,
    compiled_js: String,
    scope_entry_id: usize,
}

#[derive(serde::Serialize, serde::Deserialize)]
struct ArtifactNewSourceInterfaceImpl {
    interface: Artifact,
    method_impls: Vec<ArtifactNewSourceInterfaceMethodImpl>,
}

#[derive(serde::Serialize, serde::Deserialize)]
struct ArtifactNewSourceDataType {
    name: Option<String>,
    identity: Unique,
    data_field: String,
    def_set_id: crate::langhelp::DefSetId,
    def_set: Artifact,
    underlying_type: Artifact,
    methods: Vec<ArtifactNewSourceDataMethod>,
    interface_impls: Vec<ArtifactNewSourceInterfaceImpl>,
}

impl ExporterRootDelegate for TypeSourceDataTypeConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        let Some(id) = id else {
            return EResult::err(loc, "TypeSourceDataTypeConst requires an artifact id to deserialize".to_string());
        };
        let id_owned = id.clone();
        let res =
            impl_deserialize_independent_artifact::<
                ArtifactNewSourceDataType,
            >(xx, loc, id, data, move |body: ArtifactNewSourceDataType| {
                let set =
                    dumpster::unsync::Gc::new(crate::eval::definition_set::DefinitionSet::new(body.def_set_id));
                let dt_data = dumpster::unsync::Gc::new(super::SourceDataTypeData {
                    artifact_id: RefCell::new(None),
                    new_artifact_id: RefCell::new(Some(id_owned.clone())),
                    set: set,
                    name: body.name.clone(),
                    identity: body.identity.clone(),
                    data_field: body.data_field.clone(),
                    description: RefCell::new(None),
                    underlying_type: crate::invariant::DeserialLazy::new(),
                    methods: RefCell::new(Vec::new()),
                    interface_impls: RefCell::new(Vec::new()),
                });
                let dt_for_finalizer = dt_data.clone();
                let underlying_art = body.underlying_type.clone();
                let methods_serial = body.methods;
                let iface_impls_serial = body.interface_impls;
                let mut deps: Vec<Artifact> = Vec::new();
                deps.push(body.def_set);
                deps.push(underlying_art.clone());
                for m in &methods_serial {
                    deps.push(m.arg_type.clone());
                    deps.push(m.ret_type.clone());
                }
                for impl_data in &iface_impls_serial {
                    deps.push(impl_data.interface.clone());
                }
                let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(dt_data.clone()));
                let finalizer: crate::artifact::ArtifactFinalizer =
                    Box::new(move |xx: &ExprContext, loc: QExprId| {
                        if let Some(t) =
                            crate::artifact::deserialize_type_artifact(xx, loc, underlying_art).v {
                            dt_for_finalizer.underlying_type.set(t);
                        }
                        let def_set_id = dt_for_finalizer.set.def_set_id;
                        let methods: Vec<Rc<super::SourceDataMethodData>> = methods_serial.into_iter().map(|m| {
                            let arg_type = crate::artifact::deserialize_type_artifact(xx, loc, m.arg_type).v;
                            let ret_type = crate::artifact::deserialize_type_artifact(xx, loc, m.ret_type).v;
                            Rc::new(super::SourceDataMethodData {
                                artifact_id: RefCell::new(None),
                                name: m.name,
                                field_name: m.field_name,
                                arg_type: RefCell::new(arg_type),
                                ret_type: RefCell::new(ret_type),
                                impl_expr: RefCell::new(None),
                                compiled_js: RefCell::new(Some(m.compiled_js)),
                                captured_externals: RefCell::new(Vec::new()),
                                scope_entry_id: std::cell::Cell::new(
                                    Some(ScopeEntryId::Def(def_set_id, m.scope_entry_id)),
                                ),
                                inner_scope_entry_id: std::cell::Cell::new(None),
                            })
                        }).collect();
                        *dt_for_finalizer.methods.borrow_mut() = methods;
                        let iface_impls: Vec<super::SourceDataTypeInterfaceImplData> =
                            iface_impls_serial.into_iter().filter_map(|impl_data| {
                                let iface_any =
                                    crate::artifact::deserialize_artifact(xx, loc, impl_data.interface).v?;
                                let iface_data =
                                    iface_any
                                        .downcast::<ExporterDataAny<dumpster::unsync::Gc<super::super::type_source_interface::SourceInterfaceData>>>()
                                        .ok()?;
                                let method_impls = impl_data.method_impls.into_iter().map(|m| {
                                    super::SourceDataTypeInterfaceMethodImplData {
                                        artifact_id: RefCell::new(None),
                                        interface_method_index: m.interface_method_index,
                                        impl_expr: RefCell::new(None),
                                        impl_expr_artifact: RefCell::new(None),
                                        compiled_js: RefCell::new(Some(m.compiled_js)),
                                        captured_externals: RefCell::new(Vec::new()),
                                        scope_entry_id: std::cell::Cell::new(
                                            Some(ScopeEntryId::Def(def_set_id, m.scope_entry_id)),
                                        ),
                                        inner_scope_entry_id: std::cell::Cell::new(None),
                                    }
                                }).collect();
                                Some(super::SourceDataTypeInterfaceImplData {
                                    artifact_id: RefCell::new(None),
                                    interface: iface_data.0.clone(),
                                    method_impls,
                                })
                            }).collect();
                        *dt_for_finalizer.interface_impls.borrow_mut() = iface_impls;
                    });
                Ok((out, deps, finalizer))
            });
        let errors = res.errors;
        let Some(any) = res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(Rc::new(any) as Rc<dyn Any>),
        };
    }

    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn Any>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceDataTypeConst>().expect("expected TypeSourceDataTypeConst");
        let mut errors = None;
        let underlying_type = self_.data.underlying_type.get().clone();
        let underlying_art = serialize_artifact(xx, loc, underlying_type.as_value().as_ref());
        if let Some(errors1) = underlying_art.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(underlying_art) = underlying_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let set_const =
            super::super::type_source_exports::r#const::TypeSourceExportsLiftedConst {
                set: self_.data.set.clone(),
            };
        let def_set_art = serialize_artifact(xx, loc, &set_const as &dyn Value);
        if let Some(errors1) = def_set_art.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(def_set_art) = def_set_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let mut methods: Vec<ArtifactNewSourceDataMethod> = Vec::new();
        for m in self_.data.methods.borrow().iter() {
            let arg_type =
                m.arg_type.borrow().clone().unwrap_or_else(|| {
                    Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
                });
            let ret_type =
                m.ret_type.borrow().clone().unwrap_or_else(|| {
                    Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
                });
            let arg_art = serialize_artifact(xx, loc, arg_type.as_value().as_ref());
            if let Some(errors1) = arg_art.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            let Some(arg_art) = arg_art.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            let ret_art = serialize_artifact(xx, loc, ret_type.as_value().as_ref());
            if let Some(errors1) = ret_art.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            let Some(ret_art) = ret_art.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            let scope_idx = match m.scope_entry_id.get() {
                Some(ScopeEntryId::Def(_, idx)) => idx,
                _ => 0,
            };
            methods.push(ArtifactNewSourceDataMethod {
                name: m.name.clone(),
                field_name: m.field_name.clone(),
                arg_type: arg_art,
                ret_type: ret_art,
                compiled_js: m.compiled_js.borrow().clone().unwrap_or_default(),
                scope_entry_id: scope_idx,
            });
        }
        let mut interface_impls: Vec<ArtifactNewSourceInterfaceImpl> = Vec::new();
        for impl_data in self_.data.interface_impls.borrow().iter() {
            let iface_const = TypeSourceInterfaceConst { data: impl_data.interface.clone() };
            let iface_art = serialize_artifact(xx, loc, &iface_const as &dyn Value);
            if let Some(errors1) = iface_art.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            let Some(iface_art) = iface_art.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            let method_impls: Vec<ArtifactNewSourceInterfaceMethodImpl> =
                impl_data.method_impls.iter().map(|m| {
                    let scope_idx = match m.scope_entry_id.get() {
                        Some(ScopeEntryId::Def(_, idx)) => idx,
                        _ => 0,
                    };
                    ArtifactNewSourceInterfaceMethodImpl {
                        interface_method_index: m.interface_method_index,
                        compiled_js: m.compiled_js.borrow().clone().unwrap_or_default(),
                        scope_entry_id: scope_idx,
                    }
                }).collect();
            interface_impls.push(ArtifactNewSourceInterfaceImpl {
                interface: iface_art,
                method_impls,
            });
        }
        let art =
            impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.new_artifact_id, || {
                let body = ArtifactNewSourceDataType {
                    name: self_.data.name.clone(),
                    identity: self_.data.identity.clone(),
                    data_field: self_.data.data_field.clone(),
                    def_set_id: self_.data.set.def_set_id,
                    def_set: def_set_art,
                    underlying_type: underlying_art,
                    methods,
                    interface_impls,
                };
                make_body(serde_json::to_value(&body).unwrap())
            });
        return EResult {
            errors,
            v: Some(art),
        };
    }
}

pub(crate) struct TypeSourceDataTypeConstAspectType {
    pub data: dumpster::unsync::Gc<super::SourceDataTypeData>,
}

impl Aspect for TypeSourceDataTypeConstAspectType { }

impl AspectType for TypeSourceDataTypeConstAspectType {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let bundle =
            (value as &dyn std::any::Any)
                .downcast_ref::<ValueSourceDataTypeConst>()
                .and_then(|v| v.isolate_bundle.borrow().clone());
        Rc::new(TypePull {
            data: self.data.clone(),
            bundle,
        })
    }
}
