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

public final class T0090MapsTest {
  @Test
  public void test00000MapStringKeys() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_map",
                    mb.call(
                        mb.field(
                            mb.call(
                                mb.typesMap(),
                                mb.tupleOf(mb.typesString(), mb.typesInt())),
                            mb.str("new")),
                        mb.tupleOf())),
                mb.set(mb.field(mb.scopeUse("my_map"), mb.str("a")), mb.num(10.)),
                mb.set(mb.field(mb.scopeUse("my_map"), mb.str("b")), mb.num(20.)),
                mb.foreach(
                    mb.scopeUse("my_map"),
                    mb.str("entry"),
                    mb.seqOf(
                        mb.call(
                            mb.log(),
                            mb.field(mb.scopeUse("entry"), mb.str("key"))),
                        mb.call(
                            mb.log(),
                            mb.call(
                                mb.field(
                                    mb.field(mb.scopeUse("entry"), mb.str("value")),
                                    mb.str("to_string")),
                                mb.tupleOf())))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(4, ok.logs().size());
    assertEquals("a", ok.logs().get(0).message());
    assertEquals("10", ok.logs().get(1).message());
    assertEquals("b", ok.logs().get(2).message());
    assertEquals("20", ok.logs().get(3).message());
  }

  @Test
  public void test00010MapStringString() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_map",
                    mb.call(
                        mb.field(
                            mb.call(
                                mb.typesMap(),
                                mb.tupleOf(mb.typesString(), mb.typesString())),
                            mb.str("new")),
                        mb.tupleOf())),
                mb.set(mb.field(mb.scopeUse("my_map"), mb.str("x")), mb.str("hello")),
                mb.set(mb.field(mb.scopeUse("my_map"), mb.str("y")), mb.str("world")),
                mb.foreach(
                    mb.scopeUse("my_map"),
                    mb.str("entry"),
                    mb.seqOf(
                        mb.call(
                            mb.log(),
                            mb.field(mb.scopeUse("entry"), mb.str("key"))),
                        mb.call(
                            mb.log(),
                            mb.field(mb.scopeUse("entry"), mb.str("value"))))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(4, ok.logs().size());
    assertEquals("x", ok.logs().get(0).message());
    assertEquals("hello", ok.logs().get(1).message());
    assertEquals("y", ok.logs().get(2).message());
    assertEquals("world", ok.logs().get(3).message());
  }
}
