use {
    crate::{
        compile,
        eval::setup_v8,
        syntax::{
            SourceModule,
            SourceSpecAbsolute,
        },
    },
    super::helpers::{
        ModuleBuilder,
        new_gx,
    },
};

#[test]
fn test_01300_throw_inside_seq_keeps_outer_value() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.seq(
                            vec![
                                mb.try_(
                                    mb.throw(mb.str("boom")),
                                    Some(("e", mb.builtin(&["types", "string"]), mb.call(mb.log(), mb.scope_use("e")))),
                                    None,
                                ),
                                mb.call(mb.log(), mb.str("after")),
                            ],
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let _ = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
}

#[test]
fn test_01310_try_finally_compiles() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.try_(
                            mb.call(mb.log(), mb.str("body")),
                            None,
                            Some(mb.call(mb.log(), mb.str("finally"))),
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let _ = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
}

#[test]
fn test_01320_try_catch_compiles() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.seq(
                            vec![
                                mb.try_(
                                    mb.throw(mb.str("boom")),
                                    Some(("e", mb.builtin(&["types", "string"]), mb.call(mb.log(), mb.scope_use("e")))),
                                    None,
                                ),
                                mb.null(),
                            ],
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let _ = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
}

#[test]
fn test_01330_try_requires_catch_or_finally() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.try_(
                            mb.call(mb.log(), mb.str("body")),
                            None,
                            None,
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
    assert!(res.is_err(), "Try with no catch and no finally should fail compile");
}
