package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.nio.charset.StandardCharsets;

public final class EcoHelloWasi {
    private EcoHelloWasi() {}

    public static final String MESSAGE = "hello wasi!\n";

    public enum Mode {
        APP,
        WAT
    }

    public static TList<SourceExpr> module(EcoMb e, Mode mode) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        setup.add(e.let("wasi", e.importLocal("wasi", "wasi-0.1.atz")));
        setup.add(e.let("m", e.call(e.fld(e.v("wasm"), "new"), e.tup())));
        EcoWat.bindTypes(e, setup);
        setup.add(e.let("fd_write_fn_t", e.callM(e.fld(e.v("wasi"), "fn_types"), "get", e.s("fd_write"))));
        setup.add(e.let(
                "start_fn_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType())), e.p("ret", e.v("t_void"))))));
        var exported = new String[] {"m", "fd_write_fn_t", "start_fn_t", "t_u32"};
        var exportedFields = new TList<EcoMb.P>();
        for (var n : exported) exportedFields.add(e.p(n, e.v(n)));
        setup.add(e.ret(e.rec(exportedFields.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("wasi", e.importLocal("wasi", "wasi-0.1.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : exported) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let(
                "fd_write",
                e.call(
                        e.fld(e.v("wasm"), "import_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("module", e.fld(e.v("wasi"), "module")),
                                e.p("name", e.s("fd_write")),
                                e.p("ty", e.v("fd_write_fn_t"))))));
        top.add(e.let("hello", e.callM(e.v("m"), "cstr", e.s(MESSAGE))));
        top.add(e.let(
                "iov",
                e.call(
                        e.fld(e.v("wasm"), "data_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("iov")),
                                e.p("elem", e.v("t_u32")),
                                e.p(
                                        "values",
                                        e.tup(e.v("hello"), e.n(MESSAGE.getBytes(StandardCharsets.UTF_8).length)))))));
        top.add(e.let(
                "nwritten",
                e.call(
                        e.fld(e.v("wasm"), "data_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("nwritten")),
                                e.p("elem", e.v("t_u32")),
                                e.p("values", e.tup(e.n(0)))))));
        top.add(e.let(
                "start",
                e.call(
                        e.fld(e.v("wasm"), "func_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("_start")),
                                e.p("params", e.tup()),
                                e.p("ty", e.v("start_fn_t")),
                                e.p(
                                        "body",
                                        e.stage(e.seq(e.call(
                                                e.lower(e.v("fd_write")),
                                                e.tup(
                                                        e.n(1),
                                                        e.lower(e.v("iov")),
                                                        e.n(1),
                                                        e.lower(e.v("nwritten")))))))))));
        top.add(e.callM(e.v("m"), "export", e.s("_start")));
        top.add(
                switch (mode) {
                    case APP -> e.ret(e.call(e.fld(e.v("wasm"), "build"), e.callM0(e.v("m"), "render")));
                    case WAT -> e.call(e.mb.log(), e.fld(e.callM0(e.v("m"), "render"), "wat"));
                });
        return top;
    }
}
