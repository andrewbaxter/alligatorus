//! Generates source-level interface registration for the builtin interfaces
//! (`value`, `type`, `aspect_function`, `aspect_field`, `scope_entry`, `target`,
//! `code_dyn`). Single source of truth for method names + source-language
//! signatures, paired with each interface's matching Rust trait so the two
//! can't drift (e.g. `Target` previously had every source method registered as
//! `ret_type = any` regardless of what the Rust trait actually returned).
//!
//! Output: `$OUT_DIR/gen_builtin_interfaces.rs`, included from
//! `src/eval/value/type_source_interface/mod.rs`.
//!
//! NOTE: this generates *registration* only — the Rust traits themselves
//! still live in `langhelp.rs` / `eval/mod.rs` / `eval/aspect/`. Pulling
//! the trait defs in here is a follow-up: it needs the spec to also encode
//! each method's Rust param/return types (a different mapping from the
//! source-side `SrcType` used here).

use {
    proc_macro2::TokenStream,
    quote::{format_ident, quote},
    std::{env, fs, path::PathBuf},
};

/// Identifies the return / argument type of a spec method. Two TokenStreams:
///
/// * `path` — the Rust type (used as the trait method return type, generic
///   parameter, etc.).
/// * `mortar` — a value expression evaluating to `Rc<dyn Type>` (the
///   source-side Type instance stored in `SourceInterfaceMethodData`).
///   Codegen uses this verbatim — no further wrapping.
///
/// For types that are themselves `Type` impls (e.g. `TypeAnyDynamic`),
/// `mortar` is `Rc::new(T) as Rc<dyn Type>`.
///
/// For Rust value-carriers with a `DynamicWrap` impl (e.g.
/// `CodeValueExtern`), `mortar` is `<T as DynamicWrap>::mortar_type()` —
/// derived from `path`, so the spec author cannot specify a mismatched
/// source-side Type.
#[derive(Clone)]
struct TypeRef {
    path: TokenStream,
    mortar: TokenStream,
}

impl TypeRef {
    /// Unit-struct `Type` (`path` impls `Type` directly).
    fn unit(tokens: TokenStream) -> Self {
        TypeRef {
            mortar: quote! {
                std::rc::Rc::new(#tokens) as std::rc::Rc<dyn crate::langhelp::Type>
            },
            path: tokens,
        }
    }

    /// Rust value-carrier with a `DynamicWrap` impl whose `mortar_type()`
    /// is the source-side Type. Single source of truth: `path` drives both
    /// the trait return type and (via `DynamicWrap`) the mortar Type — no
    /// possibility of mismatch.
    fn via_dynamic_wrap(tokens: TokenStream) -> Self {
        TypeRef {
            mortar: quote! {
                <#tokens as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type()
            },
            path: tokens,
        }
    }
}

struct MethodSpec {
    /// Source-level method name.
    name: &'static str,
    /// `arg` type. Builtin-iface methods that take a record pass
    /// `TypeAnyDynamic` (Rust dispatch builds the record).
    arg_type: TypeRef,
    /// Return type. Single source of truth: the Rust trait method's
    /// return type IS `ret_type.path`, and the source-side mortar Type
    /// stored in `SourceInterfaceMethodData::ret_type` IS `ret_type.mortar`.
    /// There is no separate "override" — by construction, the spec
    /// author cannot specify two different return types that disagree.
    ret_type: TypeRef,
    /// When `Some` *and* the interface spec sets `rust_trait_name`, this
    /// method contributes a `fn` to the generated trait. The return type
    /// of that trait method is always `ret_type.path`.
    rust_trait: Option<RustTrait>,
}

struct RustTrait {
    /// `(name, rust_type_path)`. `self` is added implicitly as `&self`.
    /// The return type is not a field of `RustTrait` — it derives from
    /// `MethodSpec.ret_type.path` so source mortar and Rust trait return
    /// cannot diverge.
    params: Vec<(&'static str, TokenStream)>,
}

struct InterfaceSpec {
    /// Snake-case name used as the field on `BuiltinInterfaces`, the
    /// `BuiltinInterfaceKind` variant (PascalCased), and the description
    /// shown in `desc()` / error messages.
    name: &'static str,
    /// Explicit `Unique::Builtin(id)` for this interface's identity.
    /// Must be globally unique across every spec in this file (verified
    /// by `check_no_duplicate_ids`).
    id: usize,
    /// Short prefix for generated `field_name`s: e.g. `v` → `_im_v0`,
    /// `tg` → `_im_tg0`. Must be unique across interfaces.
    field_prefix: &'static str,
    /// `BuiltinInterfaceKind::<variant>` — Pascal-case name of the variant.
    kind: &'static str,
    /// Optional human description (defaults to `name`).
    description: Option<&'static str>,
    /// When `Some`, generate a `pub(crate) trait <name>` with methods
    /// drawn from each `MethodSpec.rust_trait`. The string is the trait's
    /// PascalCase name (e.g. `"Target"`). Methods whose `rust_trait` is
    /// `None` are skipped.
    rust_trait_name: Option<&'static str>,
    methods: Vec<MethodSpec>,
}

/// `TypeOptionDynamic { element_type: <inner mortar> }` as a `TypeRef`.
/// The trait-return path is the bare `TypeOptionDynamic` (parameterized at
/// runtime via the field, not generically); the source-mortar value
/// constructs an instance with the parameterized `element_type`.
fn option_type(inner: &TypeRef) -> TypeRef {
    let inner_mortar = &inner.mortar;
    TypeRef {
        path: quote!(crate::eval::value::type_option::dynamic::TypeOptionDynamic),
        mortar: quote! {
            std::rc::Rc::new(crate::eval::value::type_option::dynamic::TypeOptionDynamic {
                element_type: #inner_mortar,
            }) as std::rc::Rc<dyn crate::langhelp::Type>
        },
    }
}

fn interfaces() -> Vec<InterfaceSpec> {
    let any = || TypeRef::unit(quote!(crate::eval::value::type_any::dynamic::TypeAnyDynamic));
    let null = || TypeRef::unit(quote!(crate::eval::value::type_null::dynamic::TypeNullDynamic));
    let string = || TypeRef::unit(quote!(crate::eval::value::type_string::dynamic::TypeStringDynamic));
    let bool_ = || TypeRef::unit(quote!(crate::eval::value::type_bool::dynamic::TypeBoolDynamic));
    let value = || TypeRef::unit(quote!(crate::eval::value::type_value::dynamic::TypeValueDynamic));
    let aspect = || TypeRef::unit(quote!(crate::eval::value::type_aspect::dynamic::TypeAspectDynamic));
    let cve = || TypeRef::unit(quote!(crate::eval::code_value_extern::TypeCodeValueExternDynamic));
    // `CodeValue` (impls `Type` since the build.rs unification — see the
    // CodeValue impl block in `langhelp.rs`). The source-mortar value is
    // `CodeValue::placeholder()` because Type-identity is what
    // typechecking consults, not the carried code/value.
    vec![
        InterfaceSpec {
            name: "value",
            id: 2001,
            field_prefix: "v",
            kind: "Value",
            description: None,
            rust_trait_name: None,
            methods: vec![
                MethodSpec {
                    name: "type",
                    arg_type: null(),
                    ret_type: any(),
                    rust_trait: None,
                },
            ],
        },
        InterfaceSpec {
            name: "type",
            id: 2002,
            field_prefix: "t",
            kind: "Type",
            description: None,
            rust_trait_name: None,
            methods: vec![
                MethodSpec { name: "can_cast_from", arg_type: any(), ret_type: bool_(), rust_trait: None },
                MethodSpec { name: "cast_from", arg_type: any(), ret_type: cve(), rust_trait: None },
                MethodSpec { name: "desc", arg_type: null(), ret_type: string(), rust_trait: None },
                MethodSpec {
                    name: "aspect",
                    arg_type: any(),
                    ret_type: option_type(&aspect()),
                    rust_trait: None,
                },
            ],
        },
        // Shares id with the `AspectFunction` aspect spec — source-level
        // `impl_interface(aspect_function, ...)` is found by the same
        // `aspect_function_aspect_id()` lookup that the Rust trait uses.
        InterfaceSpec {
            name: "aspect_function",
            id: 1016,
            field_prefix: "af",
            kind: "AspectFunction",
            description: None,
            rust_trait_name: None,
            methods: vec![
                MethodSpec { name: "call", arg_type: any(), ret_type: cve(), rust_trait: None },
            ],
        },
        // Shares id with the `AspectField` aspect spec — same rationale as
        // `aspect_function` above.
        InterfaceSpec {
            name: "aspect_field",
            id: 1015,
            field_prefix: "afd",
            kind: "AspectField",
            description: None,
            rust_trait_name: None,
            methods: vec![
                MethodSpec { name: "field", arg_type: any(), ret_type: cve(), rust_trait: None },
                MethodSpec { name: "field_type_opt", arg_type: any(), ret_type: any(), rust_trait: None },
            ],
        },
        InterfaceSpec {
            name: "scope_entry",
            id: 2005,
            field_prefix: "se",
            kind: "ScopeEntry",
            description: None,
            rust_trait_name: Some("ScopeEntry"),
            methods: vec![
                // Unified return via `DynamicWrap`. The trait method's
                // Rust signature stays `-> CodeValue` (impls keep their
                // original signatures, no churn). `CodeValue`'s
                // `DynamicWrap::mortar_type()` returns
                // `TypeCodeValueExternDynamic` — that IS the source-side
                // Type for this `ret_type`. The wrap/unwrap between
                // `CodeValue` and the v8 `CodeValueExtern` form is
                // handled by `<CodeValue as DynamicWrap>::from_v8 /
                // into_v8` — the user-defined v8 dispatcher (in
                // `UserDefinedScopeEntry::use_`) just calls `from_v8` on
                // the v8 result.
                MethodSpec {
                    name: "use_",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate::langhelp::CodeValue),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("code", quote!(crate::langhelp::Code)),
                        ],
                    }),
                },
            ],
        },
        InterfaceSpec {
            name: "target",
            id: 2006,
            field_prefix: "tg",
            kind: "Target",
            description: None,
            rust_trait_name: Some("Target"),
            // All `target` methods currently use `TypeValueDynamic` as both
            // their source-mortar AND Rust-trait return type. That's a
            // known design issue — see #23 in TaskList: the existing
            // dispatch bridge wants richer Rust types (`(Code, ScopeEntryId)`,
            // `TargetBindSub`, `EvalRes`) that don't yet have matching
            // `Type` impls. Until those wrappers exist, the unified type
            // here is the lowest common ancestor — `value` — and the bridge
            // is responsible for extracting the real Rust value from the
            // source-side return.
            methods: vec![
                // Unified via `DynamicWrap` directly on the natural Rust
                // tuple type `(Code, ScopeEntryId)` — no wrapper struct
                // exposed. `mortar_type()` derives the source-side
                // `tuple(option(code_dyn), string)` from
                // `Code::mortar_type()` and `ScopeEntryId::mortar_type()`.
                MethodSpec {
                    name: "bind",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!((crate::langhelp::Code, crate::langhelp::ScopeEntryId)),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("sub", quote!(crate::eval::TargetBindSub)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "bind_sub_record",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(quote!(crate::eval::TargetBindSub)),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("carry", quote!(crate::langhelp::Code)),
                            ("fields", quote!(Vec<(crate::langhelp::ConstKey, crate::eval::TargetBindSub)>)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "bind_sub_tuple",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(quote!(crate::eval::TargetBindSub)),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("carry", quote!(crate::langhelp::Code)),
                            ("elements", quote!(Vec<crate::eval::TargetBindSub>)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "block",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate::langhelp::EResult<crate::langhelp::Code>),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("entries", quote!(Vec<crate::langhelp::Code>)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "code_to_bind_sub",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate::langhelp::EResult<crate::eval::TargetBindSub>),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("code", quote!(crate::langhelp::Code)),
                            ("type_", quote!(std::rc::Rc<dyn crate::langhelp::Type>)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "if_",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate::langhelp::EResult<crate::langhelp::CodeValue>),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("conditions", quote!(Vec<(crate::langhelp::CodeValue, &crate::eval::expr::Expr)>)),
                            ("else_", quote!(Option<&crate::eval::expr::Expr>)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "match_",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate::langhelp::EResult<crate::langhelp::CodeValue>),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("base", quote!(crate::langhelp::CodeValue)),
                            ("element_key", quote!(crate::langhelp::ConstKey)),
                            ("cases", quote!(Vec<(String, std::rc::Rc<dyn crate::langhelp::Type>, &crate::eval::expr::Expr)>)),
                            ("fallback", quote!(Option<&crate::eval::expr::Expr>)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "maybe_bind_sub",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate::langhelp::EResult<Option<crate::eval::TargetBindSub>>),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("cv", quote!(crate::langhelp::CodeValue)),
                        ],
                    }),
                },
                MethodSpec {
                    name: "stage",
                    arg_type: any(),
                    ret_type: TypeRef::via_dynamic_wrap(
                        quote!(crate::langhelp::EResult<crate::langhelp::CodeValue>),
                    ),
                    rust_trait: Some(RustTrait {
                        params: vec![
                            ("xx", quote!(&crate::langhelp::ExprContext)),
                            ("loc", quote!(crate::langhelp::QExprId)),
                            ("tree", quote!(crate::eval::expr::Expr)),
                            ("lower_values", quote!(Vec<crate::langhelp::CodeValue>)),
                        ],
                    }),
                },
            ],
        },
        InterfaceSpec {
            name: "code_dyn",
            id: 2007,
            field_prefix: "cd",
            kind: "CodeDyn",
            description: None,
            rust_trait_name: None,
            methods: vec![],
        },
    ]
}

fn check_no_duplicates(ifaces: &[InterfaceSpec]) {
    let mut names = std::collections::HashSet::new();
    let mut prefixes = std::collections::HashSet::new();
    let mut kinds = std::collections::HashSet::new();
    for i in ifaces {
        assert!(names.insert(i.name), "duplicate interface name: {}", i.name);
        assert!(prefixes.insert(i.field_prefix), "duplicate field_prefix: {}", i.field_prefix);
        assert!(kinds.insert(i.kind), "duplicate kind: {}", i.kind);
        let mut method_names = std::collections::HashSet::new();
        for m in &i.methods {
            assert!(
                method_names.insert(m.name),
                "duplicate method name on iface {}: {}",
                i.name,
                m.name,
            );
        }
    }
}

/// Per-Type identity markers — small constants used by individual `Type`
/// impls' `aspect()` to answer "is this my own type?". Each is a
/// `pub(crate) fn <name>() -> Unique { Unique::Builtin(<id>) }` generated
/// into `gen_local_identities.rs`. Originals have been removed; callers
/// reference these via `crate::gen::<name>()`.
fn local_identities() -> Vec<(&'static str, usize)> {
    vec![
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
    ]
}

fn generate_local_identities(items: &[(&'static str, usize)]) -> TokenStream {
    let fns: Vec<TokenStream> = items
        .iter()
        .map(|(name, id)| {
            let name = format_ident!("{}", name);
            let id = *id;
            quote! {
                #[allow(dead_code)]
                pub(crate) fn #name() -> crate::langhelp::Unique {
                    crate::langhelp::Unique::Builtin(#id)
                }
            }
        })
        .collect();
    quote! { #(#fns)* }
}

/// Builtin artifact specs — one per artifact-implementing type. Each entry
/// generates a `pub(crate) fn <name>_artifact_id() -> ArtifactIdBuiltin` and,
/// when `type_path` is set, an `impl` block on the named type with
/// `artifact_id()`, `build_artifact(data)`, `build_type_artifact()` and
/// (when `register` is true) a `register(m)` method. Replaces the
/// `generate_builtin_artifact!` / `_type!` macros.
struct ArtifactSpec {
    /// snake_case slug used in the generated `<name>_artifact_id` function
    /// name. Doubles as the human label in the duplicate-id check.
    name: &'static str,
    id: usize,
    /// Absolute path of the type that gets the `impl` block. `None` for
    /// free-standing ids (e.g. `defset` — used by a hand-written wrapper
    /// function rather than via the `Self::artifact_id()` pattern).
    type_path: Option<&'static str>,
    /// Absolute path of the data type for `build_artifact(data: <data_type>)`.
    /// Only meaningful when `type_path` is `Some`.
    data_type: Option<&'static str>,
    /// When true, also generate the `register(m)` method (the `_type!` macro's
    /// extra body).
    register: bool,
}

fn artifact_ids() -> Vec<ArtifactSpec> {
    vec![
        // Free-standing id used by `definition_set::defset_artifact_identity()`.
        ArtifactSpec {
            name: "defset",
            id: 7001,
            type_path: None,
            data_type: None,
            register: false,
        },
        // _ artifact-only types (no `register`, no `_type` variant) _
        ArtifactSpec {
            name: "type_record_artifact",
            id: 7002,
            type_path: Some("crate::eval::value::record::artifact::TypeRecordArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
        },
        ArtifactSpec {
            name: "type_tuple_artifact",
            id: 7003,
            type_path: Some("crate::eval::value::tuple::artifact::TypeTupleArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
        },
        ArtifactSpec {
            name: "type_map_artifact",
            id: 7004,
            type_path: Some("crate::eval::value::type_map::artifact::TypeMapArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
        },
        ArtifactSpec {
            name: "type_option_artifact",
            id: 7005,
            type_path: Some("crate::eval::value::type_option::artifact::TypeOptionArtifact"),
            data_type: Some("serde_json::Value"),
            register: false,
        },
        ArtifactSpec {
            name: "type_source_interface_const_artifact",
            id: 7006,
            type_path: Some(
                "crate::eval::value::type_source_interface::artifact::TypeSourceInterfaceConstArtifact",
            ),
            data_type: Some(
                "crate::eval::value::type_source_interface::BuiltinInterfaceKind",
            ),
            register: false,
        },
        // _ artifact-type types (with `register`) _
        ArtifactSpec {
            name: "type_type_source_data_type_constructor_dynamic",
            id: 7007,
            type_path: Some(
                "crate::eval::value::type_type_source_data_type_constructor::dynamic::TypeTypeSourceDataTypeConstructorDynamic",
            ),
            data_type: Some("serde_json::Value"),
            register: true,
        },
        ArtifactSpec {
            name: "type_code_value_extern_dynamic",
            id: 7008,
            type_path: Some("crate::eval::code_value_extern::TypeCodeValueExternDynamic"),
            data_type: Some("()"),
            register: true,
        },
        ArtifactSpec {
            name: "type_type_source_data_type_dynamic",
            id: 7009,
            type_path: Some(
                "crate::eval::value::type_type_source_data_type::dynamic::TypeTypeSourceDataTypeDynamic",
            ),
            data_type: Some("serde_json::Value"),
            register: true,
        },
        ArtifactSpec {
            name: "type_null_const",
            id: 7010,
            type_path: Some("crate::eval::value::type_null::r#const::TypeNullConst"),
            data_type: Some("crate::eval::value::type_null::artifact::ArtifactDataNull"),
            register: true,
        },
        ArtifactSpec {
            name: "type_any_const",
            id: 7011,
            type_path: Some("crate::eval::value::type_any::r#const::TypeAnyConst"),
            data_type: Some("serde_json::Value"),
            register: true,
        },
        ArtifactSpec {
            name: "type_string_const",
            id: 7012,
            type_path: Some("crate::eval::value::type_string::r#const::TypeStringConst"),
            data_type: Some(
                "crate::eval::value::type_string::artifact::ArtifactDataString",
            ),
            register: true,
        },
        ArtifactSpec {
            name: "type_null_dynamic",
            id: 7013,
            type_path: Some("crate::eval::value::type_null::dynamic::TypeNullDynamic"),
            data_type: Some("crate::eval::value::type_null::artifact::ArtifactDataNull"),
            register: true,
        },
        ArtifactSpec {
            name: "type_source_tagged_union_ref_dynamic",
            id: 7014,
            type_path: Some(
                "crate::eval::value::type_source_tagged_union::dynamic::TypeSourceTaggedUnionRefDynamic",
            ),
            data_type: Some("serde_json::Value"),
            register: true,
        },
        ArtifactSpec {
            name: "type_source_data_type_ref_dynamic",
            id: 7015,
            type_path: Some(
                "crate::eval::value::type_source_data_type::dynamic::TypeSourceDataTypeRefDynamic",
            ),
            data_type: Some("serde_json::Value"),
            register: true,
        },
        ArtifactSpec {
            name: "type_number_const",
            id: 7016,
            type_path: Some("crate::eval::value::type_number::r#const::TypeNumberConst"),
            data_type: Some("f64"),
            register: true,
        },
        ArtifactSpec {
            name: "type_type_source_function_dynamic",
            id: 7017,
            type_path: Some(
                "crate::eval::value::type_source_function::dynamic::TypeTypeSourceFunctionDynamic",
            ),
            data_type: Some("serde_json::Value"),
            register: true,
        },
        ArtifactSpec {
            name: "type_expr",
            id: 7018,
            type_path: Some("crate::eval::value::type_expr::TypeExpr"),
            data_type: Some("serde_json::Value"),
            register: true,
        },
        ArtifactSpec {
            name: "type_string_dynamic",
            id: 7019,
            type_path: Some("crate::eval::value::type_string::dynamic::TypeStringDynamic"),
            data_type: Some(
                "crate::eval::value::type_string::artifact::ArtifactDataString",
            ),
            register: true,
        },
        ArtifactSpec {
            name: "type_number_dynamic",
            id: 7020,
            type_path: Some("crate::eval::value::type_number::dynamic::TypeNumberDynamic"),
            data_type: Some("f64"),
            register: true,
        },
        ArtifactSpec {
            name: "type_list_artifact",
            id: 7021,
            type_path: Some("crate::eval::value::type_list::artifact::TypeListArtifact"),
            data_type: Some("crate::eval::value::type_list::artifact::ArtifactListType"),
            register: false,
        },
    ]
}

fn generate_artifact_ids(items: &[ArtifactSpec]) -> TokenStream {
    let pieces: Vec<TokenStream> = items
        .iter()
        .map(|s| {
            let id_fn = format_ident!("{}_artifact_id", s.name);
            let id = s.id;
            let id_decl = quote! {
                #[allow(dead_code)]
                pub(crate) fn #id_fn() -> crate::langhelp::ArtifactIdBuiltin {
                    crate::langhelp::ArtifactIdBuiltin(#id)
                }
            };
            let impl_block = match (s.type_path, s.data_type) {
                (Some(tp), Some(dt)) => {
                    let type_path: TokenStream = tp.parse().unwrap();
                    let data_type: TokenStream = dt.parse().unwrap();
                    let register = if s.register {
                        quote! {
                            #[allow(dead_code)]
                            pub(crate) fn register(
                                m:
                                    &mut std::collections::HashMap<
                                        crate::langhelp::ArtifactIdBuiltin,
                                        std::rc::Rc<dyn crate::langhelp::Type>,
                                    >,
                            ) {
                                m.insert(Self::artifact_id(), std::rc::Rc::new(Self));
                            }
                        }
                    } else {
                        quote! {}
                    };
                    quote! {
                        impl #type_path {
                            #[allow(dead_code)]
                            pub(crate) fn artifact_id() -> crate::langhelp::ArtifactIdBuiltin {
                                #id_fn()
                            }
                            #[allow(dead_code)]
                            pub(crate) fn build_artifact(data: #data_type) -> crate::langhelp::Artifact {
                                crate::langhelp::Artifact {
                                    type_: crate::langhelp::ArtifactId::Builtin(Self::artifact_id()),
                                    data: serde_json::to_value(data).unwrap(),
                                }
                            }
                            #[allow(dead_code)]
                            pub(crate) fn build_type_artifact() -> crate::langhelp::Artifact {
                                crate::langhelp::Artifact {
                                    type_: crate::langhelp::ArtifactId::Builtin(Self::artifact_id()),
                                    data: serde_json::Value::Null,
                                }
                            }
                            #register
                        }
                    }
                }
                _ => quote! {},
            };
            quote! { #id_decl #impl_block }
        })
        .collect();
    quote! { #(#pieces)* }
}

/// Global id-uniqueness check across every spec category. Each id appears
/// in exactly one place; collisions would silently make two distinct
/// `Unique::Builtin(N)`s look equal (or two distinct
/// `ScopeEntryId::Builtin(N)`s), so panic at build time if any match.
/// Reports the conflicting name/id pair so the spec entry to renumber is
/// obvious from the panic.
fn check_no_duplicate_ids(
    ifaces: &[InterfaceSpec],
    aspects: &[AspectSpec],
    dw_structs: &[DynWrapStructSpec],
    dw_wraps: &[DynWrapWrapSpec],
    dw_fns: &[DynWrapFnSpec],
    locals: &[(&'static str, usize)],
    artifacts: &[ArtifactSpec],
) {
    let mut seen = std::collections::HashMap::<usize, String>::new();
    let mut all: Vec<(usize, String)> = Vec::new();
    // `aspects` first; interface ids that intentionally alias an aspect's
    // id (e.g. source-level `aspect_function` iface = `AspectFunction`
    // aspect) are skipped. A clean global duplicate-check still runs across
    // the remaining categories.
    for a in aspects { all.push((a.id, format!("aspect:{}", a.name))); }
    for i in ifaces {
        if aspects.iter().any(|a| a.id == i.id) {
            // intentional alias — pinned by comment in `interfaces()` next to
            // the matching iface entry.
            continue;
        }
        all.push((i.id, format!("iface:{}", i.name)));
    }
    for s in dw_structs { all.push((s.id, format!("dyn_wrap_struct:{}", s.name))); }
    for s in dw_wraps { all.push((s.id, format!("dyn_wrap_wrap:{}", s.name))); }
    for s in dw_fns { all.push((s.id, format!("dyn_wrap_fn:{}", s.name))); }
    for (name, id) in locals { all.push((*id, format!("local:{}", name))); }
    for a in artifacts { all.push((a.id, format!("artifact:{}", a.name))); }
    for (id, label) in all {
        if let Some(existing) = seen.insert(id, label.clone()) {
            panic!("duplicate spec id {}: '{}' vs '{}'", id, existing, label);
        }
    }
}

fn generate(ifaces: &[InterfaceSpec]) -> TokenStream {
    let kind_variants: Vec<_> = ifaces.iter().map(|i| format_ident!("{}", i.kind)).collect();

    let methods_per_iface: Vec<TokenStream> = ifaces
        .iter()
        .map(|i| {
            let prefix = i.field_prefix;
            let items: Vec<TokenStream> = i
                .methods
                .iter()
                .enumerate()
                .map(|(idx, m)| {
                    let name = m.name;
                    let field_name = format!("_im_{}{}", prefix, idx);
                    let arg = &m.arg_type.mortar;
                    let ret = &m.ret_type.mortar;
                    quote! {
                        crate::eval::value::type_source_interface::SourceInterfaceMethodData {
                            name: #name.to_string(),
                            arg_type: std::cell::RefCell::new(Some(#arg)),
                            ret_type: std::cell::RefCell::new(Some(#ret)),
                            field_name: #field_name.to_string(),
                        }
                    }
                })
                .collect();
            quote! {
                vec![ #(#items),* ]
            }
        })
        .collect();

    // No generated identity fns — each interface reuses its existing
    // identity function via the spec's `identity_fn` path.
    let identity_fns: Vec<TokenStream> = vec![];

    let struct_fields: Vec<TokenStream> = ifaces
        .iter()
        .map(|i| {
            let f = format_ident!("{}", if i.name == "type" { "type_" } else { i.name });
            quote! { pub #f: dumpster::unsync::Gc<crate::eval::value::type_source_interface::SourceInterfaceData> }
        })
        .collect();

    let build_entries: Vec<TokenStream> = ifaces
        .iter()
        .enumerate()
        .map(|(i, iface)| {
            let f = format_ident!("{}", if iface.name == "type" { "type_" } else { iface.name });
            let kind = format_ident!("{}", iface.kind);
            let description = iface.description.unwrap_or(iface.name);
            let methods_expr = &methods_per_iface[i];
            let id = iface.id;
            quote! {
                let #f = dumpster::unsync::Gc::new(
                    crate::eval::value::type_source_interface::SourceInterfaceData {
                        builtin_kind: Some(
                            crate::eval::value::type_source_interface::BuiltinInterfaceKind::#kind,
                        ),
                        set: builtin_def_set.clone(),
                        description: std::cell::RefCell::new(Some(#description.to_string())),
                        is_aspect: std::cell::Cell::new(false),
                        methods: std::cell::RefCell::new(#methods_expr),
                        identity: crate::langhelp::Unique::Builtin(#id),
                    },
                );
                builtin_def_set.interfaces.borrow_mut().push(#f.clone());
            }
        })
        .collect();

    let result_fields: Vec<TokenStream> = ifaces
        .iter()
        .map(|i| {
            let f = format_ident!("{}", if i.name == "type" { "type_" } else { i.name });
            quote! { #f: #f }
        })
        .collect();

    let trait_defs: Vec<TokenStream> = ifaces
        .iter()
        .filter_map(|i| {
            let trait_name = format_ident!("{}", i.rust_trait_name?);
            let methods: Vec<TokenStream> = i
                .methods
                .iter()
                .filter_map(|m| {
                    let rt = m.rust_trait.as_ref()?;
                    // Source method names like "type", "if_", "match_",
                    // "use_" all need to be valid Rust idents on the trait.
                    // `if_`/`match_`/`use_` already are; map `type` →
                    // `type_` (the existing trait used `type_` too).
                    let method_name = format_ident!("{}", if m.name == "type" { "type_" } else { m.name });
                    let params: Vec<TokenStream> = rt
                        .params
                        .iter()
                        .map(|(n, ty)| {
                            let name = format_ident!("{}", n);
                            quote! { #name: #ty }
                        })
                        .collect();
                    // Single source of truth: trait return = `ret_type.path`.
                    let ret = &m.ret_type.path;
                    Some(quote! {
                        fn #method_name(&self, #(#params),*) -> #ret;
                    })
                })
                .collect();
            Some(quote! {
                pub(crate) trait #trait_name {
                    #(#methods)*
                }
            })
        })
        .collect();

    quote! {
        // ---- Generated by build.rs from interface specs. ----
        // Do not edit; modify build.rs::interfaces() instead.

        #(#trait_defs)*

        pub(crate) struct GenBuiltinInterfaces {
            #(#struct_fields),*
        }

        #(#identity_fns)*

        pub(crate) fn gen_build_builtin_interfaces(
            import_state: crate::langhelp::ImportStateId,
        ) -> GenBuiltinInterfaces {
            let builtin_def_set = dumpster::unsync::Gc::new(
                crate::eval::definition_set::DefinitionSet::new(
                    import_state,
                    crate::langhelp::DefSetId(usize::MAX),
                    crate::langhelp::SourceId(usize::MAX),
                ),
            );
            #(#build_entries)*
            GenBuiltinInterfaces {
                #(#result_fields),*
            }
        }
    }
}

/// Specification for an aspect trait (formerly produced by the `aspect!`
/// macro). The build emits the trait, the `aspect_<name>_aspect_id()`
/// identity function, and the `_get` / `_get_maybe` helpers.
struct AspectSpec {
    /// Trait name, PascalCase (e.g. `"AspectType"`).
    name: &'static str,
    /// Explicit `Unique::Builtin(id)` for this aspect's identity.
    /// Globally unique (verified by `check_no_duplicate_ids`).
    id: usize,
    /// Trait methods. Empty `Vec` for marker aspects like `AspectNull`.
    methods: Vec<AspectMethod>,
}

struct AspectMethod {
    name: &'static str,
    /// `(name, rust_type)`. Receiver `&self` is added implicitly.
    params: Vec<(&'static str, &'static str)>,
    /// Rust return type. Empty string = `-> ()`.
    ret: &'static str,
}

fn aspects() -> Vec<AspectSpec> {
    vec![
        AspectSpec { name: "AspectNull", id: 1001, methods: vec![] },
        AspectSpec { name: "AspectRecord", id: 1002, methods: vec![] },
        AspectSpec { name: "AspectNumberDynamic", id: 1003, methods: vec![] },
        AspectSpec { name: "AspectBoolDynamic", id: 1004, methods: vec![] },
        AspectSpec { name: "AspectStringDynamic", id: 1005, methods: vec![] },
        AspectSpec {
            name: "AspectBoolConst",
            id: 1006,
            methods: vec![AspectMethod {
                name: "get",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "bool",
            }],
        },
        AspectSpec {
            name: "AspectNumberConst",
            id: 1008,
            methods: vec![AspectMethod {
                name: "get",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "f64",
            }],
        },
        AspectSpec {
            name: "AspectStringConst",
            id: 1009,
            methods: vec![AspectMethod {
                name: "get",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "String",
            }],
        },
        AspectSpec {
            name: "AspectType",
            id: 1010,
            methods: vec![AspectMethod {
                name: "type_",
                params: vec![("value", "&dyn crate::langhelp::Value")],
                ret: "std::rc::Rc<dyn crate::langhelp::Type>",
            }],
        },
        AspectSpec {
            name: "AspectListDynamic",
            id: 1011,
            methods: vec![AspectMethod {
                name: "element_type",
                params: vec![],
                ret: "std::rc::Rc<dyn crate::langhelp::Type>",
            }],
        },
        AspectSpec {
            name: "AspectMapDynamic",
            id: 1012,
            methods: vec![
                AspectMethod {
                    name: "key_type",
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
                AspectMethod {
                    name: "value_type",
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
            ],
        },
        AspectSpec {
            name: "AspectTuple",
            id: 1013,
            methods: vec![
                AspectMethod {
                    name: "element_types",
                    params: vec![],
                    ret: "Vec<std::rc::Rc<dyn crate::langhelp::Type>>",
                },
                AspectMethod {
                    name: "take_elements",
                    params: vec![("value", "std::rc::Rc<dyn crate::langhelp::Value>")],
                    ret: "Vec<crate::langhelp::EvalRes>",
                },
            ],
        },
        AspectSpec {
            name: "AspectTaggedUnion",
            id: 1014,
            methods: vec![AspectMethod {
                name: "variants",
                params: vec![],
                ret: "Vec<(crate::langhelp::ConstKey, String, std::rc::Rc<dyn crate::langhelp::Type>)>",
            }],
        },
        AspectSpec {
            name: "AspectField",
            id: 1015,
            methods: vec![
                AspectMethod {
                    name: "field",
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
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("key", "&crate::langhelp::ConstKey"),
                    ],
                    ret: "Option<std::rc::Rc<dyn crate::langhelp::Type>>",
                },
            ],
        },
        AspectSpec {
            name: "AspectFunction",
            id: 1016,
            methods: vec![AspectMethod {
                name: "call",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("self_", "crate::langhelp::CodeValue"),
                    ("argument", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EvalRes",
            }],
        },
        AspectSpec {
            name: "AspectSet",
            id: 1017,
            methods: vec![AspectMethod {
                name: "set",
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
            name: "AspectOperatorAdd",
            id: 1018,
            methods: vec![AspectMethod {
                name: "add",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("base", "crate::langhelp::CodeValue"),
                    ("reference", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EvalRes",
            }],
        },
        AspectSpec {
            name: "AspectOperatorEq",
            id: 1019,
            methods: vec![AspectMethod {
                name: "eq",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("base", "crate::langhelp::CodeValue"),
                    ("reference", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EvalRes",
            }],
        },
        AspectSpec {
            name: "AspectOperatorLt",
            id: 1020,
            methods: vec![AspectMethod {
                name: "lt",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("base", "crate::langhelp::CodeValue"),
                    ("reference", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EvalRes",
            }],
        },
        AspectSpec {
            name: "AspectInitialBind",
            id: 1021,
            methods: vec![AspectMethod {
                name: "initial_bind",
                params: vec![("dynamic_key", "crate::langhelp::ScopeEntryId")],
                ret: "Box<dyn crate::r#gen::ScopeEntry>",
            }],
        },
        AspectSpec {
            name: "AspectBind",
            id: 1022,
            methods: vec![AspectMethod {
                name: "bind",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("cv", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)>",
            }],
        },
        AspectSpec {
            name: "AspectBindDynamic",
            id: 1023,
            methods: vec![AspectMethod {
                name: "bind_dynamic",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("cv", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EResult<Option<crate::eval::TargetBindSub>>",
            }],
        },
        AspectSpec {
            name: "AspectConsumeDynamic",
            id: 1024,
            methods: vec![AspectMethod {
                name: "to_dynamic",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("loc", "crate::langhelp::QExprId"),
                    ("cv", "crate::langhelp::CodeValue"),
                ],
                ret: "crate::langhelp::EResult<crate::code_dynamic::CodeDynamic>",
            }],
        },
        AspectSpec {
            name: "AspectStackValueDynamic",
            id: 1025,
            methods: vec![AspectMethod {
                name: "to_value",
                params: vec![
                    ("loc", "crate::langhelp::QExprId"),
                    ("self_", "crate::code_dynamic::CodeDynamic"),
                ],
                ret: "crate::langhelp::CodeValue",
            }],
        },
        AspectSpec {
            name: "AspectLiftDynamic",
            id: 1026,
            methods: vec![AspectMethod {
                name: "lift",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("scope", "&v8::PinScope"),
                    ("self_", "v8::Local<v8::Value>"),
                ],
                ret: "Result<std::rc::Rc<dyn crate::langhelp::Value>, String>",
            }],
        },
        // `AspectLowerDynamic` — almost identical to `AspectLiftDynamic`,
        // but does NOT finalize the definition set. Used by staging /
        // lowering paths where the def-set may still be in flight (sibling
        // function impls being set). `AspectLiftDynamic` remains the
        // end-of-eval path and freezes the def-set.
        AspectSpec {
            name: "AspectLowerDynamic",
            id: 1031,
            methods: vec![AspectMethod {
                name: "lower",
                params: vec![
                    ("xx", "&crate::langhelp::ExprContext"),
                    ("scope", "&v8::PinScope"),
                    ("self_", "v8::Local<v8::Value>"),
                ],
                ret: "Result<std::rc::Rc<dyn crate::langhelp::Value>, String>",
            }],
        },
        AspectSpec {
            name: "AspectIterable",
            id: 1027,
            methods: vec![
                AspectMethod {
                    name: "element_type",
                    params: vec![],
                    ret: "std::rc::Rc<dyn crate::langhelp::Type>",
                },
                AspectMethod {
                    name: "foreach",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("base", "crate::langhelp::CodeValue"),
                        ("element_key", "crate::langhelp::ScopeEntryId"),
                        ("body", "crate::langhelp::CodeValue"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "AspectArtifact",
            id: 1028,
            methods: vec![
                AspectMethod {
                    name: "to_serial",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("self_", "std::rc::Rc<dyn crate::langhelp::Value>"),
                    ],
                    ret: "crate::langhelp::EResult<crate::langhelp::Artifact>",
                },
                AspectMethod {
                    name: "from_serial",
                    params: vec![
                        ("xx", "&crate::langhelp::ExprContext"),
                        ("loc", "crate::langhelp::QExprId"),
                        ("data", "&serde_json::Value"),
                    ],
                    ret: "crate::langhelp::EvalRes",
                },
            ],
        },
        AspectSpec {
            name: "AspectAspect",
            id: 1029,
            methods: vec![
                AspectMethod {
                    name: "get_unique",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "crate::langhelp::Unique",
                },
                AspectMethod {
                    name: "get_iface_data",
                    params: vec![("value", "&dyn crate::langhelp::Value")],
                    ret: "dumpster::unsync::Gc<crate::eval::value::type_source_interface::SourceInterfaceData>",
                },
            ],
        },
    ]
}

fn check_no_duplicate_aspects(aspects: &[AspectSpec]) {
    let mut names = std::collections::HashSet::new();
    for a in aspects {
        assert!(names.insert(a.name), "duplicate aspect name: {}", a.name);
    }
}

fn snake_case(s: &str) -> String {
    let mut out = String::new();
    for (i, c) in s.chars().enumerate() {
        if c.is_uppercase() && i > 0 {
            out.push('_');
        }
        out.push(c.to_ascii_lowercase());
    }
    out
}

fn generate_aspects(aspects: &[AspectSpec]) -> TokenStream {
    let items: Vec<TokenStream> = aspects.iter().map(|a| {
        let trait_name = format_ident!("{}", a.name);
        let snake = snake_case(a.name);
        let id_fn = format_ident!("{}_aspect_id", snake);
        let get_fn = format_ident!("{}_get", snake);
        let get_maybe_fn = format_ident!("{}_get_maybe", snake);
        let id = a.id;
        let trait_methods: Vec<TokenStream> = a.methods.iter().map(|m| {
            let name = format_ident!("{}", m.name);
            let params: Vec<TokenStream> = m.params.iter().map(|(n, ty)| {
                let n = format_ident!("{}", n);
                let ty: TokenStream = ty.parse().unwrap();
                quote! { #n: #ty }
            }).collect();
            let ret: TokenStream = if m.ret.is_empty() {
                quote!{}
            } else {
                let ty: TokenStream = m.ret.parse().unwrap();
                quote! { -> #ty }
            };
            quote! { fn #name(&self, #(#params),*) #ret; }
        }).collect();
        let missing_msg = format!("Type ({{}}) is missing aspect {}", a.name);
        quote! {
            #[allow(dead_code)]
            pub(crate) trait #trait_name: crate::langhelp::Aspect {
                #(#trait_methods)*
            }
            #[allow(dead_code)]
            pub(crate) fn #id_fn() -> crate::langhelp::Unique {
                crate::langhelp::Unique::Builtin(#id)
            }
            #[allow(dead_code)]
            pub(crate) fn #get_fn(
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                v: &dyn crate::langhelp::Type,
            ) -> crate::langhelp::EResult<Box<dyn #trait_name>> {
                let Some(out) = v.aspect(xx, &#id_fn()) else {
                    return crate::langhelp::EResult::err(loc, format!(#missing_msg, v.desc()));
                };
                return crate::langhelp::EResult::ok(
                    crate::invariant::must_downcast_box::<Box<dyn #trait_name>>(out)
                );
            }
            #[allow(dead_code)]
            pub(crate) fn #get_maybe_fn(
                xx: &crate::langhelp::ExprContext,
                v: &dyn crate::langhelp::Type,
            ) -> Option<Box<dyn #trait_name>> {
                let Some(out) = v.aspect(xx, &#id_fn()) else {
                    return None;
                };
                return Some(crate::invariant::must_downcast_box::<Box<dyn #trait_name>>(out));
            }
        }
    }).collect();
    quote! {
        // ---- Generated aspect traits + helpers. ----
        #(#items)*
    }
}

// ============================================================================
// dynamic_wrap struct generation. Replaces the `dynamic_wrap!` macro's struct
// variant: declares a Rust struct with `data` fields and a v8-facing type
// wrapper that exposes computed `fields` and callable `methods`. Each
// invocation emits the same constellation the macro used to: struct,
// `TypeXxxDynamic`, identity fn, `Value`/`Type`/`Aspect*` trait impls,
// `AspectField` with name→idx dispatch, and `DynamicWrap` impl whose
// `into_v8` builds a JS object containing the fields + method closures
// keyed `_0`, `_1`, etc.
//
// Bodies (`field` accessor and `method` impl) are `TokenStream` values —
// `quote! { … }` blocks in the spec — so the actual Rust executes when
// the generated impl is compiled, and `build.rs` itself only validates
// shape (param/return types parse as rust syntax).
// ============================================================================

struct DynWrapStructSpec {
    /// Struct name (PascalCase). Produces `struct <name>` and
    /// `struct Type<name>Dynamic`.
    name: &'static str,
    /// Explicit `Unique::Builtin(id)` for this struct's type identity.
    id: usize,
    /// `data` fields stored on the Rust struct. `(name, rust_type)`.
    data_fields: Vec<(&'static str, &'static str)>,
    /// `field <name>: <ret_type> { <body> }` — computed at `into_v8` time,
    /// exposed as a JS object property `_<idx>` (idx is the field's
    /// position in declaration order across fields+methods).
    fields: Vec<DynWrapField>,
    /// `fn <name>(<args>) -> <ret> { <body> }` — exposed as a callable
    /// JS function on the wrapper object.
    methods: Vec<DynWrapMethod>,
}

struct DynWrapField {
    name: &'static str,
    /// Rust return type — must impl `DynamicWrap` so `into_v8` can wrap it
    /// (its `mortar_type()` is also used for the source-side type).
    ret: &'static str,
    /// Body — `self_: Rc<Struct>` and `scope: &mut v8::PinnedRef<…>` are
    /// in scope (from the macro's `($self_, $scope)` binders).
    body: TokenStream,
}

struct DynWrapMethod {
    name: &'static str,
    /// `(name, rust_type)`. Each type must impl `DynamicWrap` (so
    /// `from_v8` can decode the call arg).
    args: Vec<(&'static str, &'static str)>,
    /// Rust return type — must impl `DynamicWrap`.
    ret: &'static str,
    /// Body — `self_: Rc<Struct>`, `scope`, and the decoded args are in
    /// scope.
    body: TokenStream,
}

fn generate_dyn_wrap_struct(s: &DynWrapStructSpec) -> TokenStream {
    let name = format_ident!("{}", s.name);
    let type_name = format_ident!("Type{}Dynamic", s.name);
    let identity_fn = format_ident!("{}_type_identity", snake_case(s.name));
    let id = s.id;
    let desc_str = s.name.to_string();

    let data_field_decls: Vec<TokenStream> = s
        .data_fields
        .iter()
        .map(|(n, ty)| {
            let n = format_ident!("{}", n);
            let ty: TokenStream = ty.parse().unwrap();
            quote! { pub #n: #ty }
        })
        .collect();

    // For `AspectField::field` / `field_type_opt`: each field+method gets
    // an `_<idx>` JS key. The dynamic_wrap_method_name() helper strips a
    // trailing underscore (e.g. `is_some_` → `is_some`) for the source-
    // visible name.
    let field_dispatch_arms: Vec<TokenStream> = s
        .fields
        .iter()
        .enumerate()
        .map(|(idx, f)| {
            let f_name = f.name;
            let f_ty: TokenStream = f.ret.parse().unwrap();
            let idx_str = format!("_{}", idx);
            quote! {
                if key_name == crate::eval::dynamic_wrap::dynamic_wrap_method_name(#f_name) {
                    let field_type: std::rc::Rc<dyn crate::langhelp::Type> =
                        <#f_ty as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type();
                    let self_code = erb
                        .must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?
                        .unwrap()
                        .clone();
                    return Ok(crate::langhelp::CodeValue {
                        code: crate::langhelp::Code {
                            loc,
                            code: crate::langhelp::Code_::Dyn(std::rc::Rc::new(
                                crate::code_dynamic::CodeDynamic {
                                    loc,
                                    type_: crate::code_dynamic::CodeDynamicType::FieldUse(
                                        crate::code_dynamic::CodeDynamicFieldUse {
                                            base: Box::new(self_code),
                                            field: #idx_str.to_string(),
                                        },
                                    ),
                                },
                            )),
                        },
                        value: std::rc::Rc::new(crate::eval::dynamic_util::ValueDynamic {
                            source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                            type_: field_type,
                        }),
                    });
                }
            }
        })
        .collect();
    let method_dispatch_arms: Vec<TokenStream> = s
        .methods
        .iter()
        .enumerate()
        .map(|(i, m)| {
            let m_name = m.name;
            let idx = s.fields.len() + i;
            let idx_str = format!("_{}", idx);
            let arg_mortar = arg_mortar_expr(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote! {
                if key_name == crate::eval::dynamic_wrap::dynamic_wrap_method_name(#m_name) {
                    let field_type: std::rc::Rc<dyn crate::langhelp::Type> = std::rc::Rc::new(
                        crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
                            arg: #arg_mortar,
                            ret: <#m_ret as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type(),
                        },
                    );
                    let self_code = erb
                        .must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?
                        .unwrap()
                        .clone();
                    return Ok(crate::langhelp::CodeValue {
                        code: crate::langhelp::Code {
                            loc,
                            code: crate::langhelp::Code_::Dyn(std::rc::Rc::new(
                                crate::code_dynamic::CodeDynamic {
                                    loc,
                                    type_: crate::code_dynamic::CodeDynamicType::FieldUse(
                                        crate::code_dynamic::CodeDynamicFieldUse {
                                            base: Box::new(self_code),
                                            field: #idx_str.to_string(),
                                        },
                                    ),
                                },
                            )),
                        },
                        value: std::rc::Rc::new(crate::eval::dynamic_util::ValueDynamic {
                            source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                            type_: field_type,
                        }),
                    });
                }
            }
        })
        .collect();

    let field_type_opt_arms: Vec<TokenStream> = s
        .fields
        .iter()
        .map(|f| {
            let f_name = f.name;
            let f_ty: TokenStream = f.ret.parse().unwrap();
            quote! {
                if key.as_str() == crate::eval::dynamic_wrap::dynamic_wrap_method_name(#f_name) {
                    return Some(<#f_ty as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type());
                }
            }
        })
        .chain(s.methods.iter().map(|m| {
            let m_name = m.name;
            let arg_mortar = arg_mortar_expr(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote! {
                if key.as_str() == crate::eval::dynamic_wrap::dynamic_wrap_method_name(#m_name) {
                    return Some(std::rc::Rc::new(
                        crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
                            arg: #arg_mortar,
                            ret: <#m_ret as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type(),
                        },
                    ));
                }
            }
        }))
        .collect();

    // `into_v8` field setters and method callbacks.
    let field_setters: Vec<TokenStream> = s
        .fields
        .iter()
        .enumerate()
        .map(|(idx, f)| {
            let f_ty: TokenStream = f.ret.parse().unwrap();
            let body = &f.body;
            let key_str = format!("_{}", idx);
            quote! {
                {
                    #[allow(unused_variables)]
                    let self_ = _jw_self_rc.clone();
                    let _jw_field_val: #f_ty = { #body };
                    let _jw_v8_val =
                        <#f_ty as crate::eval::dynamic_wrap::DynamicWrap>::into_v8(_jw_field_val, scope);
                    let _jw_key = v8::String::new(scope, #key_str).unwrap();
                    _jw_obj.set(scope, _jw_key.into(), _jw_v8_val);
                }
            }
        })
        .collect();
    let method_setters: Vec<TokenStream> = s
        .methods
        .iter()
        .enumerate()
        .map(|(i, m)| {
            let idx = s.fields.len() + i;
            let key_str = format!("_{}", idx);
            let arg_decls: Vec<TokenStream> = m
                .args
                .iter()
                .map(|(n, ty)| {
                    let n = format_ident!("{}", n);
                    let ty: TokenStream = ty.parse().unwrap();
                    quote! {
                        let #n: #ty = <#ty as crate::eval::dynamic_wrap::DynamicWrap>::from_v8(
                            scope, _jw_args.get(_jw_arg_i),
                        );
                        _jw_arg_i += 1;
                    }
                })
                .collect();
            let m_ret: TokenStream = m.ret.parse().unwrap();
            let body = &m.body;
            quote! {
                {
                    fn _jw_cb<'s>(
                        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                        _jw_args: v8::FunctionCallbackArguments<'s>,
                        mut _jw_rv: v8::ReturnValue<'s>,
                    ) {
                        let _jw_this = _jw_args.this();
                        let _jw_dk = v8::String::new(scope, "_d").unwrap();
                        #[allow(unused_variables)]
                        let self_: std::rc::Rc<#name> =
                            crate::eval::from_v8_ref::<std::rc::Rc<#name>>(
                                &*_jw_this.get(scope, _jw_dk.into()).unwrap(),
                            )
                                .clone();
                        #[allow(unused_mut, unused_variables)]
                        let mut _jw_arg_i = 0i32;
                        #(#arg_decls)*
                        let _jw_result: #m_ret = { #body };
                        _jw_rv.set(
                            <#m_ret as crate::eval::dynamic_wrap::DynamicWrap>::into_v8(_jw_result, scope),
                        );
                    }
                    let _jw_method_fn = v8::Function::builder(_jw_cb).build(scope).unwrap();
                    let _jw_key = v8::String::new(scope, #key_str).unwrap();
                    _jw_obj.set(scope, _jw_key.into(), _jw_method_fn.into());
                }
            }
        })
        .collect();

    let no_fields_msg = format!("Cannot convert {}", s.name);
    let unknown_field_msg = format!("Unknown field '{{}}' on {}", s.name);
    let from_v8_msg = format!("from_v8 not supported for {}", s.name);
    let cannot_convert_to_msg = format!("Cannot convert to {}", s.name);

    quote! {
        pub(crate) struct #name {
            #(#data_field_decls,)*
        }
        pub(crate) struct #type_name;

        fn #identity_fn() -> crate::langhelp::Unique {
            crate::langhelp::Unique::Builtin(#id)
        }

        impl crate::langhelp::Value for #type_name {
            fn type_(
                &self,
                _xx: &crate::langhelp::ExprContext,
                _loc: crate::langhelp::QExprId,
            ) -> std::rc::Rc<dyn crate::langhelp::Type> {
                std::rc::Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
            }
        }

        impl crate::langhelp::Aspect for #type_name {}

        impl crate::langhelp::Type for #type_name {
            fn desc(&self) -> String { #desc_str.to_string() }
            fn as_value(&self) -> std::rc::Rc<dyn crate::langhelp::Value> {
                std::rc::Rc::new(#type_name)
            }
            fn aspect(
                &self,
                _xx: &crate::langhelp::ExprContext,
                key: &crate::langhelp::Unique,
            ) -> Option<Box<dyn std::any::Any>> {
                if *key == #identity_fn() {
                    return Some(Box::new(true));
                }
                if *key == crate::r#gen::aspect_field_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectField>));
                }
                if *key == crate::r#gen::aspect_consume_dynamic_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectConsumeDynamic>));
                }
                if *key == crate::r#gen::aspect_bind_dynamic_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectBindDynamic>));
                }
                if *key == crate::r#gen::aspect_bind_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectBind>));
                }
                if *key == crate::r#gen::aspect_stack_value_dynamic_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectStackValueDynamic>));
                }
                None
            }
            fn cast_to(
                &self,
                _xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                _cv: crate::langhelp::CodeValue,
                _type_: &dyn crate::langhelp::Type,
            ) -> crate::langhelp::EvalRes {
                crate::langhelp::EvalRes::err(loc, #no_fields_msg.to_string())
            }
            fn can_cast_from(
                &self,
                xx: &crate::langhelp::ExprContext,
                _loc: crate::langhelp::QExprId,
                source_type: &dyn crate::langhelp::Type,
            ) -> bool {
                source_type.aspect(xx, &#identity_fn()).is_some()
            }
            fn cast_from(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EvalRes {
                let source_type = cv.value.type_(xx, loc);
                if source_type.aspect(xx, &#identity_fn()).is_some() {
                    return crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
                        code: cv.code,
                        value: std::rc::Rc::new(crate::eval::dynamic_util::ValueDynamic {
                            source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                            type_: std::rc::Rc::new(#type_name),
                        }),
                    });
                }
                crate::langhelp::EvalRes::err(loc, #cannot_convert_to_msg.to_string())
            }
        }

        impl crate::r#gen::AspectConsumeDynamic for #type_name {
            fn to_dynamic(
                &self,
                _xx: &crate::langhelp::ExprContext,
                _loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EResult<crate::code_dynamic::CodeDynamic> {
                cv.code.cast::<crate::code_dynamic::CodeDynamic>().map(|x| x.unwrap().clone())
            }
        }

        impl crate::r#gen::AspectBindDynamic for #type_name {
            fn bind_dynamic(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EResult<Option<crate::eval::TargetBindSub>> {
                crate::eval::dynamic_util::bind_dynamic_default(xx, loc, cv)
            }
        }

        impl crate::r#gen::AspectBind for #type_name {
            fn bind(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)> {
                crate::eval::dynamic_util::bind_via_target(xx, loc, cv)
            }
        }

        impl crate::r#gen::AspectStackValueDynamic for #type_name {
            fn to_value(
                &self,
                loc: crate::langhelp::QExprId,
                value: crate::code_dynamic::CodeDynamic,
            ) -> crate::langhelp::CodeValue {
                crate::langhelp::CodeValue {
                    code: crate::langhelp::Code {
                        loc,
                        code: crate::langhelp::Code_::Dyn(std::rc::Rc::new(value)),
                    },
                    value: std::rc::Rc::new(crate::eval::dynamic_util::ValueDynamic {
                        source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                        type_: std::rc::Rc::new(#type_name),
                    }),
                }
            }
        }

        impl crate::r#gen::AspectField for #type_name {
            fn field(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                self_: crate::langhelp::CodeValue,
                key: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EvalRes {
                crate::er_seq_cv!(loc, erb, {
                    let key_type = key.value.type_(xx, loc);
                    let key_string = erb
                        .must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
                    let key_name = key_string.get(key.value.as_ref());
                    #(#field_dispatch_arms)*
                    #(#method_dispatch_arms)*
                    return erb.err_new(format!(#unknown_field_msg, key_name));
                })
            }
            fn field_type_opt(
                &self,
                _xx: &crate::langhelp::ExprContext,
                key: &crate::langhelp::ConstKey,
            ) -> Option<std::rc::Rc<dyn crate::langhelp::Type>> {
                let crate::langhelp::ConstKey::String(key) = key else { return None; };
                #(#field_type_opt_arms)*
                None
            }
        }

        impl crate::eval::dynamic_wrap::DynamicWrap for #name {
            fn mortar_type() -> std::rc::Rc<dyn crate::langhelp::Type> {
                std::rc::Rc::new(#type_name)
            }
            fn from_v8<'s>(
                _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                _value: v8::Local<'s, v8::Value>,
            ) -> Self {
                unimplemented!(#from_v8_msg)
            }
            fn into_v8<'s>(
                self,
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            ) -> v8::Local<'s, v8::Value> {
                let _jw_obj = v8::Object::new(scope);
                let _jw_self_rc = std::rc::Rc::new(self);
                let _jw_data_ext = crate::eval::to_v8(scope, _jw_self_rc.clone());
                _jw_obj.set(
                    scope,
                    v8::String::new(scope, "_d").unwrap().into(),
                    _jw_data_ext.into(),
                );
                #(#field_setters)*
                #(#method_setters)*
                _jw_obj.into()
            }
        }
    }
}

/// Spec for `dynamic_wrap!(wrap $type as $name …)` — wraps an existing type
/// rather than declaring a new struct. The Type wrapper's `aspect()` calls
/// `<name>_extra_aspect(_xx, key)` as a fallback so external code can add
/// aspects.
struct DynWrapWrapSpec {
    /// Wrapper name (PascalCase) — generates `TypeXxxDynamic` and the
    /// `<name>_type_identity`/`<name>_extra_aspect` functions are looked
    /// up via snake_case(`<name>`).
    name: &'static str,
    /// Explicit `Unique::Builtin(id)` for this wrapper's type identity.
    id: usize,
    /// The Rust type being wrapped (e.g. `"Gc<DefinitionSet>"`). Must
    /// already exist; the generator emits `impl DynamicWrap for <type>`.
    wrapped_type: &'static str,
    methods: Vec<DynWrapMethod>,
}

fn generate_dyn_wrap_wrap(s: &DynWrapWrapSpec) -> TokenStream {
    let type_name = format_ident!("Type{}Dynamic", s.name);
    let identity_fn = format_ident!("{}_type_identity", snake_case(s.name));
    let extra_aspect_fn = format_ident!("{}_extra_aspect", snake_case(s.name));
    let id = s.id;
    let desc_str = s.name.to_string();
    let wrapped: TokenStream = s.wrapped_type.parse().unwrap();

    let method_dispatch_arms: Vec<TokenStream> = s
        .methods
        .iter()
        .enumerate()
        .map(|(idx, m)| {
            let m_name = m.name;
            let idx_str = format!("_{}", idx);
            let arg_mortar = arg_mortar_expr(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote! {
                if key_name == crate::eval::dynamic_wrap::dynamic_wrap_method_name(#m_name) {
                    let field_type: std::rc::Rc<dyn crate::langhelp::Type> = std::rc::Rc::new(
                        crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
                            arg: #arg_mortar,
                            ret: <#m_ret as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type(),
                        },
                    );
                    let self_code = erb
                        .must_er(self_.code.cast::<crate::code_dynamic::CodeDynamic>())?
                        .unwrap()
                        .clone();
                    return Ok(crate::langhelp::CodeValue {
                        code: crate::langhelp::Code {
                            loc,
                            code: crate::langhelp::Code_::Dyn(std::rc::Rc::new(
                                crate::code_dynamic::CodeDynamic {
                                    loc,
                                    type_: crate::code_dynamic::CodeDynamicType::FieldUse(
                                        crate::code_dynamic::CodeDynamicFieldUse {
                                            base: Box::new(self_code),
                                            field: #idx_str.to_string(),
                                        },
                                    ),
                                },
                            )),
                        },
                        value: std::rc::Rc::new(crate::eval::dynamic_util::ValueDynamic {
                            source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                            type_: field_type,
                        }),
                    });
                }
            }
        })
        .collect();

    let field_type_opt_arms: Vec<TokenStream> = s
        .methods
        .iter()
        .map(|m| {
            let m_name = m.name;
            let arg_mortar = arg_mortar_expr(&m.args);
            let m_ret: TokenStream = m.ret.parse().unwrap();
            quote! {
                if key.as_str() == crate::eval::dynamic_wrap::dynamic_wrap_method_name(#m_name) {
                    return Some(std::rc::Rc::new(
                        crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
                            arg: #arg_mortar,
                            ret: <#m_ret as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type(),
                        },
                    ));
                }
            }
        })
        .collect();

    let method_setters: Vec<TokenStream> = s
        .methods
        .iter()
        .enumerate()
        .map(|(idx, m)| {
            let key_str = format!("_{}", idx);
            let arg_decls: Vec<TokenStream> = m
                .args
                .iter()
                .map(|(n, ty)| {
                    let n = format_ident!("{}", n);
                    let ty: TokenStream = ty.parse().unwrap();
                    quote! {
                        let #n: #ty = <#ty as crate::eval::dynamic_wrap::DynamicWrap>::from_v8(
                            scope, _jw_args.get(_jw_arg_i),
                        );
                        _jw_arg_i += 1;
                    }
                })
                .collect();
            let m_ret: TokenStream = m.ret.parse().unwrap();
            let body = &m.body;
            quote! {
                {
                    fn _jw_cb<'s>(
                        scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                        _jw_args: v8::FunctionCallbackArguments<'s>,
                        mut _jw_rv: v8::ReturnValue<'s>,
                    ) {
                        let _jw_this = _jw_args.this();
                        let _jw_dk = v8::String::new(scope, "_d").unwrap();
                        #[allow(unused_variables)]
                        let self_: std::rc::Rc<#wrapped> =
                            crate::eval::from_v8_ref::<std::rc::Rc<#wrapped>>(
                                &*_jw_this.get(scope, _jw_dk.into()).unwrap(),
                            )
                                .clone();
                        #[allow(unused_mut, unused_variables)]
                        let mut _jw_arg_i = 0i32;
                        #(#arg_decls)*
                        let _jw_result: #m_ret = { #body };
                        _jw_rv.set(
                            <#m_ret as crate::eval::dynamic_wrap::DynamicWrap>::into_v8(_jw_result, scope),
                        );
                    }
                    let _jw_method_fn = v8::Function::builder(_jw_cb).build(scope).unwrap();
                    let _jw_key = v8::String::new(scope, #key_str).unwrap();
                    _jw_obj.set(scope, _jw_key.into(), _jw_method_fn.into());
                }
            }
        })
        .collect();

    let cannot_convert_msg = format!("Cannot convert {}", s.name);
    let cannot_convert_to_msg = format!("Cannot convert to {}", s.name);
    let unknown_field_msg = format!("Unknown field '{{}}' on {}", s.name);
    let from_v8_msg = format!("from_v8 not supported for {}", s.name);

    quote! {
        pub(crate) struct #type_name;

        fn #identity_fn() -> crate::langhelp::Unique {
            crate::langhelp::Unique::Builtin(#id)
        }

        impl crate::langhelp::Value for #type_name {
            fn type_(
                &self,
                _xx: &crate::langhelp::ExprContext,
                _loc: crate::langhelp::QExprId,
            ) -> std::rc::Rc<dyn crate::langhelp::Type> {
                std::rc::Rc::new(crate::eval::value::type_any::r#const::TypeAnyConst)
            }
        }

        impl crate::langhelp::Aspect for #type_name {}

        impl crate::langhelp::Type for #type_name {
            fn desc(&self) -> String { #desc_str.to_string() }
            fn as_value(&self) -> std::rc::Rc<dyn crate::langhelp::Value> {
                std::rc::Rc::new(#type_name)
            }
            fn aspect(
                &self,
                _xx: &crate::langhelp::ExprContext,
                key: &crate::langhelp::Unique,
            ) -> Option<Box<dyn std::any::Any>> {
                if *key == #identity_fn() {
                    return Some(Box::new(true));
                }
                if *key == crate::r#gen::aspect_field_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectField>));
                }
                if *key == crate::r#gen::aspect_consume_dynamic_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectConsumeDynamic>));
                }
                if *key == crate::r#gen::aspect_bind_dynamic_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectBindDynamic>));
                }
                if *key == crate::r#gen::aspect_bind_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectBind>));
                }
                if *key == crate::r#gen::aspect_stack_value_dynamic_aspect_id() {
                    return Some(Box::new(Box::new(#type_name) as Box<dyn crate::r#gen::AspectStackValueDynamic>));
                }
                crate::eval::definition_set::#extra_aspect_fn(_xx, key)
            }
            fn cast_to(
                &self,
                _xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                _cv: crate::langhelp::CodeValue,
                _type_: &dyn crate::langhelp::Type,
            ) -> crate::langhelp::EvalRes {
                crate::langhelp::EvalRes::err(loc, #cannot_convert_msg.to_string())
            }
            fn can_cast_from(
                &self,
                xx: &crate::langhelp::ExprContext,
                _loc: crate::langhelp::QExprId,
                source_type: &dyn crate::langhelp::Type,
            ) -> bool {
                source_type.aspect(xx, &#identity_fn()).is_some()
            }
            fn cast_from(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EvalRes {
                let source_type = cv.value.type_(xx, loc);
                if source_type.aspect(xx, &#identity_fn()).is_some() {
                    return crate::langhelp::EvalRes::ok(crate::langhelp::CodeValue {
                        code: cv.code,
                        value: std::rc::Rc::new(crate::eval::dynamic_util::ValueDynamic {
                            source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                            type_: std::rc::Rc::new(#type_name),
                        }),
                    });
                }
                crate::langhelp::EvalRes::err(loc, #cannot_convert_to_msg.to_string())
            }
        }

        impl crate::r#gen::AspectConsumeDynamic for #type_name {
            fn to_dynamic(
                &self,
                _xx: &crate::langhelp::ExprContext,
                _loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EResult<crate::code_dynamic::CodeDynamic> {
                cv.code.cast::<crate::code_dynamic::CodeDynamic>().map(|x| x.unwrap().clone())
            }
        }

        impl crate::r#gen::AspectBindDynamic for #type_name {
            fn bind_dynamic(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EResult<Option<crate::eval::TargetBindSub>> {
                crate::eval::dynamic_util::bind_dynamic_default(xx, loc, cv)
            }
        }

        impl crate::r#gen::AspectBind for #type_name {
            fn bind(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                cv: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EResult<(crate::langhelp::Code, Box<dyn crate::r#gen::ScopeEntry>)> {
                crate::eval::dynamic_util::bind_via_target(xx, loc, cv)
            }
        }

        impl crate::r#gen::AspectStackValueDynamic for #type_name {
            fn to_value(
                &self,
                loc: crate::langhelp::QExprId,
                value: crate::code_dynamic::CodeDynamic,
            ) -> crate::langhelp::CodeValue {
                crate::langhelp::CodeValue {
                    code: crate::langhelp::Code {
                        loc,
                        code: crate::langhelp::Code_::Dyn(std::rc::Rc::new(value)),
                    },
                    value: std::rc::Rc::new(crate::eval::dynamic_util::ValueDynamic {
                        source: crate::eval::dynamic_util::ValueDynamicSource::Expr,
                        type_: std::rc::Rc::new(#type_name),
                    }),
                }
            }
        }

        impl crate::r#gen::AspectField for #type_name {
            fn field(
                &self,
                xx: &crate::langhelp::ExprContext,
                loc: crate::langhelp::QExprId,
                self_: crate::langhelp::CodeValue,
                key: crate::langhelp::CodeValue,
            ) -> crate::langhelp::EvalRes {
                crate::er_seq_cv!(loc, erb, {
                    let key_type = key.value.type_(xx, loc);
                    let key_string = erb
                        .must_er(crate::r#gen::aspect_string_const_get(xx, loc, key_type.as_ref()))?;
                    let key_name = key_string.get(key.value.as_ref());
                    #(#method_dispatch_arms)*
                    return erb.err_new(format!(#unknown_field_msg, key_name));
                })
            }
            fn field_type_opt(
                &self,
                _xx: &crate::langhelp::ExprContext,
                key: &crate::langhelp::ConstKey,
            ) -> Option<std::rc::Rc<dyn crate::langhelp::Type>> {
                let crate::langhelp::ConstKey::String(key) = key else { return None; };
                #(#field_type_opt_arms)*
                None
            }
        }

        impl crate::eval::dynamic_wrap::DynamicWrap for #wrapped {
            fn mortar_type() -> std::rc::Rc<dyn crate::langhelp::Type> {
                std::rc::Rc::new(#type_name)
            }
            fn from_v8<'s>(
                _scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                _value: v8::Local<'s, v8::Value>,
            ) -> Self {
                unimplemented!(#from_v8_msg)
            }
            fn into_v8<'s>(
                self,
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            ) -> v8::Local<'s, v8::Value> {
                let _jw_obj = v8::Object::new(scope);
                let _jw_self_rc = std::rc::Rc::new(self);
                let _jw_data_ext = crate::eval::to_v8(scope, _jw_self_rc.clone());
                _jw_obj.set(
                    scope,
                    v8::String::new(scope, "_d").unwrap().into(),
                    _jw_data_ext.into(),
                );
                #(#method_setters)*
                _jw_obj.into()
            }
        }
    }
}

fn dyn_wrap_wraps() -> Vec<DynWrapWrapSpec> {
    vec![
        // DefinitionSet wraps `Gc<DefinitionSet>` so the v8-side `define()`
        // result can dispatch `.new_func` / `.new_data` / `.tagged_union` /
        // `.new_interface` / `.build`. `definition_set_extra_aspect` adds
        // `AspectLiftDynamic` on top of the generator's default aspect set.
        DynWrapWrapSpec {
            name: "DefinitionSet",
            id: 4001,
            wrapped_type: "Gc<DefinitionSet>",
            methods: vec![
                DynWrapMethod {
                    name: "new_func",
                    args: vec![("name", "String")],
                    ret: "SourceNewFuncResult",
                    body: quote! {
                        let ds = (*self_).clone();
                        let entry_id = ds.next_entry_id.next();
                        let func_data = Gc::new(SourceFunctionData {
                            set: ds.clone(),
                            name: Some(name),
                            description: RefCell::new(None),
                            impl_expr: RefCell::new(None),
                            arg_type: RefCell::new(None),
                            ret_type: RefCell::new(None),
                            scope_entry_id: Cell::new(Some(
                                crate::langhelp::ScopeEntryId::Def(
                                    ds.import_id,
                                    ds.def_set_id,
                                    entry_id,
                                ),
                            )),
                            artifact_id: RefCell::new(None),
                            compiled_js: RefCell::new(None),
                            captured_externals: RefCell::new(Vec::new()),
                        });
                        ds.functions.borrow_mut().push(func_data.clone());
                        let func_ref = Rc::new(TypeSourceFunctionConst { data: func_data });
                        SourceNewFuncResult { func_ref }
                    },
                },
                DynWrapMethod {
                    name: "new_data",
                    args: vec![("name", "String")],
                    ret: "SourceNewDataResult",
                    body: quote! {
                        let ds = (*self_).clone();
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let data_field = format!("_{}", ds.next_field_id.next());
                        let dt_data = Gc::new(SourceDataTypeData {
                            set: ds.clone(),
                            name: Some(name),
                            description: RefCell::new(None),
                            underlying_type: crate::invariant::DeserialLazy::new(),
                            methods: RefCell::new(Vec::new()),
                            interface_impls: RefCell::new(Vec::new()),
                            identity,
                            data_field,
                            artifact_id: RefCell::new(None),
                        });
                        ds.data_types.borrow_mut().push(dt_data.clone());
                        let data_type_ref = Rc::new(SourceDataTypeRef { data: dt_data });
                        SourceNewDataResult { data_type_ref }
                    },
                },
                DynWrapMethod {
                    name: "build",
                    args: vec![],
                    ret: "Gc<DefinitionSet>",
                    body: quote! { (*self_).clone() },
                },
                DynWrapMethod {
                    name: "tagged_union",
                    args: vec![],
                    ret: "SourceTaggedUnionBuilder",
                    body: quote! {
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let data = Rc::new(SourceTaggedUnionData {
                            variants: RefCell::new(Vec::new()),
                            identity,
                            next_tag_id: crate::langhelp::Counter::default(),
                        });
                        SourceTaggedUnionBuilder { data }
                    },
                },
                DynWrapMethod {
                    name: "new_interface",
                    args: vec![],
                    ret: "SourceNewInterfaceResult",
                    body: quote! {
                        let ds = (*self_).clone();
                        let uc = scope.get_slot::<crate::eval::V8UniqueCounter>().unwrap();
                        let identity = Unique::Source(uc.compile_id.clone(), uc.counter.next());
                        let iface_data = Gc::new(SourceInterfaceData {
                            builtin_kind: None,
                            set: ds.clone(),
                            description: RefCell::new(None),
                            is_aspect: Cell::new(false),
                            methods: RefCell::new(Vec::new()),
                            identity,
                        });
                        ds.interfaces.borrow_mut().push(iface_data.clone());
                        let interface_ref = Rc::new(SourceInterfaceRef { data: iface_data });
                        SourceNewInterfaceResult { interface_ref }
                    },
                },
            ],
        },
    ]
}

/// Spec for `dynamic_wrap!(fn $name(ex, dynamic_scope, scope)(args) -> ret { body })`
/// — produces a scope-entry tuple `(name: &'static str, ScopeEntryId,
/// Rc<dyn Type>, v8::Local<v8::Value>)` consumed by `eval/mod.rs`'s
/// `builtin_scope_entries` setup loop. Each spec becomes a fn
/// `gen_fn_<name>(dynamic_scope) -> tuple`; callers replace the old
/// `dynamic_wrap!(fn …)` expression with `gen_fn_<name>(dynamic_scope)`.
struct DynWrapFnSpec {
    /// Function name — both the entry key and the generated fn name
    /// (`gen_fn_<name>`).
    name: &'static str,
    /// Explicit `ScopeEntryId::Builtin(id)` — usize, globally unique.
    id: usize,
    /// `(name, rust_type)` args (decoded from v8 via `DynamicWrap::from_v8`).
    args: Vec<(&'static str, &'static str)>,
    /// Rust return type. `"()"` for unit (also encodes "unit" — the macro
    /// returns `Tuple{base:None, elements: vec![]}` mortar arg-type for
    /// zero-arg fns, matching this).
    ret: &'static str,
    /// Body — `scope: &mut PinnedRef<HandleScope>` is in scope, plus
    /// each decoded arg name.
    body: TokenStream,
}

fn generate_dyn_wrap_fn(s: &DynWrapFnSpec) -> TokenStream {
    let fn_name = format_ident!("gen_fn_{}", s.name);
    let name_str = s.name;
    let id = s.id;
    let arg_mortar = arg_mortar_expr(&s.args);
    let arg_decls: Vec<TokenStream> = s
        .args
        .iter()
        .map(|(n, ty)| {
            let n = format_ident!("{}", n);
            let ty: TokenStream = ty.parse().unwrap();
            quote! {
                let #n: #ty = <#ty as crate::eval::dynamic_wrap::DynamicWrap>::from_v8(
                    scope, _jw_args.get(_jw_arg_i),
                );
                _jw_arg_i += 1;
            }
        })
        .collect();
    let ret_ty: TokenStream = s.ret.parse().unwrap();
    let body = &s.body;
    quote! {
        pub(crate) fn #fn_name<'s>(
            dynamic_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
        ) -> (
            &'static str,
            crate::langhelp::ScopeEntryId,
            std::rc::Rc<dyn crate::langhelp::Type>,
            v8::Local<'s, v8::Value>,
        ) {
            fn _jw_cb<'s>(
                scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
                _jw_args: v8::FunctionCallbackArguments<'s>,
                mut _jw_rv: v8::ReturnValue<'s>,
            ) {
                #[allow(unused_mut, unused_variables)]
                let mut _jw_arg_i = 0i32;
                #(#arg_decls)*
                let _jw_result: #ret_ty = { #body };
                _jw_rv.set(
                    <#ret_ty as crate::eval::dynamic_wrap::DynamicWrap>::into_v8(_jw_result, scope),
                );
            }
            (
                #name_str,
                crate::langhelp::ScopeEntryId::Builtin(#id),
                std::rc::Rc::new(
                    crate::eval::value::type_function::dynamic::TypeFunctionDynamic {
                        arg: #arg_mortar,
                        ret: <#ret_ty as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type(),
                    },
                ) as std::rc::Rc<dyn crate::langhelp::Type>,
                v8::Function::builder(_jw_cb).build(dynamic_scope).unwrap().cast::<v8::Value>(),
            )
        }
    }
}

/// Generates the registration loop for every `dyn_wrap_fns()` entry. The
/// produced `gen_register_builtin_fns` calls each `gen_fn_<name>` in turn,
/// hooks the result into the v8 global (when `register_v8` is true) and
/// the source scope, mirroring what the old hand-rolled loop in
/// `eval/mod.rs` did. Adding a `DynWrapFnSpec` automatically registers it
/// — no separate callsite to keep in sync.
fn generate_dyn_wrap_fn_registrar(fns: &[DynWrapFnSpec]) -> TokenStream {
    let call_exprs: Vec<TokenStream> = fns
        .iter()
        .map(|s| {
            let fn_name = format_ident!("gen_fn_{}", s.name);
            quote! { #fn_name(dynamic_scope) }
        })
        .collect();
    quote! {
        pub(crate) fn gen_register_builtin_fns<'s>(
            xx: &crate::langhelp::ExprContext,
            root_loc: crate::langhelp::QExprId,
            register_v8: bool,
            dynamic_scope: &mut v8::PinnedRef<'s, v8::HandleScope<'_>>,
            scope: &crate::langhelp::Scope,
            builtin_scope_entries: &mut Vec<(
                String,
                crate::langhelp::ScopeEntryId,
                std::rc::Rc<dyn crate::langhelp::Type>,
            )>,
        ) -> Result<(), Vec<crate::langhelp::ExprErr>> {
            for (key, dynamic_key, type_, dynamic_value) in [ #(#call_exprs),* ] {
                if register_v8 {
                    dynamic_scope
                        .get_current_context()
                        .global(dynamic_scope)
                        .set(
                            dynamic_scope,
                            v8::String::new(
                                dynamic_scope,
                                &crate::eval::scope_entry_id_dynamic_key(dynamic_key),
                            )
                                .unwrap()
                                .cast(),
                            dynamic_value,
                        );
                }
                let ib = crate::r#gen::aspect_initial_bind_get(xx, root_loc, type_.as_ref())
                    .into_res()?;
                builtin_scope_entries.push((key.to_string(), dynamic_key, type_));
                scope.values.borrow_mut().insert(
                    crate::langhelp::ConstKey::String(key.to_string()),
                    ib.initial_bind(dynamic_key),
                );
            }
            Ok(())
        }
    }
}

fn dyn_wrap_fns() -> Vec<DynWrapFnSpec> {
    vec![
        DynWrapFnSpec {
            name: "log",
            id: 5001,
            args: vec![("message", "String")],
            ret: "()",
            body: quote! {
                let timestamp_ms = std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .unwrap()
                    .as_millis() as u64;
                scope.get_slot::<V8LogStore>().unwrap().logs.borrow_mut().push(LogEntry {
                    timestamp_ms,
                    message,
                });
            },
        },
        DynWrapFnSpec {
            name: "define",
            id: 5002,
            args: vec![],
            ret: "Gc<DefinitionSet>",
            body: quote! {
                let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                let def_set_id = DefSetId(ex_slot.ex.ix.def_set_counter.next());
                let import_id = ex_slot.ex.ix.import_state;
                let source_id = ex_slot.ex.ix.source_id;
                let ds = Gc::new(DefinitionSet::new(import_id, def_set_id, source_id));
                ex_slot.ex.ix.definition_sets.borrow_mut().push(ds.clone());
                ds
            },
        },
        DynWrapFnSpec {
            name: "str_to_json",
            id: 5003,
            args: vec![("s", "String")],
            ret: "String",
            body: quote! { serde_json::to_string(&s).unwrap() },
        },
        DynWrapFnSpec {
            name: "constkey_to_str",
            id: 5004,
            args: vec![("key", "crate::langhelp::ConstKey")],
            ret: "String",
            body: quote! { key.to_str() },
        },
        DynWrapFnSpec {
            name: "new_code_value",
            id: 5005,
            args: vec![
                ("code", "crate::langhelp::Code"),
                ("value", "Rc<dyn crate::langhelp::Value>"),
            ],
            ret: "crate::eval::code_value_extern::CodeValueExtern",
            body: quote! {
                let cv = crate::langhelp::CodeValue { code, value };
                crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
            },
        },
        DynWrapFnSpec {
            name: "gen_binding_name",
            id: 5006,
            args: vec![("_xx", "crate::eval::code_value_extern::CodeValueExtern")],
            ret: "String",
            body: quote! {
                let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                let id = ex_slot.ex.next_local_id.next();
                format!("a{}", id)
            },
        },
        DynWrapFnSpec {
            name: "eval_const",
            id: 5007,
            args: vec![
                ("xx", "crate::eval::code_value_extern::ExprContextRef"),
                ("staged", "crate::eval::dynamic_wrap::StagedExpr"),
            ],
            ret: "crate::eval::code_value_extern::CodeValueExtern",
            body: quote! {
                let expr_value = staged.0;
                let expr_ast: Expr =
                    Rc::try_unwrap(crate::langhelp::downcast_value::<Expr>(expr_value))
                        .ok()
                        .expect("eval_const: staged Expr Rc not unique");
                let res = const_eval(&xx.0, &expr_ast);
                let cv = res.v.expect("eval_const: const_eval produced no value");
                crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
            },
        },
        DynWrapFnSpec {
            name: "const_eval",
            id: 5008,
            args: vec![
                ("xx", "crate::eval::code_value_extern::ExprContextRef"),
                ("staged", "crate::eval::dynamic_wrap::StagedExpr"),
            ],
            ret: "crate::eval::code_value_extern::CodeValueExtern",
            body: quote! {
                let expr_value = staged.0;
                let expr_ast: Expr =
                    Rc::try_unwrap(crate::langhelp::downcast_value::<Expr>(expr_value))
                        .ok()
                        .expect("const_eval: staged Expr Rc not unique");
                let res = const_eval(&xx.0, &expr_ast);
                let cv = res.v.unwrap_or_else(|| {
                    panic!("const_eval: const_eval produced no value; errors: {:?}", res.errors)
                });
                crate::eval::code_value_extern::CodeValueExtern(Rc::new(RefCell::new(Some(cv))))
            },
        },
        DynWrapFnSpec {
            name: "new_expr_context",
            id: 5009,
            args: vec![
                ("target_arg", "Rc<dyn crate::r#gen::Target>"),
                ("scope_arg", "std::collections::HashMap<ConstKey, Rc<dyn crate::r#gen::ScopeEntry>>"),
            ],
            ret: "crate::eval::code_value_extern::ExprContextRef",
            body: quote! {
                let ex_slot = scope.get_slot::<V8EvalContextSlot>().unwrap();
                let parent_ex = ex_slot.ex.clone();
                let mut scope_values: HashMap<ConstKey, Box<dyn crate::r#gen::ScopeEntry>> =
                    HashMap::new();
                for (k, v) in scope_arg {
                    scope_values.insert(k, Box::new(RcScopeEntryWrap(v)));
                }
                let new_scope = Rc::new(Scope {
                    values: RefCell::new(scope_values),
                    parent: None,
                });
                let xx = Rc::new(ExprContext {
                    vx: parent_ex.clone(),
                    import_state: Rc::new(Cell::new(parent_ex.ix.import_state)),
                    scope: new_scope,
                    target: target_arg,
                });
                crate::eval::code_value_extern::ExprContextRef(xx)
            },
        },
        DynWrapFnSpec {
            name: "list",
            id: 5010,
            args: vec![("element_type", "crate::eval::dynamic_wrap::AnyType")],
            ret: "crate::eval::value::type_list::dynamic::TypeListDynamic",
            body: quote! {
                crate::eval::value::type_list::dynamic::TypeListDynamic { element_type: element_type.0 }
            },
        },
        DynWrapFnSpec {
            name: "map",
            id: 5011,
            args: vec![
                ("key_type", "crate::eval::dynamic_wrap::AnyType"),
                ("value_type", "crate::eval::dynamic_wrap::AnyType"),
            ],
            ret: "crate::eval::value::type_map::dynamic::TypeMapDynamic",
            body: quote! {
                crate::eval::value::type_map::dynamic::TypeMapDynamic {
                    key_type: key_type.0,
                    value_type: value_type.0,
                }
            },
        },
        DynWrapFnSpec {
            name: "option",
            id: 5012,
            args: vec![("element_type", "crate::eval::dynamic_wrap::AnyType")],
            ret: "crate::eval::value::type_option::dynamic::TypeOptionDynamic",
            body: quote! {
                crate::eval::value::type_option::dynamic::TypeOptionDynamic { element_type: element_type.0 }
            },
        },
    ]
}

/// `dynamic_wrap!(@ arg_mortar_type)` macro arm: zero args → `TypeNullDynamic`,
/// one → that arg's mortar_type, multi → tuple of them. Mirrors what the
/// macro does for the function-style arg tuple.
fn arg_mortar_expr(args: &[(&'static str, &'static str)]) -> TokenStream {
    match args.len() {
        0 => quote! {
            std::rc::Rc::new(crate::eval::value::type_null::dynamic::TypeNullDynamic)
                as std::rc::Rc<dyn crate::langhelp::Type>
        },
        1 => {
            let ty: TokenStream = args[0].1.parse().unwrap();
            quote! { <#ty as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type() }
        },
        _ => {
            let elems: Vec<TokenStream> = args
                .iter()
                .map(|(_, ty)| {
                    let ty: TokenStream = ty.parse().unwrap();
                    quote! {
                        crate::eval::value::tuple::TupleElement::Mortar(
                            <#ty as crate::eval::dynamic_wrap::DynamicWrap>::mortar_type(),
                        )
                    }
                })
                .collect();
            quote! {
                std::rc::Rc::new(crate::eval::value::tuple::Tuple {
                    base: None,
                    elements: vec![ #(#elems),* ],
                }) as std::rc::Rc<dyn crate::langhelp::Type>
            }
        },
    }
}

fn dyn_wrap_structs() -> Vec<DynWrapStructSpec> {
    vec![
        DynWrapStructSpec {
            name: "OptionV8",
            id: 3001,
            data_fields: vec![("is_some_inner", "bool")],
            fields: vec![],
            methods: vec![DynWrapMethod {
                name: "is_some_",
                args: vec![],
                ret: "bool",
                body: quote! { self_.is_some_inner },
            }],
        },
        // `TypeRefV8` — the JS-side wrapper around an `Rc<dyn Type>`. Its
        // `aspect(unique)` builds a throwaway `ExprContext` and forwards to
        // the inner type's `aspect()`; the boolean result is wrapped as an
        // `OptionV8` (sufficient for the existing scaffold — full option
        // payload wiring is TODO at the JS layer).
        DynWrapStructSpec {
            name: "TypeRefV8",
            id: 3002,
            data_fields: vec![("type_ref", "Rc<dyn crate::langhelp::Type>")],
            fields: vec![],
            methods: vec![DynWrapMethod {
                name: "aspect",
                args: vec![("unique", "Unique")],
                ret: "OptionV8",
                body: quote! {
                    let ex_slot = scope
                        .get_slot::<crate::eval::V8EvalContextSlot>()
                        .expect("TypeRefV8.aspect: V8EvalContextSlot not set");
                    let import_state_id = ex_slot.ex.ix.import_state;
                    let xx = Rc::new(ExprContext {
                        vx: ex_slot.ex.clone(),
                        import_state: Rc::new(Cell::new(import_state_id)),
                        scope: Rc::new(Scope {
                            values: RefCell::new(HashMap::new()),
                            parent: None,
                        }),
                        target: Rc::new(crate::eval::TargetDynamic),
                    });
                    let is_some = self_.type_ref.aspect(&xx, &unique).is_some();
                    OptionV8 { is_some_inner: is_some }
                },
            }],
        },
        // SourceNewFuncResult — the v8-side builder returned by
        // `definition_set.new_func(name)`. Holds the in-progress
        // `TypeSourceFunctionConst` and exposes setters for description,
        // impl body, arg type (two flavors — raw type and iface ref), and
        // ret type. Each setter throws if called twice.
        DynWrapStructSpec {
            name: "SourceNewFuncResult",
            id: 3003,
            data_fields: vec![("func_ref", "Rc<TypeSourceFunctionConst>")],
            fields: vec![DynWrapField {
                name: "func",
                ret: "Rc<TypeSourceFunctionConst>",
                body: quote! { self_.func_ref.clone() },
            }],
            methods: vec![
                DynWrapMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote! {
                        *self_.func_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                DynWrapMethod {
                    name: "impl_",
                    args: vec![("staged", "StagedExpr")],
                    ret: "()",
                    body: quote! {
                        let mut impl_expr = self_.func_ref.data.impl_expr.borrow_mut();
                        if impl_expr.is_some() {
                            drop(impl_expr);
                            throw_error(scope, "Function already has an implementation");
                            return;
                        }
                        *impl_expr = Some(staged.0);
                    },
                },
                DynWrapMethod {
                    name: "arg_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote! {
                        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            throw_error(scope, "Function already has an arg type");
                            return;
                        }
                        *arg_type = Some(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "arg_type",
                    args: vec![("iface_ref", "Rc<SourceInterfaceRef>")],
                    ret: "()",
                    body: quote! {
                        let mut arg_type = self_.func_ref.data.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            throw_error(scope, "Function already has an arg type");
                            return;
                        }
                        *arg_type = Some(Rc::new(
                            TypeSourceInterfaceConst { data: iface_ref.data.clone() },
                        ) as Rc<dyn crate::langhelp::Type>);
                    },
                },
                DynWrapMethod {
                    name: "ret_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote! {
                        let mut ret_type = self_.func_ref.data.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            throw_error(scope, "Function already has a ret type");
                            return;
                        }
                        *ret_type = Some(type_ref.0);
                    },
                },
            ],
        },
        // SourceNewDataMethodResult — returned by `.new_func(name)` on a
        // SourceNewDataResult; lets the caller set the method's arg type,
        // ret type, and impl body.
        DynWrapStructSpec {
            name: "SourceNewDataMethodResult",
            id: 3004,
            data_fields: vec![
                ("data_type_ref", "Rc<SourceDataTypeRef>"),
                ("method_index", "usize"),
            ],
            fields: vec![],
            methods: vec![
                DynWrapMethod {
                    name: "impl_",
                    args: vec![("staged", "StagedExpr")],
                    ret: "()",
                    body: quote! {
                        let methods = self_.data_type_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut impl_expr = method.impl_expr.borrow_mut();
                        if impl_expr.is_some() {
                            drop(impl_expr);
                            drop(methods);
                            throw_error(scope, "Method already has an implementation");
                            return;
                        }
                        *impl_expr = Some(staged.0);
                    },
                },
                DynWrapMethod {
                    name: "ret_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote! {
                        let methods = self_.data_type_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut ret_type = method.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            drop(methods);
                            throw_error(scope, "Method already has a ret type");
                            return;
                        }
                        *ret_type = Some(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "arg_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote! {
                        let methods = self_.data_type_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut arg_type = method.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            drop(methods);
                            throw_error(scope, "Method already has an arg type");
                            return;
                        }
                        *arg_type = Some(type_ref.0);
                    },
                },
            ],
        },
        // SourceNewInterfaceResult — v8 builder returned by
        // `definition_set.new_interface()`. Exposes `.type` (the iface ref),
        // a description setter, `set_is_aspect`, and `.new_func(name)` to
        // declare a method.
        DynWrapStructSpec {
            name: "SourceNewInterfaceResult",
            id: 3005,
            data_fields: vec![("interface_ref", "Rc<SourceInterfaceRef>")],
            fields: vec![DynWrapField {
                name: "type_",
                ret: "Rc<SourceInterfaceRef>",
                body: quote! { self_.interface_ref.clone() },
            }],
            methods: vec![
                DynWrapMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote! {
                        *self_.interface_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                DynWrapMethod {
                    name: "new_func",
                    args: vec![("name", "String")],
                    ret: "SourceNewInterfaceMethodResult",
                    body: quote! {
                        let iface_data = &self_.interface_ref.data;
                        let field_name = format!("_im{}", crate::eval::definition_set::next_interface_method_id(&iface_data.set));
                        let method_index = {
                            let mut methods = iface_data.methods.borrow_mut();
                            let idx = methods.len();
                            methods.push(SourceInterfaceMethodData {
                                name,
                                arg_type: RefCell::new(None),
                                ret_type: RefCell::new(None),
                                field_name,
                            });
                            idx
                        };
                        SourceNewInterfaceMethodResult {
                            interface_ref: self_.interface_ref.clone(),
                            method_index,
                        }
                    },
                },
                DynWrapMethod {
                    name: "set_is_aspect",
                    args: vec![("is_aspect", "bool")],
                    ret: "()",
                    body: quote! {
                        self_.interface_ref.data.is_aspect.set(is_aspect);
                    },
                },
            ],
        },
        // SourceNewDataResult — v8 builder for new user-defined data
        // types. Exposes the data type ref, the constructor, plus setters
        // for description, underlying type, methods, and interface impls.
        DynWrapStructSpec {
            name: "SourceNewDataResult",
            id: 3006,
            data_fields: vec![("data_type_ref", "Rc<SourceDataTypeRef>")],
            fields: vec![
                DynWrapField {
                    name: "type_",
                    ret: "Rc<SourceDataTypeRef>",
                    body: quote! { self_.data_type_ref.clone() },
                },
                DynWrapField {
                    name: "new_",
                    ret: "Rc<TypeSourceDataTypeConstructorConst>",
                    body: quote! {
                        Rc::new(TypeSourceDataTypeConstructorConst {
                            data: self_.data_type_ref.data.clone(),
                        })
                    },
                },
            ],
            methods: vec![
                DynWrapMethod {
                    name: "set_description",
                    args: vec![("description", "String")],
                    ret: "()",
                    body: quote! {
                        *self_.data_type_ref.data.description.borrow_mut() = Some(description);
                    },
                },
                DynWrapMethod {
                    name: "set_underlying",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote! {
                        self_.data_type_ref.data.underlying_type.set(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "new_func",
                    args: vec![("name", "String")],
                    ret: "SourceNewDataMethodResult",
                    body: quote! {
                        let dt_data = &self_.data_type_ref.data;
                        let field_name = format!("_{}", dt_data.set.next_field_id.next());
                        let entry_id = dt_data.set.next_entry_id.next();
                        let method_index = {
                            let mut methods = dt_data.methods.borrow_mut();
                            let idx = methods.len();
                            methods.push(SourceDataMethodData {
                                name,
                                arg_type: RefCell::new(None),
                                impl_expr: RefCell::new(None),
                                ret_type: RefCell::new(None),
                                scope_entry_id: Cell::new(Some(
                                    crate::langhelp::ScopeEntryId::Def(
                                        dt_data.set.import_id,
                                        dt_data.set.def_set_id,
                                        entry_id,
                                    ),
                                )),
                                self_scope_entry_id: Cell::new(None),
                                field_name,
                                compiled_js: RefCell::new(None),
                                captured_externals: RefCell::new(Vec::new()),
                            });
                            idx
                        };
                        SourceNewDataMethodResult {
                            data_type_ref: self_.data_type_ref.clone(),
                            method_index,
                        }
                    },
                },
                DynWrapMethod {
                    name: "impl_interface",
                    args: vec![
                        ("iface_ref", "Rc<SourceInterfaceRef>"),
                        ("record", "std::collections::BTreeMap<crate::langhelp::ConstKey, crate::eval::expr::Expr>"),
                    ],
                    ret: "()",
                    body: quote! {
                        let record_entries = record;
                        let iface_data = &iface_ref.data;
                        let iface_method_names: Vec<(usize, String)> = {
                            let methods = iface_data.methods.borrow();
                            methods.iter().enumerate().map(|(i, m)| (i, m.name.clone())).collect()
                        };
                        let mut method_impls = Vec::new();
                        let dt_data = &self_.data_type_ref.data;
                        for (imethod_idx, imethod_name) in &iface_method_names {
                            let const_key = crate::langhelp::ConstKey::String(imethod_name.clone());
                            match record_entries.get(&const_key) {
                                Some(expr) => {
                                    let entry_id = dt_data.set.next_entry_id.next();
                                    method_impls.push(SourceDataTypeInterfaceMethodImplData {
                                        interface_method_index: *imethod_idx,
                                        impl_expr: RefCell::new(Some(Rc::new(expr.clone()))),
                                        impl_expr_artifact: RefCell::new(None),
                                        compiled_js: RefCell::new(None),
                                        captured_externals: RefCell::new(Vec::new()),
                                        scope_entry_id: Cell::new(Some(
                                            crate::langhelp::ScopeEntryId::Def(
                                                dt_data.set.import_id,
                                                dt_data.set.def_set_id,
                                                entry_id,
                                            ),
                                        )),
                                        self_scope_entry_id: Cell::new(None),
                                    });
                                },
                                None => {
                                    throw_error(
                                        scope,
                                        &format!("Missing implementation for interface method '{}'", imethod_name),
                                    );
                                    return;
                                },
                            }
                        }
                        dt_data.interface_impls.borrow_mut().push(SourceDataTypeInterfaceImplData {
                            interface: iface_data.clone(),
                            method_impls,
                        });
                    },
                },
            ],
        },
        // SourceTaggedUnionBuilder — v8 builder for tagged unions. Exposes
        // `.type` (the ref) and `.variant(tag, type)` to add variants.
        DynWrapStructSpec {
            name: "SourceTaggedUnionBuilder",
            id: 3007,
            data_fields: vec![("data", "Rc<SourceTaggedUnionData>")],
            fields: vec![DynWrapField {
                name: "type_",
                ret: "Rc<SourceTaggedUnionRef>",
                body: quote! {
                    Rc::new(SourceTaggedUnionRef { data: self_.data.clone() })
                },
            }],
            methods: vec![DynWrapMethod {
                name: "variant",
                args: vec![("tag", "String"), ("type_ref", "AnyType")],
                ret: "Rc<SourceTaggedUnionConstructorRef>",
                body: quote! {
                    let variant_index = {
                        let mut variants = self_.data.variants.borrow_mut();
                        let idx = variants.len();
                        let js_tag = format!(
                            "{}{}",
                            crate::code_dynamic::TAG_PREFIX,
                            self_.data.next_tag_id.next(),
                        );
                        variants.push(SourceTaggedUnionVariant {
                            js_tag,
                            tag: crate::langhelp::ConstKey::String(tag),
                            type_: type_ref.0,
                        });
                        idx
                    };
                    Rc::new(SourceTaggedUnionConstructorRef {
                        data: self_.data.clone(),
                        variant_index,
                    })
                },
            }],
        },
        // SourceNewInterfaceMethodResult — builder returned by
        // `iface.new_func(name)`. Lets the caller set arg/ret types.
        DynWrapStructSpec {
            name: "SourceNewInterfaceMethodResult",
            id: 3008,
            data_fields: vec![
                ("interface_ref", "Rc<SourceInterfaceRef>"),
                ("method_index", "usize"),
            ],
            fields: vec![],
            methods: vec![
                DynWrapMethod {
                    name: "arg_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote! {
                        let methods = self_.interface_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut arg_type = method.arg_type.borrow_mut();
                        if arg_type.is_some() {
                            drop(arg_type);
                            drop(methods);
                            throw_error(scope, "Interface method already has an arg type");
                            return;
                        }
                        *arg_type = Some(type_ref.0);
                    },
                },
                DynWrapMethod {
                    name: "ret_",
                    args: vec![("type_ref", "AnyType")],
                    ret: "()",
                    body: quote! {
                        let methods = self_.interface_ref.data.methods.borrow();
                        let method = &methods[self_.method_index];
                        let mut ret_type = method.ret_type.borrow_mut();
                        if ret_type.is_some() {
                            drop(ret_type);
                            drop(methods);
                            throw_error(scope, "Interface method already has a ret type");
                            return;
                        }
                        *ret_type = Some(type_ref.0);
                    },
                },
            ],
        },
    ]
}

fn main() {
    println!("cargo:rerun-if-changed=build.rs");
    let out_dir = PathBuf::from(env::var_os("OUT_DIR").unwrap());
    let ifaces = interfaces();
    let aspects = aspects();
    let dw_structs = dyn_wrap_structs();
    let dw_wraps = dyn_wrap_wraps();
    let dw_fns = dyn_wrap_fns();
    let locals = local_identities();
    let artifacts = artifact_ids();
    check_no_duplicates(&ifaces);
    check_no_duplicate_aspects(&aspects);
    check_no_duplicate_ids(&ifaces, &aspects, &dw_structs, &dw_wraps, &dw_fns, &locals, &artifacts);
    let iface_tokens = generate(&ifaces);
    let aspect_tokens = generate_aspects(&aspects);
    let local_tokens = generate_local_identities(&locals);
    let artifact_tokens = generate_artifact_ids(&artifacts);
    // Skip genemichaels formatting — on a ~270KB / ~6000-line tokenstream it
    // takes >10 min to format. rustc doesn't care about formatting, and the
    // user only reads gen.rs when debugging (where they can format on demand).
    let fmt = |toks: TokenStream| -> String { toks.to_string() };
    // Single-file output: every generated item collected into one
    // `$OUT_DIR/gen.rs`, included from `src/gen.rs` as the `crate::r#gen`
    // module. A narrow `use` prelude up front pulls in just the identifiers
    // the bodies reference (no globs, no re-exports), so each body works
    // without per-file scope tricks.
    let mut all = TokenStream::new();
    all.extend(gen_prelude());
    all.extend(local_tokens);
    all.extend(artifact_tokens);
    all.extend(aspect_tokens);
    all.extend(iface_tokens);
    for s in &dw_structs {
        all.extend(generate_dyn_wrap_struct(s));
    }
    for s in &dw_wraps {
        all.extend(generate_dyn_wrap_wrap(s));
    }
    for s in &dw_fns {
        all.extend(generate_dyn_wrap_fn(s));
    }
    all.extend(generate_dyn_wrap_fn_registrar(&dw_fns));
    fs::write(out_dir.join("gen.rs"), fmt(all)).unwrap();
}

/// Narrow `use` block at the top of the generated `gen.rs` — every
/// identifier any body references. No wildcards (so adding a new ident
/// requires extending this list), no `pub use` re-exports (callers reach
/// gen items via `crate::r#gen::<name>` directly).
fn gen_prelude() -> TokenStream {
    quote! {
        use std::rc::Rc;
        use std::cell::{Cell, RefCell};
        use std::collections::HashMap;
        use dumpster::unsync::Gc;
        use crate::langhelp::{
            Code, CodeValue, ConstKey, DefSetId, ExprContext, QExprId, Scope,
            ScopeEntryId, Unique, Value,
        };
        use crate::eval::TargetDynamic;
        use crate::eval::V8EvalContextSlot;
        use crate::eval::V8LogStore;
        use crate::eval::V8UniqueCounter;
        use crate::langhelp::LogEntry;
        use crate::eval::RcScopeEntryWrap;
        use crate::eval::const_eval;
        use crate::eval::definition_set::DefinitionSet;
        use crate::eval::expr::Expr;
        use crate::eval::value::type_source_function::{SourceFunctionData, TypeSourceFunctionConst};
        use crate::eval::value::type_source_tagged_union::{
            SourceTaggedUnionData, SourceTaggedUnionRef,
            SourceTaggedUnionVariant,
        };
        use crate::eval::value::type_source_tagged_union_constructor::SourceTaggedUnionConstructorRef;
        use crate::eval::value::type_source_data_type::{
            SourceDataMethodData, SourceDataTypeData, SourceDataTypeInterfaceImplData,
            SourceDataTypeInterfaceMethodImplData, SourceDataTypeRef,
        };
        use crate::eval::value::type_source_data_type_constructor::TypeSourceDataTypeConstructorConst;
        use crate::eval::value::type_source_interface::{
            SourceInterfaceData, SourceInterfaceMethodData, SourceInterfaceRef,
            TypeSourceInterfaceConst,
        };
        use crate::eval::dynamic_wrap::{AnyType, StagedExpr};
        use crate::eval::dynamic_util::throw_error;
    }
}
