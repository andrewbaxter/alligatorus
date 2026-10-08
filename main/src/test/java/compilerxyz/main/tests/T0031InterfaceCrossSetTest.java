package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0031InterfaceCrossSetTest {
    private static SourceExpr twoSets(TestMbShorthand e) {
        return e.lower(e.seq(
                e.let("da", e.define()),
                e.let("iface", e.callM0(e.v("da"), "interface")),
                e.interfaceImpl(e.v("iface")),
                e.callM(
                        e.v("iface"),
                        "func",
                        e.rec(
                                e.p("name", e.s("to_string")),
                                e.p("arg", e.mb.tupleOf()),
                                e.p("ret", e.retSome(e.mb.typesString())))),
                e.finish(e.v("da")),
                e.let("db", e.define()),
                e.let("upper", e.defObject(e.v("db"), "upper")),
                e.objectImpl(e.v("upper"), e.mb.typesString()),
                e.implInterface(
                        e.v("upper"),
                        e.fld(e.v("iface"), "type"),
                        e.p("to_string", e.call(e.fld(e.v("inner"), "toUpperCase"), e.tup()))),
                e.let("mk", e.defFunc(e.v("db"), "make_upper")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.typesString(),
                        e.retSome(e.fld(e.v("upper"), "type")),
                        e.mb.optNoneType(),
                        e.stage(e.call(e.lower(e.fld(e.v("upper"), "new")), e.v("arg")))),
                e.finish(e.v("db")),
                e.ret(e.rec(
                        e.p("make_upper", e.fld(e.v("mk"), "func")), e.p("iface_t", e.fld(e.v("iface"), "type"))))));
    }

    private static SourceExpr callViaInterface(TestMbShorthand e, SourceExpr lib, SourceExpr value) {
        return e.call(e.fld(lib, "iface_t", "to_string"), value);
    }

    @Test
    public void test00000CrossSetInterfaceCallInModule() {
        var e = new TestMbShorthand();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        e.let("lib", twoSets(e)),
                        e.let("u", e.call(e.fld(e.v("lib"), "make_upper"), e.s("hello"))),
                        e.call(e.mb.log(), callViaInterface(e, e.v("lib"), e.v("u"))))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        Compile.CompileResult ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> ok = o.value();
            case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
        }
        assertEquals(1, ok.logs().size());
        assertEquals("HELLO", ok.logs().get(0).message());
    }

    @Test
    public void test00100CrossSetInterfaceCallThroughImport(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        Helpers.writeModule(src.resolve("lib.at"), TList.of(e.ret(twoSets(e))));
        Helpers.writeModule(
                src.resolve("main.at"),
                TList.of(
                        e.let("lib", e.importLocal("lib", "lib.at")),
                        e.let("u", e.call(e.fld(e.v("lib"), "make_upper"), e.s("hello"))),
                        e.call(e.mb.log(), callViaInterface(e, e.v("lib"), e.v("u")))));
        var ok = Helpers.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals("HELLO", ok.logs().get(0).message());
    }
}
