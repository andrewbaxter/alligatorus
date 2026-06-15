use {
    crate::buildsrc::util::{
        Method,
        MethodArgs,
        MethodSig,
        TypeRef,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>, scope_ids: &mut HashSet<usize>) {
    let mut iface_names = HashSet::new();
    let mut iface_prefixes = HashSet::new();
    let mut iface_kinds = HashSet::new();
    let mut iface_struct_fields: Vec<TokenStream> = Vec::new();
    let mut iface_build_entries: Vec<TokenStream> = Vec::new();
    let mut iface_result_fields: Vec<TokenStream> = Vec::new();
    let mut iface_trait_defs: Vec<TokenStream> = Vec::new();
    let mut iface_reg_entries: Vec<TokenStream> = Vec::new();
    let any = || TypeRef::unit(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic));
    let null = || TypeRef::unit(quote!(crate::eval::value::type_null::dynamic::TypeNullDynamic));
    let string = || TypeRef::unit(quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic));
    let bool_ = || TypeRef::unit(quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic));
    let aspect = || TypeRef::unit(quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic));
    let cve = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic));

    struct InterfaceSpec {
        builtin_unique_id: usize,
        scope_id: usize,
        description: Option<&'static str>,
        field_prefix: &'static str,
        kind: &'static str,
        methods: Vec<Method>,
        name: &'static str,
        rust_trait_name: Option<&'static str>,
    }

    for iface in vec![
        //. .
        InterfaceSpec {
            name: "value",
            builtin_unique_id: 2001,
            scope_id: 3001,
            field_prefix: "v",
            kind: "Value",
            description: None,
            rust_trait_name: None,
            methods: vec![
                //. .
                Method {
                    name: "type",
                    ret_type: any(),
                    args: MethodArgs::Source(null()),
                },
            ],
        },
        InterfaceSpec {
            name: "type",
            builtin_unique_id: 2002,
            scope_id: 3002,
            field_prefix: "t",
            kind: "Type",
            description: None,
            rust_trait_name: None,
            methods: vec![
                //. .
                Method {
                    name: "can_cast_from",
                    ret_type: bool_(),
                    args: MethodArgs::Source(any()),
                },
                Method {
                    name: "cast_from",
                    ret_type: cve(),
                    args: MethodArgs::Source(any()),
                },
                Method {
                    name: "desc",
                    ret_type: string(),
                    args: MethodArgs::Source(null()),
                },
                Method {
                    name: "aspect",
                    ret_type: {
                        let inner_mortar = &aspect().mortar;
                        TypeRef {
                            path: quote!(crate::eval::value::type_option::dynamic::TypeOptionDynamic),
                            mortar: quote!{
                                std:: rc:: Rc:: new(crate:: eval:: value:: type_option:: dynamic:: TypeOptionDynamic {
                                    element_type: #inner_mortar,
                                }) as std:: rc:: Rc < dyn crate:: langhelp:: Type >
                            },
                        }
                    },
                    args: MethodArgs::Source(any()),
                },
            ],
        },
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
        InterfaceSpec {
            name: "scope_entry",
            builtin_unique_id: 2005,
            scope_id: 3005,
            field_prefix: "se",
            kind: "ScopeEntry",
            description: None,
            rust_trait_name: Some("ScopeEntry"),
            methods: vec![Method {
                name: "use_",
                ret_type: TypeRef::via_dynamic_wrap(quote!(crate::langhelp::CodeValue)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        ("xx", quote!(&crate::langhelp::ExprContext)),
                        ("loc", quote!(crate::langhelp::QExprId)),
                    ],
                    data: vec![("code", TypeRef::via_dynamic_wrap(quote!(crate::langhelp::Code)))],
                }),
            }],
        },
        InterfaceSpec {
            name: "target",
            builtin_unique_id: 2006,
            scope_id: 3006,
            field_prefix: "tg",
            kind: "Target",
            description: None,
            rust_trait_name: Some("Target"),
            methods: vec![
                //. .
                Method {
                    name: "bind",
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!((crate::langhelp::Code, crate::langhelp::ScopeEntryId)),
                    ),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                        ],
                        data: vec![("sub", TypeRef::via_dynamic_wrap(quote!(crate::eval::TargetBindSub)))],
                    }),
                },
                Method {
                    name: "bind_sub_record",
                    ret_type: TypeRef::via_dynamic_wrap(quote!(crate::eval::TargetBindSub)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![
                            ("carry", TypeRef::via_dynamic_wrap(quote!(crate::langhelp::Code))),
                            (
                                "fields",
                                TypeRef::opaque(
                                    quote!(Vec <(crate::langhelp::ConstKey, crate::eval::TargetBindSub) >),
                                ),
                            ),
                        ],
                    }),
                },
                Method {
                    name: "bind_sub_tuple",
                    ret_type: TypeRef::via_dynamic_wrap(quote!(crate::eval::TargetBindSub)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![
                            ("carry", TypeRef::via_dynamic_wrap(quote!(crate::langhelp::Code))),
                            ("elements", TypeRef::opaque(quote!(Vec < crate:: eval:: TargetBindSub >))),
                        ],
                    }),
                },
                Method {
                    name: "block",
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: Code >),
                    ),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![("entries", TypeRef::opaque(quote!(Vec < crate:: langhelp:: Code >)))],
                    }),
                },
                Method {
                    name: "code_to_bind_sub",
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: eval:: TargetBindSub >),
                    ),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![
                            ("code", TypeRef::via_dynamic_wrap(quote!(crate::langhelp::Code))),
                            ("type_", TypeRef::opaque(quote!(std:: rc:: Rc < dyn crate:: langhelp:: Type >))),
                        ],
                    }),
                },
                Method {
                    name: "if_",
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >),
                    ),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                        ],
                        data: vec![
                            (
                                "conditions",
                                TypeRef::opaque(
                                    quote!(Vec <(crate::langhelp::CodeValue, &crate::eval::expr::Expr) >),
                                ),
                            ),
                            ("else_", TypeRef::opaque(quote!(Option <& crate:: eval:: expr:: Expr >))),
                        ],
                    }),
                },
                Method {
                    name: "match_",
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >),
                    ),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                        ],
                        data: vec![
                            ("base", TypeRef::opaque(quote!(crate::langhelp::CodeValue))),
                            ("element_key", TypeRef::via_dynamic_wrap(quote!(crate::langhelp::ConstKey))),
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
                },
                Method {
                    name: "maybe_bind_sub",
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < Option < crate:: eval:: TargetBindSub >>),
                    ),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                        ],
                        data: vec![("cv", TypeRef::opaque(quote!(crate::langhelp::CodeValue)))],
                    }),
                },
                Method {
                    name: "stage",
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >),
                    ),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                        ],
                        data: vec![
                            ("tree", TypeRef::via_dynamic_wrap(quote!(crate::eval::expr::Expr))),
                            ("lower_values", TypeRef::opaque(quote!(Vec < crate:: langhelp:: CodeValue >))),
                        ],
                    }),
                },
            ],
        },
        InterfaceSpec {
            name: "code_dyn",
            builtin_unique_id: 2007,
            scope_id: 3007,
            field_prefix: "cd",
            kind: "CodeDyn",
            description: None,
            rust_trait_name: None,
            methods: vec![],
        },
    ] {
        assert!(iface_names.insert(iface.name), "duplicate interface name: {}", iface.name);
        assert!(iface_prefixes.insert(iface.field_prefix), "duplicate field_prefix: {}", iface.field_prefix);
        assert!(iface_kinds.insert(iface.kind), "duplicate kind: {}", iface.kind);
        let mut method_names = HashSet::new();
        for m in &iface.methods {
            assert!(method_names.insert(m.name), "duplicate method name on iface {}: {}", iface.name, m.name,);
        }
        let prefix = iface.field_prefix;
        let methods_expr_items: Vec<TokenStream> = iface.methods.iter().enumerate().map(|(idx, m)| {
            let name = m.name;
            let field_name = format!("_im_{}{}", prefix, idx);
            let arg_mortar = match &m.args {
                MethodArgs::Source(t) => t.mortar.clone(),
                MethodArgs::Rust(rs) => {
                    let mortars: Vec<TokenStream> =
                        rs.data.iter().map(|(_, t)| t.mortar.clone()).collect();
                    crate::buildsrc::util::arg_mortar_expr(&mortars)
                },
            };
            let ret = &m.ret_type.mortar;
            quote!{
                crate:: eval:: value:: type_source_interface:: SourceInterfaceMethodData {
                    name: #name.to_string(),
                    arg_type: std:: cell:: RefCell:: new(Some(#arg_mortar)),
                    ret_type: std:: cell:: RefCell:: new(Some(#ret)),
                    field_name: #field_name.to_string(),
                }
            }
        }).collect();
        let methods_expr = quote!{
            vec![#(#methods_expr_items), *]
        };
        let f = format_ident!("{}", if iface.name == "type" {
            "type_"
        } else {
            iface.name
        });
        iface_struct_fields.push(quote!{
            pub #f: dumpster:: unsync:: Gc < crate:: eval:: value:: type_source_interface:: SourceInterfaceData >
        });
        let kind = format_ident!("{}", iface.kind);
        let description = iface.description.unwrap_or(iface.name);
        assert!(builtin_unique_ids.insert(iface.builtin_unique_id));
        assert!(scope_ids.insert(iface.scope_id));
        let builtin_unique_id = iface.builtin_unique_id;
        let scope_id = iface.scope_id;
        iface_build_entries.push(quote!{
            let #f = dumpster:: unsync:: Gc:: new(crate:: eval:: value:: type_source_interface:: SourceInterfaceData {
                builtin_kind: Some(crate:: eval:: value:: type_source_interface:: BuiltinInterfaceKind::#kind,),
                set: builtin_def_set.clone(),
                description: std:: cell:: RefCell:: new(Some(#description.to_string())),
                is_aspect: std:: cell:: Cell:: new(false),
                methods: std:: cell:: RefCell:: new(#methods_expr),
                identity: crate:: langhelp:: Unique:: Builtin(#builtin_unique_id),
            },);
            builtin_def_set.interfaces.borrow_mut().push(#f.clone());
        });
        iface_result_fields.push(quote!{
            #f: #f
        });
        let name = iface.name;
        iface_reg_entries.push(quote!{
            {
                let iface_ref = Rc:: new(SourceInterfaceRef { data: builtin_ifaces.#f });
                let dynamic_key = ScopeEntryId:: Builtin(#scope_id);
                if register_v8 {
                    let v8_val = crate:: eval:: to_v8(dynamic_scope, iface_ref.clone());
                    dynamic_scope
                        .get_current_context()
                        .global(dynamic_scope)
                        .set(
                            dynamic_scope,
                            v8:: String:: new(
                                dynamic_scope,
                                &crate:: eval:: scope_entry_id_dynamic_key(dynamic_key),
                            )
                            .unwrap()
                            .cast(),
                            v8_val.into(),
                        );
                }
                let iface_const_type: Rc < dyn crate:: langhelp:: Type > =
                    Rc:: new(TypeSourceInterfaceConst { data: iface_ref.data.clone() });
                let ib =
                    crate::r#gen:: aspect_initial_bind_get(xx, root_loc, iface_const_type.as_ref()).into_res()?;
                builtin_scope_entries.push((#name.to_string(), dynamic_key, iface_const_type.clone()));
                scope
                    .values
                    .borrow_mut()
                    .insert(ConstKey:: String(#name.to_string()), ib.initial_bind(dynamic_key));
            }
        });
        if let Some(trait_name_str) = iface.rust_trait_name {
            let trait_name = format_ident!("{}", trait_name_str);
            let methods: Vec<TokenStream> = iface.methods.iter().filter_map(|m| {
                let rs = match &m.args {
                    MethodArgs::Rust(rs) => rs,
                    MethodArgs::Source(_) => return None,
                };
                let method_name = format_ident!("{}", if m.name == "type" {
                    "type_"
                } else {
                    m.name
                });
                let params: Vec<TokenStream> =
                    rs
                        .context
                        .iter()
                        .map(|(n, ty)| (n, ty.clone()))
                        .chain(rs.data.iter().map(|(n, t)| (n, t.path.clone())))
                        .map(|(n, ty)| {
                            let name = format_ident!("{}", n);
                            quote!{
                                #name: #ty
                            }
                        })
                        .collect();
                let ret = &m.ret_type.path;
                Some(quote!{
                    fn #method_name(&self, #(#params), *) -> #ret;
                })
            }).collect();
            iface_trait_defs.push(quote!{
                pub(crate) trait #trait_name {
                    #(#methods) *
                }
            });
        }
    }
    out.extend(quote!{
        pub(
            crate
        ) fn gen_register_builtin_interfaces <'s >(
            xx: & crate:: langhelp:: ExprContext,
            root_loc: crate:: langhelp:: QExprId,
            register_v8: bool,
            dynamic_scope: & mut v8:: PinnedRef <'s,
            v8:: HandleScope <'_ >>,
            scope: & crate:: langhelp:: Scope,
            builtin_scope_entries: & mut Vec <(
                String,
                ScopeEntryId,
                Rc < dyn crate:: langhelp:: Type >,
            ) >,
            import_state: crate:: langhelp:: ImportStateId,
        ) -> Result <(),
        Vec < crate:: langhelp:: ExprErr >> {
            let builtin_ifaces = crate::r#gen:: gen_build_builtin_interfaces(import_state);
            #(#iface_reg_entries) *
            Ok(())
        }
    });
    out.extend(quote!{
        #(#iface_trait_defs) * pub(crate) struct GenBuiltinInterfaces {
            #(#iface_struct_fields),
            *
        }
        pub(
            crate
        ) fn gen_build_builtin_interfaces(import_state: crate:: langhelp:: ImportStateId,) -> GenBuiltinInterfaces {
            let builtin_def_set =
                dumpster::unsync::Gc::new(
                    crate::eval::definition_set::DefinitionSet::new(
                        import_state,
                        crate::langhelp::DefSetId(usize::MAX),
                        crate::langhelp::SourceId(usize::MAX),
                    ),
                );
            #(#iface_build_entries) * GenBuiltinInterfaces {
                #(#iface_result_fields),
                *
            }
        }
    });
}
