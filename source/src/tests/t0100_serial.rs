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

#[test]
fn test_00200_import_definitionset() {
    setup_v8();
    let gx = new_gx([
        //. .
        ("other", {
            let mb = ModuleBuilder::default();
            SourceModule::V1(mb.block(vec![
                mb.bind("make_upper", mb.lower(mb.block(vec![
                    mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                    mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("upper"))),
                    // Set underlying type to string
                    mb.call(mb.field(mb.scope_use("ndr"), mb.str("set_underlying")), mb.scope_use("string"),),
                    // Define to_string method
                    mb.bind("mnfr", mb.call(mb.field(mb.scope_use("ndr"), mb.str("new_func")), mb.str("to_string"),)),
                    mb.call(mb.field(mb.scope_use("mnfr"), mb.str("ret")), mb.tuple(vec![mb.scope_use("string")]),),
                    mb.call(
                        mb.field(mb.scope_use("mnfr"), mb.str("impl")),
                        mb.tuple(
                            vec![mb.stage(mb.call(mb.field(mb.scope_use("self"), mb.str("toUpperCase")), mb.null(),),)]
                        ),
                    ),
                    // Return the constructor
                    mb.field(mb.scope_use("ndr"), mb.str("new")),
                ]))),
                // Return a value
                mb.call(mb.scope_use("make_upper"), mb.str("hello"))
            ]))
        }),
        ("main", {
            let mb = ModuleBuilder::default();
            SourceModule::V1(
                mb.block(
                    vec![
                        mb.bind("make_upper", mb.import(mb.str("other"))),
                        mb.bind("val", mb.call(mb.scope_use("make_upper"), mb.str("hello"),)),
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.field(mb.scope_use("val"), mb.str("to_string")), mb.null(),),
                        ),
                    ],
                ),
            )
        }),
    ].into_iter().collect());
    let logs = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "HELLO");
    assert_eq!(redact_builtins(&gx.generated_mortar_scripts.lock().unwrap()), vec!["TODO"]);
}
