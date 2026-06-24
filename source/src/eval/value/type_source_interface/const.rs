use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ExporterAny,
            impl_serialize_independent_artifact,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_root::r#const::{
                    ExporterRootDelegate,
                    TypeRoot,
                },
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
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
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectArtifact,
            AspectAspect,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectStackValueDynamic,
            AspectType,
            ScopeEntry,
            aspect_artifact_aspect_id,
            aspect_aspect_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            aspect_type_aspect_id,
            interface_const_identity,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::super::type_source_data_type::artifact::{
        ArtifactDataDefSetInterface,
        ArtifactDataDefSetInterfaceMethod,
    },
};

#[derive(Clone)]
pub(crate) struct TypeSourceInterfaceConst {
    pub data: dumpster::unsync::Gc<super::SourceInterfaceData>,
}

impl Aspect for TypeSourceInterfaceConst { }

impl AspectArtifact for TypeSourceInterfaceConst {
    fn exporter(&self) -> std::rc::Rc<dyn crate::artifact::ArtifactExporter> {
        std::rc::Rc::new(crate::eval::value::type_root::r#const::ExporterRoot::SourceInterfaceConst)
    }
}

impl AspectAspect for TypeSourceInterfaceConst {
    fn get_iface_data(&self, _value: &dyn Value) -> dumpster::unsync::Gc<super::SourceInterfaceData> {
        self.data.clone()
    }

    fn get_unique(&self, _value: &dyn Value) -> Unique {
        self.data.identity.clone()
    }
}

impl AspectBind for TypeSourceInterfaceConst {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceInterfaceConst {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceInterfaceConst {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectField for TypeSourceInterfaceConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = erb.must_er(key.value.type_(xx, loc))?;
            let key_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            let (field_name, arg_type, ret_type) = {
                let methods = self.data.methods.borrow();
                let method = methods.iter().find(|m| m.name == key_name);
                match method {
                    Some(m) => {
                        let arg_t = m.arg_type.borrow().clone().expect("Interface method missing arg type");
                        let ret_t = m.ret_type.borrow().clone().expect("Interface method missing return type");
                        (m.field_name.clone(), arg_t, ret_t)
                    },
                    None => return erb.err_new(format!("Field '{}' not found on interface", key_name)),
                }
            };
            let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
            let field_type = Rc::new(TypeFunctionDynamic {
                arg: arg_type,
                ret: ret_type,
            });
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                            base: Box::new(self_code),
                            field: ConstKey::String(field_name).to_str(),
                        }),
                    })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: field_type,
                }),
            });
        });
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(key) = key else {
            return None;
        };
        let methods = self.data.methods.borrow();
        for method in methods.iter() {
            if method.name == *key {
                return Some(Rc::new(TypeFunctionDynamic {
                    arg: Rc::new(TypeNullDynamic),
                    ret: Rc::new(TypeNullDynamic),
                }));
            }
        }
        None
    }
}

impl AspectInitialBind for TypeSourceInterfaceConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }),
        })
    }
}

impl AspectStackValueDynamic for TypeSourceInterfaceConst {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }),
            }),
        }
    }
}

impl ExporterAny for TypeSourceInterfaceConst {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(self.clone()));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(self.clone()));
    }
}

impl ExporterRootDelegate for TypeSourceInterfaceConst {
    fn self_from_artifact_data(
        xx: &ExprContext,
        loc: QExprId,
        id: Option<&crate::artifact::ArtifactIdSource>,
        data: serde_json::Value,
    ) -> EResult<Rc<dyn crate::artifact::ExporterAny>> {
        let Some(id) = id else {
            return EResult::err(loc, "TypeSourceInterfaceConst requires an artifact id to deserialize".to_string());
        };
        let id_owned = id.clone();
        let res =
            crate::artifact::impl_deserialize_independent_artifact::<ArtifactDataDefSetInterface>(
                xx,
                loc,
                id,
                data,
                move |body: ArtifactDataDefSetInterface| {
                    let methods: Vec<Rc<super::SourceInterfaceMethodData>> = body.methods.iter().map(|m| {
                        Rc::new(super::SourceInterfaceMethodData {
                            arg_type: std::cell::RefCell::new(None),
                            field_name: m.field_name.clone(),
                            name: m.name.clone(),
                            ret_type: std::cell::RefCell::new(None),
                        })
                    }).collect();
                    let iface_data = dumpster::unsync::Gc::new(super::SourceInterfaceData {
                        artifact_id: std::cell::RefCell::new(Some(id_owned.clone())),
                        builtin_kind: body.builtin_kind,
                        description: std::cell::RefCell::new(body.description.clone()),
                        identity: body.identity.clone(),
                        is_aspect: std::cell::Cell::new(body.is_aspect),
                        methods: std::cell::RefCell::new(methods),
                        set: std::cell::RefCell::new(None),
                    });
                    let iface_for_finalizer = iface_data.clone();
                    let methods_serial = body.methods;
                    let def_set_art = body.def_set.clone();
                    let mut deps: Vec<Artifact> = Vec::new();
                    deps.push(body.def_set);
                    for m in &methods_serial {
                        deps.push(m.arg_type.clone());
                        deps.push(m.ret_type.clone());
                    }
                    let out: Rc<dyn ExporterAny> = Rc::new(crate::artifact::ExporterDataAny(iface_data.clone()));
                    let finalizer: crate::artifact::ArtifactFinalizer =
                        Box::new(move |xx: &ExprContext, loc: QExprId| {
                            if let Some(ds_any) =
                                crate::artifact::independent_finalize_deserialize_artifact(xx, loc, def_set_art).v {
                                let rc_any: Rc<dyn std::any::Any> = ds_any;
                                if let Ok(wrapped) =
                                    rc_any.downcast::<
                                        crate
                                        ::artifact
                                        ::ExporterDataAny<
                                            dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>,
                                        >,
                                    >() {
                                    *iface_for_finalizer.set.borrow_mut() = Some(wrapped.0.clone());
                                }
                            }
                            let methods_borrow = iface_for_finalizer.methods.borrow();
                            for (i, m_serial) in methods_serial.iter().enumerate() {
                                let m = &methods_borrow[i];
                                if let Some(v) =
                                    crate::artifact::independent_finalize_deserialize_type_artifact(
                                        xx,
                                        loc,
                                        m_serial.arg_type.clone(),
                                    ).v {
                                    *m.arg_type.borrow_mut() = Some(v);
                                }
                                if let Some(v) =
                                    crate::artifact::independent_finalize_deserialize_type_artifact(
                                        xx,
                                        loc,
                                        m_serial.ret_type.clone(),
                                    ).v {
                                    *m.ret_type.borrow_mut() = Some(v);
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
        let self_ = self_.downcast::<TypeSourceInterfaceConst>().expect("expected TypeSourceInterfaceConst");
        let mut errors: Option<Vec<crate::langhelp::ExprErr>> = None;
        let mut methods_serial: Vec<ArtifactDataDefSetInterfaceMethod> = Vec::new();
        for m in self_.data.methods.borrow().iter() {
            let arg_type = m.arg_type.borrow().clone().unwrap_or_else(|| {
                Rc::new(TypeNullDynamic) as Rc<dyn Type>
            });
            let ret_type = m.ret_type.borrow().clone().unwrap_or_else(|| {
                Rc::new(TypeNullDynamic) as Rc<dyn Type>
            });
            let arg_art = crate::artifact::serialize_artifact(xx, loc, arg_type.as_value().as_ref());
            if let Some(errors1) = arg_art.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            let Some(arg_art) = arg_art.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            let ret_art = crate::artifact::serialize_artifact(xx, loc, ret_type.as_value().as_ref());
            if let Some(errors1) = ret_art.errors {
                errors.get_or_insert_default().extend(errors1);
            }
            let Some(ret_art) = ret_art.v else {
                return EResult {
                    errors,
                    v: None,
                };
            };
            methods_serial.push(ArtifactDataDefSetInterfaceMethod {
                name: m.name.clone(),
                field_name: m.field_name.clone(),
                arg_type: arg_art,
                ret_type: ret_art,
            });
        }
        let set_const =
            super::super::type_source_exports::r#const::TypeSourceExportsLiftedConst {
                set: self_.data.set.borrow().as_ref().unwrap().clone(),
            };
        let def_set_art = crate::artifact::serialize_artifact(xx, loc, &set_const as &dyn Value);
        if let Some(errors1) = def_set_art.errors {
            errors.get_or_insert_default().extend(errors1);
        }
        let Some(def_set_art) = def_set_art.v else {
            return EResult {
                errors,
                v: None,
            };
        };
        let art = impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.artifact_id, || {
            let body = ArtifactDataDefSetInterface {
                identity: self_.data.identity.clone(),
                description: self_.data.description.borrow().clone(),
                is_aspect: self_.data.is_aspect.get(),
                builtin_kind: self_.data.builtin_kind,
                methods: methods_serial,
                def_set_id: self_.data.set.borrow().as_ref().unwrap().def_set_id,
                def_set: def_set_art,
            };
            make_body(serde_json::to_value(&body).unwrap())
        });
        return EResult {
            errors,
            v: Some(art),
        };
    }
}

impl Type for TypeSourceInterfaceConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == interface_const_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceConstAspectTypeImpl) as Box<dyn AspectType>));
        }
        if *key == aspect_aspect_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectAspect>),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectArtifact>),
            );
        }
        if *key == aspect_field_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectField>),
            );
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        let identity = self.data.identity.clone();
        source_type.aspect(xx, &identity).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let identity = self.data.identity.clone();
        let source_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }),
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &type_type_identity()).is_some() {
            let dyn_cv = CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(super::dynamic::TypeSourceInterfaceDynamic),
                }),
            };
            return super::dynamic::TypeSourceInterfaceDynamic.cast_to(xx, loc, dyn_cv, type_);
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("interface"),
        }
    }
}

impl Value for TypeSourceInterfaceConst {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(self.clone());
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceInterfaceConst))
    }
}
pub(crate) struct TypeSourceInterfaceConstAspectTypeImpl;

impl Aspect for TypeSourceInterfaceConstAspectTypeImpl { }

impl AspectType for TypeSourceInterfaceConstAspectTypeImpl {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(super::dynamic::TypeSourceInterfaceDynamic)
    }
}
