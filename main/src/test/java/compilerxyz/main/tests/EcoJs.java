package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.*;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoJs {
    private EcoJs() {}

    private static final String TARGET = "js";

    private static final IList<String> JGFN_KINDS = IList.of("global_fn", "func_fn", "new_fn", "var_fn");

    private static SourceExpr fnDef(EcoMb e, TList<SourceExpr> out, String name, SourceExpr argType, SourceExpr ret) {
        return EcoTargetMb.fnDef(e, out, TARGET, name, argType, ret);
    }

    private static SourceExpr jmodT(EcoMb e) {
        return t(e, "jmod");
    }

    private static SourceExpr renderT(EcoMb e) {
        return e.rec(e.p("js", str(e)));
    }

    private static SourceExpr instsT(EcoMb e) {
        return e.listType(t(e, "jinst"));
    }

    private static SourceExpr instsNew(EcoMb e) {
        return e.listNew(lt(e, "jinst"));
    }

    private static SourceExpr inst(EcoMb e, SourceExpr text) {
        return mk(e, "jinst", e.p("text", text));
    }

    private static SourceExpr janyT(EcoMb e) {
        return mkArg(e, "jany", e.tup());
    }

    private static SourceExpr jvalNew(EcoMb e, SourceExpr expr) {
        return mk(e, "jval", e.p("ty", janyT(e)), e.p("expr", expr));
    }

    private static SourceExpr jvExpr(EcoMb e, SourceExpr cveTP) {
        return e.callM(reinterpret(e, cveValue(e, cveTP), "jval"), "expr", e.tup());
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
                        e.call(e.mb.reinterpret(), e.tup(loc(e), e.fld(xx(e), "target"), lt(e, "jtarget"))),
                        retInner),
                "mod",
                e.tup());
    }

    private static SourceExpr operand(EcoMb e, SourceExpr xx, SourceExpr loc, SourceExpr cv) {
        return hf(e, "h_operand", e.p("xx", xx), e.p("loc", loc), e.p("cv", cv));
    }

    private static SourceExpr isJs(EcoMb e, SourceExpr xx, SourceExpr cv) {
        return hf(e, "h_is_js", e.p("xx", xx), e.p("cv", cv));
    }

    private static SourceExpr constStr(EcoMb e, SourceExpr cv) {
        return hf(e, "h_const_str", e.p("xx", xx(e)), e.p("cv", cv));
    }

    private static SourceExpr argsT(EcoMb e) {
        return e.rec(e.p("code", codeT(e)), e.p("exprs", e.listType(str(e))));
    }

    private static SourceExpr argsOf(EcoMb e) {
        return unwrapM(
                e,
                tryEr(e, hf(e, "h_args", e.p("xx", xx(e)), e.p("loc", loc(e)), e.p("cv", a(e, "argument"))), argsT(e)),
                cveT(e));
    }

    private static int jgfnKind(String kind) {
        for (int i = 0; i < JGFN_KINDS.size(); i++) {
            if (JGFN_KINDS.get(i).equals(kind)) return i;
        }
        throw new IllegalArgumentException("no js definition kind " + kind);
    }

    private static SourceExpr jgfnIs(EcoMb e, String kind) {
        return e.eq(in(e, "kind"), num(e, jgfnKind(kind)));
    }

    private static SourceExpr jgfnStaged(EcoMb e, SourceExpr staged) {
        return unwrapCveM(e, constEvalIn(e, xx(e), e.stage(staged)));
    }

    private static SourceExpr jgfnField(EcoMb e, String key) {
        return unwrapCveM(e, e.callM(e.v("fields"), "get", e.s(key)));
    }

    private static SourceExpr pushUtf8(EcoMb e, SourceExpr out, SourceExpr text) {
        return e.foreach(e.call(e.mb.evalStrUtf8(), text), "__b", push(e, out, e.v("__b")));
    }

    private static SourceExpr lines(EcoMb e, SourceExpr insts, SourceExpr out, String indent) {
        return e.foreach(insts, "i0", push(e, out, e.cat(e.s(indent), e.callM(e.v("i0"), "text", e.tup()))));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var x = new TList<SourceExpr>();
        var entries = new TList<EcoMb.P>();
        for (int b = 0; b < 256; b++) {
            String text;
            if (b == '"') text = "\\\"";
            else if (b == '\\') text = "\\\\";
            else if (b == '\n') text = "\\n";
            else if (b == '\r') text = "\\r";
            else if (b == '\t') text = "\\t";
            else if (b < 32 || b == 127) text = String.format("\\u%04x", b);
            else text = "";
            entries.add(e.p(Integer.toString(b), e.s(text)));
        }
        var escapeTable = e.mapNew(str(e), str(e), entries.toArray(EcoMb.P[]::new));
        x.add(e.let("d", e.define()));
        x.add(e.optionTypeLet());
        defIface(e, x, "jany_m");
        defIface(e, x, "jglobal_m");
        defIface(e, x, "jnew_m");
        defIface(e, x, "jgfn_m", e.p("kind", intT(e)));
        defIface(e, x, "jinst_m");
        defIface(e, x, "jmod_m");
        defIface(e, x, "jbinding_m");
        defObj(e, x, "jinst", "js statement", e.rec(e.p("text", str(e))));
        defObj(e, x, "jcode", "js code", e.rec(e.p("insts", instsT(e))));
        x.add(e.implInterface(e.v("jcode"), e.mb.builtin("eval", "code_dyn")));
        method(e, x, "jcode", "insts", e.tup(), instsT(e), in(e, "insts"));
        defObj(e, x, "jval", "js value", e.rec(e.p("ty", ty(e)), e.p("expr", str(e))));
        defObj(
                e,
                x,
                "jmod",
                "js module",
                e.rec(
                        e.p("n", intT(e)),
                        e.p("xx", e.call(e.mb.typesOption(), e.typesExprContext())),
                        e.p("funcs", e.listType(str(e))),
                        e.p("stmts", e.listType(str(e)))));
        defObj(e, x, "jtarget", "js target", e.rec(e.p("mod", jmodT(e))));
        defObj(e, x, "jbinding", "js binding", e.rec(e.p("name", str(e))));
        defObj(e, x, "jany", "js value type", e.tup());
        defObj(e, x, "jglobal", "js global type", e.tup());
        defObj(e, x, "jnew", "js new type", e.tup());
        defObj(e, x, "jgfn_t", "js definition function", e.rec(e.p("kind", intT(e))));

        {
            fnDef(e, x, "h_insts", codeT(e), instsT(e));
            fnDef(e, x, "h_code", instsT(e), codeT(e));
            x.add(e.let("h_append_b", e.defFunc(e.v("d"), "js.h_append")));
            x.add(e.let("h_append", e.fld(e.v("h_append_b"), "func")));
            e.fnSigs.put("h_append", new SourceExpr[] {
                e.rec(e.p("out", instsT(e)), e.p("code", codeT(e))), e.optionTypeNone(), e.optionTypeNone()
            });
            fnDef(e, x, "h_is_js", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), e.mb.typesBool());
            fnDef(e, x, "h_operand", e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("cv", cveT(e))), cveT(e));
            fnThrows(e, "h_operand", errsT(e));
            fnDef(e, x, "h_args", e.rec(e.p("xx", xxT(e)), e.p("loc", locT(e)), e.p("cv", cveT(e))), argsT(e));
            fnThrows(e, "h_args", errsT(e));
            fnDef(e, x, "h_const_str", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), str(e));
            fnDef(e, x, "h_key_str", e.rec(e.p("xx", xxT(e)), e.p("cv", cveT(e))), str(e));
            fnDef(e, x, "h_gval", cveT(e), cveT(e));
            fnDef(e, x, "h_str_lit", str(e), str(e));

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
                                                    e.callM(e.v("c"), "cast", lt(e, "jcode")),
                                                    "r",
                                                    Opt.none(),
                                                    e.p("ok", e.callM(e.v("r"), "insts", e.tup())),
                                                    e.p(
                                                            "errs",
                                                            e.mb.panic(
                                                                    e.s("js: code belongs to another target")))))))));
            fnImpl(
                    e,
                    x,
                    "h_code",
                    body(e, e.ret(e.mb.as(mkArg(e, "jcode", e.rec(e.p("insts", e.v("arg")))), codeT(e)))));
            fnImpl(
                    e,
                    x,
                    "h_append",
                    body(e, e.foreach(hf(e, "h_insts", a(e, "code")), "i", push(e, a(e, "out"), e.v("i")))));
            fnImpl(
                    e,
                    x,
                    "h_is_js",
                    body(
                            e,
                            e.ret(e.ifElse(
                                    cveHas(e, a(e, "cv")),
                                    has(e, xx(e), cveType(e, a(e, "cv")), "jany_m"),
                                    e.b(false)))));
            fnImpl(
                    e,
                    x,
                    "h_operand",
                    body(
                            e,
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "operand has no value")),
                            e.ret(unwrapCveT(e, castValue(e, xx(e), loc(e), janyT(e), a(e, "cv"))))));
            fnImpl(
                    e,
                    x,
                    "h_args",
                    body(
                            e,
                            e.let("out", instsNew(e)),
                            e.let("exprs", e.listNew(str(e))),
                            e.ifOnly(e.not(cveHas(e, a(e, "cv"))), throwErr(e, loc(e), "call argument has no value")),
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
                                            e.seq(
                                                    e.let("av", operand(e, xx(e), loc(e), unwrapCveT(e, e.v("er")))),
                                                    appendCode(e, e.v("out"), cveCode(e, e.v("av"))),
                                                    push(e, e.v("exprs"), jvExpr(e, e.v("av"))))),
                                    e.seq(
                                            e.let("av", operand(e, xx(e), loc(e), a(e, "cv"))),
                                            appendCode(e, e.v("out"), cveCode(e, e.v("av"))),
                                            push(e, e.v("exprs"), jvExpr(e, e.v("av"))))),
                            e.ret(e.rec(e.p("code", codeOf(e, e.v("out"))), e.p("exprs", e.v("exprs"))))));
            fnImpl(
                    e,
                    x,
                    "h_const_str",
                    body(
                            e,
                            e.let("sk", lookup(e, xx(e), cveType(e, a(e, "cv")), e.mb.evalAspectStringConst())),
                            e.ifOnly(
                                    e.not(e.fld(e.v("sk"), "is_some")),
                                    e.mb.panic(e.s("js: expected a constant string"))),
                            e.ret(bim(
                                    e,
                                    e.mb.evalAspectStringConst(),
                                    "get",
                                    e.fld(e.v("sk"), "value"),
                                    cveValue(e, a(e, "cv"))))));
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
                    "h_gval",
                    body(
                            e,
                            e.ret(cvOf(
                                    e,
                                    cveCode(e, e.v("arg")),
                                    jvalNew(
                                            e,
                                            e.call(
                                                    e.mb.mustReinterpret(),
                                                    e.tup(
                                                            e.fld(
                                                                    e.call(
                                                                            e.mb.mustReinterpret(),
                                                                            e.tup(cveValue(e, e.v("arg")), vocT(e))),
                                                                    "data"),
                                                            str(e))))))));
            fnImpl(
                    e,
                    x,
                    "h_str_lit",
                    body(
                            e,
                            e.let("table", escapeTable),
                            e.let("out", e.listNew(e.mb.typesByte())),
                            pushUtf8(e, e.v("out"), e.s("\"")),
                            e.foreach(
                                    e.call(e.mb.evalStrUtf8(), e.v("arg")),
                                    "b",
                                    e.seq(
                                            e.let(
                                                    "esc",
                                                    e.callM(e.v("table"), "get", toStr(e, e.mb.as(e.v("b"), intT(e))))),
                                            e.ifElse(
                                                    e.eq(e.v("esc"), e.s("")),
                                                    push(e, e.v("out"), e.v("b")),
                                                    pushUtf8(e, e.v("out"), e.v("esc"))))),
                            pushUtf8(e, e.v("out"), e.s("\"")),
                            e.ret(e.matchOf(
                                    e.call(e.evalBuiltin("str_from_utf8"), e.v("out")),
                                    "__s",
                                    Opt.none(),
                                    e.p("some", e.v("__s")),
                                    e.p("none", e.mb.panic(e.s("js: string literal is not valid UTF-8")))))));
        }

        accessor(e, x, "jinst", "text", str(e));
        accessor(e, x, "jval", "expr", str(e));
        accessor(e, x, "jval", "ty", ty(e));
        x.add(e.implInterface(e.v("jval"), e.mb.evalValue(), e.p("type", in(e, "ty"))));
        accessor(e, x, "jtarget", "mod", jmodT(e));
        accessor(e, x, "jbinding", "name", str(e));
        for (var vo : new String[] {"jinst", "jmod"}) {
            markerType(e, x, vo, vo, vo + "_m", e.aspectDispatch(lv(e, vo + "_m_id")));
            x.add(e.implInterface(e.v(vo), t(e, vo + "_m")));
        }
        x.add(e.implInterface(
                e.v("jbinding"),
                e.mb.evalBinding(),
                e.p("use", cvOf(e, a(e, "code"), jvalNew(e, in(e, "name")))),
                e.p("drop", e.eresultOk(e.mb.typesCode(), codeNone(e, loc(e)))),
                e.p("fork", outer(e)),
                e.p("merge", e.eresultOk(e.mb.evalBinding(), outer(e)))));

        {
            method(e, x, "jmod", "xx", e.tup(), e.typesExprContext(), in(e, "xx", "value"));
            x.add(e.methodImpl(
                    e.v("jmod"),
                    "set_xx",
                    e.typesExprContext(),
                    e.optionTypeNone(),
                    e.stage(e.mb.set(in(e, "xx"), e.optSome(e.typesExprContext(), e.v("arg"))))));
            method(
                    e,
                    x,
                    "jmod",
                    "fresh",
                    e.tup(),
                    str(e),
                    e.seq(e.let("n", in(e, "n")), e.incr(in(e, "n")), e.ret(e.cat(e.s("$"), toStr(e, e.v("n"))))));
            {
                method(
                        e,
                        x,
                        "jmod",
                        "lower",
                        e.rec(e.p("params", e.listType(str(e))), e.p("body", e.mb.typesExpr())),
                        cveT(e),
                        e.seq(
                                e.let("entries", e.mapNew(e.mb.typesConstkey(), e.mb.evalBinding())),
                                e.foreach(
                                        a(e, "params"),
                                        "pn",
                                        e.callM(
                                                e.v("entries"),
                                                "put",
                                                e.tup(e.v("pn"), mk(e, "jbinding", e.p("name", e.v("pn")))))),
                                e.let("vx", e.call(e.evalBuiltin("eval_context"), e.tup())),
                                e.let(
                                        "sc",
                                        e.call(
                                                e.evalBuiltin("scope"),
                                                e.rec(e.p("vx", e.v("vx")), e.p("entries", e.v("entries"))))),
                                e.let("tgt", mk(e, "jtarget", e.p("mod", outer(e)))),
                                e.let(
                                        "ctx",
                                        e.call(
                                                e.evalBuiltin("expr_context"),
                                                e.rec(
                                                        e.p("scope", e.v("sc")),
                                                        e.p("target", e.mb.as(e.v("tgt"), e.mb.evalTarget())),
                                                        e.p("vx", e.v("vx")),
                                                        e.p("exception_type", str(e))))),
                                e.ret(EcoTargetMb.mustV(
                                        e,
                                        TARGET,
                                        e.call(
                                                e.mb.evalConstEval(),
                                                e.rec(e.p("xx", e.v("ctx")), e.p("staged", a(e, "body")))),
                                        "compile failed"))));
            }
            method(
                    e,
                    x,
                    "jmod",
                    "func",
                    e.rec(e.p("name", str(e)), e.p("params", e.listType(str(e))), e.p("body", e.mb.typesExpr())),
                    str(e),
                    e.seq(
                            e.let(
                                    "r",
                                    e.callM(
                                            outer(e),
                                            "lower",
                                            e.rec(e.p("params", a(e, "params")), e.p("body", a(e, "body"))))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("r"))),
                            e.ifOnly(
                                    andB(e, e.not(cveEnded(e, e.v("r"))), isJs(e, in(e, "xx", "value"), e.v("r"))),
                                    push(e, e.v("out"), inst(e, e.cat(e.s("return "), jvExpr(e, e.v("r")), e.s(";"))))),
                            e.let("all", e.listNew(str(e))),
                            push(
                                    e,
                                    e.v("all"),
                                    e.cat(
                                            e.s("function "),
                                            a(e, "name"),
                                            e.s("("),
                                            e.join(a(e, "params"), ", "),
                                            e.s(") {"))),
                            lines(e, e.v("out"), e.v("all"), "  "),
                            push(e, e.v("all"), e.s("}")),
                            push(e, in(e, "funcs"), e.join(e.v("all"), "\n")),
                            e.ret(a(e, "name"))));
            x.add(e.methodImpl(
                    e.v("jmod"),
                    "func_text",
                    e.rec(e.p("name", str(e)), e.p("params", e.listType(str(e))), e.p("lines", e.listType(str(e)))),
                    e.optionTypeNone(),
                    e.stage(e.seq(
                            e.let("all", e.listNew(str(e))),
                            push(
                                    e,
                                    e.v("all"),
                                    e.cat(
                                            e.s("function "),
                                            a(e, "name"),
                                            e.s("("),
                                            e.join(a(e, "params"), ", "),
                                            e.s(") {"))),
                            e.foreach(a(e, "lines"), "l", push(e, e.v("all"), e.cat(e.s("  "), e.v("l")))),
                            push(e, e.v("all"), e.s("}")),
                            push(e, in(e, "funcs"), e.join(e.v("all"), "\n"))))));
            method(
                    e,
                    x,
                    "jmod",
                    "declare",
                    str(e),
                    str(e),
                    e.seq(push(e, in(e, "stmts"), e.cat(e.s("let "), e.v("arg"), e.s(";"))), e.ret(e.v("arg"))));
            x.add(e.methodImpl(
                    e.v("jmod"),
                    "assign_index",
                    e.rec(e.p("target", str(e)), e.p("key", str(e)), e.p("value", str(e))),
                    e.optionTypeNone(),
                    e.stage(push(
                            e,
                            in(e, "stmts"),
                            e.cat(
                                    a(e, "target"),
                                    e.s("["),
                                    hf(e, "h_str_lit", a(e, "key")),
                                    e.s("] = "),
                                    a(e, "value"),
                                    e.s(";"))))));
            x.add(e.methodImpl(
                    e.v("jmod"),
                    "run",
                    e.mb.typesExpr(),
                    e.optionTypeNone(),
                    e.stage(e.seq(
                            e.let(
                                    "r",
                                    e.callM(
                                            outer(e),
                                            "lower",
                                            e.rec(e.p("params", e.listNew(str(e))), e.p("body", e.v("arg"))))),
                            e.let("out", instsNew(e)),
                            appendCode(e, e.v("out"), cveCode(e, e.v("r"))),
                            lines(e, e.v("out"), in(e, "stmts"), "")))));
            method(
                    e,
                    x,
                    "jmod",
                    "render",
                    e.tup(),
                    renderT(e),
                    e.seq(
                            e.let("all", e.listNew(str(e))),
                            e.foreach(in(e, "funcs"), "f", push(e, e.v("all"), e.v("f"))),
                            e.foreach(in(e, "stmts"), "s", push(e, e.v("all"), e.v("s"))),
                            e.ret(e.rec(e.p("js", e.cat(e.join(e.v("all"), "\n"), e.s("\n")))))));
        }
        {
            x.add(e.implInterface(
                    e.v("jtarget"),
                    e.mb.evalTarget(),
                    e.p("cast_exception", e.mb.panic(e.s("js target: exceptions are not supported"))),
                    e.p(
                            "initial_bind",
                            e.eresultErr(
                                    e.mb.evalBinding(),
                                    loc(e),
                                    e.s("js target: match and catch payloads are not supported"))),
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
                    e.p("jump", e.mb.panic(e.s("js target: jumps are not supported"))),
                    e.p("land", e.eresultOk(cveT(e), cvNo(e, codeNone(e, loc(e))))),
                    e.p("value_is_runtime", e.mb.bool(false)),
                    structuralExits(e)[0],
                    structuralExits(e)[1],
                    e.p(
                            "exit",
                            body(
                                    e,
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), a(e, "value", "code")),
                                    e.ifOnly(
                                            isJs(e, xx(e), a(e, "value")),
                                            e.seq(
                                                    push(
                                                            e,
                                                            e.v("out"),
                                                            inst(
                                                                    e,
                                                                    e.cat(
                                                                            e.s("return "),
                                                                            jvExpr(e, a(e, "value")),
                                                                            e.s(";")))),
                                                    retM(e, e.eresultOk(codeT(e), codeOf(e, e.v("out")))))),
                                    push(e, e.v("out"), inst(e, e.s("return;"))),
                                    e.ret(e.eresultOk(codeT(e), codeOf(e, e.v("out")))))),
                    e.p("guarded", e.mb.panic(e.s("js target: exceptions are not supported"))),
                    e.p("exc_cleanup", e.mb.panic(e.s("js target: exceptions are not supported"))),
                    e.p("exit_trampoline", e.mb.panic(e.s("js target: cleanup blocks are not supported"))),
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
                                                            isJs(e, xx(e), a(e, "cv")),
                                                            e.cat(e.s("return "), jvExpr(e, a(e, "cv")), e.s(";")),
                                                            e.dstr("return;")))),
                                    e.ret(e.call(e.evalBuiltin("codevalue_control_ended"), codeOf(e, e.v("out")))))),
                    e.p("stage", errCve(e, loc(e), "js target: nested staging is not supported")),
                    e.p("if", errCve(e, loc(e), "js target: if is not supported")),
                    e.p("try_catch_finally", errCve(e, loc(e), "js target: try is not supported")),
                    e.p("match", errCve(e, loc(e), "js target: match is not supported"))));
        }
        {
            selfType(
                    e,
                    x,
                    "jany",
                    e.s("js"),
                    e.aspectDispatch(
                            lv(e, "jany_m_id"),
                            aspId(e, "function"),
                            aspId(e, "field"),
                            aspId(e, "bind"),
                            aspId(e, "set")),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), srcType(e)),
                                    orB(
                                            e,
                                            has(e, xx(e), srcType(e), "jglobal_m"),
                                            orB(
                                                    e,
                                                    hasB(e, xx(e), srcType(e), aspIface(e, "record")),
                                                    orB(
                                                            e,
                                                            hasB(e, xx(e), srcType(e), e.mb.evalAspectStringConst()),
                                                            orB(
                                                                    e,
                                                                    isConst(e, xx(e), srcType(e)),
                                                                    hasB(
                                                                            e,
                                                                            xx(e),
                                                                            srcType(e),
                                                                            e.evalBuiltin("aspect_bool_const"))))))))),
                    body(
                            e,
                            e.let("st", cveType(e, a(e, "cv"))),
                            e.ifOnly(isEqual(e, xx(e), outer(e), e.v("st")), retM(e, okCve(e, a(e, "cv")))),
                            e.ifOnly(
                                    has(e, xx(e), e.v("st"), "jglobal_m"),
                                    retM(e, okCve(e, hf(e, "h_gval", a(e, "cv"))))),
                            e.ifOnly(
                                    hasB(e, xx(e), e.v("st"), e.mb.evalAspectStringConst()),
                                    retM(
                                            e,
                                            okCve(
                                                    e,
                                                    cvOf(
                                                            e,
                                                            codeNone(e, loc(e)),
                                                            jvalNew(e, hf(e, "h_str_lit", constStr(e, a(e, "cv")))))))),
                            e.ifOnly(
                                    hasB(e, xx(e), e.v("st"), e.evalBuiltin("aspect_bool_const")),
                                    retM(
                                            e,
                                            okCve(
                                                    e,
                                                    cvOf(
                                                            e,
                                                            codeNone(e, loc(e)),
                                                            jvalNew(
                                                                    e,
                                                                    e.ifElse(
                                                                            bim(
                                                                                    e,
                                                                                    e.evalBuiltin("aspect_bool_const"),
                                                                                    "get",
                                                                                    lookupV(
                                                                                            e,
                                                                                            xx(e),
                                                                                            e.v("st"),
                                                                                            e.evalBuiltin(
                                                                                                    "aspect_bool_const")),
                                                                                    cveValue(e, a(e, "cv"))),
                                                                            e.dstr("true"),
                                                                            e.dstr("false"))))))),
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
                                            retM(
                                                    e,
                                                    okCve(
                                                            e,
                                                            cvOf(
                                                                    e,
                                                                    codeNone(e, loc(e)),
                                                                    jvalNew(
                                                                            e,
                                                                            e.ifElse(
                                                                                    e.eq(
                                                                                            e.mb.as(
                                                                                                    e.v("l"),
                                                                                                    e.typesDouble()),
                                                                                            e.v("d")),
                                                                                    toStr(e, e.v("l")),
                                                                                    toStr(e, e.v("d"))))))))),
                            e.let("rk", lookup(e, xx(e), e.v("st"), aspIface(e, "record"))),
                            e.ifOnly(
                                    e.not(e.fld(e.v("rk"), "is_some")),
                                    retM(e, errCve(e, loc(e), "js: value cannot be converted to a js value"))),
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
                            e.let("out", instsNew(e)),
                            e.let("parts", e.listNew(str(e))),
                            e.foreach(
                                    bim0(e, aspIface(e, "record"), "field_types", e.fld(e.v("rk"), "value")),
                                    "en",
                                    e.seq(
                                            e.let("ks", keyStr(e, e.fld(e.v("en"), "key"))),
                                            e.let("fcv", unwrapCveM(e, e.callM(e.v("fields"), "get", e.v("ks")))),
                                            e.let(
                                                    "fv",
                                                    unwrapCveM(
                                                            e,
                                                            tryEr(e, operand(e, xx(e), loc(e), e.v("fcv")), cveT(e)))),
                                            appendCode(e, e.v("out"), cveCode(e, e.v("fv"))),
                                            push(e, e.v("parts"), e.cat(e.v("ks"), e.s(": "), jvExpr(e, e.v("fv")))))),
                            e.ret(okCve(
                                    e,
                                    cvOf(
                                            e,
                                            codeOf(e, e.v("out")),
                                            jvalNew(e, e.cat(e.s("{"), e.join(e.v("parts"), ", "), e.s("}"))))))),
                    body(e, e.ret(isEqual(e, xx(e), outer(e), a(e, "dest_type")))),
                    castFromSelf(e),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "jany_m"))));
            x.add(e.implInterface(e.v("jany"), t(e, "jany_m")));
            x.add(e.implInterface(
                    e.v("jany"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let("out", instsNew(e)),
                                    e.let(
                                            "f",
                                            unwrapCveM(e, tryEr(e, operand(e, xx(e), loc(e), a(e, "self")), cveT(e)))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("f"))),
                                    e.let("args", argsOf(e)),
                                    appendCode(e, e.v("out"), e.fld(e.v("args"), "code")),
                                    e.let("r", fresh(e, xmod(e))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    e.cat(
                                                            e.s("const "),
                                                            e.v("r"),
                                                            e.s(" = "),
                                                            jvExpr(e, e.v("f")),
                                                            e.s("("),
                                                            e.join(e.fld(e.v("args"), "exprs"), ", "),
                                                            e.s(");")))),
                                    e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), jvalNew(e, e.v("r"))))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));
            x.add(e.implInterface(
                    e.v("jany"),
                    aspIface(e, "field"),
                    e.p(
                            "field",
                            body(
                                    e,
                                    e.let(
                                            "s",
                                            unwrapCveM(e, tryEr(e, operand(e, xx(e), loc(e), a(e, "self")), cveT(e)))),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("s"))),
                                    e.ifOnly(
                                            hasB(e, xx(e), cveType(e, a(e, "key")), e.mb.evalAspectStringConst()),
                                            retM(
                                                    e,
                                                    okCve(
                                                            e,
                                                            cvOf(
                                                                    e,
                                                                    codeOf(e, e.v("out")),
                                                                    jvalNew(
                                                                            e,
                                                                            e.cat(
                                                                                    jvExpr(e, e.v("s")),
                                                                                    e.s("."),
                                                                                    hf(
                                                                                            e,
                                                                                            "h_key_str",
                                                                                            e.p("xx", xx(e)),
                                                                                            e.p(
                                                                                                    "cv",
                                                                                                    a(e, "key"))))))))),
                                    e.let(
                                            "k",
                                            unwrapCveM(e, tryEr(e, operand(e, xx(e), loc(e), a(e, "key")), cveT(e)))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("k"))),
                                    e.ret(okCve(
                                            e,
                                            cvOf(
                                                    e,
                                                    codeOf(e, e.v("out")),
                                                    jvalNew(
                                                            e,
                                                            e.cat(
                                                                    jvExpr(e, e.v("s")),
                                                                    e.s("["),
                                                                    jvExpr(e, e.v("k")),
                                                                    e.s("]")))))))),
                    e.p("type_opt", e.optionTypeLoweredNone()),
                    e.p("keys", e.listNew(e.mb.typesConstkey()))));
            x.add(e.implInterface(
                    e.v("jany"),
                    e.mb.evalAspectSet(),
                    e.p(
                            "set",
                            body(
                                    e,
                                    e.let(
                                            "t",
                                            unwrapCveM(e, tryEr(e, operand(e, xx(e), loc(e), a(e, "self")), cveT(e)))),
                                    e.let(
                                            "v",
                                            unwrapCveM(e, tryEr(e, operand(e, xx(e), loc(e), a(e, "value")), cveT(e)))),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("t"))),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    e.cat(
                                                            jvExpr(e, e.v("t")),
                                                            e.s(" = "),
                                                            jvExpr(e, e.v("v")),
                                                            e.s(";")))),
                                    e.ret(okCve(e, cvNo(e, codeOf(e, e.v("out")))))))));
            var brT = e.mb.typesBindResult();
            x.add(e.implInterface(
                    e.v("jany"),
                    e.mb.evalAspectBind(),
                    e.p(
                            "bind",
                            body(
                                    e,
                                    e.let(
                                            "v",
                                            unwrapM(e, tryEr(e, operand(e, xx(e), loc(e), a(e, "cv")), cveT(e)), brT)),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), cveCode(e, e.v("v"))),
                                    e.let("r", fresh(e, xmod(e, brT))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    e.cat(
                                                            e.s("const "),
                                                            e.v("r"),
                                                            e.s(" = "),
                                                            jvExpr(e, e.v("v")),
                                                            e.s(";")))),
                                    e.ret(e.eresultOk(
                                            brT,
                                            e.call(
                                                    e.mb.evalBindResult(),
                                                    e.rec(
                                                            e.p("code", codeOf(e, e.v("out"))),
                                                            e.p(
                                                                    "binding",
                                                                    mk(e, "jbinding", e.p("name", e.v("r"))))))))))));
        }
        {
            selfType(
                    e,
                    x,
                    "jglobal",
                    e.s("js global"),
                    rtDispatch(
                            e,
                            e.mb.typesString(),
                            e.aspectDispatch(
                                    lv(e, "jglobal_m_id"), aspId(e, "function"), aspId(e, "field"), aspId(e, "set"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    body(
                            e,
                            e.ret(orB(
                                    e,
                                    isEqual(e, xx(e), outer(e), a(e, "dest_type")),
                                    isEqual(e, xx(e), janyT(e), a(e, "dest_type"))))),
                    body(
                            e,
                            e.ifOnly(isEqual(e, xx(e), outer(e), a(e, "dest_type")), retM(e, okCve(e, a(e, "cv")))),
                            e.ret(castValue(e, xx(e), loc(e), a(e, "dest_type"), hf(e, "h_gval", a(e, "cv"))))),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "jglobal_m"))));
            x.add(e.implInterface(e.v("jglobal"), t(e, "jglobal_m")));
            rtLiftImpl(e, x, "jglobal");
            rtBindImpl(e, x, "jglobal", e.mb.typesString());
            rtUseImpl(e, x, "jglobal");
            for (var asp : new String[] {"function", "field", "set"}) {
                var method = asp.equals("function") ? "call" : asp;
                var extra =
                        switch (asp) {
                            case "function" -> new EcoMb.P[] {e.p("argument", a(e, "argument"))};
                            case "field" -> new EcoMb.P[] {e.p("key", a(e, "key"))};
                            default -> new EcoMb.P[] {e.p("value", a(e, "value"))};
                        };
                var callArgs = new TList<EcoMb.P>();
                callArgs.add(e.p("xx", xx(e)));
                callArgs.add(e.p("loc", loc(e)));
                callArgs.add(e.p("self", hf(e, "h_gval", a(e, "self"))));
                for (var p : extra) callArgs.add(p);
                var ps = new TList<EcoMb.P>();
                ps.add(e.p(
                        method,
                        body(
                                e,
                                e.ret(bim(
                                        e,
                                        aspIface(e, asp),
                                        method,
                                        lookupV(e, xx(e), janyT(e), aspIface(e, asp)),
                                        e.rec(callArgs.toArray(EcoMb.P[]::new)))))));
                if (asp.equals("function")) {
                    ps.add(e.p("exception_type", e.optionTypeLoweredNone()));
                } else if (asp.equals("field")) {
                    ps.add(e.p("type_opt", e.optionTypeLoweredNone()));
                    ps.add(e.p("keys", e.listNew(e.mb.typesConstkey())));
                }
                var iface = asp.equals("set") ? e.mb.evalAspectSet() : aspIface(e, asp);
                x.add(e.implInterface(e.v("jglobal"), iface, ps.toArray(EcoMb.P[]::new)));
            }
        }
        {
            selfType(
                    e,
                    x,
                    "jnew",
                    e.s("js new"),
                    rtDispatch(e, e.mb.typesString(), e.aspectDispatch(lv(e, "jnew_m_id"), aspId(e, "function"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(e, e.ret(has(e, xx(e), a(e, "other"), "jnew_m"))));
            x.add(e.implInterface(e.v("jnew"), t(e, "jnew_m")));
            rtLiftImpl(e, x, "jnew");
            rtBindImpl(e, x, "jnew", e.mb.typesString());
            rtUseImpl(e, x, "jnew");
            x.add(e.implInterface(
                    e.v("jnew"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.let("args", argsOf(e)),
                                    countLet(e, "na"),
                                    countLoop(e, "na", e.fld(e.v("args"), "exprs")),
                                    e.ifOnly(
                                            e.eq(e.fld(e.v("na"), "v"), num(e, 0)),
                                            retM(
                                                    e,
                                                    errCve(
                                                            e,
                                                            loc(e),
                                                            "js new: the first argument is the constructor"))),
                                    e.let("out", instsNew(e)),
                                    appendCode(e, e.v("out"), e.fld(e.v("args"), "code")),
                                    e.let("rest", e.listNew(str(e))),
                                    e.let("ctor", e.rec(e.p("v", e.dstr("")))),
                                    e.foreach(
                                            e.fld(e.v("args"), "exprs"),
                                            "ax",
                                            e.ifElse(
                                                    e.eq(e.fld(e.v("ctor"), "v"), e.s("")),
                                                    e.mb.set(e.fld(e.v("ctor"), "v"), e.v("ax")),
                                                    push(e, e.v("rest"), e.v("ax")))),
                                    e.let("r", fresh(e, xmod(e))),
                                    push(
                                            e,
                                            e.v("out"),
                                            inst(
                                                    e,
                                                    e.cat(
                                                            e.s("const "),
                                                            e.v("r"),
                                                            e.s(" = new "),
                                                            e.fld(e.v("ctor"), "v"),
                                                            e.s("("),
                                                            e.join(e.v("rest"), ", "),
                                                            e.s(");")))),
                                    e.ret(okCve(e, cvOf(e, codeOf(e, e.v("out")), jvalNew(e, e.v("r"))))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));
        }
        {
            selfType(
                    e,
                    x,
                    "jgfn_t",
                    e.cat(e.s("js definition function "), toStr(e, in(e, "kind"))),
                    rtDispatch(e, lt(e, "jmod"), e.aspectDispatch(lv(e, "jgfn_m_id"), aspId(e, "function"))),
                    canCastFromSelf(e),
                    castFromSelf(e),
                    noCastTo(e),
                    castToPanic(e),
                    body(
                            e,
                            e.ifOnly(e.not(has(e, xx(e), a(e, "other"), "jgfn_m")), retM(e, dfalse(e))),
                            e.ret(e.eq(in(e, "kind"), acc(e, xx(e), a(e, "other"), "jgfn_m", "kind")))));
            x.add(e.implInterface(e.v("jgfn_t"), t(e, "jgfn_m"), e.p("kind", in(e, "kind"))));
            rtLiftImpl(e, x, "jgfn_t");
            rtBindImpl(e, x, "jgfn_t", lt(e, "jmod"));
            rtUseImpl(e, x, "jgfn_t");
            x.add(e.implInterface(
                    e.v("jgfn_t"),
                    aspIface(e, "function"),
                    e.p(
                            "call",
                            body(
                                    e,
                                    e.ifOnly(
                                            jgfnIs(e, "new_fn"),
                                            retM(e, tagValue(e, jgfnStaged(e, e.s("new")), mkArg(e, "jnew", e.tup())))),
                                    e.let("argv", a(e, "argument")),
                                    e.ifOnly(
                                            e.not(cveHas(e, e.v("argv"))),
                                            retM(e, errCve(e, loc(e), "js definition argument has no value"))),
                                    e.let("rk", lookup(e, xx(e), cveType(e, e.v("argv")), aspIface(e, "record"))),
                                    e.ifOnly(
                                            e.not(e.fld(e.v("rk"), "is_some")),
                                            retM(e, errCve(e, loc(e), "js definition argument must be a record"))),
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
                                    e.let("namecv", jgfnStaged(e, graft(e, jgfnField(e, "name")))),
                                    e.ifOnly(
                                            isRuntime(e, e.v("namecv")),
                                            retM(e, errCve(e, loc(e), "js definition name must be constant"))),
                                    e.let("name", constStr(e, e.v("namecv"))),
                                    e.ifOnly(
                                            jgfnIs(e, "global_fn"),
                                            retM(e, tagValue(e, e.v("namecv"), mkArg(e, "jglobal", e.tup())))),
                                    e.let("modcv", jgfnField(e, "mod")),
                                    e.ifOnly(
                                            jgfnIs(e, "var_fn"),
                                            e.seq(
                                                    e.let(
                                                            "varcv",
                                                            jgfnStaged(
                                                                    e,
                                                                    e.callM(
                                                                            e.callM(
                                                                                    graft(e, e.v("modcv")),
                                                                                    "m",
                                                                                    e.tup()),
                                                                            "declare",
                                                                            e.lower(e.v("name"))))),
                                                    retM(e, tagValue(e, e.v("varcv"), mkArg(e, "jglobal", e.tup()))))),
                                    e.let("pcv", jgfnField(e, "params")),
                                    e.let("ptk", lookup(e, xx(e), cveType(e, e.v("pcv")), aspIface(e, "tuple"))),
                                    e.ifOnly(
                                            e.not(e.fld(e.v("ptk"), "is_some")),
                                            retM(e, errCve(e, loc(e), "js function params must be a tuple of names"))),
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
                                                    e.let("ncv", jgfnStaged(e, graft(e, unwrapCveM(e, e.v("er"))))),
                                                    e.ifOnly(
                                                            isRuntime(e, e.v("ncv")),
                                                            retM(
                                                                    e,
                                                                    errCve(
                                                                            e,
                                                                            loc(e),
                                                                            "js function parameter names must be constant"))),
                                                    push(e, e.v("names"), constStr(e, e.v("ncv"))))),
                                    e.let("bodycv", jgfnField(e, "body")),
                                    e.let(
                                            "refcv",
                                            jgfnStaged(
                                                    e,
                                                    e.callM(
                                                            e.callM(graft(e, e.v("modcv")), "m", e.tup()),
                                                            "func",
                                                            e.rec(
                                                                    e.p("name", e.lower(e.v("name"))),
                                                                    e.p("params", e.lower(e.v("names"))),
                                                                    e.p("body", graft(e, e.v("bodycv"))))))),
                                    e.ret(tagValue(e, e.v("refcv"), mkArg(e, "jglobal", e.tup()))))),
                    e.p("exception_type", e.optionTypeLoweredNone())));
        }

        x.add(e.let("js_b", e.defFunc(e.v("d"), "js")));
        x.add(e.funcImpl(
                e.v("js_b"),
                e.tup(),
                e.optionTypeSome(t(e, "jmod")),
                e.mb.optNoneType(),
                e.stage(e.seq(
                        e.let(
                                "m",
                                mk(
                                        e,
                                        "jmod",
                                        e.p("n", num(e, 0)),
                                        e.p("xx", e.optNone(e.typesExprContext())),
                                        e.p("funcs", e.listNew(str(e))),
                                        e.p("stmts", e.listNew(str(e))))),
                        e.let("tgt", mk(e, "jtarget", e.p("mod", e.v("m")))),
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
        defs.add(e.p("new", e.fld(e.v("js_b"), "func")));
        defs.add(e.p("mod_t", t(e, "jmod")));
        for (var obj : new String[] {"jany", "jglobal", "jgfn_t"}) defs.add(e.p(obj + "_new", e.fld(e.v(obj), "new")));
        x.add(e.ret(e.rec(defs.toArray(EcoMb.P[]::new))));

        var y = new TList<SourceExpr>();
        y.add(e.let("k", e.lower(e.mb.seq(x))));
        var exported = new TList<String>();
        exported.add("any");
        y.add(e.let("any", e.mb.as(e.call(e.fld(e.v("k"), "jany_new"), e.tup()), ty(e))));
        exported.add("global");
        y.add(e.let("global", e.mb.as(e.call(e.fld(e.v("k"), "jglobal_new"), e.tup()), ty(e))));
        for (var kind : JGFN_KINDS) {
            exported.add(kind);
            y.add(e.let(
                    kind,
                    e.mb.as(
                            e.call(e.fld(e.v("k"), "jgfn_t_new"), e.rec(e.p("kind", e.intOf(jgfnKind(kind))))),
                            ty(e))));
        }
        y.add(e.let("d", e.define()));
        y.add(e.optionTypeLet());
        y.add(e.let("mod_o", e.defObject(e.v("d"), "js module")));
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
                "run",
                e.mb.typesExpr(),
                e.optionTypeNone(),
                e.stage(e.callM(in(e, "m"), "run", e.v("arg")))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "assign_index",
                e.rec(e.p("target", str(e)), e.p("key", str(e)), e.p("value", str(e))),
                e.optionTypeNone(),
                e.stage(e.callM(in(e, "m"), "assign_index", e.v("arg")))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "func_text",
                e.rec(e.p("name", str(e)), e.p("params", e.listType(str(e))), e.p("lines", e.listType(str(e)))),
                e.optionTypeNone(),
                e.stage(e.callM(in(e, "m"), "func_text", e.v("arg")))));
        y.add(e.methodImpl(
                e.v("mod_o"),
                "fresh",
                e.tup(),
                e.optionTypeSome(str(e)),
                e.stage(e.callM(in(e, "m"), "fresh", e.tup()))));
        y.add(e.let("js_b", e.defFunc(e.v("d"), "js")));
        y.add(e.funcImpl(
                e.v("js_b"),
                e.tup(),
                e.optionTypeSome(e.fld(e.v("mod_o"), "type")),
                e.mb.optNoneType(),
                e.stage(mkArg(e, "mod_o", e.rec(e.p("m", e.call(e.fld(e.lowerV("k"), "new"), e.tup())))))));
        y.add(e.finish(e.v("d")));
        var ydefs = new TList<EcoMb.P>();
        ydefs.add(e.p("new", e.fld(e.v("js_b"), "func")));
        ydefs.add(e.p("mod_t", e.fld(e.v("mod_o"), "type")));
        for (var name : exported) ydefs.add(e.p(name, e.v(name)));
        y.add(e.ret(e.rec(ydefs.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("k", e.lower(e.mb.seq(y))));
        var exports = new TList<EcoMb.P>();
        for (var name : new String[] {"new", "mod_t"}) exports.add(e.p(name, e.fld(e.v("k"), name)));
        for (var name : exported) exports.add(e.p(name, e.fld(e.v("k"), name)));
        top.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return top;
    }
}
