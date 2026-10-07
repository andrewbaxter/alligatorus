package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoTargetMb.body;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;

public final class EcoWebidlJs {
    private EcoWebidlJs() {}

    private static SourceExpr var(EcoMb e, String name) {
        return e.call(e.lower(e.fld(e.v("js"), "var_fn")), e.rec(e.p("mod", e.v("m")), e.p("name", e.s(name))));
    }

    private static SourceExpr func(EcoMb e, String name, SourceExpr params, SourceExpr body) {
        return e.call(
                e.lower(e.fld(e.v("js"), "func_fn")),
                e.rec(e.p("mod", e.v("m")), e.p("name", e.s(name)), e.p("params", params), e.p("body", e.stage(body))));
    }

    private static SourceExpr sig(EcoMb e, String field) {
        return e.callM(e.v("sig"), field, e.tup());
    }

    private static SourceExpr line(EcoMb e, SourceExpr... parts) {
        return e.push(e.v("lines"), e.cat(parts));
    }

    private static SourceExpr param(EcoMb e, SourceExpr... parts) {
        return e.push(e.v("params"), e.cat(parts));
    }

    private static SourceExpr kindIs(EcoMb e, SourceExpr kind, String... names) {
        SourceExpr expr = e.eq(kind, e.s(names[names.length - 1]));
        for (int i = names.length - 2; i >= 0; i--) expr = EcoTargetMb.orB(e, e.eq(kind, e.s(names[i])), expr);
        return expr;
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var top = new TList<SourceExpr>();
        top.add(e.let("js", e.importLocal("js", "js-es2020.atz")));
        top.add(e.let("webidl", e.importLocal("webidl", "webidl.atz")));
        top.add(e.let("d", e.define()));
        top.add(e.optionTypeLet());

        top.add(e.let("runtime_b", e.defFunc(e.v("d"), "runtime")));
        var runtime = new TList<SourceExpr>();
        runtime.add(e.let("m", e.fld(e.v("arg"), "mod")));
        for (var g : new String[] {"handles", "instance", "decoder", "Array", "TextDecoder", "Uint8Array"}) {
            runtime.add(e.let(g, e.call(e.lower(e.fld(e.v("js"), "global_fn")), e.rec(e.p("name", e.s(g))))));
        }
        runtime.add(e.let("new", e.call(e.lower(e.fld(e.v("js"), "new_fn")), e.tup())));
        runtime.add(e.let("handles_v", var(e, "handles")));
        runtime.add(e.let("instance_v", var(e, "instance")));
        runtime.add(e.let("decoder_v", var(e, "decoder")));
        runtime.add(e.callM(
                e.v("m"),
                "run",
                e.stage(e.seq(
                        e.mb.set(e.lower(e.v("handles")), e.call(e.lower(e.v("new")), e.lower(e.v("Array")))),
                        e.mb.set(e.lower(e.v("decoder")), e.call(e.lower(e.v("new")), e.lower(e.v("TextDecoder"))))))));
        runtime.add(e.let(
                "h_new_f",
                func(
                        e,
                        "h_new",
                        e.tup(e.s("o")),
                        e.seq(
                                e.let("i", e.fld(e.lower(e.v("handles")), "length")),
                                e.call(e.fld(e.lower(e.v("handles")), "push"), e.v("o")),
                                e.ret(e.v("i"))))));
        runtime.add(e.let("h_get_f", func(e, "h_get", e.tup(e.s("h")), e.mb.field(e.lower(e.v("handles")), e.v("h")))));
        runtime.add(e.let(
                "str_f",
                func(
                        e,
                        "str",
                        e.tup(e.s("ptr"), e.s("len")),
                        e.seq(
                                e.let(
                                        "bytes",
                                        e.call(
                                                e.lower(e.v("new")),
                                                e.tup(
                                                        e.lower(e.v("Uint8Array")),
                                                        e.fld(e.lower(e.v("instance")), "exports", "memory", "buffer"),
                                                        e.v("ptr"),
                                                        e.v("len")))),
                                e.ret(e.call(e.fld(e.lower(e.v("decoder")), "decode"), e.v("bytes")))))));
        top.add(e.funcImpl(
                e.v("runtime_b"),
                e.rec(e.p("mod", e.fld(e.v("js"), "mod_t"))),
                e.optionTypeNone(),
                e.mb.optNoneType(),
                e.stage(e.mb.seq(runtime))));

        top.add(e.let("define_b", e.defFunc(e.v("d"), "define")));
        var define = new TList<SourceExpr>();
        define.add(e.let("m", e.fld(e.v("arg"), "mod")));
        define.add(e.let(
                "sig",
                e.call(
                        e.lower(e.fld(e.v("webidl"), "sig_name")),
                        e.rec(
                                e.p("table", e.lower(e.fld(e.v("webidl"), "spec"))),
                                e.p("name", e.fld(e.v("arg"), "name"))))));
        define.add(e.ifOnly(e.not(sig(e, "ok")), e.mb.panic(e.cat(e.s("webidl-js: "), sig(e, "err")))));
        define.add(e.let("fname", e.cat(e.s("wi"), e.callM0(e.v("m"), "fresh"))));
        define.add(e.let("kind", sig(e, "kind")));
        define.add(e.let("member", sig(e, "member")));
        define.add(e.let("params", e.listNew(e.mb.typesString())));
        define.add(e.let("lines", e.listNew(e.mb.typesString())));
        define.add(e.let("exprs", e.listNew(e.mb.typesString())));
        define.add(e.ifOnly(sig(e, "self"), e.seq(param(e, e.s("self")), line(e, e.s("const recv = h_get(self);")))));
        define.add(e.ifOnly(
                e.eq(e.v("kind"), e.s("ctor")), line(e, e.s("const recv = globalThis."), sig(e, "iface"), e.s(";"))));
        define.add(e.let("i", e.rec(e.p("v", e.intOf(0)))));
        define.add(e.foreach(
                sig(e, "args"),
                "ap",
                e.seq(
                        e.let("n", EcoTargetMb.toStr(e, e.fld(e.v("i"), "v"))),
                        e.incr(e.fld(e.v("i"), "v")),
                        e.let("ak", e.callM(e.v("ap"), "kind", e.tup())),
                        e.let("optional", e.callM(e.v("ap"), "optional", e.tup())),
                        e.let("nullable", e.callM(e.v("ap"), "nullable", e.tup())),
                        e.ifOnly(e.v("optional"), param(e, e.s("o"), e.v("n"))),
                        e.ifOnly(e.v("nullable"), param(e, e.s("n"), e.v("n"))),
                        e.ifElse(
                                e.eq(e.v("ak"), e.s("str")),
                                e.seq(param(e, e.s("a"), e.v("n"), e.s("p")), param(e, e.s("a"), e.v("n"), e.s("l"))),
                                param(e, e.s("a"), e.v("n"))),
                        e.let("x", e.rec(e.p("v", e.dstr("")))),
                        e.ifElse(
                                kindIs(e, e.v("ak"), "handle"),
                                e.mb.set(e.fld(e.v("x"), "v"), e.cat(e.s("h_get(a"), e.v("n"), e.s(")"))),
                                e.ifElse(
                                        kindIs(e, e.v("ak"), "str"),
                                        e.mb.set(
                                                e.fld(e.v("x"), "v"),
                                                e.cat(e.s("str(a"), e.v("n"), e.s("p, a"), e.v("n"), e.s("l)"))),
                                        e.ifElse(
                                                kindIs(e, e.v("ak"), "bool"),
                                                e.mb.set(
                                                        e.fld(e.v("x"), "v"),
                                                        e.cat(e.s("(a"), e.v("n"), e.s(" !== 0)"))),
                                                e.ifElse(
                                                        kindIs(e, e.v("ak"), "i64", "u64"),
                                                        e.mb.set(
                                                                e.fld(e.v("x"), "v"),
                                                                e.cat(e.s("Number(a"), e.v("n"), e.s(")"))),
                                                        e.mb.set(e.fld(e.v("x"), "v"), e.cat(e.s("a"), e.v("n"))))))),
                        e.ifOnly(
                                e.v("nullable"),
                                e.mb.set(
                                        e.fld(e.v("x"), "v"),
                                        e.cat(
                                                e.s("(n"),
                                                e.v("n"),
                                                e.s(" !== 0 ? "),
                                                e.fld(e.v("x"), "v"),
                                                e.s(" : null)")))),
                        e.ifOnly(
                                e.v("optional"),
                                e.mb.set(
                                        e.fld(e.v("x"), "v"),
                                        e.cat(
                                                e.s("(o"),
                                                e.v("n"),
                                                e.s(" !== 0 ? "),
                                                e.fld(e.v("x"), "v"),
                                                e.s(" : undefined)")))),
                        e.push(e.v("exprs"), e.fld(e.v("x"), "v")))));
        define.add(e.let("ret_out", sig(e, "ret_out")));
        define.add(e.ifOnly(e.v("ret_out"), param(e, e.s("out"))));
        define.add(e.let("argv", e.join(e.v("exprs"), ", ")));
        define.add(e.ifElse(
                e.eq(e.v("kind"), e.s("op")),
                line(e, e.s("const r = recv."), e.v("member"), e.s("("), e.v("argv"), e.s(");")),
                e.ifElse(
                        e.eq(e.v("kind"), e.s("get")),
                        line(e, e.s("const r = recv."), e.v("member"), e.s(";")),
                        e.ifElse(
                                e.eq(e.v("kind"), e.s("set")),
                                line(e, e.s("recv."), e.v("member"), e.s(" = "), e.v("argv"), e.s(";")),
                                e.ifElse(
                                        e.eq(e.v("kind"), e.s("ctor")),
                                        line(e, e.s("const r = new recv("), e.v("argv"), e.s(");")),
                                        line(e, e.s("const r = globalThis;")))))));
        define.add(e.let("rk", sig(e, "ret_kind")));
        define.add(e.let("rx", e.rec(e.p("v", e.dstr("")))));
        define.add(e.ifElse(
                kindIs(e, e.v("rk"), "handle", "str"),
                e.mb.set(e.fld(e.v("rx"), "v"), e.dstr("h_new(r)")),
                e.ifElse(
                        kindIs(e, e.v("rk"), "bool"),
                        e.mb.set(e.fld(e.v("rx"), "v"), e.dstr("(r ? 1 : 0)")),
                        e.ifElse(
                                kindIs(e, e.v("rk"), "i64", "u64"),
                                e.mb.set(e.fld(e.v("rx"), "v"), e.dstr("BigInt(r)")),
                                e.ifElse(
                                        kindIs(e, e.v("rk"), "void"),
                                        e.mb.set(e.fld(e.v("rx"), "v"), e.dstr("")),
                                        e.mb.set(e.fld(e.v("rx"), "v"), e.dstr("r")))))));
        define.add(e.ifElse(
                e.v("ret_out"),
                e.seq(
                        e.ifOnly(
                                kindIs(e, e.v("rk"), "i64", "u64", "f32", "f64"),
                                e.mb.panic(e.cat(
                                        e.s("webidl-js: nullable return of kind "),
                                        e.v("rk"),
                                        e.s(" is not supported: "),
                                        e.fld(e.v("arg"), "name")))),
                        line(e, e.s("const cell = new Uint32Array(instance.exports.memory.buffer, out, 2);")),
                        line(e, e.s("if (r === null || r === undefined) {")),
                        line(e, e.s("  cell[0] = 0;")),
                        line(e, e.s("} else {")),
                        line(e, e.s("  cell[0] = 1;")),
                        line(e, e.s("  cell[1] = "), e.fld(e.v("rx"), "v"), e.s(";")),
                        line(e, e.s("}"))),
                e.ifOnly(
                        e.not(e.eq(e.fld(e.v("rx"), "v"), e.s(""))),
                        line(e, e.s("return "), e.fld(e.v("rx"), "v"), e.s(";")))));
        define.add(e.callM(
                e.v("m"),
                "func_text",
                e.rec(e.p("name", e.v("fname")), e.p("params", e.v("params")), e.p("lines", e.v("lines")))));
        define.add(e.ret(e.v("fname")));
        top.add(e.funcImpl(
                e.v("define_b"),
                e.rec(e.p("mod", e.fld(e.v("js"), "mod_t")), e.p("name", e.mb.typesString())),
                e.optionTypeSome(e.mb.typesString()),
                e.mb.optNoneType(),
                e.stage(body(e, define.toArray(SourceExpr[]::new)))));
        top.add(e.finish(e.v("d")));
        top.add(e.ret(
                e.rec(e.p("runtime", e.fld(e.v("runtime_b"), "func")), e.p("define", e.fld(e.v("define_b"), "func")))));
        return top;
    }
}
