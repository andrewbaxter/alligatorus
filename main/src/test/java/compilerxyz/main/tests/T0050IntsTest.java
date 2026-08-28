package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0050IntsTest {
    @Test
    public void test00000IntLit() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("x", mb.as_(mb.num(4.0), mb.typesInt())),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        mb.operatorLt(mb.scopeUse("x"), mb.num(5.0)),
                                        mb.set(mb.scopeUse("x"), mb.operatorAdd(mb.scopeUse("x"), mb.num(7.0))))),
                                Opt.none()),
                        mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("x"), mb.str("to_string")), mb.tupleOf())))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok0 -> resOk = ok0;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + err);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals("11", ok.logs().get(0).message());
    }

    @Test
    public void test00100IntDynamic() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "maybe_add",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "builder",
                                                mb.call(
                                                        mb.field(mb.scopeUse("d"), mb.str("func")),
                                                        mb.str("maybe add 7"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("builder"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.typesInt()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesInt())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.seqOf(
                                                                                        mb.ifOf(
                                                                                                TList.of(
                                                                                                        new TestMb
                                                                                                                .Branch(
                                                                                                                mb
                                                                                                                        .operatorLt(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .num(
                                                                                                                                                5.0)),
                                                                                                                mb.set(
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "arg"),
                                                                                                                        mb
                                                                                                                                .operatorAdd(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .num(
                                                                                                                                                        7.0))))),
                                                                                                Opt.none()),
                                                                                        mb.return_(
                                                                                                mb.scopeUse(
                                                                                                        "arg")))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.field(mb.scopeUse("builder"), mb.str("func")))))),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.call(mb.scopeUse("maybe_add"), mb.num(4.0)), mb.str("to_string")),
                                        mb.tupleOf())))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok0 -> resOk = ok0;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + err);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals("11", ok.logs().get(0).message());
    }
}
