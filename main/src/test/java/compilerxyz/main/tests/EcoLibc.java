package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoLibc {
    private EcoLibc() {}

    private static final IList<EcoExtern> EXTERNS = IList.of(
            new EcoExtern("sin", IList.of("f64"), "f64", false),
            new EcoExtern("exp2", IList.of("f64"), "f64", false),
            new EcoExtern("fmod", IList.of("f64", "f64"), "f64", false),
            new EcoExtern("memset", IList.of("pv", "i32", "u64"), "pv", false),
            new EcoExtern("puts", IList.of("pu8"), "i32", false),
            new EcoExtern("exit", IList.of("i32"), "void", false),
            new EcoExtern("printf", IList.of("pu8"), "i32", true));

    public static SourceExpr module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        exprs.add(e.let("probe", e.call(e.v("llvm"), e.nil())));
        exprs.add(e.let("lmod_t", e.callM(e.v("probe"), "t_mod", e.nil())));
        exprs.add(e.let("lref_t", e.callM(e.v("probe"), "t_ref", e.nil())));

        exprs.add(e.let("d", e.define()));
        exprs.add(e.let("libc_o", e.defObject(e.v("d"), "libc 2.42")));
        var innerFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            innerFields.add(e.p(ex.name(), e.v("lref_t")));
            innerFields.add(e.p(ex.name() + "_t", e.mb.evalType()));
        }
        exprs.add(e.callM(e.v("libc_o"), "set_inner", e.rec(innerFields.toArray(new EcoMb.P[0]))));
        for (var ex : EXTERNS) {
            exprs.add(e.methodImpl(
                    e.v("libc_o"),
                    ex.name(),
                    e.mb.typesNull(),
                    e.retSome(e.v("lref_t")),
                    e.stage(e.fld(e.v("inner"), ex.name()))));
            exprs.add(e.methodImpl(
                    e.v("libc_o"),
                    ex.name() + "_t",
                    e.mb.typesNull(),
                    e.retSome(e.mb.evalType()),
                    e.stage(e.fld(e.v("inner"), ex.name() + "_t"))));
        }

        exprs.add(e.let("libc_builder", e.defFunc(e.v("d"), "libc")));
        var body = new TList<SourceExpr>();
        EcoExtern.bindTypes(e, body, "arg");
        var ctorFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            body.add(e.let(ex.name(), ex.declare(e, "arg")));
            body.add(e.let(ex.name() + "_t", ex.constType(e, "arg")));
            ctorFields.add(e.p(ex.name(), e.v(ex.name())));
            ctorFields.add(e.p(ex.name() + "_t", e.v(ex.name() + "_t")));
        }
        body.add(e.ret(e.call(e.lower(e.fld(e.v("libc_o"), "new")), e.rec(ctorFields.toArray(new EcoMb.P[0])))));
        exprs.add(e.funcImpl(
                e.v("libc_builder"), e.v("lmod_t"), e.retSome(e.fld(e.v("libc_o"), "type")), e.stage(e.mb.seq(body))));

        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.fld(e.v("libc_builder"), "func")));
        return e.lower(e.mb.seq(exprs));
    }
}
