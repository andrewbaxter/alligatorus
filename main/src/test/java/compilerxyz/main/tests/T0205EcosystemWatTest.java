package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0200EcosystemLlvmTest.compileOk;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.onPath;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.writeModule;
import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0205EcosystemWatTest {
    static void writeTarget(Path src) throws IOException {
        Files.createDirectories(src);
        writeModule(src.resolve("wat-2.0.atz"), EcoWat.module(new EcoMb()));
        writeModule(src.resolve("wasi-0.1.atz"), EcoWasi.module(new EcoMb()));
    }

    private static void writeHello(Path src, EcoHelloWasi.Mode mode) throws IOException {
        writeTarget(src);
        writeModule(src.resolve("hello-wasi.atz"), EcoHelloWasi.module(new EcoMb(), mode));
    }

    private static String run(Path dir, String... argv) throws IOException {
        try {
            var proc = new ProcessBuilder(argv)
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();
            var output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var code = proc.waitFor();
            Assertions.assertEquals(0, code, () -> String.join(" ", argv) + " failed:\n" + output);
            return output;
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private static int exitCode(Path dir, String... argv) throws IOException {
        try {
            var proc = new ProcessBuilder(argv)
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();
            proc.getInputStream().readAllBytes();
            return proc.waitFor();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    static String helloBytes() {
        return EcoHelloWasi.MESSAGE.replace("\n", "\\0a") + "\\00";
    }

    static final String WASI_IMPORTS =
            """
            (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32) (param i32) (param i32) (param i32) (result i32)))
            """
                    .indent(2);

    static String expectedHelloWat() {
        return "(module\n"
                + WASI_IMPORTS
                + """
                  (memory (export "memory") 1)
                  (data (i32.const 8) "%s")
                  (data (i32.const 24) "\\08\\00\\00\\00\\0c\\00\\00\\00")
                  (data (i32.const 32) "\\00\\00\\00\\00")
                  (func $_start
                    i32.const 1
                    i32.const 24
                    i32.const 1
                    i32.const 32
                    call $fd_write
                    drop
                  )
                  (export "_start" (func $_start))
                )
                """
                        .formatted(helloBytes());
    }

    @Test
    public void test00000HelloWat(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        writeHello(src, EcoHelloWasi.Mode.WAT);
        var ok = compileOk(src.resolve("hello-wasi.atz"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var wat = ok.logs().get(0).message();
        assertEquals(expectedHelloWat(), wat);
        var file = tmp.resolve("hello.wat");
        Files.writeString(file, wat);
        if (onPath("wat2wasm"))
            run(
                    tmp,
                    "wat2wasm",
                    file.toString(),
                    "-o",
                    tmp.resolve("hello.wasm").toString());
        if (onPath("wasmtime")) assertEquals(EcoHelloWasi.MESSAGE, run(tmp, "wasmtime", file.toString()));
    }

    private static TList<SourceExpr> exitCodeModule(EcoMb e) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        setup.add(e.let("wasi", e.importLocal("wasi", "wasi-0.1.atz")));
        setup.add(e.let("m", e.call(e.fld(e.v("wasm"), "new"), e.tup())));
        EcoWat.bindTypes(e, setup);
        setup.add(e.let("proc_exit_fn_t", e.callM(e.fld(e.v("wasi"), "fn_types"), "get", e.s("proc_exit"))));
        setup.add(e.let(
                "f_fn_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType(), e.v("t_i32"))), e.p("ret", e.v("t_i32"))))));
        setup.add(e.let(
                "start_fn_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType())), e.p("ret", e.v("t_void"))))));
        var exported = new String[] {"m", "proc_exit_fn_t", "f_fn_t", "start_fn_t", "t_u32", "t_u8", "t_i8", "t_i32"};
        var exportedFields = new TList<EcoMb.P>();
        for (var n : exported) exportedFields.add(e.p(n, e.v(n)));
        setup.add(e.ret(e.rec(exportedFields.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("wasi", e.importLocal("wasi", "wasi-0.1.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : exported) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let(
                "proc_exit",
                e.call(
                        e.fld(e.v("wasm"), "import_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("module", e.fld(e.v("wasi"), "module")),
                                e.p("name", e.s("proc_exit")),
                                e.p("ty", e.v("proc_exit_fn_t"))))));
        top.add(e.let(
                "f",
                e.call(
                        e.fld(e.v("wasm"), "func_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("f")),
                                e.p("params", e.tup(e.s("x"))),
                                e.p("ty", e.v("f_fn_t")),
                                e.p(
                                        "body",
                                        e.stage(e.seq(
                                                e.let("y", e.mb.operatorAdd(e.v("x"), e.n(1))),
                                                e.ifOnly(e.mb.operatorLt(e.v("y"), e.n(10)), e.ret(e.v("y"))),
                                                e.ret(
                                                        e.ifElse(
                                                                e.mb.operatorGt(e.v("y"), e.n(50)),
                                                                e.mb.as(e.n(1), e.lower(e.fld(e.v("wasm"), "i32"))),
                                                                e.mb.operatorMul(e.v("y"), e.n(2)))))))))));
        var calls = new TList<SourceExpr>();
        for (var arg : new int[] {5, 20, 100}) calls.add(e.call(e.lower(e.v("f")), e.n(arg)));
        var narrowed = e.mb.operatorAdd(
                e.call(
                        e.lower(e.v("t_i32")),
                        e.call(e.lower(e.v("t_u8")), e.mb.operatorAdd(e.call(e.lower(e.v("f")), e.n(100)), e.n(299)))),
                e.call(
                        e.lower(e.v("t_i32")),
                        e.call(e.lower(e.v("t_i8")), e.mb.operatorAdd(e.call(e.lower(e.v("f")), e.n(20)), e.n(158)))));
        top.add(e.let(
                "start",
                e.call(
                        e.fld(e.v("wasm"), "func_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("_start")),
                                e.p("params", e.tup()),
                                e.p("ty", e.v("start_fn_t")),
                                e.p(
                                        "body",
                                        e.stage(e.call(
                                                e.lower(e.v("proc_exit")),
                                                e.call(
                                                        e.lower(e.v("t_u32")),
                                                        e.mb.operatorAdd(
                                                                e.mb.operatorAdd(
                                                                        e.mb.operatorAdd(calls.get(0), calls.get(1)),
                                                                        calls.get(2)),
                                                                narrowed)))))))));
        top.add(e.callM(e.v("m"), "export", e.s("_start")));
        top.add(e.call(e.mb.log(), e.fld(e.callM0(e.v("m"), "render"), "wat")));
        return top;
    }

    @Test
    public void test00020ControlFlow(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        writeTarget(src);
        writeModule(src.resolve("main.at"), exitCodeModule(new EcoMb()));
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        var wat = ok.logs().get(0).message();
        var file = tmp.resolve("exit.wat");
        Files.writeString(file, wat);
        assertEquals(
                """
                  (func $f (param $a0 i32) (result i32)
                    (local $s2 i32)
                    (local $s6 i32)
                    block $L7
                    local.get $a0
                    i32.const 1
                    i32.add
                    local.set $s2
                    local.get $s2
                    i32.const 10
                    i32.lt_s
                    if
                    local.get $s2
                    local.set $s6
                    br $L7
                    end
                    local.get $s2
                    i32.const 50
                    i32.gt_s
                    if (result i32)
                    i32.const 1
                    else
                    local.get $s2
                    i32.const 2
                    i32.mul
                    end
                    local.set $s6
                    br $L7
                    end
                    local.get $s6
                    return
                  )
                """,
                wat.substring(wat.indexOf("  (func $f"), wat.indexOf("  (func $_start")));
        if (onPath("wat2wasm"))
            run(tmp, "wat2wasm", file.toString(), "-o", tmp.resolve("exit.wasm").toString());
        if (onPath("wasmtime")) assertEquals(37, exitCode(tmp, "wasmtime", file.toString()));
        assertEquals(
                """
                  (func $_start
                    i32.const 5
                    call $f
                    i32.const 20
                    call $f
                    i32.add
                    i32.const 100
                    call $f
                    i32.add
                    i32.const 100
                    call $f
                    i32.const 299
                    i32.add
                    i32.const 255
                    i32.and
                    i32.const 20
                    call $f
                    i32.const 158
                    i32.add
                    i32.extend8_s
                    i32.add
                    i32.add
                    call $proc_exit
                  )
                """,
                wat.substring(wat.indexOf("  (func $_start"), wat.indexOf("  (export")));
    }

    private static TList<SourceExpr> importsModule(EcoMb e, String... names) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        setup.add(e.let("wasi", e.importLocal("wasi", "wasi-0.1.atz")));
        setup.add(e.let("m", e.call(e.fld(e.v("wasm"), "new"), e.tup())));
        var fields = new TList<EcoMb.P>();
        fields.add(e.p("m", e.v("m")));
        for (var n : names) {
            setup.add(e.let(n + "_t", e.callM(e.fld(e.v("wasi"), "fn_types"), "get", e.s(n))));
            fields.add(e.p(n + "_t", e.v(n + "_t")));
        }
        setup.add(e.ret(e.rec(fields.toArray(EcoMb.P[]::new))));
        var top = new TList<SourceExpr>();
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("wasi", e.importLocal("wasi", "wasi-0.1.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        top.add(e.let("m", e.fld(e.v("k"), "m")));
        for (var n : names) {
            top.add(e.call(
                    e.fld(e.v("wasm"), "import_fn"),
                    e.rec(
                            e.p("mod", e.v("m")),
                            e.p("module", e.fld(e.v("wasi"), "module")),
                            e.p("name", e.s(n)),
                            e.p("ty", e.fld(e.v("k"), n + "_t")))));
        }
        top.add(e.call(e.mb.log(), e.fld(e.callM0(e.v("m"), "render"), "wat")));
        return top;
    }

    @Test
    public void test00005WitxLowering(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        writeTarget(src);
        writeModule(
                src.resolve("main.at"),
                importsModule(
                        new EcoMb(),
                        "args_sizes_get",
                        "args_get",
                        "fd_seek",
                        "path_open",
                        "clock_time_get",
                        "random_get",
                        "proc_exit",
                        "fd_prestat_get",
                        "sock_recv"));
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        var wat = ok.logs().get(0).message();
        assertEquals(
                """
                (module
                  (import "wasi_snapshot_preview1" "args_sizes_get" (func $args_sizes_get (param i32) (param i32) (result i32)))
                  (import "wasi_snapshot_preview1" "args_get" (func $args_get (param i32) (param i32) (result i32)))
                  (import "wasi_snapshot_preview1" "fd_seek" (func $fd_seek (param i32) (param i64) (param i32) (param i32) (result i32)))
                  (import "wasi_snapshot_preview1" "path_open" (func $path_open (param i32) (param i32) (param i32) (param i32) (param i32) (param i64) (param i64) (param i32) (param i32) (result i32)))
                  (import "wasi_snapshot_preview1" "clock_time_get" (func $clock_time_get (param i32) (param i64) (param i32) (result i32)))
                  (import "wasi_snapshot_preview1" "random_get" (func $random_get (param i32) (param i32) (result i32)))
                  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
                  (import "wasi_snapshot_preview1" "fd_prestat_get" (func $fd_prestat_get (param i32) (param i32) (result i32)))
                  (import "wasi_snapshot_preview1" "sock_recv" (func $sock_recv (param i32) (param i32) (param i32) (param i32) (param i32) (param i32) (result i32)))
                  (memory (export "memory") 1)
                )
                """,
                wat);
        if (onPath("wat2wasm")) {
            var file = tmp.resolve("imports.wat");
            Files.writeString(file, wat);
            run(
                    tmp,
                    "wat2wasm",
                    file.toString(),
                    "-o",
                    tmp.resolve("imports.wasm").toString());
        }
    }

    @Test
    public void test00010HelloApp(@TempDir Path tmp) throws IOException {
        Assumptions.assumeTrue(onPath("wasmtime"), "wasmtime not on PATH");
        var src = tmp.resolve("src");
        writeHello(src, EcoHelloWasi.Mode.APP);
        var app = T0049AppTest.compileApp(tmp.resolve("cache"), src.resolve("hello-wasi.atz"));
        assertEquals("wasmtime", app.executor().optUnwrap());
        assertEquals(expectedHelloWat(), new String(app.bytes(), StandardCharsets.UTF_8));
        var exe = tmp.resolve("hello");
        Files.write(exe, app.bytes());
        assertEquals(EcoHelloWasi.MESSAGE, run(tmp, app.executor().optUnwrap(), exe.toString()));
    }
}
