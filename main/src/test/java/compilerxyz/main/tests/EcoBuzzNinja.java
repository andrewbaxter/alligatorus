package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.call;
import static compilerxyz.main.tests.EcoBuzz.div;
import static compilerxyz.main.tests.EcoBuzz.eq;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.fld;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.ge;
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

final class EcoBuzzNinja {
    private EcoBuzzNinja() {}

    static final int NG = 1;
    static final int NT = 7;
    static final int TRACKS = 8;
    private static final int MAX_DELAY = 44100;
    private static final double RATE = 44100.0;
    private static final double PI = Math.PI;
    private static final IList<String> T_I32_FIELDS =
            IList.of("t_length", "t_pos", "t_unit", "t_ftype", "t_fcut", "t_fbw");
    private static final IList<String> T_F32_FIELDS = IList.of("t_feedback", "t_wetout");

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pt(EcoMb e, String field, SourceExpr t) {
        return idx(e, fld(e, e.v("p"), field), t);
    }

    private static SourceExpr tval(EcoMb e, SourceExpr t, int k) {
        return idx(e, idx(e, p(e, "tval"), t), e.n(k));
    }

    private static SourceExpr coef(EcoMb e, SourceExpr t, int k) {
        return idx(e, p(e, "f_a"), add(e, mul(e, t, e.n(5)), e.n(k)));
    }

    private static SourceExpr fstate(EcoMb e, String which, SourceExpr t, SourceExpr ch, int k) {
        return idx(e, p(e, which), add(e, add(e, mul(e, t, e.n(4)), mul(e, ch, e.n(2))), e.n(k)));
    }

    private static SourceExpr resetVals(EcoMb e) {
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

    private static SourceExpr setCoefs(EcoMb e, SourceExpr t, SourceExpr... values) {
        var out = new TList<SourceExpr>();
        for (int k = 0; k < 5; k++) out.add(set(e, coef(e, t, k), values[k]));
        return e.seq(out.toArray(SourceExpr[]::new));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("i32xt", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS))));
        defs.add(e.let("f32xt", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "spt", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        fields.add(EcoBuzz.field(e, "drythru", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "aux", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * EcoBuzz.MAX_BLOCK))));
        fields.add(EcoBuzz.field(e, "f_a", EcoBuzz.array(e, e.v("t_f32"), e.intOf(5 * TRACKS))));
        fields.add(EcoBuzz.field(e, "f_i", EcoBuzz.array(e, e.v("t_f32"), e.intOf(4 * TRACKS))));
        fields.add(EcoBuzz.field(e, "f_o", EcoBuzz.array(e, e.v("t_f32"), e.intOf(4 * TRACKS))));
        fields.add(EcoBuzz.field(e, "buf", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * TRACKS * MAX_DELAY))));
        fields.add(EcoBuzz.field(e, "dc_x1", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2))));
        fields.add(EcoBuzz.field(e, "dc_y1", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2))));
        for (var f : T_I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32xt")));
        for (var f : T_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xt")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "nj_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("pi_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));

        var body = new TList<SourceExpr>();
        for (var fn : new String[] {"sinf", "cosf", "tanf"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, fn, e.lowerV("f1_fn_t"), "m")));
        }
        SourceExpr setFilterSeq;
        {
            var t = e.v("t");
            var f = f32(e, pt(e, "t_fcut", t));
            var bw = f32(e, pt(e, "t_fbw", t));
            var one = f32(e, e.n(1));
            var rbj = new TList<SourceExpr>();
            rbj.add(e.let("w0", mul(e, f, e.n(2 * PI / RATE))));
            rbj.add(e.let("cs", call(e, "cosf", e.v("w0"))));
            rbj.add(e.let("alpha", mul(e, call(e, "sinf", e.v("w0")), e.n(Math.sqrt(2.0) / 2))));
            rbj.add(e.let("a0", add(e, e.v("alpha"), e.n(1))));
            rbj.add(e.let("a1", div(e, mul(e, e.v("cs"), e.n(-2)), e.v("a0"))));
            rbj.add(e.let("a2", div(e, sub(e, one, e.v("alpha")), e.v("a0"))));
            rbj.add(ifOnly(
                    e,
                    eq(e, pt(e, "t_ftype", t), e.n(0)),
                    e.let("lb1", div(e, sub(e, one, e.v("cs")), e.v("a0"))),
                    setCoefs(
                            e,
                            t,
                            mul(e, e.v("lb1"), e.n(0.5)),
                            e.v("lb1"),
                            mul(e, e.v("lb1"), e.n(0.5)),
                            e.v("a1"),
                            e.v("a2"))));
            rbj.add(ifOnly(
                    e,
                    eq(e, pt(e, "t_ftype", t), e.n(1)),
                    e.let("hb1", div(e, add(e, e.v("cs"), e.n(1)), e.v("a0"))),
                    setCoefs(
                            e,
                            t,
                            mul(e, e.v("hb1"), e.n(0.5)),
                            e.mb.operatorNeg(e.v("hb1")),
                            mul(e, e.v("hb1"), e.n(0.5)),
                            e.v("a1"),
                            e.v("a2"))));
            rbj.add(ifOnly(
                    e,
                    eq(e, pt(e, "t_ftype", t), e.n(2)),
                    e.let("ta", call(e, "tanf", mul(e, bw, e.n(PI / RATE)))),
                    e.let("tb", div(e, one, e.v("ta"))),
                    e.let("td", call(e, "cosf", mul(e, f, e.n(2 * PI / RATE)))),
                    e.let("g", div(e, one, add(e, e.v("tb"), e.n(1)))),
                    setCoefs(
                            e,
                            t,
                            e.v("g"),
                            f32(e, e.n(0)),
                            e.mb.operatorNeg(e.v("g")),
                            e.mb.operatorNeg(mul(e, mul(e, mul(e, e.v("td"), e.n(2)), e.v("g")), e.v("tb"))),
                            mul(e, sub(e, e.v("tb"), e.n(1)), e.v("g")))));
            rbj.add(ifOnly(
                    e,
                    eq(e, pt(e, "t_ftype", t), e.n(3)),
                    e.let("ra", call(e, "tanf", mul(e, bw, e.n(PI / RATE)))),
                    e.let("rc", call(e, "cosf", mul(e, f, e.n(2 * PI / RATE)))),
                    e.let("rg", div(e, one, add(e, e.v("ra"), e.n(1)))),
                    e.let("r1", mul(e, mul(e, e.v("rc"), e.n(-2)), e.v("rg"))),
                    setCoefs(
                            e,
                            t,
                            e.v("rg"),
                            e.v("r1"),
                            e.v("rg"),
                            e.v("r1"),
                            mul(e, sub(e, one, e.v("ra")), e.v("rg")))));
            setFilterSeq = e.seq(rbj.toArray(SourceExpr[]::new));
        }
        body.add(e.let(
                "setfilter",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "nj_setfilter", new String[] {"p", "t"}, e.lowerV("pi_fn_t"), setFilterSeq)));

        var t = e.v("t");
        var init = e.seq(
                set(e, p(e, "drythru"), e.n(1)),
                forRange(e, "i", e.n(2 * TRACKS * MAX_DELAY), set(e, idx(e, p(e, "buf"), e.v("i")), e.n(0))),
                forRange(e, "i", e.n(4 * TRACKS), set(e, idx(e, p(e, "f_i"), e.v("i")), e.n(0))),
                forRange(e, "i", e.n(4 * TRACKS), set(e, idx(e, p(e, "f_o"), e.v("i")), e.n(0))),
                forRange(
                        e,
                        "c",
                        e.n(2),
                        set(e, idx(e, p(e, "dc_x1"), e.v("c")), e.n(0)),
                        set(e, idx(e, p(e, "dc_y1"), e.v("c")), e.n(0))),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        set(e, pt(e, "t_unit", t), e.n(0)),
                        set(e, pt(e, "t_length", t), mul(e, p(e, "spt"), e.n(3))),
                        set(e, pt(e, "t_pos", t), e.n(0)),
                        set(e, pt(e, "t_feedback", t), e.n(0.3)),
                        set(e, pt(e, "t_wetout", t), e.ifElse(eq(e, t, e.n(0)), f32(e, e.n(0.3)), f32(e, e.n(0)))),
                        set(e, pt(e, "t_ftype", t), e.n(1)),
                        set(e, pt(e, "t_fcut", t), e.n(1000)),
                        set(e, pt(e, "t_fbw", t), e.n(1000)),
                        call(e, "setfilter", e.v("p"), t)),
                resetVals(e));
        body.add(e.let(
                "init", EcoBuzz.funcDef(e, e.v("ll"), "nj_init", new String[] {"p"}, e.lowerV("init_fn_t"), init)));

        var len = e.v("len");
        var tick = e.seq(
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        e.let("recalc", EcoBuzz.i32(e, e.n(0))),
                        e.let("ft", tval(e, t, 3)),
                        ifOnly(
                                e,
                                ne(e, e.v("ft"), e.n(-1)),
                                set(e, pt(e, "t_ftype", t), e.v("ft")),
                                set(e, e.v("recalc"), e.n(1))),
                        e.let("fc", tval(e, t, 4)),
                        ifOnly(
                                e,
                                ne(e, e.v("fc"), e.n(-1)),
                                set(e, pt(e, "t_fcut", t), e.v("fc")),
                                set(e, e.v("recalc"), e.n(1))),
                        e.let("fb", tval(e, t, 5)),
                        ifOnly(
                                e,
                                ne(e, e.v("fb"), e.n(-1)),
                                set(e, pt(e, "t_fbw", t), e.v("fb")),
                                set(e, e.v("recalc"), e.n(1))),
                        ifOnly(e, ne(e, e.v("recalc"), e.n(0)), call(e, "setfilter", e.v("p"), t)),
                        e.let("un", tval(e, t, 1)),
                        ifOnly(e, ne(e, e.v("un"), e.n(-1)), set(e, pt(e, "t_unit", t), e.v("un"))),
                        e.let("len", tval(e, t, 0)),
                        ifOnly(
                                e,
                                ne(e, len, e.n(-1)),
                                e.let("unit", pt(e, "t_unit", t)),
                                ifOnly(
                                        e,
                                        eq(e, e.v("unit"), e.n(1)),
                                        e.let("ms", i32(e, mul(e, f32(e, len), e.n(RATE / 1000)))),
                                        set(
                                                e,
                                                pt(e, "t_length", t),
                                                e.ifElse(lt(e, e.v("ms"), e.n(1)), EcoBuzz.i32(e, e.n(1)), e.v("ms")))),
                                ifOnly(e, eq(e, e.v("unit"), e.n(2)), set(e, pt(e, "t_length", t), len)),
                                ifOnly(
                                        e,
                                        eq(e, e.v("unit"), e.n(0)),
                                        set(e, pt(e, "t_length", t), mul(e, p(e, "spt"), len))),
                                ifOnly(
                                        e,
                                        eq(e, e.v("unit"), e.n(3)),
                                        e.let("l256", e.mb.operatorShr(mul(e, p(e, "spt"), len), e.n(8))),
                                        set(
                                                e,
                                                pt(e, "t_length", t),
                                                e.ifElse(
                                                        lt(e, e.v("l256"), e.n(1)),
                                                        EcoBuzz.i32(e, e.n(1)),
                                                        e.v("l256"))))),
                        ifOnly(
                                e,
                                gt(e, pt(e, "t_length", t), e.n(MAX_DELAY)),
                                set(e, pt(e, "t_length", t), e.n(MAX_DELAY))),
                        ifOnly(e, ge(e, pt(e, "t_pos", t), pt(e, "t_length", t)), set(e, pt(e, "t_pos", t), e.n(0))),
                        e.let("fbk", tval(e, t, 2)),
                        ifOnly(
                                e,
                                ne(e, e.v("fbk"), e.n(-1)),
                                set(
                                        e,
                                        pt(e, "t_feedback", t),
                                        mul(e, f32(e, sub(e, e.v("fbk"), e.n(64))), e.n(1.0 / 64)))),
                        e.let("wo", tval(e, t, 6)),
                        ifOnly(
                                e,
                                ne(e, e.v("wo"), e.n(-1)),
                                set(e, pt(e, "t_wetout", t), mul(e, f32(e, e.v("wo")), e.n(1.0 / 128))))),
                e.let("dt", idx(e, p(e, "gval"), e.n(0))),
                ifOnly(
                        e,
                        ne(e, e.v("dt"), e.n(-1)),
                        set(
                                e,
                                p(e, "drythru"),
                                e.ifElse(ne(e, e.v("dt"), e.n(0)), EcoBuzz.i32(e, e.n(1)), EcoBuzz.i32(e, e.n(0))))),
                resetVals(e));
        body.add(e.let(
                "tick", EcoBuzz.funcDef(e, e.v("ll"), "nj_tick", new String[] {"p"}, e.lowerV("tick_fn_t"), tick)));

        var i = e.v("i");
        var ch = e.v("ch");
        var si = add(e, mul(e, i, e.n(2)), ch);
        var bufIdx = add(e, mul(e, add(e, mul(e, t, e.n(2)), ch), e.n(MAX_DELAY)), pt(e, "t_pos", t));
        var work = e.seq(
                forRange(e, "i", mul(e, e.v("n"), e.n(2)), set(e, idx(e, p(e, "aux"), i), idx(e, e.v("out"), i))),
                ifOnly(
                        e,
                        eq(e, p(e, "drythru"), e.n(0)),
                        forRange(e, "i", mul(e, e.v("n"), e.n(2)), set(e, idx(e, e.v("out"), i), e.n(0)))),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        forRange(
                                e,
                                "i",
                                e.v("n"),
                                forRange(
                                        e,
                                        "ch",
                                        e.n(2),
                                        e.let("delay", idx(e, p(e, "buf"), bufIdx)),
                                        e.let(
                                                "o",
                                                sub(
                                                        e,
                                                        sub(
                                                                e,
                                                                add(
                                                                        e,
                                                                        add(
                                                                                e,
                                                                                mul(e, coef(e, t, 0), e.v("delay")),
                                                                                mul(
                                                                                        e,
                                                                                        coef(e, t, 1),
                                                                                        fstate(e, "f_i", t, ch, 0))),
                                                                        mul(
                                                                                e,
                                                                                coef(e, t, 2),
                                                                                fstate(e, "f_i", t, ch, 1))),
                                                                mul(e, coef(e, t, 3), fstate(e, "f_o", t, ch, 0))),
                                                        mul(e, coef(e, t, 4), fstate(e, "f_o", t, ch, 1)))),
                                        set(e, fstate(e, "f_o", t, ch, 1), fstate(e, "f_o", t, ch, 0)),
                                        set(e, fstate(e, "f_o", t, ch, 0), e.v("o")),
                                        set(e, fstate(e, "f_i", t, ch, 1), fstate(e, "f_i", t, ch, 0)),
                                        set(e, fstate(e, "f_i", t, ch, 0), e.v("delay")),
                                        set(
                                                e,
                                                idx(e, p(e, "buf"), bufIdx),
                                                add(
                                                        e,
                                                        mul(e, pt(e, "t_feedback", t), e.v("o")),
                                                        idx(e, p(e, "aux"), si))),
                                        set(
                                                e,
                                                idx(e, e.v("out"), si),
                                                add(
                                                        e,
                                                        idx(e, e.v("out"), si),
                                                        mul(e, e.v("o"), pt(e, "t_wetout", t))))),
                                set(e, pt(e, "t_pos", t), add(e, pt(e, "t_pos", t), e.n(1))),
                                ifOnly(
                                        e,
                                        ge(e, pt(e, "t_pos", t), pt(e, "t_length", t)),
                                        set(e, pt(e, "t_pos", t), e.n(0))))),
                EcoBuzz.mdkDcBlock(e, e.v("out"), e.v("n")));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(e, e.v("ll"), "nj_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), work)));
        return EcoBuzz.machineModule(
                e, "ninja", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
