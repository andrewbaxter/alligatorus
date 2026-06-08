use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::{
            Erb,
            value::{
                dynamic_util::{
                    ValueDynamic,
                    ValueDynamicSource,
                    bind_dynamic_default,
                    bind_via_target,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
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
        mem,
        rc::Rc,
    },
    super::TypeSourceInterfaceConst,
};

impl Aspect for TypeSourceInterfaceConst { }

impl AspectBind for TypeSourceInterfaceConst {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceInterfaceConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
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
                            field: field_name,
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

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        let methods = self.data.methods.borrow();
        for method in methods.iter() {
            if method.name == key {
                return Some(Rc::new(TypeFunctionDynamic {
                    arg: Rc::new(TypeNullDynamic),
                    ret: Rc::new(TypeNullDynamic),
                }));
            }
        }
        None
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

impl Type for TypeSourceInterfaceConst {
    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("interface"),
        }
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceInterfaceConst { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        // Check interface identity aspect
        if *key == self.data.identity {
            return Some(Box::new(true));
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
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        let identity = self.data.identity.clone();
        source_type.aspect(xx, &identity).is_some()
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
}

impl Value for TypeSourceInterfaceConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeSourceInterfaceConst { data: self.data.clone() })
    }
}
