use {
    crate::{
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
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
            },
        },
        langhelp::{
            Artifact,
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
};

pub(crate) struct TypeSourceInterfaceConst {
    pub data: dumpster::unsync::Gc<super::SourceInterfaceData>,
}

pub(crate) struct TypeSourceInterfaceConstAspectTypeImpl;

impl Aspect for TypeSourceInterfaceConstAspectTypeImpl { }

impl AspectType for TypeSourceInterfaceConstAspectTypeImpl {
    fn type_(&self, _value: &dyn Value) -> Rc<dyn Type> {
        Rc::new(super::dynamic::TypeSourceInterfaceDynamic)
    }
}

impl Aspect for TypeSourceInterfaceConst { }

impl AspectBind for TypeSourceInterfaceConst {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceInterfaceConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<TargetBindSub>> {
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

impl AspectArtifact for TypeSourceInterfaceConst {
    fn from_serial(&self, _xx: &ExprContext, loc: QExprId, _data: &serde_json::Value) -> EvalRes {
        EvalRes::err(loc, "TypeSourceInterfaceConst::from_serial: handled via Deserializer registry".to_string())
    }

    fn to_serial(
        &self,
        _xx: &ExprContext,
        loc: QExprId,
        _value: Rc<dyn Value>,
    ) -> EResult<Artifact> {
        match self.data.builtin_kind {
            Some(kind) => EResult::ok(super::artifact::TypeSourceInterfaceConstArtifact::build_artifact(kind)),
            None => EResult::err(
                loc,
                format!("TypeSourceInterfaceConst::to_serial: non-builtin interfaces not yet artifact-serializable"),
            ),
        }
    }
}

impl AspectAspect for TypeSourceInterfaceConst {
    fn get_iface_data(
        &self,
        _value: &dyn Value,
    ) -> dumpster::unsync::Gc<super::SourceInterfaceData> {
        self.data.clone()
    }

    fn get_unique(&self, _value: &dyn Value) -> Unique {
        self.data.identity.clone()
    }
}

impl Type for TypeSourceInterfaceConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(
            ValueAnyConst {
                inner: Rc::new(TypeSourceInterfaceConst { data: self.data.clone() }),
            },
        )
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
                Box::new(
                    Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectAspect>,
                ),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceInterfaceConst { data: self.data.clone() }) as Box<dyn AspectArtifact>,
                ),
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

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Artifact {
        let kind = self.data.builtin_kind.expect("type_to_serial: user-defined iface artifact not implemented");
        super::artifact::TypeSourceInterfaceConstArtifact::build_artifact(kind)
    }
}

impl Value for TypeSourceInterfaceConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
