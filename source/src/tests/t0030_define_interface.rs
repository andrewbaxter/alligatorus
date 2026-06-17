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
fn test_00900define_interface() {
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
                                                    "iface",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("interface")),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "iface_meth",
                                                    mb.call(
                                                        mb.field(mb.scope_use("iface"), mb.str("func")),
                                                        mb.str("to_string"),
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("iface_meth"), mb.str("impl")),
                                                    mb.tuple(vec![mb.scope_use("null"), mb.scope_use("string")]),
                                                ),
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
                                                mb.call(
                                                    mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.field(mb.scope_use("iface"), mb.str("type")),
                                                            mb.call(
                                                                mb.field(
                                                                    mb.call(
                                                                        mb.scope_use("map"),
                                                                        mb.tuple(
                                                                            vec![
                                                                                mb.scope_use("constkey"),
                                                                                mb.scope_use("expr"),
                                                                            ],
                                                                        ),
                                                                    ),
                                                                    mb.str("new"),
                                                                ),
                                                                mb.record(
                                                                    vec![
                                                                        (
                                                                            "to_string",
                                                                            mb.stage(
                                                                                mb.call(
                                                                                    mb.field(
                                                                                        mb.scope_use("inner"),
                                                                                        mb.str("toUpperCase"),
                                                                                    ),
                                                                                    mb.null(),
                                                                                ),
                                                                            ),
                                                                        ),
                                                                    ],
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
    let js_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(js_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a8;
let e3;
e3 = 1;
let e4;
e4 = a9;
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
e8 = e9["_4"]();
a1 = e8;
}
let a2;
{
let e10;
let e11;
e11 = a1;
let e12;
{
let e13;
e13 = "to_string";
e12 = e13;
}
e10 = e11["_2"](e12);
a2 = e10;
}
{
let e14;
let e15;
e15 = a2;
let e16;
e16 = a3;
let e17;
e17 = a4;
e14 = e15["_0"](e16,e17);
e5 = e14;
}
let a5;
{
let e18;
let e19;
e19 = a0;
let e20;
{
let e21;
e21 = "upper";
e20 = e21;
}
e18 = e19["_1"](e20);
a5 = e18;
}
{
let e22;
let e23;
e23 = a5;
let e24;
e24 = a6;
e22 = e23["_3"](e24);
e5 = e22;
}
{
let e25;
let e26;
e26 = a5;
let e27;
let e28;
e28 = a1;
e27 = e28["_0"];
let e29;
let e30;
e30 = "\"to_string\"";
let e31;
e31 = a7;
e29 = { [e30]: e31 };
e25 = e26["_5"](e27,e29);
e5 = e25;
}
{
let e32;
let e33;
e33 = a5;
e32 = e33["_1"];
e5 = e32;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(js_sources[1], r#"var d0_0_0_0;
{
d0_0_0_0 = function(a2) {
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
e4 = { "\"_0\"": e5, "\"_im0\"": e7 };
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
e10 = e11["\"_im0\""](e12);
e8 = (e9)(e10);
e3 = e8;
}
e3;"#);
}

#[test]
fn test_00910interface_cast() {
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
                                    "defs",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "iface",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("interface")),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "iface_meth",
                                                    mb.call(
                                                        mb.field(mb.scope_use("iface"), mb.str("func")),
                                                        mb.str("to_string"),
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("iface_meth"), mb.str("impl")),
                                                    mb.tuple(vec![mb.scope_use("null"), mb.scope_use("string")]),
                                                ),
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
                                                mb.call(
                                                    mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.field(mb.scope_use("iface"), mb.str("type")),
                                                            mb.call(
                                                                mb.field(
                                                                    mb.call(
                                                                        mb.scope_use("map"),
                                                                        mb.tuple(
                                                                            vec![
                                                                                mb.scope_use("constkey"),
                                                                                mb.scope_use("expr"),
                                                                            ],
                                                                        ),
                                                                    ),
                                                                    mb.str("new"),
                                                                ),
                                                                mb.record(
                                                                    vec![
                                                                        (
                                                                            "to_string",
                                                                            mb.stage(
                                                                                mb.call(
                                                                                    mb.field(
                                                                                        mb.scope_use("inner"),
                                                                                        mb.str("toUpperCase"),
                                                                                    ),
                                                                                    mb.null(),
                                                                                ),
                                                                            ),
                                                                        ),
                                                                    ],
                                                                ),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.bind(
                                                    "do_log_fn",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("func")),
                                                        mb.str("do_log"),
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("do_log_fn"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.field(mb.scope_use("iface"), mb.str("type")),
                                                            mb.scope_use("null"),
                                                            mb.stage(
                                                                mb.call(
                                                                    mb.scope_use("log"),
                                                                    mb.call(
                                                                        mb.field(
                                                                            mb.scope_use("arg"),
                                                                            mb.str("to_string"),
                                                                        ),
                                                                        mb.null(),
                                                                    ),
                                                                ),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("d"), mb.str("build")),
                                                    mb.tuple(vec![]),
                                                ),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.bind("do_log", mb.field(mb.scope_use("defs"), mb.str("do_log"))),
                                mb.bind("make_upper", mb.field(mb.scope_use("defs"), mb.str("upper"))),
                                mb.call(
                                    mb.scope_use("do_log"),
                                    mb.tuple(vec![mb.call(mb.scope_use("make_upper"), mb.str("hello"))]),
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
    let js_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(js_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a11;
let e3;
e3 = 1;
let e4;
e4 = a12;
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
e8 = e9["_4"]();
a1 = e8;
}
let a2;
{
let e10;
let e11;
e11 = a1;
let e12;
{
let e13;
e13 = "to_string";
e12 = e13;
}
e10 = e11["_2"](e12);
a2 = e10;
}
{
let e14;
let e15;
e15 = a2;
let e16;
e16 = a3;
let e17;
e17 = a4;
e14 = e15["_0"](e16,e17);
e5 = e14;
}
let a5;
{
let e18;
let e19;
e19 = a0;
let e20;
{
let e21;
e21 = "upper";
e20 = e21;
}
e18 = e19["_1"](e20);
a5 = e18;
}
{
let e22;
let e23;
e23 = a5;
let e24;
e24 = a6;
e22 = e23["_3"](e24);
e5 = e22;
}
{
let e25;
let e26;
e26 = a5;
let e27;
let e28;
e28 = a1;
e27 = e28["_0"];
let e29;
let e30;
e30 = "\"to_string\"";
let e31;
e31 = a7;
e29 = { [e30]: e31 };
e25 = e26["_5"](e27,e29);
e5 = e25;
}
let a8;
{
let e32;
let e33;
e33 = a0;
let e34;
{
let e35;
e35 = "do_log";
e34 = e35;
}
e32 = e33["_0"](e34);
a8 = e32;
}
{
let e36;
let e37;
e37 = a8;
let e38;
let e39;
e39 = BUILTIN;
let e40;
let e41;
e41 = a1;
e40 = e41["_0"];
e38 = (e39)(e40);
let e42;
e42 = a9;
let e43;
e43 = a10;
e36 = e37["_2"](e38,e42,e43);
e5 = e36;
}
{
let e44;
let e45;
e45 = a0;
e44 = e45["_2"]();
e5 = e44;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(js_sources[1], r#"var d0_0_0_1;
{
d0_0_0_1 = function(a0) {
let e0;
let e1;
e1 = BUILTIN;
let e2;
let e3;
e3 = a0;
let e4;
e4 = null;
e2 = e3["\"_im0\""](e4);
e0 = (e1)(e2);
return e0;
};
}
var d0_0_0_0;
{
d0_0_0_0 = function(a2) {
let a0 = this["\"_0\""];
let e5;
let e6;
e6 = a0;
let e7;
e7 = null;
e5 = e6["toUpperCase"](e7);
return e5;
};
}
let e8;
{
let e9;
let e10;
e10 = d0_0_0_1;
let e11;
let e12;
{
let e13;
e13 = "hello";
e12 = e13;
}
let e14;
e14 = d0_0_0_0;
e11 = { "\"_0\"": e12, "\"_im0\"": e14 };
e9 = (e10)(e11);
e8 = e9;
}
e8;"#);
}

#[test]
fn test_00920interface_wrong_map_value_type() {
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
                                                    "iface",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("interface")),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "iface_meth",
                                                    mb.call(
                                                        mb.field(mb.scope_use("iface"), mb.str("func")),
                                                        mb.str("to_string"),
                                                    ),
                                                ),
                                                mb.call(
                                                    mb.field(mb.scope_use("iface_meth"), mb.str("impl")),
                                                    mb.tuple(vec![mb.scope_use("null"), mb.scope_use("string")]),
                                                ),
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
                                                mb.call(
                                                    mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.field(mb.scope_use("iface"), mb.str("type")),
                                                            mb.call(
                                                                mb.field(
                                                                    mb.call(
                                                                        mb.scope_use("map"),
                                                                        mb.tuple(
                                                                            vec![
                                                                                mb.scope_use("string"),
                                                                                mb.scope_use("null"),
                                                                            ],
                                                                        ),
                                                                    ),
                                                                    mb.str("new"),
                                                                ),
                                                                mb.record(
                                                                    vec![
                                                                        (
                                                                            "to_string",
                                                                            mb.stage(
                                                                                mb.call(
                                                                                    mb.field(
                                                                                        mb.scope_use("inner"),
                                                                                        mb.str("toUpperCase"),
                                                                                    ),
                                                                                    mb.null(),
                                                                                ),
                                                                            ),
                                                                        ),
                                                                    ],
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
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap_err();
}
