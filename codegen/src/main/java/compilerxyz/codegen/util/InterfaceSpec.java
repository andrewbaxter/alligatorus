package compilerxyz.codegen.util;

import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record InterfaceSpec(
    String name,
    int builtinUniqueId,
    int scopeId,
    String kind,
    @Nullable String description,
    boolean registerInScope,
    @Nullable String javaTraitName,
    @Nullable String hostInterfaceBinaryName,
    TList<Method> methods) {
  public InterfaceSpec(
      String name,
      int builtinUniqueId,
      int scopeId,
      String kind,
      @Nullable String description,
      boolean registerInScope,
      @Nullable String javaTraitName,
      TList<Method> methods) {
    this(
        name,
        builtinUniqueId,
        scopeId,
        kind,
        description,
        registerInScope,
        javaTraitName,
        null,
        methods);
  }

  public @Nullable String effectiveHostInterfaceBinaryName() {
    if (hostInterfaceBinaryName != null) return hostInterfaceBinaryName;
    if (javaTraitName != null) return "compilerxyz.gen." + javaTraitName;
    return null;
  }
}
