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
    //! Define a "to_string" interface with a single method, create an "uppercase
    //! string" data type that implements it, construct one, call to_string, and log.
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([("main", SourceModule::V1(mb.block(vec![
        mb.bind("make_upper", mb.lower(mb.block(vec![
            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
            // Create interface with a to_string method
            mb.bind("iface", mb.call(mb.field(mb.scope_use("d"), mb.str("new_interface")), mb.tuple(vec![]))),
            mb.bind(
                "iface_meth",
                mb.call(mb.field(mb.scope_use("iface"), mb.str("new_func")), mb.str("to_string")),
            ),
            mb.call(mb.field(mb.scope_use("iface_meth"), mb.str("arg")), mb.tuple(vec![mb.scope_use("null")])),
            mb.call(
                mb.field(mb.scope_use("iface_meth"), mb.str("ret")),
                mb.tuple(vec![mb.scope_use("string")]),
            ),
            // Create data type with string underlying
            mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("upper"))),
            mb.call(mb.field(mb.scope_use("ndr"), mb.str("set_underlying")), mb.scope_use("string")),
            // Implement interface on data type
            mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                mb.tuple(
                    vec![
                        mb.field(mb.scope_use("iface"), mb.str("type")),
                        mb.call(
                            mb.field(
                                mb.call(
                                    mb.scope_use("map"),
                                    mb.tuple(vec![mb.scope_use("string"), mb.scope_use("expr")]),
                                ),
                                mb.str("new"),
                            ),
                            mb.record(
                                vec![
                                    (
                                        "to_string",
                                        mb.stage(
                                            mb.call(
                                                mb.field(mb.scope_use("self"), mb.str("toUpperCase")),
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
            // Return the constructor
            mb.field(mb.scope_use("ndr"), mb.str("new")),
        ]))),
        // Create an instance and call to_string
        mb.bind("val", mb.call(mb.scope_use("make_upper"), mb.str("hello"))),
        mb.call(mb.scope_use("log"), mb.call(mb.field(mb.scope_use("val"), mb.str("to_string")), mb.null())),
    ])))].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    let js_sources = sanitize_builtins(&res.generated_mortar_scripts);
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
e13 = e14["_0"](e15);
e4 = e13;
}
{
let e16;
let e17;
e17 = a2;
let e18;
e18 = a4;
e16 = e17["_1"](e18);
e4 = e16;
}
let a5;
{
let e19;
let e20;
e20 = a0;
let e21;
{
let e22;
e22 = "upper";
e21 = e22;
}
e19 = e20["_1"](e21);
a5 = e19;
}
{
let e23;
let e24;
e24 = a5;
let e25;
e25 = a6;
e23 = e24["_3"](e25);
e4 = e23;
}
{
let e26;
let e27;
e27 = a5;
let e28;
let e29;
e29 = a1;
e28 = e29["_0"];
let e30;
let e31;
e31 = "\"to_string\"";
let e32;
e32 = a7;
e30 = { [e31]: e32 };
e26 = e27["_5"](e28,e30);
e4 = e26;
}
{
let e33;
let e34;
e34 = a5;
e33 = e34["_1"];
e4 = e33;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
    assert_eq!(js_sources[1], r#"function d0_0_0() {
let a1 = this._0;
let e0;
let e1;
e1 = a1;
let e2;
e2 = null;
e0 = e1["toUpperCase"](e2);
return e0;
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
e4 = { "_0": e5, "_im0": e7 };
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
e10 = e11["_im0"](e12);
e8 = (e9)(e10);
e3 = e8;
}
e3;"#);
}

#[test]
fn test_00910interface_cast() {
    //! Like test_00900 but also defines a standalone `do_log` function that takes a
    //! `to_string` interface argument, calls `.to_string()` on it, and logs the
    //! result. The data type is passed to `do_log`, requiring an implicit cast from
    //! the data type to the interface.
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([("main", SourceModule::V1(mb.block(vec![
        // Lower block defines interface, data type, and function; returns exports
        mb.bind("defs", mb.lower(mb.block(vec![
            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
            // Create interface with a to_string method
            mb.bind("iface", mb.call(mb.field(mb.scope_use("d"), mb.str("new_interface")), mb.tuple(vec![]))),
            mb.bind(
                "iface_meth",
                mb.call(mb.field(mb.scope_use("iface"), mb.str("new_func")), mb.str("to_string")),
            ),
            mb.call(mb.field(mb.scope_use("iface_meth"), mb.str("arg")), mb.tuple(vec![mb.scope_use("null")])),
            mb.call(
                mb.field(mb.scope_use("iface_meth"), mb.str("ret")),
                mb.tuple(vec![mb.scope_use("string")]),
            ),
            // Create data type with string underlying
            mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("upper"))),
            mb.call(mb.field(mb.scope_use("ndr"), mb.str("set_underlying")), mb.scope_use("string")),
            // Implement interface on data type
            mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                mb.tuple(
                    vec![
                        mb.field(mb.scope_use("iface"), mb.str("type")),
                        mb.call(
                            mb.field(
                                mb.call(
                                    mb.scope_use("map"),
                                    mb.tuple(vec![mb.scope_use("string"), mb.scope_use("expr")]),
                                ),
                                mb.str("new"),
                            ),
                            mb.record(
                                vec![
                                    (
                                        "to_string",
                                        mb.stage(
                                            mb.call(
                                                mb.field(mb.scope_use("self"), mb.str("toUpperCase")),
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
            // Define do_log: takes a to_string interface arg, calls .to_string(), logs result
            mb.bind("do_log_fn", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("do_log"))),
            mb.call(
                mb.field(mb.scope_use("do_log_fn"), mb.str("arg_type")),
                mb.field(mb.scope_use("iface"), mb.str("type")),
            ),
            mb.call(mb.field(mb.scope_use("do_log_fn"), mb.str("ret")), mb.tuple(vec![mb.scope_use("null")])),
            mb.call(
                mb.field(mb.scope_use("do_log_fn"), mb.str("impl")),
                mb.tuple(
                    vec![
                        mb.stage(
                            mb.call(
                                mb.scope_use("log"),
                                mb.call(mb.field(mb.scope_use("arg"), mb.str("to_string")), mb.null()),
                            ),
                        ),
                    ],
                ),
            ),
            // Return exports (contains both "do_log" and "upper")
            mb.call(mb.field(mb.scope_use("d"), mb.str("build")), mb.tuple(vec![])),
        ]))),
        // Extract do_log function and upper constructor from exports
        mb.bind("do_log", mb.field(mb.scope_use("defs"), mb.str("do_log"))),
        mb.bind("make_upper", mb.field(mb.scope_use("defs"), mb.str("upper"))),
        // Call do_log with a new upper value - requires implicit cast to interface
        mb.call(mb.scope_use("do_log"), mb.tuple(vec![mb.call(mb.scope_use("make_upper"), mb.str("hello"))])),
    ])))].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    let js_sources = sanitize_builtins(&res.generated_mortar_scripts);
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
e13 = e14["_0"](e15);
e4 = e13;
}
{
let e16;
let e17;
e17 = a2;
let e18;
e18 = a4;
e16 = e17["_1"](e18);
e4 = e16;
}
let a5;
{
let e19;
let e20;
e20 = a0;
let e21;
{
let e22;
e22 = "upper";
e21 = e22;
}
e19 = e20["_1"](e21);
a5 = e19;
}
{
let e23;
let e24;
e24 = a5;
let e25;
e25 = a6;
e23 = e24["_3"](e25);
e4 = e23;
}
{
let e26;
let e27;
e27 = a5;
let e28;
let e29;
e29 = a1;
e28 = e29["_0"];
let e30;
let e31;
e31 = "\"to_string\"";
let e32;
e32 = a7;
e30 = { [e31]: e32 };
e26 = e27["_5"](e28,e30);
e4 = e26;
}
let a8;
{
let e33;
let e34;
e34 = a0;
let e35;
{
let e36;
e36 = "do_log";
e35 = e36;
}
e33 = e34["_0"](e35);
a8 = e33;
}
{
let e37;
let e38;
e38 = a8;
let e39;
let e40;
e40 = a1;
e39 = e40["_0"];
e37 = e38["_4"](e39);
e4 = e37;
}
{
let e41;
let e42;
e42 = a8;
let e43;
e43 = a9;
e41 = e42["_5"](e43);
e4 = e41;
}
{
let e44;
let e45;
e45 = a8;
let e46;
e46 = a10;
e44 = e45["_2"](e46);
e4 = e44;
}
{
let e47;
let e48;
e48 = a0;
e47 = e48["_2"]();
e4 = e47;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
    assert_eq!(js_sources[1], r#"function d0_0_1(a0) {
let e0;
let e1;
e1 = BUILTIN;
let e2;
let e3;
e3 = a0;
let e4;
e4 = null;
e2 = e3["_im0"](e4);
e0 = (e1)(e2);
return e0;
}
function d0_0_0() {
let a0 = this._0;
let e5;
let e6;
e6 = a0;
let e7;
e7 = null;
e5 = e6["toUpperCase"](e7);
return e5;
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
e11 = { "_0": e12, "_im0": e14 };
e9 = (e10)(e11);
e8 = e9;
}
e8;"#);
}

#[test]
fn test_00920interface_wrong_map_value_type() {
    //! Using map(string, null) for impl_interface should be rejected because the
    //! record values are Expr, not null.
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([("main", SourceModule::V1(mb.block(vec![mb.bind("make_upper", mb.lower(mb.block(vec![
        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
        mb.bind("iface", mb.call(mb.field(mb.scope_use("d"), mb.str("new_interface")), mb.tuple(vec![]))),
        mb.bind("iface_meth", mb.call(mb.field(mb.scope_use("iface"), mb.str("new_func")), mb.str("to_string"))),
        mb.call(mb.field(mb.scope_use("iface_meth"), mb.str("arg")), mb.tuple(vec![mb.scope_use("null")])),
        mb.call(mb.field(mb.scope_use("iface_meth"), mb.str("ret")), mb.tuple(vec![mb.scope_use("string")])),
        mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("upper"))),
        mb.call(mb.field(mb.scope_use("ndr"), mb.str("set_underlying")), mb.scope_use("string")),
        // Use map(string, null) — wrong value type, should be expr
        mb.call(
            mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
            mb.tuple(
                vec![
                    mb.field(mb.scope_use("iface"), mb.str("type")),
                    mb.call(
                        mb.field(
                            mb.call(
                                mb.scope_use("map"),
                                mb.tuple(vec![mb.scope_use("string"), mb.scope_use("null")]),
                            ),
                            mb.str("new"),
                        ),
                        mb.record(
                            vec![
                                (
                                    "to_string",
                                    mb.stage(
                                        mb.call(mb.field(mb.scope_use("self"), mb.str("toUpperCase")), mb.null()),
                                    ),
                                ),
                            ],
                        ),
                    ),
                ],
            ),
        ),
        mb.field(mb.scope_use("ndr"), mb.str("new")),
    ])))])))].into_iter().collect());
    compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap_err();
}
