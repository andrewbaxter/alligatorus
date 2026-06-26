use {
    crate::{
        compile,
        eval::setup_v8,
        syntax::{
            SourceExpr,
            SourceModule,
            SourceSpecAbsolute,
        },
        tests::helpers::new_gx,
    },
    super::helpers::{
        ModuleBuilder,
        redact_builtins,
    },
};

fn build_upper_type(mb: &ModuleBuilder) -> SourceExpr {
    return mb.call(
        mb.lower(
            mb.seq(
                vec![
                    mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                    mb.bind("u", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("upper"))),
                    mb.call(mb.field(mb.scope_use("u"), mb.str("set_inner")), mb.scope_use("null")),
                    mb.call(mb.field(mb.scope_use("u"), mb.str("value")), iface_methods(mb, vec![
                        ("type", mb.scope_use("inner")),
                    ])),
                    mb.call(mb.field(mb.scope_use("u"), mb.str("type")), iface_methods(mb, vec![
                        (
                            "can_cast_from",
                            mb.seq(
                                vec![
                                    mb.call(
                                        mb.field(
                                            mb.call(
                                                mb.field(
                                                    mb.field(mb.scope_use("arg"), mb.str("source_type")),
                                                    mb.str("aspect"),
                                                ),
                                                mb.scope_use("aspect_string_dynamic"),
                                            ),
                                            mb.str("is_some"),
                                        ),
                                        mb.tuple(vec![]),
                                    ),
                                ],
                            ),
                        ),
                        (
                            "cast_from",
                            mb.call(
                                mb.scope_use("const_eval"),
                                mb.tuple(
                                    vec![
                                        mb.field(mb.scope_use("arg"), mb.str("xx")),
                                        mb.stage(
                                            mb.call(
                                                mb.field(
                                                    mb.graft(mb.field(mb.scope_use("arg"), mb.str("cv"))),
                                                    mb.str("toUpperCase"),
                                                ),
                                                mb.tuple(vec![]),
                                            ),
                                        ),
                                    ],
                                ),
                            ),
                        ),
                        ("desc", mb.seq(vec![mb.str("upper")])),
                        (
                            "aspect",
                            mb.call(
                                mb.field(mb.call(mb.scope_use("option"), mb.scope_use("aspect")), mb.str("none")),
                                mb.null(),
                            ),
                        ),
                    ])),
                    mb.field(mb.scope_use("u"), mb.str("new")),
                ],
            ),
        ),
        mb.null(),
    );
}

fn iface_methods(mb: &ModuleBuilder, methods: Vec<(&str, SourceExpr)>) -> SourceExpr {
    let entries: Vec<(&str, SourceExpr)> = methods.into_iter().map(|(k, v)| (k, mb.stage(v))).collect();
    return mb.call(
        mb.field(
            mb.call(mb.scope_use("map"), mb.tuple(vec![mb.scope_use("constkey"), mb.scope_use("expr")])),
            mb.str("new"),
        ),
        mb.record(entries),
    );
}

#[test]
fn test_00100_impl_type() {
    setup_v8();
    let gx = new_gx([("main", SourceModule::V1({
        let mb = ModuleBuilder::default();
        mb.seq(
            vec![
                mb.bind("upper", mb.lower(build_upper_type(&mb))),
                mb.call(
                    mb.scope_use("log"),
                    mb.as_(mb.as_(mb.str("hello"), mb.scope_use("string")), mb.scope_use("upper")),
                ),
            ],
        )
    }))].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    let scripts = redact_builtins(&res.generated_dynamic_scripts);
    assert_eq!(scripts.len(), 3);
    assert!(scripts[1].contains("d0_0_0_0 = function("), "value.type compiled");
    assert!(scripts[1].contains("d0_0_0_1 = function("), "type.can_cast_from compiled (arg)");
    assert!(scripts[1].contains("d0_0_0_2 = function("), "type.cast_from compiled (arg)");
    assert!(scripts[1].contains("d0_0_0_3 = function("), "type.desc compiled (arg)");
    assert!(scripts[1].contains("source_type"), "can_cast_from accesses arg.source_type");
    assert!(scripts[1].contains("\"_0\""), "constructor builds instance object");
    assert!(scripts[2].contains("\"hello\""), "stage 3 contains the literal source string");
    assert!(scripts[2].contains("toUpperCase"), "stage 3 contains the user's toUpperCase invocation");
    assert!(!scripts[2].contains("HELLO"), "stage 3 does not precompute the upper-cased value");
}

#[test]
fn test_00200_impl_type_import() {
    setup_v8();
    let gx = new_gx([
        ("other", SourceModule::V1({
            let mb = ModuleBuilder::default();
            build_upper_type(&mb)
        })),
        ("main", SourceModule::V1({
            let mb = ModuleBuilder::default();
            mb.seq(
                vec![
                    mb.bind("upper", mb.import(mb.str("other"))),
                    mb.call(
                        mb.scope_use("log"),
                        mb.as_(mb.as_(mb.str("hello"), mb.scope_use("string")), mb.scope_use("upper")),
                    ),
                ],
            )
        })),
    ].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
}
