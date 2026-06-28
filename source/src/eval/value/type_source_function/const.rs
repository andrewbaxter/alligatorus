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
            CodeDynamicType,
        },
        eval::{
            definition_set::DefinitionSet,
            from_dynamic_ref,
            push::Push,
            to_dynamic_ext,
            value::{
                type_any::r#const::ValueAnyConst,
                type_function::dynamic::dynamic_call,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_tuple::binding::BindingValue,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            BindingId,
            Code,
            CodeValue,
            Code_,
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
            AspectFunction,
            AspectType,
            Binding,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_function_aspect_id,
            aspect_type_aspect_id,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::super::type_source_object::artifact::ArtifactDataDefSetFunction,
};

#[derive(Clone)]
pub(crate) struct TypeSourceFunctionConst {
    pub data: dumpster::unsync::Gc<super::SourceFunctionData>,
}

impl Aspect for TypeSourceFunctionConst { }

impl AspectArtifact for TypeSourceFunctionConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::artifact::ExporterRoot::SourceFunctionConst)
    }
}

impl AspectBind for TypeSourceFunctionConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        let mut value_res = crate::langhelp::EResult::ok(cv.value.clone());
        let mut errors = None;
        value_res.push_errors(&mut errors);
        let Some(value) = value_res.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        return EResult {
            errors,
            v: Some((Code {
                loc: loc,
                code: Code_::None,
            }, Box::new(BindingValue { value: value.unwrap() }) as Box<dyn Binding>)),
        };
    }
}

impl AspectFunction for TypeSourceFunctionConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        {
            let set = self.data.set.borrow().as_ref().unwrap().clone();
            xx.vx.definition_sets.borrow_mut().entry(set.def_set_id).or_insert_with(|| set.clone());
        }
        let scope_id = self.data.binding_id.get().expect("Definition set function not registered");
        let arg_type = self.data.arg_type.borrow().clone().expect("Definition set function missing arg type");
        let ret_type = self.data.ret_type.borrow().clone().expect("Definition set function missing return type");
        let func_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: scope_id }),
        };
        dynamic_call(xx, loc, func_code, arg_type, ret_type, argument)
    }

    fn exception_type(&self) -> Option<std::rc::Rc<dyn crate::langhelp::Type>> {
        self.data.exception_type.borrow().clone()
    }
}

impl AspectType for TypeSourceFunctionConst {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let f = must_downcast_ref::<TypeSourceFunctionConst>(value);
        Rc::new(TypeSourceFunctionConst { data: f.data.clone() })
    }
}

impl ExporterAny for TypeSourceFunctionConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceFunctionConst {
    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceFunctionConst>().expect("expected TypeSourceFunctionConst");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<crate::langhelp::ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let null_art = || {
            Artifact::Body(ArtifactBody {
                data: serde_json::Value::Null,
                exporter: crate::artifact::ArtifactBodyExporter::Root,
            })
        };
        let art = impl_export_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.artifact_id, || {
            let push_err = |errs: Vec<crate::langhelp::ExprErr>| {
                errors_for_closure.borrow_mut().get_or_insert_default().extend(errs);
            };
            let arg_type = self_.data.arg_type.borrow().clone().unwrap_or_else(|| {
                Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
            });
            let ret_type = self_.data.ret_type.borrow().clone().unwrap_or_else(|| {
                Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
            });
            let mut arg_art = export_artifact(xx, loc, arg_type.as_value().as_ref());
            if let Some(errs) = arg_art.errors.take() {
                push_err(errs);
            }
            let mut ret_art = export_artifact(xx, loc, ret_type.as_value().as_ref());
            if let Some(errs) = ret_art.errors.take() {
                push_err(errs);
            }
            let set_const =
                super::super::type_source_exports::r#const::TypeSourceExportsLiftedConst {
                    set: self_.data.set.borrow().as_ref().unwrap().clone(),
                };
            let mut def_set_art = export_artifact(xx, loc, &set_const as &dyn Value);
            if let Some(errs) = def_set_art.errors.take() {
                push_err(errs);
            }
            let entry_id_index = match self_.data.binding_id.get() {
                Some(BindingId::Def(_, idx)) => idx,
                _ => 0,
            };
            let captured_externals =
                crate::eval::definition_set::export_captured_externals(
                    xx,
                    loc,
                    &self_.data.captured_externals.borrow(),
                );
            let body = ArtifactDataDefSetFunction {
                name: self_.data.name.clone(),
                compiled_source: self_.data.compiled_source.borrow().clone().unwrap_or_default(),
                arg_type: arg_art.v.unwrap_or_else(null_art),
                ret_type: ret_art.v.unwrap_or_else(null_art),
                entry_id: entry_id_index,
                def_set_id: self_.data.set.borrow().as_ref().unwrap().def_set_id,
                def_set: def_set_art.v.unwrap_or_else(null_art),
                captured_externals,
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
            return EResult::err(loc, "TypeSourceFunctionConst requires an artifact id to deserialize".to_string());
        };
        let id_owned = id.clone();
        let res =
            impl_import_independent_artifact::<ArtifactDataDefSetFunction>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataDefSetFunction| {
                    let func_data = dumpster::unsync::Gc::new(super::SourceFunctionData {
                        arg_type: std::cell::RefCell::new(None),
                        artifact_id: std::cell::RefCell::new(Some(id_owned.clone())),
                        captured_externals: std::cell::RefCell::new(Vec::new()),
                        compiled_source: std::cell::RefCell::new(Some(body.compiled_source.clone())),
                        description: std::cell::RefCell::new(None),
                        exception_type: std::cell::RefCell::new(None),
                        impl_expr: std::cell::RefCell::new(None),
                        name: body.name.clone(),
                        ret_type: std::cell::RefCell::new(None),
                        binding_id: std::cell::Cell::new(Some(BindingId::Def(body.def_set_id, body.entry_id))),
                        set: std::cell::RefCell::new(None),
                    });
                    let func_for_finalizer = func_data.clone();
                    let arg_type_art = body.arg_type;
                    let ret_type_art = body.ret_type;
                    let def_set_art = body.def_set.clone();
                    let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(func_data.clone()));
                    let deps = vec![body.def_set, arg_type_art.clone(), ret_type_art.clone()];
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            if let Some(ds_any) =
                                crate::artifact::independent_finalize_import_artifact(xx, loc, def_set_art).v {
                                let rc_any: Rc<dyn Any> = ds_any;
                                if let Ok(wrapped) =
                                    rc_any.downcast::<ExporterDataAny<dumpster::unsync::Gc<DefinitionSet>>>() {
                                    *func_for_finalizer.set.borrow_mut() = Some(wrapped.0.clone());
                                }
                            }
                            if let Some(v) =
                                crate::artifact::independent_finalize_import_type_artifact(
                                    xx,
                                    loc,
                                    arg_type_art.clone(),
                                ).v {
                                *func_for_finalizer.arg_type.borrow_mut() = Some(v);
                            }
                            if let Some(v) =
                                crate::artifact::independent_finalize_import_type_artifact(
                                    xx,
                                    loc,
                                    ret_type_art.clone(),
                                ).v {
                                *func_for_finalizer.ret_type.borrow_mut() = Some(v);
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

impl Type for TypeSourceFunctionConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceFunctionConst { data: self.data.clone() }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectFunction>),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectArtifact>),
            );
        }
        if *key == aspect_type_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceFunctionConst { data: self.data.clone() }) as Box<dyn AspectType>),
            );
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_function"))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_function"),
        }
    }
}

impl Value for TypeSourceFunctionConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceFunctionConst))
    }
}

impl Push for Rc<TypeSourceFunctionConst> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(super::dynamic::TypeSourceFunctionDynamic)
    }

    fn from_dynamic<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_dynamic_ref::<Rc<TypeSourceFunctionConst>>(&*value).clone()
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_dynamic_ext(scope, self).into()
    }
}
