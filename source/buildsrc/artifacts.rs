use {
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream) {
    let mut artifact_pieces: Vec<TokenStream> = Vec::new();
    let mut builtin_artifact_ids = HashSet::new();

    struct ArtifactSpec {
        build_artifact: bool,
        build_type_artifact: bool,
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
            build_artifact: false,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "defset_method",
            builtin_artifact_id: 7022,
            type_path: None,
            data_type: None,
            register: false,
            build_artifact: false,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "defset_iface_impl",
            builtin_artifact_id: 7023,
            type_path: None,
            data_type: None,
            register: false,
            build_artifact: false,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "defset_iface_method_impl",
            builtin_artifact_id: 7024,
            type_path: None,
            data_type: None,
            register: false,
            build_artifact: false,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "defset_iface",
            builtin_artifact_id: 7025,
            type_path: None,
            data_type: None,
            register: false,
            build_artifact: false,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "type_record_artifact",
            builtin_artifact_id: 7002,
            type_path: Some("crate::eval::value::record::artifact::TypeRecordArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
            build_artifact: true,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "type_tuple_artifact",
            builtin_artifact_id: 7003,
            type_path: Some("crate::eval::value::tuple::artifact::TypeTupleArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
            build_artifact: true,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "type_map_artifact",
            builtin_artifact_id: 7004,
            type_path: Some("crate::eval::value::type_map::artifact::TypeMapArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
            build_artifact: true,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "type_option_artifact",
            builtin_artifact_id: 7005,
            type_path: Some("crate::eval::value::type_option::artifact::TypeOptionArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
            build_artifact: true,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "type_source_interface_const_artifact",
            builtin_artifact_id: 7006,
            type_path: Some("crate::eval::value::type_source_interface::artifact::TypeSourceInterfaceConstArtifact"),
            data_type: Some("crate::eval::value::type_source_interface::BuiltinInterfaceKind"),
            register: false,
            build_artifact: true,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "type_type_source_data_type_constructor_dynamic",
            builtin_artifact_id: 7007,
            type_path: Some(
                "crate::eval::value::type_type_source_data_type_constructor::dynamic::TypeTypeSourceDataTypeConstructorDynamic",
            ),
            data_type: Some("serde_json::Value"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_code_value_extern_dynamic",
            builtin_artifact_id: 7008,
            type_path: Some("crate::eval::code_value_extern::TypeCodeValueExternDynamic"),
            data_type: Some("()"),
            register: true,
            build_artifact: false,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_type_source_data_type_dynamic",
            builtin_artifact_id: 7009,
            type_path: Some("crate::eval::value::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic"),
            data_type: Some("serde_json::Value"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_null_const",
            builtin_artifact_id: 7010,
            type_path: Some("crate::eval::value::type_null::r#const::TypeNullConst"),
            data_type: Some("crate::eval::value::type_null::artifact::ArtifactDataNull"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_any_const",
            builtin_artifact_id: 7011,
            type_path: Some("crate::eval::value::type_any::r#const::TypeAnyConst"),
            data_type: Some("serde_json::Value"),
            register: true,
            build_artifact: false,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_string_const",
            builtin_artifact_id: 7012,
            type_path: Some("crate::eval::value::type_string::r#const::TypeStringConst"),
            data_type: Some("crate::eval::value::type_string::artifact::ArtifactDataString"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_null_dynamic",
            builtin_artifact_id: 7013,
            type_path: Some("crate::eval::value::type_null::dynamic::TypeNullDynamic"),
            data_type: Some("crate::eval::value::type_null::artifact::ArtifactDataNull"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_source_tagged_union_ref_dynamic",
            builtin_artifact_id: 7014,
            type_path: Some("crate::eval::value::type_source_tagged_union::dynamic::TypeSourceTaggedUnionRefDynamic"),
            data_type: Some("serde_json::Value"),
            register: true,
            build_artifact: false,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_source_data_type_ref_dynamic",
            builtin_artifact_id: 7015,
            type_path: Some("crate::eval::value::type_source_data_type::dynamic::TypeSourceDataTypeRefDynamic"),
            data_type: Some("serde_json::Value"),
            register: true,
            build_artifact: false,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_number_const",
            builtin_artifact_id: 7016,
            type_path: Some("crate::eval::value::type_number::r#const::TypeNumberConst"),
            data_type: Some("f64"),
            register: true,
            build_artifact: true,
            build_type_artifact: false,
        },
        ArtifactSpec {
            name: "type_type_source_function_dynamic",
            builtin_artifact_id: 7017,
            type_path: Some("crate::eval::value::type_source_function::dynamic::TypeTypeSourceFunctionDynamic"),
            data_type: Some("serde_json::Value"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_expr",
            builtin_artifact_id: 7018,
            type_path: Some("crate::eval::value::type_expr::TypeExpr"),
            data_type: Some("serde_json::Value"),
            register: true,
            build_artifact: false,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_string_dynamic",
            builtin_artifact_id: 7019,
            type_path: Some("crate::eval::value::type_string::dynamic::TypeStringDynamic"),
            data_type: Some("crate::eval::value::type_string::artifact::ArtifactDataString"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_number_dynamic",
            builtin_artifact_id: 7020,
            type_path: Some("crate::eval::value::type_number::dynamic::TypeNumberDynamic"),
            data_type: Some("f64"),
            register: true,
            build_artifact: true,
            build_type_artifact: true,
        },
        ArtifactSpec {
            name: "type_list_artifact",
            builtin_artifact_id: 7021,
            type_path: Some("crate::eval::value::type_list::artifact::TypeListArtifact"),
            data_type: Some("crate::eval::value::type_list::artifact::ArtifactListType"),
            register: false,
            build_artifact: true,
            build_type_artifact: false,
        },
    ] {
        let id_fn = format_ident!("{}_artifact_id", s.name);
        let builtin_artifact_id = s.builtin_artifact_id;
        assert!(builtin_artifact_ids.insert(builtin_artifact_id));
        let id_decl = quote!{
            pub(crate) fn #id_fn() -> crate:: eval:: artifact:: ArtifactIdBuiltin {
                crate:: eval:: artifact:: ArtifactIdBuiltin(#builtin_artifact_id)
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
                                    crate::eval::artifact::ArtifactIdBuiltin,
                                    std::rc::Rc<dyn crate::eval::artifact::ArtifactDeserializer>,
                                >,
                        ) {
                            m.insert(Self::artifact_id(), std::rc::Rc::new(Self));
                        }
                    }
                } else {
                    quote!{
                    }
                };
                let build_artifact = if s.build_artifact {
                    quote!{
                        #[
                            allow(dead_code)
                        ] pub(crate) fn build_artifact(data: #data_type) -> crate:: eval:: artifact:: Artifact {
                            crate::eval::artifact::Artifact {
                                deserializer: crate::eval::artifact::ArtifactId::Builtin(Self::artifact_id()),
                                data: serde_json::to_value(data).unwrap(),
                            }
                        }
                    }
                } else {
                    quote!{
                    }
                };
                let build_type_artifact = if s.build_type_artifact {
                    quote!{
                        #[allow(dead_code)]
                        pub(crate) fn build_type_artifact() -> crate::eval::artifact::Artifact {
                            crate::eval::artifact::Artifact {
                                deserializer: crate::eval::artifact::ArtifactId::Builtin(Self::artifact_id()),
                                data: serde_json::Value::Null,
                            }
                        }
                    }
                } else {
                    quote!{
                    }
                };
                quote!{
                    impl #type_path {
                        pub(crate) fn artifact_id() -> crate:: eval:: artifact:: ArtifactIdBuiltin {
                            #id_fn()
                        }
                        #build_artifact #build_type_artifact #register
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
