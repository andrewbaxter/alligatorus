package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;
import compilerxyz.eval.ExprErr;

public final class T0130DropThrowTest {
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

  private static SourceExpr makeCanaryInstance(TestMb mb, String name, String dropMessage) {
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
                    mb.field(mb.scopeUse("u"), mb.str("set_inner")), mb.typesNull()),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("value")),
                    ifaceMethods(
                        mb, TList.of(new TestMb.RecordPair("type", mb.scopeUse("inner"))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("type")),
                    ifaceMethods(
                        mb,
                        TList.of(
                            new TestMb.RecordPair(
                                "can_cast_from",
                                mb.seqOf(
                                    mb.call(
                                        mb.field(
                                            mb.call(
                                                mb.field(
                                                    mb.field(
                                                        mb.scopeUse("arg"),
                                                        mb.str("source_type")),
                                                    mb.str("aspect")),
                                                mb.evalAspectStringDynamic()),
                                            mb.str("is_some")),
                                        mb.tupleOf()))),
                            new TestMb.RecordPair(
                                "cast_from",
                                mb.call(
                                    mb.evalConstEval(),
                                    mb.tupleOf(
                                        mb.field(mb.scopeUse("arg"), mb.str("xx")),
                                        mb.stage(
                                            mb.graft(
                                                mb.field(
                                                    mb.scopeUse("arg"),
                                                    mb.str("cv"))))))),
                            new TestMb.RecordPair("desc", mb.seqOf(mb.str(name))),
                            new TestMb.RecordPair(
                                "aspect",
                                mb.call(
                                    mb.field(
                                        mb.call(mb.typesOption(), mb.typesAspect()),
                                        mb.str("none")),
                                    mb.literalNull()))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.tupleOf(
                        mb.evalAspectDropDynamic(),
                        ifaceMethods(
                            mb,
                            TList.of(
                                new TestMb.RecordPair(
                                    "drop",
                                    mb.call(
                                        mb.evalConstEval(),
                                        mb.tupleOf(
                                            mb.field(mb.scopeUse("arg"), mb.str("xx")),
                                            mb.stage(
                                                mb.call(mb.log(), mb.str(dropMessage)))))))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("new")),
                    mb.tupleOf()))),
        mb.literalNull());
  }

  private static Compile.CompileResult compileOk(TMap<String, SourceModule> modules) {
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    return ok;
  }

  private static TList<String> messages(Compile.CompileResult res) {
    var out = new TList<String>(res.logs().size());
    for (var l : res.logs()) out.add(l.message());
    return out;
  }

  @Test
  public void test01300ThrowInsideSeqKeepsOuterValue() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.tryCatchFinally(
                    mb.throw_(mb.str("boom")),
                    new TestMb.Catch(
                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e"))),
                    null),
                mb.call(mb.log(), mb.str("after")))));
    var res = compileOk(modules);
    assertEquals(TList.of("boom", "after"), messages(res));
  }

  @Test
  public void test01310TryFinallyRunsFinally() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.tryCatchFinally(
                mb.call(mb.log(), mb.str("body")),
                null,
                mb.call(mb.log(), mb.str("finally")))));
    var res = compileOk(modules);
    assertEquals(TList.of("body", "finally"), messages(res));
  }

  @Test
  public void test01320TryCatchHandlesThrow() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.tryCatchFinally(
                    mb.throw_(mb.str("boom")),
                    new TestMb.Catch(
                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e"))),
                    null),
                mb.call(mb.log(), mb.str("done")))));
    var res = compileOk(modules);
    assertEquals(TList.of("boom", "done"), messages(res));
  }

  @Test
  public void test01330TryRequiresCatchOrFinally() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.tryCatchFinally(mb.call(mb.log(), mb.str("body")), null, null)));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(
        res instanceof Result.Err<?, ?>,
        () -> "Try with no catch and no finally should fail compile");
  }

  @Test
  public void test01340ThrowCaughtLogsMessage() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.tryCatchFinally(
                mb.throw_(mb.str("boom")),
                new TestMb.Catch(
                    "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e"))),
                null)));
    var res = compileOk(modules);
    assertEquals(TList.of("boom"), messages(res));
  }

  @Test
  public void test01350ThrowWithNoCatchInFunctionNoExceptionTypeRejects() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_func",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.tupleOf(
                                    mb.typesNull(),
                                    mb.typesNull(),
                                    mb.stage(mb.throw_(mb.str("x"))))),
                            mb.scopeUse("f")))),
                mb.call(mb.scopeUse("my_func"), mb.tupleOf()))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(
        res instanceof Result.Err<?, ?>,
        () -> "throw in function with no declared exception type should be rejected: " + res);
  }

  @Test
  public void test01360SetExceptionTypeAllowsThrow() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_func",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("set_exception_type")),
                                mb.typesString()),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.tupleOf(
                                    mb.typesNull(),
                                    mb.typesNull(),
                                    mb.stage(mb.throw_(mb.str("x"))))),
                            mb.scopeUse("f")))),
                mb.tryCatchFinally(
                    mb.call(mb.scopeUse("my_func"), mb.tupleOf()),
                    new TestMb.Catch(
                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e"))),
                    null))));
    var res = compileOk(modules);
    assertEquals(TList.of("x"), messages(res));
  }

  @Test
  public void test01370TryCatchFinallyCombined() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.tryCatchFinally(
                mb.throw_(mb.str("boom")),
                new TestMb.Catch(
                    "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.str("caught"))),
                mb.call(mb.log(), mb.str("always")))));
    var res = compileOk(modules);
    assertEquals(TList.of("caught", "always"), messages(res));
  }

  @Test
  public void test01400EndOfScopeDropRuntime() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                mb.bind(
                    "cnry",
                    mb.as_(
                        mb.as_(mb.str("X"), mb.typesString()), mb.scopeUse("canary"))),
                mb.call(mb.log(), mb.str("after")))));
    var res = compileOk(modules);
    var messages = messages(res);
    assertEquals(
        TList.of("after", "dropped"),
        messages,
        () -> "end-of-scope drop should run after body; got logs=" + messages);
  }

  @Test
  public void test01410DiscardedSeqEntryRuntime() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                mb.as_(mb.as_(mb.str("Y"), mb.typesString()), mb.scopeUse("canary")),
                mb.call(mb.log(), mb.str("after")))));
    var res = compileOk(modules);
    var messages = messages(res);
    assertEquals(
        TList.of("dropped", "after"),
        messages,
        () -> "discarded seq entry should be dropped before next entry; got logs=" + messages);
  }

  @Test
  public void test01420FieldSiblingDropRuntime() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "canary_a", mb.lower(makeCanaryInstance(mb, "CanaryA", "dropped_a"))),
                mb.bind(
                    "canary_b", mb.lower(makeCanaryInstance(mb, "CanaryB", "dropped_b"))),
                mb.bind(
                    "rec",
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair(
                                "a",
                                mb.as_(
                                    mb.as_(mb.str("A"), mb.typesString()),
                                    mb.scopeUse("canary_a"))),
                            new TestMb.RecordPair(
                                "b",
                                mb.as_(
                                    mb.as_(mb.str("B"), mb.typesString()),
                                    mb.scopeUse("canary_b")))))),
                mb.field(mb.scopeUse("rec"), mb.str("a")))));
    var res = compileOk(modules);
    var messages = messages(res);
    assertTrue(
        messages.contains("dropped_b"),
        () -> "sibling 'b' should be dropped when field 'a' is accessed; got logs=" + messages);
    assertFalse(
        messages.contains("dropped_a"),
        () ->
            "extracted field 'a' should not be dropped at the field-access site; got logs="
                + messages);
  }

  @Test
  public void test01430DropOnThrowRuntime() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.tryCatchFinally(
                mb.seqOf(
                    mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                    mb.bind(
                        "cnry",
                        mb.as_(
                            mb.as_(mb.str("Z"), mb.typesString()),
                            mb.scopeUse("canary"))),
                    mb.throw_(mb.str("boom"))),
                new TestMb.Catch(
                    "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.str("caught"))),
                null)));
    var res = compileOk(modules);
    var messages = messages(res);
    int dropIdx = messages.indexOf("dropped");
    int caughtIdx = messages.indexOf("caught");
    assertTrue(dropIdx >= 0, () -> "expected 'dropped' log, got " + messages);
    assertTrue(caughtIdx >= 0, () -> "expected 'caught' log, got " + messages);
    assertTrue(
        dropIdx < caughtIdx,
        () -> "drop should run before catch handler; got logs=" + messages);
  }

  @Test
  public void test01440TryCatchUnifiesBranchTypes() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.tryCatchFinally(
                    mb.call(mb.log(), mb.str("body")),
                    new TestMb.Catch(
                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.str("caught"))),
                    null),
                mb.call(mb.log(), mb.str("after")))));
    var res = compileOk(modules);
    assertEquals(TList.of("body", "after"), messages(res));
  }

  @Test
  public void test01450TryCatchBranchTypeMismatchErrors() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.tryCatchFinally(
                mb.call(mb.log(), mb.str("body")),
                new TestMb.Catch("e", mb.builtin("types", "string"), mb.scopeUse("e")),
                null)));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(
        res instanceof Result.Err<?, ?>,
        () -> "Try with incompatible body/catch result types should fail compile: " + res);
  }

  @Test
  public void test01460FinallyDropsResult() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                mb.tryCatchFinally(
                    mb.call(mb.log(), mb.str("body")),
                    null,
                    mb.as_(
                        mb.as_(mb.str("V"), mb.typesString()), mb.scopeUse("canary"))),
                mb.call(mb.log(), mb.str("after")))));
    var res = compileOk(modules);
    var messages = messages(res);
    int dropIdx = messages.indexOf("dropped");
    int afterIdx = messages.indexOf("after");
    assertTrue(dropIdx >= 0, () -> "expected 'dropped' log, got " + messages);
    assertTrue(afterIdx >= 0, () -> "expected 'after' log, got " + messages);
    assertTrue(
        dropIdx < afterIdx,
        () -> "finally result drop should run before 'after'; got logs=" + messages);
  }

  @Test
  public void test01470BindOfThrowErrors() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_func",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("set_exception_type")),
                                mb.typesString()),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.tupleOf(
                                    mb.typesNull(),
                                    mb.typesNull(),
                                    mb.stage(
                                        mb.seqOf(
                                            mb.bind("x", mb.throw_(mb.str("boom"))),
                                            mb.literalNull())))),
                            mb.scopeUse("f")))),
                mb.tryCatchFinally(
                    mb.call(mb.scopeUse("my_func"), mb.tupleOf()),
                    new TestMb.Catch(
                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e"))),
                    null))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(
        res instanceof Result.Err<?, ?>, () -> "expected compile error binding result of throw");
    var msg = res.toString();
    assertTrue(
        msg.contains("Cannot bind result of expression that ends control flow"),
        () -> "expected bind-of-throw error message, got: " + msg);
  }

  @Test
  public void test01480LogAfterThrowInSeqErrors() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_func",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "nfr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                            mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("set_exception_type")),
                                mb.typesString()),
                            mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.tupleOf(
                                    mb.typesNull(),
                                    mb.typesNull(),
                                    mb.stage(
                                        mb.seqOf(
                                            mb.throw_(mb.str("boom")),
                                            mb.call(mb.log(), mb.str("unreachable")))))),
                            mb.scopeUse("f")))),
                mb.tryCatchFinally(
                    mb.call(mb.scopeUse("my_func"), mb.tupleOf()),
                    new TestMb.Catch(
                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e"))),
                    null))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(
        res instanceof Result.Err<?, ?>, () -> "expected compile error for code after throw");
    var msg = res.toString();
    assertTrue(
        msg.contains("Unreachable"), () -> "expected Unreachable error message, got: " + msg);
  }
}
