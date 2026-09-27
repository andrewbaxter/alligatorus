package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoLlvmIr {
    private EcoLlvmIr() {}

    private static SourceExpr ty(EcoMb e) {
        return e.mb.evalType();
    }

    private static SourceExpr str(EcoMb e) {
        return e.mb.typesString();
    }

    private static SourceExpr intT(EcoMb e) {
        return e.mb.typesInt();
    }

    private static SourceExpr boolT(EcoMb e) {
        return e.mb.typesBool();
    }

    private static SourceExpr cveT(EcoMb e) {
        return e.mb.typesCve();
    }

    private static SourceExpr codeT(EcoMb e) {
        return e.mb.typesCode();
    }

    private static SourceExpr lvalT(EcoMb e) {
        return t(e, "lval");
    }

    private static SourceExpr lmodT(EcoMb e) {
        return t(e, "lmod");
    }

    private static SourceExpr xxT(EcoMb e) {
        return e.typesExprContext();
    }

    private static SourceExpr locT(EcoMb e) {
        return e.typesLoc();
    }

    private static SourceExpr instsT(EcoMb e) {
        return e.listType(e.mb.evalValue());
    }

    private static SourceExpr lv(EcoMb e, String name) {
        return e.lowerV(name);
    }

    private static SourceExpr t(EcoMb e, String obj) {
        return e.fld(e.v(obj), "type");
    }

    private static SourceExpr lt(EcoMb e, String obj) {
        return e.lower(t(e, obj));
    }

    private static SourceExpr mk(EcoMb e, String obj, EcoMb.P... fields) {
        return e.call(e.lower(e.fld(e.v(obj), "new")), e.rec(fields));
    }

    private static SourceExpr mkArg(EcoMb e, String obj, SourceExpr arg) {
        return e.call(e.lower(e.fld(e.v(obj), "new")), arg);
    }

    private static SourceExpr hf(EcoMb e, String fn, SourceExpr arg) {
        return e.call(e.lowerV(fn), arg);
    }

    private static SourceExpr hf(EcoMb e, String fn, EcoMb.P... fields) {
        return hf(e, fn, e.rec(fields));
    }

    private static SourceExpr im0(EcoMb e, String iface, String method, SourceExpr recv) {
        return e.call(e.fld(lt(e, iface), method), recv);
    }

    private static SourceExpr bim(EcoMb e, SourceExpr iface, String method, SourceExpr recv, SourceExpr arg) {
        return e.call(e.fld(iface, method), e.tup(recv, arg));
    }

    private static SourceExpr bim0(EcoMb e, SourceExpr iface, String method, SourceExpr recv) {
        return e.call(e.fld(iface, method), recv);
    }

    private static SourceExpr lookup(EcoMb e, SourceExpr xx, SourceExpr typ, SourceExpr ifaceType) {
        return e.call(e.mb.evalAspectLookup(), e.tup(typ, ifaceType, xx));
    }

    private static SourceExpr has(EcoMb e, SourceExpr xx, SourceExpr typ, String iface) {
        return e.fld(lookup(e, xx, typ, lt(e, iface)), "is_some");
    }

    private static SourceExpr asp(EcoMb e, SourceExpr xx, SourceExpr typ, String iface) {
        return e.fld(lookup(e, xx, typ, lt(e, iface)), "value");
    }

    private static SourceExpr hasB(EcoMb e, SourceExpr xx, SourceExpr typ, SourceExpr ifaceType) {
        return e.fld(lookup(e, xx, typ, ifaceType), "is_some");
    }

    private static SourceExpr acc(EcoMb e, SourceExpr xx, SourceExpr typ, String iface, String method) {
        return im0(e, iface, method, asp(e, xx, typ, iface));
    }

    private static SourceExpr cveValue(EcoMb e, SourceExpr cveTP) {
        return e.fld(e.call(e.fld(cveTP, "value"), e.tup()), "value");
    }

    private static SourceExpr cveHas(EcoMb e, SourceExpr cveTP) {
        return e.fld(e.call(e.fld(cveTP, "value"), e.tup()), "is_some");
    }

    private static SourceExpr cveEnded(EcoMb e, SourceExpr cveTP) {
        return e.call(e.fld(cveTP, "control_ended"), e.tup());
    }

    private static SourceExpr cveCode(EcoMb e, SourceExpr cveTP) {
        return e.fld(cveTP, "code");
    }

    private static SourceExpr cveType(EcoMb e, SourceExpr cveTP) {
        return e.call(e.fld(e.mb.evalValue(), "type"), cveValue(e, cveTP));
    }

    private static SourceExpr castValue(EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr typ, SourceExpr cv) {
        return e.call(
                e.evalBuiltin("cast_value"), e.rec(e.p("xx", xx), e.p("loc", loc), e.p("type", typ), e.p("cv", cv)));
    }

    private static SourceExpr codeNone(EcoMb e, SourceExpr loc) {
        return e.call(e.evalBuiltin("code_none"), loc);
    }

    private static SourceExpr cvOf(EcoMb e, SourceExpr codeTP, SourceExpr value) {
        return e.call(e.mb.evalCodevalue(), e.rec(e.p("code", codeTP), e.p("value", value)));
    }

    private static SourceExpr cvNo(EcoMb e, SourceExpr codeTP) {
        return e.call(e.evalBuiltin("codevalue_no_value"), codeTP);
    }

    private static SourceExpr okCve(EcoMb e, SourceExpr v) {
        return e.eresultOk(e.mb.typesCve(), v);
    }

    private static SourceExpr errCve(EcoMb e, SourceExpr loc, SourceExpr msg) {
        return e.eresultErr(e.mb.typesCve(), loc, msg);
    }

    private static SourceExpr errCve(EcoMb e, SourceExpr loc, String msg) {
        return errCve(e, loc, e.s(msg));
    }

    private static SourceExpr errsT(EcoMb e) {
        return e.listType(e.mb.builtin("types", "expr_err"));
    }

    private static SourceExpr tryEr(EcoMb e, SourceExpr call, SourceExpr resultType) {
        return e.mb.tryCatchFinally(
                e.eresultOk(resultType, call),
                Opt.some(new TestMb.Catch("__errs", errsT(e), e.eresultErrs(resultType, e.v("__errs")))),
                Opt.none());
    }

    private static SourceExpr unwrapCveT(EcoMb e, SourceExpr res) {
        return e.matchOf(res, "__r", Opt.none(), e.p("ok", e.v("__r")), e.p("errs", e.mb.throw_(e.v("__r"))));
    }

    private static SourceExpr throwErr(EcoMb e, SourceExpr loc, SourceExpr msg) {
        return e.matchOf(
                errCve(e, loc, msg),
                "__x",
                Opt.none(),
                e.p("ok", e.mb.panic(e.s("unreachable"))),
                e.p("errs", e.mb.throw_(e.v("__x"))));
    }

    private static SourceExpr throwErr(EcoMb e, SourceExpr loc, String msg) {
        return throwErr(e, loc, e.s(msg));
    }

    private static SourceExpr panicErrs(EcoMb e, String what, SourceExpr errs) {
        return e.seq(
                e.let("__m", e.listNew(e.mb.typesString())),
                e.foreach(errs, "__e", e.push(e.v("__m"), e.callM(e.v("__e"), "message", e.tup()))),
                e.mb.panic(e.cat(e.s("llvm: " + what + ": "), e.join(e.v("__m"), "; "))));
    }

    private static SourceExpr mustV(EcoMb e, SourceExpr res, String what) {
        return e.matchOf(res, "__r", Opt.none(), e.p("ok", e.v("__r")), e.p("errs", panicErrs(e, what, e.v("__r"))));
    }

    private static SourceExpr panicErr(EcoMb e, String what, SourceExpr loc, SourceExpr msg) {
        return e.matchOf(
                e.eresultErr(e.mb.evalValue(), loc, msg),
                "__x",
                Opt.none(),
                e.p("ok", e.mb.panic(e.s("unreachable"))),
                e.p("errs", panicErrs(e, what, e.v("__x"))));
    }

    private static SourceExpr xx(EcoMb e) {
        return e.fld(e.v("arg"), "xx");
    }

    private static SourceExpr loc(EcoMb e) {
        return e.fld(e.v("arg"), "loc");
    }

    private static SourceExpr a(EcoMb e, String... keys) {
        return e.fld(e.v("arg"), keys);
    }

    private static SourceExpr in(EcoMb e, String... keys) {
        return e.fld(e.v("inner"), keys);
    }

    private static SourceExpr outer(EcoMb e) {
        return e.v("outer");
    }

    private static SourceExpr mod(EcoMb e) {
        return in(e, "mod");
    }

    private static SourceExpr reinterpret(EcoMb e, SourceExpr value, String obj) {
        return e.call(e.mb.mustReinterpret(), e.tup(value, lt(e, obj)));
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
        return e.listNew(e.mb.evalValue());
    }

    private static SourceExpr inst(EcoMb e, String variant, EcoMb.P... fields) {
        return mk(e, "linst", e.p("k", e.call(e.lowerV("li_" + variant), e.rec(fields))));
    }

    private static SourceExpr push(EcoMb e, SourceExpr list, SourceExpr element) {
        return e.push(list, element);
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

    private static SourceExpr dfalse(EcoMb e) {
        return e.mb.as(e.b(false), boolT(e));
    }

    private static SourceExpr isLlvm(EcoMb e, SourceExpr xx, SourceExpr cv) {
        return hf(e, "h_is_llvm", e.p("xx", xx), e.p("cv", cv));
    }

    private static SourceExpr isConst(EcoMb e, SourceExpr xx, SourceExpr typ) {
        return hasB(e, xx, typ, e.mb.evalAspectInt());
    }

    private static SourceExpr isEqual(EcoMb e, SourceExpr xx, SourceExpr self, SourceExpr other) {
        return bim(e, e.mb.evalType(), "is_equal", self, e.rec(e.p("xx", xx), e.p("other", other)));
    }

    private static SourceExpr typeAspect(EcoMb e, SourceExpr xx, SourceExpr self, SourceExpr key) {
        return bim(e, e.mb.evalType(), "aspect", self, e.rec(e.p("xx", xx), e.p("key", key)));
    }

    private static SourceExpr typeCanCastFrom(EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr self, SourceExpr src) {
        return bim(
                e,
                e.mb.evalType(),
                "can_cast_from",
                self,
                e.rec(e.p("xx", xx), e.p("loc", loc), e.p("source_type", src)));
    }

    private static SourceExpr toStr(EcoMb e, SourceExpr v) {
        return e.toStr(v);
    }

    private static SourceExpr num(EcoMb e, double v) {
        return e.intOf(v);
    }

    private static SourceExpr optNoneType(EcoMb e) {
        return e.optNone(e.mb.evalType());
    }

    private static SourceExpr opId(EcoMb e, String op) {
        return e.evalBuiltin("aspect_operator_" + op + "_id");
    }

    private static SourceExpr opIface(EcoMb e, String op) {
        return e.evalBuiltin("operator_" + op);
    }

    private static SourceExpr aspId(EcoMb e, String name) {
        return e.evalBuiltin("aspect_" + name + "_id");
    }

    private static SourceExpr aspIface(EcoMb e, String name) {
        return e.evalBuiltin("aspect_" + name);
    }

    private static void defIface(EcoMb e, TList<SourceExpr> out, String name, EcoMb.P... methods) {
        out.add(e.let(name, e.callM0(e.v("d"), "interface")));
        out.add(e.callM(e.v(name), "set_is_aspect", e.b(true)));
        out.add(e.let(name + "_id", e.callM0(e.v(name), "aspect_id")));
        for (var m : methods) {
            out.add(e.callM(
                    e.v(name),
                    "func",
                    e.rec(e.p("name", e.s(m.key())), e.p("arg", e.tup()), e.p("ret", e.retSome(m.value())))));
        }
    }

    private static void defObj(EcoMb e, TList<SourceExpr> out, String name, String desc, SourceExpr innerType) {
        out.add(e.let(name, e.defObject(e.v("d"), desc)));
        out.add(e.callM(e.v(name), "set_inner", innerType));
    }

    private static void defVariant(EcoMb e, TList<SourceExpr> out, String tag, SourceExpr typeRef) {
        out.add(e.let(
                "li_" + tag,
                e.callM(e.v("linst_u"), "variant", e.rec(e.p("tag", e.s(tag)), e.p("type_ref", typeRef)))));
    }

    private static void method(
            EcoMb e,
            TList<SourceExpr> out,
            String obj,
            String name,
            SourceExpr argType,
            SourceExpr ret,
            SourceExpr body) {
        out.add(e.methodImpl(e.v(obj), name, argType, e.retSome(ret), e.stage(body)));
    }

    private static SourceExpr retM(EcoMb e, SourceExpr value) {
        return e.mb.return_("m", value);
    }

    private static SourceExpr body(EcoMb e, SourceExpr... exprs) {
        return e.mb.labeled("m", e.seq(exprs));
    }

    private static SourceExpr unwrapM(EcoMb e, SourceExpr res, SourceExpr retInner) {
        return e.matchOf(
                res,
                "__r",
                Opt.none(),
                e.p("ok", e.v("__r")),
                e.p("errs", retM(e, e.eresultErrs(retInner, e.v("__r")))));
    }

    private static SourceExpr unwrapCveM(EcoMb e, SourceExpr res) {
        return unwrapM(e, res, e.mb.typesCve());
    }

    private static SourceExpr andB(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.ifElse(a, b, e.b(false));
    }

    private static SourceExpr orB(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.ifElse(e.not(a), b, e.b(true));
    }

    private static SourceExpr isTargetCode(EcoMb e, SourceExpr codeTP) {
        return e.eq(e.matchOf(codeTP, "__c", Opt.none(), e.p("none", num(e, 0)), e.p("target", num(e, 1))), num(e, 1));
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

    private static SourceExpr loadInst(EcoMb e, SourceExpr res, SourceExpr ty, SourceExpr ptr) {
        return inst(e, "load", e.p("res", res), e.p("ty", ty), e.p("ptr", ptr));
    }

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        out.add(e.let(name + "_b", e.defFunc(e.v("d"), "llvm." + name)));
        out.add(e.let(name, e.fld(e.v(name + "_b"), "func")));
        e.fnSigs.put(name, new SourceExpr[] {argType, e.retSome(ret)});
        return e.v(name);
    }

    private static void fnImpl(EcoMb e, TList<SourceExpr> out, String name, SourceExpr bodyExpr) {
        var sig = e.fnSigs.getOpt(name).optUnwrap();
        e.fnSigs.remove(name);
        out.add(e.funcImpl(e.v(name + "_b"), sig[0], sig[1], e.stage(bodyExpr)));
    }

    private static void accessor(EcoMb e, TList<SourceExpr> x, String obj, String field, SourceExpr typ) {
        method(e, x, obj, field, e.tup(), typ, in(e, field));
    }

    private static void intType(
            EcoMb e, TList<SourceExpr> x, String name, double bits, boolean signed, double min, double max) {
        method(
                e,
                x,
                "lmod",
                name,
                e.tup(),
                e.mb.evalType(),
                mk(
                        e,
                        "lt_int",
                        e.p("bits", num(e, bits)),
                        e.p("signed", e.b(signed)),
                        e.p("min", e.n(min)),
                        e.p("max", e.n(max)),
                        e.p("mod", outer(e))));
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
                        e.not(e.eq(e.fld(e.v("st"), "pend"), e.dstr(""))),
                        push(e, e.v("out"), e.cat(e.s("  br label %"), e.fld(e.v("st"), "pend")))),
                e.mb.set(e.fld(e.v("st"), "pend"), e.dstr("")));
    }

    private static SourceExpr renderTerminator(EcoMb e, SourceExpr... lineParts) {
        return e.seq(push(e, e.v("out"), e.cat(lineParts)), e.mb.set(e.fld(e.v("st"), "open"), num(e, 0)));
    }

    private static SourceExpr p(EcoMb e, String key) {
        return e.fld(e.v("p"), key);
    }

    private static SourceExpr irOf(EcoMb e, SourceExpr ty) {
        return e.callM(outer(e), "ir", ty);
    }

    private static SourceExpr typedOf(EcoMb e, SourceExpr lvalTP) {
        return e.callM(outer(e), "typed", lvalTP);
    }

    private static void selfType(
            EcoMb e,
            TList<SourceExpr> x,
            String obj,
            SourceExpr desc,
            SourceExpr dispatch,
            SourceExpr canCastFrom,
            SourceExpr castFrom,
            SourceExpr canCastTo,
            SourceExpr castTo,
            SourceExpr isEqual) {
        x.add(e.implInterface(e.v(obj), e.mb.evalValue(), e.p("type", outer(e))));
        x.add(e.implInterface(e.v(obj), aspIface(e, "type"), e.p("type", body(e, e.ret(outer(e))))));
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalType(),
                e.p("desc", desc),
                e.p(
                        "aspect",
                        e.ifElse(
                                e.mb.operatorEq(e.fld(e.v("arg"), "key"), aspId(e, "type")),
                                e.optSome(e.mb.typesAspect(), e.v("outer")),
                                dispatch)),
                e.p("can_cast_from", canCastFrom),
                e.p("cast_from", castFrom),
                e.p("can_cast_to", canCastTo),
                e.p("cast_to", castTo),
                e.p("is_equal", isEqual)));
    }

    private static SourceExpr noCastTo(EcoMb e) {
        return e.mb.as(e.b(false), boolT(e));
    }

    private static SourceExpr castToPanic(EcoMb e) {
        return e.mb.panic(e.s("llvm: cast_to unreachable"));
    }

    private static SourceExpr srcType(EcoMb e) {
        return a(e, "source_type");
    }

    private static SourceExpr srcBits(EcoMb e, String marker) {
        return acc(e, xx(e), srcType(e), marker, "bits");
    }

    private static SourceExpr cvBits(EcoMb e, SourceExpr st, String marker) {
        return acc(e, xx(e), st, marker, "bits");
    }

    private static SourceExpr binopBody(EcoMb e, SourceExpr op) {
        return hf(
                e,
                "h_binop",
                e.p("xx", xx(e)),
                e.p("loc", loc(e)),
                e.p("mod", mod(e)),
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
                e.p("mod", mod(e)),
                e.p("ty", outer(e)),
                e.p("op", op),
                e.p("a", a(e, "base")),
                e.p("b", a(e, "reference")));
    }

    private static SourceExpr signedOp(EcoMb e, String signed, String unsigned) {
        return e.ifElse(in(e, "signed"), e.dstr(signed), e.dstr(unsigned));
    }

    private static void op(EcoMb e, TList<SourceExpr> x, String obj, String op, SourceExpr bodyExpr) {
        x.add(e.implInterface(e.v(obj), opIface(e, op), e.p(op, body(e, e.ret(bodyExpr)))));
    }

    private static SourceExpr conv(EcoMb e, SourceExpr op, SourceExpr from, SourceExpr to) {
        return okCve(e, hf(e, "h_conv", e.p("mod", mod(e)), e.p("op", op), e.p("a", from), e.p("to", to)));
    }

    private static SourceExpr retag(EcoMb e, SourceExpr cv, SourceExpr ty) {
        return okCve(e, hf(e, "h_retag", e.p("mod", mod(e)), e.p("cv", cv), e.p("ty", ty)));
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
                                                e.p("mod", mod(e)),
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
                                                                e.p("mod", mod(e)),
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
        parts.add(e.let("v", unwrapCveM(e, tryEr(e, ssa(e, xx(e), loc(e), mod(e), e.v("cv")), cveT(e)))));
        parts.add(e.let("st", cveType(e, e.v("v"))));
        for (var c : cases) parts.add(c);
        parts.add(e.ret(errCve(e, loc(e), "cannot convert to " + what)));
        x.add(e.implInterface(
                e.v(obj),
                aspIface(e, "function"),
                e.p("call", body(e, parts.toArray(SourceExpr[]::new))),
                e.p("exception_type", optNoneType(e))));
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

    private static void markerType(
            EcoMb e, TList<SourceExpr> x, String obj, String desc, String marker, SourceExpr dispatch) {
        selfType(
                e,
                x,
                obj,
                e.dstr(desc),
                dispatch,
                body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                body(e, e.ret(okCve(e, a(e, "cv")))),
                noCastTo(e),
                castToPanic(e),
                body(e, e.ret(has(e, xx(e), a(e, "other"), marker))));
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
                                                        e.optSome(e.mb.evalType(), cveType(e, e.v("bv")))),
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

    private static SourceExpr vocT(EcoMb e) {
        return e.mb.builtin("types", "value_object_const");
    }

    private static SourceExpr lookupV(EcoMb e, SourceExpr xx, SourceExpr typ, SourceExpr ifaceType) {
        return e.fld(lookup(e, xx, typ, ifaceType), "value");
    }

    private static SourceExpr gval(EcoMb e, SourceExpr cv) {
        return hf(e, "h_gval", e.p("cv", cv), e.p("base", in(e, "base")), e.p("mem", in(e, "mem")));
    }

    private static TestMb.Branch rtBranch(EcoMb e, SourceExpr id, SourceExpr value) {
        return new TestMb.Branch(e.mb.operatorEq(e.fld(e.v("arg"), "key"), id), value);
    }

    private static SourceExpr rtDispatch(EcoMb e, SourceExpr inner) {
        var delegated = new SourceExpr[] {
            e.mb.evalAspectBindDynamicId(),
            e.mb.evalAspectConsumeDynamicId(),
            aspId(e, "initial_bind_dynamic"),
            e.mb.evalAspectDropDynamicId()
        };
        var branches = new TList<TestMb.Branch>();
        branches.add(rtBranch(e, aspId(e, "lift"), e.optSome(e.mb.typesAspect(), outer(e))));
        branches.add(rtBranch(e, aspId(e, "bind"), e.optSome(e.mb.typesAspect(), outer(e))));
        for (var id : delegated) {
            branches.add(rtBranch(e, id, typeAspect(e, xx(e), lt(e, "lref"), a(e, "key"))));
        }
        return e.mb.ifOf(branches, Opt.some(inner));
    }

    private static SourceExpr rtCanCastFrom(EcoMb e) {
        return body(
                e,
                e.ret(orB(e, isEqual(e, xx(e), lt(e, "lref"), srcType(e)), isEqual(e, xx(e), outer(e), srcType(e)))));
    }

    private static SourceExpr rtCastFrom(EcoMb e) {
        return body(
                e,
                e.ifOnly(
                        e.not(isEqual(e, xx(e), lt(e, "lref"), cveType(e, a(e, "cv")))), retM(e, okCve(e, a(e, "cv")))),
                e.ifOnly(
                        isTargetCode(e, cveCode(e, a(e, "cv"))),
                        retM(
                                e,
                                e.call(
                                        e.evalBuiltin("codevalue_dynamic"),
                                        e.rec(
                                                e.p("xx", xx(e)),
                                                e.p("loc", loc(e)),
                                                e.p("code", cveCode(e, a(e, "cv"))),
                                                e.p("type", outer(e)))))),
                e.ret(okCve(
                        e,
                        cvOf(
                                e,
                                codeNone(e, loc(e)),
                                e.call(
                                        e.evalBuiltin("value_object_const"),
                                        e.rec(
                                                e.p("type", outer(e)),
                                                e.p(
                                                        "data",
                                                        e.fld(
                                                                e.call(
                                                                        e.mb.mustReinterpret(),
                                                                        e.tup(cveValue(e, a(e, "cv")), vocT(e))),
                                                                "data"))))))));
    }

    private static void rtLiftImpl(EcoMb e, TList<SourceExpr> x, String obj) {
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectLift(),
                e.p(
                        "lift",
                        e.eresultOk(
                                e.mb.evalValue(),
                                e.call(
                                        e.evalBuiltin("value_object_const"),
                                        e.rec(e.p("type", outer(e)), e.p("data", a(e, "external_host_value"))))))));
    }

    private static void rtBindImpl(EcoMb e, TList<SourceExpr> x, String obj) {
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectBind(),
                e.p(
                        "bind",
                        bim(
                                e,
                                e.mb.evalAspectBind(),
                                "bind",
                                lookupV(e, xx(e), lt(e, "lref"), e.mb.evalAspectBind()),
                                e.rec(e.p("xx", xx(e)), e.p("loc", loc(e)), e.p("cv", a(e, "cv")))))));
    }

    private static void rtType(
            EcoMb e, TList<SourceExpr> x, String obj, String desc, String marker, SourceExpr dispatchInner) {
        selfType(
                e,
                x,
                obj,
                e.dstr(desc),
                rtDispatch(e, dispatchInner),
                rtCanCastFrom(e),
                rtCastFrom(e),
                noCastTo(e),
                castToPanic(e),
                body(e, e.ret(has(e, xx(e), a(e, "other"), marker))));
        rtLiftImpl(e, x, obj);
        rtBindImpl(e, x, obj);
    }

    public static SourceExpr module(EcoMb e) {
        var x = new TList<SourceExpr>();
        x.add(e.let("d", e.define()));
        {
            defIface(e, x, "ltype", e.p("ir", str(e)), e.p("ssa", ty(e)));
            defIface(e, x, "lint", e.p("bits", intT(e)), e.p("signed", boolT(e)));
            defIface(e, x, "lfloat", e.p("bits", intT(e)));
            defIface(e, x, "lptr", e.p("pointee", ty(e)));
            defIface(e, x, "larr", e.p("elem", ty(e)), e.p("count", intT(e)));
            defIface(e, x, "lvoid");
            defIface(e, x, "lslot", e.p("base", ty(e)), e.p("ptr", str(e)));
            defIface(e, x, "lunit_m");
            defIface(e, x, "lrange_m", e.p("elem", ty(e)), e.p("mod", e.mb.evalValue()));
            defIface(e, x, "lloop_m");
            defIface(e, x, "lgtype_m", e.p("base", ty(e)), e.p("mem", boolT(e)));
            defIface(e, x, "lrange_fn_m");

            x.add(e.let("lsrc_u", e.callM0(e.v("d"), "union")));
            for (var tag : new String[] {"ssa", "mem"}) {
                x.add(e.let(
                        "ls_" + tag,
                        e.callM(e.v("lsrc_u"), "variant", e.rec(e.p("tag", e.s(tag)), e.p("type_ref", str(e))))));
            }
            defObj(e, x, "lval", "llvm value", e.rec(e.p("ty", ty(e)), e.p("source", t(e, "lsrc_u"))));
            defObj(e, x, "lfield", "llvm field", e.rec(e.p("name", str(e)), e.p("ty", ty(e))));
            defIface(e, x, "lstruct", e.p("name", str(e)), e.p("fields", e.listType(e.mb.evalValue())));
            defIface(
                    e,
                    x,
                    "lfn",
                    e.p("params", e.listType(ty(e))),
                    e.p("ret", ty(e)),
                    e.p("varargs", boolT(e)),
                    e.p("sig", str(e)));
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
                            e.p("idx", e.listType(e.mb.evalValue()))));
            defVariant(
                    e,
                    x,
                    "call",
                    e.rec(
                            e.p("res", e.call(e.mb.typesOption(), str(e))),
                            e.p("fn", lvalT(e)),
                            e.p("args", e.listType(e.mb.evalValue()))));
            defVariant(e, x, "ret", e.rec(e.p("val", e.call(e.mb.typesOption(), e.mb.evalValue()))));

            defObj(e, x, "linst", "llvm instruction", e.rec(e.p("k", t(e, "linst_u"))));
            defIface(e, x, "linst_m");
            defIface(e, x, "lglobal_m");
            defIface(e, x, "lmod_m");
            defIface(e, x, "llabel_m");
            defIface(e, x, "lfield_m");
            defObj(e, x, "lcode", "llvm code", e.rec(e.p("insts", e.listType(e.mb.evalValue()))));
            x.add(e.implInterface(e.v("lcode"), e.mb.builtin("eval", "code_dyn")));
            method(e, x, "lcode", "insts", e.tup(), e.listType(e.mb.evalValue()), in(e, "insts"));

            defObj(
                    e,
                    x,
                    "lmod",
                    "llvm module",
                    e.rec(
                            e.p("n", intT(e)),
                            e.p("xx", e.call(e.mb.typesOption(), e.typesExprContext())),
                            e.p("structs", e.listType(ty(e))),
                            e.p("externs", e.listType(e.mb.evalValue())),
                            e.p("globals", e.listType(e.mb.evalValue())),
                            e.p("constants", e.listType(e.mb.evalValue())),
                            e.p("slots", e.listType(e.mb.evalValue())),
                            e.p("funcs", e.listType(str(e))),
                            e.p("labels", e.listType(e.mb.evalValue()))));
            defObj(e, x, "ltarget", "llvm target", e.rec(e.p("mod", lmodT(e))));
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
                            e.p("max", e.typesDouble()),
                            e.p("mod", lmodT(e))));
            defObj(e, x, "lt_float", "llvm float type", e.rec(e.p("bits", intT(e)), e.p("mod", lmodT(e))));
            defObj(e, x, "lt_ptr", "llvm pointer type", e.rec(e.p("pointee", ty(e)), e.p("mod", lmodT(e))));
            defObj(
                    e,
                    x,
                    "lt_arr",
                    "llvm array type",
                    e.rec(e.p("elem", ty(e)), e.p("count", intT(e)), e.p("mod", lmodT(e))));
            defObj(
                    e,
                    x,
                    "lt_struct",
                    "llvm struct type",
                    e.rec(e.p("name", str(e)), e.p("fields", e.listType(e.mb.evalValue())), e.p("mod", lmodT(e))));
            defObj(
                    e,
                    x,
                    "lt_fn",
                    "llvm function type",
                    e.rec(
                            e.p("params", e.listType(ty(e))),
                            e.p("ret", ty(e)),
                            e.p("varargs", boolT(e)),
                            e.p("mod", lmodT(e))));
            defObj(e, x, "lt_void", "llvm void type", e.rec(e.p("mod", lmodT(e))));
            defObj(
                    e,
                    x,
                    "lt_slot",
                    "llvm slot type",
                    e.rec(e.p("base", ty(e)), e.p("ptr", str(e)), e.p("mod", lmodT(e))));
            defObj(e, x, "lrange_t", "llvm range type", e.rec(e.p("elem", ty(e)), e.p("mod", lmodT(e))));
            defObj(
                    e,
                    x,
                    "lrange",
                    "llvm range",
                    e.rec(e.p("t", ty(e)), e.p("code", e.mb.typesCode()), e.p("n", lvalT(e))));
            defObj(e, x, "lrange_fn_t", "llvm range function type", e.rec(e.p("t", ty(e)), e.p("elem", ty(e))));
            defObj(e, x, "lloop_t", "llvm loop type", e.rec(e.p("mod", lmodT(e))));
            defObj(e, x, "lref", "llvm reference", e.rec(e.p("ref", str(e))));
            defObj(
                    e,
                    x,
                    "lt_global",
                    "llvm global type",
                    e.rec(e.p("base", ty(e)), e.p("mem", boolT(e)), e.p("mod", lmodT(e))));
        }
        {
            fnDef(e, x, "h_insts", codeT(e), instsT(e));
            fnDef(e, x, "h_code", instsT(e), codeT(e));
            x.add(e.let("h_append_b", e.defFunc(e.v("d"), "llvm.h_append")));
            x.add(e.let("h_append", e.fld(e.v("h_append_b"), "func")));
            e.fnSigs.put(
                    "h_append",
                    new SourceExpr[] {e.rec(e.p("out", instsT(e)), e.p("code", codeT(e))), e.mb.optNoneType()});
            fnDef(e, x, "h_is_llvm", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), e.mb.typesBool());
            fnDef(
                    e,
                    x,
                    "h_ssa",
                    e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("mod", lmodT(e)), e.p("cv", cveT(e))),
                    cveT(e));
            x.add(e.callM(e.v("h_ssa_b"), "set_exception_type", errsT(e)));
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
            x.add(e.callM(e.v("h_operand_b"), "set_exception_type", errsT(e)));
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
            x.add(e.callM(e.v("h_set_b"), "set_exception_type", errsT(e)));
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
            x.add(e.callM(e.v("h_binop_b"), "set_exception_type", errsT(e)));
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
            x.add(e.callM(e.v("h_cmp_b"), "set_exception_type", errsT(e)));
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
            x.add(e.callM(e.v("h_unop_b"), "set_exception_type", errsT(e)));
            fnDef(
                    e,
                    x,
                    "h_store_new",
                    e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("mod", lmodT(e)), e.p("cv", cveT(e))),
                    cveT(e));
            x.add(e.callM(e.v("h_store_new_b"), "set_exception_type", errsT(e)));
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
                            e.p("idx", e.listType(e.mb.evalValue()))),
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
            x.add(e.callM(e.v("h_gep_field_b"), "set_exception_type", errsT(e)));
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
            x.add(e.callM(e.v("h_gep_index_b"), "set_exception_type", errsT(e)));
            fnDef(e, x, "h_gref", cveT(e), str(e));
            fnDef(e, x, "h_gval", e.rec(e.p("cv", cveT(e)), e.p("base", ty(e)), e.p("mem", boolT(e))), cveT(e));
            fnImpl(
                    e,
                    x,
                    "h_gref",
                    body(
                            e,
                            e.ret(e.callM(
                                    reinterpret(
                                            e,
                                            e.fld(
                                                    e.call(
                                                            e.mb.mustReinterpret(),
                                                            e.tup(cveValue(e, e.v("arg")), vocT(e))),
                                                    "data"),
                                            "lref"),
                                    "ref",
                                    e.tup()))));
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
                            e.let("n", e.rec(e.p("v", num(e, 0)))),
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
                            e.ret(cvOf(
                                    e,
                                    codeOf(e, e.v("out")),
                                    lvalNew(e, e.callM(a(e, "mod"), "i1", e.tup()), e.v("r"))))));
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
                                    lvalNew(
                                            e,
                                            mk(e, "lt_ptr", e.p("pointee", a(e, "base")), e.p("mod", a(e, "mod"))),
                                            e.v("r"))))));
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
                            e.let("st", e.rec(e.p("i", num(e, 0)), e.p("idx", num(e, -1)), e.p("fty", optNoneType(e)))),
                            e.foreach(
                                    e.v("fields"),
                                    "f0",
                                    e.seq(
                                            e.let("f", reinterpret(e, e.v("f0"), "lfield")),
                                            e.ifOnly(
                                                    e.eq(e.callM(e.v("f"), "name", e.tup()), a(e, "name")),
                                                    e.seq(
                                                            e.mb.set(e.fld(e.v("st"), "idx"), e.fld(e.v("st"), "i")),
                                                            e.mb.set(
                                                                    e.fld(e.v("st"), "fty"),
                                                                    e.optSome(
                                                                            e.mb.evalType(),
                                                                            e.callM(e.v("f"), "ty", e.tup()))))),
                                            e.incr(e.fld(e.v("st"), "i")))),
                            e.ifOnly(
                                    e.eq(e.fld(e.v("st"), "idx"), num(e, -1)),
                                    throwErr(e, loc(e), e.cat(e.s("no struct field named "), a(e, "name")))),
                            e.let("i32", e.callM(a(e, "mod"), "i32", e.tup())),
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
                                                            e.mb.evalValue(),
                                                            lvalNew(e, e.v("i32"), e.dstr("0")),
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
                                            unwrapCveT(
                                                    e,
                                                    castValue(
                                                            e,
                                                            xx(e),
                                                            loc(e),
                                                            e.callM(a(e, "mod"), "i32", e.tup()),
                                                            a(e, "key"))))),
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
                                            e.p("idx", e.listNew(e.mb.evalValue(), lvalOf(e, e.v("k")))))),
                            appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                            e.let("r", lvRef(e, lvalOf(e, e.v("g")))),
                            e.ret(cvOf(e, codeOf(e, e.v("out")), lvalMem(e, a(e, "base"), e.v("r"))))));
        }
        {
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
                    e.matchOf(in(e, "source"), "s", Opt.none(), e.p("mem", num(e, 1)), e.p("ssa", num(e, 0))));
            x.add(e.implInterface(e.v("lval"), e.mb.evalValue(), e.p("type", in(e, "ty"))));
            accessor(e, x, "lfield", "name", str(e));
            accessor(e, x, "lfield", "ty", ty(e));
            accessor(e, x, "lref", "ref", str(e));
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
                    e.mb.optNoneType(),
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
                                            e.p("init", e.dstr("zeroinitializer")))),
                            e.ret(e.v("name"))));
            method(
                    e,
                    x,
                    "lmod",
                    "slot_type",
                    e.rec(e.p("base", ty(e)), e.p("ptr", str(e))),
                    ty(e),
                    mk(e, "lt_slot", e.p("base", a(e, "base")), e.p("ptr", a(e, "ptr")), e.p("mod", outer(e))));
            method(
                    e,
                    x,
                    "lmod",
                    "label_of",
                    e.typesJumpKey(),
                    str(e),
                    e.seq(
                            e.let("st", e.rec(e.p("name", e.dstr("")), e.p("hit", num(e, 0)))),
                            e.foreach(
                                    in(e, "labels"),
                                    "l0",
                                    e.seq(
                                            e.let("l", reinterpret(e, e.v("l0"), "llabel")),
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
                    e.seq(
                            e.let("lk", lookup(e, in(e, "xx", "value"), e.v("arg"), lt(e, "ltype"))),
                            e.ifOnly(
                                    e.not(e.fld(e.v("lk"), "is_some")),
                                    e.mb.panic(e.cat(
                                            e.s("llvm: not an llvm ty(e): "),
                                            bim0(e, e.mb.evalType(), "desc", e.v("arg"))))),
                            e.ret(im0(e, "ltype", "ir", e.fld(e.v("lk"), "value")))));
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
                    e.seq(
                            e.let("parts", e.listNew(str(e))),
                            e.foreach(
                                    im0(e, "lfn", "params", asp(e, in(e, "xx", "value"), e.v("arg"), "lfn")),
                                    "p",
                                    push(e, e.v("parts"), irOf(e, e.v("p")))),
                            e.ifOnly(
                                    im0(e, "lfn", "varargs", asp(e, in(e, "xx", "value"), e.v("arg"), "lfn")),
                                    push(e, e.v("parts"), e.dstr("..."))),
                            e.ret(e.join(e.v("parts"), ", "))));
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
                    e.listType(e.mb.evalValue()),
                    e.listType(str(e)),
                    e.seq(
                            e.let("out", e.listNew(str(e))),
                            e.let("st", e.rec(e.p("open", num(e, 1)), e.p("pend", e.dstr("")))),
                            e.let("refs", e.mapNew(str(e), e.mb.typesIntBox())),
                            e.foreach(
                                    e.v("arg"),
                                    "r0",
                                    e.matchOf(
                                            e.callM(reinterpret(e, e.v("r0"), "linst"), "k", e.tup()),
                                            "p",
                                            Opt.some(e.seq()),
                                            e.p("label", e.callM(e.v("refs"), "put", e.tup(p(e, "name"), num(e, 0)))))),
                            e.foreach(
                                    e.v("arg"),
                                    "r1",
                                    e.matchOf(
                                            e.callM(reinterpret(e, e.v("r1"), "linst"), "k", e.tup()),
                                            "p",
                                            Opt.some(e.seq()),
                                            e.p("br", countRef(e, p(e, "name"))),
                                            e.p("condbr", e.seq(countRef(e, p(e, "t")), countRef(e, p(e, "f")))))),
                            e.foreach(
                                    e.v("arg"),
                                    "i0",
                                    e.seq(
                                            e.let("k", e.callM(reinterpret(e, e.v("i0"), "linst"), "k", e.tup())),
                                            e.ifOnly(
                                                    e.eq(
                                                            e.matchOf(
                                                                    e.v("k"),
                                                                    "q",
                                                                    Opt.some(num(e, 0)),
                                                                    e.p("label", num(e, 1))),
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
                                                                            e.mb.set(
                                                                                    e.fld(e.v("st"), "pend"),
                                                                                    e.dstr("")),
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
                                                                                    typedOf(
                                                                                            e,
                                                                                            reinterpret(
                                                                                                    e, e.v("ix"),
                                                                                                    "lval")))),
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
                                                                                    typedOf(
                                                                                            e,
                                                                                            reinterpret(
                                                                                                    e, e.v("ax"),
                                                                                                    "lval")))),
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
                                                                            typedOf(
                                                                                    e,
                                                                                    reinterpret(
                                                                                            e,
                                                                                            e.fld(p(e, "val"), "value"),
                                                                                            "lval")),
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
                    str(e),
                    e.seq(
                            e.let("out", e.listNew(str(e))),
                            e.foreach(
                                    in(e, "structs"),
                                    "s",
                                    e.seq(
                                            e.let("parts", e.listNew(str(e))),
                                            e.foreach(
                                                    acc(e, in(e, "xx", "value"), e.v("s"), "lstruct", "fields"),
                                                    "f0",
                                                    push(
                                                            e,
                                                            e.v("parts"),
                                                            irOf(
                                                                    e,
                                                                    e.callM(
                                                                            reinterpret(e, e.v("f0"), "lfield"),
                                                                            "ty",
                                                                            e.tup())))),
                                            push(
                                                    e,
                                                    e.v("out"),
                                                    e.cat(
                                                            irOf(e, e.v("s")),
                                                            e.s(" = type { "),
                                                            e.join(e.v("parts"), ", "),
                                                            e.s(" }"))))),
                            e.foreach(
                                    in(e, "externs"),
                                    "f0",
                                    e.seq(
                                            e.let("f", reinterpret(e, e.v("f0"), "lval")),
                                            push(
                                                    e,
                                                    e.v("out"),
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
                                                            e.s(")"))))),
                            e.foreach(
                                    in(e, "globals"),
                                    "g0",
                                    push(
                                            e,
                                            e.v("out"),
                                            e.cat(
                                                    e.callM(reinterpret(e, e.v("g0"), "lglobal"), "name", e.tup()),
                                                    e.s(" = global "),
                                                    irOf(
                                                            e,
                                                            e.callM(
                                                                    reinterpret(e, e.v("g0"), "lglobal"),
                                                                    "ty",
                                                                    e.tup())),
                                                    e.s(" "),
                                                    e.callM(reinterpret(e, e.v("g0"), "lglobal"), "init", e.tup())))),
                            e.foreach(
                                    in(e, "constants"),
                                    "g0",
                                    push(
                                            e,
                                            e.v("out"),
                                            e.cat(
                                                    e.callM(reinterpret(e, e.v("g0"), "lglobal"), "name", e.tup()),
                                                    e.s(" = constant "),
                                                    irOf(
                                                            e,
                                                            e.callM(
                                                                    reinterpret(e, e.v("g0"), "lglobal"),
                                                                    "ty",
                                                                    e.tup())),
                                                    e.s(" "),
                                                    e.callM(reinterpret(e, e.v("g0"), "lglobal"), "init", e.tup())))),
                            e.foreach(
                                    in(e, "slots"),
                                    "g0",
                                    push(
                                            e,
                                            e.v("out"),
                                            e.cat(
                                                    e.callM(reinterpret(e, e.v("g0"), "lglobal"), "name", e.tup()),
                                                    e.s(" = internal global "),
                                                    irOf(
                                                            e,
                                                            e.callM(
                                                                    reinterpret(e, e.v("g0"), "lglobal"),
                                                                    "ty",
                                                                    e.tup())),
                                                    e.s(" "),
                                                    e.callM(reinterpret(e, e.v("g0"), "lglobal"), "init", e.tup())))),
                            e.foreach(in(e, "funcs"), "f", push(e, e.v("out"), e.v("f"))),
                            e.ret(e.cat(e.join(e.v("out"), "\n"), e.s("\n")))));

            intType(e, x, "i1", 1, false, 0, 1);
            intType(e, x, "i8", 8, true, -128, 127);
            intType(e, x, "i16", 16, true, -32768, 32767);
            intType(e, x, "i32", 32, true, -2147483648.0, 2147483647.0);
            intType(e, x, "i64", 64, true, -9223372036854775808.0, 9223372036854775807.0);
            intType(e, x, "u8", 8, false, 0, 255);
            intType(e, x, "u16", 16, false, 0, 65535);
            intType(e, x, "u32", 32, false, 0, 4294967295.0);
            intType(e, x, "u64", 64, false, 0, 18446744073709551615.0);
            method(
                    e,
                    x,
                    "lmod",
                    "f32",
                    e.tup(),
                    ty(e),
                    mk(e, "lt_float", e.p("bits", num(e, 32)), e.p("mod", outer(e))));
            method(
                    e,
                    x,
                    "lmod",
                    "f64",
                    e.tup(),
                    ty(e),
                    mk(e, "lt_float", e.p("bits", num(e, 64)), e.p("mod", outer(e))));
            method(e, x, "lmod", "void", e.tup(), ty(e), mk(e, "lt_void", e.p("mod", outer(e))));
            method(
                    e,
                    x,
                    "lmod",
                    "ptr",
                    ty(e),
                    ty(e),
                    mk(e, "lt_ptr", e.p("pointee", e.v("arg")), e.p("mod", outer(e))));
            method(
                    e,
                    x,
                    "lmod",
                    "array",
                    e.rec(e.p("elem", ty(e)), e.p("count", e.mb.typesInt())),
                    ty(e),
                    mk(e, "lt_arr", e.p("elem", a(e, "elem")), e.p("count", a(e, "count")), e.p("mod", outer(e))));
            method(
                    e,
                    x,
                    "lmod",
                    "field",
                    e.rec(e.p("name", str(e)), e.p("ty", ty(e))),
                    t(e, "lfield"),
                    mk(e, "lfield", e.p("name", a(e, "name")), e.p("ty", a(e, "ty"))));
            method(
                    e,
                    x,
                    "lmod",
                    "struct",
                    e.rec(e.p("name", str(e)), e.p("fields", e.listType(e.mb.evalValue()))),
                    ty(e),
                    e.seq(
                            e.let(
                                    "t",
                                    mk(
                                            e,
                                            "lt_struct",
                                            e.p("name", a(e, "name")),
                                            e.p("fields", a(e, "fields")),
                                            e.p("mod", outer(e)))),
                            push(e, in(e, "structs"), e.v("t")),
                            e.ret(e.v("t"))));
            method(
                    e,
                    x,
                    "lmod",
                    "fn",
                    e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e))),
                    ty(e),
                    mk(
                            e,
                            "lt_fn",
                            e.p("params", a(e, "params")),
                            e.p("ret", a(e, "ret")),
                            e.p("varargs", e.b(false)),
                            e.p("mod", outer(e))));
            method(
                    e,
                    x,
                    "lmod",
                    "fn_varargs",
                    e.rec(e.p("params", e.listType(ty(e))), e.p("ret", ty(e))),
                    ty(e),
                    mk(
                            e,
                            "lt_fn",
                            e.p("params", a(e, "params")),
                            e.p("ret", a(e, "ret")),
                            e.p("varargs", e.b(true)),
                            e.p("mod", outer(e))));
            method(e, x, "lmod", "null", e.tup(), t(e, "lref"), mk(e, "lref", e.p("ref", e.s("null"))));
            method(
                    e,
                    x,
                    "lmod",
                    "extern",
                    e.rec(e.p("name", str(e)), e.p("ty", ty(e))),
                    t(e, "lref"),
                    e.seq(
                            e.let("gname", e.cat(e.s("@"), a(e, "name"))),
                            push(e, in(e, "externs"), lvalNew(e, a(e, "ty"), e.v("gname"))),
                            e.ret(mk(e, "lref", e.p("ref", e.v("gname"))))));
            method(
                    e,
                    x,
                    "lmod",
                    "range",
                    ty(e),
                    ty(e),
                    mk(
                            e,
                            "lrange_fn_t",
                            e.p("t", mk(e, "lrange_t", e.p("elem", e.v("arg")), e.p("mod", outer(e)))),
                            e.p("elem", e.v("arg"))));
            method(e, x, "lmod", "loop", e.tup(), ty(e), mk(e, "lloop_t", e.p("mod", outer(e))));
            method(
                    e,
                    x,
                    "lmod",
                    "global_t",
                    e.rec(e.p("ty", ty(e)), e.p("mem", boolT(e))),
                    ty(e),
                    mk(e, "lt_global", e.p("base", a(e, "ty")), e.p("mem", a(e, "mem")), e.p("mod", outer(e))));
            method(e, x, "lmod", "ref", str(e), t(e, "lref"), mk(e, "lref", e.p("ref", e.v("arg"))));
            method(
                    e,
                    x,
                    "lmod",
                    "constant",
                    e.rec(e.p("ty", ty(e)), e.p("value", e.mb.typesExpr())),
                    t(e, "lref"),
                    body(
                            e,
                            e.let("loc", e.call(e.evalBuiltin("expr_loc"), a(e, "value"))),
                            e.let("xx", in(e, "xx", "value")),
                            e.let(
                                    "r",
                                    mustV(
                                            e,
                                            e.call(
                                                    e.mb.evalConstEval(),
                                                    e.rec(e.p("xx", e.v("xx")), e.p("staged", a(e, "value")))),
                                            "compile failed")),
                            e.let(
                                    "v",
                                    mustV(
                                            e,
                                            tryEr(
                                                    e,
                                                    operand(e, e.v("xx"), e.v("loc"), outer(e), a(e, "ty"), e.v("r")),
                                                    cveT(e)),
                                            "compile failed")),
                            e.ifOnly(
                                    isTargetCode(e, cveCode(e, e.v("v"))),
                                    panicErr(e, "compile failed", e.v("loc"), e.s("constant value is not a constant"))),
                            e.ret(mk(e, "lref", e.p("ref", lvRef(e, lvalOf(e, e.v("v"))))))));
            for (var konst : new boolean[] {false, true}) {
                method(
                        e,
                        x,
                        "lmod",
                        konst ? "global_const" : "global",
                        e.rec(e.p("name", str(e)), e.p("ty", ty(e)), e.p("init", e.mb.typesExpr())),
                        t(e, "lref"),
                        body(
                                e,
                                e.let("loc", e.call(e.evalBuiltin("expr_loc"), a(e, "init"))),
                                e.let("xx", in(e, "xx", "value")),
                                e.let("gname", e.cat(e.s("@"), a(e, "name"))),
                                e.let(
                                        "r",
                                        mustV(
                                                e,
                                                e.call(
                                                        e.mb.evalConstEval(),
                                                        e.rec(e.p("xx", e.v("xx")), e.p("staged", a(e, "init")))),
                                                "compile failed")),
                                e.let(
                                        "v",
                                        mustV(
                                                e,
                                                tryEr(
                                                        e,
                                                        operand(
                                                                e,
                                                                e.v("xx"),
                                                                e.v("loc"),
                                                                outer(e),
                                                                a(e, "ty"),
                                                                e.v("r")),
                                                        cveT(e)),
                                                "compile failed")),
                                e.ifOnly(
                                        isTargetCode(e, cveCode(e, e.v("v"))),
                                        panicErr(
                                                e,
                                                "compile failed",
                                                e.v("loc"),
                                                e.s("global initializer must be a constant"))),
                                push(
                                        e,
                                        in(e, konst ? "constants" : "globals"),
                                        mk(
                                                e,
                                                "lglobal",
                                                e.p("name", e.v("gname")),
                                                e.p("ty", a(e, "ty")),
                                                e.p("init", lvRef(e, lvalOf(e, e.v("v")))))),
                                e.ret(mk(e, "lref", e.p("ref", e.v("gname"))))));
            }

            method(
                    e,
                    x,
                    "lmod",
                    "func",
                    e.rec(
                            e.p("name", str(e)),
                            e.p("params", e.listType(e.mb.evalValue())),
                            e.p("ret", ty(e)),
                            e.p("body", e.mb.typesExpr())),
                    t(e, "lref"),
                    body(
                            e,
                            e.let("loc", e.call(e.evalBuiltin("expr_loc"), a(e, "body"))),
                            e.let("ptypes", e.listNew(ty(e))),
                            e.foreach(
                                    a(e, "params"),
                                    "p0",
                                    push(
                                            e,
                                            e.v("ptypes"),
                                            e.callM(reinterpret(e, e.v("p0"), "lfield"), "ty", e.tup()))),
                            e.let(
                                    "fnt",
                                    mk(
                                            e,
                                            "lt_fn",
                                            e.p("params", e.v("ptypes")),
                                            e.p("ret", a(e, "ret")),
                                            e.p("varargs", e.b(false)),
                                            e.p("mod", outer(e)))),
                            e.let("fnv", lvalNew(e, e.v("fnt"), e.cat(e.s("@"), a(e, "name")))),
                            e.let("out", instsNew(e)),
                            e.let("entries", e.mapNew(e.mb.typesConstkey(), e.mb.evalBinding())),
                            e.let("pi", e.rec(e.p("i", num(e, 0)))),
                            e.let("sig", e.listNew(str(e))),
                            e.foreach(
                                    a(e, "params"),
                                    "p0",
                                    e.seq(
                                            e.let("p", reinterpret(e, e.v("p0"), "lfield")),
                                            e.let("pty", e.callM(e.v("p"), "ty", e.tup())),
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
                                                            e.callM(e.v("p"), "name", e.tup()),
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
                            e.let("is_void", has(e, e.v("ctx"), a(e, "ret"), "lvoid")),
                            e.ifOnly(
                                    andB(
                                            e,
                                            e.not(e.v("ended")),
                                            andB(e, e.v("is_void"), isLlvm(e, e.v("ctx"), e.v("r")))),
                                    panicErr(
                                            e, "compile failed", e.v("loc"), e.s("void function body yields a value"))),
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
                                                                    a(e, "ret"),
                                                                    e.v("r")),
                                                            cveT(e)),
                                                    okCve(e, e.v("r"))),
                                            "compile failed")),
                            appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                            e.ifOnly(
                                    andB(e, e.not(e.v("ended")), e.v("is_void")),
                                    push(e, e.v("out"), inst(e, "ret", e.p("val", e.optNone(e.mb.evalValue()))))),
                            e.ifOnly(
                                    e.v("need_cast"),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    "ret",
                                                    e.p("val", e.optSome(e.mb.evalValue(), lvalOf(e, e.v("v"))))))),
                            e.let("lines", e.callM(outer(e), "render_insts", e.v("out"))),
                            push(
                                    e,
                                    in(e, "funcs"),
                                    e.cat(
                                            e.s("define "),
                                            irOf(e, a(e, "ret")),
                                            e.s(" @"),
                                            a(e, "name"),
                                            e.s("("),
                                            e.join(e.v("sig"), ", "),
                                            e.s(") {\n"),
                                            e.join(e.v("lines"), "\n"),
                                            e.s("\n}"))),
                            e.ret(mk(e, "lref", e.p("ref", e.cat(e.s("@"), a(e, "name")))))));
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
                    e.v("lt_int"), t(e, "lint"), e.p("bits", in(e, "bits")), e.p("signed", in(e, "signed"))));
            op(e, x, "lt_int", "add", tryEr(e, binopBody(e, e.dstr("add")), cveT(e)));
            op(e, x, "lt_int", "sub", tryEr(e, binopBody(e, e.dstr("sub")), cveT(e)));
            op(e, x, "lt_int", "mul", tryEr(e, binopBody(e, e.dstr("mul")), cveT(e)));
            op(e, x, "lt_int", "div", tryEr(e, binopBody(e, signedOp(e, "sdiv", "udiv")), cveT(e)));
            op(e, x, "lt_int", "mod", tryEr(e, binopBody(e, signedOp(e, "srem", "urem")), cveT(e)));
            op(e, x, "lt_int", "and", tryEr(e, binopBody(e, e.dstr("and")), cveT(e)));
            op(e, x, "lt_int", "or", tryEr(e, binopBody(e, e.dstr("or")), cveT(e)));
            op(e, x, "lt_int", "xor", tryEr(e, binopBody(e, e.dstr("xor")), cveT(e)));
            op(e, x, "lt_int", "shl", tryEr(e, binopBody(e, e.dstr("shl")), cveT(e)));
            op(e, x, "lt_int", "shr", tryEr(e, binopBody(e, signedOp(e, "ashr", "lshr")), cveT(e)));
            op(e, x, "lt_int", "ushr", tryEr(e, binopBody(e, e.dstr("lshr")), cveT(e)));
            op(e, x, "lt_int", "eq", tryEr(e, cmpBody(e, e.dstr("icmp eq")), cveT(e)));
            op(e, x, "lt_int", "ne", tryEr(e, cmpBody(e, e.dstr("icmp ne")), cveT(e)));
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
                                                    e.p("mod", mod(e)),
                                                    e.p("ty", outer(e)),
                                                    e.p("op", e.dstr("sub")),
                                                    e.p("a", literal(e, outer(e), e.dstr("0"))),
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
                                                    e.p("mod", mod(e)),
                                                    e.p("ty", outer(e)),
                                                    e.p("op", e.dstr("xor")),
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
                                                            conv(e, e.dstr("trunc"), e.v("v"), outer(e)),
                                                            conv(
                                                                    e,
                                                                    e.ifElse(
                                                                            acc(e, xx(e), e.v("st"), "lint", "signed"),
                                                                            e.dstr("sext"),
                                                                            e.dstr("zext")),
                                                                    e.v("v"),
                                                                    outer(e))))))),
                    whenSt(e, "lfloat", conv(e, signedOp(e, "fptosi", "fptoui"), e.v("v"), outer(e))),
                    whenSt(e, "lptr", conv(e, e.dstr("ptrtoint"), e.v("v"), outer(e))));

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
                            e.ret(conv(e, e.dstr("fpext"), a(e, "cv"), outer(e)))),
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
            op(e, x, "lt_float", "add", tryEr(e, binopBody(e, e.dstr("fadd")), cveT(e)));
            op(e, x, "lt_float", "sub", tryEr(e, binopBody(e, e.dstr("fsub")), cveT(e)));
            op(e, x, "lt_float", "mul", tryEr(e, binopBody(e, e.dstr("fmul")), cveT(e)));
            op(e, x, "lt_float", "div", tryEr(e, binopBody(e, e.dstr("fdiv")), cveT(e)));
            op(e, x, "lt_float", "mod", tryEr(e, binopBody(e, e.dstr("frem")), cveT(e)));
            op(e, x, "lt_float", "eq", tryEr(e, cmpBody(e, e.dstr("fcmp oeq")), cveT(e)));
            op(e, x, "lt_float", "ne", tryEr(e, cmpBody(e, e.dstr("fcmp une")), cveT(e)));
            op(e, x, "lt_float", "lt", tryEr(e, cmpBody(e, e.dstr("fcmp olt")), cveT(e)));
            op(e, x, "lt_float", "le", tryEr(e, cmpBody(e, e.dstr("fcmp ole")), cveT(e)));
            op(e, x, "lt_float", "gt", tryEr(e, cmpBody(e, e.dstr("fcmp ogt")), cveT(e)));
            op(e, x, "lt_float", "ge", tryEr(e, cmpBody(e, e.dstr("fcmp oge")), cveT(e)));
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
                                                    e.p("mod", mod(e)),
                                                    e.p("ty", outer(e)),
                                                    e.p("op", e.dstr("fneg")),
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
                                                            conv(e, e.dstr("fptrunc"), e.v("v"), outer(e)),
                                                            conv(e, e.dstr("fpext"), e.v("v"), outer(e))))))));

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
            x.add(e.implInterface(e.v("lt_ptr"), t(e, "ltype"), e.p("ir", e.dstr("ptr")), e.p("ssa", outer(e))));
            x.add(e.implInterface(e.v("lt_ptr"), t(e, "lptr"), e.p("pointee", in(e, "pointee"))));
            op(e, x, "lt_ptr", "eq", tryEr(e, cmpBody(e, e.dstr("icmp eq")), cveT(e)));
            op(e, x, "lt_ptr", "ne", tryEr(e, cmpBody(e, e.dstr("icmp ne")), cveT(e)));
            op(e, x, "lt_ptr", "lt", tryEr(e, cmpBody(e, e.dstr("icmp ult")), cveT(e)));
            op(e, x, "lt_ptr", "le", tryEr(e, cmpBody(e, e.dstr("icmp ule")), cveT(e)));
            op(e, x, "lt_ptr", "gt", tryEr(e, cmpBody(e, e.dstr("icmp ugt")), cveT(e)));
            op(e, x, "lt_ptr", "ge", tryEr(e, cmpBody(e, e.dstr("icmp uge")), cveT(e)));
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
                                                    e, tryEr(e, ssa(e, xx(e), loc(e), mod(e), a(e, "base")), cveT(e)))),
                                    e.let(
                                            "i",
                                            unwrapCveM(
                                                    e,
                                                    e.ifElse(
                                                            isLlvm(e, xx(e), a(e, "reference")),
                                                            tryEr(
                                                                    e,
                                                                    ssa(e, xx(e), loc(e), mod(e), a(e, "reference")),
                                                                    cveT(e)),
                                                            castValue(
                                                                    e,
                                                                    xx(e),
                                                                    loc(e),
                                                                    e.callM(mod(e), "i64", e.tup()),
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
                                                    e.p("mod", mod(e)),
                                                    e.p("base", in(e, "pointee")),
                                                    e.p("ptr", lvRef(e, lvalOf(e, e.v("p")))),
                                                    e.p("idx", e.listNew(e.mb.evalValue(), lvalOf(e, e.v("i")))))),
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
                    whenSt(e, "lint", conv(e, e.dstr("inttoptr"), e.v("v"), outer(e))),
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
                                                    e, tryEr(e, ssa(e, xx(e), loc(e), mod(e), a(e, "self")), cveT(e)))),
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
                                                                            e.p("mod", mod(e)),
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
                                                                            e.p("mod", mod(e)),
                                                                            e.p("base", in(e, "pointee")),
                                                                            e.p("ptr", e.v("p")),
                                                                            e.p("key", a(e, "key"))),
                                                                    cveT(e))))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                                    e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), lvalOf(e, e.v("g"))))))),
                    e.p("type_opt", optNoneType(e)),
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
                            e.let("st2", e.rec(e.p("n", num(e, 0)), e.p("bad", num(e, 0)))),
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
                                                                            mod(e),
                                                                            in(e, "elem"),
                                                                            e.v("ec")),
                                                                    cveT(e)))),
                                            e.ifOnly(
                                                    isTargetCode(e, cveCode(e, e.v("v"))),
                                                    e.mb.set(e.fld(e.v("st2"), "bad"), num(e, 1))),
                                            push(e, e.v("parts"), e.callM(mod(e), "typed", lvalOf(e, e.v("v")))),
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
                                    e.callM(mod(e), "ir", in(e, "elem")),
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
                                    e.let("i32", e.callM(mod(e), "i32", e.tup())),
                                    e.let(
                                            "k",
                                            unwrapCveM(
                                                    e,
                                                    e.ifElse(
                                                            isLlvm(e, xx(e), a(e, "key")),
                                                            tryEr(
                                                                    e,
                                                                    ssa(e, xx(e), loc(e), mod(e), a(e, "key")),
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
                                                    e.p("mod", mod(e)),
                                                    e.p("base", outer(e)),
                                                    e.p("ptr", e.v("p")),
                                                    e.p(
                                                            "idx",
                                                            e.listNew(
                                                                    e.mb.evalValue(),
                                                                    lvalNew(e, e.v("i32"), e.dstr("0")),
                                                                    lvalOf(e, e.v("k")))))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("g"))),
                                    e.let("r", lvRef(e, lvalOf(e, e.v("g")))),
                                    e.ret(okCve(
                                            e, cvOf(e, codeOf(e, e.v("out")), lvalMem(e, in(e, "elem"), e.v("r"))))))),
                    e.p("type_opt", optNoneType(e)),
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
                            e.let("bad", e.rec(e.p("v", num(e, 0)))),
                            e.foreach(
                                    in(e, "fields"),
                                    "f0",
                                    e.seq(
                                            e.let("f", reinterpret(e, e.v("f0"), "lfield")),
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
                                                                            mod(e),
                                                                            e.callM(e.v("f"), "ty", e.tup()),
                                                                            e.v("ec")),
                                                                    cveT(e)))),
                                            e.ifOnly(
                                                    isTargetCode(e, cveCode(e, e.v("v"))),
                                                    e.mb.set(e.fld(e.v("bad"), "v"), num(e, 1))),
                                            push(e, e.v("parts"), e.callM(mod(e), "typed", lvalOf(e, e.v("v")))))),
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
                    e.v("lt_struct"), t(e, "ltype"), e.p("ir", e.cat(e.s("%"), in(e, "name"))), e.p("ssa", outer(e))));
            x.add(e.implInterface(
                    e.v("lt_struct"), t(e, "lstruct"), e.p("name", in(e, "name")), e.p("fields", in(e, "fields"))));
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
                                                                    e.p("mod", mod(e)),
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
                    e.p("type_opt", optNoneType(e)),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));

            selfType(
                    e,
                    x,
                    "lt_fn",
                    e.dstr("fn"),
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
                            e.let("on", e.rec(e.p("v", num(e, 0)))),
                            e.foreach(
                                    im0(e, "lfn", "params", asp(e, xx(e), a(e, "other"), "lfn")),
                                    "p",
                                    e.seq(
                                            e.callM(e.v("om"), "put", e.tup(toStr(e, e.fld(e.v("on"), "v")), e.v("p"))),
                                            e.incr(e.fld(e.v("on"), "v")))),
                            e.let("st", e.rec(e.p("i", num(e, 0)), e.p("same", num(e, 1)))),
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
                                    e.callM(mod(e), "ir", in(e, "ret")),
                                    e.s(" ("),
                                    e.callM(mod(e), "params_ir", outer(e)),
                                    e.s(")"))),
                    e.p("ssa", outer(e))));
            x.add(e.implInterface(
                    e.v("lt_fn"),
                    t(e, "lfn"),
                    e.p("params", in(e, "params")),
                    e.p("ret", in(e, "ret")),
                    e.p("varargs", in(e, "varargs")),
                    e.p("sig", e.callM(mod(e), "params_ir", outer(e)))));
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
                                    e.let("args", e.listNew(e.mb.evalValue())),
                                    e.let("pmap", e.mapNew(str(e), ty(e))),
                                    e.let("pn", e.rec(e.p("v", num(e, 0)))),
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
                                    e.let("st", e.rec(e.p("i", num(e, 0)))),
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
                                                                                                    mod(e),
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
                                                                                                    mod(e), e.v("ec")),
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
                                    e.let("r", fresh(e, mod(e))),
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
                    e.p("exception_type", optNoneType(e))));

            selfType(
                    e,
                    x,
                    "lt_void",
                    e.dstr("void"),
                    e.aspectDispatch(lv(e, "ltype_id"), lv(e, "lvoid_id")),
                    body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e)))),
                    body(e, e.ret(okCve(e, a(e, "cv")))),
                    noCastTo(e),
                    castToPanic(e),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "lvoid"))));
            x.add(e.implInterface(e.v("lt_void"), t(e, "ltype"), e.p("ir", e.dstr("void")), e.p("ssa", outer(e))));
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
                                                    operand(e, xx(e), loc(e), mod(e), in(e, "base"), a(e, "cv")),
                                                    cveT(e)))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                            push(e, e.v("out"), storeInst(e, lvalOf(e, e.v("v")), in(e, "ptr"))),
                            e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), lvalMem(e, outer(e), in(e, "ptr")))))),
                    body(e, e.ret(typeCanCastFrom(e, xx(e), loc(e), a(e, "dest_type"), in(e, "base")))),
                    body(
                            e,
                            e.let("r", fresh(e, mod(e))),
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
                    e.p("ir", e.callM(mod(e), "ir", in(e, "base"))),
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
                                e.ifElse(in(e, "mem"), e.dstr("global("), e.dstr("const(")),
                                bim0(e, ty(e), "desc", in(e, "base")),
                                e.s(")")),
                        rtDispatch(e, e.aspectDispatch(ids.toArray(SourceExpr[]::new))),
                        rtCanCastFrom(e),
                        rtCastFrom(e),
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
                rtBindImpl(e, x, "lt_global");
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
                                                        e.p("mod", mod(e)),
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
                        e.p("exception_type", optNoneType(e))));
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
            x.add(e.implInterface(
                    e.v("lrange_t"), t(e, "lrange_m"), e.p("elem", in(e, "elem")), e.p("mod", in(e, "mod"))));
            x.add(e.implInterface(
                    e.v("lrange_t"),
                    aspIface(e, "iterable"),
                    e.p("element_type", in(e, "elem")),
                    e.p(
                            "loop_binding",
                            body(
                                    e,
                                    e.let("s", e.callM(mod(e), "slot", in(e, "elem"))),
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
                                    e.let("mod", mod(e)),
                                    e.let("elem", in(e, "elem")),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, a(e, "base"))),
                                    e.let("rv", reinterpret(e, cveValue(e, a(e, "base")), "lrange")),
                                    appendCode(e, e.v("out"), e.callM(e.v("rv"), "code", e.tup())),
                                    e.let("ns", e.callM(e.v("mod"), "slot", e.v("elem"))),
                                    push(e, e.v("out"), storeInst(e, e.callM(e.v("rv"), "n", e.tup()), e.v("ns"))),
                                    e.let("lb", reinterpret(e, a(e, "loop"), "lbinding")),
                                    e.let("cs", e.callM(e.v("lb"), "ptr", e.tup())),
                                    push(e, e.v("out"), storeInst(e, lvalNew(e, e.v("elem"), e.dstr("0")), e.v("cs"))),
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
                                                    e.p(
                                                            "cond",
                                                            lvalNew(e, e.callM(e.v("mod"), "i1", e.tup()), e.v("c"))),
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
                                                                    e.p("op", e.dstr("add")),
                                                                    e.p("a", lvalNew(e, e.v("elem"), e.v("i2"))),
                                                                    e.p("b", lvalNew(e, e.v("elem"), e.dstr("1"))))),
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            storeInst(
                                                                    e, lvalNew(e, e.v("elem"), e.v("i3")), e.v("cs"))),
                                                    push(e, e.v("out"), brInst(e, e.v("head"))))),
                                    push(e, e.v("out"), labelInst(e, e.v("end"))),
                                    e.ret(okCve(e, cvNo(e, codeOf(e, e.v("out")))))))));
            rtType(
                    e,
                    x,
                    "lrange_fn_t",
                    "range constructor",
                    "lrange_fn_m",
                    e.aspectDispatch(lv(e, "lrange_fn_m_id"), aspId(e, "function")));
            x.add(e.implInterface(e.v("lrange_fn_t"), t(e, "lrange_fn_m")));
            x.add(e.implInterface(
                    e.v("lrange_fn_t"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let(
                                            "a2",
                                            unwrapCveM(
                                                    e,
                                                    tryEr(
                                                            e,
                                                            operand(
                                                                    e,
                                                                    xx(e),
                                                                    loc(e),
                                                                    reinterpret(
                                                                            e,
                                                                            acc(
                                                                                    e,
                                                                                    xx(e),
                                                                                    in(e, "t"),
                                                                                    "lrange_m",
                                                                                    "mod"),
                                                                            "lmod"),
                                                                    in(e, "elem"),
                                                                    a(e, "argument")),
                                                            cveT(e)))),
                                    e.ret(okCve(
                                            e,
                                            cvOf(
                                                    e,
                                                    codeNone(e, loc(e)),
                                                    mk(
                                                            e,
                                                            "lrange",
                                                            e.p("t", in(e, "t")),
                                                            e.p("code", cveCode(e, e.v("a2"))),
                                                            e.p("n", lvalOf(e, e.v("a2"))))))))),
                    e.p("exception_type", optNoneType(e))));

            rtType(e, x, "lloop_t", "loop", "lloop_m", e.aspectDispatch(lv(e, "lloop_m_id"), aspId(e, "iterable")));
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
                                    e.let("mod", mod(e)),
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
                                    e.let("st", e.rec(e.p("any", num(e, 0)))),
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
                                                    e.mb.evalValue(),
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
                                                            e.mb.evalValue(),
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
                                                            e.mb.evalValue(),
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
                                                    e.callM(
                                                            mod(e),
                                                            "slot_type",
                                                            e.rec(
                                                                    e.p("base", lvTy(e, e.v("lv"))),
                                                                    e.p("ptr", lvMem(e, e.v("lv"))))),
                                                    lvMem(e, e.v("lv"))))))),
                    e.p("stage", errCve(e, loc(e), "llvm target: nested staging is not supported")),
                    e.p(
                            "if",
                            body(
                                    e,
                                    e.let("mod", mod(e)),
                                    e.let("out", instsNew(e)),
                                    e.let("i1", e.callM(e.v("mod"), "i1", e.tup())),
                                    e.let("end", freshLabel(e, e.v("mod"))),
                                    e.let(
                                            "st",
                                            e.rec(
                                                    e.p("ended", num(e, 1)),
                                                    e.p("has", num(e, 0)),
                                                    e.p("novalue", num(e, 0)),
                                                    e.p("slot", e.dstr("")),
                                                    e.p("ty", optNoneType(e)))),
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
                e.retSome(t(e, "lmod")),
                e.stage(e.seq(
                        e.let(
                                "m",
                                mk(
                                        e,
                                        "lmod",
                                        e.p("n", num(e, 0)),
                                        e.p("xx", e.optNone(e.typesExprContext())),
                                        e.p("structs", e.listNew(ty(e))),
                                        e.p("externs", e.listNew(e.mb.evalValue())),
                                        e.p("globals", e.listNew(e.mb.evalValue())),
                                        e.p("constants", e.listNew(e.mb.evalValue())),
                                        e.p("slots", e.listNew(e.mb.evalValue())),
                                        e.p("funcs", e.listNew(str(e))),
                                        e.p("labels", e.listNew(e.mb.evalValue())))),
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
        x.add(e.finish(e.v("d")));
        x.add(e.ret(e.rec(
                e.p("new", e.fld(e.v("llvm_b"), "func")),
                e.p("mod_t", t(e, "lmod")),
                e.p("ref_t", t(e, "lref")),
                e.p("field_t", t(e, "lfield")))));
        return e.mb.seq(x);
    }
}
