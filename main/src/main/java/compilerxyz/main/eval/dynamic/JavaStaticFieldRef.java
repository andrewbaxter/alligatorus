package compilerxyz.main.eval.dynamic;

public final class JavaStaticFieldRef {
    public final JavaInternalName owner;
    public final String name;
    public final JavaDataDescriptor descriptor;

    public JavaStaticFieldRef(JavaInternalName owner, String name, JavaDataDescriptor descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }
}
