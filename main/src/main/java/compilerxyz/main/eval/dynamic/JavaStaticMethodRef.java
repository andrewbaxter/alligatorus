package compilerxyz.main.eval.dynamic;

public final class JavaStaticMethodRef {
    public final JavaInternalName owner;
    public final String name;
    public final JavaMethodDescriptor descriptor;
    public final boolean ownerIsInterface;

    public JavaStaticMethodRef(
            JavaInternalName owner, String name, JavaMethodDescriptor descriptor, boolean ownerIsInterface) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.ownerIsInterface = ownerIsInterface;
    }
}
