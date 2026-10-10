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

public final class T0021ObjectValueTypeTest {
    @Test
    public void test00000ValueAndTypeRejected() {
        var e = new TestMbShorthand();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        e.let("d", e.define()),
                        e.let("o", e.defObject(e.v("d"), "self typed")),
                        e.objectImpl(e.v("o"), e.tup()),
                        e.implInterface(e.v("o"), e.mb.evalValue(), e.p("type", e.v("outer"))),
                        e.implInterface(
                                e.v("o"),
                                e.mb.evalType(),
                                e.p("desc", e.s("self typed")),
                                e.p("aspect", e.optNone(e.mb.typesAspect())),
                                e.p("can_cast_from", e.b(false)),
                                e.p("cast_from", e.mb.panic(e.s("cast_to unreachable"))),
                                e.p("can_cast_to", e.mb.as(e.b(false), e.mb.typesBool())),
                                e.p("cast_to", e.mb.panic(e.s("cast_to unreachable"))),
                                e.p(
                                        "meta",
                                        e.seq(e.ret(e.call(
                                                e.mb.builtin("eval", "meta_new"), e.s("t0021objectvaluetypetest"))))),
                                e.p("is_equal", e.b(false))),
                        e.finish(e.v("d")))));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok ->
                throw new AssertionError("expected the definition to be rejected");
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                var text = err.error().toString();
                assertTrue(text.contains("implements both eval.value and eval.type"), text);
            }
        }
    }
}
