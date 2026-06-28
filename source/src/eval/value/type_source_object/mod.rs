pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            DynamicSourceBuilder,
            Scope,
            binding_id_dynamic_key,
            body_builtin_scope,
            code_uses_binding,
            const_eval,
            definition_set::DefinitionSet,
            expr::Expr,
            from_dynamic_ref,
            push::Push,
            render_code,
            to_dynamic_ext,
            typecheck_convert,
        },
        invariant::{
            DeserialLazy,
            must_downcast_ref,
        },
        langhelp::{
            BindingId,
            ConstKey,
            Counter,
            EvalContext,
            ExprContext,
            ExprErr,
            ExprSubErr,
            External,
            QExprId,
            Type,
            Unique,
            Value,
        },
        r#gen::aspect_initial_bind_get_maybe,
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
    pub binding_id: Cell<Option<BindingId>>,
    pub captured_externals: RefCell<Vec<(BindingId, External)>>,
    pub compiled_source: RefCell<Option<String>>,
    pub field_name: String,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub inner_binding_id: Cell<Option<BindingId>>,
    pub name: String,
    pub ret_type: RefCell<Option<Rc<dyn Type>>>,
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
        if self.compiled_source.borrow().is_some() {
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
        let method_id = self.binding_id.get().unwrap();
        let body_ex = Rc::new(EvalContext {
            ix: xx.vx.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
            dynamic_scope: Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
        });
        let self_id = BindingId::Local(body_ex.next_local_id.next());
        self.inner_binding_id.set(Some(self_id));
        let arg_id = BindingId::Local(body_ex.next_local_id.next());
        let outer_id = BindingId::Builtin(usize::MAX);
        let mut body_scope = body_builtin_scope(xx);
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
            current_exception_type: None,
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
        let mut code1 = DynamicSourceBuilder::default();
        let func_name = binding_id_dynamic_key(method_id);
        let self_key = binding_id_dynamic_key(self_id);
        code1.push(root_loc, format!("var {};", func_name));
        code1.push(root_loc, "{".to_string());
        for (id, _) in &captured {
            let local_key = binding_id_dynamic_key(*id);
            code1.push(root_loc, format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key));
        }
        let arg_key = binding_id_dynamic_key(arg_id);
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
            code1.push(root_loc, format!("let {} = this;", binding_id_dynamic_key(outer_id)));
        }
        let body_var = render_code(&mut code1, &impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "};".to_string());
        code1.push(root_loc, "}".to_string());
        let compiled = code1.lines.join("\n");
        *self.captured_externals.borrow_mut() = captured;
        *self.compiled_source.borrow_mut() = Some(compiled);
        Ok(())
    }
}

pub(crate) struct SourceObjectData {
    pub artifact_id: RefCell<Option<crate::artifact::ArtifactIdSource>>,
    pub data_field: String,
    pub description: RefCell<Option<String>>,
    pub identity: Unique,
    pub interface_impls: RefCell<Vec<SourceObjectInterfaceImplData>>,
    pub methods: RefCell<Vec<Rc<SourceDataMethodData>>>,
    pub name: Option<String>,
    pub new_artifact_id: RefCell<Option<crate::artifact::ArtifactIdSource>>,
    pub set: RefCell<Option<Gc<DefinitionSet>>>,
    pub underlying_type: DeserialLazy<Rc<dyn Type>>,
}

impl SourceObjectData {
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
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceObjectData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        let set = self.set.try_borrow().map_err(|_| ())?;
        if let Some(s) = set.as_ref() {
            visitor.visit_unsync(s);
        }
        drop(set);
        let impls = self.interface_impls.try_borrow().map_err(|_| ())?;
        for impl_data in impls.iter() {
            visitor.visit_unsync(&impl_data.interface);
        }
        Ok(())
    }
}

pub(crate) struct SourceObjectInterfaceImplData {
    pub interface: Gc<super::type_source_interface::SourceInterfaceData>,
    pub method_impls: Vec<SourceObjectInterfaceMethodImplData>,
}

pub(crate) struct SourceObjectInterfaceMethodImplData {
    pub binding_id: Cell<Option<BindingId>>,
    pub captured_externals: RefCell<Vec<(BindingId, External)>>,
    pub compiled_source: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub inner_binding_id: Cell<Option<BindingId>>,
    pub interface_method_index: usize,
}

impl SourceObjectInterfaceMethodImplData {
    pub fn finish(
        &self,
        xx: &ExprContext,
        root_loc: QExprId,
        interface: &Gc<super::type_source_interface::SourceInterfaceData>,
        dt_data: &Gc<SourceObjectData>,
        underlying_type: &Rc<dyn Type>,
        data_field: &str,
    ) -> Result<(), Vec<ExprErr>> {
        if self.compiled_source.borrow().is_some() {
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
        let method_id = self.binding_id.get().unwrap();
        let builtin_kind = interface.builtin_kind;
        let body_ex = Rc::new(EvalContext {
            ix: xx.vx.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
            dynamic_scope: Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
        });
        let self_id = BindingId::Local(body_ex.next_local_id.next());
        self.inner_binding_id.set(Some(self_id));
        let outer_id = BindingId::Local(body_ex.next_local_id.next());
        let arg_id = BindingId::Local(body_ex.next_local_id.next());
        let mut body_scope = body_builtin_scope(xx);
        let outer_type: Rc<dyn Type> =
            Rc::new(super::type_source_object::dynamic::TypeSourceObjectDynamic { data: dt_data.clone() });
        if let Some(outer_self_ib) = aspect_initial_bind_get_maybe(xx, outer_type.as_ref()) {
            body_scope.insert(ConstKey::String("outer".to_string()), outer_self_ib.initial_bind(outer_id));
        }
        let (iface_method_arg_record_type, iface_method_arg_type) = {
            let iface_methods = interface.methods.borrow();
            let m = &iface_methods[self.interface_method_index];
            (m.arg_record_type.borrow().clone(), m.arg_type.borrow().clone())
        };
        let arg_binding_id = match builtin_kind {
            Some(super::type_source_interface::BuiltinInterfaceKind::Type) => self_id,
            _ => arg_id,
        };
        let method_name = {
            let iface_methods = interface.methods.borrow();
            iface_methods[self.interface_method_index].name.clone()
        };
        match builtin_kind {
            Some(super::type_source_interface::BuiltinInterfaceKind::Type) => {
                let arg_type =
                    iface_method_arg_record_type.clone().expect("Type iface method missing arg_record_type");
                let arg_ib = crate::r#gen::aspect_initial_bind_get(xx, root_loc, arg_type.as_ref()).into_res()?;
                body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_binding_id));
            },
            Some(super::type_source_interface::BuiltinInterfaceKind::Binding) |
            Some(super::type_source_interface::BuiltinInterfaceKind::Target) => {
                if let Some(self_ib) = aspect_initial_bind_get_maybe(xx, underlying_type.as_ref()) {
                    body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
                }
                let arg_field_names: Vec<&str> = match (builtin_kind, method_name.as_str()) {
                    (Some(super::type_source_interface::BuiltinInterfaceKind::Binding), "use") => {
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
                let mut fields = std::collections::BTreeMap::new();
                let cve_type: Rc<dyn Type> = Rc::new(crate::eval::code_value_extern::TypeCodeValueExternDynamic);
                for name in &arg_field_names {
                    fields.insert(
                        crate::langhelp::ConstKey::String(name.to_string()),
                        crate::eval::value::type_record::RecordField::Dynamic(cve_type.clone()),
                    );
                }
                let arg_type: Rc<dyn Type> = Rc::new(crate::eval::value::type_record::Record {
                    base: Some(Box::new(crate::eval::value::type_record::RecordBaseDynamic)),
                    fields,
                });
                if let Some(arg_ib) = aspect_initial_bind_get_maybe(xx, arg_type.as_ref()) {
                    body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_binding_id));
                }
            },
            _ => {
                if let Some(self_ib) = aspect_initial_bind_get_maybe(xx, underlying_type.as_ref()) {
                    body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
                }
                let arg_type = iface_method_arg_record_type.clone().or(iface_method_arg_type.clone());
                if let Some(arg_type) = arg_type {
                    if let Some(arg_ib) = aspect_initial_bind_get_maybe(xx, arg_type.as_ref()) {
                        body_scope.insert(
                            ConstKey::String("arg".to_string()),
                            arg_ib.initial_bind(arg_binding_id),
                        );
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
            current_exception_type: None,
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
        let mut code1 = DynamicSourceBuilder::default();
        let func_name = binding_id_dynamic_key(method_id);
        let self_key = binding_id_dynamic_key(self_id);
        code1.push(root_loc, format!("var {};", func_name));
        code1.push(root_loc, "{".to_string());
        for (id, _) in &captured {
            let local_key = binding_id_dynamic_key(*id);
            code1.push(root_loc, format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key));
        }
        if is_arg_method {
            code1.push(root_loc, format!("{} = function({}) {{", func_name, self_key));
        } else {
            let arg_key = binding_id_dynamic_key(arg_id);
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
            code1.push(root_loc, format!("let {} = this;", binding_id_dynamic_key(outer_id)));
        }
        let body_var = render_code(&mut code1, &impl_code);
        code1.push(root_loc, format!("return {};", body_var));
        code1.push(root_loc, "};".to_string());
        code1.push(root_loc, "}".to_string());
        let compiled = code1.lines.join("\n");
        *self.captured_externals.borrow_mut() = captured;
        *self.compiled_source.borrow_mut() = Some(compiled);
        Ok(())
    }
}

pub(crate) struct SourceObjectRef {
    pub data: Gc<SourceObjectData>,
}

impl Push for Rc<SourceObjectRef> {
    fn dynamic_type() -> Rc<dyn Type> {
        Rc::new(dynamic::TypeSourceObjectRefDynamic)
    }

    fn from_dynamic<
        's,
    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, value: v8::Local<'s, v8::Value>) -> Self {
        from_dynamic_ref::<Rc<SourceObjectRef>>(&*value).clone()
    }

    fn into_dynamic<'s>(self, scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>) -> v8::Local<'s, v8::Value> {
        to_dynamic_ext(scope, self).into()
    }
}
