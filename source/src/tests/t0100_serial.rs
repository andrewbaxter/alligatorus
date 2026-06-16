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
    assert_eq!(redact_builtins(&res.generated_dynamic_scripts), vec![r#"let e0;
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
            SourceModule::V1(
                mb.seq(
                    vec![
                        mb.bind(
                            "ctor",
                            mb.lower(
                                mb.seq(
                                    vec![
                                        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                        mb.bind(
                                            "ndr",
                                            mb.call(
                                                mb.field(mb.scope_use("d"), mb.str("object")),
                                                mb.str("upper"),
                                            ),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("ndr"), mb.str("set_inner")),
                                            mb.scope_use("string"),
                                        ),
                                        mb.bind(
                                            "mnfr",
                                            mb.call(
                                                mb.field(mb.scope_use("ndr"), mb.str("func")),
                                                mb.str("to_string"),
                                            ),
                                        ),
                                        mb.call(
                                            mb.field(mb.scope_use("mnfr"), mb.str("impl")),
                                            mb.tuple(
                                                vec![
                                                    mb.scope_use("null"),
                                                    mb.scope_use("string"),
                                                    mb.stage(
                                                        mb.call(
                                                            mb.field(mb.scope_use("inner"), mb.str("toUpperCase")),
                                                            mb.null(),
                                                        ),
                                                    ),
                                                ],
                                            ),
                                        ),
                                        mb.field(mb.scope_use("ndr"), mb.str("new")),
                                    ],
                                ),
                            ),
                        ),
                        mb.call(mb.scope_use("ctor"), mb.str("hello")),
                    ],
                ),
            )
        }),
        ("main", {
            let mb = ModuleBuilder::default();
            SourceModule::V1(
                mb.seq(
                    vec![
                        mb.bind("val", mb.import(mb.str("other"))),
                        mb.call(
                            mb.scope_use("log"),
                            mb.call(mb.field(mb.scope_use("val"), mb.str("to_string")), mb.null()),
                        ),
                    ],
                ),
            )
        }),
    ].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "HELLO");
    assert_eq!(redact_builtins(&res.generated_dynamic_scripts), vec![r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a7;
let e3;
e3 = 1;
let e4;
e4 = a8;
let e5;
let a0;
{
let e6;
let e7;
e7 = BUILTIN;
e6 = (e7)();
a0 = e6;
}
let a1;
{
let e8;
let e9;
e9 = a0;
let e10;
{
let e11;
e11 = "upper";
e10 = e11;
}
e8 = e9["_1"](e10);
a1 = e8;
}
{
let e12;
let e13;
e13 = a1;
let e14;
e14 = a2;
e12 = e13["_3"](e14);
e5 = e12;
}
let a3;
{
let e15;
let e16;
e16 = a1;
let e17;
{
let e18;
e18 = "to_string";
e17 = e18;
}
e15 = e16["_4"](e17);
a3 = e15;
}
{
let e19;
let e20;
e20 = a3;
let e21;
e21 = a4;
let e22;
e22 = a5;
let e23;
e23 = a6;
e19 = e20["_0"](e21,e22,e23);
e5 = e19;
}
{
let e24;
let e25;
e25 = a1;
e24 = e25["_1"];
e5 = e24;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#, r#"var d0_0_0;
{
d0_0_0 = function(a1) {
let a0 = this["\"_0\""];
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
e4 = { "\"_0\"": e5, "\"_1\"": e7 };
e3 = e4;
}
e3;"#, r#"var d0_0_0;
{
d0_0_0 = function(a1) {
let a0 = this["\"_0\""];
let e0;
let e1;
e1 = a0;
let e2;
e2 = null;
e0 = e1["toUpperCase"](e2);
return e0;
};
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
e4 = { "\"_0\"": e5, "\"_1\"": e6 };
let e7;
e7 = null;
e3 = e4["\"_1\""](e7);
e1 = (e2)(e3);
e0 = e1;
}
e0;"#]);
    let arts = test_artifacts.data.lock().unwrap();
    assert_eq!(arts.len(), 2);
    let mut art_jsons: Vec<String> = arts.values().map(|a| serde_json::to_string_pretty(a).unwrap()).collect();
    art_jsons.sort();
    assert_eq!(
        art_jsons,
        vec![
            r#"{
  "data": {
    "data_field": "_0",
    "def_set": {
      "source": [
        [
          0,
          0
        ],
        0
      ]
    },
    "identity": {
      "source": [
        [
          0,
          0
        ],
        0
      ]
    },
    "interface_impls": [],
    "methods": [
      {
        "arg_type": {
          "data": null,
          "type_": {
            "builtin": 7013
          }
        },
        "compiled_js": "var d0_0_0;\n{\nd0_0_0 = function(a1) {\nlet a0 = this[\"\\\"_0\\\"\"];\nlet e0;\nlet e1;\ne1 = a0;\nlet e2;\ne2 = null;\ne0 = e1[\"toUpperCase\"](e2);\nreturn e0;\n};\n}",
        "entry_id": 0,
        "field_name": "_1",
        "name": "to_string",
        "ret_type": {
          "data": null,
          "type_": {
            "builtin": 7019
          }
        }
      }
    ],
    "name": "upper",
    "underlying_type": {
      "data": null,
      "type_": {
        "builtin": 7019
      }
    }
  },
  "type_": {
    "builtin": 7009
  }
}"#,
            r#"{
  "data": {
    "data_types": [
      {
        "source": [
          [
            0,
            0
          ],
          1
        ]
      }
    ],
    "def_set_id": 0,
    "import_id": 0,
    "source_id": 1
  },
  "type_": {
    "builtin": 7001
  }
}"#
        ]
    );
}
