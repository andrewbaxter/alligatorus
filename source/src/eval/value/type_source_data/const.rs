use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_lift_dynamic_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicFieldUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            Erb,
            definition_set::DefinitionSet,
            value::{
                dynamic_util::{
                    ValueDynamic,
                    ValueDynamicSource,
                    bind_dynamic_default,
                    bind_via_target,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_source_interface::{
                    SourceInterfaceData,
                    TypeSourceInterfaceConst,
                },
                type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
            },
        },
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
            GlobalContext,
            QExprId,
            Type,
            Unique,
            Value,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::{
        SourceDataTypeData,
        TypeSourceDataTypeConst,
        artifact::{
            ArtifactDataDefSet,
            ArtifactDataDefSetDataType,
            ArtifactDataDefSetDataTypeEntry,
            ArtifactDataDefSetMethod,
        },
    },
};

// --- Definition set serialization ---
pub(crate) fn defset_artifact_identity() -> ArtifactIdBuiltin {
    builtin_artifact_id!()
}

pub(crate) fn deserialize_definition_set(
    xx: &ExprContext,
    gx: &GlobalContext,
    loc: QExprId,
    artifact: &Artifact,
) -> Result<Gc<DefinitionSet>, String> {
    use crate::eval::value::type_source_data::SourceDataMethodData;

    let serial: ArtifactDataDefSet =
        serde_json::from_value(
            artifact.data.clone(),
        ).map_err(|e| format!("Failed to parse definition set: {}", e))?;
    let set = Gc::new(DefinitionSet {
        artifact_id: std::cell::RefCell::new(None),
        import_id: serial.import_id,
        def_set_id: serial.def_set_id,
        functions: std::cell::RefCell::new(Vec::new()),
        data_types: std::cell::RefCell::new(Vec::new()),
        interfaces: std::cell::RefCell::new(Vec::new()),
        finished: std::cell::Cell::new(true),
        next_field_id: crate::langhelp::Counter::default(),
        next_entry_id: crate::langhelp::Counter::default(),
        next_interface_method_id: crate::langhelp::Counter::default(),
    });
    let data_types: Vec<Gc<SourceDataTypeData>> = serial.data_types.iter().enumerate().map(|(dt_i, dt)| {
        let underlying_type =
            gx
                .deserialize_type_ref(xx, loc, &dt.underlying_type)
                .unwrap_or_else(|e| panic!("Failed to deserialize underlying type for data type {}: {}", dt_i, e));
        let methods: Vec<SourceDataMethodData> = dt.methods.iter().enumerate().map(|(m_i, m)| {
            let ret_type =
                gx
                    .deserialize_type_ref(xx, loc, &m.ret_type)
                    .unwrap_or_else(|e| panic!("Failed to deserialize method ret type: {}", e));
            SourceDataMethodData {
                name: m.name.clone(),
                impl_expr: std::cell::RefCell::new(None),
                ret_type: std::cell::RefCell::new(Some(ret_type)),
                scope_entry_id: std::cell::Cell::new(
                    Some(crate::langhelp::ScopeEntryId::Def(serial.import_id, serial.def_set_id, m_i)),
                ),
                self_scope_entry_id: std::cell::Cell::new(None),
                field_name: m.field_name.clone(),
                compiled_js: std::cell::RefCell::new(Some(m.compiled_js.clone())),
            }
        }).collect();
        Gc::new(SourceDataTypeData {
            set: set.clone(),
            name: dt.name.clone(),
            description: std::cell::RefCell::new(None),
            underlying_type: std::cell::RefCell::new(Some(underlying_type)),
            methods: std::cell::RefCell::new(methods),
            interface_impls: std::cell::RefCell::new(Vec::new()),
            identity: dt.identity.clone(),
            data_field: dt.data_field.clone(),
            artifact_id: std::cell::RefCell::new(None),
        })
    }).collect();
    *set.data_types.borrow_mut() = data_types;
    Ok(set)
}

fn serialize_definition_set(
    xx: &ExprContext,
    gx: &GlobalContext,
    loc: QExprId,
    set: &Gc<DefinitionSet>,
) -> ArtifactId {
    crate::langhelp::produce_independent_artifact(gx.artifacts.as_ref(), &set.artifact_id, || {
        use crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref;

        let data_types = set.data_types.borrow();
        let serial_data_types: Vec<ArtifactDataDefSetDataTypeEntry> = data_types.iter().map(|dt| {
            let underlying_type = dt.underlying_type.borrow();
            let underlying_type = underlying_type.as_ref().expect("Data type missing underlying type");
            let methods: Vec<ArtifactDataDefSetMethod> = dt.methods.borrow().iter().map(|m| {
                let compiled_js = m.compiled_js.borrow();
                let compiled_js = compiled_js.as_ref().expect("Method missing compiled JS");
                let ret_type = m.ret_type.borrow().clone().expect("Method missing ret type");
                ArtifactDataDefSetMethod {
                    name: m.name.clone(),
                    field_name: m.field_name.clone(),
                    ret_type: serialize_type_ref(xx, gx, loc, ret_type.as_ref()),
                    compiled_js: compiled_js.clone(),
                }
            }).collect();
            ArtifactDataDefSetDataTypeEntry {
                name: dt.name.clone(),
                underlying_type: serialize_type_ref(xx, gx, loc, underlying_type.as_ref()),
                identity: dt.identity.clone(),
                data_field: dt.data_field.clone(),
                methods,
            }
        }).collect();
        let serial = ArtifactDataDefSet {
            data_types: serial_data_types,
            import_id: set.import_id,
            def_set_id: set.def_set_id,
        };
        let json = serde_json::to_value(&serial).expect("Failed to serialize definition set");
        Artifact {
            type_: ArtifactId::Builtin(defset_artifact_identity()),
            data: json,
        }
    })
}

// --- ValueSourceDataTypeConst: value produced by lifting a data type instance
// from v8 ---
pub(crate) struct ValueSourceDataTypeConst {
    pub data: Gc<SourceDataTypeData>,
    pub underlying: Rc<dyn Value>,
}

impl Value for ValueSourceDataTypeConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeSourceDataTypeConst { data: self.data.clone() })
    }
}

impl TypeSourceDataTypeConst {
    /// Materialize a constant (lifted/deserialized) data type value as a JS object.
    /// Registers the definition set with the eval context so methods get compiled.
    fn materialize_constant(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> Result<CodeDynamic, String> {
        use crate::builtin::aspect_string_const_get_maybe;

        // Register definition set with eval context
        {
            let set = &self.data.set;
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already =
                sets.iter().any(|s| std::ptr::eq(&**s as *const DefinitionSet, &**set as *const DefinitionSet));
            if !already {
                sets.push(set.clone());
            }
        }

        // Get the underlying value from ValueSourceDataTypeConst
        let lifted =
            (cv.value.as_ref() as &dyn std::any::Any)
                .downcast_ref::<ValueSourceDataTypeConst>()
                .ok_or_else(|| "Expected ValueSourceDataTypeConst for constant materialization".to_string())?;

        // Build the underlying value as a JS literal
        let underlying_type = lifted.underlying.type_(xx, loc);
        let underlying_js = if let Some(aspect_str) = aspect_string_const_get_maybe(xx, underlying_type.as_ref()) {
            let s = aspect_str.get(lifted.underlying.as_ref());
            CodeDynamic {
                loc,
                type_: CodeDynamicType::Literal(serde_json::Value::String(s)),
            }
        } else {
            return Err(
                format!(
                    "Cannot materialize constant: underlying type ({}) not supported as JS literal",
                    underlying_type.desc()
                ),
            );
        };

        // Build wrapper object fields
        let mut object_fields = vec![(self.data.data_field.clone(), underlying_js)];
        for method in self.data.methods.borrow().iter() {
            let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
            object_fields.push((method.field_name.clone(), CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
            }));
        }
        Ok(CodeDynamic {
            loc,
            type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: object_fields }),
        })
    }

    pub(crate) fn serialize_data_type(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId) -> ArtifactId {
        crate::langhelp::produce_independent_artifact(gx.artifacts.as_ref(), &self.data.artifact_id, || {
            let def_set_artifact_id = serialize_definition_set(xx, gx, loc, &self.data.set);
            let type_index = {
                let data_types = self.data.set.data_types.borrow();
                data_types.iter().position(|dt| {
                    std::ptr::eq(&**dt as *const SourceDataTypeData, &*self.data as *const SourceDataTypeData)
                }).expect("Data type not found in its definition set")
            };
            let serial = ArtifactDataDefSetDataType {
                def_set: def_set_artifact_id,
                type_index,
            };
            let json = serde_json::to_value(&serial).expect("Failed to serialize data type");
            Artifact {
                type_: ArtifactId::Builtin(TypeTypeSourceDataTypeDynamic::artifact_id()),
                data: json,
            }
        })
    }
}

impl Aspect for TypeSourceDataTypeConst { }

impl AspectArtifact for TypeSourceDataTypeConst {
    fn from_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        // Get the underlying type from the definition set to recover the artifact identity
        let underlying_type = self.data.underlying_type.borrow();
        let underlying_type =
            underlying_type.as_ref().ok_or_else(|| "Data type missing underlying type".to_string())?;

        // Use serialize_type_ref to get the underlying type's artifact identity
        let type_ref_artifact =
            crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                xx,
                gx,
                loc,
                underlying_type.as_ref(),
            );

        // Reconstruct the full underlying artifact and deserialize via the value type
        let underlying_artifact = Artifact {
            type_: type_ref_artifact.type_,
            data: data.clone(),
        };
        let underlying_value = gx.deserialize_artifact(xx, loc, &underlying_artifact)?;
        Ok(Rc::new(ValueSourceDataTypeConst {
            underlying: underlying_value,
            data: self.data.clone(),
        }))
    }

    fn to_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let lifted =
            (self_.as_ref() as &dyn Any)
                .downcast_ref::<ValueSourceDataTypeConst>()
                .expect("TypeSourceDataTypeConst::to_serial expects ValueSourceDataTypeConst");

        // Get the underlying type from the definition set (mortar type)
        let underlying_type = self.data.underlying_type.borrow();
        let underlying_type = underlying_type.as_ref().expect("Data type missing underlying type");

        // Serialize the underlying value using the mortar type's AspectArtifact
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
            match underlying_aspect.to_serial(xx, gx, loc, lifted.underlying.clone()).into_res() {
                Ok(a) => a,
                Err(e) => {
                    return EResult::err(loc, format!("Failed to serialize underlying value: {:?}", e));
                },
            };

        // Serialize the data type itself (which includes serializing the definition set)
        let data_type_artifact_id = self.serialize_data_type(xx, gx, loc);

        // Return directly — underlying data is embedded inline for primitives
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

impl AspectField for TypeSourceDataTypeConst {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());

            // Look up method by name - get types directly
            type MethodLookup = (String, Rc<dyn Type>, Rc<dyn Type>);
            let lookup_result: Option<MethodLookup> = 'lookup: {
                // Check direct methods first
                {
                    let methods = self.data.methods.borrow();
                    if let Some(m) = methods.iter().find(|m| m.name == key_name) {
                        let ret_t = m.ret_type.borrow().clone().expect("Data type method missing return type");
                        break 'lookup Some((m.field_name.clone(), Rc::new(TypeNullDynamic), ret_t));
                    }
                }

                // Check interface methods
                {
                    let interface_impls = self.data.interface_impls.borrow();
                    for impl_data in interface_impls.iter() {
                        let iface_methods = impl_data.interface.methods.borrow();
                        if let Some(method) = iface_methods.iter().find(|m| m.name == key_name) {
                            let arg_t = method.arg_type.borrow().clone().expect("Interface method missing arg type");
                            let ret_t =
                                method.ret_type.borrow().clone().expect("Interface method missing return type");
                            break 'lookup Some((method.field_name.clone(), arg_t, ret_t));
                        }
                    }
                }
                None
            };
            let (field_name, arg_type, ret_type) = match lookup_result {
                Some(r) => r,
                None => return erb.err_new(format!("Field '{}' not found on data type", key_name)),
            };
            let self_code = match erb.must_er(self_.code.cast::<CodeDynamic>())? {
                Some(c) => c.clone(),
                None => {
                    // Constant value (e.g. deserialized import) — materialize as JS object
                    erb.must_res(self.materialize_constant(xx, loc, &self_))?
                },
            };
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
                            field: field_name,
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

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        // Check direct methods
        let methods = self.data.methods.borrow();
        for method in methods.iter() {
            if method.name == key {
                return Some(Rc::new(TypeFunctionDynamic {
                    arg: Rc::new(TypeNullDynamic),
                    ret: Rc::new(TypeNullDynamic),
                }));
            }
        }
        drop(methods);

        // Check interface methods
        let interface_impls = self.data.interface_impls.borrow();
        for impl_data in interface_impls.iter() {
            let iface_methods = impl_data.interface.methods.borrow();
            for method in iface_methods.iter() {
                if method.name == key {
                    return Some(Rc::new(TypeFunctionDynamic {
                        arg: Rc::new(TypeNullDynamic),
                        ret: Rc::new(TypeNullDynamic),
                    }));
                }
            }
        }
        None
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
        let underlying_type =
            self.data.underlying_type.borrow().as_ref().expect("Data type missing underlying type").clone();
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
        if *key == aspect_field_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectField>),
            );
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
