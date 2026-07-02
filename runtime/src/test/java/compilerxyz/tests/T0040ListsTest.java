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

public final class T0040ListsTest {
  @Test
  public void test00000List() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_list",
                    mb.call(
                        mb.field(
                            mb.call(mb.typesList(), mb.typesString()), mb.str("new")),
                        mb.tupleOf(mb.str("c"), mb.str("d")))),
                mb.call(
                    mb.field(mb.scopeUse("my_list"), mb.str("push")), mb.str("a")),
                mb.call(
                    mb.field(mb.scopeUse("my_list"), mb.str("push")), mb.str("b")),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(mb.scopeUse("my_list"), mb.str("join")),
                        mb.str("_"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("c_d_a_b", ok.logs().get(0).message());
  }
}
