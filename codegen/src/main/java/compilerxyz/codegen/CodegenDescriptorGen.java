package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.experimental.CodegenDescriptorScanner;
import compilerxyz.util.TList;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;
import com.squareup.javapoet.MethodSpec;

public final class CodegenDescriptorGen {
    private CodegenDescriptorGen() {
    }

    private static final ClassName STATIC_METHOD_REF =
            ClassName.get("compilerxyz.eval.dynamic", "JavaStaticMethodRef");
    private static final ClassName VIRTUAL_METHOD_REF =
            ClassName.get("compilerxyz.eval.dynamic", "JavaVirtualMethodRef");
    private static final ClassName INTERNAL_NAME =
            ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
    private static final ClassName METHOD_DESCRIPTOR =
            ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
    private static final ClassName DATA_DESCRIPTOR =
            ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");

    public static void run(Path outRoot, Formatter formatter, TList<CodegenDescriptorScanner.Entry> entries)
            throws IOException {
        var cls =
                TypeSpec.classBuilder("GenCodegenDescriptors")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addMethod(
                                MethodSpec.constructorBuilder()
                                        .addModifiers(Modifier.PRIVATE)
                                        .build());

        var sortedEntries = TList.from(entries);
        sortedEntries.sort((a, b) -> a.constantName().compareTo(b.constantName()));
        for (var e : sortedEntries) {
            var descArgs = CodeBlock.builder();
            descArgs.add("$T.fromClass($T.class)", DATA_DESCRIPTOR, e.returnType());
            for (var p : e.paramTypes()) {
                descArgs.add(", $T.fromClass($T.class)", DATA_DESCRIPTOR, p);
            }
            var refClass = e.isStatic() ? STATIC_METHOD_REF : VIRTUAL_METHOD_REF;
            var init =
                    CodeBlock.of(
                            "new $T($T.fromClass($T.class), $S, $T.fromParts($L), $L)",
                            refClass,
                            INTERNAL_NAME,
                            e.owner(),
                            e.methodName(),
                            METHOD_DESCRIPTOR,
                            descArgs.build(),
                            e.ownerIsInterface());
            cls.addField(
                    FieldSpec.builder(
                                    refClass,
                                    e.constantName(),
                                    Modifier.PUBLIC,
                                    Modifier.STATIC,
                                    Modifier.FINAL)
                            .initializer(init)
                            .build());
        }

        var file = JavaFile.builder("compilerxyz.gen", cls.build()).skipJavaLangImports(true).build();
        GenMain.write(outRoot, file, formatter);
    }
}
