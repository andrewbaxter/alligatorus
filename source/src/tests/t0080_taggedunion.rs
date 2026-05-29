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
async fn test_0000_tagged_union_match() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "strnum",
                            mb.lower(
                                mb.block(
                                    vec![
                                        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                        mb.bind(
                                            "builder",
                                            mb.call(
                                                mb.field(mb.scope_use("d"), mb.str("tagged_union")),
                                                mb.tuple(vec![mb.str("strnum")])
                                            )
                                        ),
                                        mb.bind(
                                            "new_str",
                                            mb.call(
                                                mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")])
                                            )
                                        ),
                                        mb.bind(
                                            "new_num",
                                            mb.call(
                                                mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")])
                                            )
                                        ),
                                        mb.record(
                                            vec![
                                                ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                                ("str", mb.scope_use("new_str")),
                                                ("num", mb.scope_use("new_num"))
                                            ]
                                        )
                                    ]
                                )
                            ),
                        ),
                        mb.bind("x", mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.))),
                        mb.match_(
                            mb.scope_use("x"),
                            mb.str("y"),
                            vec![
                                (mb.str("strtag"), mb.call(mb.scope_use("log"), mb.scope_use("y"))),
                                (
                                    mb.str("numtag"),
                                    mb.call(
                                        mb.scope_use("log"),
                                        mb.call(mb.field(mb.scope_use("y"), mb.str("to_string")), mb.tuple(vec![]))
                                    ),
                                )
                            ],
                            None,
                        )
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "7");
}

#[tokio::test]
async fn test_0010_tagged_union_match_fallback() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    let logs =
        compile(
            gx,
            SourceId(0),
            SerialModule::V1(
                mb.block(
                    vec![
                        mb.bind(
                            "strnum",
                            mb.lower(
                                mb.block(
                                    vec![
                                        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                        mb.bind(
                                            "builder",
                                            mb.call(
                                                mb.field(mb.scope_use("d"), mb.str("tagged_union")),
                                                mb.tuple(vec![mb.str("strnum")])
                                            )
                                        ),
                                        mb.bind(
                                            "new_str",
                                            mb.call(
                                                mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")])
                                            )
                                        ),
                                        mb.bind(
                                            "new_num",
                                            mb.call(
                                                mb.field(mb.scope_use("builder"), mb.str("variant")),
                                                mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")])
                                            )
                                        ),
                                        mb.record(
                                            vec![
                                                ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                                ("str", mb.scope_use("new_str")),
                                                ("num", mb.scope_use("new_num"))
                                            ]
                                        )
                                    ]
                                )
                            ),
                        ),
                        mb.bind("x", mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.))),
                        mb.match_(
                            mb.scope_use("x"),
                            mb.str("y"),
                            vec![(mb.str("strtag"), mb.call(mb.scope_use("log"), mb.scope_use("y"))),],
                            Some(mb.call(mb.scope_use("log"), mb.str("fallback")))
                        )
                    ],
                ),
            ),
        )
            .await
            .unwrap();
    assert_eq!(logs.len(), 1);
    assert_eq!(logs[0].message, "fallback");
}

#[tokio::test]
async fn test_0020_tagged_union_nonexhaustive() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    compile(
        gx,
        SourceId(0),
        SerialModule::V1(
            mb.block(
                vec![
                    mb.bind(
                        "strnum",
                        mb.lower(
                            mb.block(
                                vec![
                                    mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                                    mb.bind(
                                        "builder",
                                        mb.call(mb.field(mb.scope_use("d"), mb.str("tagged_union")), mb.tuple(vec![]))
                                    ),
                                    mb.bind(
                                        "new_str",
                                        mb.call(
                                            mb.field(mb.scope_use("builder"), mb.str("variant")),
                                            mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")])
                                        )
                                    ),
                                    mb.bind(
                                        "new_num",
                                        mb.call(
                                            mb.field(mb.scope_use("builder"), mb.str("variant")),
                                            mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")])
                                        )
                                    ),
                                    mb.record(
                                        vec![
                                            ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                            ("str", mb.scope_use("new_str")),
                                            ("num", mb.scope_use("new_num"))
                                        ]
                                    )
                                ]
                            )
                        ),
                    ),
                    mb.bind("x", mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.))),
                    mb.match_(
                        mb.scope_use("x"),
                        mb.str("y"),
                        vec![
                            (
                                mb.str("numtag"),
                                mb.call(
                                    mb.scope_use("log"),
                                    mb.call(mb.field(mb.scope_use("y"), mb.str("to_string")), mb.tuple(vec![]))
                                ),
                            )
                        ],
                        None,
                    )
                ],
            ),
        ),
    )
        .await
        .err()
        .unwrap();
}

#[tokio::test]
async fn test_0030_tagged_union_exhaustive_with_fallback() {
    setup_v8();
    let gx = new_gx();
    let mb = ModuleBuilder::default();
    compile(gx, SourceId(0), SerialModule::V1(mb.block(vec![
        mb.bind(
            "strnum",
            mb.lower(
                mb.block(
                    vec![
                        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
                        mb.bind(
                            "builder",
                            mb.call(
                                mb.field(mb.scope_use("d"), mb.str("tagged_union")),
                                mb.tuple(vec![mb.str("strnum")])
                            )
                        ),
                        mb.bind(
                            "new_str",
                            mb.call(
                                mb.field(mb.scope_use("builder"), mb.str("variant")),
                                mb.tuple(vec![mb.str("strtag"), mb.scope_use("string")])
                            )
                        ),
                        mb.bind(
                            "new_num",
                            mb.call(
                                mb.field(mb.scope_use("builder"), mb.str("variant")),
                                mb.tuple(vec![mb.str("numtag"), mb.scope_use("number")])
                            )
                        ),
                        mb.record(
                            vec![
                                ("type", mb.field(mb.scope_use("builder"), mb.str("type"))),
                                ("str", mb.scope_use("new_str")),
                                ("num", mb.scope_use("new_num"))
                            ]
                        )
                    ]
                )
            ),
        ),
        mb.bind("x", mb.call(mb.field(mb.scope_use("strnum"), mb.str("num")), mb.num(7.))),
        // Exhaustive match (both strtag and numtag) with a fallback should be an error
        mb.match_(
            mb.scope_use("x"),
            mb.str("y"),
            vec![
                (mb.str("strtag"), mb.call(mb.scope_use("log"), mb.scope_use("y"))),
                (
                    mb.str("numtag"),
                    mb.call(
                        mb.scope_use("log"),
                        mb.call(mb.field(mb.scope_use("y"), mb.str("to_string")), mb.tuple(vec![]))
                    ),
                )
            ],
            Some(mb.call(mb.scope_use("log"), mb.str("fallback"))),
        )
    ]))).await.err().unwrap();
}
