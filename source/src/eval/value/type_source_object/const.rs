use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            export_artifact,
            impl_export_independent_artifact,
            impl_import_independent_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_binding::dynamic::BindingDynamicBinding,
                type_pull::r#const::TypePull,
                type_root::r#const::ExporterRootDelegate,
                type_source_interface::{
                    BuiltinInterfaceKind,
                    SourceInterfaceData,
                    r#const::TypeSourceInterfaceConst,
                },
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                value_source_object::r#const::ValueSourceObjectConst,
            },
        },
        langhelp::{
            Aspect,
            BindingId,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            QExprId,
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
            AspectLift,
            AspectPostDeserialize,
            AspectStackValueDynamic,
            AspectType,
            Binding,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_aspect_id,
            aspect_lift_get_maybe,
            aspect_post_deserialize_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
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
    super::artifact::{
        ArtifactDataDefSetInterfaceImpl,
        ArtifactDataDefSetInterfaceMethodImpl,
        ArtifactDataDefSetMethod,
        ArtifactDataDefSetObject,
    },
};

#[derive(Clone)]
pub(crate) struct TypeSourceObjectConst {
    pub data: dumpster::unsync::Gc<super::SourceObjectData>,
}

impl Aspect for TypeSourceObjectConst { }

impl AspectArtifact for TypeSourceObjectConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::artifact::ExporterRoot::SourceObjectValueConst)
    }
}

impl AspectBind for TypeSourceObjectConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceObjectConst {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceObjectConst {
    fn to_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        if !matches!(cv.code.code, Code_::None) {
            return cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone());
        }
        let instance = match (cv.value.as_deref().unwrap() as &dyn Any).downcast_ref::<ValueSourceObjectConst>() {
            Some(i) => i,
            None => return EResult::err(
                loc,
                "AspectConsumeDynamic on TypeSourceObjectConst: expected ValueSourceObjectConst".to_string(),
            ),
        };
        let underlying_type = self.data.underlying_type.get().clone();
        let mut errors = None;
        let raw_underlying_cv = CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Some(instance.underlying.clone()),
        };
        let mut converted_res = crate::eval::typecheck_convert(xx, loc, underlying_type.as_ref(), raw_underlying_cv);
        converted_res.push_errors(&mut errors);
        let Some(converted) = converted_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let mut to_dyn_res = aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref());
        to_dyn_res.push_errors(&mut errors);
        let Some(to_dyn) = to_dyn_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let mut underlying_js_res = to_dyn.to_dynamic(xx, loc, converted);
        underlying_js_res.push_errors(&mut errors);
        let Some(underlying_js) = underlying_js_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let mut object_fields: Vec<(String, CodeDynamic)> =
            vec![(ConstKey::String(self.data.data_field.clone()).to_str(), underlying_js)];
        for method in self.data.methods.borrow().iter() {
            let method_id = method.binding_id.get().expect("Method binding ID not allocated");
            object_fields.push((ConstKey::String(method.field_name.clone()).to_str(), CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
            }));
        }
        for impl_data in self.data.interface_impls.borrow().iter() {
            let iface_methods = impl_data.interface.methods.borrow();
            for method_impl in &impl_data.method_impls {
                let iface_method = &iface_methods[method_impl.interface_method_index];
                let method_id = method_impl.binding_id.get().expect("Interface method impl binding ID not allocated");
                object_fields.push((ConstKey::String(iface_method.field_name.clone()).to_str(), CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
                }));
            }
        }
        EResult {
            errors,
            v: Some(CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: object_fields }),
            }),
        }
    }
}

impl AspectField for TypeSourceObjectConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let dyn_type = super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() };
        if !matches!(self_.code.code, Code_::None) {
            return dyn_type.field(xx, loc, self_, key);
        }
        let mut errors = None;
        let mut self_code_res = self.to_dynamic(xx, loc, self_.clone());
        self_code_res.push_errors(&mut errors);
        let Some(self_code) = self_code_res.v else {
            return EvalRes {
                errors,
                v: None,
            };
        };
        let self_dynamic = CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(self_code)),
            },
            value: Some(Rc::new(crate::eval::value::value_dynamic::ValueDynamic {
                source: crate::eval::value::value_dynamic::ValueDynamicSource::Expr,
                type_: Rc::new(super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() }),
            })),
        };
        let mut res = dyn_type.field(xx, loc, self_dynamic, key);
        res.push_errors(&mut errors);
        EvalRes {
            errors,
            v: res.v,
        }
    }

    fn field_keys(&self, xx: &crate::langhelp::ExprContext) -> Vec<crate::langhelp::ConstKey> {
        super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() }.field_keys(xx)
    }

    fn field_type_opt(&self, xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() }.field_type_opt(xx, key)
    }
}

impl AspectInitialBind for TypeSourceObjectConst {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceObjectConst { data: self.data.clone() }),
        })
    }
}

impl AspectLift for TypeSourceObjectConst {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let underlying_type = self.data.underlying_type.get().clone();
        let underlying_lift =
            aspect_lift_get_maybe(
                xx,
                underlying_type.as_ref(),
            ).ok_or_else(|| format!("Underlying type ({}) missing AspectLift", underlying_type.desc()))?;
        let obj = v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected dynamic object for object lift")?;
        let data_field = ConstKey::String(self.data.data_field.clone()).to_str();
        let key = v8::String::new(scope, &data_field).unwrap();
        let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on object object")?;
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
        Ok(Rc::new(ValueSourceObjectConst {
            underlying: underlying_value,
            data: self.data.clone(),
            isolate_bundle,
        }))
    }
}

impl AspectPostDeserialize for TypeSourceObjectConst {
    fn post_deserialize(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<CodeValue> {
        let _ = (xx, loc);
        EResult::ok(cv)
    }
}

impl AspectStackValueDynamic for TypeSourceObjectConst {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() }),
            })),
        }
    }
}

impl ExporterAny for TypeSourceObjectConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceObjectConst {
    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceObjectConst>().expect("expected TypeSourceObjectConst");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<crate::langhelp::ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let art = impl_export_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.artifact_id, || {
            let push_err =
                |errors: &mut Option<Vec<crate::langhelp::ExprErr>>, errs: Vec<crate::langhelp::ExprErr>| {
                    errors.get_or_insert_default().extend(errs);
                };
            let underlying_type = self_.data.underlying_type.get().clone();
            let mut underlying_art = export_artifact(xx, loc, underlying_type.as_value().as_ref());
            if let Some(errs) = underlying_art.errors.take() {
                push_err(&mut *errors_for_closure.borrow_mut(), errs);
            }
            let underlying_art = underlying_art.v.unwrap_or(Artifact::Body(ArtifactBody {
                data: serde_json::Value::Null,
                exporter: crate::artifact::ArtifactBodyExporter::Root,
            }));
            let set_const =
                super::super::type_source_exports::r#const::TypeSourceExportsLiftedConst {
                    set: self_.data.set.borrow().as_ref().unwrap().clone(),
                };
            let mut def_set_art = export_artifact(xx, loc, &set_const as &dyn Value);
            if let Some(errs) = def_set_art.errors.take() {
                push_err(&mut *errors_for_closure.borrow_mut(), errs);
            }
            let def_set_art = def_set_art.v.unwrap_or(Artifact::Body(ArtifactBody {
                data: serde_json::Value::Null,
                exporter: crate::artifact::ArtifactBodyExporter::Root,
            }));
            let mut methods: Vec<ArtifactDataDefSetMethod> = Vec::new();
            for m in self_.data.methods.borrow().iter() {
                let arg_type = m.arg_type.borrow().clone().unwrap_or_else(|| {
                    Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
                });
                let ret_type = m.ret_type.borrow().clone().unwrap_or_else(|| {
                    Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
                });
                let mut arg_art = export_artifact(xx, loc, arg_type.as_value().as_ref());
                if let Some(errs) = arg_art.errors.take() {
                    push_err(&mut *errors_for_closure.borrow_mut(), errs);
                }
                let mut ret_art = export_artifact(xx, loc, ret_type.as_value().as_ref());
                if let Some(errs) = ret_art.errors.take() {
                    push_err(&mut *errors_for_closure.borrow_mut(), errs);
                }
                let scope_idx = match m.binding_id.get() {
                    Some(BindingId::Def(_, idx)) => idx,
                    _ => 0,
                };
                let captured_externals =
                    crate::eval::definition_set::export_captured_externals(xx, loc, &m.captured_externals.borrow());
                let compiled_source = m.compiled_source.borrow().clone().unwrap_or_else(|| {
                    eprintln!(
                        "DBG serialize data-type method '{}' (scope_id={:?}) WITHOUT compiled_source",
                        m.name.as_str(),
                        m.binding_id.get(),
                    );
                    String::new()
                });
                methods.push(ArtifactDataDefSetMethod {
                    name: m.name.clone(),
                    field_name: m.field_name.clone(),
                    arg_type: arg_art.v.unwrap_or(Artifact::Body(ArtifactBody {
                        data: serde_json::Value::Null,
                        exporter: crate::artifact::ArtifactBodyExporter::Root,
                    })),
                    ret_type: ret_art.v.unwrap_or(Artifact::Body(ArtifactBody {
                        data: serde_json::Value::Null,
                        exporter: crate::artifact::ArtifactBodyExporter::Root,
                    })),
                    compiled_source,
                    entry_id: scope_idx,
                    captured_externals,
                });
            }
            let mut interface_impls: Vec<ArtifactDataDefSetInterfaceImpl> = Vec::new();
            for impl_data in self_.data.interface_impls.borrow().iter() {
                let builtin_kind = impl_data.interface.builtin_kind;
                let user_iface = match builtin_kind {
                    Some(_) => None,
                    None => {
                        let iface_const = TypeSourceInterfaceConst { data: impl_data.interface.clone() };
                        let mut iface_art = export_artifact(xx, loc, &iface_const as &dyn Value);
                        if let Some(errs) = iface_art.errors.take() {
                            push_err(&mut *errors_for_closure.borrow_mut(), errs);
                        }
                        iface_art.v
                    },
                };
                let method_impls: Vec<ArtifactDataDefSetInterfaceMethodImpl> = impl_data.method_impls.iter().map(|m| {
                    let scope_idx = match m.binding_id.get() {
                        Some(BindingId::Def(_, idx)) => idx,
                        _ => 0,
                    };
                    let captured_externals =
                        crate::eval::definition_set::export_captured_externals(
                            xx,
                            loc,
                            &m.captured_externals.borrow(),
                        );
                    let compiled_source = m.compiled_source.borrow().clone().unwrap_or_else(|| {
                        panic!(
                            "serialize interface method_impl (interface_method_index={}, scope_id={:?}) without compiled_source",
                            m.interface_method_index,
                            m.binding_id.get(),
                        )
                    });
                    ArtifactDataDefSetInterfaceMethodImpl {
                        interface_method_index: m.interface_method_index,
                        compiled_source,
                        entry_id: scope_idx,
                        captured_externals,
                    }
                }).collect();
                interface_impls.push(ArtifactDataDefSetInterfaceImpl {
                    builtin_kind,
                    user_iface,
                    method_impls,
                });
            }
            let body = ArtifactDataDefSetObject {
                name: self_.data.name.clone(),
                identity: self_.data.identity.clone(),
                data_field: self_.data.data_field.clone(),
                def_set_id: self_.data.set.borrow().as_ref().unwrap().def_set_id,
                def_set: def_set_art,
                underlying_type: underlying_art,
                methods,
                interface_impls,
            };
            make_body(serde_json::to_value(&body).unwrap())
        });
        let errors = errors.borrow_mut().take();
        return EResult {
            errors,
            v: Some(art),
        };
    }

    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(loc, "TypeSourceObjectConst requires an artifact id to deserialize".to_string());
        };
        let id_owned = id.clone();
        let res =
            impl_import_independent_artifact::<ArtifactDataDefSetObject>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataDefSetObject| {
                    let dt_data = dumpster::unsync::Gc::new(super::SourceObjectData {
                        artifact_id: RefCell::new(Some(id_owned.clone())),
                        new_artifact_id: RefCell::new(None),
                        set: RefCell::new(None),
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
                    let def_set_art = body.def_set.clone();
                    let def_set_id_body = body.def_set_id;
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
                        if let Some(ui) = &impl_data.user_iface {
                            deps.push(ui.clone());
                        }
                    }
                    let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(dt_data.clone()));
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            if let Some(ds_any) =
                                crate::artifact::independent_finalize_import_artifact(xx, loc, def_set_art).v {
                                let rc_any: Rc<dyn Any> = ds_any;
                                if let Ok(wrapped) =
                                    rc_any.downcast::<
                                        ExporterDataAny<
                                            dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>,
                                        >,
                                    >() {
                                    *dt_for_finalizer.set.borrow_mut() = Some(wrapped.0.clone());
                                }
                            }
                            if let Some(t) =
                                crate::artifact::independent_finalize_import_type_artifact(
                                    xx,
                                    loc,
                                    underlying_art,
                                ).v {
                                dt_for_finalizer.underlying_type.set(t);
                            }
                            let def_set_id = def_set_id_body;
                            let methods: Vec<Rc<super::SourceDataMethodData>> = methods_serial.into_iter().map(|m| {
                                let arg_type =
                                    crate::artifact::independent_finalize_import_type_artifact(
                                        xx,
                                        loc,
                                        m.arg_type,
                                    ).v;
                                let ret_type =
                                    crate::artifact::independent_finalize_import_type_artifact(
                                        xx,
                                        loc,
                                        m.ret_type,
                                    ).v;
                                Rc::new(super::SourceDataMethodData {
                                    name: m.name,
                                    field_name: m.field_name,
                                    arg_type: RefCell::new(arg_type),
                                    ret_type: RefCell::new(ret_type),
                                    impl_expr: RefCell::new(None),
                                    compiled_source: RefCell::new(Some(m.compiled_source)),
                                    captured_externals: RefCell::new(
                                        crate::eval::definition_set::deexport_captured_externals(
                                            xx,
                                            loc,
                                            &m.captured_externals,
                                        ),
                                    ),
                                    binding_id: std::cell::Cell::new(Some(BindingId::Def(def_set_id, m.entry_id))),
                                    inner_binding_id: std::cell::Cell::new(None),
                                })
                            }).collect();
                            *dt_for_finalizer.methods.borrow_mut() = methods;
                            let builtin_ifaces = crate::r#gen::gen_build_builtin_interfaces(def_set_id.0.0);
                            let iface_impls: Vec<super::SourceObjectInterfaceImplData> =
                                iface_impls_serial.into_iter().filter_map(|impl_data| {
                                    let iface_data = match impl_data.builtin_kind {
                                        Some(BuiltinInterfaceKind::Value) => builtin_ifaces.value.clone(),
                                        Some(BuiltinInterfaceKind::Type) => builtin_ifaces.type_.clone(),
                                        Some(BuiltinInterfaceKind::AspectFunction) => builtin_ifaces
                                            .aspect_function
                                            .clone(),
                                        Some(BuiltinInterfaceKind::AspectField) => builtin_ifaces
                                            .aspect_field
                                            .clone(),
                                        Some(BuiltinInterfaceKind::AspectDropDynamic) => builtin_ifaces
                                            .aspect_drop_dynamic
                                            .clone(),
                                        Some(BuiltinInterfaceKind::Binding) => builtin_ifaces.binding.clone(),
                                        Some(BuiltinInterfaceKind::Target) => builtin_ifaces.target.clone(),
                                        Some(BuiltinInterfaceKind::CodeDyn) => builtin_ifaces.code_dyn.clone(),
                                        None => {
                                            let ui_art =
                                                impl_data
                                                    .user_iface
                                                    .as_ref()
                                                    .expect("non-builtin iface impl missing user_iface");
                                            let iface_any =
                                                crate::artifact::independent_finalize_import_artifact(
                                                    xx,
                                                    loc,
                                                    ui_art.clone(),
                                                ).v?;
                                            let rc_any: Rc<dyn Any> = iface_any;
                                            let wrapped =
                                                rc_any
                                                    .downcast::<
                                                        ExporterDataAny<
                                                            dumpster
                                                            ::unsync
                                                            ::Gc<
                                                                super
                                                                ::super
                                                                ::type_source_interface
                                                                ::SourceInterfaceData,
                                                            >,
                                                        >,
                                                    >()
                                                    .ok()?;
                                            wrapped.0.clone()
                                        },
                                    };
                                    let method_impls = impl_data.method_impls.into_iter().map(|m| {
                                        super::SourceObjectInterfaceMethodImplData {
                                            interface_method_index: m.interface_method_index,
                                            impl_expr: RefCell::new(None),
                                            compiled_source: RefCell::new(Some(m.compiled_source)),
                                            captured_externals: RefCell::new(
                                                crate::eval::definition_set::deexport_captured_externals(
                                                    xx,
                                                    loc,
                                                    &m.captured_externals,
                                                ),
                                            ),
                                            binding_id: std::cell::Cell::new(
                                                Some(BindingId::Def(def_set_id, m.entry_id)),
                                            ),
                                            inner_binding_id: std::cell::Cell::new(None),
                                        }
                                    }).collect();
                                    Some(super::SourceObjectInterfaceImplData {
                                        interface: iface_data,
                                        method_impls,
                                    })
                                }).collect();
                            *dt_for_finalizer.interface_impls.borrow_mut() = iface_impls;
                            if let Some(set) = dt_for_finalizer.set.borrow().as_ref() {
                                let _ = crate::eval::import_install::install_def_set_to_dynamic(xx, loc, set);
                            }
                        });
                    Ok((out, deps, finalizer))
                },
            );
        let errors = res.errors;
        let Some(any) = res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some(any),
        };
    }
}

impl Type for TypeSourceObjectConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceObjectConst { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectBindDynamic>),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_lift_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectLift>),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectArtifact>),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectInitialBind>),
            );
        }
        if *key == aspect_field_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectField>),
            );
        }
        if *key == aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceObjectConst { data: self.data.clone() }) as Box<dyn AspectPostDeserialize>,
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
                            Box::new(TypeSourceObjectConstAspectType { data: self.data.clone() }) as
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
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceObjectConst { data: self.data.clone() }),
                })),
            });
        }
        for (iface_data, identity) in iface_data {
            if type_.aspect(xx, &identity).is_some() {
                if iface_data.builtin_kind.is_some() {
                    let dyn_cv = CodeValue {
                        code: cv.code,
                        value: Some(Rc::new(ValueDynamic {
                            source: ValueDynamicSource::Expr,
                            type_: Rc::new(super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() }),
                        })),
                    };
                    let dyn_type = super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() };
                    return dyn_type.cast_to(xx, loc, dyn_cv, type_);
                }
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Some(Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceInterfaceConst { data: iface_data }),
                    })),
                });
            }
        }
        if type_.aspect(xx, &type_type_identity()).is_some() {
            let dyn_cv = CodeValue {
                code: cv.code,
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() }),
                })),
            };
            let dyn_type = super::dynamic::TypeSourceObjectDynamic { data: self.data.clone() };
            return dyn_type.cast_to(xx, loc, dyn_cv, type_);
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceObjectConst { data: self.data.clone() }))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("object({})", self.data.name.as_deref().unwrap_or("?")),
        }
    }
}

impl Value for TypeSourceObjectConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(crate::eval::value::type_root::r#const::TypeRoot::SourceObjectConst))
    }
}

pub(crate) struct TypeSourceObjectConstAspectType {
    pub data: dumpster::unsync::Gc<super::SourceObjectData>,
}

impl Aspect for TypeSourceObjectConstAspectType { }

impl AspectType for TypeSourceObjectConstAspectType {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let bundle =
            (value as &dyn std::any::Any)
                .downcast_ref::<ValueSourceObjectConst>()
                .and_then(|v| v.isolate_bundle.borrow().clone());
        Rc::new(TypePull {
            data: self.data.clone(),
            bundle: RefCell::new(bundle),
            artifact_id: RefCell::new(None),
        })
    }
}

impl crate::artifact::ExporterAny for ExporterDataAny<dumpster::unsync::Gc<super::SourceObjectData>> {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeSourceObjectConst { data: self.0.clone() }))
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        EResult::ok(Rc::new(TypeSourceObjectConst { data: self.0.clone() }))
    }
}
