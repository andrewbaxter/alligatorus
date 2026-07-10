package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

public record TypeRef(TypeName path, CodeBlock dynamicType) {

  public static TypeRef unit(ClassName type) {
    return new TypeRef(type, CodeBlock.of("new $T()", type));
  }

  public static TypeRef opaque(TypeName path) {
    return new TypeRef(
        path,
        CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.eval", "Type")));
  }

  public static TypeRef viaPush(ClassName path) {
    return new TypeRef(path, CodeBlock.of("$T.dynamicType()", path));
  }

  public static TypeRef viaPushAs(TypeName path, ClassName pushType) {
    return new TypeRef(path, CodeBlock.of("$T.dynamicType()", pushType));
  }
}
