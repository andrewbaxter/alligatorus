package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoWat {
    private EcoWat() {}

    private static final String TARGET = "wasm";

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        return EcoTargetMb.fnDef(e, out, TARGET, name, argType, ret);
    }

    private static SourceExpr mustV(EcoMb e, SourceExpr res, String what) {
        return EcoTargetMb.mustV(e, TARGET, res, what);
    }

    private static SourceExpr panicErrs(EcoMb e, String what, SourceExpr errs) {
        return EcoTargetMb.panicErrs(e, TARGET, what, errs);
    }

    private static SourceExpr wvalT(EcoMb e) {
        return t(e, "wval");
    }

    private static SourceExpr wmodT(EcoMb e) {
        return t(e, "wmod");
    }

    private static SourceExpr renderT(EcoMb e) {
        return e.rec(e.p("wat", str(e)));
    }

    private static SourceExpr instsT(EcoMb e) {
        return e.listType(t(e, "winst"));
    }

    private record IntSpec(String name, double bits, boolean signed, double min, double max) {}

    private static final IList<IntSpec> INT_SPECS = IList.of(
            new IntSpec("i1", 1, false, 0, 1),
            new IntSpec("i8", 8, true, -128, 127),
            new IntSpec("i16", 16, true, -32768, 32767),
            new IntSpec("i32", 32, true, -2147483648.0, 2147483647.0),
            new IntSpec("i64", 64, true, -9223372036854775808.0, 9223372036854775807.0),
            new IntSpec("u8", 8, false, 0, 255),
            new IntSpec("u16", 16, false, 0, 65535),
            new IntSpec("u32", 32, false, 0, 4294967295.0),
            new IntSpec("u64", 64, false, 0, 18446744073709551615.0));

    private static SourceExpr intTy(EcoMb e, String name) {
        for (var spec : INT_SPECS) {
            if (spec.name().equals(name)) {
                return mk(
                        e,
                        "wt_int",
                        e.p("bits", num(e, spec.bits())),
                        e.p("signed", e.b(spec.signed())),
                        e.p("min", e.n(spec.min())),
                        e.p("max", e.n(spec.max())));
            }
        }
        throw new IllegalArgumentException("no int type " + name);
    }

    private static SourceExpr lty(EcoMb e, String obj, SourceExpr arg) {
        return e.mb.as(e.call(e.fld(e.v("k"), obj + "_new"), arg), ty(e));
    }

    private static SourceExpr wvalOf(EcoMb e, SourceExpr cveTP) {
        return reinterpret(e, cveValue(e, cveTP), "wval");
    }

    private static SourceExpr wvalNew(EcoMb e, SourceExpr ty, String sourceTag, SourceExpr ref) {
        return mk(e, "wval", e.p("ty", ty), e.p("source", e.call(e.lowerV("ws_" + sourceTag), ref)));
    }

    private static SourceExpr wvalLocal(EcoMb e, SourceExpr ty, SourceExpr name) {
        return wvalNew(e, ty, "local", name);
    }

    private static SourceExpr wvalKonst(EcoMb e, SourceExpr ty, SourceExpr text) {
        return wvalNew(e, ty, "konst", text);
    }

    private static SourceExpr wvalSlot(EcoMb e, SourceExpr ty, SourceExpr name) {
        return wvalNew(e, ty, "slot", name);
    }

    private static SourceExpr wvalFunc(EcoMb e, SourceExpr ty, SourceExpr name) {
        return wvalNew(e, ty, "func", name);
    }

    private static SourceExpr wvRef(EcoMb e, SourceExpr wvalTP) {
        return e.callM(wvalTP, "ref", e.tup());
    }

    private static SourceExpr wvMem(EcoMb e, SourceExpr wvalTP) {
        return e.callM(wvalTP, "mem", e.tup());
    }

    private static SourceExpr wvIsMem(EcoMb e, SourceExpr wvalTP) {
        return e.eq(e.callM(wvalTP, "is_mem", e.tup()), num(e, 1));
    }

    private static SourceExpr wvTy(EcoMb e, SourceExpr wvalTP) {
        return e.callM(wvalTP, "ty", e.tup());
    }

    private static SourceExpr operand(
            EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr mod, SourceExpr ty, SourceExpr cv) {
        return hf(e, "h_operand", e.p("xx", xx), e.p("loc", loc), e.p("mod", mod), e.p("ty", ty), e.p("cv", cv));
    }

    private static SourceExpr instsNew(EcoMb e) {
        return e.listNew(lt(e, "winst"));
    }

    private static SourceExpr inst(EcoMb e, String variant, EcoMb.P... fields) {
        return mk(e, "winst", e.p("k", e.call(e.lowerV("wi_" + variant), e.rec(fields))));
    }

    private static SourceExpr inst0(EcoMb e, String variant) {
        return mk(e, "winst", e.p("k", e.call(e.lowerV("wi_" + variant), e.tup())));
    }

    private static SourceExpr appendCode(EcoMb e, SourceExpr out, SourceExpr codeTP) {
        return hf(e, "h_append", e.p("out", out), e.p("code", codeTP));
    }

    private static SourceExpr codeOf(EcoMb e, SourceExpr insts) {
        return hf(e, "h_code", insts);
    }

    private static SourceExpr ssa(EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr mod, SourceExpr cv) {
        return hf(e, "h_ssa", e.p("xx", xx), e.p("loc", loc), e.p("mod", mod), e.p("cv", cv));
    }

    private static SourceExpr isWasm(EcoMb e, SourceExpr xx, SourceExpr cv) {
        return hf(e, "h_is_wasm", e.p("xx", xx), e.p("cv", cv));
    }

    private static SourceExpr mod(EcoMb e) {
        return in(e, "mod");
    }

    private static SourceExpr xmod(EcoMb e) {
        return xmod(e, e.mb.typesCve());
    }

    private static SourceExpr xmod(EcoMb e, SourceExpr retInner) {
        return e.callM(
                unwrapM(
                        e,
                        e.call(e.mb.reinterpret(), e.tup(loc(e), e.fld(xx(e), "target"), lt(e, "wtarget"))),
                        retInner),
                "mod",
                e.tup());
    }

    private static SourceExpr fresh(EcoMb e, SourceExpr mod, SourceExpr ty) {
        return e.callM(mod, "fresh", ty);
    }

    private static SourceExpr storeInst(EcoMb e, SourceExpr val, SourceExpr name) {
        return inst(e, "store", e.p("val", val), e.p("name", name));
    }

    private static SourceExpr loadInst(EcoMb e, SourceExpr res, SourceExpr ty, SourceExpr name) {
        return inst(e, "load", e.p("res", res), e.p("ty", ty), e.p("name", name));
    }

    private static SourceExpr labelInst(EcoMb e, SourceExpr name) {
        return inst(e, "label", e.p("name", name));
    }

    private static SourceExpr brInst(EcoMb e, SourceExpr name) {
        return inst(e, "br", e.p("name", name));
    }

    private static SourceExpr irOf(EcoMb e, SourceExpr ty) {
        return e.callM(outer(e), "ir", ty);
    }

    private static SourceExpr p(EcoMb e, String key) {
        return e.fld(e.v("p"), key);
    }

    private static SourceExpr irArgXx(EcoMb e) {
        return e.callM(e.v("arg"), "xx", e.tup());
    }

    private static SourceExpr irFn(EcoMb e, SourceExpr xx, SourceExpr ty) {
        return hf(e, "h_ir", e.p("xx", xx), e.p("ty", ty));
    }

    private static SourceExpr binopBody(EcoMb e, SourceExpr op) {
        return hf(
                e,
                "h_binop",
                e.p("xx", xx(e)),
                e.p("loc", loc(e)),
                e.p("mod", xmod(e)),
                e.p("ty", outer(e)),
                e.p("op", op),
                e.p("a", a(e, "base")),
                e.p("b", a(e, "reference")),
                e.p("res", outer(e)));
    }

    private static SourceExpr cmpBody(EcoMb e, SourceExpr op) {
        return hf(
                e,
                "h_cmp",
                e.p("xx", xx(e)),
                e.p("loc", loc(e)),
                e.p("mod", xmod(e)),
                e.p("ty", outer(e)),
                e.p("op", op),
                e.p("a", a(e, "base")),
                e.p("b", a(e, "reference")));
    }

    private static SourceExpr signedOp(EcoMb e, String signed, String unsigned) {
        return e.ifElse(in(e, "signed"), e.dstr(signed), e.dstr(unsigned));
    }

    private static SourceExpr conv(EcoMb e, SourceExpr op, SourceExpr from, SourceExpr to) {
        return okCve(e, hf(e, "h_conv", e.p("mod", xmod(e)), e.p("op", op), e.p("a", from), e.p("to", to)));
    }

    private static SourceExpr retag(EcoMb e, SourceExpr cv, SourceExpr ty) {
        return okCve(e, hf(e, "h_retag", e.p("mod", xmod(e)), e.p("cv", cv), e.p("ty", ty)));
    }

    private static SourceExpr narrow(EcoMb e, SourceExpr cv, SourceExpr ty) {
        return okCve(e, hf(e, "h_narrow", e.p("xx", xx(e)), e.p("mod", xmod(e)), e.p("cv", cv), e.p("to", ty)));
    }

    private static SourceExpr literal(EcoMb e, SourceExpr ty, SourceExpr text) {
        return hf(e, "h_literal", e.p("loc", loc(e)), e.p("ty", ty), e.p("text", text));
    }

    private static SourceExpr convName(EcoMb e, SourceExpr to, String op, SourceExpr from, Opt<SourceExpr> signed) {
        var parts = new TList<SourceExpr>();
        parts.add(e.callM(xmod(e), "ir", to));
        parts.add(e.s("." + op + "_"));
        parts.add(e.callM(xmod(e), "ir", from));
        if (signed.optIsSome()) parts.add(e.ifElse(signed.optUnwrap(), e.dstr("_s"), e.dstr("_u")));
        return e.cat(parts.toArray(SourceExpr[]::new));
    }

    private static void setImpl(EcoMb e, TList<SourceExpr> x, String obj) {
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectSet(),
                e.p(
                        "set",
                        body(
                                e,
                                e.ret(tryEr(
                                        e,
                                        hf(
                                                e,
                                                "h_set",
                                                e.p("xx", xx(e)),
                                                e.p("loc", loc(e)),
                                                e.p("mod", xmod(e)),
                                                e.p("ty", outer(e)),
                                                e.p("self", a(e, "self")),
                                                e.p("value", a(e, "value"))),
                                        cveT(e)))))));
    }

    private static void bindImpl(EcoMb e, TList<SourceExpr> x, String obj) {
        var brT = e.mb.typesBindResult();
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectBind(),
                e.p(
                        "bind",
                        body(
                                e,
                                e.let(
                                        "r",
                                        unwrapM(
                                                e,
                                                tryEr(
                                                        e,
                                                        hf(
                                                                e,
                                                                "h_store_new",
                                                                e.p("xx", xx(e)),
                                                                e.p("loc", loc(e)),
                                                                e.p("mod", xmod(e, brT)),
                                                                e.p("cv", a(e, "cv"))),
                                                        cveT(e)),
                                                brT)),
                                e.ret(e.eresultOk(
                                        brT,
                                        e.call(
                                                e.mb.evalBindResult(),
                                                e.rec(
                                                        e.p("code", cveCode(e, e.v("r"))),
                                                        e.p(
                                                                "binding",
                                                                mk(
                                                                        e,
                                                                        "wbinding",
                                                                        e.p("ty", wvTy(e, wvalOf(e, e.v("r")))),
                                                                        e.p(
                                                                                "slot",
                                                                                wvMem(
                                                                                        e,
                                                                                        wvalOf(e, e.v("r"))))))))))))));
    }

    private static void convImpl(EcoMb e, TList<SourceExpr> x, String obj, String what, SourceExpr... cases) {
        var parts = new TList<SourceExpr>();
        parts.add(e.let("cv", a(e, "argument")));
        parts.add(e.ifOnly(e.not(cveHas(e, e.v("cv"))), retM(e, errCve(e, loc(e), "expression has no value"))));
        parts.add(e.let("st0", cveType(e, e.v("cv"))));
        parts.add(e.ifOnly(isConst(e, xx(e), e.v("st0")), retM(e, castValue(e, xx(e), loc(e), outer(e), e.v("cv")))));
        parts.add(e.let("v", unwrapCveM(e, tryEr(e, ssa(e, xx(e), loc(e), xmod(e), e.v("cv")), cveT(e)))));
        parts.add(e.let("st", cveType(e, e.v("v"))));
        for (var c : cases) parts.add(c);
        parts.add(e.ret(errCve(e, loc(e), "cannot convert to " + what)));
        x.add(e.implInterface(
                e.v(obj),
                aspIface(e, "function"),
                e.p("call", body(e, parts.toArray(SourceExpr[]::new))),
                e.p("exception_type", e.optionTypeLoweredNone())));
    }

    private static void bindingImpl(EcoMb e, TList<SourceExpr> x, String obj, SourceExpr useBody) {
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalBinding(),
                e.p("use", useBody),
                e.p("drop", e.eresultOk(e.mb.typesCode(), codeNone(e, loc(e)))),
                e.p("fork", outer(e)),
                e.p("merge", e.eresultOk(e.mb.evalBinding(), outer(e)))));
    }

    private static SourceExpr wide(EcoMb e, SourceExpr xx, SourceExpr typ) {
        return e.eq(acc(e, xx, typ, "wint", "bits"), num(e, 64));
    }

    private static SourceExpr ifBranchStmts(EcoMb e, String cvVar) {
        var valued = andB(e, cveHas(e, e.v(cvVar)), isWasm(e, xx(e), e.v(cvVar)));
        return e.seq(
                e.ifOnly(
                        andB(
                                e,
                                cveHas(e, e.v(cvVar)),
                                andB(
                                        e,
                                        e.not(isWasm(e, xx(e), e.v(cvVar))),
                                        e.not(hf(e, "h_is_unit", e.p("xx", xx(e)), e.p("cv", e.v(cvVar)))))),
                        retM(e, errCve(e, loc(e), "if branches must yield wasm values"))),
                e.ifOnly(
                        valued,
                        e.seq(
                                e.let(
                                        "bv",
                                        unwrapCveM(
                                                e, tryEr(e, ssa(e, xx(e), loc(e), e.v("mod"), e.v(cvVar)), cveT(e)))),
                                e.ifOnly(
                                        e.eq(e.fld(e.v("st"), "has"), num(e, 0)),
                                        e.seq(
                                                e.mb.set(
                                                        e.fld(e.v("st"), "ty"),
                                                        e.optionTypeLoweredSome(cveType(e, e.v("bv")))),
                                                e.mb.set(e.fld(e.v("st"), "has"), num(e, 1)))),
                                appendCode(e, e.v("bo"), cveCode(e, e.v("bv"))),
                                push(e, e.v("bo"), inst(e, "push", e.p("val", wvalOf(e, e.v("bv"))))))),
                e.ifOnly(
                        e.not(valued),
                        e.seq(
                                appendCode(e, e.v("bo"), cveCode(e, e.v(cvVar))),
                                e.ifOnly(
                                        e.not(cveEnded(e, e.v(cvVar))),
                                        e.mb.set(e.fld(e.v("st"), "novalue"), num(e, 1))))),
                e.ifOnly(e.not(cveEnded(e, e.v(cvVar))), e.mb.set(e.fld(e.v("st"), "ended"), num(e, 0))));
    }

    private static final IList<String> WGFN_KINDS = IList.of("import_fn", "func_fn", "data_fn", "cstr_fn");

    private static int wgfnKind(String kind) {
        for (int i = 0; i < WGFN_KINDS.size(); i++) {
            if (WGFN_KINDS.get(i).equals(kind)) return i;
        }
        throw new IllegalArgumentException("no wasm global definition kind " + kind);
    }

    private static SourceExpr wgfnIs(EcoMb e, String kind) {
        return e.eq(in(e, "kind"), num(e, wgfnKind(kind)));
    }

    private static SourceExpr wgfnStaged(EcoMb e, SourceExpr staged) {
        return unwrapCveM(e, constEvalIn(e, xx(e), e.stage(staged)));
    }

    private static SourceExpr wgfnGlobalT(EcoMb e, SourceExpr base) {
        return mk(e, "wt_global", e.p("base", base));
    }

    private static SourceExpr wgfnField(EcoMb e, String key) {
        return unwrapCveM(e, e.callM(e.v("fields"), "get", e.s(key)));
    }

    private static SourceExpr wgfnMod(EcoMb e) {
        return e.callM(graft(e, e.v("modcv")), "m", e.tup());
    }

    private static SourceExpr dataFnBody(EcoMb e) {
        return e.seq(
                e.let("elemcv", wgfnField(e, "elem")),
                e.let("elem", constTypeOf(e, "elemcv", "wasm data elem")),
                e.let("is_ptr", has(e, xx(e), e.v("elem"), "wptr")),
                e.ifOnly(
                        e.not(orB(e, e.v("is_ptr"), has(e, xx(e), e.v("elem"), "wint"))),
                        retM(
                                e,
                                errCve(
                                        e,
                                        loc(e),
                                        e.cat(
                                                e.s("wasm data elem must be an integer or pointer type, not "),
                                                tyDesc(e, e.v("elem")))))),
                e.let(
                        "width",
                        e.ifElse(
                                e.v("is_ptr"),
                                e.intOf(4),
                                e.mb.operatorDiv(
                                        e.mb.operatorAdd(acc(e, xx(e), e.v("elem"), "wint", "bits"), e.intOf(7)),
                                        e.intOf(8)))),
                e.let(
                        "min",
                        e.ifElse(
                                e.v("is_ptr"),
                                e.mb.as(e.n(0), e.typesDouble()),
                                acc(e, xx(e), e.v("elem"), "wint", "min"))),
                e.let(
                        "max",
                        e.ifElse(
                                e.v("is_ptr"),
                                e.mb.as(e.n(4294967295.0), e.typesDouble()),
                                acc(e, xx(e), e.v("elem"), "wint", "max"))),
                e.let("valuescv", wgfnField(e, "values")),
                e.let("vtk", lookup(e, xx(e), cveType(e, e.v("valuescv")), aspIface(e, "tuple"))),
                e.ifOnly(
                        e.not(e.fld(e.v("vtk"), "is_some")),
                        retM(e, errCve(e, loc(e), "wasm data values must be a tuple"))),
                e.let("accs", e.mapNew(str(e), cveT(e))),
                e.callM(e.v("accs"), "put", e.tup(e.s("0"), wgfnStaged(e, e.listNew(intT(e))))),
                e.let("fi", e.rec(e.p("i", e.intOf(0)))),
                e.foreach(
                        bim(
                                e,
                                aspIface(e, "tuple"),
                                "take_elements",
                                e.fld(e.v("vtk"), "value"),
                                e.rec(
                                        e.p("xx", xx(e)),
                                        e.p("loc", loc(e)),
                                        e.p("value", cveValue(e, e.v("valuescv"))))),
                        "er",
                        e.seq(
                                e.let("ecv", unwrapCveM(e, e.v("er"))),
                                e.let("ety", cveType(e, e.v("ecv"))),
                                e.let("is_ref", has(e, xx(e), e.v("ety"), "wgtype_m")),
                                e.ifOnly(
                                        andB(
                                                e,
                                                e.v("is_ref"),
                                                e.not(
                                                        has(
                                                                e,
                                                                xx(e),
                                                                acc(e, xx(e), e.v("ety"), "wgtype_m", "base"),
                                                                "wptr"))),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        "wasm data values may only reference data pointers"))),
                                e.ifOnly(
                                        andB(e, e.v("is_ref"), e.not(e.eq(e.v("width"), num(e, 4)))),
                                        retM(e, errCve(e, loc(e), "wasm data pointer values need a 32 bit elem"))),
                                e.ifOnly(
                                        andB(e, e.not(e.v("is_ref")), e.not(isConst(e, xx(e), e.v("ety")))),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        "wasm data values must be integers or data pointers"))),
                                e.let(
                                        "valuecv",
                                        e.ifElse(
                                                e.v("is_ref"),
                                                wgfnStaged(
                                                        e,
                                                        e.mb.as(
                                                                e.callM(
                                                                        wgfnMod(e),
                                                                        "addr",
                                                                        e.call(
                                                                                e.mb.mustReinterpret(),
                                                                                e.tup(graft(e, e.v("ecv")), str(e)))),
                                                                e.typesDouble())),
                                                wgfnStaged(e, e.mb.as(graft(e, e.v("ecv")), e.typesDouble())))),
                                e.let("k0", toStr(e, e.fld(e.v("fi"), "i"))),
                                e.incr(e.fld(e.v("fi"), "i")),
                                e.callM(
                                        e.v("accs"),
                                        "put",
                                        e.tup(
                                                toStr(e, e.fld(e.v("fi"), "i")),
                                                wgfnStaged(
                                                        e,
                                                        e.callM(
                                                                wgfnMod(e),
                                                                "data_push",
                                                                e.rec(
                                                                        e.p(
                                                                                "bytes",
                                                                                graft(
                                                                                        e,
                                                                                        e.callM(
                                                                                                e.v("accs"),
                                                                                                "get",
                                                                                                e.v("k0")))),
                                                                        e.p("width", e.lower(e.v("width"))),
                                                                        e.p("min", e.lower(e.v("min"))),
                                                                        e.p("max", e.lower(e.v("max"))),
                                                                        e.p("value", graft(e, e.v("valuecv")))))))))),
                e.let("namecv", wgfnField(e, "name")),
                e.let(
                        "refcv",
                        wgfnStaged(
                                e,
                                e.callM(
                                        wgfnMod(e),
                                        "data",
                                        e.rec(
                                                e.p("name", graft(e, e.v("namecv"))),
                                                e.p(
                                                        "bytes",
                                                        graft(
                                                                e,
                                                                e.callM(
                                                                        e.v("accs"),
                                                                        "get",
                                                                        toStr(e, e.fld(e.v("fi"), "i"))))))))),
                retM(e, tagValue(e, e.v("refcv"), wgfnGlobalT(e, mk(e, "wt_ptr", e.p("pointee", e.v("elem")))))));
    }

    private static SourceExpr constTypeOf(EcoMb e, String cvVar, String what) {
        return e.seq(
                e.let("tk", lookup(e, xx(e), cveType(e, e.v(cvVar)), aspIface(e, "type"))),
                e.ifOnly(
                        orB(e, isRuntime(e, e.v(cvVar)), e.not(e.fld(e.v("tk"), "is_some"))),
                        retM(e, errCve(e, loc(e), what + " must be a constant type"))),
                e.ret(bim(e, aspIface(e, "type"), "type", e.fld(e.v("tk"), "value"), cveValue(e, e.v(cvVar)))));
    }

    private static SourceExpr rs(EcoMb e, String method, SourceExpr arg) {
        return e.callM(e.v("r"), method, arg);
    }

    private static SourceExpr rs0(EcoMb e, String method) {
        return e.callM(e.v("r"), method, e.tup());
    }

    private static SourceExpr rsLine(EcoMb e, double delta, SourceExpr... parts) {
        return rs(e, "line", e.rec(e.p("delta", e.intOf(delta)), e.p("text", e.cat(parts))));
    }

    private static SourceExpr rsOps(EcoMb e, SourceExpr... ops) {
        return rs(e, "ops", e.listNew(lt(e, "wval"), ops));
    }

    private static void vmethod(
            EcoMb e, TList<SourceExpr> x, String obj, String name, SourceExpr argType, SourceExpr body) {
        x.add(e.methodImpl(e.v(obj), name, argType, e.optionTypeNone(), e.stage(body)));
    }

    private static SourceExpr pendActive(EcoMb e, SourceExpr pendTP) {
        return e.eq(e.callM(pendTP, "active", e.tup()), num(e, 1));
    }

    private static SourceExpr pendPos(EcoMb e, SourceExpr pendTP) {
        return e.callM(pendTP, "pos", e.tup());
    }

    private static SourceExpr usesOf(EcoMb e, SourceExpr name) {
        return e.callM(e.callM(outer(e), "use_of", name), "n", e.tup());
    }

    private static SourceExpr regionOf(EcoMb e, SourceExpr depth) {
        return e.callM(outer(e), "region_of", depth);
    }

    private static void defRenderState(EcoMb e, TList<SourceExpr> x) {
        defObj(e, x, "wuse", "wasm local use count", e.rec(e.p("name", str(e)), e.p("n", intT(e))));
        defObj(
                e,
                x,
                "wpend",
                "wasm deferred local.set",
                e.rec(e.p("name", str(e)), e.p("pos", intT(e)), e.p("at", intT(e)), e.p("active", intT(e))));
        defObj(
                e,
                x,
                "wregion",
                "wasm structured region",
                e.rec(e.p("depth", intT(e)), e.p("start", intT(e)), e.p("valued", boolT(e)), e.p("closed", intT(e))));
        defObj(
                e,
                x,
                "wrs",
                "wasm render state",
                e.rec(
                        e.p("mod", wmodT(e)),
                        e.p("out", e.listType(str(e))),
                        e.p("used", e.listType(str(e))),
                        e.p("uses", e.listType(t(e, "wuse"))),
                        e.p("pn", e.listType(t(e, "wpend"))),
                        e.p("sets", e.listType(t(e, "wblock"))),
                        e.p("blocks", e.listType(t(e, "wblock"))),
                        e.p("regions", e.listType(t(e, "wregion"))),
                        e.p("n", intT(e)),
                        e.p("h", intT(e)),
                        e.p("depth", intT(e))));
        for (var vo : new String[] {"wuse", "wpend", "wregion", "wrs"}) {
            defIface(e, x, vo + "_m");
            markerType(e, x, vo, vo, vo + "_m", e.aspectDispatch(lv(e, vo + "_m_id")));
            x.add(e.implInterface(e.v(vo), t(e, vo + "_m")));
        }
        accessor(e, x, "wuse", "name", str(e));
        accessor(e, x, "wuse", "n", intT(e));
        vmethod(e, x, "wuse", "inc", e.tup(), e.incr(in(e, "n")));
        accessor(e, x, "wpend", "name", str(e));
        accessor(e, x, "wpend", "pos", intT(e));
        accessor(e, x, "wpend", "at", intT(e));
        accessor(e, x, "wpend", "active", intT(e));
        vmethod(e, x, "wpend", "set_pos", intT(e), e.mb.set(in(e, "pos"), e.v("arg")));
        vmethod(e, x, "wpend", "set_active", intT(e), e.mb.set(in(e, "active"), e.v("arg")));
        accessor(e, x, "wregion", "depth", intT(e));
        accessor(e, x, "wregion", "start", intT(e));
        accessor(e, x, "wregion", "valued", boolT(e));
        accessor(e, x, "wregion", "closed", intT(e));
        vmethod(e, x, "wregion", "set_start", intT(e), e.mb.set(in(e, "start"), e.v("arg")));
        vmethod(e, x, "wregion", "set_closed", e.tup(), e.mb.set(in(e, "closed"), e.intOf(1)));
        vmethod(
                e,
                x,
                "wrs",
                "add_use",
                str(e),
                push(e, in(e, "uses"), mk(e, "wuse", e.p("name", e.v("arg")), e.p("n", e.intOf(0)))));
        for (var find : new String[][] {{"use_of", "uses", "wuse", "name"}}) {
            method(
                    e,
                    x,
                    "wrs",
                    find[0],
                    str(e),
                    t(e, find[2]),
                    e.seq(
                            e.let("hit", e.listNew(lt(e, find[2]))),
                            e.foreach(
                                    in(e, find[1]),
                                    "q",
                                    e.ifOnly(
                                            e.eq(e.callM(e.v("q"), find[3], e.tup()), e.v("arg")),
                                            push(e, e.v("hit"), e.v("q")))),
                            e.foreach(e.v("hit"), "q", e.ret(e.v("q"))),
                            e.mb.panic(e.cat(e.s("wasm: no " + find[2] + " entry for "), e.v("arg")))));
        }
        method(
                e,
                x,
                "wrs",
                "region_of",
                intT(e),
                t(e, "wregion"),
                e.seq(
                        e.let("hit", e.listNew(lt(e, "wregion"))),
                        e.foreach(
                                in(e, "regions"),
                                "q",
                                e.ifOnly(
                                        andB(
                                                e,
                                                e.eq(e.callM(e.v("q"), "depth", e.tup()), e.v("arg")),
                                                e.eq(e.callM(e.v("q"), "closed", e.tup()), num(e, 0))),
                                        push(e, e.v("hit"), e.v("q")))),
                        e.foreach(e.v("hit"), "q", e.ret(e.v("q"))),
                        e.mb.panic(e.cat(e.s("wasm: no open region at depth "), toStr(e, e.v("arg"))))));
        vmethod(
                e,
                x,
                "wrs",
                "line",
                e.rec(e.p("delta", intT(e)), e.p("text", str(e))),
                e.seq(
                        push(e, in(e, "out"), a(e, "text")),
                        e.incr(in(e, "n")),
                        e.mb.set(in(e, "h"), e.mb.operatorAdd(in(e, "h"), a(e, "delta")))));
        vmethod(
                e,
                x,
                "wrs",
                "flush_one",
                t(e, "wpend"),
                e.seq(
                        e.let("fpos", pendPos(e, e.v("arg"))),
                        push(
                                e,
                                in(e, "sets"),
                                mk(
                                        e,
                                        "wblock",
                                        e.p("at", e.callM(e.v("arg"), "at", e.tup())),
                                        e.p("name", e.cat(e.s("local.set "), e.callM(e.v("arg"), "name", e.tup()))))),
                        push(e, in(e, "used"), e.callM(e.v("arg"), "name", e.tup())),
                        e.callM(e.v("arg"), "set_active", e.intOf(0)),
                        e.foreach(
                                in(e, "pn"),
                                "q",
                                e.ifOnly(
                                        andB(
                                                e,
                                                pendActive(e, e.v("q")),
                                                e.mb.operatorGt(pendPos(e, e.v("q")), e.v("fpos"))),
                                        e.callM(
                                                e.v("q"),
                                                "set_pos",
                                                e.mb.operatorSub(pendPos(e, e.v("q")), num(e, 1))))),
                        e.mb.set(in(e, "h"), e.mb.operatorSub(in(e, "h"), num(e, 1)))));
        vmethod(
                e,
                x,
                "wrs",
                "flush_all",
                e.tup(),
                e.foreach(
                        in(e, "pn"),
                        "q0",
                        e.ifOnly(pendActive(e, e.v("q0")), e.callM(outer(e), "flush_one", e.v("q0")))));
        vmethod(
                e,
                x,
                "wrs",
                "control",
                e.rec(e.p("delta", intT(e)), e.p("text", str(e))),
                e.seq(e.callM(outer(e), "flush_all", e.tup()), e.callM(outer(e), "line", e.v("arg"))));
        vmethod(
                e,
                x,
                "wrs",
                "flush_named",
                str(e),
                e.foreach(
                        in(e, "pn"),
                        "q",
                        e.ifOnly(
                                andB(e, pendActive(e, e.v("q")), e.eq(e.callM(e.v("q"), "name", e.tup()), e.v("arg"))),
                                e.callM(outer(e), "flush_one", e.v("q")))));
        vmethod(
                e,
                x,
                "wrs",
                "set_local",
                str(e),
                e.seq(
                        e.let("nuses", usesOf(e, e.v("arg"))),
                        e.callM(outer(e), "flush_named", e.v("arg")),
                        e.ifOnly(
                                e.eq(e.v("nuses"), num(e, 0)),
                                e.callM(outer(e), "line", e.rec(e.p("delta", e.intOf(-1)), e.p("text", e.s("drop"))))),
                        e.ifOnly(
                                e.eq(e.v("nuses"), num(e, 1)),
                                push(
                                        e,
                                        in(e, "pn"),
                                        mk(
                                                e,
                                                "wpend",
                                                e.p("name", e.v("arg")),
                                                e.p("pos", e.mb.operatorSub(in(e, "h"), num(e, 1))),
                                                e.p("at", in(e, "n")),
                                                e.p("active", e.intOf(1))))),
                        e.ifOnly(
                                e.mb.operatorGt(e.v("nuses"), num(e, 1)),
                                e.seq(
                                        e.callM(
                                                outer(e),
                                                "line",
                                                e.rec(
                                                        e.p("delta", e.intOf(-1)),
                                                        e.p("text", e.cat(e.s("local.set "), e.v("arg"))))),
                                        push(e, in(e, "used"), e.v("arg"))))));
        vmethod(
                e,
                x,
                "wrs",
                "get_local",
                str(e),
                e.seq(
                        e.callM(outer(e), "flush_named", e.v("arg")),
                        e.callM(
                                outer(e),
                                "line",
                                e.rec(e.p("delta", e.intOf(1)), e.p("text", e.cat(e.s("local.get "), e.v("arg"))))),
                        push(e, in(e, "used"), e.v("arg"))));
        vmethod(
                e,
                x,
                "wrs",
                "ops",
                e.listType(t(e, "wval")),
                e.seq(
                        e.let("onames", e.listNew(str(e))),
                        countLet(e, "no"),
                        e.foreach(
                                e.v("arg"),
                                "o",
                                e.seq(
                                        push(
                                                e,
                                                e.v("onames"),
                                                e.matchOf(
                                                        e.callM(e.v("o"), "source", e.tup()),
                                                        "s",
                                                        Opt.some(e.dstr("")),
                                                        e.p("local", e.v("s")))),
                                        e.incr(e.fld(e.v("no"), "v")))),
                        e.let("act", e.listNew(lt(e, "wpend"))),
                        countLet(e, "na"),
                        e.foreach(
                                in(e, "pn"),
                                "q",
                                e.ifOnly(
                                        pendActive(e, e.v("q")),
                                        e.seq(push(e, e.v("act"), e.v("q")), e.incr(e.fld(e.v("na"), "v"))))),
                        e.let("jj", e.rec(e.p("v", e.intOf(0)))),
                        e.let(
                                "j",
                                e.rec(e.p(
                                        "v",
                                        e.ifElse(
                                                e.mb.operatorLt(e.fld(e.v("no"), "v"), e.fld(e.v("na"), "v")),
                                                e.fld(e.v("no"), "v"),
                                                e.fld(e.v("na"), "v"))))),
                        e.foreach(
                                e.v("arg"),
                                "_",
                                e.ifOnly(
                                        andB(
                                                e,
                                                e.eq(e.fld(e.v("jj"), "v"), num(e, 0)),
                                                e.mb.operatorGt(e.fld(e.v("j"), "v"), num(e, 0))),
                                        e.seq(
                                                e.let(
                                                        "base",
                                                        e.mb.operatorSub(e.fld(e.v("na"), "v"), e.fld(e.v("j"), "v"))),
                                                e.let("ok", e.rec(e.p("v", e.intOf(1)))),
                                                e.let("ai", e.rec(e.p("v", e.intOf(0)))),
                                                e.foreach(
                                                        e.v("act"),
                                                        "q",
                                                        e.seq(
                                                                e.ifOnly(
                                                                        e.mb.operatorGe(
                                                                                e.fld(e.v("ai"), "v"), e.v("base")),
                                                                        e.seq(
                                                                                e.let(
                                                                                        "oi",
                                                                                        e.mb.operatorSub(
                                                                                                e.fld(e.v("ai"), "v"),
                                                                                                e.v("base"))),
                                                                                e.ifOnly(
                                                                                        andB(
                                                                                                e,
                                                                                                e.eq(
                                                                                                        e.v("oi"),
                                                                                                        num(e, 0)),
                                                                                                e.not(
                                                                                                        e.eq(
                                                                                                                in(
                                                                                                                        e,
                                                                                                                        "h"),
                                                                                                                e.mb
                                                                                                                        .operatorAdd(
                                                                                                                                pendPos(
                                                                                                                                        e,
                                                                                                                                        e
                                                                                                                                                .v(
                                                                                                                                                        "q")),
                                                                                                                                e
                                                                                                                                        .fld(
                                                                                                                                                e
                                                                                                                                                        .v(
                                                                                                                                                                "j"),
                                                                                                                                                "v"))))),
                                                                                        e.mb.set(
                                                                                                e.fld(e.v("ok"), "v"),
                                                                                                num(e, 0))),
                                                                                e.let("k", e.rec(e.p("v", e.intOf(0)))),
                                                                                e.foreach(
                                                                                        e.v("onames"),
                                                                                        "on",
                                                                                        e.seq(
                                                                                                e.ifOnly(
                                                                                                        andB(
                                                                                                                e,
                                                                                                                e.eq(
                                                                                                                        e
                                                                                                                                .fld(
                                                                                                                                        e
                                                                                                                                                .v(
                                                                                                                                                        "k"),
                                                                                                                                        "v"),
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "oi")),
                                                                                                                e.not(
                                                                                                                        e
                                                                                                                                .eq(
                                                                                                                                        e
                                                                                                                                                .v(
                                                                                                                                                        "on"),
                                                                                                                                        e
                                                                                                                                                .callM(
                                                                                                                                                        e
                                                                                                                                                                .v(
                                                                                                                                                                        "q"),
                                                                                                                                                        "name",
                                                                                                                                                        e
                                                                                                                                                                .tup())))),
                                                                                                        e.mb.set(
                                                                                                                e.fld(
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "ok"),
                                                                                                                        "v"),
                                                                                                                num(
                                                                                                                        e,
                                                                                                                        0))),
                                                                                                e.incr(
                                                                                                        e.fld(
                                                                                                                e.v(
                                                                                                                        "k"),
                                                                                                                "v")))))),
                                                                e.incr(e.fld(e.v("ai"), "v")))),
                                                e.ifOnly(
                                                        e.eq(e.fld(e.v("ok"), "v"), num(e, 1)),
                                                        e.mb.set(e.fld(e.v("jj"), "v"), e.fld(e.v("j"), "v"))),
                                                e.mb.set(
                                                        e.fld(e.v("j"), "v"),
                                                        e.mb.operatorSub(e.fld(e.v("j"), "v"), num(e, 1)))))),
                        e.let("base", e.mb.operatorSub(e.fld(e.v("na"), "v"), e.fld(e.v("jj"), "v"))),
                        e.let("ai", e.rec(e.p("v", e.intOf(0)))),
                        e.foreach(
                                e.v("act"),
                                "q",
                                e.seq(
                                        e.ifOnly(
                                                e.mb.operatorGe(e.fld(e.v("ai"), "v"), e.v("base")),
                                                e.callM(e.v("q"), "set_active", e.intOf(0))),
                                        e.incr(e.fld(e.v("ai"), "v")))),
                        e.let("k", e.rec(e.p("v", e.intOf(0)))),
                        e.foreach(
                                e.v("arg"),
                                "o",
                                e.seq(
                                        e.ifOnly(
                                                e.mb.operatorGe(e.fld(e.v("k"), "v"), e.fld(e.v("jj"), "v")),
                                                e.matchOf(
                                                        e.callM(e.v("o"), "source", e.tup()),
                                                        "s",
                                                        Opt.none(),
                                                        e.p("local", e.callM(outer(e), "get_local", e.v("s"))),
                                                        e.p(
                                                                "konst",
                                                                e.callM(
                                                                        outer(e),
                                                                        "line",
                                                                        e.rec(
                                                                                e.p("delta", e.intOf(1)),
                                                                                e.p(
                                                                                        "text",
                                                                                        e.cat(
                                                                                                e.callM(
                                                                                                        in(e, "mod"),
                                                                                                        "ir",
                                                                                                        wvTy(
                                                                                                                e,
                                                                                                                e.v(
                                                                                                                        "o"))),
                                                                                                e.s(".const "),
                                                                                                e.v("s")))))),
                                                        e.p(
                                                                "slot",
                                                                e.mb.panic(e.s("wasm: slot pushed without a load"))),
                                                        e.p(
                                                                "func",
                                                                e.mb.panic(e.s("wasm: function pushed as a value"))))),
                                        e.incr(e.fld(e.v("k"), "v"))))));
        vmethod(
                e,
                x,
                "wrs",
                "region_start",
                e.tup(),
                e.seq(e.callM(regionOf(e, in(e, "depth")), "set_start", in(e, "n")), e.mb.set(in(e, "h"), e.intOf(0))));
        vmethod(
                e,
                x,
                "wrs",
                "label",
                str(e),
                e.seq(
                        e.callM(outer(e), "flush_all", e.tup()),
                        push(
                                e,
                                in(e, "blocks"),
                                mk(
                                        e,
                                        "wblock",
                                        e.p("at", e.callM(regionOf(e, in(e, "depth")), "start", e.tup())),
                                        e.p("name", e.cat(e.s("block "), e.v("arg"))))),
                        e.callM(outer(e), "line", e.rec(e.p("delta", e.intOf(0)), e.p("text", e.s("end")))),
                        e.mb.set(in(e, "h"), e.intOf(0))));
        vmethod(
                e,
                x,
                "wrs",
                "open_if",
                e.rec(e.p("text", str(e)), e.p("valued", boolT(e))),
                e.seq(
                        e.callM(outer(e), "control", e.rec(e.p("delta", e.intOf(-1)), e.p("text", a(e, "text")))),
                        e.incr(in(e, "depth")),
                        push(
                                e,
                                in(e, "regions"),
                                mk(
                                        e,
                                        "wregion",
                                        e.p("depth", in(e, "depth")),
                                        e.p("start", in(e, "n")),
                                        e.p("valued", a(e, "valued")),
                                        e.p("closed", e.intOf(0)))),
                        e.mb.set(in(e, "h"), e.intOf(0))));
        vmethod(
                e,
                x,
                "wrs",
                "else_",
                e.tup(),
                e.seq(
                        e.callM(outer(e), "control", e.rec(e.p("delta", e.intOf(0)), e.p("text", e.s("else")))),
                        e.callM(outer(e), "region_start", e.tup())));
        vmethod(
                e,
                x,
                "wrs",
                "end_if",
                e.tup(),
                e.seq(
                        e.callM(outer(e), "control", e.rec(e.p("delta", e.intOf(0)), e.p("text", e.s("end")))),
                        e.let("reg", regionOf(e, in(e, "depth"))),
                        e.callM(e.v("reg"), "set_closed", e.tup()),
                        e.mb.set(in(e, "h"), e.ifElse(e.callM(e.v("reg"), "valued", e.tup()), e.intOf(1), e.intOf(0))),
                        e.mb.set(in(e, "depth"), e.mb.operatorSub(in(e, "depth"), num(e, 1)))));
        vmethod(
                e,
                x,
                "wrs",
                "call",
                e.rec(e.p("fn", str(e)), e.p("nargs", intT(e)), e.p("res", e.call(e.mb.typesOption(), str(e)))),
                e.seq(
                        e.callM(
                                outer(e),
                                "line",
                                e.rec(
                                        e.p("delta", e.mb.operatorSub(e.intOf(0), a(e, "nargs"))),
                                        e.p("text", e.cat(e.s("call "), a(e, "fn"))))),
                        e.ifOnly(
                                e.fld(a(e, "res"), "is_some"),
                                e.seq(
                                        e.incr(in(e, "h")),
                                        e.callM(outer(e), "set_local", e.fld(a(e, "res"), "value"))))));
        method(
                e,
                x,
                "wrs",
                "finish",
                e.tup(),
                e.rec(e.p("lines", e.listType(str(e))), e.p("used", e.listType(str(e)))),
                e.seq(
                        e.callM(outer(e), "flush_all", e.tup()),
                        e.ifOnly(
                                e.not(e.eq(in(e, "depth"), num(e, 0))),
                                e.mb.panic(e.s("wasm: unbalanced structured instructions"))),
                        e.let("lines", e.listNew(str(e))),
                        e.let("k", e.rec(e.p("i", e.intOf(0)))),
                        e.foreach(
                                in(e, "out"),
                                "line",
                                e.seq(
                                        e.foreach(
                                                in(e, "sets"),
                                                "sx",
                                                e.ifOnly(
                                                        e.eq(e.callM(e.v("sx"), "at", e.tup()), e.fld(e.v("k"), "i")),
                                                        push(e, e.v("lines"), e.callM(e.v("sx"), "name", e.tup())))),
                                        e.let("opens", e.listNew(str(e))),
                                        e.foreach(
                                                in(e, "blocks"),
                                                "b",
                                                e.ifOnly(
                                                        e.eq(e.callM(e.v("b"), "at", e.tup()), e.fld(e.v("k"), "i")),
                                                        push(e, e.v("opens"), e.callM(e.v("b"), "name", e.tup())))),
                                        e.let("rev", e.listNew(str(e))),
                                        e.let("on", e.rec(e.p("v", e.intOf(0)))),
                                        e.foreach(e.v("opens"), "_", e.incr(e.fld(e.v("on"), "v"))),
                                        e.foreach(
                                                e.v("opens"),
                                                "_",
                                                e.seq(
                                                        e.let(
                                                                "want",
                                                                e.mb.operatorSub(e.fld(e.v("on"), "v"), num(e, 1))),
                                                        e.let("j", e.rec(e.p("v", e.intOf(0)))),
                                                        e.foreach(
                                                                e.v("opens"),
                                                                "o",
                                                                e.seq(
                                                                        e.ifOnly(
                                                                                e.eq(e.fld(e.v("j"), "v"), e.v("want")),
                                                                                push(e, e.v("rev"), e.v("o"))),
                                                                        e.incr(e.fld(e.v("j"), "v")))),
                                                        e.mb.set(e.fld(e.v("on"), "v"), e.v("want")))),
                                        e.foreach(e.v("rev"), "o", push(e, e.v("lines"), e.v("o"))),
                                        push(e, e.v("lines"), e.v("line")),
                                        e.incr(e.fld(e.v("k"), "i")))),
                        e.foreach(
                                in(e, "sets"),
                                "sx",
                                e.ifOnly(
                                        e.eq(e.callM(e.v("sx"), "at", e.tup()), e.fld(e.v("k"), "i")),
                                        push(e, e.v("lines"), e.callM(e.v("sx"), "name", e.tup())))),
                        e.ret(e.rec(e.p("lines", e.v("lines")), e.p("used", in(e, "used"))))));
    }

    private static SourceExpr printableBytes(EcoMb e) {
        var entries = new TList<EcoMb.P>();
        for (int b = 0; b < 256; b++) {
            var text =
                    b >= 32 && b < 127 && b != '"' && b != '\\' ? String.valueOf((char) b) : String.format("\\%02x", b);
            entries.add(e.p(Integer.toString(b), e.s(text)));
        }
        return e.mapNew(str(e), str(e), entries.toArray(EcoMb.P[]::new));
    }

    private static SourceExpr countUse(EcoMb e, SourceExpr wvalTP) {
        return e.matchOf(
                e.callM(wvalTP, "source", e.tup()), "s", Opt.some(e.seq()), e.p("local", countName(e, e.v("s"))));
    }

    private static SourceExpr countName(EcoMb e, SourceExpr name) {
        return e.callM(e.callM(e.v("r"), "use_of", name), "inc", e.tup());
    }

    public static void bindTypes(EcoMb e, TList<SourceExpr> out) {
        for (var t : new String[] {"i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void"}) {
            out.add(e.let("t_" + t, e.fld(e.v("wasm"), t)));
        }
        for (var t : new String[] {"void", "u8", "u32", "u64"}) {
            out.add(e.let("t_p" + (t.equals("void") ? "v" : t), e.call(e.fld(e.v("wasm"), "ptr"), e.v("t_" + t))));
        }
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var x = new TList<SourceExpr>();
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        {
            defIface(
                    e,
                    x,
                    "wint",
                    e.p("bits", intT(e)),
                    e.p("signed", boolT(e)),
                    e.p("min", e.typesDouble()),
                    e.p("max", e.typesDouble()));
            defIface(e, x, "wfloat", e.p("bits", intT(e)));
            defIface(e, x, "wptr", e.p("pointee", ty(e)));
            defIface(e, x, "wvoid");
            defIface(e, x, "wslot", e.p("base", ty(e)), e.p("name", str(e)));
            defIface(e, x, "wunit_m");
            defIface(e, x, "wgtype_m", e.p("base", ty(e)));
            defIface(e, x, "wgfn_m", e.p("kind", intT(e)));
            defIface(e, x, "wfn", e.p("params", e.listType(ty(e))), e.p("ret", ty(e)));

            x.add(e.let("wsrc_u", e.callM0(e.v("d"), "union")));
            for (var tag : new String[] {"local", "slot", "konst", "func"}) {
                x.add(e.let(
                        "ws_" + tag,
                        e.callM(e.v("wsrc_u"), "variant", e.rec(e.p("tag", e.s(tag)), e.p("type_ref", str(e))))));
            }
            defObj(e, x, "wval", "wasm value", e.rec(e.p("ty", ty(e)), e.p("source", t(e, "wsrc_u"))));
            defObj(
                    e,
                    x,
                    "wdata",
                    "wasm data segment",
                    e.rec(e.p("name", str(e)), e.p("addr", intT(e)), e.p("bytes", e.listType(intT(e)))));
            defObj(
                    e,
                    x,
                    "wlocal",
                    "wasm function local",
                    e.rec(e.p("name", str(e)), e.p("ty", ty(e)), e.p("param", boolT(e))));
            defObj(e, x, "wlabel", "wasm label", e.rec(e.p("key", e.typesJumpKey()), e.p("name", str(e))));
            defObj(e, x, "wblock", "wasm block opening", e.rec(e.p("at", intT(e)), e.p("name", str(e))));

            x.add(e.let("winst_u", e.callM0(e.v("d"), "union")));
            for (var v : new String[][] {
                {"label", "name"},
                {"br", "name"},
            }) {
                x.add(e.let(
                        "wi_" + v[0],
                        e.callM(
                                e.v("winst_u"),
                                "variant",
                                e.rec(e.p("tag", e.s(v[0])), e.p("type_ref", e.rec(e.p(v[1], str(e))))))));
            }
            x.add(e.let(
                    "wi_if",
                    e.callM(
                            e.v("winst_u"),
                            "variant",
                            e.rec(
                                    e.p("tag", e.s("if")),
                                    e.p(
                                            "type_ref",
                                            e.rec(
                                                    e.p("cond", wvalT(e)),
                                                    e.p("ty", e.call(e.mb.typesOption(), ty(e)))))))));
            x.add(e.let(
                    "wi_push",
                    e.callM(
                            e.v("winst_u"),
                            "variant",
                            e.rec(e.p("tag", e.s("push")), e.p("type_ref", e.rec(e.p("val", wvalT(e))))))));
            x.add(e.let(
                    "wi_setl",
                    e.callM(
                            e.v("winst_u"),
                            "variant",
                            e.rec(e.p("tag", e.s("setl")), e.p("type_ref", e.rec(e.p("name", str(e))))))));
            for (var tag : new String[] {"else", "end"}) {
                x.add(e.let(
                        "wi_" + tag,
                        e.callM(e.v("winst_u"), "variant", e.rec(e.p("tag", e.s(tag)), e.p("type_ref", e.tup())))));
            }
            for (var tag : new String[] {"binop", "cmp"}) {
                x.add(e.let(
                        "wi_" + tag,
                        e.callM(
                                e.v("winst_u"),
                                "variant",
                                e.rec(
                                        e.p("tag", e.s(tag)),
                                        e.p(
                                                "type_ref",
                                                e.rec(
                                                        e.p("res", str(e)),
                                                        e.p("op", str(e)),
                                                        e.p("a", wvalT(e)),
                                                        e.p("b", wvalT(e))))))));
            }
            for (var tag : new String[] {"unop", "conv"}) {
                x.add(e.let(
                        "wi_" + tag,
                        e.callM(
                                e.v("winst_u"),
                                "variant",
                                e.rec(
                                        e.p("tag", e.s(tag)),
                                        e.p(
                                                "type_ref",
                                                e.rec(e.p("res", str(e)), e.p("op", str(e)), e.p("a", wvalT(e))))))));
            }
            x.add(e.let(
                    "wi_load",
                    e.callM(
                            e.v("winst_u"),
                            "variant",
                            e.rec(
                                    e.p("tag", e.s("load")),
                                    e.p(
                                            "type_ref",
                                            e.rec(e.p("res", str(e)), e.p("ty", ty(e)), e.p("name", str(e))))))));
            x.add(e.let(
                    "wi_store",
                    e.callM(
                            e.v("winst_u"),
                            "variant",
                            e.rec(
                                    e.p("tag", e.s("store")),
                                    e.p("type_ref", e.rec(e.p("val", wvalT(e)), e.p("name", str(e))))))));
            x.add(e.let(
                    "wi_call",
                    e.callM(
                            e.v("winst_u"),
                            "variant",
                            e.rec(
                                    e.p("tag", e.s("call")),
                                    e.p(
                                            "type_ref",
                                            e.rec(
                                                    e.p("res", e.call(e.mb.typesOption(), str(e))),
                                                    e.p("fn", wvalT(e)),
                                                    e.p("args", e.listType(wvalT(e)))))))));
            x.add(e.let(
                    "wi_ret",
                    e.callM(
                            e.v("winst_u"),
                            "variant",
                            e.rec(
                                    e.p("tag", e.s("ret")),
                                    e.p("type_ref", e.rec(e.p("val", e.call(e.mb.typesOption(), wvalT(e)))))))));

            defObj(e, x, "winst", "wasm instruction", e.rec(e.p("k", t(e, "winst_u"))));
            defIface(e, x, "winst_m");
            defIface(e, x, "wmod_m");
            defIface(e, x, "wlabel_m");
            defIface(e, x, "wlocal_m");
            defIface(e, x, "wdata_m");
            defIface(e, x, "wblock_m");
            defObj(e, x, "wcode", "wasm code", e.rec(e.p("insts", instsT(e))));
            x.add(e.implInterface(e.v("wcode"), e.mb.builtin("eval", "code_dyn")));
            method(e, x, "wcode", "insts", e.tup(), instsT(e), in(e, "insts"));

            defObj(
                    e,
                    x,
                    "wmod",
                    "wasm module",
                    e.rec(
                            e.p("n", intT(e)),
                            e.p("xx", e.call(e.mb.typesOption(), e.typesExprContext())),
                            e.p("imports", e.listType(str(e))),
                            e.p("data", e.listType(t(e, "wdata"))),
                            e.p("data_end", intT(e)),
                            e.p("funcs", e.listType(str(e))),
                            e.p("exports", e.listType(str(e))),
                            e.p("labels", e.listType(t(e, "wlabel"))),
                            e.p("locals", e.listType(t(e, "wlocal")))));
            defObj(e, x, "wtarget", "wasm target", e.rec(e.p("mod", wmodT(e))));
            defObj(e, x, "wirctx", "wasm type rendering context", e.rec(e.p("xx", xxT(e))));
            x.add(e.let("wtype", e.callM0(e.v("d"), "interface")));
            x.add(e.interfaceImpl(e.v("wtype"), true));
            x.add(e.let("wtype_id", e.callM0(e.v("wtype"), "aspect_id")));
            x.add(e.callM(
                    e.v("wtype"),
                    "func",
                    e.rec(e.p("name", e.s("ir")), e.p("arg", t(e, "wirctx")), e.p("ret", e.optionTypeSome(str(e))))));
            x.add(e.callM(
                    e.v("wtype"),
                    "func",
                    e.rec(e.p("name", e.s("ssa")), e.p("arg", e.tup()), e.p("ret", e.optionTypeSome(ty(e))))));
            defObj(e, x, "wbinding", "wasm binding", e.rec(e.p("ty", ty(e)), e.p("slot", str(e))));
            defObj(e, x, "wbinding_none", "wasm empty binding", e.tup());
            defObj(e, x, "wunit", "wasm unit", e.tup());
            defObj(
                    e,
                    x,
                    "wt_int",
                    "wasm int type",
                    e.rec(
                            e.p("bits", intT(e)),
                            e.p("signed", boolT(e)),
                            e.p("min", e.typesDouble()),
                            e.p("max", e.typesDouble())));
            defObj(e, x, "wt_float", "wasm float type", e.rec(e.p("bits", intT(e))));
            defObj(e, x, "wt_ptr", "wasm pointer type", e.rec(e.p("pointee", ty(e))));
            defObj(e, x, "wt_fn", "wasm function type", e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e))));
            defObj(e, x, "wt_void", "wasm void type", e.tup());
            defObj(e, x, "wt_slot", "wasm slot type", e.rec(e.p("base", ty(e)), e.p("name", str(e))));
            defObj(e, x, "wgfn_t", "wasm global definition function", e.rec(e.p("kind", intT(e))));
            defObj(e, x, "wt_global", "wasm global type", e.rec(e.p("base", ty(e))));
        }
        {
            fnDef(e, x, "h_ir", e.rec(e.p("xx", xxT(e)), e.p("ty", ty(e))), str(e));
            fnImpl(
                    e,
                    x,
                    "h_ir",
                    body(
                            e,
                            e.let("lk", lookup(e, xx(e), a(e, "ty"), lt(e, "wtype"))),
                            e.ifOnly(
                                    e.not(e.fld(e.v("lk"), "is_some")),
                                    e.mb.panic(e.cat(
                                            e.s("wasm: not a wasm type: "),
                                            bim0(e, e.mb.evalType(), "desc", a(e, "ty"))))),
                            e.ret(bim(
                                    e,
                                    lt(e, "wtype"),
                                    "ir",
                                    e.fld(e.v("lk"), "value"),
                                    mk(e, "wirctx", e.p("xx", xx(e)))))));
            fnDef(e, x, "h_insts", codeT(e), instsT(e));
            fnDef(e, x, "h_code", instsT(e), codeT(e));
            x.add(e.let("h_append_b", e.defFunc(e.v("d"), "wasm.h_append")));
            x.add(e.let("h_append", e.fld(e.v("h_append_b"), "func")));
            e.fnSigs.put("h_append", new SourceExpr[] {
                e.rec(e.p("out", instsT(e)), e.p("code", codeT(e))), e.optionTypeNone(), e.optionTypeNone()
            });
            fnDef(e, x, "h_is_wasm", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), e.mb.typesBool());
            fnDef(
                    e,
                    x,
                    "h_ssa",
                    e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("mod", wmodT(e)), e.p("cv", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_ssa", errsT(e));
            fnDef(e, x, "h_mat", e.rec(e.p("mod", wmodT(e)), e.p("cv", cveT(e))), cveT(e));
            fnDef(
                    e,
                    x,
                    "h_operand",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", wmodT(e)),
                            e.p("ty", ty(e)),
                            e.p("cv", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_operand", errsT(e));
            fnDef(e, x, "h_is_unit", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), e.mb.typesBool());
            fnDef(
                    e,
                    x,
                    "h_set",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", wmodT(e)),
                            e.p("ty", ty(e)),
                            e.p("self", cveT(e)),
                            e.p("value", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_set", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_binop",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", wmodT(e)),
                            e.p("ty", ty(e)),
                            e.p("op", str(e)),
                            e.p("a", cveT(e)),
                            e.p("b", cveT(e)),
                            e.p("res", ty(e))),
                    cveT(e));
            fnThrows(e, "h_binop", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_cmp",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", wmodT(e)),
                            e.p("ty", ty(e)),
                            e.p("op", str(e)),
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
                            e.p("mod", wmodT(e)),
                            e.p("ty", ty(e)),
                            e.p("op", str(e)),
                            e.p("a", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_unop", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_store_new",
                    e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("mod", wmodT(e)), e.p("cv", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_store_new", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_conv",
                    e.rec(e.p("mod", wmodT(e)), e.p("op", str(e)), e.p("a", cveT(e)), e.p("to", ty(e))),
                    cveT(e));
            fnDef(e, x, "h_retag", e.rec(e.p("mod", wmodT(e)), e.p("cv", cveT(e)), e.p("ty", ty(e))), cveT(e));
            fnDef(
                    e,
                    x,
                    "h_narrow",
                    e.rec(e.p("xx", xxT(e)), e.p("mod", wmodT(e)), e.p("cv", cveT(e)), e.p("to", ty(e))),
                    cveT(e));
            fnDef(e, x, "h_literal", e.rec(e.p("loc", locT(e)), e.p("ty", ty(e)), e.p("text", str(e))), cveT(e));
            fnDef(e, x, "h_gref", cveT(e), str(e));
            fnDef(e, x, "h_gval", e.rec(e.p("mod", wmodT(e)), e.p("cv", cveT(e)), e.p("base", ty(e))), cveT(e));
            fnImpl(
                    e,
                    x,
                    "h_gref",
                    body(
                            e,
                            e.ret(e.call(
                                    e.mb.mustReinterpret(),
                                    e.tup(
                                            e.fld(
                                                    e.call(
                                                            e.mb.mustReinterpret(),
                                                            e.tup(cveValue(e, e.v("arg")), vocT(e))),
                                                    "data"),
                                            str(e))))));
            fnImpl(
                    e,
                    x,
                    "h_gval",
                    body(
                            e,
                            e.let("r", hf(e, "h_gref", a(e, "cv"))),
                            e.ret(cvOf(
                                    e,
                                    cveCode(e, a(e, "cv")),
                                    e.ifElse(
                                            has(e, e.callM(a(e, "mod"), "xx", e.tup()), a(e, "base"), "wfn"),
                                            wvalFunc(e, a(e, "base"), e.v("r")),
                                            wvalKonst(
                                                    e,
                                                    a(e, "base"),
                                                    toStr(e, e.callM(a(e, "mod"), "addr", e.v("r")))))))));
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
                                                    e.callM(e.v("c"), "cast", lt(e, "wcode")),
                                                    "r",
                                                    Opt.none(),
                                                    e.p("ok", e.callM(e.v("r"), "insts", e.tup())),
                                                    e.p(
                                                            "errs",
                                                            e.mb.panic(
                                                                    e.s("wasm: code belongs to another target")))))))));
            fnImpl(
                    e,
                    x,
                    "h_code",
                    body(e, e.ret(e.mb.as(mkArg(e, "wcode", e.rec(e.p("insts", e.v("arg")))), codeT(e)))));
            fnImpl(
                    e,
                    x,
                    "h_append",
                    body(e, e.foreach(hf(e, "h_insts", a(e, "code")), "i", push(e, a(e, "out"), e.v("i")))));
            fnImpl(
                    e,
                    x,
                    "h_is_wasm",
                    body(
                            e,
                            e.ret(e.ifElse(
                                    cveHas(e, a(e, "cv")),
                                    has(e, xx(e), cveType(e, a(e, "cv")), "wtype"),
                                    e.b(false)))));
            fnImpl(
                    e,
                    x,
                    "h_ssa",
                    body(
                            e,
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "expression has no value")),
                            e.ifOnly(
                                    has(e, xx(e), cveType(e, a(e, "cv")), "wgtype_m"),
                                    retM(
                                            e,
                                            hf(
                                                    e,
                                                    "h_ssa",
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("mod", a(e, "mod")),
                                                    e.p(
                                                            "cv",
                                                            unwrapCveT(
                                                                    e,
                                                                    castValue(
                                                                            e,
                                                                            xx(e),
                                                                            loc(e),
                                                                            acc(
                                                                                    e,
                                                                                    xx(e),
                                                                                    cveType(e, a(e, "cv")),
                                                                                    "wgtype_m",
                                                                                    "base"),
                                                                            a(e, "cv"))))))),
                            e.let("lk", lookup(e, xx(e), cveType(e, a(e, "cv")), lt(e, "wtype"))),
                            e.ifOnly(e.not(e.fld(e.v("lk"), "is_some")), throwErr(e, loc(e), "not a wasm value")),
                            e.let("st", im0(e, "wtype", "ssa", e.fld(e.v("lk"), "value"))),
                            e.let("c", unwrapCveT(e, castValue(e, xx(e), loc(e), e.v("st"), a(e, "cv")))),
                            e.ret(hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", e.v("c"))))));
            fnImpl(
                    e,
                    x,
                    "h_is_unit",
                    body(
                            e,
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), retM(e, dfalse(e))),
                            e.ifOnly(has(e, xx(e), cveType(e, a(e, "cv")), "wunit_m"), retM(e, e.not(dfalse(e)))),
                            e.let("tk", lookup(e, xx(e), cveType(e, a(e, "cv")), aspIface(e, "tuple"))),
                            e.ifOnly(e.not(e.fld(e.v("tk"), "is_some")), retM(e, dfalse(e))),
                            e.let("n", e.rec(e.p("v", e.intOf(0)))),
                            e.foreach(
                                    bim0(e, aspIface(e, "tuple"), "element_types", e.fld(e.v("tk"), "value")),
                                    "_",
                                    e.incr(e.fld(e.v("n"), "v"))),
                            e.ret(e.eq(e.fld(e.v("n"), "v"), num(e, 0)))));
            fnImpl(
                    e,
                    x,
                    "h_mat",
                    body(
                            e,
                            e.let("lv", wvalOf(e, a(e, "cv"))),
                            e.ifOnly(e.not(wvIsMem(e, e.v("lv"))), retM(e, a(e, "cv"))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, a(e, "cv"))),
                            e.let("r", fresh(e, a(e, "mod"), wvTy(e, e.v("lv")))),
                            push(e, e.v("out"), loadInst(e, e.v("r"), wvTy(e, e.v("lv")), wvMem(e, e.v("lv")))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, wvTy(e, e.v("lv")), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_operand",
                    body(
                            e,
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "operand has no value")),
                            e.let("c", unwrapCveT(e, castValue(e, xx(e), loc(e), a(e, "ty"), a(e, "cv")))),
                            e.ret(hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", e.v("c"))))));
            fnImpl(
                    e,
                    x,
                    "h_set",
                    body(
                            e,
                            e.let("sl", wvalOf(e, a(e, "self"))),
                            e.ifOnly(e.not(wvIsMem(e, e.v("sl"))), throwErr(e, loc(e), "value is not assignable")),
                            e.let("v", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "value"))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, a(e, "self"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                            push(e, e.v("out"), storeInst(e, wvalOf(e, e.v("v")), wvMem(e, e.v("sl")))),
                            e.ret(cvNo(e, codeOf(e, e.v("out"))))));
            fnImpl(
                    e,
                    x,
                    "h_binop",
                    body(
                            e,
                            e.let("out", instsNew(e)),
                            e.let("a2", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "a"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("a2"))),
                            e.let("b2", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "b"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("b2"))),
                            e.let("r", fresh(e, a(e, "mod"), a(e, "res"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "binop",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", wvalOf(e, e.v("a2"))),
                                            e.p("b", wvalOf(e, e.v("b2"))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, a(e, "res"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_cmp",
                    body(
                            e,
                            e.let("out", instsNew(e)),
                            e.let("a2", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "a"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("a2"))),
                            e.let("b2", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "b"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("b2"))),
                            e.let("i1", intTy(e, "i1")),
                            e.let("r", fresh(e, a(e, "mod"), e.v("i1"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "cmp",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", wvalOf(e, e.v("a2"))),
                                            e.p("b", wvalOf(e, e.v("b2"))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, e.v("i1"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_unop",
                    body(
                            e,
                            e.let("out", instsNew(e)),
                            e.let("a2", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "a"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("a2"))),
                            e.let("r", fresh(e, a(e, "mod"), a(e, "ty"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "unop",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", wvalOf(e, e.v("a2"))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, a(e, "ty"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_store_new",
                    body(
                            e,
                            e.let("v", ssa(e, xx(e), loc(e), a(e, "mod"), a(e, "cv"))),
                            e.let("ty", cveType(e, e.v("v"))),
                            e.let("s", e.callM(a(e, "mod"), "slot", e.v("ty"))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                            push(e, e.v("out"), storeInst(e, wvalOf(e, e.v("v")), e.v("s"))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), wvalSlot(e, e.v("ty"), e.v("s"))))));
            fnImpl(
                    e,
                    x,
                    "h_conv",
                    body(
                            e,
                            e.let("a0", hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", a(e, "a")))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("a0"))),
                            e.let("r", fresh(e, a(e, "mod"), a(e, "to"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "conv",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", wvalOf(e, e.v("a0"))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, a(e, "to"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_retag",
                    body(
                            e,
                            e.let("m", hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", a(e, "cv")))),
                            e.ret(cvOf(
                                    e,
                                    cveCode(e, e.v("m")),
                                    mk(
                                            e,
                                            "wval",
                                            e.p("ty", a(e, "ty")),
                                            e.p("source", e.callM(wvalOf(e, e.v("m")), "source", e.tup())))))));
            fnImpl(
                    e,
                    x,
                    "h_narrow",
                    body(
                            e,
                            e.let("bits", acc(e, xx(e), a(e, "to"), "wint", "bits")),
                            e.ifOnly(
                                    e.mb.operatorGe(e.v("bits"), num(e, 32)),
                                    retM(
                                            e,
                                            hf(
                                                    e,
                                                    "h_retag",
                                                    e.p("mod", a(e, "mod")),
                                                    e.p("cv", a(e, "cv")),
                                                    e.p("ty", a(e, "to"))))),
                            e.let("m", hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", a(e, "cv")))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("m"))),
                            e.let("r", fresh(e, a(e, "mod"), a(e, "to"))),
                            e.let("signed", acc(e, xx(e), a(e, "to"), "wint", "signed")),
                            e.ifOnly(
                                    andB(e, e.v("signed"), e.mb.operatorGt(e.v("bits"), num(e, 1))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    "unop",
                                                    e.p("res", e.v("r")),
                                                    e.p("op", e.cat(e.s("extend"), toStr(e, e.v("bits")), e.s("_s"))),
                                                    e.p("a", wvalOf(e, e.v("m")))))),
                            e.ifOnly(
                                    e.not(andB(e, e.v("signed"), e.mb.operatorGt(e.v("bits"), num(e, 1)))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    "binop",
                                                    e.p("res", e.v("r")),
                                                    e.p("op", e.s("and")),
                                                    e.p("a", wvalOf(e, e.v("m"))),
                                                    e.p(
                                                            "b",
                                                            wvalKonst(
                                                                    e,
                                                                    wvTy(e, wvalOf(e, e.v("m"))),
                                                                    e.ifElse(
                                                                            e.eq(e.v("bits"), num(e, 1)),
                                                                            e.dstr("1"),
                                                                            e.ifElse(
                                                                                    e.eq(e.v("bits"), num(e, 8)),
                                                                                    e.dstr("255"),
                                                                                    e.dstr("65535")))))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, a(e, "to"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_literal",
                    body(e, e.ret(cvOf(e, codeNone(e, loc(e)), wvalKonst(e, a(e, "ty"), a(e, "text"))))));
            fnDef(e, x, "c_ptr", ty(e), ty(e));
            fnImpl(e, x, "c_ptr", body(e, e.ret(mk(e, "wt_ptr", e.p("pointee", e.v("arg"))))));
            fnDef(e, x, "c_fn", e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e))), ty(e));
            fnImpl(
                    e,
                    x,
                    "c_fn",
                    body(e, e.ret(mk(e, "wt_fn", e.p("params", a(e, "params")), e.p("ret", a(e, "ret"))))));
            fnDef(e, x, "c_global_t", ty(e), ty(e));
            fnImpl(e, x, "c_global_t", body(e, e.ret(mk(e, "wt_global", e.p("base", e.v("arg"))))));
        }
        {
            accessor(e, x, "wval", "ty", ty(e));
            accessor(e, x, "wval", "source", t(e, "wsrc_u"));
            method(
                    e,
                    x,
                    "wval",
                    "ref",
                    e.tup(),
                    str(e),
                    e.matchOf(
                            in(e, "source"),
                            "s",
                            Opt.none(),
                            e.p("local", e.v("s")),
                            e.p("konst", e.v("s")),
                            e.p("func", e.v("s")),
                            e.p("slot", e.mb.panic(e.s("wasm: value in a slot used as an operand")))));
            method(
                    e,
                    x,
                    "wval",
                    "mem",
                    e.tup(),
                    str(e),
                    e.matchOf(
                            in(e, "source"),
                            "s",
                            Opt.none(),
                            e.p("slot", e.v("s")),
                            e.p("local", e.mb.panic(e.s("wasm: temporary has no slot"))),
                            e.p("konst", e.mb.panic(e.s("wasm: constant has no slot"))),
                            e.p("func", e.mb.panic(e.s("wasm: function has no slot")))));
            method(
                    e,
                    x,
                    "wval",
                    "is_mem",
                    e.tup(),
                    e.mb.typesInt(),
                    e.matchOf(in(e, "source"), "s", Opt.some(e.intOf(0)), e.p("slot", e.intOf(1))));
            x.add(e.implInterface(e.v("wval"), e.mb.evalValue(), e.p("type", in(e, "ty"))));
            accessor(e, x, "wtarget", "mod", wmodT(e));
            accessor(e, x, "wirctx", "xx", xxT(e));
            accessor(e, x, "winst", "k", t(e, "winst_u"));
            for (var vo : new String[] {"wlocal", "wlabel", "winst", "wmod", "wdata", "wblock"}) {
                markerType(e, x, vo, vo, vo + "_m", e.aspectDispatch(lv(e, vo + "_m_id")));
                x.add(e.implInterface(e.v(vo), t(e, vo + "_m")));
            }
            accessor(e, x, "wlocal", "name", str(e));
            accessor(e, x, "wlocal", "ty", ty(e));
            accessor(e, x, "wlocal", "param", boolT(e));
            accessor(e, x, "wlabel", "key", e.typesJumpKey());
            accessor(e, x, "wlabel", "name", str(e));
            accessor(e, x, "wdata", "name", str(e));
            accessor(e, x, "wdata", "addr", intT(e));
            accessor(e, x, "wdata", "bytes", e.listType(intT(e)));
            accessor(e, x, "wblock", "at", intT(e));
            accessor(e, x, "wblock", "name", str(e));

            method(e, x, "wmod", "xx", e.tup(), e.typesExprContext(), in(e, "xx", "value"));
            x.add(e.methodImpl(
                    e.v("wmod"),
                    "set_xx",
                    e.typesExprContext(),
                    e.optionTypeNone(),
                    e.stage(e.mb.set(in(e, "xx"), e.optSome(e.typesExprContext(), e.v("arg"))))));
            method(
                    e,
                    x,
                    "wmod",
                    "fresh_n",
                    e.tup(),
                    e.mb.typesInt(),
                    e.seq(e.let("n", in(e, "n")), e.incr(in(e, "n")), e.ret(e.v("n"))));
            for (var kind : new String[][] {{"fresh", "$t"}, {"slot", "$s"}}) {
                method(
                        e,
                        x,
                        "wmod",
                        kind[0],
                        ty(e),
                        str(e),
                        e.seq(
                                e.let("name", e.cat(e.s(kind[1]), toStr(e, e.callM(outer(e), "fresh_n", e.tup())))),
                                push(
                                        e,
                                        in(e, "locals"),
                                        mk(
                                                e,
                                                "wlocal",
                                                e.p("name", e.v("name")),
                                                e.p("ty", e.v("arg")),
                                                e.p("param", e.b(false)))),
                                e.ret(e.v("name"))));
            }
            method(
                    e,
                    x,
                    "wmod",
                    "fresh_label",
                    e.tup(),
                    str(e),
                    e.cat(e.s("$L"), toStr(e, e.callM(outer(e), "fresh_n", e.tup()))));
            method(
                    e,
                    x,
                    "wmod",
                    "label_of",
                    e.typesJumpKey(),
                    str(e),
                    e.seq(
                            e.let("st", e.rec(e.p("name", e.dstr("")), e.p("hit", e.intOf(0)))),
                            e.foreach(
                                    in(e, "labels"),
                                    "l0",
                                    e.seq(
                                            e.let("l", e.v("l0")),
                                            e.ifOnly(
                                                    e.eq(e.callM(e.v("l"), "key", e.tup()), e.v("arg")),
                                                    e.seq(
                                                            e.mb.set(
                                                                    e.fld(e.v("st"), "name"),
                                                                    e.callM(e.v("l"), "name", e.tup())),
                                                            e.mb.set(e.fld(e.v("st"), "hit"), num(e, 1)))))),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("st"), "hit"), num(e, 0)),
                                    e.seq(
                                            e.mb.set(
                                                    e.fld(e.v("st"), "name"),
                                                    e.callM(outer(e), "fresh_label", e.tup())),
                                            push(
                                                    e,
                                                    in(e, "labels"),
                                                    mk(
                                                            e,
                                                            "wlabel",
                                                            e.p("key", e.v("arg")),
                                                            e.p("name", e.fld(e.v("st"), "name")))))),
                            e.ret(e.fld(e.v("st"), "name"))));
            method(e, x, "wmod", "ir", ty(e), str(e), irFn(e, in(e, "xx", "value"), e.v("arg")));
            method(
                    e,
                    x,
                    "wmod",
                    "sig_ir",
                    ty(e),
                    str(e),
                    e.seq(
                            e.let("parts", e.listNew(str(e))),
                            e.foreach(
                                    im0(e, "wfn", "params", asp(e, in(e, "xx", "value"), e.v("arg"), "wfn")),
                                    "p",
                                    push(e, e.v("parts"), e.cat(e.s(" (param "), irOf(e, e.v("p")), e.s(")")))),
                            e.let("ret", im0(e, "wfn", "ret", asp(e, in(e, "xx", "value"), e.v("arg"), "wfn"))),
                            e.ifOnly(
                                    e.not(has(e, in(e, "xx", "value"), e.v("ret"), "wvoid")),
                                    push(e, e.v("parts"), e.cat(e.s(" (result "), irOf(e, e.v("ret")), e.s(")")))),
                            e.ret(e.join(e.v("parts"), ""))));
            method(
                    e,
                    x,
                    "wmod",
                    "addr",
                    str(e),
                    intT(e),
                    e.seq(
                            e.let("st", e.rec(e.p("addr", e.intOf(-1)))),
                            e.foreach(
                                    in(e, "data"),
                                    "d0",
                                    e.ifOnly(
                                            e.eq(e.callM(e.v("d0"), "name", e.tup()), e.v("arg")),
                                            e.mb.set(e.fld(e.v("st"), "addr"), e.callM(e.v("d0"), "addr", e.tup())))),
                            e.ifOnly(
                                    e.mb.operatorLt(e.fld(e.v("st"), "addr"), num(e, 0)),
                                    e.mb.panic(e.cat(e.s("wasm: no data segment named "), e.v("arg")))),
                            e.ret(e.fld(e.v("st"), "addr"))));
            method(
                    e,
                    x,
                    "wmod",
                    "data",
                    e.rec(e.p("name", str(e)), e.p("bytes", e.listType(intT(e)))),
                    str(e),
                    e.seq(
                            e.foreach(
                                    in(e, "data"),
                                    "d0",
                                    e.ifOnly(
                                            e.eq(e.callM(e.v("d0"), "name", e.tup()), a(e, "name")),
                                            e.mb.panic(e.cat(e.s("wasm: duplicate data segment "), a(e, "name"))))),
                            e.let(
                                    "addr",
                                    e.mb.operatorMul(
                                            e.mb.operatorDiv(
                                                    e.mb.operatorAdd(in(e, "data_end"), e.intOf(3)), e.intOf(4)),
                                            e.intOf(4))),
                            push(
                                    e,
                                    in(e, "data"),
                                    mk(
                                            e,
                                            "wdata",
                                            e.p("name", a(e, "name")),
                                            e.p("addr", e.v("addr")),
                                            e.p("bytes", a(e, "bytes")))),
                            countLet(e, "n"),
                            countLoop(e, "n", a(e, "bytes")),
                            e.mb.set(in(e, "data_end"), e.mb.operatorAdd(e.v("addr"), e.fld(e.v("n"), "v"))),
                            e.ret(a(e, "name"))));
            method(
                    e,
                    x,
                    "wmod",
                    "data_push",
                    e.rec(
                            e.p("bytes", e.listType(intT(e))),
                            e.p("width", intT(e)),
                            e.p("min", e.typesDouble()),
                            e.p("max", e.typesDouble()),
                            e.p("value", e.typesDouble())),
                    e.listType(intT(e)),
                    e.seq(
                            e.let("d", a(e, "value")),
                            e.ifOnly(
                                    e.not(e.eq(
                                            e.mb.as(e.mb.as(e.v("d"), e.mb.typesLong()), e.typesDouble()), e.v("d"))),
                                    e.mb.panic(e.cat(
                                            e.s("wasm: data element "), toStr(e, e.v("d")), e.s(" is not integral")))),
                            e.ifOnly(
                                    orB(
                                            e,
                                            e.mb.operatorLt(e.v("d"), a(e, "min")),
                                            e.mb.operatorGt(e.v("d"), a(e, "max"))),
                                    e.mb.panic(e.cat(
                                            e.s("wasm: data element "), toStr(e, e.v("d")), e.s(" is out of range")))),
                            e.let("lv", e.rec(e.p("v", e.mb.as(e.v("d"), e.mb.typesLong())))),
                            e.let("k", e.rec(e.p("i", e.intOf(0)))),
                            e.foreach(
                                    e.listNew(
                                            intT(e),
                                            e.intOf(0),
                                            e.intOf(1),
                                            e.intOf(2),
                                            e.intOf(3),
                                            e.intOf(4),
                                            e.intOf(5),
                                            e.intOf(6),
                                            e.intOf(7)),
                                    "_",
                                    e.ifOnly(
                                            e.mb.operatorLt(e.fld(e.v("k"), "i"), a(e, "width")),
                                            e.seq(
                                                    push(
                                                            e,
                                                            a(e, "bytes"),
                                                            e.mb.as(
                                                                    e.mb.operatorAnd(
                                                                            e.fld(e.v("lv"), "v"),
                                                                            e.mb.as(num(e, 255), e.mb.typesLong())),
                                                                    intT(e))),
                                                    e.mb.set(
                                                            e.fld(e.v("lv"), "v"),
                                                            e.mb.operatorUshr(
                                                                    e.fld(e.v("lv"), "v"),
                                                                    e.mb.as(num(e, 8), intT(e)))),
                                                    e.incr(e.fld(e.v("k"), "i"))))),
                            e.ret(a(e, "bytes"))));
            method(
                    e,
                    x,
                    "wmod",
                    "cstr",
                    str(e),
                    str(e),
                    e.seq(
                            e.let("bytes", e.listNew(intT(e))),
                            e.foreach(
                                    e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                    "b",
                                    push(
                                            e,
                                            e.v("bytes"),
                                            e.mb.operatorAnd(e.mb.as(e.v("b"), e.mb.typesInt()), num(e, 255)))),
                            push(e, e.v("bytes"), e.intOf(0)),
                            e.ret(e.callM(
                                    outer(e),
                                    "data",
                                    e.rec(
                                            e.p(
                                                    "name",
                                                    e.cat(e.s("str"), toStr(e, e.callM(outer(e), "fresh_n", e.tup())))),
                                            e.p("bytes", e.v("bytes")))))));
            method(
                    e,
                    x,
                    "wmod",
                    "import",
                    e.rec(e.p("module", str(e)), e.p("name", str(e)), e.p("ty", ty(e))),
                    str(e),
                    e.seq(
                            e.let("gname", e.cat(e.s("$"), a(e, "name"))),
                            e.let(
                                    "line",
                                    e.cat(
                                            e.s("(import \""),
                                            a(e, "module"),
                                            e.s("\" \""),
                                            a(e, "name"),
                                            e.s("\" (func "),
                                            e.v("gname"),
                                            e.callM(outer(e), "sig_ir", a(e, "ty")),
                                            e.s("))"))),
                            e.let("dup", e.rec(e.p("v", e.intOf(0)))),
                            e.foreach(
                                    in(e, "imports"),
                                    "l0",
                                    e.ifOnly(
                                            e.eq(e.v("l0"), e.v("line")), e.mb.set(e.fld(e.v("dup"), "v"), num(e, 1)))),
                            e.ifOnly(e.eq(e.fld(e.v("dup"), "v"), num(e, 0)), push(e, in(e, "imports"), e.v("line"))),
                            e.ret(e.v("gname"))));
            x.add(e.methodImpl(
                    e.v("wmod"),
                    "export",
                    str(e),
                    e.optionTypeNone(),
                    e.stage(push(
                            e,
                            in(e, "exports"),
                            e.cat(e.s("(export \""), e.v("arg"), e.s("\" (func $"), e.v("arg"), e.s("))"))))));
            defRenderState(e, x);
            method(
                    e,
                    x,
                    "wmod",
                    "render_insts",
                    e.rec(e.p("insts", instsT(e)), e.p("lstart", intT(e))),
                    e.rec(e.p("lines", e.listType(str(e))), e.p("used", e.listType(str(e)))),
                    e.seq(
                            e.let(
                                    "r",
                                    mk(
                                            e,
                                            "wrs",
                                            e.p("mod", outer(e)),
                                            e.p("out", e.listNew(str(e))),
                                            e.p("used", e.listNew(str(e))),
                                            e.p("uses", e.listNew(lt(e, "wuse"))),
                                            e.p("pn", e.listNew(lt(e, "wpend"))),
                                            e.p("sets", e.listNew(lt(e, "wblock"))),
                                            e.p("blocks", e.listNew(lt(e, "wblock"))),
                                            e.p(
                                                    "regions",
                                                    e.listNew(
                                                            lt(e, "wregion"),
                                                            mk(
                                                                    e,
                                                                    "wregion",
                                                                    e.p("depth", e.intOf(0)),
                                                                    e.p("start", e.intOf(0)),
                                                                    e.p("valued", e.b(false)),
                                                                    e.p("closed", e.intOf(0))))),
                                            e.p("n", e.intOf(0)),
                                            e.p("h", e.intOf(0)),
                                            e.p("depth", e.intOf(0)))),
                            e.let("li", e.rec(e.p("i", e.intOf(0)))),
                            e.foreach(
                                    in(e, "locals"),
                                    "l",
                                    e.seq(
                                            e.ifOnly(
                                                    e.mb.operatorGe(e.fld(e.v("li"), "i"), a(e, "lstart")),
                                                    rs(e, "add_use", e.callM(e.v("l"), "name", e.tup()))),
                                            e.incr(e.fld(e.v("li"), "i")))),
                            e.foreach(
                                    a(e, "insts"),
                                    "c0",
                                    e.matchOf(
                                            e.callM(e.v("c0"), "k", e.tup()),
                                            "p",
                                            Opt.some(e.seq()),
                                            e.p("if", countUse(e, p(e, "cond"))),
                                            e.p("binop", e.seq(countUse(e, p(e, "a")), countUse(e, p(e, "b")))),
                                            e.p("cmp", e.seq(countUse(e, p(e, "a")), countUse(e, p(e, "b")))),
                                            e.p("unop", countUse(e, p(e, "a"))),
                                            e.p("conv", countUse(e, p(e, "a"))),
                                            e.p("load", countName(e, p(e, "name"))),
                                            e.p("store", countUse(e, p(e, "val"))),
                                            e.p("push", countUse(e, p(e, "val"))),
                                            e.p("call", e.foreach(p(e, "args"), "ax", countUse(e, e.v("ax")))),
                                            e.p(
                                                    "ret",
                                                    e.ifOnly(
                                                            e.fld(p(e, "val"), "is_some"),
                                                            countUse(e, e.fld(p(e, "val"), "value")))))),
                            e.foreach(
                                    a(e, "insts"),
                                    "i0",
                                    e.matchOf(
                                            e.callM(e.v("i0"), "k", e.tup()),
                                            "p",
                                            Opt.none(),
                                            e.p("label", rs(e, "label", p(e, "name"))),
                                            e.p(
                                                    "br",
                                                    rs(
                                                            e,
                                                            "control",
                                                            e.rec(
                                                                    e.p("delta", e.intOf(0)),
                                                                    e.p("text", e.cat(e.s("br "), p(e, "name")))))),
                                            e.p(
                                                    "if",
                                                    e.seq(
                                                            rsOps(e, p(e, "cond")),
                                                            rs(
                                                                    e,
                                                                    "open_if",
                                                                    e.rec(
                                                                            e.p(
                                                                                    "text",
                                                                                    e.ifElse(
                                                                                            e.fld(
                                                                                                    p(e, "ty"),
                                                                                                    "is_some"),
                                                                                            e.cat(
                                                                                                    e.s("if (result "),
                                                                                                    irOf(
                                                                                                            e,
                                                                                                            e.fld(
                                                                                                                    p(
                                                                                                                            e,
                                                                                                                            "ty"),
                                                                                                                    "value")),
                                                                                                    e.s(")")),
                                                                                            e.dstr("if"))),
                                                                            e.p(
                                                                                    "valued",
                                                                                    e.fld(p(e, "ty"), "is_some")))))),
                                            e.p("else", rs0(e, "else_")),
                                            e.p("end", rs0(e, "end_if")),
                                            e.p(
                                                    "binop",
                                                    e.seq(
                                                            rsOps(e, p(e, "a"), p(e, "b")),
                                                            rsLine(
                                                                    e,
                                                                    -1,
                                                                    irOf(e, wvTy(e, p(e, "a"))),
                                                                    e.s("."),
                                                                    p(e, "op")),
                                                            rs(e, "set_local", p(e, "res")))),
                                            e.p(
                                                    "cmp",
                                                    e.seq(
                                                            rsOps(e, p(e, "a"), p(e, "b")),
                                                            rsLine(
                                                                    e,
                                                                    -1,
                                                                    irOf(e, wvTy(e, p(e, "a"))),
                                                                    e.s("."),
                                                                    p(e, "op")),
                                                            rs(e, "set_local", p(e, "res")))),
                                            e.p(
                                                    "unop",
                                                    e.seq(
                                                            rsOps(e, p(e, "a")),
                                                            rsLine(
                                                                    e,
                                                                    0,
                                                                    irOf(e, wvTy(e, p(e, "a"))),
                                                                    e.s("."),
                                                                    p(e, "op")),
                                                            rs(e, "set_local", p(e, "res")))),
                                            e.p(
                                                    "conv",
                                                    e.seq(
                                                            rsOps(e, p(e, "a")),
                                                            rsLine(e, 0, p(e, "op")),
                                                            rs(e, "set_local", p(e, "res")))),
                                            e.p(
                                                    "load",
                                                    e.seq(
                                                            rsOps(e, wvalLocal(e, p(e, "ty"), p(e, "name"))),
                                                            rs(e, "set_local", p(e, "res")))),
                                            e.p(
                                                    "store",
                                                    e.seq(rsOps(e, p(e, "val")), rs(e, "set_local", p(e, "name")))),
                                            e.p("push", rsOps(e, p(e, "val"))),
                                            e.p("setl", rs(e, "set_local", p(e, "name"))),
                                            e.p(
                                                    "call",
                                                    e.seq(
                                                            countLet(e, "na"),
                                                            countLoop(e, "na", p(e, "args")),
                                                            rs(e, "ops", p(e, "args")),
                                                            rs(
                                                                    e,
                                                                    "call",
                                                                    e.rec(
                                                                            e.p("fn", wvRef(e, p(e, "fn"))),
                                                                            e.p("nargs", e.fld(e.v("na"), "v")),
                                                                            e.p("res", p(e, "res")))))),
                                            e.p(
                                                    "ret",
                                                    e.seq(
                                                            e.ifOnly(
                                                                    e.fld(p(e, "val"), "is_some"),
                                                                    rsOps(e, e.fld(p(e, "val"), "value"))),
                                                            rs(
                                                                    e,
                                                                    "control",
                                                                    e.rec(
                                                                            e.p("delta", e.intOf(0)),
                                                                            e.p("text", e.s("return")))))))),
                            e.ret(rs0(e, "finish"))));
            method(
                    e,
                    x,
                    "wmod",
                    "render",
                    e.tup(),
                    renderT(e),
                    e.seq(
                            e.let("out", e.listNew(str(e))),
                            push(e, e.v("out"), e.s("(module")),
                            e.foreach(in(e, "imports"), "l", push(e, e.v("out"), e.cat(e.s("  "), e.v("l")))),
                            push(
                                    e,
                                    e.v("out"),
                                    e.cat(
                                            e.s("  (memory (export \"memory\") "),
                                            toStr(
                                                    e,
                                                    e.mb.operatorAdd(
                                                            e.mb.operatorDiv(in(e, "data_end"), e.intOf(65536)),
                                                            e.intOf(1))),
                                            e.s(")"))),
                            e.let("printable", printableBytes(e)),
                            e.foreach(
                                    in(e, "data"),
                                    "d0",
                                    e.seq(
                                            e.let("parts", e.listNew(str(e))),
                                            e.foreach(
                                                    e.callM(e.v("d0"), "bytes", e.tup()),
                                                    "b",
                                                    push(
                                                            e,
                                                            e.v("parts"),
                                                            e.callM(e.v("printable"), "get", toStr(e, e.v("b"))))),
                                            push(
                                                    e,
                                                    e.v("out"),
                                                    e.cat(
                                                            e.s("  (data (i32.const "),
                                                            toStr(e, e.callM(e.v("d0"), "addr", e.tup())),
                                                            e.s(") \""),
                                                            e.join(e.v("parts"), ""),
                                                            e.s("\")"))))),
                            e.foreach(in(e, "funcs"), "f", push(e, e.v("out"), e.v("f"))),
                            e.foreach(in(e, "exports"), "l", push(e, e.v("out"), e.cat(e.s("  "), e.v("l")))),
                            push(e, e.v("out"), e.s(")")),
                            e.ret(e.rec(e.p("wat", e.cat(e.join(e.v("out"), "\n"), e.s("\n")))))));
            method(
                    e,
                    x,
                    "wmod",
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
                            e.let("ptypes", acc(e, in(e, "xx", "value"), a(e, "ty"), "wfn", "params")),
                            e.let("fret", acc(e, in(e, "xx", "value"), a(e, "ty"), "wfn", "ret")),
                            countLet(e, "lstart"),
                            countLoop(e, "lstart", in(e, "locals")),
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
                            e.let("out", instsNew(e)),
                            e.let("entries", e.mapNew(e.mb.typesConstkey(), e.mb.evalBinding())),
                            e.let("pi", e.rec(e.p("i", e.intOf(0)))),
                            e.let("sig", e.listNew(str(e))),
                            e.foreach(
                                    e.v("ptypes"),
                                    "pty",
                                    e.seq(
                                            e.let("an", e.cat(e.s("$a"), toStr(e, e.fld(e.v("pi"), "i")))),
                                            push(
                                                    e,
                                                    e.v("sig"),
                                                    e.cat(
                                                            e.s(" (param "),
                                                            e.v("an"),
                                                            e.s(" "),
                                                            irOf(e, e.v("pty")),
                                                            e.s(")"))),
                                            push(
                                                    e,
                                                    in(e, "locals"),
                                                    mk(
                                                            e,
                                                            "wlocal",
                                                            e.p("name", e.v("an")),
                                                            e.p("ty", e.v("pty")),
                                                            e.p("param", e.b(true)))),
                                            e.callM(
                                                    e.v("entries"),
                                                    "put",
                                                    e.tup(
                                                            e.callM(
                                                                    e.v("pnames"),
                                                                    "get",
                                                                    toStr(e, e.fld(e.v("pi"), "i"))),
                                                            mk(
                                                                    e,
                                                                    "wbinding",
                                                                    e.p("ty", e.v("pty")),
                                                                    e.p("slot", e.v("an"))))),
                                            e.incr(e.fld(e.v("pi"), "i")))),
                            e.let("is_void", has(e, in(e, "xx", "value"), e.v("fret"), "wvoid")),
                            e.ifOnly(
                                    e.not(e.v("is_void")),
                                    push(e, e.v("sig"), e.cat(e.s(" (result "), irOf(e, e.v("fret")), e.s(")")))),
                            e.let("vx", e.call(e.evalBuiltin("eval_context"), e.tup())),
                            e.let(
                                    "sc",
                                    e.call(
                                            e.evalBuiltin("scope"),
                                            e.rec(e.p("vx", e.v("vx")), e.p("entries", e.v("entries"))))),
                            e.let("tgt", mk(e, "wtarget", e.p("mod", outer(e)))),
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
                                    mustV(
                                            e,
                                            e.call(
                                                    e.mb.evalConstEval(),
                                                    e.rec(e.p("xx", e.v("ctx")), e.p("staged", a(e, "body")))),
                                            "compile failed")),
                            e.let("ended", cveEnded(e, e.v("r"))),
                            e.ifOnly(
                                    andB(
                                            e,
                                            e.not(e.v("ended")),
                                            andB(e, e.v("is_void"), isWasm(e, e.v("ctx"), e.v("r")))),
                                    e.matchOf(
                                            e.eresultErr(
                                                    e.mb.evalValue(),
                                                    e.v("loc"),
                                                    e.s("void function body yields a value")),
                                            "__x",
                                            Opt.none(),
                                            e.p("ok", e.mb.panic(e.s("unreachable"))),
                                            e.p("errs", panicErrs(e, "compile failed", e.v("__x"))))),
                            e.let("need_cast", andB(e, e.not(e.v("ended")), e.not(e.v("is_void")))),
                            e.let(
                                    "v",
                                    mustV(
                                            e,
                                            e.ifElse(
                                                    e.v("need_cast"),
                                                    tryEr(
                                                            e,
                                                            operand(
                                                                    e,
                                                                    e.v("ctx"),
                                                                    e.v("loc"),
                                                                    outer(e),
                                                                    e.v("fret"),
                                                                    e.v("r")),
                                                            cveT(e)),
                                                    okCve(e, e.v("r"))),
                                            "compile failed")),
                            appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                            e.ifOnly(
                                    e.v("need_cast"),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(e, "ret", e.p("val", e.optSome(lt(e, "wval"), wvalOf(e, e.v("v"))))))),
                            e.let(
                                    "rendered",
                                    e.callM(
                                            outer(e),
                                            "render_insts",
                                            e.rec(e.p("insts", e.v("out")), e.p("lstart", e.fld(e.v("lstart"), "v"))))),
                            e.let("all", e.listNew(str(e))),
                            e.let("li", e.rec(e.p("i", e.intOf(0)))),
                            e.foreach(
                                    in(e, "locals"),
                                    "l",
                                    e.seq(
                                            e.ifOnly(
                                                    andB(
                                                            e,
                                                            e.mb.operatorGe(
                                                                    e.fld(e.v("li"), "i"), e.fld(e.v("lstart"), "v")),
                                                            e.not(e.callM(e.v("l"), "param", e.tup()))),
                                                    e.seq(
                                                            e.let("hit", e.rec(e.p("v", e.intOf(0)))),
                                                            e.foreach(
                                                                    e.fld(e.v("rendered"), "used"),
                                                                    "u",
                                                                    e.ifOnly(
                                                                            e.eq(
                                                                                    e.v("u"),
                                                                                    e.callM(e.v("l"), "name", e.tup())),
                                                                            e.mb.set(
                                                                                    e.fld(e.v("hit"), "v"),
                                                                                    num(e, 1)))),
                                                            e.ifOnly(
                                                                    e.eq(e.fld(e.v("hit"), "v"), num(e, 1)),
                                                                    push(
                                                                            e,
                                                                            e.v("all"),
                                                                            e.cat(
                                                                                    e.s("    (local "),
                                                                                    e.callM(e.v("l"), "name", e.tup()),
                                                                                    e.s(" "),
                                                                                    irOf(
                                                                                            e,
                                                                                            e.callM(
                                                                                                    e.v("l"), "ty",
                                                                                                    e.tup())),
                                                                                    e.s(")")))))),
                                            e.incr(e.fld(e.v("li"), "i")))),
                            e.foreach(
                                    e.fld(e.v("rendered"), "lines"),
                                    "l",
                                    push(e, e.v("all"), e.cat(e.s("    "), e.v("l")))),
                            push(
                                    e,
                                    in(e, "funcs"),
                                    e.cat(
                                            e.s("  (func $"),
                                            a(e, "name"),
                                            e.join(e.v("sig"), ""),
                                            e.s("\n"),
                                            e.join(e.v("all"), "\n"),
                                            e.s("\n  )"))),
                            e.ret(e.cat(e.s("$"), a(e, "name")))));
        }
        {
            selfType(
                    e,
                    x,
                    "wt_int",
                    e.cat(e.ifElse(in(e, "signed"), e.dstr("i"), e.dstr("u")), toStr(e, in(e, "bits"))),
                    e.aspectDispatch(
                            lv(e, "wtype_id"),
                            lv(e, "wint_id"),
                            opId(e, "add"),
                            opId(e, "sub"),
                            opId(e, "mul"),
                            opId(e, "div"),
                            opId(e, "mod"),
                            opId(e, "and"),
                            opId(e, "or"),
                            opId(e, "xor"),
                            opId(e, "shl"),
                            opId(e, "shr"),
                            opId(e, "ushr"),
                            opId(e, "eq"),
                            opId(e, "ne"),
                            opId(e, "lt"),
                            opId(e, "le"),
                            opId(e, "gt"),
                            opId(e, "ge"),
                            opId(e, "neg"),
                            opId(e, "bit_not"),
                            aspId(e, "bind"),
                            aspId(e, "set"),
                            aspId(e, "function")),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    orB(
                                            e,
                                            isConst(e, xx(e), srcType(e)),
                                            andB(
                                                    e,
                                                    has(e, xx(e), srcType(e), "wint"),
                                                    andB(
                                                            e,
                                                            e.mb.operatorLt(srcBits(e, "wint"), in(e, "bits")),
                                                            orB(
                                                                    e,
                                                                    e.eq(srcBits(e, "wint"), num(e, 1)),
                                                                    e.eq(
                                                                            acc(e, xx(e), srcType(e), "wint", "signed"),
                                                                            in(e, "signed"))))))))),
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(isEqual(e, xx(e), outer(e), e.v("st")), retM(e, okCve(e, a(e, "cv")))),
                            e.ifOnly(
                                    isConst(e, xx(e), e.v("st")),
                                    e.seq(
                                            e.let(
                                                    "d",
                                                    e.call(
                                                            e.evalBuiltin("const_numeric_to_double"),
                                                            e.rec(
                                                                    e.p("xx", xx(e)),
                                                                    e.p("value", cveValue(e, a(e, "cv")))))),
                                            e.let("l", e.mb.as(e.v("d"), e.mb.typesLong())),
                                            e.ifOnly(
                                                    e.not(e.eq(e.mb.as(e.v("l"), e.typesDouble()), e.v("d"))),
                                                    retM(e, errCve(e, loc(e), "integer literal is not integral"))),
                                            e.ifOnly(
                                                    orB(
                                                            e,
                                                            e.mb.operatorLt(e.v("d"), in(e, "min")),
                                                            e.mb.operatorGt(e.v("d"), in(e, "max"))),
                                                    retM(e, errCve(e, loc(e), "integer literal out of range"))),
                                            retM(
                                                    e,
                                                    okCve(
                                                            e,
                                                            cvOf(
                                                                    e,
                                                                    cveCode(e, a(e, "cv")),
                                                                    wvalKonst(e, outer(e), toStr(e, e.v("l")))))))),
                            e.ifOnly(
                                    e.eq(wide(e, xx(e), e.v("st")), wide(e, xx(e), outer(e))),
                                    retM(e, retag(e, a(e, "cv"), outer(e)))),
                            e.ret(conv(
                                    e,
                                    convName(
                                            e,
                                            outer(e),
                                            "extend",
                                            e.v("st"),
                                            Opt.some(e.ifElse(
                                                    e.eq(cvBits(e, e.v("st"), "wint"), num(e, 1)),
                                                    dfalse(e),
                                                    acc(e, xx(e), e.v("st"), "wint", "signed")))),
                                    a(e, "cv"),
                                    outer(e)))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "wint"),
                                    andB(
                                            e,
                                            e.eq(acc(e, xx(e), a(e, "other"), "wint", "bits"), in(e, "bits")),
                                            e.eq(acc(e, xx(e), a(e, "other"), "wint", "signed"), in(e, "signed")))))));
            x.add(e.implInterface(
                    e.v("wt_int"),
                    t(e, "wtype"),
                    e.p("ir", e.ifElse(e.eq(in(e, "bits"), num(e, 64)), e.dstr("i64"), e.dstr("i32"))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(
                    e.v("wt_int"),
                    t(e, "wint"),
                    e.p("bits", in(e, "bits")),
                    e.p("signed", in(e, "signed")),
                    e.p("min", in(e, "min")),
                    e.p("max", in(e, "max"))));
            op(e, x, "wt_int", "add", tryEr(e, binopBody(e, e.s("add")), cveT(e)));
            op(e, x, "wt_int", "sub", tryEr(e, binopBody(e, e.s("sub")), cveT(e)));
            op(e, x, "wt_int", "mul", tryEr(e, binopBody(e, e.s("mul")), cveT(e)));
            op(e, x, "wt_int", "div", tryEr(e, binopBody(e, signedOp(e, "div_s", "div_u")), cveT(e)));
            op(e, x, "wt_int", "mod", tryEr(e, binopBody(e, signedOp(e, "rem_s", "rem_u")), cveT(e)));
            op(e, x, "wt_int", "and", tryEr(e, binopBody(e, e.s("and")), cveT(e)));
            op(e, x, "wt_int", "or", tryEr(e, binopBody(e, e.s("or")), cveT(e)));
            op(e, x, "wt_int", "xor", tryEr(e, binopBody(e, e.s("xor")), cveT(e)));
            op(e, x, "wt_int", "shl", tryEr(e, binopBody(e, e.s("shl")), cveT(e)));
            op(e, x, "wt_int", "shr", tryEr(e, binopBody(e, signedOp(e, "shr_s", "shr_u")), cveT(e)));
            op(e, x, "wt_int", "ushr", tryEr(e, binopBody(e, e.s("shr_u")), cveT(e)));
            op(e, x, "wt_int", "eq", tryEr(e, cmpBody(e, e.s("eq")), cveT(e)));
            op(e, x, "wt_int", "ne", tryEr(e, cmpBody(e, e.s("ne")), cveT(e)));
            op(e, x, "wt_int", "lt", tryEr(e, cmpBody(e, signedOp(e, "lt_s", "lt_u")), cveT(e)));
            op(e, x, "wt_int", "le", tryEr(e, cmpBody(e, signedOp(e, "le_s", "le_u")), cveT(e)));
            op(e, x, "wt_int", "gt", tryEr(e, cmpBody(e, signedOp(e, "gt_s", "gt_u")), cveT(e)));
            op(e, x, "wt_int", "ge", tryEr(e, cmpBody(e, signedOp(e, "ge_s", "ge_u")), cveT(e)));
            x.add(e.implInterface(
                    e.v("wt_int"),
                    opIface(e, "neg"),
                    e.p(
                            "neg",
                            body(
                                    e,
                                    e.ret(tryEr(
                                            e,
                                            hf(
                                                    e,
                                                    "h_binop",
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("mod", xmod(e)),
                                                    e.p("ty", outer(e)),
                                                    e.p("op", e.s("sub")),
                                                    e.p("a", literal(e, outer(e), e.s("0"))),
                                                    e.p("b", a(e, "base")),
                                                    e.p("res", outer(e))),
                                            cveT(e)))))));
            x.add(e.implInterface(
                    e.v("wt_int"),
                    opIface(e, "bit_not"),
                    e.p(
                            "bit_not",
                            body(
                                    e,
                                    e.ret(tryEr(
                                            e,
                                            hf(
                                                    e,
                                                    "h_binop",
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("mod", xmod(e)),
                                                    e.p("ty", outer(e)),
                                                    e.p("op", e.s("xor")),
                                                    e.p("a", a(e, "base")),
                                                    e.p(
                                                            "b",
                                                            literal(
                                                                    e,
                                                                    outer(e),
                                                                    e.ifElse(
                                                                            e.eq(in(e, "bits"), num(e, 1)),
                                                                            e.dstr("1"),
                                                                            e.dstr("-1")))),
                                                    e.p("res", outer(e))),
                                            cveT(e)))))));
            bindImpl(e, x, "wt_int");
            setImpl(e, x, "wt_int");
            convImpl(
                    e,
                    x,
                    "wt_int",
                    "integer",
                    e.ifOnly(
                            has(e, xx(e), e.v("st"), "wint"),
                            retM(
                                    e,
                                    e.ifElse(
                                            e.eq(wide(e, xx(e), e.v("st")), wide(e, xx(e), outer(e))),
                                            e.ifElse(
                                                    e.mb.operatorLt(in(e, "bits"), cvBits(e, e.v("st"), "wint")),
                                                    narrow(e, e.v("v"), outer(e)),
                                                    retag(e, e.v("v"), outer(e))),
                                            e.ifElse(
                                                    wide(e, xx(e), e.v("st")),
                                                    narrow(
                                                            e,
                                                            hf(
                                                                    e,
                                                                    "h_conv",
                                                                    e.p("mod", xmod(e)),
                                                                    e.p(
                                                                            "op",
                                                                            convName(
                                                                                    e,
                                                                                    outer(e),
                                                                                    "wrap",
                                                                                    e.v("st"),
                                                                                    Opt.none())),
                                                                    e.p("a", e.v("v")),
                                                                    e.p("to", outer(e))),
                                                            outer(e)),
                                                    conv(
                                                            e,
                                                            convName(
                                                                    e,
                                                                    outer(e),
                                                                    "extend",
                                                                    e.v("st"),
                                                                    Opt.some(
                                                                            acc(
                                                                                    e, xx(e), e.v("st"), "wint",
                                                                                    "signed"))),
                                                            e.v("v"),
                                                            outer(e)))))),
                    e.ifOnly(
                            has(e, xx(e), e.v("st"), "wfloat"),
                            retM(
                                    e,
                                    narrow(
                                            e,
                                            hf(
                                                    e,
                                                    "h_conv",
                                                    e.p("mod", xmod(e)),
                                                    e.p(
                                                            "op",
                                                            convName(
                                                                    e,
                                                                    outer(e),
                                                                    "trunc",
                                                                    e.v("st"),
                                                                    Opt.some(in(e, "signed")))),
                                                    e.p("a", e.v("v")),
                                                    e.p("to", outer(e))),
                                            outer(e)))));

            selfType(
                    e,
                    x,
                    "wt_float",
                    e.cat(e.s("f"), toStr(e, in(e, "bits"))),
                    e.aspectDispatch(
                            lv(e, "wtype_id"),
                            lv(e, "wfloat_id"),
                            opId(e, "add"),
                            opId(e, "sub"),
                            opId(e, "mul"),
                            opId(e, "div"),
                            opId(e, "eq"),
                            opId(e, "ne"),
                            opId(e, "lt"),
                            opId(e, "le"),
                            opId(e, "gt"),
                            opId(e, "ge"),
                            opId(e, "neg"),
                            aspId(e, "bind"),
                            aspId(e, "set"),
                            aspId(e, "function")),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    orB(
                                            e,
                                            isConst(e, xx(e), srcType(e)),
                                            andB(
                                                    e,
                                                    has(e, xx(e), srcType(e), "wfloat"),
                                                    e.mb.operatorLt(srcBits(e, "wfloat"), in(e, "bits"))))))),
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(isEqual(e, xx(e), outer(e), e.v("st")), retM(e, okCve(e, a(e, "cv")))),
                            e.ifOnly(
                                    isConst(e, xx(e), e.v("st")),
                                    e.seq(
                                            e.let(
                                                    "d",
                                                    e.call(
                                                            e.evalBuiltin("const_numeric_to_double"),
                                                            e.rec(
                                                                    e.p("xx", xx(e)),
                                                                    e.p("value", cveValue(e, a(e, "cv")))))),
                                            e.let(
                                                    "s",
                                                    e.ifElse(
                                                            e.eq(in(e, "bits"), num(e, 64)),
                                                            toStr(e, e.v("d")),
                                                            toStr(
                                                                    e,
                                                                    e.mb.as(
                                                                            e.mb.as(e.v("d"), e.typesFloat()),
                                                                            e.typesDouble())))),
                                            retM(
                                                    e,
                                                    okCve(
                                                            e,
                                                            cvOf(
                                                                    e,
                                                                    cveCode(e, a(e, "cv")),
                                                                    wvalKonst(e, outer(e), e.v("s"))))))),
                            e.ret(conv(
                                    e, convName(e, outer(e), "promote", e.v("st"), Opt.none()), a(e, "cv"), outer(e)))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "wfloat"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "wfloat", "bits"), in(e, "bits"))))));
            x.add(e.implInterface(
                    e.v("wt_float"),
                    t(e, "wtype"),
                    e.p("ir", e.ifElse(e.eq(in(e, "bits"), num(e, 32)), e.dstr("f32"), e.dstr("f64"))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("wt_float"), t(e, "wfloat"), e.p("bits", in(e, "bits"))));
            op(e, x, "wt_float", "add", tryEr(e, binopBody(e, e.s("add")), cveT(e)));
            op(e, x, "wt_float", "sub", tryEr(e, binopBody(e, e.s("sub")), cveT(e)));
            op(e, x, "wt_float", "mul", tryEr(e, binopBody(e, e.s("mul")), cveT(e)));
            op(e, x, "wt_float", "div", tryEr(e, binopBody(e, e.s("div")), cveT(e)));
            op(e, x, "wt_float", "eq", tryEr(e, cmpBody(e, e.s("eq")), cveT(e)));
            op(e, x, "wt_float", "ne", tryEr(e, cmpBody(e, e.s("ne")), cveT(e)));
            op(e, x, "wt_float", "lt", tryEr(e, cmpBody(e, e.s("lt")), cveT(e)));
            op(e, x, "wt_float", "le", tryEr(e, cmpBody(e, e.s("le")), cveT(e)));
            op(e, x, "wt_float", "gt", tryEr(e, cmpBody(e, e.s("gt")), cveT(e)));
            op(e, x, "wt_float", "ge", tryEr(e, cmpBody(e, e.s("ge")), cveT(e)));
            x.add(e.implInterface(
                    e.v("wt_float"),
                    opIface(e, "neg"),
                    e.p(
                            "neg",
                            body(
                                    e,
                                    e.ret(tryEr(
                                            e,
                                            hf(
                                                    e,
                                                    "h_unop",
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("mod", xmod(e)),
                                                    e.p("ty", outer(e)),
                                                    e.p("op", e.s("neg")),
                                                    e.p("a", a(e, "base"))),
                                            cveT(e)))))));
            bindImpl(e, x, "wt_float");
            setImpl(e, x, "wt_float");
            convImpl(
                    e,
                    x,
                    "wt_float",
                    "float",
                    e.ifOnly(
                            has(e, xx(e), e.v("st"), "wint"),
                            retM(
                                    e,
                                    conv(
                                            e,
                                            convName(
                                                    e,
                                                    outer(e),
                                                    "convert",
                                                    e.v("st"),
                                                    Opt.some(acc(e, xx(e), e.v("st"), "wint", "signed"))),
                                            e.v("v"),
                                            outer(e)))),
                    e.ifOnly(
                            has(e, xx(e), e.v("st"), "wfloat"),
                            e.seq(
                                    e.let("sb", cvBits(e, e.v("st"), "wfloat")),
                                    retM(
                                            e,
                                            e.ifElse(
                                                    e.eq(e.v("sb"), in(e, "bits")),
                                                    retag(e, e.v("v"), outer(e)),
                                                    e.ifElse(
                                                            e.mb.operatorGt(e.v("sb"), in(e, "bits")),
                                                            conv(
                                                                    e,
                                                                    convName(
                                                                            e,
                                                                            outer(e),
                                                                            "demote",
                                                                            e.v("st"),
                                                                            Opt.none()),
                                                                    e.v("v"),
                                                                    outer(e)),
                                                            conv(
                                                                    e,
                                                                    convName(
                                                                            e,
                                                                            outer(e),
                                                                            "promote",
                                                                            e.v("st"),
                                                                            Opt.none()),
                                                                    e.v("v"),
                                                                    outer(e))))))));

            selfType(
                    e,
                    x,
                    "wt_ptr",
                    e.cat(e.s("ptr("), bim0(e, ty(e), "desc", in(e, "pointee")), e.s(")")),
                    e.aspectDispatch(
                            lv(e, "wtype_id"),
                            lv(e, "wptr_id"),
                            opId(e, "eq"),
                            opId(e, "ne"),
                            opId(e, "lt"),
                            opId(e, "le"),
                            opId(e, "gt"),
                            opId(e, "ge"),
                            aspId(e, "bind"),
                            aspId(e, "set")),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    andB(
                                            e,
                                            has(e, xx(e), srcType(e), "wptr"),
                                            has(e, xx(e), in(e, "pointee"), "wvoid"))))),
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(isEqual(e, xx(e), outer(e), e.v("st")), retM(e, okCve(e, a(e, "cv")))),
                            e.ret(retag(e, a(e, "cv"), outer(e)))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "wptr"),
                                    isEqual(
                                            e,
                                            xx(e),
                                            in(e, "pointee"),
                                            acc(e, xx(e), a(e, "other"), "wptr", "pointee"))))));
            x.add(e.implInterface(e.v("wt_ptr"), t(e, "wtype"), e.p("ir", e.s("i32")), e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("wt_ptr"), t(e, "wptr"), e.p("pointee", in(e, "pointee"))));
            op(e, x, "wt_ptr", "eq", tryEr(e, cmpBody(e, e.s("eq")), cveT(e)));
            op(e, x, "wt_ptr", "ne", tryEr(e, cmpBody(e, e.s("ne")), cveT(e)));
            op(e, x, "wt_ptr", "lt", tryEr(e, cmpBody(e, e.s("lt_u")), cveT(e)));
            op(e, x, "wt_ptr", "le", tryEr(e, cmpBody(e, e.s("le_u")), cveT(e)));
            op(e, x, "wt_ptr", "gt", tryEr(e, cmpBody(e, e.s("gt_u")), cveT(e)));
            op(e, x, "wt_ptr", "ge", tryEr(e, cmpBody(e, e.s("ge_u")), cveT(e)));
            bindImpl(e, x, "wt_ptr");
            setImpl(e, x, "wt_ptr");

            selfType(
                    e,
                    x,
                    "wt_fn",
                    e.s("fn"),
                    e.aspectDispatch(lv(e, "wtype_id"), lv(e, "wfn_id"), aspId(e, "function")),
                    body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                    body(e, e.ret(okCve(e, a(e, "cv")))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ifOnly(e.not(has(e, xx(e), a(e, "other"), "wfn")), retM(e, dfalse(e))),
                            e.ifOnly(
                                    e.not(isEqual(
                                            e,
                                            xx(e),
                                            in(e, "ret"),
                                            im0(e, "wfn", "ret", asp(e, xx(e), a(e, "other"), "wfn")))),
                                    retM(e, dfalse(e))),
                            e.let("om", e.mapNew(str(e), ty(e))),
                            e.let("on", e.rec(e.p("v", e.intOf(0)))),
                            e.foreach(
                                    im0(e, "wfn", "params", asp(e, xx(e), a(e, "other"), "wfn")),
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
                    e.v("wt_fn"),
                    t(e, "wtype"),
                    e.p(
                            "ir",
                            e.seq(
                                    e.let("parts", e.listNew(str(e))),
                                    e.foreach(
                                            in(e, "params"),
                                            "p",
                                            push(
                                                    e,
                                                    e.v("parts"),
                                                    e.cat(e.s(" (param "), irFn(e, irArgXx(e), e.v("p")), e.s(")")))),
                                    e.ifOnly(
                                            e.not(has(e, irArgXx(e), in(e, "ret"), "wvoid")),
                                            push(
                                                    e,
                                                    e.v("parts"),
                                                    e.cat(
                                                            e.s(" (result "),
                                                            irFn(e, irArgXx(e), in(e, "ret")),
                                                            e.s(")")))),
                                    e.ret(e.cat(e.s("(func"), e.join(e.v("parts"), ""), e.s(")"))))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("wt_fn"), t(e, "wfn"), e.p("params", in(e, "params")), e.p("ret", in(e, "ret"))));
            x.add(e.implInterface(
                    e.v("wt_fn"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "self"))),
                                    e.let("fnv", wvalOf(e, a(e, "self"))),
                                    e.let("args", e.listNew(lt(e, "wval"))),
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
                                    e.let("elems", e.listNew(e.mb.typesCve())),
                                    e.let("argv", a(e, "argument")),
                                    e.let("single", e.eq(e.fld(e.v("pn"), "v"), num(e, 1))),
                                    e.ifOnly(e.v("single"), push(e, e.v("elems"), e.v("argv"))),
                                    e.ifOnly(
                                            e.not(e.v("single")),
                                            e.seq(
                                                    e.ifOnly(
                                                            e.not(cveHas(e, e.v("argv"))),
                                                            retM(e, errCve(e, loc(e), "call argument has no value"))),
                                                    e.let(
                                                            "tk",
                                                            lookup(
                                                                    e,
                                                                    xx(e),
                                                                    cveType(e, e.v("argv")),
                                                                    aspIface(e, "tuple"))),
                                                    e.ifOnly(
                                                            e.not(e.fld(e.v("tk"), "is_some")),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            "call arguments must be a tuple"))),
                                                    e.foreach(
                                                            bim(
                                                                    e,
                                                                    aspIface(e, "tuple"),
                                                                    "take_elements",
                                                                    e.fld(e.v("tk"), "value"),
                                                                    e.rec(
                                                                            e.p("xx", xx(e)),
                                                                            e.p("loc", loc(e)),
                                                                            e.p("value", cveValue(e, e.v("argv"))))),
                                                            "er",
                                                            e.seq(
                                                                    e.let("ec0", unwrapCveM(e, e.v("er"))),
                                                                    push(e, e.v("elems"), e.v("ec0")))))),
                                    e.let("st", e.rec(e.p("i", e.intOf(0)))),
                                    e.foreach(
                                            e.v("elems"),
                                            "ec",
                                            e.seq(
                                                    e.ifOnly(
                                                            e.mb.operatorGe(
                                                                    e.fld(e.v("st"), "i"), e.fld(e.v("pn"), "v")),
                                                            retM(e, errCve(e, loc(e), "too many call arguments"))),
                                                    e.let(
                                                            "v",
                                                            unwrapCveM(
                                                                    e,
                                                                    tryEr(
                                                                            e,
                                                                            operand(
                                                                                    e,
                                                                                    xx(e),
                                                                                    loc(e),
                                                                                    xmod(e),
                                                                                    e.callM(
                                                                                            e.v("pmap"),
                                                                                            "get",
                                                                                            toStr(
                                                                                                    e,
                                                                                                    e.fld(
                                                                                                            e.v("st"),
                                                                                                            "i"))),
                                                                                    e.v("ec")),
                                                                            cveT(e)))),
                                                    appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                                    push(e, e.v("args"), wvalOf(e, e.v("v"))),
                                                    e.incr(e.fld(e.v("st"), "i")))),
                                    e.ifOnly(
                                            e.mb.operatorLt(e.fld(e.v("st"), "i"), e.fld(e.v("pn"), "v")),
                                            retM(e, errCve(e, loc(e), "too few call arguments"))),
                                    e.ifOnly(
                                            has(e, xx(e), in(e, "ret"), "wvoid"),
                                            e.seq(
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            inst(
                                                                    e,
                                                                    "call",
                                                                    e.p("res", e.optNone(str(e))),
                                                                    e.p("fn", e.v("fnv")),
                                                                    e.p("args", e.v("args")))),
                                                    retM(e, okCve(e, cvNo(e, codeOf(e, e.v("out"))))))),
                                    e.let("r", fresh(e, xmod(e), in(e, "ret"))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    "call",
                                                    e.p("res", e.optSome(str(e), e.v("r"))),
                                                    e.p("fn", e.v("fnv")),
                                                    e.p("args", e.v("args")))),
                                    e.ret(okCve(
                                            e, cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, in(e, "ret"), e.v("r"))))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));

            selfType(
                    e,
                    x,
                    "wt_void",
                    e.s("void"),
                    e.aspectDispatch(lv(e, "wtype_id"), lv(e, "wvoid_id")),
                    body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                    body(e, e.ret(okCve(e, a(e, "cv")))),
                    noCastTo(e),
                    castToPanic(e),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "wvoid"))));
            x.add(e.implInterface(
                    e.v("wt_void"),
                    t(e, "wtype"),
                    e.p("ir", e.mb.panic(e.s("wasm: void has no value type"))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("wt_void"), t(e, "wvoid")));

            selfType(
                    e,
                    x,
                    "wt_slot",
                    e.cat(bim0(e, ty(e), "desc", in(e, "base")), e.s("@"), in(e, "name")),
                    e.mb.ifOf(
                            TList.of(
                                    new TestMb.Branch(
                                            e.eq(a(e, "key"), lv(e, "wslot_id")),
                                            e.optSome(e.mb.typesAspect(), outer(e))),
                                    new TestMb.Branch(
                                            e.eq(a(e, "key"), lv(e, "wtype_id")),
                                            e.optSome(e.mb.typesAspect(), outer(e)))),
                            Opt.some(typeAspect(e, xx(e), in(e, "base"), a(e, "key")))),
                    body(e, e.ret(typeCanCastFrom(e, xx(e), loc(e), in(e, "base"), srcType(e)))),
                    body(
                            e,
                            e.let(
                                    "v",
                                    unwrapCveM(
                                            e,
                                            tryEr(
                                                    e,
                                                    operand(e, xx(e), loc(e), xmod(e), in(e, "base"), a(e, "cv")),
                                                    cveT(e)))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                            push(e, e.v("out"), storeInst(e, wvalOf(e, e.v("v")), in(e, "name"))),
                            e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), wvalSlot(e, outer(e), in(e, "name")))))),
                    body(e, e.ret(typeCanCastFrom(e, xx(e), loc(e), a(e, "dest_type"), in(e, "base")))),
                    body(
                            e,
                            e.let("r", fresh(e, xmod(e), in(e, "base"))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, a(e, "cv"))),
                            push(e, e.v("out"), loadInst(e, e.v("r"), in(e, "base"), in(e, "name"))),
                            e.let("loaded", cvOf(e, codeOf(e, e.v("out")), wvalLocal(e, in(e, "base"), e.v("r")))),
                            e.ret(castValue(e, xx(e), loc(e), a(e, "dest_type"), e.v("loaded")))),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "wslot"),
                                    andB(
                                            e,
                                            e.eq(acc(e, xx(e), a(e, "other"), "wslot", "name"), in(e, "name")),
                                            isEqual(
                                                    e,
                                                    xx(e),
                                                    in(e, "base"),
                                                    acc(e, xx(e), a(e, "other"), "wslot", "base")))))));
            x.add(e.implInterface(
                    e.v("wt_slot"),
                    t(e, "wtype"),
                    e.p("ir", irFn(e, irArgXx(e), in(e, "base"))),
                    e.p("ssa", in(e, "base"))));
            x.add(e.implInterface(
                    e.v("wt_slot"), t(e, "wslot"), e.p("base", in(e, "base")), e.p("name", in(e, "name"))));
            {
                var binOps = new String[] {
                    "add", "sub", "mul", "div", "mod", "and", "or", "xor", "shl", "shr", "ushr", "eq", "ne", "lt", "le",
                    "gt", "ge"
                };
                var unOps = new String[] {"neg", "bit_not"};
                var ids = new TList<SourceExpr>();
                ids.add(lv(e, "wgtype_m_id"));
                ids.add(aspId(e, "function"));
                for (var op : binOps) ids.add(opId(e, op));
                for (var op : unOps) ids.add(opId(e, op));
                selfType(
                        e,
                        x,
                        "wt_global",
                        e.cat(e.s("global("), bim0(e, ty(e), "desc", in(e, "base")), e.s(")")),
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
                                        hf(
                                                e,
                                                "h_gval",
                                                e.p("mod", xmod(e)),
                                                e.p("cv", a(e, "cv")),
                                                e.p("base", in(e, "base")))))),
                        body(
                                e,
                                e.ret(andB(
                                        e,
                                        has(e, xx(e), a(e, "other"), "wgtype_m"),
                                        isEqual(
                                                e,
                                                xx(e),
                                                in(e, "base"),
                                                acc(e, xx(e), a(e, "other"), "wgtype_m", "base"))))));
                x.add(e.implInterface(e.v("wt_global"), t(e, "wgtype_m"), e.p("base", in(e, "base"))));
                rtLiftImpl(e, x, "wt_global");
                rtBindImpl(e, x, "wt_global", e.mb.typesString());
                rtUseImpl(e, x, "wt_global");
                for (var op : binOps) {
                    x.add(e.implInterface(
                            e.v("wt_global"),
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
                                                            e.p(
                                                                    "base",
                                                                    hf(
                                                                            e,
                                                                            "h_gval",
                                                                            e.p("mod", xmod(e)),
                                                                            e.p("cv", a(e, "base")),
                                                                            e.p("base", in(e, "base")))),
                                                            e.p("reference", a(e, "reference")))))))));
                }
                for (var op : unOps) {
                    x.add(e.implInterface(
                            e.v("wt_global"),
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
                                                            e.p(
                                                                    "base",
                                                                    hf(
                                                                            e,
                                                                            "h_gval",
                                                                            e.p("mod", xmod(e)),
                                                                            e.p("cv", a(e, "base")),
                                                                            e.p("base", in(e, "base")))))))))));
                }
                x.add(e.implInterface(
                        e.v("wt_global"),
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
                                                        e.p(
                                                                "self",
                                                                hf(
                                                                        e,
                                                                        "h_gval",
                                                                        e.p("mod", xmod(e)),
                                                                        e.p("cv", a(e, "self")),
                                                                        e.p("base", in(e, "base")))),
                                                        e.p("argument", a(e, "argument"))))))),
                        e.p("exception_type", e.optionTypeLoweredNone())));
            }
        }
        {
            accessor(e, x, "wbinding", "ty", ty(e));
            accessor(e, x, "wbinding", "slot", str(e));
            bindingImpl(e, x, "wbinding", cvOf(e, a(e, "code"), wvalSlot(e, in(e, "ty"), in(e, "slot"))));
            bindingImpl(e, x, "wbinding_none", e.mb.panic(e.s("wasm: unit binding has no value")));

            markerType(e, x, "wunit", "unit", "wunit_m", e.aspectDispatch(lv(e, "wunit_m_id"), aspId(e, "bind")));
            x.add(e.implInterface(e.v("wunit"), t(e, "wunit_m")));
            x.add(e.implInterface(
                    e.v("wunit"),
                    e.mb.evalAspectBind(),
                    e.p(
                            "bind",
                            e.eresultOk(
                                    e.mb.typesBindResult(),
                                    e.call(
                                            e.mb.evalBindResult(),
                                            e.rec(
                                                    e.p("code", codeNone(e, loc(e))),
                                                    e.p("binding", mkArg(e, "wbinding_none", e.tup()))))))));

            selfType(
                    e,
                    x,
                    "wgfn_t",
                    e.cat(e.s("wasm global definition function "), toStr(e, in(e, "kind"))),
                    rtDispatch(e, lt(e, "wmod"), e.aspectDispatch(lv(e, "wgfn_m_id"), aspId(e, "function"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "wgfn_m"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "wgfn_m", "kind"), in(e, "kind"))))));
            rtLiftImpl(e, x, "wgfn_t");
            rtBindImpl(e, x, "wgfn_t", lt(e, "wmod"));
            rtUseImpl(e, x, "wgfn_t");
            x.add(e.implInterface(e.v("wgfn_t"), t(e, "wgfn_m"), e.p("kind", in(e, "kind"))));
            x.add(e.implInterface(
                    e.v("wgfn_t"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let("argv", a(e, "argument")),
                                    e.ifOnly(
                                            e.not(cveHas(e, e.v("argv"))),
                                            retM(e, errCve(e, loc(e), "wasm global definition argument has no value"))),
                                    e.let("st", cveType(e, e.v("argv"))),
                                    e.let("rk", lookup(e, xx(e), e.v("st"), aspIface(e, "record"))),
                                    e.ifOnly(
                                            e.not(e.fld(e.v("rk"), "is_some")),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            "wasm global definition argument must be a record"))),
                                    e.let(
                                            "want",
                                            e.ifElse(
                                                    wgfnIs(e, "cstr_fn"),
                                                    e.listNew(str(e), e.s("mod"), e.s("value")),
                                                    e.ifElse(
                                                            wgfnIs(e, "import_fn"),
                                                            e.listNew(
                                                                    str(e),
                                                                    e.s("mod"),
                                                                    e.s("module"),
                                                                    e.s("name"),
                                                                    e.s("ty")),
                                                            e.ifElse(
                                                                    wgfnIs(e, "func_fn"),
                                                                    e.listNew(
                                                                            str(e),
                                                                            e.s("mod"),
                                                                            e.s("name"),
                                                                            e.s("params"),
                                                                            e.s("ty"),
                                                                            e.s("body")),
                                                                    e.listNew(
                                                                            str(e),
                                                                            e.s("mod"),
                                                                            e.s("name"),
                                                                            e.s("elem"),
                                                                            e.s("values")))))),
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
                                                                    e.s("wasm global definition has no argument "),
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
                                                                    e.s("wasm global definition requires arguments "),
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
                                    e.let("modcv", unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("mod")))),
                                    e.ifOnly(
                                            wgfnIs(e, "cstr_fn"),
                                            e.seq(
                                                    e.let("valuecv", wgfnField(e, "value")),
                                                    e.let(
                                                            "refcv",
                                                            wgfnStaged(
                                                                    e,
                                                                    e.callM(
                                                                            wgfnMod(e),
                                                                            "cstr",
                                                                            graft(e, e.v("valuecv"))))),
                                                    retM(
                                                            e,
                                                            tagValue(
                                                                    e,
                                                                    e.v("refcv"),
                                                                    wgfnGlobalT(
                                                                            e,
                                                                            mk(
                                                                                    e,
                                                                                    "wt_ptr",
                                                                                    e.p(
                                                                                            "pointee",
                                                                                            intTy(e, "u8")))))))),
                                    e.ifOnly(wgfnIs(e, "data_fn"), dataFnBody(e)),
                                    e.let("tycv", wgfnField(e, "ty")),
                                    e.let("tyv", constTypeOf(e, "tycv", "wasm ty")),
                                    e.ifOnly(
                                            e.not(has(e, xx(e), e.v("tyv"), "wfn")),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("wasm ty must be a function type, not "),
                                                                    tyDesc(e, e.v("tyv")))))),
                                    e.let("namecv", wgfnField(e, "name")),
                                    e.ifOnly(
                                            wgfnIs(e, "import_fn"),
                                            e.seq(
                                                    e.let("modulecv", wgfnField(e, "module")),
                                                    e.let(
                                                            "refcv",
                                                            wgfnStaged(
                                                                    e,
                                                                    e.callM(
                                                                            wgfnMod(e),
                                                                            "import",
                                                                            e.rec(
                                                                                    e.p(
                                                                                            "module",
                                                                                            graft(e, e.v("modulecv"))),
                                                                                    e.p(
                                                                                            "name",
                                                                                            graft(e, e.v("namecv"))),
                                                                                    e.p("ty", e.lower(e.v("tyv"))))))),
                                                    retM(e, tagValue(e, e.v("refcv"), wgfnGlobalT(e, e.v("tyv")))))),
                                    e.let("pcv", wgfnField(e, "params")),
                                    e.let("ptk", lookup(e, xx(e), cveType(e, e.v("pcv")), aspIface(e, "tuple"))),
                                    e.ifOnly(
                                            e.not(e.fld(e.v("ptk"), "is_some")),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            "wasm function params must be a tuple of parameter names"))),
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
                                                    e.let(
                                                            "nsk",
                                                            lookup(
                                                                    e,
                                                                    xx(e),
                                                                    cveType(e, e.v("ncv")),
                                                                    e.mb.evalAspectStringConst())),
                                                    e.ifOnly(
                                                            e.not(e.fld(e.v("nsk"), "is_some")),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            "wasm function parameter names must be constant strings"))),
                                                    push(
                                                            e,
                                                            e.v("names"),
                                                            bim(
                                                                    e,
                                                                    e.mb.evalAspectStringConst(),
                                                                    "get",
                                                                    e.fld(e.v("nsk"), "value"),
                                                                    cveValue(e, e.v("ncv")))))),
                                    countLet(e, "pn"),
                                    countLoop(e, "pn", e.v("names")),
                                    countLet(e, "tn"),
                                    countLoop(e, "tn", acc(e, xx(e), e.v("tyv"), "wfn", "params")),
                                    e.ifOnly(
                                            e.not(e.eq(e.fld(e.v("pn"), "v"), e.fld(e.v("tn"), "v"))),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("wasm function has "),
                                                                    toStr(e, e.fld(e.v("pn"), "v")),
                                                                    e.s(" parameter names but its type has "),
                                                                    toStr(e, e.fld(e.v("tn"), "v")),
                                                                    e.s(" parameters"))))),
                                    e.let("bodycv", wgfnField(e, "body")),
                                    e.let(
                                            "refcv",
                                            wgfnStaged(
                                                    e,
                                                    e.callM(
                                                            wgfnMod(e),
                                                            "func",
                                                            e.rec(
                                                                    e.p("name", graft(e, e.v("namecv"))),
                                                                    e.p("params", e.lower(e.v("names"))),
                                                                    e.p("ty", e.lower(e.v("tyv"))),
                                                                    e.p("body", graft(e, e.v("bodycv"))))))),
                                    e.ret(tagValue(e, e.v("refcv"), wgfnGlobalT(e, e.v("tyv")))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));

            x.add(e.implInterface(
                    e.v("wtarget"),
                    e.mb.evalTarget(),
                    e.p("cast_exception", e.mb.panic(e.s("wasm target: exceptions are not supported"))),
                    e.p(
                            "initial_bind",
                            e.eresultErr(
                                    e.mb.evalBinding(),
                                    loc(e),
                                    e.s("wasm target: match and catch payloads are not supported"))),
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
                    e.p(
                            "jump",
                            e.eresultOk(
                                    codeT(e),
                                    codeOf(
                                            e,
                                            e.listNew(
                                                    lt(e, "winst"),
                                                    brInst(e, e.callM(mod(e), "label_of", a(e, "dest"))))))),
                    e.p(
                            "land",
                            e.eresultOk(
                                    cveT(e),
                                    cvNo(
                                            e,
                                            codeOf(
                                                    e,
                                                    e.listNew(
                                                            lt(e, "winst"),
                                                            labelInst(
                                                                    e, e.callM(mod(e), "label_of", a(e, "dest")))))))),
                    e.p("value_is_runtime", e.mb.bool(false)),
                    e.p(
                            "exit",
                            body(
                                    e,
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), a(e, "value", "code")),
                                    appendCode(
                                            e,
                                            e.v("out"),
                                            codeOf(
                                                    e,
                                                    e.listNew(
                                                            lt(e, "winst"),
                                                            brInst(e, e.callM(mod(e), "label_of", a(e, "dest")))))),
                                    e.ret(e.eresultOk(codeT(e), codeOf(e, e.v("out")))))),
                    e.p("guarded", e.mb.panic(e.s("wasm target: exceptions are not supported"))),
                    e.p("exc_cleanup", e.mb.panic(e.s("wasm target: exceptions are not supported"))),
                    e.p("exit_trampoline", e.mb.panic(e.s("wasm target: cleanup blocks are not supported"))),
                    e.p("drop", e.eresultOk(codeT(e), codeNone(e, loc(e)))),
                    e.p(
                            "return",
                            body(
                                    e,
                                    e.ifOnly(e.not(isWasm(e, xx(e), a(e, "cv"))), retM(e, a(e, "cv"))),
                                    e.let(
                                            "r",
                                            e.matchOf(
                                                    tryEr(
                                                            e,
                                                            hf(
                                                                    e,
                                                                    "h_store_new",
                                                                    e.p("xx", xx(e)),
                                                                    e.p("loc", loc(e)),
                                                                    e.p("mod", mod(e)),
                                                                    e.p("cv", a(e, "cv"))),
                                                            cveT(e)),
                                                    "__r",
                                                    Opt.none(),
                                                    e.p("ok", e.v("__r")),
                                                    e.p(
                                                            "errs",
                                                            e.mb.panic(
                                                                    e.s(
                                                                            "wasm target: return value could not be stored"))))),
                                    e.let("lv", wvalOf(e, e.v("r"))),
                                    e.ret(cvOf(
                                            e,
                                            cveCode(e, e.v("r")),
                                            wvalSlot(
                                                    e,
                                                    mk(
                                                            e,
                                                            "wt_slot",
                                                            e.p("base", wvTy(e, e.v("lv"))),
                                                            e.p("name", wvMem(e, e.v("lv")))),
                                                    wvMem(e, e.v("lv"))))))),
                    e.p("stage", errCve(e, loc(e), "wasm target: nested staging is not supported")),
                    e.p(
                            "if",
                            body(
                                    e,
                                    e.let("mod", mod(e)),
                                    e.let("out", instsNew(e)),
                                    e.let("i1", intTy(e, "i1")),
                                    e.let(
                                            "st",
                                            e.rec(
                                                    e.p("ended", e.intOf(1)),
                                                    e.p("has", e.intOf(0)),
                                                    e.p("novalue", e.intOf(0)),
                                                    e.p("ty", e.optionTypeLoweredNone()))),
                                    countLet(e, "nc"),
                                    countLoop(e, "nc", a(e, "conditions")),
                                    e.let("ci", e.rec(e.p("v", e.intOf(0)))),
                                    e.foreach(
                                            a(e, "conditions"),
                                            "br",
                                            e.seq(
                                                    e.let(
                                                            "c",
                                                            unwrapCveM(
                                                                    e,
                                                                    tryEr(
                                                                            e,
                                                                            operand(
                                                                                    e,
                                                                                    xx(e),
                                                                                    loc(e),
                                                                                    e.v("mod"),
                                                                                    e.v("i1"),
                                                                                    e.fld(e.v("br"), "condition")),
                                                                            cveT(e)))),
                                                    appendCode(e, e.v("out"), cveCode(e, e.v("c"))),
                                                    e.let("bo", instsNew(e)),
                                                    e.let("b", e.fld(e.v("br"), "body")),
                                                    ifBranchStmts(e, "b"),
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            inst(
                                                                    e,
                                                                    "if",
                                                                    e.p("cond", wvalOf(e, e.v("c"))),
                                                                    e.p("ty", e.fld(e.v("st"), "ty")))),
                                                    e.foreach(e.v("bo"), "i", push(e, e.v("out"), e.v("i"))),
                                                    e.incr(e.fld(e.v("ci"), "v")),
                                                    e.ifOnly(
                                                            orB(
                                                                    e,
                                                                    e.mb.operatorLt(
                                                                            e.fld(e.v("ci"), "v"),
                                                                            e.fld(e.v("nc"), "v")),
                                                                    e.fld(a(e, "else"), "is_some")),
                                                            push(e, e.v("out"), inst0(e, "else"))))),
                                    e.ifOnly(
                                            e.fld(a(e, "else"), "is_some"),
                                            e.seq(
                                                    e.let("bo", instsNew(e)),
                                                    e.let("b", e.fld(a(e, "else"), "value")),
                                                    ifBranchStmts(e, "b"),
                                                    e.foreach(e.v("bo"), "i", push(e, e.v("out"), e.v("i"))))),
                                    e.ifOnly(
                                            e.not(e.fld(a(e, "else"), "is_some")),
                                            e.seq(
                                                    e.mb.set(e.fld(e.v("st"), "ended"), num(e, 0)),
                                                    e.ifOnly(
                                                            e.eq(e.fld(e.v("st"), "has"), num(e, 1)),
                                                            e.mb.set(e.fld(e.v("st"), "novalue"), num(e, 1))))),
                                    e.foreach(a(e, "conditions"), "_", push(e, e.v("out"), inst0(e, "end"))),
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
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            "if: some branches yield a value and some do not"))),
                                    e.let("r", fresh(e, e.v("mod"), e.fld(e.v("st"), "ty", "value"))),
                                    push(e, e.v("out"), inst(e, "setl", e.p("name", e.v("r")))),
                                    e.ret(okCve(
                                            e,
                                            cvOf(
                                                    e,
                                                    codeOf(e, e.v("out")),
                                                    wvalLocal(e, e.fld(e.v("st"), "ty", "value"), e.v("r"))))))),
                    e.p("try_catch_finally", errCve(e, loc(e), "wasm target: try/catch/finally is not supported")),
                    e.p("match", errCve(e, loc(e), "wasm target: match is not supported"))));
        }
        x.add(e.let("wasm_b", e.defFunc(e.v("d"), "wasm")));
        x.add(e.funcImpl(
                e.v("wasm_b"),
                e.tup(),
                e.optionTypeSome(t(e, "wmod")),
                e.mb.optNoneType(),
                e.stage(e.seq(
                        e.let(
                                "m",
                                mk(
                                        e,
                                        "wmod",
                                        e.p("n", num(e, 0)),
                                        e.p("xx", e.optNone(e.typesExprContext())),
                                        e.p("imports", e.listNew(str(e))),
                                        e.p("data", e.listNew(lt(e, "wdata"))),
                                        e.p("data_end", e.intOf(8)),
                                        e.p("funcs", e.listNew(str(e))),
                                        e.p("exports", e.listNew(str(e))),
                                        e.p("labels", e.listNew(lt(e, "wlabel"))),
                                        e.p("locals", e.listNew(lt(e, "wlocal"))))),
                        e.let("tgt", mk(e, "wtarget", e.p("mod", e.v("m")))),
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
        var passthrough = new String[] {"ptr", "fn", "global_t"};
        var defs = new TList<EcoMb.P>();
        defs.add(e.p("new", e.fld(e.v("wasm_b"), "func")));
        defs.add(e.p("mod_t", t(e, "wmod")));
        for (var name : passthrough) defs.add(e.p(name, e.v("c_" + name)));
        for (var obj : new String[] {"wt_int", "wt_float", "wt_void", "wgfn_t", "wt_ptr", "wt_global"}) {
            defs.add(e.p(obj + "_new", e.fld(e.v(obj), "new")));
        }
        x.add(e.ret(e.rec(defs.toArray(EcoMb.P[]::new))));

        var y = new TList<SourceExpr>();
        y.add(e.let("k", e.lower(e.mb.seq(x))));
        var exported = new TList<String>();
        for (var spec : INT_SPECS) {
            exported.add(spec.name());
            y.add(e.let(
                    spec.name(),
                    lty(
                            e,
                            "wt_int",
                            e.rec(
                                    e.p("bits", e.intOf(spec.bits())),
                                    e.p("signed", e.b(spec.signed())),
                                    e.p("min", e.n(spec.min())),
                                    e.p("max", e.n(spec.max()))))));
        }
        for (var bits : new int[] {32, 64}) {
            exported.add("f" + bits);
            y.add(e.let("f" + bits, lty(e, "wt_float", e.rec(e.p("bits", e.intOf(bits))))));
        }
        exported.add("void");
        y.add(e.let("void", lty(e, "wt_void", e.tup())));
        y.add(e.let(
                "cstr_t", lty(e, "wt_global", e.rec(e.p("base", lty(e, "wt_ptr", e.rec(e.p("pointee", e.v("u8")))))))));
        for (var kind : WGFN_KINDS) {
            if (!kind.equals("cstr_fn")) exported.add(kind);
            y.add(e.let(kind, lty(e, "wgfn_t", e.rec(e.p("kind", e.intOf(wgfnKind(kind)))))));
        }
        y.add(e.let("d", e.define()));
        y.add(e.optionTypeLet());
        y.add(e.let("mod_o", e.defObject(e.v("d"), "wasm module")));
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
                e.v("mod_o"),
                "export",
                str(e),
                e.optionTypeNone(),
                e.stage(e.callM(in(e, "m"), "export", e.v("arg")))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "cstr",
                str(e),
                e.optionTypeSome(e.v("cstr_t")),
                e.stage(e.call(e.lowerV("cstr_fn"), e.rec(e.p("mod", outer(e)), e.p("value", e.v("arg")))))));
        y.add(e.let("wasm_b", e.defFunc(e.v("d"), "wasm")));
        y.add(e.funcImpl(
                e.v("wasm_b"),
                e.tup(),
                e.optionTypeSome(e.fld(e.v("mod_o"), "type")),
                e.mb.optNoneType(),
                e.stage(mkArg(e, "mod_o", e.rec(e.p("m", e.call(e.fld(e.lowerV("k"), "new"), e.tup())))))));
        y.add(e.let("build_b", e.defFunc(e.v("d"), "build")));
        y.add(e.funcImpl(
                e.v("build_b"),
                renderT(e),
                e.optionTypeSome(e.mb.builtin("types", "app")),
                e.mb.optNoneType(),
                e.stage(e.call(
                        e.mb.builtin("app"),
                        e.rec(
                                e.p("executor", e.optSome(str(e), e.s("wasmtime"))),
                                e.p("bytes", e.call(e.mb.evalStrUtf8(), a(e, "wat"))))))));
        y.add(e.finish(e.v("d")));
        var ydefs = new TList<EcoMb.P>();
        ydefs.add(e.p("new", e.fld(e.v("wasm_b"), "func")));
        ydefs.add(e.p("build", e.fld(e.v("build_b"), "func")));
        ydefs.add(e.p("mod_t", e.fld(e.v("mod_o"), "type")));
        for (var name : passthrough) ydefs.add(e.p(name, e.fld(e.v("k"), name)));
        for (var name : exported) ydefs.add(e.p(name, e.v(name)));
        y.add(e.ret(e.rec(ydefs.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(y))));
        var exports = new TList<EcoMb.P>();
        for (var name : new String[] {"new", "build", "mod_t"}) exports.add(e.p(name, e.fld(e.v("k"), name)));
        for (var name : passthrough) exports.add(e.p(name, e.fld(e.v("k"), name)));
        for (var name : exported) exports.add(e.p(name, e.fld(e.v("k"), name)));
        top.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return top;
    }
}
