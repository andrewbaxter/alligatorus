package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.eval.ExprErr;
import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
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
                                mb.seqOf(mb.call(
                                        mb.log(),
                                        mb.call(mb.field(mb.scopeUse("e"), mb.str("to_string")), mb.tupleOf())))))));
        var ok = Helpers.compileCaptured(modules);
        assertEquals(3, ok.logs().size());
        assertEquals(
                """
        // class version 55.0 (55)
        // access flags 0x31
        public final class compilerxyz/eval/gen/EvalRoot$0 {


          // access flags 0x9
          public static Lcompilerxyz/eval/ExprContext; __exprContext

          // access flags 0x9
          public static run()Ljava/lang/Object;
            NEW compilerxyz/gen/ListStoreInt
            DUP
            INVOKESPECIAL compilerxyz/gen/ListStoreInt.<init> ()V
            DUP
            LDC 1
            INVOKEVIRTUAL compilerxyz/gen/ListStoreInt.push (I)V
            DUP
            LDC 2
            INVOKEVIRTUAL compilerxyz/gen/ListStoreInt.push (I)V
            DUP
            LDC 7
            INVOKEVIRTUAL compilerxyz/gen/ListStoreInt.push (I)V
            ASTORE 0
            ALOAD 0
            LDC 0
            ISTORE 1
           L0
            DUP
            INVOKEVIRTUAL compilerxyz/gen/ListStoreInt.size ()I
            ILOAD 1
            SWAP
            IF_ICMPGE L1
            DUP
            ILOAD 1
            INVOKEVIRTUAL compilerxyz/gen/ListStoreInt.get (I)I
            ISTORE 2
            GETSTATIC compilerxyz/eval/gen/EvalRoot$0.__exprContext : Lcompilerxyz/eval/ExprContext;
            ILOAD 2
            INVOKESTATIC compilerxyz/eval/value/numeric/NumericFormat.formatInt (I)Ljava/lang/String;
            INVOKESTATIC compilerxyz/eval/builtin/BuiltinBodies.log (Lcompilerxyz/eval/ExprContext;Ljava/lang/String;)Ljava/lang/Object;
            POP
            ILOAD 1
            LDC 1
            IADD
            ISTORE 1
            GOTO L0
           L1
            POP
            NEW compilerxyz/eval/gen/Composite$0
            DUP
            INVOKESPECIAL compilerxyz/eval/gen/Composite$0.<init> ()V
            ARETURN
            MAXSTACK = 3
            MAXLOCALS = 3
        }
        """,
                Helpers.disassembled(ok, "compilerxyz.eval.gen.EvalRoot$0"));
    }
}
