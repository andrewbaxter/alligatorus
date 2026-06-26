use {
    crate::{
        artifact::ExporterAny,
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            from_v8_ref,
            value::{
                type_any::r#const::{
                    TypeAnyConst,
                    ValueAnyConst,
                },
                type_root::r#const::TypeRoot,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
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
            AspectAspect,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            INTERFACE_REF_TO_TYPE_FN_SCOPE_ID,
            ScopeEntry,
            aspect_aspect_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            interface_const_identity,
            interface_ref_type_identity,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::{
        SourceInterfaceRef,
        r#const::TypeSourceInterfaceConst,
    },
};

pub(crate) struct TypeSourceInterfaceDynamic;

impl Aspect for TypeSourceInterfaceDynamic { }

impl AspectAspect for TypeSourceInterfaceDynamic {
    fn get_iface_data(&self, value: &dyn Value) -> dumpster::unsync::Gc<super::SourceInterfaceData> {
        let iface_ref = must_downcast_ref::<Rc<SourceInterfaceRef>>(value as &dyn std::any::Any);
        iface_ref.data.clone()
    }

    fn get_unique(&self, value: &dyn Value) -> Unique {
        let iface_ref = must_downcast_ref::<Rc<SourceInterfaceRef>>(value as &dyn std::any::Any);
        iface_ref.data.identity.clone()
    }
}

impl AspectBind for TypeSourceInterfaceDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceInterfaceDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceInterfaceDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceInterfaceDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceInterfaceDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeSourceInterfaceDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let iface_ref: Rc<SourceInterfaceRef> = from_v8_ref::<Rc<SourceInterfaceRef>>(&value).clone();
        let iface_type: Rc<dyn Type> = Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() });
        Ok(Rc::new(ValueAnyConst { inner: iface_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceInterfaceDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceInterfaceDynamic),
            }),
        }
    }
}

impl ExporterAny for TypeSourceInterfaceDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeSourceInterfaceDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeSourceInterfaceDynamic));
    }
}

impl Type for TypeSourceInterfaceDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeSourceInterfaceDynamic) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_aspect_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectAspect>));
        }
        if *key == interface_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectLiftDynamic>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(Box::new(crate::eval::aspect::PostDeserializeIdentity) as Box<dyn crate::r#gen::AspectPostDeserialize>),
            );
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &interface_ref_type_identity()).is_some() ||
            source_type.aspect(xx, &interface_const_identity()).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &interface_ref_type_identity()).is_some() ||
            type_.aspect(xx, &interface_const_identity()).is_some() ||
            type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = match cv.value.type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        if source_type.aspect(xx, &interface_ref_type_identity()).is_some() ||
            source_type.aspect(xx, &interface_const_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceInterfaceDynamic),
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert to interface_ref"))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &interface_ref_type_identity()).is_some() ||
            type_.aspect(xx, &interface_const_identity()).is_some() {
            let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceInterfaceDynamic) as Rc<dyn Type>);
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: dest_rc,
                }),
            });
        }
        if type_.aspect(xx, &type_type_identity()).is_some() {
            let source_code = match cv.code.cast::<CodeDynamic>() {
                EResult { v: Some(Some(c)), .. } => c.clone(),
                _ => return EvalRes::err(
                    loc,
                    format!("cast interface_ref → {}: source has no runtime code", type_.desc()),
                ),
            };
            let call = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(
                            CodeDynamicBindingUse { id: INTERFACE_REF_TO_TYPE_FN_SCOPE_ID },
                        ),
                    }),
                    args: vec![source_code],
                }),
            };
            let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceInterfaceDynamic) as Rc<dyn Type>);
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(call)),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: dest_rc,
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert interface_ref"))
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceInterfaceDynamic))
    }

    fn desc(&self) -> String {
        format!("definition_set_interface_ref")
    }
}

impl Value for TypeSourceInterfaceDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeSourceInterfaceDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceInterfaceDynamic))
    }
}
