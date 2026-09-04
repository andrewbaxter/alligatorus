package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;

public final class SourceUnionEmitter {
    private SourceUnionEmitter() {}

    public static String variantBinaryNameFor(String baseBinaryName, int variantIndex) {
        return baseBinaryName + "$V" + variantIndex;
    }

    public static final String PAYLOAD_FIELD = "payload";

    public static JavaDataDescriptor payloadDescriptor(ExprContext xx, Type type) {
        return JvmSort.descriptor(xx, type);
    }
}
