package compilerxyz.eval.value.numeric;

import compilerxyz.annotations.CodegenDescriptor;

public final class NumericFormat {
    private NumericFormat() {}

    @CodegenDescriptor
    public static String formatByte(byte v) {
        return Byte.toString(v);
    }

    @CodegenDescriptor
    public static String formatShort(short v) {
        return Short.toString(v);
    }

    @CodegenDescriptor
    public static String formatInt(int v) {
        return Integer.toString(v);
    }

    @CodegenDescriptor
    public static String formatLong(long v) {
        return Long.toString(v);
    }

    @CodegenDescriptor
    public static String formatFloat(float v) {
        return Float.toString(v);
    }

    @CodegenDescriptor
    public static String formatDouble(double v) {
        return Double.toString(v);
    }

    @CodegenDescriptor
    public static String formatUnsignedByte(byte v) {
        return Integer.toString(Byte.toUnsignedInt(v));
    }

    @CodegenDescriptor
    public static String formatUnsignedShort(short v) {
        return Integer.toString(Short.toUnsignedInt(v));
    }

    @CodegenDescriptor
    public static String formatUnsignedInt(int v) {
        return Integer.toUnsignedString(v);
    }

    @CodegenDescriptor
    public static String formatUnsignedLong(long v) {
        return Long.toUnsignedString(v);
    }
}
