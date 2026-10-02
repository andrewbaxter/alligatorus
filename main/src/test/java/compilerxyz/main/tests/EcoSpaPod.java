package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;

public final class EcoSpaPod {
    private EcoSpaPod() {}

    public record SpaConst(String name, int value) {}

    static final SpaConst SPA_TYPE_ID = new SpaConst("type_id", 3);
    static final SpaConst SPA_TYPE_INT = new SpaConst("type_int", 4);
    static final SpaConst SPA_TYPE_ARRAY = new SpaConst("type_array", 13);
    static final SpaConst SPA_TYPE_OBJECT = new SpaConst("type_object", 15);
    static final SpaConst SPA_TYPE_OBJECT_FORMAT = new SpaConst("type_object_format", 0x40003);
    static final SpaConst SPA_PARAM_ENUM_FORMAT = new SpaConst("param_enum_format", 3);
    static final SpaConst SPA_FORMAT_MEDIA_TYPE = new SpaConst("format_media_type", 1);
    static final SpaConst SPA_FORMAT_MEDIA_SUBTYPE = new SpaConst("format_media_subtype", 2);
    static final SpaConst SPA_FORMAT_AUDIO_FORMAT = new SpaConst("format_audio_format", 0x10001);
    static final SpaConst SPA_FORMAT_AUDIO_RATE = new SpaConst("format_audio_rate", 0x10003);
    static final SpaConst SPA_FORMAT_AUDIO_CHANNELS = new SpaConst("format_audio_channels", 0x10004);
    static final SpaConst SPA_FORMAT_AUDIO_POSITION = new SpaConst("format_audio_position", 0x10005);
    static final SpaConst SPA_MEDIA_TYPE_AUDIO = new SpaConst("media_type_audio", 1);
    static final SpaConst SPA_MEDIA_TYPE_APPLICATION = new SpaConst("media_type_application", 6);
    static final SpaConst SPA_MEDIA_SUBTYPE_RAW = new SpaConst("media_subtype_raw", 1);
    static final SpaConst SPA_MEDIA_SUBTYPE_CONTROL = new SpaConst("media_subtype_control", 0x60001);
    static final SpaConst SPA_AUDIO_FORMAT_F32 = new SpaConst("audio_format_f32", 0x11b);
    static final SpaConst SPA_AUDIO_CHANNEL_UNKNOWN = new SpaConst("audio_channel_unknown", 0);

    private static final IList<SpaConst> SPA_CONSTS = IList.of(
            SPA_TYPE_ID,
            SPA_TYPE_INT,
            SPA_TYPE_ARRAY,
            SPA_TYPE_OBJECT,
            SPA_TYPE_OBJECT_FORMAT,
            SPA_PARAM_ENUM_FORMAT,
            SPA_FORMAT_MEDIA_TYPE,
            SPA_FORMAT_MEDIA_SUBTYPE,
            SPA_FORMAT_AUDIO_FORMAT,
            SPA_FORMAT_AUDIO_RATE,
            SPA_FORMAT_AUDIO_CHANNELS,
            SPA_FORMAT_AUDIO_POSITION,
            SPA_MEDIA_TYPE_AUDIO,
            SPA_MEDIA_TYPE_APPLICATION,
            SPA_MEDIA_SUBTYPE_RAW,
            SPA_MEDIA_SUBTYPE_CONTROL,
            SPA_AUDIO_FORMAT_F32,
            SPA_AUDIO_CHANNEL_UNKNOWN);

    private static SourceExpr spa(EcoMb e, SpaConst c) {
        return e.lower(e.v(c.name()));
    }

    private static SourceExpr podNew(EcoMb e) {
        return e.call(
                e.lower(e.fld(e.v("pod_b"), "new")), e.rec(e.p("bytes", e.listNew(e.mb.builtin("types", "ubyte")))));
    }

    private static SourceExpr podBytes(EcoMb e) {
        return e.fld(e.v("inner"), "bytes");
    }

    private static SourceExpr self(EcoMb e, String method, SourceExpr arg) {
        return e.callM(e.v("outer"), method, arg);
    }

    private static SourceExpr byteOf(EcoMb e, SourceExpr v, int shift) {
        return e.mb.operatorAnd(shift == 0 ? v : e.mb.operatorUshr(v, e.intOf(shift)), e.intOf(255));
    }

    private static void podMethod(
            EcoMb e, TList<SourceExpr> out, String name, SourceExpr arg, SourceExpr ret, SourceExpr body) {
        out.add(e.methodImpl(e.v("pod_b"), name, arg, ret, e.stage(body)));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("d", e.define()));
        exprs.add(e.optionTypeLet());
        var podT = e.fld(e.v("pod_b"), "type");
        var intT = e.mb.typesInt();
        var byteT = e.mb.builtin("types", "ubyte");
        for (var c : SPA_CONSTS) exprs.add(e.let(c.name(), e.intOf(c.value())));
        exprs.add(e.let("pod_b", e.defObject(e.v("d"), "spa pod builder")));
        exprs.add(e.objectImpl(e.v("pod_b"), e.rec(e.p("bytes", e.listType(byteT)))));
        podMethod(e, exprs, "bytes", e.tup(), e.optionTypeSome(e.listType(byteT)), podBytes(e));
        podMethod(
                e,
                exprs,
                "len",
                e.tup(),
                e.optionTypeSome(intT),
                e.seq(
                        e.let("n", e.rec(e.p("v", e.intOf(0)))),
                        e.foreach(podBytes(e), "_", e.incr(e.fld(e.v("n"), "v"))),
                        e.ret(e.fld(e.v("n"), "v"))));
        podMethod(
                e,
                exprs,
                "u32",
                intT,
                e.optionTypeNone(),
                e.seq(
                        e.push(podBytes(e), e.mb.as(byteOf(e, e.v("arg"), 0), byteT)),
                        e.push(podBytes(e), e.mb.as(byteOf(e, e.v("arg"), 8), byteT)),
                        e.push(podBytes(e), e.mb.as(byteOf(e, e.v("arg"), 16), byteT)),
                        e.push(podBytes(e), e.mb.as(byteOf(e, e.v("arg"), 24), byteT))));
        var padSteps = new SourceExpr[7];
        for (int i = 0; i < padSteps.length; i++) {
            padSteps[i] = e.ifOnly(
                    e.mb.operatorNe(e.mb.operatorMod(self(e, "len", e.tup()), e.intOf(8)), e.intOf(0)),
                    e.push(podBytes(e), e.mb.as(e.n(0), e.mb.builtin("types", "ubyte"))));
        }
        podMethod(e, exprs, "pad", e.tup(), e.optionTypeNone(), e.seq(padSteps));
        podMethod(
                e,
                exprs,
                "append",
                podT,
                e.optionTypeNone(),
                e.foreach(e.callM(e.v("arg"), "bytes", e.tup()), "b", e.push(podBytes(e), e.v("b"))));
        podMethod(
                e,
                exprs,
                "pod",
                e.rec(e.p("ty", intT), e.p("body", podT)),
                e.optionTypeNone(),
                e.seq(
                        self(e, "u32", e.callM(e.fld(e.v("arg"), "body"), "len", e.tup())),
                        self(e, "u32", e.fld(e.v("arg"), "ty")),
                        self(e, "append", e.fld(e.v("arg"), "body")),
                        self(e, "pad", e.tup())));
        for (var scalar : new String[] {"pod_id", "pod_int"}) {
            podMethod(
                    e,
                    exprs,
                    scalar,
                    intT,
                    e.optionTypeNone(),
                    e.seq(
                            e.let("b", podNew(e)),
                            e.callM(e.v("b"), "u32", e.v("arg")),
                            self(
                                    e,
                                    "pod",
                                    e.rec(
                                            e.p("ty", spa(e, scalar.equals("pod_id") ? SPA_TYPE_ID : SPA_TYPE_INT)),
                                            e.p("body", e.v("b"))))));
        }
        podMethod(
                e,
                exprs,
                "pod_id_array",
                e.listType(intT),
                e.optionTypeNone(),
                e.seq(
                        e.let("b", podNew(e)),
                        e.callM(e.v("b"), "u32", e.intOf(4)),
                        e.callM(e.v("b"), "u32", spa(e, SPA_TYPE_ID)),
                        e.foreach(e.v("arg"), "v", e.callM(e.v("b"), "u32", e.v("v"))),
                        self(e, "pod", e.rec(e.p("ty", spa(e, SPA_TYPE_ARRAY)), e.p("body", e.v("b"))))));
        podMethod(
                e,
                exprs,
                "prop",
                intT,
                e.optionTypeNone(),
                e.seq(self(e, "u32", e.v("arg")), self(e, "u32", e.intOf(0))));
        podMethod(
                e,
                exprs,
                "pod_object",
                e.rec(e.p("ty", intT), e.p("id", intT), e.p("props", podT)),
                e.optionTypeNone(),
                e.seq(
                        e.let("b", podNew(e)),
                        e.callM(e.v("b"), "u32", e.fld(e.v("arg"), "ty")),
                        e.callM(e.v("b"), "u32", e.fld(e.v("arg"), "id")),
                        e.callM(e.v("b"), "append", e.fld(e.v("arg"), "props")),
                        self(e, "pod", e.rec(e.p("ty", spa(e, SPA_TYPE_OBJECT)), e.p("body", e.v("b"))))));
        exprs.add(e.let("new_pod_b", e.defFunc(e.v("d"), "new_pod")));
        exprs.add(
                e.funcImpl(e.v("new_pod_b"), e.tup(), e.optionTypeSome(podT), e.mb.optNoneType(), e.stage(podNew(e))));
        exprs.add(e.finish(e.v("d")));
        var exports = new TList<EcoMb.P>();
        exports.add(e.p("new_pod", e.fld(e.v("new_pod_b"), "func")));
        exports.add(e.p("pod_t", podT));
        for (var c : SPA_CONSTS) exports.add(e.p(c.name(), e.v(c.name())));
        exprs.add(e.ret(e.rec(exports.toArray(EcoMb.P[]::new))));
        return exprs;
    }
}
