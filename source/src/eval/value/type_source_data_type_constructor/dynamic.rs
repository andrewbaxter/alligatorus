use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            definition_set::DefinitionSet,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            from_v8_ref,
            artifact::{
                Artifact,
                ArtifactId,
                deserialize_type_artifact,
            },
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_data_type::{
                    SourceDataMethodData,
                    SourceDataTypeData,
                },
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
            },
        },
        invariant::DeserialLazy,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            Counter,
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
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        cell::Cell,
        rc::Rc,
    },
    super::{
        TypeSourceDataTypeConstructorConst,
        artifact::ArtifactDataDefSetCtorData,
    },
};

pub(crate) fn deserialize_defset_ctor_as_type(
    xx: &ExprContext,
    loc: QExprId,
    data: &serde_json::Value,
) -> Result<(Rc<dyn Type>, Vec<Code>), String> {
    let artifact_id: ArtifactId =
        serde_json::from_value(
            data.clone(),
        ).map_err(|e| format!("Failed to parse defset ctor artifact ref: {}", e))?;
    let stored = xx.vx.ix.gx.artifacts.get(artifact_id)?;
    let serial: ArtifactDataDefSetCtorData =
        serde_json::from_value(stored.data).map_err(|e| format!("Failed to parse defset ctor data: {}", e))?;
    let mut codes: Vec<Code> = Vec::new();
    let (underlying_type, underlying_code) =
        deserialize_type_artifact(xx, loc, &serial.underlying_type).into_res().map_err(|e| format!("{:?}", e))?;
    codes.push(underlying_code);
    let methods: Vec<Rc<SourceDataMethodData>> = serial.methods.iter().enumerate().map(|(i, m)| {
        let (ret_type, ret_code) =
            deserialize_type_artifact(xx, loc, &m.ret_type).into_res().expect("Failed to resolve method ret type");
        codes.push(ret_code);
        Rc::new(SourceDataMethodData {
            artifact_id: std::cell::RefCell::new(None),
            name: m.name.clone(),
            arg_type: std::cell::RefCell::new(None),
            impl_expr: std::cell::RefCell::new(None),
            ret_type: std::cell::RefCell::new(Some(ret_type)),
            scope_entry_id: Cell::new(Some(ScopeEntryId::Def(serial.def_set_id, i))),
            inner_scope_entry_id: Cell::new(None),
            field_name: m.field_name.clone(),
            compiled_js: std::cell::RefCell::new(Some(m.compiled_js.clone())),
            captured_externals: std::cell::RefCell::new(Vec::new()),
        })
    }).collect();
    let set = dumpster::unsync::Gc::new(DefinitionSet {
        artifact_id: std::cell::RefCell::new(None),
        def_set_id: serial.def_set_id,
        functions: std::cell::RefCell::new(Vec::new()),
        data_types: std::cell::RefCell::new(Vec::new()),
        interfaces: std::cell::RefCell::new(Vec::new()),
        finished: Cell::new(true),
        next_field_id: Counter::default(),
        next_entry_id: Counter::default(),
        next_interface_method_id: Counter::default(),
        dependencies: std::cell::RefCell::new(std::collections::HashMap::new()),
    });
    let dt_data = dumpster::unsync::Gc::new(SourceDataTypeData {
        set: set.clone(),
        name: serial.name,
        description: std::cell::RefCell::new(None),
        underlying_type: {
            let dl = DeserialLazy::new();
            dl.set(underlying_type);
            dl
        },
        methods: std::cell::RefCell::new(methods),
        interface_impls: std::cell::RefCell::new(Vec::new()),
        identity: serial.identity.clone(),
        data_field: serial.data_field,
        artifact_id: std::cell::RefCell::new(None),
    });
    set.data_types.borrow_mut().push(dt_data.clone());
    Ok((Rc::new(TypeSourceDataTypeConstructorConst { data: dt_data }), codes))
}

pub(crate) fn serialize_type_ref(xx: &ExprContext, loc: QExprId, type_: &dyn Type) -> Artifact {
    type_.type_to_serial(xx, loc)
}
pub(crate) struct TypeSourceDataTypeConstructorDynamic;

impl Aspect for TypeSourceDataTypeConstructorDynamic { }

impl AspectBind for TypeSourceDataTypeConstructorDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeConstructorDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceDataTypeConstructorDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceDataTypeConstructorDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceDataTypeConstructorDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeSourceDataTypeConstructorDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let ctor_ref: Rc<TypeSourceDataTypeConstructorConst> =
            from_v8_ref::<Rc<TypeSourceDataTypeConstructorConst>>(&value).clone();
        let ctor_type: Rc<dyn Type> = Rc::new(TypeSourceDataTypeConstructorConst { data: ctor_ref.data.clone() });
        Ok(Rc::new(ValueAnyConst { inner: ctor_type }))
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

impl Type for TypeSourceDataTypeConstructorDynamic {
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
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeConstructorDynamic) as Box<dyn AspectInitialBind>));
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to data_type_constructor"))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }
        let source_code = match cv.code.cast::<CodeDynamic>() {
            EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(
                loc,
                format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc()),
            ),
        };
        let call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(
                        CodeDynamicBindingUse { id: DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID },
                    ),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc =
            type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceDataTypeConstructorDynamic) as Rc<dyn Type>);
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(call)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            }),
        })
    }

    fn desc(&self) -> String {
        format!("definition_set_data_type_constructor")
    }
}

impl Value for TypeSourceDataTypeConstructorDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
