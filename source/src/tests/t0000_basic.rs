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
async fn test_00100_call_lit() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(mb.call(mb.scope_use("log"), mb.str("Hello world (mortar)"))),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
}

#[tokio::test]
async fn test_00101_type_error() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    compile(gx, SourceId(0), SerialModule::V1(mb.call(mb.scope_use("log"), mb.null())))
        .await
        .unwrap_err();
}

#[tokio::test]
async fn test_00200_bind_use() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.seq(
                    vec![
                        mb.bind("hello", mb.str("Hello world (mortar)")),
                        mb.call(mb.scope_use("log"), mb.scope_use("hello"))
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
}

#[tokio::test]
async fn test_00300_stage_lower() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.call(mb.scope_use("log"), mb.lower(mb.stage(mb.str("Hello world (mortar)")))),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
}

#[tokio::test]
async fn test_00400_lower_nostage() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(mb.call(mb.scope_use("log"), mb.lower(mb.str("Hello world (mortar)")))),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
}

#[tokio::test]
async fn test_00500_method_call() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    compile(
        gx,
        SourceId(0),
        SerialModule::V1(
            mb.call(
                mb.scope_use("log"),
                mb.call(mb.field(mb.str("HELLO WORLD"), mb.str("toLowerCase")), mb.null()),
            ),
        ),
    )
        .await
        .unwrap();
}
