package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.eval.ExprErr;
import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0085UnionVariantSetTest {
  private static SourceExpr bindStrNum(TestMb mb) {
    return mb.bind(
        "strnum",
        mb.lower(
            mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "builder",
                    mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                mb.bind(
                    "new_str",
                    mb.field(mb.call(
                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                        mb.recordOf(
                            TList.of(
                                new TestMb.RecordPair("tag", mb.str("strtag")),
                                new TestMb.RecordPair("type_ref", mb.typesString())))), mb.str("value"))),
                mb.bind(
                    "new_num",
                    mb.field(mb.call(
                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                        mb.recordOf(
                            TList.of(
                                new TestMb.RecordPair("tag", mb.str("numtag")),
                                new TestMb.RecordPair("type_ref", mb.typesInt())))), mb.str("value"))),
                mb.bind(
                    "new_other",
                    mb.field(mb.call(
                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                        mb.recordOf(
                            TList.of(
                                new TestMb.RecordPair("tag", mb.str("othertag")),
                                new TestMb.RecordPair("type_ref", mb.typesInt())))), mb.str("value"))),
                mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair(
                                "type", mb.field(mb.scopeUse("builder"), mb.str("type"))),
                            new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                            new TestMb.RecordPair("num", mb.scopeUse("new_num")),
                            new TestMb.RecordPair("other", mb.scopeUse("new_other"))))))));
  }

  private static SourceExpr newNum(TestMb mb, double v) {
    return mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(v));
  }

  private static SourceExpr newStr(TestMb mb, String s) {
    return mb.call(mb.field(mb.scopeUse("strnum"), mb.str("str")), mb.str(s));
  }

  private static SourceExpr widened(TestMb mb, SourceExpr value) {
    return mb.as_(value, mb.field(mb.scopeUse("strnum"), mb.str("type")));
  }

  private static SourceExpr logToString(TestMb mb, String key) {
    return mb.call(
        mb.log(),
        mb.call(mb.field(mb.scopeUse(key), mb.str("to_string")), mb.tupleOf()));
  }

  private static Compile.CompileResult compileOk(TMap<String, SourceModule> modules) {
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res.resultIsOk(), () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    return ok;
  }

  private static void compileErrContaining(TMap<String, SourceModule> modules, String message) {
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res.resultIsErr(), () -> "expected compile error but got: " + res);
    assertTrue(
        res.toString().contains(message),
        () -> "expected error containing [" + message + "] but got: " + res);
  }

  @Test
  public void test00000NewValueSingleVariantExhaustive() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                bindStrNum(mb),
                mb.bind("x", newNum(mb, 7.)),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(new TestMb.Branch(mb.str("numtag"), logToString(mb, "y"))),
                    Opt.none()))));
    var ok = compileOk(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("7", ok.logs().get(0).message());
  }

  @Test
  public void test00010NewValueInactiveBranchRejected() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                bindStrNum(mb),
                mb.bind("x", newNum(mb, 7.)),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                        new TestMb.Branch(mb.str("numtag"), logToString(mb, "y"))),
                    Opt.none()))));
    compileErrContaining(modules, "not an active variant");
  }

  @Test
  public void test00020FallbackRemainingVariantsExhaustive() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                bindStrNum(mb),
                mb.bind("x", widened(mb, newNum(mb, 7.))),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y")))),
                    Opt.some(
                        mb.matchOf(
                            mb.scopeUse("y"),
                            mb.str("z"),
                            TList.of(
                                new TestMb.Branch(mb.str("numtag"), logToString(mb, "z")),
                                new TestMb.Branch(mb.str("othertag"), logToString(mb, "z"))),
                            Opt.none()))))));
    var ok = compileOk(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("7", ok.logs().get(0).message());
  }

  @Test
  public void test00030FallbackMatchedVariantRejected() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                bindStrNum(mb),
                mb.bind("x", widened(mb, newNum(mb, 7.))),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y")))),
                    Opt.some(
                        mb.matchOf(
                            mb.scopeUse("y"),
                            mb.str("z"),
                            TList.of(
                                new TestMb.Branch(
                                    mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("z"))),
                                new TestMb.Branch(mb.str("numtag"), logToString(mb, "z")),
                                new TestMb.Branch(mb.str("othertag"), logToString(mb, "z"))),
                            Opt.none()))))));
    compileErrContaining(modules, "not an active variant");
  }

  @Test
  public void test00040SetReplacesVariants() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                bindStrNum(mb),
                mb.bind("x", newNum(mb, 7.)),
                mb.set(mb.scopeUse("x"), newStr(mb, "s")),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y")))),
                    Opt.none()))));
    var ok = compileOk(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("s", ok.logs().get(0).message());
  }

  @Test
  public void test00050SetOldVariantRejected() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                bindStrNum(mb),
                mb.bind("x", newNum(mb, 7.)),
                mb.set(mb.scopeUse("x"), newStr(mb, "s")),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                        new TestMb.Branch(mb.str("numtag"), logToString(mb, "y"))),
                    Opt.none()))));
    compileErrContaining(modules, "not an active variant");
  }

  private static TList<SourceExpr> mergeSetup(TestMb mb) {
    return TList.of(
        bindStrNum(mb),
        mb.bind("x", newNum(mb, 7.)),
        mb.ifOf(
            TList.of(
                new TestMb.Branch(
                    mb.operatorEq(
                        mb.as_(mb.num(1.0), mb.typesLong()),
                        mb.as_(mb.num(1.0), mb.typesLong())),
                    mb.set(mb.scopeUse("x"), newStr(mb, "s")))),
            Opt.none()));
  }

  @Test
  public void test00060MergedBindingHasUnionOfSets() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    var exprs = new TList<SourceExpr>();
    exprs.addAll(mergeSetup(mb));
    exprs.add(
        mb.matchOf(
            mb.scopeUse("x"),
            mb.str("y"),
            TList.of(
                new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                new TestMb.Branch(mb.str("numtag"), logToString(mb, "y"))),
            Opt.none()));
    modules.put("main", new SourceModule.V1(mb.seqOf(exprs.toArray(new SourceExpr[0]))));
    var ok = compileOk(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("s", ok.logs().get(0).message());
  }

  @Test
  public void test00070MergedBindingRequiresAllActive() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    var exprs = new TList<SourceExpr>();
    exprs.addAll(mergeSetup(mb));
    exprs.add(
        mb.matchOf(
            mb.scopeUse("x"),
            mb.str("y"),
            TList.of(new TestMb.Branch(mb.str("numtag"), logToString(mb, "y"))),
            Opt.none()));
    modules.put("main", new SourceModule.V1(mb.seqOf(exprs.toArray(new SourceExpr[0]))));
    compileErrContaining(modules, "Non-exhaustive match");
  }

  @Test
  public void test00080MergedBindingExcludesUnrelatedVariants() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    var exprs = new TList<SourceExpr>();
    exprs.addAll(mergeSetup(mb));
    exprs.add(
        mb.matchOf(
            mb.scopeUse("x"),
            mb.str("y"),
            TList.of(
                new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                new TestMb.Branch(mb.str("numtag"), logToString(mb, "y")),
                new TestMb.Branch(mb.str("othertag"), logToString(mb, "y"))),
            Opt.none()));
    modules.put("main", new SourceModule.V1(mb.seqOf(exprs.toArray(new SourceExpr[0]))));
    compileErrContaining(modules, "not an active variant");
  }

  @Test
  public void test00090WidenedValueAllowsAllBranches() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                bindStrNum(mb),
                mb.bind("x", widened(mb, newNum(mb, 7.))),
                mb.matchOf(
                    mb.scopeUse("x"),
                    mb.str("y"),
                    TList.of(
                        new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                        new TestMb.Branch(mb.str("numtag"), logToString(mb, "y")),
                        new TestMb.Branch(mb.str("othertag"), logToString(mb, "y"))),
                    Opt.none()))));
    var ok = compileOk(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("7", ok.logs().get(0).message());
  }

  @Test
  public void test00100VariantAfterFinishRejected() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "res",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "builder",
                                mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                            mb.bind(
                                "new_a",
                                mb.field(mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("a")),
                                            new TestMb.RecordPair("type_ref", mb.typesInt())))), mb.str("value"))),
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.bind(
                                "late",
                                mb.call(
                                    mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair("tag", mb.str("b")),
                                            new TestMb.RecordPair("type_ref", mb.typesInt()))))),
                            mb.return_(
                                mb.ifOf(
                                    TList.of(
                                        new TestMb.Branch(
                                            mb.field(mb.scopeUse("late"), mb.str("has_errors")),
                                            mb.call(mb.log(), mb.str("guarded")))),
                                    Opt.some(mb.call(mb.log(), mb.str("unguarded")))))))))));
    var ok = compileOk(modules);
    assertEquals(1, ok.logs().size());
    assertEquals("guarded", ok.logs().get(0).message());
  }

}
