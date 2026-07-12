package compilerxyz.codegen.util;

import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public record InterfaceSpec(
    String name,
    int builtinUniqueId,
    int scopeId,
    String fieldPrefix,
    String kind,
    @Nullable String description,
    boolean registerInScope,
    @Nullable String javaTraitName,
    @Nullable String typeIdConstantName,
    @Nullable String hostInterfaceBinaryName,
    TList<Method> methods) {
  public InterfaceSpec(
      String name,
      int builtinUniqueId,
      int scopeId,
      String fieldPrefix,
      String kind,
      @Nullable String description,
      boolean registerInScope,
      @Nullable String javaTraitName,
      TList<Method> methods) {
    this(
        name,
        builtinUniqueId,
        scopeId,
        fieldPrefix,
        kind,
        description,
        registerInScope,
        javaTraitName,
        null,
        null,
        methods);
  }

  public InterfaceSpec(
      String name,
      int builtinUniqueId,
      int scopeId,
      String fieldPrefix,
      String kind,
      @Nullable String description,
      boolean registerInScope,
      @Nullable String javaTraitName,
      @Nullable String typeIdConstantName,
      TList<Method> methods) {
    this(
        name,
        builtinUniqueId,
        scopeId,
        fieldPrefix,
        kind,
        description,
        registerInScope,
        javaTraitName,
        typeIdConstantName,
        null,
        methods);
  }

  public @Nullable String effectiveHostInterfaceBinaryName() {
    if (hostInterfaceBinaryName != null) return hostInterfaceBinaryName;
    if (javaTraitName != null) return "compilerxyz.gen." + javaTraitName;
    return null;
  }
}
