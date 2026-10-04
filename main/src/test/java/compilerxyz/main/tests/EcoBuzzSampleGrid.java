package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.div;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.fld;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.ge;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.ifOnly;
import static compilerxyz.main.tests.EcoBuzz.le;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.ne;
import static compilerxyz.main.tests.EcoBuzz.or;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzSampleGrid {
    private EcoBuzzSampleGrid() {}

    static final int TRACKS = 16;
    static final int NG = 29;
    static final int NT = 9;
    static final double RAMP = 50.0;

    private static final IList<String> I32_TRACK_FIELDS = IList.of("tw", "vact", "vw", "fact", "fw");
    private static final IList<String> F32_TRACK_FIELDS =
            IList.of("tvol", "ttune", "vpos", "vrate", "vamp", "vgain", "fpos", "frate", "famp", "fgain");

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pt(EcoMb e, String field, SourceExpr t) {
        return idx(e, fld(e, e.v("p"), field), t);
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

    private static SourceExpr voiceLoop(
            EcoMb e,
            String label,
            String act,
            String wf,
            String pos,
            String rate,
            String amp,
            String gain,
            SourceExpr gainStep) {
        var t = e.v("t");
        var i = e.v("i");
        var w = e.v("w");
        return ifOnly(
                e,
                ne(e, pt(e, act, t), e.n(0)),
                e.let("w", pt(e, wf, t)),
                e.let("frames", f32(e, wave(e, w, "frames"))),
                e.mb.labeled(
                        label,
                        forRange(
                                e,
                                "i",
                                e.v("n"),
                                ifOnly(
                                        e,
                                        or(e, ge(e, pt(e, pos, t), e.v("frames")), le(e, pt(e, gain, t), e.n(0))),
                                        set(e, pt(e, act, t), e.n(0)),
                                        e.mb.returnLabel(label)),
                                e.let(
                                        "s",
                                        mul(
                                                e,
                                                mul(
                                                        e,
                                                        EcoBuzz.call(
                                                                e,
                                                                "wave_read",
                                                                p(e, "waves"),
                                                                w,
                                                                pt(e, pos, t),
                                                                e.n(0)),
                                                        pt(e, amp, t)),
                                                pt(e, gain, t))),
                                set(
                                        e,
                                        idx(e, e.v("out"), mul(e, i, e.n(2))),
                                        add(e, idx(e, e.v("out"), mul(e, i, e.n(2))), e.v("s"))),
                                set(
                                        e,
                                        idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                                        add(e, idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))), e.v("s"))),
                                set(e, pt(e, pos, t), add(e, pt(e, pos, t), pt(e, rate, t))),
                                set(e, pt(e, gain, t), gainStep))));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("i32x16", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS))));
        defs.add(e.let("f32x16", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "spt", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        fields.add(EcoBuzz.field(e, "first_wave", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "gvol", e.v("t_f32")));
        for (var f : I32_TRACK_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32x16")));
        for (var f : F32_TRACK_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32x16")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "sg_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("exp2f_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));

        var t = e.v("t");
        var initBody = e.seq(
                set(e, p(e, "first_wave"), e.n(1)),
                set(e, p(e, "gvol"), e.n(1)),
                resetVals(e),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        set(e, pt(e, "tw", t), e.n(-1)),
                        set(e, pt(e, "tvol", t), e.n(1)),
                        set(e, pt(e, "ttune", t), e.n(0)),
                        set(e, pt(e, "vact", t), e.n(0)),
                        set(e, pt(e, "fact", t), e.n(0))));
        var tickBody = e.seq(
                e.let("fw", gval(e, e.n(0))),
                ifOnly(e, ne(e, e.v("fw"), e.n(-1)), set(e, p(e, "first_wave"), e.v("fw"))),
                e.let("gv", gval(e, e.n(21))),
                ifOnly(e, ne(e, e.v("gv"), e.n(-1)), set(e, p(e, "gvol"), div(e, f32(e, e.v("gv")), e.n(128)))),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        e.let("wn", tval(e, t, 0)),
                        ifOnly(e, ne(e, e.v("wn"), e.n(-1)), set(e, pt(e, "tw", t), sub(e, e.v("wn"), e.n(1)))),
                        e.let("tv", tval(e, t, 5)),
                        ifOnly(
                                e,
                                ne(e, e.v("tv"), e.n(-1)),
                                set(e, pt(e, "tvol", t), div(e, f32(e, e.v("tv")), e.n(128)))),
                        e.let("tt", tval(e, t, 7)),
                        ifOnly(
                                e,
                                ne(e, e.v("tt"), e.n(-1)),
                                set(e, pt(e, "ttune", t), div(e, f32(e, sub(e, e.v("tt"), e.n(128))), e.n(48)))),
                        e.let("trig", gval(e, add(e, t, e.n(2)))),
                        ifOnly(
                                e,
                                ne(e, e.v("trig"), e.n(-1)),
                                ifOnly(
                                        e,
                                        ne(e, pt(e, "vact", t), e.n(0)),
                                        (e.seq(
                                                set(e, pt(e, "fact", t), e.n(1)),
                                                set(e, pt(e, "fpos", t), pt(e, "vpos", t)),
                                                set(e, pt(e, "frate", t), pt(e, "vrate", t)),
                                                set(e, pt(e, "famp", t), pt(e, "vamp", t)),
                                                set(e, pt(e, "fgain", t), pt(e, "vgain", t)),
                                                set(e, pt(e, "fw", t), pt(e, "vw", t)),
                                                set(e, pt(e, "vact", t), e.n(0))))),
                                ifOnly(
                                        e,
                                        ne(e, e.v("trig"), e.n(0)),
                                        e.let(
                                                "wi",
                                                e.ifElse(
                                                        ne(e, pt(e, "tw", t), e.n(-1)),
                                                        pt(e, "tw", t),
                                                        add(e, sub(e, p(e, "first_wave"), e.n(1)), t))),
                                        set(e, pt(e, "vw", t), e.v("wi")),
                                        set(e, pt(e, "vpos", t), e.n(0)),
                                        set(
                                                e,
                                                pt(e, "vrate", t),
                                                mul(
                                                        e,
                                                        EcoBuzz.call(e, "exp2f", pt(e, "ttune", t)),
                                                        div(e, f32(e, wave(e, e.v("wi"), "rate")), e.n(44100)))),
                                        set(
                                                e,
                                                pt(e, "vamp", t),
                                                mul(
                                                        e,
                                                        mul(e, div(e, f32(e, e.v("trig")), e.n(128)), pt(e, "tvol", t)),
                                                        p(e, "gvol"))),
                                        set(e, pt(e, "vgain", t), e.n(0)),
                                        set(e, pt(e, "vact", t), e.n(1))))),
                resetVals(e));
        var workBody = e.seq(
                EcoBuzz.call(e, "zero", e.v("out"), e.v("n")),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        voiceLoop(
                                e,
                                "fade",
                                "fact",
                                "fw",
                                "fpos",
                                "frate",
                                "famp",
                                "fgain",
                                sub(e, pt(e, "fgain", t), e.n(1.0 / RAMP))),
                        voiceLoop(
                                e,
                                "voice",
                                "vact",
                                "vw",
                                "vpos",
                                "vrate",
                                "vamp",
                                "vgain",
                                EcoBuzz.min(e, add(e, pt(e, "vgain", t), e.n(1.0 / RAMP)), f32(e, e.n(1))))));
        var body = new TList<SourceExpr>();
        body.add(e.let("wave_read", e.callM0(e.v("arg"), "wave_read")));
        body.add(e.let("zero", e.callM0(e.v("arg"), "zero")));
        body.add(e.let("exp2f", EcoBuzz.externDef(e, e.v("ll"), "exp2f", e.lowerV("exp2f_fn_t"))));
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
