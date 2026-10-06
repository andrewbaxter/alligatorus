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
            new Struct("spa_pod", IList.of(new Field("size", "u32"), new Field("type", "u32"))),
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

    private record TailStruct(
            String name, IList<Field> fields, IList<Field> views, String after, IList<String> len, int align) {}

    private static final IList<TailStruct> TAIL_STRUCTS = IList.of(
            new TailStruct(
                    "spa_pod_control",
                    IList.of(new Field("offset", "u32"), new Field("type", "u32"), new Field("value", "spa_pod")),
                    IList.of(new Field("body", "u8"), new Field("words", "u32")),
                    "value",
                    IList.of("value", "size"),
                    8),
            new TailStruct(
                    "spa_pod_sequence",
                    IList.of(new Field("pod", "spa_pod"), new Field("unit", "u32"), new Field("pad", "u32")),
                    IList.of(new Field("controls", "spa_pod_control")),
                    "pod",
                    IList.of("pod", "size"),
                    8));

    private static final IList<Field> SPA_DATA_FIELDS = IList.of(
            new Field("type", "u32"),
            new Field("flags", "u32"),
            new Field("fd", "i64"),
            new Field("mapoffset", "u32"),
            new Field("maxsize", "u32"));

    private static final IList<Field> PW_BUFFER_FIELDS = IList.of(
            new Field("user_data", "pv"),
            new Field("size", "u64"),
            new Field("requested", "u64"),
            new Field("time", "u64"));

    private static SourceExpr field(EcoMb e, EcoExtern.Llvm llvm, String name, SourceExpr ty) {
        return e.call(llvm.llvmField("field"), e.rec(e.p("name", e.s(name)), e.p("ty", ty)));
    }

    private static SourceExpr fieldList(EcoMb e, EcoExtern.Llvm llvm, TList<SourceExpr> fields) {
        return e.listNew(llvm.llvmField("field_t"), fields.toArray(SourceExpr[]::new));
    }

    private static SourceExpr structOf(EcoMb e, EcoExtern.Llvm llvm, SourceExpr name, TList<SourceExpr> fields) {
        return e.call(llvm.llvmField("struct"), e.rec(e.p("name", name), e.p("fields", fieldList(e, llvm, fields))));
    }

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

    private static final PodObject POD_AUDIO_F32_STEREO_44K1 = new PodObject(
            EcoSpaPod.SPA_TYPE_OBJECT_FORMAT,
            EcoSpaPod.SPA_PARAM_ENUM_FORMAT,
            IList.of(
                    new PodProp(EcoSpaPod.SPA_FORMAT_MEDIA_TYPE, new PodId(EcoSpaPod.SPA_MEDIA_TYPE_AUDIO)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_MEDIA_SUBTYPE, new PodId(EcoSpaPod.SPA_MEDIA_SUBTYPE_RAW)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_AUDIO_FORMAT, new PodId(EcoSpaPod.SPA_AUDIO_FORMAT_F32)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_AUDIO_RATE, new PodInt(44100)),
                    new PodProp(EcoSpaPod.SPA_FORMAT_AUDIO_CHANNELS, new PodInt(2)),
                    new PodProp(
                            EcoSpaPod.SPA_FORMAT_AUDIO_POSITION,
                            new PodIdArray(IList.of(EcoSpaPod.SPA_AUDIO_CHANNEL_FL, EcoSpaPod.SPA_AUDIO_CHANNEL_FR)))));

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

    static SourceExpr byteArrayType(EcoMb e, EcoExtern.Llvm llvm, SourceExpr count) {
        return e.call(llvm.llvmField("array"), e.rec(e.p("elem", llvm.llvmField("u8")), e.p("count", count)));
    }

    private record Global(String name, String globalName, String init) {}

    private static final IList<Global> POD_GLOBALS = IList.of(
            new Global("pod_audio_f32_mono_48k", "pw.pod.audio_f32_mono_48k", "audio"),
            new Global("pod_audio_f32_stereo_44k1", "pw.pod.audio_f32_stereo_44k1", "audio_stereo_44k1"),
            new Global("pod_midi_control", "pw.pod.midi_control", "midi"));

    private static final IList<Global> PARAMS_GLOBALS = IList.of(
            new Global("params_audio", "pw.params.audio", "pod_audio_f32_mono_48k"),
            new Global("params_audio_stereo_44k1", "pw.params.audio_stereo_44k1", "pod_audio_f32_stereo_44k1"),
            new Global("params_midi", "pw.params.midi", "pod_midi_control"));

    public static TList<SourceExpr> module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.atz")));
        exprs.add(e.let("spa", e.importLocal("spa", "spa-pod-1.6.atz")));
        var llvm = EcoExtern.Llvm.top(e);
        EcoExtern.bindTypes(e, exprs, llvm);
        for (var s : STRUCTS) {
            var fields = new TList<SourceExpr>();
            for (var f : s.fields()) fields.add(field(e, llvm, f.name(), e.v("t_" + f.ty())));
            exprs.add(e.let("t_" + s.name(), structOf(e, llvm, e.s(s.name()), fields)));
            exprs.add(e.let("t_p_" + s.name(), e.call(llvm.llvmField("ptr"), e.v("t_" + s.name()))));
        }
        for (var s : TAIL_STRUCTS) {
            var fields = new TList<SourceExpr>();
            for (var f : s.fields()) fields.add(field(e, llvm, f.name(), e.v("t_" + f.ty())));
            var views = new TList<SourceExpr>();
            for (var f : s.views()) views.add(field(e, llvm, f.name(), e.v("t_" + f.ty())));
            var len = new TList<SourceExpr>();
            for (var seg : s.len()) len.add(e.s(seg));
            exprs.add(e.let(
                    "t_" + s.name(),
                    e.call(
                            llvm.llvmField("struct_tail"),
                            e.rec(
                                    e.p("name", e.s(s.name())),
                                    e.p("fields", fieldList(e, llvm, fields)),
                                    e.p(
                                            "tail",
                                            e.call(
                                                    llvm.llvmField("tail"),
                                                    e.rec(
                                                            e.p("views", fieldList(e, llvm, views)),
                                                            e.p("after", e.s(s.after())),
                                                            e.p(
                                                                    "len",
                                                                    e.listNew(
                                                                            e.mb.typesString(),
                                                                            len.toArray(SourceExpr[]::new))),
                                                            e.p("align", e.n(s.align())))))))));
        }
        var members = new TList<String>();
        for (var ex : EXTERNS) {
            members.add(ex.name());
            exprs.add(e.let(ex.name() + "_fn_t", ex.fnType(e, llvm)));
            exprs.add(e.let(ex.name() + "_t", EcoExtern.globalT(e, llvm, e.v(ex.name() + "_fn_t"))));
        }
        exprs.add(e.let("konst_t", EcoExtern.globalT(e, llvm, e.v("t_u32"))));
        for (var k : KONSTS) {
            members.add(k.name());
            exprs.add(e.let(k.name() + "_t", e.v("konst_t")));
        }
        podBuild(e, exprs, "audio", POD_AUDIO_F32_MONO_48K);
        podBuild(e, exprs, "audio_stereo_44k1", POD_AUDIO_F32_STEREO_44K1);
        podBuild(e, exprs, "midi", POD_MIDI_CONTROL);
        exprs.add(e.let("const_bytes", e.call(llvm.llvmField("const_list"), llvm.llvmField("u8"))));
        for (var g : POD_GLOBALS) {
            members.add(g.name());
            exprs.add(e.let(g.name() + "_t", llvm.llvmField("gref_t")));
        }
        exprs.add(e.let(
                "params_arr_t",
                e.call(llvm.llvmField("array"), e.rec(e.p("elem", e.v("t_pv")), e.p("count", e.intOf(1))))));
        for (var g : PARAMS_GLOBALS) {
            members.add(g.name());
            exprs.add(e.let(
                    g.name() + "_t", EcoExtern.globalT(e, llvm, e.call(llvm.llvmField("ptr"), e.v("params_arr_t")))));
        }

        exprs.add(e.let("d", e.define()));
        exprs.add(e.optionTypeLet());
        exprs.add(e.let("pw_o", e.defObject(e.v("d"), "pipewire 1.6")));
        var innerFields = new TList<EcoMb.P>();
        for (var m : members) innerFields.add(e.p(m, e.v(m + "_t")));
        exprs.add(e.objectImpl(e.v("pw_o"), e.rec(innerFields.toArray(EcoMb.P[]::new))));
        for (var m : members) {
            exprs.add(e.methodImpl(
                    e.v("pw_o"), m, e.tup(), e.optionTypeSome(e.v(m + "_t")), e.stage(e.fld(e.v("inner"), m))));
        }
        var structNames = new TList<String>();
        for (var s : STRUCTS) structNames.add(s.name());
        for (var s : TAIL_STRUCTS) structNames.add(s.name());
        for (var name : structNames) {
            exprs.add(e.methodImpl(
                    e.v("pw_o"), name, e.tup(), e.optionTypeSome(e.mb.evalType()), e.stage(e.lowerV("t_" + name))));
        }
        EcoExtern.Llvm llvmLowered = name -> e.lower(e.fld(e.v("llvm"), name));
        var spaData = new TList<SourceExpr>();
        for (var f : SPA_DATA_FIELDS) spaData.add(field(e, llvmLowered, f.name(), e.lowerV("t_" + f.ty())));
        spaData.add(field(e, llvmLowered, "data", e.call(llvmLowered.llvmField("ptr"), e.fld(e.v("arg"), "payload"))));
        spaData.add(field(e, llvmLowered, "chunk", e.lowerV("t_p_spa_chunk")));
        var spaBuffer = TList.of(
                field(e, llvmLowered, "n_metas", e.lowerV("t_u32")),
                field(e, llvmLowered, "n_datas", e.lowerV("t_u32")),
                field(e, llvmLowered, "metas", e.lowerV("t_pv")),
                field(e, llvmLowered, "datas", e.call(llvmLowered.llvmField("ptr"), e.v("spa_data"))));
        var pwBuffer =
                TList.of(field(e, llvmLowered, "buffer", e.call(llvmLowered.llvmField("ptr"), e.v("spa_buffer"))));
        for (var f : PW_BUFFER_FIELDS) pwBuffer.add(field(e, llvmLowered, f.name(), e.lowerV("t_" + f.ty())));
        exprs.add(e.methodImpl(
                e.v("pw_o"),
                "buffer_t",
                e.rec(e.p("name", e.mb.typesString()), e.p("payload", e.mb.evalType())),
                e.optionTypeSome(e.mb.evalType()),
                e.stage(e.seq(
                        e.let(
                                "spa_data",
                                structOf(e, llvmLowered, e.cat(e.s("spa_data."), e.fld(e.v("arg"), "name")), spaData)),
                        e.let(
                                "spa_buffer",
                                structOf(
                                        e,
                                        llvmLowered,
                                        e.cat(e.s("spa_buffer."), e.fld(e.v("arg"), "name")),
                                        spaBuffer)),
                        e.ret(structOf(
                                e, llvmLowered, e.cat(e.s("pw_buffer."), e.fld(e.v("arg"), "name")), pwBuffer))))));
        exprs.add(e.methodImpl(
                e.v("pw_o"),
                "dequeue_buffer_t",
                e.mb.evalType(),
                e.optionTypeSome(e.mb.evalType()),
                e.stage(e.call(
                        llvmLowered.llvmField("fn"),
                        e.rec(
                                e.p("params", e.listNew(e.mb.evalType(), e.lowerV("t_pv"))),
                                e.p("ret", e.call(llvmLowered.llvmField("ptr"), e.v("arg"))))))));
        exprs.add(e.methodImpl(
                e.v("pw_o"),
                "queue_buffer_t",
                e.mb.evalType(),
                e.optionTypeSome(e.mb.evalType()),
                e.stage(e.call(
                        llvmLowered.llvmField("fn"),
                        e.rec(
                                e.p(
                                        "params",
                                        e.listNew(
                                                e.mb.evalType(),
                                                e.lowerV("t_pv"),
                                                e.call(llvmLowered.llvmField("ptr"), e.v("arg")))),
                                e.p("ret", e.lowerV("t_i32")))))));

        exprs.add(e.let("pw_builder", e.defFunc(e.v("d"), "pipewire")));
        var body = new TList<SourceExpr>();
        for (var ex : EXTERNS) {
            body.add(e.let(
                    ex.name(),
                    ex.declare(
                            e, e.lower(e.fld(e.v("llvm"), "extern_fn")), e.v("arg"), e.lowerV(ex.name() + "_fn_t"))));
        }
        for (var k : KONSTS) {
            body.add(e.let(
                    k.name(),
                    e.call(
                            e.lower(e.fld(e.v("llvm"), "constant_fn")),
                            e.rec(e.p("ty", e.lowerV("t_u32")), e.p("value", e.n(k.value()))))));
        }
        for (var g : POD_GLOBALS) {
            body.add(e.let(
                    g.name(),
                    e.call(
                            e.lowerV("const_bytes"),
                            e.rec(
                                    e.p("mod", e.v("arg")),
                                    e.p("name", e.s(g.globalName())),
                                    e.p("values", e.lower(e.callM0(e.v(g.init() + "_pod"), "bytes")))))));
        }
        for (var g : PARAMS_GLOBALS) {
            body.add(e.let(
                    g.name(),
                    e.call(
                            e.lower(e.fld(e.v("llvm"), "global_const_fn")),
                            e.rec(
                                    e.p("mod", e.v("arg")),
                                    e.p("name", e.s(g.globalName())),
                                    e.p("ty", e.lowerV("params_arr_t")),
                                    e.p("init", e.tup(e.v(g.init())))))));
        }
        var ctorFields = new TList<EcoMb.P>();
        for (var m : members) ctorFields.add(e.p(m, e.v(m)));
        body.add(e.callM(e.v("arg"), "lib", e.s("pipewire-0.3")));
        body.add(e.ret(e.call(e.lower(e.fld(e.v("pw_o"), "new")), e.rec(ctorFields.toArray(EcoMb.P[]::new)))));
        exprs.add(e.funcImpl(
                e.v("pw_builder"),
                e.fld(e.v("llvm"), "mod_t"),
                e.optionTypeSome(e.fld(e.v("pw_o"), "type")),
                e.mb.optNoneType(),
                e.stage(e.mb.seq(body))));

        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.fld(e.v("pw_builder"), "func")));
        return exprs;
    }
}
