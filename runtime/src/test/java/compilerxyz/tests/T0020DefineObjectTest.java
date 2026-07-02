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

public final class T0020DefineObjectTest {
  @Test
  public void test00800DefineData() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "make_upper",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "ndr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("object")),
                                    mb.str("upper"))),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("set_inner")),
                                mb.typesString()),
                            mb.bind(
                                "mnfr",
                                mb.call(
                                    mb.field(mb.scopeUse("ndr"), mb.str("func")),
                                    mb.str("to_string"))),
                            mb.call(
                                mb.field(mb.scopeUse("mnfr"), mb.str("impl")),
                                mb.tupleOf(
                                    mb.typesNull(),
                                    mb.typesString(),
                                    mb.stage(
                                        mb.call(
                                            mb.field(
                                                mb.scopeUse("inner"),
                                                mb.str("toUpperCase")),
                                            mb.literalNull())))),
                            mb.field(mb.scopeUse("ndr"), mb.str("new"))))),
                mb.bind("val", mb.call(mb.scopeUse("make_upper"), mb.str("hello"))),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(mb.scopeUse("val"), mb.str("to_string")),
                        mb.literalNull())))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO", ok.logs().get(0).message());
  }
}
