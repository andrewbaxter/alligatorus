package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.p;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoWebWasm {
    private EcoWebWasm() {}

    public static final String HTML =
            """
            <!doctype html>
            <html>
            <head><meta charset="utf-8"></head>
            <body><script src="main.js"></script></body>
            </html>
            """;

    private static SourceExpr bytesT(EcoMb e) {
        return e.call(e.mb.typesArray(), e.mb.typesByte());
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var top = new TList<SourceExpr>();
        top.add(e.let("js", e.importLocal("js", "js-es2020.atz")));
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("webidl", e.importLocal("webidl", "webidl-js.atz")));
        top.add(e.let("d", e.define()));
        top.add(e.optionTypeLet());
        top.add(e.let("build_b", e.defFunc(e.v("d"), "build")));
        var body = new TList<SourceExpr>();
        for (var g : new String[] {"WebAssembly", "fetch", "instance"}) {
            body.add(e.let(g, e.call(e.lower(e.fld(e.v("js"), "global_fn")), e.rec(e.p("name", e.s(g))))));
        }
        body.add(e.let(
                "r",
                e.call(
                        e.mb.builtin("exec"),
                        e.rec(
                                e.p("command", e.s("wat2wasm")),
                                e.p("args", e.strList(e.s("main.wat"), e.s("-o"), e.s("main.wasm"))),
                                e.p("env", e.mapNew(e.mb.typesString(), e.mb.typesString())),
                                e.p(
                                        "files",
                                        e.mapNew(
                                                e.mb.typesString(),
                                                bytesT(e),
                                                e.p("main.wat", e.call(e.mb.evalStrUtf8(), e.fld(e.v("arg"), "wat"))))),
                                e.p("success_codes", e.optNone(e.listType(e.mb.typesInt())))))));
        body.add(e.let("f", e.callM(e.callM0(e.v("r"), "dir"), "open", e.s("main.wasm"))));
        body.add(e.let(
                "wasm_bytes",
                e.callM(e.v("f"), "read", e.rec(e.p("offset", e.intOf(0)), e.p("len", e.fld(e.v("f"), "len"))))));
        body.add(e.let("m", e.call(e.lower(e.fld(e.v("js"), "new")), e.tup())));
        body.add(e.call(e.lower(e.fld(e.v("webidl"), "runtime")), e.rec(e.p("mod", e.v("m")))));
        body.add(e.let(
                "imports",
                e.call(e.lower(e.fld(e.v("js"), "var_fn")), e.rec(e.p("mod", e.v("m")), e.p("name", e.s("imports"))))));
        body.add(e.callM(e.v("m"), "run", e.stage(e.mb.set(e.lower(e.v("imports")), e.rec(e.p("webidl", e.rec()))))));
        body.add(e.foreach(
                e.fld(e.v("arg"), "deps"),
                "dep",
                e.matchOf(
                        e.callM0(e.v("dep"), "k"),
                        "p",
                        Opt.none(),
                        e.p(
                                "import",
                                e.ifOnly(
                                        e.eq(p(e, "module"), e.s(EcoWebidlWasm.MODULE)),
                                        e.callM(
                                                e.v("m"),
                                                "assign_index",
                                                e.rec(
                                                        e.p("target", e.s("imports.webidl")),
                                                        e.p("key", p(e, "name")),
                                                        e.p(
                                                                "value",
                                                                e.call(
                                                                        e.lower(e.fld(e.v("webidl"), "define")),
                                                                        e.rec(
                                                                                e.p("mod", e.v("m")),
                                                                                e.p("name", p(e, "name"))))))))))));
        body.add(e.let(
                "ready",
                e.call(
                        e.lower(e.fld(e.v("js"), "func_fn")),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("ready")),
                                e.p("params", e.tup(e.s("r"))),
                                e.p(
                                        "body",
                                        e.stage(e.seq(
                                                e.mb.set(e.lower(e.v("instance")), e.fld(e.v("r"), "instance")),
                                                e.call(
                                                        e.fld(e.lower(e.v("instance")), "exports", "_start"),
                                                        e.tup()))))))));
        body.add(e.callM(
                e.v("m"),
                "run",
                e.stage(e.call(
                        e.fld(
                                e.call(
                                        e.fld(e.lower(e.v("WebAssembly")), "instantiateStreaming"),
                                        e.tup(
                                                e.call(e.lower(e.v("fetch")), e.s("main.wasm")),
                                                e.lower(e.v("imports")))),
                                "then"),
                        e.lower(e.v("ready"))))));
        body.add(e.let("out", e.callM0(e.v("m"), "render")));
        body.add(e.ret(e.call(
                e.mb.builtin("dir"),
                e.mapNew(
                        e.mb.typesString(),
                        bytesT(e),
                        e.p("index.html", e.call(e.mb.evalStrUtf8(), e.s(HTML))),
                        e.p("main.js", e.call(e.mb.evalStrUtf8(), e.fld(e.v("out"), "js"))),
                        e.p("main.wasm", e.v("wasm_bytes"))))));
        top.add(e.funcImpl(
                e.v("build_b"),
                e.rec(e.p("wat", e.mb.typesString()), e.p("deps", e.listType(e.fld(e.v("wasm"), "dep_t")))),
                e.optionTypeSome(e.mb.builtin("types", "dir")),
                e.mb.optNoneType(),
                e.stage(e.mb.seq(body))));
        top.add(e.finish(e.v("d")));
        top.add(e.ret(e.rec(e.p("build", e.fld(e.v("build_b"), "func")))));
        return top;
    }
}
