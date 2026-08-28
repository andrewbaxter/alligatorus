package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;

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
