package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0030DefineInterfaceTest {
    @Test
    public void test00900DefineInterface() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "defs",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "iface",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("interface")), mb.tupleOf())),
                                        mb.call(
                                                mb.field(mb.scopeUse("iface"), mb.str("func")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("name", mb.str("to_string")),
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesString()))))),
                                        mb.bind(
                                                "ndr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("upper"))),
                                        mb.call(mb.field(mb.scopeUse("ndr"), mb.str("set_inner")), mb.typesString()),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectConsumeDynamic()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "to_dynamic",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                                "to_dynamic",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv"))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "jvm_class",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                                "jvm_class",
                                                                                                                                "xx")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("iface_ref", mb.evalAspectBind()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "bind",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBind(),
                                                                                                                                "bind",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectBindDynamic()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "bind_dynamic",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBindDynamic(),
                                                                                                                                "bind_dynamic",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectField()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "field",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectField(),
                                                                                                                                "field",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "self",
                                                                                                                                "key"))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "type_opt",
                                                                                                        mb.stage(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .typesOption(),
                                                                                                                                                        mb
                                                                                                                                                                .evalType()),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "none")),
                                                                                                                        mb
                                                                                                                                .tupleOf()))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "keys",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .seqOf(
                                                                                                                                mb
                                                                                                                                        .return_(
                                                                                                                                                mb
                                                                                                                                                        .call(
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .typesList(),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .typesConstkey()),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "new")),
                                                                                                                                                                mb
                                                                                                                                                                        .tupleOf())))))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref",
                                                                        mb.field(mb.scopeUse("iface"), mb.str("type"))),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "to_string",
                                                                                                        mb.stage(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "inner"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "toUpperCase")),
                                                                                                                        mb
                                                                                                                                .tupleOf())))))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "iface",
                                                                mb.field(mb.scopeUse("iface"), mb.str("type"))),
                                                        new TestMb.RecordPair(
                                                                "upper",
                                                                mb.field(mb.scopeUse("ndr"), mb.str("new"))))))))),
                        mb.bind("make_upper", mb.field(mb.scopeUse("defs"), mb.str("upper"))),
                        mb.bind("val", mb.call(mb.scopeUse("make_upper"), mb.str("hello"))),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.field(mb.scopeUse("defs"), mb.str("iface")), mb.str("to_string")),
                                        mb.scopeUse("val"))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> ok = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e ->
                throw new AssertionError("compile failed: " + res);
        }
        assertEquals(1, ok.value().logs().size());
        assertEquals("HELLO", ok.value().logs().get(0).message());
    }

    @Test
    public void test00910InterfaceCast() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "defs",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "iface",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("interface")), mb.tupleOf())),
                                        mb.call(
                                                mb.field(mb.scopeUse("iface"), mb.str("func")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("name", mb.str("to_string")),
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesString()))))),
                                        mb.bind(
                                                "ndr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("upper"))),
                                        mb.call(mb.field(mb.scopeUse("ndr"), mb.str("set_inner")), mb.typesString()),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectConsumeDynamic()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "to_dynamic",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                                "to_dynamic",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv"))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "jvm_class",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                                "jvm_class",
                                                                                                                                "xx")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("iface_ref", mb.evalAspectBind()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "bind",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBind(),
                                                                                                                                "bind",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectBindDynamic()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "bind_dynamic",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBindDynamic(),
                                                                                                                                "bind_dynamic",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectField()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "field",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectField(),
                                                                                                                                "field",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "self",
                                                                                                                                "key"))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "type_opt",
                                                                                                        mb.stage(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .typesOption(),
                                                                                                                                                        mb
                                                                                                                                                                .evalType()),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "none")),
                                                                                                                        mb
                                                                                                                                .tupleOf()))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "keys",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .seqOf(
                                                                                                                                mb
                                                                                                                                        .return_(
                                                                                                                                                mb
                                                                                                                                                        .call(
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .typesList(),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .typesConstkey()),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "new")),
                                                                                                                                                                mb
                                                                                                                                                                        .tupleOf())))))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref",
                                                                        mb.field(mb.scopeUse("iface"), mb.str("type"))),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "to_string",
                                                                                                        mb.stage(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "inner"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "toUpperCase")),
                                                                                                                        mb
                                                                                                                                .tupleOf())))))))))),
                                        mb.bind(
                                                "do_log_fn",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("do_log"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("do_log_fn"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "arg",
                                                                        mb.field(mb.scopeUse("iface"), mb.str("type"))),
                                                                new TestMb.RecordPair(
                                                                        "ret",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesOption(),
                                                                                                mb.evalType()),
                                                                                        mb.str("none")),
                                                                                mb.tupleOf())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.call(
                                                                                        mb.log(),
                                                                                        mb.call(
                                                                                                mb.field(
                                                                                                        mb.lower(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "iface"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "type"))),
                                                                                                        mb.str(
                                                                                                                "to_string")),
                                                                                                mb.scopeUse(
                                                                                                        "arg")))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "do_log",
                                                                mb.field(mb.scopeUse("do_log_fn"), mb.str("func"))),
                                                        new TestMb.RecordPair(
                                                                "upper",
                                                                mb.field(mb.scopeUse("ndr"), mb.str("new"))))))))),
                        mb.bind("do_log", mb.field(mb.scopeUse("defs"), mb.str("do_log"))),
                        mb.bind("make_upper", mb.field(mb.scopeUse("defs"), mb.str("upper"))),
                        mb.call(mb.scopeUse("do_log"), mb.call(mb.scopeUse("make_upper"), mb.str("hello"))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> ok = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e ->
                throw new AssertionError("compile failed: " + res);
        }
        assertEquals(1, ok.value().logs().size());
        assertEquals("HELLO", ok.value().logs().get(0).message());
    }

    @Test
    public void test00920InterfaceWrongMapValueType() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(mb.bind(
                        "make_upper",
                        mb.lower(mb.seqOf(
                                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                mb.bind(
                                        "iface",
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("interface")), mb.tupleOf())),
                                mb.call(
                                        mb.field(mb.scopeUse("iface"), mb.str("func")),
                                        mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair("name", mb.str("to_string")),
                                                        new TestMb.RecordPair("arg", mb.tupleOf()),
                                                        new TestMb.RecordPair(
                                                                "ret", mb.optSomeType(mb.typesString()))))),
                                mb.bind("ndr", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("upper"))),
                                mb.call(mb.field(mb.scopeUse("ndr"), mb.str("set_inner")), mb.typesString()),
                                mb.call(
                                        mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                        mb.tupleOf(
                                                mb.field(mb.scopeUse("iface"), mb.str("type")),
                                                mb.call(
                                                        mb.field(
                                                                mb.call(
                                                                        mb.typesMap(),
                                                                        mb.tupleOf(mb.typesString(), mb.typesBool())),
                                                                mb.str("new")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair(
                                                                                "to_string",
                                                                                mb.stage(
                                                                                        mb.call(
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "inner"),
                                                                                                        mb.str(
                                                                                                                "toUpperCase")),
                                                                                                mb.tupleOf())))))))),
                                mb.field(mb.scopeUse("ndr"), mb.str("new"))))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o ->
                throw new AssertionError("expected compile error but got: " + res);
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {}
        }
    }
}
