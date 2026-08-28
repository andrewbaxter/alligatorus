package compilerxyz.eval.dynamic;

import compilerxyz.eval.DefSetId;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.util.Opt;

public final class SourceUnionEmitter {
    private SourceUnionEmitter() {}

    public static String baseBinaryNameFor(DefSetId defSet, long unionIndex) {
        var compileId = defSet.compileId();
        return "SourceUnion$"
                + compileId.importState().value()
                + "_"
                + compileId.source().value()
                + "_"
                + defSet.index()
                + "_"
                + unionIndex;
    }

    public static String variantBinaryNameFor(DefSetId defSet, long unionIndex, int variantIndex) {
        return baseBinaryNameFor(defSet, unionIndex) + "$V" + variantIndex;
    }

    public static final String PAYLOAD_FIELD = "payload";

    public static JavaDataDescriptor payloadDescriptor(ExprContext xx, Type type) {
        return switch (GenAspectConsumeDynamic.getMaybe(xx, type)) {
            case Opt.Some<AspectConsumeDynamic> some -> some.value().aspectConsumeDynamicJvmClass(xx);
            case Opt.None<AspectConsumeDynamic> n -> JvmConstants.DESC_OBJECT;
        };
    }
}
