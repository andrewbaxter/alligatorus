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
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.shl;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.function.BiFunction;
import java.util.function.Function;

final class EcoBuzzInfector {
    private EcoBuzzInfector() {}

    static final int NG = 38;
    static final int NT = 7;
    static final int TRACKS = 12;
    static final int CHANNELS = 24;
    static final int WAVE = 2048;
    static final int LEVEL0 = WAVE + 4;
    static final int LEVELN = WAVE / 2 + 4;
    static final int ATTR_HQ = 1;
    static final int ATTR_THEVIDERNESS = 20;
    static final int ATTR_TUNING = 0;
    private static final double RATE = 44100.0;
    private static final double PI = Math.PI;

    private record Table(String name, double factor, double maxScan) {}

    static final IList<Table> TABLES = IList.of(
            new Table("sin", 8.0, 0.5),
            new Table("tri", 2.0, 0.25),
            new Table("sps", 1.3, 0.25),
            new Table("saw", 1.1, 0.25),
            new Table("sqr", 1.1, 0.25),
            new Table("hex", 1.64, 0.25),
            new Table("xt1", 1.5, 0.25),
            new Table("fm1", 1.5, 0.25),
            new Table("xt2", 1.5, 0.25),
            new Table("oct", 2.0, 0.25),
            new Table("prt", 4.0, 0.25),
            new Table("pr2", 4.0, 0.25),
            new Table("pr3", 4.0, 0.25),
            new Table("spl", 1.08, 0.25),
            new Table("smn", 1.1, 0.25),
            new Table("nul", 16.0, 0.5),
            new Table("sp2", 1.3, 0.25));

    static final IList<Integer> TABLES_A = IList.of(0, 1, 2, 3, 4, 5, 6, 3, 7, 7, 8, 9, 10, 11, 12, 13);
    static final IList<Integer> TABLES_B = IList.of(15, 1, 16, 3, 4, 5, 6, 4, 7, 4, 8, 9, 10, 11, 12, 14);
    static final IList<Integer> TABLES_C = IList.of(0, 1, 3, 2, 4, 9, 10, 11, 12, 5, 7, 6, 8);

    static int levelCount(Table t) {
        var count = 1;
        var max = 0.25 / WAVE;
        while (max <= t.maxScan()) {
            max *= t.factor();
            count++;
        }
        return count;
    }

    static int tableBase(int index) {
        var off = 0;
        for (int i = 0; i < index; i++) off += LEVEL0 + (levelCount(TABLES.get(i)) - 1) * LEVELN;
        return off;
    }

    static int levelIndexBase(int index) {
        var off = 0;
        for (int i = 0; i < index; i++) off += levelCount(TABLES.get(i));
        return off;
    }

    private static final IList<String> I32_FIELDS =
            IList.of("spt", "pwm1_phase", "pwm1_delta", "pwm1_depth", "pwm2_phase", "pwm2_delta", "pwm2_depth");
    private static final IList<String> C_I32_FIELDS = IList.of("c_track");
    private static final IList<String> C_F32_FIELDS = IList.of(
            "c_freq", "c_ph1", "c_ph2", "c_ph3", "c_vel", "c_detune", "c_kt_ai", "c_kt_acc", "c_c2_ai", "c_c2_acc");
    private static final IList<String> BQ_FIELDS = IList.of("a1", "a2", "b0", "b1", "b2", "x1", "x2", "y1", "y2");
    private static final IList<String> ENV_I32_FIELDS = IList.of("state", "time", "att", "sus", "dec", "rel", "stage");
    private static final IList<String> ENV_F64_FIELDS = IList.of("start", "last", "series", "sustlevel", "silence");
    private static final IList<String> T_I32_FIELDS =
            IList.of("t_channel", "t_note", "t_accent", "t_length", "t_lastnote", "t_lastaccent", "t_lastlength");
    private static final IList<String> T_F32_FIELDS = IList.of(
            "t_destfreq",
            "t_basefreq",
            "t_detune",
            "t_lfophase",
            "t_lfo2phase",
            "t_curlfo",
            "t_curlfo2",
            "t_l1_ai",
            "t_l1_acc",
            "t_l2_ai",
            "t_l2_acc");
    private static final IList<Integer> GACT_DEFAULTS = IList.of(
            3, 80, 100, 180, 3, 80, 180, 120, 48, 8, 60, 1, 240, 0, 1, 60, 0, 200, 50, 80, 40, 10, 100, 90, 180, 80,
            140, 120, 0, 80, 120, 120, 0, 40, 40, 180, 20, 0);
    private static final IList<Integer> INERTIA_PARAMS = IList.of(15, 16, 17, 22, 26, 27, 30, 31);
    private static final IList<Integer> TIMES = IList.of(1, 2, 3, 4, 6, 8, 12, 16, 24, 28, 32, 48, 64, 96, 128);
    private static final double LN_SEMI = Math.log(Math.pow(2.0, 1.0 / 12.0));
    private static final IMap<String, String> FN_TYPES = IMap.ofEntries(
            new Pair<>("fft", "fft_fn_t"),
            new Pair<>("make", "make_fn_t"),
            new Pair<>("tables", "void0_fn_t"),
            new Pair<>("level", "level_fn_t"),
            new Pair<>("wave_read", "read_fn_t"),
            new Pair<>("env_set", "envset_fn_t"),
            new Pair<>("env_noteon", "pi_fn_t"),
            new Pair<>("env_notefast", "pi_fn_t"),
            new Pair<>("env_noteoff", "pi_fn_t"),
            new Pair<>("bq_nothing", "pi_fn_t"),
            new Pair<>("env_process", "envproc_fn_t"),
            new Pair<>("env_next", "envnext_fn_t"),
            new Pair<>("bq_rbj", "bqrbj_fn_t"),
            new Pair<>("bq_peq", "bqpeq_fn_t"),
            new Pair<>("bq_proc", "bqproc_fn_t"),
            new Pair<>("coeffs", "coeffs_fn_t"),
            new Pair<>("lfo", "lfo_fn_t"),
            new Pair<>("alloc", "alloc_fn_t"),
            new Pair<>("chan_copy", "pii_fn_t"),
            new Pair<>("playnote", "playnote_fn_t"),
            new Pair<>("dolfo", "pii_fn_t"),
            new Pair<>("chan_work", "chan_fn_t"),
            new Pair<>("sinf", "f1_fn_t"),
            new Pair<>("cosf", "f1_fn_t"),
            new Pair<>("expf", "f1_fn_t"),
            new Pair<>("logf", "f1_fn_t"),
            new Pair<>("fabsf", "f1_fn_t"),
            new Pair<>("tanf", "f1_fn_t"),
            new Pair<>("sqrtf", "f1_fn_t"),
            new Pair<>("floorf", "f1_fn_t"),
            new Pair<>("powf", "f2_fn_t"),
            new Pair<>("fmodf", "f2_fn_t"),
            new Pair<>("pow", "d2_fn_t"));
    private static final IMap<String, String> GLOBAL_TYPES = IMap.ofEntries(
            new Pair<>("pool", "pool_t"),
            new Pair<>("levmax", "levmax_t"),
            new Pair<>("basewave", "wave_t2"),
            new Pair<>("sre", "wave_t2"),
            new Pair<>("sim", "wave_t2"),
            new Pair<>("lre", "wave_t2"),
            new Pair<>("lim", "wave_t2"),
            new Pair<>("gre", "wave_t2"),
            new Pair<>("gim", "wave_t2"),
            new Pair<>("fsin", "wave_t2"),
            new Pair<>("fcos", "wave_t2"),
            new Pair<>("fscr", "iwave_t"),
            new Pair<>("isine", "iwave_t"),
            new Pair<>("tables_ready", "t_i32"),
            new Pair<>("tab_base", "tabs_t"),
            new Pair<>("tab_lev", "tabs_t"),
            new Pair<>("tab_count", "tabs_t"),
            new Pair<>("map_a", "map_a_t"),
            new Pair<>("map_b", "map_a_t"),
            new Pair<>("map_c", "map_c_t"),
            new Pair<>("times", "times_t"));

    private static SourceExpr p(EcoMb e, String field) {
        return fld(e, e.v("p"), field);
    }

    private static SourceExpr pa(EcoMb e, String field, SourceExpr i) {
        return idx(e, fld(e, e.v("p"), field), i);
    }

    private static SourceExpr gact(EcoMb e, int k) {
        return idx(e, p(e, "gact"), e.n(k));
    }

    private static SourceExpr tval(EcoMb e, SourceExpr t, int k) {
        return idx(e, idx(e, p(e, "tval"), t), e.n(k));
    }

    private static SourceExpr env(EcoMb e, String field, SourceExpr index) {
        return idx(e, p(e, "e_" + field), index);
    }

    private static SourceExpr bq(EcoMb e, String field, SourceExpr index) {
        return idx(e, p(e, "f_" + field), index);
    }

    private static SourceExpr clampf(EcoMb e, SourceExpr x, double lo, double hi) {
        return e.ifElse(lt(e, x, e.n(lo)), f32(e, e.n(lo)), e.ifElse(gt(e, x, e.n(hi)), f32(e, e.n(hi)), x));
    }

    private static SourceExpr envTime(EcoMb e, SourceExpr v) {
        return mul(e, call(e, "powf", f32(e, e.n(150.0)), div(e, f32(e, v), f32(e, e.n(240)))), e.n(0.08));
    }

    private static SourceExpr envTime2(EcoMb e, SourceExpr v) {
        return mul(e, call(e, "powf", f32(e, e.n(2400.0)), div(e, f32(e, v), f32(e, e.n(240)))), e.n(0.005));
    }

    private static SourceExpr sustLevel(EcoMb e, SourceExpr v) {
        return call(e, "powf", f32(e, e.n(1.0 / 128)), sub(e, f32(e, e.n(1)), div(e, f32(e, v), f32(e, e.n(240)))));
    }

    private static SourceExpr inertiaSet(EcoMb e, SourceExpr aiPlace, SourceExpr inertia) {
        return set(e, aiPlace, call(e, "expf", mul(e, add(e, f32(e, inertia), e.n(128)), e.n(-4.5 / 240.0))));
    }

    private static SourceExpr inertiaProcess(
            EcoMb e, SourceExpr ai, SourceExpr accPlace, SourceExpr value, SourceExpr c) {
        return ifOnly(
                e,
                ne(e, accPlace, value),
                e.let("diff", mul(e, ai, f32(e, c))),
                set(
                        e,
                        accPlace,
                        e.ifElse(
                                lt(e, call(e, "fabsf", sub(e, accPlace, value)), e.v("diff")),
                                value,
                                add(
                                        e,
                                        accPlace,
                                        e.ifElse(
                                                lt(e, value, accPlace), e.mb.operatorNeg(e.v("diff")), e.v("diff"))))));
    }

    private static SourceExpr rbj(
            EcoMb e, String kind, SourceExpr index, SourceExpr fc, SourceExpr q, SourceExpr gain) {
        return call(
                e,
                "bq_rbj",
                e.v("p"),
                index,
                EcoBuzz.i32(
                        e,
                        e.n(
                                switch (kind) {
                                    case "lpf" -> 0;
                                    case "hpf" -> 1;
                                    case "bpf" -> 2;
                                    case "brf" -> 3;
                                    default -> throw new IllegalArgumentException(kind);
                                })),
                fc,
                q,
                gain);
    }

    private static SourceExpr peq(
            EcoMb e, SourceExpr index, SourceExpr fc, SourceExpr q, SourceExpr v, SourceExpr gain) {
        return call(e, "bq_peq", e.v("p"), index, fc, q, v, gain);
    }

    private static SourceExpr one(EcoMb e) {
        return f32(e, e.n(1));
    }

    private static SourceExpr cutoffHz(EcoMb e, SourceExpr cutoff, double base, double lo, double hi) {
        return e.let(
                "cf",
                clampf(
                        e,
                        mul(e, call(e, "powf", f32(e, e.n(64)), div(e, cutoff, f32(e, e.n(240)))), e.n(base)),
                        lo,
                        hi));
    }

    private static SourceExpr filterType(EcoMb e, int type, SourceExpr... stmts) {
        return ifOnly(e, eq(e, e.v("ty"), e.n(type)), stmts);
    }

    static TList<SourceExpr> module(EcoMb e) {
        var defs = new TList<SourceExpr>();
        defs.add(e.let("i32xc", EcoBuzz.array(e, e.v("t_i32"), e.intOf(CHANNELS))));
        defs.add(e.let("f32xc", EcoBuzz.array(e, e.v("t_f32"), e.intOf(CHANNELS))));
        defs.add(e.let("i32xe", EcoBuzz.array(e, e.v("t_i32"), e.intOf(2 * CHANNELS))));
        defs.add(e.let("f64xe", EcoBuzz.array(e, e.v("t_f64"), e.intOf(2 * CHANNELS))));
        defs.add(e.let("f32xb", EcoBuzz.array(e, e.v("t_f32"), e.intOf(3 * CHANNELS))));
        defs.add(e.let("i32xt", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TRACKS + 1))));
        defs.add(e.let("f32xt", EcoBuzz.array(e, e.v("t_f32"), e.intOf(TRACKS + 1))));
        defs.add(e.let("tabs_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TABLES.size()))));
        defs.add(e.let("map_a_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TABLES_A.size()))));
        defs.add(e.let("map_c_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TABLES_C.size()))));
        defs.add(e.let("times_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(TIMES.size()))));
        defs.add(e.let("pool_t", EcoBuzz.array(e, e.v("t_f32"), e.intOf(tableBase(TABLES.size())))));
        defs.add(e.let("levmax_t", EcoBuzz.array(e, e.v("t_f32"), e.intOf(levelIndexBase(TABLES.size())))));
        defs.add(e.let("wave_t2", EcoBuzz.array(e, e.v("t_f32"), e.intOf(WAVE))));
        defs.add(e.let("iwave_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(WAVE))));
        var fields = new TList<SourceExpr>();
        fields.add(EcoBuzz.field(e, "waves", e.v("p_wave_table_t")));
        fields.add(EcoBuzz.field(e, "gval", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(e, "gact", EcoBuzz.array(e, e.v("t_i32"), e.intOf(NG))));
        fields.add(EcoBuzz.field(
                e, "tval", EcoBuzz.array(e, EcoBuzz.array(e, e.v("t_i32"), e.intOf(NT)), e.intOf(TRACKS))));
        for (var f : I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("t_i32")));
        fields.add(EcoBuzz.field(e, "inr_ai", EcoBuzz.array(e, e.v("t_f32"), e.intOf(8))));
        fields.add(EcoBuzz.field(e, "inr_acc", EcoBuzz.array(e, e.v("t_f32"), e.intOf(8))));
        fields.add(EcoBuzz.field(e, "pwmbuf1", EcoBuzz.array(e, e.v("t_i32"), e.intOf(EcoBuzz.MAX_BLOCK))));
        fields.add(EcoBuzz.field(e, "pwmbuf2", EcoBuzz.array(e, e.v("t_i32"), e.intOf(EcoBuzz.MAX_BLOCK))));
        for (var f : C_I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32xc")));
        for (var f : C_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xc")));
        for (var f : BQ_FIELDS) fields.add(EcoBuzz.field(e, "f_" + f, e.v("f32xb")));
        for (var f : ENV_I32_FIELDS) fields.add(EcoBuzz.field(e, "e_" + f, e.v("i32xe")));
        for (var f : ENV_F64_FIELDS) fields.add(EcoBuzz.field(e, "e_" + f, e.v("f64xe")));
        for (var f : T_I32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("i32xt")));
        for (var f : T_F32_FIELDS) fields.add(EcoBuzz.field(e, f, e.v("f32xt")));
        defs.add(e.let("state_t", EcoBuzz.struct(e, "inf_state", fields.toArray(SourceExpr[]::new))));
        defs.add(e.let("p_state_t", EcoBuzz.ptr(e, e.v("state_t"))));
        defs.add(e.let("init_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("tick_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"))));
        defs.add(e.let("work_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"))));
        defs.add(e.let("void0_fn_t", EcoBuzz.fnType(e, e.v("t_void"))));
        defs.add(e.let("fft_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("t_i32"))));
        defs.add(e.let(
                "make_fn_t",
                EcoBuzz.fnType(
                        e, e.v("t_void"), e.v("t_i32"), e.v("t_i32"), e.v("t_i32"), e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("level_fn_t", EcoBuzz.fnType(e, e.v("t_i32"), e.v("t_i32"), e.v("t_f32"))));
        defs.add(e.let("read_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_i32"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("pi_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let("pii_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("envproc_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("envnext_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"))));
        defs.add(e.let(
                "envset_fn_t",
                EcoBuzz.fnType(
                        e,
                        e.v("t_void"),
                        e.v("p_state_t"),
                        e.v("t_i32"),
                        e.v("t_f32"),
                        e.v("t_f32"),
                        e.v("t_f32"),
                        e.v("t_i32"),
                        e.v("t_f32"))));
        defs.add(e.let(
                "bqrbj_fn_t",
                EcoBuzz.fnType(
                        e,
                        e.v("t_void"),
                        e.v("p_state_t"),
                        e.v("t_i32"),
                        e.v("t_i32"),
                        e.v("t_f32"),
                        e.v("t_f32"),
                        e.v("t_f32"))));
        defs.add(e.let(
                "bqpeq_fn_t",
                EcoBuzz.fnType(
                        e,
                        e.v("t_void"),
                        e.v("p_state_t"),
                        e.v("t_i32"),
                        e.v("t_f32"),
                        e.v("t_f32"),
                        e.v("t_f32"),
                        e.v("t_f32"))));
        defs.add(e.let("bqproc_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_state_t"), e.v("t_i32"), e.v("t_f32"))));
        defs.add(e.let(
                "coeffs_fn_t",
                EcoBuzz.fnType(
                        e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"), e.v("t_i32"), e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("alloc_fn_t", EcoBuzz.fnType(e, e.v("t_i32"), e.v("p_state_t"))));
        defs.add(e.let(
                "playnote_fn_t",
                EcoBuzz.fnType(
                        e, e.v("t_void"), e.v("p_state_t"), e.v("t_i32"), e.v("t_i32"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("lfo_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_i32"), e.v("t_f32"))));
        defs.add(e.let(
                "chan_fn_t",
                EcoBuzz.fnType(e, e.v("t_void"), e.v("p_state_t"), e.v("t_pf32"), e.v("t_i32"), e.v("t_i32"))));
        defs.add(e.let("f1_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("f2_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"), e.v("t_f32"))));
        defs.add(e.let("d2_fn_t", EcoBuzz.fnType(e, e.v("t_f64"), e.v("t_f64"), e.v("t_f64"))));

        var main = new TList<SourceExpr>();
        main.add(e.let("zero", e.callM0(e.v("arg"), "zero")));
        Function<String, SourceExpr> typeOf = name -> {
            var fnType = FN_TYPES.getOpt(name);
            if (fnType.optIsSome()) return EcoBuzz.globalT(e, e.v(fnType.optUnwrap()));
            var globalType = GLOBAL_TYPES.getOpt(name);
            if (globalType.optIsSome()) return EcoBuzz.globalMemT(e, e.v(globalType.optUnwrap()));
            throw new IllegalArgumentException("no type for builder value " + name);
        };
        var partTables = new EcoBuzz.Part("tables", typeOf);
        var partDsp = new EcoBuzz.Part("dsp", typeOf);
        var partVoice = new EcoBuzz.Part("voice", typeOf);
        var body = partTables.body;
        for (var fn : new String[] {"sinf", "cosf", "expf", "logf", "fabsf", "tanf", "sqrtf", "floorf"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, e.v("ll"), fn, e.lowerV("f1_fn_t"))));
        }
        for (var fn : new String[] {"powf", "fmodf"}) {
            body.add(e.let(fn, EcoBuzz.externDef(e, e.v("ll"), fn, e.lowerV("f2_fn_t"))));
        }
        body.add(e.let("pow", EcoBuzz.externDef(e, e.v("ll"), "pow", e.lowerV("d2_fn_t"))));
        for (var g : new String[][] {
            {"pool", "pool_t"},
            {"levmax", "levmax_t"},
            {"basewave", "wave_t2"},
            {"sre", "wave_t2"},
            {"sim", "wave_t2"},
            {"lre", "wave_t2"},
            {"lim", "wave_t2"},
            {"gre", "wave_t2"},
            {"gim", "wave_t2"},
            {"fsin", "wave_t2"},
            {"fcos", "wave_t2"},
            {"fscr", "iwave_t"},
            {"isine", "iwave_t"}
        }) {
            body.add(e.let(
                    g[0],
                    e.call(
                            EcoBuzz.llvmL(e, "global_zero_fn"),
                            e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("inf_" + g[0])), e.p("ty", e.lowerV(g[1]))))));
        }
        body.add(e.let(
                "tables_ready",
                e.call(
                        EcoBuzz.llvmL(e, "global_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("inf_tables_ready")),
                                e.p("ty", e.lowerV("t_i32")),
                                e.p("init", e.n(0))))));

        var i = e.v("i");
        var fftBody = e.seq(
                e.let("inv", e.v("inverse")),
                e.ifElse(
                        ne(e, e.v("inv"), e.n(0)),
                        forRange(
                                e,
                                "i",
                                e.n(WAVE),
                                e.let("s", idx(e, lw(e, "fscr"), i)),
                                set(
                                        e,
                                        idx(e, lw(e, "gre"), i),
                                        mul(e, idx(e, lw(e, "lim"), e.v("s")), e.n(1.0 / WAVE))),
                                set(
                                        e,
                                        idx(e, lw(e, "gim"), i),
                                        mul(e, idx(e, lw(e, "lre"), e.v("s")), e.n(1.0 / WAVE)))),
                        forRange(
                                e,
                                "i",
                                e.n(WAVE),
                                e.let("s", idx(e, lw(e, "fscr"), i)),
                                set(e, idx(e, lw(e, "gre"), i), idx(e, lw(e, "lre"), e.v("s"))),
                                set(e, idx(e, lw(e, "gim"), i), idx(e, lw(e, "lim"), e.v("s"))))),
                forRange(
                        e,
                        "st",
                        e.n(11),
                        e.let("po", shl(e, EcoBuzz.i32(e, e.n(1)), e.v("st"))),
                        e.let("pno", shl(e, EcoBuzz.i32(e, e.n(1)), sub(e, EcoBuzz.i32(e, e.n(10)), e.v("st")))),
                        e.let("sh", sub(e, EcoBuzz.i32(e, e.n(10)), e.v("st"))),
                        forRange(
                                e,
                                "j",
                                e.v("pno"),
                                e.let("base", shl(e, e.v("j"), add(e, e.v("st"), e.n(1)))),
                                forRange(
                                        e,
                                        "k",
                                        e.v("po"),
                                        e.let("b1", add(e, e.v("base"), e.v("k"))),
                                        e.let("b2", add(e, e.v("b1"), e.v("po"))),
                                        e.let("r1re", idx(e, lw(e, "gre"), e.v("b1"))),
                                        e.let("r1im", idx(e, lw(e, "gim"), e.v("b1"))),
                                        e.let("r2re", idx(e, lw(e, "gre"), e.v("b2"))),
                                        e.let("r2im", idx(e, lw(e, "gim"), e.v("b2"))),
                                        e.let("w1", and(e, shl(e, e.v("b1"), e.v("sh")), e.n(WAVE - 1))),
                                        e.let("w2", and(e, shl(e, e.v("b2"), e.v("sh")), e.n(WAVE - 1))),
                                        e.let("c1", idx(e, lw(e, "fcos"), e.v("w1"))),
                                        e.let("s1", idx(e, lw(e, "fsin"), e.v("w1"))),
                                        e.let("c2", idx(e, lw(e, "fcos"), e.v("w2"))),
                                        e.let("s2", idx(e, lw(e, "fsin"), e.v("w2"))),
                                        set(
                                                e,
                                                idx(e, lw(e, "gre"), e.v("b1")),
                                                add(
                                                        e,
                                                        e.v("r1re"),
                                                        sub(
                                                                e,
                                                                mul(e, e.v("r2re"), e.v("c1")),
                                                                mul(e, e.v("r2im"), e.v("s1"))))),
                                        set(
                                                e,
                                                idx(e, lw(e, "gim"), e.v("b1")),
                                                add(
                                                        e,
                                                        e.v("r1im"),
                                                        add(
                                                                e,
                                                                mul(e, e.v("r2re"), e.v("s1")),
                                                                mul(e, e.v("r2im"), e.v("c1"))))),
                                        set(
                                                e,
                                                idx(e, lw(e, "gre"), e.v("b2")),
                                                add(
                                                        e,
                                                        e.v("r1re"),
                                                        sub(
                                                                e,
                                                                mul(e, e.v("r2re"), e.v("c2")),
                                                                mul(e, e.v("r2im"), e.v("s2"))))),
                                        set(
                                                e,
                                                idx(e, lw(e, "gim"), e.v("b2")),
                                                add(
                                                        e,
                                                        e.v("r1im"),
                                                        add(
                                                                e,
                                                                mul(e, e.v("r2re"), e.v("s2")),
                                                                mul(e, e.v("r2im"), e.v("c2")))))))),
                ifOnly(
                        e,
                        ne(e, e.v("inv"), e.n(0)),
                        forRange(
                                e,
                                "i",
                                e.n(WAVE),
                                e.let("tr", idx(e, lw(e, "gre"), i)),
                                set(e, idx(e, lw(e, "gre"), i), idx(e, lw(e, "gim"), i)),
                                set(e, idx(e, lw(e, "gim"), i), e.v("tr")))));
        body.add(e.let(
                "fft",
                EcoBuzz.funcDef(e, e.v("ll"), "inf_fft", new String[] {"inverse"}, e.lowerV("fft_fn_t"), fftBody)));

        var makeBody = e.seq(
                e.let("base", e.v("poolbase")),
                e.let("lb", e.v("levbase")),
                forRange(
                        e,
                        "i",
                        e.n(WAVE),
                        set(e, idx(e, lw(e, "pool"), add(e, e.v("base"), i)), idx(e, lw(e, "basewave"), i))),
                forRange(
                        e,
                        "i",
                        e.n(4),
                        set(
                                e,
                                idx(e, lw(e, "pool"), add(e, e.v("base"), add(e, i, e.n(WAVE)))),
                                idx(e, lw(e, "basewave"), i))),
                set(e, idx(e, lw(e, "levmax"), e.v("lb")), f32(e, e.n(0.25 / WAVE))),
                forRange(
                        e,
                        "i",
                        e.n(WAVE),
                        set(e, idx(e, lw(e, "lre"), i), idx(e, lw(e, "basewave"), i)),
                        set(e, idx(e, lw(e, "lim"), i), f32(e, e.n(0)))),
                call(e, "fft", EcoBuzz.i32(e, e.n(0))),
                forRange(
                        e,
                        "i",
                        e.n(WAVE),
                        set(e, idx(e, lw(e, "sre"), i), idx(e, lw(e, "gre"), i)),
                        set(e, idx(e, lw(e, "sim"), i), idx(e, lw(e, "gim"), i))),
                forRange(
                        e,
                        "l",
                        sub(e, e.v("count"), e.n(1)),
                        e.let("lv", add(e, e.v("l"), e.n(1))),
                        e.let("prev", idx(e, lw(e, "levmax"), add(e, e.v("lb"), e.v("l")))),
                        e.let("mx", mul(e, e.v("prev"), e.v("factor"))),
                        set(e, idx(e, lw(e, "levmax"), add(e, e.v("lb"), e.v("lv"))), e.v("mx")),
                        e.let(
                                "first",
                                i32(
                                        e,
                                        mul(
                                                e,
                                                mul(e, e.v("crisp"), f32(e, e.n(WAVE))),
                                                div(e, idx(e, lw(e, "levmax"), e.v("lb")), e.v("mx"))))),
                        e.let(
                                "cut",
                                e.ifElse(
                                        lt(e, e.v("first"), e.n(WAVE / 4)),
                                        e.v("first"),
                                        EcoBuzz.i32(e, e.n(WAVE / 4)))),
                        forRange(
                                e,
                                "i",
                                e.n(WAVE),
                                e.ifElse(
                                        ge(e, i, e.v("cut")),
                                        e.seq(
                                                set(e, idx(e, lw(e, "lre"), i), f32(e, e.n(0))),
                                                set(e, idx(e, lw(e, "lim"), i), f32(e, e.n(0)))),
                                        e.seq(
                                                set(e, idx(e, lw(e, "lre"), i), idx(e, lw(e, "sre"), i)),
                                                set(e, idx(e, lw(e, "lim"), i), idx(e, lw(e, "sim"), i))))),
                        call(e, "fft", EcoBuzz.i32(e, e.n(1))),
                        e.let(
                                "off",
                                add(
                                        e,
                                        e.v("base"),
                                        add(e, EcoBuzz.i32(e, e.n(LEVEL0)), mul(e, e.v("l"), e.n(LEVELN))))),
                        forRange(
                                e,
                                "i",
                                e.n(WAVE / 2),
                                set(
                                        e,
                                        idx(e, lw(e, "pool"), add(e, e.v("off"), i)),
                                        idx(e, lw(e, "gre"), mul(e, i, e.n(2))))),
                        forRange(
                                e,
                                "i",
                                e.n(4),
                                set(
                                        e,
                                        idx(e, lw(e, "pool"), add(e, e.v("off"), add(e, i, e.n(WAVE / 2)))),
                                        idx(e, lw(e, "pool"), add(e, e.v("off"), i))))));
        body.add(e.let(
                "make",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_make",
                        new String[] {"poolbase", "levbase", "count", "factor", "crisp"},
                        e.lowerV("make_fn_t"),
                        makeBody)));

        var tablesBody = new TList<SourceExpr>();
        tablesBody.add(set(e, lw(e, "tables_ready"), e.n(1)));
        tablesBody.add(forRange(
                e,
                "i",
                e.n(WAVE),
                e.let("v", EcoBuzz.i32(e, e.n(0))),
                forRange(
                        e,
                        "j",
                        e.n(11),
                        ifOnly(
                                e,
                                ne(e, and(e, i, shl(e, EcoBuzz.i32(e, e.n(1)), e.v("j"))), e.n(0)),
                                set(
                                        e,
                                        e.v("v"),
                                        add(
                                                e,
                                                e.v("v"),
                                                e.mb.operatorShr(
                                                        EcoBuzz.i32(e, e.n(WAVE)), add(e, e.v("j"), e.n(1))))))),
                set(e, idx(e, lw(e, "fscr"), i), e.v("v")),
                set(e, idx(e, lw(e, "fcos"), i), call(e, "cosf", mul(e, f32(e, i), e.n(2 * PI / WAVE)))),
                set(e, idx(e, lw(e, "fsin"), i), call(e, "sinf", mul(e, f32(e, i), e.n(2 * PI / WAVE)))),
                e.let("phs", div(e, f32(e, sub(e, i, e.n(1024))), f32(e, e.n(1024)))),
                set(
                        e,
                        idx(e, lw(e, "isine"), i),
                        i32(e, mul(e, sub(e, mul(e, mul(e, e.v("phs"), e.v("phs")), e.n(2)), e.n(1)), e.n(32768))))));
        var fi = f32(e, i);
        var tableWaves = new TMap<String, SourceExpr>();
        tableWaves.put("sin", mul(e, call(e, "sinf", mul(e, fi, e.n(PI / 1024))), e.n(32000)));
        tableWaves.put(
                "tri",
                mul(
                        e,
                        e.ifElse(
                                lt(e, i, e.n(512)),
                                div(e, fi, f32(e, e.n(512))),
                                e.ifElse(
                                        lt(e, i, e.n(1024)),
                                        sub(e, one(e), div(e, f32(e, sub(e, i, e.n(512))), f32(e, e.n(512)))),
                                        e.ifElse(
                                                lt(e, i, e.n(1536)),
                                                e.mb.operatorNeg(
                                                        div(e, f32(e, sub(e, i, e.n(1024))), f32(e, e.n(512)))),
                                                e.mb.operatorNeg(
                                                        sub(
                                                                e,
                                                                one(e),
                                                                div(
                                                                        e,
                                                                        f32(e, sub(e, i, e.n(1536))),
                                                                        f32(e, e.n(512)))))))),
                        e.n(32000)));
        var x1024 = div(e, fi, f32(e, e.n(1024)));
        tableWaves.put(
                "sps",
                mul(
                        e,
                        sub(
                                e,
                                add(
                                        e,
                                        add(
                                                e,
                                                mul(
                                                        e,
                                                        call(
                                                                e,
                                                                "fmodf",
                                                                add(e, mul(e, x1024, e.n(4)), one(e)),
                                                                f32(e, e.n(2))),
                                                        e.n(0.125)),
                                                mul(
                                                        e,
                                                        call(
                                                                e,
                                                                "fmodf",
                                                                add(e, mul(e, x1024, e.n(2)), one(e)),
                                                                f32(e, e.n(2))),
                                                        e.n(0.125))),
                                        mul(e, call(e, "fmodf", add(e, x1024, one(e)), f32(e, e.n(2))), e.n(0.75))),
                                one(e)),
                        e.n(32000)));
        tableWaves.put(
                "sp2",
                mul(
                        e,
                        sub(
                                e,
                                add(
                                        e,
                                        add(
                                                e,
                                                mul(
                                                        e,
                                                        call(
                                                                e,
                                                                "fmodf",
                                                                add(e, mul(e, x1024, e.n(4)), one(e)),
                                                                f32(e, e.n(2))),
                                                        e.n(0.125)),
                                                mul(
                                                        e,
                                                        call(
                                                                e,
                                                                "fmodf",
                                                                add(e, mul(e, x1024, e.n(2)), one(e)),
                                                                f32(e, e.n(2))),
                                                        e.n(0.275))),
                                        mul(e, call(e, "fmodf", add(e, x1024, one(e)), f32(e, e.n(2))), e.n(0.6))),
                                one(e)),
                        e.n(-32000)));
        tableWaves.put("saw", mul(e, sub(e, x1024, one(e)), e.n(32000)));
        tableWaves.put("sqr", e.ifElse(lt(e, i, e.n(1024)), f32(e, e.n(-32000)), f32(e, e.n(32000))));
        tableWaves.put(
                "spl",
                e.ifElse(lt(e, i, e.n(1024)), sub(e, f32(e, e.n(32000)), mul(e, x1024, e.n(32000))), f32(e, e.n(0))));
        tableWaves.put("smn", e.ifElse(lt(e, i, e.n(1024)), mul(e, x1024, e.n(32000)), f32(e, e.n(0))));
        tableWaves.put("nul", f32(e, e.n(0)));
        var im1024 = f32(e, sub(e, i, e.n(1024)));
        tableWaves.put(
                "fm1",
                mul(
                        e,
                        call(
                                e,
                                "sinf",
                                add(
                                        e,
                                        mul(e, im1024, e.n(PI / 2048)),
                                        mul(e, call(e, "cosf", mul(e, im1024, e.n(PI * 4 / 2048))), e.n(PI)))),
                        e.n(32000)));
        tableWaves.put(
                "xt1",
                mul(
                        e,
                        call(
                                e,
                                "sinf",
                                mul(e, mul(e, fi, e.n(4 * PI / 1024)), call(e, "sinf", mul(e, fi, e.n(PI / 1024))))),
                        e.n(32000)));
        tableWaves.put(
                "xt2",
                mul(
                        e,
                        call(
                                e,
                                "sinf",
                                mul(
                                        e,
                                        mul(e, fi, e.n(2 * PI / 1024)),
                                        add(
                                                e,
                                                call(e, "sinf", mul(e, fi, e.n(3 * PI / 1024))),
                                                call(e, "sinf", mul(e, fi, e.n(4 * PI / 1024)))))),
                        e.n(32000)));
        SourceExpr octSum = f32(e, e.n(0));
        for (int j = 1; j < 8; j++) octSum = add(e, octSum, call(e, "sinf", mul(e, fi, e.n((1 << j) * 2 * PI / 1024))));
        tableWaves.put("oct", mul(e, octSum, e.n(32000.0 / 7)));
        SourceExpr prtSum = f32(e, e.n(0));
        for (int j = 1; j <= 3; j++) prtSum = add(e, prtSum, call(e, "sinf", mul(e, fi, e.n(j * 2 * PI / 1024))));
        tableWaves.put("prt", mul(e, prtSum, e.n(32000.0 / 3)));
        SourceExpr pr2Sum = f32(e, e.n(0));
        for (int j = 3; j <= 6; j++) pr2Sum = add(e, pr2Sum, call(e, "sinf", mul(e, fi, e.n(j * 2 * PI / 1024))));
        tableWaves.put("pr2", mul(e, pr2Sum, e.n(32000.0 / 3)));
        SourceExpr pr3Sum = f32(e, e.n(0));
        for (int part : new int[] {1, 2, 3, 4, 6, 8, 12, 16}) {
            pr3Sum = add(e, pr3Sum, call(e, "sinf", mul(e, fi, e.n(part * 2 * PI / 1024))));
        }
        tableWaves.put("pr3", mul(e, pr3Sum, e.n(32000.0 / 3)));
        var seg = and(e, i, e.n(255));
        var up = div(e, f32(e, seg), f32(e, e.n(256)));
        tableWaves.put(
                "hex",
                mul(
                        e,
                        e.ifElse(
                                lt(e, i, e.n(256)),
                                up,
                                e.ifElse(
                                        lt(e, i, e.n(768)),
                                        one(e),
                                        e.ifElse(
                                                lt(e, i, e.n(1024)),
                                                sub(e, one(e), up),
                                                e.ifElse(
                                                        lt(e, i, e.n(1280)),
                                                        e.mb.operatorNeg(up),
                                                        e.ifElse(
                                                                lt(e, i, e.n(1792)),
                                                                f32(e, e.n(-1)),
                                                                e.mb.operatorNeg(sub(e, one(e), up))))))),
                        e.n(32000)));
        for (int t = 0; t < TABLES.size(); t++) {
            var tb = TABLES.get(t);
            tablesBody.add(forRange(
                    e,
                    "i",
                    e.n(WAVE),
                    set(
                            e,
                            idx(e, lw(e, "basewave"), i),
                            tableWaves.getOpt(tb.name()).optUnwrap())));
            tablesBody.add(call(
                    e,
                    "make",
                    EcoBuzz.i32(e, e.n(tableBase(t))),
                    EcoBuzz.i32(e, e.n(levelIndexBase(t))),
                    EcoBuzz.i32(e, e.n(levelCount(tb))),
                    f32(e, e.n(tb.factor())),
                    f32(e, e.n(2.0 / tb.factor()))));
        }
        body.add(e.let(
                "tables",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_tables",
                        new String[] {},
                        e.lowerV("void0_fn_t"),
                        ifOnly(e, eq(e, lw(e, "tables_ready"), e.n(0)), tablesBody.toArray(SourceExpr[]::new)))));

        var ch = e.v("ch");
        var t = e.v("t");
        var ei = e.v("ei");
        var bi = e.v("bi");
        for (var tabc : new String[][] {{"tab_base", "tabs_t"}, {"tab_lev", "tabs_t"}, {"tab_count", "tabs_t"}}) {
            var vals = new TList<SourceExpr>();
            for (int k = 0; k < TABLES.size(); k++) {
                vals.add(e.intOf(
                        switch (tabc[0]) {
                            case "tab_base" -> tableBase(k);
                            case "tab_lev" -> levelIndexBase(k);
                            default -> levelCount(TABLES.get(k));
                        }));
            }
            body.add(constArray(e, tabc[0], tabc[1], vals));
        }
        body.add(constArray(e, "map_a", "map_a_t", intList(e, TABLES_A)));
        body.add(constArray(e, "map_b", "map_a_t", intList(e, TABLES_B)));
        body.add(constArray(e, "map_c", "map_c_t", intList(e, TABLES_C)));
        body.add(constArray(e, "times", "times_t", intList(e, TIMES)));

        body = partDsp.body;
        body.add(e.let(
                "level",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_level",
                        new String[] {"tab", "rate"},
                        e.lowerV("level_fn_t"),
                        e.seq(
                                e.let("count", idx(e, lw(e, "tab_count"), e.v("tab"))),
                                e.let("lb", idx(e, lw(e, "tab_lev"), e.v("tab"))),
                                e.ret(e.mb.labeled(
                                        "lev",
                                        e.seq(
                                                forRange(
                                                        e,
                                                        "i",
                                                        e.v("count"),
                                                        ifOnly(
                                                                e,
                                                                le(
                                                                        e,
                                                                        e.v("rate"),
                                                                        idx(e, lw(e, "levmax"), add(e, e.v("lb"), i))),
                                                                e.mb.return_(
                                                                        "lev",
                                                                        e.ifElse(
                                                                                gt(e, i, e.n(0)),
                                                                                sub(e, i, e.n(1)),
                                                                                EcoBuzz.i32(e, e.n(0)))))),
                                                e.mb.return_("lev", sub(e, e.v("count"), e.n(1))))))))));
        body.add(e.let(
                "wave_read",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_wave_read",
                        new String[] {"off", "bits", "pos"},
                        e.lowerV("read_fn_t"),
                        e.seq(
                                e.let("pos2", and(e, e.v("pos"), e.n(0x7FFFFFFF))),
                                e.let("sh", sub(e, EcoBuzz.i32(e, e.n(31)), e.v("bits"))),
                                e.let("wp", e.mb.operatorShr(e.v("pos2"), e.v("sh"))),
                                e.let("rem", sub(e, e.v("pos2"), shl(e, e.v("wp"), e.v("sh")))),
                                e.let(
                                        "mask",
                                        e.ifElse(
                                                eq(e, e.v("bits"), e.n(11)),
                                                EcoBuzz.i32(e, e.n(WAVE - 1)),
                                                EcoBuzz.i32(e, e.n(WAVE / 2 - 1)))),
                                e.let(
                                        "mult",
                                        e.ifElse(
                                                eq(e, e.v("bits"), e.n(11)),
                                                f32(e, e.n(Math.pow(2, -20))),
                                                f32(e, e.n(Math.pow(2, -21))))),
                                e.let("d0", idx(e, lw(e, "pool"), add(e, e.v("off"), and(e, e.v("wp"), e.v("mask"))))),
                                e.let(
                                        "d1",
                                        idx(
                                                e,
                                                lw(e, "pool"),
                                                add(e, e.v("off"), and(e, add(e, e.v("wp"), e.n(1)), e.v("mask"))))),
                                e.ret(add(
                                        e,
                                        e.v("d0"),
                                        mul(
                                                e,
                                                sub(e, e.v("d1"), e.v("d0")),
                                                mul(e, f32(e, e.v("rem")), e.v("mult")))))))));

        body.add(e.let(
                "env_set",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_env_set",
                        new String[] {"p", "ei", "att", "dec", "sl", "sust", "rel"},
                        e.lowerV("envset_fn_t"),
                        e.seq(
                                set(e, env(e, "att", ei), maxi(e, i32(e, mul(e, e.v("att"), e.n(RATE))), 1)),
                                set(e, env(e, "sus", ei), e.v("sust")),
                                set(e, env(e, "dec", ei), maxi(e, i32(e, mul(e, e.v("dec"), e.n(RATE))), 1)),
                                set(e, env(e, "rel", ei), maxi(e, i32(e, mul(e, e.v("rel"), e.n(RATE))), 1)),
                                set(e, env(e, "sustlevel", ei), f64(e, e.v("sl")))))));
        body.add(e.let(
                "env_noteon",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_env_noteon",
                        new String[] {"p", "ei"},
                        e.lowerV("pi_fn_t"),
                        e.seq(
                                set(e, env(e, "stage", ei), env(e, "att", ei)),
                                set(e, env(e, "last", ei), e.n(0)),
                                set(e, env(e, "start", ei), e.n(0)),
                                set(
                                        e,
                                        env(e, "series", ei),
                                        div(
                                                e,
                                                sub(e, f64(e, e.n(1)), env(e, "silence", ei)),
                                                f64(e, env(e, "stage", ei)))),
                                set(e, env(e, "state", ei), e.n(0)),
                                set(e, env(e, "time", ei), e.n(0))))));
        body.add(e.let(
                "env_notefast",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_env_notefast",
                        new String[] {"p", "ei"},
                        e.lowerV("pi_fn_t"),
                        e.seq(
                                e.let("st", env(e, "state", ei)),
                                ifOnly(
                                        e,
                                        and(e, ne(e, e.v("st"), e.n(4)), ne(e, e.v("st"), e.n(-1))),
                                        set(e, env(e, "rel", ei), e.n(1024)),
                                        set(e, env(e, "stage", ei), e.n(1024)),
                                        set(e, env(e, "time", ei), e.n(0)),
                                        set(e, env(e, "state", ei), e.n(-1)),
                                        set(e, env(e, "start", ei), env(e, "last", ei)),
                                        set(e, env(e, "sustlevel", ei), env(e, "last", ei)),
                                        set(
                                                e,
                                                env(e, "series", ei),
                                                div(e, e.mb.operatorNeg(env(e, "start", ei)), e.n(1024))))))));
        body.add(e.let(
                "env_noteoff",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_env_noteoff",
                        new String[] {"p", "ei"},
                        e.lowerV("pi_fn_t"),
                        e.seq(
                                e.let("st", env(e, "state", ei)),
                                e.mb.labeled(
                                        "off",
                                        ifOnly(
                                                e,
                                                and(e, lt(e, e.v("st"), e.n(3)), ne(e, e.v("st"), e.n(-1))),
                                                ifOnly(
                                                        e,
                                                        lt(e, e.v("st"), e.n(2)),
                                                        e.ifElse(
                                                                ge(e, env(e, "last", ei), env(e, "sustlevel", ei)),
                                                                e.seq(
                                                                        set(e, env(e, "sus", ei), e.n(0)),
                                                                        set(e, env(e, "time", ei), e.n(0)),
                                                                        set(e, env(e, "stage", ei), e.n(256)),
                                                                        set(e, env(e, "start", ei), env(e, "last", ei)),
                                                                        set(
                                                                                e,
                                                                                env(e, "series", ei),
                                                                                call(
                                                                                        e,
                                                                                        "pow",
                                                                                        div(
                                                                                                e,
                                                                                                env(e, "sustlevel", ei),
                                                                                                env(e, "last", ei)),
                                                                                        f64(e, e.n(1.0 / 256)))),
                                                                        set(e, env(e, "state", ei), e.n(1)),
                                                                        e.mb.returnLabel("off")),
                                                                set(e, env(e, "sustlevel", ei), env(e, "last", ei)))),
                                                set(e, env(e, "time", ei), e.n(0)),
                                                set(e, env(e, "stage", ei), env(e, "rel", ei)),
                                                set(e, env(e, "start", ei), env(e, "last", ei)),
                                                set(
                                                        e,
                                                        env(e, "series", ei),
                                                        call(
                                                                e,
                                                                "pow",
                                                                div(e, env(e, "silence", ei), env(e, "sustlevel", ei)),
                                                                div(e, f64(e, e.n(1)), f64(e, env(e, "rel", ei))))),
                                                set(e, env(e, "state", ei), e.n(3))))))));
        body.add(e.let(
                "env_process",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_env_process",
                        new String[] {"p", "ei", "dt"},
                        e.lowerV("envproc_fn_t"),
                        e.seq(
                                e.let(
                                        "v",
                                        e.ifElse(
                                                le(e, env(e, "state", ei), e.n(0)),
                                                add(
                                                        e,
                                                        env(e, "start", ei),
                                                        mul(e, f64(e, env(e, "time", ei)), env(e, "series", ei))),
                                                mul(
                                                        e,
                                                        env(e, "start", ei),
                                                        call(
                                                                e,
                                                                "pow",
                                                                env(e, "series", ei),
                                                                f64(e, env(e, "time", ei)))))),
                                set(e, env(e, "time", ei), add(e, env(e, "time", ei), e.v("dt"))),
                                envStage(
                                        e,
                                        -1,
                                        set(e, env(e, "state", ei), e.n(4)),
                                        set(e, env(e, "start", ei), e.n(0)),
                                        set(e, env(e, "series", ei), e.n(1)),
                                        set(e, env(e, "time", ei), e.n(0)),
                                        set(e, env(e, "stage", ei), e.n(1000000))),
                                envStage(
                                        e,
                                        0,
                                        set(e, env(e, "time", ei), sub(e, env(e, "time", ei), env(e, "stage", ei))),
                                        set(e, env(e, "state", ei), e.n(1)),
                                        set(e, env(e, "stage", ei), env(e, "dec", ei)),
                                        set(e, env(e, "start", ei), e.n(1)),
                                        set(
                                                e,
                                                env(e, "series", ei),
                                                call(
                                                        e,
                                                        "pow",
                                                        env(e, "sustlevel", ei),
                                                        div(e, f64(e, e.n(1)), f64(e, env(e, "dec", ei)))))),
                                envStage(
                                        e,
                                        1,
                                        set(e, env(e, "time", ei), sub(e, env(e, "time", ei), env(e, "stage", ei))),
                                        set(e, env(e, "state", ei), e.n(2)),
                                        set(e, env(e, "start", ei), env(e, "sustlevel", ei)),
                                        set(e, env(e, "series", ei), e.n(1)),
                                        set(e, env(e, "stage", ei), env(e, "sus", ei))),
                                envStage(
                                        e,
                                        2,
                                        set(e, env(e, "time", ei), sub(e, env(e, "time", ei), env(e, "stage", ei))),
                                        set(e, env(e, "state", ei), e.n(3)),
                                        set(e, env(e, "stage", ei), env(e, "rel", ei)),
                                        set(e, env(e, "start", ei), env(e, "sustlevel", ei)),
                                        set(
                                                e,
                                                env(e, "series", ei),
                                                call(
                                                        e,
                                                        "pow",
                                                        div(e, env(e, "silence", ei), env(e, "sustlevel", ei)),
                                                        div(e, f64(e, e.n(1)), f64(e, env(e, "rel", ei)))))),
                                envStage(
                                        e,
                                        3,
                                        set(e, env(e, "start", ei), env(e, "silence", ei)),
                                        set(e, env(e, "state", ei), e.n(4)),
                                        set(e, env(e, "stage", ei), e.n(10000)),
                                        set(e, env(e, "time", ei), e.n(0))),
                                set(
                                        e,
                                        env(e, "last", ei),
                                        e.ifElse(
                                                lt(e, env(e, "state", ei), e.n(1)),
                                                add(
                                                        e,
                                                        env(e, "start", ei),
                                                        mul(e, env(e, "series", ei), f64(e, env(e, "time", ei)))),
                                                mul(
                                                        e,
                                                        env(e, "start", ei),
                                                        call(
                                                                e,
                                                                "pow",
                                                                env(e, "series", ei),
                                                                f64(e, env(e, "time", ei)))))),
                                e.ret(f32(e, e.v("v")))))));
        body.add(e.let(
                "env_next",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_env_next",
                        new String[] {"p", "ei"},
                        e.lowerV("envnext_fn_t"),
                        e.ret(e.mb.labeled(
                                "nxt",
                                e.seq(
                                        ifOnly(
                                                e,
                                                lt(e, env(e, "time", ei), env(e, "stage", ei)),
                                                e.let("v", f32(e, env(e, "last", ei))),
                                                set(
                                                        e,
                                                        env(e, "last", ei),
                                                        e.ifElse(
                                                                lt(e, env(e, "state", ei), e.n(1)),
                                                                add(e, env(e, "last", ei), env(e, "series", ei)),
                                                                mul(e, env(e, "last", ei), env(e, "series", ei)))),
                                                set(e, env(e, "time", ei), add(e, env(e, "time", ei), e.n(1))),
                                                e.mb.return_("nxt", e.v("v"))),
                                        e.mb.return_(
                                                "nxt",
                                                call(e, "env_process", e.v("p"), ei, EcoBuzz.i32(e, e.n(1))))))))));

        body.add(e.let(
                "bq_rbj",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_bq_rbj",
                        new String[] {"p", "bi", "kind", "fc", "q", "gain"},
                        e.lowerV("bqrbj_fn_t"),
                        e.seq(
                                e.let("omega", mul(e, e.v("fc"), e.n(2 * PI / RATE))),
                                e.let("sn", call(e, "sinf", e.v("omega"))),
                                e.let("cs", call(e, "cosf", e.v("omega"))),
                                e.let("alpha", div(e, e.v("sn"), mul(e, e.v("q"), e.n(2)))),
                                e.let("inv", div(e, f32(e, e.n(1)), add(e, e.v("alpha"), e.n(1)))),
                                e.let("gi", mul(e, e.v("gain"), e.v("inv"))),
                                e.let("k", e.v("kind")),
                                ifOnly(
                                        e,
                                        eq(e, e.v("k"), e.n(0)),
                                        set(
                                                e,
                                                bq(e, "b0", bi),
                                                mul(e, mul(e, e.v("gi"), sub(e, f32(e, e.n(1)), e.v("cs"))), e.n(0.5))),
                                        set(e, bq(e, "b1", bi), mul(e, e.v("gi"), sub(e, f32(e, e.n(1)), e.v("cs")))),
                                        set(
                                                e,
                                                bq(e, "b2", bi),
                                                mul(
                                                        e,
                                                        mul(e, e.v("gi"), sub(e, f32(e, e.n(1)), e.v("cs"))),
                                                        e.n(0.5)))),
                                ifOnly(
                                        e,
                                        eq(e, e.v("k"), e.n(1)),
                                        set(
                                                e,
                                                bq(e, "b0", bi),
                                                mul(e, mul(e, e.v("gi"), add(e, e.v("cs"), e.n(1))), e.n(0.5))),
                                        set(
                                                e,
                                                bq(e, "b1", bi),
                                                e.mb.operatorNeg(mul(e, e.v("gi"), add(e, e.v("cs"), e.n(1))))),
                                        set(
                                                e,
                                                bq(e, "b2", bi),
                                                mul(e, mul(e, e.v("gi"), add(e, e.v("cs"), e.n(1))), e.n(0.5)))),
                                ifOnly(
                                        e,
                                        eq(e, e.v("k"), e.n(2)),
                                        set(e, bq(e, "b0", bi), mul(e, e.v("gi"), e.v("alpha"))),
                                        set(e, bq(e, "b1", bi), e.n(0)),
                                        set(e, bq(e, "b2", bi), e.mb.operatorNeg(mul(e, e.v("gi"), e.v("alpha"))))),
                                ifOnly(
                                        e,
                                        eq(e, e.v("k"), e.n(3)),
                                        set(e, bq(e, "b0", bi), e.v("gi")),
                                        set(e, bq(e, "b1", bi), mul(e, mul(e, e.v("gi"), e.v("cs")), e.n(-2))),
                                        set(e, bq(e, "b2", bi), e.v("gi"))),
                                set(e, bq(e, "a1", bi), mul(e, mul(e, e.v("cs"), e.v("inv")), e.n(-2))),
                                set(e, bq(e, "a2", bi), mul(e, sub(e, f32(e, e.n(1)), e.v("alpha")), e.v("inv")))))));
        body.add(e.let(
                "bq_peq",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_bq_peq",
                        new String[] {"p", "bi", "fc", "q", "vv", "gain"},
                        e.lowerV("bqpeq_fn_t"),
                        e.seq(
                                e.let("omega", mul(e, e.v("fc"), e.n(2 * PI / RATE))),
                                e.let("k", call(e, "tanf", mul(e, e.v("omega"), e.n(0.5)))),
                                e.let("kk", mul(e, e.v("k"), e.v("k"))),
                                e.let("vkdq", div(e, mul(e, e.v("vv"), e.v("k")), e.v("q"))),
                                e.let(
                                        "oda0",
                                        div(
                                                e,
                                                f32(e, e.n(1)),
                                                add(e, add(e, div(e, e.v("k"), e.v("q")), e.v("kk")), e.n(1)))),
                                e.let("go", mul(e, e.v("gain"), e.v("oda0"))),
                                set(
                                        e,
                                        bq(e, "b0", bi),
                                        mul(e, e.v("go"), add(e, add(e, e.v("vkdq"), e.v("kk")), e.n(1)))),
                                set(e, bq(e, "b1", bi), mul(e, e.v("go"), mul(e, sub(e, e.v("kk"), e.n(1)), e.n(2)))),
                                set(
                                        e,
                                        bq(e, "b2", bi),
                                        mul(e, e.v("go"), add(e, sub(e, e.v("kk"), e.v("vkdq")), e.n(1)))),
                                set(e, bq(e, "a1", bi), mul(e, e.v("oda0"), mul(e, sub(e, e.v("kk"), e.n(1)), e.n(2)))),
                                set(
                                        e,
                                        bq(e, "a2", bi),
                                        mul(
                                                e,
                                                e.v("oda0"),
                                                add(e, sub(e, e.v("kk"), div(e, e.v("k"), e.v("q"))), e.n(1))))))));
        body.add(e.let(
                "bq_nothing",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_bq_nothing",
                        new String[] {"p", "bi"},
                        e.lowerV("pi_fn_t"),
                        e.seq(
                                set(e, bq(e, "b0", bi), e.n(1)),
                                set(e, bq(e, "b1", bi), e.n(0)),
                                set(e, bq(e, "b2", bi), e.n(0)),
                                set(e, bq(e, "a1", bi), e.n(0)),
                                set(e, bq(e, "a2", bi), e.n(0))))));
        body.add(e.let(
                "bq_proc",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_bq_proc",
                        new String[] {"p", "bi", "x"},
                        e.lowerV("bqproc_fn_t"),
                        e.seq(
                                e.let(
                                        "y",
                                        sub(
                                                e,
                                                sub(
                                                        e,
                                                        add(
                                                                e,
                                                                add(
                                                                        e,
                                                                        mul(e, bq(e, "b0", bi), e.v("x")),
                                                                        mul(e, bq(e, "b1", bi), bq(e, "x1", bi))),
                                                                mul(e, bq(e, "b2", bi), bq(e, "x2", bi))),
                                                        mul(e, bq(e, "a1", bi), bq(e, "y1", bi))),
                                                mul(e, bq(e, "a2", bi), bq(e, "y2", bi)))),
                                set(e, bq(e, "y2", bi), bq(e, "y1", bi)),
                                set(e, bq(e, "y1", bi), e.v("y")),
                                set(e, bq(e, "x2", bi), bq(e, "x1", bi)),
                                set(e, bq(e, "x1", bi), e.v("x")),
                                e.ret(e.v("y"))))));

        var coeffsOut = new TList<SourceExpr>();
        {
            var cut = e.v("cutoff");
            var res = e.v("res");
            var thev = ATTR_THEVIDERNESS / 50.0;
            coeffsOut.add(e.let("b0", mul(e, e.v("ch"), e.n(3))));
            coeffsOut.add(e.let("b1", add(e, e.v("b0"), e.n(1))));
            coeffsOut.add(e.let("b2", add(e, e.v("b0"), e.n(2))));
            var bq0 = e.v("b0");
            var bq1 = e.v("b1");
            var bq2 = e.v("b2");
            var cf = e.v("cf");
            var q = e.v("q");
            var one = f32(e, e.n(1));
            var sr = e.v("sr");
            SourceExpr cf132 = cutoffHz(e, cut, 132, 33, 20000);
            SourceExpr cf66 = cutoffHz(e, cut, 66, 33, 20000);
            SourceExpr cfInv = cutoffHz(e, sub(e, f32(e, e.n(240)), cut), 132, 33, 20000);
            BiFunction<SourceExpr, Double, SourceExpr> scaleRes =
                    (c, denom) -> e.let("sr", call(e, "powf", div(e, c, f32(e, e.n(denom))), f32(e, e.n(thev))));
            BiFunction<Double, Double, SourceExpr> qOf =
                    (base, k) -> e.let("q", add(e, mul(e, mul(e, res, sr), e.n(k / 240.0)), e.n(base)));
            coeffsOut.add(filterType(
                    e,
                    0,
                    cf132,
                    scaleRes.apply(cf, 20000.0),
                    qOf.apply(0.707, 7.0),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            div(e, cf, e.n(3)),
                            q,
                            div(e, f32(e, e.n(Math.sqrt(0.707))), call(e, "sqrtf", q))),
                    rbj(e, "lpf", bq1, mul(e, cf, e.n(2.0 / 3)), div(e, q, e.n(2)), one),
                    rbj(e, "lpf", bq2, cf, div(e, q, e.n(3)), one)));
            coeffsOut.add(filterType(
                    e,
                    1,
                    cutoffHz(e, cut, 132, 33, 16000),
                    scaleRes.apply(cf, 22000.0),
                    qOf.apply(1.5, 10.6),
                    e.let("sep", add(e, mul(e, res, e.n(0.6 / 240)), e.n(0.05))),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            cf,
                            q,
                            div(e, f32(e, e.n(0.3)), call(e, "powf", div(e, q, e.n(2.5)), f32(e, e.n(0.05))))),
                    rbj(e, "lpf", bq1, mul(e, cf, sub(e, one, e.v("sep"))), q, one),
                    rbj(e, "lpf", bq2, minf(e, mul(e, cf, add(e, e.v("sep"), e.n(1))), f32(e, e.n(21000))), q, one)));
            coeffsOut.add(filterType(
                    e,
                    2,
                    cf132,
                    scaleRes.apply(cf, 20000.0),
                    qOf.apply(0.71, 10.0),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            cf,
                            q,
                            div(e, f32(e, e.n(0.6)), call(e, "powf", maxf(e, q, one), f32(e, e.n(1.7))))),
                    rbj(e, "lpf", bq1, cf, q, one),
                    rbj(e, "lpf", bq2, cf, q, one)));
            coeffsOut.add(filterType(
                    e,
                    3,
                    cf132,
                    scaleRes.apply(cf, 21000.0),
                    qOf.apply(1.0, 10.0),
                    rbj(e, "lpf", bq0, cf, f32(e, e.n(0.707)), f32(e, e.n(0.5))),
                    rbj(e, "lpf", bq1, cf, f32(e, e.n(0.707)), one),
                    peq(e, bq2, cf, mul(e, q, e.n(4)), mul(e, q, e.n(2)), one)));
            coeffsOut.add(filterType(
                    e,
                    4,
                    cf132,
                    scaleRes.apply(cf, 20000.0),
                    qOf.apply(0.71, 5.0),
                    rbj(e, "lpf", bq0, cf, q, div(e, f32(e, e.n(0.3)), maxf(e, mul(e, call(e, "sqrtf", q), q), one))),
                    rbj(e, "lpf", bq1, cf, q, one),
                    peq(
                            e,
                            bq2,
                            div(e, cf, e.n(2)),
                            add(e, mul(e, sub(e, q, e.n(0.7)), e.n(3)), e.n(1)),
                            add(e, mul(e, sub(e, q, e.n(0.7)), e.n(8)), e.n(1)),
                            one)));
            coeffsOut.add(filterType(
                    e,
                    5,
                    cf132,
                    scaleRes.apply(cf, 20000.0),
                    qOf.apply(0.71, 5.0),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            div(e, cf, e.n(1.41)),
                            q,
                            div(e, f32(e, e.n(0.6)), maxf(e, mul(e, call(e, "sqrtf", q), q), one))),
                    rbj(e, "lpf", bq1, minf(e, mul(e, cf, e.n(1.41)), f32(e, e.n(22000))), q, one),
                    peq(e, bq2, cf, div(e, f32(e, e.n(16)), q), mul(e, q, e.n(4)), one)));
            coeffsOut.add(filterType(
                    e,
                    6,
                    cf132,
                    scaleRes.apply(cf, 20000.0),
                    qOf.apply(0.71, 5.0),
                    rbj(e, "lpf", bq0, cf, q, div(e, f32(e, e.n(0.8)), maxf(e, q, one))),
                    rbj(e, "lpf", bq1, cf, q, one),
                    rbj(e, "brf", bq2, cf, q, one)));
            coeffsOut.add(filterType(
                    e,
                    7,
                    cf132,
                    e.let("q", add(e, mul(e, sub(e, f32(e, e.n(240)), res), e.n(4.0 / 240)), e.n(1))),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            cf,
                            f32(e, e.n(1.007)),
                            div(e, f32(e, e.n(0.8)), maxf(e, call(e, "sqrtf", q), one))),
                    rbj(e, "brf", bq1, mul(e, cf, e.n(0.707)), div(e, q, e.n(2)), one),
                    rbj(e, "brf", bq2, cf, div(e, q, e.n(2)), one)));
            coeffsOut.add(filterType(
                    e,
                    8,
                    cf132,
                    scaleRes.apply(cf, 22000.0),
                    qOf.apply(0.71, 6.0),
                    e.let("q3", add(e, mul(e, sub(e, q, e.n(0.7)), e.n(3)), e.n(1))),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            cf,
                            mul(e, q, e.n(2)),
                            div(e, f32(e, e.n(0.3)), maxf(e, call(e, "sqrtf", q), one))),
                    peq(e, bq1, div(e, cf, e.n(2)), e.v("q3"), e.v("q3"), one),
                    peq(e, bq2, div(e, cf, e.n(4)), e.v("q3"), e.v("q3"), one)));
            coeffsOut.add(filterType(
                    e,
                    9,
                    cf132,
                    scaleRes.apply(cf, 20000.0),
                    qOf.apply(0.71, 6.0),
                    e.let("q2", add(e, mul(e, sub(e, q, e.n(0.7)), e.n(2)), e.n(1))),
                    e.let("q3", add(e, mul(e, sub(e, q, e.n(0.7)), e.n(3)), e.n(1))),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            cf,
                            mul(e, q, e.n(2)),
                            div(e, f32(e, e.n(0.15)), maxf(e, call(e, "sqrtf", q), one))),
                    peq(e, bq1, mul(e, cf, e.n(0.75)), e.v("q2"), e.v("q3"), one),
                    peq(e, bq2, div(e, cf, e.n(2)), e.v("q2"), e.v("q3"), one)));
            coeffsOut.add(filterType(
                    e,
                    10,
                    cf132,
                    scaleRes.apply(cf, 20000.0),
                    e.let("q", add(e, mul(e, sr, e.n(6.0 * 120 / 240)), e.n(0.71))),
                    e.let("q2", add(e, mul(e, sub(e, q, e.n(0.7)), e.n(2)), e.n(1))),
                    e.let("q3", add(e, mul(e, sub(e, q, e.n(0.7)), e.n(3)), e.n(1))),
                    e.let("ex", add(e, mul(e, res, e.n(-1.0 / 240)), e.n(1.5))),
                    e.let(
                            "sc1",
                            call(
                                    e,
                                    "powf",
                                    minf(e, add(e, mul(e, cut, e.n(0.2 / 240)), e.n(0.33)), f32(e, e.n(0.89))),
                                    e.v("ex"))),
                    e.let(
                            "sc2",
                            call(
                                    e,
                                    "powf",
                                    minf(e, add(e, mul(e, cut, e.n(0.1 / 240)), e.n(0.14)), f32(e, e.n(0.9))),
                                    e.v("ex"))),
                    rbj(
                            e,
                            "lpf",
                            bq0,
                            cf,
                            mul(e, q, e.n(2)),
                            div(e, f32(e, e.n(0.2)), maxf(e, call(e, "sqrtf", q), one))),
                    peq(e, bq1, mul(e, cf, e.v("sc1")), e.v("q2"), e.v("q3"), one),
                    peq(e, bq2, mul(e, cf, e.v("sc2")), e.v("q2"), e.v("q3"), one)));
            SourceExpr spacing = e.let(
                    "spacing",
                    call(
                            e,
                            "powf",
                            add(e, mul(e, sub(e, f32(e, e.n(240)), res), e.n(3.0 / 240)), e.n(1.3)),
                            sub(e, one, mul(e, cf, e.n(1.0 / 20000)))));
            coeffsOut.add(filterType(
                    e,
                    11,
                    cfInv,
                    e.let("q", add(e, mul(e, res, e.n(0.6 / 240)), e.n(0.1))),
                    spacing,
                    rbj(e, "brf", bq0, cf, q, one),
                    rbj(e, "brf", bq1, div(e, cf, e.v("spacing")), q, one),
                    rbj(e, "brf", bq2, minf(e, mul(e, cf, e.v("spacing")), f32(e, e.n(21000))), q, one)));
            coeffsOut.add(filterType(
                    e,
                    12,
                    cf66,
                    e.let("q", add(e, mul(e, res, e.n(2.6 / 240)), e.n(0.71))),
                    spacing,
                    rbj(e, "hpf", bq0, cf, q, div(e, f32(e, e.n(0.71)), call(e, "powf", q, f32(e, e.n(0.7))))),
                    rbj(e, "hpf", bq1, div(e, cf, e.v("spacing")), q, one),
                    rbj(e, "hpf", bq2, minf(e, mul(e, cf, e.v("spacing")), f32(e, e.n(21000))), q, one)));
            coeffsOut.add(filterType(
                    e,
                    13,
                    cf66,
                    scaleRes.apply(cf, 20000.0),
                    qOf.apply(0.1, 2.6),
                    rbj(e, "bpf", bq0, cf, q, div(e, call(e, "powf", q, f32(e, e.n(0.7))), e.n(1.7))),
                    rbj(e, "bpf", bq1, mul(e, cf, e.n(0.9)), q, one),
                    rbj(e, "bpf", bq2, minf(e, mul(e, cf, e.n(1.01)), f32(e, e.n(21000))), q, one)));
            coeffsOut.add(filterType(
                    e,
                    14,
                    cf132,
                    e.let("q", add(e, mul(e, res, e.n(9.6 / 240)), e.n(2.1))),
                    peq(e, bq0, div(e, cf, e.n(4)), one, q, div(e, f32(e, e.n(0.25)), call(e, "sqrtf", q))),
                    peq(e, bq1, div(e, cf, e.n(2)), f32(e, e.n(2)), div(e, one, q), one),
                    peq(e, bq2, cf, one, q, one)));
            for (int ft = 15; ft <= 16; ft++) {
                double[] c1 = ft == 15 ? new double[] {270, 800, 400} : new double[] {650, 400, 270};
                double[] c2 = ft == 15 ? new double[] {2140, 1150, 800} : new double[] {1080, 1700, 2140};
                coeffsOut.add(filterType(
                        e,
                        ft,
                        e.let("q", add(e, mul(e, res, e.n(32.6 / 240)), e.n(2.1))),
                        e.let("cc", clampf(e, cut, 0, 240)),
                        e.let("c1", threesel(e, e.v("cc"), c1[0], c1[1], c1[2])),
                        e.let("c2", threesel(e, e.v("cc"), c2[0], c2[1], c2[2])),
                        peq(e, bq0, e.v("c1"), f32(e, e.n(2.5)), q, div(e, one, q)),
                        rbj(e, "lpf", bq1, mul(e, e.v("c2"), e.n(1.2)), call(e, "sqrtf", q), one),
                        peq(e, bq2, e.v("c2"), f32(e, e.n(2.5)), call(e, "sqrtf", q), one)));
            }
            coeffsOut.add(filterType(
                    e,
                    17,
                    call(e, "bq_nothing", e.v("p"), bq0),
                    call(e, "bq_nothing", e.v("p"), bq1),
                    call(e, "bq_nothing", e.v("p"), bq2)));
        }
        body.add(e.let(
                "coeffs",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_coeffs",
                        new String[] {"p", "ch", "ty", "cutoff", "res"},
                        e.lowerV("coeffs_fn_t"),
                        e.seq(coeffsOut.toArray(SourceExpr[]::new)))));

        var lfoOut = new TList<SourceExpr>();
        {
            var phase = e.v("phase");
            var phs = e.v("phs");
            lfoOut.add(e.let(
                    "phs",
                    sub(
                            e,
                            phase,
                            mul(
                                    e,
                                    call(e, "floorf", add(e, mul(e, phase, e.n(1.0 / (2 * PI))), e.n(0.5))),
                                    e.n(2 * PI)))));
            var saw = mul(e, sub(e, mul(e, sub(e, phs, e.n(PI)), e.n(1.0 / PI)), e.n(0.5)), e.n(2));
            var steps = call(e, "floorf", add(e, mul(e, saw, e.n(4)), e.n(0.5)));
            var quarter = PI / 4;
            var shapes = new TList<SourceExpr>();
            shapes.add(call(e, "sinf", phs));
            shapes.add(saw);
            shapes.add(mul(e, saw, e.n(-1)));
            shapes.add(e.ifElse(lt(e, phs, e.n(PI)), f32(e, e.n(1)), f32(e, e.n(-1))));
            shapes.add(mul(
                    e,
                    sub(
                            e,
                            e.ifElse(
                                    lt(e, phs, e.n(PI)),
                                    mul(e, phs, e.n(1.0 / PI)),
                                    sub(e, f32(e, e.n(2)), mul(e, phs, e.n(1.0 / PI)))),
                            e.n(0.5)),
                    e.n(2)));
            shapes.add(call(e, "sinf", mul(e, mul(e, phs, e.n(8)), call(e, "sinf", phs))));
            shapes.add(call(
                    e, "sinf", mul(e, mul(e, phs, e.n(2)), call(e, "powf", f32(e, e.n(2)), call(e, "cosf", phs)))));
            shapes.add(call(e, "sinf", mul(e, phs, call(e, "powf", f32(e, e.n(4)), call(e, "sinf", phs)))));
            shapes.add(add(
                    e,
                    mul(e, call(e, "sinf", mul(e, phs, e.n(2))), e.n(0.5)),
                    mul(e, call(e, "cosf", mul(e, phs, e.n(3))), e.n(0.5))));
            shapes.add(mul(e, steps, e.n(0.25)));
            shapes.add(mul(e, steps, e.n(-0.25)));
            var fm = mul(e, call(e, "fmodf", phs, f32(e, e.n(quarter))), e.n(0.5 / quarter));
            var fmInv = mul(
                    e, call(e, "fmodf", sub(e, f32(e, e.n(2 * PI)), phs), f32(e, e.n(quarter))), e.n(0.5 / quarter));
            shapes.add(add(e, mul(e, steps, e.n(0.125)), fm));
            shapes.add(add(e, mul(e, steps, e.n(-0.125)), fm));
            shapes.add(add(e, mul(e, steps, e.n(0.125)), fmInv));
            shapes.add(add(e, mul(e, steps, e.n(-0.125)), fmInv));
            var q8 = call(e, "floorf", add(e, mul(e, phase, e.n(8 / PI)), e.n(0.5)));
            shapes.add(mul(
                    e,
                    call(
                            e,
                            "sinf",
                            add(e, mul(e, q8, e.n(19123)), mul(e, call(e, "sinf", mul(e, q8, e.n(12))), e.n(40)))),
                    e.n(0.5)));
            var q4 = call(e, "floorf", add(e, mul(e, phase, e.n(4 / PI)), e.n(0.5)));
            var q16 = call(e, "floorf", add(e, mul(e, phase, e.n(16 / PI)), e.n(0.5)));
            shapes.add(mul(
                    e,
                    call(
                            e,
                            "sinf",
                            add(e, mul(e, q4, e.n(1239543)), mul(e, call(e, "sinf", mul(e, q16, e.n(15))), e.n(40)))),
                    e.n(0.5)));
            var arms = new TList<SourceExpr>();
            for (int k = 0; k < shapes.size(); k++) {
                arms.add(ifOnly(e, eq(e, e.v("shape"), e.n(k)), e.mb.return_("lfo", shapes.get(k))));
            }
            arms.add(e.mb.return_("lfo", f32(e, e.n(0))));
            lfoOut.add(e.ret(e.mb.labeled("lfo", e.seq(arms.toArray(SourceExpr[]::new)))));
        }
        body.add(e.let(
                "lfo",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_lfo",
                        new String[] {"shape", "phase"},
                        e.lowerV("lfo_fn_t"),
                        e.seq(lfoOut.toArray(SourceExpr[]::new)))));

        BiFunction<Double, Boolean, SourceExpr> allocPass = (minAmp, excludeFree) -> {
            var cond = gt(e, env(e, "state", add(e, EcoBuzz.i32(e, e.n(CHANNELS)), e.v("i"))), e.n(0));
            if (excludeFree) {
                cond = and(e, cond, ne(e, env(e, "state", add(e, EcoBuzz.i32(e, e.n(CHANNELS)), e.v("i"))), e.n(4)));
            }
            var amp = mul(
                    e,
                    pa(e, "c_vel", e.v("i")),
                    f32(e, env(e, "last", add(e, EcoBuzz.i32(e, e.n(CHANNELS)), e.v("i")))));
            return e.seq(
                    set(e, e.v("minamp"), f32(e, e.n(minAmp))),
                    forRange(
                            e,
                            "i",
                            e.n(CHANNELS),
                            ifOnly(
                                    e,
                                    and(e, cond, lt(e, amp, e.v("minamp"))),
                                    set(e, e.v("minamp"), amp),
                                    set(e, e.v("last"), e.v("i")))));
        };
        body = partVoice.body;
        body.add(e.let(
                "alloc",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_alloc",
                        new String[] {"p"},
                        e.lowerV("alloc_fn_t"),
                        e.seq(
                                e.let("minamp", f32(e, e.n(0.1))),
                                e.let("last", EcoBuzz.i32(e, e.n(-1))),
                                allocPass.apply(0.1, true),
                                ifOnly(
                                        e,
                                        eq(e, e.v("last"), e.n(-1)),
                                        forRange(
                                                e,
                                                "i",
                                                e.n(CHANNELS),
                                                ifOnly(
                                                        e,
                                                        and(
                                                                e,
                                                                eq(e, e.v("last"), e.n(-1)),
                                                                eq(
                                                                        e,
                                                                        env(
                                                                                e,
                                                                                "state",
                                                                                add(
                                                                                        e,
                                                                                        EcoBuzz.i32(e, e.n(CHANNELS)),
                                                                                        e.v("i"))),
                                                                        e.n(4))),
                                                        set(e, e.v("last"), e.v("i"))))),
                                ifOnly(e, eq(e, e.v("last"), e.n(-1)), allocPass.apply(9e6, false)),
                                ifOnly(e, eq(e, e.v("last"), e.n(-1)), set(e, e.v("last"), e.n(0))),
                                e.ret(e.v("last"))))));

        var copy = new TList<SourceExpr>();
        for (var f : C_I32_FIELDS) copy.add(set(e, pa(e, f, e.v("dst")), pa(e, f, e.v("src"))));
        for (var f : C_F32_FIELDS) copy.add(set(e, pa(e, f, e.v("dst")), pa(e, f, e.v("src"))));
        for (int k = 0; k < 3; k++) {
            for (var f : BQ_FIELDS) {
                copy.add(set(
                        e,
                        bq(e, f, add(e, mul(e, e.v("dst"), e.n(3)), e.n(k))),
                        bq(e, f, add(e, mul(e, e.v("src"), e.n(3)), e.n(k)))));
            }
        }
        for (int w = 0; w < 2; w++) {
            for (var f : ENV_I32_FIELDS) {
                copy.add(set(
                        e,
                        env(e, f, add(e, e.v("dst"), e.n(w * CHANNELS))),
                        env(e, f, add(e, e.v("src"), e.n(w * CHANNELS)))));
            }
            for (var f : ENV_F64_FIELDS) {
                copy.add(set(
                        e,
                        env(e, f, add(e, e.v("dst"), e.n(w * CHANNELS))),
                        env(e, f, add(e, e.v("src"), e.n(w * CHANNELS)))));
            }
        }
        body.add(e.let(
                "chan_copy",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_chan_copy",
                        new String[] {"p", "dst", "src"},
                        e.lowerV("pii_fn_t"),
                        e.seq(copy.toArray(SourceExpr[]::new)))));

        var note = e.v("note");
        var newCh = e.v("t");
        var fenv = newCh;
        var aenv = add(e, EcoBuzz.i32(e, e.n(CHANNELS)), newCh);
        body.add(e.let(
                "playnote",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_playnote",
                        new String[] {"p", "t", "note", "accent", "length"},
                        e.lowerV("playnote_fn_t"),
                        e.seq(e.ifElse(
                                eq(e, note, e.n(EcoBuzz.NOTE_OFF)),
                                e.seq(
                                        e.let("och", pa(e, "t_channel", t)),
                                        ifOnly(
                                                e,
                                                ne(e, e.v("och"), e.n(-1)),
                                                call(e, "env_noteoff", e.v("p"), e.v("och")),
                                                call(
                                                        e,
                                                        "env_noteoff",
                                                        e.v("p"),
                                                        add(e, EcoBuzz.i32(e, e.n(CHANNELS)), e.v("och"))))),
                                e.seq(
                                        e.let("old", pa(e, "t_channel", t)),
                                        set(e, pa(e, "t_channel", t), t),
                                        ifOnly(
                                                e,
                                                ne(e, e.v("old"), e.n(-1)),
                                                e.let("nn", call(e, "alloc", e.v("p"))),
                                                call(
                                                        e,
                                                        "env_notefast",
                                                        e.v("p"),
                                                        add(e, EcoBuzz.i32(e, e.n(CHANNELS)), e.v("old"))),
                                                set(e, pa(e, "c_track", e.v("old")), e.n(-1)),
                                                call(e, "chan_copy", e.v("p"), e.v("nn"), e.v("old"))),
                                        set(e, pa(e, "c_track", newCh), t),
                                        bqReset(e, add(e, mul(e, newCh, e.n(3)), e.n(0))),
                                        bqReset(e, add(e, mul(e, newCh, e.n(3)), e.n(1))),
                                        bqReset(e, add(e, mul(e, newCh, e.n(3)), e.n(2))),
                                        set(e, pa(e, "c_ph1", newCh), e.n(0)),
                                        set(e, pa(e, "c_ph2", newCh), e.n(0)),
                                        set(e, pa(e, "c_ph3", newCh), e.n(0)),
                                        set(e, pa(e, "t_lastnote", t), note),
                                        set(e, pa(e, "t_lastaccent", t), e.v("accent")),
                                        set(e, pa(e, "t_lastlength", t), e.v("length")),
                                        set(e, pa(e, "c_vel", newCh), div(e, f32(e, e.v("accent")), f32(e, e.n(240)))),
                                        e.let(
                                                "sust",
                                                e.ifElse(
                                                        eq(e, e.v("length"), e.n(240)),
                                                        EcoBuzz.i32(e, e.n(1)),
                                                        maxi(
                                                                e,
                                                                i32(
                                                                        e,
                                                                        div(
                                                                                e,
                                                                                mul(
                                                                                        e,
                                                                                        f32(e, e.v("length")),
                                                                                        f32(e, p(e, "spt"))),
                                                                                e.n(16))),
                                                                1))),
                                        call(
                                                e,
                                                "env_set",
                                                e.v("p"),
                                                fenv,
                                                envTime2(e, gact(e, 18)),
                                                envTime(e, gact(e, 19)),
                                                sustLevel(e, gact(e, 20)),
                                                e.v("sust"),
                                                envTime(e, gact(e, 21))),
                                        call(
                                                e,
                                                "env_set",
                                                e.v("p"),
                                                aenv,
                                                envTime2(e, gact(e, 33)),
                                                envTime(e, gact(e, 34)),
                                                sustLevel(e, gact(e, 35)),
                                                e.v("sust"),
                                                envTime(e, gact(e, 36))),
                                        ifOnly(
                                                e,
                                                eq(e, and(e, gact(e, 37), e.n(4)), e.n(0)),
                                                call(e, "env_noteon", e.v("p"), fenv)),
                                        ifOnly(
                                                e,
                                                eq(e, and(e, gact(e, 37), e.n(8)), e.n(0)),
                                                call(e, "env_noteon", e.v("p"), aenv)),
                                        set(e, pa(e, "c_detune", newCh), pa(e, "t_detune", t)),
                                        e.let(
                                                "f",
                                                div(
                                                        e,
                                                        mul(
                                                                e,
                                                                call(
                                                                        e,
                                                                        "powf",
                                                                        f32(e, e.n(2)),
                                                                        add(
                                                                                e,
                                                                                f32(
                                                                                        e,
                                                                                        e.mb.operatorShr(
                                                                                                sub(e, note, e.n(1)),
                                                                                                e.n(4))),
                                                                                div(
                                                                                        e,
                                                                                        f32(
                                                                                                e,
                                                                                                sub(
                                                                                                        e,
                                                                                                        and(
                                                                                                                e,
                                                                                                                note,
                                                                                                                e.n(
                                                                                                                        15)),
                                                                                                        e.n(58))),
                                                                                        f32(e, e.n(12))))),
                                                                e.n(220)),
                                                        e.n(RATE))),
                                        set(e, pa(e, "t_basefreq", t), e.v("f")),
                                        set(e, pa(e, "t_destfreq", t), e.v("f")),
                                        ifOnly(e, eq(e, gact(e, 13), e.n(0)), set(e, pa(e, "c_freq", newCh), e.v("f"))),
                                        ifOnly(
                                                e,
                                                ne(e, and(e, gact(e, 37), e.n(1)), e.n(0)),
                                                set(e, pa(e, "t_lfophase", t), e.n(0))),
                                        ifOnly(
                                                e,
                                                ne(e, and(e, gact(e, 37), e.n(2)), e.n(0)),
                                                set(e, pa(e, "t_lfo2phase", t), e.n(0)))))))));

        Function<String[], SourceExpr> lfoStep = names -> {
            var cur = names[0];
            var phase = names[1];
            var ai = names[2];
            var acc = names[3];
            var shape = Integer.parseInt(names[4]);
            var rateP = Integer.parseInt(names[5]);
            var rate = e.v("r" + cur);
            return e.seq(
                    inertiaProcess(
                            e,
                            pa(e, ai, t),
                            pa(e, acc, t),
                            mul(e, call(e, "lfo", gact(e, shape), pa(e, phase, t)), e.n(30)),
                            e.v("c")),
                    set(e, pa(e, cur, t), div(e, pa(e, acc, t), e.n(30))),
                    e.let("r" + cur, gact(e, rateP)),
                    e.let(
                            "d" + cur,
                            e.ifElse(
                                    lt(e, rate, e.n(240)),
                                    mul(
                                            e,
                                            mul(
                                                    e,
                                                    call(
                                                            e,
                                                            "powf",
                                                            f32(e, e.n(600.0)),
                                                            div(e, f32(e, rate), f32(e, e.n(240)))),
                                                    e.n(0.03)),
                                            e.n(2 * PI / RATE)),
                                    div(
                                            e,
                                            f32(e, e.n(2 * PI)),
                                            f32(
                                                    e,
                                                    mul(
                                                            e,
                                                            idx(e, lw(e, "times"), sub(e, rate, e.n(240))),
                                                            p(e, "spt")))))),
                    set(e, pa(e, phase, t), add(e, pa(e, phase, t), mul(e, e.v("d" + cur), f32(e, e.v("c"))))),
                    ifOnly(
                            e,
                            gt(e, pa(e, phase, t), e.n(1024 * PI)),
                            set(e, pa(e, phase, t), sub(e, pa(e, phase, t), e.n(1024 * PI)))));
        };
        body.add(e.let(
                "dolfo",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_dolfo",
                        new String[] {"p", "t", "c"},
                        e.lowerV("pii_fn_t"),
                        e.seq(
                                lfoStep.apply(
                                        new String[] {"t_curlfo", "t_lfophase", "t_l1_ai", "t_l1_acc", "28", "25"}),
                                lfoStep.apply(
                                        new String[] {"t_curlfo2", "t_lfo2phase", "t_l2_ai", "t_l2_acc", "32", "29"
                                        })))));

        var chanOut = new TList<SourceExpr>();
        {
            var c = e.v("c");
            var p = e.v("p");
            var wfenv = ch;
            var waenv = add(e, EcoBuzz.i32(e, e.n(CHANNELS)), ch);
            chanOut.add(e.let(
                    "trk",
                    e.ifElse(eq(e, pa(e, "c_track", ch), e.n(-1)), EcoBuzz.i32(e, e.n(TRACKS)), pa(e, "c_track", ch))));
            var trk = e.v("trk");
            var names = new String[] {"cut0", "res0", "mod0", "shape0", "l1d1", "l1d2", "l2d1", "l2d2"};
            for (int k = 0; k < 8; k++) chanOut.add(e.let(names[k], pa(e, "inr_acc", e.n(k))));
            chanOut.add(e.let("kta", f32(e, e.n(0))));
            chanOut.add(inertiaProcess(
                    e,
                    pa(e, "c_kt_ai", ch),
                    pa(e, "c_kt_acc", ch),
                    mul(e, f32(e, sub(e, gact(e, 24), e.n(120))), e.n(1.0 / 60)),
                    c));
            chanOut.add(set(e, e.v("kta"), pa(e, "c_kt_acc", ch)));
            var active = new TList<SourceExpr>();
            active.add(e.let(
                    "pdev",
                    mul(e, call(e, "logf", mul(e, pa(e, "c_freq", ch), e.n(RATE / 264.0))), e.n(1.0 / LN_SEMI))));
            active.add(e.let("keytrack", mul(e, mul(e, e.v("kta"), e.v("pdev")), e.n(240.0 / 6 / 12))));
            active.add(e.let(
                    "freq0",
                    e.ifElse(
                            ne(e, and(e, gact(e, 37), e.n(16)), e.n(0)),
                            mul(
                                    e,
                                    call(
                                            e,
                                            "powf",
                                            f32(e, e.n(2)),
                                            mul(
                                                    e,
                                                    call(
                                                            e,
                                                            "floorf",
                                                            add(
                                                                    e,
                                                                    mul(
                                                                            e,
                                                                            call(
                                                                                    e,
                                                                                    "logf",
                                                                                    mul(
                                                                                            e,
                                                                                            pa(e, "c_freq", ch),
                                                                                            e.n(RATE / 220.0))),
                                                                            e.n(1.0 / LN_SEMI)),
                                                                    e.n(0.5))),
                                                    e.n(1.0 / 12))),
                                    e.n(220.0 / RATE)),
                            pa(e, "c_freq", ch))));
            active.add(e.let(
                    "freq",
                    e.ifElse(
                            ne(e, pa(e, "c_detune", ch), e.n(0)),
                            mul(
                                    e,
                                    e.v("freq0"),
                                    call(
                                            e,
                                            "powf",
                                            f32(e, e.n(2)),
                                            mul(
                                                    e,
                                                    add(
                                                            e,
                                                            mul(e, pa(e, "c_detune", ch), e.n(100.0 / 256)),
                                                            e.n(ATTR_TUNING)),
                                                    e.n(1.0 / 1200)))),
                            e.v("freq0"))));
            var gdet = mul(e, f32(e, gact(e, 9)), e.n(1.0 / (24 * 240)));
            active.add(e.let("f1", mul(e, call(e, "powf", f32(e, e.n(2)), e.mb.operatorNeg(gdet)), e.v("freq"))));
            active.add(e.let(
                    "f2",
                    mul(
                            e,
                            call(
                                    e,
                                    "powf",
                                    f32(e, e.n(2)),
                                    add(e, mul(e, f32(e, sub(e, gact(e, 8), e.n(36))), e.n(1.0 / 12)), gdet)),
                            e.v("freq"))));
            active.add(e.let("f3", mul(e, e.v("freq"), e.n(0.5))));
            var fmul = 2147483648.0;
            active.add(e.let("ph1", i32(e, mul(e, pa(e, "c_ph1", ch), e.n(fmul)))));
            active.add(e.let("ph2", i32(e, mul(e, pa(e, "c_ph2", ch), e.n(fmul)))));
            active.add(e.let("ph3", i32(e, mul(e, pa(e, "c_ph3", ch), e.n(fmul)))));
            active.add(e.let("df1", i32(e, mul(e, e.v("f1"), e.n(fmul)))));
            active.add(e.let("df2", i32(e, mul(e, e.v("f2"), e.n(fmul)))));
            active.add(e.let("df3", i32(e, mul(e, e.v("f3"), e.n(fmul)))));
            active.add(e.let("vel", mul(e, pa(e, "c_vel", ch), e.n(1.41))));
            active.add(e.let("wa", gact(e, 0)));
            active.add(e.let("wb", gact(e, 4)));
            active.add(e.let("ws", gact(e, 11)));
            var nul = EcoBuzz.i32(e, e.n(15));
            active.add(e.let(
                    "t1a", e.ifElse(lt(e, e.v("wa"), e.n(TABLES_A.size())), idx(e, lw(e, "map_a"), e.v("wa")), nul)));
            active.add(e.let(
                    "t1b", e.ifElse(lt(e, e.v("wa"), e.n(TABLES_A.size())), idx(e, lw(e, "map_b"), e.v("wa")), nul)));
            active.add(e.let(
                    "t2a", e.ifElse(lt(e, e.v("wb"), e.n(TABLES_A.size())), idx(e, lw(e, "map_a"), e.v("wb")), nul)));
            active.add(e.let(
                    "t2b", e.ifElse(lt(e, e.v("wb"), e.n(TABLES_A.size())), idx(e, lw(e, "map_b"), e.v("wb")), nul)));
            active.add(e.let(
                    "t3", e.ifElse(lt(e, e.v("ws"), e.n(TABLES_C.size())), idx(e, lw(e, "map_c"), e.v("ws")), nul)));
            for (var lv : new String[][] {{"1a", "f1"}, {"1b", "f1"}, {"2a", "f2"}, {"2b", "f2"}, {"3", "f3"}}) {
                active.add(e.let("lv" + lv[0], call(e, "level", e.v("t" + lv[0]), e.v(lv[1]))));
                active.add(e.let(
                        "off" + lv[0],
                        add(
                                e,
                                idx(e, lw(e, "tab_base"), e.v("t" + lv[0])),
                                e.ifElse(
                                        eq(e, e.v("lv" + lv[0]), e.n(0)),
                                        EcoBuzz.i32(e, e.n(0)),
                                        add(e, mul(e, sub(e, e.v("lv" + lv[0]), e.n(1)), e.n(LEVELN)), e.n(LEVEL0))))));
                active.add(e.let(
                        "bits" + lv[0],
                        e.ifElse(eq(e, e.v("lv" + lv[0]), e.n(0)), EcoBuzz.i32(e, e.n(11)), EcoBuzz.i32(e, e.n(10)))));
            }
            active.add(e.let("pwoff1", i32(e, mul(e, f32(e, gact(e, 3)), e.n((1 << 30) / 120.0)))));
            active.add(e.let("pwoff2", i32(e, mul(e, f32(e, gact(e, 7)), e.n((1 << 30) / 120.0)))));
            active.add(e.let(
                    "coeff1",
                    i32(e, call(e, "floorf", add(e, mul(e, e.v("f1"), e.n(65536.0 * RATE / 1000)), e.n(0.5))))));
            active.add(e.let("mix", mul(e, f32(e, gact(e, 10)), e.n(1.0 / 240))));
            active.add(e.let("sub", mul(e, f32(e, gact(e, 12)), e.n(1.0 / 240))));
            active.add(e.let("kshape", mul(e, add(e, e.v("shape0"), e.n(1)), e.n(1.0 / 241))));
            active.add(e.let("res", e.v("res0")));
            active.add(e.let("s0", EcoBuzz.i32(e, e.n(0))));
            active.add(e.let("c0", EcoBuzz.i32(e, e.n(0))));
            active.add(e.let("max", EcoBuzz.i32(e, e.n(0))));
            active.add(e.let("ncur", EcoBuzz.i32(e, e.n(0))));
            active.add(e.let("go", EcoBuzz.i32(e, e.n(0))));
            active.add(e.let("cuta", f32(e, e.n(0))));
            active.add(e.let("cutb", f32(e, e.n(0))));
            var segs = new TList<SourceExpr>();
            segs.add(set(e, e.v("c0"), c));
            segs.add(set(e, e.v("max"), sub(e, c, e.v("s0"))));
            var last = env(e, "last", wfenv);
            var series = env(e, "series", wfenv);
            segs.add(ifOnly(
                    e,
                    ne(e, env(e, "state", wfenv), e.n(4)),
                    set(
                            e,
                            e.v("ncur"),
                            mul(e, minf(e, e.v("max"), sub(e, env(e, "stage", wfenv), env(e, "time", wfenv))), e.n(2))),
                    set(e, e.v("cuta"), mul(e, call(e, "powf", f32(e, last), e.v("kshape")), e.n(240))),
                    set(e, e.v("go"), e.n(1)),
                    EcoBuzz.whileLoop(
                            e,
                            "hq",
                            ne(e, e.v("go"), e.n(0)),
                            set(e, e.v("ncur"), div(e, mul(e, e.v("ncur"), e.n(50)), e.n(100))),
                            set(
                                    e,
                                    e.v("cutb"),
                                    mul(
                                            e,
                                            call(
                                                    e,
                                                    "powf",
                                                    f32(
                                                            e,
                                                            e.ifElse(
                                                                    lt(e, env(e, "state", wfenv), e.n(1)),
                                                                    add(e, last, mul(e, series, f64(e, e.v("ncur")))),
                                                                    mul(
                                                                            e,
                                                                            last,
                                                                            call(
                                                                                    e,
                                                                                    "pow",
                                                                                    series,
                                                                                    f64(e, e.v("ncur")))))),
                                                    e.v("kshape")),
                                            e.n(240))),
                            set(
                                    e,
                                    e.v("go"),
                                    e.ifElse(
                                            and(
                                                    e,
                                                    gt(
                                                            e,
                                                            call(e, "fabsf", sub(e, e.v("cutb"), e.v("cuta"))),
                                                            e.n(3.0 / (ATTR_HQ + 1))),
                                                    ne(e, e.v("ncur"), e.n(0))),
                                            EcoBuzz.i32(e, e.n(1)),
                                            EcoBuzz.i32(e, e.n(0))))),
                    set(e, e.v("max"), maxi(e, e.v("ncur"), 1)),
                    ifOnly(
                            e,
                            gt(e, sub(e, e.v("c0"), e.v("s0")), e.v("max")),
                            set(e, e.v("c0"), add(e, e.v("s0"), e.v("max"))))));
            segs.add(e.let("fe", call(e, "env_process", p, wfenv, sub(e, e.v("c0"), e.v("s0")))));
            var curlfo = pa(e, "t_curlfo", trk);
            var curlfo2 = pa(e, "t_curlfo2", trk);
            segs.add(e.let(
                    "cutoff",
                    add(
                            e,
                            add(
                                    e,
                                    add(
                                            e,
                                            add(e, e.v("cut0"), e.v("keytrack")),
                                            mul(e, sub(e, e.v("l1d1"), e.n(120)), curlfo)),
                                    mul(e, sub(e, e.v("l2d1"), e.n(120)), curlfo2)),
                            mul(
                                    e,
                                    add(
                                            e,
                                            mul(e, sub(e, e.v("l1d2"), e.n(120)), curlfo),
                                            mul(e, sub(e, e.v("mod0"), e.n(120)), e.n(2))),
                                    call(e, "powf", mul(e, pa(e, "c_vel", ch), e.v("fe")), e.v("kshape"))))));
            segs.add(set(e, e.v("res"), add(e, e.v("res"), mul(e, sub(e, e.v("l2d2"), e.n(120)), curlfo2))));
            segs.add(set(e, e.v("res"), clampf(e, e.v("res"), 0, 240)));
            segs.add(call(e, "coeffs", p, ch, gact(e, 14), e.v("cutoff"), e.v("res")));
            var b0 = mul(e, ch, e.n(3));
            var b1 = add(e, mul(e, ch, e.n(3)), e.n(1));
            var b2 = add(e, mul(e, ch, e.n(3)), e.n(2));
            segs.add(forRange(
                    e,
                    "j",
                    sub(e, e.v("c0"), e.v("s0")),
                    e.let("i", add(e, e.v("s0"), e.v("j"))),
                    e.let("amp", mul(e, call(e, "env_next", p, waenv), e.v("vel"))),
                    e.let("pwm1", add(e, e.v("pwoff1"), mul(e, e.v("coeff1"), pa(e, "pwmbuf1", i)))),
                    e.let("pwm2", add(e, e.v("pwoff2"), mul(e, e.v("coeff1"), pa(e, "pwmbuf2", i)))),
                    e.let(
                            "osc1",
                            sub(
                                    e,
                                    call(e, "wave_read", e.v("off1a"), e.v("bits1a"), e.v("ph1")),
                                    call(
                                            e,
                                            "wave_read",
                                            e.v("off1b"),
                                            e.v("bits1b"),
                                            add(e, e.v("ph1"), e.v("pwm1"))))),
                    e.let(
                            "osc2",
                            sub(
                                    e,
                                    call(e, "wave_read", e.v("off2a"), e.v("bits2a"), e.v("ph2")),
                                    call(
                                            e,
                                            "wave_read",
                                            e.v("off2b"),
                                            e.v("bits2b"),
                                            add(e, e.v("ph2"), e.v("pwm2"))))),
                    e.let("osc3", call(e, "wave_read", e.v("off3"), e.v("bits3"), e.v("ph3"))),
                    e.let(
                            "output",
                            add(
                                    e,
                                    add(e, e.v("osc1"), mul(e, sub(e, e.v("osc2"), e.v("osc1")), e.v("mix"))),
                                    mul(e, e.v("osc3"), e.v("sub")))),
                    e.let(
                            "y",
                            mul(
                                    e,
                                    e.v("amp"),
                                    call(
                                            e,
                                            "bq_proc",
                                            p,
                                            b2,
                                            call(
                                                    e,
                                                    "bq_proc",
                                                    p,
                                                    b1,
                                                    call(e, "bq_proc", p, b0, mul(e, e.v("amp"), e.v("output"))))))),
                    set(
                            e,
                            idx(e, e.v("out"), mul(e, i, e.n(2))),
                            add(e, idx(e, e.v("out"), mul(e, i, e.n(2))), e.v("y"))),
                    set(
                            e,
                            idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))),
                            add(e, idx(e, e.v("out"), add(e, mul(e, i, e.n(2)), e.n(1))), e.v("y"))),
                    set(e, e.v("ph1"), add(e, e.v("ph1"), e.v("df1"))),
                    set(e, e.v("ph2"), add(e, e.v("ph2"), e.v("df2"))),
                    set(e, e.v("ph3"), add(e, e.v("ph3"), e.v("df3")))));
            segs.add(set(e, e.v("s0"), e.v("c0")));
            active.add(EcoBuzz.whileLoop(e, "segs", lt(e, e.v("s0"), c), segs.toArray(SourceExpr[]::new)));
            for (var ph : new String[][] {{"c_ph1", "f1"}, {"c_ph2", "f2"}, {"c_ph3", "f3"}}) {
                active.add(set(
                        e,
                        pa(e, ph[0], ch),
                        call(e, "fmodf", add(e, pa(e, ph[0], ch), mul(e, e.v(ph[1]), f32(e, c))), f32(e, e.n(1)))));
            }
            var gl = e.v("gl");
            var dest = pa(e, "t_destfreq", trk);
            var cur = pa(e, "c_freq", ch);
            active.add(ifOnly(
                    e,
                    and(e, ne(e, pa(e, "c_track", ch), e.n(-1)), eq(e, pa(e, "t_channel", trk), ch)),
                    e.let("gl", gact(e, 13)),
                    e.ifElse(
                            eq(e, gl, e.n(0)),
                            set(e, cur, dest),
                            ifOnly(
                                    e,
                                    ne(e, cur, dest),
                                    e.let(
                                            "g",
                                            call(
                                                    e,
                                                    "powf",
                                                    f32(e, e.n(1.01)),
                                                    mul(
                                                            e,
                                                            f32(e, c),
                                                            sub(
                                                                    e,
                                                                    f32(e, e.n(1)),
                                                                    call(
                                                                            e,
                                                                            "powf",
                                                                            mul(e, f32(e, gl), e.n(1.0 / 241)),
                                                                            f32(e, e.n(0.03))))))),
                                    set(
                                            e,
                                            cur,
                                            e.ifElse(
                                                    lt(e, cur, dest),
                                                    minf(e, mul(e, cur, e.v("g")), dest),
                                                    maxf(e, div(e, cur, e.v("g")), dest)))))));
            chanOut.add(e.ifElse(
                    eq(e, env(e, "state", waenv), e.n(4)),
                    e.seq(call(e, "env_process", p, waenv, c), call(e, "env_process", p, wfenv, c)),
                    e.seq(active.toArray(SourceExpr[]::new))));
        }
        body.add(e.let(
                "chan_work",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_chan_work",
                        new String[] {"p", "out", "c", "ch"},
                        e.lowerV("chan_fn_t"),
                        e.seq(chanOut.toArray(SourceExpr[]::new)))));

        body = main;
        var init = new TList<SourceExpr>();
        for (int k = 0; k < NG; k++) init.add(set(e, gact(e, k), e.n(GACT_DEFAULTS.get(k))));
        init.add(resetVals(e));
        init.add(forRange(
                e, "k", e.n(8), set(e, pa(e, "inr_ai", e.v("k")), e.n(1)), set(e, pa(e, "inr_acc", e.v("k")), e.n(0))));
        init.add(set(e, p(e, "pwm1_phase"), e.n(0)));
        init.add(set(e, p(e, "pwm2_phase"), e.n(0)));
        var chInit = new TList<SourceExpr>();
        chInit.add(set(e, pa(e, "c_freq", ch), e.n(0.01)));
        chInit.add(set(e, pa(e, "c_track", ch), e.n(-1)));
        for (var f : new String[] {"c_ph1", "c_ph2", "c_ph3", "c_vel", "c_detune", "c_kt_acc", "c_c2_acc"}) {
            chInit.add(set(e, pa(e, f, ch), e.n(0)));
        }
        chInit.add(set(e, pa(e, "c_kt_ai", ch), e.n(1)));
        chInit.add(set(e, pa(e, "c_c2_ai", ch), e.n(1)));
        for (int k = 0; k < 3; k++) {
            for (var f : BQ_FIELDS) chInit.add(set(e, bq(e, f, add(e, mul(e, ch, e.n(3)), e.n(k))), e.n(0)));
        }
        for (int w = 0; w < 2; w++) {
            var eidx = add(e, ch, e.n(w * CHANNELS));
            chInit.add(set(e, env(e, "silence", eidx), e.n(w == 0 ? 1.0 / 4096 : 1.0 / 128)));
            chInit.add(set(e, env(e, "start", eidx), e.n(1.0 / 4096)));
            chInit.add(set(e, env(e, "last", eidx), e.n(1.0 / 4096)));
            chInit.add(set(e, env(e, "series", eidx), e.n(1)));
            chInit.add(set(e, env(e, "sustlevel", eidx), e.n(1)));
            chInit.add(set(e, env(e, "state", eidx), e.n(4)));
            for (var f : new String[] {"time", "att", "sus", "dec", "rel", "stage"}) {
                chInit.add(set(e, env(e, f, eidx), e.n(0)));
            }
        }
        init.add(forRange(e, "ch", e.n(CHANNELS), chInit.toArray(SourceExpr[]::new)));
        var tInit = new TList<SourceExpr>();
        tInit.add(set(e, pa(e, "t_channel", t), e.n(-1)));
        tInit.add(set(e, pa(e, "t_note", t), e.n(0)));
        tInit.add(set(e, pa(e, "t_accent", t), e.n(224)));
        tInit.add(set(e, pa(e, "t_length", t), e.n(40)));
        for (var f : new String[] {"t_lastnote", "t_lastaccent", "t_lastlength"})
            tInit.add(set(e, pa(e, f, t), e.n(0)));
        tInit.add(set(e, pa(e, "t_destfreq", t), e.n(0.01)));
        for (var f : new String[] {
            "t_basefreq", "t_detune", "t_lfophase", "t_lfo2phase", "t_curlfo", "t_curlfo2", "t_l1_acc", "t_l2_acc"
        }) {
            tInit.add(set(e, pa(e, f, t), e.n(0)));
        }
        tInit.add(set(e, pa(e, "t_l1_ai", t), e.n(1)));
        tInit.add(set(e, pa(e, "t_l2_ai", t), e.n(1)));
        init.add(forRange(e, "t", e.n(TRACKS + 1), tInit.toArray(SourceExpr[]::new)));
        init.add(call(e, "tables"));
        body.add(e.let(
                "init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_init",
                        new String[] {"p"},
                        e.lowerV("init_fn_t"),
                        e.seq(init.toArray(SourceExpr[]::new)))));

        var tick = new TList<SourceExpr>();
        tick.add(forRange(
                e,
                "k",
                e.n(NG),
                e.let("gv", pa(e, "gval", e.v("k"))),
                ifOnly(e, ne(e, e.v("gv"), e.n(EcoBuzz.NO_VALUE)), set(e, pa(e, "gact", e.v("k")), e.v("gv")))));
        tick.add(forRange(e, "k", e.n(8), inertiaSet(e, pa(e, "inr_ai", e.v("k")), gact(e, 23))));
        tick.add(forRange(
                e,
                "t",
                e.n(TRACKS),
                e.let("tch", pa(e, "t_channel", t)),
                e.let("vel", tval(e, t, 1)),
                ifOnly(e, ne(e, e.v("vel"), e.n(EcoBuzz.NO_VALUE)), set(e, pa(e, "t_accent", t), e.v("vel"))),
                e.let("len", tval(e, t, 2)),
                ifOnly(e, ne(e, e.v("len"), e.n(EcoBuzz.NO_VALUE)), set(e, pa(e, "t_length", t), e.v("len"))),
                e.let("nt", tval(e, t, 0)),
                ifOnly(
                        e,
                        ne(e, e.v("nt"), e.n(EcoBuzz.NO_VALUE)),
                        set(e, pa(e, "t_note", t), e.v("nt")),
                        call(e, "playnote", e.v("p"), t, e.v("nt"), pa(e, "t_accent", t), pa(e, "t_length", t))),
                ifOnly(
                        e,
                        ne(e, e.v("tch"), e.n(-1)),
                        inertiaSet(
                                e,
                                pa(e, "c_kt_ai", e.v("tch")),
                                i32(
                                        e,
                                        mul(
                                                e,
                                                call(e, "sqrtf", div(e, f32(e, gact(e, 23)), f32(e, e.n(240)))),
                                                e.n(240)))),
                        inertiaSet(e, pa(e, "c_c2_ai", e.v("tch")), EcoBuzz.i32(e, e.n(1)))),
                inertiaSet(e, pa(e, "t_l1_ai", t), EcoBuzz.i32(e, e.n(1))),
                inertiaSet(e, pa(e, "t_l2_ai", t), EcoBuzz.i32(e, e.n(1)))));
        tick.add(resetVals(e));
        body.add(e.let(
                "tick",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_tick",
                        new String[] {"p"},
                        e.lowerV("tick_fn_t"),
                        e.seq(tick.toArray(SourceExpr[]::new)))));

        var work = new TList<SourceExpr>();
        work.add(call(e, "zero", e.v("out"), e.v("n")));
        for (var pw : new String[][] {{"pwm1", "1", "2"}, {"pwm2", "5", "6"}}) {
            var rate = e.v(pw[0] + "_rate");
            work.add(e.let(pw[0] + "_rate", gact(e, Integer.parseInt(pw[1]))));
            work.add(set(e, p(e, pw[0] + "_delta"), mul(e, rate, e.n(300))));
            work.add(set(
                    e,
                    p(e, pw[0] + "_depth"),
                    div(
                            e,
                            mul(
                                    e,
                                    div(e, EcoBuzz.i32(e, e.n(65535 * 120)), add(e, rate, e.n(120))),
                                    gact(e, Integer.parseInt(pw[2]))),
                            e.n(240))));
            work.add(forRange(
                    e,
                    "i",
                    e.v("n"),
                    set(e, p(e, pw[0] + "_phase"), add(e, p(e, pw[0] + "_phase"), p(e, pw[0] + "_delta"))),
                    e.let("tab", e.mb.operatorShr(p(e, pw[0] + "_phase"), e.n(17))),
                    e.let("frac", mul(e, f32(e, and(e, p(e, pw[0] + "_phase"), e.n(65535))), e.n(1.0 / 65536))),
                    e.let("s0", f32(e, idx(e, lw(e, "isine"), and(e, e.v("tab"), e.n(2047))))),
                    e.let("s1", f32(e, idx(e, lw(e, "isine"), and(e, add(e, e.v("tab"), e.n(1)), e.n(2047))))),
                    e.let("pt", add(e, e.v("s0"), mul(e, e.v("frac"), sub(e, e.v("s1"), e.v("s0"))))),
                    set(
                            e,
                            pa(e, pw[0].replace("pwm", "pwmbuf"), e.v("i")),
                            i32(
                                    e,
                                    call(
                                            e,
                                            "floorf",
                                            add(
                                                    e,
                                                    mul(
                                                            e,
                                                            mul(e, f32(e, p(e, pw[0] + "_depth")), e.v("pt")),
                                                            e.n(1.0 / 32768)),
                                                    e.n(0.5)))))));
        }
        for (int k = 0; k < 8; k++) {
            work.add(inertiaProcess(
                    e,
                    pa(e, "inr_ai", e.n(k)),
                    pa(e, "inr_acc", e.n(k)),
                    f32(e, gact(e, INERTIA_PARAMS.get(k))),
                    e.v("n")));
        }
        work.add(forRange(e, "t", e.n(TRACKS), call(e, "dolfo", e.v("p"), t, e.v("n"))));
        work.add(forRange(e, "ch", e.n(CHANNELS), call(e, "chan_work", e.v("p"), e.v("out"), e.v("n"), ch)));
        work.add(forRange(
                e,
                "i",
                mul(e, e.v("n"), e.n(2)),
                e.let("x", idx(e, e.v("out"), e.v("i"))),
                ifOnly(
                        e,
                        e.not(and(e, lt(e, e.v("x"), e.n(4000000)), gt(e, e.v("x"), e.n(-4000000)))),
                        set(e, idx(e, e.v("out"), e.v("i")), e.n(0)))));
        body.add(e.let(
                "work",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "inf_work",
                        new String[] {"p", "out", "n"},
                        e.lowerV("work_fn_t"),
                        e.seq(work.toArray(SourceExpr[]::new)))));
        return EcoBuzz.machineModule(
                e,
                "inf",
                defs,
                new String[] {"init", "tick", "work"},
                TList.of(partTables, partDsp, partVoice),
                main,
                new SourceExpr[] {e.v("init"), e.v("tick"), e.v("work")});
    }

    private static SourceExpr constArray(EcoMb e, String name, String ty, TList<SourceExpr> vals) {
        return e.let(
                name,
                e.call(
                        EcoBuzz.llvmL(e, "global_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s("inf_" + name)),
                                e.p("ty", e.lowerV(ty)),
                                e.p("init", e.listNew(e.mb.typesInt(), vals.toArray(SourceExpr[]::new))))));
    }

    private static TList<SourceExpr> intList(EcoMb e, IList<Integer> values) {
        var out = new TList<SourceExpr>();
        for (var v : values) out.add(e.intOf(v));
        return out;
    }

    private static SourceExpr f64(EcoMb e, SourceExpr v) {
        return EcoBuzz.conv(e, "t_f64", v);
    }

    private static SourceExpr maxi(EcoMb e, SourceExpr v, int floor) {
        return e.ifElse(lt(e, v, e.n(floor)), EcoBuzz.i32(e, e.n(floor)), v);
    }

    private static SourceExpr envStage(EcoMb e, int state, SourceExpr... then) {
        var ei = e.v("ei");
        return ifOnly(
                e,
                and(e, eq(e, env(e, "state", ei), e.n(state)), ge(e, env(e, "time", ei), env(e, "stage", ei))),
                then);
    }

    private static SourceExpr bqReset(EcoMb e, SourceExpr bi) {
        return e.seq(
                set(e, bq(e, "x1", bi), e.n(0)),
                set(e, bq(e, "x2", bi), e.n(0)),
                set(e, bq(e, "y1", bi), e.n(0)),
                set(e, bq(e, "y2", bi), e.n(0)));
    }

    private static SourceExpr resetVals(EcoMb e) {
        return e.seq(
                forRange(e, "k", e.n(NG), set(e, pa(e, "gval", e.v("k")), e.n(EcoBuzz.NO_VALUE))),
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

    private static SourceExpr maxf(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.ifElse(gt(e, a, b), a, b);
    }

    private static SourceExpr minf(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.ifElse(lt(e, a, b), a, b);
    }

    private static SourceExpr threesel(EcoMb e, SourceExpr sel, double a, double b, double c) {
        return e.ifElse(
                lt(e, sel, e.n(120)),
                add(e, mul(e, sel, e.n((b - a) / 120)), e.n(a)),
                add(e, mul(e, sub(e, sel, e.n(120)), e.n((c - b) / 120)), e.n(b)));
    }
}
