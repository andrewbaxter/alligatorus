use crate::r#gen::ScopeEntry;
use {
    crate::{
        r#gen::{AspectBind, AspectBindDynamic, AspectConsumeDynamic, AspectField, AspectInitialBind, AspectStackValueDynamic, aspect_bind_aspect_id, aspect_bind_dynamic_aspect_id, aspect_consume_dynamic_aspect_id, aspect_field_aspect_id, aspect_initial_bind_aspect_id, aspect_stack_value_dynamic_aspect_id, aspect_string_const_get},
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::{
            Erb,
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
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
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::TypeSourceInterfaceConst,
};
use crate::r#gen::interface_const_identity;

impl Aspect for TypeSourceInterfaceConst { }

impl crate::r#gen::AspectArtifact for TypeSourceInterfaceConst {
    fn from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        // Builtin interfaces deserialize via `TypeSourceInterfaceConstArtifact`'s
        // `Deserializer` (registered in `artifact.rs`). `from_serial` here is
        // unreachable on the literal-deserialization path; non-builtin
        // interfaces aren't yet artifact-serializable.
        EvalRes::err(loc, "TypeSourceInterfaceConst::from_serial: handled via Deserializer registry".to_string())
    }

    fn to_serial(&self, _xx: &ExprContext, loc: QExprId, _value: Rc<dyn Value>) -> EResult<crate::langhelp::Artifact> {
        // Only builtin interfaces are serializable for now (round-tripped
        // via their `BuiltinInterfaceKind` and rebuilt by
        // `gen_build_builtin_interfaces` on deserialize).
        match self.data.builtin_kind {
            Some(kind) => EResult::ok(
                super::artifact::TypeSourceInterfaceConstArtifact::build_artifact(kind),
            ),
            None => EResult::err(
                loc,
                format!("TypeSourceInterfaceConst::to_serial: non-builtin interfaces not yet artifact-serializable"),
            ),
        }
    }
}

impl crate::r#gen::AspectAspect for TypeSourceInterfaceConst {
    fn get_unique(&self, _value: &dyn crate::langhelp::Value) -> crate::langhelp::Unique {
        self.data.identity.clone()
    }

    fn get_iface_data(
        &self,
        _value: &dyn crate::langhelp::Value,
    ) -> dumpster::unsync::Gc<super::SourceInterfaceData> {
        self.data.clone()
    }
}

impl AspectBind for TypeSourceInterfaceConst {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceInterfaceConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
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
            let key_type = key.value.type_(xx, loc);
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
                            field: crate::langhelp::ConstKey::String(field_name).to_str(),
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

/// Marker `Unique` used to identify *any* concrete `TypeSourceInterfaceConst`
/// instance — distinct from `interface_ref_type_identity()` (which marks the
/// type-erased dynamic ref) and from each interface's own `data.identity`.

impl Type for TypeSourceInterfaceConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst {
            inner: Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }),
        })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        // Check interface identity aspect
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == interface_const_identity() {
            return Some(Box::new(true));
        }
        if *key == crate::r#gen::aspect_type_aspect_id() {
            // Iface-ref-as-type: a value of this iface used as a type arg
            // (e.g. `map(string, type)`) resolves to the iface's dynamic
            // representation (`TypeSourceInterfaceDynamic`) so map/list/etc.
            // see a single runtime type for iface-ref-typed entries.
            return Some(Box::new(
                Box::new(TypeSourceInterfaceConstAspectTypeImpl)
                    as Box<dyn crate::r#gen::AspectType>,
            ));
        }
        if *key == crate::r#gen::aspect_aspect_aspect_id() {
            return Some(Box::new(
                Box::new(TypeSourceInterfaceConst { data: self.data.clone() })
                    as Box<dyn crate::r#gen::AspectAspect>,
            ));
        }
        if *key == crate::r#gen::aspect_artifact_aspect_id() {
            return Some(Box::new(
                Box::new(TypeSourceInterfaceConst { data: self.data.clone() })
                    as Box<dyn crate::r#gen::AspectArtifact>,
            ));
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

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let identity = self.data.identity.clone();
        let source_type = cv.value.type_(xx, loc);
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

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some() {
            // The iface ref's v8 form is `External(Rc<SourceInterfaceRef>)`.
            // Re-type the cv as `TypeSourceInterfaceDynamic` (the dynamic
            // representation that carries iface refs) and dispatch through
            // its `cast_to(any)`, which emits `__interface_ref_to_type` to
            // wrap the iface as `Rc<dyn Type>` at v8.
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

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("interface"),
        }
    }

    fn type_to_serial(
        &self,
        _xx: &ExprContext,
        _loc: QExprId,
    ) -> crate::langhelp::Artifact {
        // For now we only round-trip builtin interfaces — `build_builtin_interfaces`
        // can rebuild them from `BuiltinInterfaceKind`. User-defined interfaces
        // would need a per-interface artifact stored alongside the def-set.
        let kind = self.data.builtin_kind
            .expect("type_to_serial: user-defined iface artifact not implemented");
        super::artifact::TypeSourceInterfaceConstArtifact::build_artifact(kind)
    }
}

impl Value for TypeSourceInterfaceConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

pub(crate) struct TypeSourceInterfaceConstAspectTypeImpl;

impl crate::langhelp::Aspect for TypeSourceInterfaceConstAspectTypeImpl { }

impl crate::r#gen::AspectType for TypeSourceInterfaceConstAspectTypeImpl {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(super::dynamic::TypeSourceInterfaceDynamic)
    }
}
