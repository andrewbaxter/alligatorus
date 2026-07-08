package compilerxyz.eval.dynamic;

import compilerxyz.util.TList;

public class JavaMethodDescriptor {
  public static final JavaMethodDescriptor VOID_NOARGS =
      new JavaMethodDescriptor("()V");

  public final String value;

    public String value() { return value; }

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

  public static JavaMethodDescriptor fromConstructorParts(JavaDataDescriptor... argDescriptors) {
    return fromParts(JavaDataDescriptor.DESC_VOID, TList.of(argDescriptors));
  }

  public TList<JavaDataDescriptor> parameterDescriptors() {
    var out = new TList<JavaDataDescriptor>();
    if (value.isEmpty() || value.charAt(0) != '(') {
      throw new IllegalStateException("JavaMethodDescriptor missing '(': " + value);
    }
    int i = 1;
    while (i < value.length() && value.charAt(i) != ')') {
      int start = i;
      while (i < value.length() && value.charAt(i) == '[') i++;
      char c = value.charAt(i);
      if (c == 'L') {
        while (i < value.length() && value.charAt(i) != ';') i++;
        i++;
      } else {
        i++;
      }
      out.add(JavaDataDescriptor.create(value.substring(start, i)));
    }
    return out;
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
