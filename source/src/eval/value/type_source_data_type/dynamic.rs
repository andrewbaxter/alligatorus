use {
    crate::{
        builtin::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
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
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            from_v8_ref,
            value::{
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_interface::TypeSourceInterfaceConst,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
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
    dumpster::unsync::Gc,
    std::{
        any::Any,
        mem,
        rc::Rc,
    },
    super::{
        SourceDataTypeData,
        TypeSourceDataTypeConst,
    },
};

pub(crate) struct TypeSourceDataTypeDynamic {
    pub data: Option<Gc<SourceDataTypeData>>,
}

impl Aspect for TypeSourceDataTypeDynamic { }

impl AspectBind for TypeSourceDataTypeDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceDataTypeDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectField for TypeSourceDataTypeDynamic {
    fn field(&self, xx: &ExprContext, loc: QExprId, self_: CodeValue, key: CodeValue) -> EvalRes {
        let data = self.data.as_ref().expect("TypeSourceDataTypeDynamic field access requires data");
        return er_seq_cv!(loc, erb, {
            let key_type = key.value.type_(xx, loc);
            let key_string = erb.must_er(aspect_string_const_get(xx, loc, key_type.as_ref()))?;
            let key_name = key_string.get(key.value.as_ref());
            type MethodLookup = (String, Rc<dyn Type>, Rc<dyn Type>);
            let lookup_result: Option<MethodLookup> = 'lookup: {
                {
                    let methods = data.methods.borrow();
                    if let Some(m) = methods.iter().find(|m| m.name == key_name) {
                        let ret_t = m.ret_type.borrow().clone().expect("Data type method missing return type");
                        break 'lookup Some((m.field_name.clone(), Rc::new(TypeNullDynamic), ret_t));
                    }
                }
                {
                    let interface_impls = data.interface_impls.borrow();
                    for impl_data in interface_impls.iter() {
                        let iface_methods = impl_data.interface.methods.borrow();
                        if let Some(method) = iface_methods.iter().find(|m| m.name == key_name) {
                            let arg_t = method.arg_type.borrow().clone().expect("Interface method missing arg type");
                            let ret_t =
                                method.ret_type.borrow().clone().expect("Interface method missing return type");
                            break 'lookup Some((method.field_name.clone(), arg_t, ret_t));
                        }
                    }
                }
                None
            };
            let (field_name, arg_type, ret_type) = match lookup_result {
                Some(r) => r,
                None => return erb.err_new(format!("Field '{}' not found on data type", key_name)),
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

    fn field_type_opt(&self, _xx: &ExprContext, key: &ConstKey) -> Option<Rc<dyn Type>> {
        let data = self.data.as_ref()?;
        let ConstKey::String(key) = key else {
            return None;
        };
        let methods = data.methods.borrow();
        for method in methods.iter() {
            if method.name == *key {
                return Some(Rc::new(TypeFunctionDynamic {
                    arg: Rc::new(TypeNullDynamic),
                    ret: Rc::new(TypeNullDynamic),
                }));
            }
        }
        drop(methods);
        let interface_impls = data.interface_impls.borrow();
        for impl_data in interface_impls.iter() {
            let iface_methods = impl_data.interface.methods.borrow();
            for method in iface_methods.iter() {
                if method.name == *key {
                    return Some(Rc::new(TypeFunctionDynamic {
                        arg: Rc::new(TypeNullDynamic),
                        ret: Rc::new(TypeNullDynamic),
                    }));
                }
            }
        }
        None
    }
}

impl AspectInitialBind for TypeSourceDataTypeDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceDataTypeDynamic { data: self.data.clone() }),
        })
    }
}

impl AspectLiftDynamic for TypeSourceDataTypeDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        if let Some(data) = &self.data {
            let underlying_type = data.underlying_type.get().clone();
            let underlying_lift =
                crate::builtin::aspect_lift_dynamic_get_maybe(xx, underlying_type.as_ref()).ok_or_else(|| {
                    format!("Underlying type ({}) missing AspectLiftDynamic", underlying_type.desc())
                })?;
            let obj =
                v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lift")?;
            let data_field = data.data_field.clone();
            let key = v8::String::new(scope, &data_field).unwrap();
            let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
            let underlying_value = underlying_lift.lift(xx, scope, underlying_v8)?;
            Ok(Rc::new(crate::eval::value::value_source_data_type::r#const::ValueSourceDataTypeConst {
                underlying: underlying_value,
                data: data.clone(),
            }))
        } else {
            let type_: &Rc<dyn Type> = from_v8_ref(&value);
            let dt = must_downcast_ref::<TypeSourceDataTypeConst>(type_.as_ref());
            dt.data.set.finish()?;
            Ok(Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceDataTypeConst { data: dt.data.clone() }),
            }))
        }
    }
}

impl AspectStackValueDynamic for TypeSourceDataTypeDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceDataTypeDynamic { data: self.data.clone() }),
            }),
        }
    }
}

impl Type for TypeSourceDataTypeDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeDynamic { data: self.data.clone() })
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectLiftDynamic>,
                ),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }
        if let Some(data) = &self.data {
            if *key == data.identity {
                return Some(Box::new(true));
            }
            if *key == aspect_field_aspect_id() {
                return Some(
                    Box::new(
                        Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectField>,
                    ),
                );
            }
            let interface_impls = data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if *key == impl_data.interface.identity {
                    return Some(Box::new(true));
                }
            }
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        if let Some(data) = &self.data {
            if type_.aspect(xx, &data.identity).is_some() {
                return true;
            }
            let interface_impls = data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if type_.aspect(xx, &impl_data.interface.identity).is_some() {
                    return true;
                }
            }
        }
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if let Some(data) = &self.data {
            if type_.aspect(xx, &data.identity).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceDataTypeDynamic { data: self.data.clone() }),
                    }),
                });
            }
            let interface_impls = data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if type_.aspect(xx, &impl_data.interface.identity).is_some() {
                    return EvalRes::ok(CodeValue {
                        code: cv.code,
                        value: Rc::new(ValueDynamic {
                            source: ValueDynamicSource::Expr,
                            type_: Rc::new(TypeSourceInterfaceConst { data: impl_data.interface.clone() }),
                        }),
                    });
                }
            }
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn desc(&self) -> String {
        if let Some(data) = &self.data {
            match data.description.borrow().as_deref() {
                Some(d) => d.to_string(),
                None => format!("data_type({})", data.name.as_deref().unwrap_or("?")),
            }
        } else {
            format!("definition_set_data_type_ref")
        }
    }
}

impl Value for TypeSourceDataTypeDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
