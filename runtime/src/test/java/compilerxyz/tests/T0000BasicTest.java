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

public final class T0000BasicTest {
  @Test
  public void test00100CallLit() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(mb.call(mb.log(), mb.str("Hello world (mortar)"))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("Hello world (mortar)", ok.logs().get(0).message());
  }

  @Test
  public void test00101TypeError() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put("main", new SourceModule.V1(mb.call(mb.log(), mb.literalNull())));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>, () -> "expected compile error but got: " + res);
  }

  @Test
  public void test00200BindUse() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("hello", mb.str("Hello world (mortar)")),
                mb.call(mb.log(), mb.scopeUse("hello")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("Hello world (mortar)", ok.logs().get(0).message());
  }

  @Test
  public void test00300StageLower() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.call(mb.log(), mb.lower(mb.str("Hello world (mortar)")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("Hello world (mortar)", ok.logs().get(0).message());
  }

  @Test
  public void test00400LowerNostage() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.call(mb.log(), mb.lower(mb.str("Hello world (mortar)")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("Hello world (mortar)", ok.logs().get(0).message());
  }

  @Test
  public void test00500MethodCall() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.call(
                mb.log(),
                mb.call(
                    mb.field(mb.str("HELLO WORLD"), mb.str("toLowerCase")),
                    mb.tupleOf()))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
  }
}
