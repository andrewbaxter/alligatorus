use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
        },
        dynamic_code::DynamicCode,
        eval::{
            from_v8_ref,
            value::value_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
                bind_js_some,
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
        DefinitionSetInterface,
        DefinitionSetInterfaceRef,
    },
};

pub(crate) struct TypeSourceInterfaceRefDynamic;

fn interface_ref_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for TypeSourceInterfaceRefDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::value_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceInterfaceRefDynamic {
    fn desc(&self) -> String {
        format!("definition_set_interface_ref")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceInterfaceRefDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == interface_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceRefDynamic) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceRefDynamic) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceRefDynamic) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceInterfaceRefDynamic) as Box<dyn AspectLiftJs>));
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
                    type_: Rc::new(TypeSourceInterfaceRefDynamic),
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert to interface_ref"))
    }
}

impl Aspect for TypeSourceInterfaceRefDynamic { }

impl AspectConsumeJs for TypeSourceInterfaceRefDynamic {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<DynamicCode> {
        cv.code.cast::<DynamicCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for TypeSourceInterfaceRefDynamic {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<
        (Option<DynamicCode>, Box<dyn crate::eval::value::value_scope_entry::dynamic::DynamicScopeEntry>),
    > {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectJsStackValue for TypeSourceInterfaceRefDynamic {
    fn to_value(&self, loc: QExprId, value: DynamicCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceInterfaceRefDynamic),
            }),
        }
    }
}

impl AspectLiftJs for TypeSourceInterfaceRefDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let iface_ref: Rc<DefinitionSetInterfaceRef> =
            from_v8_ref::<Rc<DefinitionSetInterfaceRef>>(&value).clone();
        iface_ref.data.set.finish()?;
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(DefinitionSetInterface { data: iface_ref.data.clone() }),
        }))
    }
}
