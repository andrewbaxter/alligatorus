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

fn build_nodejs_code(mb: &ModuleBuilder) -> SourceExpr {
    return mb.lower(mb.block(vec![
        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
        // # Define "code" tagged union type
        mb.bind("code_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("tagged_union")), mb.tuple(vec![]))),
        mb.bind("code", mb.field(mb.scope_use("code_builder"), mb.str("type"))),
        mb.bind(
            "code_call",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(
                    vec![
                        mb.str("call"),
                        mb.record(vec![("func", mb.scope_use("code")), ("arg", mb.scope_use("code"))])
                    ]
                )
            )
        ),
        mb.bind(
            "code_lit_str",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("lit_str"), mb.scope_use("string")])
            )
        ),
        mb.bind(
            "code_field",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(
                    vec![
                        mb.str("field"),
                        mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("code"))])
                    ]
                )
            )
        ),
        mb.bind(
            "code_scope_use",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("scope_use"), mb.scope_use("string")])
            )
        ),
        // # Define render-code function (recursive inner version)
        mb.bind(
            "render_inner_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("Render Node.js code (inner)"))
        ),
        mb.bind("render_inner", mb.field(mb.scope_use("render_inner_builder"), mb.str("func"))),
        mb.call(
            mb.field(mb.scope_use("render_inner_builder"), mb.str("arg")),
            mb.record(
                vec![
                    (
                        "context",
                        mb.record(
                            vec![
                                ("expr_count", mb.scope_use("number")),
                                ("out", mb.call(mb.scope_use("list"), mb.scope_use("string")))
                            ]
                        ),
                    ),
                    ("code", mb.scope_use("code"))
                ]
            )
        ),
        mb.call(mb.field(mb.scope_use("render_inner_builder"), mb.str("ret")), mb.scope_use("string")),
        mb.call(
            mb.field(mb.scope_use("render_inner_builder"), mb.str("impl")),
            mb.stage(
                mb.block(
                    vec![
                        mb.bind(
                            "key",
                            mb.call(
                                mb.field(
                                    mb.call(
                                        mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                                        mb.tuple(
                                            vec![
                                                mb.str("e"),
                                                mb.call(
                                                    mb.field(
                                                        mb.field(
                                                            mb.field(mb.scope_use("arg"), mb.str("context")),
                                                            mb.str("expr_count")
                                                        ),
                                                        mb.str("to_string")
                                                    ),
                                                    mb.tuple(vec![])
                                                )
                                            ]
                                        )
                                    ),
                                    mb.str("join")
                                ),
                                mb.str("")
                            )
                        ),
                        mb.set(
                            mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("expr_count")),
                            mb.operator_add(
                                mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("expr_count")),
                                mb.num(1.)
                            )
                        ),
                        mb.bind("out", mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("out"))),
                        mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str("let ")),
                        mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.scope_use("key")),
                        mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n{\n")),
                        mb.bind(
                            "value",
                            mb.call(
                                mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                                mb.tuple(vec![mb.scope_use("key"), mb.str(" = "),])
                            )
                        ),
                        mb.match_(
                            mb.field(mb.scope_use("arg"), mb.str("code")),
                            mb.str("code"),
                            vec![
                                (
                                    mb.str("call"),
                                    mb.block(
                                        vec![
                                            mb.bind(
                                                "func",
                                                mb.call(
                                                    mb.lower(mb.scope_use("render_inner")),
                                                    mb.record(
                                                        vec![
                                                            (
                                                                "context",
                                                                mb.field(mb.scope_use("arg"), mb.str("context")),
                                                            ),
                                                            ("code", mb.field(mb.scope_use("code"), mb.str("func")))
                                                        ]
                                                    )
                                                )
                                            ),
                                            mb.bind(
                                                "rendered_arg",
                                                mb.call(
                                                    mb.lower(mb.scope_use("render_inner")),
                                                    mb.record(
                                                        vec![
                                                            (
                                                                "context",
                                                                mb.field(mb.scope_use("arg"), mb.str("context")),
                                                            ),
                                                            ("code", mb.field(mb.scope_use("code"), mb.str("arg")))
                                                        ]
                                                    )
                                                )
                                            ),
                                            mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str("(")),
                                            mb.call(
                                                mb.field(mb.scope_use("value"), mb.str("push")),
                                                mb.scope_use("func")
                                            ),
                                            mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(")(")),
                                            mb.call(
                                                mb.field(mb.scope_use("value"), mb.str("push")),
                                                mb.scope_use("rendered_arg")
                                            ),
                                            mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(")")),
                                        ]
                                    ),
                                ),
                                (
                                    mb.str("lit_str"),
                                    mb.block(
                                        vec![
                                            mb.call(
                                                mb.field(mb.scope_use("value"), mb.str("push")),
                                                mb.call(mb.scope_use("str_to_json"), mb.scope_use("code"))
                                            )
                                        ]
                                    ),
                                ),
                                (
                                    mb.str("field"),
                                    mb.block(
                                        vec![
                                            mb.bind(
                                                "base",
                                                mb.call(
                                                    mb.lower(mb.scope_use("render_inner")),
                                                    mb.record(
                                                        vec![
                                                            (
                                                                "context",
                                                                mb.field(mb.scope_use("arg"), mb.str("context")),
                                                            ),
                                                            ("code", mb.field(mb.scope_use("code"), mb.str("base")))
                                                        ]
                                                    )
                                                )
                                            ),
                                            mb.bind(
                                                "field",
                                                mb.call(
                                                    mb.lower(mb.scope_use("render_inner")),
                                                    mb.record(
                                                        vec![
                                                            (
                                                                "context",
                                                                mb.field(mb.scope_use("arg"), mb.str("context")),
                                                            ),
                                                            ("code", mb.field(mb.scope_use("code"), mb.str("field")))
                                                        ]
                                                    )
                                                )
                                            ),
                                            mb.call(
                                                mb.field(mb.scope_use("value"), mb.str("push")),
                                                mb.scope_use("base")
                                            ),
                                            mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(".")),
                                            mb.call(
                                                mb.field(mb.scope_use("value"), mb.str("push")),
                                                mb.scope_use("field")
                                            ),
                                        ]
                                    ),
                                ),
                                (
                                    mb.str("scope_use"),
                                    mb.block(
                                        vec![
                                            mb.call(
                                                mb.field(mb.scope_use("value"), mb.str("push")),
                                                mb.scope_use("code")
                                            ),
                                        ]
                                    ),
                                ),
                            ],
                            None
                        ),
                        mb.call(
                            mb.field(mb.scope_use("out"), mb.str("push")),
                            mb.call(mb.field(mb.scope_use("value"), mb.str("join")), mb.str(""))
                        ),
                        mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n}\n")),
                        mb.scope_use("key")
                    ]
                )
            )
        ),
        // Define render-code func (outer)
        mb.bind("render_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("Render Node.js code"))),
        mb.bind("render", mb.field(mb.scope_use("render_builder"), mb.str("func"))),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("arg")), mb.scope_use("code")),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("ret")), mb.scope_use("string")),
        mb.call(
            mb.field(mb.scope_use("render_builder"), mb.str("impl")),
            mb.stage(
                mb.block(
                    vec![
                        mb.bind(
                            "context",
                            mb.record(
                                vec![
                                    (
                                        "out",
                                        mb.call(
                                            mb.field(
                                                mb.call(mb.scope_use("list"), mb.scope_use("string")),
                                                mb.str("new")
                                            ),
                                            mb.tuple(vec![])
                                        ),
                                    ),
                                    ("expr_count", mb.num(0.)),
                                ]
                            )
                        ),
                        mb.call(
                            mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("push")),
                            mb.call(
                                mb.lower(mb.scope_use("render_inner")),
                                mb.record(vec![("context", mb.scope_use("context")), ("code", mb.scope_use("arg"))])
                            )
                        ),
                        mb.call(mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("join")), mb.str(""))
                    ]
                )
            )
        ),
        // Assemble and return
        mb.record(
            vec![
                ("type", mb.scope_use("code")),
                ("call", mb.scope_use("code_call")),
                ("lit_str", mb.scope_use("code_lit_str")),
                ("field", mb.scope_use("code_field")),
                ("scope_use", mb.scope_use("code_scope_use")),
                ("render", mb.scope_use("render"))
            ]
        )
    ]));
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
                                mb.bind("code", build_nodejs_code(&mb)),
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
                                                                    ("field", code("lit_str", mb.str("log")))
                                                                ]
                                                            )
                                                        ),
                                                    ),
                                                    ("arg", code("lit_str", mb.str("Hello world (node)")))
                                                ]
                                            )
                                        )
                                    )
                                )
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
    assert_eq!(js_sources[0], r#"let e0;
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
e7 = e8._3();
a1 = e7;
}
let a2;
{
let e9;
let e10;
e10 = a1;
e9 = e10._0;
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
{
let e18;
let e19;
e19 = a2;
let e20;
e20 = a2;
e18 = { arg: e19, func: e20 };
e17 = e18;
}
e15 = (e16)(e17);
e11 = e12._1(e13,e15);
a3 = e11;
}
let a5;
{
let e21;
let e22;
e22 = a1;
let e23;
{
let e24;
e24 = "lit_str";
e23 = e24;
}
let e25;
e25 = a4;
e21 = e22._1(e23,e25);
a5 = e21;
}
let a6;
{
let e26;
let e27;
e27 = a1;
let e28;
{
let e29;
e29 = "field";
e28 = e29;
}
let e30;
let e31;
e31 = BUILTIN;
let e32;
{
let e33;
let e34;
e34 = a2;
let e35;
e35 = a2;
e33 = { base: e34, field: e35 };
e32 = e33;
}
e30 = (e31)(e32);
e26 = e27._1(e28,e30);
a6 = e26;
}
let a8;
{
let e36;
let e37;
e37 = a1;
let e38;
{
let e39;
e39 = "scope_use";
e38 = e39;
}
let e40;
e40 = a7;
e36 = e37._1(e38,e40);
a8 = e36;
}
let a9;
{
let e41;
let e42;
e42 = a0;
let e43;
{
let e44;
e44 = "Render Node.js code (inner)";
e43 = e44;
}
e41 = e42._0(e43);
a9 = e41;
}
let a10;
{
let e45;
let e46;
e46 = a9;
e45 = e46._0;
a10 = e45;
}
{
let e47;
let e48;
e48 = a9;
let e49;
let e50;
e50 = BUILTIN;
let e51;
{
let e52;
let e53;
e53 = a2;
let e54;
e54 = a11;
e52 = { code: e53, context: e54 };
e51 = e52;
}
e49 = (e50)(e51);
e47 = e48._3(e49);
e4 = e47;
}
{
let e55;
let e56;
e56 = a9;
let e57;
e57 = a12;
e55 = e56._5(e57);
e4 = e55;
}
{
let e58;
let e59;
e59 = a9;
let e60;
let e61;
e61 = BUILTIN;
let e62;
e62 = a13;
let e63;
e63 = a14;
let e64;
e64 = a10;
let e65;
e65 = a15;
let e66;
e66 = a10;
let e67;
e67 = a16;
let e68;
e68 = a10;
let e69;
e69 = a17;
let e70;
e70 = a10;
e60 = (e61)(e62,e63,e64,e65,e66,e67,e68,e69,e70);
e58 = e59._2(e60);
e4 = e58;
}
let a18;
{
let e71;
let e72;
e72 = a0;
let e73;
{
let e74;
e74 = "Render Node.js code";
e73 = e74;
}
e71 = e72._0(e73);
a18 = e71;
}
let a19;
{
let e75;
let e76;
e76 = a18;
e75 = e76._0;
a19 = e75;
}
{
let e77;
let e78;
e78 = a18;
let e79;
e79 = a20;
e77 = e78._3(e79);
e4 = e77;
}
{
let e80;
let e81;
e81 = a18;
let e82;
e82 = a21;
e80 = e81._5(e82);
e4 = e80;
}
{
let e83;
let e84;
e84 = a18;
let e85;
let e86;
e86 = BUILTIN;
let e87;
e87 = a22;
let e88;
e88 = a23;
let e89;
e89 = a10;
e85 = (e86)(e87,e88,e89);
e83 = e84._2(e85);
e4 = e83;
}
{
let e90;
e4 = e90;
}
{
let e91;
let e92;
e92 = a3;
let e93;
e93 = a6;
let e94;
e94 = a5;
let e95;
e95 = a19;
let e96;
e96 = a8;
let e97;
e97 = a2;
e91 = { call: e92, field: e93, lit_str: e94, render: e95, scope_use: e96, type: e97 };
e4 = e91;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
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
e7 = e8.context;
e6 = e7.expr_count;
e5 = e6.toString();
e2 = [e3, e5];
let e9;
{
let e10;
e10 = "";
e9 = e10;
}
e1 = e2.join(e9);
a1 = e1;
}
{
let e11;
let e12;
let e13;
e13 = a0;
e12 = e13.context;
let e14;
let e15;
let e16;
let e17;
e17 = a0;
e16 = e17.context;
e15 = e16.expr_count;
let e18;
{
let e19;
e19 = 1.0;
e18 = e19;
}
e14 = (e15 + e18);
e12.expr_count = e14;
e11 = null;
e0 = e11;
}
let a2;
{
let e20;
let e21;
let e22;
e22 = a0;
e21 = e22.context;
e20 = e21.out;
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
e23 = e24.push(e25);
e0 = e23;
}
{
let e27;
let e28;
e28 = a2;
let e29;
e29 = a1;
e27 = e28.push(e29);
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
e30 = e31.push(e32);
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
e39 = e40.code;
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
{
let e45;
let e46;
let e47;
e47 = a4;
e46 = e47.func;
let e48;
let e49;
e49 = a0;
e48 = e49.context;
e45 = { code: e46, context: e48 };
e44 = e45;
}
e42 = (e43)(e44);
a5 = e42;
}
let a6;
{
let e50;
let e51;
e51 = d0_0_0;
let e52;
{
let e53;
let e54;
let e55;
e55 = a4;
e54 = e55.arg;
let e56;
let e57;
e57 = a0;
e56 = e57.context;
e53 = { code: e54, context: e56 };
e52 = e53;
}
e50 = (e51)(e52);
a6 = e50;
}
{
let e58;
let e59;
e59 = a3;
let e60;
{
let e61;
e61 = "(";
e60 = e61;
}
e58 = e59.push(e60);
e41 = e58;
}
{
let e62;
let e63;
e63 = a3;
let e64;
e64 = a5;
e62 = e63.push(e64);
e41 = e62;
}
{
let e65;
let e66;
e66 = a3;
let e67;
{
let e68;
e68 = ")(";
e67 = e68;
}
e65 = e66.push(e67);
e41 = e65;
}
{
let e69;
let e70;
e70 = a3;
let e71;
e71 = a6;
e69 = e70.push(e71);
e41 = e69;
}
{
let e72;
let e73;
e73 = a3;
let e74;
{
let e75;
e75 = ")";
e74 = e75;
}
e72 = e73.push(e74);
e41 = e72;
}
e38 = e41;
break; }
case "t1": {
let e76;
{
let e77;
let e78;
e78 = a3;
let e79;
let e80;
e80 = BUILTIN;
let e81;
e81 = a4;
e79 = (e80)(e81);
e77 = e78.push(e79);
e76 = e77;
}
e38 = e76;
break; }
case "t2": {
let e82;
let a7;
{
let e83;
let e84;
e84 = d0_0_0;
let e85;
{
let e86;
let e87;
let e88;
e88 = a4;
e87 = e88.base;
let e89;
let e90;
e90 = a0;
e89 = e90.context;
e86 = { code: e87, context: e89 };
e85 = e86;
}
e83 = (e84)(e85);
a7 = e83;
}
let a8;
{
let e91;
let e92;
e92 = d0_0_0;
let e93;
{
let e94;
let e95;
let e96;
e96 = a4;
e95 = e96.field;
let e97;
let e98;
e98 = a0;
e97 = e98.context;
e94 = { code: e95, context: e97 };
e93 = e94;
}
e91 = (e92)(e93);
a8 = e91;
}
{
let e99;
let e100;
e100 = a3;
let e101;
e101 = a7;
e99 = e100.push(e101);
e82 = e99;
}
{
let e102;
let e103;
e103 = a3;
let e104;
{
let e105;
e105 = ".";
e104 = e105;
}
e102 = e103.push(e104);
e82 = e102;
}
{
let e106;
let e107;
e107 = a3;
let e108;
e108 = a8;
e106 = e107.push(e108);
e82 = e106;
}
e38 = e82;
break; }
case "t3": {
let e109;
{
let e110;
let e111;
e111 = a3;
let e112;
e112 = a4;
e110 = e111.push(e112);
e109 = e110;
}
e38 = e109;
break; }
} }
e0 = e38;
}
{
let e113;
let e114;
e114 = a2;
let e115;
let e116;
e116 = a3;
let e117;
{
let e118;
e118 = "";
e117 = e118;
}
e115 = e116.join(e117);
e113 = e114.push(e115);
e0 = e113;
}
{
let e119;
let e120;
e120 = a2;
let e121;
{
let e122;
e122 = ";\n}\n";
e121 = e122;
}
e119 = e120.push(e121);
e0 = e119;
}
{
let e123;
e123 = a1;
e0 = e123;
}
return e0;
}
function d0_0_1(a0) {
let e124;
let a1;
{
let e125;
{
let e126;
let e127;
{
let e128;
e128 = 0.0;
e127 = e128;
}
let e129;
e129 = [];
e126 = { expr_count: e127, out: e129 };
e125 = e126;
}
a1 = e125;
}
{
let e130;
let e131;
let e132;
e132 = a1;
e131 = e132.out;
let e133;
let e134;
e134 = d0_0_0;
let e135;
{
let e136;
let e137;
e137 = a0;
let e138;
e138 = a1;
e136 = { code: e137, context: e138 };
e135 = e136;
}
e133 = (e134)(e135);
e130 = e131.push(e133);
e124 = e130;
}
{
let e139;
let e140;
let e141;
e141 = a1;
e140 = e141.out;
let e142;
{
let e143;
e143 = "";
e142 = e143;
}
e139 = e140.join(e142);
e124 = e139;
}
return e124;
}
let e144;
{
let e145;
let e146;
e146 = BUILTIN;
let e147;
let e148;
e148 = d0_0_1;
let e149;
let e150;
{
let e151;
let e152;
let e153;
{
let e154;
e154 = "Hello world (node)";
e153 = e154;
}
e152 = { t1: e153 };
let e155;
let e156;
{
let e157;
let e158;
let e159;
{
let e160;
e160 = "console";
e159 = e160;
}
e158 = { t3: e159 };
let e161;
let e162;
{
let e163;
e163 = "log";
e162 = e163;
}
e161 = { t1: e162 };
e157 = { base: e158, field: e161 };
e156 = e157;
}
e155 = { t2: e156 };
e151 = { arg: e152, func: e155 };
e150 = e151;
}
e149 = { t0: e150 };
e147 = (e148)(e149);
e145 = (e146)(e147);
e144 = e145;
}
e144;"#);
}
