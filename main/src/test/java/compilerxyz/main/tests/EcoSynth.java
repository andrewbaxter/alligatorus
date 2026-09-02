package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.nio.charset.StandardCharsets;

public final class EcoSynth {
    private EcoSynth() {}

    private static String dbits(double v) {
        return Double.toString(v);
    }

    private static final String TWO_PI = dbits(2.0 * Math.PI);
    private static final String AMP = dbits(0.2);
    private static final String D440 = dbits(440.0);
    private static final String D69 = dbits(69.0);
    private static final String D12 = dbits(12.0);
    private static final String RATE = dbits(48000.0);
    private static final String DZERO = dbits(0.0);

    private static final class B {
        final EcoMb e;
        final String fb;
        final TList<SourceExpr> out;

        B(EcoMb e, String fb, TList<SourceExpr> out) {
            this.e = e;
            this.fb = fb;
            this.out = out;
        }

        SourceExpr m(String method, SourceExpr arg) {
            return e.callM(e.v(fb), method, arg);
        }

        void let(String name, SourceExpr value) {
            out.add(e.let(name, value));
        }

        void stmt(SourceExpr x) {
            out.add(x);
        }

        SourceExpr argOf(String value) {
            return e.callM(e.v("ll"), "arg_of", e.v(value));
        }

        void konst(String name, String ty, String ref) {
            let(name, m("konst", e.rec(e.p("ty", e.s(ty)), e.p("ref", e.s(ref)))));
        }

        void bin(String name, String op, String a, String b) {
            let(name, m("bin", e.rec(e.p("op", e.s(op)), e.p("a", e.v(a)), e.p("b", e.v(b)))));
        }

        void cmp(String name, String op, String a, String b) {
            let(name, m("cmp", e.rec(e.p("op", e.s(op)), e.p("a", e.v(a)), e.p("b", e.v(b)))));
        }

        void conv(String name, String op, String a, String ty) {
            let(name, m("conv", e.rec(e.p("op", e.s(op)), e.p("a", e.v(a)), e.p("ty", e.s(ty)))));
        }

        void load(String name, String ty, String p) {
            let(name, m("load", e.rec(e.p("ty", e.s(ty)), e.p("p", e.v(p)))));
        }

        void store(String v, String p) {
            stmt(m("store", e.rec(e.p("v", e.v(v)), e.p("p", e.v(p)))));
        }

        void gep8(String name, String p, SourceExpr off) {
            let(
                    name,
                    m(
                            "gep",
                            e.rec(
                                    e.p("base", e.s("i8")),
                                    e.p("p", e.v(p)),
                                    e.p("idx", e.strList(e.cat(e.s("i32 "), off))))));
        }

        void gep8(String name, String p, String constOff) {
            gep8(name, p, e.s(constOff));
        }

        void gepIdx(String name, String elemTy, String p, String idxValue) {
            let(
                    name,
                    m(
                            "gep",
                            e.rec(e.p("base", e.s(elemTy)), e.p("p", e.v(p)), e.p("idx", e.strList(argOf(idxValue))))));
        }

        void alloca(String name, String ty) {
            let(name, m("alloca", e.s(ty)));
        }

        void call(String name, SourceExpr f, SourceExpr... args) {
            let(name, m("call", e.rec(e.p("f", f), e.p("args", e.strList(args)))));
        }

        void callVoid(SourceExpr f, SourceExpr... args) {
            stmt(m("call_void", e.rec(e.p("f", f), e.p("args", e.strList(args)))));
        }

        void select(String name, String c, String a, String b) {
            let(name, m("select", e.rec(e.p("c", e.v(c)), e.p("a", e.v(a)), e.p("b", e.v(b)))));
        }

        void blocks(String... names) {
            for (var n : names) {
                let(n, m("block", e.nil()));
            }
        }

        void place(String label) {
            stmt(m("place", e.v(label)));
        }

        void br(String label) {
            stmt(m("br", e.v(label)));
        }

        void condBr(String c, String t, String f) {
            stmt(m("cond_br", e.rec(e.p("c", e.v(c)), e.p("t", e.v(t)), e.p("f", e.v(f)))));
        }

        void retVoid() {
            stmt(m("ret_void", e.nil()));
        }

        void ret(String v) {
            stmt(m("ret", e.v(v)));
        }
    }

    private static SourceExpr pwK(EcoMb e, String name) {
        return e.callM(e.v("pw"), name, e.nil());
    }

    private static SourceExpr libcFn(EcoMb e, String name) {
        return e.callM(e.v("c"), name, e.nil());
    }

    private static SourceExpr pwFn(EcoMb e, String name) {
        return e.callM(e.v("pw"), name, e.nil());
    }

    private static SourceExpr strGlobal(EcoMb e, String bind, String globalName, String content) {
        var escaped = new StringBuilder();
        for (var ch : content.getBytes(StandardCharsets.UTF_8)) {
            escaped.append(String.format("\\%02X", ch));
        }
        escaped.append("\\00");
        return e.let(
                bind,
                e.callM(
                        e.v("ll"),
                        "global_const",
                        e.rec(
                                e.p("name", e.s(globalName)),
                                e.p(
                                        "type",
                                        e.s("[" + (content.getBytes(StandardCharsets.UTF_8).length + 1) + " x i8]")),
                                e.p("init", e.s("c\"" + escaped + "\"")))));
    }

    private static SourceExpr dglobal(EcoMb e, String bind, String globalName, String ty, String init) {
        return e.let(
                bind,
                e.callM(
                        e.v("ll"),
                        "global",
                        e.rec(e.p("name", e.s(globalName)), e.p("type", e.s(ty)), e.p("init", e.s(init)))));
    }

    public static SourceExpr module(EcoMb e) {
        var top = new TList<SourceExpr>();
        top.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        top.add(e.let("libc", e.importLocal("libc", "libc-2.42.at")));
        top.add(e.let("pipewire", e.importLocal("pipewire", "pipewire-1.6.at")));
        top.add(e.let("ll", e.call(e.v("llvm"), e.nil())));
        top.add(e.let("c", e.call(e.v("libc"), e.v("ll"))));
        top.add(e.let("pw", e.call(e.v("pipewire"), e.v("ll"))));

        top.add(dglobal(e, "g_phase", "g_phase", "double", "0.000000e+00"));
        top.add(dglobal(e, "g_inc", "g_inc", "double", "0.000000e+00"));
        top.add(dglobal(e, "g_gate", "g_gate", "double", "0.000000e+00"));
        top.add(dglobal(e, "g_note", "g_note", "i32", "0"));
        top.add(dglobal(e, "g_audio_stream", "g_audio_stream", "ptr", "null"));
        top.add(dglobal(e, "g_midi_stream", "g_midi_stream", "ptr", "null"));

        top.add(strGlobal(e, "s_media_type", "s.media_type", "media.type"));
        top.add(strGlobal(e, "s_audio", "s.audio", "Audio"));
        top.add(strGlobal(e, "s_media_category", "s.media_category", "media.category"));
        top.add(strGlobal(e, "s_playback", "s.playback", "Playback"));
        top.add(strGlobal(e, "s_media_role", "s.media_role", "media.role"));
        top.add(strGlobal(e, "s_music", "s.music", "Music"));
        top.add(strGlobal(e, "s_midi", "s.midi", "Midi"));
        top.add(strGlobal(e, "s_capture", "s.capture", "Capture"));
        top.add(strGlobal(e, "s_dsp", "s.dsp", "DSP"));
        top.add(strGlobal(e, "s_aname", "s.aname", "atsynth-out"));
        top.add(strGlobal(e, "s_mname", "s.mname", "atsynth-midi-in"));

        top.add(e.let("pod_audio", e.callM(e.v("pw"), "pod_audio_f32_mono_48k", e.v("ll"))));
        top.add(e.let("pod_midi", e.callM(e.v("pw"), "pod_midi_control", e.v("ll"))));
        top.add(e.let(
                "g_audio_params",
                e.callM(
                        e.v("pw"),
                        "params_of",
                        e.rec(
                                e.p("ll", e.v("ll")),
                                e.p("pod", e.v("pod_audio")),
                                e.p("name", e.s("pw.params.audio"))))));
        top.add(e.let(
                "g_midi_params",
                e.callM(
                        e.v("pw"),
                        "params_of",
                        e.rec(e.p("ll", e.v("ll")), e.p("pod", e.v("pod_midi")), e.p("name", e.s("pw.params.midi"))))));

        top.add(e.let(
                "g_audio_events",
                e.callM(
                        e.v("ll"),
                        "global_const",
                        e.rec(
                                e.p("name", e.s("pw.events.audio")),
                                e.p("type", e.callM(e.v("pw"), "events_ty", e.nil())),
                                e.p("init", e.callM(e.v("pw"), "events_init", e.s("@on_process")))))));
        top.add(e.let(
                "g_midi_events",
                e.callM(
                        e.v("ll"),
                        "global_const",
                        e.rec(
                                e.p("name", e.s("pw.events.midi")),
                                e.p("type", e.callM(e.v("pw"), "events_ty", e.nil())),
                                e.p("init", e.callM(e.v("pw"), "events_init", e.s("@on_midi")))))));

        {
            top.add(e.let(
                    "fbp",
                    e.callM(
                            e.v("ll"),
                            "func",
                            e.rec(
                                    e.p("name", e.s("on_process")),
                                    e.p("ret", e.s("void")),
                                    e.p("params", e.strList(e.s("ptr")))))));
            var b = new B(e, "fbp", top);
            b.blocks("pb_main", "pb_fill", "pb_head", "pb_body", "pb_end", "pb_queue", "pb_out");
            b.konst("p_null", "ptr", "null");
            b.load("p_stream", "ptr", "g_audio_stream");
            b.call("p_buf", pwFn(e, "pw_stream_dequeue_buffer"), b.argOf("p_stream"));
            b.cmp("p_bnull", "icmp eq", "p_buf", "p_null");
            b.condBr("p_bnull", "pb_out", "pb_main");
            b.place("pb_main");
            b.load("p_spabuf", "ptr", "p_buf");
            b.gep8("p_datas_p", "p_spabuf", pwK(e, "off_spabuf_datas"));
            b.load("p_datas", "ptr", "p_datas_p");
            b.gep8("p_dst_p", "p_datas", pwK(e, "off_spadata_data"));
            b.load("p_dst", "ptr", "p_dst_p");
            b.cmp("p_dnull", "icmp eq", "p_dst", "p_null");
            b.condBr("p_dnull", "pb_queue", "pb_fill");
            b.place("pb_fill");
            b.gep8("p_maxsize_p", "p_datas", pwK(e, "off_spadata_maxsize"));
            b.load("p_maxsize", "i32", "p_maxsize_p");
            b.konst("p_c4", "i32", "4");
            b.bin("p_maxframes", "udiv", "p_maxsize", "p_c4");
            b.gep8("p_req_p", "p_buf", pwK(e, "off_pwbuf_requested"));
            b.load("p_req", "i64", "p_req_p");
            b.conv("p_req32", "trunc", "p_req", "i32");
            b.konst("p_z64", "i64", "0");
            b.cmp("p_reqz", "icmp eq", "p_req", "p_z64");
            b.select("p_n0", "p_reqz", "p_maxframes", "p_req32");
            b.cmp("p_ngt", "icmp ugt", "p_n0", "p_maxframes");
            b.select("p_n", "p_ngt", "p_maxframes", "p_n0");
            b.alloca("p_islot", "i32");
            b.konst("p_zero", "i32", "0");
            b.store("p_zero", "p_islot");
            b.br("pb_head");
            b.place("pb_head");
            b.load("p_i", "i32", "p_islot");
            b.cmp("p_cont", "icmp ult", "p_i", "p_n");
            b.condBr("p_cont", "pb_body", "pb_end");
            b.place("pb_body");
            b.load("p_ph", "double", "g_phase");
            b.call("p_sin", libcFn(e, "sin"), b.argOf("p_ph"));
            b.load("p_gate", "double", "g_gate");
            b.bin("p_sv", "fmul", "p_sin", "p_gate");
            b.conv("p_svf", "fptrunc", "p_sv", "float");
            b.gepIdx("p_addr", "float", "p_dst", "p_i");
            b.store("p_svf", "p_addr");
            b.load("p_inc", "double", "g_inc");
            b.bin("p_ph2", "fadd", "p_ph", "p_inc");
            b.konst("p_twopi", "double", TWO_PI);
            b.cmp("p_wrap", "fcmp ogt", "p_ph2", "p_twopi");
            b.bin("p_ph2w", "fsub", "p_ph2", "p_twopi");
            b.select("p_ph3", "p_wrap", "p_ph2w", "p_ph2");
            b.store("p_ph3", "g_phase");
            b.konst("p_one", "i32", "1");
            b.bin("p_i2", "add", "p_i", "p_one");
            b.store("p_i2", "p_islot");
            b.br("pb_head");
            b.place("pb_end");
            b.gep8("p_chunk_p", "p_datas", pwK(e, "off_spadata_chunk"));
            b.load("p_chunk", "ptr", "p_chunk_p");
            b.store("p_zero", "p_chunk");
            b.gep8("p_chsize_p", "p_chunk", pwK(e, "off_chunk_size"));
            b.bin("p_bytes", "mul", "p_n", "p_c4");
            b.store("p_bytes", "p_chsize_p");
            b.gep8("p_chstride_p", "p_chunk", pwK(e, "off_chunk_stride"));
            b.store("p_c4", "p_chstride_p");
            b.br("pb_queue");
            b.place("pb_queue");
            b.callVoid(pwFn(e, "pw_stream_queue_buffer"), b.argOf("p_stream"), b.argOf("p_buf"));
            b.retVoid();
            b.place("pb_out");
            b.retVoid();
            top.add(e.let("fbp_done", b.m("finish", e.nil())));
        }
        {
            top.add(e.let(
                    "fbm",
                    e.callM(
                            e.v("ll"),
                            "func",
                            e.rec(
                                    e.p("name", e.s("on_midi")),
                                    e.p("ret", e.s("void")),
                                    e.p("params", e.strList(e.s("ptr")))))));
            var b = new B(e, "fbm", top);
            b.blocks(
                    "mb_main",
                    "mb_scan",
                    "mb_head",
                    "mb_body",
                    "mb_raw",
                    "mb_check_ump",
                    "mb_ump",
                    "mb_ump_go",
                    "mb_decide",
                    "mb_on_check",
                    "mb_do_on",
                    "mb_off_check",
                    "mb_do_off",
                    "mb_off_match",
                    "mb_next",
                    "mb_queue",
                    "mb_out");
            b.konst("m_null", "ptr", "null");
            b.load("m_stream", "ptr", "g_midi_stream");
            b.call("m_buf", pwFn(e, "pw_stream_dequeue_buffer"), b.argOf("m_stream"));
            b.cmp("m_bnull", "icmp eq", "m_buf", "m_null");
            b.condBr("m_bnull", "mb_out", "mb_main");
            b.place("mb_main");
            b.load("m_spabuf", "ptr", "m_buf");
            b.gep8("m_datas_p", "m_spabuf", pwK(e, "off_spabuf_datas"));
            b.load("m_datas", "ptr", "m_datas_p");
            b.gep8("m_pod_pp", "m_datas", pwK(e, "off_spadata_data"));
            b.load("m_pod", "ptr", "m_pod_pp");
            b.cmp("m_pnull", "icmp eq", "m_pod", "m_null");
            b.condBr("m_pnull", "mb_queue", "mb_scan");
            b.place("mb_scan");
            b.gep8("m_ptype_p", "m_pod", "4");
            b.load("m_ptype", "i32", "m_ptype_p");
            b.konst("m_seq_t", "i32", "16");
            b.cmp("m_isseq", "icmp eq", "m_ptype", "m_seq_t");
            b.konst("m_zero", "i32", "0");
            b.load("m_bodysz", "i32", "m_pod");
            b.konst("m_c8", "i32", "8");
            b.bin("m_endoff", "add", "m_bodysz", "m_c8");
            b.gep8("m_end", "m_pod", e.callM(e.v("m_endoff"), "r", e.nil()));
            b.alloca("m_cslot", "ptr");
            b.gep8("m_c0", "m_pod", pwK(e, "off_seq_first_control"));
            b.store("m_c0", "m_cslot");
            b.alloca("m_st_slot", "i32");
            b.alloca("m_note_slot", "i32");
            b.alloca("m_vel_slot", "i32");
            b.alloca("m_have_slot", "i32");
            b.condBr("m_isseq", "mb_head", "mb_queue");
            b.place("mb_head");
            b.load("m_c", "ptr", "m_cslot");
            b.gep8("m_cend", "m_c", "16");
            b.cmp("m_inb", "icmp ule", "m_cend", "m_end");
            b.condBr("m_inb", "mb_body", "mb_queue");
            b.place("mb_body");
            b.store("m_zero", "m_have_slot");
            b.gep8("m_ctype_p", "m_c", pwK(e, "off_control_type"));
            b.load("m_ctype", "i32", "m_ctype_p");
            b.gep8("m_vsize_p", "m_c", pwK(e, "off_control_value"));
            b.load("m_vsize", "i32", "m_vsize_p");
            b.konst("m_ct_midi", "i32", "2");
            b.cmp("m_ismidi", "icmp eq", "m_ctype", "m_ct_midi");
            b.condBr("m_ismidi", "mb_raw", "mb_check_ump");
            b.place("mb_raw");
            b.gep8("m_vdata", "m_c", "16");
            b.load("m_s8", "i8", "m_vdata");
            b.conv("m_s32", "zext", "m_s8", "i32");
            b.konst("m_f0", "i32", "240");
            b.bin("m_st_raw", "and", "m_s32", "m_f0");
            b.gep8("m_note_p", "m_c", "17");
            b.load("m_note8", "i8", "m_note_p");
            b.conv("m_note_raw", "zext", "m_note8", "i32");
            b.gep8("m_vel_p", "m_c", "18");
            b.load("m_vel8", "i8", "m_vel_p");
            b.conv("m_vel_raw", "zext", "m_vel8", "i32");
            b.store("m_st_raw", "m_st_slot");
            b.store("m_note_raw", "m_note_slot");
            b.store("m_vel_raw", "m_vel_slot");
            b.konst("m_one", "i32", "1");
            b.store("m_one", "m_have_slot");
            b.br("mb_decide");
            b.place("mb_check_ump");
            b.konst("m_ct_ump", "i32", "4");
            b.cmp("m_isump", "icmp eq", "m_ctype", "m_ct_ump");
            b.condBr("m_isump", "mb_ump", "mb_decide");
            b.place("mb_ump");
            b.gep8("m_w_p", "m_c", "16");
            b.load("m_w", "i32", "m_w_p");
            b.konst("m_c28", "i32", "28");
            b.bin("m_mt", "lshr", "m_w", "m_c28");
            b.konst("m_c2", "i32", "2");
            b.cmp("m_ismt2", "icmp eq", "m_mt", "m_c2");
            b.condBr("m_ismt2", "mb_ump_go", "mb_decide");
            b.place("mb_ump_go");
            b.konst("m_c16", "i32", "16");
            b.bin("m_wst", "lshr", "m_w", "m_c16");
            b.konst("m_f0b", "i32", "240");
            b.bin("m_st_ump", "and", "m_wst", "m_f0b");
            b.bin("m_wnote", "lshr", "m_w", "m_c8");
            b.konst("m_7f", "i32", "127");
            b.bin("m_note_ump", "and", "m_wnote", "m_7f");
            b.bin("m_vel_ump", "and", "m_w", "m_7f");
            b.store("m_st_ump", "m_st_slot");
            b.store("m_note_ump", "m_note_slot");
            b.store("m_vel_ump", "m_vel_slot");
            b.konst("m_oneb", "i32", "1");
            b.store("m_oneb", "m_have_slot");
            b.br("mb_decide");
            b.place("mb_decide");
            b.load("m_have", "i32", "m_have_slot");
            b.konst("m_zerob", "i32", "0");
            b.cmp("m_havez", "icmp eq", "m_have", "m_zerob");
            b.condBr("m_havez", "mb_next", "mb_on_check");
            b.place("mb_on_check");
            b.load("m_st", "i32", "m_st_slot");
            b.load("m_note", "i32", "m_note_slot");
            b.load("m_vel", "i32", "m_vel_slot");
            b.konst("m_0x90", "i32", "144");
            b.cmp("m_ison", "icmp eq", "m_st", "m_0x90");
            b.cmp("m_velnz", "icmp ne", "m_vel", "m_zerob");
            b.bin("m_realon", "and", "m_ison", "m_velnz");
            b.condBr("m_realon", "mb_do_on", "mb_off_check");
            b.place("mb_do_on");
            b.conv("m_noted", "uitofp", "m_note", "double");
            b.konst("m_d69", "double", D69);
            b.bin("m_semi", "fsub", "m_noted", "m_d69");
            b.konst("m_d12", "double", D12);
            b.bin("m_oct", "fdiv", "m_semi", "m_d12");
            b.call("m_mult", libcFn(e, "exp2"), b.argOf("m_oct"));
            b.konst("m_d440", "double", D440);
            b.bin("m_freq", "fmul", "m_d440", "m_mult");
            b.konst("m_twopi", "double", TWO_PI);
            b.bin("m_w0", "fmul", "m_twopi", "m_freq");
            b.konst("m_rate", "double", RATE);
            b.bin("m_incv", "fdiv", "m_w0", "m_rate");
            b.store("m_incv", "g_inc");
            b.store("m_note", "g_note");
            b.konst("m_amp", "double", AMP);
            b.store("m_amp", "g_gate");
            b.br("mb_next");
            b.place("mb_off_check");
            b.konst("m_0x80", "i32", "128");
            b.cmp("m_isoff", "icmp eq", "m_st", "m_0x80");
            b.cmp("m_velz", "icmp eq", "m_vel", "m_zerob");
            b.bin("m_onvz", "and", "m_ison", "m_velz");
            b.bin("m_anyoff", "or", "m_isoff", "m_onvz");
            b.condBr("m_anyoff", "mb_do_off", "mb_next");
            b.place("mb_do_off");
            b.load("m_cur", "i32", "g_note");
            b.cmp("m_same", "icmp eq", "m_note", "m_cur");
            b.condBr("m_same", "mb_off_match", "mb_next");
            b.place("mb_off_match");
            b.konst("m_dz", "double", DZERO);
            b.store("m_dz", "g_gate");
            b.br("mb_next");
            b.place("mb_next");
            b.konst("m_c7", "i32", "7");
            b.bin("m_vs7", "add", "m_vsize", "m_c7");
            b.konst("m_mask8", "i32", "4294967288");
            b.bin("m_pad", "and", "m_vs7", "m_mask8");
            b.konst("m_c16b", "i32", "16");
            b.bin("m_adv", "add", "m_pad", "m_c16b");
            b.gep8("m_c2p", "m_c", e.callM(e.v("m_adv"), "r", e.nil()));
            b.store("m_c2p", "m_cslot");
            b.br("mb_head");
            b.place("mb_queue");
            b.callVoid(pwFn(e, "pw_stream_queue_buffer"), b.argOf("m_stream"), b.argOf("m_buf"));
            b.retVoid();
            b.place("mb_out");
            b.retVoid();
            top.add(e.let("fbm_done", b.m("finish", e.nil())));
        }
        {
            top.add(e.let(
                    "fbmain",
                    e.callM(
                            e.v("ll"),
                            "func",
                            e.rec(e.p("name", e.s("main")), e.p("ret", e.s("i32")), e.p("params", e.strList())))));
            var b = new B(e, "fbmain", top);
            b.konst("x_null", "ptr", "null");
            b.callVoid(pwFn(e, "pw_init"), b.argOf("x_null"), b.argOf("x_null"));
            b.call("x_loop", pwFn(e, "pw_main_loop_new"), b.argOf("x_null"));
            b.call("x_l", pwFn(e, "pw_main_loop_get_loop"), b.argOf("x_loop"));

            b.call(
                    "x_aprops",
                    pwFn(e, "pw_properties_new"),
                    b.argOf("s_media_type"),
                    b.argOf("s_audio"),
                    b.argOf("s_media_category"),
                    b.argOf("s_playback"),
                    b.argOf("s_media_role"),
                    b.argOf("s_music"),
                    b.argOf("x_null"));
            b.call(
                    "x_astream",
                    pwFn(e, "pw_stream_new_simple"),
                    b.argOf("x_l"),
                    b.argOf("s_aname"),
                    b.argOf("x_aprops"),
                    b.argOf("g_audio_events"),
                    b.argOf("x_null"));
            b.store("x_astream", "g_audio_stream");

            b.call(
                    "x_mprops",
                    pwFn(e, "pw_properties_new"),
                    b.argOf("s_media_type"),
                    b.argOf("s_midi"),
                    b.argOf("s_media_category"),
                    b.argOf("s_capture"),
                    b.argOf("s_media_role"),
                    b.argOf("s_dsp"),
                    b.argOf("x_null"));
            b.call(
                    "x_mstream",
                    pwFn(e, "pw_stream_new_simple"),
                    b.argOf("x_l"),
                    b.argOf("s_mname"),
                    b.argOf("x_mprops"),
                    b.argOf("g_midi_events"),
                    b.argOf("x_null"));
            b.store("x_mstream", "g_midi_stream");

            b.call(
                    "x_ares",
                    pwFn(e, "pw_stream_connect"),
                    b.argOf("x_astream"),
                    e.cat(e.s("i32 "), pwK(e, "direction_output")),
                    e.cat(e.s("i32 "), pwK(e, "id_any")),
                    e.cat(e.s("i32 "), pwK(e, "flags_autoconnect_map_rt")),
                    b.argOf("g_audio_params"),
                    e.s("i32 1"));
            b.call(
                    "x_mres",
                    pwFn(e, "pw_stream_connect"),
                    b.argOf("x_mstream"),
                    e.cat(e.s("i32 "), pwK(e, "direction_input")),
                    e.cat(e.s("i32 "), pwK(e, "id_any")),
                    e.cat(e.s("i32 "), pwK(e, "flags_autoconnect_map_rt")),
                    b.argOf("g_midi_params"),
                    e.s("i32 1"));

            b.call("x_run", pwFn(e, "pw_main_loop_run"), b.argOf("x_loop"));
            b.callVoid(pwFn(e, "pw_main_loop_destroy"), b.argOf("x_loop"));
            b.callVoid(pwFn(e, "pw_deinit"));
            b.konst("x_zero", "i32", "0");
            b.ret("x_zero");
            top.add(e.let("fbmain_done", b.m("finish", e.nil())));
        }

        top.add(e.call(e.mb.log(), e.callM(e.v("ll"), "render", e.nil())));
        return e.mb.seq(top);
    }
}
