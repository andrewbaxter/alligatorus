use {
    crate::buildsrc::util::{
        InterfaceSpec,
        Method,
        MethodArgs,
        TypeRef,
        arg_dynamic_type_expr_str,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

const ASPECT_COMPILE_SCOPE_ID: usize = 7023;
const CAST_VALUE_COMPILE_SCOPE_ID: usize = 7024;
const PANIC_COMPILE_SCOPE_ID: usize = 7026;
const THROW_COMPILE_SCOPE_ID: usize = 7025;

fn any_const(type_tokens: TokenStream) -> TokenStream {
    quote!{
        crate:: eval:: builtin_tree:: BuiltinLeaf:: AnyConst {
            type_: std:: rc:: Rc:: new(#type_tokens) as std:: rc:: Rc < dyn crate:: langhelp:: Type >,
        }
    }
}

fn aspect_id(
    out: &mut TokenStream,
    scope_ids: &mut HashSet<usize>,
    builtin_scope_id: usize,
    aspect_id_fn: TokenStream,
) -> TokenStream {
    assert!(scope_ids.insert(builtin_scope_id));
    let mod_name = format_ident!("builtin_b{}", builtin_scope_id);
    let fn_name = format_ident!("b{}_leaf", builtin_scope_id);
    let prelude = submodule_prelude();
    out.extend(quote!{
        pub(crate) mod #mod_name {
            #prelude pub(
                crate
            ) fn #fn_name <'s >(
                dynamic_scope: & mut v8:: PinnedRef <'s,
                v8:: HandleScope <'_ >>,
            ) -> crate:: eval:: builtin_tree:: BuiltinLeaf <'s > {
                let unique = crate:: r#gen::#aspect_id_fn();
                let unique_dynamic = crate::eval::to_dynamic_ext(dynamic_scope, unique.clone());
                crate:: eval:: builtin_tree:: BuiltinLeaf:: AspectId {
                    dynamic_key: BindingId:: Builtin(#builtin_scope_id),
                    unique: unique,
                    dynamic_value: unique_dynamic.into(),
                }
            }
        }
        pub(crate) use #mod_name::*;
    });
    quote!{
        #fn_name(dynamic_scope)
    }
}

fn branch(entries: Vec<(&'static str, TokenStream)>) -> TokenStream {
    let items: Vec<TokenStream> = entries.into_iter().map(|(k, v)| quote!{
        (#k, #v)
    }).collect();
    quote!{
        crate::eval::builtin_tree::BuiltinNode::Branch(vec![#(#items), *])
    }
}

fn constructor(
    out: &mut TokenStream,
    scope_ids: &mut HashSet<usize>,
    builtin_scope_id: usize,
    args: &[(&'static str, &'static str)],
    ret: &'static str,
    body: TokenStream,
    type_tokens: TokenStream,
) -> TokenStream {
    assert!(scope_ids.insert(builtin_scope_id));
    let arg_dynamic_type = arg_dynamic_type_expr_str(args);
    let arg_decls: Vec<TokenStream> = args.iter().map(|(n, ty)| {
        let n = format_ident!("{}", n);
        let ty: TokenStream = ty.parse().unwrap();
        quote!{
            let #n: #ty = <#ty as crate:: eval:: push:: Push >:: from_dynamic(scope, _jw_args.get(_jw_arg_i));
            _jw_arg_i += 1;
        }
    }).collect();
    let ret_ty: TokenStream = ret.parse().unwrap();
    let mod_name = format_ident!("builtin_b{}", builtin_scope_id);
    let fn_name = format_ident!("b{}_leaf", builtin_scope_id);
    let prelude = submodule_prelude();
    out.extend(quote!{
        pub(crate) mod #mod_name {
            #prelude fn _jw_cb <'s >(
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
                _jw_rv.set(<#ret_ty as crate:: eval:: push:: Push >:: into_dynamic(_jw_result, scope));
            }
            pub(
                crate
            ) fn #fn_name <'s >(
                dynamic_scope: & mut v8:: PinnedRef <'s,
                v8:: HandleScope <'_ >>,
            ) -> crate:: eval:: builtin_tree:: BuiltinLeaf <'s > {
                let _ = #arg_dynamic_type;
                crate:: eval:: builtin_tree:: BuiltinLeaf:: Constructor {
                    dynamic_key: crate:: langhelp:: BindingId:: Builtin(#builtin_scope_id),
                    type_: std:: rc:: Rc:: new(#type_tokens) as std:: rc:: Rc < dyn crate:: langhelp:: Type >,
                    dynamic_value: v8:: Function:: builder(_jw_cb).build(dynamic_scope).unwrap().cast::< v8:: Value >(),
                }
            }
        }
        pub(crate) use #mod_name::*;
    });
    quote!{
        #fn_name(dynamic_scope)
    }
}

pub fn emit_gen_builtin_tree(
    out: &mut TokenStream,
    scope_ids: &mut HashSet<usize>,
    extra_iface_specs: &mut Vec<InterfaceSpec>,
) {
    assert!(scope_ids.insert(ASPECT_COMPILE_SCOPE_ID));
    assert!(scope_ids.insert(CAST_VALUE_COMPILE_SCOPE_ID));
    assert!(scope_ids.insert(THROW_COMPILE_SCOPE_ID));
    assert!(scope_ids.insert(PANIC_COMPILE_SCOPE_ID));
    let any = || TypeRef::unit(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic));
    let cve = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic));
    let iface_aspect_function = InterfaceSpec {
        name: "aspect_function",
        builtin_unique_id: 1016,
        scope_id: 3003,
        field_prefix: "af",
        kind: "AspectFunction",
        description: None,
        register_in_scope: true,
        rust_trait_name: None,
        methods: vec![Method {
            name: "call",
            ret_type: cve(),
            args: MethodArgs::Source(vec![("inner", cve()), ("argument", cve())]),
        }],
    };
    let iface_aspect_field = InterfaceSpec {
        name: "aspect_field",
        builtin_unique_id: 1030,
        scope_id: 3004,
        field_prefix: "afd",
        kind: "AspectField",
        description: None,
        register_in_scope: true,
        rust_trait_name: None,
        methods: vec![Method {
            name: "field",
            ret_type: cve(),
            args: MethodArgs::Source(vec![("inner", cve()), ("key", cve())]),
        }, Method {
            name: "field_type_opt",
            ret_type: any(),
            args: MethodArgs::Source(vec![("key", cve())]),
        }],
    };
    let xx_ref = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeExprContextPushDynamic));
    let iface_aspect_drop_dynamic = InterfaceSpec {
        name: "aspect_drop_dynamic",
        builtin_unique_id: 2008,
        scope_id: 3008,
        field_prefix: "dd",
        kind: "AspectDropDynamic",
        description: None,
        register_in_scope: true,
        rust_trait_name: None,
        methods: vec![Method {
            name: "drop",
            ret_type: cve(),
            args: MethodArgs::Source(vec![("xx", xx_ref()), ("inner", cve())]),
        }],
    };
    let iface_binding = {
        use crate::buildsrc::util::MethodSig;

        InterfaceSpec {
            name: "binding",
            builtin_unique_id: 2005,
            scope_id: 3005,
            field_prefix: "se",
            kind: "Binding",
            description: None,
            register_in_scope: true,
            rust_trait_name: Some("Binding"),
            methods: vec![Method {
                name: "use_",
                ret_type: TypeRef::via_push(quote!(crate::langhelp::CodeValue)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![("code", TypeRef::via_push(quote!(crate::langhelp::Code)))],
                }),
            }],
        }
    };
    let iface_target = {
        use crate::buildsrc::util::MethodSig;

        InterfaceSpec {
            name: "target",
            builtin_unique_id: 2006,
            scope_id: 3006,
            field_prefix: "tg",
            kind: "Target",
            description: None,
            register_in_scope: true,
            rust_trait_name: Some("Target"),
            methods: vec![Method {
                name: "bind",
                ret_type: TypeRef::via_push(quote!((crate::langhelp::Code, crate::langhelp::BindingId))),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![("sub", TypeRef::via_push(quote!(crate::eval::dynamic::target::TargetBindSub)))],
                }),
            }, Method {
                name: "bind_sub_record",
                ret_type: TypeRef::via_push(quote!(crate::eval::dynamic::target::TargetBindSub)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId)))],
                    data: vec![
                        ("carry", TypeRef::via_push(quote!(crate::langhelp::Code))),
                        (
                            "fields",
                            TypeRef::opaque(
                                quote!(Vec <(crate::langhelp::ConstKey, crate::eval::dynamic::target::TargetBindSub) >),
                            ),
                        ),
                    ],
                }),
            }, Method {
                name: "bind_sub_tuple",
                ret_type: TypeRef::via_push(quote!(crate::eval::dynamic::target::TargetBindSub)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId)))],
                    data: vec![
                        ("carry", TypeRef::via_push(quote!(crate::langhelp::Code))),
                        (
                            "elements",
                            TypeRef::opaque(quote!(Vec < crate:: eval:: dynamic:: target:: TargetBindSub >)),
                        ),
                    ],
                }),
            }, Method {
                name: "seq",
                ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: Code >)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId)))],
                    data: vec![("entries", TypeRef::opaque(quote!(Vec < crate:: langhelp:: Code >)))],
                }),
            }, Method {
                name: "code_to_bind_sub",
                ret_type: TypeRef::via_push(
                    quote!(crate:: langhelp:: EResult < crate:: eval:: dynamic:: target:: TargetBindSub >),
                ),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId)))],
                    data: vec![
                        ("code", TypeRef::via_push(quote!(crate::langhelp::Code))),
                        ("type_", TypeRef::opaque(quote!(std:: rc:: Rc < dyn crate:: langhelp:: Type >))),
                    ],
                }),
            }, Method {
                name: "if_",
                ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![
                        (
                            "conditions",
                            TypeRef::opaque(quote!(Vec <(crate::langhelp::CodeValue, &crate::eval::expr::Expr) >)),
                        ),
                        ("else_", TypeRef::opaque(quote!(Option <& crate:: eval:: expr:: Expr >))),
                    ],
                }),
            }, Method {
                name: "match_",
                ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![
                        ("base", TypeRef::opaque(quote!(crate::langhelp::CodeValue))),
                        ("element_key", TypeRef::via_push(quote!(crate::langhelp::ConstKey))),
                        (
                            "cases",
                            TypeRef::opaque(
                                quote!(
                                    Vec <(
                                        String,
                                        std:: rc:: Rc < dyn crate:: langhelp:: Type >,
                                        &crate::eval::expr::Expr
                                    ) >
                                ),
                            ),
                        ),
                        ("fallback", TypeRef::opaque(quote!(Option <& crate:: eval:: expr:: Expr >))),
                    ],
                }),
            }, Method {
                name: "maybe_bind_sub",
                ret_type: TypeRef::via_push(
                    quote!(crate:: langhelp:: EResult < Option < crate:: eval:: dynamic:: target:: TargetBindSub >>),
                ),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![("cv", TypeRef::opaque(quote!(crate::langhelp::CodeValue)))],
                }),
            }, Method {
                name: "stage",
                ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![
                        ("tree", TypeRef::via_push(quote!(crate::eval::expr::Expr))),
                        ("lower_values", TypeRef::opaque(quote!(Vec < crate:: langhelp:: CodeValue >))),
                        ("graft_values", TypeRef::opaque(quote!(Vec < crate:: langhelp:: CodeValue >))),
                    ],
                }),
            }, Method {
                name: "cast_exception",
                ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: Code >)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![
                        ("call_code", TypeRef::via_push(quote!(crate::langhelp::Code))),
                        ("catch_binding", TypeRef::opaque(quote!(crate::langhelp::BindingId))),
                        ("cast_cv", TypeRef::opaque(quote!(crate::langhelp::CodeValue))),
                    ],
                }),
            }, Method {
                name: "drop",
                ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: Code >)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        (
                            "xx",
                            TypeRef::via_push_as(
                                quote!(&crate::langhelp::ExprContext),
                                quote!(crate::eval::code_value_extern::ExprContextPush),
                            ),
                        ),
                        ("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId))),
                    ],
                    data: vec![("cv", TypeRef::opaque(quote!(crate::langhelp::CodeValue)))],
                }),
            }, Method {
                name: "try_finally",
                ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: Code >)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![("loc", TypeRef::via_push(quote!(crate::langhelp::QExprId)))],
                    data: vec![
                        ("body", TypeRef::via_push(quote!(crate::langhelp::Code))),
                        ("finally", TypeRef::via_push(quote!(crate::langhelp::Code))),
                    ],
                }),
            }],
        }
    };
    let iface_code_dyn = InterfaceSpec {
        name: "code_dyn",
        builtin_unique_id: 2007,
        scope_id: 3007,
        field_prefix: "cd",
        kind: "CodeDyn",
        description: None,
        register_in_scope: true,
        rust_trait_name: None,
        methods: vec![],
    };
    let tree =
        branch(
            vec![
                (
                    "throw",
                    leaf(
                        initial_bind(
                            THROW_COMPILE_SCOPE_ID,
                            quote!(crate::eval::value::type_throw::dynamic::TypeThrowDynamic),
                        ),
                    ),
                ),
                (
                    "panic",
                    leaf(
                        initial_bind(
                            PANIC_COMPILE_SCOPE_ID,
                            quote!(crate::eval::value::type_panic::dynamic::TypePanicDynamic),
                        ),
                    ),
                ),
                ("log", leaf(push_fn(out, scope_ids, 5001, &[("message", "String")], "()", quote!{
                    let timestamp_ms =
                        std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as
                            u64;
                    scope.get_slot::<DynamicLogStore>().unwrap().logs.borrow_mut().push(LogEntry {
                        timestamp_ms,
                        message,
                    });
                }))),
                ("define", leaf(push_fn(out, scope_ids, 5002, &[], "DefinitionSetBuilder", quote!{
                    let ex = scope.get_slot::<DynamicExprContextSlot>().unwrap().xx.vx.clone();
                    let compile_id = crate::langhelp::CompileId(ex.ix.import_state, ex.ix.source_id);
                    let def_set_id = DefSetId(compile_id, ex.ix.def_set_counter.next());
                    let inner = Gc::new(DefinitionSet::new(def_set_id));
                    ex.definition_sets.borrow_mut().insert(def_set_id, inner.clone());
                    DefinitionSetBuilder { inner }
                }))),
                (
                    "types",
                    branch(
                        vec![
                            (
                                "list",
                                leaf(
                                    constructor(
                                        out,
                                        scope_ids,
                                        5010,
                                        &[("element_type", "crate::eval::push::AnyType")],
                                        "crate::eval::value::type_list::dynamic::TypeListDynamic",
                                        quote!{
                                            crate::eval::value::type_list::dynamic::TypeListDynamic {
                                                element_type: element_type.0
                                            }
                                        },
                                        quote!(
                                            crate::eval::value::type_list_constructor::r#const::TypeListConstructorConst
                                        ),
                                    ),
                                ),
                            ),
                            (
                                "map",
                                leaf(
                                    constructor(
                                        out,
                                        scope_ids,
                                        5011,
                                        &[
                                            ("key_type", "crate::eval::push::AnyType"),
                                            ("value_type", "crate::eval::push::AnyType"),
                                        ],
                                        "crate::eval::value::type_map::dynamic::TypeMapDynamic",
                                        quote!{
                                            crate::eval::value::type_map::dynamic::TypeMapDynamic {
                                                key_type: key_type.0,
                                                value_type: value_type.0,
                                            }
                                        },
                                        quote!(
                                            crate::eval::value::type_map_constructor::r#const::TypeMapConstructorConst
                                        ),
                                    ),
                                ),
                            ),
                            (
                                "option",
                                leaf(
                                    constructor(
                                        out,
                                        scope_ids,
                                        5012,
                                        &[("element_type", "crate::eval::push::AnyType")],
                                        "crate::eval::value::type_option::dynamic::TypeOptionDynamic",
                                        quote!{
                                            crate::eval::value::type_option::dynamic::TypeOptionDynamic {
                                                element_type: element_type.0
                                            }
                                        },
                                        quote!(
                                            crate
                                            ::eval
                                            ::value
                                            ::type_option_constructor
                                            ::r#const
                                            ::TypeOptionConstructorConst
                                        ),
                                    ),
                                ),
                            ),
                            (
                                "string",
                                leaf(any_const(quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic))),
                            ),
                            (
                                "number",
                                leaf(any_const(quote!(crate::eval::value::type_number::dynamic::TypeNumberDynamic))),
                            ),
                            (
                                "null",
                                leaf(any_const(quote!(crate::eval::value::type_null::dynamic::TypeNullDynamic))),
                            ),
                            (
                                "bool",
                                leaf(any_const(quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic))),
                            ),
                            ("any", leaf(any_const(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic)))),
                            (
                                "constkey",
                                leaf(
                                    any_const(quote!(crate::eval::value::type_constkey::dynamic::TypeConstkeyDynamic)),
                                ),
                            ),
                            ("expr", leaf(any_const(quote!(crate::eval::value::type_expr::TypeExpr)))),
                            (
                                "type",
                                leaf(any_const(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic))),
                            ),
                            (
                                "aspect",
                                leaf(any_const(quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic))),
                            ),
                            (
                                "codedyn",
                                leaf(
                                    any_const(
                                        quote!(
                                            crate::eval::value::type_code_dyn_handle::dynamic::TypeCodeDynHandleDynamic
                                        ),
                                    ),
                                ),
                            ),
                            (
                                "cve",
                                leaf(any_const(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic))),
                            ),
                        ],
                    ),
                ),
                (
                    "eval",
                    branch(
                        vec![
                            (
                                "aspect_lookup",
                                leaf(
                                    initial_bind(
                                        ASPECT_COMPILE_SCOPE_ID,
                                        quote!(
                                            crate::eval::value::type_function_aspect::dynamic::TypeFunctionAspectDynamic
                                        ),
                                    ),
                                ),
                            ),
                            (
                                "cast_value",
                                leaf(
                                    initial_bind(
                                        CAST_VALUE_COMPILE_SCOPE_ID,
                                        quote!(
                                            crate
                                            ::eval
                                            ::value
                                            ::type_function_cast_value
                                            ::dynamic
                                            ::TypeFunctionCastValueDynamic
                                        ),
                                    ),
                                ),
                            ),
                            (
                                "const_eval",
                                leaf(
                                    push_fn(
                                        out,
                                        scope_ids,
                                        5008,
                                        &[
                                            ("xx", "crate::eval::code_value_extern::ExprContextPush"),
                                            ("staged", "crate::eval::push::StagedExpr"),
                                        ],
                                        "crate::eval::code_value_extern::CodeValueExtern",
                                        quote!{
                                            let expr_value = staged.0;
                                            let expr_ast: Expr =
                                                Rc::try_unwrap(crate::langhelp::downcast_value::<Expr>(expr_value))
                                                    .ok()
                                                    .expect("const_eval: staged Expr Rc not unique");
                                            let res = const_eval(&xx.0, &expr_ast);
                                            let cv = res.v.unwrap_or_else(|| {
                                                panic!(
                                                    "const_eval: const_eval produced no value; errors: {:?}",
                                                    res.errors
                                                )
                                            });
                                            crate::eval::code_value_extern::CodeValueExtern(
                                                Rc::new(RefCell::new(Some(cv)))
                                            )
                                        },
                                    ),
                                ),
                            ),
                            (
                                "codevalue",
                                leaf(
                                    push_fn(
                                        out,
                                        scope_ids,
                                        5005,
                                        &[
                                            ("code", "crate::langhelp::Code"),
                                            ("value", "Rc<dyn crate::langhelp::Value>"),
                                        ],
                                        "crate::eval::code_value_extern::CodeValueExtern",
                                        quote!{
                                            let cv = crate::langhelp::CodeValue {
                                                code,
                                                value: Some(value),
                                            };
                                            crate::eval::code_value_extern::CodeValueExtern(
                                                Rc::new(RefCell::new(Some(cv)))
                                            )
                                        },
                                    ),
                                ),
                            ),
                            ("str_to_json", leaf(push_fn(out, scope_ids, 5003, &[("s", "String")], "String", quote!{
                                serde_json::to_string(&s).unwrap()
                            }))),
                            (
                                "constkey_to_str",
                                leaf(
                                    push_fn(
                                        out,
                                        scope_ids,
                                        5004,
                                        &[("key", "crate::langhelp::ConstKey")],
                                        "String",
                                        quote!{
                                            key.to_str()
                                        },
                                    ),
                                ),
                            ),
                            (
                                "gen_binding_name",
                                leaf(
                                    push_fn(
                                        out,
                                        scope_ids,
                                        5006,
                                        &[("_xx", "crate::eval::code_value_extern::ExprContextPush")],
                                        "String",
                                        quote!{
                                            let ex =
                                                scope.get_slot::<DynamicExprContextSlot>().unwrap().xx.vx.clone();
                                            let id = ex.next_local_id.next();
                                            format!("a{}", id)
                                        },
                                    ),
                                ),
                            ),
                            (
                                "new_expr_context",
                                leaf(
                                    push_fn(
                                        out,
                                        scope_ids,
                                        5009,
                                        &[
                                            ("target_arg", "Rc<dyn crate::r#gen::Target>"),
                                            (
                                                "scope_arg",
                                                "std::collections::HashMap<ConstKey, Rc<dyn crate::r#gen::Binding>>",
                                            ),
                                        ],
                                        "crate::eval::code_value_extern::ExprContextPush",
                                        quote!{
                                            let parent_ex =
                                                scope.get_slot::<DynamicExprContextSlot>().unwrap().xx.vx.clone();
                                            let mut scope_values: HashMap<ConstKey, Box<dyn crate::r#gen::Binding>> =
                                                HashMap::new();
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
                                                current_exception_type: Some(
                                                    Rc::new(crate::eval::value::type_string::dynamic::TypeStringDynamic)
                                                ),
                                            });
                                            crate::eval::code_value_extern::ExprContextPush(xx)
                                        },
                                    ),
                                ),
                            ),
                            ("aspect_function", leaf(interface(out, iface_aspect_function, extra_iface_specs))),
                            ("aspect_field", leaf(interface(out, iface_aspect_field, extra_iface_specs))),
                            (
                                "aspect_drop_dynamic",
                                leaf(interface(out, iface_aspect_drop_dynamic, extra_iface_specs)),
                            ),
                            (
                                "aspect_string_dynamic",
                                leaf(aspect_id(out, scope_ids, 4001, quote!(aspect_string_dynamic_aspect_id))),
                            ),
                            (
                                "aspect_function_id",
                                leaf(aspect_id(out, scope_ids, 4002, quote!(aspect_function_aspect_id))),
                            ),
                            (
                                "aspect_field_id",
                                leaf(aspect_id(out, scope_ids, 4003, quote!(aspect_field_aspect_id))),
                            ),
                            ("binding", leaf(interface(out, iface_binding, extra_iface_specs))),
                            ("target", leaf(interface(out, iface_target, extra_iface_specs))),
                            ("code_dyn", leaf(interface(out, iface_code_dyn, extra_iface_specs))),
                        ],
                    ),
                ),
            ],
        );
    out.extend(quote!{
        pub(
            crate
        ) fn gen_builtin_tree <'s >(
            dynamic_scope: & mut v8:: PinnedRef <'s,
            v8:: HandleScope <'_ >>,
            import_state: crate:: langhelp:: ImportStateId,
        ) -> crate:: eval:: builtin_tree:: BuiltinNode <'s > {
            let builtin_ifaces = crate::r#gen::gen_build_builtin_interfaces(import_state);
            #tree
        }
    });
}

fn initial_bind(dynamic_key_scope_id: usize, type_tokens: TokenStream) -> TokenStream {
    quote!{
        crate:: eval:: builtin_tree:: BuiltinLeaf:: InitialBind {
            dynamic_key: crate:: langhelp:: BindingId:: Builtin(#dynamic_key_scope_id),
            type_: std:: rc:: Rc:: new(#type_tokens) as std:: rc:: Rc < dyn crate:: langhelp:: Type >,
        }
    }
}

fn interface(out: &mut TokenStream, spec: InterfaceSpec, extra_iface_specs: &mut Vec<InterfaceSpec>) -> TokenStream {
    let scope_id = spec.scope_id;
    let field = format_ident!("{}", if spec.name == "type" {
        "type_"
    } else {
        spec.name
    });
    let mod_name = format_ident!("builtin_b{}", scope_id);
    let fn_name = format_ident!("b{}_leaf", scope_id);
    let prelude = submodule_prelude();
    extra_iface_specs.push(spec);
    out.extend(quote!{
        pub(crate) mod #mod_name {
            #prelude pub(
                crate
            ) fn #fn_name <'s >(
                dynamic_scope: & mut v8:: PinnedRef <'s,
                v8:: HandleScope <'_ >>,
                builtin_ifaces: & GenBuiltinInterfaces,
            ) -> crate:: eval:: builtin_tree:: BuiltinLeaf <'s > {
                let iface_ref = Rc:: new(SourceInterfaceRef {
                    data: builtin_ifaces.#field.clone()
                });
                let dynamic_val = crate::eval::to_dynamic_ext(dynamic_scope, iface_ref.clone());
                let iface_const_type: Rc<dyn crate::langhelp::Type> =
                    Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() });
                crate:: eval:: builtin_tree:: BuiltinLeaf:: Interface {
                    dynamic_key: BindingId:: Builtin(#scope_id),
                    type_: iface_const_type,
                    dynamic_value: dynamic_val.into(),
                }
            }
        }
        pub(crate) use #mod_name::*;
    });
    quote!{
        #fn_name(dynamic_scope, &builtin_ifaces)
    }
}

fn leaf(inner: TokenStream) -> TokenStream {
    quote!{
        crate:: eval:: builtin_tree:: BuiltinNode:: Leaf(#inner)
    }
}

fn push_fn(
    out: &mut TokenStream,
    scope_ids: &mut HashSet<usize>,
    builtin_scope_id: usize,
    args: &[(&'static str, &'static str)],
    ret: &'static str,
    body: TokenStream,
) -> TokenStream {
    assert!(scope_ids.insert(builtin_scope_id));
    let arg_dynamic_type = arg_dynamic_type_expr_str(args);
    let arg_decls: Vec<TokenStream> = args.iter().map(|(n, ty)| {
        let n = format_ident!("{}", n);
        let ty: TokenStream = ty.parse().unwrap();
        quote!{
            let #n: #ty = <#ty as crate:: eval:: push:: Push >:: from_dynamic(scope, _jw_args.get(_jw_arg_i));
            _jw_arg_i += 1;
        }
    }).collect();
    let ret_ty: TokenStream = ret.parse().unwrap();
    let mod_name = format_ident!("builtin_b{}", builtin_scope_id);
    let fn_name = format_ident!("b{}_leaf", builtin_scope_id);
    let prelude = submodule_prelude();
    out.extend(quote!{
        pub(crate) mod #mod_name {
            #prelude fn _jw_cb <'s >(
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
                _jw_rv.set(<#ret_ty as crate:: eval:: push:: Push >:: into_dynamic(_jw_result, scope));
            }
            pub(
                crate
            ) fn #fn_name <'s >(
                dynamic_scope: & mut v8:: PinnedRef <'s,
                v8:: HandleScope <'_ >>,
            ) -> crate:: eval:: builtin_tree:: BuiltinLeaf <'s > {
                crate:: eval:: builtin_tree:: BuiltinLeaf:: Function {
                    dynamic_key: crate:: langhelp:: BindingId:: Builtin(#builtin_scope_id),
                    type_: std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                        arg: #arg_dynamic_type,
                        ret: <#ret_ty as crate:: eval:: push:: Push >:: dynamic_type(),
                    }) as std:: rc:: Rc < dyn crate:: langhelp:: Type >,
                    dynamic_value: v8:: Function:: builder(_jw_cb).build(dynamic_scope).unwrap().cast::< v8:: Value >(),
                }
            }
        }
        pub(crate) use #mod_name::*;
    });
    quote!{
        #fn_name(dynamic_scope)
    }
}

fn submodule_prelude() -> TokenStream {
    quote!{
        #[allow(unused_imports)]
        use super::*;
        #[allow(unused_imports)]
        use std::rc::Rc;
        #[allow(unused_imports)]
        use std::cell::{
            Cell,
            RefCell,
        };
        #[allow(unused_imports)]
        use std::collections::HashMap;
        #[allow(unused_imports)]
        use dumpster::unsync::Gc;
        #[allow(unused_imports)]
        use crate::langhelp::{
            ConstKey,
            DefSetId,
            ExprContext,
            Scope,
            BindingId,
            Unique,
        };
        #[allow(unused_imports)]
        use crate::eval::DynamicExprContextSlot;
        #[allow(unused_imports)]
        use crate::eval::DynamicLogStore;
        #[allow(unused_imports)]
        use crate::langhelp::LogEntry;
        #[allow(unused_imports)]
        use crate::eval::RcBindingWrap;
        #[allow(unused_imports)]
        use crate::eval::const_eval;
        #[allow(unused_imports)]
        use crate::eval::definition_set::DefinitionSet;
        #[allow(unused_imports)]
        use crate::eval::expr::Expr;
        #[allow(unused_imports)]
        use crate::eval::value::type_source_interface::{
            SourceInterfaceRef,
            r#const::TypeSourceInterfaceConst,
        };
        #[allow(unused_imports)]
        use crate::eval::push::{
            AnyType,
            StagedExpr,
        };
    }
}
