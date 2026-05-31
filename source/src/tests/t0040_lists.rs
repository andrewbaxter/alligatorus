use {
    super::helpers::{ModuleBuilder, sanitize_builtins},
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
            gx.clone(),
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "my_list",
                            mb.call(
                                mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                                mb.tuple(vec![mb.str("c"), mb.str("d")])
                            )
                        ),
                        mb.call(mb.field(mb.scope_use("my_list"), mb.str("push")), mb.str("a")),
                        mb.call(mb.field(mb.scope_use("my_list"), mb.str("push")), mb.str("b")),
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.field(mb.scope_use("my_list"), mb.str("join")), mb.str("_"))
                        ),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "c_d_a_b");
    assert_eq!(sanitize_builtins(&gx.js_sources.lock().unwrap()), vec![r#"let e0;
let a0;
{
let e1;
let e2;
{
let e3;
e3 = "c";
e2 = e3;
}
let e4;
{
let e5;
e5 = "d";
e4 = e5;
}
e1 = [e2, e4];
a0 = e1;
}
{
let e6;
let e7;
e7 = a0;
let e8;
{
let e9;
e9 = "a";
e8 = e9;
}
e6 = e7.push(e8);
e0 = e6;
}
{
let e10;
let e11;
e11 = a0;
let e12;
{
let e13;
e13 = "b";
e12 = e13;
}
e10 = e11.push(e12);
e0 = e10;
}
{
let e14;
let e15;
e15 = BUILTIN;
let e16;
let e17;
e17 = a0;
let e18;
{
let e19;
e19 = "_";
e18 = e19;
}
e16 = e17.join(e18);
e14 = (e15)(e16);
e0 = e14;
}
e0;"#]);
}
