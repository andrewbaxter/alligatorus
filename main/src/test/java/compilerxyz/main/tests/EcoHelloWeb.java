package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.nio.charset.StandardCharsets;

public final class EcoHelloWeb {
    private EcoHelloWeb() {}

    public static final String TAG = "h1";
    public static final String MESSAGE = "Hello world";

    private static SourceExpr len(EcoMb e, String s) {
        return e.n(s.getBytes(StandardCharsets.UTF_8).length);
    }

    private static SourceExpr hook(EcoMb e, String kind, String iface, String member) {
        return e.call(e.fld(e.v("webidl"), kind), e.rec(e.p("iface", e.s(iface)), e.p("member", e.s(member))));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        setup.add(e.let("m", e.call(e.fld(e.v("wasm"), "new"), e.tup())));
        EcoWat.bindTypes(e, setup);
        setup.add(e.let(
                "start_fn_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType())), e.p("ret", e.v("t_void"))))));
        setup.add(e.ret(e.rec(e.p("m", e.v("m")), e.p("start_fn_t", e.v("start_fn_t")), e.p("t_i32", e.v("t_i32")))));

        var top = new TList<SourceExpr>();
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("webidl", e.importLocal("webidl", "webidl-wasm.atz")));
        top.add(e.let("web", e.importLocal("web", "web-wasm.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        top.add(e.let("m", e.fld(e.v("k"), "m")));
        top.add(e.let("tag", e.callM(e.v("m"), "cstr", e.s(TAG))));
        top.add(e.let("text", e.callM(e.v("m"), "cstr", e.s(MESSAGE))));
        top.add(e.let(
                "cell",
                e.call(
                        e.fld(e.v("wasm"), "data_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("cell")),
                                e.p("elem", e.fld(e.v("k"), "t_i32")),
                                e.p("values", e.tup(e.n(0), e.n(0)))))));
        top.add(e.let("document", hook(e, "get", "Window", "document")));
        top.add(e.let("create", hook(e, "op", "Document", "createElement")));
        top.add(e.let("set_text", hook(e, "set", "Node", "textContent")));
        top.add(e.let("body", hook(e, "get", "Document", "body")));
        top.add(e.let("append", hook(e, "op", "Node", "appendChild")));
        top.add(e.let(
                "start",
                e.call(
                        e.fld(e.v("wasm"), "func_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("_start")),
                                e.p("params", e.tup()),
                                e.p("ty", e.fld(e.v("k"), "start_fn_t")),
                                e.p(
                                        "body",
                                        e.stage(e.seq(
                                                e.let("win", e.call(e.lower(e.fld(e.v("webidl"), "global")), e.tup())),
                                                e.let("doc", e.call(e.lower(e.v("document")), e.v("win"))),
                                                e.let(
                                                        "h1",
                                                        e.call(
                                                                e.lower(e.v("create")),
                                                                e.tup(
                                                                        e.v("doc"),
                                                                        e.lower(e.v("tag")),
                                                                        len(e, TAG),
                                                                        e.n(0),
                                                                        e.n(0)))),
                                                e.call(
                                                        e.lower(e.v("set_text")),
                                                        e.tup(
                                                                e.v("h1"),
                                                                e.n(1),
                                                                e.lower(e.v("text")),
                                                                len(e, MESSAGE))),
                                                e.call(e.lower(e.v("body")), e.tup(e.v("doc"), e.lower(e.v("cell")))),
                                                e.call(
                                                        e.lower(e.v("append")),
                                                        e.tup(
                                                                e.mb.field(e.lower(e.v("cell")), e.n(1)),
                                                                e.v("h1"))))))))));
        top.add(e.callM(e.v("m"), "export", e.s("_start")));
        top.add(e.ret(e.call(e.fld(e.v("web"), "build"), e.callM0(e.v("m"), "render"))));
        return top;
    }
}
