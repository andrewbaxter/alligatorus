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

public final class T0030DefineInterfaceTest {
  @Test
  public void test00900DefineInterface() {
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
                                "iface",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("interface")),
                                    mb.tupleOf())),
                            mb.bind(
                                "iface_meth",
                                mb.call(
                                    mb.field(mb.scopeUse("iface"), mb.str("func")),
                                    mb.str("to_string"))),
                            mb.call(
                                mb.field(mb.scopeUse("iface_meth"), mb.str("impl")),
                                mb.tupleOf(mb.typesNull(), mb.typesString())),
                            mb.bind(
                                "ndr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("object")),
                                    mb.str("upper"))),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("set_inner")),
                                mb.typesString()),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                mb.tupleOf(
                                    mb.field(mb.scopeUse("iface"), mb.str("type")),
                                    mb.call(
                                        mb.field(
                                            mb.call(
                                                mb.typesMap(),
                                                mb.tupleOf(mb.typesConstkey(), mb.typesExpr())),
                                            mb.str("new")),
                                        mb.recordOf(
                                            TList.of(
                                                new TestMb.RecordPair(
                                                    "to_string",
                                                    mb.stage(
                                                        mb.call(
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("toUpperCase")),
                                                            mb.literalNull())))))))),
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

  @Test
  public void test00910InterfaceCast() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "defs",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "iface",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("interface")),
                                    mb.tupleOf())),
                            mb.bind(
                                "iface_meth",
                                mb.call(
                                    mb.field(mb.scopeUse("iface"), mb.str("func")),
                                    mb.str("to_string"))),
                            mb.call(
                                mb.field(mb.scopeUse("iface_meth"), mb.str("impl")),
                                mb.tupleOf(mb.typesNull(), mb.typesString())),
                            mb.bind(
                                "ndr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("object")),
                                    mb.str("upper"))),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("set_inner")),
                                mb.typesString()),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                mb.tupleOf(
                                    mb.field(mb.scopeUse("iface"), mb.str("type")),
                                    mb.call(
                                        mb.field(
                                            mb.call(
                                                mb.typesMap(),
                                                mb.tupleOf(mb.typesConstkey(), mb.typesExpr())),
                                            mb.str("new")),
                                        mb.recordOf(
                                            TList.of(
                                                new TestMb.RecordPair(
                                                    "to_string",
                                                    mb.stage(
                                                        mb.call(
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("toUpperCase")),
                                                            mb.literalNull())))))))),
                            mb.bind(
                                "do_log_fn",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("func")),
                                    mb.str("do_log"))),
                            mb.call(
                                mb.field(mb.scopeUse("do_log_fn"), mb.str("impl")),
                                mb.tupleOf(
                                    mb.field(mb.scopeUse("iface"), mb.str("type")),
                                    mb.typesNull(),
                                    mb.stage(
                                        mb.call(
                                            mb.log(),
                                            mb.call(
                                                mb.field(
                                                    mb.scopeUse("arg"),
                                                    mb.str("to_string")),
                                                mb.literalNull()))))),
                            mb.call(
                                mb.field(mb.scopeUse("d"), mb.str("finish")),
                                mb.tupleOf())))),
                mb.bind("do_log", mb.field(mb.scopeUse("defs"), mb.str("do_log"))),
                mb.bind("make_upper", mb.field(mb.scopeUse("defs"), mb.str("upper"))),
                mb.call(
                    mb.scopeUse("do_log"),
                    mb.tupleOf(mb.call(mb.scopeUse("make_upper"), mb.str("hello")))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO", ok.logs().get(0).message());
  }

  @Test
  public void test00920InterfaceWrongMapValueType() {
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
                                "iface",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("interface")),
                                    mb.tupleOf())),
                            mb.bind(
                                "iface_meth",
                                mb.call(
                                    mb.field(mb.scopeUse("iface"), mb.str("func")),
                                    mb.str("to_string"))),
                            mb.call(
                                mb.field(mb.scopeUse("iface_meth"), mb.str("impl")),
                                mb.tupleOf(mb.typesNull(), mb.typesString())),
                            mb.bind(
                                "ndr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("object")),
                                    mb.str("upper"))),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("set_inner")),
                                mb.typesString()),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                mb.tupleOf(
                                    mb.field(mb.scopeUse("iface"), mb.str("type")),
                                    mb.call(
                                        mb.field(
                                            mb.call(
                                                mb.typesMap(),
                                                mb.tupleOf(mb.typesString(), mb.typesNull())),
                                            mb.str("new")),
                                        mb.recordOf(
                                            TList.of(
                                                new TestMb.RecordPair(
                                                    "to_string",
                                                    mb.stage(
                                                        mb.call(
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("toUpperCase")),
                                                            mb.literalNull())))))))),
                            mb.field(mb.scopeUse("ndr"), mb.str("new"))))))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>, () -> "expected compile error but got: " + res);
  }
}
