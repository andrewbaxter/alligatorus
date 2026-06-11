//! One of the first milestones of this project is to generate a `nodejs` target
//! within the language.
//!
//! These tests break this down into:
//!
//! * Chunks of the `nodejs` target AST
//!
//! * Tests to confirm the chunks work
use {
    crate::{
        compile,
        eval::setup_v8,
        syntax::{
            SourceExpr,
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

//. fn build_nodejs(mb: &ModuleBuilder) -> SourceExpr {
//.    return mb.block(vec![
//.        //. .
//.        mb.bind("code", build_nodejs_code(mb)),
//.        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
//.        // # Define the "nodejs" target, required by eval
//.        mb.bind("ntarget", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs target"))),
//.        mb.call(mb.field(mb.scope_use("ntarget"), mb.str("underlying")), mb.null()),
//.        mb.call(
//.            mb.field(mb.scope_use("ntarget"), mb.str("impl_interface")),
//.            mb.tuple(vec![mb.scope_use("target"), mb.record(vec![
//.                // TODO
//.                ])]),
//.        ),
//.        // # Define a generic nodejs value
//.        //
//.        // This will be used for objects and functions for the first examples
//.        mb.bind("nvalue_base", mb.call(mb.field(mb.scope_use("d"), mb.str("tagged_union")), mb.tuple(vec![]))),
//.        mb.bind("code_call", mb.call(mb.field(mb.scope_use("nvalue_base"), mb.str("variant")), mb.tuple(vec![
//.            //. .
//.            mb.str("binding"),
//.            mb.scope_use("string"),
//.        ]))),
//.        mb.bind("code_call", mb.call(mb.field(mb.scope_use("nvalue_base"), mb.str("variant")), mb.tuple(vec![
//.            //. .
//.            mb.str("expr"),
//.            mb.tuple(vec![]),
//.        ]))),
//.        mb.bind("nvalue", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs value"))),
//.        mb.call(mb.field(mb.scope_use("nvalue"), mb.str("underlying")), mb.record(vec![
//.            //. .
//.            ("base", mb.field(mb.scope_use("nvalue_base"), mb.str("type"))),
//.            ("type", mb.scope_use("type")),
//.        ])),
//.        // # Define a nodejs string type
//.        mb.bind("ntype_str", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs string"))),
//.        mb.call(mb.field(mb.scope_use("ntype_str"), mb.str("underlying")), mb.tuple(vec![])),
//.        // # Define a generic nodejs function type.
//.        //
//.        // This needs to implement AspectFunction at a minimum, and it should generate
//.        // "code" (from `build_nodejs_code`)
//.        mb.bind("ntype_func", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs function"))),
//.        mb.call(mb.field(mb.scope_use("ntype_func"), mb.str("underlying")), mb.record(vec![
//.            //. .
//.            ("arg", mb.scope_use("type")),
//.            ("ret", mb.scope_use("type")),
//.        ])),
//.        // # Define a generic nodejs object type
//.        //
//.        // This will need to implement `AspectField` and `AspectInitialBind` at a minimum,
//.        // and generate "code" (from `build_nodejs_code`).
//.        mb.bind("ntype_object", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs object"))),
//.        mb.call(
//.            mb.field(mb.scope_use("ntype_object"), mb.str("underlying")),
//.            mb.call(mb.scope_use("map"), mb.tuple(vec![mb.scope_use("string"), mb.scope_use("type")])),
//.        ),
//.        // # Define a "nodejs app" object to put it all together
//.        //
//.        // This object uses the target, types, and scope entries to turn staged code into
//.        // a functional nodejs program
//.        mb.bind("napp", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs object"))),
//.        mb.bind("root", mb.call(mb.scope_use("option"), mb.scope_use("expr"))),
//.        mb.call(
//.            mb.field(mb.scope_use("napp"), mb.str("underlying")),
//.            mb.record(vec![("root", mb.scope_use("root"))]),
//.        ),
//.        mb.bind("napp_root", mb.call(mb.field(mb.scope_use("napp"), mb.str("new_func")), mb.str("root"))),
//.        mb.call(mb.field(mb.scope_use("napp_root"), mb.str("arg")), mb.scope_use("expr")),
//.        mb.call(mb.field(mb.scope_use("napp_root"), mb.str("ret")), mb.scope_use("null")),
//.        mb.call(
//.            mb.field(mb.scope_use("napp_root"), mb.str("impl")),
//.            mb.stage(
//.                mb.set(
//.                    mb.field(mb.scope_use("self"), mb.str("root")),
//.                    mb.call(mb.lower(mb.field(mb.scope_use("root"), mb.str("some"))), mb.scope_use("arg")),
//.                ),
//.            ),
//.        ),
//.        mb.bind("napp_build", mb.call(mb.field(mb.scope_use("napp"), mb.str("new_func")), mb.str("build"))),
//.        mb.call(mb.field(mb.scope_use("napp_build"), mb.str("arg")), mb.tuple(vec![])),
//.        mb.call(mb.field(mb.scope_use("napp_build"), mb.str("ret")), mb.scope_use("string")),
//.        mb.call(mb.field(mb.scope_use("napp_build"), mb.str("impl")), mb.stage(mb.block(vec![
//.            //. .
//.            mb.match_(mb.field(mb.scope_use("self"), mb.str("root")), mb.str("root"), vec![
//.                //. .
//.                (mb.str("none"), mb.block(vec![mb.str("")])),
//.                (mb.str("some"), mb.block(vec![
//.                    //. .
//.                    mb.bind("res", mb.call(mb.scope_use("const_eval"), mb.tuple(vec![
//.                        //. .
//.                        mb.call(mb.scope_use("new_expr_context"), mb.record(vec![
//.                            //. .
//.                            ("target", mb.call(mb.lower(mb.field(mb.scope_use("ntarget"), mb.str("new"))), mb.null())),
//.                            ("scope", mb.record(vec![])),
//.                        ])),
//.                        mb.scope_use("root"),
//.                    ]))),
//.                    mb.call(
//.                        mb.lower(mb.field(mb.scope_use("code"), mb.str("render"))),
//.                        mb.call(
//.                            mb.field(
//.                                mb.field(mb.field(mb.scope_use("res"), mb.str("cv")), mb.str("code")),
//.                                mb.str("cast"),
//.                            ),
//.                            mb.lower(mb.field(mb.scope_use("code"), mb.str("type"))),
//.                        ),
//.                    ),
//.                ])),
//.            ], None),
//.        ]))),
//.        // # Build a nice constructor for the node app object
//.        mb.bind("nodejs", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("nodejs"))),
//.        mb.call(mb.field(mb.scope_use("nodejs"), mb.str("arg")), mb.tuple(vec![])),
//.        mb.call(
//.            mb.field(mb.scope_use("nodejs"), mb.str("ret")),
//.            mb.field(mb.scope_use("napp"), mb.str("type")),
//.        ),
//.        mb.call(
//.            mb.field(mb.scope_use("nodejs"), mb.str("impl")),
//.            mb.stage(
//.                mb.call(
//.                    mb.lower(mb.field(mb.scope_use("napp"), mb.str("new"))),
//.                    mb.record(
//.                        vec![
//.                            ("root", mb.call(mb.field(mb.scope_use("root"), mb.str("none")), mb.tuple(vec![]))),
//.                        ],
//.                    ),
//.                ),
//.            ),
//.        ),
//.        mb.field(mb.scope_use("nodejs"), mb.str("func")),
//.    ]);
//. }
fn build_nodejs_code(mb: &ModuleBuilder) -> SourceExpr {
    return mb.block(vec![
        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
        // # Define "code" tagged union type
        mb.bind("code_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("tagged_union")), mb.tuple(vec![]))),
        mb.bind("code", mb.field(mb.scope_use("code_builder"), mb.str("type"))),
        mb.bind("code_call", mb.call(mb.field(mb.scope_use("code_builder"), mb.str("variant")), mb.tuple(vec![
            //. .
            mb.str("call"),
            mb.record(vec![("func", mb.scope_use("code")), ("arg", mb.scope_use("code"))]),
        ]))),
        mb.bind(
            "code_lit_str",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("lit_str"), mb.scope_use("string")]),
            ),
        ),
        mb.bind("code_field", mb.call(mb.field(mb.scope_use("code_builder"), mb.str("variant")), mb.tuple(vec![
            //. .
            mb.str("field"),
            mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("code"))]),
        ]))),
        mb.bind(
            "code_scope_use",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("scope_use"), mb.scope_use("string")]),
            ),
        ),
        // # Define render-code function (recursive inner version)
        mb.bind(
            "render_inner_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("Render Node.js code (inner)")),
        ),
        mb.bind("render_inner", mb.field(mb.scope_use("render_inner_builder"), mb.str("func"))),
        mb.call(mb.field(mb.scope_use("render_inner_builder"), mb.str("arg")), mb.record(vec![
            //. .
            ("context", mb.record(vec![
                //. .
                ("expr_count", mb.scope_use("number")),
                ("out", mb.call(mb.scope_use("list"), mb.scope_use("string"))),
            ])),
            ("code", mb.scope_use("code")),
        ])),
        mb.call(mb.field(mb.scope_use("render_inner_builder"), mb.str("ret")), mb.scope_use("string")),
        mb.call(mb.field(mb.scope_use("render_inner_builder"), mb.str("impl")), mb.stage(mb.block(vec![
            //. .
            mb.bind(
                "key",
                mb.call(
                    mb.field(
                        mb.call(
                            mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                            mb.tuple(vec![
                                //. .
                                mb.str("e"),
                                mb.call(
                                    mb.field(
                                        mb.field(
                                            mb.field(mb.scope_use("arg"), mb.str("context")),
                                            mb.str("expr_count"),
                                        ),
                                        mb.str("to_string"),
                                    ),
                                    mb.tuple(vec![]),
                                ),
                            ]),
                        ),
                        mb.str("join"),
                    ),
                    mb.str(""),
                ),
            ),
            mb.set(
                mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("expr_count")),
                mb.operator_add(
                    mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("expr_count")),
                    mb.num(1.),
                ),
            ),
            mb.bind("out", mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("out"))),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str("let ")),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.scope_use("key")),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n{\n")),
            mb.bind(
                "value",
                mb.call(
                    mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                    mb.tuple(vec![mb.scope_use("key"), mb.str(" = ")]),
                ),
            ),
            mb.match_(mb.field(mb.scope_use("arg"), mb.str("code")), mb.str("code"), vec![
                //. .
                (mb.str("call"), mb.block(vec![
                    //. .
                    mb.bind("func", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("func"))),
                    ]))),
                    mb.bind("rendered_arg", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("arg"))),
                    ]))),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str("(")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("func")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(")(")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("rendered_arg")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(")")),
                ])),
                (mb.str("lit_str"), mb.block(vec![
                    //. .
                    mb.call(
                        mb.field(mb.scope_use("value"), mb.str("push")),
                        mb.call(mb.scope_use("str_to_json"), mb.scope_use("code")),
                    ),
                ])),
                (mb.str("field"), mb.block(vec![
                    //. .
                    mb.bind("base", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("base"))),
                    ]))),
                    mb.bind("field", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("field"))),
                    ]))),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("base")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(".")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("field")),
                ])),
                (
                    mb.str("scope_use"),
                    mb.block(
                        vec![mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("code"))],
                    ),
                ),
            ], None),
            mb.call(
                mb.field(mb.scope_use("out"), mb.str("push")),
                mb.call(mb.field(mb.scope_use("value"), mb.str("join")), mb.str("")),
            ),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n}\n")),
            mb.scope_use("key"),
        ]))),
        // Define render-code func (outer)
        mb.bind(
            "render_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("Render Node.js code")),
        ),
        mb.bind("render", mb.field(mb.scope_use("render_builder"), mb.str("func"))),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("arg")), mb.scope_use("code")),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("ret")), mb.scope_use("string")),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("impl")), mb.stage(mb.block(vec![
            //. .
            mb.bind("context", mb.record(vec![
                //. .
                (
                    "out",
                    mb.call(
                        mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                        mb.tuple(vec![]),
                    ),
                ),
                ("expr_count", mb.as_(mb.num(0.), mb.scope_use("number"))),
            ])),
            mb.call(
                mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("push")),
                mb.call(
                    mb.lower(mb.scope_use("render_inner")),
                    mb.record(vec![("context", mb.scope_use("context")), ("code", mb.scope_use("arg"))]),
                ),
            ),
            mb.call(mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("join")), mb.str("")),
        ]))),
        // Assemble and return
        mb.record(vec![
            //. .
            ("type", mb.scope_use("code")),
            ("call", mb.scope_use("code_call")),
            ("lit_str", mb.scope_use("code_lit_str")),
            ("field", mb.scope_use("code_field")),
            ("scope_use", mb.scope_use("code_scope_use")),
            ("render", mb.scope_use("render")),
        ]),
    ]);
}

#[test]
fn test_0000_nodejs_code() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let code = |key: &str, arg: SourceExpr| {
        return mb.call(mb.field(mb.scope_use("code"), mb.str(key)), arg);
    };
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.block(
                            vec![
                                mb.bind("code", mb.lower(build_nodejs_code(&mb))),
                                mb.call(
                                    mb.scope_use("log"),
                                    code(
                                        "render",
                                        code(
                                            "call",
                                            mb.record(
                                                vec![
                                                    (
                                                        "func",
                                                        code(
                                                            "field",
                                                            mb.record(
                                                                vec![
                                                                    ("base", code("scope_use", mb.str("console"))),
                                                                    ("field", code("lit_str", mb.str("log"))),
                                                                ],
                                                            ),
                                                        ),
                                                    ),
                                                    ("arg", code("lit_str", mb.str("Hello world (node)"))),
                                                ],
                                            ),
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
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, r#"let e0;
{
let e1;
{
let e2;
{
e2 = console;
}
let e3;
{
e3 = "log";
}
e1 = e2.e3;
}
let e4;
{
e4 = "Hello world (node)";
}
e0 = (e1)(e4);
}
e0"#);
    let js_sources = sanitize_builtins(&res.generated_mortar_scripts);
    assert_eq!(js_sources.len(), 2);
    assert_eq!(
        js_sources[0],
        r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a24;
let e3;
e3 = a25;
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
e7 = e8["_3"]();
a1 = e7;
}
let a2;
{
let e9;
let e10;
e10 = a1;
e9 = e10["_0"];
a2 = e9;
}
let a3;
{
let e11;
let e12;
e12 = a1;
let e13;
{
let e14;
e14 = "call";
e13 = e14;
}
let e15;
let e16;
e16 = BUILTIN;
let e17;
let e18;
e18 = a2;
let e19;
e19 = a2;
e17 = { "\"arg\"": e18, "\"func\"": e19 };
e15 = (e16)(e17);
e11 = e12["_1"](e13,e15);
a3 = e11;
}
let a5;
{
let e20;
let e21;
e21 = a1;
let e22;
{
let e23;
e23 = "lit_str";
e22 = e23;
}
let e24;
e24 = a4;
e20 = e21["_1"](e22,e24);
a5 = e20;
}
let a6;
{
let e25;
let e26;
e26 = a1;
let e27;
{
let e28;
e28 = "field";
e27 = e28;
}
let e29;
let e30;
e30 = BUILTIN;
let e31;
let e32;
e32 = a2;
let e33;
e33 = a2;
e31 = { "\"base\"": e32, "\"field\"": e33 };
e29 = (e30)(e31);
e25 = e26["_1"](e27,e29);
a6 = e25;
}
let a8;
{
let e34;
let e35;
e35 = a1;
let e36;
{
let e37;
e37 = "scope_use";
e36 = e37;
}
let e38;
e38 = a7;
e34 = e35["_1"](e36,e38);
a8 = e34;
}
let a9;
{
let e39;
let e40;
e40 = a0;
let e41;
{
let e42;
e42 = "Render Node.js code (inner)";
e41 = e42;
}
e39 = e40["_0"](e41);
a9 = e39;
}
let a10;
{
let e43;
let e44;
e44 = a9;
e43 = e44["_0"];
a10 = e43;
}
{
let e45;
let e46;
e46 = a9;
let e47;
let e48;
e48 = BUILTIN;
let e49;
let e50;
e50 = a2;
let e51;
e51 = a11;
e49 = { "\"code\"": e50, "\"context\"": e51 };
e47 = (e48)(e49);
e45 = e46["_3"](e47);
e4 = e45;
}
{
let e52;
let e53;
e53 = a9;
let e54;
e54 = a12;
e52 = e53["_5"](e54);
e4 = e52;
}
{
let e55;
let e56;
e56 = a9;
let e57;
let e58;
e58 = BUILTIN;
let e59;
e59 = a13;
let e60;
e60 = a14;
let e61;
e61 = a10;
let e62;
e62 = a15;
let e63;
e63 = a10;
let e64;
e64 = a16;
let e65;
e65 = a10;
let e66;
e66 = a17;
let e67;
e67 = a10;
e57 = (e58)(e59,e60,e61,e62,e63,e64,e65,e66,e67);
e55 = e56["_2"](e57);
e4 = e55;
}
let a18;
{
let e68;
let e69;
e69 = a0;
let e70;
{
let e71;
e71 = "Render Node.js code";
e70 = e71;
}
e68 = e69["_0"](e70);
a18 = e68;
}
let a19;
{
let e72;
let e73;
e73 = a18;
e72 = e73["_0"];
a19 = e72;
}
{
let e74;
let e75;
e75 = a18;
let e76;
e76 = a20;
e74 = e75["_3"](e76);
e4 = e74;
}
{
let e77;
let e78;
e78 = a18;
let e79;
e79 = a21;
e77 = e78["_5"](e79);
e4 = e77;
}
{
let e80;
let e81;
e81 = a18;
let e82;
let e83;
e83 = BUILTIN;
let e84;
e84 = a22;
let e85;
e85 = a23;
let e86;
e86 = a10;
e82 = (e83)(e84,e85,e86);
e80 = e81["_2"](e82);
e4 = e80;
}
{
let e87;
let e88;
e88 = a3;
let e89;
e89 = a6;
let e90;
e90 = a5;
let e91;
e91 = a19;
let e92;
e92 = a8;
let e93;
e93 = a2;
e87 = { "\"call\"": e88, "\"field\"": e89, "\"lit_str\"": e90, "\"render\"": e91, "\"scope_use\"": e92, "\"type\"": e93 };
e4 = e87;
}
e0 = (e1)(e2,e3,e4);
e0;"#
    );
    assert_eq!(js_sources[1], r#"function d0_0_0(a0) {
let e0;
let a1;
{
let e1;
let e2;
let e3;
{
let e4;
e4 = "e";
e3 = e4;
}
let e5;
let e6;
let e7;
let e8;
e8 = a0;
e7 = e8["\"context\""];
e6 = e7["\"expr_count\""];
e5 = e6["toString"]();
e2 = [e3, e5];
let e9;
{
let e10;
e10 = "";
e9 = e10;
}
e1 = e2["join"](e9);
a1 = e1;
}
{
let e11;
let e12;
let e13;
e13 = a0;
e12 = e13["\"context\""];
let e14;
let e15;
let e16;
let e17;
e17 = a0;
e16 = e17["\"context\""];
e15 = e16["\"expr_count\""];
let e18;
{
let e19;
e19 = 1.0;
e18 = e19;
}
e14 = (e15 + e18);
e12["\"expr_count\""] = e14;
e11 = null;
e0 = e11;
}
let a2;
{
let e20;
let e21;
let e22;
e22 = a0;
e21 = e22["\"context\""];
e20 = e21["\"out\""];
a2 = e20;
}
{
let e23;
let e24;
e24 = a2;
let e25;
{
let e26;
e26 = "let ";
e25 = e26;
}
e23 = e24["push"](e25);
e0 = e23;
}
{
let e27;
let e28;
e28 = a2;
let e29;
e29 = a1;
e27 = e28["push"](e29);
e0 = e27;
}
{
let e30;
let e31;
e31 = a2;
let e32;
{
let e33;
e33 = ";\n{\n";
e32 = e33;
}
e30 = e31["push"](e32);
e0 = e30;
}
let a3;
{
let e34;
let e35;
e35 = a1;
let e36;
{
let e37;
e37 = " = ";
e36 = e37;
}
e34 = [e35, e36];
a3 = e34;
}
{
let e38;
let e39;
let e40;
e40 = a0;
e39 = e40["\"code\""];
{ let [__tag, a4] = Object.entries(e39)[0];
switch (__tag) {
case "t0": {
let e41;
let a5;
{
let e42;
let e43;
e43 = d0_0_0;
let e44;
let e45;
let e46;
e46 = a4;
e45 = e46["\"func\""];
let e47;
let e48;
e48 = a0;
e47 = e48["\"context\""];
e44 = { "\"code\"": e45, "\"context\"": e47 };
e42 = (e43)(e44);
a5 = e42;
}
let a6;
{
let e49;
let e50;
e50 = d0_0_0;
let e51;
let e52;
let e53;
e53 = a4;
e52 = e53["\"arg\""];
let e54;
let e55;
e55 = a0;
e54 = e55["\"context\""];
e51 = { "\"code\"": e52, "\"context\"": e54 };
e49 = (e50)(e51);
a6 = e49;
}
{
let e56;
let e57;
e57 = a3;
let e58;
{
let e59;
e59 = "(";
e58 = e59;
}
e56 = e57["push"](e58);
e41 = e56;
}
{
let e60;
let e61;
e61 = a3;
let e62;
e62 = a5;
e60 = e61["push"](e62);
e41 = e60;
}
{
let e63;
let e64;
e64 = a3;
let e65;
{
let e66;
e66 = ")(";
e65 = e66;
}
e63 = e64["push"](e65);
e41 = e63;
}
{
let e67;
let e68;
e68 = a3;
let e69;
e69 = a6;
e67 = e68["push"](e69);
e41 = e67;
}
{
let e70;
let e71;
e71 = a3;
let e72;
{
let e73;
e73 = ")";
e72 = e73;
}
e70 = e71["push"](e72);
e41 = e70;
}
e38 = e41;
break; }
case "t1": {
let e74;
{
let e75;
let e76;
e76 = a3;
let e77;
let e78;
e78 = BUILTIN;
let e79;
e79 = a4;
e77 = (e78)(e79);
e75 = e76["push"](e77);
e74 = e75;
}
e38 = e74;
break; }
case "t2": {
let e80;
let a7;
{
let e81;
let e82;
e82 = d0_0_0;
let e83;
let e84;
let e85;
e85 = a4;
e84 = e85["\"base\""];
let e86;
let e87;
e87 = a0;
e86 = e87["\"context\""];
e83 = { "\"code\"": e84, "\"context\"": e86 };
e81 = (e82)(e83);
a7 = e81;
}
let a8;
{
let e88;
let e89;
e89 = d0_0_0;
let e90;
let e91;
let e92;
e92 = a4;
e91 = e92["\"field\""];
let e93;
let e94;
e94 = a0;
e93 = e94["\"context\""];
e90 = { "\"code\"": e91, "\"context\"": e93 };
e88 = (e89)(e90);
a8 = e88;
}
{
let e95;
let e96;
e96 = a3;
let e97;
e97 = a7;
e95 = e96["push"](e97);
e80 = e95;
}
{
let e98;
let e99;
e99 = a3;
let e100;
{
let e101;
e101 = ".";
e100 = e101;
}
e98 = e99["push"](e100);
e80 = e98;
}
{
let e102;
let e103;
e103 = a3;
let e104;
e104 = a8;
e102 = e103["push"](e104);
e80 = e102;
}
e38 = e80;
break; }
case "t3": {
let e105;
{
let e106;
let e107;
e107 = a3;
let e108;
e108 = a4;
e106 = e107["push"](e108);
e105 = e106;
}
e38 = e105;
break; }
} }
e0 = e38;
}
{
let e109;
let e110;
e110 = a2;
let e111;
let e112;
e112 = a3;
let e113;
{
let e114;
e114 = "";
e113 = e114;
}
e111 = e112["join"](e113);
e109 = e110["push"](e111);
e0 = e109;
}
{
let e115;
let e116;
e116 = a2;
let e117;
{
let e118;
e118 = ";\n}\n";
e117 = e118;
}
e115 = e116["push"](e117);
e0 = e115;
}
{
let e119;
e119 = a1;
e0 = e119;
}
return e0;
}
function d0_0_1(a0) {
let e120;
let a1;
{
let e121;
let e122;
{
let e123;
e123 = 0.0;
e122 = e123;
}
let e124;
e124 = [];
e121 = { "\"expr_count\"": e122, "\"out\"": e124 };
a1 = e121;
}
{
let e125;
let e126;
let e127;
e127 = a1;
e126 = e127["\"out\""];
let e128;
let e129;
e129 = d0_0_0;
let e130;
let e131;
e131 = a0;
let e132;
e132 = a1;
e130 = { "\"code\"": e131, "\"context\"": e132 };
e128 = (e129)(e130);
e125 = e126["push"](e128);
e120 = e125;
}
{
let e133;
let e134;
let e135;
e135 = a1;
e134 = e135["\"out\""];
let e136;
{
let e137;
e137 = "";
e136 = e137;
}
e133 = e134["join"](e136);
e120 = e133;
}
return e120;
}
let e138;
{
let e139;
let e140;
e140 = BUILTIN;
let e141;
let e142;
e142 = d0_0_1;
let e143;
let e144;
let e145;
let e146;
{
let e147;
e147 = "Hello world (node)";
e146 = e147;
}
e145 = { "t1": e146 };
let e148;
let e149;
let e150;
let e151;
{
let e152;
e152 = "console";
e151 = e152;
}
e150 = { "t3": e151 };
let e153;
let e154;
{
let e155;
e155 = "log";
e154 = e155;
}
e153 = { "t1": e154 };
e149 = { "\"base\"": e150, "\"field\"": e153 };
e148 = { "t2": e149 };
e144 = { "\"arg\"": e145, "\"func\"": e148 };
e143 = { "t0": e144 };
e141 = (e142)(e143);
e139 = (e140)(e141);
e138 = e139;
}
e138;"#);
}
//. #[test]
//. fn test_0000_nodejs_helloworld() {
//.    setup_v8();
//.    let gx = new_gx([
//.        //. .
//.        ("nodejs", SourceModule::V1({
//.            let mb = ModuleBuilder::default();
//.            build_nodejs(&mb)
//.        })),
//.        ("main", SourceModule::V1({
//.            let mb = ModuleBuilder::default();
//.            mb.block(vec![
//.                //. .
//.                mb.bind("nodejs", mb.import(mb.str("nodejs"))),
//.                mb.bind("app", mb.call(mb.scope_use("nodejs"), mb.tuple(vec![]))),
//.                mb.call(
//.                    mb.field(mb.scope_use("app"), mb.str("root")),
//.                    mb.stage(
//.                        mb.call(mb.field(mb.scope_use("console"), mb.str("log")), mb.str("Hello world (node)")),
//.                    ),
//.                ),
//.                mb.call(
//.                    mb.scope_use("log"),
//.                    mb.call(mb.field(mb.scope_use("app"), mb.str("build")), mb.tuple(vec![])),
//.                ),
//.            ])
//.        })),
//.    ].into_iter().collect());
//.    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
//.    assert_eq!(res.logs.len(), 1);
//.    assert_eq!(res.logs[0].message, r#"let e0;
//. {
//. let e1;
//. {
//. let e2;
//. {
//. e2 = console;
//. }
//. let e3;
//. {
//. e3 = "log";
//. }
//. e1 = e2.e3;
//. }
//. let e4;
//. {
//. e4 = "Hello world (node)";
//. }
//. e0 = (e1)(e4);
//. }
//. e0"#);
//.    let js_sources = sanitize_builtins(&res.generated_mortar_scripts);
//.    assert_eq!(js_sources.len(), 2);
//.    assert_eq!(js_sources[0], r#"TODO"#);
//.    assert_eq!(js_sources[1], r#"TODO"#);
//. }
