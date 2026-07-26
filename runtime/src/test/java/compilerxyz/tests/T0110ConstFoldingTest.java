package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;

import org.junit.jupiter.api.Test;
import compilerxyz.eval.ExprErr;

public final class T0110ConstFoldingTest {
  @Test
  public void test00000ConstFoldBind() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("x", mb.num(7.0)),
                mb.ifOf(
                    TList.of(
                        new TestMb.Branch(
                            mb.operatorLt(mb.scopeUse("x"), mb.num(10.0)),
                            mb.call(mb.log(), mb.str("less")))),
                    mb.call(mb.log(), mb.str("not less"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("less", ok.logs().get(0).message());
  }

  @Test
  public void test00010NoConstFoldWithAs() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("x", mb.as_(mb.num(7.0), mb.typesInt())),
                mb.ifOf(
                    TList.of(
                        new TestMb.Branch(
                            mb.operatorLt(mb.scopeUse("x"), mb.num(10.0)),
                            mb.call(mb.log(), mb.str("less")))),
                    mb.call(mb.log(), mb.str("not less"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("less", ok.logs().get(0).message());
  }

  @Test
  public void test00020ConstFoldedAddBytecode() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "x",
                    mb.as_(
                        mb.operatorAdd(mb.num(1.), mb.num(2.)), mb.typesInt())),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(mb.scopeUse("x"), mb.str("to_string")), mb.tupleOf())))));
    var ok = Helpers.compileCaptured(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("3", ok.logs().get(0).message());
    assertEquals(
        """
        // class version 55.0 (55)
        // access flags 0x31
        public final class compilerxyz/eval/gen/EvalRoot$0 {


          // access flags 0x9
          public static Lcompilerxyz/eval/ExprContext; __exprContext

          // access flags 0x9
          public static run()Ljava/lang/Object;
            LDC 3
            LSTORE 0
            GETSTATIC compilerxyz/eval/gen/EvalRoot$0.__exprContext : Lcompilerxyz/eval/ExprContext;
            LLOAD 0
            INVOKESTATIC compilerxyz/eval/value/int_/IntFormat.format (J)Ljava/lang/String;
            INVOKESTATIC compilerxyz/eval/builtin/BuiltinBodies.log (Lcompilerxyz/eval/ExprContext;Ljava/lang/String;)Ljava/lang/Object;
            POP
            NEW compilerxyz/eval/gen/Composite$0
            DUP
            INVOKESPECIAL compilerxyz/eval/gen/Composite$0.<init> ()V
            ARETURN
            MAXSTACK = 3
            MAXLOCALS = 2
        }
        """,
        Helpers.disassembled(ok, "compilerxyz.eval.gen.EvalRoot$0"));
  }
}
