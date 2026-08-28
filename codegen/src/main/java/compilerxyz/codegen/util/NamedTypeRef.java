package compilerxyz.codegen.util;

import com.squareup.javapoet.CodeBlock;
import compilerxyz.prelude.util.Opt;

public record NamedTypeRef(String name, TypeRef type, Opt<CodeBlock> derivedFrom) {
    public NamedTypeRef(String name, TypeRef type) {
        this(name, type, Opt.none());
    }
}
