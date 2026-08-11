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

public final class T0010DefineFuncTest {
  @Test
  public void test00650DefineVoidFunc() {
    // A function declared to return NO value (ret: none). Its body just performs
    // a side effect and yields nothing; calling it produces no stack value.
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "act",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")),
                                    mb.str("act"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair("arg", mb.typesNull()),
                                        new TestMb.RecordPair("ret", mb.optNoneType()),
                                        new TestMb.RecordPair(
                                            "impl_body",
                                            mb.stage(mb.call(mb.log(), mb.str("acted"))))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.return_(mb.scopeUse("f"))))),
                mb.call(mb.scopeUse("act"), mb.tupleOf()))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("acted", ok.logs().get(0).message());
  }

  @Test
  public void test00600DefineFunc() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "hello",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")),
                                    mb.str("f"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair("arg", mb.typesNull()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesNull())),
                                        new TestMb.RecordPair(
                                            "impl_body",
                                            mb.stage(mb.call(mb.log(), mb.str("hello"))))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.return_(mb.scopeUse("f"))))),
                mb.call(mb.scopeUse("hello"), mb.tupleOf()))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("hello", ok.logs().get(0).message());
  }

  @Test
  public void test00700DefineFuncArgRet() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_func",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")),
                                    mb.str("f"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair("arg", mb.typesString()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
                                        new TestMb.RecordPair(
                                            "impl_body",
                                            mb.stage(
                                                mb.call(
                                                    mb.field(
                                                        mb.scopeUse("arg"),
                                                        mb.str("toUpperCase")),
                                                    mb.tupleOf())))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.return_(mb.scopeUse("f"))))),
                mb.call(
                    mb.log(),
                    mb.call(mb.scopeUse("my_func"), mb.str("hello world"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO WORLD", ok.logs().get(0).message());
  }

  @Test
  public void test00710DefineFuncUnifiedImpl() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_func",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")),
                                    mb.str("f"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair("arg", mb.typesString()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
                                        new TestMb.RecordPair(
                                            "impl_body",
                                            mb.stage(
                                                mb.call(
                                                    mb.field(
                                                        mb.scopeUse("arg"),
                                                        mb.str("toUpperCase")),
                                                    mb.tupleOf())))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.return_(mb.scopeUse("f"))))),
                mb.call(
                    mb.log(),
                    mb.call(mb.scopeUse("my_func"), mb.str("hello world"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO WORLD", ok.logs().get(0).message());
  }

  @Test
  public void test00720DefineFuncCallBytecode() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "inc",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")),
                                    mb.str("inc"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair("arg", mb.typesInt()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesInt())),
                                        new TestMb.RecordPair(
                                            "impl_body",
                                            mb.stage(
                                                mb.operatorAdd(
                                                    mb.scopeUse("arg"),
                                                    mb.as_(
                                                        mb.num(1.), mb.typesInt()))))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.return_(mb.scopeUse("f"))))),
                mb.bind(
                    "y",
                    mb.call(
                        mb.scopeUse("inc"), mb.as_(mb.num(2.), mb.typesInt()))),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(mb.scopeUse("y"), mb.str("to_string")), mb.tupleOf())))));
    var ok = Helpers.compileCaptured(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("3", ok.logs().get(0).message());
    assertEquals(
        """
        // class version 55.0 (55)
        // access flags 0x31
        public final class compilerxyz/eval/gen/Functions$0_0_0 {


          // access flags 0x9
          public static Lcompilerxyz/eval/ExprContext; __exprContext

          // access flags 0x9
          public static _0(I)I
            ILOAD 0
            LDC 1
            IADD
            IRETURN
            MAXSTACK = 2
            MAXLOCALS = 1
        }
        """,
        Helpers.disassembled(ok, "compilerxyz.eval.gen.Functions$0_0_0"));
    assertEquals(
        """
        // class version 55.0 (55)
        // access flags 0x31
        public final class compilerxyz/eval/gen/EvalRoot$1 {


          // access flags 0x9
          public static Lcompilerxyz/eval/ExprContext; __exprContext

          // access flags 0x9
          public static run()Ljava/lang/Object;
            LDC 2
            INVOKESTATIC compilerxyz/eval/gen/Functions$0_0_0._0 (I)I
            ISTORE 0
            GETSTATIC compilerxyz/eval/gen/EvalRoot$1.__exprContext : Lcompilerxyz/eval/ExprContext;
            ILOAD 0
            INVOKESTATIC compilerxyz/eval/value/numeric/NumericFormat.formatInt (I)Ljava/lang/String;
            INVOKESTATIC compilerxyz/eval/builtin/BuiltinBodies.log (Lcompilerxyz/eval/ExprContext;Ljava/lang/String;)Ljava/lang/Object;
            POP
            NEW compilerxyz/eval/gen/Composite$0
            DUP
            INVOKESPECIAL compilerxyz/eval/gen/Composite$0.<init> ()V
            ARETURN
            MAXSTACK = 2
            MAXLOCALS = 1
        }
        """,
        Helpers.disassembled(ok, "compilerxyz.eval.gen.EvalRoot$1"));
  }
}
