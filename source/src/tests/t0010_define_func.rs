use {
    super::helpers::{ModuleBuilder, sanitize_builtins},
    crate::{
        compile,
        new_gx,
        eval::setup_v8,
        langhelp::SourceId,
        syntax::SerialModule,
    },
};

#[tokio::test]
async fn test_00600define_func() {
    //! Define a function that prints "hello", then call it.
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx.clone(),
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "hello",
                            mb.lower(
                                mb.block(
                                    vec![
                                        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                        mb.bind(
                                            "nfr",
                                            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("f"))
                                        ),
                                        mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                                        mb.call(
                                            mb.field(mb.scope_use("nfr"), mb.str("arg")),
                                            mb.tuple(vec![mb.scope_use("null")]),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("nfr"), mb.str("ret")),
                                            mb.tuple(vec![mb.scope_use("null")]),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                            mb.tuple(vec![mb.stage(mb.call(mb.scope_use("log"), mb.str("hello")))])
                                        ),
                                        mb.scope_use("f")
                                    ]
                                )
                            )
                        ),
                        mb.call(mb.scope_use("hello"), mb.tuple(vec![])),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "hello");
    let js_sources = sanitize_builtins(&gx.js_sources.lock().unwrap());
    assert_eq!(js_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a6;
let e3;
e3 = a7;
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
let e9;
{
let e10;
e10 = "f";
e9 = e10;
}
e7 = e8._0(e9);
a1 = e7;
}
let a2;
{
let e11;
let e12;
e12 = a1;
e11 = e12._0;
a2 = e11;
}
{
let e13;
let e14;
e14 = a1;
let e15;
e15 = a3;
e13 = e14._3(e15);
e4 = e13;
}
{
let e16;
let e17;
e17 = a1;
let e18;
e18 = a4;
e16 = e17._5(e18);
e4 = e16;
}
{
let e19;
let e20;
e20 = a1;
let e21;
e21 = a5;
e19 = e20._2(e21);
e4 = e19;
}
{
let e22;
e22 = a2;
e4 = e22;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
    assert_eq!(js_sources[1], r#"function d0_0_0(a0) {
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
}
let e4;
{
let e5;
let e6;
e6 = d0_0_0;
e5 = (e6)();
e4 = e5;
}
e4;"#);
}

#[tokio::test]
async fn test_00700define_func_arg_ret() {
    //! Define a function with string arg and string ret that uppercases the arg, call
    //! it with a string, and log the result.
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx.clone(),
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind("my_func", mb.lower(mb.block(vec![
                            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                            mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("f"))),
                            mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                            // Set arg type to string
                            mb.call(mb.field(mb.scope_use("nfr"), mb.str("arg")), mb.tuple(vec![mb.scope_use("string")]),),
                            // Set ret type to string
                            mb.call(mb.field(mb.scope_use("nfr"), mb.str("ret")), mb.tuple(vec![mb.scope_use("string")]),),
                            // Impl: call toUpperCase on arg and return
                            mb.call(
                                mb.field(mb.scope_use("nfr"), mb.str("impl")),
                                mb.tuple(
                                    vec![
                                        mb.stage(
                                            mb.call(mb.field(mb.scope_use("arg"), mb.str("toUpperCase")), mb.null(),),
                                        )
                                    ]
                                ),
                            ),
                            mb.scope_use("f"),
                        ]))),
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.scope_use("my_func"), mb.tuple(vec![mb.str("hello world")])),
                        ),
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "HELLO WORLD");
    let js_sources = sanitize_builtins(&gx.js_sources.lock().unwrap());
    assert_eq!(js_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a6;
let e3;
e3 = a7;
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
let e9;
{
let e10;
e10 = "f";
e9 = e10;
}
e7 = e8._0(e9);
a1 = e7;
}
let a2;
{
let e11;
let e12;
e12 = a1;
e11 = e12._0;
a2 = e11;
}
{
let e13;
let e14;
e14 = a1;
let e15;
e15 = a3;
e13 = e14._3(e15);
e4 = e13;
}
{
let e16;
let e17;
e17 = a1;
let e18;
e18 = a4;
e16 = e17._5(e18);
e4 = e16;
}
{
let e19;
let e20;
e20 = a1;
let e21;
e21 = a5;
e19 = e20._2(e21);
e4 = e19;
}
{
let e22;
e22 = a2;
e4 = e22;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
    assert_eq!(js_sources[1], r#"function d0_0_0(a0) {
let e0;
let e1;
e1 = a0;
let e2;
e2 = null;
e0 = e1.toUpperCase(e2);
return e0;
}
let e3;
{
let e4;
let e5;
e5 = BUILTIN;
let e6;
let e7;
e7 = d0_0_0;
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
