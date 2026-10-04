package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.stream.JsonWriter;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.gen.codec.SourceModuleCodec;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.safejson.Json;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0047FileTest {
    private static void writeModule(Path file, TList<SourceExpr> exprs) throws IOException {
        var sw = new StringWriter();
        var writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        SourceModuleCodec.encode(writer, new SourceModule.V1(exprs), Json.Style.COMPACT);
        writer.close();
        Files.writeString(file, sw.toString());
    }

    private static Result<Compile.CompileResult, TList<ExprErr>> compile(Path cacheDir, Path entry) {
        try (var cache = new CacheReal(cacheDir)) {
            return Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
        }
    }

    private static Compile.CompileResult expectOk(Result<Compile.CompileResult, TList<ExprErr>> res) {
        return switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> ok.value();
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                throw new AssertionError("compile failed: " + err.error());
        };
    }

    private static TList<ExprErr> expectErr(Result<Compile.CompileResult, TList<ExprErr>> res) {
        return switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok ->
                throw new AssertionError(
                        "expected failure; got logs " + ok.value().logs());
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> err.error();
        };
    }

    private static SourceExpr open(TestMb mb, String path) {
        return mb.call(mb.builtin("file", "open"), mb.str(path));
    }

    private static SourceExpr logStr(TestMb mb, SourceExpr value) {
        return mb.call(mb.log(), mb.call(mb.field(value, mb.str("to_string")), mb.tupleOf()));
    }

    private static SourceExpr read(TestMb mb, SourceExpr handle, double offset, double len) {
        return mb.call(
                mb.field(handle, mb.str("read")),
                mb.recordOf(TList.of(
                        new TestMb.RecordPair("offset", mb.num(offset)), new TestMb.RecordPair("len", mb.num(len)))));
    }

    @Test
    public void test00000ReadLocalFile(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src.resolve("sub"));
        Files.write(src.resolve("sub").resolve("data.bin"), new byte[] {1, 2, 3, (byte) 250});
        var main = src.resolve("main.at");
        writeModule(
                main,
                TList.of(
                        logStr(mb, mb.field(open(mb, "sub/data.bin"), mb.str("len"))),
                        mb.bind("b", read(mb, open(mb, "sub/data.bin"), 1, 3)),
                        logStr(mb, mb.field(mb.scopeUse("b"), mb.str("len"))),
                        mb.foreach(mb.scopeUse("b"), mb.str("e"), mb.seqOf(logStr(mb, mb.scopeUse("e"))))));
        var logs = expectOk(compile(tmp.resolve("cache"), main)).logs();
        assertEquals(5, logs.size());
        assertEquals("4", logs.get(0).message());
        assertEquals("3", logs.get(1).message());
        assertEquals("2", logs.get(2).message());
        assertEquals("3", logs.get(3).message());
        assertEquals("-6", logs.get(4).message());
    }

    @Test
    public void test00050HandleStaysInItsEvaluation(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        Files.write(tmp.resolve("data.bin"), new byte[] {1, 2, 3});
        var main = tmp.resolve("main.at");
        writeModule(
                main, TList.of(mb.bind("f", open(mb, "data.bin")), mb.bind("s", mb.stage(mb.lower(mb.scopeUse("f"))))));
        var errs = expectErr(compile(tmp.resolve("cache"), main));
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
    }

    @Test
    public void test00100MissingFile(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var main = tmp.resolve("main.at");
        var openCall = open(mb, "nope.bin");
        writeModule(main, TList.of(logStr(mb, mb.field(openCall, mb.str("len")))));
        var errs = expectErr(compile(tmp.resolve("cache"), main));
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        assertEquals(openCall.id().value(), errs.get(0).loc().expr().optUnwrap());
        assertTrue(
                errs.get(0).message().contains("file.open: local file not found"),
                () -> "unexpected message: " + errs.get(0).message());
    }

    @Test
    public void test00200ReadOutOfRange(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        Files.write(tmp.resolve("data.bin"), new byte[] {1, 2, 3});
        var main = tmp.resolve("main.at");
        var readCall = read(mb, open(mb, "data.bin"), 2, 2);
        writeModule(main, TList.of(logStr(mb, mb.field(readCall, mb.str("len")))));
        var errs = expectErr(compile(tmp.resolve("cache"), main));
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        assertEquals(readCall.id().value(), errs.get(0).loc().expr().optUnwrap());
        assertTrue(
                errs.get(0).message().contains("file.read: range [2, 4) outside file of length 3"),
                () -> "unexpected message: " + errs.get(0).message());
    }

    @Test
    public void test00300DataFileInvalidatesCache(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var data = tmp.resolve("data.bin");
        Files.write(data, new byte[] {1, 2, 3});
        var main = tmp.resolve("main.at");
        writeModule(main, TList.of(logStr(mb, mb.field(open(mb, "data.bin"), mb.str("len")))));
        var cacheDir = tmp.resolve("cache");
        assertEquals("3", expectOk(compile(cacheDir, main)).logs().get(0).message());
        assertEquals("3", expectOk(compile(cacheDir, main)).logs().get(0).message());
        Files.write(data, new byte[] {1, 2, 3, 4, 5});
        Files.setLastModifiedTime(
                data, FileTime.fromMillis(Files.getLastModifiedTime(data).toMillis() + 100000));
        assertEquals("5", expectOk(compile(cacheDir, main)).logs().get(0).message());
    }
}
