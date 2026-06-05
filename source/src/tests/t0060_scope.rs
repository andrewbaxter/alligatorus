use {
    crate::{
        compile,
        eval::setup_v8,
        syntax::{
            SourceModule,
            SourceSpecAbsolute,
        },
    },
    super::helpers::{
        ModuleBuilder,
        new_gx,
        sanitize_builtins,
    },
};

#[test]
fn test_0000_scope_ok() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.block(
                            vec![
                                mb.bind("x", mb.str("zebra")),
                                mb.block(
                                    vec![
                                        mb.bind("x", mb.str("apple")),
                                        mb.call(mb.scope_use("log"), mb.scope_use("x"),),
                                    ]
                                ),
                                mb.call(mb.scope_use("log"), mb.scope_use("x"),),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let logs = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(logs.len(), 2);
    assert_eq!(logs[0].message, "apple");
    assert_eq!(logs[1].message, "zebra");
    assert_eq!(sanitize_builtins(&gx.generated_mortar_scripts.lock().unwrap()), vec![r#"let e0;
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

#[test]
fn test_0010_scope_bad() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.block(
                            vec![
                                mb.block(vec![mb.bind("x", mb.str("apple")),]),
                                mb.call(mb.scope_use("log"), mb.scope_use("x"),),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).err().unwrap();
}
