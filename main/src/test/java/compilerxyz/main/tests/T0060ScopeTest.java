package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0060ScopeTest {
    @Test
    public void test00000ScopeOk() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("x", mb.str("zebra")),
                        mb.seqOf(mb.bind("x", mb.str("apple")), mb.call(mb.log(), mb.scopeUse("x"))),
                        mb.call(mb.log(), mb.scopeUse("x")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> ok2 -> true;
                    case Result.Err<?, ?> err -> false;
                },
                () -> "compile failed: " + res);
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(2, ok.logs().size());
        assertEquals("apple", ok.logs().get(0).message());
        assertEquals("zebra", ok.logs().get(1).message());
    }

    @Test
    public void test00010ScopeBad() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(
                        TList.of(mb.seqOf(mb.bind("x", mb.str("apple"))), mb.call(mb.log(), mb.scopeUse("x")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> ok -> false;
                    case Result.Err<?, ?> err -> true;
                },
                () -> "expected compile error but got: " + res);
    }
}
