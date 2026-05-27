use {
    super::helpers::ModuleBuilder,
    crate::{
        compile,
        new_gx,
        eval::setup_v8,
        langhelp::SourceId,
        syntax::SerialModule,
    },
};

/// This tests basic number operations (creation, binding, comparison,
/// modification, to_string). Note that how this test is set up, all numeric
/// operations can happen at half-eval time (e.g. "constant folding") so only the
/// result number is transferred to v8 before doing `to_string`.
#[tokio::test]
async fn test_0000_number_lit() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind("x", mb.num(4.)),
                        mb.if_(
                            vec![
                                (
                                    mb.operator_lt(mb.scope_use("x"), mb.num(5.)),
                                    mb.set(mb.scope_use("x"), mb.operator_add(mb.scope_use("x"), mb.num(7.))),
                                )
                            ],
                            None
                        ),
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.field(mb.scope_use("x"), mb.str("to_string")), mb.tuple(vec![]))
                        ),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "11");
}

/// This tests basic number operations (creation, binding, comparison,
/// modification, to_string) in v8. Because the function argument is a mortar
/// number, all operations need to happen in v8 instead of at evaluation time.
#[tokio::test]
async fn test_00100_number_mortar() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "maybe_add",
                            mb.lower(
                                mb.block(
                                    vec![
                                        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                        mb.bind(
                                            "builder",
                                            mb.call(
                                                mb.field(mb.scope_use("d"), mb.str("new_func")),
                                                mb.str("maybe add 7")
                                            )
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("builder"), mb.str("arg")),
                                            mb.tuple(vec![mb.scope_use("number")]),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("builder"), mb.str("ret")),
                                            mb.tuple(vec![mb.scope_use("number")]),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("builder"), mb.str("impl")),
                                            mb.tuple(
                                                vec![
                                                    mb.stage(
                                                        mb.block(
                                                            vec![
                                                                mb.if_(
                                                                    vec![
                                                                        (
                                                                            mb.operator_lt(
                                                                                mb.scope_use("arg"),
                                                                                mb.num(5.)
                                                                            ),
                                                                            mb.set(
                                                                                mb.scope_use("arg"),
                                                                                mb.operator_add(
                                                                                    mb.scope_use("arg"),
                                                                                    mb.num(7.)
                                                                                )
                                                                            ),
                                                                        )
                                                                    ],
                                                                    None
                                                                ),
                                                                mb.scope_use("arg"),
                                                            ]
                                                        )
                                                    )
                                                ]
                                            )
                                        ),
                                        mb.field(mb.scope_use("builder"), mb.str("func"))
                                    ]
                                )
                            )
                        ),
                        mb.call(
                            mb.scope_use("log",),
                            mb.call(
                                mb.field(
                                    mb.call(mb.scope_use("maybe_add"), mb.tuple(vec![mb.num(4.)])),
                                    mb.str("to_string")
                                ),
                                mb.tuple(vec![])
                            )
                        ),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "11");
}
