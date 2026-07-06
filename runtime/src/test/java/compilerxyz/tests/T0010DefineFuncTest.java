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
                                mb.tupleOf(
                                    mb.typesNull(),
                                    mb.typesNull(),
                                    mb.stage(mb.call(mb.log(), mb.str("hello"))))),
                            mb.scopeUse("f")))),
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
                                mb.tupleOf(
                                    mb.typesString(),
                                    mb.typesString(),
                                    mb.stage(
                                        mb.call(
                                            mb.field(
                                                mb.scopeUse("arg"),
                                                mb.str("toUpperCase")),
                                            mb.tupleOf())))),
                            mb.scopeUse("f")))),
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
                                mb.tupleOf(
                                    mb.typesString(),
                                    mb.typesString(),
                                    mb.stage(
                                        mb.call(
                                            mb.field(
                                                mb.scopeUse("arg"),
                                                mb.str("toUpperCase")),
                                            mb.tupleOf())))),
                            mb.scopeUse("f")))),
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
}
