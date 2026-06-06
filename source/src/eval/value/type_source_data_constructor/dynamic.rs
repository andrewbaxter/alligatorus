use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBindConst,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectFunction,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            AspectType,
            aspect_artifact_aspect_id,
            aspect_bind_const_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_function_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_stack_value_dynamic_get,
            aspect_type_aspect_id,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicNewObject,
            CodeDynamicType,
        },
        eval::{
            Erb,
            definition_set::DefinitionSet,
            from_v8_ref,
            typecheck_convert,
            value::{
                type_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                    bind_dynamic_some,
                },
                type_source_data::{
                    SourceDataMethodData,
                    SourceDataType,
                    SourceDataTypeData,
                },
            },
        },
        langhelp::{
            Artifact,
            ArtifactId,
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
    super::SourceDataTypeConstructor,
};

// --- TypeTypeSourceDataTypeConstructorDynamic: the type-type for
// SourceDataTypeConstructor values ---
pub(crate) struct TypeTypeSourceDataTypeConstructorDynamic;

generate_builtin_artifact!(TypeTypeSourceDataTypeConstructorDynamic, serde_json::Value);

impl Value for TypeTypeSourceDataTypeConstructorDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeTypeSourceDataTypeConstructorDynamic {
    fn desc(&self) -> String {
        "definition_set_data_type_constructor_type".to_string()
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeTypeSourceDataTypeConstructorDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == crate::eval::value::type_any::r#const::type_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeConstructorDynamic) as Box<dyn AspectArtifact>));
        }
        if *key == aspect_type_aspect_id() {
            return Some(Box::new(Box::new(TypeTypeSourceDataTypeConstructorDynamic) as Box<dyn AspectType>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert definition_set_data_type_constructor_type".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_constructor_type".to_string())
    }
}

impl Aspect for TypeTypeSourceDataTypeConstructorDynamic { }

impl AspectArtifact for TypeTypeSourceDataTypeConstructorDynamic {
    fn to_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        self_: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        let ctor =
            (self_.as_ref() as &dyn Any)
                .downcast_ref::<SourceDataTypeConstructor>()
                .expect("TypeTypeSourceDataTypeConstructorDynamic::to_serial expects SourceDataTypeConstructor");
        let artifact_id = ctor.serialize_ctor(xx, gx, loc);
        EResult::ok(Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        })
    }

    fn from_serial(
        &self,
        xx: &ExprContext,
        gx: &GlobalContext,
        loc: QExprId,
        data: &serde_json::Value,
    ) -> Result<Rc<dyn Value>, String> {
        let type_ = deserialize_defset_ctor_as_type(xx, gx, loc, data)?;
        Ok(type_.as_value())
    }
}

impl AspectType for TypeTypeSourceDataTypeConstructorDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let ctor = (value as &dyn Any).downcast_ref::<SourceDataTypeConstructor>().unwrap();
        Rc::new(SourceDataTypeConstructor { data: ctor.data.clone() })
    }
}

// --- TypeSourceDataTypeConstructorDynamic: eval_half type during JS execution ---
pub(crate) struct TypeSourceDataTypeConstructorDynamic;

impl Value for TypeSourceDataTypeConstructorDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceDataTypeConstructorDynamic {
    fn desc(&self) -> String {
        format!("definition_set_data_type_constructor")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeConstructorDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeConstructorDynamic) as Box<dyn AspectConsumeDynamic>),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeConstructorDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeConstructorDynamic) as Box<dyn AspectStackValueDynamic>),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeConstructorDynamic) as Box<dyn AspectLiftDynamic>));
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

impl Aspect for TypeSourceDataTypeConstructorDynamic { }

impl AspectConsumeDynamic for TypeSourceDataTypeConstructorDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamic for TypeSourceDataTypeConstructorDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<CodeDynamic>, Box<dyn crate::eval::value::type_scope_entry::dynamic::DynamicScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_dynamic(xx, loc, cv).map(|c| bind_dynamic_some(loc, c, vtype))
    }
}

impl AspectStackValueDynamic for TypeSourceDataTypeConstructorDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceDataTypeConstructorDynamic),
            }),
        }
    }
}

impl AspectLiftDynamic for TypeSourceDataTypeConstructorDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let ctor_ref: Rc<SourceDataTypeConstructor> =
            from_v8_ref::<Rc<SourceDataTypeConstructor>>(&value).clone();
        ctor_ref.data.set.finish()?;
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(SourceDataTypeConstructor { data: ctor_ref.data.clone() }),
        }))
    }
}

// --- SourceDataTypeConstructor as Type: used after lift ---
impl Value for SourceDataTypeConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTypeSourceDataTypeConstructorDynamic)
    }
}

impl Type for SourceDataTypeConstructor {
    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("definition_set_data_type_constructor"),
        }
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(SourceDataTypeConstructor { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(
                Box::new(
                    Box::new(SourceDataTypeConstructor { data: self.data.clone() }) as Box<dyn AspectFunction>,
                ),
            );
        }
        if *key == aspect_bind_const_aspect_id() {
            return Some(
                Box::new(
                    Box::new(SourceDataTypeConstructor { data: self.data.clone() }) as Box<dyn AspectBindConst>,
                ),
            );
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

impl Aspect for SourceDataTypeConstructor { }

impl AspectBindConst for SourceDataTypeConstructor {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<Box<dyn ScopeEntry>> {
        EResult::ok(
            Box::new(crate::eval::value::type_tuple::ScopeEntryValue { value: Rc::from(cv.value) }) as
                Box<dyn ScopeEntry>,
        )
    }
}

impl AspectFunction for SourceDataTypeConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        // Register with eval context (dedup)
        {
            let set = &self.data.set;
            let mut sets = xx.vx.definition_sets.borrow_mut();
            let already =
                sets.iter().any(|s| std::ptr::eq(&**s as *const DefinitionSet, &**set as *const DefinitionSet));
            if !already {
                sets.push(set.clone());
            }
        }
        let underlying_type =
            self.data.underlying_type.borrow().as_ref().expect("Data type missing underlying type").clone();
        return er_seq_cv!(loc, erb, {
            // Convert argument to underlying type
            let converted = erb.must_er(typecheck_convert(xx, loc, underlying_type.as_ref(), argument))?;
            let to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, underlying_type.as_ref()))?;
            let underlying_js = erb.must_er(to_dynamic.to_dynamic(xx, loc, converted))?;

            // Build wrapper object fields
            let mut object_fields = vec![(self.data.data_field.clone(), underlying_js),];
            for method in self.data.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
                object_fields.push((method.field_name.clone(), CodeDynamic {
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
                    object_fields.push((iface_method.field_name.clone(), CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: method_id }),
                    }));
                }
            }
            let result_type = Rc::new(SourceDataType { data: self.data.clone(), });
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, result_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewObject(CodeDynamicNewObject { fields: object_fields }),
            }));
        });
    }
}

impl SourceDataTypeConstructor {
    pub(crate) fn serialize_ctor(&self, xx: &ExprContext, gx: &GlobalContext, loc: QExprId) -> ArtifactId {
        // Check if already serialized
        if let Some(existing_id) = self.data.artifact_id.borrow().clone() {
            return existing_id;
        }

        // Reserve an artifact ID
        let artifact_id = gx.artifacts.reserve_id();
        *self.data.artifact_id.borrow_mut() = Some(artifact_id.clone());

        // Serialize the definition set data type info
        let serial_data = {
            let underlying_type = self.data.underlying_type.borrow();
            let underlying_type = underlying_type.as_ref().expect("Data type missing underlying type");
            let methods: Vec<ArtifactDataDefSetCtorMethod> = self.data.methods.borrow().iter().map(|m| {
                let compiled_js = m.compiled_js.borrow();
                let compiled_js = compiled_js.as_ref().expect("Method missing compiled JS");
                let ret_type = m.ret_type.borrow().clone().expect("Method missing ret type");
                ArtifactDataDefSetCtorMethod {
                    name: m.name.clone(),
                    field_name: m.field_name.clone(),
                    ret_type: serialize_type_ref(xx, gx, loc, ret_type.as_ref()),
                    compiled_js: compiled_js.clone(),
                }
            }).collect();
            ArtifactDataDefSetCtorData {
                name: self.data.name.clone(),
                data_field: self.data.data_field.clone(),
                underlying_type: serialize_type_ref(xx, gx, loc, underlying_type.as_ref()),
                identity: unique_to_serial(&self.data.identity),
                methods,
                import_id: self.data.set.import_id,
                def_set_id: self.data.set.def_set_id,
            }
        };
        let json = serde_json::to_value(&serial_data).expect("Failed to serialize defset ctor data");

        // Store in artifact store
        gx.artifacts.set(artifact_id.clone(), Artifact {
            type_: ArtifactId::Builtin(TypeTypeSourceDataTypeConstructorDynamic::artifact_id()),
            data: json,
        });
        artifact_id
    }
}

fn deserialize_defset_ctor_as_type(
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
    let serial: ArtifactDataDefSetCtorData =
        serde_json::from_value(stored.data).map_err(|e| format!("Failed to parse defset ctor data: {}", e))?;
    let identity = serial_to_unique(&serial.identity);
    let underlying_type = gx.deserialize_type_ref(xx, loc, &serial.underlying_type)?;
    let methods: Vec<SourceDataMethodData> = serial.methods.iter().enumerate().map(|(i, m)| {
        let ret_type = gx.deserialize_type_ref(xx, loc, &m.ret_type).expect("Failed to resolve method ret type");
        let entry_id = serial.def_set_id;
        SourceDataMethodData {
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
        data_types: std::cell::RefCell::new(Vec::new()),
        interfaces: std::cell::RefCell::new(Vec::new()),
        finished: Cell::new(true),
        next_field_id: crate::langhelp::Counter::default(),
        next_entry_id: crate::langhelp::Counter::default(),
        next_interface_method_id: crate::langhelp::Counter::default(),
    });
    let dt_data = dumpster::unsync::Gc::new(SourceDataTypeData {
        set: set.clone(),
        name: serial.name,
        description: std::cell::RefCell::new(None),
        underlying_type: std::cell::RefCell::new(Some(underlying_type)),
        methods: std::cell::RefCell::new(methods),
        interface_impls: std::cell::RefCell::new(Vec::new()),
        identity,
        data_field: serial.data_field,
        artifact_id: std::cell::RefCell::new(None),
    });
    set.data_types.borrow_mut().push(dt_data.clone());
    Ok(Rc::new(SourceDataTypeConstructor { data: dt_data }))
}

use super::artifact::{
    ArtifactDataDefSetCtorData,
    ArtifactDataDefSetCtorMethod,
};

#[derive(serde::Serialize, serde::Deserialize)]
pub(crate) enum ArtifactDataUnique {
    Builtin {
        file: String,
        line: usize,
    },
    Source {
        import_state: usize,
        source: usize,
        counter: usize,
    },
}

// --- Conversion helpers ---
pub(crate) fn serialize_type_ref(xx: &ExprContext, gx: &GlobalContext, loc: QExprId, type_: &dyn Type) -> Artifact {
    if let Some(artifact) = type_.type_to_serial(xx, gx, loc) {
        return artifact;
    }
    let type_type = type_.type_(xx, loc);
    let aspect =
        crate::builtin::aspect_artifact_get_maybe(
            xx,
            type_type.as_ref(),
        ).unwrap_or_else(|| panic!("Type ({}) type-type missing AspectArtifact", type_.desc()));
    aspect.to_serial(xx, gx, loc, type_.as_value()).into_res().unwrap_or_else(|e| {
        panic!("Failed to serialize type ref for {}: {:?}", type_.desc(), e)
    })
}

pub(crate) fn unique_to_serial(u: &Unique) -> ArtifactDataUnique {
    match u {
        Unique::Builtin { file, line } => ArtifactDataUnique::Builtin {
            file: file.to_string(),
            line: *line,
        },
        Unique::Source(compile_id, counter) => ArtifactDataUnique::Source {
            import_state: compile_id.0.0,
            source: compile_id.1.0,
            counter: *counter,
        },
    }
}

pub(crate) fn serial_to_unique(s: &ArtifactDataUnique) -> Unique {
    match s {
        ArtifactDataUnique::Builtin { file, line } => {
            // For deserialized builtins, use Compile with a deterministic id since we can't
            // reconstruct &'static str
            Unique::Source(
                crate::langhelp::CompileId(crate::langhelp::ImportStateId(usize::MAX), crate::langhelp::SourceId(0)),
                *line,
            )
        },
        ArtifactDataUnique::Source { import_state, source, counter } => Unique::Source(
            crate::langhelp::CompileId(
                crate::langhelp::ImportStateId(*import_state),
                crate::langhelp::SourceId(*source),
            ),
            *counter,
        ),
    }
}
