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
        sanitize_builtins,
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
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["boom", "after"]);
}

#[test]
fn test_01310_try_finally_runs_finally() {
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
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["body", "finally"]);
}

#[test]
fn test_01320_try_catch_handles_throw() {
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
                                mb.call(mb.log(), mb.str("done")),
                            ],
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["boom", "done"]);
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

#[test]
fn test_01340_throw_caught_logs_message() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.try_(
                            mb.throw(mb.str("boom")),
                            Some(("e", mb.builtin(&["types", "string"]), mb.call(mb.log(), mb.scope_use("e")))),
                            None,
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["boom"]);
    let scripts = sanitize_builtins(&res.generated_dynamic_scripts);
    let combined = scripts.join("\n");
    assert!(combined.contains("try"), "generated dynamic must contain try");
    assert!(combined.contains("catch"), "generated dynamic must contain catch");
    assert!(combined.contains("throw"), "generated dynamic must contain throw");
}

#[test]
fn test_01350_throw_with_no_catch_in_function_no_exception_type_rejects() {
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
                                mb.bind(
                                    "my_func",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "nfr",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("func")),
                                                        mb.str("f"),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "f",
                                                    mb.field(mb.scope_use("nfr"), mb.str("func")),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_null(),
                                                            mb.types_null(),
                                                            mb.stage(mb.throw(mb.str("x"))),
                                                        ],
                                                    ),
                                                ),
                                                mb.scope_use("f"),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.call(mb.scope_use("my_func"), mb.tuple(vec![])),
                            ],
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
    assert!(
        res.is_err(),
        "throw in function with no declared exception type should be rejected: {:?}",
        res.map(|_| ())
    );
}

#[test]
fn test_01360_set_exception_type_allows_throw() {
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
                                mb.bind(
                                    "my_func",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "nfr",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("func")),
                                                        mb.str("f"),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "f",
                                                    mb.field(mb.scope_use("nfr"), mb.str("func")),
                                                ),
                                                mb.call(
                                                    mb.field(
                                                        mb.scope_use("nfr"),
                                                        mb.str("set_exception_type"),
                                                    ),
                                                    mb.types_string(),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_null(),
                                                            mb.types_null(),
                                                            mb.stage(mb.throw(mb.str("x"))),
                                                        ],
                                                    ),
                                                ),
                                                mb.scope_use("f"),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.try_(
                                    mb.call(mb.scope_use("my_func"), mb.tuple(vec![])),
                                    Some(("e", mb.builtin(&["types", "string"]), mb.call(mb.log(), mb.scope_use("e")))),
                                    None,
                                ),
                            ],
                        ),
                    ),
                ),
            ].into_iter().collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap_or_else(|e| {
        panic!("expected compile to succeed: {:?}", e)
    });
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["x"]);
}
