package compilerxyz.eval.dynamic;

import compilerxyz.annotations.BuiltinFull;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@BuiltinFull(uniqueId = 6014L)
public final class JavaDataDescriptor {
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

    public String value() { return value; }

    public JavaDataDescriptor(String value) {
        this.value = value;
    }

    public static JavaDataDescriptor create(String value) {
        return new JavaDataDescriptor(value);
    }

    public static JavaDataDescriptor fromJVMName(JavaInternalName name) {
        return new JavaDataDescriptor("L" + name.value + ";");
    }

    public static JavaDataDescriptor fromInternalName(JavaInternalName name) {
        return fromJVMName(name);
    }

    public static JavaDataDescriptor fromClass(Class<?> t) {
        if (t.isPrimitive()) {
            return switch (t.getName()) {
                case "int" -> DESC_INT;
                case "byte" -> DESC_BYTE;
                case "char" -> DESC_CHAR;
                case "short" -> DESC_SHORT;
                case "long" -> DESC_LONG;
                case "boolean" -> DESC_BOOL;
                case "float" -> DESC_FLOAT;
                case "double" -> DESC_DOUBLE;
                case "void" -> DESC_VOID;
                default -> throw new AssertionError("Unknown primitive: " + t);
            };
        }
        if (t.isArray()) return new JavaDataDescriptor("[" + fromClass(t.getComponentType()).value);
        return fromJVMName(JavaInternalName.fromClass(t));
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
