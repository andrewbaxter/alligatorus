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
                        mb.seq(
                            vec![
                                mb.bind("x", mb.str("zebra")),
                                mb.seq(
                                    vec![
                                        mb.bind("x", mb.str("apple")),
                                        mb.call(mb.scope_use("log"), mb.scope_use("x")),
                                    ],
                                ),
                                mb.call(mb.scope_use("log"), mb.scope_use("x")),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 2);
    assert_eq!(res.logs[0].message, "apple");
    assert_eq!(res.logs[1].message, "zebra");
    assert_eq!(sanitize_builtins(&res.generated_dynamic_scripts), vec![r#"let e0;
{
let e1;
{
let e2;
let e3;
e3 = BUILTIN;
let e4;
{
let e5;
e5 = "apple";
e4 = e5;
}
e2 = (e3)(e4);
e1 = e2;
}
e0 = e1;
}
{
let e6;
let e7;
e7 = BUILTIN;
let e8;
{
let e9;
e9 = "zebra";
e8 = e9;
}
e6 = (e7)(e8);
e0 = e6;
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
                        mb.seq(
                            vec![
                                mb.seq(vec![mb.bind("x", mb.str("apple"))]),
                                mb.call(mb.scope_use("log"), mb.scope_use("x")),
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
