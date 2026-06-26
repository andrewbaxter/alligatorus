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
            serialize_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicType,
        },
        eval::{
            definition_set::DefinitionSet,
            from_v8_ref,
            push::Push,
            to_v8,
            value::{
                tuple::scope_entry::ScopeEntryValue,
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_function::dynamic::dynamic_call,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            DefSetId,
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
            AspectFunction,
            AspectType,
            ScopeEntry,
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
    super::super::type_source_data_type::artifact::ArtifactDataDefSetFunction,
};

#[derive(Clone)]
pub(crate) struct TypeSourceFunctionConst {
    pub data: dumpster::unsync::Gc<super::SourceFunctionData>,
}

impl Aspect for TypeSourceFunctionConst { }

impl AspectArtifact for TypeSourceFunctionConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::eval::value::type_root::r#const::ExporterRoot::SourceFunctionConst)
    }
}

impl AspectBind for TypeSourceFunctionConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
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
            }, Box::new(ScopeEntryValue { value }) as Box<dyn ScopeEntry>)),
        };
    }
}

impl AspectFunction for TypeSourceFunctionConst {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        {
            let set = self.data.set.borrow().as_ref().unwrap().clone();
            xx.vx.definition_sets.borrow_mut().entry(set.def_set_id).or_insert_with(|| set.clone());
        }
        let scope_id = self.data.scope_entry_id.get().expect("Definition set function not registered");
        let arg_type = self.data.arg_type.borrow().clone().expect("Definition set function missing arg type");
        let ret_type = self.data.ret_type.borrow().clone().expect("Definition set function missing return type");
        let func_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: scope_id }),
        };
        dynamic_call(xx, loc, func_code, arg_type, ret_type, argument)
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
            impl_deserialize_independent_artifact::<ArtifactDataDefSetFunction>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataDefSetFunction| {
                    let func_data = dumpster::unsync::Gc::new(super::SourceFunctionData {
                        arg_type: std::cell::RefCell::new(None),
                        artifact_id: std::cell::RefCell::new(Some(id_owned.clone())),
                        captured_externals: std::cell::RefCell::new(Vec::new()),
                        compiled_js: std::cell::RefCell::new(Some(body.compiled_js.clone())),
                        description: std::cell::RefCell::new(None),
                        impl_expr: std::cell::RefCell::new(None),
                        name: body.name.clone(),
                        ret_type: std::cell::RefCell::new(None),
                        scope_entry_id: std::cell::Cell::new(
                            Some(ScopeEntryId::Def(body.def_set_id, body.entry_id)),
                        ),
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
                                crate::artifact::independent_finalize_deserialize_artifact(xx, loc, def_set_art).v {
                                let rc_any: Rc<dyn Any> = ds_any;
                                if let Ok(wrapped) =
                                    rc_any.downcast::<ExporterDataAny<dumpster::unsync::Gc<DefinitionSet>>>() {
                                    *func_for_finalizer.set.borrow_mut() = Some(wrapped.0.clone());
                                }
                            }
                            if let Some(v) =
                                crate::artifact::independent_finalize_deserialize_type_artifact(
                                    xx,
                                    loc,
                                    arg_type_art.clone(),
                                ).v {
                                *func_for_finalizer.arg_type.borrow_mut() = Some(v);
                            }
                            if let Some(v) =
                                crate::artifact::independent_finalize_deserialize_type_artifact(
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
        let mut errors = res.errors;
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
        let self_ = self_.downcast::<TypeSourceFunctionConst>().expect("expected TypeSourceFunctionConst");
        let arg_type = self_.data.arg_type.borrow().clone().unwrap_or_else(|| {
            Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
        });
        let ret_type = self_.data.ret_type.borrow().clone().unwrap_or_else(|| {
            Rc::new(super::super::type_null::dynamic::TypeNullDynamic) as Rc<dyn Type>
        });
        let arg_art = serialize_artifact(xx, loc, arg_type.as_value().as_ref());
        let mut errors = arg_art.errors;
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
        let set_const =
            super::super::type_source_exports::r#const::TypeSourceExportsLiftedConst {
                set: self_.data.set.borrow().as_ref().unwrap().clone(),
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
        let entry_id_index = match self_.data.scope_entry_id.get() {
            Some(ScopeEntryId::Def(_, idx)) => idx,
            _ => 0,
        };
        let captured_externals =
            crate::eval::definition_set::serialize_captured_externals(
                xx,
                loc,
                &self_.data.captured_externals.borrow(),
            );
        let art = impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.artifact_id, || {
            let body = ArtifactDataDefSetFunction {
                name: self_.data.name.clone(),
                compiled_js: self_.data.compiled_js.borrow().clone().unwrap_or_default(),
                arg_type: arg_art,
                ret_type: ret_art,
                entry_id: entry_id_index,
                def_set_id: self_.data.set.borrow().as_ref().unwrap().def_set_id,
                def_set: def_set_art,
                captured_externals,
            };
            make_body(serde_json::to_value(&body).unwrap())
        });
        return EResult {
            errors,
            v: Some(art),
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
        if *key == crate::r#gen::aspect_unlift_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::UnliftIdentity) as Box<dyn crate::r#gen::AspectUnlift>),
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

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_v8_ref::<Rc<TypeSourceFunctionConst>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }
}
