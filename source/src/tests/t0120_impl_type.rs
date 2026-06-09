use {
    crate::{
        compile,
        eval::setup_v8,
        syntax::{
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

#[test]
fn test_00100_impl_type() {
    setup_v8();
    let gx = new_gx([("main", SourceModule::V1({
        let mb = ModuleBuilder::default();
        mb.block(vec![
            // Bind new "upper" type
            mb.bind("upper", mb.lower(
                // Instantiate type
                mb.call(
                    // Return constructor of type that implements "type"
                    mb.lower(
                        mb.block(
                            vec![
                                mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                mb.bind(
                                    "u",
                                    mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("upper")),
                                ),
                                mb.call(mb.field(mb.scope_use("u"), mb.str("set_underlying")), mb.tuple(vec![])),
                                mb.call(
                                    mb.field(mb.scope_use("u"), mb.str("impl_interface")),
                                    mb.tuple(vec![mb.scope_use("value"), mb.record(vec![
                                        //. .
                                        ("type", mb.stage(mb.block(vec![mb.call(mb.scope_use("panic"), mb.tuple(vec![]))]))),
                                    ])]),
                                ),
                                mb.call(
                                    mb.field(mb.scope_use("u"), mb.str("impl_interface")),
                                    // Create a new aspect for types that can be implemented from dynamic.
                                    mb.tuple(vec![mb.scope_use("type"), mb.record(vec![
                                        //. .
                                        ("can_cast_from", mb.stage(mb.block(vec![
                                            // Need a new "option" type like "list" and "map". For now it just has an
                                            // `is_some() -> bool` method.
                                            mb.call(mb.field(
                                                // The dynamic function on the type should be implemented so that it receives an
                                                // aspect, then internally uses an aspect trait method to return the aspect's
                                                // unique, and does the lookup
                                                mb.call(
                                                    mb.field(
                                                        mb.field(mb.scope_use("arg"), mb.str("source_type")),
                                                        mb.str("aspect"),
                                                    ),
                                                    mb.scope_use("aspect_string_dynamic"),
                                                ),
                                                mb.str("is_some"),
                                            ), mb.tuple(vec![])),
                                        ]))),
                                        (
                                            "cast_from",
                                            mb.stage(
                                                mb.call(
                                                    mb.scope_use("eval_const"),
                                                    mb.record(
                                                        vec![
                                                            ("xx", mb.field(mb.scope_use("arg"), mb.str("xx"))),
                                                            ("expr", mb.stage(mb.call(mb.field(
                                                                // Graft is like "lower" except it just takes an EvalRes and puts it in the tree,
                                                                // to be used as-is when evaluated.
                                                                mb.graft(mb.field(mb.scope_use("arg"), "cv")),
                                                                mb.str("to_upper"),
                                                            ), mb.tuple(vec![])))),
                                                        ],
                                                    ),
                                                ),
                                            ),
                                        ),
                                        ("desc", mb.stage(mb.block(vec![mb.str("upper")]))),
                                    ])]),
                                ),
                                mb.field(mb.scope_use("u"), mb.str("new")),
                            ],
                        ),
                    ),
                    mb.tuple(vec![]),
                ),
            )),
            // Cast a string to the "upper" type (which actually results in a string) and log
            // it
            mb.call(mb.scope_use("log"), mb.as_(mb.str("hello"), mb.scope_use("upper"))),
        ])
    }))].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    assert_eq!(redact_builtins(&res.generated_mortar_scripts), vec!["TODO"]);
}
// TODO as module
