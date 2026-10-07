package compilerxyz.main.tests;

import static compilerxyz.main.tests.EcoBuzz.add;
import static compilerxyz.main.tests.EcoBuzz.div;
import static compilerxyz.main.tests.EcoBuzz.eq;
import static compilerxyz.main.tests.EcoBuzz.f32;
import static compilerxyz.main.tests.EcoBuzz.fld;
import static compilerxyz.main.tests.EcoBuzz.forRange;
import static compilerxyz.main.tests.EcoBuzz.ge;
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
import compilerxyz.prelude.util.IMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

final class EcoBuzzGunpowder {
    private EcoBuzzGunpowder() {}

    static final double RATE = 44100.0;
    static final int SPT = (int) (60.0 * RATE / (EcoBuzzGunpowderData.BPM * EcoBuzzGunpowderData.TPB));
    static final int BUF_FLOATS = EcoBuzz.MAX_BLOCK * 2;
    private static final int WAV_HEADER = 44;

    private record Port(String stem, String module, int globals, boolean trackArray) {}

    private static final IMap<String, Port> PORTS = IMap.ofEntries(
            new Pair<>(
                    "BTDSys SampleGrid - byte x 16", new Port("sg", "buzz-samplegrid.atz", EcoBuzzSampleGrid.NG, true)),
            new Pair<>("Fuzzpilz UnwieldyTracker", new Port("ut", "buzz-unwieldytracker.atz", 0, false)),
            new Pair<>("M4", new Port("m4", "buzz-m4.atz", EcoBuzzM4.NG, true)),
            new Pair<>("FSM Infector", new Port("inf", "buzz-infector.atz", EcoBuzzInfector.NG, true)),
            new Pair<>("FSM TunaMan (Stereo)", new Port("tm", "buzz-tunaman.atz", EcoBuzzTunaMan.NG, true)),
            new Pair<>("Jeskola NiNjA dElaY (Stereo)", new Port("nj", "buzz-ninja.atz", EcoBuzzNinja.NG, true)),
            new Pair<>("m4wii", new Port("mw", "buzz-m4wii.atz", EcoBuzzM4wII.NG, true)),
            new Pair<>("Arguelles kAway", new Port("ka", "buzz-kaway.atz", EcoBuzzKAway.NG, true)));
    private static final IMap<String, Port> VST_PORTS = IMap.ofEntries(
            new Pair<>("Classic EQ", new Port("ceq", "buzz-classiceq.atz", EcoBuzzVst.NG, true)),
            new Pair<>("KarmaFX Filter", new Port("kfx", "buzz-karmafilter.atz", EcoBuzzVst.NG, true)),
            new Pair<>("KarmaFX Filter2", new Port("kfx", "buzz-karmafilter.atz", EcoBuzzVst.NG, true)),
            new Pair<>("KarmaFX Filter3", new Port("kfx", "buzz-karmafilter.atz", EcoBuzzVst.NG, true)),
            new Pair<>("BloodOverdrive", new Port("bod", "buzz-bloodoverdrive.atz", EcoBuzzVst.NG, true)),
            new Pair<>("Blue Cat Phaser VST(", new Port("bcp", "buzz-bluecatphaser.atz", EcoBuzzVst.NG, true)));

    private record Inst(String id, EcoBuzzGunpowderData.Machine data, Opt<Port> port, boolean generatorType) {}

    private static final class Setup {
        final TList<SourceExpr> exprs = new TList<>();
        final TList<String> names = new TList<>();

        void def(EcoMb e, String name, SourceExpr value) {
            exprs.add(e.let(name, value));
            names.add(name);
        }
    }

    private static String ident(String name) {
        var sb = new StringBuilder();
        for (var c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));
            else sb.append('_');
        }
        return sb.toString();
    }

    private static List<EcoBuzzGunpowderData.Conn> inputs(String name) {
        var out = new ArrayList<EcoBuzzGunpowderData.Conn>();
        for (var c : EcoBuzzGunpowderData.CONNS) if (c.dst().equals(name)) out.add(c);
        return out;
    }

    private static SourceExpr u8At(EcoMb e, SourceExpr arr, int off) {
        return EcoBuzz.and(e, e.mb.as(e.call(e.fld(arr, "get"), e.n(off)), e.mb.typesInt()), e.n(255));
    }

    private static SourceExpr u16At(EcoMb e, SourceExpr arr, int off) {
        return EcoBuzz.or(e, u8At(e, arr, off), EcoBuzz.shl(e, u8At(e, arr, off + 1), e.n(8)));
    }

    private static SourceExpr u32At(EcoMb e, SourceExpr arr, int off) {
        return EcoBuzz.or(e, u16At(e, arr, off), EcoBuzz.shl(e, u16At(e, arr, off + 2), e.n(16)));
    }

    private static int tag(String s) {
        return (s.charAt(0) & 0xFF)
                | ((s.charAt(1) & 0xFF) << 8)
                | ((s.charAt(2) & 0xFF) << 16)
                | ((s.charAt(3) & 0xFF) << 24);
    }

    private static SourceExpr check(EcoMb e, SourceExpr cond, SourceExpr path, String message) {
        return e.ifOnly(e.not(cond), e.mb.panic(e.cat(path, e.s(message))));
    }

    private static final IList<String> WAV_FIELDS = IList.of("ch", "rate", "len", "frames");

    private static SourceExpr evField(EcoMb e, SourceExpr ev, SourceExpr base, int k) {
        SourceExpr acc = EcoBuzz.i32(e, e.n(0));
        for (int d = 0; d < ENC_DIGITS; d++) {
            var digit = sub(
                    e,
                    EcoBuzz.i32(e, idx(e, ev, add(e, mul(e, add(e, base, e.n(k)), e.n(ENC_DIGITS)), e.n(d)))),
                    e.n(ENC_ZERO));
            acc = add(e, mul(e, acc, e.n(ENC_BASE)), digit);
        }
        return acc;
    }

    private static SourceExpr llvm(EcoMb e, String name) {
        return e.fld(e.v("llvm"), name);
    }

    private static SourceExpr fnDef(EcoMb e, String name, String[] params, String fnType, SourceExpr body) {
        var names = new TList<SourceExpr>();
        for (var p : params) names.add(e.s(p));
        return e.let(
                name,
                e.call(
                        llvm(e, "func_fn"),
                        e.rec(
                                e.p("mod", e.v("ll")),
                                e.p("name", e.s(name)),
                                e.p("params", e.tup(names.toArray(SourceExpr[]::new))),
                                e.p("ty", e.v(fnType)),
                                e.p("body", EcoBuzz.body(e, body)))));
    }

    private static SourceExpr globalZero(EcoMb e, String name, SourceExpr ty) {
        return e.let(
                name,
                e.call(llvm(e, "global_zero_fn"), e.rec(e.p("mod", e.v("ll")), e.p("name", e.s(name)), e.p("ty", ty))));
    }

    private static SourceExpr globalInit(EcoMb e, String name, SourceExpr ty, SourceExpr init) {
        return e.let(
                name,
                e.call(
                        llvm(e, "global_fn"),
                        e.rec(e.p("mod", e.v("ll")), e.p("name", e.s(name)), e.p("ty", ty), e.p("init", init))));
    }

    private static SourceExpr globalConst(EcoMb e, String name, SourceExpr ty, SourceExpr init) {
        return e.let(
                name,
                e.call(
                        llvm(e, "global_const_fn"),
                        e.rec(e.p("mod", e.v("ll")), e.p("name", e.s(name)), e.p("ty", ty), e.p("init", init))));
    }

    private static SourceExpr constList(EcoMb e, String name, SourceExpr values) {
        return e.let(
                name,
                e.call(
                        e.v("const_list_i8"),
                        e.rec(e.p("mod", e.v("ll")), e.p("name", e.s(name)), e.p("values", values))));
    }

    private static SourceExpr addrOf(EcoMb e, String pname, String globalName, SourceExpr ptrGlobalTy) {
        return e.let(
                pname, e.call(llvm(e, "ref_fn"), e.rec(e.p("ref", e.s("@" + globalName)), e.p("ty", ptrGlobalTy))));
    }

    private static SourceExpr cstr(EcoMb e, String bind, String content) {
        return e.let(bind, e.callM(e.v("ll"), "cstr", e.s(content)));
    }

    private static final int ENC_BASE = 64;
    private static final int ENC_ZERO = 48;
    private static final int ENC_DIGITS = 3;

    private static final IList<String> PW_NAMES = IList.of(
            "pw_init",
            "pw_deinit",
            "pw_main_loop_new",
            "pw_main_loop_get_loop",
            "pw_main_loop_run",
            "pw_main_loop_quit",
            "pw_main_loop_destroy",
            "pw_stream_new_simple",
            "pw_stream_connect",
            "pw_properties_new",
            "direction_output",
            "id_any",
            "flags_autoconnect_map_rt",
            "params_audio_stereo_44k1");

    private static final IList<String> EVENT_FIELDS = IList.of(
            "destroy",
            "state_changed",
            "control_info",
            "io_changed",
            "param_changed",
            "add_buffer",
            "remove_buffer",
            "process",
            "drained",
            "command",
            "trigger_done");

    private static SourceExpr ptrGlobalT(EcoMb e, String ptrTypeName) {
        return e.call(llvm(e, "global_t"), e.rec(e.p("ty", e.v(ptrTypeName)), e.p("mem", e.b(false))));
    }

    public static TList<SourceExpr> module(EcoMb e, EcoRootMode mode) {
        var byName = new HashMap<String, Inst>();
        var all = new ArrayList<Inst>();
        for (var m : EcoBuzzGunpowderData.MACHINES) {
            var inst = new Inst(
                    ident(m.name()),
                    m,
                    m.dll().equals("Polac VST 1.1") ? VST_PORTS.getOpt(m.name()) : PORTS.getOpt(m.dll()),
                    (!(m.dll().contains("TunaMan")
                            || m.dll().contains("Polac VST 1.1")
                            || m.dll().contains("NiNjA")
                            || m.dll().equals("Master"))));
            byName.put(m.name(), inst);
            all.add(inst);
        }
        var reach = new HashSet<String>();
        var frontier = new ArrayList<String>();
        frontier.add("Master");
        while (!frontier.isEmpty()) {
            var cur = frontier.remove(frontier.size() - 1);
            for (var c : EcoBuzzGunpowderData.CONNS) {
                if (c.dst().equals(cur) && reach.add(c.src())) frontier.add(c.src());
            }
        }
        var indeg = new HashMap<String, Integer>();
        for (var n : reach) indeg.put(n, 0);
        for (var c : EcoBuzzGunpowderData.CONNS) {
            if (reach.contains(c.src()) && reach.contains(c.dst())) {
                indeg.put(c.dst(), indeg.getOrDefault(c.dst(), 0) + 1);
            }
        }
        var insts = new ArrayList<Inst>();
        var ready = new ArrayList<String>();
        for (var inst : all) {
            if (reach.contains(inst.data().name())
                    && indeg.getOrDefault(inst.data().name(), -1) == 0) {
                ready.add(inst.data().name());
            }
        }
        while (!ready.isEmpty()) {
            var n = ready.remove(0);
            insts.add(Objects.requireNonNull(byName.get(n)));
            for (var c : EcoBuzzGunpowderData.CONNS) {
                if (c.src().equals(n) && reach.contains(c.dst()) && !c.dst().equals("Master")) {
                    var d = indeg.getOrDefault(c.dst(), 0) - 1;
                    indeg.put(c.dst(), d);
                    if (d == 0) ready.add(c.dst());
                }
            }
        }
        var k = new Setup();
        var pre = new TList<SourceExpr>();
        EcoBuzz.prelude(e, pre);
        for (var expr : pre) k.exprs.add(expr);
        k.names.add("llvm");
        for (var t : new String[] {"i1", "i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "f32", "f64", "void"}) {
            k.names.add("t_" + t);
        }
        for (var t : new String[] {"t_pv", "t_pu8", "t_pi8", "t_pf32", "t_pi32", "t_range_i32"}) k.names.add(t);
        k.def(e, "ll", e.call(llvm(e, "new"), e.tup()));
        k.def(e, "pw", e.call(e.importLocal("pipewire", "pipewire-1.6.atz"), e.v("ll")));
        k.def(e, "dsp_m", e.importLocal("dsp", "buzz-dsp.atz"));
        k.def(e, "wave_t", e.fld(e.v("dsp_m"), "wave_t"));
        k.def(e, "wave_table_t", e.fld(e.v("dsp_m"), "wave_table_t"));
        var usedPorts = new HashSet<String>();
        for (var inst : insts) {
            if (inst.port().optIsSome() && usedPorts.add(inst.port().optUnwrap().stem())) {
                var p = inst.port().optUnwrap();
                k.def(e, p.stem() + "_m", e.importLocal(p.stem(), p.module()));
                k.def(e, p.stem() + "_state_t", e.fld(e.v(p.stem() + "_m"), "state_t"));
                k.def(e, p.stem() + "_p_state_t", e.call(llvm(e, "ptr"), e.v(p.stem() + "_state_t")));
                k.def(e, p.stem() + "_p_state_g", ptrGlobalT(e, p.stem() + "_p_state_t"));
            }
        }
        k.def(e, "t_events", e.callM0(e.v("pw"), "pw_stream_events"));
        k.def(
                e,
                "t_buf_audio",
                e.callM(e.v("pw"), "buffer_t", e.rec(e.p("name", e.s("audio")), e.p("payload", e.v("t_f32")))));
        for (var op : new String[] {"dequeue", "queue"}) {
            k.def(e, op + "_fn_t", e.callM(e.v("pw"), op + "_buffer_t", e.v("t_buf_audio")));
        }
        {
            var frames = new TList<EcoMb.P>();
            for (var w : EcoBuzzGunpowderData.WAVES) frames.add(e.p(w.path(), e.intOf(w.frames())));
            k.exprs.add(e.let(
                    "wav_expected", e.mapNew(e.mb.typesString(), e.mb.typesIntBox(), frames.toArray(EcoMb.P[]::new))));
            for (var f : WAV_FIELDS) k.exprs.add(e.let("wav_" + f, e.mapNew(e.mb.typesString(), e.mb.typesIntBox())));
            var hdr = e.v("h");
            for (var w : EcoBuzzGunpowderData.WAVES) {
                var path = e.s(w.path());
                k.exprs.add(e.seq(
                        e.let(
                                "h",
                                e.callM(
                                        e.importLocal("f", w.path()),
                                        "read",
                                        e.rec(e.p("offset", e.intOf(0)), e.p("len", e.intOf(WAV_HEADER))))),
                        check(e, eq(e, u32At(e, hdr, 0), e.n(tag("RIFF"))), path, ": not a RIFF file"),
                        check(e, eq(e, u32At(e, hdr, 8), e.n(tag("WAVE"))), path, ": not a WAVE file"),
                        check(e, eq(e, u32At(e, hdr, 12), e.n(tag("fmt "))), path, ": fmt chunk not first"),
                        check(e, eq(e, u32At(e, hdr, 16), e.n(16)), path, ": fmt chunk is not 16 bytes"),
                        check(e, eq(e, u16At(e, hdr, 20), e.n(1)), path, ": not PCM"),
                        check(e, eq(e, u16At(e, hdr, 34), e.n(16)), path, ": not 16-bit"),
                        check(e, eq(e, u32At(e, hdr, 36), e.n(tag("data"))), path, ": data chunk not at 36"),
                        e.let("ch", u16At(e, hdr, 22)),
                        e.let("rate", u32At(e, hdr, 24)),
                        e.let("len", u32At(e, hdr, 40)),
                        e.let("frames", div(e, e.v("len"), mul(e, e.v("ch"), e.n(2)))),
                        check(
                                e,
                                eq(
                                        e,
                                        e.v("frames"),
                                        e.mb.as(e.callM(e.v("wav_expected"), "get", path), e.mb.typesInt())),
                                path,
                                ": frame count differs from the song's wave table"),
                        e.callM(e.v("wav_ch"), "put", e.tup(path, e.v("ch"))),
                        e.callM(e.v("wav_rate"), "put", e.tup(path, e.v("rate"))),
                        e.callM(e.v("wav_len"), "put", e.tup(path, e.v("len"))),
                        e.callM(e.v("wav_frames"), "put", e.tup(path, e.v("frames")))));
            }
        }
        for (var w : EcoBuzzGunpowderData.WAVES) {
            for (var f : WAV_FIELDS) {
                k.def(
                        e,
                        "w" + f + "_" + w.index(),
                        e.mb.as(e.callM(e.v("wav_" + f), "get", e.s(w.path())), e.mb.typesInt()));
            }
            k.def(
                    e,
                    "wb_" + w.index(),
                    e.callM(
                            e.importLocal("f", w.path()),
                            "read",
                            e.rec(e.p("offset", e.intOf(WAV_HEADER)), e.p("len", e.v("wlen_" + w.index())))));
        }
        k.def(
                e,
                "buf_t",
                e.call(llvm(e, "array"), e.rec(e.p("elem", e.v("t_f32")), e.p("count", e.intOf(BUF_FLOATS)))));
        k.def(e, "pf32_g", ptrGlobalT(e, "t_pf32"));
        k.def(e, "pi32_g", ptrGlobalT(e, "t_pi32"));
        k.def(e, "pi8_g", ptrGlobalT(e, "t_pi8"));
        k.def(e, "void_fn_t", EcoBuzz.fnType(e, e.v("t_void")));
        k.def(e, "const_list_i8", e.call(llvm(e, "const_list"), e.v("t_i8")));
        k.def(e, "block_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("t_i32")));
        k.def(e, "render_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("t_pf32"), e.v("t_i32")));
        k.def(e, "cb_fn_t", EcoBuzz.fnType(e, e.v("t_void"), e.v("t_pv")));
        k.def(e, "main_fn_t", EcoBuzz.fnType(e, e.v("t_i32")));
        var fields = new TList<EcoMb.P>();
        for (var name : k.names) fields.add(e.p(name, e.v(name)));
        k.exprs.add(e.ret(e.rec(fields.toArray(EcoMb.P[]::new))));
        var x = new TList<SourceExpr>();
        x.add(e.let("k", e.lower(e.mb.seq(k.exprs))));
        for (var name : k.names) x.add(e.let(name, e.fld(e.v("k"), name)));
        x.add(e.let("range_i32", e.v("t_range_i32")));
        x.add(e.let("loop", e.call(llvm(e, "ref_fn"), e.rec(e.p("ref", e.s("loop")), e.p("ty", llvm(e, "loop"))))));
        x.add(e.let(
                "dsp",
                e.call(
                        e.fld(e.v("dsp_m"), "new"),
                        e.rec(e.p("m", e.v("ll")), e.p("range_i32", e.v("range_i32")), e.p("loop", e.v("loop"))))));
        var usedPortsTop = new HashSet<String>();
        for (var inst : insts) {
            if (inst.port().optIsSome()
                    && usedPortsTop.add(inst.port().optUnwrap().stem())) {
                var p = inst.port().optUnwrap();
                x.add(e.let(p.stem(), e.call(e.fld(e.v(p.stem() + "_m"), "new"), e.v("dsp"))));
                for (var fn : new String[] {"init", "tick", "work"}) {
                    x.add(e.let(p.stem() + "_" + fn, e.callM0(e.v(p.stem()), fn)));
                }
            }
        }
        for (var fn : EcoBuzzDsp.FNS) x.add(e.let(fn, e.callM0(e.v("dsp"), fn)));
        x.add(e.let("nullp", e.callM0(e.v("ll"), "null")));
        for (var op : new String[] {"dequeue", "queue"}) {
            x.add(e.let(
                    op,
                    e.call(
                            llvm(e, "extern_fn"),
                            e.rec(
                                    e.p("name", e.s("pw_stream_" + op + "_buffer")),
                                    e.p("ty", e.v(op + "_fn_t")),
                                    e.p("libs", e.tup(e.s(EcoPipewire.LIB)))))));
        }
        for (var fn : PW_NAMES) x.add(e.let(fn, e.callM0(e.v("pw"), fn)));
        var firstData = "wave_" + EcoBuzzGunpowderData.WAVES.get(0).index();
        for (var w : EcoBuzzGunpowderData.WAVES) {
            x.add(constList(e, "wave_" + w.index(), e.v("wb_" + w.index())));
        }
        var entries = new SourceExpr[EcoBuzzDsp.MAX_WAVES];
        var byIndex = new HashMap<Integer, EcoBuzzGunpowderData.Wave>();
        for (var w : EcoBuzzGunpowderData.WAVES) byIndex.put(w.index(), w);
        x.add(e.let(
                "wave_empty",
                e.rec(
                        e.p("data", e.v(firstData)),
                        e.p("frames", e.intOf(0)),
                        e.p("channels", e.intOf(1)),
                        e.p("rate", e.intOf(44100)),
                        e.p("vol", e.n(0)),
                        e.p("loop_start", e.intOf(0)),
                        e.p("loop_end", e.intOf(0)),
                        e.p("loop", e.intOf(0)))));
        for (int i = 0; i < entries.length; i++) {
            var w = byIndex.get(i);
            if (w == null) {
                entries[i] = e.v("wave_empty");
            } else {
                entries[i] = e.rec(
                        e.p("data", e.v("wave_" + i)),
                        e.p("frames", e.v("wframes_" + i)),
                        e.p("channels", e.v("wch_" + i)),
                        e.p("rate", e.v("wrate_" + i)),
                        e.p("vol", e.n(w.volume())),
                        e.p("loop_start", e.intOf(w.loopStart())),
                        e.p("loop_end", e.intOf(w.loopEnd())),
                        e.p("loop", e.intOf(w.loop() ? 1 : 0)));
            }
        }
        x.add(globalConst(e, "wavetable", e.v("wave_table_t"), e.rec(e.p("w", e.tup(entries)))));
        for (var inst : insts) {
            var id = inst.id();
            x.add(globalZero(e, "buf_" + id, e.v("buf_t")));
            x.add(addrOf(e, "pbuf_" + id, "buf_" + id, e.v("pf32_g")));
            if (inst.port().optIsNone()) continue;
            var stem = inst.port().optUnwrap().stem();
            x.add(globalZero(e, "st_" + id, e.v(stem + "_state_t")));
            x.add(addrOf(e, "p_" + id, "st_" + id, e.v(stem + "_p_state_g")));
            var evs = inst.data().events();
            if (evs.size() > 0) {
                var sb = new StringBuilder();
                for (var row : evs) {
                    for (int v : row) {
                        if (v < 0 || v >= ENC_BASE * ENC_BASE * ENC_BASE)
                            throw new IllegalArgumentException("value " + v);
                        for (int d = ENC_DIGITS - 1; d >= 0; d--) {
                            sb.append((char) (ENC_ZERO + ((v / (int) Math.pow(ENC_BASE, d)) % ENC_BASE)));
                        }
                    }
                }
                x.add(constList(e, "evt_" + id, e.s(sb.toString())));
                x.add(addrOf(e, "ev_" + id, "evt_" + id, e.v("pi8_g")));
                x.add(globalInit(e, "cur_" + id, e.v("t_i32"), e.n(0)));
            }
        }
        x.add(globalZero(e, "buf_master", e.v("buf_t")));
        x.add(addrOf(e, "pbuf_master", "buf_master", e.v("pf32_g")));
        x.add(globalInit(e, "g_tick", e.v("t_i32"), e.n(0)));
        x.add(globalInit(e, "g_pos", e.v("t_i32"), e.n(0)));
        x.add(globalInit(e, "g_done", e.v("t_i32"), e.n(0)));
        x.add(globalInit(e, "g_stream", e.v("t_pv"), e.v("nullp")));
        x.add(globalInit(e, "g_mloop", e.v("t_pv"), e.v("nullp")));

        var initBody = new TList<SourceExpr>();
        initBody.add(EcoBuzz.callV(e, lw(e, "osc_init"), e.tup()));
        for (var inst : insts) {
            if (inst.port().optIsNone()) continue;
            var st = lw(e, "p_" + inst.id());
            initBody.add(EcoBuzz.callV(e, lw(e, inst.port().optUnwrap().stem() + "_init"), st));
            initBody.add(set(e, fld(e, st, "waves"), lw(e, "wavetable")));
            initBody.add(set(e, fld(e, st, "spt"), e.n(SPT)));
            var port = inst.port().optUnwrap();
            var g = inst.data().gstate();
            for (int i = 0; i < g.size() && port.globals() > 0; i++) {
                if (g.get(i) != EcoBuzz.NO_VALUE) initBody.add(set(e, idx(e, fld(e, st, "gval"), i), e.n(g.get(i))));
            }
            var ts = inst.data().tstate();
            for (int t = 0; t < ts.size(); t++) {
                for (int i = 0; i < ts.get(t).size(); i++) {
                    if (ts.get(t).get(i) == EcoBuzz.NO_VALUE) continue;
                    var place = port.trackArray()
                            ? idx(e, idx(e, fld(e, st, "tval"), t), i)
                            : idx(e, fld(e, st, "tval"), i);
                    initBody.add(set(e, place, e.n(ts.get(t).get(i))));
                }
            }
            for (var vp : EcoBuzzGunpowderData.VST_PARAMS) {
                if (!vp.machine().equals(inst.data().name())) continue;
                initBody.add(set(e, idx(e, fld(e, st, "pv"), e.n(vp.param())), e.n(vp.value())));
            }
            initBody.add(EcoBuzz.callV(e, lw(e, port.stem() + "_tick"), st));
        }
        x.add(fnDef(e, "song_init", new String[] {}, "void_fn_t", e.seq(initBody.toArray(SourceExpr[]::new))));

        var tickBody = new TList<SourceExpr>();
        for (var inst : insts) {
            if (inst.port().optIsNone()) continue;
            var id = inst.id();
            var n = inst.data().events().size();
            var port = inst.port().optUnwrap();
            if (n > 0) {
                var ev = lw(e, "ev_" + id);
                var cur = lw(e, "cur_" + id);
                var st = lw(e, "p_" + id);
                var base = e.v("base");
                var value = evField(e, ev, base, 4);
                var param = evField(e, ev, base, 3);
                var trackAssign = port.trackArray()
                        ? set(e, idx(e, idx(e, fld(e, st, "tval"), evField(e, ev, base, 2)), param), value)
                        : set(e, idx(e, fld(e, st, "tval"), param), value);
                var assign = port.globals() > 0
                        ? e.ifElse(
                                eq(e, evField(e, ev, base, 1), e.n(0)),
                                set(e, idx(e, fld(e, st, "gval"), param), value),
                                trackAssign)
                        : trackAssign;
                tickBody.add(EcoBuzz.whileLoop(
                        e,
                        "ev_" + id,
                        lt(e, cur, e.n(n)),
                        e.let("base", mul(e, cur, e.n(5))),
                        e.ifOnly(ne(e, evField(e, ev, base, 0), lw(e, "g_tick")), e.mb.returnLabel("ev_" + id)),
                        assign,
                        set(e, cur, add(e, cur, e.n(1)))));
            }
            tickBody.add(EcoBuzz.callV(e, lw(e, inst.port().optUnwrap().stem() + "_tick"), lw(e, "p_" + inst.id())));
        }
        x.add(fnDef(e, "song_tick", new String[] {}, "void_fn_t", e.seq(tickBody.toArray(SourceExpr[]::new))));

        var procBody = new TList<SourceExpr>();
        for (var inst : insts) {
            var buf = lw(e, "pbuf_" + inst.id());
            if (!inst.generatorType()) {
                procBody.add(EcoBuzz.callV(e, lw(e, "zero"), buf, e.v("n")));
                for (var c : inputs(inst.data().name())) {
                    procBody.add(EcoBuzz.callV(
                            e,
                            lw(e, "mix_add"),
                            buf,
                            lw(e, "pbuf_" + ident(c.src())),
                            e.v("n"),
                            e.n(c.amp() / 16384.0)));
                }
            }
            if (inst.port().optIsSome()) {
                procBody.add(EcoBuzz.callV(
                        e, lw(e, inst.port().optUnwrap().stem() + "_work"), lw(e, "p_" + inst.id()), buf, e.v("n")));
            } else if (inst.generatorType()) {
                procBody.add(EcoBuzz.callV(e, lw(e, "zero"), buf, e.v("n")));
            }
        }
        procBody.add(EcoBuzz.callV(e, lw(e, "zero"), lw(e, "pbuf_master"), e.v("n")));
        for (var c : inputs("Master")) {
            procBody.add(EcoBuzz.callV(
                    e,
                    lw(e, "mix_add"),
                    lw(e, "pbuf_master"),
                    lw(e, "pbuf_" + ident(c.src())),
                    e.v("n"),
                    e.n(c.amp() / 16384.0 / EcoBuzz.BUZZ_SCALE)));
        }
        x.add(fnDef(e, "song_process", new String[] {"n"}, "block_fn_t", e.seq(procBody.toArray(SourceExpr[]::new))));

        var outIdx = add(e, mul(e, e.v("off"), e.n(2)), e.v("i"));
        var render = e.seq(
                e.let("off", EcoBuzz.i32(e, e.n(0))),
                EcoBuzz.whileLoop(
                        e,
                        "rn",
                        lt(e, e.v("off"), e.v("frames")),
                        e.let("rest", sub(e, e.v("frames"), e.v("off"))),
                        e.let("chunk0", EcoBuzz.min(e, e.v("rest"), EcoBuzz.i32(e, e.n(EcoBuzz.MAX_BLOCK)))),
                        e.let("chunk", EcoBuzz.min(e, e.v("chunk0"), sub(e, EcoBuzz.i32(e, e.n(SPT)), lw(e, "g_pos")))),
                        e.ifElse(
                                ne(e, lw(e, "g_done"), e.n(0)),
                                forRange(
                                        e,
                                        "i",
                                        mul(e, e.v("chunk"), e.n(2)),
                                        set(e, idx(e, e.v("out"), outIdx), f32(e, e.n(0)))),
                                e.seq(
                                        ifOnly(e, eq(e, lw(e, "g_pos"), e.n(0)), EcoBuzz.call(e, "song_tick", e.tup())),
                                        EcoBuzz.call(e, "song_process", e.v("chunk")),
                                        forRange(
                                                e,
                                                "i",
                                                mul(e, e.v("chunk"), e.n(2)),
                                                set(
                                                        e,
                                                        idx(e, e.v("out"), outIdx),
                                                        idx(e, lw(e, "pbuf_master"), e.v("i")))),
                                        set(e, lw(e, "g_pos"), add(e, lw(e, "g_pos"), e.v("chunk"))),
                                        ifOnly(
                                                e,
                                                ge(e, lw(e, "g_pos"), e.n(SPT)),
                                                set(e, lw(e, "g_pos"), e.n(0)),
                                                set(e, lw(e, "g_tick"), add(e, lw(e, "g_tick"), e.n(1))),
                                                ifOnly(
                                                        e,
                                                        ge(e, lw(e, "g_tick"), e.n(EcoBuzzGunpowderData.SONG_END)),
                                                        set(e, lw(e, "g_done"), e.n(1)))))),
                        set(e, e.v("off"), add(e, e.v("off"), e.v("chunk")))));
        x.add(fnDef(e, "song_render", new String[] {"out", "frames"}, "render_fn_t", render));

        var onProcess = e.seq(
                e.let("buf", EcoBuzz.callV(e, lw(e, "dequeue"), lw(e, "g_stream"))),
                ifOnly(
                        e,
                        ne(e, e.v("buf"), lw(e, "nullp")),
                        e.let("datas", e.fld(e.v("buf"), "buffer", "datas")),
                        e.let("out", e.fld(e.v("datas"), "data")),
                        ifOnly(
                                e,
                                ne(e, e.v("out"), lw(e, "nullp")),
                                e.let("maxframes", div(e, e.fld(e.v("datas"), "maxsize"), e.n(8))),
                                e.let("req", e.fld(e.v("buf"), "requested")),
                                e.let(
                                        "n0",
                                        e.ifElse(
                                                eq(e, e.v("req"), e.n(0)),
                                                e.v("maxframes"),
                                                EcoBuzz.conv(e, "t_u32", e.v("req")))),
                                e.let(
                                        "n",
                                        e.ifElse(
                                                EcoBuzz.gt(e, e.v("n0"), e.v("maxframes")),
                                                e.v("maxframes"),
                                                e.v("n0"))),
                                EcoBuzz.call(e, "song_render", e.v("out"), EcoBuzz.i32(e, e.v("n"))),
                                e.let("chunk", e.fld(e.v("datas"), "chunk")),
                                set(e, e.fld(e.v("chunk"), "offset"), e.n(0)),
                                set(e, e.fld(e.v("chunk"), "size"), mul(e, e.v("n"), e.n(8))),
                                set(e, e.fld(e.v("chunk"), "stride"), e.n(8))),
                        EcoBuzz.callV(e, lw(e, "queue"), lw(e, "g_stream"), e.v("buf"))),
                ifOnly(e, ne(e, lw(e, "g_done"), e.n(0)), EcoBuzz.call(e, "pw_main_loop_quit", lw(e, "g_mloop"))));
        x.add(fnDef(e, "on_process", new String[] {"data"}, "cb_fn_t", onProcess));

        var eventFields = new TList<EcoMb.P>();
        eventFields.add(e.p("version", e.n(2)));
        for (var f : EVENT_FIELDS) eventFields.add(e.p(f, e.v(f.equals("process") ? "on_process" : "nullp")));
        x.add(globalConst(e, "g_events", e.v("t_events"), e.rec(eventFields.toArray(EcoMb.P[]::new))));
        x.add(cstr(e, "s_media_type", "media.type"));
        x.add(cstr(e, "s_audio", "Audio"));
        x.add(cstr(e, "s_media_category", "media.category"));
        x.add(cstr(e, "s_playback", "Playback"));
        x.add(cstr(e, "s_media_role", "media.role"));
        x.add(cstr(e, "s_music", "Music"));
        x.add(cstr(e, "s_name", "atbuzz-out"));

        var main = e.seq(
                EcoBuzz.call(e, "song_init", e.tup()),
                EcoBuzz.call(e, "pw_init", lw(e, "nullp"), lw(e, "nullp")),
                e.let("mloop", EcoBuzz.call(e, "pw_main_loop_new", lw(e, "nullp"))),
                set(e, lw(e, "g_mloop"), e.v("mloop")),
                e.let("l", EcoBuzz.call(e, "pw_main_loop_get_loop", e.v("mloop"))),
                e.let(
                        "props",
                        EcoBuzz.call(
                                e,
                                "pw_properties_new",
                                lw(e, "s_media_type"),
                                lw(e, "s_audio"),
                                lw(e, "s_media_category"),
                                lw(e, "s_playback"),
                                lw(e, "s_media_role"),
                                lw(e, "s_music"),
                                lw(e, "nullp"))),
                e.let(
                        "stream",
                        EcoBuzz.call(
                                e,
                                "pw_stream_new_simple",
                                e.v("l"),
                                lw(e, "s_name"),
                                e.v("props"),
                                lw(e, "g_events"),
                                lw(e, "nullp"))),
                set(e, lw(e, "g_stream"), e.v("stream")),
                EcoBuzz.call(
                        e,
                        "pw_stream_connect",
                        e.v("stream"),
                        lw(e, "direction_output"),
                        lw(e, "id_any"),
                        lw(e, "flags_autoconnect_map_rt"),
                        lw(e, "params_audio_stereo_44k1"),
                        e.n(1)),
                EcoBuzz.call(e, "pw_main_loop_run", e.v("mloop")),
                EcoBuzz.call(e, "pw_main_loop_destroy", e.v("mloop")),
                EcoBuzz.call(e, "pw_deinit", e.tup()),
                e.ret(e.n(0)));
        x.add(fnDef(e, "main", new String[] {}, "main_fn_t", main));
        x.add(mode.ecoRootModeFinish(e, e.v("llvm"), e.v("ll")));
        return x;
    }
}
