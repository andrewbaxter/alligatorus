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
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

final class EcoBuzzM4wII {
    private EcoBuzzM4wII() {}

    static final int NG = 57;
    static final int NT = 2;
    static final int TRACKS = 8;
    static final int WAVE_COUNT = 42;
    static final int WAVES_BYTES = WAVE_COUNT * 2048 * 2;
    private static final int COEFS = 4 * 128 * 128 * 8;
    private static final int LFOOSC = 0x10000;
    private static final int NONLIN = 256;
    private static final int WAVE_STEPUP = 33;
    private static final int WAVE_STEPDN = 34;
    private static final int WAVE_WACKY1 = 41;
    private static final int WAVE_WACKY2 = 9;
    private static final int NUMWAVES = 43;
    private static final double RATE = 44100.0;
    private static final double NOTECONST = 1.05946309436;
    private static final double AMPCONV = 7.45058059692e-9;
    private static final double PI = Math.PI;
    private static final int ATTR_INERTIA = 1;
    private static final int ATTR_LFO_SCALE = 127;
    private static final IList<Integer> OSC_OFFS =
            IList.of(0, 2048, 3072, 3584, 3840, 3968, 4032, 4064, 4080, 4088, 4092);
    private static final IList<Integer> GACT_DEFAULTS = IList.of(
            0, 0, 64, 2, 0, 0, 0, 64, 0, 64, 0, 64, 64, 80, 0, 0, 64, 0, 64, 7, 11, 11, 127, 11, 64, 5, 5, 16, 127, 32,
            2, 0, 32, 32, 7, 15, 14, 127, 15, 96, 0, 0, 0, 0, 64, 0, 0, 0, 0, 64, 0, 0, 0, 64, 0, 64, 32);
    private static final IList<Integer> LFO1_DESTS = IList.of(
            0, 0b0001, 0b0010, 0b0100, 0b1000, 0b0011, 0b0101, 0b1001, 0b0110, 0b1010, 0b1100, 0b0111, 0b1011, 0b1101,
            0b1110, 0b1111, 0b10000, 0b101000);
    private static final IList<Integer> LFO2_DESTS = IList.of(
            0, 0b0001, 0b0010, 0b0100, 0b1000, 0b0011, 0b0101, 0b1001, 0b0110, 0b1010, 0b1100, 0b0111, 0b1011, 0b1101,
            0b1110, 0b1111, 0b10000, 0b100000);
    private static final IList<String> L1_FLAGS = IList.of("l_osc1", "l_pw1", "l_amp", "l_cut", "l_vib", "l_vcf");
    private static final IList<String> L2_FLAGS = IList.of("l_osc2", "l_pw2", "l_mix", "l_reso", "l_phase2", "l_lfo1");

    private record ModDest(int dest, int param, int min, int max, int writeParam) {}

    private static final IList<ModDest> MOD1 = IList.of(
            new ModDest(1, 39, 0, 127, 39),
            new ModDest(2, 32, 0, 127, 32),
            new ModDest(3, 16, 0, 127, 16),
            new ModDest(4, 13, 0, 127, 13),
            new ModDest(5, 43, 0, 255, 43),
            new ModDest(6, 42, 0, 128, 42),
            new ModDest(7, 9, 0, 127, 9),
            new ModDest(8, 12, 0, 127, 12));
    private static final IList<ModDest> MOD2 = IList.of(
            new ModDest(1, 24, 0, 127, 24),
            new ModDest(2, 33, 0, 127, 33),
            new ModDest(3, 51, 0, 127, 51),
            new ModDest(4, 56, 0, 200, 56),
            new ModDest(5, 48, 0, 255, 48),
            new ModDest(6, 47, 0, 129, 47),
            new ModDest(7, 11, 0, 127, 11),
            new ModDest(8, 34, 0, 127, 34),
            new ModDest(9, 35, 0, 127, 35),
            new ModDest(10, 38, 0, 127, 38),
            new ModDest(11, 37, 0, 127, 36));
    private static final IList<Integer> MOD1_RESEND = IList.of(39, 32, 16, 13, 42, 43, 9, 12);
    private static final IList<Integer> MOD2_RESEND = IList.of(24, 33, 51, 56, 48, 47, 11, 34, 35, 38, 37);

    private static final IList<String> I32_FIELDS = IList.of(
            "spt",
            "noise1",
            "noise2",
            "subvol",
            "ampgain",
            "ftype_base",
            "resonance",
            "db24",
            "db18",
            "peak",
            "phat",
            "dist",
            "ueg_att",
            "ueg_dec",
            "ueg_sus",
            "ueg_level",
            "ueg_rel",
            "uenvmod",
            "uegdest",
            "aeg_att",
            "aeg_dec",
            "aeg_sus",
            "aeg_level",
            "aeg_rel",
            "feg_att",
            "feg_dec",
            "feg_sus",
            "feg_rel",
            "fenvmod",
            "glide",
            "glidetime",
            "sync",
            "lfo1noise",
            "lfo2noise",
            "lfo1synced",
            "lfo2synced",
            "lfo1tab",
            "lfo2tab",
            "ph_lfo1",
            "ph_lfo2",
            "padd_lfo1",
            "padd_lfo2",
            "lfo1freq",
            "lfo2freq",
            "lfo1phdiff",
            "lfo2phdiff",
            "lfo1amount",
            "lfo2amount",
            "playmode",
            "bendamt",
            "bendtime",
            "l_vcf",
            "l_vib",
            "l_osc1",
            "l_pw1",
            "l_amp",
            "l_cut",
            "l_1lock2",
            "l_osc2",
            "l_pw2",
            "l_mix",
            "l_reso",
            "l_2lock1",
            "l_2lock2",
            "l_lfo1",
            "l_phase2",
            "moddest1",
            "modamt1",
            "moddest2",
            "modamt2",
            "modwheel",
            "phdiff2",
            "bal1",
            "bal2",
            "mixtype",
            "oscwave1",
            "oscwave2",
            "oscwave3",
            "w1_host",
            "w2_host",
            "w3_host",
            "w1_custom",
            "w2_custom",
            "w3_custom",
            "wavetabw",
            "wavesemi",
            "wavefixed");
    private static final IList<String> F32_FIELDS = IList.of(
            "center1",
            "center2",
            "cutoff",
            "cutoff_target",
            "cutoff_add",
            "ueg_susfrac",
            "feg_level",
            "feg_susfrac",
            "aeg_susfrac",
            "detune_semi",
            "detune_fine",
            "tsdsf",
            "pitchmod",
            "bendfactor",
            "bendglide");
    private static final IList<String> T_I32_FIELDS = IList.of(
            "t_count",
            "t_note",
            "t_phase1",
            "t_phase2",
            "t_phasesub",
            "t_ph1",
            "t_ph2",
            "t_lvl1",
            "t_lvl2",
            "t_lvl3",
            "t_c1",
            "t_c2",
            "t_padd1",
            "t_padd2",
            "t_glideactive",
            "t_glidecount",
            "t_pitchmodactive",
            "t_uegstate",
            "t_uegcount",
            "t_uegamp",
            "t_uegadd",
            "t_uegtarget",
            "t_uegmix",
            "t_uegreso",
            "t_uegphase",
            "t_r1",
            "t_r2",
            "t_r3",
            "t_r4",
            "t_r5",
            "t_aegstate",
            "t_aegcount",
            "t_volume",
            "t_amp",
            "t_ampadd",
            "t_amptarget",
            "t_fegstate",
            "t_fegcount",
            "t_phlfo1",
            "t_phlfo2",
            "t_l1osc1",
            "t_l1vol",
            "t_l1cut",
            "t_l2osc2",
            "t_l2mix",
            "t_l2reso",
            "t_iir_len",
            "t_seed");
    private static final IList<String> T_F32_FIELDS = IList.of(
            "t_center1",
            "t_center2",
            "t_ps1a",
            "t_ps1b",
            "t_ps2a",
            "t_ps2b",
            "t_freq",
            "t_freqfrom",
            "t_glidemul",
            "t_glidefactor",
            "t_uegpw1",
            "t_uegpw2",
            "t_pf1",
            "t_pf2",
            "t_oldout",
            "t_oldoldout",
            "t_x1",
            "t_x2",
            "t_y1",
            "t_y2",
            "t_x241",
            "t_x242",
            "t_y241",
            "t_y242",
            "t_cut",
            "t_cutadd",
            "t_cuttarget",
            "t_l1pw1",
            "t_l2pw2",
            "t_iir_lastcut",
            "t_iir_lastres");
    private static final IMap<String, String> FN_TYPES = IMap.ofEntries(
            new Pair<>("tables", "void0_fn_t"),
            new Pair<>("wave_at", "wave_fn_t"),
            new Pair<>("lfo_at", "lfo_fn_t"),
            new Pair<>("noteon", "pi_fn_t"),
            new Pair<>("noteoff", "pi_fn_t"),
            new Pair<>("osc", "pif_fn_t"),
            new Pair<>("vca", "pif_fn_t"),
            new Pair<>("ueg", "pi_fn_t"),
            new Pair<>("filter", "filter_fn_t"),
            new Pair<>("iir", "iir_fn_t"),
            new Pair<>("newphases", "pi_fn_t"),
            new Pair<>("trackwork", "track_fn_t"),
            new Pair<>("sinf", "f1_fn_t"),
            new Pair<>("cosf", "f1_fn_t"),
            new Pair<>("sinhf", "f1_fn_t"),
            new Pair<>("logf", "f1_fn_t"),
            new Pair<>("fabsf", "f1_fn_t"),
            new Pair<>("floorf", "f1_fn_t"),
            new Pair<>("powf", "f2_fn_t"),
            new Pair<>("pow", "d2_fn_t"),
            new Pair<>("tan", "d1_fn_t"),
            new Pair<>("rint", "d1_fn_t"),
            new Pair<>("sinh", "d1_fn_t"));
    private static final IMap<String, String> GLOBAL_TYPES = IMap.ofEntries(
            new Pair<>("coefs", "coefs_t"),
            new Pair<>("lfoosc", "lfoosc_t"),
            new Pair<>("lfovib", "lfoosc_t"),
            new Pair<>("nonlin", "nonlin_t"),
            new Pair<>("tables_ready", "t_i32"),
            new Pair<>("osc_offs", "offs_t"),
            new Pair<>("lfo1_dests", "dests_t"),
            new Pair<>("lfo2_dests", "dests_t"));

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pt(EcoMb e, String field) {
        return idx(e, fld(e, e.v("p"), field), e.v("t"));
    }

    private static SourceExpr gval(EcoMb e, int k) {
        return idx(e, p(e, "gval"), e.n(k));
    }

    private static SourceExpr ctl(EcoMb e, int k) {
        return idx(e, p(e, "ctl"), e.n(k));
    }

    private static SourceExpr onGval(EcoMb e, int k, SourceExpr... then) {
        var inner = new TList<SourceExpr>();
        inner.add(e.let("v", gval(e, k)));
        for (var x : then) inner.add(x);
        return ifOnly(e, ne(e, gval(e, k), e.n(-1)), e.seq(inner.toArray(SourceExpr[]::new)));
    }

    private static SourceExpr onGvalCtl(EcoMb e, int k, SourceExpr... then) {
        var inner = new TList<SourceExpr>();
        inner.add(set(e, ctl(e, k), e.v("v")));
        for (var x : then) inner.add(x);
        return onGval(e, k, inner.toArray(SourceExpr[]::new));
    }

    private static SourceExpr onGvalMod(EcoMb e, int k, String destField, int dest, SourceExpr... then) {
        var inner = new TList<SourceExpr>();
        inner.add(ifOnly(
                e,
                e.not(and(e, ne(e, e.v("mod"), e.n(0)), eq(e, p(e, destField), e.n(dest)))),
                set(e, ctl(e, k), e.v("v"))));
        for (var x : then) inner.add(x);
        return onGval(e, k, inner.toArray(SourceExpr[]::new));
    }

    private static SourceExpr envSamples(EcoMb e, SourceExpr v) {
        var ms =
                f32(e, mul(e, call(e, "pow", div(e, f64(e, add(e, v, e.n(2))), e.n(129)), f64(e, e.n(3))), e.n(10000)));
        return add(e, i32(e, mul(e, mul(e, f64(e, ms), e.n(RATE)), e.n(1.0 / 1000.0))), e.n(1));
    }

    private static SourceExpr sustainSamples(EcoMb e, SourceExpr v) {
        return e.ifElse(eq(e, v, e.n(128)), EcoBuzz.i32(e, e.n(-1)), envSamples(e, v));
    }

    private static SourceExpr scalLfoFreq(EcoMb e, SourceExpr v) {
        return f32(
                e,
                mul(
                        e,
                        sub(
                                e,
                                call(e, "pow", div(e, f64(e, add(e, v, e.n(8))), e.n(124)), f64(e, e.n(4))),
                                e.n(0.000017324998565270)),
                        e.n(40.00072)));
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

    private static SourceExpr f2i(EcoMb e, SourceExpr x) {
        return f2iD(e, f64(e, x));
    }

    private static SourceExpr f2iD(EcoMb e, SourceExpr x) {
        return i32(e, call(e, "rint", sub(e, x, e.n(0.5))));
    }

    private static SourceExpr f64(EcoMb e, SourceExpr v) {
        return EcoBuzz.conv(e, "t_f64", v);
    }

    private static SourceExpr biquadSum(
            EcoMb e, SourceExpr c0, Function<Integer, SourceExpr> ck, SourceExpr x, IList<String> taps) {
        var acc = mul(e, f64(e, c0), f64(e, x));
        for (int k = 0; k < taps.size(); k++) {
            acc = add(e, acc, mul(e, f64(e, ck.apply(k + 1)), f64(e, pt(e, taps.get(k)))));
        }
        return f32(e, acc);
    }

    private static SourceExpr lfoRead(EcoMb e, String tab, SourceExpr phase) {
        return call(e, "lfo_at", e.v("p"), p(e, tab), e.mb.operatorUshr(phase, e.n(21)));
    }

    private static SourceExpr lfoReadIdx(EcoMb e, String tab, SourceExpr index) {
        return call(e, "lfo_at", e.v("p"), p(e, tab), index);
    }

    private static SourceExpr lfoOsc(EcoMb e, String table, SourceExpr arg) {
        return idx(e, lw(e, table), add(e, arg, e.n(0x8000)));
    }

    private static SourceExpr clampi(EcoMb e, SourceExpr v, int lo, int hi) {
        return e.ifElse(
                lt(e, v, e.n(lo)), EcoBuzz.i32(e, e.n(lo)), e.ifElse(gt(e, v, e.n(hi)), EcoBuzz.i32(e, e.n(hi)), v));
    }

    private static SourceExpr noiseStep(EcoMb e, String out, SourceExpr bal) {
        var sum = add(e, add(e, add(e, pt(e, "t_r1"), pt(e, "t_r2")), pt(e, "t_r3")), pt(e, "t_r4"));
        return e.seq(
                e.let(out + "n", e.mb.operatorShr(shl(e, sum, e.n(16)), e.n(16))),
                set(e, pt(e, "t_r1"), pt(e, "t_r2")),
                set(e, pt(e, "t_r2"), pt(e, "t_r3")),
                set(e, pt(e, "t_r3"), pt(e, "t_r4")),
                set(e, pt(e, "t_r4"), e.v(out + "n")),
                set(e, e.v(out), e.mb.operatorShr(mul(e, e.v(out + "n"), bal), e.n(7))));
    }

    private static SourceExpr noise2Step(EcoMb e, String out, SourceExpr bal, SourceExpr center) {
        return e.seq(
                set(e, pt(e, "t_seed"), add(e, mul(e, pt(e, "t_seed"), e.n(1103515245)), e.n(12345))),
                e.let(out + "r", sub(e, and(e, e.mb.operatorUshr(pt(e, "t_seed"), e.n(16)), e.n(32767)), e.n(16384))),
                set(e, pt(e, "t_r5"), add(e, pt(e, "t_r5"), i32(e, mul(e, f32(e, e.v(out + "r")), center)))),
                set(e, pt(e, "t_r5"), clampi(e, pt(e, "t_r5"), -32768, 32768)),
                set(e, e.v(out), e.mb.operatorShr(mul(e, pt(e, "t_r5"), bal), e.n(7))));
    }

    private static SourceExpr oscRead(
            EcoMb e, String oscwave, String host, String custom, SourceExpr phase, String lvl, SourceExpr bal) {
        var sh = add(e, EcoBuzz.i32(e, e.n(16)), pt(e, lvl));
        var size = sub(e, e.mb.operatorShr(EcoBuzz.i32(e, e.n(2048)), pt(e, lvl)), e.n(1));
        var offset = idx(e, lw(e, "osc_offs"), pt(e, lvl));
        var index = e.mb.operatorUshr(phase, sh);
        var index2 = and(e, add(e, index, e.n(1)), size);
        var frac = e.mb.operatorShr(
                and(e, phase, sub(e, shl(e, EcoBuzz.i32(e, e.n(1)), sh), e.n(1))), sub(e, sh, e.n(12)));
        var x1 = call(e, "wave_at", e.v("p"), p(e, host), e.n(-1), add(e, index, offset));
        var x2 = call(e, "wave_at", e.v("p"), p(e, host), e.n(-1), add(e, index2, offset));
        var banded = add(e, x1, f2iD(e, mul(e, f64(e, sub(e, x2, x1)), mul(e, f64(e, frac), e.n(0.000244140625)))));
        var plain = call(
                e,
                "wave_at",
                e.v("p"),
                e.ifElse(lt(e, p(e, custom), e.n(0)), p(e, host), EcoBuzz.i32(e, e.n(-1))),
                p(e, custom),
                e.mb.operatorUshr(phase, e.n(16)));
        return e.mb.operatorShr(
                mul(
                        e,
                        e.ifElse(
                                ne(e, p(e, oscwave), e.n(-1)),
                                e.ifElse(eq(e, bal, e.n(0)), EcoBuzz.i32(e, e.n(0)), banded),
                                plain),
                        bal),
                e.n(7));
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

    private static SourceExpr absi(EcoMb e, SourceExpr x) {
        return e.ifElse(lt(e, x, e.n(0)), e.mb.operatorNeg(x), x);
    }

    private static SourceExpr levelLoop(EcoMb e, String label, String lvl, SourceExpr rate) {
        return e.seq(
                set(e, pt(e, lvl), e.n(0)),
                EcoBuzz.whileLoop(
                        e,
                        label,
                        and(
                                e,
                                gt(
                                        e,
                                        mul(
                                                e,
                                                rate,
                                                mul(
                                                        e,
                                                        f32(e, e.mb.operatorShr(EcoBuzz.i32(e, e.n(2048)), pt(e, lvl))),
                                                        e.n(1.0 / RATE))),
                                        e.n(1)),
                                lt(e, pt(e, lvl), e.n(10))),
                        set(e, pt(e, lvl), add(e, pt(e, lvl), e.n(1)))));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("i32xt", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS))));
        defs.add(e.let("f32xt", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS))));
        defs.add(e.let(
                "data_t",
                EcoBuzz.struct(
                        e, "m4wii_waves", EcoBuzz.field(e, "b", EcoBuzz.array(e, e.v("t_i8"), e.intOf(WAVES_BYTES))))));
        defs.add(e.let("p_data_t", EcoBuzz.ptr(e, e.v("data_t"))));
        defs.add(e.let("coefs_t", EcoBuzz.array(e, e.v("t_f32"), e.intOf(COEFS))));
        defs.add(e.let("lfoosc_t", EcoBuzz.array(e, e.v("t_f32"), e.intOf(LFOOSC))));
        defs.add(e.let("nonlin_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NONLIN))));
        defs.add(e.let("offs_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(OSC_OFFS.size()))));
        defs.add(e.let("dests_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(LFO1_DESTS.size()))));
        defs.add(e.let(
                "wave_bytes",
                e.callM(
                        e.call(e.mb.builtin("file", "open"), e.s("buzz-m4wii-waves.raw")),
                        "read",
                        e.rec(e.p("offset", e.intOf(0)), e.p("len", e.intOf(WAVES_BYTES))))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "data", e.v("p_data_t")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(e, "ctl", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        for (var f : I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_i32")));
        for (var f : F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_f32")));
        for (var f : T_I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32xt")));
        for (var f : T_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xt")));
        fields.add(EcoBuzz.field(e, "t_iir_coef", EcoBuzz.array(e, e.v("t_f64"), e.intOf(9 * TRACKS))));
        fields.add(EcoBuzz.field(e, "t_iir_hist", EcoBuzz.array(e, e.v("t_f32"), e.intOf(4 * TRACKS))));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "mw_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("void0_fn_t", EcoBuzz.fnType(e, e.v("t_void"))));
        defs.add(e.let(
                "wave_fn_t",
                EcoBuzz.fnType(e, e.v("t_i32"), e.v("p_state_t"), e.v("t_i32"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("lfo_fn_t", EcoBuzz.fnType(e, e.v("t_i32"), e.v("p_state_t"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("pi_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let("pif_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let("filter_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"), e.v("t_f32"))));
        defs.add(e.let(
                "iir_fn_t",
                EcoBuzz.fnType(
                        e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"), e.v("t_f32"), e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let(
                "track_fn_t",
                EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("f2_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("d1_fn_t", EcoBuzz.fnType(e, e.v("t_f64"), e.v("t_f64"))));
        defs.add(e.let("d2_fn_t", EcoBuzz.fnType(e, e.v("t_f64"), e.v("t_f64"), e.v("t_f64"))));

        var main = new TList<SourceExpr>();
        main.add(e.let("zero", e.callM0(e.v("arg"), "zero")));
        Function<String, SourceExpr> typeOf = name -> {
            if (name.equals("osc_at")) return EcoBuzz.globalT(e, e.v("osc_at_fn_t"));
            var fnType = FN_TYPES.getOpt(name);
            if (fnType.optIsSome()) return EcoBuzz.globalT(e, e.v(fnType.optUnwrap()));
            var globalType = GLOBAL_TYPES.getOpt(name);
            if (globalType.optIsSome()) return EcoBuzz.globalMemT(e, e.v(globalType.optUnwrap()));
            throw new IllegalArgumentException("no type for builder value " + name);
        };
        defs.add(e.let("osc_at_fn_t", EcoBuzz.fnType(e, e.v("t_i32"), e.v("t_i32"), e.v("t_i32"))));
        var partTables = new EcoBuzz.Part("tables", typeOf);
        var partVoice = new EcoBuzz.Part("voice", typeOf);
        var body = partTables.body;
        for (var fn : new String[] {"sinf", "cosf", "sinhf", "logf", "fabsf", "floorf"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, e.v("ll"), fn, e.lowerV("f1_fn_t"))));
        }
        body.add(e.let("powf", EcoBuzz.externDef(e, e.v("ll"), "powf", e.lowerV("f2_fn_t"))));
        body.add(e.let("pow", EcoBuzz.externDef(e, e.v("ll"), "pow", e.lowerV("d2_fn_t"))));
        body.add(e.let("tan", EcoBuzz.externDef(e, e.v("ll"), "tan", e.lowerV("d1_fn_t"))));
        body.add(e.let("rint", EcoBuzz.externDef(e, e.v("ll"), "rint", e.lowerV("d1_fn_t"))));
        body.add(e.let("sinh", EcoBuzz.externDef(e, e.v("ll"), "sinh", e.lowerV("d1_fn_t"))));
        for (var g : new String[][] {
            {"coefs", "coefs_t"}, {"lfoosc", "lfoosc_t"}, {"lfovib", "lfoosc_t"}, {"nonlin", "nonlin_t"}
        }) {
            body.add(e.let(
                    g[0],
                    e.call(
                            EcoBuzz.llvmL(e, "global_zero_fn"),
                            e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("mw_" + g[0])), e.p("ty", e.lowerV(g[1]))))));
        }
        body.add(e.let(
                "tables_ready",
                e.call(
                        EcoBuzz.llvmL(e, "global_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("mw_tables_ready")),
                                e.p("ty", e.lowerV("t_i32")),
                                e.p("init", e.n(0))))));
        var offs = new TList<SourceExpr>();
        for (var v : OSC_OFFS) offs.add(e.intOf(v));
        body.add(e.let(
                "osc_offs",
                e.call(
                        EcoBuzz.llvmL(e, "global_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("mw_osc_offs")),
                                e.p("ty", e.lowerV("offs_t")),
                                e.p("init", e.listNew(e.mb.typesInt(), offs.toArray(SourceExpr[]::new)))))));
        var pp = e.v("pp");
        var nlLo = i32(
                e,
                div(
                        e,
                        call(
                                e,
                                "pow",
                                mul(e, f64(e, call(e, "fabsf", f32(e, sub(e, pp, e.n(127))))), e.n(128)),
                                f64(e, e.n(1.5))),
                        e.n(150)));
        var nlHi =
                i32(e, div(e, call(e, "pow", mul(e, f64(e, add(e, pp, e.n(1))), e.n(128)), f64(e, e.n(1.5))), e.n(80)));
        body.add(e.let(
                "tables",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_tables",
                        new String[] {},
                        e.lowerV("void0_fn_t"),
                        ifOnly(
                                e,
                                eq(e, lw(e, "tables_ready"), e.n(0)),
                                set(e, lw(e, "tables_ready"), e.n(1)),
                                EcoBuzzM4.coefTable(e),
                                forRange(
                                        e,
                                        "pp",
                                        e.n(LFOOSC),
                                        set(
                                                e,
                                                idx(e, lw(e, "lfoosc"), pp),
                                                f32(
                                                        e,
                                                        call(
                                                                e,
                                                                "pow",
                                                                f64(e, e.n(NOTECONST)),
                                                                div(e, f64(e, sub(e, pp, e.n(0x8000))), e.n(512))))),
                                        set(
                                                e,
                                                idx(e, lw(e, "lfovib"), pp),
                                                f32(
                                                        e,
                                                        call(
                                                                e,
                                                                "pow",
                                                                f64(e, e.n(NOTECONST)),
                                                                div(e, f64(e, sub(e, pp, e.n(0x8000))), e.n(16384)))))),
                                forRange(
                                        e,
                                        "pp",
                                        e.n(128),
                                        e.let("nl", e.mb.operatorNeg(nlLo)),
                                        set(
                                                e,
                                                idx(e, lw(e, "nonlin"), pp),
                                                e.ifElse(
                                                        lt(e, e.v("nl"), e.n(-30000)),
                                                        sub(
                                                                e,
                                                                EcoBuzz.i32(e, e.n(-30000)),
                                                                i32(
                                                                        e,
                                                                        call(
                                                                                e,
                                                                                "pow",
                                                                                f64(
                                                                                        e,
                                                                                        add(
                                                                                                e,
                                                                                                e.mb.operatorNeg(
                                                                                                        e.v("nl")),
                                                                                                e.n(30000))),
                                                                                f64(e, e.n(0.82))))),
                                                        e.v("nl")))),
                                forRange(
                                        e,
                                        "pp",
                                        e.n(128),
                                        e.let("ph", add(e, pp, e.n(128))),
                                        e.let("nh", nlHi),
                                        set(
                                                e,
                                                idx(e, lw(e, "nonlin"), add(e, pp, e.n(128))),
                                                e.ifElse(
                                                        gt(e, e.v("nh"), e.n(30000)),
                                                        add(
                                                                e,
                                                                EcoBuzz.i32(e, e.n(30000)),
                                                                i32(
                                                                        e,
                                                                        call(
                                                                                e,
                                                                                "pow",
                                                                                f64(e, sub(e, e.v("nh"), e.n(30000))),
                                                                                f64(e, e.n(0.82))))),
                                                        e.v("nh"))))))));
        body = partVoice.body;
        body.add(e.let("osc_at", e.callM0(e.fld(e.v("arg"), "dsp"), "osc_at")));
        body.add(e.let(
                "m4wii_waves",
                e.call(
                        EcoBuzz.llvmL(e, "global_const_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("mw_waves")),
                                e.p("ty", e.lowerV("data_t")),
                                e.p("init", e.rec(e.p("b", e.lowerV("wave_bytes"))))))));
        var bytes = fld(e, p(e, "data"), "b");
        var bIdx = mul(e, add(e, shl(e, e.v("custom"), e.n(11)), e.v("index")), e.n(2));
        body.add(e.let(
                "wave_at",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_wave_at",
                        new String[] {"p", "host", "custom", "index"},
                        e.lowerV("wave_fn_t"),
                        e.ret(e.ifElse(
                                ge(e, e.v("custom"), e.n(0)),
                                or(
                                        e,
                                        shl(e, i32(e, idx(e, bytes, add(e, bIdx, e.n(1)))), e.n(8)),
                                        and(e, i32(e, idx(e, bytes, bIdx)), e.n(255))),
                                EcoBuzz.callV(e, lw(e, "osc_at"), e.v("host"), e.v("index")))))));
        body.add(e.let(
                "lfo_at",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_lfo_at",
                        new String[] {"p", "tab", "index"},
                        e.lowerV("lfo_fn_t"),
                        e.ret(e.ifElse(
                                ge(e, e.v("tab"), e.n(100)),
                                call(
                                        e,
                                        "wave_at",
                                        e.v("p"),
                                        EcoBuzz.i32(e, e.n(0)),
                                        sub(e, e.v("tab"), e.n(100)),
                                        e.v("index")),
                                EcoBuzz.callV(e, lw(e, "osc_at"), e.v("tab"), e.v("index")))))));
        var t = e.v("t");
        var vol = pt(e, "t_volume");
        var noteOn = e.seq(
                set(e, pt(e, "t_freqfrom"), pt(e, "t_freq")),
                set(
                        e,
                        pt(e, "t_freq"),
                        f32(
                                e,
                                mul(
                                        e,
                                        call(
                                                e,
                                                "pow",
                                                f64(e, e.n(2)),
                                                div(
                                                        e,
                                                        f64(
                                                                e,
                                                                sub(
                                                                        e,
                                                                        add(
                                                                                e,
                                                                                mul(
                                                                                        e,
                                                                                        e.mb.operatorShr(
                                                                                                pt(e, "t_note"),
                                                                                                e.n(4)),
                                                                                        e.n(12)),
                                                                                and(e, pt(e, "t_note"), e.n(15))),
                                                                        e.n(1))),
                                                        e.n(12))),
                                        e.n(16.3516)))),
                e.ifElse(
                        and(e, ne(e, p(e, "glide"), e.n(0)), ne(e, pt(e, "t_aegstate"), e.n(0))),
                        e.seq(
                                set(e, pt(e, "t_glideactive"), e.n(1)),
                                set(
                                        e,
                                        pt(e, "t_glidemul"),
                                        f32(
                                                e,
                                                call(
                                                        e,
                                                        "pow",
                                                        e.ifElse(
                                                                gt(e, pt(e, "t_freq"), pt(e, "t_freqfrom")),
                                                                f64(e, e.n(2)),
                                                                f64(e, e.n(0.5))),
                                                        div(e, f64(e, e.n(1)), f64(e, p(e, "glidetime")))))),
                                set(e, pt(e, "t_glidefactor"), e.n(1)),
                                set(
                                        e,
                                        pt(e, "t_glidecount"),
                                        i32(
                                                e,
                                                div(
                                                        e,
                                                        call(e, "logf", div(e, pt(e, "t_freq"), pt(e, "t_freqfrom"))),
                                                        call(e, "logf", pt(e, "t_glidemul")))))),
                        set(e, pt(e, "t_glideactive"), e.n(0))),
                ifOnly(
                        e,
                        or(e, ne(e, p(e, "playmode"), e.n(8)), eq(e, pt(e, "t_aegstate"), e.n(0))),
                        set(e, p(e, "aeg_level"), i32(e, mul(e, f32(e, vol), p(e, "aeg_susfrac")))),
                        set(e, pt(e, "t_aegstate"), e.n(1)),
                        set(e, pt(e, "t_aegcount"), p(e, "aeg_att")),
                        set(e, pt(e, "t_ampadd"), div(e, vol, p(e, "aeg_att"))),
                        ifOnly(e, ne(e, and(e, p(e, "playmode"), e.n(1)), e.n(0)), set(e, pt(e, "t_amp"), e.n(0))),
                        set(e, pt(e, "t_amptarget"), vol),
                        ifOnly(
                                e,
                                ne(e, and(e, p(e, "playmode"), e.n(2)), e.n(0)),
                                set(e, p(e, "ph_lfo1"), e.n(0)),
                                set(e, p(e, "ph_lfo2"), e.n(0))),
                        set(e, pt(e, "t_fegstate"), e.n(1)),
                        set(e, pt(e, "t_fegcount"), p(e, "feg_att")),
                        set(e, pt(e, "t_cutadd"), div(e, f32(e, p(e, "fenvmod")), f32(e, p(e, "feg_att")))),
                        ifOnly(e, ne(e, and(e, p(e, "playmode"), e.n(4)), e.n(0)), set(e, pt(e, "t_cut"), e.n(0))),
                        set(e, pt(e, "t_cuttarget"), f32(e, p(e, "fenvmod"))),
                        e.ifElse(
                                ne(e, p(e, "uenvmod"), e.n(0)),
                                e.seq(
                                        set(e, pt(e, "t_uegstate"), e.n(1)),
                                        set(e, pt(e, "t_uegcount"), p(e, "ueg_att")),
                                        set(e, pt(e, "t_uegadd"), div(e, p(e, "uenvmod"), p(e, "ueg_att"))),
                                        set(e, pt(e, "t_uegamp"), e.n(0)),
                                        set(e, pt(e, "t_uegtarget"), p(e, "uenvmod")),
                                        set(e, pt(e, "t_pf1"), e.n(1)),
                                        set(e, pt(e, "t_pf2"), e.n(1)),
                                        set(e, pt(e, "t_uegmix"), e.n(0)),
                                        set(e, pt(e, "t_uegpw1"), e.n(0)),
                                        set(e, pt(e, "t_uegpw2"), e.n(0)),
                                        set(e, pt(e, "t_uegreso"), e.n(0)),
                                        set(e, pt(e, "t_uegphase"), e.n(0))),
                                e.seq(
                                        set(e, pt(e, "t_uegstate"), e.n(0)),
                                        set(e, pt(e, "t_uegamp"), e.n(0)),
                                        set(e, pt(e, "t_uegtarget"), e.n(0))))),
                set(e, pt(e, "t_pitchmodactive"), e.n(0)),
                e.let("basef", e.ifElse(ne(e, pt(e, "t_glideactive"), e.n(0)), pt(e, "t_freqfrom"), pt(e, "t_freq"))),
                set(e, pt(e, "t_padd1"), i32(e, mul(e, mul(e, e.v("basef"), p(e, "tsdsf")), e.n(65536)))),
                set(
                        e,
                        pt(e, "t_padd2"),
                        i32(
                                e,
                                mul(
                                        e,
                                        mul(
                                                e,
                                                mul(e, mul(e, e.v("basef"), p(e, "detune_semi")), p(e, "detune_fine")),
                                                p(e, "tsdsf")),
                                        e.n(65536)))),
                e.ifElse(
                        ne(e, p(e, "oscwave1"), e.n(-1)),
                        levelLoop(e, "lv1", "t_lvl1", pt(e, "t_freq")),
                        set(e, pt(e, "t_lvl1"), e.n(0))),
                e.ifElse(
                        ne(e, p(e, "oscwave2"), e.n(-1)),
                        levelLoop(
                                e,
                                "lv2",
                                "t_lvl2",
                                mul(e, mul(e, pt(e, "t_freq"), p(e, "detune_semi")), p(e, "detune_fine"))),
                        set(e, pt(e, "t_lvl2"), e.n(0))),
                e.ifElse(
                        ne(e, p(e, "oscwave3"), e.n(-1)),
                        levelLoop(e, "lv3", "t_lvl3", mul(e, pt(e, "t_freq"), e.n(0.5))),
                        set(e, pt(e, "t_lvl3"), e.n(0))));
        body.add(e.let(
                "noteon",
                EcoBuzz.funcDef(e, e.v("ll"), "mw_noteon", new String[] {"p", "t"}, e.lowerV("pi_fn_t"), noteOn)));

        var noteOff = ifOnly(
                e,
                ne(e, pt(e, "t_aegstate"), e.n(0)),
                set(e, pt(e, "t_aegstate"), e.n(4)),
                set(e, pt(e, "t_aegcount"), p(e, "aeg_rel")),
                set(
                        e,
                        pt(e, "t_ampadd"),
                        i32(
                                e,
                                div(
                                        e,
                                        e.mb.operatorNeg(mul(e, p(e, "aeg_susfrac"), f32(e, vol))),
                                        f32(e, p(e, "aeg_rel"))))),
                set(e, pt(e, "t_amptarget"), e.n(0)),
                set(e, pt(e, "t_fegstate"), e.n(4)),
                set(e, pt(e, "t_cutadd"), div(e, e.mb.operatorNeg(p(e, "feg_level")), f32(e, p(e, "feg_rel")))),
                set(e, pt(e, "t_fegcount"), p(e, "feg_rel")),
                set(e, pt(e, "t_cuttarget"), e.n(0)),
                set(e, pt(e, "t_uegstate"), e.n(4)),
                set(e, pt(e, "t_uegadd"), div(e, e.mb.operatorNeg(p(e, "ueg_level")), p(e, "ueg_rel"))),
                set(e, pt(e, "t_uegcount"), shl(e, p(e, "ueg_rel"), e.n(1))),
                set(e, pt(e, "t_uegtarget"), e.n(0)));
        body.add(e.let(
                "noteoff",
                EcoBuzz.funcDef(e, e.v("ll"), "mw_noteoff", new String[] {"p", "t"}, e.lowerV("pi_fn_t"), noteOff)));

        var o = e.v("o");
        var o2 = e.v("o2");
        var mixCase = new TList<SourceExpr>();
        mixCase.add(ifOnly(
                e,
                or(
                        e,
                        or(e, eq(e, p(e, "mixtype"), e.n(0)), eq(e, p(e, "mixtype"), e.n(12))),
                        eq(e, p(e, "mixtype"), e.n(13))),
                set(e, o, add(e, o, o2))));
        mixCase.add(ifOnly(
                e,
                eq(e, p(e, "mixtype"), e.n(1)),
                set(e, o, sub(e, shl(e, absi(e, sub(e, o, o2)), e.n(1)), e.n(0x8000)))));
        mixCase.add(ifOnly(e, eq(e, p(e, "mixtype"), e.n(2)), set(e, o, e.mb.operatorShr(mul(e, o, o2), e.n(15)))));
        mixCase.add(
                ifOnly(e, eq(e, p(e, "mixtype"), e.n(3)), ifOnly(e, lt(e, absi(e, o), absi(e, o2)), set(e, o, o2))));
        mixCase.add(
                ifOnly(e, eq(e, p(e, "mixtype"), e.n(4)), ifOnly(e, gt(e, absi(e, o), absi(e, o2)), set(e, o, o2))));
        mixCase.add(ifOnly(e, eq(e, p(e, "mixtype"), e.n(5)), set(e, o, and(e, o, o2))));
        mixCase.add(ifOnly(e, eq(e, p(e, "mixtype"), e.n(6)), set(e, o, or(e, o, o2))));
        mixCase.add(ifOnly(e, eq(e, p(e, "mixtype"), e.n(7)), set(e, o, e.mb.operatorXor(o, o2))));
        mixCase.add(ifOnly(
                e,
                eq(e, p(e, "mixtype"), e.n(8)),
                ifOnly(
                        e,
                        lt(e, o, o2),
                        set(e, o, e.mb.operatorXor(o, e.mb.operatorShr(mul(e, add(e, o2, o), o), e.n(15)))))));
        mixCase.add(ifOnly(
                e,
                eq(e, p(e, "mixtype"), e.n(9)),
                set(e, o, i32(e, div(e, mul(e, f32(e, o), f32(e, o2)), e.n(16384))))));
        mixCase.add(ifOnly(
                e,
                eq(e, p(e, "mixtype"), e.n(10)),
                set(e, o, e.mb.operatorXor(o, o2)),
                set(e, o, i32(e, div(e, mul(e, f32(e, o), f32(e, o2)), e.n(16384))))));
        mixCase.add(ifOnly(
                e,
                eq(e, p(e, "mixtype"), e.n(11)),
                set(e, o, add(e, o, o2)),
                set(e, o, shl(e, e.mb.operatorShr(o, e.n(13)), e.n(13)))));
        var oscBody = new TList<SourceExpr>();
        oscBody.add(e.let("o", EcoBuzz.i32(e, e.n(0))));
        oscBody.add(e.let("o2", EcoBuzz.i32(e, e.n(0))));
        oscBody.add(e.let("b1", p(e, "bal1")));
        oscBody.add(e.let("b2", p(e, "bal2")));
        oscBody.add(e.ifElse(
                or(e, ne(e, p(e, "l_mix"), e.n(0)), ne(e, pt(e, "t_uegmix"), e.n(0))),
                e.seq(
                        ifOnly(
                                e,
                                ne(e, p(e, "l_mix"), e.n(0)),
                                set(
                                        e,
                                        e.v("b2"),
                                        add(
                                                e,
                                                e.v("b2"),
                                                e.mb.operatorShr(
                                                        mul(
                                                                e,
                                                                lfoRead(e, "lfo2tab", pt(e, "t_phlfo2")),
                                                                pt(e, "t_l2mix")),
                                                        e.n(15))))),
                        set(e, e.v("b2"), clampi(e, add(e, e.v("b2"), pt(e, "t_uegmix")), 0, 127)),
                        set(e, e.v("b1"), sub(e, EcoBuzz.i32(e, e.n(127)), e.v("b2"))),
                        e.ifElse(
                                eq(e, p(e, "noise1"), e.n(1)),
                                noiseStep(e, "o", e.v("b1")),
                                e.ifElse(
                                        eq(e, p(e, "noise1"), e.n(2)),
                                        noise2Step(e, "o", e.v("b1"), p(e, "center2")),
                                        set(
                                                e,
                                                o,
                                                oscRead(
                                                        e,
                                                        "oscwave1",
                                                        "w1_host",
                                                        "w1_custom",
                                                        pt(e, "t_ph1"),
                                                        "t_lvl1",
                                                        e.v("b1"))))),
                        e.ifElse(
                                ne(e, p(e, "noise2"), e.n(0)),
                                noiseStep(e, "o2", e.v("b2")),
                                set(
                                        e,
                                        o2,
                                        oscRead(
                                                e,
                                                "oscwave2",
                                                "w2_host",
                                                "w2_custom",
                                                pt(e, "t_ph2"),
                                                "t_lvl2",
                                                e.v("b2"))))),
                e.seq(
                        e.ifElse(
                                eq(e, p(e, "noise1"), e.n(1)),
                                noiseStep(e, "o", e.v("b1")),
                                e.ifElse(
                                        eq(e, p(e, "noise1"), e.n(2)),
                                        noise2Step(e, "o", e.v("b1"), p(e, "center1")),
                                        set(
                                                e,
                                                o,
                                                oscRead(
                                                        e,
                                                        "oscwave1",
                                                        "w1_host",
                                                        "w1_custom",
                                                        pt(e, "t_ph1"),
                                                        "t_lvl1",
                                                        e.v("b1"))))),
                        e.ifElse(
                                eq(e, p(e, "noise2"), e.n(1)),
                                noiseStep(e, "o2", e.v("b2")),
                                e.ifElse(
                                        eq(e, p(e, "noise2"), e.n(2)),
                                        noise2Step(e, "o2", e.v("b2"), p(e, "center2")),
                                        set(
                                                e,
                                                o2,
                                                oscRead(
                                                        e,
                                                        "oscwave2",
                                                        "w2_host",
                                                        "w2_custom",
                                                        pt(e, "t_ph2"),
                                                        "t_lvl2",
                                                        e.v("b2"))))))));
        oscBody.addAll(mixCase);
        oscBody.add(set(
                e,
                o,
                add(
                        e,
                        o,
                        oscRead(
                                e,
                                "oscwave3",
                                "w3_host",
                                "w3_custom",
                                pt(e, "t_phasesub"),
                                "t_lvl3",
                                p(e, "subvol")))));
        oscBody.add(set(
                e,
                o,
                e.ifElse(
                        le(e, p(e, "ampgain"), e.n(32)),
                        e.mb.operatorShr(mul(e, o, p(e, "ampgain")), e.n(5)),
                        e.mb.operatorShr(mul(e, o, sub(e, p(e, "ampgain"), e.n(16))), e.n(4)))));
        oscBody.add(ifOnly(
                e,
                eq(e, p(e, "mixtype"), e.n(12)),
                e.let("oi", clampi(e, add(e, e.mb.operatorShr(o, e.n(9)), e.n(128)), 0, 255)),
                e.let("oi2", clampi(e, add(e, e.v("oi"), e.n(1)), 0, 255)),
                e.let("on1", idx(e, lw(e, "nonlin"), e.v("oi"))),
                e.let("on2", idx(e, lw(e, "nonlin"), e.v("oi2"))),
                set(
                        e,
                        o,
                        add(
                                e,
                                e.v("on1"),
                                e.mb.operatorShr(mul(e, sub(e, e.v("on2"), e.v("on1")), and(e, o, e.n(255))), e.n(8)))),
                set(e, o, clampi(e, o, -48000, 64000))));
        oscBody.add(e.ret(f32(e, o)));
        body.add(e.let(
                "osc",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_osc",
                        new String[] {"p", "t"},
                        e.lowerV("pif_fn_t"),
                        e.seq(oscBody.toArray(SourceExpr[]::new)))));

        var vca = e.seq(
                e.let("c0", pt(e, "t_aegcount")),
                set(e, pt(e, "t_aegcount"), sub(e, e.v("c0"), e.n(1))),
                ifOnly(
                        e,
                        or(
                                e,
                                or(
                                        e,
                                        eq(e, e.v("c0"), e.n(0)),
                                        and(
                                                e,
                                                eq(e, pt(e, "t_aegstate"), e.n(1)),
                                                ge(e, pt(e, "t_amp"), pt(e, "t_amptarget")))),
                                or(
                                        e,
                                        and(
                                                e,
                                                eq(e, pt(e, "t_aegstate"), e.n(2)),
                                                le(e, pt(e, "t_amp"), pt(e, "t_amptarget"))),
                                        and(
                                                e,
                                                eq(e, pt(e, "t_aegstate"), e.n(4)),
                                                lt(e, pt(e, "t_amp"), pt(e, "t_amptarget"))))),
                        set(e, pt(e, "t_aegstate"), add(e, pt(e, "t_aegstate"), e.n(1))),
                        e.let("st", pt(e, "t_aegstate")),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(2)),
                                set(e, pt(e, "t_aegcount"), p(e, "aeg_dec")),
                                set(e, pt(e, "t_amptarget"), p(e, "aeg_level")),
                                set(e, pt(e, "t_ampadd"), div(e, sub(e, pt(e, "t_amptarget"), vol), p(e, "aeg_dec")))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(3)),
                                set(e, pt(e, "t_aegcount"), p(e, "aeg_sus")),
                                set(e, pt(e, "t_amp"), pt(e, "t_amptarget")),
                                set(e, pt(e, "t_ampadd"), e.n(0))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(4)),
                                set(e, pt(e, "t_aegcount"), add(e, p(e, "aeg_rel"), e.n(100))),
                                set(
                                        e,
                                        pt(e, "t_ampadd"),
                                        i32(
                                                e,
                                                div(
                                                        e,
                                                        e.mb.operatorNeg(mul(e, p(e, "aeg_susfrac"), f32(e, vol))),
                                                        f32(e, p(e, "aeg_rel"))))),
                                set(e, pt(e, "t_amptarget"), e.n(0))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(5)),
                                set(e, pt(e, "t_aegstate"), e.n(0)),
                                set(e, pt(e, "t_aegcount"), e.n(-1)),
                                set(e, pt(e, "t_amp"), e.n(0)),
                                set(e, pt(e, "t_note"), e.n(0)))),
                set(e, pt(e, "t_amp"), add(e, pt(e, "t_amp"), pt(e, "t_ampadd"))),
                e.ret(e.mb.labeled(
                        "vca",
                        e.seq(
                                ifOnly(
                                        e,
                                        ne(e, p(e, "l_amp"), e.n(0)),
                                        e.let(
                                                "a",
                                                add(
                                                        e,
                                                        f32(e, pt(e, "t_amp")),
                                                        f32(
                                                                e,
                                                                shl(
                                                                        e,
                                                                        mul(
                                                                                e,
                                                                                lfoRead(
                                                                                        e,
                                                                                        "lfo1tab",
                                                                                        pt(e, "t_phlfo1")),
                                                                                pt(e, "t_l1vol")),
                                                                        e.n(5))))),
                                        e.mb.return_(
                                                "vca",
                                                mul(
                                                        e,
                                                        e.ifElse(lt(e, e.v("a"), e.n(0)), f32(e, e.n(0)), e.v("a")),
                                                        e.n(AMPCONV)))),
                                e.mb.return_("vca", mul(e, f32(e, pt(e, "t_amp")), e.n(AMPCONV)))))));
        body.add(e.let(
                "vca", EcoBuzz.funcDef(e, e.v("ll"), "mw_vca", new String[] {"p", "t"}, e.lowerV("pif_fn_t"), vca)));

        var uamp = pt(e, "t_uegamp");
        var lfoAmts1 = new TList<SourceExpr>();
        lfoAmts1.add(set(e, pt(e, "t_l1osc1"), e.mb.operatorShr(mul(e, e.v("n"), e.n(ATTR_LFO_SCALE)), e.n(8))));
        lfoAmts1.add(
                set(e, pt(e, "t_l1pw1"), mul(e, f32(e, e.v("n")), e.n(ATTR_LFO_SCALE / (256.0 * 127.0 * 32768.0)))));
        lfoAmts1.add(set(e, pt(e, "t_l1vol"), e.mb.operatorShr(mul(e, e.v("n"), e.n(ATTR_LFO_SCALE)), e.n(8))));
        lfoAmts1.add(set(e, pt(e, "t_l1cut"), e.mb.operatorShr(mul(e, e.v("n"), e.n(ATTR_LFO_SCALE)), e.n(8))));
        var lfoAmts2 = new TList<SourceExpr>();
        lfoAmts2.add(set(e, pt(e, "t_l2osc2"), e.mb.operatorShr(mul(e, e.v("n"), e.n(ATTR_LFO_SCALE)), e.n(8))));
        lfoAmts2.add(
                set(e, pt(e, "t_l2pw2"), mul(e, f32(e, e.v("n")), e.n(ATTR_LFO_SCALE / (256.0 * 127.0 * 32768.0)))));
        lfoAmts2.add(set(e, pt(e, "t_l2mix"), e.mb.operatorShr(mul(e, e.v("n"), e.n(ATTR_LFO_SCALE)), e.n(8))));
        lfoAmts2.add(set(e, pt(e, "t_l2reso"), e.mb.operatorShr(mul(e, e.v("n"), e.n(ATTR_LFO_SCALE)), e.n(8))));
        var uegDest = new TList<SourceExpr>();
        uegDest.add(e.let("ud", p(e, "uegdest")));
        var pfTab = lfoOsc(e, "lfoosc", e.mb.operatorShr(uamp, e.n(13)));
        uegDest.add(ifOnly(
                e,
                or(e, eq(e, e.v("ud"), e.n(1)), eq(e, e.v("ud"), e.n(3))),
                set(e, pt(e, "t_pf1"), pfTab),
                set(e, pt(e, "t_pitchmodactive"), e.n(1))));
        uegDest.add(ifOnly(
                e,
                or(e, eq(e, e.v("ud"), e.n(2)), eq(e, e.v("ud"), e.n(3))),
                set(e, pt(e, "t_pf2"), pfTab),
                set(e, pt(e, "t_pitchmodactive"), e.n(1))));
        uegDest.add(ifOnly(
                e, eq(e, e.v("ud"), e.n(4)), set(e, pt(e, "t_uegpw1"), mul(e, f32(e, uamp), e.n(1.0 / 134217600.0)))));
        uegDest.add(ifOnly(
                e, eq(e, e.v("ud"), e.n(5)), set(e, pt(e, "t_uegpw2"), mul(e, f32(e, uamp), e.n(1.0 / 134217600.0)))));
        uegDest.add(ifOnly(e, eq(e, e.v("ud"), e.n(6)), set(e, pt(e, "t_uegmix"), div(e, uamp, e.n(0xFFFFF)))));
        var amts1 = new TList<SourceExpr>();
        amts1.add(e.let("n", clampi(e, add(e, p(e, "lfo1amount"), e.mb.operatorShr(uamp, e.n(19))), 0, 256)));
        amts1.addAll(lfoAmts1);
        uegDest.add(ifOnly(e, eq(e, e.v("ud"), e.n(7)), amts1.toArray(SourceExpr[]::new)));
        var amts2 = new TList<SourceExpr>();
        amts2.add(e.let("n", clampi(e, add(e, p(e, "lfo2amount"), e.mb.operatorShr(uamp, e.n(19))), 0, 255)));
        amts2.addAll(lfoAmts2);
        uegDest.add(ifOnly(e, eq(e, e.v("ud"), e.n(8)), amts2.toArray(SourceExpr[]::new)));
        uegDest.add(ifOnly(e, eq(e, e.v("ud"), e.n(9)), set(e, pt(e, "t_uegreso"), e.mb.operatorShr(uamp, e.n(19)))));
        uegDest.add(ifOnly(e, eq(e, e.v("ud"), e.n(10)), set(e, pt(e, "t_uegphase"), uamp)));
        var ueg = ifOnly(
                e,
                ne(e, pt(e, "t_uegstate"), e.n(0)),
                e.let("c0", pt(e, "t_uegcount")),
                set(e, pt(e, "t_uegcount"), sub(e, e.v("c0"), e.n(1))),
                ifOnly(
                        e,
                        or(
                                e,
                                eq(e, e.v("c0"), e.n(0)),
                                or(
                                        e,
                                        and(e, gt(e, pt(e, "t_uegadd"), e.n(0)), gt(e, uamp, pt(e, "t_uegtarget"))),
                                        and(e, lt(e, pt(e, "t_uegadd"), e.n(0)), lt(e, uamp, pt(e, "t_uegtarget"))))),
                        set(e, pt(e, "t_uegstate"), add(e, pt(e, "t_uegstate"), e.n(1))),
                        e.let("st", pt(e, "t_uegstate")),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(2)),
                                set(e, pt(e, "t_uegcount"), p(e, "ueg_dec")),
                                set(e, pt(e, "t_uegtarget"), p(e, "ueg_level")),
                                set(
                                        e,
                                        pt(e, "t_uegadd"),
                                        div(e, sub(e, pt(e, "t_uegtarget"), p(e, "uenvmod")), p(e, "ueg_dec")))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(3)),
                                set(e, pt(e, "t_uegcount"), p(e, "ueg_sus")),
                                set(e, pt(e, "t_uegadd"), e.n(0))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(4)),
                                set(e, pt(e, "t_uegcount"), p(e, "ueg_rel")),
                                set(e, pt(e, "t_uegadd"), div(e, e.mb.operatorNeg(p(e, "ueg_level")), p(e, "ueg_rel"))),
                                set(e, pt(e, "t_uegtarget"), e.n(0))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(5)),
                                set(e, pt(e, "t_uegstate"), e.n(0)),
                                set(e, pt(e, "t_uegcount"), e.n(0)),
                                set(e, uamp, e.n(0)),
                                set(e, pt(e, "t_uegtarget"), e.n(0)),
                                set(e, pt(e, "t_uegadd"), e.n(0)),
                                set(e, pt(e, "t_pf1"), e.n(1)),
                                set(e, pt(e, "t_pf2"), e.n(1)),
                                set(e, pt(e, "t_uegmix"), e.n(0)),
                                set(e, pt(e, "t_uegpw1"), e.n(0)),
                                set(e, pt(e, "t_uegpw2"), e.n(0)),
                                set(e, pt(e, "t_uegreso"), e.n(0)),
                                set(e, pt(e, "t_uegphase"), e.n(0)))),
                set(e, uamp, add(e, uamp, pt(e, "t_uegadd"))),
                set(e, pt(e, "t_pitchmodactive"), e.n(0)),
                ifOnly(
                        e,
                        and(e, ne(e, p(e, "uegdest"), e.n(0)), ne(e, uamp, e.n(0))),
                        uegDest.toArray(SourceExpr[]::new)));
        body.add(e.let(
                "ueg", EcoBuzz.funcDef(e, e.v("ll"), "mw_ueg", new String[] {"p", "t"}, e.lowerV("pi_fn_t"), ueg)));

        var x = e.v("x");
        var iirK = idx(e, p(e, "t_iir_coef"), mul(e, t, e.n(9)));
        Function<SourceExpr, SourceExpr> coef = k -> idx(e, p(e, "t_iir_coef"), add(e, mul(e, t, e.n(9)), k));
        Function<SourceExpr, SourceExpr> hist = k -> idx(e, p(e, "t_iir_hist"), add(e, mul(e, t, e.n(4)), k));
        var sec = e.v("sec");
        var iirBody = e.seq(
                ifOnly(
                        e,
                        le(e, pt(e, "t_count"), e.n(0)),
                        ifOnly(
                                e,
                                or(
                                        e,
                                        ne(e, pt(e, "t_iir_lastcut"), e.v("cutf")),
                                        ne(e, pt(e, "t_iir_lastres"), e.v("resf"))),
                                e.let(
                                        "cf0r",
                                        add(
                                                e,
                                                call(
                                                        e,
                                                        "pow",
                                                        mul(
                                                                e,
                                                                EcoBuzz.conv(e, "t_f64", e.v("cutf")),
                                                                e.n(Math.sqrt(22000.0) / 32768.0)),
                                                        EcoBuzz.conv(e, "t_f64", e.n(2))),
                                                e.n(70))),
                                e.let(
                                        "cf0",
                                        e.ifElse(
                                                gt(e, e.v("cf0r"), e.n(22020)),
                                                EcoBuzz.conv(e, "t_f64", e.n(22020)),
                                                e.ifElse(
                                                        lt(e, e.v("cf0r"), e.n(50)),
                                                        EcoBuzz.conv(e, "t_f64", e.n(50)),
                                                        e.v("cf0r")))),
                                e.let(
                                        "cf1",
                                        e.ifElse(
                                                eq(e, p(e, "phat"), e.n(3)),
                                                e.ifElse(
                                                        lt(e, mul(e, e.v("cf0"), e.n(0.8)), e.n(50)),
                                                        EcoBuzz.conv(e, "t_f64", e.n(50)),
                                                        mul(e, e.v("cf0"), e.n(0.8))),
                                                e.v("cf0"))),
                                e.let("resd", EcoBuzz.conv(e, "t_f64", e.v("resf"))),
                                e.let(
                                        "r0",
                                        e.ifElse(
                                                lt(e, e.v("cf1"), e.n(550)),
                                                mul(
                                                        e,
                                                        div(e, EcoBuzz.conv(e, "t_f64", e.n(1)), e.v("resd")),
                                                        mul(
                                                                e,
                                                                sub(e, EcoBuzz.conv(e, "t_f64", e.n(550)), e.v("cf1")),
                                                                e.n(10.0 / 550.0))),
                                                div(e, EcoBuzz.conv(e, "t_f64", e.n(1)), e.v("resd")))),
                                e.let(
                                        "rr",
                                        e.ifElse(
                                                gt(e, e.v("r0"), e.n(1)),
                                                EcoBuzz.conv(e, "t_f64", e.n(1)),
                                                e.ifElse(
                                                        lt(e, e.v("r0"), e.n(0.005)),
                                                        EcoBuzz.conv(e, "t_f64", e.n(0.005)),
                                                        e.v("r0")))),
                                e.let("kk", EcoBuzz.conv(e, "t_f64", e.n(1))),
                                forRange(
                                        e,
                                        "sec",
                                        pt(e, "t_iir_len"),
                                        e.let("fc", e.ifElse(eq(e, sec, e.n(0)), e.v("cf0"), e.v("cf1"))),
                                        e.let(
                                                "wp",
                                                mul(
                                                        e,
                                                        call(e, "tan", mul(e, e.v("fc"), e.n(PI / RATE))),
                                                        e.n(2 * RATE))),
                                        e.let(
                                                "pb1",
                                                div(
                                                        e,
                                                        mul(
                                                                e,
                                                                e.ifElse(
                                                                        eq(e, sec, e.n(0)),
                                                                        EcoBuzz.conv(e, "t_f64", e.n(0.765367)),
                                                                        EcoBuzz.conv(e, "t_f64", e.n(1.847759))),
                                                                e.v("rr")),
                                                        e.v("wp"))),
                                        e.let(
                                                "pb2",
                                                div(e, EcoBuzz.conv(e, "t_f64", e.n(1)), mul(e, e.v("wp"), e.v("wp")))),
                                        e.let(
                                                "bd",
                                                add(
                                                        e,
                                                        add(
                                                                e,
                                                                mul(e, e.v("pb2"), e.n(4 * RATE * RATE)),
                                                                mul(e, e.v("pb1"), e.n(2 * RATE))),
                                                        e.n(1))),
                                        set(e, e.v("kk"), div(e, e.v("kk"), e.v("bd"))),
                                        e.let("cb", add(e, mul(e, sec, e.n(4)), e.n(1))),
                                        set(
                                                e,
                                                coef.apply(e.v("cb")),
                                                div(
                                                        e,
                                                        sub(
                                                                e,
                                                                EcoBuzz.conv(e, "t_f64", e.n(2)),
                                                                mul(e, e.v("pb2"), e.n(8 * RATE * RATE))),
                                                        e.v("bd"))),
                                        set(
                                                e,
                                                coef.apply(add(e, e.v("cb"), e.n(1))),
                                                div(
                                                        e,
                                                        add(
                                                                e,
                                                                sub(
                                                                        e,
                                                                        mul(e, e.v("pb2"), e.n(4 * RATE * RATE)),
                                                                        mul(e, e.v("pb1"), e.n(2 * RATE))),
                                                                e.n(1)),
                                                        e.v("bd"))),
                                        set(e, coef.apply(add(e, e.v("cb"), e.n(2))), EcoBuzz.conv(e, "t_f64", e.n(2))),
                                        set(
                                                e,
                                                coef.apply(add(e, e.v("cb"), e.n(3))),
                                                EcoBuzz.conv(e, "t_f64", e.n(1)))),
                                set(e, iirK, e.v("kk")),
                                set(e, pt(e, "t_iir_lastcut"), e.v("cutf")),
                                set(e, pt(e, "t_iir_lastres"), e.v("resf")),
                                set(e, pt(e, "t_count"), e.n(24)))),
                e.let("outp", mul(e, EcoBuzz.conv(e, "t_f64", e.v("x")), iirK)),
                forRange(
                        e,
                        "sec",
                        pt(e, "t_iir_len"),
                        e.let("h1", EcoBuzz.conv(e, "t_f64", hist.apply(mul(e, sec, e.n(2))))),
                        e.let("h2", EcoBuzz.conv(e, "t_f64", hist.apply(add(e, mul(e, sec, e.n(2)), e.n(1))))),
                        e.let("cb", add(e, mul(e, sec, e.n(4)), e.n(1))),
                        set(e, e.v("outp"), sub(e, e.v("outp"), mul(e, e.v("h1"), coef.apply(e.v("cb"))))),
                        e.let("newh", sub(e, e.v("outp"), mul(e, e.v("h2"), coef.apply(add(e, e.v("cb"), e.n(1)))))),
                        set(
                                e,
                                e.v("outp"),
                                add(e, e.v("newh"), mul(e, e.v("h1"), coef.apply(add(e, e.v("cb"), e.n(2)))))),
                        set(
                                e,
                                e.v("outp"),
                                add(e, e.v("outp"), mul(e, e.v("h2"), coef.apply(add(e, e.v("cb"), e.n(3)))))),
                        set(e, hist.apply(add(e, mul(e, sec, e.n(2)), e.n(1))), f32(e, e.v("h1"))),
                        set(e, hist.apply(mul(e, sec, e.n(2))), f32(e, e.v("newh")))),
                e.ret(e.ifElse(
                        eq(e, p(e, "phat"), e.n(2)), sub(e, e.v("x"), f32(e, e.v("outp"))), f32(e, e.v("outp")))));
        body.add(e.let(
                "iir",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_iir",
                        new String[] {"p", "t", "x", "cutf", "resf"},
                        e.lowerV("iir_fn_t"),
                        iirBody)));

        var cf = idx(e, lw(e, "coefs"), e.v("ofs"));
        Function<Integer, SourceExpr> cfk = k -> idx(e, lw(e, "coefs"), add(e, e.v("ofs"), e.n(k)));
        var fegStep = ifOnly(
                e,
                ne(e, pt(e, "t_fegstate"), e.n(0)),
                e.let("c0", pt(e, "t_fegcount")),
                set(e, pt(e, "t_fegcount"), sub(e, e.v("c0"), e.n(1))),
                ifOnly(
                        e,
                        or(
                                e,
                                eq(e, e.v("c0"), e.n(0)),
                                or(
                                        e,
                                        and(
                                                e,
                                                lt(e, pt(e, "t_cutadd"), e.n(0)),
                                                lt(e, pt(e, "t_cut"), pt(e, "t_cuttarget"))),
                                        and(
                                                e,
                                                gt(e, pt(e, "t_cutadd"), e.n(0)),
                                                gt(e, pt(e, "t_cut"), pt(e, "t_cuttarget"))))),
                        set(e, pt(e, "t_fegstate"), add(e, pt(e, "t_fegstate"), e.n(1))),
                        e.let("st", pt(e, "t_fegstate")),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(2)),
                                set(e, pt(e, "t_fegcount"), p(e, "feg_dec")),
                                set(e, pt(e, "t_cuttarget"), p(e, "feg_level")),
                                set(
                                        e,
                                        pt(e, "t_cutadd"),
                                        div(
                                                e,
                                                sub(e, pt(e, "t_cuttarget"), pt(e, "t_cut")),
                                                f32(e, pt(e, "t_fegcount"))))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(3)),
                                set(e, pt(e, "t_fegcount"), p(e, "feg_sus")),
                                set(e, pt(e, "t_cutadd"), e.n(0))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(4)),
                                set(e, pt(e, "t_fegcount"), p(e, "feg_rel")),
                                set(
                                        e,
                                        pt(e, "t_cutadd"),
                                        div(e, e.mb.operatorNeg(pt(e, "t_cut")), f32(e, p(e, "feg_rel")))),
                                set(e, pt(e, "t_cuttarget"), e.n(0))),
                        ifOnly(
                                e,
                                eq(e, e.v("st"), e.n(5)),
                                set(e, pt(e, "t_fegstate"), e.n(0)),
                                set(e, pt(e, "t_fegcount"), e.n(0)),
                                set(e, pt(e, "t_cut"), e.n(0)),
                                set(e, pt(e, "t_cutadd"), e.n(0)),
                                set(e, pt(e, "t_cuttarget"), e.n(0)))),
                set(e, pt(e, "t_cut"), add(e, pt(e, "t_cut"), pt(e, "t_cutadd"))));
        var lfoCutTerm = e.ifElse(
                ne(e, p(e, "l_vcf"), e.n(0)),
                e.seq(
                        set(
                                e,
                                pt(e, "t_oldoldout"),
                                add(
                                        e,
                                        pt(e, "t_oldoldout"),
                                        mul(e, sub(e, pt(e, "t_oldout"), pt(e, "t_oldoldout")), e.v("vcfk")))),
                        e.mb.return_("lct", mul(e, f2i(e, pt(e, "t_oldoldout")), pt(e, "t_l1cut")))),
                mul(e, f2i(e, f32(e, lfoRead(e, "lfo1tab", pt(e, "t_phlfo1")))), pt(e, "t_l1cut")));
        var filterBody = e.seq(
                fegStep,
                e.let(
                        "r",
                        clampi(
                                e,
                                add(
                                        e,
                                        add(e, p(e, "resonance"), pt(e, "t_uegreso")),
                                        e.ifElse(
                                                ne(e, p(e, "l_reso"), e.n(0)),
                                                e.mb.operatorShr(
                                                        mul(
                                                                e,
                                                                lfoRead(e, "lfo2tab", pt(e, "t_phlfo2")),
                                                                pt(e, "t_l2reso")),
                                                        e.n(15)),
                                                EcoBuzz.i32(e, e.n(0)))),
                                0,
                                127)),
                e.ret(e.mb.labeled(
                        "flt",
                        e.seq(
                                ifOnly(
                                        e,
                                        ne(e, p(e, "phat"), e.n(0)),
                                        ifOnly(
                                                e,
                                                gt(e, pt(e, "t_count"), e.n(0)),
                                                set(e, pt(e, "t_count"), sub(e, pt(e, "t_count"), e.n(1)))),
                                        e.let("cutf", f32(e, e.n(0))),
                                        e.let("resf", f32(e, e.n(0))),
                                        ifOnly(
                                                e,
                                                le(e, pt(e, "t_count"), e.n(0)),
                                                e.let("vcfk", f32(e, e.n(0.1))),
                                                e.let(
                                                        "cutr",
                                                        e.ifElse(
                                                                ne(e, p(e, "l_cut"), e.n(0)),
                                                                add(
                                                                        e,
                                                                        mul(
                                                                                e,
                                                                                add(e, p(e, "cutoff"), pt(e, "t_cut")),
                                                                                e.n(256)),
                                                                        f32(
                                                                                e,
                                                                                e.mb.operatorShr(
                                                                                        e.mb.labeled("lct", lfoCutTerm),
                                                                                        e.n(7)))),
                                                                mul(
                                                                        e,
                                                                        add(e, p(e, "cutoff"), pt(e, "t_cut")),
                                                                        e.n(256)))),
                                                set(
                                                        e,
                                                        e.v("cutf"),
                                                        e.ifElse(
                                                                gt(e, e.v("cutr"), e.n(32768)),
                                                                f32(e, e.n(32768)),
                                                                e.ifElse(
                                                                        lt(e, e.v("cutr"), e.n(0)),
                                                                        f32(e, e.n(0)),
                                                                        e.v("cutr")))),
                                                set(e, e.v("resf"), div(e, f32(e, add(e, e.v("r"), e.n(7))), e.n(7)))),
                                        set(
                                                e,
                                                pt(e, "t_iir_len"),
                                                e.ifElse(
                                                        ne(e, p(e, "db24"), e.n(0)),
                                                        EcoBuzz.i32(e, e.n(2)),
                                                        EcoBuzz.i32(e, e.n(1)))),
                                        e.mb.return_("flt", call(e, "iir", e.v("p"), t, x, e.v("cutf"), e.v("resf")))),
                                e.let("vcfk", f32(e, e.n(0.5))),
                                e.let(
                                        "c",
                                        clampi(
                                                e,
                                                e.ifElse(
                                                        ne(e, p(e, "l_cut"), e.n(0)),
                                                        i32(
                                                                e,
                                                                add(
                                                                        e,
                                                                        add(e, p(e, "cutoff"), pt(e, "t_cut")),
                                                                        f32(
                                                                                e,
                                                                                e.mb.operatorShr(
                                                                                        e.mb.labeled("lct", lfoCutTerm),
                                                                                        e.n(15))))),
                                                        f2i(e, add(e, p(e, "cutoff"), pt(e, "t_cut")))),
                                                1,
                                                127)),
                                e.let(
                                        "ofs",
                                        add(
                                                e,
                                                shl(e, add(e, shl(e, e.v("c"), e.n(7)), e.v("r")), e.n(3)),
                                                p(e, "ftype_base"))),
                                e.let("y", biquadSum(e, cf, cfk, x, IList.of("t_x1", "t_x2", "t_y1", "t_y2"))),
                                set(e, pt(e, "t_y2"), pt(e, "t_y1")),
                                set(e, pt(e, "t_y1"), e.v("y")),
                                set(e, pt(e, "t_x2"), pt(e, "t_x1")),
                                set(e, pt(e, "t_x1"), x),
                                ifOnly(
                                        e,
                                        eq(e, p(e, "db24"), e.n(0)),
                                        e.mb.return_(
                                                "flt",
                                                e.ifElse(ne(e, p(e, "peak"), e.n(0)), add(e, e.v("y"), x), e.v("y")))),
                                e.let(
                                        "y24",
                                        biquadSum(
                                                e,
                                                cf,
                                                cfk,
                                                e.v("y"),
                                                IList.of("t_x241", "t_x242", "t_y241", "t_y242"))),
                                set(e, pt(e, "t_y242"), pt(e, "t_y241")),
                                set(e, pt(e, "t_y241"), e.v("y24")),
                                set(e, pt(e, "t_x242"), pt(e, "t_x241")),
                                set(e, pt(e, "t_x241"), e.v("y")),
                                ifOnly(
                                        e,
                                        eq(e, p(e, "db18"), e.n(0)),
                                        e.mb.return_(
                                                "flt",
                                                e.ifElse(
                                                        ne(e, p(e, "peak"), e.n(0)),
                                                        add(e, e.v("y24"), x),
                                                        e.v("y24")))),
                                e.mb.return_(
                                        "flt",
                                        e.ifElse(
                                                ne(e, p(e, "peak"), e.n(0)),
                                                mul(e, add(e, add(e, e.v("y"), e.v("y24")), x), e.n(0.5)),
                                                mul(e, add(e, e.v("y"), e.v("y24")), e.n(0.5))))))));
        body.add(e.let(
                "filter",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "mw_filter", new String[] {"p", "t", "x"}, e.lowerV("filter_fn_t"), filterBody)));

        var lfo1Idx = add(
                e,
                e.mb.operatorShr(mul(e, lfoRead(e, "lfo1tab", pt(e, "t_phlfo1")), pt(e, "t_l1osc1")), e.n(8)),
                e.n(0));
        var lfo1Lock = add(
                e,
                e.mb.operatorShr(
                        mul(
                                e,
                                lfoReadIdx(e, "lfo1tab", e.mb.operatorUshr(pt(e, "t_phase2"), e.n(16))),
                                pt(e, "t_l1osc1")),
                        e.n(8)),
                e.n(0));
        var newPhases = new TList<SourceExpr>();
        newPhases.add(e.let("pm1", f32(e, e.n(1))));
        newPhases.add(ifOnly(
                e,
                ne(e, pt(e, "t_glideactive"), e.n(0)),
                set(e, e.v("pm1"), pt(e, "t_glidefactor")),
                set(e, pt(e, "t_glidefactor"), mul(e, pt(e, "t_glidefactor"), pt(e, "t_glidemul"))),
                e.let("gc", pt(e, "t_glidecount")),
                set(e, pt(e, "t_glidecount"), sub(e, e.v("gc"), e.n(1))),
                ifOnly(
                        e,
                        eq(e, e.v("gc"), e.n(0)),
                        set(e, pt(e, "t_glideactive"), e.n(0)),
                        set(e, pt(e, "t_padd1"), i32(e, mul(e, mul(e, pt(e, "t_freq"), p(e, "tsdsf")), e.n(65536)))),
                        set(
                                e,
                                pt(e, "t_padd2"),
                                i32(
                                        e,
                                        mul(
                                                e,
                                                mul(
                                                        e,
                                                        mul(
                                                                e,
                                                                mul(e, pt(e, "t_freq"), p(e, "detune_semi")),
                                                                p(e, "detune_fine")),
                                                        p(e, "tsdsf")),
                                                e.n(65536)))))));
        newPhases.add(ifOnly(
                e, ne(e, p(e, "bendfactor"), e.n(1)), set(e, e.v("pm1"), mul(e, e.v("pm1"), p(e, "bendfactor")))));
        newPhases.add(ifOnly(
                e,
                and(e, ne(e, p(e, "l_vib"), e.n(0)), eq(e, p(e, "l_1lock2"), e.n(0))),
                set(e, e.v("pm1"), mul(e, e.v("pm1"), lfoOsc(e, "lfovib", lfo1Idx)))));
        newPhases.add(e.let("pm2", e.v("pm1")));
        newPhases.add(ifOnly(
                e,
                and(e, ne(e, p(e, "l_vib"), e.n(0)), ne(e, p(e, "l_1lock2"), e.n(0))),
                set(e, e.v("pm1"), mul(e, e.v("pm1"), lfoOsc(e, "lfovib", lfo1Lock)))));
        newPhases.add(ifOnly(
                e,
                ne(e, p(e, "l_osc1"), e.n(0)),
                set(
                        e,
                        e.v("pm1"),
                        mul(
                                e,
                                e.v("pm1"),
                                e.ifElse(
                                        ne(e, p(e, "l_1lock2"), e.n(0)),
                                        lfoOsc(e, "lfoosc", lfo1Lock),
                                        lfoOsc(e, "lfoosc", lfo1Idx))))));
        newPhases.add(ifOnly(
                e,
                ne(e, p(e, "l_osc2"), e.n(0)),
                e.let(
                        "l2src",
                        e.ifElse(
                                ne(e, p(e, "l_2lock1"), e.n(0)),
                                lfoReadIdx(e, "lfo2tab", e.mb.operatorUshr(pt(e, "t_phase1"), e.n(16))),
                                e.ifElse(
                                        ne(e, p(e, "l_2lock2"), e.n(0)),
                                        lfoReadIdx(e, "lfo2tab", e.mb.operatorUshr(pt(e, "t_phase2"), e.n(16))),
                                        lfoRead(e, "lfo2tab", pt(e, "t_phlfo1"))))),
                set(
                        e,
                        e.v("pm2"),
                        mul(
                                e,
                                e.v("pm2"),
                                lfoOsc(
                                        e,
                                        "lfoosc",
                                        e.mb.operatorShr(mul(e, e.v("l2src"), pt(e, "t_l2osc2")), e.n(8)))))));
        newPhases.add(ifOnly(
                e,
                ne(e, pt(e, "t_pitchmodactive"), e.n(0)),
                set(e, e.v("pm1"), mul(e, e.v("pm1"), pt(e, "t_pf1"))),
                set(e, e.v("pm2"), mul(e, e.v("pm2"), pt(e, "t_pf2")))));
        newPhases.add(e.let(
                "pa1",
                e.ifElse(
                        ne(e, e.v("pm1"), e.n(1)),
                        f2iD(e, mul(e, f64(e, pt(e, "t_padd1")), f64(e, e.v("pm1")))),
                        pt(e, "t_padd1"))));
        newPhases.add(set(e, pt(e, "t_phase1"), add(e, pt(e, "t_phase1"), e.v("pa1"))));
        newPhases.add(set(e, pt(e, "t_phasesub"), add(e, pt(e, "t_phasesub"), e.mb.operatorShr(e.v("pa1"), e.n(1)))));
        newPhases.add(set(
                e,
                pt(e, "t_phase2"),
                add(
                        e,
                        pt(e, "t_phase2"),
                        e.ifElse(
                                ne(e, e.v("pm2"), e.n(1)),
                                f2iD(e, mul(e, f64(e, pt(e, "t_padd2")), f64(e, e.v("pm2")))),
                                pt(e, "t_padd2")))));
        Function<String[], SourceExpr> centerUpdate = names -> {
            var centerT = names[0];
            var lflag = names[1];
            var lfoTab = names[2];
            var phl = names[3];
            var lpw = names[4];
            var uegpw = names[5];
            var psa = names[6];
            var psb = names[7];
            var cc = names[8];
            var centerG = names[9];
            return e.seq(
                    e.ifElse(
                            or(e, ne(e, p(e, lflag), e.n(0)), ne(e, pt(e, uegpw), e.n(0))),
                            e.seq(
                                    e.let(
                                            "cv",
                                            add(
                                                    e,
                                                    add(
                                                            e,
                                                            p(e, centerG),
                                                            e.ifElse(
                                                                    ne(e, p(e, lflag), e.n(0)),
                                                                    mul(
                                                                            e,
                                                                            f32(e, lfoRead(e, lfoTab, pt(e, phl))),
                                                                            pt(e, lpw)),
                                                                    f32(e, e.n(0)))),
                                                    pt(e, uegpw))),
                                    set(
                                            e,
                                            pt(e, centerT),
                                            e.ifElse(
                                                    lt(e, e.v("cv"), e.n(0)),
                                                    f32(e, e.n(0)),
                                                    e.ifElse(gt(e, e.v("cv"), e.n(1)), f32(e, e.n(1)), e.v("cv"))))),
                            set(e, pt(e, centerT), p(e, centerG))),
                    set(e, pt(e, psa), div(e, f32(e, e.n(0.5)), pt(e, centerT))),
                    set(e, pt(e, psb), div(e, f32(e, e.n(0.5)), sub(e, f32(e, e.n(1)), pt(e, centerT)))),
                    set(e, pt(e, cc), f2i(e, mul(e, pt(e, centerT), e.n(0x8000000)))));
        };
        newPhases.add(ifOnly(
                e,
                ne(e, and(e, pt(e, "t_phase1"), e.n(0xf8000000)), e.n(0)),
                centerUpdate.apply(new String[] {
                    "t_center1",
                    "l_pw1",
                    "lfo1tab",
                    "t_phlfo1",
                    "t_l1pw1",
                    "t_uegpw1",
                    "t_ps1a",
                    "t_ps1b",
                    "t_c1",
                    "center1"
                }),
                centerUpdate.apply(new String[] {
                    "t_center2",
                    "l_pw2",
                    "lfo2tab",
                    "t_phlfo2",
                    "t_l2pw2",
                    "t_uegpw2",
                    "t_ps2a",
                    "t_ps2b",
                    "t_c2",
                    "center2"
                })));
        newPhases.add(ifOnly(
                e,
                and(e, ge(e, pt(e, "t_phase1"), e.n(0x8000000)), eq(e, p(e, "sync"), e.n(1))),
                set(e, pt(e, "t_phase2"), e.n(0))));
        newPhases.add(set(e, pt(e, "t_phase1"), and(e, pt(e, "t_phase1"), e.n(0x7ffffff))));
        newPhases.add(set(e, pt(e, "t_phase2"), and(e, pt(e, "t_phase2"), e.n(0x7ffffff))));
        newPhases.add(set(e, pt(e, "t_phasesub"), and(e, pt(e, "t_phasesub"), e.n(0x7ffffff))));
        newPhases.add(set(
                e,
                pt(e, "t_ph1"),
                e.ifElse(
                        lt(e, pt(e, "t_phase1"), pt(e, "t_c1")),
                        f2iD(e, mul(e, f64(e, pt(e, "t_phase1")), f64(e, pt(e, "t_ps1a")))),
                        f2iD(
                                e,
                                add(
                                        e,
                                        mul(
                                                e,
                                                f64(e, sub(e, pt(e, "t_phase1"), pt(e, "t_c1"))),
                                                f64(e, pt(e, "t_ps1b"))),
                                        e.n(0x4000000))))));
        newPhases.add(set(
                e,
                pt(e, "t_ph2"),
                e.ifElse(
                        lt(e, pt(e, "t_phase2"), pt(e, "t_c2")),
                        f2iD(e, mul(e, f64(e, pt(e, "t_phase2")), f64(e, pt(e, "t_ps2a")))),
                        f2iD(
                                e,
                                add(
                                        e,
                                        mul(
                                                e,
                                                f64(e, sub(e, pt(e, "t_phase2"), pt(e, "t_c2"))),
                                                f64(e, pt(e, "t_ps2b"))),
                                        e.n(0x4000000))))));
        newPhases.add(set(e, pt(e, "t_ph2"), add(e, add(e, pt(e, "t_ph2"), p(e, "phdiff2")), pt(e, "t_uegphase"))));
        newPhases.add(ifOnly(
                e,
                ne(e, p(e, "l_phase2"), e.n(0)),
                set(
                        e,
                        pt(e, "t_ph2"),
                        add(
                                e,
                                pt(e, "t_ph2"),
                                mul(
                                        e,
                                        f2i(e, f32(e, lfoRead(e, "lfo2tab", pt(e, "t_phlfo2")))),
                                        p(e, "lfo2amount"))))));
        newPhases.add(set(e, pt(e, "t_ph2"), and(e, pt(e, "t_ph2"), e.n(0x7ffffff))));
        newPhases.add(e.ifElse(
                ne(e, p(e, "l_1lock2"), e.n(0)),
                set(e, pt(e, "t_phlfo1"), shl(e, pt(e, "t_phase2"), e.n(5))),
                e.ifElse(
                        ne(e, p(e, "l_lfo1"), e.n(0)),
                        set(
                                e,
                                pt(e, "t_phlfo1"),
                                add(
                                        e,
                                        pt(e, "t_phlfo1"),
                                        i32(
                                                e,
                                                div(
                                                        e,
                                                        f32(e, e.n(0x200000 * 2048.0)),
                                                        f32(
                                                                e,
                                                                e.mb.operatorShr(
                                                                        shl(
                                                                                e,
                                                                                p(e, "spt"),
                                                                                f2i(
                                                                                        e,
                                                                                        f32(
                                                                                                e,
                                                                                                mul(
                                                                                                        e,
                                                                                                        mul(
                                                                                                                e,
                                                                                                                p(
                                                                                                                        e,
                                                                                                                        "lfo1freq"),
                                                                                                                lfoRead(
                                                                                                                        e,
                                                                                                                        "lfo2tab",
                                                                                                                        pt(
                                                                                                                                e,
                                                                                                                                "t_phlfo2"))),
                                                                                                        p(
                                                                                                                e,
                                                                                                                "lfo2amount"))))),
                                                                        e.n(15))))))),
                        set(e, pt(e, "t_phlfo1"), add(e, pt(e, "t_phlfo1"), p(e, "padd_lfo1"))))));
        newPhases.add(e.ifElse(
                ne(e, p(e, "l_2lock1"), e.n(0)),
                set(e, pt(e, "t_phlfo2"), shl(e, pt(e, "t_phase1"), e.n(5))),
                e.ifElse(
                        ne(e, p(e, "l_2lock2"), e.n(0)),
                        set(e, pt(e, "t_phlfo2"), shl(e, pt(e, "t_phase2"), e.n(5))),
                        set(e, pt(e, "t_phlfo2"), add(e, pt(e, "t_phlfo2"), p(e, "padd_lfo2"))))));
        body.add(e.let(
                "newphases",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_newphases",
                        new String[] {"p", "t"},
                        e.lowerV("pi_fn_t"),
                        e.seq(newPhases.toArray(SourceExpr[]::new)))));

        var i = e.v("i");
        var trackWork = forRange(
                e,
                "i",
                e.v("n"),
                set(e, p(e, "cutoff"), add(e, p(e, "cutoff"), p(e, "cutoff_add"))),
                ifOnly(
                        e,
                        or(
                                e,
                                and(e, gt(e, p(e, "cutoff_add"), e.n(0)), gt(e, p(e, "cutoff"), p(e, "cutoff_target"))),
                                and(
                                        e,
                                        lt(e, p(e, "cutoff_add"), e.n(0)),
                                        lt(e, p(e, "cutoff"), p(e, "cutoff_target")))),
                        set(e, p(e, "cutoff"), p(e, "cutoff_target"))),
                e.ifElse(
                        ne(e, pt(e, "t_aegstate"), e.n(0)),
                        e.seq(
                                call(e, "ueg", e.v("p"), t),
                                e.let("osc_o", mul(e, call(e, "osc", e.v("p"), t), call(e, "vca", e.v("p"), t))),
                                e.let("s", call(e, "filter", e.v("p"), t, add(e, e.v("osc_o"), pt(e, "t_oldout")))),
                                set(e, pt(e, "t_oldout"), e.v("osc_o")),
                                e.let(
                                        "sd",
                                        e.mb.labeled(
                                                "dist",
                                                e.seq(
                                                        ifOnly(
                                                                e,
                                                                eq(e, p(e, "dist"), e.n(1)),
                                                                e.mb.return_(
                                                                        "dist",
                                                                        e.ifElse(
                                                                                gt(e, e.v("s"), e.n(16000)),
                                                                                f32(e, e.n(32768)),
                                                                                e.ifElse(
                                                                                        lt(e, e.v("s"), e.n(-16000)),
                                                                                        f32(e, e.n(-32768)),
                                                                                        e.v("s"))))),
                                                        ifOnly(
                                                                e,
                                                                eq(e, p(e, "dist"), e.n(2)),
                                                                e.mb.return_(
                                                                        "dist",
                                                                        e.ifElse(
                                                                                gt(e, e.v("s"), e.n(32768)),
                                                                                f32(e, e.n(8000)),
                                                                                e.ifElse(
                                                                                        lt(e, e.v("s"), e.n(-32768)),
                                                                                        f32(e, e.n(-8000)),
                                                                                        e.v("s"))))),
                                                        ifOnly(
                                                                e,
                                                                eq(e, p(e, "dist"), e.n(3)),
                                                                e.mb.return_(
                                                                        "dist",
                                                                        e.ifElse(
                                                                                gt(e, e.v("s"), e.n(32768)),
                                                                                f32(e, e.n(32768)),
                                                                                e.ifElse(
                                                                                        lt(e, e.v("s"), e.n(-32768)),
                                                                                        f32(e, e.n(-32768)),
                                                                                        e.v("s"))))),
                                                        ifOnly(
                                                                e,
                                                                eq(e, p(e, "dist"), e.n(4)),
                                                                e.let(
                                                                        "di",
                                                                        clampi(
                                                                                e,
                                                                                add(
                                                                                        e,
                                                                                        e.mb.operatorShr(
                                                                                                f2i(e, e.v("s")),
                                                                                                e.n(9)),
                                                                                        e.n(128)),
                                                                                0,
                                                                                255)),
                                                                e.let(
                                                                        "di2",
                                                                        clampi(e, add(e, e.v("di"), e.n(1)), 0, 255)),
                                                                e.let("dn1", idx(e, lw(e, "nonlin"), e.v("di"))),
                                                                e.let("dn2", idx(e, lw(e, "nonlin"), e.v("di2"))),
                                                                e.mb.return_(
                                                                        "dist",
                                                                        f32(
                                                                                e,
                                                                                add(
                                                                                        e,
                                                                                        e.v("dn1"),
                                                                                        e.mb.operatorShr(
                                                                                                mul(
                                                                                                        e,
                                                                                                        sub(
                                                                                                                e,
                                                                                                                e.v(
                                                                                                                        "dn2"),
                                                                                                                e.v(
                                                                                                                        "dn1")),
                                                                                                        and(
                                                                                                                e,
                                                                                                                f2i(
                                                                                                                        e,
                                                                                                                        mul(
                                                                                                                                e,
                                                                                                                                e
                                                                                                                                        .v(
                                                                                                                                                "s"),
                                                                                                                                e
                                                                                                                                        .n(
                                                                                                                                                256))),
                                                                                                                e.n(
                                                                                                                        255))),
                                                                                                e.n(8)))))),
                                                        e.mb.return_("dist", e.v("s"))))),
                                set(
                                        e,
                                        idx(e, e.v("out"), mul(e, i, e.n(2))),
                                        add(e, idx(e, e.v("out"), mul(e, i, e.n(2))), e.v("sd"))),
                                set(
                                        e,
                                        idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                                        add(e, idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))), e.v("sd")))),
                        e.seq(e.let("zf", call(e, "filter", e.v("p"), t, f32(e, e.n(0)))))),
                call(e, "newphases", e.v("p"), t));
        body.add(e.let(
                "trackwork",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_trackwork",
                        new String[] {"p", "t", "out", "n"},
                        e.lowerV("track_fn_t"),
                        trackWork)));

        body = main;
        var init = new TList<SourceExpr>();
        init.add(call(e, "tables"));
        for (var f : I32_FIELDS) {
            if (f.equals("spt")) continue;
            init.add(set(e, p(e, f), e.n(0)));
        }
        for (var f : F32_FIELDS) init.add(set(e, p(e, f), e.n(0)));
        init.add(set(e, p(e, "tsdsf"), e.n(2048.0 / RATE)));
        init.add(set(e, p(e, "cutoff"), e.n(32)));
        init.add(set(e, p(e, "cutoff_target"), e.n(32)));
        init.add(set(e, p(e, "resonance"), e.n(32)));
        init.add(set(e, p(e, "ueg_att"), envSamples(e, EcoBuzz.i32(e, e.n(7)))));
        init.add(set(e, p(e, "ueg_dec"), envSamples(e, EcoBuzz.i32(e, e.n(11)))));
        init.add(set(e, p(e, "ueg_sus"), envSamples(e, EcoBuzz.i32(e, e.n(11)))));
        init.add(set(e, p(e, "ueg_rel"), envSamples(e, EcoBuzz.i32(e, e.n(11)))));
        init.add(set(e, p(e, "ueg_level"), e.n(127)));
        init.add(set(e, p(e, "feg_att"), envSamples(e, EcoBuzz.i32(e, e.n(7)))));
        init.add(set(e, p(e, "feg_dec"), envSamples(e, EcoBuzz.i32(e, e.n(15)))));
        init.add(set(e, p(e, "feg_sus"), envSamples(e, EcoBuzz.i32(e, e.n(14)))));
        init.add(set(e, p(e, "feg_level"), e.n(1)));
        init.add(set(e, p(e, "feg_rel"), envSamples(e, EcoBuzz.i32(e, e.n(15)))));
        init.add(set(e, p(e, "aeg_att"), envSamples(e, EcoBuzz.i32(e, e.n(5)))));
        init.add(set(e, p(e, "aeg_dec"), envSamples(e, EcoBuzz.i32(e, e.n(16)))));
        init.add(set(e, p(e, "aeg_sus"), envSamples(e, EcoBuzz.i32(e, e.n(16)))));
        init.add(set(e, p(e, "aeg_level"), e.n(127)));
        init.add(set(e, p(e, "aeg_rel"), envSamples(e, EcoBuzz.i32(e, e.n(32)))));
        for (var f : new String[] {"w1_custom", "w2_custom", "w3_custom", "oscwave1", "oscwave2", "oscwave3"}) {
            init.add(set(e, p(e, f), e.n(-1)));
        }
        init.add(set(e, p(e, "bendfactor"), e.n(1)));
        init.add(set(e, p(e, "bendglide"), e.n(1)));
        init.add(set(e, p(e, "detune_semi"), e.n(1)));
        init.add(set(e, p(e, "detune_fine"), e.n(1)));
        init.add(set(e, p(e, "subvol"), e.n(64)));
        init.add(set(e, p(e, "center1"), e.n(64.0 / 127)));
        init.add(set(e, p(e, "center2"), e.n(64.0 / 127)));
        init.add(set(e, p(e, "lfo1phdiff"), e.n(64 << 25)));
        init.add(set(e, p(e, "lfo2phdiff"), e.n(64 << 25)));
        init.add(set(e, p(e, "bal1"), e.n(63)));
        init.add(set(e, p(e, "bal2"), e.n(64)));
        init.add(set(e, p(e, "ampgain"), e.n(32)));
        init.add(set(e, p(e, "bendamt"), e.n(2)));
        for (int k = 0; k < NG; k++) init.add(set(e, ctl(e, k), e.n(GACT_DEFAULTS.get(k))));
        var tInit = new TList<SourceExpr>();
        for (var f : T_I32_FIELDS) tInit.add(set(e, pt(e, f), e.n(0)));
        for (var f : T_F32_FIELDS) tInit.add(set(e, pt(e, f), e.n(0)));
        tInit.add(set(e, pt(e, "t_r1"), e.n(26474)));
        tInit.add(set(e, pt(e, "t_r2"), e.n(13075)));
        tInit.add(set(e, pt(e, "t_r3"), e.n(18376)));
        tInit.add(set(e, pt(e, "t_r4"), e.n(31291)));
        tInit.add(set(e, pt(e, "t_aegcount"), e.n(-1)));
        tInit.add(set(e, pt(e, "t_fegcount"), e.n(-1)));
        tInit.add(set(e, pt(e, "t_uegcount"), e.n(-1)));
        tInit.add(set(e, pt(e, "t_center1"), p(e, "center1")));
        tInit.add(set(e, pt(e, "t_center2"), p(e, "center2")));
        tInit.add(set(e, pt(e, "t_ps1a"), div(e, f32(e, e.n(0.5)), pt(e, "t_center1"))));
        tInit.add(set(e, pt(e, "t_ps1b"), div(e, f32(e, e.n(0.5)), sub(e, f32(e, e.n(1)), pt(e, "t_center1")))));
        tInit.add(set(e, pt(e, "t_ps2a"), div(e, f32(e, e.n(0.5)), pt(e, "t_center2"))));
        tInit.add(set(e, pt(e, "t_ps2b"), div(e, f32(e, e.n(0.5)), sub(e, f32(e, e.n(1)), pt(e, "t_center2")))));
        tInit.add(set(e, pt(e, "t_c1"), f2i(e, mul(e, pt(e, "t_center1"), e.n(0x8000000)))));
        tInit.add(set(e, pt(e, "t_c2"), f2i(e, mul(e, pt(e, "t_center2"), e.n(0x8000000)))));
        tInit.add(set(e, pt(e, "t_volume"), e.n(64 << 20)));
        tInit.add(set(e, pt(e, "t_pf1"), e.n(1)));
        tInit.add(set(e, pt(e, "t_pf2"), e.n(1)));
        tInit.add(set(e, pt(e, "t_iir_lastcut"), e.n(-1)));
        tInit.add(set(e, pt(e, "t_iir_lastres"), e.n(-1)));
        tInit.add(set(e, pt(e, "t_count"), e.n(1)));
        tInit.add(set(e, pt(e, "t_iir_len"), e.n(2)));
        tInit.add(set(e, pt(e, "t_seed"), add(e, t, e.n(12345))));
        tInit.add(forRange(
                e, "k", e.n(9), set(e, idx(e, p(e, "t_iir_coef"), add(e, mul(e, t, e.n(9)), e.v("k"))), e.n(0))));
        tInit.add(forRange(
                e, "k", e.n(4), set(e, idx(e, p(e, "t_iir_hist"), add(e, mul(e, t, e.n(4)), e.v("k"))), e.n(0))));
        init.add(forRange(e, "t", e.n(TRACKS), tInit.toArray(SourceExpr[]::new)));
        init.add(resetVals(e));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        e.seq(init.toArray(SourceExpr[]::new)))));

        var tick = new TList<SourceExpr>();
        tick.add(e.let("mod", EcoBuzz.i32(e, e.n(0))));
        var resend1 = new TList<SourceExpr>();
        for (var k : MOD1_RESEND) resend1.add(ifOnly(e, eq(e, gval(e, k), e.n(-1)), set(e, gval(e, k), ctl(e, k))));
        var resend2 = new TList<SourceExpr>();
        for (var k : MOD2_RESEND) resend2.add(ifOnly(e, eq(e, gval(e, k), e.n(-1)), set(e, gval(e, k), ctl(e, k))));
        var md1 = new TList<SourceExpr>();
        md1.add(set(e, ctl(e, 52), e.v("v")));
        md1.add(set(e, p(e, "moddest1"), e.v("v")));
        md1.addAll(resend1);
        tick.add(onGval(e, 52, md1.toArray(SourceExpr[]::new)));
        tick.add(onGvalCtl(e, 53, set(e, p(e, "modamt1"), shl(e, sub(e, e.v("v"), e.n(64)), e.n(1)))));
        var md2 = new TList<SourceExpr>();
        md2.add(set(e, ctl(e, 52), e.v("v")));
        md2.add(set(e, p(e, "moddest2"), e.v("v")));
        md2.addAll(resend2);
        tick.add(onGval(e, 54, md2.toArray(SourceExpr[]::new)));
        tick.add(onGvalCtl(e, 55, set(e, p(e, "modamt2"), shl(e, sub(e, e.v("v"), e.n(64)), e.n(1)))));
        var wheel = new TList<SourceExpr>();
        wheel.add(set(e, ctl(e, 1), e.v("v")));
        wheel.add(set(e, p(e, "modwheel"), e.v("v")));
        wheel.add(set(e, e.v("mod"), e.n(1)));
        for (var md : MOD1) {
            wheel.add(ifOnly(
                    e,
                    eq(e, p(e, "moddest1"), e.n(md.dest())),
                    e.let(
                            "base",
                            e.mb.labeled(
                                    "mb" + md.dest(),
                                    e.seq(
                                            ifOnly(
                                                    e,
                                                    eq(e, gval(e, md.param()), e.n(-1)),
                                                    e.mb.return_("mb" + md.dest(), ctl(e, md.param()))),
                                            set(e, ctl(e, md.param()), gval(e, md.param())),
                                            e.mb.return_("mb" + md.dest(), gval(e, md.param()))))),
                    set(
                            e,
                            gval(e, md.writeParam()),
                            clampi(
                                    e,
                                    add(
                                            e,
                                            e.v("base"),
                                            e.mb.operatorShr(mul(e, p(e, "modamt1"), p(e, "modwheel")), e.n(7))),
                                    md.min(),
                                    md.max()))));
        }
        for (var md : MOD2) {
            wheel.add(ifOnly(
                    e,
                    eq(e, p(e, "moddest2"), e.n(md.dest())),
                    e.let(
                            "base",
                            e.mb.labeled(
                                    "mc" + md.dest(),
                                    e.seq(
                                            ifOnly(
                                                    e,
                                                    eq(e, gval(e, md.param()), e.n(-1)),
                                                    e.mb.return_("mc" + md.dest(), ctl(e, md.param()))),
                                            set(e, ctl(e, md.param()), gval(e, md.param())),
                                            e.mb.return_("mc" + md.dest(), gval(e, md.param()))))),
                    set(
                            e,
                            gval(e, md.writeParam()),
                            clampi(
                                    e,
                                    add(
                                            e,
                                            e.v("base"),
                                            e.mb.operatorShr(mul(e, p(e, "modamt2"), p(e, "modwheel")), e.n(7))),
                                    md.min(),
                                    md.max()))));
        }
        tick.add(onGval(e, 1, wheel.toArray(SourceExpr[]::new)));
        var ftypes = new TList<SourceExpr>();
        ftypes.add(set(e, p(e, "phat"), e.n(0)));
        ftypes.add(set(e, p(e, "peak"), e.n(0)));
        ftypes.add(set(e, p(e, "db18"), e.n(0)));
        ftypes.add(set(e, p(e, "db24"), e.n(0)));
        ftypes.add(set(e, p(e, "ftype_base"), e.n(0)));
        ftypes.add(ifOnly(e, eq(e, e.v("v"), e.n(0)), set(e, p(e, "db24"), e.n(1))));
        ftypes.add(ifOnly(e, eq(e, e.v("v"), e.n(1)), set(e, p(e, "db24"), e.n(1)), set(e, p(e, "db18"), e.n(1))));
        ftypes.add(ifOnly(
                e,
                and(e, ge(e, e.v("v"), e.n(2)), le(e, e.v("v"), e.n(5))),
                set(e, p(e, "ftype_base"), mul(e, sub(e, e.v("v"), e.n(2)), e.n(128 * 128 * 8)))));
        ftypes.add(ifOnly(
                e,
                eq(e, e.v("v"), e.n(6)),
                set(e, p(e, "db24"), e.n(1)),
                set(e, p(e, "db18"), e.n(1)),
                set(e, p(e, "peak"), e.n(1)),
                set(e, p(e, "ftype_base"), e.n(2 * 128 * 128 * 8))));
        ftypes.add(ifOnly(
                e,
                eq(e, e.v("v"), e.n(7)),
                set(e, p(e, "db24"), e.n(1)),
                set(e, p(e, "db18"), e.n(1)),
                set(e, p(e, "ftype_base"), e.n(2 * 128 * 128 * 8))));
        ftypes.add(ifOnly(
                e,
                eq(e, e.v("v"), e.n(8)),
                set(e, p(e, "db24"), e.n(1)),
                set(e, p(e, "db18"), e.n(1)),
                set(e, p(e, "ftype_base"), e.n(128 * 128 * 8))));
        ftypes.add(ifOnly(e, eq(e, e.v("v"), e.n(9)), set(e, p(e, "phat"), e.n(1))));
        ftypes.add(ifOnly(e, eq(e, e.v("v"), e.n(10)), set(e, p(e, "phat"), e.n(2))));
        ftypes.add(ifOnly(e, eq(e, e.v("v"), e.n(11)), set(e, p(e, "phat"), e.n(1)), set(e, p(e, "db24"), e.n(1))));
        ftypes.add(ifOnly(e, eq(e, e.v("v"), e.n(12)), set(e, p(e, "phat"), e.n(2)), set(e, p(e, "db24"), e.n(1))));
        ftypes.add(ifOnly(e, eq(e, e.v("v"), e.n(13)), set(e, p(e, "phat"), e.n(3)), set(e, p(e, "db24"), e.n(1))));
        tick.add(onGvalCtl(e, 30, ftypes.toArray(SourceExpr[]::new)));
        tick.add(onGvalCtl(e, 31, set(e, p(e, "dist"), e.v("v"))));
        tick.add(onGvalMod(e, 56, "moddest2", 4, set(e, p(e, "ampgain"), e.v("v"))));
        tick.add(onGvalMod(
                e,
                32,
                "moddest1",
                2,
                set(e, p(e, "cutoff_target"), f32(e, e.v("v"))),
                e.let(
                        "ca",
                        div(
                                e,
                                sub(e, p(e, "cutoff_target"), p(e, "cutoff")),
                                mul(e, f32(e, p(e, "spt")), e.n(ATTR_INERTIA * 0.5)))),
                set(
                        e,
                        p(e, "cutoff_add"),
                        e.ifElse(
                                gt(e, e.v("ca"), e.n(40)),
                                f32(e, e.n(40)),
                                e.ifElse(lt(e, e.v("ca"), e.n(-40)), f32(e, e.n(-40)), e.v("ca"))))));
        tick.add(onGvalMod(e, 33, "moddest2", 2, set(e, p(e, "resonance"), e.v("v"))));
        tick.add(onGvalMod(e, 34, "moddest2", 8, set(e, p(e, "feg_att"), envSamples(e, e.v("v")))));
        tick.add(onGvalMod(e, 35, "moddest2", 9, set(e, p(e, "feg_dec"), envSamples(e, e.v("v")))));
        tick.add(onGvalCtl(e, 36, set(e, p(e, "feg_sus"), sustainSamples(e, e.v("v")))));
        tick.add(onGvalMod(
                e,
                37,
                "moddest2",
                11,
                set(e, p(e, "feg_susfrac"), mul(e, f32(e, e.v("v")), e.n(1.0 / 127))),
                set(e, p(e, "feg_level"), mul(e, p(e, "feg_susfrac"), f32(e, p(e, "fenvmod"))))));
        tick.add(onGvalMod(e, 38, "moddest2", 10, set(e, p(e, "feg_rel"), envSamples(e, e.v("v")))));
        tick.add(onGvalMod(
                e,
                39,
                "moddest1",
                1,
                set(e, p(e, "fenvmod"), shl(e, sub(e, e.v("v"), e.n(64)), e.n(1))),
                set(e, p(e, "feg_level"), mul(e, p(e, "feg_susfrac"), f32(e, p(e, "fenvmod"))))));
        tick.add(onGvalCtl(e, 19, set(e, p(e, "ueg_att"), envSamples(e, e.v("v")))));
        tick.add(onGvalCtl(e, 20, set(e, p(e, "ueg_dec"), envSamples(e, e.v("v")))));
        tick.add(onGvalCtl(e, 21, set(e, p(e, "ueg_sus"), sustainSamples(e, e.v("v")))));
        tick.add(onGvalCtl(
                e,
                22,
                set(e, p(e, "ueg_susfrac"), mul(e, f32(e, e.v("v")), e.n(1.0 / 127))),
                set(e, p(e, "ueg_level"), f2i(e, mul(e, f32(e, p(e, "uenvmod")), p(e, "ueg_susfrac"))))));
        tick.add(onGvalCtl(e, 23, set(e, p(e, "ueg_rel"), envSamples(e, e.v("v")))));
        tick.add(onGvalMod(
                e,
                24,
                "moddest2",
                1,
                set(e, p(e, "uenvmod"), shl(e, sub(e, e.v("v"), e.n(64)), e.n(20))),
                set(e, p(e, "ueg_level"), f2i(e, mul(e, f32(e, p(e, "uenvmod")), p(e, "ueg_susfrac"))))));
        tick.add(onGvalCtl(e, 50, set(e, p(e, "uegdest"), e.v("v"))));
        tick.add(onGvalCtl(e, 3, set(e, p(e, "bendamt"), e.v("v"))));
        var bendTargetD =
                call(e, "pow", f64(e, e.n(NOTECONST)), f64(e, mul(e, f32(e, p(e, "bendamt")), p(e, "pitchmod"))));
        var bendTarget = f32(e, bendTargetD);
        tick.add(onGvalCtl(
                e,
                2,
                set(e, p(e, "pitchmod"), mul(e, f32(e, sub(e, e.v("v"), e.n(64))), e.n(1.0 / 64))),
                e.ifElse(
                        ne(e, p(e, "glidetime"), e.n(0)),
                        e.seq(
                                set(e, p(e, "bendtime"), div(e, p(e, "glidetime"), e.n(32))),
                                set(
                                        e,
                                        p(e, "bendglide"),
                                        f32(
                                                e,
                                                call(
                                                        e,
                                                        "pow",
                                                        div(e, bendTargetD, f64(e, p(e, "bendfactor"))),
                                                        div(e, f64(e, e.n(32)), f64(e, p(e, "glidetime"))))))),
                        e.seq(
                                set(e, p(e, "bendtime"), e.n(0)),
                                set(
                                        e,
                                        p(e, "bendfactor"),
                                        e.ifElse(ne(e, p(e, "pitchmod"), e.n(0)), bendTarget, f32(e, e.n(1))))))));
        tick.add(onGvalMod(
                e,
                16,
                "moddest1",
                3,
                set(e, p(e, "bal1"), sub(e, EcoBuzz.i32(e, e.n(127)), e.v("v"))),
                set(e, p(e, "bal2"), e.v("v"))));
        tick.add(onGvalCtl(
                e,
                4,
                e.ifElse(
                        eq(e, e.v("v"), e.n(0)),
                        e.seq(
                                set(e, p(e, "glide"), e.n(0)),
                                forRange(e, "t", e.n(TRACKS), set(e, pt(e, "t_glideactive"), e.n(0)))),
                        e.seq(
                                set(e, p(e, "glide"), e.n(1)),
                                set(
                                        e,
                                        p(e, "glidetime"),
                                        i32(e, div(e, mul(e, f32(e, e.v("v")), e.n(10000000)), e.n(RATE))))))));
        tick.add(onGvalCtl(e, 5, set(e, p(e, "wavetabw"), e.v("v"))));
        tick.add(onGvalCtl(e, 7, set(e, p(e, "wavesemi"), sub(e, e.v("v"), e.n(64)))));
        tick.add(onGvalCtl(e, 6, set(e, p(e, "wavefixed"), e.v("v"))));
        Function<String[], SourceExpr> waveSelect = names -> e.ifElse(
                le(e, e.v("v"), e.n(5)),
                e.seq(
                        set(e, p(e, names[0]), e.v("v")),
                        set(e, p(e, names[1]), e.n(-1)),
                        set(e, p(e, names[2]), e.ifElse(eq(e, e.v("v"), e.n(0)), EcoBuzz.i32(e, e.n(-1)), e.v("v")))),
                e.seq(set(e, p(e, names[1]), sub(e, e.v("v"), e.n(6))), set(e, p(e, names[2]), e.n(-1))));
        tick.add(onGvalCtl(e, 17, waveSelect.apply(new String[] {"w3_host", "w3_custom", "oscwave3"})));
        tick.add(onGvalCtl(e, 18, set(e, p(e, "subvol"), e.v("v"))));
        tick.add(onGvalMod(e, 9, "moddest1", 7, set(e, p(e, "center1"), mul(e, f32(e, e.v("v")), e.n(1.0 / 127)))));
        tick.add(onGvalMod(e, 11, "moddest2", 7, set(e, p(e, "center2"), mul(e, f32(e, e.v("v")), e.n(1.0 / 127)))));
        tick.add(onGvalMod(
                e,
                12,
                "moddest1",
                8,
                set(
                        e,
                        p(e, "detune_semi"),
                        f32(e, call(e, "pow", f64(e, e.n(1.05946309435929526)), f64(e, sub(e, e.v("v"), e.n(64))))))));
        tick.add(onGvalMod(
                e,
                13,
                "moddest1",
                4,
                set(
                        e,
                        p(e, "detune_fine"),
                        f32(e, call(e, "pow", f64(e, e.n(1.00091728179580156)), f64(e, sub(e, e.v("v"), e.n(64))))))));
        tick.add(onGvalCtl(e, 14, set(e, p(e, "sync"), e.v("v"))));
        tick.add(onGvalMod(e, 51, "moddest2", 3, set(e, p(e, "phdiff2"), shl(e, e.v("v"), e.n(20)))));
        tick.add(onGvalCtl(e, 0, set(e, p(e, "playmode"), e.v("v"))));
        tick.add(onGvalCtl(e, 15, set(e, p(e, "mixtype"), e.v("v"))));
        Function<String[], SourceExpr> oscSelect = names -> e.ifElse(
                eq(e, e.v("v"), e.n(NUMWAVES + 5)),
                e.seq(set(e, p(e, names[3]), e.n(1)), set(e, p(e, names[2]), e.n(-1))),
                e.ifElse(
                        eq(e, e.v("v"), e.n(NUMWAVES + 6)),
                        e.seq(set(e, p(e, names[3]), e.n(2)), set(e, p(e, names[2]), e.n(-1))),
                        e.seq(set(e, p(e, names[3]), e.n(0)), waveSelect.apply(names))));
        tick.add(onGvalCtl(e, 8, oscSelect.apply(new String[] {"w1_host", "w1_custom", "oscwave1", "noise1"})));
        tick.add(onGvalCtl(e, 10, oscSelect.apply(new String[] {"w2_host", "w2_custom", "oscwave2", "noise2"})));
        tick.add(onGvalCtl(e, 25, set(e, p(e, "aeg_att"), envSamples(e, e.v("v")))));
        tick.add(onGvalCtl(e, 26, set(e, p(e, "aeg_dec"), envSamples(e, e.v("v")))));
        tick.add(onGvalCtl(e, 27, set(e, p(e, "aeg_sus"), sustainSamples(e, e.v("v")))));
        tick.add(onGvalCtl(e, 28, set(e, p(e, "aeg_susfrac"), mul(e, f32(e, e.v("v")), e.n(1.0 / 127)))));
        tick.add(onGvalCtl(e, 29, set(e, p(e, "aeg_rel"), envSamples(e, e.v("v")))));
        var destTab1 = new TList<SourceExpr>();
        for (var d : LFO1_DESTS) destTab1.add(e.intOf(d));
        var destTab2 = new TList<SourceExpr>();
        for (var d : LFO2_DESTS) destTab2.add(e.intOf(d));
        var l1flags = new TList<SourceExpr>();
        for (int b = 0; b < L1_FLAGS.size(); b++) {
            l1flags.add(set(
                    e,
                    p(e, L1_FLAGS.get(b)),
                    and(e, e.mb.operatorShr(idx(e, lw(e, "lfo1_dests"), e.v("v")), e.n(b)), e.n(1))));
        }
        tick.add(onGvalCtl(e, 40, l1flags.toArray(SourceExpr[]::new)));
        Function<String[], SourceExpr> lfoWave = names -> e.seq(
                set(
                        e,
                        p(e, names[0]),
                        e.ifElse(
                                le(e, e.v("v"), e.n(4)),
                                e.v("v"),
                                e.ifElse(
                                        eq(e, e.v("v"), e.n(5)),
                                        EcoBuzz.i32(e, e.n(100 + WAVE_STEPUP)),
                                        e.ifElse(
                                                eq(e, e.v("v"), e.n(6)),
                                                EcoBuzz.i32(e, e.n(100 + WAVE_STEPDN)),
                                                e.ifElse(
                                                        eq(e, e.v("v"), e.n(7)),
                                                        EcoBuzz.i32(e, e.n(100 + WAVE_WACKY1)),
                                                        EcoBuzz.i32(e, e.n(100 + WAVE_WACKY2))))))),
                set(
                        e,
                        p(e, names[1]),
                        e.ifElse(eq(e, e.v("v"), e.n(4)), EcoBuzz.i32(e, e.n(1)), EcoBuzz.i32(e, e.n(0)))));
        tick.add(onGvalCtl(e, 41, lfoWave.apply(new String[] {"lfo1tab", "lfo1noise"})));
        tick.add(onGvalMod(
                e,
                42,
                "moddest1",
                6,
                e.ifElse(
                        and(e, gt(e, e.v("v"), e.n(116)), le(e, e.v("v"), e.n(127))),
                        e.seq(
                                set(e, p(e, "lfo1synced"), e.n(1)),
                                set(e, p(e, "l_1lock2"), e.n(0)),
                                set(e, p(e, "lfo1freq"), sub(e, e.v("v"), e.n(117)))),
                        e.ifElse(
                                eq(e, e.v("v"), e.n(128)),
                                e.seq(
                                        set(e, p(e, "lfo1synced"), e.n(0)),
                                        set(e, p(e, "l_1lock2"), e.n(1)),
                                        set(e, p(e, "lfo1freq"), e.n(0))),
                                e.seq(
                                        set(e, p(e, "lfo1synced"), e.n(0)),
                                        set(e, p(e, "l_1lock2"), e.n(0)),
                                        set(e, p(e, "lfo1freq"), e.v("v")))))));
        tick.add(onGvalMod(e, 43, "moddest1", 5, set(e, p(e, "lfo1amount"), e.v("v"))));
        tick.add(set(e, p(e, "padd_lfo1"), lfoPhaseAdd(e, "lfo1synced", "lfo1noise", "lfo1freq")));
        var l2flags = new TList<SourceExpr>();
        for (int b = 0; b < L2_FLAGS.size(); b++) {
            l2flags.add(set(
                    e,
                    p(e, L2_FLAGS.get(b)),
                    and(e, e.mb.operatorShr(idx(e, lw(e, "lfo2_dests"), e.v("v")), e.n(b)), e.n(1))));
        }
        l2flags.add(set(e, p(e, "l_2lock1"), e.n(0)));
        l2flags.add(set(e, p(e, "l_2lock2"), e.n(0)));
        tick.add(onGvalCtl(e, 45, l2flags.toArray(SourceExpr[]::new)));
        tick.add(onGvalCtl(e, 46, lfoWave.apply(new String[] {"lfo2tab", "lfo2noise"})));
        tick.add(onGvalMod(
                e,
                47,
                "moddest2",
                6,
                set(e, p(e, "l_2lock1"), e.n(0)),
                set(e, p(e, "l_2lock2"), e.n(0)),
                set(e, p(e, "lfo2synced"), e.n(0)),
                set(e, p(e, "lfo2freq"), e.n(0)),
                e.ifElse(
                        and(e, gt(e, e.v("v"), e.n(116)), le(e, e.v("v"), e.n(127))),
                        e.seq(set(e, p(e, "lfo2synced"), e.n(1)), set(e, p(e, "lfo2freq"), sub(e, e.v("v"), e.n(117)))),
                        e.ifElse(
                                eq(e, e.v("v"), e.n(128)),
                                set(e, p(e, "l_2lock2"), e.n(1)),
                                e.ifElse(
                                        eq(e, e.v("v"), e.n(129)),
                                        set(e, p(e, "l_2lock1"), e.n(1)),
                                        set(e, p(e, "lfo2freq"), e.v("v")))))));
        tick.add(onGvalMod(e, 48, "moddest2", 5, set(e, p(e, "lfo2amount"), e.v("v"))));
        tick.add(onGvalCtl(e, 44, set(e, p(e, "lfo1phdiff"), shl(e, e.v("v"), e.n(25)))));
        tick.add(onGvalCtl(e, 49, set(e, p(e, "lfo2phdiff"), shl(e, e.v("v"), e.n(25)))));
        tick.add(set(e, p(e, "padd_lfo2"), lfoPhaseAdd(e, "lfo2synced", "lfo2noise", "lfo2freq")));
        var trackTick = new TList<SourceExpr>();
        trackTick.add(e.let("tvol", idx(e, idx(e, p(e, "tval"), t), e.n(1))));
        trackTick.add(ifOnly(e, ne(e, e.v("tvol"), e.n(-1)), set(e, pt(e, "t_volume"), shl(e, e.v("tvol"), e.n(20)))));
        trackTick.add(e.let("tnote", idx(e, idx(e, p(e, "tval"), t), e.n(0))));
        trackTick.add(ifOnly(
                e,
                ne(e, e.v("tnote"), e.n(-1)),
                e.ifElse(
                        and(e, ge(e, e.v("tnote"), e.n(1)), le(e, e.v("tnote"), e.n(156))),
                        e.seq(set(e, pt(e, "t_note"), e.v("tnote")), call(e, "noteon", e.v("p"), t)),
                        ifOnly(e, eq(e, e.v("tnote"), e.n(EcoBuzz.NOTE_OFF)), call(e, "noteoff", e.v("p"), t)))));
        trackTick.add(e.let("n", p(e, "lfo1amount")));
        trackTick.addAll(lfoAmts1);
        var amts2Tick = new TList<SourceExpr>();
        amts2Tick.add(e.let("n", p(e, "lfo2amount")));
        amts2Tick.addAll(lfoAmts2);
        trackTick.add(e.seq(amts2Tick.toArray(SourceExpr[]::new)));
        tick.add(forRange(e, "t", e.n(TRACKS), trackTick.toArray(SourceExpr[]::new)));
        tick.add(resetVals(e));
        body.add(e.let(
                "lfo1_dests",
                e.call(
                        EcoBuzz.llvmL(e, "global_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("mw_lfo1_dests")),
                                e.p("ty", e.lowerV("dests_t")),
                                e.p("init", e.listNew(e.mb.typesInt(), destTab1.toArray(SourceExpr[]::new)))))));
        body.add(e.let(
                "lfo2_dests",
                e.call(
                        EcoBuzz.llvmL(e, "global_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("mw_lfo2_dests")),
                                e.p("ty", e.lowerV("dests_t")),
                                e.p("init", e.listNew(e.mb.typesInt(), destTab2.toArray(SourceExpr[]::new)))))));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "mw_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        e.seq(tick.toArray(SourceExpr[]::new)))));

        var work = e.seq(
                call(e, "zero", e.v("out"), e.v("n")),
                e.let("oldcut", p(e, "cutoff")),
                e.let("got", EcoBuzz.i32(e, e.n(0))),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        ifOnly(
                                e,
                                ne(e, pt(e, "t_aegstate"), e.n(0)),
                                set(e, pt(e, "t_phlfo1"), add(e, p(e, "ph_lfo1"), mul(e, t, p(e, "lfo1phdiff")))),
                                set(e, pt(e, "t_phlfo2"), add(e, p(e, "ph_lfo2"), mul(e, t, p(e, "lfo2phdiff")))),
                                set(e, p(e, "cutoff"), e.v("oldcut")),
                                call(e, "trackwork", e.v("p"), t, e.v("out"), e.v("n")),
                                set(e, e.v("got"), e.n(1)))),
                ifOnly(
                        e,
                        ne(e, p(e, "bendtime"), e.n(0)),
                        set(e, p(e, "bendfactor"), mul(e, p(e, "bendfactor"), p(e, "bendglide"))),
                        set(e, p(e, "bendtime"), sub(e, p(e, "bendtime"), e.n(1)))),
                set(e, p(e, "ph_lfo1"), add(e, p(e, "ph_lfo1"), mul(e, p(e, "padd_lfo1"), e.v("n")))),
                set(e, p(e, "ph_lfo2"), add(e, p(e, "ph_lfo2"), mul(e, p(e, "padd_lfo2"), e.v("n")))),
                ifOnly(
                        e,
                        eq(e, e.v("got"), e.n(0)),
                        set(e, p(e, "cutoff"), add(e, p(e, "cutoff"), mul(e, p(e, "cutoff_add"), f32(e, e.v("n"))))),
                        ifOnly(
                                e,
                                or(
                                        e,
                                        and(
                                                e,
                                                gt(e, p(e, "cutoff_add"), e.n(0)),
                                                gt(e, p(e, "cutoff"), p(e, "cutoff_target"))),
                                        and(
                                                e,
                                                lt(e, p(e, "cutoff_add"), e.n(0)),
                                                lt(e, p(e, "cutoff"), p(e, "cutoff_target")))),
                                set(e, p(e, "cutoff"), p(e, "cutoff_target")))));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(e, e.v("ll"), "mw_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), work)));
        return EcoBuzz.machineModule(
                e,
                "m4wii",
                defs,
                new String[] {"init", "tick", "work"},
                TList.of(partTables, partVoice),
                main,
                new SourceExpr[] {e.v("init"), e.v("tick"), e.v("work")});
    }
}
