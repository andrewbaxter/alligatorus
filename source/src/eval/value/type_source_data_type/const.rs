use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_lift_dynamic_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            definition_set::DefinitionSet,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_interface::{
                    SourceInterfaceData,
                    TypeSourceInterfaceConst,
                },
                type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
                value_source_data_type::r#const::ValueSourceDataTypeConst,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Artifact,
            ArtifactId,
            ArtifactIdBuiltin,
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
        rc::Rc,
    },
    super::{
        TypeSourceDataTypeConst,
        artifact::{
            ArtifactDataDefSet,
            ArtifactDataDefSetDataType,
            ArtifactDataDefSetMethod,
        },
    },
};

pub(crate) fn defset_artifact_identity() -> ArtifactIdBuiltin {
    builtin_artifact_id!()
}

fn serialize_definition_set(
    xx: &ExprContext,
    loc: QExprId,
    set: &Gc<DefinitionSet>,
) -> ArtifactId {
    crate::langhelp::produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &set.artifact_id, || {
        use crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref;

        let def_set_artifact_id = set.artifact_id.borrow().clone().unwrap();
        let data_types = set.data_types.borrow();
        let data_type_ids: Vec<ArtifactId> = data_types.iter().map(|dt| {
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
                let serial = ArtifactDataDefSetDataType {
                    name: dt.name.clone(),
                    data_field: dt.data_field.clone(),
                    underlying_type: serialize_type_ref(xx, loc, underlying_type.as_ref()),
                    identity: dt.identity.clone(),
                    methods,
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
        };
        Artifact {
            type_: ArtifactId::Builtin(defset_artifact_identity()),
            data: serde_json::to_value(&serial).expect("Failed to serialize definition set"),
        }
    })
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
    fn from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        let dt_data = self.data.clone();
        let data = data.clone();
        _xx.vx.ix.pending_finalizers.borrow_mut().push(Box::new(move |xx, loc| {
            let underlying_type = dt_data.underlying_type.get();
            let build_type_artifact =
                crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                    xx,
                    loc,
                    underlying_type.as_ref(),
                );
            drop(underlying_type);
            let underlying_artifact = Artifact {
                type_: build_type_artifact.type_,
                data: data,
            };
            let underlying_cv =
                crate::eval::util::deserialize_artifact(xx, loc, &underlying_artifact)
                    .into_res()
                    .map_err(|e| format!("Failed to deserialize underlying value: {:?}", e))?;
            {
                let set = &dt_data.set;
                let mut sets = xx.vx.definition_sets.borrow_mut();
                let already = sets.iter().any(|s| s.def_set_id == set.def_set_id);
                if !already {
                    sets.push(set.clone());
                }
            }
            let underlying_value_type = underlying_cv.value.type_(xx, loc);
            let aspect_str =
                aspect_string_const_get(xx, loc, underlying_value_type.as_ref())
                    .into_res()
                    .map_err(|e| format!("Failed to get string aspect: {:?}", e))?;
            let s = aspect_str.get(underlying_cv.value.as_ref());
            let underlying_js = CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::Literal(serde_json::Value::String(s)),
            };
            let mut object_fields = vec![(dt_data.data_field.clone(), underlying_js)];
            for method in dt_data.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
                object_fields.push((method.field_name.clone(), CodeDynamic {
                    loc: loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
                }));
            }
            let code = CodeDynamic {
                loc: loc,
                type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: object_fields }),
            };
            Ok(vec![Code {
                loc: loc,
                code: Code_::Dyn(Rc::new(code)),
            }])
        }));
        EvalRes::ok(CodeValue {
            code: Code {
                loc: loc,
                code: Code_::None,
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: Some(self.data.clone()) }),
            }),
        })
    }

    fn to_serial(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let lifted = must_downcast_ref::<ValueSourceDataTypeConst>(self_.as_ref());

        let underlying_type = self.data.underlying_type.get();

        let underlying_aspect = match crate::builtin::aspect_artifact_get_maybe(xx, underlying_type.as_ref()) {
            Some(a) => a,
            None => {
                return EResult::err(
                    loc,
                    format!("Underlying type ({}) missing AspectArtifact", underlying_type.desc()),
                );
            },
        };
        let underlying_artifact =
            match underlying_aspect.to_serial(xx, loc, lifted.underlying.clone()).into_res() {
                Ok(a) => a,
                Err(e) => {
                    return EResult::err(loc, format!("Failed to serialize underlying value: {:?}", e));
                },
            };

        let data_type_artifact_id = self.serialize_data_type(xx, loc);

        EResult::ok(Artifact {
            type_: data_type_artifact_id,
            data: underlying_artifact.data,
        })
    }
}

impl AspectBind for TypeSourceDataTypeConst {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
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

// --- AspectLiftDynamic: lift a data type instance from v8 ---
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
        let data_field = self.data.data_field.clone();
        let key = v8::String::new(scope, &data_field).unwrap();
        let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
        let underlying_value = underlying_lift.lift(xx, scope, underlying_v8)?;
        Ok(Rc::new(ValueSourceDataTypeConst {
            underlying: underlying_value,
            data: self.data.clone(),
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
                type_: Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }),
            }),
        }
    }
}

impl Type for TypeSourceDataTypeConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeConst { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        // Check own data type identity aspect
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

        // Check interface identity aspects
        {
            let interface_impls = self.data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if *key == impl_data.interface.identity {
                    return Some(Box::new(true));
                }
            }
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        let (own_identity, iface_identities) = {
            let own = self.data.identity.clone();
            let interface_impls = self.data.interface_impls.borrow();
            let ifaces: Vec<crate::langhelp::Unique> = interface_impls.iter().map(|impl_data| {
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
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        // Extract own identity + interface identities
        let (own_identity, iface_data) = {
            let own = self.data.identity.clone();
            let interface_impls = self.data.interface_impls.borrow();
            let ifaces: Vec<(Gc<SourceInterfaceData>, crate::langhelp::Unique)> =
                interface_impls.iter().map(|impl_data| {
                    (impl_data.interface.clone(), impl_data.interface.identity.clone())
                }).collect();
            (own, ifaces)
        };

        // Same data type check
        if type_.aspect(xx, &own_identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }),
                }),
            });
        }

        // Interface casting
        for (iface_data, identity) in iface_data {
            if type_.aspect(xx, &identity).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceInterfaceConst { data: iface_data }),
                    }),
                });
            }
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("data_type({})", self.data.name.as_deref().unwrap_or("?")),
        }
    }
}

impl Value for TypeSourceDataTypeConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTypeSourceDataTypeDynamic)
    }
}
