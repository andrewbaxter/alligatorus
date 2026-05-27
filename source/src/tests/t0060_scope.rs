use crate::{
    compile,
    eval::setup_v8,
    langhelp::SourceId,
    new_gx,
    syntax::SerialModule,
    tests::helpers::ModuleBuilder,
};

#[tokio::test]
async fn test_0000_scope_ok() {
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
                        mb.bind("x", mb.str("zebra")),
                        mb.block(
                            vec![mb.bind("x", mb.str("apple")), mb.call(mb.scope_use("log"), mb.scope_use("x"),),]
                        ),
                        mb.call(mb.scope_use("log"), mb.scope_use("x"),),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 2);
    assert_eq!(logs[0].message, "apple");
    assert_eq!(logs[1].message, "zebra");
}

#[tokio::test]
async fn test_0010_scope_bad() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    compile(
        gx,
        SourceId(0),
        SerialModule::V1(
            mb.block(
                vec![mb.block(vec![mb.bind("x", mb.str("apple")),]), mb.call(mb.scope_use("log"), mb.scope_use("x"),),],
            ),
        ),
    )
        .await
        .err()
        .unwrap();
}
