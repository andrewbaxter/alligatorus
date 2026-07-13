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

public final class T0080UnionTest {
  @Test
  public void test00000UnionMatch() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "strnum",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "builder",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("union")),
                                    mb.tupleOf())),
                            mb.bind(
                                "new_str",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("strtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesString()))))),
                            mb.bind(
                                "new_num",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("numtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesNumber()))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.recordOf(
                                TList.of(
                                    new TestMb.RecordPair(
                                        "type",
                                        mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                    new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                    new TestMb.RecordPair("num", mb.scopeUse("new_num"))))))),
                mb.bind(
                    "x",
                    mb.call(
                        mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.))),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(
                            mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                        new TestMb.Branch(
                            mb.str("numtag"),
                            mb.call(
                                mb.log(),
                                mb.call(
                                    mb.field(mb.scopeUse("y"), mb.str("to_string")),
                                    mb.tupleOf())))),
                    null))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("7", ok.logs().get(0).message());
  }

  @Test
  public void test00010UnionMatchFallback() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "strnum",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "builder",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("union")),
                                    mb.tupleOf())),
                            mb.bind(
                                "new_str",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("strtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesString()))))),
                            mb.bind(
                                "new_num",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("numtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesNumber()))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.recordOf(
                                TList.of(
                                    new TestMb.RecordPair(
                                        "type",
                                        mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                    new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                    new TestMb.RecordPair("num", mb.scopeUse("new_num"))))))),
                mb.bind(
                    "x",
                    mb.call(
                        mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.))),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(
                            mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y")))),
                    mb.call(mb.log(), mb.str("fallback"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("fallback", ok.logs().get(0).message());
  }

  @Test
  public void test00020UnionNonexhaustive() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "strnum",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "builder",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("union")),
                                    mb.tupleOf())),
                            mb.bind(
                                "new_str",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("strtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesString()))))),
                            mb.bind(
                                "new_num",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("numtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesNumber()))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.recordOf(
                                TList.of(
                                    new TestMb.RecordPair(
                                        "type",
                                        mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                    new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                    new TestMb.RecordPair("num", mb.scopeUse("new_num"))))))),
                mb.bind(
                    "x",
                    mb.call(
                        mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.))),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(
                            mb.str("numtag"),
                            mb.call(
                                mb.log(),
                                mb.call(
                                    mb.field(mb.scopeUse("y"), mb.str("to_string")),
                                    mb.tupleOf())))),
                    null))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>, () -> "expected compile error but got: " + res);
  }

  @Test
  public void test00030UnionExhaustiveWithFallback() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "strnum",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "builder",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("union")),
                                    mb.tupleOf())),
                            mb.bind(
                                "new_str",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("strtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesString()))))),
                            mb.bind(
                                "new_num",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("numtag")),
                                            new TestMb.RecordPair(
                                                "type_ref", mb.typesNumber()))))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.recordOf(
                                TList.of(
                                    new TestMb.RecordPair(
                                        "type",
                                        mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                    new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                    new TestMb.RecordPair("num", mb.scopeUse("new_num"))))))),
                mb.bind(
                    "x",
                    mb.call(
                        mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.))),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(
                            mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                        new TestMb.Branch(
                            mb.str("numtag"),
                            mb.call(
                                mb.log(),
                                mb.call(
                                    mb.field(mb.scopeUse("y"), mb.str("to_string")),
                                    mb.tupleOf())))),
                    mb.call(mb.log(), mb.str("fallback"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>, () -> "expected compile error but got: " + res);
  }
}
