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
fn test_0000_tagged_union_match() {
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
                                    "strnum",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "builder",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("union")),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_str",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_num",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")]),
                                                    ),
                                                ),
                                                mb.record(
                                                    vec![
                                                        ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                                        ("str", mb.scope_use("new_str")),
                                                        ("num", mb.scope_use("new_num")),
                                                    ],
                                                ),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.bind(
                                    "x",
                                    mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.)),
                                ),
                                mb.match_(
                                    mb.scope_use("x"),
                                    mb.str("y"),
                                    vec![
                                        (mb.str("strtag"), mb.call(mb.scope_use("log"), mb.scope_use("y"))),
                                        (
                                            mb.str("numtag"),
                                            mb.call(
                                                mb.scope_use("log"),
                                                mb.call(
                                                    mb.field(mb.scope_use("y"), mb.str("to_string")),
                                                    mb.tuple(vec![]),
                                                ),
                                            ),
                                        ),
                                    ],
                                    None,
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
    assert_eq!(res.logs[0].message, "7");
    let js_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(js_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a6;
let e3;
e3 = 1;
let e4;
e4 = a7;
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
e8 = e9["_3"]();
a1 = e8;
}
let a3;
{
let e10;
let e11;
e11 = a1;
let e12;
{
let e13;
e13 = "strtag";
e12 = e13;
}
let e14;
e14 = a2;
e10 = e11["_1"](e12,e14);
a3 = e10;
}
let a5;
{
let e15;
let e16;
e16 = a1;
let e17;
{
let e18;
e18 = "numtag";
e17 = e18;
}
let e19;
e19 = a4;
e15 = e16["_1"](e17,e19);
a5 = e15;
}
{
let e20;
let e21;
e21 = a5;
let e22;
e22 = a3;
let e23;
let e24;
e24 = a1;
e23 = e24["_0"];
e20 = { "\"num\"": e21, "\"str\"": e22, "\"type\"": e23 };
e5 = e20;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(js_sources[1], r#"let e0;
let a0;
{
let e1;
let e2;
{
let e3;
e3 = 7.0;
e2 = e3;
}
e1 = { "\"t1\"": e2 };
a0 = e1;
}
{
let e4;
let e5;
e5 = a0;
{ let [__tag, a1] = Object.entries(e5)[0];
switch (__tag) {
case "\"t0\"": {
let e6;
let e7;
e7 = BUILTIN;
let e8;
e8 = a1;
e6 = (e7)(e8);
e4 = e6;
break; }
case "\"t1\"": {
let e9;
let e10;
e10 = BUILTIN;
let e11;
let e12;
e12 = a1;
e11 = e12["toString"]();
e9 = (e10)(e11);
e4 = e9;
break; }
} }
e0 = e4;
}
e0;"#);
}

#[test]
fn test_0010_tagged_union_match_fallback() {
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
                                    "strnum",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "builder",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("union")),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_str",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_num",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")]),
                                                    ),
                                                ),
                                                mb.record(
                                                    vec![
                                                        ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                                        ("str", mb.scope_use("new_str")),
                                                        ("num", mb.scope_use("new_num")),
                                                    ],
                                                ),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.bind(
                                    "x",
                                    mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.)),
                                ),
                                mb.match_(
                                    mb.scope_use("x"),
                                    mb.str("y"),
                                    vec![(mb.str("strtag"), mb.call(mb.scope_use("log"), mb.scope_use("y")))],
                                    Some(mb.call(mb.scope_use("log"), mb.str("fallback"))),
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
    assert_eq!(res.logs[0].message, "fallback");
    let js_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(js_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a6;
let e3;
e3 = 1;
let e4;
e4 = a7;
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
e8 = e9["_3"]();
a1 = e8;
}
let a3;
{
let e10;
let e11;
e11 = a1;
let e12;
{
let e13;
e13 = "strtag";
e12 = e13;
}
let e14;
e14 = a2;
e10 = e11["_1"](e12,e14);
a3 = e10;
}
let a5;
{
let e15;
let e16;
e16 = a1;
let e17;
{
let e18;
e18 = "numtag";
e17 = e18;
}
let e19;
e19 = a4;
e15 = e16["_1"](e17,e19);
a5 = e15;
}
{
let e20;
let e21;
e21 = a5;
let e22;
e22 = a3;
let e23;
let e24;
e24 = a1;
e23 = e24["_0"];
e20 = { "\"num\"": e21, "\"str\"": e22, "\"type\"": e23 };
e5 = e20;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(js_sources[1], r#"let e0;
let a0;
{
let e1;
let e2;
{
let e3;
e3 = 7.0;
e2 = e3;
}
e1 = { "\"t1\"": e2 };
a0 = e1;
}
{
let e4;
let e5;
e5 = a0;
{ let [__tag, a1] = Object.entries(e5)[0];
switch (__tag) {
case "\"t0\"": {
let e6;
let e7;
e7 = BUILTIN;
let e8;
e8 = a1;
e6 = (e7)(e8);
e4 = e6;
break; }
default: {
let e9;
let e10;
e10 = BUILTIN;
let e11;
{
let e12;
e12 = "fallback";
e11 = e12;
}
e9 = (e10)(e11);
e4 = e9;
}
} }
e0 = e4;
}
e0;"#);
}

#[test]
fn test_0020_tagged_union_nonexhaustive() {
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
                                    "strnum",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "builder",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("union")),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_str",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_num",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")]),
                                                    ),
                                                ),
                                                mb.record(
                                                    vec![
                                                        ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                                        ("str", mb.scope_use("new_str")),
                                                        ("num", mb.scope_use("new_num")),
                                                    ],
                                                ),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.bind(
                                    "x",
                                    mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.)),
                                ),
                                mb.match_(
                                    mb.scope_use("x"),
                                    mb.str("y"),
                                    vec![
                                        (
                                            mb.str("numtag"),
                                            mb.call(
                                                mb.scope_use("log"),
                                                mb.call(
                                                    mb.field(mb.scope_use("y"), mb.str("to_string")),
                                                    mb.tuple(vec![]),
                                                ),
                                            ),
                                        ),
                                    ],
                                    None,
                                ),
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

#[test]
fn test_0030_tagged_union_exhaustive_with_fallback() {
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
                                    "strnum",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "builder",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("union")),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_str",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")]),
                                                    ),
                                                ),
                                                mb.bind(
                                                    "new_num",
                                                    mb.call(
                                                        mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                        mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")]),
                                                    ),
                                                ),
                                                mb.record(
                                                    vec![
                                                        ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                                        ("str", mb.scope_use("new_str")),
                                                        ("num", mb.scope_use("new_num")),
                                                    ],
                                                ),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.bind(
                                    "x",
                                    mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.)),
                                ),
                                mb.match_(
                                    mb.scope_use("x"),
                                    mb.str("y"),
                                    vec![
                                        (mb.str("strtag"), mb.call(mb.scope_use("log"), mb.scope_use("y"))),
                                        (
                                            mb.str("numtag"),
                                            mb.call(
                                                mb.scope_use("log"),
                                                mb.call(
                                                    mb.field(mb.scope_use("y"), mb.str("to_string")),
                                                    mb.tuple(vec![]),
                                                ),
                                            ),
                                        ),
                                    ],
                                    Some(mb.call(mb.scope_use("log"), mb.str("fallback"))),
                                ),
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
