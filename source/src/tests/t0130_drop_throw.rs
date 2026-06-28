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

fn iface_methods(mb: &ModuleBuilder, methods: Vec<(&str, SourceExpr)>) -> SourceExpr {
    let entries: Vec<(&str, SourceExpr)> = methods.into_iter().map(|(k, v)| (k, mb.stage(v))).collect();
    return mb.call(
        mb.field(mb.call(mb.types_map(), mb.tuple(vec![mb.types_constkey(), mb.types_expr()])), mb.str("new")),
        mb.record(entries),
    );
}

fn make_canary_instance(mb: &ModuleBuilder, name: &str, drop_message: &str) -> SourceExpr {
    return mb.call(
        mb.lower(
            mb.seq(
                vec![
                    mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                    mb.bind("u", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str(name))),
                    mb.call(mb.field(mb.scope_use("u"), mb.str("set_inner")), mb.types_null()),
                    mb.call(
                        mb.field(mb.scope_use("u"), mb.str("value")),
                        iface_methods(mb, vec![("type", mb.scope_use("inner"))]),
                    ),
                    mb.call(
                        mb.field(mb.scope_use("u"), mb.str("type")),
                        iface_methods(
                            mb,
                            vec![
                                (
                                    "can_cast_from",
                                    mb.seq(
                                        vec![
                                            mb.call(
                                                mb.field(
                                                    mb.call(
                                                        mb.field(
                                                            mb.field(mb.scope_use("arg"), mb.str("source_type")),
                                                            mb.str("aspect"),
                                                        ),
                                                        mb.eval_aspect_string_dynamic(),
                                                    ),
                                                    mb.str("is_some"),
                                                ),
                                                mb.tuple(vec![]),
                                            ),
                                        ],
                                    ),
                                ),
                                (
                                    "cast_from",
                                    mb.call(
                                        mb.eval_const_eval(),
                                        mb.tuple(
                                            vec![
                                                mb.field(mb.scope_use("arg"), mb.str("xx")),
                                                mb.stage(mb.graft(mb.field(mb.scope_use("arg"), mb.str("cv")))),
                                            ],
                                        ),
                                    ),
                                ),
                                ("desc", mb.seq(vec![mb.str(name)])),
                                (
                                    "aspect",
                                    mb.call(
                                        mb.field(mb.call(mb.types_option(), mb.types_aspect()), mb.str("none")),
                                        mb.null(),
                                    ),
                                ),
                            ],
                        ),
                    ),
                    mb.call(
                        mb.field(mb.scope_use("u"), mb.str("impl_interface")),
                        mb.tuple(
                            vec![
                                mb.builtin(&["eval", "aspect_drop_dynamic"]),
                                iface_methods(
                                    mb,
                                    vec![
                                        (
                                            "drop",
                                            mb.call(
                                                mb.eval_const_eval(),
                                                mb.tuple(
                                                    vec![
                                                        mb.field(mb.scope_use("arg"), mb.str("xx")),
                                                        mb.stage(mb.call(mb.log(), mb.str(drop_message))),
                                                    ],
                                                ),
                                            ),
                                        ),
                                    ],
                                ),
                            ],
                        ),
                    ),
                    mb.field(mb.scope_use("u"), mb.str("new")),
                ],
            ),
        ),
        mb.null(),
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
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.scope_use("e")),
                                        ),
                                    ),
                                    None,
                                ),
                                mb.call(mb.log(), mb.str("after")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
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
            ]
                .into_iter()
                .collect(),
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
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.scope_use("e")),
                                        ),
                                    ),
                                    None,
                                ),
                                mb.call(mb.log(), mb.str("done")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
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
            [("main", SourceModule::V1(mb.try_(mb.call(mb.log(), mb.str("body")), None, None)))]
                .into_iter()
                .collect(),
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
            ]
                .into_iter()
                .collect(),
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
                                                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
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
            ]
                .into_iter()
                .collect(),
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
                                                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("set_exception_type")),
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
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.scope_use("e")),
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
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap_or_else(|e| {
        panic!("expected compile to succeed: {:?}", e)
    });
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["x"]);
}

#[test]
fn test_01370_try_catch_finally_combined() {
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
                            Some(("e", mb.builtin(&["types", "string"]), mb.call(mb.log(), mb.str("caught")))),
                            Some(mb.call(mb.log(), mb.str("always"))),
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["caught", "always"]);
    let scripts = sanitize_builtins(&res.generated_dynamic_scripts);
    let combined = scripts.join("\n");
    assert!(combined.contains("try"), "generated dynamic must contain try");
    assert!(combined.contains("catch"), "generated dynamic must contain catch");
    assert!(combined.contains("finally"), "generated dynamic must contain finally");
}

#[test]
fn test_01400_end_of_scope_drop_runtime() {
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
                                mb.bind("canary", mb.lower(make_canary_instance(&mb, "Canary", "dropped"))),
                                mb.bind(
                                    "cnry",
                                    mb.as_(mb.as_(mb.str("X"), mb.types_string()), mb.scope_use("canary")),
                                ),
                                mb.call(mb.log(), mb.str("after")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
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
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.seq(
                            vec![
                                mb.bind("canary", mb.lower(make_canary_instance(&mb, "Canary", "dropped"))),
                                mb.as_(mb.as_(mb.str("Y"), mb.types_string()), mb.scope_use("canary")),
                                mb.call(mb.log(), mb.str("after")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
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
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.seq(
                            vec![
                                mb.bind("canary_a", mb.lower(make_canary_instance(&mb, "CanaryA", "dropped_a"))),
                                mb.bind("canary_b", mb.lower(make_canary_instance(&mb, "CanaryB", "dropped_b"))),
                                mb.bind(
                                    "rec",
                                    mb.record(
                                        vec![
                                            (
                                                "a",
                                                mb.as_(
                                                    mb.as_(mb.str("A"), mb.types_string()),
                                                    mb.scope_use("canary_a"),
                                                ),
                                            ),
                                            (
                                                "b",
                                                mb.as_(
                                                    mb.as_(mb.str("B"), mb.types_string()),
                                                    mb.scope_use("canary_b"),
                                                ),
                                            ),
                                        ],
                                    ),
                                ),
                                mb.field(mb.scope_use("rec"), mb.str("a")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
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
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.try_(
                            mb.seq(
                                vec![
                                    mb.bind("canary", mb.lower(make_canary_instance(&mb, "Canary", "dropped"))),
                                    mb.bind(
                                        "cnry",
                                        mb.as_(mb.as_(mb.str("Z"), mb.types_string()), mb.scope_use("canary")),
                                    ),
                                    mb.throw(mb.str("boom")),
                                ],
                            ),
                            Some(("e", mb.builtin(&["types", "string"]), mb.call(mb.log(), mb.str("caught")))),
                            None,
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    let drop_idx =
        messages
            .iter()
            .position(|m| *m == "dropped")
            .expect(&format!("expected 'dropped' log, got {:?}", messages));
    let caught_idx =
        messages.iter().position(|m| *m == "caught").expect(&format!("expected 'caught' log, got {:?}", messages));
    assert!(drop_idx < caught_idx, "drop should run before catch handler; got logs={:?}", messages,);
}

#[test]
fn test_01440_try_catch_unifies_branch_types() {
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
                                    mb.call(mb.log(), mb.str("body")),
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.str("caught")),
                                        ),
                                    ),
                                    None,
                                ),
                                mb.call(mb.log(), mb.str("after")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap_or_else(|e| {
        panic!("expected compile to succeed: {:?}", e)
    });
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    assert_eq!(messages, vec!["body", "after"]);
}

#[test]
fn test_01450_try_catch_branch_type_mismatch_errors() {
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
                            Some(("e", mb.builtin(&["types", "string"]), mb.scope_use("e"))),
                            None,
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string()));
    assert!(res.is_err(), "Try with incompatible body/catch result types should fail compile: {:?}", res.map(|_| ()));
}

#[test]
fn test_01460_finally_drops_result() {
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
                                mb.bind("canary", mb.lower(make_canary_instance(&mb, "Canary", "dropped"))),
                                mb.try_(
                                    mb.call(mb.log(), mb.str("body")),
                                    None,
                                    Some(mb.as_(mb.as_(mb.str("V"), mb.types_string()), mb.scope_use("canary"))),
                                ),
                                mb.call(mb.log(), mb.str("after")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    let messages: Vec<&str> = res.logs.iter().map(|l| l.message.as_str()).collect();
    let drop_idx =
        messages
            .iter()
            .position(|m| *m == "dropped")
            .expect(&format!("expected 'dropped' log, got {:?}", messages));
    let after_idx =
        messages.iter().position(|m| *m == "after").expect(&format!("expected 'after' log, got {:?}", messages));
    assert!(drop_idx < after_idx, "finally result drop should run before 'after'; got logs={:?}", messages,);
}

#[test]
fn test_01470_bind_of_throw_errors() {
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
                                                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("set_exception_type")),
                                                    mb.types_string(),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_null(),
                                                            mb.types_null(),
                                                            mb.stage(
                                                                mb.seq(
                                                                    vec![
                                                                        mb.bind("x", mb.throw(mb.str("boom"))),
                                                                        mb.null(),
                                                                    ],
                                                                ),
                                                            ),
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
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.scope_use("e")),
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
    let err = match res {
        Err(e) => e,
        Ok(_) => panic!("expected compile error binding result of throw"),
    };
    let msg = format!("{:?}", err);
    assert!(
        msg.contains("Cannot bind result of expression that ends control flow"),
        "expected bind-of-throw error message, got: {}",
        msg,
    );
}

#[test]
fn test_01480_log_after_throw_in_seq_errors() {
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
                                                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("set_exception_type")),
                                                    mb.types_string(),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_null(),
                                                            mb.types_null(),
                                                            mb.stage(
                                                                mb.seq(
                                                                    vec![
                                                                        mb.throw(mb.str("boom")),
                                                                        mb.call(mb.log(), mb.str("unreachable")),
                                                                    ],
                                                                ),
                                                            ),
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
                                    Some(
                                        (
                                            "e",
                                            mb.builtin(&["types", "string"]),
                                            mb.call(mb.log(), mb.scope_use("e")),
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
    let err = match res {
        Err(e) => e,
        Ok(_) => panic!("expected compile error for code after throw"),
    };
    let msg = format!("{:?}", err);
    assert!(msg.contains("Unreachable"), "expected Unreachable error message, got: {}", msg,);
}
