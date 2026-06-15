//! Single-file home for every build-time generated item: aspect traits + helpers
//! (`AspectType`, `aspect_type_aspect_id`, `aspect_type_get`,
//! `aspect_type_get_maybe`, etc.), builtin source-language interface registration
//! (`Target` and `ScopeEntry` Rust traits + `BuiltinInterfaces`
//!
//! * `gen_build_builtin_interfaces`), `dynamic_wrap`-replacement struct/ wrap/fn
//!   definitions, the generated registration loop
//!   (`crate::r#gen::gen_register_builtin_fns`), and per-`Type` identity functions
//!   (`xyz_identity`). The spec lives in `build.rs`; every id is checked globally for
//!   uniqueness there.
include!(concat!(env!("OUT_DIR"), "/gen.rs"));
