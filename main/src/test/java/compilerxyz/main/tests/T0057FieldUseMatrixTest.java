package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0057FieldUseMatrixTest {
    private static final IList<String> NUMERICS =
            IList.of("byte", "short", "int", "long", "ubyte", "ushort", "uint", "ulong");
    private static final IList<String> FLOATS = IList.of("float", "double");

    @Test
    public void test00000PrimitiveRecordFieldUse() {
        var e = new EcoMb();
        var exprs = new TList<SourceExpr>();
        var expected = new TList<String>();

        for (var ty : NUMERICS) {
            var dyn = e.mb.as(e.n(5.), e.mb.builtin("types", ty));
            exprs.add(e.let("r_" + ty, e.rec(e.p("v", dyn))));
            exprs.add(e.let("g_" + ty, e.fld(e.v("r_" + ty), "v")));
            exprs.add(e.ifElse(
                    e.mb.operatorEq(e.v("g_" + ty), e.mb.as(e.n(5.), e.mb.builtin("types", ty))),
                    e.call(e.mb.log(), e.s("ok:" + ty)),
                    e.call(e.mb.log(), e.s("bad:" + ty))));
            expected.add("ok:" + ty);
        }
        for (var ty : FLOATS) {
            var dyn = e.mb.as(e.n(1.5), e.mb.builtin("types", ty));
            exprs.add(e.let("r_" + ty, e.rec(e.p("v", dyn))));
            exprs.add(e.let("g_" + ty, e.fld(e.v("r_" + ty), "v")));
            exprs.add(e.ifElse(
                    e.mb.operatorEq(e.v("g_" + ty), e.mb.as(e.n(1.5), e.mb.builtin("types", ty))),
                    e.call(e.mb.log(), e.s("ok:" + ty)),
                    e.call(e.mb.log(), e.s("bad:" + ty))));
            expected.add("ok:" + ty);
        }
        exprs.add(e.let(
                "r_bool",
                e.rec(e.p(
                        "v", e.mb.operatorLt(e.mb.as(e.n(0.), e.mb.typesInt()), e.mb.as(e.n(1.), e.mb.typesInt()))))));
        exprs.add(e.let("g_bool", e.fld(e.v("r_bool"), "v")));
        exprs.add(e.ifElse(e.v("g_bool"), e.call(e.mb.log(), e.s("ok:bool")), e.call(e.mb.log(), e.s("bad:bool"))));
        expected.add("ok:bool");
        exprs.add(e.let("r_string", e.rec(e.p("v", e.toStr(e.mb.as(e.n(7.), e.mb.typesInt()))))));
        exprs.add(e.let("g_string", e.fld(e.v("r_string"), "v")));
        exprs.add(e.call(e.mb.log(), e.v("g_string")));
        expected.add("7");

        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(exprs));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, ?> ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
            case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
        }
        assertEquals(expected.size(), ok.value().logs().size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), ok.value().logs().get(i).message());
        }
    }
}
