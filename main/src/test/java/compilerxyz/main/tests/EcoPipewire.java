package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoPipewire {
    private EcoPipewire() {}

    private static final IList<EcoExtern> EXTERNS = IList.of(
            new EcoExtern("pw_init", IList.of("pv", "pv"), "void", false),
            new EcoExtern("pw_deinit", IList.of(), "void", false),
            new EcoExtern("pw_main_loop_new", IList.of("pv"), "pv", false),
            new EcoExtern("pw_main_loop_get_loop", IList.of("pv"), "pv", false),
            new EcoExtern("pw_main_loop_run", IList.of("pv"), "i32", false),
            new EcoExtern("pw_main_loop_quit", IList.of("pv"), "i32", false),
            new EcoExtern("pw_main_loop_destroy", IList.of("pv"), "void", false),
            new EcoExtern("pw_stream_new_simple", IList.of("pv", "pu8", "pv", "pv", "pv"), "pv", false),
            new EcoExtern("pw_stream_connect", IList.of("pv", "u32", "u32", "u32", "pv", "u32"), "i32", false),
            new EcoExtern("pw_stream_dequeue_buffer", IList.of("pv"), "p_pw_buffer", false),
            new EcoExtern("pw_stream_queue_buffer", IList.of("pv", "p_pw_buffer"), "i32", false),
            new EcoExtern("pw_properties_new", IList.of("pu8"), "pv", true));

    private record Konst(String name, double value) {}

    private static final IList<Konst> KONSTS = IList.of(
            new Konst("direction_input", 0),
            new Konst("direction_output", 1),
            new Konst("id_any", 4294967295.0),
            new Konst("flags_autoconnect_map_rt", 21),
            new Konst("pod_type_sequence", 16),
            new Konst("control_midi", 2),
            new Konst("control_ump", 4));

    private record Field(String name, String ty) {}

    private record Struct(String name, IList<Field> fields) {}

    private static final IList<Struct> STRUCTS = IList.of(
            new Struct(
                    "spa_chunk",
                    IList.of(
                            new Field("offset", "u32"),
                            new Field("size", "u32"),
                            new Field("stride", "i32"),
                            new Field("flags", "i32"))),
            new Struct(
                    "spa_data",
                    IList.of(
                            new Field("type", "u32"),
                            new Field("flags", "u32"),
                            new Field("fd", "i64"),
                            new Field("mapoffset", "u32"),
                            new Field("maxsize", "u32"),
                            new Field("data", "pv"),
                            new Field("chunk", "p_spa_chunk"))),
            new Struct(
                    "spa_buffer",
                    IList.of(
                            new Field("n_metas", "u32"),
                            new Field("n_datas", "u32"),
                            new Field("metas", "pv"),
                            new Field("datas", "p_spa_data"))),
            new Struct(
                    "pw_buffer",
                    IList.of(
                            new Field("buffer", "p_spa_buffer"),
                            new Field("user_data", "pv"),
                            new Field("size", "u64"),
                            new Field("requested", "u64"),
                            new Field("time", "u64"))),
            new Struct("spa_pod", IList.of(new Field("size", "u32"), new Field("type", "u32"))),
            new Struct(
                    "spa_pod_control",
                    IList.of(new Field("offset", "u32"), new Field("type", "u32"), new Field("value", "spa_pod"))),
            new Struct(
                    "spa_pod_sequence",
                    IList.of(new Field("pod", "spa_pod"), new Field("unit", "u32"), new Field("pad", "u32"))),
            new Struct(
                    "pw_stream_events",
                    IList.of(
                            new Field("version", "u32"),
                            new Field("destroy", "pv"),
                            new Field("state_changed", "pv"),
                            new Field("control_info", "pv"),
                            new Field("io_changed", "pv"),
                            new Field("param_changed", "pv"),
                            new Field("add_buffer", "pv"),
                            new Field("remove_buffer", "pv"),
                            new Field("process", "pv"),
                            new Field("drained", "pv"),
                            new Field("command", "pv"),
                            new Field("trigger_done", "pv"))));

    private sealed interface PodValue permits PodId, PodInt, PodIdArray {}

    private record PodId(EcoSpaPod.SpaConst value) implements PodValue {}

    private record PodInt(int value) implements PodValue {}

    private record PodIdArray(IList<EcoSpaPod.SpaConst> values) implements PodValue {}

    private record PodProp(EcoSpaPod.SpaConst key, PodValue value) {}

    private record PodObject(EcoSpaPod.SpaConst objectType, EcoSpaPod.SpaConst objectId, IList<PodProp> props) {}

    private static final PodObject POD_AUDIO_F32_MONO_48K = new PodObject(
            EcoSpaPod.SPA_TYPE_OBJECT_FORMAT,
            EcoSpaPod.SPA_PARAM_ENUM_FORMAT,
            IList.of(
                    new PodProp(EcoSpaPod.SPA_FORMAT_MEDIA_TYPE, new PodId(EcoSpaPod.SPA_MEDIA_TYPE_AUDIO)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_MEDIA_SUBTYPE, new PodId(EcoSpaPod.SPA_MEDIA_SUBTYPE_RAW)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_AUDIO_FORMAT, new PodId(EcoSpaPod.SPA_AUDIO_FORMAT_F32)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_AUDIO_RATE, new PodInt(48000)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_AUDIO_CHANNELS, new PodInt(1)),
                    new PodProp(
                            EcoSpaPod.SPA_FORMAT_AUDIO_POSITION,
                            new PodIdArray(IList.of(EcoSpaPod.SPA_AUDIO_CHANNEL_UNKNOWN)))));

    private static final PodObject POD_MIDI_CONTROL = new PodObject(
            EcoSpaPod.SPA_TYPE_OBJECT_FORMAT,
            EcoSpaPod.SPA_PARAM_ENUM_FORMAT,
            IList.of(
                    new PodProp(EcoSpaPod.SPA_FORMAT_MEDIA_TYPE, new PodId(EcoSpaPod.SPA_MEDIA_TYPE_APPLICATION)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_MEDIA_SUBTYPE, new PodId(EcoSpaPod.SPA_MEDIA_SUBTYPE_CONTROL))));

    private static SourceExpr spaRef(EcoMb e, EcoSpaPod.SpaConst c) {
        return e.fld(e.v("spa"), c.name());
    }

    private static void podBuild(EcoMb e, TList<SourceExpr> body, String name, PodObject object) {
        var props = name + "_props";
        body.add(e.let(props, e.callM0(e.v("spa"), "new_pod")));
        for (var prop : object.props()) {
            body.add(e.callM(e.v(props), "prop", spaRef(e, prop.key())));
            body.add(
                    switch (prop.value()) {
                        case PodId id -> e.callM(e.v(props), "pod_id", spaRef(e, id.value()));
                        case PodInt i -> e.callM(e.v(props), "pod_int", e.intOf(i.value()));
                        case PodIdArray a -> {
                            var values = new TList<SourceExpr>();
                            for (var v : a.values()) values.add(spaRef(e, v));
                            yield e.callM(
                                    e.v(props),
                                    "pod_id_array",
                                    e.listNew(e.mb.typesInt(), values.toArray(SourceExpr[]::new)));
                        }
                    });
        }
        body.add(e.let(name + "_pod", e.callM0(e.v("spa"), "new_pod")));
        body.add(e.callM(
                e.v(name + "_pod"),
                "pod_object",
                e.rec(
                        e.p("ty", spaRef(e, object.objectType())),
                        e.p("id", spaRef(e, object.objectId())),
                        e.p("props", e.v(props)))));
    }

    static SourceExpr byteTuple(EcoMb e, int[] bytes) {
        var parts = new SourceExpr[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            parts[i] = e.n(bytes[i]);
        }
        return e.tup(parts);
    }

    static SourceExpr byteArrayType(EcoMb e, EcoExtern.Llvm llvm, SourceExpr count) {
        return e.call(llvm.llvmField("array"), e.rec(e.p("elem", llvm.llvmField("u8")), e.p("count", count)));
    }

    static SourceExpr byteArrayConstType(EcoMb e, EcoExtern.Llvm llvm, SourceExpr count) {
        return EcoExtern.globalT(e, llvm, e.call(llvm.llvmField("ptr"), byteArrayType(e, llvm, count)), false);
    }

    private static SourceExpr paramsArrayType(EcoMb e, EcoExtern.Llvm llvm) {
        return e.call(
                llvm.llvmField("array"),
                e.rec(e.p("elem", e.call(llvm.llvmField("ptr"), llvm.llvmField("void"))), e.p("count", e.intOf(1))));
    }

    private static SourceExpr paramsConstType(EcoMb e, EcoExtern.Llvm llvm) {
        return EcoExtern.globalT(e, llvm, e.call(llvm.llvmField("ptr"), paramsArrayType(e, llvm)), false);
    }

    private static SourceExpr globalConstStaged(EcoMb e, String ll) {
        return e.mb.as(e.v(ll), e.lower(e.fld(e.v("llvm"), "global_const_fn")));
    }

    private static SourceExpr podLen(EcoMb e, String name) {
        return e.callM0(e.v(name + "_pod"), "len");
    }

    private static SourceExpr podConst(EcoMb e, String ll, String globalName, String name) {
        return e.call(
                globalConstStaged(e, ll),
                e.rec(
                        e.p("name", e.s(globalName)),
                        e.p("ty", e.lower(byteArrayType(e, EcoExtern.Llvm.top(e), podLen(e, name)))),
                        e.p("init", e.lower(e.callM0(e.v(name + "_pod"), "bytes")))));
    }

    private static SourceExpr paramsConst(EcoMb e, String ll, String name, String pod) {
        return e.call(
                globalConstStaged(e, ll),
                e.rec(
                        e.p("name", e.s(name)),
                        e.p("ty", e.lower(paramsArrayType(e, EcoExtern.Llvm.top(e)))),
                        e.p("init", e.tup(e.v(pod)))));
    }

    public static SourceExpr module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        exprs.add(e.let("spa", e.importLocal("spa", "spa-pod-1.6.at")));

        exprs.add(e.let("d", e.define()));
        exprs.add(e.optionTypeLet());
        exprs.add(e.let("pw_o", e.defObject(e.v("d"), "pipewire 1.6")));
        var innerFields = new TList<EcoMb.P>();
        var members = new TList<String>();
        var refMembers = new TList<String>();
        for (var ex : EXTERNS) refMembers.add(ex.name());
        for (var k : KONSTS) refMembers.add(k.name());
        for (var g : new String[] {"pod_audio_f32_mono_48k", "pod_midi_control", "params_audio", "params_midi"}) {
            refMembers.add(g);
        }
        for (var m : refMembers) {
            innerFields.add(e.p(m, e.fld(e.v("llvm"), "ref_t")));
            innerFields.add(e.p(m + "_t", e.mb.evalType()));
            members.add(m);
        }
        for (var s : STRUCTS) {
            innerFields.add(e.p(s.name(), e.mb.evalType()));
            members.add(s.name());
        }
        exprs.add(e.callM(e.v("pw_o"), "set_inner", e.rec(innerFields.toArray(EcoMb.P[]::new))));
        for (var m : refMembers) {
            exprs.add(e.methodImpl(
                    e.v("pw_o"),
                    m,
                    e.tup(),
                    e.optionTypeSome(e.fld(e.v("llvm"), "ref_t")),
                    e.stage(e.fld(e.v("inner"), m))));
            exprs.add(e.methodImpl(
                    e.v("pw_o"),
                    m + "_t",
                    e.tup(),
                    e.optionTypeSome(e.mb.evalType()),
                    e.stage(e.fld(e.v("inner"), m + "_t"))));
        }
        for (var s : STRUCTS) {
            exprs.add(e.methodImpl(
                    e.v("pw_o"),
                    s.name(),
                    e.tup(),
                    e.optionTypeSome(e.mb.evalType()),
                    e.stage(e.fld(e.v("inner"), s.name()))));
        }

        podBuild(e, exprs, "audio", POD_AUDIO_F32_MONO_48K);
        podBuild(e, exprs, "midi", POD_MIDI_CONTROL);
        exprs.add(e.let("pw_builder", e.defFunc(e.v("d"), "pipewire")));
        var body = new TList<SourceExpr>();
        var llvm = EcoExtern.Llvm.staged(e);
        EcoExtern.bindTypes(e, body, llvm);
        for (var s : STRUCTS) {
            var fieldList = new TList<SourceExpr>();
            for (var f : s.fields()) {
                fieldList.add(e.call(
                        llvm.llvmField("field"), e.rec(e.p("name", e.s(f.name())), e.p("ty", e.v("t_" + f.ty())))));
            }
            var fields = fieldList.toArray(SourceExpr[]::new);
            body.add(e.let(
                    "t_" + s.name(),
                    e.call(
                            llvm.llvmField("struct"),
                            e.rec(e.p("name", e.s(s.name())), e.p("fields", e.listNew(e.mb.evalValue(), fields))))));
            body.add(e.let("t_p_" + s.name(), e.call(llvm.llvmField("ptr"), e.v("t_" + s.name()))));
        }
        var ctorFields = new TList<EcoMb.P>();
        for (var ex : EXTERNS) {
            body.add(e.let(ex.name(), ex.declare(e, "arg", llvm)));
            body.add(e.let(ex.name() + "_t", ex.constType(e, llvm)));
        }
        for (var k : KONSTS) {
            body.add(e.let(
                    k.name(),
                    e.call(
                            e.mb.as(e.v("arg"), e.lower(e.fld(e.v("llvm"), "constant_fn"))),
                            e.rec(e.p("ty", e.lower(e.fld(e.v("llvm"), "u32"))), e.p("value", e.n(k.value()))))));
            body.add(e.let(k.name() + "_t", EcoExtern.globalT(e, llvm, e.v("t_u32"), false)));
        }
        for (var s : STRUCTS) {
            ctorFields.add(e.p(s.name(), e.v("t_" + s.name())));
        }
        body.add(e.let("pod_audio_f32_mono_48k", podConst(e, "arg", "pw.pod.audio_f32_mono_48k", "audio")));
        body.add(e.let("pod_midi_control", podConst(e, "arg", "pw.pod.midi_control", "midi")));
        body.add(e.let("pod_audio_f32_mono_48k_t", byteArrayConstType(e, llvm, e.lower(podLen(e, "audio")))));
        body.add(e.let("pod_midi_control_t", byteArrayConstType(e, llvm, e.lower(podLen(e, "midi")))));
        body.add(e.let("params_audio", paramsConst(e, "arg", "pw.params.audio", "pod_audio_f32_mono_48k")));
        body.add(e.let("params_audio_t", paramsConstType(e, llvm)));
        body.add(e.let("params_midi", paramsConst(e, "arg", "pw.params.midi", "pod_midi_control")));
        body.add(e.let("params_midi_t", paramsConstType(e, llvm)));
        for (var m : refMembers) {
            ctorFields.add(e.p(m, e.v(m)));
            ctorFields.add(e.p(m + "_t", e.v(m + "_t")));
        }
        body.add(e.ret(e.call(e.lower(e.fld(e.v("pw_o"), "new")), e.rec(ctorFields.toArray(EcoMb.P[]::new)))));
        exprs.add(e.funcImpl(
                e.v("pw_builder"),
                e.fld(e.v("llvm"), "mod_t"),
                e.optionTypeSome(e.fld(e.v("pw_o"), "type")),
                e.stage(e.mb.seq(body))));

        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.fld(e.v("pw_builder"), "func")));
        return e.mb.seq(exprs);
    }
}
