package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoWebglUniforms {
    private EcoWebglUniforms() {}

    private static final String TARGET = "uniforms";

    static final IList<String> UGFN_KINDS = IList.of("wasm_fn", "glsl_fn");

    private static int ugfnKind(String kind) {
        for (int i = 0; i < UGFN_KINDS.size(); i++) {
            if (UGFN_KINDS.get(i).equals(kind)) return i;
        }
        throw new IllegalArgumentException("no uniforms definition kind " + kind);
    }

    private static SourceExpr staged(EcoMb e, SourceExpr expr) {
        return unwrapCveM(e, constEvalIn(e, xx(e), e.stage(expr)));
    }

    private static SourceExpr field(EcoMb e, String key) {
        return unwrapCveM(e, e.callM(e.v("fields"), "get", e.s(key)));
    }

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        return EcoTargetMb.fnDef(e, out, TARGET, name, argType, ret);
    }

    private static SourceExpr constStr(EcoMb e, SourceExpr cv) {
        return bim(
                e,
                e.mb.evalAspectStringConst(),
                "get",
                lookupV(e, xx(e), cveType(e, cv), e.mb.evalAspectStringConst()),
                cveValue(e, cv));
    }

    private static SourceExpr intCve(EcoMb e, SourceExpr value) {
        return staged(e, e.lower(e.mb.as(value, intT(e))));
    }

    private static SourceExpr elemType(EcoMb e, SourceExpr kind) {
        return e.callM(
                e.mapNew(
                        str(e),
                        ty(e),
                        e.p("float", e.lowerV("t_f32")),
                        e.p("uint", e.lowerV("t_u32")),
                        e.p("int", e.lowerV("t_i32")),
                        e.p("bool", e.lowerV("t_i32"))),
                "get",
                kind);
    }

    private static SourceExpr place(EcoMb e, SourceExpr selfCv, SourceExpr kind, SourceExpr byteOffset) {
        return e.seq(
                e.let("__et", elemType(e, kind)),
                e.let("__pt", e.call(e.lowerV("c_ptr"), e.v("__et"))),
                e.let(
                        "__pv",
                        unwrapCveM(
                                e,
                                castValue(
                                        e,
                                        xx(e),
                                        loc(e),
                                        e.v("__pt"),
                                        unwrapCveM(e, tagValue(e, selfCv, e.lowerV("t_gpv")))))),
                e.ret(unwrapCveM(
                        e,
                        bim(
                                e,
                                aspIface(e, "field"),
                                "field",
                                lookupV(e, xx(e), e.v("__pt"), aspIface(e, "field")),
                                e.rec(
                                        e.p("xx", xx(e)),
                                        e.p("loc", loc(e)),
                                        e.p("self", e.v("__pv")),
                                        e.p("key", intCve(e, e.mb.operatorDiv(byteOffset, e.intOf(4)))))))));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var x = new TList<SourceExpr>();
        x.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        x.add(e.let("glsl", e.importLocal("glsl", "glsl-es-3.00.atz")));
        for (var t : new String[] {"f32", "i32", "u32", "void"}) x.add(e.let("t_" + t, e.fld(e.v("wasm"), t)));
        x.add(e.let("c_ptr", e.fld(e.v("wasm"), "ptr")));
        x.add(e.let("t_gpv", e.call(e.fld(e.v("wasm"), "global_t"), e.call(e.v("c_ptr"), e.v("t_void")))));
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        defIface(e, x, "ufield_m");
        defIface(e, x, "ublock_m", e.p("name", str(e)));
        defIface(e, x, "uvec_m", e.p("offset", intT(e)), e.p("kind", str(e)), e.p("n", intT(e)), e.p("mat", boolT(e)));
        defIface(e, x, "ugfn_m", e.p("kind", intT(e)));
        defObj(
                e,
                x,
                "ufield",
                "std140 field",
                e.rec(
                        e.p("name", str(e)),
                        e.p("ty", ty(e)),
                        e.p("offset", intT(e)),
                        e.p("kind", str(e)),
                        e.p("n", intT(e)),
                        e.p("mat", boolT(e))));
        for (var f : new String[] {"name", "kind"}) accessor(e, x, "ufield", f, str(e));
        for (var f : new String[] {"offset", "n"}) accessor(e, x, "ufield", f, intT(e));
        accessor(e, x, "ufield", "ty", ty(e));
        accessor(e, x, "ufield", "mat", boolT(e));
        markerType(e, x, "ufield", "ufield", "ufield_m", e.aspectDispatch(lv(e, "ufield_m_id")));
        x.add(e.implInterface(e.v("ufield"), t(e, "ufield_m")));
        defObj(
                e,
                x,
                "ut_block",
                "std140 block in wasm memory",
                e.rec(e.p("name", str(e)), e.p("size", intT(e)), e.p("fields", e.listType(t(e, "ufield")))));
        defObj(
                e,
                x,
                "ut_vec",
                "std140 vector or matrix field",
                e.rec(e.p("offset", intT(e)), e.p("kind", str(e)), e.p("n", intT(e)), e.p("mat", boolT(e))));
        defObj(e, x, "ugfn_t", "uniforms definition function", e.rec(e.p("kind", intT(e))));

        fnDef(
                e,
                x,
                "h_layout",
                e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("cv", cveT(e))),
                e.rec(e.p("size", intT(e)), e.p("fields", e.listType(t(e, "ufield")))));
        fnThrows(e, "h_layout", errsT(e));
        fnDef(
                e,
                x,
                "h_elements",
                e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("cv", cveT(e))),
                e.listType(cveT(e)));
        fnThrows(e, "h_elements", errsT(e));
        fnDef(e, x, "h_align", e.rec(e.p("v", intT(e)), e.p("a", intT(e))), intT(e));
        fnImpl(
                e,
                x,
                "h_align",
                body(
                        e,
                        e.ret(e.mb.operatorMul(
                                e.mb.operatorDiv(
                                        e.mb.operatorAdd(a(e, "v"), e.mb.operatorSub(a(e, "a"), e.intOf(1))),
                                        a(e, "a")),
                                a(e, "a")))));
        fnImpl(
                e,
                x,
                "h_elements",
                body(
                        e,
                        e.let("els", e.listNew(e.mb.typesCve())),
                        e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "argument has no value")),
                        e.let("tk", lookup(e, xx(e), cveType(e, a(e, "cv")), aspIface(e, "tuple"))),
                        e.ifOnly(
                                e.not(e.fld(e.v("tk"), "is_some")),
                                throwErr(e, loc(e), "uniforms: fields must be a tuple of (name, type) tuples")),
                        e.foreach(
                                bim(
                                        e,
                                        aspIface(e, "tuple"),
                                        "take_elements",
                                        e.fld(e.v("tk"), "value"),
                                        e.rec(
                                                e.p("xx", xx(e)),
                                                e.p("loc", loc(e)),
                                                e.p("value", cveValue(e, a(e, "cv"))))),
                                "er",
                                push(e, e.v("els"), unwrapCveT(e, e.v("er")))),
                        e.ret(e.v("els"))));
        fnImpl(
                e,
                x,
                "h_layout",
                body(
                        e,
                        e.let("fields", e.listNew(lt(e, "ufield"))),
                        e.let("cur", e.rec(e.p("v", e.intOf(0)))),
                        e.foreach(
                                hf(e, "h_elements", e.p("xx", xx(e)), e.p("loc", loc(e)), e.p("cv", a(e, "cv"))),
                                "pc",
                                e.seq(
                                        e.let(
                                                "pair",
                                                hf(
                                                        e,
                                                        "h_elements",
                                                        e.p("xx", xx(e)),
                                                        e.p("loc", loc(e)),
                                                        e.p("cv", e.v("pc")))),
                                        countLet(e, "n2"),
                                        countLoop(e, "n2", e.v("pair")),
                                        e.ifOnly(
                                                e.not(e.eq(e.fld(e.v("n2"), "v"), num(e, 2))),
                                                throwErr(e, loc(e), "uniforms: a field is a (name, type) tuple")),
                                        e.let("i", e.rec(e.p("v", e.intOf(0)))),
                                        e.let("fname", e.rec(e.p("v", e.dstr("")))),
                                        e.foreach(
                                                e.v("pair"),
                                                "pe",
                                                e.seq(
                                                        e.ifOnly(
                                                                e.eq(e.fld(e.v("i"), "v"), num(e, 0)),
                                                                e.seq(
                                                                        e.ifOnly(
                                                                                e.not(
                                                                                        hasB(
                                                                                                e,
                                                                                                xx(e),
                                                                                                cveType(e, e.v("pe")),
                                                                                                e.mb
                                                                                                        .evalAspectStringConst())),
                                                                                throwErr(
                                                                                        e,
                                                                                        loc(e),
                                                                                        "uniforms: field name must be a constant string")),
                                                                        e.mb.set(
                                                                                e.fld(e.v("fname"), "v"),
                                                                                constStr(e, e.v("pe"))))),
                                                        e.ifOnly(
                                                                e.eq(e.fld(e.v("i"), "v"), num(e, 1)),
                                                                e.seq(
                                                                        e.let(
                                                                                "tk",
                                                                                lookup(
                                                                                        e,
                                                                                        xx(e),
                                                                                        cveType(e, e.v("pe")),
                                                                                        aspIface(e, "type"))),
                                                                        e.ifOnly(
                                                                                orB(
                                                                                        e,
                                                                                        isRuntime(e, e.v("pe")),
                                                                                        e.not(
                                                                                                e.fld(
                                                                                                        e.v("tk"),
                                                                                                        "is_some"))),
                                                                                throwErr(
                                                                                        e,
                                                                                        loc(e),
                                                                                        "uniforms: field type must be a constant glsl type")),
                                                                        e.let(
                                                                                "fty",
                                                                                bim(
                                                                                        e,
                                                                                        aspIface(e, "type"),
                                                                                        "type",
                                                                                        e.fld(e.v("tk"), "value"),
                                                                                        cveValue(e, e.v("pe")))),
                                                                        e.let(
                                                                                "d",
                                                                                e.call(
                                                                                        e.lower(
                                                                                                e.fld(
                                                                                                        e.v("glsl"),
                                                                                                        "desc")),
                                                                                        e.rec(
                                                                                                e.p("xx", xx(e)),
                                                                                                e.p(
                                                                                                        "ty",
                                                                                                        e.v("fty"))))),
                                                                        e.let("n", e.fld(e.v("d"), "n")),
                                                                        e.let(
                                                                                "align",
                                                                                e.ifElse(
                                                                                        orB(
                                                                                                e,
                                                                                                e.fld(e.v("d"), "mat"),
                                                                                                e.mb.operatorGe(
                                                                                                        e.v("n"),
                                                                                                        num(e, 3))),
                                                                                        e.intOf(16),
                                                                                        e.ifElse(
                                                                                                e.eq(
                                                                                                        e.v("n"),
                                                                                                        num(e, 2)),
                                                                                                e.intOf(8),
                                                                                                e.intOf(4)))),
                                                                        e.let(
                                                                                "size",
                                                                                e.ifElse(
                                                                                        e.fld(e.v("d"), "mat"),
                                                                                        e.mb.operatorMul(
                                                                                                e.v("n"), e.intOf(16)),
                                                                                        e.mb.operatorMul(
                                                                                                e.v("n"), e.intOf(4)))),
                                                                        e.let(
                                                                                "offset",
                                                                                hf(
                                                                                        e,
                                                                                        "h_align",
                                                                                        e.p(
                                                                                                "v",
                                                                                                e.fld(e.v("cur"), "v")),
                                                                                        e.p("a", e.v("align")))),
                                                                        e.mb.set(
                                                                                e.fld(e.v("cur"), "v"),
                                                                                e.mb.operatorAdd(
                                                                                        e.v("offset"), e.v("size"))),
                                                                        push(
                                                                                e,
                                                                                e.v("fields"),
                                                                                mk(
                                                                                        e,
                                                                                        "ufield",
                                                                                        e.p(
                                                                                                "name",
                                                                                                e.fld(
                                                                                                        e.v("fname"),
                                                                                                        "v")),
                                                                                        e.p("ty", e.v("fty")),
                                                                                        e.p("offset", e.v("offset")),
                                                                                        e.p(
                                                                                                "kind",
                                                                                                e.fld(
                                                                                                        e.v("d"),
                                                                                                        "kind")),
                                                                                        e.p("n", e.v("n")),
                                                                                        e.p(
                                                                                                "mat",
                                                                                                e.fld(
                                                                                                        e.v("d"),
                                                                                                        "mat")))))),
                                                        e.incr(e.fld(e.v("i"), "v")))))),
                        e.ret(e.rec(
                                e.p("size", hf(e, "h_align", e.p("v", e.fld(e.v("cur"), "v")), e.p("a", e.intOf(16)))),
                                e.p("fields", e.v("fields"))))));

        {
            selfType(
                    e,
                    x,
                    "ut_block",
                    e.cat(e.s("std140 block "), in(e, "name")),
                    rtDispatch(e, str(e), e.aspectDispatch(lv(e, "ublock_m_id"), aspId(e, "field"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "ublock_m"),
                                    e.eq(in(e, "name"), acc(e, xx(e), a(e, "other"), "ublock_m", "name"))))));
            x.add(e.implInterface(e.v("ut_block"), t(e, "ublock_m"), e.p("name", in(e, "name"))));
            rtLiftImpl(e, x, "ut_block");
            rtBindImpl(e, x, "ut_block", str(e));
            rtUseImpl(e, x, "ut_block");
            x.add(e.implInterface(
                    e.v("ut_block"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.ifOnly(
                                            e.not(hasB(
                                                    e, xx(e), cveType(e, a(e, "key")), e.mb.evalAspectStringConst())),
                                            retM(e, errCve(e, loc(e), "uniforms: field must be a constant string"))),
                                    e.let("key", constStr(e, a(e, "key"))),
                                    e.ifOnly(
                                            e.eq(e.v("key"), e.s("ptr")),
                                            retM(e, tagValue(e, a(e, "self"), e.lowerV("t_gpv")))),
                                    e.ifOnly(
                                            e.eq(e.v("key"), e.s("size")), retM(e, okCve(e, intCve(e, in(e, "size"))))),
                                    e.foreach(
                                            in(e, "fields"),
                                            "f",
                                            e.ifOnly(
                                                    e.eq(e.callM(e.v("f"), "name", e.tup()), e.v("key")),
                                                    e.seq(
                                                            e.ifOnly(
                                                                    andB(
                                                                            e,
                                                                            e.eq(
                                                                                    e.callM(e.v("f"), "n", e.tup()),
                                                                                    num(e, 1)),
                                                                            e.not(e.callM(e.v("f"), "mat", e.tup()))),
                                                                    retM(
                                                                            e,
                                                                            okCve(
                                                                                    e,
                                                                                    place(
                                                                                            e,
                                                                                            a(e, "self"),
                                                                                            e.callM(
                                                                                                    e.v("f"), "kind",
                                                                                                    e.tup()),
                                                                                            e.callM(
                                                                                                    e.v("f"), "offset",
                                                                                                    e.tup()))))),
                                                            retM(
                                                                    e,
                                                                    tagValue(
                                                                            e,
                                                                            a(e, "self"),
                                                                            mk(
                                                                                    e,
                                                                                    "ut_vec",
                                                                                    e.p(
                                                                                            "offset",
                                                                                            e.callM(
                                                                                                    e.v("f"), "offset",
                                                                                                    e.tup())),
                                                                                    e.p(
                                                                                            "kind",
                                                                                            e.callM(
                                                                                                    e.v("f"), "kind",
                                                                                                    e.tup())),
                                                                                    e.p(
                                                                                            "n",
                                                                                            e.callM(
                                                                                                    e.v("f"), "n",
                                                                                                    e.tup())),
                                                                                    e.p(
                                                                                            "mat",
                                                                                            e.callM(
                                                                                                    e.v("f"), "mat",
                                                                                                    e.tup())))))))),
                                    e.ret(errCve(
                                            e,
                                            loc(e),
                                            e.cat(
                                                    e.s("uniforms: block "),
                                                    in(e, "name"),
                                                    e.s(" has no field "),
                                                    e.v("key")))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));
        }
        {
            selfType(
                    e,
                    x,
                    "ut_vec",
                    e.cat(e.s("std140 "), in(e, "kind"), e.s(" field at "), toStr(e, in(e, "offset"))),
                    rtDispatch(e, str(e), e.aspectDispatch(lv(e, "uvec_m_id"), aspId(e, "field"), aspId(e, "set"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "uvec_m"),
                                    e.eq(in(e, "offset"), acc(e, xx(e), a(e, "other"), "uvec_m", "offset"))))));
            x.add(e.implInterface(
                    e.v("ut_vec"),
                    t(e, "uvec_m"),
                    e.p("offset", in(e, "offset")),
                    e.p("kind", in(e, "kind")),
                    e.p("n", in(e, "n")),
                    e.p("mat", in(e, "mat"))));
            rtLiftImpl(e, x, "ut_vec");
            rtBindImpl(e, x, "ut_vec", str(e));
            rtUseImpl(e, x, "ut_vec");
            var elemOffset = e.ifElse(
                    in(e, "mat"),
                    e.mb.operatorAdd(
                            in(e, "offset"),
                            e.mb.operatorAdd(
                                    e.mb.operatorMul(e.mb.operatorDiv(e.v("idx"), in(e, "n")), e.intOf(16)),
                                    e.mb.operatorMul(e.mb.operatorMod(e.v("idx"), in(e, "n")), e.intOf(4)))),
                    e.mb.operatorAdd(in(e, "offset"), e.mb.operatorMul(e.v("idx"), e.intOf(4))));
            var count = e.ifElse(in(e, "mat"), e.mb.operatorMul(in(e, "n"), in(e, "n")), in(e, "n"));
            x.add(e.implInterface(
                    e.v("ut_vec"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.ifOnly(
                                            e.not(isConst(e, xx(e), cveType(e, a(e, "key")))),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            "uniforms: component index must be a constant integer"))),
                                    e.let(
                                            "idx",
                                            e.mb.as(
                                                    e.call(
                                                            e.evalBuiltin("const_numeric_to_double"),
                                                            e.rec(
                                                                    e.p("xx", xx(e)),
                                                                    e.p("value", cveValue(e, a(e, "key"))))),
                                                    intT(e))),
                                    e.ifOnly(
                                            orB(
                                                    e,
                                                    e.mb.operatorLt(e.v("idx"), num(e, 0)),
                                                    e.mb.operatorGe(e.v("idx"), count)),
                                            retM(e, errCve(e, loc(e), "uniforms: component index out of range"))),
                                    e.ret(okCve(e, place(e, a(e, "self"), in(e, "kind"), elemOffset))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));
            x.add(e.implInterface(
                    e.v("ut_vec"),
                    e.mb.evalAspectSet(),
                    e.p(
                            "set",
                            body(
                                    e,
                                    e.let(
                                            "elems",
                                            unwrapM(
                                                    e,
                                                    tryEr(
                                                            e,
                                                            hf(
                                                                    e,
                                                                    "h_elements",
                                                                    e.p("xx", xx(e)),
                                                                    e.p("loc", loc(e)),
                                                                    e.p("cv", a(e, "value"))),
                                                            e.listType(cveT(e))),
                                                    cveT(e))),
                                    countLet(e, "ne"),
                                    countLoop(e, "ne", e.v("elems")),
                                    e.ifOnly(
                                            e.not(e.eq(e.fld(e.v("ne"), "v"), count)),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("uniforms: field takes a tuple of "),
                                                                    toStr(e, count),
                                                                    e.s(" components"))))),
                                    e.let("out", e.listNew(cveT(e))),
                                    e.let("idxc", e.rec(e.p("v", e.intOf(0)))),
                                    e.foreach(
                                            e.v("elems"),
                                            "ec",
                                            e.seq(
                                                    e.let("idx", e.fld(e.v("idxc"), "v")),
                                                    e.let("pl", place(e, a(e, "self"), in(e, "kind"), elemOffset)),
                                                    e.let("et", elemType(e, in(e, "kind"))),
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            unwrapCveM(
                                                                    e,
                                                                    bim(
                                                                            e,
                                                                            e.mb.evalAspectSet(),
                                                                            "set",
                                                                            lookupV(
                                                                                    e,
                                                                                    xx(e),
                                                                                    e.v("et"),
                                                                                    e.mb.evalAspectSet()),
                                                                            e.rec(
                                                                                    e.p("xx", xx(e)),
                                                                                    e.p("loc", loc(e)),
                                                                                    e.p("self", e.v("pl")),
                                                                                    e.p("value", e.v("ec")))))),
                                                    e.incr(e.fld(e.v("idxc"), "v")))),
                                    e.let("codes", e.listNew(codeT(e))),
                                    e.foreach(e.v("out"), "c", push(e, e.v("codes"), cveCode(e, e.v("c")))),
                                    e.ret(okCve(
                                            e,
                                            cvNo(
                                                    e,
                                                    unwrapM(
                                                            e,
                                                            bim(
                                                                    e,
                                                                    e.mb.evalTarget(),
                                                                    "seq",
                                                                    e.fld(xx(e), "target"),
                                                                    e.rec(
                                                                            e.p("loc", loc(e)),
                                                                            e.p("entries", e.v("codes")))),
                                                            cveT(e)))))))));
        }
        {
            selfType(
                    e,
                    x,
                    "ugfn_t",
                    e.cat(e.s("uniforms definition function "), toStr(e, in(e, "kind"))),
                    rtDispatch(e, str(e), e.aspectDispatch(lv(e, "ugfn_m_id"), aspId(e, "function"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "ugfn_m"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "ugfn_m", "kind"), in(e, "kind"))))));
            rtLiftImpl(e, x, "ugfn_t");
            rtBindImpl(e, x, "ugfn_t", str(e));
            rtUseImpl(e, x, "ugfn_t");
            x.add(e.implInterface(e.v("ugfn_t"), t(e, "ugfn_m"), e.p("kind", in(e, "kind"))));
            x.add(e.implInterface(
                    e.v("ugfn_t"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let("argv", a(e, "argument")),
                                    e.ifOnly(
                                            e.not(cveHas(e, e.v("argv"))),
                                            retM(e, errCve(e, loc(e), "uniforms definition argument has no value"))),
                                    e.let("rk", lookup(e, xx(e), cveType(e, e.v("argv")), aspIface(e, "record"))),
                                    e.ifOnly(
                                            e.not(e.fld(e.v("rk"), "is_some")),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            "uniforms definition argument must be {mod, name, fields}"))),
                                    e.let(
                                            "fields",
                                            bim(
                                                    e,
                                                    aspIface(e, "record"),
                                                    "take_fields",
                                                    e.fld(e.v("rk"), "value"),
                                                    e.rec(
                                                            e.p("xx", xx(e)),
                                                            e.p("loc", loc(e)),
                                                            e.p("value", cveValue(e, e.v("argv")))))),
                                    e.let("modcv", field(e, "mod")),
                                    e.let("namecv", staged(e, graft(e, field(e, "name")))),
                                    e.ifOnly(
                                            isRuntime(e, e.v("namecv")),
                                            retM(e, errCve(e, loc(e), "uniforms block name must be constant"))),
                                    e.let("fieldscv", field(e, "fields")),
                                    e.let("block_fn", e.lower(e.fld(e.v("glsl"), "uniform_block_fn"))),
                                    e.let("alloc_fn", e.lower(e.fld(e.v("wasm"), "alloc_fn"))),
                                    e.ifOnly(
                                            e.eq(in(e, "kind"), num(e, ugfnKind("glsl_fn"))),
                                            retM(
                                                    e,
                                                    okCve(
                                                            e,
                                                            staged(
                                                                    e,
                                                                    e.call(
                                                                            e.lower(e.v("block_fn")),
                                                                            e.rec(
                                                                                    e.p("mod", graft(e, e.v("modcv"))),
                                                                                    e.p(
                                                                                            "name",
                                                                                            graft(e, e.v("namecv"))),
                                                                                    e.p("layout", e.s("std140")),
                                                                                    e.p(
                                                                                            "fields",
                                                                                            graft(
                                                                                                    e,
                                                                                                    e.v(
                                                                                                            "fieldscv"))))))))),
                                    e.let(
                                            "layout",
                                            unwrapM(
                                                    e,
                                                    tryEr(
                                                            e,
                                                            hf(
                                                                    e,
                                                                    "h_layout",
                                                                    e.p("xx", xx(e)),
                                                                    e.p("loc", loc(e)),
                                                                    e.p("cv", e.v("fieldscv"))),
                                                            e.rec(
                                                                    e.p("size", intT(e)),
                                                                    e.p("fields", e.listType(lt(e, "ufield"))))),
                                                    cveT(e))),
                                    e.let(
                                            "refcv",
                                            staged(
                                                    e,
                                                    e.call(
                                                            e.lower(e.v("alloc_fn")),
                                                            e.rec(
                                                                    e.p("mod", graft(e, e.v("modcv"))),
                                                                    e.p("name", graft(e, e.v("namecv"))),
                                                                    e.p(
                                                                            "size",
                                                                            e.lower(e.fld(e.v("layout"), "size"))))))),
                                    e.ret(tagValue(
                                            e,
                                            e.v("refcv"),
                                            mk(
                                                    e,
                                                    "ut_block",
                                                    e.p("name", constStr(e, e.v("namecv"))),
                                                    e.p("size", e.fld(e.v("layout"), "size")),
                                                    e.p("fields", e.fld(e.v("layout"), "fields"))))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));
        }
        x.add(e.finish(e.v("d")));
        var defs = new TList<EcoMb.P>();
        defs.add(e.p("ugfn_t_new", e.fld(e.v("ugfn_t"), "new")));
        x.add(e.ret(e.rec(defs.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(x))));
        var exports = new TList<EcoMb.P>();
        for (var kind : UGFN_KINDS) {
            exports.add(e.p(
                    kind,
                    e.mb.as(
                            e.call(e.fld(e.v("k"), "ugfn_t_new"), e.rec(e.p("kind", e.intOf(ugfnKind(kind))))),
                            ty(e))));
        }
        top.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return top;
    }
}
