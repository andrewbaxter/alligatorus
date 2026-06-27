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
fn test_00600define_func() {
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
                                    "hello",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "nfr",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("func")),
                                                        mb.str("f"),
                                                    ),
                                                ),
                                                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_null(),
                                                            mb.types_null(),
                                                            mb.stage(
                                                                mb.call(mb.log(), mb.str("hello")),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.scope_use("f"),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.call(mb.scope_use("hello"), mb.tuple(vec![])),
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
    assert_eq!(res.logs[0].message, "hello");
    let dynamic_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(dynamic_sources[0], r#"let e0;
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
let e10;
{
let e11;
e11 = "f";
e10 = e11;
}
e8 = e9["_0"](e10);
a1 = e8;
}
let a2;
{
let e12;
let e13;
e13 = a1;
e12 = e13["_0"];
a2 = e12;
}
{
let e14;
let e15;
e15 = a1;
let e16;
e16 = a3;
let e17;
e17 = a4;
let e18;
e18 = a5;
e14 = e15["_2"](e16,e17,e18);
e5 = e14;
}
{
let e19;
e19 = a2;
e5 = e19;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(dynamic_sources[1], r#"var d0_0_0_0;
{
d0_0_0_0 = function(a0) {
let e0;
let e1;
e1 = BUILTIN;
let e2;
{
let e3;
e3 = "hello";
e2 = e3;
}
e0 = (e1)(e2);
return e0;
};
}
let e4;
{
let e5;
let e6;
e6 = d0_0_0_0;
e5 = (e6)();
e4 = e5;
}
e4;"#);
}

#[test]
fn test_00700define_func_arg_ret() {
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
                                    "my_func",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "nfr",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("func")),
                                                        mb.str("f"),
                                                    ),
                                                ),
                                                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_string(),
                                                            mb.types_string(),
                                                            mb.stage(
                                                                mb.call(
                                                                    mb.field(
                                                                        mb.scope_use("arg"),
                                                                        mb.str("toUpperCase"),
                                                                    ),
                                                                    mb.null(),
                                                                ),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.scope_use("f"),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.call(
                                    mb.log(),
                                    mb.call(mb.scope_use("my_func"), mb.tuple(vec![mb.str("hello world")])),
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
    assert_eq!(res.logs[0].message, "HELLO WORLD");
    let dynamic_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(dynamic_sources[0], r#"let e0;
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
let e10;
{
let e11;
e11 = "f";
e10 = e11;
}
e8 = e9["_0"](e10);
a1 = e8;
}
let a2;
{
let e12;
let e13;
e13 = a1;
e12 = e13["_0"];
a2 = e12;
}
{
let e14;
let e15;
e15 = a1;
let e16;
e16 = a3;
let e17;
e17 = a4;
let e18;
e18 = a5;
e14 = e15["_2"](e16,e17,e18);
e5 = e14;
}
{
let e19;
e19 = a2;
e5 = e19;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#);
    assert_eq!(dynamic_sources[1], r#"var d0_0_0_0;
{
d0_0_0_0 = function(a0) {
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
{
let e4;
let e5;
e5 = BUILTIN;
let e6;
let e7;
e7 = d0_0_0_0;
let e8;
{
let e9;
e9 = "hello world";
e8 = e9;
}
e6 = (e7)(e8);
e4 = (e5)(e6);
e3 = e4;
}
e3;"#);
}

#[test]
fn test_00710define_func_unified_impl() {
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
                                    "my_func",
                                    mb.lower(
                                        mb.seq(
                                            vec![
                                                mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
                                                mb.bind(
                                                    "nfr",
                                                    mb.call(
                                                        mb.field(mb.scope_use("d"), mb.str("func")),
                                                        mb.str("f"),
                                                    ),
                                                ),
                                                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                                mb.call(
                                                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                                    mb.tuple(
                                                        vec![
                                                            mb.types_string(),
                                                            mb.types_string(),
                                                            mb.stage(
                                                                mb.call(
                                                                    mb.field(
                                                                        mb.scope_use("arg"),
                                                                        mb.str("toUpperCase"),
                                                                    ),
                                                                    mb.null(),
                                                                ),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.scope_use("f"),
                                            ],
                                        ),
                                    ),
                                ),
                                mb.call(
                                    mb.log(),
                                    mb.call(mb.scope_use("my_func"), mb.tuple(vec![mb.str("hello world")])),
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
    assert_eq!(res.logs[0].message, "HELLO WORLD");
}
