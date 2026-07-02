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
                        mb.field(mb.call(mb.typesList(), mb.typesNumber()), mb.str("new")),
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
}
