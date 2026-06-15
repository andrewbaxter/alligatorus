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
/// builtin aspect ids, else `option_aspect.none(())`. The type implementing this
/// must have `impl_interface`'d each named aspect, so returning `self` is
/// sufficient for the host to invoke the aspect's methods via normal interface
/// dispatch.
fn aspect_dispatch(mb: &ModuleBuilder, aspect_names: Vec<&str>) -> SourceExpr {
    let conditions: Vec<(SourceExpr, SourceExpr)> = aspect_names.into_iter().map(|aspect| {
        (
            mb.operator_eq(mb.field(mb.scope_use("arg"), mb.str("key")), mb.scope_use(aspect)),
            // `outer_self` = the data type instance (`this` at v8). Casting it to `aspect`
            // via `option.some` emits `__data_instance_to_aspect` which captures the instance
            // into an `AspectUserDefined` external.
            mb.call(
                mb.field(mb.call(mb.scope_use("option"), mb.scope_use("aspect")), mb.str("some")),
                mb.scope_use("outer_self"),
            ),
        )
    }).collect();
    return mb.if_(conditions, Some(aspect_dispatch_none(mb)));
}

/// `option(aspect).none(())` — the no-aspects-here dispatch body, used by types
/// that don't expose any aspects.
fn aspect_dispatch_none(mb: &ModuleBuilder) -> SourceExpr {
    return mb.call(
        mb.field(mb.call(mb.scope_use("option"), mb.scope_use("aspect")), mb.str("none")),
        mb.null(),
    );
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
    // data-type instance via `__data_instance_to_aspect` into an `AspectUserDefined`
    // external.
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
        // `type` field stores the value's source-language type as `any` (= `Rc<dyn Type>`
        // at runtime). This makes `nvalue.new({type: T})` accept any type-bearing value
        // `T` uniformly — both concrete ntype_object instances (via
        // `TypeAnyDynamic::cast_from`) and already-`any`-typed types like an
        // `nscope_entry`'s stored type.
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
    exprs.push(mb.call(mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("set_is_aspect")), mb.bool(true)));
    exprs.push(
        mb.bind(
            "acn_to_code",
            mb.call(mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("new_func")), mb.str("to_code")),
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
            mb.field(mb.scope_use("code"), mb.str("type")),
        ),
    );

    // # ntype_str — nodejs string type. Its instances are types.
    exprs.push(
        mb.bind("ntype_str", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs string"))),
    );
    exprs.push(mb.call(mb.field(mb.scope_use("ntype_str"), mb.str("set_underlying")), mb.tuple(vec![])));
    exprs.extend(impl_as_self_type(mb, "ntype_str", "nodejs string", aspect_dispatch_none(mb)));
    exprs.push(
        mb.call(
            mb.field(mb.scope_use("ntype_str"), mb.str("impl_interface")),
            mb.tuple(
                vec![
                    mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("type")),
                    iface_methods(
                        mb,
                        vec![
                            ("to_code", nvalue_base_to_code(mb, mb.field(mb.scope_use("arg"), mb.str("base")))),
                        ],
                    ),
                ],
            ),
        ),
    );

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
        ("arg", mb.scope_use("any")),
        ("ret", mb.scope_use("any")),
    ])));
    exprs.extend(
        impl_as_self_type(mb, "ntype_func", "nodejs function", aspect_dispatch(mb, vec!["aspect_function_id"])),
    );

    // aspect_function.call — `arg` carries `{self, argument}` as CodeValueExterns
    // (the receiver and argument CVs) plus `xx`. Returns a CodeValueExtern (the
    // result CV). The body uses `new_code_value(code, value)` to explicitly construct
    // that CV — same shape as the Rust `AspectFunction::call` impl.
    //
    // TODO(code-arg): pass the real test-`code` AST as the code arg once the test's
    // `code` tagged union impls the `code_dyn` builtin interface (so it casts to
    // `codedyn`). For now the code arg is `option(codedyn).none(())` — produces a
    // `Code_::None` result, which doesn't match the test's expected JS output but
    // lets the impl compile against the new contract.
    exprs.push(
        impl_iface(
            mb,
            "ntype_func",
            "aspect_function",
            vec![("call", mb.call(mb.scope_use("new_code_value"), mb.tuple(vec![
                // code arg: option(codedyn).none(())
                mb.call(mb.field(mb.call(mb.scope_use("option"), mb.scope_use("codedyn")), mb.str("none")), mb.null()),
                // value arg: original nvalue construction; auto-casts TSDT(nvalue) → value via
                // __data_instance_to_value (nvalue impls the value iface).
                mb.block(vec![
                    // func_code: unwrap .value as an nvalue instance so .base falls through.
                    // `cast_value` panics at runtime if the value isn't a `ValueUserDefined` whose
                    // data type matches nvalue.
                    mb.bind(
                        "func_code",
                        nvalue_base_to_code(
                            mb,
                            mb.field(
                                mb.call(
                                    mb.scope_use("cast_value"),
                                    mb.tuple(
                                        vec![
                                            mb.field(
                                                mb.field(mb.scope_use("arg"), mb.str("self")),
                                                mb.str("value"),
                                            ),
                                            mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("type"))),
                                        ],
                                    ),
                                ),
                                mb.str("base"),
                            ),
                        ),
                    ),
                    // arg_code: aspect-based lookup + dispatch via aspect_lookup.
                    mb.bind(
                        "arg_code",
                        mb.match_(
                            mb.call(
                                mb.scope_use("aspect_lookup"),
                                mb.tuple(
                                    vec![
                                        mb.field(
                                            mb.field(
                                                mb.field(mb.scope_use("arg"), mb.str("argument")),
                                                mb.str("value"),
                                            ),
                                            mb.str("type"),
                                        ),
                                        mb.lower(mb.field(mb.scope_use("aspect_consume_nodejs"), mb.str("type"))),
                                    ],
                                ),
                            ),
                            mb.str("v"),
                            vec![
                                (
                                    mb.str("some"),
                                    mb.call(
                                        mb.field(mb.scope_use("v"), mb.str("to_code")),
                                        mb.call(
                                            mb.scope_use("cast_value"),
                                            mb.tuple(
                                                vec![
                                                    mb.field(
                                                        mb.field(mb.scope_use("arg"), mb.str("argument")),
                                                        mb.str("value"),
                                                    ),
                                                    mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("type"))),
                                                ],
                                            ),
                                        ),
                                    ),
                                ),
                                // none case: no `aspect_consume_nodejs` on the arg's type. Helloworld doesn't hit
                                // this — the arg is a string and nstring impls the aspect. Return a placeholder
                                // `code.lit_str("")` so the match arms are type-consistent.
                                (mb.str("none"), mb.call(mb.lower(mb.field(mb.scope_use("code"), mb.str("lit_str"))), mb.str(""))),
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
                                                vec![
                                                    ("func", mb.scope_use("func_code")),
                                                    ("arg", mb.scope_use("arg_code")),
                                                ],
                                            ),
                                        ),
                                    ),
                                ),
                                ("type", mb.field(mb.scope_use("self"), mb.str("ret"))),
                            ],
                        ),
                    ),
                ]),
            ])))],
        ),
    );

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
            mb.call(mb.scope_use("map"), mb.tuple(vec![mb.scope_use("string"), mb.scope_use("any")])),
        ),
    );
    exprs.extend(
        impl_as_self_type(mb, "ntype_object", "nodejs object", aspect_dispatch(mb, vec!["aspect_field_id"])),
    );

    // aspect_field.field — same contract as aspect_function.call. arg = {self, key,
    // xx} CodeValueExterns/ExprContextRef. Returns CodeValueExtern via
    // new_code_value. TODO(code-arg): real codedyn arg once test's `code` impls
    // code_dyn iface.
    exprs.push(
        impl_iface(
            mb,
            "ntype_object",
            "aspect_field",
            vec![
                ("field", mb.call(mb.scope_use("new_code_value"), mb.tuple(vec![
                    // code arg: option(codedyn).none(())
                    mb.call(mb.field(mb.call(mb.scope_use("option"), mb.scope_use("codedyn")), mb.str("none")), mb.null()),
                    // value arg: original nvalue construction (auto-casts to value)
                    mb.block(
                        vec![
                            mb.bind(
                                "base_code",
                                nvalue_base_to_code(
                                    mb,
                                    mb.field(
                                        mb.call(
                                            mb.scope_use("cast_value"),
                                            mb.tuple(
                                                vec![
                                                    mb.field(
                                                        mb.field(mb.scope_use("arg"), mb.str("self")),
                                                        mb.str("value"),
                                                    ),
                                                    mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("type"))),
                                                ],
                                            ),
                                        ),
                                        mb.str("base"),
                                    ),
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
                                                    mb.lower(mb.field(mb.scope_use("code"), mb.str("field"))),
                                                    mb.record(
                                                        vec![
                                                            ("base", mb.scope_use("base_code")),
                                                            (
                                                                "field",
                                                                mb.call(
                                                                    mb.lower(
                                                                        mb.field(
                                                                            mb.scope_use("code"),
                                                                            mb.str("lit_str"),
                                                                        ),
                                                                    ),
                                                                    mb.field(mb.scope_use("arg"), mb.str("key")),
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
                                                mb.field(mb.scope_use("arg"), mb.str("key")),
                                            ),
                                        ),
                                    ],
                                ),
                            ),
                        ],
                    ),
                ]))),
                (
                    "field_type_opt",
                    mb.call(
                        mb.field(mb.scope_use("self"), mb.str("get")),
                        mb.field(mb.scope_use("arg"), mb.str("key")),
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
    // encounters a `scope_use` and looks up the entry, it calls `use(loc) -> nvalue`,
    // which just hands back the wrapped value (cf. `TypeUnique`'s `AspectBind` impl
    // in Rust).
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
            mb.record(vec![("name", mb.scope_use("string")), ("type", mb.scope_use("any"))]),
        ),
    );

    // `use` constructs a fresh `nvalue` at v8 time, with
    // `base = nvalue_binding(self.name)` and `type = self.type` — both drawn from the
    // entry's underlying. The ret_type is now `CodeValue` (via the
    // `<CodeValue as DynamicWrap>::mortar_type()` = `TypeCodeValueExtern`
    // unification), so the body wraps its produced value in a `CodeValueExtern` via
    // `new_code_value(code, value)`.
    //
    // TODO(code-arg): the function's `code` arg has source-type `CodeValueExtern`
    // (per `build_aspect_arg_record_type`), but `new_code_value` expects
    // `option(code_dyn)`. Until either the CVE can cast through to `option(code_dyn)`
    // or the arg-record exposes typed-per-Rust-param fields, the code slot uses
    // `option(codedyn).none(())` (mirrors `aspect_function.call`).
    exprs.push(
        impl_iface(
            mb,
            "nscope_entry",
            "scope_entry",
            vec![("use", mb.call(mb.scope_use("new_code_value"), mb.tuple(vec![
                // code arg: `option(codedyn).none(())` placeholder
                mb.call(mb.field(mb.call(mb.scope_use("option"), mb.scope_use("codedyn")), mb.str("none")), mb.null()),
                // value arg: the freshly-constructed nvalue
                mb.call(
                    mb.lower(mb.field(mb.scope_use("nvalue"), mb.str("new"))),
                    mb.record(
                        vec![
                            (
                                "base",
                                mb.call(
                                    mb.lower(mb.scope_use("nvalue_binding")),
                                    mb.field(mb.scope_use("self"), mb.str("name")),
                                ),
                            ),
                            ("type", mb.field(mb.scope_use("self"), mb.str("type"))),
                        ],
                    ),
                ),
            ])))],
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
        // via `gen_binding_name` (assumed compiler helper). `target.bind` ret_type is now
        // `(Code, ScopeEntryId)` (via the generic
        // `<(A, B) as DynamicWrap>::mortar_type()`), so the source body returns a
        // `tuple(option(code_dyn), string)`. The first element should ideally be the real
        // test `code` AST cast to `code_dyn`; until the test's `code` tagged_union impls
        // the `code_dyn` builtin interface, use the `option(codedyn).none(())`
        // placeholder pattern (matches `aspect_function.call`).
        (
            "bind",
            mb.block(
                vec![
                    mb.bind(
                        "name",
                        mb.call(mb.scope_use("gen_binding_name"), mb.field(mb.scope_use("arg"), mb.str("xx"))),
                    ),
                    mb.tuple(vec![
                        // code arg: `option(codedyn).none(())` placeholder
                        mb.call(mb.field(mb.call(mb.scope_use("option"), mb.scope_use("codedyn")), mb.str("none")), mb.null()),
                        mb.scope_use("name"),
                    ]),
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
        // block(loc, entries) -> EResult`<Code>`. New ret mortar is `option(code_dyn)` —
        // placeholder until the test's `code` impls `code_dyn`.
        (
            "block",
            mb.call(
                mb.field(mb.call(mb.scope_use("option"), mb.scope_use("codedyn")), mb.str("none")),
                mb.null(),
            ),
        ),
        // code_to_bind_sub(loc, code, type_) -> EResult`<TargetBindSub>`. TargetBindSub's
        // mortar is currently `TypeAnyDynamic` (permissive placeholder), so a record
        // passes the typecheck.
        (
            "code_to_bind_sub",
            mb.record(
                vec![
                    ("code", mb.field(mb.scope_use("arg"), mb.str("code"))),
                    ("type", mb.field(mb.scope_use("arg"), mb.str("type"))),
                ],
            ),
        ),
        // if(xx, loc, conditions, else) -> EResult`<CodeValue>`. Ret mortar is
        // `TypeCodeValueExternDynamic` — `arg.xx` is already CVE-typed, so return it as a
        // stub. helloworld doesn't exercise this body.
        ("if", mb.field(mb.scope_use("arg"), mb.str("xx"))),
        // match: same story as `if`.
        ("match", mb.field(mb.scope_use("arg"), mb.str("xx"))),
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
        // stage(xx, loc, tree, lower_values) -> EResult`<CodeValue>`. Same CVE-typed stub
        // pattern as `if`/`match`. helloworld doesn't re-stage.
        ("stage", mb.field(mb.scope_use("arg"), mb.str("xx"))),
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
        mb.match_(mb.field(mb.scope_use("self"), mb.str("root")), mb.str("root"), vec![(mb.str("some"), mb.block(vec![
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
                                mb.tuple(vec![mb.scope_use("string"), mb.scope_use("any")]),
                            ),
                            mb.str("new"),
                        ),
                        mb.record(vec![("log", mb.scope_use("ntype_log"))]),
                    ),
                ),
            ),
            // Wrap the `console` binding name + type as an `nscope_entry`; the entry's `use`
            // constructs the actual nvalue (with `base = nvalue_binding(name)`,
            // `type = type`) at v8 time when `console` is resolved from scope.
            mb.bind(
                "console_entry",
                mb.call(
                    mb.lower(mb.field(mb.scope_use("nscope_entry"), mb.str("new"))),
                    mb.record(vec![("name", mb.str("console")), ("type", mb.scope_use("ntype_console"))]),
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
            // Render the resulting code AST as a JS source string. `res.cv.code.code` is a
            // `Code_` tagged union with variants `none` (no code emitted) and `dyn(handle)`
            // (a `code_dyn` handle); the `dyn` variant is what the nodejs target's
            // code-emitting impls produce, and we cast it to the nodejs `code` tagged union
            // before rendering.
            mb.match_(
                mb.field(mb.field(mb.scope_use("res"), mb.str("code")), mb.str("code")),
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
        ])), (mb.str("none"), mb.block(vec![mb.str("")]))], None),
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
        // # Internal tagged union of nodejs-code variants. Not exposed
        //
        // directly — the public `code` binding (below) is a wrapper data type whose
        // underlying is this tagged union. The wrapper exists so the type can impl
        // `code_dyn` (only data types can impl interfaces), and `self` inside iface impls
        // resolves to the underlying tagged-union value — making `code_dyn.get` body
        // `mb.scope_use("self")` return a matchable tagged-union value.
        mb.bind("code_tu_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("tagged_union")), mb.tuple(vec![]))),
        mb.bind("code_tu", mb.field(mb.scope_use("code_tu_builder"), mb.str("type"))),
        // # Wrapper data type — set_underlying first; variants reference
        //
        // `code` (the wrapper) below, so we need `code` bound before adding variants. The
        // tagged-union ref is shared by mutable Rc so adding variants after
        // `set_underlying` is fine.
        mb.bind("code_data", mb.call(mb.field(mb.scope_use("d"), mb.str("new_data")), mb.str("nodejs code"))),
        mb.call(mb.field(mb.scope_use("code_data"), mb.str("set_underlying")), mb.scope_use("code_tu")),
        mb.bind("code", mb.field(mb.scope_use("code_data"), mb.str("type"))),
        // # Variants — payload types reference `code` (the wrapper), so the
        //
        // AST is recursively built out of wrappers.
        mb.bind("code_tu_call", mb.call(mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")), mb.tuple(vec![
            //. .
            mb.str("call"),
            mb.record(vec![("func", mb.scope_use("code")), ("arg", mb.scope_use("code"))]),
        ]))),
        mb.bind(
            "code_tu_lit_str",
            mb.call(
                mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("lit_str"), mb.scope_use("string")]),
            ),
        ),
        mb.bind(
            "code_tu_field",
            mb.call(mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")), mb.tuple(vec![
                //. .
                mb.str("field"),
                mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("code"))]),
            ])),
        ),
        mb.bind(
            "code_tu_scope_use",
            mb.call(
                mb.field(mb.scope_use("code_tu_builder"), mb.str("variant")),
                mb.tuple(vec![mb.str("scope_use"), mb.scope_use("string")]),
            ),
        ),
        // # Impl `code_dyn` on the wrapper (empty — the iface has no
        //
        // methods; impl is what flags the data type as code_dyn-castable).
        mb.call(
            mb.field(mb.scope_use("code_data"), mb.str("impl_interface")),
            mb.tuple(vec![mb.scope_use("code_dyn"), iface_methods(mb, vec![])]),
        ),
        // # `code.get()` — a regular data-type method. Body is `self`, which
        //
        // in method impls resolves to the underlying tagged-union value. The renderer
        // calls `<code_dyn_handle>.cast(code.type).get()` to recover the original
        // tagged-union value for matching.
        mb.bind("code_get_builder", mb.call(mb.field(mb.scope_use("code_data"), mb.str("new_func")), mb.str("get"))),
        mb.call(mb.field(mb.scope_use("code_get_builder"), mb.str("arg")), mb.tuple(vec![])),
        mb.call(mb.field(mb.scope_use("code_get_builder"), mb.str("ret")), mb.scope_use("code_tu")),
        mb.call(mb.field(mb.scope_use("code_get_builder"), mb.str("impl")), mb.stage(mb.scope_use("self"))),
        // # Standalone construction functions — each builds the tagged-union
        //
        // variant and wraps in the data type. Re-exposed at the record level (below) so
        // callers can keep doing `code.call(...)`, etc.
        mb.bind("code_call_builder", mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("code.call"))),
        mb.bind("code_call", mb.field(mb.scope_use("code_call_builder"), mb.str("func"))),
        mb.call(
            mb.field(mb.scope_use("code_call_builder"), mb.str("arg")),
            mb.record(vec![("func", mb.scope_use("code")), ("arg", mb.scope_use("code"))]),
        ),
        mb.call(mb.field(mb.scope_use("code_call_builder"), mb.str("ret")), mb.scope_use("code")),
        mb.call(
            mb.field(mb.scope_use("code_call_builder"), mb.str("impl")),
            mb.stage(
                mb.call(
                    mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                    mb.call(mb.lower(mb.scope_use("code_tu_call")), mb.scope_use("arg")),
                ),
            ),
        ),
        mb.bind(
            "code_lit_str_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("code.lit_str")),
        ),
        mb.bind("code_lit_str", mb.field(mb.scope_use("code_lit_str_builder"), mb.str("func"))),
        mb.call(mb.field(mb.scope_use("code_lit_str_builder"), mb.str("arg")), mb.scope_use("string")),
        mb.call(mb.field(mb.scope_use("code_lit_str_builder"), mb.str("ret")), mb.scope_use("code")),
        mb.call(
            mb.field(mb.scope_use("code_lit_str_builder"), mb.str("impl")),
            mb.stage(
                mb.call(
                    mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                    mb.call(mb.lower(mb.scope_use("code_tu_lit_str")), mb.scope_use("arg")),
                ),
            ),
        ),
        mb.bind(
            "code_field_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("code.field")),
        ),
        mb.bind("code_field", mb.field(mb.scope_use("code_field_builder"), mb.str("func"))),
        mb.call(
            mb.field(mb.scope_use("code_field_builder"), mb.str("arg")),
            mb.record(vec![("base", mb.scope_use("code")), ("field", mb.scope_use("code"))]),
        ),
        mb.call(mb.field(mb.scope_use("code_field_builder"), mb.str("ret")), mb.scope_use("code")),
        mb.call(
            mb.field(mb.scope_use("code_field_builder"), mb.str("impl")),
            mb.stage(
                mb.call(
                    mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                    mb.call(mb.lower(mb.scope_use("code_tu_field")), mb.scope_use("arg")),
                ),
            ),
        ),
        mb.bind(
            "code_scope_use_builder",
            mb.call(mb.field(mb.scope_use("d"), mb.str("new_func")), mb.str("code.scope_use")),
        ),
        mb.bind("code_scope_use", mb.field(mb.scope_use("code_scope_use_builder"), mb.str("func"))),
        mb.call(mb.field(mb.scope_use("code_scope_use_builder"), mb.str("arg")), mb.scope_use("string")),
        mb.call(mb.field(mb.scope_use("code_scope_use_builder"), mb.str("ret")), mb.scope_use("code")),
        mb.call(
            mb.field(mb.scope_use("code_scope_use_builder"), mb.str("impl")),
            mb.stage(
                mb.call(
                    mb.lower(mb.field(mb.scope_use("code_data"), mb.str("new"))),
                    mb.call(mb.lower(mb.scope_use("code_tu_scope_use")), mb.scope_use("arg")),
                ),
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
            mb.match_(
                mb.call(
                    mb.field(mb.field(mb.scope_use("arg"), mb.str("code")), mb.str("get")),
                    mb.tuple(vec![]),
                ),
                mb.str("code"),
                vec![
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
                ],
                None,
            ),
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
        // Assemble and return — `type` is the wrapper data type. The variant-constructor
        // standalone funcs are re-exposed as record fields so callers can keep doing
        // `code.call(...)`, etc.
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
            mb.call(mb.scope_use("map"), mb.tuple(vec![mb.scope_use("constkey"), mb.scope_use("expr")])),
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
        impl_iface(mb, target_var, "value", vec![("type", mb.scope_use("outer_self"))]),
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
/// argument is cast to `nvalue_base` first — at impl-compile time the static type
/// may be `any` (the receiver's underlying field type isn't inferred).
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
e2 = a50;
let e3;
e3 = a51;
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
e12 = a0;
let e13;
{
let e14;
e14 = "nodejs code";
e13 = e14;
}
e11 = e12["_1"](e13);
a3 = e11;
}
{
let e15;
let e16;
e16 = a3;
let e17;
let e18;
e18 = BUILTIN;
let e19;
e19 = a2;
e17 = (e18)(e19);
e15 = e16["_3"](e17);
e4 = e15;
}
let a4;
{
let e20;
let e21;
e21 = a3;
e20 = e21["_0"];
a4 = e20;
}
let a5;
{
let e22;
let e23;
e23 = a1;
let e24;
{
let e25;
e25 = "call";
e24 = e25;
}
let e26;
let e27;
e27 = BUILTIN;
let e28;
let e29;
let e30;
e30 = BUILTIN;
let e31;
e31 = a4;
e29 = (e30)(e31);
let e32;
let e33;
e33 = BUILTIN;
let e34;
e34 = a4;
e32 = (e33)(e34);
e28 = { "\"arg\"": e29, "\"func\"": e32 };
e26 = (e27)(e28);
e22 = e23["_1"](e24,e26);
a5 = e22;
}
let a7;
{
let e35;
let e36;
e36 = a1;
let e37;
{
let e38;
e38 = "lit_str";
e37 = e38;
}
let e39;
e39 = a6;
e35 = e36["_1"](e37,e39);
a7 = e35;
}
let a8;
{
let e40;
let e41;
e41 = a1;
let e42;
{
let e43;
e43 = "field";
e42 = e43;
}
let e44;
let e45;
e45 = BUILTIN;
let e46;
let e47;
let e48;
e48 = BUILTIN;
let e49;
e49 = a4;
e47 = (e48)(e49);
let e50;
let e51;
e51 = BUILTIN;
let e52;
e52 = a4;
e50 = (e51)(e52);
e46 = { "\"base\"": e47, "\"field\"": e50 };
e44 = (e45)(e46);
e40 = e41["_1"](e42,e44);
a8 = e40;
}
let a10;
{
let e53;
let e54;
e54 = a1;
let e55;
{
let e56;
e56 = "scope_use";
e55 = e56;
}
let e57;
e57 = a9;
e53 = e54["_1"](e55,e57);
a10 = e53;
}
{
let e58;
let e59;
e59 = a3;
let e60;
e60 = BUILTIN;
let e61;
e61 = {  };
e58 = e59["_5"](e60,e61);
e4 = e58;
}
let a11;
{
let e62;
let e63;
e63 = a3;
let e64;
{
let e65;
e65 = "get";
e64 = e65;
}
e62 = e63["_4"](e64);
a11 = e62;
}
{
let e66;
let e67;
e67 = a11;
let e68;
let e69;
e69 = BUILTIN;
let e70;
e70 = [];
e68 = (e69)(e70);
e66 = e67["_2"](e68);
e4 = e66;
}
{
let e71;
let e72;
e72 = a11;
let e73;
let e74;
e74 = BUILTIN;
let e75;
e75 = a2;
e73 = (e74)(e75);
e71 = e72["_1"](e73);
e4 = e71;
}
{
let e76;
let e77;
e77 = a11;
let e78;
e78 = a12;
e76 = e77["_0"](e78);
e4 = e76;
}
let a13;
{
let e79;
let e80;
e80 = a0;
let e81;
{
let e82;
e82 = "code.call";
e81 = e82;
}
e79 = e80["_0"](e81);
a13 = e79;
}
let a14;
{
let e83;
let e84;
e84 = a13;
e83 = e84["_0"];
a14 = e83;
}
{
let e85;
let e86;
e86 = a13;
let e87;
let e88;
e88 = BUILTIN;
let e89;
let e90;
let e91;
e91 = BUILTIN;
let e92;
e92 = a4;
e90 = (e91)(e92);
let e93;
let e94;
e94 = BUILTIN;
let e95;
e95 = a4;
e93 = (e94)(e95);
e89 = { "\"arg\"": e90, "\"func\"": e93 };
e87 = (e88)(e89);
e85 = e86["_3"](e87);
e4 = e85;
}
{
let e96;
let e97;
e97 = a13;
let e98;
let e99;
e99 = BUILTIN;
let e100;
e100 = a4;
e98 = (e99)(e100);
e96 = e97["_5"](e98);
e4 = e96;
}
{
let e101;
let e102;
e102 = a13;
let e103;
let e104;
e104 = BUILTIN;
let e105;
e105 = a15;
let e106;
e106 = a16;
let e107;
let e108;
e108 = a3;
e107 = e108["_1"];
let e109;
e109 = a17;
let e110;
e110 = a5;
e103 = (e104)(e105,e106,e107,e109,e110);
e101 = e102["_2"](e103);
e4 = e101;
}
let a18;
{
let e111;
let e112;
e112 = a0;
let e113;
{
let e114;
e114 = "code.lit_str";
e113 = e114;
}
e111 = e112["_0"](e113);
a18 = e111;
}
let a19;
{
let e115;
let e116;
e116 = a18;
e115 = e116["_0"];
a19 = e115;
}
{
let e117;
let e118;
e118 = a18;
let e119;
e119 = a20;
e117 = e118["_3"](e119);
e4 = e117;
}
{
let e120;
let e121;
e121 = a18;
let e122;
let e123;
e123 = BUILTIN;
let e124;
e124 = a4;
e122 = (e123)(e124);
e120 = e121["_5"](e122);
e4 = e120;
}
{
let e125;
let e126;
e126 = a18;
let e127;
let e128;
e128 = BUILTIN;
let e129;
e129 = a21;
let e130;
e130 = a22;
let e131;
let e132;
e132 = a3;
e131 = e132["_1"];
let e133;
e133 = a23;
let e134;
e134 = a7;
e127 = (e128)(e129,e130,e131,e133,e134);
e125 = e126["_2"](e127);
e4 = e125;
}
let a24;
{
let e135;
let e136;
e136 = a0;
let e137;
{
let e138;
e138 = "code.field";
e137 = e138;
}
e135 = e136["_0"](e137);
a24 = e135;
}
let a25;
{
let e139;
let e140;
e140 = a24;
e139 = e140["_0"];
a25 = e139;
}
{
let e141;
let e142;
e142 = a24;
let e143;
let e144;
e144 = BUILTIN;
let e145;
let e146;
let e147;
e147 = BUILTIN;
let e148;
e148 = a4;
e146 = (e147)(e148);
let e149;
let e150;
e150 = BUILTIN;
let e151;
e151 = a4;
e149 = (e150)(e151);
e145 = { "\"base\"": e146, "\"field\"": e149 };
e143 = (e144)(e145);
e141 = e142["_3"](e143);
e4 = e141;
}
{
let e152;
let e153;
e153 = a24;
let e154;
let e155;
e155 = BUILTIN;
let e156;
e156 = a4;
e154 = (e155)(e156);
e152 = e153["_5"](e154);
e4 = e152;
}
{
let e157;
let e158;
e158 = a24;
let e159;
let e160;
e160 = BUILTIN;
let e161;
e161 = a26;
let e162;
e162 = a27;
let e163;
let e164;
e164 = a3;
e163 = e164["_1"];
let e165;
e165 = a28;
let e166;
e166 = a8;
e159 = (e160)(e161,e162,e163,e165,e166);
e157 = e158["_2"](e159);
e4 = e157;
}
let a29;
{
let e167;
let e168;
e168 = a0;
let e169;
{
let e170;
e170 = "code.scope_use";
e169 = e170;
}
e167 = e168["_0"](e169);
a29 = e167;
}
let a30;
{
let e171;
let e172;
e172 = a29;
e171 = e172["_0"];
a30 = e171;
}
{
let e173;
let e174;
e174 = a29;
let e175;
e175 = a31;
e173 = e174["_3"](e175);
e4 = e173;
}
{
let e176;
let e177;
e177 = a29;
let e178;
let e179;
e179 = BUILTIN;
let e180;
e180 = a4;
e178 = (e179)(e180);
e176 = e177["_5"](e178);
e4 = e176;
}
{
let e181;
let e182;
e182 = a29;
let e183;
let e184;
e184 = BUILTIN;
let e185;
e185 = a32;
let e186;
e186 = a33;
let e187;
let e188;
e188 = a3;
e187 = e188["_1"];
let e189;
e189 = a34;
let e190;
e190 = a10;
e183 = (e184)(e185,e186,e187,e189,e190);
e181 = e182["_2"](e183);
e4 = e181;
}
let a35;
{
let e191;
let e192;
e192 = a0;
let e193;
{
let e194;
e194 = "Render Node.js code (inner)";
e193 = e194;
}
e191 = e192["_0"](e193);
a35 = e191;
}
let a36;
{
let e195;
let e196;
e196 = a35;
e195 = e196["_0"];
a36 = e195;
}
{
let e197;
let e198;
e198 = a35;
let e199;
let e200;
e200 = BUILTIN;
let e201;
let e202;
let e203;
e203 = BUILTIN;
let e204;
e204 = a4;
e202 = (e203)(e204);
let e205;
let e206;
e206 = BUILTIN;
let e207;
let e208;
e208 = a37;
let e209;
e209 = a38;
e207 = { "\"expr_count\"": e208, "\"out\"": e209 };
e205 = (e206)(e207);
e201 = { "\"code\"": e202, "\"context\"": e205 };
e199 = (e200)(e201);
e197 = e198["_3"](e199);
e4 = e197;
}
{
let e210;
let e211;
e211 = a35;
let e212;
e212 = a39;
e210 = e211["_5"](e212);
e4 = e210;
}
{
let e213;
let e214;
e214 = a35;
let e215;
let e216;
e216 = BUILTIN;
let e217;
e217 = a40;
let e218;
e218 = a41;
let e219;
e219 = a36;
let e220;
e220 = a42;
let e221;
e221 = a36;
let e222;
e222 = a43;
let e223;
e223 = a36;
let e224;
e224 = a44;
let e225;
e225 = a36;
e215 = (e216)(e217,e218,e219,e220,e221,e222,e223,e224,e225);
e213 = e214["_2"](e215);
e4 = e213;
}
let a45;
{
let e226;
let e227;
e227 = a0;
let e228;
{
let e229;
e229 = "Render Node.js code";
e228 = e229;
}
e226 = e227["_0"](e228);
a45 = e226;
}
let a46;
{
let e230;
let e231;
e231 = a45;
e230 = e231["_0"];
a46 = e230;
}
{
let e232;
let e233;
e233 = a45;
let e234;
let e235;
e235 = BUILTIN;
let e236;
e236 = a4;
e234 = (e235)(e236);
e232 = e233["_3"](e234);
e4 = e232;
}
{
let e237;
let e238;
e238 = a45;
let e239;
e239 = a47;
e237 = e238["_5"](e239);
e4 = e237;
}
{
let e240;
let e241;
e241 = a45;
let e242;
let e243;
e243 = BUILTIN;
let e244;
e244 = a48;
let e245;
e245 = a49;
let e246;
e246 = a36;
e242 = (e243)(e244,e245,e246);
e240 = e241["_2"](e242);
e4 = e240;
}
{
let e247;
let e248;
e248 = a14;
let e249;
e249 = a25;
let e250;
e250 = a19;
let e251;
e251 = a46;
let e252;
e252 = a30;
let e253;
e253 = a4;
e247 = { "\"call\"": e248, "\"field\"": e249, "\"lit_str\"": e250, "\"render\"": e251, "\"scope_use\"": e252, "\"type\"": e253 };
e4 = e247;
}
e0 = (e1)(e2,e3,e4);
e0;"#
    );
    assert_eq!(js_sources[1], r#"var d0_0_1;
{
d0_0_1 = function(a0) {
let e0;
let e1;
let e2;
e2 = a0;
e1 = { "\"t0\"": e2 };
let e3;
e3 = d0_0_0;
e0 = { "\"_0\"": e1, "\"_1\"": e3 };
return e0;
};
}
var d0_0_2;
{
d0_0_2 = function(a0) {
let e4;
let e5;
let e6;
e6 = a0;
e5 = { "\"t1\"": e6 };
let e7;
e7 = d0_0_0;
e4 = { "\"_0\"": e5, "\"_1\"": e7 };
return e4;
};
}
var d0_0_3;
{
d0_0_3 = function(a0) {
let e8;
let e9;
let e10;
e10 = a0;
e9 = { "\"t2\"": e10 };
let e11;
e11 = d0_0_0;
e8 = { "\"_0\"": e9, "\"_1\"": e11 };
return e8;
};
}
var d0_0_4;
{
d0_0_4 = function(a0) {
let e12;
let e13;
let e14;
e14 = a0;
e13 = { "\"t3\"": e14 };
let e15;
e15 = d0_0_0;
e12 = { "\"_0\"": e13, "\"_1\"": e15 };
return e12;
};
}
var d0_0_5;
{
d0_0_5 = function(a0) {
let e16;
let a1;
{
let e17;
let e18;
let e19;
{
let e20;
e20 = "e";
e19 = e20;
}
let e21;
let e22;
let e23;
let e24;
e24 = a0;
e23 = e24["\"context\""];
e22 = e23["\"expr_count\""];
e21 = e22["toString"]();
e18 = [e19, e21];
let e25;
{
let e26;
e26 = "";
e25 = e26;
}
e17 = e18["join"](e25);
a1 = e17;
}
{
let e27;
let e28;
let e29;
e29 = a0;
e28 = e29["\"context\""];
let e30;
let e31;
let e32;
let e33;
e33 = a0;
e32 = e33["\"context\""];
e31 = e32["\"expr_count\""];
let e34;
{
let e35;
e35 = 1.0;
e34 = e35;
}
e30 = (e31 + e34);
e28["\"expr_count\""] = e30;
e27 = null;
e16 = e27;
}
let a2;
{
let e36;
let e37;
let e38;
e38 = a0;
e37 = e38["\"context\""];
e36 = e37["\"out\""];
a2 = e36;
}
{
let e39;
let e40;
e40 = a2;
let e41;
{
let e42;
e42 = "let ";
e41 = e42;
}
e39 = e40["push"](e41);
e16 = e39;
}
{
let e43;
let e44;
e44 = a2;
let e45;
e45 = a1;
e43 = e44["push"](e45);
e16 = e43;
}
{
let e46;
let e47;
e47 = a2;
let e48;
{
let e49;
e49 = ";\n{\n";
e48 = e49;
}
e46 = e47["push"](e48);
e16 = e46;
}
let a3;
{
let e50;
let e51;
e51 = a1;
let e52;
{
let e53;
e53 = " = ";
e52 = e53;
}
e50 = [e51, e52];
a3 = e50;
}
{
let e54;
let e55;
let e56;
let e57;
e57 = a0;
e56 = e57["\"code\""];
e55 = e56["\"_1\""]();
{ let [__tag, a4] = Object.entries(e55)[0];
switch (__tag) {
case "\"t0\"": {
let e58;
let a5;
{
let e59;
let e60;
e60 = d0_0_5;
let e61;
let e62;
let e63;
e63 = a4;
e62 = e63["\"func\""];
let e64;
let e65;
e65 = a0;
e64 = e65["\"context\""];
e61 = { "\"code\"": e62, "\"context\"": e64 };
e59 = (e60)(e61);
a5 = e59;
}
let a6;
{
let e66;
let e67;
e67 = d0_0_5;
let e68;
let e69;
let e70;
e70 = a4;
e69 = e70["\"arg\""];
let e71;
let e72;
e72 = a0;
e71 = e72["\"context\""];
e68 = { "\"code\"": e69, "\"context\"": e71 };
e66 = (e67)(e68);
a6 = e66;
}
{
let e73;
let e74;
e74 = a3;
let e75;
{
let e76;
e76 = "(";
e75 = e76;
}
e73 = e74["push"](e75);
e58 = e73;
}
{
let e77;
let e78;
e78 = a3;
let e79;
e79 = a5;
e77 = e78["push"](e79);
e58 = e77;
}
{
let e80;
let e81;
e81 = a3;
let e82;
{
let e83;
e83 = ")(";
e82 = e83;
}
e80 = e81["push"](e82);
e58 = e80;
}
{
let e84;
let e85;
e85 = a3;
let e86;
e86 = a6;
e84 = e85["push"](e86);
e58 = e84;
}
{
let e87;
let e88;
e88 = a3;
let e89;
{
let e90;
e90 = ")";
e89 = e90;
}
e87 = e88["push"](e89);
e58 = e87;
}
e54 = e58;
break; }
case "\"t1\"": {
let e91;
{
let e92;
let e93;
e93 = a3;
let e94;
let e95;
e95 = BUILTIN;
let e96;
e96 = a4;
e94 = (e95)(e96);
e92 = e93["push"](e94);
e91 = e92;
}
e54 = e91;
break; }
case "\"t2\"": {
let e97;
let a7;
{
let e98;
let e99;
e99 = d0_0_5;
let e100;
let e101;
let e102;
e102 = a4;
e101 = e102["\"base\""];
let e103;
let e104;
e104 = a0;
e103 = e104["\"context\""];
e100 = { "\"code\"": e101, "\"context\"": e103 };
e98 = (e99)(e100);
a7 = e98;
}
let a8;
{
let e105;
let e106;
e106 = d0_0_5;
let e107;
let e108;
let e109;
e109 = a4;
e108 = e109["\"field\""];
let e110;
let e111;
e111 = a0;
e110 = e111["\"context\""];
e107 = { "\"code\"": e108, "\"context\"": e110 };
e105 = (e106)(e107);
a8 = e105;
}
{
let e112;
let e113;
e113 = a3;
let e114;
e114 = a7;
e112 = e113["push"](e114);
e97 = e112;
}
{
let e115;
let e116;
e116 = a3;
let e117;
{
let e118;
e118 = ".";
e117 = e118;
}
e115 = e116["push"](e117);
e97 = e115;
}
{
let e119;
let e120;
e120 = a3;
let e121;
e121 = a8;
e119 = e120["push"](e121);
e97 = e119;
}
e54 = e97;
break; }
case "\"t3\"": {
let e122;
{
let e123;
let e124;
e124 = a3;
let e125;
e125 = a4;
e123 = e124["push"](e125);
e122 = e123;
}
e54 = e122;
break; }
} }
e16 = e54;
}
{
let e126;
let e127;
e127 = a2;
let e128;
let e129;
e129 = a3;
let e130;
{
let e131;
e131 = "";
e130 = e131;
}
e128 = e129["join"](e130);
e126 = e127["push"](e128);
e16 = e126;
}
{
let e132;
let e133;
e133 = a2;
let e134;
{
let e135;
e135 = ";\n}\n";
e134 = e135;
}
e132 = e133["push"](e134);
e16 = e132;
}
{
let e136;
e136 = a1;
e16 = e136;
}
return e16;
};
}
var d0_0_6;
{
d0_0_6 = function(a0) {
let e137;
let a1;
{
let e138;
let e139;
{
let e140;
e140 = 0.0;
e139 = e140;
}
let e141;
e141 = [];
e138 = { "\"expr_count\"": e139, "\"out\"": e141 };
a1 = e138;
}
{
let e142;
let e143;
let e144;
e144 = a1;
e143 = e144["\"out\""];
let e145;
let e146;
e146 = d0_0_5;
let e147;
let e148;
e148 = a0;
let e149;
e149 = a1;
e147 = { "\"code\"": e148, "\"context\"": e149 };
e145 = (e146)(e147);
e142 = e143["push"](e145);
e137 = e142;
}
{
let e150;
let e151;
let e152;
e152 = a1;
e151 = e152["\"out\""];
let e153;
{
let e154;
e154 = "";
e153 = e154;
}
e150 = e151["join"](e153);
e137 = e150;
}
return e137;
};
}
var d0_0_0;
{
d0_0_0 = function(a1) {
let a0 = this["\"_0\""];
let e155;
e155 = a0;
return e155;
};
}
let e156;
{
let e157;
let e158;
e158 = BUILTIN;
let e159;
let e160;
e160 = d0_0_6;
let e161;
let e162;
e162 = d0_0_1;
let e163;
let e164;
let e165;
e165 = d0_0_2;
let e166;
{
let e167;
e167 = "Hello world (node)";
e166 = e167;
}
e164 = (e165)(e166);
let e168;
let e169;
e169 = d0_0_3;
let e170;
let e171;
let e172;
e172 = d0_0_4;
let e173;
{
let e174;
e174 = "console";
e173 = e174;
}
e171 = (e172)(e173);
let e175;
let e176;
e176 = d0_0_2;
let e177;
{
let e178;
e178 = "log";
e177 = e178;
}
e175 = (e176)(e177);
e170 = { "\"base\"": e171, "\"field\"": e175 };
e168 = (e169)(e170);
e163 = { "\"arg\"": e164, "\"func\"": e168 };
e161 = (e162)(e163);
e159 = (e160)(e161);
e157 = (e158)(e159);
e156 = e157;
}
e156;"#);
}
