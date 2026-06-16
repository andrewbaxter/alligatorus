use {
    crate::{
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
            AspectStackValueDynamic,
            ScopeEntry,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_field_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_stack_value_dynamic_aspect_id,
            aspect_string_const_get,
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
    pub data: Gc<SourceDataTypeData>,
}

impl Aspect for TypeSourceDataTypeDynamic { }

impl AspectBind for TypeSourceDataTypeDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::r#gen::ScopeEntry>)> {
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
                    // Fall through to underlying type's field access — e.g. for an nvalue whose
                    // underlying is `record(base, type)`, accessing `.base` reads the underlying
                    // record's field via `instance.<data_field>.<key>`.
                    //
                    // `Record::AspectField` has two branches: if `self.base` is `Some`, it emits a
                    // normal JS field access (good for dynamic receivers). If `None`, it tries to
                    // downcast the receiver value to a const `Record` — which fails for the
                    // `ValueDynamic` we'd build here. The Record stored as the underlying type was
                    // constructed by source-level `mb.record(...)`, which always sets `base: None`.
                    // Replace it with a base=Some(RecordBaseDynamic) copy so the dispatch lands in
                    // the dynamic-friendly branch.
                    let underlying_type = data.underlying_type.get().clone();
                    let dispatch_type: Rc<dyn crate::langhelp::Type> =
                        if let Some(record) =
                            (underlying_type.as_ref() as
                                &dyn std::any::Any).downcast_ref::<crate::eval::value::record::Record>() {
                            let mut fields = std::collections::BTreeMap::new();
                            for (k, f) in &record.fields {
                                let t = match f {
                                    crate::eval::value::record::RecordField::Dynamic(t) => t.clone(),
                                    crate::eval::value::record::RecordField::EvalRes(_) => {
                                        return erb.err_new(
                                            "data type underlying record field is non-type".to_string(),
                                        );
                                    },
                                };
                                fields.insert(k.clone(), crate::eval::value::record::RecordField::Dynamic(t),);
                            }
                            Rc::new(crate::eval::value::record::Record {
                                base: Some(Box::new(crate::eval::value::record::RecordBaseDynamic)),
                                fields,
                            })
                        } else {
                            underlying_type.clone()
                        };
                    let underlying_field = crate::r#gen::aspect_field_get_maybe(xx, dispatch_type.as_ref());
                    if let Some(underlying_field) = underlying_field {
                        let self_code = erb.must_er(self_.code.cast::<CodeDynamic>())?.unwrap().clone();

                        // Build `instance.<data_field>` as the new receiver, then dispatch field access
                        // on the underlying type.
                        let underlying_access = CodeDynamic {
                            loc,
                            type_: CodeDynamicType::FieldUse(CodeDynamicFieldUse {
                                base: Box::new(self_code),
                                field: crate::langhelp::ConstKey::String(data.data_field.clone()).to_str(),
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
                            field: crate::langhelp::ConstKey::String(field_name).to_str(),
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
            let underlying_lift =
                crate::r#gen::aspect_lift_dynamic_get_maybe(xx, underlying_type.as_ref()).ok_or_else(|| {
                    format!("Underlying type ({}) missing AspectLiftDynamic", underlying_type.desc())
                })?;
            let obj =
                v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lift")?;
            let data_field = crate::langhelp::ConstKey::String(data.data_field.clone()).to_str();
            let key = v8::String::new(scope, &data_field).unwrap();
            let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
            let underlying_value = underlying_lift.lift(xx, scope, underlying_v8)?;

            // If this data type implements the builtin `type` interface, the instance acts as
            // a Type at use sites — capture the JS handle and a clone of the shared isolate
            // so `TypePull.cast_from` can re-enter the isolate at as-time.
            let has_type_iface = data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind ==
                    Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
            });
            let isolate_bundle = if has_type_iface {
                let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, obj);
                std::cell::RefCell::new(Some(instance_global))
            } else {
                std::cell::RefCell::new(None)
            };
            Ok(Rc::new(crate::eval::value::value_source_data_type::r#const::ValueSourceDataTypeConst {
                underlying: underlying_value,
                data: data.clone(),
                isolate_bundle,
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

impl crate::r#gen::AspectLowerDynamic for TypeSourceDataTypeDynamic {
    fn lower(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let data = &self.data;
        let underlying_type = data.underlying_type.get().clone();
        let underlying_lower =
            crate::r#gen::aspect_lower_dynamic_get_maybe(xx, underlying_type.as_ref()).ok_or_else(|| {
                format!("Underlying type ({}) missing AspectLowerDynamic", underlying_type.desc())
            })?;
        let obj = v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lower")?;
        let data_field = crate::langhelp::ConstKey::String(data.data_field.clone()).to_str();
        let key = v8::String::new(scope, &data_field).unwrap();
        let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
        let underlying_value = underlying_lower.lower(xx, scope, underlying_v8)?;
        let has_type_iface = data.interface_impls.borrow().iter().any(|impl_data| {
            impl_data.interface.builtin_kind ==
                Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
        });
        let isolate_bundle = if has_type_iface {
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, obj);
            std::cell::RefCell::new(Some(instance_global))
        } else {
            std::cell::RefCell::new(None)
        };
        Ok(Rc::new(crate::eval::value::value_source_data_type::r#const::ValueSourceDataTypeConst {
            underlying: underlying_value,
            data: data.clone(),
            isolate_bundle,
        }))
    }
}

impl crate::r#gen::AspectType for TypeSourceDataTypeDynamic {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let data = self.data.clone();

        // If the value is a `ValueSourceDataTypeConst` (the lifted form of an instance),
        // thread its `isolate_bundle` through so `cast_from` can dispatch via v8.
        let bundle =
            (value as &dyn std::any::Any)
                .downcast_ref::<crate::eval::value::value_source_data_type::r#const::ValueSourceDataTypeConst>()
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
        if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as
                        Box<dyn crate::r#gen::AspectLowerDynamic>,
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

            // If the data type implements the builtin `type` interface, instances of it can
            // be used as types — `AspectType.type_(value)` returns a `TypePull` that
            // delegates `cast_from`/`can_cast_from`/`desc` to the user-provided impls.
            if *key == crate::r#gen::aspect_type_aspect_id() {
                let has_type_impl = interface_impls.iter().any(|impl_data| {
                    impl_data.interface.builtin_kind ==
                        Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
                });
                if has_type_impl {
                    return Some(
                        Box::new(
                            Box::new(TypeSourceDataTypeDynamic { data: self.data.clone() }) as
                                Box<dyn crate::r#gen::AspectType>,
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
        if type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some() {
            // Only castable to `any` when this data type implements the `type` interface —
            // instances of non-type data types are JS objects, not `Rc<dyn Type>`, so they
            // can't be represented as `any` (which is exactly `Rc<dyn Type>` in v8).
            return data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind ==
                    Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
            });
        }
        if type_.aspect(xx, &crate::r#gen::type_aspect_identity()).is_some() {
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

        // Check `any` BEFORE the interface_impls loop: if dest is the source `any`
        // builtin and this data type acts as a type (impls the `type` interface),
        // preserve the v8 instance via `__data_instance_to_type` rather than letting the
        // loop match `aspect_field`/`aspect_function` aspect identities (now that
        // `TypeAnyDynamic` answers those for dynamic field/call dispatch).
        if type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some() {
            let has_type_impl = data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind ==
                    Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
            });
            if has_type_impl {
                let source_code = match cv.code.cast::<CodeDynamic>() {
                    crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
                    _ => return EvalRes::err(loc, format!("cast {} → any: source has no runtime code", self.desc())),
                };
                let template: Rc<dyn Type> =
                    Rc::new(
                        crate::eval::value::type_source_data_type::TypeSourceDataTypeConst { data: self.data.clone() },
                    );
                let template_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                xx.vx.externals.borrow_mut().push((template_id, crate::langhelp::External::Type(template)));
                let template_code = CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(
                        crate::code_dynamic::CodeDynamicBindingUse { id: template_id },
                    ),
                };
                let call = CodeDynamic {
                    loc,
                    type_: CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                        func: Box::new(CodeDynamic {
                            loc,
                            type_: CodeDynamicType::BindingUse(
                                crate::code_dynamic::CodeDynamicBindingUse {
                                    id: crate::eval::DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID,
                                },
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
                        type_: Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
                    }),
                });
            }
        }
        let interface_impls = data.interface_impls.borrow();
        for impl_data in interface_impls.iter() {
            if type_.aspect(xx, &impl_data.interface.identity).is_some() {
                // Type interface: wrap the runtime instance into a `Rc<dyn Type>` external via
                // `__data_instance_to_type` so downstream `__record_to_type` / `__tuple_to_type`
                // see an external rather than a raw instance object. For the
                // `Type`/`Target`/`ScopeEntry` builtin interfaces, the runtime value needs to be
                // a `v8::External` wrapping the matching `Rc<dyn …>` so downstream
                // `Push::from_v8` (or `__record_to_type` / `__tuple_to_type` for `Type`)
                // sees an external rather than a raw instance object. The cast emits a
                // `__data_instance_to_<kind>(template, instance)` call which the rust-in-v8
                // callback turns into the matching external. For each builtin iface, the runtime
                // value is `Rc<dyn …>` wrapped in a `v8::External` — exactly the representation
                // of the corresponding `…Dynamic` dynamic type. The result CV's static type must
                // match that runtime form (each type has a single representation, so a Dyn-coded
                // CV cannot be typed `TypeSourceInterfaceConst`, whose representation is the
                // compile-time iface ref).
                let runtime_setup: Option<(crate::langhelp::ScopeEntryId, Rc<dyn crate::langhelp::Type>)> =
                    match impl_data.interface.builtin_kind {
                        Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type) => Some(
                            (
                                crate::eval::DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID,
                                Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic),
                            ),
                        ),
                        Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Target) => Some(
                            (
                                crate::eval::DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID,
                                Rc::new(crate::eval::value::type_any_target::dynamic::TypeAnyTargetDynamic),
                            ),
                        ),
                        Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::ScopeEntry) => Some(
                            (
                                crate::eval::DATA_INSTANCE_TO_SCOPE_ENTRY_FN_SCOPE_ID,
                                Rc::new(crate::eval::value::type_any_scope_entry::dynamic::TypeAnyScopeEntryDynamic),
                            ),
                        ),
                        Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Value) => Some(
                            (
                                crate::eval::DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID,
                                Rc::new(crate::eval::value::type_value::dynamic::TypeValueDynamic),
                            ),
                        ),
                        Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::CodeDyn) => Some(
                            (
                                crate::eval::DATA_INSTANCE_TO_CODE_FN_SCOPE_ID,
                                Rc::new(crate::eval::value::type_code_source::TypeCodeDynHandleDynamic),
                            ),
                        ),
                        _ => None,
                    };
                if let Some((fn_id, result_type)) = runtime_setup {
                    let source_code = match cv.code.cast::<CodeDynamic>() {
                        crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
                        _ => return EvalRes::err(
                            loc,
                            format!("cast {} → interface: source has no runtime code", self.desc()),
                        ),
                    };

                    // Capture a `TypeSourceDataTypeConst` carrying this data type as an
                    // External::Type; the v8 callback receives it alongside the runtime instance and
                    // produces the appropriate `Rc<dyn …>` external.
                    let template: Rc<dyn Type> =
                        Rc::new(
                            crate::eval::value::type_source_data_type::TypeSourceDataTypeConst {
                                data: self.data.clone(),
                            },
                        );
                    let template_id = ScopeEntryId::Local(xx.vx.next_local_id.next());
                    xx.vx.externals.borrow_mut().push((template_id, crate::langhelp::External::Type(template)));
                    let template_code = CodeDynamic {
                        loc,
                        type_: CodeDynamicType::BindingUse(
                            crate::code_dynamic::CodeDynamicBindingUse { id: template_id },
                        ),
                    };
                    let call = CodeDynamic {
                        loc,
                        type_: CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                            func: Box::new(CodeDynamic {
                                loc,
                                type_: CodeDynamicType::BindingUse(
                                    crate::code_dynamic::CodeDynamicBindingUse { id: fn_id },
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
        if type_.aspect(xx, &crate::r#gen::type_aspect_identity()).is_some() {
            // Emit `__data_instance_to_aspect(<instance>)` — wraps the v8 instance into an
            // `AspectPull` external.
            let self_code = match cv.code.cast::<CodeDynamic>() {
                crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
                _ => return EvalRes::err(loc, format!("cast {} → aspect: source has no runtime code", self.desc())),
            };
            let call = CodeDynamic {
                loc,
                type_: crate::code_dynamic::CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                    func: Box::new(CodeDynamic {
                        loc,
                        type_: crate::code_dynamic::CodeDynamicType::BindingUse(
                            crate::code_dynamic::CodeDynamicBindingUse {
                                id: crate::eval::DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID,
                            },
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
                    type_: Rc::new(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic),
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

    fn type_to_serial(&self, xx: &ExprContext, loc: QExprId) -> crate::langhelp::Artifact {
        let data = &self.data;
        crate::eval::definition_set::serialize_definition_set(xx, loc, &data.set);
        let artifact_id = data.artifact_id.borrow().clone().expect("type_to_serial: data type artifact_id not set");
        crate::langhelp::Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        }
    }
}

impl Value for TypeSourceDataTypeDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

/// Parameterless dynamic marker for `Rc<SourceDataTypeRef>` v8 marshaling. A
/// `SourceDataTypeRef` is a reference to a data-type _definition_, not an instance
/// of one (that's `TypeSourceDataTypeDynamic`); the two need distinct static-type
/// singletons. The v8 runtime value stored at field accessors is
/// `Rc<dyn Type> = TypeSourceDataTypeConst`, which carries the actual data type;
/// `lift` unwraps that to the const form.
pub(crate) struct TypeSourceDataTypeRefDynamic;

impl Aspect for TypeSourceDataTypeRefDynamic { }

impl AspectBind for TypeSourceDataTypeRefDynamic {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::r#gen::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeRefDynamic {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
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
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let dt_ref: &Rc<super::SourceDataTypeRef> = from_v8_ref(&value);
        dt_ref.data.set.finish()?;
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

impl crate::r#gen::AspectLowerDynamic for TypeSourceDataTypeRefDynamic {
    fn lower(
        &self,
        _xx: &ExprContext,
        _scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        // Same as `lift` but does NOT finalize the def-set.
        let dt_ref: &Rc<super::SourceDataTypeRef> = from_v8_ref(&value);
        Ok(Rc::new(TypeSourceDataTypeConst { data: dt_ref.data.clone() }))
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
        if *key == crate::r#gen::aspect_lower_dynamic_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeRefDynamic) as Box<dyn crate::r#gen::AspectLowerDynamic>),
            );
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
        // Convert to any type-of-types (e.g. `TypeAnyDynamic`, `TypeAnyConst`).
        type_.aspect(xx, &crate::r#gen::type_type_identity()).is_some()
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, "Cannot convert to definition_set_data_type_ref".to_string())
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        if type_.aspect(xx, &crate::r#gen::type_type_identity()).is_none() {
            return EvalRes::err(loc, format!("Cannot convert {} to {}", self.desc(), type_.desc()));
        }

        // Emit a call to the `__data_type_ref_to_type` rust-in-v8 function. It reads the
        // `Rc<SourceDataTypeRef>` from the v8 value (the ref's representation) and
        // returns a `Rc<dyn Type> = TypeSourceDataTypeConst` (the representation the dest
        // expects).
        let source_code = match cv.code.cast::<CodeDynamic>() {
            crate::langhelp::EResult { v: Some(Some(c)), .. } => c.clone(),
            _ => return EvalRes::err(
                loc,
                format!("cast {} → {}: source has no runtime code", self.desc(), type_.desc()),
            ),
        };
        let call = CodeDynamic {
            loc,
            type_: CodeDynamicType::Call(crate::code_dynamic::CodeDynamicCall {
                func: Box::new(CodeDynamic {
                    loc,
                    type_: CodeDynamicType::BindingUse(
                        crate::code_dynamic::CodeDynamicBindingUse {
                            id: crate::eval::DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID,
                        },
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

    fn type_to_serial(&self, _xx: &ExprContext, _loc: QExprId) -> crate::langhelp::Artifact {
        Self::build_type_artifact()
    }
}

impl Value for TypeSourceDataTypeRefDynamic {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}

/// A type produced by `AspectType.type_` on a data-type instance whose data type
/// implements the builtin `type` interface. When the instance carries an
/// bundle handle, `cast_from` emits JS that re-enters the shared v8 isolate and
/// invokes the compiled `_im2` method on the original JS instance. Without a
/// bundle (e.g. for newly-constructed const values that haven't been through v8),
/// falls back to the const-eval dispatch in `pull_type_dispatch`.
pub(crate) struct TypePull {
    pub bundle: Option<v8::Global<v8::Object>>,
    pub data: Gc<SourceDataTypeData>,
}

impl Aspect for TypePull { }

impl Type for TypePull {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypePull {
            data: self.data.clone(),
            bundle: self.bundle.clone(),
        })
    }

    fn aspect(&self, xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        // First check user-defined `is_aspect` interfaces directly: if this data type
        // impls one whose identity matches `key`, return a `TypePullAspect` handle
        // (carries the iface ref, so source `aspect_handle.method(arg)` dispatches via
        // the iface's compiled method `field_name` against the bundle's v8 instance).
        let bundle_opt = self.bundle.as_ref();
        if let Some(bundle) = bundle_opt {
            let interface_impls = self.data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if *key == impl_data.interface.identity && impl_data.interface.is_aspect.get() {
                    return Some(
                        Box::new(Box::new(crate::eval::value::value_pull::aspect::TypePullAspect {
                            iface_ref: std::rc::Rc::new(
                                crate::eval::value::type_source_interface::SourceInterfaceRef {
                                    data: impl_data.interface.clone(),
                                },
                            ),
                            bundle: Some(bundle.clone()),
                        }) as Box<dyn crate::r#gen::AspectField>),
                    );
                }
            }
        }

        // Delegate to the source-level `type.aspect(key)` impl for builtin aspect_field /
        // aspect_function. The result is an `option(aspect)`; on `some`, the wrapped
        // `AspectPull` implements both `AspectField` and `AspectFunction`
        // (forwarding to v8 dispatch on the instance), so we cast to whichever trait the
        // caller asked for via `key`.
        let bundle = bundle_opt?;
        let dummy_loc: crate::langhelp::QExprId = (crate::langhelp::SourceId(0), crate::syntax::ExprId(0));
        let aspect =
            crate::eval::pull_type_dispatch::pull_aspect_v8(&self.data, bundle, xx, dummy_loc, key)?;
        if *key == crate::r#gen::aspect_field_aspect_id() {
            return Some(
                Box::new(Box::new(aspect) as Box<dyn crate::r#gen::AspectField>),
            );
        }
        if *key == crate::r#gen::aspect_function_aspect_id() {
            return Some(
                Box::new(Box::new(aspect) as Box<dyn crate::r#gen::AspectFunction>),
            );
        }
        None
    }

    fn can_cast_from(&self, xx: &ExprContext, loc: QExprId, source_type: &dyn Type) -> bool {
        if let Some(bundle) = &self.bundle {
            return crate::eval::pull_type_dispatch::pull_can_cast_from_v8(
                &self.data,
                bundle,
                xx,
                loc,
                source_type,
            );
        }
        crate::eval::pull_type_dispatch::pull_can_cast_from(&self.data, xx, loc, source_type)
    }

    fn cast_from(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue) -> EvalRes {
        if let Some(bundle) = &self.bundle {
            return crate::eval::pull_type_dispatch::pull_cast_from_v8(
                &self.data,
                bundle,
                xx,
                loc,
                cv,
            );
        }
        crate::eval::pull_type_dispatch::pull_cast_from(&self.data, xx, loc, cv)
    }

    fn cast_to(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue, _type_: &dyn Type) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn desc(&self) -> String {
        crate::eval::pull_type_dispatch::pull_desc(&self.data)
    }

    fn type_to_serial(&self, xx: &ExprContext, loc: QExprId) -> crate::langhelp::Artifact {
        crate::eval::definition_set::serialize_definition_set(xx, loc, &self.data.set);
        let artifact_id =
            self
                .data
                .artifact_id
                .borrow()
                .clone()
                .expect("type_to_serial: data type artifact_id not set after serialize_definition_set");
        crate::langhelp::Artifact {
            type_: artifact_id,
            data: serde_json::Value::Null,
        }
    }
}

impl Value for TypePull {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
    }
}
