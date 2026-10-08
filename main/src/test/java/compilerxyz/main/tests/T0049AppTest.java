package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.builtin.App;
import compilerxyz.main.langhelp.ArtifactResolve;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Main;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0049AppTest {
    private static final String SCRIPT = "#!/bin/sh\nexit 7\n";

    static App compileApp(Path cacheDir, Path entry) {
        try (var cache = new CacheReal(cacheDir)) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
            Compile.CompileResult out =
                    switch (res) {
                        case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> ok.value();
                        case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                            throw new AssertionError("compile failed: " + err.error());
                    };
            assertTrue(out.artifact().optIsSome(), "root module exported nothing");
            return switch (ArtifactResolve.artifactResolveApp(
                    cache, out.artifact().optUnwrap())) {
                case Result.Ok<App, String> ok -> ok.value();
                case Result.Err<App, String> err -> throw new AssertionError("not an app: " + err.error());
            };
        }
    }

    private static SourceExpr appOf(TestMbShorthand e, SourceExpr executor, SourceExpr bytes) {
        return e.call(e.mb.builtin("app"), e.rec(e.p("executor", executor), e.p("bytes", bytes)));
    }

    @Test
    public void test00000ExportAndRun(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var main = tmp.resolve("main.at");
        T0047FileTest.writeModule(
                main, TList.of(e.ret(appOf(e, e.mb.optNoneString(), T0048ExecTest.bytesOf(e, SCRIPT)))));
        var cacheDir = tmp.resolve("cache");
        var app = compileApp(cacheDir, main);
        assertTrue(app.executor().optIsNone());
        assertArrayEquals(SCRIPT.getBytes(StandardCharsets.UTF_8), app.bytes());
        assertEquals(7, Main.runApp(app, new String[] {}));
        var cached = compileApp(cacheDir, main);
        assertArrayEquals(SCRIPT.getBytes(StandardCharsets.UTF_8), cached.bytes());
    }

    @Test
    public void test00100Executor(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var main = tmp.resolve("main.at");
        T0047FileTest.writeModule(
                main,
                TList.of(e.ret(appOf(
                        e, e.optSome(e.mb.typesString(), e.s("sh")), T0048ExecTest.bytesOf(e, "exit \"$1\"\n")))));
        var app = compileApp(tmp.resolve("cache"), main);
        assertEquals("sh", app.executor().optUnwrap());
        assertEquals(5, Main.runApp(app, new String[] {"5"}));
    }
}
