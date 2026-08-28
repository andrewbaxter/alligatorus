package compilerxyz.main.eval.dynamic;

public final class JavaInternalName {
    public final String value;

    public String value() {
        return value;
    }

    public JavaInternalName(String value) {
        this.value = value;
    }

    public static JavaInternalName of(String value) {
        return new JavaInternalName(value);
    }

    public static JavaInternalName fromClass(Class<?> klass) {
        return new JavaInternalName(klass.getName().replace('.', '/'));
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JavaInternalName other && other.value.equals(this.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
