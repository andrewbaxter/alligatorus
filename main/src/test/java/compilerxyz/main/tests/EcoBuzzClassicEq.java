package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.set;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzClassicEq {
    private EcoBuzzClassicEq() {}

    static final int STAGES = 7;
    private static final IList<Double> COEFS = IList.of(
            1.003226084,
            -1.993873536,
            0.9906984072,
            -1.993892983,
            0.9939050443,
            1.004077802,
            -1.993167993,
            0.9891907433,
            -1.993167993,
            0.9932685456,
            1.004909335,
            -1.977513708,
            0.9733008255,
            -1.977513708,
            0.9782101609,
            1.006492421,
            -1.898182582,
            0.9017829435,
            -1.898182582,
            0.9082753647,
            1.087565151,
            -1.739908285,
            0.727357796,
            -1.739908285,
            0.8149229468,
            1.156790772,
            -0.9357836493,
            0.3741910103,
            -0.9357836493,
            0.5309817828,
            1.280859928,
            -0.9631613034,
            0.2608283664,
            -0.5744141091,
            0.1529411002);

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        var fields = new TList<SourceExpr>();
        EcoBuzzVst.commonFields(e, fields);
        fields.add(EcoBuzz.field(e, "coef", EcoBuzz.array(e, e.v("t_f32"), e.intOf(STAGES * 5))));
        fields.add(EcoBuzz.field(e, "fs", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * STAGES * 4))));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "ceq_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        EcoBuzzVst.commonDefs(e, defs);

        var body = new TList<SourceExpr>();
        var init = new TList<SourceExpr>();
        for (int k = 0; k < COEFS.size(); k++) {
            init.add(set(e, idx(e, EcoBuzzVst.p(e, "coef"), e.n(k)), e.n(COEFS.get(k))));
        }
        init.add(forRange(e, "k", e.n(2 * STAGES * 4), set(e, idx(e, EcoBuzzVst.p(e, "fs"), e.v("k")), e.n(0))));
        init.add(forRange(e, "k", e.n(EcoBuzzVst.PARAMS), set(e, idx(e, EcoBuzzVst.p(e, "pv"), e.v("k")), e.n(0))));
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
                        e.seq(EcoBuzzVst.applyParamAutomation(e), EcoBuzzVst.resetVals(e)))));
        var i = e.v("i");
        var ch = e.v("ch");
        var stage = e.v("s");
        var sample = idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), ch));
        var stageBody = new TList<SourceExpr>();
        stageBody.addAll(EcoBuzzVst.biquadStep(
                e, mul(e, stage, e.n(5)), mul(e, add(e, mul(e, ch, e.n(STAGES)), stage), e.n(4)), e.v("x"), "y"));
        stageBody.add(set(e, e.v("x"), e.v("y")));
        var work = forRange(
                e,
                "i",
                e.v("n"),
                forRange(
                        e,
                        "ch",
                        e.n(2),
                        e.let("x", sample),
                        forRange(e, "s", e.n(STAGES), stageBody.toArray(SourceExpr[]::new)),
                        set(e, sample, e.v("x"))));
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
