use {
    crate::{
        artifact::ExporterAny,
        eval::{
            dynamic::code::{
                CodeDynamic,
                CodeDynamicBindingUse,
                CodeDynamicCall,
                CodeDynamicType,
            },
            value::{
                type_any::r#const::ValueAnyConst,
                type_binding::dynamic::BindingDynamicBinding,
                type_root::r#const::TypeRoot,
                type_source_object::{
                    dynamic::TypeSourceObjectDynamic,
                    r#const::TypeSourceObjectConst,
                },
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
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
            AspectFunction,
            AspectInitialBind,
            Binding,
            CAST_VALUE_FN_SCOPE_ID,
            aspect_function_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_tuple_get_maybe,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
};

pub(crate) struct TypeFunctionCastValueDynamic;

impl Aspect for TypeFunctionCastValueDynamic { }

impl AspectFunction for TypeFunctionCastValueDynamic {
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
                format!("cast_value: argument type ({}) is not a tuple — expected `(value, type)`", arg_type.desc(),),
            ),
        };
        let arg_value = argument.value.unwrap();
        let elements = arg_tuple.take_elements(arg_value);
        if elements.len() != 2 {
            return EvalRes::err(
                loc,
                format!("cast_value: expected 2-tuple `(value, type)`, got {} elements", elements.len()),
            );
        }
        let mut iter = elements.into_iter();
        let value = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let type_ = match iter.next().unwrap().into_res() {
            Ok(cv) => cv,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let type_value_any: &dyn Any = type_.value.as_deref().unwrap();
        let data = match type_value_any.downcast_ref::<TypeSourceObjectConst>() {
            Some(t) => t.data.clone(),
            None => return EvalRes::err(
                loc,
                format!(
                    "cast_value: type argument's value is not `TypeSourceObjectConst` (got {})",
                    type_
                        .value
                        .as_ref()
                        .unwrap()
                        .type_(xx, loc)
                        .v
                        .map(|t| t.desc())
                        .unwrap_or_else(|| "?".to_string()),
                ),
            ),
        };
        return er_seq_cv!(loc, erb, {
            let value_code = erb.must_er(value.code.cast::<CodeDynamic>())?.unwrap().clone();
            let template: Rc<dyn Type> = Rc::new(TypeSourceObjectConst { data: data.clone() });
            let template_id = BindingId::Local(xx.vx.next_local_id.next());
            xx.vx.externals.borrow_mut().push((template_id, External::Type(template)));
            let type_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: template_id }),
            };
            let cast_fn = CodeDynamic {
                loc,
                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: CAST_VALUE_FN_SCOPE_ID, }),
            };
            let call_code = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(cast_fn),
                    args: vec![value_code, type_code],
                }),
            };
            let result_type: Rc<dyn Type> = Rc::new(TypeSourceObjectDynamic { data });
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(call_code))
                },
                value: Some(Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: result_type,
                })),
            });
        });
    }

    fn exception_type(&self) -> Option<std::rc::Rc<dyn crate::langhelp::Type>> {
        None
    }
}

impl AspectInitialBind for TypeFunctionCastValueDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeFunctionCastValueDynamic),
        })
    }
}

impl ExporterAny for TypeFunctionCastValueDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeFunctionCastValueDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeFunctionCastValueDynamic));
    }
}

impl Type for TypeFunctionCastValueDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(ValueAnyConst { inner: Rc::new(TypeFunctionCastValueDynamic) })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeFunctionCastValueDynamic) as Box<dyn AspectFunction>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeFunctionCastValueDynamic) as Box<dyn AspectInitialBind>));
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
        "fn-cast-value".to_string()
    }
}

impl Value for TypeFunctionCastValueDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeFunctionCastValueDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::FunctionCastValueDynamic))
    }
}
