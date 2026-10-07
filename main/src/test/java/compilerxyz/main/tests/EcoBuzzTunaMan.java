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
import static compilerxyz.main.tests.EcoBuzz.i32;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.ifOnly;
import static compilerxyz.main.tests.EcoBuzz.lt;
import static compilerxyz.main.tests.EcoBuzz.lw;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.ne;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzTunaMan {
    private EcoBuzzTunaMan() {}

    static final int NG = 1;
    static final int NT = 9;
    static final int TRACKS = 8;
    private static final int DELAY = 32768;
    private static final int DELAY_MASK = DELAY - 1;
    private static final int CHUNK = 64;
    private static final double RATE = 44100.0;
    private static final double PI = Math.PI;
    private static final IList<Integer> TIMES = IList.of(1, 2, 3, 4, 6, 8, 12, 16, 24, 28, 32, 48, 64, 96, 128);
    private static final IList<String> T_I32_FIELDS =
            IList.of("t_transpose", "t_finetune", "t_overlap", "t_vibrate", "t_vibdepth", "t_reverse");
    private static final IList<String> T_F32_FIELDS = IList.of("t_moddepth", "t_wetout", "t_lfodelta");

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pt(EcoMb e, String field, SourceExpr t) {
        return idx(e, fld(e, e.v("p"), field), t);
    }

    private static SourceExpr tval(EcoMb e, SourceExpr t, int k) {
        return idx(e, idx(e, p(e, "tval"), t), e.n(k));
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

    private static SourceExpr dbGain(EcoMb e, SourceExpr v) {
        return e.ifElse(
                ne(e, v, e.n(0)),
                call(e, "powf", f32(e, e.n(2)), mul(e, sub(e, mul(e, f32(e, v), e.n(0.1)), e.n(24)), e.n(1.0 / 6))),
                f32(e, e.n(0)));
    }

    private static SourceExpr modDepth(EcoMb e, SourceExpr v) {
        return f32(e, i32(e, mul(e, add(e, mul(e, f32(e, v), e.n(2.2)), e.n(30)), e.n(RATE / 1000))));
    }

    private static SourceExpr lfoDelta(EcoMb e, SourceExpr rate) {
        return e.ifElse(
                lt(e, rate, e.n(240)),
                mul(
                        e,
                        call(e, "powf", f32(e, e.n(800)), mul(e, f32(e, rate), e.n(1.0 / 255))),
                        e.n(2 * PI * 0.05 / RATE)),
                div(
                        e,
                        mul(e, f32(e, i32(e, div(e, f32(e, e.n(RATE)), f32(e, p(e, "spt"))))), e.n(2 * PI / RATE)),
                        f32(e, idx(e, lw(e, "times"), sub(e, rate, e.n(240))))));
    }

    private static SourceExpr f64(EcoMb e, SourceExpr v) {
        return EcoBuzz.conv(e, "t_f64", v);
    }

    private static SourceExpr readTap(EcoMb e, SourceExpr ch, SourceExpr intpos, SourceExpr frac) {
        var base = mul(e, ch, e.n(DELAY));
        var d0 = idx(e, p(e, "buf"), add(e, base, and(e, intpos, e.n(DELAY_MASK))));
        var d1 = idx(e, p(e, "buf"), add(e, base, and(e, add(e, intpos, e.n(1)), e.n(DELAY_MASK))));
        return add(e, d0, mul(e, frac, sub(e, d1, d0)));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("global_list_i32", e.call(EcoBuzz.llvm(e, "global_list"), e.v("t_i32"))));
        defs.add(e.let("i32xt", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS))));
        defs.add(e.let("f32xt", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "spt", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        fields.add(EcoBuzz.field(e, "buf", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * DELAY))));
        fields.add(EcoBuzz.field(e, "pos", e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "dryout", e.v("t_f32")));
        fields.add(EcoBuzz.field(e, "t_phase", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * TRACKS))));
        fields.add(EcoBuzz.field(e, "t_lfophase", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2 * TRACKS))));
        fields.add(EcoBuzz.field(e, "dc_x1", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2))));
        fields.add(EcoBuzz.field(e, "dc_y1", EcoBuzz.array(e, e.v("t_f32"), e.intOf(2))));
        for (var f : T_I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32xt")));
        for (var f : T_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xt")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "tm_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("f2_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("d1_fn_t", EcoBuzz.fnType(e, e.v("t_f64"), e.v("t_f64"))));

        var body = new TList<SourceExpr>();
        body.add(e.let("floor", EcoBuzz.externDef(e, "floor", e.lowerV("d1_fn_t"), "m")));
        for (var fn : new String[] {"sinf"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, fn, e.lowerV("f1_fn_t"), "m")));
        }
        for (var fn : new String[] {"powf", "fmodf"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, fn, e.lowerV("f2_fn_t"), "m")));
        }
        var timesInit = new TList<SourceExpr>();
        for (var v : TIMES) timesInit.add(e.intOf(v));
        body.add(e.let(
                "times",
                e.call(
                        e.lowerV("global_list_i32"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("tm_times")),
                                e.p("values", e.listNew(e.mb.typesInt(), timesInit.toArray(SourceExpr[]::new)))))));

        var t = e.v("t");
        var init = e.seq(
                set(e, p(e, "dryout"), e.n(1)),
                set(e, p(e, "pos"), e.n(0)),
                forRange(e, "i", e.n(2 * DELAY), set(e, idx(e, p(e, "buf"), e.v("i")), e.n(0))),
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        set(e, pt(e, "t_moddepth", t), modDepth(e, EcoBuzz.i32(e, e.n(20)))),
                        set(e, pt(e, "t_transpose", t), e.n(12)),
                        set(e, pt(e, "t_finetune", t), e.n(20)),
                        set(e, pt(e, "t_wetout", t), e.ifElse(eq(e, t, e.n(0)), f32(e, e.n(1)), f32(e, e.n(0)))),
                        set(e, pt(e, "t_overlap", t), e.n(80)),
                        set(e, pt(e, "t_vibdepth", t), e.n(0)),
                        set(e, pt(e, "t_vibrate", t), e.n(80)),
                        set(e, pt(e, "t_reverse", t), e.n(0)),
                        set(e, pt(e, "t_lfophase", mul(e, t, e.n(2))), e.n(0)),
                        set(e, pt(e, "t_lfophase", add(e, mul(e, t, e.n(2)), e.n(1))), e.n(0)),
                        set(e, pt(e, "t_lfodelta", t), lfoDelta(e, EcoBuzz.i32(e, e.n(80)))),
                        set(e, pt(e, "t_phase", mul(e, t, e.n(2))), e.n(0)),
                        set(e, pt(e, "t_phase", add(e, mul(e, t, e.n(2)), e.n(1))), e.n(0))),
                resetVals(e));
        body.add(e.let(
                "init", EcoBuzz.funcDef(e, e.v("ll"), "tm_init", new String[] {"p"}, e.lowerV("init_fn_t"), init)));

        var tick = e.seq(
                forRange(
                        e,
                        "t",
                        e.n(TRACKS),
                        e.let("md", tval(e, t, 0)),
                        ifOnly(e, ne(e, e.v("md"), e.n(-1)), set(e, pt(e, "t_moddepth", t), modDepth(e, e.v("md")))),
                        e.let("tr", tval(e, t, 1)),
                        ifOnly(
                                e,
                                ne(e, e.v("tr"), e.n(-1)),
                                set(e, pt(e, "t_transpose", t), sub(e, e.v("tr"), e.n(24)))),
                        e.let("ft", tval(e, t, 2)),
                        ifOnly(
                                e,
                                ne(e, e.v("ft"), e.n(-1)),
                                set(e, pt(e, "t_finetune", t), sub(e, e.v("ft"), e.n(100)))),
                        e.let("wo", tval(e, t, 3)),
                        ifOnly(e, ne(e, e.v("wo"), e.n(-1)), set(e, pt(e, "t_wetout", t), dbGain(e, e.v("wo")))),
                        e.let("ov", tval(e, t, 4)),
                        ifOnly(e, ne(e, e.v("ov"), e.n(-1)), set(e, pt(e, "t_overlap", t), e.v("ov"))),
                        e.let("vd", tval(e, t, 5)),
                        ifOnly(e, ne(e, e.v("vd"), e.n(-1)), set(e, pt(e, "t_vibdepth", t), e.v("vd"))),
                        e.let("vr", tval(e, t, 6)),
                        ifOnly(e, ne(e, e.v("vr"), e.n(-1)), set(e, pt(e, "t_vibrate", t), e.v("vr"))),
                        e.let("rv", tval(e, t, 7)),
                        ifOnly(e, ne(e, e.v("rv"), e.n(-1)), set(e, pt(e, "t_reverse", t), e.v("rv"))),
                        set(e, pt(e, "t_lfodelta", t), lfoDelta(e, pt(e, "t_vibrate", t))),
                        ifOnly(
                                e,
                                eq(e, tval(e, t, 8), e.n(1)),
                                set(e, pt(e, "t_lfophase", mul(e, t, e.n(2))), e.n(0)),
                                set(e, pt(e, "t_lfophase", add(e, mul(e, t, e.n(2)), e.n(1))), e.n(0)))),
                e.let("dr", idx(e, p(e, "gval"), e.n(0))),
                ifOnly(e, ne(e, e.v("dr"), e.n(-1)), set(e, p(e, "dryout"), dbGain(e, e.v("dr")))),
                resetVals(e));
        body.add(e.let(
                "tick", EcoBuzz.funcDef(e, e.v("ll"), "tm_tick", new String[] {"p"}, e.lowerV("tick_fn_t"), tick)));

        var i = e.v("i");
        var ch = e.v("ch");
        var work = e.seq(
                e.let("silent", EcoBuzz.i32(e, e.n(0))),
                forRange(
                        e,
                        "ch",
                        e.n(2),
                        ifOnly(
                                e,
                                eq(e, e.v("silent"), e.n(0)),
                                forRange(
                                        e,
                                        "i",
                                        e.v("n"),
                                        e.let("x", idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), ch))),
                                        set(
                                                e,
                                                idx(
                                                        e,
                                                        p(e, "buf"),
                                                        add(
                                                                e,
                                                                mul(e, ch, e.n(DELAY)),
                                                                and(e, add(e, p(e, "pos"), i), e.n(DELAY_MASK)))),
                                                e.v("x")),
                                        set(
                                                e,
                                                idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), ch)),
                                                mul(e, p(e, "dryout"), e.v("x")))),
                                forRange(
                                        e,
                                        "t",
                                        e.n(TRACKS),
                                        ifOnly(
                                                e,
                                                ne(e, pt(e, "t_wetout", t), e.n(0)),
                                                e.let(
                                                        "ratio",
                                                        call(
                                                                e,
                                                                "powf",
                                                                f32(e, e.n(2)),
                                                                add(
                                                                        e,
                                                                        mul(
                                                                                e,
                                                                                f32(e, pt(e, "t_transpose", t)),
                                                                                e.n(1.0 / 12)),
                                                                        mul(
                                                                                e,
                                                                                f32(e, pt(e, "t_finetune", t)),
                                                                                e.n(1.0 / 1200))))),
                                                e.let(
                                                        "delta",
                                                        f64(
                                                                e,
                                                                e.ifElse(
                                                                        ne(e, pt(e, "t_reverse", t), e.n(0)),
                                                                        add(e, e.v("ratio"), e.n(1)),
                                                                        add(
                                                                                e,
                                                                                e.mb.operatorNeg(e.v("ratio")),
                                                                                e.n(1))))),
                                                e.let("odmd", div(e, f64(e, e.n(1)), f64(e, pt(e, "t_moddepth", t)))),
                                                e.let("disp", i32(e, mul(e, pt(e, "t_moddepth", t), e.n(0.5)))),
                                                e.let("dispf", f64(e, e.v("disp"))),
                                                e.let("smooth", mul(e, f64(e, pt(e, "t_overlap", t)), e.n(0.005))),
                                                e.let("odsmooth", div(e, f64(e, e.n(1)), e.v("smooth"))),
                                                e.let("pi", add(e, mul(e, t, e.n(2)), ch)),
                                                e.let("phase", f64(e, pt(e, "t_phase", e.v("pi")))),
                                                e.let("npos", p(e, "pos")),
                                                e.let("so", EcoBuzz.i32(e, e.n(0))),
                                                EcoBuzz.whileLoop(
                                                        e,
                                                        "chunk",
                                                        lt(e, e.v("so"), e.v("n")),
                                                        e.let(
                                                                "c",
                                                                EcoBuzz.min(
                                                                        e,
                                                                        EcoBuzz.i32(e, e.n(CHUNK)),
                                                                        sub(e, e.v("n"), e.v("so")))),
                                                        e.let("vibterm", f64(e, e.n(0))),
                                                        ifOnly(
                                                                e,
                                                                ne(e, pt(e, "t_vibdepth", t), e.n(0)),
                                                                set(
                                                                        e,
                                                                        e.v("vibterm"),
                                                                        f64(
                                                                                e,
                                                                                sub(
                                                                                        e,
                                                                                        call(
                                                                                                e,
                                                                                                "powf",
                                                                                                f32(e, e.n(2)),
                                                                                                mul(
                                                                                                        e,
                                                                                                        mul(
                                                                                                                e,
                                                                                                                f32(
                                                                                                                        e,
                                                                                                                        pt(
                                                                                                                                e,
                                                                                                                                "t_vibdepth",
                                                                                                                                t)),
                                                                                                                e.n(
                                                                                                                        1.0
                                                                                                                                / 1200)),
                                                                                                        call(
                                                                                                                e,
                                                                                                                "sinf",
                                                                                                                pt(
                                                                                                                        e,
                                                                                                                        "t_lfophase",
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "pi"))))),
                                                                                        e.n(1)))),
                                                                set(
                                                                        e,
                                                                        pt(e, "t_lfophase", e.v("pi")),
                                                                        call(
                                                                                e,
                                                                                "fmodf",
                                                                                add(
                                                                                        e,
                                                                                        pt(e, "t_lfophase", e.v("pi")),
                                                                                        mul(
                                                                                                e,
                                                                                                pt(e, "t_lfodelta", t),
                                                                                                mul(
                                                                                                        e,
                                                                                                        f32(
                                                                                                                e,
                                                                                                                e.v(
                                                                                                                        "c")),
                                                                                                        e.n(2)))),
                                                                                f32(e, e.n(2 * PI))))),
                                                        forRange(
                                                                e,
                                                                "k",
                                                                e.v("c"),
                                                                e.let("i", add(e, e.v("so"), e.v("k"))),
                                                                e.let("ph", mul(e, e.v("phase"), e.v("odmd"))),
                                                                e.let(
                                                                        "rp0",
                                                                        sub(
                                                                                e,
                                                                                f64(e, e.v("npos")),
                                                                                add(e, e.v("phase"), e.n(10)))),
                                                                e.let(
                                                                        "rp",
                                                                        e.ifElse(
                                                                                lt(e, e.v("rp0"), e.n(0)),
                                                                                add(e, e.v("rp0"), e.n(DELAY)),
                                                                                e.v("rp0"))),
                                                                e.let(
                                                                        "ip",
                                                                        i32(
                                                                                e,
                                                                                call(
                                                                                        e,
                                                                                        "floor",
                                                                                        add(e, e.v("rp"), e.n(0.5))))),
                                                                e.let(
                                                                        "frac",
                                                                        f32(e, sub(e, e.v("rp"), f64(e, e.v("ip"))))),
                                                                e.let("a0", readTap(e, ch, e.v("ip"), e.v("frac"))),
                                                                e.let("a", e.v("a0")),
                                                                ifOnly(
                                                                        e,
                                                                        lt(e, e.v("ph"), e.v("smooth")),
                                                                        e.let(
                                                                                "win",
                                                                                f32(
                                                                                        e,
                                                                                        mul(
                                                                                                e,
                                                                                                e.v("ph"),
                                                                                                e.v("odsmooth")))),
                                                                        set(
                                                                                e,
                                                                                e.v("a"),
                                                                                add(
                                                                                        e,
                                                                                        mul(e, e.v("win"), e.v("a0")),
                                                                                        mul(
                                                                                                e,
                                                                                                sub(
                                                                                                        e,
                                                                                                        f32(e, e.n(1)),
                                                                                                        e.v("win")),
                                                                                                readTap(
                                                                                                        e,
                                                                                                        ch,
                                                                                                        sub(
                                                                                                                e,
                                                                                                                e.v(
                                                                                                                        "ip"),
                                                                                                                e.v(
                                                                                                                        "disp")),
                                                                                                        e.v(
                                                                                                                "frac")))))),
                                                                set(
                                                                        e,
                                                                        idx(
                                                                                e,
                                                                                e.v("out"),
                                                                                add(e, mul(e, i, e.n(2)), ch)),
                                                                        add(
                                                                                e,
                                                                                idx(
                                                                                        e,
                                                                                        e.v("out"),
                                                                                        add(e, mul(e, i, e.n(2)), ch)),
                                                                                mul(
                                                                                        e,
                                                                                        pt(e, "t_wetout", t),
                                                                                        e.v("a")))),
                                                                set(
                                                                        e,
                                                                        e.v("phase"),
                                                                        add(
                                                                                e,
                                                                                e.v("phase"),
                                                                                add(e, e.v("delta"), e.v("vibterm")))),
                                                                ifOnly(
                                                                        e,
                                                                        ge(e, e.v("phase"), e.v("dispf")),
                                                                        set(
                                                                                e,
                                                                                e.v("phase"),
                                                                                sub(e, e.v("phase"), e.v("dispf")))),
                                                                ifOnly(
                                                                        e,
                                                                        lt(e, e.v("phase"), e.n(0)),
                                                                        set(
                                                                                e,
                                                                                e.v("phase"),
                                                                                add(e, e.v("phase"), e.v("dispf")))),
                                                                set(
                                                                        e,
                                                                        e.v("npos"),
                                                                        and(
                                                                                e,
                                                                                add(e, e.v("npos"), e.n(1)),
                                                                                e.n(DELAY_MASK)))),
                                                        set(e, e.v("phase"), f64(e, f32(e, e.v("phase")))),
                                                        set(e, e.v("so"), add(e, e.v("so"), e.v("c")))),
                                                set(e, pt(e, "t_phase", e.v("pi")), f32(e, e.v("phase"))))),
                                ifOnly(
                                        e,
                                        eq(e, ch, e.n(0)),
                                        set(e, e.v("silent"), e.n(1)),
                                        forRange(
                                                e,
                                                "i",
                                                e.v("n"),
                                                e.let("y", idx(e, e.v("out"), mul(e, i, e.n(2)))),
                                                ifOnly(
                                                        e,
                                                        EcoBuzz.or(
                                                                e,
                                                                ge(e, e.v("y"), e.n(1)),
                                                                EcoBuzz.le(e, e.v("y"), e.n(-1))),
                                                        set(e, e.v("silent"), e.n(0))))))),
                set(e, p(e, "pos"), and(e, add(e, p(e, "pos"), e.v("n")), e.n(DELAY_MASK))),
                ifOnly(
                        e,
                        ne(e, e.v("silent"), e.n(0)),
                        forRange(e, "i", mul(e, e.v("n"), e.n(2)), set(e, idx(e, e.v("out"), i), e.n(0)))),
                ifOnly(e, eq(e, e.v("silent"), e.n(0)), EcoBuzz.mdkDcBlock(e, e.v("out"), e.v("n"))));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(e, e.v("ll"), "tm_work", new String[] {"p", "out", "n"}, e.lowerV("work_fn_t"), work)));
        return EcoBuzz.machineModule(
                e, "tunaman", defs, new String[] {"init", "tick", "work"}, new TList<>(), body, new SourceExpr[] {
                    e.v("init"), e.v("tick"), e.v("work")
                });
    }
}
