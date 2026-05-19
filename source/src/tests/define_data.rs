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
async fn test_00800define_data() {
    //! Define an "uppercase string" data type with string as the underlying type,
    //! a "to_string" method that uppercases self, construct one, call to_string, and log.
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs = compile(gx, SourceId(0), SerialModule::V1(mb.seq(vec![
        // Define the data type inside a lower block, return the constructor
        mb.bind("make_upper", mb.lower(mb.seq(vec![
            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
            mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.tuple(vec![]))),
            // Set underlying type to String
            mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("set_underlying")),
                mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
            ),
            // Define to_string method
            mb.bind("mnfr", mb.call(
                mb.field(mb.scope_use("ndr"), mb.str("new_func")),
                mb.str("to_string"),
            )),
            mb.call(
                mb.field(mb.scope_use("mnfr"), mb.str("ret")),
                mb.tuple(vec![mb.stage(mb.scope_use("String"))]),
            ),
            mb.call(
                mb.field(mb.scope_use("mnfr"), mb.str("impl")),
                mb.tuple(vec![mb.stage(
                    mb.call(
                        mb.field(mb.scope_use("self"), mb.str("toUpperCase")),
                        mb.null(),
                    ),
                )]),
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
