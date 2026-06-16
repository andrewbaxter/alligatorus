use {
    proc_macro2::TokenStream,
    quote::quote,
};

/// Combine per-arg dynamic type expressions into a single dynamic type: null for 0 args, the
/// sole dynamic type for 1, a `Tuple` dynamic type for N. Each `dynamic_types[i]` is a `TokenStream`
/// that evaluates to `Rc<dyn Type>`.
pub fn arg_dynamic_type_expr(dynamic_types: &[TokenStream]) -> TokenStream {
    match dynamic_types.len() {
        0 => quote!{
            std::rc::Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic) as
                std::rc::Rc<dyn crate::langhelp::Type>
        },
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

/// Convenience over [`arg_dynamic_type_expr`] for args given as
/// `(name, rust-type-as-string)` where each type implements `Push`. Wraps
/// each via `dynamic_type()` then combines.
pub fn arg_dynamic_type_expr_str(args: &[(&'static str, &'static str)]) -> TokenStream {
    let dynamic_types: Vec<TokenStream> = args.iter().map(|(_, ty)| {
        let ty: TokenStream = ty.parse().unwrap();
        quote!(< #ty as crate:: eval:: push:: Push >:: dynamic_type())
    }).collect();
    arg_dynamic_type_expr(&dynamic_types)
}

pub struct PushMethod {
    pub args: Vec<(&'static str, &'static str)>,
    pub body: TokenStream,
    pub name: &'static str,
    pub ret: &'static str,
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
    /// Method with a generated Rust trait method counterpart. The source-language
    /// `arg_type` is derived from `data`, keeping the two views in sync.
    Rust(MethodSig),
    /// Source-language only method (no Rust trait method generated).
    Source(TypeRef),
}

pub struct MethodSig {
    /// Params that appear in the Rust trait method signature but not in the
    /// source-language arg type (e.g. `xx: &ExprContext`, `loc: QExprId`).
    pub context: Vec<(&'static str, TokenStream)>,
    /// Data params. Each `TypeRef`'s `path` is used for the Rust trait method
    /// signature; its `dynamic_type` contributes to the derived source-language arg type
    /// (null dynamic type for 0 params, the single param's dynamic type for 1, a tuple dynamic type for
    /// N).
    pub data: Vec<(&'static str, TypeRef)>,
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
    /// `path` is the Rust type used in the trait method signature; the source-language
    /// dynamic type is `TypeAnyDynamic`. Use when the Rust type has no `Push` impl
    /// (e.g. references, `Vec<...>` of compiler-internal types) so the source-language
    /// side falls back to `any` while the Rust signature still carries the real type.
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
