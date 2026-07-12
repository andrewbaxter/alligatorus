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

public final class T0050NumbersTest {
  @Test
  public void test00000NumberLit() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("x", mb.as_(mb.num(4.0), mb.typesNumber())),
                mb.ifOf(
                    TList.of(
                        new TestMb.Branch(
                            mb.operatorLt(mb.scopeUse("x"), mb.num(5.0)),
                            mb.set(
                                mb.scopeUse("x"),
                                mb.operatorAdd(mb.scopeUse("x"), mb.num(7.0))))),
                    null),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(mb.scopeUse("x"), mb.str("to_string")),
                        mb.tupleOf())))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("11", ok.logs().get(0).message());
  }

  @Test
  public void test00100NumberDynamic() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "maybe_add",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "builder",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")),
                                    mb.str("maybe add 7"))),
                            mb.call(
                                mb.field(mb.scopeUse("builder"), mb.str("impl")),
                                mb.tupleOf(
                                    mb.typesNumber(),
                                    mb.typesNumber(),
                                    mb.stage(
                                        mb.seqOf(
                                            mb.ifOf(
                                                TList.of(
                                                    new TestMb.Branch(
                                                        mb.operatorLt(
                                                            mb.scopeUse("arg"), mb.num(5.0)),
                                                        mb.set(
                                                            mb.scopeUse("arg"),
                                                            mb.operatorAdd(
                                                                mb.scopeUse("arg"),
                                                                mb.num(7.0))))),
                                                null),
                                            mb.scopeUse("arg"))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.field(mb.scopeUse("builder"), mb.str("func"))))),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(
                            mb.call(mb.scopeUse("maybe_add"), mb.num(4.0)),
                            mb.str("to_string")),
                        mb.tupleOf())))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("11", ok.logs().get(0).message());
  }
}
