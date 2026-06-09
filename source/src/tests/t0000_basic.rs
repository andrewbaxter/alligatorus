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
fn test_00100_call_lit() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [("main", SourceModule::V1(mb.call(mb.scope_use("log"), mb.str("Hello world (mortar)"))))]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "Hello world (mortar)");
    assert_eq!(sanitize_builtins(&res.generated_mortar_scripts), vec![r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
{
let e3;
e3 = "Hello world (mortar)";
e2 = e3;
}
e0 = (e1)(e2);
e0;"#]);
}

#[test]
fn test_00101_type_error() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([("main", SourceModule::V1(mb.call(mb.scope_use("log"), mb.null())))].into_iter().collect());
    compile(gx, SourceSpecAbsolute::Virtual("main".to_string())).unwrap_err();
}

#[test]
fn test_00200_bind_use() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.block(
                            vec![
                                mb.bind("hello", mb.str("Hello world (mortar)")),
                                mb.call(mb.scope_use("log"), mb.scope_use("hello")),
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
    assert_eq!(res.logs[0].message, "Hello world (mortar)");
    assert_eq!(sanitize_builtins(&res.generated_mortar_scripts), vec![r#"let e0;
{
let e1;
let e2;
e2 = BUILTIN;
let e3;
{
let e4;
e4 = "Hello world (mortar)";
e3 = e4;
}
e1 = (e2)(e3);
e0 = e1;
}
e0;"#]);
}

#[test]
fn test_00300_stage_lower() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [("main", SourceModule::V1(mb.call(mb.scope_use("log"), mb.lower(mb.str("Hello world (mortar)")))))]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "Hello world (mortar)");
    assert_eq!(sanitize_builtins(&res.generated_mortar_scripts), vec![r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a0;
let e3;
e3 = null;
let e4;
e4 = a1;
e0 = (e1)(e2,e3,e4);
e0;"#, r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
{
let e3;
e3 = "Hello world (mortar)";
e2 = e3;
}
e0 = (e1)(e2);
e0;"#]);
}

#[test]
fn test_00400_lower_nostage() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [("main", SourceModule::V1(mb.call(mb.scope_use("log"), mb.lower(mb.str("Hello world (mortar)")))))]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "Hello world (mortar)");
    assert_eq!(sanitize_builtins(&res.generated_mortar_scripts), vec![r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a0;
let e3;
e3 = null;
let e4;
e4 = a1;
e0 = (e1)(e2,e3,e4);
e0;"#, r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
{
let e3;
e3 = "Hello world (mortar)";
e2 = e3;
}
e0 = (e1)(e2);
e0;"#]);
}

#[test]
fn test_00500_method_call() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.field(mb.str("HELLO WORLD"), mb.str("toLowerCase")), mb.null()),
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(sanitize_builtins(&res.generated_mortar_scripts), vec![r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
let e3;
{
let e4;
e4 = "HELLO WORLD";
e3 = e4;
}
let e5;
e5 = null;
e2 = e3["toLowerCase"](e5);
e0 = (e1)(e2);
e0;"#]);
}
