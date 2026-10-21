package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0020DefineObjectTest {
    @Test
    public void test00800DefineData() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "make_upper",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "ndr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("upper"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("inner", mb.typesString()),
                                                                new TestMb.RecordPair(
                                                                        "description", mb.optNoneString())))),
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
                                                                                                                                "value",
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
                                        mb.bind(
                                                "mnfr",
                                                mb.call(
                                                        mb.field(mb.scopeUse("ndr"), mb.str("func")),
                                                        mb.str("to_string"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("mnfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesString())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.call(
                                                                                        mb.field(
                                                                                                mb.scopeUse("inner"),
                                                                                                mb.str("toUpperCase")),
                                                                                        mb.tupleOf())))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.field(mb.scopeUse("ndr"), mb.str("new")))))),
                        mb.bind("val", mb.call(mb.scopeUse("make_upper"), mb.str("hello"))),
                        mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("val"), mb.str("to_string")), mb.tupleOf())))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> resOk = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals("HELLO", ok.logs().get(0).message());
    }
}
