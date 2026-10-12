package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0160EvenOddTest {
    private static SourceExpr ifaceMethods(TestMb mb, TList<TestMb.RecordPair> methods) {
        var staged = new TList<TestMb.RecordPair>(methods.size());
        for (var m : methods) {
            staged.add(new TestMb.RecordPair(m.key(), mb.stage(m.value())));
        }
        return mb.call(
                mb.field(mb.call(mb.typesMap(), mb.tupleOf(mb.typesConstkey(), mb.typesExpr())), mb.str("new")),
                mb.recordOf(staged));
    }

    private static SourceExpr unwrapCv(TestMb mb, SourceExpr cv) {
        return mb.call(
                mb.evalCodevalue(),
                mb.recordOf(TList.of(
                        new TestMb.RecordPair("code", mb.field(cv, mb.str("code"))),
                        new TestMb.RecordPair(
                                "value",
                                mb.call(
                                        mb.field(
                                                mb.call(
                                                        mb.mustReinterpret(),
                                                        mb.tupleOf(cvValue(mb, cv), mb.lower(mb.scopeUse("vb_type")))),
                                                mb.str("get_wrapped")),
                                        mb.tupleOf())))));
    }

    private static SourceExpr cvValue(TestMb mb, SourceExpr cv) {
        return mb.field(mb.call(mb.field(cv, mb.str("value")), mb.tupleOf()), mb.str("value"));
    }

    private static SourceExpr delegateNull(TestMb mb, SourceExpr underlying, SourceExpr aspectRef, String methodName) {
        return mb.call(
                mb.field(aspectRef, mb.str(methodName)),
                mb.tupleOf(
                        mb.field(
                                mb.call(
                                        mb.evalAspectLookup(),
                                        mb.tupleOf(underlying, aspectRef, mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                                mb.str("value")),
                        mb.recordOf(TList.of(
                                new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                                new TestMb.RecordPair("cv", mb.field(mb.scopeUse("arg"), mb.str("cv")))))));
    }

    private static SourceExpr delegateNullXxOnly(
            TestMb mb, SourceExpr underlying, SourceExpr aspectRef, String methodName) {
        return mb.call(
                mb.field(aspectRef, mb.str(methodName)),
                mb.tupleOf(
                        mb.field(
                                mb.call(mb.evalAspectLookup(), mb.tupleOf(underlying, aspectRef, mb.scopeUse("arg"))),
                                mb.str("value")),
                        mb.scopeUse("arg")));
    }

    private static SourceExpr castFromPassthrough(TestMb mb) {
        return mb.call(
                mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("ok")),
                mb.field(mb.scopeUse("arg"), mb.str("cv")));
    }

    private static SourceExpr evenOddAddWrapped(TestMb mb, SourceExpr parity) {
        return mb.call(
                mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("ok")),
                mb.call(
                        mb.evalCodevalue(),
                        mb.recordOf(TList.of(
                                new TestMb.RecordPair("code", mb.field(mb.scopeUse("sum_cv_"), mb.str("code"))),
                                new TestMb.RecordPair(
                                        "value",
                                        mb.call(
                                                mb.lower(mb.scopeUse("even_value_new")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "type",
                                                                        mb.call(
                                                                                mb.lower(mb.scopeUse("even_type_new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "parity",
                                                                                                        parity))))),
                                                                new TestMb.RecordPair(
                                                                        "source",
                                                                        mb.call(
                                                                                mb.lower(mb.scopeUse("vs_expr")),
                                                                                mb.tupleOf())),
                                                                new TestMb.RecordPair(
                                                                        "wrapped",
                                                                        cvValue(mb, mb.scopeUse("sum_cv_")))))))))));
    }

    private static TList<SourceExpr> bindEvenOddType(TestMb mb, String name) {
        return bindEvenOddType(
                mb,
                name,
                mb.seqOf(mb.return_(mb.constEvalCode(mb.call(
                        mb.evalConstEval(),
                        mb.recordOf(TList.of(
                                new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                new TestMb.RecordPair("staged", mb.stage(mb.seqOf())))))))));
    }

    private static TList<SourceExpr> bindEvenOddType(TestMb mb, String name, SourceExpr dropBody) {
        return TList.of(mb.bind(
                "even_type",
                mb.lower(mb.seqOf(
                        mb.bind(
                                "even_type_new",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "vs",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                                        mb.bind(
                                                "vs_expr",
                                                mb.call(
                                                        mb.field(mb.scopeUse("vs"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("expr")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.tupleOf()))))),
                                        mb.bind(
                                                "vs_binding",
                                                mb.call(
                                                        mb.field(mb.scopeUse("vs"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("binding")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref",
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "int",
                                                                                                        mb
                                                                                                                .evalBinding()),
                                                                                                new TestMb.RecordPair(
                                                                                                        "state",
                                                                                                        mb.recordOf(
                                                                                                                TList
                                                                                                                        .of(
                                                                                                                                new TestMb
                                                                                                                                        .RecordPair(
                                                                                                                                        "parity",
                                                                                                                                        mb
                                                                                                                                                .typesLong()),
                                                                                                                                new TestMb
                                                                                                                                        .RecordPair(
                                                                                                                                        "setter",
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .typesOption(),
                                                                                                                                                        mb
                                                                                                                                                                .builtin(
                                                                                                                                                                        "eval",
                                                                                                                                                                        "binding_setter"))))))))))))),
                                        mb.bind(
                                                "u",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str(name))),
                                        mb.call(
                                                mb.field(mb.scopeUse("u"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "inner",
                                                                        mb.recordOf(
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "parity",
                                                                                                mb.typesLong())))),
                                                                new TestMb.RecordPair(
                                                                        "description", mb.optNoneString())))),
                                        mb.bind(
                                                "u_gp_method",
                                                mb.call(
                                                        mb.field(mb.scopeUse("u"), mb.str("func")),
                                                        mb.str("get_parity"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("u_gp_method"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesLong())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.seqOf(
                                                                                        mb.return_(
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "inner"),
                                                                                                        mb.str(
                                                                                                                "parity"))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectInitialBind()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "initial_bind",
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesEresult(),
                                                                                                                        mb
                                                                                                                                .evalBinding()),
                                                                                                                mb.str(
                                                                                                                        "ok")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))))))))),
                                        mb.bind(
                                                "vb",
                                                mb.call(
                                                        mb.field(mb.scopeUse("d"), mb.str("object")),
                                                        mb.str("EvenOddValue"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("vb"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "inner",
                                                                        mb.recordOf(
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "type",
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "u"),
                                                                                                        mb.str(
                                                                                                                "type"))),
                                                                                        new TestMb.RecordPair(
                                                                                                "source",
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "vs"),
                                                                                                        mb.str(
                                                                                                                "type"))),
                                                                                        new TestMb.RecordPair(
                                                                                                "wrapped",
                                                                                                mb.evalValue())))),
                                                                new TestMb.RecordPair(
                                                                        "description", mb.optNoneString())))),
                                        mb.bind(
                                                "vb_gw_method",
                                                mb.call(
                                                        mb.field(mb.scopeUse("vb"), mb.str("func")),
                                                        mb.str("get_wrapped"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("vb_gw_method"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.evalValue())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.seqOf(
                                                                                        mb.return_(
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "inner"),
                                                                                                        mb.str(
                                                                                                                "wrapped"))))))))),
                                        mb.bind(
                                                "vb_gs_method",
                                                mb.call(
                                                        mb.field(mb.scopeUse("vb"), mb.str("func")),
                                                        mb.str("get_source"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("vb_gs_method"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret",
                                                                        mb.optSomeType(
                                                                                mb.field(
                                                                                        mb.scopeUse("vs"),
                                                                                        mb.str("type")))),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.seqOf(
                                                                                        mb.return_(
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "inner"),
                                                                                                        mb.str(
                                                                                                                "source"))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("vb"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("iface_ref", mb.evalValue()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "type",
                                                                                                mb.seqOf(
                                                                                                        mb.return_(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "inner"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "type"))))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("vb"), mb.str("impl_interface")),
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
                                                                                                delegateNull(
                                                                                                        mb,
                                                                                                        mb.typesLong(),
                                                                                                        mb
                                                                                                                .evalAspectConsumeDynamic(),
                                                                                                        "to_dynamic")),
                                                                                        new TestMb.RecordPair(
                                                                                                "jvm_class",
                                                                                                delegateNullXxOnly(
                                                                                                        mb,
                                                                                                        mb.typesLong(),
                                                                                                        mb
                                                                                                                .evalAspectConsumeDynamic(),
                                                                                                        "jvm_class")))))))),
                                        mb.bind(
                                                "bb",
                                                mb.call(
                                                        mb.field(mb.scopeUse("d"), mb.str("object")),
                                                        mb.str("EvenOddBinding"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("bb"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "inner",
                                                                        mb.recordOf(
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "int",
                                                                                                mb.evalBinding()),
                                                                                        new TestMb.RecordPair(
                                                                                                "state",
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "parity",
                                                                                                                        mb
                                                                                                                                .typesLong()),
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "setter",
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .typesOption(),
                                                                                                                                        mb
                                                                                                                                                .builtin(
                                                                                                                                                        "eval",
                                                                                                                                                        "binding_setter"))))))))),
                                                                new TestMb.RecordPair(
                                                                        "description", mb.optNoneString())))),
                                        mb.bind(
                                                "gp_method",
                                                mb.call(
                                                        mb.field(mb.scopeUse("bb"), mb.str("func")),
                                                        mb.str("get_parity"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("gp_method"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesLong())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.seqOf(
                                                                                        mb.return_(
                                                                                                mb.field(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "inner"),
                                                                                                                mb.str(
                                                                                                                        "state")),
                                                                                                        mb.str(
                                                                                                                "parity"))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("bb"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectInitialBind()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "initial_bind",
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesEresult(),
                                                                                                                        mb
                                                                                                                                .evalBinding()),
                                                                                                                mb.str(
                                                                                                                        "ok")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("bb"), mb.str("impl_interface")),
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
                                                                                                delegateNull(
                                                                                                        mb,
                                                                                                        mb.typesLong(),
                                                                                                        mb
                                                                                                                .evalAspectConsumeDynamic(),
                                                                                                        "to_dynamic")),
                                                                                        new TestMb.RecordPair(
                                                                                                "jvm_class",
                                                                                                delegateNullXxOnly(
                                                                                                        mb,
                                                                                                        mb.typesLong(),
                                                                                                        mb
                                                                                                                .evalAspectConsumeDynamic(),
                                                                                                        "jvm_class")))))))),
                                        mb.bind("even_binding_new", mb.field(mb.scopeUse("bb"), mb.str("new"))),
                                        mb.bind(
                                                "iset",
                                                mb.call(
                                                        mb.field(mb.scopeUse("d"), mb.str("object")),
                                                        mb.str("EvenOddIntSetter"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("iset"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "inner",
                                                                        mb.recordOf(
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "outer",
                                                                                                mb.builtin(
                                                                                                        "eval",
                                                                                                        "binding_setter")),
                                                                                        new TestMb.RecordPair(
                                                                                                "parity",
                                                                                                mb.typesLong())))),
                                                                new TestMb.RecordPair(
                                                                        "description", mb.optNoneString())))),
                                        mb.call(
                                                mb.field(mb.scopeUse("iset"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref",
                                                                        mb.builtin("eval", "binding_setter")),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "set",
                                                                                                mb.seqOf(
                                                                                                        mb.return_(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .builtin(
                                                                                                                                                        "eval",
                                                                                                                                                        "binding_setter"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "set")),
                                                                                                                        mb
                                                                                                                                .tupleOf(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "inner"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "outer")),
                                                                                                                                        mb
                                                                                                                                                .recordOf(
                                                                                                                                                        TList
                                                                                                                                                                .of(
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "xx",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "xx"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "loc",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "loc"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "new_binding",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                "even_binding_new")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                TList
                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                        "int",
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "new_binding"))),
                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                        "state",
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                "parity",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                "inner"),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                "parity"))),
                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                "setter",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .typesOption(),
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .builtin(
                                                                                                                                                                                                                                                                                                                                                "eval",
                                                                                                                                                                                                                                                                                                                                                "binding_setter")),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                "some")),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                "inner"),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                "outer"))))))))))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "drop_modified",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "drop_modified")))))))))))))))),
                                        mb.bind("int_setter_new", mb.field(mb.scopeUse("iset"), mb.str("new"))),
                                        mb.bind("bb_type", mb.field(mb.scopeUse("bb"), mb.str("type"))),
                                        mb.bind("even_value_new", mb.field(mb.scopeUse("vb"), mb.str("new"))),
                                        mb.bind("even_type_new", mb.field(mb.scopeUse("u"), mb.str("new"))),
                                        mb.bind("u_type", mb.field(mb.scopeUse("u"), mb.str("type"))),
                                        mb.bind("vb_type", mb.field(mb.scopeUse("vb"), mb.str("type"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("bb"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("iface_ref", mb.evalBinding()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "drop",
                                                                                                mb.bindingDropViaUse()),
                                                                                        new TestMb.RecordPair(
                                                                                                "use",
                                                                                                mb.seqOf(
                                                                                                        mb.set(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .field(
                                                                                                                                                mb
                                                                                                                                                        .scopeUse(
                                                                                                                                                                "inner"),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                "state")),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "setter")),
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .typesOption(),
                                                                                                                                                        mb
                                                                                                                                                                .builtin(
                                                                                                                                                                        "eval",
                                                                                                                                                                        "binding_setter")),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "some")),
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "setter")))),
                                                                                                        mb.bind(
                                                                                                                "int_use_cv_",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .evalBinding(),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "use")),
                                                                                                                        mb
                                                                                                                                .tupleOf(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "inner"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "int")),
                                                                                                                                        mb
                                                                                                                                                .recordOf(
                                                                                                                                                        TList
                                                                                                                                                                .of(
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "xx",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "xx"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "loc",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "loc"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "setter",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                "int_setter_new")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                TList
                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                        "outer",
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "setter"))),
                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                        "parity",
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "inner"),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "state")),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "parity"))))))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "code",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "code")))))))),
                                                                                                        mb.return_(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .evalCodevalue(),
                                                                                                                        mb
                                                                                                                                .recordOf(
                                                                                                                                        TList
                                                                                                                                                .of(
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "code",
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "int_use_cv_"),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "code"))),
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "value",
                                                                                                                                                                mb
                                                                                                                                                                        .call(
                                                                                                                                                                                mb
                                                                                                                                                                                        .lower(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "even_value_new")),
                                                                                                                                                                                mb
                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                TList
                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                        "type",
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .lower(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                        "even_type_new")),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                "parity",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                "inner"),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                "state")),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                "parity"))))))),
                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                        "source",
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .lower(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                        "vs_binding")),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                "int",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                "inner"),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                "int"))),
                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                "state",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                "inner"),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                "state"))))))),
                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                        "wrapped",
                                                                                                                                                                                                                        cvValue(
                                                                                                                                                                                                                                mb,
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                "int_use_cv_"))))))))))))),
                                                                                        new TestMb.RecordPair(
                                                                                                "fork",
                                                                                                mb.seqOf(
                                                                                                        mb.return_(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .lower(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "even_binding_new")),
                                                                                                                        mb
                                                                                                                                .recordOf(
                                                                                                                                        TList
                                                                                                                                                .of(
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "int",
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "inner"),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "int"))),
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "state",
                                                                                                                                                                mb
                                                                                                                                                                        .recordOf(
                                                                                                                                                                                TList
                                                                                                                                                                                        .of(
                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                        "parity",
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                        "inner"),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                        "state")),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                        "parity"))),
                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                        "setter",
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                        "inner"),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                        "state")),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                        "setter")))))))))))),
                                                                                        new TestMb.RecordPair(
                                                                                                "merge",
                                                                                                mb.labeled(
                                                                                                        "merge_res",
                                                                                                        mb.seqOf(
                                                                                                                mb.bind(
                                                                                                                        "initial_",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "inner"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "state")),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "parity"))),
                                                                                                                mb
                                                                                                                        .foreach(
                                                                                                                                mb
                                                                                                                                        .field(
                                                                                                                                                mb
                                                                                                                                                        .scopeUse(
                                                                                                                                                                "arg"),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                "others")),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "b"),
                                                                                                                                mb
                                                                                                                                        .ifOf(
                                                                                                                                                TList
                                                                                                                                                        .of(
                                                                                                                                                                new TestMb
                                                                                                                                                                        .Branch(
                                                                                                                                                                        mb
                                                                                                                                                                                .operatorEq(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .call(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .mustReinterpret(),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                        "b"),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .lower(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "bb_type")))),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                        "get_parity")),
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .tupleOf()),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                        "initial_")),
                                                                                                                                                                        mb
                                                                                                                                                                                .seqOf())),
                                                                                                                                                Opt
                                                                                                                                                        .some(
                                                                                                                                                                mb
                                                                                                                                                                        .return_(
                                                                                                                                                                                "merge_res",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .typesEresult(),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .evalBinding()),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                "err")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                "loc")),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                "evenodd merge:"
                                                                                                                                                                                                                                        + " branch parities"
                                                                                                                                                                                                                                        + " differ"))))))),
                                                                                                                mb
                                                                                                                        .return_(
                                                                                                                                mb
                                                                                                                                        .call(
                                                                                                                                                mb
                                                                                                                                                        .field(
                                                                                                                                                                mb
                                                                                                                                                                        .call(
                                                                                                                                                                                mb
                                                                                                                                                                                        .typesEresult(),
                                                                                                                                                                                mb
                                                                                                                                                                                        .evalBinding()),
                                                                                                                                                                mb
                                                                                                                                                                        .str(
                                                                                                                                                                                "ok")),
                                                                                                                                                mb
                                                                                                                                                        .scopeUse(
                                                                                                                                                                "outer")))))))))))),
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
                                                                                                "is_equal",
                                                                                                mb.panic(
                                                                                                        mb.str(
                                                                                                                "EvenOdd is_equal: not implemented"))),
                                                                                        new TestMb.RecordPair(
                                                                                                "can_cast_from",
                                                                                                mb.seqOf(
                                                                                                        mb.return_(
                                                                                                                mb
                                                                                                                        .operatorEq(
                                                                                                                                mb
                                                                                                                                        .num(
                                                                                                                                                1.0),
                                                                                                                                mb
                                                                                                                                        .num(
                                                                                                                                                1.0))))),
                                                                                        new TestMb.RecordPair(
                                                                                                "cast_from",
                                                                                                mb.seqOf(
                                                                                                        mb.bind(
                                                                                                                "val_opt_",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "cv")),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "value")),
                                                                                                                        mb
                                                                                                                                .tupleOf())),
                                                                                                        mb.return_(
                                                                                                                mb.ifOf(
                                                                                                                        TList
                                                                                                                                .of(
                                                                                                                                        new TestMb
                                                                                                                                                .Branch(
                                                                                                                                                mb
                                                                                                                                                        .field(
                                                                                                                                                                mb
                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                "val_opt_"),
                                                                                                                                                                mb
                                                                                                                                                                        .str(
                                                                                                                                                                                "is_some")),
                                                                                                                                                mb
                                                                                                                                                        .seqOf(
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "val_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "val_opt_"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "value"))),
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "val_type_obj_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .evalValue(),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                "type")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "val_"))),
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "int_aspect_opt_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .evalAspectLookup(),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                "val_type_obj_"),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .evalAspectInt(),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                "xx"))))),
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "int_dynamic_opt_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .evalType(),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                "aspect")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                "val_type_obj_"),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                        "xx",
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "xx"))),
                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                        "key",
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .evalTypeLongDynamicIdentity())))))),
                                                                                                                                                                mb
                                                                                                                                                                        .return_(
                                                                                                                                                                                mb
                                                                                                                                                                                        .ifOf(
                                                                                                                                                                                                TList
                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                        .Branch(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                        "int_aspect_opt_"),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                        "is_some")),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .seqOf(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .bind(
                                                                                                                                                                                                                                                        "cast_cv_",
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .evalType(),
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                        "cast_from")),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .typesLong(),
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                "xx",
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                "xx"))),
                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                "loc",
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                "loc"))),
                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                "cv",
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                "cv"))))))),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "value"))),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .return_(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .typesEresult(),
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .typesCve()),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                        "ok")),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .evalCodevalue(),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                "code",
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                "cast_cv_"),
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                "code"))),
                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                "value",
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                "even_value_new")),
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                        "type",
                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                .lower(
                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                        "even_type_new")),
                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                                                                                "parity",
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .operatorMod(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .evalAspectInt(),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "get")),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "int_aspect_opt_"),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "value")),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "val_"))),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .as(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .num(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                2.0),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .typesLong()))))))),
                                                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                        "source",
                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                .lower(
                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                        "vs_expr")),
                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                .tupleOf())),
                                                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                        "wrapped",
                                                                                                                                                                                                                                                                                                                                                                                        cvValue(
                                                                                                                                                                                                                                                                                                                                                                                                mb,
                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                "cast_cv_")))))))))))))),
                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                        .Branch(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                        "int_dynamic_opt_"),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                        "is_some")),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .typesEresult(),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .typesCve()),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "err")),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "loc")),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "cast: cannot cast a dynamic int to"
                                                                                                                                                                                                                                                                                + " evenodd; parity unknown"))))),
                                                                                                                                                                                                Opt
                                                                                                                                                                                                        .some(
                                                                                                                                                                                                                castFromPassthrough(
                                                                                                                                                                                                                        mb))))))),
                                                                                                                        Opt
                                                                                                                                .some(
                                                                                                                                        castFromPassthrough(
                                                                                                                                                mb)))))),
                                                                                        new TestMb.RecordPair(
                                                                                                "desc",
                                                                                                mb.seqOf(
                                                                                                        mb.return_(
                                                                                                                mb.str(
                                                                                                                        "evenodd")))),
                                                                                        new TestMb.RecordPair(
                                                                                                "aspect",
                                                                                                mb.ifOf(
                                                                                                        TList.of(
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .operatorEq(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "key")),
                                                                                                                                        mb
                                                                                                                                                .evalAspectBindId()),
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .typesOption(),
                                                                                                                                                                        mb
                                                                                                                                                                                .typesAspect()),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "some")),
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "outer"))),
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .operatorEq(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "key")),
                                                                                                                                        mb
                                                                                                                                                .evalAspectBindDynamicId()),
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .typesOption(),
                                                                                                                                                                        mb
                                                                                                                                                                                .typesAspect()),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "some")),
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "outer"))),
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .operatorEq(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "key")),
                                                                                                                                        mb
                                                                                                                                                .evalAspectConsumeDynamicId()),
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .typesOption(),
                                                                                                                                                                        mb
                                                                                                                                                                                .typesAspect()),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "some")),
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "outer"))),
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .operatorEq(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "key")),
                                                                                                                                        mb
                                                                                                                                                .evalAspectDropDynamicId()),
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .typesOption(),
                                                                                                                                                                        mb
                                                                                                                                                                                .typesAspect()),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "some")),
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "outer"))),
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .operatorEq(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "key")),
                                                                                                                                        mb
                                                                                                                                                .evalAspectSetId()),
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .typesOption(),
                                                                                                                                                                        mb
                                                                                                                                                                                .typesAspect()),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "some")),
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "outer"))),
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .operatorEq(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "key")),
                                                                                                                                        mb
                                                                                                                                                .evalOperatorAddId()),
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .typesOption(),
                                                                                                                                                                        mb
                                                                                                                                                                                .typesAspect()),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "some")),
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "outer")))),
                                                                                                        Opt.some(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .typesOption(),
                                                                                                                                                        mb
                                                                                                                                                                .typesAspect()),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "none")),
                                                                                                                        mb
                                                                                                                                .tupleOf())))),
                                                                                        new TestMb.RecordPair(
                                                                                                "can_cast_to",
                                                                                                mb.seqOf(
                                                                                                        mb.return_(
                                                                                                                mb.bool(
                                                                                                                        false)))),
                                                                                        new TestMb.RecordPair(
                                                                                                "cast_to",
                                                                                                mb.panic(
                                                                                                        mb.str(
                                                                                                                "EvenOdd cast_to: not implemented"))))))))),
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
                                                                                                mb.seqOf(
                                                                                                        mb.bind(
                                                                                                                "int_bind_er",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .evalAspectBind(),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "bind")),
                                                                                                                        mb
                                                                                                                                .tupleOf(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .evalAspectLookup(),
                                                                                                                                                                        mb
                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .typesLong(),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .evalAspectBind(),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .field(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                        "xx")))),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "value")),
                                                                                                                                        mb
                                                                                                                                                .recordOf(
                                                                                                                                                        TList
                                                                                                                                                                .of(
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "xx",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "xx"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "loc",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "loc"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "cv",
                                                                                                                                                                                unwrapCv(
                                                                                                                                                                                        mb,
                                                                                                                                                                                        mb
                                                                                                                                                                                                .field(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                        "cv"))))))))),
                                                                                                        mb.bind(
                                                                                                                "int_bind_res",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "int_bind_er"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "value"))),
                                                                                                        mb.bind(
                                                                                                                "int_code",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "int_bind_res"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "code"))),
                                                                                                        mb.bind(
                                                                                                                "int_binding",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "int_bind_res"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "binding"))),
                                                                                                        mb.return_(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .typesEresult(),
                                                                                                                                                        mb
                                                                                                                                                                .typesBindResult()),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "ok")),
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .evalBindResult(),
                                                                                                                                        mb
                                                                                                                                                .recordOf(
                                                                                                                                                        TList
                                                                                                                                                                .of(
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "code",
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "int_code")),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "binding",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                "even_binding_new")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                TList
                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                        "int",
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                        "int_binding")),
                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                        "state",
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                "parity",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                "inner"),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                "parity"))),
                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                "setter",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .typesOption(),
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .builtin(
                                                                                                                                                                                                                                                                                                                                                "eval",
                                                                                                                                                                                                                                                                                                                                                "binding_setter")),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                "none")),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .tupleOf())))))))))))))))))))))),
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
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .evalAspectBindDynamic(),
                                                                                                                mb.str(
                                                                                                                        "bind_dynamic")),
                                                                                                        mb.tupleOf(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .call(
                                                                                                                                                mb
                                                                                                                                                        .evalAspectLookup(),
                                                                                                                                                mb
                                                                                                                                                        .tupleOf(
                                                                                                                                                                mb
                                                                                                                                                                        .typesLong(),
                                                                                                                                                                mb
                                                                                                                                                                        .evalAspectBindDynamic(),
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "arg"),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "xx")))),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "value")),
                                                                                                                mb
                                                                                                                        .recordOf(
                                                                                                                                TList
                                                                                                                                        .of(
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "xx",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "xx"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "loc",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "loc"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "cv",
                                                                                                                                                        unwrapCv(
                                                                                                                                                                mb,
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "arg"),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "cv"))))))))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectDropDynamic()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "drop", dropBody))))))),
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
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                mb.str(
                                                                                                                        "to_dynamic")),
                                                                                                        mb.tupleOf(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .call(
                                                                                                                                                mb
                                                                                                                                                        .evalAspectLookup(),
                                                                                                                                                mb
                                                                                                                                                        .tupleOf(
                                                                                                                                                                mb
                                                                                                                                                                        .typesLong(),
                                                                                                                                                                mb
                                                                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "arg"),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "xx")))),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "value")),
                                                                                                                mb
                                                                                                                        .recordOf(
                                                                                                                                TList
                                                                                                                                        .of(
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "xx",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "xx"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "loc",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "loc"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "cv",
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .evalCodevalue(),
                                                                                                                                                                        mb
                                                                                                                                                                                .recordOf(
                                                                                                                                                                                        TList
                                                                                                                                                                                                .of(
                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                "code",
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                "cv")),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                "code"))),
                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                "value",
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .mustReinterpret(),
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                                                                cvValue(
                                                                                                                                                                                                                                                                                        mb,
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                        "cv"))),
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                "vb_type")))),
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                "get_wrapped")),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .tupleOf()))))))))))),
                                                                                        new TestMb.RecordPair(
                                                                                                "jvm_class",
                                                                                                delegateNullXxOnly(
                                                                                                        mb,
                                                                                                        mb.typesLong(),
                                                                                                        mb
                                                                                                                .evalAspectConsumeDynamic(),
                                                                                                        "jvm_class")))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("iface_ref", mb.evalAspectSet()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "set",
                                                                                                mb.seqOf(
                                                                                                        mb.bind(
                                                                                                                "conv_er_",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .evalType(),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "cast_from")),
                                                                                                                        mb
                                                                                                                                .tupleOf(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "outer"),
                                                                                                                                        mb
                                                                                                                                                .recordOf(
                                                                                                                                                        TList
                                                                                                                                                                .of(
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "xx",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "xx"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "loc",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "loc"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "cv",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "value")))))))),
                                                                                                        mb.return_(
                                                                                                                mb.ifOf(
                                                                                                                        TList
                                                                                                                                .of(
                                                                                                                                        new TestMb
                                                                                                                                                .Branch(
                                                                                                                                                mb
                                                                                                                                                        .field(
                                                                                                                                                                mb
                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                "conv_er_"),
                                                                                                                                                                mb
                                                                                                                                                                        .str(
                                                                                                                                                                                "has_errors")),
                                                                                                                                                mb
                                                                                                                                                        .scopeUse(
                                                                                                                                                                "conv_er_"))),
                                                                                                                        Opt
                                                                                                                                .some(
                                                                                                                                        mb
                                                                                                                                                .seqOf(
                                                                                                                                                        mb
                                                                                                                                                                .bind(
                                                                                                                                                                        "conv_cv_",
                                                                                                                                                                        mb
                                                                                                                                                                                .field(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                        "conv_er_"),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .str(
                                                                                                                                                                                                        "value"))),
                                                                                                                                                        mb
                                                                                                                                                                .bind(
                                                                                                                                                                        "rhs_parity_",
                                                                                                                                                                        mb
                                                                                                                                                                                .call(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .field(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .mustReinterpret(),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .evalValue(),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "type")),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                        "conv_cv_"),
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                        "value")),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .tupleOf()),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "value"))),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .lower(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                        "u_type")))),
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                        "get_parity")),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .tupleOf())),
                                                                                                                                                        mb
                                                                                                                                                                .return_(
                                                                                                                                                                        mb
                                                                                                                                                                                .seqOf(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .bind(
                                                                                                                                                                                                        "self_val_",
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "self")),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "value")),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .tupleOf()),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                        "value"))),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .bind(
                                                                                                                                                                                                        "src_",
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .mustReinterpret(),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "self_val_"),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .lower(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                        "vb_type")))),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                        "get_source")),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .tupleOf())),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .return_(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .matchOf(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                        "src_"),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                        "sv"),
                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                .Branch(
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                "binding"),
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .seqOf(
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .bind(
                                                                                                                                                                                                                                                                                "new_binding_",
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                "even_binding_new")),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                        "int",
                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                        "sv"),
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                        "int"))),
                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                        "state",
                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                "parity",
                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                "rhs_parity_")),
                                                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                "setter",
                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                "sv"),
                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                "state")),
                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                "setter")))))))))),
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .builtin(
                                                                                                                                                                                                                                                                                                                "eval",
                                                                                                                                                                                                                                                                                                                "binding_setter"),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                "set")),
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                "sv"),
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                "state")),
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                "setter")),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                "value")),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                        "xx",
                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                        "xx"))),
                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                        "loc",
                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                        "loc"))),
                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                        "new_binding",
                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                        "new_binding_")),
                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                        "drop_modified",
                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                .as(
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .operatorEq(
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .num(
                                                                                                                                                                                                                                                                                                                                                                                        1.0),
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .num(
                                                                                                                                                                                                                                                                                                                                                                                        2.0)),
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .typesBool())))))),
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .return_(
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .evalAspectSet(),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                "set")),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .evalAspectLookup(),
                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .typesLong(),
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .evalAspectSet(),
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                "xx")))),
                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                "value")),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                        "xx",
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                        "xx"))),
                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                        "loc",
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                        "loc"))),
                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                        "self",
                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                .evalBinding(),
                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                                        "use")),
                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                        "sv"),
                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                                                        "int")),
                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                .recordOf(
                                                                                                                                                                                                                                                                                                                                                                                                        TList
                                                                                                                                                                                                                                                                                                                                                                                                                .of(
                                                                                                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                                                                "xx",
                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                "xx"))),
                                                                                                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                                                                "loc",
                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                "loc"))),
                                                                                                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                                                                "setter",
                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "int_setter_new")),
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "outer",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "sv"),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "state")),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "setter")),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "value"))),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "parity",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "sv"),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "state")),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        "parity"))))))),
                                                                                                                                                                                                                                                                                                                                                                                                                        new TestMb
                                                                                                                                                                                                                                                                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                                                                                                                                                                                                                                                                "code",
                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                "self")),
                                                                                                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                "code")))))))),
                                                                                                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                                                                                                        "value",
                                                                                                                                                                                                                                                                                                                                                        unwrapCv(
                                                                                                                                                                                                                                                                                                                                                                mb,
                                                                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                                                                "conv_cv_"))))))))))),
                                                                                                                                                                                                                        Opt
                                                                                                                                                                                                                                .some(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .typesEresult(),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .typesCve()),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "err")),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                        "loc")),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "set: cannot set a non-binding evenodd value")))))))))))))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalOperatorAdd()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "add",
                                                                                                mb.seqOf(
                                                                                                        mb.bind(
                                                                                                                "sum_er_",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .evalOperatorAdd(),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "add")),
                                                                                                                        mb
                                                                                                                                .tupleOf(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .evalAspectLookup(),
                                                                                                                                                                        mb
                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .typesLong(),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .evalOperatorAdd(),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .field(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                        "xx")))),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "value")),
                                                                                                                                        mb
                                                                                                                                                .recordOf(
                                                                                                                                                        TList
                                                                                                                                                                .of(
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "xx",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "xx"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "loc",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "loc"))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "base",
                                                                                                                                                                                unwrapCv(
                                                                                                                                                                                        mb,
                                                                                                                                                                                        mb
                                                                                                                                                                                                .field(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                        "base")))),
                                                                                                                                                                        new TestMb
                                                                                                                                                                                .RecordPair(
                                                                                                                                                                                "reference",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .evalType(),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                "cast_from")),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .typesLong(),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                        "xx",
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                        "xx"))),
                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                        "loc",
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                        "loc"))),
                                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                                        "cv",
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                        "reference"))))))),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "value")))))))),
                                                                                                        mb.bind(
                                                                                                                "sum_cv_",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "sum_er_"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "value"))),
                                                                                                        mb.bind(
                                                                                                                "ref_opt_",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "arg"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "reference")),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "value")),
                                                                                                                        mb
                                                                                                                                .tupleOf())),
                                                                                                        mb.return_(
                                                                                                                mb.ifOf(
                                                                                                                        TList
                                                                                                                                .of(
                                                                                                                                        new TestMb
                                                                                                                                                .Branch(
                                                                                                                                                mb
                                                                                                                                                        .field(
                                                                                                                                                                mb
                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                "ref_opt_"),
                                                                                                                                                                mb
                                                                                                                                                                        .str(
                                                                                                                                                                                "is_some")),
                                                                                                                                                mb
                                                                                                                                                        .seqOf(
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "ref_val_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "ref_opt_"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "value"))),
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "ref_type_obj_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .evalValue(),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                "type")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "ref_val_"))),
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "ref_int_aspect_opt_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .evalAspectLookup(),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                "ref_type_obj_"),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .evalAspectInt(),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                "xx"))))),
                                                                                                                                                                mb
                                                                                                                                                                        .bind(
                                                                                                                                                                                "ref_int_dynamic_opt_",
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .evalType(),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                "aspect")),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                "ref_type_obj_"),
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .recordOf(
                                                                                                                                                                                                                                TList
                                                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                        "xx",
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "xx"))),
                                                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                                                                        "key",
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .evalTypeLongDynamicIdentity())))))),
                                                                                                                                                                mb
                                                                                                                                                                        .return_(
                                                                                                                                                                                mb
                                                                                                                                                                                        .ifOf(
                                                                                                                                                                                                TList
                                                                                                                                                                                                        .of(
                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                        .Branch(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                        "ref_int_aspect_opt_"),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                        "is_some")),
                                                                                                                                                                                                                        evenOddAddWrapped(
                                                                                                                                                                                                                                mb,
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .operatorMod(
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .operatorAdd(
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                "inner"),
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                "parity")),
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .evalAspectInt(),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                "get")),
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .field(
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                                "ref_int_aspect_opt_"),
                                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                                                                                                                                "value")),
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                "ref_val_")))),
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .as(
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .num(
                                                                                                                                                                                                                                                                                2.0),
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .typesLong())))),
                                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                                        .Branch(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                        "ref_int_dynamic_opt_"),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                        "is_some")),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .typesEresult(),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .typesCve()),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "err")),
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                        "loc")),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "operator_add: reference is a dynamic"
                                                                                                                                                                                                                                                                                + " int; parity unknown"))))),
                                                                                                                                                                                                Opt
                                                                                                                                                                                                        .some(
                                                                                                                                                                                                                mb
                                                                                                                                                                                                                        .seqOf(
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .bind(
                                                                                                                                                                                                                                                "ref_evenodd_type_",
                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                        .call(
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .mustReinterpret(),
                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                "ref_type_obj_"),
                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                        .lower(
                                                                                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                                                                                                                "u_type"))))),
                                                                                                                                                                                                                                mb
                                                                                                                                                                                                                                        .return_(
                                                                                                                                                                                                                                                evenOddAddWrapped(
                                                                                                                                                                                                                                                        mb,
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .operatorMod(
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .operatorAdd(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                        "inner"),
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                        "parity")),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .call(
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                                                                                        "ref_evenodd_type_"),
                                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                                                                                        "get_parity")),
                                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                                .tupleOf())),
                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                .as(
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .num(
                                                                                                                                                                                                                                                                                                        2.0),
                                                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                                                .typesLong()))))))))))),
                                                                                                                        Opt
                                                                                                                                .some(
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .call(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .typesEresult(),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .typesCve()),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "err")),
                                                                                                                                                        mb
                                                                                                                                                                .tupleOf(
                                                                                                                                                                        mb
                                                                                                                                                                                .field(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .str(
                                                                                                                                                                                                        "loc")),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "operator_add: reference has no compile-time value")))))))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("iface_ref", mb.evalBinding()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        ifaceMethods(
                                                                                mb,
                                                                                TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                                "drop",
                                                                                                mb.bindingDropViaUse()),
                                                                                        new TestMb.RecordPair(
                                                                                                "use",
                                                                                                mb.matchOf(
                                                                                                        mb.call(
                                                                                                                mb
                                                                                                                        .evalCodevalueDynamic(),
                                                                                                                mb
                                                                                                                        .recordOf(
                                                                                                                                TList
                                                                                                                                        .of(
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "loc",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "loc"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "code",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "code"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "type",
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "outer"))))),
                                                                                                        mb.str("__cvd"),
                                                                                                        TList.of(
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .str(
                                                                                                                                        "ok"),
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "__cvd")),
                                                                                                                new TestMb
                                                                                                                        .Branch(
                                                                                                                        mb
                                                                                                                                .str(
                                                                                                                                        "errs"),
                                                                                                                        mb
                                                                                                                                .panic(
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "codevalue_dynamic failed")))),
                                                                                                        Opt.none())),
                                                                                        new TestMb.RecordPair(
                                                                                                "fork",
                                                                                                mb.scopeUse("outer")),
                                                                                        new TestMb.RecordPair(
                                                                                                "merge",
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesEresult(),
                                                                                                                        mb
                                                                                                                                .evalBinding()),
                                                                                                                mb.str(
                                                                                                                        "ok")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.scopeUse("even_type_new"))))),
                        mb.return_(mb.call(
                                mb.scopeUse("even_type_new"),
                                mb.recordOf(TList.of(new TestMb.RecordPair("parity", mb.num(0.0))))))))));
    }

    private static TList<SourceExpr> moduleSeq(TList<SourceExpr> prefix, SourceExpr... rest) {
        var all = new TList<SourceExpr>(prefix.size() + rest.length);
        for (var e : prefix) all.add(e);
        for (var e : rest) all.add(e);
        return all;
    }

    private static Compile.CompileResult compileOk(TMap<String, SourceModule> modules) {
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                throw new AssertionError("compile failed: " + res);
            }
        }
        return okRes.value();
    }

    @Test
    public void test00110DropCannotThrow() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd", mb.throw_(mb.str("drop boom"))),
                        mb.call(mb.log(), mb.str("defined")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "a throwing drop should fail compile: " + res);
        assertTrue(
                res.toString().contains("throw not allowed"),
                () -> "expected the throw-not-allowed gate to fire, got: " + res);
    }

    @Test
    public void test00120DropProducedCodeCannotThrow() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(
                                mb,
                                "EvenOdd",
                                mb.seqOf(mb.return_(mb.constEvalCode(mb.call(
                                        mb.evalConstEval(),
                                        mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                                        new TestMb.RecordPair(
                                                                "staged",
                                                                mb.stage(mb.throw_(mb.str("drop boom"))))))))))),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.call(mb.log(), mb.str("bound")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "a drop that produces a throw should fail compile: " + res);
        assertTrue(
                res.toString().contains("throw not allowed"),
                () -> "expected the throw-not-allowed gate to fire on the produced drop code, got: " + res);
    }

    @Test
    public void test00100DefineEvenOddType() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(bindEvenOddType(mb, "EvenOdd"), mb.call(mb.log(), mb.str("defined")))));
        var res = compileOk(modules);
        assertTrue(res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
    }

    @Test
    public void test00200CastAndBind() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.call(mb.log(), mb.str("bound")))));
        var res = compileOk(modules);
        assertTrue(res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
    }

    @Test
    public void test00300CastAndBindWithSet() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.set(mb.scopeUse("a"), mb.as(mb.num(4.0), mb.scopeUse("even_type"))),
                        mb.call(mb.log(), mb.str("set-done")))));
        compileOk(modules);
    }

    @Test
    public void test00400BindUseInLog() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.bind("b", mb.scopeUse("a")),
                        mb.call(mb.log(), mb.str("used")))));
        var res = compileOk(modules);
        assertTrue(res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
    }

    @Test
    public void test00500CastConstInt() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.call(mb.log(), mb.str("cast-const")))));
        var res = compileOk(modules);
        assertTrue(res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
    }

    @Test
    public void test00600AddConstInt() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.bind("b", mb.operatorAdd(mb.scopeUse("a"), mb.num(3.0))),
                        mb.call(mb.log(), mb.str("add-const")))));
        var res = compileOk(modules);
        assertTrue(res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
    }

    @Test
    public void test00700AddEvenOdd() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.bind("b", mb.as(mb.num(4.0), mb.scopeUse("even_type"))),
                        mb.bind("c", mb.operatorAdd(mb.scopeUse("a"), mb.scopeUse("b"))),
                        mb.call(mb.log(), mb.str("add-evenodd")))));
        var res = compileOk(modules);
        assertTrue(res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
    }

    @Test
    public void test00900SetConstInt() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.set(mb.scopeUse("a"), mb.num(4.0)),
                        mb.call(mb.log(), mb.str("set-const")))));
        var res = compileOk(modules);
        assertTrue(res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
    }

    @Test
    public void test01000SetDynamicIntErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.set(mb.scopeUse("a"), mb.as(mb.num(4.0), mb.typesLong())),
                        mb.call(mb.log(), mb.str("unreachable")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected compile error, got " + res);
        assertTrue(
                res.toString().contains("cast: cannot cast a dynamic int"),
                () -> "expected dynamic-int cast error from set, got " + res);
    }

    private static SourceExpr evenCast(TestMb mb, double v) {
        return mb.as(mb.num(v), mb.scopeUse("even_type"));
    }

    private static SourceExpr dynTrue(TestMb mb) {
        return mb.operatorEq(mb.as(mb.num(1.0), mb.typesLong()), mb.as(mb.num(1.0), mb.typesLong()));
    }

    private static void assertParityMergeError(TMap<String, SourceModule> modules) {
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected merge error, got " + res);
        assertTrue(
                res.toString().contains("evenodd merge: branch parities differ"),
                () -> "expected parity merge error, got " + res);
    }

    private static SourceExpr bindStrNum(TestMb mb) {
        return mb.bind(
                "strnum",
                mb.lower(mb.seqOf(
                        mb.bind("d2", mb.call(mb.define(), mb.tupleOf())),
                        mb.bind("builder", mb.call(mb.field(mb.scopeUse("d2"), mb.str("union")), mb.tupleOf())),
                        mb.bind(
                                "new_str",
                                mb.call(
                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                        mb.recordOf(TList.of(
                                                new TestMb.RecordPair("tag", mb.str("strtag")),
                                                new TestMb.RecordPair("type_ref", mb.typesString()))))),
                        mb.bind(
                                "new_num",
                                mb.call(
                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                        mb.recordOf(TList.of(
                                                new TestMb.RecordPair("tag", mb.str("numtag")),
                                                new TestMb.RecordPair("type_ref", mb.typesLong()))))),
                        mb.call(mb.field(mb.scopeUse("d2"), mb.str("finish")), mb.tupleOf()),
                        mb.return_(mb.recordOf(TList.of(
                                new TestMb.RecordPair("type", mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                new TestMb.RecordPair("num", mb.scopeUse("new_num"))))))));
    }

    private TMap<String, SourceModule> ifElseModule(TestMb mb, SourceExpr thenSet, SourceExpr elseSet) {
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.ifOf(TList.of(new TestMb.Branch(dynTrue(mb), thenSet)), Opt.some(elseSet)),
                        mb.call(mb.log(), mb.str("done")))));
        return modules;
    }

    @Test
    public void test01100IfElseSetConstCompatible() {
        var mb = new TestMb();
        compileOk(ifElseModule(mb, mb.set(mb.scopeUse("a"), mb.num(4.0)), mb.set(mb.scopeUse("a"), mb.num(6.0))));
    }

    @Test
    public void test01150IfElseSetConstIncompatible() {
        var mb = new TestMb();
        assertParityMergeError(
                ifElseModule(mb, mb.set(mb.scopeUse("a"), mb.num(4.0)), mb.set(mb.scopeUse("a"), mb.num(3.0))));
    }

    @Test
    public void test01200IfElseSetEvenOddCompatible() {
        var mb = new TestMb();
        compileOk(ifElseModule(
                mb, mb.set(mb.scopeUse("a"), evenCast(mb, 4.0)), mb.set(mb.scopeUse("a"), evenCast(mb, 6.0))));
    }

    @Test
    public void test01250IfElseSetEvenOddIncompatible() {
        var mb = new TestMb();
        assertParityMergeError(ifElseModule(
                mb, mb.set(mb.scopeUse("a"), evenCast(mb, 4.0)), mb.set(mb.scopeUse("a"), evenCast(mb, 3.0))));
    }

    @Test
    public void test01300IfElseAddConstCompatible() {
        var mb = new TestMb();
        compileOk(ifElseModule(
                mb,
                mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.num(2.0))),
                mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.num(4.0)))));
    }

    @Test
    public void test01350IfElseAddConstIncompatible() {
        var mb = new TestMb();
        assertParityMergeError(ifElseModule(
                mb,
                mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.num(2.0))),
                mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.num(3.0)))));
    }

    @Test
    public void test01400IfElseAddEvenOddCompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind("b", evenCast(mb, 4.0)),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        dynTrue(mb),
                                        mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.scopeUse("b"))))),
                                Opt.some(mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.scopeUse("b"))))),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test01450IfElseAddEvenOddIncompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind("b", evenCast(mb, 4.0)),
                        mb.bind("c", evenCast(mb, 3.0)),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        dynTrue(mb),
                                        mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.scopeUse("b"))))),
                                Opt.some(mb.set(mb.scopeUse("a"), mb.operatorAdd(mb.scopeUse("a"), mb.scopeUse("c"))))),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test01500IfNoElseSetCompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(dynTrue(mb), mb.set(mb.scopeUse("a"), mb.num(4.0)))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test01550IfNoElseSetIncompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(dynTrue(mb), mb.set(mb.scopeUse("a"), mb.num(3.0)))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test01600ForeachSetCompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesLong()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.)))),
                        mb.foreach(mb.scopeUse("my_list"), mb.str("e"), mb.set(mb.scopeUse("a"), mb.num(4.0))),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test01650ForeachSetIncompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesLong()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.)))),
                        mb.foreach(mb.scopeUse("my_list"), mb.str("e"), mb.set(mb.scopeUse("a"), mb.num(3.0))),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test01700TryCatchSetCompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.tryCatchFinally(
                                mb.set(mb.scopeUse("a"), mb.num(4.0)),
                                Opt.some(
                                        new TestMb.Catch("e", mb.typesString(), mb.set(mb.scopeUse("a"), mb.num(6.0)))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test01750TryCatchSetIncompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.tryCatchFinally(
                                mb.set(mb.scopeUse("a"), mb.num(4.0)),
                                Opt.some(
                                        new TestMb.Catch("e", mb.typesString(), mb.set(mb.scopeUse("a"), mb.num(3.0)))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test01800MatchSetCompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        bindStrNum(mb),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(
                                        new TestMb.Branch(mb.str("strtag"), mb.set(mb.scopeUse("a"), mb.num(4.0))),
                                        new TestMb.Branch(mb.str("numtag"), mb.set(mb.scopeUse("a"), mb.num(6.0)))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test01850MatchSetIncompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        bindStrNum(mb),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(
                                        new TestMb.Branch(mb.str("strtag"), mb.set(mb.scopeUse("a"), mb.num(4.0))),
                                        new TestMb.Branch(mb.str("numtag"), mb.set(mb.scopeUse("a"), mb.num(3.0)))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test00450CastDynamicIntErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.as(mb.num(2.0), mb.typesLong()), mb.scopeUse("even_type"))),
                        mb.call(mb.log(), mb.str("unreachable")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected compile error, got " + res);
        assertTrue(
                res.toString().contains("cast: cannot cast a dynamic int"),
                () -> "expected dynamic-int cast error, got " + res);
    }

    @Test
    public void test01900MatchFallbackSetCompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        bindStrNum(mb),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(new TestMb.Branch(mb.str("strtag"), mb.set(mb.scopeUse("a"), mb.num(4.0)))),
                                Opt.some(mb.set(mb.scopeUse("a"), mb.num(6.0)))),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test01950MatchFallbackSetIncompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        bindStrNum(mb),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(new TestMb.Branch(mb.str("strtag"), mb.set(mb.scopeUse("a"), mb.num(4.0)))),
                                Opt.some(mb.set(mb.scopeUse("a"), mb.num(3.0)))),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test02000NestedBranchSetCompatible() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesLong()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.)))),
                        mb.foreach(
                                mb.scopeUse("my_list"),
                                mb.str("e"),
                                mb.ifOf(
                                        TList.of(new TestMb.Branch(dynTrue(mb), mb.set(mb.scopeUse("a"), mb.num(4.0)))),
                                        Opt.some(mb.set(mb.scopeUse("a"), mb.num(6.0))))),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test02050NestedBranchSetIncompatibleInner() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesLong()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.)))),
                        mb.foreach(
                                mb.scopeUse("my_list"),
                                mb.str("e"),
                                mb.ifOf(
                                        TList.of(new TestMb.Branch(dynTrue(mb), mb.set(mb.scopeUse("a"), mb.num(4.0)))),
                                        Opt.some(mb.set(mb.scopeUse("a"), mb.num(3.0))))),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test02100NestedBranchSetIncompatibleOuter() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        dynTrue(mb),
                                        mb.ifOf(
                                                TList.of(
                                                        new TestMb.Branch(
                                                                dynTrue(mb), mb.set(mb.scopeUse("a"), mb.num(4.0)))),
                                                Opt.some(mb.set(mb.scopeUse("a"), mb.num(6.0)))))),
                                Opt.some(mb.set(mb.scopeUse("a"), mb.num(3.0)))),
                        mb.call(mb.log(), mb.str("done")))));
        assertParityMergeError(modules);
    }

    @Test
    public void test02150NestedBranchSetCompatibleDeep() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", evenCast(mb, 2.0)),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        dynTrue(mb),
                                        mb.ifOf(
                                                TList.of(
                                                        new TestMb.Branch(
                                                                dynTrue(mb), mb.set(mb.scopeUse("a"), mb.num(4.0)))),
                                                Opt.some(mb.set(mb.scopeUse("a"), mb.num(6.0)))))),
                                Opt.some(mb.set(mb.scopeUse("a"), mb.num(8.0)))),
                        mb.call(mb.log(), mb.str("done")))));
        compileOk(modules);
    }

    @Test
    public void test00800AddDynamicIntErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(moduleSeq(
                        bindEvenOddType(mb, "EvenOdd"),
                        mb.bind("a", mb.as(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.bind("c", mb.operatorAdd(mb.scopeUse("a"), mb.as(mb.num(3.0), mb.typesLong()))),
                        mb.call(mb.log(), mb.str("unreachable")))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected compile error, got " + res);
        assertTrue(
                res.toString().contains("operator_add: reference is a dynamic int"),
                () -> "expected dynamic-int parity error, got " + res);
    }
}
