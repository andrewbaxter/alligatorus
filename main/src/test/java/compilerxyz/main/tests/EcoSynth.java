package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoSynth {
    private EcoSynth() {}

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double AMP = 0.2;
    private static final double RATE = 48000.0;

    private static SourceExpr lo(EcoMb e, String name) {
        return e.lower(e.v(name));
    }

    private static final IList<String> LLVM_TYPES = IList.of("i32", "u32", "f32", "f64", "void");

    private interface Refs {
        SourceExpr refsRef(String name);

        static Refs setup(EcoMb e) {
            return e::v;
        }

        static Refs top(EcoMb e) {
            return name -> e.fld(e.v("k"), name);
        }
    }

    private static SourceExpr ty(EcoMb e, Refs refs, String name) {
        var base = name.substring("t_".length());
        if (LLVM_TYPES.contains(base)) {
            return e.fld(refs.refsRef("llvm"), base);
        }
        return refs.refsRef(name);
    }

    private record Cx(TList<SourceExpr> setup, TList<EcoMb.P> fields, TList<SourceExpr> top) {}

    private static void defType(EcoMb e, Cx cx, String name, SourceExpr typeExpr) {
        cx.fields().add(e.p(name, typeExpr));
    }

    private static void defValue(EcoMb e, Cx cx, String name, SourceExpr value) {
        cx.top().add(e.let(name, value));
    }

    private static SourceExpr callFn(EcoMb e, String fn, SourceExpr arg) {
        return e.call(lo(e, fn), arg);
    }

    private static SourceExpr callFn(EcoMb e, String fn, SourceExpr... args) {
        return e.call(lo(e, fn), e.tup(args));
    }

    private static SourceExpr conv(EcoMb e, String type, SourceExpr value) {
        return e.call(e.lower(ty(e, Refs.top(e), type)), value);
    }

    private static SourceExpr set(EcoMb e, SourceExpr base, SourceExpr value) {
        return e.mb.set(base, value);
    }

    private static void cstr(EcoMb e, Cx cx, String bind, String content) {
        defValue(e, cx, bind, e.callM(Refs.top(e).refsRef("ll"), "cstr", e.s(content)));
    }

    private static void global(EcoMb e, Cx cx, String bind, String name, String type, SourceExpr init) {
        var top = Refs.top(e);
        defValue(
                e,
                cx,
                bind,
                e.call(
                        e.fld(Refs.top(e).refsRef("llvm"), "global_fn"),
                        e.rec(
                                e.p("mod", top.refsRef("ll")),
                                e.p("name", e.s(name)),
                                e.p("ty", ty(e, top, type)),
                                e.p("init", init))));
    }

    private static void func(
            EcoMb e, Cx cx, String name, String[] paramNames, String[] paramTypes, String ret, SourceExpr body) {
        var top = Refs.top(e);
        var setup = Refs.setup(e);
        var names = new TList<SourceExpr>();
        var types = new TList<SourceExpr>();
        for (int i = 0; i < paramNames.length; i++) {
            names.add(e.s(paramNames[i]));
            types.add(ty(e, setup, paramTypes[i]));
        }
        defType(
                e,
                cx,
                name + "_fn_t",
                e.callM(
                        e.v("llvm"),
                        "fn",
                        e.rec(
                                e.p("params", e.listNew(e.mb.evalType(), types.toArray(SourceExpr[]::new))),
                                e.p("ret", ty(e, setup, ret)))));
        defValue(
                e,
                cx,
                name,
                e.call(
                        e.fld(Refs.top(e).refsRef("llvm"), "func_fn"),
                        e.rec(
                                e.p("mod", top.refsRef("ll")),
                                e.p("name", e.s(name)),
                                e.p("params", e.tup(names.toArray(SourceExpr[]::new))),
                                e.p("ty", top.refsRef(name + "_fn_t")),
                                e.p("body", e.stage(body)))));
    }

    private static void events(EcoMb e, Cx cx, String bind, String name, String process) {
        var fields = new TList<EcoMb.P>();
        fields.add(e.p("version", e.n(2)));
        for (var f : new String[] {
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
            "trigger_done"
        }) {
            fields.add(e.p(f, e.v(f.equals("process") ? process : "nullp")));
        }
        var top = Refs.top(e);
        defValue(
                e,
                cx,
                bind,
                e.call(
                        e.fld(Refs.top(e).refsRef("llvm"), "global_const_fn"),
                        e.rec(
                                e.p("mod", top.refsRef("ll")),
                                e.p("name", e.s(name)),
                                e.p("ty", top.refsRef("t_events")),
                                e.p("init", e.rec(fields.toArray(EcoMb.P[]::new))))));
    }

    private static SourceExpr pwT(EcoMb e, Refs refs, String name) {
        return e.callM(refs.refsRef("pw"), name, e.tup());
    }

    public static TList<SourceExpr> module(EcoMb e, EcoRootMode mode) {
        var cx = new Cx(new TList<>(), new TList<>(), new TList<>());
        var setup = cx.setup();
        var top = cx.top();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        setup.add(e.let("ll", e.call(e.fld(e.v("llvm"), "new"), e.tup())));
        setup.add(e.let("c", e.importLocal("libc", "libc-2.42.atz")));
        setup.add(e.let("pw", e.call(e.importLocal("pipewire", "pipewire-1.6.atz"), e.v("ll"))));
        setup.add(e.let("t_pv", e.callM(e.v("llvm"), "ptr", e.fld(e.v("llvm"), "void"))));
        setup.add(e.let("t_events", pwT(e, Refs.setup(e), "pw_stream_events")));
        for (var n : new String[] {"llvm", "ll", "c", "pw", "t_pv", "t_events"})
            cx.fields().add(e.p(n, e.v(n)));
        for (var stream : new String[] {"audio", "midi"}) {
            var payload =
                    stream.equals("audio") ? e.fld(e.v("llvm"), "f32") : pwT(e, Refs.setup(e), "spa_pod_sequence");
            setup.add(e.let(
                    "t_buf_" + stream,
                    e.callM(e.v("pw"), "buffer_t", e.rec(e.p("name", e.s(stream)), e.p("payload", payload)))));
            for (var op : new String[] {"dequeue", "queue"}) {
                defType(
                        e,
                        cx,
                        op + "_" + stream + "_fn_t",
                        e.callM(e.v("pw"), op + "_buffer_t", e.v("t_buf_" + stream)));
                defValue(
                        e,
                        cx,
                        op + "_" + stream,
                        e.call(
                                e.fld(Refs.top(e).refsRef("llvm"), "extern_fn"),
                                e.rec(
                                        e.p("libs", e.tup(e.s(EcoPipewire.LIB))),
                                        e.p("name", e.s("pw_stream_" + op + "_buffer")),
                                        e.p("ty", Refs.top(e).refsRef(op + "_" + stream + "_fn_t")))));
            }
        }
        defType(e, cx, "t_range_u32", e.callM(e.v("llvm"), "range", e.fld(e.v("llvm"), "u32")));
        defValue(e, cx, "nullp", e.callM(Refs.top(e).refsRef("ll"), "null", e.tup()));
        defValue(e, cx, "range_u32", Refs.top(e).refsRef("t_range_u32"));
        defValue(
                e,
                cx,
                "loop",
                e.call(
                        e.fld(Refs.top(e).refsRef("llvm"), "ref_fn"),
                        e.rec(
                                e.p("ref", e.s("loop")),
                                e.p("ty", e.fld(Refs.top(e).refsRef("llvm"), "loop")))));

        for (var fn : new String[] {"sin", "exp2"}) {
            defValue(e, cx, fn, e.fld(Refs.top(e).refsRef("c"), fn));
        }
        for (var fn : new String[] {
            "pw_init",
            "pw_deinit",
            "pw_main_loop_new",
            "pw_main_loop_get_loop",
            "pw_main_loop_run",
            "pw_main_loop_destroy",
            "pw_stream_new_simple",
            "pw_stream_connect",
            "pw_properties_new",
            "direction_input",
            "direction_output",
            "id_any",
            "flags_autoconnect_map_rt",
            "pod_type_sequence",
            "control_midi",
            "control_ump",
            "params_audio",
            "params_midi"
        }) {
            defValue(e, cx, fn, pwT(e, Refs.top(e), fn));
        }

        global(e, cx, "g_phase", "g_phase", "t_f64", e.n(0));
        global(e, cx, "g_inc", "g_inc", "t_f64", e.n(0));
        global(e, cx, "g_gate", "g_gate", "t_f64", e.n(0));
        global(e, cx, "g_note", "g_note", "t_u32", e.n(0));
        global(e, cx, "g_audio_stream", "g_audio_stream", "t_pv", e.v("nullp"));
        global(e, cx, "g_midi_stream", "g_midi_stream", "t_pv", e.v("nullp"));

        cstr(e, cx, "s_media_type", "media.type");
        cstr(e, cx, "s_audio", "Audio");
        cstr(e, cx, "s_media_category", "media.category");
        cstr(e, cx, "s_playback", "Playback");
        cstr(e, cx, "s_media_role", "media.role");
        cstr(e, cx, "s_music", "Music");
        cstr(e, cx, "s_midi", "Midi");
        cstr(e, cx, "s_capture", "Capture");
        cstr(e, cx, "s_dsp", "DSP");
        cstr(e, cx, "s_aname", "atsynth-out");
        cstr(e, cx, "s_mname", "atsynth-midi-in");

        func(
                e,
                cx,
                "on_process",
                new String[] {"data"},
                new String[] {"t_pv"},
                "t_void",
                e.seq(
                        e.let("buf", callFn(e, "dequeue_audio", lo(e, "g_audio_stream"))),
                        e.ifOnly(
                                e.mb.operatorNe(e.v("buf"), lo(e, "nullp")),
                                e.seq(
                                        e.let("datas", e.fld(e.v("buf"), "buffer", "datas")),
                                        e.let("out", e.fld(e.v("datas"), "data")),
                                        e.ifOnly(
                                                e.mb.operatorNe(e.v("out"), lo(e, "nullp")),
                                                e.seq(
                                                        e.let(
                                                                "maxframes",
                                                                e.mb.operatorDiv(
                                                                        e.fld(e.v("datas"), "maxsize"), e.n(4))),
                                                        e.let("req", e.fld(e.v("buf"), "requested")),
                                                        e.let(
                                                                "n0",
                                                                e.ifElse(
                                                                        e.eq(e.v("req"), e.n(0)),
                                                                        e.v("maxframes"),
                                                                        conv(e, "t_u32", e.v("req")))),
                                                        e.let(
                                                                "n",
                                                                e.ifElse(
                                                                        e.mb.operatorGt(e.v("n0"), e.v("maxframes")),
                                                                        e.v("maxframes"),
                                                                        e.v("n0"))),
                                                        e.foreach(
                                                                callFn(e, "range_u32", e.v("n")),
                                                                "i",
                                                                e.seq(
                                                                        e.let(
                                                                                "sv",
                                                                                e.mb.operatorMul(
                                                                                        callFn(
                                                                                                e,
                                                                                                "sin",
                                                                                                lo(e, "g_phase")),
                                                                                        lo(e, "g_gate"))),
                                                                        set(
                                                                                e,
                                                                                e.mb.field(e.v("out"), e.v("i")),
                                                                                conv(e, "t_f32", e.v("sv"))),
                                                                        set(
                                                                                e,
                                                                                lo(e, "g_phase"),
                                                                                e.mb.operatorAdd(
                                                                                        lo(e, "g_phase"),
                                                                                        lo(e, "g_inc"))),
                                                                        e.ifOnly(
                                                                                e.mb.operatorGt(
                                                                                        lo(e, "g_phase"), e.n(TWO_PI)),
                                                                                set(
                                                                                        e,
                                                                                        lo(e, "g_phase"),
                                                                                        e.mb.operatorSub(
                                                                                                lo(e, "g_phase"),
                                                                                                e.n(TWO_PI)))))),
                                                        e.let("chunk", e.fld(e.v("datas"), "chunk")),
                                                        set(e, e.fld(e.v("chunk"), "offset"), e.n(0)),
                                                        set(
                                                                e,
                                                                e.fld(e.v("chunk"), "size"),
                                                                e.mb.operatorMul(e.v("n"), e.n(4))),
                                                        set(e, e.fld(e.v("chunk"), "stride"), e.n(4)))),
                                        callFn(e, "queue_audio", lo(e, "g_audio_stream"), e.v("buf"))))));

        var midiEvent = e.ifElse(
                e.eq(e.v("ctype"), lo(e, "control_midi")),
                e.seq(
                        e.let("d", e.fld(e.v("c"), "body")),
                        set(e, e.v("st"), e.mb.operatorAnd(conv(e, "t_u32", e.mb.field(e.v("d"), e.n(0))), e.n(240))),
                        set(e, e.v("note"), conv(e, "t_u32", e.mb.field(e.v("d"), e.n(1)))),
                        set(e, e.v("vel"), conv(e, "t_u32", e.mb.field(e.v("d"), e.n(2)))),
                        set(e, e.v("have"), e.n(1))),
                e.ifOnly(
                        e.eq(e.v("ctype"), lo(e, "control_ump")),
                        e.seq(
                                e.let("w", e.mb.field(e.fld(e.v("c"), "words"), e.n(0))),
                                e.ifOnly(
                                        e.eq(e.mb.operatorShr(e.v("w"), e.n(28)), e.n(2)),
                                        e.seq(
                                                set(
                                                        e,
                                                        e.v("st"),
                                                        e.mb.operatorAnd(
                                                                e.mb.operatorShr(e.v("w"), e.n(16)), e.n(240))),
                                                set(
                                                        e,
                                                        e.v("note"),
                                                        e.mb.operatorAnd(e.mb.operatorShr(e.v("w"), e.n(8)), e.n(127))),
                                                set(e, e.v("vel"), e.mb.operatorAnd(e.v("w"), e.n(127))),
                                                set(e, e.v("have"), e.n(1)))))));
        var noteEvent = e.ifOnly(
                e.mb.operatorNe(e.v("have"), e.n(0)),
                e.ifElse(
                        e.mb.operatorAnd(e.eq(e.v("st"), e.n(144)), e.mb.operatorNe(e.v("vel"), e.n(0))),
                        e.seq(
                                set(
                                        e,
                                        lo(e, "g_inc"),
                                        e.mb.operatorMul(
                                                callFn(
                                                        e,
                                                        "exp2",
                                                        e.mb.operatorDiv(
                                                                e.mb.operatorSub(
                                                                        conv(e, "t_f64", e.v("note")), e.n(69)),
                                                                e.n(12))),
                                                e.n(TWO_PI * 440.0 / RATE))),
                                set(e, lo(e, "g_note"), e.v("note")),
                                set(e, lo(e, "g_gate"), e.n(AMP))),
                        e.ifOnly(
                                e.mb.operatorOr(
                                        e.eq(e.v("st"), e.n(128)),
                                        e.mb.operatorAnd(e.eq(e.v("st"), e.n(144)), e.eq(e.v("vel"), e.n(0)))),
                                e.ifOnly(e.eq(e.v("note"), lo(e, "g_note")), set(e, lo(e, "g_gate"), e.n(0))))));
        func(
                e,
                cx,
                "on_midi",
                new String[] {"data"},
                new String[] {"t_pv"},
                "t_void",
                e.seq(
                        e.let("buf", callFn(e, "dequeue_midi", lo(e, "g_midi_stream"))),
                        e.ifOnly(
                                e.mb.operatorNe(e.v("buf"), lo(e, "nullp")),
                                e.seq(
                                        e.let("seqp", e.fld(e.v("buf"), "buffer", "datas", "data")),
                                        e.ifOnly(
                                                e.mb.operatorNe(e.v("seqp"), lo(e, "nullp")),
                                                e.ifOnly(
                                                        e.eq(
                                                                e.fld(e.v("seqp"), "pod", "type"),
                                                                lo(e, "pod_type_sequence")),
                                                        e.seq(
                                                                e.let("end", e.fld(e.v("seqp"), "next")),
                                                                e.let("c", e.fld(e.v("seqp"), "controls")),
                                                                e.mb.labeled(
                                                                        "scan",
                                                                        e.foreach(
                                                                                lo(e, "loop"),
                                                                                "_",
                                                                                e.seq(
                                                                                        e.ifOnly(
                                                                                                e.mb.operatorGt(
                                                                                                        e.mb
                                                                                                                .operatorAdd(
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "c"),
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        1)),
                                                                                                        e.v("end")),
                                                                                                e.mb.returnLabel(
                                                                                                        "scan")),
                                                                                        e.let(
                                                                                                "st",
                                                                                                conv(
                                                                                                        e, "t_u32",
                                                                                                        e.n(0))),
                                                                                        e.let(
                                                                                                "note",
                                                                                                conv(
                                                                                                        e, "t_u32",
                                                                                                        e.n(0))),
                                                                                        e.let(
                                                                                                "vel",
                                                                                                conv(
                                                                                                        e, "t_u32",
                                                                                                        e.n(0))),
                                                                                        e.let(
                                                                                                "have",
                                                                                                conv(
                                                                                                        e, "t_u32",
                                                                                                        e.n(0))),
                                                                                        e.let(
                                                                                                "ctype",
                                                                                                e.fld(
                                                                                                        e.v("c"),
                                                                                                        "type")),
                                                                                        midiEvent,
                                                                                        noteEvent,
                                                                                        set(
                                                                                                e,
                                                                                                e.v("c"),
                                                                                                e.fld(
                                                                                                        e.v("c"),
                                                                                                        "next")))))))),
                                        callFn(e, "queue_midi", lo(e, "g_midi_stream"), e.v("buf"))))));

        events(e, cx, "g_audio_events", "pw.events.audio", "on_process");
        events(e, cx, "g_midi_events", "pw.events.midi", "on_midi");

        func(
                e,
                cx,
                "main",
                new String[] {},
                new String[] {},
                "t_i32",
                e.seq(
                        callFn(e, "pw_init", lo(e, "nullp"), lo(e, "nullp")),
                        e.let("mloop", callFn(e, "pw_main_loop_new", lo(e, "nullp"))),
                        e.let("l", callFn(e, "pw_main_loop_get_loop", e.v("mloop"))),
                        e.let(
                                "aprops",
                                callFn(
                                        e,
                                        "pw_properties_new",
                                        lo(e, "s_media_type"),
                                        lo(e, "s_audio"),
                                        lo(e, "s_media_category"),
                                        lo(e, "s_playback"),
                                        lo(e, "s_media_role"),
                                        lo(e, "s_music"),
                                        lo(e, "nullp"))),
                        e.let(
                                "astream",
                                callFn(
                                        e,
                                        "pw_stream_new_simple",
                                        e.v("l"),
                                        lo(e, "s_aname"),
                                        e.v("aprops"),
                                        lo(e, "g_audio_events"),
                                        lo(e, "nullp"))),
                        set(e, lo(e, "g_audio_stream"), e.v("astream")),
                        e.let(
                                "mprops",
                                callFn(
                                        e,
                                        "pw_properties_new",
                                        lo(e, "s_media_type"),
                                        lo(e, "s_midi"),
                                        lo(e, "s_media_category"),
                                        lo(e, "s_capture"),
                                        lo(e, "s_media_role"),
                                        lo(e, "s_dsp"),
                                        lo(e, "nullp"))),
                        e.let(
                                "mstream",
                                callFn(
                                        e,
                                        "pw_stream_new_simple",
                                        e.v("l"),
                                        lo(e, "s_mname"),
                                        e.v("mprops"),
                                        lo(e, "g_midi_events"),
                                        lo(e, "nullp"))),
                        set(e, lo(e, "g_midi_stream"), e.v("mstream")),
                        callFn(
                                e,
                                "pw_stream_connect",
                                e.v("astream"),
                                lo(e, "direction_output"),
                                lo(e, "id_any"),
                                lo(e, "flags_autoconnect_map_rt"),
                                lo(e, "params_audio"),
                                e.n(1)),
                        callFn(
                                e,
                                "pw_stream_connect",
                                e.v("mstream"),
                                lo(e, "direction_input"),
                                lo(e, "id_any"),
                                lo(e, "flags_autoconnect_map_rt"),
                                lo(e, "params_midi"),
                                e.n(1)),
                        callFn(e, "pw_main_loop_run", e.v("mloop")),
                        callFn(e, "pw_main_loop_destroy", e.v("mloop")),
                        callFn(e, "pw_deinit", e.tup()),
                        e.ret(e.n(0))));

        top.add(mode.ecoRootModeFinish(
                e, Refs.top(e).refsRef("llvm"), Refs.top(e).refsRef("ll")));
        setup.add(e.ret(e.rec(cx.fields().toArray(EcoMb.P[]::new))));
        var module = new TList<SourceExpr>();
        module.add(e.let("k", e.lower(e.mb.seq(setup))));
        module.addAll(top);
        return module;
    }
}
