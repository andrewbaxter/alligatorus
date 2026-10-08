package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoGlsl {
    private final EcoMb e;

    private EcoGlsl(EcoMb e) {
        this.e = e;
    }

    private static final String TARGET = "glsl";

    static final IList<String> KINDS = IList.of("float", "int", "uint", "bool");

    static final IList<String> GGFN_KINDS = IList.of("func_fn", "in_fn", "out_fn", "uniform_block_fn", "gl_fn");

    static final IList<IList<String>> GL_VARS = IList.of(
            IList.of("gl_Position", "float", "4"),
            IList.of("gl_PointSize", "float", "1"),
            IList.of("gl_VertexID", "int", "1"),
            IList.of("gl_InstanceID", "int", "1"),
            IList.of("gl_FragCoord", "float", "4"),
            IList.of("gl_FrontFacing", "bool", "1"),
            IList.of("gl_PointCoord", "float", "2"),
            IList.of("gl_FragDepth", "float", "1"));

    static final IList<IList<String>> BUILTINS = IList.of(
            IList.of("radians", "G G"),
            IList.of("degrees", "G G"),
            IList.of("sin", "G G"),
            IList.of("cos", "G G"),
            IList.of("tan", "G G"),
            IList.of("asin", "G G"),
            IList.of("acos", "G G"),
            IList.of("atan", "G G G", "G G"),
            IList.of("sinh", "G G"),
            IList.of("cosh", "G G"),
            IList.of("tanh", "G G"),
            IList.of("asinh", "G G"),
            IList.of("acosh", "G G"),
            IList.of("atanh", "G G"),
            IList.of("pow", "G G G"),
            IList.of("exp", "G G"),
            IList.of("log", "G G"),
            IList.of("exp2", "G G"),
            IList.of("log2", "G G"),
            IList.of("sqrt", "G G"),
            IList.of("inversesqrt", "G G"),
            IList.of("abs", "G G", "GI GI"),
            IList.of("sign", "G G", "GI GI"),
            IList.of("floor", "G G"),
            IList.of("trunc", "G G"),
            IList.of("round", "G G"),
            IList.of("roundEven", "G G"),
            IList.of("ceil", "G G"),
            IList.of("fract", "G G"),
            IList.of("mod", "G G G", "G G F"),
            IList.of("min", "G G G", "G G F", "GI GI GI", "GI GI I", "GU GU GU", "GU GU U"),
            IList.of("max", "G G G", "G G F", "GI GI GI", "GI GI I", "GU GU GU", "GU GU U"),
            IList.of("clamp", "G G G G", "G G F F", "GI GI GI GI", "GI GI I I", "GU GU GU GU", "GU GU U U"),
            IList.of("mix", "G G G G", "G G G F", "G G G GB"),
            IList.of("step", "G G G", "G F G"),
            IList.of("smoothstep", "G G G G", "G F F G"),
            IList.of("isnan", "GB G"),
            IList.of("isinf", "GB G"),
            IList.of("floatBitsToInt", "GI G"),
            IList.of("floatBitsToUint", "GU G"),
            IList.of("intBitsToFloat", "G GI"),
            IList.of("uintBitsToFloat", "G GU"),
            IList.of("packSnorm2x16", "U F2"),
            IList.of("packUnorm2x16", "U F2"),
            IList.of("packHalf2x16", "U F2"),
            IList.of("unpackSnorm2x16", "F2 U"),
            IList.of("unpackUnorm2x16", "F2 U"),
            IList.of("unpackHalf2x16", "F2 U"),
            IList.of("length", "F G"),
            IList.of("distance", "F G G"),
            IList.of("dot", "F G G"),
            IList.of("cross", "F3 F3 F3"),
            IList.of("normalize", "G G"),
            IList.of("faceforward", "G G G G"),
            IList.of("reflect", "G G G"),
            IList.of("refract", "G G G F"),
            IList.of("matrixCompMult", "M M M"),
            IList.of("outerProduct", "M V V"),
            IList.of("transpose", "M M"),
            IList.of("determinant", "F M"),
            IList.of("inverse", "M M"),
            IList.of("lessThan", "BV V V", "BV IV IV", "BV UV UV"),
            IList.of("lessThanEqual", "BV V V", "BV IV IV", "BV UV UV"),
            IList.of("greaterThan", "BV V V", "BV IV IV", "BV UV UV"),
            IList.of("greaterThanEqual", "BV V V", "BV IV IV", "BV UV UV"),
            IList.of("equal", "BV V V", "BV IV IV", "BV UV UV", "BV BV BV"),
            IList.of("notEqual", "BV V V", "BV IV IV", "BV UV UV", "BV BV BV"),
            IList.of("any", "B BV"),
            IList.of("all", "B BV"),
            IList.of("not", "BV BV"),
            IList.of("dFdx", "G G"),
            IList.of("dFdy", "G G"),
            IList.of("fwidth", "G G"));

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        return EcoTargetMb.fnDef(e, out, TARGET, name, argType, ret);
    }

    private static SourceExpr gmodT(EcoMb e) {
        return t(e, "gmod");
    }

    private static SourceExpr renderT(EcoMb e) {
        return e.rec(e.p("glsl", str(e)));
    }

    private static SourceExpr descT(EcoMb e) {
        return e.rec(e.p("kind", str(e)), e.p("n", intT(e)), e.p("mat", boolT(e)));
    }

    private static SourceExpr instsT(EcoMb e) {
        return e.listType(t(e, "ginst"));
    }

    private static SourceExpr instsNew(EcoMb e) {
        return e.listNew(lt(e, "ginst"));
    }

    private static SourceExpr inst(EcoMb e, SourceExpr text) {
        return mk(e, "ginst", e.p("text", text));
    }

    private static SourceExpr gvalNew(EcoMb e, SourceExpr ty, SourceExpr expr) {
        return mk(e, "gval", e.p("ty", ty), e.p("expr", expr));
    }

    private static SourceExpr gvExpr(EcoMb e, SourceExpr cveTP) {
        return e.callM(reinterpret(e, cveValue(e, cveTP), "gval"), "expr", e.tup());
    }

    private static SourceExpr appendCode(EcoMb e, SourceExpr out, SourceExpr codeTP) {
        return hf(e, "h_append", e.p("out", out), e.p("code", codeTP));
    }

    private static SourceExpr codeOf(EcoMb e, SourceExpr insts) {
        return hf(e, "h_code", insts);
    }

    private static SourceExpr fresh(EcoMb e, SourceExpr mod) {
        return e.callM(mod, "fresh", e.tup());
    }

    private static SourceExpr xmod(EcoMb e) {
        return xmod(e, e.mb.typesCve());
    }

    private static SourceExpr xmod(EcoMb e, SourceExpr retInner) {
        return e.callM(
                unwrapM(
                        e,
                        e.call(e.mb.reinterpret(), e.tup(loc(e), e.fld(xx(e), "target"), lt(e, "gtarget"))),
                        retInner),
                "mod",
                e.tup());
    }

    private static SourceExpr operand(EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr ty, SourceExpr cv) {
        return hf(e, "h_operand", e.p("xx", xx), e.p("loc", loc), e.p("ty", ty), e.p("cv", cv));
    }

    private static SourceExpr operandM(EcoMb e, SourceExpr ty, SourceExpr cv) {
        return unwrapCveM(e, tryEr(e, operand(e, xx(e), loc(e), ty, cv), cveT(e)));
    }

    private static SourceExpr isGlsl(EcoMb e, SourceExpr xx, SourceExpr cv) {
        return hf(e, "h_is_glsl", e.p("xx", xx), e.p("cv", cv));
    }

    private static SourceExpr irOf(EcoMb e, SourceExpr xx, SourceExpr ty) {
        return hf(e, "h_ir", e.p("xx", xx), e.p("ty", ty));
    }

    private static SourceExpr descOf(EcoMb e, SourceExpr xx, SourceExpr ty) {
        return hf(e, "h_desc", e.p("xx", xx), e.p("ty", ty));
    }

    private static SourceExpr typeOf(EcoMb e, SourceExpr desc) {
        return hf(e, "h_type_of", desc);
    }

    private static SourceExpr scalarT(EcoMb e, SourceExpr kind) {
        return mk(e, "gt_scalar", e.p("kind", kind));
    }

    private static SourceExpr scalarT(EcoMb e, String kind) {
        return scalarT(e, e.s(kind));
    }

    private static SourceExpr vecT(EcoMb e, SourceExpr kind, SourceExpr n) {
        return mk(e, "gt_vec", e.p("kind", kind), e.p("n", n));
    }

    private static SourceExpr arith(EcoMb e, String op) {
        return tryEr(
                e,
                hf(
                        e,
                        "h_arith",
                        e.p("xx", xx(e)),
                        e.p("loc", loc(e)),
                        e.p("mod", xmod(e)),
                        e.p("op", e.s(op)),
                        e.p("self", outer(e)),
                        e.p("a", a(e, "base")),
                        e.p("b", a(e, "reference"))),
                cveT(e));
    }

    private static SourceExpr cmp(EcoMb e, String op) {
        return tryEr(
                e,
                hf(
                        e,
                        "h_cmp",
                        e.p("xx", xx(e)),
                        e.p("loc", loc(e)),
                        e.p("mod", xmod(e)),
                        e.p("op", e.s(op)),
                        e.p("self", outer(e)),
                        e.p("a", a(e, "base")),
                        e.p("b", a(e, "reference"))),
                cveT(e));
    }

    private static SourceExpr unary(EcoMb e, String op) {
        return tryEr(
                e,
                hf(
                        e,
                        "h_unop",
                        e.p("xx", xx(e)),
                        e.p("loc", loc(e)),
                        e.p("mod", xmod(e)),
                        e.p("op", e.s(op)),
                        e.p("ty", outer(e)),
                        e.p("a", a(e, "base"))),
                cveT(e));
    }

    private static SourceExpr ggfnIs(EcoMb e, String kind) {
        return e.eq(in(e, "kind"), num(e, ggfnKind(kind)));
    }

    private static int ggfnKind(String kind) {
        for (int i = 0; i < GGFN_KINDS.size(); i++) {
            if (GGFN_KINDS.get(i).equals(kind)) return i;
        }
        throw new IllegalArgumentException("no glsl definition kind " + kind);
    }

    private static SourceExpr ggfnStaged(EcoMb e, SourceExpr staged) {
        return unwrapCveM(e, constEvalIn(e, xx(e), e.stage(staged)));
    }

    private static SourceExpr ggfnField(EcoMb e, String key) {
        return unwrapCveM(e, e.callM(e.v("fields"), "get", e.s(key)));
    }

    private static SourceExpr ggfnMod(EcoMb e) {
        return e.callM(graft(e, e.v("modcv")), "m", e.tup());
    }

    private static SourceExpr globalT(EcoMb e, SourceExpr base) {
        return mk(e, "gt_global", e.p("base", base));
    }

    private static SourceExpr constStr(EcoMb e, SourceExpr cv) {
        return hf(e, "h_const_str", e.p("xx", xx(e)), e.p("cv", cv));
    }

    private static SourceExpr elements(EcoMb e, SourceExpr argv) {
        return hf(e, "h_elements", e.p("xx", xx(e)), e.p("loc", loc(e)), e.p("cv", argv));
    }

    private static SourceExpr elementsM(EcoMb e, SourceExpr argv) {
        return unwrapM(e, tryEr(e, elements(e, argv), e.listType(e.mb.typesCve())), cveT(e));
    }

    void defArith(TList<SourceExpr> x) {
        fnImpl(
                e,
                x,
                "h_arith",
                body(
                        e,
                        e.let("out", instsNew(e)),
                        e.let("av", operand(e, xx(e), loc(e), a(e, "self"), a(e, "a"))),
                        appendCode(e, e.v("out"), cveCode(e, e.v("av"))),
                        e.let("ad", descOf(e, xx(e), a(e, "self"))),
                        e.ifOnly(e.not(cveHas(e, a(e, "b"))), throwErr(e, loc(e), "operand has no value")),
                        e.let("bt0", cveType(e, a(e, "b"))),
                        e.let(
                                "bt",
                                e.ifElse(
                                        has(e, xx(e), e.v("bt0"), "gtype"),
                                        e.ifElse(
                                                has(e, xx(e), e.v("bt0"), "ggtype_m"),
                                                acc(e, xx(e), e.v("bt0"), "ggtype_m", "base"),
                                                e.v("bt0")),
                                        e.mb.as(scalarT(e, e.fld(e.v("ad"), "kind")), ty(e)))),
                        e.let("bv", operand(e, xx(e), loc(e), e.v("bt"), a(e, "b"))),
                        appendCode(e, e.v("out"), cveCode(e, e.v("bv"))),
                        e.let("bd", descOf(e, xx(e), e.v("bt"))),
                        e.ifOnly(
                                e.not(e.eq(e.fld(e.v("ad"), "kind"), e.fld(e.v("bd"), "kind"))),
                                throwErr(
                                        e,
                                        loc(e),
                                        e.cat(
                                                e.s("glsl: operands of "),
                                                a(e, "op"),
                                                e.s(" have different component types: "),
                                                tyDesc(e, a(e, "self")),
                                                e.s(" and "),
                                                tyDesc(e, e.v("bt"))))),
                        e.let("an", e.fld(e.v("ad"), "n")),
                        e.let("bn", e.fld(e.v("bd"), "n")),
                        e.let("am", e.fld(e.v("ad"), "mat")),
                        e.let("bm", e.fld(e.v("bd"), "mat")),
                        e.let("rc", e.rec(e.p("v", e.optionTypeLoweredNone()))),
                        e.ifOnly(
                                orB(
                                        e,
                                        isEqual(e, xx(e), a(e, "self"), e.v("bt")),
                                        orB(
                                                e,
                                                e.eq(e.v("bn"), num(e, 1)),
                                                andB(
                                                        e,
                                                        e.eq(a(e, "op"), e.s("*")),
                                                        andB(
                                                                e,
                                                                e.eq(e.v("an"), e.v("bn")),
                                                                andB(e, e.v("am"), e.not(e.v("bm"))))))),
                                e.mb.set(e.fld(e.v("rc"), "v"), e.optionTypeLoweredSome(a(e, "self")))),
                        e.ifOnly(
                                andB(
                                        e,
                                        e.not(e.fld(e.v("rc"), "v", "is_some")),
                                        orB(
                                                e,
                                                e.eq(e.v("an"), num(e, 1)),
                                                andB(
                                                        e,
                                                        e.eq(a(e, "op"), e.s("*")),
                                                        andB(
                                                                e,
                                                                e.eq(e.v("an"), e.v("bn")),
                                                                andB(e, e.v("bm"), e.not(e.v("am"))))))),
                                e.mb.set(e.fld(e.v("rc"), "v"), e.optionTypeLoweredSome(e.v("bt")))),
                        e.ifOnly(
                                e.not(e.fld(e.v("rc"), "v", "is_some")),
                                throwErr(
                                        e,
                                        loc(e),
                                        e.cat(
                                                e.s("glsl: cannot apply "),
                                                a(e, "op"),
                                                e.s(" to "),
                                                tyDesc(e, a(e, "self")),
                                                e.s(" and "),
                                                tyDesc(e, e.v("bt"))))),
                        e.let("res", e.fld(e.v("rc"), "v", "value")),
                        e.ret(hf(
                                e,
                                "h_stmt",
                                e.p("mod", a(e, "mod")),
                                e.p("out", e.v("out")),
                                e.p("ty", e.v("res")),
                                e.p(
                                        "expr",
                                        e.cat(
                                                gvExpr(e, e.v("av")),
                                                e.s(" "),
                                                a(e, "op"),
                                                e.s(" "),
                                                gvExpr(e, e.v("bv"))))))));
        fnImpl(
                e,
                x,
                "h_cmp",
                body(
                        e,
                        e.let("out", instsNew(e)),
                        e.let("av", operand(e, xx(e), loc(e), a(e, "self"), a(e, "a"))),
                        appendCode(e, e.v("out"), cveCode(e, e.v("av"))),
                        e.let("bv", operand(e, xx(e), loc(e), a(e, "self"), a(e, "b"))),
                        appendCode(e, e.v("out"), cveCode(e, e.v("bv"))),
                        e.ret(hf(
                                e,
                                "h_stmt",
                                e.p("mod", a(e, "mod")),
                                e.p("out", e.v("out")),
                                e.p("ty", e.mb.as(scalarT(e, "bool"), ty(e))),
                                e.p(
                                        "expr",
                                        e.cat(
                                                gvExpr(e, e.v("av")),
                                                e.s(" "),
                                                a(e, "op"),
                                                e.s(" "),
                                                gvExpr(e, e.v("bv"))))))));
        fnImpl(
                e,
                x,
                "h_unop",
                body(
                        e,
                        e.let("out", instsNew(e)),
                        e.let("av", operand(e, xx(e), loc(e), a(e, "ty"), a(e, "a"))),
                        appendCode(e, e.v("out"), cveCode(e, e.v("av"))),
                        e.ret(hf(
                                e,
                                "h_stmt",
                                e.p("mod", a(e, "mod")),
                                e.p("out", e.v("out")),
                                e.p("ty", a(e, "ty")),
                                e.p("expr", e.cat(a(e, "op"), gvExpr(e, e.v("av"))))))));
        fnImpl(
                e,
                x,
                "h_construct",
                body(
                        e,
                        e.let("mod", a(e, "mod")),
                        e.let("td", descOf(e, xx(e), a(e, "ty"))),
                        e.let("elems", elements(e, a(e, "argument"))),
                        e.let("out", instsNew(e)),
                        e.let("exprs", e.listNew(str(e))),
                        e.let("st", e.rec(e.p("n", e.intOf(0)), e.p("comps", e.intOf(0)), e.p("mat", e.intOf(0)))),
                        e.foreach(
                                e.v("elems"),
                                "ec",
                                e.seq(
                                        e.ifOnly(
                                                e.not(cveHas(e, e.v("ec"))),
                                                throwErr(e, loc(e), "argument has no value")),
                                        e.let("et0", cveType(e, e.v("ec"))),
                                        e.let(
                                                "et",
                                                e.ifElse(
                                                        has(e, xx(e), e.v("et0"), "gtype"),
                                                        e.ifElse(
                                                                has(e, xx(e), e.v("et0"), "ggtype_m"),
                                                                acc(e, xx(e), e.v("et0"), "ggtype_m", "base"),
                                                                e.v("et0")),
                                                        e.mb.as(scalarT(e, e.fld(e.v("td"), "kind")), ty(e)))),
                                        e.let("ev", operand(e, xx(e), loc(e), e.v("et"), e.v("ec"))),
                                        appendCode(e, e.v("out"), cveCode(e, e.v("ev"))),
                                        push(e, e.v("exprs"), gvExpr(e, e.v("ev"))),
                                        e.let("ed", descOf(e, xx(e), e.v("et"))),
                                        e.ifOnly(
                                                e.fld(e.v("ed"), "mat"), e.mb.set(e.fld(e.v("st"), "mat"), e.intOf(1))),
                                        e.mb.set(
                                                e.fld(e.v("st"), "comps"),
                                                e.mb.operatorAdd(
                                                        e.fld(e.v("st"), "comps"),
                                                        hf(e, "h_components", e.p("xx", xx(e)), e.p("ty", e.v("et"))))),
                                        e.incr(e.fld(e.v("st"), "n")))),
                        e.let("want", hf(e, "h_components", e.p("xx", xx(e)), e.p("ty", a(e, "ty")))),
                        e.let("n", e.fld(e.v("st"), "n")),
                        e.let("comps", e.fld(e.v("st"), "comps")),
                        e.let(
                                "ok",
                                e.ifElse(
                                        e.eq(e.v("n"), num(e, 1)),
                                        orB(
                                                e,
                                                e.eq(e.v("comps"), num(e, 1)),
                                                orB(
                                                        e,
                                                        e.mb.operatorGe(e.v("comps"), e.v("want")),
                                                        e.eq(e.fld(e.v("st"), "mat"), num(e, 1)))),
                                        andB(
                                                e,
                                                e.eq(e.v("comps"), e.v("want")),
                                                e.eq(e.fld(e.v("st"), "mat"), num(e, 0))))),
                        e.ifOnly(
                                e.not(e.v("ok")),
                                throwErr(
                                        e,
                                        loc(e),
                                        e.cat(
                                                e.s("glsl: "),
                                                tyDesc(e, a(e, "ty")),
                                                e.s(" constructor needs "),
                                                toStr(e, e.v("want")),
                                                e.s(" components but got "),
                                                toStr(e, e.v("comps"))))),
                        e.ret(hf(
                                e,
                                "h_stmt",
                                e.p("mod", e.v("mod")),
                                e.p("out", e.v("out")),
                                e.p("ty", a(e, "ty")),
                                e.p(
                                        "expr",
                                        e.cat(
                                                irOf(e, xx(e), a(e, "ty")),
                                                e.s("("),
                                                e.join(e.v("exprs"), ", "),
                                                e.s(")")))))));
    }

    private void bindImpl(TList<SourceExpr> x, String obj) {
        var brT = e.mb.typesBindResult();
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectBind(),
                e.p(
                        "bind",
                        body(
                                e,
                                e.let(
                                        "v",
                                        unwrapM(
                                                e,
                                                tryEr(e, operand(e, xx(e), loc(e), outer(e), a(e, "cv")), cveT(e)),
                                                brT)),
                                e.let("out", instsNew(e)),
                                appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                e.let("r", fresh(e, xmod(e, brT))),
                                push(
                                        e,
                                        e.v("out"),
                                        inst(
                                                e,
                                                e.cat(
                                                        irOf(e, xx(e), outer(e)),
                                                        e.s(" "),
                                                        e.v("r"),
                                                        e.s(" = "),
                                                        gvExpr(e, e.v("v")),
                                                        e.s(";")))),
                                e.ret(e.eresultOk(
                                        brT,
                                        e.call(
                                                e.mb.evalBindResult(),
                                                e.rec(
                                                        e.p("code", codeOf(e, e.v("out"))),
                                                        e.p(
                                                                "binding",
                                                                mk(
                                                                        e,
                                                                        "gbinding",
                                                                        e.p("ty", outer(e)),
                                                                        e.p("name", e.v("r"))))))))))));
    }

    private void setImpl(TList<SourceExpr> x, String obj) {
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectSet(),
                e.p(
                        "set",
                        body(
                                e,
                                e.let("t", operandM(e, outer(e), a(e, "self"))),
                                e.let("v", operandM(e, outer(e), a(e, "value"))),
                                e.let("out", instsNew(e)),
                                appendCode(e, e.v("out"), cveCode(e, e.v("t"))),
                                appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                push(
                                        e,
                                        e.v("out"),
                                        inst(e, e.cat(gvExpr(e, e.v("t")), e.s(" = "), gvExpr(e, e.v("v")), e.s(";")))),
                                e.ret(okCve(e, cvNo(e, codeOf(e, e.v("out")))))))));
    }

    private void constructImpl(TList<SourceExpr> x, String obj) {
        x.add(e.implInterface(
                e.v(obj),
                aspIface(e, "function"),
                e.p(
                        "call",
                        body(
                                e,
                                e.ret(tryEr(
                                        e,
                                        hf(
                                                e,
                                                "h_construct",
                                                e.p("xx", xx(e)),
                                                e.p("loc", loc(e)),
                                                e.p("mod", xmod(e)),
                                                e.p("ty", outer(e)),
                                                e.p("argument", a(e, "argument"))),
                                        cveT(e))))),
                e.p("exception_type", e.optionTypeLoweredNone())));
    }

    private SourceExpr vecIr(SourceExpr kind, SourceExpr n) {
        return e.cat(
                e.ifElse(
                        e.eq(kind, e.s("float")),
                        e.dstr(""),
                        e.ifElse(
                                e.eq(kind, e.s("int")),
                                e.dstr("i"),
                                e.ifElse(e.eq(kind, e.s("uint")), e.dstr("u"), e.dstr("b")))),
                e.s("vec"),
                toStr(e, n));
    }

    void defTypes(TList<SourceExpr> x) {
        var numOps = new String[][] {{"add", "+"}, {"sub", "-"}, {"mul", "*"}, {"div", "/"}};
        var intOps =
                new String[][] {{"mod", "%"}, {"and", "&"}, {"or", "|"}, {"xor", "^"}, {"shl", "<<"}, {"shr", ">>"}};
        var cmpOps = new String[][] {{"lt", "<"}, {"le", "<="}, {"gt", ">"}, {"ge", ">="}};
        var eqOps = new String[][] {{"eq", "=="}, {"ne", "!="}};
        var boolOps = new String[][] {{"and", "&&"}, {"or", "||"}};
        {
            var ids = new TList<SourceExpr>();
            ids.add(lv(e, "gtype_id"));
            ids.add(lv(e, "gscalar_id"));
            for (var op : numOps) ids.add(opId(e, op[0]));
            for (var op : intOps) ids.add(opId(e, op[0]));
            for (var op : cmpOps) ids.add(opId(e, op[0]));
            for (var op : eqOps) ids.add(opId(e, op[0]));
            ids.add(opId(e, "neg"));
            ids.add(opId(e, "bit_not"));
            ids.add(aspId(e, "bind"));
            ids.add(aspId(e, "set"));
            ids.add(aspId(e, "function"));
            selfType(
                    e,
                    x,
                    "gt_scalar",
                    in(e, "kind"),
                    e.aspectDispatch(ids.toArray(SourceExpr[]::new)),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    e.ifElse(
                                            e.eq(in(e, "kind"), e.s("bool")),
                                            hasB(e, xx(e), srcType(e), e.evalBuiltin("aspect_bool_const")),
                                            isConst(e, xx(e), srcType(e)))))),
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(isEqual(e, xx(e), outer(e), e.v("st")), retM(e, okCve(e, a(e, "cv")))),
                            e.seq(
                                    e.ifOnly(
                                            e.eq(in(e, "kind"), e.s("bool")),
                                            e.seq(
                                                    e.ifOnly(
                                                            e.not(
                                                                    hasB(
                                                                            e,
                                                                            xx(e),
                                                                            e.v("st"),
                                                                            e.evalBuiltin("aspect_bool_const"))),
                                                            retM(e, errCve(e, loc(e), "glsl: expected a boolean"))),
                                                    retM(
                                                            e,
                                                            okCve(
                                                                    e,
                                                                    cvOf(
                                                                            e,
                                                                            cveCode(e, a(e, "cv")),
                                                                            gvalNew(
                                                                                    e,
                                                                                    outer(e),
                                                                                    e.ifElse(
                                                                                            bim(
                                                                                                    e,
                                                                                                    e.evalBuiltin(
                                                                                                            "aspect_bool_const"),
                                                                                                    "get",
                                                                                                    lookupV(
                                                                                                            e,
                                                                                                            xx(e),
                                                                                                            e.v("st"),
                                                                                                            e
                                                                                                                    .evalBuiltin(
                                                                                                                            "aspect_bool_const")),
                                                                                                    cveValue(
                                                                                                            e,
                                                                                                            a(
                                                                                                                    e,
                                                                                                                    "cv"))),
                                                                                            e.dstr("true"),
                                                                                            e.dstr("false")))))))),
                                    e.ifOnly(
                                            e.not(isConst(e, xx(e), e.v("st"))),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("glsl: "),
                                                                    tyDesc(e, e.v("st")),
                                                                    e.s(" cannot convert to "),
                                                                    tyDesc(e, outer(e)))))),
                                    e.let(
                                            "d",
                                            e.call(
                                                    e.evalBuiltin("const_numeric_to_double"),
                                                    e.rec(e.p("xx", xx(e)), e.p("value", cveValue(e, a(e, "cv")))))),
                                    e.ifOnly(
                                            e.eq(in(e, "kind"), e.s("float")),
                                            retM(
                                                    e,
                                                    okCve(
                                                            e,
                                                            cvOf(
                                                                    e,
                                                                    cveCode(e, a(e, "cv")),
                                                                    gvalNew(
                                                                            e,
                                                                            outer(e),
                                                                            hf(e, "h_float_lit", e.v("d"))))))),
                                    e.let("l", e.mb.as(e.v("d"), e.mb.typesLong())),
                                    e.ifOnly(
                                            e.not(e.eq(e.mb.as(e.v("l"), e.typesDouble()), e.v("d"))),
                                            retM(e, errCve(e, loc(e), "glsl: integer literal is not integral"))),
                                    e.ifOnly(
                                            andB(
                                                    e,
                                                    e.eq(in(e, "kind"), e.s("uint")),
                                                    e.mb.operatorLt(e.v("d"), num(e, 0))),
                                            retM(e, errCve(e, loc(e), "glsl: uint literal is negative"))),
                                    retM(
                                            e,
                                            okCve(
                                                    e,
                                                    cvOf(
                                                            e,
                                                            cveCode(e, a(e, "cv")),
                                                            gvalNew(
                                                                    e,
                                                                    outer(e),
                                                                    e.cat(
                                                                            toStr(e, e.v("l")),
                                                                            e.ifElse(
                                                                                    e.eq(in(e, "kind"), e.s("uint")),
                                                                                    e.dstr("u"),
                                                                                    e.dstr(""))))))))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "gscalar"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "gscalar", "kind"), in(e, "kind"))))));
            x.add(e.implInterface(e.v("gt_scalar"), t(e, "gtype"), e.p("ir", in(e, "kind"))));
            x.add(e.implInterface(e.v("gt_scalar"), t(e, "gscalar"), e.p("kind", in(e, "kind"))));
            for (var op : numOps) {
                op(
                        e,
                        x,
                        "gt_scalar",
                        op[0],
                        e.ifElse(
                                e.eq(in(e, "kind"), e.s("bool")),
                                errCve(e, loc(e), "glsl: arithmetic on bool"),
                                arith(e, op[1])));
            }
            for (var op : intOps) {
                var text = op[1];
                SourceExpr onBool = errCve(e, loc(e), "glsl: " + text + " on bool");
                for (var bop : boolOps) if (bop[0].equals(op[0])) onBool = arith(e, bop[1]);
                op(
                        e,
                        x,
                        "gt_scalar",
                        op[0],
                        e.ifElse(
                                e.eq(in(e, "kind"), e.s("bool")),
                                onBool,
                                e.ifElse(
                                        e.eq(in(e, "kind"), e.s("float")),
                                        errCve(e, loc(e), "glsl: " + text + " on float"),
                                        arith(e, text))));
            }
            for (var op : cmpOps) {
                op(
                        e,
                        x,
                        "gt_scalar",
                        op[0],
                        e.ifElse(
                                e.eq(in(e, "kind"), e.s("bool")),
                                errCve(e, loc(e), "glsl: ordering comparison on bool"),
                                cmp(e, op[1])));
            }
            for (var op : eqOps) op(e, x, "gt_scalar", op[0], cmp(e, op[1]));
            op(
                    e,
                    x,
                    "gt_scalar",
                    "neg",
                    e.ifElse(
                            e.eq(in(e, "kind"), e.s("bool")),
                            errCve(e, loc(e), "glsl: negating a bool"),
                            unary(e, "-")));
            op(
                    e,
                    x,
                    "gt_scalar",
                    "bit_not",
                    e.ifElse(
                            e.eq(in(e, "kind"), e.s("bool")),
                            unary(e, "!"),
                            e.ifElse(
                                    e.eq(in(e, "kind"), e.s("float")),
                                    errCve(e, loc(e), "glsl: bitwise not on float"),
                                    unary(e, "~"))));
            bindImpl(x, "gt_scalar");
            setImpl(x, "gt_scalar");
            constructImpl(x, "gt_scalar");
        }
        {
            var ids = new TList<SourceExpr>();
            ids.add(lv(e, "gtype_id"));
            ids.add(lv(e, "gvec_id"));
            for (var op : numOps) ids.add(opId(e, op[0]));
            for (var op : intOps) ids.add(opId(e, op[0]));
            for (var op : eqOps) ids.add(opId(e, op[0]));
            ids.add(opId(e, "neg"));
            ids.add(opId(e, "bit_not"));
            ids.add(aspId(e, "bind"));
            ids.add(aspId(e, "set"));
            ids.add(aspId(e, "function"));
            ids.add(aspId(e, "field"));
            selfType(
                    e,
                    x,
                    "gt_vec",
                    vecIr(in(e, "kind"), in(e, "n")),
                    e.aspectDispatch(ids.toArray(SourceExpr[]::new)),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "gvec"),
                                    andB(
                                            e,
                                            e.eq(acc(e, xx(e), a(e, "other"), "gvec", "kind"), in(e, "kind")),
                                            e.eq(acc(e, xx(e), a(e, "other"), "gvec", "n"), in(e, "n")))))));
            x.add(e.implInterface(e.v("gt_vec"), t(e, "gtype"), e.p("ir", vecIr(in(e, "kind"), in(e, "n")))));
            x.add(e.implInterface(e.v("gt_vec"), t(e, "gvec"), e.p("kind", in(e, "kind")), e.p("n", in(e, "n"))));
            for (var op : numOps) {
                op(
                        e,
                        x,
                        "gt_vec",
                        op[0],
                        e.ifElse(
                                e.eq(in(e, "kind"), e.s("bool")),
                                errCve(e, loc(e), "glsl: arithmetic on bvec"),
                                arith(e, op[1])));
            }
            for (var op : intOps) {
                op(
                        e,
                        x,
                        "gt_vec",
                        op[0],
                        e.ifElse(
                                orB(e, e.eq(in(e, "kind"), e.s("bool")), e.eq(in(e, "kind"), e.s("float"))),
                                errCve(e, loc(e), "glsl: " + op[1] + " on a non-integer vector"),
                                arith(e, op[1])));
            }
            for (var op : eqOps) op(e, x, "gt_vec", op[0], cmp(e, op[1]));
            op(
                    e,
                    x,
                    "gt_vec",
                    "neg",
                    e.ifElse(
                            e.eq(in(e, "kind"), e.s("bool")),
                            errCve(e, loc(e), "glsl: negating a bvec"),
                            unary(e, "-")));
            op(
                    e,
                    x,
                    "gt_vec",
                    "bit_not",
                    e.ifElse(
                            e.eq(in(e, "kind"), e.s("bool")),
                            errCve(e, loc(e), "glsl: use not() on a bvec"),
                            e.ifElse(
                                    e.eq(in(e, "kind"), e.s("float")),
                                    errCve(e, loc(e), "glsl: bitwise not on a float vector"),
                                    unary(e, "~"))));
            bindImpl(x, "gt_vec");
            setImpl(x, "gt_vec");
            constructImpl(x, "gt_vec");
            x.add(e.implInterface(
                    e.v("gt_vec"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.let("s", operandM(e, outer(e), a(e, "self"))),
                                    e.ifOnly(
                                            e.not(hasB(
                                                    e, xx(e), cveType(e, a(e, "key")), e.mb.evalAspectStringConst())),
                                            retM(e, errCve(e, loc(e), "glsl: swizzle must be a constant string"))),
                                    e.let("sw", constStr(e, a(e, "key"))),
                                    e.let("sets", e.listNew(str(e), e.s("xyzw"), e.s("rgba"), e.s("stpq"))),
                                    e.let("st", e.rec(e.p("len", e.intOf(0)), e.p("bad", e.intOf(0)))),
                                    e.foreach(
                                            e.call(e.mb.evalStrUtf8(), e.v("sw")),
                                            "b",
                                            e.seq(
                                                    e.incr(e.fld(e.v("st"), "len")),
                                                    e.let("hit", e.rec(e.p("v", e.intOf(0)))),
                                                    e.foreach(
                                                            e.v("sets"),
                                                            "set",
                                                            e.seq(
                                                                    e.let("i", e.rec(e.p("v", e.intOf(0)))),
                                                                    e.foreach(
                                                                            e.call(e.mb.evalStrUtf8(), e.v("set")),
                                                                            "c",
                                                                            e.seq(
                                                                                    e.ifOnly(
                                                                                            andB(
                                                                                                    e,
                                                                                                    e.eq(
                                                                                                            e.v("c"),
                                                                                                            e.v("b")),
                                                                                                    e.mb.operatorLt(
                                                                                                            e.fld(
                                                                                                                    e.v(
                                                                                                                            "i"),
                                                                                                                    "v"),
                                                                                                            in(
                                                                                                                    e,
                                                                                                                    "n"))),
                                                                                            e.mb.set(
                                                                                                    e.fld(
                                                                                                            e.v("hit"),
                                                                                                            "v"),
                                                                                                    num(e, 1))),
                                                                                    e.incr(e.fld(e.v("i"), "v")))))),
                                                    e.ifOnly(
                                                            e.eq(e.fld(e.v("hit"), "v"), num(e, 0)),
                                                            e.mb.set(e.fld(e.v("st"), "bad"), e.intOf(1))))),
                                    e.ifOnly(
                                            orB(
                                                    e,
                                                    e.eq(e.fld(e.v("st"), "bad"), num(e, 1)),
                                                    orB(
                                                            e,
                                                            e.eq(e.fld(e.v("st"), "len"), num(e, 0)),
                                                            e.mb.operatorGt(e.fld(e.v("st"), "len"), num(e, 4)))),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("glsl: invalid swizzle ."),
                                                                    e.v("sw"),
                                                                    e.s(" on "),
                                                                    tyDesc(e, outer(e)))))),
                                    e.let(
                                            "rty",
                                            e.ifElse(
                                                    e.eq(e.fld(e.v("st"), "len"), num(e, 1)),
                                                    e.mb.as(scalarT(e, in(e, "kind")), ty(e)),
                                                    e.mb.as(vecT(e, in(e, "kind"), e.fld(e.v("st"), "len")), ty(e)))),
                                    e.ret(okCve(
                                            e,
                                            cvOf(
                                                    e,
                                                    cveCode(e, e.v("s")),
                                                    gvalNew(
                                                            e,
                                                            e.v("rty"),
                                                            e.cat(gvExpr(e, e.v("s")), e.s("."), e.v("sw")))))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));
        }
        {
            var ids = new TList<SourceExpr>();
            ids.add(lv(e, "gtype_id"));
            ids.add(lv(e, "gmat_id"));
            for (var op : numOps) ids.add(opId(e, op[0]));
            for (var op : eqOps) ids.add(opId(e, op[0]));
            ids.add(opId(e, "neg"));
            ids.add(aspId(e, "bind"));
            ids.add(aspId(e, "set"));
            ids.add(aspId(e, "function"));
            selfType(
                    e,
                    x,
                    "gt_mat",
                    e.cat(e.s("mat"), toStr(e, in(e, "n"))),
                    e.aspectDispatch(ids.toArray(SourceExpr[]::new)),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "gmat"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "gmat", "n"), in(e, "n"))))));
            x.add(e.implInterface(e.v("gt_mat"), t(e, "gtype"), e.p("ir", e.cat(e.s("mat"), toStr(e, in(e, "n"))))));
            x.add(e.implInterface(e.v("gt_mat"), t(e, "gmat"), e.p("n", in(e, "n"))));
            for (var op : numOps) op(e, x, "gt_mat", op[0], arith(e, op[1]));
            for (var op : eqOps) op(e, x, "gt_mat", op[0], cmp(e, op[1]));
            op(e, x, "gt_mat", "neg", unary(e, "-"));
            bindImpl(x, "gt_mat");
            setImpl(x, "gt_mat");
            constructImpl(x, "gt_mat");
        }
        selfType(
                e,
                x,
                "gt_void",
                e.s("void"),
                e.aspectDispatch(lv(e, "gtype_id"), lv(e, "gvoid_id")),
                body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                castFromSelf(e),
                noCastTo(e),
                castToPanic(e),
                body(e, e.ret(has(e, xx(e), a(e, "other"), "gvoid"))));
        x.add(e.implInterface(e.v("gt_void"), t(e, "gtype"), e.p("ir", e.s("void"))));
        x.add(e.implInterface(e.v("gt_void"), t(e, "gvoid")));
    }

    void defRefs(TList<SourceExpr> x) {
        selfType(
                e,
                x,
                "gt_fn",
                e.s("fn"),
                e.aspectDispatch(lv(e, "gtype_id"), lv(e, "gfn_id"), aspId(e, "function")),
                body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                castFromSelf(e),
                noCastTo(e),
                castToPanic(e),
                body(
                        e,
                        e.ifOnly(e.not(has(e, xx(e), a(e, "other"), "gfn")), retM(e, dfalse(e))),
                        e.ifOnly(
                                e.not(isEqual(
                                        e,
                                        xx(e),
                                        in(e, "ret"),
                                        im0(e, "gfn", "ret", asp(e, xx(e), a(e, "other"), "gfn")))),
                                retM(e, dfalse(e))),
                        e.let("om", e.mapNew(str(e), ty(e))),
                        countLet(e, "on"),
                        e.foreach(
                                im0(e, "gfn", "params", asp(e, xx(e), a(e, "other"), "gfn")),
                                "p",
                                e.seq(
                                        e.callM(e.v("om"), "put", e.tup(toStr(e, e.fld(e.v("on"), "v")), e.v("p"))),
                                        e.incr(e.fld(e.v("on"), "v")))),
                        e.let("st", e.rec(e.p("i", e.intOf(0)), e.p("same", e.intOf(1)))),
                        e.foreach(
                                in(e, "params"),
                                "p",
                                e.seq(
                                        e.ifOnly(
                                                e.mb.operatorLt(e.fld(e.v("st"), "i"), e.fld(e.v("on"), "v")),
                                                e.ifOnly(
                                                        e.not(
                                                                isEqual(
                                                                        e,
                                                                        xx(e),
                                                                        e.v("p"),
                                                                        e.callM(
                                                                                e.v("om"),
                                                                                "get",
                                                                                toStr(e, e.fld(e.v("st"), "i"))))),
                                                        e.mb.set(e.fld(e.v("st"), "same"), num(e, 0)))),
                                        e.incr(e.fld(e.v("st"), "i")))),
                        e.ret(andB(
                                e,
                                e.eq(e.fld(e.v("st"), "i"), e.fld(e.v("on"), "v")),
                                e.eq(e.fld(e.v("st"), "same"), num(e, 1))))));
        x.add(e.implInterface(
                e.v("gt_fn"), t(e, "gtype"), e.p("ir", e.mb.panic(e.s("glsl: function type has no name")))));
        x.add(e.implInterface(e.v("gt_fn"), t(e, "gfn"), e.p("params", in(e, "params")), e.p("ret", in(e, "ret"))));
        x.add(e.implInterface(
                e.v("gt_fn"),
                aspIface(e, "function"),
                e.p(
                        "call",
                        body(
                                e,
                                e.let("out", instsNew(e)),
                                e.let("f", operandM(e, outer(e), a(e, "self"))),
                                appendCode(e, e.v("out"), cveCode(e, e.v("f"))),
                                e.let("elems", elementsM(e, a(e, "argument"))),
                                countLet(e, "ne"),
                                countLoop(e, "ne", e.v("elems")),
                                countLet(e, "np"),
                                countLoop(e, "np", in(e, "params")),
                                e.ifOnly(
                                        e.not(e.eq(e.fld(e.v("ne"), "v"), e.fld(e.v("np"), "v"))),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        e.cat(
                                                                e.s("glsl: call has "),
                                                                toStr(e, e.fld(e.v("ne"), "v")),
                                                                e.s(" arguments but the function takes "),
                                                                toStr(e, e.fld(e.v("np"), "v")))))),
                                e.let("pmap", e.mapNew(str(e), ty(e))),
                                e.let("pn", e.rec(e.p("v", e.intOf(0)))),
                                e.foreach(
                                        in(e, "params"),
                                        "p",
                                        e.seq(
                                                e.callM(
                                                        e.v("pmap"),
                                                        "put",
                                                        e.tup(toStr(e, e.fld(e.v("pn"), "v")), e.v("p"))),
                                                e.incr(e.fld(e.v("pn"), "v")))),
                                e.let("exprs", e.listNew(str(e))),
                                e.let("i", e.rec(e.p("v", e.intOf(0)))),
                                e.foreach(
                                        e.v("elems"),
                                        "ec",
                                        e.seq(
                                                e.let(
                                                        "v",
                                                        operandM(
                                                                e,
                                                                e.callM(
                                                                        e.v("pmap"),
                                                                        "get",
                                                                        toStr(e, e.fld(e.v("i"), "v"))),
                                                                e.v("ec"))),
                                                appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                                push(e, e.v("exprs"), gvExpr(e, e.v("v"))),
                                                e.incr(e.fld(e.v("i"), "v")))),
                                e.let(
                                        "call",
                                        e.cat(gvExpr(e, e.v("f")), e.s("("), e.join(e.v("exprs"), ", "), e.s(")"))),
                                e.ifOnly(
                                        has(e, xx(e), in(e, "ret"), "gvoid"),
                                        e.seq(
                                                push(e, e.v("out"), inst(e, e.cat(e.v("call"), e.s(";")))),
                                                retM(e, okCve(e, cvNo(e, codeOf(e, e.v("out"))))))),
                                e.ret(okCve(
                                        e,
                                        hf(
                                                e,
                                                "h_stmt",
                                                e.p("mod", xmod(e)),
                                                e.p("out", e.v("out")),
                                                e.p("ty", in(e, "ret")),
                                                e.p("expr", e.v("call"))))))),
                e.p("exception_type", e.optionTypeLoweredNone())));

        {
            var binOps = new String[] {
                "add", "sub", "mul", "div", "mod", "and", "or", "xor", "shl", "shr", "eq", "ne", "lt", "le", "gt", "ge"
            };
            var unOps = new String[] {"neg", "bit_not"};
            var ids = new TList<SourceExpr>();
            ids.add(lv(e, "ggtype_m_id"));
            ids.add(aspId(e, "function"));
            ids.add(aspId(e, "field"));
            ids.add(aspId(e, "set"));
            for (var op : binOps) ids.add(opId(e, op));
            for (var op : unOps) ids.add(opId(e, op));
            selfType(
                    e,
                    x,
                    "gt_global",
                    e.cat(e.s("global("), tyDesc(e, in(e, "base")), e.s(")")),
                    rtDispatch(e, e.mb.typesString(), e.aspectDispatch(ids.toArray(SourceExpr[]::new))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), a(e, "dest_type")),
                                    orB(
                                            e,
                                            isEqual(e, xx(e), in(e, "base"), a(e, "dest_type")),
                                            typeCanCastFrom(e, xx(e), loc(e), a(e, "dest_type"), in(e, "base")))))),
                    body(
                            e,
                            e.ifOnly(isEqual(e, xx(e), outer(e), a(e, "dest_type")), retM(e, okCve(e, a(e, "cv")))),
                            e.ret(castValue(
                                    e,
                                    xx(e),
                                    loc(e),
                                    a(e, "dest_type"),
                                    hf(e, "h_gval", e.p("cv", a(e, "cv")), e.p("base", in(e, "base")))))),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "ggtype_m"),
                                    isEqual(
                                            e,
                                            xx(e),
                                            in(e, "base"),
                                            acc(e, xx(e), a(e, "other"), "ggtype_m", "base"))))));
            x.add(e.implInterface(e.v("gt_global"), t(e, "ggtype_m"), e.p("base", in(e, "base"))));
            rtLiftImpl(e, x, "gt_global");
            rtBindImpl(e, x, "gt_global", e.mb.typesString());
            rtUseImpl(e, x, "gt_global");
            var selfArg = hf(e, "h_gval", e.p("cv", a(e, "self")), e.p("base", in(e, "base")));
            var baseArg = hf(e, "h_gval", e.p("cv", a(e, "base")), e.p("base", in(e, "base")));
            for (var op : binOps) {
                x.add(e.implInterface(
                        e.v("gt_global"),
                        opIface(e, op),
                        e.p(
                                op,
                                body(
                                        e,
                                        e.ret(bim(
                                                e,
                                                opIface(e, op),
                                                op,
                                                lookupV(e, xx(e), in(e, "base"), opIface(e, op)),
                                                e.rec(
                                                        e.p("xx", xx(e)),
                                                        e.p("loc", loc(e)),
                                                        e.p("base", baseArg),
                                                        e.p("reference", a(e, "reference")))))))));
            }
            for (var op : unOps) {
                x.add(e.implInterface(
                        e.v("gt_global"),
                        opIface(e, op),
                        e.p(
                                op,
                                body(
                                        e,
                                        e.ret(bim(
                                                e,
                                                opIface(e, op),
                                                op,
                                                lookupV(e, xx(e), in(e, "base"), opIface(e, op)),
                                                e.rec(e.p("xx", xx(e)), e.p("loc", loc(e)), e.p("base", baseArg))))))));
            }
            x.add(e.implInterface(
                    e.v("gt_global"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.ret(bim(
                                            e,
                                            aspIface(e, "function"),
                                            "call",
                                            lookupV(e, xx(e), in(e, "base"), aspIface(e, "function")),
                                            e.rec(
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("self", selfArg),
                                                    e.p("argument", a(e, "argument"))))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));
            x.add(e.implInterface(
                    e.v("gt_global"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.ret(bim(
                                            e,
                                            aspIface(e, "field"),
                                            "field",
                                            lookupV(e, xx(e), in(e, "base"), aspIface(e, "field")),
                                            e.rec(
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("self", selfArg),
                                                    e.p("key", a(e, "key"))))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));
            x.add(e.implInterface(
                    e.v("gt_global"),
                    e.mb.evalAspectSet(),
                    e.p(
                            "set",
                            body(
                                    e,
                                    e.ret(bim(
                                            e,
                                            e.mb.evalAspectSet(),
                                            "set",
                                            lookupV(e, xx(e), in(e, "base"), e.mb.evalAspectSet()),
                                            e.rec(
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("self", selfArg),
                                                    e.p("value", a(e, "value")))))))));
        }
        {
            selfType(
                    e,
                    x,
                    "gt_block",
                    e.cat(e.s("uniform block "), in(e, "name")),
                    rtDispatch(e, e.mb.typesString(), e.aspectDispatch(lv(e, "gblock_m_id"), aspId(e, "field"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "gblock_m"),
                                    e.eq(in(e, "name"), acc(e, xx(e), a(e, "other"), "gblock_m", "name"))))));
            x.add(e.implInterface(
                    e.v("gt_block"), t(e, "gblock_m"), e.p("fields", in(e, "fields")), e.p("name", in(e, "name"))));
            rtLiftImpl(e, x, "gt_block");
            rtBindImpl(e, x, "gt_block", e.mb.typesString());
            rtUseImpl(e, x, "gt_block");
            x.add(e.implInterface(
                    e.v("gt_block"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.ifOnly(
                                            e.not(hasB(
                                                    e, xx(e), cveType(e, a(e, "key")), e.mb.evalAspectStringConst())),
                                            retM(e, errCve(e, loc(e), "glsl: block field must be a constant string"))),
                                    e.let("key", constStr(e, a(e, "key"))),
                                    e.let(
                                            "inst",
                                            e.call(
                                                    e.mb.mustReinterpret(),
                                                    e.tup(
                                                            e.fld(
                                                                    e.call(
                                                                            e.mb.mustReinterpret(),
                                                                            e.tup(cveValue(e, a(e, "self")), vocT(e))),
                                                                    "data"),
                                                            str(e)))),
                                    e.foreach(
                                            in(e, "fields"),
                                            "f",
                                            e.ifOnly(
                                                    e.eq(e.callM(e.v("f"), "name", e.tup()), e.v("key")),
                                                    retM(
                                                            e,
                                                            okCve(
                                                                    e,
                                                                    cvOf(
                                                                            e,
                                                                            cveCode(e, a(e, "self")),
                                                                            gvalNew(
                                                                                    e,
                                                                                    e.callM(e.v("f"), "ty", e.tup()),
                                                                                    e.cat(
                                                                                            e.v("inst"),
                                                                                            e.s("."),
                                                                                            e.v("key")))))))),
                                    e.ret(errCve(
                                            e,
                                            loc(e),
                                            e.cat(
                                                    e.s("glsl: block "),
                                                    in(e, "name"),
                                                    e.s(" has no field "),
                                                    e.v("key")))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));
        }
    }

    void defModule(TList<SourceExpr> x) {
        method(e, x, "gmod", "xx", e.tup(), e.typesExprContext(), in(e, "xx", "value"));
        x.add(e.methodImpl(
                e.v("gmod"),
                "set_xx",
                e.typesExprContext(),
                e.optionTypeNone(),
                e.stage(e.mb.set(in(e, "xx"), e.optSome(e.typesExprContext(), e.v("arg"))))));
        method(
                e,
                x,
                "gmod",
                "fresh",
                e.tup(),
                str(e),
                e.seq(e.let("n", in(e, "n")), e.incr(in(e, "n")), e.ret(e.cat(e.s("t"), toStr(e, e.v("n"))))));
        x.add(e.methodImpl(
                e.v("gmod"), "decl", str(e), e.optionTypeNone(), e.stage(push(e, in(e, "decls"), e.v("arg")))));
        method(
                e,
                x,
                "gmod",
                "declare",
                e.rec(e.p("text", str(e)), e.p("name", str(e))),
                str(e),
                e.seq(push(e, in(e, "decls"), a(e, "text")), e.ret(a(e, "name"))));
        method(
                e,
                x,
                "gmod",
                "block",
                e.rec(e.p("name", str(e)), e.p("layout", str(e)), e.p("fields", e.listType(t(e, "gfield")))),
                str(e),
                e.seq(
                        e.let("all", e.listNew(str(e))),
                        push(
                                e,
                                e.v("all"),
                                e.cat(
                                        e.s("layout("),
                                        a(e, "layout"),
                                        e.s(") uniform "),
                                        a(e, "name"),
                                        e.s("_block {"))),
                        e.foreach(
                                a(e, "fields"),
                                "f",
                                push(
                                        e,
                                        e.v("all"),
                                        e.cat(
                                                e.s("  "),
                                                irOf(e, in(e, "xx", "value"), e.callM(e.v("f"), "ty", e.tup())),
                                                e.s(" "),
                                                e.callM(e.v("f"), "name", e.tup()),
                                                e.s(";")))),
                        push(e, e.v("all"), e.cat(e.s("} "), a(e, "name"), e.s(";"))),
                        push(e, in(e, "decls"), e.join(e.v("all"), "\n")),
                        e.ret(a(e, "name"))));
        method(
                e,
                x,
                "gmod",
                "func",
                e.rec(
                        e.p("name", str(e)),
                        e.p("params", e.listType(str(e))),
                        e.p("ty", ty(e)),
                        e.p("body", e.mb.typesExpr())),
                str(e),
                body(
                        e,
                        e.let("loc", e.call(e.evalBuiltin("expr_loc"), a(e, "body"))),
                        e.let("ptypes", acc(e, in(e, "xx", "value"), a(e, "ty"), "gfn", "params")),
                        e.let("fret", acc(e, in(e, "xx", "value"), a(e, "ty"), "gfn", "ret")),
                        e.let("pnames", e.mapNew(str(e), str(e))),
                        e.let("ni", e.rec(e.p("i", e.intOf(0)))),
                        e.foreach(
                                a(e, "params"),
                                "pn",
                                e.seq(
                                        e.callM(
                                                e.v("pnames"),
                                                "put",
                                                e.tup(toStr(e, e.fld(e.v("ni"), "i")), e.v("pn"))),
                                        e.incr(e.fld(e.v("ni"), "i")))),
                        e.let("entries", e.mapNew(e.mb.typesConstkey(), e.mb.evalBinding())),
                        e.let("pi", e.rec(e.p("i", e.intOf(0)))),
                        e.let("sig", e.listNew(str(e))),
                        e.foreach(
                                e.v("ptypes"),
                                "pty",
                                e.seq(
                                        e.let("an", e.cat(e.s("a"), toStr(e, e.fld(e.v("pi"), "i")))),
                                        push(
                                                e,
                                                e.v("sig"),
                                                e.cat(irOf(e, in(e, "xx", "value"), e.v("pty")), e.s(" "), e.v("an"))),
                                        e.callM(
                                                e.v("entries"),
                                                "put",
                                                e.tup(
                                                        e.callM(e.v("pnames"), "get", toStr(e, e.fld(e.v("pi"), "i"))),
                                                        mk(
                                                                e,
                                                                "gbinding",
                                                                e.p("ty", e.v("pty")),
                                                                e.p("name", e.v("an"))))),
                                        e.incr(e.fld(e.v("pi"), "i")))),
                        e.let("is_void", has(e, in(e, "xx", "value"), e.v("fret"), "gvoid")),
                        e.let("vx", e.call(e.evalBuiltin("eval_context"), e.tup())),
                        e.let(
                                "sc",
                                e.call(
                                        e.evalBuiltin("scope"),
                                        e.rec(e.p("vx", e.v("vx")), e.p("entries", e.v("entries"))))),
                        e.let("tgt", mk(e, "gtarget", e.p("mod", outer(e)))),
                        e.let(
                                "ctx",
                                e.call(
                                        e.evalBuiltin("expr_context"),
                                        e.rec(
                                                e.p("scope", e.v("sc")),
                                                e.p("target", e.mb.as(e.v("tgt"), e.mb.evalTarget())),
                                                e.p("vx", e.v("vx")),
                                                e.p("exception_type", str(e))))),
                        e.let(
                                "r",
                                EcoTargetMb.mustV(
                                        e,
                                        TARGET,
                                        e.call(
                                                e.mb.evalConstEval(),
                                                e.rec(e.p("xx", e.v("ctx")), e.p("staged", a(e, "body")))),
                                        "compile failed")),
                        e.let("ended", cveEnded(e, e.v("r"))),
                        e.let("out", instsNew(e)),
                        e.ifOnly(
                                andB(e, e.not(e.v("ended")), andB(e, e.v("is_void"), isGlsl(e, e.v("ctx"), e.v("r")))),
                                e.matchOf(
                                        e.eresultErr(
                                                e.mb.evalValue(), e.v("loc"), e.s("void function body yields a value")),
                                        "__x",
                                        Opt.none(),
                                        e.p("ok", e.mb.panic(e.s("unreachable"))),
                                        e.p("errs", EcoTargetMb.panicErrs(e, TARGET, "compile failed", e.v("__x"))))),
                        e.let("need_cast", andB(e, e.not(e.v("ended")), e.not(e.v("is_void")))),
                        e.let(
                                "v",
                                EcoTargetMb.mustV(
                                        e,
                                        TARGET,
                                        e.ifElse(
                                                e.v("need_cast"),
                                                tryEr(
                                                        e,
                                                        operand(e, e.v("ctx"), e.v("loc"), e.v("fret"), e.v("r")),
                                                        cveT(e)),
                                                okCve(e, e.v("r"))),
                                        "compile failed")),
                        appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                        e.ifOnly(
                                e.v("need_cast"),
                                push(e, e.v("out"), inst(e, e.cat(e.s("return "), gvExpr(e, e.v("v")), e.s(";"))))),
                        e.let("all", e.listNew(str(e))),
                        push(
                                e,
                                e.v("all"),
                                e.cat(
                                        irOf(e, in(e, "xx", "value"), e.v("fret")),
                                        e.s(" "),
                                        a(e, "name"),
                                        e.s("("),
                                        e.join(e.v("sig"), ", "),
                                        e.s(") {"))),
                        e.foreach(
                                e.v("out"),
                                "i0",
                                push(e, e.v("all"), e.cat(e.s("  "), e.callM(e.v("i0"), "text", e.tup())))),
                        push(e, e.v("all"), e.s("}")),
                        push(e, in(e, "funcs"), e.join(e.v("all"), "\n")),
                        e.ret(a(e, "name"))));
        method(
                e,
                x,
                "gmod",
                "render",
                e.tup(),
                renderT(e),
                e.seq(
                        e.let("all", e.listNew(str(e))),
                        push(e, e.v("all"), e.s("#version 300 es")),
                        push(e, e.v("all"), e.s("precision highp float;")),
                        e.foreach(in(e, "decls"), "d", push(e, e.v("all"), e.v("d"))),
                        e.foreach(in(e, "funcs"), "f", push(e, e.v("all"), e.v("f"))),
                        e.ret(e.rec(e.p("glsl", e.cat(e.join(e.v("all"), "\n"), e.s("\n")))))));
    }

    private SourceExpr ifBranchStmts(String cvVar) {
        var valued = andB(e, cveHas(e, e.v(cvVar)), isGlsl(e, xx(e), e.v(cvVar)));
        return e.seq(
                e.ifOnly(
                        andB(e, cveHas(e, e.v(cvVar)), e.not(isGlsl(e, xx(e), e.v(cvVar)))),
                        retM(e, errCve(e, loc(e), "if branches must yield glsl values"))),
                e.ifOnly(e.not(valued), appendCode(e, e.v("bo"), cveCode(e, e.v(cvVar)))),
                e.ifOnly(
                        valued,
                        e.seq(
                                e.ifOnly(
                                        e.eq(e.fld(e.v("st"), "has"), num(e, 0)),
                                        e.seq(
                                                e.mb.set(
                                                        e.fld(e.v("st"), "ty"),
                                                        e.optionTypeLoweredSome(cveType(e, e.v(cvVar)))),
                                                e.mb.set(e.fld(e.v("st"), "has"), num(e, 1)))),
                                e.let("bv", operandM(e, e.fld(e.v("st"), "ty", "value"), e.v(cvVar))),
                                appendCode(e, e.v("bo"), cveCode(e, e.v("bv"))),
                                push(
                                        e,
                                        e.v("bo"),
                                        inst(
                                                e,
                                                e.cat(
                                                        e.fld(e.v("st"), "res"),
                                                        e.s(" = "),
                                                        gvExpr(e, e.v("bv")),
                                                        e.s(";")))))),
                e.ifOnly(
                        andB(e, e.not(valued), e.not(cveEnded(e, e.v(cvVar)))),
                        e.mb.set(e.fld(e.v("st"), "novalue"), num(e, 1))),
                e.ifOnly(e.not(cveEnded(e, e.v(cvVar))), e.mb.set(e.fld(e.v("st"), "ended"), num(e, 0))));
    }

    private SourceExpr indent(SourceExpr depth) {
        return e.seq(
                e.let("__pre", e.rec(e.p("v", e.dstr("")))),
                e.let("__k", e.rec(e.p("v", e.intOf(0)))),
                e.foreach(
                        a(e, "conditions"),
                        "_",
                        e.seq(
                                e.ifOnly(
                                        e.mb.operatorLt(e.fld(e.v("__k"), "v"), depth),
                                        e.mb.set(e.fld(e.v("__pre"), "v"), e.cat(e.fld(e.v("__pre"), "v"), e.s("  ")))),
                                e.incr(e.fld(e.v("__k"), "v")))),
                e.ret(e.fld(e.v("__pre"), "v")));
    }

    private SourceExpr pushIndented(SourceExpr insts, SourceExpr pre, String extra) {
        return e.foreach(
                insts, "i0", push(e, e.v("out"), inst(e, e.cat(pre, e.s(extra), e.callM(e.v("i0"), "text", e.tup())))));
    }

    void defTarget(TList<SourceExpr> x) {
        x.add(e.implInterface(
                e.v("gtarget"),
                e.mb.evalTarget(),
                e.p("cast_exception", e.mb.panic(e.s("glsl target: exceptions are not supported"))),
                e.p(
                        "initial_bind",
                        e.eresultErr(
                                e.mb.evalBinding(),
                                loc(e),
                                e.s("glsl target: match and catch payloads are not supported"))),
                e.p(
                        "seq",
                        body(
                                e,
                                e.let("out", instsNew(e)),
                                e.let("st", e.rec(e.p("any", e.intOf(0)))),
                                e.foreach(
                                        a(e, "entries"),
                                        "en",
                                        e.seq(
                                                appendCode(e, e.v("out"), e.v("en")),
                                                e.ifOnly(
                                                        isTargetCode(e, e.v("en")),
                                                        e.mb.set(e.fld(e.v("st"), "any"), num(e, 1))))),
                                e.ret(e.eresultOk(
                                        codeT(e),
                                        e.ifElse(
                                                e.eq(e.fld(e.v("st"), "any"), num(e, 1)),
                                                codeOf(e, e.v("out")),
                                                codeNone(e, loc(e))))))),
                e.p("jump", e.eresultErr(codeT(e), loc(e), e.s("glsl target: jumps are not supported"))),
                e.p("land", e.eresultOk(cveT(e), cvNo(e, codeNone(e, loc(e))))),
                e.p("value_is_runtime", e.mb.bool(false)),
                structuralExits(e)[0],
                structuralExits(e)[1],
                e.p("exit", e.eresultErr(codeT(e), loc(e), e.s("glsl target: labeled exits are not supported"))),
                e.p("guarded", e.mb.panic(e.s("glsl target: exceptions are not supported"))),
                e.p("exc_cleanup", e.mb.panic(e.s("glsl target: exceptions are not supported"))),
                e.p("exit_trampoline", e.mb.panic(e.s("glsl target: cleanup blocks are not supported"))),
                e.p("drop", e.eresultOk(codeT(e), codeNone(e, loc(e)))),
                e.p(
                        "return",
                        body(
                                e,
                                e.let("out", instsNew(e)),
                                appendCode(e, e.v("out"), a(e, "cv", "code")),
                                push(
                                        e,
                                        e.v("out"),
                                        inst(
                                                e,
                                                e.ifElse(
                                                        isGlsl(e, xx(e), a(e, "cv")),
                                                        e.cat(e.s("return "), gvExpr(e, a(e, "cv")), e.s(";")),
                                                        e.dstr("return;")))),
                                e.ret(e.call(e.evalBuiltin("codevalue_control_ended"), codeOf(e, e.v("out")))))),
                e.p("stage", errCve(e, loc(e), "glsl target: nested staging is not supported")),
                e.p(
                        "if",
                        body(
                                e,
                                e.let("mod", xmod(e)),
                                e.let("out", instsNew(e)),
                                e.let(
                                        "st",
                                        e.rec(
                                                e.p("ended", e.intOf(1)),
                                                e.p("has", e.intOf(0)),
                                                e.p("novalue", e.intOf(0)),
                                                e.p("ty", e.optionTypeLoweredNone()),
                                                e.p("res", fresh(e, e.v("mod"))))),
                                countLet(e, "nc"),
                                countLoop(e, "nc", a(e, "conditions")),
                                e.let("ci", e.rec(e.p("v", e.intOf(0)))),
                                e.let("boolt", e.mb.as(scalarT(e, "bool"), ty(e))),
                                e.foreach(
                                        a(e, "conditions"),
                                        "br",
                                        e.seq(
                                                e.let("c", operandM(e, e.v("boolt"), e.fld(e.v("br"), "condition"))),
                                                e.let("pre", indent(e.fld(e.v("ci"), "v"))),
                                                e.let("cl", instsNew(e)),
                                                appendCode(e, e.v("cl"), cveCode(e, e.v("c"))),
                                                pushIndented(e.v("cl"), e.v("pre"), ""),
                                                push(
                                                        e,
                                                        e.v("out"),
                                                        inst(
                                                                e,
                                                                e.cat(
                                                                        e.v("pre"),
                                                                        e.s("if ("),
                                                                        gvExpr(e, e.v("c")),
                                                                        e.s(") {")))),
                                                e.let("bo", instsNew(e)),
                                                e.let("b", e.fld(e.v("br"), "body")),
                                                ifBranchStmts("b"),
                                                pushIndented(e.v("bo"), e.v("pre"), "  "),
                                                e.incr(e.fld(e.v("ci"), "v")),
                                                e.ifElse(
                                                        orB(
                                                                e,
                                                                e.mb.operatorLt(
                                                                        e.fld(e.v("ci"), "v"), e.fld(e.v("nc"), "v")),
                                                                e.fld(a(e, "else"), "is_some")),
                                                        push(
                                                                e,
                                                                e.v("out"),
                                                                inst(e, e.cat(e.v("pre"), e.s("} else {")))),
                                                        push(e, e.v("out"), inst(e, e.cat(e.v("pre"), e.s("}"))))))),
                                e.ifOnly(
                                        e.fld(a(e, "else"), "is_some"),
                                        e.seq(
                                                e.let("pre", indent(e.fld(e.v("nc"), "v"))),
                                                e.let("bo", instsNew(e)),
                                                e.let("b", e.fld(a(e, "else"), "value")),
                                                ifBranchStmts("b"),
                                                pushIndented(e.v("bo"), e.v("pre"), ""),
                                                push(
                                                        e,
                                                        e.v("out"),
                                                        inst(
                                                                e,
                                                                e.cat(
                                                                        indent(
                                                                                e.mb.operatorSub(
                                                                                        e.fld(e.v("nc"), "v"),
                                                                                        num(e, 1))),
                                                                        e.s("}")))))),
                                e.ifOnly(
                                        e.not(e.fld(a(e, "else"), "is_some")),
                                        e.seq(
                                                e.mb.set(e.fld(e.v("st"), "ended"), num(e, 0)),
                                                e.ifOnly(
                                                        e.eq(e.fld(e.v("st"), "has"), num(e, 1)),
                                                        e.mb.set(e.fld(e.v("st"), "novalue"), num(e, 1))))),
                                e.let("depth", e.rec(e.p("v", e.mb.operatorSub(e.fld(e.v("nc"), "v"), num(e, 2))))),
                                e.foreach(
                                        a(e, "conditions"),
                                        "_",
                                        e.ifOnly(
                                                e.mb.operatorGe(e.fld(e.v("depth"), "v"), num(e, 0)),
                                                e.seq(
                                                        push(
                                                                e,
                                                                e.v("out"),
                                                                inst(
                                                                        e,
                                                                        e.cat(
                                                                                indent(e.fld(e.v("depth"), "v")),
                                                                                e.s("}")))),
                                                        e.mb.set(
                                                                e.fld(e.v("depth"), "v"),
                                                                e.mb.operatorSub(
                                                                        e.fld(e.v("depth"), "v"), num(e, 1)))))),
                                e.ifOnly(
                                        e.eq(e.fld(e.v("st"), "ended"), num(e, 1)),
                                        retM(
                                                e,
                                                okCve(
                                                        e,
                                                        e.call(
                                                                e.evalBuiltin("codevalue_control_ended"),
                                                                codeOf(e, e.v("out")))))),
                                e.ifOnly(
                                        e.eq(e.fld(e.v("st"), "has"), num(e, 0)),
                                        retM(e, okCve(e, cvNo(e, codeOf(e, e.v("out")))))),
                                e.ifOnly(
                                        e.eq(e.fld(e.v("st"), "novalue"), num(e, 1)),
                                        retM(e, errCve(e, loc(e), "if: some branches yield a value and some do not"))),
                                e.let("all", instsNew(e)),
                                push(
                                        e,
                                        e.v("all"),
                                        inst(
                                                e,
                                                e.cat(
                                                        irOf(e, xx(e), e.fld(e.v("st"), "ty", "value")),
                                                        e.s(" "),
                                                        e.fld(e.v("st"), "res"),
                                                        e.s(";")))),
                                e.foreach(e.v("out"), "i0", push(e, e.v("all"), e.v("i0"))),
                                e.ret(okCve(
                                        e,
                                        cvOf(
                                                e,
                                                codeOf(e, e.v("all")),
                                                gvalNew(
                                                        e,
                                                        e.fld(e.v("st"), "ty", "value"),
                                                        e.fld(e.v("st"), "res"))))))),
                e.p("try_catch_finally", errCve(e, loc(e), "glsl target: try is not supported")),
                e.p("match", errCve(e, loc(e), "glsl target: match is not supported"))));
    }

    private SourceExpr wantList(String... keys) {
        var items = new TList<SourceExpr>();
        for (var k : keys) items.add(e.s(k));
        return e.listNew(str(e), items.toArray(SourceExpr[]::new));
    }

    void defHooks(TList<SourceExpr> x) {
        fnDef(e, x, "h_var_code", str(e), str(e));
        fnDef(
                e,
                x,
                "h_fields",
                e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("cv", cveT(e))),
                e.listType(t(e, "gfield")));
        fnThrows(e, "h_fields", errsT(e));
        fnImpl(
                e,
                x,
                "h_var_code",
                body(
                        e,
                        e.let("st", e.rec(e.p("kind", e.listNew(e.mb.typesByte())), e.p("n", e.dstr("")))),
                        e.foreach(
                                e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                "b",
                                e.ifElse(
                                        andB(
                                                e,
                                                e.mb.operatorGe(e.mb.as(e.v("b"), intT(e)), num(e, '0')),
                                                e.mb.operatorLe(e.mb.as(e.v("b"), intT(e)), num(e, '9'))),
                                        e.mb.set(
                                                e.fld(e.v("st"), "n"),
                                                hf(e, "h_utf8", e.listNew(e.mb.typesByte(), e.v("b")))),
                                        push(e, e.fld(e.v("st"), "kind"), e.v("b")))),
                        e.let("kind", hf(e, "h_utf8", e.fld(e.v("st"), "kind"))),
                        e.ret(e.cat(
                                e.ifElse(
                                        e.eq(e.v("kind"), e.s("float")),
                                        e.dstr("f"),
                                        e.ifElse(
                                                e.eq(e.v("kind"), e.s("int")),
                                                e.dstr("i"),
                                                e.ifElse(e.eq(e.v("kind"), e.s("uint")), e.dstr("u"), e.dstr("b")))),
                                e.fld(e.v("st"), "n")))));
        fnImpl(
                e,
                x,
                "h_fields",
                body(
                        e,
                        e.let("fields", e.listNew(lt(e, "gfield"))),
                        e.let("pairs", elements(e, a(e, "cv"))),
                        e.foreach(
                                e.v("pairs"),
                                "pc",
                                e.seq(
                                        e.let("pair", elements(e, e.v("pc"))),
                                        countLet(e, "n2"),
                                        countLoop(e, "n2", e.v("pair")),
                                        e.ifOnly(
                                                e.not(e.eq(e.fld(e.v("n2"), "v"), num(e, 2))),
                                                throwErr(e, loc(e), "glsl: a block field is a (name, type) tuple")),
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
                                                                                        "glsl: block field name must be a constant string")),
                                                                        e.mb.set(
                                                                                e.fld(e.v("fname"), "v"),
                                                                                constStr(e, e.v("pe"))))),
                                                        e.ifOnly(
                                                                e.eq(e.fld(e.v("i"), "v"), num(e, 1)),
                                                                e.seq(
                                                                        e.let("ptycv", e.v("pe")),
                                                                        e.let(
                                                                                "tk",
                                                                                lookup(
                                                                                        e,
                                                                                        xx(e),
                                                                                        cveType(e, e.v("ptycv")),
                                                                                        aspIface(e, "type"))),
                                                                        e.ifOnly(
                                                                                orB(
                                                                                        e,
                                                                                        isRuntime(e, e.v("ptycv")),
                                                                                        e.not(
                                                                                                e.fld(
                                                                                                        e.v("tk"),
                                                                                                        "is_some"))),
                                                                                throwErr(
                                                                                        e,
                                                                                        loc(e),
                                                                                        "glsl: block field type must be a constant type")),
                                                                        e.let(
                                                                                "fty",
                                                                                bim(
                                                                                        e,
                                                                                        aspIface(e, "type"),
                                                                                        "type",
                                                                                        e.fld(e.v("tk"), "value"),
                                                                                        cveValue(e, e.v("ptycv")))),
                                                                        e.ifOnly(
                                                                                e.not(
                                                                                        orB(
                                                                                                e,
                                                                                                has(
                                                                                                        e,
                                                                                                        xx(e),
                                                                                                        e.v("fty"),
                                                                                                        "gscalar"),
                                                                                                orB(
                                                                                                        e,
                                                                                                        has(
                                                                                                                e,
                                                                                                                xx(e),
                                                                                                                e.v(
                                                                                                                        "fty"),
                                                                                                                "gvec"),
                                                                                                        has(
                                                                                                                e,
                                                                                                                xx(e),
                                                                                                                e.v(
                                                                                                                        "fty"),
                                                                                                                "gmat")))),
                                                                                throwErr(
                                                                                        e,
                                                                                        loc(e),
                                                                                        e.cat(
                                                                                                e.s(
                                                                                                        "glsl: a block field cannot have type "),
                                                                                                tyDesc(
                                                                                                        e,
                                                                                                        e.v("fty"))))),
                                                                        push(
                                                                                e,
                                                                                e.v("fields"),
                                                                                mk(
                                                                                        e,
                                                                                        "gfield",
                                                                                        e.p(
                                                                                                "name",
                                                                                                e.fld(
                                                                                                        e.v("fname"),
                                                                                                        "v")),
                                                                                        e.p("ty", e.v("fty")))))),
                                                        e.incr(e.fld(e.v("i"), "v")))))),
                        e.ret(e.v("fields"))));
        selfType(
                e,
                x,
                "ggfn_t",
                e.cat(e.s("glsl definition function "), toStr(e, in(e, "kind"))),
                rtDispatch(e, lt(e, "gmod"), e.aspectDispatch(lv(e, "ggfn_m_id"), aspId(e, "function"))),
                canCastFromSelf(e),
                castFromSelf(e),
                noCastTo(e),
                castToPanic(e),
                body(
                        e,
                        e.ret(andB(
                                e,
                                has(e, xx(e), a(e, "other"), "ggfn_m"),
                                e.eq(acc(e, xx(e), a(e, "other"), "ggfn_m", "kind"), in(e, "kind"))))));
        rtLiftImpl(e, x, "ggfn_t");
        rtBindImpl(e, x, "ggfn_t", lt(e, "gmod"));
        rtUseImpl(e, x, "ggfn_t");
        x.add(e.implInterface(e.v("ggfn_t"), t(e, "ggfn_m"), e.p("kind", in(e, "kind"))));
        var glVars = new TList<EcoMb.P>();
        for (var gv : GL_VARS) glVars.add(e.p(gv.get(0), e.s(gv.get(1) + gv.get(2))));
        x.add(e.implInterface(
                e.v("ggfn_t"),
                aspIface(e, "function"),
                e.p(
                        "call",
                        body(
                                e,
                                e.let("argv", a(e, "argument")),
                                e.ifOnly(
                                        e.not(cveHas(e, e.v("argv"))),
                                        retM(e, errCve(e, loc(e), "glsl definition argument has no value"))),
                                e.let("rk", lookup(e, xx(e), cveType(e, e.v("argv")), aspIface(e, "record"))),
                                e.ifOnly(
                                        e.not(e.fld(e.v("rk"), "is_some")),
                                        retM(e, errCve(e, loc(e), "glsl definition argument must be a record"))),
                                e.let(
                                        "want",
                                        e.ifElse(
                                                ggfnIs(e, "func_fn"),
                                                wantList("mod", "name", "params", "ty", "body"),
                                                e.ifElse(
                                                        ggfnIs(e, "uniform_block_fn"),
                                                        wantList("mod", "name", "layout", "fields"),
                                                        e.ifElse(
                                                                ggfnIs(e, "gl_fn"),
                                                                wantList("name"),
                                                                wantList("mod", "name", "ty"))))),
                                e.let("kc", e.rec(e.p("n", e.intOf(0)), e.p("bad", e.dstr("")))),
                                e.foreach(
                                        bim0(e, aspIface(e, "record"), "field_types", e.fld(e.v("rk"), "value")),
                                        "en",
                                        e.seq(
                                                e.incr(e.fld(e.v("kc"), "n")),
                                                e.let("ks", keyStr(e, e.fld(e.v("en"), "key"))),
                                                e.let("hit", e.rec(e.p("v", e.intOf(0)))),
                                                e.foreach(
                                                        e.v("want"),
                                                        "w",
                                                        e.ifOnly(
                                                                e.eq(e.v("w"), e.v("ks")),
                                                                e.mb.set(e.fld(e.v("hit"), "v"), num(e, 1)))),
                                                e.ifOnly(
                                                        e.eq(e.fld(e.v("hit"), "v"), num(e, 0)),
                                                        e.mb.set(e.fld(e.v("kc"), "bad"), e.v("ks"))))),
                                e.ifOnly(
                                        e.not(e.eq(e.fld(e.v("kc"), "bad"), e.s(""))),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        e.cat(
                                                                e.s("glsl definition has no argument "),
                                                                e.fld(e.v("kc"), "bad"))))),
                                countLet(e, "wn"),
                                countLoop(e, "wn", e.v("want")),
                                e.ifOnly(
                                        e.not(e.eq(e.fld(e.v("kc"), "n"), e.fld(e.v("wn"), "v"))),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        e.cat(
                                                                e.s("glsl definition requires arguments "),
                                                                e.join(e.v("want"), ", "))))),
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
                                e.let("namecv", ggfnStaged(e, graft(e, ggfnField(e, "name")))),
                                e.ifOnly(
                                        isRuntime(e, e.v("namecv")),
                                        retM(e, errCve(e, loc(e), "glsl definition name must be constant"))),
                                e.let("name", constStr(e, e.v("namecv"))),
                                e.ifOnly(
                                        ggfnIs(e, "gl_fn"),
                                        e.seq(
                                                e.let("vars", e.mapNew(str(e), str(e), glVars.toArray(EcoMb.P[]::new))),
                                                e.ifOnly(
                                                        e.not(e.callM(e.v("vars"), "has", e.v("name"))),
                                                        retM(
                                                                e,
                                                                errCve(
                                                                        e,
                                                                        loc(e),
                                                                        e.cat(
                                                                                e.s("glsl: no builtin variable "),
                                                                                e.v("name"))))),
                                                e.let(
                                                        "vty",
                                                        typeOf(
                                                                e,
                                                                hf(
                                                                        e,
                                                                        "h_parse_desc",
                                                                        hf(
                                                                                e,
                                                                                "h_var_code",
                                                                                e.callM(
                                                                                        e.v("vars"),
                                                                                        "get",
                                                                                        e.v("name")))))),
                                                retM(e, tagValue(e, e.v("namecv"), globalT(e, e.v("vty")))))),
                                e.let("modcv", ggfnField(e, "mod")),
                                e.ifOnly(
                                        ggfnIs(e, "uniform_block_fn"),
                                        e.seq(
                                                e.let("layoutcv", ggfnStaged(e, graft(e, ggfnField(e, "layout")))),
                                                e.ifOnly(
                                                        isRuntime(e, e.v("layoutcv")),
                                                        retM(
                                                                e,
                                                                errCve(
                                                                        e,
                                                                        loc(e),
                                                                        "glsl block layout must be constant"))),
                                                e.let(
                                                        "fieldsv",
                                                        unwrapM(
                                                                e,
                                                                tryEr(
                                                                        e,
                                                                        hf(
                                                                                e,
                                                                                "h_fields",
                                                                                e.p("xx", xx(e)),
                                                                                e.p("loc", loc(e)),
                                                                                e.p("cv", ggfnField(e, "fields"))),
                                                                        e.listType(lt(e, "gfield"))),
                                                                cveT(e))),
                                                e.let(
                                                        "refcv",
                                                        ggfnStaged(
                                                                e,
                                                                e.callM(
                                                                        ggfnMod(e),
                                                                        "block",
                                                                        e.rec(
                                                                                e.p("name", graft(e, e.v("namecv"))),
                                                                                e.p(
                                                                                        "layout",
                                                                                        graft(e, e.v("layoutcv"))),
                                                                                e.p(
                                                                                        "fields",
                                                                                        e.lower(e.v("fieldsv"))))))),
                                                retM(
                                                        e,
                                                        tagValue(
                                                                e,
                                                                e.v("refcv"),
                                                                mk(
                                                                        e,
                                                                        "gt_block",
                                                                        e.p("name", e.v("name")),
                                                                        e.p("fields", e.v("fieldsv"))))))),
                                e.let("tycv", ggfnField(e, "ty")),
                                e.let(
                                        "tyv",
                                        e.seq(
                                                e.let(
                                                        "tk",
                                                        lookup(e, xx(e), cveType(e, e.v("tycv")), aspIface(e, "type"))),
                                                e.ifOnly(
                                                        orB(
                                                                e,
                                                                isRuntime(e, e.v("tycv")),
                                                                e.not(e.fld(e.v("tk"), "is_some"))),
                                                        retM(e, errCve(e, loc(e), "glsl ty must be a constant type"))),
                                                e.ret(
                                                        bim(
                                                                e,
                                                                aspIface(e, "type"),
                                                                "type",
                                                                e.fld(e.v("tk"), "value"),
                                                                cveValue(e, e.v("tycv")))))),
                                e.ifOnly(
                                        orB(e, ggfnIs(e, "in_fn"), ggfnIs(e, "out_fn")),
                                        e.seq(
                                                e.ifOnly(
                                                        e.not(
                                                                orB(
                                                                        e,
                                                                        has(e, xx(e), e.v("tyv"), "gscalar"),
                                                                        orB(
                                                                                e,
                                                                                has(e, xx(e), e.v("tyv"), "gvec"),
                                                                                has(e, xx(e), e.v("tyv"), "gmat")))),
                                                        retM(
                                                                e,
                                                                errCve(
                                                                        e,
                                                                        loc(e),
                                                                        e.cat(
                                                                                e.s(
                                                                                        "glsl: a variable cannot have type "),
                                                                                tyDesc(e, e.v("tyv")))))),
                                                e.let(
                                                        "decl",
                                                        e.cat(
                                                                e.ifElse(
                                                                        ggfnIs(e, "in_fn"),
                                                                        e.dstr("in "),
                                                                        e.dstr("out ")),
                                                                irOf(e, xx(e), e.v("tyv")),
                                                                e.s(" "),
                                                                e.v("name"),
                                                                e.s(";"))),
                                                e.let(
                                                        "refcv",
                                                        ggfnStaged(
                                                                e,
                                                                e.callM(
                                                                        ggfnMod(e),
                                                                        "declare",
                                                                        e.rec(
                                                                                e.p("text", e.lower(e.v("decl"))),
                                                                                e.p(
                                                                                        "name",
                                                                                        graft(e, e.v("namecv"))))))),
                                                retM(e, tagValue(e, e.v("refcv"), globalT(e, e.v("tyv")))))),
                                e.ifOnly(
                                        e.not(has(e, xx(e), e.v("tyv"), "gfn")),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        e.cat(
                                                                e.s("glsl ty must be a function type, not "),
                                                                tyDesc(e, e.v("tyv")))))),
                                e.let("pcv", ggfnField(e, "params")),
                                e.let("ptk", lookup(e, xx(e), cveType(e, e.v("pcv")), aspIface(e, "tuple"))),
                                e.ifOnly(
                                        e.not(e.fld(e.v("ptk"), "is_some")),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        "glsl function params must be a tuple of parameter names"))),
                                e.let("names", e.listNew(str(e))),
                                e.foreach(
                                        bim(
                                                e,
                                                aspIface(e, "tuple"),
                                                "take_elements",
                                                e.fld(e.v("ptk"), "value"),
                                                e.rec(
                                                        e.p("xx", xx(e)),
                                                        e.p("loc", loc(e)),
                                                        e.p("value", cveValue(e, e.v("pcv"))))),
                                        "er",
                                        e.seq(
                                                e.let("ncv", unwrapCveM(e, e.v("er"))),
                                                e.ifOnly(
                                                        e.not(
                                                                hasB(
                                                                        e,
                                                                        xx(e),
                                                                        cveType(e, e.v("ncv")),
                                                                        e.mb.evalAspectStringConst())),
                                                        retM(
                                                                e,
                                                                errCve(
                                                                        e,
                                                                        loc(e),
                                                                        "glsl function parameter names must be constant strings"))),
                                                push(e, e.v("names"), constStr(e, e.v("ncv"))))),
                                countLet(e, "pn"),
                                countLoop(e, "pn", e.v("names")),
                                countLet(e, "tn"),
                                countLoop(e, "tn", acc(e, xx(e), e.v("tyv"), "gfn", "params")),
                                e.ifOnly(
                                        e.not(e.eq(e.fld(e.v("pn"), "v"), e.fld(e.v("tn"), "v"))),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        e.cat(
                                                                e.s("glsl function has "),
                                                                toStr(e, e.fld(e.v("pn"), "v")),
                                                                e.s(" parameter names but its type has "),
                                                                toStr(e, e.fld(e.v("tn"), "v")),
                                                                e.s(" parameters"))))),
                                e.let("bodycv", ggfnField(e, "body")),
                                e.let(
                                        "refcv",
                                        ggfnStaged(
                                                e,
                                                e.callM(
                                                        ggfnMod(e),
                                                        "func",
                                                        e.rec(
                                                                e.p("name", graft(e, e.v("namecv"))),
                                                                e.p("params", e.lower(e.v("names"))),
                                                                e.p("ty", e.lower(e.v("tyv"))),
                                                                e.p("body", graft(e, e.v("bodycv"))))))),
                                e.ret(tagValue(e, e.v("refcv"), globalT(e, e.v("tyv")))))),
                e.p("exception_type", e.optionTypeLoweredNone())));
    }

    void defBuiltins(TList<SourceExpr> x) {
        var entries = new TList<EcoMb.P>();
        for (var row : BUILTINS) {
            var sigs = new TList<SourceExpr>();
            for (int i = 1; i < row.size(); i++) {
                var toks = row.get(i).split(" ");
                boolean anyG = false;
                boolean anyV = false;
                for (var tok : toks) {
                    switch (tok) {
                        case "G", "GI", "GU", "GB" -> anyG = true;
                        case "V", "IV", "UV", "BV", "M" -> anyV = true;
                        default -> {}
                    }
                }
                int lo = anyG ? 0 : 1;
                int hi = anyG || anyV ? 3 : 1;
                for (int k = lo; k <= hi; k++) {
                    var parts = new TList<String>();
                    for (var tok : toks) {
                        parts.add(
                                switch (tok) {
                                    case "G", "V" -> "f" + (k + 1);
                                    case "GI", "IV" -> "i" + (k + 1);
                                    case "GU", "UV" -> "u" + (k + 1);
                                    case "GB", "BV" -> "b" + (k + 1);
                                    case "M" -> "m" + (k + 1);
                                    case "F" -> "f1";
                                    case "I" -> "i1";
                                    case "U" -> "u1";
                                    case "B" -> "b1";
                                    case "F2" -> "f2";
                                    case "F3" -> "f3";
                                    default -> throw new IllegalArgumentException("unknown glsl family token " + tok);
                                });
                    }
                    sigs.add(e.s(String.join(" ", parts)));
                }
            }
            entries.add(e.p(row.get(0), e.listNew(str(e), sigs.toArray(SourceExpr[]::new))));
        }
        x.add(e.let("gsigs", e.mapNew(str(e), e.listType(str(e)), entries.toArray(EcoMb.P[]::new))));
        fnDef(e, x, "h_desc_match", e.rec(e.p("want", str(e)), e.p("have", str(e))), boolT(e));
        fnImpl(
                e,
                x,
                "h_desc_match",
                body(
                        e,
                        e.ifOnly(e.eq(a(e, "want"), a(e, "have")), retM(e, e.mb.as(e.b(true), boolT(e)))),
                        e.ifOnly(e.not(e.eq(a(e, "have"), e.s("?1"))), retM(e, dfalse(e))),
                        e.let("wd", hf(e, "h_parse_desc", a(e, "want"))),
                        e.ret(andB(
                                e,
                                e.eq(e.fld(e.v("wd"), "n"), num(e, 1)),
                                andB(
                                        e,
                                        e.not(e.fld(e.v("wd"), "mat")),
                                        e.not(e.eq(e.fld(e.v("wd"), "kind"), e.s("bool"))))))));
        selfType(
                e,
                x,
                "gbuiltin_t",
                e.cat(e.s("glsl builtin "), in(e, "name")),
                rtDispatch(e, lt(e, "gmod"), e.aspectDispatch(lv(e, "gbuiltin_m_id"), aspId(e, "function"))),
                canCastFromSelf(e),
                castFromSelf(e),
                noCastTo(e),
                castToPanic(e),
                body(
                        e,
                        e.ret(andB(
                                e,
                                has(e, xx(e), a(e, "other"), "gbuiltin_m"),
                                e.eq(acc(e, xx(e), a(e, "other"), "gbuiltin_m", "name"), in(e, "name"))))));
        rtLiftImpl(e, x, "gbuiltin_t");
        rtBindImpl(e, x, "gbuiltin_t", lt(e, "gmod"));
        rtUseImpl(e, x, "gbuiltin_t");
        x.add(e.implInterface(e.v("gbuiltin_t"), t(e, "gbuiltin_m"), e.p("name", in(e, "name"))));
        x.add(e.implInterface(
                e.v("gbuiltin_t"),
                aspIface(e, "function"),
                e.p(
                        "call",
                        body(
                                e,
                                e.let("mod", xmod(e)),
                                e.let("elems", elementsM(e, a(e, "argument"))),
                                e.let("descs", e.listNew(str(e))),
                                e.foreach(
                                        e.v("elems"),
                                        "ec",
                                        e.seq(
                                                e.ifOnly(
                                                        e.not(cveHas(e, e.v("ec"))),
                                                        retM(e, errCve(e, loc(e), "argument has no value"))),
                                                e.let("et0", cveType(e, e.v("ec"))),
                                                e.ifElse(
                                                        has(e, xx(e), e.v("et0"), "gtype"),
                                                        e.seq(
                                                                e.let(
                                                                        "et",
                                                                        e.ifElse(
                                                                                has(e, xx(e), e.v("et0"), "ggtype_m"),
                                                                                acc(
                                                                                        e,
                                                                                        xx(e),
                                                                                        e.v("et0"),
                                                                                        "ggtype_m",
                                                                                        "base"),
                                                                                e.v("et0"))),
                                                                e.let("d", descOf(e, xx(e), e.v("et"))),
                                                                push(
                                                                        e,
                                                                        e.v("descs"),
                                                                        e.cat(
                                                                                e.ifElse(
                                                                                        e.fld(e.v("d"), "mat"),
                                                                                        e.dstr("m"),
                                                                                        hf(
                                                                                                e,
                                                                                                "h_var_code",
                                                                                                e.fld(
                                                                                                        e.v("d"),
                                                                                                        "kind"))),
                                                                                toStr(e, e.fld(e.v("d"), "n"))))),
                                                        e.ifElse(
                                                                hasB(
                                                                        e,
                                                                        xx(e),
                                                                        e.v("et0"),
                                                                        e.evalBuiltin("aspect_bool_const")),
                                                                push(e, e.v("descs"), e.dstr("b1")),
                                                                e.ifElse(
                                                                        isConst(e, xx(e), e.v("et0")),
                                                                        push(e, e.v("descs"), e.dstr("?1")),
                                                                        retM(
                                                                                e,
                                                                                errCve(
                                                                                        e,
                                                                                        loc(e),
                                                                                        e.cat(
                                                                                                e.s("glsl: "),
                                                                                                tyDesc(e, e.v("et0")),
                                                                                                e.s(
                                                                                                        " is not a glsl value"))))))))),
                                e.let("sigs", e.callM(e.lowerV("gsigs"), "get", in(e, "name"))),
                                countLet(e, "nd"),
                                countLoop(e, "nd", e.v("descs")),
                                e.let("hit", e.listNew(str(e))),
                                e.foreach(
                                        e.v("sigs"),
                                        "sig",
                                        e.seq(
                                                e.let("parts", hf(e, "h_split", e.v("sig"))),
                                                countLet(e, "np"),
                                                countLoop(e, "np", e.v("parts")),
                                                e.ifOnly(
                                                        e.eq(
                                                                e.fld(e.v("np"), "v"),
                                                                e.mb.operatorAdd(e.fld(e.v("nd"), "v"), num(e, 1))),
                                                        e.seq(
                                                                e.let("ok", e.rec(e.p("v", e.intOf(1)))),
                                                                e.let("i", e.rec(e.p("v", e.intOf(0)))),
                                                                e.foreach(
                                                                        e.v("parts"),
                                                                        "pd",
                                                                        e.seq(
                                                                                e.ifOnly(
                                                                                        e.mb.operatorGt(
                                                                                                e.fld(e.v("i"), "v"),
                                                                                                num(e, 0)),
                                                                                        e.seq(
                                                                                                e.let(
                                                                                                        "j",
                                                                                                        e.rec(
                                                                                                                e.p(
                                                                                                                        "v",
                                                                                                                        e
                                                                                                                                .intOf(
                                                                                                                                        1)))),
                                                                                                e.foreach(
                                                                                                        e.v("descs"),
                                                                                                        "ad",
                                                                                                        e.seq(
                                                                                                                e
                                                                                                                        .ifOnly(
                                                                                                                                andB(
                                                                                                                                        e,
                                                                                                                                        e
                                                                                                                                                .eq(
                                                                                                                                                        e
                                                                                                                                                                .fld(
                                                                                                                                                                        e
                                                                                                                                                                                .v(
                                                                                                                                                                                        "j"),
                                                                                                                                                                        "v"),
                                                                                                                                                        e
                                                                                                                                                                .fld(
                                                                                                                                                                        e
                                                                                                                                                                                .v(
                                                                                                                                                                                        "i"),
                                                                                                                                                                        "v")),
                                                                                                                                        e
                                                                                                                                                .not(
                                                                                                                                                        hf(
                                                                                                                                                                e,
                                                                                                                                                                "h_desc_match",
                                                                                                                                                                e
                                                                                                                                                                        .p(
                                                                                                                                                                                "want",
                                                                                                                                                                                e
                                                                                                                                                                                        .v(
                                                                                                                                                                                                "pd")),
                                                                                                                                                                e
                                                                                                                                                                        .p(
                                                                                                                                                                                "have",
                                                                                                                                                                                e
                                                                                                                                                                                        .v(
                                                                                                                                                                                                "ad"))))),
                                                                                                                                e
                                                                                                                                        .mb
                                                                                                                                        .set(
                                                                                                                                                e
                                                                                                                                                        .fld(
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "ok"),
                                                                                                                                                                "v"),
                                                                                                                                                num(
                                                                                                                                                        e,
                                                                                                                                                        0))),
                                                                                                                e.incr(
                                                                                                                        e
                                                                                                                                .fld(
                                                                                                                                        e
                                                                                                                                                .v(
                                                                                                                                                        "j"),
                                                                                                                                        "v")))))),
                                                                                e.incr(e.fld(e.v("i"), "v")))),
                                                                e.ifOnly(
                                                                        e.eq(e.fld(e.v("ok"), "v"), num(e, 1)),
                                                                        push(e, e.v("hit"), e.v("sig"))))))),
                                countLet(e, "nh"),
                                countLoop(e, "nh", e.v("hit")),
                                e.ifOnly(
                                        e.eq(e.fld(e.v("nh"), "v"), num(e, 0)),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        e.cat(
                                                                e.s("glsl: no overload of "),
                                                                in(e, "name"),
                                                                e.s(" takes ("),
                                                                e.join(e.v("descs"), ", "),
                                                                e.s(")"))))),
                                e.let("chosen", e.rec(e.p("v", e.dstr("")))),
                                e.foreach(
                                        e.v("hit"),
                                        "h",
                                        e.ifOnly(
                                                e.eq(e.fld(e.v("chosen"), "v"), e.s("")),
                                                e.mb.set(e.fld(e.v("chosen"), "v"), e.v("h")))),
                                e.let("parts", hf(e, "h_split", e.fld(e.v("chosen"), "v"))),
                                e.let("out", instsNew(e)),
                                e.let("exprs", e.listNew(str(e))),
                                e.let("i", e.rec(e.p("v", e.intOf(0)))),
                                e.let("rty", e.rec(e.p("v", e.optionTypeLoweredNone()))),
                                e.foreach(
                                        e.v("parts"),
                                        "pd",
                                        e.seq(
                                                e.let("pty", typeOf(e, hf(e, "h_parse_desc", e.v("pd")))),
                                                e.ifOnly(
                                                        e.eq(e.fld(e.v("i"), "v"), num(e, 0)),
                                                        e.mb.set(
                                                                e.fld(e.v("rty"), "v"),
                                                                e.optionTypeLoweredSome(e.v("pty")))),
                                                e.ifOnly(
                                                        e.mb.operatorGt(e.fld(e.v("i"), "v"), num(e, 0)),
                                                        e.seq(
                                                                e.let("j", e.rec(e.p("v", e.intOf(1)))),
                                                                e.foreach(
                                                                        e.v("elems"),
                                                                        "ec",
                                                                        e.seq(
                                                                                e.ifOnly(
                                                                                        e.eq(
                                                                                                e.fld(e.v("j"), "v"),
                                                                                                e.fld(e.v("i"), "v")),
                                                                                        e.seq(
                                                                                                e.let(
                                                                                                        "ev",
                                                                                                        operandM(
                                                                                                                e,
                                                                                                                e.v(
                                                                                                                        "pty"),
                                                                                                                e.v(
                                                                                                                        "ec"))),
                                                                                                appendCode(
                                                                                                        e,
                                                                                                        e.v("out"),
                                                                                                        cveCode(
                                                                                                                e,
                                                                                                                e.v(
                                                                                                                        "ev"))),
                                                                                                push(
                                                                                                        e,
                                                                                                        e.v("exprs"),
                                                                                                        gvExpr(
                                                                                                                e,
                                                                                                                e.v(
                                                                                                                        "ev"))))),
                                                                                e.incr(e.fld(e.v("j"), "v")))))),
                                                e.incr(e.fld(e.v("i"), "v")))),
                                e.ret(okCve(
                                        e,
                                        hf(
                                                e,
                                                "h_stmt",
                                                e.p("mod", e.v("mod")),
                                                e.p("out", e.v("out")),
                                                e.p("ty", e.fld(e.v("rty"), "v", "value")),
                                                e.p(
                                                        "expr",
                                                        e.cat(
                                                                in(e, "name"),
                                                                e.s("("),
                                                                e.join(e.v("exprs"), ", "),
                                                                e.s(")")))))))),
                e.p("exception_type", e.optionTypeLoweredNone())));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var g = new EcoGlsl(e);
        var x = new TList<SourceExpr>();
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        g.defData(x);
        g.defHelpers(x);
        g.defArith(x);
        g.defTypes(x);
        g.defRefs(x);
        g.defModule(x);
        g.defTarget(x);
        g.defHooks(x);
        g.defBuiltins(x);
        fnDef(e, x, "c_fn", e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e))), ty(e));
        fnImpl(e, x, "c_fn", body(e, e.ret(mk(e, "gt_fn", e.p("params", a(e, "params")), e.p("ret", a(e, "ret"))))));
        fnDef(e, x, "c_desc", e.rec(e.p("xx", xxT(e)), e.p("ty", ty(e))), descT(e));
        fnImpl(e, x, "c_desc", body(e, e.ret(descOf(e, xx(e), a(e, "ty")))));
        x.add(e.let("glsl_b", e.defFunc(e.v("d"), "glsl")));
        x.add(e.funcImpl(
                e.v("glsl_b"),
                e.tup(),
                e.optionTypeSome(t(e, "gmod")),
                e.mb.optNoneType(),
                e.stage(e.seq(
                        e.let(
                                "m",
                                mk(
                                        e,
                                        "gmod",
                                        e.p("n", num(e, 0)),
                                        e.p("xx", e.optNone(e.typesExprContext())),
                                        e.p("decls", e.listNew(str(e))),
                                        e.p("funcs", e.listNew(str(e))))),
                        e.let("tgt", mk(e, "gtarget", e.p("mod", e.v("m")))),
                        e.let("vx", e.call(e.evalBuiltin("eval_context"), e.tup())),
                        e.let(
                                "sc",
                                e.call(
                                        e.evalBuiltin("scope"),
                                        e.rec(
                                                e.p("vx", e.v("vx")),
                                                e.p("entries", e.mapNew(e.mb.typesConstkey(), e.mb.evalBinding()))))),
                        e.let(
                                "ctx",
                                e.call(
                                        e.evalBuiltin("expr_context"),
                                        e.rec(
                                                e.p("scope", e.v("sc")),
                                                e.p("target", e.mb.as(e.v("tgt"), e.mb.evalTarget())),
                                                e.p("vx", e.v("vx")),
                                                e.p("exception_type", str(e))))),
                        e.callM(e.v("m"), "set_xx", e.v("ctx")),
                        e.ret(e.v("m"))))));
        x.add(e.finish(e.v("d")));
        var defs = new TList<EcoMb.P>();
        defs.add(e.p("new", e.fld(e.v("glsl_b"), "func")));
        defs.add(e.p("mod_t", t(e, "gmod")));
        defs.add(e.p("fn", e.v("c_fn")));
        defs.add(e.p("desc", e.v("c_desc")));
        defs.add(e.p("field_t", t(e, "gfield")));
        defs.add(e.p("field_new", e.fld(e.v("gfield"), "new")));
        for (var obj : new String[] {"gt_scalar", "gt_vec", "gt_mat", "gt_void", "ggfn_t", "gbuiltin_t"}) {
            defs.add(e.p(obj + "_new", e.fld(e.v(obj), "new")));
        }
        x.add(e.ret(e.rec(defs.toArray(EcoMb.P[]::new))));

        var y = new TList<SourceExpr>();
        y.add(e.let("k", e.lower(e.mb.seq(x))));
        var exported = new TList<String>();
        for (var kind : KINDS) {
            exported.add(kind);
            y.add(e.let(kind, lty(e, "gt_scalar", e.rec(e.p("kind", e.s(kind))))));
            for (int n = 2; n <= 4; n++) {
                var name = (kind.equals("float") ? "" : kind.substring(0, 1)) + "vec" + n;
                exported.add(name);
                y.add(e.let(name, lty(e, "gt_vec", e.rec(e.p("kind", e.s(kind)), e.p("n", e.intOf(n))))));
            }
        }
        for (int n = 2; n <= 4; n++) {
            exported.add("mat" + n);
            y.add(e.let("mat" + n, lty(e, "gt_mat", e.rec(e.p("n", e.intOf(n))))));
        }
        exported.add("void");
        y.add(e.let("void", lty(e, "gt_void", e.tup())));
        for (var kind : GGFN_KINDS) {
            exported.add(kind);
            y.add(e.let(kind, lty(e, "ggfn_t", e.rec(e.p("kind", e.intOf(ggfnKind(kind)))))));
        }
        var builtins = new TList<EcoMb.P>();
        for (var row : BUILTINS) {
            builtins.add(e.p(row.get(0), lty(e, "gbuiltin_t", e.rec(e.p("name", e.s(row.get(0)))))));
        }
        y.add(e.let("gbuiltins", e.rec(builtins.toArray(EcoMb.P[]::new))));
        y.add(e.let("d", e.define()));
        y.add(e.optionTypeLet());
        y.add(e.let("mod_o", e.defObject(e.v("d"), "glsl module")));
        y.add(e.objectImpl(e.v("mod_o"), e.rec(e.p("m", e.fld(e.v("k"), "mod_t")))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "render",
                e.tup(),
                e.optionTypeSome(renderT(e)),
                e.stage(e.callM(in(e, "m"), "render", e.tup()))));
        y.add(e.methodImpl(
                e.v("mod_o"), "m", e.tup(), e.optionTypeSome(e.fld(e.v("k"), "mod_t")), e.stage(in(e, "m"))));
        y.add(e.methodImpl(
                e.v("mod_o"), "decl", str(e), e.optionTypeNone(), e.stage(e.callM(in(e, "m"), "decl", e.v("arg")))));
        y.add(e.let("glsl_b", e.defFunc(e.v("d"), "glsl")));
        y.add(e.funcImpl(
                e.v("glsl_b"),
                e.tup(),
                e.optionTypeSome(e.fld(e.v("mod_o"), "type")),
                e.mb.optNoneType(),
                e.stage(mkArg(e, "mod_o", e.rec(e.p("m", e.call(e.fld(e.lowerV("k"), "new"), e.tup())))))));
        y.add(e.finish(e.v("d")));
        var ydefs = new TList<EcoMb.P>();
        ydefs.add(e.p("new", e.fld(e.v("glsl_b"), "func")));
        ydefs.add(e.p("mod_t", e.fld(e.v("mod_o"), "type")));
        for (var name : new String[] {"fn", "desc", "field_t", "field_new"})
            ydefs.add(e.p(name, e.fld(e.v("k"), name)));
        for (var name : exported) ydefs.add(e.p(name, e.v(name)));
        ydefs.add(e.p("builtins", e.v("gbuiltins")));
        y.add(e.ret(e.rec(ydefs.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(y))));
        var exports = new TList<EcoMb.P>();
        for (var name : new String[] {"new", "mod_t", "fn", "desc", "field_t", "field_new", "builtins"}) {
            exports.add(e.p(name, e.fld(e.v("k"), name)));
        }
        for (var name : exported) exports.add(e.p(name, e.fld(e.v("k"), name)));
        top.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return top;
    }

    private static SourceExpr lty(EcoMb e, String obj, SourceExpr arg) {
        return e.mb.as(e.call(e.fld(e.v("k"), obj + "_new"), arg), ty(e));
    }

    void defData(TList<SourceExpr> x) {
        defIface(e, x, "gfield_m");
        defObj(e, x, "gfield", "glsl block field", e.rec(e.p("name", str(e)), e.p("ty", ty(e))));
        accessor(e, x, "gfield", "name", str(e));
        accessor(e, x, "gfield", "ty", ty(e));
        defIface(e, x, "gtype", e.p("ir", str(e)));
        defIface(e, x, "gscalar", e.p("kind", str(e)));
        defIface(e, x, "gvec", e.p("kind", str(e)), e.p("n", intT(e)));
        defIface(e, x, "gmat", e.p("n", intT(e)));
        defIface(e, x, "gvoid");
        defIface(e, x, "gfn", e.p("params", e.listType(ty(e))), e.p("ret", ty(e)));
        defIface(e, x, "ggtype_m", e.p("base", ty(e)));
        defIface(e, x, "gblock_m", e.p("fields", e.listType(t(e, "gfield"))), e.p("name", str(e)));
        defIface(e, x, "ggfn_m", e.p("kind", intT(e)));
        defIface(e, x, "gbuiltin_m", e.p("name", str(e)));
        for (var vo : new String[] {"ginst", "gmod"}) defIface(e, x, vo + "_m");
        defObj(e, x, "ginst", "glsl statement", e.rec(e.p("text", str(e))));
        accessor(e, x, "ginst", "text", str(e));
        defObj(e, x, "gcode", "glsl code", e.rec(e.p("insts", instsT(e))));
        x.add(e.implInterface(e.v("gcode"), e.mb.builtin("eval", "code_dyn")));
        method(e, x, "gcode", "insts", e.tup(), instsT(e), in(e, "insts"));
        defObj(e, x, "gval", "glsl value", e.rec(e.p("ty", ty(e)), e.p("expr", str(e))));
        accessor(e, x, "gval", "expr", str(e));
        accessor(e, x, "gval", "ty", ty(e));
        x.add(e.implInterface(e.v("gval"), e.mb.evalValue(), e.p("type", in(e, "ty"))));
        defObj(
                e,
                x,
                "gmod",
                "glsl module",
                e.rec(
                        e.p("n", intT(e)),
                        e.p("xx", e.call(e.mb.typesOption(), e.typesExprContext())),
                        e.p("decls", e.listType(str(e))),
                        e.p("funcs", e.listType(str(e)))));
        defObj(e, x, "gtarget", "glsl target", e.rec(e.p("mod", gmodT(e))));
        accessor(e, x, "gtarget", "mod", gmodT(e));
        defObj(e, x, "gbinding", "glsl binding", e.rec(e.p("ty", ty(e)), e.p("name", str(e))));
        x.add(e.implInterface(
                e.v("gbinding"),
                e.mb.evalBinding(),
                e.p("use", cvOf(e, a(e, "code"), gvalNew(e, in(e, "ty"), in(e, "name")))),
                e.p("drop", e.eresultOk(e.mb.typesCode(), codeNone(e, loc(e)))),
                e.p("fork", outer(e)),
                e.p("merge", e.eresultOk(e.mb.evalBinding(), outer(e)))));
        defObj(e, x, "gt_scalar", "glsl scalar type", e.rec(e.p("kind", str(e))));
        defObj(e, x, "gt_vec", "glsl vector type", e.rec(e.p("kind", str(e)), e.p("n", intT(e))));
        defObj(e, x, "gt_mat", "glsl matrix type", e.rec(e.p("n", intT(e))));
        defObj(e, x, "gt_void", "glsl void type", e.tup());
        defObj(e, x, "gt_fn", "glsl function type", e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e))));
        defObj(e, x, "gt_global", "glsl global type", e.rec(e.p("base", ty(e))));
        defObj(
                e,
                x,
                "gt_block",
                "glsl uniform block type",
                e.rec(e.p("name", str(e)), e.p("fields", e.listType(t(e, "gfield")))));
        defObj(e, x, "ggfn_t", "glsl definition function", e.rec(e.p("kind", intT(e))));
        defObj(e, x, "gbuiltin_t", "glsl builtin function", e.rec(e.p("name", str(e))));
        for (var vo : new String[] {"ginst", "gmod", "gfield"}) {
            markerType(e, x, vo, vo, vo + "_m", e.aspectDispatch(lv(e, vo + "_m_id")));
            x.add(e.implInterface(e.v(vo), t(e, vo + "_m")));
        }
    }

    void defHelpers(TList<SourceExpr> x) {
        fnDef(e, x, "h_utf8", e.listType(e.mb.typesByte()), str(e));
        fnDef(e, x, "h_insts", codeT(e), instsT(e));
        fnDef(e, x, "h_code", instsT(e), codeT(e));
        x.add(e.let("h_append_b", e.defFunc(e.v("d"), "glsl.h_append")));
        x.add(e.let("h_append", e.fld(e.v("h_append_b"), "func")));
        e.fnSigs.put("h_append", new SourceExpr[] {
            e.rec(e.p("out", instsT(e)), e.p("code", codeT(e))), e.optionTypeNone(), e.optionTypeNone()
        });
        fnDef(e, x, "h_is_glsl", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), boolT(e));
        fnDef(e, x, "h_ir", e.rec(e.p("xx", xxT(e)), e.p("ty", ty(e))), str(e));
        fnDef(e, x, "h_desc", e.rec(e.p("xx", xxT(e)), e.p("ty", ty(e))), descT(e));
        fnDef(e, x, "h_type_of", descT(e), ty(e));
        fnDef(e, x, "h_parse_desc", str(e), descT(e));
        fnDef(e, x, "h_split", str(e), e.listType(str(e)));
        fnDef(e, x, "h_float_lit", e.typesDouble(), str(e));
        fnDef(
                e,
                x,
                "h_operand",
                e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("ty", ty(e)), e.p("cv", cveT(e))),
                cveT(e));
        fnThrows(e, "h_operand", errsT(e));
        fnDef(e, x, "h_const_str", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), str(e));
        fnDef(
                e,
                x,
                "h_stmt",
                e.rec(e.p("mod", gmodT(e)), e.p("out", instsT(e)), e.p("ty", ty(e)), e.p("expr", str(e))),
                cveT(e));
        fnDef(
                e,
                x,
                "h_arith",
                e.rec(
                        e.p("xx", xxT(e)),
                        e.p("loc", locT(e)),
                        e.p("mod", gmodT(e)),
                        e.p("op", str(e)),
                        e.p("self", ty(e)),
                        e.p("a", cveT(e)),
                        e.p("b", cveT(e))),
                cveT(e));
        fnThrows(e, "h_arith", errsT(e));
        fnDef(
                e,
                x,
                "h_cmp",
                e.rec(
                        e.p("xx", xxT(e)),
                        e.p("loc", locT(e)),
                        e.p("mod", gmodT(e)),
                        e.p("op", str(e)),
                        e.p("self", ty(e)),
                        e.p("a", cveT(e)),
                        e.p("b", cveT(e))),
                cveT(e));
        fnThrows(e, "h_cmp", errsT(e));
        fnDef(
                e,
                x,
                "h_unop",
                e.rec(
                        e.p("xx", xxT(e)),
                        e.p("loc", locT(e)),
                        e.p("mod", gmodT(e)),
                        e.p("op", str(e)),
                        e.p("ty", ty(e)),
                        e.p("a", cveT(e))),
                cveT(e));
        fnThrows(e, "h_unop", errsT(e));
        fnDef(e, x, "h_gval", e.rec(e.p("cv", cveT(e)), e.p("base", ty(e))), cveT(e));
        fnDef(
                e,
                x,
                "h_construct",
                e.rec(
                        e.p("xx", xxT(e)),
                        e.p("loc", locT(e)),
                        e.p("mod", gmodT(e)),
                        e.p("ty", ty(e)),
                        e.p("argument", cveT(e))),
                cveT(e));
        fnThrows(e, "h_construct", errsT(e));
        fnDef(e, x, "h_components", e.rec(e.p("xx", xxT(e)), e.p("ty", ty(e))), intT(e));
        fnDef(
                e,
                x,
                "h_elements",
                e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("cv", cveT(e))),
                e.listType(e.mb.typesCve()));
        fnThrows(e, "h_elements", errsT(e));
        fnImpl(
                e,
                x,
                "h_elements",
                body(
                        e,
                        e.let("els", e.listNew(e.mb.typesCve())),
                        e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "argument has no value")),
                        e.let("tk", lookup(e, xx(e), cveType(e, a(e, "cv")), aspIface(e, "tuple"))),
                        e.ifElse(
                                e.fld(e.v("tk"), "is_some"),
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
                                push(e, e.v("els"), a(e, "cv"))),
                        e.ret(e.v("els"))));

        fnImpl(
                e,
                x,
                "h_insts",
                body(
                        e,
                        e.ret(e.matchOf(
                                e.v("arg"),
                                "c",
                                Opt.none(),
                                e.p("none", instsNew(e)),
                                e.p(
                                        "target",
                                        e.matchOf(
                                                e.callM(e.v("c"), "cast", lt(e, "gcode")),
                                                "r",
                                                Opt.none(),
                                                e.p("ok", e.callM(e.v("r"), "insts", e.tup())),
                                                e.p(
                                                        "errs",
                                                        e.mb.panic(e.s("glsl: code belongs to another target")))))))));
        fnImpl(e, x, "h_code", body(e, e.ret(e.mb.as(mkArg(e, "gcode", e.rec(e.p("insts", e.v("arg")))), codeT(e)))));
        fnImpl(
                e,
                x,
                "h_append",
                body(e, e.foreach(hf(e, "h_insts", a(e, "code")), "i", push(e, a(e, "out"), e.v("i")))));
        fnImpl(
                e,
                x,
                "h_is_glsl",
                body(
                        e,
                        e.ret(e.ifElse(
                                cveHas(e, a(e, "cv")), has(e, xx(e), cveType(e, a(e, "cv")), "gtype"), e.b(false)))));
        fnImpl(
                e,
                x,
                "h_ir",
                body(
                        e,
                        e.let("lk", lookup(e, xx(e), a(e, "ty"), lt(e, "gtype"))),
                        e.ifOnly(
                                e.not(e.fld(e.v("lk"), "is_some")),
                                e.mb.panic(e.cat(e.s("glsl: not a glsl type: "), tyDesc(e, a(e, "ty"))))),
                        e.ret(bim0(e, lt(e, "gtype"), "ir", e.fld(e.v("lk"), "value")))));
        fnImpl(
                e,
                x,
                "h_desc",
                body(
                        e,
                        e.ifOnly(
                                has(e, xx(e), a(e, "ty"), "gscalar"),
                                retM(
                                        e,
                                        e.rec(
                                                e.p("kind", acc(e, xx(e), a(e, "ty"), "gscalar", "kind")),
                                                e.p("n", e.intOf(1)),
                                                e.p("mat", dfalse(e))))),
                        e.ifOnly(
                                has(e, xx(e), a(e, "ty"), "gvec"),
                                retM(
                                        e,
                                        e.rec(
                                                e.p("kind", acc(e, xx(e), a(e, "ty"), "gvec", "kind")),
                                                e.p("n", acc(e, xx(e), a(e, "ty"), "gvec", "n")),
                                                e.p("mat", dfalse(e))))),
                        e.ifOnly(
                                has(e, xx(e), a(e, "ty"), "gmat"),
                                retM(
                                        e,
                                        e.rec(
                                                e.p("kind", e.dstr("float")),
                                                e.p("n", acc(e, xx(e), a(e, "ty"), "gmat", "n")),
                                                e.p("mat", e.mb.as(e.b(true), boolT(e)))))),
                        e.mb.panic(e.cat(e.s("glsl: type has no components: "), tyDesc(e, a(e, "ty"))))));
        fnImpl(
                e,
                x,
                "h_type_of",
                body(
                        e,
                        e.ifOnly(a(e, "mat"), retM(e, e.mb.as(mk(e, "gt_mat", e.p("n", a(e, "n"))), ty(e)))),
                        e.ifOnly(e.eq(a(e, "n"), num(e, 1)), retM(e, e.mb.as(scalarT(e, a(e, "kind")), ty(e)))),
                        e.ret(e.mb.as(vecT(e, a(e, "kind"), a(e, "n")), ty(e)))));
        fnImpl(
                e,
                x,
                "h_parse_desc",
                body(
                        e,
                        e.let("st", e.rec(e.p("kind", e.dstr("")), e.p("n", e.intOf(0)), e.p("i", e.intOf(0)))),
                        e.foreach(
                                e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                "b",
                                e.seq(
                                        e.let("c", e.mb.as(e.v("b"), intT(e))),
                                        e.ifElse(
                                                e.eq(e.fld(e.v("st"), "i"), num(e, 0)),
                                                e.mb.set(
                                                        e.fld(e.v("st"), "kind"),
                                                        e.ifElse(
                                                                e.eq(e.v("c"), num(e, 'f')),
                                                                e.dstr("float"),
                                                                e.ifElse(
                                                                        e.eq(e.v("c"), num(e, 'i')),
                                                                        e.dstr("int"),
                                                                        e.ifElse(
                                                                                e.eq(e.v("c"), num(e, 'u')),
                                                                                e.dstr("uint"),
                                                                                e.ifElse(
                                                                                        e.eq(e.v("c"), num(e, 'b')),
                                                                                        e.dstr("bool"),
                                                                                        e.dstr("mat")))))),
                                                e.mb.set(
                                                        e.fld(e.v("st"), "n"),
                                                        e.mb.operatorSub(e.v("c"), e.intOf('0')))),
                                        e.incr(e.fld(e.v("st"), "i")))),
                        e.ifOnly(
                                e.eq(e.fld(e.v("st"), "kind"), e.s("mat")),
                                retM(
                                        e,
                                        e.rec(
                                                e.p("kind", e.dstr("float")),
                                                e.p("n", e.fld(e.v("st"), "n")),
                                                e.p("mat", e.mb.as(e.b(true), boolT(e)))))),
                        e.ret(e.rec(
                                e.p("kind", e.fld(e.v("st"), "kind")),
                                e.p("n", e.fld(e.v("st"), "n")),
                                e.p("mat", dfalse(e))))));
        fnImpl(
                e,
                x,
                "h_split",
                body(
                        e,
                        e.let("out", e.listNew(str(e))),
                        e.let("cur", e.rec(e.p("b", e.listNew(e.mb.typesByte())))),
                        e.foreach(
                                e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                "b",
                                e.ifElse(
                                        e.eq(e.mb.as(e.v("b"), intT(e)), num(e, ' ')),
                                        e.seq(
                                                push(e, e.v("out"), hf(e, "h_utf8", e.fld(e.v("cur"), "b"))),
                                                e.mb.set(e.fld(e.v("cur"), "b"), e.listNew(e.mb.typesByte()))),
                                        push(e, e.fld(e.v("cur"), "b"), e.v("b")))),
                        push(e, e.v("out"), hf(e, "h_utf8", e.fld(e.v("cur"), "b"))),
                        e.ret(e.v("out"))));
        fnImpl(
                e,
                x,
                "h_float_lit",
                body(
                        e,
                        e.let("s", toStr(e, e.v("arg"))),
                        e.let("st", e.rec(e.p("frac", e.intOf(0)))),
                        e.foreach(
                                e.call(e.mb.evalStrUtf8(), e.v("s")),
                                "b",
                                e.seq(
                                        e.let("c", e.mb.as(e.v("b"), intT(e))),
                                        e.ifOnly(
                                                orB(
                                                        e,
                                                        e.eq(e.v("c"), num(e, '.')),
                                                        orB(
                                                                e,
                                                                e.eq(e.v("c"), num(e, 'e')),
                                                                orB(
                                                                        e,
                                                                        e.eq(e.v("c"), num(e, 'E')),
                                                                        e.eq(e.v("c"), num(e, 'n'))))),
                                                e.mb.set(e.fld(e.v("st"), "frac"), e.intOf(1))))),
                        e.ret(e.ifElse(
                                e.eq(e.fld(e.v("st"), "frac"), num(e, 1)), e.v("s"), e.cat(e.v("s"), e.s(".0"))))));
        fnImpl(
                e,
                x,
                "h_operand",
                body(
                        e,
                        e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "operand has no value")),
                        e.ret(unwrapCveT(e, castValue(e, xx(e), loc(e), a(e, "ty"), a(e, "cv"))))));
        fnImpl(
                e,
                x,
                "h_const_str",
                body(
                        e,
                        e.let("sk", lookup(e, xx(e), cveType(e, a(e, "cv")), e.mb.evalAspectStringConst())),
                        e.ifOnly(
                                e.not(e.fld(e.v("sk"), "is_some")),
                                e.mb.panic(e.s("glsl: expected a constant string"))),
                        e.ret(bim(
                                e,
                                e.mb.evalAspectStringConst(),
                                "get",
                                e.fld(e.v("sk"), "value"),
                                cveValue(e, a(e, "cv"))))));
        fnImpl(
                e,
                x,
                "h_stmt",
                body(
                        e,
                        e.let("r", fresh(e, a(e, "mod"))),
                        push(
                                e,
                                a(e, "out"),
                                inst(
                                        e,
                                        e.cat(
                                                irOf(e, e.callM(a(e, "mod"), "xx", e.tup()), a(e, "ty")),
                                                e.s(" "),
                                                e.v("r"),
                                                e.s(" = "),
                                                a(e, "expr"),
                                                e.s(";")))),
                        e.ret(cvOf(e, codeOf(e, a(e, "out")), gvalNew(e, a(e, "ty"), e.v("r"))))));
        fnImpl(
                e,
                x,
                "h_gval",
                body(
                        e,
                        e.ret(cvOf(
                                e,
                                cveCode(e, a(e, "cv")),
                                gvalNew(
                                        e,
                                        a(e, "base"),
                                        e.call(
                                                e.mb.mustReinterpret(),
                                                e.tup(
                                                        e.fld(
                                                                e.call(
                                                                        e.mb.mustReinterpret(),
                                                                        e.tup(cveValue(e, a(e, "cv")), vocT(e))),
                                                                "data"),
                                                        str(e))))))));
        fnImpl(
                e,
                x,
                "h_components",
                body(
                        e,
                        e.let("d", descOf(e, xx(e), a(e, "ty"))),
                        e.ret(e.ifElse(
                                e.fld(e.v("d"), "mat"),
                                e.mb.operatorMul(e.fld(e.v("d"), "n"), e.fld(e.v("d"), "n")),
                                e.fld(e.v("d"), "n")))));
        fnImpl(
                e,
                x,
                "h_utf8",
                body(
                        e,
                        e.ret(e.matchOf(
                                e.call(e.evalBuiltin("str_from_utf8"), e.v("arg")),
                                "__s",
                                Opt.none(),
                                e.p("some", e.v("__s")),
                                e.p("none", e.mb.panic(e.s("glsl: text is not valid UTF-8")))))));
    }
}
