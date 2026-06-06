use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_artifact_aspect_id,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_lift_js_get_maybe,
            aspect_string_get,
        },
        eval::{
            Erb,
            mortar_function::MortarTypeFunction,
            mortar_js_value::{
                MortarJsValue,
                MortarJsValueSource,
                bind_js_some,
            },
            mortar_null::TypeMortarNull,
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
            JsCodeFieldUse,
            JsCodeNewObject,
            JsCodeType,
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
        cell::RefCell,
        mem,
        rc::Rc,
    },
    super::{
        DefinitionSet,
        DefinitionSetDataType,
        DefinitionSetInterface,
    },
};

impl Value for DefinitionSetDataType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::super::type_ref::TypeAny)
    }
}

impl Type for DefinitionSetDataType {
    fn desc(&self) -> String {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        match dt.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("data_type(index={})", self.data_type_index),
        }
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(DefinitionSetDataType {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        })
    }

    fn type_to_serial(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId) -> Option<Artifact> {
        let artifact_id = self.serialize_data_type(xx, gx, loc);
        Some(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        // Check own data type identity aspect
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            if *key == dt.identity {
                return Some(Box::new(true));
            }
        }
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectLiftJs>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectArtifact>));
        }

        // Check interface identity aspects
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let interface_impls = dt.interface_impls.borrow();
            let interfaces = self.set.interfaces.borrow();
            for impl_data in interface_impls.iter() {
                let iface = &interfaces[impl_data.interface_index];
                if *key == iface.identity {
                    return Some(Box::new(true));
                }
            }
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        let (own_identity, iface_data) = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let own = dt.identity.clone();
            let interface_impls = dt.interface_impls.borrow();
            let interfaces = self.set.interfaces.borrow();
            let ifaces: Vec<crate::langhelp::Unique> = interface_impls.iter().map(|impl_data| {
                let iface = &interfaces[impl_data.interface_index];
                iface.identity.clone()
            }).collect();
            (own, ifaces)
        };
        if type_.aspect(xx, &own_identity).is_some() {
            return true;
        }
        for identity in iface_data {
            if type_.aspect(xx, &identity).is_some() {
                return true;
            }
        }
        false
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        // Extract own identity + interface identities (drop borrows before calling
        // aspect())
        let (own_identity, iface_data) = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let own = dt.identity.clone();
            let interface_impls = dt.interface_impls.borrow();
            let interfaces = self.set.interfaces.borrow();
            let ifaces: Vec<(usize, crate::langhelp::Unique)> = interface_impls.iter().map(|impl_data| {
                let iface = &interfaces[impl_data.interface_index];
                (impl_data.interface_index, iface.identity.clone())
            }).collect();
            (own, ifaces)
        };

        // Same data type check
        if type_.aspect(xx, &own_identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(DefinitionSetDataType {
                        set: self.set.clone(),
                        data_type_index: self.data_type_index,
                    }),
                }),
            });
        }

        // Interface casting
        for (interface_index, identity) in iface_data {
            if type_.aspect(xx, &identity).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(MortarJsValue {
                        source: MortarJsValueSource::Expr,
                        type_: Rc::new(DefinitionSetInterface {
                            set: self.set.clone(),
                            interface_index,
                        }),
                    }),
                });
            }
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }
}

impl Aspect for DefinitionSetDataType { }

impl AspectConsumeJs for DefinitionSetDataType {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for DefinitionSetDataType {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn crate::eval::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectJsStackValue for DefinitionSetDataType {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(DefinitionSetDataType {
                    set: self.set.clone(),
                    data_type_index: self.data_type_index,
                }),
            }),
        }
    }
}

impl AspectField for DefinitionSetDataType {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());

            // Look up method by name - get types directly
            type MethodLookup = (String, Rc<dyn Type>, Rc<dyn Type>);
            let lookup_result: Option<MethodLookup> = 'lookup: {
                let data_types = self.set.data_types.borrow();
                let dt = &data_types[self.data_type_index];

                // Check direct methods first
                {
                    let methods = dt.methods.borrow();
                    if let Some(m) = methods.iter().find(|m| m.name == key_name) {
                        let ret_t = m.ret_type.borrow().clone().expect("Data type method missing return type");
                        break 'lookup Some((m.field_name.clone(), Rc::new(TypeMortarNull), ret_t));
                    }
                }

                // Check interface methods
                {
                    let interface_impls = dt.interface_impls.borrow();
                    for impl_data in interface_impls.iter() {
                        let interfaces = self.set.interfaces.borrow();
                        let iface = &interfaces[impl_data.interface_index];
                        let iface_methods = iface.methods.borrow();
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
            let self_code = match erb.must_er(self_.code.cast::<JsCode>())? {
                Some(c) => c.clone(),
                None => {
                    // Constant value (e.g. deserialized import) — materialize as JS object
                    erb.must_res(self.materialize_constant(xx, loc, &self_))?
                },
            };
            let field_type = Rc::new(MortarTypeFunction {
                arg: arg_type,
                ret: ret_type,
            });
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::FieldUse(JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: field_name,
                        }),
                    })),
                },
                value: Rc::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];

        // Check direct methods
        let methods = dt.methods.borrow();
        for method in methods.iter() {
            if method.name == key {
                return Some(Rc::new(MortarTypeFunction {
                    arg: Rc::new(TypeMortarNull),
                    ret: Rc::new(TypeMortarNull),
                }));
            }
        }
        drop(methods);

        // Check interface methods
        let interface_impls = dt.interface_impls.borrow();
        for impl_data in interface_impls.iter() {
            let interfaces = self.set.interfaces.borrow();
            let iface = &interfaces[impl_data.interface_index];
            let iface_methods = iface.methods.borrow();
            for method in iface_methods.iter() {
                if method.name == key {
                    return Some(Rc::new(MortarTypeFunction {
                        arg: Rc::new(TypeMortarNull),
                        ret: Rc::new(TypeMortarNull),
                    }));
                }
            }
        }
        None
    }
}

// --- AspectLiftJs: lift a data type instance from v8 ---
impl AspectLiftJs for DefinitionSetDataType {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let underlying_type = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            dt.underlying_type.borrow().as_ref().expect("Data type missing underlying type").clone()
        };
        let underlying_lift =
            aspect_lift_js_get_maybe(
                xx,
                underlying_type.as_ref(),
            ).ok_or_else(|| format!("Underlying type ({}) missing AspectLiftJs", underlying_type.desc()))?;
        let obj = v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lift")?;
        let data_field = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            dt.data_field.clone()
        };
        let key = v8::String::new(scope, &data_field).unwrap();
        let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
        let underlying_value = underlying_lift.lift(xx, scope, underlying_v8)?;
        Ok(Rc::new(LiftedDefSetDataTypeValue {
            underlying: underlying_value,
            set: self.set.clone(),
            data_type_index: self.data_type_index,
            artifact_id: RefCell::new(None),
        }))
    }
}

// --- LiftedDefSetDataTypeValue: value produced by lifting a data type instance
// from v8 ---
pub(crate) struct LiftedDefSetDataTypeValue {
    pub underlying: Rc<dyn Value>,
    pub set: Gc<DefinitionSet>,
    pub data_type_index: usize,
    pub artifact_id: RefCell<Option<ArtifactId>>,
}

impl Value for LiftedDefSetDataTypeValue {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(DefinitionSetDataType {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        })
    }
}

// --- AspectArtifact: serialize/deserialize data type instances ---
pub(crate) fn defset_data_type_artifact_identity() -> ArtifactIdBuiltin {
    builtin_artifact_id!()
}

impl AspectArtifact for DefinitionSetDataType {
    fn to_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let lifted =
            (self_.as_ref() as &dyn Any)
                .downcast_ref::<LiftedDefSetDataTypeValue>()
                .expect("DefinitionSetDataType::to_serial expects LiftedDefSetDataTypeValue");

        // Get the underlying type from the definition set (mortar type)
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        let underlying_type = dt.underlying_type.borrow();
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

    fn from_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        // Get the underlying type from the definition set to recover the artifact identity
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        let underlying_type = dt.underlying_type.borrow();
        let underlying_type =
            underlying_type.as_ref().ok_or_else(|| "Data type missing underlying type".to_string())?;

        // Get the artifact identity from the underlying type's type_to_serial
        let type_ref_artifact =
            underlying_type
                .type_to_serial(xx, gx, loc)
                .ok_or_else(
                    || format!("Underlying type ({}) does not support type_to_serial", underlying_type.desc()),
                )?;

        // Reconstruct the full underlying artifact and deserialize via the value type
        let underlying_artifact = Artifact {
            type_: type_ref_artifact.type_,
            data: data.clone(),
        };
        let underlying_value = gx.deserialize_artifact(xx, loc, &underlying_artifact)?;
        Ok(Rc::new(LiftedDefSetDataTypeValue {
            underlying: underlying_value,
            set: self.set.clone(),
            data_type_index: self.data_type_index,
            artifact_id: RefCell::new(None),
        }))
    }
}

impl DefinitionSetDataType {
    /// Materialize a constant (lifted/deserialized) data type value as a JS object.
    /// Registers the definition set with the eval context so methods get compiled.
    fn materialize_constant(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> Result<JsCode, String> {
        use crate::builtin::aspect_string_get_maybe;

        // Register definition set with eval context
        {
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already =
                sets
                    .iter()
                    .any(|s| std::ptr::eq(&**s as *const DefinitionSet, &*self.set as *const DefinitionSet));
            if !already {
                sets.push(self.set.clone());
            }
        }

        // Get the underlying value from LiftedDefSetDataTypeValue
        let lifted =
            (cv.value.as_ref() as &dyn std::any::Any)
                .downcast_ref::<LiftedDefSetDataTypeValue>()
                .ok_or_else(|| "Expected LiftedDefSetDataTypeValue for constant materialization".to_string())?;

        // Build the underlying value as a JS literal
        let underlying_type = lifted.underlying.type_(xx, loc);
        let underlying_js = if let Some(aspect_str) = aspect_string_get_maybe(xx, underlying_type.as_ref()) {
            let s = aspect_str.get(lifted.underlying.as_ref());
            JsCode {
                loc,
                type_: JsCodeType::Literal(serde_json::Value::String(s)),
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
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        let mut object_fields = vec![(dt.data_field.clone(), underlying_js)];
        for method in dt.methods.borrow().iter() {
            let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
            object_fields.push((method.field_name.clone(), JsCode {
                loc,
                type_: JsCodeType::BindingUse(JsCodeBindingUse { id: method_id }),
            }));
        }
        Ok(JsCode {
            loc,
            type_: JsCodeType::NewObject(JsCodeNewObject { fields: object_fields }),
        })
    }

    fn serialize_data_type(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId) -> ArtifactId {
        // Check if already serialized
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            if let Some(existing_id) = dt.artifact_id.borrow().clone() {
                return existing_id;
            }
        }

        // First serialize the definition set
        let def_set_artifact_id = serialize_definition_set(xx, gx, loc, &self.set);

        // Reserve an ID for the data type
        let dt_artifact_id = gx.artifacts.reserve_id();
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            *dt.artifact_id.borrow_mut() = Some(dt_artifact_id.clone());
        }
        let serial = SerialDefSetDataType {
            def_set: def_set_artifact_id,
            type_index: self.data_type_index,
        };
        let json = serde_json::to_value(&serial).expect("Failed to serialize data type");
        gx.artifacts.set(dt_artifact_id.clone(), Artifact {
            type_: ArtifactId::Builtin(defset_data_type_artifact_identity()),
            data: json,
        });
        dt_artifact_id
    }
}

// --- Serializable data structures ---
#[derive(serde::Serialize, serde::Deserialize)]
struct SerialDefSetDataType {
    def_set: ArtifactId,
    type_index: usize,
}

#[derive(serde::Serialize, serde::Deserialize)]
struct SerialDefSet {
    data_types: Vec<SerialDefSetDataTypeEntry>,
    import_id: crate::langhelp::ImportStateId,
    def_set_id: crate::langhelp::DefSetId,
}

#[derive(serde::Serialize, serde::Deserialize)]
struct SerialDefSetDataTypeEntry {
    name: Option<String>,
    underlying_type: Artifact,
    identity: super::data_type_constructor::SerialUnique,
    data_field: String,
    methods: Vec<SerialDefSetMethod>,
}

#[derive(serde::Serialize, serde::Deserialize)]
struct SerialDefSetMethod {
    name: String,
    field_name: String,
    ret_type: Artifact,
    compiled_js: String,
}

// --- Definition set serialization ---
pub(crate) fn defset_artifact_identity() -> ArtifactIdBuiltin {
    builtin_artifact_id!()
}

fn serialize_definition_set(
    xx: &ExprContext,
    gx: &GlobalContext,
    loc: QExprId,
    set: &Gc<DefinitionSet>,
) -> ArtifactId {
    use super::data_type_constructor::{
        serialize_type_ref,
        unique_to_serial,
    };

    let data_types = set.data_types.borrow();
    let serial_data_types: Vec<SerialDefSetDataTypeEntry> = data_types.iter().map(|dt| {
        let underlying_type = dt.underlying_type.borrow();
        let underlying_type = underlying_type.as_ref().expect("Data type missing underlying type");
        let methods: Vec<SerialDefSetMethod> = dt.methods.borrow().iter().map(|m| {
            let compiled_js = m.compiled_js.borrow();
            let compiled_js = compiled_js.as_ref().expect("Method missing compiled JS");
            let ret_type = m.ret_type.borrow().clone().expect("Method missing ret type");
            SerialDefSetMethod {
                name: m.name.clone(),
                field_name: m.field_name.clone(),
                ret_type: serialize_type_ref(xx, gx, loc, ret_type.as_ref()),
                compiled_js: compiled_js.clone(),
            }
        }).collect();
        SerialDefSetDataTypeEntry {
            name: dt.name.clone(),
            underlying_type: serialize_type_ref(xx, gx, loc, underlying_type.as_ref()),
            identity: unique_to_serial(&dt.identity),
            data_field: dt.data_field.clone(),
            methods,
        }
    }).collect();
    let serial = SerialDefSet {
        data_types: serial_data_types,
        import_id: set.import_id,
        def_set_id: set.def_set_id,
    };
    let json = serde_json::to_value(&serial).expect("Failed to serialize definition set");
    let artifact_id = gx.artifacts.reserve_id();
    gx.artifacts.set(artifact_id.clone(), Artifact {
        type_: ArtifactId::Builtin(defset_artifact_identity()),
        data: json,
    });
    artifact_id
}

/// Deserialize a definition set from its stored artifact data.
pub(crate) fn deserialize_defset_data_type(
    xx: &ExprContext,
    gx: &GlobalContext,
    loc: QExprId,
    data: &serde_json::Value,
) -> Result<Rc<dyn Type>, String> {
    let serial: SerialDefSetDataType =
        serde_json::from_value(data.clone()).map_err(|e| format!("Failed to parse defset data type: {}", e))?;

    // Reconstruct the definition set from its artifact
    let def_set_artifact = gx.artifacts.get(serial.def_set)?;
    let def_set = deserialize_definition_set(xx, gx, loc, &def_set_artifact)?;
    Ok(Rc::new(DefinitionSetDataType {
        set: def_set,
        data_type_index: serial.type_index,
    }))
}

fn deserialize_definition_set(
    xx: &ExprContext,
    gx: &GlobalContext,
    loc: QExprId,
    artifact: &Artifact,
) -> Result<Gc<DefinitionSet>, String> {
    use super::{
        DefinitionSetDataMethodData,
        DefinitionSetDataTypeData,
        data_type_constructor::serial_to_unique,
    };

    let serial: SerialDefSet =
        serde_json::from_value(
            artifact.data.clone(),
        ).map_err(|e| format!("Failed to parse definition set: {}", e))?;
    let data_types: Vec<DefinitionSetDataTypeData> = serial.data_types.iter().enumerate().map(|(dt_i, dt)| {
        let underlying_type =
            gx
                .deserialize_type_ref(xx, loc, &dt.underlying_type)
                .unwrap_or_else(|e| panic!("Failed to deserialize underlying type for data type {}: {}", dt_i, e));
        let methods: Vec<DefinitionSetDataMethodData> = dt.methods.iter().enumerate().map(|(m_i, m)| {
            let ret_type =
                gx
                    .deserialize_type_ref(xx, loc, &m.ret_type)
                    .unwrap_or_else(|e| panic!("Failed to deserialize method ret type: {}", e));
            DefinitionSetDataMethodData {
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
        DefinitionSetDataTypeData {
            name: dt.name.clone(),
            description: std::cell::RefCell::new(None),
            underlying_type: std::cell::RefCell::new(Some(underlying_type)),
            methods: std::cell::RefCell::new(methods),
            interface_impls: std::cell::RefCell::new(Vec::new()),
            identity: serial_to_unique(&dt.identity),
            data_field: dt.data_field.clone(),
            artifact_id: std::cell::RefCell::new(None),
        }
    }).collect();
    Ok(Gc::new(DefinitionSet {
        import_id: serial.import_id,
        def_set_id: serial.def_set_id,
        functions: std::cell::RefCell::new(Vec::new()),
        data_types: std::cell::RefCell::new(data_types),
        interfaces: std::cell::RefCell::new(Vec::new()),
        finished: std::cell::Cell::new(true),
        next_field_id: crate::langhelp::Counter::default(),
        next_entry_id: crate::langhelp::Counter::default(),
        next_interface_method_id: crate::langhelp::Counter::default(),
    }))
}
