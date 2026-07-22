package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.eval.ExprErr;
import compilerxyz.langhelp.Compile;
import org.junit.jupiter.api.Disabled;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0160EvenOddTest {
  private static SourceExpr optAspectSome(TestMb mb, SourceExpr value) {
    return mb.call(
        mb.field(mb.call(mb.typesOption(), mb.typesAspect()), mb.str("some")),
        value);
  }

  private static SourceExpr optAspectNone(TestMb mb) {
    return mb.call(
        mb.field(mb.call(mb.typesOption(), mb.typesAspect()), mb.str("none")),
        mb.literalNull());
  }

  private static SourceExpr argKey(TestMb mb) {
    return mb.field(mb.scopeUse("arg"), mb.str("key"));
  }

  private static SourceExpr argXx(TestMb mb) {
    return mb.field(mb.scopeUse("arg"), mb.str("xx"));
  }

  private static SourceExpr identRef(TestMb mb) {
    return mb.lower(mb.scopeUse("ident"));
  }

  private static SourceExpr intAspectDelegate(TestMb mb) {
    return mb.call(
        mb.field(mb.typesInt(), mb.str("aspect")),
        mb.recordOf(
            TList.of(
                new TestMb.RecordPair("xx", argXx(mb)),
                new TestMb.RecordPair("key", argKey(mb)))));
  }

  private static SourceExpr buildAspectImpl(TestMb mb) {
    var branches = new TList<TestMb.Branch>();
    branches.add(
        new TestMb.Branch(
            mb.operatorEq(argKey(mb), identRef(mb)),
            optAspectSome(mb, mb.scopeUse("outer"))));
    TList<SourceExpr> whitelist =
        TList.of(
            mb.evalAspectSetId(),
            mb.evalOperatorAddId(),
            mb.evalAspectBindId(),
            mb.evalAspectBindDynamicId(),
            mb.evalAspectConsumeDynamicId(),
            mb.evalAspectDropDynamicLightId(),
            mb.evalAspectStackValueDynamicId(),
            mb.evalAspectInitialBindId(),
            mb.evalAspectFieldId());
    for (var id : whitelist) {
      branches.add(
          new TestMb.Branch(mb.operatorEq(argKey(mb), id), intAspectDelegate(mb)));
    }
    return mb.ifOf(branches, optAspectNone(mb));
  }

  private static SourceExpr buildCanCastFromImpl(TestMb mb) {
    var srcAspect =
        mb.call(
            mb.field(
                mb.field(mb.scopeUse("arg"), mb.str("source_type")),
                mb.str("aspect")),
            mb.recordOf(
                TList.of(
                    new TestMb.RecordPair("xx", argXx(mb)),
                    new TestMb.RecordPair("key", identRef(mb)))));
    var isSome = mb.field(srcAspect, mb.str("is_some"));
    var delegateInt =
        mb.call(
            mb.field(mb.typesInt(), mb.str("can_cast_from")),
            mb.recordOf(
                TList.of(
                    new TestMb.RecordPair("xx", argXx(mb)),
                    new TestMb.RecordPair(
                        "loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                    new TestMb.RecordPair(
                        "source_type",
                        mb.field(mb.scopeUse("arg"), mb.str("source_type"))))));
    return mb.ifOf(
        TList.of(
            new TestMb.Branch(isSome, mb.as_(mb.bool(true), mb.typesBool()))),
        delegateInt);
  }

  private static SourceExpr ifaceMethods(TestMb mb, TList<TestMb.RecordPair> methods) {
    var staged = new TList<TestMb.RecordPair>(methods.size());
    for (var m : methods) {
      staged.add(new TestMb.RecordPair(m.key(), mb.stage(m.value())));
    }
    return mb.call(
        mb.field(
            mb.call(mb.typesMap(), mb.tupleOf(mb.typesConstkey(), mb.typesExpr())),
            mb.str("new")),
        mb.recordOf(staged));
  }

  /**
   * Builds a source-object type "Even" (or "Odd") wrapping a long. Impls AspectSet: takes an
   * argument of the SAME EvenOdd type (looked up via `sameTypeRef`) — emits code that reinterprets
   * arg to the same type and stores it. Attempting to set an "Even" with an "Odd" value fails at
   * compile time because the arg's static type is different.
   */
  private static SourceExpr buildEvenOddType(TestMb mb, String name, double parityInt) {
    return mb.call(
        mb.lower(
            mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "u",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("object")),
                        mb.str(name))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("set_inner")), mb.typesInt()),
                mb.bind("ident", mb.call(mb.evalFreshUnique(), mb.tupleOf())),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("iface_ref", mb.evalType()),
                            new TestMb.RecordPair(
                                "record",
                                ifaceMethods(
                                    mb,
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "can_cast_from", buildCanCastFromImpl(mb)),
                                        new TestMb.RecordPair(
                                            "cast_from",
                                            mb.call(
                                                mb.field(
                                                    mb.call(mb.typesEresult(), mb.typesCve()),
                                                    mb.str("ok")),
                                                mb.call(
                                                    mb.evalCodevalue(),
                                                    mb.recordOf(
                                                        TList.of(
                                                            new TestMb.RecordPair(
                                                                "code",
                                                                mb.field(
                                                                    mb.field(
                                                                        mb.call(
                                                                            mb.field(
                                                                                mb.typesInt(),
                                                                                mb.str("cast_from")),
                                                                            mb.recordOf(
                                                                                TList.of(
                                                                                    new TestMb.RecordPair(
                                                                                        "xx",
                                                                                        mb.field(
                                                                                            mb.scopeUse("arg"),
                                                                                            mb.str("xx"))),
                                                                                    new TestMb.RecordPair(
                                                                                        "loc",
                                                                                        mb.field(
                                                                                            mb.scopeUse("arg"),
                                                                                            mb.str("loc"))),
                                                                                    new TestMb.RecordPair(
                                                                                        "cv",
                                                                                        mb.field(
                                                                                            mb.scopeUse("arg"),
                                                                                            mb.str("cv")))))),
                                                                        mb.str("value")),
                                                                    mb.str("code"))),
                                                            new TestMb.RecordPair(
                                                                "value",
                                                                mb.call(
                                                                    mb.evalStack(),
                                                                    mb.scopeUse("outer")))))))),
                                        new TestMb.RecordPair(
                                            "desc",
                                            mb.seqOf(mb.return_(mb.str(name)))),
                                        new TestMb.RecordPair(
                                            "aspect", buildAspectImpl(mb)),
                                        new TestMb.RecordPair(
                                            "can_cast_to", mb.bool(false)),
                                        new TestMb.RecordPair(
                                            "cast_to",
                                            mb.panic(mb.str(name + ".cast_to unreachable"))))))))),
                mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.field(mb.scopeUse("u"), mb.str("new"))))),
        mb.num(parityInt));
  }

  @Test
  public void test00100DefineEvenType() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("even_t", mb.lower(buildEvenOddType(mb, "Even", 2.0))),
                mb.call(mb.log(), mb.str("defined")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertTrue(ok.logs().size() >= 1);
  }

  @Test
  public void test00200SetSameType() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("even_t", mb.lower(buildEvenOddType(mb, "Even", 2.0))),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_t"))),
                mb.bind("b", mb.as_(mb.num(4.0), mb.scopeUse("even_t"))),
                mb.set(mb.scopeUse("a"), mb.scopeUse("b")),
                mb.call(mb.log(), mb.str("ok")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
  }

  @Test
  public void test00250SetInIfBothBranches() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("even_t", mb.lower(buildEvenOddType(mb, "Even", 2.0))),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_t"))),
                mb.bind("b", mb.as_(mb.num(4.0), mb.scopeUse("even_t"))),
                mb.ifOf(
                    TList.of(
                        new TestMb.Branch(
                            mb.bool(true), mb.set(mb.scopeUse("a"), mb.scopeUse("b")))),
                    mb.set(mb.scopeUse("a"), mb.scopeUse("b"))),
                mb.call(mb.log(), mb.str("ok")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
  }

  @Test
  public void test00260SetInIfNoElse() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("even_t", mb.lower(buildEvenOddType(mb, "Even", 2.0))),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_t"))),
                mb.bind("b", mb.as_(mb.num(4.0), mb.scopeUse("even_t"))),
                mb.ifOf(
                    TList.of(
                        new TestMb.Branch(
                            mb.bool(true), mb.set(mb.scopeUse("a"), mb.scopeUse("b")))),
                    null),
                mb.call(mb.log(), mb.str("ok")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
  }

  @Test
  public void test00275SetInTryCatch() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("even_t", mb.lower(buildEvenOddType(mb, "Even", 2.0))),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_t"))),
                mb.bind("b", mb.as_(mb.num(4.0), mb.scopeUse("even_t"))),
                mb.tryCatchFinally(
                    mb.seqOf(
                        mb.set(mb.scopeUse("a"), mb.scopeUse("b")),
                        mb.call(mb.log(), mb.str("body"))),
                    new TestMb.Catch(
                        "e",
                        mb.typesString(),
                        mb.seqOf(
                            mb.set(mb.scopeUse("a"), mb.scopeUse("b")),
                            mb.call(mb.log(), mb.str("catch")))),
                    null),
                mb.call(mb.log(), mb.str("ok")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
  }

  @Test
  public void test00280SetInTryFinally() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("even_t", mb.lower(buildEvenOddType(mb, "Even", 2.0))),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_t"))),
                mb.bind("b", mb.as_(mb.num(4.0), mb.scopeUse("even_t"))),
                mb.tryCatchFinally(
                    mb.seqOf(
                        mb.set(mb.scopeUse("a"), mb.scopeUse("b")),
                        mb.call(mb.log(), mb.str("body"))),
                    null,
                    mb.seqOf(
                        mb.set(mb.scopeUse("a"), mb.scopeUse("b")),
                        mb.call(mb.log(), mb.str("fin")))),
                mb.call(mb.log(), mb.str("ok")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
  }

  @Test
  public void test00300AddSameType() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("even_t", mb.lower(buildEvenOddType(mb, "Even", 2.0))),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_t"))),
                mb.bind("b", mb.as_(mb.num(4.0), mb.scopeUse("even_t"))),
                mb.bind("c", mb.operatorAdd(mb.scopeUse("a"), mb.scopeUse("b"))),
                mb.call(mb.log(), mb.str("ok")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
  }

}
