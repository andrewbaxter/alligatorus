package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class EcoLlvmIr {

    private EcoLlvmIr() {}

    private static SourceExpr freshTmp(EcoMb e) {
        return e.let("r", e.cat(e.s("%t"), e.toStr(e.fld(e.v("inner"), "tmp"))));
    }

    private static SourceExpr bump(EcoMb e) {
        return e.incr(e.fld(e.v("inner"), "tmp"));
    }

    private static SourceExpr emit(EcoMb e, SourceExpr line) {
        return e.push(e.fld(e.v("inner"), "lines"), line);
    }

    private static SourceExpr newLv(EcoMb e, SourceExpr ty, SourceExpr ref) {
        return e.call(e.lower(e.fld(e.v("lvo"), "new")), e.rec(e.p("ty", ty), e.p("ref", ref)));
    }

    private static SourceExpr argF(EcoMb e, String... path) {
        return e.fld(e.v("arg"), path);
    }

    private static SourceExpr vT(EcoMb e, SourceExpr value) {
        return e.callM(value, "t", e.nil());
    }

    private static SourceExpr vR(EcoMb e, SourceExpr value) {
        return e.callM(value, "r", e.nil());
    }

    private static SourceExpr cveLval(EcoMb e, SourceExpr cve) {
        return e.call(
                e.mb.mustReinterpret(),
                e.tup(
                        e.fld(e.call(e.fld(cve, "value"), e.mb.tupleOf()), "value"),
                        e.lower(e.fld(e.v("lval"), "type"))));
    }

    private static SourceExpr targetPanic(EcoMb e, String method) {
        return e.mb.panic(e.s("ltarget." + method + " not implemented"));
    }

    private static SourceExpr lvalOf(EcoMb e, SourceExpr cve) {
        return e.callM(cveLval(e, cve), "lv", e.nil());
    }

    private static SourceExpr appendCode(EcoMb e, SourceExpr code, SourceExpr out) {
        return e.call(e.lowerV("append_code"), e.rec(e.p("code", code), e.p("out", out)));
    }

    private static SourceExpr newLco(EcoMb e, String linesVar) {
        return e.call(e.lower(e.fld(e.v("lco"), "new")), e.rec(e.p("lines", e.v(linesVar))));
    }

    private static SourceExpr asCode(EcoMb e, SourceExpr lcoInst) {
        return e.mb.as(lcoInst, e.mb.typesCode());
    }

    private static void operandStmts(EcoMb e, SourceExpr cve, String pfx, String outVar, TList<SourceExpr> body) {
        body.add(e.let(pfx + "_val", e.fld(e.call(e.fld(cve, "value"), e.tup()), "value")));
        body.add(e.let(
                pfx + "_acl",
                e.call(
                        e.mb.evalAspectLookup(),
                        e.tup(
                                e.call(e.fld(e.mb.evalValue(), "type"), e.v(pfx + "_val")),
                                e.lower(e.fld(e.v("acl"), "type")),
                                e.fld(e.v("arg"), "xx")))));
        body.add(e.let(
                pfx + "_lv",
                e.ifElse(
                        e.fld(e.v(pfx + "_acl"), "is_some"),
                        e.call(
                                e.fld(e.lower(e.fld(e.v("acl"), "type")), "to_lv"),
                                e.tup(e.fld(e.v(pfx + "_acl"), "value"), cve)),
                        e.callM(
                                e.fld(e.v("inner"), "fb"),
                                "konst",
                                e.rec(
                                        e.p("ty", e.s("double")),
                                        e.p(
                                                "ref",
                                                e.call(
                                                        e.mb.builtin("eval", "const_numeric_to_str"),
                                                        e.rec(
                                                                e.p("xx", e.fld(e.v("arg"), "xx")),
                                                                e.p("value", e.v(pfx + "_val"))))))))));
        body.add(appendCode(e, e.fld(cve, "code"), e.v(outVar)));
    }

    private static SourceExpr aspectResult(EcoMb e, String linesVar, String lvalVar) {
        return e.ret(e.eresultOk(
                e.mb.typesCve(),
                e.call(
                        e.mb.evalCodevalue(),
                        e.rec(e.p("code", asCode(e, newLco(e, linesVar))), e.p("value", e.v(lvalVar))))));
    }

    private static SourceExpr toLvBody(EcoMb e) {
        return e.callM(cveLval(e, e.v("arg")), "lv", e.nil());
    }

    private static SourceExpr binOpBody(EcoMb e, String op, String resTy, String tyVar) {
        var body = new TList<SourceExpr>();
        body.add(e.let("out", e.listNew(e.mb.typesString())));
        operandStmts(e, e.fld(e.v("arg"), "base"), "a", "out", body);
        operandStmts(e, e.fld(e.v("arg"), "reference"), "b", "out", body);
        body.add(e.let("r", e.callM(e.fld(e.v("inner"), "fb"), "fresh", e.nil())));
        body.add(e.push(
                e.v("out"),
                e.cat(
                        e.s("  "),
                        e.v("r"),
                        e.s(" = " + op + " double "),
                        e.callM(e.v("a_lv"), "r", e.nil()),
                        e.s(", "),
                        e.callM(e.v("b_lv"), "r", e.nil()))));
        body.add(e.let(
                "res_lval",
                e.call(
                        e.lower(e.fld(e.v("lval"), "new")),
                        e.rec(
                                e.p(
                                        "v",
                                        e.call(
                                                e.lower(e.fld(e.v("lvo"), "new")),
                                                e.rec(e.p("ty", e.s(resTy)), e.p("ref", e.v("r"))))),
                                e.p("ty", e.v(tyVar))))));
        body.add(aspectResult(e, "out", "res_lval"));
        return e.mb.seq(body);
    }

    public static SourceExpr module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("d", e.define()));

        exprs.add(e.let("lvo", e.defObject(e.v("d"), "llvm value")));
        exprs.add(
                e.callM(e.v("lvo"), "set_inner", e.rec(e.p("ty", e.mb.typesString()), e.p("ref", e.mb.typesString()))));
        exprs.add(e.methodImpl(
                e.v("lvo"), "t", e.mb.typesNull(), e.retSome(e.mb.typesString()), e.stage(e.fld(e.v("inner"), "ty"))));
        exprs.add(e.methodImpl(
                e.v("lvo"), "r", e.mb.typesNull(), e.retSome(e.mb.typesString()), e.stage(e.fld(e.v("inner"), "ref"))));

        exprs.add(e.let("fvo", e.defObject(e.v("d"), "llvm extern function")));
        exprs.add(e.callM(
                e.v("fvo"),
                "set_inner",
                e.rec(
                        e.p("ref", e.mb.typesString()),
                        e.p("ret", e.mb.typesString()),
                        e.p("callty", e.mb.typesString()))));

        exprs.add(e.let("lfb", e.defObject(e.v("d"), "llvm function builder")));
        exprs.add(e.callM(
                e.v("lfb"),
                "set_inner",
                e.rec(
                        e.p("name", e.mb.typesString()),
                        e.p("ret", e.mb.typesString()),
                        e.p("params", e.strListType()),
                        e.p("lines", e.strListType()),
                        e.p("tmp", e.mb.typesInt()),
                        e.p("funcs_out", e.strListType()))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "param",
                e.rec(e.p("i", e.mb.typesInt()), e.p("ty", e.mb.typesString())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(newLv(e, argF(e, "ty"), e.cat(e.s("%a"), e.toStr(argF(e, "i")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "konst",
                e.rec(e.p("ty", e.mb.typesString()), e.p("ref", e.mb.typesString())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(newLv(e, argF(e, "ty"), argF(e, "ref")))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "bin",
                e.rec(
                        e.p("op", e.mb.typesString()),
                        e.p("a", e.fld(e.v("lvo"), "type")),
                        e.p("b", e.fld(e.v("lvo"), "type"))),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        e.let("aty", vT(e, argF(e, "a"))),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = "),
                                        argF(e, "op"),
                                        e.s(" "),
                                        e.v("aty"),
                                        e.s(" "),
                                        vR(e, argF(e, "a")),
                                        e.s(", "),
                                        vR(e, argF(e, "b")))),
                        e.ret(newLv(e, e.v("aty"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "cmp",
                e.rec(
                        e.p("op", e.mb.typesString()),
                        e.p("a", e.fld(e.v("lvo"), "type")),
                        e.p("b", e.fld(e.v("lvo"), "type"))),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = "),
                                        argF(e, "op"),
                                        e.s(" "),
                                        vT(e, argF(e, "a")),
                                        e.s(" "),
                                        vR(e, argF(e, "a")),
                                        e.s(", "),
                                        vR(e, argF(e, "b")))),
                        e.ret(newLv(e, e.s("i1"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "conv",
                e.rec(
                        e.p("op", e.mb.typesString()),
                        e.p("a", e.fld(e.v("lvo"), "type")),
                        e.p("ty", e.mb.typesString())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = "),
                                        argF(e, "op"),
                                        e.s(" "),
                                        vT(e, argF(e, "a")),
                                        e.s(" "),
                                        vR(e, argF(e, "a")),
                                        e.s(" to "),
                                        argF(e, "ty"))),
                        e.ret(newLv(e, argF(e, "ty"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "load",
                e.rec(e.p("ty", e.mb.typesString()), e.p("p", e.fld(e.v("lvo"), "type"))),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = load "),
                                        argF(e, "ty"),
                                        e.s(", ptr "),
                                        vR(e, argF(e, "p")))),
                        e.ret(newLv(e, argF(e, "ty"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "store",
                e.rec(e.p("v", e.fld(e.v("lvo"), "type")), e.p("p", e.fld(e.v("lvo"), "type"))),
                e.retSome(e.mb.typesNull()),
                e.stage(emit(
                        e,
                        e.cat(
                                e.s("  store "),
                                vT(e, argF(e, "v")),
                                e.s(" "),
                                vR(e, argF(e, "v")),
                                e.s(", ptr "),
                                vR(e, argF(e, "p")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "gep",
                e.rec(
                        e.p("base", e.mb.typesString()),
                        e.p("p", e.fld(e.v("lvo"), "type")),
                        e.p("idx", e.strListType())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = getelementptr inbounds "),
                                        argF(e, "base"),
                                        e.s(", ptr "),
                                        vR(e, argF(e, "p")),
                                        e.s(", "),
                                        e.join(argF(e, "idx"), ", "))),
                        e.ret(newLv(e, e.s("ptr"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "alloca",
                e.mb.typesString(),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        emit(e, e.cat(e.s("  "), e.v("r"), e.s(" = alloca "), e.v("arg"))),
                        e.ret(newLv(e, e.s("ptr"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "call",
                e.rec(e.p("f", e.fld(e.v("fvo"), "type")), e.p("args", e.strListType())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = call "),
                                        e.callM(argF(e, "f"), "callty", e.nil()),
                                        e.s(" "),
                                        e.callM(argF(e, "f"), "r", e.nil()),
                                        e.s("("),
                                        e.join(argF(e, "args"), ", "),
                                        e.s(")"))),
                        e.ret(newLv(e, e.callM(argF(e, "f"), "retty", e.nil()), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "call_void",
                e.rec(e.p("f", e.fld(e.v("fvo"), "type")), e.p("args", e.strListType())),
                e.retSome(e.mb.typesNull()),
                e.stage(emit(
                        e,
                        e.cat(
                                e.s("  call "),
                                e.callM(argF(e, "f"), "callty", e.nil()),
                                e.s(" "),
                                e.callM(argF(e, "f"), "r", e.nil()),
                                e.s("("),
                                e.join(argF(e, "args"), ", "),
                                e.s(")"))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "block",
                e.mb.typesNull(),
                e.retSome(e.mb.typesString()),
                e.stage(e.seq(
                        e.let("r", e.cat(e.s("b"), e.toStr(e.fld(e.v("inner"), "tmp")))),
                        e.incr(e.fld(e.v("inner"), "tmp")),
                        e.ret(e.v("r"))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "place",
                e.mb.typesString(),
                e.retSome(e.mb.typesNull()),
                e.stage(emit(e, e.cat(e.v("arg"), e.s(":"))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "br",
                e.mb.typesString(),
                e.retSome(e.mb.typesNull()),
                e.stage(emit(e, e.cat(e.s("  br label %"), e.v("arg"))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "cond_br",
                e.rec(e.p("c", e.fld(e.v("lvo"), "type")), e.p("t", e.mb.typesString()), e.p("f", e.mb.typesString())),
                e.retSome(e.mb.typesNull()),
                e.stage(emit(
                        e,
                        e.cat(
                                e.s("  br i1 "),
                                vR(e, argF(e, "c")),
                                e.s(", label %"),
                                argF(e, "t"),
                                e.s(", label %"),
                                argF(e, "f"))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "phi",
                e.rec(e.p("ty", e.mb.typesString()), e.p("entries", e.strListType())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = phi "),
                                        argF(e, "ty"),
                                        e.s(" "),
                                        e.join(argF(e, "entries"), ", "))),
                        e.ret(newLv(e, argF(e, "ty"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "select",
                e.rec(
                        e.p("c", e.fld(e.v("lvo"), "type")),
                        e.p("a", e.fld(e.v("lvo"), "type")),
                        e.p("b", e.fld(e.v("lvo"), "type"))),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        freshTmp(e),
                        bump(e),
                        e.let("aty", vT(e, argF(e, "a"))),
                        emit(
                                e,
                                e.cat(
                                        e.s("  "),
                                        e.v("r"),
                                        e.s(" = select i1 "),
                                        vR(e, argF(e, "c")),
                                        e.s(", "),
                                        e.v("aty"),
                                        e.s(" "),
                                        vR(e, argF(e, "a")),
                                        e.s(", "),
                                        vT(e, argF(e, "b")),
                                        e.s(" "),
                                        vR(e, argF(e, "b")))),
                        e.ret(newLv(e, e.v("aty"), e.v("r")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "ret",
                e.fld(e.v("lvo"), "type"),
                e.retSome(e.mb.typesNull()),
                e.stage(emit(e, e.cat(e.s("  ret "), vT(e, e.v("arg")), e.s(" "), vR(e, e.v("arg")))))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "ret_void",
                e.mb.typesNull(),
                e.retSome(e.mb.typesNull()),
                e.stage(emit(e, e.s("  ret void")))));

        exprs.add(e.methodImpl(
                e.v("lfb"),
                "finish",
                e.mb.typesNull(),
                e.retSome(e.mb.typesString()),
                e.stage(e.seq(
                        e.let("parts", e.listNew(e.mb.typesString())),
                        e.let("pi", e.rec(e.p("i", e.intOf(0)))),
                        e.foreach(
                                e.fld(e.v("inner"), "params"),
                                "pt",
                                e.seq(
                                        e.push(
                                                e.v("parts"),
                                                e.cat(e.v("pt"), e.s(" %a"), e.toStr(e.fld(e.v("pi"), "i")))),
                                        e.incr(e.fld(e.v("pi"), "i")))),
                        e.let(
                                "rendered",
                                e.cat(
                                        e.s("define "),
                                        e.fld(e.v("inner"), "ret"),
                                        e.s(" @"),
                                        e.fld(e.v("inner"), "name"),
                                        e.s("("),
                                        e.join(e.v("parts"), ", "),
                                        e.s(") {\n"),
                                        e.join(e.fld(e.v("inner"), "lines"), "\n"),
                                        e.s("\n}\n"))),
                        e.push(e.fld(e.v("inner"), "funcs_out"), e.v("rendered")),
                        e.ret(e.v("rendered"))))));

        exprs.add(e.methodImpl(
                e.v("fvo"), "r", e.mb.typesNull(), e.retSome(e.mb.typesString()), e.stage(e.fld(e.v("inner"), "ref"))));
        exprs.add(e.methodImpl(
                e.v("fvo"),
                "retty",
                e.mb.typesNull(),
                e.retSome(e.mb.typesString()),
                e.stage(e.fld(e.v("inner"), "ret"))));
        exprs.add(e.methodImpl(
                e.v("fvo"),
                "callty",
                e.mb.typesNull(),
                e.retSome(e.mb.typesString()),
                e.stage(e.fld(e.v("inner"), "callty"))));

        exprs.add(e.let("lapp", e.defObject(e.v("d"), "llvm module")));
        exprs.add(e.callM(
                e.v("lapp"),
                "set_inner",
                e.rec(e.p("decls", e.strListType()), e.p("globals", e.strListType()), e.p("funcs", e.strListType()))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "extern_func",
                e.rec(e.p("name", e.mb.typesString()), e.p("ret", e.mb.typesString()), e.p("params", e.strListType())),
                e.retSome(e.fld(e.v("fvo"), "type")),
                e.stage(e.seq(
                        e.push(
                                e.fld(e.v("inner"), "decls"),
                                e.cat(
                                        e.s("declare "),
                                        argF(e, "ret"),
                                        e.s(" @"),
                                        argF(e, "name"),
                                        e.s("("),
                                        e.join(argF(e, "params"), ", "),
                                        e.s(")"))),
                        e.ret(e.call(
                                e.lower(e.fld(e.v("fvo"), "new")),
                                e.rec(
                                        e.p("ref", e.cat(e.s("@"), argF(e, "name"))),
                                        e.p("ret", argF(e, "ret")),
                                        e.p("callty", argF(e, "ret")))))))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "extern_func_varargs",
                e.rec(e.p("name", e.mb.typesString()), e.p("ret", e.mb.typesString()), e.p("params", e.strListType())),
                e.retSome(e.fld(e.v("fvo"), "type")),
                e.stage(e.seq(
                        e.let("psl", e.join(argF(e, "params"), ", ")),
                        e.let("pn", e.rec(e.p("n", e.intOf(0)))),
                        e.foreach(argF(e, "params"), "pp", e.incr(e.fld(e.v("pn"), "n"))),
                        e.let(
                                "full",
                                e.ifElse(
                                        e.mb.operatorNe(e.fld(e.v("pn"), "n"), e.intOf(0)),
                                        e.cat(e.v("psl"), e.s(", ...")),
                                        e.s("..."))),
                        e.push(
                                e.fld(e.v("inner"), "decls"),
                                e.cat(
                                        e.s("declare "),
                                        argF(e, "ret"),
                                        e.s(" @"),
                                        argF(e, "name"),
                                        e.s("("),
                                        e.v("full"),
                                        e.s(")"))),
                        e.ret(e.call(
                                e.lower(e.fld(e.v("fvo"), "new")),
                                e.rec(
                                        e.p("ref", e.cat(e.s("@"), argF(e, "name"))),
                                        e.p("ret", argF(e, "ret")),
                                        e.p("callty", e.cat(argF(e, "ret"), e.s(" ("), e.v("full"), e.s(")"))))))))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "global",
                e.rec(
                        e.p("name", e.mb.typesString()),
                        e.p("type", e.mb.typesString()),
                        e.p("init", e.mb.typesString())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        e.push(
                                e.fld(e.v("inner"), "globals"),
                                e.cat(
                                        e.s("@"),
                                        argF(e, "name"),
                                        e.s(" = global "),
                                        argF(e, "type"),
                                        e.s(" "),
                                        argF(e, "init"))),
                        e.ret(newLv(e, e.s("ptr"), e.cat(e.s("@"), argF(e, "name"))))))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "global_const",
                e.rec(
                        e.p("name", e.mb.typesString()),
                        e.p("type", e.mb.typesString()),
                        e.p("init", e.mb.typesString())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(e.seq(
                        e.push(
                                e.fld(e.v("inner"), "globals"),
                                e.cat(
                                        e.s("@"),
                                        argF(e, "name"),
                                        e.s(" = constant "),
                                        argF(e, "type"),
                                        e.s(" "),
                                        argF(e, "init"))),
                        e.ret(newLv(e, e.s("ptr"), e.cat(e.s("@"), argF(e, "name"))))))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "konst",
                e.rec(e.p("ty", e.mb.typesString()), e.p("ref", e.mb.typesString())),
                e.retSome(e.fld(e.v("lvo"), "type")),
                e.stage(newLv(e, argF(e, "ty"), argF(e, "ref")))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "func",
                e.rec(e.p("name", e.mb.typesString()), e.p("ret", e.mb.typesString()), e.p("params", e.strListType())),
                e.retSome(e.fld(e.v("lfb"), "type")),
                e.stage(e.call(
                        e.lower(e.fld(e.v("lfb"), "new")),
                        e.rec(
                                e.p("name", argF(e, "name")),
                                e.p("ret", argF(e, "ret")),
                                e.p("params", argF(e, "params")),
                                e.p("lines", e.listNew(e.mb.typesString())),
                                e.p("tmp", e.intOf(0)),
                                e.p("funcs_out", e.fld(e.v("inner"), "funcs")))))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "render",
                e.mb.typesNull(),
                e.retSome(e.mb.typesString()),
                e.stage(e.seq(
                        e.let("out", e.listNew(e.mb.typesString())),
                        e.foreach(e.fld(e.v("inner"), "decls"), "dl", e.push(e.v("out"), e.v("dl"))),
                        e.foreach(e.fld(e.v("inner"), "globals"), "gl", e.push(e.v("out"), e.v("gl"))),
                        e.foreach(e.fld(e.v("inner"), "funcs"), "fn", e.push(e.v("out"), e.v("fn"))),
                        e.ret(e.cat(e.join(e.v("out"), "\n"), e.s("\n")))))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "arg_of",
                e.fld(e.v("lvo"), "type"),
                e.retSome(e.mb.typesString()),
                e.stage(e.cat(vT(e, e.v("arg")), e.s(" "), vR(e, e.v("arg"))))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "phi_of",
                e.rec(e.p("v", e.fld(e.v("lvo"), "type")), e.p("label", e.mb.typesString())),
                e.retSome(e.mb.typesString()),
                e.stage(e.cat(e.s("[ "), vR(e, argF(e, "v")), e.s(", %"), argF(e, "label"), e.s(" ]")))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "array_of",
                e.rec(e.p("n", e.mb.typesInt()), e.p("ty", e.mb.typesString())),
                e.retSome(e.mb.typesString()),
                e.stage(e.cat(e.s("["), e.toStr(argF(e, "n")), e.s(" x "), argF(e, "ty"), e.s("]")))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "struct_of",
                e.strListType(),
                e.retSome(e.mb.typesString()),
                e.stage(e.cat(e.s("{ "), e.join(e.v("arg"), ", "), e.s(" }")))));

        exprs.add(e.methodImpl(
                e.v("lapp"),
                "t_app",
                e.mb.typesNull(),
                e.retSome(e.mb.evalType()),
                e.stage(e.lower(e.fld(e.v("lapp"), "type")))));
        exprs.add(e.methodImpl(
                e.v("lapp"),
                "t_value",
                e.mb.typesNull(),
                e.retSome(e.mb.evalType()),
                e.stage(e.lower(e.fld(e.v("lvo"), "type")))));
        exprs.add(e.methodImpl(
                e.v("lapp"),
                "t_extern",
                e.mb.typesNull(),
                e.retSome(e.mb.evalType()),
                e.stage(e.lower(e.fld(e.v("fvo"), "type")))));
        exprs.add(e.methodImpl(
                e.v("lapp"),
                "t_fb",
                e.mb.typesNull(),
                e.retSome(e.mb.evalType()),
                e.stage(e.lower(e.fld(e.v("lfb"), "type")))));

        {
            exprs.add(e.let("lval", e.defObject(e.v("d"), "llvm staged value")));
            exprs.add(e.callM(
                    e.v("lval"), "set_inner", e.rec(e.p("v", e.fld(e.v("lvo"), "type")), e.p("ty", e.mb.evalType()))));
            exprs.add(e.methodImpl(
                    e.v("lval"),
                    "lv",
                    e.mb.typesNull(),
                    e.retSome(e.fld(e.v("lvo"), "type")),
                    e.stage(e.fld(e.v("inner"), "v"))));
            exprs.add(e.implInterface(e.v("lval"), e.mb.evalValue(), e.p("type", e.fld(e.v("inner"), "ty"))));

            exprs.add(e.let("lco", e.defObject(e.v("d"), "llvm staged code")));
            exprs.add(e.callM(e.v("lco"), "set_inner", e.rec(e.p("lines", e.strListType()))));
            exprs.add(e.methodImpl(
                    e.v("lco"),
                    "append_to",
                    e.strListType(),
                    e.retSome(e.mb.typesNull()),
                    e.stage(e.foreach(e.fld(e.v("inner"), "lines"), "ln", e.push(e.v("arg"), e.v("ln"))))));
            exprs.add(e.implInterface(e.v("lco"), e.mb.builtin("eval", "code_dyn")));

            exprs.add(e.methodImpl(
                    e.v("lfb"),
                    "fresh",
                    e.mb.typesNull(),
                    e.retSome(e.mb.typesString()),
                    e.stage(e.seq(
                            e.let("r", e.cat(e.s("%t"), e.toStr(e.fld(e.v("inner"), "tmp")))),
                            e.incr(e.fld(e.v("inner"), "tmp")),
                            e.ret(e.v("r"))))));
            exprs.add(e.methodImpl(
                    e.v("lfb"),
                    "splice",
                    e.strListType(),
                    e.retSome(e.mb.typesNull()),
                    e.stage(e.foreach(e.v("arg"), "ln", e.push(e.fld(e.v("inner"), "lines"), e.v("ln"))))));

            exprs.add(e.let("append_code_builder", e.defFunc(e.v("d"), "llvm.append_code")));
            exprs.add(e.let("append_code", e.fld(e.v("append_code_builder"), "func")));
            exprs.add(e.funcImpl(
                    e.v("append_code_builder"),
                    e.rec(e.p("code", e.mb.typesCode()), e.p("out", e.strListType())),
                    e.retSome(e.mb.typesString()),
                    e.stage(e.seq(e.ret(e.mb.matchOf(
                            e.fld(e.v("arg"), "code"),
                            e.s("c"),
                            TList.of(
                                    new TestMb.Branch(e.s("none"), e.mb.as(e.s(""), e.mb.typesString())),
                                    new TestMb.Branch(
                                            e.s("target"),
                                            e.mb.matchOf(
                                                    e.callM(e.v("c"), "cast", e.lower(e.fld(e.v("lco"), "type"))),
                                                    e.s("r"),
                                                    TList.of(
                                                            new TestMb.Branch(
                                                                    e.s("ok"),
                                                                    e.seq(
                                                                            e.callM(
                                                                                    e.v("r"),
                                                                                    "append_to",
                                                                                    e.fld(e.v("arg"), "out")),
                                                                            e.ret(
                                                                                    e.mb.as(
                                                                                            e.s(""),
                                                                                            e.mb.typesString())))),
                                                            new TestMb.Branch(
                                                                    e.s("errs"),
                                                                    e.mb.panic(
                                                                            e.s(
                                                                                    "append_code: code is not llvm code")))),
                                                    Opt.none()))),
                            Opt.none()))))));

            exprs.add(e.let("acl", e.callM0(e.v("d"), "interface")));
            exprs.add(e.callM(e.v("acl"), "set_is_aspect", e.b(true)));
            exprs.add(e.let("acl_id", e.callM0(e.v("acl"), "aspect_id")));
            exprs.add(e.callM(
                    e.callM(e.v("acl"), "func", e.s("to_lv")),
                    "impl",
                    e.rec(e.p("arg", e.mb.typesCve()), e.p("ret", e.retSome(e.fld(e.v("lvo"), "type"))))));

            exprs.add(e.let("ltype_fn", e.defObject(e.v("d"), "llvm staged fn type")));
            exprs.add(e.callM(
                    e.v("ltype_fn"),
                    "set_inner",
                    e.rec(
                            e.p("fnv", e.fld(e.v("fvo"), "type")),
                            e.p("fb", e.fld(e.v("lfb"), "type")),
                            e.p("ret_ty", e.mb.evalType()))));
            e.implAsSelfType(
                    e.v("ltype_fn"),
                    "llvm staged fn",
                    e.aspectDispatch(e.mb.builtin("eval", "aspect_function_id"), e.lowerV("acl_id")),
                    exprs);
            exprs.add(e.implInterface(e.v("ltype_fn"), e.fld(e.v("acl"), "type"), e.p("to_lv", toLvBody(e))));
            var callBody = new TList<SourceExpr>();
            callBody.add(e.let("out", e.listNew(e.mb.typesString())));
            operandStmts(e, e.fld(e.v("arg"), "argument"), "a", "out", callBody);
            callBody.add(e.let("r", e.callM(e.fld(e.v("inner"), "fb"), "fresh", e.nil())));
            callBody.add(e.push(
                    e.v("out"),
                    e.cat(
                            e.s("  "),
                            e.v("r"),
                            e.s(" = call "),
                            e.callM(e.fld(e.v("inner"), "fnv"), "callty", e.nil()),
                            e.s(" "),
                            e.callM(e.fld(e.v("inner"), "fnv"), "r", e.nil()),
                            e.s("("),
                            e.callM(e.v("a_lv"), "t", e.nil()),
                            e.s(" "),
                            e.callM(e.v("a_lv"), "r", e.nil()),
                            e.s(")"))));
            callBody.add(e.let(
                    "res_lval",
                    e.call(
                            e.lower(e.fld(e.v("lval"), "new")),
                            e.rec(
                                    e.p(
                                            "v",
                                            e.call(
                                                    e.lower(e.fld(e.v("lvo"), "new")),
                                                    e.rec(
                                                            e.p(
                                                                    "ty",
                                                                    e.callM(
                                                                            e.fld(e.v("inner"), "fnv"),
                                                                            "retty",
                                                                            e.nil())),
                                                            e.p("ref", e.v("r"))))),
                                    e.p("ty", e.fld(e.v("inner"), "ret_ty"))))));
            callBody.add(aspectResult(e, "out", "res_lval"));
            exprs.add(e.implInterface(
                    e.v("ltype_fn"),
                    e.mb.builtin("eval", "aspect_function"),
                    e.p("exception_type", e.optNone(e.mb.evalType())),
                    e.p("call", e.mb.seq(callBody))));

            exprs.add(e.let("marker_f64", e.callM0(e.v("d"), "interface")));
            exprs.add(e.callM(e.v("marker_f64"), "set_is_aspect", e.b(true)));
            exprs.add(e.let("marker_f64_id", e.callM0(e.v("marker_f64"), "aspect_id")));
            exprs.add(e.let("ltype_f64", e.defObject(e.v("d"), "llvm staged f64 type")));
            exprs.add(e.callM(e.v("ltype_f64"), "set_inner", e.rec(e.p("fb", e.fld(e.v("lfb"), "type")))));
            e.implAsSelfTypeEq(
                    e.v("ltype_f64"),
                    "llvm staged f64",
                    e.aspectDispatch(
                            e.mb.builtin("eval", "aspect_operator_add_id"),
                            e.mb.builtin("eval", "aspect_operator_lt_id"),
                            e.lowerV("acl_id"),
                            e.lowerV("marker_f64_id")),
                    field -> e.fld(
                            e.call(
                                    e.mb.evalAspectLookup(),
                                    e.tup(
                                            e.fld(e.v("arg"), field),
                                            e.lower(e.fld(e.v("marker_f64"), "type")),
                                            e.fld(e.v("arg"), "xx"))),
                            "is_some"),
                    exprs);
            exprs.add(e.implInterface(e.v("ltype_f64"), e.fld(e.v("marker_f64"), "type")));
            exprs.add(e.implInterface(e.v("ltype_f64"), e.fld(e.v("acl"), "type"), e.p("to_lv", toLvBody(e))));
            exprs.add(e.implInterface(
                    e.v("ltype_f64"),
                    e.mb.builtin("eval", "operator_add"),
                    e.p("add", binOpBody(e, "fadd", "double", "outer"))));
            exprs.add(e.implInterface(
                    e.v("ltype_f64"),
                    e.mb.builtin("eval", "operator_lt"),
                    e.p("lt", binOpBody(e, "fcmp olt", "i1", "outer"))));

            exprs.add(e.let("lbinding", e.defObject(e.v("d"), "llvm staged binding")));
            exprs.add(e.callM(e.v("lbinding"), "set_inner", e.rec(e.p("val", e.fld(e.v("lval"), "type")))));
            exprs.add(e.implInterface(
                    e.v("lbinding"),
                    e.mb.evalBinding(),
                    e.p("drop", e.mb.panic(e.s("lbinding.drop not implemented"))),
                    e.p(
                            "use",
                            e.seq(e.ret(e.call(
                                    e.mb.evalCodevalue(),
                                    e.rec(
                                            e.p("code", e.fld(e.v("arg"), "code")),
                                            e.p("value", e.fld(e.v("inner"), "val"))))))),
                    e.p("fork", e.v("outer")),
                    e.p("merge", e.mb.panic(e.s("lbinding.merge not implemented")))));

            exprs.add(e.let("ltarget", e.defObject(e.v("d"), "llvm target")));
            exprs.add(e.callM(
                    e.v("ltarget"),
                    "set_inner",
                    e.rec(e.p("fb", e.fld(e.v("lfb"), "type")), e.p("f64t", e.mb.evalType()))));
            var ifBody = new TList<SourceExpr>();
            ifBody.add(e.let("out", e.listNew(e.mb.typesString())));
            ifBody.add(e.let("slot", e.callM(e.fld(e.v("inner"), "fb"), "alloca", e.s("double"))));
            ifBody.add(e.let("end", e.callM(e.fld(e.v("inner"), "fb"), "block", e.nil())));
            var perBranch = new TList<SourceExpr>();
            perBranch.add(appendCode(e, e.fld(e.v("br"), "condition", "code"), e.v("out")));
            perBranch.add(e.let("c_lv", lvalOf(e, e.fld(e.v("br"), "condition"))));
            perBranch.add(e.let("thn", e.callM(e.fld(e.v("inner"), "fb"), "block", e.nil())));
            perBranch.add(e.let("nxt", e.callM(e.fld(e.v("inner"), "fb"), "block", e.nil())));
            perBranch.add(e.push(
                    e.v("out"),
                    e.cat(
                            e.s("  br i1 "),
                            e.callM(e.v("c_lv"), "r", e.nil()),
                            e.s(", label %"),
                            e.v("thn"),
                            e.s(", label %"),
                            e.v("nxt"))));
            perBranch.add(e.push(e.v("out"), e.cat(e.v("thn"), e.s(":"))));
            perBranch.add(appendCode(e, e.fld(e.v("br"), "body", "code"), e.v("out")));
            perBranch.add(e.let("b_lv", lvalOf(e, e.fld(e.v("br"), "body"))));
            perBranch.add(e.push(
                    e.v("out"),
                    e.cat(
                            e.s("  store double "),
                            e.callM(e.v("b_lv"), "r", e.nil()),
                            e.s(", ptr "),
                            e.callM(e.v("slot"), "r", e.nil()))));
            perBranch.add(e.push(e.v("out"), e.cat(e.s("  br label %"), e.v("end"))));
            perBranch.add(e.push(e.v("out"), e.cat(e.v("nxt"), e.s(":"))));
            ifBody.add(e.foreach(e.fld(e.v("arg"), "conditions"), "br", e.mb.seq(perBranch)));
            ifBody.add(e.ifOnly(
                    e.fld(e.v("arg"), "else", "is_some"),
                    e.mb.seqOf(
                            appendCode(e, e.fld(e.v("arg"), "else", "value", "code"), e.v("out")),
                            e.let("e_lv", lvalOf(e, e.fld(e.v("arg"), "else", "value"))),
                            e.push(
                                    e.v("out"),
                                    e.cat(
                                            e.s("  store double "),
                                            e.callM(e.v("e_lv"), "r", e.nil()),
                                            e.s(", ptr "),
                                            e.callM(e.v("slot"), "r", e.nil()))))));
            ifBody.add(e.push(e.v("out"), e.cat(e.s("  br label %"), e.v("end"))));
            ifBody.add(e.push(e.v("out"), e.cat(e.v("end"), e.s(":"))));
            ifBody.add(e.let("r", e.callM(e.fld(e.v("inner"), "fb"), "fresh", e.nil())));
            ifBody.add(e.push(
                    e.v("out"),
                    e.cat(e.s("  "), e.v("r"), e.s(" = load double, ptr "), e.callM(e.v("slot"), "r", e.nil()))));
            ifBody.add(e.let(
                    "res_lval",
                    e.call(
                            e.lower(e.fld(e.v("lval"), "new")),
                            e.rec(
                                    e.p(
                                            "v",
                                            e.call(
                                                    e.lower(e.fld(e.v("lvo"), "new")),
                                                    e.rec(e.p("ty", e.s("double")), e.p("ref", e.v("r"))))),
                                    e.p("ty", e.fld(e.v("inner"), "f64t"))))));
            ifBody.add(aspectResult(e, "out", "res_lval"));
            exprs.add(e.implInterface(
                    e.v("ltarget"),
                    e.mb.evalTarget(),
                    e.p(
                            "cast_exception",
                            e.eresultOk(e.call(e.mb.typesOption(), e.mb.typesCode()), e.optNone(e.mb.typesCode()))),
                    e.p("initial_bind", targetPanic(e, "initial_bind")),
                    e.p(
                            "seq",
                            e.seq(
                                    e.let("out", e.listNew(e.mb.typesString())),
                                    e.foreach(e.fld(e.v("arg"), "entries"), "en", appendCode(e, e.v("en"), e.v("out"))),
                                    e.ret(e.eresultOk(e.mb.typesCode(), asCode(e, newLco(e, "out")))))),
                    e.p("jump", targetPanic(e, "jump")),
                    e.p("land", targetPanic(e, "land")),
                    e.p("guarded", targetPanic(e, "guarded")),
                    e.p("exc_cleanup", targetPanic(e, "exc_cleanup")),
                    e.p("exit_trampoline", targetPanic(e, "exit_trampoline")),
                    e.p("fresh", e.eresultOk(e.mb.typesCode(), e.fld(e.v("arg"), "code"))),
                    e.p("drop", e.eresultOk(e.mb.typesCode(), e.codeNone())),
                    e.p("return", targetPanic(e, "return")),
                    e.p("stage", targetPanic(e, "stage")),
                    e.p("if", e.mb.seq(ifBody)),
                    e.p("try_catch_finally", targetPanic(e, "try_catch_finally")),
                    e.p("match", targetPanic(e, "match"))));

            exprs.add(e.methodImpl(
                    e.v("lapp"),
                    "func_staged",
                    e.rec(
                            e.p("name", e.mb.typesString()),
                            e.p("sin", e.fld(e.v("fvo"), "type")),
                            e.p("body", e.mb.typesExpr())),
                    e.retSome(e.mb.typesString()),
                    e.stage(e.seq(
                            e.let(
                                    "fb2",
                                    e.call(
                                            e.lower(e.fld(e.v("lfb"), "new")),
                                            e.rec(
                                                    e.p("name", argF(e, "name")),
                                                    e.p("ret", e.s("double")),
                                                    e.p("params", e.strList(e.s("double"))),
                                                    e.p("lines", e.listNew(e.mb.typesString())),
                                                    e.p("tmp", e.intOf(0)),
                                                    e.p("funcs_out", e.fld(e.v("inner"), "funcs"))))),
                            e.let(
                                    "x_lv",
                                    e.callM(
                                            e.v("fb2"),
                                            "param",
                                            e.rec(e.p("i", e.intOf(0)), e.p("ty", e.s("double"))))),
                            e.let(
                                    "f64t",
                                    e.call(e.lower(e.fld(e.v("ltype_f64"), "new")), e.rec(e.p("fb", e.v("fb2"))))),
                            e.let(
                                    "x_lval",
                                    e.call(
                                            e.lower(e.fld(e.v("lval"), "new")),
                                            e.rec(e.p("v", e.v("x_lv")), e.p("ty", e.v("f64t"))))),
                            e.let(
                                    "x_bind",
                                    e.call(e.lower(e.fld(e.v("lbinding"), "new")), e.rec(e.p("val", e.v("x_lval"))))),
                            e.let(
                                    "fnt",
                                    e.call(
                                            e.lower(e.fld(e.v("ltype_fn"), "new")),
                                            e.rec(
                                                    e.p("fnv", argF(e, "sin")),
                                                    e.p("fb", e.v("fb2")),
                                                    e.p("ret_ty", e.v("f64t"))))),
                            e.let(
                                    "fn_addr",
                                    e.callM(
                                            e.v("fb2"),
                                            "konst",
                                            e.rec(
                                                    e.p("ty", e.s("ptr")),
                                                    e.p("ref", e.callM(argF(e, "sin"), "r", e.nil()))))),
                            e.let(
                                    "fn_lval",
                                    e.call(
                                            e.lower(e.fld(e.v("lval"), "new")),
                                            e.rec(e.p("v", e.v("fn_addr")), e.p("ty", e.v("fnt"))))),
                            e.let(
                                    "fn_bind",
                                    e.call(e.lower(e.fld(e.v("lbinding"), "new")), e.rec(e.p("val", e.v("fn_lval"))))),
                            e.let("vx2", e.call(e.mb.builtin("eval", "eval_context"), e.mb.tupleOf())),
                            e.let(
                                    "sc",
                                    e.call(
                                            e.mb.builtin("eval", "scope"),
                                            e.rec(
                                                    e.p("vx", e.v("vx2")),
                                                    e.p(
                                                            "entries",
                                                            e.callM(
                                                                    e.call(
                                                                            e.mb.typesMap(),
                                                                            e.mb.tupleOf(
                                                                                    e.mb.typesConstkey(),
                                                                                    e.mb.evalBinding())),
                                                                    "new",
                                                                    e.rec(
                                                                            e.p("x", e.v("x_bind")),
                                                                            e.p("sin", e.v("fn_bind")))))))),
                            e.let(
                                    "tgt",
                                    e.call(
                                            e.lower(e.fld(e.v("ltarget"), "new")),
                                            e.rec(e.p("fb", e.v("fb2")), e.p("f64t", e.v("f64t"))))),
                            e.let(
                                    "ctx",
                                    e.call(
                                            e.mb.builtin("eval", "expr_context"),
                                            e.rec(
                                                    e.p("scope", e.v("sc")),
                                                    e.p("target", e.mb.as(e.v("tgt"), e.mb.evalTarget())),
                                                    e.p("vx", e.v("vx2")),
                                                    e.p("exception_type", e.mb.typesString())))),
                            e.let(
                                    "res",
                                    e.call(
                                            e.mb.evalConstEval(),
                                            e.rec(e.p("xx", e.v("ctx")), e.p("staged", argF(e, "body"))))),
                            e.let("res_cve", e.fld(e.v("res"), "value")),
                            e.let("root_lines", e.listNew(e.mb.typesString())),
                            appendCode(e, e.fld(e.v("res_cve"), "code"), e.v("root_lines")),
                            e.callM(e.v("fb2"), "splice", e.v("root_lines")),
                            e.let("vopt", e.call(e.fld(e.v("res_cve"), "value"), e.mb.tupleOf())),
                            e.let(
                                    "res_lval",
                                    e.call(
                                            e.mb.mustReinterpret(),
                                            e.tup(e.fld(e.v("vopt"), "value"), e.lower(e.fld(e.v("lval"), "type"))))),
                            e.let("res_lv", e.call(e.fld(e.v("res_lval"), "lv"), e.nil())),
                            e.callM(e.v("fb2"), "ret", e.v("res_lv")),
                            e.ret(e.callM(e.v("fb2"), "finish", e.nil()))))));
        }

        exprs.add(e.let("llvm_builder", e.defFunc(e.v("d"), "llvm")));
        exprs.add(e.funcImpl(
                e.v("llvm_builder"),
                e.mb.typesNull(),
                e.retSome(e.fld(e.v("lapp"), "type")),
                e.stage(e.call(
                        e.lower(e.fld(e.v("lapp"), "new")),
                        e.rec(
                                e.p("decls", e.listNew(e.mb.typesString())),
                                e.p("globals", e.listNew(e.mb.typesString())),
                                e.p("funcs", e.listNew(e.mb.typesString())))))));

        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.fld(e.v("llvm_builder"), "func")));
        return e.lower(e.mb.seq(exprs));
    }
}
