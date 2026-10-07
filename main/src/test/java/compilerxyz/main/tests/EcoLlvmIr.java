package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoLlvmIr {
    private EcoLlvmIr() {}

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        return EcoTargetMb.fnDef(e, out, "llvm", name, argType, ret);
    }

    private static SourceExpr panicErrs(EcoMb e, String what, SourceExpr errs) {
        return EcoTargetMb.panicErrs(e, "llvm", what, errs);
    }

    private static SourceExpr mustV(EcoMb e, SourceExpr res, String what) {
        return EcoTargetMb.mustV(e, "llvm", res, what);
    }

    private static SourceExpr lvalT(EcoMb e) {
        return t(e, "lval");
    }

    private static SourceExpr lmodT(EcoMb e) {
        return t(e, "lmod");
    }

    private static SourceExpr ltailOptT(EcoMb e) {
        return e.call(e.mb.typesOption(), t(e, "ltail"));
    }

    private static SourceExpr lmodOptT(EcoMb e) {
        return e.call(e.mb.typesOption(), t(e, "lmod"));
    }

    private static SourceExpr renderT(EcoMb e) {
        return e.rec(e.p("ir", str(e)), e.p("libs", e.listType(str(e))));
    }

    private static SourceExpr initFn(EcoMb e, SourceExpr ty, SourceExpr cv) {
        return hf(e, "h_init", e.p("xx", xx(e)), e.p("loc", loc(e)), e.p("ty", ty), e.p("cv", cv));
    }

    private record HostInt(String name, double min, double max) {}

    private static final IList<HostInt> HOST_INTS = IList.of(
            new HostInt("byte", -128, 127),
            new HostInt("ubyte", 0, 255),
            new HostInt("short", -32768, 32767),
            new HostInt("ushort", 0, 65535),
            new HostInt("int", -2147483648.0, 2147483647.0),
            new HostInt("uint", 0, 4294967295.0),
            new HostInt("long", -9223372036854775808.0, 9223372036854775807.0),
            new HostInt("ulong", 0, 18446744073709551615.0));

    private static final IList<String> DATA_FACTORIES =
            IList.of("const_list", "global_list", "const_def", "global_def");

    private record LlvmPrim(String llvm, String ir, String host, int bits, boolean signed, boolean isFloat) {}

    private static final IList<LlvmPrim> LLVM_PRIMS = IList.of(
            new LlvmPrim("i8", "i8", "byte", 8, true, false),
            new LlvmPrim("u8", "i8", "ubyte", 8, false, false),
            new LlvmPrim("i16", "i16", "short", 16, true, false),
            new LlvmPrim("u16", "i16", "ushort", 16, false, false),
            new LlvmPrim("i32", "i32", "int", 32, true, false),
            new LlvmPrim("u32", "i32", "uint", 32, false, false),
            new LlvmPrim("i64", "i64", "long", 64, true, false),
            new LlvmPrim("u64", "i64", "ulong", 64, false, false),
            new LlvmPrim("f32", "float", "float", 32, false, true),
            new LlvmPrim("f64", "double", "double", 64, false, true));

    private static SourceExpr isPrim(EcoMb e, SourceExpr xx, SourceExpr ty, LlvmPrim prim) {
        return prim.isFloat()
                ? andB(e, has(e, xx, ty, "lfloat"), e.eq(acc(e, xx, ty, "lfloat", "bits"), num(e, prim.bits())))
                : andB(
                        e,
                        has(e, xx, ty, "lint"),
                        andB(
                                e,
                                e.eq(acc(e, xx, ty, "lint", "bits"), num(e, prim.bits())),
                                prim.signed()
                                        ? acc(e, xx, ty, "lint", "signed")
                                        : e.not(acc(e, xx, ty, "lint", "signed"))));
    }

    private static SourceExpr[] globalDef(EcoMb e, boolean konst, SourceExpr init) {
        return new SourceExpr[] {
            e.let("gname", e.cat(e.s("@"), a(e, "name"))),
            push(
                    e,
                    in(e, konst ? "constants" : "globals"),
                    mk(e, "lglobal", e.p("name", e.v("gname")), e.p("ty", e.v("gty")), e.p("init", init))),
            e.ret(mk(
                    e,
                    "lgref",
                    e.p("ref", e.v("gname")),
                    e.p("mod", outer(e)),
                    e.p(
                            "ty",
                            konst
                                    ? mk(
                                            e,
                                            "lt_global",
                                            e.p("base", mk(e, "lt_ptr", e.p("pointee", e.v("gty")))),
                                            e.p("mem", e.b(false)))
                                    : mk(e, "lt_global", e.p("base", e.v("gty")), e.p("mem", e.b(true))))))
        };
    }

    private static SourceExpr ptrPointeeLet(EcoMb e) {
        return e.let("pointee", e.callM(e.v("desc"), "pointee", e.tup()));
    }

    private static SourceExpr ptrObjLet(EcoMb e) {
        return e.let("gref_obj", e.fld(e.lowerV("k"), "gref_obj_t"));
    }

    private static SourceExpr grefsFill(EcoMb e, SourceExpr grefs) {
        return e.foreach(
                grefs,
                "g",
                push(e, e.v("refs"), e.call(e.mb.mustReinterpret(), e.tup(e.v("g"), e.lower(e.v("gref_obj"))))));
    }

    private static SourceExpr grefsNew(EcoMb e) {
        return e.let("refs", e.listNew(e.lower(e.v("gref_obj"))));
    }

    private static SourceExpr specialize(EcoMb e, String name, SourceExpr argType, SourceExpr implBody) {
        return e.seq(
                e.let("sp_d", e.define()),
                e.let("sp", e.callM(e.v("sp_d"), "func", e.s(name))),
                e.callM(
                        e.v("sp"),
                        "impl",
                        e.rec(
                                e.p("arg", argType),
                                e.p("ret", e.optionTypeLoweredSome(e.lowerV("gref_t"))),
                                e.p("impl_body", e.stage(implBody)),
                                e.p("exception_type", e.optionTypeLoweredNone()),
                                e.p("description", e.optNone(str(e))))),
                e.finish(e.v("sp_d")),
                retM(
                        e,
                        okCve(
                                e,
                                cvOf(
                                        e,
                                        codeNone(e, loc(e)),
                                        unwrapM(
                                                e,
                                                bim(
                                                        e,
                                                        e.mb.evalAspectLift(),
                                                        "lift",
                                                        lookupV(e, xx(e), ty(e), e.mb.evalAspectLift()),
                                                        e.rec(
                                                                e.p("xx", xx(e)),
                                                                e.p("loc", loc(e)),
                                                                e.p(
                                                                        "external_host_value",
                                                                        e.call(
                                                                                e.mb.mustReinterpret(),
                                                                                e.tup(
                                                                                        e.mb.as(
                                                                                                e.fld(
                                                                                                        e.v("sp"),
                                                                                                        "func"),
                                                                                                ty(e)),
                                                                                        e.mb.builtin(
                                                                                                "types", "object")))))),
                                                e.mb.typesCve())))));
    }

    private static SourceExpr initMismatch(EcoMb e, SourceExpr st) {
        return throwErr(
                e, loc(e), e.cat(e.s("cannot initialize llvm "), tyDesc(e, a(e, "ty")), e.s(" from "), tyDesc(e, st)));
    }

    private static SourceExpr foldJoin(EcoMb e, SourceExpr parts, String open, String close) {
        return e.mb.labeled(
                "fold",
                e.seq(
                        e.let("accs", e.mapNew(str(e), cveT(e))),
                        e.callM(e.v("accs"), "put", e.tup(e.s("0"), hostCve(e, e.stage(e.s(open))))),
                        e.let("fi", e.rec(e.p("i", e.intOf(0)))),
                        e.foreach(
                                parts,
                                "p",
                                e.seq(
                                        e.let("k0", toStr(e, e.fld(e.v("fi"), "i"))),
                                        e.incr(e.fld(e.v("fi"), "i")),
                                        e.callM(
                                                e.v("accs"),
                                                "put",
                                                e.tup(
                                                        toStr(e, e.fld(e.v("fi"), "i")),
                                                        hostCve(
                                                                e,
                                                                e.stage(
                                                                        e.cat(
                                                                                graft(
                                                                                        e,
                                                                                        e.callM(
                                                                                                e.v("accs"),
                                                                                                "get",
                                                                                                e.v("k0"))),
                                                                                e.lower(
                                                                                        e.ifElse(
                                                                                                e.eq(
                                                                                                        e.fld(
                                                                                                                e.v(
                                                                                                                        "fi"),
                                                                                                                "i"),
                                                                                                        num(e, 1)),
                                                                                                e.dstr(""),
                                                                                                e.dstr(", "))),
                                                                                graft(e, e.v("p"))))))))),
                        e.mb.return_(
                                "fold",
                                hostCve(
                                        e,
                                        e.stage(e.cat(
                                                graft(e, e.callM(e.v("accs"), "get", toStr(e, e.fld(e.v("fi"), "i")))),
                                                e.s(close)))))));
    }

    private static SourceExpr irArgXx(EcoMb e) {
        return e.callM(e.v("arg"), "xx", e.tup());
    }

    private static SourceExpr irArgMod(EcoMb e) {
        return e.callM(e.v("arg"), "mod", e.tup());
    }

    private static SourceExpr irFn(EcoMb e, SourceExpr xx, SourceExpr mod, SourceExpr ty) {
        return hf(e, "h_ir", e.p("xx", xx), e.p("mod", mod), e.p("ty", ty));
    }

    private static SourceExpr instsT(EcoMb e) {
        return e.listType(t(e, "linst"));
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
                        e.call(e.mb.reinterpret(), e.tup(loc(e), e.fld(xx(e), "target"), lt(e, "ltarget"))),
                        retInner),
                "mod",
                e.tup());
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
                        "lt_int",
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

    private static SourceExpr lvalOf(EcoMb e, SourceExpr cveTP) {
        return reinterpret(e, cveValue(e, cveTP), "lval");
    }

    private static SourceExpr lvalNew(EcoMb e, SourceExpr ty, SourceExpr ref) {
        return mk(e, "lval", e.p("ty", ty), e.p("source", e.call(e.lowerV("ls_ssa"), ref)));
    }

    private static SourceExpr lvalMem(EcoMb e, SourceExpr ty, SourceExpr ptr) {
        return mk(e, "lval", e.p("ty", ty), e.p("source", e.call(e.lowerV("ls_mem"), ptr)));
    }

    private static SourceExpr lvMem(EcoMb e, SourceExpr lvalTP) {
        return e.callM(lvalTP, "mem", e.tup());
    }

    private static SourceExpr lvIsMem(EcoMb e, SourceExpr lvalTP) {
        return e.eq(e.callM(lvalTP, "is_mem", e.tup()), num(e, 1));
    }

    private static SourceExpr operand(
            EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr mod, SourceExpr ty, SourceExpr cv) {
        return hf(e, "h_operand", e.p("xx", xx), e.p("loc", loc), e.p("mod", mod), e.p("ty", ty), e.p("cv", cv));
    }

    private static SourceExpr instsNew(EcoMb e) {
        return e.listNew(lt(e, "linst"));
    }

    private static SourceExpr inst(EcoMb e, String variant, EcoMb.P... fields) {
        return mk(e, "linst", e.p("k", e.call(e.lowerV("li_" + variant), e.rec(fields))));
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

    private static SourceExpr isLlvm(EcoMb e, SourceExpr xx, SourceExpr cv) {
        return hf(e, "h_is_llvm", e.p("xx", xx), e.p("cv", cv));
    }

    private static void defVariant(EcoMb e, TList<SourceExpr> out, String tag, SourceExpr typeRef) {
        out.add(e.let(
                "li_" + tag,
                e.callM(e.v("linst_u"), "variant", e.rec(e.p("tag", e.s(tag)), e.p("type_ref", typeRef)))));
    }

    private static SourceExpr fresh(EcoMb e, SourceExpr mod) {
        return e.callM(mod, "fresh", e.tup());
    }

    private static SourceExpr freshLabel(EcoMb e, SourceExpr mod) {
        return e.callM(mod, "fresh_label", e.tup());
    }

    private static SourceExpr lvRef(EcoMb e, SourceExpr lvalTP) {
        return e.callM(lvalTP, "ref", e.tup());
    }

    private static SourceExpr lvTy(EcoMb e, SourceExpr lvalTP) {
        return e.callM(lvalTP, "ty", e.tup());
    }

    private static SourceExpr storeInst(EcoMb e, SourceExpr val, SourceExpr ptr) {
        return inst(e, "store", e.p("val", val), e.p("ptr", ptr));
    }

    private static SourceExpr labelInst(EcoMb e, SourceExpr name) {
        return inst(e, "label", e.p("name", name));
    }

    private static SourceExpr brInst(EcoMb e, SourceExpr name) {
        return inst(e, "br", e.p("name", name));
    }

    private static SourceExpr instBinopI64(EcoMb e, String res, String op, SourceExpr a, SourceExpr b) {
        return push(
                e,
                e.v("out"),
                inst(
                        e,
                        "binop",
                        e.p("res", e.v(res)),
                        e.p("op", e.s(op)),
                        e.p("a", lvalNew(e, e.v("i64"), a)),
                        e.p("b", lvalNew(e, e.v("i64"), b))));
    }

    private static SourceExpr loadInst(EcoMb e, SourceExpr res, SourceExpr ty, SourceExpr ptr) {
        return inst(e, "load", e.p("res", res), e.p("ty", ty), e.p("ptr", ptr));
    }

    private static SourceExpr renderArm(EcoMb e, SourceExpr... lineParts) {
        return e.seq(push(e, e.v("out"), e.cat(lineParts)), e.mb.set(e.fld(e.v("st"), "open"), num(e, 1)));
    }

    private static SourceExpr countRef(EcoMb e, SourceExpr name) {
        return e.callM(
                e.v("refs"),
                "put",
                e.tup(name, e.mb.operatorAdd(e.mb.as(e.callM(e.v("refs"), "get", name), e.mb.typesInt()), num(e, 1))));
    }

    private static SourceExpr flushPend(EcoMb e) {
        return e.seq(
                e.ifOnly(
                        e.not(e.eq(e.fld(e.v("st"), "pend"), e.s(""))),
                        push(e, e.v("out"), e.cat(e.s("  br label %"), e.fld(e.v("st"), "pend")))),
                e.mb.set(e.fld(e.v("st"), "pend"), e.s("")));
    }

    private static SourceExpr renderTerminator(EcoMb e, SourceExpr... lineParts) {
        return e.seq(push(e, e.v("out"), e.cat(lineParts)), e.mb.set(e.fld(e.v("st"), "open"), num(e, 0)));
    }

    private static SourceExpr irOf(EcoMb e, SourceExpr ty) {
        return e.callM(outer(e), "ir", ty);
    }

    private static SourceExpr typedOf(EcoMb e, SourceExpr lvalTP) {
        return e.callM(outer(e), "typed", lvalTP);
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

    private static SourceExpr literal(EcoMb e, SourceExpr ty, SourceExpr ref) {
        return hf(e, "h_literal", e.p("loc", loc(e)), e.p("ty", ty), e.p("ref", ref));
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
                                                                        "lbinding",
                                                                        e.p("ty", lvTy(e, lvalOf(e, e.v("r")))),
                                                                        e.p(
                                                                                "ptr",
                                                                                lvMem(
                                                                                        e,
                                                                                        lvalOf(e, e.v("r"))))))))))))));
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

    private static SourceExpr whenSt(EcoMb e, String marker, SourceExpr result) {
        return e.ifOnly(has(e, xx(e), e.v("st"), marker), retM(e, result));
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

    private static SourceExpr ifBranchStmts(EcoMb e, String cvVar) {
        var valued = andB(e, cveHas(e, e.v(cvVar)), isLlvm(e, xx(e), e.v(cvVar)));
        return e.seq(
                e.ifOnly(
                        andB(
                                e,
                                cveHas(e, e.v(cvVar)),
                                andB(
                                        e,
                                        e.not(isLlvm(e, xx(e), e.v(cvVar))),
                                        e.not(hf(e, "h_is_unit", e.p("xx", xx(e)), e.p("cv", e.v(cvVar)))))),
                        retM(e, errCve(e, loc(e), "if branches must yield llvm values"))),
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
                                                        e.fld(e.v("st"), "slot"),
                                                        e.callM(e.v("mod"), "slot", cveType(e, e.v("bv")))),
                                                e.mb.set(
                                                        e.fld(e.v("st"), "ty"),
                                                        e.optionTypeLoweredSome(cveType(e, e.v("bv")))),
                                                e.mb.set(e.fld(e.v("st"), "has"), num(e, 1)))),
                                appendCode(e, e.v("out"), cveCode(e, e.v("bv"))),
                                push(e, e.v("out"), storeInst(e, lvalOf(e, e.v("bv")), e.fld(e.v("st"), "slot"))))),
                e.ifOnly(
                        e.not(valued),
                        e.seq(
                                appendCode(e, e.v("out"), cveCode(e, e.v(cvVar))),
                                e.ifOnly(
                                        e.not(cveEnded(e, e.v(cvVar))),
                                        e.mb.set(e.fld(e.v("st"), "novalue"), num(e, 1))))),
                e.ifOnly(
                        e.not(cveEnded(e, e.v(cvVar))),
                        e.seq(
                                push(e, e.v("out"), brInst(e, e.v("end"))),
                                e.mb.set(e.fld(e.v("st"), "ended"), num(e, 0)))));
    }

    private static SourceExpr gval(EcoMb e, SourceExpr cv) {
        return hf(e, "h_gval", e.p("cv", cv), e.p("base", in(e, "base")), e.p("mem", in(e, "mem")));
    }

    private static final IList<String> LGFN_KINDS = IList.of(
            "global_fn",
            "global_const_fn",
            "constant_fn",
            "extern_fn",
            "func_fn",
            "ref_fn",
            "cstr_fn",
            "global_zero_fn");

    private static int lgfnKind(String kind) {
        for (int i = 0; i < LGFN_KINDS.size(); i++) {
            if (LGFN_KINDS.get(i).equals(kind)) return i;
        }
        throw new IllegalArgumentException("no llvm global definition kind " + kind);
    }

    private static SourceExpr lgfnIs(EcoMb e, String kind) {
        return e.eq(in(e, "kind"), num(e, lgfnKind(kind)));
    }

    private static SourceExpr lgfnStaged(EcoMb e, SourceExpr staged) {
        return unwrapCveM(e, constEvalIn(e, xx(e), e.stage(staged)));
    }

    private static SourceExpr lgfnGlobalT(EcoMb e, SourceExpr base, boolean mem) {
        return mk(e, "lt_global", e.p("base", base), e.p("mem", e.b(mem)));
    }

    private static SourceExpr lgfnField(EcoMb e, String key) {
        return unwrapCveM(e, e.callM(e.v("fields"), "get", e.s(key)));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var x = new TList<SourceExpr>();
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        {
            defIface(
                    e,
                    x,
                    "lint",
                    e.p("bits", intT(e)),
                    e.p("signed", boolT(e)),
                    e.p("min", e.typesDouble()),
                    e.p("max", e.typesDouble()));
            defIface(e, x, "lfloat", e.p("bits", intT(e)));
            defIface(e, x, "lptr", e.p("pointee", ty(e)));
            defIface(e, x, "larr", e.p("elem", ty(e)), e.p("count", intT(e)));
            defIface(e, x, "lvoid");
            defIface(e, x, "lslot", e.p("base", ty(e)), e.p("ptr", str(e)));
            defIface(e, x, "lunit_m");
            defIface(e, x, "lrange_m", e.p("elem", ty(e)));
            defIface(e, x, "lloop_m");
            defIface(e, x, "lgtype_m", e.p("base", ty(e)), e.p("mem", boolT(e)));
            defIface(e, x, "lgfn_m", e.p("kind", intT(e)));

            x.add(e.let("lsrc_u", e.callM0(e.v("d"), "union")));
            for (var tag : new String[] {"ssa", "mem"}) {
                x.add(e.let(
                        "ls_" + tag,
                        e.callM(e.v("lsrc_u"), "variant", e.rec(e.p("tag", e.s(tag)), e.p("type_ref", str(e))))));
            }
            defObj(e, x, "lval", "llvm value", e.rec(e.p("ty", ty(e)), e.p("source", t(e, "lsrc_u"))));
            defObj(e, x, "lfield", "llvm field", e.rec(e.p("name", str(e)), e.p("ty", ty(e))));
            defObj(
                    e,
                    x,
                    "ltail",
                    "llvm struct tail",
                    e.rec(
                            e.p("views", e.listType(t(e, "lfield"))),
                            e.p("after", str(e)),
                            e.p("len", e.listType(str(e))),
                            e.p("align", intT(e))));
            defIface(
                    e,
                    x,
                    "lstruct",
                    e.p("name", str(e)),
                    e.p("fields", e.listType(t(e, "lfield"))),
                    e.p("tail", ltailOptT(e)));
            defIface(e, x, "lfn", e.p("params", e.listType(ty(e))), e.p("ret", ty(e)), e.p("varargs", boolT(e)));
            defObj(e, x, "lglobal", "llvm global", e.rec(e.p("name", str(e)), e.p("ty", ty(e)), e.p("init", str(e))));
            defObj(e, x, "llabel", "llvm label", e.rec(e.p("key", e.typesJumpKey()), e.p("name", str(e))));

            x.add(e.let("linst_u", e.callM0(e.v("d"), "union")));
            defVariant(e, x, "label", e.rec(e.p("name", str(e))));
            defVariant(e, x, "br", e.rec(e.p("name", str(e))));
            defVariant(e, x, "condbr", e.rec(e.p("cond", lvalT(e)), e.p("t", str(e)), e.p("f", str(e))));
            defVariant(
                    e,
                    x,
                    "binop",
                    e.rec(e.p("res", str(e)), e.p("op", str(e)), e.p("a", lvalT(e)), e.p("b", lvalT(e))));
            defVariant(
                    e, x, "cmp", e.rec(e.p("res", str(e)), e.p("op", str(e)), e.p("a", lvalT(e)), e.p("b", lvalT(e))));
            defVariant(e, x, "unop", e.rec(e.p("res", str(e)), e.p("op", str(e)), e.p("a", lvalT(e))));
            defVariant(
                    e, x, "conv", e.rec(e.p("res", str(e)), e.p("op", str(e)), e.p("a", lvalT(e)), e.p("to", ty(e))));
            defVariant(e, x, "load", e.rec(e.p("res", str(e)), e.p("ty", ty(e)), e.p("ptr", str(e))));
            defVariant(e, x, "store", e.rec(e.p("val", lvalT(e)), e.p("ptr", str(e))));
            defVariant(
                    e,
                    x,
                    "gep",
                    e.rec(
                            e.p("res", str(e)),
                            e.p("base", ty(e)),
                            e.p("ptr", str(e)),
                            e.p("idx", e.listType(t(e, "lval")))));
            defVariant(
                    e,
                    x,
                    "call",
                    e.rec(
                            e.p("res", e.call(e.mb.typesOption(), str(e))),
                            e.p("fn", lvalT(e)),
                            e.p("args", e.listType(t(e, "lval")))));
            defVariant(e, x, "ret", e.rec(e.p("val", e.call(e.mb.typesOption(), t(e, "lval")))));

            defObj(e, x, "linst", "llvm instruction", e.rec(e.p("k", t(e, "linst_u"))));
            defIface(e, x, "linst_m");
            defIface(e, x, "lglobal_m");
            defIface(e, x, "lmod_m");
            defIface(e, x, "llabel_m");
            defIface(e, x, "lfield_m");
            defObj(e, x, "lcode", "llvm code", e.rec(e.p("insts", e.listType(t(e, "linst")))));
            x.add(e.implInterface(e.v("lcode"), e.mb.builtin("eval", "code_dyn")));
            method(e, x, "lcode", "insts", e.tup(), e.listType(t(e, "linst")), in(e, "insts"));

            defObj(
                    e,
                    x,
                    "lmod",
                    "llvm module",
                    e.rec(
                            e.p("n", intT(e)),
                            e.p("xx", e.call(e.mb.typesOption(), e.typesExprContext())),
                            e.p("structs", e.listType(ty(e))),
                            e.p("struct_lines", e.listType(str(e))),
                            e.p("externs", e.listType(t(e, "lval"))),
                            e.p("globals", e.listType(t(e, "lglobal"))),
                            e.p("constants", e.listType(t(e, "lglobal"))),
                            e.p("slots", e.listType(t(e, "lglobal"))),
                            e.p("funcs", e.listType(str(e))),
                            e.p("libs", e.listType(str(e))),
                            e.p("labels", e.listType(t(e, "llabel")))));
            defObj(e, x, "ltarget", "llvm target", e.rec(e.p("mod", lmodT(e))));
            defObj(e, x, "lirctx", "llvm ir rendering context", e.rec(e.p("xx", xxT(e)), e.p("mod", lmodOptT(e))));
            x.add(e.let("ltype", e.callM0(e.v("d"), "interface")));
            x.add(e.interfaceImpl(e.v("ltype"), true));
            x.add(e.let("ltype_id", e.callM0(e.v("ltype"), "aspect_id")));
            x.add(e.callM(
                    e.v("ltype"),
                    "func",
                    e.rec(e.p("name", e.s("ir")), e.p("arg", t(e, "lirctx")), e.p("ret", e.optionTypeSome(str(e))))));
            x.add(e.callM(
                    e.v("ltype"),
                    "func",
                    e.rec(e.p("name", e.s("ssa")), e.p("arg", e.tup()), e.p("ret", e.optionTypeSome(ty(e))))));
            defObj(e, x, "lbinding", "llvm binding", e.rec(e.p("ty", ty(e)), e.p("ptr", str(e))));
            defObj(e, x, "lbinding_none", "llvm empty binding", e.tup());
            defObj(e, x, "lunit", "llvm unit", e.tup());
            defObj(
                    e,
                    x,
                    "lt_int",
                    "llvm int type",
                    e.rec(
                            e.p("bits", intT(e)),
                            e.p("signed", boolT(e)),
                            e.p("min", e.typesDouble()),
                            e.p("max", e.typesDouble())));
            defObj(e, x, "lt_float", "llvm float type", e.rec(e.p("bits", intT(e))));
            defObj(e, x, "lt_ptr", "llvm pointer type", e.rec(e.p("pointee", ty(e))));
            defObj(e, x, "lt_arr", "llvm array type", e.rec(e.p("elem", ty(e)), e.p("count", intT(e))));
            defObj(
                    e,
                    x,
                    "lt_struct",
                    "llvm struct type",
                    e.rec(e.p("name", str(e)), e.p("fields", e.listType(t(e, "lfield"))), e.p("tail", ltailOptT(e))));
            defObj(
                    e,
                    x,
                    "lt_fn",
                    "llvm function type",
                    e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e)), e.p("varargs", boolT(e))));
            defObj(e, x, "lt_void", "llvm void type", e.tup());
            defObj(e, x, "lt_slot", "llvm slot type", e.rec(e.p("base", ty(e)), e.p("ptr", str(e))));
            defObj(e, x, "lrange_t", "llvm range type", e.rec(e.p("elem", ty(e))));
            defObj(
                    e,
                    x,
                    "lrange",
                    "llvm range",
                    e.rec(e.p("t", ty(e)), e.p("code", e.mb.typesCode()), e.p("n", lvalT(e))));
            defObj(e, x, "lloop_t", "llvm loop type", e.tup());
            defObj(e, x, "lgfn_t", "llvm global definition function", e.rec(e.p("kind", intT(e))));
            defObj(e, x, "lt_global", "llvm global type", e.rec(e.p("base", ty(e)), e.p("mem", boolT(e))));
            defIface(e, x, "lgref_m");
            defObj(
                    e,
                    x,
                    "lgref",
                    "llvm global reference",
                    e.rec(e.p("ref", str(e)), e.p("ty", ty(e)), e.p("mod", lmodT(e))));
            accessor(e, x, "lgref", "ref", str(e));
            accessor(e, x, "lgref", "ty", ty(e));
            defObj(e, x, "lt_gref", "llvm global reference type", e.tup());
        }
        {
            fnDef(e, x, "h_ir", e.rec(e.p("xx", xxT(e)), e.p("mod", lmodOptT(e)), e.p("ty", ty(e))), str(e));
            fnDef(e, x, "h_params_ir", e.rec(e.p("xx", xxT(e)), e.p("mod", lmodOptT(e)), e.p("ty", ty(e))), str(e));
            fnImpl(
                    e,
                    x,
                    "h_ir",
                    body(
                            e,
                            e.let("lk", lookup(e, xx(e), a(e, "ty"), lt(e, "ltype"))),
                            e.ifOnly(
                                    e.not(e.fld(e.v("lk"), "is_some")),
                                    e.mb.panic(e.cat(
                                            e.s("llvm: not an llvm type: "),
                                            bim0(e, e.mb.evalType(), "desc", a(e, "ty"))))),
                            e.ret(bim(
                                    e,
                                    lt(e, "ltype"),
                                    "ir",
                                    e.fld(e.v("lk"), "value"),
                                    mk(e, "lirctx", e.p("xx", xx(e)), e.p("mod", a(e, "mod")))))));
            fnImpl(
                    e,
                    x,
                    "h_params_ir",
                    body(
                            e,
                            e.let("parts", e.listNew(str(e))),
                            e.foreach(
                                    im0(e, "lfn", "params", asp(e, xx(e), a(e, "ty"), "lfn")),
                                    "p",
                                    push(e, e.v("parts"), irFn(e, xx(e), a(e, "mod"), e.v("p")))),
                            e.ifOnly(
                                    im0(e, "lfn", "varargs", asp(e, xx(e), a(e, "ty"), "lfn")),
                                    push(e, e.v("parts"), e.s("..."))),
                            e.ret(e.join(e.v("parts"), ", "))));
            fnDef(e, x, "h_insts", codeT(e), instsT(e));
            fnDef(e, x, "h_code", instsT(e), codeT(e));
            x.add(e.let("h_append_b", e.defFunc(e.v("d"), "llvm.h_append")));
            x.add(e.let("h_append", e.fld(e.v("h_append_b"), "func")));
            e.fnSigs.put("h_append", new SourceExpr[] {
                e.rec(e.p("out", instsT(e)), e.p("code", codeT(e))), e.optionTypeNone(), e.optionTypeNone()
            });
            fnDef(e, x, "h_is_llvm", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), e.mb.typesBool());
            fnDef(
                    e,
                    x,
                    "h_ssa",
                    e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("mod", lmodT(e)), e.p("cv", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_ssa", errsT(e));
            fnDef(e, x, "h_mat", e.rec(e.p("mod", lmodT(e)), e.p("cv", cveT(e))), cveT(e));
            fnDef(
                    e,
                    x,
                    "h_operand",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", lmodT(e)),
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
                            e.p("mod", lmodT(e)),
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
                            e.p("mod", lmodT(e)),
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
                            e.p("mod", lmodT(e)),
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
                            e.p("mod", lmodT(e)),
                            e.p("ty", ty(e)),
                            e.p("op", str(e)),
                            e.p("a", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_unop", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_store_new",
                    e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("mod", lmodT(e)), e.p("cv", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_store_new", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_conv",
                    e.rec(e.p("mod", lmodT(e)), e.p("op", str(e)), e.p("a", cveT(e)), e.p("to", ty(e))),
                    cveT(e));
            fnDef(
                    e,
                    x,
                    "h_gep",
                    e.rec(
                            e.p("mod", lmodT(e)),
                            e.p("base", ty(e)),
                            e.p("ptr", str(e)),
                            e.p("idx", e.listType(t(e, "lval")))),
                    cveT(e));
            fnDef(e, x, "h_key_str", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), str(e));
            fnDef(e, x, "h_retag", e.rec(e.p("mod", lmodT(e)), e.p("cv", cveT(e)), e.p("ty", ty(e))), cveT(e));
            fnDef(e, x, "h_literal", e.rec(e.p("loc", locT(e)), e.p("ty", ty(e)), e.p("ref", str(e))), cveT(e));
            fnDef(
                    e,
                    x,
                    "h_gep_field",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", lmodT(e)),
                            e.p("st", ty(e)),
                            e.p("ptr", str(e)),
                            e.p("name", str(e))),
                    cveT(e));
            fnThrows(e, "h_gep_field", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_tail_view",
                    e.rec(e.p("tail", t(e, "ltail")), e.p("name", str(e))),
                    e.call(e.mb.typesOption(), ty(e)));
            fnDef(
                    e,
                    x,
                    "h_gep_tail",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", lmodT(e)),
                            e.p("st", ty(e)),
                            e.p("ptr", str(e)),
                            e.p("name", str(e))),
                    cveT(e));
            fnThrows(e, "h_gep_tail", errsT(e));
            fnDef(
                    e,
                    x,
                    "h_gep_index",
                    e.rec(
                            e.p("xx", xxT(e)),
                            e.p("loc", locT(e)),
                            e.p("mod", lmodT(e)),
                            e.p("base", ty(e)),
                            e.p("ptr", str(e)),
                            e.p("key", cveT(e))),
                    cveT(e));
            fnThrows(e, "h_gep_index", errsT(e));
            fnDef(e, x, "h_gref", cveT(e), str(e));
            fnDef(e, x, "h_gval", e.rec(e.p("cv", cveT(e)), e.p("base", ty(e)), e.p("mem", boolT(e))), cveT(e));
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
                                            a(e, "mem"),
                                            lvalMem(e, a(e, "base"), e.v("r")),
                                            lvalNew(e, a(e, "base"), e.v("r")))))));

            fnImpl(
                    e,
                    x,
                    "h_insts",
                    body(
                            e,
                            e.ret(
                                    e.matchOf(
                                            e.v("arg"),
                                            "c",
                                            Opt.none(),
                                            e.p("none", instsNew(e)),
                                            e.p(
                                                    "target",
                                                    e.matchOf(
                                                            e.callM(e.v("c"), "cast", lt(e, "lcode")),
                                                            "r",
                                                            Opt.none(),
                                                            e.p("ok", e.callM(e.v("r"), "insts", e.tup())),
                                                            e.p(
                                                                    "errs",
                                                                    e.mb.panic(
                                                                            e.s(
                                                                                    "llvm: codeT(e) belongs to another target")))))))));
            fnImpl(
                    e,
                    x,
                    "h_code",
                    body(e, e.ret(e.mb.as(mkArg(e, "lcode", e.rec(e.p("insts", e.v("arg")))), codeT(e)))));
            fnImpl(
                    e,
                    x,
                    "h_append",
                    body(e, e.foreach(hf(e, "h_insts", a(e, "code")), "i", push(e, a(e, "out"), e.v("i")))));
            fnImpl(
                    e,
                    x,
                    "h_is_llvm",
                    body(
                            e,
                            e.ret(e.ifElse(
                                    cveHas(e, a(e, "cv")),
                                    has(e, xx(e), cveType(e, a(e, "cv")), "ltype"),
                                    e.b(false)))));
            fnImpl(
                    e,
                    x,
                    "h_ssa",
                    body(
                            e,
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "expression has no value")),
                            e.ifOnly(
                                    has(e, xx(e), cveType(e, a(e, "cv")), "lgtype_m"),
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
                                                                                    "lgtype_m",
                                                                                    "base"),
                                                                            a(e, "cv"))))))),
                            e.let("lk", lookup(e, xx(e), cveType(e, a(e, "cv")), lt(e, "ltype"))),
                            e.ifOnly(e.not(e.fld(e.v("lk"), "is_some")), throwErr(e, loc(e), "not an llvm value")),
                            e.let("st", im0(e, "ltype", "ssa", e.fld(e.v("lk"), "value"))),
                            e.let("c", unwrapCveT(e, castValue(e, xx(e), loc(e), e.v("st"), a(e, "cv")))),
                            e.ret(hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", e.v("c"))))));
            fnImpl(
                    e,
                    x,
                    "h_is_unit",
                    body(
                            e,
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), retM(e, dfalse(e))),
                            e.ifOnly(has(e, xx(e), cveType(e, a(e, "cv")), "lunit_m"), retM(e, e.not(dfalse(e)))),
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
                            e.let("lv", lvalOf(e, a(e, "cv"))),
                            e.ifOnly(e.not(lvIsMem(e, e.v("lv"))), retM(e, a(e, "cv"))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, a(e, "cv"))),
                            e.let("r", fresh(e, a(e, "mod"))),
                            push(e, e.v("out"), loadInst(e, e.v("r"), lvTy(e, e.v("lv")), lvMem(e, e.v("lv")))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalNew(e, lvTy(e, e.v("lv")), e.v("r"))))));
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
                            e.let("sl", lvalOf(e, a(e, "self"))),
                            e.ifOnly(e.not(lvIsMem(e, e.v("sl"))), throwErr(e, loc(e), "value is not assignable")),
                            e.let("v", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "value"))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, a(e, "self"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                            push(e, e.v("out"), storeInst(e, lvalOf(e, e.v("v")), lvMem(e, e.v("sl")))),
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
                            e.let("r", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "binop",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", lvalOf(e, e.v("a2"))),
                                            e.p("b", lvalOf(e, e.v("b2"))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalNew(e, a(e, "res"), e.v("r"))))));
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
                            e.let("r", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "cmp",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", lvalOf(e, e.v("a2"))),
                                            e.p("b", lvalOf(e, e.v("b2"))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalNew(e, intTy(e, "i1"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_unop",
                    body(
                            e,
                            e.let("out", instsNew(e)),
                            e.let("a2", operand(e, xx(e), loc(e), a(e, "mod"), a(e, "ty"), a(e, "a"))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("a2"))),
                            e.let("r", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "unop",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", lvalOf(e, e.v("a2"))))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalNew(e, a(e, "ty"), e.v("r"))))));
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
                            push(e, e.v("out"), storeInst(e, lvalOf(e, e.v("v")), e.v("s"))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalMem(e, e.v("ty"), e.v("s"))))));
            fnImpl(
                    e,
                    x,
                    "h_conv",
                    body(
                            e,
                            e.let("a0", hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", a(e, "a")))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("a0"))),
                            e.let("r", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "conv",
                                            e.p("res", e.v("r")),
                                            e.p("op", a(e, "op")),
                                            e.p("a", lvalOf(e, e.v("a0"))),
                                            e.p("to", a(e, "to")))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalNew(e, a(e, "to"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_gep",
                    body(
                            e,
                            e.let("out", instsNew(e)),
                            e.let("r", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "gep",
                                            e.p("res", e.v("r")),
                                            e.p("base", a(e, "base")),
                                            e.p("ptr", a(e, "ptr")),
                                            e.p("idx", a(e, "idx")))),
                            e.ret(cvOf(
                                    e,
                                    codeOf(e, e.v("out")),
                                    lvalNew(e, mk(e, "lt_ptr", e.p("pointee", a(e, "base"))), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_key_str",
                    body(
                            e,
                            e.ret(e.call(
                                    e.evalBuiltin("constkey_to_str"),
                                    e.call(
                                            e.evalBuiltin("extract_constkey"),
                                            e.rec(e.p("xx", xx(e)), e.p("value", cveValue(e, a(e, "cv")))))))));
            fnImpl(
                    e,
                    x,
                    "h_retag",
                    body(
                            e,
                            e.let("m", hf(e, "h_mat", e.p("mod", a(e, "mod")), e.p("cv", a(e, "cv")))),
                            e.ret(cvOf(
                                    e, cveCode(e, e.v("m")), lvalNew(e, a(e, "ty"), lvRef(e, lvalOf(e, e.v("m"))))))));
            fnImpl(
                    e,
                    x,
                    "h_literal",
                    body(e, e.ret(cvOf(e, codeNone(e, loc(e)), lvalNew(e, a(e, "ty"), a(e, "ref"))))));
            fnImpl(
                    e,
                    x,
                    "h_gep_field",
                    body(
                            e,
                            e.let("fields", acc(e, xx(e), a(e, "st"), "lstruct", "fields")),
                            e.let(
                                    "st",
                                    e.rec(
                                            e.p("i", e.intOf(0)),
                                            e.p("idx", e.intOf(-1)),
                                            e.p("fty", e.optionTypeLoweredNone()))),
                            e.foreach(
                                    e.v("fields"),
                                    "f0",
                                    e.seq(
                                            e.let("f", e.v("f0")),
                                            e.ifOnly(
                                                    e.eq(e.callM(e.v("f"), "name", e.tup()), a(e, "name")),
                                                    e.seq(
                                                            e.mb.set(e.fld(e.v("st"), "idx"), e.fld(e.v("st"), "i")),
                                                            e.mb.set(
                                                                    e.fld(e.v("st"), "fty"),
                                                                    e.optionTypeLoweredSome(
                                                                            e.callM(e.v("f"), "ty", e.tup()))))),
                                            e.incr(e.fld(e.v("st"), "i")))),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("st"), "idx"), num(e, -1)),
                                    retM(
                                            e,
                                            hf(
                                                    e,
                                                    "h_gep_tail",
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("mod", a(e, "mod")),
                                                    e.p("st", a(e, "st")),
                                                    e.p("ptr", a(e, "ptr")),
                                                    e.p("name", a(e, "name"))))),
                            e.let("tailo", acc(e, xx(e), a(e, "st"), "lstruct", "tail")),
                            e.ifOnly(
                                    andB(
                                            e,
                                            e.fld(e.v("tailo"), "is_some"),
                                            orB(
                                                    e,
                                                    e.eq(a(e, "name"), e.s("next")),
                                                    e.fld(
                                                            hf(
                                                                    e,
                                                                    "h_tail_view",
                                                                    e.p("tail", e.fld(e.v("tailo"), "value")),
                                                                    e.p("name", a(e, "name"))),
                                                            "is_some"))),
                                    throwErr(
                                            e,
                                            loc(e),
                                            e.cat(
                                                    e.s("struct field "),
                                                    a(e, "name"),
                                                    e.s(" is ambiguous with the struct tail")))),
                            e.let("i32", intTy(e, "i32")),
                            e.let(
                                    "g",
                                    hf(
                                            e,
                                            "h_gep",
                                            e.p("mod", a(e, "mod")),
                                            e.p("base", a(e, "st")),
                                            e.p("ptr", a(e, "ptr")),
                                            e.p(
                                                    "idx",
                                                    e.listNew(
                                                            lt(e, "lval"),
                                                            lvalNew(e, e.v("i32"), e.s("0")),
                                                            lvalNew(
                                                                    e,
                                                                    e.v("i32"),
                                                                    toStr(e, e.fld(e.v("st"), "idx"))))))),
                            e.let("r", lvRef(e, lvalOf(e, e.v("g")))),
                            e.ret(cvOf(
                                    e, cveCode(e, e.v("g")), lvalMem(e, e.fld(e.v("st"), "fty", "value"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_gep_index",
                    body(
                            e,
                            e.let("out", instsNew(e)),
                            e.let(
                                    "k",
                                    e.ifElse(
                                            isLlvm(e, xx(e), a(e, "key")),
                                            ssa(e, xx(e), loc(e), a(e, "mod"), a(e, "key")),
                                            unwrapCveT(e, castValue(e, xx(e), loc(e), intTy(e, "i32"), a(e, "key"))))),
                            e.ifOnly(
                                    e.not(has(e, xx(e), cveType(e, e.v("k")), "lint")),
                                    throwErr(e, loc(e), "index must be an integer")),
                            appendCode(e, e.v("out"), cveCode(e, e.v("k"))),
                            e.let(
                                    "g",
                                    hf(
                                            e,
                                            "h_gep",
                                            e.p("mod", a(e, "mod")),
                                            e.p("base", a(e, "base")),
                                            e.p("ptr", a(e, "ptr")),
                                            e.p("idx", e.listNew(lt(e, "lval"), lvalOf(e, e.v("k")))))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                            e.let("r", lvRef(e, lvalOf(e, e.v("g")))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalMem(e, a(e, "base"), e.v("r"))))));
            fnImpl(
                    e,
                    x,
                    "h_tail_view",
                    body(
                            e,
                            e.foreach(
                                    e.callM(a(e, "tail"), "views", e.tup()),
                                    "v0",
                                    e.seq(
                                            e.let("v", e.v("v0")),
                                            e.ifOnly(
                                                    e.eq(e.callM(e.v("v"), "name", e.tup()), a(e, "name")),
                                                    retM(
                                                            e,
                                                            e.optionTypeLoweredSome(
                                                                    e.callM(e.v("v"), "ty", e.tup())))))),
                            e.ret(e.optionTypeLoweredNone())));
            fnImpl(
                    e,
                    x,
                    "h_gep_tail",
                    body(
                            e,
                            e.let("tailo", acc(e, xx(e), a(e, "st"), "lstruct", "tail")),
                            e.ifOnly(
                                    e.not(e.fld(e.v("tailo"), "is_some")),
                                    throwErr(e, loc(e), e.cat(e.s("no struct field named "), a(e, "name")))),
                            e.let("tl", e.fld(e.v("tailo"), "value")),
                            e.let("out", instsNew(e)),
                            e.let("i64", intTy(e, "i64")),
                            e.ifOnly(
                                    e.not(e.eq(a(e, "name"), e.s("next"))),
                                    e.seq(
                                            e.let(
                                                    "vo",
                                                    hf(
                                                            e,
                                                            "h_tail_view",
                                                            e.p("tail", e.v("tl")),
                                                            e.p("name", a(e, "name")))),
                                            e.ifOnly(
                                                    e.not(e.fld(e.v("vo"), "is_some")),
                                                    throwErr(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("no struct field or tail view named "),
                                                                    a(e, "name")))),
                                            e.let("rv", fresh(e, a(e, "mod"))),
                                            push(
                                                    e,
                                                    e.v("out"),
                                                    inst(
                                                            e,
                                                            "gep",
                                                            e.p("res", e.v("rv")),
                                                            e.p("base", a(e, "st")),
                                                            e.p("ptr", a(e, "ptr")),
                                                            e.p(
                                                                    "idx",
                                                                    e.listNew(
                                                                            lt(e, "lval"),
                                                                            lvalNew(e, e.v("i64"), e.s("1")))))),
                                            retM(
                                                    e,
                                                    cvOf(
                                                            e,
                                                            codeOf(e, e.v("out")),
                                                            lvalNew(
                                                                    e,
                                                                    mk(
                                                                            e,
                                                                            "lt_ptr",
                                                                            e.p("pointee", e.fld(e.v("vo"), "value"))),
                                                                    e.v("rv")))))),
                            e.let("fields", acc(e, xx(e), a(e, "st"), "lstruct", "fields")),
                            e.let("after", e.callM(e.v("tl"), "after", e.tup())),
                            e.let(
                                    "fi",
                                    e.rec(
                                            e.p("i", e.intOf(0)),
                                            e.p("idx", e.intOf(-1)),
                                            e.p("fty", e.optionTypeLoweredNone()))),
                            e.foreach(
                                    e.v("fields"),
                                    "f0",
                                    e.seq(
                                            e.let("f", e.v("f0")),
                                            e.ifOnly(
                                                    e.eq(e.callM(e.v("f"), "name", e.tup()), e.v("after")),
                                                    e.seq(
                                                            e.mb.set(e.fld(e.v("fi"), "idx"), e.fld(e.v("fi"), "i")),
                                                            e.mb.set(
                                                                    e.fld(e.v("fi"), "fty"),
                                                                    e.optionTypeLoweredSome(
                                                                            e.callM(e.v("f"), "ty", e.tup()))))),
                                            e.incr(e.fld(e.v("fi"), "i")))),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("fi"), "idx"), num(e, -1)),
                                    throwErr(e, loc(e), e.cat(e.s("struct tail: no field named "), e.v("after")))),
                            e.let("aty", e.fld(e.v("fi"), "fty", "value")),
                            e.let("i32", intTy(e, "i32")),
                            e.let("ra", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "gep",
                                            e.p("res", e.v("ra")),
                                            e.p("base", a(e, "st")),
                                            e.p("ptr", a(e, "ptr")),
                                            e.p(
                                                    "idx",
                                                    e.listNew(
                                                            lt(e, "lval"),
                                                            lvalNew(e, e.v("i32"), e.s("0")),
                                                            lvalNew(
                                                                    e,
                                                                    e.v("i32"),
                                                                    toStr(e, e.fld(e.v("fi"), "idx"))))))),
                            e.let("re", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "gep",
                                            e.p("res", e.v("re")),
                                            e.p("base", e.v("aty")),
                                            e.p("ptr", e.v("ra")),
                                            e.p("idx", e.listNew(lt(e, "lval"), lvalNew(e, e.v("i64"), e.s("1")))))),
                            e.let(
                                    "cur",
                                    e.rec(e.p("ty", e.optionTypeLoweredSome(a(e, "st"))), e.p("ptr", a(e, "ptr")))),
                            e.foreach(
                                    e.callM(e.v("tl"), "len", e.tup()),
                                    "seg0",
                                    e.seq(
                                            e.let("seg", e.v("seg0")),
                                            e.ifOnly(
                                                    e.not(has(e, xx(e), e.fld(e.v("cur"), "ty", "value"), "lstruct")),
                                                    throwErr(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("struct tail length path: not a struct at "),
                                                                    e.v("seg")))),
                                            e.let(
                                                    "g",
                                                    hf(
                                                            e,
                                                            "h_gep_field",
                                                            e.p("xx", xx(e)),
                                                            e.p("loc", loc(e)),
                                                            e.p("mod", a(e, "mod")),
                                                            e.p("st", e.fld(e.v("cur"), "ty", "value")),
                                                            e.p("ptr", e.fld(e.v("cur"), "ptr")),
                                                            e.p("name", e.v("seg")))),
                                            appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                                            e.let("gl", lvalOf(e, e.v("g"))),
                                            e.mb.set(
                                                    e.fld(e.v("cur"), "ty"),
                                                    e.optionTypeLoweredSome(lvTy(e, e.v("gl")))),
                                            e.mb.set(e.fld(e.v("cur"), "ptr"), lvMem(e, e.v("gl"))))),
                            e.let("lty", e.fld(e.v("cur"), "ty", "value")),
                            e.ifOnly(
                                    e.not(has(e, xx(e), e.v("lty"), "lint")),
                                    throwErr(e, loc(e), "struct tail length must be an integer field")),
                            e.let("rl", fresh(e, a(e, "mod"))),
                            push(e, e.v("out"), loadInst(e, e.v("rl"), e.v("lty"), e.fld(e.v("cur"), "ptr"))),
                            e.let("len", e.rec(e.p("ref", e.v("rl")))),
                            e.ifOnly(
                                    e.mb.operatorLt(acc(e, xx(e), e.v("lty"), "lint", "bits"), num(e, 64)),
                                    e.seq(
                                            e.let("rz", fresh(e, a(e, "mod"))),
                                            push(
                                                    e,
                                                    e.v("out"),
                                                    inst(
                                                            e,
                                                            "conv",
                                                            e.p("res", e.v("rz")),
                                                            e.p("op", e.s("zext")),
                                                            e.p("a", lvalNew(e, e.v("lty"), e.v("rl"))),
                                                            e.p("to", e.v("i64")))),
                                            e.mb.set(e.fld(e.v("len"), "ref"), e.v("rz")))),
                            e.let("pi", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "conv",
                                            e.p("res", e.v("pi")),
                                            e.p("op", e.s("ptrtoint")),
                                            e.p(
                                                    "a",
                                                    lvalNew(
                                                            e,
                                                            mk(e, "lt_ptr", e.p("pointee", a(e, "st"))),
                                                            a(e, "ptr"))),
                                            e.p("to", e.v("i64")))),
                            e.let("ei", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "conv",
                                            e.p("res", e.v("ei")),
                                            e.p("op", e.s("ptrtoint")),
                                            e.p(
                                                    "a",
                                                    lvalNew(e, mk(e, "lt_ptr", e.p("pointee", e.v("aty"))), e.v("re"))),
                                            e.p("to", e.v("i64")))),
                            e.let("align", e.callM(e.v("tl"), "align", e.tup())),
                            e.let("off", fresh(e, a(e, "mod"))),
                            instBinopI64(e, "off", "sub", e.v("ei"), e.v("pi")),
                            e.let("tot", fresh(e, a(e, "mod"))),
                            instBinopI64(e, "tot", "add", e.v("off"), e.fld(e.v("len"), "ref")),
                            e.let("up", fresh(e, a(e, "mod"))),
                            instBinopI64(
                                    e, "up", "add", e.v("tot"), toStr(e, e.mb.operatorSub(e.v("align"), e.intOf(1)))),
                            e.let("al", fresh(e, a(e, "mod"))),
                            instBinopI64(
                                    e, "al", "and", e.v("up"), toStr(e, e.mb.operatorSub(e.intOf(0), e.v("align")))),
                            e.let("nx", fresh(e, a(e, "mod"))),
                            push(
                                    e,
                                    e.v("out"),
                                    inst(
                                            e,
                                            "gep",
                                            e.p("res", e.v("nx")),
                                            e.p("base", intTy(e, "i8")),
                                            e.p("ptr", a(e, "ptr")),
                                            e.p("idx", e.listNew(lt(e, "lval"), lvalNew(e, e.v("i64"), e.v("al")))))),
                            e.ret(cvOf(
                                    e,
                                    codeOf(e, e.v("out")),
                                    lvalNew(e, mk(e, "lt_ptr", e.p("pointee", a(e, "st"))), e.v("nx"))))));
        }
        {
            fnDef(e, x, "c_ptr", ty(e), ty(e));
            fnImpl(e, x, "c_ptr", body(e, e.ret(mk(e, "lt_ptr", e.p("pointee", e.v("arg"))))));
            fnDef(e, x, "c_array", e.rec(e.p("elem", ty(e)), e.p("count", intT(e))), ty(e));
            fnImpl(
                    e,
                    x,
                    "c_array",
                    body(e, e.ret(mk(e, "lt_arr", e.p("elem", a(e, "elem")), e.p("count", a(e, "count"))))));
            fnDef(e, x, "c_field", e.rec(e.p("name", str(e)), e.p("ty", ty(e))), t(e, "lfield"));
            fnImpl(e, x, "c_field", body(e, e.ret(mk(e, "lfield", e.p("name", a(e, "name")), e.p("ty", a(e, "ty"))))));
            fnDef(e, x, "c_struct", e.rec(e.p("name", str(e)), e.p("fields", e.listType(t(e, "lfield")))), ty(e));
            fnImpl(
                    e,
                    x,
                    "c_struct",
                    body(
                            e,
                            e.ret(mk(
                                    e,
                                    "lt_struct",
                                    e.p("name", a(e, "name")),
                                    e.p("fields", a(e, "fields")),
                                    e.p("tail", e.optNone(lt(e, "ltail")))))));
            fnDef(
                    e,
                    x,
                    "c_tail",
                    e.rec(
                            e.p("views", e.listType(t(e, "lfield"))),
                            e.p("after", str(e)),
                            e.p("len", e.listType(str(e))),
                            e.p("align", intT(e))),
                    t(e, "ltail"));
            fnImpl(
                    e,
                    x,
                    "c_tail",
                    body(
                            e,
                            e.ret(mk(
                                    e,
                                    "ltail",
                                    e.p("views", a(e, "views")),
                                    e.p("after", a(e, "after")),
                                    e.p("len", a(e, "len")),
                                    e.p("align", a(e, "align"))))));
            fnDef(
                    e,
                    x,
                    "c_struct_tail",
                    e.rec(e.p("name", str(e)), e.p("fields", e.listType(t(e, "lfield"))), e.p("tail", t(e, "ltail"))),
                    ty(e));
            fnImpl(
                    e,
                    x,
                    "c_struct_tail",
                    body(
                            e,
                            e.ret(mk(
                                    e,
                                    "lt_struct",
                                    e.p("name", a(e, "name")),
                                    e.p("fields", a(e, "fields")),
                                    e.p("tail", e.optSome(lt(e, "ltail"), a(e, "tail")))))));
            for (var varargs : new boolean[] {false, true}) {
                var name = varargs ? "c_fn_varargs" : "c_fn";
                fnDef(e, x, name, e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e))), ty(e));
                fnImpl(
                        e,
                        x,
                        name,
                        body(
                                e,
                                e.ret(mk(
                                        e,
                                        "lt_fn",
                                        e.p("params", a(e, "params")),
                                        e.p("ret", a(e, "ret")),
                                        e.p("varargs", e.b(varargs))))));
            }
            fnDef(e, x, "c_range", ty(e), ty(e));
            fnDef(
                    e,
                    x,
                    "h_range_call",
                    e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("elem", ty(e)), e.p("cv", cveT(e))),
                    e.call(e.mb.typesEresult(), cveT(e)));
            fnImpl(
                    e,
                    x,
                    "h_range_call",
                    body(
                            e,
                            e.let(
                                    "a2",
                                    unwrapCveM(
                                            e,
                                            tryEr(
                                                    e,
                                                    operand(e, xx(e), loc(e), xmod(e), a(e, "elem"), a(e, "cv")),
                                                    cveT(e)))),
                            e.ret(okCve(
                                    e,
                                    cvOf(
                                            e,
                                            codeNone(e, loc(e)),
                                            mk(
                                                    e,
                                                    "lrange",
                                                    e.p("t", mk(e, "lrange_t", e.p("elem", a(e, "elem")))),
                                                    e.p("code", cveCode(e, e.v("a2"))),
                                                    e.p("n", lvalOf(e, e.v("a2")))))))));
            fnImpl(
                    e,
                    x,
                    "c_range",
                    body(
                            e,
                            e.let("elem", e.v("arg")),
                            e.let("h_range_call_f", e.lowerV("h_range_call")),
                            e.let("cf_d", e.define()),
                            e.let("cf", e.callM(e.v("cf_d"), "const_func", e.s("llvm range"))),
                            e.callM(
                                    e.v("cf"),
                                    "impl",
                                    e.rec(
                                            e.p(
                                                    "impl_body",
                                                    e.stage(
                                                            e.call(
                                                                    e.lower(e.v("h_range_call_f")),
                                                                    e.rec(
                                                                            e.p("xx", xx(e)),
                                                                            e.p("loc", loc(e)),
                                                                            e.p("elem", e.lower(e.v("elem"))),
                                                                            e.p("cv", a(e, "argument")))))),
                                            e.p("exception_type", e.optionTypeLoweredNone()),
                                            e.p("description", e.optNone(str(e))))),
                            e.finish(e.v("cf_d")),
                            e.ret(e.mb.as(e.fld(e.v("cf"), "func"), ty(e)))));
            fnDef(e, x, "c_global_t", e.rec(e.p("ty", ty(e)), e.p("mem", boolT(e))), ty(e));
            fnImpl(
                    e,
                    x,
                    "c_global_t",
                    body(e, e.ret(mk(e, "lt_global", e.p("base", a(e, "ty")), e.p("mem", a(e, "mem"))))));
        }
        {
            for (var name :
                    new String[] {"h_init", "h_init_int", "h_init_float", "h_init_ptr", "h_init_arr", "h_init_struct"
                    }) {
                fnDef(
                        e,
                        x,
                        name,
                        e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("ty", ty(e)), e.p("cv", cveT(e))),
                        cveT(e));
                fnThrows(e, name, errsT(e));
            }
            fnImpl(
                    e,
                    x,
                    "h_init",
                    body(
                            e,
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "initializer has no value")),
                            e.ifOnly(has(e, xx(e), a(e, "ty"), "lint"), retM(e, hf(e, "h_init_int", e.v("arg")))),
                            e.ifOnly(has(e, xx(e), a(e, "ty"), "lfloat"), retM(e, hf(e, "h_init_float", e.v("arg")))),
                            e.ifOnly(has(e, xx(e), a(e, "ty"), "lptr"), retM(e, hf(e, "h_init_ptr", e.v("arg")))),
                            e.ifOnly(has(e, xx(e), a(e, "ty"), "larr"), retM(e, hf(e, "h_init_arr", e.v("arg")))),
                            e.ifOnly(
                                    e.not(has(e, xx(e), a(e, "ty"), "lstruct")),
                                    throwErr(
                                            e,
                                            loc(e),
                                            e.cat(e.s("llvm "), tyDesc(e, a(e, "ty")), e.s(" cannot be initialized")))),
                            e.ret(hf(e, "h_init_struct", e.v("arg")))));
            SourceExpr anyHostInt = dfalse(e);
            var hostIntFits = new TList<SourceExpr>();
            for (var host : HOST_INTS) {
                var isHost = isEqual(e, xx(e), e.mb.builtin("types", host.name()), e.v("st"));
                anyHostInt = orB(e, isHost, anyHostInt);
                hostIntFits.add(e.ifOnly(
                        andB(
                                e,
                                isHost,
                                orB(
                                        e,
                                        e.mb.operatorLt(e.n(host.min()), acc(e, xx(e), a(e, "ty"), "lint", "min")),
                                        e.mb.operatorGt(e.n(host.max()), acc(e, xx(e), a(e, "ty"), "lint", "max")))),
                        throwErr(
                                e,
                                loc(e),
                                e.cat(
                                        e.s("host " + host.name() + " values do not fit llvm "),
                                        tyDesc(e, a(e, "ty"))))));
            }
            fnImpl(
                    e,
                    x,
                    "h_init_int",
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
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
                                                    throwErr(e, loc(e), "integer initializer is not integral")),
                                            e.ifOnly(
                                                    orB(
                                                            e,
                                                            e.mb.operatorLt(
                                                                    e.v("d"), acc(e, xx(e), a(e, "ty"), "lint", "min")),
                                                            e.mb.operatorGt(
                                                                    e.v("d"),
                                                                    acc(e, xx(e), a(e, "ty"), "lint", "max"))),
                                                    throwErr(e, loc(e), "integer initializer out of range")),
                                            e.let("s", toStr(e, e.v("l"))),
                                            retM(e, strCve(e, e.v("s"))))),
                            e.ifOnly(e.not(anyHostInt), initMismatch(e, e.v("st"))),
                            e.ifOnly(
                                    andB(
                                            e,
                                            isEqual(e, xx(e), e.mb.builtin("types", "byte"), e.v("st")),
                                            e.eq(acc(e, xx(e), a(e, "ty"), "lint", "bits"), num(e, 8))),
                                    retM(
                                            e,
                                            hostCve(
                                                    e,
                                                    e.stage(
                                                            toStr(
                                                                    e,
                                                                    e.mb.operatorAnd(
                                                                            e.mb.as(
                                                                                    graft(e, a(e, "cv")),
                                                                                    e.mb.typesInt()),
                                                                            num(e, 255))))))),
                            e.seq(hostIntFits.toArray(SourceExpr[]::new)),
                            e.ret(hostCve(e, e.stage(toStr(e, graft(e, a(e, "cv"))))))));
            fnImpl(
                    e,
                    x,
                    "h_init_float",
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.let("bits", acc(e, xx(e), a(e, "ty"), "lfloat", "bits")),
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
                                                            e.eq(e.v("bits"), num(e, 64)),
                                                            toStr(e, e.v("d")),
                                                            toStr(
                                                                    e,
                                                                    e.mb.as(
                                                                            e.mb.as(e.v("d"), e.typesFloat()),
                                                                            e.typesDouble())))),
                                            retM(e, strCve(e, e.v("s"))))),
                            e.ifOnly(e.not(isEqual(e, xx(e), e.typesDouble(), e.v("st"))), initMismatch(e, e.v("st"))),
                            e.ifOnly(
                                    e.eq(e.v("bits"), num(e, 64)),
                                    retM(e, hostCve(e, e.stage(toStr(e, graft(e, a(e, "cv"))))))),
                            e.ret(hostCve(
                                    e,
                                    e.stage(toStr(
                                            e,
                                            e.mb.as(
                                                    e.mb.as(graft(e, a(e, "cv")), e.typesFloat()),
                                                    e.typesDouble())))))));
            fnImpl(
                    e,
                    x,
                    "h_init_ptr",
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(
                                    has(e, xx(e), e.v("st"), "lgtype_m"),
                                    e.seq(
                                            e.ifOnly(
                                                    e.not(isRuntime(e, a(e, "cv"))),
                                                    e.seq(
                                                            e.let("s", hf(e, "h_gref", a(e, "cv"))),
                                                            retM(e, strCve(e, e.v("s"))))),
                                            retM(e, retagRuntime(e, a(e, "cv"), str(e))))),
                            e.ifOnly(
                                    has(e, xx(e), e.v("st"), "lgref_m"),
                                    e.seq(
                                            e.let("lgref_ty", lt(e, "lgref")),
                                            e.let("pointee", acc(e, xx(e), a(e, "ty"), "lptr", "pointee")),
                                            retM(
                                                    e,
                                                    hostCve(
                                                            e,
                                                            e.stage(
                                                                    e.callM(
                                                                            e.call(
                                                                                    e.mb.mustReinterpret(),
                                                                                    e.tup(
                                                                                            graft(e, a(e, "cv")),
                                                                                            e.lower(e.v("lgref_ty")))),
                                                                            "ptr_ref",
                                                                            e.lower(e.v("pointee")))))))),
                            initMismatch(e, e.v("st"))));
            var hostListInits = new TList<SourceExpr>();
            defObj(e, x, "llist", "llvm list initializer", e.rec(e.p("ir", str(e)), e.p("count", intT(e))));
            accessor(e, x, "llist", "ir", str(e));
            accessor(e, x, "llist", "count", intT(e));
            for (var prim : LLVM_PRIMS) {
                var fmt = "h_fmt_list_" + prim.llvm();
                var fn = "h_init_list_" + prim.llvm();
                var hostT = e.mb.builtin("types", prim.host());
                fnDef(e, x, fmt, e.listType(hostT), t(e, "llist"));
                fnImpl(
                        e,
                        x,
                        fmt,
                        body(
                                e,
                                e.let("parts", e.listNew(str(e))),
                                countLet(e, "n"),
                                e.foreach(
                                        e.v("arg"),
                                        "v",
                                        e.seq(
                                                push(
                                                        e,
                                                        e.v("parts"),
                                                        e.cat(
                                                                e.s(prim.ir() + " "),
                                                                toStr(
                                                                        e,
                                                                        prim.host()
                                                                                        .equals("float")
                                                                                ? e.mb.as(e.v("v"), e.typesDouble())
                                                                                : e.v("v")))),
                                                e.incr(e.fld(e.v("n"), "v")))),
                                e.ret(mk(
                                        e,
                                        "llist",
                                        e.p("ir", e.cat(e.s("["), e.join(e.v("parts"), ", "), e.s("]"))),
                                        e.p("count", e.fld(e.v("n"), "v"))))));
                fnDef(e, x, fn, e.rec(e.p("count", intT(e)), e.p("values", e.listType(hostT))), str(e));
                fnImpl(
                        e,
                        x,
                        fn,
                        body(
                                e,
                                e.let("list", hf(e, fmt, a(e, "values"))),
                                e.ifOnly(
                                        e.not(e.eq(e.callM(e.v("list"), "count", e.tup()), a(e, "count"))),
                                        e.mb.panic(e.cat(
                                                e.s("llvm: array initializer has "),
                                                toStr(e, e.callM(e.v("list"), "count", e.tup())),
                                                e.s(" elements, expected "),
                                                toStr(e, a(e, "count"))))),
                                e.ret(e.callM(e.v("list"), "ir", e.tup()))));
                var isElem = isPrim(e, xx(e), e.v("elem"), prim);
                hostListInits.add(e.ifOnly(
                        isElem,
                        e.seq(
                                e.let(
                                        "values",
                                        unwrapCveT(e, castValue(e, xx(e), loc(e), e.listType(hostT), a(e, "cv")))),
                                e.let("init_list", e.lowerV(fn)),
                                retM(
                                        e,
                                        hostCve(
                                                e,
                                                e.stage(e.call(
                                                        e.lower(e.v("init_list")),
                                                        e.rec(
                                                                e.p("count", e.lower(e.v("count"))),
                                                                e.p("values", graft(e, e.v("values")))))))))));
            }
            fnImpl(
                    e,
                    x,
                    "h_init_arr",
                    body(
                            e,
                            e.let("elem", acc(e, xx(e), a(e, "ty"), "larr", "elem")),
                            e.let("count", acc(e, xx(e), a(e, "ty"), "larr", "count")),
                            e.let("eir", irFn(e, xx(e), e.optNone(lt(e, "lmod")), e.v("elem"))),
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.seq(hostListInits.toArray(SourceExpr[]::new)),
                            e.let("tk", lookup(e, xx(e), e.v("st"), aspIface(e, "tuple"))),
                            e.ifOnly(
                                    e.fld(e.v("tk"), "is_some"),
                                    e.seq(
                                            e.let("parts", e.listNew(cveT(e))),
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
                                                    e.seq(
                                                            e.let(
                                                                    "v",
                                                                    initFn(e, e.v("elem"), unwrapCveT(e, e.v("er")))),
                                                            push(
                                                                    e,
                                                                    e.v("parts"),
                                                                    hostCve(
                                                                            e,
                                                                            e.stage(
                                                                                    e.cat(
                                                                                            e.lower(e.v("eir")),
                                                                                            e.s(" "),
                                                                                            graft(e, e.v("v")))))))),
                                            countLet(e, "n"),
                                            countLoop(e, "n", e.v("parts")),
                                            e.ifOnly(
                                                    e.not(e.eq(e.fld(e.v("n"), "v"), e.v("count"))),
                                                    throwErr(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s("array initializer has "),
                                                                    toStr(e, e.fld(e.v("n"), "v")),
                                                                    e.s(" elements, expected "),
                                                                    toStr(e, e.v("count"))))),
                                            retM(e, foldJoin(e, e.v("parts"), "[", "]")))),
                            initMismatch(e, e.v("st"))));
            fnImpl(
                    e,
                    x,
                    "h_init_struct",
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.let("rk", lookup(e, xx(e), e.v("st"), aspIface(e, "record"))),
                            e.ifOnly(e.not(e.fld(e.v("rk"), "is_some")), initMismatch(e, e.v("st"))),
                            e.let("sfields", acc(e, xx(e), a(e, "ty"), "lstruct", "fields")),
                            e.let("kc", e.rec(e.p("n", e.intOf(0)), e.p("bad", e.dstr("")))),
                            e.foreach(
                                    bim0(e, aspIface(e, "record"), "field_types", e.fld(e.v("rk"), "value")),
                                    "en",
                                    e.seq(
                                            e.incr(e.fld(e.v("kc"), "n")),
                                            e.let("ks", keyStr(e, e.fld(e.v("en"), "key"))),
                                            e.let("hit", e.rec(e.p("v", e.intOf(0)))),
                                            e.foreach(
                                                    e.v("sfields"),
                                                    "f0",
                                                    e.ifOnly(
                                                            e.eq(e.callM(e.v("f0"), "name", e.tup()), e.v("ks")),
                                                            e.mb.set(e.fld(e.v("hit"), "v"), num(e, 1)))),
                                            e.ifOnly(
                                                    e.eq(e.fld(e.v("hit"), "v"), num(e, 0)),
                                                    e.mb.set(e.fld(e.v("kc"), "bad"), e.v("ks"))))),
                            e.ifOnly(
                                    e.not(e.eq(e.fld(e.v("kc"), "bad"), e.s(""))),
                                    throwErr(
                                            e,
                                            loc(e),
                                            e.cat(
                                                    e.s("llvm "),
                                                    tyDesc(e, a(e, "ty")),
                                                    e.s(" has no field "),
                                                    e.fld(e.v("kc"), "bad")))),
                            countLet(e, "sn"),
                            countLoop(e, "sn", e.v("sfields")),
                            e.ifOnly(
                                    e.not(e.eq(e.fld(e.v("kc"), "n"), e.fld(e.v("sn"), "v"))),
                                    throwErr(
                                            e,
                                            loc(e),
                                            e.cat(
                                                    e.s("initializer for llvm "),
                                                    tyDesc(e, a(e, "ty")),
                                                    e.s(" is missing fields")))),
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
                                                    e.p("value", cveValue(e, a(e, "cv")))))),
                            e.let("parts", e.listNew(cveT(e))),
                            e.foreach(
                                    e.v("sfields"),
                                    "f0",
                                    e.seq(
                                            e.let("f", e.v("f0")),
                                            e.let("fty", e.callM(e.v("f"), "ty", e.tup())),
                                            e.let("fir", irFn(e, xx(e), e.optNone(lt(e, "lmod")), e.v("fty"))),
                                            e.let(
                                                    "v",
                                                    initFn(
                                                            e,
                                                            e.v("fty"),
                                                            unwrapCveT(
                                                                    e,
                                                                    e.callM(
                                                                            e.v("fields"),
                                                                            "get",
                                                                            e.callM(e.v("f"), "name", e.tup()))))),
                                            push(
                                                    e,
                                                    e.v("parts"),
                                                    hostCve(
                                                            e,
                                                            e.stage(
                                                                    e.cat(
                                                                            e.lower(e.v("fir")),
                                                                            e.s(" "),
                                                                            graft(e, e.v("v")))))))),
                            e.ret(foldJoin(e, e.v("parts"), "{ ", " }"))));
            accessor(e, x, "lval", "ty", ty(e));
            method(
                    e,
                    x,
                    "lval",
                    "ref",
                    e.tup(),
                    str(e),
                    e.matchOf(
                            in(e, "source"),
                            "s",
                            Opt.none(),
                            e.p("ssa", e.v("s")),
                            e.p("mem", e.mb.panic(e.s("llvm: value in memory used as an operand")))));
            method(
                    e,
                    x,
                    "lval",
                    "mem",
                    e.tup(),
                    str(e),
                    e.matchOf(
                            in(e, "source"),
                            "s",
                            Opt.none(),
                            e.p("mem", e.v("s")),
                            e.p("ssa", e.mb.panic(e.s("llvm: register value has no address")))));
            method(
                    e,
                    x,
                    "lval",
                    "is_mem",
                    e.tup(),
                    e.mb.typesInt(),
                    e.matchOf(in(e, "source"), "s", Opt.none(), e.p("mem", e.intOf(1)), e.p("ssa", e.intOf(0))));
            x.add(e.implInterface(e.v("lval"), e.mb.evalValue(), e.p("type", in(e, "ty"))));
            accessor(e, x, "lfield", "name", str(e));
            accessor(e, x, "lfield", "ty", ty(e));
            accessor(e, x, "ltail", "views", e.listType(t(e, "lfield")));
            accessor(e, x, "ltail", "after", str(e));
            accessor(e, x, "ltail", "len", e.listType(str(e)));
            accessor(e, x, "ltail", "align", intT(e));
            accessor(e, x, "ltarget", "mod", lmodT(e));
            accessor(e, x, "lirctx", "xx", xxT(e));
            accessor(e, x, "lirctx", "mod", lmodOptT(e));
            accessor(e, x, "linst", "k", t(e, "linst_u"));
            for (var vo : new String[] {"lfield", "lglobal", "llabel", "linst", "lmod"}) {
                markerType(e, x, vo, vo, vo + "_m", e.aspectDispatch(lv(e, vo + "_m_id")));
                x.add(e.implInterface(e.v(vo), t(e, vo + "_m")));
            }
            accessor(e, x, "lglobal", "name", str(e));
            accessor(e, x, "lglobal", "ty", ty(e));
            accessor(e, x, "lglobal", "init", str(e));
            accessor(e, x, "llabel", "key", e.typesJumpKey());
            accessor(e, x, "llabel", "name", str(e));

            method(e, x, "lmod", "xx", e.tup(), e.typesExprContext(), in(e, "xx", "value"));
            x.add(e.methodImpl(
                    e.v("lmod"),
                    "set_xx",
                    e.typesExprContext(),
                    e.optionTypeNone(),
                    e.stage(e.mb.set(in(e, "xx"), e.optSome(e.typesExprContext(), e.v("arg"))))));
            method(
                    e,
                    x,
                    "lmod",
                    "fresh_n",
                    e.tup(),
                    e.mb.typesInt(),
                    e.seq(e.let("n", in(e, "n")), e.incr(in(e, "n")), e.ret(e.v("n"))));
            method(
                    e,
                    x,
                    "lmod",
                    "fresh",
                    e.tup(),
                    str(e),
                    e.cat(e.s("%t"), toStr(e, e.callM(outer(e), "fresh_n", e.tup()))));
            method(
                    e,
                    x,
                    "lmod",
                    "fresh_label",
                    e.tup(),
                    str(e),
                    e.cat(e.s("L"), toStr(e, e.callM(outer(e), "fresh_n", e.tup()))));
            method(
                    e,
                    x,
                    "lmod",
                    "slot",
                    ty(e),
                    str(e),
                    e.seq(
                            e.let("name", e.cat(e.s("@s"), toStr(e, e.callM(outer(e), "fresh_n", e.tup())))),
                            push(
                                    e,
                                    in(e, "slots"),
                                    mk(
                                            e,
                                            "lglobal",
                                            e.p("name", e.v("name")),
                                            e.p("ty", e.v("arg")),
                                            e.p("init", e.s("zeroinitializer")))),
                            e.ret(e.v("name"))));
            method(
                    e,
                    x,
                    "lmod",
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
                                                            "llabel",
                                                            e.p("key", e.v("arg")),
                                                            e.p("name", e.fld(e.v("st"), "name")))))),
                            e.ret(e.fld(e.v("st"), "name"))));
            method(
                    e,
                    x,
                    "lmod",
                    "ir",
                    ty(e),
                    str(e),
                    irFn(e, in(e, "xx", "value"), e.optSome(lt(e, "lmod"), outer(e)), e.v("arg")));
            method(
                    e,
                    x,
                    "lmod",
                    "use_struct",
                    ty(e),
                    str(e),
                    e.seq(
                            e.let("name", acc(e, in(e, "xx", "value"), e.v("arg"), "lstruct", "name")),
                            e.let("st", e.rec(e.p("hit", e.intOf(0)))),
                            e.foreach(
                                    in(e, "structs"),
                                    "s",
                                    e.ifOnly(
                                            e.eq(
                                                    acc(e, in(e, "xx", "value"), e.v("s"), "lstruct", "name"),
                                                    e.v("name")),
                                            e.mb.set(e.fld(e.v("st"), "hit"), num(e, 1)))),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("st"), "hit"), num(e, 0)),
                                    e.seq(
                                            push(e, in(e, "structs"), e.v("arg")),
                                            e.let("parts", e.listNew(str(e))),
                                            e.foreach(
                                                    acc(e, in(e, "xx", "value"), e.v("arg"), "lstruct", "fields"),
                                                    "f0",
                                                    push(e, e.v("parts"), irOf(e, e.callM(e.v("f0"), "ty", e.tup())))),
                                            push(
                                                    e,
                                                    in(e, "struct_lines"),
                                                    e.cat(
                                                            e.s("%"),
                                                            e.v("name"),
                                                            e.s(" = type { "),
                                                            e.join(e.v("parts"), ", "),
                                                            e.s(" }"))))),
                            e.ret(e.cat(e.s("%"), e.v("name")))));
            method(
                    e,
                    x,
                    "lmod",
                    "typed",
                    lvalT(e),
                    str(e),
                    e.cat(irOf(e, lvTy(e, e.v("arg"))), e.s(" "), lvRef(e, e.v("arg"))));
            method(
                    e,
                    x,
                    "lmod",
                    "params_ir",
                    ty(e),
                    str(e),
                    hf(
                            e,
                            "h_params_ir",
                            e.p("xx", in(e, "xx", "value")),
                            e.p("mod", e.optSome(lt(e, "lmod"), outer(e))),
                            e.p("ty", e.v("arg"))));
            method(
                    e,
                    x,
                    "lmod",
                    "callsig",
                    ty(e),
                    str(e),
                    e.seq(e.ret(e.ifElse(
                            im0(e, "lfn", "varargs", asp(e, in(e, "xx", "value"), e.v("arg"), "lfn")),
                            irOf(e, e.v("arg")),
                            irOf(e, im0(e, "lfn", "ret", asp(e, in(e, "xx", "value"), e.v("arg"), "lfn")))))));
            method(
                    e,
                    x,
                    "lmod",
                    "render_insts",
                    e.listType(t(e, "linst")),
                    e.listType(str(e)),
                    e.seq(
                            e.let("out", e.listNew(str(e))),
                            e.let("st", e.rec(e.p("open", e.intOf(1)), e.p("pend", e.dstr("")))),
                            e.let("refs", e.mapNew(str(e), e.mb.typesIntBox())),
                            e.foreach(
                                    e.v("arg"),
                                    "r0",
                                    e.matchOf(
                                            e.callM(e.v("r0"), "k", e.tup()),
                                            "p",
                                            Opt.some(e.seq()),
                                            e.p("label", e.callM(e.v("refs"), "put", e.tup(p(e, "name"), num(e, 0)))))),
                            e.foreach(
                                    e.v("arg"),
                                    "r1",
                                    e.matchOf(
                                            e.callM(e.v("r1"), "k", e.tup()),
                                            "p",
                                            Opt.some(e.seq()),
                                            e.p("br", countRef(e, p(e, "name"))),
                                            e.p("condbr", e.seq(countRef(e, p(e, "t")), countRef(e, p(e, "f")))))),
                            e.foreach(
                                    e.v("arg"),
                                    "i0",
                                    e.seq(
                                            e.let("k", e.callM(e.v("i0"), "k", e.tup())),
                                            e.ifOnly(
                                                    e.eq(
                                                            e.matchOf(
                                                                    e.v("k"),
                                                                    "q",
                                                                    Opt.some(e.intOf(0)),
                                                                    e.p("label", e.intOf(1))),
                                                            num(e, 0)),
                                                    flushPend(e)),
                                            e.matchOf(
                                                    e.v("k"),
                                                    "p",
                                                    Opt.none(),
                                                    e.p(
                                                            "label",
                                                            e.seq(
                                                                    e.ifOnly(
                                                                            e.eq(e.fld(e.v("st"), "open"), num(e, 1)),
                                                                            push(
                                                                                    e,
                                                                                    e.v("out"),
                                                                                    e.cat(
                                                                                            e.s("  br label %"),
                                                                                            p(e, "name")))),
                                                                    e.ifElse(
                                                                            andB(
                                                                                    e,
                                                                                    e.eq(
                                                                                            e.fld(e.v("st"), "pend"),
                                                                                            p(e, "name")),
                                                                                    e.eq(
                                                                                            e.mb.as(
                                                                                                    e.callM(
                                                                                                            e.v("refs"),
                                                                                                            "get",
                                                                                                            p(
                                                                                                                    e,
                                                                                                                    "name")),
                                                                                                    e.mb.typesInt()),
                                                                                            num(e, 1))),
                                                                            e.mb.set(e.fld(e.v("st"), "pend"), e.s("")),
                                                                            e.seq(
                                                                                    flushPend(e),
                                                                                    push(
                                                                                            e,
                                                                                            e.v("out"),
                                                                                            e.cat(
                                                                                                    p(e, "name"),
                                                                                                    e.s(":"))))),
                                                                    e.mb.set(e.fld(e.v("st"), "open"), num(e, 1)))),
                                                    e.p(
                                                            "br",
                                                            e.seq(
                                                                    e.mb.set(e.fld(e.v("st"), "pend"), p(e, "name")),
                                                                    e.mb.set(e.fld(e.v("st"), "open"), num(e, 0)))),
                                                    e.p(
                                                            "condbr",
                                                            renderTerminator(
                                                                    e,
                                                                    e.s("  br i1 "),
                                                                    lvRef(e, p(e, "cond")),
                                                                    e.s(", label %"),
                                                                    p(e, "t"),
                                                                    e.s(", label %"),
                                                                    p(e, "f"))),
                                                    e.p(
                                                            "binop",
                                                            renderArm(
                                                                    e,
                                                                    e.s("  "),
                                                                    p(e, "res"),
                                                                    e.s(" = "),
                                                                    p(e, "op"),
                                                                    e.s(" "),
                                                                    irOf(e, lvTy(e, p(e, "a"))),
                                                                    e.s(" "),
                                                                    lvRef(e, p(e, "a")),
                                                                    e.s(", "),
                                                                    lvRef(e, p(e, "b")))),
                                                    e.p(
                                                            "cmp",
                                                            renderArm(
                                                                    e,
                                                                    e.s("  "),
                                                                    p(e, "res"),
                                                                    e.s(" = "),
                                                                    p(e, "op"),
                                                                    e.s(" "),
                                                                    irOf(e, lvTy(e, p(e, "a"))),
                                                                    e.s(" "),
                                                                    lvRef(e, p(e, "a")),
                                                                    e.s(", "),
                                                                    lvRef(e, p(e, "b")))),
                                                    e.p(
                                                            "unop",
                                                            renderArm(
                                                                    e,
                                                                    e.s("  "),
                                                                    p(e, "res"),
                                                                    e.s(" = "),
                                                                    p(e, "op"),
                                                                    e.s(" "),
                                                                    typedOf(e, p(e, "a")))),
                                                    e.p(
                                                            "conv",
                                                            renderArm(
                                                                    e,
                                                                    e.s("  "),
                                                                    p(e, "res"),
                                                                    e.s(" = "),
                                                                    p(e, "op"),
                                                                    e.s(" "),
                                                                    typedOf(e, p(e, "a")),
                                                                    e.s(" to "),
                                                                    irOf(e, p(e, "to")))),
                                                    e.p(
                                                            "load",
                                                            renderArm(
                                                                    e,
                                                                    e.s("  "),
                                                                    p(e, "res"),
                                                                    e.s(" = load "),
                                                                    irOf(e, p(e, "ty")),
                                                                    e.s(", ptr "),
                                                                    p(e, "ptr"))),
                                                    e.p(
                                                            "store",
                                                            renderArm(
                                                                    e,
                                                                    e.s("  store "),
                                                                    typedOf(e, p(e, "val")),
                                                                    e.s(", ptr "),
                                                                    p(e, "ptr"))),
                                                    e.p(
                                                            "gep",
                                                            e.seq(
                                                                    e.let("parts", e.listNew(str(e))),
                                                                    e.foreach(
                                                                            p(e, "idx"),
                                                                            "ix",
                                                                            push(
                                                                                    e,
                                                                                    e.v("parts"),
                                                                                    typedOf(e, e.v("ix")))),
                                                                    push(
                                                                            e,
                                                                            e.v("out"),
                                                                            e.cat(
                                                                                    e.s("  "),
                                                                                    p(e, "res"),
                                                                                    e.s(" = getelementptr inbounds "),
                                                                                    irOf(e, p(e, "base")),
                                                                                    e.s(", ptr "),
                                                                                    p(e, "ptr"),
                                                                                    e.s(", "),
                                                                                    e.join(e.v("parts"), ", "))),
                                                                    e.mb.set(e.fld(e.v("st"), "open"), num(e, 1)))),
                                                    e.p(
                                                            "call",
                                                            e.seq(
                                                                    e.let("parts", e.listNew(str(e))),
                                                                    e.foreach(
                                                                            p(e, "args"),
                                                                            "ax",
                                                                            push(
                                                                                    e,
                                                                                    e.v("parts"),
                                                                                    typedOf(e, e.v("ax")))),
                                                                    push(
                                                                            e,
                                                                            e.v("out"),
                                                                            e.cat(
                                                                                    e.s("  "),
                                                                                    e.ifElse(
                                                                                            e.fld(
                                                                                                    p(e, "res"),
                                                                                                    "is_some"),
                                                                                            e.cat(
                                                                                                    e.fld(
                                                                                                            p(e, "res"),
                                                                                                            "value"),
                                                                                                    e.s(" = ")),
                                                                                            e.dstr("")),
                                                                                    e.s("call "),
                                                                                    e.callM(
                                                                                            outer(e),
                                                                                            "callsig",
                                                                                            lvTy(e, p(e, "fn"))),
                                                                                    e.s(" "),
                                                                                    lvRef(e, p(e, "fn")),
                                                                                    e.s("("),
                                                                                    e.join(e.v("parts"), ", "),
                                                                                    e.s(")"))),
                                                                    e.mb.set(e.fld(e.v("st"), "open"), num(e, 1)))),
                                                    e.p(
                                                            "ret",
                                                            renderTerminator(
                                                                    e,
                                                                    e.s("  ret "),
                                                                    e.ifElse(
                                                                            e.fld(p(e, "val"), "is_some"),
                                                                            typedOf(e, e.fld(p(e, "val"), "value")),
                                                                            e.dstr("void"))))))),
                            flushPend(e),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("st"), "open"), num(e, 1)),
                                    e.mb.panic(e.s("llvm: function body does not end in a terminator"))),
                            e.ret(e.v("out"))));

            method(
                    e,
                    x,
                    "lmod",
                    "render",
                    e.tup(),
                    renderT(e),
                    e.seq(
                            e.let("out", e.listNew(str(e))),
                            e.let("declared", e.listNew(str(e))),
                            e.foreach(
                                    in(e, "externs"),
                                    "f0",
                                    e.seq(
                                            e.let("f", e.v("f0")),
                                            e.let(
                                                    "line",
                                                    e.cat(
                                                            e.s("declare "),
                                                            irOf(
                                                                    e,
                                                                    im0(
                                                                            e,
                                                                            "lfn",
                                                                            "ret",
                                                                            asp(
                                                                                    e,
                                                                                    in(e, "xx", "value"),
                                                                                    lvTy(e, e.v("f")),
                                                                                    "lfn"))),
                                                            e.s(" "),
                                                            lvRef(e, e.v("f")),
                                                            e.s("("),
                                                            e.callM(outer(e), "params_ir", lvTy(e, e.v("f"))),
                                                            e.s(")"))),
                                            e.let("dup", e.rec(e.p("v", e.intOf(0)))),
                                            e.foreach(
                                                    e.v("declared"),
                                                    "d0",
                                                    e.ifOnly(
                                                            e.eq(e.v("d0"), e.v("line")),
                                                            e.mb.set(e.fld(e.v("dup"), "v"), num(e, 1)))),
                                            e.ifOnly(
                                                    e.eq(e.fld(e.v("dup"), "v"), num(e, 0)),
                                                    e.seq(
                                                            push(e, e.v("declared"), e.v("line")),
                                                            push(e, e.v("out"), e.v("line")))))),
                            e.foreach(
                                    in(e, "globals"),
                                    "g0",
                                    push(
                                            e,
                                            e.v("out"),
                                            e.cat(
                                                    e.callM(e.v("g0"), "name", e.tup()),
                                                    e.s(" = global "),
                                                    irOf(e, e.callM(e.v("g0"), "ty", e.tup())),
                                                    e.s(" "),
                                                    e.callM(e.v("g0"), "init", e.tup())))),
                            e.foreach(
                                    in(e, "constants"),
                                    "g0",
                                    push(
                                            e,
                                            e.v("out"),
                                            e.cat(
                                                    e.callM(e.v("g0"), "name", e.tup()),
                                                    e.s(" = constant "),
                                                    irOf(e, e.callM(e.v("g0"), "ty", e.tup())),
                                                    e.s(" "),
                                                    e.callM(e.v("g0"), "init", e.tup())))),
                            e.foreach(
                                    in(e, "slots"),
                                    "g0",
                                    push(
                                            e,
                                            e.v("out"),
                                            e.cat(
                                                    e.callM(e.v("g0"), "name", e.tup()),
                                                    e.s(" = internal global "),
                                                    irOf(e, e.callM(e.v("g0"), "ty", e.tup())),
                                                    e.s(" "),
                                                    e.callM(e.v("g0"), "init", e.tup())))),
                            e.foreach(in(e, "funcs"), "f", push(e, e.v("out"), e.v("f"))),
                            e.let("all", e.listNew(str(e))),
                            e.foreach(in(e, "struct_lines"), "l", push(e, e.v("all"), e.v("l"))),
                            e.foreach(e.v("out"), "l", push(e, e.v("all"), e.v("l"))),
                            e.ret(e.rec(
                                    e.p("ir", e.cat(e.join(e.v("all"), "\n"), e.s("\n"))),
                                    e.p("libs", in(e, "libs"))))));
            x.add(e.methodImpl(
                    e.v("lmod"),
                    "lib",
                    str(e),
                    e.optionTypeNone(),
                    e.stage(e.seq(
                            e.let("dup", e.rec(e.p("v", e.intOf(0)))),
                            e.foreach(
                                    in(e, "libs"),
                                    "l0",
                                    e.ifOnly(e.eq(e.v("l0"), e.v("arg")), e.mb.set(e.fld(e.v("dup"), "v"), num(e, 1)))),
                            e.ifOnly(e.eq(e.fld(e.v("dup"), "v"), num(e, 0)), push(e, in(e, "libs"), e.v("arg")))))));

            method(
                    e,
                    x,
                    "lmod",
                    "extern",
                    e.rec(e.p("name", str(e)), e.p("ty", ty(e))),
                    str(e),
                    e.seq(
                            e.let("gname", e.cat(e.s("@"), a(e, "name"))),
                            push(e, in(e, "externs"), lvalNew(e, a(e, "ty"), e.v("gname"))),
                            e.ret(e.v("gname"))));
            method(
                    e,
                    x,
                    "lmod",
                    "add_global",
                    e.rec(e.p("konst", boolT(e)), e.p("name", str(e)), e.p("ty", ty(e)), e.p("init", str(e))),
                    str(e),
                    e.seq(
                            e.let("gname", e.cat(e.s("@"), a(e, "name"))),
                            e.let(
                                    "g",
                                    mk(
                                            e,
                                            "lglobal",
                                            e.p("name", e.v("gname")),
                                            e.p("ty", a(e, "ty")),
                                            e.p("init", a(e, "init")))),
                            e.ifOnly(a(e, "konst"), push(e, in(e, "constants"), e.v("g"))),
                            e.ifOnly(e.not(a(e, "konst")), push(e, in(e, "globals"), e.v("g"))),
                            e.ret(e.v("gname"))));
            method(
                    e,
                    x,
                    "lmod",
                    "add_cstr",
                    str(e),
                    str(e),
                    e.seq(
                            e.let("bytes", e.call(e.mb.evalStrUtf8(), e.v("arg"))),
                            e.let("parts", e.listNew(str(e))),
                            e.foreach(
                                    e.v("bytes"),
                                    "b",
                                    push(
                                            e,
                                            e.v("parts"),
                                            e.cat(
                                                    e.s("i8 "),
                                                    toStr(
                                                            e,
                                                            e.mb.operatorAnd(
                                                                    e.mb.as(e.v("b"), e.mb.typesInt()),
                                                                    num(e, 255)))))),
                            push(e, e.v("parts"), e.s("i8 0")),
                            e.let("gname", e.cat(e.s("@str"), toStr(e, e.callM(outer(e), "fresh_n", e.tup())))),
                            push(
                                    e,
                                    in(e, "constants"),
                                    mk(
                                            e,
                                            "lglobal",
                                            e.p("name", e.v("gname")),
                                            e.p(
                                                    "ty",
                                                    mk(
                                                            e,
                                                            "lt_arr",
                                                            e.p("elem", intTy(e, "u8")),
                                                            e.p(
                                                                    "count",
                                                                    e.mb.operatorAdd(
                                                                            e.fld(e.v("bytes"), "len"), num(e, 1))))),
                                            e.p("init", e.cat(e.s("["), e.join(e.v("parts"), ", "), e.s("]"))))),
                            e.ret(e.v("gname"))));

            method(
                    e,
                    x,
                    "lmod",
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
                            e.let("ptypes", acc(e, in(e, "xx", "value"), a(e, "ty"), "lfn", "params")),
                            e.let("fret", acc(e, in(e, "xx", "value"), a(e, "ty"), "lfn", "ret")),
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
                                            e.let("an", e.cat(e.s("%a"), toStr(e, e.fld(e.v("pi"), "i")))),
                                            push(e, e.v("sig"), e.cat(irOf(e, e.v("pty")), e.s(" "), e.v("an"))),
                                            e.let("s", e.callM(outer(e), "slot", e.v("pty"))),
                                            push(
                                                    e,
                                                    e.v("out"),
                                                    storeInst(e, lvalNew(e, e.v("pty"), e.v("an")), e.v("s"))),
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
                                                                    "lbinding",
                                                                    e.p("ty", e.v("pty")),
                                                                    e.p("ptr", e.v("s"))))),
                                            e.incr(e.fld(e.v("pi"), "i")))),
                            e.let("vx", e.call(e.evalBuiltin("eval_context"), e.tup())),
                            e.let(
                                    "sc",
                                    e.call(
                                            e.evalBuiltin("scope"),
                                            e.rec(e.p("vx", e.v("vx")), e.p("entries", e.v("entries"))))),
                            e.let("tgt", mk(e, "ltarget", e.p("mod", outer(e)))),
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
                            e.let("is_void", has(e, e.v("ctx"), e.v("fret"), "lvoid")),
                            e.ifOnly(
                                    andB(
                                            e,
                                            e.not(e.v("ended")),
                                            andB(e, e.v("is_void"), isLlvm(e, e.v("ctx"), e.v("r")))),
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
                                    andB(e, e.not(e.v("ended")), e.v("is_void")),
                                    push(e, e.v("out"), inst(e, "ret", e.p("val", e.optNone(lt(e, "lval")))))),
                            e.ifOnly(
                                    e.v("need_cast"),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(e, "ret", e.p("val", e.optSome(lt(e, "lval"), lvalOf(e, e.v("v"))))))),
                            e.let("lines", e.callM(outer(e), "render_insts", e.v("out"))),
                            push(
                                    e,
                                    in(e, "funcs"),
                                    e.cat(
                                            e.s("define "),
                                            irOf(e, e.v("fret")),
                                            e.s(" @"),
                                            a(e, "name"),
                                            e.s("("),
                                            e.join(e.v("sig"), ", "),
                                            e.s(") {\n"),
                                            e.join(e.v("lines"), "\n"),
                                            e.s("\n}"))),
                            e.ret(e.cat(e.s("@"), a(e, "name")))));
        }
        {
            selfType(
                    e,
                    x,
                    "lt_int",
                    e.cat(e.ifElse(in(e, "signed"), e.dstr("i"), e.dstr("u")), toStr(e, in(e, "bits"))),
                    e.aspectDispatch(
                            lv(e, "ltype_id"),
                            lv(e, "lint_id"),
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
                                                    has(e, xx(e), srcType(e), "lint"),
                                                    andB(
                                                            e,
                                                            e.mb.operatorLt(srcBits(e, "lint"), in(e, "bits")),
                                                            orB(
                                                                    e,
                                                                    e.eq(srcBits(e, "lint"), num(e, 1)),
                                                                    e.eq(
                                                                            acc(e, xx(e), srcType(e), "lint", "signed"),
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
                                                                    lvalNew(e, outer(e), toStr(e, e.v("l")))))))),
                            e.let("sb", cvBits(e, e.v("st"), "lint")),
                            e.let(
                                    "op",
                                    e.ifElse(
                                            e.eq(e.v("sb"), num(e, 1)),
                                            e.dstr("zext"),
                                            e.ifElse(
                                                    acc(e, xx(e), e.v("st"), "lint", "signed"),
                                                    e.dstr("sext"),
                                                    e.dstr("zext")))),
                            e.ret(conv(e, e.v("op"), a(e, "cv"), outer(e)))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "lint"),
                                    andB(
                                            e,
                                            e.eq(acc(e, xx(e), a(e, "other"), "lint", "bits"), in(e, "bits")),
                                            e.eq(acc(e, xx(e), a(e, "other"), "lint", "signed"), in(e, "signed")))))));
            x.add(e.implInterface(
                    e.v("lt_int"),
                    t(e, "ltype"),
                    e.p("ir", e.cat(e.s("i"), toStr(e, in(e, "bits")))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(
                    e.v("lt_int"),
                    t(e, "lint"),
                    e.p("bits", in(e, "bits")),
                    e.p("signed", in(e, "signed")),
                    e.p("min", in(e, "min")),
                    e.p("max", in(e, "max"))));
            op(e, x, "lt_int", "add", tryEr(e, binopBody(e, e.s("add")), cveT(e)));
            op(e, x, "lt_int", "sub", tryEr(e, binopBody(e, e.s("sub")), cveT(e)));
            op(e, x, "lt_int", "mul", tryEr(e, binopBody(e, e.s("mul")), cveT(e)));
            op(e, x, "lt_int", "div", tryEr(e, binopBody(e, signedOp(e, "sdiv", "udiv")), cveT(e)));
            op(e, x, "lt_int", "mod", tryEr(e, binopBody(e, signedOp(e, "srem", "urem")), cveT(e)));
            op(e, x, "lt_int", "and", tryEr(e, binopBody(e, e.s("and")), cveT(e)));
            op(e, x, "lt_int", "or", tryEr(e, binopBody(e, e.s("or")), cveT(e)));
            op(e, x, "lt_int", "xor", tryEr(e, binopBody(e, e.s("xor")), cveT(e)));
            op(e, x, "lt_int", "shl", tryEr(e, binopBody(e, e.s("shl")), cveT(e)));
            op(e, x, "lt_int", "shr", tryEr(e, binopBody(e, signedOp(e, "ashr", "lshr")), cveT(e)));
            op(e, x, "lt_int", "ushr", tryEr(e, binopBody(e, e.s("lshr")), cveT(e)));
            op(e, x, "lt_int", "eq", tryEr(e, cmpBody(e, e.s("icmp eq")), cveT(e)));
            op(e, x, "lt_int", "ne", tryEr(e, cmpBody(e, e.s("icmp ne")), cveT(e)));
            op(e, x, "lt_int", "lt", tryEr(e, cmpBody(e, signedOp(e, "icmp slt", "icmp ult")), cveT(e)));
            op(e, x, "lt_int", "le", tryEr(e, cmpBody(e, signedOp(e, "icmp sle", "icmp ule")), cveT(e)));
            op(e, x, "lt_int", "gt", tryEr(e, cmpBody(e, signedOp(e, "icmp sgt", "icmp ugt")), cveT(e)));
            op(e, x, "lt_int", "ge", tryEr(e, cmpBody(e, signedOp(e, "icmp sge", "icmp uge")), cveT(e)));
            x.add(e.implInterface(
                    e.v("lt_int"),
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
                    e.v("lt_int"),
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
                                                                            e.dstr("true"),
                                                                            e.dstr("-1")))),
                                                    e.p("res", outer(e))),
                                            cveT(e)))))));
            bindImpl(e, x, "lt_int");
            setImpl(e, x, "lt_int");
            convImpl(
                    e,
                    x,
                    "lt_int",
                    "integer",
                    e.ifOnly(
                            has(e, xx(e), e.v("st"), "lint"),
                            e.seq(
                                    e.let("sb", cvBits(e, e.v("st"), "lint")),
                                    retM(
                                            e,
                                            e.ifElse(
                                                    e.eq(e.v("sb"), in(e, "bits")),
                                                    retag(e, e.v("v"), outer(e)),
                                                    e.ifElse(
                                                            e.mb.operatorGt(e.v("sb"), in(e, "bits")),
                                                            conv(e, e.s("trunc"), e.v("v"), outer(e)),
                                                            conv(
                                                                    e,
                                                                    e.ifElse(
                                                                            acc(e, xx(e), e.v("st"), "lint", "signed"),
                                                                            e.dstr("sext"),
                                                                            e.dstr("zext")),
                                                                    e.v("v"),
                                                                    outer(e))))))),
                    whenSt(e, "lfloat", conv(e, signedOp(e, "fptosi", "fptoui"), e.v("v"), outer(e))),
                    whenSt(e, "lptr", conv(e, e.s("ptrtoint"), e.v("v"), outer(e))));

            selfType(
                    e,
                    x,
                    "lt_float",
                    e.cat(e.s("f"), toStr(e, in(e, "bits"))),
                    e.aspectDispatch(
                            lv(e, "ltype_id"),
                            lv(e, "lfloat_id"),
                            opId(e, "add"),
                            opId(e, "sub"),
                            opId(e, "mul"),
                            opId(e, "div"),
                            opId(e, "mod"),
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
                                                    has(e, xx(e), srcType(e), "lfloat"),
                                                    e.mb.operatorLt(srcBits(e, "lfloat"), in(e, "bits"))))))),
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
                                                                    lvalNew(e, outer(e), e.v("s"))))))),
                            e.ret(conv(e, e.s("fpext"), a(e, "cv"), outer(e)))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "lfloat"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "lfloat", "bits"), in(e, "bits"))))));
            x.add(e.implInterface(
                    e.v("lt_float"),
                    t(e, "ltype"),
                    e.p("ir", e.ifElse(e.eq(in(e, "bits"), num(e, 32)), e.dstr("float"), e.dstr("double"))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("lt_float"), t(e, "lfloat"), e.p("bits", in(e, "bits"))));
            op(e, x, "lt_float", "add", tryEr(e, binopBody(e, e.s("fadd")), cveT(e)));
            op(e, x, "lt_float", "sub", tryEr(e, binopBody(e, e.s("fsub")), cveT(e)));
            op(e, x, "lt_float", "mul", tryEr(e, binopBody(e, e.s("fmul")), cveT(e)));
            op(e, x, "lt_float", "div", tryEr(e, binopBody(e, e.s("fdiv")), cveT(e)));
            op(e, x, "lt_float", "mod", tryEr(e, binopBody(e, e.s("frem")), cveT(e)));
            op(e, x, "lt_float", "eq", tryEr(e, cmpBody(e, e.s("fcmp oeq")), cveT(e)));
            op(e, x, "lt_float", "ne", tryEr(e, cmpBody(e, e.s("fcmp une")), cveT(e)));
            op(e, x, "lt_float", "lt", tryEr(e, cmpBody(e, e.s("fcmp olt")), cveT(e)));
            op(e, x, "lt_float", "le", tryEr(e, cmpBody(e, e.s("fcmp ole")), cveT(e)));
            op(e, x, "lt_float", "gt", tryEr(e, cmpBody(e, e.s("fcmp ogt")), cveT(e)));
            op(e, x, "lt_float", "ge", tryEr(e, cmpBody(e, e.s("fcmp oge")), cveT(e)));
            x.add(e.implInterface(
                    e.v("lt_float"),
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
                                                    e.p("op", e.s("fneg")),
                                                    e.p("a", a(e, "base"))),
                                            cveT(e)))))));
            bindImpl(e, x, "lt_float");
            setImpl(e, x, "lt_float");
            convImpl(
                    e,
                    x,
                    "lt_float",
                    "float",
                    whenSt(
                            e,
                            "lint",
                            conv(
                                    e,
                                    e.ifElse(
                                            acc(e, xx(e), e.v("st"), "lint", "signed"),
                                            e.dstr("sitofp"),
                                            e.dstr("uitofp")),
                                    e.v("v"),
                                    outer(e))),
                    e.ifOnly(
                            has(e, xx(e), e.v("st"), "lfloat"),
                            e.seq(
                                    e.let("sb", cvBits(e, e.v("st"), "lfloat")),
                                    retM(
                                            e,
                                            e.ifElse(
                                                    e.eq(e.v("sb"), in(e, "bits")),
                                                    retag(e, e.v("v"), outer(e)),
                                                    e.ifElse(
                                                            e.mb.operatorGt(e.v("sb"), in(e, "bits")),
                                                            conv(e, e.s("fptrunc"), e.v("v"), outer(e)),
                                                            conv(e, e.s("fpext"), e.v("v"), outer(e))))))));

            selfType(
                    e,
                    x,
                    "lt_ptr",
                    e.cat(e.s("ptr("), bim0(e, ty(e), "desc", in(e, "pointee")), e.s(")")),
                    e.aspectDispatch(
                            lv(e, "ltype_id"),
                            lv(e, "lptr_id"),
                            opId(e, "eq"),
                            opId(e, "ne"),
                            opId(e, "lt"),
                            opId(e, "le"),
                            opId(e, "gt"),
                            opId(e, "ge"),
                            opId(e, "add"),
                            aspId(e, "bind"),
                            aspId(e, "set"),
                            aspId(e, "function"),
                            aspId(e, "field")),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    orB(e, has(e, xx(e), srcType(e), "lptr"), has(e, xx(e), srcType(e), "lfn"))))),
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
                                    has(e, xx(e), a(e, "other"), "lptr"),
                                    isEqual(
                                            e,
                                            xx(e),
                                            in(e, "pointee"),
                                            acc(e, xx(e), a(e, "other"), "lptr", "pointee"))))));
            x.add(e.implInterface(e.v("lt_ptr"), t(e, "ltype"), e.p("ir", e.s("ptr")), e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("lt_ptr"), t(e, "lptr"), e.p("pointee", in(e, "pointee"))));
            op(e, x, "lt_ptr", "eq", tryEr(e, cmpBody(e, e.s("icmp eq")), cveT(e)));
            op(e, x, "lt_ptr", "ne", tryEr(e, cmpBody(e, e.s("icmp ne")), cveT(e)));
            op(e, x, "lt_ptr", "lt", tryEr(e, cmpBody(e, e.s("icmp ult")), cveT(e)));
            op(e, x, "lt_ptr", "le", tryEr(e, cmpBody(e, e.s("icmp ule")), cveT(e)));
            op(e, x, "lt_ptr", "gt", tryEr(e, cmpBody(e, e.s("icmp ugt")), cveT(e)));
            op(e, x, "lt_ptr", "ge", tryEr(e, cmpBody(e, e.s("icmp uge")), cveT(e)));
            x.add(e.implInterface(
                    e.v("lt_ptr"),
                    opIface(e, "add"),
                    e.p(
                            "add",
                            body(
                                    e,
                                    e.let(
                                            "p",
                                            unwrapCveM(
                                                    e,
                                                    tryEr(e, ssa(e, xx(e), loc(e), xmod(e), a(e, "base")), cveT(e)))),
                                    e.let(
                                            "i",
                                            unwrapCveM(
                                                    e,
                                                    e.ifElse(
                                                            isLlvm(e, xx(e), a(e, "reference")),
                                                            tryEr(
                                                                    e,
                                                                    ssa(e, xx(e), loc(e), xmod(e), a(e, "reference")),
                                                                    cveT(e)),
                                                            castValue(
                                                                    e,
                                                                    xx(e),
                                                                    loc(e),
                                                                    intTy(e, "i64"),
                                                                    a(e, "reference"))))),
                                    e.ifOnly(
                                            e.not(has(e, xx(e), cveType(e, e.v("i")), "lint")),
                                            retM(e, errCve(e, loc(e), "pointer offset must be an integer"))),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("p"))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("i"))),
                                    e.let(
                                            "g",
                                            hf(
                                                    e,
                                                    "h_gep",
                                                    e.p("mod", xmod(e)),
                                                    e.p("base", in(e, "pointee")),
                                                    e.p("ptr", lvRef(e, lvalOf(e, e.v("p")))),
                                                    e.p("idx", e.listNew(lt(e, "lval"), lvalOf(e, e.v("i")))))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                                    e.ret(okCve(
                                            e,
                                            cvOf(
                                                    e,
                                                    codeOf(e, e.v("out")),
                                                    lvalNew(e, outer(e), lvRef(e, lvalOf(e, e.v("g")))))))))));
            bindImpl(e, x, "lt_ptr");
            setImpl(e, x, "lt_ptr");
            convImpl(
                    e,
                    x,
                    "lt_ptr",
                    "pointer",
                    whenSt(e, "lint", conv(e, e.s("inttoptr"), e.v("v"), outer(e))),
                    whenSt(e, "lptr", retag(e, e.v("v"), outer(e))),
                    whenSt(e, "lfn", retag(e, e.v("v"), outer(e))));
            x.add(e.implInterface(
                    e.v("lt_ptr"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.ifOnly(
                                            has(e, xx(e), in(e, "pointee"), "lvoid"),
                                            retM(e, errCve(e, loc(e), "cannot index a void pointer"))),
                                    e.let(
                                            "s",
                                            unwrapCveM(
                                                    e,
                                                    tryEr(e, ssa(e, xx(e), loc(e), xmod(e), a(e, "self")), cveT(e)))),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("s"))),
                                    e.let("p", lvRef(e, lvalOf(e, e.v("s")))),
                                    e.let(
                                            "g",
                                            unwrapCveM(
                                                    e,
                                                    e.ifElse(
                                                            has(e, xx(e), in(e, "pointee"), "lstruct"),
                                                            tryEr(
                                                                    e,
                                                                    hf(
                                                                            e,
                                                                            "h_gep_field",
                                                                            e.p("xx", xx(e)),
                                                                            e.p("loc", loc(e)),
                                                                            e.p("mod", xmod(e)),
                                                                            e.p("st", in(e, "pointee")),
                                                                            e.p("ptr", e.v("p")),
                                                                            e.p(
                                                                                    "name",
                                                                                    hf(
                                                                                            e,
                                                                                            "h_key_str",
                                                                                            e.p("xx", xx(e)),
                                                                                            e.p("cv", a(e, "key"))))),
                                                                    cveT(e)),
                                                            tryEr(
                                                                    e,
                                                                    hf(
                                                                            e,
                                                                            "h_gep_index",
                                                                            e.p("xx", xx(e)),
                                                                            e.p("loc", loc(e)),
                                                                            e.p("mod", xmod(e)),
                                                                            e.p("base", in(e, "pointee")),
                                                                            e.p("ptr", e.v("p")),
                                                                            e.p("key", a(e, "key"))),
                                                                    cveT(e))))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                                    e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), lvalOf(e, e.v("g"))))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));

            selfType(
                    e,
                    x,
                    "lt_arr",
                    e.cat(
                            e.s("array("),
                            bim0(e, ty(e), "desc", in(e, "elem")),
                            e.s(", "),
                            toStr(e, in(e, "count")),
                            e.s(")")),
                    e.aspectDispatch(
                            lv(e, "ltype_id"), lv(e, "larr_id"), aspId(e, "field"), aspId(e, "bind"), aspId(e, "set")),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    hasB(e, xx(e), srcType(e), aspIface(e, "tuple"))))),
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(isEqual(e, xx(e), outer(e), e.v("st")), retM(e, okCve(e, a(e, "cv")))),
                            e.let("tk", lookup(e, xx(e), e.v("st"), aspIface(e, "tuple"))),
                            e.ifOnly(
                                    e.not(e.fld(e.v("tk"), "is_some")),
                                    retM(e, errCve(e, loc(e), "array initializer must be a tuple"))),
                            e.let(
                                    "elems",
                                    bim(
                                            e,
                                            aspIface(e, "tuple"),
                                            "take_elements",
                                            e.fld(e.v("tk"), "value"),
                                            e.rec(
                                                    e.p("xx", xx(e)),
                                                    e.p("loc", loc(e)),
                                                    e.p("value", cveValue(e, a(e, "cv")))))),
                            e.let("parts", e.listNew(str(e))),
                            e.let("st2", e.rec(e.p("n", e.intOf(0)), e.p("bad", e.intOf(0)))),
                            e.foreach(
                                    e.v("elems"),
                                    "er",
                                    e.seq(
                                            e.let("ec", unwrapCveM(e, e.v("er"))),
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
                                                                            in(e, "elem"),
                                                                            e.v("ec")),
                                                                    cveT(e)))),
                                            e.ifOnly(
                                                    isTargetCode(e, cveCode(e, e.v("v"))),
                                                    e.mb.set(e.fld(e.v("st2"), "bad"), num(e, 1))),
                                            push(e, e.v("parts"), e.callM(xmod(e), "typed", lvalOf(e, e.v("v")))),
                                            e.incr(e.fld(e.v("st2"), "n")))),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("st2"), "bad"), num(e, 1)),
                                    retM(e, errCve(e, loc(e), "array initializer element is not a constant"))),
                            e.ifOnly(
                                    e.not(e.eq(e.fld(e.v("st2"), "n"), in(e, "count"))),
                                    retM(e, errCve(e, loc(e), "array initializer has the wrong element count"))),
                            e.ret(okCve(
                                    e,
                                    cvOf(
                                            e,
                                            codeNone(e, loc(e)),
                                            lvalNew(
                                                    e,
                                                    outer(e),
                                                    e.cat(e.s("["), e.join(e.v("parts"), ", "), e.s("]"))))))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "larr"),
                                    andB(
                                            e,
                                            e.eq(acc(e, xx(e), a(e, "other"), "larr", "count"), in(e, "count")),
                                            isEqual(
                                                    e,
                                                    xx(e),
                                                    in(e, "elem"),
                                                    acc(e, xx(e), a(e, "other"), "larr", "elem")))))));
            x.add(e.implInterface(
                    e.v("lt_arr"),
                    t(e, "ltype"),
                    e.p(
                            "ir",
                            e.cat(
                                    e.s("["),
                                    toStr(e, in(e, "count")),
                                    e.s(" x "),
                                    irFn(e, irArgXx(e), irArgMod(e), in(e, "elem")),
                                    e.s("]"))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(
                    e.v("lt_arr"), t(e, "larr"), e.p("elem", in(e, "elem")), e.p("count", in(e, "count"))));
            bindImpl(e, x, "lt_arr");
            setImpl(e, x, "lt_arr");
            x.add(e.implInterface(
                    e.v("lt_arr"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.let("sl", lvalOf(e, a(e, "self"))),
                                    e.ifOnly(
                                            e.not(lvIsMem(e, e.v("sl"))),
                                            retM(e, errCve(e, loc(e), "array value is not addressable"))),
                                    e.let("p", lvMem(e, e.v("sl"))),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "self"))),
                                    e.let("i32", intTy(e, "i32")),
                                    e.let(
                                            "k",
                                            unwrapCveM(
                                                    e,
                                                    e.ifElse(
                                                            isLlvm(e, xx(e), a(e, "key")),
                                                            tryEr(
                                                                    e,
                                                                    ssa(e, xx(e), loc(e), xmod(e), a(e, "key")),
                                                                    cveT(e)),
                                                            castValue(e, xx(e), loc(e), e.v("i32"), a(e, "key"))))),
                                    e.ifOnly(
                                            e.not(has(e, xx(e), cveType(e, e.v("k")), "lint")),
                                            retM(e, errCve(e, loc(e), "index must be an integer"))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("k"))),
                                    e.let(
                                            "g",
                                            hf(
                                                    e,
                                                    "h_gep",
                                                    e.p("mod", xmod(e)),
                                                    e.p("base", outer(e)),
                                                    e.p("ptr", e.v("p")),
                                                    e.p(
                                                            "idx",
                                                            e.listNew(
                                                                    lt(e, "lval"),
                                                                    lvalNew(e, e.v("i32"), e.s("0")),
                                                                    lvalOf(e, e.v("k")))))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                                    e.let("r", lvRef(e, lvalOf(e, e.v("g")))),
                                    e.ret(okCve(
                                            e, cvOf(e, codeOf(e, e.v("out")), lvalMem(e, in(e, "elem"), e.v("r"))))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));

            selfType(
                    e,
                    x,
                    "lt_struct",
                    e.cat(e.s("struct "), in(e, "name")),
                    e.aspectDispatch(
                            lv(e, "ltype_id"),
                            lv(e, "lstruct_id"),
                            aspId(e, "field"),
                            aspId(e, "bind"),
                            aspId(e, "set")),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    hasB(e, xx(e), srcType(e), aspIface(e, "record"))))),
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(isEqual(e, xx(e), outer(e), e.v("st")), retM(e, okCve(e, a(e, "cv")))),
                            e.let("rk", lookup(e, xx(e), e.v("st"), aspIface(e, "record"))),
                            e.ifOnly(
                                    e.not(e.fld(e.v("rk"), "is_some")),
                                    retM(e, errCve(e, loc(e), "struct initializer must be a record"))),
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
                                                    e.p("value", cveValue(e, a(e, "cv")))))),
                            e.let("parts", e.listNew(str(e))),
                            e.let("bad", e.rec(e.p("v", e.intOf(0)))),
                            e.foreach(
                                    in(e, "fields"),
                                    "f0",
                                    e.seq(
                                            e.let("f", e.v("f0")),
                                            e.let(
                                                    "ec",
                                                    unwrapCveM(
                                                            e,
                                                            e.callM(
                                                                    e.v("fields"),
                                                                    "get",
                                                                    e.callM(e.v("f"), "name", e.tup())))),
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
                                                                            e.callM(e.v("f"), "ty", e.tup()),
                                                                            e.v("ec")),
                                                                    cveT(e)))),
                                            e.ifOnly(
                                                    isTargetCode(e, cveCode(e, e.v("v"))),
                                                    e.mb.set(e.fld(e.v("bad"), "v"), num(e, 1))),
                                            push(e, e.v("parts"), e.callM(xmod(e), "typed", lvalOf(e, e.v("v")))))),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("bad"), "v"), num(e, 1)),
                                    retM(e, errCve(e, loc(e), "struct initializer field is not a constant"))),
                            e.ret(okCve(
                                    e,
                                    cvOf(
                                            e,
                                            codeNone(e, loc(e)),
                                            lvalNew(
                                                    e,
                                                    outer(e),
                                                    e.cat(e.s("{ "), e.join(e.v("parts"), ", "), e.s(" }"))))))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "lstruct"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "lstruct", "name"), in(e, "name"))))));
            x.add(e.implInterface(
                    e.v("lt_struct"),
                    t(e, "ltype"),
                    e.p(
                            "ir",
                            e.ifElse(
                                    e.fld(irArgMod(e), "is_some"),
                                    e.callM(e.fld(irArgMod(e), "value"), "use_struct", outer(e)),
                                    e.cat(e.s("%"), in(e, "name")))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(
                    e.v("lt_struct"),
                    t(e, "lstruct"),
                    e.p("name", in(e, "name")),
                    e.p("fields", in(e, "fields")),
                    e.p("tail", in(e, "tail"))));
            bindImpl(e, x, "lt_struct");
            setImpl(e, x, "lt_struct");
            x.add(e.implInterface(
                    e.v("lt_struct"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.let("sl", lvalOf(e, a(e, "self"))),
                                    e.ifOnly(
                                            e.not(lvIsMem(e, e.v("sl"))),
                                            retM(e, errCve(e, loc(e), "struct value is not addressable"))),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "self"))),
                                    e.let(
                                            "g",
                                            unwrapCveM(
                                                    e,
                                                    tryEr(
                                                            e,
                                                            hf(
                                                                    e,
                                                                    "h_gep_field",
                                                                    e.p("xx", xx(e)),
                                                                    e.p("loc", loc(e)),
                                                                    e.p("mod", xmod(e)),
                                                                    e.p("st", outer(e)),
                                                                    e.p("ptr", lvMem(e, e.v("sl"))),
                                                                    e.p(
                                                                            "name",
                                                                            hf(
                                                                                    e,
                                                                                    "h_key_str",
                                                                                    e.p("xx", xx(e)),
                                                                                    e.p("cv", a(e, "key"))))),
                                                            cveT(e)))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                                    e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), lvalOf(e, e.v("g"))))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));

            selfType(
                    e,
                    x,
                    "lt_fn",
                    e.s("fn"),
                    e.aspectDispatch(lv(e, "ltype_id"), lv(e, "lfn_id"), aspId(e, "function")),
                    body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                    body(e, e.ret(okCve(e, a(e, "cv")))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ifOnly(e.not(has(e, xx(e), a(e, "other"), "lfn")), retM(e, dfalse(e))),
                            e.ifOnly(
                                    e.not(e.eq(
                                            im0(e, "lfn", "varargs", asp(e, xx(e), a(e, "other"), "lfn")),
                                            in(e, "varargs"))),
                                    retM(e, dfalse(e))),
                            e.ifOnly(
                                    e.not(isEqual(
                                            e,
                                            xx(e),
                                            in(e, "ret"),
                                            im0(e, "lfn", "ret", asp(e, xx(e), a(e, "other"), "lfn")))),
                                    retM(e, dfalse(e))),
                            e.let("om", e.mapNew(str(e), ty(e))),
                            e.let("on", e.rec(e.p("v", e.intOf(0)))),
                            e.foreach(
                                    im0(e, "lfn", "params", asp(e, xx(e), a(e, "other"), "lfn")),
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
                    e.v("lt_fn"),
                    t(e, "ltype"),
                    e.p(
                            "ir",
                            e.cat(
                                    irFn(e, irArgXx(e), irArgMod(e), in(e, "ret")),
                                    e.s(" ("),
                                    hf(
                                            e,
                                            "h_params_ir",
                                            e.p("xx", irArgXx(e)),
                                            e.p("mod", irArgMod(e)),
                                            e.p("ty", outer(e))),
                                    e.s(")"))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(
                    e.v("lt_fn"),
                    t(e, "lfn"),
                    e.p("params", in(e, "params")),
                    e.p("ret", in(e, "ret")),
                    e.p("varargs", in(e, "varargs"))));
            x.add(e.implInterface(
                    e.v("lt_fn"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "self"))),
                                    e.let("fnv", lvalOf(e, a(e, "self"))),
                                    e.let("args", e.listNew(lt(e, "lval"))),
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
                                    e.let(
                                            "single",
                                            andB(e, e.eq(e.fld(e.v("pn"), "v"), num(e, 1)), e.not(in(e, "varargs")))),
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
                                                            e.mb.operatorLt(
                                                                    e.fld(e.v("st"), "i"), e.fld(e.v("pn"), "v")),
                                                            e.seq(
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
                                                                                                                    e
                                                                                                                            .fld(
                                                                                                                                    e
                                                                                                                                            .v(
                                                                                                                                                    "st"),
                                                                                                                                    "i"))),
                                                                                                    e.v("ec")),
                                                                                            cveT(e)))),
                                                                    appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                                                    push(e, e.v("args"), lvalOf(e, e.v("v"))))),
                                                    e.ifOnly(
                                                            e.mb.operatorGe(
                                                                    e.fld(e.v("st"), "i"), e.fld(e.v("pn"), "v")),
                                                            e.seq(
                                                                    e.ifOnly(
                                                                            e.not(in(e, "varargs")),
                                                                            retM(
                                                                                    e,
                                                                                    errCve(
                                                                                            e,
                                                                                            loc(e),
                                                                                            "too many call arguments"))),
                                                                    e.let(
                                                                            "v",
                                                                            unwrapCveM(
                                                                                    e,
                                                                                    tryEr(
                                                                                            e,
                                                                                            ssa(
                                                                                                    e, xx(e), loc(e),
                                                                                                    xmod(e), e.v("ec")),
                                                                                            cveT(e)))),
                                                                    appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                                                    push(e, e.v("args"), lvalOf(e, e.v("v"))))),
                                                    e.incr(e.fld(e.v("st"), "i")))),
                                    e.ifOnly(
                                            e.mb.operatorLt(e.fld(e.v("st"), "i"), e.fld(e.v("pn"), "v")),
                                            retM(e, errCve(e, loc(e), "too few call arguments"))),
                                    e.ifOnly(
                                            has(e, xx(e), in(e, "ret"), "lvoid"),
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
                                    e.let("r", fresh(e, xmod(e))),
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
                                            e, cvOf(e, codeOf(e, e.v("out")), lvalNew(e, in(e, "ret"), e.v("r"))))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));

            selfType(
                    e,
                    x,
                    "lt_void",
                    e.s("void"),
                    e.aspectDispatch(lv(e, "ltype_id"), lv(e, "lvoid_id")),
                    body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                    body(e, e.ret(okCve(e, a(e, "cv")))),
                    noCastTo(e),
                    castToPanic(e),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "lvoid"))));
            x.add(e.implInterface(e.v("lt_void"), t(e, "ltype"), e.p("ir", e.s("void")), e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("lt_void"), t(e, "lvoid")));

            selfType(
                    e,
                    x,
                    "lt_slot",
                    e.cat(bim0(e, ty(e), "desc", in(e, "base")), e.s("@"), in(e, "ptr")),
                    e.mb.ifOf(
                            TList.of(
                                    new TestMb.Branch(
                                            e.eq(a(e, "key"), lv(e, "lslot_id")),
                                            e.optSome(e.mb.typesAspect(), outer(e))),
                                    new TestMb.Branch(
                                            e.eq(a(e, "key"), lv(e, "ltype_id")),
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
                            push(e, e.v("out"), storeInst(e, lvalOf(e, e.v("v")), in(e, "ptr"))),
                            e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), lvalMem(e, outer(e), in(e, "ptr")))))),
                    body(e, e.ret(typeCanCastFrom(e, xx(e), loc(e), a(e, "dest_type"), in(e, "base")))),
                    body(
                            e,
                            e.let("r", fresh(e, xmod(e))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, a(e, "cv"))),
                            push(e, e.v("out"), loadInst(e, e.v("r"), in(e, "base"), in(e, "ptr"))),
                            e.let("loaded", cvOf(e, codeOf(e, e.v("out")), lvalNew(e, in(e, "base"), e.v("r")))),
                            e.ret(castValue(e, xx(e), loc(e), a(e, "dest_type"), e.v("loaded")))),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "lslot"),
                                    andB(
                                            e,
                                            e.eq(acc(e, xx(e), a(e, "other"), "lslot", "ptr"), in(e, "ptr")),
                                            isEqual(
                                                    e,
                                                    xx(e),
                                                    in(e, "base"),
                                                    acc(e, xx(e), a(e, "other"), "lslot", "base")))))));
            x.add(e.implInterface(
                    e.v("lt_slot"),
                    t(e, "ltype"),
                    e.p("ir", irFn(e, irArgXx(e), irArgMod(e), in(e, "base"))),
                    e.p("ssa", in(e, "base"))));
            x.add(e.implInterface(e.v("lt_slot"), t(e, "lslot"), e.p("base", in(e, "base")), e.p("ptr", in(e, "ptr"))));
            {
                var binOps = new String[] {
                    "add", "sub", "mul", "div", "mod", "and", "or", "xor", "shl", "shr", "ushr", "eq", "ne", "lt", "le",
                    "gt", "ge"
                };
                var unOps = new String[] {"neg", "bit_not"};
                var ids = new TList<SourceExpr>();
                ids.add(lv(e, "lgtype_m_id"));
                ids.add(aspId(e, "set"));
                ids.add(aspId(e, "function"));
                ids.add(aspId(e, "field"));
                for (var op : binOps) ids.add(opId(e, op));
                for (var op : unOps) ids.add(opId(e, op));
                selfType(
                        e,
                        x,
                        "lt_global",
                        e.cat(
                                e.ifElse(in(e, "mem"), e.dstr("global("), e.s("const(")),
                                bim0(e, ty(e), "desc", in(e, "base")),
                                e.s(")")),
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
                                e.ret(castValue(e, xx(e), loc(e), a(e, "dest_type"), gval(e, a(e, "cv"))))),
                        body(
                                e,
                                e.ret(andB(
                                        e,
                                        has(e, xx(e), a(e, "other"), "lgtype_m"),
                                        andB(
                                                e,
                                                e.eq(in(e, "mem"), acc(e, xx(e), a(e, "other"), "lgtype_m", "mem")),
                                                isEqual(
                                                        e,
                                                        xx(e),
                                                        in(e, "base"),
                                                        acc(e, xx(e), a(e, "other"), "lgtype_m", "base")))))));
                x.add(e.implInterface(
                        e.v("lt_global"), t(e, "lgtype_m"), e.p("base", in(e, "base")), e.p("mem", in(e, "mem"))));
                rtLiftImpl(e, x, "lt_global");
                rtBindImpl(e, x, "lt_global", e.mb.typesString());
                rtUseImpl(e, x, "lt_global");
                x.add(e.implInterface(
                        e.v("lt_global"),
                        e.mb.evalAspectSet(),
                        e.p(
                                "set",
                                body(
                                        e,
                                        e.ifOnly(
                                                e.not(in(e, "mem")),
                                                retM(e, errCve(e, loc(e), "cannot assign to a constant global"))),
                                        e.ret(tryEr(
                                                e,
                                                hf(
                                                        e,
                                                        "h_set",
                                                        e.p("xx", xx(e)),
                                                        e.p("loc", loc(e)),
                                                        e.p("mod", xmod(e)),
                                                        e.p("ty", in(e, "base")),
                                                        e.p("self", gval(e, a(e, "self"))),
                                                        e.p("value", a(e, "value"))),
                                                cveT(e)))))));
                for (var op : binOps) {
                    x.add(e.implInterface(
                            e.v("lt_global"),
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
                                                            e.p("base", gval(e, a(e, "base"))),
                                                            e.p("reference", a(e, "reference")))))))));
                }
                for (var op : unOps) {
                    x.add(e.implInterface(
                            e.v("lt_global"),
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
                                                            e.p("base", gval(e, a(e, "base"))))))))));
                }
                x.add(e.implInterface(
                        e.v("lt_global"),
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
                                                        e.p("self", gval(e, a(e, "self"))),
                                                        e.p("argument", a(e, "argument"))))))),
                        e.p("exception_type", e.optionTypeLoweredNone())));
                x.add(e.implInterface(
                        e.v("lt_global"),
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
                                                        e.p("self", gval(e, a(e, "self"))),
                                                        e.p("key", a(e, "key"))))))),
                        e.p(
                                "type_opt",
                                bim(
                                        e,
                                        aspIface(e, "field"),
                                        "type_opt",
                                        lookupV(e, xx(e), in(e, "base"), aspIface(e, "field")),
                                        e.v("arg"))),
                        e.p(
                                "keys",
                                bim(
                                        e,
                                        aspIface(e, "field"),
                                        "keys",
                                        lookupV(e, e.v("arg"), in(e, "base"), aspIface(e, "field")),
                                        e.v("arg")))));
            }
        }
        {
            accessor(e, x, "lbinding", "ty", ty(e));
            accessor(e, x, "lbinding", "ptr", str(e));
            method(e, x, "lbinding", "val", e.tup(), lvalT(e), lvalMem(e, in(e, "ty"), in(e, "ptr")));
            bindingImpl(e, x, "lbinding", cvOf(e, a(e, "code"), lvalMem(e, in(e, "ty"), in(e, "ptr"))));
            bindingImpl(e, x, "lbinding_none", e.mb.panic(e.s("llvm: unit binding has no value")));

            markerType(e, x, "lunit", "unit", "lunit_m", e.aspectDispatch(lv(e, "lunit_m_id"), aspId(e, "bind")));
            x.add(e.implInterface(e.v("lunit"), t(e, "lunit_m")));
            x.add(e.implInterface(
                    e.v("lunit"),
                    e.mb.evalAspectBind(),
                    e.p(
                            "bind",
                            e.eresultOk(
                                    e.mb.typesBindResult(),
                                    e.call(
                                            e.mb.evalBindResult(),
                                            e.rec(
                                                    e.p("code", codeNone(e, loc(e))),
                                                    e.p("binding", mkArg(e, "lbinding_none", e.tup()))))))));

            accessor(e, x, "lrange", "code", codeT(e));
            accessor(e, x, "lrange", "n", lvalT(e));
            x.add(e.implInterface(e.v("lrange"), e.mb.evalValue(), e.p("type", in(e, "t"))));
            selfType(
                    e,
                    x,
                    "lrange_t",
                    e.cat(e.s("range("), bim0(e, ty(e), "desc", in(e, "elem")), e.s(")")),
                    e.aspectDispatch(lv(e, "lrange_m_id"), aspId(e, "iterable")),
                    body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                    body(e, e.ret(okCve(e, a(e, "cv")))),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "lrange_m"),
                                    isEqual(
                                            e,
                                            xx(e),
                                            in(e, "elem"),
                                            acc(e, xx(e), a(e, "other"), "lrange_m", "elem"))))));
            x.add(e.implInterface(e.v("lrange_t"), t(e, "lrange_m"), e.p("elem", in(e, "elem"))));
            x.add(e.implInterface(
                    e.v("lrange_t"),
                    aspIface(e, "iterable"),
                    e.p("element_type", in(e, "elem")),
                    e.p(
                            "loop_binding",
                            body(
                                    e,
                                    e.let(
                                            "s",
                                            e.callM(
                                                    e.callM(
                                                            reinterpret(e, e.fld(e.v("arg"), "target"), "ltarget"),
                                                            "mod",
                                                            e.tup()),
                                                    "slot",
                                                    in(e, "elem"))),
                                    e.ret(mk(e, "lbinding", e.p("ty", in(e, "elem")), e.p("ptr", e.v("s")))))),
                    e.p(
                            "element_access",
                            okCve(
                                    e,
                                    cvOf(
                                            e,
                                            codeNone(e, loc(e)),
                                            e.callM(reinterpret(e, a(e, "loop"), "lbinding"), "val", e.tup())))),
                    e.p(
                            "foreach",
                            body(
                                    e,
                                    e.let("mod", xmod(e)),
                                    e.let("elem", in(e, "elem")),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "base"))),
                                    e.let("rv", reinterpret(e, cveValue(e, a(e, "base")), "lrange")),
                                    appendCode(e, e.v("out"), e.callM(e.v("rv"), "code", e.tup())),
                                    e.let("ns", e.callM(e.v("mod"), "slot", e.v("elem"))),
                                    push(e, e.v("out"), storeInst(e, e.callM(e.v("rv"), "n", e.tup()), e.v("ns"))),
                                    e.let("lb", reinterpret(e, a(e, "loop"), "lbinding")),
                                    e.let("cs", e.callM(e.v("lb"), "ptr", e.tup())),
                                    push(e, e.v("out"), storeInst(e, lvalNew(e, e.v("elem"), e.s("0")), e.v("cs"))),
                                    e.let("head", freshLabel(e, e.v("mod"))),
                                    e.let("bodyl", freshLabel(e, e.v("mod"))),
                                    e.let("end", freshLabel(e, e.v("mod"))),
                                    push(e, e.v("out"), brInst(e, e.v("head"))),
                                    push(e, e.v("out"), labelInst(e, e.v("head"))),
                                    e.let("i", fresh(e, e.v("mod"))),
                                    push(e, e.v("out"), loadInst(e, e.v("i"), e.v("elem"), e.v("cs"))),
                                    e.let("nn", fresh(e, e.v("mod"))),
                                    push(e, e.v("out"), loadInst(e, e.v("nn"), e.v("elem"), e.v("ns"))),
                                    e.let("c", fresh(e, e.v("mod"))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    "cmp",
                                                    e.p("res", e.v("c")),
                                                    e.p(
                                                            "op",
                                                            e.ifElse(
                                                                    acc(e, xx(e), e.v("elem"), "lint", "signed"),
                                                                    e.dstr("icmp slt"),
                                                                    e.dstr("icmp ult"))),
                                                    e.p("a", lvalNew(e, e.v("elem"), e.v("i"))),
                                                    e.p("b", lvalNew(e, e.v("elem"), e.v("nn"))))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    "condbr",
                                                    e.p("cond", lvalNew(e, intTy(e, "i1"), e.v("c"))),
                                                    e.p("t", e.v("bodyl")),
                                                    e.p("f", e.v("end")))),
                                    push(e, e.v("out"), labelInst(e, e.v("bodyl"))),
                                    appendCode(e, e.v("out"), a(e, "element_bind")),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "body"))),
                                    e.ifOnly(
                                            e.not(cveEnded(e, a(e, "body"))),
                                            e.seq(
                                                    e.let("i2", fresh(e, e.v("mod"))),
                                                    push(e, e.v("out"), loadInst(e, e.v("i2"), e.v("elem"), e.v("cs"))),
                                                    e.let("i3", fresh(e, e.v("mod"))),
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            inst(
                                                                    e,
                                                                    "binop",
                                                                    e.p("res", e.v("i3")),
                                                                    e.p("op", e.s("add")),
                                                                    e.p("a", lvalNew(e, e.v("elem"), e.v("i2"))),
                                                                    e.p("b", lvalNew(e, e.v("elem"), e.s("1"))))),
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            storeInst(
                                                                    e, lvalNew(e, e.v("elem"), e.v("i3")), e.v("cs"))),
                                                    push(e, e.v("out"), brInst(e, e.v("head"))))),
                                    push(e, e.v("out"), labelInst(e, e.v("end"))),
                                    e.ret(okCve(e, cvNo(e, codeOf(e, e.v("out")))))))));
            selfType(
                    e,
                    x,
                    "lloop_t",
                    e.s("loop"),
                    rtDispatch(e, e.mb.typesString(), e.aspectDispatch(lv(e, "lloop_m_id"), aspId(e, "iterable"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "lloop_m"))));
            rtLiftImpl(e, x, "lloop_t");
            rtBindImpl(e, x, "lloop_t", e.mb.typesString());
            rtUseImpl(e, x, "lloop_t");
            x.add(e.implInterface(e.v("lloop_t"), t(e, "lloop_m")));
            x.add(e.implInterface(
                    e.v("lloop_t"),
                    aspIface(e, "iterable"),
                    e.p("element_type", mkArg(e, "lunit", e.tup())),
                    e.p("loop_binding", mkArg(e, "lbinding_none", e.tup())),
                    e.p("element_access", okCve(e, cvOf(e, codeNone(e, loc(e)), mkArg(e, "lunit", e.tup())))),
                    e.p(
                            "foreach",
                            body(
                                    e,
                                    e.let("mod", xmod(e)),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "base"))),
                                    e.let("head", freshLabel(e, e.v("mod"))),
                                    push(e, e.v("out"), labelInst(e, e.v("head"))),
                                    appendCode(e, e.v("out"), a(e, "element_bind")),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "body"))),
                                    e.ifOnly(
                                            e.not(cveEnded(e, a(e, "body"))),
                                            push(e, e.v("out"), brInst(e, e.v("head")))),
                                    e.ret(okCve(e, cvNo(e, codeOf(e, e.v("out")))))))));

            selfType(
                    e,
                    x,
                    "lgfn_t",
                    e.cat(e.s("llvm global definition function "), toStr(e, in(e, "kind"))),
                    rtDispatch(e, lt(e, "lmod"), e.aspectDispatch(lv(e, "lgfn_m_id"), aspId(e, "function"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ret(andB(
                                    e,
                                    has(e, xx(e), a(e, "other"), "lgfn_m"),
                                    e.eq(acc(e, xx(e), a(e, "other"), "lgfn_m", "kind"), in(e, "kind"))))));
            rtLiftImpl(e, x, "lgfn_t");
            rtBindImpl(e, x, "lgfn_t", lt(e, "lmod"));
            rtUseImpl(e, x, "lgfn_t");
            x.add(e.implInterface(e.v("lgfn_t"), t(e, "lgfn_m"), e.p("kind", in(e, "kind"))));
            x.add(e.implInterface(
                    e.v("lgfn_t"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let("argv", a(e, "argument")),
                                    e.ifOnly(
                                            e.not(cveHas(e, e.v("argv"))),
                                            retM(e, errCve(e, loc(e), "llvm global definition argument has no value"))),
                                    e.let("st", cveType(e, e.v("argv"))),
                                    e.let("rk", lookup(e, xx(e), e.v("st"), aspIface(e, "record"))),
                                    e.ifOnly(
                                            e.not(e.fld(e.v("rk"), "is_some")),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            "llvm global definition argument must be a record"))),
                                    e.let(
                                            "want",
                                            e.ifElse(
                                                    lgfnIs(e, "cstr_fn"),
                                                    e.listNew(str(e), e.s("mod"), e.s("value")),
                                                    e.ifElse(
                                                            lgfnIs(e, "constant_fn"),
                                                            e.listNew(str(e), e.s("ty"), e.s("value")),
                                                            e.ifElse(
                                                                    lgfnIs(e, "ref_fn"),
                                                                    e.listNew(str(e), e.s("ref"), e.s("ty")),
                                                                    e.ifElse(
                                                                            lgfnIs(e, "extern_fn"),
                                                                            e.listNew(
                                                                                    str(e),
                                                                                    e.s("mod"),
                                                                                    e.s("name"),
                                                                                    e.s("ty")),
                                                                            e.ifElse(
                                                                                    lgfnIs(e, "func_fn"),
                                                                                    e.listNew(
                                                                                            str(e),
                                                                                            e.s("mod"),
                                                                                            e.s("name"),
                                                                                            e.s("params"),
                                                                                            e.s("ty"),
                                                                                            e.s("body")),
                                                                                    e.ifElse(
                                                                                            lgfnIs(e, "global_zero_fn"),
                                                                                            e.listNew(
                                                                                                    str(e),
                                                                                                    e.s("mod"),
                                                                                                    e.s("name"),
                                                                                                    e.s("ty")),
                                                                                            e.listNew(
                                                                                                    str(e),
                                                                                                    e.s("mod"),
                                                                                                    e.s("name"),
                                                                                                    e.s("ty"),
                                                                                                    e.s("init"))))))))),
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
                                                                    e.s("llvm global definition has no argument "),
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
                                                                    e.s("llvm global definition requires arguments "),
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
                                    e.ifOnly(
                                            lgfnIs(e, "cstr_fn"),
                                            e.seq(
                                                    e.let(
                                                            "modcv",
                                                            unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("mod")))),
                                                    e.let(
                                                            "valuecv",
                                                            unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("value")))),
                                                    e.let(
                                                            "refcv",
                                                            lgfnStaged(
                                                                    e,
                                                                    e.callM(
                                                                            e.callM(
                                                                                    graft(e, e.v("modcv")),
                                                                                    "m",
                                                                                    e.tup()),
                                                                            "add_cstr",
                                                                            graft(e, e.v("valuecv"))))),
                                                    retM(
                                                            e,
                                                            tagValue(
                                                                    e,
                                                                    e.v("refcv"),
                                                                    lgfnGlobalT(
                                                                            e,
                                                                            mk(
                                                                                    e,
                                                                                    "lt_ptr",
                                                                                    e.p("pointee", intTy(e, "u8"))),
                                                                            false))))),
                                    e.let("tycv", unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("ty")))),
                                    e.let("tk", lookup(e, xx(e), cveType(e, e.v("tycv")), aspIface(e, "type"))),
                                    e.ifOnly(
                                            orB(e, isRuntime(e, e.v("tycv")), e.not(e.fld(e.v("tk"), "is_some"))),
                                            retM(e, errCve(e, loc(e), "llvm ty must be a constant type"))),
                                    e.let(
                                            "tyv",
                                            bim(
                                                    e,
                                                    aspIface(e, "type"),
                                                    "type",
                                                    e.fld(e.v("tk"), "value"),
                                                    cveValue(e, e.v("tycv")))),
                                    e.ifOnly(
                                            lgfnIs(e, "ref_fn"),
                                            e.seq(
                                                    e.ifOnly(
                                                            e.not(
                                                                    orB(
                                                                            e,
                                                                            has(e, xx(e), e.v("tyv"), "lgtype_m"),
                                                                            has(e, xx(e), e.v("tyv"), "lloop_m"))),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            e.cat(
                                                                                    e.s(
                                                                                            "llvm reference ty is not a reference type: "),
                                                                                    tyDesc(e, e.v("tyv")))))),
                                                    e.let(
                                                            "rcv",
                                                            unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("ref")))),
                                                    retM(e, tagValue(e, e.v("rcv"), e.v("tyv"))))),
                                    e.ifOnly(
                                            e.not(has(e, xx(e), e.v("tyv"), "ltype")),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            e.cat(
                                                                    e.s(
                                                                            "llvm global definition ty is not an llvm type: "),
                                                                    tyDesc(e, e.v("tyv")))))),
                                    e.ifOnly(
                                            lgfnIs(e, "extern_fn"),
                                            e.seq(
                                                    e.let(
                                                            "modcv",
                                                            unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("mod")))),
                                                    e.ifOnly(
                                                            e.not(has(e, xx(e), e.v("tyv"), "lfn")),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            e.cat(
                                                                                    e.s(
                                                                                            "llvm extern ty must be a function type, not "),
                                                                                    tyDesc(e, e.v("tyv")))))),
                                                    e.let("namecv", lgfnField(e, "name")),
                                                    e.seq(
                                                            e.let(
                                                                    "refcv",
                                                                    lgfnStaged(
                                                                            e,
                                                                            e.callM(
                                                                                    e.callM(
                                                                                            graft(e, e.v("modcv")),
                                                                                            "m",
                                                                                            e.tup()),
                                                                                    "extern",
                                                                                    e.rec(
                                                                                            e.p(
                                                                                                    "name",
                                                                                                    graft(
                                                                                                            e,
                                                                                                            e.v(
                                                                                                                    "namecv"))),
                                                                                            e.p(
                                                                                                    "ty",
                                                                                                    e.lower(
                                                                                                            e.v(
                                                                                                                    "tyv"))))))),
                                                            retM(
                                                                    e,
                                                                    tagValue(
                                                                            e,
                                                                            e.v("refcv"),
                                                                            lgfnGlobalT(e, e.v("tyv"), false)))))),
                                    e.ifOnly(
                                            lgfnIs(e, "func_fn"),
                                            e.seq(
                                                    e.let(
                                                            "modcv",
                                                            unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("mod")))),
                                                    e.ifOnly(
                                                            e.not(has(e, xx(e), e.v("tyv"), "lfn")),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            e.cat(
                                                                                    e.s(
                                                                                            "llvm function ty must be a function type, not "),
                                                                                    tyDesc(e, e.v("tyv")))))),
                                                    e.let("pcv", lgfnField(e, "params")),
                                                    e.let(
                                                            "ptk",
                                                            lookup(
                                                                    e,
                                                                    xx(e),
                                                                    cveType(e, e.v("pcv")),
                                                                    aspIface(e, "tuple"))),
                                                    e.ifOnly(
                                                            e.not(e.fld(e.v("ptk"), "is_some")),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            "llvm function params must be a tuple of parameter names"))),
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
                                                                                            "llvm function parameter names must be constant"
                                                                                                    + " strings"))),
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
                                                    countLoop(e, "tn", acc(e, xx(e), e.v("tyv"), "lfn", "params")),
                                                    e.ifOnly(
                                                            e.not(e.eq(e.fld(e.v("pn"), "v"), e.fld(e.v("tn"), "v"))),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            e.cat(
                                                                                    e.s("llvm function has "),
                                                                                    toStr(e, e.fld(e.v("pn"), "v")),
                                                                                    e.s(
                                                                                            " parameter names but its type has "),
                                                                                    toStr(e, e.fld(e.v("tn"), "v")),
                                                                                    e.s(" parameters"))))),
                                                    e.let("namecv", lgfnField(e, "name")),
                                                    e.let("bodycv", lgfnField(e, "body")),
                                                    e.seq(
                                                            e.let(
                                                                    "refcv",
                                                                    lgfnStaged(
                                                                            e,
                                                                            e.callM(
                                                                                    e.callM(
                                                                                            graft(e, e.v("modcv")),
                                                                                            "m",
                                                                                            e.tup()),
                                                                                    "func",
                                                                                    e.rec(
                                                                                            e.p(
                                                                                                    "name",
                                                                                                    graft(
                                                                                                            e,
                                                                                                            e.v(
                                                                                                                    "namecv"))),
                                                                                            e.p(
                                                                                                    "params",
                                                                                                    e.lower(
                                                                                                            e.v(
                                                                                                                    "names"))),
                                                                                            e.p(
                                                                                                    "ty",
                                                                                                    e.lower(
                                                                                                            e.v(
                                                                                                                    "tyv"))),
                                                                                            e.p(
                                                                                                    "body",
                                                                                                    graft(
                                                                                                            e,
                                                                                                            e.v(
                                                                                                                    "bodycv"))))))),
                                                            retM(
                                                                    e,
                                                                    tagValue(
                                                                            e,
                                                                            e.v("refcv"),
                                                                            lgfnGlobalT(e, e.v("tyv"), false)))))),
                                    e.let(
                                            "ir",
                                            e.ifElse(
                                                    lgfnIs(e, "global_zero_fn"),
                                                    unwrapCveM(
                                                            e, constEvalIn(e, xx(e), e.stage(e.s("zeroinitializer")))),
                                                    unwrapCveM(
                                                            e,
                                                            tryEr(
                                                                    e,
                                                                    initFn(
                                                                            e,
                                                                            e.v("tyv"),
                                                                            unwrapCveM(
                                                                                    e,
                                                                                    e.callM(
                                                                                            e.v("fields"),
                                                                                            "get",
                                                                                            e.ifElse(
                                                                                                    lgfnIs(
                                                                                                            e,
                                                                                                            "constant_fn"),
                                                                                                    e.dstr("value"),
                                                                                                    e.dstr("init"))))),
                                                                    cveT(e))))),
                                    e.ifOnly(
                                            lgfnIs(e, "constant_fn"),
                                            e.seq(retM(e, tagValue(e, e.v("ir"), lgfnGlobalT(e, e.v("tyv"), false))))),
                                    e.let("modcv", unwrapCveM(e, e.callM(e.v("fields"), "get", e.s("mod")))),
                                    e.let("namecv", lgfnField(e, "name")),
                                    e.seq(
                                            e.let(
                                                    "refcv",
                                                    lgfnStaged(
                                                            e,
                                                            e.callM(
                                                                    e.callM(graft(e, e.v("modcv")), "m", e.tup()),
                                                                    "add_global",
                                                                    e.rec(
                                                                            e.p(
                                                                                    "konst",
                                                                                    e.lower(
                                                                                            lgfnIs(
                                                                                                    e,
                                                                                                    "global_const_fn"))),
                                                                            e.p("name", graft(e, e.v("namecv"))),
                                                                            e.p("ty", e.lower(e.v("tyv"))),
                                                                            e.p("init", graft(e, e.v("ir"))))))),
                                            retM(
                                                    e,
                                                    tagValue(
                                                            e,
                                                            e.v("refcv"),
                                                            e.ifElse(
                                                                    lgfnIs(e, "global_const_fn"),
                                                                    lgfnGlobalT(
                                                                            e,
                                                                            mk(e, "lt_ptr", e.p("pointee", e.v("tyv"))),
                                                                            false),
                                                                    lgfnGlobalT(e, e.v("tyv"), true))))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));

            x.add(e.implInterface(
                    e.v("ltarget"),
                    e.mb.evalTarget(),
                    e.p("cast_exception", e.mb.panic(e.s("llvm target: exceptions are not supported"))),
                    e.p(
                            "initial_bind",
                            e.eresultErr(
                                    e.mb.evalBinding(),
                                    loc(e),
                                    e.s("llvm target: match and catch payloads are not supported"))),
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
                                                    lt(e, "linst"),
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
                                                            lt(e, "linst"),
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
                                                            lt(e, "linst"),
                                                            brInst(e, e.callM(mod(e), "label_of", a(e, "dest")))))),
                                    e.ret(e.eresultOk(codeT(e), codeOf(e, e.v("out")))))),
                    e.p("guarded", e.mb.panic(e.s("llvm target: exceptions are not supported"))),
                    e.p("exc_cleanup", e.mb.panic(e.s("llvm target: exceptions are not supported"))),
                    e.p("exit_trampoline", e.mb.panic(e.s("llvm target: cleanup blocks are not supported"))),
                    e.p("drop", e.eresultOk(codeT(e), codeNone(e, loc(e)))),
                    e.p(
                            "return",
                            body(
                                    e,
                                    e.ifOnly(e.not(isLlvm(e, xx(e), a(e, "cv"))), retM(e, a(e, "cv"))),
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
                                                                            "llvm target: return value could not be stored"))))),
                                    e.let("lv", lvalOf(e, e.v("r"))),
                                    e.ret(cvOf(
                                            e,
                                            cveCode(e, e.v("r")),
                                            lvalMem(
                                                    e,
                                                    mk(
                                                            e,
                                                            "lt_slot",
                                                            e.p("base", lvTy(e, e.v("lv"))),
                                                            e.p("ptr", lvMem(e, e.v("lv")))),
                                                    lvMem(e, e.v("lv"))))))),
                    e.p("stage", errCve(e, loc(e), "llvm target: nested staging is not supported")),
                    e.p(
                            "if",
                            body(
                                    e,
                                    e.let("mod", mod(e)),
                                    e.let("out", instsNew(e)),
                                    e.let("i1", intTy(e, "i1")),
                                    e.let("end", freshLabel(e, e.v("mod"))),
                                    e.let(
                                            "st",
                                            e.rec(
                                                    e.p("ended", e.intOf(1)),
                                                    e.p("has", e.intOf(0)),
                                                    e.p("novalue", e.intOf(0)),
                                                    e.p("slot", e.dstr("")),
                                                    e.p("ty", e.optionTypeLoweredNone()))),
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
                                                    e.let("thn", freshLabel(e, e.v("mod"))),
                                                    e.let("nxt", freshLabel(e, e.v("mod"))),
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            inst(
                                                                    e,
                                                                    "condbr",
                                                                    e.p("cond", lvalOf(e, e.v("c"))),
                                                                    e.p("t", e.v("thn")),
                                                                    e.p("f", e.v("nxt")))),
                                                    push(e, e.v("out"), labelInst(e, e.v("thn"))),
                                                    e.let("b", e.fld(e.v("br"), "body")),
                                                    ifBranchStmts(e, "b"),
                                                    push(e, e.v("out"), labelInst(e, e.v("nxt"))))),
                                    e.ifOnly(
                                            e.fld(a(e, "else"), "is_some"),
                                            e.seq(e.let("b", e.fld(a(e, "else"), "value")), ifBranchStmts(e, "b"))),
                                    e.ifOnly(
                                            e.not(e.fld(a(e, "else"), "is_some")),
                                            e.mb.set(e.fld(e.v("st"), "ended"), num(e, 0))),
                                    e.ifOnly(
                                            e.eq(e.fld(e.v("st"), "ended"), num(e, 1)),
                                            retM(
                                                    e,
                                                    okCve(
                                                            e,
                                                            e.call(
                                                                    e.evalBuiltin("codevalue_control_ended"),
                                                                    codeOf(e, e.v("out")))))),
                                    push(e, e.v("out"), labelInst(e, e.v("end"))),
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
                                    e.let("r", fresh(e, e.v("mod"))),
                                    push(
                                            e,
                                            e.v("out"),
                                            loadInst(
                                                    e,
                                                    e.v("r"),
                                                    e.fld(e.v("st"), "ty", "value"),
                                                    e.fld(e.v("st"), "slot"))),
                                    e.ret(okCve(
                                            e,
                                            cvOf(
                                                    e,
                                                    codeOf(e, e.v("out")),
                                                    lvalNew(e, e.fld(e.v("st"), "ty", "value"), e.v("r"))))))),
                    e.p("try_catch_finally", errCve(e, loc(e), "llvm target: try/catch/finally is not supported")),
                    e.p("match", errCve(e, loc(e), "llvm target: match is not supported"))));
        }
        x.add(e.let("llvm_b", e.defFunc(e.v("d"), "llvm")));
        x.add(e.funcImpl(
                e.v("llvm_b"),
                e.tup(),
                e.optionTypeSome(t(e, "lmod")),
                e.mb.optNoneType(),
                e.stage(e.seq(
                        e.let(
                                "m",
                                mk(
                                        e,
                                        "lmod",
                                        e.p("n", num(e, 0)),
                                        e.p("xx", e.optNone(e.typesExprContext())),
                                        e.p("structs", e.listNew(ty(e))),
                                        e.p("struct_lines", e.listNew(str(e))),
                                        e.p("externs", e.listNew(lt(e, "lval"))),
                                        e.p("globals", e.listNew(lt(e, "lglobal"))),
                                        e.p("constants", e.listNew(lt(e, "lglobal"))),
                                        e.p("slots", e.listNew(lt(e, "lglobal"))),
                                        e.p("funcs", e.listNew(str(e))),
                                        e.p("libs", e.listNew(str(e))),
                                        e.p("labels", e.listNew(lt(e, "llabel"))))),
                        e.let("tgt", mk(e, "ltarget", e.p("mod", e.v("m")))),
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
        {
            selfType(
                    e,
                    x,
                    "lt_gref",
                    e.s("llvm global reference"),
                    rtDispatch(e, lt(e, "lgref"), e.aspectDispatch(lv(e, "lgref_m_id"))),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    isEqual(e, xx(e), lt(e, "lgref"), srcType(e))))),
                    body(
                            e,
                            e.ifOnly(
                                    isEqual(e, xx(e), outer(e), cveType(e, a(e, "cv"))), retM(e, okCve(e, a(e, "cv")))),
                            e.ret(okCve(e, retagRuntime(e, a(e, "cv"), outer(e))))),
                    noCastTo(e),
                    castToPanic(e),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "lgref_m"))));
            x.add(e.implInterface(e.v("lt_gref"), t(e, "lgref_m")));
            x.add(e.implInterface(
                    e.v("lt_gref"),
                    e.mb.evalAspectLift(),
                    e.p(
                            "lift",
                            body(
                                    e,
                                    e.let("host", reinterpret(e, a(e, "external_host_value"), "lgref")),
                                    e.ret(e.eresultOk(
                                            e.mb.evalValue(),
                                            e.call(
                                                    e.evalBuiltin("value_object_const"),
                                                    e.rec(
                                                            e.p("type", e.callM(e.v("host"), "ty", e.tup())),
                                                            e.p(
                                                                    "data",
                                                                    e.call(
                                                                            e.mb.mustReinterpret(),
                                                                            e.tup(
                                                                                    e.callM(
                                                                                            e.v("host"),
                                                                                            "ref",
                                                                                            e.tup()),
                                                                                    e.mb.builtin(
                                                                                            "types",
                                                                                            "object"))))))))))));
            rtBindImpl(e, x, "lt_gref", lt(e, "lgref"));
            rtUseImpl(e, x, "lt_gref");
            for (var prim : LLVM_PRIMS) {
                var elemT =
                        prim.isFloat() ? mk(e, "lt_float", e.p("bits", num(e, prim.bits()))) : intTy(e, prim.llvm());
                var hostT = e.mb.builtin("types", prim.host());
                for (var konst : new boolean[] {true, false}) {
                    var kind = konst ? "const" : "global";
                    var list = new TList<SourceExpr>();
                    list.add(e.let("list", hf(e, "h_fmt_list_" + prim.llvm(), a(e, "values"))));
                    list.add(e.let(
                            "gty",
                            mk(e, "lt_arr", e.p("elem", elemT), e.p("count", e.callM(e.v("list"), "count", e.tup())))));
                    list.addAll(TList.of(globalDef(e, konst, e.callM(e.v("list"), "ir", e.tup()))));
                    method(
                            e,
                            x,
                            "lmod",
                            kind + "_list_" + prim.llvm(),
                            e.rec(e.p("name", str(e)), e.p("values", e.listType(hostT))),
                            t(e, "lgref"),
                            e.seq(list.toArray(SourceExpr[]::new)));
                    var scalar = new TList<SourceExpr>();
                    scalar.add(e.let("gty", elemT));
                    scalar.addAll(TList.of(globalDef(
                            e,
                            konst,
                            e.ifElse(
                                    e.fld(a(e, "init"), "is_some"),
                                    toStr(
                                            e,
                                            prim.host().equals("float")
                                                    ? e.mb.as(e.fld(a(e, "init"), "value"), e.typesDouble())
                                                    : e.fld(a(e, "init"), "value")),
                                    e.dstr("zeroinitializer")))));
                    method(
                            e,
                            x,
                            "lmod",
                            kind + "_" + prim.llvm(),
                            e.rec(e.p("name", str(e)), e.p("init", e.call(e.mb.typesOption(), hostT))),
                            t(e, "lgref"),
                            e.seq(scalar.toArray(SourceExpr[]::new)));
                    var array = new TList<SourceExpr>();
                    array.add(e.let("gty", mk(e, "lt_arr", e.p("elem", elemT), e.p("count", a(e, "count")))));
                    array.addAll(TList.of(globalDef(
                            e,
                            konst,
                            e.ifElse(
                                    e.fld(a(e, "init"), "is_some"),
                                    hf(
                                            e,
                                            "h_init_list_" + prim.llvm(),
                                            e.p("count", a(e, "count")),
                                            e.p("values", e.fld(a(e, "init"), "value"))),
                                    e.dstr("zeroinitializer")))));
                    method(
                            e,
                            x,
                            "lmod",
                            kind + "_array_" + prim.llvm(),
                            e.rec(
                                    e.p("name", str(e)),
                                    e.p("count", intT(e)),
                                    e.p("init", e.call(e.mb.typesOption(), e.listType(hostT)))),
                            t(e, "lgref"),
                            e.seq(array.toArray(SourceExpr[]::new)));
                }
            }
            method(
                    e,
                    x,
                    "lgref",
                    "ptr_ref",
                    ty(e),
                    str(e),
                    e.seq(
                            e.let("cx", e.callM(in(e, "mod"), "xx", e.tup())),
                            e.let("gt", in(e, "ty")),
                            e.let("base", acc(e, e.v("cx"), e.v("gt"), "lgtype_m", "base")),
                            e.let(
                                    "target",
                                    e.ifElse(
                                            acc(e, e.v("cx"), e.v("gt"), "lgtype_m", "mem"),
                                            e.v("base"),
                                            acc(e, e.v("cx"), e.v("base"), "lptr", "pointee"))),
                            e.ifOnly(
                                    e.not(orB(
                                            e,
                                            has(e, e.v("cx"), e.v("arg"), "lvoid"),
                                            orB(
                                                    e,
                                                    isEqual(e, e.v("cx"), e.v("target"), e.v("arg")),
                                                    andB(
                                                            e,
                                                            has(e, e.v("cx"), e.v("target"), "larr"),
                                                            isEqual(
                                                                    e,
                                                                    e.v("cx"),
                                                                    acc(e, e.v("cx"), e.v("target"), "larr", "elem"),
                                                                    e.v("arg")))))),
                                    e.mb.panic(e.cat(
                                            e.s("llvm: pointer initializer "),
                                            in(e, "ref"),
                                            e.s(" points to "),
                                            tyDesc(e, e.v("target")),
                                            e.s(", expected "),
                                            tyDesc(e, e.v("arg"))))),
                            e.ret(in(e, "ref"))));
            method(
                    e,
                    x,
                    "lmod",
                    "h_fmt_list_ptr",
                    e.rec(e.p("pointee", ty(e)), e.p("values", e.listType(t(e, "lgref")))),
                    t(e, "llist"),
                    e.seq(
                            e.let("parts", e.listNew(str(e))),
                            countLet(e, "n"),
                            e.foreach(
                                    a(e, "values"),
                                    "g",
                                    e.seq(
                                            push(
                                                    e,
                                                    e.v("parts"),
                                                    e.cat(e.s("ptr "), e.callM(e.v("g"), "ptr_ref", a(e, "pointee")))),
                                            e.incr(e.fld(e.v("n"), "v")))),
                            e.ret(mk(
                                    e,
                                    "llist",
                                    e.p("ir", e.cat(e.s("["), e.join(e.v("parts"), ", "), e.s("]"))),
                                    e.p("count", e.fld(e.v("n"), "v"))))));
            for (var konst : new boolean[] {true, false}) {
                var kind = konst ? "const" : "global";
                var ptrElem = mk(e, "lt_ptr", e.p("pointee", a(e, "pointee")));
                var ptrs = new TList<SourceExpr>();
                ptrs.add(e.let(
                        "list",
                        e.callM(
                                outer(e),
                                "h_fmt_list_ptr",
                                e.rec(e.p("pointee", a(e, "pointee")), e.p("values", a(e, "values"))))));
                ptrs.add(e.let(
                        "gty",
                        mk(e, "lt_arr", e.p("elem", ptrElem), e.p("count", e.callM(e.v("list"), "count", e.tup())))));
                ptrs.addAll(TList.of(globalDef(e, konst, e.callM(e.v("list"), "ir", e.tup()))));
                method(
                        e,
                        x,
                        "lmod",
                        kind + "_list_ptr",
                        e.rec(e.p("name", str(e)), e.p("pointee", ty(e)), e.p("values", e.listType(t(e, "lgref")))),
                        t(e, "lgref"),
                        e.seq(ptrs.toArray(SourceExpr[]::new)));
                var scalar = new TList<SourceExpr>();
                scalar.add(e.let("gty", ptrElem));
                scalar.addAll(TList.of(globalDef(
                        e,
                        konst,
                        e.ifElse(
                                e.fld(a(e, "init"), "is_some"),
                                e.callM(e.fld(a(e, "init"), "value"), "ptr_ref", a(e, "pointee")),
                                e.dstr("null")))));
                method(
                        e,
                        x,
                        "lmod",
                        kind + "_ptr",
                        e.rec(
                                e.p("name", str(e)),
                                e.p("pointee", ty(e)),
                                e.p("init", e.call(e.mb.typesOption(), t(e, "lgref")))),
                        t(e, "lgref"),
                        e.seq(scalar.toArray(SourceExpr[]::new)));
                var array = new TList<SourceExpr>();
                array.add(e.let("gty", mk(e, "lt_arr", e.p("elem", ptrElem), e.p("count", a(e, "count")))));
                array.addAll(TList.of(globalDef(
                        e,
                        konst,
                        e.ifElse(
                                e.fld(a(e, "init"), "is_some"),
                                e.seq(
                                        e.let(
                                                "list",
                                                e.callM(
                                                        outer(e),
                                                        "h_fmt_list_ptr",
                                                        e.rec(
                                                                e.p("pointee", a(e, "pointee")),
                                                                e.p("values", e.fld(a(e, "init"), "value"))))),
                                        e.ifOnly(
                                                e.not(e.eq(e.callM(e.v("list"), "count", e.tup()), a(e, "count"))),
                                                e.mb.panic(e.cat(
                                                        e.s("llvm: array initializer has "),
                                                        toStr(e, e.callM(e.v("list"), "count", e.tup())),
                                                        e.s(" elements, expected "),
                                                        toStr(e, a(e, "count"))))),
                                        e.ret(e.callM(e.v("list"), "ir", e.tup()))),
                                e.dstr("zeroinitializer")))));
                method(
                        e,
                        x,
                        "lmod",
                        kind + "_array_ptr",
                        e.rec(
                                e.p("name", str(e)),
                                e.p("pointee", ty(e)),
                                e.p("count", intT(e)),
                                e.p("init", e.call(e.mb.typesOption(), e.listType(t(e, "lgref"))))),
                        t(e, "lgref"),
                        e.seq(array.toArray(SourceExpr[]::new)));
            }
            defObj(
                    e,
                    x,
                    "ldesc",
                    "llvm data type description",
                    e.rec(e.p("prim", str(e)), e.p("count", intT(e)), e.p("pointee", ty(e))));
            accessor(e, x, "ldesc", "pointee", ty(e));
            accessor(e, x, "ldesc", "prim", str(e));
            accessor(e, x, "ldesc", "count", intT(e));
            fnDef(e, x, "c_describe", e.rec(e.p("xx", xxT(e)), e.p("ty", ty(e))), t(e, "ldesc"));
            var describe = new TList<SourceExpr>();
            describe.add(e.let("cx", xx(e)));
            describe.add(e.let("dty", a(e, "ty")));
            describe.add(e.let("r", e.rec(e.p("prim", e.dstr("")), e.p("count", e.intOf(-1)))));
            describe.add(e.let(
                    "elem",
                    e.ifElse(
                            has(e, e.v("cx"), e.v("dty"), "larr"),
                            acc(e, e.v("cx"), e.v("dty"), "larr", "elem"),
                            e.v("dty"))));
            describe.add(e.ifOnly(
                    has(e, e.v("cx"), e.v("dty"), "larr"),
                    e.mb.set(e.fld(e.v("r"), "count"), acc(e, e.v("cx"), e.v("dty"), "larr", "count"))));
            for (var prim : LLVM_PRIMS) {
                describe.add(e.ifOnly(
                        isPrim(e, e.v("cx"), e.v("elem"), prim), e.mb.set(e.fld(e.v("r"), "prim"), e.s(prim.llvm()))));
            }
            describe.add(
                    e.ifOnly(has(e, e.v("cx"), e.v("elem"), "lptr"), e.mb.set(e.fld(e.v("r"), "prim"), e.s("ptr"))));
            describe.add(e.ret(mk(
                    e,
                    "ldesc",
                    e.p("prim", e.fld(e.v("r"), "prim")),
                    e.p("count", e.fld(e.v("r"), "count")),
                    e.p(
                            "pointee",
                            e.ifElse(
                                    has(e, e.v("cx"), e.v("elem"), "lptr"),
                                    acc(e, e.v("cx"), e.v("elem"), "lptr", "pointee"),
                                    e.v("elem"))))));
            fnImpl(e, x, "c_describe", body(e, describe.toArray(SourceExpr[]::new)));
        }
        x.add(e.finish(e.v("d")));
        var passthrough = new String[] {
            "field_t", "ptr", "array", "field", "struct", "struct_tail", "tail", "fn", "fn_varargs", "range", "global_t"
        };
        var defs = new TList<EcoMb.P>();
        defs.add(e.p("new", e.fld(e.v("llvm_b"), "func")));
        defs.add(e.p("mod_t", t(e, "lmod")));
        defs.add(e.p("field_t", t(e, "lfield")));
        defs.add(e.p("describe", e.v("c_describe")));
        defs.add(e.p("gref_obj_t", t(e, "lgref")));
        for (var i = 1; i < passthrough.length; i++) defs.add(e.p(passthrough[i], e.v("c_" + passthrough[i])));
        for (var obj :
                new String[] {"lt_int", "lt_float", "lt_void", "lloop_t", "lgfn_t", "lt_ptr", "lt_global", "lt_gref"}) {
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
                            "lt_int",
                            e.rec(
                                    e.p("bits", e.intOf(spec.bits())),
                                    e.p("signed", e.b(spec.signed())),
                                    e.p("min", e.n(spec.min())),
                                    e.p("max", e.n(spec.max()))))));
        }
        for (var bits : new int[] {32, 64}) {
            exported.add("f" + bits);
            y.add(e.let("f" + bits, lty(e, "lt_float", e.rec(e.p("bits", e.intOf(bits))))));
        }
        exported.add("void");
        y.add(e.let("void", lty(e, "lt_void", e.tup())));
        exported.add("loop");
        y.add(e.let("loop", lty(e, "lloop_t", e.tup())));
        y.add(e.let(
                "null_t",
                lty(
                        e,
                        "lt_global",
                        e.rec(
                                e.p("base", lty(e, "lt_ptr", e.rec(e.p("pointee", e.v("void"))))),
                                e.p("mem", e.b(false))))));
        y.add(e.let(
                "cstr_t",
                lty(
                        e,
                        "lt_global",
                        e.rec(
                                e.p("base", lty(e, "lt_ptr", e.rec(e.p("pointee", e.v("u8"))))),
                                e.p("mem", e.b(false))))));
        for (var kind : LGFN_KINDS) {
            if (!kind.equals("cstr_fn")) exported.add(kind);
            y.add(e.let(kind, lty(e, "lgfn_t", e.rec(e.p("kind", e.intOf(lgfnKind(kind)))))));
        }
        exported.add("gref_t");
        y.add(e.let("gref_t", lty(e, "lt_gref", e.tup())));
        y.add(e.let("d", e.define()));
        y.add(e.optionTypeLet());
        y.add(e.let("mod_o", e.defObject(e.v("d"), "llvm module")));
        y.add(e.objectImpl(e.v("mod_o"), e.rec(e.p("m", e.fld(e.v("k"), "mod_t")))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "render",
                e.tup(),
                e.optionTypeSome(renderT(e)),
                e.stage(e.callM(in(e, "m"), "render", e.tup()))));
        y.add(e.methodImpl(
                e.v("mod_o"), "lib", str(e), e.optionTypeNone(), e.stage(e.callM(in(e, "m"), "lib", e.v("arg")))));
        y.add(e.methodImpl(
                e.v("mod_o"), "m", e.tup(), e.optionTypeSome(e.fld(e.v("k"), "mod_t")), e.stage(in(e, "m"))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "null",
                e.tup(),
                e.optionTypeSome(e.v("null_t")),
                e.stage(e.call(e.lowerV("ref_fn"), e.rec(e.p("ref", e.s("null")), e.p("ty", e.lowerV("null_t")))))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "cstr",
                str(e),
                e.optionTypeSome(e.v("cstr_t")),
                e.stage(e.call(e.lowerV("cstr_fn"), e.rec(e.p("mod", outer(e)), e.p("value", e.v("arg")))))));
        for (var factory : DATA_FACTORIES) {
            var kind = factory.startsWith("const") ? "const" : "global";
            var fb = new TList<SourceExpr>();
            fb.add(e.let("tycv", a(e, "argument")));
            fb.add(e.let("tk", lookup(e, xx(e), cveType(e, e.v("tycv")), aspIface(e, "type"))));
            fb.add(e.ifOnly(
                    orB(e, isRuntime(e, e.v("tycv")), e.not(e.fld(e.v("tk"), "is_some"))),
                    retM(e, errCve(e, loc(e), "llvm." + factory + ": argument must be a constant type"))));
            fb.add(e.let(
                    "desc",
                    e.call(
                            e.fld(e.lowerV("k"), "describe"),
                            e.rec(
                                    e.p("xx", xx(e)),
                                    e.p(
                                            "ty",
                                            bim(
                                                    e,
                                                    aspIface(e, "type"),
                                                    "type",
                                                    e.fld(e.v("tk"), "value"),
                                                    cveValue(e, e.v("tycv"))))))));
            fb.add(e.let("prim", e.callM(e.v("desc"), "prim", e.tup())));
            fb.add(e.let("count", e.callM(e.v("desc"), "count", e.tup())));
            fb.add(e.let("scalar", e.mb.operatorLt(e.v("count"), num(e, 0))));
            fb.add(e.let("mod_t", e.lower(e.fld(e.v("mod_o"), "type"))));
            var isPtr = e.eq(e.v("prim"), e.s("ptr"));
            var ptrLmod = e.callM(e.fld(e.v("arg"), "mod"), "m", e.tup());
            if (factory.endsWith("_list")) {
                fb.add(e.ifOnly(
                        andB(e, isPtr, e.v("scalar")),
                        e.seq(
                                ptrPointeeLet(e),
                                ptrObjLet(e),
                                specialize(
                                        e,
                                        "llvm." + factory,
                                        e.rec(
                                                e.p("mod", e.v("mod_t")),
                                                e.p("name", str(e)),
                                                e.p("values", e.listType(e.lowerV("gref_t")))),
                                        e.seq(
                                                grefsNew(e),
                                                grefsFill(e, e.fld(e.v("arg"), "values")),
                                                e.ret(e.callM(
                                                        ptrLmod,
                                                        kind + "_list_ptr",
                                                        e.rec(
                                                                e.p("name", e.fld(e.v("arg"), "name")),
                                                                e.p("pointee", e.lower(e.v("pointee"))),
                                                                e.p("values", e.v("refs"))))))))));
            } else {
                fb.add(e.ifOnly(
                        andB(e, isPtr, e.v("scalar")),
                        e.seq(
                                ptrPointeeLet(e),
                                ptrObjLet(e),
                                specialize(
                                        e,
                                        "llvm." + factory,
                                        e.rec(
                                                e.p("mod", e.v("mod_t")),
                                                e.p("name", str(e)),
                                                e.p("init", e.call(e.mb.typesOption(), e.lowerV("gref_t")))),
                                        e.seq(
                                                e.let("init", e.fld(e.v("arg"), "init")),
                                                e.let(
                                                        "ref",
                                                        e.ifElse(
                                                                e.fld(e.v("init"), "is_some"),
                                                                e.optSome(
                                                                        e.lower(e.v("gref_obj")),
                                                                        e.call(
                                                                                e.mb.mustReinterpret(),
                                                                                e.tup(
                                                                                        e.fld(e.v("init"), "value"),
                                                                                        e.lower(e.v("gref_obj"))))),
                                                                e.optNone(e.lower(e.v("gref_obj"))))),
                                                e.ret(e.callM(
                                                        ptrLmod,
                                                        kind + "_ptr",
                                                        e.rec(
                                                                e.p("name", e.fld(e.v("arg"), "name")),
                                                                e.p("pointee", e.lower(e.v("pointee"))),
                                                                e.p("init", e.v("ref"))))))))));
                fb.add(e.ifOnly(
                        andB(e, isPtr, e.not(e.v("scalar"))),
                        e.seq(
                                ptrPointeeLet(e),
                                ptrObjLet(e),
                                specialize(
                                        e,
                                        "llvm." + factory,
                                        e.rec(
                                                e.p("mod", e.v("mod_t")),
                                                e.p("name", str(e)),
                                                e.p(
                                                        "init",
                                                        e.call(e.mb.typesOption(), e.listType(e.lowerV("gref_t"))))),
                                        e.seq(
                                                e.let("init", e.fld(e.v("arg"), "init")),
                                                e.let(
                                                        "ref",
                                                        e.ifElse(
                                                                e.fld(e.v("init"), "is_some"),
                                                                e.seq(
                                                                        grefsNew(e),
                                                                        grefsFill(e, e.fld(e.v("init"), "value")),
                                                                        e.ret(
                                                                                e.optSome(
                                                                                        e.listType(
                                                                                                e.lower(
                                                                                                        e.v(
                                                                                                                "gref_obj"))),
                                                                                        e.v("refs")))),
                                                                e.optNone(e.listType(e.lower(e.v("gref_obj")))))),
                                                e.ret(e.callM(
                                                        ptrLmod,
                                                        kind + "_array_ptr",
                                                        e.rec(
                                                                e.p("name", e.fld(e.v("arg"), "name")),
                                                                e.p("pointee", e.lower(e.v("pointee"))),
                                                                e.p("count", e.lower(e.v("count"))),
                                                                e.p("init", e.v("ref"))))))))));
            }
            for (var prim : LLVM_PRIMS) {
                var hostT = e.mb.builtin("types", prim.host());
                var lmod = e.callM(e.fld(e.v("arg"), "mod"), "m", e.tup());
                var isPrim = e.eq(e.v("prim"), e.s(prim.llvm()));
                if (factory.endsWith("_list")) {
                    fb.add(e.ifOnly(
                            andB(e, isPrim, e.v("scalar")),
                            specialize(
                                    e,
                                    "llvm." + factory,
                                    e.rec(
                                            e.p("mod", e.v("mod_t")),
                                            e.p("name", str(e)),
                                            e.p("values", e.listType(hostT))),
                                    e.callM(
                                            lmod,
                                            kind + "_list_" + prim.llvm(),
                                            e.rec(
                                                    e.p("name", e.fld(e.v("arg"), "name")),
                                                    e.p("values", e.fld(e.v("arg"), "values")))))));
                } else {
                    fb.add(e.ifOnly(
                            andB(e, isPrim, e.v("scalar")),
                            specialize(
                                    e,
                                    "llvm." + factory,
                                    e.rec(
                                            e.p("mod", e.v("mod_t")),
                                            e.p("name", str(e)),
                                            e.p("init", e.call(e.mb.typesOption(), hostT))),
                                    e.callM(
                                            lmod,
                                            kind + "_" + prim.llvm(),
                                            e.rec(
                                                    e.p("name", e.fld(e.v("arg"), "name")),
                                                    e.p("init", e.fld(e.v("arg"), "init")))))));
                    fb.add(e.ifOnly(
                            andB(e, isPrim, e.not(e.v("scalar"))),
                            specialize(
                                    e,
                                    "llvm." + factory,
                                    e.rec(
                                            e.p("mod", e.v("mod_t")),
                                            e.p("name", str(e)),
                                            e.p("init", e.call(e.mb.typesOption(), e.listType(hostT)))),
                                    e.callM(
                                            lmod,
                                            kind + "_array_" + prim.llvm(),
                                            e.rec(
                                                    e.p("name", e.fld(e.v("arg"), "name")),
                                                    e.p("count", e.lower(e.v("count"))),
                                                    e.p("init", e.fld(e.v("arg"), "init")))))));
                }
            }
            fb.add(e.ret(errCve(e, loc(e), "llvm." + factory + ": unsupported type")));
            y.add(e.let(factory + "_b", e.callM(e.v("d"), "const_func", e.s("llvm." + factory))));
            y.add(e.callM(
                    e.v(factory + "_b"),
                    "impl",
                    e.rec(
                            e.p("impl_body", e.stage(body(e, fb.toArray(SourceExpr[]::new)))),
                            e.p("exception_type", e.mb.optNoneType()),
                            e.p("description", e.optNone(str(e))))));
        }
        y.add(e.let("llvm_b", e.defFunc(e.v("d"), "llvm")));
        y.add(e.funcImpl(
                e.v("llvm_b"),
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
                e.stage(e.seq(
                        e.let("args", e.strList(e.s("-O2"), e.s("app.ll"), e.s("-o"), e.s("app"))),
                        e.foreach(a(e, "libs"), "l", push(e, e.v("args"), e.cat(e.s("-l"), e.v("l")))),
                        e.let(
                                "r",
                                e.call(
                                        e.mb.builtin("exec"),
                                        e.rec(
                                                e.p("command", e.s("clang")),
                                                e.p("args", e.v("args")),
                                                e.p("env", e.mapNew(str(e), str(e))),
                                                e.p(
                                                        "files",
                                                        e.mapNew(
                                                                str(e),
                                                                e.call(e.mb.typesArray(), e.mb.typesByte()),
                                                                e.p("app.ll", e.call(e.mb.evalStrUtf8(), a(e, "ir"))))),
                                                e.p("success_codes", e.optNone(e.listType(intT(e))))))),
                        e.let("f", e.callM(e.callM0(e.v("r"), "dir"), "open", e.s("app"))),
                        e.ret(e.call(
                                e.mb.builtin("app"),
                                e.rec(
                                        e.p("executor", e.mb.optNoneString()),
                                        e.p(
                                                "bytes",
                                                e.callM(
                                                        e.v("f"),
                                                        "read",
                                                        e.rec(
                                                                e.p("offset", e.intOf(0)),
                                                                e.p("len", e.fld(e.v("f"), "len"))))))))))));
        y.add(e.finish(e.v("d")));
        var ydefs = new TList<EcoMb.P>();
        ydefs.add(e.p("new", e.fld(e.v("llvm_b"), "func")));
        ydefs.add(e.p("build", e.fld(e.v("build_b"), "func")));
        for (var factory : DATA_FACTORIES) ydefs.add(e.p(factory, e.fld(e.v(factory + "_b"), "func")));
        ydefs.add(e.p("mod_t", e.fld(e.v("mod_o"), "type")));
        for (var name : passthrough) ydefs.add(e.p(name, e.fld(e.v("k"), name)));

        for (var name : exported) ydefs.add(e.p(name, e.v(name)));
        y.add(e.ret(e.rec(ydefs.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(y))));
        var exports = new TList<EcoMb.P>();
        for (var name : new String[] {"new", "build", "mod_t"}) exports.add(e.p(name, e.fld(e.v("k"), name)));
        for (var factory : DATA_FACTORIES) exports.add(e.p(factory, e.fld(e.v("k"), factory)));
        for (var name : passthrough) exports.add(e.p(name, e.fld(e.v("k"), name)));
        for (var name : exported) exports.add(e.p(name, e.fld(e.v("k"), name)));
        top.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return top;
    }
}
