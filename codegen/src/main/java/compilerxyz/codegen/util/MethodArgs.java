package compilerxyz.codegen.util;

import compilerxyz.util.TList;

public sealed interface MethodArgs {

  record Source(TList<NamedTypeRef> fields) implements MethodArgs {}

  record Rust(MethodSig sig) implements MethodArgs {}
}
