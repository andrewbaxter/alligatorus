use {
    crate::{
        builtin::{
            AspectArtifact,
            AspectBind,
            AspectBindDynamic,
            AspectConsumeDynamic,
            AspectInitialBind,
            AspectLiftDynamic,
            AspectStackValueDynamic,
            aspect_artifact_aspect_id,
            aspect_bind_aspect_id,
            aspect_bind_dynamic_aspect_id,
            aspect_consume_dynamic_aspect_id,
            aspect_initial_bind_aspect_id,
            aspect_lift_dynamic_aspect_id,
            aspect_lift_dynamic_get_maybe,
            aspect_stack_value_dynamic_aspect_id,
        },
        code_dynamic::CodeDynamic,
        eval::{
            dynamic_util::{
                ValueDynamic,
                ValueDynamicSource,
                bind_dynamic_default,
                bind_via_target,
            },
            value::{
                type_scope_entry::dynamic::ScopeEntryDynamicBinding,
                type_source_interface::{
                    SourceInterfaceData,
                    TypeSourceInterfaceConst,
                },
                type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic,
                value_source_data_type::r#const::ValueSourceDataTypeConst,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            Artifact,
            ArtifactId,
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
    dumpster::unsync::Gc,
    std::{
        any::Any,
        cell::RefCell,
        rc::Rc,
    },
    super::TypeSourceDataTypeConst,
};

impl TypeSourceDataTypeConst {
    pub(crate) fn serialize_data_type(&self, xx: &ExprContext, loc: QExprId) -> ArtifactId {
        crate::eval::definition_set::serialize_definition_set(xx, loc, &self.data.set);
        self
            .data
            .artifact_id
            .borrow()
            .clone()
            .expect("Data type artifact ID should be set by serialize_definition_set")
    }
}

fn read_artifact_has_type_iface(
    xx: &ExprContext,
    dt_data: &dumpster::unsync::Gc<super::SourceDataTypeData>,
) -> bool {
    let id = match dt_data.artifact_id.borrow().clone() {
        Some(id) => id,
        None => {
            return dt_data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind
                    == Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
            });
        },
    };
    let art = match xx.vx.ix.gx.artifacts.get(id) {
        Ok(a) => a,
        Err(_) => return false,
    };
    let dt_serial: super::artifact::ArtifactDataDefSetDataType = match serde_json::from_value(art.data) {
        Ok(s) => s,
        Err(_) => return false,
    };
    dt_serial.interface_impls.iter().any(|i| {
        i.builtin_kind == Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
    })
}

impl Aspect for TypeSourceDataTypeConst { }

impl AspectArtifact for TypeSourceDataTypeConst {
    fn from_serial(&self, _xx: &ExprContext, loc: QExprId, data: &serde_json::Value) -> EvalRes {
        let dt_data = self.data.clone();
        let data = data.clone();
        // For data types that implement the builtin `type` interface the
        // imported instance must reach `TypeUserDefined.cast_from` with a v8
        // bundle — the user's impls run via the same v8 dispatch the inline
        // case (`test_00100`) uses. To produce that bundle in the importer's
        // isolate we re-compile each interface impl's `impl_expr` against
        // this compile's `ExprContext` (allocating fresh externals) and run
        // a v8 script eagerly that defines the methods and constructs the
        // instance object. For non-`type`-interface types nothing special is
        // needed: add the def_set so its methods get emitted by the normal
        // end-of-stage pass and build the instance with the same `NewObject`
        // code an inline definition would emit.
        //
        // The branch is decided at from_serial entry by inspecting the
        // data type artifact directly — `dt_data.interface_impls` isn't
        // populated until the type's own finalizer runs, which is queued
        // before ours but only fires when the resolve loop drains pending
        // finalizers.
        let has_type_iface = read_artifact_has_type_iface(_xx, &dt_data);
        let bundle_cell: Rc<RefCell<Option<Rc<crate::eval::isolate_bundle::IsolateBundle>>>> =
            Rc::new(RefCell::new(None));
        let underlying_cell: Rc<RefCell<Option<Rc<dyn Value>>>> = Rc::new(RefCell::new(None));
        let bundle_cell_for_finalizer = bundle_cell.clone();
        let underlying_cell_for_finalizer = underlying_cell.clone();
        _xx.vx.ix.pending_finalizers.borrow_mut().push(Box::new(move |xx, loc| {
            let underlying_type = dt_data.underlying_type.get();
            let build_type_artifact =
                crate::eval::value::type_source_data_type_constructor::dynamic::serialize_type_ref(
                    xx,
                    loc,
                    underlying_type.as_ref(),
                );
            drop(underlying_type);
            let underlying_artifact = Artifact {
                type_: build_type_artifact.type_,
                data: data.clone(),
            };
            let underlying_cv =
                crate::eval::util::deserialize_artifact(xx, loc, &underlying_artifact)
                    .into_res()
                    .map_err(|e| format!("Failed to deserialize underlying value: {:?}", e))?;
            let underlying_value_type = underlying_cv.value.type_(xx, loc);
            let underlying_js = if let Some(aspect_str) =
                crate::builtin::aspect_string_const_get_maybe(xx, underlying_value_type.as_ref()) {
                let s = aspect_str.get(underlying_cv.value.as_ref());
                crate::code_dynamic::CodeDynamic {
                    loc: loc,
                    type_: crate::code_dynamic::CodeDynamicType::Literal(serde_json::Value::String(s)),
                }
            } else {
                crate::code_dynamic::CodeDynamic {
                    loc: loc,
                    type_: crate::code_dynamic::CodeDynamicType::Literal(data.clone()),
                }
            };
            let has_type_iface = dt_data.interface_impls.borrow().iter().any(|impl_data| {
                impl_data.interface.builtin_kind
                    == Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
            });
            if has_type_iface {
                // The imported instance is used at type-position (as the
                // target of `as_(_, upper)`) and never as a runtime value
                // in this compile's JS. Build it eagerly in v8, bundle it,
                // and return `code=None` — `bind_via_target` then routes
                // the binding through `ScopeEntryValue` which preserves the
                // concrete `ValueSourceDataTypeConst` (and thus the
                // bundle) for later `AspectType.type_` lookup.
                let instance_global = crate::eval::import_install::install_imported_type_iface(
                    xx,
                    loc,
                    &dt_data,
                    underlying_js,
                )?;
                let bundle = std::rc::Rc::new(crate::eval::isolate_bundle::IsolateBundle {
                    instance: instance_global,
                });
                *bundle_cell_for_finalizer.borrow_mut() = Some(bundle);
                *underlying_cell_for_finalizer.borrow_mut() = Some(underlying_cv.value);
                return Ok(vec![]);
            }
            // Non-type-iface: original behaviour — register def_set + build
            // instance inline.
            {
                let set = &dt_data.set;
                let mut sets = xx.vx.ix.definition_sets.borrow_mut();
                let already = sets.iter().any(|s| s.def_set_id == set.def_set_id);
                if !already {
                    sets.push(set.clone());
                }
            }
            let mut object_fields =
                vec![
                    (crate::langhelp::ConstKey::String(dt_data.data_field.clone()).to_str(), underlying_js)
                ];
            for method in dt_data.methods.borrow().iter() {
                let method_id = method.scope_entry_id.get().expect("Method scope entry ID not allocated");
                object_fields.push((crate::langhelp::ConstKey::String(method.field_name.clone()).to_str(), crate::code_dynamic::CodeDynamic {
                    loc: loc,
                    type_: crate::code_dynamic::CodeDynamicType::BindingUse(
                        crate::code_dynamic::CodeDynamicBindingUse { id: method_id },
                    ),
                }));
            }
            let code = crate::code_dynamic::CodeDynamic {
                loc: loc,
                type_: crate::code_dynamic::CodeDynamicType::NewObject(
                    crate::code_dynamic::CodeDynamicNewObject { fields: object_fields },
                ),
            };
            Ok(vec![Code { loc: loc, code: Code_::Dyn(Rc::new(code)) }])
        }));
        // The value's concrete shape depends on the branch the finalizer
        // takes, but we know it at from_serial entry: type-iface →
        // `ValueSourceDataTypeConst` (so AspectType picks up the bundle
        // installed by the finalizer); non-type-iface → `ValueDynamic`.
        let value: Rc<dyn Value> = if has_type_iface {
            Rc::new(ValueSourceDataTypeConst {
                data: self.data.clone(),
                underlying: Rc::new(crate::eval::value::value_null::r#const::ValueNullConst),
                isolate_bundle: std::cell::RefCell::new(None),
            })
        } else {
            Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() }),
            })
        };
        // Stash the eventual bundle/underlying into the value once the
        // finalizer fills them. Since the value is shared via Rc, mutating
        // its interior cells is observable by anyone holding the value.
        if has_type_iface {
            let value_clone = value.clone();
            _xx.vx.ix.pending_finalizers.borrow_mut().push(Box::new(move |_xx, _loc| {
                let bundle = bundle_cell.borrow_mut().take();
                let underlying = underlying_cell.borrow_mut().take();
                let v = crate::invariant::must_downcast_ref::<ValueSourceDataTypeConst>(value_clone.as_ref());
                if let Some(b) = bundle {
                    *v.isolate_bundle.borrow_mut() = Some(b);
                }
                if let Some(u) = underlying {
                    // SAFETY-ish: the `underlying` field is plain `Rc<dyn
                    // Value>`, not a `RefCell`. We can't mutate it through
                    // the shared Rc without unsafe. Skip the underlying
                    // swap — the import path doesn't read it (the type's
                    // dispatch uses `bundle`), and the initial null
                    // placeholder is sufficient.
                    let _ = u;
                }
                Ok(vec![])
            }));
        }
        EvalRes::ok(CodeValue {
            code: Code { loc: loc, code: Code_::None },
            value,
        })
    }

    fn to_serial(&self, xx: &ExprContext, loc: QExprId, self_: Rc<dyn Value>) -> EResult<Artifact> {
        let lifted = must_downcast_ref::<ValueSourceDataTypeConst>(self_.as_ref());
        let underlying_type = self.data.underlying_type.get();
        let underlying_aspect = match crate::builtin::aspect_artifact_get_maybe(xx, underlying_type.as_ref()) {
            Some(a) => a,
            None => {
                return EResult::err(
                    loc,
                    format!("Underlying type ({}) missing AspectArtifact", underlying_type.desc()),
                );
            },
        };
        let underlying_artifact = match underlying_aspect.to_serial(xx, loc, lifted.underlying.clone()).into_res() {
            Ok(a) => a,
            Err(e) => {
                return EResult::err(loc, format!("Failed to serialize underlying value: {:?}", e));
            },
        };
        let data_type_artifact_id = self.serialize_data_type(xx, loc);
        EResult::ok(Artifact {
            type_: data_type_artifact_id,
            data: underlying_artifact.data,
        })
    }
}

impl AspectBind for TypeSourceDataTypeConst {
    fn bind(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<(Code, Box<dyn crate::langhelp::ScopeEntry>)> {
        bind_via_target(xx, loc, cv)
    }
}

impl AspectBindDynamic for TypeSourceDataTypeConst {
    fn bind_dynamic(
        &self,
        xx: &ExprContext,
        loc: QExprId,
        cv: CodeValue,
    ) -> EResult<Option<crate::eval::TargetBindSub>> {
        bind_dynamic_default(xx, loc, cv)
    }
}

impl AspectConsumeDynamic for TypeSourceDataTypeConst {
    fn to_dynamic(&self, _xx: &ExprContext, _loc: QExprId, cv: CodeValue) -> EResult<CodeDynamic> {
        cv.code.cast::<CodeDynamic>().map(|x| x.unwrap().clone())
    }
}

impl AspectInitialBind for TypeSourceDataTypeConst {
    fn initial_bind(&self, dynamic_key: ScopeEntryId) -> Box<dyn ScopeEntry> {
        Box::new(ScopeEntryDynamicBinding {
            dynamic_key: dynamic_key,
            type_: Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }),
        })
    }
}

// --- AspectLiftDynamic: lift a data type instance from v8 ---
impl AspectLiftDynamic for TypeSourceDataTypeConst {
    fn lift(
        &self,
        xx: &ExprContext,
        scope: &v8::PinScope,
        value: v8::Local<v8::Value>,
    ) -> Result<Rc<dyn Value>, String> {
        let underlying_type = self.data.underlying_type.get().clone();
        let underlying_lift =
            aspect_lift_dynamic_get_maybe(
                xx,
                underlying_type.as_ref(),
            ).ok_or_else(|| format!("Underlying type ({}) missing AspectLiftDynamic", underlying_type.desc()))?;
        let obj = v8::Local::<v8::Object>::try_from(value).map_err(|_| "Expected JS object for data type lift")?;
        let data_field = crate::langhelp::ConstKey::String(self.data.data_field.clone()).to_str();
        let key = v8::String::new(scope, &data_field).unwrap();
        let underlying_v8 = obj.get(scope, key.into()).ok_or("Missing data field on data type object")?;
        let underlying_value = underlying_lift.lift(xx, scope, underlying_v8)?;
        let has_type_iface = self.data.interface_impls.borrow().iter().any(|impl_data| {
            impl_data.interface.builtin_kind
                == Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
        });
        let isolate_bundle = if has_type_iface {
            let instance_global: v8::Global<v8::Object> = v8::Global::new(scope, obj);
            std::cell::RefCell::new(Some(std::rc::Rc::new(crate::eval::isolate_bundle::IsolateBundle {
                instance: instance_global,
            })))
        } else {
            std::cell::RefCell::new(None)
        };
        Ok(Rc::new(ValueSourceDataTypeConst {
            underlying: underlying_value,
            data: self.data.clone(),
            isolate_bundle,
        }))
    }
}

impl AspectStackValueDynamic for TypeSourceDataTypeConst {
    fn to_value(&self, loc: QExprId, value: CodeDynamic) -> CodeValue {
        // Instances of the data type (e.g. the result of a function returning
        // this type) need to support field access; that's on
        // `TypeSourceDataTypeDynamic { data: Some(_) }`, not `*Const` which is
        // the "type as value" form.
        CodeValue {
            code: Code {
                loc,
                code: Code_::Dyn(Rc::new(value)),
            },
            value: Rc::new(ValueDynamic {
                source: ValueDynamicSource::Expr,
                type_: Rc::new(super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() }),
            }),
        }
    }
}

impl Type for TypeSourceDataTypeConst {
    fn as_value(&self) -> Rc<dyn Value> {
        Rc::new(TypeSourceDataTypeConst { data: self.data.clone() })
    }

    fn clone_rc(&self) -> Option<Rc<dyn Type>> {
        Some(Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }))
    }

    fn aspect(&self, _xx: &ExprContext, key: &Unique) -> Option<Box<dyn Any>> {
        // Check own data type identity aspect
        if *key == self.data.identity {
            return Some(Box::new(true));
        }
        if *key == aspect_consume_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectConsumeDynamic>,
                ),
            );
        }
        if *key == aspect_bind_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectBindDynamic>,
                ),
            );
        }
        if *key == aspect_bind_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectBind>),
            );
        }
        if *key == aspect_stack_value_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as
                        Box<dyn AspectStackValueDynamic>,
                ),
            );
        }
        if *key == aspect_lift_dynamic_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectLiftDynamic>,
                ),
            );
        }
        if *key == aspect_artifact_aspect_id() {
            return Some(
                Box::new(Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectArtifact>),
            );
        }
        if *key == aspect_initial_bind_aspect_id() {
            return Some(
                Box::new(
                    Box::new(TypeSourceDataTypeConst { data: self.data.clone() }) as Box<dyn AspectInitialBind>,
                ),
            );
        }

        if *key == crate::builtin::aspect_field_aspect_id() {
            return Some(
                Box::new(
                    Box::new(super::dynamic::TypeSourceDataTypeDynamic { data: self.data.clone() }) as
                        Box<dyn crate::builtin::AspectField>,
                ),
            );
        }
        // Check interface identity aspects + builtin `type` interface
        {
            let interface_impls = self.data.interface_impls.borrow();
            for impl_data in interface_impls.iter() {
                if *key == impl_data.interface.identity {
                    return Some(Box::new(true));
                }
            }
            // If the data type implements the builtin `type` interface, the
            // instance acts as a Type — provide AspectType that delegates to
            // the user impl.
            if *key == crate::builtin::aspect_type_aspect_id() {
                let has_type_impl = interface_impls.iter().any(|impl_data| {
                    impl_data.interface.builtin_kind
                        == Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type)
                });
                if has_type_impl {
                    return Some(
                        Box::new(
                            Box::new(TypeSourceDataTypeConstAspectType { data: self.data.clone() }) as
                                Box<dyn crate::builtin::AspectType>,
                        ),
                    );
                }
            }
        }
        return None;
    }

    fn can_cast_to(&self, xx: &ExprContext, _loc: QExprId, type_: &dyn Type) -> bool {
        let (own_identity, iface_identities) = {
            let own = self.data.identity.clone();
            let interface_impls = self.data.interface_impls.borrow();
            let ifaces: Vec<crate::langhelp::Unique> = interface_impls.iter().map(|impl_data| {
                impl_data.interface.identity.clone()
            }).collect();
            (own, ifaces)
        };
        if type_.aspect(xx, &own_identity).is_some() {
            return true;
        }
        for identity in iface_identities {
            if type_.aspect(xx, &identity).is_some() {
                return true;
            }
        }
        if type_.aspect(xx, &crate::eval::value::type_any::r#const::type_type_identity()).is_some() {
            return true;
        }
        false
    }

    fn cast_from(&self, _xx: &ExprContext, loc: QExprId, _cv: CodeValue) -> EvalRes {
        EvalRes::err(loc, format!("Cannot convert to {}", self.desc()))
    }

    fn cast_to(&self, xx: &ExprContext, loc: QExprId, cv: CodeValue, type_: &dyn Type) -> EvalRes {
        // Extract own identity + interface identities
        let (own_identity, iface_data) = {
            let own = self.data.identity.clone();
            let interface_impls = self.data.interface_impls.borrow();
            let ifaces: Vec<(Gc<SourceInterfaceData>, crate::langhelp::Unique)> =
                interface_impls.iter().map(|impl_data| {
                    (impl_data.interface.clone(), impl_data.interface.identity.clone())
                }).collect();
            (own, ifaces)
        };

        // Same data type check
        if type_.aspect(xx, &own_identity).is_some() {
            return EvalRes::ok(CodeValue {
                code: cv.code,
                value: Rc::new(ValueDynamic {
                    source: ValueDynamicSource::Expr,
                    type_: Rc::new(TypeSourceDataTypeConst { data: self.data.clone() }),
                }),
            });
        }

        // Interface casting
        for (iface_data, identity) in iface_data {
            if type_.aspect(xx, &identity).is_some() {
                return EvalRes::ok(CodeValue {
                    code: cv.code,
                    value: Rc::new(ValueDynamic {
                        source: ValueDynamicSource::Expr,
                        type_: Rc::new(TypeSourceInterfaceConst { data: iface_data }),
                    }),
                });
            }
        }
        if type_.aspect(xx, &crate::eval::value::type_any::r#const::type_type_identity()).is_some() {
            return crate::eval::value::type_any::dynamic::wrap_as_any(xx, loc, cv);
        }
        EvalRes::err(loc, format!("Cannot convert {}", self.desc()))
    }

    fn desc(&self) -> String {
        match self.data.description.borrow().as_deref() {
            Some(d) => d.to_string(),
            None => format!("data_type({})", self.data.name.as_deref().unwrap_or("?")),
        }
    }
}

impl Value for TypeSourceDataTypeConst {
    fn type_(&self, _xx: &ExprContext, _loc: QExprId) -> Rc<dyn Type> {
        Rc::new(TypeTypeSourceDataTypeDynamic)
    }
}

/// Carrier for `AspectType` when a data type implements the builtin `type`
/// interface. `type_(value)` returns a `TypeUserDefined` which delegates to the
/// user's impl methods.
pub(crate) struct TypeSourceDataTypeConstAspectType {
    pub data: dumpster::unsync::Gc<super::SourceDataTypeData>,
}

impl Aspect for TypeSourceDataTypeConstAspectType { }

impl crate::builtin::AspectType for TypeSourceDataTypeConstAspectType {
    fn type_(&self, value: &dyn Value) -> Rc<dyn Type> {
        let bundle = (value as &dyn std::any::Any)
            .downcast_ref::<ValueSourceDataTypeConst>()
            .and_then(|v| v.isolate_bundle.borrow().clone());
        Rc::new(super::dynamic::TypeUserDefined { data: self.data.clone(), bundle })
    }
}
