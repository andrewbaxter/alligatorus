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
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.ne;
import static compilerxyz.main.tests.EcoBuzz.or;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

final class EcoBuzzSampleGrid {
    private EcoBuzzSampleGrid() {}

    static final int TRACKS = 16;
    static final int NG = 29;
    static final int NT = 9;
    static final int RAMP = 50;
    private static final int QUEUE = 16;
    private static final int SOLO_OFF = 16;
    private static final int CUT_NONE = 0xFFFFFFF;
    private static final int TRIG_VEL = 0;
    private static final int TRIG_PAN = 1;
    private static final int TRIG_TUNE = 2;
    private static final int TRIG_PROB = 3;
    private static final int TRIG_ARG = 4;
    private static final int CMD_DELAY = 1;
    private static final int CMD_RETRIG = 2;
    private static final int CMD_OFFSET = 3;
    private static final int CMD_REVERSE = 4;
    private static final int CMD_PROB = 5;
    private static final int CMD_CUT = 6;
    private static final int CMD_DELAY_CUT = 7;
    private static final double SHUFFLE_UNIT = 512.0;
    private static final int LCG_MUL = 1103515245;
    private static final int LCG_ADD = 12345;
    private static final double LCG_SCALE = 1.0 / 0x7fffffff;

    private static final IList<String> I32_FIELDS = IList.of(
            "first_wave",
            "trig_type",
            "solo",
            "inertia_len",
            "shuffle_on",
            "shuffle_step",
            "shuffle_count",
            "shuffle_now",
            "initialising",
            "rnd",
            "gvol_cnt",
            "gpan_cnt",
            "gtune_cnt");
    private static final IList<String> F32_FIELDS = IList.of(
            "gvol_cur",
            "gvol_tgt",
            "gvol_step",
            "gpan_cur",
            "gpan_tgt",
            "gpan_step",
            "gtune_cur",
            "gtune_tgt",
            "gtune_step",
            "shuffle_size",
            "shuffle_rnd");
    private static final IList<String> I32_TRACK_FIELDS = IList.of(
            "tw",
            "tmute",
            "tgroup",
            "tsubdiv",
            "tvol_cnt",
            "tpan_cnt",
            "ttune_cnt",
            "vact",
            "vw",
            "vage",
            "vcut",
            "vrampcnt",
            "fact",
            "fw",
            "fage",
            "fcut",
            "frampcnt",
            "q_count");
    private static final IList<String> F32_TRACK_FIELDS = IList.of(
            "tvol_cur",
            "tvol_tgt",
            "tvol_step",
            "tpan_cur",
            "tpan_tgt",
            "tpan_step",
            "ttune_cur",
            "ttune_tgt",
            "ttune_step",
            "vpos",
            "vrate",
            "vvel",
            "vpan",
            "vrampvol",
            "vrampstep",
            "fpos",
            "frate",
            "fvel",
            "fpan",
            "frampvol",
            "frampstep");
    private static final IList<String> I32_QUEUE_FIELDS = IList.of("q_delay", "q_cut");
    private static final IList<String> F32_QUEUE_FIELDS = IList.of("q_vel", "q_pan", "q_rate", "q_off");

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pt(EcoMb e, String field, SourceExpr t) {
        return idx(e, fld(e, e.v("p"), field), t);
    }

    private static SourceExpr q(EcoMb e, String field, SourceExpr t, SourceExpr k) {
        return idx(e, fld(e, e.v("p"), field), add(e, mul(e, t, e.n(QUEUE)), k));
    }

    private static SourceExpr gval(EcoMb e, SourceExpr k) {
        return idx(e, p(e, "gval"), k);
    }

    private static SourceExpr tval(EcoMb e, SourceExpr t, int k) {
        return idx(e, idx(e, p(e, "tval"), t), e.n(k));
    }

    private static SourceExpr wave(EcoMb e, SourceExpr wi, String field) {
        return fld(e, idx(e, fld(e, p(e, "waves"), "w"), wi), field);
    }

    private static SourceExpr resetVals(EcoMb e) {
        return e.seq(
                forRange(e, "k", e.n(NG), set(e, gval(e, e.v("k")), e.n(EcoBuzz.NO_VALUE))),
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

    private static SourceExpr inertiaSet(EcoMb e, Function<String, SourceExpr> at, SourceExpr value) {
        return e.seq(
                e.let("iv", value),
                set(e, at.apply("_tgt"), e.v("iv")),
                e.ifElse(
                        or(e, ne(e, p(e, "initialising"), e.n(0)), eq(e, p(e, "inertia_len"), e.n(0))),
                        e.seq(set(e, at.apply("_cur"), e.v("iv")), set(e, at.apply("_cnt"), e.n(0))),
                        e.seq(
                                set(
                                        e,
                                        at.apply("_step"),
                                        div(e, sub(e, e.v("iv"), at.apply("_cur")), f32(e, p(e, "inertia_len")))),
                                set(e, at.apply("_cnt"), p(e, "inertia_len")))));
    }

    private static SourceExpr inertiaUpdate(EcoMb e, Function<String, SourceExpr> at) {
        return ifOnly(
                e,
                gt(e, at.apply("_cnt"), e.n(0)),
                set(e, at.apply("_cur"), add(e, at.apply("_cur"), mul(e, at.apply("_step"), f32(e, e.v("n"))))),
                set(e, at.apply("_cnt"), sub(e, at.apply("_cnt"), e.v("n"))),
                ifOnly(
                        e,
                        le(e, at.apply("_cnt"), e.n(0)),
                        set(e, at.apply("_cur"), at.apply("_tgt")),
                        set(e, at.apply("_cnt"), e.n(0))));
    }

    private static Function<String, SourceExpr> global(EcoMb e, String name) {
        return s -> p(e, name + s);
    }

    private static Function<String, SourceExpr> track(EcoMb e, String name, SourceExpr t) {
        return s -> pt(e, name + s, t);
    }

    private static SourceExpr frandStep(EcoMb e) {
        return set(e, p(e, "rnd"), and(e, add(e, mul(e, p(e, "rnd"), e.n(LCG_MUL)), e.n(LCG_ADD)), e.n(0x7fffffff)));
    }

    private static SourceExpr frandValue(EcoMb e) {
        return mul(e, f32(e, p(e, "rnd")), e.n(LCG_SCALE));
    }

    private static SourceExpr cmdLen(EcoMb e, SourceExpr t, SourceExpr n) {
        return div(e, mul(e, n, p(e, "spt")), pt(e, "tsubdiv", t));
    }

    private static SourceExpr qCopy(EcoMb e, SourceExpr t, SourceExpr dst, SourceExpr src) {
        var out = new TList<SourceExpr>();
        for (var f : I32_QUEUE_FIELDS) out.add(set(e, q(e, f, t, dst), q(e, f, t, src)));
        for (var f : F32_QUEUE_FIELDS) out.add(set(e, q(e, f, t, dst), q(e, f, t, src)));
        return e.seq(out.toArray(SourceExpr[]::new));
    }

    private static SourceExpr push(EcoMb e, SourceExpr t) {
        var k = e.v("qk");
        var m = e.v("qm");
        return ifOnly(
                e,
                lt(e, pt(e, "q_count", t), e.n(QUEUE)),
                e.let("qk", pt(e, "q_count", t)),
                set(e, q(e, "q_delay", t, k), e.v("delay")),
                set(e, q(e, "q_cut", t, k), e.v("cut")),
                set(e, q(e, "q_vel", t, k), e.v("vel")),
                set(e, q(e, "q_pan", t, k), e.v("pan")),
                set(e, q(e, "q_rate", t, k), e.v("rate")),
                set(e, q(e, "q_off", t, k), e.v("off")),
                set(e, pt(e, "q_count", t), add(e, k, e.n(1))),
                forRange(
                        e,
                        "qj",
                        k,
                        e.let("qm", sub(e, k, e.v("qj"))),
                        ifOnly(
                                e,
                                gt(e, q(e, "q_delay", t, sub(e, m, e.n(1))), q(e, "q_delay", t, m)),
                                e.let("sd", q(e, "q_delay", t, m)),
                                e.let("sc", q(e, "q_cut", t, m)),
                                e.let("sv", q(e, "q_vel", t, m)),
                                e.let("sp", q(e, "q_pan", t, m)),
                                e.let("sr", q(e, "q_rate", t, m)),
                                e.let("so", q(e, "q_off", t, m)),
                                qCopy(e, t, m, sub(e, m, e.n(1))),
                                set(e, q(e, "q_delay", t, sub(e, m, e.n(1))), e.v("sd")),
                                set(e, q(e, "q_cut", t, sub(e, m, e.n(1))), e.v("sc")),
                                set(e, q(e, "q_vel", t, sub(e, m, e.n(1))), e.v("sv")),
                                set(e, q(e, "q_pan", t, sub(e, m, e.n(1))), e.v("sp")),
                                set(e, q(e, "q_rate", t, sub(e, m, e.n(1))), e.v("sr")),
                                set(e, q(e, "q_off", t, sub(e, m, e.n(1))), e.v("so")))));
    }

    private static SourceExpr voiceToFade(EcoMb e, SourceExpr t) {
        return ifOnly(
                e,
                ne(e, pt(e, "vact", t), e.n(0)),
                set(e, pt(e, "fact", t), e.n(1)),
                set(e, pt(e, "fw", t), pt(e, "vw", t)),
                set(e, pt(e, "fage", t), pt(e, "vage", t)),
                set(e, pt(e, "fcut", t), pt(e, "vcut", t)),
                set(e, pt(e, "fpos", t), pt(e, "vpos", t)),
                set(e, pt(e, "frate", t), pt(e, "vrate", t)),
                set(e, pt(e, "fvel", t), pt(e, "vvel", t)),
                set(e, pt(e, "fpan", t), pt(e, "vpan", t)),
                set(e, pt(e, "frampvol", t), e.n(1)),
                set(e, pt(e, "frampstep", t), e.n(-2.0 / RAMP)),
                set(e, pt(e, "frampcnt", t), e.n(RAMP / 2)),
                set(e, pt(e, "vact", t), e.n(0)));
    }

    private static SourceExpr voiceSample(
            EcoMb e, SourceExpr t, String prefix, String outL, String outR, boolean fade) {
        var act = pt(e, prefix + "act", t);
        var w = pt(e, prefix + "w", t);
        var age = pt(e, prefix + "age", t);
        var pos = pt(e, prefix + "pos", t);
        var rampcnt = pt(e, prefix + "rampcnt", t);
        var rampvol = pt(e, prefix + "rampvol", t);
        var step = mul(e, EcoBuzz.conv(e, "t_f64", pt(e, prefix + "rate", t)), e.v("tunemul"));
        var frames = f32(e, wave(e, w, "frames"));
        var ramp = ifOnly(
                e,
                gt(e, rampcnt, e.n(0)),
                set(e, e.v(outL), mul(e, e.v(outL), rampvol)),
                set(e, e.v(outR), mul(e, e.v(outR), rampvol)),
                set(e, rampvol, add(e, rampvol, pt(e, prefix + "rampstep", t))),
                set(e, rampcnt, sub(e, rampcnt, e.n(1))),
                fade ? ifOnly(e, eq(e, rampcnt, e.n(0)), set(e, act, e.n(0))) : e.seq());
        return ifOnly(
                e,
                ne(e, act, e.n(0)),
                e.ifElse(
                        lt(e, age, e.n(0)),
                        set(e, age, add(e, age, e.n(1))),
                        e.seq(
                                e.ifElse(
                                        and(
                                                e,
                                                and(e, lt(e, pos, frames), ge(e, pos, e.n(0))),
                                                le(e, age, pt(e, prefix + "cut", t))),
                                        e.seq(
                                                set(
                                                        e,
                                                        e.v(outL),
                                                        mul(
                                                                e,
                                                                call(e, "wave_hermite", p(e, "waves"), w, pos, e.n(0)),
                                                                wave(e, w, "vol"))),
                                                set(
                                                        e,
                                                        e.v(outR),
                                                        e.ifElse(
                                                                gt(e, wave(e, w, "channels"), e.n(1)),
                                                                mul(
                                                                        e,
                                                                        call(
                                                                                e,
                                                                                "wave_hermite",
                                                                                p(e, "waves"),
                                                                                w,
                                                                                pos,
                                                                                e.n(1)),
                                                                        wave(e, w, "vol")),
                                                                e.v(outL))),
                                                set(e, age, add(e, age, e.n(1))),
                                                set(e, pos, f32(e, add(e, EcoBuzz.conv(e, "t_f64", pos), step)))),
                                        set(e, act, e.n(0))),
                                ramp)));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("i32x16", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS))));
        defs.add(e.let("f32x16", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS))));
        defs.add(e.let("i32xq", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS * QUEUE))));
        defs.add(e.let("f32xq", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS * QUEUE))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "spt", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        for (var f : I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_i32")));
        for (var f : F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_f32")));
        for (var f : I32_TRACK_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32x16")));
        for (var f : F32_TRACK_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32x16")));
        for (var f : I32_QUEUE_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32xq")));
        for (var f : F32_QUEUE_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xq")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "sg_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("d2_fn_t", EcoBuzz.fnType(e, e.v("t_f64"), e.v("t_f64"), e.v("t_f64"))));

        var t = e.v("t");
        var init = new TList<SourceExpr>();
        for (var f : I32_FIELDS) init.add(set(e, p(e, f), e.n(0)));
        for (var f : F32_FIELDS) init.add(set(e, p(e, f), e.n(0)));
        init.add(set(e, p(e, "first_wave"), e.n(1)));
        init.add(set(e, p(e, "solo"), e.n(-1)));
        init.add(set(e, p(e, "shuffle_step"), e.n(2)));
        init.add(set(e, p(e, "initialising"), e.n(1)));
        init.add(set(e, p(e, "rnd"), e.n(0x16BA2118)));
        init.add(set(e, p(e, "gvol_cur"), e.n(1)));
        init.add(set(e, p(e, "gvol_tgt"), e.n(1)));
        init.add(set(e, p(e, "gpan_cur"), e.n(1)));
        init.add(set(e, p(e, "gpan_tgt"), e.n(1)));
        init.add(resetVals(e));
        var tInit = new TList<SourceExpr>();
        for (var f : I32_TRACK_FIELDS) tInit.add(set(e, pt(e, f, t), e.n(0)));
        for (var f : F32_TRACK_FIELDS) tInit.add(set(e, pt(e, f, t), e.n(0)));
        tInit.add(set(e, pt(e, "tsubdiv", t), e.n(6)));
        tInit.add(set(e, pt(e, "tvol_cur", t), e.n(1)));
        tInit.add(set(e, pt(e, "tvol_tgt", t), e.n(1)));
        tInit.add(set(e, pt(e, "tpan_cur", t), e.n(1)));
        tInit.add(set(e, pt(e, "tpan_tgt", t), e.n(1)));
        init.add(forRange(e, "t", e.n(TRACKS), tInit.toArray(SourceExpr[]::new)));
        var initBody = e.seq(init.toArray(SourceExpr[]::new));

        var trig = e.v("trig");
        var cmd = e.v("cmd");
        var arg = e.v("arg");
        var trigger = e.seq(
                e.let("vel", f32(e, e.n(1))),
                e.let("pan", f32(e, e.n(1))),
                e.let("rate", f32(e, e.n(1))),
                e.let("off", f32(e, e.n(0))),
                e.let("cut", EcoBuzz.i32(e, e.n(-1))),
                ifOnly(e, ne(e, p(e, "shuffle_now"), e.n(0)), frandStep(e)),
                e.let(
                        "delay",
                        e.ifElse(
                                ne(e, p(e, "shuffle_now"), e.n(0)),
                                i32(
                                        e,
                                        mul(
                                                e,
                                                mul(
                                                        e,
                                                        p(e, "shuffle_size"),
                                                        add(
                                                                e,
                                                                f32(e, e.n(1)),
                                                                mul(
                                                                        e,
                                                                        mul(e, sub(e, frandValue(e), e.n(0.5)), e.n(2)),
                                                                        p(e, "shuffle_rnd")))),
                                                f32(e, p(e, "spt")))),
                                EcoBuzz.i32(e, e.n(0)))),
                e.let("arg", e.ifElse(eq(e, tval(e, t, 2), e.n(-1)), EcoBuzz.i32(e, e.n(0)), tval(e, t, 2))),
                e.let("cmd", e.ifElse(eq(e, tval(e, t, 1), e.n(-1)), EcoBuzz.i32(e, e.n(0)), tval(e, t, 1))),
                e.let("skip", EcoBuzz.i32(e, e.n(0))),
                e.let("rint", EcoBuzz.i32(e, e.n(0))),
                e.let("rcount", EcoBuzz.i32(e, e.n(0))),
                ifOnly(
                        e,
                        eq(e, p(e, "trig_type"), e.n(TRIG_VEL)),
                        set(e, e.v("vel"), mul(e, f32(e, trig), e.n(1.0 / 128)))),
                ifOnly(
                        e,
                        eq(e, p(e, "trig_type"), e.n(TRIG_PAN)),
                        set(e, e.v("pan"), mul(e, f32(e, trig), e.n(1.0 / 128)))),
                ifOnly(
                        e,
                        eq(e, p(e, "trig_type"), e.n(TRIG_TUNE)),
                        set(
                                e,
                                e.v("rate"),
                                f32(
                                        e,
                                        call(
                                                e,
                                                "pow",
                                                EcoBuzz.conv(e, "t_f64", e.n(2)),
                                                div(e, EcoBuzz.conv(e, "t_f64", sub(e, trig, e.n(128))), e.n(48)))))),
                ifOnly(
                        e,
                        eq(e, p(e, "trig_type"), e.n(TRIG_PROB)),
                        frandStep(e),
                        ifOnly(e, gt(e, mul(e, frandValue(e), e.n(0xFE)), f32(e, trig)), set(e, e.v("skip"), e.n(1)))),
                ifOnly(e, eq(e, p(e, "trig_type"), e.n(TRIG_ARG)), set(e, arg, trig)),
                ifOnly(e, eq(e, cmd, e.n(CMD_DELAY)), set(e, e.v("delay"), add(e, e.v("delay"), cmdLen(e, t, arg)))),
                ifOnly(
                        e,
                        eq(e, cmd, e.n(CMD_RETRIG)),
                        e.let("ra", e.ifElse(eq(e, e.mb.operatorShr(arg, e.n(4)), e.n(0)), or(e, arg, e.n(0x10)), arg)),
                        e.let(
                                "rb",
                                e.ifElse(
                                        eq(e, and(e, e.v("ra"), e.n(0xF)), e.n(0)),
                                        or(e, e.v("ra"), e.n(1)),
                                        e.v("ra"))),
                        set(e, e.v("rint"), cmdLen(e, t, e.mb.operatorShr(e.v("rb"), e.n(4)))),
                        set(
                                e,
                                e.v("rcount"),
                                sub(e, div(e, mul(e, p(e, "spt"), and(e, e.v("rb"), e.n(0xF))), e.v("rint")), e.n(1)))),
                ifOnly(e, eq(e, cmd, e.n(CMD_OFFSET)), set(e, e.v("off"), mul(e, f32(e, arg), e.n(1.0 / 256)))),
                ifOnly(
                        e,
                        eq(e, cmd, e.n(CMD_REVERSE)),
                        set(e, e.v("off"), mul(e, f32(e, arg), e.n(1.0 / 256))),
                        set(e, e.v("rate"), mul(e, e.v("rate"), e.n(-1)))),
                ifOnly(
                        e,
                        eq(e, cmd, e.n(CMD_PROB)),
                        frandStep(e),
                        ifOnly(e, gt(e, mul(e, frandValue(e), e.n(0xFF)), f32(e, arg)), set(e, e.v("skip"), e.n(1)))),
                ifOnly(e, eq(e, cmd, e.n(CMD_CUT)), set(e, e.v("cut"), cmdLen(e, t, arg))),
                ifOnly(
                        e,
                        eq(e, cmd, e.n(CMD_DELAY_CUT)),
                        set(e, e.v("delay"), add(e, e.v("delay"), cmdLen(e, t, e.mb.operatorShr(arg, e.n(4))))),
                        set(e, e.v("cut"), cmdLen(e, t, and(e, arg, e.n(0xF))))),
                ifOnly(
                        e,
                        eq(e, e.v("skip"), e.n(0)),
                        push(e, t),
                        forRange(
                                e,
                                "rk",
                                e.v("rcount"),
                                set(e, e.v("delay"), add(e, e.v("delay"), e.v("rint"))),
                                push(e, t))));

        var tickTrack = e.seq(
                ifOnly(e, ne(e, tval(e, t, 0), e.n(-1)), set(e, pt(e, "tw", t), tval(e, t, 0))),
                ifOnly(e, ne(e, tval(e, t, 3), e.n(-1)), set(e, pt(e, "tsubdiv", t), tval(e, t, 3))),
                ifOnly(e, ne(e, tval(e, t, 4), e.n(-1)), set(e, pt(e, "tmute", t), tval(e, t, 4))),
                ifOnly(
                        e,
                        ne(e, tval(e, t, 5), e.n(-1)),
                        inertiaSet(e, track(e, "tvol", t), mul(e, f32(e, tval(e, t, 5)), e.n(1.0 / 128)))),
                ifOnly(
                        e,
                        ne(e, tval(e, t, 6), e.n(-1)),
                        inertiaSet(
                                e, track(e, "tpan", t), mul(e, f32(e, sub(e, tval(e, t, 6), e.n(1))), e.n(1.0 / 127)))),
                ifOnly(
                        e,
                        ne(e, tval(e, t, 7), e.n(-1)),
                        inertiaSet(
                                e,
                                track(e, "ttune", t),
                                mul(e, f32(e, sub(e, tval(e, t, 7), e.n(128))), e.n(1.0 / 48)))),
                ifOnly(e, ne(e, tval(e, t, 8), e.n(-1)), set(e, pt(e, "tgroup", t), tval(e, t, 8))),
                e.let("trig", gval(e, add(e, t, e.n(2)))),
                ifOnly(e, ne(e, trig, e.n(-1)), e.ifElse(eq(e, trig, e.n(0)), voiceToFade(e, t), trigger)));

        var tickBody = e.seq(
                ifOnly(e, ne(e, gval(e, e.n(0)), e.n(-1)), set(e, p(e, "first_wave"), gval(e, e.n(0)))),
                ifOnly(e, ne(e, gval(e, e.n(19)), e.n(-1)), set(e, p(e, "trig_type"), gval(e, e.n(19)))),
                ifOnly(
                        e,
                        ne(e, gval(e, e.n(20)), e.n(-1)),
                        set(
                                e,
                                p(e, "solo"),
                                e.ifElse(
                                        eq(e, gval(e, e.n(20)), e.n(SOLO_OFF)),
                                        EcoBuzz.i32(e, e.n(-1)),
                                        gval(e, e.n(20))))),
                ifOnly(
                        e,
                        ne(e, gval(e, e.n(28)), e.n(-1)),
                        set(e, p(e, "inertia_len"), div(e, mul(e, gval(e, e.n(28)), p(e, "spt")), e.n(16)))),
                ifOnly(
                        e,
                        ne(e, gval(e, e.n(21)), e.n(-1)),
                        inertiaSet(e, global(e, "gvol"), mul(e, f32(e, gval(e, e.n(21))), e.n(1.0 / 128)))),
                ifOnly(
                        e,
                        ne(e, gval(e, e.n(22)), e.n(-1)),
                        inertiaSet(
                                e,
                                global(e, "gpan"),
                                sub(
                                        e,
                                        f32(e, e.n(2)),
                                        mul(e, f32(e, sub(e, gval(e, e.n(22)), e.n(1))), e.n(1.0 / 127))))),
                ifOnly(
                        e,
                        ne(e, gval(e, e.n(23)), e.n(-1)),
                        inertiaSet(
                                e,
                                global(e, "gtune"),
                                mul(e, f32(e, sub(e, gval(e, e.n(23)), e.n(128))), e.n(1.0 / 48)))),
                ifOnly(
                        e,
                        ne(e, gval(e, e.n(24)), e.n(-1)),
                        e.ifElse(
                                eq(e, gval(e, e.n(24)), e.n(0)),
                                set(e, p(e, "shuffle_on"), e.n(0)),
                                e.seq(
                                        set(
                                                e,
                                                p(e, "shuffle_size"),
                                                mul(e, f32(e, gval(e, e.n(24))), e.n(1.0 / SHUFFLE_UNIT))),
                                        set(e, p(e, "shuffle_on"), e.n(1))))),
                ifOnly(e, ne(e, gval(e, e.n(25)), e.n(-1)), set(e, p(e, "shuffle_step"), gval(e, e.n(25)))),
                ifOnly(
                        e,
                        ne(e, gval(e, e.n(26)), e.n(-1)),
                        set(e, p(e, "shuffle_rnd"), mul(e, f32(e, gval(e, e.n(26))), e.n(1.0 / 254)))),
                ifOnly(e, eq(e, gval(e, e.n(27)), e.n(1)), set(e, p(e, "shuffle_count"), e.n(0))),
                set(e, p(e, "shuffle_now"), e.n(0)),
                ifOnly(
                        e,
                        ne(e, p(e, "shuffle_on"), e.n(0)),
                        set(e, p(e, "shuffle_count"), add(e, p(e, "shuffle_count"), e.n(1))),
                        ifOnly(
                                e,
                                ge(e, p(e, "shuffle_count"), p(e, "shuffle_step")),
                                set(e, p(e, "shuffle_now"), e.n(1)),
                                set(e, p(e, "shuffle_count"), e.n(0)))),
                forRange(e, "t", e.n(TRACKS), tickTrack),
                resetVals(e));

        var i = e.v("i");
        var pending = forRange(
                e,
                "k",
                e.n(QUEUE),
                ifOnly(
                        e,
                        lt(e, e.v("k"), pt(e, "q_count", t)),
                        set(e, q(e, "q_delay", t, e.v("k")), sub(e, q(e, "q_delay", t, e.v("k")), e.v("n")))));
        var due = and(
                e,
                gt(e, pt(e, "q_count", t), e.n(0)),
                le(e, q(e, "q_delay", t, EcoBuzz.i32(e, e.n(0))), add(e, i, e.n(1))));
        var fireEntry = EcoBuzz.i32(e, e.n(0));
        var wi = e.v("wi");
        var triggerOn = e.seq(
                voiceToFade(e, t),
                e.let(
                        "wi",
                        e.ifElse(
                                ne(e, pt(e, "tw", t), e.n(0)),
                                sub(e, pt(e, "tw", t), e.n(1)),
                                add(e, sub(e, p(e, "first_wave"), e.n(1)), t))),
                ifOnly(
                        e,
                        and(e, ge(e, wi, e.n(0)), lt(e, wi, e.n(EcoBuzzDsp.MAX_WAVES))),
                        set(e, pt(e, "vw", t), wi),
                        set(e, pt(e, "vpos", t), mul(e, f32(e, wave(e, wi, "frames")), q(e, "q_off", t, fireEntry))),
                        set(e, pt(e, "vage", t), e.n(0)),
                        set(e, pt(e, "vvel", t), q(e, "q_vel", t, fireEntry)),
                        set(e, pt(e, "vpan", t), q(e, "q_pan", t, fireEntry)),
                        set(e, pt(e, "vrate", t), q(e, "q_rate", t, fireEntry)),
                        set(
                                e,
                                pt(e, "vcut", t),
                                e.ifElse(
                                        lt(e, q(e, "q_cut", t, fireEntry), e.n(0)),
                                        EcoBuzz.i32(e, e.n(CUT_NONE)),
                                        q(e, "q_cut", t, fireEntry))),
                        set(e, pt(e, "vrampcnt", t), e.n(RAMP)),
                        set(e, pt(e, "vrampvol", t), e.n(0)),
                        set(e, pt(e, "vrampstep", t), e.n(1.0 / RAMP)),
                        set(e, pt(e, "vact", t), e.n(1))));
        var fire = ifOnly(
                e,
                due,
                forRange(
                        e,
                        "fk",
                        e.n(QUEUE),
                        ifOnly(
                                e,
                                due,
                                triggerOn,
                                forRange(
                                        e,
                                        "fj",
                                        e.n(QUEUE - 1),
                                        ifOnly(
                                                e,
                                                lt(e, add(e, e.v("fj"), e.n(1)), pt(e, "q_count", t)),
                                                qCopy(e, t, e.v("fj"), add(e, e.v("fj"), e.n(1))))),
                                set(e, pt(e, "q_count", t), sub(e, pt(e, "q_count", t), e.n(1))))));
        var tuneMul = mul(
                e,
                call(e, "pow", EcoBuzz.conv(e, "t_f64", e.n(2)), EcoBuzz.conv(e, "t_f64", pt(e, "ttune_cur", t))),
                call(e, "pow", EcoBuzz.conv(e, "t_f64", e.n(2)), EcoBuzz.conv(e, "t_f64", p(e, "gtune_cur"))));
        var workTrack = e.seq(
                inertiaUpdate(e, track(e, "tvol", t)),
                inertiaUpdate(e, track(e, "tpan", t)),
                inertiaUpdate(e, track(e, "ttune", t)),
                e.let("tunemul", tuneMul),
                e.let(
                        "audible",
                        e.ifElse(
                                eq(e, p(e, "solo"), e.n(-1)),
                                and(e, eq(e, pt(e, "tmute", t), e.n(0)), eq(e, pt(e, "tgroup", t), e.n(0))),
                                and(e, eq(e, t, p(e, "solo")), eq(e, pt(e, "tgroup", t), e.n(0))))),
                forRange(
                        e,
                        "i",
                        e.v("n"),
                        fire,
                        e.let("vl", f32(e, e.n(0))),
                        e.let("vr", f32(e, e.n(0))),
                        e.let("fl", f32(e, e.n(0))),
                        e.let("fr", f32(e, e.n(0))),
                        voiceSample(e, t, "v", "vl", "vr", false),
                        voiceSample(e, t, "f", "fl", "fr", true),
                        ifOnly(
                                e,
                                e.v("audible"),
                                e.let(
                                        "tl",
                                        add(
                                                e,
                                                mul(
                                                        e,
                                                        mul(e, e.v("vl"), pt(e, "vvel", t)),
                                                        sub(e, f32(e, e.n(2)), pt(e, "vpan", t))),
                                                mul(
                                                        e,
                                                        mul(e, e.v("fl"), pt(e, "fvel", t)),
                                                        sub(e, f32(e, e.n(2)), pt(e, "fpan", t))))),
                                e.let(
                                        "tr",
                                        add(
                                                e,
                                                mul(e, mul(e, e.v("vr"), pt(e, "vvel", t)), pt(e, "vpan", t)),
                                                mul(e, mul(e, e.v("fr"), pt(e, "fvel", t)), pt(e, "fpan", t)))),
                                set(
                                        e,
                                        idx(e, e.v("out"), mul(e, i, e.n(2))),
                                        add(
                                                e,
                                                idx(e, e.v("out"), mul(e, i, e.n(2))),
                                                mul(
                                                        e,
                                                        mul(
                                                                e,
                                                                mul(e, e.v("tl"), pt(e, "tvol_cur", t)),
                                                                sub(e, f32(e, e.n(2)), pt(e, "tpan_cur", t))),
                                                        mul(e, p(e, "gvol_cur"), p(e, "gpan_cur"))))),
                                set(
                                        e,
                                        idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                                        add(
                                                e,
                                                idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                                                mul(
                                                        e,
                                                        mul(
                                                                e,
                                                                mul(e, e.v("tr"), pt(e, "tvol_cur", t)),
                                                                pt(e, "tpan_cur", t)),
                                                        mul(
                                                                e,
                                                                p(e, "gvol_cur"),
                                                                sub(e, f32(e, e.n(2)), p(e, "gpan_cur")))))))),
                pending);
        var workBody = e.seq(
                set(e, p(e, "initialising"), e.n(0)),
                inertiaUpdate(e, global(e, "gvol")),
                inertiaUpdate(e, global(e, "gpan")),
                inertiaUpdate(e, global(e, "gtune")),
                call(e, "zero", e.v("out"), e.v("n")),
                forRange(e, "t", e.n(TRACKS), workTrack));

        var body = new TList<SourceExpr>();
        body.add(e.let("wave_hermite", e.callM0(e.v("arg"), "wave_hermite")));
        body.add(e.let("zero", e.callM0(e.v("arg"), "zero")));
        body.add(e.let("pow", EcoBuzz.externDef(e, "pow", e.lowerV("d2_fn_t"), "m")));
        body.add(e.let(
                "init", EcoBuzz.funcDef(e, e.v("ll"), "sg_init", new String[] {"p"}, e.lowerV("init_fn_t"), initBody)));
        body.add(e.let(
                "tick", EcoBuzz.funcDef(e, e.v("ll"), "sg_tick", new String[] {"p"}, e.lowerV("tick_fn_t"), tickBody)));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "sg_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), workBody)));
        return EcoBuzz.machineModule(
                e, "samplegrid", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
