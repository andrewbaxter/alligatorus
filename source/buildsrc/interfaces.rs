use {
    crate::buildsrc::util::{
        MethodSpec,
        RustTrait,
        TypeRef,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>) {
    let mut iface_names = HashSet::new();
    let mut iface_prefixes = HashSet::new();
    let mut iface_kinds = HashSet::new();
    let mut iface_struct_fields: Vec<TokenStream> = Vec::new();
    let mut iface_build_entries: Vec<TokenStream> = Vec::new();
    let mut iface_result_fields: Vec<TokenStream> = Vec::new();
    let mut iface_trait_defs: Vec<TokenStream> = Vec::new();
    let any = || TypeRef::unit(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic));
    let null = || TypeRef::unit(quote!(crate::eval::value::type_null::dynamic::TypeNullDynamic));
    let string = || TypeRef::unit(quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic));
    let bool_ = || TypeRef::unit(quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic));
    let aspect = || TypeRef::unit(quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic));
    let cve = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic));

    struct InterfaceSpec {
        builtin_unique_id: usize,
        description: Option<&'static str>,
        field_prefix: &'static str,
        kind: &'static str,
        methods: Vec<MethodSpec>,
        name: &'static str,
        rust_trait_name: Option<&'static str>,
    }

    for iface in vec![
        //. .
        InterfaceSpec {
            name: "value",
            builtin_unique_id: 2001,
            field_prefix: "v",
            kind: "Value",
            description: None,
            rust_trait_name: None,
            methods: vec![
                //. .
                MethodSpec {
                    name: "type",
                    arg_type: null(),
                    ret_type: any(),
                    rust_trait: None,
                },
            ],
        },
        InterfaceSpec {
            name: "type",
            builtin_unique_id: 2002,
            field_prefix: "t",
            kind: "Type",
            description: None,
            rust_trait_name: None,
            methods: vec![
                //. .
                MethodSpec {
                    name: "can_cast_from",
                    arg_type: any(),
                    ret_type: bool_(),
                    rust_trait: None,
                },
                MethodSpec {
                    name: "cast_from",
                    arg_type: any(),
                    ret_type: cve(),
                    rust_trait: None,
                },
                MethodSpec {
                    name: "desc",
                    arg_type: null(),
                    ret_type: string(),
                    rust_trait: None,
                },
                MethodSpec {
                    name: "aspect",
                    arg_type: any(),
                    ret_type: {
                        let inner_mortar = &aspect().mortar;
                        TypeRef {
                            path: quote!(crate::eval::value::type_option::dynamic::TypeOptionDynamic),
                            mortar: quote!{
                                std:: rc:: Rc:: new(
                                    crate:: eval:: value:: type_option:: dynamic:: TypeOptionDynamic {
                                        element_type: #inner_mortar,
                                    }
                                ) as std:: rc:: Rc < dyn crate:: langhelp:: Type >
                            },
                        }
                    },
                    rust_trait: None,
                },
            ],
        },
        InterfaceSpec {
            name: "aspect_function",
            builtin_unique_id: 1016,
            field_prefix: "af",
            kind: "AspectFunction",
            description: None,
            rust_trait_name: None,
            methods: vec![MethodSpec {
                name: "call",
                arg_type: any(),
                ret_type: cve(),
                rust_trait: None,
            }],
        },
        InterfaceSpec {
            name: "aspect_field",
            builtin_unique_id: 1015,
            field_prefix: "afd",
            kind: "AspectField",
            description: None,
            rust_trait_name: None,
            methods: vec![
                //. .
                MethodSpec {
                    name: "field",
                    arg_type: any(),
                    ret_type: cve(),
                    rust_trait: None,
                },
                MethodSpec {
                    name: "field_type_opt",
                    arg_type: any(),
                    ret_type: any(),
                    rust_trait: None,
                },
            ],
        },
        InterfaceSpec {
            name: "scope_entry",
            builtin_unique_id: 2005,
            field_prefix: "se",
            kind: "ScopeEntry",
            description: None,
            rust_trait_name: Some("ScopeEntry"),
            methods: vec![MethodSpec {
                name: "use_",
                arg_type: any(),
                ret_type: TypeRef::via_dynamic_wrap(quote!(crate::langhelp::CodeValue)),
                rust_trait: Some(RustTrait { params: vec![
                    //. .
                    ("xx", quote!(&crate::langhelp::ExprContext)),
                    ("loc", quote!(crate::langhelp::QExprId)),
                    ("code", quote!(crate::langhelp::Code)),
                ] }),
            }],
        },
        InterfaceSpec {
            name: "target",
            builtin_unique_id: 2006,
            field_prefix: "tg",
            kind: "Target",
            description: None,
            rust_trait_name: Some("Target"),
            methods: vec![
                //. .
                MethodSpec {
                    name: "bind",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!((crate::langhelp::Code, crate::langhelp::ScopeEntryId)),
                    ),
                    rust_trait: Some(
                        RustTrait {
                            params: vec![
                                ("xx", quote!(&crate::langhelp::ExprContext)),
                                ("loc", quote!(crate::langhelp::QExprId)),
                                ("sub", quote!(crate::eval::TargetBindSub)),
                            ],
                        },
                    ),
                },
                MethodSpec {
                    name: "bind_sub_record",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(quote!(crate::eval::TargetBindSub)),
                    rust_trait: Some(RustTrait { params: vec![
                        //. .
                        ("loc", quote!(crate::langhelp::QExprId)),
                        ("carry", quote!(crate::langhelp::Code)),
                        ("fields", quote!(Vec <(crate::langhelp::ConstKey, crate::eval::TargetBindSub) >)),
                    ] }),
                },
                MethodSpec {
                    name: "bind_sub_tuple",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(quote!(crate::eval::TargetBindSub)),
                    rust_trait: Some(RustTrait { params: vec![
                        //. .
                        ("loc", quote!(crate::langhelp::QExprId)),
                        ("carry", quote!(crate::langhelp::Code)),
                        ("elements", quote!(Vec < crate:: eval:: TargetBindSub >)),
                    ] }),
                },
                MethodSpec {
                    name: "block",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: Code >),
                    ),
                    rust_trait: Some(
                        RustTrait {
                            params: vec![
                                ("loc", quote!(crate::langhelp::QExprId)),
                                ("entries", quote!(Vec < crate:: langhelp:: Code >)),
                            ],
                        },
                    ),
                },
                MethodSpec {
                    name: "code_to_bind_sub",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: eval:: TargetBindSub >),
                    ),
                    rust_trait: Some(
                        RustTrait {
                            params: vec![
                                ("loc", quote!(crate::langhelp::QExprId)),
                                ("code", quote!(crate::langhelp::Code)),
                                ("type_", quote!(std:: rc:: Rc < dyn crate:: langhelp:: Type >)),
                            ],
                        },
                    ),
                },
                MethodSpec {
                    name: "if_",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >),
                    ),
                    rust_trait: Some(
                        RustTrait {
                            params: vec![
                                ("xx", quote!(&crate::langhelp::ExprContext)),
                                ("loc", quote!(crate::langhelp::QExprId)),
                                (
                                    "conditions",
                                    quote!(Vec <(crate::langhelp::CodeValue, &crate::eval::expr::Expr) >),
                                ),
                                ("else_", quote!(Option <& crate:: eval:: expr:: Expr >)),
                            ],
                        },
                    ),
                },
                MethodSpec {
                    name: "match_",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >),
                    ),
                    rust_trait: Some(
                        RustTrait {
                            params: vec![
                                ("xx", quote!(&crate::langhelp::ExprContext)),
                                ("loc", quote!(crate::langhelp::QExprId)),
                                ("base", quote!(crate::langhelp::CodeValue)),
                                ("element_key", quote!(crate::langhelp::ConstKey)),
                                (
                                    "cases",
                                    quote!(
                                        Vec <(
                                            String,
                                            std:: rc:: Rc < dyn crate:: langhelp:: Type >,
                                            &crate::eval::expr::Expr
                                        ) >
                                    ),
                                ),
                                ("fallback", quote!(Option <& crate:: eval:: expr:: Expr >)),
                            ],
                        },
                    ),
                },
                MethodSpec {
                    name: "maybe_bind_sub",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < Option < crate:: eval:: TargetBindSub >>),
                    ),
                    rust_trait: Some(
                        RustTrait {
                            params: vec![
                                ("xx", quote!(&crate::langhelp::ExprContext)),
                                ("loc", quote!(crate::langhelp::QExprId)),
                                ("cv", quote!(crate::langhelp::CodeValue)),
                            ],
                        },
                    ),
                },
                MethodSpec {
                    name: "stage",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >),
                    ),
                    rust_trait: Some(
                        RustTrait {
                            params: vec![
                                ("xx", quote!(&crate::langhelp::ExprContext)),
                                ("loc", quote!(crate::langhelp::QExprId)),
                                ("tree", quote!(crate::eval::expr::Expr)),
                                ("lower_values", quote!(Vec < crate:: langhelp:: CodeValue >)),
                            ],
                        },
                    ),
                },
            ],
        },
        InterfaceSpec {
            name: "code_dyn",
            builtin_unique_id: 2007,
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
            let arg = &m.arg_type.mortar;
            let ret = &m.ret_type.mortar;
            quote!{
                crate:: eval:: value:: type_source_interface:: SourceInterfaceMethodData {
                    name: #name.to_string(),
                    arg_type: std:: cell:: RefCell:: new(Some(#arg)),
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
        let builtin_unique_id = iface.builtin_unique_id;
        iface_build_entries.push(quote!{
            let #f = dumpster:: unsync:: Gc:: new(
                crate:: eval:: value:: type_source_interface:: SourceInterfaceData {
                    builtin_kind: Some(
                        crate:: eval:: value:: type_source_interface:: BuiltinInterfaceKind::#kind,
                    ),
                    set: builtin_def_set.clone(),
                    description: std:: cell:: RefCell:: new(Some(#description.to_string())),
                    is_aspect: std:: cell:: Cell:: new(false),
                    methods: std:: cell:: RefCell:: new(#methods_expr),
                    identity: crate:: langhelp:: Unique:: Builtin(#builtin_unique_id),
                },
            );
            builtin_def_set.interfaces.borrow_mut().push(#f.clone());
        });
        iface_result_fields.push(quote!{
            #f: #f
        });
        if let Some(trait_name_str) = iface.rust_trait_name {
            let trait_name = format_ident!("{}", trait_name_str);
            let methods: Vec<TokenStream> = iface.methods.iter().filter_map(|m| {
                let rt = m.rust_trait.as_ref()?;
                let method_name = format_ident!("{}", if m.name == "type" {
                    "type_"
                } else {
                    m.name
                });
                let params: Vec<TokenStream> = rt.params.iter().map(|(n, ty)| {
                    let name = format_ident!("{}", n);
                    quote!{
                        #name: #ty
                    }
                }).collect();
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
