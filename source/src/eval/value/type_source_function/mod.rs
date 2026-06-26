pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            DynamicSourceBuilder,
            Scope,
            build_builtin_scope,
            const_eval,
            definition_set::DefinitionSet,
            expr::Expr,
            render_code,
            scope_entry_id_dynamic_key,
            typecheck_convert,
        },
        invariant::must_downcast_ref,
        langhelp::{
            ConstKey,
            Counter,
            EvalContext,
            ExprContext,
            ExprErr,
            External,
            QExprId,
            ScopeEntryId,
            Type,
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

pub(crate) struct SourceFunctionData {
    pub arg_type: RefCell<Option<Rc<dyn Type>>>,
    pub artifact_id: RefCell<Option<crate::artifact::ArtifactIdSource>>,
    pub captured_externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub compiled_source: RefCell<Option<String>>,
    pub description: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub name: Option<String>,
    pub ret_type: RefCell<Option<Rc<dyn Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub set: RefCell<Option<Gc<DefinitionSet>>>,
}

impl SourceFunctionData {
    pub fn finish(&self, xx: &ExprContext, root_loc: QExprId) -> Result<(), Vec<ExprErr>> {
        if self.compiled_source.borrow().is_some() {
            return Ok(());
        }
        let name = self.name.as_deref().unwrap_or("(unnamed)").to_string();
        let impl_expr_value = self.impl_expr.borrow_mut().take().ok_or_else(|| vec![ExprErr {
            loc: root_loc,
            errors: vec![
                crate::langhelp::ExprSubErr::Opaque(
                    format!("Definition set function '{}' is missing an implementation", name),
                ),
            ],
        }])?;
        let impl_expr_ast = must_downcast_ref::<Expr>(impl_expr_value.as_ref());
        let arg_type = self.arg_type.borrow().clone().ok_or_else(|| vec![ExprErr {
            loc: root_loc,
            errors: vec![
                crate::langhelp::ExprSubErr::Opaque(
                    format!("Definition set function '{}' is missing an arg type", name),
                ),
            ],
        }])?;
        let ret_type = self.ret_type.borrow().clone().ok_or_else(|| vec![ExprErr {
            loc: root_loc,
            errors: vec![
                crate::langhelp::ExprSubErr::Opaque(
                    format!("Definition set function '{}' is missing a return type", name),
                ),
            ],
        }])?;
        let scope_id = self.scope_entry_id.get().unwrap();
        let builtin_scope_entries = xx.vx.ix.builtin_scope_entries.borrow().clone();
        let body_ex = Rc::new(EvalContext {
            ix: xx.vx.ix.clone(),
            next_local_id: Counter::default(),
            externals: RefCell::new(Vec::new()),
            dynamic_scope: Cell::new(None),
            definition_sets: RefCell::new(std::collections::HashMap::new()),
        });
        let arg_id = ScopeEntryId::Local(body_ex.next_local_id.next());
        let mut body_scope = build_builtin_scope(xx, root_loc, &builtin_scope_entries)?;
        if let Some(arg_ib) = aspect_initial_bind_get_maybe(xx, arg_type.as_ref()) {
            body_scope.insert(ConstKey::String("arg".to_string()), arg_ib.initial_bind(arg_id));
        }
        let func_xx = Rc::new(ExprContext {
            vx: body_ex,
            import_state: xx.import_state.clone(),
            scope: Rc::new(Scope {
                values: RefCell::new(body_scope),
                parent: None,
            }),
            target: xx.target.clone(),
        });
        let impl_result = const_eval(&func_xx, impl_expr_ast);
        if let Some(errs) = impl_result.errors {
            return Err(errs);
        }
        let impl_cv = impl_result.v.unwrap();
        let r = typecheck_convert(&func_xx, root_loc, ret_type.as_ref(), impl_cv);
        if let Some(errs) = r.errors {
            return Err(errs);
        }
        let impl_cv = r.v.unwrap();
        let impl_code = impl_cv.code.cast::<CodeDynamic>().into_res()?.unwrap().clone();
        let captured = func_xx.vx.externals.borrow().clone();
        let mut code1 = DynamicSourceBuilder::default();
        let func_name = scope_entry_id_dynamic_key(scope_id);
        code1.push(root_loc, format!("var {};", func_name));
        code1.push(root_loc, "{".to_string());
        for (id, _) in &captured {
            let local_key = scope_entry_id_dynamic_key(*id);
            code1.push(root_loc, format!("let {} = globalThis[\"{}_{}\"];", local_key, func_name, local_key));
        }
        code1.push(root_loc, format!("{} = function({}) {{", func_name, scope_entry_id_dynamic_key(arg_id)));
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

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceFunctionData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        let set = self.set.try_borrow().map_err(|_| ())?;
        if let Some(s) = set.as_ref() {
            visitor.visit_unsync(s);
        }
        Ok(())
    }
}

impl crate::artifact::ExporterAny for crate::artifact::ExporterDataAny<dumpster::unsync::Gc<SourceFunctionData>> {
    fn as_type(&self) -> crate::langhelp::EResult<Rc<dyn Type>> {
        crate::langhelp::EResult::ok(
            Rc::new(
                crate::eval::value::type_source_function::r#const::TypeSourceFunctionConst { data: self.0.clone() },
            ),
        )
    }

    fn as_value(&self) -> crate::langhelp::EResult<Rc<dyn crate::langhelp::Value>> {
        let inner: Rc<dyn Type> =
            Rc::new(
                crate::eval::value::type_source_function::r#const::TypeSourceFunctionConst { data: self.0.clone() },
            );
        crate::langhelp::EResult::ok(Rc::new(crate::eval::value::type_any::r#const::ValueAnyConst { inner }))
    }
}
