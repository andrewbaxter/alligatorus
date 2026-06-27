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
fn test_0000_const_fold_bind() {
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
                                mb.bind("x", mb.num(7.)),
                                mb.if_(
                                    vec![
                                        (
                                            mb.operator_lt(mb.scope_use("x"), mb.num(10.)),
                                            mb.call(mb.log(), mb.str("less")),
                                        ),
                                    ],
                                    Some(mb.call(mb.log(), mb.str("not less"))),
                                ),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "less");
    assert_eq!(sanitize_builtins(&res.generated_dynamic_scripts), vec![r#"let e0;
{
let e1;
let e2;
e2 = BUILTIN;
let e3;
{
let e4;
e4 = "less";
e3 = e4;
}
e1 = (e2)(e3);
e0 = e1;
}
e0;"#]);
}

#[test]
fn test_0010_no_const_fold_with_as() {
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
                                mb.bind("x", mb.as_(mb.num(7.), mb.types_number())),
                                mb.if_(
                                    vec![
                                        (
                                            mb.operator_lt(mb.scope_use("x"), mb.num(10.)),
                                            mb.call(mb.log(), mb.str("less")),
                                        ),
                                    ],
                                    Some(mb.call(mb.log(), mb.str("not less"))),
                                ),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "less");
    assert_eq!(sanitize_builtins(&res.generated_dynamic_scripts), vec![r#"let e0;
let a0;
{
let e1;
{
let e2;
e2 = 7.0;
e1 = e2;
}
a0 = e1;
}
{
let e3;
let e4;
let e5;
e5 = a0;
let e6;
{
let e7;
e7 = 10.0;
e6 = e7;
}
e4 = (e5 < e6);
if (e4) {
let e8;
let e9;
e9 = BUILTIN;
let e10;
{
let e11;
e11 = "less";
e10 = e11;
}
e8 = (e9)(e10);
e3 = e8;
} else {
let e12;
let e13;
e13 = BUILTIN;
let e14;
{
let e15;
e15 = "not less";
e14 = e15;
}
e12 = (e13)(e14);
e3 = e12;
}
e0 = e3;
}
e0;"#]);
}
