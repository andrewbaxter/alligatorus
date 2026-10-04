package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.and;
import static compilerxyz.main.tests.EcoBuzz.eq;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.fld;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.i32;
import static compilerxyz.main.tests.EcoBuzz.idx;
import static compilerxyz.main.tests.EcoBuzz.ifOnly;
import static compilerxyz.main.tests.EcoBuzz.lt;
import static compilerxyz.main.tests.EcoBuzz.lw;
import static compilerxyz.main.tests.EcoBuzz.mul;
import static compilerxyz.main.tests.EcoBuzz.or;
import static compilerxyz.main.tests.EcoBuzz.set;
import static compilerxyz.main.tests.EcoBuzz.shl;
import static compilerxyz.main.tests.EcoBuzz.sub;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

final class EcoBuzzDsp {
    private EcoBuzzDsp() {}

    static final IList<String> FNS = IList.of("wave_read", "zero", "mix_add", "note_rate", "osc_init", "osc_at");
    static final int MAX_WAVES = 32;
    static final int OSC_WAVES = 6;
    static final int OSC_BASE = 2048;
    static final int OSC_LEVELS = 11;
    static final int OSC_STRIDE = 4092;

    private static SourceExpr sample16(EcoMb e, SourceExpr data, SourceExpr byteIndex) {
        var lo = and(e, i32(e, idx(e, data, byteIndex)), e.n(255));
        var hi = shl(e, i32(e, idx(e, data, add(e, byteIndex, e.n(1)))), e.n(8));
        return f32(e, or(e, hi, lo));
    }

    private static SourceExpr accessor(EcoMb e, String obj, String name, SourceExpr type) {
        return e.methodImpl(e.v(obj), name, e.tup(), e.optionTypeSome(type), e.stage(e.fld(e.v("inner"), name)));
    }

    static TList<SourceExpr> module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        EcoBuzz.prelude(e, exprs);
        exprs.add(e.let(
                "wave_t",
                EcoBuzz.struct(
                        e,
                        "bz_wave",
                        EcoBuzz.field(e, "data", e.v("t_pi8")),
                        EcoBuzz.field(e, "frames", e.v("t_i32")),
                        EcoBuzz.field(e, "channels", e.v("t_i32")),
                        EcoBuzz.field(e, "rate", e.v("t_i32")),
                        EcoBuzz.field(e, "vol", e.v("t_f32")),
                        EcoBuzz.field(e, "loop_start", e.v("t_i32")),
                        EcoBuzz.field(e, "loop_end", e.v("t_i32")),
                        EcoBuzz.field(e, "loop", e.v("t_i32")))));
        exprs.add(e.let(
                "wave_table_t",
                EcoBuzz.struct(
                        e,
                        "bz_wave_table",
                        EcoBuzz.field(e, "w", EcoBuzz.array(e, e.v("wave_t"), e.intOf(MAX_WAVES))))));
        exprs.add(e.let("p_wave_table_t", EcoBuzz.ptr(e, e.v("wave_table_t"))));
        exprs.add(e.let(
                "wave_read_fn_t",
                EcoBuzz.fnType(e, e.v("t_f32"), e.v("p_wave_table_t"), e.v("t_i32"), e.v("t_f32"), e.v("t_i32"))));
        exprs.add(e.let("zero_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("t_pf32"), e.v("t_i32"))));
        exprs.add(e.let(
                "mix_add_fn_t",
                EcoBuzz.fnType(e, e.v("t_void"), e.v("t_pf32"), e.v("t_pf32"), e.v("t_i32"), e.v("t_f32"))));
        exprs.add(e.let("note_rate_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_i32"), e.v("t_i32"))));
        exprs.add(e.let("exp2f_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));
        exprs.add(e.let("osc_init_fn_t", EcoBuzz.fnType(e, e.v("t_void"))));
        exprs.add(e.let("osc_at_fn_t", EcoBuzz.fnType(e, e.v("t_i32"), e.v("t_i32"), e.v("t_i32"))));
        exprs.add(e.let("osctab_t", EcoBuzz.array(e, e.v("t_i32"), e.intOf(OSC_WAVES * OSC_STRIDE))));
        exprs.add(e.let("sinf_fn_t", EcoBuzz.fnType(e, e.v("t_f32"), e.v("t_f32"))));

        exprs.add(e.let("d", e.define()));
        exprs.add(e.optionTypeLet());
        exprs.add(e.let("types_o", e.defObject(e.v("d"), "buzz llvm types")));
        var typeFields = new TList<EcoMb.P>();
        for (var name : EcoBuzz.TYPE_NAMES) typeFields.add(e.p(name, e.mb.evalType()));
        exprs.add(e.objectImpl(e.v("types_o"), e.rec(typeFields.toArray(EcoMb.P[]::new))));
        for (var name : EcoBuzz.TYPE_NAMES) exprs.add(accessor(e, "types_o", name, e.mb.evalType()));
        exprs.add(e.let("dsp_o", e.defObject(e.v("d"), "buzz dsp")));
        var inner = new TList<EcoMb.P>();
        inner.add(e.p("m", EcoBuzz.llvm(e, "mod_t")));
        inner.add(e.p("types", e.fld(e.v("types_o"), "type")));
        inner.add(e.p("range_i32", e.v("t_range_i32")));
        inner.add(e.p("loop", EcoBuzz.llvm(e, "loop")));
        for (var fn : FNS) inner.add(e.p(fn, EcoBuzz.globalT(e, e.v(fn + "_fn_t"))));
        exprs.add(e.objectImpl(e.v("dsp_o"), e.rec(inner.toArray(EcoMb.P[]::new))));
        exprs.add(accessor(e, "dsp_o", "m", EcoBuzz.llvm(e, "mod_t")));
        exprs.add(accessor(e, "dsp_o", "range_i32", e.v("t_range_i32")));
        exprs.add(accessor(e, "dsp_o", "loop", EcoBuzz.llvm(e, "loop")));
        exprs.add(accessor(e, "dsp_o", "types", e.fld(e.v("types_o"), "type")));
        for (var fn : FNS) exprs.add(accessor(e, "dsp_o", fn, EcoBuzz.globalT(e, e.v(fn + "_fn_t"))));

        exprs.add(e.let("dsp_b", e.defFunc(e.v("d"), "buzz dsp")));
        var body = new TList<SourceExpr>();
        body.add(e.let("ll", e.fld(e.v("arg"), "m")));
        var typeInit = new TList<EcoMb.P>();
        for (var name : EcoBuzz.TYPE_NAMES) typeInit.add(e.p(name, e.lowerV(name)));
        EcoBuzz.rebindTypes(
                e,
                body,
                e.call(e.lower(e.fld(e.v("types_o"), "new")), e.rec(typeInit.toArray(EcoMb.P[]::new))),
                e.fld(e.v("arg"), "range_i32"),
                e.fld(e.v("arg"), "loop"));
        body.add(e.let("exp2f", EcoBuzz.externDef(e, e.v("ll"), "exp2f", e.lowerV("exp2f_fn_t"))));
        body.add(e.let("sinf", EcoBuzz.externDef(e, e.v("ll"), "sinf", e.lowerV("sinf_fn_t"))));
        body.add(e.let(
                "osctab",
                e.call(
                        EcoBuzz.llvmL(e, "global_zero_fn"),
                        e.rec(e.p("mod", e.v("ll")), e.p("name", e.s("bz_osctab")), e.p("ty", e.lowerV("osctab_t"))))));

        var w = idx(e, fld(e, e.v("waves"), "w"), e.v("wi"));
        var waveRead = e.seq(
                e.let("i", i32(e, e.v("pos"))),
                e.let("frac", sub(e, e.v("pos"), f32(e, e.v("i")))),
                e.let("chn", fld(e, w, "channels")),
                e.let("frames", fld(e, w, "frames")),
                e.let("data", fld(e, w, "data")),
                e.let(
                        "i1",
                        e.ifElse(lt(e, add(e, e.v("i"), e.n(1)), e.v("frames")), add(e, e.v("i"), e.n(1)), e.v("i"))),
                e.let("s0", sample16(e, e.v("data"), mul(e, add(e, mul(e, e.v("i"), e.v("chn")), e.v("ch")), e.n(2)))),
                e.let("s1", sample16(e, e.v("data"), mul(e, add(e, mul(e, e.v("i1"), e.v("chn")), e.v("ch")), e.n(2)))),
                e.ret(add(e, e.v("s0"), mul(e, sub(e, e.v("s1"), e.v("s0")), e.v("frac")))));
        body.add(e.let(
                "wave_read",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bz_wave_read",
                        new String[] {"waves", "wi", "pos", "ch"},
                        e.lowerV("wave_read_fn_t"),
                        waveRead)));
        body.add(e.let(
                "zero",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bz_zero",
                        new String[] {"dst", "n"},
                        e.lowerV("zero_fn_t"),
                        forRange(
                                e,
                                "i",
                                mul(e, e.v("n"), e.n(2)),
                                set(e, idx(e, e.v("dst"), e.v("i")), f32(e, e.n(0)))))));
        body.add(e.let(
                "mix_add",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bz_mix_add",
                        new String[] {"dst", "src", "n", "amp"},
                        e.lowerV("mix_add_fn_t"),
                        forRange(
                                e,
                                "i",
                                mul(e, e.v("n"), e.n(2)),
                                set(
                                        e,
                                        idx(e, e.v("dst"), e.v("i")),
                                        add(
                                                e,
                                                idx(e, e.v("dst"), e.v("i")),
                                                mul(e, idx(e, e.v("src"), e.v("i")), e.v("amp"))))))));
        var midi = sub(
                e,
                add(e, mul(e, e.mb.operatorShr(e.v("note"), e.n(4)), e.n(12)), and(e, e.v("note"), e.n(15))),
                e.n(1));
        body.add(e.let(
                "note_rate",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bz_note_rate",
                        new String[] {"note", "root"},
                        e.lowerV("note_rate_fn_t"),
                        e.seq(
                                e.let("semis", f32(e, sub(e, midi, e.v("root")))),
                                e.ret(EcoBuzz.call(e, "exp2f", EcoBuzz.div(e, e.v("semis"), f32(e, e.n(12)))))))));
        var i = e.v("i");
        var k = e.v("k");
        var kk = e.v("kk");
        var wave = e.v("wave");
        var nn = e.v("nn");
        var ph = e.v("ph");
        var acc = e.v("acc");
        SourceExpr partial = EcoBuzz.call(e, "sinf", mul(e, mul(e, ph, f32(e, kk)), f32(e, e.n(2 * Math.PI))));
        body.add(e.let(
                "osc_init",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bz_osc_init",
                        new String[] {},
                        e.lowerV("osc_init_fn_t"),
                        e.seq(
                                e.let("seed", EcoBuzz.i32(e, e.n(12345))),
                                forRange(
                                        e,
                                        "wave",
                                        e.n(OSC_WAVES),
                                        forRange(
                                                e,
                                                "lv",
                                                e.n(OSC_LEVELS),
                                                e.let("nn", e.mb.operatorShr(EcoBuzz.i32(e, e.n(OSC_BASE)), e.v("lv"))),
                                                e.let(
                                                        "off",
                                                        add(
                                                                e,
                                                                mul(e, wave, e.n(OSC_STRIDE)),
                                                                EcoBuzz.and(
                                                                        e,
                                                                        EcoBuzz.i32(e, e.n(OSC_STRIDE)),
                                                                        e.mb.operatorXor(
                                                                                e.mb.operatorShr(
                                                                                        EcoBuzz.i32(e, e.n(OSC_STRIDE)),
                                                                                        e.v("lv")),
                                                                                e.n(-1))))),
                                                forRange(
                                                        e,
                                                        "i",
                                                        nn,
                                                        e.let("ph", EcoBuzz.div(e, f32(e, i), f32(e, nn))),
                                                        e.let("acc", f32(e, e.n(0))),
                                                        ifOnly(
                                                                e,
                                                                eq(e, wave, e.n(0)),
                                                                set(
                                                                        e,
                                                                        acc,
                                                                        EcoBuzz.call(
                                                                                e,
                                                                                "sinf",
                                                                                mul(e, ph, f32(e, e.n(2 * Math.PI)))))),
                                                        ifOnly(
                                                                e,
                                                                EcoBuzz.or(e, eq(e, wave, e.n(1)), eq(e, wave, e.n(5))),
                                                                forRange(
                                                                        e,
                                                                        "k",
                                                                        sub(e, EcoBuzz.div(e, nn, e.n(2)), e.n(1)),
                                                                        e.let("kk", add(e, k, e.n(1))),
                                                                        set(
                                                                                e,
                                                                                acc,
                                                                                add(
                                                                                        e,
                                                                                        acc,
                                                                                        EcoBuzz.div(
                                                                                                e,
                                                                                                mul(
                                                                                                        e,
                                                                                                        partial,
                                                                                                        e.ifElse(
                                                                                                                eq(
                                                                                                                        e,
                                                                                                                        EcoBuzz
                                                                                                                                .and(
                                                                                                                                        e,
                                                                                                                                        kk,
                                                                                                                                        e
                                                                                                                                                .n(
                                                                                                                                                        1)),
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        1)),
                                                                                                                f32(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        1)),
                                                                                                                f32(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        -1)))),
                                                                                                f32(e, kk))))),
                                                                set(e, acc, mul(e, acc, e.n(2 / Math.PI)))),
                                                        ifOnly(
                                                                e,
                                                                eq(e, wave, e.n(2)),
                                                                forRange(
                                                                        e,
                                                                        "k",
                                                                        EcoBuzz.div(e, nn, e.n(4)),
                                                                        e.let("kk", add(e, mul(e, k, e.n(2)), e.n(1))),
                                                                        set(
                                                                                e,
                                                                                acc,
                                                                                add(
                                                                                        e,
                                                                                        acc,
                                                                                        EcoBuzz.div(
                                                                                                e,
                                                                                                partial,
                                                                                                f32(e, kk))))),
                                                                set(e, acc, mul(e, acc, e.n(4 / Math.PI)))),
                                                        ifOnly(
                                                                e,
                                                                eq(e, wave, e.n(3)),
                                                                forRange(
                                                                        e,
                                                                        "k",
                                                                        EcoBuzz.div(e, nn, e.n(4)),
                                                                        e.let("kk", add(e, mul(e, k, e.n(2)), e.n(1))),
                                                                        set(
                                                                                e,
                                                                                acc,
                                                                                add(
                                                                                        e,
                                                                                        acc,
                                                                                        EcoBuzz.div(
                                                                                                e,
                                                                                                mul(
                                                                                                        e,
                                                                                                        partial,
                                                                                                        e.ifElse(
                                                                                                                eq(
                                                                                                                        e,
                                                                                                                        EcoBuzz
                                                                                                                                .and(
                                                                                                                                        e,
                                                                                                                                        k,
                                                                                                                                        e
                                                                                                                                                .n(
                                                                                                                                                        1)),
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        0)),
                                                                                                                f32(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        1)),
                                                                                                                f32(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        -1)))),
                                                                                                f32(
                                                                                                        e,
                                                                                                        mul(
                                                                                                                e, kk,
                                                                                                                kk)))))),
                                                                set(e, acc, mul(e, acc, e.n(8 / (Math.PI * Math.PI))))),
                                                        ifOnly(
                                                                e,
                                                                eq(e, wave, e.n(4)),
                                                                set(
                                                                        e,
                                                                        e.v("seed"),
                                                                        add(
                                                                                e,
                                                                                mul(e, e.v("seed"), e.n(1103515245)),
                                                                                e.n(12345))),
                                                                set(
                                                                        e,
                                                                        acc,
                                                                        mul(
                                                                                e,
                                                                                f32(
                                                                                        e,
                                                                                        sub(
                                                                                                e,
                                                                                                EcoBuzz.and(
                                                                                                        e,
                                                                                                        e.mb
                                                                                                                .operatorUshr(
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "seed"),
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        15)),
                                                                                                        e.n(65535)),
                                                                                                e.n(32768))),
                                                                                e.n(1.0 / 32768)))),
                                                        e.let(
                                                                "clamped",
                                                                e.ifElse(
                                                                        EcoBuzz.gt(e, acc, e.n(1)),
                                                                        f32(e, e.n(1)),
                                                                        e.ifElse(
                                                                                lt(e, acc, e.n(-1)),
                                                                                f32(e, e.n(-1)),
                                                                                acc))),
                                                        set(
                                                                e,
                                                                idx(e, lw(e, "osctab"), add(e, e.v("off"), i)),
                                                                i32(e, mul(e, e.v("clamped"), e.n(32767)))))))))));
        body.add(e.let(
                "osc_at",
                EcoBuzz.funcDef(
                        e,
                        e.v("ll"),
                        "bz_osc_at",
                        new String[] {"wave", "index"},
                        e.lowerV("osc_at_fn_t"),
                        e.ret(idx(e, lw(e, "osctab"), add(e, mul(e, e.v("wave"), e.n(OSC_STRIDE)), e.v("index")))))));
        var ctor = new TList<EcoMb.P>();
        ctor.add(e.p("m", e.v("ll")));
        ctor.add(e.p("types", e.v("ty")));
        ctor.add(e.p("range_i32", e.v("range_i32")));
        ctor.add(e.p("loop", e.v("loop")));
        for (var fn : FNS) ctor.add(e.p(fn, e.v(fn)));
        body.add(e.ret(e.call(e.lower(e.fld(e.v("dsp_o"), "new")), e.rec(ctor.toArray(EcoMb.P[]::new)))));
        exprs.add(e.funcImpl(
                e.v("dsp_b"),
                e.call(
                        e.mb.builtin("types", "record"),
                        e.rec(
                                e.p("m", EcoBuzz.llvm(e, "mod_t")),
                                e.p("range_i32", e.v("t_range_i32")),
                                e.p("loop", EcoBuzz.llvm(e, "loop")))),
                e.optionTypeSome(e.fld(e.v("dsp_o"), "type")),
                e.mb.optNoneType(),
                e.stage(e.seq(body.toArray(SourceExpr[]::new)))));
        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.rec(
                e.p("new", e.fld(e.v("dsp_b"), "func")),
                e.p("dsp_t", e.fld(e.v("dsp_o"), "type")),
                e.p("wave_t", e.v("wave_t")),
                e.p("wave_table_t", e.v("wave_table_t")))));
        return exprs;
    }
}
