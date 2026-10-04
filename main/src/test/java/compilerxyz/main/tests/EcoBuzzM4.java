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
import static compilerxyz.main.tests.EcoBuzz.le;
import static compilerxyz.main.tests.EcoBuzz.lt;
import static compilerxyz.main.tests.EcoBuzz.lw;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.ne;
import static compilerxyz.main.tests.EcoBuzz.or;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.shl;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzM4 {
    private EcoBuzzM4() {}

    static final int NG = 35;
    static final int NT = 2;
    static final int TRACKS = 8;
    static final int NUMWAVES = 126;
    static final int WAVE_LEN = 2048;
    static final int WAVES_BYTES = NUMWAVES * WAVE_LEN * 2;
    static final int COEFS = 4 * 128 * 128 * 8;
    static final int LFOOSC = 0x10000;
    private static final double RATE = 44100.0;
    private static final double TSDSF = 2048.0 / RATE;
    private static final double PHASE_ONE = 0x8000000;
    private static final int LFO_MASK_A = 0b1000011101100010;
    private static final int LFO_MASK_B = 0b0101101100110100;
    private static final int LFO_MASK_C = 0b0110110011001000;
    private static final int LFO_MASK_D = 0b0111011010110000;

    private static final IList<String> I32_FIELDS = IList.of(
            "spt",
            "ftype_base",
            "cutoff",
            "resonance",
            "db24",
            "db18",
            "peg_att",
            "peg_dec",
            "penv_mod",
            "pitch_mod",
            "aeg_att",
            "aeg_sus",
            "aeg_rel",
            "feg_att",
            "feg_sus",
            "feg_rel",
            "fenv_mod",
            "glide",
            "glide_time",
            "sync",
            "wave1",
            "wave2",
            "wavesub",
            "noise1",
            "noise2",
            "sub_vol",
            "bal1",
            "bal2",
            "mix_type",
            "lfo1_wave",
            "lfo2_wave",
            "lfo1_noise",
            "lfo2_noise",
            "lfo1_synced",
            "lfo2_synced",
            "lfo1_freq",
            "lfo2_freq",
            "lfo1_amount",
            "lfo2_amount",
            "lfo1_pd",
            "lfo2_pd",
            "phase_lfo1",
            "phase_lfo2",
            "padd_lfo1",
            "padd_lfo2",
            "l1_osc1",
            "l1_vol",
            "l1_cut",
            "l2_osc2",
            "l2_mix",
            "l2_reso",
            "f_osc1",
            "f_pw1",
            "f_amp",
            "f_cut",
            "f_osc2",
            "f_pw2",
            "f_mix",
            "f_reso",
            "ntracks");
    private static final IList<String> F32_FIELDS =
            IList.of("tsdsf", "detune_semi", "detune_fine", "center1", "center2", "l1_pw1", "l2_pw2");
    private static final IList<String> T_I32_FIELDS = IList.of(
            "t_phase1",
            "t_phase2",
            "t_phasesub",
            "t_ph1",
            "t_ph2",
            "t_c1",
            "t_c2",
            "t_padd1",
            "t_padd2",
            "t_glide_act",
            "t_glide_cnt",
            "t_pm_act",
            "t_peg_state",
            "t_peg_cnt",
            "t_r1",
            "t_r2",
            "t_r3",
            "t_r4",
            "t_aeg_state",
            "t_aeg_cnt",
            "t_volume",
            "t_amp",
            "t_amp_add",
            "t_feg_state",
            "t_feg_cnt",
            "t_phl1",
            "t_phl2");
    private static final IList<String> T_F32_FIELDS = IList.of(
            "t_center1",
            "t_center2",
            "t_ps1a",
            "t_ps1b",
            "t_ps2a",
            "t_ps2b",
            "t_freq",
            "t_freq_from",
            "t_glide_mul",
            "t_glide_fac",
            "t_pitch_mul",
            "t_pitch_fac",
            "t_old_out",
            "t_x1",
            "t_x2",
            "t_y1",
            "t_y2",
            "t_x241",
            "t_x242",
            "t_y241",
            "t_y242",
            "t_cut",
            "t_cut_add");

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pt(EcoMb e, String field) {
        return idx(e, fld(e, e.v("p"), field), e.v("t"));
    }

    private static SourceExpr gval(EcoMb e, int k) {
        return idx(e, p(e, "gval"), e.n(k));
    }

    private static SourceExpr lfoTab(EcoMb e, String wave, String phase) {
        return call(e, "osc_at", p(e, wave), e.mb.operatorUshr(pt(e, phase), e.n(21)));
    }

    private static SourceExpr wave16(EcoMb e, SourceExpr index) {
        var bytes = fld(e, p(e, "data"), "b");
        var b = mul(e, index, e.n(2));
        return or(
                e,
                shl(e, i32(e, idx(e, bytes, add(e, b, e.n(1)))), e.n(8)),
                and(e, i32(e, idx(e, bytes, b)), e.n(255)));
    }

    private static SourceExpr envSamples(EcoMb e, SourceExpr v) {
        var ms = mul(
                e, call(e, "powf", div(e, f32(e, add(e, v, e.n(2))), f32(e, e.n(129))), f32(e, e.n(3))), e.n(10000));
        return add(e, i32(e, mul(e, ms, e.n(RATE / 1000.0))), e.n(1));
    }

    private static SourceExpr onGval(EcoMb e, int k, SourceExpr... then) {
        var inner = new TList<SourceExpr>();
        inner.add(e.let("v", gval(e, k)));
        for (var x : then) inner.add(x);
        return ifOnly(e, ne(e, gval(e, k), e.n(-1)), e.seq(inner.toArray(SourceExpr[]::new)));
    }

    private static SourceExpr scalLfoFreq(EcoMb e, SourceExpr v) {
        return mul(
                e,
                sub(
                        e,
                        call(e, "powf", div(e, f32(e, add(e, v, e.n(8))), f32(e, e.n(124))), f32(e, e.n(4))),
                        e.n(0.000017324998565270)),
                e.n(40.00072));
    }

    private static SourceExpr lfoPhaseAdd(EcoMb e, String synced, String noise, String freq) {
        var spt = p(e, "spt");
        var fr = p(e, freq);
        return e.ifElse(
                ne(e, p(e, synced), e.n(0)),
                e.ifElse(
                        ne(e, p(e, noise), e.n(0)),
                        div(e, EcoBuzz.i32(e, e.n(0x200000)), shl(e, spt, fr)),
                        i32(e, div(e, f32(e, e.n(0x200000 * 2048.0)), f32(e, shl(e, spt, fr))))),
                e.ifElse(
                        ne(e, p(e, noise), e.n(0)),
                        i32(e, mul(e, div(e, scalLfoFreq(e, fr), f32(e, e.n(RATE))), e.n(0x200000))),
                        i32(e, mul(e, mul(e, scalLfoFreq(e, fr), p(e, "tsdsf")), e.n(0x200000)))));
    }

    private static SourceExpr lfoFlags(EcoMb e, int gvalIndex, String fa, String fb, String fc, String fd) {
        return onGval(
                e,
                gvalIndex,
                set(e, p(e, fa), and(e, e.mb.operatorUshr(EcoBuzz.i32(e, e.n(LFO_MASK_A)), e.v("v")), e.n(1))),
                set(e, p(e, fb), and(e, e.mb.operatorUshr(EcoBuzz.i32(e, e.n(LFO_MASK_B)), e.v("v")), e.n(1))),
                set(e, p(e, fc), and(e, e.mb.operatorUshr(EcoBuzz.i32(e, e.n(LFO_MASK_C)), e.v("v")), e.n(1))),
                set(e, p(e, fd), and(e, e.mb.operatorUshr(EcoBuzz.i32(e, e.n(LFO_MASK_D)), e.v("v")), e.n(1))));
    }

    private static SourceExpr oscSource(EcoMb e, String noise, String wave, String ph, String bal, String out) {
        var rnd = out + "n";
        var sum = add(e, add(e, add(e, pt(e, "t_r1"), pt(e, "t_r2")), pt(e, "t_r3")), pt(e, "t_r4"));
        return e.ifElse(
                ne(e, p(e, noise), e.n(0)),
                e.seq(
                        e.let(rnd, e.mb.operatorShr(shl(e, sum, e.n(16)), e.n(16))),
                        set(e, pt(e, "t_r1"), pt(e, "t_r2")),
                        set(e, pt(e, "t_r2"), pt(e, "t_r3")),
                        set(e, pt(e, "t_r3"), pt(e, "t_r4")),
                        set(e, pt(e, "t_r4"), e.v(rnd)),
                        set(e, e.v(out), e.mb.operatorShr(mul(e, e.v(rnd), e.v(bal)), e.n(7)))),
                set(
                        e,
                        e.v(out),
                        e.mb.operatorShr(
                                mul(
                                        e,
                                        wave16(
                                                e,
                                                add(
                                                        e,
                                                        shl(e, p(e, wave), e.n(11)),
                                                        e.mb.operatorShr(pt(e, ph), e.n(16)))),
                                        e.v(bal)),
                                e.n(7))));
    }

    private static SourceExpr iabs(EcoMb e, SourceExpr x) {
        return e.ifElse(lt(e, x, e.n(0)), e.mb.operatorNeg(x), x);
    }

    private static SourceExpr biquad(EcoMb e, String x1, String x2, String y1, String y2, SourceExpr x, String out) {
        var c = lw(e, "coefs");
        var ofs = e.v("ofs");
        return e.seq(
                set(
                        e,
                        e.v(out),
                        add(
                                e,
                                add(
                                        e,
                                        add(
                                                e,
                                                add(
                                                        e,
                                                        mul(e, idx(e, c, ofs), x),
                                                        mul(e, idx(e, c, add(e, ofs, e.n(1))), pt(e, x1))),
                                                mul(e, idx(e, c, add(e, ofs, e.n(2))), pt(e, x2))),
                                        mul(e, idx(e, c, add(e, ofs, e.n(3))), pt(e, y1))),
                                mul(e, idx(e, c, add(e, ofs, e.n(4))), pt(e, y2)))),
                set(e, pt(e, y2), pt(e, y1)),
                set(e, pt(e, y1), e.v(out)),
                set(e, pt(e, x2), pt(e, x1)),
                set(e, pt(e, x1), x));
    }

    private static SourceExpr scaledStep(EcoMb e, SourceExpr padd, SourceExpr factor) {
        return e.ifElse(eq(e, padd, e.n(0)), EcoBuzz.i32(e, e.n(0)), i32(e, mul(e, f32(e, padd), factor)));
    }

    private static SourceExpr phaseStep(EcoMb e, SourceExpr factor) {
        var pf = idx(
                e,
                lw(e, "lfoosc"),
                add(
                        e,
                        e.mb.operatorShr(mul(e, lfoTab(e, "lfo1_wave", "t_phl1"), p(e, "l1_osc1")), e.n(7)),
                        e.n(0x8000)));
        var pf2 = idx(
                e,
                lw(e, "lfoosc"),
                add(
                        e,
                        e.mb.operatorShr(mul(e, lfoTab(e, "lfo2_wave", "t_phl2"), p(e, "l2_osc2")), e.n(7)),
                        e.n(0x8000)));
        return e.seq(
                e.let("f1", e.ifElse(ne(e, p(e, "f_osc1"), e.n(0)), mul(e, factor, pf), factor)),
                e.let("f2", e.ifElse(ne(e, p(e, "f_osc2"), e.n(0)), mul(e, factor, pf2), factor)),
                set(e, pt(e, "t_phase1"), add(e, pt(e, "t_phase1"), scaledStep(e, pt(e, "t_padd1"), e.v("f1")))),
                set(
                        e,
                        pt(e, "t_phasesub"),
                        add(
                                e,
                                pt(e, "t_phasesub"),
                                scaledStep(e, e.mb.operatorShr(pt(e, "t_padd1"), e.n(1)), e.v("f1")))),
                set(e, pt(e, "t_phase2"), add(e, pt(e, "t_phase2"), scaledStep(e, pt(e, "t_padd2"), e.v("f2")))));
    }

    private static SourceExpr pulseCenter(
            EcoMb e,
            String flag,
            String center,
            String wave,
            String ph,
            String amount,
            String tcenter,
            String psa,
            String psb,
            String c) {
        return e.seq(
                e.let("cc", add(e, p(e, center), mul(e, f32(e, lfoTab(e, wave, ph)), p(e, amount)))),
                set(
                        e,
                        pt(e, tcenter),
                        e.ifElse(
                                ne(e, p(e, flag), e.n(0)),
                                e.ifElse(
                                        lt(e, e.v("cc"), e.n(0)),
                                        f32(e, e.n(0)),
                                        e.ifElse(gt(e, e.v("cc"), e.n(1)), f32(e, e.n(1)), e.v("cc"))),
                                p(e, center))),
                set(e, pt(e, psa), div(e, f32(e, e.n(0.5)), pt(e, tcenter))),
                set(e, pt(e, psb), div(e, f32(e, e.n(0.5)), sub(e, f32(e, e.n(1)), pt(e, tcenter)))),
                set(e, pt(e, c), i32(e, mul(e, pt(e, tcenter), e.n(PHASE_ONE)))));
    }

    private static SourceExpr warp(EcoMb e, String phase, String c, String psa, String psb, String out) {
        return set(
                e,
                pt(e, out),
                e.ifElse(
                        lt(e, pt(e, phase), pt(e, c)),
                        i32(e, mul(e, f32(e, pt(e, phase)), pt(e, psa))),
                        i32(e, add(e, mul(e, f32(e, sub(e, pt(e, phase), pt(e, c))), pt(e, psb)), e.n(0x4000000)))));
    }

    private static SourceExpr setPhaseAdds(EcoMb e, SourceExpr freq) {
        return e.seq(
                set(e, pt(e, "t_padd1"), i32(e, mul(e, mul(e, freq, p(e, "tsdsf")), e.n(65536)))),
                set(
                        e,
                        pt(e, "t_padd2"),
                        i32(
                                e,
                                mul(
                                        e,
                                        mul(
                                                e,
                                                mul(e, mul(e, freq, p(e, "detune_semi")), p(e, "detune_fine")),
                                                p(e, "tsdsf")),
                                        e.n(65536)))));
    }

    static SourceExpr coefTable(EcoMb e) {
        var f = e.v("f");
        var r = e.v("r");
        var tt = e.v("tt");
        return forRange(
                e,
                "tt",
                e.n(4),
                forRange(
                        e,
                        "f",
                        e.n(128),
                        forRange(
                                e,
                                "r",
                                e.n(128),
                                e.let(
                                        "cut",
                                        add(
                                                e,
                                                mul(
                                                        e,
                                                        call(
                                                                e,
                                                                "powf",
                                                                div(e, f32(e, add(e, f, e.n(5))), f32(e, e.n(132))),
                                                                f32(e, e.n(1.7))),
                                                        e.n(13000)),
                                                e.n(30))),
                                e.let("omega", mul(e, e.v("cut"), e.n(2 * Math.PI / RATE))),
                                e.let("sn", call(e, "sinf", e.v("omega"))),
                                e.let("cs", call(e, "cosf", e.v("omega"))),
                                e.let(
                                        "alpha",
                                        e.ifElse(
                                                lt(e, tt, e.n(2)),
                                                div(
                                                        e,
                                                        e.v("sn"),
                                                        add(
                                                                e,
                                                                mul(
                                                                        e,
                                                                        call(
                                                                                e,
                                                                                "powf",
                                                                                div(
                                                                                        e,
                                                                                        div(
                                                                                                e,
                                                                                                mul(
                                                                                                        e,
                                                                                                        f32(e, r),
                                                                                                        f32(
                                                                                                                e,
                                                                                                                add(
                                                                                                                        e,
                                                                                                                        f,
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        70)))),
                                                                                                f32(e, e.n(197))),
                                                                                        f32(e, e.n(127))),
                                                                                f32(e, e.n(4))),
                                                                        e.n(150)),
                                                                e.n(0.1))),
                                                mul(
                                                        e,
                                                        e.v("sn"),
                                                        call(
                                                                e,
                                                                "sinhf",
                                                                div(
                                                                        e,
                                                                        mul(
                                                                                e,
                                                                                add(
                                                                                        e,
                                                                                        mul(
                                                                                                e,
                                                                                                call(
                                                                                                        e,
                                                                                                        "powf",
                                                                                                        div(
                                                                                                                e,
                                                                                                                f32(
                                                                                                                        e,
                                                                                                                        r),
                                                                                                                f32(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        127))),
                                                                                                        f32(e, e.n(4))),
                                                                                                e.n(4)),
                                                                                        e.n(0.1)),
                                                                                e.v("omega")),
                                                                        e.v("sn")))))),
                                e.let("a0", add(e, f32(e, e.n(1)), e.v("alpha"))),
                                e.let("a1", mul(e, e.v("cs"), e.n(-2))),
                                e.let("a2", sub(e, f32(e, e.n(1)), e.v("alpha"))),
                                e.let(
                                        "b0",
                                        e.ifElse(
                                                eq(e, tt, e.n(0)),
                                                div(e, sub(e, f32(e, e.n(1)), e.v("cs")), e.n(2)),
                                                e.ifElse(
                                                        eq(e, tt, e.n(1)),
                                                        div(e, add(e, f32(e, e.n(1)), e.v("cs")), e.n(2)),
                                                        e.ifElse(eq(e, tt, e.n(2)), e.v("alpha"), f32(e, e.n(1)))))),
                                e.let(
                                        "b1",
                                        e.ifElse(
                                                eq(e, tt, e.n(0)),
                                                sub(e, f32(e, e.n(1)), e.v("cs")),
                                                e.ifElse(
                                                        eq(e, tt, e.n(1)),
                                                        e.mb.operatorNeg(add(e, f32(e, e.n(1)), e.v("cs"))),
                                                        e.ifElse(
                                                                eq(e, tt, e.n(2)),
                                                                f32(e, e.n(0)),
                                                                mul(e, e.v("cs"), e.n(-2)))))),
                                e.let(
                                        "b2",
                                        e.ifElse(
                                                eq(e, tt, e.n(0)),
                                                div(e, sub(e, f32(e, e.n(1)), e.v("cs")), e.n(2)),
                                                e.ifElse(
                                                        eq(e, tt, e.n(1)),
                                                        div(e, add(e, f32(e, e.n(1)), e.v("cs")), e.n(2)),
                                                        e.ifElse(
                                                                eq(e, tt, e.n(2)),
                                                                e.mb.operatorNeg(e.v("alpha")),
                                                                f32(e, e.n(1)))))),
                                e.let(
                                        "base",
                                        mul(
                                                e,
                                                add(e, add(e, mul(e, tt, e.n(128 * 128)), mul(e, f, e.n(128))), r),
                                                e.n(8))),
                                set(e, idx(e, lw(e, "coefs"), e.v("base")), div(e, e.v("b0"), e.v("a0"))),
                                set(
                                        e,
                                        idx(e, lw(e, "coefs"), add(e, e.v("base"), e.n(1))),
                                        div(e, e.v("b1"), e.v("a0"))),
                                set(
                                        e,
                                        idx(e, lw(e, "coefs"), add(e, e.v("base"), e.n(2))),
                                        div(e, e.v("b2"), e.v("a0"))),
                                set(
                                        e,
                                        idx(e, lw(e, "coefs"), add(e, e.v("base"), e.n(3))),
                                        div(e, e.mb.operatorNeg(e.v("a1")), e.v("a0"))),
                                set(
                                        e,
                                        idx(e, lw(e, "coefs"), add(e, e.v("base"), e.n(4))),
                                        div(e, e.mb.operatorNeg(e.v("a2")), e.v("a0"))))));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("i32x8", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS))));
        defs.add(e.let("f32x8", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS))));
        defs.add(e.let(
                "data_t",
                EcoBuzz.struct(
                        e, "m4_waves", EcoBuzz.field(e, "b", EcoBuzz.array(e, e.v("t_i8"), e.intOf(WAVES_BYTES))))));
        defs.add(e.let("p_data_t", EcoBuzz.ptr(e, e.v("data_t"))));
        defs.add(e.let("coefs_t", EcoBuzz.array(e, e.v("t_f32"), e.intOf(COEFS))));
        defs.add(e.let("lfoosc_t", EcoBuzz.array(e, e.v("t_f32"), e.intOf(LFOOSC))));
        defs.add(e.let(
                "wave_bytes",
                e.callM(
                        e.call(e.mb.builtin("file", "open"), e.s("buzz-m4-waves.raw")),
                        "read",
                        e.rec(e.p("offset", e.intOf(0)), e.p("len", e.intOf(WAVES_BYTES))))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "data", e.v("p_data_t")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        for (var f : I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_i32")));
        for (var f : F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_f32")));
        for (var f : T_I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32x8")));
        for (var f : T_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32x8")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "m4_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("track_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let("osc_fn_t", EcoBuzz.fnType(e, e.v("t_i32"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let("vca_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let("filter_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"), e.v("t_f32"))));
        defs.add(e.let("tables_fn_t", EcoBuzz.fnType(e, e.v("t_void"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("f2_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"), e.v("t_f32"))));

        var body = new TList<SourceExpr>();
        body.add(e.let("osc_at", e.callM0(e.v("arg"), "osc_at")));
        body.add(e.let("zero", e.callM0(e.v("arg"), "zero")));
        for (var fn : new String[] {"sinf", "cosf", "sinhf", "logf", "exp2f"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, e.v("ll"), fn, e.lowerV("f1_fn_t"))));
        }
        body.add(e.let("powf", EcoBuzz.externDef(e, e.v("ll"), "powf", e.lowerV("f2_fn_t"))));
        body.add(e.let(
                "m4_waves",
                e.call(
                        EcoBuzz.llvmL(e, "global_const_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("m4_waves")),
                                e.p("ty", e.lowerV("data_t")),
                                e.p("init", e.rec(e.p("b", e.lowerV("wave_bytes"))))))));
        body.add(e.let(
                "coefs",
                e.call(
                        EcoBuzz.llvmL(e, "global_zero_fn"),
                        e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("m4_coefs")), e.p("ty", e.lowerV("coefs_t"))))));
        body.add(e.let(
                "lfoosc",
                e.call(
                        EcoBuzz.llvmL(e, "global_zero_fn"),
                        e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("m4_lfoosc")), e.p("ty", e.lowerV("lfoosc_t"))))));
        body.add(e.let(
                "tables_ready",
                e.call(
                        EcoBuzz.llvmL(e, "global_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("m4_tables_ready")),
                                e.p("ty", e.lowerV("t_i32")),
                                e.p("init", e.n(0))))));

        var f = e.v("f");
        var coefsBody = e.seq(ifOnly(
                e,
                eq(e, lw(e, "tables_ready"), e.n(0)),
                set(e, lw(e, "tables_ready"), e.n(1)),
                coefTable(e),
                forRange(
                        e,
                        "pp",
                        e.n(LFOOSC),
                        set(
                                e,
                                idx(e, lw(e, "lfoosc"), e.v("pp")),
                                call(
                                        e,
                                        "powf",
                                        f32(e, e.n(1.00004230724139582)),
                                        f32(e, sub(e, e.v("pp"), e.n(0x8000))))))));
        body.add(e.let(
                "tables",
                EcoBuzz.funcDef(e, e.v("ll"), "m4_tables", new String[] {}, e.lowerV("tables_fn_t"), (coefsBody))));

        var t = e.v("t");
        var initBody = new TList<SourceExpr>();
        initBody.add(call(e, "tables", e.tup()));
        initBody.add(set(e, p(e, "data"), lw(e, "m4_waves")));
        initBody.add(forRange(e, "k", e.n(NG), set(e, idx(e, p(e, "gval"), e.v("k")), e.n(EcoBuzz.NO_VALUE))));
        initBody.add(forRange(
                e,
                "t",
                e.n(TRACKS),
                forRange(e, "k", e.n(NT), set(e, idx(e, idx(e, p(e, "tval"), t), e.v("k")), e.n(EcoBuzz.NO_VALUE)))));
        initBody.add(set(e, p(e, "tsdsf"), e.n(TSDSF)));
        initBody.add(set(e, p(e, "ftype_base"), e.n(0)));
        initBody.add(set(e, p(e, "cutoff"), e.n(32)));
        initBody.add(set(e, p(e, "resonance"), e.n(32)));
        initBody.add(set(e, p(e, "db24"), e.n(0)));
        initBody.add(set(e, p(e, "db18"), e.n(0)));
        initBody.add(set(e, p(e, "peg_att"), envSamples(e, EcoBuzz.i32(e, e.n(7)))));
        initBody.add(set(e, p(e, "peg_dec"), envSamples(e, EcoBuzz.i32(e, e.n(11)))));
        initBody.add(set(e, p(e, "penv_mod"), e.n(0)));
        initBody.add(set(e, p(e, "pitch_mod"), e.n(0)));
        initBody.add(set(e, p(e, "feg_att"), envSamples(e, EcoBuzz.i32(e, e.n(7)))));
        initBody.add(set(e, p(e, "feg_sus"), envSamples(e, EcoBuzz.i32(e, e.n(14)))));
        initBody.add(set(e, p(e, "feg_rel"), envSamples(e, EcoBuzz.i32(e, e.n(15)))));
        initBody.add(set(e, p(e, "fenv_mod"), e.n(0)));
        initBody.add(set(e, p(e, "aeg_att"), envSamples(e, EcoBuzz.i32(e, e.n(5)))));
        initBody.add(set(e, p(e, "aeg_sus"), envSamples(e, EcoBuzz.i32(e, e.n(16)))));
        initBody.add(set(e, p(e, "aeg_rel"), envSamples(e, EcoBuzz.i32(e, e.n(32)))));
        for (var z : new String[] {
            "wave1",
            "wave2",
            "wavesub",
            "noise1",
            "noise2",
            "sync",
            "lfo1_noise",
            "lfo2_noise",
            "lfo1_synced",
            "lfo2_synced",
            "phase_lfo1",
            "phase_lfo2",
            "lfo1_wave",
            "lfo2_wave",
            "padd_lfo1",
            "padd_lfo2",
            "lfo1_amount",
            "lfo2_amount",
            "lfo1_freq",
            "lfo2_freq",
            "mix_type",
            "f_osc1",
            "f_pw1",
            "f_amp",
            "f_cut",
            "f_osc2",
            "f_pw2",
            "f_mix",
            "f_reso",
            "glide",
            "glide_time",
            "l1_osc1",
            "l1_vol",
            "l1_cut",
            "l2_osc2",
            "l2_mix",
            "l2_reso"
        }) {
            initBody.add(set(e, p(e, z), e.n(0)));
        }
        initBody.add(set(e, p(e, "detune_semi"), e.n(1)));
        initBody.add(set(e, p(e, "detune_fine"), e.n(1)));
        initBody.add(set(e, p(e, "l1_pw1"), e.n(0)));
        initBody.add(set(e, p(e, "l2_pw2"), e.n(0)));
        initBody.add(set(e, p(e, "sub_vol"), e.n(64)));
        initBody.add(set(e, p(e, "center1"), e.n(64 / 127.0)));
        initBody.add(set(e, p(e, "center2"), e.n(64 / 127.0)));
        initBody.add(set(e, p(e, "lfo1_pd"), e.n(64 << 25)));
        initBody.add(set(e, p(e, "lfo2_pd"), e.n(64 << 25)));
        initBody.add(set(e, p(e, "bal1"), e.n(63)));
        initBody.add(set(e, p(e, "bal2"), e.n(64)));
        initBody.add(set(e, p(e, "ntracks"), e.n(TRACKS)));
        var trackInit = new TList<SourceExpr>();
        for (var z : new String[] {
            "t_aeg_state", "t_feg_state", "t_peg_state", "t_phase1", "t_phase2", "t_ph1", "t_ph2", "t_phasesub",
            "t_amp", "t_glide_act", "t_pm_act", "t_padd1", "t_padd2", "t_glide_cnt", "t_peg_cnt", "t_amp_add",
            "t_phl1", "t_phl2"
        }) {
            trackInit.add(set(e, pt(e, z), e.n(0)));
        }
        for (var z : new String[] {
            "t_x1",
            "t_x2",
            "t_y1",
            "t_y2",
            "t_x241",
            "t_x242",
            "t_y241",
            "t_y242",
            "t_old_out",
            "t_freq",
            "t_freq_from",
            "t_cut",
            "t_cut_add",
            "t_glide_mul",
            "t_glide_fac",
            "t_pitch_mul",
            "t_pitch_fac"
        }) {
            trackInit.add(set(e, pt(e, z), e.n(0)));
        }
        trackInit.add(set(e, pt(e, "t_r1"), e.n(26474)));
        trackInit.add(set(e, pt(e, "t_r2"), e.n(13075)));
        trackInit.add(set(e, pt(e, "t_r3"), e.n(18376)));
        trackInit.add(set(e, pt(e, "t_r4"), e.n(31291)));
        trackInit.add(set(e, pt(e, "t_aeg_cnt"), e.n(-1)));
        trackInit.add(set(e, pt(e, "t_feg_cnt"), e.n(-1)));
        trackInit.add(set(e, pt(e, "t_center1"), p(e, "center1")));
        trackInit.add(set(e, pt(e, "t_center2"), p(e, "center2")));
        trackInit.add(set(e, pt(e, "t_ps1a"), div(e, f32(e, e.n(0.5)), pt(e, "t_center1"))));
        trackInit.add(set(e, pt(e, "t_ps1b"), div(e, f32(e, e.n(0.5)), sub(e, f32(e, e.n(1)), pt(e, "t_center1")))));
        trackInit.add(set(e, pt(e, "t_ps2a"), div(e, f32(e, e.n(0.5)), pt(e, "t_center2"))));
        trackInit.add(set(e, pt(e, "t_ps2b"), div(e, f32(e, e.n(0.5)), sub(e, f32(e, e.n(1)), pt(e, "t_center2")))));
        trackInit.add(set(e, pt(e, "t_c1"), i32(e, mul(e, pt(e, "t_center1"), e.n(PHASE_ONE)))));
        trackInit.add(set(e, pt(e, "t_c2"), i32(e, mul(e, pt(e, "t_center2"), e.n(PHASE_ONE)))));
        trackInit.add(set(e, pt(e, "t_volume"), e.n(64 << 20)));
        initBody.add(forRange(e, "t", e.n(TRACKS), trackInit.toArray(SourceExpr[]::new)));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "m4_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        (e.seq(initBody.toArray(SourceExpr[]::new))))));

        var note = e.v("note");
        var trackTick = e.seq(
                e.let("vol", idx(e, idx(e, p(e, "tval"), t), e.n(1))),
                ifOnly(e, ne(e, e.v("vol"), e.n(-1)), set(e, pt(e, "t_volume"), shl(e, e.v("vol"), e.n(20)))),
                e.let("note", idx(e, idx(e, p(e, "tval"), t), e.n(0))),
                ifOnly(
                        e,
                        ne(e, note, e.n(-1)),
                        e.ifElse(
                                and(e, ge(e, note, e.n(1)), le(e, note, e.n(156))),
                                e.seq(
                                        set(e, pt(e, "t_freq_from"), pt(e, "t_freq")),
                                        set(
                                                e,
                                                pt(e, "t_freq"),
                                                mul(
                                                        e,
                                                        call(
                                                                e,
                                                                "exp2f",
                                                                div(
                                                                        e,
                                                                        f32(
                                                                                e,
                                                                                sub(
                                                                                        e,
                                                                                        add(
                                                                                                e,
                                                                                                mul(
                                                                                                        e,
                                                                                                        e.mb
                                                                                                                .operatorShr(
                                                                                                                        note,
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        4)),
                                                                                                        e.n(12)),
                                                                                                and(e, note, e.n(15))),
                                                                                        e.n(1))),
                                                                        f32(e, e.n(12)))),
                                                        e.n(16.3516))),
                                        e.ifElse(
                                                ne(e, p(e, "glide"), e.n(0)),
                                                e.seq(
                                                        set(e, pt(e, "t_glide_act"), e.n(1)),
                                                        set(
                                                                e,
                                                                pt(e, "t_glide_mul"),
                                                                call(
                                                                        e,
                                                                        "powf",
                                                                        e.ifElse(
                                                                                gt(
                                                                                        e,
                                                                                        pt(e, "t_freq"),
                                                                                        pt(e, "t_freq_from")),
                                                                                f32(e, e.n(2)),
                                                                                f32(e, e.n(0.5))),
                                                                        div(
                                                                                e,
                                                                                f32(e, e.n(1)),
                                                                                f32(e, p(e, "glide_time"))))),
                                                        set(e, pt(e, "t_glide_fac"), e.n(1)),
                                                        set(
                                                                e,
                                                                pt(e, "t_glide_cnt"),
                                                                e.ifElse(
                                                                        le(e, pt(e, "t_freq_from"), e.n(0)),
                                                                        EcoBuzz.i32(e, e.n(-2147483648.0)),
                                                                        i32(
                                                                                e,
                                                                                div(
                                                                                        e,
                                                                                        call(
                                                                                                e,
                                                                                                "logf",
                                                                                                div(
                                                                                                        e,
                                                                                                        pt(e, "t_freq"),
                                                                                                        pt(
                                                                                                                e,
                                                                                                                "t_freq_from"))),
                                                                                        call(
                                                                                                e,
                                                                                                "logf",
                                                                                                pt(
                                                                                                        e,
                                                                                                        "t_glide_mul"))))))),
                                                set(e, pt(e, "t_glide_act"), e.n(0))),
                                        set(e, pt(e, "t_aeg_state"), e.n(1)),
                                        set(e, pt(e, "t_aeg_cnt"), p(e, "aeg_att")),
                                        set(e, pt(e, "t_amp_add"), div(e, pt(e, "t_volume"), p(e, "aeg_att"))),
                                        set(e, pt(e, "t_amp"), e.n(0)),
                                        e.ifElse(
                                                ne(e, p(e, "pitch_mod"), e.n(0)),
                                                e.seq(
                                                        set(e, pt(e, "t_pm_act"), e.n(1)),
                                                        set(e, pt(e, "t_peg_state"), e.n(1)),
                                                        set(e, pt(e, "t_peg_cnt"), p(e, "peg_att")),
                                                        set(
                                                                e,
                                                                pt(e, "t_pitch_mul"),
                                                                call(
                                                                        e,
                                                                        "powf",
                                                                        call(
                                                                                e,
                                                                                "powf",
                                                                                f32(e, e.n(1.01)),
                                                                                f32(e, p(e, "penv_mod"))),
                                                                        div(
                                                                                e,
                                                                                f32(e, e.n(1)),
                                                                                f32(e, p(e, "peg_att"))))),
                                                        set(e, pt(e, "t_pitch_fac"), e.n(1))),
                                                set(e, pt(e, "t_pm_act"), e.n(0))),
                                        set(e, pt(e, "t_feg_state"), e.n(1)),
                                        set(e, pt(e, "t_feg_cnt"), p(e, "feg_att")),
                                        set(
                                                e,
                                                pt(e, "t_cut_add"),
                                                div(e, f32(e, p(e, "fenv_mod")), f32(e, p(e, "feg_att")))),
                                        set(e, pt(e, "t_cut"), e.n(0))),
                                ifOnly(e, eq(e, note, e.n(EcoBuzz.NOTE_OFF)), set(e, pt(e, "t_aeg_state"), e.n(0))))),
                e.ifElse(
                        ne(e, pt(e, "t_glide_act"), e.n(0)),
                        setPhaseAdds(e, pt(e, "t_freq_from")),
                        setPhaseAdds(e, pt(e, "t_freq"))));
        body.add(e.let(
                "track_tick",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "m4_track_tick", new String[] {"p", "t"}, e.lowerV("track_fn_t"), (trackTick))));

        var tick = new TList<SourceExpr>();
        tick.add(onGval(
                e,
                18,
                e.ifElse(
                        eq(e, e.v("v"), e.n(0)),
                        e.seq(
                                set(e, p(e, "db18"), e.n(0)),
                                set(e, p(e, "db24"), e.n(1)),
                                set(e, p(e, "ftype_base"), e.n(0))),
                        e.ifElse(
                                eq(e, e.v("v"), e.n(1)),
                                e.seq(
                                        set(e, p(e, "db18"), e.n(1)),
                                        set(e, p(e, "db24"), e.n(1)),
                                        set(e, p(e, "ftype_base"), e.n(0))),
                                e.seq(
                                        set(e, p(e, "db18"), e.n(0)),
                                        set(e, p(e, "db24"), e.n(0)),
                                        set(
                                                e,
                                                p(e, "ftype_base"),
                                                mul(e, sub(e, e.v("v"), e.n(2)), e.n(128 * 128 * 8))))))));
        tick.add(onGval(e, 19, set(e, p(e, "cutoff"), e.v("v"))));
        tick.add(onGval(e, 20, set(e, p(e, "resonance"), e.v("v"))));
        tick.add(onGval(e, 21, set(e, p(e, "feg_att"), envSamples(e, e.v("v")))));
        tick.add(onGval(e, 22, set(e, p(e, "feg_sus"), envSamples(e, e.v("v")))));
        tick.add(onGval(e, 23, set(e, p(e, "feg_rel"), envSamples(e, e.v("v")))));
        tick.add(onGval(e, 24, set(e, p(e, "fenv_mod"), shl(e, sub(e, e.v("v"), e.n(64)), e.n(1)))));
        tick.add(onGval(e, 11, set(e, p(e, "peg_att"), envSamples(e, e.v("v")))));
        tick.add(onGval(e, 12, set(e, p(e, "peg_dec"), envSamples(e, e.v("v")))));
        tick.add(onGval(
                e,
                13,
                e.ifElse(
                        ne(e, sub(e, e.v("v"), e.n(64)), e.n(0)),
                        set(e, p(e, "pitch_mod"), e.n(1)),
                        e.seq(
                                set(e, p(e, "pitch_mod"), e.n(0)),
                                forRange(e, "t", p(e, "ntracks"), set(e, pt(e, "t_pm_act"), e.n(0))))),
                set(e, p(e, "penv_mod"), sub(e, e.v("v"), e.n(64)))));
        tick.add(onGval(
                e,
                8,
                set(e, p(e, "bal1"), sub(e, EcoBuzz.i32(e, e.n(127)), e.v("v"))),
                set(e, p(e, "bal2"), e.v("v"))));
        tick.add(onGval(
                e,
                14,
                e.ifElse(
                        eq(e, e.v("v"), e.n(0)),
                        e.seq(
                                set(e, p(e, "glide"), e.n(0)),
                                forRange(e, "t", p(e, "ntracks"), set(e, pt(e, "t_glide_act"), e.n(0)))),
                        e.seq(
                                set(e, p(e, "glide"), e.n(1)),
                                set(e, p(e, "glide_time"), div(e, mul(e, e.v("v"), e.n(10000000)), e.n((int)
                                        RATE)))))));
        tick.add(onGval(e, 9, set(e, p(e, "wavesub"), e.v("v"))));
        tick.add(onGval(e, 10, set(e, p(e, "sub_vol"), e.v("v"))));
        tick.add(onGval(e, 1, set(e, p(e, "center1"), div(e, f32(e, e.v("v")), e.n(127)))));
        tick.add(onGval(e, 3, set(e, p(e, "center2"), div(e, f32(e, e.v("v")), e.n(127)))));
        tick.add(onGval(
                e,
                4,
                set(
                        e,
                        p(e, "detune_semi"),
                        call(e, "powf", f32(e, e.n(1.05946309435929526)), f32(e, sub(e, e.v("v"), e.n(64)))))));
        tick.add(onGval(
                e,
                5,
                set(
                        e,
                        p(e, "detune_fine"),
                        call(e, "powf", f32(e, e.n(1.00091728179580156)), f32(e, sub(e, e.v("v"), e.n(64)))))));
        tick.add(onGval(e, 6, set(e, p(e, "sync"), e.v("v"))));
        tick.add(onGval(e, 7, set(e, p(e, "mix_type"), e.v("v"))));
        tick.add(onGval(
                e,
                0,
                e.ifElse(
                        eq(e, e.v("v"), e.n(NUMWAVES)),
                        set(e, p(e, "noise1"), e.n(1)),
                        e.seq(set(e, p(e, "noise1"), e.n(0)), set(e, p(e, "wave1"), e.v("v"))))));
        tick.add(onGval(
                e,
                2,
                e.ifElse(
                        eq(e, e.v("v"), e.n(NUMWAVES)),
                        set(e, p(e, "noise2"), e.n(1)),
                        e.seq(set(e, p(e, "noise2"), e.n(0)), set(e, p(e, "wave2"), e.v("v"))))));
        tick.add(onGval(e, 15, set(e, p(e, "aeg_att"), envSamples(e, e.v("v")))));
        tick.add(onGval(e, 16, set(e, p(e, "aeg_sus"), envSamples(e, e.v("v")))));
        tick.add(onGval(e, 17, set(e, p(e, "aeg_rel"), envSamples(e, e.v("v")))));
        tick.add(lfoFlags(e, 25, "f_osc1", "f_pw1", "f_amp", "f_cut"));
        tick.add(onGval(
                e,
                26,
                set(e, p(e, "lfo1_wave"), e.v("v")),
                set(e, p(e, "lfo1_noise"), i32(e, eq(e, e.v("v"), e.n(4))))));
        tick.add(onGval(
                e,
                27,
                e.ifElse(
                        gt(e, e.v("v"), e.n(116)),
                        e.seq(
                                set(e, p(e, "lfo1_synced"), e.n(1)),
                                set(e, p(e, "lfo1_freq"), sub(e, e.v("v"), e.n(117)))),
                        e.seq(set(e, p(e, "lfo1_synced"), e.n(0)), set(e, p(e, "lfo1_freq"), e.v("v"))))));
        tick.add(onGval(e, 28, set(e, p(e, "lfo1_amount"), e.v("v"))));
        tick.add(set(e, p(e, "padd_lfo1"), lfoPhaseAdd(e, "lfo1_synced", "lfo1_noise", "lfo1_freq")));
        tick.add(lfoFlags(e, 30, "f_osc2", "f_pw2", "f_mix", "f_reso"));
        tick.add(onGval(
                e,
                31,
                set(e, p(e, "lfo2_wave"), e.v("v")),
                set(e, p(e, "lfo2_noise"), i32(e, eq(e, e.v("v"), e.n(4))))));
        tick.add(onGval(
                e,
                32,
                e.ifElse(
                        gt(e, e.v("v"), e.n(116)),
                        e.seq(
                                set(e, p(e, "lfo2_synced"), e.n(1)),
                                set(e, p(e, "lfo2_freq"), sub(e, e.v("v"), e.n(117)))),
                        e.seq(set(e, p(e, "lfo2_synced"), e.n(0)), set(e, p(e, "lfo2_freq"), e.v("v"))))));
        tick.add(onGval(e, 33, set(e, p(e, "lfo2_amount"), e.v("v"))));
        tick.add(onGval(e, 29, set(e, p(e, "lfo1_pd"), shl(e, e.v("v"), e.n(25)))));
        tick.add(onGval(e, 34, set(e, p(e, "lfo2_pd"), shl(e, e.v("v"), e.n(25)))));
        tick.add(set(e, p(e, "padd_lfo2"), lfoPhaseAdd(e, "lfo2_synced", "lfo2_noise", "lfo2_freq")));
        tick.add(set(e, p(e, "l1_osc1"), e.mb.operatorShr(mul(e, p(e, "lfo1_amount"), e.n(127)), e.n(7))));
        tick.add(set(
                e, p(e, "l1_pw1"), div(e, f32(e, mul(e, p(e, "lfo1_amount"), e.n(127))), e.n(128.0 * 127.0 * 0x8000))));
        tick.add(set(e, p(e, "l1_vol"), e.mb.operatorShr(mul(e, p(e, "lfo1_amount"), e.n(127)), e.n(7))));
        tick.add(set(e, p(e, "l1_cut"), e.mb.operatorShr(mul(e, p(e, "lfo1_amount"), e.n(127)), e.n(7))));
        tick.add(set(e, p(e, "l2_osc2"), e.mb.operatorShr(mul(e, p(e, "lfo2_amount"), e.n(127)), e.n(7))));
        tick.add(set(
                e, p(e, "l2_pw2"), div(e, f32(e, mul(e, p(e, "lfo2_amount"), e.n(127))), e.n(128.0 * 127.0 * 0x8000))));
        tick.add(set(e, p(e, "l2_mix"), e.mb.operatorShr(mul(e, p(e, "lfo2_amount"), e.n(127)), e.n(7))));
        tick.add(set(e, p(e, "l2_reso"), e.mb.operatorShr(mul(e, p(e, "lfo2_amount"), e.n(127)), e.n(7))));
        tick.add(forRange(e, "t", p(e, "ntracks"), call(e, "track_tick", e.v("p"), t)));
        tick.add(forRange(e, "k", e.n(NG), set(e, idx(e, p(e, "gval"), e.v("k")), e.n(EcoBuzz.NO_VALUE))));
        tick.add(forRange(
                e,
                "t",
                e.n(TRACKS),
                forRange(e, "k", e.n(NT), set(e, idx(e, idx(e, p(e, "tval"), t), e.v("k")), e.n(EcoBuzz.NO_VALUE)))));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "m4_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        (e.seq(tick.toArray(SourceExpr[]::new))))));

        var osc = e.seq(
                e.let("b1", EcoBuzz.i32(e, e.n(0))),
                e.let("b2", EcoBuzz.i32(e, e.n(0))),
                e.ifElse(
                        ne(e, p(e, "f_mix"), e.n(0)),
                        e.seq(
                                e.let(
                                        "bb",
                                        add(
                                                e,
                                                p(e, "bal2"),
                                                e.mb.operatorShr(
                                                        mul(e, lfoTab(e, "lfo2_wave", "t_phl2"), p(e, "l2_mix")),
                                                        e.n(15)))),
                                set(
                                        e,
                                        e.v("b2"),
                                        e.ifElse(
                                                lt(e, e.v("bb"), e.n(0)),
                                                EcoBuzz.i32(e, e.n(0)),
                                                e.ifElse(
                                                        gt(e, e.v("bb"), e.n(127)),
                                                        EcoBuzz.i32(e, e.n(127)),
                                                        e.v("bb")))),
                                set(e, e.v("b1"), sub(e, EcoBuzz.i32(e, e.n(127)), e.v("b2")))),
                        e.seq(set(e, e.v("b1"), p(e, "bal1")), set(e, e.v("b2"), p(e, "bal2")))),
                e.let("o", EcoBuzz.i32(e, e.n(0))),
                e.let("o2", EcoBuzz.i32(e, e.n(0))),
                oscSource(e, "noise1", "wave1", "t_ph1", "b1", "o"),
                oscSource(e, "noise2", "wave2", "t_ph2", "b2", "o2"),
                e.let("mt", p(e, "mix_type")),
                e.let(
                        "m",
                        e.ifElse(
                                eq(e, e.v("mt"), e.n(0)),
                                add(e, e.v("o"), e.v("o2")),
                                e.ifElse(
                                        eq(e, e.v("mt"), e.n(1)),
                                        sub(e, shl(e, iabs(e, sub(e, e.v("o"), e.v("o2"))), e.n(1)), e.n(0x8000)),
                                        e.ifElse(
                                                eq(e, e.v("mt"), e.n(2)),
                                                e.mb.operatorShr(mul(e, e.v("o"), e.v("o2")), e.n(15)),
                                                e.ifElse(
                                                        eq(e, e.v("mt"), e.n(3)),
                                                        e.ifElse(
                                                                lt(e, iabs(e, e.v("o")), iabs(e, e.v("o2"))),
                                                                e.v("o2"),
                                                                e.v("o")),
                                                        e.ifElse(
                                                                eq(e, e.v("mt"), e.n(4)),
                                                                e.ifElse(
                                                                        gt(e, iabs(e, e.v("o")), iabs(e, e.v("o2"))),
                                                                        e.v("o2"),
                                                                        e.v("o")),
                                                                e.ifElse(
                                                                        eq(e, e.v("mt"), e.n(5)),
                                                                        and(e, e.v("o"), e.v("o2")),
                                                                        e.ifElse(
                                                                                eq(e, e.v("mt"), e.n(6)),
                                                                                or(e, e.v("o"), e.v("o2")),
                                                                                e.mb.operatorXor(
                                                                                        e.v("o"), e.v("o2")))))))))),
                e.ret(add(
                        e,
                        e.v("m"),
                        e.mb.operatorShr(
                                mul(
                                        e,
                                        wave16(
                                                e,
                                                add(
                                                        e,
                                                        shl(e, p(e, "wavesub"), e.n(11)),
                                                        e.mb.operatorShr(pt(e, "t_phasesub"), e.n(16)))),
                                        p(e, "sub_vol")),
                                e.n(7)))));
        body.add(e.let(
                "osc", EcoBuzz.funcDef(e, e.v("ll"), "m4_osc", new String[] {"p", "t"}, e.lowerV("osc_fn_t"), (osc))));

        var vca = e.seq(
                e.let("c", pt(e, "t_aeg_cnt")),
                set(e, pt(e, "t_aeg_cnt"), sub(e, e.v("c"), e.n(1))),
                ifOnly(
                        e,
                        eq(e, e.v("c"), e.n(0)),
                        set(e, pt(e, "t_aeg_state"), add(e, pt(e, "t_aeg_state"), e.n(1))),
                        e.let("s", pt(e, "t_aeg_state")),
                        ifOnly(
                                e,
                                eq(e, e.v("s"), e.n(2)),
                                set(e, pt(e, "t_aeg_cnt"), p(e, "aeg_sus")),
                                set(e, pt(e, "t_amp"), pt(e, "t_volume")),
                                set(e, pt(e, "t_amp_add"), e.n(0))),
                        ifOnly(
                                e,
                                eq(e, e.v("s"), e.n(3)),
                                set(e, pt(e, "t_aeg_cnt"), p(e, "aeg_rel")),
                                set(
                                        e,
                                        pt(e, "t_amp_add"),
                                        div(e, e.mb.operatorNeg(pt(e, "t_volume")), p(e, "aeg_rel")))),
                        ifOnly(
                                e,
                                eq(e, e.v("s"), e.n(4)),
                                set(e, pt(e, "t_aeg_state"), e.n(0)),
                                set(e, pt(e, "t_aeg_cnt"), e.n(-1)),
                                set(e, pt(e, "t_amp"), e.n(0)))),
                set(e, pt(e, "t_amp"), add(e, pt(e, "t_amp"), pt(e, "t_amp_add"))),
                e.let(
                        "a",
                        add(
                                e,
                                f32(e, pt(e, "t_amp")),
                                f32(e, shl(e, mul(e, lfoTab(e, "lfo1_wave", "t_phl1"), p(e, "l1_vol")), e.n(5))))),
                e.let(
                        "amp",
                        e.ifElse(
                                ne(e, p(e, "f_amp"), e.n(0)),
                                e.ifElse(lt(e, e.v("a"), e.n(0)), f32(e, e.n(0)), e.v("a")),
                                f32(e, pt(e, "t_amp")))),
                e.ret(mul(e, e.v("amp"), e.n(1.0 / PHASE_ONE))));
        body.add(e.let(
                "vca", EcoBuzz.funcDef(e, e.v("ll"), "m4_vca", new String[] {"p", "t"}, e.lowerV("vca_fn_t"), (vca))));

        var filter = e.seq(
                ifOnly(
                        e,
                        ne(e, pt(e, "t_feg_state"), e.n(0)),
                        e.let("c", pt(e, "t_feg_cnt")),
                        set(e, pt(e, "t_feg_cnt"), sub(e, e.v("c"), e.n(1))),
                        ifOnly(
                                e,
                                eq(e, e.v("c"), e.n(0)),
                                set(e, pt(e, "t_feg_state"), add(e, pt(e, "t_feg_state"), e.n(1))),
                                e.let("s", pt(e, "t_feg_state")),
                                ifOnly(
                                        e,
                                        eq(e, e.v("s"), e.n(2)),
                                        set(e, pt(e, "t_feg_cnt"), p(e, "feg_sus")),
                                        set(e, pt(e, "t_cut"), f32(e, p(e, "fenv_mod"))),
                                        set(e, pt(e, "t_cut_add"), e.n(0))),
                                ifOnly(
                                        e,
                                        eq(e, e.v("s"), e.n(3)),
                                        set(e, pt(e, "t_feg_cnt"), p(e, "feg_rel")),
                                        set(
                                                e,
                                                pt(e, "t_cut_add"),
                                                div(
                                                        e,
                                                        f32(e, e.mb.operatorNeg(p(e, "fenv_mod"))),
                                                        f32(e, p(e, "feg_rel"))))),
                                ifOnly(
                                        e,
                                        eq(e, e.v("s"), e.n(4)),
                                        set(e, pt(e, "t_feg_state"), e.n(0)),
                                        set(e, pt(e, "t_feg_cnt"), e.n(-1)),
                                        set(e, pt(e, "t_cut"), e.n(0)),
                                        set(e, pt(e, "t_cut_add"), e.n(0)))),
                        set(e, pt(e, "t_cut"), add(e, pt(e, "t_cut"), pt(e, "t_cut_add")))),
                e.let(
                        "c0",
                        e.ifElse(
                                ne(e, p(e, "f_cut"), e.n(0)),
                                i32(
                                        e,
                                        add(
                                                e,
                                                add(e, f32(e, p(e, "cutoff")), pt(e, "t_cut")),
                                                f32(
                                                        e,
                                                        e.mb.operatorShr(
                                                                mul(
                                                                        e,
                                                                        lfoTab(e, "lfo1_wave", "t_phl1"),
                                                                        p(e, "l1_cut")),
                                                                e.n(15))))),
                                i32(e, add(e, f32(e, p(e, "cutoff")), pt(e, "t_cut"))))),
                e.let(
                        "c",
                        e.ifElse(
                                lt(e, e.v("c0"), e.n(0)),
                                EcoBuzz.i32(e, e.n(0)),
                                e.ifElse(gt(e, e.v("c0"), e.n(127)), EcoBuzz.i32(e, e.n(127)), e.v("c0")))),
                e.let(
                        "r0",
                        e.ifElse(
                                ne(e, p(e, "f_reso"), e.n(0)),
                                add(
                                        e,
                                        p(e, "resonance"),
                                        e.mb.operatorShr(
                                                mul(e, lfoTab(e, "lfo2_wave", "t_phl2"), p(e, "l2_reso")), e.n(15))),
                                p(e, "resonance"))),
                e.let(
                        "r",
                        e.ifElse(
                                lt(e, e.v("r0"), e.n(0)),
                                EcoBuzz.i32(e, e.n(0)),
                                e.ifElse(gt(e, e.v("r0"), e.n(127)), EcoBuzz.i32(e, e.n(127)), e.v("r0")))),
                e.let("ofs", add(e, shl(e, add(e, shl(e, e.v("c"), e.n(7)), e.v("r")), e.n(3)), p(e, "ftype_base"))),
                e.let("y", f32(e, e.n(0))),
                e.let("y24", f32(e, e.n(0))),
                biquad(e, "t_x1", "t_x2", "t_y1", "t_y2", e.v("x"), "y"),
                e.ret(e.mb.labeled(
                        "flt",
                        e.seq(
                                ifOnly(
                                        e,
                                        ne(e, p(e, "db24"), e.n(0)),
                                        biquad(e, "t_x241", "t_x242", "t_y241", "t_y242", e.v("y"), "y24"),
                                        e.mb.return_(
                                                "flt",
                                                e.ifElse(
                                                        eq(e, p(e, "db18"), e.n(0)),
                                                        e.v("y24"),
                                                        mul(e, add(e, e.v("y"), e.v("y24")), e.n(0.5))))),
                                e.mb.return_("flt", e.v("y"))))));
        body.add(e.let(
                "filter",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "m4_filter", new String[] {"p", "t", "x"}, e.lowerV("filter_fn_t"), (filter))));

        var glideStep = e.seq(
                set(e, pt(e, "t_glide_fac"), mul(e, pt(e, "t_glide_fac"), pt(e, "t_glide_mul"))),
                e.let("gc", pt(e, "t_glide_cnt")),
                set(e, pt(e, "t_glide_cnt"), sub(e, e.v("gc"), e.n(1))),
                ifOnly(
                        e,
                        eq(e, e.v("gc"), e.n(0)),
                        set(e, pt(e, "t_glide_act"), e.n(0)),
                        setPhaseAdds(e, pt(e, "t_freq"))));
        var newPhases = e.seq(
                e.ifElse(
                        ne(e, pt(e, "t_pm_act"), e.n(0)),
                        e.seq(
                                e.ifElse(
                                        ne(e, pt(e, "t_glide_act"), e.n(0)),
                                        e.seq(
                                                phaseStep(e, mul(e, pt(e, "t_glide_fac"), pt(e, "t_pitch_fac"))),
                                                glideStep),
                                        phaseStep(e, pt(e, "t_pitch_fac"))),
                                set(e, pt(e, "t_pitch_fac"), mul(e, pt(e, "t_pitch_fac"), pt(e, "t_pitch_mul"))),
                                e.let("pc", pt(e, "t_peg_cnt")),
                                set(e, pt(e, "t_peg_cnt"), sub(e, e.v("pc"), e.n(1))),
                                ifOnly(
                                        e,
                                        eq(e, e.v("pc"), e.n(0)),
                                        set(e, pt(e, "t_peg_state"), add(e, pt(e, "t_peg_state"), e.n(1))),
                                        e.ifElse(
                                                eq(e, pt(e, "t_peg_state"), e.n(2)),
                                                e.seq(
                                                        set(e, pt(e, "t_peg_cnt"), p(e, "peg_dec")),
                                                        set(
                                                                e,
                                                                pt(e, "t_pitch_mul"),
                                                                call(
                                                                        e,
                                                                        "powf",
                                                                        call(
                                                                                e,
                                                                                "powf",
                                                                                f32(e, e.n(1 / 1.01)),
                                                                                f32(e, p(e, "penv_mod"))),
                                                                        div(
                                                                                e,
                                                                                f32(e, e.n(1)),
                                                                                f32(e, p(e, "peg_dec")))))),
                                                set(e, pt(e, "t_pm_act"), e.n(0))))),
                        e.ifElse(
                                ne(e, pt(e, "t_glide_act"), e.n(0)),
                                e.seq(phaseStep(e, pt(e, "t_glide_fac")), glideStep),
                                phaseStep(e, f32(e, e.n(1))))),
                ifOnly(
                        e,
                        ne(e, and(e, pt(e, "t_phase1"), e.n(-134217728)), e.n(0)),
                        pulseCenter(
                                e,
                                "f_pw1",
                                "center1",
                                "lfo1_wave",
                                "t_phl1",
                                "l1_pw1",
                                "t_center1",
                                "t_ps1a",
                                "t_ps1b",
                                "t_c1"),
                        pulseCenter(
                                e,
                                "f_pw2",
                                "center2",
                                "lfo2_wave",
                                "t_phl2",
                                "l2_pw2",
                                "t_center2",
                                "t_ps2a",
                                "t_ps2b",
                                "t_c2"),
                        ifOnly(e, ne(e, p(e, "sync"), e.n(0)), set(e, pt(e, "t_phase2"), pt(e, "t_phase1")))),
                set(e, pt(e, "t_phase1"), and(e, pt(e, "t_phase1"), e.n(0x7ffffff))),
                set(e, pt(e, "t_phase2"), and(e, pt(e, "t_phase2"), e.n(0x7ffffff))),
                set(e, pt(e, "t_phasesub"), and(e, pt(e, "t_phasesub"), e.n(0x7ffffff))),
                warp(e, "t_phase1", "t_c1", "t_ps1a", "t_ps1b", "t_ph1"),
                warp(e, "t_phase2", "t_c2", "t_ps2a", "t_ps2b", "t_ph2"),
                set(e, pt(e, "t_phl1"), add(e, pt(e, "t_phl1"), p(e, "padd_lfo1"))),
                set(e, pt(e, "t_phl2"), add(e, pt(e, "t_phl2"), p(e, "padd_lfo2"))));
        body.add(e.let(
                "new_phases",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "m4_new_phases", new String[] {"p", "t"}, e.lowerV("track_fn_t"), (newPhases))));

        var i = e.v("i");
        var work = e.seq(
                call(e, "zero", e.v("out"), e.v("n")),
                forRange(
                        e,
                        "t",
                        p(e, "ntracks"),
                        ifOnly(
                                e,
                                ne(e, pt(e, "t_aeg_state"), e.n(0)),
                                set(e, pt(e, "t_phl1"), add(e, p(e, "phase_lfo1"), mul(e, t, p(e, "lfo1_pd")))),
                                set(e, pt(e, "t_phl2"), add(e, p(e, "phase_lfo2"), mul(e, t, p(e, "lfo2_pd")))),
                                forRange(
                                        e,
                                        "i",
                                        e.v("n"),
                                        ifOnly(
                                                e,
                                                ne(e, pt(e, "t_aeg_state"), e.n(0)),
                                                e.let(
                                                        "o",
                                                        mul(
                                                                e,
                                                                f32(e, call(e, "osc", e.v("p"), t)),
                                                                call(e, "vca", e.v("p"), t))),
                                                e.let(
                                                        "y",
                                                        call(
                                                                e,
                                                                "filter",
                                                                e.v("p"),
                                                                t,
                                                                add(e, pt(e, "t_old_out"), e.v("o")))),
                                                set(e, pt(e, "t_old_out"), e.v("o")),
                                                set(
                                                        e,
                                                        idx(e, e.v("out"), mul(e, i, e.n(2))),
                                                        add(e, idx(e, e.v("out"), mul(e, i, e.n(2))), e.v("y"))),
                                                set(
                                                        e,
                                                        idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                                                        add(
                                                                e,
                                                                idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                                                                e.v("y")))),
                                        call(e, "new_phases", e.v("p"), t)))),
                set(e, p(e, "phase_lfo1"), add(e, p(e, "phase_lfo1"), mul(e, p(e, "padd_lfo1"), e.v("n")))),
                set(e, p(e, "phase_lfo2"), add(e, p(e, "phase_lfo2"), mul(e, p(e, "padd_lfo2"), e.v("n")))));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "m4_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), (work))));
        return EcoBuzz.machineModule(
                e, "m4", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
