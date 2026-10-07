package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.luben.zstd.Zstd;
import com.google.gson.stream.JsonWriter;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.gen.codec.SourceModuleCodec;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.safejson.Json;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0200EcosystemLlvmTest {

    static byte[] serializeModuleZ(TList<SourceExpr> exprs) throws IOException {
        var sw = new StringWriter();
        var writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        writer.setIndent("");
        SourceModuleCodec.encode(writer, new SourceModule.V1(exprs), Json.Style.COMPACT);
        writer.close();
        return Zstd.compress(sw.toString().getBytes(StandardCharsets.UTF_8), ZSTD_LEVEL);
    }

    static final int ZSTD_LEVEL = 19;

    static void writeModule(Path file, TList<SourceExpr> exprs) throws IOException {
        if (file.getFileName().toString().endsWith(".atz")) {
            Files.write(file, serializeModuleZ(exprs));
        } else {
            var sw = new StringWriter();
            var writer = new JsonWriter(sw);
            writer.setSerializeNulls(true);
            writer.setIndent("  ");
            SourceModuleCodec.encode(writer, new SourceModule.V1(exprs), Json.Style.PRETTY);
            writer.close();
            var text = sw.toString();
            Files.writeString(file, text);
        }
    }

    static Compile.CompileResult compileOk(Path entry, Path cacheDir) {
        try (var cache = new CacheReal(cacheDir)) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
            Result.Ok<Compile.CompileResult, ?> ok;
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
                case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
            }
            return ok.value();
        }
    }

    static void assertValidIr(Path tmp, String name, String ir) throws IOException {
        var llvmAs = System.getenv("PATH");
        boolean available = false;
        if (llvmAs != null) {
            for (var dir : llvmAs.split(File.pathSeparator)) {
                if (Files.isExecutable(Path.of(dir, "llvm-as"))) {
                    available = true;
                    break;
                }
            }
        }
        if (!available) return;
        var file = tmp.resolve(name + ".ll");
        Files.writeString(file, ir);
        Process proc;
        try {
            proc = new ProcessBuilder("llvm-as", file.toString(), "-o", "/dev/null")
                    .redirectErrorStream(true)
                    .start();
            var output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var code = proc.waitFor();
            Assertions.assertEquals(0, code, () -> "llvm-as rejected " + name + ":\n" + output + "\n" + ir);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private static SourceExpr fnType(EcoMb e, String ret, String... params) {
        var types = new TList<SourceExpr>();
        for (var p : params) types.add(e.v(p));
        return e.callM(
                e.v("llvm"),
                "fn",
                e.rec(
                        e.p("params", e.listNew(e.mb.evalType(), types.toArray(SourceExpr[]::new))),
                        e.p("ret", e.v(ret))));
    }

    private static SourceExpr func(EcoMb e, String name, String fnType, SourceExpr body, String... params) {
        var names = new TList<SourceExpr>();
        for (var p : params) names.add(e.s(p));
        return e.let(
                name,
                e.call(
                        e.fld(e.v("llvm"), "func_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s(name)),
                                e.p("params", e.tup(names.toArray(SourceExpr[]::new))),
                                e.p("ty", e.v(fnType)),
                                e.p("body", e.stage(body)))));
    }

    @Test
    public void test00000HelloWorldIr(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        var hello = "hello ecosys!\n".getBytes(StandardCharsets.UTF_8);
        var helloInit = new SourceExpr[hello.length + 1];
        for (int i = 0; i < hello.length; i++) helloInit[i] = e.n(hello[i] & 0xFF);
        helloInit[hello.length] = e.n(0);
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        EcoExtern.bindTypes(e, setup, EcoExtern.Llvm.top(e));
        var puts = new EcoExtern("puts", IList.of("pu8"), "i32", false);
        setup.add(e.let("puts_fn_t", puts.fnType(e, EcoExtern.Llvm.top(e))));
        setup.add(e.let("main_fn_t", fnType(e, "t_i32")));
        setup.add(e.let("hello_arr_t", EcoPipewire.byteArrayType(e, EcoExtern.Llvm.top(e), e.intOf(15))));
        var exported = new TList<String>();
        exported.add("ll");
        exported.add("puts_fn_t");
        exported.add("main_fn_t");
        exported.add("hello_arr_t");
        for (var t : new String[] {"i1", "i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void", "pv", "pu8"}) {
            exported.add("t_" + t);
        }
        var exportedFields = new TList<EcoMb.P>();
        for (var n : exported) exportedFields.add(e.p(n, e.v(n)));
        setup.add(e.ret(e.rec(exportedFields.toArray(EcoMb.P[]::new))));
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : exported) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("puts", puts.declare(e, e.fld(e.v("llvm"), "extern_fn"), e.v("puts_fn_t"))));
        top.add(e.let(
                "hello",
                e.call(
                        e.fld(e.v("llvm"), "global_const_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("str.hello")),
                                e.p("ty", e.v("hello_arr_t")),
                                e.p("init", e.tup(helloInit))))));
        top.add(func(
                e, "main", "main_fn_t", e.seq(e.call(e.lower(e.v("puts")), e.lower(e.v("hello"))), e.ret(e.n(0)))));
        top.add(e.call(e.mb.log(), e.fld(e.callM(e.v("ll"), "render", e.tup()), "ir")));
        writeModule(src.resolve("main.at"), top);

        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        var helloSb = new StringBuilder("[");
        var helloRaw = "hello ecosys!\n".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < helloRaw.length; i++) {
            if (i > 0) helloSb.append(", ");
            helloSb.append("i8 ").append(helloRaw[i] & 0xFF);
        }
        var helloBytes = helloSb.append(", i8 0]").toString();
        assertEquals(
                """
                declare i32 @puts(ptr)
                @str.hello = constant [15 x i8] %s
                define i32 @main() {
                  %%t0 = call i32 @puts(ptr @str.hello)
                  ret i32 0
                }
                """
                        .formatted(helloBytes),
                ir);
        assertValidIr(tmp, "hello", ir);
    }

    private static Path writeListInitModule(Path tmp, String elementType) throws IOException {
        var e = new EcoMb();
        var elemT = e.mb.builtin("types", elementType);
        return writeArrayInitModule(
                tmp,
                e,
                4,
                TList.of(
                        e.let(
                                "bytes",
                                e.listNew(
                                        elemT,
                                        e.mb.as(e.n('h'), elemT),
                                        e.mb.as(e.n('i'), elemT),
                                        e.mb.as(e.n('\n'), elemT))),
                        e.push(e.v("bytes"), e.mb.as(e.n(0), elemT))),
                e.v("bytes"));
    }

    private static Path writeArrayInitModule(Path tmp, EcoMb e, int count, TList<SourceExpr> preInit, SourceExpr init)
            throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        EcoExtern.bindTypes(e, setup, EcoExtern.Llvm.top(e));
        var puts = new EcoExtern("puts", IList.of("pu8"), "i32", false);
        setup.add(e.let("puts_fn_t", puts.fnType(e, EcoExtern.Llvm.top(e))));
        setup.add(e.let("main_fn_t", fnType(e, "t_i32")));
        setup.add(e.let("hi_arr_t", EcoPipewire.byteArrayType(e, EcoExtern.Llvm.top(e), e.intOf(Math.max(count, 0)))));
        var exported = new TList<String>();
        exported.add("ll");
        exported.add("puts_fn_t");
        exported.add("main_fn_t");
        exported.add("hi_arr_t");
        setup.add(e.let("hi_def", e.call(e.fld(e.v("llvm"), "const_list"), e.v("t_u8"))));
        exported.add("hi_def");
        for (var t : new String[] {"i1", "i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void", "pv", "pu8"}) {
            exported.add("t_" + t);
        }
        var exportedFields = new TList<EcoMb.P>();
        for (var n : exported) exportedFields.add(e.p(n, e.v(n)));
        setup.add(e.ret(e.rec(exportedFields.toArray(EcoMb.P[]::new))));
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : exported) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("puts", puts.declare(e, e.fld(e.v("llvm"), "extern_fn"), e.v("puts_fn_t"))));
        top.addAll(preInit);
        top.add(e.let(
                "hi",
                count < 0
                        ? e.call(
                                e.v("hi_def"),
                                e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("str.hi")), e.p("values", init)))
                        : e.call(
                                e.fld(e.v("llvm"), "global_const_fn"),
                                e.rec(
                                        e.p("mod", e.v("ll")),
                                        e.p("name", e.s("str.hi")),
                                        e.p("ty", e.v("hi_arr_t")),
                                        e.p("init", init)))));
        top.add(func(e, "main", "main_fn_t", e.seq(e.call(e.lower(e.v("puts")), e.lower(e.v("hi"))), e.ret(e.n(0)))));
        top.add(e.call(e.mb.log(), e.fld(e.callM(e.v("ll"), "render", e.tup()), "ir")));
        writeModule(src.resolve("main.at"), top);

        return src.resolve("main.at");
    }

    @Test
    public void test00050ListArrayInit(@TempDir Path tmp) throws IOException {
        var ok = compileOk(writeListInitModule(tmp, "ubyte"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                """
                declare i32 @puts(ptr)
                @str.hi = constant [4 x i8] [i8 104, i8 105, i8 10, i8 0]
                define i32 @main() {
                  %t0 = call i32 @puts(ptr @str.hi)
                  ret i32 0
                }
                """,
                ir);
        assertValidIr(tmp, "hi", ir);
    }

    @Test
    public void test00055ListConstDerivedLength(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var ubyte = e.mb.builtin("types", "ubyte");
        var entry = writeArrayInitModule(
                tmp,
                e,
                -1,
                TList.of(
                        e.let(
                                "bytes",
                                e.listNew(
                                        ubyte,
                                        e.mb.as(e.n('h'), ubyte),
                                        e.mb.as(e.n('i'), ubyte),
                                        e.mb.as(e.n('\n'), ubyte))),
                        e.push(e.v("bytes"), e.mb.as(e.n(0), ubyte))),
                e.v("bytes"));
        var ok = compileOk(entry, tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                """
                declare i32 @puts(ptr)
                @str.hi = constant [4 x i8] [i8 104, i8 105, i8 10, i8 0]
                define i32 @main() {
                  %t0 = call i32 @puts(ptr @str.hi)
                  ret i32 0
                }
                """,
                ir);
        assertValidIr(tmp, "hi", ir);
    }

    private static Path writeTypedDefinersModule(Path tmp, int mismatch) throws IOException {
        var e = new EcoMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
        var ubyte = e.mb.builtin("types", "ubyte");
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        EcoExtern.bindTypes(e, setup, EcoExtern.Llvm.top(e));
        setup.add(e.let("arr_t", EcoPipewire.byteArrayType(e, EcoExtern.Llvm.top(e), e.intOf(3))));
        setup.add(e.let("global_i32", e.call(e.fld(e.v("llvm"), "global_def"), e.v("t_i32"))));
        setup.add(e.ret(e.rec(
                e.p("ll", e.v("ll")),
                e.p("global_i32", e.v("global_i32")),
                e.p("arr_t", e.v("arr_t")),
                e.p("pi32", e.call(e.fld(e.v("llvm"), "ptr"), e.v("t_i32"))),
                e.p(
                        "pu8_arr2",
                        e.call(
                                e.fld(e.v("llvm"), "array"),
                                e.rec(e.p("elem", e.v("t_pu8")), e.p("count", e.intOf(2))))),
                e.p("pv", e.v("t_pv")),
                e.p("pu8", e.v("t_pu8")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        top.add(e.let("ll", e.fld(e.v("k"), "ll")));
        top.add(e.let("const_arr", e.call(e.fld(e.v("llvm"), "const_def"), e.fld(e.v("k"), "arr_t"))));
        top.add(e.let("global_arr", e.call(e.fld(e.v("llvm"), "global_def"), e.fld(e.v("k"), "arr_t"))));
        top.add(e.let("ptrs_i32", e.call(e.fld(e.v("llvm"), "const_list"), e.fld(e.v("k"), "pi32"))));
        top.add(e.let("ptrs_u8", e.call(e.fld(e.v("llvm"), "const_list"), e.fld(e.v("k"), "pu8"))));
        top.add(e.let("ptr_u8", e.call(e.fld(e.v("llvm"), "global_def"), e.fld(e.v("k"), "pu8"))));
        top.add(e.let("ptr_u8_arr2", e.call(e.fld(e.v("llvm"), "const_def"), e.fld(e.v("k"), "pu8_arr2"))));
        top.add(e.let(
                "a",
                e.call(
                        e.fld(e.v("k"), "global_i32"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("a")),
                                e.p("init", e.optSome(e.mb.typesInt(), e.intOf(7)))))));
        top.add(e.call(
                e.fld(e.v("k"), "global_i32"),
                e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("b")), e.p("init", e.optNone(e.mb.typesInt())))));
        top.add(e.let(
                "c",
                e.call(
                        e.v("const_arr"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("c")),
                                e.p(
                                        "init",
                                        e.optSome(
                                                e.listType(ubyte),
                                                e.listNew(
                                                        ubyte,
                                                        e.mb.as(e.n(1), ubyte),
                                                        e.mb.as(e.n(2), ubyte),
                                                        e.mb.as(e.n(3), ubyte))))))));
        top.add(e.call(
                e.v("global_arr"),
                e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("d")), e.p("init", e.optNone(e.listType(ubyte))))));
        var grefList = e.listType(e.fld(e.v("llvm"), "gref_t"));
        top.add(e.call(
                e.v("ptrs_i32"),
                e.rec(
                        e.p("mod", e.v("ll")),
                        e.p("name", e.s("pa")),
                        e.p("values", e.callM(grefList, "new", e.v("a"))))));
        top.add(e.call(
                e.v("ptrs_u8"),
                e.rec(
                        e.p("mod", e.v("ll")),
                        e.p("name", e.s("pc")),
                        e.p("values", e.callM(grefList, "new", e.v(mismatch == 1 ? "a" : "c"))))));
        var gref = e.fld(e.v("llvm"), "gref_t");
        top.add(e.call(
                e.v("ptr_u8"),
                e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("qc")), e.p("init", e.optSome(gref, e.v("c"))))));
        top.add(e.call(
                e.v("ptr_u8"), e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("qn")), e.p("init", e.optNone(gref)))));
        top.add(e.call(
                e.v("ptr_u8_arr2"),
                e.rec(
                        e.p("mod", e.v("ll")),
                        e.p("name", e.s("qcc")),
                        e.p("init", e.optSome(grefList, e.callM(grefList, "new", e.tup(e.v("c"), e.v("c"))))))));
        top.add(e.call(
                e.v("ptr_u8_arr2"),
                e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("qz")), e.p("init", e.optNone(grefList)))));
        top.add(e.call(
                e.fld(e.v("llvm"), "global_fn"),
                e.rec(
                        e.p("mod", e.v("ll")),
                        e.p("name", e.s("qm")),
                        e.p("ty", e.fld(e.v("k"), "pu8")),
                        e.p("init", e.v(mismatch == 2 ? "a" : "c")))));
        top.add(e.call(
                e.fld(e.v("llvm"), "global_fn"),
                e.rec(
                        e.p("mod", e.v("ll")),
                        e.p("name", e.s("qv")),
                        e.p("ty", e.fld(e.v("k"), "pv")),
                        e.p("init", e.v("a")))));
        top.add(e.call(e.mb.log(), e.fld(e.callM(e.v("ll"), "render", e.tup()), "ir")));
        writeModule(src.resolve("main.at"), top);
        return src.resolve("main.at");
    }

    @Test
    public void test00057PointerListWrongPointee(@TempDir Path tmp) throws IOException {
        assertPointerMismatch(writeTypedDefinersModule(tmp, 1), tmp);
    }

    @Test
    public void test00058MacroGlobalWrongPointee(@TempDir Path tmp) throws IOException {
        assertPointerMismatch(writeTypedDefinersModule(tmp, 2), tmp);
    }

    private static void assertPointerMismatch(Path entry, Path tmp) {
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> throw new AssertionError("expected compile failure");
                case Result.Err<Compile.CompileResult, ?> err ->
                    Assertions.assertTrue(
                            err.error().toString().contains("llvm: pointer initializer @a points to"),
                            () -> "unexpected: " + res);
            }
        }
    }

    @Test
    public void test00056TypedDefinersOptionalInit(@TempDir Path tmp) throws IOException {
        var ok = compileOk(writeTypedDefinersModule(tmp, 0), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                """
                @a = global i32 7
                @b = global i32 zeroinitializer
                @d = global [3 x i8] zeroinitializer
                @qc = global ptr @c
                @qn = global ptr null
                @qm = global ptr @c
                @qv = global ptr @a
                @c = constant [3 x i8] [i8 1, i8 2, i8 3]
                @pa = constant [1 x ptr] [ptr @a]
                @pc = constant [1 x ptr] [ptr @c]
                @qcc = constant [2 x ptr] [ptr @c, ptr @c]
                @qz = constant [2 x ptr] zeroinitializer
                """,
                ir);
    }

    @Test
    public void test00060ListArrayInitElementTooWide(@TempDir Path tmp) throws IOException {
        var entry = writeListInitModule(tmp, "int");
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> throw new AssertionError("expected compile failure");
                case Result.Err<Compile.CompileResult, ?> err -> {
                    var errors = (TList<?>) err.error();
                    assertEquals(1, errors.size(), () -> "unexpected: " + res);
                    var only = (ExprErr) errors.get(0);
                    assertEquals(
                            mainSourceId(cache, entry),
                            only.loc().source().value(),
                            () -> "error not located in main.at: " + res);
                    Assertions.assertTrue(
                            only.message()
                                    .contains("source list(dynamic int) cannot convert to dest list(dynamic ubyte)"),
                            () -> "unexpected: " + res);
                }
            }
        }
    }

    @Test
    public void test00070StringArrayInit(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var ok = compileOk(writeArrayInitModule(tmp, e, 5, TList.of(), e.s("hé\n\0")), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                """
                declare i32 @puts(ptr)
                @str.hi = constant [5 x i8] [i8 104, i8 195, i8 169, i8 10, i8 0]
                define i32 @main() {
                  %t0 = call i32 @puts(ptr @str.hi)
                  ret i32 0
                }
                """,
                ir);
        assertValidIr(tmp, "hi", ir);
    }

    private static SourceExpr fileRead(EcoMb e, String path, int offset, int len) {
        return e.callM(
                e.importLocal("f", path), "read", e.rec(e.p("offset", e.intOf(offset)), e.p("len", e.intOf(len))));
    }

    @Test
    public void test00075FileArrayInit(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        Files.write(src.resolve("data.bin"), new byte[] {7, 104, 105, 10, 0, (byte) 200, 9});
        var ok = compileOk(
                writeArrayInitModule(tmp, e, 5, TList.of(), fileRead(e, "data.bin", 1, 5)), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                """
                declare i32 @puts(ptr)
                @str.hi = constant [5 x i8] [i8 104, i8 105, i8 10, i8 0, i8 200]
                define i32 @main() {
                  %t0 = call i32 @puts(ptr @str.hi)
                  ret i32 0
                }
                """,
                ir);
        assertValidIr(tmp, "hi", ir);
    }

    @Test
    public void test00076FileArrayInitBig(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var data = new byte[500000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 31);
        Files.write(src.resolve("data.bin"), data);
        var ok = compileOk(
                writeArrayInitModule(tmp, e, data.length, TList.of(), fileRead(e, "data.bin", 0, data.length)),
                tmp.resolve("cache"));
        Assertions.assertTrue(ok.logs().get(0).message().contains("[500000 x i8]"));
    }

    @Test
    public void test00080StringArrayInitWrongLength(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var entry = writeArrayInitModule(tmp, e, 5, TList.of(), e.s("hi\n\0"));
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> throw new AssertionError("expected compile failure");
                case Result.Err<Compile.CompileResult, ?> err -> {
                    var errors = (TList<?>) err.error();
                    assertEquals(1, errors.size(), () -> "unexpected: " + res);
                    var only = (ExprErr) errors.get(0);
                    Assertions.assertTrue(
                            only.message().contains("panic: llvm: array initializer has 4 elements, expected 5"),
                            () -> "unexpected: " + res);
                }
            }
        }
    }

    @Test
    public void test00090Cstr(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        EcoExtern.bindTypes(e, setup, EcoExtern.Llvm.top(e));
        var puts = new EcoExtern("puts", IList.of("pu8"), "i32", false);
        setup.add(e.let("puts_fn_t", puts.fnType(e, EcoExtern.Llvm.top(e))));
        setup.add(e.let("main_fn_t", fnType(e, "t_i32")));
        setup.add(e.ret(e.rec(
                e.p("ll", e.v("ll")),
                e.p("puts_fn_t", e.v("puts_fn_t")),
                e.p("main_fn_t", e.v("main_fn_t")),
                e.p("t_i32", e.v("t_i32")),
                e.p("t_pu8", e.v("t_pu8")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : new String[] {"ll", "puts_fn_t", "main_fn_t", "t_i32", "t_pu8"})
            top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("puts", puts.declare(e, e.fld(e.v("llvm"), "extern_fn"), e.v("puts_fn_t"))));
        top.add(e.let("hi", e.callM(e.v("ll"), "cstr", e.s("hé\n"))));
        top.add(func(e, "main", "main_fn_t", e.seq(e.call(e.lower(e.v("puts")), e.lower(e.v("hi"))), e.ret(e.n(0)))));
        top.add(e.call(e.mb.log(), e.fld(e.callM(e.v("ll"), "render", e.tup()), "ir")));
        writeModule(src.resolve("main.at"), top);

        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                """
                declare i32 @puts(ptr)
                @str0 = constant [5 x i8] [i8 104, i8 195, i8 169, i8 10, i8 0]
                define i32 @main() {
                  %t1 = call i32 @puts(ptr @str0)
                  ret i32 0
                }
                """,
                ir);
        assertValidIr(tmp, "cstr", ir);
    }

    private static final String LIBC_DECLS = """
            declare double @sin(double)
            """;

    private static void writeStagedModule(Path src, EcoMb e, String name, SourceExpr body) throws IOException {
        writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
        writeModule(src.resolve("libc-2.42.atz"), EcoLibc.module(new EcoMb()));
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        EcoExtern.bindTypes(e, setup, EcoExtern.Llvm.top(e));
        setup.add(e.let("f_fn_t", fnType(e, "t_f64", "t_f64")));
        setup.add(e.ret(e.rec(e.p("ll", e.v("ll")), e.p("f_fn_t", e.v("f_fn_t")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : new String[] {"ll", "f_fn_t"}) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("sin", e.fld(e.importLocal("libc", "libc-2.42.atz"), "sin")));
        top.add(func(e, name, "f_fn_t", body, "x"));
        top.add(e.call(e.mb.log(), e.fld(e.callM(e.v("ll"), "render", e.tup()), "ir")));
        writeModule(src.resolve("main.at"), top);
    }

    @Test
    public void test00100LibcSin(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var e = new EcoMb();
        writeStagedModule(src, e, "sin1", e.call(e.lower(e.v("sin")), e.v("x")));
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                LIBC_DECLS
                        + """
                        @s0 = internal global double zeroinitializer
                        define double @sin1(double %a0) {
                          store double %a0, ptr @s0
                          %t1 = load double, ptr @s0
                          %t2 = call double @sin(double %t1)
                          ret double %t2
                        }
                        """,
                ir);
        assertValidIr(tmp, "sin1", ir);
    }

    @Test
    public void test00150StagedLowering(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var e = new EcoMb();
        writeStagedModule(
                src,
                e,
                "sin2p",
                e.mb.operatorAdd(e.call(e.lower(e.v("sin")), e.call(e.lower(e.v("sin")), e.v("x"))), e.v("x")));
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                LIBC_DECLS
                        + """
                        @s0 = internal global double zeroinitializer
                        define double @sin2p(double %a0) {
                          store double %a0, ptr @s0
                          %t1 = load double, ptr @s0
                          %t2 = call double @sin(double %t1)
                          %t3 = call double @sin(double %t2)
                          %t4 = load double, ptr @s0
                          %t5 = fadd double %t3, %t4
                          ret double %t5
                        }
                        """,
                ir);
        assertValidIr(tmp, "sin2p", ir);
    }

    @Test
    public void test00160StagedIf(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var e = new EcoMb();
        writeStagedModule(
                src,
                e,
                "condsin",
                e.ifElse(e.mb.operatorLt(e.v("x"), e.mb.num(0.5)), e.call(e.lower(e.v("sin")), e.v("x")), e.v("x")));
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        assertEquals(
                LIBC_DECLS
                        + """
                        @s0 = internal global double zeroinitializer
                        @s8 = internal global double zeroinitializer
                        define double @condsin(double %a0) {
                          store double %a0, ptr @s0
                          %t1 = load double, ptr @s0
                          %t2 = fcmp olt double %t1, 0.5
                          br i1 %t2, label %L6, label %L7
                        L6:
                          %t3 = load double, ptr @s0
                          %t4 = call double @sin(double %t3)
                          store double %t4, ptr @s8
                          br label %L5
                        L7:
                          %t9 = load double, ptr @s0
                          store double %t9, ptr @s8
                          br label %L5
                        L5:
                          %t10 = load double, ptr @s8
                          ret double %t10
                        }
                        """,
                ir);
        assertValidIr(tmp, "condsin", ir);
    }

    private static final String POD_AUDIO_F32_MONO_48K_HEX =
            "A0000000 0F000000 03000400 03000000 01000000 00000000 04000000 03000000 01000000 00000000"
                    + " 02000000 00000000 04000000 03000000 01000000 00000000 01000100 00000000 04000000 03000000"
                    + " 1B010000 00000000 03000100 00000000 04000000 04000000 80BB0000 00000000 04000100 00000000"
                    + " 04000000 04000000 01000000 00000000 05000100 00000000 0C000000 0D000000 04000000 03000000"
                    + " 00000000 00000000";

    private static final String POD_MIDI_CONTROL_HEX =
            "38000000 0F000000 03000400 03000000 01000000 00000000 04000000 03000000 06000000 00000000"
                    + " 02000000 00000000 04000000 03000000 01000600 00000000";

    private static String byteArrayConstantIr(String name, String hex) {
        var digits = hex.replace(" ", "");
        var parts = new TList<String>();
        for (int i = 0; i < digits.length(); i += 2) {
            parts.add("i8 " + Integer.parseInt(digits.substring(i, i + 2), 16));
        }
        return "@" + name + " = constant [" + parts.size() + " x i8] [" + String.join(", ", parts) + "]";
    }

    @Test
    public void test00200Synth(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
        writeModule(src.resolve("libc-2.42.atz"), EcoLibc.module(new EcoMb()));
        writeModule(src.resolve("spa-pod-1.6.atz"), EcoSpaPod.module(new EcoMb()));
        writeModule(src.resolve("pipewire-1.6.atz"), EcoPipewire.module(new EcoMb()));
        writeModule(src.resolve("synth-midi-sine.atz"), EcoSynth.module(new EcoMb(), EcoRootMode.IR));

        var ok = compileOk(src.resolve("synth-midi-sine.atz"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        Files.writeString(tmp.resolve("synth.ll"), ir);
        for (var expected : new String[] {
            "%spa_data.audio = type { i32, i32, i64, i32, i32, ptr, ptr }",
            "%spa_data.midi = type { i32, i32, i64, i32, i32, ptr, ptr }",
            "%spa_pod_control = type { i32, i32, %spa_pod }",
            "%pw_stream_events = type { i32, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr }",
            "declare ptr @pw_stream_dequeue_buffer(ptr)",
            "declare double @sin(double)",
            "declare double @exp2(double)",
            "@g_phase = global double 0.0",
            byteArrayConstantIr("pw.pod.audio_f32_mono_48k", POD_AUDIO_F32_MONO_48K_HEX),
            byteArrayConstantIr("pw.pod.midi_control", POD_MIDI_CONTROL_HEX),
            "@pw.params.audio = constant [1 x ptr] [ptr @pw.pod.audio_f32_mono_48k]",
            "@pw.events.audio = constant %pw_stream_events { i32 2, ptr null, ptr null, ptr null, ptr null, ptr null,"
                    + " ptr null, ptr null, ptr @on_process, ptr null, ptr null, ptr null }",
            "define void @on_process(ptr %a0) {",
            "define void @on_midi(ptr %a0) {",
            "define i32 @main() {",
            "call i32 @pw_stream_connect(",
            " = call ptr @pw_stream_dequeue_buffer(ptr %t",
            "call ptr (ptr, ...) @pw_properties_new(",
            "getelementptr inbounds %spa_data.audio, ptr %t",
            "getelementptr inbounds %spa_pod_control, ptr %t",
            "= ptrtoint ptr %t",
            "getelementptr inbounds i8, ptr %t",
            "fptrunc double %t",
            "= icmp ult i32 %t",
        }) {
            Assertions.assertTrue(ir.contains(expected), () -> "missing <" + expected + "> in:\n" + ir);
        }
        for (var declared : new String[] {
            "declare ptr @pw_stream_dequeue_buffer(ptr)", "declare i32 @pw_stream_queue_buffer(ptr, ptr)"
        }) {
            assertEquals(2, ir.split(Pattern.quote(declared), -1).length, () -> "not declared once: " + declared);
        }
        assertValidIr(tmp, "synth", ir);
    }

    static long mainSourceId(CacheReal cache, Path entry) {
        return switch (cache.sourcesGet(new SourceSpecAbsolute.Local(entry.toString()))) {
            case Result.Ok<Sources.SourceResolved, String> ok ->
                switch (ok.value()) {
                    case Sources.SourceResolved.Module m -> m.id().value();
                    case Sources.SourceResolved.Bundle b -> throw new AssertionError("entry is a bundle: " + entry);
                    case Sources.SourceResolved.File f -> throw new AssertionError("entry is not a module: " + entry);
                };
            case Result.Err<Sources.SourceResolved, String> err ->
                throw new AssertionError("entry does not resolve: " + err.error());
        };
    }

    static boolean onPath(String tool) {
        var path = System.getenv("PATH");
        if (path == null) return false;
        for (var dir : path.split(File.pathSeparator)) {
            if (Files.isExecutable(Path.of(dir, tool))) return true;
        }
        return false;
    }

    @Test
    public void test00210SynthApp(@TempDir Path tmp) throws IOException {
        boolean canLink = false;
        if (onPath("clang")) {
            var probe = tmp.resolve("probe.c");
            Files.writeString(probe, "int main(void) { return 0; }\n");
            var argv = new ArrayList<String>();
            argv.add("clang");
            argv.add(probe.toString());
            argv.add("-lpipewire-0.3");
            argv.add("-lm");
            argv.add("-o");
            argv.add(tmp.resolve("probe").toString());
            try {
                var proc = new ProcessBuilder(argv).redirectErrorStream(true).start();
                proc.getInputStream().readAllBytes();
                canLink = proc.waitFor() == 0;
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }
        Assumptions.assumeTrue(canLink, "clang cannot link libpipewire-0.3 and libm");
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
        writeModule(src.resolve("libc-2.42.atz"), EcoLibc.module(new EcoMb()));
        writeModule(src.resolve("spa-pod-1.6.atz"), EcoSpaPod.module(new EcoMb()));
        writeModule(src.resolve("pipewire-1.6.atz"), EcoPipewire.module(new EcoMb()));
        writeModule(src.resolve("synth-midi-sine.atz"), EcoSynth.module(new EcoMb(), EcoRootMode.APP));
        var app = T0049AppTest.compileApp(tmp.resolve("cache"), src.resolve("synth-midi-sine.atz"));
        Assertions.assertTrue(app.executor().optIsNone());
        var bytes = app.bytes();
        Assertions.assertTrue(bytes.length > 4, "app is empty");
        Assertions.assertArrayEquals(new byte[] {0x7f, 'E', 'L', 'F'}, Arrays.copyOf(bytes, 4));
    }

    private static void writeIfChanged(Path file, byte[] content) throws IOException {
        if (Files.exists(file) && Arrays.equals(Files.readAllBytes(file), content)) {
            return;
        }
        Files.write(file, content);
    }

    @Test
    public void test00250BuzzSong(@TempDir Path tmp) throws IOException {
        var repoRoot = Path.of("").toAbsolutePath().getParent();
        if (repoRoot == null) throw new IllegalStateException("test working directory has no parent");
        Assumptions.assumeTrue(Files.isDirectory(repoRoot.resolve("wine-audio").resolve("music")));
        var src = repoRoot.resolve("tmp-buzz-test");
        Files.createDirectories(src);
        try {
            writeModule(src.resolve("llvm-ir-22.atz"), EcoLlvmIr.module(new EcoMb()));
            writeModule(src.resolve("libc-2.42.atz"), EcoLibc.module(new EcoMb()));
            writeModule(src.resolve("spa-pod-1.6.atz"), EcoSpaPod.module(new EcoMb()));
            writeModule(src.resolve("pipewire-1.6.atz"), EcoPipewire.module(new EcoMb()));
            writeModule(src.resolve("buzz-dsp.atz"), EcoBuzzDsp.module(new EcoMb()));
            writeModule(src.resolve("buzz-samplegrid.atz"), EcoBuzzSampleGrid.module(new EcoMb()));
            writeModule(src.resolve("buzz-unwieldytracker.atz"), EcoBuzzUnwieldyTracker.module(new EcoMb()));
            writeModule(src.resolve("buzz-m4.atz"), EcoBuzzM4.module(new EcoMb()));
            writeModule(src.resolve("buzz-infector.atz"), EcoBuzzInfector.module(new EcoMb()));
            writeModule(src.resolve("buzz-tunaman.atz"), EcoBuzzTunaMan.module(new EcoMb()));
            writeModule(src.resolve("buzz-ninja.atz"), EcoBuzzNinja.module(new EcoMb()));
            writeModule(src.resolve("buzz-m4wii.atz"), EcoBuzzM4wII.module(new EcoMb()));
            writeModule(src.resolve("buzz-classiceq.atz"), EcoBuzzClassicEq.module(new EcoMb()));
            writeModule(src.resolve("buzz-karmafilter.atz"), EcoBuzzKarmaFilter.module(new EcoMb()));
            writeModule(src.resolve("buzz-kaway.atz"), EcoBuzzKAway.module(new EcoMb()));
            writeModule(src.resolve("buzz-bloodoverdrive.atz"), EcoBuzzBloodOverdrive.module(new EcoMb()));
            writeModule(src.resolve("buzz-bluecatphaser.atz"), EcoBuzzBlueCatPhaser.module(new EcoMb()));
            Files.copy(
                    repoRoot.resolve("ecosystem").resolve("buzz-m4wii-waves.raw"),
                    src.resolve("buzz-m4wii-waves.raw"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(
                    repoRoot.resolve("ecosystem").resolve("buzz-m4-waves.raw"),
                    src.resolve("buzz-m4-waves.raw"),
                    StandardCopyOption.REPLACE_EXISTING);
            writeModule(
                    src.resolve("buzz-gunpowder-valentine.atz"), EcoBuzzGunpowder.module(new EcoMb(), EcoRootMode.IR));
            var ok = compileOk(src.resolve("buzz-gunpowder-valentine.atz"), tmp.resolve("cache"));
            assertEquals(1, ok.logs().size());
            var ir = ok.logs().get(0).message();
            Files.writeString(tmp.resolve("buzz.ll"), ir);
            for (var expected : new String[] {
                "define float @bz_wave_read(",
                "define void @sg_tick(",
                "define void @ut_work(",
                "define void @song_render(",
                "define i32 @main() {",
                "@wavetable = constant %bz_wave_table",
                "@pw.params.audio_stereo_44k1 = constant [1 x ptr] [ptr @pw.pod.audio_f32_stereo_44k1]"
            }) {
                Assertions.assertTrue(ir.contains(expected), () -> "missing in IR: " + expected);
            }
            assertValidIr(tmp, "buzz", ir);
        } finally {
            if (System.getProperty("buzz.keep") == null) {
                try (var files = Files.list(src)) {
                    for (var f : files.toList()) Files.delete(f);
                }
                Files.delete(src);
            }
        }
    }

    @Test
    public void test00400BuzzOracleSweeps(@TempDir Path tmp) throws IOException, InterruptedException {
        var repoRoot = Path.of("").toAbsolutePath().getParent();
        if (repoRoot == null) throw new IllegalStateException("test working directory has no parent");
        Assumptions.assumeTrue(System.getProperty("buzz.oracle") != null);
        Assumptions.assumeTrue(Files.isDirectory(
                repoRoot.resolve("wine-audio").resolve("music").resolve("Jeskola Buzz")));
        var dirs = System.getenv("PATH").split(File.pathSeparator);
        for (var tool :
                new String[] {"wine", "python3", "clang", "llvm-as", "llc", "llvm-objcopy", "i686-w64-mingw32-gcc"}) {
            var found = false;
            for (var dir : dirs) found |= Files.isExecutable(Path.of(dir).resolve(tool));
            Assumptions.assumeTrue(found, () -> tool + " not on PATH");
        }
        var proc = new ProcessBuilder(
                        "sh",
                        repoRoot.resolve("ecosystem/harness/verify-buzz-params.sh")
                                .toString(),
                        System.getProperty("java.class.path"),
                        tmp.toString())
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start();
        var output = new String(proc.getInputStream().readAllBytes());
        var code = proc.waitFor();
        Assertions.assertEquals(0, code, () -> output);
    }

    @Test
    public void test00300WriteEcosystem() throws IOException {
        var repoRoot = Path.of("").toAbsolutePath().getParent();
        if (repoRoot == null) throw new IllegalStateException("test working directory has no parent");
        var ecosystem = repoRoot.resolve("ecosystem");
        Assumptions.assumeTrue(Files.exists(repoRoot.resolve("pom.xml")) && Files.isDirectory(ecosystem));
        writeIfChanged(ecosystem.resolve("llvm-ir-22.atz"), serializeModuleZ(EcoLlvmIr.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("libc-2.42.atz"), serializeModuleZ(EcoLibc.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("spa-pod-1.6.atz"), serializeModuleZ(EcoSpaPod.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("pipewire-1.6.atz"), serializeModuleZ(EcoPipewire.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("synth-midi-sine.atz"),
                serializeModuleZ(EcoSynth.module(new EcoMb(), EcoRootMode.APP)));
        writeIfChanged(
                ecosystem.resolve("synth-midi-sine-ir.atz"),
                serializeModuleZ(EcoSynth.module(new EcoMb(), EcoRootMode.IR)));
        writeIfChanged(ecosystem.resolve("buzz-dsp.atz"), serializeModuleZ(EcoBuzzDsp.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-samplegrid.atz"), serializeModuleZ(EcoBuzzSampleGrid.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-unwieldytracker.atz"),
                serializeModuleZ(EcoBuzzUnwieldyTracker.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-m4.atz"), serializeModuleZ(EcoBuzzM4.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-infector.atz"), serializeModuleZ(EcoBuzzInfector.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-tunaman.atz"), serializeModuleZ(EcoBuzzTunaMan.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-ninja.atz"), serializeModuleZ(EcoBuzzNinja.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-m4wii.atz"), serializeModuleZ(EcoBuzzM4wII.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-classiceq.atz"), serializeModuleZ(EcoBuzzClassicEq.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-karmafilter.atz"), serializeModuleZ(EcoBuzzKarmaFilter.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-kaway.atz"), serializeModuleZ(EcoBuzzKAway.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-bloodoverdrive.atz"),
                serializeModuleZ(EcoBuzzBloodOverdrive.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-bluecatphaser.atz"),
                serializeModuleZ(EcoBuzzBlueCatPhaser.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-gunpowder-valentine.atz"),
                serializeModuleZ(EcoBuzzGunpowder.module(new EcoMb(), EcoRootMode.APP)));
        writeIfChanged(
                ecosystem.resolve("buzz-gunpowder-valentine-ir.atz"),
                serializeModuleZ(EcoBuzzGunpowder.module(new EcoMb(), EcoRootMode.IR)));
        writeIfChanged(ecosystem.resolve("wat-2.0.atz"), serializeModuleZ(EcoWat.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("js-es2020.atz"), serializeModuleZ(EcoJs.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("webidl.atz"), serializeModuleZ(EcoWebidl.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("webidl-wasm.atz"), serializeModuleZ(EcoWebidlWasm.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("webidl-js.atz"), serializeModuleZ(EcoWebidlJs.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("web-wasm.atz"), serializeModuleZ(EcoWebWasm.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("hello-web.atz"), serializeModuleZ(EcoHelloWeb.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("wasi-0.1.atz"), serializeModuleZ(EcoWasi.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("hello-wasi.atz"),
                serializeModuleZ(EcoHelloWasi.module(new EcoMb(), EcoHelloWasi.Mode.APP)));
        writeIfChanged(
                ecosystem.resolve("hello-wasi-wat.atz"),
                serializeModuleZ(EcoHelloWasi.module(new EcoMb(), EcoHelloWasi.Mode.WAT)));
    }
}
