use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            impl_import_independent_artifact,
            impl_export_independent_artifact,
            independent_finalize_import_artifact,
            export_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            typecheck_convert,
            value::{
                type_tuple::binding::BindingValue,
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
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
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectFunction,
            Binding,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_stack_value_dynamic_get,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::{
        TypeSourceObjectNewConst,
        artifact::ArtifactDataSourceObjectNew,
    },
};

impl crate
::artifact
::ExporterAny for crate
::artifact
::ExporterDataAny<
    Rc<std::cell::RefCell<Option<dumpster::unsync::Gc<super::super::type_source_object::SourceObjectData>>>>,
> {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        let borrowed = self.0.borrow();
        let data = borrowed.as_ref().expect("constructor holder not populated").clone();
        EResult::ok(Rc::new(TypeSourceObjectNewConst { data }))
    }

    fn as_value(&self) -> EResult<Rc<dyn crate::langhelp::Value>> {
        let borrowed = self.0.borrow();
        let data = borrowed.as_ref().expect("constructor holder not populated").clone();
        let inner: Rc<dyn Type> = Rc::new(TypeSourceObjectNewConst { data });
        EResult::ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner }))
    }
}

// TODO(porting): export_ctor removed pending export_type_ref design.
// Constructor types are now serialized as part of the parent DefinitionSet.
impl Aspect for TypeSourceObjectNewConst { }

impl AspectBind for TypeSourceObjectNewConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
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
            }, Box::new(BindingValue { value }) as Box<dyn Binding>)),
        };
    }
}

impl AspectFunction for TypeSourceObjectNewConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        {
            let set = self.data.set.borrow().as_ref().unwrap().clone();
            xx.vx.definition_sets.borrow_mut().entry(set.def_set_id).or_insert_with(|| set.clone());
        }
        let underlying_type = self.data.underlying_type.get().clone();
        return er_seq_cv!(loc, erb, {
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;
            let mut object_fields = vec![(ConstKey::String(self.data.data_field.clone()).to_str(), underlying_js)];
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
                    let method_id =
                        method_impl.binding_id.get().expect("Interface method impl binding ID not allocated");
                    object_fields.push((ConstKey::String(iface_method.field_name.clone()).to_str(), CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
                    }));
                }
            }
            let result_type =
                Rc::new(
                    super::super::type_source_object::dynamic::TypeSourceObjectDynamic { data: self.data.clone(), }
                );
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: object_fields }),
            }));
        });
    }
}

impl ExporterAny for TypeSourceObjectNewConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceObjectNewConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(
                loc,
                "TypeSourceObjectNewConst requires an artifact id to deserialize".to_string(),
            );
        };
        let id_owned = id.clone();
        let holder:
            Rc<
                std
                ::cell
                ::RefCell<Option<dumpster::unsync::Gc<super::super::type_source_object::SourceObjectData>>>,
            > =
            Rc::new(std::cell::RefCell::new(None));
        let holder_for_finalizer = holder.clone();
        let res =
            impl_import_independent_artifact::<ArtifactDataSourceObjectNew>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataSourceObjectNew| {
                    let object_art = body.object;
                    let deps = vec![object_art.clone()];
                    let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(holder.clone()));
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            if let Some(parent_any) =
                                independent_finalize_import_artifact(xx, loc, object_art).v {
                                let rc_any: Rc<dyn Any> = parent_any;
                                if let Ok(wrapped) =
                                    rc_any.downcast::<
                                        ExporterDataAny<
                                            dumpster
                                            ::unsync
                                            ::Gc<super::super::type_source_object::SourceObjectData>,
                                        >,
                                    >() {
                                    let dt_data = wrapped.0.clone();
                                    *dt_data.new_artifact_id.borrow_mut() = Some(id_owned.clone());
                                    *holder_for_finalizer.borrow_mut() = Some(dt_data);
                                }
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

    fn self_export(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ =
            self_
                .downcast::<TypeSourceObjectNewConst>()
                .expect("expected TypeSourceObjectNewConst");
        let errors: std::rc::Rc<std::cell::RefCell<Option<Vec<crate::langhelp::ExprErr>>>> =
            std::rc::Rc::new(std::cell::RefCell::new(None));
        let errors_for_closure = errors.clone();
        let art =
            impl_export_independent_artifact(
                xx.vx.ix.gx.artifacts.as_ref(),
                &self_.data.new_artifact_id,
                || {
                    let dt_const =
                        super::super::type_source_object::r#const::TypeSourceObjectConst {
                            data: self_.data.clone(),
                        };
                    let mut object_art = export_artifact(xx, loc, &dt_const as &dyn Value);
                    if let Some(errs) = object_art.errors.take() {
                        errors_for_closure.borrow_mut().get_or_insert_default().extend(errs);
                    }
                    let object_art = object_art.v.unwrap_or(Artifact::Body(ArtifactBody {
                        data: serde_json::Value::Null,
                        exporter: crate::artifact::ArtifactBodyExporter::Root,
                    }));
                    let body = ArtifactDataSourceObjectNew { object: object_art };
                    make_body(serde_json::to_value(&body).unwrap())
                },
            );
        let errors = errors.borrow_mut().take();
        return EResult {
            errors,
            v: Some(art),
        };
    }
}

impl Type for TypeSourceObjectNewConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceObjectNewConst { data: self.data.clone() }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceObjectNewConst { data: self.data.clone() }) as
                        Box<dyn AspectFunction>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceObjectNewConst { data: self.data.clone() }) as Box<dyn AspectBind>,
                ),
            );
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to object_new"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert object_new"))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_object_new"),
        }
    }
}

impl Value for TypeSourceObjectNewConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceObjectNewConst))
    }
}
