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
    assert_eq!(logs.len(), 3);
    assert_eq!(logs[0].message, "1");
    assert_eq!(logs[1].message, "2");
    assert_eq!(logs[2].message, "7");
    assert_eq!(*gx.js_sources.lock().unwrap(), vec![r#"let e0;
let a0;
{
let e1;
let e2;
{
let e3;
e3 = 1.0;
e2 = e3;
}
let e4;
{
let e5;
e5 = 2.0;
e4 = e5;
}
let e6;
{
let e7;
e7 = 7.0;
e6 = e7;
}
e1 = [e2, e4, e6];
a0 = e1;
}
{
let e8;
let e9;
e9 = a0;
for (let a1 of e9) {
let e10;
{
let e11;
let e12;
e12 = b1148;
let e13;
let e14;
e14 = a1;
e13 = e14.toString();
e11 = (e12)(e13);
e10 = e11;
}
e8 = e10;
}
e0 = e8;
}
e0;"#]);
}
