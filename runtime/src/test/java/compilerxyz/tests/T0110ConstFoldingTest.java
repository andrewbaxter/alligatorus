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
}
