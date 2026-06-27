use {
    crate::buildsrc::util::{
        InterfaceSpec,
        snake_case,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>, _scope_ids: &mut HashSet<usize>) {
    let mut aspect_names = HashSet::new();
    let mut aspect_token_items: Vec<TokenStream> = Vec::new();

    struct AspectMethod {
        doc: TokenStream,
        name: &'static str,
        params: Vec<(&'static str, &'static str)>,
        ret: &'static str,
    }

    struct AspectSpec {
        builtin_unique_id: usize,
        doc: TokenStream,
        methods: Vec<AspectMethod>,
        name: &'static str,
    }

    fn doc(text: &str) -> TokenStream {
        return quote!(#[doc =#text]);
    }

    for a in [
        //. .
        AspectSpec {
            doc: doc("Identity aspect for null"),
            name: "Null",
            builtin_unique_id: 1001,
            methods: vec![],
        },
        AspectSpec {
            doc: doc("Identity aspect for record"),
            name: "Record",
            builtin_unique_id: 1002,
            methods: vec![],
        },
        AspectSpec {
            doc: doc("Identity aspect for dynamic number"),
            name: "NumberDynamic",
            builtin_unique_id: 1003,
            methods: vec![],
        },
        AspectSpec {
            doc: doc("Identity aspect for dynamic bool"),
            name: "BoolDynamic",
            builtin_unique_id: 1004,
            methods: vec![],
        },
        AspectSpec {
            name: "StringDynamic",
            doc: doc("Identity aspect for dynamic string"),
            builtin_unique_id: 1005,
            methods: vec![],
        },
        AspectSpec {
            name: "BoolConst",
            doc: doc("Identity aspect for const bool"),
            builtin_unique_id: 1006,
            methods: vec![AspectMethod {
                name: "get",
                doc: doc(""),
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "bool",
            }],
        },
        AspectSpec {
            name: "NumberConst",
            doc: doc("Identity aspect for const number"),
            builtin_unique_id: 1008,
            methods: vec![AspectMethod {
                name: "get",
                doc: doc(""),
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "f64",
            }],
        },
        AspectSpec {
            name: "StringConst",
            doc: doc("Identity aspect for const str"),
            builtin_unique_id: 1009,
            methods: vec![AspectMethod {
                name: "get",
                doc: doc(""),
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "String",
            }],
        },
        AspectSpec {
            name: "Type",
            doc: doc("Converts a known type Value into a type rust value"),
            builtin_unique_id: 1010,
            methods: vec![AspectMethod {
                name: "type_",
                doc: doc(""),
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "std::rc::Rc<dyn crate::langhelp::Type>",
            }],
        },
        AspectSpec {
            name: "ListDynamic",
            doc: doc("Identity aspect for dynamic list"),
            builtin_unique_id: 1011,
            methods: vec![],
        },
        AspectSpec {
            name: "MapDynamic",
            doc: doc("Identity aspect for dynamic map"),
            builtin_unique_id: 1012,
            methods: vec![
                //. .
                AspectMethod {
                    name: "key_type",
                    doc: doc(""),
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
                AspectMethod {
                    name: "value_type",
                    doc: doc(""),
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
            ],
        },
        AspectSpec {
            name: "Tuple",
            doc: doc("Identity aspect for tuple"),
            builtin_unique_id: 1013,
            methods: vec![
                //. .
                AspectMethod {
                    name: "element_types",
                    doc: doc("Get the types of elements"),
                    params: vec![],
                    ret: "Vec<std::rc::Rc<dyn crate::langhelp::Type>>",
                },
                AspectMethod {
                    // TODO what's this
                    name: "take_elements",
                    doc: doc(""),
                    params: vec![("value", "std::rc::Rc<dyn crate::langhelp::Value>")],
                    ret: "Vec<crate::langhelp::EvalRes>",
                },
            ],
        },
        AspectSpec {
            name: "Union",
            doc: doc("Identity aspect for union"),
            builtin_unique_id: 1014,
            methods: vec![AspectMethod {
                name: "variants",
                doc: doc(""),
                params: vec![],
                ret: "Vec<(crate::langhelp::ConstKey, String, std::rc::Rc<dyn crate::langhelp::Type>)>",
            }],
        },
        AspectSpec {
            name: "Field",
            doc: doc("Allows field expressions to work on the value of this type"),
            builtin_unique_id: 1015,
            methods: vec![
                //. .
                AspectMethod {
                    name: "field",
                    doc: doc(
                        "Return a value representing the access of this field. Error if the field doesn't exist.",
                    ),
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
                    doc: doc("Same as `field` but return a None if the field doesn't exist."),
                    params: vec![("xx", "&crate::langhelp::ExprContext"), ("key", "&crate::langhelp::ConstKey")],
                    ret: "Option<std::rc::Rc<dyn crate::langhelp::Type>>",
                },
                AspectMethod {
                    name: "field_keys",
                    doc: doc(
                        "Enumerate the known field keys on this type. Used to identify sibling fields whose values need to be dropped when one field is extracted. Return empty if the type does not own its fields (e.g. records that borrow).",
                    ),
                    params: vec![("xx", "&crate::langhelp::ExprContext")],
                    ret: "Vec<crate::langhelp::ConstKey>",
                },
            ],
        },
        AspectSpec {
            name: "Function",
            doc: doc("Allows the value of this type to be called (as a function)"),
            builtin_unique_id: 1116,
            methods: vec![
                AspectMethod {
                    name: "call",
                    doc: doc("Make a call using the function referred to by the value."),
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("self_", "crate::langhelp::CodeValue"),
                        ("argument", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
                AspectMethod {
                    name: "exception_type",
                    doc: doc("Returns the declared exception type of this function, or TypeNever if none."),
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
            ],
        },
        AspectSpec {
            name: "Set",
            doc: doc("Allows the value to be assigned new data (mutated)."),
            builtin_unique_id: 1017,
            methods: vec![AspectMethod {
                name: "set",
                doc: doc("Mutate the value with the data from a new value."),
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
            doc: doc(""),
            builtin_unique_id: 1018,
            methods: vec![
                //. .
                AspectMethod {
                    name: "add",
                    doc: doc(""),
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
            doc: doc(""),
            builtin_unique_id: 1019,
            methods: vec![
                //. .
                AspectMethod {
                    name: "eq",
                    doc: doc(""),
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
            doc: doc(""),
            builtin_unique_id: 1020,
            methods: vec![
                //. .
                AspectMethod {
                    name: "lt",
                    doc: doc(""),
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
            doc: doc(
                "Creates bindings for pre-existing values. Namely function arguments and global scope values that were externally injected.",
            ),
            builtin_unique_id: 1021,
            methods: vec![
                //. .
                AspectMethod {
                    name: "initial_bind",
                    doc: doc(""),
                    params: vec![
                        //. .
                        ("dynamic_key", "crate::langhelp::BindingId"),
                    ],
                    ret: "Box<dyn crate::r#gen::Binding>",
                },
            ],
        },
        AspectSpec {
            name: "Bind",
            doc: doc("Creates code to bind a value, and returns the binding (binding)."),
            builtin_unique_id: 1022,
            methods: vec![
                //. .
                AspectMethod {
                    name: "bind",
                    doc: doc(""),
                    params: vec![
                        //. .
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("cv", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::Binding>)>",
                },
            ],
        },
        AspectSpec {
            name: "BindDynamic",
            doc: doc(
                "Creates code to prepare a value for binding, used by the Dynamic target (call tree rooted in Bind). This may be called recursively, e.g. in records, to prepare elements. If None, the value does not require dynamic binding - this is used by records/tuples to determine if the containing record/tuple also needs dynamic binding.",
            ),
            builtin_unique_id: 1023,
            methods: vec![
                //. .
                AspectMethod {
                    name: "bind_dynamic",
                    doc: doc(""),
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
            doc: doc(
                "Used to ensure a value is fully dynamic (turns const values into dynamic, does nothing for dynamic values).",
            ),
            builtin_unique_id: 1024,
            methods: vec![
                //. .
                AspectMethod {
                    name: "to_dynamic",
                    doc: doc("Convert the value to dynamic and return the code that this produced."),
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
            doc: doc(""),
            builtin_unique_id: 1025,
            methods: vec![
                //. .
                AspectMethod {
                    name: "to_value",
                    doc: doc(""),
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
            name: "PostDeserialize",
            doc: doc(
                "Called on imported values to convert them from const to dynamic form when appropriate (e.g. instances of source data/union types). Record/tuple types apply PostDeserialize to their elements recursively. Default: no-op.",
            ),
            builtin_unique_id: 1031,
            methods: vec![
                //. .
                AspectMethod {
                    name: "post_deserialize",
                    doc: doc(""),
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("cv", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EResult<crate::langhelp::CodeValue>",
                },
            ],
        },
        AspectSpec {
            name: "Lift",
            doc: doc(
                "Called to turn a dynamic value back into a const after dynamic evaluation finishes. This is also used to increase the type specificity at the border: e.g. a definition set function result (generic) has its type replaced with `TypeSourceFunction` with specific signature and becomes callable.",
            ),
            builtin_unique_id: 1026,
            methods: vec![
                //. .
                AspectMethod {
                    name: "lift",
                    doc: doc(""),
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
            doc: doc("Allows foreach expressions to be run on the value."),
            builtin_unique_id: 1027,
            methods: vec![
                //. .
                AspectMethod {
                    name: "element_type",
                    doc: doc(""),
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
                AspectMethod {
                    name: "foreach",
                    doc: doc(""),
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("base", "crate::langhelp::CodeValue"),
                        ("element_key", "crate::langhelp::BindingId"),
                        ("body", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "Artifact",
            doc: doc("Allows a value to be exported from a module."),
            builtin_unique_id: 1028,
            methods: vec![
                //. .
                AspectMethod {
                    name: "exporter",
                    doc: doc("Returns the exporter"),
                    params: vec![],
                    ret: "Rc<dyn crate::artifact::ArtifactExporter>",
                },
            ],
        },
        AspectSpec {
            name: "DropDynamic",
            doc: doc(
                "Allows a value to emit cleanup (drop) code when its scope ends, when it is discarded as a Seq entry, or when a sibling field is not extracted.",
            ),
            builtin_unique_id: 1032,
            methods: vec![AspectMethod {
                name: "drop",
                doc: doc(""),
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("cv", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EResult<crate::langhelp::Code>",
            }],
        },
        AspectSpec {
            name: "Aspect",
            doc: doc("Identity aspect for aspects (types representing values holding aspect implementations)."),
            builtin_unique_id: 1029,
            methods: vec![
                //. .
                AspectMethod {
                    name: "get_unique",
                    doc: doc(""),
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "crate::langhelp::Unique",
                },
                AspectMethod {
                    // TODO
                    name: "get_iface_data",
                    doc: doc(""),
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "dumpster::unsync::Gc<crate::eval::value::type_source_interface::SourceInterfaceData>",
                },
            ],
        },
    ] {
        assert!(aspect_names.insert(a.name), "duplicate aspect name: {}", a.name);
        let full_name = format!("Aspect{}", a.name);
        let doc = a.doc;
        let trait_name = format_ident!("{}", full_name);
        let snake = snake_case(&full_name);
        let id_fn = format_ident!("{}_aspect_id", snake);
        let get_fn = format_ident!("{}_get", snake);
        let get_maybe_fn = format_ident!("{}_get_maybe", snake);
        assert!(builtin_unique_ids.insert(a.builtin_unique_id));
        let builtin_unique_id = a.builtin_unique_id;
        let trait_methods: Vec<TokenStream> = a.methods.into_iter().map(|m| {
            let doc = m.doc;
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
                #doc fn #name(&self, #(#params), *) #ret;
            }
        }).collect();
        let missing_msg = format!("Type ({{}}) is missing aspect {}", full_name);
        aspect_token_items.push(quote!{
            #doc pub(crate) trait #trait_name: crate:: langhelp:: Aspect {
                #(#trait_methods) *
            }
            pub(crate) fn #id_fn() -> crate:: langhelp:: Unique {
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

pub fn interface_specs() -> Vec<InterfaceSpec> {
    vec![]
}
