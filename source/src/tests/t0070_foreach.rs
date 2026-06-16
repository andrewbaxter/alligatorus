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
fn test_0000_foreach_list() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.seq(
                            vec![
                                mb.bind(
                                    "my_list",
                                    mb.call(
                                        mb.field(
                                            mb.call(mb.scope_use("list"), mb.scope_use("number")),
                                            mb.str("new"),
                                        ),
                                        mb.tuple(vec![mb.num(1.), mb.num(2.), mb.num(7.)]),
                                    ),
                                ),
                                mb.foreach(
                                    mb.scope_use("my_list"),
                                    mb.str("e"),
                                    mb.seq(
                                        vec![
                                            mb.call(
                                                mb.scope_use("log"),
                                                mb.call(
                                                    mb.field(mb.scope_use("e"), mb.str("to_string")),
                                                    mb.tuple(vec![]),
                                                ),
                                            ),
                                        ],
                                    ),
                                ),
                            ],
                        ),
                    ),
                ),
            ]
                .into_iter()
                .collect(),
        );
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 3);
    assert_eq!(res.logs[0].message, "1");
    assert_eq!(res.logs[1].message, "2");
    assert_eq!(res.logs[2].message, "7");
}
