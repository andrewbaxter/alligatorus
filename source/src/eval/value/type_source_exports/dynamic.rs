use {
    crate::{
        builtin::{
            AspectBind,
            AspectConsumeDynamic,
            AspectField,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
        },
        code_dynamic::CodeDynamic,
        eval::{
            Erb,
            definition_set::DefinitionSetExports,
            from_v8_ref,
            value::{
                type_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                type_source_data_type_constructor::TypeSourceDataTypeConstructorConst,
                type_source_function::TypeSourceFunctionConst,
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

pub(crate) struct TypeSourceExportsDynamic;

impl Value for TypeSourceExportsDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for TypeSourceExportsDynamic {
    fn desc(&self) -> String {
        format!("definition_set_exports")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceExportsDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceExportsDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceExportsDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceExportsDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceExportsDynamic) as Box<dyn AspectLiftDynamic>));
        }
        return None;
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_exports"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_exports"))
    }
}

impl Aspect for TypeSourceExportsDynamic { }

impl AspectConsumeDynamic for TypeSourceExportsDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectBind for TypeSourceExportsDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        crate::eval::value::type_dynamic::mortar_bind_to_scope(xx, loc, cv)
    }
}

impl AspectStackValueDynamic for TypeSourceExportsDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceExportsDynamic),
            }),
        }
    }
}

impl AspectLiftDynamic for TypeSourceExportsDynamic {
    fn lift(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let exports: Rc<DefinitionSetExports> = from_v8_ref::<Rc<DefinitionSetExports>>(&value).clone();
        exports.set.finish()?;
        Ok(Rc::new(ValueDynamic {
            source: ValueDynamicSource::Expr,
            type_: Rc::new(DefinitionSetExportsType { set: exports.set.clone() }),
        }))
    }
}

// --- Post-lift type with field access ---
struct DefinitionSetExportsType {
    set: dumpster::unsync::Gc<crate::eval::definition_set::DefinitionSet>,
}

impl Value for DefinitionSetExportsType {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

impl Type for DefinitionSetExportsType {
    fn desc(&self) -> String {
        format!("definition_set_exports")
    }

    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(DefinitionSetExportsType { set: self.set.clone() })
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

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert definition_set_exports"))
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to definition_set_exports"))
    }
}

impl Aspect for DefinitionSetExportsType { }

impl AspectBind for DefinitionSetExportsType {
    fn bind(&self, _xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        EResult::ok((
            Code {
                loc: loc,
                code: Code_::None,
            },
            Box::new(crate::eval::value::type_tuple::ScopeEntryValue { value: Rc::from(cv.value) }) as
                Box<dyn ScopeEntry>,
        ))
    }
}

impl AspectField for DefinitionSetExportsType {
    fn field(&self, xx: &ExprContext, loc: QExprId, _self_: CodeValue, key: CodeValue) -> EvalRes {
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());

            // Look up named functions
            {
                let functions = self.set.functions.borrow();
                for func_data in functions.iter() {
                    if func_data.name.as_deref() == Some(&key_name) {
                        return Ok(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::None
                            },
                            value: Rc::new(ValueDynamic {
                                source: ValueDynamicSource::DefinitionSet,
                                type_: Rc::new(TypeSourceFunctionConst { data: func_data.clone(), }),
                            }),
                        });
                    }
                }
            }

            // Look up named data type constructors
            {
                let data_types = self.set.data_types.borrow();
                for dt_data in data_types.iter() {
                    if dt_data.name.as_deref() == Some(&key_name) {
                        return Ok(CodeValue {
                            code: Code {
                                loc,
                                code: Code_::None
                            },
                            value: Rc::new(ValueDynamic {
                                source: ValueDynamicSource::Expr,
                                type_: Rc::new(TypeSourceDataTypeConstructorConst { data: dt_data.clone(), }),
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
            for func_data in functions.iter() {
                if func_data.name.as_deref() == Some(key) {
                    return Some(Rc::new(TypeSourceFunctionConst { data: func_data.clone() }));
                }
            }
        }
        {
            let data_types = self.set.data_types.borrow();
            for dt_data in data_types.iter() {
                if dt_data.name.as_deref() == Some(key) {
                    return Some(Rc::new(TypeSourceDataTypeConstructorConst { data: dt_data.clone() }));
                }
            }
        }
        None
    }
}
