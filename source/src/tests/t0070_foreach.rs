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
async fn test_0000_list() {
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
                            "my_list",
                            mb.call(
                                mb.field(mb.call(mb.scope_use("list"), mb.scope_use("number")), mb.str("new")),
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
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "c_d_a_b");
}
