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
            from_dynamic_ref,
            value::{
                type_any::r#const::ValueAnyConst,
                type_binding::dynamic::BindingDynamicBinding,
                type_root::r#const::TypeRoot,
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
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLift,
            AspectStackValueDynamic,
            Binding,
            FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            type_type_identity,
        },
    },
    std::{
        any::Any,
        rc::Rc,
    },
    super::r#const::TypeSourceFunctionConst,
};

pub(crate) struct TypeSourceFunctionDynamic;

impl Aspect for TypeSourceFunctionDynamic { }

impl AspectBind for TypeSourceFunctionDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn Binding>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceFunctionDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceFunctionDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceFunctionDynamic {
    fn initial_bind(&self, dynamic_key: BindingId) -> Box<dyn Binding> {
        Box::new(BindingDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceFunctionDynamic),
        })
    }
}

impl AspectLift for TypeSourceFunctionDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let func_ref: Rc<TypeSourceFunctionConst> =
            from_dynamic_ref::<Rc<TypeSourceFunctionConst>>(&value).clone();
        let set = func_ref.data.set.borrow().as_ref().unwrap().clone();
        xx.vx.definition_sets.borrow_mut().entry(set.def_set_id).or_insert_with(|| set.clone());
        let func_type: Rc<dyn Type> = Rc::new(TypeSourceFunctionConst { data: func_ref.data.clone() });
        Ok(Rc::new(ValueAnyConst { inner: func_type }))
    }
}

impl AspectStackValueDynamic for TypeSourceFunctionDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceFunctionDynamic),
            }),
        }
    }
}

impl ExporterAny for TypeSourceFunctionDynamic {
    fn as_type(&self) -> EResult<Rc<dyn Type>> {
        return EResult::ok(Rc::new(TypeSourceFunctionDynamic));
    }

    fn as_value(&self) -> EResult<Rc<dyn Value>> {
        return EResult::ok(Rc::new(TypeSourceFunctionDynamic));
    }
}

impl Type for TypeSourceFunctionDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceFunctionDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectLift>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceFunctionDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == crate::r#gen::aspect_post_deserialize_aspect_id() {
            return Some(
                Box::new(
                    Box::new(crate::eval::aspect::PostDeserializeIdentity) as
                        Box<dyn crate::r#gen::AspectPostDeserialize>,
                ),
            );
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_function"))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }
        let source_code = match cv.code.cast::<CodeDynamic>() {
            EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(
                loc,
                format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc()),
            ),
        };
        let call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(CodeDynamicCall {
                func: Box::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(
                        CodeDynamicBindingUse { id: FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID },
                    ),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceFunctionDynamic) as Rc<dyn Type>);
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(call)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: dest_rc,
            }),
        })
    }

    fn desc(&self) -> String {
        format!("definition_set_function")
    }
}

impl Value for TypeSourceFunctionDynamic {
    fn as_exporter_any(&self) -> Rc<dyn ExporterAny> {
        return Rc::new(TypeSourceFunctionDynamic);
    }

    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> EResult<Rc<dyn Type>> {
        EResult::ok(Rc::new(TypeRoot::SourceFunctionDynamic))
    }
}
