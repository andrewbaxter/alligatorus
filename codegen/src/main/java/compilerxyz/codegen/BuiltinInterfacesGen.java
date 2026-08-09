package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.util.BuiltinMethodEmit;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

/**
 * Unified generator for {@code @BuiltinFull} interfaces. Emits a {@code Gen<X>}
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
      long uniqueId,
      long identityId,
      boolean isAspect,
      boolean isRecord,
      TList<BuiltinMethodEmit.Method> methods,
      TList<DataField> dataFields,
      @org.jspecify.annotations.Nullable String outerSimpleName,
      @org.jspecify.annotations.Nullable String nestedSimpleName) {
    public BuiltinSpec(
        String simpleName,
        String pkg,
        long uniqueId,
        long identityId,
        boolean isAspect,
        boolean isRecord,
        TList<BuiltinMethodEmit.Method> methods,
        TList<DataField> dataFields) {
      this(simpleName, pkg, uniqueId, identityId, isAspect, isRecord,
          methods, dataFields, null, null);
    }
  }

  public record DataField(
      String langName,
      String jvmFieldName,
      TypeName fieldJavaType) {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN = ClassName.get("compilerxyz.eval", "Unique", "Builtin");
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
      TSet<Long> aspectIds,
      TList<BuiltinSpec> specs)
      throws IOException {
    var seenNames = new TSet<String>();
    var identityIds = new TSet<Long>();
    for (var spec : specs) {
      if (!seenNames.add(spec.simpleName)) {
        throw new IllegalStateException("duplicate builtin interface name: " + spec.simpleName);
      }
      if (!aspectIds.add(spec.uniqueId)) {
        throw new IllegalStateException(
            "duplicate aspect id " + spec.uniqueId + " (in builtins)");
      }
      if (spec.identityId != 0L && !identityIds.add(spec.identityId)) {
        throw new IllegalStateException(
            "duplicate aspect identity id " + spec.identityId + " (in builtins)");
      }
      writeCompanion(outRoot, formatter, spec);
    }
  }

  private static void writeCompanion(Path outRoot, Formatter formatter, BuiltinSpec spec)
      throws IOException {
    var companionName = "Gen" + spec.simpleName;
    var self = ClassName.get(GEN_PACKAGE, companionName);
    ClassName interfaceRef;
    if (spec.outerSimpleName != null && spec.nestedSimpleName != null) {
      interfaceRef = ClassName.get(spec.pkg, spec.outerSimpleName, spec.nestedSimpleName);
    } else {
      interfaceRef = ClassName.get(spec.pkg, spec.simpleName);
    }

    var cls = TypeSpec.classBuilder(companionName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    if (spec.isAspect) {
      cls.addField(
          FieldSpec.builder(UNIQUE, "AspectId", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.uniqueId)
              .build());
      cls.addField(
          FieldSpec.builder(UNIQUE, "IdentityId", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
              .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.identityId)
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

    cls.addMethod(buildLink(spec, interfaceRef));

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
    b.addStatement("var dataFields = new $T<$T>()", TLIST, OBJECT_DATA_FIELD);
    b.addStatement("var castRules = new $T<$T>()", TLIST, TYPE_OBJECT_CAST_RULE);
    b.addStatement("var trivialIdentityAspects = new $T<$T>()", TLIST, UNIQUE);
    if (spec.isAspect) {
      b.addStatement("trivialIdentityAspects.add($T.IdentityId)", self);
      b.addStatement(
          "var meta = new $T($S, trivialIdentityAspects, castRules, new $T<$T>(), "
              + "$T.<$T>some(new $T($T.AspectId, $T.IdentityId)))",
          TYPE_OBJECT_META, typeDesc,
          TLIST, TYPE_OBJECT_CAST_FROM_RULE,
          OPT, OBJECT_ASPECT_SOURCE, OBJECT_ASPECT_SOURCE_STATIC, self, self);
      b.addStatement(
          "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
              + "dataFields, methods, $T.DYNAMIC, $T.some(jvmClass))",
          TYPE_OBJECT_DYNAMIC, interfaceRef, OBJECT_DROP_KIND, OPT);
    } else if (spec.isRecord) {
      b.addStatement(
          "trivialIdentityAspects.add(new $T($LL))", UNIQUE_BUILTIN, spec.uniqueId);
      emitTypeHostRestamp(b, interfaceRef);
      b.addStatement(
          "var meta = new $T($S, trivialIdentityAspects, castRules, new $T<$T>(), "
              + "$T.<$T>none())",
          TYPE_OBJECT_META, typeDesc,
          TLIST, TYPE_OBJECT_CAST_FROM_RULE,
          OPT, OBJECT_ASPECT_SOURCE);
      b.addStatement(
          "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
              + "dataFields, methods, $T.DYNAMIC_LIGHT, $T.<$T>none())",
          TYPE_OBJECT_DYNAMIC, interfaceRef, OBJECT_DROP_KIND, OPT, JAVA_INTERNAL_NAME);
    } else {
      b.addStatement(
          "trivialIdentityAspects.add(new $T($LL))", UNIQUE_BUILTIN, spec.uniqueId);
      emitTypeHostRestamp(b, interfaceRef);
      b.addStatement(
          "var meta = new $T($S, trivialIdentityAspects, castRules, new $T<$T>(), "
              + "$T.<$T>some(new $T(new $T($LL), new $T($LL))))",
          TYPE_OBJECT_META, typeDesc,
          TLIST, TYPE_OBJECT_CAST_FROM_RULE,
          OPT, OBJECT_ASPECT_SOURCE, OBJECT_ASPECT_SOURCE_STATIC,
          UNIQUE_BUILTIN,
          spec.uniqueId,
          UNIQUE_BUILTIN,
          spec.uniqueId);
      b.addStatement(
          "var instance = new $T(meta, jvmClass, jvmClassDesc, $T.class, "
              + "dataFields, methods, $T.DYNAMIC_LIGHT, $T.some(jvmClass))",
          TYPE_OBJECT_DYNAMIC, interfaceRef, OBJECT_DROP_KIND, OPT);
    }

    b.addStatement("return instance");
    return b.build();
  }

  private static void emitTypeHostRestamp(MethodSpec.Builder b, ClassName interfaceRef) {
    var genTrivial = ClassName.get(GEN_PACKAGE, "GenTrivialIdentityAspects");
    b.beginControlFlow(
        "if ($T.class.isAssignableFrom($T.class))", TYPE, interfaceRef);
    b.addStatement("trivialIdentityAspects.add($T.TYPE_IDENTITY)", genTrivial);
    b.addStatement(
        "castRules.add(new $T.RestampToIdentity($T.TYPE_IDENTITY))",
        TYPE_OBJECT_CAST_RULE,
        genTrivial);
    b.endControlFlow();
  }

  private static MethodSpec buildLink(BuiltinSpec spec, ClassName interfaceRef) {
    var b = MethodSpec.methodBuilder("link")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    b.addStatement("var instance = Holder.INSTANCE");
    b.addStatement("var methods = instance.methods()");
    b.addStatement("var dataFields = instance.dataFields()");
    b.beginControlFlow("if (!methods.isEmpty() || !dataFields.isEmpty())");
    b.addStatement("return");
    b.endControlFlow();
    if (scannedLeafDescriptors != null) {
      for (var m : spec.methods) {
        BuiltinMethodEmit.emitObjectMethod(
            b, interfaceRef, "instance", "methods", m, scannedLeafDescriptors);
      }
      for (var f : spec.dataFields) {
        var descExpr = BuiltinMethodEmit.resolveDescriptor(f.fieldJavaType, scannedLeafDescriptors);
        if (descExpr == null) continue;
        var rawClass = BuiltinMethodEmit.rawClass(f.fieldJavaType);
        if (rawClass == null) continue;
        if (spec.isRecord) {
          b.addStatement(
              "dataFields.add(new $T($S, new $T($S, $T.fromParts($T.fromClass($T.class), $T.of())), $L))",
              OBJECT_DATA_FIELD,
              f.langName,
              ClassName.get("compilerxyz.eval.value.object_", "ObjectDataField", "Access", "MethodCall"),
              f.jvmFieldName,
              ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor"),
              JAVA_DATA_DESCRIPTOR,
              rawClass,
              TLIST,
              descExpr);
        } else {
          b.addStatement(
              "dataFields.add($T.ofField($S, $S, $T.fromClass($T.class), $L))",
              OBJECT_DATA_FIELD,
              f.langName,
              f.jvmFieldName,
              JAVA_DATA_DESCRIPTOR,
              rawClass,
              descExpr);
        }
      }
    }
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
