package compilerxyz.codegen.util;

import com.squareup.javapoet.CodeBlock;
import compilerxyz.util.TList;

public record PushMethod(
    String name, TList<NamedTypeRef> args, String ret, CodeBlock body, String dynRuntimeMethod) {
  public PushMethod(String name, TList<NamedTypeRef> args, String ret, CodeBlock body) {
    this(name, args, ret, body, null);
  }
}
