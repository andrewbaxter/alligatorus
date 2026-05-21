use {
    super::helpers::ModuleBuilder,
    crate::{
        compile,
        new_gx,
        eval::setup_v8,
        langhelp::SourceId,
        syntax::SerialModule,
    },
};

#[tokio::test]
async fn test_00900define_interface() {
    //! Define a "to_string" interface with a single method, create an "uppercase string"
    //! data type that implements it, construct one, call to_string, and log.
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs = compile(gx, SourceId(0), SerialModule::V1(mb.seq(vec![
        mb.bind("make_upper", mb.lower(mb.seq(vec![
            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),

            // Create interface with a to_string method
            mb.bind("iface", mb.call(
                mb.field(mb.scope_use("d"), mb.str("new_interface")),
                mb.tuple(vec![]),
            )),
            mb.bind("iface_meth", mb.call(
                mb.field(mb.scope_use("iface"), mb.str("new_func")),
                mb.str("to_string"),
            )),
            mb.call(
                mb.field(mb.scope_use("iface_meth"), mb.str("ret")),
                mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
            ),

            // Create data type with String underlying
            mb.bind("ndr", mb.call(
                mb.field(mb.scope_use("d"), mb.str("new_data")),
                mb.str("upper"),
            )),
            mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("set_underlying")),
                mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
            ),

            // Implement interface on data type
            mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                mb.tuple(vec![
                    mb.field(mb.scope_use("iface"), mb.str("type")),
                    mb.stage(mb.record(vec![
                        ("to_string", mb.call(
                            mb.field(mb.scope_use("self"), mb.str("toUpperCase")),
                            mb.null(),
                        )),
                    ])),
                ]),
            ),

            // Return the constructor
            mb.field(mb.scope_use("ndr"), mb.str("new")),
        ]))),
        // Create an instance and call to_string
        mb.bind("val", mb.call(
            mb.scope_use("make_upper"),
            mb.tuple(vec![mb.str("hello")]),
        )),
        mb.call(
            mb.scope_use("log"),
            mb.call(
                mb.field(mb.scope_use("val"), mb.str("to_string")),
                mb.null(),
            ),
        ),
    ]))).await.unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "HELLO");
}

#[tokio::test]
async fn test_00910interface_cast() {
    //! Like test_00900 but also defines a standalone `do_log` function that takes a `to_string`
    //! interface argument, calls `.to_string()` on it, and logs the result. The data type is
    //! passed to `do_log`, requiring an implicit cast from the data type to the interface.
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs = compile(gx, SourceId(0), SerialModule::V1(mb.seq(vec![
        // Lower block defines interface, data type, and function; returns exports
        mb.bind("defs", mb.lower(mb.seq(vec![
            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),

            // Create interface with a to_string method
            mb.bind("iface", mb.call(
                mb.field(mb.scope_use("d"), mb.str("new_interface")),
                mb.tuple(vec![]),
            )),
            mb.bind("iface_meth", mb.call(
                mb.field(mb.scope_use("iface"), mb.str("new_func")),
                mb.str("to_string"),
            )),
            mb.call(
                mb.field(mb.scope_use("iface_meth"), mb.str("ret")),
                mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
            ),

            // Create data type with String underlying
            mb.bind("ndr", mb.call(
                mb.field(mb.scope_use("d"), mb.str("new_data")),
                mb.str("upper"),
            )),
            mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("set_underlying")),
                mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
            ),

            // Implement interface on data type
            mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("impl_interface")),
                mb.tuple(vec![
                    mb.field(mb.scope_use("iface"), mb.str("type")),
                    mb.stage(mb.record(vec![
                        ("to_string", mb.call(
                            mb.field(mb.scope_use("self"), mb.str("toUpperCase")),
                            mb.null(),
                        )),
                    ])),
                ]),
            ),

            // Define do_log: takes a to_string interface arg, calls .to_string(), logs result
            mb.bind("do_log_fn", mb.call(
                mb.field(mb.scope_use("d"), mb.str("new_func")),
                mb.str("do_log"),
            )),
            mb.call(
                mb.field(mb.scope_use("do_log_fn"), mb.str("arg_type")),
                mb.field(mb.scope_use("iface"), mb.str("type")),
            ),
            mb.call(
                mb.field(mb.scope_use("do_log_fn"), mb.str("impl")),
                mb.tuple(vec![mb.stage(
                    mb.call(
                        mb.scope_use("log"),
                        mb.call(
                            mb.field(mb.scope_use("arg"), mb.str("to_string")),
                            mb.null(),
                        ),
                    ),
                )]),
            ),

            // Return exports (contains both "do_log" and "upper")
            mb.call(
                mb.field(mb.scope_use("d"), mb.str("build")),
                mb.tuple(vec![]),
            ),
        ]))),
        // Extract do_log function and upper constructor from exports
        mb.bind("do_log", mb.field(mb.scope_use("defs"), mb.str("do_log"))),
        mb.bind("make_upper", mb.field(mb.scope_use("defs"), mb.str("upper"))),
        // Call do_log with a new upper value - requires implicit cast to interface
        mb.call(
            mb.scope_use("do_log"),
            mb.tuple(vec![mb.call(
                mb.scope_use("make_upper"),
                mb.tuple(vec![mb.str("hello")]),
            )]),
        ),
    ]))).await.unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "HELLO");
}
