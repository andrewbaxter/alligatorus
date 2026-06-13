//! Re-installation of an imported data-type instance in the importing
//! compile's isolate.
//!
//! When `from_serial` encounters an instance of a data type whose impls
//! reference per-isolate externals (StagedExprCells captured by the user's
//! `stage(...)` bodies in `cast_from`/`can_cast_from`/`desc`), we can't reuse
//! the originating compile's `compiled_js` — its scope-entry names point at
//! globals that exist only in the source isolate. Instead we re-compile the
//! user's `impl_expr` against this compile's `ExprContext` (which allocates
//! fresh `Local` ids and adds the externals to this compile's
//! `ex.externals`), render the same JS the def-set pass would render, and
//! run it eagerly so the methods + their externals are live in this
//! isolate's globals before `TypeUserDefined.cast_from` dispatches.

use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            JsSourceBuilder,
            build_builtin_scope,
            build_cast_arg_record_type,
            const_eval,
            render_code,
            scope_entry_id_dynamic_key,
            to_v8,
            value::type_source_data_type::SourceDataTypeData,
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
    },
    dumpster::unsync::Gc,
    std::{
        cell::RefCell,
        rc::Rc,
    },
};

/// Re-compile each builtin-`type` interface impl, render to JS, install the
/// captured externals as v8 globals, run a script that defines the methods
/// and constructs the instance object, and return a persistent handle to
/// that object.
pub(crate) fn install_imported_type_iface(
    xx: &ExprContext,
    loc: QExprId,
    dt_data: &Gc<SourceDataTypeData>,
    underlying_js: CodeDynamic,
) -> Result<v8::Global<v8::Object>, String> {
    // Each impl body is compiled against its own fresh `EvalContext` so the
    // externals it captures (StagedExprCells, StageLiftCells) are scoped to
    // this re-install rather than mixed in with the importer's main
    // body. We collect those externals here and install them as globals
    // just before running the eager script.
    let body_ex = Rc::new(EvalContext {
        ix: xx.vx.ix.clone(),
        next_local_id: Counter::default(),
        externals: RefCell::new(Vec::new()),
        v8_scope: std::cell::Cell::new(None),
        v8_dispatcher: std::cell::Cell::new(None),
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
        let is_type_iface = impl_data.interface.builtin_kind
            == Some(crate::eval::value::type_source_interface::BuiltinInterfaceKind::Type);
        for method_impl in &impl_data.method_impls {
            let method_id = method_impl
                .scope_entry_id
                .get()
                .ok_or_else(|| "Interface method impl missing scope_entry_id".to_string())?;
            let impl_expr_value = method_impl
                .impl_expr
                .borrow()
                .clone()
                .ok_or_else(|| "Interface method impl missing impl_expr".to_string())?;
            let impl_expr_ast = must_downcast_ref::<crate::eval::expr::Expr>(impl_expr_value.as_ref());
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
            method_impl.self_scope_entry_id.set(Some(self_id));
            let mut body_scope = build_builtin_scope(xx, loc, &builtin_scope_entries)
                .map_err(|e| format!("build_builtin_scope: {:?}", e))?;
            let body_shape = if is_type_iface {
                let arg_record_type = build_cast_arg_record_type();
                let arg_ib = crate::builtin::aspect_initial_bind_get(xx, loc, arg_record_type.as_ref())
                    .into_res()
                    .map_err(|e| format!("aspect_initial_bind for arg: {:?}", e))?;
                body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(self_id));
                BodyShape::ArgMethod(self_id)
            } else {
                let self_ib = crate::builtin::aspect_initial_bind_get(xx, loc, underlying_type.as_ref())
                    .into_res()
                    .map_err(|e| format!("aspect_initial_bind for self: {:?}", e))?;
                body_scope.insert(ConstKey::String("self".to_string()), self_ib.initial_bind(self_id));
                BodyShape::SelfMethod(self_id, dt_data.data_field.clone())
            };
            let method_xx = Rc::new(ExprContext {
                vx: body_ex.clone(),
                import_state: xx.import_state.clone(),
                scope: Rc::new(Scope { values: RefCell::new(body_scope), parent: None }),
                target: target.clone(),
            });
            let impl_result = const_eval(&method_xx, impl_expr_ast);
            if let Some(errs) = impl_result.errors {
                let iface_methods = impl_data.interface.methods.borrow();
                let mname = &iface_methods[method_impl.interface_method_index].name;
                return Err(
                    format!("Failed to const_eval imported interface impl '{}': {:?}", mname, errs),
                );
            }
            let impl_cv = impl_result.v.unwrap();
            let impl_cv = {
                let r = crate::eval::typecheck_convert(&method_xx, loc, ret_type.as_ref(), impl_cv);
                if let Some(errs) = r.errors {
                    let iface_methods = impl_data.interface.methods.borrow();
                    let mname = &iface_methods[method_impl.interface_method_index].name;
                    return Err(
                        format!("typecheck_convert on imported interface impl '{}': {:?}", mname, errs),
                    );
                }
                r.v.unwrap()
            };
            let impl_code = impl_cv
                .code
                .cast::<CodeDynamic>()
                .into_res()
                .map_err(|e| format!("Interface method body must produce JS code: {:?}", e))?
                .ok_or_else(|| "Interface method body produced no code".to_string())?
                .clone();
            method_codes.push((method_id, body_shape, impl_code));
        }
    }

    // Install captured externals as v8 globals so the rendered method
    // bodies can reference them (same setup `eval()` does at end-of-stage).
    let scope_ptr = xx
        .vx
        .v8_scope
        .get()
        .ok_or_else(|| "install_imported_type_iface: no active v8 scope".to_string())?;
    // SAFETY: pointer set by `eval()` for its full duration; the dispatcher
    // already re-borrows the same way (see `StatelessV8CastDispatcher`).
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> =
        unsafe { &mut *(scope_ptr.as_ptr() as *mut _) };
    for (id, ext) in body_ex.externals.borrow().iter() {
        let key = v8::String::new(scope, &scope_entry_id_dynamic_key(*id))
            .ok_or_else(|| "external key alloc".to_string())?;
        let v8_val: v8::Local<v8::Value> = match ext {
            External::StagedExprCell(cell) => to_v8(scope, cell.clone()).into(),
            External::StageLiftCell(cell) => to_v8(scope, cell.clone()).into(),
            External::Type(t) => crate::eval::rc_type_to_v8(scope, t.clone()).into(),
            External::Unique(u) => to_v8(scope, u.clone()).into(),
            External::IsolateBundle(b) => v8::Local::new(scope, &b.instance).into(),
        };
        scope.get_current_context().global(scope).set(scope, key.cast(), v8_val);
    }

    // Render: method function definitions + IIFE that builds and returns
    // the instance object. The IIFE wraps `let` declarations so they don't
    // collide with the importer's main stage script.
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
                        serde_json::to_string(
                            &crate::langhelp::ConstKey::String(data_field.clone()).to_str(),
                        ).unwrap(),
                    ),
                );
            },
        }
        let body_var = render_code(&mut code, impl_code);
        code.push(loc, format!("return {};", body_var));
        code.push(loc, "}".to_string());
        // Stash the compiled JS on the method_impl so the end-of-stage pass
        // skips re-emitting it (matches what `eval()`'s `Fresh` body branch
        // does for inline-compiled impls).
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
    field_decls
        .push((crate::langhelp::ConstKey::String(dt_data.data_field.clone()).to_str(), underlying_var));
    for method in dt_data.methods.borrow().iter() {
        let id = method
            .scope_entry_id
            .get()
            .ok_or_else(|| "Method scope_entry_id not allocated".to_string())?;
        field_decls.push((
            crate::langhelp::ConstKey::String(method.field_name.clone()).to_str(),
            scope_entry_id_dynamic_key(id),
        ));
    }
    for impl_data in dt_data.interface_impls.borrow().iter() {
        let methods = impl_data.interface.methods.borrow();
        for m in &impl_data.method_impls {
            let id = m
                .scope_entry_id
                .get()
                .ok_or_else(|| "Interface method impl scope_entry_id not allocated".to_string())?;
            let field_name = methods[m.interface_method_index].field_name.clone();
            field_decls.push((
                crate::langhelp::ConstKey::String(field_name).to_str(),
                scope_entry_id_dynamic_key(id),
            ));
        }
    }
    let obj_lit = field_decls
        .iter()
        .map(|(k, v)| format!("{}: {}", serde_json::to_string(k).unwrap(), v))
        .collect::<Vec<_>>()
        .join(", ");
    code.push(loc, format!("return {{ {} }};", obj_lit));
    code.push(loc, "})()".to_string());

    let source = code.lines.join("\n");
    let script = v8::Script::compile(
        scope,
        v8::String::new(scope, &source).ok_or_else(|| "Failed to allocate script source".to_string())?,
        None,
    ).ok_or_else(|| "Failed to compile eager import script".to_string())?;
    let result = script
        .run(scope)
        .ok_or_else(|| "Eager import script returned no value".to_string())?;
    let instance_obj = v8::Local::<v8::Object>::try_from(result)
        .map_err(|_| "Eager import script did not return an object".to_string())?;
    Ok(v8::Global::new(scope, instance_obj))
}

