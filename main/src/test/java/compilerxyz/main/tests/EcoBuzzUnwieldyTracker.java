package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.and;
import static compilerxyz.main.tests.EcoBuzz.div;
import static compilerxyz.main.tests.EcoBuzz.eq;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.fld;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.ge;
import static compilerxyz.main.tests.EcoBuzz.gt;
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

final class EcoBuzzUnwieldyTracker {
    private EcoBuzzUnwieldyTracker() {}

    static final int NT = 13;
    private static final int P_SUB = 0;
    private static final int P_OFS = 3;
    private static final int P_STR = 4;
    private static final int P_NOTE = 5;
    private static final int P_SMP = 6;
    private static final int P_VEL = 7;
    private static final int P_PAN = 8;
    private static final int P_EC1 = 9;
    private static final int P_EP1 = 10;
    private static final int P_EC2 = 11;
    private static final int P_EP2 = 12;
    private static final int ROOT_MIDI = 48;
    private static final double RATE = 44100.0;
    private static final double SMARK = 10000.0 / RATE;
    private static final double RAMP_ATTR = 50.0;
    private static final double DECLICK_ATTR = 15.0;

    private static final IList<String> I32_FIELDS =
            IList.of("spt", "smp", "stretch", "active", "dying", "ocount", "ofl", "subdiv");
    private static final IList<String> F32_FIELDS =
            IList.of("fstep", "pos", "amp", "tamp", "astep", "pan", "tpan", "pstep", "opos", "ogain", "ofd", "vel");

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr tval(EcoMb e, int k) {
        return idx(e, p(e, "tval"), e.n(k));
    }

    private static SourceExpr wave(EcoMb e, SourceExpr wi, String field) {
        return fld(e, idx(e, fld(e, p(e, "waves"), "w"), wi), field);
    }

    private static SourceExpr resetVals(EcoMb e) {
        return forRange(e, "k", e.n(NT), set(e, idx(e, p(e, "tval"), e.v("k")), e.n(EcoBuzz.NO_VALUE)));
    }

    private static SourceExpr stretchStep(EcoMb e) {
        return div(e, div(e, f32(e, wave(e, p(e, "smp"), "frames")), f32(e, p(e, "stretch"))), f32(e, p(e, "spt")));
    }

    private static SourceExpr reverseCommand(EcoMb e, int ecIndex, int epIndex) {
        var ep = e.v("ep");
        return e.seq(
                e.let("ep", tval(e, epIndex)),
                ifOnly(
                        e,
                        and(e, eq(e, tval(e, ecIndex), e.n(8)), ne(e, ep, e.n(-1))),
                        e.let("astep", EcoBuzz.call(e, "fabsf", p(e, "fstep"))),
                        e.let("sdel", div(e, mul(e, f32(e, p(e, "spt")), f32(e, ep)), f32(e, p(e, "subdiv")))),
                        set(e, p(e, "pos"), add(e, p(e, "pos"), mul(e, e.v("sdel"), e.v("astep")))),
                        set(e, p(e, "fstep"), e.mb.operatorNeg(e.v("astep")))));
    }

    private static SourceExpr rampToward(EcoMb e, String cur, String target, String step) {
        return e.ifElse(
                lt(e, p(e, cur), p(e, target)),
                set(e, p(e, cur), EcoBuzz.min(e, add(e, p(e, cur), p(e, step)), p(e, target))),
                set(
                        e,
                        p(e, cur),
                        e.ifElse(
                                EcoBuzz.gt(e, sub(e, p(e, cur), p(e, step)), p(e, target)),
                                sub(e, p(e, cur), p(e, step)),
                                p(e, target))));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "tval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT))));
        for (var f : I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_i32")));
        for (var f : F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_f32")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "ut_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("fabsf_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));

        var i = e.v("i");
        var w = e.v("w");
        var initBody = e.seq(
                resetVals(e),
                set(e, p(e, "smp"), e.n(-1)),
                set(e, p(e, "stretch"), e.n(0)),
                set(e, p(e, "active"), e.n(0)),
                set(e, p(e, "dying"), e.n(0)),
                set(e, p(e, "subdiv"), e.n(6)),
                set(e, p(e, "fstep"), e.n(1)),
                set(e, p(e, "pos"), e.n(0)),
                set(e, p(e, "amp"), e.n(0)),
                set(e, p(e, "tamp"), e.n(1)),
                set(e, p(e, "vel"), e.n(1)),
                set(e, p(e, "astep"), e.n(SMARK / RAMP_ATTR)),
                set(e, p(e, "pan"), e.n(1)),
                set(e, p(e, "tpan"), e.n(1)),
                set(e, p(e, "pstep"), e.n(SMARK / RAMP_ATTR)),
                set(e, p(e, "ocount"), e.n(0)),
                set(e, p(e, "ogain"), e.n(0)),
                set(e, p(e, "opos"), e.n(0)),
                set(e, p(e, "ofd"), e.n(Math.pow(0.001, SMARK / DECLICK_ATTR))),
                set(e, p(e, "ofl"), e.n((int) (DECLICK_ATTR / SMARK))));
        var tickBody = e.seq(
                e.let("sub", tval(e, P_SUB)),
                ifOnly(e, ne(e, e.v("sub"), e.n(-1)), set(e, p(e, "subdiv"), e.v("sub"))),
                e.let("str", tval(e, P_STR)),
                ifOnly(
                        e,
                        ne(e, e.v("str"), e.n(-1)),
                        set(e, p(e, "stretch"), e.v("str")),
                        ifOnly(
                                e,
                                and(e, ne(e, e.v("str"), e.n(0)), ne(e, p(e, "smp"), e.n(-1))),
                                set(e, p(e, "fstep"), stretchStep(e)))),
                e.let("pan", tval(e, P_PAN)),
                ifOnly(
                        e,
                        ne(e, e.v("pan"), e.n(-1)),
                        set(e, p(e, "tpan"), sub(e, f32(e, e.n(2)), div(e, f32(e, e.v("pan")), e.n(64))))),
                e.let("vel", tval(e, P_VEL)),
                ifOnly(
                        e,
                        ne(e, e.v("vel"), e.n(-1)),
                        set(e, p(e, "vel"), div(e, f32(e, e.v("vel")), e.n(64))),
                        set(e, p(e, "tamp"), p(e, "vel"))),
                e.let("smp", tval(e, P_SMP)),
                ifOnly(e, ne(e, e.v("smp"), e.n(-1)), set(e, p(e, "smp"), sub(e, e.v("smp"), e.n(1)))),
                e.let("nte", tval(e, P_NOTE)),
                ifOnly(
                        e,
                        and(e, ne(e, e.v("nte"), e.n(-1)), ne(e, p(e, "smp"), e.n(-1))),
                        e.ifElse(
                                eq(e, e.v("nte"), e.n(EcoBuzz.NOTE_OFF)),
                                e.seq(set(e, p(e, "dying"), e.n(1)), set(e, p(e, "tamp"), e.n(0))),
                                e.seq(
                                        set(e, p(e, "pos"), e.n(0)),
                                        set(e, p(e, "ocount"), e.n(0)),
                                        set(e, p(e, "dying"), e.n(0)),
                                        set(e, p(e, "tamp"), p(e, "vel")),
                                        ifOnly(e, eq(e, p(e, "active"), e.n(0)), set(e, p(e, "amp"), e.n(0))),
                                        set(e, p(e, "active"), e.n(1)),
                                        set(
                                                e,
                                                p(e, "fstep"),
                                                e.ifElse(
                                                        ne(e, p(e, "stretch"), e.n(0)),
                                                        stretchStep(e),
                                                        mul(
                                                                e,
                                                                EcoBuzz.call(
                                                                        e, "note_rate", e.v("nte"), e.n(ROOT_MIDI)),
                                                                div(
                                                                        e,
                                                                        f32(e, wave(e, p(e, "smp"), "rate")),
                                                                        e.n(RATE)))))))),
                e.let("ofs", tval(e, P_OFS)),
                ifOnly(
                        e,
                        and(e, ne(e, e.v("ofs"), e.n(-1)), ne(e, p(e, "smp"), e.n(-1))),
                        ifOnly(
                                e,
                                ne(e, p(e, "active"), e.n(0)),
                                set(e, p(e, "opos"), p(e, "pos")),
                                set(e, p(e, "ogain"), e.n(1)),
                                set(e, p(e, "ocount"), p(e, "ofl"))),
                        set(
                                e,
                                p(e, "pos"),
                                div(
                                        e,
                                        mul(e, f32(e, wave(e, p(e, "smp"), "frames")), f32(e, e.v("ofs"))),
                                        e.n(65536)))),
                reverseCommand(e, P_EC1, P_EP1),
                reverseCommand(e, P_EC2, P_EP2),
                resetVals(e));
        var workBody = e.seq(
                EcoBuzz.call(e, "zero", e.v("out"), e.v("n")),
                ifOnly(
                        e,
                        ne(e, p(e, "active"), e.n(0)),
                        e.let("w", p(e, "smp")),
                        e.let("frames", f32(e, wave(e, w, "frames"))),
                        e.let("chn", wave(e, w, "channels")),
                        e.mb.labeled(
                                "gen",
                                forRange(
                                        e,
                                        "i",
                                        e.v("n"),
                                        ifOnly(
                                                e,
                                                or(e, ge(e, p(e, "pos"), e.v("frames")), lt(e, p(e, "pos"), e.n(0))),
                                                set(e, p(e, "active"), e.n(0)),
                                                set(e, p(e, "dying"), e.n(0)),
                                                set(e, p(e, "amp"), e.n(0)),
                                                e.mb.returnLabel("gen")),
                                        e.let("l", EcoBuzz.call(e, "wave_read", p(e, "waves"), w, p(e, "pos"), e.n(0))),
                                        e.let(
                                                "r",
                                                e.ifElse(
                                                        gt(e, e.v("chn"), e.n(1)),
                                                        EcoBuzz.call(
                                                                e, "wave_read", p(e, "waves"), w, p(e, "pos"), e.n(1)),
                                                        e.v("l"))),
                                        ifOnly(
                                                e,
                                                and(
                                                        e,
                                                        gt(e, p(e, "ocount"), e.n(0)),
                                                        and(
                                                                e,
                                                                ge(e, p(e, "opos"), e.n(0)),
                                                                lt(e, p(e, "opos"), e.v("frames")))),
                                                set(
                                                        e,
                                                        e.v("l"),
                                                        add(
                                                                e,
                                                                e.v("l"),
                                                                mul(
                                                                        e,
                                                                        EcoBuzz.call(
                                                                                e,
                                                                                "wave_read",
                                                                                p(e, "waves"),
                                                                                w,
                                                                                p(e, "opos"),
                                                                                e.n(0)),
                                                                        p(e, "ogain")))),
                                                set(
                                                        e,
                                                        e.v("r"),
                                                        add(
                                                                e,
                                                                e.v("r"),
                                                                mul(
                                                                        e,
                                                                        e.ifElse(
                                                                                gt(e, e.v("chn"), e.n(1)),
                                                                                EcoBuzz.call(
                                                                                        e,
                                                                                        "wave_read",
                                                                                        p(e, "waves"),
                                                                                        w,
                                                                                        p(e, "opos"),
                                                                                        e.n(1)),
                                                                                EcoBuzz.call(
                                                                                        e,
                                                                                        "wave_read",
                                                                                        p(e, "waves"),
                                                                                        w,
                                                                                        p(e, "opos"),
                                                                                        e.n(0))),
                                                                        p(e, "ogain")))),
                                                set(e, p(e, "opos"), add(e, p(e, "opos"), p(e, "fstep"))),
                                                set(e, p(e, "ogain"), mul(e, p(e, "ogain"), p(e, "ofd"))),
                                                set(e, p(e, "ocount"), sub(e, p(e, "ocount"), e.n(1)))),
                                        e.let("gl", EcoBuzz.min(e, p(e, "pan"), f32(e, e.n(1)))),
                                        e.let(
                                                "gr",
                                                EcoBuzz.min(e, sub(e, f32(e, e.n(2)), p(e, "pan")), f32(e, e.n(1)))),
                                        set(
                                                e,
                                                idx(e, e.v("out"), mul(e, i, e.n(2))),
                                                mul(e, mul(e, e.v("l"), p(e, "amp")), e.v("gl"))),
                                        set(
                                                e,
                                                idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                                                mul(e, mul(e, e.v("r"), p(e, "amp")), e.v("gr"))),
                                        rampToward(e, "amp", "tamp", "astep"),
                                        rampToward(e, "pan", "tpan", "pstep"),
                                        ifOnly(
                                                e,
                                                and(e, ne(e, p(e, "dying"), e.n(0)), le(e, p(e, "amp"), e.n(0))),
                                                set(e, p(e, "active"), e.n(0)),
                                                set(e, p(e, "dying"), e.n(0)),
                                                e.mb.returnLabel("gen")),
                                        set(e, p(e, "pos"), add(e, p(e, "pos"), p(e, "fstep")))))));
        var body = new TList<SourceExpr>();
        body.add(e.let("wave_read", e.callM0(e.v("arg"), "wave_read")));
        body.add(e.let("zero", e.callM0(e.v("arg"), "zero")));
        body.add(e.let("note_rate", e.callM0(e.v("arg"), "note_rate")));
        body.add(e.let("fabsf", EcoBuzz.externDef(e, e.v("ll"), "fabsf", e.lowerV("fabsf_fn_t"))));
        body.add(e.let(
                "init", EcoBuzz.funcDef(e, e.v("ll"), "ut_init", new String[] {"p"}, e.lowerV("init_fn_t"), initBody)));
        body.add(e.let(
                "tick", EcoBuzz.funcDef(e, e.v("ll"), "ut_tick", new String[] {"p"}, e.lowerV("tick_fn_t"), tickBody)));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e, e.v("ll"), "ut_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), workBody)));
        return EcoBuzz.machineModule(
                e,
                "unwieldytracker",
                defs,
                new String[] {"init", "tick", "work"},
                new TList<>(),
                body,
                new SourceExpr[] {e.v("init"), e.v("tick"), e.v("work")});
    }
}
