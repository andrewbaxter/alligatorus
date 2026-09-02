package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoPipewire {
    private EcoPipewire() {}

    private record Extern(String name, String ret, IList<String> params) {}

    private static final IList<Extern> EXTERNS = IList.of(
            new Extern("pw_init", "void", IList.of("ptr", "ptr")),
            new Extern("pw_deinit", "void", IList.of()),
            new Extern("pw_main_loop_new", "ptr", IList.of("ptr")),
            new Extern("pw_main_loop_get_loop", "ptr", IList.of("ptr")),
            new Extern("pw_main_loop_run", "i32", IList.of("ptr")),
            new Extern("pw_main_loop_quit", "i32", IList.of("ptr")),
            new Extern("pw_main_loop_destroy", "void", IList.of("ptr")),
            new Extern("pw_stream_new_simple", "ptr", IList.of("ptr", "ptr", "ptr", "ptr", "ptr")),
            new Extern("pw_stream_connect", "i32", IList.of("ptr", "i32", "i32", "i32", "ptr", "i32")),
            new Extern("pw_stream_dequeue_buffer", "ptr", IList.of("ptr")),
            new Extern("pw_stream_queue_buffer", "i32", IList.of("ptr", "ptr")));

    private static final IList<Extern> VARARG_EXTERNS =
            IList.of(new Extern("pw_properties_new", "ptr", IList.of("ptr")));

    private record Konst(String name, String value) {}

    private static final IList<Konst> KONSTS = IList.of(
            new Konst("direction_input", "0"),
            new Konst("direction_output", "1"),
            new Konst("id_any", "4294967295"),
            new Konst("flag_autoconnect", "1"),
            new Konst("flag_map_buffers", "4"),
            new Konst("flag_rt_process", "16"),
            new Konst("flags_autoconnect_map_rt", "21"),
            new Konst("flags_map_rt", "20"),
            new Konst("off_pwbuf_buffer", "0"),
            new Konst("off_pwbuf_requested", "24"),
            new Konst("off_spabuf_n_datas", "4"),
            new Konst("off_spabuf_datas", "16"),
            new Konst("sizeof_spa_data", "40"),
            new Konst("off_spadata_maxsize", "20"),
            new Konst("off_spadata_data", "24"),
            new Konst("off_spadata_chunk", "32"),
            new Konst("off_chunk_offset", "0"),
            new Konst("off_chunk_size", "4"),
            new Konst("off_chunk_stride", "8"),
            new Konst("sizeof_spa_pod", "8"),
            new Konst("off_seq_first_control", "16"),
            new Konst("off_control_type", "4"),
            new Konst("off_control_value", "8"),
            new Konst("control_midi", "2"),
            new Konst("control_ump", "4"));

    private static final String EVENTS_TY = "{ i32, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr }";

    private static final String POD_AUDIO_F32_MONO_48K =
            "c\"\\A0\\00\\00\\00\\0F\\00\\00\\00\\03\\00\\04\\00\\03\\00\\00\\00\\01\\00\\00\\00\\00\\00\\00\\00"
                    + "\\04\\00\\00\\00\\03\\00\\00\\00\\01\\00\\00\\00\\00\\00\\00\\00\\02\\00\\00\\00\\00\\00\\00\\00"
                    + "\\04\\00\\00\\00\\03\\00\\00\\00\\01\\00\\00\\00\\00\\00\\00\\00\\01\\00\\01\\00\\00\\00\\00\\00"
                    + "\\04\\00\\00\\00\\03\\00\\00\\00\\1B\\01\\00\\00\\00\\00\\00\\00\\03\\00\\01\\00\\00\\00\\00\\00"
                    + "\\04\\00\\00\\00\\04\\00\\00\\00\\80\\BB\\00\\00\\00\\00\\00\\00\\04\\00\\01\\00\\00\\00\\00\\00"
                    + "\\04\\00\\00\\00\\04\\00\\00\\00\\01\\00\\00\\00\\00\\00\\00\\00\\05\\00\\01\\00\\00\\00\\00\\00"
                    + "\\0C\\00\\00\\00\\0D\\00\\00\\00\\04\\00\\00\\00\\03\\00\\00\\00\\00\\00\\00\\00\\00\\00\\00\\00\"";

    private static final String POD_MIDI_CONTROL =
            "c\"\\38\\00\\00\\00\\0F\\00\\00\\00\\03\\00\\04\\00\\03\\00\\00\\00\\01\\00\\00\\00\\00\\00\\00\\00"
                    + "\\04\\00\\00\\00\\03\\00\\00\\00\\06\\00\\00\\00\\00\\00\\00\\00\\02\\00\\00\\00\\00\\00\\00\\00"
                    + "\\04\\00\\00\\00\\03\\00\\00\\00\\01\\00\\06\\00\\00\\00\\00\\00\"";

    public static SourceExpr module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        exprs.add(e.let("probe", e.call(e.v("llvm"), e.nil())));
        exprs.add(e.let("lapp_t", e.callM(e.v("probe"), "t_app", e.nil())));
        exprs.add(e.let("fnv_t", e.callM(e.v("probe"), "t_extern", e.nil())));
        exprs.add(e.let("lv_t", e.callM(e.v("probe"), "t_value", e.nil())));

        exprs.add(e.let("d", e.define()));
        exprs.add(e.let("pw_o", e.defObject(e.v("d"), "pipewire 1.6")));
        var innerFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            innerFields.add(e.p(ex.name(), e.v("fnv_t")));
        }
        for (var ex : VARARG_EXTERNS) {
            innerFields.add(e.p(ex.name(), e.v("fnv_t")));
        }
        exprs.add(e.callM(e.v("pw_o"), "set_inner", e.rec(innerFields.toArray(new EcoMb.P[0]))));
        for (var ex : EXTERNS) {
            exprs.add(e.methodImpl(
                    e.v("pw_o"),
                    ex.name(),
                    e.mb.typesNull(),
                    e.retSome(e.v("fnv_t")),
                    e.stage(e.fld(e.v("inner"), ex.name()))));
        }
        for (var ex : VARARG_EXTERNS) {
            exprs.add(e.methodImpl(
                    e.v("pw_o"),
                    ex.name(),
                    e.mb.typesNull(),
                    e.retSome(e.v("fnv_t")),
                    e.stage(e.fld(e.v("inner"), ex.name()))));
        }
        for (var k : KONSTS) {
            exprs.add(e.methodImpl(
                    e.v("pw_o"), k.name(), e.mb.typesNull(), e.retSome(e.mb.typesString()), e.stage(e.s(k.value()))));
        }
        exprs.add(e.methodImpl(
                e.v("pw_o"), "events_ty", e.mb.typesNull(), e.retSome(e.mb.typesString()), e.stage(e.s(EVENTS_TY))));
        exprs.add(e.methodImpl(
                e.v("pw_o"),
                "events_init",
                e.mb.typesString(),
                e.retSome(e.mb.typesString()),
                e.stage(e.cat(
                        e.s("{ i32 2, ptr null, ptr null, ptr null, ptr null, ptr null, ptr null, ptr null, ptr "),
                        e.v("arg"),
                        e.s(", ptr null, ptr null, ptr null }")))));
        exprs.add(e.methodImpl(
                e.v("pw_o"),
                "pod_audio_f32_mono_48k",
                e.v("lapp_t"),
                e.retSome(e.v("lv_t")),
                e.stage(e.callM(
                        e.v("arg"),
                        "global_const",
                        e.rec(
                                e.p("name", e.s("pw.pod.audio_f32_mono_48k")),
                                e.p("type", e.s("[168 x i8]")),
                                e.p("init", e.s(POD_AUDIO_F32_MONO_48K)))))));
        exprs.add(e.methodImpl(
                e.v("pw_o"),
                "pod_midi_control",
                e.v("lapp_t"),
                e.retSome(e.v("lv_t")),
                e.stage(e.callM(
                        e.v("arg"),
                        "global_const",
                        e.rec(
                                e.p("name", e.s("pw.pod.midi_control")),
                                e.p("type", e.s("[64 x i8]")),
                                e.p("init", e.s(POD_MIDI_CONTROL)))))));
        exprs.add(e.methodImpl(
                e.v("pw_o"),
                "params_of",
                e.rec(e.p("ll", e.v("lapp_t")), e.p("pod", e.v("lv_t")), e.p("name", e.mb.typesString())),
                e.retSome(e.v("lv_t")),
                e.stage(e.callM(
                        e.fld(e.v("arg"), "ll"),
                        "global_const",
                        e.rec(
                                e.p("name", e.fld(e.v("arg"), "name")),
                                e.p("type", e.s("[1 x ptr]")),
                                e.p(
                                        "init",
                                        e.cat(
                                                e.s("[ptr "),
                                                e.callM(e.fld(e.v("arg"), "pod"), "r", e.nil()),
                                                e.s("]"))))))));

        exprs.add(e.let("pw_builder", e.defFunc(e.v("d"), "pipewire")));
        var body = new TList<SourceExpr>();
        var ctorFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            var params = new TList<SourceExpr>();
            for (var p : ex.params()) {
                params.add(e.s(p));
            }
            body.add(e.let(
                    ex.name(),
                    e.callM(
                            e.v("arg"),
                            "extern_func",
                            e.rec(
                                    e.p("name", e.s(ex.name())),
                                    e.p("ret", e.s(ex.ret())),
                                    e.p("params", listOf(e, params))))));
            ctorFields.add(e.p(ex.name(), e.v(ex.name())));
        }
        for (var ex : VARARG_EXTERNS) {
            var params = new TList<SourceExpr>();
            for (var p : ex.params()) {
                params.add(e.s(p));
            }
            body.add(e.let(
                    ex.name(),
                    e.callM(
                            e.v("arg"),
                            "extern_func_varargs",
                            e.rec(
                                    e.p("name", e.s(ex.name())),
                                    e.p("ret", e.s(ex.ret())),
                                    e.p("params", listOf(e, params))))));
            ctorFields.add(e.p(ex.name(), e.v(ex.name())));
        }
        body.add(e.ret(e.call(e.lower(e.fld(e.v("pw_o"), "new")), e.rec(ctorFields.toArray(new EcoMb.P[0])))));
        exprs.add(e.funcImpl(
                e.v("pw_builder"), e.v("lapp_t"), e.retSome(e.fld(e.v("pw_o"), "type")), e.stage(e.mb.seq(body))));

        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.fld(e.v("pw_builder"), "func")));
        return e.lower(e.mb.seq(exprs));
    }

    private static SourceExpr listOf(EcoMb e, TList<SourceExpr> elements) {
        var arr = new SourceExpr[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            arr[i] = elements.get(i);
        }
        return e.strList(arr);
    }
}
