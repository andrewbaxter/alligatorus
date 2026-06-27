use {
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>, scope_ids: &mut HashSet<usize>) {
    let mut local_fns: Vec<TokenStream> = Vec::new();
    for (name, builtin_unique_id) in [
        //. .
        ("expr_context_ref_type_identity", 6001),
        ("code_value_extern_type_identity", 6002),
        ("type_unique_identity", 6003),
        ("interface_const_identity", 6004),
        ("interface_ref_type_identity", 6005),
        ("type_type_identity", 6006),
        ("union_ref_type_identity", 6007),
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
            pub(crate) fn #name_ident() -> crate:: langhelp:: Unique {
                crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
            }
        });
    }
    let mut local_consts: Vec<TokenStream> = Vec::new();
    for (name, scope_id) in [
        //. .
        ("ASPECT_LOOKUP_FN_SCOPE_ID", 7001usize),
        ("CAST_VALUE_FN_SCOPE_ID", 7002),
        ("CVE_EXTRACT_FN_SCOPE_ID", 7003),
        ("CVE_GET_CODE_ENUM_FN_SCOPE_ID", 7004),
        ("CVE_GET_VALUE_FN_SCOPE_ID", 7005),
        ("DATA_INSTANCE_TO_ASPECT_FN_SCOPE_ID", 7006),
        ("DATA_INSTANCE_TO_CODE_FN_SCOPE_ID", 7007),
        ("DATA_INSTANCE_TO_BINDING_FN_SCOPE_ID", 7008),
        ("DATA_INSTANCE_TO_TARGET_FN_SCOPE_ID", 7009),
        ("DATA_INSTANCE_TO_TYPE_FN_SCOPE_ID", 7010),
        ("DATA_INSTANCE_TO_VALUE_FN_SCOPE_ID", 7011),
        ("DATA_TYPE_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID", 7012),
        ("DATA_TYPE_REF_TO_TYPE_FN_SCOPE_ID", 7013),
        ("FUNCTION_CONST_TO_TYPE_FN_SCOPE_ID", 7014),
        ("INTERFACE_REF_TO_TYPE_FN_SCOPE_ID", 7015),
        ("RECORD_TO_TYPE_FN_SCOPE_ID", 7016),
        ("STAGE_FN_SCOPE_ID", 7017),
        ("TAGGED_UNION_CONSTRUCTOR_TO_TYPE_FN_SCOPE_ID", 7018),
        ("TAGGED_UNION_REF_TO_TYPE_FN_SCOPE_ID", 7019),
        ("TUPLE_TO_TYPE_FN_SCOPE_ID", 7020),
        ("UNIQUE_EQ_FN_SCOPE_ID", 7021),
        ("VALUE_GET_TYPE_FN_SCOPE_ID", 7022),
        ("ASPECT_COMPILE_SCOPE_ID", 7023),
        ("CAST_VALUE_COMPILE_SCOPE_ID", 7024),
    ] {
        assert!(scope_ids.insert(scope_id));
        let name_ident = format_ident!("{}", name);
        local_consts.push(quote!{
            #[allow(dead_code)] pub(
                crate
            ) const #name_ident: crate:: langhelp:: BindingId = crate:: langhelp:: BindingId:: Builtin(#scope_id);
        });
    }
    out.extend(quote!{
        #(#local_fns) * #(#local_consts) *
    });
}
