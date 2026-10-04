package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0200EcosystemLlvmTest {
    static String serializeModule(TList<SourceExpr> exprs) throws IOException {
        var sw = new StringWriter();
        var writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        writer.setIndent("  ");
        SourceModuleCodec.encode(writer, new SourceModule.V1(exprs), Json.Style.PRETTY);
        writer.close();
        return sw.toString();
    }

    static void writeModule(Path file, TList<SourceExpr> exprs) throws IOException {
        Files.writeString(file, serializeModule(exprs));
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
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        var hello = "hello ecosys!\n".getBytes(StandardCharsets.UTF_8);
        var helloInit = new SourceExpr[hello.length + 1];
        for (int i = 0; i < hello.length; i++) helloInit[i] = e.n(hello[i] & 0xFF);
        helloInit[hello.length] = e.n(0);
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
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
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : exported) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("puts", puts.declare(e, e.fld(e.v("llvm"), "extern_fn"), e.v("ll"), e.v("puts_fn_t"))));
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
        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.tup())));
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
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        EcoExtern.bindTypes(e, setup, EcoExtern.Llvm.top(e));
        var puts = new EcoExtern("puts", IList.of("pu8"), "i32", false);
        setup.add(e.let("puts_fn_t", puts.fnType(e, EcoExtern.Llvm.top(e))));
        setup.add(e.let("main_fn_t", fnType(e, "t_i32")));
        setup.add(e.let("hi_arr_t", EcoPipewire.byteArrayType(e, EcoExtern.Llvm.top(e), e.intOf(count))));
        var exported = new TList<String>();
        exported.add("ll");
        exported.add("puts_fn_t");
        exported.add("main_fn_t");
        exported.add("hi_arr_t");
        for (var t : new String[] {"i1", "i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void", "pv", "pu8"}) {
            exported.add("t_" + t);
        }
        var exportedFields = new TList<EcoMb.P>();
        for (var n : exported) exportedFields.add(e.p(n, e.v(n)));
        setup.add(e.ret(e.rec(exportedFields.toArray(EcoMb.P[]::new))));
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : exported) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("puts", puts.declare(e, e.fld(e.v("llvm"), "extern_fn"), e.v("ll"), e.v("puts_fn_t"))));
        top.addAll(preInit);
        top.add(e.let(
                "hi",
                e.call(
                        e.fld(e.v("llvm"), "global_const_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("str.hi")),
                                e.p("ty", e.v("hi_arr_t")),
                                e.p("init", init)))));
        top.add(func(e, "main", "main_fn_t", e.seq(e.call(e.lower(e.v("puts")), e.lower(e.v("hi"))), e.ret(e.n(0)))));
        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.tup())));
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
                    assertEquals(0L, only.loc().source().value(), () -> "error not located in main.at: " + res);
                    Assertions.assertTrue(
                            only.message().contains("host int values do not fit llvm u8"), () -> "unexpected: " + res);
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
                e.call(e.mb.builtin("file", "open"), e.s(path)),
                "read",
                e.rec(e.p("offset", e.intOf(offset)), e.p("len", e.intOf(len))));
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
                    assertEquals(0L, only.loc().source().value(), () -> "error not located in main.at: " + res);
                    Assertions.assertTrue(
                            only.message().contains("string initializer has 4 bytes, expected 5"),
                            () -> "unexpected: " + res);
                }
            }
        }
    }

    @Test
    public void test00090Cstr(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
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
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : new String[] {"ll", "puts_fn_t", "main_fn_t", "t_i32", "t_pu8"})
            top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("puts", puts.declare(e, e.fld(e.v("llvm"), "extern_fn"), e.v("ll"), e.v("puts_fn_t"))));
        top.add(e.let("hi", e.callM(e.v("ll"), "cstr", e.s("hé\n"))));
        top.add(func(e, "main", "main_fn_t", e.seq(e.call(e.lower(e.v("puts")), e.lower(e.v("hi"))), e.ret(e.n(0)))));
        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.tup())));
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

    private static final String LIBC_DECLS =
            """
            declare double @sin(double)
            declare double @exp2(double)
            declare double @fmod(double, double)
            declare float @sinf(float)
            declare float @cosf(float)
            declare float @tanf(float)
            declare float @expf(float)
            declare float @exp2f(float)
            declare float @logf(float)
            declare float @powf(float, float)
            declare float @sqrtf(float)
            declare float @floorf(float)
            declare float @fabsf(float)
            declare float @fmodf(float, float)
            declare double @pow(double, double)
            declare double @exp(double)
            declare double @log(double)
            declare double @cos(double)
            declare double @tan(double)
            declare double @sqrt(double)
            declare double @floor(double)
            declare double @fabs(double)
            declare ptr @memset(ptr, i32, i64)
            declare i32 @puts(ptr)
            declare void @exit(i32)
            declare i32 @printf(ptr, ...)
            """;

    private static void writeStagedModule(Path src, EcoMb e, String name, SourceExpr body) throws IOException {
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        writeModule(src.resolve("libc-2.42.at"), EcoLibc.module(new EcoMb()));
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        setup.add(e.let("libc", e.importLocal("libc", "libc-2.42.at")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        setup.add(e.let("c", e.call(e.v("libc"), e.v("ll"))));
        EcoExtern.bindTypes(e, setup, EcoExtern.Llvm.top(e));
        setup.add(e.let("f_fn_t", fnType(e, "t_f64", "t_f64")));
        setup.add(e.ret(e.rec(e.p("ll", e.v("ll")), e.p("c", e.v("c")), e.p("f_fn_t", e.v("f_fn_t")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : new String[] {"ll", "c", "f_fn_t"}) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("sin", e.callM(e.v("c"), "sin", e.tup())));
        top.add(func(e, name, "f_fn_t", body, "x"));
        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.tup())));
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
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        writeModule(src.resolve("libc-2.42.at"), EcoLibc.module(new EcoMb()));
        writeModule(src.resolve("spa-pod-1.6.at"), EcoSpaPod.module(new EcoMb()));
        writeModule(src.resolve("pipewire-1.6.at"), EcoPipewire.module(new EcoMb()));
        writeModule(src.resolve("synth-midi-sine.at"), EcoSynth.module(new EcoMb()));

        var ok = compileOk(src.resolve("synth-midi-sine.at"), tmp.resolve("cache"));
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

    private static void writeIfChanged(Path file, String content) throws IOException {
        if (Files.exists(file) && Files.readString(file).equals(content)) {
            return;
        }
        Files.writeString(file, content);
    }

    @Test
    public void test00250BuzzSong(@TempDir Path tmp) throws IOException {
        var repoRoot = Path.of("").toAbsolutePath().getParent();
        if (repoRoot == null) throw new IllegalStateException("test working directory has no parent");
        Assumptions.assumeTrue(Files.isDirectory(repoRoot.resolve("wine-audio").resolve("music")));
        var src = repoRoot.resolve("tmp-buzz-test");
        Files.createDirectories(src);
        try {
            writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
            writeModule(src.resolve("libc-2.42.at"), EcoLibc.module(new EcoMb()));
            writeModule(src.resolve("spa-pod-1.6.at"), EcoSpaPod.module(new EcoMb()));
            writeModule(src.resolve("pipewire-1.6.at"), EcoPipewire.module(new EcoMb()));
            writeModule(src.resolve("buzz-dsp.at"), EcoBuzzDsp.module(new EcoMb()));
            writeModule(src.resolve("buzz-samplegrid.at"), EcoBuzzSampleGrid.module(new EcoMb()));
            writeModule(src.resolve("buzz-unwieldytracker.at"), EcoBuzzUnwieldyTracker.module(new EcoMb()));
            writeModule(src.resolve("buzz-m4.at"), EcoBuzzM4.module(new EcoMb()));
            writeModule(src.resolve("buzz-infector.at"), EcoBuzzInfector.module(new EcoMb()));
            writeModule(src.resolve("buzz-tunaman.at"), EcoBuzzTunaMan.module(new EcoMb()));
            writeModule(src.resolve("buzz-ninja.at"), EcoBuzzNinja.module(new EcoMb()));
            writeModule(src.resolve("buzz-m4wii.at"), EcoBuzzM4wII.module(new EcoMb()));
            writeModule(src.resolve("buzz-classiceq.at"), EcoBuzzClassicEq.module(new EcoMb()));
            writeModule(src.resolve("buzz-karmafilter.at"), EcoBuzzKarmaFilter.module(new EcoMb()));
            writeModule(src.resolve("buzz-kaway.at"), EcoBuzzKAway.module(new EcoMb()));
            Files.copy(
                    repoRoot.resolve("ecosystem").resolve("buzz-m4wii-waves.raw"),
                    src.resolve("buzz-m4wii-waves.raw"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(
                    repoRoot.resolve("ecosystem").resolve("buzz-m4-waves.raw"),
                    src.resolve("buzz-m4-waves.raw"),
                    StandardCopyOption.REPLACE_EXISTING);
            writeModule(src.resolve("buzz-gunpowder-valentine.at"), EcoBuzzGunpowder.module(new EcoMb()));
            var ok = compileOk(src.resolve("buzz-gunpowder-valentine.at"), tmp.resolve("cache"));
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
    public void test00300WriteEcosystem() throws IOException {
        var repoRoot = Path.of("").toAbsolutePath().getParent();
        if (repoRoot == null) throw new IllegalStateException("test working directory has no parent");
        var ecosystem = repoRoot.resolve("ecosystem");
        Assumptions.assumeTrue(Files.exists(repoRoot.resolve("pom.xml")) && Files.isDirectory(ecosystem));
        writeIfChanged(ecosystem.resolve("llvm-ir-22.at"), serializeModule(EcoLlvmIr.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("libc-2.42.at"), serializeModule(EcoLibc.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("spa-pod-1.6.at"), serializeModule(EcoSpaPod.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("pipewire-1.6.at"), serializeModule(EcoPipewire.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("synth-midi-sine.at"), serializeModule(EcoSynth.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-dsp.at"), serializeModule(EcoBuzzDsp.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-samplegrid.at"), serializeModule(EcoBuzzSampleGrid.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-unwieldytracker.at"),
                serializeModule(EcoBuzzUnwieldyTracker.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-m4.at"), serializeModule(EcoBuzzM4.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-infector.at"), serializeModule(EcoBuzzInfector.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-tunaman.at"), serializeModule(EcoBuzzTunaMan.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-ninja.at"), serializeModule(EcoBuzzNinja.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-m4wii.at"), serializeModule(EcoBuzzM4wII.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-classiceq.at"), serializeModule(EcoBuzzClassicEq.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-karmafilter.at"), serializeModule(EcoBuzzKarmaFilter.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("buzz-kaway.at"), serializeModule(EcoBuzzKAway.module(new EcoMb())));
        writeIfChanged(
                ecosystem.resolve("buzz-gunpowder-valentine.at"),
                serializeModule(EcoBuzzGunpowder.module(new EcoMb())));
    }
}
