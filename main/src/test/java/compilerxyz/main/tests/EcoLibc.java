package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoLibc {
    private EcoLibc() {}

    private record Extern(String name, String ret, IList<String> params) {}

    private static final IList<Extern> EXTERNS = IList.of(
            new Extern("sin", "double", IList.of("double")),
            new Extern("exp2", "double", IList.of("double")),
            new Extern("fmod", "double", IList.of("double", "double")),
            new Extern("memset", "ptr", IList.of("ptr", "i32", "i64")),
            new Extern("puts", "i32", IList.of("ptr")),
            new Extern("exit", "void", IList.of("i32")));

    private static final IList<Extern> VARARG_EXTERNS = IList.of(new Extern("printf", "i32", IList.of("ptr")));

    public static SourceExpr module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        exprs.add(e.let("probe", e.call(e.v("llvm"), e.nil())));
        exprs.add(e.let("lapp_t", e.callM(e.v("probe"), "t_app", e.nil())));
        exprs.add(e.let("fnv_t", e.callM(e.v("probe"), "t_extern", e.nil())));

        exprs.add(e.let("d", e.define()));
        exprs.add(e.let("libc_o", e.defObject(e.v("d"), "libc 2.42")));
        var innerFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            innerFields.add(e.p(ex.name(), e.v("fnv_t")));
        }
        for (var ex : VARARG_EXTERNS) {
            innerFields.add(e.p(ex.name(), e.v("fnv_t")));
        }
        exprs.add(e.callM(e.v("libc_o"), "set_inner", e.rec(innerFields.toArray(new EcoMb.P[0]))));
        for (var ex : EXTERNS) {
            exprs.add(e.methodImpl(
                    e.v("libc_o"),
                    ex.name(),
                    e.mb.typesNull(),
                    e.retSome(e.v("fnv_t")),
                    e.stage(e.fld(e.v("inner"), ex.name()))));
        }
        for (var ex : VARARG_EXTERNS) {
            exprs.add(e.methodImpl(
                    e.v("libc_o"),
                    ex.name(),
                    e.mb.typesNull(),
                    e.retSome(e.v("fnv_t")),
                    e.stage(e.fld(e.v("inner"), ex.name()))));
        }

        exprs.add(e.let("libc_builder", e.defFunc(e.v("d"), "libc")));
        var body = new TList<SourceExpr>();
        var ctorFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            var params = new TList<SourceExpr>();
            for (var p : ex.params()) {
                params.add(e.s(p));
            }
            body.add(e.let(
                    ex.name(),
                    e.callM(
                            e.v("arg"),
                            "extern_func",
                            e.rec(
                                    e.p("name", e.s(ex.name())),
                                    e.p("ret", e.s(ex.ret())),
                                    e.p("params", listOf(e, params))))));
            ctorFields.add(e.p(ex.name(), e.v(ex.name())));
        }
        for (var ex : VARARG_EXTERNS) {
            var params = new TList<SourceExpr>();
            for (var p : ex.params()) {
                params.add(e.s(p));
            }
            body.add(e.let(
                    ex.name(),
                    e.callM(
                            e.v("arg"),
                            "extern_func_varargs",
                            e.rec(
                                    e.p("name", e.s(ex.name())),
                                    e.p("ret", e.s(ex.ret())),
                                    e.p("params", listOf(e, params))))));
            ctorFields.add(e.p(ex.name(), e.v(ex.name())));
        }
        body.add(e.ret(e.call(e.lower(e.fld(e.v("libc_o"), "new")), e.rec(ctorFields.toArray(new EcoMb.P[0])))));
        exprs.add(e.funcImpl(
                e.v("libc_builder"), e.v("lapp_t"), e.retSome(e.fld(e.v("libc_o"), "type")), e.stage(e.mb.seq(body))));

        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.fld(e.v("libc_builder"), "func")));
        return e.lower(e.mb.seq(exprs));
    }

    private static SourceExpr listOf(EcoMb e, TList<SourceExpr> elements) {
        var arr = new SourceExpr[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            arr[i] = elements.get(i);
        }
        return e.strList(arr);
    }
}
