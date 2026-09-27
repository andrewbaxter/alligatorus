package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.stream.JsonWriter;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0200EcosystemLlvmTest {
    static String serializeModule(SourceExpr expr) throws IOException {
        var sw = new StringWriter();
        var writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        writer.setIndent("");
        SourceModuleCodec.encode(writer, new SourceModule.V1(expr), Json.Style.COMPACT);
        writer.close();
        return sw.toString();
    }

    static void writeModule(Path file, SourceExpr expr) throws IOException {
        Files.writeString(file, serializeModule(expr));
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

    private static SourceExpr fieldList(EcoMb e, SourceExpr... fields) {
        return e.listNew(e.mb.evalValue(), fields);
    }

    private static SourceExpr func(EcoMb e, String name, SourceExpr params, String ret, SourceExpr body) {
        return e.let(
                name,
                e.callM(
                        e.v("ll"),
                        "func",
                        e.rec(
                                e.p("name", e.s(name)),
                                e.p("params", params),
                                e.p("ret", e.v(ret)),
                                e.p("body", e.stage(body)))));
    }

    @Test
    public void test00000HelloWorldIr(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        var hello = "hello ecosys!\n".getBytes(StandardCharsets.UTF_8);
        var ints = new int[hello.length + 1];
        for (int i = 0; i < hello.length; i++) ints[i] = hello[i] & 0xFF;
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        EcoExtern.bindTypes(e, setup, "ll");
        var puts = new EcoExtern("puts", IList.of("pu8"), "i32", false);
        setup.add(e.let("puts_t", puts.constType(e, "ll")));
        setup.add(e.let("hello_t", EcoPipewire.byteArrayConstType(e, "ll", 15)));
        var exported = new TList<String>();
        exported.add("ll");
        exported.add("puts_t");
        exported.add("hello_t");
        for (var t : new String[] {"i1", "i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void", "pv", "pu8"}) {
            exported.add("t_" + t);
        }
        var exportedFields = new TList<EcoMb.P>();
        for (var n : exported) exportedFields.add(e.p(n, e.v(n)));
        setup.add(e.ret(e.rec(exportedFields.toArray(EcoMb.P[]::new))));
        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : exported) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("puts", e.mb.as(puts.declare(e, "ll"), e.v("puts_t"))));
        top.add(e.let(
                "hello",
                e.mb.as(
                        e.callM(
                                e.v("ll"),
                                "global_const",
                                e.rec(
                                        e.p("name", e.s("str.hello")),
                                        e.p("ty", EcoPipewire.byteArrayType(e, "ll", 15)),
                                        e.p("init", e.stage(EcoPipewire.byteTuple(e, ints))))),
                        e.v("hello_t"))));
        top.add(func(
                e,
                "main",
                fieldList(e),
                "t_i32",
                e.seq(e.call(e.lower(e.v("puts")), e.lower(e.v("hello"))), e.ret(e.n(0)))));
        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.tup())));
        writeModule(src.resolve("main.at"), e.mb.seq(top));

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

    private static final String LIBC_DECLS =
            """
            declare double @sin(double)
            declare double @exp2(double)
            declare double @fmod(double, double)
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
        EcoExtern.bindTypes(e, setup, "ll");
        setup.add(e.let("sin_t", e.callM(e.v("c"), "sin_t", e.tup())));
        setup.add(e.ret(e.rec(
                e.p("ll", e.v("ll")), e.p("c", e.v("c")), e.p("t_f64", e.v("t_f64")), e.p("sin_t", e.v("sin_t")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : new String[] {"ll", "c", "t_f64", "sin_t"}) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let("sin", e.mb.as(e.callM(e.v("c"), "sin", e.tup()), e.v("sin_t"))));
        top.add(func(
                e,
                name,
                fieldList(e, e.callM(e.v("ll"), "field", e.rec(e.p("name", e.s("x")), e.p("ty", e.v("t_f64"))))),
                "t_f64",
                body));
        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.tup())));
        writeModule(src.resolve("main.at"), e.mb.seq(top));
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

    @Test
    public void test00200Synth(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        writeModule(src.resolve("libc-2.42.at"), EcoLibc.module(new EcoMb()));
        writeModule(src.resolve("pipewire-1.6.at"), EcoPipewire.module(new EcoMb()));
        writeModule(src.resolve("synth-midi-sine.at"), EcoSynth.module(new EcoMb()));

        var ok = compileOk(src.resolve("synth-midi-sine.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var ir = ok.logs().get(0).message();
        Files.writeString(tmp.resolve("synth.ll"), ir);
        for (var expected : new String[] {
            "%spa_data = type { i32, i32, i64, i32, i32, ptr, ptr }",
            "%pw_stream_events = type { i32, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr }",
            "declare ptr @pw_stream_dequeue_buffer(ptr)",
            "declare double @sin(double)",
            "declare double @exp2(double)",
            "@g_phase = global double 0.0",
            "@pw.pod.audio_f32_mono_48k = constant [168 x i8]",
            "@pw.params.audio = constant [1 x ptr] [ptr @pw.pod.audio_f32_mono_48k]",
            "@pw.events.audio = constant %pw_stream_events { i32 2, ptr null, ptr null, ptr null, ptr null, ptr null,"
                    + " ptr null, ptr null, ptr @on_process, ptr null, ptr null, ptr null }",
            "define void @on_process(ptr %a0) {",
            "define void @on_midi(ptr %a0) {",
            "define i32 @main() {",
            "call i32 @pw_stream_connect(",
            " = call ptr @pw_stream_dequeue_buffer(ptr %t",
            "call ptr (ptr, ...) @pw_properties_new(",
            "getelementptr inbounds %spa_data, ptr %t",
            "fptrunc double %t",
            "= icmp ult i32 %t",
        }) {
            Assertions.assertTrue(ir.contains(expected), () -> "missing <" + expected + "> in:\n" + ir);
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
    public void test00300WriteEcosystem() throws IOException {
        var repoRoot = Path.of("").toAbsolutePath().getParent();
        if (repoRoot == null) throw new IllegalStateException("test working directory has no parent");
        var ecosystem = repoRoot.resolve("ecosystem");
        Assumptions.assumeTrue(Files.exists(repoRoot.resolve("pom.xml")) && Files.isDirectory(ecosystem));
        writeIfChanged(ecosystem.resolve("llvm-ir-22.at"), serializeModule(EcoLlvmIr.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("libc-2.42.at"), serializeModule(EcoLibc.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("pipewire-1.6.at"), serializeModule(EcoPipewire.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("synth-midi-sine.at"), serializeModule(EcoSynth.module(new EcoMb())));
    }
}
