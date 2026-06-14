//! All build-time generated items. Includes per-Type identity functions
//! (one `pub(crate) fn xyz_identity() -> Unique` per entry in
//! `build.rs::local_identities()`). Other generated items still live in
//! their own gen files (`gen_aspects.rs`, `gen_builtin_interfaces.rs`,
//! `gen_dw_*.rs`, `gen_dyn_wrap_fns.rs`) included from their original
//! Rust modules. Consolidating everything here is on the roadmap.
include!(concat!(env!("OUT_DIR"), "/gen_local_identities.rs"));
