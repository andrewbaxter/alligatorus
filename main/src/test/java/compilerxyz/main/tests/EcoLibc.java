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
            new EcoExtern("sinf", IList.of("f32"), "f32", false),
            new EcoExtern("cosf", IList.of("f32"), "f32", false),
            new EcoExtern("tanf", IList.of("f32"), "f32", false),
            new EcoExtern("expf", IList.of("f32"), "f32", false),
            new EcoExtern("exp2f", IList.of("f32"), "f32", false),
            new EcoExtern("logf", IList.of("f32"), "f32", false),
            new EcoExtern("powf", IList.of("f32", "f32"), "f32", false),
            new EcoExtern("sqrtf", IList.of("f32"), "f32", false),
            new EcoExtern("floorf", IList.of("f32"), "f32", false),
            new EcoExtern("fabsf", IList.of("f32"), "f32", false),
            new EcoExtern("fmodf", IList.of("f32", "f32"), "f32", false),
            new EcoExtern("pow", IList.of("f64", "f64"), "f64", false),
            new EcoExtern("exp", IList.of("f64"), "f64", false),
            new EcoExtern("log", IList.of("f64"), "f64", false),
            new EcoExtern("cos", IList.of("f64"), "f64", false),
            new EcoExtern("tan", IList.of("f64"), "f64", false),
            new EcoExtern("sqrt", IList.of("f64"), "f64", false),
            new EcoExtern("floor", IList.of("f64"), "f64", false),
            new EcoExtern("fabs", IList.of("f64"), "f64", false),
            new EcoExtern("memset", IList.of("pv", "i32", "u64"), "pv", false),
            new EcoExtern("puts", IList.of("pu8"), "i32", false),
            new EcoExtern("exit", IList.of("i32"), "void", false),
            new EcoExtern("printf", IList.of("pu8"), "i32", true));

    public static TList<SourceExpr> module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        var llvm = EcoExtern.Llvm.top(e);
        EcoExtern.bindTypes(e, exprs, llvm);
        for (var ex : EXTERNS) {
            exprs.add(e.let(ex.name() + "_fn_t", ex.fnType(e, llvm)));
            exprs.add(e.let(ex.name() + "_t", EcoExtern.globalT(e, llvm, e.v(ex.name() + "_fn_t"))));
        }

        exprs.add(e.let("d", e.define()));
        exprs.add(e.optionTypeLet());
        exprs.add(e.let("libc_o", e.defObject(e.v("d"), "libc 2.42")));
        var innerFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) innerFields.add(e.p(ex.name(), e.v(ex.name() + "_t")));
        exprs.add(e.objectImpl(e.v("libc_o"), e.rec(innerFields.toArray(EcoMb.P[]::new))));
        for (var ex : EXTERNS) {
            exprs.add(e.methodImpl(
                    e.v("libc_o"),
                    ex.name(),
                    e.tup(),
                    e.optionTypeSome(e.v(ex.name() + "_t")),
                    e.stage(e.fld(e.v("inner"), ex.name()))));
        }

        exprs.add(e.let("libc_builder", e.defFunc(e.v("d"), "libc")));
        var ctorFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            ctorFields.add(e.p(
                    ex.name(),
                    ex.declare(
                            e, e.lower(e.fld(e.v("llvm"), "extern_fn")), e.v("arg"), e.lowerV(ex.name() + "_fn_t"))));
        }
        exprs.add(e.funcImpl(
                e.v("libc_builder"),
                e.fld(e.v("llvm"), "mod_t"),
                e.optionTypeSome(e.fld(e.v("libc_o"), "type")),
                e.mb.optNoneType(),
                e.stage(e.seq(
                        e.callM(e.v("arg"), "lib", e.s("m")),
                        e.ret(e.call(
                                e.lower(e.fld(e.v("libc_o"), "new")), e.rec(ctorFields.toArray(EcoMb.P[]::new))))))));

        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.fld(e.v("libc_builder"), "func")));
        return exprs;
    }
}
