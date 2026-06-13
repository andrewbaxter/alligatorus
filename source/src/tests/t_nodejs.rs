//! One of the first milestones of this project is to generate a `nodejs` target
//! within the language.
//!
//! These tests break this down into:
//!
//! * Chunks of the `nodejs` target AST
//!
//! * Tests to confirm the chunks work
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

/// Build an `aspect(arg) -> option<any>` dispatch body that returns
/// `option_aspect.some(self)` when `arg.key` (a Unique) equals any of the named
/// builtin aspect ids, else `option_aspect.none(())`. The type implementing this must
/// have `impl_interface`'d each named aspect, so returning `self` is sufficient
/// for the host to invoke the aspect's methods via normal interface dispatch.
fn aspect_dispatch(mb: &ModuleBuilder, aspect_names: Vec<&str>) -> SourceExpr {
    let conditions: Vec<(SourceExpr, SourceExpr)> = aspect_names.into_iter().map(|aspect| {
        (
            mb.operator_eq(mb.field(mb.scope_use("arg"), mb.str("key")), mb.scope_use(aspect)),
            // `outer_self` = the data type instance (`this` at v8). Casting it
            // to `aspect` via `option.some` emits `__data_instance_to_aspect`
            // which captures the instance into an `AspectUserDefined` external.
            mb.call(mb.field(mb.call(mb.scope_use("option"), mb.scope_use("aspect")), mb.str("some")), mb.scope_use("outer_self")),
        )
    }).collect();
    return mb.if_(conditions, Some(aspect_dispatch_none(mb)));
}

/// `option(aspect).none(())` — the no-aspects-here dispatch body, used by types
/// that don't expose any aspects.
fn aspect_dispatch_none(mb: &ModuleBuilder) -> SourceExpr {
    return mb.call(mb.field(mb.call(mb.scope_use("option"), mb.scope_use("aspect")), mb.str("none")), mb.null());
}

fn build_nodejs(mb: &ModuleBuilder) -> SourceExpr {
    let mut exprs: Vec<SourceExpr> = Vec::new();

    // # Code AST + helpers (build_nodejs_code) and a definition set.
    exprs.push(mb.bind("code", build_nodejs_code(mb)));
    exprs.push(mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))));

    // `option` is treated here as a builtin type constructor (like `list` / `map`),
    // taking a payload type and exposing `.some` / `.none` constructors. Not exposed
    // as a scope entry yet — TODO to register it.
    exprs.push(mb.bind("root", mb.call(mb.scope_use("option"), mb.scope_use("expr"))));

    // `option_aspect` is used by the `aspect(arg)` dispatch on each type below — the
    // payload is the source-language `aspect` builtin type, which wraps a user
    // data-type instance via `__data_instance_to_aspect` into an
    // `AspectUserDefined` external.
    exprs.push(mb.bind("option_aspect", mb.call(mb.scope_use("option"), mb.scope_use("aspect"))));

    // # nvalue_base — a tagged union shared between every nodejs value, capturing how
    //
    // the value materializes into JS code:
    //
    // * `binding(name)` — references an existing JS binding (e.g. `e3`)
    //
    // * `expr(code)`    — inlines a `code` AST fragment
    exprs.push(
        mb.bind("nvalue_base", mb.call(mb.field(mb.scope_use("d"), mb.str("tagged_union")), mb.tuple(vec![]))),
    );
    exprs.push(
        mb.bind("nvalue_binding", mb.call(mb.field(mb.scope_use("nvalue_base"), mb.str("variant")), mb.tuple(vec![
            //. .
            mb.str("binding"),
            mb.scope_use("string"),
        ]))),
    );
    exprs.push(
        mb.bind("nvalue_expr", mb.call(mb.field(mb.scope_use("nvalue_base"), mb.str("variant")), mb.tuple(vec![
            //. .
            mb.str("expr"),
            mb.field(mb.scope_use("code"), mb.str("type")),
        ]))),
    );

    // # nvalue — universal carrier for every nodejs value: `(base, type)` where
    //
    // `type` is the nodejs type the value has at use-time. nvalue's `value.type`
    // returns `self.type`, so aspect lookups on an nvalue dispatch through that
    // contained type (ntype_str / ntype_func / ntype_object / ...).
    exprs.push(mb.bind("nvalue", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs value"))));
    exprs.push(mb.call(mb.field(mb.scope_use("nvalue"), mb.str("set_underlying")), mb.record(vec![
        //. .
        ("base", mb.field(mb.scope_use("nvalue_base"), mb.str("type"))),
        // `type` field stores the value's source-language type as `any`
        // (= `Rc<dyn Type>` at runtime). This makes `nvalue.new({type: T})`
        // accept any type-bearing value `T` uniformly — both concrete
        // ntype_object instances (via `TypeAnyDynamic::cast_from`) and
        // already-`any`-typed types like an `nscope_entry`'s stored type.
        ("type", mb.scope_use("any")),
    ])));
    exprs.push(
        impl_iface(mb, "nvalue", "value", vec![("type", mb.field(mb.scope_use("self"), mb.str("type")))]),
    );

    // # aspect_consume_nodejs — source-level `is_aspect` interface.
    exprs.push(
        mb.bind(
            "aspect_consume_nodejs",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_interface")), mb.tuple(vec![])),
        ),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("set_is_aspect")),
            mb.bool(true),
        ),
    );
    exprs.push(
        mb.bind(
            "acn_to_code",
            mb.call(
                mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("new_func")),
                mb.str("to_code"),
            ),
        ),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("acn_to_code"), mb.str("arg")),
            mb.field(mb.scope_use("nvalue"), mb.str("type")),
        ),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("acn_to_code"), mb.str("ret")),
            mb.scope_use("any"),
        ),
    );

    // # ntype_str — nodejs string type. Its instances are types.
    exprs.push(
        mb.bind("ntype_str", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs string"))),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("ntype_str"), mb.str("set_underlying")), mb.tuple(vec![])));
    exprs.extend(impl_as_self_type(mb, "ntype_str", "nodejs string", aspect_dispatch_none(mb)));
    exprs.push(mb.call(
        mb.field(mb.scope_use("ntype_str"), mb.str("impl_interface")),
        mb.tuple(vec![
            mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("type")),
            iface_methods(mb, vec![(
                "to_code",
                nvalue_base_to_code(mb, mb.field(mb.scope_use("arg"), mb.str("base"))),
            )]),
        ]),
    ));

    // # ntype_func — nodejs function type `(arg, ret)`. Implements `aspect_function`
    //
    // so calling an nvalue of this type produces a `code.call` AST node wrapped in a
    // new nvalue whose type is `self.ret`.
    exprs.push(
        mb.bind(
            "ntype_func",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs function")),
        ),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("ntype_func"), mb.str("set_underlying")), mb.record(vec![
        //. .
        ("arg", mb.scope_use("type")),
        ("ret", mb.scope_use("type")),
    ])));
    exprs.extend(
        impl_as_self_type(mb, "ntype_func", "nodejs function", aspect_dispatch(mb, vec!["aspect_function_id"])),
    );

    // aspect_function.call — `arg` carries `{self_, argument}` as CodeValueExterns
    // (the receiver and argument CVs) plus `xx`. Returns a CodeValueExtern (the
    // result CV). The body uses `new_code_value(code, value)` to explicitly
    // construct that CV — same shape as the Rust `AspectFunction::call` impl.
    //
    // TODO(code-arg): pass the real test-`code` AST as the code arg once the
    // test's `code` tagged union impls the `code_dyn` builtin interface (so
    // it casts to `codedyn`). For now the code arg is `option(codedyn).none(())`
    // — produces a `Code_::None` result, which doesn't match the test's
    // expected JS output but lets the impl compile against the new contract.
    exprs.push(impl_iface(mb, "ntype_func", "aspect_function", vec![("call", mb.call(
        mb.scope_use("new_code_value"),
        mb.tuple(vec![
            // code arg: option(codedyn).none(())
            mb.call(
                mb.field(
                    mb.call(mb.scope_use("option"), mb.scope_use("codedyn")),
                    mb.str("none"),
                ),
                mb.null(),
            ),
            // value arg: original nvalue construction; auto-casts TSDT(nvalue) →
            // value via __data_instance_to_value (nvalue impls the value iface).
            mb.block(vec![
                // func_code: cast .value to nvalue so .base falls through.
                mb.bind(
                    "func_code",
                    nvalue_base_to_code(
                        mb,
                        mb.field(
                            mb.as_(
                                mb.field(
                                    mb.field(mb.scope_use("arg"), mb.str("self_")),
                                    mb.str("value"),
                                ),
                                mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("type"))),
                            ),
                            mb.str("base"),
                        ),
                    ),
                ),
                // arg_code: aspect-based lookup + dispatch via aspect_lookup.
                mb.bind(
                    "arg_code",
                    mb.match_(
                        mb.call(mb.scope_use("aspect_lookup"), mb.tuple(vec![
                            mb.field(
                                mb.field(
                                    mb.field(mb.scope_use("arg"), mb.str("argument")),
                                    mb.str("value"),
                                ),
                                mb.str("type"),
                            ),
                            mb.lower(mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("type"))),
                        ])),
                        mb.str("v"),
                        vec![
                            (
                                mb.str("some"),
                                mb.call(
                                    mb.field(mb.scope_use("v"), mb.str("to_code")),
                                    mb.as_(
                                        mb.field(
                                            mb.field(mb.scope_use("arg"), mb.str("argument")),
                                            mb.str("value"),
                                        ),
                                        mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("type"))),
                                    ),
                                ),
                            ),
                            (mb.str("none"), mb.scope_use("null")),
                        ],
                        None,
                    ),
                ),
                mb.call(
                    mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("new"))),
                    mb.record(
                        vec![
                            (
                                "base",
                                mb.call(
                                    mb.lower(mb.scope_use("nvalue_expr")),
                                    mb.call(
                                        mb.lower(mb.field(mb.scope_use("code"), mb.str("call"))),
                                        mb.record(
                                            vec![("func", mb.scope_use("func_code")), ("arg", mb.scope_use("arg_code"))],
                                        ),
                                    ),
                                ),
                            ),
                            ("type", mb.field(mb.scope_use("self"), mb.str("ret"))),
                        ],
                    ),
                ),
            ]),
        ]),
    ))]));

    // # ntype_object — nodejs object type, parameterized by `map(string, type)`. Its
    //
    // instances are types. Implements `aspect_field` (look up a field by name).
    //
    // Scope initialization (turning a type + a binding name into a ScopeEntry) is
    // done by the napp.build code below directly constructing nvalues — not via
    // `aspect_initial_bind`, which is dynamic-/v8-specific.
    exprs.push(
        mb.bind(
            "ntype_object",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs object")),
        ),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("ntype_object"), mb.str("set_underlying")),
            mb.call(mb.scope_use("map"), mb.tuple(vec![mb.scope_use("string"), mb.scope_use("type")])),
        ),
    );
    exprs.extend(
        impl_as_self_type(mb, "ntype_object", "nodejs object", aspect_dispatch(mb, vec!["aspect_field_id"])),
    );

    // aspect_field.field — same contract as aspect_function.call. arg = {self_,
    // key, xx} CodeValueExterns/ExprContextRef. Returns CodeValueExtern via
    // new_code_value. TODO(code-arg): real codedyn arg once test's `code`
    // impls code_dyn iface.
    exprs.push(
        impl_iface(
            mb,
            "ntype_object",
            "aspect_field",
            vec![
                ("field", mb.call(
                    mb.scope_use("new_code_value"),
                    mb.tuple(vec![
                        // code arg: option(codedyn).none(())
                        mb.call(
                            mb.field(
                                mb.call(mb.scope_use("option"), mb.scope_use("codedyn")),
                                mb.str("none"),
                            ),
                            mb.null(),
                        ),
                        // value arg: original nvalue construction (auto-casts to value)
                        mb.block(vec![
                            mb.bind(
                                "base_code",
                                nvalue_base_to_code(
                                    mb,
                                    mb.field(
                                        mb.as_(
                                            mb.field(
                                                mb.field(mb.scope_use("arg"), mb.str("self_")),
                                                mb.str("value"),
                                            ),
                                            mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("type"))),
                                        ),
                                        mb.str("base"),
                                    ),
                                ),
                            ),
                            mb.call(mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("new"))), mb.record(vec![
                                (
                                    "base",
                                    mb.call(
                                        mb.lower(mb.scope_use("nvalue_expr")),
                                        mb.call(
                                            mb.lower(mb.field(mb.scope_use("code"), mb.str("field"))),
                                            mb.record(
                                                vec![
                                                    ("base", mb.scope_use("base_code")),
                                                    (
                                                        "field",
                                                        mb.call(
                                                            mb.lower(mb.field(mb.scope_use("code"), mb.str("lit_str"))),
                                                            mb.as_(mb.field(mb.scope_use("arg"), mb.str("key")), mb.scope_use("string")),
                                                        ),
                                                    ),
                                                ],
                                            ),
                                        ),
                                    ),
                                ),
                                (
                                    "type",
                                    mb.call(
                                        mb.field(mb.scope_use("self"), mb.str("get")),
                                        mb.as_(mb.field(mb.scope_use("arg"), mb.str("key")), mb.scope_use("string")),
                                    ),
                                ),
                            ])),
                        ]),
                    ]),
                )),
                (
                    "field_type_opt",
                    mb.call(
                        mb.field(mb.scope_use("self"), mb.str("get")),
                        mb.as_(mb.field(mb.scope_use("arg"), mb.str("key")), mb.scope_use("string")),
                    ),
                ),
            ],
        ),
    );

    // # nscope_entry — what a `new_expr_context` scope record maps names to.
    //
    // The dynamic target uses Rust-side `ScopeEntry` trait objects with an
    // `aspect_initial_bind` flow; nodejs needs its own equivalent. An nscope_entry
    // wraps an nvalue (the value the binding resolves to) and is produced directly
    // when the scope is populated — no `aspect_initial_bind` dance. When `const_eval`
    // encounters a `scope_use` and looks up the entry, it calls
    // `use_(loc) -> nvalue`, which just hands back the wrapped value (cf.
    // `TypeUnique`'s `AspectBind` impl in Rust).
    //
    // TODO: a builtin `scope_entry` interface (mirroring the Rust trait) doesn't
    // exist yet — see open work on that. For now the impl is sketched out under a
    // placeholder interface name.
    exprs.push(
        mb.bind(
            "nscope_entry",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs scope entry")),
        ),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("nscope_entry"), mb.str("set_underlying")),
            mb.record(vec![
                ("name", mb.scope_use("string")),
                ("type", mb.scope_use("any")),
            ]),
        ),
    );
    // `use_` constructs a fresh `nvalue` at v8 time, with `base =
    // nvalue_binding(self.name)` and `type = self.type` — both drawn from
    // the entry's underlying. The new nvalue's value-interface return then
    // lands in Rust as a `ValueUserDefined` (since `scope_entry.use_`'s
    // ret_type is `TypeValueDynamic`).
    exprs.push(
        impl_iface(
            mb,
            "nscope_entry",
            "scope_entry",
            vec![(
                "use_",
                mb.call(
                    mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("new"))),
                    mb.record(vec![
                        (
                            "base",
                            mb.call(
                                mb.lower(mb.scope_use("nvalue_binding")),
                                mb.field(mb.scope_use("self"), mb.str("name")),
                            ),
                        ),
                        ("type", mb.field(mb.scope_use("self"), mb.str("type"))),
                    ]),
                ),
            )],
        ),
    );

    // # ntarget — the nodejs target. Implements the builtin `target` interface so
    //
    // that `const_eval` with an ntarget instance as `target` walks the staged AST
    // through these method bodies, accumulating nodejs `code` instead of v8 JS.
    //
    // Bodies below are intentionally minimal: each constructs the smallest plausible
    // `code` fragment, enough to thread `console.log("Hello world (node)")` through
    // const_eval. They mirror the shape of the Rust `TargetDynamic` impl but produce
    // nodejs `code` AST nodes instead of `CodeDynamic`.
    exprs.push(
        mb.bind("ntarget", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs target"))),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("ntarget"), mb.str("set_underlying")), mb.scope_use("null")));
    exprs.push(impl_iface(mb, "ntarget", "target", vec![
        // bind(xx, loc, sub) -> (Code, ScopeEntryId)
        //
        // The dynamic target emits `let aN; { ... aN = code; }` and returns the fresh
        // binding's ScopeEntryId. For nodejs we do the same shape — using
        // `code.scope_use(name)` as the rebound code, and threading the generated name
        // via `gen_binding_name` (assumed compiler helper).
        (
            "bind",
            mb.block(
                vec![
                    mb.bind(
                        "name",
                        mb.call(mb.scope_use("gen_binding_name"), mb.field(mb.scope_use("arg"), mb.str("xx"))),
                    ),
                    mb.tuple(
                        vec![
                            mb.call(
                                mb.lower(mb.field(mb.scope_use("code"), mb.str("scope_use"))),
                                mb.scope_use("name"),
                            ),
                            mb.scope_use("name"),
                        ],
                    ),
                ],
            ),
        ),
        // bind_sub_record(loc, carry, fields) -> TargetBindSub
        //
        // The dynamic target produces a record literal `{f1: ..., f2: ...}`. We build the
        // equivalent in nodejs as a record carried through the bind sub so it can be
        // materialized later when a real binding is requested.
        (
            "bind_sub_record",
            mb.record(
                vec![
                    ("carry", mb.field(mb.scope_use("arg"), mb.str("carry"))),
                    ("fields", mb.field(mb.scope_use("arg"), mb.str("fields"))),
                ],
            ),
        ),
        // bind_sub_tuple(loc, carry, elements) -> TargetBindSub
        (
            "bind_sub_tuple",
            mb.record(
                vec![
                    ("carry", mb.field(mb.scope_use("arg"), mb.str("carry"))),
                    ("elements", mb.field(mb.scope_use("arg"), mb.str("elements"))),
                ],
            ),
        ),
        // block(loc, entries) -> Code
        //
        // Concatenate the entries' code; the last entry's value (if any) is the block's
        // result. For nodejs we use a `code.expr_sequence` constructor (assumed; falls
        // back to the last entry if only one is given).
        ("block", mb.field(mb.scope_use("arg"), mb.str("entries"))),
        // code_to_bind_sub(loc, code, type_) -> TargetBindSub
        (
            "code_to_bind_sub",
            mb.record(
                vec![
                    ("code", mb.field(mb.scope_use("arg"), mb.str("code"))),
                    ("type", mb.field(mb.scope_use("arg"), mb.str("type"))),
                ],
            ),
        ),
        // if_(xx, loc, conditions, else_) -> EvalRes
        //
        // TODO: emit a `code.if` AST. The helloworld test doesn't exercise this.
        ("if_", mb.scope_use("null")),
        // match_(xx, loc, base, element_key, cases, fallback) -> EvalRes
        //
        // TODO: emit a `code.switch` AST. The helloworld test doesn't exercise this.
        ("match_", mb.scope_use("null")),
        // maybe_bind_sub(xx, loc, cv) -> Option`<TargetBindSub>`
        //
        // The dynamic target wraps the cv's code into a bind sub if it's CodeDynamic;
        // here every cv carries nodejs code so we always succeed.
        (
            "maybe_bind_sub",
            mb.call(
                mb.lower(mb.field(mb.scope_use("root"), mb.str("some"))),
                mb.field(mb.scope_use("arg"), mb.str("cv")),
            ),
        ),
        // stage(xx, loc, tree, lower_values) -> EvalRes
        //
        // For nodejs we never need to re-stage (the compile-time `stage` machinery is
        // handled by the host v8 isolate), so return null.
        ("stage", mb.scope_use("null")),
    ]));

    // # napp — the nodejs app object. Holds the staged root expression (as
    //
    // `option<expr>`) and exposes `root(expr)` / `build()` methods.
    exprs.push(mb.bind("napp", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs app"))));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("napp"), mb.str("set_underlying")),
            mb.record(vec![("root", mb.scope_use("root"))]),
        ),
    );

    // napp.root(expr) — stash `arg` into `self.root` as `option.some(arg)`.
    exprs.push(mb.bind("napp_root", mb.call(mb.field(mb.scope_use("napp"), mb.str("new_func")), mb.str("root"))));
    exprs.push(mb.call(mb.field(mb.scope_use("napp_root"), mb.str("arg")), mb.scope_use("expr")));
    exprs.push(mb.call(mb.field(mb.scope_use("napp_root"), mb.str("ret")), mb.scope_use("null")));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("napp_root"), mb.str("impl")),
            mb.stage(
                mb.set(
                    mb.field(mb.scope_use("self"), mb.str("root")),
                    mb.call(mb.lower(mb.field(mb.scope_use("root"), mb.str("some"))), mb.scope_use("arg")),
                ),
            ),
        ),
    );

    // napp.build() — match on `self.root`. For `some(expr)`, const_eval the staged
    // expression under a fresh ExprContext with an ntarget instance as target and a
    // scope pre-populated with `console` (an nvalue referencing the JS global binding
    // `console`, typed as a nodejs object with a `log` field of type
    // `string -> string`). Then `code.render(...)` the resulting CodeValue.code.
    exprs.push(mb.bind("napp_build", mb.call(mb.field(mb.scope_use("napp"), mb.str("new_func")), mb.str("build"))));
    exprs.push(mb.call(mb.field(mb.scope_use("napp_build"), mb.str("arg")), mb.tuple(vec![])));
    exprs.push(mb.call(mb.field(mb.scope_use("napp_build"), mb.str("ret")), mb.scope_use("string")));
    exprs.push(mb.call(mb.field(mb.scope_use("napp_build"), mb.str("impl")), mb.stage(mb.block(vec![
        //. .
        mb.match_(
            mb.field(mb.scope_use("self"), mb.str("root")),
            mb.str("root"),
            vec![(mb.str("some"), mb.block(vec![
                // Build a nodejs `string -> string` function type for `console.log`.
                mb.bind(
                    "ntype_str_inst",
                    mb.call(mb.lower(mb.field(mb.scope_use("ntype_str"), mb.str("new"))), mb.tuple(vec![])),
                ),
                mb.bind(
                    "ntype_log",
                    mb.call(
                        mb.lower(mb.field(mb.scope_use("ntype_func"), mb.str("new"))),
                        mb.record(
                            vec![("arg", mb.scope_use("ntype_str_inst")), ("ret", mb.scope_use("ntype_str_inst"))],
                        ),
                    ),
                ),
                // Build the `console` nodejs object type: `{ log: ntype_log }`.
                mb.bind(
                    "ntype_console",
                    mb.call(
                        mb.lower(mb.field(mb.scope_use("ntype_object"), mb.str("new"))),
                        mb.call(
                            mb.field(
                                mb.call(
                                    mb.scope_use("map"),
                                    mb.tuple(vec![mb.scope_use("string"), mb.scope_use("type")]),
                                ),
                                mb.str("new"),
                            ),
                            mb.record(vec![("log", mb.scope_use("ntype_log"))]),
                        ),
                    ),
                ),
                // Wrap the `console` binding name + type as an `nscope_entry`; the
                // entry's `use_` constructs the actual nvalue (with
                // `base = nvalue_binding(name)`, `type = type`) at v8 time when
                // `console` is resolved from scope.
                mb.bind(
                    "console_entry",
                    mb.call(
                        mb.lower(mb.field(mb.scope_use("nscope_entry"), mb.str("new"))),
                        mb.record(vec![
                            ("name", mb.str("console")),
                            ("type", mb.scope_use("ntype_console")),
                        ]),
                    ),
                ),
                // const_eval the staged root with an ntarget + a scope containing `console`.
                mb.bind(
                    "res",
                    mb.call(
                        mb.scope_use("const_eval"),
                        mb.tuple(
                            vec![
                                mb.call(
                                    mb.scope_use("new_expr_context"),
                                    mb.tuple(
                                        vec![
                                            mb.call(
                                                mb.lower(mb.field(mb.scope_use("ntarget"), mb.str("new"))),
                                                mb.null(),
                                            ),
                                            mb.record(vec![("console", mb.scope_use("console_entry"))]),
                                        ],
                                    ),
                                ),
                                mb.scope_use("root"),
                            ],
                        ),
                    ),
                ),
                // Render the resulting code AST as a JS source string.
                // `res.cv.code.code` is a `Code_` tagged union with variants
                // `none` (no code emitted) and `dyn(handle)` (a `code_dyn`
                // handle); the `dyn` variant is what the nodejs target's
                // code-emitting impls produce, and we cast it to the nodejs
                // `code` tagged union before rendering.
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
                                mb.lower(mb.field(mb.scope_use("code"), mb.str("render"))),
                                mb.call(
                                    mb.field(mb.scope_use("v"), mb.str("cast")),
                                    mb.lower(mb.field(mb.scope_use("code"), mb.str("type"))),
                                ),
                            ),
                        ),
                        (mb.str("none"), mb.str("")),
                    ],
                    None,
                ),
            ])), (mb.str("none"), mb.block(vec![mb.str("")]))],
            None,
        ),
    ]))));

    // # nodejs() — public constructor that returns a fresh napp with `root` set to
    //
    // `option.none(())`.
    exprs.push(mb.bind("nodejs", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("nodejs"))));
    exprs.push(mb.call(mb.field(mb.scope_use("nodejs"), mb.str("arg")), mb.tuple(vec![])));
    exprs.push(
        mb.call(mb.field(mb.scope_use("nodejs"), mb.str("ret")), mb.field(mb.scope_use("napp"), mb.str("type"))),
    );
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("nodejs"), mb.str("impl")),
            mb.stage(
                mb.call(
                    mb.lower(mb.field(mb.scope_use("napp"), mb.str("new"))),
                    mb.record(
                        vec![
                            (
                                "root",
                                mb.call(mb.lower(mb.field(mb.scope_use("root"), mb.str("none"))), mb.null()),
                            ),
                        ],
                    ),
                ),
            ),
        ),
    );
    exprs.push(mb.field(mb.scope_use("nodejs"), mb.str("func")));
    return mb.block(exprs);
}

fn build_nodejs_code(mb: &ModuleBuilder) -> SourceExpr {
    return mb.block(vec![
        mb.bind("d", mb.call(mb.scope_use("define"), mb.tuple(vec![]))),
        // # Define "code" tagged union type
        mb.bind("code_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("tagged_union")), mb.tuple(vec![]))),
        mb.bind("code", mb.field(mb.scope_use("code_builder"), mb.str("type"))),
        mb.bind("code_call", mb.call(mb.field(mb.scope_use("code_builder"), mb.str("variant")), mb.tuple(vec![
            //. .
            mb.str("call"),
            mb.record(vec![("func", mb.scope_use("code")), ("arg", mb.scope_use("code"))]),
        ]))),
        mb.bind(
            "code_lit_str",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("lit_str"), mb.scope_use("string")]),
            ),
        ),
        mb.bind("code_field", mb.call(mb.field(mb.scope_use("code_builder"), mb.str("variant")), mb.tuple(vec![
            //. .
            mb.str("field"),
            mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("code"))]),
        ]))),
        mb.bind(
            "code_scope_use",
            mb.call(
                mb.field(mb.scope_use("code_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("scope_use"), mb.scope_use("string")]),
            ),
        ),
        // # Define render-code function (recursive inner version)
        mb.bind(
            "render_inner_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("Render Node.js code (inner)")),
        ),
        mb.bind("render_inner", mb.field(mb.scope_use("render_inner_builder"), mb.str("func"))),
        mb.call(mb.field(mb.scope_use("render_inner_builder"), mb.str("arg")), mb.record(vec![
            //. .
            ("context", mb.record(vec![
                //. .
                ("expr_count", mb.scope_use("number")),
                ("out", mb.call(mb.scope_use("list"), mb.scope_use("string"))),
            ])),
            ("code", mb.scope_use("code")),
        ])),
        mb.call(mb.field(mb.scope_use("render_inner_builder"), mb.str("ret")), mb.scope_use("string")),
        mb.call(mb.field(mb.scope_use("render_inner_builder"), mb.str("impl")), mb.stage(mb.block(vec![
            //. .
            mb.bind(
                "key",
                mb.call(
                    mb.field(
                        mb.call(
                            mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                            mb.tuple(vec![
                                //. .
                                mb.str("e"),
                                mb.call(
                                    mb.field(
                                        mb.field(
                                            mb.field(mb.scope_use("arg"), mb.str("context")),
                                            mb.str("expr_count"),
                                        ),
                                        mb.str("to_string"),
                                    ),
                                    mb.tuple(vec![]),
                                ),
                            ]),
                        ),
                        mb.str("join"),
                    ),
                    mb.str(""),
                ),
            ),
            mb.set(
                mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("expr_count")),
                mb.operator_add(
                    mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("expr_count")),
                    mb.num(1.),
                ),
            ),
            mb.bind("out", mb.field(mb.field(mb.scope_use("arg"), mb.str("context")), mb.str("out"))),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str("let ")),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.scope_use("key")),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n{\n")),
            mb.bind(
                "value",
                mb.call(
                    mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                    mb.tuple(vec![mb.scope_use("key"), mb.str(" = ")]),
                ),
            ),
            mb.match_(mb.field(mb.scope_use("arg"), mb.str("code")), mb.str("code"), vec![
                //. .
                (mb.str("call"), mb.block(vec![
                    //. .
                    mb.bind("func", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("func"))),
                    ]))),
                    mb.bind("rendered_arg", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("arg"))),
                    ]))),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str("(")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("func")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(")(")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("rendered_arg")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(")")),
                ])),
                (mb.str("lit_str"), mb.block(vec![
                    //. .
                    mb.call(
                        mb.field(mb.scope_use("value"), mb.str("push")),
                        mb.call(mb.scope_use("str_to_json"), mb.scope_use("code")),
                    ),
                ])),
                (mb.str("field"), mb.block(vec![
                    //. .
                    mb.bind("base", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("base"))),
                    ]))),
                    mb.bind("field", mb.call(mb.lower(mb.scope_use("render_inner")), mb.record(vec![
                        //. .
                        ("context", mb.field(mb.scope_use("arg"), mb.str("context"))),
                        ("code", mb.field(mb.scope_use("code"), mb.str("field"))),
                    ]))),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("base")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.str(".")),
                    mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("field")),
                ])),
                (
                    mb.str("scope_use"),
                    mb.block(
                        vec![mb.call(mb.field(mb.scope_use("value"), mb.str("push")), mb.scope_use("code"))],
                    ),
                ),
            ], None),
            mb.call(
                mb.field(mb.scope_use("out"), mb.str("push")),
                mb.call(mb.field(mb.scope_use("value"), mb.str("join")), mb.str("")),
            ),
            mb.call(mb.field(mb.scope_use("out"), mb.str("push")), mb.str(";\n}\n")),
            mb.scope_use("key"),
        ]))),
        // Define render-code func (outer)
        mb.bind(
            "render_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("Render Node.js code")),
        ),
        mb.bind("render", mb.field(mb.scope_use("render_builder"), mb.str("func"))),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("arg")), mb.scope_use("code")),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("ret")), mb.scope_use("string")),
        mb.call(mb.field(mb.scope_use("render_builder"), mb.str("impl")), mb.stage(mb.block(vec![
            //. .
            mb.bind("context", mb.record(vec![
                //. .
                (
                    "out",
                    mb.call(
                        mb.field(mb.call(mb.scope_use("list"), mb.scope_use("string")), mb.str("new")),
                        mb.tuple(vec![]),
                    ),
                ),
                ("expr_count", mb.as_(mb.num(0.), mb.scope_use("number"))),
            ])),
            mb.call(
                mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("push")),
                mb.call(
                    mb.lower(mb.scope_use("render_inner")),
                    mb.record(vec![("context", mb.scope_use("context")), ("code", mb.scope_use("arg"))]),
                ),
            ),
            mb.call(mb.field(mb.field(mb.scope_use("context"), mb.str("out")), mb.str("join")), mb.str("")),
        ]))),
        // Assemble and return
        mb.record(vec![
            //. .
            ("type", mb.scope_use("code")),
            ("call", mb.scope_use("code_call")),
            ("lit_str", mb.scope_use("code_lit_str")),
            ("field", mb.scope_use("code_field")),
            ("scope_use", mb.scope_use("code_scope_use")),
            ("render", mb.scope_use("render")),
        ]),
    ]);
}

/// Build a `map(string, expr).new({...})` literal — used as the implementations
/// record by `impl_interface`. Each method body is wrapped in `mb.stage(...)`
/// before being placed in the record.
fn iface_methods(mb: &ModuleBuilder, methods: Vec<(&str, SourceExpr)>) -> SourceExpr {
    let entries: Vec<(&str, SourceExpr)> = methods.into_iter().map(|(k, v)| (k, mb.stage(v))).collect();
    return mb.call(
        mb.field(
            mb.call(mb.scope_use("map"), mb.tuple(vec![mb.scope_use("string"), mb.scope_use("expr")])),
            mb.str("new"),
        ),
        mb.record(entries),
    );
}

/// Make a data type also be a _type_: its instances become values whose type is
/// themselves, and they satisfy the builtin `type` interface. Mirrors the pattern
/// in `t0120_impl_type` but also includes the `aspect` method — without it, aspect
/// lookups on these types always return `None`, so impls like
/// `aspect_function.call` / `aspect_field.field` would never be reached.
///
/// `aspect_dispatch` is the body for `aspect(arg) -> option<aspect_impl>`: given
/// `arg.key` (a Unique), it should return `option.some(<impl>)` if the type
/// implements that aspect, or `option.none(())` otherwise. Real dispatch needs to
/// compare `arg.key` against builtin aspect ids, which currently has no operator —
/// see open TODO about adding `operator_eq`.
fn impl_as_self_type(
    mb: &ModuleBuilder,
    target_var: &str,
    desc: &str,
    aspect_dispatch: SourceExpr,
) -> Vec<SourceExpr> {
    return vec![
        impl_iface(mb, target_var, "value", vec![("type", mb.scope_use("self"))]),
        impl_iface(
            mb,
            target_var,
            "type",
            vec![
                ("can_cast_from", mb.bool(false)),
                ("cast_from", mb.field(mb.scope_use("arg"), mb.str("cv"))),
                ("desc", mb.str(desc)),
                ("aspect", aspect_dispatch),
            ],
        ),
    ];
}

/// `data_type.impl_interface(iface, map(string,expr).new({ name: stage(body), ... }))`.
fn impl_iface(mb: &ModuleBuilder, target_var: &str, iface: &str, methods: Vec<(&str, SourceExpr)>) -> SourceExpr {
    return mb.call(
        mb.field(mb.scope_use(target_var), mb.str("impl_interface")),
        mb.tuple(vec![mb.scope_use(iface), iface_methods(mb, methods)]),
    );
}

/// Convert an `nvalue_base` (tagged union of `binding(string)` / `expr(code)`)
/// into a `code` AST node. `base_expr` should evaluate to an `nvalue_base`. `code`
/// from the outer scope is lowered in for the `scope_use` constructor. The
/// argument is cast to `nvalue_base` first — at impl-compile time the static
/// type may be `any` (the receiver's underlying field type isn't inferred).
fn nvalue_base_to_code(mb: &ModuleBuilder, base_expr: SourceExpr) -> SourceExpr {
    return mb.match_(
        mb.as_(base_expr, mb.lower(mb.field(mb.scope_use("nvalue_base"), mb.str("type")))),
        mb.str("v"),
        vec![
            (
                mb.str("binding"),
                mb.call(mb.lower(mb.field(mb.scope_use("code"), mb.str("scope_use"))), mb.scope_use("v")),
            ),
            (mb.str("expr"), mb.scope_use("v")),
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
                        mb.block(
                            vec![
                                mb.bind("code", mb.lower(build_nodejs_code(&mb))),
                                mb.call(
                                    mb.scope_use("log"),
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
    let js_sources = sanitize_builtins(&res.generated_mortar_scripts);
    assert_eq!(js_sources.len(), 2);
    assert_eq!(
        js_sources[0],
        r#"let e0;
let e1;
e1 = BUILTIN;
let e2;
e2 = a23;
let e3;
e3 = a24;
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
e7 = e8["_3"]();
a1 = e7;
}
let a2;
{
let e9;
let e10;
e10 = a1;
e9 = e10["_0"];
a2 = e9;
}
let a3;
{
let e11;
let e12;
e12 = a1;
let e13;
{
let e14;
e14 = "call";
e13 = e14;
}
let e15;
let e16;
e16 = BUILTIN;
let e17;
let e18;
e18 = a2;
let e19;
e19 = a2;
e17 = { "\"arg\"": e18, "\"func\"": e19 };
e15 = (e16)(e17);
e11 = e12["_1"](e13,e15);
a3 = e11;
}
let a5;
{
let e20;
let e21;
e21 = a1;
let e22;
{
let e23;
e23 = "lit_str";
e22 = e23;
}
let e24;
e24 = a4;
e20 = e21["_1"](e22,e24);
a5 = e20;
}
let a6;
{
let e25;
let e26;
e26 = a1;
let e27;
{
let e28;
e28 = "field";
e27 = e28;
}
let e29;
let e30;
e30 = BUILTIN;
let e31;
let e32;
e32 = a2;
let e33;
e33 = a2;
e31 = { "\"base\"": e32, "\"field\"": e33 };
e29 = (e30)(e31);
e25 = e26["_1"](e27,e29);
a6 = e25;
}
let a8;
{
let e34;
let e35;
e35 = a1;
let e36;
{
let e37;
e37 = "scope_use";
e36 = e37;
}
let e38;
e38 = a7;
e34 = e35["_1"](e36,e38);
a8 = e34;
}
let a9;
{
let e39;
let e40;
e40 = a0;
let e41;
{
let e42;
e42 = "Render Node.js code (inner)";
e41 = e42;
}
e39 = e40["_0"](e41);
a9 = e39;
}
let a10;
{
let e43;
let e44;
e44 = a9;
e43 = e44["_0"];
a10 = e43;
}
{
let e45;
let e46;
e46 = a9;
let e47;
let e48;
e48 = BUILTIN;
let e49;
let e50;
e50 = a2;
let e51;
e51 = a11;
e49 = { "\"code\"": e50, "\"context\"": e51 };
e47 = (e48)(e49);
e45 = e46["_3"](e47);
e4 = e45;
}
{
let e52;
let e53;
e53 = a9;
let e54;
e54 = a12;
e52 = e53["_5"](e54);
e4 = e52;
}
{
let e55;
let e56;
e56 = a9;
let e57;
let e58;
e58 = BUILTIN;
let e59;
e59 = a13;
let e60;
e60 = a14;
let e61;
e61 = a10;
let e62;
e62 = a15;
let e63;
e63 = a10;
let e64;
e64 = a16;
let e65;
e65 = a10;
let e66;
e66 = a17;
let e67;
e67 = a10;
e57 = (e58)(e59,e60,e61,e62,e63,e64,e65,e66,e67);
e55 = e56["_2"](e57);
e4 = e55;
}
let a18;
{
let e68;
let e69;
e69 = a0;
let e70;
{
let e71;
e71 = "Render Node.js code";
e70 = e71;
}
e68 = e69["_0"](e70);
a18 = e68;
}
let a19;
{
let e72;
let e73;
e73 = a18;
e72 = e73["_0"];
a19 = e72;
}
{
let e74;
let e75;
e75 = a18;
let e76;
let e77;
e77 = BUILTIN;
let e78;
e78 = a2;
e76 = (e77)(e78);
e74 = e75["_3"](e76);
e4 = e74;
}
{
let e79;
let e80;
e80 = a18;
let e81;
e81 = a20;
e79 = e80["_5"](e81);
e4 = e79;
}
{
let e82;
let e83;
e83 = a18;
let e84;
let e85;
e85 = BUILTIN;
let e86;
e86 = a21;
let e87;
e87 = a22;
let e88;
e88 = a10;
e84 = (e85)(e86,e87,e88);
e82 = e83["_2"](e84);
e4 = e82;
}
{
let e89;
let e90;
e90 = a3;
let e91;
e91 = a6;
let e92;
e92 = a5;
let e93;
e93 = a19;
let e94;
e94 = a8;
let e95;
e95 = a2;
e89 = { "\"call\"": e90, "\"field\"": e91, "\"lit_str\"": e92, "\"render\"": e93, "\"scope_use\"": e94, "\"type\"": e95 };
e4 = e89;
}
e0 = (e1)(e2,e3,e4);
e0;"#
    );
    assert_eq!(js_sources[1], r#"var d0_0_0;
{
d0_0_0 = function(a0) {
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
e40 = a0;
e39 = e40["\"code\""];
{ let [__tag, a4] = Object.entries(e39)[0];
switch (__tag) {
case "\"t0\"": {
let e41;
let a5;
{
let e42;
let e43;
e43 = d0_0_0;
let e44;
let e45;
let e46;
e46 = a4;
e45 = e46["\"func\""];
let e47;
let e48;
e48 = a0;
e47 = e48["\"context\""];
e44 = { "\"code\"": e45, "\"context\"": e47 };
e42 = (e43)(e44);
a5 = e42;
}
let a6;
{
let e49;
let e50;
e50 = d0_0_0;
let e51;
let e52;
let e53;
e53 = a4;
e52 = e53["\"arg\""];
let e54;
let e55;
e55 = a0;
e54 = e55["\"context\""];
e51 = { "\"code\"": e52, "\"context\"": e54 };
e49 = (e50)(e51);
a6 = e49;
}
{
let e56;
let e57;
e57 = a3;
let e58;
{
let e59;
e59 = "(";
e58 = e59;
}
e56 = e57["push"](e58);
e41 = e56;
}
{
let e60;
let e61;
e61 = a3;
let e62;
e62 = a5;
e60 = e61["push"](e62);
e41 = e60;
}
{
let e63;
let e64;
e64 = a3;
let e65;
{
let e66;
e66 = ")(";
e65 = e66;
}
e63 = e64["push"](e65);
e41 = e63;
}
{
let e67;
let e68;
e68 = a3;
let e69;
e69 = a6;
e67 = e68["push"](e69);
e41 = e67;
}
{
let e70;
let e71;
e71 = a3;
let e72;
{
let e73;
e73 = ")";
e72 = e73;
}
e70 = e71["push"](e72);
e41 = e70;
}
e38 = e41;
break; }
case "\"t1\"": {
let e74;
{
let e75;
let e76;
e76 = a3;
let e77;
let e78;
e78 = BUILTIN;
let e79;
e79 = a4;
e77 = (e78)(e79);
e75 = e76["push"](e77);
e74 = e75;
}
e38 = e74;
break; }
case "\"t2\"": {
let e80;
let a7;
{
let e81;
let e82;
e82 = d0_0_0;
let e83;
let e84;
let e85;
e85 = a4;
e84 = e85["\"base\""];
let e86;
let e87;
e87 = a0;
e86 = e87["\"context\""];
e83 = { "\"code\"": e84, "\"context\"": e86 };
e81 = (e82)(e83);
a7 = e81;
}
let a8;
{
let e88;
let e89;
e89 = d0_0_0;
let e90;
let e91;
let e92;
e92 = a4;
e91 = e92["\"field\""];
let e93;
let e94;
e94 = a0;
e93 = e94["\"context\""];
e90 = { "\"code\"": e91, "\"context\"": e93 };
e88 = (e89)(e90);
a8 = e88;
}
{
let e95;
let e96;
e96 = a3;
let e97;
e97 = a7;
e95 = e96["push"](e97);
e80 = e95;
}
{
let e98;
let e99;
e99 = a3;
let e100;
{
let e101;
e101 = ".";
e100 = e101;
}
e98 = e99["push"](e100);
e80 = e98;
}
{
let e102;
let e103;
e103 = a3;
let e104;
e104 = a8;
e102 = e103["push"](e104);
e80 = e102;
}
e38 = e80;
break; }
case "\"t3\"": {
let e105;
{
let e106;
let e107;
e107 = a3;
let e108;
e108 = a4;
e106 = e107["push"](e108);
e105 = e106;
}
e38 = e105;
break; }
} }
e0 = e38;
}
{
let e109;
let e110;
e110 = a2;
let e111;
let e112;
e112 = a3;
let e113;
{
let e114;
e114 = "";
e113 = e114;
}
e111 = e112["join"](e113);
e109 = e110["push"](e111);
e0 = e109;
}
{
let e115;
let e116;
e116 = a2;
let e117;
{
let e118;
e118 = ";\n}\n";
e117 = e118;
}
e115 = e116["push"](e117);
e0 = e115;
}
{
let e119;
e119 = a1;
e0 = e119;
}
return e0;
};
}
var d0_0_1;
{
d0_0_1 = function(a0) {
let e120;
let a1;
{
let e121;
let e122;
{
let e123;
e123 = 0.0;
e122 = e123;
}
let e124;
e124 = [];
e121 = { "\"expr_count\"": e122, "\"out\"": e124 };
a1 = e121;
}
{
let e125;
let e126;
let e127;
e127 = a1;
e126 = e127["\"out\""];
let e128;
let e129;
e129 = d0_0_0;
let e130;
let e131;
e131 = a0;
let e132;
e132 = a1;
e130 = { "\"code\"": e131, "\"context\"": e132 };
e128 = (e129)(e130);
e125 = e126["push"](e128);
e120 = e125;
}
{
let e133;
let e134;
let e135;
e135 = a1;
e134 = e135["\"out\""];
let e136;
{
let e137;
e137 = "";
e136 = e137;
}
e133 = e134["join"](e136);
e120 = e133;
}
return e120;
};
}
let e138;
{
let e139;
let e140;
e140 = BUILTIN;
let e141;
let e142;
e142 = d0_0_1;
let e143;
let e144;
let e145;
let e146;
{
let e147;
e147 = "Hello world (node)";
e146 = e147;
}
e145 = { "\"t1\"": e146 };
let e148;
let e149;
let e150;
let e151;
{
let e152;
e152 = "console";
e151 = e152;
}
e150 = { "\"t3\"": e151 };
let e153;
let e154;
{
let e155;
e155 = "log";
e154 = e155;
}
e153 = { "\"t1\"": e154 };
e149 = { "\"base\"": e150, "\"field\"": e153 };
e148 = { "\"t2\"": e149 };
e144 = { "\"arg\"": e145, "\"func\"": e148 };
e143 = { "\"t0\"": e144 };
e141 = (e142)(e143);
e139 = (e140)(e141);
e138 = e139;
}
e138;"#);
}

#[test]
fn test_0010_nodejs_helloworld() {
    setup_v8();
    let gx = new_gx([
        //. .
        ("nodejs", SourceModule::V1({
            let mb = ModuleBuilder::default();
            mb.lower(build_nodejs(&mb))
        })),
        ("main", SourceModule::V1({
            let mb = ModuleBuilder::default();
            mb.block(vec![
                //. .
                mb.bind("nodejs", mb.import(mb.str("nodejs"))),
                mb.bind("app", mb.call(mb.scope_use("nodejs"), mb.tuple(vec![]))),
                mb.call(
                    mb.field(mb.scope_use("app"), mb.str("root")),
                    mb.stage(
                        mb.call(mb.field(mb.scope_use("console"), mb.str("log")), mb.str("Hello world (node)")),
                    ),
                ),
                mb.call(
                    mb.scope_use("log"),
                    mb.call(mb.field(mb.scope_use("app"), mb.str("build")), mb.tuple(vec![])),
                ),
            ])
        })),
    ].into_iter().collect());
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
    let js_sources = sanitize_builtins(&res.generated_mortar_scripts);
    assert_eq!(js_sources.len(), 2);
    assert_eq!(js_sources[0], r#"TODO"#);
    assert_eq!(js_sources[1], r#"TODO"#);
}
