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
import compilerxyz.prelude.util.Result;
import java.io.IOException;
import java.io.StringWriter;
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
            var res = Compile.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
            Result.Ok<Compile.CompileResult, ?> ok;
            switch (res) {
                case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
                case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
            }
            return ok.value();
        }
    }

    @Test
    public void test00000HelloWorldIr(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")),
                        e.let("ll", e.call(e.v("llvm"), e.nil())),
                        e.let(
                                "puts",
                                e.callM(
                                        e.v("ll"),
                                        "extern_func",
                                        e.rec(
                                                e.p("name", e.s("puts")),
                                                e.p("ret", e.s("i32")),
                                                e.p("params", e.strList(e.s("ptr")))))),
                        e.let(
                                "hello",
                                e.callM(
                                        e.v("ll"),
                                        "global_const",
                                        e.rec(
                                                e.p("name", e.s("str.hello")),
                                                e.p(
                                                        "type",
                                                        e.callM(
                                                                e.v("ll"),
                                                                "array_of",
                                                                e.rec(e.p("n", e.intOf(15)), e.p("ty", e.s("i8"))))),
                                                e.p("init", e.s("c\"hello ecosys!\\0A\\00\""))))),
                        e.let(
                                "fb",
                                e.callM(
                                        e.v("ll"),
                                        "func",
                                        e.rec(
                                                e.p("name", e.s("main")),
                                                e.p("ret", e.s("i32")),
                                                e.p("params", e.strList())))),
                        e.callM(
                                e.v("fb"),
                                "call",
                                e.rec(
                                        e.p("f", e.v("puts")),
                                        e.p("args", e.strList(e.callM(e.v("ll"), "arg_of", e.v("hello")))))),
                        e.callM(
                                e.v("fb"),
                                "ret",
                                e.callM(e.v("fb"), "konst", e.rec(e.p("ty", e.s("i32")), e.p("ref", e.s("0"))))),
                        e.call(e.mb.log(), e.callM(e.v("fb"), "finish", e.nil())),
                        e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.nil()))));

        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(2, ok.logs().size());
        assertEquals(
                """
                define i32 @main() {
                  %t0 = call i32 @puts(ptr @str.hello)
                  ret i32 0
                }
                """,
                ok.logs().get(0).message());
        assertEquals(
                """
                declare i32 @puts(ptr)
                @str.hello = constant [15 x i8] c"hello ecosys!\\0A\\00"
                define i32 @main() {
                  %t0 = call i32 @puts(ptr @str.hello)
                  ret i32 0
                }

                """,
                ok.logs().get(1).message());
    }

    @Test
    public void test00100LibcSin(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        writeModule(src.resolve("libc-2.42.at"), EcoLibc.module(new EcoMb()));
        var e = new EcoMb();
        writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")),
                        e.let("libc", e.importLocal("libc", "libc-2.42.at")),
                        e.let("ll", e.call(e.v("llvm"), e.nil())),
                        e.let("c", e.call(e.v("libc"), e.v("ll"))),
                        e.let(
                                "fb",
                                e.callM(
                                        e.v("ll"),
                                        "func",
                                        e.rec(
                                                e.p("name", e.s("sin1")),
                                                e.p("ret", e.s("double")),
                                                e.p("params", e.strList(e.s("double")))))),
                        e.let("x", e.callM(e.v("fb"), "param", e.rec(e.p("i", e.intOf(0)), e.p("ty", e.s("double"))))),
                        e.let(
                                "s",
                                e.callM(
                                        e.v("fb"),
                                        "call",
                                        e.rec(
                                                e.p("f", e.callM(e.v("c"), "sin", e.nil())),
                                                e.p("args", e.strList(e.callM(e.v("ll"), "arg_of", e.v("x"))))))),
                        e.callM(e.v("fb"), "ret", e.v("s")),
                        e.call(e.mb.log(), e.callM(e.v("fb"), "finish", e.nil())),
                        e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.nil()))));

        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(2, ok.logs().size());
        assertEquals(
                """
                declare double @sin(double)
                declare double @exp2(double)
                declare double @fmod(double, double)
                declare ptr @memset(ptr, i32, i64)
                declare i32 @puts(ptr)
                declare void @exit(i32)
                declare i32 @printf(ptr, ...)
                define double @sin1(double %a0) {
                  %t0 = call double @sin(double %a0)
                  ret double %t0
                }

                """,
                ok.logs().get(1).message());
    }

    @Test
    public void test00150StagedLowering(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")),
                        e.let("ll", e.call(e.v("llvm"), e.nil())),
                        e.let(
                                "sinf",
                                e.callM(
                                        e.v("ll"),
                                        "extern_func",
                                        e.rec(
                                                e.p("name", e.s("sin")),
                                                e.p("ret", e.s("double")),
                                                e.p("params", e.strList(e.s("double")))))),
                        e.let(
                                "def",
                                e.callM(
                                        e.v("ll"),
                                        "func_staged",
                                        e.rec(
                                                e.p("name", e.s("sin2p")),
                                                e.p("sin", e.v("sinf")),
                                                e.p(
                                                        "body",
                                                        e.stage(
                                                                e.mb.operatorAdd(
                                                                        e.call(
                                                                                e.v("sin"),
                                                                                e.call(e.v("sin"), e.v("x"))),
                                                                        e.v("x"))))))),
                        e.call(e.mb.log(), e.v("def"))));

        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        assertEquals(
                """
                define double @sin2p(double %a0) {
                  %t0 = call double @sin(double %a0)
                  %t1 = call double @sin(double %t0)
                  %t2 = fadd double %t1, %a0
                  ret double %t2
                }
                """,
                ok.logs().get(0).message());
    }

    @Test
    public void test00160StagedIf(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("llvm-ir-22.at"), EcoLlvmIr.module(new EcoMb()));
        var e = new EcoMb();
        writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")),
                        e.let("ll", e.call(e.v("llvm"), e.nil())),
                        e.let(
                                "sinf",
                                e.callM(
                                        e.v("ll"),
                                        "extern_func",
                                        e.rec(
                                                e.p("name", e.s("sin")),
                                                e.p("ret", e.s("double")),
                                                e.p("params", e.strList(e.s("double")))))),
                        e.let(
                                "def",
                                e.callM(
                                        e.v("ll"),
                                        "func_staged",
                                        e.rec(
                                                e.p("name", e.s("condsin")),
                                                e.p("sin", e.v("sinf")),
                                                e.p(
                                                        "body",
                                                        e.stage(
                                                                e.ifElse(
                                                                        e.mb.operatorLt(e.v("x"), e.mb.num(0.5)),
                                                                        e.call(e.v("sin"), e.v("x")),
                                                                        e.v("x"))))))),
                        e.call(e.mb.log(), e.v("def"))));

        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        assertEquals(
                """
                define double @condsin(double %a0) {
                  %t2 = alloca double
                  %t0 = fcmp olt double %a0, 0.5
                  br i1 %t0, label %b4, label %b5
                b4:
                  %t1 = call double @sin(double %a0)
                  store double %t1, ptr %t2
                  br label %b3
                b5:
                  store double %a0, ptr %t2
                  br label %b3
                b3:
                  %t6 = load double, ptr %t2
                  ret double %t6
                }
                """,
                ok.logs().get(0).message());
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
            "declare ptr @pw_stream_dequeue_buffer(ptr)",
            "declare double @sin(double)",
            "declare double @exp2(double)",
            "@g_phase = global double 0.000000e+00",
            "@pw.pod.audio_f32_mono_48k = constant [168 x i8]",
            "@pw.params.audio = constant [1 x ptr] [ptr @pw.pod.audio_f32_mono_48k]",
            "define void @on_process(ptr %a0) {",
            "define void @on_midi(ptr %a0) {",
            "define i32 @main() {",
            "call i32 @pw_stream_connect(",
            " = call ptr @pw_stream_dequeue_buffer(ptr %t",
            "call ptr (ptr, ...) @pw_properties_new(",
        }) {
            Assertions.assertTrue(ir.contains(expected), () -> "missing <" + expected + "> in:\n" + ir);
        }
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
        var ecosystem = repoRoot.resolve("ecosystem");
        Assumptions.assumeTrue(Files.exists(repoRoot.resolve("pom.xml")) && Files.isDirectory(ecosystem));
        writeIfChanged(ecosystem.resolve("llvm-ir-22.at"), serializeModule(EcoLlvmIr.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("libc-2.42.at"), serializeModule(EcoLibc.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("pipewire-1.6.at"), serializeModule(EcoPipewire.module(new EcoMb())));
        writeIfChanged(ecosystem.resolve("synth-midi-sine.at"), serializeModule(EcoSynth.module(new EcoMb())));
    }
}
