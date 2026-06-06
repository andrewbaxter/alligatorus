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
fn test_0000_list() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.block(
                            vec![
                                mb.bind(
                                    "my_list",
                                    mb.call(
                                        mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new"),),
                                        mb.tuple(vec![mb.str("c"), mb.str("d")])
                                    )
                                ),
                                mb.call(mb.field(mb.scope_use("my_list"), mb.str("push")), mb.str("a")),
                                mb.call(mb.field(mb.scope_use("my_list"), mb.str("push")), mb.str("b")),
                                mb.call(
                                    mb.scope_use("log"),
                                    mb.call(mb.field(mb.scope_use("my_list"), mb.str("join")), mb.str("_"))
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
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, "c_d_a_b");
}
