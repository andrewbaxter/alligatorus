package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public final class EcoMb {
    public final TMap<String, SourceExpr[]> fnSigs = new TMap<>();

    public final TestMb mb = new TestMb();

    public SourceExpr v(String name) {
        return mb.scopeUse(name);
    }

    public SourceExpr s(String value) {
        return mb.str(value);
    }

    public SourceExpr n(double value) {
        return mb.num(value);
    }

    public SourceExpr b(boolean value) {
        return mb.bool(value);
    }

    public SourceExpr intOf(double value) {
        return mb.as(mb.num(value), mb.typesInt());
    }

    public SourceExpr let(String name, SourceExpr value) {
        return mb.bind(name, value);
    }

    public SourceExpr fld(SourceExpr base, String... keys) {
        var expr = base;
        for (var key : keys) {
            expr = mb.field(expr, mb.str(key));
        }
        return expr;
    }

    public SourceExpr call(SourceExpr func, SourceExpr arg) {
        return mb.call(func, arg);
    }

    public SourceExpr callM(SourceExpr receiver, String method, SourceExpr arg) {
        return mb.call(fld(receiver, method), arg);
    }

    public SourceExpr callM0(SourceExpr receiver, String method) {
        return callM(receiver, method, mb.tupleOf());
    }

    public record P(String key, SourceExpr value) {}

    public P p(String key, SourceExpr value) {
        return new P(key, value);
    }

    public SourceExpr rec(P... pairs) {
        var entries = new TList<TestMb.RecordPair>(pairs.length);
        for (var pair : pairs) {
            entries.add(new TestMb.RecordPair(pair.key(), pair.value()));
        }
        return mb.recordOf(entries);
    }

    public SourceExpr seq(SourceExpr... exprs) {
        return mb.seqOf(exprs);
    }

    public SourceExpr tup(SourceExpr... exprs) {
        return mb.tupleOf(exprs);
    }

    public SourceExpr ret(SourceExpr value) {
        return mb.return_(value);
    }

    public SourceExpr listType(SourceExpr elementType) {
        return mb.call(mb.typesList(), elementType);
    }

    public SourceExpr strListType() {
        return listType(mb.typesString());
    }

    public SourceExpr listNew(SourceExpr elementType, SourceExpr... elements) {
        return callM(listType(elementType), "new", mb.tupleOf(elements));
    }

    public SourceExpr strList(SourceExpr... elements) {
        return listNew(mb.typesString(), elements);
    }

    public SourceExpr push(SourceExpr list, SourceExpr element) {
        return callM(list, "push", element);
    }

    public SourceExpr join(SourceExpr list, String separator) {
        return callM(list, "join", mb.str(separator));
    }

    public SourceExpr cat(SourceExpr... parts) {
        return join(strList(parts), "");
    }

    public SourceExpr toStr(SourceExpr value) {
        return callM0(value, "to_string");
    }

    public SourceExpr foreach(SourceExpr base, String elementName, SourceExpr body) {
        return mb.foreach(base, mb.str(elementName), body);
    }

    public SourceExpr ifElse(SourceExpr condition, SourceExpr then, SourceExpr else_) {
        return mb.ifOf(TList.of(new TestMb.Branch(condition, then)), Opt.some(else_));
    }

    public SourceExpr ifOnly(SourceExpr condition, SourceExpr then) {
        return mb.ifOf(TList.of(new TestMb.Branch(condition, then)), Opt.none());
    }

    public SourceExpr incr(SourceExpr base) {
        return mb.set(base, mb.operatorAdd(base, mb.num(1.)));
    }

    public SourceExpr optSome(SourceExpr elementType, SourceExpr value) {
        return callM(mb.call(mb.typesOption(), elementType), "some", value);
    }

    public SourceExpr optNone(SourceExpr elementType) {
        return callM(mb.call(mb.typesOption(), elementType), "none", mb.tupleOf());
    }

    public SourceExpr retSome(SourceExpr typeExpr) {
        return mb.optSomeType(typeExpr);
    }

    public SourceExpr stage(SourceExpr expr) {
        return mb.stage(expr);
    }

    public SourceExpr lower(SourceExpr expr) {
        return mb.lower(expr);
    }

    public SourceExpr lowerV(String name) {
        return mb.lower(mb.scopeUse(name));
    }

    public SourceExpr define() {
        return mb.call(mb.define(), mb.tupleOf());
    }

    public SourceExpr defFunc(SourceExpr definer, String name) {
        return callM(definer, "func", mb.str(name));
    }

    public SourceExpr defObject(SourceExpr definer, String name) {
        return callM(definer, "object", mb.str(name));
    }

    public SourceExpr finish(SourceExpr definer) {
        return callM0(definer, "finish");
    }

    public SourceExpr funcImpl(SourceExpr funcBuilder, SourceExpr arg, SourceExpr ret, SourceExpr stagedBody) {
        return callM(funcBuilder, "impl", rec(p("arg", arg), p("ret", ret), p("impl_body", stagedBody)));
    }

    public SourceExpr methodImpl(
            SourceExpr objectBuilder, String name, SourceExpr arg, SourceExpr ret, SourceExpr stagedBody) {
        return funcImpl(callM(objectBuilder, "func", mb.str(name)), arg, ret, stagedBody);
    }

    public SourceExpr importLocal(String key, String path) {
        var entry = new SourceExpr.RecordEntry(mb.str(key), mb.sourceLocal(mb.str(path)));
        return mb.field(mb.importRecord(TList.of(entry)), mb.str(key));
    }

    public SourceExpr implInterface(SourceExpr objectBuilder, SourceExpr ifaceRef, P... methods) {
        var staged = new TList<TestMb.RecordPair>(methods.length);
        for (var m : methods) {
            staged.add(new TestMb.RecordPair(m.key(), mb.stage(m.value())));
        }
        var record = mb.call(
                mb.field(mb.call(mb.typesMap(), mb.tupleOf(mb.typesConstkey(), mb.typesExpr())), mb.str("new")),
                mb.recordOf(staged));
        return callM(objectBuilder, "impl_interface", rec(p("iface_ref", ifaceRef), p("record", record)));
    }

    public SourceExpr eresultOk(SourceExpr resultType, SourceExpr value) {
        return callM(mb.call(mb.typesEresult(), resultType), "ok", value);
    }

    public SourceExpr eresultErr(SourceExpr resultType, SourceExpr loc, SourceExpr message) {
        return callM(mb.call(mb.typesEresult(), resultType), "err", mb.tupleOf(loc, message));
    }

    public SourceExpr eresultErrs(SourceExpr resultType, SourceExpr errors) {
        return callM(mb.call(mb.typesEresult(), resultType), "errs", errors);
    }

    public SourceExpr typesDouble() {
        return mb.builtin("types", "double");
    }

    public SourceExpr typesFloat() {
        return mb.builtin("types", "float");
    }

    public SourceExpr typesExprContext() {
        return mb.builtin("types", "expr_context");
    }

    public SourceExpr typesLoc() {
        return mb.builtin("types", "qexpr_id");
    }

    public SourceExpr typesJumpKey() {
        return mb.builtin("types", "jump_key");
    }

    public SourceExpr evalBuiltin(String name) {
        return mb.builtin("eval", name);
    }

    public SourceExpr dstr(String value) {
        return mb.as(mb.str(value), mb.typesString());
    }

    public SourceExpr matchOf(SourceExpr base, String key, Opt<SourceExpr> fallback, P... branches) {
        var out = new TList<TestMb.Branch>(branches.length);
        for (var b : branches) {
            out.add(new TestMb.Branch(mb.str(b.key()), b.value()));
        }
        return mb.matchOf(base, mb.str(key), out, fallback);
    }

    public SourceExpr mapType(SourceExpr keyType, SourceExpr valueType) {
        return mb.call(mb.typesMap(), mb.tupleOf(keyType, valueType));
    }

    public SourceExpr mapNew(SourceExpr keyType, SourceExpr valueType, P... entries) {
        return callM(mapType(keyType, valueType), "new", rec(entries));
    }

    public SourceExpr not(SourceExpr a) {
        return ifElse(a, mb.operatorLt(intOf(1), intOf(0)), b(true));
    }

    public SourceExpr eq(SourceExpr a, SourceExpr b) {
        return mb.operatorEq(a, b);
    }

    public SourceExpr aspectDispatch(SourceExpr... aspectIds) {
        var branches = new TList<TestMb.Branch>(aspectIds.length);
        for (var id : aspectIds) {
            branches.add(
                    new TestMb.Branch(mb.operatorEq(fld(v("arg"), "key"), id), optSome(mb.typesAspect(), v("outer"))));
        }
        return mb.ifOf(branches, Opt.some(optNone(mb.typesAspect())));
    }
}
