package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.call;
import static compilerxyz.main.tests.EcoBuzz.div;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.gt;
import static compilerxyz.main.tests.EcoBuzz.i32;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.ifOnly;
import static compilerxyz.main.tests.EcoBuzz.lt;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.ne;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzKarmaFilter {
    private EcoBuzzKarmaFilter() {}

    private static final double RATE = 44100.0;
    private static final double TWO_PI = 2 * Math.PI;
    private static final double F0_BASE = 65.88;
    private static final double F0_SQ = 21919.4;
    private static final double F0_NOTCH_BASE = 110.54;
    private static final double F0_NOTCH_SQ = 21852.6;
    private static final double F0_RESONATOR_SQ = RATE / 2;
    private static final double Q_BASE = 0.7;
    private static final double Q_RES = 20.0;
    private static final double Q_NOTCH = 0.901;
    private static final double RESONATOR_RADIUS = 0.99;
    private static final int MODES = 4;
    private static final int MODE_LOWPASS = 0;
    private static final int MODE_HIGHPASS = 1;
    private static final int MODE_RESONATOR = 2;
    private static final int COEF_MAIN = 0;
    private static final int COEF_FIXED = 5;
    private static final int STATE_FIXED = 8;
    private static final IList<Double> FIXED = IList.of(0.6107, 0.0, -0.6107, -0.7772, -0.2217);

    private static SourceExpr p(EcoMb e, String field) {
        return EcoBuzzVst.p(e, field);
    }

    private static SourceExpr pv(EcoMb e, int k) {
        var v = idx(e, p(e, "pv"), e.n(k));
        return e.ifElse(lt(e, v, e.n(0)), f32(e, e.n(0)), e.ifElse(gt(e, v, e.n(1)), f32(e, e.n(1)), v));
    }

    private static SourceExpr coef(EcoMb e, int k) {
        return idx(e, p(e, "coef"), e.n(COEF_MAIN + k));
    }

    private static SourceExpr setCoefs(
            EcoMb e, SourceExpr b0, SourceExpr b1, SourceExpr b2, SourceExpr a1, SourceExpr a2) {
        return e.seq(
                set(e, coef(e, 0), b0),
                set(e, coef(e, 1), b1),
                set(e, coef(e, 2), b2),
                set(e, coef(e, 3), a1),
                set(e, coef(e, 4), a2));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        var fields = new TList<SourceExpr>();
        EcoBuzzVst.commonFields(e, fields, EcoBuzzVst.PARAMS);
        fields.add(EcoBuzz.field(e, "coef", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * 5))));
        fields.add(EcoBuzz.field(e, "fs", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * 2 * 4))));
        fields.add(EcoBuzz.field(e, "drive", e.v("t_f32")));
        fields.add(EcoBuzz.field(e, "use_main", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "use_fixed", e.v("t_i32")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "kfx_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        EcoBuzzVst.commonDefs(e, defs);
        defs.add(e.let("recalc_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));

        var body = new TList<SourceExpr>();
        body.add(e.let("sinf", EcoBuzz.externDef(e, "sinf", e.lowerV("f1_fn_t"), "m")));
        body.add(e.let("cosf", EcoBuzz.externDef(e, "cosf", e.lowerV("f1_fn_t"), "m")));
        body.add(e.let("sqrtf", EcoBuzz.externDef(e, "sqrtf", e.lowerV("f1_fn_t"), "m")));

        var recalc = new TList<SourceExpr>();
        recalc.add(e.let("cut", pv(e, 0)));
        recalc.add(e.let("res", pv(e, 1)));
        recalc.add(set(e, p(e, "drive"), pv(e, 2)));
        recalc.add(e.let("mode", EcoBuzz.min(e, i32(e, mul(e, pv(e, 3), e.n(MODES))), e.n(MODES - 1))));
        recalc.add(set(e, p(e, "use_main"), e.ifElse(gt(e, pv(e, 6), e.n(0)), i32(e, e.n(1)), i32(e, e.n(0)))));
        recalc.add(set(e, p(e, "use_fixed"), e.ifElse(gt(e, pv(e, 7), e.n(0)), i32(e, e.n(1)), i32(e, e.n(0)))));
        recalc.add(e.let("cut2", mul(e, e.v("cut"), e.v("cut"))));
        recalc.add(e.let(
                "f0",
                e.ifElse(
                        EcoBuzz.eq(e, e.v("mode"), e.n(MODE_RESONATOR)),
                        mul(e, e.v("cut2"), e.n(F0_RESONATOR_SQ)),
                        e.ifElse(
                                EcoBuzz.eq(e, e.v("mode"), e.n(MODE_RESONATOR + 1)),
                                add(e, mul(e, e.v("cut2"), e.n(F0_NOTCH_SQ)), e.n(F0_NOTCH_BASE)),
                                add(e, mul(e, e.v("cut2"), e.n(F0_SQ)), e.n(F0_BASE))))));
        recalc.add(e.let("w0", mul(e, e.v("f0"), e.n(TWO_PI / RATE))));
        recalc.add(e.let("cw", call(e, "cosf", e.v("w0"))));
        recalc.add(e.let("sw", call(e, "sinf", e.v("w0"))));
        recalc.add(e.let(
                "q",
                e.ifElse(
                        EcoBuzz.eq(e, e.v("mode"), e.n(MODE_RESONATOR + 1)),
                        f32(e, e.n(Q_NOTCH)),
                        add(e, mul(e, e.v("res"), e.n(Q_RES)), e.n(Q_BASE)))));
        recalc.add(e.let("alpha", div(e, e.v("sw"), mul(e, e.v("q"), e.n(2)))));
        recalc.add(e.let("a0", add(e, e.v("alpha"), e.n(1))));
        recalc.add(e.let("a1", div(e, mul(e, e.v("cw"), e.n(-2)), e.v("a0"))));
        recalc.add(e.let("a2", div(e, sub(e, f32(e, e.n(1)), e.v("alpha")), e.v("a0"))));
        var lowpass = e.seq(
                e.let("g", mul(e, add(e, add(e, e.v("a1"), e.v("a2")), e.n(1)), e.n(0.25))),
                setCoefs(e, e.v("g"), mul(e, e.v("g"), e.n(2)), e.v("g"), e.v("a1"), e.v("a2")));
        var highpass = e.seq(
                e.let("g", mul(e, sub(e, sub(e, e.v("a2"), e.v("a1")), e.n(-1)), e.n(-0.25))),
                setCoefs(e, e.v("g"), mul(e, e.v("g"), e.n(-2)), e.v("g"), e.v("a1"), e.v("a2")));
        var resonator = e.seq(
                e.let("r", mul(e, e.v("res"), e.n(RESONATOR_RADIUS))),
                setCoefs(
                        e,
                        sub(e, f32(e, e.n(1)), e.v("r")),
                        f32(e, e.n(0)),
                        mul(e, mul(e, e.v("r"), e.n(-1)), sub(e, f32(e, e.n(1)), e.v("r"))),
                        mul(e, mul(e, e.v("r"), e.v("cw")), e.n(-2)),
                        mul(e, e.v("r"), e.v("r"))));
        var notch = e.seq(
                e.let("bn", mul(e, add(e, e.v("a2"), e.n(1)), e.n(0.5))),
                setCoefs(
                        e,
                        sub(e, f32(e, e.n(1)), mul(e, e.v("res"), sub(e, f32(e, e.n(1)), e.v("bn")))),
                        e.v("a1"),
                        add(e, e.v("a2"), mul(e, e.v("res"), sub(e, e.v("bn"), e.v("a2")))),
                        e.v("a1"),
                        e.v("a2")));
        recalc.add(e.ifElse(
                EcoBuzz.eq(e, e.v("mode"), e.n(MODE_LOWPASS)),
                lowpass,
                e.ifElse(
                        EcoBuzz.eq(e, e.v("mode"), e.n(MODE_HIGHPASS)),
                        highpass,
                        e.ifElse(EcoBuzz.eq(e, e.v("mode"), e.n(MODE_RESONATOR)), resonator, notch))));
        body.add(e.let(
                "recalc",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "kfx_recalc",
                        new String[] {"p"},
                        e.lowerV("recalc_fn_t"),
                        e.seq(recalc.toArray(SourceExpr[]::new)))));

        var init = new TList<SourceExpr>();
        for (int k = 0; k < FIXED.size(); k++)
            init.add(set(e, idx(e, p(e, "coef"), e.n(COEF_FIXED + k)), e.n(FIXED.get(k))));
        init.add(forRange(e, "k", e.n(2 * 2 * 4), set(e, idx(e, p(e, "fs"), e.v("k")), e.n(0))));
        init.add(forRange(e, "k", e.n(EcoBuzzVst.PARAMS), set(e, idx(e, p(e, "pv"), e.v("k")), e.n(0))));
        init.add(set(e, idx(e, p(e, "pv"), e.n(0)), e.n(1)));
        init.add(set(e, idx(e, p(e, "pv"), e.n(6)), e.n(1)));
        init.add(call(e, "recalc", e.v("p")));
        init.add(EcoBuzzVst.resetVals(e));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "kfx_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        e.seq(init.toArray(SourceExpr[]::new)))));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "kfx_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        e.seq(
                                EcoBuzzVst.applyParamAutomation(e, EcoBuzzVst.PARAMS),
                                call(e, "recalc", e.v("p")),
                                EcoBuzzVst.resetVals(e)))));

        var i = e.v("i");
        var ch = e.v("ch");
        var sample = idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), ch));
        var chBody = new TList<SourceExpr>();
        chBody.add(e.let("x", sample));
        var main = new TList<SourceExpr>();
        main.addAll(EcoBuzzVst.biquadStep(e, i32(e, e.n(COEF_MAIN)), mul(e, ch, e.n(4)), e.v("x"), "y"));
        main.add(e.let("z", mul(e, e.v("y"), e.n(1.0 / EcoBuzz.BUZZ_SCALE))));
        main.add(e.let(
                "sat", div(e, mul(e, e.v("z"), e.n(2)), call(e, "sqrtf", add(e, mul(e, e.v("z"), e.v("z")), e.n(1))))));
        main.add(set(
                e,
                e.v("x"),
                mul(
                        e,
                        add(
                                e,
                                mul(e, e.v("z"), sub(e, f32(e, e.n(1)), p(e, "drive"))),
                                mul(e, e.v("sat"), p(e, "drive"))),
                        e.n(EcoBuzz.BUZZ_SCALE))));
        chBody.add(ifOnly(e, ne(e, p(e, "use_main"), e.n(0)), main.toArray(SourceExpr[]::new)));
        var fixed = new TList<SourceExpr>();
        fixed.addAll(EcoBuzzVst.biquadStep(
                e, i32(e, e.n(COEF_FIXED)), add(e, mul(e, ch, e.n(4)), e.n(STATE_FIXED)), e.v("x"), "yf"));
        fixed.add(set(e, e.v("x"), e.v("yf")));
        chBody.add(ifOnly(e, ne(e, p(e, "use_fixed"), e.n(0)), fixed.toArray(SourceExpr[]::new)));
        chBody.add(set(e, sample, e.v("x")));
        var work = forRange(e, "i", e.v("n"), forRange(e, "ch", e.n(2), chBody.toArray(SourceExpr[]::new)));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "kfx_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), work)));
        return EcoBuzz.machineModule(
                e, "karmafilter", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
