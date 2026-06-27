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
    },
};

#[test]
fn test_01300_import_transitive_definitionset() {
    setup_v8();
    let gx = new_gx([
        //. .
        ("module_b", SourceModule::V1({
            let mb = ModuleBuilder::default();
            mb.lower(mb.seq(vec![
                //. .
                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("func_b"))),
                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                    mb.tuple(vec![mb.scope_use("null"), mb.scope_use("string"), mb.stage(mb.str("hello"))]),
                ),
                mb.call(mb.field(mb.scope_use("d"), mb.str("finish")), mb.tuple(vec![])),
                mb.scope_use("f"),
            ]))
        })),
        ("module_a", SourceModule::V1({
            let mb = ModuleBuilder::default();
            mb.lower(mb.seq(vec![
                //. .
                mb.bind("func_b", mb.import(mb.str("module_b"))),
                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                mb.bind("nfr", mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("func_a"))),
                mb.bind("f", mb.field(mb.scope_use("nfr"), mb.str("func"))),
                mb.call(
                    mb.field(mb.scope_use("nfr"), mb.str("impl")),
                    mb.tuple(
                        vec![
                            mb.scope_use("null"),
                            mb.scope_use("string"),
                            mb.stage(mb.call(mb.lower(mb.scope_use("func_b")), mb.null())),
                        ],
                    ),
                ),
                mb.call(mb.field(mb.scope_use("d"), mb.str("finish")), mb.tuple(vec![])),
                mb.scope_use("f"),
            ]))
        })),
        ("main", SourceModule::V1({
            let mb = ModuleBuilder::default();
            mb.seq(vec![
                //. .
                mb.bind("func_a", mb.import(mb.str("module_a"))),
                mb.call(mb.scope_use("log"), mb.call(mb.scope_use("func_a"), mb.null())),
            ])
        })),
    ].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "hello");
}
