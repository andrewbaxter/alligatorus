package compilerxyz.codegen.util;

import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record InterfaceSpec(
    String name,
    int builtinUniqueId,
    int scopeId,
    String kind,
    Opt<String> description,
    boolean registerInScope,
    Opt<String> javaTraitName,
    Opt<String> hostInterfaceBinaryName,
    TList<Method> methods) {
  public InterfaceSpec(
      String name,
      int builtinUniqueId,
      int scopeId,
      String kind,
      Opt<String> description,
      boolean registerInScope,
      Opt<String> javaTraitName,
      TList<Method> methods) {
    this(
        name,
        builtinUniqueId,
        scopeId,
        kind,
        description,
        registerInScope,
        javaTraitName,
        Opt.none(),
        methods);
  }

  public Opt<String> effectiveHostInterfaceBinaryName() {
    if (hostInterfaceBinaryName.optIsSome()) return hostInterfaceBinaryName;
    if (javaTraitName.optIsSome()) return Opt.some("compilerxyz.gen." + javaTraitName.optUnwrap());
    return Opt.none();
  }
}
