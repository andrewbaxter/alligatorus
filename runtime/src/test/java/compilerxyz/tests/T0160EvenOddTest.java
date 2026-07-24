package compilerxyz.tests;

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

public final class T0160EvenOddTest {
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

  private static SourceExpr delegateVia(
      TestMb mb, SourceExpr valueType, SourceExpr aspectRef, String methodName) {
    return mb.call(
        mb.field(
            mb.field(
                mb.call(
                    mb.evalAspectLookup(),
                    mb.tupleOf(
                        valueType,
                        aspectRef,
                        mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                mb.str("value")),
            mb.str(methodName)),
        mb.recordOf(
            TList.of(
                new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                new TestMb.RecordPair("cv", mb.field(mb.scopeUse("arg"), mb.str("cv"))))));
  }

  private static SourceExpr delegateNull(TestMb mb, SourceExpr underlying, SourceExpr aspectRef, String methodName) {
    return mb.call(
        mb.field(
            mb.field(
                mb.call(
                    mb.evalAspectLookup(),
                    mb.tupleOf(
                        underlying,
                        aspectRef,
                        mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                mb.str("value")),
            mb.str(methodName)),
        mb.recordOf(
            TList.of(
                new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                new TestMb.RecordPair("cv", mb.field(mb.scopeUse("arg"), mb.str("cv"))))));
  }

  private static SourceExpr delegateNullXxOnly(TestMb mb, SourceExpr underlying, SourceExpr aspectRef, String methodName) {
    return mb.call(
        mb.field(
            mb.field(
                mb.call(
                    mb.evalAspectLookup(),
                    mb.tupleOf(
                        underlying,
                        aspectRef,
                        mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                mb.str("value")),
            mb.str(methodName)),
        mb.recordOf(
            TList.of(
                new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))))));
  }

  private static SourceExpr evenOddDropBody(TestMb mb) {
    var underlyingCodeExpr =
        mb.field(
            mb.call(
                mb.field(
                    mb.field(
                        mb.call(
                            mb.evalAspectLookup(),
                            mb.tupleOf(
                                mb.typesNull(),
                                mb.evalAspectDropDynamicLight(),
                                mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                        mb.str("value")),
                    mb.str("drop_dynamic_light")),
                mb.recordOf(
                    TList.of(
                        new TestMb.RecordPair(
                            "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                        new TestMb.RecordPair(
                            "loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                        new TestMb.RecordPair(
                            "cv", mb.field(mb.scopeUse("arg"), mb.str("cv")))))),
            mb.str("value"));
    return mb.seqOf(
        mb.bind("underlying_code_", underlyingCodeExpr),
        mb.return_(
            mb.call(
                mb.field(mb.call(mb.typesEresult(), mb.typesCode()), mb.str("ok")),
                mb.field(
                    mb.call(
                        mb.evalConstEval(),
                        mb.recordOf(
                            TList.of(
                                new TestMb.RecordPair(
                                    "xx_", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                new TestMb.RecordPair(
                                    "staged",
                                    mb.stage(
                                        mb.return_(mb.graft(mb.scopeUse("underlying_code_")))))))),
                    mb.str("code")))));
  }

  private static SourceExpr evenOddBindBody(TestMb mb) {
    var intBindCall =
        mb.call(
            mb.field(
                mb.field(
                    mb.call(
                        mb.evalAspectLookup(),
                        mb.tupleOf(
                            mb.typesInt(),
                            mb.evalAspectBind(),
                            mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                    mb.str("value")),
                mb.str("bind")),
            mb.recordOf(
                TList.of(
                    new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                    new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                    new TestMb.RecordPair("cv", mb.field(mb.scopeUse("arg"), mb.str("cv"))))));
    return mb.seqOf(
        mb.bind("int_bind_er", intBindCall),
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
                                                "state", mb.scopeUse("inner"))))))))))));
  }

  private static SourceExpr castFromPassthrough(TestMb mb) {
    return mb.call(
        mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("ok")),
        mb.field(mb.scopeUse("arg"), mb.str("cv")));
  }

  private static SourceExpr evenOddCastFromBody(TestMb mb) {
    var wrapped =
        mb.call(
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
                                                                        mb.field(
                                                                            mb.scopeUse(
                                                                                "int_aspect_opt_"),
                                                                            mb.str("value")),
                                                                        mb.str("get")),
                                                                    mb.recordOf(
                                                                        TList.of(
                                                                            new TestMb.RecordPair(
                                                                                "value",
                                                                                mb.scopeUse(
                                                                                    "val_"))))),
                                                                mb.num(2.0))))))),
                                        new TestMb.RecordPair(
                                            "source",
                                            mb.call(
                                                mb.lower(mb.scopeUse("vs_expr")),
                                                mb.literalNull()))))))))));
    return mb.seqOf(
        mb.bind(
            "val_opt_",
            mb.call(
                mb.field(mb.field(mb.scopeUse("arg"), mb.str("cv")), mb.str("value")),
                mb.recordOf(TList.of()))),
        mb.return_(
            mb.ifOf(
                TList.of(
                    new TestMb.Branch(
                        mb.field(mb.scopeUse("val_opt_"), mb.str("is_some")),
                        mb.seqOf(
                            mb.bind("val_", mb.field(mb.scopeUse("val_opt_"), mb.str("value"))),
                            mb.bind(
                                "int_aspect_opt_",
                                mb.call(
                                    mb.evalAspectLookup(),
                                    mb.tupleOf(
                                        mb.call(
                                            mb.field(mb.scopeUse("val_"), mb.str("type")),
                                            mb.recordOf(TList.of())),
                                        mb.evalAspectInt(),
                                        mb.field(mb.scopeUse("arg"), mb.str("xx"))))),
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
                                                        mb.str("value"))),
                                                mb.return_(wrapped)))),
                                    castFromPassthrough(mb)))))),
                castFromPassthrough(mb))));
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

  private static SourceExpr evenOddAddBody(TestMb mb) {
    var constIntParity =
        mb.operatorMod(
            mb.operatorAdd(
                mb.field(mb.scopeUse("inner"), mb.str("parity")),
                mb.call(
                    mb.field(
                        mb.field(mb.scopeUse("ref_int_aspect_opt_"), mb.str("value")),
                        mb.str("get")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("value", mb.scopeUse("ref_val_")))))),
            mb.num(2.0));
    var evenOddParity =
        mb.operatorMod(
            mb.operatorAdd(
                mb.field(mb.scopeUse("inner"), mb.str("parity")),
                mb.call(
                    mb.field(mb.scopeUse("ref_evenodd_type_"), mb.str("get_parity")),
                    mb.literalNull())),
            mb.num(2.0));
    return mb.seqOf(
        mb.bind(
            "sum_er_",
            mb.call(
                mb.field(
                    mb.field(
                        mb.call(
                            mb.evalAspectLookup(),
                            mb.tupleOf(
                                mb.typesInt(),
                                mb.evalOperatorAdd(),
                                mb.field(mb.scopeUse("arg"), mb.str("xx")))),
                        mb.str("value")),
                    mb.str("add")),
                mb.recordOf(
                    TList.of(
                        new TestMb.RecordPair("xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                        new TestMb.RecordPair("loc", mb.field(mb.scopeUse("arg"), mb.str("loc"))),
                        new TestMb.RecordPair(
                            "base", mb.field(mb.scopeUse("arg"), mb.str("base"))),
                        new TestMb.RecordPair(
                            "reference", mb.field(mb.scopeUse("arg"), mb.str("reference"))))))),
        mb.bind("sum_cv_", mb.field(mb.scopeUse("sum_er_"), mb.str("value"))),
        mb.bind(
            "ref_opt_",
            mb.call(
                mb.field(
                    mb.field(mb.scopeUse("arg"), mb.str("reference")), mb.str("value")),
                mb.recordOf(TList.of()))),
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
                                    mb.field(mb.scopeUse("ref_val_"), mb.str("type")),
                                    mb.recordOf(TList.of()))),
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
                                    mb.field(mb.scopeUse("ref_type_obj_"), mb.str("aspect")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                            new TestMb.RecordPair(
                                                "key", mb.evalTypeIntDynamicIdentity()))))),
                            mb.return_(
                                mb.ifOf(
                                    TList.of(
                                        new TestMb.Branch(
                                            mb.field(
                                                mb.scopeUse("ref_int_aspect_opt_"),
                                                mb.str("is_some")),
                                            evenOddAddWrapped(mb, constIntParity)),
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
                                    mb.seqOf(
                                        mb.bind(
                                            "ref_evenodd_type_",
                                            mb.call(
                                                mb.mustReinterpret(),
                                                mb.tupleOf(
                                                    mb.scopeUse("ref_type_obj_"),
                                                    mb.lower(mb.scopeUse("u_type"))))),
                                        mb.return_(evenOddAddWrapped(mb, evenOddParity)))))))),
                mb.call(
                    mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("err")),
                    mb.tupleOf(
                        mb.field(mb.scopeUse("arg"), mb.str("loc")),
                        mb.str("operator_add: reference has no compile-time value"))))));
  }

  private static SourceExpr evenOddSetApply(TestMb mb, SourceExpr parity) {
    return mb.seqOf(
        mb.bind(
            "self_val_",
            mb.field(
                mb.call(
                    mb.field(
                        mb.field(mb.scopeUse("arg"), mb.str("self")), mb.str("value")),
                    mb.recordOf(TList.of())),
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
                            mb.set(
                                mb.field(
                                    mb.field(mb.scopeUse("sv"), mb.str("state")),
                                    mb.str("parity")),
                                parity),
                            mb.return_(
                                mb.call(
                                    mb.field(
                                        mb.field(
                                            mb.call(
                                                mb.evalAspectLookup(),
                                                mb.tupleOf(
                                                    mb.typesInt(),
                                                    mb.evalAspectSet(),
                                                    mb.field(
                                                        mb.scopeUse("arg"), mb.str("xx")))),
                                            mb.str("value")),
                                        mb.str("set")),
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
                                                    mb.field(
                                                        mb.field(
                                                            mb.scopeUse("sv"), mb.str("int")),
                                                        mb.str("use")),
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
                                                                "code",
                                                                mb.field(
                                                                    mb.field(
                                                                        mb.scopeUse("arg"),
                                                                        mb.str("self")),
                                                                    mb.str("code"))))))),
                                            new TestMb.RecordPair(
                                                "value",
                                                mb.field(mb.scopeUse("arg"), mb.str("value")))))))))),
                mb.call(
                    mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("err")),
                    mb.tupleOf(
                        mb.field(mb.scopeUse("arg"), mb.str("loc")),
                        mb.str("set: cannot set a non-binding evenodd value"))))));
  }

  private static SourceExpr evenOddSetBody(TestMb mb) {
    var constIntParity =
        mb.operatorMod(
            mb.call(
                mb.field(
                    mb.field(mb.scopeUse("rhs_int_aspect_opt_"), mb.str("value")),
                    mb.str("get")),
                mb.recordOf(
                    TList.of(new TestMb.RecordPair("value", mb.scopeUse("rhs_val_"))))),
            mb.num(2.0));
    var evenOddParity =
        mb.call(
            mb.field(mb.scopeUse("rhs_evenodd_type_"), mb.str("get_parity")),
            mb.literalNull());
    return mb.seqOf(
        mb.bind(
            "rhs_opt_",
            mb.call(
                mb.field(
                    mb.field(mb.scopeUse("arg"), mb.str("value")), mb.str("value")),
                mb.recordOf(TList.of()))),
        mb.return_(
            mb.ifOf(
                TList.of(
                    new TestMb.Branch(
                        mb.field(mb.scopeUse("rhs_opt_"), mb.str("is_some")),
                        mb.seqOf(
                            mb.bind("rhs_val_", mb.field(mb.scopeUse("rhs_opt_"), mb.str("value"))),
                            mb.bind(
                                "rhs_type_obj_",
                                mb.call(
                                    mb.field(mb.scopeUse("rhs_val_"), mb.str("type")),
                                    mb.recordOf(TList.of()))),
                            mb.bind(
                                "rhs_int_aspect_opt_",
                                mb.call(
                                    mb.evalAspectLookup(),
                                    mb.tupleOf(
                                        mb.scopeUse("rhs_type_obj_"),
                                        mb.evalAspectInt(),
                                        mb.field(mb.scopeUse("arg"), mb.str("xx"))))),
                            mb.bind(
                                "rhs_int_dynamic_opt_",
                                mb.call(
                                    mb.field(mb.scopeUse("rhs_type_obj_"), mb.str("aspect")),
                                    mb.recordOf(
                                        TList.of(
                                            new TestMb.RecordPair(
                                                "xx", mb.field(mb.scopeUse("arg"), mb.str("xx"))),
                                            new TestMb.RecordPair(
                                                "key", mb.evalTypeIntDynamicIdentity()))))),
                            mb.return_(
                                mb.ifOf(
                                    TList.of(
                                        new TestMb.Branch(
                                            mb.field(
                                                mb.scopeUse("rhs_int_aspect_opt_"),
                                                mb.str("is_some")),
                                            evenOddSetApply(mb, constIntParity)),
                                        new TestMb.Branch(
                                            mb.field(
                                                mb.scopeUse("rhs_int_dynamic_opt_"),
                                                mb.str("is_some")),
                                            mb.call(
                                                mb.field(
                                                    mb.call(mb.typesEresult(), mb.typesCve()),
                                                    mb.str("err")),
                                                mb.tupleOf(
                                                    mb.field(mb.scopeUse("arg"), mb.str("loc")),
                                                    mb.str(
                                                        "set: value is a dynamic int; parity"
                                                            + " unknown"))))),
                                    mb.seqOf(
                                        mb.bind(
                                            "rhs_evenodd_type_",
                                            mb.call(
                                                mb.mustReinterpret(),
                                                mb.tupleOf(
                                                    mb.scopeUse("rhs_type_obj_"),
                                                    mb.lower(mb.scopeUse("u_type"))))),
                                        mb.return_(evenOddSetApply(mb, evenOddParity)))))))),
                mb.call(
                    mb.field(mb.call(mb.typesEresult(), mb.typesCve()), mb.str("err")),
                    mb.tupleOf(
                        mb.field(mb.scopeUse("arg"), mb.str("loc")),
                        mb.str("set: value has no compile-time value"))))));
  }

  private static SourceExpr makeEvenOddTypeInner(TestMb mb, String name) {
    return mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "vs",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("union")),
                        mb.tupleOf())),
                mb.bind(
                    "vs_expr",
                    mb.call(
                        mb.field(mb.scopeUse("vs"), mb.str("variant")),
                        mb.recordOf(
                            TList.of(
                                new TestMb.RecordPair("tag", mb.str("expr")),
                                new TestMb.RecordPair("type_ref", mb.typesNull()))))),
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
                                                "int", mb.evalBinding()),
                                            new TestMb.RecordPair(
                                                "state",
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "parity", mb.typesInt()))))))))))),
                mb.bind(
                    "u",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("object")),
                        mb.str(name))),
                mb.call(
                    mb.field(mb.scopeUse("u"), mb.str("set_inner")),
                    mb.recordOf(
                        TList.of(new TestMb.RecordPair("parity", mb.typesInt())))),
                mb.bind(
                    "u_gp_method",
                    mb.call(mb.field(mb.scopeUse("u"), mb.str("func")), mb.str("get_parity"))),
                mb.call(
                    mb.field(mb.scopeUse("u_gp_method"), mb.str("impl")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("arg", mb.typesNull()),
                            new TestMb.RecordPair("ret", mb.typesInt()),
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
                                "ret", mb.field(mb.scopeUse("vs"), mb.str("type"))),
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
                                                mb.typesInt(),
                                                mb.evalAspectConsumeDynamic(),
                                                "to_dynamic")),
                                        new TestMb.RecordPair(
                                            "jvm_class",
                                            delegateNullXxOnly(
                                                mb,
                                                mb.typesInt(),
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
                                            "parity", mb.typesInt()))))))),
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
                            new TestMb.RecordPair("ret", mb.typesInt()),
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
                                                mb.typesInt(),
                                                mb.evalAspectConsumeDynamic(),
                                                "to_dynamic")),
                                        new TestMb.RecordPair(
                                            "jvm_class",
                                            delegateNullXxOnly(
                                                mb,
                                                mb.typesInt(),
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
                                                mb.bind(
                                                    "int_use_cv_",
                                                    mb.call(
                                                        mb.field(
                                                            mb.field(
                                                                mb.scopeUse("inner"),
                                                                mb.str("int")),
                                                            mb.str("use")),
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
                                                                    "code",
                                                                    mb.field(
                                                                        mb.scopeUse("arg"),
                                                                        mb.str("code"))))))),
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
                                            "fork", mb.scopeUse("outer")),
                                        new TestMb.RecordPair(
                                            "merge",
                                            mb.call(
                                                mb.field(
                                                    mb.call(
                                                        mb.typesEresult(), mb.evalBinding()),
                                                    mb.str("ok")),
                                                mb.scopeUse("outer"))))))))),
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
                                            mb.seqOf(
                                                mb.return_(
                                                    mb.operatorEq(mb.num(0.0), mb.num(1.0))))),
                                        new TestMb.RecordPair(
                                            "can_cast_from",
                                            mb.seqOf(
                                                mb.return_(
                                                    mb.operatorEq(mb.num(1.0), mb.num(1.0))))),
                                        new TestMb.RecordPair(
                                            "cast_from", evenOddCastFromBody(mb)),
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
                                                mb.call(
                                                    mb.field(
                                                        mb.call(mb.typesOption(), mb.typesAspect()),
                                                        mb.str("none")),
                                                    mb.literalNull()))),
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
                                            "bind", evenOddBindBody(mb)))))))),
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
                                            delegateVia(
                                                mb,
                                                mb.typesInt(),
                                                mb.evalAspectBindDynamic(),
                                                "bind_dynamic")))))))),
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
                                            "drop", evenOddDropBody(mb)))))))),
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
                                                mb.typesInt(),
                                                mb.evalAspectConsumeDynamic(),
                                                "to_dynamic")),
                                        new TestMb.RecordPair(
                                            "jvm_class",
                                            delegateNullXxOnly(
                                                mb,
                                                mb.typesInt(),
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
                                            "set", evenOddSetBody(mb)))))))),
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
                                            "add", evenOddAddBody(mb)))))))),
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
                                                                mb.scopeUse("outer"))))))),
                                        new TestMb.RecordPair(
                                            "fork", mb.scopeUse("outer")),
                                        new TestMb.RecordPair(
                                            "merge",
                                            mb.call(
                                                mb.field(
                                                    mb.call(
                                                        mb.typesEresult(), mb.evalBinding()),
                                                    mb.str("ok")),
                                                mb.scopeUse("outer"))))))))),
                mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.scopeUse("even_type_new")));
  }

  private static TList<SourceExpr> bindEvenOddType(TestMb mb, String name) {
    return TList.of(
        mb.bind(
            "even_type",
            mb.lower(
                mb.seqOf(
                    mb.bind("even_type_new", mb.lower(makeEvenOddTypeInner(mb, name))),
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

  private static Compile.CompileResult compileOk(TMap<String, SourceModule> modules) {
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    return ok;
  }

  @Test
  public void test00100DefineEvenOddType() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.call(mb.log(), mb.str("defined")))));
    var res = compileOk(modules);
    assertTrue(
        res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
  }

  @Test
  public void test00200CastAndBind() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind(
                    "a",
                    mb.as_(mb.as_(mb.num(2.0), mb.typesInt()), mb.scopeUse("even_type"))),
                mb.call(mb.log(), mb.str("bound")))));
    var res = compileOk(modules);
    assertTrue(
        res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
  }

  @Test
  public void test00300CastAndBindWithSet() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind(
                    "a",
                    mb.as_(mb.as_(mb.num(2.0), mb.typesInt()), mb.scopeUse("even_type"))),
                mb.set(
                    mb.scopeUse("a"),
                    mb.as_(mb.num(4.0), mb.scopeUse("even_type"))),
                mb.call(mb.log(), mb.str("set-done")))));
    compileOk(modules);
  }

  @Test
  public void test00400BindUseInLog() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind(
                    "a",
                    mb.as_(mb.as_(mb.num(2.0), mb.typesInt()), mb.scopeUse("even_type"))),
                mb.bind("b", mb.scopeUse("a")),
                mb.call(mb.log(), mb.str("used")))));
    var res = compileOk(modules);
    assertTrue(
        res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
  }

  @Test
  public void test00500CastConstInt() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                mb.call(mb.log(), mb.str("cast-const")))));
    var res = compileOk(modules);
    assertTrue(
        res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
  }

  @Test
  public void test00600AddConstInt() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                mb.bind("b", mb.operatorAdd(mb.scopeUse("a"), mb.num(3.0))),
                mb.call(mb.log(), mb.str("add-const")))));
    var res = compileOk(modules);
    assertTrue(
        res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
  }

  @Test
  public void test00700AddEvenOdd() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                mb.bind("b", mb.as_(mb.num(4.0), mb.scopeUse("even_type"))),
                mb.bind("c", mb.operatorAdd(mb.scopeUse("a"), mb.scopeUse("b"))),
                mb.call(mb.log(), mb.str("add-evenodd")))));
    var res = compileOk(modules);
    assertTrue(
        res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
  }

  @Test
  public void test00900SetConstInt() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                mb.set(mb.scopeUse("a"), mb.num(4.0)),
                mb.call(mb.log(), mb.str("set-const")))));
    var res = compileOk(modules);
    assertTrue(
        res.logs().size() >= 1, () -> "expected at least one log entry, got " + res.logs());
  }

  @Test
  public void test01000SetDynamicIntErrors() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                mb.set(mb.scopeUse("a"), mb.as_(mb.num(4.0), mb.typesInt())),
                mb.call(mb.log(), mb.str("unreachable")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>, () -> "expected compile error, got " + res);
    assertTrue(
        res.toString().contains("set: value is a dynamic int"),
        () -> "expected dynamic-int set error, got " + res);
  }

  @Test
  public void test00800AddDynamicIntErrors() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main",
        new SourceModule.V1(
            moduleSeq(
                mb,
                bindEvenOddType(mb, "EvenOdd"),
                mb.bind("a", mb.as_(mb.num(2.0), mb.scopeUse("even_type"))),
                mb.bind(
                    "c",
                    mb.operatorAdd(
                        mb.scopeUse("a"), mb.as_(mb.num(3.0), mb.typesInt()))),
                mb.call(mb.log(), mb.str("unreachable")))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Err<?, ?>, () -> "expected compile error, got " + res);
    assertTrue(
        res.toString().contains("operator_add: reference is a dynamic int"),
        () -> "expected dynamic-int parity error, got " + res);
  }
}
