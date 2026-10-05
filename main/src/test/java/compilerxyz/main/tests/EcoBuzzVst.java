package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.fld;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.ifOnly;
import static compilerxyz.main.tests.EcoBuzz.lt;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.ne;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;

final class EcoBuzzVst {
    private EcoBuzzVst() {}

    static final int NG = 6;
    static final int NT = 9;
    static final int TRACKS = 8;
    static final int PARAMS = 10;
    static final int P_PARAM = 6;
    static final int P_VALUE = 7;
    static final int NO_PARAM = 12543;
    static final double VALUE_MAX = 32767.0;

    static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    static SourceExpr resetVals(EcoMb e) {
        return e.seq(
                forRange(e, "k", e.n(NG), set(e, idx(e, p(e, "gval"), e.v("k")), e.n(EcoBuzz.NO_VALUE))),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        forRange(
                                e,
                                "k",
                                e.n(NT),
                                set(e, idx(e, idx(e, p(e, "tval"), e.v("t")), e.v("k")), e.n(EcoBuzz.NO_VALUE)))));
    }

    static SourceExpr applyParamAutomation(EcoMb e, int params) {
        var t = e.v("t");
        return forRange(
                e,
                "t",
                e.n(TRACKS),
                e.let("pi", idx(e, idx(e, p(e, "tval"), t), e.n(P_PARAM))),
                e.let("pvv", idx(e, idx(e, p(e, "tval"), t), e.n(P_VALUE))),
                ifOnly(
                        e,
                        EcoBuzz.and(
                                e,
                                EcoBuzz.and(
                                        e, ne(e, e.v("pi"), e.n(EcoBuzz.NO_VALUE)), ne(e, e.v("pi"), e.n(NO_PARAM))),
                                EcoBuzz.and(
                                        e, ne(e, e.v("pvv"), e.n(EcoBuzz.NO_VALUE)), lt(e, e.v("pi"), e.n(params)))),
                        set(e, idx(e, p(e, "pv"), e.v("pi")), mul(e, f32(e, e.v("pvv")), e.n(1.0 / VALUE_MAX)))));
    }

    static void commonFields(EcoMb e, TList<SourceExpr> fields, int params) {
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "spt", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        fields.add(EcoBuzz.field(e, "pv", EcoBuzz.array(e, e.v("t_f32"), e.intOf(params))));
    }

    static void commonDefs(EcoMb e, TList<SourceExpr> defs) {
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
    }

    static TList<SourceExpr> biquadStep(EcoMb e, SourceExpr coefBase, SourceExpr stateBase, SourceExpr x, String out) {
        var c = coefBase;
        var s = stateBase;
        var st = p(e, "fs");
        var cf = p(e, "coef");
        return TList.of(
                e.let(
                        out,
                        sub(
                                e,
                                sub(
                                        e,
                                        add(
                                                e,
                                                add(
                                                        e,
                                                        mul(e, idx(e, cf, c), x),
                                                        mul(e, idx(e, cf, add(e, c, e.n(1))), idx(e, st, s))),
                                                mul(e, idx(e, cf, add(e, c, e.n(2))), idx(e, st, add(e, s, e.n(1))))),
                                        mul(e, idx(e, cf, add(e, c, e.n(3))), idx(e, st, add(e, s, e.n(2))))),
                                mul(e, idx(e, cf, add(e, c, e.n(4))), idx(e, st, add(e, s, e.n(3)))))),
                set(e, idx(e, st, add(e, s, e.n(3))), idx(e, st, add(e, s, e.n(2)))),
                set(e, idx(e, st, add(e, s, e.n(2))), e.v(out)),
                set(e, idx(e, st, add(e, s, e.n(1))), idx(e, st, s)),
                set(e, idx(e, st, s), x));
    }
}
