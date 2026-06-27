use {
    crate::buildsrc::util::arg_dynamic_type_expr_str,
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub const PUSHFN_LOG_NAME: &str = "log";
pub const PUSHFN_DEFINE_NAME: &str = "define";
pub const PUSHFN_STR_TO_JSON_NAME: &str = "str_to_json";
pub const PUSHFN_CONSTKEY_TO_STR_NAME: &str = "constkey_to_str";
pub const PUSHFN_CODEVALUE_NAME: &str = "codevalue";
pub const PUSHFN_GEN_BINDING_NAME_NAME: &str = "gen_binding_name";
pub const PUSHFN_CONST_EVAL_NAME: &str = "const_eval";
pub const PUSHFN_NEW_EXPR_CONTEXT_NAME: &str = "new_expr_context";
pub const PUSHFN_LIST_NAME: &str = "list";
pub const PUSHFN_MAP_NAME: &str = "map";
pub const PUSHFN_OPTION_NAME: &str = "option";

pub fn build(out: &mut TokenStream, scope_ids: &mut HashSet<usize>) {
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
            name: PUSHFN_LOG_NAME,
            builtin_scope_id: 5001,
            args: vec![("message", "String")],
            ret: "()",
            body: quote!{
                let timestamp_ms =
                    std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as u64;
                scope.get_slot::<DynamicLogStore>().unwrap().logs.borrow_mut().push(LogEntry {
                    timestamp_ms,
                    message,
                });
            },
        },
        PushFnSpec {
            name: PUSHFN_DEFINE_NAME,
            builtin_scope_id: 5002,
            args: vec![],
            ret: "DefinitionSetBuilder",
            body: quote!{
                let ex_slot = scope.get_slot::<DynamicEvalContextSlot>().unwrap();
                let compile_id = crate::langhelp::CompileId(ex_slot.ex.ix.import_state, ex_slot.ex.ix.source_id);
                let def_set_id = DefSetId(compile_id, ex_slot.ex.ix.def_set_counter.next());
                let inner = Gc::new(DefinitionSet::new(def_set_id));
                ex_slot.ex.definition_sets.borrow_mut().insert(def_set_id, inner.clone());
                DefinitionSetBuilder { inner }
            },
        },
        PushFnSpec {
            name: PUSHFN_STR_TO_JSON_NAME,
            builtin_scope_id: 5003,
            args: vec![("s", "String")],
            ret: "String",
            body: quote!{
                serde_json::to_string(&s).unwrap()
            },
        },
        PushFnSpec {
            name: PUSHFN_CONSTKEY_TO_STR_NAME,
            builtin_scope_id: 5004,
            args: vec![("key", "crate::langhelp::ConstKey")],
            ret: "String",
            body: quote!{
                key.to_str()
            },
        },
        PushFnSpec {
            name: PUSHFN_CODEVALUE_NAME,
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
            name: PUSHFN_GEN_BINDING_NAME_NAME,
            builtin_scope_id: 5006,
            args: vec![("_xx", "crate::eval::code_value_extern::CodeValueExtern")],
            ret: "String",
            body: quote!{
                let ex_slot = scope.get_slot::<DynamicEvalContextSlot>().unwrap();
                let id = ex_slot.ex.next_local_id.next();
                format!("a{}", id)
            },
        },
        PushFnSpec {
            name: PUSHFN_CONST_EVAL_NAME,
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
            name: PUSHFN_NEW_EXPR_CONTEXT_NAME,
            builtin_scope_id: 5009,
            args: vec![
                ("target_arg", "Rc<dyn crate::r#gen::Target>"),
                ("scope_arg", "std::collections::HashMap<ConstKey, Rc<dyn crate::r#gen::Binding>>"),
            ],
            ret: "crate::eval::code_value_extern::ExprContextPush",
            body: quote!{
                let ex_slot = scope.get_slot::<DynamicEvalContextSlot>().unwrap();
                let parent_ex = ex_slot.ex.clone();
                let mut scope_values: HashMap<ConstKey, Box<dyn crate::r#gen::Binding>> = HashMap::new();
                for (k, v) in scope_arg {
                    scope_values.insert(k, Box::new(RcBindingWrap(v)));
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
            name: PUSHFN_LIST_NAME,
            builtin_scope_id: 5010,
            args: vec![("element_type", "crate::eval::push::AnyType")],
            ret: "crate::eval::value::type_list::dynamic::TypeListDynamic",
            body: quote!{
                crate::eval::value::type_list::dynamic::TypeListDynamic { element_type: element_type.0 }
            },
        },
        PushFnSpec {
            name: PUSHFN_MAP_NAME,
            builtin_scope_id: 5011,
            args: vec![("key_type", "crate::eval::push::AnyType"), ("value_type", "crate::eval::push::AnyType")],
            ret: "crate::eval::value::type_map::dynamic::TypeMapDynamic",
            body: quote!{
                crate::eval::value::type_map::dynamic::TypeMapDynamic {
                    key_type: key_type.0,
                    value_type: value_type.0,
                }
            },
        },
        PushFnSpec {
            name: PUSHFN_OPTION_NAME,
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
                let #n: #ty = <#ty as crate:: eval:: push:: Push >:: from_dynamic(scope, _jw_args.get(_jw_arg_i),);
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
                crate::langhelp::BindingId,
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
                    _jw_rv.set(<#ret_ty as crate:: eval:: push:: Push >:: into_dynamic(_jw_result, scope),);
                }
                (
                    #name_str,
                    crate:: langhelp:: BindingId:: Builtin(#builtin_scope_id),
                    std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                        arg: #arg_dynamic_type,
                        ret: <#ret_ty as crate:: eval:: push:: Push >:: dynamic_type(),
                    },) as std:: rc:: Rc < dyn crate:: langhelp:: Type >,
                    v8::Function::builder(_jw_cb).build(dynamic_scope).unwrap().cast::<v8::Value>(),
                )
            }
        });
    }
}
