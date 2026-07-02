package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
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
    @Nullable ClassName javaDynamicTypeClass,
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
        methods);
  }
}
