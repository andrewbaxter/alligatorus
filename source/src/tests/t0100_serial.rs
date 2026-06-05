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
        redact_builtins,
    },
};

#[test]
fn test_00100_import_literal() {
    setup_v8();
    let gx = new_gx([("other", {
        let mb = ModuleBuilder::default();
        SourceModule::V1(mb.str("Hello world (mortar)"))
    }), ("main", {
        let mb = ModuleBuilder::default();
        SourceModule::V1(mb.call(mb.scope_use("log"), mb.import(mb.str("other"))))
    })].into_iter().collect());
    let logs = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "Hello world (mortar)");
    assert_eq!(
        redact_builtins(&gx.generated_mortar_scripts.lock().unwrap()),
        vec![
            "let e0;\nlet e1;\ne1 = BUILTIN;\nlet e2;\n{\nlet e3;\ne3 = \"Hello world (mortar)\";\ne2 = e3;\n}\ne0 = (e1)(e2);\ne0;"
        ]
    );
}
