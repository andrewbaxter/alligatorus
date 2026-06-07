use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamicSub,
            AspectConsumeDynamic,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_sub_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            definition_set::DefinitionSet,
            from_v8_ref,
            value::{
                type_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                    bind_dynamic,
                    bind_dynamic_sub_default,
                },
                type_source_data::{
                    SourceDataMethodData,
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
            Type,
            Unique,
            Value,
        },
    },
    std::{
        any::Any,
        cell::Cell,
        rc::Rc,
    },
    super::TypeSourceDataTypeConstructorConst,
};

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
        if *key == aspect_bind_dynamic_sub_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeConstructorDynamic) as Box<dyn AspectBindDynamicSub>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeConstructorDynamic) as Box<dyn AspectBind>));
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

impl AspectBindDynamicSub for TypeSourceDataTypeConstructorDynamic {
    fn bind_dynamic_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(CodeDynamic, crate::eval::aspect::ScopeEntryDynamicSub)> {
        bind_dynamic_sub_default(xx, loc, cv)
    }
}

impl AspectBind for TypeSourceDataTypeConstructorDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_dynamic(xx, loc, cv)
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
        let ctor_ref: Rc<TypeSourceDataTypeConstructorConst> =
            from_v8_ref::<Rc<TypeSourceDataTypeConstructorConst>>(&value).clone();
        ctor_ref.data.set.finish()?;
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(TypeSourceDataTypeConstructorConst { data: ctor_ref.data.clone() }),
        }))
    }
}

pub(crate) fn deserialize_defset_ctor_as_type(
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
    Ok(Rc::new(TypeSourceDataTypeConstructorConst { data: dt_data }))
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
