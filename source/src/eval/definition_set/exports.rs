use {
    super::{
        DefinitionSetDataTypeConstructor,
        DefinitionSetExports,
        DefinitionSetFunction,
    },
    crate::{
        builtin::{
            AspectBind,
            AspectBindJs,
            AspectConsumeJs,
            AspectField,
            AspectJsStackValue,
            AspectLiftJs,
            aspect_bind_aspect_id,
            aspect_bind_js_aspect_id,
            aspect_consume_js_aspect_id,
            aspect_field_aspect_id,
            aspect_js_stack_value_aspect_id,
            aspect_lift_js_aspect_id,
            aspect_string_get,
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
            ScopeEntry,
            Type,
            Unique,
            Value,
        },
    },
    crate::eval::{
        Erb,
        mortar_js_value::{
            MortarJsValue,
            MortarJsValueDataDefinitionSet,
            MortarJsValueSource,
            bind_js_some,
        },
        rc_from_v8,
    },
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
};

pub(crate) struct MortarTypeDefinitionSetExports;

impl Value for MortarTypeDefinitionSetExports {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for MortarTypeDefinitionSetExports {
    fn desc(&self) -> String {
        format!("definition_set_exports")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetExports) as Box<dyn AspectConsumeJs>));
        }
        if *key == aspect_bind_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetExports) as Box<dyn AspectBindJs>));
        }
        if *key == aspect_js_stack_value_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetExports) as Box<dyn AspectJsStackValue>));
        }
        if *key == aspect_lift_js_aspect_id() {
            return Some(Box::new(Box::new(MortarTypeDefinitionSetExports) as Box<dyn AspectLiftJs>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_exports"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_exports"))
    }
}

impl Aspect for MortarTypeDefinitionSetExports { }

impl AspectConsumeJs for MortarTypeDefinitionSetExports {
    fn to_js(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<JsCode> {
        cv.code.cast::<JsCode>().map(|x| x.unwrap().clone())
    }
}

impl AspectBindJs for MortarTypeDefinitionSetExports {
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

impl AspectJsStackValue for MortarTypeDefinitionSetExports {
    fn to_value(&self, loc: QExprId, value: JsCode) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Box::new(MortarJsValue {
                source: MortarJsValueSource::Expr,
                type_: Rc::new(MortarTypeDefinitionSetExports),
            }),
        }
    }
}

impl AspectLiftJs for MortarTypeDefinitionSetExports {
    fn lift(&self, _scope: &v8::PinScope, value: v8::Local<v8::Value>) -> Result<Box<dyn Value>, String> {
        let exports: Rc<DefinitionSetExports> = rc_from_v8(&value);
        exports.set.finish()?;
        Ok(Box::new(MortarJsValue {
            source: MortarJsValueSource::Expr,
            type_: Rc::new(DefinitionSetExportsType { set: exports.set.clone() }),
        }))
    }
}

// --- Post-lift type with field access ---
struct DefinitionSetExportsType {
    set: dumpster::unsync::Gc<super::DefinitionSet>,
}

impl Value for DefinitionSetExportsType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::type_ref::TypeType)
    }
}

impl Type for DefinitionSetExportsType {
    fn desc(&self) -> String {
        format!("definition_set_exports")
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_field_aspect_id() {
            return Some(
                Box::new(Box::new(DefinitionSetExportsType { set: self.set.clone() }) as Box<dyn AspectField>),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(DefinitionSetExportsType { set: self.set.clone() }) as Box<dyn AspectBind>),
            );
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_exports"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: &CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_exports"))
    }
}

impl Aspect for DefinitionSetExportsType { }

impl AspectBind for DefinitionSetExportsType {
    fn bind(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<Box<dyn ScopeEntry>> {
        EResult::ok(
            Box::new(crate::eval::tuple::ValueScopeEntry { value: Rc::from(cv.value) }) as Box<dyn ScopeEntry>,
        )
    }
}

impl AspectField for DefinitionSetExportsType {
    fn field(&self, xx: &ExprContext, loc: QExprId, _self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());

            // Look up named functions
            {
                let functions = self.set.functions.borrow();
                for (i, func_data) in functions.iter().enumerate() {
                    if func_data.name.as_deref() == Some(&key_name) {
                        return Ok(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::None
                            },
                            value: Box::new(MortarJsValue {
                                source: MortarJsValueSource::DefinitionSet(MortarJsValueDataDefinitionSet {
                                    set: self.set.clone(),
                                    func_index: i,
                                }),
                                type_: Rc::new(DefinitionSetFunction {
                                    set: self.set.clone(),
                                    func_index: i,
                                }),
                            }),
                        });
                    }
                }
            }

            // Look up named data type constructors
            {
                let data_types = self.set.data_types.borrow();
                for (i, dt_data) in data_types.iter().enumerate() {
                    if dt_data.name.as_deref() == Some(&key_name) {
                        return Ok(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::None
                            },
                            value: Box::new(MortarJsValue {
                                source: MortarJsValueSource::Expr,
                                type_: Rc::new(DefinitionSetDataTypeConstructor {
                                    set: self.set.clone(),
                                    data_type_index: i,
                                }),
                            }),
                        });
                    }
                }
            }
            return erb.err_new(format!("'{}' not found in definition set", key_name));
        });
    }

    fn field_opt(&self, _xx: &ExprContext, key: &str) -> Option<Rc<dyn Type>> {
        {
            let functions = self.set.functions.borrow();
            for (i, func_data) in functions.iter().enumerate() {
                if func_data.name.as_deref() == Some(key) {
                    return Some(Rc::new(DefinitionSetFunction {
                        set: self.set.clone(),
                        func_index: i,
                    }));
                }
            }
        }
        {
            let data_types = self.set.data_types.borrow();
            for (i, dt_data) in data_types.iter().enumerate() {
                if dt_data.name.as_deref() == Some(key) {
                    return Some(Rc::new(DefinitionSetDataTypeConstructor {
                        set: self.set.clone(),
                        data_type_index: i,
                    }));
                }
            }
        }
        None
    }
}
