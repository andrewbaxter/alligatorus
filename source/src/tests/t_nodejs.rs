use {
    crate::{
        compile,
        eval::setup_v8,
        syntax::{
            SourceExpr,
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

#[allow(dead_code)]
fn aspect_dispatch(mb: &ModuleBuilder, aspect_names: Vec<&str>) -> SourceExpr {
    let conditions: Vec<(SourceExpr, SourceExpr)> = aspect_names.into_iter().map(|aspect| {
        (
            mb.operator_eq(mb.field(mb.scope_use("arg"), mb.str("key")), mb.builtin(&["eval", aspect])),
            mb.call(
                mb.field(mb.call(mb.types_option(), mb.types_aspect()), mb.str("some")),
                mb.scope_use("outer"),
            ),
        )
    }).collect();
    return mb.if_(conditions, Some(aspect_dispatch_none(mb)));
}

#[allow(dead_code)]
fn aspect_dispatch_none(mb: &ModuleBuilder) -> SourceExpr {
    return mb.call(mb.field(mb.call(mb.types_option(), mb.types_aspect()), mb.str("none")), mb.null());
}

#[allow(dead_code)]
fn build_nodejs(mb: &ModuleBuilder) -> SourceExpr {
    let mut exprs: Vec<SourceExpr> = Vec::new();
    exprs.push(mb.bind("code", build_nodejs_code(mb)));
    exprs.push(mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))));
    exprs.push(mb.bind("root", mb.call(mb.types_option(), mb.types_expr())));
    exprs.push(mb.bind("option_aspect", mb.call(mb.types_option(), mb.types_aspect())));
    exprs.push(mb.bind("nvalue_base", mb.call(mb.field(mb.scope_use("d"), mb.str("union")), mb.tuple(vec![]))));
    exprs.push(
        mb.bind(
            "nvalue_binding",
            mb.call(
                mb.field(mb.scope_use("nvalue_base"), mb.str("variant")),
                mb.tuple(vec![mb.str("binding"), mb.types_string()]),
            ),
        ),
    );
    exprs.push(
        mb.bind(
            "nvalue_expr",
            mb.call(
                mb.field(mb.scope_use("nvalue_base"), mb.str("variant")),
                mb.tuple(vec![mb.str("expr"), mb.types_null()]),
            ),
        ),
    );
    exprs.push(
        mb.bind("nvalue", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs value"))),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("nvalue"), mb.str("set_inner")),
            mb.record(
                vec![("base", mb.field(mb.scope_use("nvalue_base"), mb.str("type"))), ("type", mb.types_type())],
            ),
        ),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("nvalue"), mb.str("value")),
            iface_methods(mb, vec![("type", mb.field(mb.scope_use("inner"), mb.str("type")))]),
        ),
    );
    exprs.push(
        mb.bind(
            "aspect_consume_nodejs",
            mb.call(mb.field(mb.scope_use("d"), mb.str("interface")), mb.tuple(vec![])),
        ),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("set_is_aspect")), mb.bool(true)));
    exprs.push(
        mb.bind(
            "acn_to_code",
            mb.call(mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("func")), mb.str("to_code")),
        ),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("acn_to_code"), mb.str("impl")),
            mb.tuple(vec![mb.types_cve(), mb.field(mb.scope_use("code"), mb.str("type"))]),
        ),
    );
    exprs.push(
        mb.bind("ntype_str", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs string"))),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("ntype_str"), mb.str("set_inner")), mb.tuple(vec![])));
    exprs.extend(impl_as_self_type(mb, "ntype_str", "nodejs string", aspect_dispatch_none(mb)));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("ntype_str"), mb.str("impl_interface")),
            mb.tuple(
                vec![
                    mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("type")),
                    iface_methods(mb, vec![("to_code", nvalue_to_code(mb, mb.scope_use("arg")))]),
                ],
            ),
        ),
    );
    exprs.push(
        mb.bind("ntype_func", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs function"))),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("ntype_func"), mb.str("set_inner")),
            mb.record(vec![("arg", mb.types_type()), ("ret", mb.types_type())]),
        ),
    );
    exprs.extend(
        impl_as_self_type(mb, "ntype_func", "nodejs function", aspect_dispatch(mb, vec!["aspect_function_id"])),
    );
    exprs.push(
        impl_iface(
            mb,
            "ntype_func",
            "aspect_function",
            vec![
                (
                    "call",
                    mb.seq(
                        vec![
                            mb.bind(
                                "func_code",
                                nvalue_to_code(mb, mb.field(mb.scope_use("arg"), mb.str("inner"))),
                            ),
                            mb.bind(
                                "arg_code",
                                mb.match_(
                                    mb.call(
                                        mb.eval_aspect_lookup(),
                                        mb.tuple(
                                            vec![
                                                mb.field(
                                                    mb.field(
                                                        mb.field(mb.scope_use("arg"), mb.str("argument")),
                                                        mb.str("value"),
                                                    ),
                                                    mb.str("type"),
                                                ),
                                                mb.lower(
                                                    mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("type")),
                                                ),
                                            ],
                                        ),
                                    ),
                                    mb.str("v"),
                                    vec![
                                        (
                                            mb.str("some"),
                                            mb.call(
                                                mb.field(mb.scope_use("v"), mb.str("to_code")),
                                                mb.field(mb.scope_use("arg"), mb.str("argument")),
                                            ),
                                        ),
                                        (
                                            mb.str("none"),
                                            mb.call(
                                                mb.lower(mb.field(mb.scope_use("code"), mb.str("lit_str"))),
                                                mb.as_(
                                                    mb.field(mb.scope_use("arg"), mb.str("argument")),
                                                    mb.types_string(),
                                                ),
                                            ),
                                        ),
                                    ],
                                    None,
                                ),
                            ),
                            mb.bind(
                                "call_code",
                                mb.call(
                                    mb.lower(mb.field(mb.scope_use("code"), mb.str("call"))),
                                    mb.record(
                                        vec![
                                            ("func", mb.scope_use("func_code")),
                                            ("arg", mb.scope_use("arg_code")),
                                        ],
                                    ),
                                ),
                            ),
                            mb.call(
                                mb.eval_codevalue(),
                                mb.tuple(
                                    vec![
                                        mb.call(
                                            mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("some")),
                                            mb.scope_use("call_code"),
                                        ),
                                        mb.call(
                                            mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("new"))),
                                            mb.record(
                                                vec![
                                                    (
                                                        "base",
                                                        mb.call(mb.lower(mb.scope_use("nvalue_expr")), mb.null()),
                                                    ),
                                                    ("type", mb.field(mb.scope_use("inner"), mb.str("ret"))),
                                                ],
                                            ),
                                        ),
                                    ],
                                ),
                            ),
                        ],
                    ),
                ),
            ],
        ),
    );
    exprs.push(
        mb.bind("ntype_object", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs object"))),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("ntype_object"), mb.str("set_inner")),
            mb.call(mb.types_map(), mb.tuple(vec![mb.types_string(), mb.types_type()])),
        ),
    );
    exprs.extend(
        impl_as_self_type(mb, "ntype_object", "nodejs object", aspect_dispatch(mb, vec!["aspect_field_id"])),
    );
    exprs.push(
        impl_iface(
            mb,
            "ntype_object",
            "aspect_field",
            vec![
                (
                    "field",
                    mb.seq(
                        vec![
                            mb.bind(
                                "base_code",
                                nvalue_to_code(mb, mb.field(mb.scope_use("arg"), mb.str("inner"))),
                            ),
                            mb.bind(
                                "field_code",
                                mb.call(
                                    mb.lower(mb.field(mb.scope_use("code"), mb.str("field"))),
                                    mb.record(
                                        vec![
                                            ("base", mb.scope_use("base_code")),
                                            (
                                                "field",
                                                mb.call(
                                                    mb.lower(mb.field(mb.scope_use("code"), mb.str("lit_str"))),
                                                    mb.field(mb.scope_use("arg"), mb.str("key")),
                                                ),
                                            ),
                                        ],
                                    ),
                                ),
                            ),
                            mb.call(
                                mb.eval_codevalue(),
                                mb.tuple(
                                    vec![
                                        mb.call(
                                            mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("some")),
                                            mb.scope_use("field_code"),
                                        ),
                                        mb.call(
                                            mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("new"))),
                                            mb.record(
                                                vec![
                                                    (
                                                        "base",
                                                        mb.call(mb.lower(mb.scope_use("nvalue_expr")), mb.null()),
                                                    ),
                                                    (
                                                        "type",
                                                        mb.call(
                                                            mb.field(mb.scope_use("inner"), mb.str("get")),
                                                            mb.field(mb.scope_use("arg"), mb.str("key")),
                                                        ),
                                                    ),
                                                ],
                                            ),
                                        ),
                                    ],
                                ),
                            ),
                        ],
                    ),
                ),
                (
                    "field_type_opt",
                    mb.call(
                        mb.field(mb.scope_use("inner"), mb.str("get")),
                        mb.field(mb.scope_use("arg"), mb.str("key")),
                    ),
                ),
            ],
        ),
    );
    exprs.push(
        mb.bind("nbinding", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs binding"))),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("nbinding"), mb.str("set_inner")),
            mb.record(vec![("name", mb.types_string()), ("type", mb.types_type())]),
        ),
    );
    exprs.push(
        impl_iface(
            mb,
            "nbinding",
            "binding",
            vec![
                (
                    "use",
                    mb.call(
                        mb.eval_codevalue(),
                        mb.tuple(
                            vec![
                                mb.call(
                                    mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("some")),
                                    mb.call(
                                        mb.lower(mb.field(mb.scope_use("code"), mb.str("scope_use"))),
                                        mb.field(mb.scope_use("inner"), mb.str("name")),
                                    ),
                                ),
                                mb.call(
                                    mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("new"))),
                                    mb.record(
                                        vec![
                                            (
                                                "base",
                                                mb.call(
                                                    mb.lower(mb.scope_use("nvalue_binding")),
                                                    mb.field(mb.scope_use("inner"), mb.str("name")),
                                                ),
                                            ),
                                            ("type", mb.field(mb.scope_use("inner"), mb.str("type"))),
                                        ],
                                    ),
                                ),
                            ],
                        ),
                    ),
                ),
            ],
        ),
    );
    exprs.push(
        mb.bind("ntarget", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs target"))),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("ntarget"), mb.str("set_inner")), mb.types_null()));
    exprs.push(
        impl_iface(
            mb,
            "ntarget",
            "target",
            vec![
                (
                    "bind",
                    mb.seq(
                        vec![
                            mb.bind(
                                "name",
                                mb.call(mb.eval_gen_binding_name(), mb.field(mb.scope_use("arg"), mb.str("xx"))),
                            ),
                            mb.tuple(
                                vec![
                                    mb.call(
                                        mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("none")),
                                        mb.null(),
                                    ),
                                    mb.scope_use("name"),
                                ],
                            ),
                        ],
                    ),
                ),
                ("bind_sub_record", mb.panic(mb.str("ntarget.bind_sub_record not implemented"))),
                ("bind_sub_tuple", mb.panic(mb.str("ntarget.bind_sub_tuple not implemented"))),
                (
                    "seq",
                    mb.call(mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("none")), mb.null()),
                ),
                ("code_to_bind_sub", mb.panic(mb.str("ntarget.code_to_bind_sub not implemented"))),
                ("if", mb.panic(mb.str("ntarget.if not implemented"))),
                ("match", mb.panic(mb.str("ntarget.match not implemented"))),
                (
                    "maybe_bind_sub",
                    mb.call(
                        mb.lower(mb.field(mb.scope_use("root"), mb.str("some"))),
                        mb.field(mb.scope_use("arg"), mb.str("cv")),
                    ),
                ),
                ("stage", mb.panic(mb.str("ntarget.stage not implemented"))),
                (
                    "cast_exception",
                    mb.call(mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("none")), mb.null()),
                ),
                (
                    "drop",
                    mb.call(mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("none")), mb.null()),
                ),
                (
                    "try_finally",
                    mb.call(mb.field(mb.call(mb.types_option(), mb.types_codedyn()), mb.str("none")), mb.null()),
                ),
            ],
        ),
    );
    exprs.push(mb.bind("napp", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs app"))));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("napp"), mb.str("set_inner")),
            mb.record(vec![("root", mb.scope_use("root"))]),
        ),
    );
    exprs.push(mb.bind("napp_root", mb.call(mb.field(mb.scope_use("napp"), mb.str("func")), mb.str("root"))));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("napp_root"), mb.str("impl")),
            mb.tuple(
                vec![
                    mb.types_expr(),
                    mb.types_null(),
                    mb.stage(
                        mb.set(
                            mb.field(mb.scope_use("inner"), mb.str("root")),
                            mb.call(
                                mb.lower(mb.field(mb.scope_use("root"), mb.str("some"))),
                                mb.scope_use("arg"),
                            ),
                        ),
                    ),
                ],
            ),
        ),
    );
    exprs.push(mb.bind("napp_build", mb.call(mb.field(mb.scope_use("napp"), mb.str("func")), mb.str("build"))));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("napp_build"), mb.str("impl")),
            mb.tuple(
                vec![
                    mb.tuple(vec![]),
                    mb.types_string(),
                    mb.stage(
                        mb.seq(
                            vec![
                                mb.match_(
                                    mb.field(mb.scope_use("inner"), mb.str("root")),
                                    mb.str("root"),
                                    vec![
                                        (
                                            mb.str("some"),
                                            mb.seq(
                                                vec![
                                                    mb.bind(
                                                        "ntype_str_inst",
                                                        mb.call(
                                                            mb.lower(
                                                                mb.field(mb.scope_use("ntype_str"), mb.str("new")),
                                                            ),
                                                            mb.tuple(vec![]),
                                                        ),
                                                    ),
                                                    mb.bind(
                                                        "ntype_log",
                                                        mb.call(
                                                            mb.lower(
                                                                mb.field(mb.scope_use("ntype_func"), mb.str("new")),
                                                            ),
                                                            mb.record(
                                                                vec![
                                                                    ("arg", mb.scope_use("ntype_str_inst")),
                                                                    ("ret", mb.scope_use("ntype_str_inst")),
                                                                ],
                                                            ),
                                                        ),
                                                    ),
                                                    mb.bind(
                                                        "ntype_console",
                                                        mb.call(
                                                            mb.lower(
                                                                mb.field(
                                                                    mb.scope_use("ntype_object"),
                                                                    mb.str("new"),
                                                                ),
                                                            ),
                                                            mb.call(
                                                                mb.field(
                                                                    mb.call(
                                                                        mb.types_map(),
                                                                        mb.tuple(
                                                                            vec![mb.types_string(), mb.types_type()],
                                                                        ),
                                                                    ),
                                                                    mb.str("new"),
                                                                ),
                                                                mb.record(
                                                                    vec![("log", mb.scope_use("ntype_log"))],
                                                                ),
                                                            ),
                                                        ),
                                                    ),
                                                    mb.bind(
                                                        "console_entry",
                                                        mb.call(
                                                            mb.lower(
                                                                mb.field(mb.scope_use("nbinding"), mb.str("new")),
                                                            ),
                                                            mb.record(
                                                                vec![
                                                                    ("name", mb.str("console")),
                                                                    ("type", mb.scope_use("ntype_console")),
                                                                ],
                                                            ),
                                                        ),
                                                    ),
                                                    mb.bind(
                                                        "res",
                                                        mb.call(
                                                            mb.eval_const_eval(),
                                                            mb.tuple(
                                                                vec![
                                                                    mb.call(
                                                                        mb.eval_new_expr_context(),
                                                                        mb.tuple(
                                                                            vec![
                                                                                mb.call(
                                                                                    mb.lower(
                                                                                        mb.field(
                                                                                            mb.scope_use("ntarget"),
                                                                                            mb.str("new"),
                                                                                        ),
                                                                                    ),
                                                                                    mb.null(),
                                                                                ),
                                                                                mb.record(
                                                                                    vec![
                                                                                        (
                                                                                            "console",
                                                                                            mb.scope_use(
                                                                                                "console_entry",
                                                                                            ),
                                                                                        ),
                                                                                    ],
                                                                                ),
                                                                            ],
                                                                        ),
                                                                    ),
                                                                    mb.scope_use("root"),
                                                                ],
                                                            ),
                                                        ),
                                                    ),
                                                    mb.match_(
                                                        mb.field(
                                                            mb.field(mb.scope_use("res"), mb.str("code")),
                                                            mb.str("code"),
                                                        ),
                                                        mb.str("v"),
                                                        vec![
                                                            (
                                                                mb.str("dyn"),
                                                                mb.call(
                                                                    mb.lower(
                                                                        mb.field(
                                                                            mb.scope_use("code"),
                                                                            mb.str("render"),
                                                                        ),
                                                                    ),
                                                                    mb.call(
                                                                        mb.field(mb.scope_use("v"), mb.str("cast")),
                                                                        mb.lower(
                                                                            mb.field(
                                                                                mb.scope_use("code"),
                                                                                mb.str("type"),
                                                                            ),
                                                                        ),
                                                                    ),
                                                                ),
                                                            ),
                                                            (mb.str("none"), mb.str("")),
                                                        ],
                                                        None,
                                                    ),
                                                ],
                                            ),
                                        ),
                                        (mb.str("none"), mb.seq(vec![mb.str("")])),
                                    ],
                                    None,
                                ),
                            ],
                        ),
                    ),
                ],
            ),
        ),
    );
    exprs.push(mb.bind("nodejs", mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("nodejs"))));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("nodejs"), mb.str("impl")),
            mb.tuple(
                vec![
                    mb.tuple(vec![]),
                    mb.field(mb.scope_use("napp"), mb.str("type_ref")),
                    mb.stage(
                        mb.call(
                            mb.lower(mb.field(mb.scope_use("napp"), mb.str("new"))),
                            mb.record(
                                vec![
                                    (
                                        "root",
                                        mb.call(
                                            mb.lower(mb.field(mb.scope_use("root"), mb.str("none"))),
                                            mb.null(),
                                        ),
                                    ),
                                ],
                            ),
                        ),
                    ),
                ],
            ),
        ),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("d"), mb.str("finish")), mb.tuple(vec![])));
    exprs.push(mb.field(mb.scope_use("nodejs"), mb.str("func")));
    return mb.seq(exprs);
}

fn build_nodejs_code(mb: &ModuleBuilder) -> SourceExpr {
    return mb.seq(
        vec![
            mb.bind("d", mb.call(mb.define(), mb.tuple(vec![]))),
            mb.bind("code_tu_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("union")), mb.tuple(vec![]))),
            mb.bind("code_tu", mb.field(mb.scope_use("code_tu_builder"), mb.str("type"))),
            mb.bind("code_data", mb.call(mb.field(mb.scope_use("d"), mb.str("object")), mb.str("nodejs code"))),
            mb.call(mb.field(mb.scope_use("code_data"), mb.str("set_inner")), mb.scope_use("code_tu")),
            mb.bind("code", mb.field(mb.scope_use("code_data"), mb.str("type_ref"))),
            mb.bind(
                "code_tu_call",
                mb.call(
                    mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")),
                    mb.tuple(
                        vec![
                            mb.str("call"),
                            mb.record(vec![("func", mb.scope_use("code")), ("arg", mb.scope_use("code"))]),
                        ],
                    ),
                ),
            ),
            mb.bind(
                "code_tu_lit_str",
                mb.call(
                    mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")),
                    mb.tuple(vec![mb.str("lit_str"), mb.types_string()]),
                ),
            ),
            mb.bind(
                "code_tu_field",
                mb.call(
                    mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")),
                    mb.tuple(
                        vec![
                            mb.str("field"),
                            mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("code"))]),
                        ],
                    ),
                ),
            ),
            mb.bind(
                "code_tu_scope_use",
                mb.call(
                    mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")),
                    mb.tuple(vec![mb.str("scope_use"), mb.types_string()]),
                ),
            ),
            mb.call(
                mb.field(mb.scope_use("code_data"), mb.str("impl_interface")),
                mb.tuple(vec![mb.eval_code_dyn(), iface_methods(mb, vec![])]),
            ),
            mb.bind(
                "code_get_builder",
                mb.call(mb.field(mb.scope_use("code_data"), mb.str("func")), mb.str("get")),
            ),
            mb.call(
                mb.field(mb.scope_use("code_get_builder"), mb.str("impl")),
                mb.tuple(vec![mb.tuple(vec![]), mb.scope_use("code_tu"), mb.stage(mb.scope_use("inner"))]),
            ),
            mb.bind(
                "code_call_builder",
                mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("code.call")),
            ),
            mb.bind("code_call", mb.field(mb.scope_use("code_call_builder"), mb.str("func"))),
            mb.call(
                mb.field(mb.scope_use("code_call_builder"), mb.str("impl")),
                mb.tuple(
                    vec![
                        mb.record(vec![("func", mb.scope_use("code")), ("arg", mb.scope_use("code"))]),
                        mb.scope_use("code"),
                        mb.stage(
                            mb.call(
                                mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                                mb.call(mb.lower(mb.scope_use("code_tu_call")), mb.scope_use("arg")),
                            ),
                        ),
                    ],
                ),
            ),
            mb.bind(
                "code_lit_str_builder",
                mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("code.lit_str")),
            ),
            mb.bind("code_lit_str", mb.field(mb.scope_use("code_lit_str_builder"), mb.str("func"))),
            mb.call(
                mb.field(mb.scope_use("code_lit_str_builder"), mb.str("impl")),
                mb.tuple(
                    vec![
                        mb.types_string(),
                        mb.scope_use("code"),
                        mb.stage(
                            mb.call(
                                mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                                mb.call(mb.lower(mb.scope_use("code_tu_lit_str")), mb.scope_use("arg")),
                            ),
                        ),
                    ],
                ),
            ),
            mb.bind(
                "code_field_builder",
                mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("code.field")),
            ),
            mb.bind("code_field", mb.field(mb.scope_use("code_field_builder"), mb.str("func"))),
            mb.call(
                mb.field(mb.scope_use("code_field_builder"), mb.str("impl")),
                mb.tuple(
                    vec![
                        mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("code"))]),
                        mb.scope_use("code"),
                        mb.stage(
                            mb.call(
                                mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                                mb.call(mb.lower(mb.scope_use("code_tu_field")), mb.scope_use("arg")),
                            ),
                        ),
                    ],
                ),
            ),
            mb.bind(
                "code_scope_use_builder",
                mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("code.scope_use")),
            ),
            mb.bind("code_scope_use", mb.field(mb.scope_use("code_scope_use_builder"), mb.str("func"))),
            mb.call(
                mb.field(mb.scope_use("code_scope_use_builder"), mb.str("impl")),
                mb.tuple(
                    vec![
                        mb.types_string(),
                        mb.scope_use("code"),
                        mb.stage(
                            mb.call(
                                mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                                mb.call(mb.lower(mb.scope_use("code_tu_scope_use")), mb.scope_use("arg")),
                            ),
                        ),
                    ],
                ),
            ),
            mb.bind(
                "render_inner_builder",
                mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("Render Node.js code (inner)")),
            ),
            mb.bind("render_inner", mb.field(mb.scope_use("render_inner_builder"), mb.str("func"))),
            mb.call(
                mb.field(mb.scope_use("render_inner_builder"), mb.str("impl")),
                mb.tuple(
                    vec![
                        mb.record(
                            vec![
                                (
                                    "context",
                                    mb.record(
                                        vec![
                                            ("expr_count", mb.types_number()),
                                            ("out", mb.call(mb.types_list(), mb.types_string())),
                                        ],
                                    ),
                                ),
                                ("code", mb.scope_use("code")),
                            ],
                        ),
                        mb.types_string(),
                        mb.stage(
                            mb.seq(
                                vec![
                                    mb.bind(
                                        "key",
                                        mb.call(
                                            mb.field(
                                                mb.call(
                                                    mb.field(
                                                        mb.call(mb.types_list(), mb.types_string()),
                                                        mb.str("new"),
                                                    ),
                                                    mb.tuple(
                                                        vec![
                                                            mb.str("e"),
                                                            mb.call(
                                                                mb.field(
                                                                    mb.field(
                                                                        mb.field(
                                                                            mb.scope_use("arg"),
                                                                            mb.str("context"),
                                                                        ),
                                                                        mb.str("expr_count"),
                                                                    ),
                                                                    mb.str("to_string"),
                                                                ),
                                                                mb.tuple(vec![]),
                                                            ),
                                                        ],
                                                    ),
                                                ),
                                                mb.str("join"),
                                            ),
                                            mb.str(""),
                                        ),
                                    ),
                                    mb.set(
                                        mb.field(
                                            mb.field(mb.scope_use("arg"), mb.str("context")),
                                            mb.str("expr_count"),
                                        ),
                                        mb.operator_add(
                                            mb.field(
                                                mb.field(mb.scope_use("arg"), mb.str("context")),
                                                mb.str("expr_count"),
                                            ),
                                            mb.num(1.),
                                        ),
                                    ),
                                    mb.bind(
                                        "out",
                                        mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("out")),
                                    ),
                                    mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str("let ")),
                                    mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.scope_use("key")),
                                    mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n{\n")),
                                    mb.bind(
                                        "value",
                                        mb.call(
                                            mb.field(mb.call(mb.types_list(), mb.types_string()), mb.str("new")),
                                            mb.tuple(vec![mb.scope_use("key"), mb.str(" = ")]),
                                        ),
                                    ),
                                    mb.match_(
                                        mb.call(
                                            mb.field(mb.field(mb.scope_use("arg"), mb.str("code")), mb.str("get")),
                                            mb.tuple(vec![]),
                                        ),
                                        mb.str("code"),
                                        vec![
                                            (
                                                mb.str("call"),
                                                mb.seq(
                                                    vec![
                                                        mb.bind(
                                                            "func",
                                                            mb.call(
                                                                mb.lower(mb.scope_use("render_inner")),
                                                                mb.record(
                                                                    vec![
                                                                        (
                                                                            "context",
                                                                            mb.field(
                                                                                mb.scope_use("arg"),
                                                                                mb.str("context"),
                                                                            ),
                                                                        ),
                                                                        (
                                                                            "code",
                                                                            mb.field(
                                                                                mb.scope_use("code"),
                                                                                mb.str("func"),
                                                                            ),
                                                                        ),
                                                                    ],
                                                                ),
                                                            ),
                                                        ),
                                                        mb.bind(
                                                            "rendered_arg",
                                                            mb.call(
                                                                mb.lower(mb.scope_use("render_inner")),
                                                                mb.record(
                                                                    vec![
                                                                        (
                                                                            "context",
                                                                            mb.field(
                                                                                mb.scope_use("arg"),
                                                                                mb.str("context"),
                                                                            ),
                                                                        ),
                                                                        (
                                                                            "code",
                                                                            mb.field(
                                                                                mb.scope_use("code"),
                                                                                mb.str("arg"),
                                                                            ),
                                                                        ),
                                                                    ],
                                                                ),
                                                            ),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.str("("),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.scope_use("func"),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.str(")("),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.scope_use("rendered_arg"),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.str(")"),
                                                        ),
                                                    ],
                                                ),
                                            ),
                                            (
                                                mb.str("lit_str"),
                                                mb.seq(
                                                    vec![
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.call(mb.eval_str_to_json(), mb.scope_use("code")),
                                                        ),
                                                    ],
                                                ),
                                            ),
                                            (
                                                mb.str("field"),
                                                mb.seq(
                                                    vec![
                                                        mb.bind(
                                                            "base",
                                                            mb.call(
                                                                mb.lower(mb.scope_use("render_inner")),
                                                                mb.record(
                                                                    vec![
                                                                        (
                                                                            "context",
                                                                            mb.field(
                                                                                mb.scope_use("arg"),
                                                                                mb.str("context"),
                                                                            ),
                                                                        ),
                                                                        (
                                                                            "code",
                                                                            mb.field(
                                                                                mb.scope_use("code"),
                                                                                mb.str("base"),
                                                                            ),
                                                                        ),
                                                                    ],
                                                                ),
                                                            ),
                                                        ),
                                                        mb.bind(
                                                            "field",
                                                            mb.call(
                                                                mb.lower(mb.scope_use("render_inner")),
                                                                mb.record(
                                                                    vec![
                                                                        (
                                                                            "context",
                                                                            mb.field(
                                                                                mb.scope_use("arg"),
                                                                                mb.str("context"),
                                                                            ),
                                                                        ),
                                                                        (
                                                                            "code",
                                                                            mb.field(
                                                                                mb.scope_use("code"),
                                                                                mb.str("field"),
                                                                            ),
                                                                        ),
                                                                    ],
                                                                ),
                                                            ),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.scope_use("base"),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.str("."),
                                                        ),
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.scope_use("field"),
                                                        ),
                                                    ],
                                                ),
                                            ),
                                            (
                                                mb.str("scope_use"),
                                                mb.seq(
                                                    vec![
                                                        mb.call(
                                                            mb.field(mb.scope_use("value"), mb.str("push")),
                                                            mb.scope_use("code"),
                                                        ),
                                                    ],
                                                ),
                                            ),
                                        ],
                                        None,
                                    ),
                                    mb.call(
                                        mb.field(mb.scope_use("out"), mb.str("push")),
                                        mb.call(mb.field(mb.scope_use("value"), mb.str("join")), mb.str("")),
                                    ),
                                    mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n}\n")),
                                    mb.scope_use("key"),
                                ],
                            ),
                        ),
                    ],
                ),
            ),
            mb.bind(
                "render_builder",
                mb.call(mb.field(mb.scope_use("d"), mb.str("func")), mb.str("Render Node.js code")),
            ),
            mb.bind("render", mb.field(mb.scope_use("render_builder"), mb.str("func"))),
            mb.call(
                mb.field(mb.scope_use("render_builder"), mb.str("impl")),
                mb.tuple(
                    vec![
                        mb.scope_use("code"),
                        mb.types_string(),
                        mb.stage(
                            mb.seq(
                                vec![
                                    mb.bind(
                                        "context",
                                        mb.record(
                                            vec![
                                                (
                                                    "out",
                                                    mb.call(
                                                        mb.field(
                                                            mb.call(mb.types_list(), mb.types_string()),
                                                            mb.str("new"),
                                                        ),
                                                        mb.tuple(vec![]),
                                                    ),
                                                ),
                                                ("expr_count", mb.as_(mb.num(0.), mb.types_number())),
                                            ],
                                        ),
                                    ),
                                    mb.call(
                                        mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("push")),
                                        mb.call(
                                            mb.lower(mb.scope_use("render_inner")),
                                            mb.record(
                                                vec![
                                                    ("context", mb.scope_use("context")),
                                                    ("code", mb.scope_use("arg")),
                                                ],
                                            ),
                                        ),
                                    ),
                                    mb.call(
                                        mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("join")),
                                        mb.str(""),
                                    ),
                                ],
                            ),
                        ),
                    ],
                ),
            ),
            mb.call(mb.field(mb.scope_use("d"), mb.str("finish")), mb.tuple(vec![])),
            mb.record(
                vec![
                    ("type", mb.scope_use("code")),
                    ("call", mb.scope_use("code_call")),
                    ("lit_str", mb.scope_use("code_lit_str")),
                    ("field", mb.scope_use("code_field")),
                    ("scope_use", mb.scope_use("code_scope_use")),
                    ("render", mb.scope_use("render")),
                ],
            ),
        ],
    );
}

fn iface_methods(mb: &ModuleBuilder, methods: Vec<(&str, SourceExpr)>) -> SourceExpr {
    let entries: Vec<(&str, SourceExpr)> = methods.into_iter().map(|(k, v)| (k, mb.stage(v))).collect();
    return mb.call(
        mb.field(mb.call(mb.types_map(), mb.tuple(vec![mb.types_constkey(), mb.types_expr()])), mb.str("new")),
        mb.record(entries),
    );
}

#[allow(dead_code)]
fn impl_as_self_type(
    mb: &ModuleBuilder,
    target_var: &str,
    desc: &str,
    aspect_dispatch: SourceExpr,
) -> Vec<SourceExpr> {
    return vec![
        mb.call(
            mb.field(mb.scope_use(target_var), mb.str("value")),
            iface_methods(mb, vec![("type", mb.scope_use("outer"))]),
        ),
        mb.call(
            mb.field(mb.scope_use(target_var), mb.str("type")),
            iface_methods(
                mb,
                vec![
                    ("can_cast_from", mb.bool(false)),
                    ("cast_from", mb.field(mb.scope_use("arg"), mb.str("cv"))),
                    ("desc", mb.str(desc)),
                    ("aspect", aspect_dispatch),
                ],
            ),
        ),
    ];
}

#[allow(dead_code)]
fn impl_iface(mb: &ModuleBuilder, target_var: &str, iface: &str, methods: Vec<(&str, SourceExpr)>) -> SourceExpr {
    return mb.call(
        mb.field(mb.scope_use(target_var), mb.str("impl_interface")),
        mb.tuple(vec![mb.builtin(&["eval", iface]), iface_methods(mb, methods)]),
    );
}

#[allow(dead_code)]
fn nvalue_to_code(mb: &ModuleBuilder, cve_expr: SourceExpr) -> SourceExpr {
    return mb.match_(
        mb.field(mb.field(cve_expr, mb.str("code")), mb.str("code")),
        mb.str("c"),
        vec![
            (
                mb.str("dyn"),
                mb.call(
                    mb.field(mb.scope_use("c"), mb.str("cast")),
                    mb.lower(mb.field(mb.scope_use("code"), mb.str("type"))),
                ),
            ),
            (mb.str("none"), mb.call(mb.lower(mb.field(mb.scope_use("code"), mb.str("lit_str"))), mb.str(""))),
        ],
        None,
    );
}

#[test]
fn test_0000_nodejs_code() {
    setup_v8();
    let mb = ModuleBuilder::default();
    let code = |key: &str, arg: SourceExpr| {
        return mb.call(mb.field(mb.scope_use("code"), mb.str(key)), arg);
    };
    let gx =
        new_gx(
            [
                (
                    "main",
                    SourceModule::V1(
                        mb.seq(
                            vec![
                                mb.bind("code", mb.lower(build_nodejs_code(&mb))),
                                mb.call(
                                    mb.log(),
                                    code(
                                        "render",
                                        code(
                                            "call",
                                            mb.record(
                                                vec![
                                                    (
                                                        "func",
                                                        code(
                                                            "field",
                                                            mb.record(
                                                                vec![
                                                                    ("base", code("scope_use", mb.str("console"))),
                                                                    ("field", code("lit_str", mb.str("log"))),
                                                                ],
                                                            ),
                                                        ),
                                                    ),
                                                    ("arg", code("lit_str", mb.str("Hello world (node)"))),
                                                ],
                                            ),
                                        ),
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
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, r#"let e0;
{
let e1;
{
let e2;
{
e2 = console;
}
let e3;
{
e3 = "log";
}
e1 = e2.e3;
}
let e4;
{
e4 = "Hello world (node)";
}
e0 = (e1)(e4);
}
e0"#);
    let dynamic_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert_eq!(dynamic_sources.len(), 2);
    assert_eq!(
        dynamic_sources[0],
        r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a50;
let e3;
e3 = 1;
let e4;
e4 = a51;
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
e8 = e9["_3"]();
a1 = e8;
}
let a2;
{
let e10;
let e11;
e11 = a1;
e10 = e11["_0"];
a2 = e10;
}
let a3;
{
let e12;
let e13;
e13 = a0;
let e14;
{
let e15;
e15 = "nodejs code";
e14 = e15;
}
e12 = e13["_1"](e14);
a3 = e12;
}
{
let e16;
let e17;
e17 = a3;
let e18;
let e19;
e19 = BUILTIN;
let e20;
e20 = a2;
e18 = (e19)(e20);
e16 = e17["_3"](e18);
e5 = e16;
}
let a4;
{
let e21;
let e22;
e22 = a3;
e21 = e22["_0"];
a4 = e21;
}
let a5;
{
let e23;
let e24;
e24 = a1;
let e25;
{
let e26;
e26 = "call";
e25 = e26;
}
let e27;
let e28;
e28 = BUILTIN;
let e29;
let e30;
let e31;
e31 = BUILTIN;
let e32;
e32 = a4;
e30 = (e31)(e32);
let e33;
let e34;
e34 = BUILTIN;
let e35;
e35 = a4;
e33 = (e34)(e35);
e29 = { "\"arg\"": e30, "\"func\"": e33 };
e27 = (e28)(e29);
e23 = e24["_1"](e25,e27);
a5 = e23;
}
let a7;
{
let e36;
let e37;
e37 = a1;
let e38;
{
let e39;
e39 = "lit_str";
e38 = e39;
}
let e40;
e40 = a6;
e36 = e37["_1"](e38,e40);
a7 = e36;
}
let a8;
{
let e41;
let e42;
e42 = a1;
let e43;
{
let e44;
e44 = "field";
e43 = e44;
}
let e45;
let e46;
e46 = BUILTIN;
let e47;
let e48;
let e49;
e49 = BUILTIN;
let e50;
e50 = a4;
e48 = (e49)(e50);
let e51;
let e52;
e52 = BUILTIN;
let e53;
e53 = a4;
e51 = (e52)(e53);
e47 = { "\"base\"": e48, "\"field\"": e51 };
e45 = (e46)(e47);
e41 = e42["_1"](e43,e45);
a8 = e41;
}
let a10;
{
let e54;
let e55;
e55 = a1;
let e56;
{
let e57;
e57 = "scope_use";
e56 = e57;
}
let e58;
e58 = a9;
e54 = e55["_1"](e56,e58);
a10 = e54;
}
{
let e59;
let e60;
e60 = a3;
let e61;
e61 = BUILTIN;
let e62;
e62 = {  };
e59 = e60["_5"](e61,e62);
e5 = e59;
}
let a11;
{
let e63;
let e64;
e64 = a3;
let e65;
{
let e66;
e66 = "get";
e65 = e66;
}
e63 = e64["_4"](e65);
a11 = e63;
}
{
let e67;
let e68;
e68 = a11;
let e69;
let e70;
e70 = BUILTIN;
let e71;
e71 = [];
e69 = (e70)(e71);
let e72;
let e73;
e73 = BUILTIN;
let e74;
e74 = a2;
e72 = (e73)(e74);
let e75;
e75 = a12;
e67 = e68["_0"](e69,e72,e75);
e5 = e67;
}
let a13;
{
let e76;
let e77;
e77 = a0;
let e78;
{
let e79;
e79 = "code.call";
e78 = e79;
}
e76 = e77["_0"](e78);
a13 = e76;
}
let a14;
{
let e80;
let e81;
e81 = a13;
e80 = e81["_0"];
a14 = e80;
}
{
let e82;
let e83;
e83 = a13;
let e84;
let e85;
e85 = BUILTIN;
let e86;
let e87;
let e88;
e88 = BUILTIN;
let e89;
e89 = a4;
e87 = (e88)(e89);
let e90;
let e91;
e91 = BUILTIN;
let e92;
e92 = a4;
e90 = (e91)(e92);
e86 = { "\"arg\"": e87, "\"func\"": e90 };
e84 = (e85)(e86);
let e93;
let e94;
e94 = BUILTIN;
let e95;
e95 = a4;
e93 = (e94)(e95);
let e96;
let e97;
e97 = BUILTIN;
let e98;
e98 = a15;
let e99;
e99 = 2;
let e100;
e100 = a16;
let e101;
let e102;
e102 = a3;
e101 = e102["_1"];
let e103;
e103 = a17;
let e104;
e104 = a5;
e96 = (e97)(e98,e99,e100,e101,e103,e104);
e82 = e83["_2"](e84,e93,e96);
e5 = e82;
}
let a18;
{
let e105;
let e106;
e106 = a0;
let e107;
{
let e108;
e108 = "code.lit_str";
e107 = e108;
}
e105 = e106["_0"](e107);
a18 = e105;
}
let a19;
{
let e109;
let e110;
e110 = a18;
e109 = e110["_0"];
a19 = e109;
}
{
let e111;
let e112;
e112 = a18;
let e113;
e113 = a23;
let e114;
let e115;
e115 = BUILTIN;
let e116;
e116 = a4;
e114 = (e115)(e116);
let e117;
let e118;
e118 = BUILTIN;
let e119;
e119 = a20;
let e120;
e120 = 2;
let e121;
e121 = a21;
let e122;
let e123;
e123 = a3;
e122 = e123["_1"];
let e124;
e124 = a22;
let e125;
e125 = a7;
e117 = (e118)(e119,e120,e121,e122,e124,e125);
e111 = e112["_2"](e113,e114,e117);
e5 = e111;
}
let a24;
{
let e126;
let e127;
e127 = a0;
let e128;
{
let e129;
e129 = "code.field";
e128 = e129;
}
e126 = e127["_0"](e128);
a24 = e126;
}
let a25;
{
let e130;
let e131;
e131 = a24;
e130 = e131["_0"];
a25 = e130;
}
{
let e132;
let e133;
e133 = a24;
let e134;
let e135;
e135 = BUILTIN;
let e136;
let e137;
let e138;
e138 = BUILTIN;
let e139;
e139 = a4;
e137 = (e138)(e139);
let e140;
let e141;
e141 = BUILTIN;
let e142;
e142 = a4;
e140 = (e141)(e142);
e136 = { "\"base\"": e137, "\"field\"": e140 };
e134 = (e135)(e136);
let e143;
let e144;
e144 = BUILTIN;
let e145;
e145 = a4;
e143 = (e144)(e145);
let e146;
let e147;
e147 = BUILTIN;
let e148;
e148 = a26;
let e149;
e149 = 2;
let e150;
e150 = a27;
let e151;
let e152;
e152 = a3;
e151 = e152["_1"];
let e153;
e153 = a28;
let e154;
e154 = a8;
e146 = (e147)(e148,e149,e150,e151,e153,e154);
e132 = e133["_2"](e134,e143,e146);
e5 = e132;
}
let a29;
{
let e155;
let e156;
e156 = a0;
let e157;
{
let e158;
e158 = "code.scope_use";
e157 = e158;
}
e155 = e156["_0"](e157);
a29 = e155;
}
let a30;
{
let e159;
let e160;
e160 = a29;
e159 = e160["_0"];
a30 = e159;
}
{
let e161;
let e162;
e162 = a29;
let e163;
e163 = a34;
let e164;
let e165;
e165 = BUILTIN;
let e166;
e166 = a4;
e164 = (e165)(e166);
let e167;
let e168;
e168 = BUILTIN;
let e169;
e169 = a31;
let e170;
e170 = 2;
let e171;
e171 = a32;
let e172;
let e173;
e173 = a3;
e172 = e173["_1"];
let e174;
e174 = a33;
let e175;
e175 = a10;
e167 = (e168)(e169,e170,e171,e172,e174,e175);
e161 = e162["_2"](e163,e164,e167);
e5 = e161;
}
let a35;
{
let e176;
let e177;
e177 = a0;
let e178;
{
let e179;
e179 = "Render Node.js code (inner)";
e178 = e179;
}
e176 = e177["_0"](e178);
a35 = e176;
}
let a36;
{
let e180;
let e181;
e181 = a35;
e180 = e181["_0"];
a36 = e180;
}
{
let e182;
let e183;
e183 = a35;
let e184;
let e185;
e185 = BUILTIN;
let e186;
let e187;
let e188;
e188 = BUILTIN;
let e189;
e189 = a4;
e187 = (e188)(e189);
let e190;
let e191;
e191 = BUILTIN;
let e192;
let e193;
e193 = a42;
let e194;
e194 = a43;
e192 = { "\"expr_count\"": e193, "\"out\"": e194 };
e190 = (e191)(e192);
e186 = { "\"code\"": e187, "\"context\"": e190 };
e184 = (e185)(e186);
let e195;
e195 = a44;
let e196;
let e197;
e197 = BUILTIN;
let e198;
e198 = a37;
let e199;
e199 = 4;
let e200;
e200 = a38;
let e201;
e201 = a36;
let e202;
e202 = a39;
let e203;
e203 = a36;
let e204;
e204 = a40;
let e205;
e205 = a36;
let e206;
e206 = a41;
let e207;
e207 = a36;
e196 = (e197)(e198,e199,e200,e201,e202,e203,e204,e205,e206,e207);
e182 = e183["_2"](e184,e195,e196);
e5 = e182;
}
let a45;
{
let e208;
let e209;
e209 = a0;
let e210;
{
let e211;
e211 = "Render Node.js code";
e210 = e211;
}
e208 = e209["_0"](e210);
a45 = e208;
}
let a46;
{
let e212;
let e213;
e213 = a45;
e212 = e213["_0"];
a46 = e212;
}
{
let e214;
let e215;
e215 = a45;
let e216;
let e217;
e217 = BUILTIN;
let e218;
e218 = a4;
e216 = (e217)(e218);
let e219;
e219 = a49;
let e220;
let e221;
e221 = BUILTIN;
let e222;
e222 = a47;
let e223;
e223 = 1;
let e224;
e224 = a48;
let e225;
e225 = a36;
e220 = (e221)(e222,e223,e224,e225);
e214 = e215["_2"](e216,e219,e220);
e5 = e214;
}
{
let e226;
let e227;
e227 = a0;
e226 = e227["_5"]();
e5 = e226;
}
{
let e228;
let e229;
e229 = a14;
let e230;
e230 = a25;
let e231;
e231 = a19;
let e232;
e232 = a46;
let e233;
e233 = a30;
let e234;
e234 = a4;
e228 = { "\"call\"": e229, "\"field\"": e230, "\"lit_str\"": e231, "\"render\"": e232, "\"scope_use\"": e233, "\"type\"": e234 };
e5 = e228;
}
e0 = (e1)(e2,e3,e4,e5);
e0;"#
    );
    assert_eq!(dynamic_sources[1], r#"var d0_0_0_1;
{
d0_0_0_1 = function(a0) {
let e0;
let e1;
let e2;
e2 = a0;
e1 = { "\"t0\"": e2 };
let e3;
e3 = d0_0_0_0;
e0 = { "\"_0\"": e1, "\"_1\"": e3 };
return e0;
};
}
var d0_0_0_2;
{
d0_0_0_2 = function(a0) {
let e0;
let e1;
let e2;
e2 = a0;
e1 = { "\"t1\"": e2 };
let e3;
e3 = d0_0_0_0;
e0 = { "\"_0\"": e1, "\"_1\"": e3 };
return e0;
};
}
var d0_0_0_3;
{
d0_0_0_3 = function(a0) {
let e0;
let e1;
let e2;
e2 = a0;
e1 = { "\"t2\"": e2 };
let e3;
e3 = d0_0_0_0;
e0 = { "\"_0\"": e1, "\"_1\"": e3 };
return e0;
};
}
var d0_0_0_4;
{
d0_0_0_4 = function(a0) {
let e0;
let e1;
let e2;
e2 = a0;
e1 = { "\"t3\"": e2 };
let e3;
e3 = d0_0_0_0;
e0 = { "\"_0\"": e1, "\"_1\"": e3 };
return e0;
};
}
var d0_0_0_5;
{
d0_0_0_5 = function(a0) {
let e0;
let a1;
{
let e1;
let e2;
let e3;
{
let e4;
e4 = "e";
e3 = e4;
}
let e5;
let e6;
let e7;
let e8;
e8 = a0;
e7 = e8["\"context\""];
e6 = e7["\"expr_count\""];
e5 = e6["toString"]();
e2 = [e3, e5];
let e9;
{
let e10;
e10 = "";
e9 = e10;
}
e1 = e2["join"](e9);
a1 = e1;
}
{
let e11;
let e12;
let e13;
e13 = a0;
e12 = e13["\"context\""];
let e14;
let e15;
let e16;
let e17;
e17 = a0;
e16 = e17["\"context\""];
e15 = e16["\"expr_count\""];
let e18;
{
let e19;
e19 = 1.0;
e18 = e19;
}
e14 = (e15 + e18);
e12["\"expr_count\""] = e14;
e11 = null;
e0 = e11;
}
let a2;
{
let e20;
let e21;
let e22;
e22 = a0;
e21 = e22["\"context\""];
e20 = e21["\"out\""];
a2 = e20;
}
{
let e23;
let e24;
e24 = a2;
let e25;
{
let e26;
e26 = "let ";
e25 = e26;
}
e23 = e24["push"](e25);
e0 = e23;
}
{
let e27;
let e28;
e28 = a2;
let e29;
e29 = a1;
e27 = e28["push"](e29);
e0 = e27;
}
{
let e30;
let e31;
e31 = a2;
let e32;
{
let e33;
e33 = ";\n{\n";
e32 = e33;
}
e30 = e31["push"](e32);
e0 = e30;
}
let a3;
{
let e34;
let e35;
e35 = a1;
let e36;
{
let e37;
e37 = " = ";
e36 = e37;
}
e34 = [e35, e36];
a3 = e34;
}
{
let e38;
let e39;
let e40;
let e41;
e41 = a0;
e40 = e41["\"code\""];
e39 = e40["\"_1\""]();
{ let [__tag, a4] = Object.entries(e39)[0];
switch (__tag) {
case "\"t0\"": {
let e42;
let a5;
{
let e43;
let e44;
e44 = d0_0_0_5;
let e45;
let e46;
let e47;
e47 = a4;
e46 = e47["\"func\""];
let e48;
let e49;
e49 = a0;
e48 = e49["\"context\""];
e45 = { "\"code\"": e46, "\"context\"": e48 };
e43 = (e44)(e45);
a5 = e43;
}
let a6;
{
let e50;
let e51;
e51 = d0_0_0_5;
let e52;
let e53;
let e54;
e54 = a4;
e53 = e54["\"arg\""];
let e55;
let e56;
e56 = a0;
e55 = e56["\"context\""];
e52 = { "\"code\"": e53, "\"context\"": e55 };
e50 = (e51)(e52);
a6 = e50;
}
{
let e57;
let e58;
e58 = a3;
let e59;
{
let e60;
e60 = "(";
e59 = e60;
}
e57 = e58["push"](e59);
e42 = e57;
}
{
let e61;
let e62;
e62 = a3;
let e63;
e63 = a5;
e61 = e62["push"](e63);
e42 = e61;
}
{
let e64;
let e65;
e65 = a3;
let e66;
{
let e67;
e67 = ")(";
e66 = e67;
}
e64 = e65["push"](e66);
e42 = e64;
}
{
let e68;
let e69;
e69 = a3;
let e70;
e70 = a6;
e68 = e69["push"](e70);
e42 = e68;
}
{
let e71;
let e72;
e72 = a3;
let e73;
{
let e74;
e74 = ")";
e73 = e74;
}
e71 = e72["push"](e73);
e42 = e71;
}
e38 = e42;
break; }
case "\"t1\"": {
let e75;
{
let e76;
let e77;
e77 = a3;
let e78;
let e79;
e79 = BUILTIN;
let e80;
e80 = a4;
e78 = (e79)(e80);
e76 = e77["push"](e78);
e75 = e76;
}
e38 = e75;
break; }
case "\"t2\"": {
let e81;
let a7;
{
let e82;
let e83;
e83 = d0_0_0_5;
let e84;
let e85;
let e86;
e86 = a4;
e85 = e86["\"base\""];
let e87;
let e88;
e88 = a0;
e87 = e88["\"context\""];
e84 = { "\"code\"": e85, "\"context\"": e87 };
e82 = (e83)(e84);
a7 = e82;
}
let a8;
{
let e89;
let e90;
e90 = d0_0_0_5;
let e91;
let e92;
let e93;
e93 = a4;
e92 = e93["\"field\""];
let e94;
let e95;
e95 = a0;
e94 = e95["\"context\""];
e91 = { "\"code\"": e92, "\"context\"": e94 };
e89 = (e90)(e91);
a8 = e89;
}
{
let e96;
let e97;
e97 = a3;
let e98;
e98 = a7;
e96 = e97["push"](e98);
e81 = e96;
}
{
let e99;
let e100;
e100 = a3;
let e101;
{
let e102;
e102 = ".";
e101 = e102;
}
e99 = e100["push"](e101);
e81 = e99;
}
{
let e103;
let e104;
e104 = a3;
let e105;
e105 = a8;
e103 = e104["push"](e105);
e81 = e103;
}
e38 = e81;
break; }
case "\"t3\"": {
let e106;
{
let e107;
let e108;
e108 = a3;
let e109;
e109 = a4;
e107 = e108["push"](e109);
e106 = e107;
}
e38 = e106;
break; }
} }
e0 = e38;
}
{
let e110;
let e111;
e111 = a2;
let e112;
let e113;
e113 = a3;
let e114;
{
let e115;
e115 = "";
e114 = e115;
}
e112 = e113["join"](e114);
e110 = e111["push"](e112);
e0 = e110;
}
{
let e116;
let e117;
e117 = a2;
let e118;
{
let e119;
e119 = ";\n}\n";
e118 = e119;
}
e116 = e117["push"](e118);
e0 = e116;
}
{
let e120;
e120 = a1;
e0 = e120;
}
return e0;
};
}
var d0_0_0_6;
{
d0_0_0_6 = function(a0) {
let e0;
let a1;
{
let e1;
let e2;
{
let e3;
e3 = 0.0;
e2 = e3;
}
let e4;
e4 = [];
e1 = { "\"expr_count\"": e2, "\"out\"": e4 };
a1 = e1;
}
{
let e5;
let e6;
let e7;
e7 = a1;
e6 = e7["\"out\""];
let e8;
let e9;
e9 = d0_0_0_5;
let e10;
let e11;
e11 = a0;
let e12;
e12 = a1;
e10 = { "\"code\"": e11, "\"context\"": e12 };
e8 = (e9)(e10);
e5 = e6["push"](e8);
e0 = e5;
}
{
let e13;
let e14;
let e15;
e15 = a1;
e14 = e15["\"out\""];
let e16;
{
let e17;
e17 = "";
e16 = e17;
}
e13 = e14["join"](e16);
e0 = e13;
}
return e0;
};
}
var d0_0_0_0;
{
d0_0_0_0 = function(a1) {
let a0 = this["\"_0\""];
let e0;
e0 = a0;
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
e4 = d0_0_0_6;
let e5;
let e6;
e6 = d0_0_0_1;
let e7;
let e8;
let e9;
e9 = d0_0_0_2;
let e10;
{
let e11;
e11 = "Hello world (node)";
e10 = e11;
}
e8 = (e9)(e10);
let e12;
let e13;
e13 = d0_0_0_3;
let e14;
let e15;
let e16;
e16 = d0_0_0_4;
let e17;
{
let e18;
e18 = "console";
e17 = e18;
}
e15 = (e16)(e17);
let e19;
let e20;
e20 = d0_0_0_2;
let e21;
{
let e22;
e22 = "log";
e21 = e22;
}
e19 = (e20)(e21);
e14 = { "\"base\"": e15, "\"field\"": e19 };
e12 = (e13)(e14);
e7 = { "\"arg\"": e8, "\"func\"": e12 };
e5 = (e6)(e7);
e3 = (e4)(e5);
e1 = (e2)(e3);
e0 = e1;
}
e0;"#);
}

#[test]
fn test_0010_nodejs_helloworld() {
    setup_v8();
    let gx = new_gx([("nodejs", SourceModule::V1({
        let mb = ModuleBuilder::default();
        mb.lower(build_nodejs(&mb))
    })), ("main", SourceModule::V1({
        let mb = ModuleBuilder::default();
        mb.seq(
            vec![
                mb.bind("nodejs", mb.import(mb.str("nodejs"))),
                mb.bind("app", mb.call(mb.scope_use("nodejs"), mb.tuple(vec![]))),
                mb.call(
                    mb.field(mb.scope_use("app"), mb.str("root")),
                    mb.stage(
                        mb.call(mb.field(mb.scope_use("console"), mb.str("log")), mb.str("Hello world (node)")),
                    ),
                ),
                mb.call(mb.log(), mb.call(mb.field(mb.scope_use("app"), mb.str("build")), mb.tuple(vec![]))),
            ],
        )
    }))].into_iter().collect());
    let res = compile(gx.clone(), SourceSpecAbsolute::Virtual("main".to_string())).unwrap();
    assert_eq!(res.logs.len(), 1);
    assert_eq!(res.logs[0].message, r#"let e0;
{
let e1;
{
let e2;
{
e2 = console;
}
let e3;
{
e3 = "log";
}
e1 = e2.e3;
}
let e4;
{
e4 = "Hello world (node)";
}
e0 = (e1)(e4);
}
e0"#);
    let dynamic_sources = sanitize_builtins(&res.generated_dynamic_scripts);
    assert!(dynamic_sources.len() >= 2, "main first-pass and dynamic substage scripts emitted");
    assert!(
        dynamic_sources.iter().any(|s| s.contains("e4[\"\\\"_7\\\"\"](e5)") && s.contains("e9[\"\\\"_8\\\"\"]()")),
        "main dynamic substage calls napp.root and napp.build",
    );
}
