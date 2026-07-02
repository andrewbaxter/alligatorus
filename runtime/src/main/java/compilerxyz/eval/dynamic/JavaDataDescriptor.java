package compilerxyz.eval.dynamic;

public class JavaDataDescriptor {
    public static final JavaDataDescriptor DESC_VOID = new JavaDataDescriptor("V");
    public static final JavaDataDescriptor DESC_INT = new JavaDataDescriptor("I");
    public static final JavaDataDescriptor DESC_BYTE = new JavaDataDescriptor("B");
    public static final JavaDataDescriptor DESC_CHAR = new JavaDataDescriptor("C");
    public static final JavaDataDescriptor DESC_SHORT = new JavaDataDescriptor("S");
    public static final JavaDataDescriptor DESC_LONG = new JavaDataDescriptor("J");
    public static final JavaDataDescriptor DESC_BOOL = new JavaDataDescriptor("Z");
    public static final JavaDataDescriptor DESC_FLOAT = new JavaDataDescriptor("F");
    public static final JavaDataDescriptor DESC_DOUBLE = new JavaDataDescriptor("D");

    public final String value;

    public JavaDataDescriptor(String value) {
        this.value = value;
    }

    public static JavaDataDescriptor create(String value) {
        return new JavaDataDescriptor(value);
    }

    public static JavaDataDescriptor fromJVMName(JavaInternalName name) {
        return new JavaDataDescriptor("L" + name.value + ";");
    }

    public static JavaDataDescriptor fromObjectClass(Class<?> klass) {
        return fromJVMName(JavaInternalName.fromClass(klass));
    }

    public static JavaDataDescriptor fromClass(Class<?> t) {
        if (t == int.class) return DESC_INT;
        if (t == byte.class) return DESC_BYTE;
        if (t == char.class) return DESC_CHAR;
        if (t == short.class) return DESC_SHORT;
        if (t == long.class) return DESC_LONG;
        if (t == boolean.class) return DESC_BOOL;
        if (t == float.class) return DESC_FLOAT;
        if (t == double.class) return DESC_DOUBLE;
        if (t == void.class) return DESC_VOID;
        if (t.isPrimitive()) throw new AssertionError("Unknown primitive: " + t);
        if (t.isArray()) return new JavaDataDescriptor("[" + fromClass(t.getComponentType()).value);
        return fromObjectClass(t);
    }

    public JavaDataDescriptor array() {
        return new JavaDataDescriptor("[" + value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JavaDataDescriptor other && other.value.equals(this.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
