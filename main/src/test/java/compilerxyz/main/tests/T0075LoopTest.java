package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0075LoopTest {

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
    public void test00000LoopUntilReturn() {
        var mb = new TestMb();
        var exprs = new TList<SourceExpr>();
        exprs.add(mb.bind("i", mb.as(mb.num(0), mb.typesInt())));
        exprs.add(mb.labeled(
                "done",
                mb.loop(mb.seqOf(
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        mb.operatorGe(mb.scopeUse("i"), mb.num(3)),
                                        mb.return_(Opt.some("done"), Opt.none()))),
                                Opt.none()),
                        mb.call(mb.log(), intToString(mb, mb.scopeUse("i"))),
                        mb.set(mb.scopeUse("i"), mb.operatorAdd(mb.scopeUse("i"), mb.num(1)))))));
        exprs.add(mb.call(mb.log(), mb.str("after")));
        assertEquals(TList.of("0", "1", "2", "after"), run(exprs));
    }

    @Test
    public void test00100NestedLoops() {
        var mb = new TestMb();
        var exprs = new TList<SourceExpr>();
        exprs.add(mb.bind("n", mb.as(mb.num(0), mb.typesInt())));
        exprs.add(mb.bind("i", mb.as(mb.num(0), mb.typesInt())));
        exprs.add(mb.labeled(
                "outer",
                mb.loop(mb.seqOf(
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        mb.operatorGe(mb.scopeUse("i"), mb.num(2)),
                                        mb.return_(Opt.some("outer"), Opt.none()))),
                                Opt.none()),
                        mb.bind("j", mb.as(mb.num(0), mb.typesInt())),
                        mb.labeled(
                                "inner",
                                mb.loop(mb.seqOf(
                                        mb.ifOf(
                                                TList.of(
                                                        new TestMb.Branch(
                                                                mb.operatorGe(mb.scopeUse("j"), mb.num(3)),
                                                                mb.return_(Opt.some("inner"), Opt.none()))),
                                                Opt.none()),
                                        mb.set(mb.scopeUse("n"), mb.operatorAdd(mb.scopeUse("n"), mb.num(1))),
                                        mb.set(mb.scopeUse("j"), mb.operatorAdd(mb.scopeUse("j"), mb.num(1)))))),
                        mb.set(mb.scopeUse("i"), mb.operatorAdd(mb.scopeUse("i"), mb.num(1)))))));
        exprs.add(mb.call(mb.log(), intToString(mb, mb.scopeUse("n"))));
        assertEquals(TList.of("6"), run(exprs));
    }
}
