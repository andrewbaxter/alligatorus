use {
    proc_macro2::TokenStream,
    quote::quote,
};

pub fn arg_mortar_expr(args: &[(&'static str, &'static str)]) -> TokenStream {
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

pub struct DynWrapMethod {
    pub args: Vec<(&'static str, &'static str)>,
    pub body: TokenStream,
    pub name: &'static str,
    pub ret: &'static str,
}

pub struct MethodSpec {
    pub arg_type: TypeRef,
    pub name: &'static str,
    pub ret_type: TypeRef,
    pub rust_trait: Option<RustTrait>,
}

pub struct RustTrait {
    pub params: Vec<(&'static str, TokenStream)>,
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
    pub mortar: TokenStream,
    pub path: TokenStream,
}

impl TypeRef {
    pub fn unit(tokens: TokenStream) -> Self {
        TypeRef {
            mortar: quote!{
                std:: rc:: Rc:: new(#tokens) as std:: rc:: Rc < dyn crate:: langhelp:: Type >
            },
            path: tokens,
        }
    }

    pub fn via_dynamic_wrap(tokens: TokenStream) -> Self {
        TypeRef {
            mortar: quote!{
                <#tokens as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type()
            },
            path: tokens,
        }
    }
}
