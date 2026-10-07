package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.call;
import static compilerxyz.main.tests.EcoBuzz.div;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.gt;
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

final class EcoBuzzClassicEq {
    private EcoBuzzClassicEq() {}

    static final int PARAMS = 23;
    static final int BANDS = 7;
    private static final int WARM_BANDS = 5;
    private static final int MAX_ORDER = 6;
    private static final int COEFS_PER_BAND = 2 * MAX_ORDER + 1;
    private static final int STATE_PER_BAND = 2 * MAX_ORDER;
    private static final int P_IN = 0;
    private static final int P_OUT = 2;
    private static final int P_BANDS = 4;
    private static final int P_WARM = 18;
    private static final int P_SATURATION = 19;
    private static final double GAIN_DB_SPAN = 40.0;
    private static final double WARM_K = 0.05;
    private static final double SLEW_MAX = 2.04 * EcoBuzz.BUZZ_SCALE;
    private static final IList<Double> COEFS = IList.of(
            0.0015558670361902113,
            0.0004252953160884742,
            -0.0021978632530261551,
            -0.00042394216407641398,
            0.00064064351506900197,
            0.0,
            0.0,
            -1.7215332235725196,
            0.033875795572978581,
            1.0998533610841084,
            -0.4121889494219374,
            0.0,
            0.0,
            0.0044654241081053554,
            -1.283045530521086e-06,
            -0.0044641398466966188,
            0.0,
            0.0,
            0.0,
            0.0,
            -1.9890220994865775,
            0.98910287177680134,
            0.0,
            0.0,
            0.0,
            0.0,
            0.013958095252026652,
            -0.0025599001820982106,
            -0.025305098779117574,
            0.0025564305928727524,
            0.011350473268715165,
            0.0,
            0.0,
            -2.1481461356593079,
            0.51211983676617712,
            1.4217724617635161,
            -0.78574333429121201,
            0.0,
            0.0,
            0.042601373480450032,
            -0.084521314195520794,
            -0.038006607015484008,
            0.15940883088805075,
            -0.042111379735252585,
            -0.074872214706540333,
            0.037501311021884608,
            -3.8722409703913128,
            4.7499355900232043,
            -0.22292449620582844,
            -4.1035788505230988,
            3.237698977398713,
            -0.78888060186639386,
            0.12204164528542048,
            -3.447204265479615e-06,
            -0.12203778827131867,
            0.0,
            0.0,
            0.0,
            0.0,
            -1.6327905021167222,
            0.70212367557070787,
            0.0,
            0.0,
            0.0,
            0.0,
            0.31050071876864421,
            -2.6898091259481649e-06,
            -0.31049924520429006,
            0.0,
            0.0,
            0.0,
            0.0,
            -0.71823108056777918,
            0.24213999965839059,
            0.0,
            0.0,
            0.0,
            0.0,
            0.50466119234471485,
            -1.2583813383808446e-06,
            -0.50466255736659393,
            0.0,
            0.0,
            0.0,
            0.0,
            1.0123567427692408,
            0.019370462532067246,
            0.0,
            0.0,
            0.0,
            0.0);

    private static SourceExpr p(EcoMb e, String field) {
        return EcoBuzzVst.p(e, field);
    }

    private static SourceExpr f64(EcoMb e, SourceExpr v) {
        return EcoBuzz.conv(e, "t_f64", v);
    }

    private static SourceExpr pv(EcoMb e, SourceExpr k) {
        return idx(e, p(e, "pv"), k);
    }

    private static SourceExpr pow10(EcoMb e, SourceExpr x) {
        return call(e, "pow", f64(e, e.n(10)), x);
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("coef_t", EcoBuzz.array(e, e.v("t_f64"), e.intOf(BANDS * COEFS_PER_BAND))));
        var fields = new TList<SourceExpr>();
        EcoBuzzVst.commonFields(e, fields, PARAMS);
        fields.add(EcoBuzz.field(e, "fs", EcoBuzz.array(e, e.v("t_f64"), e.intOf(2 * BANDS * STATE_PER_BAND))));
        fields.add(EcoBuzz.field(e, "gin", EcoBuzz.array(e, e.v("t_f64"), e.intOf(2))));
        fields.add(EcoBuzz.field(e, "gout", EcoBuzz.array(e, e.v("t_f64"), e.intOf(2))));
        fields.add(EcoBuzz.field(e, "bgain", EcoBuzz.array(e, e.v("t_f64"), e.intOf(2 * BANDS))));
        fields.add(EcoBuzz.field(e, "warm", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "sat", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "slew", EcoBuzz.array(e, e.v("t_f64"), e.intOf(2))));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "ceq_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        EcoBuzzVst.commonDefs(e, defs);
        defs.add(e.let("recalc_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("d2_fn_t", EcoBuzz.fnType(e, e.v("t_f64"), e.v("t_f64"), e.v("t_f64"))));

        var body = new TList<SourceExpr>();
        body.add(e.let("pow", EcoBuzz.externDef(e, "pow", e.lowerV("d2_fn_t"), "m")));
        body.add(e.let(
                "coef",
                e.call(
                        EcoBuzz.llvmL(e, "global_zero_fn"),
                        e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("ceq_coef")), e.p("ty", e.lowerV("coef_t"))))));
        var c = e.v("c");
        var b = e.v("b");
        var recalc = e.seq(
                forRange(
                        e,
                        "c",
                        e.n(2),
                        set(
                                e,
                                idx(e, p(e, "gin"), c),
                                pow10(
                                        e,
                                        mul(
                                                e,
                                                sub(e, f64(e, pv(e, add(e, c, e.n(P_IN)))), e.n(0.5)),
                                                e.n(GAIN_DB_SPAN / 20)))),
                        set(
                                e,
                                idx(e, p(e, "gout"), c),
                                pow10(
                                        e,
                                        mul(
                                                e,
                                                sub(e, f64(e, pv(e, add(e, c, e.n(P_OUT)))), e.n(0.5)),
                                                e.n(GAIN_DB_SPAN / 20)))),
                        forRange(
                                e,
                                "b",
                                e.n(BANDS),
                                set(
                                        e,
                                        idx(e, p(e, "bgain"), add(e, mul(e, c, e.n(BANDS)), b)),
                                        pow10(
                                                e,
                                                sub(
                                                        e,
                                                        f64(
                                                                e,
                                                                pv(
                                                                        e,
                                                                        add(
                                                                                e,
                                                                                add(e, mul(e, c, e.n(BANDS)), b),
                                                                                e.n(P_BANDS)))),
                                                        e.n(0.5)))))),
                set(
                        e,
                        p(e, "warm"),
                        e.ifElse(gt(e, pv(e, e.n(P_WARM)), e.n(0.5)), EcoBuzz.i32(e, e.n(1)), EcoBuzz.i32(e, e.n(0)))),
                set(
                        e,
                        p(e, "sat"),
                        e.ifElse(
                                gt(e, pv(e, e.n(P_SATURATION)), e.n(0.5)),
                                EcoBuzz.i32(e, e.n(1)),
                                EcoBuzz.i32(e, e.n(0)))));
        body.add(e.let(
                "recalc",
                EcoBuzz.funcDef(e, e.v("ll"), "ceq_recalc", new String[] {"p"}, e.lowerV("recalc_fn_t"), recalc)));

        var init = new TList<SourceExpr>();
        for (int k = 0; k < COEFS.size(); k++)
            init.add(set(e, idx(e, EcoBuzz.lw(e, "coef"), e.n(k)), e.n(COEFS.get(k))));
        init.add(forRange(e, "k", e.n(2 * BANDS * STATE_PER_BAND), set(e, idx(e, p(e, "fs"), e.v("k")), e.n(0))));
        init.add(forRange(e, "k", e.n(PARAMS), set(e, pv(e, e.v("k")), e.n(0))));
        init.add(forRange(e, "c", e.n(2), set(e, idx(e, p(e, "slew"), e.v("c")), e.n(0))));
        init.add(forRange(e, "k", e.n(P_WARM), set(e, pv(e, e.v("k")), e.n(0.5))));
        init.add(call(e, "recalc", e.v("p")));
        init.add(EcoBuzzVst.resetVals(e));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "ceq_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        e.seq(init.toArray(SourceExpr[]::new)))));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "ceq_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        e.seq(
                                EcoBuzzVst.applyParamAutomation(e, PARAMS),
                                call(e, "recalc", e.v("p")),
                                EcoBuzzVst.resetVals(e)))));

        var i = e.v("i");
        var k = e.v("k");
        var sample = idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), c));
        var cb = add(e, mul(e, c, e.n(BANDS)), b);
        var st = mul(e, cb, e.n(STATE_PER_BAND));
        var cf = mul(e, b, e.n(COEFS_PER_BAND));
        var xs = idx(e, p(e, "fs"), add(e, st, k));
        var ys = idx(e, p(e, "fs"), add(e, add(e, st, e.n(MAX_ORDER)), k));
        var bandBody = new TList<SourceExpr>();
        bandBody.add(e.let("y", mul(e, idx(e, EcoBuzz.lw(e, "coef"), cf), e.v("xin"))));
        bandBody.add(forRange(
                e,
                "k",
                e.n(MAX_ORDER),
                set(
                        e,
                        e.v("y"),
                        sub(
                                e,
                                add(
                                        e,
                                        e.v("y"),
                                        mul(e, idx(e, EcoBuzz.lw(e, "coef"), add(e, add(e, cf, e.n(1)), k)), xs)),
                                mul(
                                        e,
                                        idx(e, EcoBuzz.lw(e, "coef"), add(e, add(e, cf, e.n(MAX_ORDER + 1)), k)),
                                        ys)))));
        for (int o = MAX_ORDER - 1; o > 0; o--) {
            bandBody.add(set(e, idx(e, p(e, "fs"), add(e, st, e.n(o))), idx(e, p(e, "fs"), add(e, st, e.n(o - 1)))));
            bandBody.add(set(
                    e,
                    idx(e, p(e, "fs"), add(e, st, e.n(MAX_ORDER + o))),
                    idx(e, p(e, "fs"), add(e, st, e.n(MAX_ORDER + o - 1)))));
        }
        bandBody.add(set(e, idx(e, p(e, "fs"), st), e.v("xin")));
        bandBody.add(set(e, idx(e, p(e, "fs"), add(e, st, e.n(MAX_ORDER))), e.v("y")));
        bandBody.add(e.let("yg", mul(e, e.v("y"), idx(e, p(e, "bgain"), cb))));
        bandBody.add(set(e, e.v("full"), add(e, e.v("full"), e.v("yg"))));
        bandBody.add(ifOnly(e, lt(e, b, e.n(WARM_BANDS)), set(e, e.v("low"), add(e, e.v("low"), e.v("yg")))));
        var chBody = new TList<SourceExpr>();
        chBody.add(e.let("xin", mul(e, f64(e, sample), idx(e, p(e, "gin"), c))));
        chBody.add(e.let("full", f64(e, e.n(0))));
        chBody.add(e.let("low", f64(e, e.n(0))));
        chBody.add(forRange(e, "b", e.n(BANDS), bandBody.toArray(SourceExpr[]::new)));
        chBody.add(ifOnly(
                e,
                ne(e, p(e, "warm"), e.n(0)),
                e.let("mag", e.ifElse(lt(e, e.v("low"), e.n(0)), e.mb.operatorNeg(e.v("low")), e.v("low"))),
                set(
                        e,
                        e.v("full"),
                        sub(
                                e,
                                add(
                                        e,
                                        e.v("full"),
                                        div(
                                                e,
                                                e.v("low"),
                                                add(e, mul(e, e.v("mag"), e.n(WARM_K / EcoBuzz.BUZZ_SCALE)), e.n(1)))),
                                e.v("low")))));
        chBody.add(ifOnly(
                e,
                ne(e, p(e, "sat"), e.n(0)),
                e.let("step", sub(e, e.v("full"), idx(e, p(e, "slew"), c))),
                ifOnly(e, gt(e, e.v("step"), e.n(SLEW_MAX)), set(e, e.v("step"), f64(e, e.n(SLEW_MAX)))),
                ifOnly(e, lt(e, e.v("step"), e.n(-SLEW_MAX)), set(e, e.v("step"), f64(e, e.n(-SLEW_MAX)))),
                set(e, e.v("full"), add(e, idx(e, p(e, "slew"), c), e.v("step"))),
                set(e, idx(e, p(e, "slew"), c), e.v("full"))));
        chBody.add(set(e, sample, f32(e, mul(e, e.v("full"), idx(e, p(e, "gout"), c)))));
        var work = forRange(e, "i", e.v("n"), forRange(e, "c", e.n(2), chBody.toArray(SourceExpr[]::new)));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "ceq_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), work)));
        return EcoBuzz.machineModule(
                e, "classiceq", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
