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
fn test_00800define_data() {
    //! Define an "uppercase string" data type with string as the underlying type, a
    //! "to_string" method that uppercases self, construct one, call to_string, and log.
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx = new_gx([("main", SourceModule::V1(mb.block(vec![
        // Define the data type inside a lower block, return the constructor
        mb.bind("make_upper", mb.lower(mb.block(vec![
            mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
            mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("upper"))),
            // Set underlying type to string
            mb.call(mb.field(mb.scope_use("ndr"), mb.str("set_underlying")), mb.scope_use("string")),
            // Define to_string method
            mb.bind("mnfr", mb.call(mb.field(mb.scope_use("ndr"), mb.str("new_func")), mb.str("to_string"))),
            mb.call(mb.field(mb.scope_use("mnfr"), mb.str("ret")), mb.tuple(vec![mb.scope_use("string")])),
            mb.call(
                mb.field(mb.scope_use("mnfr"), mb.str("impl")),
                mb.tuple(
                    vec![mb.stage(mb.call(mb.field(mb.scope_use("self"), mb.str("toUpperCase")), mb.null()))],
                ),
            ),
            // Return the constructor
            mb.field(mb.scope_use("ndr"), mb.str("new")),
        ]))),
        // Create an instance and call to_string
        mb.bind("val", mb.call(mb.scope_use("make_upper"), mb.str("hello"))),
        mb.call(mb.scope_use("log"), mb.call(mb.field(mb.scope_use("val"), mb.str("to_string")), mb.null())),
    ])))].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    let js_sources = sanitize_builtins(&res.generated_mortar_scripts);
    assert_eq!(js_sources[0], r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a6;
let e3;
e3 = a7;
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
let a3;
{
let e14;
let e15;
e15 = a1;
let e16;
{
let e17;
e17 = "to_string";
e16 = e17;
}
e14 = e15["_4"](e16);
a3 = e14;
}
{
let e18;
let e19;
e19 = a3;
let e20;
e20 = a4;
e18 = e19["_1"](e20);
e4 = e18;
}
{
let e21;
let e22;
e22 = a3;
let e23;
e23 = a5;
e21 = e22["_0"](e23);
e4 = e21;
}
{
let e24;
let e25;
e25 = a1;
e24 = e25["_1"];
e4 = e24;
}
e0 = (e1)(e2,e3,e4);
e0;"#);
    assert_eq!(js_sources[1], r#"var d0_0_0;
{
d0_0_0 = function() {
let a0 = this._0;
let e0;
let e1;
e1 = a0;
let e2;
e2 = null;
e0 = e1["toUpperCase"](e2);
return e0;
};
}
let e3;
let a0;
{
let e4;
let e5;
{
let e6;
e6 = "hello";
e5 = e6;
}
let e7;
e7 = d0_0_0;
e4 = { "_0": e5, "_1": e7 };
a0 = e4;
}
{
let e8;
let e9;
e9 = BUILTIN;
let e10;
let e11;
e11 = a0;
let e12;
e12 = null;
e10 = e11["_1"](e12);
e8 = (e9)(e10);
e3 = e8;
}
e3;"#);
}
