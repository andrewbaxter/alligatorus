use crate::{
    compile,
    eval::setup_v8,
    langhelp::SourceId,
    new_gx,
    syntax::{
        SourceModule,
        SourceSpecAbsolute,
    },
    tests::helpers::{
        ModuleBuilder,
        redact_builtins,
    },
};

#[tokio::test]
async fn test_00100_import_literal() {
    setup_v8();
    let gx = new_gx([
        //. .
        ("other", {
            let mb = ModuleBuilder::default();
            SourceModule::V1(mb.str("Hello world (mortar)"))
        }),
        ("main", {
            let mb = ModuleBuilder::default();
            SourceModule::V1(mb.call(mb.scope_use("log"), mb.import(mb.str("other"))))
        }),
    ].into_iter().collect());
    let mb = ModuleBuilder::default();
    let logs = compile(gx.clone(), SourceSpecAbsolute::Virtual("main")).await.unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
    assert_eq!(redact_builtins(&gx.generated_mortar_scripts.lock().unwrap()), vec![r#"TODO"#]);
}
