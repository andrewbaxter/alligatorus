package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.and;
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
import static compilerxyz.main.tests.EcoBuzz.or;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzKAway {
    private EcoBuzzKAway() {}

    static final int NG = 31;
    static final int NT = 4;
    static final int TRACKS = 8;
    private static final int HARMONICS = 7;
    private static final int WAVES = 17;
    private static final int WAVE_STRIDE = 640;
    private static final int WAVE_LEN = 513;
    private static final double RATE = 44100.0;
    private static final double TABLE_STEP = 0.0122718;
    private static final double TWO_PI = 2 * Math.PI;
    private static final int RAND_SEED = 1;
    private static final IList<Integer> OCTAVE_BREAKS = IList.of(12, 28, 44, 60, 76, 92, 108, 124, 140);
    private static final IList<String> H_F32_FIELDS = IList.of("h_detune", "h_trans", "h_vol");
    private static final IList<String> F32_FIELDS = IList.of(
            "cutoff_target",
            "cutoff_cur",
            "inertia1",
            "f_x1",
            "f_x2",
            "f_y1",
            "f_y2",
            "f_b0",
            "f_b1",
            "f_b2",
            "f_a1",
            "f_a2");
    private static final IList<String> I32_FIELDS = IList.of("res1", "ftype", "playing");
    private static final IList<String> T_F32_FIELDS =
            IList.of("t_env", "t_target", "t_smooth", "t_vol", "t_length", "t_counter");
    private static final IList<String> T_I32_FIELDS = IList.of("t_state");

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pt(EcoMb e, String field, SourceExpr t) {
        return idx(e, fld(e, e.v("p"), field), t);
    }

    private static SourceExpr ph(EcoMb e, String field, int h, SourceExpr t) {
        return idx(e, fld(e, e.v("p"), field), add(e, t, e.n(h * TRACKS)));
    }

    private static SourceExpr hv(EcoMb e, String field, int h) {
        return idx(e, fld(e, e.v("p"), field), e.n(h));
    }

    private static SourceExpr tval(EcoMb e, SourceExpr t, int k) {
        return idx(e, idx(e, p(e, "tval"), t), e.n(k));
    }

    private static SourceExpr tab(EcoMb e, int wave, SourceExpr i) {
        return idx(e, p(e, "table"), add(e, i, e.n(wave * WAVE_STRIDE)));
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

    private static SourceExpr onGval(EcoMb e, int k, SourceExpr... apply) {
        var body = new TList<SourceExpr>();
        body.add(e.let("v", idx(e, p(e, "gval"), e.n(k))));
        body.add(ifOnly(e, ne(e, e.v("v"), e.n(EcoBuzz.NO_VALUE)), apply));
        return e.seq(body.toArray(SourceExpr[]::new));
    }

    private static SourceExpr biquad(
            EcoMb e, SourceExpr b0, SourceExpr b1, SourceExpr b2, SourceExpr a0, SourceExpr a1, SourceExpr a2) {
        return e.seq(
                set(e, p(e, "f_b0"), div(e, b0, a0)),
                set(e, p(e, "f_b1"), div(e, b1, a0)),
                set(e, p(e, "f_b2"), div(e, b2, a0)),
                set(e, p(e, "f_a1"), e.mb.operatorNeg(div(e, a1, a0))),
                set(e, p(e, "f_a2"), e.mb.operatorNeg(div(e, a2, a0))));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("i32xt", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS))));
        defs.add(e.let("f32xt", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS))));
        defs.add(e.let("f32xh", EcoBuzz.array(e, e.v("t_f32"), e.intOf(HARMONICS))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "spt", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        fields.add(EcoBuzz.field(e, "table", EcoBuzz.array(e, e.v("t_f32"), e.intOf(WAVES * WAVE_STRIDE))));
        for (var f : H_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xh")));
        fields.add(EcoBuzz.field(e, "h_wave", EcoBuzz.array(e, e.v("t_i32"), e.intOf(HARMONICS))));
        for (var f : F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_f32")));
        for (var f : I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "phase", EcoBuzz.array(e, e.v("t_f32"), e.intOf(HARMONICS * TRACKS))));
        fields.add(EcoBuzz.field(e, "inc", EcoBuzz.array(e, e.v("t_f32"), e.intOf(HARMONICS * TRACKS))));
        for (var f : T_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xt")));
        for (var f : T_I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32xt")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "ka_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let(
                "coefs_fn_t",
                EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("f2_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"), e.v("t_f32"))));

        var body = new TList<SourceExpr>();
        body.add(e.let("zero", e.callM0(e.v("arg"), "zero")));
        for (var fn : new String[] {"sinf", "cosf", "sinhf"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, e.v("ll"), fn, e.lowerV("f1_fn_t"))));
        }
        body.add(e.let("powf", EcoBuzz.externDef(e, e.v("ll"), "powf", e.lowerV("f2_fn_t"))));
        SourceExpr coefsSeq;
        {
            var cut = e.v("cut");
            var res1 = e.v("res1");
            var ftype = e.v("ftype");
            var one = f32(e, e.n(1));
            var zero = f32(e, e.n(0));
            var sw = e.v("sw");
            var cw = e.v("cw");
            var alpha = e.v("alpha");
            var a0 = add(e, alpha, e.n(1));
            var a1 = mul(e, cw, e.n(-2));
            var a2 = sub(e, one, alpha);
            coefsSeq = e.seq(
                    e.let(
                            "f",
                            add(
                                    e,
                                    mul(
                                            e,
                                            call(
                                                    e,
                                                    "powf",
                                                    mul(e, f32(e, add(e, cut, e.n(5))), e.n(1.0 / 132)),
                                                    f32(e, e.n(1.7))),
                                            e.n(13000)),
                                    e.n(30))),
                    e.let("w", mul(e, e.v("f"), e.n(TWO_PI / RATE))),
                    e.let("sw", call(e, "sinf", e.v("w"))),
                    e.let("cw", call(e, "cosf", e.v("w"))),
                    e.let("alpha", f32(e, e.n(0))),
                    ifOnly(
                            e,
                            lt(e, ftype, e.n(2)),
                            e.let("ra", mul(e, f32(e, mul(e, add(e, cut, e.n(70)), res1)), e.n(1.0 / 197))),
                            e.let(
                                    "rq",
                                    add(
                                            e,
                                            mul(
                                                    e,
                                                    call(e, "powf", mul(e, e.v("ra"), e.n(1.0 / 127)), f32(e, e.n(4))),
                                                    e.n(150)),
                                            e.n(0.1))),
                            set(e, alpha, div(e, sw, e.v("rq")))),
                    ifOnly(
                            e,
                            ge(e, ftype, e.n(2)),
                            e.let(
                                    "bq",
                                    add(
                                            e,
                                            mul(
                                                    e,
                                                    call(
                                                            e,
                                                            "powf",
                                                            mul(e, f32(e, res1), e.n(1.0 / 127)),
                                                            f32(e, e.n(4))),
                                                    e.n(4)),
                                            e.n(0.1))),
                            set(e, alpha, mul(e, sw, call(e, "sinhf", div(e, mul(e, e.v("bq"), e.v("w")), sw))))),
                    ifOnly(
                            e,
                            eq(e, ftype, e.n(0)),
                            e.let("lp", sub(e, one, cw)),
                            biquad(e, mul(e, e.v("lp"), e.n(0.5)), e.v("lp"), mul(e, e.v("lp"), e.n(0.5)), a0, a1, a2)),
                    ifOnly(
                            e,
                            eq(e, ftype, e.n(1)),
                            e.let("hp", add(e, cw, e.n(1))),
                            biquad(
                                    e,
                                    mul(e, e.v("hp"), e.n(0.5)),
                                    e.mb.operatorNeg(e.v("hp")),
                                    mul(e, e.v("hp"), e.n(0.5)),
                                    a0,
                                    a1,
                                    a2)),
                    ifOnly(e, eq(e, ftype, e.n(2)), biquad(e, alpha, zero, e.mb.operatorNeg(alpha), a0, a1, a2)),
                    ifOnly(e, eq(e, ftype, e.n(3)), biquad(e, one, mul(e, cw, e.n(-2)), one, a0, a1, a2)),
                    ifOnly(e, gt(e, ftype, e.n(3)), biquad(e, one, zero, zero, one, zero, zero)));
        }
        body.add(e.let(
                "coefs",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "ka_coefs",
                        new String[] {"p", "cut", "res1", "ftype"},
                        e.lowerV("coefs_fn_t"),
                        coefsSeq)));

        var t = e.v("t");
        var init = new TList<SourceExpr>();
        for (int h = 0; h < HARMONICS; h++) {
            for (var f : H_F32_FIELDS) init.add(set(e, hv(e, f, h), e.n(0)));
            init.add(set(e, hv(e, "h_wave", h), e.n(0)));
        }
        init.add(set(e, p(e, "cutoff_target"), e.n(64)));
        init.add(set(e, p(e, "cutoff_cur"), e.n(0)));
        init.add(set(e, p(e, "inertia1"), e.n(16)));
        init.add(set(e, p(e, "res1"), e.n(64)));
        init.add(set(e, p(e, "ftype"), e.n(4)));
        init.add(set(e, p(e, "playing"), e.n(0)));
        for (var f : new String[] {"f_x1", "f_x2", "f_y1", "f_y2", "f_b0", "f_b1", "f_b2", "f_a1", "f_a2"}) {
            init.add(set(e, p(e, f), e.n(0)));
        }
        init.add(forRange(
                e,
                "t",
                e.n(TRACKS),
                forRange(
                        e,
                        "h",
                        e.n(HARMONICS),
                        e.let("hi", add(e, mul(e, e.v("h"), e.n(TRACKS)), t)),
                        set(e, idx(e, p(e, "phase"), e.v("hi")), e.n(0)),
                        set(e, idx(e, p(e, "inc"), e.v("hi")), e.n(1))),
                set(e, pt(e, "t_env", t), e.n(0)),
                set(e, pt(e, "t_target", t), e.n(0)),
                set(e, pt(e, "t_smooth", t), e.n(528)),
                set(e, pt(e, "t_length", t), e.n(32)),
                set(e, pt(e, "t_vol", t), e.n(128)),
                set(e, pt(e, "t_counter", t), e.n(0)),
                set(e, pt(e, "t_state", t), e.n(0))));
        SourceExpr tableGenSeq;
        {
            var i = e.v("i");
            var fi = e.v("fi");
            var lo = lt(e, i, e.n(256));
            tableGenSeq = e.seq(
                    forRange(e, "i", e.n(WAVES * WAVE_STRIDE), set(e, idx(e, p(e, "table"), i), e.n(0))),
                    e.let("seed", EcoBuzz.i32(e, e.n(RAND_SEED))),
                    e.let("prev", f32(e, e.n(0))),
                    forRange(
                            e,
                            "i",
                            e.n(WAVE_LEN),
                            e.let("fi", f32(e, i)),
                            e.let("s", call(e, "sinf", mul(e, fi, e.n(TABLE_STEP)))),
                            e.let("saw", sub(e, mul(e, fi, e.n(126)), e.n(32768))),
                            set(e, tab(e, 0, i), mul(e, e.v("s"), e.n(32000))),
                            set(e, tab(e, 4, i), mul(e, e.v("s"), e.n(-32000))),
                            set(e, tab(e, 1, i), e.v("saw")),
                            set(e, tab(e, 5, i), e.mb.operatorNeg(e.v("saw"))),
                            set(e, tab(e, 2, i), e.ifElse(lo, f32(e, e.n(-32000)), f32(e, e.n(32000)))),
                            set(e, tab(e, 6, i), e.ifElse(lo, f32(e, e.n(32000)), f32(e, e.n(-32000)))),
                            e.let(
                                    "tri",
                                    e.ifElse(
                                            lo,
                                            sub(e, mul(e, fi, e.n(256)), e.n(32768)),
                                            sub(e, f32(e, e.n(98304)), mul(e, fi, e.n(256))))),
                            set(e, tab(e, 3, i), e.v("tri")),
                            set(e, tab(e, 7, i), e.mb.operatorNeg(e.v("tri"))),
                            set(e, e.v("seed"), add(e, mul(e, e.v("seed"), e.n(214013)), e.n(2531011))),
                            e.let("r", f32(e, and(e, e.mb.operatorShr(e.v("seed"), e.n(16)), e.n(0x7fff)))),
                            set(e, e.v("prev"), add(e, e.v("prev"), mul(e, sub(e, e.v("r"), e.v("prev")), e.n(0.25)))),
                            set(e, tab(e, 8, i), e.v("prev")),
                            e.let("fk", f32(e, e.mb.operatorShr(i, e.n(3)))),
                            e.let("s2", call(e, "sinf", mul(e, mul(e, e.v("fk"), e.v("fk")), e.n(TABLE_STEP)))),
                            set(e, tab(e, 9, i), mul(e, e.v("s2"), e.n(32000))),
                            set(e, tab(e, 10, i), mul(e, e.v("s2"), e.n(-32000))),
                            e.let("para", mul(e, mul(e, fi, fi), e.n(0.5))),
                            set(e, tab(e, 11, i), e.v("para")),
                            set(e, tab(e, 12, i), e.mb.operatorNeg(e.v("para"))),
                            e.let("pulse16", e.ifElse(lt(e, i, e.n(16)), f32(e, e.n(32000)), f32(e, e.n(0)))),
                            set(e, tab(e, 13, i), e.v("pulse16")),
                            set(e, tab(e, 14, i), e.mb.operatorNeg(e.v("pulse16"))),
                            e.let("pulse4", e.ifElse(lt(e, i, e.n(4)), f32(e, e.n(32000)), f32(e, e.n(0)))),
                            set(e, tab(e, 15, i), e.v("pulse4")),
                            set(e, tab(e, 16, i), e.mb.operatorNeg(e.v("pulse4")))));
        }
        init.add(tableGenSeq);
        init.add(resetVals(e));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "ka_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        e.seq(init.toArray(SourceExpr[]::new)))));

        SourceExpr noteOnSeq;
        {
            var noteBody = new TList<SourceExpr>();
            noteBody.add(e.let("m", e.v("note")));
            for (var brk : OCTAVE_BREAKS) {
                noteBody.add(ifOnly(e, gt(e, e.v("note"), e.n(brk)), set(e, e.v("m"), sub(e, e.v("m"), e.n(4)))));
            }
            noteBody.add(e.let(
                    "semi", add(e, sub(e, f32(e, e.v("m")), e.n(42)), mul(e, hv(e, "h_detune", 0), e.n(1.0 / 128)))));
            noteBody.add(
                    set(e, ph(e, "inc", 0, t), call(e, "powf", f32(e, e.n(2)), mul(e, e.v("semi"), e.n(1.0 / 12)))));
            for (int h = 1; h < HARMONICS; h++) {
                noteBody.add(set(
                        e,
                        ph(e, "inc", h, t),
                        call(
                                e,
                                "powf",
                                f32(e, e.n(2)),
                                mul(
                                        e,
                                        add(
                                                e,
                                                add(
                                                        e,
                                                        mul(e, hv(e, "h_detune", h), e.n(1.0 / 128)),
                                                        hv(e, "h_trans", h)),
                                                e.v("semi")),
                                        e.n(1.0 / 12)))));
            }
            noteBody.add(set(e, pt(e, "t_state", t), e.n(1)));
            noteBody.add(set(e, pt(e, "t_counter", t), e.n(0)));
            noteBody.add(set(e, pt(e, "t_target", t), mul(e, pt(e, "t_vol", t), e.n(256))));
            noteOnSeq = e.seq(noteBody.toArray(SourceExpr[]::new));
        }
        var tick = new TList<SourceExpr>();
        tick.add(onGval(e, 0, set(e, hv(e, "h_detune", 0), sub(e, f32(e, e.v("v")), e.n(64)))));
        tick.add(onGval(e, 1, set(e, hv(e, "h_wave", 0), e.v("v"))));
        tick.add(onGval(e, 2, set(e, hv(e, "h_vol", 0), mul(e, f32(e, e.v("v")), e.n(1.0 / 128)))));
        for (int h = 1; h < HARMONICS; h++) {
            int base = 3 + 4 * (h - 1);
            tick.add(onGval(e, base, set(e, hv(e, "h_trans", h), sub(e, f32(e, e.v("v")), e.n(64)))));
            tick.add(onGval(e, base + 1, set(e, hv(e, "h_detune", h), sub(e, f32(e, e.v("v")), e.n(64)))));
            tick.add(onGval(e, base + 2, set(e, hv(e, "h_wave", h), e.v("v"))));
            tick.add(onGval(e, base + 3, set(e, hv(e, "h_vol", h), mul(e, f32(e, e.v("v")), e.n(1.0 / 128)))));
        }
        tick.add(onGval(e, 27, set(e, p(e, "cutoff_target"), f32(e, e.v("v")))));
        tick.add(onGval(e, 28, set(e, p(e, "inertia1"), f32(e, add(e, e.v("v"), e.n(1))))));
        tick.add(onGval(e, 29, set(e, p(e, "res1"), add(e, e.v("v"), e.n(1)))));
        tick.add(onGval(e, 30, set(e, p(e, "ftype"), e.v("v"))));
        tick.add(forRange(
                e,
                "t",
                e.n(TRACKS),
                e.let("len", tval(e, t, 2)),
                ifOnly(e, ne(e, e.v("len"), e.n(EcoBuzz.NO_VALUE)), set(e, pt(e, "t_length", t), f32(e, e.v("len")))),
                e.let("vol", tval(e, t, 1)),
                ifOnly(
                        e,
                        ne(e, e.v("vol"), e.n(EcoBuzz.NO_VALUE)),
                        set(e, pt(e, "t_vol", t), f32(e, e.v("vol"))),
                        ifOnly(
                                e,
                                eq(e, pt(e, "t_state", t), e.n(1)),
                                set(e, pt(e, "t_target", t), mul(e, pt(e, "t_vol", t), e.n(256))))),
                e.let("sm", tval(e, t, 3)),
                ifOnly(
                        e,
                        ne(e, e.v("sm"), e.n(EcoBuzz.NO_VALUE)),
                        set(e, pt(e, "t_smooth", t), f32(e, add(e, mul(e, e.v("sm"), e.n(32)), e.n(16))))),
                e.let("note", tval(e, t, 0)),
                ifOnly(e, eq(e, e.v("note"), e.n(EcoBuzz.NOTE_OFF)), set(e, pt(e, "t_state", t), e.n(2))),
                ifOnly(e, and(e, gt(e, e.v("note"), e.n(0)), ne(e, e.v("note"), e.n(EcoBuzz.NOTE_OFF))), noteOnSeq)));
        tick.add(resetVals(e));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "ka_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        e.seq(tick.toArray(SourceExpr[]::new)))));

        var i = e.v("i");
        var sample = new TList<SourceExpr>();
        sample.add(e.let("acc", f32(e, e.n(0))));
        var track = new TList<SourceExpr>();
        track.add(e.let("x", f32(e, e.n(0))));
        for (int h = 0; h < HARMONICS; h++) {
            track.add(ifOnly(
                    e,
                    gt(e, hv(e, "h_vol", h), e.n(0.01)),
                    e.let("ph", add(e, ph(e, "phase", h, t), ph(e, "inc", h, t))),
                    ifOnly(e, ge(e, e.v("ph"), e.n(512)), set(e, e.v("ph"), sub(e, e.v("ph"), e.n(512)))),
                    set(e, ph(e, "phase", h, t), e.v("ph")),
                    e.let("pi", i32(e, e.v("ph"))),
                    e.let("fr", sub(e, e.v("ph"), f32(e, e.v("pi")))),
                    e.let("ti", add(e, mul(e, hv(e, "h_wave", h), e.n(WAVE_STRIDE)), e.v("pi"))),
                    e.let("a", idx(e, p(e, "table"), e.v("ti"))),
                    e.let("b", idx(e, p(e, "table"), add(e, e.v("ti"), e.n(1)))),
                    set(
                            e,
                            e.v("x"),
                            add(
                                    e,
                                    e.v("x"),
                                    mul(
                                            e,
                                            add(e, e.v("a"), mul(e, sub(e, e.v("b"), e.v("a")), e.v("fr"))),
                                            hv(e, "h_vol", h))))));
        }
        track.add(e.let("d", sub(e, pt(e, "t_target", t), pt(e, "t_env", t))));
        track.add(ifOnly(
                e,
                or(e, gt(e, e.v("d"), e.n(1)), lt(e, e.v("d"), e.n(-1))),
                set(e, pt(e, "t_env", t), add(e, pt(e, "t_env", t), div(e, e.v("d"), pt(e, "t_smooth", t))))));
        track.add(set(e, e.v("x"), mul(e, e.v("x"), e.n(1.0 / 7))));
        track.add(ifOnly(
                e,
                or(e, gt(e, e.v("x"), e.n(1)), lt(e, e.v("x"), e.n(-1))),
                set(e, e.v("acc"), add(e, e.v("acc"), mul(e, mul(e, pt(e, "t_env", t), e.v("x")), e.n(1.0 / 32768))))));
        sample.add(forRange(
                e, "t", e.n(TRACKS), ifOnly(e, ne(e, pt(e, "t_state", t), e.n(0)), track.toArray(SourceExpr[]::new))));
        sample.add(e.let(
                "y",
                add(
                        e,
                        add(
                                e,
                                add(
                                        e,
                                        add(e, mul(e, p(e, "f_b0"), e.v("acc")), mul(e, p(e, "f_b1"), p(e, "f_x1"))),
                                        mul(e, p(e, "f_b2"), p(e, "f_x2"))),
                                mul(e, p(e, "f_a1"), p(e, "f_y1"))),
                        mul(e, p(e, "f_a2"), p(e, "f_y2")))));
        sample.add(set(e, p(e, "f_x2"), p(e, "f_x1")));
        sample.add(set(e, p(e, "f_x1"), e.v("acc")));
        sample.add(set(e, p(e, "f_y2"), p(e, "f_y1")));
        sample.add(set(e, p(e, "f_y1"), e.v("y")));
        sample.add(set(e, idx(e, e.v("out"), mul(e, i, e.n(2))), e.v("y")));
        sample.add(set(e, idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))), e.v("y")));

        var work = e.seq(
                set(e, p(e, "playing"), e.n(0)),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        ifOnly(
                                e,
                                eq(e, pt(e, "t_state", t), e.n(1)),
                                set(e, pt(e, "t_counter", t), add(e, pt(e, "t_counter", t), e.n(1))),
                                ifOnly(
                                        e,
                                        gt(e, pt(e, "t_counter", t), pt(e, "t_length", t)),
                                        set(e, pt(e, "t_state", t), e.n(2)))),
                        ifOnly(
                                e,
                                eq(e, pt(e, "t_state", t), e.n(2)),
                                set(e, pt(e, "t_target", t), e.n(0)),
                                ifOnly(e, lt(e, pt(e, "t_env", t), e.n(64)), set(e, pt(e, "t_state", t), e.n(0)))),
                        ifOnly(e, ne(e, pt(e, "t_state", t), e.n(0)), set(e, p(e, "playing"), e.n(1)))),
                e.let("cd", sub(e, p(e, "cutoff_target"), p(e, "cutoff_cur"))),
                ifOnly(
                        e,
                        or(e, lt(e, e.v("cd"), e.n(-1)), gt(e, e.v("cd"), e.n(1))),
                        set(e, p(e, "cutoff_cur"), add(e, p(e, "cutoff_cur"), div(e, e.v("cd"), p(e, "inertia1"))))),
                call(e, "coefs", e.v("p"), i32(e, p(e, "cutoff_cur")), p(e, "res1"), p(e, "ftype")),
                ifOnly(e, eq(e, p(e, "playing"), e.n(0)), call(e, "zero", e.v("out"), e.v("n"))),
                ifOnly(
                        e,
                        ne(e, p(e, "playing"), e.n(0)),
                        forRange(e, "i", e.v("n"), sample.toArray(SourceExpr[]::new))));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(e, e.v("ll"), "ka_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), work)));
        return EcoBuzz.machineModule(
                e, "kaway", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
