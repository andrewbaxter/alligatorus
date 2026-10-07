package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.call;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.ge;
import static compilerxyz.main.tests.EcoBuzz.gt;
import static compilerxyz.main.tests.EcoBuzz.i32;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.ifOnly;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzBlueCatPhaser {
    private EcoBuzzBlueCatPhaser() {}

    static final int MAX_STAGES = 32;
    private static final double RATE = 44100.0;
    private static final double TWO_PI = 2 * Math.PI;
    private static final double GAIN_DB_MIN = -20.0;
    private static final double GAIN_DB_SPAN = 40.0;
    private static final double RATE_HZ_MAX = 20.0;
    private static final double ACTIVE_RELEASE = 6.3;
    private static final double ACTIVE_THRESHOLD = 1e-6 * EcoBuzz.BUZZ_SCALE;
    private static final int COEF_HOLD = 3;
    private static final IList<String> F32_FIELDS =
            IList.of("gain", "dry", "wet", "fb", "depth", "phase_inc", "phase_r", "a_held");
    private static final IList<String> I32_FIELDS = IList.of("stages", "bypass", "pos", "active_until", "tri");

    private static SourceExpr p(EcoMb e, String field) {
        return EcoBuzzVst.p(e, field);
    }

    private static SourceExpr pv(EcoMb e, int k) {
        return idx(e, p(e, "pv"), e.n(k));
    }

    private static SourceExpr stage(EcoMb e, String field, SourceExpr c, SourceExpr k) {
        return idx(e, p(e, field), add(e, mul(e, c, e.n(MAX_STAGES)), k));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        var fields = new TList<SourceExpr>();
        EcoBuzzVst.commonFields(e, fields, EcoBuzzVst.PARAMS);
        for (var f : F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_f32")));
        for (var f : I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "phase", e.v("t_f64")));
        fields.add(EcoBuzz.field(e, "zprev", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2))));
        fields.add(EcoBuzz.field(e, "ap_x", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * MAX_STAGES))));
        fields.add(EcoBuzz.field(e, "ap_y", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * MAX_STAGES))));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "bcp_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        EcoBuzzVst.commonDefs(e, defs);
        defs.add(e.let("recalc_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("f2_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"), e.v("t_f32"))));

        var body = new TList<SourceExpr>();
        body.add(e.let("sinf", EcoBuzz.externDef(e, "sinf", e.lowerV("f1_fn_t"), "m")));
        body.add(e.let("powf", EcoBuzz.externDef(e, "powf", e.lowerV("f2_fn_t"), "m")));
        var recalc = e.seq(
                set(
                        e,
                        p(e, "bypass"),
                        e.ifElse(gt(e, pv(e, 0), e.n(0.5)), EcoBuzz.i32(e, e.n(1)), EcoBuzz.i32(e, e.n(0)))),
                set(
                        e,
                        p(e, "gain"),
                        call(
                                e,
                                "powf",
                                f32(e, e.n(10)),
                                mul(e, add(e, mul(e, pv(e, 1), e.n(GAIN_DB_SPAN)), e.n(GAIN_DB_MIN)), e.n(1.0 / 20)))),
                set(e, p(e, "dry"), pv(e, 2)),
                set(e, p(e, "wet"), mul(e, sub(e, pv(e, 3), e.n(0.5)), e.n(2))),
                set(e, p(e, "fb"), mul(e, sub(e, pv(e, 4), e.n(0.5)), e.n(2))),
                set(e, p(e, "stages"), i32(e, add(e, mul(e, pv(e, 5), e.n(MAX_STAGES)), e.n(0.5)))),
                set(e, p(e, "depth"), pv(e, 6)),
                set(e, p(e, "phase_inc"), mul(e, pv(e, 7), e.n(RATE_HZ_MAX / RATE))),
                set(e, p(e, "phase_r"), pv(e, 9)),
                set(e, p(e, "tri"), e.ifElse(ge(e, pv(e, 8), e.n(1)), EcoBuzz.i32(e, e.n(1)), EcoBuzz.i32(e, e.n(0)))));
        body.add(e.let(
                "recalc",
                EcoBuzz.funcDef(e, e.v("ll"), "bcp_recalc", new String[] {"p"}, e.lowerV("recalc_fn_t"), recalc)));

        var init = new TList<SourceExpr>();
        init.add(forRange(e, "k", e.n(EcoBuzzVst.PARAMS), set(e, idx(e, p(e, "pv"), e.v("k")), e.n(0))));
        init.add(set(e, pv(e, 1), e.n(0.5)));
        init.add(set(e, pv(e, 2), e.n(1)));
        init.add(set(e, pv(e, 3), e.n(1)));
        init.add(set(e, pv(e, 4), e.n(0.5)));
        init.add(set(e, pv(e, 5), e.n(0.25)));
        init.add(set(e, pv(e, 6), e.n(0.5)));
        init.add(set(e, pv(e, 7), e.n(0.05)));
        init.add(set(e, p(e, "phase"), e.n(0)));
        init.add(set(e, p(e, "a_held"), e.n(0)));
        init.add(set(e, p(e, "pos"), e.n(0)));
        init.add(set(e, p(e, "active_until"), e.n((int) (ACTIVE_RELEASE * RATE))));
        init.add(forRange(e, "c", e.n(2), set(e, idx(e, p(e, "zprev"), e.v("c")), e.n(0))));
        init.add(forRange(
                e,
                "k",
                e.n(2 * MAX_STAGES),
                set(e, idx(e, p(e, "ap_x"), e.v("k")), e.n(0)),
                set(e, idx(e, p(e, "ap_y"), e.v("k")), e.n(0))));
        init.add(call(e, "recalc", e.v("p")));
        init.add(EcoBuzzVst.resetVals(e));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bcp_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        e.seq(init.toArray(SourceExpr[]::new)))));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bcp_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        e.seq(
                                EcoBuzzVst.applyParamAutomation(e, EcoBuzzVst.PARAMS),
                                call(e, "recalc", e.v("p")),
                                EcoBuzzVst.resetVals(e)))));

        var i = e.v("i");
        var c = e.v("c");
        var k = e.v("k");
        var sample = idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), c));
        var chBody = new TList<SourceExpr>();
        chBody.add(e.let("x", sample));
        chBody.add(e.let("ph", add(e, p(e, "phase"), EcoBuzz.conv(e, "t_f64", p(e, "phase_r")))));
        chBody.add(ifOnly(e, ge(e, e.v("ph"), e.n(1)), set(e, e.v("ph"), sub(e, e.v("ph"), e.n(1)))));
        chBody.add(ifOnly(
                e,
                EcoBuzz.eq(
                        e,
                        sub(e, p(e, "pos"), mul(e, EcoBuzz.div(e, p(e, "pos"), e.n(COEF_HOLD)), e.n(COEF_HOLD))),
                        e.n(0)),
                e.let(
                        "lfo",
                        e.ifElse(
                                EcoBuzz.ne(e, p(e, "tri"), e.n(0)),
                                f32(
                                        e,
                                        e.ifElse(
                                                EcoBuzz.lt(e, e.v("ph"), e.n(0.25)),
                                                mul(e, e.v("ph"), e.n(4)),
                                                e.ifElse(
                                                        EcoBuzz.lt(e, e.v("ph"), e.n(0.75)),
                                                        sub(e, mul(e, e.v("ph"), e.n(-4)), e.n(-2)),
                                                        sub(e, mul(e, e.v("ph"), e.n(4)), e.n(4))))),
                                call(e, "sinf", f32(e, mul(e, e.v("ph"), e.n(TWO_PI)))))),
                set(e, p(e, "a_held"), mul(e, e.v("lfo"), e.mb.operatorNeg(p(e, "depth"))))));
        chBody.add(e.let("a", p(e, "a_held")));
        chBody.add(e.let("u", add(e, e.v("x"), mul(e, idx(e, p(e, "zprev"), c), p(e, "fb")))));
        chBody.add(forRange(
                e,
                "k",
                p(e, "stages"),
                e.let("y", add(e, mul(e, sub(e, e.v("u"), stage(e, "ap_y", c, k)), e.v("a")), stage(e, "ap_x", c, k))),
                set(e, stage(e, "ap_x", c, k), e.v("u")),
                set(e, stage(e, "ap_y", c, k), e.v("y")),
                set(e, e.v("u"), e.v("y"))));
        chBody.add(set(e, idx(e, p(e, "zprev"), c), e.v("u")));
        chBody.add(set(
                e, sample, mul(e, add(e, mul(e, e.v("x"), p(e, "dry")), mul(e, e.v("u"), p(e, "wet"))), p(e, "gain"))));
        var work = ifOnly(
                e,
                EcoBuzz.eq(e, p(e, "bypass"), e.n(0)),
                e.let("active", EcoBuzz.i32(e, e.n(0))),
                forRange(
                        e,
                        "i",
                        mul(e, e.v("n"), e.n(2)),
                        e.let("xs", idx(e, e.v("out"), i)),
                        ifOnly(
                                e,
                                EcoBuzz.or(
                                        e,
                                        gt(e, e.v("xs"), e.n(ACTIVE_THRESHOLD)),
                                        EcoBuzz.lt(e, e.v("xs"), e.n(-ACTIVE_THRESHOLD))),
                                set(e, e.v("active"), e.n(1)))),
                ifOnly(
                        e,
                        EcoBuzz.ne(e, e.v("active"), e.n(0)),
                        set(e, p(e, "active_until"), add(e, p(e, "pos"), e.n((int) (ACTIVE_RELEASE * RATE))))),
                forRange(
                        e,
                        "i",
                        e.v("n"),
                        ifOnly(
                                e,
                                EcoBuzz.lt(e, p(e, "pos"), p(e, "active_until")),
                                set(
                                        e,
                                        p(e, "phase"),
                                        add(e, p(e, "phase"), EcoBuzz.conv(e, "t_f64", p(e, "phase_inc")))),
                                ifOnly(
                                        e,
                                        ge(e, p(e, "phase"), e.n(1)),
                                        set(e, p(e, "phase"), sub(e, p(e, "phase"), e.n(1))))),
                        forRange(e, "c", e.n(2), chBody.toArray(SourceExpr[]::new)),
                        set(e, p(e, "pos"), add(e, p(e, "pos"), e.n(1)))));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "bcp_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), work)));
        return EcoBuzz.machineModule(
                e, "bluecatphaser", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
