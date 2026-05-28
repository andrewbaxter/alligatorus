//! One of the first milestones of this project is to generate a `nodejs` target
//! within the language.
//!
//! These tests break this down into:
//!
//! * Chunks of the `nodejs` target AST
//!
//! * Tests to confirm the chunks work
use {
    super::helpers::ModuleBuilder,
    crate::{
        compile,
        eval::setup_v8,
        langhelp::SourceId,
        new_gx,
        syntax::{
            SerialExpr,
            SerialModule,
        },
    },
};

fn build_nodejs_code(mb: &ModuleBuilder) -> SerialExpr {
    return mb.lower(mb.block(vec![
        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
        // Define code variant
        mb.bind("code_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("new_tagged_union")), mb.str("Node.js code"))),
        mb.bind("code", mb.field(mb.scope_use("code_builder"), mb.str("type"))),
        mb.bind(
            "code_call",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(
                    vec![
                        mb.str("call"),
                        mb.record(
                            vec![
                                ("func", mb.scope_use("code")),
                                ("args", mb.call(mb.scope_use("list"), mb.scope_use("string")))
                            ]
                        )
                    ]
                )
            )
        ),
        mb.bind(
            "code_lit",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("lit"), mb.scope_use("string")])
            )
        ),
        mb.bind(
            "code_field_use",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(
                    vec![
                        mb.str("field_use"),
                        mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("string"))])
                    ]
                )
            )
        ),
        mb.bind(
            "code_scope_use",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("scope_use"), mb.record(vec![("field", mb.scope_use("string"))])])
            )
        ),
        // Define render code function (inner)
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
                        mb.call(
                            mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("push")),
                            mb.scope_use("key")
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
                        mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.scope_use("key")),
                        mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(" = ")),
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
                                                "args",
                                                mb.call(
                                                    mb.field(
                                                        mb.call(mb.scope_use("list"), mb.scope_use("string")),
                                                        mb.str("new")
                                                    ),
                                                    mb.tuple(vec![])
                                                )
                                            ),
                                            mb.foreach(
                                                mb.field(mb.scope_use("code"), mb.str("args")),
                                                mb.str("arg"),
                                                mb.call(
                                                    mb.field(mb.scope_use("args"), mb.str("push")),
                                                    mb.call(
                                                        mb.lower(mb.scope_use("render_inner")),
                                                        mb.record(
                                                            vec![
                                                                (
                                                                    "context",
                                                                    mb.field(mb.scope_use("arg"), mb.str("context")),
                                                                ),
                                                                ("code", mb.scope_use("arg"))
                                                            ]
                                                        )
                                                    )
                                                )
                                            ),
                                            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str("(")),
                                            mb.call(
                                                mb.field(mb.scope_use("out"), mb.str("push")),
                                                mb.scope_use("func")
                                            ),
                                            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(")(")),
                                            mb.bind("arg_count", mb.num(0.)),
                                            mb.foreach(
                                                mb.scope_use("args"),
                                                mb.str("arg"),
                                                mb.block(
                                                    vec![
                                                        mb.if_(vec![(
                                                            // todo flip + use gt for clarity
                                                            mb.operator_lt(mb.num(0.), mb.scope_use("arg_count")),
                                                            mb.call(
                                                                mb.field(mb.scope_use("out"), mb.str("push")),
                                                                mb.str(", ")
                                                            ),
                                                        )], None),
                                                        mb.call(
                                                            mb.field(mb.scope_use("out"), mb.str("push")),
                                                            mb.scope_use("arg")
                                                        ),
                                                        mb.set(
                                                            mb.scope_use("arg_count"),
                                                            mb.operator_add(mb.scope_use("arg_count"), mb.num(1.))
                                                        ),
                                                    ]
                                                )
                                            ),
                                            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(")")),
                                        ]
                                    ),
                                ),
                                (
                                    mb.str("lit"),
                                    mb.block(
                                        vec![
                                            mb.call(
                                                mb.field(mb.scope_use("out"), mb.str("push")),
                                                mb.call(mb.scope_use("str_to_json"), mb.scope_use("code"))
                                            )
                                        ]
                                    ),
                                ),
                                (
                                    mb.str("field_use"),
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
                                                mb.field(mb.scope_use("out"), mb.str("push")),
                                                mb.scope_use("base")
                                            ),
                                            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(".")),
                                            mb.call(
                                                mb.field(mb.scope_use("out"), mb.str("push")),
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
                                                mb.field(mb.scope_use("out"), mb.str("push")),
                                                mb.field(mb.scope_use("code"), mb.str("field"))
                                            ),
                                        ]
                                    ),
                                ),
                            ],
                            None
                        ),
                        mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n}\n")),
                        mb.scope_use("key")
                    ]
                )
            )
        ),
        // Define render func (outer)
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
                ("lit", mb.scope_use("code_lit")),
                ("field_use", mb.scope_use("code_field_use")),
                ("scope_use", mb.scope_use("code_scope_use")),
                ("render", mb.scope_use("render"))
            ]
        )
    ]));
}

#[tokio::test]
async fn test_0000_nodejs_code() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let code = |key: &str, arg: SerialExpr| {
        return mb.call(mb.field(mb.scope_use("code"), mb.str(key)), arg);
    };
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind("code", build_nodejs_code(&mb)),
                        mb.call(
                            mb.scope_use("log"),
                            code(
                                "render",
                                code(
                                    "call",
                                    mb.tuple(
                                        vec![
                                            code(
                                                "field",
                                                mb.tuple(
                                                    vec![
                                                        code("scope_use", mb.str("console")),
                                                        code("lit_str", mb.str("log"))
                                                    ]
                                                )
                                            ),
                                            code("lit_str", mb.str("Hello world (node)"))
                                        ]
                                    )
                                )
                            )
                        )
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, r#"let e0;
{
let e1;
{
let e2;
{
e2 = console;
}
e1 = e2.log;
}
let e3;
{
e3 = "Hello world (node)";
}
e0 = (e1)(e3);
}
e0;"#);
}
