package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.nio.charset.StandardCharsets;

public final class EcoSynth {
    private EcoSynth() {}

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double AMP = 0.2;
    private static final double RATE = 48000.0;

    private static SourceExpr lo(EcoMb e, String name) {
        return e.lower(e.v(name));
    }

    private record Cx(TList<SourceExpr> setup, TList<SourceExpr> top, TList<String> exported) {}

    private static void defType(EcoMb e, Cx cx, String name, SourceExpr typeExpr) {
        cx.setup().add(e.let(name, typeExpr));
        cx.exported().add(name);
    }

    private static void defValue(EcoMb e, Cx cx, String name, SourceExpr value, SourceExpr typeExpr) {
        defType(e, cx, name + "_t", typeExpr);
        cx.top().add(e.let(name, e.mb.as(value, e.v(name + "_t"))));
    }

    private static SourceExpr callFn(EcoMb e, String fn, SourceExpr arg) {
        return e.call(lo(e, fn), arg);
    }

    private static SourceExpr callFn(EcoMb e, String fn, SourceExpr... args) {
        return e.call(lo(e, fn), e.tup(args));
    }

    private static SourceExpr conv(EcoMb e, String type, SourceExpr value) {
        return e.call(lo(e, type), value);
    }

    private static SourceExpr as(EcoMb e, SourceExpr value, String type) {
        return e.mb.as(value, lo(e, type));
    }

    private static SourceExpr set(EcoMb e, SourceExpr base, SourceExpr value) {
        return e.mb.set(base, value);
    }

    private static void cstr(EcoMb e, Cx cx, String bind, String globalName, String content) {
        var bytes = content.getBytes(StandardCharsets.UTF_8);
        var ints = new int[bytes.length + 1];
        for (int i = 0; i < bytes.length; i++) {
            ints[i] = bytes[i] & 0xFF;
        }
        ints[bytes.length] = 0;
        defValue(
                e,
                cx,
                bind,
                e.callM(
                        e.v("ll"),
                        "global_const",
                        e.rec(
                                e.p("name", e.s(globalName)),
                                e.p("ty", EcoPipewire.byteArrayType(e, "ll", ints.length)),
                                e.p("init", e.stage(EcoPipewire.byteTuple(e, ints))))),
                EcoPipewire.byteArrayConstType(e, "ll", ints.length));
    }

    private static void global(EcoMb e, Cx cx, String bind, String name, String type, SourceExpr init) {
        defValue(
                e,
                cx,
                bind,
                e.callM(
                        e.v("ll"),
                        "global",
                        e.rec(e.p("name", e.s(name)), e.p("ty", e.v(type)), e.p("init", e.stage(init)))),
                EcoExtern.globalT(e, "ll", e.v(type), true));
    }

    private static void func(
            EcoMb e, Cx cx, String name, String[] paramNames, String[] paramTypes, String ret, SourceExpr body) {
        var fields = new TList<SourceExpr>();
        var types = new TList<SourceExpr>();
        for (int i = 0; i < paramNames.length; i++) {
            fields.add(
                    e.callM(e.v("ll"), "field", e.rec(e.p("name", e.s(paramNames[i])), e.p("ty", e.v(paramTypes[i])))));
            types.add(e.v(paramTypes[i]));
        }
        var fnType = e.callM(
                e.v("ll"),
                "fn",
                e.rec(
                        e.p("params", e.listNew(e.mb.evalType(), types.toArray(SourceExpr[]::new))),
                        e.p("ret", e.v(ret))));
        defValue(
                e,
                cx,
                name,
                e.callM(
                        e.v("ll"),
                        "func",
                        e.rec(
                                e.p("name", e.s(name)),
                                e.p("params", e.listNew(e.mb.evalValue(), fields.toArray(SourceExpr[]::new))),
                                e.p("ret", e.v(ret)),
                                e.p("body", e.stage(body)))),
                EcoExtern.globalT(e, "ll", fnType, false));
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
            fields.add(e.p(f, lo(e, f.equals("process") ? process : "nullp")));
        }
        defValue(
                e,
                cx,
                bind,
                e.callM(
                        e.v("ll"),
                        "global_const",
                        e.rec(
                                e.p("name", e.s(name)),
                                e.p("ty", e.v("t_events")),
                                e.p("init", e.stage(e.rec(fields.toArray(EcoMb.P[]::new)))))),
                EcoExtern.globalT(e, "ll", e.callM(e.v("ll"), "ptr", e.v("t_events")), false));
    }

    private static SourceExpr pwT(EcoMb e, String name) {
        return e.callM(e.v("pw"), name, e.tup());
    }

    public static SourceExpr module(EcoMb e) {
        var cx = new Cx(new TList<>(), new TList<>(), new TList<>());
        var setup = cx.setup();
        var top = cx.top();
        setup.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        setup.add(e.let("libc", e.importLocal("libc", "libc-2.42.at")));
        setup.add(e.let("pipewire", e.importLocal("pipewire", "pipewire-1.6.at")));
        setup.add(e.let("ll", e.call(e.v("llvm"), e.tup())));
        setup.add(e.let("c", e.call(e.v("libc"), e.v("ll"))));
        setup.add(e.let("pw", e.call(e.v("pipewire"), e.v("ll"))));
        for (var n : new String[] {"ll", "c", "pw"}) cx.exported().add(n);
        EcoExtern.bindTypes(e, setup, "ll");
        for (var t : new String[] {"i1", "i8", "i32", "i64", "u8", "u32", "u64", "f32", "f64", "void", "pv", "pu8"}) {
            cx.exported().add("t_" + t);
        }
        defType(e, cx, "t_pf32", e.callM(e.v("ll"), "ptr", e.v("t_f32")));
        defType(e, cx, "t_pu32", e.callM(e.v("ll"), "ptr", e.v("t_u32")));
        defType(e, cx, "t_events", pwT(e, "pw_stream_events"));
        defType(e, cx, "t_pseq", e.callM(e.v("ll"), "ptr", pwT(e, "spa_pod_sequence")));
        defType(e, cx, "t_pctl", e.callM(e.v("ll"), "ptr", pwT(e, "spa_pod_control")));
        defValue(e, cx, "nullp", e.callM(e.v("ll"), "null", e.tup()), EcoExtern.globalT(e, "ll", e.v("t_pv"), false));
        defValue(
                e, cx, "range_u32", e.callM(e.v("ll"), "ref", e.s("range")), e.callM(e.v("ll"), "range", e.v("t_u32")));
        defValue(e, cx, "loop", e.callM(e.v("ll"), "ref", e.s("loop")), e.callM(e.v("ll"), "loop", e.tup()));

        for (var fn : new String[] {"sin", "exp2"}) {
            defValue(e, cx, fn, e.callM(e.v("c"), fn, e.tup()), e.callM(e.v("c"), fn + "_t", e.tup()));
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
            "pw_stream_dequeue_buffer",
            "pw_stream_queue_buffer",
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
            defValue(e, cx, fn, pwT(e, fn), pwT(e, fn + "_t"));
        }

        global(e, cx, "g_phase", "g_phase", "t_f64", e.n(0));
        global(e, cx, "g_inc", "g_inc", "t_f64", e.n(0));
        global(e, cx, "g_gate", "g_gate", "t_f64", e.n(0));
        global(e, cx, "g_note", "g_note", "t_u32", e.n(0));
        global(e, cx, "g_audio_stream", "g_audio_stream", "t_pv", lo(e, "nullp"));
        global(e, cx, "g_midi_stream", "g_midi_stream", "t_pv", lo(e, "nullp"));

        cstr(e, cx, "s_media_type", "s.media_type", "media.type");
        cstr(e, cx, "s_audio", "s.audio", "Audio");
        cstr(e, cx, "s_media_category", "s.media_category", "media.category");
        cstr(e, cx, "s_playback", "s.playback", "Playback");
        cstr(e, cx, "s_media_role", "s.media_role", "media.role");
        cstr(e, cx, "s_music", "s.music", "Music");
        cstr(e, cx, "s_midi", "s.midi", "Midi");
        cstr(e, cx, "s_capture", "s.capture", "Capture");
        cstr(e, cx, "s_dsp", "s.dsp", "DSP");
        cstr(e, cx, "s_aname", "s.aname", "atsynth-out");
        cstr(e, cx, "s_mname", "s.mname", "atsynth-midi-in");

        func(
                e,
                cx,
                "on_process",
                new String[] {"data"},
                new String[] {"t_pv"},
                "t_void",
                e.seq(
                        e.let("buf", callFn(e, "pw_stream_dequeue_buffer", lo(e, "g_audio_stream"))),
                        e.ifOnly(
                                e.mb.operatorNe(e.v("buf"), lo(e, "nullp")),
                                e.seq(
                                        e.let("datas", e.fld(e.fld(e.v("buf"), "buffer"), "datas")),
                                        e.let("dst", e.fld(e.v("datas"), "data")),
                                        e.ifOnly(
                                                e.mb.operatorNe(e.v("dst"), lo(e, "nullp")),
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
                                                        e.let("out", as(e, e.v("dst"), "t_pf32")),
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
                                        callFn(e, "pw_stream_queue_buffer", lo(e, "g_audio_stream"), e.v("buf"))))));

        func(
                e,
                cx,
                "on_midi",
                new String[] {"data"},
                new String[] {"t_pv"},
                "t_void",
                e.seq(
                        e.let("buf", callFn(e, "pw_stream_dequeue_buffer", lo(e, "g_midi_stream"))),
                        e.ifOnly(
                                e.mb.operatorNe(e.v("buf"), lo(e, "nullp")),
                                e.seq(
                                        e.let("pod", e.fld(e.fld(e.fld(e.v("buf"), "buffer"), "datas"), "data")),
                                        e.ifOnly(
                                                e.mb.operatorNe(e.v("pod"), lo(e, "nullp")),
                                                e.seq(
                                                        e.let("seqp", as(e, e.v("pod"), "t_pseq")),
                                                        e.ifOnly(
                                                                e.eq(
                                                                        e.fld(e.fld(e.v("seqp"), "pod"), "type"),
                                                                        lo(e, "pod_type_sequence")),
                                                                e.seq(
                                                                        e.let(
                                                                                "end",
                                                                                e.mb.operatorAdd(
                                                                                        as(e, e.v("pod"), "t_pu8"),
                                                                                        e.mb.operatorAdd(
                                                                                                e.fld(
                                                                                                        e.fld(
                                                                                                                e.v(
                                                                                                                        "seqp"),
                                                                                                                "pod"),
                                                                                                        "size"),
                                                                                                e.n(8)))),
                                                                        e.let(
                                                                                "c",
                                                                                as(
                                                                                        e,
                                                                                        e.mb.operatorAdd(
                                                                                                e.v("seqp"), e.n(1)),
                                                                                        "t_pctl")),
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
                                                                                                                                as(
                                                                                                                                        e,
                                                                                                                                        e
                                                                                                                                                .v(
                                                                                                                                                        "c"),
                                                                                                                                        "t_pu8"),
                                                                                                                                e
                                                                                                                                        .n(
                                                                                                                                                16)),
                                                                                                                e.v(
                                                                                                                        "end")),
                                                                                                        e.mb
                                                                                                                .returnLabel(
                                                                                                                        "scan")),
                                                                                                e.let(
                                                                                                        "st",
                                                                                                        conv(
                                                                                                                e,
                                                                                                                "t_u32",
                                                                                                                e.n(
                                                                                                                        0))),
                                                                                                e.let(
                                                                                                        "note",
                                                                                                        conv(
                                                                                                                e,
                                                                                                                "t_u32",
                                                                                                                e.n(
                                                                                                                        0))),
                                                                                                e.let(
                                                                                                        "vel",
                                                                                                        conv(
                                                                                                                e,
                                                                                                                "t_u32",
                                                                                                                e.n(
                                                                                                                        0))),
                                                                                                e.let(
                                                                                                        "have",
                                                                                                        conv(
                                                                                                                e,
                                                                                                                "t_u32",
                                                                                                                e.n(
                                                                                                                        0))),
                                                                                                e.let(
                                                                                                        "ctype",
                                                                                                        e.fld(
                                                                                                                e.v(
                                                                                                                        "c"),
                                                                                                                "type")),
                                                                                                e.let(
                                                                                                        "vsize",
                                                                                                        e.fld(
                                                                                                                e.fld(
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "c"),
                                                                                                                        "value"),
                                                                                                                "size")),
                                                                                                e.ifElse(
                                                                                                        e.eq(
                                                                                                                e.v(
                                                                                                                        "ctype"),
                                                                                                                lo(
                                                                                                                        e,
                                                                                                                        "control_midi")),
                                                                                                        e.seq(
                                                                                                                e.let(
                                                                                                                        "d",
                                                                                                                        e
                                                                                                                                .mb
                                                                                                                                .operatorAdd(
                                                                                                                                        as(
                                                                                                                                                e,
                                                                                                                                                e
                                                                                                                                                        .v(
                                                                                                                                                                "c"),
                                                                                                                                                "t_pu8"),
                                                                                                                                        e
                                                                                                                                                .n(
                                                                                                                                                        16))),
                                                                                                                set(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "st"),
                                                                                                                        e
                                                                                                                                .mb
                                                                                                                                .operatorAnd(
                                                                                                                                        conv(
                                                                                                                                                e,
                                                                                                                                                "t_u32",
                                                                                                                                                e
                                                                                                                                                        .mb
                                                                                                                                                        .field(
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "d"),
                                                                                                                                                                e
                                                                                                                                                                        .n(
                                                                                                                                                                                0))),
                                                                                                                                        e
                                                                                                                                                .n(
                                                                                                                                                        240))),
                                                                                                                set(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "note"),
                                                                                                                        conv(
                                                                                                                                e,
                                                                                                                                "t_u32",
                                                                                                                                e
                                                                                                                                        .mb
                                                                                                                                        .field(
                                                                                                                                                e
                                                                                                                                                        .v(
                                                                                                                                                                "d"),
                                                                                                                                                e
                                                                                                                                                        .n(
                                                                                                                                                                1)))),
                                                                                                                set(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "vel"),
                                                                                                                        conv(
                                                                                                                                e,
                                                                                                                                "t_u32",
                                                                                                                                e
                                                                                                                                        .mb
                                                                                                                                        .field(
                                                                                                                                                e
                                                                                                                                                        .v(
                                                                                                                                                                "d"),
                                                                                                                                                e
                                                                                                                                                        .n(
                                                                                                                                                                2)))),
                                                                                                                set(
                                                                                                                        e,
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "have"),
                                                                                                                        e
                                                                                                                                .n(
                                                                                                                                        1))),
                                                                                                        e.ifOnly(
                                                                                                                e.eq(
                                                                                                                        e
                                                                                                                                .v(
                                                                                                                                        "ctype"),
                                                                                                                        lo(
                                                                                                                                e,
                                                                                                                                "control_ump")),
                                                                                                                e.seq(
                                                                                                                        e
                                                                                                                                .let(
                                                                                                                                        "w",
                                                                                                                                        e
                                                                                                                                                .mb
                                                                                                                                                .field(
                                                                                                                                                        as(
                                                                                                                                                                e,
                                                                                                                                                                e
                                                                                                                                                                        .mb
                                                                                                                                                                        .operatorAdd(
                                                                                                                                                                                as(
                                                                                                                                                                                        e,
                                                                                                                                                                                        e
                                                                                                                                                                                                .v(
                                                                                                                                                                                                        "c"),
                                                                                                                                                                                        "t_pu8"),
                                                                                                                                                                                e
                                                                                                                                                                                        .n(
                                                                                                                                                                                                16)),
                                                                                                                                                                "t_pu32"),
                                                                                                                                                        e
                                                                                                                                                                .n(
                                                                                                                                                                        0))),
                                                                                                                        e
                                                                                                                                .ifOnly(
                                                                                                                                        e
                                                                                                                                                .eq(
                                                                                                                                                        e
                                                                                                                                                                .mb
                                                                                                                                                                .operatorShr(
                                                                                                                                                                        e
                                                                                                                                                                                .v(
                                                                                                                                                                                        "w"),
                                                                                                                                                                        e
                                                                                                                                                                                .n(
                                                                                                                                                                                        28)),
                                                                                                                                                        e
                                                                                                                                                                .n(
                                                                                                                                                                        2)),
                                                                                                                                        e
                                                                                                                                                .seq(
                                                                                                                                                        set(
                                                                                                                                                                e,
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "st"),
                                                                                                                                                                e
                                                                                                                                                                        .mb
                                                                                                                                                                        .operatorAnd(
                                                                                                                                                                                e
                                                                                                                                                                                        .mb
                                                                                                                                                                                        .operatorShr(
                                                                                                                                                                                                e
                                                                                                                                                                                                        .v(
                                                                                                                                                                                                                "w"),
                                                                                                                                                                                                e
                                                                                                                                                                                                        .n(
                                                                                                                                                                                                                16)),
                                                                                                                                                                                e
                                                                                                                                                                                        .n(
                                                                                                                                                                                                240))),
                                                                                                                                                        set(
                                                                                                                                                                e,
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "note"),
                                                                                                                                                                e
                                                                                                                                                                        .mb
                                                                                                                                                                        .operatorAnd(
                                                                                                                                                                                e
                                                                                                                                                                                        .mb
                                                                                                                                                                                        .operatorShr(
                                                                                                                                                                                                e
                                                                                                                                                                                                        .v(
                                                                                                                                                                                                                "w"),
                                                                                                                                                                                                e
                                                                                                                                                                                                        .n(
                                                                                                                                                                                                                8)),
                                                                                                                                                                                e
                                                                                                                                                                                        .n(
                                                                                                                                                                                                127))),
                                                                                                                                                        set(
                                                                                                                                                                e,
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "vel"),
                                                                                                                                                                e
                                                                                                                                                                        .mb
                                                                                                                                                                        .operatorAnd(
                                                                                                                                                                                e
                                                                                                                                                                                        .v(
                                                                                                                                                                                                "w"),
                                                                                                                                                                                e
                                                                                                                                                                                        .n(
                                                                                                                                                                                                127))),
                                                                                                                                                        set(
                                                                                                                                                                e,
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "have"),
                                                                                                                                                                e
                                                                                                                                                                        .n(
                                                                                                                                                                                1))))))),
                                                                                                e.ifOnly(
                                                                                                        e.mb.operatorNe(
                                                                                                                e.v(
                                                                                                                        "have"),
                                                                                                                e.n(0)),
                                                                                                        e.ifElse(
                                                                                                                e.mb
                                                                                                                        .operatorAnd(
                                                                                                                                e
                                                                                                                                        .eq(
                                                                                                                                                e
                                                                                                                                                        .v(
                                                                                                                                                                "st"),
                                                                                                                                                e
                                                                                                                                                        .n(
                                                                                                                                                                144)),
                                                                                                                                e
                                                                                                                                        .mb
                                                                                                                                        .operatorNe(
                                                                                                                                                e
                                                                                                                                                        .v(
                                                                                                                                                                "vel"),
                                                                                                                                                e
                                                                                                                                                        .n(
                                                                                                                                                                0))),
                                                                                                                e.seq(
                                                                                                                        set(
                                                                                                                                e,
                                                                                                                                lo(
                                                                                                                                        e,
                                                                                                                                        "g_inc"),
                                                                                                                                e
                                                                                                                                        .mb
                                                                                                                                        .operatorMul(
                                                                                                                                                callFn(
                                                                                                                                                        e,
                                                                                                                                                        "exp2",
                                                                                                                                                        e
                                                                                                                                                                .mb
                                                                                                                                                                .operatorDiv(
                                                                                                                                                                        e
                                                                                                                                                                                .mb
                                                                                                                                                                                .operatorSub(
                                                                                                                                                                                        conv(
                                                                                                                                                                                                e,
                                                                                                                                                                                                "t_f64",
                                                                                                                                                                                                e
                                                                                                                                                                                                        .v(
                                                                                                                                                                                                                "note")),
                                                                                                                                                                                        e
                                                                                                                                                                                                .n(
                                                                                                                                                                                                        69)),
                                                                                                                                                                        e
                                                                                                                                                                                .n(
                                                                                                                                                                                        12))),
                                                                                                                                                e
                                                                                                                                                        .n(
                                                                                                                                                                TWO_PI
                                                                                                                                                                        * 440.0
                                                                                                                                                                        / RATE))),
                                                                                                                        set(
                                                                                                                                e,
                                                                                                                                lo(
                                                                                                                                        e,
                                                                                                                                        "g_note"),
                                                                                                                                e
                                                                                                                                        .v(
                                                                                                                                                "note")),
                                                                                                                        set(
                                                                                                                                e,
                                                                                                                                lo(
                                                                                                                                        e,
                                                                                                                                        "g_gate"),
                                                                                                                                e
                                                                                                                                        .n(
                                                                                                                                                AMP))),
                                                                                                                e
                                                                                                                        .ifOnly(
                                                                                                                                e
                                                                                                                                        .mb
                                                                                                                                        .operatorOr(
                                                                                                                                                e
                                                                                                                                                        .eq(
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "st"),
                                                                                                                                                                e
                                                                                                                                                                        .n(
                                                                                                                                                                                128)),
                                                                                                                                                e
                                                                                                                                                        .mb
                                                                                                                                                        .operatorAnd(
                                                                                                                                                                e
                                                                                                                                                                        .eq(
                                                                                                                                                                                e
                                                                                                                                                                                        .v(
                                                                                                                                                                                                "st"),
                                                                                                                                                                                e
                                                                                                                                                                                        .n(
                                                                                                                                                                                                144)),
                                                                                                                                                                e
                                                                                                                                                                        .eq(
                                                                                                                                                                                e
                                                                                                                                                                                        .v(
                                                                                                                                                                                                "vel"),
                                                                                                                                                                                e
                                                                                                                                                                                        .n(
                                                                                                                                                                                                0)))),
                                                                                                                                e
                                                                                                                                        .ifOnly(
                                                                                                                                                e
                                                                                                                                                        .eq(
                                                                                                                                                                e
                                                                                                                                                                        .v(
                                                                                                                                                                                "note"),
                                                                                                                                                                lo(
                                                                                                                                                                        e,
                                                                                                                                                                        "g_note")),
                                                                                                                                                set(
                                                                                                                                                        e,
                                                                                                                                                        lo(
                                                                                                                                                                e,
                                                                                                                                                                "g_gate"),
                                                                                                                                                        e
                                                                                                                                                                .n(
                                                                                                                                                                        0)))))),
                                                                                                set(
                                                                                                        e,
                                                                                                        e.v("c"),
                                                                                                        as(
                                                                                                                e,
                                                                                                                e.mb
                                                                                                                        .operatorAdd(
                                                                                                                                as(
                                                                                                                                        e,
                                                                                                                                        e
                                                                                                                                                .v(
                                                                                                                                                        "c"),
                                                                                                                                        "t_pu8"),
                                                                                                                                e
                                                                                                                                        .mb
                                                                                                                                        .operatorAdd(
                                                                                                                                                e
                                                                                                                                                        .mb
                                                                                                                                                        .operatorAnd(
                                                                                                                                                                e
                                                                                                                                                                        .mb
                                                                                                                                                                        .operatorAdd(
                                                                                                                                                                                e
                                                                                                                                                                                        .v(
                                                                                                                                                                                                "vsize"),
                                                                                                                                                                                e
                                                                                                                                                                                        .n(
                                                                                                                                                                                                7)),
                                                                                                                                                                e
                                                                                                                                                                        .n(
                                                                                                                                                                                4294967288.0)),
                                                                                                                                                e
                                                                                                                                                        .n(
                                                                                                                                                                16))),
                                                                                                                "t_pctl"))))))))),
                                        callFn(e, "pw_stream_queue_buffer", lo(e, "g_midi_stream"), e.v("buf"))))));

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

        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.tup())));
        var exportedFields = new TList<EcoMb.P>();
        for (var n : cx.exported()) exportedFields.add(e.p(n, e.v(n)));
        setup.add(e.ret(e.rec(exportedFields.toArray(EcoMb.P[]::new))));
        var module = new TList<SourceExpr>();
        module.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : cx.exported()) module.add(e.let(n, e.fld(e.v("k"), n)));
        module.addAll(top);
        return e.mb.seq(module);
    }
}
