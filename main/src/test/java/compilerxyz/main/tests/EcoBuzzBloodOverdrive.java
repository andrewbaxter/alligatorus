package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.and;
import static compilerxyz.main.tests.EcoBuzz.call;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.gt;
import static compilerxyz.main.tests.EcoBuzz.i32;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.ifOnly;
import static compilerxyz.main.tests.EcoBuzz.lt;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzBloodOverdrive {
    private EcoBuzzBloodOverdrive() {}

    private static final double SMOOTH = 0.99975;
    private static final double SMOOTH_IN = 0.00025;
    private static final double SVF2_F = 0.25;
    private static final double SHAPER_STEP = Math.PI / 65536;
    private static final double OUT_CLAMP = 0.99995;
    private static final double DENORMAL = 5.9604645e-8;
    private static final IList<String> F32_FIELDS =
            IList.of("p0", "p1", "p2", "p0_t", "p1_t", "p2_t", "f_t", "k", "k2");
    private static final IList<String> CH_FIELDS =
            IList.of("f", "low", "band", "high", "low2", "band2", "high2", "y1", "y2");
    private static final IList<String> FLUSHED = IList.of("low", "band", "high", "low2", "band2", "high2", "y1", "y2");

    private static SourceExpr p(EcoMb e, String field) {
        return EcoBuzzVst.p(e, field);
    }

    private static SourceExpr ch(EcoMb e, String field, SourceExpr c) {
        return idx(e, p(e, field), c);
    }

    private static SourceExpr pv(EcoMb e, int k) {
        return idx(e, p(e, "pv"), e.n(k));
    }

    private static SourceExpr cube(EcoMb e, SourceExpr v) {
        return mul(e, mul(e, v, v), v);
    }

    private static SourceExpr clamp(EcoMb e, SourceExpr v, double limit) {
        return e.ifElse(
                lt(e, v, e.n(-limit)), f32(e, e.n(-limit)), e.ifElse(gt(e, v, e.n(limit)), f32(e, e.n(limit)), v));
    }

    private static SourceExpr shaped(EcoMb e, SourceExpr v) {
        return call(e, "sinf", mul(e, f32(e, i32(e, mul(e, clamp(e, v, 1.0), e.n(32767)))), e.n(SHAPER_STEP)));
    }

    private static SourceExpr smooth(EcoMb e, SourceExpr place, SourceExpr target) {
        return set(e, place, add(e, mul(e, place, e.n(SMOOTH)), target));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        var fields = new TList<SourceExpr>();
        EcoBuzzVst.commonFields(e, fields, EcoBuzzVst.PARAMS);
        for (var f : F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_f32")));
        for (var f : CH_FIELDS) fields.add(EcoBuzz.field(e, f, EcoBuzz.array(e, e.v("t_f32"), e.intOf(2))));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "bod_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        EcoBuzzVst.commonDefs(e, defs);
        defs.add(e.let("recalc_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));

        var body = new TList<SourceExpr>();
        body.add(e.let("sinf", EcoBuzz.externDef(e, "sinf", e.lowerV("f1_fn_t"), "m")));
        var recalc = e.seq(
                set(e, p(e, "f_t"), mul(e, cube(e, pv(e, 1)), e.n(SMOOTH_IN))),
                e.let("drive", e.ifElse(gt(e, pv(e, 3), e.n(0.5)), f32(e, e.n(2500)), f32(e, e.n(25)))),
                set(e, p(e, "p0_t"), mul(e, add(e, mul(e, cube(e, pv(e, 2)), e.v("drive")), e.n(1)), e.n(SMOOTH_IN))),
                set(e, p(e, "p1_t"), mul(e, sub(e, f32(e, e.n(1)), pv(e, 0)), e.n(SMOOTH_IN))),
                set(e, p(e, "p2_t"), mul(e, mul(e, pv(e, 5), pv(e, 5)), e.n(SMOOTH_IN))),
                set(e, p(e, "k2"), add(e, mul(e, pv(e, 4), e.n(0.7)), e.n(0.2))),
                set(e, p(e, "k"), sub(e, f32(e, e.n(1)), p(e, "k2"))));
        body.add(e.let(
                "recalc",
                EcoBuzz.funcDef(e, e.v("ll"), "bod_recalc", new String[] {"p"}, e.lowerV("recalc_fn_t"), recalc)));

        var init = new TList<SourceExpr>();
        init.add(forRange(e, "k", e.n(EcoBuzzVst.PARAMS), set(e, idx(e, p(e, "pv"), e.v("k")), e.n(0))));
        init.add(set(e, pv(e, 1), e.n(0.5)));
        init.add(set(e, pv(e, 3), e.n(0.33)));
        init.add(set(e, pv(e, 5), e.n(1)));
        for (var f : new String[] {"p0", "p1", "p2"}) init.add(set(e, p(e, f), e.n(1)));
        init.add(forRange(e, "c", e.n(2), set(e, ch(e, "f", e.v("c")), e.n(0))));
        for (var f : FLUSHED) init.add(forRange(e, "c", e.n(2), set(e, ch(e, f, e.v("c")), e.n(0))));
        init.add(call(e, "recalc", e.v("p")));
        init.add(EcoBuzzVst.resetVals(e));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bod_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        e.seq(init.toArray(SourceExpr[]::new)))));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bod_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        e.seq(
                                EcoBuzzVst.applyParamAutomation(e, EcoBuzzVst.PARAMS),
                                call(e, "recalc", e.v("p")),
                                EcoBuzzVst.resetVals(e)))));

        var i = e.v("i");
        var c = e.v("c");
        var sample = idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), c));
        var chBody = new TList<SourceExpr>();
        chBody.add(e.let("x", mul(e, sample, e.n(1.0 / EcoBuzz.BUZZ_SCALE))));
        chBody.add(smooth(e, ch(e, "f", c), p(e, "f_t")));
        chBody.add(e.let("fc", ch(e, "f", c)));
        chBody.add(set(e, ch(e, "low", c), add(e, ch(e, "low", c), mul(e, e.v("fc"), ch(e, "band", c)))));
        chBody.add(set(e, ch(e, "high", c), sub(e, sub(e, e.v("x"), ch(e, "low", c)), ch(e, "band", c))));
        chBody.add(set(e, ch(e, "band", c), add(e, ch(e, "band", c), mul(e, e.v("fc"), ch(e, "high", c)))));
        chBody.add(e.let(
                "u",
                mul(
                        e,
                        add(e, mul(e, add(e, ch(e, "high", c), ch(e, "low", c)), p(e, "p1")), ch(e, "band", c)),
                        p(e, "p0"))));
        chBody.add(set(e, ch(e, "low2", c), add(e, ch(e, "low2", c), mul(e, ch(e, "band2", c), e.n(SVF2_F)))));
        chBody.add(set(e, ch(e, "high2", c), sub(e, sub(e, e.v("u"), ch(e, "low2", c)), ch(e, "band2", c))));
        chBody.add(set(e, ch(e, "band2", c), add(e, ch(e, "band2", c), mul(e, ch(e, "high2", c), e.n(SVF2_F)))));
        chBody.add(e.let(
                "s",
                add(
                        e,
                        add(e, shaped(e, ch(e, "low2", c)), shaped(e, ch(e, "band2", c))),
                        shaped(e, ch(e, "high2", c)))));
        chBody.add(e.let("v", clamp(e, mul(e, e.v("s"), p(e, "p2")), OUT_CLAMP)));
        chBody.add(set(e, ch(e, "y1", c), add(e, mul(e, e.v("v"), p(e, "k")), mul(e, ch(e, "y1", c), p(e, "k2")))));
        chBody.add(
                set(e, ch(e, "y2", c), add(e, mul(e, ch(e, "y1", c), p(e, "k")), mul(e, ch(e, "y2", c), p(e, "k2")))));
        chBody.add(set(e, sample, mul(e, ch(e, "y2", c), e.n(EcoBuzz.BUZZ_SCALE))));
        var work = new TList<SourceExpr>();
        for (var f : FLUSHED) {
            var place = ch(e, f, e.v("c"));
            work.add(forRange(
                    e,
                    "c",
                    e.n(2),
                    ifOnly(
                            e,
                            and(e, gt(e, place, e.n(-DENORMAL)), lt(e, place, e.n(DENORMAL))),
                            set(e, place, e.n(0)))));
        }
        work.add(forRange(
                e,
                "i",
                e.v("n"),
                smooth(e, p(e, "p0"), p(e, "p0_t")),
                smooth(e, p(e, "p1"), p(e, "p1_t")),
                smooth(e, p(e, "p2"), p(e, "p2_t")),
                forRange(e, "c", e.n(2), chBody.toArray(SourceExpr[]::new))));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bod_work",
                        new String[] {"p", "out", "n"},
                        e.lowerV("work_fn_t"),
                        e.seq(work.toArray(SourceExpr[]::new)))));
        return EcoBuzz.machineModule(
                e,
                "bloodoverdrive",
                defs,
                new String[] {"init", "tick", "work"},
                new TList<>(),
                body,
                new SourceExpr[] {e.v("init"), e.v("tick"), e.v("work")});
    }
}
