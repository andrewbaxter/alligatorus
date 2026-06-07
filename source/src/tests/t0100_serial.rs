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
        new_gx_with_artifacts,
        redact_builtins,
    },
};

#[test]
fn test_00100_import_literal() {
    setup_v8();
    let (gx, test_artifacts) = new_gx_with_artifacts([("other", {
        let mb = ModuleBuilder::default();
        SourceModule::V1(mb.str("Hello world (mortar)"))
    }), ("main", {
        let mb = ModuleBuilder::default();
        SourceModule::V1(mb.call(mb.scope_use("log"), mb.import(mb.str("other"))))
    })].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "Hello world (mortar)");
    assert_eq!(redact_builtins(&res.generated_mortar_scripts), vec![r#"let e0;
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
    assert_eq!(test_artifacts.data.lock().unwrap().len(), 0);
}

#[test]
fn test_00200_import_definitionset() {
    setup_v8();
    let (gx, test_artifacts) = new_gx_with_artifacts([
        //. .
        ("other", {
            let mb = ModuleBuilder::default();
            SourceModule::V1(mb.block(vec![
                // First lower: create the definition set and get the constructor
                mb.bind("ctor", mb.lower(mb.block(vec![
                    mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                    mb.bind("ndr", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("upper"))),
                    // Set underlying type to string
                    mb.call(mb.field(mb.scope_use("ndr"), mb.str("set_underlying")), mb.scope_use("string")),
                    // Define to_string method
                    mb.bind("mnfr", mb.call(mb.field(mb.scope_use("ndr"), mb.str("new_func")), mb.str("to_string")),),
                    mb.call(mb.field(mb.scope_use("mnfr"), mb.str("ret")), mb.tuple(vec![mb.scope_use("string")]),),
                    mb.call(
                        mb.field(mb.scope_use("mnfr"), mb.str("impl")),
                        mb.tuple(
                            vec![mb.stage(mb.call(mb.field(mb.scope_use("self"), mb.str("toUpperCase")), mb.null()))]
                        ),
                    ),
                    // Return the constructor
                    mb.field(mb.scope_use("ndr"), mb.str("new")),
                ]))),
                // Call the constructor to create an instance
                mb.call(mb.scope_use("ctor"), mb.str("hello")),
            ]))
        }),
        ("main", {
            let mb = ModuleBuilder::default();
            SourceModule::V1(
                mb.block(
                    vec![
                        mb.bind("val", mb.import(mb.str("other"))),
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.field(mb.scope_use("val"), mb.str("to_string")), mb.null(),),
                        ),
                    ],
                ),
            )
        }),
    ].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    assert_eq!(redact_builtins(&res.generated_mortar_scripts), vec![r#"let e0;
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
e7 = e8._1(e9);
a1 = e7;
}
{
let e11;
let e12;
e12 = a1;
let e13;
e13 = a2;
e11 = e12._3(e13);
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
e14 = e15._4(e16);
a3 = e14;
}
{
let e18;
let e19;
e19 = a3;
let e20;
e20 = a4;
e18 = e19._1(e20);
e4 = e18;
}
{
let e21;
let e22;
e22 = a3;
let e23;
e23 = a5;
e21 = e22._0(e23);
e4 = e21;
}
{
let e24;
let e25;
e25 = a1;
e24 = e25._1;
e4 = e24;
}
e0 = (e1)(e2,e3,e4);
e0;"#, r#"function d0_0_0() {
let a0 = this._0;
let e0;
let e1;
e1 = a0;
let e2;
e2 = null;
e0 = e1.toUpperCase(e2);
return e0;
}
let e3;
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
e4 = { _0: e5, _1: e7 };
e3 = e4;
}
e3;"#, r#"function d0_0_0() {
let a0 = this._0;
let e0;
let e1;
e1 = a0;
let e2;
e2 = null;
e0 = e1.toUpperCase(e2);
return e0;
}
let e0;
{
let e1;
let e2;
e2 = BUILTIN;
let e3;
let e4;
let e5;
e5 = "hello";
let e6;
e6 = d0_0_0;
e4 = { _0: e5, _1: e6 };
let e7;
e7 = null;
e3 = e4._1(e7);
e1 = (e2)(e3);
e0 = e1;
}
e0;"#]);

    // Assert on serialized artifacts
    let arts = test_artifacts.data.lock().unwrap();
    assert_eq!(arts.len(), 2);
    let mut art_jsons: Vec<String> = arts.values().map(|a| serde_json::to_string_pretty(a).unwrap()).collect();
    art_jsons.sort();
    assert_eq!(
        art_jsons,
        vec![
            r#"{
  "type_": {
    "builtin": {
      "file": "src/eval/value/type_source_data/const.rs",
      "line": 560
    }
  },
  "data": {
    "data_types": [
      {
        "data_field": "_0",
        "identity": {
          "Source": {
            "counter": 0,
            "import_state": 0,
            "source": 0
          }
        },
        "methods": [
          {
            "compiled_js": "function d0_0_0() {\nlet a0 = this._0;\nlet e0;\nlet e1;\ne1 = a0;\nlet e2;\ne2 = null;\ne0 = e1.toUpperCase(e2);\nreturn e0;\n}",
            "field_name": "_1",
            "name": "to_string",
            "ret_type": {
              "data": null,
              "type_": {
                "builtin": {
                  "file": "src/eval/value/type_string/dynamic.rs",
                  "line": 72
                }
              }
            }
          }
        ],
        "name": "upper",
        "underlying_type": {
          "data": null,
          "type_": {
            "builtin": {
              "file": "src/eval/value/type_string/dynamic.rs",
              "line": 72
            }
          }
        }
      }
    ],
    "def_set_id": 0,
    "import_id": 0
  }
}"#,
            r#"{
  "type_": {
    "builtin": {
      "file": "src/eval/value/type_type_source_data_type/dynamic.rs",
      "line": 38
    }
  },
  "data": {
    "def_set": {
      "source": [
        [
          0,
          0
        ],
        0
      ]
    },
    "type_index": 0
  }
}"#
        ]
    );
}
