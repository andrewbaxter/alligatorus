use {
    crate::buildsrc::util::{
        InterfaceSpec,
        Method,
        MethodArgs,
        TypeRef,
    },
    quote::quote,
};

pub fn specs() -> Vec<InterfaceSpec> {
    let any = || TypeRef::unit(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic));
    let string = || TypeRef::unit(quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic));
    let bool_ = || TypeRef::unit(quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic));
    let aspect = || TypeRef::unit(quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic));
    let cve = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic));
    let xx_ref = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeExprContextPushDynamic));
    let type_ref_push = || TypeRef::unit(quote!(crate::r#gen::TypeTypeRefPushDynamic));
    let unique = || TypeRef::unit(quote!(crate::eval::value::type_unique::r#const::TypeUnique));
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
                    args: MethodArgs::Source(vec![]),
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
                    args: MethodArgs::Source(
                        vec![("xx", xx_ref()), ("source_type", type_ref_push()), ("cv", cve())],
                    ),
                },
                Method {
                    name: "cast_from",
                    ret_type: cve(),
                    args: MethodArgs::Source(
                        vec![("xx", xx_ref()), ("source_type", type_ref_push()), ("cv", cve())],
                    ),
                },
                Method {
                    name: "desc",
                    ret_type: string(),
                    args: MethodArgs::Source(vec![]),
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
                    args: MethodArgs::Source(vec![("key", unique())]),
                },
            ],
        },
    ]
}
