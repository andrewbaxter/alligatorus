package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0041ListIndexTest {

    private static TList<String> run(TList<SourceExpr> exprs) {
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(exprs));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> resOk = ok;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                throw new AssertionError("compile failed: " + res);
        }
        var messages = new TList<String>();
        for (var entry : resOk.value().logs()) messages.add(entry.message());
        return messages;
    }

    private static SourceExpr intToString(TestMb mb, SourceExpr intExpr) {
        return mb.call(mb.field(intExpr, mb.str("to_string")), mb.tupleOf());
    }

    @Test
    public void test00000SpecializedLenGetSet() {
        var mb = new TestMb();
        var exprs = new TList<SourceExpr>();
        exprs.add(mb.bind(
                "l",
                mb.call(
                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                        mb.tupleOf(mb.num(5), mb.num(6), mb.num(7)))));
        exprs.add(mb.call(mb.log(), intToString(mb, mb.field(mb.scopeUse("l"), mb.str("len")))));
        exprs.add(mb.call(mb.log(), intToString(mb, mb.call(mb.field(mb.scopeUse("l"), mb.str("get")), mb.num(1)))));
        exprs.add(mb.bind("i", mb.as(mb.num(2), mb.typesInt())));
        exprs.add(mb.call(mb.field(mb.scopeUse("l"), mb.str("set")), mb.tupleOf(mb.scopeUse("i"), mb.num(9))));
        exprs.add(mb.call(
                mb.log(), intToString(mb, mb.call(mb.field(mb.scopeUse("l"), mb.str("get")), mb.scopeUse("i")))));
        exprs.add(mb.call(mb.log(), intToString(mb, mb.call(mb.field(mb.scopeUse("l"), mb.str("get")), mb.num(0)))));
        assertEquals(TList.of("3", "6", "9", "5"), run(exprs));
    }

    @Test
    public void test00100BoxedLenGetSet() {
        var mb = new TestMb();
        var exprs = new TList<SourceExpr>();
        exprs.add(mb.bind(
                "l",
                mb.call(
                        mb.field(mb.call(mb.typesList(), mb.typesString()), mb.str("new")),
                        mb.tupleOf(mb.str("a"), mb.str("b")))));
        exprs.add(mb.call(mb.log(), intToString(mb, mb.field(mb.scopeUse("l"), mb.str("len")))));
        exprs.add(mb.call(mb.field(mb.scopeUse("l"), mb.str("set")), mb.tupleOf(mb.num(0), mb.str("c"))));
        exprs.add(mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("l"), mb.str("get")), mb.num(0))));
        exprs.add(mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("l"), mb.str("join")), mb.str("_"))));
        assertEquals(TList.of("2", "c", "c_b"), run(exprs));
    }
}
