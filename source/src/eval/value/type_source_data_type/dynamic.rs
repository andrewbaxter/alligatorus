use {
    crate::{
        code_dynamic::{
            CodeDynamic,
            CodeDynamicBindingUse,
            CodeDynamicCall,
            CodeDynamicFieldUse,
            CodeDynamicType,
        },
        eval::{
            TargetBindSub,
            definition_set::serialize_definition_set,
            dynamic_util::{
                bind_dynamic_default,
                bind_via_target,
            },
            from_v8_ref,
            value::{
                record::{
                    Record,
                    RecordBaseDynamic,
                    RecordField,
                },
                type_any::{
                    dynamic::TypeAnyDynamic,
                    r#const::TypeAnyConst,
                },
                type_any_scope_entry::dynamic::TypeAnyScopeEntryDynamic,
                type_any_target::dynamic::TypeAnyTargetDynamic,
                type_aspect::dynamic::TypeAspectDynamic,
                type_code_source::TypeCodeDynHandleDynamic,
                type_function::dynamic::TypeFunctionDynamic,
                type_null::dynamic::TypeNullDynamic,
                type_pull::TypePull,
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_interface::{
                    BuiltinInterfaceKind,
                    r#const::TypeSourceInterfaceConst,
                },
                type_value::dynamic::TypeValueDynamic,
                value_dynamic::{
                    ValueDynamic,
                    ValueDynamicSource,
                },
                value_source_data_type::r#const::ValueSourceDataTypeConst,
            },
        },
        langhelp::{
            Artifact,
            Aspect,
            Code,
            CodeValue,
            Code_,
            ConstKey,
            EResult,
            EvalRes,
            ExprContext,
            External,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectField,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectLowerDynamic,
            AspectStackValueDynamic,
            AspectType,
            DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID,
            DATA_INSTANCE_TO_CODE_FN_SCOPE_ID,
            DATA_INSTANCE_TO_SCOPE_ENTRY_FN_SCOPE_ID,
            DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID,
            DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID,
            DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID,
            DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_field_get_maybe,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_lift_dynamic_get_maybe,
            aspect_lower_dynamic_aspect_id,
            aspect_lower_dynamic_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
            aspect_type_aspect_id,
            type_aspect_identity,
            type_type_identity,
        },
    },
    dumpster::unsync::Gc,
    std::{
        any::Any,
        rc::Rc,
    },
    super::{
        SourceDataTypeData,
        r#const::TypeSourceDataTypeConst,
    },
};

pub(crate) struct TypeSourceDataTypeDynamic {
    pub data: Gc<SourceDataTypeData>,
}

impl Aspect for TypeSourceDataTypeDynamic { }

impl AspectBind for TypeSourceDataTypeDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
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
        let data = &self.data;
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
                        let arg_t: Rc<dyn Type> =
                            m.arg_type.borrow().clone().unwrap_or_else(|| Rc::new(TypeNullDynamic));
                        break 'lookup Some((m.field_name.clone(), arg_t, ret_t));
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
                None => {
                    let underlying_type = data.underlying_type.get().clone();
                    let dispatch_type: Rc<dyn Type> =
                        if let Some(record) =
                            (underlying_type.as_ref() as &dyn std::any::Any).downcast_ref::<Record>() {
                            let mut fields = std::collections::BTreeMap::new();
                            for (k, f) in &record.fields {
                                let t = match f {
                                    RecordField::Dynamic(t) => t.clone(),
                                    RecordField::EvalRes(_) => {
                                        return erb.err_new(
                                            "data type underlying record field is non-type".to_string(),
                                        );
                                    },
                                };
                                fields.insert(k.clone(), RecordField::Dynamic(t),);
                            }
                            Rc::new(Record {
                                base: Some(Box::new(RecordBaseDynamic)),
                                fields,
                            })
                        } else {
                            underlying_type.clone()
                        };
                    let underlying_field = aspect_field_get_maybe(xx, dispatch_type.as_ref());
                    if let Some(underlying_field) = underlying_field {
                        let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();
                        let underlying_access = CodeDynamic {
                            loc,
                            type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                                base: Box::new(self_code),
                                field: ConstKey::String(data.data_field.clone()).to_str(),
                            }),
                        };
                        let underlying_cv = CodeValue {
                            code: Code {
                                loc,
                                code: Code_::Dyn(Rc::new(underlying_access))
                            },
                            value: Rc::new(ValueDynamic {
                                source: ValueDynamicSource::Expr,
                                type_: dispatch_type.clone(),
                            }),
                        };
                        let v = erb.must_er(underlying_field.field(xx, loc, underlying_cv, key))?;
                        return Ok(v);
                    }
                    return erb.err_new(format!("Field '{}' not found on data type", key_name));
                },
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
                            field: ConstKey::String(field_name).to_str(),
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
        let data = &self.data;
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
        {
            let data = &self.data;
            let underlying_type = data.underlying_type.get().clone();
            let underlying_lift = aspect_lift_dynamic_get_maybe(xx, underlying_type.as_ref()).ok_or_else(|| {
                format!("Underlying type ({}) missing AspectLiftDynamic", underlying_type.desc())
            })?;
            let obj =
                v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lift")?;
            let data_field = ConstKey::String(data.data_field.clone()).to_str();
            let key = v8::String::new(scope, &data_field).unwrap();
            let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
            let underlying_value = underlying_lift.lift(xx, scope, underlying_v8)?;
            let has_type_iface = data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
            });
            let isolate_bundle = if has_type_iface {
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, obj);
                std::cell::RefCell::new(Some(instance_global))
            } else {
                std::cell::RefCell::new(None)
            };
            Ok(Rc::new(ValueSourceDataTypeConst {
                underlying: underlying_value,
                data: data.clone(),
                isolate_bundle,
            }))
        }
    }
}

impl AspectLowerDynamic for TypeSourceDataTypeDynamic {
    fn lower(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let data = &self.data;
        let underlying_type = data.underlying_type.get().clone();
        let underlying_lower = aspect_lower_dynamic_get_maybe(xx, underlying_type.as_ref()).ok_or_else(|| {
            format!("Underlying type ({}) missing AspectLowerDynamic", underlying_type.desc())
        })?;
        let obj = v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lower")?;
        let data_field = ConstKey::String(data.data_field.clone()).to_str();
        let key = v8::String::new(scope, &data_field).unwrap();
        let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
        let underlying_value = underlying_lower.lower(xx, scope, underlying_v8)?;
        let has_type_iface = data.interface_impls.borrow().iter().any(|impl_data| {
            impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
        });
        let isolate_bundle = if has_type_iface {
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, obj);
            std::cell::RefCell::new(Some(instance_global))
        } else {
            std::cell::RefCell::new(None)
        };
        Ok(Rc::new(ValueSourceDataTypeConst {
            underlying: underlying_value,
            data: data.clone(),
            isolate_bundle,
        }))
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

impl AspectType for TypeSourceDataTypeDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let data = self.data.clone();
        let bundle =
            (value as &dyn std::any::Any)
                .downcast_ref::<ValueSourceDataTypeConst>()
                .and_then(|v| v.isolate_bundle.borrow().clone());
        Rc::new(TypePull {
            data,
            bundle,
        })
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
        if *key == aspect_lower_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectLowerDynamic>,
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
        {
            let data = &self.data;
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
            if *key == aspect_type_aspect_id() {
                let has_type_impl = interface_impls.iter().any(|impl_data| {
                    impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
                });
                if has_type_impl {
                    return Some(
                        Box::new(
                            Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as Box<dyn AspectType>,
                        ),
                    );
                }
            }
        }
        return None;
    }

    fn can_cast_from(&self, xx: &ExprContext, _loc: QExprId, source_type: &dyn Type) -> bool {
        source_type.aspect(xx, &self.data.identity).is_some()
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        let data = &self.data;
        if type_.aspect(xx, &data.identity).is_some() {
            return true;
        }
        let interface_impls = data.interface_impls.borrow();
        for impl_data in interface_impls.iter() {
            if type_.aspect(xx, &impl_data.interface.identity).is_some() {
                return true;
            }
        }
        if type_.aspect(xx, &type_type_identity()).is_some() {
            return data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
            });
        }
        if type_.aspect(xx, &type_aspect_identity()).is_some() {
            return true;
        }
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        let data = &self.data;
        if type_.aspect(xx, &data.identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceDataTypeDynamic { data: self.data.clone() }),
                }),
            });
        }
        if type_.aspect(xx, &type_type_identity()).is_some() {
            let has_type_impl = data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type)
            });
            if has_type_impl {
                let source_code = match cv.code.cast::<CodeDynamic>() {
                    EResult { v: Some(Some(c)), .. } => c.clone(),
                    _ => return EvalRes::err(loc, format!("cast {} → any: source has no runtime code", self.desc())),
                };
                let template: Rc<dyn Type> = Rc::new(TypeSourceDataTypeConst { data: self.data.clone() });
                let template_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                xx.vx.externals.borrow_mut().push((template_id, External::Type(template)));
                let template_code = CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: template_id }),
                };
                let call = CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Call(CodeDynamicCall {
                        func: Box::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(
                                CodeDynamicBindingUse { id: DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID },
                            ),
                        }),
                        args: vec![template_code, source_code],
                    }),
                };
                return EvalRes::ok(CodeValue {
                    code: Code {
                        loc,
                        code: Code_::Dyn(Rc::new(call)),
                    },
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeAnyDynamic),
                    }),
                });
            }
        }
        let interface_impls = data.interface_impls.borrow();
        for impl_data in interface_impls.iter() {
            if type_.aspect(xx, &impl_data.interface.identity).is_some() {
                let runtime_setup: Option<(ScopeEntryId, Rc<dyn Type>)> = match impl_data.interface.builtin_kind {
                    Some(BuiltinInterfaceKind::Type) => Some(
                        (DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID, Rc::new(TypeAnyDynamic)),
                    ),
                    Some(BuiltinInterfaceKind::Target) => Some(
                        (DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID, Rc::new(TypeAnyTargetDynamic)),
                    ),
                    Some(BuiltinInterfaceKind::ScopeEntry) => Some(
                        (DATA_INSTANCE_TO_SCOPE_ENTRY_FN_SCOPE_ID, Rc::new(TypeAnyScopeEntryDynamic)),
                    ),
                    Some(BuiltinInterfaceKind::Value) => Some(
                        (DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID, Rc::new(TypeValueDynamic)),
                    ),
                    Some(BuiltinInterfaceKind::CodeDyn) => Some(
                        (DATA_INSTANCE_TO_CODE_FN_SCOPE_ID, Rc::new(TypeCodeDynHandleDynamic)),
                    ),
                    _ => None,
                };
                if let Some((fn_id, result_type)) = runtime_setup {
                    let source_code = match cv.code.cast::<CodeDynamic>() {
                        EResult { v: Some(Some(c)), .. } => c.clone(),
                        _ => return EvalRes::err(
                            loc,
                            format!("cast {} → interface: source has no runtime code", self.desc()),
                        ),
                    };
                    let template: Rc<dyn Type> = Rc::new(TypeSourceDataTypeConst { data: self.data.clone() });
                    let template_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                    xx.vx.externals.borrow_mut().push((template_id, External::Type(template)));
                    let template_code = CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: template_id }),
                    };
                    let call = CodeDynamic {
                        loc,
                        type_: CodeDynamicType::Call(CodeDynamicCall {
                            func: Box::new(CodeDynamic {
                                loc,
                                type_: CodeDynamicType::BindingUse(CodeDynamicBindingUse { id: fn_id }),
                            }),
                            args: vec![template_code, source_code],
                        }),
                    };
                    return EvalRes::ok(CodeValue {
                        code: Code {
                            loc,
                            code: Code_::Dyn(Rc::new(call)),
                        },
                        value: Rc::new(ValueDynamic {
                            source: ValueDynamicSource::Expr,
                            type_: result_type,
                        }),
                    });
                }
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceInterfaceConst { data: impl_data.interface.clone() }),
                    }),
                });
            }
        }
        if type_.aspect(xx, &type_aspect_identity()).is_some() {
            let self_code = match cv.code.cast::<CodeDynamic>() {
                EResult { v: Some(Some(c)), .. } => c.clone(),
                _ => return EvalRes::err(loc, format!("cast {} → aspect: source has no runtime code", self.desc())),
            };
            let call = CodeDynamic {
                loc,
                type_: CodeDynamicType::Call(CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(
                            CodeDynamicBindingUse { id: DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID },
                        ),
                    }),
                    args: vec![self_code],
                }),
            };
            return EvalRes::ok(CodeValue {
                code: Code {
                    loc,
                    code: Code_::Dyn(Rc::new(call)),
                },
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeAspectDynamic),
                }),
            });
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn desc(&self) -> String {
        let data = &self.data;
        match data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("data_type({})", data.name.as_deref().unwrap_or("?")),
        }
    }

    fn type_to_serial(&self, xx: &ExprContext, loc: QExprId) -> Artifact {
        let data = &self.data;
        serialize_definition_set(xx, loc, &data.set);
        let artifact_id = data.artifact_id.borrow().clone().expect("type_to_serial: data type artifact_id not set");
        Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        }
    }
}

impl Value for TypeSourceDataTypeDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
pub(crate) struct TypeSourceDataTypeRefDynamic;

impl Aspect for TypeSourceDataTypeRefDynamic { }

impl AspectBind for TypeSourceDataTypeRefDynamic {
    fn bind(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<(Code, Box<dyn ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeRefDynamic {
    fn bind_dynamic(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EResult<Option<TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceDataTypeRefDynamic {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceDataTypeRefDynamic {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key,
            type_: Rc::new(TypeSourceDataTypeRefDynamic),
        })
    }
}

impl AspectLiftDynamic for TypeSourceDataTypeRefDynamic {
    fn lift(
        &self,
        xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let dt_ref: &Rc<super::SourceDataTypeRef> = from_v8_ref(&value);
        dt_ref.data.set.finish(xx)?;
        Ok(Rc::new(TypeSourceDataTypeConst { data: dt_ref.data.clone() }))
    }
}

impl AspectLowerDynamic for TypeSourceDataTypeRefDynamic {
    fn lower(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let dt_ref: &Rc<super::SourceDataTypeRef> = from_v8_ref(&value);
        Ok(Rc::new(TypeSourceDataTypeConst { data: dt_ref.data.clone() }))
    }
}

impl AspectStackValueDynamic for TypeSourceDataTypeRefDynamic {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(TypeSourceDataTypeRefDynamic),
            }),
        }
    }
}

impl Type for TypeSourceDataTypeRefDynamic {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeRefDynamic)
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn AspectConsumeDynamic>));
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn AspectBindDynamic>));
        }
        if *key == aspect_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn AspectBind>));
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn AspectInitialBind>));
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn AspectLiftDynamic>));
        }
        if *key == aspect_lower_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn AspectLowerDynamic>));
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn AspectStackValueDynamic>));
        }
        None
    }

    fn can_cast_from(&self, _xx: &ExprContext, _loc: QExprId, _source_type: &dyn Type) -> bool {
        false
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        type_.aspect(xx, &type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_ref".to_string())
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
                        CodeDynamicBindingUse { id: DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID },
                    ),
                }),
                args: vec![source_code],
            }),
        };
        let dest_rc = type_.clone_rc().unwrap_or_else(|| Rc::new(TypeSourceDataTypeRefDynamic) as Rc<dyn Type>);
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

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceDataTypeRefDynamic))
    }

    fn desc(&self) -> String {
        "definition_set_data_type_ref".to_string()
    }

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> Artifact {
        Self::build_type_artifact()
    }
}

impl Value for TypeSourceDataTypeRefDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeAnyConst)
    }
}
