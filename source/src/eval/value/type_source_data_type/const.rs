use {
    crate::{
        artifact::{
            Artifact,
            ArtifactBody,
            ArtifactId,
            ExporterAny,
            deserialize_artifact,
            impl_serialize_independent_artifact,
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
struct ArtifactNewSourceDataType {
    name: Option<String>,
    identity: Unique,
    data_field: String,
}

impl ExporterRootDelegate for TypeSourceDataTypeConst {
    fn self_from_artifact_data(
        _xx: &ExprContext,
        _loc: QExprId,
        _data: serde_json::Value,
    ) -> EResult<Rc<dyn Any>> {
        unimplemented!()
    }

    fn self_to_serial(
        xx: &ExprContext,
        _loc: QExprId,
        self_: Rc<dyn Any>,
        make_body: Box<dyn FnOnce(serde_json::Value) -> ArtifactBody>,
    ) -> EResult<Artifact> {
        let self_ = self_.downcast::<TypeSourceDataTypeConst>().expect("expected TypeSourceDataTypeConst");
        let art =
            impl_serialize_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self_.data.new_artifact_id, || {
                let body = ArtifactNewSourceDataType {
                    name: self_.data.name.clone(),
                    identity: self_.data.identity.clone(),
                    data_field: self_.data.data_field.clone(),
                };
                make_body(serde_json::to_value(&body).unwrap())
            });
        EResult::ok(art)
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
