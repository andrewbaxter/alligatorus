mod buildsrc;

use {
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::{
        collections::HashSet,
        env,
        fs,
        path::PathBuf,
    },
};

fn arg_mortar_expr(args: &[(&'static str, &'static str)]) -> TokenStream {
    match args.len() {
        0 => quote!{
            std::rc::Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic) as
                std::rc::Rc<dyn crate::langhelp::Type>
        },
        1 => {
            let ty: TokenStream = args[0].1.parse().unwrap();
            quote!{
                <#ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type()
            }
        },
        _ => {
            let elems: Vec<TokenStream> = args.iter().map(|(_, ty)| {
                let ty: TokenStream = ty.parse().unwrap();
                quote!{
                    crate:: eval:: value:: tuple:: TupleElement:: Mortar(
                        <#ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
                    )
                }
            }).collect();
            quote!{
                std::rc::Rc::new(crate::eval::value::tuple::Tuple {
                    base: None,
                    elements: vec![#(#elems), *],
                }) as std::rc::Rc<dyn crate::langhelp::Type>
            }
        },
    }
}

struct DynWrapMethod {
    args: Vec<(&'static str, &'static str)>,
    body: TokenStream,
    name: &'static str,
    ret: &'static str,
}

fn main() {
    println!("cargo:rerun-if-changed=build.rs");
    let mut builtin_unique_ids = HashSet::new();
    let mut out = quote!{
        use std::rc::Rc;
        use std::cell::{
            Cell,
            RefCell
        };
        use std::collections::HashMap;
        use dumpster::unsync::Gc;
        use crate::langhelp::{
            Code,
            CodeValue,
            ConstKey,
            DefSetId,
            ExprContext,
            QExprId,
            Scope,
            ScopeEntryId,
            Unique,
            Value,
        };
        use crate::eval::TargetDynamic;
        use crate::eval::V8EvalContextSlot;
        use crate::eval::V8LogStore;
        use crate::eval::V8UniqueCounter;
        use crate::langhelp::LogEntry;
        use crate::eval::RcScopeEntryWrap;
        use crate::eval::const_eval;
        use crate::eval::definition_set::DefinitionSet;
        use crate::eval::expr::Expr;
        use crate::eval::value::type_source_function::{
            SourceFunctionData,
            TypeSourceFunctionConst
        };
        use crate::eval::value::type_source_tagged_union::{
            SourceTaggedUnionData,
            SourceTaggedUnionRef,
            SourceTaggedUnionVariant,
        };
        use crate::eval::value::type_source_tagged_union_constructor::SourceTaggedUnionConstructorRef;
        use crate::eval::value::type_source_data_type::{
            SourceDataMethodData,
            SourceDataTypeData,
            SourceDataTypeInterfaceImplData,
            SourceDataTypeInterfaceMethodImplData,
            SourceDataTypeRef,
        };
        use crate::eval::value::type_source_data_type_constructor::TypeSourceDataTypeConstructorConst;
        use crate::eval::value::type_source_interface::{
            SourceInterfaceData,
            SourceInterfaceMethodData,
            SourceInterfaceRef,
            TypeSourceInterfaceConst,
        };
        use crate::eval::dynamic_wrap::{
            AnyType,
            StagedExpr
        };
        use crate::eval::dynamic_util::throw_error;
    };
    {
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
                name: "AspectNull",
                builtin_unique_id: 1001,
                methods: vec![],
            },
            AspectSpec {
                name: "AspectRecord",
                builtin_unique_id: 1002,
                methods: vec![],
            },
            AspectSpec {
                name: "AspectNumberDynamic",
                builtin_unique_id: 1003,
                methods: vec![],
            },
            AspectSpec {
                name: "AspectBoolDynamic",
                builtin_unique_id: 1004,
                methods: vec![],
            },
            AspectSpec {
                name: "AspectStringDynamic",
                builtin_unique_id: 1005,
                methods: vec![],
            },
            AspectSpec {
                name: "AspectBoolConst",
                builtin_unique_id: 1006,
                methods: vec![AspectMethod {
                    name: "get",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "bool",
                }],
            },
            AspectSpec {
                name: "AspectNumberConst",
                builtin_unique_id: 1008,
                methods: vec![AspectMethod {
                    name: "get",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "f64",
                }],
            },
            AspectSpec {
                name: "AspectStringConst",
                builtin_unique_id: 1009,
                methods: vec![AspectMethod {
                    name: "get",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "String",
                }],
            },
            AspectSpec {
                name: "AspectType",
                builtin_unique_id: 1010,
                methods: vec![AspectMethod {
                    name: "type_",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                }],
            },
            AspectSpec {
                name: "AspectListDynamic",
                builtin_unique_id: 1011,
                methods: vec![AspectMethod {
                    name: "element_type",
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                }],
            },
            AspectSpec {
                name: "AspectMapDynamic",
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
                name: "AspectTuple",
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
                name: "AspectTaggedUnion",
                builtin_unique_id: 1014,
                methods: vec![AspectMethod {
                    name: "variants",
                    params: vec![],
                    ret: "Vec<(crate::langhelp::ConstKey, String, std::rc::Rc<dyn crate::langhelp::Type>)>",
                }],
            },
            AspectSpec {
                name: "AspectField",
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
                        params: vec![
                            ("xx", "&crate::langhelp::ExprContext"),
                            ("key", "&crate::langhelp::ConstKey"),
                        ],
                        ret: "Option<std::rc::Rc<dyn crate::langhelp::Type>>",
                    },
                ],
            },
            AspectSpec {
                name: "AspectFunction",
                builtin_unique_id: 1016,
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
                name: "AspectSet",
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
                name: "AspectOperatorAdd",
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
                name: "AspectOperatorEq",
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
                name: "AspectOperatorLt",
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
                name: "AspectInitialBind",
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
                name: "AspectBind",
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
                name: "AspectBindDynamic",
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
                name: "AspectConsumeDynamic",
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
                name: "AspectStackValueDynamic",
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
                name: "AspectLiftDynamic",
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
                name: "AspectLowerDynamic",
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
                name: "AspectIterable",
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
                name: "AspectArtifact",
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
                name: "AspectAspect",
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
            let trait_name = format_ident!("{}", a.name);
            let snake = snake_case(a.name);
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
            let missing_msg = format!("Type ({{}}) is missing aspect {}", a.name);
            aspect_token_items.push(quote!{
                #[allow(dead_code)] pub(crate) trait #trait_name: crate:: langhelp:: Aspect {
                    #(#trait_methods) *
                }
                #[allow(dead_code)] pub(crate) fn #id_fn() -> crate:: langhelp:: Unique {
                    crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
                }
                #[
                    allow(dead_code)
                ] pub(
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
                #[
                    allow(dead_code)
                ] pub(
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
    }
    {
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
    {
        let mut local_fns: Vec<TokenStream> = Vec::new();
        for (name, builtin_unique_id) in [
            //. .
            ("expr_context_ref_type_identity", 6001),
            ("code_value_extern_type_identity", 6002),
            ("type_unique_identity", 6003),
            ("interface_const_identity", 6004),
            ("interface_ref_type_identity", 6005),
            ("type_type_identity", 6006),
            ("tagged_union_ref_type_identity", 6007),
            ("type_option_identity", 6008),
            ("type_code_struct_identity", 6009),
            ("type_code_enum_identity", 6010),
            ("type_code_dyn_handle_identity", 6011),
            ("type_aspect_identity", 6012),
            ("expr_type_identity", 6013),
        ] {
            assert!(builtin_unique_ids.insert(builtin_unique_id));
            let name_ident = format_ident!("{}", name);
            local_fns.push(quote!{
                #[allow(dead_code)] pub(crate) fn #name_ident() -> crate:: langhelp:: Unique {
                    crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
                }
            });
        }
        out.extend(quote!{
            #(#local_fns) *
        });
    }
    {
        let mut artifact_pieces: Vec<TokenStream> = Vec::new();
        let mut builtin_artifact_ids = HashSet::new();

        struct ArtifactSpec {
            builtin_artifact_id: usize,
            data_type: Option<&'static str>,
            name: &'static str,
            register: bool,
            type_path: Option<&'static str>,
        }

        for s in &vec![
            //. .
            ArtifactSpec {
                name: "defset",
                builtin_artifact_id: 7001,
                type_path: None,
                data_type: None,
                register: false,
            },
            ArtifactSpec {
                name: "type_record_artifact",
                builtin_artifact_id: 7002,
                type_path: Some("crate::eval::value::record::artifact::TypeRecordArtifact"),
                data_type: Some("serde_json::Value"),
                register: false,
            },
            ArtifactSpec {
                name: "type_tuple_artifact",
                builtin_artifact_id: 7003,
                type_path: Some("crate::eval::value::tuple::artifact::TypeTupleArtifact"),
                data_type: Some("serde_json::Value"),
                register: false,
            },
            ArtifactSpec {
                name: "type_map_artifact",
                builtin_artifact_id: 7004,
                type_path: Some("crate::eval::value::type_map::artifact::TypeMapArtifact"),
                data_type: Some("serde_json::Value"),
                register: false,
            },
            ArtifactSpec {
                name: "type_option_artifact",
                builtin_artifact_id: 7005,
                type_path: Some("crate::eval::value::type_option::artifact::TypeOptionArtifact"),
                data_type: Some("serde_json::Value"),
                register: false,
            },
            ArtifactSpec {
                name: "type_source_interface_const_artifact",
                builtin_artifact_id: 7006,
                type_path: Some(
                    "crate::eval::value::type_source_interface::artifact::TypeSourceInterfaceConstArtifact",
                ),
                data_type: Some("crate::eval::value::type_source_interface::BuiltinInterfaceKind"),
                register: false,
            },
            ArtifactSpec {
                name: "type_type_source_data_type_constructor_dynamic",
                builtin_artifact_id: 7007,
                type_path: Some(
                    "crate::eval::value::type_type_source_data_type_constructor::dynamic::TypeTypeSourceDataTypeConstructorDynamic",
                ),
                data_type: Some("serde_json::Value"),
                register: true,
            },
            ArtifactSpec {
                name: "type_code_value_extern_dynamic",
                builtin_artifact_id: 7008,
                type_path: Some("crate::eval::code_value_extern::TypeCodeValueExternDynamic"),
                data_type: Some("()"),
                register: true,
            },
            ArtifactSpec {
                name: "type_type_source_data_type_dynamic",
                builtin_artifact_id: 7009,
                type_path: Some(
                    "crate::eval::value::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic",
                ),
                data_type: Some("serde_json::Value"),
                register: true,
            },
            ArtifactSpec {
                name: "type_null_const",
                builtin_artifact_id: 7010,
                type_path: Some("crate::eval::value::type_null::r#const::TypeNullConst"),
                data_type: Some("crate::eval::value::type_null::artifact::ArtifactDataNull"),
                register: true,
            },
            ArtifactSpec {
                name: "type_any_const",
                builtin_artifact_id: 7011,
                type_path: Some("crate::eval::value::type_any::r#const::TypeAnyConst"),
                data_type: Some("serde_json::Value"),
                register: true,
            },
            ArtifactSpec {
                name: "type_string_const",
                builtin_artifact_id: 7012,
                type_path: Some("crate::eval::value::type_string::r#const::TypeStringConst"),
                data_type: Some("crate::eval::value::type_string::artifact::ArtifactDataString"),
                register: true,
            },
            ArtifactSpec {
                name: "type_null_dynamic",
                builtin_artifact_id: 7013,
                type_path: Some("crate::eval::value::type_null::dynamic::TypeNullDynamic"),
                data_type: Some("crate::eval::value::type_null::artifact::ArtifactDataNull"),
                register: true,
            },
            ArtifactSpec {
                name: "type_source_tagged_union_ref_dynamic",
                builtin_artifact_id: 7014,
                type_path: Some(
                    "crate::eval::value::type_source_tagged_union::dynamic::TypeSourceTaggedUnionRefDynamic",
                ),
                data_type: Some("serde_json::Value"),
                register: true,
            },
            ArtifactSpec {
                name: "type_source_data_type_ref_dynamic",
                builtin_artifact_id: 7015,
                type_path: Some("crate::eval::value::type_source_data_type::dynamic::TypeSourceDataTypeRefDynamic"),
                data_type: Some("serde_json::Value"),
                register: true,
            },
            ArtifactSpec {
                name: "type_number_const",
                builtin_artifact_id: 7016,
                type_path: Some("crate::eval::value::type_number::r#const::TypeNumberConst"),
                data_type: Some("f64"),
                register: true,
            },
            ArtifactSpec {
                name: "type_type_source_function_dynamic",
                builtin_artifact_id: 7017,
                type_path: Some("crate::eval::value::type_source_function::dynamic::TypeTypeSourceFunctionDynamic"),
                data_type: Some("serde_json::Value"),
                register: true,
            },
            ArtifactSpec {
                name: "type_expr",
                builtin_artifact_id: 7018,
                type_path: Some("crate::eval::value::type_expr::TypeExpr"),
                data_type: Some("serde_json::Value"),
                register: true,
            },
            ArtifactSpec {
                name: "type_string_dynamic",
                builtin_artifact_id: 7019,
                type_path: Some("crate::eval::value::type_string::dynamic::TypeStringDynamic"),
                data_type: Some("crate::eval::value::type_string::artifact::ArtifactDataString"),
                register: true,
            },
            ArtifactSpec {
                name: "type_number_dynamic",
                builtin_artifact_id: 7020,
                type_path: Some("crate::eval::value::type_number::dynamic::TypeNumberDynamic"),
                data_type: Some("f64"),
                register: true,
            },
            ArtifactSpec {
                name: "type_list_artifact",
                builtin_artifact_id: 7021,
                type_path: Some("crate::eval::value::type_list::artifact::TypeListArtifact"),
                data_type: Some("crate::eval::value::type_list::artifact::ArtifactListType"),
                register: false,
            },
        ] {
            let id_fn = format_ident!("{}_artifact_id", s.name);
            let builtin_artifact_id = s.builtin_artifact_id;
            assert!(builtin_artifact_ids.insert(builtin_artifact_id));
            let id_decl = quote!{
                #[allow(dead_code)] pub(crate) fn #id_fn() -> crate:: langhelp:: ArtifactIdBuiltin {
                    crate:: langhelp:: ArtifactIdBuiltin(#builtin_artifact_id)
                }
            };
            let impl_block = match (s.type_path, s.data_type) {
                (Some(tp), Some(dt)) => {
                    let type_path: TokenStream = tp.parse().unwrap();
                    let data_type: TokenStream = dt.parse().unwrap();
                    let register = if s.register {
                        quote!{
                            #[allow(dead_code)]
                            pub(crate) fn register(
                                m:
                                    &mut std
                                    ::collections
                                    ::HashMap<
                                        crate::langhelp::ArtifactIdBuiltin,
                                        std::rc::Rc<dyn crate::langhelp::Type>,
                                    >,
                            ) {
                                m.insert(Self::artifact_id(), std::rc::Rc::new(Self));
                            }
                        }
                    } else {
                        quote!{
                        }
                    };
                    quote!{
                        impl #type_path {
                            #[allow(dead_code)] pub(crate) fn artifact_id() -> crate:: langhelp:: ArtifactIdBuiltin {
                                #id_fn()
                            }
                            #[
                                allow(dead_code)
                            ] pub(crate) fn build_artifact(data: #data_type) -> crate:: langhelp:: Artifact {
                                crate::langhelp::Artifact {
                                    type_: crate::langhelp::ArtifactId::Builtin(Self::artifact_id()),
                                    data: serde_json::to_value(data).unwrap(),
                                }
                            }
                            #[allow(dead_code)]
                            pub(crate) fn build_type_artifact() -> crate::langhelp::Artifact {
                                crate::langhelp::Artifact {
                                    type_: crate::langhelp::ArtifactId::Builtin(Self::artifact_id()),
                                    data: serde_json::Value::Null,
                                }
                            }
                            #register
                        }
                    }
                },
                _ => quote!{
                },
            };
            artifact_pieces.push(quote!{
                #id_decl #impl_block
            });
        }
        out.extend(quote!{
            #(#artifact_pieces) *
        });
    }
    let fmt = |toks: TokenStream| -> String {
        toks.to_string()
    };
    {
        struct DynWrapField {
            body: TokenStream,
            name: &'static str,
            ret: &'static str,
        }

        struct DynWrapStructSpec {
            builtin_unique_id: usize,
            data_fields: Vec<(&'static str, &'static str)>,
            fields: Vec<DynWrapField>,
            methods: Vec<DynWrapMethod>,
            name: &'static str,
        }

        for s in &vec![
            //. .
            DynWrapStructSpec {
                name: "OptionV8",
                builtin_unique_id: 3001,
                data_fields: vec![("is_some_inner", "bool")],
                fields: vec![],
                methods: vec![DynWrapMethod {
                    name: "is_some_",
                    args: vec![],
                    ret: "bool",
                    body: quote!{
                        self_.is_some_inner
                    },
                }],
            },
            DynWrapStructSpec {
                name: "TypeRefV8",
                builtin_unique_id: 3002,
                data_fields: vec![("type_ref", "Rc<dyn crate::langhelp::Type>")],
                fields: vec![],
                methods: vec![DynWrapMethod {
                    name: "aspect",
                    args: vec![("unique", "Unique")],
                    ret: "OptionV8",
                    body: quote!{
                        let ex_slot =
                            scope
                                .get_slot::<crate::eval::V8EvalContextSlot>()
                                .expect("TypeRefV8.aspect: V8EvalContextSlot not set");
                        let import_state_id = ex_slot.ex.ix.import_state;
                        let xx = Rc::new(ExprContext {
                            vx: ex_slot.ex.clone(),
                            import_state: Rc::new(Cell::new(import_state_id)),
                            scope: Rc::new(Scope {
                                values: RefCell::new(HashMap::new()),
                                parent: None,
                            }),
                            target: Rc::new(crate::eval::TargetDynamic),
                        });
                        let is_some = self_.type_ref.aspect(&xx, &unique).is_some();
                        OptionV8 { is_some_inner: is_some }
                    },
                }],
            },
            DynWrapStructSpec {
                name: "SourceNewFuncResult",
                builtin_unique_id: 3003,
                data_fields: vec![("func_ref", "Rc<TypeSourceFunctionConst>")],
                fields: vec![DynWrapField {
                    name: "func",
                    ret: "Rc<TypeSourceFunctionConst>",
                    body: quote!{
                        self_.func_ref.clone()
                    },
                }],
                //. .
                methods: vec![DynWrapMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote!{
                        *self_.func_ref.data.description.borrow_mut() = Some(description);
                    },
                }, DynWrapMethod {
                    name: "impl_",
                    args: vec![("staged", "StagedExpr")],
                    ret: "()",
                    body: quote!{
                        let mut impl_expr = self_.func_ref.data.impl_expr.borrow_mut();
                        if impl_expr.is_some() {
                            drop(impl_expr);
                            throw_error(scope, "Function already has an implementation");
                            return;
                        }
                        *impl_expr = Some(staged.0);
                    },
                }, DynWrapMethod {
                    name: "arg_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            throw_error(scope, "Function already has an arg type");
                            return;
                        }
                        *arg_type = Some(type_ref.0);
                    },
                }, DynWrapMethod {
                    name: "arg_type",
                    args: vec![("iface_ref", "Rc<SourceInterfaceRef>")],
                    ret: "()",
                    body: quote!{
                        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            throw_error(scope, "Function already has an arg type");
                            return;
                        }
                        *arg_type =
                            Some(
                                Rc::new(TypeSourceInterfaceConst { data: iface_ref.data.clone() },) as
                                    Rc<dyn crate::langhelp::Type>
                            );
                    },
                }, DynWrapMethod {
                    name: "ret_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote!{
                        let mut ret_type = self_.func_ref.data.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            throw_error(scope, "Function already has a ret type");
                            return;
                        }
                        *ret_type = Some(type_ref.0);
                    },
                }],
            },
            DynWrapStructSpec {
                name: "SourceNewDataMethodResult",
                builtin_unique_id: 3004,
                data_fields: vec![("data_type_ref", "Rc<SourceDataTypeRef>"), ("method_index", "usize")],
                fields: vec![],
                methods: vec![
                    //. .
                    DynWrapMethod {
                        name: "impl_",
                        args: vec![("staged", "StagedExpr")],
                        ret: "()",
                        body: quote!{
                            let methods = self_.data_type_ref.data.methods.borrow();
                            let method = &methods[self_.method_index];
                            let mut impl_expr = method.impl_expr.borrow_mut();
                            if impl_expr.is_some() {
                                drop(impl_expr);
                                drop(methods);
                                throw_error(scope, "Method already has an implementation");
                                return;
                            }
                            *impl_expr = Some(staged.0);
                        },
                    },
                    DynWrapMethod {
                        name: "ret_",
                        args: vec![("type_ref", "AnyType")],
                        ret: "()",
                        body: quote!{
                            let methods = self_.data_type_ref.data.methods.borrow();
                            let method = &methods[self_.method_index];
                            let mut ret_type = method.ret_type.borrow_mut();
                            if ret_type.is_some() {
                                drop(ret_type);
                                drop(methods);
                                throw_error(scope, "Method already has a ret type");
                                return;
                            }
                            *ret_type = Some(type_ref.0);
                        },
                    },
                    DynWrapMethod {
                        name: "arg_",
                        args: vec![("type_ref", "AnyType")],
                        ret: "()",
                        body: quote!{
                            let methods = self_.data_type_ref.data.methods.borrow();
                            let method = &methods[self_.method_index];
                            let mut arg_type = method.arg_type.borrow_mut();
                            if arg_type.is_some() {
                                drop(arg_type);
                                drop(methods);
                                throw_error(scope, "Method already has an arg type");
                                return;
                            }
                            *arg_type = Some(type_ref.0);
                        },
                    },
                ],
            },
            DynWrapStructSpec {
                name: "SourceNewInterfaceResult",
                builtin_unique_id: 3005,
                data_fields: vec![("interface_ref", "Rc<SourceInterfaceRef>")],
                fields: vec![DynWrapField {
                    name: "type_",
                    ret: "Rc<SourceInterfaceRef>",
                    body: quote!{
                        self_.interface_ref.clone()
                    },
                }],
                methods: vec![
                    //. .
                    DynWrapMethod {
                        name: "set_description",
                        args: vec![("description", "String")],
                        ret: "()",
                        body: quote!{
                            *self_.interface_ref.data.description.borrow_mut() = Some(description);
                        },
                    },
                    DynWrapMethod {
                        name: "new_func",
                        args: vec![("name", "String")],
                        ret: "SourceNewInterfaceMethodResult",
                        body: quote!{
                            let iface_data = &self_.interface_ref.data;
                            let field_name =
                                format!(
                                    "_im{}",
                                    crate::eval::definition_set::next_interface_method_id(&iface_data.set)
                                );
                            let method_index = {
                                let mut methods = iface_data.methods.borrow_mut();
                                let idx = methods.len();
                                methods.push(SourceInterfaceMethodData {
                                    name,
                                    arg_type: RefCell::new(None),
                                    ret_type: RefCell::new(None),
                                    field_name,
                                });
                                idx
                            };
                            SourceNewInterfaceMethodResult {
                                interface_ref: self_.interface_ref.clone(),
                                method_index,
                            }
                        },
                    },
                    DynWrapMethod {
                        name: "set_is_aspect",
                        args: vec![("is_aspect", "bool")],
                        ret: "()",
                        body: quote!{
                            self_.interface_ref.data.is_aspect.set(is_aspect);
                        },
                    },
                ],
            },
            DynWrapStructSpec {
                name: "SourceNewDataResult",
                builtin_unique_id: 3006,
                data_fields: vec![("data_type_ref", "Rc<SourceDataTypeRef>")],
                fields: vec![
                    //. .
                    DynWrapField {
                        name: "type_",
                        ret: "Rc<SourceDataTypeRef>",
                        body: quote!{
                            self_.data_type_ref.clone()
                        },
                    },
                    DynWrapField {
                        name: "new_",
                        ret: "Rc<TypeSourceDataTypeConstructorConst>",
                        body: quote!{
                            Rc::new(TypeSourceDataTypeConstructorConst { data: self_.data_type_ref.data.clone(), })
                        },
                    },
                ],
                methods: vec![
                    //. .
                    DynWrapMethod {
                        name: "set_description",
                        args: vec![("description", "String")],
                        ret: "()",
                        body: quote!{
                            *self_.data_type_ref.data.description.borrow_mut() = Some(description);
                        },
                    },
                    DynWrapMethod {
                        name: "set_underlying",
                        args: vec![("type_ref", "AnyType")],
                        ret: "()",
                        body: quote!{
                            self_.data_type_ref.data.underlying_type.set(type_ref.0);
                        },
                    },
                    DynWrapMethod {
                        name: "new_func",
                        args: vec![("name", "String")],
                        ret: "SourceNewDataMethodResult",
                        body: quote!{
                            let dt_data = &self_.data_type_ref.data;
                            let field_name = format!("_{}", dt_data.set.next_field_id.next());
                            let entry_id = dt_data.set.next_entry_id.next();
                            let method_index = {
                                let mut methods = dt_data.methods.borrow_mut();
                                let idx = methods.len();
                                methods.push(SourceDataMethodData {
                                    name,
                                    arg_type: RefCell::new(None),
                                    impl_expr: RefCell::new(None),
                                    ret_type: RefCell::new(None),
                                    scope_entry_id: Cell::new(
                                        Some(
                                            crate::langhelp::ScopeEntryId::Def(
                                                dt_data.set.import_id,
                                                dt_data.set.def_set_id,
                                                entry_id,
                                            ),
                                        )
                                    ),
                                    self_scope_entry_id: Cell::new(None),
                                    field_name,
                                    compiled_js: RefCell::new(None),
                                    captured_externals: RefCell::new(Vec::new()),
                                });
                                idx
                            };
                            SourceNewDataMethodResult {
                                data_type_ref: self_.data_type_ref.clone(),
                                method_index,
                            }
                        },
                    },
                    DynWrapMethod {
                        name: "impl_interface",
                        args: vec![
                            ("iface_ref", "Rc<SourceInterfaceRef>"),
                            (
                                "record",
                                "std::collections::BTreeMap<crate::langhelp::ConstKey, crate::eval::expr::Expr>",
                            ),
                        ],
                        ret: "()",
                        body: quote!{
                            let record_entries = record;
                            let iface_data = &iface_ref.data;
                            let iface_method_names: Vec<(usize, String)> = {
                                let methods = iface_data.methods.borrow();
                                methods.iter().enumerate().map(|(i, m)| (i, m.name.clone())).collect()
                            };
                            let mut method_impls = Vec::new();
                            let dt_data = &self_.data_type_ref.data;
                            for (imethod_idx, imethod_name) in &iface_method_names {
                                let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
                                match record_entries.get(&const_key) {
                                    Some(expr) => {
                                        let entry_id = dt_data.set.next_entry_id.next();
                                        method_impls.push(SourceDataTypeInterfaceMethodImplData {
                                            interface_method_index: *imethod_idx,
                                            impl_expr: RefCell::new(Some(Rc::new(expr.clone()))),
                                            impl_expr_artifact: RefCell::new(None),
                                            compiled_js: RefCell::new(None),
                                            captured_externals: RefCell::new(Vec::new()),
                                            scope_entry_id: Cell::new(
                                                Some(
                                                    crate::langhelp::ScopeEntryId::Def(
                                                        dt_data.set.import_id,
                                                        dt_data.set.def_set_id,
                                                        entry_id,
                                                    ),
                                                )
                                            ),
                                            self_scope_entry_id: Cell::new(None),
                                        });
                                    },
                                    None => {
                                        throw_error(
                                            scope,
                                            &format!("Missing implementation for interface method '{}'", imethod_name),
                                        );
                                        return;
                                    },
                                }
                            }
                            dt_data.interface_impls.borrow_mut().push(SourceDataTypeInterfaceImplData {
                                interface: iface_data.clone(),
                                method_impls,
                            });
                        },
                    },
                ],
            },
            DynWrapStructSpec {
                name: "SourceTaggedUnionBuilder",
                builtin_unique_id: 3007,
                data_fields: vec![("data", "Rc<SourceTaggedUnionData>")],
                fields: vec![DynWrapField {
                    name: "type_",
                    ret: "Rc<SourceTaggedUnionRef>",
                    body: quote!{
                        Rc::new(SourceTaggedUnionRef { data: self_.data.clone() })
                    },
                }],
                methods: vec![DynWrapMethod {
                    name: "variant",
                    args: vec![("tag", "String"), ("type_ref", "AnyType")],
                    ret: "Rc<SourceTaggedUnionConstructorRef>",
                    body: quote!{
                        let variant_index = {
                            let mut variants = self_.data.variants.borrow_mut();
                            let idx = variants.len();
                            let js_tag =
                                format!("{}{}", crate::code_dynamic::TAG_PREFIX, self_.data.next_tag_id.next(),);
                            variants.push(SourceTaggedUnionVariant {
                                js_tag,
                                tag: crate::langhelp::ConstKey::String(tag),
                                type_: type_ref.0,
                            });
                            idx
                        };
                        Rc::new(SourceTaggedUnionConstructorRef {
                            data: self_.data.clone(),
                            variant_index,
                        })
                    },
                }],
            },
            DynWrapStructSpec {
                name: "SourceNewInterfaceMethodResult",
                builtin_unique_id: 3008,
                data_fields: vec![("interface_ref", "Rc<SourceInterfaceRef>"), ("method_index", "usize")],
                fields: vec![],
                methods: vec![
                    //. .
                    DynWrapMethod {
                        name: "arg_",
                        args: vec![("type_ref", "AnyType")],
                        ret: "()",
                        body: quote!{
                            let methods = self_.interface_ref.data.methods.borrow();
                            let method = &methods[self_.method_index];
                            let mut arg_type = method.arg_type.borrow_mut();
                            if arg_type.is_some() {
                                drop(arg_type);
                                drop(methods);
                                throw_error(scope, "Interface method already has an arg type");
                                return;
                            }
                            *arg_type = Some(type_ref.0);
                        },
                    },
                    DynWrapMethod {
                        name: "ret_",
                        args: vec![("type_ref", "AnyType")],
                        ret: "()",
                        body: quote!{
                            let methods = self_.interface_ref.data.methods.borrow();
                            let method = &methods[self_.method_index];
                            let mut ret_type = method.ret_type.borrow_mut();
                            if ret_type.is_some() {
                                drop(ret_type);
                                drop(methods);
                                throw_error(scope, "Interface method already has a ret type");
                                return;
                            }
                            *ret_type = Some(type_ref.0);
                        },
                    },
                ],
            },
        ] {
            let name = format_ident!("{}", s.name);
            let type_name = format_ident!("Type{}Dynamic", s.name);
            let identity_fn = format_ident!("{}_type_identity", snake_case(s.name));
            let builtin_unique_id = s.builtin_unique_id;
            assert!(builtin_unique_ids.insert(builtin_unique_id));
            let desc_str = s.name.to_string();
            let data_field_decls: Vec<TokenStream> = s.data_fields.iter().map(|(n, ty)| {
                let n = format_ident!("{}", n);
                let ty: TokenStream = ty.parse().unwrap();
                quote!{
                    pub #n: #ty
                }
            }).collect();
            let field_dispatch_arms: Vec<TokenStream> = s.fields.iter().enumerate().map(|(idx, f)| {
                let f_name = f.name;
                let f_ty: TokenStream = f.ret.parse().unwrap();
                let idx_str = format!("_{}", idx);
                quote!{
                    if key_name == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#f_name) {
                        let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = <#f_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(
                        );
                        let self_code =
                            erb.must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?.unwrap().clone();
                        return Ok(crate:: langhelp:: CodeValue {
                            code: crate:: langhelp:: Code {
                                loc,
                                code: crate:: langhelp:: Code_:: Dyn(
                                    std:: rc:: Rc:: new(crate:: code_dynamic:: CodeDynamic {
                                        loc,
                                        type_: crate:: code_dynamic:: CodeDynamicType:: FieldUse(
                                            crate:: code_dynamic:: CodeDynamicFieldUse {
                                                base: Box:: new(self_code),
                                                field: #idx_str.to_string(),
                                            },
                                        ),
                                    },)
                                ),
                            },
                            value: std:: rc:: Rc:: new(crate::eval::dynamic_util::ValueDynamic {
                                source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                                type_: field_type,
                            }),
                        });
                    }
                }
            }).collect();
            let method_dispatch_arms: Vec<TokenStream> = s.methods.iter().enumerate().map(|(i, m)| {
                let m_name = m.name;
                let idx = s.fields.len() + i;
                let idx_str = format!("_{}", idx);
                let arg_mortar = arg_mortar_expr(&m.args);
                let m_ret: TokenStream = m.ret.parse().unwrap();
                quote!{
                    if key_name == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#m_name) {
                        let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = std:: rc:: Rc:: new(
                            crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                                arg: #arg_mortar,
                                ret: <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
                            },
                        );
                        let self_code =
                            erb.must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?.unwrap().clone();
                        return Ok(crate:: langhelp:: CodeValue {
                            code: crate:: langhelp:: Code {
                                loc,
                                code: crate:: langhelp:: Code_:: Dyn(
                                    std:: rc:: Rc:: new(crate:: code_dynamic:: CodeDynamic {
                                        loc,
                                        type_: crate:: code_dynamic:: CodeDynamicType:: FieldUse(
                                            crate:: code_dynamic:: CodeDynamicFieldUse {
                                                base: Box:: new(self_code),
                                                field: #idx_str.to_string(),
                                            },
                                        ),
                                    },)
                                ),
                            },
                            value: std:: rc:: Rc:: new(crate::eval::dynamic_util::ValueDynamic {
                                source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                                type_: field_type,
                            }),
                        });
                    }
                }
            }).collect();
            let field_type_opt_arms: Vec<TokenStream> = s.fields.iter().map(|f| {
                let f_name = f.name;
                let f_ty: TokenStream = f.ret.parse().unwrap();
                quote!{
                    if key.as_str() == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#f_name) {
                        return Some(<#f_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type());
                    }
                }
            }).chain(s.methods.iter().map(|m| {
                let m_name = m.name;
                let arg_mortar = arg_mortar_expr(&m.args);
                let m_ret: TokenStream = m.ret.parse().unwrap();
                quote!{
                    if key.as_str() == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#m_name) {
                        return Some(
                            std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                                arg: #arg_mortar,
                                ret: <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
                            },)
                        );
                    }
                }
            })).collect();
            let field_setters: Vec<TokenStream> = s.fields.iter().enumerate().map(|(idx, f)| {
                let f_ty: TokenStream = f.ret.parse().unwrap();
                let body = &f.body;
                let key_str = format!("_{}", idx);
                quote!{
                    {
                        #[allow(unused_variables)]
                        let self_ = _jw_self_rc.clone();
                        let _jw_field_val: #f_ty = {
                            #body
                        };
                        let _jw_v8_val = <#f_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: into_v8(
                            _jw_field_val,
                            scope
                        );
                        let _jw_key = v8:: String:: new(scope, #key_str).unwrap();
                        _jw_obj.set(scope, _jw_key.into(), _jw_v8_val);
                    }
                }
            }).collect();
            let method_setters: Vec<TokenStream> = s.methods.iter().enumerate().map(|(i, m)| {
                let idx = s.fields.len() + i;
                let key_str = format!("_{}", idx);
                let arg_decls: Vec<TokenStream> = m.args.iter().map(|(n, ty)| {
                    let n = format_ident!("{}", n);
                    let ty: TokenStream = ty.parse().unwrap();
                    quote!{
                        let #n: #ty = <#ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: from_v8(
                            scope,
                            _jw_args.get(_jw_arg_i),
                        );
                        _jw_arg_i += 1;
                    }
                }).collect();
                let m_ret: TokenStream = m.ret.parse().unwrap();
                let body = &m.body;
                quote!{
                    {
                        fn _jw_cb <'s >(
                            scope: & mut v8:: PinnedRef <'s,
                            v8:: HandleScope <'_ >>,
                            _jw_args: v8:: FunctionCallbackArguments <'s >,
                            mut _jw_rv: v8:: ReturnValue <'s >,
                        ) {
                            let _jw_this = _jw_args.this();
                            let _jw_dk = v8::String::new(scope, "_d").unwrap();
                            #[
                                allow(unused_variables)
                            ] let self_: std:: rc:: Rc <#name > = crate:: eval:: from_v8_ref::< std:: rc:: Rc <#name >>(
                                &*_jw_this.get(scope, _jw_dk.into()).unwrap(),
                            ).clone();
                            #[allow(unused_mut, unused_variables)]
                            let mut _jw_arg_i = 0i32;
                            #(#arg_decls) * let _jw_result: #m_ret = {
                                #body
                            };
                            _jw_rv.set(
                                <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: into_v8(_jw_result, scope),
                            );
                        }
                        let _jw_method_fn = v8::Function::builder(_jw_cb).build(scope).unwrap();
                        let _jw_key = v8:: String:: new(scope, #key_str).unwrap();
                        _jw_obj.set(scope, _jw_key.into(), _jw_method_fn.into());
                    }
                }
            }).collect();
            let no_fields_msg = format!("Cannot convert {}", s.name);
            let unknown_field_msg = format!("Unknown field '{{}}' on {}", s.name);
            let from_v8_msg = format!("from_v8 not supported for {}", s.name);
            let cannot_convert_to_msg = format!("Cannot convert to {}", s.name);
            out.extend(quote!{
                pub(crate) struct #name {
                    #(#data_field_decls,) *
                }
                pub(crate) struct #type_name;
                fn #identity_fn() -> crate:: langhelp:: Unique {
                    crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
                }
                impl crate:: langhelp:: Value for #type_name {
                    fn type_(
                        &self,
                        _xx: &crate::langhelp::ExprContext,
                        _loc: crate::langhelp::QExprId,
                    ) -> std::rc::Rc<dyn crate::langhelp::Type> {
                        std::rc::Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
                    }
                }
                impl crate:: langhelp:: Aspect for #type_name {
                }
                impl crate:: langhelp:: Type for #type_name {
                    fn desc(&self) -> String {
                        #desc_str.to_string()
                    }
                    fn as_value(&self) -> std:: rc:: Rc < dyn crate:: langhelp:: Value > {
                        std:: rc:: Rc:: new(#type_name)
                    }
                    fn aspect(
                        &self,
                        _xx: & crate:: langhelp:: ExprContext,
                        key: & crate:: langhelp:: Unique,
                    ) -> Option < Box < dyn std:: any:: Any >> {
                        if * key == #identity_fn() {
                            return Some(Box::new(true));
                        }
                        if * key == crate:: r#gen:: aspect_field_aspect_id() {
                            return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectField >));
                        }
                        if * key == crate:: r#gen:: aspect_consume_dynamic_aspect_id() {
                            return Some(
                                Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectConsumeDynamic >)
                            );
                        }
                        if * key == crate:: r#gen:: aspect_bind_dynamic_aspect_id() {
                            return Some(
                                Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBindDynamic >)
                            );
                        }
                        if * key == crate:: r#gen:: aspect_bind_aspect_id() {
                            return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBind >));
                        }
                        if * key == crate:: r#gen:: aspect_stack_value_dynamic_aspect_id() {
                            return Some(
                                Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectStackValueDynamic >)
                            );
                        }
                        None
                    }
                    fn cast_to(
                        &self,
                        _xx: & crate:: langhelp:: ExprContext,
                        loc: crate:: langhelp:: QExprId,
                        _cv: crate:: langhelp:: CodeValue,
                        _type_: & dyn crate:: langhelp:: Type,
                    ) -> crate:: langhelp:: EvalRes {
                        crate:: langhelp:: EvalRes:: err(loc, #no_fields_msg.to_string())
                    }
                    fn can_cast_from(
                        &self,
                        xx: & crate:: langhelp:: ExprContext,
                        _loc: crate:: langhelp:: QExprId,
                        source_type: & dyn crate:: langhelp:: Type,
                    ) -> bool {
                        source_type.aspect(xx, &#identity_fn()).is_some()
                    }
                    fn cast_from(
                        &self,
                        xx: & crate:: langhelp:: ExprContext,
                        loc: crate:: langhelp:: QExprId,
                        cv: crate:: langhelp:: CodeValue,
                    ) -> crate:: langhelp:: EvalRes {
                        let source_type = cv.value.type_(xx, loc);
                        if source_type.aspect(xx, &#identity_fn()).is_some() {
                            return crate:: langhelp:: EvalRes:: ok(crate:: langhelp:: CodeValue {
                                code: cv.code,
                                value: std:: rc:: Rc:: new(crate:: eval:: dynamic_util:: ValueDynamic {
                                    source: crate:: eval:: dynamic_util:: ValueDynamicSource:: Expr,
                                    type_: std:: rc:: Rc:: new(#type_name),
                                }),
                            });
                        }
                        crate:: langhelp:: EvalRes:: err(loc, #cannot_convert_to_msg.to_string())
                    }
                }
                impl crate:: r#gen:: AspectConsumeDynamic for #type_name {
                    fn to_dynamic(
                        &self,
                        _xx: &crate::langhelp::ExprContext,
                        _loc: crate::langhelp::QExprId,
                        cv: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EResult<crate::code_dynamic::CodeDynamic> {
                        cv.code.cast::<crate::code_dynamic::CodeDynamic>().map(|x| x.unwrap().clone())
                    }
                }
                impl crate:: r#gen:: AspectBindDynamic for #type_name {
                    fn bind_dynamic(
                        &self,
                        xx: &crate::langhelp::ExprContext,
                        loc: crate::langhelp::QExprId,
                        cv: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EResult<Option<crate::eval::TargetBindSub>> {
                        crate::eval::dynamic_util::bind_dynamic_default(xx, loc, cv)
                    }
                }
                impl crate:: r#gen:: AspectBind for #type_name {
                    fn bind(
                        &self,
                        xx: &crate::langhelp::ExprContext,
                        loc: crate::langhelp::QExprId,
                        cv: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)> {
                        crate::eval::dynamic_util::bind_via_target(xx, loc, cv)
                    }
                }
                impl crate:: r#gen:: AspectStackValueDynamic for #type_name {
                    fn to_value(
                        &self,
                        loc: crate:: langhelp:: QExprId,
                        value: crate:: code_dynamic:: CodeDynamic,
                    ) -> crate:: langhelp:: CodeValue {
                        crate:: langhelp:: CodeValue {
                            code: crate:: langhelp:: Code {
                                loc,
                                code: crate:: langhelp:: Code_:: Dyn(std::rc::Rc::new(value)),
                            },
                            value: std:: rc:: Rc:: new(crate:: eval:: dynamic_util:: ValueDynamic {
                                source: crate:: eval:: dynamic_util:: ValueDynamicSource:: Expr,
                                type_: std:: rc:: Rc:: new(#type_name),
                            }),
                        }
                    }
                }
                impl crate:: r#gen:: AspectField for #type_name {
                    fn field(
                        &self,
                        xx: &crate::langhelp::ExprContext,
                        loc: crate::langhelp::QExprId,
                        self_: crate::langhelp::CodeValue,
                        key: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EvalRes {
                        crate::er_seq_cv!(loc, erb, {
                            let key_type = key.value.type_(xx, loc);
                            let key_string =
                                erb.must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
                            let key_name = key_string.get(key.value.as_ref());
                            #(
                                #field_dispatch_arms
                            ) * #(#method_dispatch_arms) * return erb.err_new(format!(#unknown_field_msg, key_name));
                        })
                    }
                    fn field_type_opt(
                        &self,
                        _xx: & crate:: langhelp:: ExprContext,
                        key: & crate:: langhelp:: ConstKey,
                    ) -> Option < std:: rc:: Rc < dyn crate:: langhelp:: Type >> {
                        let crate::langhelp::ConstKey::String(key) = key else {
                            return None;
                        };
                        #(#field_type_opt_arms) * None
                    }
                }
                impl crate:: eval:: dynamic_wrap:: DynamicWrap for #name {
                    fn mortar_type() -> std:: rc:: Rc < dyn crate:: langhelp:: Type > {
                        std:: rc:: Rc:: new(#type_name)
                    }
                    fn from_v8<
                        's
                    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>,) -> Self {
                        unimplemented!(#from_v8_msg)
                    }
                    fn into_v8 <'s >(
                        self,
                        scope: & mut v8:: PinnedRef <'s,
                        v8:: HandleScope <'_ >>,
                    ) -> v8:: Local <'s,
                    v8:: Value > {
                        let _jw_obj = v8::Object::new(scope);
                        let _jw_self_rc = std::rc::Rc::new(self);
                        let _jw_data_ext = crate::eval::to_v8(scope, _jw_self_rc.clone());
                        _jw_obj.set(scope, v8::String::new(scope, "_d").unwrap().into(), _jw_data_ext.into(),);
                        #(#field_setters) * #(#method_setters) * _jw_obj.into()
                    }
                }
            });
        }
    }
    {
        struct DynWrapWrapSpec {
            builtin_unique_id: usize,
            methods: Vec<DynWrapMethod>,
            name: &'static str,
            wrapped_type: &'static str,
        }

        for s in &vec![
            //. .
            DynWrapWrapSpec {
                name: "DefinitionSet",
                builtin_unique_id: 4001,
                wrapped_type: "Gc<DefinitionSet>",
                methods: vec![
                    //. .
                    DynWrapMethod {
                        name: "new_func",
                        args: vec![("name", "String")],
                        ret: "SourceNewFuncResult",
                        body: quote!{
                            let ds = (*self_).clone();
                            let entry_id = ds.next_entry_id.next();
                            let func_data = Gc::new(SourceFunctionData {
                                set: ds.clone(),
                                name: Some(name),
                                description: RefCell::new(None),
                                impl_expr: RefCell::new(None),
                                arg_type: RefCell::new(None),
                                ret_type: RefCell::new(None),
                                scope_entry_id: Cell::new(
                                    Some(crate::langhelp::ScopeEntryId::Def(ds.import_id, ds.def_set_id, entry_id,),)
                                ),
                                artifact_id: RefCell::new(None),
                                compiled_js: RefCell::new(None),
                                captured_externals: RefCell::new(Vec::new()),
                            });
                            ds.functions.borrow_mut().push(func_data.clone());
                            let func_ref = Rc::new(TypeSourceFunctionConst { data: func_data });
                            SourceNewFuncResult { func_ref }
                        },
                    },
                    DynWrapMethod {
                        name: "new_data",
                        args: vec![("name", "String")],
                        ret: "SourceNewDataResult",
                        body: quote!{
                            let ds = (*self_).clone();
                            let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                            let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                            let data_field = format!("_{}", ds.next_field_id.next());
                            let dt_data = Gc::new(SourceDataTypeData {
                                set: ds.clone(),
                                name: Some(name),
                                description: RefCell::new(None),
                                underlying_type: crate::invariant::DeserialLazy::new(),
                                methods: RefCell::new(Vec::new()),
                                interface_impls: RefCell::new(Vec::new()),
                                identity,
                                data_field,
                                artifact_id: RefCell::new(None),
                            });
                            ds.data_types.borrow_mut().push(dt_data.clone());
                            let data_type_ref = Rc::new(SourceDataTypeRef { data: dt_data });
                            SourceNewDataResult { data_type_ref }
                        },
                    },
                    DynWrapMethod {
                        name: "build",
                        args: vec![],
                        ret: "Gc<DefinitionSet>",
                        body: quote!{
                            (*self_).clone()
                        },
                    },
                    DynWrapMethod {
                        name: "tagged_union",
                        args: vec![],
                        ret: "SourceTaggedUnionBuilder",
                        body: quote!{
                            let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                            let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                            let data = Rc::new(SourceTaggedUnionData {
                                variants: RefCell::new(Vec::new()),
                                identity,
                                next_tag_id: crate::langhelp::Counter::default(),
                            });
                            SourceTaggedUnionBuilder { data }
                        },
                    },
                    DynWrapMethod {
                        name: "new_interface",
                        args: vec![],
                        ret: "SourceNewInterfaceResult",
                        body: quote!{
                            let ds = (*self_).clone();
                            let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                            let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                            let iface_data = Gc::new(SourceInterfaceData {
                                builtin_kind: None,
                                set: ds.clone(),
                                description: RefCell::new(None),
                                is_aspect: Cell::new(false),
                                methods: RefCell::new(Vec::new()),
                                identity,
                            });
                            ds.interfaces.borrow_mut().push(iface_data.clone());
                            let interface_ref = Rc::new(SourceInterfaceRef { data: iface_data });
                            SourceNewInterfaceResult { interface_ref }
                        },
                    },
                ],
            },
        ] {
            let type_name = format_ident!("Type{}Dynamic", s.name);
            let identity_fn = format_ident!("{}_type_identity", snake_case(s.name));
            let extra_aspect_fn = format_ident!("{}_extra_aspect", snake_case(s.name));
            let builtin_unique_id = s.builtin_unique_id;
            assert!(builtin_unique_ids.insert(builtin_unique_id));
            let desc_str = s.name.to_string();
            let wrapped: TokenStream = s.wrapped_type.parse().unwrap();
            let method_dispatch_arms: Vec<TokenStream> = s.methods.iter().enumerate().map(|(idx, m)| {
                let m_name = m.name;
                let idx_str = format!("_{}", idx);
                let arg_mortar = arg_mortar_expr(&m.args);
                let m_ret: TokenStream = m.ret.parse().unwrap();
                quote!{
                    if key_name == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#m_name) {
                        let field_type: std:: rc:: Rc < dyn crate:: langhelp:: Type > = std:: rc:: Rc:: new(
                            crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                                arg: #arg_mortar,
                                ret: <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
                            },
                        );
                        let self_code =
                            erb.must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?.unwrap().clone();
                        return Ok(crate:: langhelp:: CodeValue {
                            code: crate:: langhelp:: Code {
                                loc,
                                code: crate:: langhelp:: Code_:: Dyn(
                                    std:: rc:: Rc:: new(crate:: code_dynamic:: CodeDynamic {
                                        loc,
                                        type_: crate:: code_dynamic:: CodeDynamicType:: FieldUse(
                                            crate:: code_dynamic:: CodeDynamicFieldUse {
                                                base: Box:: new(self_code),
                                                field: #idx_str.to_string(),
                                            },
                                        ),
                                    },)
                                ),
                            },
                            value: std:: rc:: Rc:: new(crate::eval::dynamic_util::ValueDynamic {
                                source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                                type_: field_type,
                            }),
                        });
                    }
                }
            }).collect();
            let field_type_opt_arms: Vec<TokenStream> = s.methods.iter().map(|m| {
                let m_name = m.name;
                let arg_mortar = arg_mortar_expr(&m.args);
                let m_ret: TokenStream = m.ret.parse().unwrap();
                quote!{
                    if key.as_str() == crate:: eval:: dynamic_wrap:: dynamic_wrap_method_name(#m_name) {
                        return Some(
                            std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                                arg: #arg_mortar,
                                ret: <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
                            },)
                        );
                    }
                }
            }).collect();
            let method_setters: Vec<TokenStream> = s.methods.iter().enumerate().map(|(idx, m)| {
                let key_str = format!("_{}", idx);
                let arg_decls: Vec<TokenStream> = m.args.iter().map(|(n, ty)| {
                    let n = format_ident!("{}", n);
                    let ty: TokenStream = ty.parse().unwrap();
                    quote!{
                        let #n: #ty = <#ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: from_v8(
                            scope,
                            _jw_args.get(_jw_arg_i),
                        );
                        _jw_arg_i += 1;
                    }
                }).collect();
                let m_ret: TokenStream = m.ret.parse().unwrap();
                let body = &m.body;
                quote!{
                    {
                        fn _jw_cb <'s >(
                            scope: & mut v8:: PinnedRef <'s,
                            v8:: HandleScope <'_ >>,
                            _jw_args: v8:: FunctionCallbackArguments <'s >,
                            mut _jw_rv: v8:: ReturnValue <'s >,
                        ) {
                            let _jw_this = _jw_args.this();
                            let _jw_dk = v8::String::new(scope, "_d").unwrap();
                            #[
                                allow(unused_variables)
                            ] let self_: std:: rc:: Rc <#wrapped > = crate:: eval:: from_v8_ref::< std:: rc:: Rc <#wrapped >>(
                                &*_jw_this.get(scope, _jw_dk.into()).unwrap(),
                            ).clone();
                            #[allow(unused_mut, unused_variables)]
                            let mut _jw_arg_i = 0i32;
                            #(#arg_decls) * let _jw_result: #m_ret = {
                                #body
                            };
                            _jw_rv.set(
                                <#m_ret as crate:: eval:: dynamic_wrap:: DynamicWrap >:: into_v8(_jw_result, scope),
                            );
                        }
                        let _jw_method_fn = v8::Function::builder(_jw_cb).build(scope).unwrap();
                        let _jw_key = v8:: String:: new(scope, #key_str).unwrap();
                        _jw_obj.set(scope, _jw_key.into(), _jw_method_fn.into());
                    }
                }
            }).collect();
            let cannot_convert_msg = format!("Cannot convert {}", s.name);
            let cannot_convert_to_msg = format!("Cannot convert to {}", s.name);
            let unknown_field_msg = format!("Unknown field '{{}}' on {}", s.name);
            let from_v8_msg = format!("from_v8 not supported for {}", s.name);
            out.extend(quote!{
                pub(crate) struct #type_name;
                fn #identity_fn() -> crate:: langhelp:: Unique {
                    crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
                }
                impl crate:: langhelp:: Value for #type_name {
                    fn type_(
                        &self,
                        _xx: &crate::langhelp::ExprContext,
                        _loc: crate::langhelp::QExprId,
                    ) -> std::rc::Rc<dyn crate::langhelp::Type> {
                        std::rc::Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
                    }
                }
                impl crate:: langhelp:: Aspect for #type_name {
                }
                impl crate:: langhelp:: Type for #type_name {
                    fn desc(&self) -> String {
                        #desc_str.to_string()
                    }
                    fn as_value(&self) -> std:: rc:: Rc < dyn crate:: langhelp:: Value > {
                        std:: rc:: Rc:: new(#type_name)
                    }
                    fn aspect(
                        &self,
                        _xx: & crate:: langhelp:: ExprContext,
                        key: & crate:: langhelp:: Unique,
                    ) -> Option < Box < dyn std:: any:: Any >> {
                        if * key == #identity_fn() {
                            return Some(Box::new(true));
                        }
                        if * key == crate:: r#gen:: aspect_field_aspect_id() {
                            return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectField >));
                        }
                        if * key == crate:: r#gen:: aspect_consume_dynamic_aspect_id() {
                            return Some(
                                Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectConsumeDynamic >)
                            );
                        }
                        if * key == crate:: r#gen:: aspect_bind_dynamic_aspect_id() {
                            return Some(
                                Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBindDynamic >)
                            );
                        }
                        if * key == crate:: r#gen:: aspect_bind_aspect_id() {
                            return Some(Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectBind >));
                        }
                        if * key == crate:: r#gen:: aspect_stack_value_dynamic_aspect_id() {
                            return Some(
                                Box:: new(Box:: new(#type_name) as Box < dyn crate:: r#gen:: AspectStackValueDynamic >)
                            );
                        }
                        crate:: eval:: definition_set::#extra_aspect_fn(_xx, key)
                    }
                    fn cast_to(
                        &self,
                        _xx: & crate:: langhelp:: ExprContext,
                        loc: crate:: langhelp:: QExprId,
                        _cv: crate:: langhelp:: CodeValue,
                        _type_: & dyn crate:: langhelp:: Type,
                    ) -> crate:: langhelp:: EvalRes {
                        crate:: langhelp:: EvalRes:: err(loc, #cannot_convert_msg.to_string())
                    }
                    fn can_cast_from(
                        &self,
                        xx: & crate:: langhelp:: ExprContext,
                        _loc: crate:: langhelp:: QExprId,
                        source_type: & dyn crate:: langhelp:: Type,
                    ) -> bool {
                        source_type.aspect(xx, &#identity_fn()).is_some()
                    }
                    fn cast_from(
                        &self,
                        xx: & crate:: langhelp:: ExprContext,
                        loc: crate:: langhelp:: QExprId,
                        cv: crate:: langhelp:: CodeValue,
                    ) -> crate:: langhelp:: EvalRes {
                        let source_type = cv.value.type_(xx, loc);
                        if source_type.aspect(xx, &#identity_fn()).is_some() {
                            return crate:: langhelp:: EvalRes:: ok(crate:: langhelp:: CodeValue {
                                code: cv.code,
                                value: std:: rc:: Rc:: new(crate:: eval:: dynamic_util:: ValueDynamic {
                                    source: crate:: eval:: dynamic_util:: ValueDynamicSource:: Expr,
                                    type_: std:: rc:: Rc:: new(#type_name),
                                }),
                            });
                        }
                        crate:: langhelp:: EvalRes:: err(loc, #cannot_convert_to_msg.to_string())
                    }
                }
                impl crate:: r#gen:: AspectConsumeDynamic for #type_name {
                    fn to_dynamic(
                        &self,
                        _xx: &crate::langhelp::ExprContext,
                        _loc: crate::langhelp::QExprId,
                        cv: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EResult<crate::code_dynamic::CodeDynamic> {
                        cv.code.cast::<crate::code_dynamic::CodeDynamic>().map(|x| x.unwrap().clone())
                    }
                }
                impl crate:: r#gen:: AspectBindDynamic for #type_name {
                    fn bind_dynamic(
                        &self,
                        xx: &crate::langhelp::ExprContext,
                        loc: crate::langhelp::QExprId,
                        cv: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EResult<Option<crate::eval::TargetBindSub>> {
                        crate::eval::dynamic_util::bind_dynamic_default(xx, loc, cv)
                    }
                }
                impl crate:: r#gen:: AspectBind for #type_name {
                    fn bind(
                        &self,
                        xx: &crate::langhelp::ExprContext,
                        loc: crate::langhelp::QExprId,
                        cv: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)> {
                        crate::eval::dynamic_util::bind_via_target(xx, loc, cv)
                    }
                }
                impl crate:: r#gen:: AspectStackValueDynamic for #type_name {
                    fn to_value(
                        &self,
                        loc: crate:: langhelp:: QExprId,
                        value: crate:: code_dynamic:: CodeDynamic,
                    ) -> crate:: langhelp:: CodeValue {
                        crate:: langhelp:: CodeValue {
                            code: crate:: langhelp:: Code {
                                loc,
                                code: crate:: langhelp:: Code_:: Dyn(std::rc::Rc::new(value)),
                            },
                            value: std:: rc:: Rc:: new(crate:: eval:: dynamic_util:: ValueDynamic {
                                source: crate:: eval:: dynamic_util:: ValueDynamicSource:: Expr,
                                type_: std:: rc:: Rc:: new(#type_name),
                            }),
                        }
                    }
                }
                impl crate:: r#gen:: AspectField for #type_name {
                    fn field(
                        &self,
                        xx: &crate::langhelp::ExprContext,
                        loc: crate::langhelp::QExprId,
                        self_: crate::langhelp::CodeValue,
                        key: crate::langhelp::CodeValue,
                    ) -> crate::langhelp::EvalRes {
                        crate::er_seq_cv!(loc, erb, {
                            let key_type = key.value.type_(xx, loc);
                            let key_string =
                                erb.must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
                            let key_name = key_string.get(key.value.as_ref());
                            #(#method_dispatch_arms) * return erb.err_new(format!(#unknown_field_msg, key_name));
                        })
                    }
                    fn field_type_opt(
                        &self,
                        _xx: & crate:: langhelp:: ExprContext,
                        key: & crate:: langhelp:: ConstKey,
                    ) -> Option < std:: rc:: Rc < dyn crate:: langhelp:: Type >> {
                        let crate::langhelp::ConstKey::String(key) = key else {
                            return None;
                        };
                        #(#field_type_opt_arms) * None
                    }
                }
                impl crate:: eval:: dynamic_wrap:: DynamicWrap for #wrapped {
                    fn mortar_type() -> std:: rc:: Rc < dyn crate:: langhelp:: Type > {
                        std:: rc:: Rc:: new(#type_name)
                    }
                    fn from_v8<
                        's
                    >(_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>, _value: v8::Local<'s, v8::Value>,) -> Self {
                        unimplemented!(#from_v8_msg)
                    }
                    fn into_v8 <'s >(
                        self,
                        scope: & mut v8:: PinnedRef <'s,
                        v8:: HandleScope <'_ >>,
                    ) -> v8:: Local <'s,
                    v8:: Value > {
                        let _jw_obj = v8::Object::new(scope);
                        let _jw_self_rc = std::rc::Rc::new(self);
                        let _jw_data_ext = crate::eval::to_v8(scope, _jw_self_rc.clone());
                        _jw_obj.set(scope, v8::String::new(scope, "_d").unwrap().into(), _jw_data_ext.into(),);
                        #(#method_setters) * _jw_obj.into()
                    }
                }
            });
        }
    }
    {
        let mut dw_fn_call_exprs: Vec<TokenStream> = Vec::new();
        let mut builtin_scope_ids = HashSet::new();

        struct DynWrapFnSpec {
            args: Vec<(&'static str, &'static str)>,
            body: TokenStream,
            builtin_scope_id: usize,
            name: &'static str,
            ret: &'static str,
        }

        for s in &vec![
            //. .
            DynWrapFnSpec {
                name: "log",
                builtin_scope_id: 5001,
                args: vec![("message", "String")],
                ret: "()",
                body: quote!{
                    let timestamp_ms =
                        std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis() as
                            u64;
                    scope.get_slot::<V8LogStore>().unwrap().logs.borrow_mut().push(LogEntry {
                        timestamp_ms,
                        message,
                    });
                },
            },
            DynWrapFnSpec {
                name: "define",
                builtin_scope_id: 5002,
                args: vec![],
                ret: "Gc<DefinitionSet>",
                body: quote!{
                    let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                    let def_set_id = DefSetId(ex_slot.ex.ix.def_set_counter.next());
                    let import_id = ex_slot.ex.ix.import_state;
                    let source_id = ex_slot.ex.ix.source_id;
                    let ds = Gc::new(DefinitionSet::new(import_id, def_set_id, source_id));
                    ex_slot.ex.ix.definition_sets.borrow_mut().push(ds.clone());
                    ds
                },
            },
            DynWrapFnSpec {
                name: "str_to_json",
                builtin_scope_id: 5003,
                args: vec![("s", "String")],
                ret: "String",
                body: quote!{
                    serde_json::to_string(&s).unwrap()
                },
            },
            DynWrapFnSpec {
                name: "constkey_to_str",
                builtin_scope_id: 5004,
                args: vec![("key", "crate::langhelp::ConstKey")],
                ret: "String",
                body: quote!{
                    key.to_str()
                },
            },
            DynWrapFnSpec {
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
            DynWrapFnSpec {
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
            DynWrapFnSpec {
                name: "eval_const",
                builtin_scope_id: 5007,
                args: vec![
                    ("xx", "crate::eval::code_value_extern::ExprContextRef"),
                    ("staged", "crate::eval::dynamic_wrap::StagedExpr"),
                ],
                ret: "crate::eval::code_value_extern::CodeValueExtern",
                body: quote!{
                    let expr_value = staged.0;
                    let expr_ast: Expr =
                        Rc::try_unwrap(crate::langhelp::downcast_value::<Expr>(expr_value))
                            .ok()
                            .expect("eval_const: staged Expr Rc not unique");
                    let res = const_eval(&xx.0, &expr_ast);
                    let cv = res.v.expect("eval_const: const_eval produced no value");
                    crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
                },
            },
            DynWrapFnSpec {
                name: "const_eval",
                builtin_scope_id: 5008,
                args: vec![
                    ("xx", "crate::eval::code_value_extern::ExprContextRef"),
                    ("staged", "crate::eval::dynamic_wrap::StagedExpr"),
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
            DynWrapFnSpec {
                name: "new_expr_context",
                builtin_scope_id: 5009,
                args: vec![
                    ("target_arg", "Rc<dyn crate::r#gen::Target>"),
                    ("scope_arg", "std::collections::HashMap<ConstKey, Rc<dyn crate::r#gen::ScopeEntry>>"),
                ],
                ret: "crate::eval::code_value_extern::ExprContextRef",
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
                    crate::eval::code_value_extern::ExprContextRef(xx)
                },
            },
            DynWrapFnSpec {
                name: "list",
                builtin_scope_id: 5010,
                args: vec![("element_type", "crate::eval::dynamic_wrap::AnyType")],
                ret: "crate::eval::value::type_list::dynamic::TypeListDynamic",
                body: quote!{
                    crate::eval::value::type_list::dynamic::TypeListDynamic { element_type: element_type.0 }
                },
            },
            DynWrapFnSpec {
                name: "map",
                builtin_scope_id: 5011,
                args: vec![
                    ("key_type", "crate::eval::dynamic_wrap::AnyType"),
                    ("value_type", "crate::eval::dynamic_wrap::AnyType"),
                ],
                ret: "crate::eval::value::type_map::dynamic::TypeMapDynamic",
                body: quote!{
                    crate::eval::value::type_map::dynamic::TypeMapDynamic {
                        key_type: key_type.0,
                        value_type: value_type.0,
                    }
                },
            },
            DynWrapFnSpec {
                name: "option",
                builtin_scope_id: 5012,
                args: vec![("element_type", "crate::eval::dynamic_wrap::AnyType")],
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
            let arg_mortar = arg_mortar_expr(&s.args);
            let arg_decls: Vec<TokenStream> = s.args.iter().map(|(n, ty)| {
                let n = format_ident!("{}", n);
                let ty: TokenStream = ty.parse().unwrap();
                quote!{
                    let #n: #ty = <#ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: from_v8(
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
                        _jw_rv.set(
                            <#ret_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: into_v8(_jw_result, scope),
                        );
                    }
                    (
                        #name_str,
                        crate:: langhelp:: ScopeEntryId:: Builtin(#builtin_scope_id),
                        std:: rc:: Rc:: new(crate:: eval:: value:: type_function:: dynamic:: TypeFunctionDynamic {
                            arg: #arg_mortar,
                            ret: <#ret_ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type(),
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
    fs::write(PathBuf::from(env::var_os("OUT_DIR").unwrap()).join("gen.rs"), fmt(out)).unwrap();
}

struct MethodSpec {
    arg_type: TypeRef,
    name: &'static str,
    ret_type: TypeRef,
    rust_trait: Option<RustTrait>,
}

struct RustTrait {
    params: Vec<(&'static str, TokenStream)>,
}

fn snake_case(s: &str) -> String {
    let mut out = String::new();
    for (i, c) in s.chars().enumerate() {
        if c.is_uppercase() && i > 0 {
            out.push('_');
        }
        out.push(c.to_ascii_lowercase());
    }
    out
}

#[derive(Clone)]
struct TypeRef {
    mortar: TokenStream,
    path: TokenStream,
}

impl TypeRef {
    fn unit(tokens: TokenStream) -> Self {
        TypeRef {
            mortar: quote!{
                std:: rc:: Rc:: new(#tokens) as std:: rc:: Rc < dyn crate:: langhelp:: Type >
            },
            path: tokens,
        }
    }

    fn via_dynamic_wrap(tokens: TokenStream) -> Self {
        TypeRef {
            mortar: quote!{
                <#tokens as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type()
            },
            path: tokens,
        }
    }
}
