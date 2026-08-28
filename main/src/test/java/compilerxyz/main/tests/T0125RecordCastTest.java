package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0125RecordCastTest {
    @Test
    public void test00100RecordCastSourceless() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var reorderedType = mb.recordOf(TList.of(
                new TestMb.RecordPair("b", mb.as_(mb.num(0.0), mb.typesInt())),
                new TestMb.RecordPair("a", mb.as_(mb.num(0.0), mb.typesInt()))));
        var sourceRecord = mb.recordOf(TList.of(
                new TestMb.RecordPair("a", mb.as_(mb.num(1.0), mb.typesInt())),
                new TestMb.RecordPair("b", mb.as_(mb.num(2.0), mb.typesInt()))));
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("r", mb.as_(sourceRecord, reorderedType)),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.field(mb.scopeUse("r"), mb.str("a")), mb.str("to_string")),
                                        mb.tupleOf())),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.field(mb.scopeUse("r"), mb.str("b")), mb.str("to_string")),
                                        mb.tupleOf())))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        @SuppressWarnings("unchecked")
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(2, ok.logs().size());
        assertEquals("1", ok.logs().get(0).message());
        assertEquals("2", ok.logs().get(1).message());
    }

    @Test
    public void test00200RecordCastDynamic() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var reorderedType = mb.recordOf(TList.of(
                new TestMb.RecordPair("b", mb.as_(mb.num(0.0), mb.typesInt())),
                new TestMb.RecordPair("a", mb.as_(mb.num(0.0), mb.typesInt()))));
        var sourceRecord = mb.recordOf(TList.of(
                new TestMb.RecordPair("a", mb.as_(mb.num(1.0), mb.typesInt())),
                new TestMb.RecordPair("b", mb.as_(mb.num(2.0), mb.typesInt()))));
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("src", sourceRecord),
                        mb.bind("r", mb.as_(mb.scopeUse("src"), reorderedType)),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.field(mb.scopeUse("r"), mb.str("a")), mb.str("to_string")),
                                        mb.tupleOf())),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.field(mb.scopeUse("r"), mb.str("b")), mb.str("to_string")),
                                        mb.tupleOf())))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        @SuppressWarnings("unchecked")
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(2, ok.logs().size());
        assertEquals("1", ok.logs().get(0).message());
        assertEquals("2", ok.logs().get(1).message());
    }
}
