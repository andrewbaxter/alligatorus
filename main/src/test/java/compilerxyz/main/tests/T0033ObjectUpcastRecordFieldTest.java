package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0033ObjectUpcastRecordFieldTest {
    @Test
    public void test00000ObjectConstValueIntoValueTypedRecordField() {
        var e = new TestMbShorthand();
        var lib = e.lower(e.seq(
                e.let("d", e.define()),
                e.let("holder", e.defObject(e.v("d"), "holder")),
                e.objectImpl(e.v("holder"), e.rec(e.p("v", e.mb.evalValue()))),
                e.methodImpl(
                        e.v("holder"), "get", e.tup(), e.retSome(e.mb.evalValue()), e.stage(e.fld(e.v("inner"), "v"))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("make", e.fld(e.v("holder"), "new"))))));
        var main = TList.of(
                e.let("lib", lib),
                e.let(
                        "h",
                        e.call(
                                e.fld(e.v("lib"), "make"),
                                e.rec(e.p(
                                        "v",
                                        e.call(
                                                e.evalBuiltin("value_object_const"),
                                                e.rec(e.p("type", e.mb.typesInt()), e.p("data", e.tup()))))))),
                e.let("got", e.callM0(e.v("h"), "get")),
                e.call(e.mb.log(), e.s("ok")));
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(main));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        Compile.CompileResult ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> ok = o.value();
            case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
        }
        assertEquals(1, ok.logs().size());
        assertEquals("ok", ok.logs().get(0).message());
    }
}
