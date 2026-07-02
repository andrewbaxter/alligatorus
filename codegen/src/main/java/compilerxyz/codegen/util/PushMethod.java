package compilerxyz.codegen.util;

import com.squareup.javapoet.CodeBlock;
import compilerxyz.util.TList;

public record PushMethod(
    String name, TList<NamedTypeRef> args, String ret, CodeBlock body) {}
