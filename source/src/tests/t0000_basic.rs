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
            gx.clone(),
            SourceId(0),
            SerialModule::V1(mb.call(mb.scope_use("log"), mb.str("Hello world (mortar)"))),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
    assert_eq!(*gx.js_sources.lock().unwrap(), vec![r#"let e0;
let e1;
e1 = b0;
let e2;
{
let e3;
e3 = "Hello world (mortar)";
e2 = e3;
}
e0 = (e1)(e2);
e0;"#]);
}

#[tokio::test]
async fn test_00101_type_error() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    compile(gx, SourceId(0), SerialModule::V1(mb.call(mb.scope_use("log"), mb.null()))).await.unwrap_err();
}

#[tokio::test]
async fn test_00200_bind_use() {
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
    assert_eq!(*gx.js_sources.lock().unwrap(), vec![r#"let e0;
let a0;
{
let e1;
{
let e2;
e2 = "Hello world (mortar)";
e1 = e2;
}
a0 = e1;
}
{
let e3;
let e4;
e4 = b0;
let e5;
e5 = a0;
e3 = (e4)(e5);
e0 = e3;
}
e0;"#]);
}

#[tokio::test]
async fn test_00300_stage_lower() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx.clone(),
            SourceId(0),
            SerialModule::V1(mb.call(mb.scope_use("log"), mb.lower(mb.stage(mb.str("Hello world (mortar)"))))),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
    assert_eq!(*gx.js_sources.lock().unwrap(), vec![r#"let e0;
let e1;
e1 = b18446744073709551615;
let e2;
e2 = a0;
let e3;
e3 = null;
let e4;
e4 = a1;
e0 = (e1)(e2,e3,e4);
e0;"#, r#"let e0;
let e1;
e1 = b0;
let e2;
{
let e3;
e3 = "Hello world (mortar)";
e2 = e3;
}
e0 = (e1)(e2);
e0;"#]);
}

#[tokio::test]
async fn test_00400_lower_nostage() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx.clone(),
            SourceId(0),
            SerialModule::V1(mb.call(mb.scope_use("log"), mb.lower(mb.str("Hello world (mortar)")))),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
    assert_eq!(*gx.js_sources.lock().unwrap(), vec![r#"let e0;
let e1;
e1 = b18446744073709551615;
let e2;
e2 = a0;
let e3;
e3 = null;
let e4;
e4 = a1;
e0 = (e1)(e2,e3,e4);
e0;"#, r#"let e0;
let e1;
e1 = b0;
let e2;
{
let e3;
e3 = "Hello world (mortar)";
e2 = e3;
}
e0 = (e1)(e2);
e0;"#]);
}

#[tokio::test]
async fn test_00500_method_call() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    compile(
        gx.clone(),
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
    assert_eq!(*gx.js_sources.lock().unwrap(), vec![r#"let e0;
let e1;
e1 = b0;
let e2;
let e3;
{
let e4;
e4 = "HELLO WORLD";
e3 = e4;
}
let e5;
e5 = null;
e2 = e3.toLowerCase(e5);
e0 = (e1)(e2);
e0;"#]);
}
