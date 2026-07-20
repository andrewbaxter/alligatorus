package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.util.TList;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

/**
 * Emits {@code Gen<TraitName>} companion classes for every {@code @BuiltinTrait} interface.
 * Each companion carries a {@code dynamicType()} returning a {@link
 * compilerxyz.eval.value.object_.TypeObjectDynamic} constructed from the trait's metadata:
 * {@code @BuiltinTypeId} for the typeId, {@code @BuiltinTrivialIdentityAspect} (if present)
 * for the trivial identity aspect entry, jvmClass/jvmClassDesc from the interface itself.
 */
public final class TraitsGen {
  private TraitsGen() {}

  public record TraitSpec(
      String simpleName,
      String pkg,
      long typeId,
      @org.jspecify.annotations.Nullable Long trivialIdentityAspectValue) {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";
  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN = ClassName.get("compilerxyz.eval", "Unique", "Builtin");
  private static final ClassName TYPE_ID = ClassName.get("compilerxyz.eval", "TypeId");
  private static final ClassName TYPE_ID_COMPOSITE = ClassName.get("compilerxyz.eval", "TypeId", "Composite");
  private static final ClassName GEN_TYPE_IDS = ClassName.get("compilerxyz.gen", "GenTypeIds");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName TYPE_OBJECT_META =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectMeta");
  private static final ClassName TYPE_OBJECT_CAST_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule");
  private static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastFromRule");
  private static final ClassName OBJECT_DATA_FIELD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDataField");
  private static final ClassName OBJECT_METHOD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectMethod");
  private static final ClassName OBJECT_DROP_KIND =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDropKind");
  private static final ClassName OBJECT_ASPECT_SOURCE =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectAspectSource");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName CAST_RULE_INVOKE_VIRTUAL_BY_DEST_IDENTITY =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule", "InvokeVirtualByDestIdentity");
  private static final ClassName GEN_TRIVIAL_IDENTITY_ASPECTS =
      ClassName.get("compilerxyz.gen", "GenTrivialIdentityAspects");
  private static final ClassName INTERFACE_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
  private static final ClassName OBJECT_ASPECT_SOURCE_STATIC =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectAspectSource", "Static");

  public static void run(Path outRoot, Formatter formatter, TList<TraitSpec> specs) throws IOException {
    for (var spec : specs) {
      writeCompanion(outRoot, formatter, spec);
    }
  }

  private static void writeCompanion(Path outRoot, Formatter formatter, TraitSpec spec)
      throws IOException {
    var companionName = "Gen" + spec.simpleName();
    var traitRef = ClassName.get(spec.pkg(), spec.simpleName());
    var typeIdConstName = snakeUpper(spec.simpleName()) + "_TYPE_ID";

    var cls = TypeSpec.classBuilder(companionName)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    cls.addMethod(MethodSpec.methodBuilder("dynamicType")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TYPE)
        .addStatement("return Holder.INSTANCE")
        .build());

    var holder = TypeSpec.classBuilder("Holder")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    holder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
    holder.addField(FieldSpec.builder(
            TYPE_OBJECT_DYNAMIC, "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("build()")
        .build());

    var build = MethodSpec.methodBuilder("build")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .returns(TYPE_OBJECT_DYNAMIC);
    build.addStatement("var jvmClass = $T.fromClass($T.class)", JAVA_INTERNAL_NAME, traitRef);
    build.addStatement(
        "var trivialIdentityAspects = new $T<$T>()", TLIST, UNIQUE);
    if (spec.trivialIdentityAspectValue() != null) {
      build.addStatement(
          "trivialIdentityAspects.add(new $T($LL))",
          UNIQUE_BUILTIN,
          spec.trivialIdentityAspectValue());
    }
    build.addStatement(
        "var castRules = new $T<$T>()", TLIST, TYPE_OBJECT_CAST_RULE);
    build.addStatement(
        "var typeId = $T.$L", GEN_TYPE_IDS, typeIdConstName);
    if (spec.trivialIdentityAspectValue() != null) {
      build.addStatement(
          "var meta = new $T(typeId, $S, trivialIdentityAspects, castRules, new $T<$T>(), "
              + "$T.<$T>some(new $T(new $T($LL))))",
          TYPE_OBJECT_META,
          snakeCase(spec.simpleName()),
          TLIST,
          TYPE_OBJECT_CAST_FROM_RULE,
          OPT,
          OBJECT_ASPECT_SOURCE,
          OBJECT_ASPECT_SOURCE_STATIC,
          UNIQUE_BUILTIN,
          spec.trivialIdentityAspectValue());
    } else {
      build.addStatement(
          "var meta = new $T(typeId, $S, trivialIdentityAspects, castRules, new $T<$T>(), "
              + "$T.<$T>none())",
          TYPE_OBJECT_META,
          snakeCase(spec.simpleName()),
          TLIST,
          TYPE_OBJECT_CAST_FROM_RULE,
          OPT,
          OBJECT_ASPECT_SOURCE);
    }
    build.addStatement(
        "return new $T(meta, jvmClass, $T.fromClass($T.class), $T.class, "
            + "new $T<$T>(), new $T<$T>(), $T.DYNAMIC_LIGHT, $T.some(jvmClass))",
        TYPE_OBJECT_DYNAMIC,
        JAVA_DATA_DESCRIPTOR,
        traitRef,
        traitRef,
        TLIST,
        OBJECT_DATA_FIELD,
        TLIST,
        OBJECT_METHOD,
        OBJECT_DROP_KIND,
        OPT);

    holder.addMethod(build.build());
    cls.addType(holder.build());

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static String snakeCase(String s) {
    var sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      var c = s.charAt(i);
      if (Character.isUpperCase(c) && i > 0) sb.append('_');
      sb.append(Character.toLowerCase(c));
    }
    return sb.toString();
  }

  private static String snakeUpper(String s) {
    return snakeCase(s).toUpperCase();
  }
}
