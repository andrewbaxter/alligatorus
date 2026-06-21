pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            JsSourceBuilder,
            Scope,
            artifact::{
                ArtifactId,
                impl_serialize_independent_artifact as produce_independent_artifact,
            },
            build_aspect_arg_record_type,
            build_builtin_scope,
            build_cast_arg_record_type,
            code_uses_binding,
            const_eval,
            definition_set::{
                DefinitionSet,
                serialize_captured_externals,
            },
            expr::Expr,
            expr_artifact::{
                ArtifactExpr,
                expr_to_artifact,
            },
            from_v8_ref,
            push::Push,
            render_code,
            scope_entry_id_dynamic_key,
            to_v8,
            typecheck_convert,
            value,
            value::{
                type_source_data_type::artifact::{
                    ArtifactDataDefSetDataType,
                    ArtifactDataDefSetInterface,
                    ArtifactDataDefSetInterfaceImpl,
                    ArtifactDataDefSetInterfaceMethod,
                    ArtifactDataDefSetInterfaceMethodImpl,
                    ArtifactDataDefSetMethod,
                },
                type_source_data_type_constructor::dynamic::serialize_type_ref,
            },
        },
        invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
        langhelp::{
            ConstKey,
            Counter,
            EvalContext,
            ExprContext,
            ExprErr,
            ExprSubErr,
            External,
            QExprId,
            ScopeEntryId,
            Type,
            Unique,
            Value,
        },
        r#gen::{
            aspect_initial_bind_get,
            aspect_initial_bind_get_maybe,
        },
    },
    dumpster::unsync::Gc,
    std::{
        cell::{
            Cell,
            RefCell,
        },
        rc::Rc,
    },
};

pub(crate) struct SourceDataMethodData {
    pub arg_type: RefCell<Option<Rc<dyn Type>>>,
    pub artifact_id: RefCell<Option<ArtifactId>>,
    pub captured_externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub compiled_js: RefCell<Option<String>>,
    pub field_name: String,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub inner_scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub name: String,
    pub ret_type: RefCell<Option<Rc<dyn Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
}

impl SourceDataMethodData {
    pub fn finish(
        &self,
        xx: &ExprContext,
        root_loc: QExprId,
        dt_name: &str,
        underlying_type: &Rc<dyn Type>,
        data_field: &str,
    ) -> Result<(), Vec<ExprErr>> {
        if self.compiled_js.borrow().is_some() {
            return Ok(());
        }
        let impl_expr_value = self.impl_expr.borrow().clone().ok_or_else(|| vec![ExprErr {
            loc: root_loc,
            errors: vec![
                ExprSubErr::Opaque(
                    format!("Data type '{}' method '{}' is missing implementation", dt_name, self.name),
                ),
            ],
        }])?;
        let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr_value.as_ref());
        let ret_type = self.ret_type.borrow().clone().ok_or_else(|| vec![ExprErr {
            loc: root_loc,
            errors: vec![
                ExprSubErr::Opaque(format!("Data type '{}' method '{}' is missing a return type", dt_name, self.name)),
            ],
        }])?;
        let method_id = self.scope_entry_id.get().unwrap();
        let builtin_scope_entries = xx.vx.ix.builtin_scope_entries.borrow().clone();
        let body_ex = Rc::new(EvalContext {
            ix: xx.vx.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
            v8_scope: Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
        });
        let self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        self.inner_scope_entry_id.set(Some(self_id));
        let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        let outer_id = ScopeEntryId::Builtin(usize::MAX);
        let mut body_scope = build_builtin_scope(xx, root_loc, &builtin_scope_entries)?;
        if let Some(self_ib) = aspect_initial_bind_get_maybe(xx, underlying_type.as_ref()) {
            body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
        }
        if let Some(arg_type) = self.arg_type.borrow().clone() {
            if let Some(arg_ib) = aspect_initial_bind_get_maybe(xx, arg_type.as_ref()) {
                body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
            }
        }
        let method_xx = Rc::new(ExprContext {
            vx: body_ex,
            import_state: xx.import_state.clone(),
            scope: Rc::new(Scope {
                values: RefCell::new(body_scope),
                parent: None,
            }),
            target: xx.target.clone(),
        });
        let impl_result = const_eval(&method_xx, impl_expr_ast);
        if let Some(errs) = impl_result.errors {
            return Err(errs);
        }
        let impl_cv = impl_result.v.unwrap();
        let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
        if let Some(errs) = r.errors {
            return Err(errs);
        }
        let impl_cv = r.v.unwrap();
        let impl_code = impl_cv.code.cast::<CodeDynamic>().into_res()?.unwrap().clone();
        let captured = method_xx.vx.externals.borrow().clone();
        let mut code1 = JsSourceBuilder::default();
        let func_name = scope_entry_id_dynamic_key(method_id);
        let self_key = scope_entry_id_dynamic_key(self_id);
        code1.push(root_loc, format!("var {};", func_name));
        code1.push(root_loc, "{".to_string());
        for (id, _) in &captured {
            let local_key = scope_entry_id_dynamic_key(*id);
            code1.push(root_loc, format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key));
        }
        let arg_key = scope_entry_id_dynamic_key(arg_id);
        code1.push(root_loc, format!("{} = function({}) {{", func_name, arg_key));
        code1.push(
            root_loc,
            format!(
                "let {} = this[{}];",
                self_key,
                serde_json::to_string(&ConstKey::String(data_field.to_string()).to_str()).unwrap(),
            ),
        );
        if code_uses_binding(&impl_code, outer_id) {
            code1.push(root_loc, format!("let {} = this;", scope_entry_id_dynamic_key(outer_id)));
        }
        let body_var = render_code(&mut code1, &impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "};".to_string());
        code1.push(root_loc, "}".to_string());
        let compiled = code1.lines.join("\n");
        *self.captured_externals.borrow_mut() = captured;
        *self.compiled_js.borrow_mut() = Some(compiled);
        Ok(())
    }

    pub fn serialize(&self, xx: &ExprContext, loc: QExprId) -> ArtifactId {
        use crate::{
            eval::{
                artifact::Artifact,
                value::type_null::dynamic::TypeNullDynamic,
            },
            r#gen::defset_method_artifact_id,
        };

        produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self.artifact_id, || {
            let compiled_js = self.compiled_js.borrow().clone().unwrap_or_default();
            let ret_type = self.ret_type.borrow().clone().unwrap_or_else(|| Rc::new(TypeNullDynamic));
            let arg_type: Rc<dyn Type> = self.arg_type.borrow().clone().unwrap_or_else(|| Rc::new(TypeNullDynamic));
            let captured_externals = serialize_captured_externals(xx, loc, &self.captured_externals.borrow());
            let entry_id = match self.scope_entry_id.get() {
                Some(ScopeEntryId::Def(_, eid)) => eid,
                _ => panic!("Method scope_entry_id missing or not Def"),
            };
            let serial = ArtifactDataDefSetMethod {
                name: self.name.clone(),
                entry_id,
                field_name: self.field_name.clone(),
                arg_type: serialize_type_ref(xx, loc, arg_type.as_ref()),
                ret_type: serialize_type_ref(xx, loc, ret_type.as_ref()),
                compiled_js,
                captured_externals,
            };
            Artifact {
                deserializer: crate::eval::artifact::ArtifactId::Builtin(defset_method_artifact_id()),
                data: serde_json::to_value(&serial).expect("Failed to serialize method"),
            }
        })
    }
}

pub(crate) struct SourceDataTypeData {
    pub artifact_id: RefCell<Option<ArtifactId>>,
    pub data_field: String,
    pub description: RefCell<Option<String>>,
    pub identity: Unique,
    pub interface_impls: RefCell<Vec<SourceDataTypeInterfaceImplData>>,
    pub methods: RefCell<Vec<Rc<SourceDataMethodData>>>,
    pub name: Option<String>,
    pub set: Gc<DefinitionSet>,
    pub underlying_type: DeserialLazy<Rc<dyn Type>>,
}

impl SourceDataTypeData {
    pub fn finish(self_gc: &Gc<Self>, xx: &ExprContext, root_loc: QExprId) -> Result<(), Vec<ExprErr>> {
        let name = self_gc.name.as_deref().unwrap_or("(unnamed)").to_string();
        let underlying_type: Rc<dyn Type> = self_gc.underlying_type.get().clone();
        let data_field = self_gc.data_field.clone();
        for method in self_gc.methods.borrow().iter() {
            method.finish(xx, root_loc, &name, &underlying_type, &data_field)?;
        }
        for impl_data in self_gc.interface_impls.borrow().iter() {
            for method_impl in &impl_data.method_impls {
                method_impl.finish(xx, root_loc, &impl_data.interface, self_gc, &underlying_type, &data_field)?;
            }
        }
        Ok(())
    }

    pub fn serialize(&self, xx: &ExprContext, loc: QExprId, def_set_artifact_id: ArtifactId) -> ArtifactId {
        produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self.artifact_id, || {
            let underlying_type = self.underlying_type.get();
            let mut members: Vec<ArtifactId> = Vec::new();
            for m in self.methods.borrow().iter() {
                members.push(m.serialize(xx, loc));
            }
            for impl_data in self.interface_impls.borrow().iter() {
                members.push(impl_data.serialize(xx, loc));
            }
            let serial = ArtifactDataDefSetDataType {
                name: self.name.clone(),
                data_field: self.data_field.clone(),
                underlying_type: serialize_type_ref(xx, loc, underlying_type.as_ref()),
                identity: self.identity.clone(),
                members,
                def_set: def_set_artifact_id,
            };
            crate::eval::value::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic::build_artifact(
                serde_json::to_value(&serial).expect("Failed to serialize data type"),
            )
        })
    }
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceDataTypeData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        let impls = self.interface_impls.try_borrow().map_err(|_| ())?;
        for impl_data in impls.iter() {
            visitor.visit_unsync(&impl_data.interface);
        }
        Ok(())
    }
}

pub(crate) struct SourceDataTypeInterfaceImplData {
    pub artifact_id: RefCell<Option<ArtifactId>>,
    pub interface: Gc<super::type_source_interface::SourceInterfaceData>,
    pub method_impls: Vec<SourceDataTypeInterfaceMethodImplData>,
}

impl SourceDataTypeInterfaceImplData {
    pub fn serialize(&self, xx: &ExprContext, loc: QExprId) -> ArtifactId {
        use crate::{
            eval::artifact::Artifact,
            r#gen::defset_iface_impl_artifact_id,
        };

        produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self.artifact_id, || {
            let builtin_kind = self.interface.builtin_kind;
            let method_impls: Vec<ArtifactId> = self.method_impls.iter().map(|m| m.serialize(xx, loc)).collect();
            let user_iface = match builtin_kind {
                Some(_) => None,
                None => Some(self.interface.serialize(xx, loc)),
            };
            let serial = ArtifactDataDefSetInterfaceImpl {
                builtin_kind,
                user_iface,
                method_impls,
            };
            Artifact {
                deserializer: crate::eval::artifact::ArtifactId::Builtin(defset_iface_impl_artifact_id()),
                data: serde_json::to_value(&serial).expect("Failed to serialize iface impl"),
            }
        })
    }
}

pub(crate) struct SourceDataTypeInterfaceMethodImplData {
    pub artifact_id: RefCell<Option<ArtifactId>>,
    pub captured_externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub compiled_js: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub impl_expr_artifact: RefCell<Option<ArtifactExpr>>,
    pub inner_scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub interface_method_index: usize,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
}

impl SourceDataTypeInterfaceMethodImplData {
    pub fn finish(
        &self,
        xx: &ExprContext,
        root_loc: QExprId,
        interface: &Gc<super::type_source_interface::SourceInterfaceData>,
        dt_data: &Gc<SourceDataTypeData>,
        underlying_type: &Rc<dyn Type>,
        data_field: &str,
    ) -> Result<(), Vec<ExprErr>> {
        if self.compiled_js.borrow().is_some() {
            return Ok(());
        }
        let impl_expr_value = self.impl_expr.borrow().clone().ok_or_else(|| vec![ExprErr {
            loc: root_loc,
            errors: vec![ExprSubErr::Opaque(format!("Interface method impl missing impl_expr"))],
        }])?;
        let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr_value.as_ref());
        let ret_type: Rc<dyn Type> = {
            let iface_methods = interface.methods.borrow();
            let iface_method = &iface_methods[self.interface_method_index];
            iface_method.ret_type.borrow().clone().unwrap()
        };
        let method_id = self.scope_entry_id.get().unwrap();
        let builtin_kind = interface.builtin_kind;
        let builtin_scope_entries = xx.vx.ix.builtin_scope_entries.borrow().clone();
        let body_ex = Rc::new(EvalContext {
            ix: xx.vx.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
            v8_scope: Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
        });
        let self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        self.inner_scope_entry_id.set(Some(self_id));
        let outer_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        let mut body_scope = build_builtin_scope(xx, root_loc, &builtin_scope_entries)?;
        let outer_type: Rc<dyn Type> =
            Rc::new(super::type_source_data_type::dynamic::TypeSourceDataTypeDynamic { data: dt_data.clone() });
        if let Some(outer_self_ib) = aspect_initial_bind_get_maybe(xx, outer_type.as_ref()) {
            body_scope.insert(ConstKey::String("outer".to_string()), outer_self_ib.initial_bind(outer_id));
        }
        let method_name = {
            let iface_methods = interface.methods.borrow();
            iface_methods[self.interface_method_index].name.clone()
        };
        match builtin_kind {
            Some(super::type_source_interface::BuiltinInterfaceKind::Type) => {
                if method_name == "aspect" {
                    let mut fields = std::collections::BTreeMap::new();
                    fields.insert(
                        ConstKey::String("key".to_string()),
                        value::record::RecordField::Dynamic(
                            Rc::new(value::value_unique::TypeUnique) as Rc<dyn Type>,
                        ),
                    );
                    let arg_record_type: Rc<dyn Type> = Rc::new(value::record::Record {
                        base: Some(Box::new(value::record::RecordBaseDynamic)),
                        fields,
                    });
                    let arg_ib = aspect_initial_bind_get(xx, root_loc, arg_record_type.as_ref()).into_res()?;
                    body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(self_id));
                } else {
                    let arg_record_type = build_cast_arg_record_type();
                    let arg_ib = aspect_initial_bind_get(xx, root_loc, arg_record_type.as_ref()).into_res()?;
                    body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(self_id));
                }
            },
            Some(super::type_source_interface::BuiltinInterfaceKind::AspectFunction) |
            Some(super::type_source_interface::BuiltinInterfaceKind::AspectField) |
            Some(super::type_source_interface::BuiltinInterfaceKind::ScopeEntry) |
            Some(super::type_source_interface::BuiltinInterfaceKind::Target) => {
                if let Some(self_ib) = aspect_initial_bind_get_maybe(xx, underlying_type.as_ref()) {
                    body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
                }
                let arg_field_names: Vec<&str> = match (builtin_kind, method_name.as_str()) {
                    (Some(super::type_source_interface::BuiltinInterfaceKind::AspectFunction), "call") => {
                        vec!["inner", "argument"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::AspectField), "field") => {
                        vec!["inner", "key"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::AspectField), "field_type_opt") => {
                        vec!["key"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::ScopeEntry), "use") => {
                        vec!["loc", "code"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "bind") => {
                        vec!["xx", "loc", "sub"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "bind_sub_record") => {
                        vec!["loc", "carry", "fields"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "bind_sub_tuple") => {
                        vec!["loc", "carry", "elements"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "seq") => vec![
                        "loc",
                        "entries",
                    ],
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "code_to_bind_sub") => {
                        vec!["loc", "code", "type"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "if") => {
                        vec!["xx", "loc", "conditions", "else"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "match") => {
                        vec!["xx", "loc", "base", "element_key", "cases", "fallback"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "maybe_bind_sub") => {
                        vec!["xx", "loc", "cv"]
                    },
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Target), "stage") => {
                        vec!["xx", "loc", "tree", "lower_values"]
                    },
                    _ => vec![],
                };
                let arg_type: Rc<dyn Type> = build_aspect_arg_record_type(&arg_field_names);
                if let Some(arg_ib) = aspect_initial_bind_get_maybe(xx, arg_type.as_ref()) {
                    body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
                }
            },
            _ => {
                if let Some(self_ib) = aspect_initial_bind_get_maybe(xx, underlying_type.as_ref()) {
                    body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
                }
                let iface_method_arg_type = {
                    let iface_methods = interface.methods.borrow();
                    iface_methods[self.interface_method_index].arg_type.borrow().clone()
                };
                if let Some(arg_type) = iface_method_arg_type {
                    if let Some(arg_ib) = aspect_initial_bind_get_maybe(xx, arg_type.as_ref()) {
                        body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
                    }
                }
            },
        }
        let method_xx = Rc::new(ExprContext {
            vx: body_ex,
            import_state: xx.import_state.clone(),
            scope: Rc::new(Scope {
                values: RefCell::new(body_scope),
                parent: None,
            }),
            target: xx.target.clone(),
        });
        let impl_result = const_eval(&method_xx, impl_expr_ast);
        if let Some(errs) = impl_result.errors {
            return Err(errs);
        }
        let impl_cv = impl_result.v.unwrap();
        let r = typecheck_convert(&method_xx, root_loc, ret_type.as_ref(), impl_cv);
        if let Some(errs) = r.errors {
            return Err(errs);
        }
        let impl_cv = r.v.unwrap();
        let impl_code = impl_cv.code.cast::<CodeDynamic>().into_res()?.unwrap().clone();
        let captured = method_xx.vx.externals.borrow().clone();
        let is_arg_method = matches!(builtin_kind, Some(super::type_source_interface::BuiltinInterfaceKind::Type));
        let mut code1 = JsSourceBuilder::default();
        let func_name = scope_entry_id_dynamic_key(method_id);
        let self_key = scope_entry_id_dynamic_key(self_id);
        code1.push(root_loc, format!("var {};", func_name));
        code1.push(root_loc, "{".to_string());
        for (id, _) in &captured {
            let local_key = scope_entry_id_dynamic_key(*id);
            code1.push(root_loc, format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key));
        }
        if is_arg_method {
            code1.push(root_loc, format!("{} = function({}) {{", func_name, self_key));
        } else {
            let arg_key = scope_entry_id_dynamic_key(arg_id);
            code1.push(root_loc, format!("{} = function({}) {{", func_name, arg_key));
            code1.push(
                root_loc,
                format!(
                    "let {} = this[{}];",
                    self_key,
                    serde_json::to_string(&ConstKey::String(data_field.to_string()).to_str()).unwrap(),
                ),
            );
        }
        if code_uses_binding(&impl_code, outer_id) {
            code1.push(root_loc, format!("let {} = this;", scope_entry_id_dynamic_key(outer_id)));
        }
        let body_var = render_code(&mut code1, &impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "};".to_string());
        code1.push(root_loc, "}".to_string());
        let compiled = code1.lines.join("\n");
        *self.captured_externals.borrow_mut() = captured;
        *self.compiled_js.borrow_mut() = Some(compiled);
        Ok(())
    }

    pub fn serialize(&self, xx: &ExprContext, loc: QExprId) -> ArtifactId {
        use crate::{
            eval::artifact::Artifact,
            r#gen::defset_iface_method_impl_artifact_id,
        };

        produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self.artifact_id, || {
            let compiled_js = self.compiled_js.borrow().clone().unwrap_or_default();
            let entry_id = match self.scope_entry_id.get().expect("Interface method impl missing scope_entry_id") {
                ScopeEntryId::Def(_, id) => id,
                other => panic!("Interface method impl has non-Def scope_entry_id: {:?}", other),
            };
            if self.impl_expr_artifact.borrow().is_none() {
                let impl_expr_value = self.impl_expr.borrow().clone();
                if let Some(impl_expr_value) = impl_expr_value {
                    let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr_value.as_ref());
                    if let Ok(serialized) = expr_to_artifact(xx, impl_expr_ast) {
                        *self.impl_expr_artifact.borrow_mut() = Some(serialized);
                    }
                }
            }
            let impl_expr = self.impl_expr_artifact.borrow().clone().unwrap_or_else(|| {
                use crate::eval::expr_artifact::{
                    ArtifactExpr,
                    ArtifactExpr_,
                    ArtifactQExprId,
                };

                ArtifactExpr {
                    loc: ArtifactQExprId(0, 0),
                    r#type: Box::new(
                        ArtifactExpr_::Literal {
                            value: crate::eval::value::type_null::r#const::TypeNullConst::build_artifact(()),
                        },
                    ),
                }
            });
            let captured_externals = serialize_captured_externals(xx, loc, &self.captured_externals.borrow());
            let serial = ArtifactDataDefSetInterfaceMethodImpl {
                interface_method_index: self.interface_method_index,
                entry_id,
                compiled_js,
                impl_expr,
                captured_externals,
            };
            Artifact {
                deserializer: crate::eval::artifact::ArtifactId::Builtin(defset_iface_method_impl_artifact_id()),
                data: serde_json::to_value(&serial).expect("Failed to serialize iface method impl"),
            }
        })
    }
}

pub(crate) struct SourceDataTypeRef {
    pub data: Gc<SourceDataTypeData>,
}

impl Push for Rc<SourceDataTypeRef> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(dynamic::TypeSourceDataTypeRefDynamic)
    }

    fn from_v8<'s>(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_v8_ref::<Rc<SourceDataTypeRef>>(&*value).clone()
    }

    fn into_v8<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_v8(scope, self).into()
    }
}
