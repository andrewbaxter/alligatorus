package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0090MapsTest {
    @Test
    public void test00000MapStringKeys() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "my_map",
                                mb.call(
                                        mb.field(
                                                mb.call(mb.typesMap(), mb.tupleOf(mb.typesString(), mb.typesIntBox())),
                                                mb.str("new")),
                                        mb.tupleOf())),
                        mb.call(mb.field(mb.scopeUse("my_map"), mb.str("put")), mb.tupleOf(mb.str("a"), mb.num(10.))),
                        mb.call(mb.field(mb.scopeUse("my_map"), mb.str("put")), mb.tupleOf(mb.str("b"), mb.num(20.))),
                        mb.foreach(
                                mb.scopeUse("my_map"),
                                mb.str("entry"),
                                mb.seqOf(
                                        mb.call(mb.log(), mb.field(mb.scopeUse("entry"), mb.str("key"))),
                                        mb.call(
                                                mb.log(),
                                                mb.call(
                                                        mb.field(
                                                                mb.field(mb.scopeUse("entry"), mb.str("value")),
                                                                mb.str("to_string")),
                                                        mb.tupleOf())))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(4, ok.logs().size());
        assertEquals("a", ok.logs().get(0).message());
        assertEquals("10", ok.logs().get(1).message());
        assertEquals("b", ok.logs().get(2).message());
        assertEquals("20", ok.logs().get(3).message());
    }

    @Test
    public void test00010MapStringString() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "my_map",
                                mb.call(
                                        mb.field(
                                                mb.call(mb.typesMap(), mb.tupleOf(mb.typesString(), mb.typesString())),
                                                mb.str("new")),
                                        mb.tupleOf())),
                        mb.call(
                                mb.field(mb.scopeUse("my_map"), mb.str("put")),
                                mb.tupleOf(mb.str("x"), mb.str("hello"))),
                        mb.call(
                                mb.field(mb.scopeUse("my_map"), mb.str("put")),
                                mb.tupleOf(mb.str("y"), mb.str("world"))),
                        mb.foreach(
                                mb.scopeUse("my_map"),
                                mb.str("entry"),
                                mb.seqOf(
                                        mb.call(mb.log(), mb.field(mb.scopeUse("entry"), mb.str("key"))),
                                        mb.call(mb.log(), mb.field(mb.scopeUse("entry"), mb.str("value"))))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(4, ok.logs().size());
        assertEquals("x", ok.logs().get(0).message());
        assertEquals("hello", ok.logs().get(1).message());
        assertEquals("y", ok.logs().get(2).message());
        assertEquals("world", ok.logs().get(3).message());
    }

    private static SourceModule mapGetModule(TestMb mb, SourceExpr getCall) {
        return new SourceModule.V1(TList.of(
                mb.bind(
                        "my_map",
                        mb.call(
                                mb.field(
                                        mb.call(mb.typesMap(), mb.tupleOf(mb.typesString(), mb.typesString())),
                                        mb.str("new")),
                                mb.tupleOf())),
                mb.call(mb.field(mb.scopeUse("my_map"), mb.str("put")), mb.tupleOf(mb.str("a"), mb.str("hello"))),
                mb.call(mb.log(), getCall)));
    }

    private static SourceExpr mapGetCall(TestMb mb, String lookup) {
        return mb.call(mb.field(mb.scopeUse("my_map"), mb.str("get")), mb.str(lookup));
    }

    @Test
    public void test00020MapGetPresent() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", mapGetModule(mb, mapGetCall(mb, "a")));
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
        assertEquals("hello", ok.logs().get(0).message());
    }

    @Test
    public void test00030MapGetMissingPanics() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var getCall = mapGetCall(mb, "zzz");
        modules.put("main", mapGetModule(mb, getCall));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        var errs =
                switch (res) {
                    case Result.Ok<Compile.CompileResult, TList<ExprErr>> o ->
                        throw new AssertionError("missing key should panic; got: " + res);
                    case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> e.error();
                };
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        var err = errs.get(0);
        assertEquals(
                getCall.id().value(),
                err.loc().expr().optUnwrap(),
                () -> "panic should point at the get call; got: " + err);
        assertTrue(
                err.message().contains("panic: map.get: missing key zzz"),
                () -> "expected missing-key panic; got: " + err.message());
    }
}
