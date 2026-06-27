use {
    crate::{
        eval::{
            DynamicSourceBuilder,
            definition_set::DefinitionSet,
            rc_type_to_dynamic_ext,
            binding_id_dynamic_key,
            to_dynamic_ext,
        },
        langhelp::{
            ExprContext,
            External,
            QExprId,
            BindingId,
        },
    },
    dumpster::unsync::Gc,
};

pub(crate) fn install_def_set_to_dynamic(xx: &ExprContext, loc: QExprId, set: &Gc<DefinitionSet>) -> Result<(), String> {
    if !xx.vx.ix.installed_def_sets.borrow_mut().insert(set.def_set_id) {
        return Ok(());
    }
    for dep in set.dependencies.borrow().values() {
        install_def_set_to_dynamic(xx, loc, dep)?;
    }
    let Some(scope_ptr) = xx.vx.dynamic_scope.get() else {
        return Ok(());
    };
    let scope: &mut v8::PinnedRef<'_, v8::HandleScope<'_, v8::Context>> = unsafe {
        &mut *(scope_ptr.as_ptr() as *mut _)
    };
    let mut installed_externals: Vec<(String, External)> = Vec::new();
    let mut collect_externals = |func_name: &str, captured: &[(BindingId, External)]| {
        for (id, value) in captured {
            let local_key = binding_id_dynamic_key(*id);
            installed_externals.push((format!("{}_{}", func_name, local_key), value.clone()));
        }
    };
    for func_data in set.functions.borrow().iter() {
        let func_name =
            binding_id_dynamic_key(
                func_data.binding_id.get().ok_or_else(|| "func missing binding_id".to_string())?,
            );
        collect_externals(&func_name, &func_data.captured_externals.borrow());
    }
    for dt in set.objects.borrow().iter() {
        for method in dt.methods.borrow().iter() {
            let func_name =
                binding_id_dynamic_key(
                    method.binding_id.get().ok_or_else(|| "method missing binding_id".to_string())?,
                );
            collect_externals(&func_name, &method.captured_externals.borrow());
        }
        for impl_data in dt.interface_impls.borrow().iter() {
            for m in &impl_data.method_impls {
                let func_name =
                    binding_id_dynamic_key(
                        m.binding_id.get().ok_or_else(|| "iface method missing binding_id".to_string())?,
                    );
                collect_externals(&func_name, &m.captured_externals.borrow());
            }
        }
    }
    for (key_str, value) in &installed_externals {
        let key =
            v8::String::new(scope, key_str).ok_or_else(|| "install_def_set_to_dynamic: key alloc".to_string())?.cast();
        let dynamic_val: v8::Local<v8::Value> = match value {
            External::StagedExprCell(cell) => to_dynamic_ext(scope, cell.clone()).into(),
            External::StageLiftCell(cell) => to_dynamic_ext(scope, cell.clone()).into(),
            External::CodeValueExternCell(cell) => to_dynamic_ext(scope, cell.clone()).into(),
            External::Type(t) => rc_type_to_dynamic_ext(scope, t.clone()).into(),
            External::Unique(u) => to_dynamic_ext(scope, *u).into(),
        };
        scope.get_current_context().global(scope).set(scope, key, dynamic_val);
    }
    let mut code = DynamicSourceBuilder::default();
    for func_data in set.functions.borrow().iter() {
        if let Some(dynamic_code) = func_data.compiled_source.borrow().clone() {
            for line in dynamic_code.lines() {
                code.push(loc, line.to_string());
            }
        }
    }
    for dt in set.objects.borrow().iter() {
        for method in dt.methods.borrow().iter() {
            if let Some(dynamic_code) = method.compiled_source.borrow().clone() {
                for line in dynamic_code.lines() {
                    code.push(loc, line.to_string());
                }
            }
        }
        for impl_data in dt.interface_impls.borrow().iter() {
            for m in &impl_data.method_impls {
                if let Some(dynamic_code) = m.compiled_source.borrow().clone() {
                    for line in dynamic_code.lines() {
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
            v8::String::new(scope, &source).ok_or_else(|| "install_def_set_to_dynamic: source alloc".to_string())?,
            None,
        ).ok_or_else(|| "install_def_set_to_dynamic: compile failed".to_string())?;
    script.run(scope).ok_or_else(|| "install_def_set_to_dynamic: run failed".to_string())?;
    Ok(())
}

