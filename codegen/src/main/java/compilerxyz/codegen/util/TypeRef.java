package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import compilerxyz.prelude.util.TMap;

public record TypeRef(TypeName path, CodeBlock dynamicType) {

    public static TypeRef leaf(ClassName descriptor, TMap<ClassName, CodeBlock> scannedLeaves) {
        TypeName leafValueClass;
        _leafValueClass:
        {
            var value = DynamicTypes.NULLARY.getOrNull(descriptor);
            {
                leafValueClass = value != null ? value : descriptor;
                break _leafValueClass;
            }
        }
        return new TypeRef(leafValueClass, DynamicTypes.leaf(descriptor, scannedLeaves));
    }
}
