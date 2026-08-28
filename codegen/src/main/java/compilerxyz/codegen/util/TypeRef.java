package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import compilerxyz.prelude.util.TMap;

public record TypeRef(TypeName path, CodeBlock dynamicType) {

    public static TypeRef leaf(ClassName descriptor, TMap<ClassName, CodeBlock> scannedLeaves) {
        return new TypeRef(DynamicTypes.leafValueClass(descriptor), DynamicTypes.leaf(descriptor, scannedLeaves));
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

    public static TypeRef opaque(TypeName path) {
        return new TypeRef(path, CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.main.gen", "GenType")));
    }
}
