use {
    crate::buildsrc::util::{
        InterfaceSpec,
        Method,
        MethodArgs,
        MethodSig,
        TypeRef,
    },
    quote::quote,
};

pub fn specs() -> Vec<InterfaceSpec> {
    let any = || TypeRef::unit(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic));
    let empty_tuple = || TypeRef {
        path: quote!(crate::eval::value::tuple::Tuple),
        dynamic_type: quote!{
            std::rc::Rc::new(crate::eval::value::tuple::Tuple { elements: vec![] }) as
                std::rc::Rc<dyn crate::langhelp::Type>
        },
    };
    let string = || TypeRef::unit(quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic));
    let bool_ = || TypeRef::unit(quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic));
    let aspect = || TypeRef::unit(quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic));
    let cve = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic));
    vec![
        //. .
        InterfaceSpec {
            name: "value",
            builtin_unique_id: 2001,
            scope_id: 3001,
            field_prefix: "v",
            kind: "Value",
            description: None,
            register_in_scope: false,
            rust_trait_name: None,
            methods: vec![
                //. .
                Method {
                    name: "type",
                    ret_type: any(),
                    args: MethodArgs::Source(empty_tuple()),
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
            register_in_scope: false,
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
                    args: MethodArgs::Source(empty_tuple()),
                },
                Method {
                    name: "aspect",
                    ret_type: {
                        let inner_dynamic_type = &aspect().dynamic_type;
                        TypeRef {
                            path: quote!(crate::eval::value::type_option::dynamic::TypeOptionDynamic),
                            dynamic_type: quote!{
                                std:: rc:: Rc:: new(crate:: eval:: value:: type_option:: dynamic:: TypeOptionDynamic {
                                    element_type: #inner_dynamic_type,
                                }) as std:: rc:: Rc < dyn crate:: langhelp:: Type >
                            },
                        }
                    },
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
            register_in_scope: true,
            rust_trait_name: Some("ScopeEntry"),
            methods: vec![Method {
                name: "use_",
                ret_type: TypeRef::via_push(quote!(crate::langhelp::CodeValue)),
                args: MethodArgs::Rust(MethodSig {
                    context: vec![
                        ("xx", quote!(&crate::langhelp::ExprContext)),
                        ("loc", quote!(crate::langhelp::QExprId)),
                    ],
                    data: vec![("code", TypeRef::via_push(quote!(crate::langhelp::Code)))],
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
            register_in_scope: true,
            rust_trait_name: Some("Target"),
            methods: vec![
                //. .
                Method {
                    name: "bind",
                    ret_type: TypeRef::via_push(quote!((crate::langhelp::Code, crate::langhelp::ScopeEntryId))),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                        ],
                        data: vec![("sub", TypeRef::via_push(quote!(crate::eval::TargetBindSub)))],
                    }),
                },
                Method {
                    name: "bind_sub_record",
                    ret_type: TypeRef::via_push(quote!(crate::eval::TargetBindSub)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![
                            ("carry", TypeRef::via_push(quote!(crate::langhelp::Code))),
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
                    ret_type: TypeRef::via_push(quote!(crate::eval::TargetBindSub)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![
                            ("carry", TypeRef::via_push(quote!(crate::langhelp::Code))),
                            ("elements", TypeRef::opaque(quote!(Vec < crate:: eval:: TargetBindSub >))),
                        ],
                    }),
                },
                Method {
                    name: "seq",
                    ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: Code >)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![("entries", TypeRef::opaque(quote!(Vec < crate:: langhelp:: Code >)))],
                    }),
                },
                Method {
                    name: "code_to_bind_sub",
                    ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: eval:: TargetBindSub >)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![("loc", quote!(crate::langhelp::QExprId))],
                        data: vec![
                            ("code", TypeRef::via_push(quote!(crate::langhelp::Code))),
                            ("type_", TypeRef::opaque(quote!(std:: rc:: Rc < dyn crate:: langhelp:: Type >))),
                        ],
                    }),
                },
                Method {
                    name: "if_",
                    ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >)),
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
                    ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
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
                },
                Method {
                    name: "maybe_bind_sub",
                    ret_type: TypeRef::via_push(
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
                    ret_type: TypeRef::via_push(quote!(crate:: langhelp:: EResult < crate:: langhelp:: CodeValue >)),
                    args: MethodArgs::Rust(MethodSig {
                        context: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                        ],
                        data: vec![
                            ("tree", TypeRef::via_push(quote!(crate::eval::expr::Expr))),
                            ("lower_values", TypeRef::opaque(quote!(Vec < crate:: langhelp:: CodeValue >))),
                            ("graft_values", TypeRef::opaque(quote!(Vec < crate:: langhelp:: CodeValue >))),
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
            register_in_scope: true,
            rust_trait_name: None,
            methods: vec![],
        },
    ]
}
