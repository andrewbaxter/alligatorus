package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

public final class T0032InterfaceMethodArgsTest {
    private static SourceExpr lib(TestMbShorthand e) {
        return e.lower(e.seq(
                e.let("d", e.define()),
                e.let("iface", e.callM0(e.v("d"), "interface")),
                e.interfaceImpl(e.v("iface")),
                e.callM(
                        e.v("iface"),
                        "func",
                        e.rec(
                                e.p("name", e.s("scale")),
                                e.p("arg", e.rec(e.p("by", e.mb.typesInt()), e.p("label", e.mb.typesString()))),
                                e.p("ret", e.retSome(e.mb.typesString())))),
                e.callM(
                        e.v("iface"),
                        "func",
                        e.rec(
                                e.p("name", e.s("pair")),
                                e.p("arg", e.tup(e.mb.typesInt(), e.mb.typesInt())),
                                e.p("ret", e.retSome(e.mb.typesInt())))),
                e.let("counter", e.defObject(e.v("d"), "counter")),
                e.objectImpl(e.v("counter"), e.mb.typesInt()),
                e.implInterface(
                        e.v("counter"),
                        e.fld(e.v("iface"), "type"),
                        e.p(
                                "scale",
                                e.cat(
                                        e.fld(e.v("arg"), "label"),
                                        e.s("="),
                                        e.toStr(e.mb.operatorMul(e.v("inner"), e.fld(e.v("arg"), "by"))))),
                        e.p(
                                "pair",
                                e.mb.operatorAdd(
                                        e.v("inner"),
                                        e.mb.operatorMul(
                                                e.mb.field(e.v("arg"), e.mb.num(0)),
                                                e.mb.field(e.v("arg"), e.mb.num(1)))))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("make", e.fld(e.v("counter"), "new")), e.p("iface_t", e.fld(e.v("iface"), "type"))))));
    }

    private static TList<SourceExpr> useLib(TestMbShorthand e, SourceExpr lib) {
        return TList.of(
                e.let("lib", lib),
                e.let("c", e.call(e.fld(e.v("lib"), "make"), e.intOf(7))),
                e.call(
                        e.mb.log(),
                        e.call(
                                e.fld(e.v("lib"), "iface_t", "scale"),
                                e.rec(e.p("self", e.v("c")), e.p("by", e.intOf(3)), e.p("label", e.s("x"))))),
                e.call(
                        e.mb.log(),
                        e.toStr(e.call(e.fld(e.v("lib"), "iface_t", "pair"), e.tup(e.v("c"), e.intOf(2), e.intOf(5))))),
                e.call(
                        e.mb.log(),
                        e.pipe(
                                e.v("c"),
                                e.fld(e.v("lib"), "iface_t", "scale"),
                                e.rec(e.p("by", e.intOf(4)), e.p("label", e.s("y"))))),
                e.call(
                        e.mb.log(),
                        e.toStr(e.pipe(
                                e.v("c"), e.fld(e.v("lib"), "iface_t", "pair"), e.tup(e.intOf(1), e.intOf(6))))));
    }

    @Test
    public void test00000RecordAndTupleMethodArgs() {
        var e = new TestMbShorthand();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(useLib(e, lib(e))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        Compile.CompileResult ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> ok = o.value();
            case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
        }
        assertEquals(4, ok.logs().size());
        assertEquals("x=21", ok.logs().get(0).message());
        assertEquals("17", ok.logs().get(1).message());
        assertEquals("y=28", ok.logs().get(2).message());
        assertEquals("13", ok.logs().get(3).message());
    }

    @Test
    public void test00200PipeRecordWithSelfFieldIsAnError() {
        var e = new TestMbShorthand();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        e.let("lib", lib(e)),
                        e.let("c", e.call(e.fld(e.v("lib"), "make"), e.intOf(7))),
                        e.call(
                                e.mb.log(),
                                e.pipe(
                                        e.v("c"),
                                        e.fld(e.v("lib"), "iface_t", "scale"),
                                        e.rec(
                                                e.p("self", e.v("c")),
                                                e.p("by", e.intOf(3)),
                                                e.p("label", e.s("x"))))))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> throw new AssertionError("expected a compile error");
            case Result.Err<Compile.CompileResult, ?> err -> {
                var text = err.error().toString();
                assertTrue(text.contains("already has a 'self' field"), text);
            }
        }
    }

    @Test
    public void test00300ReservedSelfInterfaceArgIsAnError() {
        var e = new TestMbShorthand();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        e.let("d", e.define()),
                        e.let("iface", e.callM0(e.v("d"), "interface")),
                        e.interfaceImpl(e.v("iface")),
                        e.callM(
                                e.v("iface"),
                                "func",
                                e.rec(
                                        e.p("name", e.s("scale")),
                                        e.p("arg", e.rec(e.p("self", e.mb.typesInt()))),
                                        e.p("ret", e.retSome(e.mb.typesString())))),
                        e.finish(e.v("d")))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> throw new AssertionError("expected a compile error");
            case Result.Err<Compile.CompileResult, ?> err -> {
                var text = err.error().toString();
                assertTrue(text.contains("reserved for the receiver"), text);
            }
        }
    }

    @Test
    public void test00400ImplBodyWithoutValueIsAnError() {
        var e = new TestMbShorthand();
        var modules = new TMap<String, SourceModule>();
        var body = e.seq(
                e.let("a", e.intOf(1)),
                e.mb.labeled(
                        "m",
                        e.seq(e.ifOnly(e.v("arg"), e.mb.return_("m", e.intOf(3))), e.mb.return_("m", e.intOf(5)))));
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        e.let(
                                "lib",
                                e.lower(e.seq(
                                        e.let("d", e.define()),
                                        e.let("iface", e.callM0(e.v("d"), "interface")),
                                        e.interfaceImpl(e.v("iface")),
                                        e.callM(
                                                e.v("iface"),
                                                "func",
                                                e.rec(
                                                        e.p("name", e.s("pick")),
                                                        e.p("arg", e.mb.typesBool()),
                                                        e.p("ret", e.retSome(e.mb.typesInt())))),
                                        e.let("o", e.defObject(e.v("d"), "o")),
                                        e.objectImpl(e.v("o"), e.mb.typesInt()),
                                        e.implInterface(e.v("o"), e.fld(e.v("iface"), "type"), e.p("pick", body)),
                                        e.finish(e.v("d")),
                                        e.ret(e.rec(
                                                e.p("make", e.fld(e.v("o"), "new")),
                                                e.p("iface_t", e.fld(e.v("iface"), "type"))))))),
                        e.let("c", e.call(e.fld(e.v("lib"), "make"), e.intOf(7))),
                        e.call(
                                e.mb.log(),
                                e.toStr(e.pipe(e.v("c"), e.fld(e.v("lib"), "iface_t", "pick"), e.mb.bool(true)))))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> throw new AssertionError("expected a compile error");
            case Result.Err<Compile.CompileResult, ?> err -> {
                var text = err.error().toString();
                assertTrue(text.contains("Interface method 'pick' impl body produces no value"), text);
            }
        }
    }

    @Test
    public void test00100RecordAndTupleMethodArgsThroughImport(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        Helpers.writeModule(src.resolve("lib.at"), TList.of(e.ret(lib(e))));
        Helpers.writeModule(src.resolve("main.at"), useLib(e, e.importLocal("lib", "lib.at")));
        var ok = Helpers.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals("x=21", ok.logs().get(0).message());
        assertEquals("17", ok.logs().get(1).message());
    }
}
