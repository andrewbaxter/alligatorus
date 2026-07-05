package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.eval.ExprErr;
import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0126CompositeDropOrderTest {

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
                mb.field(mb.scopeUse("u"), mb.str("new")))),
        mb.literalNull());
  }

  private static TList<String> messages(Compile.CompileResult res) {
    var out = new TList<String>(res.logs().size());
    for (var l : res.logs()) out.add(l.message());
    return out;
  }

  @Test
  public void test00100TupleSourcelessDropReverseOrder() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.tupleOf(
                    makeCanaryInstance(mb, "A", "drop_A"),
                    mb.str("const_b"),
                    makeCanaryInstance(mb, "C", "drop_C")),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(
        TList.of("drop_C", "drop_A", "after"),
        messages(ok),
        () -> "sourceless tuple drop should reverse construction order; got " + messages(ok));
  }

  @Test
  public void test00200TupleSourcedDropReverseOrder() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "t",
                    mb.tupleOf(
                        makeCanaryInstance(mb, "A", "drop_A"),
                        mb.str("const_b"),
                        makeCanaryInstance(mb, "C", "drop_C"))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(
        TList.of("after", "drop_C", "drop_A"),
        messages(ok),
        () -> "sourced tuple drop should reverse construction order at end of scope; got "
            + messages(ok));
  }

  @Test
  public void test00300RecordSourcelessDropReverseOrder() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.recordOf(
                    TList.of(
                        new TestMb.RecordPair("a", makeCanaryInstance(mb, "A", "drop_A")),
                        new TestMb.RecordPair("b", mb.str("const_b")),
                        new TestMb.RecordPair("c", makeCanaryInstance(mb, "C", "drop_C")))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(
        TList.of("drop_C", "drop_A", "after"),
        messages(ok),
        () -> "sourceless record drop should reverse construction order (canonical: a,b,c); got "
            + messages(ok));
  }

  @Test
  public void test00400RecordSourcedDropReverseOrder() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "r",
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("a", makeCanaryInstance(mb, "A", "drop_A")),
                            new TestMb.RecordPair("b", mb.str("const_b")),
                            new TestMb.RecordPair("c", makeCanaryInstance(mb, "C", "drop_C"))))),
                mb.call(mb.log(), mb.str("after")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(
        TList.of("after", "drop_C", "drop_A"),
        messages(ok),
        () -> "sourced record drop should reverse construction order at end of scope; got "
            + messages(ok));
  }
}
