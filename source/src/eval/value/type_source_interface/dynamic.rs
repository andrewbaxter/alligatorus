use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            from_v8_ref,
            value::type_scope_entry::dynamic::ScopeEntryDynamicBinding,
        },
        langhelp::{
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
    std::{
        any::Any,
        rc::Rc,
    },
    super::{
        SourceInterfaceRef,
        TypeSourceInterfaceConst,
    },
};

fn interface_ref_type_identity() -> Unique {
    Unique::Builtin {
        file: file!().to_string(),
        line: line!() as usize,
    }
}
pub(crate) struct TypeSourceInterfaceDynamic;

impl Aspect for TypeSourceInterfaceDynamic { }

impl crate::builtin::AspectAspect for TypeSourceInterfaceDynamic {
    fn get_unique(&self, value: &dyn crate::langhelp::Value) -> crate::langhelp::Unique {
        let iface_ref =
            crate::invariant::must_downcast_ref::<Rc<SourceInterfaceRef>>(value as &dyn std::any::Any);
        iface_ref.data.identity.clone()
    }

    fn get_iface_data(
        &self,
        value: &dyn crate::langhelp::Value,
    ) -> dumpster::unsync::Gc<super::SourceInterfaceData> {
        let iface_ref =
            crate::invariant::must_downcast_ref::<Rc<SourceInterfaceRef>>(value as &dyn std::any::Any);
        iface_ref.data.clone()
    }
}

impl AspectBind for TypeSourceInterfaceDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceInterfaceDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
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
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let iface_ref: Rc<SourceInterfaceRef> = from_v8_ref::<Rc<SourceInterfaceRef>>(&value).clone();
        iface_ref.data.set.finish()?;
        Ok(Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() }))
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

impl Type for TypeSourceInterfaceDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceInterfaceDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == crate::builtin::aspect_aspect_aspect_id() {
            return Some(Box::new(
                Box::new(TypeSourceInterfaceDynamic) as Box<dyn crate::builtin::AspectAspect>,
            ));
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
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &interface_ref_type_identity()).is_some()
            || source_type
                .aspect(
                    xx,
                    &crate::eval::value::type_source_interface::r#const::interface_const_identity(),
                )
                .is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &interface_ref_type_identity()).is_some()
            || source_type
                .aspect(
                    xx,
                    &crate::eval::value::type_source_interface::r#const::interface_const_identity(),
                )
                .is_some()
        {
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

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        // A dynamic interface ref can flow into any specific interface type
        // (Const) — the runtime value carries the actual interface data and
        // matches identity at use-time. Also flow into any type-of-types
        // (TypeAnyDynamic etc.) via the `__interface_ref_to_type` converter.
        type_.aspect(xx, &interface_ref_type_identity()).is_some()
            || type_
                .aspect(
                    xx,
                    &crate::eval::value::type_source_interface::r#const::interface_const_identity(),
                )
                .is_some()
            || type_.aspect(xx, &crate::eval::value::type_any::r#const::type_type_identity()).is_some()
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_
            .aspect(xx, &interface_ref_type_identity())
            .is_some()
            || type_
                .aspect(
                    xx,
                    &crate::eval::value::type_source_interface::r#const::interface_const_identity(),
                )
                .is_some()
        {
            let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceInterfaceDynamic) as Rc<dyn Type>);
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: dest_rc,
                }),
            });
        }
        if type_.aspect(xx, &crate::eval::value::type_any::r#const::type_type_identity()).is_some() {
            let source_code = match cv.code.cast::<crate::code_dynamic::CodeDynamic>() {
                crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
                _ => return EvalRes::err(loc, format!("cast interface_ref → {}: source has no runtime code", type_.desc())),
            };
            let call = crate::code_dynamic::CodeDynamic {
                loc,
                type_: crate::code_dynamic::CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                    func: Box::new(crate::code_dynamic::CodeDynamic {
                        loc,
                        type_: crate::code_dynamic::CodeDynamicType::BindingUse(crate::code_dynamic::CodeDynamicBindingUse {
                            id: crate::eval::INTERFACE_REF_TO_TYPE_FN_SCOPE_ID,
                        }),
                    }),
                    args: vec![source_code],
                }),
            };
            let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceInterfaceDynamic) as Rc<dyn Type>);
            return EvalRes::ok(CodeValue {
                code: Code { loc, code: Code_::Dyn(Rc::new(call)) },
                value: Rc::new(ValueDynamic { source: ValueDynamicSource::Expr, type_: dest_rc }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert interface_ref"))
    }

    fn desc(&self) -> String {
        format!("definition_set_interface_ref")
    }
}

impl Value for TypeSourceInterfaceDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
