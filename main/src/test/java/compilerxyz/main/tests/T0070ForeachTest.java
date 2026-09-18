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

public final class T0070ForeachTest {
    @Test
    public void test00000ForeachList() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(7.)))),
                        mb.foreach(
                                mb.scopeUse("my_list"),
                                mb.str("e"),
                                mb.seqOf(mb.call(
                                        mb.log(),
                                        mb.call(mb.field(mb.scopeUse("e"), mb.str("to_string")), mb.tupleOf())))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> resOk = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(3, ok.logs().size());
        assertEquals("1", ok.logs().get(0).message());
        assertEquals("2", ok.logs().get(1).message());
        assertEquals("7", ok.logs().get(2).message());
    }

    @Test
    public void test00010ForeachBytecode() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(7.)))),
                        mb.foreach(
                                mb.scopeUse("my_list"),
                                mb.str("e"),
                                mb.seqOf(mb.seqOf(
                                        mb.call(
                                                mb.log(),
                                                mb.call(mb.field(mb.scopeUse("e"), mb.str("to_string")), mb.tupleOf())),
                                        mb.tupleOf()))))));
        var ok = Helpers.compileCaptured(modules);
        assertEquals(3, ok.logs().size());
        assertEquals(
                """
        // class version 55.0 (55)
        // access flags 0x31
        public final class compilerxyz/main/eval/gen/EvalRoot$0 {


          // access flags 0x9
          public static Lcompilerxyz/main/eval/ExprContext; __exprContext

          // access flags 0x9
          public static run()Ljava/lang/Object;
            NEW compilerxyz/main/gen/ListStoreInt
            DUP
            INVOKESPECIAL compilerxyz/main/gen/ListStoreInt.<init> ()V
            DUP
            LDC 1
            INVOKEVIRTUAL compilerxyz/main/gen/ListStoreInt.push (I)V
            DUP
            LDC 2
            INVOKEVIRTUAL compilerxyz/main/gen/ListStoreInt.push (I)V
            DUP
            LDC 7
            INVOKEVIRTUAL compilerxyz/main/gen/ListStoreInt.push (I)V
            ASTORE 0
            ALOAD 0
            ASTORE 1
            LDC 0
            ISTORE 2
           L0
            ILOAD 2
            ALOAD 1
            INVOKEVIRTUAL compilerxyz/main/gen/ListStoreInt.size ()I
            IF_ICMPGE L1
            ALOAD 1
            ILOAD 2
            INVOKEVIRTUAL compilerxyz/main/gen/ListStoreInt.get (I)I
            ISTORE 3
            GETSTATIC compilerxyz/main/eval/gen/EvalRoot$0.__exprContext : Lcompilerxyz/main/eval/ExprContext;
            GETFIELD compilerxyz/main/eval/ExprContext.vx : Lcompilerxyz/main/eval/EvalContext;
            ILOAD 3
            INVOKESTATIC compilerxyz/main/eval/value/numeric/NumericFormat.formatInt (I)Ljava/lang/String;
            INVOKESTATIC compilerxyz/main/eval/builtin/BuiltinBodies.log (Lcompilerxyz/main/eval/EvalContext;Ljava/lang/String;)V
            ILOAD 2
            LDC 1
            IADD
            ISTORE 2
            GOTO L0
           L1
            ACONST_NULL
            ARETURN
            MAXSTACK = 3
            MAXLOCALS = 4
        }
        """,
                Helpers.disassembled(ok, "compilerxyz.main.eval.gen.EvalRoot$0"));
    }
}
