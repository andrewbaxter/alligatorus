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

public final class T0110ConstFoldingTest {
    @Test
    public void test00000ConstFoldBind() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("x", mb.num(7.0)),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        mb.operatorLt(mb.scopeUse("x"), mb.num(10.0)),
                                        mb.call(mb.log(), mb.str("less")))),
                                Opt.some(mb.call(mb.log(), mb.str("not less")))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> resOk = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals("less", ok.logs().get(0).message());
    }

    @Test
    public void test00010NoConstFoldWithAs() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("x", mb.as(mb.num(7.0), mb.typesInt())),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        mb.operatorLt(mb.scopeUse("x"), mb.num(10.0)),
                                        mb.seqOf(mb.call(mb.log(), mb.str("less")), mb.tupleOf()))),
                                Opt.some(mb.seqOf(mb.call(mb.log(), mb.str("not less")), mb.tupleOf()))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> resOk = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals("less", ok.logs().get(0).message());
    }

    @Test
    public void test00020ConstFoldedAddBytecode() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("x", mb.as(mb.operatorAdd(mb.num(1.), mb.num(2.)), mb.typesInt())),
                        mb.seqOf(
                                mb.call(
                                        mb.log(),
                                        mb.call(mb.field(mb.scopeUse("x"), mb.str("to_string")), mb.tupleOf())),
                                mb.tupleOf()))));
        var ok = Helpers.compileCaptured(modules);
        assertEquals(1, ok.logs().size());
        assertEquals("3", ok.logs().get(0).message());
        assertEquals(
                """
        // class version 55.0 (55)
        // access flags 0x31
        public final class compilerxyz/main/eval/gen/EvalRoot$0 {


          // access flags 0x9
          public static Lcompilerxyz/main/eval/ExprContext; __exprContext

          // access flags 0x9
          public static run()Ljava/lang/Object;
            LDC 3
            ISTORE 0
            GETSTATIC compilerxyz/main/eval/gen/EvalRoot$0.__exprContext : Lcompilerxyz/main/eval/ExprContext;
            GETFIELD compilerxyz/main/eval/ExprContext.vx : Lcompilerxyz/main/eval/EvalContext;
            ILOAD 0
            INVOKESTATIC compilerxyz/main/eval/value/numeric/NumericFormat.formatInt (I)Ljava/lang/String;
            INVOKESTATIC compilerxyz/main/eval/builtin/BuiltinBodies.log (Lcompilerxyz/main/eval/EvalContext;Ljava/lang/String;)V
            ACONST_NULL
            ARETURN
            MAXSTACK = 2
            MAXLOCALS = 1
        }
        """,
                Helpers.disassembled(ok, "compilerxyz.main.eval.gen.EvalRoot$0"));
    }
}
