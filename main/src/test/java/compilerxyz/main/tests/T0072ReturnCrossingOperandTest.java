package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0072ReturnCrossingOperandTest {
    @Test
    void returnInsideCallArgument(@TempDir Path tmp) throws IOException {
        run(tmp, false);
    }

    @Test
    void returnInsideCallArgumentWithTemporary(@TempDir Path tmp) throws IOException {
        run(tmp, true);
    }

    private static void run(Path tmp, boolean pendingTemporary) throws IOException {
        var e = new EcoMb();
        var strT = e.mb.typesString();
        var defs = new TList<SourceExpr>();
        defs.add(e.let("d", e.define()));
        defs.add(e.let("gb", e.defFunc(e.v("d"), "g")));
        defs.add(e.funcImpl(
                e.v("gb"),
                e.mb.typesBool(),
                e.retSome(strT),
                e.mb.optNoneType(),
                e.stage(e.mb.labeled(
                        "m",
                        e.seq(
                                e.let("a", e.cat(e.s("a"), e.s(""))),
                                e.let(
                                        "r",
                                        e.cat(
                                                pendingTemporary ? e.cat(e.v("a"), e.s("")) : e.v("a"),
                                                e.cat(
                                                        e.s("+"),
                                                        e.ifElse(
                                                                e.v("arg"),
                                                                e.mb.return_("m", e.cat(e.s("early"), e.s(""))),
                                                                e.cat(e.s("late"), e.s("")))))),
                                e.mb.return_("m", e.v("r")))))));
        defs.add(e.finish(e.v("d")));
        defs.add(e.ret(e.fld(e.v("gb"), "func")));
        var top = new TList<SourceExpr>();
        top.add(e.let("g", e.lower(e.mb.seq(defs))));
        for (var flag : new boolean[] {false, true}) {
            top.add(e.call(e.mb.log(), e.call(e.v("g"), e.b(flag))));
        }
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        T0200EcosystemLlvmTest.writeModule(src.resolve("main.at"), top);
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache),
                    new SourceSpecAbsolute.Local(src.resolve("main.at").toString()));
            Result.Ok<Compile.CompileResult, ?> ok;
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
                case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
            }
            var messages = new TList<String>();
            for (var entry : ok.value().logs()) messages.add(entry.message());
            assertEquals(TList.of("a+late", "early"), messages);
        }
    }
}
