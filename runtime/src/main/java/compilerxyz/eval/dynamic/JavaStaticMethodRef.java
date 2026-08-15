package compilerxyz.eval.dynamic;

/**
 * A fully-resolved reference to a static method (owner internal name + method name + descriptor),
 * generated from an {@code @CodegenDescriptor}-annotated method so emit sites carry no string method
 * names.
 */
public final class JavaStaticMethodRef {
  public final JavaInternalName owner;
  public final String name;
  public final JavaMethodDescriptor descriptor;
  public final boolean ownerIsInterface;

  public JavaStaticMethodRef(
      JavaInternalName owner, String name, JavaMethodDescriptor descriptor,
      boolean ownerIsInterface) {
    this.owner = owner;
    this.name = name;
    this.descriptor = descriptor;
    this.ownerIsInterface = ownerIsInterface;
  }
}
