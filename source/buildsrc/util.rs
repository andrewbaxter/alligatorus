use {
    proc_macro2::TokenStream,
    quote::quote,
};

/// Combine per-arg mortar expressions into a single mortar: null for 0 args, the
/// sole mortar for 1, a `Tuple` mortar for N. Each `mortars[i]` is a `TokenStream`
/// that evaluates to `Rc<dyn Type>`.
pub fn arg_mortar_expr(mortars: &[TokenStream]) -> TokenStream {
    match mortars.len() {
        0 => quote!{
            std::rc::Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic) as
                std::rc::Rc<dyn crate::langhelp::Type>
        },
        1 => mortars[0].clone(),
        _ => {
            let elems: Vec<TokenStream> = mortars.iter().map(|m| quote!{
                crate:: eval:: value:: tuple:: TupleElement:: Mortar(#m)
            }).collect();
            quote!{
                std::rc::Rc::new(crate::eval::value::tuple::Tuple { elements: vec![#(#elems), *], }) as
                    std::rc::Rc<dyn crate::langhelp::Type>
            }
        },
    }
}

/// Convenience over [`arg_mortar_expr`] for args given as
/// `(name, rust-type-as-string)` where each type implements `DynamicWrap`. Wraps
/// each via `mortar_type()` then combines.
pub fn arg_mortar_expr_str(args: &[(&'static str, &'static str)]) -> TokenStream {
    let mortars: Vec<TokenStream> = args.iter().map(|(_, ty)| {
        let ty: TokenStream = ty.parse().unwrap();
        quote!(< #ty as crate:: eval:: dynamic_wrap:: DynamicWrap >:: mortar_type())
    }).collect();
    arg_mortar_expr(&mortars)
}

pub struct DynWrapMethod {
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
    /// signature; its `mortar` contributes to the derived source-language arg type
    /// (null mortar for 0 params, the single param's mortar for 1, a tuple mortar for
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
    pub mortar: TokenStream,
    pub path: TokenStream,
}

impl TypeRef {
    /// `path` is the Rust type used in the trait method signature; the source-language
    /// mortar is `TypeAnyDynamic`. Use when the Rust type has no `DynamicWrap` impl
    /// (e.g. references, `Vec<...>` of compiler-internal types) so the source-language
    /// side falls back to `any` while the Rust signature still carries the real type.
    pub fn opaque(path: TokenStream) -> Self {
        TypeRef {
            mortar: quote!{
                std::rc::Rc::new(crate::eval::value::type_any::dynamic::TypeAnyDynamic) as
                    std::rc::Rc<dyn crate::langhelp::Type>
            },
            path: path,
        }
    }

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
