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

public final class T0106TypeCompositeTest {
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

    private static SourceExpr elem(EcoMb e, SourceExpr tuple, double index) {
        return e.mb.field(tuple, e.mb.num(index));
    }

    @Test
    public void test00000AsRecordType() {
        var e = new EcoMb();
        var main = e.seq(
                e.let("r", e.mb.as(e.rec(e.p("a", e.dstr("y"))), e.rec(e.p("a", e.mb.typesString())))),
                e.call(e.mb.log(), e.fld(e.v("r"), "a")));
        assertEquals(TList.of("y"), compileLogs(main));
    }

    @Test
    public void test00100AsNestedRecordType() {
        var e = new EcoMb();
        var main = e.seq(
                e.let(
                        "r",
                        e.mb.as(
                                e.rec(e.p("a", e.rec(e.p("b", e.dstr("nested"))))),
                                e.rec(e.p("a", e.rec(e.p("b", e.mb.typesString())))))),
                e.call(e.mb.log(), e.fld(e.v("r"), "a", "b")));
        assertEquals(TList.of("nested"), compileLogs(main));
    }

    @Test
    public void test00300FunctionRecordArgType() {
        var e = new EcoMb();
        var main = e.seq(
                e.let(
                        "g",
                        e.lower(e.seq(
                                e.let("d", e.define()),
                                e.let("fb", e.defFunc(e.v("d"), "g")),
                                e.funcImpl(
                                        e.v("fb"),
                                        e.rec(e.p("a", e.mb.typesString()), e.p("b", e.mb.typesString())),
                                        e.retSome(e.mb.typesString()),
                                        e.mb.optNoneType(),
                                        e.stage(e.cat(e.fld(e.v("arg"), "a"), e.fld(e.v("arg"), "b")))),
                                e.finish(e.v("d")),
                                e.ret(e.fld(e.v("fb"), "func"))))),
                e.call(e.mb.log(), e.call(e.v("g"), e.rec(e.p("a", e.dstr("ar")), e.p("b", e.dstr("gs"))))));
        assertEquals(TList.of("args"), compileLogs(main));
    }

    private static SourceExpr tupleConcatFn(EcoMb e, SourceExpr argType) {
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
                                e.stage(e.cat(elem(e, e.v("arg"), 0), elem(e, e.v("arg"), 1)))),
                        e.finish(e.v("d")),
                        e.ret(e.fld(e.v("fb"), "func")))));
    }

    @Test
    public void test00400AsTupleType() {
        var e = new EcoMb();
        var tupT = e.tup(e.mb.typesString(), e.mb.typesString());
        var main = e.seq(
                tupleConcatFn(e, tupT),
                e.call(e.mb.log(), e.call(e.v("g"), e.mb.as(e.tup(e.dstr("as-"), e.dstr("tuple")), tupT))));
        assertEquals(TList.of("as-tuple"), compileLogs(main));
    }

    @Test
    public void test00500FunctionTupleArgType() {
        var e = new EcoMb();
        var main = e.seq(
                tupleConcatFn(e, e.tup(e.mb.typesString(), e.mb.typesString())),
                e.call(e.mb.log(), e.call(e.v("g"), e.tup(e.dstr("tu"), e.dstr("ple")))));
        assertEquals(TList.of("tuple"), compileLogs(main));
    }

    @Test
    public void test00600RecordValueHoldingTypes() {
        var e = new EcoMb();
        var main = e.seq(
                e.let("types_rec", e.rec(e.p("t", e.mb.typesInt()))),
                e.call(e.mb.log(), e.toStr(e.mb.as(e.n(5.), e.fld(e.v("types_rec"), "t")))));
        assertEquals(TList.of("5"), compileLogs(main));
    }

    @Test
    public void test00700RecordTypeFromMap() {
        var e = new EcoMb();
        var main = e.seq(
                e.let(
                        "t",
                        e.lower(e.seq(
                                e.let(
                                        "m",
                                        e.mapNew(e.mb.typesConstkey(), e.mb.evalType(), e.p("a", e.mb.typesString()))),
                                e.callM(
                                        e.v("m"),
                                        "put",
                                        e.tup(e.mb.as(e.s("b"), e.mb.typesConstkey()), e.mb.typesString())),
                                e.ret(e.call(e.mb.builtin("types", "record"), e.v("m")))))),
                e.let("r", e.mb.as(e.rec(e.p("a", e.dstr("ay")), e.p("b", e.dstr("bee"))), e.v("t"))),
                e.call(e.mb.log(), e.fld(e.v("r"), "a")),
                e.call(e.mb.log(), e.fld(e.v("r"), "b")));
        assertEquals(TList.of("ay", "bee"), compileLogs(main));
    }

    @Test
    public void test00800TupleTypeFromList() {
        var e = new EcoMb();
        var main = e.seq(
                e.let(
                        "t",
                        e.lower(e.seq(
                                e.let("l", e.listNew(e.mb.evalType(), e.mb.typesString())),
                                e.callM(e.v("l"), "push", e.mb.typesString()),
                                e.ret(e.call(e.mb.builtin("types", "tuple"), e.v("l")))))),
                e.let("r", e.mb.as(e.tup(e.dstr("first"), e.dstr("second")), e.v("t"))),
                e.call(e.mb.log(), elem(e, e.v("r"), 0)),
                e.call(e.mb.log(), elem(e, e.v("r"), 1)));
        assertEquals(TList.of("first", "second"), compileLogs(main));
    }
}
