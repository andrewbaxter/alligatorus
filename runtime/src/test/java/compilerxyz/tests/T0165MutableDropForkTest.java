package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.eval.ExprErr;
import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;

/**
 * A mutable drop type (a copy of the evenodd fixture whose drop aspect is a
 * drop canary): the binding's parity state selects the drop log message at
 * drop-codegen time. Each expression that can throw is a control-flow fork
 * whose exceptional edge must run the drops as of that point in the code
 * path — a throw before a state mutation must log the pre-mutation drop
 * message, a throw after it the post-mutation one. This requires an implicit
 * drop-finally per unique fork, not one shared finally generated from the
 * scope's final state.
 */
public final class T0165MutableDropForkTest {
  private static SourceExpr dropCodeLogging(TestMb mb, String msg) {
    return mb.constEvalCode(
        mb.call(
            mb.evalConstEval(),
            mb.recordOf(
                TList.of(
                    new TestMb.RecordPair(
                        "xx_", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                    new TestMb.RecordPair(
                        "staged",
                        mb.stage(
                            mb.seqOf(
                                mb.call(mb.log(), mb.str(msg)),
                                mb.return_(mb.graft(mb.scopeUse("underlying_code_"))))))))));
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


  private static SourceExpr delegateNull(TestMb mb, SourceExpr underlying, SourceExpr aspectRef, String methodName) {
    return mb.call(
        mb.field(aspectRef, mb.str(methodName)),
        mb.tupleOf(
            mb.field(
                mb.call(
                    mb.evalAspectLookup(),
                    mb.tupleOf(
                        underlying,
                        aspectRef,
                        mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                mb.str("value")),
            mb.recordOf(
                TList.of(
                    new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                    new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                    new TestMb.RecordPair("cv", mb.field(mb.scopeUse("arg"), mb.str("cv")))))));
  }

  private static SourceExpr delegateNullXxOnly(TestMb mb, SourceExpr underlying, SourceExpr aspectRef, String methodName) {
    return mb.call(
        mb.field(aspectRef, mb.str(methodName)),
        mb.tupleOf(
            mb.field(
                mb.call(
                    mb.evalAspectLookup(),
                    mb.tupleOf(underlying, aspectRef, mb.scopeUse("arg"))),
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
            mb.recordOf(
                TList.of(
                    new TestMb.RecordPair(
                        "code", mb.field(mb.scopeUse("sum_cv_"), mb.str("code"))),
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
                                                    new TestMb.RecordPair("parity", parity))))),
                                    new TestMb.RecordPair(
                                        "source",
                                        mb.call(
                                            mb.lower(mb.scopeUse("vs_expr")),
                                            mb.literalNull()))))))))));
  }





  private static TList<SourceExpr> bindEvenOddType(TestMb mb, String name) {
    return TList.of(
        mb.bind(
            "even_type",
            mb.lower(
                mb.seqOf(
                    mb.bind("even_type_new", mb.lower(mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "vs",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("union")),
                        mb.tupleOf())),
                mb.bind(
                    "vs_expr",
                    mb.field(mb.call(
                        mb.field(mb.scopeUse("vs"), mb.str("variant")),
                        mb.recordOf(
                            TList.of(
                                new TestMb.RecordPair("tag", mb.str("expr")),
                                new TestMb.RecordPair("type_ref", mb.typesNull())))), mb.str("value"))),
                mb.bind(
                    "vs_binding",
                    mb.field(mb.call(
                        mb.field(mb.scopeUse("vs"), mb.str("variant")),
                        mb.recordOf(
                            TList.of(
                                new TestMb.RecordPair("tag", mb.str("binding")),
                                new TestMb.RecordPair(
                                    "type_ref",
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "int", mb.evalBinding()),
                                            new TestMb.RecordPair(
                                                "state",
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "parity", mb.typesLong()),
                                                        new TestMb.RecordPair("setter", mb.call(mb.typesOption(), mb.builtin("eval", "binding_setter")))))))))))), mb.str("value"))),
                mb.bind(
                    "u",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("object")),
                        mb.str(name))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("set_inner")),
                    mb.recordOf(
                        TList.of(new TestMb.RecordPair("parity", mb.typesLong())))),
                mb.bind(
                    "u_gp_method",
                    mb.call(mb.field(mb.scopeUse("u"), mb.str("func")), mb.str("get_parity"))),
                mb.call(
                    mb.field(mb.scopeUse("u_gp_method"), mb.str("impl")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("arg", mb.typesNull()),
                            new TestMb.RecordPair("ret", mb.optSomeType(mb.typesLong())),
                            new TestMb.RecordPair(
                                "impl_body",
                                mb.stage(
                                    mb.seqOf(
                                        mb.return_(
                                            mb.field(
                                                mb.scopeUse("inner"), mb.str("parity"))))))))),
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
                                            "initial_bind", mb.scopeUse("outer")))))))),
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
                mb.bind(
                    "vb",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("object")),
                        mb.str("EvenOddValue"))),
                mb.call(
                    mb.field(mb.scopeUse("vb"), mb.str("set_inner")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair(
                                "type", mb.field(mb.scopeUse("u"), mb.str("type"))),
                            new TestMb.RecordPair(
                                "source", mb.field(mb.scopeUse("vs"), mb.str("type")))))),
                mb.bind(
                    "vb_gs_method",
                    mb.call(mb.field(mb.scopeUse("vb"), mb.str("func")), mb.str("get_source"))),
                mb.call(
                    mb.field(mb.scopeUse("vb_gs_method"), mb.str("impl")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("arg", mb.typesNull()),
                            new TestMb.RecordPair(
                                "ret", mb.optSomeType(mb.field(mb.scopeUse("vs"), mb.str("type")))),
                            new TestMb.RecordPair(
                                "impl_body",
                                mb.stage(
                                    mb.seqOf(
                                        mb.return_(
                                            mb.field(
                                                mb.scopeUse("inner"), mb.str("source"))))))))),
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
                                                    mb.field(
                                                        mb.scopeUse("inner"),
                                                        mb.str("type"))))))))))),
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
                                                mb.evalAspectConsumeDynamic(),
                                                "to_dynamic")),
                                        new TestMb.RecordPair(
                                            "jvm_class",
                                            delegateNullXxOnly(
                                                mb,
                                                mb.typesLong(),
                                                mb.evalAspectConsumeDynamic(),
                                                "jvm_class")))))))),
                mb.bind(
                    "bb",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("object")),
                        mb.str("EvenOddBinding"))),
                mb.call(
                    mb.field(mb.scopeUse("bb"), mb.str("set_inner")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("int", mb.evalBinding()),
                            new TestMb.RecordPair(
                                "state",
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "parity", mb.typesLong()),
                                        new TestMb.RecordPair("setter", mb.call(mb.typesOption(), mb.builtin("eval", "binding_setter"))))))))),
                mb.bind(
                    "gp_method",
                    mb.call(
                        mb.field(mb.scopeUse("bb"), mb.str("func")),
                        mb.str("get_parity"))),
                mb.call(
                    mb.field(mb.scopeUse("gp_method"), mb.str("impl")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("arg", mb.typesNull()),
                            new TestMb.RecordPair("ret", mb.optSomeType(mb.typesLong())),
                            new TestMb.RecordPair(
                                "impl_body",
                                mb.stage(
                                    mb.seqOf(
                                        mb.return_(
                                            mb.field(
                                                mb.field(
                                                    mb.scopeUse("inner"),
                                                    mb.str("state")),
                                                mb.str("parity"))))))))),
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
                                            "initial_bind", mb.scopeUse("outer")))))))),
                mb.call(
                    mb.field(mb.scopeUse("bb"), mb.str("impl_interface")),
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
                                                mb.evalAspectConsumeDynamic(),
                                                "to_dynamic")),
                                        new TestMb.RecordPair(
                                            "jvm_class",
                                            delegateNullXxOnly(
                                                mb,
                                                mb.typesLong(),
                                                mb.evalAspectConsumeDynamic(),
                                                "jvm_class")))))))),
                mb.bind("even_binding_new", mb.field(mb.scopeUse("bb"), mb.str("new"))),
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
                                            "use",
                                            mb.seqOf(
                                                mb.set(
                                                    mb.field(
                                                        mb.field(
                                                            mb.scopeUse("inner"),
                                                            mb.str("state")),
                                                        mb.str("setter")),
                                                    mb.field(
                                                        mb.scopeUse("arg"), mb.str("setter"))),
                                                mb.bind(
                                                    "int_use_cv_",
                                                    mb.call(
                                                        mb.field(
                                                            mb.evalBinding(),
                                                            mb.str("use")),
                                                        mb.tupleOf(
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("int")),
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
                                                                    "setter",
                                                                    mb.call(
                                                                        mb.field(
                                                                            mb.call(mb.typesOption(), mb.builtin("eval", "binding_setter")),
                                                                            mb.str("none")),
                                                                        mb.literalNull())),
                                                                new TestMb.RecordPair(
                                                                    "code",
                                                                    mb.field(
                                                                        mb.scopeUse("arg"),
                                                                        mb.str("code")))))))),
                                                mb.return_(
                                            mb.call(
                                                mb.evalCodevalue(),
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "code",
                                                            mb.field(
                                                                mb.scopeUse("int_use_cv_"),
                                                                mb.str("code"))),
                                                        new TestMb.RecordPair(
                                                            "value",
                                                            mb.call(
                                                                mb.lower(
                                                                    mb.scopeUse("even_value_new")),
                                                                mb.recordOf(
                                                                    TList.of(
                                                                        new TestMb.RecordPair(
                                                                            "type",
                                                                            mb.call(
                                                                                mb.lower(
                                                                                    mb.scopeUse(
                                                                                        "even_type_new")),
                                                                                mb.recordOf(
                                                                                    TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                            "parity",
                                                                                            mb.field(
                                                                                                mb.field(
                                                                                                    mb.scopeUse(
                                                                                                        "inner"),
                                                                                                    mb.str(
                                                                                                        "state")),
                                                                                                mb.str(
                                                                                                    "parity"))))))),
                                                                        new TestMb.RecordPair(
                                                                            "source",
                                                                            mb.call(
                                                                                mb.lower(
                                                                                    mb.scopeUse(
                                                                                        "vs_binding")),
                                                                                mb.recordOf(
                                                                                    TList.of(
                                                                                        new TestMb.RecordPair(
                                                                                            "int",
                                                                                            mb.field(
                                                                                                mb.scopeUse(
                                                                                                    "inner"),
                                                                                                mb.str(
                                                                                                    "int"))),
                                                                                        new TestMb.RecordPair(
                                                                                            "state",
                                                                                            mb.field(
                                                                                                mb.scopeUse(
                                                                                                    "inner"),
                                                                                                mb.str(
                                                                                                    "state"))))))))))))))))),
                                        new TestMb.RecordPair(
                                            "fork",
                                            mb.seqOf(
                                                mb.return_(
                                                    mb.call(
                                                        mb.lower(mb.scopeUse("even_binding_new")),
                                                        mb.recordOf(
                                                            TList.of(
                                                                new TestMb.RecordPair(
                                                                    "int",
                                                                    mb.field(
                                                                        mb.scopeUse("inner"),
                                                                        mb.str("int"))),
                                                                new TestMb.RecordPair(
                                                                    "state",
                                                                    mb.recordOf(
                                                                        TList.of(
                                                                            new TestMb.RecordPair(
                                                                                "parity",
                                                                                mb.field(
                                                                                    mb.field(
                                                                                        mb.scopeUse(
                                                                                            "inner"),
                                                                                        mb.str(
                                                                                            "state")),
                                                                                    mb.str(
                                                                                        "parity"))),
                                                                            new TestMb.RecordPair(
                                                                                "setter",
                                                                                mb.field(
                                                                                    mb.field(
                                                                                        mb.scopeUse(
                                                                                            "inner"),
                                                                                        mb.str(
                                                                                            "state")),
                                                                                    mb.str(
                                                                                        "setter")))))))))))),
                                        new TestMb.RecordPair(
                                            "merge",
                                            mb.labeled(
                                                "merge_res",
                                                mb.seqOf(
                                                    mb.bind(
                                                        "initial_",
                                                        mb.field(
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("state")),
                                                            mb.str("parity"))),
                                                    mb.foreach(
                                                        mb.field(
                                                            mb.scopeUse("arg"), mb.str("others")),
                                                        mb.str("b"),
                                                        mb.ifOf(
                                                            TList.of(
                                                                new TestMb.Branch(
                                                                    mb.operatorEq(
                                                                        mb.call(
                                                                            mb.field(
                                                                                mb.call(
                                                                                    mb.mustReinterpret(),
                                                                                    mb.tupleOf(
                                                                                        mb.scopeUse(
                                                                                            "b"),
                                                                                        mb.lower(
                                                                                            mb.scopeUse(
                                                                                                "bb_type")))),
                                                                                mb.str(
                                                                                    "get_parity")),
                                                                            mb.literalNull()),
                                                                        mb.scopeUse("initial_")),
                                                                    mb.as_(
                                                                        mb.literalNull(),
                                                                        mb.typesNull()))),
                                                            Opt.some(mb.return_(
                                                                "merge_res",
                                                                mb.call(
                                                                    mb.field(
                                                                        mb.call(
                                                                            mb.typesEresult(),
                                                                            mb.evalBinding()),
                                                                        mb.str("err")),
                                                                    mb.tupleOf(
                                                                        mb.field(
                                                                            mb.scopeUse("arg"),
                                                                            mb.str("loc")),
                                                                        mb.str(
                                                                            "evenodd merge:"
                                                                                + " branch parities"
                                                                                + " differ"))))))),
                                                    mb.return_(
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.typesEresult(),
                                                                    mb.evalBinding()),
                                                                mb.str("ok")),
                                                            mb.scopeUse("outer")))))),
                                        new TestMb.RecordPair(
                                            "equals",
                                            mb.seqOf(
                                                mb.return_(
                                                    mb.operatorEq(
                                                        mb.field(
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("state")),
                                                            mb.str("parity")),
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.mustReinterpret(),
                                                                    mb.tupleOf(
                                                                        mb.field(
                                                                            mb.scopeUse("arg"),
                                                                            mb.str("other")),
                                                                        mb.lower(
                                                                            mb.scopeUse(
                                                                                "bb_type")))),
                                                                mb.str("get_parity")),
                                                            mb.literalNull()))))))))))),
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
                                        new TestMb.RecordPair(
                                            "type", mb.scopeUse("outer")))))))),
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
                                            mb.panic(mb.str("EvenOdd is_equal: not implemented"))),
                                        new TestMb.RecordPair(
                                            "can_cast_from",
                                            mb.seqOf(
                                                mb.return_(
                                                    mb.operatorEq(mb.num(1.0), mb.num(1.0))))),
                                        new TestMb.RecordPair(
                                            "cast_from", mb.seqOf(
        mb.bind(
            "val_opt_",
            mb.call(
                mb.field(mb.field(mb.scopeUse("arg"), mb.str("cv")), mb.str("value")),
                mb.tupleOf())),
        mb.return_(
            mb.ifOf(
                TList.of(
                    new TestMb.Branch(
                        mb.field(mb.scopeUse("val_opt_"), mb.str("is_some")),
                        mb.seqOf(
                            mb.bind("val_", mb.field(mb.scopeUse("val_opt_"), mb.str("value"))),
                            mb.bind(
                                "val_type_obj_",
                                mb.call(
                                    mb.field(mb.evalValue(), mb.str("type")),
                                    mb.scopeUse("val_"))),
                            mb.bind(
                                "int_aspect_opt_",
                                mb.call(
                                    mb.evalAspectLookup(),
                                    mb.tupleOf(
                                        mb.scopeUse("val_type_obj_"),
                                        mb.evalAspectInt(),
                                        mb.field(mb.scopeUse("arg"), mb.str("xx"))))),
                            mb.bind(
                                "int_dynamic_opt_",
                                mb.call(
                                    mb.field(mb.evalType(), mb.str("aspect")),
                                    mb.tupleOf(
                                        mb.scopeUse("val_type_obj_"),
                                        mb.recordOf(
                                            TList.of(
                                                new TestMb.RecordPair(
                                                    "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                                new TestMb.RecordPair(
                                                    "key", mb.evalTypeLongDynamicIdentity())))))),
                            mb.return_(
                                mb.ifOf(
                                    TList.of(
                                        new TestMb.Branch(
                                            mb.field(
                                                mb.scopeUse("int_aspect_opt_"),
                                                mb.str("is_some")),
                                            mb.seqOf(
                                                mb.bind(
                                                    "cast_cv_",
                                                    mb.field(
                                                        mb.call(
                                                            mb.field(
                                                                mb.evalType(),
                                                                mb.str("cast_from")),
                                                            mb.tupleOf(
                                                                mb.typesLong(),
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
                                                                                mb.str("cv"))))))),
                                                        mb.str("value"))),
                                                mb.return_(mb.call(
            mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("ok")),
            mb.call(
                mb.evalCodevalue(),
                mb.recordOf(
                    TList.of(
                        new TestMb.RecordPair(
                            "code",
                            mb.field(mb.scopeUse("cast_cv_"), mb.str("code"))),
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
                                                            mb.operatorMod(
                                                                mb.call(
                                                                    mb.field(
                                                                        mb.evalAspectInt(),
                                                                        mb.str("get")),
                                                                    mb.tupleOf(
                                                                        mb.field(
                                                                            mb.scopeUse(
                                                                                "int_aspect_opt_"),
                                                                            mb.str("value")),
                                                                        mb.scopeUse("val_"))),
                                                                mb.as_(mb.num(2.0), mb.typesLong()))))))),
                                        new TestMb.RecordPair(
                                            "source",
                                            mb.call(
                                                mb.lower(mb.scopeUse("vs_expr")),
                                                mb.literalNull()))))))))))))),
                                        new TestMb.Branch(
                                            mb.field(
                                                mb.scopeUse("int_dynamic_opt_"),
                                                mb.str("is_some")),
                                            mb.call(
                                                mb.field(
                                                    mb.call(mb.typesEresult(), mb.typesCve()),
                                                    mb.str("err")),
                                                mb.tupleOf(
                                                    mb.field(mb.scopeUse("arg"), mb.str("loc")),
                                                    mb.str(
                                                        "cast: cannot cast a dynamic int to"
                                                            + " evenodd; parity unknown"))))),
                                    Opt.some(castFromPassthrough(mb))))))),
                Opt.some(castFromPassthrough(mb)))))),
                                        new TestMb.RecordPair(
                                            "desc", mb.seqOf(mb.return_(mb.str("evenodd")))),
                                        new TestMb.RecordPair(
                                            "aspect",
                                            mb.ifOf(
                                                TList.of(
                                                    new TestMb.Branch(
                                                        mb.operatorEq(
                                                            mb.field(mb.scopeUse("arg"), mb.str("key")),
                                                            mb.evalAspectBindId()),
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.typesOption(), mb.typesAspect()),
                                                                mb.str("some")),
                                                            mb.scopeUse("outer"))),
                                                    new TestMb.Branch(
                                                        mb.operatorEq(
                                                            mb.field(mb.scopeUse("arg"), mb.str("key")),
                                                            mb.evalAspectBindDynamicId()),
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.typesOption(), mb.typesAspect()),
                                                                mb.str("some")),
                                                            mb.scopeUse("outer"))),
                                                    new TestMb.Branch(
                                                        mb.operatorEq(
                                                            mb.field(mb.scopeUse("arg"), mb.str("key")),
                                                            mb.evalAspectConsumeDynamicId()),
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.typesOption(), mb.typesAspect()),
                                                                mb.str("some")),
                                                            mb.scopeUse("outer"))),
                                                    new TestMb.Branch(
                                                        mb.operatorEq(
                                                            mb.field(mb.scopeUse("arg"), mb.str("key")),
                                                            mb.evalAspectDropDynamicId()),
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.typesOption(), mb.typesAspect()),
                                                                mb.str("some")),
                                                            mb.scopeUse("outer"))),
                                                    new TestMb.Branch(
                                                        mb.operatorEq(
                                                            mb.field(mb.scopeUse("arg"), mb.str("key")),
                                                            mb.evalAspectSetId()),
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.typesOption(), mb.typesAspect()),
                                                                mb.str("some")),
                                                            mb.scopeUse("outer"))),
                                                    new TestMb.Branch(
                                                        mb.operatorEq(
                                                            mb.field(mb.scopeUse("arg"), mb.str("key")),
                                                            mb.evalOperatorAddId()),
                                                        mb.call(
                                                            mb.field(
                                                                mb.call(
                                                                    mb.typesOption(), mb.typesAspect()),
                                                                mb.str("some")),
                                                            mb.scopeUse("outer")))),
                                                Opt.some(mb.call(
                                                    mb.field(
                                                        mb.call(mb.typesOption(), mb.typesAspect()),
                                                        mb.str("none")),
                                                    mb.literalNull())))),
                                        new TestMb.RecordPair(
                                            "can_cast_to",
                                            mb.seqOf(mb.return_(mb.bool(false)))),
                                        new TestMb.RecordPair(
                                            "cast_to",
                                            mb.panic(
                                                mb.str("EvenOdd cast_to: not implemented"))))))))),
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
                                            "bind", mb.seqOf(
        mb.bind("int_bind_er", mb.call(
            mb.field(mb.evalAspectBind(), mb.str("bind")),
            mb.tupleOf(
                mb.field(
                    mb.call(
                        mb.evalAspectLookup(),
                        mb.tupleOf(
                            mb.typesLong(),
                            mb.evalAspectBind(),
                            mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                    mb.str("value")),
                mb.recordOf(
                    TList.of(
                        new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                        new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                        new TestMb.RecordPair("cv", mb.field(mb.scopeUse("arg"), mb.str("cv")))))))),
        mb.bind("int_bind_res", mb.field(mb.scopeUse("int_bind_er"), mb.str("value"))),
        mb.bind("int_code", mb.field(mb.scopeUse("int_bind_res"), mb.str("code"))),
        mb.bind("int_binding", mb.field(mb.scopeUse("int_bind_res"), mb.str("binding"))),
        mb.return_(
            mb.call(
                mb.field(
                    mb.call(mb.typesEresult(), mb.typesBindResult()),
                    mb.str("ok")),
                mb.call(
                    mb.evalBindResult(),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("code", mb.scopeUse("int_code")),
                            new TestMb.RecordPair(
                                "binding",
                                mb.call(
                                    mb.lower(mb.scopeUse("even_binding_new")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "int",
                                                mb.scopeUse("int_binding")),
                                            new TestMb.RecordPair(
                                                "state",
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "parity",
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("parity"))),
                                                        new TestMb.RecordPair(
                                                            "setter",
                                                            mb.call(
                                                                mb.field(
                                                                    mb.call(
                                                                        mb.typesOption(),
                                                                        mb.builtin(
                                                                            "eval",
                                                                            "binding_setter")),
                                                                    mb.str("none")),
                                                                mb.literalNull())))))))))))))))))))))),
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
        mb.field(mb.evalAspectBindDynamic(), mb.str("bind_dynamic")),
        mb.tupleOf(
            mb.field(
                mb.call(
                    mb.evalAspectLookup(),
                    mb.tupleOf(
                        mb.typesLong(),
                        mb.evalAspectBindDynamic(),
                        mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                mb.str("value")),
            mb.recordOf(
                TList.of(
                    new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                    new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                    new TestMb.RecordPair("cv", mb.field(mb.scopeUse("arg"), mb.str("cv")))))))))))))),
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
                                            "drop", mb.seqOf(
        mb.bind("underlying_code_", mb.field(
            mb.call(
                mb.field(mb.evalAspectDropDynamicLight(), mb.str("drop_dynamic_light")),
                mb.tupleOf(
                    mb.field(
                        mb.call(
                            mb.evalAspectLookup(),
                            mb.tupleOf(
                                mb.typesLong(),
                                mb.evalAspectDropDynamicLight(),
                                mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                        mb.str("value")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair(
                                "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                            new TestMb.RecordPair(
                                "loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                            new TestMb.RecordPair(
                                "cv", mb.field(mb.scopeUse("arg"), mb.str("cv"))))))),
            mb.str("value"))),
        mb.return_(
            mb.ifOf(
                TList.of(
                    new TestMb.Branch(
                        mb.operatorEq(
                            mb.field(mb.scopeUse("inner"), mb.str("parity")),
                            mb.as_(mb.num(0.0), mb.typesLong())),
                        dropCodeLogging(mb, "drop_even"))),
                Opt.some(dropCodeLogging(mb, "drop_odd")))))))))))),
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
                                            delegateNull(
                                                mb,
                                                mb.typesLong(),
                                                mb.evalAspectConsumeDynamic(),
                                                "to_dynamic")),
                                        new TestMb.RecordPair(
                                            "jvm_class",
                                            delegateNullXxOnly(
                                                mb,
                                                mb.typesLong(),
                                                mb.evalAspectConsumeDynamic(),
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
                                            "set", mb.seqOf(
        mb.bind(
            "conv_er_",
            mb.call(
                mb.field(mb.evalType(), mb.str("cast_from")),
                mb.tupleOf(
                    mb.scopeUse("outer"),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                            new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                            new TestMb.RecordPair(
                                "cv", mb.field(mb.scopeUse("arg"), mb.str("value")))))))),
        mb.return_(
            mb.ifOf(
                TList.of(
                    new TestMb.Branch(
                        mb.field(mb.scopeUse("conv_er_"), mb.str("has_errors")),
                        mb.scopeUse("conv_er_"))),
                Opt.some(mb.seqOf(
                    mb.bind("conv_cv_", mb.field(mb.scopeUse("conv_er_"), mb.str("value"))),
                    mb.bind(
                        "rhs_parity_",
                        mb.call(
                            mb.field(
                                mb.call(
                                    mb.mustReinterpret(),
                                    mb.tupleOf(
                                        mb.call(
                                            mb.field(mb.evalValue(), mb.str("type")),
                                            mb.field(
                                                mb.call(
                                                    mb.field(
                                                        mb.scopeUse("conv_cv_"),
                                                        mb.str("value")),
                                                    mb.tupleOf()),
                                                mb.str("value"))),
                                        mb.lower(mb.scopeUse("u_type")))),
                                mb.str("get_parity")),
                            mb.literalNull())),
                    mb.return_(mb.seqOf(
        mb.bind(
            "self_val_",
            mb.field(
                mb.call(
                    mb.field(
                        mb.field(mb.scopeUse("arg"), mb.str("self")), mb.str("value")),
                    mb.tupleOf()),
                mb.str("value"))),
        mb.bind(
            "src_",
            mb.call(
                mb.field(
                    mb.call(
                        mb.mustReinterpret(),
                        mb.tupleOf(
                            mb.scopeUse("self_val_"), mb.lower(mb.scopeUse("vb_type")))),
                    mb.str("get_source")),
                mb.literalNull())),
        mb.return_(
            mb.matchOf(
                mb.scopeUse("src_"),
                mb.str("sv"),
                TList.of(
                    new TestMb.Branch(
                        mb.str("binding"),
                        mb.seqOf(
                            mb.bind(
                                "new_binding_",
                                mb.call(
                                    mb.lower(mb.scopeUse("even_binding_new")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "int",
                                                mb.field(mb.scopeUse("sv"), mb.str("int"))),
                                            new TestMb.RecordPair(
                                                "state",
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair("parity", mb.scopeUse("rhs_parity_")),
                                                        new TestMb.RecordPair(
                                                            "setter",
                                                            mb.field(
                                                                mb.field(
                                                                    mb.scopeUse("sv"),
                                                                    mb.str("state")),
                                                                mb.str("setter")))))))))),
                            mb.call(
                                mb.field(
                                    mb.builtin("eval", "binding_setter"),
                                    mb.str("set")),
                                mb.tupleOf(
                                    mb.field(
                                        mb.field(
                                            mb.field(mb.scopeUse("sv"), mb.str("state")),
                                            mb.str("setter")),
                                        mb.str("value")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                            new TestMb.RecordPair(
                                                "loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                                            new TestMb.RecordPair(
                                                "new_binding", mb.scopeUse("new_binding_")),
                                            new TestMb.RecordPair(
                                                "drop_modified",
                                                mb.as_(
                                                    mb.operatorEq(mb.num(1.0), mb.num(1.0)),
                                                    mb.typesBool())))))),
                            mb.return_(
                                mb.call(
                                    mb.field(mb.evalAspectSet(), mb.str("set")),
                                    mb.tupleOf(
                                        mb.field(
                                            mb.call(
                                                mb.evalAspectLookup(),
                                                mb.tupleOf(
                                                    mb.typesLong(),
                                                    mb.evalAspectSet(),
                                                    mb.field(
                                                        mb.scopeUse("arg"), mb.str("xx")))),
                                            mb.str("value")),
                                        mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "xx",
                                                mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                            new TestMb.RecordPair(
                                                "loc",
                                                mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                                            new TestMb.RecordPair(
                                                "self",
                                                mb.call(
                                                    mb.field(mb.evalBinding(), mb.str("use")),
                                                    mb.tupleOf(
                                                        mb.field(
                                                            mb.scopeUse("sv"), mb.str("int")),
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
                                                                "setter",
                                                                mb.call(
                                                                    mb.field(
                                                                        mb.call(mb.typesOption(), mb.builtin("eval", "binding_setter")),
                                                                        mb.str("none")),
                                                                    mb.literalNull())),
                                                            new TestMb.RecordPair(
                                                                "code",
                                                                mb.field(
                                                                    mb.field(
                                                                        mb.scopeUse("arg"),
                                                                        mb.str("self")),
                                                                    mb.str("code")))))))),
                                            new TestMb.RecordPair(
                                                "value", mb.scopeUse("conv_cv_")))))))))),
                Opt.some(mb.call(
                    mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("err")),
                    mb.tupleOf(
                        mb.field(mb.scopeUse("arg"), mb.str("loc")),
                        mb.str("set: cannot set a non-binding evenodd value")))))))))))))))))))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("iface_ref", mb.evalOperatorAdd()),
                            new TestMb.RecordPair(
                                "record",
                                ifaceMethods(
                                    mb,
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "add", mb.seqOf(
        mb.bind(
            "sum_er_",
            mb.call(
                mb.field(mb.evalOperatorAdd(), mb.str("add")),
                mb.tupleOf(
                    mb.field(
                        mb.call(
                            mb.evalAspectLookup(),
                            mb.tupleOf(
                                mb.typesLong(),
                                mb.evalOperatorAdd(),
                                mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                        mb.str("value")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                            new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                            new TestMb.RecordPair(
                                "base", mb.field(mb.scopeUse("arg"), mb.str("base"))),
                            new TestMb.RecordPair(
                                "reference",
                                mb.field(
                                    mb.call(
                                        mb.field(mb.evalType(), mb.str("cast_from")),
                                        mb.tupleOf(
                                            mb.typesLong(),
                                            mb.recordOf(
                                                TList.of(
                                                    new TestMb.RecordPair(
                                                        "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                                    new TestMb.RecordPair(
                                                        "loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                                                    new TestMb.RecordPair(
                                                        "cv",
                                                        mb.field(
                                                            mb.scopeUse("arg"), mb.str("reference"))))))),
                                    mb.str("value")))))))),
        mb.bind("sum_cv_", mb.field(mb.scopeUse("sum_er_"), mb.str("value"))),
        mb.bind(
            "ref_opt_",
            mb.call(
                mb.field(
                    mb.field(mb.scopeUse("arg"), mb.str("reference")), mb.str("value")),
                mb.tupleOf())),
        mb.return_(
            mb.ifOf(
                TList.of(
                    new TestMb.Branch(
                        mb.field(mb.scopeUse("ref_opt_"), mb.str("is_some")),
                        mb.seqOf(
                            mb.bind("ref_val_", mb.field(mb.scopeUse("ref_opt_"), mb.str("value"))),
                            mb.bind(
                                "ref_type_obj_",
                                mb.call(
                                    mb.field(mb.evalValue(), mb.str("type")),
                                    mb.scopeUse("ref_val_"))),
                            mb.bind(
                                "ref_int_aspect_opt_",
                                mb.call(
                                    mb.evalAspectLookup(),
                                    mb.tupleOf(
                                        mb.scopeUse("ref_type_obj_"),
                                        mb.evalAspectInt(),
                                        mb.field(mb.scopeUse("arg"), mb.str("xx"))))),
                            mb.bind(
                                "ref_int_dynamic_opt_",
                                mb.call(
                                    mb.field(mb.evalType(), mb.str("aspect")),
                                    mb.tupleOf(
                                        mb.scopeUse("ref_type_obj_"),
                                        mb.recordOf(
                                            TList.of(
                                                new TestMb.RecordPair(
                                                    "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                                new TestMb.RecordPair(
                                                    "key", mb.evalTypeLongDynamicIdentity())))))),
                            mb.return_(
                                mb.ifOf(
                                    TList.of(
                                        new TestMb.Branch(
                                            mb.field(
                                                mb.scopeUse("ref_int_aspect_opt_"),
                                                mb.str("is_some")),
                                            evenOddAddWrapped(mb, mb.operatorMod(
            mb.operatorAdd(
                mb.field(mb.scopeUse("inner"), mb.str("parity")),
                mb.call(
                    mb.field(mb.evalAspectInt(), mb.str("get")),
                    mb.tupleOf(
                        mb.field(mb.scopeUse("ref_int_aspect_opt_"), mb.str("value")),
                        mb.scopeUse("ref_val_")))),
            mb.as_(mb.num(2.0), mb.typesLong())))),
                                        new TestMb.Branch(
                                            mb.field(
                                                mb.scopeUse("ref_int_dynamic_opt_"),
                                                mb.str("is_some")),
                                            mb.call(
                                                mb.field(
                                                    mb.call(mb.typesEresult(), mb.typesCve()),
                                                    mb.str("err")),
                                                mb.tupleOf(
                                                    mb.field(mb.scopeUse("arg"), mb.str("loc")),
                                                    mb.str(
                                                        "operator_add: reference is a dynamic"
                                                            + " int; parity unknown"))))),
                                    Opt.some(mb.seqOf(
                                        mb.bind(
                                            "ref_evenodd_type_",
                                            mb.call(
                                                mb.mustReinterpret(),
                                                mb.tupleOf(
                                                    mb.scopeUse("ref_type_obj_"),
                                                    mb.lower(mb.scopeUse("u_type"))))),
                                        mb.return_(evenOddAddWrapped(mb, mb.operatorMod(
            mb.operatorAdd(
                mb.field(mb.scopeUse("inner"), mb.str("parity")),
                mb.call(
                    mb.field(mb.scopeUse("ref_evenodd_type_"), mb.str("get_parity")),
                    mb.literalNull())),
            mb.as_(mb.num(2.0), mb.typesLong()))))))))))),
                Opt.some(mb.call(
                    mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("err")),
                    mb.tupleOf(
                        mb.field(mb.scopeUse("arg"), mb.str("loc")),
                        mb.str("operator_add: reference has no compile-time value")))))))))))))),
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
                                            "use",
                                            mb.call(
                                                mb.evalCodevalue(),
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "code",
                                                            mb.field(
                                                                mb.scopeUse("arg"),
                                                                mb.str("code"))),
                                                        new TestMb.RecordPair(
                                                            "value",
                                                            mb.call(
                                                                mb.evalStack(),
                                                                mb.recordOf(
                                                                    TList.of(
                                                                        new TestMb.RecordPair(
                                                                            "type",
                                                                            mb.scopeUse(
                                                                                "outer")),
                                                                        new TestMb.RecordPair(
                                                                            "width",
                                                                            mb.as_(
                                                                                mb.num(1.),
                                                                                mb.typesLong())))))))))),
                                        new TestMb.RecordPair(
                                            "fork", mb.scopeUse("outer")),
                                        new TestMb.RecordPair(
                                            "merge",
                                            mb.call(
                                                mb.field(
                                                    mb.call(
                                                        mb.typesEresult(), mb.evalBinding()),
                                                    mb.str("ok")),
                                                mb.scopeUse("outer"))),
                                        new TestMb.RecordPair(
                                            "equals",
                                            mb.seqOf(
                                                mb.return_(
                                                    mb.operatorEq(
                                                        mb.num(1.0), mb.num(1.0))))))))))),
                mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.scopeUse("even_type_new"))))),
                    mb.return_(
                        mb.call(
                            mb.scopeUse("even_type_new"),
                            mb.recordOf(
                                TList.of(new TestMb.RecordPair("parity", mb.num(0.0))))))))));
  }

  private static SourceExpr moduleSeq(TestMb mb, TList<SourceExpr> prefix, SourceExpr... rest) {
    var all = new TList<SourceExpr>(prefix.size() + rest.length);
    for (var e : prefix) all.add(e);
    for (var e : rest) all.add(e);
    return mb.seq(all);
  }

  private static SourceExpr maybeThrow(TestMb mb, boolean doThrow, String msg) {
    return mb.ifOf(
        TList.of(new TestMb.Branch(
            mb.operatorEq(
                mb.as_(mb.num(1.0), mb.typesLong()),
                mb.as_(mb.num(doThrow ? 1.0 : 2.0), mb.typesLong())),
            mb.throw_(mb.str(msg)))),
        Opt.none());
  }

  private static SourceExpr program(TestMb mb, boolean throwBefore, boolean throwAfter) {
    return moduleSeq(
        mb,
        bindEvenOddType(mb, "MutCanary"),
        mb.tryCatchFinally(
            mb.seqOf(
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                maybeThrow(mb, throwBefore, "boom_before"),
                mb.set(mb.scopeUse("a"), mb.num(3.0)),
                maybeThrow(mb, throwAfter, "boom_after")),
            Opt.some(new TestMb.Catch(
                "e",
                mb.builtin("types", "string"),
                mb.seqOf(mb.call(mb.log(), mb.scopeUse("e"))))),
            Opt.none()),
        mb.call(mb.log(), mb.str("after")));
  }

  private static TList<String> compileAndRun(SourceExpr main) {
    var modules = new TMap<String, SourceModule>();
    modules.put("main", new SourceModule.V1(main));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
    switch (res) {
      case Result.Ok<?, ?> o -> {
        @SuppressWarnings("unchecked")
        var cast = (Result.Ok<Compile.CompileResult, TList<ExprErr>>) o;
        resOk = cast;
      }
      case Result.Err<?, ?> e -> {
        assertTrue(false, () -> "compile failed: " + res);
        throw new RuntimeException("unreachable");
      }
    }
    var ok = resOk.value();
    var out = new TList<String>(ok.logs().size());
    for (var l : ok.logs()) out.add(l.message());
    return out;
  }

  @Test
  public void test00050FixtureSanityNoForks() {
    // Fixture sanity: with no branching constructs between bind, set, and
    // scope exit, the end-of-scope drop reflects the post-set state.
    var mb = new TestMb();
    var messages =
        compileAndRun(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "MutCanary"),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                mb.set(mb.scopeUse("a"), mb.num(3.0)),
                mb.call(mb.log(), mb.str("after"))));
    assertEquals(
        TList.of("after", "drop_odd"),
        messages,
        () -> "end-of-scope drop should log the post-set message; got logs=" + messages);
  }

  @Test
  public void test00100NoThrowDropsWithMutatedMessage() {
    // No fork taken at runtime; the end-of-scope drop reflects the final
    // (post-set) state even though branching constructs intervened.
    var mb = new TestMb();
    var messages = compileAndRun(program(mb, false, false));
    assertEquals(
        TList.of("drop_odd", "after"),
        messages,
        () -> "end-of-scope drop should log the post-mutation message; got logs=" + messages);
  }

  @Test
  public void test00200ThrowBeforeMutationDropsWithInitialMessage() {
    // The first throw fork happens while parity is still 0, so its
    // exceptional edge must drop with the pre-mutation message even though a
    // later statement mutates the state.
    var mb = new TestMb();
    var messages = compileAndRun(program(mb, true, false));
    assertEquals(
        TList.of("drop_even", "boom_before", "after"),
        messages,
        () -> "throw before the set must drop with the pre-mutation message; got logs="
            + messages);
  }

  @Test
  public void test00250ThrowAfterMutationSameStatement() {
    // Mutation and throw inside one statement of the outer block: the drop at
    // the throw point must already reflect the mutated state.
    var mb = new TestMb();
    var messages =
        compileAndRun(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "MutCanary"),
                mb.tryCatchFinally(
                    mb.seqOf(
                        mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.seqOf(
                            mb.set(mb.scopeUse("a"), mb.num(3.0)),
                            maybeThrow(mb, true, "boom_same"))),
                    Opt.some(new TestMb.Catch(
                        "e",
                        mb.builtin("types", "string"),
                        mb.seqOf(mb.call(mb.log(), mb.scopeUse("e"))))),
                    Opt.none()),
                mb.call(mb.log(), mb.str("after"))));
    assertEquals(
        TList.of("drop_odd", "boom_same", "after"),
        messages,
        () -> "throw after a same-statement mutation must drop with the mutated state; got"
            + " logs=" + messages);
  }

  @Test
  public void test00260ThrowBeforeMutationSameStatement() {
    // The runtime path throws before the mutation even though the mutation
    // sits later in the same statement; the drop must use the initial state.
    var mb = new TestMb();
    var messages =
        compileAndRun(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "MutCanary"),
                mb.tryCatchFinally(
                    mb.seqOf(
                        mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                        mb.seqOf(
                            maybeThrow(mb, true, "boom_first"),
                            mb.set(mb.scopeUse("a"), mb.num(3.0)),
                            maybeThrow(mb, false, "boom_last"))),
                    Opt.some(new TestMb.Catch(
                        "e",
                        mb.builtin("types", "string"),
                        mb.seqOf(mb.call(mb.log(), mb.scopeUse("e"))))),
                    Opt.none()),
                mb.call(mb.log(), mb.str("after"))));
    assertEquals(
        TList.of("drop_even", "boom_first", "after"),
        messages,
        () -> "throw before a same-statement mutation must drop with the initial state; got"
            + " logs=" + messages);
  }

  @Test
  public void test00300ThrowAfterMutationDropsWithMutatedMessage() {
    var mb = new TestMb();
    var messages = compileAndRun(program(mb, false, true));
    assertEquals(
        TList.of("drop_odd", "boom_after", "after"),
        messages,
        () -> "throw after the set must drop with the post-mutation message; got logs="
            + messages);
  }
}
