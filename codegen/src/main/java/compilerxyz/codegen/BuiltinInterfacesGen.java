package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.util.BuiltinMethodEmit;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

/**
 * Unified generator for {@code @Builtin} interfaces. Emits a {@code Gen<X>}
 * companion class carrying the runtime {@code TypeObjectDynamic} descriptor
 * plus, for aspects, an {@code AspectId} static and {@code get}/{@code getMaybe}
 * lookup helpers. Aspects and traits share this shell; the differences are
 * mode-branched here rather than split across separate generators.
 */
public final class BuiltinInterfacesGen {
  private BuiltinInterfacesGen() {}

  public record BuiltinSpec(
      String simpleName,
      String pkg,
      long typeId,
      long uniqueId,
      boolean isAspect,
      @org.jspecify.annotations.Nullable Long trivialIdentity,
      TList<BuiltinMethodEmit.Method> methods) {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN = ClassName.get("compilerxyz.eval", "Unique", "Builtin");
  private static final ClassName TYPE_ID_BUILTIN = ClassName.get("compilerxyz.eval", "TypeId", "Builtin");
  private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName QEXPR_ID = ClassName.get("compilerxyz.eval", "QExprId");
  private static final ClassName ASPECT_LOOKUP = ClassName.get("compilerxyz.eval.aspect", "AspectLookup");

  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName TYPE_OBJECT_META =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectMeta");
  private static final ClassName TYPE_OBJECT_CAST_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule");
  private static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastFromRule");
  private static final ClassName OBJECT_METHOD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectMethod");
  private static final ClassName OBJECT_DATA_FIELD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDataField");
  private static final ClassName OBJECT_DROP_KIND =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDropKind");
  private static final ClassName OBJECT_ASPECT_SOURCE =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectAspectSource");
  private static final ClassName OBJECT_ASPECT_SOURCE_STATIC =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectAspectSource", "Static");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");

  private static @org.jspecify.annotations.Nullable TMap<ClassName, CodeBlock>
      scannedLeafDescriptors;

  public static void setScannedLeafDescriptors(TMap<ClassName, CodeBlock> scanned) {
    scannedLeafDescriptors = scanned;
  }

  public static void run(
      Path outRoot,
      Formatter formatter,
      TSet<Long> builtinUniqueIds,
      TList<BuiltinSpec> specs)
      throws IOException {
    var seenNames = new TSet<String>();
    for (var spec : specs) {
      if (!seenNames.add(spec.simpleName)) {
        throw new IllegalStateException("duplicate builtin interface name: " + spec.simpleName);
      }
      if (spec.isAspect && !builtinUniqueIds.add(spec.uniqueId)) {
        throw new IllegalStateException(
            "duplicate builtin_unique_id " + spec.uniqueId + " (in aspects)");
      }
      writeCompanion(outRoot, formatter, spec);
    }
  }

  private static void writeCompanion(Path outRoot, Formatter formatter, BuiltinSpec spec)
      throws IOException {
    var companionName = "Gen" + spec.simpleName;
    var self = ClassName.get(GEN_PACKAGE, companionName);
    var interfaceRef = ClassName.get(spec.pkg, spec.simpleName);

    var cls = TypeSpec.classBuilder(companionName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    if (spec.isAspect) {
      cls.addField(
          FieldSpec.builder(UNIQUE, "AspectId", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.uniqueId)
              .build());
      cls.addMethod(
          MethodSpec.methodBuilder("getMaybe")
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(ParameterizedTypeName.get(OPT, interfaceRef))
              .addParameter(EXPR_CONTEXT, "xx")
              .addParameter(TYPE, "t")
              .addStatement("return $T.getMaybe(xx, t, AspectId, $T.class)", ASPECT_LOOKUP, interfaceRef)
              .build());
      cls.addMethod(
          MethodSpec.methodBuilder("get")
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(ParameterizedTypeName.get(ERESULT, interfaceRef))
              .addParameter(EXPR_CONTEXT, "xx")
              .addParameter(QEXPR_ID, "loc")
              .addParameter(TYPE, "t")
              .addStatement("return $T.get(xx, loc, t, AspectId, $T.class)", ASPECT_LOOKUP, interfaceRef)
              .build());
    }

    cls.addMethod(
        MethodSpec.methodBuilder("dynamicType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(spec.isAspect ? TYPE_OBJECT_DYNAMIC : TYPE)
            .addStatement("return Holder.INSTANCE")
            .build());

    var holder = TypeSpec.classBuilder("Holder")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    holder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
    holder.addField(
        FieldSpec.builder(TYPE_OBJECT_DYNAMIC, "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("build()")
            .build());
    holder.addMethod(buildBuild(spec, self, interfaceRef));
    cls.addType(holder.build());

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static MethodSpec buildBuild(BuiltinSpec spec, ClassName self, ClassName interfaceRef) {
    var typeDesc = spec.isAspect
        ? "aspect_" + snake(spec.simpleName.substring("Aspect".length()))
        : snake(spec.simpleName);

    var b = MethodSpec.methodBuilder("build")
        .addModifiers(spec.isAspect ? Modifier.PUBLIC : Modifier.PRIVATE, Modifier.STATIC)
        .returns(TYPE_OBJECT_DYNAMIC);

    b.addStatement("var jvmClass = $T.fromClass($T.class)", JAVA_INTERNAL_NAME, interfaceRef);
    b.addStatement("var jvmClassDesc = $T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, interfaceRef);
    b.addStatement("var methods = new $T<$T>()", TLIST, OBJECT_METHOD);
    b.addStatement("var castRules = new $T<$T>()", TLIST, TYPE_OBJECT_CAST_RULE);
    b.addStatement("var trivialIdentityAspects = new $T<$T>()", TLIST, UNIQUE);
    b.addStatement("var typeId = new $T($LL)", TYPE_ID_BUILTIN, spec.typeId);

    if (spec.isAspect) {
      b.addStatement(
          "var meta = new $T(typeId, $S, trivialIdentityAspects, castRules, new $T<$T>(), "
              + "$T.<$T>some(new $T($T.AspectId)))",
          TYPE_OBJECT_META, typeDesc,
          TLIST, TYPE_OBJECT_CAST_FROM_RULE,
          OPT, OBJECT_ASPECT_SOURCE, OBJECT_ASPECT_SOURCE_STATIC, self);
      b.addStatement(
          "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
              + "new $T<$T>(), methods, $T.DYNAMIC, $T.some(jvmClass))",
          TYPE_OBJECT_DYNAMIC, interfaceRef, TLIST, OBJECT_DATA_FIELD, OBJECT_DROP_KIND, OPT);
    } else {
      b.addStatement(
          "trivialIdentityAspects.add(new $T($LL))", UNIQUE_BUILTIN, spec.uniqueId);
      if (spec.trivialIdentity != null) {
        b.addStatement(
            "trivialIdentityAspects.add(new $T($LL))", UNIQUE_BUILTIN, spec.trivialIdentity);
        b.addStatement(
            "var meta = new $T(typeId, $S, trivialIdentityAspects, castRules, new $T<$T>(), "
                + "$T.<$T>some(new $T(new $T($LL))))",
            TYPE_OBJECT_META, typeDesc,
            TLIST, TYPE_OBJECT_CAST_FROM_RULE,
            OPT, OBJECT_ASPECT_SOURCE, OBJECT_ASPECT_SOURCE_STATIC,
            UNIQUE_BUILTIN, spec.trivialIdentity);
      } else {
        b.addStatement(
            "var meta = new $T(typeId, $S, trivialIdentityAspects, castRules, new $T<$T>(), "
                + "$T.<$T>none())",
            TYPE_OBJECT_META, typeDesc,
            TLIST, TYPE_OBJECT_CAST_FROM_RULE,
            OPT, OBJECT_ASPECT_SOURCE);
      }
      b.addStatement(
          "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
              + "new $T<$T>(), methods, $T.DYNAMIC_LIGHT, $T.<$T>none())",
          TYPE_OBJECT_DYNAMIC, interfaceRef, TLIST, OBJECT_DATA_FIELD, OBJECT_DROP_KIND,
          OPT, JAVA_INTERNAL_NAME);
    }

    if (scannedLeafDescriptors != null) {
      for (var m : spec.methods) {
        if (!m.sourceVisible()) continue;
        BuiltinMethodEmit.emitObjectMethod(
            b, interfaceRef, "instance", "methods", m, scannedLeafDescriptors);
      }
    }

    b.addStatement("return instance");
    return b.build();
  }

  private static String snake(String camel) {
    var out = new StringBuilder();
    for (var i = 0; i < camel.length(); i++) {
      var c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) out.append('_');
      out.append(Character.toLowerCase(c));
    }
    return out.toString();
  }
}
