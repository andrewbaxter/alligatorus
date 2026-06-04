use {
    crate::{
        compile,
        eval::setup_v8,
        langhelp::SourceId,
        new_gx,
        syntax::SourceModule,
    },
    super::helpers::ModuleBuilder,
};

#[tokio::test]
async fn test_0000_map_string_keys() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx.clone(),
            SourceId(0),
            SourceModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "my_map",
                            mb.call(
                                mb.field(
                                    mb.call(
                                        mb.scope_use("map"),
                                        mb.tuple(vec![mb.scope_use("string"), mb.scope_use("number")])
                                    ),
                                    mb.str("new"),
                                ),
                                mb.tuple(vec![])
                            )
                        ),
                        mb.set(mb.field(mb.scope_use("my_map"), mb.str("a")), mb.num(10.)),
                        mb.set(mb.field(mb.scope_use("my_map"), mb.str("b")), mb.num(20.)),
                        mb.foreach(
                            mb.scope_use("my_map"),
                            mb.str("entry"),
                            mb.block(
                                vec![
                                    mb.call(mb.scope_use("log"), mb.field(mb.scope_use("entry"), mb.str("key"))),
                                    mb.call(
                                        mb.scope_use("log"),
                                        mb.call(
                                            mb.field(
                                                mb.field(mb.scope_use("entry"), mb.str("value")),
                                                mb.str("to_string")
                                            ),
                                            mb.tuple(vec![])
                                        )
                                    ),
                                ]
                            )
                        ),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 4);
    assert_eq!(logs[0].message, "a");
    assert_eq!(logs[1].message, "10");
    assert_eq!(logs[2].message, "b");
    assert_eq!(logs[3].message, "20");
}

#[tokio::test]
async fn test_0010_map_string_string() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx.clone(),
            SourceId(0),
            SourceModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "my_map",
                            mb.call(
                                mb.field(
                                    mb.call(
                                        mb.scope_use("map"),
                                        mb.tuple(vec![mb.scope_use("string"), mb.scope_use("string")])
                                    ),
                                    mb.str("new"),
                                ),
                                mb.tuple(vec![])
                            )
                        ),
                        mb.set(mb.field(mb.scope_use("my_map"), mb.str("x")), mb.str("hello")),
                        mb.set(mb.field(mb.scope_use("my_map"), mb.str("y")), mb.str("world")),
                        mb.foreach(
                            mb.scope_use("my_map"),
                            mb.str("entry"),
                            mb.block(
                                vec![
                                    mb.call(mb.scope_use("log"), mb.field(mb.scope_use("entry"), mb.str("key"))),
                                    mb.call(mb.scope_use("log"), mb.field(mb.scope_use("entry"), mb.str("value"))),
                                ]
                            )
                        ),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 4);
    assert_eq!(logs[0].message, "x");
    assert_eq!(logs[1].message, "hello");
    assert_eq!(logs[2].message, "y");
    assert_eq!(logs[3].message, "world");
}
