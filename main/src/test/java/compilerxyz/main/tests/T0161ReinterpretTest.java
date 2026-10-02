package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0161ReinterpretTest {
    private static SourceExpr lib(EcoMb e) {
        return e.lower(e.seq(
                e.let("d", e.define()),
                e.let("oa", e.defObject(e.v("d"), "a")),
                e.objectImpl(e.v("oa"), e.mb.typesString()),
                e.let("ob", e.defObject(e.v("d"), "b")),
                e.objectImpl(e.v("ob"), e.mb.typesString()),
                e.methodImpl(e.v("oa"), "get", e.tup(), e.retSome(e.mb.typesString()), e.stage(e.v("inner"))),
                e.methodImpl(e.v("ob"), "get", e.tup(), e.retSome(e.mb.typesString()), e.stage(e.v("inner"))),
                e.let("mk", e.defFunc(e.v("d"), "mk_a")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.typesString(),
                        e.retSome(e.fld(e.v("oa"), "type")),
                        e.mb.optNoneType(),
                        e.stage(e.call(e.lower(e.fld(e.v("oa"), "new")), e.v("arg")))),
                e.finish(e.v("d")),
                e.ret(e.rec(
                        e.p("mk_a", e.fld(e.v("mk"), "func")),
                        e.p("a_t", e.fld(e.v("oa"), "type")),
                        e.p("b_t", e.fld(e.v("ob"), "type"))))));
    }

    @Test
    public void test00000WrongObjectFails() {
        var e = new EcoMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        e.let("l", lib(e)),
                        e.let("av", e.call(e.fld(e.v("l"), "mk_a"), e.s("hi"))),
                        e.call(
                                e.mb.log(),
                                e.callM0(
                                        e.call(e.mb.mustReinterpret(), e.tup(e.v("av"), e.fld(e.v("l"), "b_t"))),
                                        "get")))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> ok -> false;
                    case Result.Err<?, ?> err -> true;
                },
                () -> "reinterpreting an object as a different object type must fail; got: " + res);
        var combined = res.toString();
        assertTrue(
                combined.contains("ClassCastException"), () -> "expected a cast failure to surface; got: " + combined);
    }

    private static TList<String> reinterpretLogs(String targetType) {
        var e = new EcoMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        e.let("l", lib(e)),
                        e.let("av", e.call(e.fld(e.v("l"), "mk_a"), e.s("hi"))),
                        e.let("loc", e.call(e.evalBuiltin("expr_loc"), e.stage(e.tup()))),
                        e.matchOf(
                                e.call(e.mb.reinterpret(), e.tup(e.v("loc"), e.v("av"), e.fld(e.v("l"), targetType))),
                                "r",
                                Opt.none(),
                                e.p("ok", e.call(e.mb.log(), e.callM0(e.v("r"), "get"))),
                                e.p(
                                        "errs",
                                        e.foreach(
                                                e.v("r"),
                                                "err",
                                                e.call(e.mb.log(), e.callM(e.v("err"), "message", e.tup()))))))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, ?> ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
            case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
        }
        var out = new TList<String>();
        for (var log : ok.value().logs()) out.add(log.message());
        return out;
    }

    @Test
    public void test00100ReinterpretMatchIsOk() {
        assertEquals(TList.of("hi"), reinterpretLogs("a_t"));
    }

    @Test
    public void test00200ReinterpretMismatchIsError() {
        var logs = reinterpretLogs("b_t");
        assertEquals(1, logs.size(), () -> "expected one error; got: " + logs);
        assertTrue(logs.get(0).contains("reinterpret: value is not a "), () -> "expected mismatch error; got: " + logs);
    }
}
