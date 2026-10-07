package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public record EcoExtern(String name, IList<String> params, String ret, boolean varargs) {
    public interface Llvm {
        SourceExpr llvmField(String name);

        static Llvm top(EcoMb e) {
            return name -> e.fld(e.v("llvm"), name);
        }
    }

    public static void bindTypes(EcoMb e, TList<SourceExpr> out, Llvm llvm) {
        for (var t : new String[] {"i1", "i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void"}) {
            out.add(e.let("t_" + t, llvm.llvmField(t)));
        }
        out.add(e.let("t_pv", e.call(llvm.llvmField("ptr"), e.v("t_void"))));
        out.add(e.let("t_pu8", e.call(llvm.llvmField("ptr"), e.v("t_u8"))));
    }

    public SourceExpr fnType(EcoMb e, Llvm llvm) {
        var list = new TList<SourceExpr>();
        for (var p : params) {
            list.add(e.v("t_" + p));
        }
        var ps = list.toArray(SourceExpr[]::new);
        return e.call(
                llvm.llvmField(varargs ? "fn_varargs" : "fn"),
                e.rec(e.p("params", e.listNew(e.mb.evalType(), ps)), e.p("ret", e.v("t_" + ret))));
    }

    public static SourceExpr globalT(EcoMb e, Llvm llvm, SourceExpr ty) {
        return e.call(llvm.llvmField("global_t"), e.rec(e.p("ty", ty), e.p("mem", e.b(false))));
    }

    public SourceExpr externT(EcoMb e, Llvm llvm, SourceExpr constFnType, String... libs) {
        var libExprs = new SourceExpr[libs.length];
        for (int i = 0; i < libs.length; i++) libExprs[i] = e.s(libs[i]);
        return e.call(
                llvm.llvmField("extern_t"),
                e.rec(
                        e.p("name", e.s(name)),
                        e.p("ty", constFnType),
                        e.p("libs", e.listNew(e.mb.typesString(), libExprs))));
    }

    public SourceExpr declare(EcoMb e, SourceExpr externFn, SourceExpr constFnType, String... libs) {
        var libExprs = new SourceExpr[libs.length];
        for (int i = 0; i < libs.length; i++) libExprs[i] = e.s(libs[i]);
        return e.call(externFn, e.rec(e.p("name", e.s(name)), e.p("ty", constFnType), e.p("libs", e.tup(libExprs))));
    }
}
