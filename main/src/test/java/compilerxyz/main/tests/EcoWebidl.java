package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoWebidl {
    private EcoWebidl() {}

    public static final String SPEC_REPO = "https://github.com/w3c/webref";

    public static final String SPEC_COMMIT = "8a9e087aee12097e86b036b8560b6b0217f784eb";

    public static final IList<String> SPEC_FILES =
            IList.of("dom.idl", "html.idl", "cssom-view.idl", "webgl1.idl", "webgl2.idl");

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        return EcoTargetMb.fnDef(e, out, "webidl", name, argType, ret);
    }

    private static void fnImplVoid(EcoMb e, TList<SourceExpr> out, String name, SourceExpr bodyExpr) {
        var sig = e.fnSigs.getOpt(name).optUnwrap();
        e.fnSigs.remove(name);
        out.add(e.funcImpl(e.v(name + "_b"), sig[0], e.optionTypeNone(), sig[2], e.stage(bodyExpr)));
    }

    private static SourceExpr lexT(EcoMb e) {
        return t(e, "lex");
    }

    private static SourceExpr curT(EcoMb e) {
        return t(e, "cur");
    }

    private static SourceExpr lc(EcoMb e) {
        return e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)));
    }

    private static SourceExpr lcArgs(EcoMb e) {
        return e.rec(e.p("lex", a(e, "lex")), e.p("cur", a(e, "cur")));
    }

    private static SourceExpr lcArgs(EcoMb e, EcoMb.P extra) {
        return e.rec(e.p("lex", a(e, "lex")), e.p("cur", a(e, "cur")), extra);
    }

    private static SourceExpr peek(EcoMb e) {
        return hf(e, "h_peek", lcArgs(e));
    }

    private static SourceExpr next(EcoMb e) {
        return hf(e, "h_next", lcArgs(e));
    }

    private static SourceExpr nextText(EcoMb e) {
        return e.callM(next(e), "text", e.tup());
    }

    private static SourceExpr accept(EcoMb e, String text) {
        return hf(e, "h_accept", lcArgs(e, e.p("text", e.s(text))));
    }

    private static SourceExpr expect(EcoMb e, String text) {
        return hf(e, "h_expect", lcArgs(e, e.p("text", e.s(text))));
    }

    private static SourceExpr skipBalanced(EcoMb e, String close) {
        return hf(e, "h_skip_balanced", lcArgs(e, e.p("close", e.s(close))));
    }

    private static SourceExpr parseType(EcoMb e) {
        return hf(e, "h_type", lcArgs(e));
    }

    private static SourceExpr byteIs(EcoMb e, SourceExpr n, int c) {
        return e.eq(n, num(e, c));
    }

    private static SourceExpr sigOf(EcoMb e) {
        return mk(
                e,
                "isig",
                e.p("ok", e.eq(e.fld(e.v("st"), "ok"), num(e, 1))),
                e.p("err", e.fld(e.v("st"), "err")),
                e.p("iface", e.fld(e.v("st"), "iface")),
                e.p("member", e.fld(e.v("st"), "member")),
                e.p("base", e.fld(e.v("st"), "base")),
                e.p("kind", a(e, "kind")),
                e.p("self", e.eq(e.fld(e.v("st"), "self"), num(e, 1))),
                e.p("args", e.v("args")),
                e.p("ret_out", e.eq(e.fld(e.v("st"), "ret_out"), num(e, 1))),
                e.p("ret_kind", e.fld(e.v("st"), "ret_kind")));
    }

    private static SourceExpr sigErr(EcoMb e, SourceExpr message) {
        return e.seq(
                e.mb.set(e.fld(e.v("st"), "ok"), e.intOf(0)),
                e.mb.set(e.fld(e.v("st"), "err"), message),
                retM(e, sigOf(e)));
    }

    private static SourceExpr sigCall(EcoMb e, SourceExpr iface, SourceExpr member, String kind) {
        return hf(
                e,
                "h_sig",
                e.rec(e.p("table", a(e, "table")), e.p("iface", iface), e.p("member", member), e.p("kind", e.s(kind))));
    }

    private static SourceExpr inRange(EcoMb e, SourceExpr n, int lo, int hi) {
        return andB(e, e.mb.operatorGe(n, num(e, lo)), e.mb.operatorLe(n, num(e, hi)));
    }

    private static SourceExpr utf8(EcoMb e, SourceExpr bytes) {
        return e.matchOf(
                e.call(e.evalBuiltin("str_from_utf8"), bytes),
                "__s",
                Opt.none(),
                e.p("some", e.v("__s")),
                e.p("none", e.mb.panic(e.s("webidl: spec text is not valid UTF-8"))));
    }

    private static SourceExpr itype(EcoMb e, SourceExpr name, SourceExpr nullable, SourceExpr args) {
        return mk(e, "itype", e.p("name", name), e.p("nullable", nullable), e.p("args", args));
    }

    private static SourceExpr imember(
            EcoMb e,
            SourceExpr kind,
            SourceExpr name,
            SourceExpr ty,
            SourceExpr args,
            SourceExpr readonly,
            SourceExpr static_,
            SourceExpr special) {
        return imember(e, kind, name, ty, args, readonly, static_, special, e.dstr(""));
    }

    private static SourceExpr imember(
            EcoMb e,
            SourceExpr kind,
            SourceExpr name,
            SourceExpr ty,
            SourceExpr args,
            SourceExpr readonly,
            SourceExpr static_,
            SourceExpr special,
            SourceExpr value) {
        return mk(
                e,
                "imember",
                e.p("kind", kind),
                e.p("name", name),
                e.p("ty", ty),
                e.p("args", args),
                e.p("readonly", readonly),
                e.p("static_", static_),
                e.p("special", special),
                e.p("value", value));
    }

    private static SourceExpr ifound(EcoMb e, SourceExpr found, SourceExpr iface, SourceExpr member, SourceExpr count) {
        return mk(e, "ifound", e.p("found", found), e.p("iface", iface), e.p("member", member), e.p("count", count));
    }

    private static SourceExpr noMember(EcoMb e) {
        return imember(
                e, e.dstr(""), e.dstr(""), noType(e), e.listNew(lt(e, "iarg")), e.b(false), e.b(false), e.dstr(""));
    }

    static final IList<String> BUF_NAMES = IList.of(
            "BufferSource",
            "AllowSharedBufferSource",
            "ArrayBuffer",
            "ArrayBufferView",
            "Int8Array",
            "Uint8Array",
            "Uint8ClampedArray",
            "Int16Array",
            "Uint16Array",
            "Int32Array",
            "Uint32Array",
            "Float32Array",
            "Float64Array",
            "DataView");

    private static SourceExpr noType(EcoMb e) {
        return itype(e, e.dstr("undefined"), e.b(false), e.listNew(lt(e, "itype")));
    }

    private static SourceExpr idef(
            EcoMb e,
            SourceExpr kind,
            SourceExpr name,
            SourceExpr parent,
            SourceExpr partial,
            SourceExpr members,
            SourceExpr ty) {
        return mk(
                e,
                "idef",
                e.p("kind", kind),
                e.p("name", name),
                e.p("parent", parent),
                e.p("partial", partial),
                e.p("members", members),
                e.p("strings", e.listNew(str(e))),
                e.p("ty", ty));
    }

    private static SourceExpr pushDef(EcoMb e, SourceExpr def) {
        return push(e, a(e, "out"), def);
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var x = new TList<SourceExpr>();
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        defObj(e, x, "tok", "webidl token", e.rec(e.p("kind", str(e)), e.p("text", str(e))));
        accessor(e, x, "tok", "kind", str(e));
        accessor(e, x, "tok", "text", str(e));
        defObj(
                e,
                x,
                "lex",
                "webidl token stream",
                e.rec(e.p("toks", e.mapType(str(e), t(e, "tok"))), e.p("n", intT(e))));
        accessor(e, x, "lex", "toks", e.mapType(str(e), t(e, "tok")));
        accessor(e, x, "lex", "n", intT(e));
        defObj(e, x, "cur", "webidl cursor", e.rec(e.p("i", intT(e))));
        accessor(e, x, "cur", "i", intT(e));
        x.add(e.methodImpl(e.v("cur"), "adv", e.tup(), e.optionTypeNone(), e.stage(e.incr(in(e, "i")))));
        defObj(
                e,
                x,
                "itype",
                "webidl type",
                e.rec(e.p("name", str(e)), e.p("nullable", boolT(e)), e.p("args", e.listType(t(e, "itype")))));
        accessor(e, x, "itype", "name", str(e));
        accessor(e, x, "itype", "nullable", boolT(e));
        accessor(e, x, "itype", "args", e.listType(t(e, "itype")));
        defObj(
                e,
                x,
                "iarg",
                "webidl argument",
                e.rec(
                        e.p("name", str(e)),
                        e.p("ty", t(e, "itype")),
                        e.p("optional", boolT(e)),
                        e.p("variadic", boolT(e))));
        accessor(e, x, "iarg", "name", str(e));
        accessor(e, x, "iarg", "ty", t(e, "itype"));
        accessor(e, x, "iarg", "optional", boolT(e));
        accessor(e, x, "iarg", "variadic", boolT(e));
        defObj(
                e,
                x,
                "imember",
                "webidl member",
                e.rec(
                        e.p("kind", str(e)),
                        e.p("name", str(e)),
                        e.p("ty", t(e, "itype")),
                        e.p("args", e.listType(t(e, "iarg"))),
                        e.p("readonly", boolT(e)),
                        e.p("static_", boolT(e)),
                        e.p("special", str(e)),
                        e.p("value", str(e))));
        accessor(e, x, "imember", "value", str(e));
        accessor(e, x, "imember", "kind", str(e));
        accessor(e, x, "imember", "name", str(e));
        accessor(e, x, "imember", "ty", t(e, "itype"));
        accessor(e, x, "imember", "args", e.listType(t(e, "iarg")));
        accessor(e, x, "imember", "readonly", boolT(e));
        accessor(e, x, "imember", "static_", boolT(e));
        accessor(e, x, "imember", "special", str(e));
        defObj(
                e,
                x,
                "idef",
                "webidl definition",
                e.rec(
                        e.p("kind", str(e)),
                        e.p("name", str(e)),
                        e.p("parent", str(e)),
                        e.p("partial", boolT(e)),
                        e.p("members", e.listType(t(e, "imember"))),
                        e.p("strings", e.listType(str(e))),
                        e.p("ty", t(e, "itype"))));
        accessor(e, x, "idef", "kind", str(e));
        accessor(e, x, "idef", "name", str(e));
        accessor(e, x, "idef", "parent", str(e));
        accessor(e, x, "idef", "partial", boolT(e));
        accessor(e, x, "idef", "members", e.listType(t(e, "imember")));
        accessor(e, x, "idef", "strings", e.listType(str(e)));
        accessor(e, x, "idef", "ty", t(e, "itype"));
        defObj(
                e,
                x,
                "itable",
                "webidl definition table",
                e.rec(e.p("defs", e.mapType(str(e), t(e, "idef"))), e.p("typedefs", e.mapType(str(e), t(e, "itype")))));
        accessor(e, x, "itable", "defs", e.mapType(str(e), t(e, "idef")));
        accessor(e, x, "itable", "typedefs", e.mapType(str(e), t(e, "itype")));
        defObj(
                e,
                x,
                "ifound",
                "webidl member lookup",
                e.rec(
                        e.p("found", boolT(e)),
                        e.p("iface", str(e)),
                        e.p("member", t(e, "imember")),
                        e.p("count", intT(e))));
        accessor(e, x, "ifound", "found", boolT(e));
        accessor(e, x, "ifound", "iface", str(e));
        accessor(e, x, "ifound", "member", t(e, "imember"));
        accessor(e, x, "ifound", "count", intT(e));
        defObj(
                e,
                x,
                "iparam",
                "webidl abi parameter",
                e.rec(e.p("optional", boolT(e)), e.p("nullable", boolT(e)), e.p("kind", str(e))));
        accessor(e, x, "iparam", "optional", boolT(e));
        accessor(e, x, "iparam", "nullable", boolT(e));
        accessor(e, x, "iparam", "kind", str(e));
        defObj(
                e,
                x,
                "isig",
                "webidl abi signature",
                e.rec(
                        e.p("ok", boolT(e)),
                        e.p("err", str(e)),
                        e.p("iface", str(e)),
                        e.p("member", str(e)),
                        e.p("base", str(e)),
                        e.p("kind", str(e)),
                        e.p("self", boolT(e)),
                        e.p("args", e.listType(t(e, "iparam"))),
                        e.p("ret_out", boolT(e)),
                        e.p("ret_kind", str(e))));
        for (var f : new String[] {"ok", "self", "ret_out"}) accessor(e, x, "isig", f, boolT(e));
        for (var f : new String[] {"err", "iface", "member", "base", "kind", "ret_kind"}) {
            accessor(e, x, "isig", f, str(e));
        }
        accessor(e, x, "isig", "args", e.listType(t(e, "iparam")));
        for (var vo : new String[] {
            "tok", "lex", "cur", "itype", "iarg", "imember", "idef", "itable", "ifound", "iparam", "isig"
        }) {
            defIface(e, x, vo + "_m");
            markerType(e, x, vo, vo, vo + "_m", e.aspectDispatch(lv(e, vo + "_m_id")));
            x.add(e.implInterface(e.v(vo), t(e, vo + "_m")));
        }

        fnDef(e, x, "h_lex", str(e), lexT(e));
        fnDef(e, x, "h_peek", lc(e), str(e));
        fnDef(e, x, "h_next", lc(e), t(e, "tok"));
        fnDef(e, x, "h_accept", e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("text", str(e))), boolT(e));
        fnDef(e, x, "h_expect", e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("text", str(e))), e.tup());
        fnDef(e, x, "h_skip_balanced", e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("close", str(e))), e.tup());
        fnDef(e, x, "h_skip_ext", lc(e), e.tup());
        fnDef(e, x, "h_skip_to_semi", lc(e), e.tup());
        fnDef(e, x, "h_skip_default", lc(e), e.tup());
        fnDef(e, x, "h_type", lc(e), t(e, "itype"));
        fnDef(
                e,
                x,
                "h_type_args",
                e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("out", e.listType(t(e, "itype")))),
                e.tup());
        fnDef(
                e,
                x,
                "h_union_rest",
                e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("out", e.listType(t(e, "itype")))),
                e.tup());
        fnDef(
                e,
                x,
                "h_args",
                e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("out", e.listType(t(e, "iarg")))),
                e.tup());
        fnDef(e, x, "h_member", lc(e), t(e, "imember"));
        fnDef(
                e,
                x,
                "h_members",
                e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("out", e.listType(t(e, "imember")))),
                e.tup());
        fnDef(
                e,
                x,
                "h_enum_values",
                e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("out", e.listType(str(e)))),
                e.tup());
        fnDef(
                e,
                x,
                "h_def",
                e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("out", e.listType(t(e, "idef")))),
                boolT(e));
        fnDef(
                e,
                x,
                "h_defs",
                e.rec(e.p("lex", lexT(e)), e.p("cur", curT(e)), e.p("out", e.listType(t(e, "idef")))),
                e.tup());
        fnDef(e, x, "h_parse", str(e), e.listType(t(e, "idef")));
        fnDef(e, x, "h_table", e.listType(t(e, "idef")), t(e, "itable"));
        fnDef(e, x, "h_has", e.rec(e.p("defs", e.mapType(str(e), t(e, "idef"))), e.p("name", str(e))), boolT(e));
        fnDef(
                e,
                x,
                "h_has_td",
                e.rec(e.p("typedefs", e.mapType(str(e), t(e, "itype"))), e.p("name", str(e))),
                boolT(e));
        fnDef(e, x, "h_resolve", e.rec(e.p("table", t(e, "itable")), e.p("ty", t(e, "itype"))), t(e, "itype"));
        fnDef(e, x, "h_kind", e.rec(e.p("table", t(e, "itable")), e.p("ty", t(e, "itype"))), str(e));
        fnDef(
                e,
                x,
                "h_param",
                e.rec(e.p("table", t(e, "itable")), e.p("ty", t(e, "itype")), e.p("optional", boolT(e))),
                t(e, "iparam"));
        fnDef(
                e,
                x,
                "h_sig",
                e.rec(e.p("table", t(e, "itable")), e.p("iface", str(e)), e.p("member", str(e)), e.p("kind", str(e))),
                t(e, "isig"));
        fnDef(e, x, "h_sig_name", e.rec(e.p("table", t(e, "itable")), e.p("name", str(e))), t(e, "isig"));
        fnDef(
                e,
                x,
                "h_abi_ret",
                e.rec(e.p("table", t(e, "itable")), e.p("ty", t(e, "itype"))),
                e.rec(e.p("out", boolT(e)), e.p("kind", str(e))));
        fnDef(
                e,
                x,
                "h_find",
                e.rec(
                        e.p("table", t(e, "itable")),
                        e.p("iface", str(e)),
                        e.p("member", str(e)),
                        e.p("kind", str(e)),
                        e.p("index", intT(e))),
                t(e, "ifound"));
        fnDef(e, x, "h_mangle", e.rec(e.p("iface", str(e)), e.p("member", str(e)), e.p("kind", str(e))), str(e));
        fnDef(e, x, "h_number", str(e), e.typesDouble());
        fnDef(
                e,
                x,
                "h_const_value",
                e.rec(e.p("table", t(e, "itable")), e.p("iface", str(e)), e.p("member", str(e))),
                e.typesDouble());
        fnDef(e, x, "h_split_overload", str(e), e.rec(e.p("base", str(e)), e.p("index", intT(e))));

        {
            var emit = e.seq(e.ifOnly(
                    e.mb.operatorGt(e.fld(e.v("st"), "n"), num(e, 0)),
                    e.seq(
                            e.callM(
                                    e.v("toks"),
                                    "put",
                                    e.tup(
                                            toStr(e, e.fld(e.v("st"), "count")),
                                            mk(
                                                    e,
                                                    "tok",
                                                    e.p("kind", e.dstr("id")),
                                                    e.p("text", utf8(e, e.fld(e.v("st"), "tok")))))),
                            e.incr(e.fld(e.v("st"), "count")),
                            e.mb.set(e.fld(e.v("st"), "tok"), e.listNew(e.mb.typesByte())),
                            e.mb.set(e.fld(e.v("st"), "n"), e.intOf(0)))));
            var pushByte = e.seq(push(e, e.fld(e.v("st"), "tok"), e.v("b")), e.incr(e.fld(e.v("st"), "n")));
            var emitSym = e.seq(
                    e.callM(
                            e.v("toks"),
                            "put",
                            e.tup(
                                    toStr(e, e.fld(e.v("st"), "count")),
                                    mk(
                                            e,
                                            "tok",
                                            e.p("kind", e.dstr("sym")),
                                            e.p("text", utf8(e, e.listNew(e.mb.typesByte(), e.v("b"))))))),
                    e.incr(e.fld(e.v("st"), "count")));
            var emitStr = e.seq(
                    e.callM(
                            e.v("toks"),
                            "put",
                            e.tup(
                                    toStr(e, e.fld(e.v("st"), "count")),
                                    mk(
                                            e,
                                            "tok",
                                            e.p("kind", e.dstr("str")),
                                            e.p("text", utf8(e, e.fld(e.v("st"), "tok")))))),
                    e.incr(e.fld(e.v("st"), "count")),
                    e.mb.set(e.fld(e.v("st"), "tok"), e.listNew(e.mb.typesByte())),
                    e.mb.set(e.fld(e.v("st"), "n"), e.intOf(0)));
            var isWordChar = orB(
                    e,
                    inRange(e, e.v("n"), 'a', 'z'),
                    orB(
                            e,
                            inRange(e, e.v("n"), 'A', 'Z'),
                            orB(
                                    e,
                                    inRange(e, e.v("n"), '0', '9'),
                                    orB(e, byteIs(e, e.v("n"), '_'), byteIs(e, e.v("n"), '-')))));
            var numericWord = andB(
                    e,
                    e.mb.operatorGt(e.fld(e.v("st"), "n"), num(e, 0)),
                    orB(e, inRange(e, e.fld(e.v("st"), "first"), '0', '9'), byteIs(e, e.fld(e.v("st"), "first"), '-')));
            var isSpace = orB(
                    e,
                    byteIs(e, e.v("n"), ' '),
                    orB(e, byteIs(e, e.v("n"), '\n'), orB(e, byteIs(e, e.v("n"), '\t'), byteIs(e, e.v("n"), '\r'))));
            var plain = e.ifElse(
                    byteIs(e, e.v("n"), '/'),
                    e.seq(emit, e.mb.set(e.fld(e.v("st"), "mode"), e.intOf(1))),
                    e.ifElse(
                            byteIs(e, e.v("n"), '"'),
                            e.seq(emit, e.mb.set(e.fld(e.v("st"), "mode"), e.intOf(4))),
                            e.ifElse(
                                    isWordChar,
                                    e.seq(
                                            e.ifOnly(
                                                    e.eq(e.fld(e.v("st"), "n"), num(e, 0)),
                                                    e.mb.set(e.fld(e.v("st"), "first"), e.v("n"))),
                                            pushByte),
                                    e.ifElse(
                                            andB(e, byteIs(e, e.v("n"), '.'), numericWord),
                                            pushByte,
                                            e.ifElse(isSpace, emit, e.seq(emit, emitSym))))));
            fnImpl(
                    e,
                    x,
                    "h_lex",
                    body(
                            e,
                            e.let("toks", e.mapNew(str(e), lt(e, "tok"))),
                            e.let(
                                    "st",
                                    e.rec(
                                            e.p("mode", e.intOf(0)),
                                            e.p("tok", e.listNew(e.mb.typesByte())),
                                            e.p("n", e.intOf(0)),
                                            e.p("first", e.intOf(0)),
                                            e.p("count", e.intOf(0)))),
                            e.foreach(
                                    e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                    "b",
                                    e.seq(
                                            e.let("n", e.mb.as(e.v("b"), intT(e))),
                                            e.let("mode", e.fld(e.v("st"), "mode")),
                                            e.ifElse(
                                                    e.eq(e.v("mode"), num(e, 1)),
                                                    e.ifElse(
                                                            byteIs(e, e.v("n"), '/'),
                                                            e.mb.set(e.fld(e.v("st"), "mode"), e.intOf(2)),
                                                            e.ifElse(
                                                                    byteIs(e, e.v("n"), '*'),
                                                                    e.mb.set(e.fld(e.v("st"), "mode"), e.intOf(3)),
                                                                    e.mb.panic(e.s("webidl: stray '/'")))),
                                                    e.ifElse(
                                                            e.eq(e.v("mode"), num(e, 2)),
                                                            e.ifOnly(
                                                                    byteIs(e, e.v("n"), '\n'),
                                                                    e.mb.set(e.fld(e.v("st"), "mode"), e.intOf(0))),
                                                            e.ifElse(
                                                                    e.eq(e.v("mode"), num(e, 3)),
                                                                    e.ifOnly(
                                                                            byteIs(e, e.v("n"), '*'),
                                                                            e.mb.set(
                                                                                    e.fld(e.v("st"), "mode"),
                                                                                    e.intOf(5))),
                                                                    e.ifElse(
                                                                            e.eq(e.v("mode"), num(e, 5)),
                                                                            e.ifElse(
                                                                                    byteIs(e, e.v("n"), '/'),
                                                                                    e.mb.set(
                                                                                            e.fld(e.v("st"), "mode"),
                                                                                            e.intOf(0)),
                                                                                    e.ifOnly(
                                                                                            e.not(
                                                                                                    byteIs(
                                                                                                            e, e.v("n"),
                                                                                                            '*')),
                                                                                            e.mb.set(
                                                                                                    e.fld(
                                                                                                            e.v("st"),
                                                                                                            "mode"),
                                                                                                    e.intOf(3)))),
                                                                            e.ifElse(
                                                                                    e.eq(e.v("mode"), num(e, 4)),
                                                                                    e.ifElse(
                                                                                            byteIs(e, e.v("n"), '"'),
                                                                                            e.seq(
                                                                                                    emitStr,
                                                                                                    e.mb.set(
                                                                                                            e.fld(
                                                                                                                    e.v(
                                                                                                                            "st"),
                                                                                                                    "mode"),
                                                                                                            e.intOf(
                                                                                                                    0))),
                                                                                            pushByte),
                                                                                    plain))))))),
                            emit,
                            e.ret(mk(e, "lex", e.p("toks", e.v("toks")), e.p("n", e.fld(e.v("st"), "count"))))));
        }
        fnImpl(
                e,
                x,
                "h_peek",
                body(
                        e,
                        e.ifOnly(
                                e.mb.operatorGe(e.callM(a(e, "cur"), "i", e.tup()), e.callM(a(e, "lex"), "n", e.tup())),
                                retM(e, e.dstr(""))),
                        e.ret(e.callM(
                                e.callM(
                                        e.callM(a(e, "lex"), "toks", e.tup()),
                                        "get",
                                        toStr(e, e.callM(a(e, "cur"), "i", e.tup()))),
                                "text",
                                e.tup()))));
        fnImpl(
                e,
                x,
                "h_next",
                body(
                        e,
                        e.ifOnly(
                                e.mb.operatorGe(e.callM(a(e, "cur"), "i", e.tup()), e.callM(a(e, "lex"), "n", e.tup())),
                                e.mb.panic(e.s("webidl: unexpected end of input"))),
                        e.let(
                                "t",
                                e.callM(
                                        e.callM(a(e, "lex"), "toks", e.tup()),
                                        "get",
                                        toStr(e, e.callM(a(e, "cur"), "i", e.tup())))),
                        e.callM(a(e, "cur"), "adv", e.tup()),
                        e.ret(e.v("t"))));
        fnImpl(
                e,
                x,
                "h_accept",
                body(
                        e,
                        e.ifOnly(e.not(e.eq(peek(e), a(e, "text"))), retM(e, dfalse(e))),
                        e.callM(a(e, "cur"), "adv", e.tup()),
                        e.ret(e.b(true))));
        fnImplVoid(
                e,
                x,
                "h_expect",
                e.seq(
                        e.let("t", nextText(e)),
                        e.ifOnly(
                                e.not(e.eq(e.v("t"), a(e, "text"))),
                                e.mb.panic(e.cat(
                                        e.s("webidl: expected '"),
                                        a(e, "text"),
                                        e.s("' but found '"),
                                        e.v("t"),
                                        e.s("'"))))));
        fnImplVoid(
                e,
                x,
                "h_skip_balanced",
                e.seq(
                        e.let("t", nextText(e)),
                        e.ifOnly(
                                e.not(e.eq(e.v("t"), a(e, "close"))),
                                e.seq(
                                        e.ifOnly(e.eq(e.v("t"), e.s("(")), skipBalanced(e, ")")),
                                        e.ifOnly(e.eq(e.v("t"), e.s("[")), skipBalanced(e, "]")),
                                        e.ifOnly(e.eq(e.v("t"), e.s("{")), skipBalanced(e, "}")),
                                        hf(e, "h_skip_balanced", lcArgs(e, e.p("close", a(e, "close"))))))));
        fnImplVoid(
                e,
                x,
                "h_skip_ext",
                e.ifOnly(accept(e, "["), e.seq(skipBalanced(e, "]"), hf(e, "h_skip_ext", lcArgs(e)))));
        fnImplVoid(
                e,
                x,
                "h_skip_to_semi",
                e.seq(
                        e.let("t", nextText(e)),
                        e.ifOnly(
                                e.not(e.eq(e.v("t"), e.s(";"))),
                                e.seq(
                                        e.ifOnly(e.eq(e.v("t"), e.s("(")), skipBalanced(e, ")")),
                                        e.ifOnly(e.eq(e.v("t"), e.s("[")), skipBalanced(e, "]")),
                                        e.ifOnly(e.eq(e.v("t"), e.s("{")), skipBalanced(e, "}")),
                                        hf(e, "h_skip_to_semi", lcArgs(e))))));
        fnImplVoid(
                e,
                x,
                "h_skip_default",
                e.seq(
                        e.let("p", peek(e)),
                        e.ifOnly(
                                e.not(orB(
                                        e,
                                        e.eq(e.v("p"), e.s(",")),
                                        orB(e, e.eq(e.v("p"), e.s(")")), e.eq(e.v("p"), e.s(";"))))),
                                e.seq(
                                        e.let("t", nextText(e)),
                                        e.ifOnly(e.eq(e.v("t"), e.s("(")), skipBalanced(e, ")")),
                                        e.ifOnly(e.eq(e.v("t"), e.s("[")), skipBalanced(e, "]")),
                                        e.ifOnly(e.eq(e.v("t"), e.s("{")), skipBalanced(e, "}")),
                                        hf(e, "h_skip_default", lcArgs(e))))));
        fnImpl(
                e,
                x,
                "h_type",
                body(
                        e,
                        hf(e, "h_skip_ext", lcArgs(e)),
                        e.let("first", nextText(e)),
                        e.let("name", e.rec(e.p("v", e.dstr("")))),
                        e.let("args", e.listNew(lt(e, "itype"))),
                        e.ifElse(
                                e.eq(e.v("first"), e.s("(")),
                                e.seq(
                                        e.mb.set(e.fld(e.v("name"), "v"), e.dstr("union")),
                                        push(e, e.v("args"), parseType(e)),
                                        hf(e, "h_union_rest", lcArgs(e, e.p("out", e.v("args")))),
                                        expect(e, ")")),
                                e.seq(
                                        e.mb.set(e.fld(e.v("name"), "v"), e.v("first")),
                                        e.ifOnly(
                                                orB(
                                                        e,
                                                        e.eq(e.v("first"), e.s("unsigned")),
                                                        e.eq(e.v("first"), e.s("unrestricted"))),
                                                e.mb.set(
                                                        e.fld(e.v("name"), "v"),
                                                        e.cat(e.fld(e.v("name"), "v"), e.s(" "), nextText(e)))),
                                        e.ifOnly(
                                                andB(
                                                        e,
                                                        orB(
                                                                e,
                                                                e.eq(e.fld(e.v("name"), "v"), e.s("long")),
                                                                e.eq(e.fld(e.v("name"), "v"), e.s("unsigned long"))),
                                                        e.eq(peek(e), e.s("long"))),
                                                e.mb.set(
                                                        e.fld(e.v("name"), "v"),
                                                        e.cat(e.fld(e.v("name"), "v"), e.s(" "), nextText(e)))),
                                        e.ifOnly(
                                                accept(e, "<"),
                                                e.seq(
                                                        hf(e, "h_type_args", lcArgs(e, e.p("out", e.v("args")))),
                                                        expect(e, ">"))))),
                        e.let("nullable", accept(e, "?")),
                        e.ret(itype(e, e.fld(e.v("name"), "v"), e.v("nullable"), e.v("args")))));
        fnImplVoid(
                e,
                x,
                "h_type_args",
                e.seq(
                        push(e, a(e, "out"), parseType(e)),
                        e.ifOnly(accept(e, ","), hf(e, "h_type_args", lcArgs(e, e.p("out", a(e, "out")))))));
        fnImplVoid(
                e,
                x,
                "h_union_rest",
                e.ifOnly(
                        accept(e, "or"),
                        e.seq(
                                push(e, a(e, "out"), parseType(e)),
                                hf(e, "h_union_rest", lcArgs(e, e.p("out", a(e, "out")))))));
        fnImplVoid(
                e,
                x,
                "h_args",
                e.ifOnly(
                        e.not(accept(e, ")")),
                        e.seq(
                                hf(e, "h_skip_ext", lcArgs(e)),
                                e.let("optional", accept(e, "optional")),
                                e.let("ty", parseType(e)),
                                e.let("variadic", accept(e, ".")),
                                e.ifOnly(e.v("variadic"), e.seq(expect(e, "."), expect(e, "."))),
                                e.let("name", nextText(e)),
                                e.ifOnly(accept(e, "="), hf(e, "h_skip_default", lcArgs(e))),
                                push(
                                        e,
                                        a(e, "out"),
                                        mk(
                                                e,
                                                "iarg",
                                                e.p("name", e.v("name")),
                                                e.p("ty", e.v("ty")),
                                                e.p("optional", e.v("optional")),
                                                e.p("variadic", e.v("variadic")))),
                                e.ifElse(
                                        accept(e, ","),
                                        hf(e, "h_args", lcArgs(e, e.p("out", a(e, "out")))),
                                        expect(e, ")")))));
        fnImpl(
                e,
                x,
                "h_member",
                body(
                        e,
                        hf(e, "h_skip_ext", lcArgs(e)),
                        e.let(
                                "st",
                                e.rec(
                                        e.p("readonly", e.intOf(0)),
                                        e.p("static_", e.intOf(0)),
                                        e.p("special", e.dstr("")))),
                        e.ifOnly(accept(e, "static"), e.mb.set(e.fld(e.v("st"), "static_"), e.intOf(1))),
                        e.ifOnly(
                                accept(e, "stringifier"),
                                e.ifOnly(
                                        accept(e, ";"),
                                        retM(
                                                e,
                                                imember(
                                                        e,
                                                        e.dstr("other"),
                                                        e.dstr("stringifier"),
                                                        noType(e),
                                                        e.listNew(lt(e, "iarg")),
                                                        e.b(false),
                                                        e.b(false),
                                                        e.dstr(""))))),
                        e.ifOnly(accept(e, "inherit"), e.seq()),
                        e.ifOnly(accept(e, "readonly"), e.mb.set(e.fld(e.v("st"), "readonly"), e.intOf(1))),
                        e.ifOnly(accept(e, "required"), e.seq()),
                        e.ifOnly(accept(e, "async"), e.seq()),
                        e.ifOnly(accept(e, "getter"), e.mb.set(e.fld(e.v("st"), "special"), e.dstr("getter"))),
                        e.ifOnly(accept(e, "setter"), e.mb.set(e.fld(e.v("st"), "special"), e.dstr("setter"))),
                        e.ifOnly(accept(e, "deleter"), e.mb.set(e.fld(e.v("st"), "special"), e.dstr("deleter"))),
                        e.ifOnly(
                                accept(e, "const"),
                                e.seq(
                                        e.let("cty", parseType(e)),
                                        e.let("cname", nextText(e)),
                                        expect(e, "="),
                                        e.let("cvalue", nextText(e)),
                                        expect(e, ";"),
                                        retM(
                                                e,
                                                imember(
                                                        e,
                                                        e.dstr("const"),
                                                        e.v("cname"),
                                                        e.v("cty"),
                                                        e.listNew(lt(e, "iarg")),
                                                        e.b(true),
                                                        e.b(true),
                                                        e.dstr(""),
                                                        e.v("cvalue"))))),
                        e.let("p", peek(e)),
                        e.ifOnly(
                                orB(
                                        e,
                                        e.eq(e.v("p"), e.s("iterable")),
                                        orB(e, e.eq(e.v("p"), e.s("maplike")), e.eq(e.v("p"), e.s("setlike")))),
                                e.seq(
                                        e.let("kw", nextText(e)),
                                        hf(e, "h_skip_to_semi", lcArgs(e)),
                                        retM(
                                                e,
                                                imember(
                                                        e,
                                                        e.dstr("other"),
                                                        e.v("kw"),
                                                        noType(e),
                                                        e.listNew(lt(e, "iarg")),
                                                        e.b(false),
                                                        e.b(false),
                                                        e.dstr(""))))),
                        e.ifOnly(
                                accept(e, "constructor"),
                                e.seq(
                                        expect(e, "("),
                                        e.let("cargs", e.listNew(lt(e, "iarg"))),
                                        hf(e, "h_args", lcArgs(e, e.p("out", e.v("cargs")))),
                                        expect(e, ";"),
                                        retM(
                                                e,
                                                imember(
                                                        e,
                                                        e.dstr("ctor"),
                                                        e.dstr("constructor"),
                                                        noType(e),
                                                        e.v("cargs"),
                                                        e.b(false),
                                                        e.b(false),
                                                        e.dstr(""))))),
                        e.ifOnly(
                                accept(e, "attribute"),
                                e.seq(
                                        e.let("aty", parseType(e)),
                                        e.let("aname", nextText(e)),
                                        expect(e, ";"),
                                        retM(
                                                e,
                                                imember(
                                                        e,
                                                        e.dstr("attr"),
                                                        e.v("aname"),
                                                        e.v("aty"),
                                                        e.listNew(lt(e, "iarg")),
                                                        e.eq(e.fld(e.v("st"), "readonly"), num(e, 1)),
                                                        e.eq(e.fld(e.v("st"), "static_"), num(e, 1)),
                                                        e.fld(e.v("st"), "special"))))),
                        e.let("ty", parseType(e)),
                        e.let("name", e.ifElse(e.eq(peek(e), e.s("(")), e.dstr(""), nextText(e))),
                        e.ifOnly(accept(e, "="), hf(e, "h_skip_default", lcArgs(e))),
                        e.ifOnly(
                                accept(e, ";"),
                                retM(
                                        e,
                                        imember(
                                                e,
                                                e.dstr("field"),
                                                e.v("name"),
                                                e.v("ty"),
                                                e.listNew(lt(e, "iarg")),
                                                e.b(false),
                                                e.b(false),
                                                e.dstr("")))),
                        expect(e, "("),
                        e.let("args", e.listNew(lt(e, "iarg"))),
                        hf(e, "h_args", lcArgs(e, e.p("out", e.v("args")))),
                        expect(e, ";"),
                        e.ret(imember(
                                e,
                                e.dstr("op"),
                                e.v("name"),
                                e.v("ty"),
                                e.v("args"),
                                e.eq(e.fld(e.v("st"), "readonly"), num(e, 1)),
                                e.eq(e.fld(e.v("st"), "static_"), num(e, 1)),
                                e.fld(e.v("st"), "special")))));
        fnImplVoid(
                e,
                x,
                "h_members",
                e.ifOnly(
                        e.not(accept(e, "}")),
                        e.seq(
                                push(e, a(e, "out"), hf(e, "h_member", lcArgs(e))),
                                hf(e, "h_members", lcArgs(e, e.p("out", a(e, "out")))))));
        fnImplVoid(
                e,
                x,
                "h_enum_values",
                e.seq(
                        e.let("t", next(e)),
                        e.ifOnly(
                                e.not(e.eq(e.callM(e.v("t"), "text", e.tup()), e.s("}"))),
                                e.seq(
                                        e.ifOnly(
                                                e.eq(e.callM(e.v("t"), "kind", e.tup()), e.s("str")),
                                                push(e, a(e, "out"), e.callM(e.v("t"), "text", e.tup()))),
                                        hf(e, "h_enum_values", lcArgs(e, e.p("out", a(e, "out"))))))));
        fnImpl(
                e,
                x,
                "h_def",
                body(
                        e,
                        hf(e, "h_skip_ext", lcArgs(e)),
                        e.ifOnly(e.eq(peek(e), e.s("")), retM(e, dfalse(e))),
                        e.let("partial", accept(e, "partial")),
                        e.let("kw", nextText(e)),
                        e.ifOnly(
                                e.eq(e.v("kw"), e.s("interface")),
                                e.seq(
                                        e.let("mixin", accept(e, "mixin")),
                                        e.let("name", nextText(e)),
                                        e.let("parent", e.ifElse(accept(e, ":"), nextText(e), e.dstr(""))),
                                        expect(e, "{"),
                                        e.let("members", e.listNew(lt(e, "imember"))),
                                        hf(e, "h_members", lcArgs(e, e.p("out", e.v("members")))),
                                        expect(e, ";"),
                                        pushDef(
                                                e,
                                                idef(
                                                        e,
                                                        e.ifElse(e.v("mixin"), e.dstr("mixin"), e.dstr("interface")),
                                                        e.v("name"),
                                                        e.v("parent"),
                                                        e.v("partial"),
                                                        e.v("members"),
                                                        noType(e))),
                                        retM(e, e.b(true)))),
                        e.ifOnly(
                                orB(e, e.eq(e.v("kw"), e.s("dictionary")), e.eq(e.v("kw"), e.s("namespace"))),
                                e.seq(
                                        e.let("name", nextText(e)),
                                        e.let("parent", e.ifElse(accept(e, ":"), nextText(e), e.dstr(""))),
                                        expect(e, "{"),
                                        e.let("members", e.listNew(lt(e, "imember"))),
                                        hf(e, "h_members", lcArgs(e, e.p("out", e.v("members")))),
                                        expect(e, ";"),
                                        pushDef(
                                                e,
                                                idef(
                                                        e,
                                                        e.v("kw"),
                                                        e.v("name"),
                                                        e.v("parent"),
                                                        e.v("partial"),
                                                        e.v("members"),
                                                        noType(e))),
                                        retM(e, e.b(true)))),
                        e.ifOnly(
                                e.eq(e.v("kw"), e.s("enum")),
                                e.seq(
                                        e.let("name", nextText(e)),
                                        expect(e, "{"),
                                        e.let(
                                                "def",
                                                idef(
                                                        e,
                                                        e.dstr("enum"),
                                                        e.v("name"),
                                                        e.dstr(""),
                                                        e.b(false),
                                                        e.listNew(lt(e, "imember")),
                                                        noType(e))),
                                        hf(
                                                e,
                                                "h_enum_values",
                                                lcArgs(e, e.p("out", e.callM(e.v("def"), "strings", e.tup())))),
                                        expect(e, ";"),
                                        pushDef(e, e.v("def")),
                                        retM(e, e.b(true)))),
                        e.ifOnly(
                                e.eq(e.v("kw"), e.s("typedef")),
                                e.seq(
                                        e.let("ty", parseType(e)),
                                        e.let("name", nextText(e)),
                                        expect(e, ";"),
                                        e.let(
                                                "def",
                                                idef(
                                                        e,
                                                        e.dstr("typedef"),
                                                        e.v("name"),
                                                        e.dstr(""),
                                                        e.b(false),
                                                        e.listNew(lt(e, "imember")),
                                                        e.v("ty"))),
                                        pushDef(e, e.v("def")),
                                        retM(e, e.b(true)))),
                        e.ifOnly(
                                e.eq(e.v("kw"), e.s("callback")),
                                e.seq(
                                        e.ifOnly(
                                                accept(e, "interface"),
                                                e.seq(
                                                        e.let("cname", nextText(e)),
                                                        expect(e, "{"),
                                                        e.let("cmembers", e.listNew(lt(e, "imember"))),
                                                        hf(e, "h_members", lcArgs(e, e.p("out", e.v("cmembers")))),
                                                        expect(e, ";"),
                                                        pushDef(
                                                                e,
                                                                idef(
                                                                        e,
                                                                        e.dstr("callback_interface"),
                                                                        e.v("cname"),
                                                                        e.dstr(""),
                                                                        e.b(false),
                                                                        e.v("cmembers"),
                                                                        noType(e))),
                                                        retM(e, e.b(true)))),
                                        e.let("name", nextText(e)),
                                        expect(e, "="),
                                        e.let("ty", parseType(e)),
                                        expect(e, "("),
                                        e.let("cargs", e.listNew(lt(e, "iarg"))),
                                        hf(e, "h_args", lcArgs(e, e.p("out", e.v("cargs")))),
                                        expect(e, ";"),
                                        e.let(
                                                "def",
                                                idef(
                                                        e,
                                                        e.dstr("callback"),
                                                        e.v("name"),
                                                        e.dstr(""),
                                                        e.b(false),
                                                        e.listNew(lt(e, "imember")),
                                                        e.v("ty"))),
                                        pushDef(e, e.v("def")),
                                        retM(e, e.b(true)))),
                        expect(e, "includes"),
                        e.let("target", nextText(e)),
                        expect(e, ";"),
                        pushDef(
                                e,
                                idef(
                                        e,
                                        e.dstr("includes"),
                                        e.v("kw"),
                                        e.v("target"),
                                        e.b(false),
                                        e.listNew(lt(e, "imember")),
                                        noType(e))),
                        e.ret(e.b(true))));
        fnImplVoid(
                e,
                x,
                "h_defs",
                e.ifOnly(
                        hf(e, "h_def", lcArgs(e, e.p("out", a(e, "out")))),
                        hf(e, "h_defs", lcArgs(e, e.p("out", a(e, "out"))))));
        fnImpl(
                e,
                x,
                "h_parse",
                body(
                        e,
                        e.let("lex", hf(e, "h_lex", e.v("arg"))),
                        e.let("cur", mk(e, "cur", e.p("i", e.intOf(0)))),
                        e.let("out", e.listNew(lt(e, "idef"))),
                        hf(e, "h_defs", e.rec(e.p("lex", e.v("lex")), e.p("cur", e.v("cur")), e.p("out", e.v("out")))),
                        e.ret(e.v("out"))));
        fnImpl(
                e,
                x,
                "h_has",
                body(
                        e,
                        e.foreach(
                                a(e, "defs"),
                                "en",
                                e.ifOnly(
                                        e.eq(e.fld(e.v("en"), "key"), a(e, "name")),
                                        retM(e, e.mb.as(e.b(true), boolT(e))))),
                        e.ret(dfalse(e))));
        fnImpl(
                e,
                x,
                "h_has_td",
                body(
                        e,
                        e.foreach(
                                a(e, "typedefs"),
                                "en",
                                e.ifOnly(
                                        e.eq(e.fld(e.v("en"), "key"), a(e, "name")),
                                        retM(e, e.mb.as(e.b(true), boolT(e))))),
                        e.ret(dfalse(e))));
        fnImpl(
                e,
                x,
                "h_resolve",
                body(
                        e,
                        e.let("tds", e.callM(a(e, "table"), "typedefs", e.tup())),
                        e.let("name", e.callM(a(e, "ty"), "name", e.tup())),
                        e.ifOnly(
                                e.not(hf(e, "h_has_td", e.rec(e.p("typedefs", e.v("tds")), e.p("name", e.v("name"))))),
                                retM(e, a(e, "ty"))),
                        e.let(
                                "r",
                                hf(
                                        e,
                                        "h_resolve",
                                        e.rec(
                                                e.p("table", a(e, "table")),
                                                e.p("ty", e.callM(e.v("tds"), "get", e.v("name")))))),
                        e.ret(itype(
                                e,
                                e.callM(e.v("r"), "name", e.tup()),
                                orB(
                                        e,
                                        e.callM(a(e, "ty"), "nullable", e.tup()),
                                        e.callM(e.v("r"), "nullable", e.tup())),
                                e.callM(e.v("r"), "args", e.tup())))));
        {
            var scalars = new String[][] {
                {"DOMString", "str"},
                {"USVString", "str"},
                {"ByteString", "str"},
                {"CSSOMString", "str"},
                {"boolean", "bool"},
                {"byte", "i8"},
                {"octet", "u8"},
                {"short", "i16"},
                {"unsigned short", "u16"},
                {"long", "i32"},
                {"unsigned long", "u32"},
                {"long long", "i64"},
                {"unsigned long long", "u64"},
                {"float", "f32"},
                {"unrestricted float", "f32"},
                {"double", "f64"},
                {"unrestricted double", "f64"},
                {"undefined", "void"}
            };
            var kindEntries = new TList<EcoMb.P>();
            for (var sc : scalars) kindEntries.add(e.p(sc[0], e.s(sc[1])));
            var bufEntries = new TList<EcoMb.P>();
            for (var name : BUF_NAMES) bufEntries.add(e.p(name, e.s("buf")));
            fnImpl(
                    e,
                    x,
                    "h_kind",
                    body(
                            e,
                            e.let("bufs", e.mapNew(str(e), str(e), bufEntries.toArray(EcoMb.P[]::new))),
                            e.ifOnly(
                                    e.callM(e.v("bufs"), "has", e.callM(a(e, "ty"), "name", e.tup())),
                                    retM(e, e.dstr("buf"))),
                            e.let("r", hf(e, "h_resolve", e.rec(e.p("table", a(e, "table")), e.p("ty", a(e, "ty"))))),
                            e.let("name", e.callM(e.v("r"), "name", e.tup())),
                            e.ifOnly(e.callM(e.v("bufs"), "has", e.v("name")), retM(e, e.dstr("buf"))),
                            e.ifOnly(
                                    e.eq(e.v("name"), e.s("union")),
                                    e.seq(
                                            e.foreach(
                                                    e.callM(e.v("r"), "args", e.tup()),
                                                    "ua",
                                                    e.ifOnly(
                                                            e.eq(
                                                                    hf(
                                                                            e,
                                                                            "h_kind",
                                                                            e.rec(
                                                                                    e.p("table", a(e, "table")),
                                                                                    e.p("ty", e.v("ua")))),
                                                                    e.s("str")),
                                                            retM(e, e.dstr("str")))),
                                            retM(e, e.dstr("handle")))),
                            e.let("scalars", e.mapNew(str(e), str(e), kindEntries.toArray(EcoMb.P[]::new))),
                            e.foreach(
                                    e.v("scalars"),
                                    "en",
                                    e.ifOnly(
                                            e.eq(e.fld(e.v("en"), "key"), e.v("name")),
                                            retM(e, e.fld(e.v("en"), "value")))),
                            e.let("defs", e.callM(a(e, "table"), "defs", e.tup())),
                            e.ifOnly(
                                    andB(
                                            e,
                                            hf(e, "h_has", e.rec(e.p("defs", e.v("defs")), e.p("name", e.v("name")))),
                                            e.eq(
                                                    e.callM(e.callM(e.v("defs"), "get", e.v("name")), "kind", e.tup()),
                                                    e.s("enum"))),
                                    retM(e, e.dstr("str"))),
                            e.ret(e.dstr("handle"))));
        }
        fnImpl(
                e,
                x,
                "h_param",
                body(
                        e,
                        e.let("r", hf(e, "h_resolve", e.rec(e.p("table", a(e, "table")), e.p("ty", a(e, "ty"))))),
                        e.ret(mk(
                                e,
                                "iparam",
                                e.p("optional", a(e, "optional")),
                                e.p("nullable", e.callM(e.v("r"), "nullable", e.tup())),
                                e.p(
                                        "kind",
                                        hf(e, "h_kind", e.rec(e.p("table", a(e, "table")), e.p("ty", a(e, "ty")))))))));
        fnImpl(
                e,
                x,
                "h_abi_ret",
                body(
                        e,
                        e.let("r", hf(e, "h_resolve", e.rec(e.p("table", a(e, "table")), e.p("ty", a(e, "ty"))))),
                        e.let("k", hf(e, "h_kind", e.rec(e.p("table", a(e, "table")), e.p("ty", a(e, "ty"))))),
                        e.ret(e.rec(e.p("out", e.callM(e.v("r"), "nullable", e.tup())), e.p("kind", e.v("k"))))));
        fnImpl(
                e,
                x,
                "h_find",
                body(
                        e,
                        e.let("defs", e.callM(a(e, "table"), "defs", e.tup())),
                        e.let("none", ifound(e, e.b(false), e.dstr(""), noMember(e), e.intOf(0))),
                        e.ifOnly(
                                e.not(hf(e, "h_has", e.rec(e.p("defs", e.v("defs")), e.p("name", a(e, "iface"))))),
                                retM(e, e.v("none"))),
                        e.let("def", e.callM(e.v("defs"), "get", a(e, "iface"))),
                        e.let("hits", e.listNew(lt(e, "imember"))),
                        e.foreach(
                                e.callM(e.v("def"), "members", e.tup()),
                                "m",
                                e.ifOnly(
                                        andB(
                                                e,
                                                e.eq(e.callM(e.v("m"), "name", e.tup()), a(e, "member")),
                                                e.eq(e.callM(e.v("m"), "kind", e.tup()), a(e, "kind"))),
                                        push(e, e.v("hits"), e.v("m")))),
                        countLet(e, "nh"),
                        countLoop(e, "nh", e.v("hits")),
                        e.let("count", e.fld(e.v("nh"), "v")),
                        e.ifOnly(
                                e.eq(e.v("count"), num(e, 0)),
                                e.seq(
                                        e.ifOnly(
                                                e.eq(e.callM(e.v("def"), "parent", e.tup()), e.s("")),
                                                retM(e, e.v("none"))),
                                        retM(
                                                e,
                                                hf(
                                                        e,
                                                        "h_find",
                                                        e.rec(
                                                                e.p("table", a(e, "table")),
                                                                e.p("iface", e.callM(e.v("def"), "parent", e.tup())),
                                                                e.p("member", a(e, "member")),
                                                                e.p("kind", a(e, "kind")),
                                                                e.p("index", a(e, "index"))))))),
                        e.let(
                                "want",
                                e.ifElse(
                                        andB(
                                                e,
                                                e.mb.operatorLt(a(e, "index"), num(e, 0)),
                                                e.eq(e.v("count"), num(e, 1))),
                                        e.intOf(0),
                                        a(e, "index"))),
                        e.let("i", e.rec(e.p("v", e.intOf(0)))),
                        e.foreach(
                                e.v("hits"),
                                "h",
                                e.seq(
                                        e.ifOnly(
                                                e.eq(e.fld(e.v("i"), "v"), e.v("want")),
                                                retM(e, ifound(e, e.b(true), a(e, "iface"), e.v("h"), e.v("count")))),
                                        e.incr(e.fld(e.v("i"), "v")))),
                        e.ret(ifound(e, e.b(false), a(e, "iface"), noMember(e), e.v("count")))));
        fnImpl(
                e,
                x,
                "h_number",
                body(
                        e,
                        e.let(
                                "st",
                                e.rec(
                                        e.p("v", e.mb.as(e.n(0), e.typesDouble())),
                                        e.p("neg", e.intOf(0)),
                                        e.p("hex", e.intOf(0)),
                                        e.p("i", e.intOf(0)))),
                        e.foreach(
                                e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                "b",
                                e.seq(
                                        e.let("n", e.mb.as(e.v("b"), intT(e))),
                                        e.let("i", e.fld(e.v("st"), "i")),
                                        e.let(
                                                "digit",
                                                e.ifElse(
                                                        inRange(e, e.v("n"), '0', '9'),
                                                        e.mb.operatorSub(e.v("n"), e.intOf('0')),
                                                        e.ifElse(
                                                                inRange(e, e.v("n"), 'a', 'f'),
                                                                e.mb.operatorSub(e.v("n"), e.intOf('a' - 10)),
                                                                e.ifElse(
                                                                        inRange(e, e.v("n"), 'A', 'F'),
                                                                        e.mb.operatorSub(e.v("n"), e.intOf('A' - 10)),
                                                                        e.intOf(-1))))),
                                        e.ifElse(
                                                andB(e, e.eq(e.v("i"), num(e, 0)), byteIs(e, e.v("n"), '-')),
                                                e.mb.set(e.fld(e.v("st"), "neg"), e.intOf(1)),
                                                e.ifElse(
                                                        andB(
                                                                e,
                                                                e.eq(e.v("i"), num(e, 1)),
                                                                orB(
                                                                        e,
                                                                        byteIs(e, e.v("n"), 'x'),
                                                                        byteIs(e, e.v("n"), 'X'))),
                                                        e.mb.set(e.fld(e.v("st"), "hex"), e.intOf(1)),
                                                        e.ifElse(
                                                                orB(
                                                                        e,
                                                                        e.mb.operatorLt(e.v("digit"), num(e, 0)),
                                                                        andB(
                                                                                e,
                                                                                e.eq(
                                                                                        e.fld(e.v("st"), "hex"),
                                                                                        num(e, 0)),
                                                                                e.mb.operatorGt(
                                                                                        e.v("digit"), num(e, 9)))),
                                                                e.mb.panic(
                                                                        e.cat(
                                                                                e.s("webidl: malformed number "),
                                                                                e.v("arg"))),
                                                                e.mb.set(
                                                                        e.fld(e.v("st"), "v"),
                                                                        e.mb.operatorAdd(
                                                                                e.mb.operatorMul(
                                                                                        e.fld(e.v("st"), "v"),
                                                                                        e.ifElse(
                                                                                                e.eq(
                                                                                                        e.fld(
                                                                                                                e.v(
                                                                                                                        "st"),
                                                                                                                "hex"),
                                                                                                        num(e, 1)),
                                                                                                e.mb.as(
                                                                                                        e.n(16),
                                                                                                        e
                                                                                                                .typesDouble()),
                                                                                                e.mb.as(
                                                                                                        e.n(10),
                                                                                                        e
                                                                                                                .typesDouble()))),
                                                                                e.mb.as(
                                                                                        e.v("digit"),
                                                                                        e.typesDouble())))))),
                                        e.incr(e.fld(e.v("st"), "i")))),
                        e.ret(e.ifElse(
                                e.eq(e.fld(e.v("st"), "neg"), num(e, 1)),
                                e.mb.operatorSub(e.mb.as(e.n(0), e.typesDouble()), e.fld(e.v("st"), "v")),
                                e.fld(e.v("st"), "v")))));
        fnImpl(
                e,
                x,
                "h_const_value",
                body(
                        e,
                        e.let(
                                "f",
                                hf(
                                        e,
                                        "h_find",
                                        e.rec(
                                                e.p("table", a(e, "table")),
                                                e.p("iface", a(e, "iface")),
                                                e.p("member", a(e, "member")),
                                                e.p("kind", e.s("const")),
                                                e.p("index", e.intOf(-1))))),
                        e.ifOnly(
                                e.not(e.callM(e.v("f"), "found", e.tup())),
                                e.mb.panic(
                                        e.cat(e.s("webidl: no const "), a(e, "member"), e.s(" on "), a(e, "iface")))),
                        e.ret(hf(e, "h_number", e.callM(e.callM(e.v("f"), "member", e.tup()), "value", e.tup())))));
        fnImpl(
                e,
                x,
                "h_split_overload",
                body(
                        e,
                        e.let(
                                "st",
                                e.rec(
                                        e.p("hash", e.intOf(0)),
                                        e.p("a", e.listNew(e.mb.typesByte())),
                                        e.p("b", e.listNew(e.mb.typesByte())))),
                        e.foreach(
                                e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                "b",
                                e.ifElse(
                                        andB(
                                                e,
                                                e.eq(e.fld(e.v("st"), "hash"), num(e, 0)),
                                                byteIs(e, e.mb.as(e.v("b"), intT(e)), '#')),
                                        e.mb.set(e.fld(e.v("st"), "hash"), e.intOf(1)),
                                        e.ifElse(
                                                e.eq(e.fld(e.v("st"), "hash"), num(e, 0)),
                                                push(e, e.fld(e.v("st"), "a"), e.v("b")),
                                                push(e, e.fld(e.v("st"), "b"), e.v("b"))))),
                        e.ifOnly(
                                e.eq(e.fld(e.v("st"), "hash"), num(e, 0)),
                                retM(e, e.rec(e.p("base", e.v("arg")), e.p("index", e.intOf(-1))))),
                        e.ret(e.rec(
                                e.p("base", utf8(e, e.fld(e.v("st"), "a"))),
                                e.p("index", e.mb.as(hf(e, "h_number", utf8(e, e.fld(e.v("st"), "b"))), intT(e)))))));
        fnImpl(
                e,
                x,
                "h_sig",
                body(
                        e,
                        e.let("args", e.listNew(lt(e, "iparam"))),
                        e.let("sp", hf(e, "h_split_overload", a(e, "member"))),
                        e.let(
                                "st",
                                e.rec(
                                        e.p("ok", e.intOf(1)),
                                        e.p("err", e.dstr("")),
                                        e.p("iface", a(e, "iface")),
                                        e.p("member", a(e, "member")),
                                        e.p("base", e.fld(e.v("sp"), "base")),
                                        e.p("self", e.intOf(0)),
                                        e.p("ret_out", e.intOf(0)),
                                        e.p("ret_kind", e.dstr("void")))),
                        e.ifOnly(
                                e.eq(a(e, "kind"), e.s("global")),
                                e.seq(e.mb.set(e.fld(e.v("st"), "ret_kind"), e.dstr("handle")), retM(e, sigOf(e)))),
                        e.ifOnly(
                                e.eq(a(e, "kind"), e.s("callback")),
                                e.seq(
                                        e.mb.set(e.fld(e.v("st"), "ret_kind"), e.dstr("handle")),
                                        push(
                                                e,
                                                e.v("args"),
                                                mk(
                                                        e,
                                                        "iparam",
                                                        e.p("optional", e.b(false)),
                                                        e.p("nullable", e.b(false)),
                                                        e.p("kind", e.dstr("str")))),
                                        retM(e, sigOf(e)))),
                        e.ifOnly(
                                e.eq(a(e, "kind"), e.s("ctor")),
                                e.seq(
                                        e.mb.set(e.fld(e.v("st"), "member"), e.dstr("constructor")),
                                        e.mb.set(e.fld(e.v("st"), "base"), e.dstr("constructor")))),
                        e.let(
                                "mkind",
                                e.ifElse(
                                        e.eq(a(e, "kind"), e.s("op")),
                                        e.dstr("op"),
                                        e.ifElse(e.eq(a(e, "kind"), e.s("ctor")), e.dstr("ctor"), e.dstr("attr")))),
                        e.let(
                                "f",
                                hf(
                                        e,
                                        "h_find",
                                        e.rec(
                                                e.p("table", a(e, "table")),
                                                e.p("iface", a(e, "iface")),
                                                e.p("member", e.fld(e.v("st"), "base")),
                                                e.p("kind", e.v("mkind")),
                                                e.p("index", e.fld(e.v("sp"), "index"))))),
                        e.ifOnly(
                                andB(
                                        e,
                                        e.not(e.callM(e.v("f"), "found", e.tup())),
                                        e.mb.operatorGt(e.callM(e.v("f"), "count", e.tup()), num(e, 1))),
                                sigErr(
                                        e,
                                        e.cat(
                                                toStr(e, e.callM(e.v("f"), "count", e.tup())),
                                                e.s(" overloads of "),
                                                e.fld(e.v("st"), "base"),
                                                e.s(" on "),
                                                e.callM(e.v("f"), "iface", e.tup()),
                                                e.s("; select one as "),
                                                e.fld(e.v("st"), "base"),
                                                e.s("#<index> (declaration order from 0)")))),
                        e.ifOnly(
                                e.not(e.callM(e.v("f"), "found", e.tup())),
                                sigErr(
                                        e,
                                        e.cat(
                                                e.s("no "),
                                                e.v("mkind"),
                                                e.s(" "),
                                                e.fld(e.v("st"), "member"),
                                                e.s(" on "),
                                                a(e, "iface")))),
                        e.mb.set(e.fld(e.v("st"), "iface"), e.callM(e.v("f"), "iface", e.tup())),
                        e.let("m", e.callM(e.v("f"), "member", e.tup())),
                        e.ifOnly(
                                andB(e, e.eq(a(e, "kind"), e.s("set")), e.callM(e.v("m"), "readonly", e.tup())),
                                sigErr(e, e.cat(e.s("readonly attribute "), e.fld(e.v("st"), "member")))),
                        e.ifOnly(
                                e.not(orB(e, e.eq(a(e, "kind"), e.s("ctor")), e.callM(e.v("m"), "static_", e.tup()))),
                                e.mb.set(e.fld(e.v("st"), "self"), e.intOf(1))),
                        e.ifOnly(
                                orB(e, e.eq(a(e, "kind"), e.s("op")), e.eq(a(e, "kind"), e.s("ctor"))),
                                e.foreach(
                                        e.callM(e.v("m"), "args", e.tup()),
                                        "ar",
                                        e.seq(
                                                e.ifOnly(
                                                        e.callM(e.v("ar"), "variadic", e.tup()),
                                                        sigErr(
                                                                e,
                                                                e.cat(
                                                                        e.s("variadic argument "),
                                                                        e.callM(e.v("ar"), "name", e.tup()),
                                                                        e.s(" is not supported")))),
                                                push(
                                                        e,
                                                        e.v("args"),
                                                        hf(
                                                                e,
                                                                "h_param",
                                                                e.rec(
                                                                        e.p("table", a(e, "table")),
                                                                        e.p("ty", e.callM(e.v("ar"), "ty", e.tup())),
                                                                        e.p(
                                                                                "optional",
                                                                                e.callM(
                                                                                        e.v("ar"),
                                                                                        "optional",
                                                                                        e.tup())))))))),
                        e.ifOnly(
                                e.eq(a(e, "kind"), e.s("set")),
                                push(
                                        e,
                                        e.v("args"),
                                        hf(
                                                e,
                                                "h_param",
                                                e.rec(
                                                        e.p("table", a(e, "table")),
                                                        e.p("ty", e.callM(e.v("m"), "ty", e.tup())),
                                                        e.p("optional", e.b(false)))))),
                        e.ifOnly(
                                e.eq(a(e, "kind"), e.s("ctor")),
                                e.mb.set(e.fld(e.v("st"), "ret_kind"), e.dstr("handle"))),
                        e.ifOnly(
                                orB(e, e.eq(a(e, "kind"), e.s("op")), e.eq(a(e, "kind"), e.s("get"))),
                                e.seq(
                                        e.let(
                                                "ar",
                                                hf(
                                                        e,
                                                        "h_abi_ret",
                                                        e.rec(
                                                                e.p("table", a(e, "table")),
                                                                e.p("ty", e.callM(e.v("m"), "ty", e.tup()))))),
                                        e.ifOnly(
                                                e.fld(e.v("ar"), "out"),
                                                e.mb.set(e.fld(e.v("st"), "ret_out"), e.intOf(1))),
                                        e.mb.set(e.fld(e.v("st"), "ret_kind"), e.fld(e.v("ar"), "kind")))),
                        e.ifOnly(
                                e.eq(e.fld(e.v("st"), "ret_kind"), e.s("buf")),
                                sigErr(e, e.cat(e.s("buffer-typed return of "), e.fld(e.v("st"), "base")))),
                        e.ret(sigOf(e))));
        fnImpl(
                e,
                x,
                "h_sig_name",
                body(
                        e,
                        e.ifOnly(
                                e.eq(a(e, "name"), e.s("global")),
                                retM(e, sigCall(e, e.dstr(""), e.dstr(""), "global"))),
                        e.ifOnly(
                                e.eq(a(e, "name"), e.s("callback")),
                                retM(e, sigCall(e, e.dstr(""), e.dstr(""), "callback"))),
                        e.let(
                                "st",
                                e.rec(
                                        e.p("dot", e.intOf(0)),
                                        e.p("a", e.listNew(e.mb.typesByte())),
                                        e.p("b", e.listNew(e.mb.typesByte())),
                                        e.p("nb", e.intOf(0)),
                                        e.p("last", e.intOf(0)))),
                        e.foreach(
                                e.call(e.mb.evalStrUtf8(), a(e, "name")),
                                "b",
                                e.seq(
                                        e.let("n", e.mb.as(e.v("b"), intT(e))),
                                        e.ifElse(
                                                andB(
                                                        e,
                                                        e.eq(e.fld(e.v("st"), "dot"), num(e, 0)),
                                                        byteIs(e, e.v("n"), '.')),
                                                e.mb.set(e.fld(e.v("st"), "dot"), e.intOf(1)),
                                                e.ifElse(
                                                        e.eq(e.fld(e.v("st"), "dot"), num(e, 0)),
                                                        push(e, e.fld(e.v("st"), "a"), e.v("b")),
                                                        e.seq(
                                                                push(e, e.fld(e.v("st"), "b"), e.v("b")),
                                                                e.incr(e.fld(e.v("st"), "nb")),
                                                                e.mb.set(e.fld(e.v("st"), "last"), e.v("n"))))))),
                        e.ifOnly(
                                e.eq(e.fld(e.v("st"), "dot"), num(e, 0)),
                                e.mb.panic(e.cat(e.s("webidl: malformed import name "), a(e, "name")))),
                        e.let("set", byteIs(e, e.fld(e.v("st"), "last"), '=')),
                        e.let("mb", e.listNew(e.mb.typesByte())),
                        e.let("i", e.rec(e.p("v", e.intOf(0)))),
                        e.foreach(
                                e.fld(e.v("st"), "b"),
                                "b",
                                e.seq(
                                        e.ifOnly(
                                                orB(
                                                        e,
                                                        e.not(e.v("set")),
                                                        e.mb.operatorLt(
                                                                e.fld(e.v("i"), "v"),
                                                                e.mb.operatorSub(e.fld(e.v("st"), "nb"), num(e, 1)))),
                                                push(e, e.v("mb"), e.v("b"))),
                                        e.incr(e.fld(e.v("i"), "v")))),
                        e.let("iface", utf8(e, e.fld(e.v("st"), "a"))),
                        e.let("member", utf8(e, e.v("mb"))),
                        e.ifOnly(e.v("set"), retM(e, sigCall(e, e.v("iface"), e.v("member"), "set"))),
                        e.ifOnly(
                                e.eq(e.v("member"), e.s("constructor")),
                                retM(e, sigCall(e, e.v("iface"), e.v("member"), "ctor"))),
                        e.let("op", sigCall(e, e.v("iface"), e.v("member"), "op")),
                        e.ifOnly(e.callM(e.v("op"), "ok", e.tup()), retM(e, e.v("op"))),
                        e.ret(sigCall(e, e.v("iface"), e.v("member"), "get"))));
        fnImpl(
                e,
                x,
                "h_mangle",
                body(
                        e,
                        e.ret(e.cat(
                                a(e, "iface"),
                                e.s("."),
                                a(e, "member"),
                                e.ifElse(e.eq(a(e, "kind"), e.s("set")), e.dstr("="), e.dstr(""))))));
        fnImpl(
                e,
                x,
                "h_table",
                body(
                        e,
                        e.let("defs", e.mapNew(str(e), lt(e, "idef"))),
                        e.let("typedefs", e.mapNew(str(e), lt(e, "itype"))),
                        e.let("kind", e.rec(e.p("v", e.dstr("")))),
                        e.foreach(
                                e.v("arg"),
                                "d0",
                                e.seq(
                                        e.mb.set(e.fld(e.v("kind"), "v"), e.callM(e.v("d0"), "kind", e.tup())),
                                        e.ifOnly(
                                                e.eq(e.fld(e.v("kind"), "v"), e.s("typedef")),
                                                e.callM(
                                                        e.v("typedefs"),
                                                        "put",
                                                        e.tup(
                                                                e.callM(e.v("d0"), "name", e.tup()),
                                                                e.callM(e.v("d0"), "ty", e.tup())))),
                                        e.ifOnly(
                                                andB(
                                                        e,
                                                        e.not(e.eq(e.fld(e.v("kind"), "v"), e.s("typedef"))),
                                                        andB(
                                                                e,
                                                                e.not(e.eq(e.fld(e.v("kind"), "v"), e.s("includes"))),
                                                                e.not(e.callM(e.v("d0"), "partial", e.tup())))),
                                                e.callM(
                                                        e.v("defs"),
                                                        "put",
                                                        e.tup(e.callM(e.v("d0"), "name", e.tup()), e.v("d0")))))),
                        e.foreach(
                                e.v("arg"),
                                "d1",
                                e.ifOnly(
                                        andB(
                                                e,
                                                e.callM(e.v("d1"), "partial", e.tup()),
                                                hf(
                                                        e,
                                                        "h_has",
                                                        e.rec(
                                                                e.p("defs", e.v("defs")),
                                                                e.p("name", e.callM(e.v("d1"), "name", e.tup()))))),
                                        e.foreach(
                                                e.callM(e.v("d1"), "members", e.tup()),
                                                "m",
                                                push(
                                                        e,
                                                        e.callM(
                                                                e.callM(
                                                                        e.v("defs"),
                                                                        "get",
                                                                        e.callM(e.v("d1"), "name", e.tup())),
                                                                "members",
                                                                e.tup()),
                                                        e.v("m"))))),
                        e.foreach(
                                e.v("arg"),
                                "d2",
                                e.ifOnly(
                                        andB(
                                                e,
                                                e.eq(e.callM(e.v("d2"), "kind", e.tup()), e.s("includes")),
                                                andB(
                                                        e,
                                                        hf(
                                                                e,
                                                                "h_has",
                                                                e.rec(
                                                                        e.p("defs", e.v("defs")),
                                                                        e.p(
                                                                                "name",
                                                                                e.callM(e.v("d2"), "name", e.tup())))),
                                                        hf(
                                                                e,
                                                                "h_has",
                                                                e.rec(
                                                                        e.p("defs", e.v("defs")),
                                                                        e.p(
                                                                                "name",
                                                                                e.callM(
                                                                                        e.v("d2"), "parent",
                                                                                        e.tup())))))),
                                        e.foreach(
                                                e.callM(
                                                        e.callM(
                                                                e.v("defs"),
                                                                "get",
                                                                e.callM(e.v("d2"), "parent", e.tup())),
                                                        "members",
                                                        e.tup()),
                                                "m",
                                                push(
                                                        e,
                                                        e.callM(
                                                                e.callM(
                                                                        e.v("defs"),
                                                                        "get",
                                                                        e.callM(e.v("d2"), "name", e.tup())),
                                                                "members",
                                                                e.tup()),
                                                        e.v("m"))))),
                        e.ret(mk(e, "itable", e.p("defs", e.v("defs")), e.p("typedefs", e.v("typedefs"))))));

        x.add(e.finish(e.v("d")));
        var defs = new TList<EcoMb.P>();
        defs.add(e.p("parse", e.v("h_parse")));
        defs.add(e.p("table", e.v("h_table")));
        for (var fn :
                new String[] {"has", "resolve", "kind", "abi_ret", "find", "mangle", "sig", "sig_name", "const_value"
                }) {
            defs.add(e.p(fn, e.v("h_" + fn)));
        }
        for (var obj : new String[] {"itype", "iarg", "imember", "idef", "itable", "ifound", "iparam", "isig"}) {
            defs.add(e.p(obj + "_t", t(e, obj)));
        }
        x.add(e.ret(e.rec(defs.toArray(EcoMb.P[]::new))));

        var y = new TList<SourceExpr>();
        y.add(e.let("k", e.lower(e.mb.seq(x))));
        var texts = new TList<SourceExpr>();
        for (var file : SPEC_FILES) {
            texts.add(e.seq(
                    e.let("f", e.fld(e.importGit("w", SPEC_REPO, SPEC_COMMIT), "ed", "idl", file)),
                    e.ret(utf8(
                            e,
                            e.mb.as(
                                    e.callM(
                                            e.v("f"),
                                            "read",
                                            e.rec(e.p("offset", e.intOf(0)), e.p("len", e.fld(e.v("f"), "len")))),
                                    e.listType(e.mb.typesByte()))))));
            texts.add(e.s("\n"));
        }
        y.add(e.let("text", e.cat(texts.toArray(SourceExpr[]::new))));
        y.add(e.let("spec_defs", e.call(e.fld(e.v("k"), "parse"), e.v("text"))));
        y.add(e.let("spec", e.call(e.fld(e.v("k"), "table"), e.v("spec_defs"))));
        var names = new String[] {
            "parse",
            "table",
            "has",
            "resolve",
            "kind",
            "abi_ret",
            "find",
            "mangle",
            "sig",
            "sig_name",
            "const_value",
            "itype_t",
            "iarg_t",
            "imember_t",
            "idef_t",
            "itable_t",
            "ifound_t",
            "iparam_t",
            "isig_t"
        };
        var ydefs = new TList<EcoMb.P>();
        for (var name : names) ydefs.add(e.p(name, e.fld(e.v("k"), name)));
        ydefs.add(e.p("spec_defs", e.v("spec_defs")));
        ydefs.add(e.p("spec", e.v("spec")));
        y.add(e.ret(e.rec(ydefs.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("s", e.lower(e.mb.seq(y))));
        var exports = new TList<EcoMb.P>();
        for (var name : names) exports.add(e.p(name, e.fld(e.v("s"), name)));
        exports.add(e.p("spec_defs", e.fld(e.v("s"), "spec_defs")));
        exports.add(e.p("spec", e.fld(e.v("s"), "spec")));
        top.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return top;
    }
}
