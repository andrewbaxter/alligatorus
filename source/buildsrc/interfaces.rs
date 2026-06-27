use {
    crate::buildsrc::util::{
        InterfaceSpec,
        MethodArgs,
    },
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(
    out: &mut TokenStream,
    builtin_unique_ids: &mut HashSet<usize>,
    scope_ids: &mut HashSet<usize>,
    extra_specs: Vec<InterfaceSpec>,
) {
    let mut iface_names = HashSet::new();
    let mut iface_prefixes = HashSet::new();
    let mut iface_kinds = HashSet::new();
    let mut iface_struct_fields: Vec<TokenStream> = Vec::new();
    let mut iface_build_entries: Vec<TokenStream> = Vec::new();
    let mut iface_result_fields: Vec<TokenStream> = Vec::new();
    let mut iface_trait_defs: Vec<TokenStream> = Vec::new();
    let mut iface_identity_fns: Vec<TokenStream> = Vec::new();
    let mut iface_field_consts: Vec<TokenStream> = Vec::new();
    for iface in extra_specs {
        assert!(iface_names.insert(iface.name), "duplicate interface name: {}", iface.name);
        assert!(iface_prefixes.insert(iface.field_prefix), "duplicate field_prefix: {}", iface.field_prefix);
        assert!(iface_kinds.insert(iface.kind), "duplicate kind: {}", iface.kind);
        let mut method_names = HashSet::new();
        for m in &iface.methods {
            assert!(method_names.insert(m.name), "duplicate method name on iface {}: {}", iface.name, m.name,);
        }
        let prefix = iface.field_prefix;
        for (idx, m) in iface.methods.iter().enumerate() {
            let field_name = format!("_im_{}{}", prefix, idx);
            let iface_upper = iface.name.to_uppercase().replace('-', "_");
            let method_upper = m.name.trim_end_matches('_').to_uppercase().replace('-', "_");
            let const_name = format_ident!("IM_{}_{}_FIELD", iface_upper, method_upper);
            iface_field_consts.push(quote!{
                #[allow(dead_code)] pub(crate) const #const_name: & str = #field_name;
            });
        }
        let methods_expr_items: Vec<TokenStream> = iface.methods.iter().enumerate().map(|(idx, m)| {
            let name = m.name.trim_end_matches('_');
            let field_name = format!("_im_{}{}", prefix, idx);
            let arg_dynamic_type = match &m.args {
                MethodArgs::Source(_) => {
                    quote!{
                        std::rc::Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic) as
                            std::rc::Rc<dyn crate::langhelp::Type>
                    }
                },
                MethodArgs::Rust(rs) => {
                    let dynamic_types: Vec<TokenStream> =
                        rs.data.iter().map(|(_, t)| t.dynamic_type.clone()).collect();
                    crate::buildsrc::util::arg_dynamic_type_expr(&dynamic_types)
                },
            };
            let arg_record_dynamic_type = match &m.args {
                MethodArgs::Source(fields) => {
                    let inserts: Vec<TokenStream> = fields.iter().map(|(name, t)| {
                        let dt = &t.dynamic_type;
                        quote!{
                            _m.insert(
                                crate:: langhelp:: ConstKey:: String(#name.to_string()),
                                crate:: eval:: value:: type_record:: RecordField:: Dynamic(#dt),
                            );
                        }
                    }).collect();
                    quote!{
                        Some(std:: rc:: Rc:: new(crate:: eval:: value:: type_record:: Record {
                            base: Some(Box::new(crate::eval::value::type_record::RecordBaseDynamic)),
                            fields: {
                                let mut _m = std::collections::BTreeMap::new();
                                #(#inserts) * _m
                            },
                        }) as std:: rc:: Rc < dyn crate:: langhelp:: Type >)
                    }
                },
                MethodArgs::Rust(_) => quote!{
                    None
                },
            };
            let ret = &m.ret_type.dynamic_type;
            quote!{
                std:: rc:: Rc:: new(crate:: eval:: value:: type_source_interface:: SourceInterfaceMethodData {
                    name: #name.to_string(),
                    arg_type: std:: cell:: RefCell:: new(Some(#arg_dynamic_type)),
                    arg_record_type: std:: cell:: RefCell:: new(#arg_record_dynamic_type),
                    ret_type: std:: cell:: RefCell:: new(Some(#ret)),
                    field_name: #field_name.to_string(),
                })
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
        {
            let fn_name = format_ident!("builtin_{}_iface_identity", if iface.name == "type" {
                "type"
            } else {
                iface.name
            });
            iface_identity_fns.push(quote!{
                #[allow(dead_code)] pub(crate) fn #fn_name() -> crate:: langhelp:: Unique {
                    crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
                }
            });
        }
        iface_build_entries.push(quote!{
            let #f = dumpster:: unsync:: Gc:: new(crate:: eval:: value:: type_source_interface:: SourceInterfaceData {
                artifact_id: RefCell:: new(None),
                builtin_kind: Some(crate:: eval:: value:: type_source_interface:: BuiltinInterfaceKind::#kind,),
                set: std:: cell:: RefCell:: new(Some(builtin_def_set.clone())),
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
                        crate::langhelp::DefSetId(
                            crate::langhelp::CompileId(import_state, crate::langhelp::SourceId(usize::MAX),),
                            usize::MAX,
                        ),
                    ),
                );
            #(#iface_build_entries) * GenBuiltinInterfaces {
                #(#iface_result_fields),
                *
            }
        }
        #(#iface_identity_fns) * #(#iface_field_consts) *
    });
}
