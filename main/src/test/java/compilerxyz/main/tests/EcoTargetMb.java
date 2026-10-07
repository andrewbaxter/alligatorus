package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

final class EcoTargetMb {
    private EcoTargetMb() {}

    static SourceExpr ty(EcoMb e) {
        return e.mb.evalType();
    }

    static SourceExpr str(EcoMb e) {
        return e.mb.typesString();
    }

    static SourceExpr intT(EcoMb e) {
        return e.mb.typesInt();
    }

    static SourceExpr boolT(EcoMb e) {
        return e.mb.typesBool();
    }

    static SourceExpr cveT(EcoMb e) {
        return e.mb.typesCve();
    }

    static SourceExpr codeT(EcoMb e) {
        return e.mb.typesCode();
    }

    static SourceExpr constEvalIn(EcoMb e, SourceExpr xx, SourceExpr staged) {
        return e.call(e.mb.evalConstEval(), e.rec(e.p("xx", xx), e.p("staged", staged)));
    }

    static SourceExpr hostCve(EcoMb e, SourceExpr staged) {
        return unwrapCveT(e, constEvalIn(e, xx(e), staged));
    }

    static SourceExpr strCve(EcoMb e, SourceExpr str) {
        return hostCve(e, e.stage(e.lower(str)));
    }

    static SourceExpr graft(EcoMb e, SourceExpr cv) {
        return e.mb.graft(cv);
    }

    static SourceExpr keyStr(EcoMb e, SourceExpr key) {
        return e.call(e.evalBuiltin("constkey_to_str"), key);
    }

    static SourceExpr tyDesc(EcoMb e, SourceExpr ty) {
        return bim0(e, e.mb.evalType(), "desc", ty);
    }

    static SourceExpr countLet(EcoMb e, String counter) {
        return e.let(counter, e.rec(e.p("v", e.intOf(0))));
    }

    static SourceExpr countLoop(EcoMb e, String counter, SourceExpr list) {
        return e.foreach(list, "_", e.incr(e.fld(e.v(counter), "v")));
    }

    static SourceExpr xxT(EcoMb e) {
        return e.typesExprContext();
    }

    static SourceExpr locT(EcoMb e) {
        return e.typesLoc();
    }

    static SourceExpr lv(EcoMb e, String name) {
        return e.lowerV(name);
    }

    static SourceExpr t(EcoMb e, String obj) {
        return e.fld(e.v(obj), "type");
    }

    static SourceExpr lt(EcoMb e, String obj) {
        return e.lower(t(e, obj));
    }

    static SourceExpr mk(EcoMb e, String obj, EcoMb.P... fields) {
        return e.call(e.lower(e.fld(e.v(obj), "new")), e.rec(fields));
    }

    static SourceExpr mkArg(EcoMb e, String obj, SourceExpr arg) {
        return e.call(e.lower(e.fld(e.v(obj), "new")), arg);
    }

    static SourceExpr hf(EcoMb e, String fn, SourceExpr arg) {
        return e.call(e.lowerV(fn), arg);
    }

    static SourceExpr hf(EcoMb e, String fn, EcoMb.P... fields) {
        return hf(e, fn, e.rec(fields));
    }

    static SourceExpr im0(EcoMb e, String iface, String method, SourceExpr recv) {
        return e.call(e.fld(lt(e, iface), method), recv);
    }

    static SourceExpr bim(EcoMb e, SourceExpr iface, String method, SourceExpr recv, SourceExpr arg) {
        return e.call(e.fld(iface, method), e.tup(recv, arg));
    }

    static SourceExpr bim0(EcoMb e, SourceExpr iface, String method, SourceExpr recv) {
        return e.call(e.fld(iface, method), recv);
    }

    static SourceExpr lookup(EcoMb e, SourceExpr xx, SourceExpr typ, SourceExpr ifaceType) {
        return e.call(e.mb.evalAspectLookup(), e.tup(typ, ifaceType, xx));
    }

    static SourceExpr has(EcoMb e, SourceExpr xx, SourceExpr typ, String iface) {
        return e.fld(lookup(e, xx, typ, lt(e, iface)), "is_some");
    }

    static SourceExpr asp(EcoMb e, SourceExpr xx, SourceExpr typ, String iface) {
        return e.fld(lookup(e, xx, typ, lt(e, iface)), "value");
    }

    static SourceExpr hasB(EcoMb e, SourceExpr xx, SourceExpr typ, SourceExpr ifaceType) {
        return e.fld(lookup(e, xx, typ, ifaceType), "is_some");
    }

    static SourceExpr acc(EcoMb e, SourceExpr xx, SourceExpr typ, String iface, String method) {
        return im0(e, iface, method, asp(e, xx, typ, iface));
    }

    static SourceExpr cveValue(EcoMb e, SourceExpr cveTP) {
        return e.fld(e.call(e.fld(cveTP, "value"), e.tup()), "value");
    }

    static SourceExpr cveHas(EcoMb e, SourceExpr cveTP) {
        return e.fld(e.call(e.fld(cveTP, "value"), e.tup()), "is_some");
    }

    static SourceExpr cveEnded(EcoMb e, SourceExpr cveTP) {
        return e.call(e.fld(cveTP, "control_ended"), e.tup());
    }

    static SourceExpr cveCode(EcoMb e, SourceExpr cveTP) {
        return e.fld(cveTP, "code");
    }

    static SourceExpr cveType(EcoMb e, SourceExpr cveTP) {
        return e.call(e.fld(e.mb.evalValue(), "type"), cveValue(e, cveTP));
    }

    static SourceExpr castValue(EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr typ, SourceExpr cv) {
        return e.call(
                e.evalBuiltin("cast_value"), e.rec(e.p("xx", xx), e.p("loc", loc), e.p("type", typ), e.p("cv", cv)));
    }

    static SourceExpr codeNone(EcoMb e, SourceExpr loc) {
        return e.call(e.evalBuiltin("code_none"), loc);
    }

    static SourceExpr cvOf(EcoMb e, SourceExpr codeTP, SourceExpr value) {
        return e.call(e.mb.evalCodevalue(), e.rec(e.p("code", codeTP), e.p("value", value)));
    }

    static SourceExpr cvNo(EcoMb e, SourceExpr codeTP) {
        return e.call(e.evalBuiltin("codevalue_no_value"), codeTP);
    }

    static SourceExpr okCve(EcoMb e, SourceExpr v) {
        return e.eresultOk(e.mb.typesCve(), v);
    }

    static SourceExpr errCve(EcoMb e, SourceExpr loc, SourceExpr msg) {
        return e.eresultErr(e.mb.typesCve(), loc, msg);
    }

    static SourceExpr errCve(EcoMb e, SourceExpr loc, String msg) {
        return errCve(e, loc, e.s(msg));
    }

    static SourceExpr errsT(EcoMb e) {
        return e.listType(e.mb.builtin("types", "expr_err"));
    }

    static SourceExpr tryEr(EcoMb e, SourceExpr call, SourceExpr resultType) {
        return e.mb.tryCatchFinally(
                e.eresultOk(resultType, call),
                Opt.some(new TestMb.Catch("__errs", errsT(e), e.eresultErrs(resultType, e.v("__errs")))),
                Opt.none());
    }

    static SourceExpr unwrapCveT(EcoMb e, SourceExpr res) {
        return e.matchOf(res, "__r", Opt.none(), e.p("ok", e.v("__r")), e.p("errs", e.mb.throw_(e.v("__r"))));
    }

    static SourceExpr throwErr(EcoMb e, SourceExpr loc, SourceExpr msg) {
        return e.matchOf(
                errCve(e, loc, msg),
                "__x",
                Opt.none(),
                e.p("ok", e.mb.panic(e.s("unreachable"))),
                e.p("errs", e.mb.throw_(e.v("__x"))));
    }

    static SourceExpr throwErr(EcoMb e, SourceExpr loc, String msg) {
        return throwErr(e, loc, e.s(msg));
    }

    static SourceExpr panicErrs(EcoMb e, String target, String what, SourceExpr errs) {
        return e.seq(
                e.let("__m", e.listNew(e.mb.typesString())),
                e.foreach(errs, "__e", e.push(e.v("__m"), e.callM(e.v("__e"), "message", e.tup()))),
                e.mb.panic(e.cat(e.s(target + ": " + what + ": "), e.join(e.v("__m"), "; "))));
    }

    static SourceExpr mustV(EcoMb e, String target, SourceExpr res, String what) {
        return e.matchOf(
                res, "__r", Opt.none(), e.p("ok", e.v("__r")), e.p("errs", panicErrs(e, target, what, e.v("__r"))));
    }

    static SourceExpr xx(EcoMb e) {
        return e.fld(e.v("arg"), "xx");
    }

    static SourceExpr loc(EcoMb e) {
        return e.fld(e.v("arg"), "loc");
    }

    static SourceExpr a(EcoMb e, String... keys) {
        return e.fld(e.v("arg"), keys);
    }

    static SourceExpr in(EcoMb e, String... keys) {
        return e.fld(e.v("inner"), keys);
    }

    static SourceExpr outer(EcoMb e) {
        return e.v("outer");
    }

    static SourceExpr reinterpret(EcoMb e, SourceExpr value, String obj) {
        return e.call(e.mb.mustReinterpret(), e.tup(value, lt(e, obj)));
    }

    static SourceExpr push(EcoMb e, SourceExpr list, SourceExpr element) {
        return e.push(list, element);
    }

    static SourceExpr dfalse(EcoMb e) {
        return e.mb.as(e.b(false), boolT(e));
    }

    static SourceExpr isConst(EcoMb e, SourceExpr xx, SourceExpr typ) {
        return hasB(e, xx, typ, e.mb.evalAspectInt());
    }

    static SourceExpr isEqual(EcoMb e, SourceExpr xx, SourceExpr self, SourceExpr other) {
        return bim(e, e.mb.evalType(), "is_equal", self, e.rec(e.p("xx", xx), e.p("other", other)));
    }

    static SourceExpr typeAspect(EcoMb e, SourceExpr xx, SourceExpr self, SourceExpr key) {
        return bim(e, e.mb.evalType(), "aspect", self, e.rec(e.p("xx", xx), e.p("key", key)));
    }

    static SourceExpr typeCanCastFrom(EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr self, SourceExpr src) {
        return bim(
                e,
                e.mb.evalType(),
                "can_cast_from",
                self,
                e.rec(e.p("xx", xx), e.p("loc", loc), e.p("source_type", src)));
    }

    static SourceExpr toStr(EcoMb e, SourceExpr v) {
        return e.toStr(v);
    }

    static SourceExpr num(EcoMb e, double v) {
        return e.n(v);
    }

    static SourceExpr opId(EcoMb e, String op) {
        return e.evalBuiltin("aspect_operator_" + op + "_id");
    }

    static SourceExpr opIface(EcoMb e, String op) {
        return e.evalBuiltin("operator_" + op);
    }

    static SourceExpr aspId(EcoMb e, String name) {
        return e.evalBuiltin("aspect_" + name + "_id");
    }

    static SourceExpr aspIface(EcoMb e, String name) {
        return e.evalBuiltin("aspect_" + name);
    }

    static void defIface(EcoMb e, TList<SourceExpr> out, String name, EcoMb.P... methods) {
        out.add(e.let(name, e.callM0(e.v("d"), "interface")));
        out.add(e.interfaceImpl(e.v(name), true));
        out.add(e.let(name + "_id", e.callM0(e.v(name), "aspect_id")));
        for (var m : methods) {
            out.add(e.callM(
                    e.v(name),
                    "func",
                    e.rec(e.p("name", e.s(m.key())), e.p("arg", e.tup()), e.p("ret", e.optionTypeSome(m.value())))));
        }
    }

    static void defObj(EcoMb e, TList<SourceExpr> out, String name, String desc, SourceExpr innerType) {
        out.add(e.let(name, e.defObject(e.v("d"), desc)));
        out.add(e.objectImpl(e.v(name), innerType));
    }

    static void method(
            EcoMb e,
            TList<SourceExpr> out,
            String obj,
            String name,
            SourceExpr argType,
            SourceExpr ret,
            SourceExpr body) {
        out.add(e.methodImpl(e.v(obj), name, argType, e.optionTypeSome(ret), e.stage(body)));
    }

    static SourceExpr retM(EcoMb e, SourceExpr value) {
        return e.mb.return_("m", value);
    }

    static SourceExpr body(EcoMb e, SourceExpr... exprs) {
        var seq = e.seq(exprs);
        if (!returnsTo(seq, "m")) {
            return seq;
        }
        return e.mb.labeled("m", seq);
    }

    static boolean returnsTo(SourceExpr expr, String label) {
        return switch (expr.variant()) {
            case SourceExpr.As v -> returnsTo(v.base(), label) || returnsTo(v.target(), label);
            case SourceExpr.Bind v -> returnsTo(v.value(), label);
            case SourceExpr.Call v -> returnsTo(v.func(), label) || returnsTo(v.arg(), label);
            case SourceExpr.Field v -> returnsTo(v.base(), label) || returnsTo(v.key(), label);
            case SourceExpr.Foreach v ->
                returnsTo(v.base(), label) || returnsTo(v.elementKey(), label) || returnsTo(v.body(), label);
            case SourceExpr.Graft v -> returnsTo(v.expr(), label);
            case SourceExpr.If v -> {
                for (var branch : v.conditions()) {
                    if (returnsTo(branch.condition(), label) || returnsTo(branch.body(), label)) {
                        yield true;
                    }
                }
                yield returnsToOpt(v.fallback(), label);
            }
            case SourceExpr.Labeled v -> !v.label().equals(label) && returnsTo(v.inner(), label);
            case SourceExpr.Literal v -> false;
            case SourceExpr.Lower v -> returnsTo(v.expr(), label);
            case SourceExpr.Match v -> {
                if (returnsTo(v.base(), label) || returnsTo(v.elementKey(), label)) {
                    yield true;
                }
                for (var branch : v.cases()) {
                    if (returnsTo(branch.condition(), label) || returnsTo(branch.body(), label)) {
                        yield true;
                    }
                }
                yield returnsToOpt(v.fallback(), label);
            }
            case SourceExpr.OperatorBinary v -> returnsTo(v.base(), label) || returnsTo(v.reference(), label);
            case SourceExpr.OperatorUnary v -> returnsTo(v.base(), label);
            case SourceExpr.Record v -> {
                for (var entry : v.entries()) {
                    if (returnsTo(entry.key(), label) || returnsTo(entry.value(), label)) {
                        yield true;
                    }
                }
                yield false;
            }
            case SourceExpr.Return v ->
                (v.label().optIsSome() && v.label().optUnwrap().equals(label)) || returnsToOpt(v.value(), label);
            case SourceExpr.ScopeUse v -> returnsTo(v.key(), label);
            case SourceExpr.Seq v -> returnsToAny(v.exprs(), label);
            case SourceExpr.Set v -> returnsTo(v.base(), label) || returnsTo(v.value(), label);
            case SourceExpr.Stage v -> returnsTo(v.expr(), label);
            case SourceExpr.Try v -> {
                if (returnsTo(v.body(), label) || returnsToOpt(v.finallyBlock(), label)) {
                    yield true;
                }
                if (v.catchClause().optIsNone()) yield false;
                var c = v.catchClause().optUnwrap();
                yield returnsTo(c.bindingName(), label)
                        || returnsTo(c.exceptionType(), label)
                        || returnsTo(c.body(), label);
            }
            case SourceExpr.Tuple v -> returnsToAny(v.exprs(), label);
        };
    }

    static boolean returnsToOpt(Opt<SourceExpr> expr, String label) {
        return expr.optIsSome() && returnsTo(expr.optUnwrap(), label);
    }

    static boolean returnsToAny(TList<SourceExpr> exprs, String label) {
        for (var expr : exprs) {
            if (returnsTo(expr, label)) {
                return true;
            }
        }
        return false;
    }

    static SourceExpr unwrapM(EcoMb e, SourceExpr res, SourceExpr retInner) {
        return e.matchOf(
                res,
                "__r",
                Opt.none(),
                e.p("ok", e.v("__r")),
                e.p("errs", retM(e, e.eresultErrs(retInner, e.v("__r")))));
    }

    static SourceExpr unwrapCveM(EcoMb e, SourceExpr res) {
        return unwrapM(e, res, e.mb.typesCve());
    }

    static SourceExpr andB(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.ifElse(a, b, e.b(false));
    }

    static SourceExpr orB(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.ifElse(e.not(a), b, e.b(true));
    }

    static SourceExpr isTargetCode(EcoMb e, SourceExpr codeTP) {
        return e.eq(
                e.matchOf(codeTP, "__c", Opt.none(), e.p("none", e.intOf(0)), e.p("target", e.intOf(1))), e.intOf(1));
    }

    static SourceExpr fnDef(
            EcoMb e, TList<SourceExpr> out, String target, String name, SourceExpr argType, SourceExpr ret) {
        out.add(e.let(name + "_b", e.defFunc(e.v("d"), target + "." + name)));
        out.add(e.let(name, e.fld(e.v(name + "_b"), "func")));
        e.fnSigs.put(name, new SourceExpr[] {argType, e.optionTypeSome(ret), e.optionTypeNone()});
        return e.v(name);
    }

    static void fnImpl(EcoMb e, TList<SourceExpr> out, String name, SourceExpr bodyExpr) {
        var sig = e.fnSigs.getOpt(name).optUnwrap();
        e.fnSigs.remove(name);
        out.add(e.funcImpl(e.v(name + "_b"), sig[0], sig[1], sig[2], e.stage(bodyExpr)));
    }

    static void fnThrows(EcoMb e, String name, SourceExpr exceptionType) {
        e.fnSigs.getOpt(name).optUnwrap()[2] = e.optionTypeSome(exceptionType);
    }

    static void accessor(EcoMb e, TList<SourceExpr> x, String obj, String field, SourceExpr typ) {
        method(e, x, obj, field, e.tup(), typ, in(e, field));
    }

    static SourceExpr p(EcoMb e, String key) {
        return e.fld(e.v("p"), key);
    }

    static void selfType(
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
                aspIface(e, "artifact"),
                e.p("exporter", e.call(e.evalBuiltin("exporter_source_object_type_instance"), e.tup()))));
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalType(),
                e.p("desc", desc),
                e.p(
                        "aspect",
                        e.ifElse(
                                e.mb.operatorEq(e.fld(e.v("arg"), "key"), aspId(e, "type")),
                                e.optSome(e.mb.typesAspect(), e.v("outer")),
                                e.ifElse(
                                        e.mb.operatorEq(e.fld(e.v("arg"), "key"), aspId(e, "artifact")),
                                        e.optSome(e.mb.typesAspect(), e.v("outer")),
                                        dispatch))),
                e.p("can_cast_from", canCastFrom),
                e.p("cast_from", castFrom),
                e.p("can_cast_to", canCastTo),
                e.p("cast_to", castTo),
                e.p("is_equal", isEqual)));
    }

    static SourceExpr noCastTo(EcoMb e) {
        return e.mb.as(e.b(false), boolT(e));
    }

    static SourceExpr castToPanic(EcoMb e) {
        return e.mb.panic(e.s("cast_to unreachable"));
    }

    static SourceExpr srcType(EcoMb e) {
        return a(e, "source_type");
    }

    static SourceExpr srcBits(EcoMb e, String marker) {
        return acc(e, xx(e), srcType(e), marker, "bits");
    }

    static SourceExpr cvBits(EcoMb e, SourceExpr st, String marker) {
        return acc(e, xx(e), st, marker, "bits");
    }

    static void op(EcoMb e, TList<SourceExpr> x, String obj, String op, SourceExpr bodyExpr) {
        x.add(e.implInterface(e.v(obj), opIface(e, op), e.p(op, body(e, e.ret(bodyExpr)))));
    }

    static void markerType(EcoMb e, TList<SourceExpr> x, String obj, String desc, String marker, SourceExpr dispatch) {
        selfType(
                e,
                x,
                obj,
                e.s(desc),
                dispatch,
                canCastFromSelf(e),
                castFromSelf(e),
                noCastTo(e),
                castToPanic(e),
                body(e, e.ret(has(e, xx(e), a(e, "other"), marker))));
    }

    static SourceExpr vocT(EcoMb e) {
        return e.mb.builtin("types", "value_object_const");
    }

    static SourceExpr lookupV(EcoMb e, SourceExpr xx, SourceExpr typ, SourceExpr ifaceType) {
        return e.fld(lookup(e, xx, typ, ifaceType), "value");
    }

    static TestMb.Branch rtBranch(EcoMb e, SourceExpr id, SourceExpr value) {
        return new TestMb.Branch(e.mb.operatorEq(e.fld(e.v("arg"), "key"), id), value);
    }

    static SourceExpr rtDispatch(EcoMb e, SourceExpr underType, SourceExpr inner) {
        var delegated = new SourceExpr[] {
            e.mb.evalAspectBindDynamicId(), e.mb.evalAspectConsumeDynamicId(), e.mb.evalAspectDropDynamicId()
        };
        var branches = new TList<TestMb.Branch>();
        for (var own : new String[] {"lift", "bind", "initial_bind_dynamic", "field_use_dynamic"}) {
            branches.add(rtBranch(e, aspId(e, own), e.optSome(e.mb.typesAspect(), outer(e))));
        }
        for (var id : delegated) {
            branches.add(rtBranch(e, id, typeAspect(e, xx(e), underType, a(e, "key"))));
        }
        return e.mb.ifOf(branches, Opt.some(inner));
    }

    static SourceExpr canCastFromSelf(EcoMb e) {
        return body(e, e.ret(isEqual(e, xx(e), outer(e), srcType(e))));
    }

    static SourceExpr castFromSelf(EcoMb e) {
        return body(e, e.ret(okCve(e, a(e, "cv"))));
    }

    static SourceExpr isRuntime(EcoMb e, SourceExpr cv) {
        return bim(
                e,
                e.mb.evalTarget(),
                "value_is_runtime",
                e.fld(xx(e), "target"),
                e.rec(e.p("xx", xx(e)), e.p("value", cveValue(e, cv))));
    }

    static SourceExpr retagRuntime(EcoMb e, SourceExpr cv, SourceExpr type) {
        return cvOf(
                e,
                cveCode(e, cv),
                e.call(
                        e.evalBuiltin("value_dynamic"),
                        e.rec(
                                e.p(
                                        "source",
                                        e.fld(
                                                e.call(
                                                        e.mb.mustReinterpret(),
                                                        e.tup(cveValue(e, cv), e.mb.builtin("types", "value_dynamic"))),
                                                "source")),
                                e.p("type", type))));
    }

    static SourceExpr tagValue(EcoMb e, SourceExpr cv, SourceExpr type) {
        return e.ifElse(
                isRuntime(e, cv),
                okCve(e, retagRuntime(e, cv, type)),
                okCve(
                        e,
                        cvOf(
                                e,
                                codeNone(e, loc(e)),
                                e.call(
                                        e.evalBuiltin("value_object_const"),
                                        e.rec(
                                                e.p("type", type),
                                                e.p(
                                                        "data",
                                                        e.fld(
                                                                e.call(
                                                                        e.mb.mustReinterpret(),
                                                                        e.tup(cveValue(e, cv), vocT(e))),
                                                                "data")))))));
    }

    static void rtLiftImpl(EcoMb e, TList<SourceExpr> x, String obj) {
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

    static void rtBindImpl(EcoMb e, TList<SourceExpr> x, String obj, SourceExpr underType) {
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectBind(),
                e.p(
                        "bind",
                        bim(
                                e,
                                e.mb.evalAspectBind(),
                                "bind",
                                lookupV(e, xx(e), underType, e.mb.evalAspectBind()),
                                e.rec(e.p("xx", xx(e)), e.p("loc", loc(e)), e.p("cv", a(e, "cv")))))));
    }

    static void rtUseImpl(EcoMb e, TList<SourceExpr> x, String obj) {
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectFieldUseDynamic(),
                e.p(
                        "field_use_dynamic",
                        okCve(
                                e,
                                cvOf(
                                        e,
                                        codeNone(e, loc(e)),
                                        e.call(
                                                e.evalBuiltin("value_dynamic"),
                                                e.rec(e.p("source", a(e, "source")), e.p("type", outer(e)))))))));
        x.add(e.implInterface(
                e.v(obj),
                e.mb.evalAspectInitialBindDynamic(),
                e.p(
                        "initial_bind_dynamic",
                        e.call(
                                e.evalBuiltin("binding_dynamic"),
                                e.rec(e.p("dynamic_key", e.v("arg")), e.p("type", outer(e)))))));
    }
}
