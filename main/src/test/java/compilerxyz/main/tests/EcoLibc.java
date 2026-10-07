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

    private static final String MATH_LIB = "m";

    private static final IList<String> NOT_MATH = IList.of("memset", "puts", "exit", "printf");

    public static TList<SourceExpr> module(EcoMb e) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        var llvm = EcoExtern.Llvm.top(e);
        EcoExtern.bindTypes(e, setup, llvm);
        var typeFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            setup.add(e.let(ex.name() + "_fn_t", ex.fnType(e, llvm)));
            typeFields.add(e.p(ex.name() + "_fn_t", e.v(ex.name() + "_fn_t")));
        }
        setup.add(e.ret(e.rec(typeFields.toArray(EcoMb.P[]::new))));

        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        var fields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            var libs = NOT_MATH.contains(ex.name()) ? new String[0] : new String[] {MATH_LIB};
            fields.add(e.p(
                    ex.name(),
                    ex.declare(e, e.fld(e.v("llvm"), "extern_fn"), e.fld(e.v("k"), ex.name() + "_fn_t"), libs)));
        }
        top.add(e.ret(e.rec(fields.toArray(EcoMb.P[]::new))));
        return top;
    }
}
