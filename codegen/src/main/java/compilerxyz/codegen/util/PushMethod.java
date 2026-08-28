package compilerxyz.codegen.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.util.TList;

public record PushMethod(
        String name, TList<NamedTypeRef> args, String ret, CodeBlock body, String dynRuntimeMethod, TypeRef retClass) {
    public PushMethod(String name, TList<NamedTypeRef> args, String ret, CodeBlock body) {
        this(name, args, ret, body, null, TypeRef.opaque(ClassName.OBJECT));
    }

    public PushMethod(String name, TList<NamedTypeRef> args, String ret, CodeBlock body, String dynRuntimeMethod) {
        this(name, args, ret, body, dynRuntimeMethod, TypeRef.opaque(ClassName.OBJECT));
    }
}
