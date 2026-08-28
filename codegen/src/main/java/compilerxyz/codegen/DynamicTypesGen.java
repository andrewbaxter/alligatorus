package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.prelude.util.TMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import javax.lang.model.element.Modifier;

public final class DynamicTypesGen {
    private DynamicTypesGen() {}

    private static final String GEN_PACKAGE = "compilerxyz.main.gen";
    private static final ClassName TYPE = ClassName.get("compilerxyz.main.eval", "Type");
    private static final ClassName OPT = ClassName.get("compilerxyz.prelude.util", "Opt");

    public static void run(Path outRoot, Formatter formatter, TMap<ClassName, CodeBlock> scannedLeaves)
            throws IOException {
        var cls = TypeSpec.classBuilder("GenDynamicTypes").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        cls.addMethod(
                MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

        var method = MethodSpec.methodBuilder("byClassName")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(OPT, TYPE))
                .addParameter(String.class, "name");
        var names = new ArrayList<ClassName>();
        for (var e : scannedLeaves.entrySet()) names.add(e.getKey());
        names.sort(Comparator.comparing(ClassName::reflectionName));
        method.beginControlFlow("switch (name)");
        for (var cn : names) {
            method.addStatement("case $S: return $T.some($L)", cn.reflectionName(), OPT, scannedLeaves.get(cn));
        }
        method.addStatement("default: return $T.none()", OPT);
        method.endControlFlow();
        cls.addMethod(method.build());

        var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
        GenMain.write(outRoot, file, formatter);
    }
}
