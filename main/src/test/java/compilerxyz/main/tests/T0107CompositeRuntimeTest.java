package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0107CompositeRuntimeTest {
    private static TList<String> compileLogs(SourceExpr main) {
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(TList.of(main)));
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

    private static SourceExpr elem(TestMbShorthand e, SourceExpr tuple, double index) {
        return e.mb.field(tuple, e.mb.num(index));
    }

    private static SourceExpr concatFn(TestMbShorthand e, SourceExpr argType, SourceExpr first, SourceExpr second) {
        return e.let(
                "g",
                e.lower(e.seq(
                        e.let("d", e.define()),
                        e.let("fb", e.defFunc(e.v("d"), "g")),
                        e.funcImpl(
                                e.v("fb"),
                                argType,
                                e.retSome(e.mb.typesString()),
                                e.mb.optNoneType(),
                                e.stage(e.cat(first, second))),
                        e.finish(e.v("d")),
                        e.ret(e.fld(e.v("fb"), "func")))));
    }

    @Test
    public void test00000TupleElementOfBoundTuple() {
        var e = new TestMbShorthand();
        var main = e.seq(e.let("t", e.tup(e.dstr("t0"), e.dstr("t1"))), e.call(e.mb.log(), elem(e, e.v("t"), 1)));
        assertEquals(TList.of("t1"), compileLogs(main));
    }

    @Test
    public void test00100BoundTuplePassedToFunction() {
        var e = new TestMbShorthand();
        var main = e.seq(
                concatFn(
                        e,
                        e.tup(e.mb.typesString(), e.mb.typesString()),
                        elem(e, e.v("arg"), 0),
                        elem(e, e.v("arg"), 1)),
                e.let("t", e.tup(e.dstr("tu"), e.dstr("ple"))),
                e.call(e.mb.log(), e.call(e.v("g"), e.v("t"))));
        assertEquals(TList.of("tuple"), compileLogs(main));
    }

    @Test
    public void test00200BoundRecordPassedToFunction() {
        var e = new TestMbShorthand();
        var main = e.seq(
                concatFn(
                        e,
                        e.rec(e.p("a", e.mb.typesString()), e.p("b", e.mb.typesString())),
                        e.fld(e.v("arg"), "a"),
                        e.fld(e.v("arg"), "b")),
                e.let("r", e.rec(e.p("a", e.dstr("re")), e.p("b", e.dstr("cord")))),
                e.call(e.mb.log(), e.call(e.v("g"), e.v("r"))));
        assertEquals(TList.of("record"), compileLogs(main));
    }

    @Test
    public void test00300OptionOfRecordValueField() {
        var e = new TestMbShorthand();
        var recT = e.rec(e.p("a", e.mb.typesString()));
        var main = e.seq(
                e.let("o", e.optSome(recT, e.rec(e.p("a", e.dstr("via-value"))))),
                e.ifOnly(e.fld(e.v("o"), "is_some"), e.call(e.mb.log(), e.fld(e.v("o"), "value", "a"))));
        assertEquals(TList.of("via-value"), compileLogs(main));
    }

    @Test
    public void test00400ForeachListOfRecords() {
        var e = new TestMbShorthand();
        var recT = e.rec(e.p("a", e.mb.typesString()));
        var main = e.seq(
                e.let("l", e.listNew(recT, e.rec(e.p("a", e.dstr("x"))), e.rec(e.p("a", e.dstr("y"))))),
                e.foreach(e.v("l"), "it", e.call(e.mb.log(), e.fld(e.v("it"), "a"))));
        assertEquals(TList.of("x", "y"), compileLogs(main));
    }

    @Test
    public void test00500MatchOptionOfString() {
        var e = new TestMbShorthand();
        var main = e.seq(
                e.let("o", e.optSome(e.mb.typesString(), e.dstr("some-s"))),
                e.matchOf(
                        e.v("o"),
                        "x",
                        e.p("some", e.call(e.mb.log(), e.v("x"))),
                        e.p("none", e.call(e.mb.log(), e.s("none")))));
        assertEquals(TList.of("some-s"), compileLogs(main));
    }

    @Test
    public void test00600MatchOptionOfRecord() {
        var e = new TestMbShorthand();
        var recT = e.rec(e.p("a", e.mb.typesString()));
        var main = e.seq(
                e.let("o", e.optNone(recT)),
                e.matchOf(
                        e.v("o"),
                        "x",
                        e.p("some", e.call(e.mb.log(), e.fld(e.v("x"), "a"))),
                        e.p("none", e.call(e.mb.log(), e.s("none")))));
        assertEquals(TList.of("none"), compileLogs(main));
    }

    @Test
    public void test00700MatchSpecializedOptionOfInt() {
        var e = new TestMbShorthand();
        var intT = e.mb.typesInt();
        var arms = new TestMbShorthand.P[] {
            e.p("some", e.call(e.mb.log(), e.toStr(e.v("x")))), e.p("none", e.call(e.mb.log(), e.s("none")))
        };
        var main = e.seq(e.matchOf(e.optSome(intT, e.intOf(7)), "x", arms), e.matchOf(e.optNone(intT), "x", arms));
        assertEquals(TList.of("7", "none"), compileLogs(main));
    }
}
