use {
    crate::{
        compile,
        eval::setup_v8,
        langhelp::SourceId,
        new_gx,
        syntax::SerialModule,
    },
    super::helpers::ModuleBuilder,
};

#[tokio::test]
async fn test_0000_foreach_list() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx.clone(),
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "my_list",
                            mb.call(
                                mb.field(mb.call(mb.scope_use("list"), mb.scope_use("number")), mb.str("new"),),
                                mb.tuple(vec![mb.num(1.), mb.num(2.), mb.num(7.)])
                            )
                        ),
                        mb.foreach(
                            mb.scope_use("my_list"),
                            mb.str("e"),
                            mb.block(
                                vec![
                                    mb.call(
                                        mb.scope_use("log"),
                                        mb.call(mb.field(mb.scope_use("e"), mb.str("to_string")), mb.tuple(vec![]))
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
    assert_eq!(logs.len(), 3);
    assert_eq!(logs[0].message, "1");
    assert_eq!(logs[1].message, "2");
    assert_eq!(logs[2].message, "7");
}
