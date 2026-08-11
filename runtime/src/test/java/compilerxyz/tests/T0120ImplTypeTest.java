package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public final class T0120ImplTypeTest {
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

  private static SourceExpr buildUpperType(TestMb mb) {
    return mb.call(
        mb.lower(
            mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "u",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("object")),
                        mb.str("upper"))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("set_inner")), mb.typesNull()),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("iface_ref", mb.evalAspectLift()),
                            new TestMb.RecordPair(
                                "record",
                                mb.call(
                                    mb.field(
                                        mb.call(
                                            mb.typesMap(),
                                            mb.tupleOf(
                                                mb.typesConstkey(),
                                                mb.typesExpr())),
                                        mb.str("new")),
                                    mb.recordOf(TList.of())))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair(
                                "iface_ref", mb.evalAspectConsumeDynamic()),
                            new TestMb.RecordPair(
                                "record",
                                ifaceMethods(
                                    mb,
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "to_dynamic",
                                            mb.delegateAspect(
                                                mb.typesNull(),
                                                mb.evalAspectConsumeDynamic(),
                                                "to_dynamic",
                                                "xx",
                                                "loc",
                                                "cv")),
                                        new TestMb.RecordPair(
                                            "jvm_class",
                                            mb.delegateAspect(
                                                mb.typesNull(),
                                                mb.evalAspectConsumeDynamic(),
                                                "jvm_class",
                                                "xx")))))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("iface_ref", mb.evalAspectBind()),
                            new TestMb.RecordPair(
                                "record",
                                ifaceMethods(
                                    mb,
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "bind",
                                            mb.delegateAspect(
                                                mb.typesNull(),
                                                mb.evalAspectBind(),
                                                "bind",
                                                "xx",
                                                "loc",
                                                "cv")))))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair(
                                "iface_ref", mb.evalAspectBindDynamic()),
                            new TestMb.RecordPair(
                                "record",
                                ifaceMethods(
                                    mb,
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "bind_dynamic",
                                            mb.delegateAspect(
                                                mb.typesNull(),
                                                mb.evalAspectBindDynamic(),
                                                "bind_dynamic",
                                                "xx",
                                                "loc",
                                                "cv")))))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("iface_ref", mb.evalValue()),
                            new TestMb.RecordPair(
                                "record",
                                ifaceMethods(
                                    mb,
                                    TList.of(
                                        new TestMb.RecordPair("type", mb.scopeUse("inner")))))))),
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
                                "can_cast_from",
                                mb.seqOf(
                                    mb.return_(mb.field(
                                        mb.call(
                                            mb.field(
                                                mb.field(
                                                    mb.scopeUse("arg"),
                                                    mb.str("source_type")),
                                                mb.str("aspect")),
                                            mb.recordOf(
                                                TList.of(
                                                    new TestMb.RecordPair(
                                                        "xx",
                                                        mb.field(
                                                            mb.scopeUse("arg"),
                                                            mb.str("xx"))),
                                                    new TestMb.RecordPair(
                                                        "key",
                                                        mb.evalAspectStringDynamic())))),
                                        mb.str("is_some"))))),
                            new TestMb.RecordPair(
                                "cast_from",
                                mb.call(
                                    mb.evalConstEval(),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "xx_",
                                                mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                            new TestMb.RecordPair(
                                                "staged",
                                                mb.stage(
                                                    mb.call(
                                                        mb.field(
                                                            mb.graft(
                                                                mb.field(
                                                                    mb.scopeUse("arg"),
                                                                    mb.str("cv"))),
                                                            mb.str("toUpperCase")),
                                                        mb.tupleOf()))))))),
                            new TestMb.RecordPair("desc", mb.seqOf(mb.return_(mb.str("upper")))),
                            new TestMb.RecordPair(
                                "aspect",
                                mb.call(
                                    mb.field(
                                        mb.call(mb.typesOption(), mb.typesAspect()),
                                        mb.str("none")),
                                    mb.literalNull())),
                            new TestMb.RecordPair(
                                "is_equal",
                                mb.seqOf(
                                    mb.return_(mb.operatorEq(mb.num(0.0), mb.num(1.0))))),
                            new TestMb.RecordPair(
                                "can_cast_to",
                                mb.seqOf(
                                    mb.return_(mb.operatorEq(mb.num(1.0), mb.num(1.0))))),
                            new TestMb.RecordPair(
                                "cast_to",
                                mb.call(
                                    mb.field(
                                        mb.call(mb.typesEresult(), mb.typesCve()),
                                        mb.str("ok")),
                                    mb.field(mb.scopeUse("arg"), mb.str("cv")))))))))),
                mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.field(mb.scopeUse("u"), mb.str("new"))))),
        mb.literalNull());
  }

  @Test
  public void test00100ImplType() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("upper", mb.lower(buildUpperType(mb))),
                mb.call(
                    mb.log(),
                    mb.as_(
                        mb.as_(mb.str("hello"), mb.typesString()),
                        mb.scopeUse("upper"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO", ok.logs().get(0).message());
  }

  @Test
  public void test00200ImplTypeImport() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put("other", new SourceModule.V1(buildUpperType(mb)));
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("upper", mb.importExpr(mb.str("other"))),
                mb.call(
                    mb.log(),
                    mb.as_(
                        mb.as_(mb.str("hello"), mb.typesString()),
                        mb.scopeUse("upper"))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO", ok.logs().get(0).message());
  }
}
