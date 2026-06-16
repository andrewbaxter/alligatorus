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
e3 = a9;
let e4;
let a0;
{
let e5;
let e6;
e6 = BUILTIN;
e5 = (e6)();
a0 = e5;
}
let a1;
{
let e7;
let e8;
e8 = a0;
e7 = e8["_4"]();
a1 = e7;
}
let a2;
{
let e9;
let e10;
e10 = a1;
let e11;
{
let e12;
e12 = "to_string";
e11 = e12;
}
e9 = e10["_2"](e11);
a2 = e9;
}
{
let e13;
let e14;
e14 = a2;
let e15;
e15 = a3;
let e16;
e16 = a4;
e13 = e14["_0"](e15,e16);
e4 = e13;
}
let a5;
{
let e17;
let e18;
e18 = a0;
let e19;
{
let e20;
e20 = "upper";
e19 = e20;
}
e17 = e18["_1"](e19);
a5 = e17;
}
{
let e21;
let e22;
e22 = a5;
let e23;
e23 = a6;
e21 = e22["_3"](e23);
e4 = e21;
}
{
let e24;
let e25;
e25 = a5;
let e26;
let e27;
e27 = a1;
e26 = e27["_0"];
let e28;
let e29;
e29 = "\"to_string\"";
let e30;
e30 = a7;
e28 = { [e29]: e30 };
e24 = e25["_5"](e26,e28);
e4 = e24;
}
{
let e31;
let e32;
e32 = a5;
e31 = e32["_1"];
e4 = e31;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
    assert_eq!(js_sources[1], r#"var d0_0_0;
{
d0_0_0 = function(a2) {
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
e7 = d0_0_0;
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
e3 = a12;
let e4;
let a0;
{
let e5;
let e6;
e6 = BUILTIN;
e5 = (e6)();
a0 = e5;
}
let a1;
{
let e7;
let e8;
e8 = a0;
e7 = e8["_4"]();
a1 = e7;
}
let a2;
{
let e9;
let e10;
e10 = a1;
let e11;
{
let e12;
e12 = "to_string";
e11 = e12;
}
e9 = e10["_2"](e11);
a2 = e9;
}
{
let e13;
let e14;
e14 = a2;
let e15;
e15 = a3;
let e16;
e16 = a4;
e13 = e14["_0"](e15,e16);
e4 = e13;
}
let a5;
{
let e17;
let e18;
e18 = a0;
let e19;
{
let e20;
e20 = "upper";
e19 = e20;
}
e17 = e18["_1"](e19);
a5 = e17;
}
{
let e21;
let e22;
e22 = a5;
let e23;
e23 = a6;
e21 = e22["_3"](e23);
e4 = e21;
}
{
let e24;
let e25;
e25 = a5;
let e26;
let e27;
e27 = a1;
e26 = e27["_0"];
let e28;
let e29;
e29 = "\"to_string\"";
let e30;
e30 = a7;
e28 = { [e29]: e30 };
e24 = e25["_5"](e26,e28);
e4 = e24;
}
let a8;
{
let e31;
let e32;
e32 = a0;
let e33;
{
let e34;
e34 = "do_log";
e33 = e34;
}
e31 = e32["_0"](e33);
a8 = e31;
}
{
let e35;
let e36;
e36 = a8;
let e37;
let e38;
e38 = BUILTIN;
let e39;
let e40;
e40 = a1;
e39 = e40["_0"];
e37 = (e38)(e39);
let e41;
e41 = a9;
let e42;
e42 = a10;
e35 = e36["_2"](e37,e41,e42);
e4 = e35;
}
{
let e43;
let e44;
e44 = a0;
e43 = e44["_2"]();
e4 = e43;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
    assert_eq!(js_sources[1], r#"var d0_0_1;
{
d0_0_1 = function(a0) {
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
var d0_0_0;
{
d0_0_0 = function(a2) {
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
e10 = d0_0_1;
let e11;
let e12;
{
let e13;
e13 = "hello";
e12 = e13;
}
let e14;
e14 = d0_0_0;
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
