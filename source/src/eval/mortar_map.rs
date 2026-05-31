use {
    crate::{
        builtin::{
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectFunction,
            AspectJsStackValue,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_function_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_js_stack_value_get,
            aspect_string_get,
            aspect_string_get_maybe,
            aspect_tuple_get_maybe,
        },
        jscode::{
            JsCode,
            JsCodeFieldUse,
            JsCodeNewObject,
            JsCodeType,
            JsCodeBindingUse,
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
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataAccess,
            MortarJsValueSource,
            bind_js_some,
        },
        tuple::StaticValue,
    },
};

// --- map constructor (the "map" function in scope) ---
#[derive(Clone)]
pub(crate) struct TypeMapConstructor;

impl Value for TypeMapConstructor {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeMapConstructor)
    }
}

impl Type for TypeMapConstructor {
    fn desc(&self) -> String {
        "map constructor".to_string()
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(TypeMapConstructor) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map constructor".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to map constructor".to_string())
    }
}

impl Aspect for TypeMapConstructor { }

impl AspectFunction for TypeMapConstructor {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, argument: CodeValue) -> EvalRes {
        let arg_type = argument.value.type_(xx, loc);
        let arg_tuple = match aspect_tuple_get_maybe(xx, arg_type.as_ref()) {
            Some(t) => t,
            None => return EvalRes::err(loc, "map() requires a tuple of (key_type, value_type)".to_string()),
        };
        let element_types = arg_tuple.element_types();
        if element_types.len() != 2 {
            return EvalRes::err(
                loc,
                format!("map() requires exactly 2 type arguments (key, value), got {}", element_types.len()),
            );
        }
        let key_type = element_types[0].clone();
        let value_type = element_types[1].clone();

        // Validate key type is constkey-able (has AspectString)
        if aspect_string_get_maybe(xx, key_type.as_ref()).is_none() {
            return EvalRes::err(
                loc,
                format!("map key type '{}' is not constkey-able (must be string-like)", key_type.desc()),
            );
        }
        EvalRes::ok(CodeValue {
            code: Code {
                loc,
                code: Code_::None,
            },
            value: Box::new(TypeMortarMap {
                key_type,
                value_type,
            }),
        })
    }
}

// --- map type (result of map(string, number), and also type of map instances) ---
#[derive(Clone)]
pub(crate) struct TypeMortarMap {
    pub(crate) key_type: Rc<dyn Type>,
    pub(crate) value_type: Rc<dyn Type>,
}

impl Value for TypeMortarMap {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}

impl Type for TypeMortarMap {
    fn desc(&self) -> String {
        format!("map({}, {})", self.key_type.desc(), self.value_type.desc())
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
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map type".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, cv: &CodeValue) -> EvalRes {
        let source_type = cv.value.type_(_xx, loc);
        let source_any: &dyn Any = source_type.as_ref();
        if source_any.downcast_ref::<TypeMortarMap>().is_some() ||
            source_any.downcast_ref::<super::tuple::RecordValue>().is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code.clone(),
                value: Box::new(MortarJsValue {
                    source: MortarJsValueSource::Expr,
                    type_: Rc::new(self.clone()),
                }),
            });
        }

        // ExprType sources (staged expressions): preserve the original value since
        // BTreeMap::from_v8 expects the staged Expr (e.g. a Record) to be intact.
        if source_any.downcast_ref::<super::expr::ExprType>().is_some() {
            let value_any: &dyn Any = cv.value.as_ref();
            if let Some(expr) = value_any.downcast_ref::<super::expr::Expr>() {
                return EvalRes::ok(CodeValue {
                    code: cv.code.clone(),
                    value: Box::new(expr.clone()),
                });
            }
        }
        EvalRes::err(loc, "Cannot convert to map type".to_string())
    }
}

impl Aspect for TypeMortarMap { }

impl AspectField for TypeMortarMap {
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
                value: Box::new(StaticValue { type_: Rc::new(TypeMapNew {
                    key_type: self.key_type.clone(),
                    value_type: self.value_type.clone(),
                }) }),
            });
        }

        // Instance-level fields
        return er_seq_cv!(loc, erb, {
            let field_type = match self.field_opt(xx, &key_name) {
                Some(t) => t,
                None => return erb.err_new(format!("Field '{}' not found on map", key_name)),
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

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        match key {
            "new" => Some(Rc::new(TypeMapNew {
                key_type: self.key_type.clone(),
                value_type: self.value_type.clone(),
            })),
            _ => None,
        }
    }
}

impl AspectJsStackValue for TypeMortarMap {
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

impl AspectConsumeJs for TypeMortarMap {
    fn to_js(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        // If there's already JS code, use it directly
        if let Some(js) = cv.code.cast::<JsCode>().v.flatten() {
            return EResult::ok(js.clone());
        }

        // Otherwise wrap the value as a staged external (e.g. staged record expressions)
        let cell = Rc::new(super::expr::StagedExprCell { value: std::cell::RefCell::new(Some(cv.value)) });
        let id = ScopeEntryId::Local(xx.vx.next_local_id.next());
        xx.vx.externals.borrow_mut().push((id, cell));
        EResult::ok(JsCode {
            loc,
            type_: JsCodeType::BindingUse(crate::jscode::JsCodeBindingUse { id }),
        })
    }
}

impl AspectBindJs for TypeMortarMap {
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

// --- map new function ---
#[derive(Clone)]
struct TypeMapNew {
    key_type: Rc<dyn Type>,
    value_type: Rc<dyn Type>,
}

impl Value for TypeMapNew {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(self.clone())
    }
}

impl Type for TypeMapNew {
    fn desc(&self) -> String {
        format!("map({}, {}).new", self.key_type.desc(), self.value_type.desc())
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_function_aspect_id() {
            return Some(Box::new(Box::new(self.clone()) as Box<dyn AspectFunction>));
        }
        None
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, "Cannot convert map new".to_string())
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to map new".to_string())
    }
}

impl Aspect for TypeMapNew { }

impl AspectFunction for TypeMapNew {
    fn call(&self, xx: &ExprContext, loc: QExprId, _function: CodeValue, _argument: CodeValue) -> EvalRes {
        let instance_type = Rc::new(TypeMortarMap {
            key_type: self.key_type.clone(),
            value_type: self.value_type.clone(),
        });
        return er_seq_cv!(loc, erb, {
            let ret_stack = erb.must_er(aspect_js_stack_value_get(xx, loc, instance_type.as_ref()))?;
            return Ok(ret_stack.to_value(loc, JsCode {
                loc,
                type_: JsCodeType::NewObject(JsCodeNewObject { fields: vec![] }),
            }));
        });
    }
}
