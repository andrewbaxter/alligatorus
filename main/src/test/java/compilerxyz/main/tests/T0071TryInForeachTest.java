package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0071TryInForeachTest {
    private static SourceExpr tryCall(TestMbShorthand e, SourceExpr errsT, SourceExpr boxT) {
        return e.matchOf(
                e.mb.tryCatchFinally(
                        e.eresultOk(boxT, e.call(e.lower(e.fld(e.v("fb"), "func")), e.v("arg"))),
                        Opt.some(new TestMb.Catch("__errs", errsT, e.eresultErrs(boxT, e.v("__errs")))),
                        Opt.none()),
                "__r",
                e.p("ok", e.v("__r")),
                e.p("errs", e.mb.return_("m", e.eresultErrs(boxT, e.v("__r")))));
    }

    private static void run(Path tmp, String label, boolean inLoop) throws IOException {
        var e = new TestMbShorthand();
        var errsT = e.listType(e.mb.builtin("types", "expr_err"));
        var intT = e.mb.typesInt();
        var boxT = e.mb.typesIntBox();
        var locExpr = e.tup();
        var erInt = e.call(e.mb.typesEresult(), e.mb.typesIntBox());
        var defs = new TList<SourceExpr>();
        defs.add(e.let("d", e.define()));
        defs.add(e.let("fb", e.defFunc(e.v("d"), "f")));
        defs.add(e.funcImpl(
                e.v("fb"),
                e.mb.typesBool(),
                e.retSome(intT),
                e.mb.optSomeType(errsT),
                e.stage(e.seq(
                        e.let("loc", e.call(e.evalBuiltin("expr_loc"), e.stage(locExpr))),
                        e.ifOnly(
                                e.v("arg"),
                                e.matchOf(
                                        e.eresultErr(boxT, e.v("loc"), e.s("boom")),
                                        "__x",
                                        e.p("ok", e.mb.panic(e.s("unreachable"))),
                                        e.p("errs", e.mb.throw_(e.v("__x"))))),
                        e.ret(e.mb.as(e.n(7), intT))))));
        defs.add(e.let("gb", e.defFunc(e.v("d"), "g")));
        SourceExpr middle = inLoop
                ? e.foreach(
                        e.v("ones"),
                        "__i",
                        e.seq(e.let("r0", tryCall(e, errsT, boxT)), e.push(e.v("out"), e.toStr(e.v("r0")))))
                : e.let("r0", tryCall(e, errsT, boxT));
        defs.add(e.funcImpl(
                e.v("gb"),
                e.mb.typesBool(),
                e.retSome(erInt),
                e.mb.optNoneType(),
                e.stage(e.mb.labeled(
                        "m",
                        e.seq(
                                e.let("out", e.listNew(e.mb.typesString())),
                                e.let("ones", e.listNew(e.mb.typesString(), e.s("one"))),
                                middle,
                                e.push(e.v("out"), e.s("x")),
                                e.mb.return_("m", e.eresultOk(boxT, e.mb.as(e.n(1), intT))))))));
        defs.add(e.finish(e.v("d")));
        defs.add(e.ret(e.fld(e.v("gb"), "func")));
        var top = new TList<SourceExpr>();
        top.add(e.let("g", e.lower(e.mb.seq(defs))));
        for (var flag : new boolean[] {false, true}) {
            top.add(e.matchOf(
                    e.call(e.v("g"), e.b(flag)),
                    "__u",
                    e.p("ok", e.call(e.mb.log(), e.cat(e.s("ok "), e.toStr(e.v("__u"))))),
                    e.p(
                            "errs",
                            e.foreach(
                                    e.v("__u"), "__e", e.call(e.mb.log(), e.callM(e.v("__e"), "message", e.tup()))))));
        }
        var src = tmp.resolve("src_" + label);
        Files.createDirectories(src);
        Helpers.writeModule(src.resolve("main.at"), top);
        try (var cache = new CacheReal(tmp.resolve("cache_" + label))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache, cache),
                    new SourceSpecAbsolute.Local(src.resolve("main.at").toString()));
            Result.Ok<Compile.CompileResult, ?> ok;
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
                case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
            }
            var messages = new TList<String>();
            for (var entry : ok.value().logs()) messages.add(entry.message());
            var sourceId =
                    switch (cache.sourcesGet(
                            new SourceSpecAbsolute.Local(src.resolve("main.at").toString()))) {
                        case Result.Ok<Sources.SourceResolved, String> resolved ->
                            switch (resolved.value()) {
                                case Sources.SourceResolved.Module m -> m.id().value();
                                case Sources.SourceResolved.Bundle b -> throw new AssertionError("main.at is a bundle");
                                case Sources.SourceResolved.File f ->
                                    throw new AssertionError("main.at is not a module");
                            };
                        case Result.Err<Sources.SourceResolved, String> err ->
                            throw new AssertionError("main.at does not resolve: " + err.error());
                    };
            assertEquals(TList.of("ok 1", "[" + sourceId + ":" + locExpr.id().value() + "] boom"), messages);
        }
    }

    @Test
    void plain(@TempDir Path tmp) throws IOException {
        run(tmp, "plain", false);
    }

    @Test
    void loop(@TempDir Path tmp) throws IOException {
        run(tmp, "loop", true);
    }
}
