package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoWebidlWasm {
    private EcoWebidlWasm() {}

    public static final String MODULE = "webidl";

    public static final IList<String> KINDS = IList.of("op", "get", "set", "ctor");

    private static int kindOf(String kind) {
        for (int i = 0; i < KINDS.size(); i++) {
            if (KINDS.get(i).equals(kind)) return i;
        }
        throw new IllegalArgumentException("no webidl hook kind " + kind);
    }

    private static SourceExpr kindIs(EcoMb e, String kind) {
        return e.eq(in(e, "kind"), num(e, kindOf(kind)));
    }

    private static SourceExpr kindStr(EcoMb e) {
        var expr = e.dstr("ctor");
        for (int i = KINDS.size() - 2; i >= 0; i--) {
            expr = e.ifElse(kindIs(e, KINDS.get(i)), e.dstr(KINDS.get(i)), expr);
        }
        return expr;
    }

    private static SourceExpr webidl(EcoMb e, String fn) {
        return e.lower(e.fld(e.v("webidl"), fn));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var x = new TList<SourceExpr>();
        x.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        x.add(e.let("webidl", e.importLocal("webidl", "webidl.atz")));
        EcoWat.bindTypes(e, x);
        for (var t : new String[] {"i16", "u16"}) x.add(e.let("t_" + t, e.fld(e.v("wasm"), t)));
        x.add(e.let("t_pi32", e.call(e.fld(e.v("wasm"), "ptr"), e.v("t_i32"))));
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        defIface(e, x, "wop_m", e.p("kind", intT(e)));
        defObj(e, x, "wop_t", "webidl binding hook", e.rec(e.p("kind", intT(e))));
        selfType(
                e,
                x,
                "wop_t",
                e.cat(e.s("webidl "), kindStr(e)),
                rtDispatch(e, str(e), e.aspectDispatch(lv(e, "wop_m_id"), aspId(e, "function"))),
                canCastFromSelf(e),
                castFromSelf(e),
                noCastTo(e),
                castToPanic(e),
                body(
                        e,
                        e.ifOnly(e.not(has(e, xx(e), a(e, "other"), "wop_m")), retM(e, dfalse(e))),
                        e.ret(e.eq(in(e, "kind"), acc(e, xx(e), a(e, "other"), "wop_m", "kind")))));
        x.add(e.implInterface(e.v("wop_t"), t(e, "wop_m"), e.p("kind", in(e, "kind"))));
        rtLiftImpl(e, x, "wop_t");
        rtBindImpl(e, x, "wop_t", str(e));
        rtUseImpl(e, x, "wop_t");

        EcoTargetMb.fnDef(
                e, x, "webidl", "h_str", e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("cv", cveT(e))), str(e));
        fnThrows(e, "h_str", errsT(e));
        fnImpl(
                e,
                x,
                "h_str",
                body(
                        e,
                        e.let("c", unwrapCveT(e, constEvalIn(e, xx(e), e.stage(graft(e, a(e, "cv")))))),
                        e.ifOnly(
                                isRuntime(e, e.v("c")),
                                throwErr(e, loc(e), "webidl: iface and member must be constant strings")),
                        e.let("sk", lookup(e, xx(e), cveType(e, e.v("c")), e.mb.evalAspectStringConst())),
                        e.ifOnly(
                                e.not(e.fld(e.v("sk"), "is_some")),
                                throwErr(e, loc(e), "webidl: iface and member must be strings")),
                        e.ret(bim(
                                e,
                                e.mb.evalAspectStringConst(),
                                "get",
                                e.fld(e.v("sk"), "value"),
                                cveValue(e, e.v("c"))))));

        var wkindsPairs = new String[][] {
            {"flag", "t_i32"},
            {"handle", "t_i32"},
            {"ptr", "t_pu8"},
            {"len", "t_u32"},
            {"out", "t_pi32"},
            {"bool", "t_i32"},
            {"i8", "t_i8"},
            {"u8", "t_u8"},
            {"i16", "t_i16"},
            {"u16", "t_u16"},
            {"i32", "t_i32"},
            {"u32", "t_u32"},
            {"i64", "t_i64"},
            {"u64", "t_u64"},
            {"f32", "t_f32"},
            {"f64", "t_f64"}
        };
        var wkindsEntries = new TList<EcoMb.P>();
        for (var pair : wkindsPairs) wkindsEntries.add(e.p(pair[0], e.lowerV(pair[1])));
        var wkinds = e.mapNew(str(e), ty(e), wkindsEntries.toArray(EcoMb.P[]::new));
        var wretsPairs = new String[][] {
            {"void", "t_void"},
            {"str", "t_i32"},
            {"handle", "t_i32"},
            {"bool", "t_i32"},
            {"i8", "t_i8"},
            {"u8", "t_u8"},
            {"i16", "t_i16"},
            {"u16", "t_u16"},
            {"i32", "t_i32"},
            {"u32", "t_u32"},
            {"i64", "t_i64"},
            {"u64", "t_u64"},
            {"f32", "t_f32"},
            {"f64", "t_f64"}
        };
        var wretsEntries = new TList<EcoMb.P>();
        for (var pair : wretsPairs) wretsEntries.add(e.p(pair[0], e.lowerV(pair[1])));
        var wrets = e.mapNew(str(e), ty(e), wretsEntries.toArray(EcoMb.P[]::new));
        x.add(e.implInterface(
                e.v("wop_t"),
                aspIface(e, "function"),
                e.p(
                        "call",
                        body(
                                e,
                                e.let("kind", kindStr(e)),
                                e.let("argv", a(e, "argument")),
                                e.ifOnly(
                                        e.not(cveHas(e, e.v("argv"))),
                                        retM(e, errCve(e, loc(e), "webidl: hook argument has no value"))),
                                e.let("rk", lookup(e, xx(e), cveType(e, e.v("argv")), aspIface(e, "record"))),
                                e.ifOnly(
                                        e.not(e.fld(e.v("rk"), "is_some")),
                                        retM(e, errCve(e, loc(e), "webidl: hook argument must be {iface, member}"))),
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
                                e.let(
                                        "iface",
                                        unwrapM(
                                                e,
                                                tryEr(
                                                        e,
                                                        hf(
                                                                e,
                                                                "h_str",
                                                                e.p("xx", xx(e)),
                                                                e.p("loc", loc(e)),
                                                                e.p(
                                                                        "cv",
                                                                        unwrapCveM(
                                                                                e,
                                                                                e.callM(
                                                                                        e.v("fields"),
                                                                                        "get",
                                                                                        e.s("iface"))))),
                                                        str(e)),
                                                cveT(e))),
                                e.let("mcell", e.rec(e.p("v", e.dstr("constructor")))),
                                e.ifOnly(
                                        e.not(kindIs(e, "ctor")),
                                        e.seq(
                                                e.let(
                                                        "mname",
                                                        unwrapM(
                                                                e,
                                                                tryEr(
                                                                        e,
                                                                        hf(
                                                                                e,
                                                                                "h_str",
                                                                                e.p("xx", xx(e)),
                                                                                e.p("loc", loc(e)),
                                                                                e.p(
                                                                                        "cv",
                                                                                        unwrapCveM(
                                                                                                e,
                                                                                                e.callM(
                                                                                                        e.v("fields"),
                                                                                                        "get",
                                                                                                        e.s(
                                                                                                                "member"))))),
                                                                        str(e)),
                                                                cveT(e))),
                                                e.mb.set(e.fld(e.v("mcell"), "v"), e.v("mname")))),
                                e.let("member", e.fld(e.v("mcell"), "v")),
                                e.let(
                                        "sig",
                                        e.call(
                                                webidl(e, "sig"),
                                                e.rec(
                                                        e.p("table", e.lower(e.fld(e.v("webidl"), "spec"))),
                                                        e.p("iface", e.v("iface")),
                                                        e.p("member", e.v("member")),
                                                        e.p("kind", e.v("kind"))))),
                                e.ifOnly(
                                        e.not(e.callM(e.v("sig"), "ok", e.tup())),
                                        retM(
                                                e,
                                                errCve(
                                                        e,
                                                        loc(e),
                                                        e.cat(e.s("webidl: "), e.callM(e.v("sig"), "err", e.tup()))))),
                                e.let("kinds", e.listNew(str(e))),
                                e.ifOnly(e.callM(e.v("sig"), "self", e.tup()), push(e, e.v("kinds"), e.dstr("handle"))),
                                e.foreach(
                                        e.callM(e.v("sig"), "args", e.tup()),
                                        "ap",
                                        e.seq(
                                                e.ifOnly(
                                                        e.callM(e.v("ap"), "optional", e.tup()),
                                                        push(e, e.v("kinds"), e.dstr("flag"))),
                                                e.ifOnly(
                                                        e.callM(e.v("ap"), "nullable", e.tup()),
                                                        push(e, e.v("kinds"), e.dstr("flag"))),
                                                e.ifElse(
                                                        e.eq(e.callM(e.v("ap"), "kind", e.tup()), e.s("str")),
                                                        e.seq(
                                                                push(e, e.v("kinds"), e.dstr("ptr")),
                                                                push(e, e.v("kinds"), e.dstr("len"))),
                                                        push(e, e.v("kinds"), e.callM(e.v("ap"), "kind", e.tup()))))),
                                e.let("ret_out", e.callM(e.v("sig"), "ret_out", e.tup())),
                                e.ifOnly(e.v("ret_out"), push(e, e.v("kinds"), e.dstr("out"))),
                                e.let("wkinds", wkinds),
                                e.let("wrets", wrets),
                                e.let("params", e.listNew(ty(e))),
                                e.foreach(
                                        e.v("kinds"),
                                        "k",
                                        push(e, e.v("params"), e.callM(e.v("wkinds"), "get", e.v("k")))),
                                e.let(
                                        "rkind",
                                        e.ifElse(
                                                e.v("ret_out"),
                                                e.dstr("void"),
                                                e.callM(e.v("sig"), "ret_kind", e.tup()))),
                                e.let("rty", e.callM(e.v("wrets"), "get", e.v("rkind"))),
                                e.let(
                                        "fnty",
                                        e.call(
                                                e.lower(e.fld(e.v("wasm"), "fn")),
                                                e.rec(e.p("params", e.v("params")), e.p("ret", e.v("rty"))))),
                                e.let(
                                        "name",
                                        e.call(
                                                webidl(e, "mangle"),
                                                e.rec(
                                                        e.p("iface", e.callM(e.v("sig"), "iface", e.tup())),
                                                        e.p("member", e.callM(e.v("sig"), "member", e.tup())),
                                                        e.p("kind", e.v("kind"))))),
                                e.let(
                                        "ity",
                                        e.call(
                                                e.lower(e.fld(e.v("wasm"), "import_t")),
                                                e.rec(
                                                        e.p("module", e.s(MODULE)),
                                                        e.p("name", e.v("name")),
                                                        e.p("ty", e.v("fnty"))))),
                                e.let(
                                        "refcv",
                                        unwrapCveM(
                                                e,
                                                constEvalIn(e, xx(e), e.stage(e.cat(e.s("$"), e.lower(e.v("name"))))))),
                                e.ret(tagValue(e, e.v("refcv"), e.v("ity"))))),
                e.p("exception_type", e.optionTypeLoweredNone())));
        x.add(e.finish(e.v("d")));
        var defs = new TList<EcoMb.P>();
        defs.add(e.p("wop_t_new", e.fld(e.v("wop_t"), "new")));
        defs.add(e.p(
                "global_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType())), e.p("ret", e.v("t_i32"))))));
        x.add(e.ret(e.rec(defs.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(x))));
        var exports = new TList<EcoMb.P>();
        exports.add(e.p("module", e.s(MODULE)));
        for (var kind : KINDS) {
            exports.add(e.p(
                    kind,
                    e.mb.as(e.call(e.fld(e.v("k"), "wop_t_new"), e.rec(e.p("kind", e.intOf(kindOf(kind))))), ty(e))));
        }
        exports.add(e.p(
                "global",
                e.call(
                        e.fld(e.v("wasm"), "import_fn"),
                        e.rec(
                                e.p("module", e.s(MODULE)),
                                e.p("name", e.s("global")),
                                e.p("ty", e.fld(e.v("k"), "global_t"))))));
        top.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return top;
    }
}
