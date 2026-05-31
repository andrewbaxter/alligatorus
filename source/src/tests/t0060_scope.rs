use crate::{
    compile,
    eval::setup_v8,
    langhelp::SourceId,
    new_gx,
    syntax::SerialModule,
    tests::helpers::{ModuleBuilder, sanitize_builtins},
};

#[tokio::test]
async fn test_0000_scope_ok() {
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
    assert_eq!(sanitize_builtins(&gx.js_sources.lock().unwrap()), vec![r#"let e0;
let a0;
{
let e1;
{
let e2;
e2 = "zebra";
e1 = e2;
}
a0 = e1;
}
{
let e3;
let a1;
{
let e4;
{
let e5;
e5 = "apple";
e4 = e5;
}
a1 = e4;
}
{
let e6;
let e7;
e7 = BUILTIN;
let e8;
e8 = a1;
e6 = (e7)(e8);
e3 = e6;
}
e0 = e3;
}
{
let e9;
let e10;
e10 = BUILTIN;
let e11;
e11 = a0;
e9 = (e10)(e11);
e0 = e9;
}
e0;"#]);
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
