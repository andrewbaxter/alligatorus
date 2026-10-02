package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0045ArraysTest {
    @Test
    public void test00000BuiltinByteArray() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("bytes", mb.call(mb.evalStrUtf8(), mb.str("hé"))),
                        mb.bind("typed", mb.as(mb.scopeUse("bytes"), mb.call(mb.typesArray(), mb.typesByte()))),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.field(mb.scopeUse("typed"), mb.str("len")), mb.str("to_string")),
                                        mb.tupleOf())),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(
                                                mb.call(mb.field(mb.scopeUse("typed"), mb.str("get")), mb.num(1)),
                                                mb.str("to_string")),
                                        mb.tupleOf())),
                        mb.foreach(
                                mb.scopeUse("typed"),
                                mb.str("e"),
                                mb.seqOf(mb.call(
                                        mb.log(),
                                        mb.call(mb.field(mb.scopeUse("e"), mb.str("to_string")), mb.tupleOf())))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> resOk = ok;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                throw new AssertionError("compile failed: " + res);
        }
        var logs = resOk.value().logs();
        assertEquals(5, logs.size());
        assertEquals("3", logs.get(0).message());
        assertEquals("-61", logs.get(1).message());
        assertEquals("104", logs.get(2).message());
        assertEquals("-61", logs.get(3).message());
        assertEquals("-87", logs.get(4).message());
    }
}
