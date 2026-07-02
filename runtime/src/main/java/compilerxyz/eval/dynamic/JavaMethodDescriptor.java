package compilerxyz.eval.dynamic;

import compilerxyz.util.TList;

public class JavaMethodDescriptor {
  public final String value;

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
