package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.langhelp.serve.ServeExport;
import compilerxyz.main.langserver.SourceLocal;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A marker the ide recognizes is an object defined in source, not a builtin: here a module defines
 * `editor` holding a dir, another module exports an instance, and the language server describes the
 * export by the object's name and reads a dir entry out of it by path.
 */
public final class T0222ServeExportTest {
    private static final String WASM = "\0asm\1\0\0\0";

    @Test
    public void test00000DescribeAndRead(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var dirType = e.mb.builtin("types", "dir");
        var lib = tmp.resolve("editor.at");
        T0047FileTest.writeModule(
                lib,
                TList.of(e.ret(e.lower(e.seq(
                        e.let("d", e.define()),
                        e.let("editor_o", e.defObject(e.v("d"), "editor")),
                        e.objectImpl(e.v("editor_o"), e.rec(e.p("files", dirType))),
                        e.let("build_b", e.defFunc(e.v("d"), "build")),
                        e.funcImpl(
                                e.v("build_b"),
                                dirType,
                                e.retSome(e.fld(e.v("editor_o"), "type")),
                                e.mb.optNoneType(),
                                e.stage(e.call(e.lower(e.fld(e.v("editor_o"), "new")), e.rec(e.p("files", e.v("arg")))))),
                        e.finish(e.v("d")),
                        e.ret(e.rec(e.p("build", e.fld(e.v("build_b"), "func")))))))));
        var main = tmp.resolve("main.at");
        T0047FileTest.writeModule(
                main,
                TList.of(
                        e.let("editor", e.importLocal("editor", "editor.at")),
                        e.ret(e.call(
                                e.fld(e.v("editor"), "build"),
                                e.call(
                                        e.mb.builtin("dir"),
                                        e.mapNew(
                                                e.mb.typesString(),
                                                e.call(e.mb.typesArray(), e.mb.typesByte()),
                                                e.p("main.wasm", T0048ExecTest.bytesOf(e, WASM)),
                                                e.p("notes.txt", T0048ExecTest.bytesOf(e, "hi\n"))))))));
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(main.toString()));
            Compile.CompileResult out =
                    switch (res) {
                        case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> ok.value();
                        case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                            throw new AssertionError("compile failed: " + err.error());
                    };
            assertTrue(out.artifact().optIsSome(), "root module exported nothing");
            Artifact artifact = out.artifact().optUnwrap();
            var export = ServeExport.exportDescribe(cache, artifact);
            assertTrue(export.optIsSome(), "export not described");
            assertEquals("editor", export.optUnwrap().typeName().optUnwrap());
            assertEquals(
                    new SourceLocal(lib.toString()),
                    export.optUnwrap().typeSource().optUnwrap());
            var read = ServeExport.exportRead(cache, artifact, TList.of("files", "main.wasm"));
            switch (read) {
                case Result.Ok<byte[], String> ok ->
                    assertArrayEquals(WASM.getBytes(StandardCharsets.UTF_8), ok.value());
                case Result.Err<byte[], String> err -> throw new AssertionError("read failed: " + err.error());
            }
            var missing = ServeExport.exportRead(cache, artifact, TList.of("files", "nope"));
            switch (missing) {
                case Result.Ok<byte[], String> ok -> throw new AssertionError("read a missing entry");
                case Result.Err<byte[], String> err -> assertEquals("the dir at /files has no entry nope", err.error());
            }
        }
    }
}
