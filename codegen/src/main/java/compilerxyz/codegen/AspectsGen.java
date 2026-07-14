package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

public final class AspectsGen {
  private AspectsGen() {}

  private static final ClassName ASPECT =
      ClassName.get("compilerxyz.eval.aspect", "Aspect");
  private static final ClassName ASPECT_LOOKUP =
      ClassName.get("compilerxyz.eval.aspect", "AspectLookup");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN =
      ClassName.get("compilerxyz.eval", "Unique", "Builtin");
  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName VALUE = ClassName.get("compilerxyz.gen", "Value");
  private static final ClassName EXPR_CONTEXT =
      ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName QEXPR_ID = ClassName.get("compilerxyz.eval", "QExprId");
  private static final ClassName CODE_VALUE = ClassName.get("compilerxyz.eval", "CodeValue");
  private static final ClassName CODE = ClassName.get("compilerxyz.eval", "Code");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName BINDING_ID = ClassName.get("compilerxyz.eval", "BindingId");
  private static final ClassName BINDING = ClassName.get("compilerxyz.gen", "Binding");
  private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName CODE_DYNAMIC =
      ClassName.get("compilerxyz.eval.dynamic", "CodeDynamic");
  private static final ClassName DYN_UTIL =
      ClassName.get("compilerxyz.eval.dynamic", "DynUtil");
  private static final ClassName TARGET_BIND_SUB =
      ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub");
  private static final ClassName ARTIFACT_EXPORTER =
      ClassName.get("compilerxyz.artifact", "ArtifactExporter");
  private static final ClassName SOURCE_INTERFACE_DATA =
      ClassName.get(
          "compilerxyz.eval.value.source_interface", "InterfaceData");
  private static final ClassName VALUE_DYNAMIC_SOURCE =
      ClassName.get("compilerxyz.eval.value", "ValueDynamic", "Source");

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName TYPE_METHOD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeMethodDynamic");
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
  private static final ClassName TYPE_OBJECT_CAST_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule");
  private static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastFromRule");
  private static final ClassName TYPE_OBJECT_LIFT =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectLift");
  private static final ClassName CAST_RULE_CALL_BY_DEST_IDENTITY =
      ClassName.get(
          "compilerxyz.eval.value.object_", "TypeObjectCastRule", "CallByDestIdentity");
  private static final ClassName CAST_RULE_INVOKE_VIRTUAL_BY_DEST_IDENTITY =
      ClassName.get(
          "compilerxyz.eval.value.object_", "TypeObjectCastRule", "InvokeVirtualByDestIdentity");
  private static final ClassName DYN_RUNTIME_HELPER =
      ClassName.get(
          "compilerxyz.eval.value.object_", "TypeObjectCastRule", "DynRuntimeHelper");
  private static final ClassName GEN_IDENTITIES =
      ClassName.get("compilerxyz.gen", "GenIdentities");
  private static final ClassName DYN_RUNTIME =
      ClassName.get("compilerxyz.eval.dynamic", "DynRuntime");
  private static final ClassName TYPE_ID_BUILTIN =
      ClassName.get("compilerxyz.eval", "TypeId", "Builtin");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName RECORD_CLASS =
      ClassName.get("compilerxyz.eval.value.record", "Record");
  private static final ClassName RECORD_FIELD =
      ClassName.get("compilerxyz.eval.value.record", "RecordField");
  private static final ClassName RECORD_FIELD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");
  private static final ClassName CONST_KEY_STR =
      ClassName.get("compilerxyz.eval", "ConstKey", "Str");
  private static final ClassName TYPE_ERESULT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");
  private static final ClassName TYPE_LIST_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.list", "TypeListDynamic");
  private static final ClassName TYPE_OPTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic");
  private static final ClassName TYPE_MAP_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic");
  private static final ClassName TYPE_UNIQUE =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName TYPE_CONSTKEY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.constkey", "TypeConstkeyDynamic");
  private static final ClassName BIND_RESULT_TYPE =
      ClassName.get("compilerxyz.eval.value.bind_result", "BindResultType");
  private static final ClassName INTERFACE_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");

  private static final TMap<ClassName, ClassName> COMPOSITE_DESCRIPTORS = compositeDescriptors();
  private static final TMap<ClassName, CodeBlock> LEAF_DESCRIPTORS = leafDescriptors();

  private static TMap<ClassName, ClassName> compositeDescriptors() {
    var m = new TMap<ClassName, ClassName>();
    m.put(ERESULT, TYPE_ERESULT_DYNAMIC);
    m.put(OPT, TYPE_OPTION_DYNAMIC);
    m.put(TLIST, TYPE_LIST_DYNAMIC);
    m.put(ClassName.get("compilerxyz.util", "TMap"), TYPE_MAP_DYNAMIC);
    return m;
  }

  /**
   * Maps a source-value ClassName (as it appears in {@link AspectMethod}) to the CodeBlock that
   * constructs the runtime {@code Type} descriptor for that value. Returns {@code null} for
   * unmapped types.
   */
  private static TMap<ClassName, CodeBlock> leafDescriptors() {
    var m = new TMap<ClassName, CodeBlock>();
    // Static-factory (value class exposes its own dynamicType()).
    for (var cn :
        new ClassName[] {
          TYPE,
          EXPR_CONTEXT,
          QEXPR_ID,
          CODE_VALUE,
          CODE,
          CONST_KEY,
          BINDING_ID,
          CODE_DYNAMIC,
          JAVA_DATA_DESCRIPTOR,
          TARGET_BIND_SUB,
          INTERFACE_DATA,
          VALUE,
          BINDING,
        }) {
      m.put(cn, CodeBlock.of("$T.dynamicType()", cn));
    }
    // Nullary descriptors — value class distinct from descriptor class.
    m.put(UNIQUE, CodeBlock.of("new $T()", TYPE_UNIQUE));
    return m;
  }


  private static final TypeName EVAL_RES = ParameterizedTypeName.get(ERESULT, CODE_VALUE);

  private static final TypeName OPT_TYPE = ParameterizedTypeName.get(OPT, TYPE);

  public record Param(String name, TypeName type, boolean nullable) {
    public Param(String name, TypeName type) {
      this(name, type, false);
    }
  }

  private static final ClassName NULLABLE =
      ClassName.get("org.jspecify.annotations", "Nullable");

  public record AspectMethod(
      String javadoc,
      String name,
      TList<Param> params,
      TypeName ret,
      @org.jspecify.annotations.Nullable CodeBlock defaultBody,
      boolean sourceVisible) {
    public AspectMethod(String javadoc, String name, TList<Param> params, TypeName ret) {
      this(javadoc, name, params, ret, null, true);
    }
  }

  public record NestedRecord(String javadoc, String name, TList<Param> components) {}

  public record AspectSpec(
      String javadoc,
      String name,
      long builtinUniqueId,
      TList<NestedRecord> nestedRecords,
      TList<AspectMethod> methods) {
    public AspectSpec(String javadoc, String name, long id, TList<AspectMethod> methods) {
      this(javadoc, name, id, TList.of(), methods);
    }
  }

  public static void run(
      Path outRoot,
      Formatter formatter,
      TSet<Long> builtinUniqueIds,
      TSet<Long> scopeIds,
      TList<AspectSpec> extraSpecs)
      throws IOException {
    var seenNames = new TSet<String>();
    var specs = new TList<AspectSpec>();
    specs.addAll(specs());
    specs.addAll(extraSpecs);
    for (var spec : specs) {
      if (!seenNames.add(spec.name)) {
        throw new IllegalStateException("duplicate aspect name: " + spec.name);
      }
      if (!builtinUniqueIds.add(spec.builtinUniqueId)) {
        throw new IllegalStateException(
            "duplicate builtin_unique_id " + spec.builtinUniqueId + " (in aspects)");
      }
      writeAspect(outRoot, formatter, spec);
    }
  }

  private static void writeAspect(Path outRoot, Formatter formatter, AspectSpec spec)
      throws IOException {
    var fullName = "Aspect" + spec.name;
    var self = ClassName.get(GEN_PACKAGE, fullName);

    var typeBuilder =
        TypeSpec.interfaceBuilder(fullName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ASPECT);
    if (spec.javadoc != null && !spec.javadoc.isEmpty()) {
      typeBuilder.addJavadoc(spec.javadoc + "\n");
    }
    typeBuilder.addJavadoc(
        "<p>Generated by {@code AspectsGen}; port of"
            + " {@code source/buildsrc/aspects.rs}'s {@code Aspect" + spec.name
            + "} entry (Rust builtin_unique_id " + spec.builtinUniqueId + ").\n");

    typeBuilder.addField(
            FieldSpec.builder(UNIQUE, "ID", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId)
                    .build());

    for (var nested : spec.nestedRecords) {
      typeBuilder.addType(buildNestedRecord(nested));
    }

    var prefix = "aspect" + spec.name;
    for (var m : spec.methods) {
      var javaName = InterfacesGen.escapeJavaKeyword(
          prefix + Character.toUpperCase(m.name.charAt(0)) + m.name.substring(1));
      var method =
          MethodSpec.methodBuilder(javaName)
              .returns(m.ret);
      if (m.defaultBody != null) {
        method.addModifiers(Modifier.PUBLIC, Modifier.DEFAULT).addCode(m.defaultBody);
      } else {
        method.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
      }
      if (m.javadoc != null && !m.javadoc.isEmpty()) {
        method.addJavadoc(m.javadoc + "\n");
      }
      for (var p : m.params) {
        var pb = ParameterSpec.builder(p.type, InterfacesGen.escapeJavaKeyword(p.name));
        if (p.nullable) {
          pb.addAnnotation(AnnotationSpec.builder(NULLABLE).build());
        }
        method.addParameter(pb.build());
      }
      typeBuilder.addMethod(method.build());
    }

    var getMaybe =
            MethodSpec.methodBuilder("getMaybe")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(OPT, self))
                    .addParameter(EXPR_CONTEXT, "xx")
                    .addParameter(TYPE, "t")
                    .addStatement("return $T.getMaybe(xx, t, ID, $T.class)", ASPECT_LOOKUP, self)
                    .build();
    var get =
            MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(ERESULT, self))
                    .addParameter(EXPR_CONTEXT, "xx")
                    .addParameter(QEXPR_ID, "loc")
                    .addParameter(TYPE, "t")
                    .addStatement("return $T.get(xx, loc, t, ID, $T.class)", ASPECT_LOOKUP, self)
                    .build();
    typeBuilder.addMethod(getMaybe);
    typeBuilder.addMethod(get);

    emitDynamicType(typeBuilder, spec, self);

    var file = JavaFile.builder(GEN_PACKAGE, typeBuilder.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static void emitDynamicType(TypeSpec.Builder typeBuilder, AspectSpec spec, ClassName self) {
    var dynamicType =
        MethodSpec.methodBuilder("dynamicType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TYPE_OBJECT_DYNAMIC)
            .addStatement("return Holder.INSTANCE")
            .build();
    typeBuilder.addMethod(dynamicType);

    var holder =
        TypeSpec.classBuilder("Holder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    holder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
    holder.addField(
        FieldSpec.builder(TYPE_OBJECT_DYNAMIC, "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("build()")
            .build());
    holder.addMethod(buildDynamicTypeBuild(spec, self));
    typeBuilder.addType(holder.build());
  }

  private static MethodSpec buildDynamicTypeBuild(AspectSpec spec, ClassName self) {
    var jvmIfaceStr = "compilerxyz/gen/Aspect" + spec.name;
    var typeDesc = "aspect_" + snake(spec.name);
    var typeIdValue = spec.builtinUniqueId + 10000L;

    var b = MethodSpec.methodBuilder("build")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TYPE_OBJECT_DYNAMIC);
    b.addStatement("var jvmInterface = $T.of($S)", JAVA_INTERNAL_NAME, jvmIfaceStr);
    b.addStatement(
        "var jvmInterfaceDesc = $T.fromInternalName(jvmInterface)", JAVA_DATA_DESCRIPTOR);
    b.addStatement("var methods = new $T<$T>()", TLIST, OBJECT_METHOD);
    b.addStatement("var castRules = new $T<$T>()", TLIST, TYPE_OBJECT_CAST_RULE);
    b.addStatement(
        "castRules.add(new $T($T.INTERFACE_REF_TYPE_IDENTITY, $T.fromClass($T.class), $S, "
            + "$T.fromParts($T.fromClass($T.class), $T.of())))",
        CAST_RULE_INVOKE_VIRTUAL_BY_DEST_IDENTITY,
        GEN_IDENTITIES,
        JAVA_INTERNAL_NAME,
        TYPE_OBJECT_DYNAMIC,
        "getInterfaceData",
        JAVA_METHOD_DESCRIPTOR,
        JAVA_DATA_DESCRIPTOR,
        INTERFACE_DATA,
        TLIST);

    b.addStatement(
        "var instance = new $T(new $T($LL), $S, jvmInterface, jvmInterfaceDesc, new $T<$T>(), methods, "
            + "$T.DYNAMIC, new $T<$T>(), castRules, new $T<$T>(), $T.<$T>none(), "
            + "$T.<$T>some(new $T($T.ID)), $T.some(jvmInterface))",
        TYPE_OBJECT_DYNAMIC,
        TYPE_ID_BUILTIN,
        typeIdValue,
        typeDesc,
        TLIST,
        OBJECT_DATA_FIELD,
        OBJECT_DROP_KIND,
        TLIST,
        UNIQUE,
        TLIST,
        TYPE_OBJECT_CAST_FROM_RULE,
        OPT,
        TYPE_OBJECT_LIFT,
        OPT,
        OBJECT_ASPECT_SOURCE,
        OBJECT_ASPECT_SOURCE_STATIC,
        self,
        OPT);

    var prefix = "aspect" + spec.name;
    for (var m : spec.methods) {
      if (!m.sourceVisible) continue;
      emitMethod(b, spec, self, m, prefix);
    }

    b.addStatement("return instance");
    return b.build();
  }

  /** Attempts to emit ObjectMethod for {@code m}; skips if any param/ret cannot be resolved. */
  private static void emitMethod(
      MethodSpec.Builder b, AspectSpec spec, ClassName self, AspectMethod m, String prefix) {
    var retDesc = resolveDescriptor(m.ret);
    if (retDesc == null) return;
    var paramDescs = new TList<CodeBlock>();
    var paramRawClasses = new TList<TypeName>();
    for (var p : m.params) {
      var pd = resolveDescriptor(p.type);
      if (pd == null) return;
      paramDescs.add(pd);
      paramRawClasses.add(rawClass(p.type));
    }
    var retRaw = rawClass(m.ret);
    if (retRaw == null) return;

    var javaMethodName =
        InterfacesGen.escapeJavaKeyword(
            prefix + Character.toUpperCase(m.name.charAt(0)) + m.name.substring(1));
    var sourceName = snake(m.name);
    var suffix = m.name;

    // argRecord fields
    var argRecordVar = "argRecord_" + suffix + "Fields";
    var argRecordName = "argRecord_" + suffix;
    b.addStatement("var $L = new $T<$T, $T>()", argRecordVar, TORDERED_MAP, CONST_KEY, RECORD_FIELD);
    for (int i = 0; i < m.params.size(); i++) {
      var p = m.params.get(i);
      b.addStatement(
          "$L.put(new $T($S), new $T($L))",
          argRecordVar,
          CONST_KEY_STR,
          p.name,
          RECORD_FIELD_DYNAMIC,
          paramDescs.get(i));
    }
    b.addStatement("var $L = new $T($T.none(), $L)", argRecordName, RECORD_CLASS, OPT, argRecordVar);

    // ret type
    var retTypeName = "retType_" + suffix;
    b.addStatement("var $L = $L", retTypeName, retDesc);

    // JVM method descriptor
    var descName = "desc_" + suffix;
    var argClassBuilder = CodeBlock.builder();
    for (int i = 0; i < paramRawClasses.size(); i++) {
      if (i > 0) argClassBuilder.add(", ");
      argClassBuilder.add("$T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, paramRawClasses.get(i));
    }
    b.addStatement(
        "var $L = $T.fromParts($T.fromClass($T.class), $T.of($L))",
        descName,
        JAVA_METHOD_DESCRIPTOR,
        JAVA_DATA_DESCRIPTOR,
        retRaw,
        TLIST,
        argClassBuilder.build());

    var methodVar = "method_" + suffix;
    b.addStatement(
        "var $L = new $T(instance, $L, $L, jvmInterface, $S, $L, true, false)",
        methodVar,
        TYPE_METHOD_DYNAMIC,
        argRecordName,
        retTypeName,
        javaMethodName,
        descName);
    b.addStatement("methods.add(new $T($S, $L))", OBJECT_METHOD, sourceName, methodVar);
  }

  /** Returns null if the type can't be mapped to a runtime {@code Type} descriptor. */
  private static CodeBlock resolveDescriptor(TypeName tn) {
    if (tn instanceof ClassName cn) {
      var m = LEAF_DESCRIPTORS.get(cn);
      if (m != null) return m;
      // Nested classes inside compilerxyz.gen aspects — treat as delegated to the outer aspect's
      // BindResult-style factory only if we've registered it. Otherwise skip.
      if (cn.equals(ClassName.get(GEN_PACKAGE, "AspectBind", "BindResult"))) {
        return CodeBlock.of("$T.dynamicType()", BIND_RESULT_TYPE);
      }
      return null;
    }
    if (tn instanceof ParameterizedTypeName pt) {
      var wrapper = COMPOSITE_DESCRIPTORS.get(pt.rawType);
      if (wrapper == null) return null;
      var parts = new TList<CodeBlock>();
      for (var arg : pt.typeArguments) {
        var sub = resolveDescriptor(arg);
        if (sub == null) return null;
        parts.add(sub);
      }
      var joined = CodeBlock.builder();
      for (int i = 0; i < parts.size(); i++) {
        if (i > 0) joined.add(", ");
        joined.add(parts.get(i));
      }
      return CodeBlock.of("new $T($L)", wrapper, joined.build());
    }
    return null;
  }

  /** Returns the raw class TypeName used for JavaDataDescriptor.fromClass, or null for primitives. */
  private static TypeName rawClass(TypeName tn) {
    if (tn.isPrimitive()) return null;
    if (tn instanceof ClassName cn) return cn;
    if (tn instanceof ParameterizedTypeName pt) return pt.rawType;
    return null;
  }

  private static TypeSpec buildNestedRecord(NestedRecord nested) {
    var cls =
        TypeSpec.classBuilder(nested.name)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
    if (nested.javadoc != null && !nested.javadoc.isEmpty()) {
      cls.addJavadoc(nested.javadoc + "\n");
    }
    var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    for (var c : nested.components) {
      cls.addField(
          FieldSpec.builder(c.type, c.name, Modifier.PUBLIC, Modifier.FINAL).build());
      ctor.addParameter(ParameterSpec.builder(c.type, c.name).build());
      ctor.addStatement("this.$N = $N", c.name, c.name);
    }
    cls.addMethod(ctor.build());
    for (var c : nested.components) {
      cls.addMethod(
          MethodSpec.methodBuilder(c.name)
              .addModifiers(Modifier.PUBLIC)
              .returns(c.type)
              .addStatement("return this.$N", c.name)
              .build());
    }
    return cls.build();
  }

  private static String snake(String camel) {
    var out = new StringBuilder();
    for (var i = 0; i < camel.length(); i++) {
      var c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) {
        out.append('_');
      }
      out.append(Character.toLowerCase(c));
    }
    return out.toString();
  }

  private static TList<AspectSpec> specs() {
    var xxParam = new Param("xx", EXPR_CONTEXT);
    var locParam = new Param("loc", QEXPR_ID);
    return TList.of(
        // AspectNull (id 1001) migrated: scanned from handwritten/AspectNull.java
        // AspectRecord (id 1002) migrated: scanned from handwritten/AspectRecord.java
        // AspectNumberDynamic (id 1003) migrated: scanned from handwritten/AspectNumberDynamic.java
        // AspectBoolDynamic (id 1004) migrated: scanned from handwritten/AspectBoolDynamic.java
        // AspectStringDynamic (id 1005) migrated: scanned from handwritten/AspectStringDynamic.java
        // AspectBoolConst (id 1006) migrated: scanned from handwritten/AspectBoolConst.java
        // AspectNumberConst (id 1008) migrated: scanned from handwritten/AspectNumberConst.java
        // AspectStringConst (id 1009) migrated: scanned from handwritten/AspectStringConst.java
        // AspectType (id 1010) migrated: scanned from handwritten/AspectType.java
        // AspectListDynamic (id 1011) migrated: scanned from handwritten/AspectListDynamic.java
        // AspectDynamicBoxed (id 1033) migrated: scanned from handwritten/AspectDynamicBoxed.java
        // AspectDynamicUnboxed (id 1034) migrated: scanned from handwritten/AspectDynamicUnboxed.java
        // AspectMapDynamic (id 1012) migrated: scanned from handwritten/AspectMapDynamic.java
        // AspectTuple (id 1013) migrated: scanned from handwritten/AspectTuple.java
        new AspectSpec(
            "Identity aspect for union",
            "Union",
            1014,
            TList.of(
                new NestedRecord(
                    "One union variant (tag, runtime js-tag string, payload type).",
                    "Variant",
                    TList.of(
                        new Param("key", CONST_KEY),
                        new Param("jsTag", ClassName.get(String.class)),
                        new Param("type", TYPE)))),
            TList.of(
                new AspectMethod(
                    "",
                    "variants",
                    TList.of(),
                    ParameterizedTypeName.get(
                        TLIST,
                        ClassName.get(GEN_PACKAGE, "AspectUnion", "Variant"))))),
        // AspectField (id 1015) migrated: scanned from handwritten/AspectField.java
        // AspectFunction (id 1116) migrated: scanned from handwritten/AspectFunction.java
        // AspectSet (id 1017) migrated: scanned from handwritten/AspectSet.java
        // AspectOperatorAdd (id 1018) migrated: scanned from handwritten/AspectOperatorAdd.java
        // AspectOperatorEq (id 1019) migrated: scanned from handwritten/AspectOperatorEq.java
        // AspectOperatorLt (id 1020) migrated: scanned from handwritten/AspectOperatorLt.java
        // AspectInitialBind (id 1021) migrated: scanned from handwritten/AspectInitialBind.java
        new AspectSpec(
            "Creates code to bind a value, and returns the binding (binding).",
            "Bind",
            1022,
            TList.of(
                new NestedRecord(
                    "Result of {@code bind}: the emitted binding code and the resulting binding.",
                    "BindResult",
                    TList.of(new Param("code", CODE), new Param("binding", BINDING)))),
            TList.of(
                new AspectMethod(
                    "",
                    "bind",
                    TList.of(xxParam, locParam, new Param("cv", CODE_VALUE)),
                    ParameterizedTypeName.get(
                        ERESULT,
                        ClassName.get(GEN_PACKAGE, "AspectBind", "BindResult")))))
        // AspectBindDynamic (id 1023) migrated: scanned from handwritten/AspectBindDynamic.java
        // AspectConsumeDynamic (id 1024) migrated: scanned from handwritten/AspectConsumeDynamic.java
        // AspectStackValueDynamic (id 1025) migrated: scanned from handwritten/AspectStackValueDynamic.java
        // AspectPostDeserialize (id 1031) migrated: scanned from handwritten/AspectPostDeserialize.java
        // AspectLift (id 1026) migrated: scanned from handwritten/AspectLift.java
        // AspectIterable (id 1027) migrated: scanned from handwritten/AspectIterable.java
        // AspectArtifact (id 1028) migrated: scanned from handwritten/AspectArtifact.java
        // AspectDropDynamic (id 1032) migrated: scanned from handwritten/AspectDropDynamic.java
        // AspectDropDynamicLight (id 1035) migrated: scanned from handwritten/AspectDropDynamicLight.java
        // AspectFieldUseConst (id 1036) migrated: scanned from handwritten/AspectFieldUseConst.java
        // AspectFieldUseDynamic (id 1037) migrated: scanned from handwritten/AspectFieldUseDynamic.java
        // AspectElementUseConst (id 1038) migrated: scanned from handwritten/AspectElementUseConst.java
        // AspectElementUseDynamic (id 1039) migrated: scanned from handwritten/AspectElementUseDynamic.java
        // AspectElementUseDynamicBind (id 1040) migrated: scanned from handwritten/AspectElementUseDynamicBind.java
        // AspectAspect (id 1029) migrated: scanned from handwritten/AspectAspect.java
        );
  }
}
