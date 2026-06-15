use {
    proc_macro2::TokenStream,
    quote::{
        format_ident,
        quote,
    },
    std::collections::HashSet,
};

pub fn build(out: &mut TokenStream, builtin_unique_ids: &mut HashSet<usize>) {
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
            pub(crate) fn #name_ident() -> crate:: langhelp:: Unique {
                crate:: langhelp:: Unique:: Builtin(#builtin_unique_id)
            }
        });
    }
    out.extend(quote!{
        #(#local_fns) *
    });
}
