package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.Method;
import compilerxyz.codegen.util.MethodArgs;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import javax.lang.model.element.Modifier;

public final class InterfacesGen {
  private InterfacesGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName SOURCE_INTERFACE_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
  private static final ClassName SOURCE_INTERFACE_METHOD_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceMethodData");
  private static final ClassName BUILTIN_INTERFACE_KIND =
      ClassName.get("compilerxyz.eval.value.source_interface", "BuiltinInterfaceKind");

  private static final ClassName DEFINITION_SET = ClassName.get("compilerxyz.eval", "DefinitionSet");
  private static final ClassName DEF_SET_ID = ClassName.get("compilerxyz.eval", "DefSetId");
  private static final ClassName COMPILE_ID = ClassName.get("compilerxyz.eval", "CompileId");
  private static final ClassName SOURCE_ID = ClassName.get("compilerxyz.eval", "SourceId");
  private static final ClassName IMPORT_STATE_ID =
      ClassName.get("compilerxyz.eval", "ImportStateId");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN =
      ClassName.get("compilerxyz.eval", "Unique", "Builtin");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName CONST_KEY_STR =
      ClassName.get("compilerxyz.eval", "ConstKey", "Str");

  private static final ClassName RECORD = ClassName.get("compilerxyz.eval.value.record", "Record");
  private static final ClassName RECORD_FIELD =
      ClassName.get("compilerxyz.eval.value.record", "RecordField");
  private static final ClassName RECORD_FIELD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");
  private static final ClassName RECORD_BASE_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.record", "RecordBaseDynamic");

  private static final ClassName TUPLE = ClassName.get("compilerxyz.eval.value.tuple", "Tuple");
  private static final ClassName TUPLE_ELEMENT =
      ClassName.get("compilerxyz.eval.value.tuple", "TupleElement");
  private static final ClassName TUPLE_ELEMENT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.tuple", "TupleElement", "Dynamic");

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");

  public static void run(
      Path outRoot,
      Formatter formatter,
      TList<InterfaceSpec> specs,
      TSet<Long> builtinUniqueIds,
      TSet<Long> scopeIds)
      throws IOException {
    validate(specs, builtinUniqueIds, scopeIds);
    writeHolder(outRoot, formatter, specs);
    for (var spec : specs) {
      if (spec.javaTraitName() != null) {
        writeTrait(outRoot, formatter, spec);
      }
    }
  }

  private static void validate(
      TList<InterfaceSpec> specs, TSet<Long> builtinUniqueIds, TSet<Long> scopeIds) {
    var names = new TSet<String>();
    var prefixes = new TSet<String>();
    var kinds = new TSet<String>();
    for (var spec : specs) {
      if (!names.add(spec.name())) {
        throw new IllegalStateException("duplicate interface name: " + spec.name());
      }
      if (!prefixes.add(spec.fieldPrefix())) {
        throw new IllegalStateException("duplicate field_prefix: " + spec.fieldPrefix());
      }
      if (!kinds.add(spec.kind())) {
        throw new IllegalStateException("duplicate kind: " + spec.kind());
      }
      if (!builtinUniqueIds.add((long) spec.builtinUniqueId())) {
        throw new IllegalStateException(
            "duplicate builtin_unique_id " + spec.builtinUniqueId() + " (in interfaces)");
      }
      if (!scopeIds.add((long) spec.scopeId())) {
        throw new IllegalStateException(
            "duplicate scope_id " + spec.scopeId() + " (in interfaces)");
      }
      var methodNames = new TSet<String>();
      for (var m : spec.methods()) {
        if (!methodNames.add(m.name())) {
          throw new IllegalStateException(
              "duplicate method name on iface " + spec.name() + ": " + m.name());
        }
      }
    }
  }

  private static void writeHolder(Path outRoot, Formatter formatter, TList<InterfaceSpec> specs)
      throws IOException {
      var holder =
              TypeSpec.classBuilder("GenBuiltinInterfaces")
                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    for (var spec : specs) {
      holder.addField(
              FieldSpec.builder(
                              SOURCE_INTERFACE_DATA,
                              fieldNameForSpec(spec),
                              Modifier.PUBLIC,
                              Modifier.FINAL)
                      .build());

      var ifaceUpper = upper(spec.name());
      for (var i = 0; i < spec.methods().size(); i++) {
        var m = spec.methods().get(i);
        var fieldName = "_im_" + spec.fieldPrefix() + i;
        var constName = "IM_" + ifaceUpper + "_" + upper(m.name()) + "_FIELD";
        holder.addField(
                FieldSpec.builder(
                                String.class,
                                constName,
                                Modifier.PUBLIC,
                                Modifier.STATIC,
                                Modifier.FINAL)
                        .initializer("$S", fieldName)
                        .build());
      }

      holder.addMethod(
              MethodSpec.methodBuilder(identityMethodName(spec))
                      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                      .returns(UNIQUE)
                      .addStatement(
                              "return new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId())
                      .build());
    }

    var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE);
    for (var spec : specs) {
      var fname = fieldNameForSpec(spec);
      ctor.addParameter(ParameterSpec.builder(SOURCE_INTERFACE_DATA, fname).build());
      ctor.addStatement("this.$N = $N", fname, fname);
    }
    holder.addMethod(ctor.build());

    holder.addMethod(buildFactory(specs));

    var file = JavaFile.builder(GEN_PACKAGE, holder.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static MethodSpec buildFactory(TList<InterfaceSpec> specs) {
      var build =
              MethodSpec.methodBuilder("build")
                      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                      .returns(ClassName.get(GEN_PACKAGE, "GenBuiltinInterfaces"))
                      .addParameter(ParameterSpec.builder(IMPORT_STATE_ID, "importState").build());

    build.addStatement(
        "var builtinDefSet = new $T(new $T(new $T(importState, new $T(Long.MAX_VALUE)),"
            + " Long.MAX_VALUE))",
        DEFINITION_SET,
        DEF_SET_ID,
        COMPILE_ID,
        SOURCE_ID);

    var ctorArgs = new TList<String>();
    for (var spec : specs) {
      var fname = fieldNameForSpec(spec);
      ctorArgs.add(fname);
      build.addStatement(
          "var $N = new $T(builtinDefSet.defSetId, new $T($LL), $T.$L, builtinDefSet)",
          fname,
          SOURCE_INTERFACE_DATA,
          UNIQUE_BUILTIN,
          spec.builtinUniqueId(),
          BUILTIN_INTERFACE_KIND,
          camelToScreamingSnake(spec.kind()));
      var description = spec.description() != null ? spec.description() : spec.name();
      build.addStatement("$N.description = $S", fname, description);

      for (var i = 0; i < spec.methods().size(); i++) {
        var m = spec.methods().get(i);
        var fieldName = "_im_" + spec.fieldPrefix() + i;
        var methodVar = fname + "Method" + i;
        build.addStatement(
            "var $N = new $T($S, $S)",
            methodVar,
            SOURCE_INTERFACE_METHOD_DATA,
            m.name(),
            fieldName);

        for (var dfn : dataFieldNames(m)) {
          build.addStatement("$N.dataFieldNames.add($S)", methodVar, dfn);
        }

        build.addCode(buildSourceArgRecordType(methodVar, m.args()));

        build.addStatement(
            "$N.retType = $L", methodVar, asTypeExpr(m.retType().dynamicType()));

        build.addStatement("$N.methods.add($N)", fname, methodVar);
      }
      build.addStatement("builtinDefSet.interfaces.add($N)", fname);
    }

    var ctorCall = new StringBuilder("return new $T(");
    for (var i = 0; i < ctorArgs.size(); i++) {
      if (i > 0) ctorCall.append(", ");
      ctorCall.append("$N");
    }
    ctorCall.append(")");
    var args = new TList<Object>();
    args.add(ClassName.get(GEN_PACKAGE, "GenBuiltinInterfaces"));
    args.addAll(ctorArgs);
    build.addStatement(ctorCall.toString(), args.toArray());

    return build.build();
  }

  private static CodeBlock buildSourceArgRecordType(String methodVar, MethodArgs src) {
    var b = CodeBlock.builder();
    var mapVar = methodVar + "ArgFields";
    b.addStatement(
        "var $N = new $T<$T, $T>()", mapVar, TORDERED_MAP, CONST_KEY, RECORD_FIELD);
    for (var field : src.fields()) {
      b.addStatement(
          "$N.put(new $T($S), new $T($L))",
          mapVar,
          CONST_KEY_STR,
          field.name(),
          RECORD_FIELD_DYNAMIC,
          asTypeExpr(field.type().dynamicType()));
    }
    b.addStatement(
        "$N.argType = new $T($T.none(), $N)",
        methodVar,
        RECORD,
        OPT,
        mapVar);
    return b.build();
  }

  private static CodeBlock argDynamicTypeExpr(TList<CodeBlock> dyns) {
    if (dyns.size() == 1) {
      return asTypeExpr(dyns.get(0));
    }
    var elements = CodeBlock.builder();
    elements.add("$T.<$T>of(", TLIST, TUPLE_ELEMENT);
    for (var i = 0; i < dyns.size(); i++) {
      if (i > 0) elements.add(", ");
      elements.add("new $T($L)", TUPLE_ELEMENT_DYNAMIC, asTypeExpr(dyns.get(i)));
    }
    elements.add(")");
    return CodeBlock.of("new $T($T.none(), $L)", TUPLE, OPT, elements.build());
  }

  private static CodeBlock asTypeExpr(CodeBlock dyn) {
    return CodeBlock.of("($T) $L", TYPE, dyn);
  }

  private static TList<String> dataFieldNames(Method m) {
    var out = new TList<String>();
    for (var f : m.args().fields()) out.add(stripTrailingUnderscore(f.name()));
    return out;
  }

  private static void writeTrait(Path outRoot, Formatter formatter, InterfaceSpec spec)
      throws IOException {
      var trait =
              TypeSpec.interfaceBuilder(spec.javaTraitName())
                      .addModifiers(Modifier.PUBLIC);

    for (var m : spec.methods()) {
      var method =
          MethodSpec.methodBuilder(javaTraitMethodName(spec, m))
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .returns(m.retType().path());
      for (var p : m.args().fields()) {
        method.addParameter(
            ParameterSpec.builder(p.type().path(), escapeJavaKeyword(p.name())).build());
      }
      trait.addMethod(method.build());
    }

    if (spec.typeIdConstantName() != null) {
      addDynamicTypeFactory(trait, spec);
    }

    var file = JavaFile.builder(GEN_PACKAGE, trait.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName OBJECT_DROP_KIND =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDropKind");
  private static final ClassName OBJECT_DATA_FIELD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDataField");
  private static final ClassName OBJECT_METHOD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectMethod");
  private static final ClassName TYPE_OBJECT_CAST_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule");
  private static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastFromRule");
  private static final ClassName TYPE_OBJECT_LIFT =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectLift");
  private static final ClassName OBJECT_ASPECT_SOURCE =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectAspectSource");
  private static final ClassName GEN_TYPE_IDS =
      ClassName.get("compilerxyz.gen", "GenTypeIds");
  private static final ClassName GEN_BUILTIN_INTERFACES =
      ClassName.get("compilerxyz.gen", "GenBuiltinInterfaces");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");

  private static void addDynamicTypeFactory(TypeSpec.Builder trait, InterfaceSpec spec) {
    var traitClass = ClassName.get("compilerxyz.gen", spec.javaTraitName());
    var jvmInternalPath = "compilerxyz/gen/" + spec.javaTraitName();

    trait.addMethod(
        MethodSpec.methodBuilder("dynamicType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TYPE)
            .addStatement("return DynamicType.INSTANCE")
            .build());

    var holder =
        TypeSpec.classBuilder("DynamicType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    holder.addField(
        FieldSpec.builder(
                TYPE_OBJECT_DYNAMIC, "INSTANCE", Modifier.STATIC, Modifier.FINAL)
            .initializer("build()")
            .build());

    holder.addMethod(
        MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    var build =
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(TYPE_OBJECT_DYNAMIC);
    build.addStatement("var identityAspects = new $T<$T>()", TLIST, UNIQUE);
    build.addStatement(
        "identityAspects.add($T.$N())", GEN_BUILTIN_INTERFACES, identityMethodName(spec));
    build.addStatement(
        "return new $T($T.$N, $S, $T.of($S), $T.fromClass($T.class), "
            + "new $T<$T>(), new $T<$T>(), $T.DYNAMIC_LIGHT, identityAspects, "
            + "new $T<$T>(), new $T<$T>(), $T.<$T>none(), $T.<$T>none(), $T.<$T>none())",
        TYPE_OBJECT_DYNAMIC,
        GEN_TYPE_IDS,
        spec.typeIdConstantName(),
        spec.name(),
        JAVA_INTERNAL_NAME,
        jvmInternalPath,
        JAVA_DATA_DESCRIPTOR,
        traitClass,
        TLIST,
        OBJECT_DATA_FIELD,
        TLIST,
        OBJECT_METHOD,
        OBJECT_DROP_KIND,
        TLIST,
        TYPE_OBJECT_CAST_RULE,
        TLIST,
        TYPE_OBJECT_CAST_FROM_RULE,
        OPT,
        TYPE_OBJECT_LIFT,
        OPT,
        OBJECT_ASPECT_SOURCE,
        OPT,
        JAVA_INTERNAL_NAME);
    holder.addMethod(build.build());

    trait.addType(holder.build());
  }

  private static String fieldNameForSpec(InterfaceSpec spec) {
    return escapeJavaKeyword(spec.name().replace('-', '_'));
  }

  private static String identityMethodName(InterfaceSpec spec) {
    return "builtin" + camelUpper(spec.name()) + "IfaceIdentity";
  }

  private static String camelUpper(String name) {
    var out = new StringBuilder();
    var nextUpper = true;
    for (var c : name.toCharArray()) {
      if (c == '-' || c == '_') {
        nextUpper = true;
        continue;
      }
      out.append(nextUpper ? Character.toUpperCase(c) : c);
      nextUpper = false;
    }
    return out.toString();
  }

  private static String javaMethodName(String name) {
    return escapeJavaKeyword(stripTrailingUnderscore(name));
  }

  private static String javaTraitMethodName(InterfaceSpec spec, Method m) {
    var prefix = snakeToCamel(spec.name());
    return escapeJavaKeyword(prefix + snakeToPascal(m.name()));
  }

  private static String snakeToCamel(String s) {
    var parts = s.split("_");
    var out = new StringBuilder(parts[0]);
    for (var i = 1; i < parts.length; i++) {
      if (parts[i].isEmpty()) continue;
      out.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
    }
    return out.toString();
  }

  private static String snakeToPascal(String s) {
    if (s.isEmpty()) return s;
    var camel = snakeToCamel(s);
    return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
  }

  private static String stripTrailingUnderscore(String s) {
    var i = s.length();
    while (i > 0 && s.charAt(i - 1) == '_') i--;
    return s.substring(0, i);
  }

  private static String upper(String s) {
    return stripTrailingUnderscore(s).toUpperCase(Locale.ROOT).replace('-', '_');
  }

  private static String camelToScreamingSnake(String s) {
    var out = new StringBuilder();
    for (var i = 0; i < s.length(); i++) {
      var c = s.charAt(i);
      if (i > 0
          && Character.isUpperCase(c)
          && Character.isLowerCase(s.charAt(i - 1))) out.append('_');
      out.append(Character.toUpperCase(c));
    }
    return out.toString();
  }

  private static String escapeJavaKeyword(String s) {
    return switch (s) {
      case "type",
              "class",
              "interface",
              "if",
              "else",
              "for",
              "while",
              "do",
              "return",
              "switch",
              "case",
              "default",
              "break",
              "continue",
              "new",
              "this",
              "super",
              "null",
              "true",
              "false",
              "void",
              "int",
              "long",
              "short",
              "byte",
              "char",
              "boolean",
              "float",
              "double",
              "static",
              "final",
              "public",
              "private",
              "protected",
              "abstract",
              "package",
              "import",
              "throws",
              "throw",
              "try",
              "catch",
              "finally",
              "enum",
              "record",
              "instanceof",
              "extends",
              "implements" -> s + "_";
      default -> s;
    };
  }

  @SuppressWarnings("unused")
  private static TList<NamedTypeRef> sourceFieldList(MethodArgs src) {
    return src.fields();
  }
}
