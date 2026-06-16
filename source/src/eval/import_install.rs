use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            JsSourceBuilder,
            build_builtin_scope,
            build_cast_arg_record_type,
            const_eval,
            definition_set::DefinitionSet,
            expr::Expr,
            rc_type_to_v8,
            render_code,
            scope_entry_id_dynamic_key,
            to_v8,
            typecheck_convert,
            value::{
                type_source_data_type::SourceDataTypeData,
                type_source_interface::BuiltinInterfaceKind,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            ConstKey,
            Counter,
            EvalContext,
            ExprContext,
            External,
            QExprId,
            Scope,
            ScopeEntryId,
            Type,
        },
        r#gen::aspect_initial_bind_get,
    },
    dumpster::unsync::Gc,
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

pub(crate) fn install_def_set_to_v8(xx: &ExprContext, loc: QExprId, set: &Gc<DefinitionSet>) -> Result<(), String> {
    let Some(scope_ptr) = xx.vx.v8_scope.get() else {
        return Ok(());
    };
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let mut installed_externals: Vec<(String, External)> = Vec::new();
    let mut collect_externals = |func_name: &str, captured: &[(ScopeEntryId, External)]| {
        for (id, value) in captured {
            let local_key = scope_entry_id_dynamic_key(*id);
            installed_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
        }
    };
    for func_data in set.functions.borrow().iter() {
        let func_name =
            scope_entry_id_dynamic_key(
                func_data.scope_entry_id.get().ok_or_else(|| "func missing scope_entry_id".to_string())?,
            );
        collect_externals(&func_name, &func_data.captured_externals.borrow());
    }
    for dt in set.data_types.borrow().iter() {
        for method in dt.methods.borrow().iter() {
            let func_name =
                scope_entry_id_dynamic_key(
                    method.scope_entry_id.get().ok_or_else(|| "method missing scope_entry_id".to_string())?,
                );
            collect_externals(&func_name, &method.captured_externals.borrow());
        }
        for impl_data in dt.interface_impls.borrow().iter() {
            for m in &impl_data.method_impls {
                let func_name =
                    scope_entry_id_dynamic_key(
                        m.scope_entry_id.get().ok_or_else(|| "iface method missing scope_entry_id".to_string())?,
                    );
                collect_externals(&func_name, &m.captured_externals.borrow());
            }
        }
    }
    for (key_str, value) in &installed_externals {
        let key =
            v8::String::new(scope, key_str).ok_or_else(|| "install_def_set_to_v8: key alloc".to_string())?.cast();
        let v8_val: v8::Local<v8::Value> = match value {
            External::StagedExprCell(cell) => to_v8(scope, cell.clone()).into(),
            External::StageLiftCell(cell) => to_v8(scope, cell.clone()).into(),
            External::CodeValueExternCell(cell) => to_v8(scope, cell.clone()).into(),
            External::Type(t) => rc_type_to_v8(scope, t.clone()).into(),
            External::Unique(u) => to_v8(scope, *u).into(),
        };
        scope.get_current_context().global(scope).set(scope, key, v8_val);
    }
    let mut code = JsSourceBuilder::default();
    for func_data in set.functions.borrow().iter() {
        if let Some(js) = func_data.compiled_js.borrow().clone() {
            for line in js.lines() {
                code.push(loc, line.to_string());
            }
        }
    }
    for dt in set.data_types.borrow().iter() {
        for method in dt.methods.borrow().iter() {
            if let Some(js) = method.compiled_js.borrow().clone() {
                for line in js.lines() {
                    code.push(loc, line.to_string());
                }
            }
        }
        for impl_data in dt.interface_impls.borrow().iter() {
            for m in &impl_data.method_impls {
                if let Some(js) = m.compiled_js.borrow().clone() {
                    for line in js.lines() {
                        code.push(loc, line.to_string());
                    }
                }
            }
        }
    }
    if code.lines.is_empty() {
        return Ok(());
    }
    let source = code.lines.join("\n");
    let script =
        v8::Script::compile(
            scope,
            v8::String::new(scope, &source).ok_or_else(|| "install_def_set_to_v8: source alloc".to_string())?,
            None,
        ).ok_or_else(|| "install_def_set_to_v8: compile failed".to_string())?;
    script.run(scope).ok_or_else(|| "install_def_set_to_v8: run failed".to_string())?;
    Ok(())
}

pub(crate) fn install_imported_type_iface(
    xx: &ExprContext,
    loc: QExprId,
    dt_data: &Gc<SourceDataTypeData>,
    underlying_js: CodeDynamic,
) -> Result<v8::Global<v8::Object>, String> {
    let body_ex = Rc::new(EvalContext {
        ix: xx.vx.ix.clone(),
        next_local_id: Counter::default(),
        externals: RefCell::new(Vec::new()),
        v8_scope: std::cell::Cell::new(None),
    });
    let target = xx.target.clone();
    let builtin_scope_entries = xx.vx.ix.builtin_scope_entries.borrow().clone();
    let underlying_type = dt_data.underlying_type.get().clone();

    enum BodyShape {
        ArgMethod(ScopeEntryId),
        SelfMethod(ScopeEntryId, String),
    }

    let mut method_codes: Vec<(ScopeEntryId, BodyShape, CodeDynamic)> = Vec::new();
    for impl_data in dt_data.interface_impls.borrow().iter() {
        let is_type_iface = impl_data.interface.builtin_kind == Some(BuiltinInterfaceKind::Type);
        for method_impl in &impl_data.method_impls {
            let method_id =
                method_impl
                    .scope_entry_id
                    .get()
                    .ok_or_else(|| "Interface method impl missing scope_entry_id".to_string())?;
            let impl_expr_value =
                method_impl
                    .impl_expr
                    .borrow()
                    .clone()
                    .ok_or_else(|| "Interface method impl missing impl_expr".to_string())?;
            let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr_value.as_ref());
            let ret_type: Rc<dyn Type> = {
                let iface_methods = impl_data.interface.methods.borrow();
                let iface_method = &iface_methods[method_impl.interface_method_index];
                iface_method
                    .ret_type
                    .borrow()
                    .clone()
                    .ok_or_else(|| "Interface method missing return type".to_string())?
            };
            let self_id = ScopeEntryId::Local(body_ex.next_local_id.next());
            method_impl.inner_scope_entry_id.set(Some(self_id));
            let mut body_scope =
                build_builtin_scope(
                    xx,
                    loc,
                    &builtin_scope_entries,
                ).map_err(|e| format!("build_builtin_scope: {:?}", e))?;
            let body_shape = if is_type_iface {
                let arg_record_type = build_cast_arg_record_type();
                let arg_ib =
                    aspect_initial_bind_get(xx, loc, arg_record_type.as_ref())
                        .into_res()
                        .map_err(|e| format!("aspect_initial_bind for arg: {:?}", e))?;
                body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(self_id));
                BodyShape::ArgMethod(self_id)
            } else {
                let self_ib =
                    aspect_initial_bind_get(xx, loc, underlying_type.as_ref())
                        .into_res()
                        .map_err(|e| format!("aspect_initial_bind for self: {:?}", e))?;
                body_scope.insert(ConstKey::String("inner".to_string()), self_ib.initial_bind(self_id));
                BodyShape::SelfMethod(self_id, dt_data.data_field.clone())
            };
            let method_xx = Rc::new(ExprContext {
                vx: body_ex.clone(),
                import_state: xx.import_state.clone(),
                scope: Rc::new(Scope {
                    values: RefCell::new(body_scope),
                    parent: None,
                }),
                target: target.clone(),
            });
            let impl_result = const_eval(&method_xx, impl_expr_ast);
            if let Some(errs) = impl_result.errors {
                let iface_methods = impl_data.interface.methods.borrow();
                let mname = &iface_methods[method_impl.interface_method_index].name;
                return Err(format!("Failed to const_eval imported interface impl '{}': {:?}", mname, errs));
            }
            let impl_cv = impl_result.v.unwrap();
            let impl_cv = {
                let r = typecheck_convert(&method_xx, loc, ret_type.as_ref(), impl_cv);
                if let Some(errs) = r.errors {
                    let iface_methods = impl_data.interface.methods.borrow();
                    let mname = &iface_methods[method_impl.interface_method_index].name;
                    return Err(format!("typecheck_convert on imported interface impl '{}': {:?}", mname, errs));
                }
                r.v.unwrap()
            };
            let impl_code =
                impl_cv
                    .code
                    .cast::<CodeDynamic>()
                    .into_res()
                    .map_err(|e| format!("Interface method body must produce JS code: {:?}", e))?
                    .ok_or_else(|| "Interface method body produced no code".to_string())?
                    .clone();
            method_codes.push((method_id, body_shape, impl_code));
        }
    }
    let scope_ptr =
        xx.vx.v8_scope.get().ok_or_else(|| "install_imported_type_iface: no active v8 scope".to_string())?;
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    for (id, ext) in body_ex.externals.borrow().iter() {
        let key =
            v8::String::new(
                scope,
                &scope_entry_id_dynamic_key(*id),
            ).ok_or_else(|| "external key alloc".to_string())?;
        let v8_val: v8::Local<v8::Value> = match ext {
            External::StagedExprCell(cell) => to_v8(scope, cell.clone()).into(),
            External::StageLiftCell(cell) => to_v8(scope, cell.clone()).into(),
            External::CodeValueExternCell(cell) => to_v8(scope, cell.clone()).into(),
            External::Type(t) => rc_type_to_v8(scope, t.clone()).into(),
            External::Unique(u) => to_v8(scope, *u).into(),
        };
        scope.get_current_context().global(scope).set(scope, key.cast(), v8_val);
    }
    let mut code = JsSourceBuilder::default();
    for (method_id, shape, impl_code) in &method_codes {
        let func_name = scope_entry_id_dynamic_key(*method_id);
        let start_line = code.lines.len();
        match shape {
            BodyShape::ArgMethod(arg_id) => {
                code.push(loc, format!("function {}({}) {{", func_name, scope_entry_id_dynamic_key(*arg_id)));
            },
            BodyShape::SelfMethod(self_id, data_field) => {
                code.push(loc, format!("function {}() {{", func_name));
                code.push(
                    loc,
                    format!(
                        "let {} = this[{}];",
                        scope_entry_id_dynamic_key(*self_id),
                        serde_json::to_string(&ConstKey::String(data_field.clone()).to_str(),).unwrap(),
                    ),
                );
            },
        }
        let body_var = render_code(&mut code, impl_code);
        code.push(loc, format!("return {};", body_var));
        code.push(loc, "}".to_string());
        let compiled = code.lines[start_line..].join("\n");
        if let Some(slot) = dt_data.interface_impls.borrow().iter().find_map(|i| {
            i.method_impls.iter().find(|m| m.scope_entry_id.get() == Some(*method_id))
        }) {
            *slot.compiled_js.borrow_mut() = Some(compiled);
        }
    }
    code.push(loc, "(function() {".to_string());
    let underlying_var = render_code(&mut code, &underlying_js);
    let mut field_decls: Vec<(String, String)> = Vec::new();
    field_decls.push((ConstKey::String(dt_data.data_field.clone()).to_str(), underlying_var));
    for method in dt_data.methods.borrow().iter() {
        let id = method.scope_entry_id.get().ok_or_else(|| "Method scope_entry_id not allocated".to_string())?;
        field_decls.push((ConstKey::String(method.field_name.clone()).to_str(), scope_entry_id_dynamic_key(id)));
    }
    for impl_data in dt_data.interface_impls.borrow().iter() {
        let methods = impl_data.interface.methods.borrow();
        for m in &impl_data.method_impls {
            let id =
                m
                    .scope_entry_id
                    .get()
                    .ok_or_else(|| "Interface method impl scope_entry_id not allocated".to_string())?;
            let field_name = methods[m.interface_method_index].field_name.clone();
            field_decls.push((ConstKey::String(field_name).to_str(), scope_entry_id_dynamic_key(id)));
        }
    }
    let obj_lit =
        field_decls
            .iter()
            .map(|(k, v)| format!("{}: {}", serde_json::to_string(k).unwrap(), v))
            .collect::<Vec<_>>()
            .join(", ");
    code.push(loc, format!("return {{ {} }};", obj_lit));
    code.push(loc, "})()".to_string());
    let source = code.lines.join("\n");
    let script =
        v8::Script::compile(
            scope,
            v8::String::new(scope, &source).ok_or_else(|| "Failed to allocate script source".to_string())?,
            None,
        ).ok_or_else(|| "Failed to compile eager import script".to_string())?;
    let result = script.run(scope).ok_or_else(|| "Eager import script returned no value".to_string())?;
    let instance_obj =
        v8::Local::<v8::Object>::try_from(
            result,
        ).map_err(|_| "Eager import script did not return an object".to_string())?;
    Ok(v8::Global::new(scope, instance_obj))
}
