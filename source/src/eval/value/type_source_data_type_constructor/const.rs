use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactIdSource,
            ExporterAny,
            ExporterDataAny,
            impl_deserialize_independent_artifact,
            impl_serialize_independent_artifact,
            independent_finalize_deserialize_artifact,
            serialize_artifact,
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
                tuple::scope_entry::ScopeEntryValue,
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
            ScopeEntry,
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
        TypeSourceDataTypeConstructorConst,
        artifact::ArtifactDataSourceDataTypeConstructor,
    },
};

// TODO(porting): serialize_ctor removed pending serialize_type_ref design.
// Constructor types are now serialized as part of the parent DefinitionSet.
impl Aspect for TypeSourceDataTypeConstructorConst { }

impl AspectBind for TypeSourceDataTypeConstructorConst {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((Code {
            loc: loc,
            code: Code_::None,
        }, Box::new(ScopeEntryValue { value: Rc::from(cv.value) }) as Box<dyn ScopeEntry>))
    }
}

impl AspectFunction for TypeSourceDataTypeConstructorConst {
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
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
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
                        method_impl.scope_entry_id.get().expect("Interface method impl scope entry ID not allocated");
                    object_fields.push((ConstKey::String(iface_method.field_name.clone()).to_str(), CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
                    }));
                }
            }
            let result_type =
                Rc::new(
                    super::super::type_source_data_type::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone(), }
                );
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: object_fields }),
            }));
        });
    }
}

impl ExporterAny for TypeSourceDataTypeConstructorConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceDataTypeConstructorConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(
                loc,
                "TypeSourceDataTypeConstructorConst requires an artifact id to deserialize".to_string(),
            );
        };
        let id_owned = id.clone();
        let holder:
            Rc<
                std
                ::cell
                ::RefCell<Option<dumpster::unsync::Gc<super::super::type_source_data_type::SourceDataTypeData>>>,
            > =
            Rc::new(std::cell::RefCell::new(None));
        let holder_for_finalizer = holder.clone();
        let res =
            impl_deserialize_independent_artifact::<ArtifactDataSourceDataTypeConstructor>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataSourceDataTypeConstructor| {
                    let data_type_art = body.data_type;
                    let deps = vec![data_type_art.clone()];
                    let out: Rc<dyn ExporterAny> = Rc::new(ExporterDataAny(holder.clone()));
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            if let Some(parent_any) =
                                independent_finalize_deserialize_artifact(xx, loc, data_type_art).v {
                                let rc_any: Rc<dyn Any> = parent_any;
                                if let Ok(wrapped) =
                                    rc_any.downcast::<
                                        ExporterDataAny<
                                            dumpster
                                            ::unsync
                                            ::Gc<super::super::type_source_data_type::SourceDataTypeData>,
                                        >,
                                    >() {
                                    let dt_data = wrapped.0.clone();
                                    *dt_data.constructor_artifact_id.borrow_mut() = Some(id_owned.clone());
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

    fn self_to_serial(
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn crate::artifact::ExporterAny>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ =
            self_
                .downcast::<TypeSourceDataTypeConstructorConst>()
                .expect("expected TypeSourceDataTypeConstructorConst");
        let mut errors: Option<Vec<crate::langhelp::ExprErr>> = None;
        let dt_const =
            super::super::type_source_data_type::r#const::TypeSourceDataTypeConst { data: self_.data.clone() };
        let data_type_art = serialize_artifact(xx, loc, &dt_const as &dyn Value);
        if let Some(errors1) = data_type_art.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(data_type_art) = data_type_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let art =
            impl_serialize_independent_artifact(
                xx.vx.ix.gx.artifacts.as_ref(),
                &self_.data.constructor_artifact_id,
                || {
                    let body = ArtifactDataSourceDataTypeConstructor { data_type: data_type_art };
                    make_body(serde_json::to_value(&body).unwrap())
                },
            );
        return EResult {
            errors,
            v: Some(art),
        };
    }
}

impl Type for TypeSourceDataTypeConstructorConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceDataTypeConstructorConst { data: self.data.clone() }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConstructorConst { data: self.data.clone() }) as
                        Box<dyn AspectFunction>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConstructorConst { data: self.data.clone() }) as Box<dyn AspectBind>,
                ),
            );
        }
        return None;
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_data_type_constructor"),
        }
    }
}

impl Value for TypeSourceDataTypeConstructorConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceDataTypeConstructorConst))
    }
}
