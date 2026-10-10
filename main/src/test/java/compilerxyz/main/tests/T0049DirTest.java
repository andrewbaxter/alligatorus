package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.builtin.Dir;
import compilerxyz.main.langhelp.ArtifactResolve;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Main;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0049DirTest {

    @Test
    public void test00000ExportAndWrite(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var main = tmp.resolve("main.at");
        T0047FileTest.writeModule(
                main,
                TList.of(e.ret(e.call(
                        e.mb.builtin("dir"),
                        e.mapNew(
                                e.mb.typesString(),
                                e.call(e.mb.typesArray(), e.mb.typesByte()),
                                e.p("index.html", T0048ExecTest.bytesOf(e, "<h1>hi</h1>\n")),
                                e.p("js/main.js", T0048ExecTest.bytesOf(e, "console.log(1);\n")))))));
        Dir dir;
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache, cache),
                    new SourceSpecAbsolute.Local(main.toString()));
            Compile.CompileResult out =
                    switch (res) {
                        case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> ok.value();
                        case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                            throw new AssertionError("compile failed: " + err.error());
                    };
            assertTrue(out.artifact().optIsSome(), "root module exported nothing");
            dir = switch (ArtifactResolve.artifactResolveDir(
                    cache, out.artifact().optUnwrap())) {
                case Result.Ok<Dir, String> ok -> ok.value();
                case Result.Err<Dir, String> err -> throw new AssertionError("not a dir: " + err.error());
            };
        }
        assertEquals(2, dir.files().size());
        assertArrayEquals(
                "<h1>hi</h1>\n".getBytes(StandardCharsets.UTF_8),
                dir.files().getOpt("index.html").optUnwrap());
        var out = tmp.resolve("out");
        Main.writeDir(dir, out);
        assertEquals("<h1>hi</h1>\n", Files.readString(out.resolve("index.html")));
        assertEquals("console.log(1);\n", Files.readString(out.resolve("js/main.js")));
    }
}
