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

fn build_nodejs_code(mb: &ModuleBuilder) -> SerialExpr { }

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
