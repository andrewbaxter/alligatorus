use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBind,
            AspectBindJs,
            AspectConsumeJs,
            AspectFunction,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_function_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_lift_js_aspect_id,
        },
        eval::{
            Erb,
            from_v8_ref,
            mortar_js_value::{
                MortarJsValue,
                MortarJsValueSource,
                bind_js_some,
            },
            typecheck_convert,
        },
        jscode::{
            JsCode,
            JsCodeBindingUse,
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
            ScopeEntry,
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        cell::Cell,
        mem,
        rc::Rc,
    },
    super::{
        DefinitionSet,
        DefinitionSetDataMethodData,
        DefinitionSetDataType,
        DefinitionSetDataTypeConstructor,
        DefinitionSetDataTypeData,
    },
};

// --- MortarTypeDefinitionSetDataTypeConstructor: eval_half type during JS
// execution ---
pub(crate) struct MortarTypeDefinitionSetDataTypeConstructor;

impl Value for MortarTypeDefinitionSetDataTypeConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeAny)
    }
}

impl Type for MortarTypeDefinitionSetDataTypeConstructor {
    fn desc(&self) -> String {
        format!("definition_set_data_type_constructor")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(MortarTypeDefinitionSetDataTypeConstructor)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectConsumeJs>),
            );
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(
                Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectJsStackValue>),
            );
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetDataTypeConstructor) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Aspect for MortarTypeDefinitionSetDataTypeConstructor { }

impl AspectConsumeJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeDefinitionSetDataTypeConstructor {
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

impl AspectJsStackValue for MortarTypeDefinitionSetDataTypeConstructor {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetDataTypeConstructor),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetDataTypeConstructor {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let ctor_ref: Rc<DefinitionSetDataTypeConstructor> =
            from_v8_ref::<Rc<DefinitionSetDataTypeConstructor>>(&value).clone();
        ctor_ref.set.finish()?;
        Ok(Rc::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetDataTypeConstructor {
                set: ctor_ref.set.clone(),
                data_type_index: ctor_ref.data_type_index,
            }),
        }))
    }
}

// --- DefinitionSetDataTypeConstructor as Type: used after lift ---
impl Value for DefinitionSetDataTypeConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(super::super::type_ref::TypeAny)
    }
}

impl Type for DefinitionSetDataTypeConstructor {
    fn desc(&self) -> String {
        let data_types = self.set.data_types.borrow();
        let dt = &data_types[self.data_type_index];
        match dt.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_data_type_constructor"),
        }
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(DefinitionSetDataTypeConstructor {
            set: self.set.clone(),
            data_type_index: self.data_type_index,
        })
    }

    fn type_to_serial(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId) -> Option<Artifact> {
        let artifact_id = self.serialize_ctor(xx, gx, loc);
        Some(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataTypeConstructor {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectFunction>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataTypeConstructor {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectBind>));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(DefinitionSetDataTypeConstructor {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            }) as Box<dyn AspectArtifact>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert data_type_constructor"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }
}

impl Aspect for DefinitionSetDataTypeConstructor { }

impl AspectBind for DefinitionSetDataTypeConstructor {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<Box<dyn ScopeEntry>> {
        EResult::ok(
            Box::new(crate::eval::tuple::ValueScopeEntry { value: Rc::from(cv.value) }) as Box<dyn ScopeEntry>,
        )
    }
}

impl AspectFunction for DefinitionSetDataTypeConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Register with eval context (dedup)
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
        let underlying_type = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            dt.underlying_type.borrow().as_ref().expect("Data type missing underlying type").clone()
        };
        return er_seq_cv!(loc, erb, {
            // Convert argument to underlying type
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_js = erb.must_er(aspect_consume_js_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_js.to_js(xx, loc, converted))?;

            // Build wrapper object fields
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let mut object_fields = vec![(dt.data_field.clone(), underlying_js),];
            for method in dt.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
                object_fields.push((method.field_name.clone(), JsCode {
                    loc,
                    type_: JsCodeType::BindingUse(JsCodeBindingUse { id: method_id }),
                }));
            }
            for impl_data in dt.interface_impls.borrow().iter() {
                let interfaces = self.set.interfaces.borrow();
                let iface = &interfaces[impl_data.interface_index];
                let iface_methods = iface.methods.borrow();
                for method_impl in &impl_data.method_impls {
                    let iface_method = &iface_methods[method_impl.interface_method_index];
                    let method_id =
                        method_impl.scope_entry_id.get().expect("Interface method impl scope entry ID not allocated");
                    object_fields.push((iface_method.field_name.clone(), JsCode {
                        loc,
                        type_: JsCodeType::BindingUse(JsCodeBindingUse { id: method_id }),
                    }));
                }
            }
            let result_type = Rc::new(DefinitionSetDataType {
                set: self.set.clone(),
                data_type_index: self.data_type_index,
            });
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::NewObject(JsCodeNewObject { fields: object_fields }),
            }));
        });
    }
}

pub(crate) fn defset_ctor_artifact_identity() -> ArtifactIdBuiltin {
    ArtifactIdBuiltin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}

impl DefinitionSetDataTypeConstructor {
    pub(crate) fn serialize_ctor(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId) -> ArtifactId {
        // Check if already serialized
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            if let Some(existing_id) = dt.artifact_id.borrow().clone() {
                return existing_id;
            }
        }

        // Reserve an artifact ID
        let artifact_id = gx.artifacts.reserve_id();
        {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            *dt.artifact_id.borrow_mut() = Some(artifact_id.clone());
        }

        // Serialize the definition set data type info
        let serial_data = {
            let data_types = self.set.data_types.borrow();
            let dt = &data_types[self.data_type_index];
            let underlying_type = dt.underlying_type.borrow();
            let underlying_type = underlying_type.as_ref().expect("Data type missing underlying type");
            let methods: Vec<SerialMethod> = dt.methods.borrow().iter().map(|m| {
                let compiled_js = m.compiled_js.borrow();
                let compiled_js = compiled_js.as_ref().expect("Method missing compiled JS");
                let ret_type = m.ret_type.borrow().clone().expect("Method missing ret type");
                SerialMethod {
                    name: m.name.clone(),
                    field_name: m.field_name.clone(),
                    ret_type: serialize_type_ref(xx, gx, loc, ret_type.as_ref()),
                    compiled_js: compiled_js.clone(),
                }
            }).collect();
            SerialDefSetCtorData {
                name: dt.name.clone(),
                data_field: dt.data_field.clone(),
                underlying_type: serialize_type_ref(xx, gx, loc, underlying_type.as_ref()),
                identity: unique_to_serial(&dt.identity),
                methods,
                import_id: self.set.import_id,
                def_set_id: self.set.def_set_id,
            }
        };
        let json = serde_json::to_value(&serial_data).expect("Failed to serialize defset ctor data");
        let builtin_id = defset_ctor_artifact_identity();

        // Store in artifact store
        gx.artifacts.set(artifact_id.clone(), Artifact {
            type_: ArtifactId::Builtin(builtin_id),
            data: json,
        });
        artifact_id
    }
}

impl AspectArtifact for DefinitionSetDataTypeConstructor {
    fn to_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        _self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let artifact_id = self.serialize_ctor(xx, gx, loc);
        let builtin_id = defset_ctor_artifact_identity();
        EResult::ok(Artifact {
            type_: ArtifactId::Builtin(builtin_id),
            data: serde_json::to_value(&artifact_id).unwrap(),
        })
    }

    fn from_serial(
        &self,
        _xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        defset_ctor_from_serial(_xx, gx, loc, data)
    }
}

/// Deserialize a definition set data type constructor from artifact data.
pub(crate) fn deserialize_defset_ctor(
    xx: &ExprContext,
    gx: &GlobalContext,
    loc: QExprId,
    data: &serde_json::Value,
) -> Result<Rc<dyn Type>, String> {
    // data contains a serialized ArtifactId reference
    let artifact_id: ArtifactId =
        serde_json::from_value(
            data.clone(),
        ).map_err(|e| format!("Failed to parse defset ctor artifact ref: {}", e))?;
    let stored = gx.artifacts.get(artifact_id)?;
    let serial: SerialDefSetCtorData =
        serde_json::from_value(stored.data).map_err(|e| format!("Failed to parse defset ctor data: {}", e))?;
    let identity = serial_to_unique(&serial.identity);
    let underlying_type = gx.deserialize_type_ref(xx, loc, &serial.underlying_type)?;
    let methods: Vec<DefinitionSetDataMethodData> = serial.methods.iter().enumerate().map(|(i, m)| {
        let ret_type = gx.deserialize_type_ref(xx, loc, &m.ret_type).expect("Failed to resolve method ret type");
        let entry_id = serial.def_set_id;
        DefinitionSetDataMethodData {
            name: m.name.clone(),
            impl_expr: std::cell::RefCell::new(None),
            ret_type: std::cell::RefCell::new(Some(ret_type)),
            scope_entry_id: Cell::new(Some(crate::langhelp::ScopeEntryId::Def(serial.import_id, entry_id, i))),
            self_scope_entry_id: Cell::new(None),
            field_name: m.field_name.clone(),
            compiled_js: std::cell::RefCell::new(Some(m.compiled_js.clone())),
        }
    }).collect();
    let set = dumpster::unsync::Gc::new(DefinitionSet {
        import_id: serial.import_id,
        def_set_id: serial.def_set_id,
        functions: std::cell::RefCell::new(Vec::new()),
        data_types: std::cell::RefCell::new(vec![DefinitionSetDataTypeData {
            name: serial.name,
            description: std::cell::RefCell::new(None),
            underlying_type: std::cell::RefCell::new(Some(underlying_type)),
            methods: std::cell::RefCell::new(methods),
            interface_impls: std::cell::RefCell::new(Vec::new()),
            identity,
            data_field: serial.data_field,
            artifact_id: std::cell::RefCell::new(None),
        }]),
        interfaces: std::cell::RefCell::new(Vec::new()),
        finished: Cell::new(true),
        next_field_id: crate::langhelp::Counter::default(),
        next_entry_id: crate::langhelp::Counter::default(),
        next_interface_method_id: crate::langhelp::Counter::default(),
    });
    Ok(Rc::new(DefinitionSetDataTypeConstructor {
        set,
        data_type_index: 0,
    }))
}

fn defset_ctor_from_serial(
    xx: &ExprContext,
    gx: &GlobalContext,
    loc: QExprId,
    data: &serde_json::Value,
) -> Result<Rc<dyn Value>, String> {
    // data contains a serialized ArtifactId reference
    let artifact_id: ArtifactId =
        serde_json::from_value(
            data.clone(),
        ).map_err(|e| format!("Failed to parse defset ctor artifact ref: {}", e))?;
    let stored = gx.artifacts.get(artifact_id)?;
    let serial: SerialDefSetCtorData =
        serde_json::from_value(stored.data).map_err(|e| format!("Failed to parse defset ctor data: {}", e))?;

    // Reconstruct DefinitionSet
    let identity = serial_to_unique(&serial.identity);
    let underlying_type = gx.deserialize_type_ref(xx, loc, &serial.underlying_type)?;
    let methods: Vec<DefinitionSetDataMethodData> = serial.methods.iter().enumerate().map(|(i, m)| {
        let ret_type = gx.deserialize_type_ref(xx, loc, &m.ret_type).expect("Failed to resolve method ret type");
        let entry_id = serial.def_set_id;
        DefinitionSetDataMethodData {
            name: m.name.clone(),
            impl_expr: std::cell::RefCell::new(None),
            ret_type: std::cell::RefCell::new(Some(ret_type)),
            scope_entry_id: Cell::new(Some(crate::langhelp::ScopeEntryId::Def(serial.import_id, entry_id, i))),
            self_scope_entry_id: Cell::new(None),
            field_name: m.field_name.clone(),
            compiled_js: std::cell::RefCell::new(Some(m.compiled_js.clone())),
        }
    }).collect();
    let set = dumpster::unsync::Gc::new(DefinitionSet {
        import_id: serial.import_id,
        def_set_id: serial.def_set_id,
        functions: std::cell::RefCell::new(Vec::new()),
        data_types: std::cell::RefCell::new(vec![DefinitionSetDataTypeData {
            name: serial.name,
            description: std::cell::RefCell::new(None),
            underlying_type: std::cell::RefCell::new(Some(underlying_type)),
            methods: std::cell::RefCell::new(methods),
            interface_impls: std::cell::RefCell::new(Vec::new()),
            identity,
            data_field: serial.data_field,
            artifact_id: std::cell::RefCell::new(None),
        }]),
        interfaces: std::cell::RefCell::new(Vec::new()),
        finished: Cell::new(true),
        next_field_id: crate::langhelp::Counter::default(),
        next_entry_id: crate::langhelp::Counter::default(),
        next_interface_method_id: crate::langhelp::Counter::default(),
    });
    let ctor = DefinitionSetDataTypeConstructor {
        set,
        data_type_index: 0,
    };
    Ok(Rc::new(MortarJsValue {
        source: MortarJsValueSource::Expr,
        type_: Rc::new(ctor),
    }))
}

// --- Serializable data structures ---
#[derive(serde::Serialize, serde::Deserialize)]
struct SerialDefSetCtorData {
    name: Option<String>,
    data_field: String,
    underlying_type: Artifact,
    identity: SerialUnique,
    methods: Vec<SerialMethod>,
    import_id: crate::langhelp::ImportStateId,
    def_set_id: crate::langhelp::DefSetId,
}

#[derive(serde::Serialize, serde::Deserialize)]
struct SerialMethod {
    name: String,
    field_name: String,
    ret_type: Artifact,
    compiled_js: String,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) enum SerialUnique {
    Builtin {
        file: String,
        line: usize,
    },
    Compile {
        import_state: usize,
        source: usize,
        counter: usize,
    },
}

// --- Conversion helpers ---
pub(crate) fn serialize_type_ref(xx: &ExprContext, gx: &GlobalContext, loc: QExprId, type_: &dyn Type) -> Artifact {
    type_.type_to_serial(xx, gx, loc).unwrap_or_else(|| {
        panic!("Type ({}) does not support type_to_serial", type_.desc())
    })
}

pub(crate) fn unique_to_serial(u: &Unique) -> SerialUnique {
    match u {
        Unique::Builtin { file, line } => SerialUnique::Builtin {
            file: file.to_string(),
            line: *line,
        },
        Unique::Compile(compile_id, counter) => SerialUnique::Compile {
            import_state: compile_id.0.0,
            source: compile_id.1.0,
            counter: *counter,
        },
    }
}

pub(crate) fn serial_to_unique(s: &SerialUnique) -> Unique {
    match s {
        SerialUnique::Builtin { file, line } => {
            // For deserialized builtins, use Compile with a deterministic id since we can't
            // reconstruct &'static str
            Unique::Compile(
                crate::langhelp::CompileId(crate::langhelp::ImportStateId(usize::MAX), crate::langhelp::SourceId(0)),
                *line,
            )
        },
        SerialUnique::Compile { import_state, source, counter } => Unique::Compile(
            crate::langhelp::CompileId(
                crate::langhelp::ImportStateId(*import_state),
                crate::langhelp::SourceId(*source),
            ),
            *counter,
        ),
    }
}
