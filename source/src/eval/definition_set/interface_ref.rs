use {
    super::{
        DefinitionSetInterface,
        DefinitionSetInterfaceRef,
    },
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
        jscode::JsCode,
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
    crate::eval::{
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueSource,
            bind_js_some,
        },
        from_v8_ref,
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct MortarTypeDefinitionSetInterfaceRef;

fn interface_ref_type_identity() -> Unique {
    Unique::Builtin {
        file: file!(),
        line: line!() as usize,
    }
}

impl Value for MortarTypeDefinitionSetInterfaceRef {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for MortarTypeDefinitionSetInterfaceRef {
    fn desc(&self) -> String {
        format!("definition_set_interface_ref")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == interface_ref_type_identity() {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetInterfaceRef) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetInterfaceRef) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetInterfaceRef) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetInterfaceRef) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert interface_ref"))
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> bool {
        let source_type = cv.value.type_(xx, loc);
        source_type.aspect(xx, &interface_ref_type_identity()).is_some()
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(xx, loc);
        if source_type.aspect(xx, &interface_ref_type_identity()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(MortarTypeDefinitionSetInterfaceRef),
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert to interface_ref"))
    }
}

impl Aspect for MortarTypeDefinitionSetInterfaceRef { }

impl AspectConsumeJs for MortarTypeDefinitionSetInterfaceRef {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeDefinitionSetInterfaceRef {
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

impl AspectJsStackValue for MortarTypeDefinitionSetInterfaceRef {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetInterfaceRef),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetInterfaceRef {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let iface_ref: Rc<DefinitionSetInterfaceRef> = from_v8_ref::<Rc<DefinitionSetInterfaceRef>>(&value).clone();
        iface_ref.set.finish()?;
        Ok(Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetInterface {
                set: iface_ref.set.clone(),
                interface_index: iface_ref.interface_index,
            }),
        }))
    }
}
