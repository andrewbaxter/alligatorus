use {
    crate::{
        compile,
        eval::setup_v8,
        syntax::{
            SourceExpr,
            SourceModule,
            SourceSpecAbsolute,
        },
    },
    super::helpers::{
        ModuleBuilder,
        new_gx,
    },
};

fn panicky_fn(mb: &ModuleBuilder, message: &str) -> SourceExpr {
    return mb.lower(
        mb.seq(
            vec![
                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("f"))),
                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                    mb.tuple(vec![mb.types_null(), mb.types_null(), mb.stage(mb.panic(mb.str(message)))]),
                ),
                mb.scope_use("f"),
            ],
        ),
    );
}

fn panicky_fn_with_exception_type(mb: &ModuleBuilder, message: &str) -> SourceExpr {
    return mb.lower(
        mb.seq(
            vec![
                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("f"))),
                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                mb.call(mb.field(mb.scope_use("nfr"), mb.str("set_exception_type")), mb.types_string()),
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                    mb.tuple(vec![mb.types_null(), mb.types_null(), mb.stage(mb.panic(mb.str(message)))]),
                ),
                mb.scope_use("f"),
            ],
        ),
    );
}

#[test]
fn test_01500_panic_terminates_call() {
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
                                mb.bind("f", panicky_fn(&mb, "boom")),
                                mb.call(mb.scope_use("f"), mb.tuple(vec![])),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
    let err = res.expect_err("panic from called function should propagate as uncaught runtime exception");
    let combined = format!("{:?}", err);
    assert!(combined.contains("boom"), "expected panic message to surface; got: {}", combined);
}

#[test]
fn test_01510_user_try_catch_does_not_catch_panic() {
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
                                mb.bind("f", panicky_fn(&mb, "uncatchable")),
                                mb.try_(
                                    mb.call(mb.scope_use("f"), mb.tuple(vec![])),
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.str("caught")),
                                        ),
                                    ),
                                    None,
                                ),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
    let err = res.expect_err("panic should propagate through user try/catch");
    let combined = format!("{:?}", err);
    assert!(combined.contains("uncatchable"), "expected panic message to surface; got: {}", combined);
    let logs_pattern = format!("{:?}", err);
    assert!(!logs_pattern.contains("\"caught\""), "catch body must not have run");
}

#[test]
fn test_01520_exception_cast_does_not_catch_panic() {
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
                                mb.bind("f", panicky_fn_with_exception_type(&mb, "inner panic")),
                                mb.try_(
                                    mb.call(mb.scope_use("f"), mb.tuple(vec![])),
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.str("caught")),
                                        ),
                                    ),
                                    None,
                                ),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
    let err =
        res.expect_err(
            "panic inside a function declaring its own exception type should still propagate through the auto-injected cast_exception try/catch",
        );
    let combined = format!("{:?}", err);
    assert!(combined.contains("inner panic"), "expected panic message to surface; got: {}", combined);
}
