package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.eval.ExprErr;
import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0150ReturnTest {
  @Test
  public void test01600ReturnEmptyFromBlock() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.call(mb.log(), mb.str("before")),
                mb.returnEmpty(),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>,
        () -> "expected unreachable-code error: " + res);
  }

  @Test
  public void test01610ReturnLabeledSkipsLog() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.call(mb.log(), mb.str("before")),
                mb.labeled("outer", mb.seqOf(
                    mb.call(mb.log(), mb.str("inside")),
                    mb.return_("outer", null))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(3, ok.logs().size());
    assertEquals("before", ok.logs().get(0).message());
    assertEquals("inside", ok.logs().get(1).message());
    assertEquals("after", ok.logs().get(2).message());
  }

  @Test
  public void test01620ReturnUnknownLabelErrors() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(mb.return_("nowhere", null)));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>,
        () -> "expected unknown-label error: " + res);
  }

  @Test
  public void test01625ReturnToSiblingLabelErrors() {
    // A label that exists elsewhere in the program but does not enclose the
    // return must be an error, same as a label that exists nowhere.
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.labeled("other", mb.call(mb.log(), mb.str("in-other"))),
                mb.labeled("outer", mb.seqOf(mb.return_("other", null))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>,
        () -> "expected non-enclosing-label error: " + res);
  }

  @Test
  public void test01630ReturnFromForeachSkipsRest() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_list",
                    mb.call(
                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(3.)))),
                mb.foreach(
                    mb.scopeUse("my_list"),
                    mb.str("e"),
                    mb.seqOf(
                        mb.call(mb.log(), mb.str("iter")),
                        mb.returnEmpty(),
                        mb.call(mb.log(), mb.str("SHOULD NOT LOG")))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>,
        () -> "expected unreachable-after-return error: " + res);
  }

  @Test
  public void test01635ReturnWithValueFromBlock() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.call(
                mb.log(),
                mb.labeled("out",
                    mb.seqOf(
                        mb.return_("out", mb.str("returned-value")))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("returned-value", ok.logs().get(0).message());
  }

  @Test
  public void test01640ReturnFromIf() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.labeled("out", mb.seqOf(
                mb.call(mb.log(), mb.str("before")),
                mb.ifOf(
                    TList.of(new TestMb.Branch(
                        mb.bool(true),
                        mb.seqOf(
                            mb.call(mb.log(), mb.str("in-if")),
                            mb.return_("out", null),
                            mb.call(mb.log(), mb.str("SHOULD NOT LOG"))))),
                    mb.call(mb.log(), mb.str("else"))),
                mb.call(mb.log(), mb.str("SHOULD NOT LOG AFTER"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>,
        () -> "expected unreachable errors: " + res);
  }

  // Spec: "correct exit point (by skipping over a log statement using a label)
  // from each exit target" — verify the labeled-return path fires each construct
  // exactly at the label, with a log after the return inside the exit target's
  // body that never runs.

  @Test
  public void test01650LabeledExitFromForeachSkipsLogInside() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "my_list",
                    mb.call(
                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(3.)))),
                mb.labeled(
                    "outer",
                    mb.foreach(
                        mb.scopeUse("my_list"),
                        mb.str("e"),
                        mb.seqOf(
                            mb.call(mb.log(), mb.str("iter")),
                            mb.return_("outer", null)))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(2, ok.logs().size());
    assertEquals("iter", ok.logs().get(0).message());
    assertEquals("after", ok.logs().get(1).message());
  }

  @Test
  public void test01660LabeledExitFromIfSkipsLogAfter() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.labeled(
                    "outer",
                    mb.ifOf(
                        TList.of(new TestMb.Branch(
                            mb.bool(true),
                            mb.seqOf(
                                mb.call(mb.log(), mb.str("in-if")),
                                mb.return_("outer", null)))),
                        mb.call(mb.log(), mb.str("SHOULD NOT LOG else")))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(2, ok.logs().size());
    assertEquals("in-if", ok.logs().get(0).message());
    assertEquals("after", ok.logs().get(1).message());
  }

  @Test
  public void test01670LabeledExitFromMatchSkipsLogAfter() {
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
                            mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                            mb.return_(mb.recordOf(
                                TList.of(
                                    new TestMb.RecordPair(
                                        "type",
                                        mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                    new TestMb.RecordPair("str", mb.scopeUse("new_str")))))))),
                mb.bind(
                    "x",
                    mb.call(
                        mb.field(mb.scopeUse("strnum"), mb.str("str")), mb.str("value"))),
                mb.labeled(
                    "outer",
                    mb.matchOf(
                        mb.scopeUse("x"),
                        mb.str("y"),
                        TList.of(
                            new TestMb.Branch(
                                mb.str("strtag"),
                                mb.seqOf(
                                    mb.call(mb.log(), mb.str("in-match")),
                                    mb.return_("outer", null)))),
                        null)),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(2, ok.logs().size());
    assertEquals("in-match", ok.logs().get(0).message());
    assertEquals("after", ok.logs().get(1).message());
  }

  @Test
  public void test01675IfWithReturningBranchCascade() {
    // if's first branch has a control-ended body (return); cascade should
    // pick up else's type for the if's result type. Without the cascade fix,
    // targetIf would reject "first branch body has no value".
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.labeled(
                "out",
                mb.seqOf(
                    mb.call(mb.log(), mb.str("before")),
                    mb.ifOf(
                        TList.of(new TestMb.Branch(
                            mb.bool(false),
                            mb.return_("out", null))),
                        mb.call(mb.log(), mb.str("else"))),
                    mb.call(mb.log(), mb.str("after"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(3, ok.logs().size());
    assertEquals("before", ok.logs().get(0).message());
    assertEquals("else", ok.logs().get(1).message());
    assertEquals("after", ok.logs().get(2).message());
  }

  @Test
  public void test01677LabeledReturnFromTryBodyRunsFinally() {
    // A labeled return exiting a try body must run the user finally block on
    // its way out, exactly like a normal exit or an exception would.
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.labeled(
                    "outer",
                    mb.tryCatchFinally(
                        mb.seqOf(
                            mb.call(mb.log(), mb.str("body")),
                            mb.return_("outer", null)),
                        null,
                        mb.call(mb.log(), mb.str("finally")))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    var messages = new TList<String>(ok.logs().size());
    for (var l : ok.logs()) messages.add(l.message());
    assertEquals(
        TList.of("body", "finally", "after"),
        messages,
        () -> "labeled return out of a try body must run the finally; got logs=" + messages);
  }

  @Test
  public void test01680ImplicitEmptyTupleReturnFromNoValueReturn() {
    // Spec: "If return has no value and the exit target needs a value, it
    // implicitly returns an empty tuple." Verify: block value produced by
    // return-with-no-value is empty tuple, observable as no-op when passed
    // to a caller expecting empty tuple (no crash, program compiles).
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("t", mb.labeled("out", mb.seqOf(mb.returnEmpty()))),
                mb.call(mb.log(), mb.str("done")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("done", ok.logs().get(0).message());
  }

}
