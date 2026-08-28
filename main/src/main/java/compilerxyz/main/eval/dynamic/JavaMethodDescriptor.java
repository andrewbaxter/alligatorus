package compilerxyz.main.eval.dynamic;

import compilerxyz.prelude.util.TList;

public final class JavaMethodDescriptor {
    public static final JavaMethodDescriptor VOID_NOARGS = new JavaMethodDescriptor("()V");

    public final String value;

    public String value() {
        return value;
    }

    private JavaMethodDescriptor(String value) {
        this.value = value;
    }

    public static JavaMethodDescriptor fromParts(
            JavaDataDescriptor returnDescriptor, TList<JavaDataDescriptor> argDescriptors) {
        var b = new StringBuilder("(");
        for (var d : argDescriptors) b.append(d.value);
        b.append(')').append(returnDescriptor.value);
        return new JavaMethodDescriptor(b.toString());
    }

    public static JavaMethodDescriptor fromConstructorParts(TList<JavaDataDescriptor> argDescriptors) {
        return fromParts(JavaDataDescriptor.DESC_VOID, argDescriptors);
    }

    public static JavaMethodDescriptor fromParts(
            JavaDataDescriptor returnDescriptor, JavaDataDescriptor... argDescriptors) {
        return fromParts(returnDescriptor, TList.of(argDescriptors));
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JavaMethodDescriptor other && other.value.equals(this.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
