package compilerxyz.eval.value.numeric;

import compilerxyz.annotations.CodegenDescriptor;

public final class Unsigned {
    private Unsigned() {}

    @CodegenDescriptor
    public static double toDouble(long x) {
        if (x >= 0) return (double) x;
        return ((double) (x >>> 1)) * 2.0 + (double) (x & 1L);
    }

    @CodegenDescriptor
    public static float toFloat(long x) {
        if (x >= 0) return (float) x;
        return ((float) (x >>> 1)) * 2.0f + (float) (x & 1L);
    }
}
