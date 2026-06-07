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
            from_v8_ref,
            value::type_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic,
                bind_dynamic_sub_default,
            },
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

pub(crate) struct TypeSourceInterfaceDynamic;

fn interface_ref_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for TypeSourceInterfaceDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceInterfaceDynamic {
    fn desc(&self) -> String {
        format!("definition_set_interface_ref")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceInterfaceDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == interface_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_sub_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceDynamic) as Box<dyn AspectBindDynamicSub>));
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
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert interface_ref"))
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &interface_ref_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &interface_ref_type_identity()).is_some() {
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
}

impl Aspect for TypeSourceInterfaceDynamic { }

impl AspectConsumeDynamic for TypeSourceInterfaceDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindDynamicSub for TypeSourceInterfaceDynamic {
    fn bind_dynamic_sub(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(CodeDynamic, crate::eval::aspect::ScopeEntryDynamicSub)> {
        bind_dynamic_sub_default(xx, loc, cv)
    }
}

impl AspectBind for TypeSourceInterfaceDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_dynamic(xx, loc, cv)
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

impl AspectLiftDynamic for TypeSourceInterfaceDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let iface_ref: Rc<SourceInterfaceRef> = from_v8_ref::<Rc<SourceInterfaceRef>>(&value).clone();
        iface_ref.data.set.finish()?;
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() }),
        }))
    }
}
