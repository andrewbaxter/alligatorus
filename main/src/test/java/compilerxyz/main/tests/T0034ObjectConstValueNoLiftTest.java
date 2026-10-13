package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0034ObjectConstValueNoLiftTest {
    @Test
    public void test00000ObjectConstValueFromLoweredBlockIsAnError() {
        var e = new TestMbShorthand();
        var voc =
                e.call(e.evalBuiltin("value_object_const"), e.rec(e.p("type", e.mb.typesInt()), e.p("data", e.tup())));
        var main = TList.of(
                e.let("lib", e.lower(e.seq(e.ret(e.rec(e.p("v", voc)))))),
                e.let("got", e.fld(e.v("lib"), "v")),
                e.call(e.mb.log(), e.s("unreachable")));
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(main));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        TList<ExprErr> errs;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok ->
                throw new AssertionError("compile succeeded, expected a lift error: "
                        + ok.value().logs());
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> errs = err.error();
        }
        var text = errs.toString();
        assertTrue(text.contains("does not implement AspectLift"), () -> "unexpected errors: " + text);
    }
}
