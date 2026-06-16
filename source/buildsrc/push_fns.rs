use {
    crate::buildsrc::util::arg_dynamic_type_expr_str,
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, scope_ids: &mut HashSet<usize>) {
    let mut dw_fn_call_exprs: Vec<TokenStream> = Vec::new();
    let builtin_scope_ids = scope_ids;

    struct PushFnSpec {
        args: Vec<(&'static str, &'static str)>,
        body: TokenStream,
        builtin_scope_id: usize,
        name: &'static str,
        ret: &'static str,
    }

    for s in &vec![
        //. .
        PushFnSpec {
            name: "log",
            builtin_scope_id: 5001,
            args: vec![("message", "String")],
            ret: "()",
            body: quote!{
                let timestamp_ms =
                    std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as u64;
                scope.get_slot::<V8LogStore>().unwrap().logs.borrow_mut().push(LogEntry {
                    timestamp_ms,
                    message,
                });
            },
        },
        PushFnSpec {
            name: "define",
            builtin_scope_id: 5002,
            args: vec![],
            ret: "DefinitionSetBuilder",
            body: quote!{
                let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                let def_set_id = DefSetId(ex_slot.ex.ix.def_set_counter.next());
                let import_id = ex_slot.ex.ix.import_state;
                let source_id = ex_slot.ex.ix.source_id;
                let inner = Gc::new(DefinitionSet::new(import_id, def_set_id, source_id));
                ex_slot.ex.ix.definition_sets.borrow_mut().push(inner.clone());
                DefinitionSetBuilder { inner }
            },
        },
        PushFnSpec {
            name: "str_to_json",
            builtin_scope_id: 5003,
            args: vec![("s", "String")],
            ret: "String",
            body: quote!{
                serde_json::to_string(&s).unwrap()
            },
        },
        PushFnSpec {
            name: "constkey_to_str",
            builtin_scope_id: 5004,
            args: vec![("key", "crate::langhelp::ConstKey")],
            ret: "String",
            body: quote!{
                key.to_str()
            },
        },
        PushFnSpec {
            name: "new_code_value",
            builtin_scope_id: 5005,
            args: vec![("code", "crate::langhelp::Code"), ("value", "Rc<dyn crate::langhelp::Value>")],
            ret: "crate::eval::code_value_extern::CodeValueExtern",
            body: quote!{
                let cv = crate::langhelp::CodeValue {
                    code,
                    value
                };
                crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
            },
        },
        PushFnSpec {
            name: "gen_binding_name",
            builtin_scope_id: 5006,
            args: vec![("_xx", "crate::eval::code_value_extern::CodeValueExtern")],
            ret: "String",
            body: quote!{
                let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                let id = ex_slot.ex.next_local_id.next();
                format!("a{}", id)
            },
        },
        PushFnSpec {
            name: "const_eval",
            builtin_scope_id: 5008,
            args: vec![
                ("xx", "crate::eval::code_value_extern::ExprContextPush"),
                ("staged", "crate::eval::push::StagedExpr"),
            ],
            ret: "crate::eval::code_value_extern::CodeValueExtern",
            body: quote!{
                let expr_value = staged.0;
                let expr_ast: Expr =
                    Rc::try_unwrap(crate::langhelp::downcast_value::<Expr>(expr_value))
                        .ok()
                        .expect("const_eval: staged Expr Rc not unique");
                let res = const_eval(&xx.0, &expr_ast);
                let cv = res.v.unwrap_or_else(|| {
                    panic!("const_eval: const_eval produced no value; errors: {:?}", res.errors)
                });
                crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
            },
        },
        PushFnSpec {
            name: "new_expr_context",
            builtin_scope_id: 5009,
            args: vec![
                ("target_arg", "Rc<dyn crate::r#gen::Target>"),
                ("scope_arg", "std::collections::HashMap<ConstKey, Rc<dyn crate::r#gen::ScopeEntry>>"),
            ],
            ret: "crate::eval::code_value_extern::ExprContextPush",
            body: quote!{
                let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                let parent_ex = ex_slot.ex.clone();
                let mut scope_values: HashMap<ConstKey, Box<dyn crate::r#gen::ScopeEntry>> = HashMap::new();
                for (k, v) in scope_arg {
                    scope_values.insert(k, Box::new(RcScopeEntryWrap(v)));
                }
                let new_scope = Rc::new(Scope {
                    values: RefCell::new(scope_values),
                    parent: None,
                });
                let xx = Rc::new(ExprContext {
                    vx: parent_ex.clone(),
                    import_state: Rc::new(Cell::new(parent_ex.ix.import_state)),
                    scope: new_scope,
                    target: target_arg,
                });
                crate::eval::code_value_extern::ExprContextPush(xx)
            },
        },
        PushFnSpec {
            name: "list",
            builtin_scope_id: 5010,
            args: vec![("element_type", "crate::eval::push::AnyType")],
            ret: "crate::eval::value::type_list::dynamic::TypeListDynamic",
            body: quote!{
                crate::eval::value::type_list::dynamic::TypeListDynamic { element_type: element_type.0 }
            },
        },
        PushFnSpec {
            name: "map",
            builtin_scope_id: 5011,
            args: vec![
                ("key_type", "crate::eval::push::AnyType"),
                ("value_type", "crate::eval::push::AnyType"),
            ],
            ret: "crate::eval::value::type_map::dynamic::TypeMapDynamic",
            body: quote!{
                crate::eval::value::type_map::dynamic::TypeMapDynamic {
                    key_type: key_type.0,
                    value_type: value_type.0,
                }
            },
        },
        PushFnSpec {
            name: "option",
            builtin_scope_id: 5012,
            args: vec![("element_type", "crate::eval::push::AnyType")],
            ret: "crate::eval::value::type_option::dynamic::TypeOptionDynamic",
            body: quote!{
                crate::eval::value::type_option::dynamic::TypeOptionDynamic { element_type: element_type.0 }
            },
        },
    ] {
        let fn_name = format_ident!("gen_fn_{}", s.name);
        let name_str = s.name;
        let builtin_scope_id = s.builtin_scope_id;
        assert!(builtin_scope_ids.insert(builtin_scope_id));
        let arg_dynamic_type = arg_dynamic_type_expr_str(&s.args);
        let arg_decls: Vec<TokenStream> = s.args.iter().map(|(n, ty)| {
            let n = format_ident!("{}", n);
            let ty: TokenStream = ty.parse().unwrap();
            quote!{
                let #n: #ty = <#ty as crate:: eval:: push:: Push >:: from_v8(
                    scope,
                    _jw_args.get(_jw_arg_i),
                );
                _jw_arg_i += 1;
            }
        }).collect();
        let ret_ty: TokenStream = s.ret.parse().unwrap();
        let body = &s.body;
        out.extend(quote!{
            pub(
                crate
            ) fn #fn_name <'s >(
                dynamic_scope: & mut v8:: PinnedRef <'s,
                v8:: HandleScope <'_ >>,
            ) ->(
                &'static str,
                crate::langhelp::ScopeEntryId,
                std:: rc:: Rc < dyn crate:: langhelp:: Type >,
                v8:: Local <'s,
                v8:: Value >,
            ) {
                fn _jw_cb <'s >(
                    scope: & mut v8:: PinnedRef <'s,
                    v8:: HandleScope <'_ >>,
                    _jw_args: v8:: FunctionCallbackArguments <'s >,
                    mut _jw_rv: v8:: ReturnValue <'s >,
                ) {
                    #[allow(unused_mut, unused_variables)]
                    let mut _jw_arg_i = 0i32;
                    #(#arg_decls) * let _jw_result: #ret_ty = {
                        #body
                    };
                    _jw_rv.set(<#ret_ty as crate:: eval:: push:: Push >:: into_v8(_jw_result, scope),);
                }
                (
                    #name_str,
                    crate:: langhelp:: ScopeEntryId:: Builtin(#builtin_scope_id),
                    std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                        arg: #arg_dynamic_type,
                        ret: <#ret_ty as crate:: eval:: push:: Push >:: dynamic_type(),
                    },) as std:: rc:: Rc < dyn crate:: langhelp:: Type >,
                    v8::Function::builder(_jw_cb).build(dynamic_scope).unwrap().cast::<v8::Value>(),
                )
            }
        });
        let fn_name = format_ident!("gen_fn_{}", s.name);
        dw_fn_call_exprs.push(quote!{
            #fn_name(dynamic_scope)
        });
    }
    out.extend(quote!{
        pub(
            crate
        ) fn gen_register_builtin_fns <'s >(
            xx: & crate:: langhelp:: ExprContext,
            root_loc: crate:: langhelp:: QExprId,
            register_v8: bool,
            dynamic_scope: & mut v8:: PinnedRef <'s,
            v8:: HandleScope <'_ >>,
            scope: & crate:: langhelp:: Scope,
            builtin_scope_entries: & mut Vec <(
                String,
                crate::langhelp::ScopeEntryId,
                std:: rc:: Rc < dyn crate:: langhelp:: Type >,
            ) >,
        ) -> Result <(),
        Vec < crate:: langhelp:: ExprErr >> {
            for(key, dynamic_key, type_, dynamic_value) in[#(#dw_fn_call_exprs), *] {
                if register_v8 {
                    dynamic_scope
                        .get_current_context()
                        .global(dynamic_scope)
                        .set(
                            dynamic_scope,
                            v8::String::new(dynamic_scope, &crate::eval::scope_entry_id_dynamic_key(dynamic_key),)
                                .unwrap()
                                .cast(),
                            dynamic_value,
                        );
                }
                let ib = crate::r#gen::aspect_initial_bind_get(xx, root_loc, type_.as_ref()).into_res()?;
                builtin_scope_entries.push((key.to_string(), dynamic_key, type_));
                scope
                    .values
                    .borrow_mut()
                    .insert(crate::langhelp::ConstKey::String(key.to_string()), ib.initial_bind(dynamic_key),);
            }
            Ok(())
        }
    });
}
