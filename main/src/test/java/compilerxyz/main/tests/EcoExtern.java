package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public record EcoExtern(String name, IList<String> params, String ret, boolean varargs) {
    public static void bindTypes(EcoMb e, TList<SourceExpr> out, String ll) {
        for (var t : new String[] {"i1", "i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void"}) {
            out.add(e.let("t_" + t, e.callM(e.v(ll), t, e.nil())));
        }
        out.add(e.let("t_pv", e.callM(e.v(ll), "ptr", e.v("t_void"))));
        out.add(e.let("t_pu8", e.callM(e.v(ll), "ptr", e.v("t_u8"))));
    }

    public SourceExpr fnType(EcoMb e, String ll) {
        var list = new TList<SourceExpr>();
        for (var p : params) {
            list.add(e.v("t_" + p));
        }
        var ps = list.toArray(new SourceExpr[0]);
        return e.callM(
                e.v(ll),
                varargs ? "fn_varargs" : "fn",
                e.rec(e.p("params", e.listNew(e.mb.evalType(), ps)), e.p("ret", e.v("t_" + ret))));
    }

    public static SourceExpr globalT(EcoMb e, String ll, SourceExpr ty, boolean mem) {
        return e.callM(e.v(ll), "global_t", e.rec(e.p("ty", ty), e.p("mem", e.b(mem))));
    }

    public SourceExpr constType(EcoMb e, String ll) {
        return globalT(e, ll, fnType(e, ll), false);
    }

    public SourceExpr declare(EcoMb e, String ll) {
        return e.callM(e.v(ll), "extern", e.rec(e.p("name", e.s(name)), e.p("ty", fnType(e, ll))));
    }
}
