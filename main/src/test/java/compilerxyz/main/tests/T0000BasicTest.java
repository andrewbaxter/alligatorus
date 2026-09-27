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

public final class T0000BasicTest {
    @Test
    public void test00100CallLit() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.call(mb.log(), mb.str("Hello world (mortar)"))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("Hello world (mortar)", ok.logs().get(0).message());
    }

    @Test
    public void test00101TypeError() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.call(mb.log(), mb.tupleOf())));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> false;
                    case Result.Err<?, ?> e -> true;
                },
                () -> "expected compile error but got: " + res);
    }

    @Test
    public void test00200BindUse() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("hello", mb.str("Hello world (mortar)")), mb.call(mb.log(), mb.scopeUse("hello")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("Hello world (mortar)", ok.logs().get(0).message());
    }

    @Test
    public void test00300StageLower() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.call(mb.log(), mb.lower(mb.str("Hello world (mortar)")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("Hello world (mortar)", ok.logs().get(0).message());
    }

    @Test
    public void test00400LowerNostage() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.call(mb.log(), mb.lower(mb.str("Hello world (mortar)")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("Hello world (mortar)", ok.logs().get(0).message());
    }

    @Test
    public void test00500MethodCall() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.call(
                        mb.log(), mb.call(mb.field(mb.str("HELLO WORLD"), mb.str("toLowerCase")), mb.tupleOf()))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
    }
}
