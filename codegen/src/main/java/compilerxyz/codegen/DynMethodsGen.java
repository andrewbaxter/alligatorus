package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.util.TList;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

final class DynMethodsGen {
  private DynMethodsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName TYPE_FUNCTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeFunctionDynamic");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName DYN_RUNTIME =
      ClassName.get("compilerxyz.eval.dynamic", "DynRuntime");

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName TYPE_EXPR =
      ClassName.get("compilerxyz.eval.value.expr", "TypeExpr");
  private static final ClassName TYPE_INTERFACE_SPEC =
      ClassName.get("compilerxyz.eval.value.interface_spec", "TypeInterfaceSpec");
  private static final ClassName INTERFACE_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
  private static final ClassName TYPE_UNIQUE =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");

  private static final ClassName EXPR_CONTEXT =
      ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName STAGED_EXPR_CELL =
      ClassName.get("compilerxyz.eval.expr", "StagedExprCell");
  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");

  private record DynMethodSpec(
      String constName,
      String methodName,
      CodeBlock argType,
      CodeBlock retType,
      CodeBlock retDesc,
      TList<CodeBlock> argDescs) {}

  private static CodeBlock cls(ClassName c) {
    return CodeBlock.of("$T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, c);
  }

  private static CodeBlock descInt() {
    return CodeBlock.of("$T.DESC_INT", JAVA_DATA_DESCRIPTOR);
  }

  private static CodeBlock objectArray() {
    return CodeBlock.of("$T.fromClass(Object[].class)", JAVA_DATA_DESCRIPTOR);
  }

  private static TList<DynMethodSpec> specs() {
    var out = new TList<DynMethodSpec>();

    out.add(
        new DynMethodSpec(
            "STAGE_TYPE",
            "stage",
            CodeBlock.of("$T.dynamicType()", TYPE),
            CodeBlock.of("new $T()", TYPE_EXPR),
            cls(STAGED_EXPR_CELL),
            TList.of(cls(EXPR_CONTEXT), cls(STAGED_EXPR_CELL), descInt(), objectArray())));

    out.add(
        new DynMethodSpec(
            "INTERFACE_REF_TO_TYPE_TYPE",
            "interfaceRefToType",
            CodeBlock.of("$T.dynamicType()", INTERFACE_DATA),
            CodeBlock.of("$T.dynamicType()", TYPE),
            cls(TYPE),
            TList.of(cls(EXPR_CONTEXT), cls(INTERFACE_DATA))));

    out.add(
        new DynMethodSpec(
            "OBJECT_TO_INTERFACE_SPEC",
            "objectToInterfaceSpec",
            CodeBlock.of("$T.dynamicType()", TYPE),
            CodeBlock.of("new $T()", TYPE_INTERFACE_SPEC),
            cls(TYPE_INTERFACE_SPEC),
            TList.of(cls(TYPE_OBJECT_DYNAMIC))));

    out.add(
        new DynMethodSpec(
            "OBJECT_TO_INTERFACE_DATA",
            "objectToInterfaceData",
            CodeBlock.of("$T.dynamicType()", TYPE),
            CodeBlock.of("$T.dynamicType()", INTERFACE_DATA),
            cls(INTERFACE_DATA),
            TList.of(cls(TYPE_OBJECT_DYNAMIC))));

    out.add(
        new DynMethodSpec(
            "SPEC_TO_INTERFACE_DATA",
            "specToInterfaceData",
            CodeBlock.of("new $T()", TYPE_INTERFACE_SPEC),
            CodeBlock.of("$T.dynamicType()", INTERFACE_DATA),
            cls(INTERFACE_DATA),
            TList.of(cls(TYPE_INTERFACE_SPEC))));

    out.add(
        new DynMethodSpec(
            "INTERFACE_REF_TO_INTERFACE_SPEC",
            "interfaceRefToInterfaceSpec",
            CodeBlock.of("$T.dynamicType()", INTERFACE_DATA),
            CodeBlock.of("new $T()", TYPE_INTERFACE_SPEC),
            cls(TYPE_INTERFACE_SPEC),
            TList.of(cls(INTERFACE_DATA))));

    out.add(
        new DynMethodSpec(
            "UNIQUE_EQ_TYPE",
            "uniqueEq",
            CodeBlock.of("new $T()", TYPE_UNIQUE),
            CodeBlock.of("new $T()", TYPE_BOOL_DYNAMIC),
            CodeBlock.of("$T.DESC_BOOL", JAVA_DATA_DESCRIPTOR),
            TList.of(cls(EXPR_CONTEXT), cls(UNIQUE), cls(UNIQUE))));

    return out;
  }

  static void run(Path outRoot, Formatter formatter) throws IOException {
    var cls =
        TypeSpec.classBuilder("GenDynMethods").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    cls.addField(
        FieldSpec.builder(
                JAVA_INTERNAL_NAME,
                "INTNAME",
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL)
            .initializer("$T.fromClass($T.class)", JAVA_INTERNAL_NAME, DYN_RUNTIME)
            .build());

    for (var s : specs()) {
      var argsListing = CodeBlock.builder();
      boolean first = true;
      for (var a : s.argDescs) {
        if (!first) argsListing.add(", ");
        argsListing.add(a);
        first = false;
      }
      var descriptor =
          CodeBlock.of(
              "$T.fromParts($L, $T.of($L))",
              JAVA_METHOD_DESCRIPTOR,
              s.retDesc,
              TLIST,
              argsListing.build());
      cls.addField(
          FieldSpec.builder(
                  TYPE_FUNCTION_DYNAMIC,
                  s.constName,
                  Modifier.PUBLIC,
                  Modifier.STATIC,
                  Modifier.FINAL)
              .initializer(
                  "new $T($L, $L, INTNAME, $S, $L, true)",
                  TYPE_FUNCTION_DYNAMIC,
                  s.argType,
                  s.retType,
                  s.methodName,
                  descriptor)
              .build());
    }

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }
}
