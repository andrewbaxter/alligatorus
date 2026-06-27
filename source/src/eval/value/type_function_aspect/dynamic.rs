use {
    crate::{
        artifact::ExporterAny,
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicType,
        },
        eval::value::{
            type_any::r#const::ValueAnyConst,
            type_binding::dynamic::BindingDynamicBinding,
            type_option::dynamic::TypeOptionDynamic,
            type_pull::aspect::TypePullAspect,
            type_root::r#const::TypeRoot,
            type_source_interface::SourceInterfaceRef,
            value_dynamic::{
                ValueDynamic,
                ValueDynamicSource,
            },
        },
        langhelp::{
            Aspect,
            BindingId,
            Code,
            CodeValue,
            Code_,
            EResult,
            EvalRes,
            ExprContext,
            External,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            ASPECT_LOOKUP_FN_SCOPE_ID,
            AspectFunction,
            AspectInitialBind,
            Binding,
            aspect_aspect_get_maybe,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_tuple_get_maybe,
            aspect_type_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

#[derive(Clone)]
pub(crate) struct TypeFunctionAspectDynamic;

impl Aspect for TypeFunctionAspectDynamic { }

impl AspectFunction for TypeFunctionAspectDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_type = match argument.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let arg_tuple = match aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            Some(t) => t,
            None => return EvalRes::err(
                loc,
                format!("aspect(): argument type ({}) is not a tuple — expected `(base, key)`", arg_type.desc(),),
            ),
        };
        let argument_value = argument.value.unwrap();
        let elements = arg_tuple.take_elements(argument_value);
        if elements.len() != 2 {
            return EvalRes::err(
                loc,
                format!("aspect(): expected 2-tuple `(base, key)`, got {} elements", elements.len()),
            );
        }
        let mut iter = elements.into_iter();
        let base = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_meta = match key.value.as_ref().unwrap().type_(xx, loc).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_type = match aspect_type_get_maybe(xx, key_meta.as_ref()) {
            Some(at) => at.type_(key.value.as_deref().unwrap()),
            None => key_meta.clone(),
        };
        let aspect_aspect = match aspect_aspect_get_maybe(xx, key_type.as_ref()) {
            Some(a) => a,
            None => return EvalRes::err(
                loc,
                format!("aspect(): key argument's type ({}) is not an aspect interface", key_type.desc(),),
            ),
        };
        let key_as_value = key_type.as_value();
        let unique = aspect_aspect.get_unique(key_as_value.as_ref());
        let iface_data = aspect_aspect.get_iface_data(key_as_value.as_ref());
        let iface_ref = Rc::new(SourceInterfaceRef { data: iface_data });
        let base_code = match base.code.cast::<CodeDynamic>().into_res() {
            Ok(Some(c)) => c.clone(),
            _ => return EvalRes::err(loc, "aspect(): base has no runtime code".to_string()),
        };
        let unique_id = BindingId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((unique_id, External::Unique(unique)));
        let unique_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: unique_id }),
        };
        let lookup_fn = CodeDynamic {
            loc,
            type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: ASPECT_LOOKUP_FN_SCOPE_ID }),
        };
        let call_code = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(lookup_fn),
                args: vec![base_code, unique_code],
            }),
        };
        let handle_type: Rc<dyn Type> = Rc::new(TypePullAspect {
            iface_ref,
            bundle: None,
        });
        let option_type: Rc<dyn Type> = Rc::new(TypeOptionDynamic { element_type: handle_type });
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(call_code)),
            },
            value: Some(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: option_type,
            })),
        })
    }

    fn exception_type(&self) -> std::rc::Rc<dyn crate::langhelp::Type> {
        std::rc::Rc::new(crate::eval::value::type_never::r#const::TypeNever)
    }
}

impl AspectInitialBind for TypeFunctionAspectDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeFunctionAspectDynamic),
        })
    }
}

impl ExporterAny for TypeFunctionAspectDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeFunctionAspectDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeFunctionAspectDynamic));
    }
}

impl Type for TypeFunctionAspectDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeFunctionAspectDynamic) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeFunctionAspectDynamic) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeFunctionAspectDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        None
    }

    fn desc(&self) -> String {
        "fn-aspect".to_string()
    }
}

impl Value for TypeFunctionAspectDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeFunctionAspectDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::FunctionAspectDynamic))
    }
}
