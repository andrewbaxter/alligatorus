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
fn test_0000_number_lit() {
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
                                mb.bind("x", mb.as_(mb.num(4.), mb.types_number())),
                                mb.if_(
                                    vec![
                                        (
                                            mb.operator_lt(mb.scope_use("x"), mb.num(5.)),
                                            mb.set(
                                                mb.scope_use("x"),
                                                mb.operator_add(mb.scope_use("x"), mb.num(7.)),
                                            ),
                                        ),
                                    ],
                                    None,
                                ),
                                mb.call(
                                    mb.log(),
                                    mb.call(mb.field(mb.scope_use("x"), mb.str("to_string")), mb.tuple(vec![])),
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
    assert_eq!(res.logs[0].message, "11");
    assert_eq!(sanitize_builtins(&res.generated_dynamic_scripts), vec![r#"let e0;
let a0;
{
let e1;
{
let e2;
e2 = 4.0;
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
e7 = 5.0;
e6 = e7;
}
e4 = (e5 < e6);
if (e4) {
let e8;
{
let e9;
e9 = a0;
e8 = e9;
}
{
let e10;
let e11;
let e12;
e12 = a0;
let e13;
{
let e14;
e14 = 7.0;
e13 = e14;
}
e11 = (e12 + e13);
a0 = e11;
e10 = null;
e8 = e10;
}
e3 = e8;
}
e0 = e3;
}
{
let e15;
let e16;
e16 = BUILTIN;
let e17;
let e18;
e18 = a0;
e17 = e18["toString"]();
e15 = (e16)(e17);
e0 = e15;
}
e0;"#]);
}

#[test]
fn test_00100_number_dynamic() {
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
                                mb.bind(
                                    "maybe_add",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "builder",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("func")),
                                                        mb.str("maybe add 7"),
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("builder"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_number(),
                                                            mb.types_number(),
                                                            mb.stage(
                                                                mb.seq(
                                                                    vec![
                                                                        mb.if_(
                                                                            vec![
                                                                                (
                                                                                    mb.operator_lt(
                                                                                        mb.scope_use("arg"),
                                                                                        mb.num(5.),
                                                                                    ),
                                                                                    mb.set(
                                                                                        mb.scope_use("arg"),
                                                                                        mb.operator_add(
                                                                                            mb.scope_use("arg"),
                                                                                            mb.num(7.),
                                                                                        ),
                                                                                    ),
                                                                                ),
                                                                            ],
                                                                            None,
                                                                        ),
                                                                        mb.scope_use("arg"),
                                                                    ],
                                                                ),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.field(mb.scope_use("builder"), mb.str("func")),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.call(
                                    mb.log(),
                                    mb.call(
                                        mb.field(
                                            mb.call(mb.scope_use("maybe_add"), mb.tuple(vec![mb.num(4.)])),
                                            mb.str("to_string"),
                                        ),
                                        mb.tuple(vec![]),
                                    ),
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
    assert_eq!(res.logs[0].message, "11");
    let dynamic_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(dynamic_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a5;
let e3;
e3 = 1;
let e4;
e4 = a6;
let e5;
let a0;
{
let e6;
let e7;
e7 = BUILTIN;
e6 = (e7)();
a0 = e6;
}
let a1;
{
let e8;
let e9;
e9 = a0;
let e10;
{
let e11;
e11 = "maybe add 7";
e10 = e11;
}
e8 = e9["_0"](e10);
a1 = e8;
}
{
let e12;
let e13;
e13 = a1;
let e14;
e14 = a2;
let e15;
e15 = a3;
let e16;
e16 = a4;
e12 = e13["_2"](e14,e15,e16);
e5 = e12;
}
{
let e17;
let e18;
e18 = a1;
e17 = e18["_0"];
e5 = e17;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(dynamic_sources[1], r#"var d0_0_0_0;
{
d0_0_0_0 = function(a0) {
let e0;
{
let e1;
let e2;
let e3;
e3 = a0;
let e4;
{
let e5;
e5 = 5.0;
e4 = e5;
}
e2 = (e3 < e4);
if (e2) {
let e6;
{
let e7;
e7 = a0;
e6 = e7;
}
{
let e8;
let e9;
let e10;
e10 = a0;
let e11;
{
let e12;
e12 = 7.0;
e11 = e12;
}
e9 = (e10 + e11);
a0 = e9;
e8 = null;
e6 = e8;
}
e1 = e6;
}
e0 = e1;
}
{
let e13;
e13 = a0;
e0 = e13;
}
return e0;
};
}
let e14;
{
let e15;
let e16;
e16 = BUILTIN;
let e17;
let e18;
let e19;
e19 = d0_0_0_0;
let e20;
{
let e21;
e21 = 4.0;
e20 = e21;
}
e18 = (e19)(e20);
e17 = e18["toString"]();
e15 = (e16)(e17);
e14 = e15;
}
e14;"#);
}
