package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0200EcosystemLlvmTest.compileOk;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.writeModule;
import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0210EcosystemJsTest {
    public static final String TARGET_FILE = "js-es2020.atz";

    @Test
    public void test00000Hello(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve(TARGET_FILE), EcoJs.module(new EcoMb()));
        var e = new EcoMb();
        var setup = new TList<SourceExpr>();
        setup.add(e.let("js", e.importLocal("js", TARGET_FILE)));
        setup.add(e.let("m", e.call(e.fld(e.v("js"), "new"), e.tup())));
        setup.add(e.ret(e.rec(e.p("m", e.v("m")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("js", e.importLocal("js", TARGET_FILE)));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        top.add(e.let("m", e.fld(e.v("k"), "m")));
        top.add(e.let("console", e.call(e.fld(e.v("js"), "global_fn"), e.rec(e.p("name", e.s("console"))))));
        top.add(e.let("decoder", e.call(e.fld(e.v("js"), "global_fn"), e.rec(e.p("name", e.s("TextDecoder"))))));
        top.add(e.let("mk", e.call(e.fld(e.v("js"), "new_fn"), e.tup())));
        top.add(e.let(
                "greet",
                e.call(
                        e.fld(e.v("js"), "func_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("greet")),
                                e.p("params", e.tup(e.s("who"))),
                                e.p(
                                        "body",
                                        e.stage(e.seq(e.call(
                                                e.fld(e.lower(e.v("console")), "log"),
                                                e.tup(e.s("hello, \"world\"\n"), e.v("who"))))))))));
        top.add(e.let(
                "state", e.call(e.fld(e.v("js"), "var_fn"), e.rec(e.p("mod", e.v("m")), e.p("name", e.s("state"))))));
        top.add(e.callM(
                e.v("m"),
                "run",
                e.stage(e.seq(
                        e.let("d", e.call(e.lower(e.v("mk")), e.tup(e.lower(e.v("decoder")), e.s("utf-8")))),
                        e.call(
                                e.lower(e.v("greet")),
                                e.rec(
                                        e.p("name", e.fld(e.v("d"), "encoding")),
                                        e.p("n", e.n(3)),
                                        e.p("ok", e.b(true)))),
                        e.let("first", e.mb.field(e.v("d"), e.n(0))),
                        e.mb.set(e.fld(e.v("d"), "encoding"), e.s("x")),
                        e.mb.set(e.lower(e.v("state")), e.v("first"))))));
        top.add(e.callM(
                e.v("m"),
                "assign_index",
                e.rec(e.p("target", e.s("imports")), e.p("key", e.s("a \"b\"")), e.p("value", e.s("greet")))));
        top.add(e.call(e.mb.log(), e.fld(e.callM0(e.v("m"), "render"), "js")));

        writeModule(src.resolve("main.at"), top);
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(1, ok.logs().size());
        assertEquals(
                """
                function greet(who) {
                  const $0 = console.log("hello, \\"world\\"\\n", who);
                }
                let state;
                const $1 = new TextDecoder("utf-8");
                const $2 = $1;
                const $3 = greet({name: $2.encoding, n: 3, ok: true});
                const $4 = $2[0];
                $2.encoding = "x";
                state = $4;
                imports["a \\"b\\""] = greet;
                """,
                ok.logs().get(0).message());
    }
}
