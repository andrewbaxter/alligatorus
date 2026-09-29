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

        static Llvm staged(EcoMb e) {
            return name -> e.lower(e.fld(e.v("llvm"), name));
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

    public static SourceExpr globalT(EcoMb e, Llvm llvm, SourceExpr ty, boolean mem) {
        return e.call(llvm.llvmField("global_t"), e.rec(e.p("ty", ty), e.p("mem", e.b(mem))));
    }

    public SourceExpr constType(EcoMb e, Llvm llvm) {
        return globalT(e, llvm, fnType(e, llvm), false);
    }

    public SourceExpr declare(EcoMb e, String ll, Llvm llvm) {
        return e.callM(e.v(ll), "extern", e.rec(e.p("name", e.s(name)), e.p("ty", fnType(e, llvm))));
    }
}
