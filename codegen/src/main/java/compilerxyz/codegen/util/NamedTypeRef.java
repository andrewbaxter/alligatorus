package compilerxyz.codegen.util;

import com.squareup.javapoet.CodeBlock;
import org.jspecify.annotations.Nullable;

public record NamedTypeRef(String name, TypeRef type, @Nullable CodeBlock derivedFrom) {
  public NamedTypeRef(String name, TypeRef type) {
    this(name, type, null);
  }
}
