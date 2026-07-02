package compilerxyz.codegen.util;

import compilerxyz.util.TList;

public record MethodSig(TList<NamedTypeRef> context, TList<NamedTypeRef> data) {}
