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
        sanitize_builtins,
    },
};

/// Build a source-level definition of an object type with a drop implementation.
/// The drop impl body is the source expression `call(log, str(drop_message))` —
/// at drop dispatch the glue const-evals this body with `inner` in scope, getting
/// the log call code with null result type. Returns the SourceExpr that, when
/// evaluated in a lower context, produces the constructor `(underlying) -> Counter`.
fn make_counter_ctor(mb: &ModuleBuilder, name: &str, drop_message: &str) -> SourceExpr {
    return mb.lower(
        mb.seq(
            vec![
                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str(name))),
                mb.call(mb.field(mb.scope_use("ndr"), mb.str("set_inner")), mb.types_string()),
                mb.call(
                    mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                    mb.tuple(
                        vec![
                            mb.builtin(&["eval", "aspect_drop_dynamic"]),
                            mb.call(
                                mb.field(
                                    mb.call(mb.types_map(), mb.tuple(vec![mb.types_constkey(), mb.types_expr()])),
                                    mb.str("new"),
                                ),
                                mb.record(
                                    vec![("drop", mb.stage(mb.call(mb.log(), mb.str(drop_message))))],
                                ),
                            ),
                        ],
                    ),
                ),
                mb.field(mb.scope_use("ndr"), mb.str("new")),
            ],
        ),
    );
}

#[test]
fn test_01400_end_of_scope_drop_runtime() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([(
        "main",
        SourceModule::V1(
            mb.seq(
                vec![
                    mb.bind("make_counter", make_counter_ctor(&mb, "Counter", "dropped")),
                    mb.bind("ctr", mb.call(mb.scope_use("make_counter"), mb.str("X"))),
                    mb.call(mb.log(), mb.str("after")),
                ],
            ),
        ),
    )].into_iter().collect());
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(
        messages,
        vec!["after", "dropped"],
        "end-of-scope drop should run after body; got logs={:?}",
        messages,
    );
    let scripts = sanitize_builtins(&res.generated_dynamic_scripts);
    let combined = scripts.join("\n");
    assert!(combined.contains("try"), "generated dynamic must contain try for drop wrap");
    assert!(combined.contains("finally"), "generated dynamic must contain finally for drop wrap");
}

#[test]
fn test_01410_discarded_seq_entry_runtime() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([(
        "main",
        SourceModule::V1(
            mb.seq(
                vec![
                    mb.bind("make_counter", make_counter_ctor(&mb, "Counter", "dropped")),
                    mb.call(mb.scope_use("make_counter"), mb.str("Y")),
                    mb.call(mb.log(), mb.str("after")),
                ],
            ),
        ),
    )].into_iter().collect());
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(
        messages,
        vec!["dropped", "after"],
        "discarded seq entry should be dropped before next entry; got logs={:?}",
        messages,
    );
}

#[test]
fn test_01420_field_sibling_drop_runtime() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([(
        "main",
        SourceModule::V1(
            mb.seq(
                vec![
                    mb.bind("make_a", make_counter_ctor(&mb, "CounterA", "dropped_a")),
                    mb.bind("make_b", make_counter_ctor(&mb, "CounterB", "dropped_b")),
                    mb.bind(
                        "rec",
                        mb.record(
                            vec![
                                ("a", mb.call(mb.scope_use("make_a"), mb.str("A"))),
                                ("b", mb.call(mb.scope_use("make_b"), mb.str("B"))),
                            ],
                        ),
                    ),
                    mb.field(mb.scope_use("rec"), mb.str("a")),
                ],
            ),
        ),
    )].into_iter().collect());
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert!(
        messages.contains(&"dropped_b"),
        "sibling 'b' should be dropped when field 'a' is accessed; got logs={:?}",
        messages,
    );
    assert!(
        !messages.iter().any(|m| *m == "dropped_a"),
        "extracted field 'a' should not be dropped at the field-access site; got logs={:?}",
        messages,
    );
}

#[test]
fn test_01430_drop_on_throw_runtime() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([(
        "main",
        SourceModule::V1(
            mb.try_(
                mb.seq(
                    vec![
                        mb.bind("make_counter", make_counter_ctor(&mb, "Counter", "dropped")),
                        mb.bind("ctr", mb.call(mb.scope_use("make_counter"), mb.str("Z"))),
                        mb.throw(mb.str("boom")),
                    ],
                ),
                Some(("e", mb.builtin(&["types", "string"]), mb.call(mb.log(), mb.str("caught")))),
                None,
            ),
        ),
    )].into_iter().collect());
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    let drop_idx = messages.iter().position(|m| *m == "dropped").expect(
        &format!("expected 'dropped' log, got {:?}", messages),
    );
    let caught_idx = messages.iter().position(|m| *m == "caught").expect(
        &format!("expected 'caught' log, got {:?}", messages),
    );
    assert!(
        drop_idx < caught_idx,
        "drop should run before catch handler; got logs={:?}",
        messages,
    );
}

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
