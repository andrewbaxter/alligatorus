use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectFunction,
            AspectIterable,
            AspectListDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_consume_dynamic_get,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_iterable_aspect_id,
            aspect_list_dynamic_aspect_id,
            aspect_list_dynamic_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
            aspect_stack_value_dynamic_get,
            aspect_string_const_get,
            aspect_string_dynamic_get_maybe,
            aspect_tuple_get_maybe,
        },
        code_dynamic::{
            CodeDynamic,
            CodeDynamicFieldUse,
            CodeDynamicForeach,
            CodeDynamicNewArray,
            CodeDynamicType,
        },
        eval::{
            Erb,
            typecheck_convert,
            value::{
                dynamic_util::{
                    ValueDynamic,
                    ValueDynamicDataAccess,
                    ValueDynamicSource,
                },
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_string::dynamic::TypeStringDynamic,
                value_empty::r#const::ValueEmptyConst,
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
            ScopeEntry,
            ScopeEntryId,
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
};

// --- list type (result of list(string), and also type of list instances) ---
#[derive(Clone)]
pub(crate) struct TypeListDynamic {
    pub(crate) element_type: Rc<dyn Type>,
}

impl Aspect for TypeListDynamic { }

impl AspectBind for TypeListDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        crate::eval::value::dynamic_util::bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeListDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        crate::eval::value::dynamic_util::bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeListDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectField for TypeListDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_const_get(xx, loc, key_type.as_ref()).into_res() {
            Ok(v) => v,
            Err(e) => return EvalRes {
                v: None,
                errors: Some(e),
            },
        };
        let key_name = key_string.get(key.value.as_ref());
        if key_name == "new" {
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::None,
                },
                value: Rc::new(
                    ValueEmptyConst {
                        type_: Rc::new(TypeListNewDynamic { element_type: self.element_type.clone() }),
                    },
                ),
            });
        }

        // Instance-level fields (push, join, etc.)
        return er_seq_cv!(loc, erb, {
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on list", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                            base: Box::new(self_code),
                            field: key_name.clone(),
                        }),
                    })),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Field(ValueDynamicDataAccess { key: key_name, }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "new" => Some(Rc::new(TypeListNewDynamic { element_type: self.element_type.clone() })),
            "push" => Some(Rc::new(TypeFunctionDynamic {
                arg: self.element_type.clone(),
                ret: Rc::new(TypeNullDynamic),
            })),
            "join" => {
                if aspect_string_dynamic_get_maybe(xx, self.element_type.as_ref()).is_some() {
                    Some(Rc::new(TypeFunctionDynamic {
                        arg: Rc::new(TypeStringDynamic),
                        ret: Rc::new(TypeStringDynamic),
                    }))
                } else {
                    None
                }
            },
            _ => None,
        }
    }
}

impl AspectIterable for TypeListDynamic {
    fn element_type(&self) -> Rc<dyn Type> {
        self.element_type.clone()
    }

    fn foreach(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        base: CodeValue,
        element_key: ScopeEntryId,
        body: CodeValue,
    ) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let base_consume = erb.must_er(aspect_consume_dynamic_get(xx, loc, &*Rc::new(self.clone())))?;
            let base_js = erb.must_er(base_consume.to_dynamic(xx, loc, base))?;
            let body_js = match body.code.cast::<CodeDynamic>().into_res() {
                Ok(Some(js)) => js.clone(),
                Ok(None) => CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Literal(serde_json::Value::Null)
                },
                Err(e) => {
                    erb.errors.extend(e);
                    return Err(());
                },
            };
            let foreach_js = CodeDynamic {
                loc,
                type_: CodeDynamicType::Foreach(CodeDynamicForeach {
                    base: Box::new(base_js),
                    element_key,
                    body: Box::new(body_js),
                }),
            };
            let null_type = Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic);
            let stack_value = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, null_type.as_ref()))?;
            return Ok(stack_value.to_value(loc, foreach_js));
        });
    }
}

impl AspectListDynamic for TypeListDynamic {
    fn element_type(&self) -> Rc<dyn Type> {
        self.element_type.clone()
    }
}

impl AspectStackValueDynamic for TypeListDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(self.clone()),
            }),
        }
    }
}

impl crate::eval::dynamic_wrap::DynamicWrap for TypeListDynamic {
    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let rc: Rc<TypeListDynamic> = crate::eval::from_v8_ref::<Rc<TypeListDynamic>>(&*value).clone();
        (*rc).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        crate::eval::to_v8(scope, Rc::new(self)).into()
    }

    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeListDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeListDynamic { element_type: self.element_type.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_iterable_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectIterable>));
        }
        if *key == aspect_list_dynamic_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectListDynamic>));
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        aspect_list_dynamic_get_maybe(xx, source_type).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(_xx, loc);
        if aspect_list_dynamic_get_maybe(_xx, source_type.as_ref()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(crate::eval::value::dynamic_util::ValueDynamic {
                    source: crate::eval::value::dynamic_util::ValueDynamicSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        }
        EvalRes::err(loc, "Cannot convert to list type".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert list type".to_string())
    }

    fn desc(&self) -> String {
        format!("list({})", self.element_type.desc())
    }
}

impl Value for TypeListDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}

// --- list new function ---
#[derive(Clone)]
struct TypeListNewDynamic {
    element_type: Rc<dyn Type>,
}

impl Aspect for TypeListNewDynamic { }

impl AspectFunction for TypeListNewDynamic {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let arg_type = argument.value.type_(xx, loc);
            let mut js_elements = vec![];
            if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
                let elements = arg_tuple.take_elements(argument.value);
                for elem_res in elements {
                    let elem_cv = erb.must_er(elem_res)?;
                    let converted = erb.must_er(typecheck_convert(xx, loc, self.element_type.as_ref(), elem_cv))?;
                    let elem_to_dynamic =
                        erb.must_er(aspect_consume_dynamic_get(xx, loc, self.element_type.as_ref()))?;
                    let js = erb.must_er(elem_to_dynamic.to_dynamic(xx, loc, converted))?;
                    js_elements.push(js);
                }
            } else {
                let converted = erb.must_er(typecheck_convert(xx, loc, self.element_type.as_ref(), argument))?;
                let elem_to_dynamic = erb.must_er(aspect_consume_dynamic_get(xx, loc, self.element_type.as_ref()))?;
                let js = erb.must_er(elem_to_dynamic.to_dynamic(xx, loc, converted))?;
                js_elements.push(js);
            }
            let instance_type = Rc::new(TypeListDynamic { element_type: self.element_type.clone() });
            let ret_stack = erb.must_er(aspect_stack_value_dynamic_get(xx, loc, instance_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, CodeDynamic {
                loc,
                type_: CodeDynamicType::NewArray(CodeDynamicNewArray { elements: js_elements }),
            }));
        });
    }
}

impl Type for TypeListNewDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeListNewDynamic { element_type: self.element_type.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to list new".to_string())
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert list new".to_string())
    }

    fn desc(&self) -> String {
        format!("list({}).new", self.element_type.desc())
    }
}

impl Value for TypeListNewDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}
