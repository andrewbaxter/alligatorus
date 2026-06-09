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
    // This test demonstrates defining a new type in source. Until now only
    // source-defined interfaces were supported.
    setup_v8();
    let gx = new_gx([("main", SourceModule::V1({
        let mb = ModuleBuilder::default();
        mb.block(vec![
            // Bind new "upper" type
            mb.bind("upper", mb.lower(
                // Instantiate type.
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
                                mb.call(
                                    mb.field(mb.scope_use("u"), mb.str("set_underlying")),
                                    mb.scope_use("null"),
                                ),
                                mb.call(
                                    mb.field(mb.scope_use("u"), mb.str("impl_interface")),
                                    mb.tuple(vec![mb.scope_use("value"), mb.record(vec![
                                        //. .
                                        ("type", mb.stage(mb.scope_use("self"))),
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
                                                                mb.graft(mb.field(mb.scope_use("arg"), mb.str("cv"))),
                                                                mb.str("toUpperCase"),
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
                    mb.null(),
                ),
            )),
            // Cast a string to the "upper" type (which actually results in a string) and log
            // it
            mb.call(mb.scope_use("log"), mb.as_(
                mb.as_(mb.str("hello"), mb.scope_use("string")),
                // For this to work,
                //
                // 1. const source data types will need to also implement `Type`.
                //
                // 2. The "type" will need to keep the v8 engine alive, and keep a reference to the
                //    instance, then call the v8 methods when any trait method is called, then lift the
                //    values back out.
                mb.scope_use("upper"),
            )),
        ])
    }))].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    let scripts = redact_builtins(&res.generated_mortar_scripts);
    assert_eq!(scripts.len(), 3);

    // Stage 1: build the upper data type — call define(), new_data("upper"),
    // set_underlying(null), impl_interface(value, ...), impl_interface(type, ...),
    // and return u.new.
    assert_eq!(scripts[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a7;
let e3;
e3 = a8;
let e4;
let a0;
{
let e5;
let e6;
e6 = BUILTIN;
e5 = (e6)();
a0 = e5;
}
let a1;
{
let e7;
let e8;
e8 = a0;
let e9;
{
let e10;
e10 = "upper";
e9 = e10;
}
e7 = e8["_1"](e9);
a1 = e7;
}
{
let e11;
let e12;
e12 = a1;
let e13;
e13 = a2;
e11 = e12["_3"](e13);
e4 = e11;
}
{
let e14;
let e15;
e15 = a1;
let e16;
e16 = BUILTIN;
let e17;
let e18;
e18 = "\"type\"";
let e19;
e19 = a3;
e17 = { [e18]: e19 };
e14 = e15["_5"](e16,e17);
e4 = e14;
}
{
let e20;
let e21;
e21 = a1;
let e22;
e22 = BUILTIN;
let e23;
let e24;
e24 = "\"can_cast_from\"";
let e25;
e25 = a4;
let e26;
e26 = "\"cast_from\"";
let e27;
e27 = a5;
let e28;
e28 = "\"desc\"";
let e29;
e29 = a6;
e23 = { [e24]: e25, [e26]: e27, [e28]: e29 };
e20 = e21["_5"](e22,e23);
e4 = e20;
}
{
let e30;
let e31;
e31 = a1;
e30 = e31["_1"];
e4 = e30;
}
e0 = (e1)(e2,e3,e4);
e0;"#);

    // Stage 2: call the constructor with `null` to construct the `upper` instance.
    assert_eq!(scripts[1], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a0;
let e3;
e3 = a1;
let e4;
let e5;
e5 = null;
e4 = { "_0": e5 };
e0 = (e1)(e2,e3,e4);
e0;"#);

    // Stage 3: cast "hello" through the user-defined type — produces
    // "hello".toUpperCase() and logs it.
    assert_eq!(scripts[2], r#"let e0;
{
let e1;
let e2;
e2 = BUILTIN;
let e3;
let e4;
{
let e5;
e5 = "hello";
e4 = e5;
}
e3 = e4["toUpperCase"]();
e1 = (e2)(e3);
e0 = e1;
}
e0;"#);
}
// TODO as module
