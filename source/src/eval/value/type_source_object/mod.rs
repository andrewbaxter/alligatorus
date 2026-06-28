pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        eval::{
            DynamicSourceBuilder,
            Scope,
            binding_id_dynamic_key,
            body_builtin_scope,
            code_uses_binding,
            const_eval,
            definition_set::DefinitionSet,
            dynamic::code::CodeDynamic,
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
        r#gen::{
            aspect_initial_bind_get,
            aspect_initial_bind_get_maybe,
            aspect_tuple_get_maybe,
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

trait CompiledSourceSink {
    fn set_compiled_source(&self, compiled: String);
}

fn build_method_body(
    impl_data: &SourceObjectInterfaceMethodImplData,
    xx: &ExprContext,
    root_loc: QExprId,
    interface: &Gc<super::type_source_interface::SourceInterfaceData>,
    dt_data: &Gc<SourceObjectData>,
    underlying_type: &Rc<dyn Type>,
) -> Result<MethodBodyArtifacts, Vec<ExprErr>> {
    let impl_expr_value = impl_data.impl_expr.borrow().clone().ok_or_else(|| vec![ExprErr {
        loc: root_loc,
        errors: vec![ExprSubErr::Opaque(format!("Interface method impl missing impl_expr"))],
    }])?;
    let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr_value.as_ref());
    let builtin_kind = interface.builtin_kind;
    let ret_type: Rc<dyn Type> = {
        let iface_methods = interface.methods.borrow();
        iface_methods[impl_data.interface_method_index]
            .ret_type
            .borrow()
            .clone()
            .expect("Interface method missing return type")
    };
    let body_ex = Rc::new(EvalContext {
        ix: xx.vx.ix.clone(),
        next_local_id: Counter::default(),
        externals: RefCell::new(Vec::new()),
        dynamic_scope: Cell::new(None),
        definition_sets: RefCell::new(std::collections::HashMap::new()),
    });
    let self_id = BindingId::Local(body_ex.next_local_id.next());
    impl_data.inner_binding_id.set(Some(self_id));
    let outer_id = BindingId::Local(body_ex.next_local_id.next());
    let mut body_scope = body_builtin_scope(xx);
    let outer_type: Rc<dyn Type> =
        Rc::new(super::type_source_object::dynamic::TypeSourceObjectDynamic { data: dt_data.clone() });
    if let Some(outer_self_ib) = aspect_initial_bind_get_maybe(xx, outer_type.as_ref()) {
        body_scope.insert(ConstKey::String("outer".to_string()), outer_self_ib.initial_bind(outer_id));
    }
    let is_arg_method = matches!(builtin_kind, Some(super::type_source_interface::BuiltinInterfaceKind::Type));
    let arg_param_ids: Vec<BindingId>;
    match builtin_kind {
        Some(super::type_source_interface::BuiltinInterfaceKind::Type) => {
            let arg_type = {
                let iface_methods = interface.methods.borrow();
                iface_methods[impl_data.interface_method_index]
                    .arg_record_type
                    .borrow()
                    .clone()
                    .expect("Type iface method missing arg_record_type")
            };
            let arg_ib = aspect_initial_bind_get(xx, root_loc, arg_type.as_ref()).into_res()?;
            body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(self_id));
            arg_param_ids = vec![];
        },
        Some(super::type_source_interface::BuiltinInterfaceKind::Binding) |
        Some(super::type_source_interface::BuiltinInterfaceKind::Target) => {
            if let Some(self_ib) = aspect_initial_bind_get_maybe(xx, underlying_type.as_ref()) {
                body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
            }
            let (data_field_names, method_arg_type) = {
                let iface_methods = interface.methods.borrow();
                let m = &iface_methods[impl_data.interface_method_index];
                (
                    m.data_field_names.clone(),
                    m.arg_type.borrow().clone().expect("Builtin Binding/Target method missing arg_type"),
                )
            };
            let param_types: Vec<Rc<dyn Type>> = match data_field_names.len() {
                0 => vec![],
                1 => vec![method_arg_type.clone()],
                _ => {
                    let tuple =
                        aspect_tuple_get_maybe(
                            xx,
                            method_arg_type.as_ref(),
                        ).expect("Multi-field builtin method arg_type must be a Tuple");
                    tuple.element_types()
                },
            };
            assert_eq!(
                param_types.len(),
                data_field_names.len(),
                "data_field_names ({}) does not match arg_type element count ({})",
                data_field_names.len(),
                param_types.len()
            );
            let mut ids = Vec::with_capacity(data_field_names.len());
            let mut fields = std::collections::BTreeMap::new();
            for (name, ty) in data_field_names.iter().zip(param_types.iter()) {
                let pid = BindingId::Local(body_ex.next_local_id.next());
                ids.push(pid);
                let ib = aspect_initial_bind_get(xx, root_loc, ty.as_ref()).into_res()?;
                fields.insert(
                    ConstKey::String(name.clone()),
                    Rc::<dyn crate::r#gen::Binding>::from(ib.initial_bind(pid)),
                );
            }
            let arg_record = Rc::new(crate::eval::value::type_argument_record::r#const::ArgumentRecord { fields });
            body_scope.insert(
                ConstKey::String("arg".to_string()),
                Box::new(
                    crate::eval::value::type_argument_record::r#const::ArgumentRecordBinding { record: arg_record },
                ),
            );
            arg_param_ids = ids;
        },
        _ => {
            if let Some(self_ib) = aspect_initial_bind_get_maybe(xx, underlying_type.as_ref()) {
                body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
            }
            let arg_id = BindingId::Local(body_ex.next_local_id.next());
            let (record_t, plain_t) = {
                let iface_methods = interface.methods.borrow();
                let m = &iface_methods[impl_data.interface_method_index];
                (m.arg_record_type.borrow().clone(), m.arg_type.borrow().clone())
            };
            let arg_type = record_t.or(plain_t);
            if let Some(arg_type) = arg_type {
                if let Some(arg_ib) = aspect_initial_bind_get_maybe(xx, arg_type.as_ref()) {
                    body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
                }
            }
            arg_param_ids = vec![arg_id];
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
    let impl_code =
        impl_cv
            .code
            .cast::<CodeDynamic>()
            .into_res()?
            .expect("Interface method body must produce dynamic code")
            .clone();
    let captured = method_xx.vx.externals.borrow().clone();
    Ok(MethodBodyArtifacts {
        self_id,
        outer_id,
        arg_param_ids,
        is_arg_method,
        impl_code,
        captured,
    })
}

fn emit_method_js(
    code1: &mut DynamicSourceBuilder,
    root_loc: QExprId,
    method_id: BindingId,
    inner_binding_id: BindingId,
    arg_param_ids: &[BindingId],
    outer_id: BindingId,
    data_field: &str,
    is_arg_method: bool,
    impl_code: &CodeDynamic,
    captured: &[(BindingId, External)],
    target_compiled: &dyn CompiledSourceSink,
    ds: &DefinitionSet,
) -> Result<(), Vec<ExprErr>> {
    let start_line = code1.lines.len();
    let func_name = binding_id_dynamic_key(method_id);
    let self_key = binding_id_dynamic_key(inner_binding_id);
    code1.push(root_loc, format!("var {};", func_name));
    code1.push(root_loc, "{".to_string());
    for (id, _) in captured {
        let local_key = binding_id_dynamic_key(*id);
        code1.push(root_loc, format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key));
    }
    if is_arg_method {
        code1.push(root_loc, format!("{} = function({}) {{", func_name, self_key));
    } else {
        let param_keys: Vec<String> = arg_param_ids.iter().map(|id| binding_id_dynamic_key(*id)).collect();
        code1.push(root_loc, format!("{} = function({}) {{", func_name, param_keys.join(", ")));
        code1.push(
            root_loc,
            format!(
                "let {} = this[{}];",
                self_key,
                serde_json::to_string(&ConstKey::String(data_field.to_string()).to_str()).unwrap(),
            ),
        );
    }
    if code_uses_binding(impl_code, outer_id) {
        code1.push(root_loc, format!("let {} = this;", binding_id_dynamic_key(outer_id)));
    }
    let body_var = render_code(code1, impl_code);
    code1.push(root_loc, format!("return {};", body_var));
    code1.push(root_loc, "};".to_string());
    code1.push(root_loc, "}".to_string());
    let compiled = code1.lines[start_line..].join("\n");
    if let Some(errs) = ds.check_mutable(root_loc).errors {
        return Err(errs);
    }
    target_compiled.set_compiled_source(compiled);
    Ok(())
}

struct MethodBodyArtifacts {
    arg_param_ids: Vec<BindingId>,
    captured: Vec<(BindingId, External)>,
    impl_code: CodeDynamic,
    is_arg_method: bool,
    outer_id: BindingId,
    self_id: BindingId,
}

pub(crate) fn register_in_set(ds: &Gc<DefinitionSet>, name: String, identity: Unique) -> Rc<SourceObjectRef> {
    let data_field = format!("_{}", ds.next_field_id.next());
    let dt_data = Gc::new(SourceObjectData {
        set: RefCell::new(Some(ds.clone())),
        name: Some(name),
        description: RefCell::new(None),
        underlying_type: DeserialLazy::new(),
        methods: RefCell::new(Vec::new()),
        interface_impls: RefCell::new(Vec::new()),
        identity,
        data_field,
        artifact_id: RefCell::new(None),
        new_artifact_id: RefCell::new(None),
    });
    ds.objects.borrow_mut().push(dt_data.clone());
    Rc::new(SourceObjectRef { data: dt_data })
}

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

impl SourceDataMethodData {
    pub fn install(
        &self,
        xx: &ExprContext,
        root_loc: QExprId,
        code1: &mut DynamicSourceBuilder,
        underlying_type: &Rc<dyn Type>,
        data_field: &str,
        ds: &DefinitionSet,
    ) -> Result<(), Vec<ExprErr>> {
        let method_id = self.binding_id.get().expect("Method binding ID not assigned");
        if let Some(compiled_source) = self.compiled_source.borrow().clone() {
            for line in compiled_source.lines() {
                code1.push(root_loc, line.to_string());
            }
            return Ok(());
        }
        let impl_expr = self.impl_expr.borrow();
        let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr.as_ref().unwrap().as_ref());
        let ret_type: Rc<dyn Type> = self.ret_type.borrow().clone().expect("Data type method missing return type");
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
        let impl_code =
            impl_cv.code.cast::<CodeDynamic>().into_res()?.expect("Method body must produce dynamic code").clone();
        let captured = method_xx.vx.externals.borrow().clone();
        *self.captured_externals.borrow_mut() = captured.clone();
        emit_method_js(
            code1,
            root_loc,
            method_id,
            self_id,
            &[arg_id],
            outer_id,
            data_field,
            false,
            &impl_code,
            &captured,
            self,
            ds,
        )
    }
}

impl CompiledSourceSink for SourceDataMethodData {
    fn set_compiled_source(&self, compiled: String) {
        *self.compiled_source.borrow_mut() = Some(compiled);
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

    pub fn install(
        self_gc: &Gc<Self>,
        xx: &ExprContext,
        root_loc: QExprId,
        code1: &mut DynamicSourceBuilder,
        ds: &DefinitionSet,
    ) -> Result<bool, Vec<ExprErr>> {
        let underlying_type: Rc<dyn Type> = self_gc.underlying_type.get().clone();
        let data_field = self_gc.data_field.clone();
        let mut any = false;
        for method in self_gc.methods.borrow().iter() {
            method.install(xx, root_loc, code1, &underlying_type, &data_field, ds)?;
            any = true;
        }
        for impl_data in self_gc.interface_impls.borrow().iter() {
            for method_impl in &impl_data.method_impls {
                method_impl.install(
                    xx,
                    root_loc,
                    code1,
                    &impl_data.interface,
                    self_gc,
                    &underlying_type,
                    &data_field,
                    ds,
                )?;
                any = true;
            }
        }
        Ok(any)
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
        let method_id = self.binding_id.get().unwrap();
        let body = build_method_body(self, xx, root_loc, interface, dt_data, underlying_type)?;
        let ds =
            dt_data.set.borrow().as_ref().expect("SourceObjectData missing definition set during finish").clone();
        *self.captured_externals.borrow_mut() = body.captured.clone();
        let mut code1 = DynamicSourceBuilder::default();
        emit_method_js(
            &mut code1,
            root_loc,
            method_id,
            body.self_id,
            &body.arg_param_ids,
            body.outer_id,
            data_field,
            body.is_arg_method,
            &body.impl_code,
            &body.captured,
            self,
            &ds,
        )
    }

    pub fn install(
        &self,
        xx: &ExprContext,
        root_loc: QExprId,
        code1: &mut DynamicSourceBuilder,
        interface: &Gc<super::type_source_interface::SourceInterfaceData>,
        dt_data: &Gc<SourceObjectData>,
        underlying_type: &Rc<dyn Type>,
        data_field: &str,
        ds: &DefinitionSet,
    ) -> Result<(), Vec<ExprErr>> {
        let method_id = self.binding_id.get().expect("Interface method impl binding ID not assigned");
        if let Some(compiled_source) = self.compiled_source.borrow().clone() {
            for line in compiled_source.lines() {
                code1.push(root_loc, line.to_string());
            }
            return Ok(());
        }
        let body = build_method_body(self, xx, root_loc, interface, dt_data, underlying_type)?;
        *self.captured_externals.borrow_mut() = body.captured.clone();
        emit_method_js(
            code1,
            root_loc,
            method_id,
            body.self_id,
            &body.arg_param_ids,
            body.outer_id,
            data_field,
            body.is_arg_method,
            &body.impl_code,
            &body.captured,
            self,
            ds,
        )
    }
}

impl CompiledSourceSink for SourceObjectInterfaceMethodImplData {
    fn set_compiled_source(&self, compiled: String) {
        *self.compiled_source.borrow_mut() = Some(compiled);
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
