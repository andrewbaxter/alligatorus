use {
    proc_macro2::TokenStream,
    quote::quote,
};

pub fn arg_dynamic_type_expr(dynamic_types: &[TokenStream]) -> TokenStream {
    match dynamic_types.len() {
        1 => dynamic_types[0].clone(),
        _ => {
            let elems: Vec<TokenStream> = dynamic_types.iter().map(|m| quote!{
                crate:: eval:: value:: tuple:: TupleElement:: Dynamic(#m)
            }).collect();
            quote!{
                std::rc::Rc::new(crate::eval::value::tuple::Tuple { elements: vec![#(#elems), *], }) as
                    std::rc::Rc<dyn crate::langhelp::Type>
            }
        },
    }
}

pub fn arg_dynamic_type_expr_str(args: &[(&'static str, &'static str)]) -> TokenStream {
    let dynamic_types: Vec<TokenStream> = args.iter().map(|(_, ty)| {
        let ty: TokenStream = ty.parse().unwrap();
        quote!(< #ty as crate:: eval:: push:: Push >:: dynamic_type())
    }).collect();
    arg_dynamic_type_expr(&dynamic_types)
}

pub struct InterfaceSpec {
    pub builtin_unique_id: usize,
    pub description: Option<&'static str>,
    pub field_prefix: &'static str,
    pub kind: &'static str,
    pub methods: Vec<Method>,
    pub name: &'static str,
    pub register_in_scope: bool,
    pub rust_trait_name: Option<&'static str>,
    pub scope_id: usize,
}

pub struct Method {
    pub args: MethodArgs,
    pub name: &'static str,
    pub ret_type: TypeRef,
}

pub enum MethodArgs {
    Rust(MethodSig),
    Source(TypeRef),
}

pub struct MethodSig {
    pub context: Vec<(&'static str, TokenStream)>,
    pub data: Vec<(&'static str, TypeRef)>,
}

pub struct PushMethod {
    pub args: Vec<(&'static str, &'static str)>,
    pub body: TokenStream,
    pub name: &'static str,
    pub ret: &'static str,
}

pub fn snake_case(s: &str) -> String {
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
pub struct TypeRef {
    pub dynamic_type: TokenStream,
    pub path: TokenStream,
}

impl TypeRef {
    pub fn opaque(path: TokenStream) -> Self {
        TypeRef {
            dynamic_type: quote!{
                std::rc::Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic) as
                    std::rc::Rc<dyn crate::langhelp::Type>
            },
            path: path,
        }
    }

    pub fn unit(tokens: TokenStream) -> Self {
        TypeRef {
            dynamic_type: quote!{
                std:: rc:: Rc:: new(#tokens) as std:: rc:: Rc < dyn crate:: langhelp:: Type >
            },
            path: tokens,
        }
    }

    pub fn via_push(tokens: TokenStream) -> Self {
        TypeRef {
            dynamic_type: quote!{
                <#tokens as crate:: eval:: push:: Push >:: dynamic_type()
            },
            path: tokens,
        }
    }
}
