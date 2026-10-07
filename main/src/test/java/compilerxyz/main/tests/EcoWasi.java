package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Supplier;

public final class EcoWasi {
    private EcoWasi() {}

    public static final String MODULE = "wasi_snapshot_preview1";

    public static final String SPEC_REPO = "https://github.com/WebAssembly/WASI";

    public static final String SPEC_COMMIT = "fae981bae14809d91f9bc2d63852d461f331d161";

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        return EcoTargetMb.fnDef(e, out, "wasi", name, argType, ret);
    }

    private static void fnImplVoid(EcoMb e, TList<SourceExpr> out, String name, SourceExpr bodyExpr) {
        var sig = e.fnSigs.getOpt(name).optUnwrap();
        e.fnSigs.remove(name);
        out.add(e.funcImpl(e.v(name + "_b"), sig[0], e.optionTypeNone(), sig[2], e.stage(bodyExpr)));
    }

    private static SourceExpr sxT(EcoMb e) {
        return t(e, "sx");
    }

    private static SourceExpr typesT(EcoMb e) {
        return e.mapType(str(e), t(e, "wt"));
    }

    private static SourceExpr byteIs(EcoMb e, SourceExpr b, int c) {
        return e.eq(e.mb.as(b, intT(e)), num(e, c));
    }

    private static SourceExpr atomOf(EcoMb e, SourceExpr node) {
        return e.callM(node, "atom", e.tup());
    }

    private static SourceExpr kidsOf(EcoMb e, SourceExpr node) {
        return e.callM(node, "kids", e.tup());
    }

    private static SourceExpr kid(EcoMb e, SourceExpr node, SourceExpr i) {
        return hf(e, "h_kid", e.p("node", node), e.p("i", i));
    }

    private static SourceExpr kid(EcoMb e, SourceExpr node, int i) {
        return kid(e, node, e.intOf(i));
    }

    private static SourceExpr head(EcoMb e, SourceExpr node) {
        return hf(e, "h_head", node);
    }

    private static SourceExpr isRef(EcoMb e, SourceExpr atom) {
        return hf(e, "h_is_ref", atom);
    }

    private static SourceExpr wasmField(EcoMb e, String name) {
        return e.fld(e.lowerV("wasm"), name);
    }

    private static SourceExpr wasmType(EcoMb e, String name) {
        return e.mb.as(wasmField(e, name), ty(e));
    }

    private static SourceExpr ptrOf(EcoMb e, SourceExpr pointee) {
        return e.call(wasmField(e, "ptr"), pointee);
    }

    private static SourceExpr valueType(EcoMb e, SourceExpr node) {
        return hf(e, "h_value_type", e.p("types", a(e, "types")), e.p("node", node));
    }

    private static SourceExpr kindOf(EcoMb e, SourceExpr wt) {
        return e.callM(wt, "kind", e.tup());
    }

    private static SourceExpr newNode(EcoMb e, SourceExpr atom) {
        return mk(e, "sx", e.p("atom", atom), e.p("kids", e.listNew(lt(e, "sx"))));
    }

    private static SourceExpr putType(
            EcoMb e, SourceExpr types, SourceExpr name, SourceExpr kind, SourceExpr repr, SourceExpr elem) {
        return e.callM(types, "put", e.tup(name, mk(e, "wt", e.p("kind", kind), e.p("repr", repr), e.p("elem", elem))));
    }

    private static SourceExpr specText(EcoMb e, String file) {
        return e.seq(
                e.let("f", e.fld(e.importGit("w", SPEC_REPO, SPEC_COMMIT), "preview1", "witx", file)),
                e.ret(utf8(
                        e,
                        e.mb.as(
                                e.callM(
                                        e.v("f"),
                                        "read",
                                        e.rec(e.p("offset", e.intOf(0)), e.p("len", e.fld(e.v("f"), "len")))),
                                e.listType(e.mb.typesByte())))));
    }

    private static SourceExpr utf8(EcoMb e, SourceExpr bytes) {
        return e.matchOf(
                e.call(e.evalBuiltin("str_from_utf8"), bytes),
                "__s",
                Opt.none(),
                e.p("some", e.v("__s")),
                e.p("none", e.mb.panic(e.s("witx: spec text is not valid UTF-8"))));
    }

    private static SourceExpr stackTop(EcoMb e) {
        return e.callM(e.v("stack"), "get", toStr(e, e.fld(e.v("st"), "depth")));
    }

    private static SourceExpr setSt(EcoMb e, String field, SourceExpr value) {
        return e.mb.set(e.fld(e.v("st"), field), value);
    }

    private static SourceExpr stIs(EcoMb e, String field, int value) {
        return e.eq(e.fld(e.v("st"), field), num(e, value));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var x = new TList<SourceExpr>();
        x.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        defObj(e, x, "sx", "witx s-expression", e.rec(e.p("atom", str(e)), e.p("kids", e.listType(sxT(e)))));
        accessor(e, x, "sx", "atom", str(e));
        accessor(e, x, "sx", "kids", e.listType(sxT(e)));
        defObj(e, x, "wt", "witx type", e.rec(e.p("kind", str(e)), e.p("repr", str(e)), e.p("elem", str(e))));
        accessor(e, x, "wt", "kind", str(e));
        accessor(e, x, "wt", "repr", str(e));
        accessor(e, x, "wt", "elem", str(e));

        fnDef(e, x, "h_kid", e.rec(e.p("node", sxT(e)), e.p("i", intT(e))), sxT(e));
        fnImpl(
                e,
                x,
                "h_kid",
                body(
                        e,
                        e.let("k", e.rec(e.p("i", e.intOf(0)))),
                        e.foreach(
                                kidsOf(e, a(e, "node")),
                                "c",
                                e.seq(
                                        e.ifOnly(e.eq(e.fld(e.v("k"), "i"), a(e, "i")), retM(e, e.v("c"))),
                                        e.incr(e.fld(e.v("k"), "i")))),
                        e.mb.panic(e.cat(
                                e.s("witx: node "),
                                atomOf(e, a(e, "node")),
                                e.s(" has no child "),
                                toStr(e, a(e, "i"))))));
        fnDef(e, x, "h_head", sxT(e), str(e));
        fnImpl(
                e,
                x,
                "h_head",
                body(e, e.foreach(kidsOf(e, e.v("arg")), "c", retM(e, atomOf(e, e.v("c")))), e.ret(e.dstr(""))));
        fnDef(e, x, "h_is_ref", str(e), boolT(e));
        fnImpl(
                e,
                x,
                "h_is_ref",
                body(
                        e,
                        e.foreach(e.call(e.mb.evalStrUtf8(), e.v("arg")), "b", retM(e, byteIs(e, e.v("b"), '$'))),
                        e.ret(dfalse(e))));

        fnDef(e, x, "h_parse", str(e), sxT(e));
        {
            Supplier<SourceExpr> flush = () -> e.ifOnly(
                    e.mb.operatorGt(e.fld(e.v("st"), "n"), num(e, 0)),
                    e.seq(
                            push(e, kidsOf(e, stackTop(e)), newNode(e, utf8(e, e.fld(e.v("st"), "tok")))),
                            setSt(e, "tok", e.listNew(e.mb.typesByte())),
                            setSt(e, "n", e.intOf(0))));
            Supplier<SourceExpr> pushByte =
                    () -> e.seq(push(e, e.fld(e.v("st"), "tok"), e.v("b")), e.incr(e.fld(e.v("st"), "n")));
            var onOpen = e.seq(
                    flush.get(),
                    e.let("node", newNode(e, e.dstr(""))),
                    push(e, kidsOf(e, stackTop(e)), e.v("node")),
                    e.incr(e.fld(e.v("st"), "depth")),
                    e.callM(e.v("stack"), "put", e.tup(toStr(e, e.fld(e.v("st"), "depth")), e.v("node"))));
            var onClose = e.seq(
                    flush.get(),
                    e.ifOnly(stIs(e, "depth", 0), e.mb.panic(e.s("witx: unbalanced ')'"))),
                    setSt(e, "depth", e.mb.operatorSub(e.fld(e.v("st"), "depth"), num(e, 1))));
            var isSpace = orB(
                    e,
                    byteIs(e, e.v("b"), ' '),
                    orB(e, byteIs(e, e.v("b"), '\n'), orB(e, byteIs(e, e.v("b"), '\t'), byteIs(e, e.v("b"), '\r'))));
            var plain = e.ifElse(
                    byteIs(e, e.v("b"), ';'),
                    e.seq(flush.get(), setSt(e, "comment", e.intOf(1))),
                    e.ifElse(
                            byteIs(e, e.v("b"), '"'),
                            e.seq(flush.get(), setSt(e, "quoted", e.intOf(1)), e.incr(e.fld(e.v("st"), "n"))),
                            e.ifElse(
                                    byteIs(e, e.v("b"), '('),
                                    onOpen,
                                    e.ifElse(
                                            byteIs(e, e.v("b"), ')'),
                                            onClose,
                                            e.ifElse(isSpace, flush.get(), pushByte.get())))));
            fnImpl(
                    e,
                    x,
                    "h_parse",
                    body(
                            e,
                            e.let("root", newNode(e, e.dstr(""))),
                            e.let("stack", e.mapNew(str(e), lt(e, "sx"))),
                            e.callM(e.v("stack"), "put", e.tup(e.s("0"), e.v("root"))),
                            e.let(
                                    "st",
                                    e.rec(
                                            e.p("depth", e.intOf(0)),
                                            e.p("tok", e.listNew(e.mb.typesByte())),
                                            e.p("n", e.intOf(0)),
                                            e.p("comment", e.intOf(0)),
                                            e.p("quoted", e.intOf(0)))),
                            e.foreach(
                                    e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                    "b",
                                    e.ifElse(
                                            stIs(e, "comment", 1),
                                            e.ifOnly(byteIs(e, e.v("b"), '\n'), setSt(e, "comment", e.intOf(0))),
                                            e.ifElse(
                                                    stIs(e, "quoted", 1),
                                                    e.ifElse(
                                                            byteIs(e, e.v("b"), '"'),
                                                            e.seq(setSt(e, "quoted", e.intOf(0)), flush.get()),
                                                            pushByte.get()),
                                                    plain))),
                            flush.get(),
                            e.ifOnly(e.not(stIs(e, "depth", 0)), e.mb.panic(e.s("witx: unbalanced '('"))),
                            e.ret(e.v("root"))));
        }

        fnDef(e, x, "h_types", e.rec(e.p("root", sxT(e)), e.p("types", typesT(e))), e.tup());
        {
            Supplier<SourceExpr> name = () -> atomOf(e, kid(e, e.v("n"), 1));
            Supplier<SourceExpr> def = () -> e.v("def");
            fnImplVoid(
                    e,
                    x,
                    "h_types",
                    e.foreach(
                            kidsOf(e, a(e, "root")),
                            "n",
                            e.ifOnly(
                                    e.eq(head(e, e.v("n")), e.s("typename")),
                                    e.seq(
                                            e.let("def", kid(e, e.v("n"), 2)),
                                            e.let("dh", head(e, def.get())),
                                            e.ifElse(
                                                    e.not(e.eq(atomOf(e, def.get()), e.s(""))),
                                                    putType(
                                                            e,
                                                            a(e, "types"),
                                                            name.get(),
                                                            e.dstr("scalar"),
                                                            atomOf(e, def.get()),
                                                            e.dstr("")),
                                                    e.ifElse(
                                                            e.eq(e.v("dh"), e.s("handle")),
                                                            putType(
                                                                    e,
                                                                    a(e, "types"),
                                                                    name.get(),
                                                                    e.dstr("handle"),
                                                                    e.dstr("i32"),
                                                                    e.dstr("")),
                                                            e.ifElse(
                                                                    orB(
                                                                            e,
                                                                            e.eq(e.v("dh"), e.s("enum")),
                                                                            e.eq(e.v("dh"), e.s("flags"))),
                                                                    putType(
                                                                            e,
                                                                            a(e, "types"),
                                                                            name.get(),
                                                                            e.v("dh"),
                                                                            atomOf(e, kid(e, kid(e, def.get(), 1), 2)),
                                                                            e.dstr("")),
                                                                    e.ifElse(
                                                                            e.eq(e.v("dh"), e.s("list")),
                                                                            putType(
                                                                                    e,
                                                                                    a(e, "types"),
                                                                                    name.get(),
                                                                                    e.dstr("list"),
                                                                                    e.dstr(""),
                                                                                    atomOf(e, kid(e, def.get(), 1))),
                                                                            e.ifElse(
                                                                                    orB(
                                                                                            e,
                                                                                            e.eq(
                                                                                                    e.v("dh"),
                                                                                                    e.s("record")),
                                                                                            e.eq(
                                                                                                    e.v("dh"),
                                                                                                    e.s("union"))),
                                                                                    putType(
                                                                                            e,
                                                                                            a(e, "types"),
                                                                                            name.get(),
                                                                                            e.v("dh"),
                                                                                            e.dstr(""),
                                                                                            e.dstr("")),
                                                                                    e.mb.panic(
                                                                                            e.cat(
                                                                                                    e.s(
                                                                                                            "witx: unsupported type definition "),
                                                                                                    e.v("dh"),
                                                                                                    e.s(" for "),
                                                                                                    name
                                                                                                            .get())))))))))));
        }

        fnDef(e, x, "h_scalar", str(e), ty(e));
        {
            var cases = new TList<SourceExpr>();
            for (var m : new String[][] {
                {"u8", "u8"},
                {"u16", "u16"},
                {"u32", "u32"},
                {"u64", "u64"},
                {"s8", "i8"},
                {"s16", "i16"},
                {"s32", "i32"},
                {"s64", "i64"},
                {"char", "u32"},
                {"f32", "f32"},
                {"f64", "f64"},
            }) {
                cases.add(e.ifOnly(e.eq(e.v("arg"), e.s(m[0])), retM(e, wasmType(e, m[1]))));
            }
            cases.add(e.mb.panic(e.cat(e.s("witx: not a scalar type: "), e.v("arg"))));
            fnImpl(e, x, "h_scalar", body(e, cases.toArray(SourceExpr[]::new)));
        }
        fnDef(e, x, "h_value_type", e.rec(e.p("types", typesT(e)), e.p("node", sxT(e))), ty(e));
        fnImpl(
                e,
                x,
                "h_value_type",
                body(
                        e,
                        e.let("atom", atomOf(e, a(e, "node"))),
                        e.ifOnly(
                                e.eq(e.v("atom"), e.s("")),
                                e.seq(
                                        e.let("form", atomOf(e, kid(e, a(e, "node"), 1))),
                                        e.ifOnly(
                                                e.not(
                                                        andB(
                                                                e,
                                                                e.eq(head(e, a(e, "node")), e.s("@witx")),
                                                                orB(
                                                                        e,
                                                                        e.eq(e.v("form"), e.s("pointer")),
                                                                        e.eq(e.v("form"), e.s("const_pointer"))))),
                                                e.mb.panic(
                                                        e.cat(
                                                                e.s("witx: unsupported type form "),
                                                                head(e, a(e, "node"))))),
                                        retM(e, ptrOf(e, valueType(e, kid(e, a(e, "node"), 2)))))),
                        e.ifOnly(e.not(isRef(e, e.v("atom"))), retM(e, hf(e, "h_scalar", e.v("atom")))),
                        e.let("wt", e.callM(a(e, "types"), "get", e.v("atom"))),
                        e.let("kind", kindOf(e, e.v("wt"))),
                        e.ifOnly(e.eq(e.v("kind"), e.s("handle")), retM(e, wasmType(e, "i32"))),
                        e.ifOnly(
                                orB(e, e.eq(e.v("kind"), e.s("record")), e.eq(e.v("kind"), e.s("union"))),
                                retM(e, wasmType(e, "void"))),
                        e.ifOnly(
                                e.eq(e.v("kind"), e.s("list")),
                                e.mb.panic(e.cat(e.s("witx: list type used by value: "), e.v("atom")))),
                        e.ret(hf(e, "h_scalar", e.callM(e.v("wt"), "repr", e.tup())))));
        fnDef(
                e,
                x,
                "h_param",
                e.rec(e.p("types", typesT(e)), e.p("node", sxT(e)), e.p("out", e.listType(ty(e)))),
                e.tup());
        fnImplVoid(
                e,
                x,
                "h_param",
                e.seq(
                        e.let("tn", kid(e, a(e, "node"), 2)),
                        e.let("atom", atomOf(e, e.v("tn"))),
                        e.let(
                                "is_list",
                                andB(
                                        e,
                                        isRef(e, e.v("atom")),
                                        e.eq(kindOf(e, e.callM(a(e, "types"), "get", e.v("atom"))), e.s("list")))),
                        e.ifElse(
                                e.eq(e.v("atom"), e.s("string")),
                                e.seq(
                                        push(e, a(e, "out"), ptrOf(e, wasmField(e, "u8"))),
                                        push(e, a(e, "out"), wasmField(e, "u32"))),
                                e.ifElse(
                                        e.v("is_list"),
                                        e.seq(
                                                push(e, a(e, "out"), ptrOf(e, wasmField(e, "void"))),
                                                push(e, a(e, "out"), wasmField(e, "u32"))),
                                        push(e, a(e, "out"), valueType(e, e.v("tn")))))));
        fnDef(
                e,
                x,
                "h_result",
                e.rec(e.p("types", typesT(e)), e.p("node", sxT(e)), e.p("out", e.listType(ty(e)))),
                ty(e));
        fnImpl(
                e,
                x,
                "h_result",
                body(
                        e,
                        e.let("ex", kid(e, a(e, "node"), 2)),
                        e.ifOnly(
                                e.not(e.eq(head(e, e.v("ex")), e.s("expected"))),
                                e.mb.panic(e.cat(e.s("witx: unsupported result form "), head(e, e.v("ex"))))),
                        countLet(e, "nk"),
                        countLoop(e, "nk", kidsOf(e, e.v("ex"))),
                        e.let("oks", e.listNew(lt(e, "sx"))),
                        e.ifOnly(
                                e.eq(e.fld(e.v("nk"), "v"), num(e, 3)),
                                e.seq(
                                        e.let("okn", kid(e, e.v("ex"), 1)),
                                        e.ifElse(
                                                e.eq(head(e, e.v("okn")), e.s("tuple")),
                                                e.seq(
                                                        e.let("ti", e.rec(e.p("i", e.intOf(0)))),
                                                        e.foreach(
                                                                kidsOf(e, e.v("okn")),
                                                                "tk",
                                                                e.seq(
                                                                        e.ifOnly(
                                                                                e.mb.operatorGt(
                                                                                        e.fld(e.v("ti"), "i"),
                                                                                        num(e, 0)),
                                                                                push(e, e.v("oks"), e.v("tk"))),
                                                                        e.incr(e.fld(e.v("ti"), "i"))))),
                                                push(e, e.v("oks"), e.v("okn"))))),
                        e.foreach(e.v("oks"), "ok", push(e, a(e, "out"), ptrOf(e, valueType(e, e.v("ok"))))),
                        e.let("errn", kid(e, kid(e, e.v("ex"), e.mb.operatorSub(e.fld(e.v("nk"), "v"), num(e, 1))), 1)),
                        e.ret(valueType(e, e.v("errn")))));
        fnDef(
                e,
                x,
                "h_fn_types",
                e.rec(e.p("root", sxT(e)), e.p("types", typesT(e)), e.p("fns", e.mapType(str(e), ty(e)))),
                e.tup());
        fnImplVoid(
                e,
                x,
                "h_fn_types",
                e.foreach(
                        kidsOf(e, a(e, "root")),
                        "m",
                        e.ifOnly(
                                e.eq(head(e, e.v("m")), e.s("module")),
                                e.foreach(
                                        kidsOf(e, e.v("m")),
                                        "f",
                                        e.ifOnly(
                                                andB(
                                                        e,
                                                        e.eq(head(e, e.v("f")), e.s("@interface")),
                                                        e.eq(atomOf(e, kid(e, e.v("f"), 1)), e.s("func"))),
                                                e.seq(
                                                        e.let("name", atomOf(e, kid(e, kid(e, e.v("f"), 2), 1))),
                                                        e.let("params", e.listNew(ty(e))),
                                                        e.let("ret", e.rec(e.p("v", e.optionTypeLoweredNone()))),
                                                        e.foreach(
                                                                kidsOf(e, e.v("f")),
                                                                "p",
                                                                e.seq(
                                                                        e.ifOnly(
                                                                                e.eq(head(e, e.v("p")), e.s("param")),
                                                                                hf(
                                                                                        e,
                                                                                        "h_param",
                                                                                        e.p("types", a(e, "types")),
                                                                                        e.p("node", e.v("p")),
                                                                                        e.p("out", e.v("params")))),
                                                                        e.ifOnly(
                                                                                e.eq(head(e, e.v("p")), e.s("result")),
                                                                                e.mb.set(
                                                                                        e.fld(e.v("ret"), "v"),
                                                                                        e.optionTypeLoweredSome(
                                                                                                hf(
                                                                                                        e,
                                                                                                        "h_result",
                                                                                                        e.p(
                                                                                                                "types",
                                                                                                                a(
                                                                                                                        e,
                                                                                                                        "types")),
                                                                                                        e.p(
                                                                                                                "node",
                                                                                                                e.v(
                                                                                                                        "p")),
                                                                                                        e.p(
                                                                                                                "out",
                                                                                                                e.v(
                                                                                                                        "params")))))))),
                                                        e.callM(
                                                                a(e, "fns"),
                                                                "put",
                                                                e.tup(
                                                                        e.v("name"),
                                                                        e.call(
                                                                                wasmField(e, "fn"),
                                                                                e.rec(
                                                                                        e.p("params", e.v("params")),
                                                                                        e.p(
                                                                                                "ret",
                                                                                                e.ifElse(
                                                                                                        e.fld(
                                                                                                                e.v(
                                                                                                                        "ret"),
                                                                                                                "v",
                                                                                                                "is_some"),
                                                                                                        e.fld(
                                                                                                                e.v(
                                                                                                                        "ret"),
                                                                                                                "v",
                                                                                                                "value"),
                                                                                                        wasmType(
                                                                                                                e,
                                                                                                                "void")))))))))))));
        x.add(e.finish(e.v("d")));
        x.add(e.ret(e.rec(
                e.p("parse", e.v("h_parse")),
                e.p("types", e.v("h_types")),
                e.p("fn_types", e.v("h_fn_types")),
                e.p("wt_t", t(e, "wt")))));

        var y = new TList<SourceExpr>();
        y.add(e.let("k", e.lower(e.mb.seq(x))));
        y.add(e.let("typenames", e.call(e.fld(e.v("k"), "parse"), specText(e, "typenames.witx"))));
        y.add(e.let("preview1", e.call(e.fld(e.v("k"), "parse"), specText(e, "wasi_snapshot_preview1.witx"))));
        y.add(e.let("types", e.mapNew(str(e), e.fld(e.v("k"), "wt_t"))));
        y.add(e.call(e.fld(e.v("k"), "types"), e.rec(e.p("root", e.v("typenames")), e.p("types", e.v("types")))));
        y.add(e.let("fns", e.mapNew(str(e), ty(e))));
        y.add(e.call(
                e.fld(e.v("k"), "fn_types"),
                e.rec(e.p("root", e.v("preview1")), e.p("types", e.v("types")), e.p("fns", e.v("fns")))));
        y.add(e.ret(e.rec(e.p("fn_types", e.v("fns")))));

        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(y))));
        top.add(e.ret(e.rec(e.p("module", e.s(MODULE)), e.p("fn_types", e.fld(e.v("k"), "fn_types")))));
        return top;
    }
}
