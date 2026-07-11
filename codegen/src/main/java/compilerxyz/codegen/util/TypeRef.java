package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

public record TypeRef(TypeName path, CodeBlock dynamicType) {

  public static TypeRef leaf(ClassName descriptor) {
    return new TypeRef(DynamicTypes.leafValueClass(descriptor), DynamicTypes.leaf(descriptor));
  }

  public static TypeRef composite1(ClassName wrapper, TypeRef inner) {
    return new TypeRef(
        DynamicTypes.compositeValueClass(wrapper, inner.path()),
        DynamicTypes.composite(wrapper, inner.dynamicType()));
  }

  public static TypeRef composite2(ClassName wrapper, TypeRef first, TypeRef second) {
    return new TypeRef(
        DynamicTypes.compositeValueClass(wrapper, first.path(), second.path()),
        DynamicTypes.composite(wrapper, first.dynamicType(), second.dynamicType()));
  }

  /**
   * The runtime descriptor is unknown / not yet designed. Path can be anything; dynamicType falls
   * through to {@code Type.dynamicType()}. Use only when the class in question has no runtime
   * Type descriptor and the callee's dispatch treats it as any-Type.
   */
  public static TypeRef opaque(TypeName path) {
    return new TypeRef(
        path,
        CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.eval", "Type")));
  }

}
