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

public final class T0100ImportTest {
  @Test
  public void test00100ImportLiteral() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "other",
        new SourceModule.V1(mb.str("Hello world (mortar)")));
    modules.put(
        "main",
        new SourceModule.V1(
            mb.call(mb.log(), mb.importExpr(mb.str("other")))));
    var gxa = Helpers.newGxWithArtifacts(modules);
    var res = Compile.compile(gxa.gx(), new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("Hello world (mortar)", ok.logs().get(0).message());
    assertEquals(0, gxa.artifacts().data.size());
  }

  @Test
  public void test00200ImportDefinitionset() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "other",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "ctor",
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
                                            mb.tupleOf())))),
                            mb.field(mb.scopeUse("ndr"), mb.str("new"))))),
                mb.call(mb.scopeUse("ctor"), mb.str("hello")))));
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("val", mb.importExpr(mb.str("other"))),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(mb.scopeUse("val"), mb.str("to_string")),
                        mb.tupleOf())))));
    var gxa = Helpers.newGxWithArtifacts(modules);
    var res = Compile.compile(gxa.gx(), new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO", ok.logs().get(0).message());
    assertEquals(2, gxa.artifacts().data.size());
  }

  @Test
  public void test00300ImportTransitiveDefinitionset() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "module_b",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "nfr",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("func")),
                        mb.str("func_b"))),
                mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                mb.call(
                    mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                    mb.tupleOf(
                        mb.typesNull(),
                        mb.typesString(),
                        mb.stage(mb.str("hello")))),
                mb.call(
                    mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.scopeUse("f"))));
    modules.put(
        "module_a",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("func_b", mb.importExpr(mb.str("module_b"))),
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "nfr",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("func")),
                        mb.str("func_a"))),
                mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                mb.call(
                    mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                    mb.tupleOf(
                        mb.typesNull(),
                        mb.typesString(),
                        mb.stage(
                            mb.call(
                                mb.lower(mb.scopeUse("func_b")),
                                mb.literalNull())))),
                mb.call(
                    mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.scopeUse("f"))));
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("func_a", mb.importExpr(mb.str("module_a"))),
                mb.call(
                    mb.log(),
                    mb.call(mb.scopeUse("func_a"), mb.literalNull())))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("hello", ok.logs().get(0).message());
  }

  @Test
  public void test00400ImportCycle() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put("module_a", new SourceModule.V1(mb.importExpr(mb.str("module_b"))));
    modules.put("module_b", new SourceModule.V1(mb.importExpr(mb.str("module_a"))));
    modules.put("main", new SourceModule.V1(mb.importExpr(mb.str("module_a"))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(
        res instanceof Result.Err<?, ?>, () -> "expected import cycle error, got: " + res);
  }
}
