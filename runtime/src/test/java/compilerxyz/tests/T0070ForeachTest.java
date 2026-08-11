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

public final class T0070ForeachTest {
  @Test
  public void test00000ForeachList() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_list",
                    mb.call(
                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(7.)))),
                mb.foreach(
                    mb.scopeUse("my_list"),
                    mb.str("e"),
                    mb.seqOf(
                        mb.call(
                            mb.log(),
                            mb.call(
                                mb.field(mb.scopeUse("e"), mb.str("to_string")),
                                mb.tupleOf())))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
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
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_list",
                    mb.call(
                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(7.)))),
                mb.foreach(
                    mb.scopeUse("my_list"),
                    mb.str("e"),
                    mb.seqOf(
                        mb.call(
                            mb.log(),
                            mb.call(
                                mb.field(mb.scopeUse("e"), mb.str("to_string")),
                                mb.tupleOf())))))));
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
            NEW compilerxyz/util/TList
            DUP
            INVOKESPECIAL compilerxyz/util/TList.<init> ()V
            DUP
            LDC 1
            INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
            INVOKEVIRTUAL compilerxyz/util/TList.add (Ljava/lang/Object;)Z
            POP
            DUP
            LDC 2
            INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
            INVOKEVIRTUAL compilerxyz/util/TList.add (Ljava/lang/Object;)Z
            POP
            DUP
            LDC 7
            INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
            INVOKEVIRTUAL compilerxyz/util/TList.add (Ljava/lang/Object;)Z
            POP
            ASTORE 0
            ALOAD 0
            INVOKEINTERFACE java/lang/Iterable.iterator ()Ljava/util/Iterator; (itf)
           L0
            DUP
            INVOKEINTERFACE java/util/Iterator.hasNext ()Z (itf)
            IFEQ L1
            DUP
            INVOKEINTERFACE java/util/Iterator.next ()Ljava/lang/Object; (itf)
            CHECKCAST java/lang/Integer
            ASTORE 1
            GETSTATIC compilerxyz/eval/gen/EvalRoot$0.__exprContext : Lcompilerxyz/eval/ExprContext;
            ALOAD 1
            INVOKEVIRTUAL java/lang/Integer.intValue ()I
            INVOKESTATIC compilerxyz/eval/value/numeric/NumericFormat.formatInt (I)Ljava/lang/String;
            INVOKESTATIC compilerxyz/eval/builtin/BuiltinBodies.log (Lcompilerxyz/eval/ExprContext;Ljava/lang/String;)Ljava/lang/Object;
            POP
            GOTO L0
           L1
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
