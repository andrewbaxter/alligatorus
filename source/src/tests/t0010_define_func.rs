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

#[tokio::test]
async fn test_00600define_func() {
    //! Define a function that prints "hello", then call it.
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
                            "hello",
                            mb.lower(
                                mb.block(
                                    vec![
                                        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                        mb.bind(
                                            "nfr",
                                            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("f"))
                                        ),
                                        mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                        mb.call(
                                            mb.field(mb.scope_use("nfr"), mb.str("arg")),
                                            mb.tuple(vec![mb.scope_use("null")]),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("nfr"), mb.str("ret")),
                                            mb.tuple(vec![mb.scope_use("null")]),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                            mb.tuple(vec![mb.stage(mb.call(mb.scope_use("log"), mb.str("hello")))])
                                        ),
                                        mb.scope_use("f")
                                    ]
                                )
                            )
                        ),
                        mb.call(mb.scope_use("hello"), mb.tuple(vec![])),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "hello");
}

#[tokio::test]
async fn test_00700define_func_arg_ret() {
    //! Define a function with string arg and string ret that uppercases the arg, call
    //! it with a string, and log the result.
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
                        mb.bind("my_func", mb.lower(mb.block(vec![
                            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                            mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("f"))),
                            mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                            // Set arg type to string
                            mb.call(mb.field(mb.scope_use("nfr"), mb.str("arg")), mb.tuple(vec![mb.scope_use("string")]),),
                            // Set ret type to string
                            mb.call(mb.field(mb.scope_use("nfr"), mb.str("ret")), mb.tuple(vec![mb.scope_use("string")]),),
                            // Impl: call toUpperCase on arg and return
                            mb.call(
                                mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                mb.tuple(
                                    vec![
                                        mb.stage(
                                            mb.call(mb.field(mb.scope_use("arg"), mb.str("toUpperCase")), mb.null(),),
                                        )
                                    ]
                                ),
                            ),
                            mb.scope_use("f"),
                        ]))),
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.scope_use("my_func"), mb.tuple(vec![mb.str("hello world")])),
                        ),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "HELLO WORLD");
}
