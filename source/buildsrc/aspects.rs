use {
    crate::buildsrc::util::{
        InterfaceSpec,
        Method,
        MethodArgs,
        TypeRef,
        snake_case,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>, scope_ids: &mut HashSet<usize>) {
    let mut aspect_names = HashSet::new();
    let mut aspect_token_items: Vec<TokenStream> = Vec::new();

    struct AspectMethod {
        name: &'static str,
        params: Vec<(&'static str, &'static str)>,
        ret: &'static str,
    }

    struct AspectSpec {
        builtin_unique_id: usize,
        methods: Vec<AspectMethod>,
        name: &'static str,
    }

    for a in &vec![
        //. .
        AspectSpec {
            name: "Null",
            builtin_unique_id: 1001,
            methods: vec![],
        },
        AspectSpec {
            name: "Record",
            builtin_unique_id: 1002,
            methods: vec![],
        },
        AspectSpec {
            name: "NumberDynamic",
            builtin_unique_id: 1003,
            methods: vec![],
        },
        AspectSpec {
            name: "BoolDynamic",
            builtin_unique_id: 1004,
            methods: vec![],
        },
        AspectSpec {
            name: "StringDynamic",
            builtin_unique_id: 1005,
            methods: vec![],
        },
        AspectSpec {
            name: "BoolConst",
            builtin_unique_id: 1006,
            methods: vec![AspectMethod {
                name: "get",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "bool",
            }],
        },
        AspectSpec {
            name: "NumberConst",
            builtin_unique_id: 1008,
            methods: vec![AspectMethod {
                name: "get",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "f64",
            }],
        },
        AspectSpec {
            name: "StringConst",
            builtin_unique_id: 1009,
            methods: vec![AspectMethod {
                name: "get",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "String",
            }],
        },
        AspectSpec {
            name: "Type",
            builtin_unique_id: 1010,
            methods: vec![AspectMethod {
                name: "type_",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "std::rc::Rc<dyn crate::langhelp::Type>",
            }],
        },
        AspectSpec {
            name: "ListDynamic",
            builtin_unique_id: 1011,
            methods: vec![AspectMethod {
                name: "element_type",
                params: vec![],
                ret: "std::rc::Rc<dyn crate::langhelp::Type>",
            }],
        },
        AspectSpec {
            name: "MapDynamic",
            builtin_unique_id: 1012,
            methods: vec![
                //. .
                AspectMethod {
                    name: "key_type",
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
                AspectMethod {
                    name: "value_type",
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
            ],
        },
        AspectSpec {
            name: "Tuple",
            builtin_unique_id: 1013,
            methods: vec![
                //. .
                AspectMethod {
                    name: "element_types",
                    params: vec![],
                    ret: "Vec<std::rc::Rc<dyn crate::langhelp::Type>>",
                },
                AspectMethod {
                    name: "take_elements",
                    params: vec![("value", "std::rc::Rc<dyn crate::langhelp::Value>")],
                    ret: "Vec<crate::langhelp::EvalRes>",
                },
            ],
        },
        AspectSpec {
            name: "TaggedUnion",
            builtin_unique_id: 1014,
            methods: vec![AspectMethod {
                name: "variants",
                params: vec![],
                ret: "Vec<(crate::langhelp::ConstKey, String, std::rc::Rc<dyn crate::langhelp::Type>)>",
            }],
        },
        AspectSpec {
            name: "Field",
            builtin_unique_id: 1015,
            methods: vec![
                //. .
                AspectMethod {
                    name: "field",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("self_", "crate::langhelp::CodeValue"),
                        ("key", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
                AspectMethod {
                    name: "field_type_opt",
                    params: vec![("xx", "&crate::langhelp::ExprContext"), ("key", "&crate::langhelp::ConstKey")],
                    ret: "Option<std::rc::Rc<dyn crate::langhelp::Type>>",
                },
            ],
        },
        AspectSpec {
            name: "Function",
            builtin_unique_id: 1116,
            methods: vec![AspectMethod {
                name: "call",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("self_", "crate::langhelp::CodeValue"),
                    ("argument", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EvalRes",
            }],
        },
        AspectSpec {
            name: "Set",
            builtin_unique_id: 1017,
            methods: vec![AspectMethod {
                name: "set",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("self_", "crate::langhelp::CodeValue"),
                    ("value", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EvalRes",
            }],
        },
        AspectSpec {
            name: "OperatorAdd",
            builtin_unique_id: 1018,
            methods: vec![
                //. .
                AspectMethod {
                    name: "add",
                    params: vec![
                        //. .
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("base", "crate::langhelp::CodeValue"),
                        ("reference", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "OperatorEq",
            builtin_unique_id: 1019,
            methods: vec![
                //. .
                AspectMethod {
                    name: "eq",
                    params: vec![
                        //. .
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("base", "crate::langhelp::CodeValue"),
                        ("reference", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "OperatorLt",
            builtin_unique_id: 1020,
            methods: vec![
                //. .
                AspectMethod {
                    name: "lt",
                    params: vec![
                        //. .
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("base", "crate::langhelp::CodeValue"),
                        ("reference", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "InitialBind",
            builtin_unique_id: 1021,
            methods: vec![
                //. .
                AspectMethod {
                    name: "initial_bind",
                    params: vec![
                        //. .
                        ("dynamic_key", "crate::langhelp::ScopeEntryId"),
                    ],
                    ret: "Box<dyn crate::r#gen::ScopeEntry>",
                },
            ],
        },
        AspectSpec {
            name: "Bind",
            builtin_unique_id: 1022,
            methods: vec![
                //. .
                AspectMethod {
                    name: "bind",
                    params: vec![
                        //. .
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("cv", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)>",
                },
            ],
        },
        AspectSpec {
            name: "BindDynamic",
            builtin_unique_id: 1023,
            methods: vec![
                //. .
                AspectMethod {
                    name: "bind_dynamic",
                    params: vec![
                        //. .
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("cv", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EResult<Option<crate::eval::TargetBindSub>>",
                },
            ],
        },
        AspectSpec {
            name: "ConsumeDynamic",
            builtin_unique_id: 1024,
            methods: vec![
                //. .
                AspectMethod {
                    name: "to_dynamic",
                    params: vec![
                        //. .
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("cv", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EResult<crate::code_dynamic::CodeDynamic>",
                },
            ],
        },
        AspectSpec {
            name: "StackValueDynamic",
            builtin_unique_id: 1025,
            methods: vec![
                //. .
                AspectMethod {
                    name: "to_value",
                    params: vec![
                        //. .
                        ("loc", "crate::langhelp::QExprId"),
                        ("self_", "crate::code_dynamic::CodeDynamic"),
                    ],
                    ret: "crate::langhelp::CodeValue",
                },
            ],
        },
        AspectSpec {
            name: "LiftDynamic",
            builtin_unique_id: 1026,
            methods: vec![
                //. .
                AspectMethod {
                    name: "lift",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("scope", "&v8::PinScope"),
                        ("self_", "v8::Local<v8::Value>"),
                    ],
                    ret: "Result<std::rc::Rc<dyn crate::langhelp::Value>, String>",
                },
            ],
        },
        AspectSpec {
            name: "LowerDynamic",
            builtin_unique_id: 1031,
            methods: vec![
                //. .
                AspectMethod {
                    name: "lower",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("scope", "&v8::PinScope"),
                        ("self_", "v8::Local<v8::Value>"),
                    ],
                    ret: "Result<std::rc::Rc<dyn crate::langhelp::Value>, String>",
                },
            ],
        },
        AspectSpec {
            name: "Iterable",
            builtin_unique_id: 1027,
            methods: vec![
                //. .
                AspectMethod {
                    name: "element_type",
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
                AspectMethod {
                    name: "foreach",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("base", "crate::langhelp::CodeValue"),
                        ("element_key", "crate::langhelp::ScopeEntryId"),
                        ("body", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "Artifact",
            builtin_unique_id: 1028,
            methods: vec![
                //. .
                AspectMethod {
                    name: "to_serial",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("self_", "std::rc::Rc<dyn crate::langhelp::Value>"),
                    ],
                    ret: "crate::langhelp::EResult<crate::langhelp::Artifact>",
                },
                AspectMethod {
                    name: "from_serial",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("data", "&serde_json::Value"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "Aspect",
            builtin_unique_id: 1029,
            methods: vec![
                //. .
                AspectMethod {
                    name: "get_unique",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "crate::langhelp::Unique",
                },
                AspectMethod {
                    name: "get_iface_data",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "dumpster::unsync::Gc<crate::eval::value::type_source_interface::SourceInterfaceData>",
                },
            ],
        },
    ] {
        assert!(aspect_names.insert(a.name), "duplicate aspect name: {}", a.name);
        let full_name = format!("Aspect{}", a.name);
        let trait_name = format_ident!("{}", full_name);
        let snake = snake_case(&full_name);
        let id_fn = format_ident!("{}_aspect_id", snake);
        let get_fn = format_ident!("{}_get", snake);
        let get_maybe_fn = format_ident!("{}_get_maybe", snake);
        assert!(builtin_unique_ids.insert(a.builtin_unique_id));
        let builtin_unique_id = a.builtin_unique_id;
        let trait_methods: Vec<TokenStream> = a.methods.iter().map(|m| {
            let name = format_ident!("{}", m.name);
            let params: Vec<TokenStream> = m.params.iter().map(|(n, ty)| {
                let n = format_ident!("{}", n);
                let ty: TokenStream = ty.parse().unwrap();
                quote!{
                    #n: #ty
                }
            }).collect();
            let ret: TokenStream = if m.ret.is_empty() {
                quote!{
                }
            } else {
                let ty: TokenStream = m.ret.parse().unwrap();
                quote!{
                    -> #ty
                }
            };
            quote!{
                fn #name(&self, #(#params), *) #ret;
            }
        }).collect();
        let missing_msg = format!("Type ({{}}) is missing aspect {}", full_name);
        aspect_token_items.push(quote!{
            pub(crate) trait #trait_name: crate:: langhelp:: Aspect {
                #(#trait_methods) *
            }
            pub(crate) fn #id_fn() -> crate:: langhelp:: Unique {
                crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
            }
            pub(
                crate
            ) fn #get_fn(
                xx: & crate:: langhelp:: ExprContext,
                loc: crate:: langhelp:: QExprId,
                v: & dyn crate:: langhelp:: Type,
            ) -> crate:: langhelp:: EResult < Box < dyn #trait_name >> {
                let Some(out) = v.aspect(xx, &#id_fn()) else {
                    return crate::langhelp::EResult::err(loc, format!(#missing_msg, v.desc()));
                };
                return crate:: langhelp:: EResult:: ok(
                    crate:: invariant:: must_downcast_box::< Box < dyn #trait_name >>(out)
                );
            }
            pub(
                crate
            ) fn #get_maybe_fn(
                xx: & crate:: langhelp:: ExprContext,
                v: & dyn crate:: langhelp:: Type,
            ) -> Option < Box < dyn #trait_name >> {
                let Some(out) = v.aspect(xx, &#id_fn()) else {
                    return None;
                };
                return Some(crate:: invariant:: must_downcast_box::< Box < dyn #trait_name >>(out));
            }
        });
    }
    out.extend(quote!{
        #(#aspect_token_items) *
    });

    struct AspectScopeEntry {
        aspect_fn: &'static str,
        key: &'static str,
        scope_id: usize,
    }

    let mut reg_entries: Vec<TokenStream> = Vec::new();
    for e in &[
        //. .
        AspectScopeEntry {
            key: "aspect_string_dynamic",
            scope_id: 4001,
            aspect_fn: "aspect_string_dynamic",
        },
        AspectScopeEntry {
            key: "aspect_function_id",
            scope_id: 4002,
            aspect_fn: "aspect_function",
        },
        AspectScopeEntry {
            key: "aspect_field_id",
            scope_id: 4003,
            aspect_fn: "aspect_field",
        },
    ] {
        assert!(scope_ids.insert(e.scope_id));
        let key = e.key;
        let scope_id = e.scope_id;
        let aspect_id_fn = format_ident!("{}_aspect_id", e.aspect_fn);
        reg_entries.push(quote!{
            {
                let dynamic_key = ScopeEntryId:: Builtin(#scope_id);
                let unique = crate:: r#gen:: #aspect_id_fn();
                if register_v8 {
                    let unique_v8 = crate::eval::to_v8(dynamic_scope, unique.clone());
                    dynamic_scope
                        .get_current_context()
                        .global(dynamic_scope)
                        .set(
                            dynamic_scope,
                            v8::String::new(dynamic_scope, &crate::eval::scope_entry_id_dynamic_key(dynamic_key),)
                                .unwrap()
                                .cast(),
                            unique_v8.into(),
                        );
                }
                builtin_scope_entries.push((#key.to_string(), dynamic_key, unique_type.clone()));
                scope.values.borrow_mut(
                ).insert(
                    ConstKey:: String(#key.to_string()),
                    Box::new(crate::eval::value::value_unique::ScopeEntryUniqueDualMode {
                        dynamic_key,
                        unique,
                    }) as Box<dyn crate::r#gen::ScopeEntry>,
                );
            }
        });
    }
    out.extend(quote!{
        pub(
            crate
        ) fn gen_register_builtin_aspect_ids <'s >(
            register_v8: bool,
            dynamic_scope: & mut v8:: PinnedRef <'s,
            v8:: HandleScope <'_ >>,
            scope: & crate:: langhelp:: Scope,
            builtin_scope_entries: & mut Vec <(String, ScopeEntryId, Rc < dyn crate:: langhelp:: Type >,) >,
        ) {
            let unique_type: Rc<dyn crate::langhelp::Type> = Rc::new(crate::eval::value::value_unique::TypeUnique);
            #(#reg_entries) *
        }
    });
}

pub fn interface_specs() -> Vec<InterfaceSpec> {
    let any = || TypeRef::unit(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic));
    let cve = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic));
    vec![
        //. .
        InterfaceSpec {
            name: "aspect_function",
            builtin_unique_id: 1016,
            scope_id: 3003,
            field_prefix: "af",
            kind: "AspectFunction",
            description: None,
            rust_trait_name: None,
            methods: vec![Method {
                name: "call",
                ret_type: cve(),
                args: MethodArgs::Source(any()),
            }],
        },
        InterfaceSpec {
            name: "aspect_field",
            builtin_unique_id: 1030,
            scope_id: 3004,
            field_prefix: "afd",
            kind: "AspectField",
            description: None,
            rust_trait_name: None,
            methods: vec![
                //. .
                Method {
                    name: "field",
                    ret_type: cve(),
                    args: MethodArgs::Source(any()),
                },
                Method {
                    name: "field_type_opt",
                    ret_type: any(),
                    args: MethodArgs::Source(any()),
                },
            ],
        },
    ]
}
