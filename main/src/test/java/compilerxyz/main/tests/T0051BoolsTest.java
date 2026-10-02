package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0051BoolsTest {
    private static TList<String> compileLogs(TList<SourceExpr> main) {
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(main));
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

    private static SourceExpr logIf(TestMb mb, SourceExpr condition, String yes, String no) {
        return mb.ifOf(
                TList.of(new TestMb.Branch(condition, mb.call(mb.log(), mb.str(yes)))),
                Opt.some(mb.call(mb.log(), mb.str(no))));
    }

    @Test
    public void test00000NotConst() {
        var mb = new TestMb();
        assertEquals(
                TList.of("not-false", "not-true"),
                compileLogs(TList.of(
                        logIf(mb, mb.operatorBitNot(mb.bool(false)), "not-false", "wrong"),
                        logIf(mb, mb.operatorBitNot(mb.bool(true)), "wrong", "not-true"))));
    }

    @Test
    public void test00100NotDynamic() {
        var mb = new TestMb();
        assertEquals(
                TList.of("not-false", "not-true"),
                compileLogs(TList.of(
                        mb.bind("f", mb.as(mb.bool(false), mb.typesBool())),
                        mb.bind("t", mb.as(mb.bool(true), mb.typesBool())),
                        logIf(mb, mb.operatorBitNot(mb.scopeUse("f")), "not-false", "wrong"),
                        logIf(mb, mb.operatorBitNot(mb.scopeUse("t")), "wrong", "not-true"))));
    }
}
