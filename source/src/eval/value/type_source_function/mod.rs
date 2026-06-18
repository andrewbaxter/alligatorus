pub mod artifact;
pub mod dynamic;
pub mod r#const;

use {
    crate::{
        code_dynamic::CodeDynamic,
        eval::{
            JsSourceBuilder,
            Scope,
            build_builtin_scope,
            const_eval,
            definition_set::{
                DefinitionSet,
                serialize_captured_externals,
            },
            expr::Expr,
            render_code,
            scope_entry_id_dynamic_key,
            typecheck_convert,
            value::{
                type_source_data_type::artifact::ArtifactDataDefSetFunction,
                type_source_data_type_constructor::dynamic::serialize_type_ref,
            },
        },
        invariant::must_downcast_ref,
        langhelp::{
            ArtifactId,
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
            produce_independent_artifact,
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
    pub artifact_id: RefCell<Option<ArtifactId>>,
    pub captured_externals: RefCell<Vec<(ScopeEntryId, External)>>,
    pub compiled_js: RefCell<Option<String>>,
    pub description: RefCell<Option<String>>,
    pub impl_expr: RefCell<Option<Rc<dyn Value>>>,
    pub name: Option<String>,
    pub ret_type: RefCell<Option<Rc<dyn Type>>>,
    pub scope_entry_id: Cell<Option<ScopeEntryId>>,
    pub set: Gc<DefinitionSet>,
}

impl SourceFunctionData {
    pub fn finish(&self, xx: &ExprContext, root_loc: QExprId) -> Result<(), Vec<ExprErr>> {
        if self.compiled_js.borrow().is_some() {
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
            v8_scope: Cell::new(None),
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
        let mut code1 = JsSourceBuilder::default();
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
        *self.compiled_js.borrow_mut() = Some(compiled);
        Ok(())
    }

    pub fn serialize(&self, xx: &ExprContext, loc: QExprId, def_set_artifact_id: ArtifactId) -> ArtifactId {
        use crate::{
            eval::value::type_null::dynamic::TypeNullDynamic,
            langhelp::Type as _,
        };

        produce_independent_artifact(xx.vx.ix.gx.artifacts.as_ref(), &self.artifact_id, || {
            let arg_type = self.arg_type.borrow().clone().unwrap_or_else(|| Rc::new(TypeNullDynamic));
            let ret_type = self.ret_type.borrow().clone().unwrap_or_else(|| Rc::new(TypeNullDynamic));
            let compiled_js = self.compiled_js.borrow().clone().unwrap_or_default();
            let entry_id = match self.scope_entry_id.get().expect("Function missing scope_entry_id") {
                ScopeEntryId::Def(_, id) => id,
                other => panic!("Function has non-Def scope_entry_id: {:?}", other),
            };
            let captured_externals = serialize_captured_externals(xx, loc, &self.captured_externals.borrow());
            let serial = ArtifactDataDefSetFunction {
                name: self.name.clone(),
                entry_id,
                arg_type: serialize_type_ref(xx, loc, arg_type.as_ref()),
                ret_type: serialize_type_ref(xx, loc, ret_type.as_ref()),
                compiled_js,
                def_set: def_set_artifact_id,
                captured_externals,
            };

            use crate::eval::value::type_source_function::dynamic::TypeTypeSourceFunctionDynamic;

            TypeTypeSourceFunctionDynamic::build_artifact(
                serde_json::to_value(&serial).expect("Failed to serialize function"),
            )
        })
    }
}

unsafe impl<V: dumpster::Visitor> dumpster::TraceWith<V> for SourceFunctionData {
    fn accept(&self, visitor: &mut V) -> Result<(), ()> {
        visitor.visit_unsync(&self.set);
        Ok(())
    }
}
