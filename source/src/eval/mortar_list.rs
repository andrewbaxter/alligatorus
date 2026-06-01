use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectFunction,
            AspectIterable,
            AspectJsStackValue,
            AspectMortarList,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_consume_js_get,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_iterable_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_js_string_get_maybe,
            aspect_mortar_list_aspect_id,
            aspect_mortar_list_get_maybe,
            aspect_string_get,
            aspect_tuple_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeFieldUse,
            JsCodeForeach,
            JsCodeNewArray,
            JsCodeType,
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
    super::{
        Erb,
        mortar_function::MortarTypeFunction,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataAccess,
            MortarJsValueSource,
            bind_js_some,
        },
        mortar_null::TypeMortarNull,
        mortar_string::TypeMortarString,
        tuple::StaticValue,
        typecheck_convert,
    },
};

// --- list constructor (the "list" function in scope) ---
#[derive(Clone)]
pub(crate) struct TypeListConstructor;

impl Value for TypeListConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeListConstructor)
    }
}

impl Type for TypeListConstructor {
    fn desc(&self) -> String {
        "list constructor".to_string()
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeListConstructor) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert list constructor".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to list constructor".to_string())
    }
}

impl Aspect for TypeListConstructor { }

impl AspectFunction for TypeListConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let element_type = argument.value.type_(xx, loc);
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Box::new(TypeMortarList { element_type }),
        })
    }
}

impl super::jswrap::JsWrap for TypeMortarList {
    fn mortar_type() -> Rc<dyn Type> {
        Rc::new(super::type_ref::TypeType)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        let rc: Rc<TypeMortarList> = super::rc_from_v8(&*value);
        (*rc).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        super::rc_to_v8(scope, Rc::new(self)).into()
    }
}

// --- list type (result of list(string), and also type of list instances) ---
#[derive(Clone)]
pub(crate) struct TypeMortarList {
    pub(crate) element_type: Rc<dyn Type>,
}

impl Value for TypeMortarList {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}

impl Type for TypeMortarList {
    fn desc(&self) -> String {
        format!("list({})", self.element_type.desc())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectField>));
        }
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_iterable_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectIterable>));
        }
        if *key == aspect_mortar_list_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectMortarList>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert list type".to_string())
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> bool {
        let source_type = cv.value.type_(xx, loc);
        aspect_mortar_list_get_maybe(xx, source_type.as_ref()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        let source_type = cv.value.type_(_xx, loc);
        if aspect_mortar_list_get_maybe(_xx, source_type.as_ref()).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Box::new(super::mortar_js_value::MortarJsValue {
                    source: super::mortar_js_value::MortarJsValueSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        }
        EvalRes::err(loc, "Cannot convert to list type".to_string())
    }
}

impl Aspect for TypeMortarList { }

impl AspectMortarList for TypeMortarList {
    fn element_type(&self) -> Rc<dyn Type> {
        self.element_type.clone()
    }
}

impl AspectField for TypeMortarList {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let key_type = key.value.type_(xx, loc);
        let key_string = match aspect_string_get(xx, loc, key_type.as_ref()).into_res() {
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
                value: Box::new(
                    StaticValue { type_: Rc::new(TypeListNew { element_type: self.element_type.clone() }) },
                ),
            });
        }

        // Instance-level fields (push, join, etc.)
        return er_seq_cv!(loc, erb, {
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on list", key_name)),
            };
            let self_code = erb.must_er(self_.code.cast::<JsCode>())?.unwrap().clone();
            return Ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(JsCode {
                        loc,
                        type_: JsCodeType::FieldUse(JsCodeFieldUse {
                            base: Box::new(self_code),
                            field: key_name.clone(),
                        }),
                    })),
                },
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Field(MortarJsValueDataAccess {
                        loc,
                        base: Box::new(MortarJsValue {
                            source: MortarJsValueSource::Expr,
                            type_: Rc::new(self.clone()),
                        }),
                        key: key_name,
                    }),
                    type_: field_type,
                }),
            });
        });
    }

    fn field_opt(&self, xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "new" => Some(Rc::new(TypeListNew { element_type: self.element_type.clone() })),
            "push" => Some(Rc::new(MortarTypeFunction {
                arg: self.element_type.clone(),
                ret: Rc::new(TypeMortarNull),
            })),
            "join" => {
                if aspect_js_string_get_maybe(xx, self.element_type.as_ref()).is_some() {
                    Some(Rc::new(MortarTypeFunction {
                        arg: Rc::new(TypeMortarString),
                        ret: Rc::new(TypeMortarString),
                    }))
                } else {
                    None
                }
            },
            _ => None,
        }
    }
}

impl AspectJsStackValue for TypeMortarList {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(self.clone()),
            }),
        }
    }
}

impl AspectConsumeJs for TypeMortarList {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for TypeMortarList {
    fn bind_js(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Option<JsCode>, Box<dyn super::mortar_scope_entry::MortarScopeEntry>)> {
        let vtype = cv.value.type_(xx, loc);
        self.to_js(xx, loc, cv).map(|c| bind_js_some(loc, c, vtype))
    }
}

impl AspectIterable for TypeMortarList {
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
            let base_consume = erb.must_er(aspect_consume_js_get(xx, loc, &*Rc::new(self.clone())))?;
            let base_js = erb.must_er(base_consume.to_js(xx, loc, base))?;
            let body_js = match body.code.cast::<JsCode>().into_res() {
                Ok(Some(js)) => js.clone(),
                Ok(None) => JsCode {
                    loc,
                    type_: JsCodeType::Literal(serde_json::Value::Null)
                },
                Err(e) => {
                    erb.errors.extend(e);
                    return Err(());
                },
            };
            let foreach_js = JsCode {
                loc,
                type_: JsCodeType::Foreach(JsCodeForeach {
                    base: Box::new(base_js),
                    element_key,
                    body: Box::new(body_js),
                }),
            };
            let null_type = Rc::new(super::mortar_null::TypeMortarNull);
            let stack_value = erb.must_er(aspect_js_stack_value_get(xx, loc, null_type.as_ref()))?;
            return Ok(stack_value.to_value(loc, foreach_js));
        });
    }
}

// --- list new function ---
#[derive(Clone)]
struct TypeListNew {
    element_type: Rc<dyn Type>,
}

impl Value for TypeListNew {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}

impl Type for TypeListNew {
    fn desc(&self) -> String {
        format!("list({}).new", self.element_type.desc())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert list new".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to list new".to_string())
    }
}

impl Aspect for TypeListNew { }

impl AspectFunction for TypeListNew {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let arg_type = argument.value.type_(xx, loc);
            let mut js_elements = vec![];
            if let Some(arg_tuple) = aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
                let elements = arg_tuple.take_elements(argument.value);
                for elem_res in elements {
                    let elem_cv = erb.must_er(elem_res)?;
                    let converted = erb.must_er(typecheck_convert(xx, loc, self.element_type.as_ref(), elem_cv))?;
                    let elem_to_js = erb.must_er(aspect_consume_js_get(xx, loc, self.element_type.as_ref()))?;
                    let js = erb.must_er(elem_to_js.to_js(xx, loc, converted))?;
                    js_elements.push(js);
                }
            } else {
                let converted = erb.must_er(typecheck_convert(xx, loc, self.element_type.as_ref(), argument))?;
                let elem_to_js = erb.must_er(aspect_consume_js_get(xx, loc, self.element_type.as_ref()))?;
                let js = erb.must_er(elem_to_js.to_js(xx, loc, converted))?;
                js_elements.push(js);
            }
            let instance_type = Rc::new(TypeMortarList { element_type: self.element_type.clone() });
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, instance_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::NewArray(JsCodeNewArray { elements: js_elements }),
            }));
        });
    }
}
