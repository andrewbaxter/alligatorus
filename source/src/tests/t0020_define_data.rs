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
fn test_00800define_data() {
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
                                    "make_upper",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "ndr",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("object")),
                                                        mb.str("upper"),
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("ndr"), mb.str("set_inner")),
                                                    mb.scope_use("string"),
                                                ),
                                                mb.bind(
                                                    "mnfr",
                                                    mb.call(
                                                        mb.field(mb.scope_use("ndr"), mb.str("func")),
                                                        mb.str("to_string"),
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("mnfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.scope_use("null"),
                                                            mb.scope_use("string"),
                                                            mb.stage(
                                                                mb.call(
                                                                    mb.field(
                                                                        mb.scope_use("inner"),
                                                                        mb.str("toUpperCase"),
                                                                    ),
                                                                    mb.null(),
                                                                ),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.field(mb.scope_use("ndr"), mb.str("new")),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.bind("val", mb.call(mb.scope_use("make_upper"), mb.str("hello"))),
                                mb.call(
                                    mb.scope_use("log"),
                                    mb.call(mb.field(mb.scope_use("val"), mb.str("to_string")), mb.null()),
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
    assert_eq!(res.logs[0].message, "HELLO");
    let dynamic_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(dynamic_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a7;
let e3;
e3 = 1;
let e4;
e4 = a8;
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
e11 = "upper";
e10 = e11;
}
e8 = e9["_1"](e10);
a1 = e8;
}
{
let e12;
let e13;
e13 = a1;
let e14;
e14 = a2;
e12 = e13["_3"](e14);
e5 = e12;
}
let a3;
{
let e15;
let e16;
e16 = a1;
let e17;
{
let e18;
e18 = "to_string";
e17 = e18;
}
e15 = e16["_4"](e17);
a3 = e15;
}
{
let e19;
let e20;
e20 = a3;
let e21;
e21 = a4;
let e22;
e22 = a5;
let e23;
e23 = a6;
e19 = e20["_0"](e21,e22,e23);
e5 = e19;
}
{
let e24;
let e25;
e25 = a1;
e24 = e25["_1"];
e5 = e24;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(dynamic_sources[1], r#"var d0_0_0_0;
{
d0_0_0_0 = function(a1) {
let a0 = this["\"_0\""];
let e0;
let e1;
e1 = a0;
let e2;
e2 = null;
e0 = e1["toUpperCase"](e2);
return e0;
};
}
let e3;
let a0;
{
let e4;
let e5;
{
let e6;
e6 = "hello";
e5 = e6;
}
let e7;
e7 = d0_0_0_0;
e4 = { "\"_0\"": e5, "\"_1\"": e7 };
a0 = e4;
}
{
let e8;
let e9;
e9 = BUILTIN;
let e10;
let e11;
e11 = a0;
let e12;
e12 = null;
e10 = e11["\"_1\""](e12);
e8 = (e9)(e10);
e3 = e8;
}
e3;"#);
}
