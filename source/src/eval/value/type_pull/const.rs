use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            ExporterRoot,
            import_artifact,
            impl_import_independent_artifact,
            impl_export_independent_artifact,
            independent_finalize_import_artifact,
            export_artifact,
        },
        eval::{
            DynamicSourceBuilder,
            aspect::AspectPull,
            import_install::install_def_set_to_dynamic,
            pull_type_dispatch::{
                pull_aspect_v8,
                pull_can_cast_from_v8,
                pull_cast_from_v8,
                pull_desc,
            },
            render_code,
            scope_entry_id_dynamic_key,
            value::{
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_source_data_type::{
                    SourceDataTypeData,
                    r#const::TypeSourceDataTypeConst,
                },
                type_source_interface::SourceInterfaceRef,
                value_pull::aspect::TypePullAspect,
            },
        },
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            ExprErr,
            QExprId,
            SourceId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectArtifact,
            AspectField,
            AspectFunction,
            aspect_artifact_aspect_id,
            aspect_consume_dynamic_get_maybe,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_lift_get_maybe,
        },
        syntax::ExprId,
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
    super::artifact::ArtifactDataPull,
};

pub(crate) fn pull_mini_eval(
    xx: &ExprContext,
    loc: QExprId,
    data: &Gc<SourceDataTypeData>,
    inner: Rc<dyn Value>,
) -> Result<v8::Global<v8::Object>, String> {
    let set =
        data.set.borrow().as_ref().cloned().ok_or_else(|| "Pull mini-eval: data type has no def-set".to_string())?;
    install_def_set_to_dynamic(xx, loc, &set)?;
    let underlying_type = data.underlying_type.get().clone();
    let to_dyn =
        aspect_consume_dynamic_get_maybe(
            xx,
            underlying_type.as_ref(),
        ).ok_or_else(|| format!("Underlying type ({}) missing AspectConsumeDynamic", underlying_type.desc()))?;
    let inner_cv = CodeValue {
        code: Code {
            loc,
            code: Code_::None,
        },
        value: inner,
    };
    let inner_dyn = to_dyn.to_dynamic(xx, loc, inner_cv).into_res().map_err(|e| format!("to_dynamic: {:?}", e))?;
    let scope_ptr =
        xx
            .vx
            .dynamic_scope
            .get()
            .ok_or_else(|| "Pull mini-eval requires an active dynamic scope (run from dynamic stage)".to_string())?;
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let mut code = DynamicSourceBuilder::default();
    code.push(loc, "(function() {".to_string());
    let underlying_var = render_code(&mut code, &inner_dyn);
    let mut field_decls: Vec<(String, String)> = Vec::new();
    field_decls.push((ConstKey::String(data.data_field.clone()).to_str(), underlying_var));
    for method in data.methods.borrow().iter() {
        let id = method.scope_entry_id.get().ok_or_else(|| "Method scope_entry_id not allocated".to_string())?;
        field_decls.push((ConstKey::String(method.field_name.clone()).to_str(), scope_entry_id_dynamic_key(id)));
    }
    for impl_data in data.interface_impls.borrow().iter() {
        let methods = impl_data.interface.methods.borrow();
        for m in &impl_data.method_impls {
            let id =
                m
                    .scope_entry_id
                    .get()
                    .ok_or_else(|| "Interface method impl scope_entry_id not allocated".to_string())?;
            let field_name = methods[m.interface_method_index].field_name.clone();
            field_decls.push((ConstKey::String(field_name).to_str(), scope_entry_id_dynamic_key(id)));
        }
    }
    let obj_lit =
        field_decls
            .iter()
            .map(|(k, v)| format!("{}: {}", serde_json::to_string(k).unwrap(), v))
            .collect::<Vec<_>>()
            .join(", ");
    code.push(loc, format!("return {{ {} }};", obj_lit));
    code.push(loc, "})()".to_string());
    let source = code.lines.join("\n");
    let script =
        v8::Script::compile(
            scope,
            v8::String::new(scope, &source).ok_or_else(|| "Pull mini-eval: script source alloc".to_string())?,
            None,
        ).ok_or_else(|| "Pull mini-eval: compile failed".to_string())?;
    let result = script.run(scope).ok_or_else(|| "Pull mini-eval: run returned no value".to_string())?;
    let instance_obj =
        v8::Local::<v8::Object>::try_from(
            result,
        ).map_err(|_| "Pull mini-eval: script did not return an object".to_string())?;
    Ok(v8::Global::new(scope, instance_obj))
}

#[derive(Clone)]
pub(crate) struct TypePull {
    pub artifact_id: RefCell<Option<ArtifactIdSource>>,
    pub bundle: RefCell<Option<v8::Global<v8::Object>>>,
    pub data: Gc<SourceDataTypeData>,
}

impl Aspect for TypePull { }

impl AspectArtifact for TypePull {
    fn exporter(&self) -> Rc<dyn crate::artifact::ArtifactExporter> {
        Rc::new(ExporterRoot::Pull)
    }
}

impl ExporterAny for TypePull {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypePull {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(loc, "TypePull requires an artifact id to deserialize".to_string());
        };
        let id_owned = id.clone();
        impl_import_independent_artifact::<ArtifactDataPull>(xx, loc, id, data, move |body: ArtifactDataPull| {
            // Eagerly deserialize the data type dependency so we can construct the TypePull
            // with a real Gc`<SourceDataTypeData>`. The cache lookup in import_artifact
            // ensures the later deps loop reuses the same Gc.
            let dt_any =
                import_artifact(xx, loc, body.data_type.clone())
                    .v
                    .ok_or_else(|| "TypePull deserialize: failed to deserialize data type".to_string())?;
            let dt_rc_any: Rc<dyn Any> = dt_any;
            let dt_wrapped =
                dt_rc_any
                    .downcast::<crate::artifact::ExporterDataAny<Gc<SourceDataTypeData>>>()
                    .map_err(
                        |_| "TypePull deserialize: data type artifact did not yield SourceDataTypeData".to_string(),
                    )?;
            let data = dt_wrapped.0.clone();
            let pull = Rc::new(TypePull {
                data: data,
                bundle: RefCell::new(None),
                artifact_id: RefCell::new(Some(id_owned.clone())),
            });
            let pull_for_finalizer = pull.clone();
            let inner_art = body.inner;
            let deps: Vec<Artifact> = vec![inner_art.clone()];
            let finalizer: crate::artifact::ArtifactFinalizer = Box::new(move |xx: &ExprContext, loc: QExprId| {
                let inner_any =
                    independent_finalize_import_artifact(xx, loc, inner_art)
                        .v
                        .expect("TypePull finalizer: failed to deserialize inner artifact");
                let inner_value =
                    inner_any.as_value().v.expect("TypePull finalizer: inner artifact did not yield a Value");
                let bundle =
                    pull_mini_eval(
                        xx,
                        loc,
                        &pull_for_finalizer.data,
                        inner_value,
                    ).expect("TypePull finalizer: pull_mini_eval failed");
                *pull_for_finalizer.bundle.borrow_mut() = Some(bundle);
            });
            let out: Rc<dyn ExporterAny> = pull;
            Ok((out, deps, finalizer))
        })
    }

    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypePull>().expect("expected TypePull");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let abort: std::rc::Rc<std::cell::Cell<bool>> = std::rc::Rc::new(std::cell::Cell::new(false));
        let abort_for_closure = abort.clone();
        let null_art = Artifact::Body(ArtifactBody {
            data: serde_json::Value::Null,
            exporter: crate::artifact::ArtifactBodyExporter::Root,
        });
        let null_art_for_closure = null_art.clone();
        let art = impl_export_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.artifact_id, || {
            let push_err = |errs: Vec<ExprErr>| {
                errors_for_closure.borrow_mut().get_or_insert_default().extend(errs);
            };
            let push_err_one = |loc: QExprId, msg: String| {
                let err = ExprErr {
                    loc,
                    errors: vec![crate::langhelp::ExprSubErr::Opaque(msg)],
                };
                errors_for_closure.borrow_mut().get_or_insert_default().push(err);
                abort_for_closure.set(true);
            };
            let dt_const = TypeSourceDataTypeConst { data: self_.data.clone() };
            let mut dt_art = export_artifact(xx, loc, &dt_const as &dyn Value);
            if let Some(errs) = dt_art.errors.take() {
                push_err(errs);
            }
            let dt_art = dt_art.v.unwrap_or_else(|| null_art_for_closure.clone());
            let bundle_borrow = self_.bundle.borrow();
            let Some(bundle) = bundle_borrow.as_ref() else {
                push_err_one(loc, "Cannot serialize Pull type without an instantiated bundle".to_string());
                return make_body(serde_json::to_value(&ArtifactDataPull {
                    data_type: dt_art,
                    inner: null_art_for_closure.clone(),
                }).unwrap());
            };
            let underlying_type = self_.data.underlying_type.get().clone();
            let lift = match aspect_lift_get_maybe(xx, underlying_type.as_ref()) {
                Some(v) => v,
                None => {
                    push_err_one(
                        loc,
                        format!("Underlying type ({}) missing AspectLift", underlying_type.desc()),
                    );
                    return make_body(serde_json::to_value(&ArtifactDataPull {
                        data_type: dt_art,
                        inner: null_art_for_closure.clone(),
                    }).unwrap());
                },
            };
            let scope_ptr = match xx.vx.dynamic_scope.get() {
                Some(p) => p,
                None => {
                    push_err_one(loc, "Pull serialize requires active dynamic scope to lift inner data".to_string());
                    return make_body(serde_json::to_value(&ArtifactDataPull {
                        data_type: dt_art,
                        inner: null_art_for_closure.clone(),
                    }).unwrap());
                },
            };
            let inner_value = unsafe {
                let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> =
                    &mut *(scope_ptr.as_ptr() as *mut _);
                let instance = v8::Local::new(scope, bundle);
                let data_field_str = ConstKey::String(self_.data.data_field.clone()).to_str();
                let key = match v8::String::new(scope, &data_field_str) {
                    Some(k) => k,
                    None => {
                        push_err_one(loc, "Pull serialize: data field key alloc".to_string());
                        return make_body(serde_json::to_value(&ArtifactDataPull {
                            data_type: dt_art,
                            inner: null_art_for_closure.clone(),
                        }).unwrap());
                    },
                };
                let inner_v8 = match instance.get(scope, key.into()) {
                    Some(v) => v,
                    None => {
                        push_err_one(loc, "Pull serialize: missing data field on instance".to_string());
                        return make_body(serde_json::to_value(&ArtifactDataPull {
                            data_type: dt_art,
                            inner: null_art_for_closure.clone(),
                        }).unwrap());
                    },
                };
                match lift.lift(xx, scope, inner_v8) {
                    Ok(v) => v,
                    Err(e) => {
                        push_err_one(loc, format!("Pull serialize lift: {}", e));
                        return make_body(serde_json::to_value(&ArtifactDataPull {
                            data_type: dt_art,
                            inner: null_art_for_closure.clone(),
                        }).unwrap());
                    },
                }
            };
            drop(bundle_borrow);
            let mut inner_art = export_artifact(xx, loc, inner_value.as_ref());
            if let Some(errs) = inner_art.errors.take() {
                push_err(errs);
            }
            let body = ArtifactDataPull {
                data_type: dt_art,
                inner: inner_art.v.unwrap_or_else(|| null_art_for_closure.clone()),
            };
            make_body(serde_json::to_value(&body).unwrap())
        });
        let errors = errors.borrow_mut().take();
        if abort.get() {
            return EResult {
                errors,
                v: None,
            };
        }
        EResult {
            errors,
            v: Some(art),
        }
    }
}

impl Type for TypePull {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypePull {
            data: self.data.clone(),
            bundle: RefCell::new(self.bundle.borrow().clone()),
            artifact_id: RefCell::new(self.artifact_id.borrow().clone()),
        })
    }

    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypePull {
                data: self.data.clone(),
                bundle: RefCell::new(self.bundle.borrow().clone()),
                artifact_id: RefCell::new(self.artifact_id.borrow().clone()),
            }) as Box<dyn AspectArtifact>));
        }
        let bundle_borrow = self.bundle.borrow();
        let bundle_opt = bundle_borrow.as_ref();
        if let Some(bundle) = bundle_opt {
            let interface_impls = self.data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if *key == impl_data.interface.identity && impl_data.interface.is_aspect.get() {
                    return Some(Box::new(Box::new(TypePullAspect {
                        iface_ref: Rc::new(SourceInterfaceRef { data: impl_data.interface.clone() }),
                        bundle: Some(bundle.clone()),
                    }) as Box<dyn AspectField>));
                }
            }
        }
        let bundle = bundle_opt?;
        let dummy_loc: QExprId = (SourceId(0), ExprId(0));
        let aspect = pull_aspect_v8(&self.data, bundle, xx, dummy_loc, key)?;
        let aspect_pull = (&*aspect as &dyn std::any::Any).downcast_ref::<AspectPull>().unwrap().clone();
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(aspect_pull) as Box<dyn AspectField>));
        }
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(aspect_pull) as Box<dyn AspectFunction>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        let bundle = self.bundle.borrow();
        let bundle = bundle.as_ref().expect("TypePull::can_cast_from called before bundle populated");
        pull_can_cast_from_v8(&self.data, bundle, xx, loc, source_type)
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let bundle = self.bundle.borrow();
        let bundle = bundle.as_ref().expect("TypePull::cast_from called before bundle populated");
        pull_cast_from_v8(&self.data, bundle, xx, loc, cv)
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn desc(&self) -> String {
        pull_desc(&self.data)
    }
}

impl Value for TypePull {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        Rc::new(TypePull {
            data: self.data.clone(),
            bundle: RefCell::new(self.bundle.borrow().clone()),
            artifact_id: RefCell::new(self.artifact_id.borrow().clone()),
        })
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::Pull))
    }
}
