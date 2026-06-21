use {
    crate::artifact::ExporterAny,
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::value::{
            type_any::r#const::TypeAnyConst,
            type_function::dynamic::TypeFunctionDynamic,
            type_scope_entry::dynamic::ScopeEntryDynamicBinding,
            type_source_interface::SourceInterfaceRef,
            value_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
            },
        },
        langhelp::{
            EResult,
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EvalRes,
            ExprContext,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectField,
            AspectInitialBind,
            ScopeEntry,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_string_const_get,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypePullAspect {
    pub bundle: Option<v8::Global<v8::Object>>,
    pub iface_ref: Rc<SourceInterfaceRef>,
}

impl Aspect for TypePullAspect {
    fn v8_instance(&self) -> Option<v8::Global<v8::Object>> {
        self.bundle.clone()
    }
}

impl AspectField for TypePullAspect {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let self_code = match self_.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "TypePullAspect.field: receiver has no runtime code".to_string()),
        };
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(s) => s,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        let methods = self.iface_ref.data.methods.borrow();
        let method = match methods.iter().find(|m| m.name == key_name) {
            Some(m) => m,
            None => return EvalRes::err(
                loc,
                format!(
                    "aspect handle has no method '{}' on interface '{}'",
                    key_name,
                    self.iface_ref.data.description.borrow().as_deref().unwrap_or("?").to_string(),
                ),
            ),
        };
        let arg_type = match method.arg_type.borrow().clone() {
            Some(t) => t,
            None => return EvalRes::err(loc, "interface method missing arg type".to_string()),
        };
        let ret_type = match method.ret_type.borrow().clone() {
            Some(t) => t,
            None => return EvalRes::err(loc, "interface method missing ret type".to_string()),
        };
        let field_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                base: Box::new(self_code),
                field: ConstKey::String(method.field_name.clone()).to_str(),
            }),
        };
        let func_type = Rc::new(TypeFunctionDynamic {
            arg: arg_type,
            ret: ret_type,
        });
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(field_code)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: func_type,
            }),
        })
    }

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let ConstKey::String(s) = key else {
            return None;
        };
        let methods = self.iface_ref.data.methods.borrow();
        let method = methods.iter().find(|m| m.name == *s)?;
        let arg_type = method.arg_type.borrow().clone()?;
        let ret_type = method.ret_type.borrow().clone()?;
        Some(Rc::new(TypeFunctionDynamic {
            arg: arg_type,
            ret: ret_type,
        }))
    }
}

impl AspectInitialBind for TypePullAspect {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypePullAspect {
                iface_ref: self.iface_ref.clone(),
                bundle: self.bundle.clone(),
            }),
        })
    }
}

impl Type for TypePullAspect {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypePullAspect {
            iface_ref: self.iface_ref.clone(),
            bundle: self.bundle.clone(),
        })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(TypePullAspect {
                iface_ref: self.iface_ref.clone(),
                bundle: self.bundle.clone(),
            }) as Box<dyn AspectField>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypePullAspect {
                iface_ref: self.iface_ref.clone(),
                bundle: self.bundle.clone(),
            }) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_function_aspect_id() {
            return None;
        }
        None
    }

    fn desc(&self) -> String {
        format!("aspect<{}>", self.iface_ref.data.description.borrow().as_deref().unwrap_or("?"))
    }
}

impl ExporterAny for TypePullAspect {
    fn as_type(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }

    fn as_value(self) -> EResult<Rc<dyn Value>> {
        unimplemented!()
    }
}

impl Value for TypePullAspect {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        unimplemented!()
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeAnyConst))
    }
}
